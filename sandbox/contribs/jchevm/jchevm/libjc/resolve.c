
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
 * $Id: resolve.c,v 1.18 2005/05/10 17:21:07 archiecobbs Exp $
 */

#include "libjc.h"

/* Internal functions */
static void		_jc_resolve_vtable(_jc_jvm *vm, _jc_type *type);
static jint		_jc_resolve_exec(_jc_env *env, _jc_type *type,
				_jc_resolve_info *info);
static jint		_jc_resolve_java_symbol(_jc_env *env,
				_jc_resolve_info *info, const char *name,
				Elf_Addr *result, jboolean *found);

static _jc_elf_resolver	_jc_resolve_symbol;

/* Empty interface method lookup tables */
const void		*_jc_empty_quick_table[_JC_IMETHOD_HASHSIZE];
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
	info.type = type;
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

	/* For ELF types, resolve type by resolving all ELF symbols */
	if (!_JC_ACC_TEST(type, INTERP)
	    && _jc_resolve_exec(env, type, &info) != JNI_OK)
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

	/* Compute vtable and mtable for this class (as needed) */
	if (!_JC_ACC_TEST(type, INTERFACE)) {
		_jc_jvm *const vm = env->vm;

		if (!vm->loader_enabled || !vm->generation_enabled)
			_jc_resolve_vtable(vm, type);
	}

	/* Resolve interpreted types */
	if (_JC_ACC_TEST(type, INTERP)
	    && _jc_resolve_interp(env, type, &info) != JNI_OK)
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
 * Resolve vtable and mtable (as needed).
 */
static void
_jc_resolve_vtable(_jc_jvm *vm, _jc_type *type)
{
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	int i;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(type, INTERFACE)
	    && (!vm->loader_enabled || !vm->generation_enabled));

	/*
	 * Copy superclass' vtable and mtable. At this point these tables
	 * are correct except for overridden superclass methods.
	 */
	if (type->superclass != NULL) {
		_jc_type *const stype = type->superclass;
		_jc_nonarray_type *const sntype = &stype->u.nonarray;

		/*
		 * For interpreted types, copy superclass vtable.
		 * For ELF types, the vtable should already be correct.
		 */
		if (_JC_ACC_TEST(type, INTERP)) {
			memcpy(type->vtable, stype->vtable,
			    sntype->num_vmethods * sizeof(*type->vtable));
		}

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
			if (_JC_ACC_TEST(type, INTERP)) {
				type->vtable[orm->vtable_index]
				    = method->function;
			} else {
				_JC_ASSERT(type->vtable[orm->vtable_index]
				    == method->function);
			}

			/* Override in mtable */
			ntype->mtable[orm->vtable_index] = method;
		}
	}
}

/*
 * Resolve a non-interpreted type, i.e., an ELF-loaded type.
 */
static jint
_jc_resolve_exec(_jc_env *env, _jc_type *type, _jc_resolve_info *info)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *const loader = type->loader;
	_jc_elf *const elf = type->u.nonarray.u.elf;
	jint status;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(type, INTERP));

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

