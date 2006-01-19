
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
static void		_jc_resolve_vtable(_jc_jvm *vm, _jc_type *type);

/* Empty interface method lookup tables */
_jc_method		*_jc_empty_quick_table[_JC_IMETHOD_HASHSIZE];
_jc_method		**_jc_empty_imethod_table[_JC_IMETHOD_HASHSIZE];

/*
 * Resolve a type.
 *
 * This means resolving all symbolic references to other types,
 * methods, etc. In addition, update the list of implicit references
 * from this class' class loader to other types' Class objects.
 *
 * This function synchronizes on the type's Class object.
 * The type must not be an array type.
 */
jint
_jc_resolve_type(_jc_env *env, _jc_type *type)
{
	_jc_class_loader *const loader = type->loader;
	_jc_object *const obj = type->instance;
	jboolean class_locked = JNI_FALSE;
	_jc_resolve_info info;
	jint status = JNI_ERR;
	int i;

	/* Optimization for array types */
	if (_JC_FLG_TEST(type, ARRAY)) {
		_JC_ASSERT(_JC_FLG_TEST(type, RESOLVED));
		return JNI_OK;
	}

	/* Initialize resolve info */
	memset(&info, 0, sizeof(info));
	info.loader = loader;

	/* Lock class object (except during initial bootstrap) */
	if (obj != NULL) {
		if (_jc_lock_object(env, obj) != JNI_OK)
			goto fail;
		class_locked = JNI_TRUE;
	}

	/* Already resolved? */
	if (_JC_FLG_TEST(type, RESOLVED)) {
		_JC_ASSERT(_JC_FLG_TEST(type, LOADED));
		_JC_ASSERT(_JC_FLG_TEST(type, VERIFIED));
		_JC_ASSERT(_JC_FLG_TEST(type, PREPARED));
		goto done;
	}

	/* Prepare class first */
	if (!_JC_FLG_TEST(type, PREPARED)
	    && _jc_prepare_type(env, type) != JNI_OK)
		goto fail;

	/* Ensure superclass and superinterfaces are resolved */
	if (type->superclass != NULL
	    && !_JC_FLG_TEST(type->superclass, RESOLVED)
	    && _jc_resolve_type(env, type->superclass) != JNI_OK)
		goto fail;
	for (i = 0; i < type->num_interfaces; i++) {
		_jc_type *const iftype = type->interfaces[i];

		if (!_JC_FLG_TEST(iftype, RESOLVED)
		    && _jc_resolve_type(env, iftype) != JNI_OK)
			goto fail;
	}

	/* Compute vtable and mtable for this class */
	if (!_JC_ACC_TEST(type, INTERFACE))
		_jc_resolve_vtable(env->vm, type);

	/* Resolve type */
	if (_jc_resolve_interp(env, type, &info) != JNI_OK)
	    	goto fail;

	/* Merge in type's implicit references into class loader's list */
	if (_jc_merge_implicit_refs(env, &info) != JNI_OK) {
		_jc_post_exception_info(env);
		goto fail;
	}

	/* Mark type as resolved */
	type->flags |= _JC_TYPE_RESOLVED;

	/* Free temporary supers info */
	_jc_vm_free(&type->u.nonarray.supers);

done:
	_JC_ASSERT(_JC_FLG_TEST(type, LOADED));
	_JC_ASSERT(_JC_FLG_TEST(type, RESOLVED));
	status = JNI_OK;
	goto out;

fail:
	/* Failed */
	status = JNI_ERR;

out:
	/* Unlock class object */
	if (class_locked) {
		jint status2;

		if ((status2 = _jc_unlock_object(env, obj)) != JNI_OK)
			status = status2;
	}

	/* Clean up and return */
	_jc_vm_free(&info.implicit_refs);
	return status;
}

/*
 * Resolve vtable and mtable.
 */
static void
_jc_resolve_vtable(_jc_jvm *vm, _jc_type *type)
{
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	int i;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(type, INTERFACE));

	/*
	 * Copy superclass' vtable and mtable. At this point these tables
	 * are correct except for overridden superclass methods.
	 */
	if (type->superclass != NULL) {
		_jc_type *const stype = type->superclass;
		_jc_nonarray_type *const sntype = &stype->u.nonarray;

		/* Copy superclass vtable */
		memcpy(type->vtable, stype->vtable,
		    sntype->num_vmethods * sizeof(*type->vtable));

		/* Copy superclass mtable (if needed) */
		memcpy(ntype->mtable, sntype->mtable,
		    sntype->num_vmethods * sizeof(*ntype->mtable));
	}

	/* Handle overridden methods */
	for (i = 0; i < ntype->num_methods; i++) {
		_jc_method *const method = ntype->methods[i];
		_jc_type *stype;

		/* Ignore non-virtual methods */
		if (_JC_ACC_TEST(method, STATIC) || *method->name == '<')
			continue;

		/* Override overridden methods */
		_JC_ASSERT(method->function != NULL);
		for (stype = type->superclass;
		    stype != NULL; stype = stype->superclass) {
			_jc_method *orm;

			/* Is this method overridden? */
			if ((orm = _jc_method_lookup(stype, method)) == NULL)
				continue;

			/* Override in vtable */
			type->vtable[orm->vtable_index]
				= method->function;

			/* Override in mtable */
			ntype->mtable[orm->vtable_index] = method;
		}
	}
}

