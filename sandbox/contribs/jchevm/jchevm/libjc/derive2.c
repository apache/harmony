
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

/*
 * This file performs class derivation for interpreted classes.
 * I.e., it is the non-ELF equivalent of derive.c.
 */

/* Internal functions */
static jint	_jc_derive_fields(_jc_env *env, _jc_type *type);
static jint	_jc_derive_methods(_jc_env *env, _jc_type *type);

/*
 * Given a class file, create a corresponding internal representation
 * of the Java run-time type. This is done when there is no ELF object
 * available, so we have to create the type structure at runtime and
 * interpret the class' bytecode for method execution.
 *
 * If unsuccessful, an exception is stored.
 */
_jc_type *
_jc_derive_type_interp(_jc_env *env,
	_jc_class_loader *loader, _jc_classbytes *cbytes)
{
	_jc_jvm *const vm = env->vm;
	int num_class_vmethods;
	int num_super_vmethods;
	int num_vmethods;
	_jc_nonarray_type *ntype;
	_jc_type **interfaces;
	_jc_type *superclass;
	_jc_classfile *cfile;
	_jc_type_node node_key;
	_jc_type_node *node;
	size_t virtual_offset;
	size_t static_offset;
	int vtable_index;
	_jc_type *type;
	void *mark;
	int i;

	/* Parse class file */
	if ((cfile = _jc_parse_classfile(env, cbytes, 2)) == NULL)
		return NULL;

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Mark top of class loader memory */
	mark = _jc_uni_mark(&loader->uni);

	/* Get superclass (it should already be loaded) */
	if (cfile->superclass != NULL) {
		node_key.type = (_jc_type *)((char *)&cfile->superclass
		    - _JC_OFFSETOF(_jc_type, name));
		node = _jc_splay_find(&loader->initiated_types, &node_key);
		_JC_ASSERT(node != NULL && node->type != NULL);
		superclass = node->type;
	} else
		superclass = NULL;

	/* Get interfaces (they should already be loaded) */
	if (cfile->num_interfaces > 0) {

		/* Allocate array */
		if ((interfaces = _JC_STACK_ALLOC(env, cfile->num_interfaces
		    * sizeof(*interfaces))) == NULL)
			goto fail;

		/* Fill array */
		for (i = 0; i < cfile->num_interfaces; i++) {
			node_key.type = (_jc_type *)((char *)&cfile
			    ->interfaces[i] - _JC_OFFSETOF(_jc_type, name));
			node = _jc_splay_find(&loader->initiated_types,
			    &node_key);
			_JC_ASSERT(node != NULL && node->type != NULL);
			interfaces[i] = node->type;
		}
	} else
		interfaces = NULL;

	/* Compute the size of this class' vtable */
	num_class_vmethods = 0;
	num_super_vmethods = 0;
	if (!_JC_ACC_TEST(cfile, INTERFACE)) {

		/* Get total virtual methods in all superclasses */
		if (superclass != NULL) {
			num_super_vmethods
			    = superclass->u.nonarray.num_vmethods;
		}

		/* Count virtual methods in this class */
		for (i = 0; i < cfile->num_methods; i++) {
			_jc_cf_method *const method = &cfile->methods[i];

			if (!_JC_ACC_TEST(method, STATIC)
			    && *method->name != '<')
				num_class_vmethods++;
		}
	}
	num_vmethods = num_super_vmethods + num_class_vmethods;

	/* Allocate and start building the type structure */
	if ((type = _jc_cl_alloc(env, loader, sizeof(*type)
	    + num_vmethods * sizeof(*type->vtable))) == NULL)
		goto fail;
	memset(type, 0, sizeof(*type));
	ntype = &type->u.nonarray;
	ntype->num_vmethods = num_vmethods;
	type->superclass = superclass;
	type->access_flags = cfile->access_flags;
	type->flags = _JC_TYPE_REFERENCE | _JC_TYPE_LOADED;
	type->loader = loader;
	ntype->cfile = cfile;

	/* Allocate memory for mtable */
	if (num_vmethods > 0
	    && (ntype->mtable = _jc_cl_alloc(env, loader,
	      num_vmethods * sizeof(*ntype->mtable))) == NULL)
		goto fail;

	/* Allocate memory for type name */
	if ((type->name = _jc_cl_strdup(env, loader, cfile->name)) == NULL)
		goto fail;

	/*
	 * Create the java.lang.Class instance for this type, unless during
	 * initialization when java.lang.Class hasn't been loaded yet.
	 */
	if ((vm->initialization == NULL || vm->initialization->create_class)
	    && _jc_create_class_instance(env, type) != JNI_OK)
		goto fail;

	/* Populate interface list */
	type->num_interfaces = cfile->num_interfaces;
	if (type->num_interfaces > 0
	    && (type->interfaces = _jc_cl_alloc(env, loader,
	      type->num_interfaces * sizeof(*type->interfaces))) == NULL)
		goto fail;
	for (i = 0; i < type->num_interfaces; i++)
		*((_jc_type **)type->interfaces + i) = interfaces[i];

	/* Derive fields */
	if (_jc_derive_fields(env, type) != JNI_OK)
		goto fail;

	/* Derive methods */
	if (_jc_derive_methods(env, type) != JNI_OK)
		goto fail;

	/* Compute field sizes and offsets */
	ntype->num_virtual_refs = type->superclass != NULL ?
	    type->superclass->u.nonarray.num_virtual_refs : 0;
	virtual_offset = type->superclass != NULL ?
	    (type->superclass->u.nonarray.instance_size
	      - ntype->num_virtual_refs * sizeof(void *)) : sizeof(_jc_object);
	static_offset = 0;
	for (i = 0; i < ntype->num_fields; i++) {
		_jc_field *const field = ntype->fields[i];
		const u_char ptype = _jc_sig_types[(u_char)*field->signature];
		const int align = _jc_type_align[ptype];
		const int size = _jc_type_sizes[ptype];

		if (_JC_ACC_TEST(field, STATIC)) {
			while (static_offset % align != 0)
				static_offset++;
			field->offset = static_offset;
			static_offset += size;
		} else {
			_JC_ASSERT(!_JC_ACC_TEST(type, INTERFACE));
			if (ptype == _JC_TYPE_REFERENCE) {
				field->offset = -++ntype->num_virtual_refs
				    * sizeof(void *);
			} else {
				while (virtual_offset % align != 0)
					virtual_offset++;
				field->offset = virtual_offset;
				virtual_offset += size;
			}
		}
	}

	/* Compute instance size */
	if (!_JC_ACC_TEST(type, INTERFACE)) {
		ntype->instance_size = virtual_offset
		    + ntype->num_virtual_refs * sizeof(void *);
	}

	/* Initialize heap block size index skip word flag */
	if (!_JC_ACC_TEST(type, ABSTRACT))
		_jc_initialize_bsi(vm, type);

	/* Allocate memory for static fields, if any */
	if (static_offset > 0
	    && (ntype->class_fields = _jc_cl_zalloc(env,
	      loader, static_offset)) == NULL)
		goto fail;

	/* Build trampolines */
	for (i = 0; i < ntype->num_methods; i++) {
		_jc_method *const method = ntype->methods[i];
		u_char rtype = method->param_ptypes[method->num_parameters];
		u_char *trampoline;
		const void *func;
		int len;

		/* Skip interface methods */
		if (_JC_ACC_TEST(type, INTERFACE)
		    && !_JC_ACC_TEST(method, STATIC))
			continue;

		/* Get the appropriate destination function */
		func = _JC_ACC_TEST(method, NATIVE) ?
		    _jc_interp_native_funcs[rtype] : _jc_interp_funcs[rtype];

		/* Create trampoline */
		len = _jc_build_trampoline(NULL, method, func);
		if ((trampoline = _jc_cl_alloc(env, loader, len)) == NULL)
			goto fail;
		_jc_build_trampoline(trampoline, method, func);
		_jc_iflush(trampoline, len);
		method->function = trampoline;
	}

	/* Skip vtable stuff for interfaces */
	if (_JC_ACC_TEST(type, INTERFACE))
		goto skip_vtable;

	/*
	 * Fill in the vtable and mtable portions corresponding to this class.
	 * We fill in and override the superclass portions during resolution.
	 */
	vtable_index = num_super_vmethods;
	for (i = 0; i < ntype->num_methods; i++) {
		_jc_method *const method = ntype->methods[i];

		if (_JC_ACC_TEST(method, STATIC) || *method->name == '<')
			continue;
		method->vtable_index = vtable_index;
		type->vtable[vtable_index] = method->function;
		ntype->mtable[vtable_index] = method;
		vtable_index++;
	}

	/* Sanity check */
	_JC_ASSERT(vtable_index == num_vmethods);

skip_vtable:
	/* Allocate and initialize type node */
	if ((node = _jc_cl_zalloc(env, loader, sizeof(*node))) == NULL)
		goto fail;
	node->type = type;

	/* Add node to the loader's initiated types tree */
	_jc_splay_insert(&loader->initiated_types, node);

	/* Add type to the loader's defined types tree */
	_jc_splay_insert(&loader->defined_types, type);

	/* Done */
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	return type;

fail:
	/* Give back class loader memory */
	_jc_uni_reset(&loader->uni, mark);

	/* Free parsed class file */
	_jc_destroy_classfile(&cfile);

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	return NULL;
}