retry:
	/* Is this type defined in an ELF file that's already resolved? */
	if (elf->info == NULL) {
		_JC_MUTEX_UNLOCK(env, loader->mutex);
		return JNI_OK;
	}

	/* Is another thread currently resolving this ELF object? */
	if (elf->info->resolver != NULL) {

		/* If recursively resolving, something wierd is happening */
		if (elf->info->resolver == env) {
			_JC_MUTEX_UNLOCK(env, loader->mutex);
			_jc_post_exception_msg(env, _JC_LinkageError,
			    "recursively resolving `%s'", elf->pathname);
			return JNI_ERR;
		}

		/* Wait for the other thread to finish */
		_jc_loader_wait(env, loader);
		goto retry;
	}

	/* Mark this thread as resolving */
	elf->info->resolver = env;

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Verbosity */
	if (loader == vm->boot.loader) {
		VERBOSE(RESOLUTION, vm,
		    "resolving `%s' (via bootstrap loader)", type->name);
	} else {
		VERBOSE(RESOLUTION, vm,
		    "resolving `%s' (via %s@%p)", type->name,
		    loader->instance->type->name, loader->instance);
	}

	/*
	 * Resolve all external symbols in the ELF image. As we resolve
	 * symbols that refer to other types, update the list of implicit
	 * references from this type to other types.
	 */
	status = _jc_elf_link(env, elf, _jc_resolve_symbol, info);

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Mark this thread as no longer resolving */
	_JC_ASSERT(elf->info->resolver == env);
	elf->info->resolver = NULL;

	/* Free ELF linking info; this signals that the ELF file is resolved */
	if (status == JNI_OK) {
		_jc_elf_link_cleanup(elf);
		_JC_ASSERT(elf->info == NULL);
	}

	/* Wake up other threads waiting on us */
	if (loader->waiters) {
		loader->waiters = JNI_FALSE;
		_JC_COND_BROADCAST(loader->cond);
	}

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Done */
	return status;
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
		_jc_type *itype;

		itype = (!_JC_FLG_TEST(type, RESOLVED)
		      && !_JC_ACC_TEST(type, INTERP)) ?
		    type->u.nonarray.supers->interfaces[i] :
		    type->interfaces[i];
		if ((field = _jc_resolve_field(env,
		    itype, name, sig, is_static)) != NULL)
			return field;
	}

	/* Search for field in superclasses */
	while (JNI_TRUE) {
		type = (!_JC_FLG_TEST(type, RESOLVED)
		      && !_JC_ACC_TEST(type, INTERP)) ?
		    type->u.nonarray.supers->superclass : type->superclass;
		if (type == NULL)
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
		stype = (!_JC_FLG_TEST(stype, RESOLVED)
		      && !_JC_ACC_TEST(stype, INTERP)) ?
		    stype->u.nonarray.supers->superclass : stype->superclass;
	}

	/* Search for method in superinterfaces */
	for (i = 0; i < type->num_interfaces; i++) {
		_jc_type *itype;

		itype = (!_JC_FLG_TEST(type, RESOLVED)
		      && !_JC_ACC_TEST(type, INTERP)) ?
		    type->u.nonarray.supers->interfaces[i] :
		    type->interfaces[i];
		if ((method = _jc_resolve_method(env,
		    itype, name, sig)) != NULL)
			return method;
	}

	/* Not found */
	return NULL;
}

/*
 * Resolve an unresolved external reference in an ELF file.
 *
 * This function is used as a callback from the ELF linker code.
 */
static jint
_jc_resolve_symbol(_jc_env *env, void *arg, const char *name, Elf_Addr *result)
{
	_jc_jvm *const vm = env->vm;
	_jc_resolve_info *const info = arg;
	_jc_native_lib *lib;
	const void *func;
	jboolean found;

	/* If not a symbol we know about? */
	if (strncmp(name, "_jc_", 4) != 0)
		goto search_external;

	/* Handle "_jc_check_address" */
	if (strcmp(name, "_jc_check_address") == 0) {
		*result = (Elf_Addr)vm->check_address;
		return JNI_OK;
	}

	/* Handle empty interface method tables */
	if (strcmp(name, "_jc_empty_quick_table") == 0) {
		*result = (Elf_Addr)_jc_empty_quick_table;
		return JNI_OK;
	}
	if (strcmp(name, "_jc_empty_imethod_table") == 0) {
		*result = (Elf_Addr)_jc_empty_imethod_table;
		return JNI_OK;
	}

	/* Search C support functions, which all have "_jc_cs_" prefix */
	if (strncmp(name, "_jc_cs_", 7) == 0) {
		if ((func = dlsym(NULL, name)) != NULL) {
			*result = (Elf_Addr)func;
			return JNI_OK;
		}
	}

	/* Try to parse a Java type, method, etc. symbol */
	if (_jc_resolve_java_symbol(env, info, name, result, &found) != JNI_OK)
		return JNI_ERR;
	if (found)
		return JNI_OK;

	/*
	 * If symbol started with "_jc_" but was not found, then
	 * generate an error. Alternately, we could try to search
	 * for it in external libraries, but that is likely to not
	 * be correct and means a less clear namespace separation.
	 */
	goto not_found;

search_external:
	/*
	 * If we get here, the symbol is not a Java symbol.
	 * Search for it first in the shared libraries associated with
	 * the class loader, then in the shared C libraries that are
	 * loaded on behalf of this process by the normal runtime linker.
	 */

	/* Search the loader's shared libraries */
	STAILQ_FOREACH(lib, &info->loader->native_libs, link) {
		if ((func = dlsym(lib->handle, name)) != NULL) {
			*result = (Elf_Addr)func;
			return JNI_OK;
		}
	}

	/* Search normally loaded shared libraries */
	if ((func = dlsym(RTLD_DEFAULT, name)) != NULL) {
		*result = (Elf_Addr)func;
		return JNI_OK;
	}

#ifdef __linux__
    {
    	static void *kludge;

	/* Stupid Linux. dlsym() won't retrieve GCC helper functions */
	if ((kludge != NULL
	      || (kludge = dlopen("/lib/libgcc_s.so.1", RTLD_NOW)) != NULL)
	    && (func = dlsym(kludge, name)) != NULL) {
		*result = (Elf_Addr)func;
		return JNI_OK;
	}
    }
#endif

not_found:
	/* Symbol not found */
	_jc_post_exception_msg(env, _JC_LinkageError,
	    "can't resolve symbol `%s' in ELF object `%s'",
	    name, info->type->u.nonarray.u.elf->pathname);
	return JNI_ERR;
}