/*
 * Resolve a field per JVMS section 5.4.3.2.
 * Note the static-ness of the field is not checked.
 *
 * Returns NULL and stores a NoSuchFieldError upon failure.
 */
_jc_field *
_jc_resolve_field(_jc_env *env, _jc_type *type,
	const char *name, const char *sig, int is_static)
{
	_jc_field *field;
	int i;

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));

	/* Search for field in class */
	if ((field = _jc_get_declared_field(env,
	    type, name, sig, is_static)) != NULL)
		return field;

	/* Search for field in superinterfaces */
	for (i = 0; i < type->num_interfaces; i++) {
		_jc_type *const itype = type->interfaces[i];

		if ((field = _jc_resolve_field(env,
		    itype, name, sig, is_static)) != NULL)
			return field;
	}

	/* Search for field in superclasses */
	while (JNI_TRUE) {
		if ((type = type->superclass) == NULL)
			break;
		if ((field = _jc_get_declared_field(env,
		    type, name, sig, is_static)) != NULL)
			return field;
	}

	/* Not found */
	return NULL;
}

/*
 * Resolve a method per JVMS section 5.4.3.3 and 5.4.3.4.
 * Note the static-ness of the method is not checked.
 *
 * Returns NULL and stores an exception upon failure.
 */
_jc_method *
_jc_resolve_method(_jc_env *env, _jc_type *type,
	const char *name, const char *sig)
{
	_jc_method *method;
	_jc_type *stype;
	jboolean clinit;
	int i;

	/* Special case for <clinit> (don't recurse) */
	clinit = strcmp(name, "<clinit>") == 0;

	/* Search for method in class and superclasses */
	for (stype = type; stype != NULL; ) {
		if ((method = _jc_get_declared_method(env,
		    stype, name, sig, 0, 0)) != NULL)
			return method;
		if (clinit)
			return NULL;
		stype = stype->superclass;
	}

	/* Search for method in superinterfaces */
	for (i = 0; i < type->num_interfaces; i++) {
		_jc_type *const itype = type->interfaces[i];

		if ((method = _jc_resolve_method(env,
		    itype, name, sig)) != NULL)
			return method;
	}

	/* Not found */
	return NULL;
}

/*
 * Add an implicit reference from one class loader to another.
 *
 * If the two loaders are the same, or the other loader is the boot
 * loader (which is never GC'd), then don't bother adding the reference.
 *
 * If the other loader is already in the list, don't bother adding it.
 *
 * Stores an exception on failure.
 */
jint
_jc_resolve_add_loader_ref(_jc_env *env, _jc_resolve_info *info,
	_jc_class_loader *loader)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *ref;

	/* Avoid unecessary references */
	if (loader == info->loader || loader == vm->boot.loader)
		return JNI_OK;

	/*
	 * Sanity check: the boot loader should never reference other
	 * class loaders directly, because the boot loader has no parent.
	 */
	_JC_ASSERT(info->loader != vm->boot.loader);

	/* Sanity check: non-boot loaders have associated ClassLoader objects */
	_JC_ASSERT(loader->instance != NULL);

	/* Get reference to the ClassLoader object */
	ref = loader->instance;

	/* Add reference to loader instance */
	return _jc_resolve_add_ref(env, info, ref);
}

/*
 * Add an implicit reference from one class loader to an object.
 *
 * Stores an exception on failure.
 */
jint
_jc_resolve_add_ref(_jc_env *env, _jc_resolve_info *info, _jc_object *ref)
{
	int lim;
	int i;

	/* Sanity check */
	if (ref == NULL)
		return JNI_OK;

	/* See if reference already exists in list (via binary search) */
	for (i = 0, lim = info->num_implicit_refs; lim != 0; lim >>= 1) {
		const int j = i + (lim >> 1);

		if (ref == info->implicit_refs[j])
			return JNI_OK;
		if (ref > info->implicit_refs[j]) {
			i = j + 1;
			lim--;
		}
	}

	/* Make room in the array for the new reference */
	if (info->num_implicit_refs == info->num_implicit_alloc) {
		const int new_alloc = info->num_implicit_alloc
		    + _JC_CL_ALLOC_IMPLICIT_REFS;
		_jc_object **new_refs;

		if ((new_refs = _jc_vm_realloc(env, info->implicit_refs,
		    new_alloc * sizeof(*new_refs))) == NULL)
			return JNI_ERR;
		info->implicit_refs = new_refs;
		info->num_implicit_alloc = new_alloc;
	}

	/* Shift higher references over by one */
	memmove(info->implicit_refs + i + 1, info->implicit_refs + i,
	    (info->num_implicit_refs++ - i) * sizeof(*info->implicit_refs));

	/* Insert the new reference in its proper place */
	info->implicit_refs[i] = ref;
	return JNI_OK;
}

