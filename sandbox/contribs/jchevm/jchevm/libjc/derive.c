
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
 * $Id: derive.c,v 1.20 2005/05/10 14:41:38 archiecobbs Exp $
 */

#include "libjc.h"

/* Internal functions */
static jint	_jc_derive_type(_jc_env *env, _jc_type *type,
			_jc_type_node *node);
static void	_jc_underive_type(_jc_env *env, _jc_type *type,
			_jc_type_node *node);

/*
 * List of types that need to be specially recognized during garbage
 * collection in order to handle implicit references. All under java.lang.
 */
static const char	*const _jc_special_classes[] = {
	"Class",
	"ClassLoader",
	"VMThrowable",
	"ref/Reference",
	NULL
};

/*
 * Given the bytes of a class file, derive the internal representation
 * of the corresponding Java run-time type. This assumes the type node
 * is in the loader's initiating types tree. We try to either find the
 * type structure in an ELF file (possibly generating it on demand), or
 * else we create one from the class file.
 *
 * If successful, the type is added to both the class loader's
 * initiated and defined types trees.
 *
 * If unsuccessful, an exception is posted.
 */
_jc_type *
_jc_derive_type_from_classfile(_jc_env *env, _jc_class_loader *loader,
	const char *name, _jc_classbytes *cbytes)
{
	_jc_jvm *const vm = env->vm;
	jboolean deriving_node = JNI_FALSE;
	_jc_type *const key = (_jc_type *)((char *)&name
	    - _JC_OFFSETOF(_jc_type, name));
	_jc_super_info *supers = NULL;
	_jc_class_node *cnode = NULL;
	_jc_classfile *cfile;
	_jc_type_node node;
	_jc_type *type = NULL;
	_jc_type *stype;
	int i;

	/* Parse class file */
	if ((cfile = _jc_parse_classfile(env, cbytes, 1)) == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}

	/*
	 * If name not explicitly supplied, get it from the classfile.
	 * Otherwise, verify that the name matches what we expect.
	 */
	if (name == NULL)
		name = cfile->name;
	else if (strcmp(cfile->name, name) != 0) {
		_jc_post_exception_msg(env, _JC_NoClassDefFoundError,
		    "class file for `%s' actually defines `%s'",
		    name, cfile->name);
		goto fail;
	}

	/* The bootstrap loader must always be used for core Java types */
	if (loader != vm->boot.loader && _JC_CORE_CLASS(name)) {
		_jc_post_exception_msg(env, _JC_LinkageError,
		    "type `%s' must be defined by the bootstrap loader", name);
		goto fail;
	}

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Add class file to VM class file tree */
	if ((cnode = _jc_ref_class_node(env,
	    name, cbytes->hash, cbytes)) == NULL) {
		_JC_MUTEX_UNLOCK(env, vm->mutex);
		_jc_post_exception_info(env);
		return NULL;
	}

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Create a temporary fake type node for the deriving types tree */
	memset(&node, 0, sizeof(node));
	node.type = (_jc_type *)((char *)&name - _JC_OFFSETOF(_jc_type, name));

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Has this loader already been recorded as an initiating loader? */
	if (_jc_splay_find(&loader->initiated_types, &node) != NULL) {
		_JC_MUTEX_UNLOCK(env, loader->mutex);
		_jc_post_exception_msg(env, _JC_LinkageError,
		    "type `%s' has already been defined", name);
		goto fail;
	}

	/* Detect attempts to recursively derive the same type */
	if (_jc_splay_find(&loader->deriving_types, &node) != NULL) {
		_JC_MUTEX_UNLOCK(env, loader->mutex);
		_jc_post_exception_msg(env, _JC_ClassCircularityError,
		    "class `%s'", name);
		goto fail;
	}

	/* Sanity check */
	_JC_ASSERT(_jc_splay_find(&loader->defined_types, node.type) == NULL);

	/* Add node to partially derived types tree */
	_jc_splay_insert(&loader->deriving_types, &node);
	deriving_node = JNI_TRUE;

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Allocate structure for temporarily saving super types */
	if (!vm->loader_enabled || !vm->generation_enabled) {
		if ((supers = _jc_vm_alloc(env,
		    sizeof(*supers) + cfile->num_interfaces
		      * sizeof(*supers->interfaces))) == NULL) {
			_jc_post_exception_info(env);
			goto fail;
		}
	}

	/* Skip loading superclass for java.lang.Object */
	if (cfile->superclass == NULL) {
		stype = NULL;
		goto no_superclass;
	}

	/* Load and validate superclass */
	if ((stype = _jc_load_type(env, loader, cfile->superclass)) == NULL)
		goto fail;
	if ((_JC_ACC_TEST(cfile, INTERFACE) && stype != vm->boot.types.Object)
	    || (stype->access_flags & (_JC_ACC_INTERFACE|_JC_ACC_FINAL)) != 0) {
		_jc_post_exception_msg(env, _JC_IncompatibleClassChangeError,
		    "%s `%s' cannot be the superclass of `%s'",
		    _JC_ACC_TEST(stype, INTERFACE) ?
		      "interface" : "final class", stype->name, name);
		goto fail;
	}
	if (supers != NULL)
		supers->superclass = stype;

no_superclass:
	/* Load and validate superinterfaces */
	for (i = 0; i < cfile->num_interfaces; i++) {
		_jc_type *siface;

		if ((siface = _jc_load_type(env,
		    loader, cfile->interfaces[i])) == NULL)
			goto fail;
		if (!_JC_ACC_TEST(siface, INTERFACE)) {
			_jc_post_exception_msg(env,
			    _JC_IncompatibleClassChangeError, "non-interface"
			    " class `%s' cannot be a superinterface of `%s'",
			    siface->name, name);
			goto fail;
		}
		if (supers != NULL)
			supers->interfaces[i] = siface;
	}

	/* Loading supertypes may have loaded ELF object with type as well */
	_JC_MUTEX_LOCK(env, loader->mutex);
	type = _jc_splay_find(&loader->defined_types, key);
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	if (type != NULL)
		goto done;

	/* If the ELF loader is disabled, we must interpret */
	if (!vm->loader_enabled)
		goto create_type;

	/* Try to load an ELF object containing the type */
	if (_jc_load_object(env, loader, name) == JNI_OK)
		goto find_type;

	/* If something unexpected happened, bail out */
	if (env->ex.num != _JC_LinkageError) {
		_jc_post_exception_info(env);
		goto fail;
	}

	/* If on-demand object generation is disabled, interpret */
	if (!vm->generation_enabled)
		goto create_type;

	/* Generate a new ELF object file for this class */
	if (_jc_generate_object(env, loader, name) != JNI_OK)
		goto fail;

	/* Try again to load object file containing the type */
	if (_jc_load_object(env, loader, name) != JNI_OK) {
		_jc_post_exception_info(env);
		goto fail;
	}

find_type:
	/*
	 * We've done our best to load the right ELF object.
	 * Look in the types tree to see if our type is there.
	 */
	_JC_MUTEX_LOCK(env, loader->mutex);
	type = _jc_splay_find(&loader->defined_types, key);
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	if (type == NULL) {
		_jc_post_exception_msg(env, _JC_LinkageError,
		    "no ELF object found containing `%s'", name);
		goto fail;
	}

	/* Save super types (so we can access them before resolution) */
	type->u.nonarray.supers = supers;
	supers = NULL;

	/* Done */
	goto done;

create_type:
	/*
	 * We are unable or unwilling to find or create an ELF object.
	 * We must interpret this Java class.
	 */
	if ((type = _jc_derive_type_interp(env, loader, cbytes)) == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}
	goto done;

fail:
	_JC_ASSERT(type == NULL);

done:
	/* Remove node from the loader's deriving types tree */
	if (deriving_node) {
		_JC_MUTEX_LOCK(env, loader->mutex);
		_jc_splay_remove(&loader->deriving_types, &node);
		_JC_MUTEX_UNLOCK(env, loader->mutex);
	}

	/* Unreference class file in VM class file tree */
	_JC_MUTEX_LOCK(env, vm->mutex);
	_jc_unref_class_node(vm, &cnode);
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Destroy parsed class file */
	_jc_destroy_classfile(&cfile);

	/* If this thread wants to save class file, save it */
	if (type == NULL) {
		_jc_class_save *csave;

		for (csave = env->class_save;
		    csave != NULL; csave = csave->next) {
			if (strcmp(csave->name, name) == 0) {
				_JC_ASSERT(csave->bytes == NULL);
				csave->bytes = _jc_dup_classbytes(cbytes);
				break;
			}
		}
	}

	/* Free leftover supers info (if any) */
	_jc_vm_free(&supers);

	/* Done */
	_JC_ASSERT(type == NULL || _JC_FLG_TEST(type, LOADED));
	return type;
}

