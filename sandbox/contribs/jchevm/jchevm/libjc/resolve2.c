
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
static jint		_jc_resolve_fields(_jc_env *env, _jc_type *type,
				_jc_resolve_info *info);
static jint		_jc_resolve_methods(_jc_env *env, _jc_type *type,
				_jc_resolve_info *info);
static jint		_jc_resolve_bytecode(_jc_env *env,
				_jc_method *const method,
				_jc_cf_bytecode *bytecode,
				_jc_resolve_info *rinfo);
static jint		_jc_derive_imethod_tables(_jc_env *env, _jc_type *type);
static jint		_jc_derive_instanceof_table(_jc_env *env,
				_jc_type *type);
static jint		_jc_resolve_inner_classes(_jc_env *env, _jc_type *type,
				_jc_resolve_info *info);
static int		_jc_add_iface_methods(_jc_type *type,
				_jc_method **methods);
static int		_jc_method_sorter(const void *item1, const void *item2);
static int		_jc_type_sorter(const void *item1, const void *item2);

/*
 * Resolve an interpreted class.
 *
 * If unsuccessful, an exception is posted.
 */
jint
_jc_resolve_interp(_jc_env *env, _jc_type *type, _jc_resolve_info *info)
{
	_jc_jvm *const vm = env->vm;
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	_jc_classfile *const cfile = ntype->cfile;

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));

	/* Verbosity */
	if (type->loader == vm->boot.loader) {
		VERBOSE(RESOLUTION, vm,
		    "resolving `%s' (via bootstrap loader)", type->name);
	} else {
		VERBOSE(RESOLUTION, vm,
		    "resolving `%s' (via %s@%p)", type->name,
		    type->loader->instance->type->name, type->loader->instance);
	}

	/* Copy source filename */
	_JC_ASSERT(ntype->source_file == NULL);
	if (cfile->source_file != NULL) {
		_JC_MUTEX_LOCK(env, type->loader->mutex);
		if ((ntype->source_file = _jc_cl_strdup(env,
		    type->loader, cfile->source_file)) == NULL) {
			_JC_MUTEX_UNLOCK(env, type->loader->mutex);
			_jc_post_exception_info(env);
			goto fail;
		}
		_JC_MUTEX_UNLOCK(env, type->loader->mutex);
	}

	/* Resolve fields */
	if (_jc_resolve_fields(env, type, info) != JNI_OK)
		return JNI_ERR;

	/* Resolve methods */
	if (_jc_resolve_methods(env, type, info) != JNI_OK)
		return JNI_ERR;

	/* Resolve inner classes */
	if (_jc_resolve_inner_classes(env, type, info) != JNI_OK)
		return JNI_ERR;

	/* Build interface method hash tables */
	if (!_JC_ACC_TEST(type, INTERFACE)
	    && !_JC_ACC_TEST(type, ABSTRACT)
	    && _jc_derive_imethod_tables(env, type) != JNI_OK)
		goto fail;

	/* Build instanceof hash table */
	if (_jc_derive_instanceof_table(env, type) != JNI_OK)
		goto fail;

	/* We don't need the class file anymore */
	_jc_destroy_classfile(&type->u.nonarray.cfile);

	/* Done */
	return JNI_OK;

fail:
	/* Clean up after failure */
	_JC_MUTEX_LOCK(env, type->loader->mutex);
	if (ntype->source_file != NULL) {
		_jc_cl_unalloc(type->loader,
		    &ntype->source_file, strlen(ntype->source_file));
	}
	_JC_MUTEX_UNLOCK(env, type->loader->mutex);
	return JNI_ERR;
}

/*
 * Resolve fields.
 *
 * If unsuccessful, an exception is posted.
 */
