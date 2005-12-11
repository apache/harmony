
/*
 * Copyright 2005 The Apache Software Foundation or its licensors,
 * as applicable.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * $Id$
 */

#include "libjc.h"

/* Internal functions */
static _jc_type	*_jc_usercl_load_type(_jc_env *env, _jc_class_loader *loader,
			const char *name);
static _jc_type	*_jc_bootcl_load_type(_jc_env *env, const char *name);

/*
 * Load a primitive type.
 *
 * This does not create the Class instance; for primitive types,
 * that's done manually at a later point during bootstrapping.
 *
 * Also, we do not add primitive types to the bootstrap loader's
 * initiated or derived type trees, because they are always "found"
 * explicitly and never by searching in these trees by name (what
 * name would you use anyway?).
 *
 * No need for locking, there is always only one thread because
 * this happens early during VM bootstrapping.
 *
 * If unsuccessful an exception is stored.
 */
_jc_type *
_jc_load_primitive_type(_jc_env *env, int ptype)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *type;

	/* Sanity check */
	_JC_ASSERT(ptype >= _JC_TYPE_BOOLEAN && ptype <= _JC_TYPE_VOID);
	_JC_ASSERT(vm->initialization != NULL);

	/* Verbosity */
	VERBOSE(CLASS, vm, "loading primitive type `%s' (%c)",
	    _jc_prim_names[ptype], _jc_prim_chars[ptype]);

	/* Lock boot loader */
	_JC_MUTEX_LOCK(env, vm->boot.loader->mutex);

	/* Create new type descriptor */
	if ((type = _jc_cl_zalloc(env, vm->boot.loader, sizeof(*type))) == NULL)
		goto fail;
	if ((type->name = _jc_cl_strdup(env,
	    vm->boot.loader, _jc_prim_names[ptype])) == NULL) {
		_jc_cl_unalloc(vm->boot.loader, &type, sizeof(*type));
		goto fail;
	}

	type->loader = vm->boot.loader;
	type->access_flags = _JC_ACC_PUBLIC | _JC_ACC_FINAL;
	type->flags = ptype | _JC_TYPE_RESOLVED | _JC_TYPE_VERIFIED
	    | _JC_TYPE_PREPARED | _JC_TYPE_INITIALIZED | _JC_TYPE_LOADED;
	type->initial_lockword = (_JC_TYPE_INVALID << _JC_LW_TYPE_SHIFT)
	    | _JC_LW_ODD_BIT;		/* the lockword should never be used */

	/* Unlock boot loader */
	_JC_MUTEX_UNLOCK(env, vm->boot.loader->mutex);

	/* Done */
	return type;

fail:
	/* Clean up after failure */
	_JC_MUTEX_UNLOCK(env, vm->boot.loader->mutex);
	return NULL;
}

/*
 * Same as _jc_load_type() but with a non-NUL-terminated name.
 */
_jc_type *
_jc_load_type2(_jc_env *env, _jc_class_loader *loader,
	const char *name, size_t len)
{
	char *buf;

	if ((buf = _JC_STACK_ALLOC(env, len + 1)) == NULL) {
		_jc_post_exception_info(env);
		return NULL;
	}
	memcpy(buf, name, len);
	buf[len] = '\0';
	return _jc_load_type(env, loader, buf);
}

/*
 * Load an array or non-array reference type using the supplied class loader.
 *
 * This expects non-array class names to be encoded "like/This",
 * not "Llike/This;".
 */
_jc_type *
_jc_load_type(_jc_env *env, _jc_class_loader *loader, const char *name)
{
	_jc_jvm *const vm = env->vm;

	/* Sanity check */
	if (strchr(name, '.') != NULL) {
		_jc_post_exception_msg(env, _JC_NoClassDefFoundError,
		    "class name `%s' is not in internal format", name);
		return NULL;
	}

	/* Do bootstrap or user-defined class loading as appropriate */
	return (loader == vm->boot.loader) ?
	    _jc_bootcl_load_type(env, name) :
	    _jc_usercl_load_type(env, loader, name);
}

/*
 * Load a type using the bootstrap class loader.
 *
 * If successful, the class is added to the boot loader's initiating
 * and derived types trees.
 *
 * We use the vm->boot.loading_types tree to handle the race between
 * two threads trying to load the same class simultaneously (we don't
 * need to explicitly handle that case for user-defined class loaders,
 * as the ClassLoader instance itself is expected to do that).
 */