/*
 * Given an object file, load all the types defined in that file at once.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_derive_types_from_object(_jc_env *env, _jc_elf *elf)
{
	_jc_jvm *const vm = env->vm;
	const int num_types = elf->types.size;
	_jc_class_loader *const loader = elf->loader;
	_jc_type_node *nodes = NULL;
	_jc_type **types;
	int i;

	/* Get the list of types defined in this object */
	if ((types = _JC_STACK_ALLOC(env, num_types * sizeof(*types))) == NULL)
		return JNI_ERR;
	_jc_splay_list(&elf->types, (void **)types);

	/* Point types at ELF file and verify object versions */
	for (i = 0; i < num_types; i++) {
		_jc_type *const type = types[i];

		type->u.nonarray.u.elf = elf;
		if (type->u.nonarray.block_size_index != _JC_LIBJC_VERSION) {
			_JC_EX_STORE(env, LinkageError,
			    "type `%s' in ELF object `%s' was created from an"
			    " incompatible `_jc_defs.h' version 0x%04x !="
			    " 0x%04x", type->name, elf->pathname,
			    type->u.nonarray.block_size_index,
			    _JC_LIBJC_VERSION);
			return JNI_ERR;
		}
	}

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Verify that all types defined in the object are not yet loaded */
	for (i = 0; i < num_types; i++) {
		_jc_type *const type = types[i];
		_jc_type *otype;

		if ((otype = _jc_splay_find(&loader->defined_types,
		    type)) != NULL) {
			_JC_EX_STORE(env, LinkageError,
			    "can't load `%s' because it defines the type `%s'"
			    " which was already defined by the previously"
			    " loaded object `%s'", elf->pathname, type->name,
			    otype->u.nonarray.u.elf->pathname);
			goto fail;
		}
	}

	/* Allocate and initialize type nodes */
	if ((nodes = _jc_cl_alloc(env, loader,
	    num_types * sizeof(*nodes))) == NULL)
		goto fail;
	for (i = 0; i < num_types; i++) {
		_jc_type_node *const node = &nodes[i];

		/* Initialize node */
		node->type = types[i];
		node->thread = NULL;
#ifndef NDEBUG
		memset(&node->node, 0, sizeof(node->node));
#endif
	}

	/* Derive all types; if something fails, un-do everything */
	for (i = 0; i < num_types; i++) {
		_jc_type_node *const node = &nodes[i];
		_jc_type *const type = types[i];

		if (_jc_derive_type(env, type, node) != JNI_OK) {
			while (--i > 0)
				_jc_underive_type(env, types[i], &nodes[i]);
			goto fail;
		}
	}

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Verbosity */
	if ((vm->verbose_flags & (1 << _JC_VERBOSE_OBJ)) != 0) {
		if (num_types == 1) {
			_jc_printf(vm, "[verbose %s: loaded `%s' from `%s']\n",
			    _jc_verbose_names[_JC_VERBOSE_OBJ], types[0]->name,
			    elf->pathname);
		} else {
			_jc_printf(vm, "[verbose %s: types loaded from `%s':\n",
			    _jc_verbose_names[_JC_VERBOSE_OBJ], elf->pathname);
			for (i = 0; i < num_types; i++) {
				_jc_type *const type = types[i];

				_jc_printf(vm, "\t%s%s\n", type->name,
				    i == num_types - 1 ? "]" : "");
			}
		}
	}

	/* Done */
	return JNI_OK;