static jint
_jc_resolve_fields(_jc_env *env, _jc_type *type, _jc_resolve_info *info)
{
	_jc_jvm *const vm = env->vm;
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	_jc_classfile *const cfile = ntype->cfile;
	size_t initial_values_size;
	void *initial_values;
	char *ptr;
	int i;

	/* Resolve each field's type and count static initializers */
	initial_values_size = 0;
	for (i = 0; i < ntype->num_fields; i++) {
		_jc_cf_field *const cfield = &cfile->fields[i];
		const u_char ptype = _jc_sig_types[(u_char)*cfield->descriptor];
		_jc_field *const field = ntype->fields[i];

		/* Resolve type and add up initializer sizes */
		switch (ptype) {
		case _JC_TYPE_BOOLEAN:
		case _JC_TYPE_BYTE:
		case _JC_TYPE_CHAR:
		case _JC_TYPE_SHORT:
		case _JC_TYPE_INT:
		case _JC_TYPE_LONG:
		case _JC_TYPE_FLOAT:
		case _JC_TYPE_DOUBLE:
			field->type = vm->boot.types.prim[ptype];
			if (cfield->initial_value != NULL)
				initial_values_size += _jc_type_sizes[ptype];
			break;
		case _JC_TYPE_REFERENCE:
		    {
			_jc_class_loader *const loader = type->loader;
			const char *desc = cfield->descriptor;
			_jc_class_ref ref;
			const char *end;
			u_char btype;

			/* Parse field's type */
			end = _jc_parse_class_ref(desc, &ref, 0, &btype);
			if (btype == _JC_TYPE_INVALID
			    || (*desc != '[' && *(end - 1) != ';'))
				goto invalid_signature;

			/* Resolve field's type */
			field->type = (*desc == '[') ?
			    _jc_load_type2(env, loader, desc, end - desc) :
			    _jc_load_type2(env, loader,
			      desc + 1, end - desc - 2);
			if (field->type == NULL)
				return JNI_ERR;
			if (_jc_resolve_add_loader_ref(env, info,
			    field->type->loader) != JNI_OK) {
				_jc_post_exception_info(env);
				return JNI_ERR;
			}

			/* Add initial value size */
			if (cfield->initial_value != NULL) {
				_JC_ASSERT(field->type
				    == vm->boot.types.String);
				initial_values_size +=
				    strlen(cfield->initial_value->u.Utf8) + 1;
			}
			break;
		    }
		default:
invalid_signature:
			_jc_post_exception_msg(env, _JC_ClassFormatError,
			    "invalid descriptor `%s' for field `%s.%s'",
			    cfield->descriptor, cfile->name, cfield->name);
			return JNI_ERR;
		}
	}

	/* Any initial values? */
	if (initial_values_size == 0)
		goto skip_initial_values;

	/* Allocate storage for initial values */
	_JC_MUTEX_LOCK(env, type->loader->mutex);
	if ((initial_values = _jc_cl_zalloc(env,
	    type->loader, initial_values_size)) == NULL) {
		_JC_MUTEX_UNLOCK(env, type->loader->mutex);
		_jc_post_exception_info(env);
		goto fail;
	}
	_JC_MUTEX_UNLOCK(env, type->loader->mutex);

	/* Copy initial values from classfile */
	ptr = initial_values;
	for (i = 0; i < ntype->num_fields; i++) {
		_jc_cf_field *const cfield = &cfile->fields[i];
		_jc_field *const field = ntype->fields[i];
		_jc_value prim_value;
		u_char ptype;

		/* Does this field have a static initializer? */
		if (cfield->initial_value == NULL)
			continue;
		field->initial_value = ptr;
		ptype = _jc_sig_types[(u_char)*field->signature];

		/* Handle strings */
		if (ptype == _JC_TYPE_REFERENCE) {
			const char *const utf8 = cfield->initial_value->u.Utf8;
			const size_t slen = strlen(utf8) + 1;

			memcpy(field->initial_value, utf8, slen);
			ptr += slen;
			continue;
		}

		/* Handle primitives */
		switch (ptype) {
		case _JC_TYPE_BOOLEAN:
			prim_value.z = !!cfield->initial_value->u.Integer;
			break;
		case _JC_TYPE_BYTE:
			prim_value.b = cfield->initial_value->u.Integer;
			break;
		case _JC_TYPE_CHAR:
			prim_value.c = cfield->initial_value->u.Integer;
			break;
		case _JC_TYPE_SHORT:
			prim_value.s = cfield->initial_value->u.Integer;
			break;
		case _JC_TYPE_INT:
			prim_value.i = cfield->initial_value->u.Integer;
			break;
		case _JC_TYPE_LONG:
			prim_value.j = cfield->initial_value->u.Long;
			break;
		case _JC_TYPE_FLOAT:
			prim_value.f = cfield->initial_value->u.Float;
			break;
		case _JC_TYPE_DOUBLE:
			prim_value.d = cfield->initial_value->u.Double;
			break;
		default:
			_JC_ASSERT(JNI_FALSE);
		}
		memcpy(field->initial_value,
		    &prim_value, _jc_type_sizes[ptype]);
		ptr += _jc_type_sizes[ptype];
	}

skip_initial_values:
	/* Done */
	return JNI_OK;

fail:
	/* Clean up after failure */
	_JC_MUTEX_LOCK(env, type->loader->mutex);
	_jc_cl_unalloc(type->loader, &initial_values, initial_values_size);
	_JC_MUTEX_UNLOCK(env, type->loader->mutex);
	return JNI_ERR;
}

/*
 * Resolve methods.
 *
 * If unsuccessful, an exception is posted.
 */
static jint
_jc_resolve_methods(_jc_env *env, _jc_type *const type, _jc_resolve_info *info)
{
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	_jc_class_loader *const loader = type->loader;
	_jc_classfile *const cfile = ntype->cfile;
	int i;

	/* Resolve method signature info */
	for (i = 0; i < ntype->num_methods; i++) {
		_jc_cf_method *const cmethod = &cfile->methods[i];
		_jc_method *const method = ntype->methods[i];
		int signature_hash = 0;
		const char *s;
		int j;

		/* Compute method's signature hash */
		for (s = method->name; *s != '\0'; s++)
			signature_hash = (signature_hash * 31) + (u_char)*s;
		for (s = method->signature; *s != '\0'; s++)
			signature_hash = (signature_hash * 31) + (u_char)*s;
		method->signature_hash_bucket = signature_hash
		    & (_JC_IMETHOD_HASHSIZE - 1);

		/* Compute the number of parameters */
		if ((j = _jc_resolve_signature(env, method, NULL)) == -1) {
			_jc_post_exception_info(env);
			return JNI_ERR;
		}

		/* Initialize easy stuff */
		method->num_parameters = j;
		method->num_exceptions = cmethod->exceptions != NULL ?
		    cmethod->exceptions->num_exceptions : 0;

		/* Resolve method parameter types */
		if (_jc_resolve_signature(env, method, info) == -1)
			return JNI_ERR;

		/* Determine parameter count with long/double counted twice */
		_JC_ASSERT(method->code.num_params2 == 0);
		method->code.num_params2 = method->num_parameters;
		for (j = 0; j < method->num_parameters; j++) {
			if (_jc_dword_type[method->param_ptypes[j]])
				method->code.num_params2++;
		}

		/* Resolve exception types */
		for (j = 0; j < method->num_exceptions; j++) {
			if ((method->exceptions[j] = _jc_load_type(env, loader,
			    cmethod->exceptions->exceptions[j])) == NULL)
				return JNI_ERR;
			if (_jc_resolve_add_loader_ref(env, info,
			    method->exceptions[j]->loader) != JNI_OK) {
				_jc_post_exception_info(env);
				return JNI_ERR;
			}
		}
	}

	/* Resolve bytecode for non-abstract methods */
	for (i = 0; i < ntype->num_methods; i++) {
		_jc_cf_method *const cmethod = &cfile->methods[i];
		_jc_method *const method = ntype->methods[i];

		if (_JC_ACC_TEST(method, ABSTRACT)
		    || _JC_ACC_TEST(method, NATIVE))
			continue;
		if (_jc_resolve_bytecode(env,
		    method, cmethod->code, info) != JNI_OK)
			return JNI_ERR;
	}

	/* Skip remaining stuff for interfaces */
	if (_JC_ACC_TEST(type, INTERFACE))
		goto done;

done:
	/* Done */
	return JNI_OK;
}

/*
 * Parse/resolve method signature.
 *
 * Returns the number of parameters and fills in method->param_ptypes;
 * if info != NULL, also resolves and fills in method->param_types and
 * method->return_type.
 *
 * Returns -1 and stores an exception (if info == NULL) or posts an
 * exception (if info != NULL) on failure.
 */