/*
 * Resolve a symbol that refers to a Java type, method, etc.
 *
 * Sets *found to true or false depending on the outcome.
 * If not found, *result will not be modified and JNI_OK
 * is returned.
 *
 * An error is returned only if an exception is thrown.
 */
static jint
_jc_resolve_java_symbol(_jc_env *env, _jc_resolve_info *info,
	const char *name, Elf_Addr *result, jboolean *found)
{
	_jc_jvm *const vm = env->vm;
	int ptype = _JC_TYPE_INVALID;
	char *class_name;
	const char *s;
	_jc_type *type;
	int dims = 0;
	char *p;
	int i;

	/* Find dollar sign */
	if (strncmp(name, "_jc_", 4) != 0)
		goto unknown;
	name += 4;
	if ((s = strchr(name, '$')) == NULL)
		goto unknown;
	s++;

	/* Check for an array type and get dimensions if so */
	if (strncmp(s, "array", 5) == 0) {
		s += 5;
		for (i = 0; i < 3; i++) {
			if (!isdigit(s[i]))
				break;
			if (dims == 0 && s[i] == '0')
				break;
			dims = dims * 10 + (s[i] - '0');
			if (dims < 0 || dims > 255)
				goto unknown;
		}
		if (dims == 0)		/* "$array$x" abbreviates "$array1$x" */
			dims = 1;
		if (s[i] != '$')
			goto unknown;
		s += i + 1;
	}

	/* Check for a primitive base type */
	if (strncmp(s, "prim$", 5) == 0) {
		s += 5;				/* advance past the "prim$" */
		switch (*name) {
		case 'b':			/* boolean or byte */
			if (strncmp(name, "boolean$", 8) == 0)
				ptype = _JC_TYPE_BOOLEAN;
			else if (strncmp(name, "byte$", 5) == 0)
				ptype = _JC_TYPE_BYTE;
			break;
		case 'c':			/* char */
			if (strncmp(name, "char$", 5) == 0)
				ptype = _JC_TYPE_CHAR;
			break;
		case 's':			/* short */
			if (strncmp(name, "short$", 6) == 0)
				ptype = _JC_TYPE_SHORT;
			break;
		case 'i':			/* int */
			if (strncmp(name, "int$", 4) == 0)
				ptype = _JC_TYPE_INT;
			break;
		case 'l':			/* long */
			if (strncmp(name, "long$", 5) == 0)
				ptype = _JC_TYPE_LONG;
			break;
		case 'f':			/* float */
			if (strncmp(name, "float$", 6) == 0)
				ptype = _JC_TYPE_FLOAT;
			break;
		case 'd':			/* double */
			if (strncmp(name, "double$", 7) == 0)
				ptype = _JC_TYPE_DOUBLE;
			break;
		case 'v':			/* void */
			if (strncmp(name, "void$", 5) == 0)
				ptype = _JC_TYPE_VOID;
			break;
		default:
			break;
		}

		/* Bail out if none matched */
		if (ptype == _JC_TYPE_INVALID)
			goto unknown;

		/* Handle zero and one dimensional types directly */
		switch (dims) {
		case 0:
			type = vm->boot.types.prim[ptype];
			goto got_type;
		case 1:
			if (ptype == _JC_TYPE_VOID)
				goto unknown;		/* no void[] type */
			type = vm->boot.types.prim_array[ptype];
			goto got_type;
		default:
			break;
		}

		/* Generate multi-dimensional array type name */
		if ((class_name = _JC_STACK_ALLOC(env, dims + 2)) == NULL) {
			_jc_post_exception_info(env);
			return JNI_ERR;
		}
		memset(class_name, '[', dims);
		class_name[dims] = _jc_prim_chars[ptype];
		class_name[dims + 1] = '\0';
		goto resolve_type;
	}

	/* Generate (possibly array) type name */
	if ((class_name = _JC_STACK_ALLOC(env,
	    dims + (s - name) + 3)) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}
	p = class_name;
	if (dims > 0) {
		memset(p, '[', dims);
		p += dims;
		*p++ = 'L';
	}
	if ((p = _jc_name_decode(name, p)) == NULL)
		goto unknown;
	if (dims > 0)
		*p++ = ';';
	*p = '\0';

resolve_type:
	/*
	 * Resolve the type. If we can't, but "jc.ignore.resolution.failures"
	 * is set to true, then ignore the failure and just return zero.
	 */
	if ((type = _jc_load_type(env, info->loader, class_name)) == NULL) {
		if (vm->ignore_resolution_failures
		    && _jc_unpost_exception(env, _JC_LinkageError)) {
			*result = (Elf_Addr)0;
			goto found;
		}
		return JNI_ERR;
	}

got_type:
	/* Sanity check */
	_JC_ASSERT(type->instance != NULL);

	/* Add a reference to the resolved type's class loader */
	if (_jc_resolve_add_loader_ref(env, info, type->loader) != JNI_OK) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Is just the type needed? */
	if (strcmp(s, "type") == 0) {
		*result = (Elf_Addr)type;
		goto found;
	}

	/* Is the type's Class instance needed? */
	if (strcmp(s, "class_object") == 0) {
		*result = (Elf_Addr)type->instance;
		_JC_ASSERT(*result != 0);
		goto found;
	}

	/* Is the type's static fields structure needed? */
	if (strcmp(s, "class_fields") == 0) {
		if (_JC_FLG_TEST(type, ARRAY)
		    || type->u.nonarray.class_fields == NULL)
			goto unknown;
		*result = (Elf_Addr)type->u.nonarray.class_fields;
		_JC_ASSERT(*result != 0);
		goto found;
	}

	/* Is one of the type's methods or method info structures needed? */
	if (strncmp(s, "method$", 7) == 0
	    || strncmp(s, "method_info$", 12) == 0) {
		const int is_info = (s[6] == '_');
		size_t name_len;
		jlong sig_hash;
		char *buf;

		/* Sanity check */
		if (_JC_FLG_TEST(type, ARRAY))
			goto unknown;

		/* Parse out and decode method name */
		name = s + (is_info ? 12 : 7);
		if ((s = strchr(name, '$')) == NULL)
			goto unknown;
		name_len = s - name;
		s++;
		if (strncmp(name, "_init", 5) == 0)
			name = "<init>";
		else if (strncmp(name, "_clinit", 7) == 0)
			name = "<clinit>";
		else {
			if ((buf = _JC_STACK_ALLOC(env,
			    name_len + 1)) == NULL) {
				_jc_post_exception_info(env);
				return JNI_ERR;
			}
			if (_jc_name_decode(name, buf) == NULL)
				goto unknown;
			name = buf;
		}

		/* Parse out method signature hash */
		for (sig_hash = 0, i = 0; i < 16 && s[i] != '\0'; i++) {
			if (!isxdigit(s[i]))
				goto unknown;
			sig_hash = (sig_hash << 4) | _JC_HEXVAL(s[i]);
		}

		/* Intpreted types must be resolved here */
		if (_JC_ACC_TEST(type, INTERP)
		    && _jc_resolve_type(env, type) != JNI_OK)
			return JNI_ERR;

		/* Search for method */
		for (i = 0; i < type->u.nonarray.num_methods; i++) {
			_jc_method *const method = type->u.nonarray.methods[i];

			/* Is this the method? */
			if (method->signature_hash != sig_hash
			    || strcmp(method->name, name) != 0)
				continue;

			/* Return method info if desired */
			if (is_info) {
				*result = (Elf_Addr)method;
				_JC_ASSERT(*result != 0);
				goto found;
			}

			/*
			 * If method is native, JCNI, and resolved,
			 * optimize by linking directly to it if enabled.
			 */
			if (_JC_ACC_TEST(method, NATIVE)
			    && vm->resolve_native_directly) {

				/* Resolve native method if not already */
				if (method->native_function == NULL) {
					if (_jc_resolve_native_method(env,
					      method) != JNI_OK
					    && !_jc_unpost_exception(env,
					      _JC_UnsatisfiedLinkError))
						return JNI_ERR;
				}

				/* Method must be resolved and a JCNI method */
				if (method->native_function != NULL
				    && _JC_ACC_TEST(method, JCNI)) {
					*result = (Elf_Addr)
					    method->native_function;
					_JC_ASSERT(*result != 0);
					goto found;
				}
			}

			/* Resolve normally to method's C function */
			*result = (Elf_Addr)method->function;
			_JC_ASSERT(*result != 0);
			goto found;
		}

		/* Not found */
		goto unknown;
	}

unknown:
	/* Not a recognized symbol */
	*found = JNI_FALSE;
	return JNI_OK;

found:
	/* Found it */
	*found = JNI_TRUE;
	return JNI_OK;
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