fail:
	/* Attempt to give back class loader memory */
	_jc_cl_unalloc(loader, &nodes, num_types * sizeof(*nodes));

	/* Done */
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	return JNI_ERR;
}

/*
 * Derive a non-array type. This fills in all the runtime field of
 * the _jc_type structure and adds the type to the derived types tree.
 *
 * If unsuccessful an exception is stored.
 *
 * NOTE: This assumes the type's class loader mutex is locked.
 */
static jint
_jc_derive_type(_jc_env *env, _jc_type *type, _jc_type_node *node)
{
	_jc_jvm *const vm = env->vm;
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	_jc_class_loader *const loader = type->loader;
	_jc_class_node *cnode;
	int i;

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));
	_JC_ASSERT(!_JC_FLG_TEST(type, LOADED));
	_JC_MUTEX_ASSERT(env, loader->mutex);

	/*
	 * Create the java.lang.Class instance for this type, unless during
	 * initialization when java.lang.Class hasn't been loaded yet.
	 */
	if ((vm->initialization == NULL || vm->initialization->create_class)
	    && _jc_create_class_instance(env, type) != JNI_OK)
		return JNI_ERR;

	/* Create mtable if needed (i.e., if interpreter is enabled) */
	if (!_JC_ACC_TEST(type, INTERFACE)
	    && (!vm->loader_enabled || !vm->generation_enabled)) {

		/* Allocate mtable */
		if ((ntype->mtable = _jc_cl_alloc(env, loader,
		    ntype->num_vmethods * sizeof(*ntype->mtable))) == NULL)
			return JNI_ERR;

		/* Fill in this class' portion */
		for (i = 0; i < ntype->num_methods; i++) {
			_jc_method *const method = ntype->methods[i];

			if (_JC_ACC_TEST(method, STATIC)
			    || *method->name == '<')
			    	continue;
			_JC_ASSERT(method->vtable_index < ntype->num_vmethods);
			_JC_ASSERT(type->vtable[method->vtable_index]
			    == method->function);
			ntype->mtable[method->vtable_index] = method;
		}
	}

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Add reference to the type's class file in the VM class file tree */
	if ((cnode = _jc_ref_class_node(env,
	    type->name, ntype->hash, NULL)) == NULL) {
		_JC_MUTEX_UNLOCK(env, vm->mutex);
		goto fail;
	}

	/* Add reference to all other class files this type depends on */
	for (i = 0; i < ntype->num_class_depends; i++) {
		_jc_class_depend *const dep
		    = &type->u.nonarray.class_depends[i];

		/* Find/create class file node and add reference to it */
		if (_jc_ref_class_node(env,
		    dep->name, dep->hash, NULL) != NULL)
			continue;

		/* Failed; un-reference previously referenced class nodes */
		_jc_unref_class_deps(vm, ntype->class_depends, i);
		_jc_unref_class_node(vm, &cnode);

		/* Bail out */
		_JC_MUTEX_UNLOCK(env, vm->mutex);
		goto fail;
	}

	/* Add this class' methods to the VM method tree */
	for (i = 0; i < ntype->num_methods; i++) {
		_jc_method *method = ntype->methods[i];

		/* Add method to the method tree if it exists */
		if (method->function != NULL)
			_jc_splay_insert(&vm->method_tree, method);
	}

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Initialize heap block size index skip word flag */
	if (!_JC_ACC_TEST(type, ABSTRACT))
		_jc_initialize_bsi(vm, type);

	/* Add node to the loader's initiated types tree */
	_jc_splay_insert(&loader->initiated_types, node);

	/* Remove type from the ELF file's unloaded types tree */
	_jc_splay_remove(&ntype->u.elf->types, type);

	/* Add type to the loader's defined types tree */
	_jc_splay_insert(&loader->defined_types, type);

	/* Point type node at real type */
	_JC_ASSERT(strcmp(node->type->name, type->name) == 0);
	node->type = type;

	/* Add reference to ELF file */
	_jc_elf_addref(ntype->u.elf);

	/* Set type loaded flag */
	type->flags |= _JC_TYPE_LOADED;

	/* Done */
	return JNI_OK;