int
_jc_resolve_signature(_jc_env *env, _jc_method *method, _jc_resolve_info *info)
{
	_jc_jvm *const vm = env->vm;
	const jboolean resolve = info != NULL;
	int done = 0;
	const char *s;
	int i;

	/* Sanity check signature */
	if (*method->signature != '(')
		goto invalid;

	/* Parse parameters */
	for (i = 0, s = method->signature + 1; !done; i++) {
		_jc_type *type = NULL;
		int ptype;

		/* Handle return value */
		if (*s == ')') {
			done = 1;
			s++;
		}

		/* Parse type */
		ptype = _jc_sig_types[(u_char)*s];
		switch (ptype) {
		case _JC_TYPE_VOID:
			if (!done)
				goto invalid;
			/* FALLTHROUGH */
		case _JC_TYPE_BOOLEAN:
		case _JC_TYPE_BYTE:
		case _JC_TYPE_CHAR:
		case _JC_TYPE_SHORT:
		case _JC_TYPE_INT:
		case _JC_TYPE_LONG:
		case _JC_TYPE_FLOAT:
		case _JC_TYPE_DOUBLE:
			if (resolve)
				type = vm->boot.types.prim[ptype];
			s++;
			break;
		case _JC_TYPE_REFERENCE:
		    {
			_jc_class_loader *const loader = method->class->loader;
			_jc_class_ref ref;
			const char *end;
			u_char btype;

			end = _jc_parse_class_ref(s, &ref, 0, &btype);
			if (btype == _JC_TYPE_INVALID
			    || (*s != '[' && *(end - 1) != ';'))
				goto invalid;
			if (resolve) {
				type = (*s == '[') ?
				    _jc_load_type2(env, loader, s, end - s) :
				    _jc_load_type2(env, loader,
					s + 1, end - s - 2);
				if (type == NULL)
					return -1;	/* exception posted */
			}
			s = end;
			break;
		    }
		default:
			goto invalid;
		}

		/* Set param (or return value) type */
		if (resolve) {
			if (!done)
				method->param_types[i] = type;
			else
				method->return_type = type;
			if (_jc_resolve_add_loader_ref(env,
			    info, type->loader) != JNI_OK)
				goto fail;
		}

		/* Set param (or return value) ptype */
		if (method->param_ptypes != NULL)
			method->param_ptypes[i] = ptype;
	}

	/* Trailing garbage? */
	if (*s != '\0')
		goto invalid;

	/* Done */
	return i - 1;

invalid:
	/* Method signature is invalid */
	_JC_EX_STORE(env, ClassFormatError,
	    "invalid signature `%s' for %s.%s", method->signature,
	    method->class->name, method->name);

fail:
	/* Post exception if resolving and return failure */
	if (resolve)
		_jc_post_exception_info(env);
	return -1;
}

/*
 * Parse and resolve method bytecode.
 *
 * Posts an exception upon failure.
 */