/*
 * Derive fields.
 *
 * If unsuccessful an exception is stored.
 */
static jint
_jc_derive_fields(_jc_env *env, _jc_type *type)
{
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	_jc_classfile *const cfile = ntype->cfile;
	int i;

	/* Sanity check */
	_JC_MUTEX_ASSERT(env, type->loader->mutex);

	/* Allocate fields array */
	ntype->num_fields = cfile->num_fields;
	if (ntype->num_fields > 0
	    && (ntype->fields = _jc_cl_alloc(env, type->loader,
	      ntype->num_fields * sizeof(*ntype->fields))) == NULL)
		return JNI_ERR;

	/* Create field structures */
	for (i = 0; i < cfile->num_fields; i++) {
		_jc_cf_field *const cfield = &cfile->fields[i];
		_jc_field *field;
		size_t nlen;
		size_t slen;

		/* Allocate structure */
		nlen = strlen(cfield->name) + 1;
		slen = strlen(cfield->descriptor) + 1;
		if ((field = _jc_cl_alloc(env, type->loader,
		    sizeof(*field) + nlen + slen)) == NULL)
			return JNI_ERR;
		ntype->fields[i] = field;

		/* Initialize it */
		memset(field, 0, sizeof(*field));
		field->name = (char *)(field + 1);
		memcpy((char *)field->name, cfield->name, nlen);
		field->signature = field->name + nlen;
		memcpy((char *)field->signature, cfield->descriptor, slen);
		field->class = type;
		field->access_flags = cfield->access_flags;
	}

	/* Done */
	return JNI_OK;
}