static _jc_type *
_jc_bootcl_load_type(_jc_env *env, const char *name)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *const loader = vm->boot.loader;
	jboolean loader_locked = JNI_FALSE;
	_jc_classbytes *cbytes;
	_jc_type_node loading_node;
	_jc_type_node node_key;
	_jc_type_node *node;
	_jc_type *type_key;
	_jc_type *type = NULL;
	int index;

	/* Sanity check */
	_JC_ASSERT(strchr(name, '.') == NULL);

	/*
	 * Set up a fake type to use as the key for searching trees.
	 * This works because 'name' is the only field in _jc_type
	 * that is accessed when searching the splay trees.
	 */
	type_key = (_jc_type *)((char *)&name - _JC_OFFSETOF(_jc_type, name));
	node_key.type = type_key;

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);
	loader_locked = JNI_TRUE;

retry:
	/* Check if loader already been recorded as an initiating loader */
	if ((node = _jc_splay_find(&loader->initiated_types,
	    &node_key)) != NULL) {
		_JC_MUTEX_UNLOCK(env, loader->mutex);
		_JC_ASSERT(node->type != NULL);
		return node->type;
	}

	/*
	 * If another thread is loading this type, wait for it and try again.
	 * The only way this thread could already be loading this type is if
	 * we're about to throw a ClassCircularityError.
	 */
	if ((node = _jc_splay_find(&vm->boot.loading_types,
	    &node_key)) != NULL) {
		if (node->thread == env)
			goto not_loading;
		_jc_loader_wait(env, loader);
		goto retry;
	}

	/* Add type to the loading types tree */
	node = &loading_node;
	memset(node, 0, sizeof(*node));
	node->type = type_key;
	node->thread = env;
	_jc_splay_insert(&vm->boot.loading_types, node);

not_loading:
	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	loader_locked = JNI_FALSE;

	/* Verbosity */
	VERBOSE(CLASS, vm, "loading `%s' (via bootstrap loader)", name);

	/* Handle array types specially */
	if (*name == '[') {
		type = _jc_derive_array_type(env, loader, name);
		goto done;
	}

	/* Find the class file bytes in the filesystem */
	if ((cbytes = _jc_bootcl_find_classbytes(env,
	    name, &index)) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}

	/* Attempt to derive the type from the class file bytes */
	type = _jc_derive_type_from_classfile(env,
	    loader, name, cbytes);

	/* Verbosity */
	if (type != NULL) {
		VERBOSE(CLASS, vm, "found `%s' classfile in `%s'",
		    type->name, vm->boot.class_path[index].pathname);
	}

	/* Free class file bytes */
	_jc_free_classbytes(&cbytes);

done:
	/* Lock loader */
	if (!loader_locked)
		_JC_MUTEX_LOCK(env, loader->mutex);

	/* Remove type from loading types tree and notify waiting threads */
	if (node == &loading_node) {
		_jc_splay_remove(&vm->boot.loading_types, node);
		if (loader->waiters) {
			loader->waiters = JNI_FALSE;
			_JC_COND_BROADCAST(loader->cond);
		}
	}

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Done */
	return type;
}

/*
 * Load a type using a user-defined class loader.
 *
 * If successful, the class is added to the supplied loader's initiating
 * types tree and the defining loader's defined types tree.
 */