static jint
_jc_resolve_bytecode(_jc_env *env, _jc_method *const method,
	_jc_cf_bytecode *bytecode, _jc_resolve_info *rinfo)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *const loader = method->class->loader;
	_jc_classfile *const cfile = method->class->u.nonarray.cfile;
	_jc_method_code *const interp = &method->code;
	_jc_cf_code code_mem;
	_jc_cf_code *const code = &code_mem;
	jboolean mutex_locked;
	int i;

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(method->class, RESOLVED));

	/* Parse bytecode */
	if (_jc_parse_code(env, cfile, bytecode, code) != JNI_OK) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Allocate resolved method info */
	_JC_MUTEX_LOCK(env, loader->mutex);
	mutex_locked = JNI_TRUE;
	if ((interp->insns = _jc_cl_alloc(env, loader,
	    code->num_insns * sizeof(*interp->insns))) == NULL)
		goto post_fail;
	if ((interp->traps = _jc_cl_alloc(env, loader,
	    code->num_traps * sizeof(*interp->traps))) == NULL)
		goto post_fail;
	if ((interp->linemaps = _jc_cl_alloc(env, loader,
	    code->num_linemaps * sizeof(*interp->linemaps))) == NULL)
		goto post_fail;
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	mutex_locked = JNI_FALSE;

	/* Initialize */
	interp->max_stack = code->max_stack;
	interp->max_locals = code->max_locals;
	interp->num_insns = code->num_insns;
	interp->num_traps = code->num_traps;
	interp->num_linemaps = code->num_linemaps;

	/* Copy line number table */
	for (i = 0; i < interp->num_linemaps; i++) {
		_jc_cf_linemap *const clinemap = &code->linemaps[i];
		_jc_linemap *const linemap = &interp->linemaps[i];

		linemap->index = clinemap->index;
		linemap->line = clinemap->line;
	}

	/* Resolve and copy trap info */
	for (i = 0; i < code->num_traps; i++) {
		_jc_cf_trap *const ptrap = &code->traps[i];
		_jc_interp_trap *const itrap = &interp->traps[i];

		itrap->start = &interp->insns[ptrap->start];
		itrap->end = &interp->insns[ptrap->end];
		itrap->target = &interp->insns[ptrap->target];
		if (ptrap->type == NULL) {
			itrap->type = NULL;
			continue;
		}
		if ((itrap->type = _jc_load_type(env,
		    loader, ptrap->type)) == NULL)
			goto fail;
		if (_jc_resolve_add_loader_ref(env,
		    rinfo, itrap->type->loader) != JNI_OK)
			goto post_fail;
	}

	/* Resolve and copy instructions */
	for (i = 0; i < code->num_insns; i++) {
		_jc_cf_insn *const cinsn = &code->insns[i];
		_jc_insn *const insn = &interp->insns[i];
		_jc_insn_info *const info = &insn->info;
		u_char opcode;

		/* Get opcode */
		opcode = cinsn->opcode;

		/* Copy and resolve additional info, possibly changing opcode */
		switch (opcode) {
		case _JC_aload:
		case _JC_astore:
		case _JC_dload:
		case _JC_dstore:
		case _JC_fload:
		case _JC_fstore:
		case _JC_iload:
		case _JC_istore:
		case _JC_lload:
		case _JC_lstore:
		case _JC_ret:
			info->local = cinsn->u.local.index;
			break;
		case _JC_anewarray:
		    {
			const char *etype = cinsn->u.type.name;
			const size_t elen = strlen(etype) + 1;
			char *atype;

			if ((atype = _jc_vm_alloc(env, elen + 3)) == NULL)
				goto post_fail;
			atype[0] = '[';
			if (*etype == '[')
				memcpy(atype + 1, etype, elen);
			else {
				atype[1] = 'L';
				memcpy(atype + 2, etype, elen);
				atype[elen + 1] = ';';
				atype[elen + 2] = '\0';
			}
			if ((info->type = _jc_load_type(env,
			    loader, atype)) == NULL) {
				_jc_vm_free(&atype);
				goto fail;
			}
			_jc_vm_free(&atype);
			if (_jc_resolve_add_loader_ref(env,
			    rinfo, info->type->loader) != JNI_OK)
				goto post_fail;
			break;
		    }
		case _JC_checkcast:
		case _JC_instanceof:
		case _JC_new:
			if ((info->type = _jc_load_type(env,
			    loader, cinsn->u.type.name)) == NULL)
				goto fail;
			if (_jc_resolve_add_loader_ref(env,
			    rinfo, info->type->loader) != JNI_OK)
				goto post_fail;
			break;
		case _JC_bipush:
		case _JC_sipush:
			info->constant.i = cinsn->u.immediate.value;
			opcode = _JC_ldc;
			break;
		case _JC_getfield:
		case _JC_getstatic:
		case _JC_putfield:
		case _JC_putstatic:
		    {
			const int is_static = opcode == _JC_getstatic
			    || opcode == _JC_putstatic;
			_jc_cf_ref *const ref = cinsn->u.fieldref.field;
			_jc_field *field;
			_jc_type *type;
			u_char ptype;

			/* Resolve field's class */
			if ((type = _jc_load_type(env,
			    loader, ref->class)) == NULL)
				goto fail;
			if (_jc_resolve_add_loader_ref(env,
			    rinfo, type->loader) != JNI_OK)
				goto post_fail;

			/* Resolve field */
			if ((field = _jc_resolve_field(env, type,
			    ref->name, ref->descriptor, is_static)) == NULL) {
				env->ex.num = _JC_IncompatibleClassChangeError;
				goto post_fail;
			}

			/* Compute field offset/location */
			info->field.field = field;
			ptype = _jc_sig_types[(u_char)*field->signature];
			switch (opcode) {
			case _JC_getfield:
				info->field.u.offset = field->offset;
				switch (ptype) {
				case _JC_TYPE_BOOLEAN:
					opcode = _JC_getfield_z;
					break;
				case _JC_TYPE_BYTE:
					opcode = _JC_getfield_b;
					break;
				case _JC_TYPE_CHAR:
					opcode = _JC_getfield_c;
					break;
				case _JC_TYPE_SHORT:
					opcode = _JC_getfield_s;
					break;
				case _JC_TYPE_INT:
					opcode = _JC_getfield_i;
					break;
				case _JC_TYPE_LONG:
					opcode = _JC_getfield_j;
					break;
				case _JC_TYPE_FLOAT:
					opcode = _JC_getfield_f;
					break;
				case _JC_TYPE_DOUBLE:
					opcode = _JC_getfield_d;
					break;
				case _JC_TYPE_REFERENCE:
					opcode = _JC_getfield_l;
					break;
				default:
					_JC_ASSERT(JNI_FALSE);
				}
				break;
			case _JC_putfield:
				info->field.u.offset = field->offset;
				switch (ptype) {
				case _JC_TYPE_BOOLEAN:
					opcode = _JC_putfield_z;
					break;
				case _JC_TYPE_BYTE:
					opcode = _JC_putfield_b;
					break;
				case _JC_TYPE_CHAR:
					opcode = _JC_putfield_c;
					break;
				case _JC_TYPE_SHORT:
					opcode = _JC_putfield_s;
					break;
				case _JC_TYPE_INT:
					opcode = _JC_putfield_i;
					break;
				case _JC_TYPE_LONG:
					opcode = _JC_putfield_j;
					break;
				case _JC_TYPE_FLOAT:
					opcode = _JC_putfield_f;
					break;
				case _JC_TYPE_DOUBLE:
					opcode = _JC_putfield_d;
					break;
				case _JC_TYPE_REFERENCE:
					opcode = _JC_putfield_l;
					break;
				default:
					_JC_ASSERT(JNI_FALSE);
				}
				break;
			case _JC_getstatic:
			case _JC_putstatic:
				info->field.u.data = (char *)field->class->
				    u.nonarray.class_fields + field->offset;
				break;
			default:
				_JC_ASSERT(JNI_FALSE);
			}

			/* Done */
			break;
		    }
		case _JC_goto:
		case _JC_if_acmpeq:
		case _JC_if_acmpne:
		case _JC_if_icmpeq:
		case _JC_if_icmpne:
		case _JC_if_icmplt:
		case _JC_if_icmpge:
		case _JC_if_icmpgt:
		case _JC_if_icmple:
		case _JC_ifeq:
		case _JC_ifne:
		case _JC_iflt:
		case _JC_ifge:
		case _JC_ifgt:
		case _JC_ifle:
		case _JC_ifnonnull:
		case _JC_ifnull:
		case _JC_jsr:
			info->target = &interp->insns[cinsn->u.branch.target];
			break;
		case _JC_iinc:
			info->iinc.local = cinsn->u.iinc.index;
			info->iinc.value = cinsn->u.iinc.value;
			break;
		case _JC_invokeinterface:
		case _JC_invokespecial:
		case _JC_invokestatic:
		case _JC_invokevirtual:
		    {
			_jc_cf_ref *const ref = cinsn->u.invoke.method;
			_jc_invoke *const invoke = &info->invoke;
			_jc_method *imethod;
			_jc_type *type;
			int j;

			/* Resolve method's class */
			if ((type = _jc_load_type(env,
			    loader, ref->class)) == NULL)
				goto fail;
			if (_jc_resolve_add_loader_ref(env,
			    rinfo, type->loader) != JNI_OK)
				goto post_fail;

			/* Check class vs. interface */
			if ((opcode == _JC_invokeinterface)
			    != _JC_ACC_TEST(type, INTERFACE)) {
				_JC_EX_STORE(env, IncompatibleClassChangeError,
				    "%s %s.%s%s", _jc_bytecode_names[opcode],
				    ref->class, ref->name, ref->descriptor);
				goto post_fail;
			}

			/* Resolve method */
			if ((imethod = _jc_resolve_method(env,
			    type, ref->name, ref->descriptor)) == NULL) {
				if (opcode == _JC_invokespecial)
					env->ex.num = _JC_AbstractMethodError;
				goto post_fail;
			}

			/*
			 * Handle mismatch of the opcode and the interfaceness
			 * of the method being invoked.
			 *
			 * The first case is "Miranda methods", a normal
			 * invocation of an interface method. This happens when
			 * an abstract class implements an interface but not
			 * all of the interface's methods.
			 *
			 * The second case is INVOKEINTERFACE on a method of
			 * java.lang.Object. This only happens when an interface
			 * declares one of these methods (e.g., hashCode()).
			 */
			if (_JC_ACC_TEST(imethod->class, INTERFACE))
				opcode = _JC_invokeinterface;
			else if (opcode == _JC_invokeinterface)
			    	opcode = _JC_invokevirtual;

			/* Check static-ness and virtual-ness */
			if (((opcode == _JC_invokestatic)
			      != _JC_ACC_TEST(imethod, STATIC))
			    || (opcode != _JC_invokespecial
			      && *imethod->name == '<')) {
				_JC_EX_STORE(env, IncompatibleClassChangeError,
				    "%s.%s%s invoked from %s.%s%s",
				    imethod->class->name, imethod->name,
				    imethod->signature, method->class->name,
				    method->name, method->signature);
				goto post_fail;
			}

			/* Handle invokespecial */
			if (opcode == _JC_invokespecial) do {
				_jc_method *vmethod;
				_jc_type *stype;

				/* Check for abstract method */
				if (_JC_ACC_TEST(imethod, ABSTRACT)) {
					_JC_EX_STORE(env, AbstractMethodError,
					    "%s.%s%s", ref->class, ref->name,
					    ref->descriptor);
					goto post_fail;
				}

				/* Perform invokespecial test */
				if (*imethod->name == '<')
					break;
				if (!_JC_ACC_TEST(method->class, SUPER))
					break;
				for (stype = method->class->superclass;
				    stype != imethod->class && stype != NULL;
				    stype = stype->superclass);
				if (stype == NULL)
					break;

				/* Search class hierarchy for matching method */
				vmethod = NULL;
				for (stype = method->class->superclass;
				    stype != NULL; stype = stype->superclass) {
					if ((vmethod = _jc_get_declared_method(
					    env, stype, imethod->name,
					    imethod->signature, _JC_ACC_STATIC,
					    0)) != NULL)
						break;
				}

				/* Not found? */
				if (vmethod == NULL) {
					_JC_EX_STORE(env, AbstractMethodError,
					    "%s.%s%s", ref->class, ref->name,
					    ref->descriptor);
					goto post_fail;
				}

				/* Check for illegal constructor access */
				if (*imethod->name == '<'
				    && vmethod->class != imethod->class) {
					_JC_EX_STORE(env, NoSuchMethodError,
					    "%s.%s%s", ref->class, ref->name,
					    ref->descriptor);
					goto post_fail;
				}

				/* OK, got it */
				imethod = vmethod;
			} while (JNI_FALSE);

			/* Sanity check */
			_JC_ASSERT((opcode == _JC_invokeinterface)
			    == _JC_ACC_TEST(imethod->class, INTERFACE));

			/* Count the number of words to pop off the stack */
			invoke->pop = imethod->num_parameters
			    + (opcode != _JC_invokestatic);
			for (j = 0; j < imethod->num_parameters; j++) {
				if (_jc_dword_type[imethod->param_ptypes[j]])
					invoke->pop++;
			}

			/* Optimization: de-virtualize final methods */
			if (_JC_ACC_TEST(imethod->class, FINAL)
			    && opcode == _JC_invokevirtual)
				opcode = _JC_invokespecial;

			/* OK */
			invoke->method = imethod;
			break;
		    }
		case _JC_ldc:
		    {
			_jc_cf_constant *const c = cinsn->u.constant;

			switch (c->type) {
			case CONSTANT_Integer:
				info->constant.i = c->u.Integer;
				break;
			case CONSTANT_Float:
				info->constant.f = c->u.Float;
				break;
			case CONSTANT_String:
			    {
				const char *const utf8 = c->u.String;
				char *copy;

				/* Copy string data */
				_JC_MUTEX_LOCK(env, loader->mutex);
				if ((copy = _jc_cl_strdup(env,
				    loader, utf8)) == NULL) {
					_JC_MUTEX_UNLOCK(env, loader->mutex);
					goto post_fail;
				}
				_JC_MUTEX_UNLOCK(env, loader->mutex);
				info->string.string = NULL;
				info->string.utf8 = copy;

				/* Set opcode */
				opcode = _JC_ldc_string;
				break;
			    }
			case CONSTANT_Class:		/* JSR 202 */
			    {
				_jc_type *type;

				if ((type = _jc_load_type(env,
				    loader, c->u.Class)) == NULL)
					goto fail;
				if (_jc_resolve_add_loader_ref(env,
				    rinfo, type->loader) != JNI_OK)
					goto post_fail;
				_JC_ASSERT(type->instance != NULL);
				info->constant.l = type->instance;
				break;
			    }
			default:
				_JC_ASSERT(JNI_FALSE);
			}
			break;
		    }
		case _JC_ldc2_w:
		    {
			_jc_cf_constant *const c = cinsn->u.constant;

			switch (c->type) {
			case CONSTANT_Long:
				info->constant.j = c->u.Long;
				break;
			case CONSTANT_Double:
				info->constant.d = c->u.Double;
				break;
			default:
				_JC_ASSERT(JNI_FALSE);
			}
			break;
		    }
		case _JC_lookupswitch:
		    {
			_jc_cf_lookupswitch *const csw = cinsn->u.lookupswitch;
			_jc_lookupswitch *sw;
			int j;

			/* Allocate structure */
			_JC_MUTEX_LOCK(env, loader->mutex);
			if ((sw = _jc_cl_alloc(env, loader, sizeof(*sw)
			    + csw->num_pairs * sizeof(*sw->pairs))) == NULL) {
				_JC_MUTEX_UNLOCK(env, loader->mutex);
				goto post_fail;
			}
			_JC_MUTEX_UNLOCK(env, loader->mutex);

			/* Copy info */
			sw->default_target = &interp->insns[csw->default_target];
			sw->num_pairs = csw->num_pairs;
			for (j = 0; j < sw->num_pairs; j++) {
				_jc_cf_lookup *const clup = &csw->pairs[j];
				_jc_lookup *const lup = &sw->pairs[j];

				lup->match = clup->match;
				lup->target = &interp->insns[clup->target];
			}

			/* Done */
			info->lookupswitch = sw;
			break;
		    }
		case _JC_multianewarray:
			if ((info->multianewarray.type = _jc_load_type(env,
			    loader, cinsn->u.multianewarray.type)) == NULL)
				goto fail;
			if (_jc_resolve_add_loader_ref(env, rinfo,
			    info->multianewarray.type->loader) != JNI_OK)
				goto post_fail;
			info->multianewarray.dims = cinsn->u.multianewarray.dims;
			break;
		case _JC_newarray:
			info->type = vm->boot.types.prim_array[
			    cinsn->u.newarray.type];
			break;
		case _JC_tableswitch:
		    {
			_jc_cf_tableswitch *const csw = cinsn->u.tableswitch;
			const int num_targets = csw->high - csw->low + 1;
			_jc_tableswitch *sw;
			int j;

			/* Allocate structure */
			_JC_MUTEX_LOCK(env, loader->mutex);
			if ((sw = _jc_cl_alloc(env, loader, sizeof(*sw)
			    + num_targets * sizeof(*sw->targets))) == NULL) {
				_JC_MUTEX_UNLOCK(env, loader->mutex);
				goto post_fail;
			}
			_JC_MUTEX_UNLOCK(env, loader->mutex);

			/* Copy info */
			sw->default_target = &interp->insns[csw->default_target];
			sw->low = csw->low;
			sw->high = csw->high;
			for (j = 0; j < num_targets; j++)
				sw->targets[j] = &interp->insns[csw->targets[j]];

			/* Done */
			info->tableswitch = sw;
			break;
		    }
		default:
			break;
		}

		/* Set opcode */
		insn->action = opcode;
	}

	/* Finally, convert opcodes to interpreter jump targets */
	for (i = 0; i < code->num_insns; i++) {
		_jc_insn *const insn = &interp->insns[i];

		insn->action = _jc_interp_targets[insn->action];
		_JC_ASSERT(insn->action != 0);
	}

	/* Free parsed code */
	_jc_destroy_code(code);

	/* Done */
	return JNI_OK;