fail:
	/* Give back Class object memory */
	if (vm->initialization == NULL || vm->initialization->create_class) {
		_jc_cl_unalloc(loader, &type->instance,
		    vm->boot.types.Class->u.nonarray.instance_size);
	}

	/* Done */
	return JNI_ERR;
}

/*
 * Reverse the effects of _jc_derive_type().
 *
 * NOTE: This assumes the type's class loader mutex is locked.
 */
static void
_jc_underive_type(_jc_env *env, _jc_type *type, _jc_type_node *node)
{
	_jc_jvm *const vm = env->vm;
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	_jc_class_loader *const loader = type->loader;
	_jc_class_node key;
	_jc_class_node *cnode;
	int i;

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));
	_JC_ASSERT(_JC_FLG_TEST(type, LOADED));
	_JC_ASSERT(type->instance != NULL
	    || (vm->initialization != NULL
	      && !vm->initialization->create_class));
	_JC_MUTEX_ASSERT(env, loader->mutex);

	/* Unset type loaded flag */
	type->flags &= ~_JC_TYPE_LOADED;

	/* Remove type from the loader's defined types tree */
	_jc_splay_remove(&loader->defined_types, type);

	/* Put type back in ELF file's unloaded types tree */
	_jc_splay_insert(&ntype->u.elf->types, type);

	/* Remove type node from the loader's initiated types tree */
	_jc_splay_remove(&loader->initiated_types, node);

	/* Unreference ELF file */
	_jc_elf_unref(&ntype->u.elf);

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Unreference class file in VM class file tree */
	key.name = type->name;
	cnode = _jc_splay_find(&vm->classfiles, &key);
	_JC_ASSERT(node != NULL);
	_jc_unref_class_node(vm, &cnode);

	/* Unreference class file dependencies */
	_jc_unref_class_deps(vm, ntype->class_depends,
	    ntype->num_class_depends);

	/* Remove class' methods from the VM method tree */
	for (i = 0; i < ntype->num_methods; i++) {
		_jc_method *method = ntype->methods[i];

		/* Add method to the method tree if it exists */
		if (method->function != NULL)
			_jc_splay_remove(&vm->method_tree, method);
	}

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Give back java.lang.Class object memory */
	if (vm->initialization == NULL || vm->initialization->create_class) {
		_jc_cl_unalloc(loader, &type->instance,
		    vm->boot.types.Class->u.nonarray.instance_size);
	}
}