static _jc_type *
_jc_usercl_load_type(_jc_env *env, _jc_class_loader *loader, const char *name)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *type = NULL;
	_jc_type_node *node;
	_jc_type_node *temp;
	jobject sref = NULL;

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Create a type node for this class; set up fake type for search */
	if ((node = _jc_cl_zalloc(env, loader, sizeof(*node))) == NULL) {
		_JC_MUTEX_UNLOCK(env, loader->mutex);
		_jc_post_exception_info(env);
		return NULL;
	}
	node->type = (_jc_type *)((char *)&name - _JC_OFFSETOF(_jc_type, name));

	/* Check if loader already been recorded as an initiating loader */
	if ((temp = _jc_splay_find(&loader->initiated_types, node)) != NULL) {
		_jc_cl_unalloc(loader, &node, sizeof(*node));
		_JC_MUTEX_UNLOCK(env, loader->mutex);
		_JC_ASSERT(temp->type != NULL);
		return temp->type;
	}

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Verbosity */
	VERBOSE(CLASS, vm, "loading `%s' (via %s@%p)", name,
	    loader->instance->type->name, loader->instance);

	/* We handle array types but use ClassLoader for non-array types */
	if (*name == '[') {
		if ((type = _jc_derive_array_type(env, loader, name)) == NULL)
			goto fail;
	} else {
		const size_t name_len = strlen(name);
		char *ncopy;
		int i;

		/* Convert "/" in name back to "." */
		if ((ncopy = _JC_STACK_ALLOC(env, name_len)) == NULL) {
			_jc_post_exception_info(env);
			goto fail;
		}
		memcpy(ncopy, name, name_len);
		for (i = 0; i < name_len; i++) {
			if (ncopy[i] == '/')
				ncopy[i] = '.';
		}

		/* Create String object from name */
		if ((sref = _jc_new_local_native_ref(env,
		    _jc_new_string(env, ncopy, name_len))) == NULL)
			goto fail;

		/* Invoke loadClass() */
		if (_jc_invoke_virtual(env,
		    vm->boot.methods.ClassLoader.loadClass,
		    loader->instance, *sref) != JNI_OK)
			goto fail;

		/* Check for null return value */
		if (env->retval.l == NULL) {
			_jc_post_exception(env, _JC_NullPointerException);
			goto fail;
		}

		/* Extract pointer from Class.vmdata field */
		type = *_JC_VMFIELD(vm, env->retval.l,
		    Class, vmdata, _jc_type *);

		/* Sanity check */
		_JC_ASSERT((type->flags & (_JC_TYPE_ARRAY|_JC_TYPE_MASK))
		    == _JC_TYPE_REFERENCE);

		/* Make sure that the correct class was returned */
		if (strcmp(type->name, name) != 0) {
			_jc_post_exception_msg(env, _JC_NoClassDefFoundError,
			    "asked for `%s' from `%s' but got `%s' instead",
			    name, loader->instance->type->name, type->name);
			goto fail;
		}
	}

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Add type to the loader's initiated types tree if not already there */
	node->type = type;
	if (_jc_splay_find(&loader->initiated_types, node) == NULL)
		_jc_splay_insert(&loader->initiated_types, node);
	else
		_jc_cl_unalloc(loader, &node, sizeof(*node));

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Free local reference */
	_jc_free_local_native_ref(&sref);

	/* Verbosity */
	VERBOSE(CLASS, vm, "loaded `%s' via %s@%p", type->name,
	    loader->instance->type->name, loader->instance);

	/* Done */
	return type;

fail:
	/* Give back node */
	_JC_MUTEX_LOCK(env, loader->mutex);
	_jc_cl_unalloc(loader, &node, sizeof(*node));
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Free local reference */
	_jc_free_local_native_ref(&sref);

	/* Done */
	return NULL;
}

/*
 * Create the Class object associated with 'type'. Class objects
 * are allocated from the class loader's memory rather than the heap.
 *
 * If unsuccessful an exception is stored.
 *
 * NOTE: We never invoke constructors for Class instances.
 *
 * NOTE: This assumes the type's class loader mutex is locked.
 */
jint
_jc_create_class_instance(_jc_env *env, _jc_type *type)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *const loader = type->loader;
	_jc_type *const class_type = vm->boot.types.Class;
	void *mem;

	/* Sanity check */
	_JC_ASSERT(type->instance == NULL);
	_JC_ASSERT(vm->boot.types.Class != NULL);
	_JC_ASSERT(vm->boot.fields.Class.vmdata != NULL);
	_JC_ASSERT(vm->initialization == NULL
	    || vm->initialization->create_class);
	_JC_MUTEX_ASSERT(env, loader->mutex);

	/* Allocate the class object from the loader's memory area */
	if ((mem = _jc_cl_alloc(env, loader,
	    class_type->u.nonarray.instance_size)) == NULL)
		goto fail;

	/* Initialize new Class object */
	type->instance = _jc_initialize_class_object(env, mem);

	/* Set Class.vmdata to point to the type */
	*((_jc_type **)((char *)type->instance
	    + vm->boot.fields.Class.vmdata->offset)) = type;

	/* Done */
	return JNI_OK;

fail:
	/* Give back Class object memory */
	if (mem != NULL) {
		_jc_cl_unalloc(loader, &mem,
		    class_type->u.nonarray.instance_size);
		type->instance = NULL;
	}

	/* Done */
	return JNI_ERR;
}

/*
 * Find a type in a class loader's initiated types tree.
 */
_jc_type *
_jc_find_type(_jc_env *env, _jc_class_loader *loader, const char *name)
{
	_jc_type_node node_key;
	_jc_type_node *node;
	_jc_type *type_key;

	/*
	 * Set up a fake type to use as the key for searching trees.
	 * This works because 'name' is the only field in _jc_type
	 * that is accessed when searching the splay trees.
	 */
	type_key = (_jc_type *)((char *)&name - _JC_OFFSETOF(_jc_type, name));
	memset(&node_key, 0, sizeof(node_key));
	node_key.type = type_key;

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Search for type */
	node = _jc_splay_find(&loader->initiated_types, &node_key);

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Return result */
	if (node != NULL) {
		_JC_ASSERT(node->type != NULL);
		return node->type;
	}
	return NULL;
}