/*
 * Derive methods.
 *
 * If unsuccessful an exception is stored.
 */
static jint
_jc_derive_methods(_jc_env *env, _jc_type *type)
{
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	_jc_classfile *const cfile = ntype->cfile;
	int i;

	/* Sanity check */
	_JC_MUTEX_ASSERT(env, type->loader->mutex);

	/* Allocate methods array */
	ntype->num_methods = cfile->num_methods;
	if (ntype->num_methods > 0
	    && (ntype->methods = _jc_cl_alloc(env, type->loader,
	      ntype->num_methods * sizeof(*ntype->methods))) == NULL)
		return JNI_ERR;

	/* Create method structures */
	for (i = 0; i < cfile->num_methods; i++) {
		_jc_cf_method *const cmethod = &cfile->methods[i];
		_jc_method *method;
		size_t mem_size;
		size_t nlen;
		size_t slen;
		int nparam;
		void *mem;

		/* Allocate structure */
		nlen = strlen(cmethod->name) + 1;
		slen = strlen(cmethod->descriptor) + 1;
		if ((method = _jc_cl_alloc(env, type->loader,
		    sizeof(*method) + nlen + slen)) == NULL)
			return JNI_ERR;
		ntype->methods[i] = method;

		/* Initialize it */
		memset(method, 0, sizeof(*method));
		method->name = (char *)(method + 1);
		memcpy((char *)method->name, cmethod->name, nlen);
		method->signature = method->name + nlen;
		memcpy((char *)method->signature, cmethod->descriptor, slen);
		method->class = type;
		method->access_flags = cmethod->access_flags;

		/* Parse signature and count number of parameters */
		if ((nparam = _jc_resolve_signature(env, method, NULL)) == -1)
			return JNI_ERR;
		method->num_parameters = nparam;
		if (cmethod->exceptions != NULL) {
			method->num_exceptions
			    = cmethod->exceptions->num_exceptions;
		}

		/* Allocate storage for stuff */
		mem_size = (method->num_exceptions + method->num_parameters)
		    * sizeof(_jc_type *) + (method->num_parameters + 1);
		if ((mem = _jc_cl_zalloc(env, type->loader, mem_size)) == NULL)
			return JNI_ERR;

		/* Initialize pointers */
		if (method->num_parameters > 0)
			method->param_types = mem;
		if (method->num_exceptions > 0) {
			method->exceptions = (_jc_type **)mem
			    + method->num_parameters;
		}
		method->param_ptypes = (u_char *)((_jc_type **)mem
		    + method->num_parameters + method->num_exceptions);

		/* Get parameter ptypes */
		_jc_resolve_signature(env, method, NULL);
	}

	/* Done */
	return JNI_OK;
}

