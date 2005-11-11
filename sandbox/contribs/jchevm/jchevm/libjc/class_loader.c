
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
 * $Id: class_loader.c,v 1.13 2005/11/09 18:14:22 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * Find the internal _jc_class_loader structure corresponding to
 * a ClassLoader object. Create one if it doesn't already exist,
 * but do so atomically.
 *
 * Posts an exception on failure.
 */
_jc_class_loader *
_jc_get_loader(_jc_env *env, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	jboolean vm_locked = JNI_FALSE;
	_jc_class_loader *loader;
	_jc_resolve_info info;

	/* Check for null */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		return NULL;
	}
	_JC_ASSERT(_jc_subclass_of(obj, vm->boot.types.ClassLoader));

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);
	vm_locked = JNI_TRUE;

	/* See if loader structure already exists */
	if ((loader = _jc_get_vm_pointer(vm, obj,
	    vm->boot.fields.ClassLoader.vmdata)) != NULL)
		goto done;

	/* Create a new loader structure */
	if ((loader = _jc_create_loader(env)) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}
	loader->instance = obj;

	/* Set the ClassLoader.vmdata field */
	if (_jc_set_vm_pointer(env, obj,
	    vm->boot.fields.ClassLoader.vmdata, loader) != JNI_OK) {
	    	_jc_destroy_loader(vm, &loader);
	    	goto done;
	}

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);
	vm_locked = JNI_FALSE;

	/* Create reference list with one reference */
	memset(&info, 0, sizeof(info));
	info.loader = loader,
	info.implicit_refs = &loader->instance;
	info.num_implicit_refs = 1;

	/* Put ClassLoader object on implicit reference list */
	if (_jc_merge_implicit_refs(env, &info) != JNI_OK) {
		_jc_destroy_loader(vm, &loader);
		_jc_post_exception_info(env);
		goto done;
	}

done:
	/* Unlock VM */
	if (vm_locked)
		_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Done */
	return loader;
}

/*
 * Find the class loader to use when loading a type from a JNI function.
 *
 * The specified algorithm is:
 *  (a) Are we running within a JNI method? If so, find the ClassLoader
 *      associated with the native method's class.
 *  (b) Otherwise, we are being called using the invocation interface
 *      (e.g., from the main java startup C program) so we must use the
 *      loader returned by ClassLoader.getSystemClassLoader().
 *
 * Note: the caller must retain a native reference to the class loader
 * instance (if any) while the class loader is being used.
 */
_jc_class_loader *
_jc_get_jni_loader(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;

	/* Use calling method's loader, if known */
	if (env->jni_method != NULL)
		return env->jni_method->class->loader;

	/* Invoke ClassLoader.getSystemClassLoader() */
	if (_jc_invoke_static(env,
	    vm->boot.methods.ClassLoader.getSystemClassLoader) != JNI_OK)
		return NULL;

	/* Get internal loader structure from ClassLoader instance */
	return _jc_get_loader(env, env->retval.l);
}

/*
 * Allocate and link a new classloader structure into a VM.
 *
 * An exception is stored if unsuccessful.
 *
 * NOTE: The global VM mutex must be held when calling this function.
 */
_jc_class_loader *
_jc_create_loader(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *loader;

	/* Sanity check */
	_JC_MUTEX_ASSERT(env, vm->mutex);

	/* Create and initialize new structure */
	if ((loader = _jc_vm_zalloc(env, sizeof(*loader)
	    + vm->object_path_len * sizeof(*loader->objects_loaded))) == NULL)
		return NULL;
	loader->objects_loaded = (jboolean *)(loader + 1);

	/* Initialize class loader memory manager */
	_jc_uni_alloc_init(&loader->uni, _JC_CL_ALLOC_MIN_PAGES,
	    &vm->avail_loader_pages);

	/* Initialize mutex and condition variable */
	if (_jc_mutex_init(env, &loader->mutex) != JNI_OK)
		goto fail1;
	if (_jc_cond_init(env, &loader->cond) != JNI_OK)
		goto fail2;

	/* Initialize initiated, defining, and partially derived type trees */
	_jc_splay_init(&loader->initiated_types,
	    _jc_node_cmp, _JC_OFFSETOF(_jc_type_node, node));
	_jc_splay_init(&loader->deriving_types,
	    _jc_node_cmp, _JC_OFFSETOF(_jc_type_node, node));
	_jc_splay_init(&loader->defined_types,
	    _jc_type_cmp, _JC_OFFSETOF(_jc_type, node));

	/* Initialize native library list */
	STAILQ_INIT(&loader->native_libs);

	/* Link new class loader into the VM */
	LIST_INSERT_HEAD(&vm->class_loaders, loader, link);

	/* Done */
	return loader;

	/* Clean up after failure */
fail2:	_jc_mutex_destroy(&loader->mutex);
fail1:	_jc_uni_alloc_free(&loader->uni);
	_jc_vm_free(&loader);
	return NULL;
}

/*
 * Free a class loader info structure.
 *
 * Most loader specific memory is allocated from the loader's memory
 * pool, so it all gets automatically freed via _jc_cl_alloc_free().
 *
 * NOTE: The global VM mutex must be held or the world must be stopped.
 */