/*
 * Derive an array type using the supplied loader.
 */
_jc_type *
_jc_derive_array_type(_jc_env *env, _jc_class_loader *loader, const char *name)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *const obj_type = vm->boot.types.Object;
	_jc_type *elem_type = NULL;
	_jc_type *type = NULL;
	const char *base_name;
	_jc_type *base_type;
	_jc_type_node *node;
	_jc_type_node key;
	u_char base_ptype;
	void *mark;
	int dims;

	/* Sanity check */
	_JC_ASSERT(vm->boot.types.Object != NULL);
	_JC_ASSERT(*name == '[');

	/* Count number of dimensions and get base class name */
	for (base_name = name; *++base_name == '['; );
	dims = base_name - name;

	/* Check number of dimensions */
	if (dims < 0 || dims > 255) {
		_jc_post_exception_msg(env, _JC_NoClassDefFoundError,
		    "array type has too many (%d) dimensions: %s",
		    dims, name);
		return NULL;
	}

	/* Remaining class name must not contain any '[' characters */
	if (strchr(base_name, '[') != NULL) {
		_jc_post_exception_msg(env, _JC_NoClassDefFoundError,
		    "%s", name);
		return NULL;
	}

	/* Recursively load element type if multi-dimensional */
	if (dims > 1) {
		if ((elem_type = _jc_load_type(env, loader, name + 1)) == NULL)
			return NULL;
		base_type = elem_type->u.array.base_type;
		goto got_elem_type;
	}

	/* Create/find base type */
	base_ptype = _jc_sig_types[(u_char)*base_name];
	switch (base_ptype) {
	case _JC_TYPE_BOOLEAN:
	case _JC_TYPE_BYTE:
	case _JC_TYPE_CHAR:
	case _JC_TYPE_SHORT:
	case _JC_TYPE_INT:
	case _JC_TYPE_LONG:
	case _JC_TYPE_FLOAT:
	case _JC_TYPE_DOUBLE:
		if (base_name[1] != '\0')
			goto invalid;
		base_type = vm->boot.types.prim[base_ptype];
		_JC_ASSERT(base_type != NULL);
		break;
	case _JC_TYPE_REFERENCE:
	    {
		const size_t base_name_len = strlen(base_name);
		char *bname;

		/* Extract base class name */
		if (base_name[0] != 'L'
		    || base_name_len < 3
		    || base_name[base_name_len - 1] != ';')
			goto invalid;
		if ((bname = _JC_STACK_ALLOC(env, base_name_len - 1)) == NULL) {
			_jc_post_exception_info(env);
			return NULL;
		}
		memcpy(bname, base_name + 1, base_name_len - 2);
		bname[base_name_len - 2] = '\0';

		/* Load/find base class */
		if ((base_type = _jc_load_type(env, loader, bname)) == NULL)
			return NULL;
		break;
	    }
	default:
	invalid:
		_jc_post_exception_msg(env, _JC_NoClassDefFoundError,
		    "invalid class name: %s", name);
		return NULL;
	}
	elem_type = base_type;