post_fail:
	_jc_post_exception_info(env);

fail:
	/* Give back class loader memory */
	if (!mutex_locked) {
		_JC_MUTEX_LOCK(env, loader->mutex);
		mutex_locked = JNI_TRUE;
	}
	_jc_cl_unalloc(loader, &interp->traps,
	    code->num_traps * sizeof(*interp->traps));
	_jc_cl_unalloc(loader, &interp->insns,
	    code->num_insns * sizeof(*interp->insns));
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Free parsed code */
	_jc_destroy_code(code);

	/* Done */
	return JNI_ERR;
}

/*
 * Create the interface method hash table and "quick" hash table.
 */
static jint
_jc_derive_imethod_tables(_jc_env *env, _jc_type *type)
{
	int heads[_JC_IMETHOD_HASHSIZE];
	int num_singleton_buckets;
	int num_nonempty_buckets;
	_jc_method **methods;
	int max_methods;
	int num_methods;
	_jc_type *stype;
	_jc_method **ptr;
	int *follows;
	int i;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(type, INTERFACE));

	/* Count the number of interface methods (including duplicates) */
	max_methods = 0;
	for (stype = type; stype != NULL; stype = stype->superclass)
		max_methods += _jc_add_iface_methods(stype, NULL);

	/* Anything to do? */
	if (max_methods == 0) {
		type->imethod_quick_table = _jc_empty_quick_table;
		type->imethod_hash_table = _jc_empty_imethod_table;
		return JNI_OK;
	}

	/* Allocate temporary array */
	if ((methods = _JC_STACK_ALLOC(env,
	    max_methods * sizeof(*methods))) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Gather interface methods */
	i = 0;
	for (stype = type; stype != NULL; stype = stype->superclass)
		i += _jc_add_iface_methods(stype, &methods[i]);
	_JC_ASSERT(i == max_methods);

	/* Resolve all interface methods */
	for (i = 0; i < max_methods; i++) {
		_jc_method *const imethod = methods[i];
		_jc_method *method = NULL;
		_jc_type *stype;

		/* Resolve the interface method via name & signature */
		for (stype = type; stype != NULL; stype = stype->superclass) {
			method = _jc_method_lookup(stype, imethod);
			if (method != NULL && _JC_ACC_TEST(method, PUBLIC))
				break;
		}
		if (stype == NULL) {
			_jc_post_exception_msg(env, _JC_NoSuchMethodError,
			    "%s.%s%s", imethod->class->name, imethod->name,
			    imethod->signature);
			return JNI_ERR;
		}

		/* Add method to our list */
		methods[i] = method;
	}

	/* Sort methods by name & signature */
	qsort(methods, max_methods, sizeof(*methods), _jc_method_sorter);

	/* Eliminate redundancies (methods declared in multiple interfaces) */
	num_methods = max_methods;
	for (i = 0; i < num_methods - 1; i++) {
		_jc_method *const current = methods[i];
		_jc_method *const next = methods[i + 1];

		if (current != next)
			continue;
		memmove(methods + i, methods + i + 1,
		    (--num_methods - i) * sizeof(*methods));
	}

	/* Allocate links for hash buckets */
	if ((follows = _JC_STACK_ALLOC(env,
	    num_methods * sizeof(*follows))) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Sort interface methods into hash buckets */
	num_nonempty_buckets = 0;
	num_singleton_buckets = 0;
	memset(&heads, ~0, sizeof(heads));
	for (i = 0; i < num_methods; i++) {
		_jc_method *const method = methods[i];
		const int bucket = method->signature_hash_bucket;

		/* Keep track of the number of nonempty and singleton buckets */
		if (heads[bucket] == -1) {
			num_singleton_buckets++;
			num_nonempty_buckets++;
		} else if (follows[heads[bucket]] == -1)
			num_singleton_buckets--;

		/* Add method to bucket */
		follows[i] = heads[bucket];
		heads[bucket] = i;
	}

	/* Allocate "quick" table, hash buckets, and room for bucket lists */
	_JC_MUTEX_LOCK(env, type->loader->mutex);
	if ((type->imethod_hash_table = _jc_cl_zalloc(env, type->loader,
	    _JC_IMETHOD_HASHSIZE * sizeof(*type->imethod_hash_table)
	    + (num_singleton_buckets > 0 ? 
	      _JC_IMETHOD_HASHSIZE * sizeof(*type->imethod_quick_table) : 0)
	    + (num_methods + num_nonempty_buckets)
	      * sizeof(**type->imethod_hash_table))) == NULL) {
		_JC_MUTEX_UNLOCK(env, type->loader->mutex);
		_jc_post_exception_info(env);
		return JNI_ERR;
	}
	_JC_MUTEX_UNLOCK(env, type->loader->mutex);

	/* Fill in the "quick" hash table (if any) */
	ptr = (_jc_method **)(type->imethod_hash_table + _JC_IMETHOD_HASHSIZE);
	if (num_singleton_buckets > 0) {
		type->imethod_quick_table = ptr;
		ptr += _JC_IMETHOD_HASHSIZE;
		for (i = 0; i < _JC_IMETHOD_HASHSIZE; i++) {
			if (heads[i] != -1 && follows[heads[i]] == -1)
				type->imethod_quick_table[i] = methods[heads[i]];
		}
	} else
		type->imethod_quick_table = _jc_empty_quick_table;

	/* Fill in the hash table */
	for (i = 0; i < _JC_IMETHOD_HASHSIZE; i++) {
		int next = heads[i];

		/* Skip this if bucket is empty */
		if (next == -1)
			continue;

		/* Add entries in the hash bucket */
		type->imethod_hash_table[i] = ptr;
		while (next != -1) {
			*ptr++ = methods[next];
			next = follows[next];
		}

		/* Terminate entry list with a NULL pointer */
		ptr++;
	}

	/* Sanity check */
	_JC_ASSERT((char *)ptr == (char *)type->imethod_hash_table
	    + _JC_IMETHOD_HASHSIZE * sizeof(*type->imethod_hash_table)
	    + (num_singleton_buckets > 0 ? 
	      _JC_IMETHOD_HASHSIZE * sizeof(*type->imethod_quick_table) : 0)
	    + (num_methods + num_nonempty_buckets)
	      * sizeof(**type->imethod_hash_table));

	/* Done */
	return JNI_OK;
}

/*
 * Create the instanceof table.
 */
static jint
_jc_derive_instanceof_table(_jc_env *env, _jc_type *const type)
{
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	int heads[_JC_INSTANCEOF_HASHSIZE];
	int num_nonempty_buckets;
	_jc_type **types = NULL;
	_jc_type *stype;
	int num_types;
	_jc_type **ptr;
	int *follows;
	int i;

again:
	/* Scan instanceof types from superclasses & superinterfaces */
	num_types = 0;
	for (stype = type; stype != NULL; stype = stype->superclass) {
		int j;

		/* Inherit instanceof's from each implemented interface */
		for (j = 0; j < stype->num_interfaces; j++) {
			_jc_type *const iftype = stype->interfaces[j];
			int bkt;

			for (bkt = 0; bkt < _JC_INSTANCEOF_HASHSIZE; bkt++) {
				ptr = iftype->u.nonarray
				    .instanceof_hash_table[bkt];
				if (ptr == NULL)
					continue;
				while (*ptr != NULL) {
					if (types != NULL)
						types[num_types] = *ptr;
					num_types++;
					ptr++;
				}
			}
		}

		/* Add one for this (super)class */
		if (types != NULL)
			types[num_types] = stype;
		num_types++;
	}

	/* Allocate temporary array and scan again */
	if (types == NULL) {
		if ((types = _JC_STACK_ALLOC(env,
		    num_types * sizeof(*types))) == NULL) {
			_jc_post_exception_info(env);
			return JNI_ERR;
		}
		goto again;
	}

	/* Sort types */
	qsort(types, num_types, sizeof(*types), _jc_type_sorter);

	/* Eliminate redundancies */
	for (i = 0; i < num_types - 1; i++) {
		_jc_type *const current = types[i];
		_jc_type *const next = types[i + 1];

		if (current != next)
			continue;
		memmove(types + i, types + i + 1,
		    (--num_types - i) * sizeof(*types));
	}

	/* Allocate links for hash buckets */
	if ((follows = _JC_STACK_ALLOC(env,
	    num_types * sizeof(*follows))) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Put instanceof types into hash buckets */
	num_nonempty_buckets = 0;
	memset(&heads, ~0, sizeof(heads));
	for (i = 0; i < num_types; i++) {
		_jc_type *const type = types[i];
		const int bucket = _JC_INSTANCEOF_BUCKET(type);

		/* Keep track of the number of nonempty buckets */
		if (heads[bucket] == -1)
			num_nonempty_buckets++;

		/* Add type to bucket */
		follows[i] = heads[bucket];
		heads[bucket] = i;
	}

	/* Allocate hash buckets, and room for bucket lists */
	_JC_MUTEX_LOCK(env, type->loader->mutex);
	if ((ntype->instanceof_hash_table = _jc_cl_zalloc(env, type->loader,
	    _JC_INSTANCEOF_HASHSIZE * sizeof(*ntype->instanceof_hash_table)
	    + (num_types + num_nonempty_buckets)
	      * sizeof(**ntype->instanceof_hash_table))) == NULL) {
		_JC_MUTEX_UNLOCK(env, type->loader->mutex);
		_jc_post_exception_info(env);
		return JNI_ERR;
	}
	_JC_MUTEX_UNLOCK(env, type->loader->mutex);

	/* Fill in the hash table */
	ptr = (_jc_type **)(ntype->instanceof_hash_table
	    + _JC_INSTANCEOF_HASHSIZE);
	for (i = 0; i < _JC_INSTANCEOF_HASHSIZE; i++) {
		int next = heads[i];

		/* Skip this if bucket is empty */
		if (next == -1)
			continue;

		/* Add entries in the hash bucket */
		ntype->instanceof_hash_table[i] = ptr;
		while (next != -1) {
			*ptr++ = types[next];
			next = follows[next];
		}

		/* Terminate entry list with a NULL pointer */
		ptr++;
	}

	/* Sanity check */
	_JC_ASSERT((char *)ptr == (char *)ntype->instanceof_hash_table
	    + _JC_INSTANCEOF_HASHSIZE * sizeof(*ntype->instanceof_hash_table)
	    + (num_types + num_nonempty_buckets)
	      * sizeof(**ntype->instanceof_hash_table));

	/* Done */
	return JNI_OK;
}

/*
 * Resolve inner classes.
 */
static jint
_jc_resolve_inner_classes(_jc_env *env, _jc_type *type, _jc_resolve_info *info)
{
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	_jc_classfile *const cfile = ntype->cfile;
	_jc_cf_inner_classes *const cinners = cfile->inner_classes;
	int count;
	int i;

	/* Sanity check */
	_JC_ASSERT(ntype->inner_classes == NULL);

	/* Any inner class info? */
	if (cinners == NULL)
		return JNI_OK;

again:
	/* Find my inner classes */
	count = 0;
	for (i = 0; i < cinners->num_classes; i++) {
		_jc_cf_inner_class *const cinner = &cinners->classes[i];

		/* Look for classes that are inner to this class */
		if (cinner->outer != NULL
		    && strcmp(cinner->outer, type->name) != 0)
			continue;
		if (cinner->inner == NULL
		    || strcmp(cinner->inner, type->name) == 0)
			continue;

		/* Resolve the inner class */
		if (ntype->inner_classes != NULL) {
			_jc_inner_class *const inner
			    = &ntype->inner_classes[count];

			/* Resolve inner class type */
			if ((inner->type = _jc_load_type(env,
			    type->loader, cinner->inner)) == NULL)
				return JNI_ERR;
			if (_jc_resolve_add_loader_ref(env, info,
			    inner->type->loader) != JNI_OK) {
				_jc_post_exception_info(env);
				return JNI_ERR;
			}

			/* Set access flags */
			inner->access_flags = cinner->access_flags;

			/* Set back pointer */
			inner->type->u.nonarray.outer_class = type;
		}
		count++;
	}

	/* Allocate memory and rescan */
	if (ntype->inner_classes == NULL) {
		_JC_MUTEX_LOCK(env, type->loader->mutex);
		if ((ntype->inner_classes = _jc_cl_alloc(env, type->loader,
		    count * sizeof(*ntype->inner_classes))) == NULL) {
			_JC_MUTEX_UNLOCK(env, type->loader->mutex);
			_jc_post_exception_info(env);
			return JNI_ERR;
		}
		_JC_MUTEX_UNLOCK(env, type->loader->mutex);
		goto again;
	}

	/* Set count */
	ntype->num_inner_classes = count;

	/* Done */
	return JNI_OK;
}

/*
 * Find all interface methods a class supposedly implements.
 */
static int
_jc_add_iface_methods(_jc_type *type, _jc_method **methods)
{
	int count = 0;
	int i;

	/* Check all declared interfaces */
	for (i = 0; i < type->num_interfaces; i++) {
		_jc_type *const iftype = type->interfaces[i];
		int num;
		int j;

		/* Add interface methods for this interface */
		for (j = 0; j < iftype->u.nonarray.num_methods; j++) {
			_jc_method *const imethod
			    = iftype->u.nonarray.methods[j];

			/* Add interface method */
			if (!_JC_ACC_TEST(imethod, STATIC)) {
				if (methods != NULL)
					*methods++ = imethod;
				count++;
			}
		}

		/* Recurse on interface */
		num = _jc_add_iface_methods(iftype, methods);
		if (methods != NULL)
			methods += num;
		count += num;
	}

	/* Recurse on superclass */
	if (type->superclass != NULL)
		count += _jc_add_iface_methods(type->superclass, methods);

	/* Done */
	return count;
}

/*
 * Lookup an instance method.
 */
_jc_method *
_jc_method_lookup(_jc_type *type, _jc_method *key)
{
	_jc_nonarray_type *const ntype = &type->u.nonarray;
	_jc_method **methodp;

	/* Sanity check */
	_JC_ASSERT(*key->name != '<');

	/* Search */
	methodp = bsearch(&key, ntype->methods, ntype->num_methods,
	    sizeof(*ntype->methods), _jc_method_sorter);
	if (methodp != NULL && !_JC_ACC_TEST(*methodp, STATIC))
		return *methodp;

	/* Not found */
	return NULL;
}

/*
 * Sorts method pointers by name, then signature.
 *
 * This must sort the same as org.dellroad.jc.cgen.Util.methodComparator.
 */
static int
_jc_method_sorter(const void *item1, const void *item2)
{
        const _jc_method *const method1 = *((_jc_method **)item1);
        const _jc_method *const method2 = *((_jc_method **)item2);
	int diff;

	if ((diff = strcmp(method1->name, method2->name)) != 0)
		return diff;
	return strcmp(method1->signature, method2->signature);
}

/*
 * Sorts type pointers by name.
 */
static int
_jc_type_sorter(const void *item1, const void *item2)
{
        const _jc_type *const type1 = *((_jc_type **)item1);
        const _jc_type *const type2 = *((_jc_type **)item2);

	return strcmp(type1->name, type2->name);
}