void
_jc_destroy_loader(_jc_jvm *vm, _jc_class_loader **loaderp)
{
	_jc_class_loader *loader = *loaderp;

	/* Sanity check */
	_JC_ASSERT(vm->world_stopped || vm->world_ending);

	/* Sanity check */
	if (loader == NULL)
		return;
	*loaderp = NULL;

	/* Sanity check */
	_JC_ASSERT(loader->deriving_types.size == 0);

	/* Unload associated native libraries */
	_jc_unload_native_libraries(vm, loader);

	/*
	 * Walk the tree of classes defined by this loader
	 * and for each class:
	 *
	 * 1. Remove the class' methods from the method tree
	 * 2. Unreference class and dependencies from VM class file tree
	 * 3. Destroy any associated ELF linking information
	 */
	while (loader->defined_types.size > 0) {
		_jc_class_node *cnode;
		_jc_class_node key;
		_jc_type *type;
		int j;

		/* Get type at the root of the tree */
		_JC_ASSERT(loader->defined_types.root != NULL);
		type = _JC_NODE2ITEM(&loader->defined_types,
		    loader->defined_types.root);

		/* The following stuff does not apply to array types */
		if (_JC_FLG_TEST(type, ARRAY))
			goto remove_type;

		/* Remove all this class' methods from the method tree */
		if (!_JC_ACC_TEST(type, INTERP)) {
			for (j = 0; j < type->u.nonarray.num_methods; j++) {
				_jc_method *const method
				    = type->u.nonarray.methods[j];

				if (method->function == NULL)
					continue;
				_jc_splay_remove(&vm->method_tree, method);
			}
		}

		/* Unreference the class file associated with this class */
		key.name = type->name;
		cnode = _jc_splay_find(&vm->classfiles, &key);
		_JC_ASSERT(cnode != NULL);
		_jc_unref_class_node(vm, &cnode);

		/* Unreference class files this class depends on */
		_jc_unref_class_deps(vm, type->u.nonarray.class_depends,
		    type->u.nonarray.num_class_depends);

		/* Free supers info (unresolved ELF types) */
		_jc_vm_free(&type->u.nonarray.supers);

		/* Unreference parsed class file or ELF object */
		if (_JC_ACC_TEST(type, INTERP))
			_jc_destroy_classfile(&type->u.nonarray.u.cfile);
		else
			_jc_elf_unref(&type->u.nonarray.u.elf);

remove_type:
		/* Remove this type from the tree */
		_jc_splay_remove(&loader->defined_types, type);
	}

	/* Free implicit reference list */
	_jc_vm_free(&loader->implicit_refs);

	/* Free class loader memory */
	_jc_uni_alloc_free(&loader->uni);

	/* Destroy mutex and condition variable */
	_jc_mutex_destroy(&loader->mutex);
	_jc_cond_destroy(&loader->cond);

	/* Unlink from VM */
	LIST_REMOVE(loader, link);

	/* Free class loader */
	_jc_vm_free(&loader);
}

/*
 * Wait for any other thread which the process of loading
 * a type to finish.
 *
 * NOTE: This assumes that the loader mutex is locked.
 */
void
_jc_loader_wait(_jc_env *env, _jc_class_loader *loader)
{
	/* Sanity check */
	_JC_MUTEX_ASSERT(env, loader->mutex);

	/* Exit Java mode */
	_jc_stopping_java(env, "waiting for class loader %p (%s)",
	    loader, (loader->instance == NULL) ?
	      "boot loader" : loader->instance->type->name);

	/* Notify thread we're waiting */
	loader->waiters = JNI_TRUE;

	/* Wait for other thread */
	_JC_COND_WAIT(env, loader->cond, loader->mutex);

	/* Resume Java */
	_jc_resuming_java(env);
}

/*
 * Add some implicit references to the implicit reference list
 * associated with a class loader. We keep this list sorted.
 * This assumes that the 'refs' array is itself already sorted.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_merge_implicit_refs(_jc_env *env, _jc_resolve_info *info)
{
	_jc_class_loader *const loader = info->loader;
	_jc_object **refs = info->implicit_refs;
	int num_refs = info->num_implicit_refs;
	_jc_object **new_implicit_refs;
	int i0;
	int i;
	int j;

	/* Avoid realloc() of size zero */
	if (num_refs == 0)
		return JNI_OK;

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Extend loader's implicit reference array */
	if ((new_implicit_refs = _jc_vm_realloc(env,
	    loader->implicit_refs, (loader->num_implicit_refs + num_refs)
	      * sizeof(*loader->implicit_refs))) == NULL) {
		_JC_MUTEX_UNLOCK(env, loader->mutex);
		return JNI_ERR;
	}
	loader->implicit_refs = new_implicit_refs;

	/* Sortedly merge in new refs, working backwards */
	i = loader->num_implicit_refs;
	for (j = num_refs; j > 0; j--) {
		for (i0 = i; i > 0
		    && loader->implicit_refs[i - 1] > refs[j - 1]; i--);
		if (i > 0 && loader->implicit_refs[i - 1] == refs[j - 1]) {
			num_refs--;
			continue;
		}
		memmove(loader->implicit_refs + i + j,
		    loader->implicit_refs + i,
		    (i0 - i) * sizeof(*loader->implicit_refs));
		loader->implicit_refs[i + j - 1] = refs[j - 1];
	}

	/* Update array length */
	loader->num_implicit_refs += num_refs;

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Done */
	return JNI_OK;
}