got_elem_type:
	/*
	 * The array type's defining loader is the same as it's base type.
	 * If this is different from the initiating loader, then load the
	 * type via the defining loader first. This ensures that the type
	 * ends up in the defining loader's initiated types tree as required.
	 */
	if (base_type->loader != loader) {
		if ((type = _jc_load_type(env,
		    base_type->loader, name)) == NULL)
			return NULL;
		return type;
	}

	/* Lock loader and mark memory */
	_JC_MUTEX_LOCK(env, loader->mutex);
	mark = _jc_uni_mark(&loader->uni);

	/* Check whether another thread has beaten us to the punch */
	key.type = (_jc_type *)((char *)&name - _JC_OFFSETOF(_jc_type, name));
	_JC_ASSERT(_jc_splay_find(&loader->deriving_types, &key) == NULL);
	if ((type = _jc_splay_find(&loader->defined_types, key.type)) != NULL) {
		_JC_MUTEX_UNLOCK(env, loader->mutex);
		return type;
	}

	/*
	 * Allocate memory for the new type descriptor plus a copy of
	 * java.lang.Object's vtable (and mtable if Object is interpreted).
	 */
	_JC_ASSERT(obj_type->u.nonarray.num_vmethods != 0);
	if ((type = _jc_cl_alloc(env, loader,
	    sizeof(*type) + obj_type->u.nonarray.num_vmethods
	      * sizeof(*type->vtable))) == NULL)
		goto fail;

	/* Initialize type */
	memset(type, 0, sizeof(*type));
	if ((type->name = _jc_cl_strdup(env, loader, name)) == NULL)
		goto fail;
	type->loader = loader;
	type->superclass = vm->boot.types.Object;
	type->u.array.dimensions = dims;
	type->u.array.base_type = base_type;
	type->u.array.element_type = elem_type;
	type->access_flags = base_type->access_flags
	    & (_JC_ACC_PUBLIC|_JC_ACC_PROTECTED|_JC_ACC_PRIVATE);
	type->access_flags |= _JC_ACC_FINAL | _JC_ACC_ABSTRACT;
	type->flags = (_JC_TYPE_ARRAY | _JC_TYPE_REFERENCE)
	    | _JC_TYPE_RESOLVED | _JC_TYPE_VERIFIED | _JC_TYPE_PREPARED
	    | _JC_TYPE_INITIALIZED | _JC_TYPE_LOADED;
	type->initial_lockword =
	    ((elem_type->flags & _JC_TYPE_MASK) << _JC_LW_TYPE_SHIFT)
	    | _JC_LW_LIVE_BIT | _JC_LW_KEEP_BIT
	    | _JC_LW_ARRAY_BIT | _JC_LW_ODD_BIT;

	/* Copy Object's vtable */
	memcpy(type->vtable, obj_type->vtable,
	    obj_type->u.nonarray.num_vmethods * sizeof(*type->vtable));

	/* Copy java.lang.Object's interface method lookup info */
	_JC_ASSERT(vm->boot.array.interfaces != NULL);
	type->num_interfaces = vm->boot.array.num_interfaces;
	type->interfaces = vm->boot.array.interfaces;
	type->imethod_hash_table = vm->boot.array.imethod_hash_table;
	type->imethod_quick_table = vm->boot.array.imethod_quick_table;

	/* Initialize type node */
	if ((node = _jc_cl_zalloc(env, loader, sizeof(*node))) == NULL)
		goto fail;
	memset(node, 0, sizeof(*node));
	node->type = type;

	/* Create Class instance (unless Class itself is not loaded yet) */
	if ((vm->initialization == NULL || vm->initialization->create_class)
	    && _jc_create_class_instance(env, type) != JNI_OK)
		goto fail;

	/* Add type node to the loader's initiated types tree */
	_jc_splay_insert(&loader->initiated_types, node);

	/* Add type to the loader's defined types tree */
	_jc_splay_insert(&loader->defined_types, type);

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Done */
	return type;

fail:
	/* Give back loader memory */
	_jc_uni_reset(&loader->uni, mark);

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Post exception and exit */
	_jc_post_exception_info(env);
	return NULL;
}

/*
 * Initialize the lockword for a class (i.e., non-array, non-interface) type.
 * We initialize abstract class lockwords even though they are never directly
 * used. This is so concrete subclasses can use some of the lockword bits.
 */
void
_jc_initialize_lockword(_jc_env *env, _jc_type *type, _jc_type *stype)
{
	_jc_word lockword = 0;
	int ref_count_field;

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));
	_JC_ASSERT(!_JC_ACC_TEST(type, INTERFACE));
	_JC_ASSERT(stype == NULL || stype->initial_lockword != 0);

	/* Set number of reference fields */
	ref_count_field =
	    type->u.nonarray.num_virtual_refs < _JC_LW_MAX(REF_COUNT) - 1 ?
	      type->u.nonarray.num_virtual_refs : _JC_LW_MAX(REF_COUNT) - 1;
	lockword |= ref_count_field << _JC_LW_REF_COUNT_SHIFT;

	/* java.lang.Object gets neither SPECIAL nor FINALIZE bits */
	if (stype == NULL)
		goto no_superclass;

	/*
	 * If superclass is special, then so is this class. All
	 * top level special classes are in the java.lang package.
	 */
	if (_JC_LW_TEST(stype->initial_lockword, SPECIAL))
		lockword |= _JC_LW_SPECIAL_BIT;
	else if (strncmp(type->name, "java/lang/", 10) == 0) {
		const char *const subname = type->name + 10;
		const char *const *sp;

		/* See if this class is listed as special */
		for (sp = _jc_special_classes; *sp != NULL; sp++) {
			if (strcmp(subname, *sp) == 0) {
				lockword |= _JC_LW_SPECIAL_BIT;
				break;
			}
		}
	}

	/* Determine if class or a superclass overrides Object.finalize() */
	if (_JC_LW_TEST(stype->initial_lockword, FINALIZE)
	    || _jc_get_declared_method(env, type,
	      "finalize", "()V", _JC_ACC_STATIC, 0) != NULL)
		lockword |= _JC_LW_FINALIZE_BIT;

no_superclass:
	/* Set type bits and odd bit */
	lockword |= (_JC_TYPE_REFERENCE << _JC_LW_TYPE_SHIFT)
	    | _JC_LW_LIVE_BIT | _JC_LW_KEEP_BIT | _JC_LW_ODD_BIT;

	/* Done */
	type->initial_lockword = lockword;
}

/*
 * Initialize heap block size index for a class and set the
 * _JC_TYPE_SKIPWORD flag in type->flags if appropriate.
 */
void
_jc_initialize_bsi(_jc_jvm *vm, _jc_type *type)
{
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	int block_size;
	int bsi;

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));
	_JC_ASSERT(!_JC_ACC_TEST(type, ABSTRACT));

	/* Get block size index */
	bsi = _jc_heap_block_size(vm, ntype->instance_size);

	/* Reverse-engineer actual block size */
	block_size = bsi < 0 ?
	    ((-bsi * _JC_PAGE_SIZE) - _JC_HEAP_BLOCK_OFFSET) :
	    vm->heap.sizes[bsi].size;

	/* Determine if there's room for a skip word */
	if (block_size - sizeof(_jc_word) >= ntype->instance_size
	    && ntype->num_virtual_refs >= _JC_SKIPWORD_MIN_REFS)
		type->flags |= _JC_TYPE_SKIPWORD;

	/* Done */
	ntype->block_size_index = bsi;
}

