
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
static _jc_object	*_jc_get_reflected(_jc_env *env, _jc_method *constr,
				_jc_type *type, const char *name, jint slot);

/*
 * Extract a pointer from a gnu.classpath.Pointer object which
 * is referred to by the given field in the given object.
 *
 * If the field is null then NULL is returned.
 */
void *
_jc_get_vm_pointer(_jc_jvm *vm, _jc_object *obj, _jc_field *field)
{
	_jc_object *pobj;

	/* Sanity check */
	_JC_ASSERT(obj != NULL && field != NULL);
	_JC_ASSERT(_jc_subclass_of(obj, field->class));

	/* Get reference to the Pointer object */
	pobj = *((_jc_object **)((char *)obj + field->offset));
	if (pobj == NULL)
		return NULL;

	/* Extract the contained pointer */
	return *_JC_VMFIELD(vm, pobj, Pointer, data, void *);
}

/*
 * Store a pointer into a field of type gnu.classpath.Pointer.
 *
 * Posts an exception on failure.
 */
jint
_jc_set_vm_pointer(_jc_env *env, _jc_object *obj, _jc_field *field, void *ptr)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *pobj;

	/* Sanity check */
	_JC_ASSERT(obj != NULL && field != NULL);
	_JC_ASSERT(_jc_subclass_of(obj, field->class));

	/* Handle easy case */
	if (ptr == NULL) {
		pobj = NULL;
		goto done;
	}

	/* Create a new Pointer object (if one doesn't already exist) */
	pobj = *((_jc_object **)((char *)obj + field->offset));
	if (pobj == NULL) {
		if ((pobj = _jc_new_object(env,
		    vm->boot.types.Pointer)) == NULL)
			return JNI_ERR;
	}

	/* Store the pointer in the Pointer object */
	*_JC_VMFIELD(vm, pobj, Pointer, data, void *) = ptr;

done:
	/* Set the Pointer in the object (after storing ptr within) */
	*((_jc_object **)((char *)obj + field->offset)) = pobj;
	return JNI_OK;
}

/*
 * Resolve a field declared in a specific class.
 *
 * If unsuccessful a NoSuchFieldError exception is stored.
 */
_jc_field *
_jc_get_declared_field(_jc_env *env, _jc_type *type, const char *name,
	const char *sig, int is_static)
{
	_jc_field key_data;
	_jc_field *const key = &key_data;
	_jc_field **fieldp;
	_jc_field *field;

	/* Only normal reference types have declared fields */
	if ((type->flags & (_JC_TYPE_ARRAY|_JC_TYPE_MASK))
	    != _JC_TYPE_REFERENCE)
		goto fail;

	/* Binary search for field */
	key->name = name;
	key->signature = sig;
	key->access_flags = is_static ? _JC_ACC_STATIC : 0;
	if ((fieldp = bsearch(&key, type->u.nonarray.fields,
	    type->u.nonarray.num_fields, sizeof(*type->u.nonarray.fields),
	    _jc_field_compare)) == NULL)
	      	goto fail;
	field = *fieldp;

	/* Done */
	return field;

fail:
	/* Not found */
	_JC_EX_STORE(env, NoSuchFieldError, "%s.%s (type `%s', %sstatic)",
	    type->name, name, sig, is_static ? "" : "non-");
	return NULL;
}

/*
 * Resolve a method declared in a specific class.
 *
 * The method's access flags in 'flags_mask' must match 'flags'.
 *
 * If unsuccessful a NoSuchMethodError exception is stored.
 */
_jc_method *
_jc_get_declared_method(_jc_env *env, _jc_type *type, const char *name,
	const char *sig, int flags_mask, int flags)
{
	_jc_method key_data;
	_jc_method *const key = &key_data;
	_jc_method **methodp;
	_jc_method *method;

	/* Only normal reference types have declared methods */
	if ((type->flags & (_JC_TYPE_ARRAY|_JC_TYPE_MASK))
	    != _JC_TYPE_REFERENCE)
		goto fail;

	/* Binary search for method */
	key->name = name;
	key->signature = sig;
	if ((methodp = bsearch(&key, type->u.nonarray.methods,
	    type->u.nonarray.num_methods, sizeof(*type->u.nonarray.methods),
	    _jc_method_compare)) == NULL)
	      	goto fail;
	method = *methodp;

	/* Check access flags */
	if ((method->access_flags & flags_mask) != flags)
		goto fail;

	/* Done */
	return method;

fail:
	/* Not found */
	_JC_EX_STORE(env, NoSuchMethodError, "%s.%s%s", type->name, name, sig);
	return NULL;
}

/*
 * Get the java.lang.reflect.Field object corresponding to 'field'.
 */
_jc_object *
_jc_get_reflected_field(_jc_env *env, _jc_field *field)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *const dclass = field->class;
	int slot;

	/* Determine slot */
	for (slot = 0; slot < dclass->u.nonarray.num_fields; slot++) {
		if (dclass->u.nonarray.fields[slot] == field)
			break;
	}
	_JC_ASSERT(slot < dclass->u.nonarray.num_fields);

	/* Return reflection object */
	return _jc_get_reflected(env, vm->boot.methods.Field.init,
	    dclass, field->name, slot);
}

/*
 * Get the java.lang.reflect.Method object corresponding to 'method'.
 */
_jc_object *
_jc_get_reflected_method(_jc_env *env, _jc_method *method)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *const dclass = method->class;
	int slot;

	/* Sanity check */
	_JC_ASSERT(*method->name != '<');

	/* Determine slot */
	for (slot = 0; slot < dclass->u.nonarray.num_methods; slot++) {
		if (dclass->u.nonarray.methods[slot] == method)
			break;
	}
	_JC_ASSERT(slot < dclass->u.nonarray.num_methods);

	/* Return reflection object */
	return _jc_get_reflected(env, vm->boot.methods.Method.init,
	    dclass, method->name, slot);
}

/*
 * Get the java.lang.reflect.Constructor object corresponding to 'method'.
 */
_jc_object *
_jc_get_reflected_constructor(_jc_env *env, _jc_method *method)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *const dclass = method->class;
	int slot;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC) && *method->name == '<');

	/* Determine slot */
	for (slot = 0; slot < dclass->u.nonarray.num_methods; slot++) {
		if (dclass->u.nonarray.methods[slot] == method)
			break;
	}
	_JC_ASSERT(slot < dclass->u.nonarray.num_methods);

	/* Return reflection object */
	return _jc_get_reflected(env, vm->boot.methods.Constructor.init,
	    dclass, NULL, slot);
}

/*
 * Get a java.lang.reflect object. We create a new one each time,
 * because these objects are mutable (they inherit AccessibleObject).
 */
static _jc_object *
_jc_get_reflected(_jc_env *env, _jc_method *constr,
	_jc_type *type, const char *name, jint slot)
{
	_jc_object *rtn = NULL;
	jobject rref = NULL;
	_jc_word params[3];
	int nparam = 0;

	/* Create the new reflection instance */
	if ((rref = _jc_new_local_native_ref(env,
	    _jc_new_object(env, constr->class))) == NULL)
		goto done;

	/* Prepare constructor parameters */
	params[nparam++] = (_jc_word)type->instance;
	if (name != NULL) {
		_jc_object *string;

		if ((string = _jc_new_string(env, name, strlen(name))) == NULL)
			goto done;
		params[nparam++] = (_jc_word)string;
	}
	params[nparam++] = (_jc_word)slot;

	/* Initialize it */
	if (_jc_invoke_nonvirtual_a(env, constr, *rref, params) != JNI_OK)
		goto done;

	/* Done */
	rtn = *rref;

done:
	/* Free local native refs */
	_jc_free_local_native_ref(&rref);

	/* Done */
	return rtn;
}

/*
 * Find the _jc_method structure corresponding to a Method object.
 */
_jc_method *
_jc_get_method(_jc_env *env, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	_jc_method *method;
	_jc_object *cl;
	_jc_type *type;
	jint slot;

	/* Sanity check */
	_JC_ASSERT(obj != NULL && obj->type == vm->boot.types.Method);

	/* Locate declaring class */
	cl = *_JC_VMFIELD(vm, obj, Method, declaringClass, _jc_object *);
	type = *_JC_VMFIELD(vm, cl, Class, vmdata, _jc_type *);
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));

	/* Locate method */
	slot = *_JC_VMFIELD(vm, obj, Method, slot, jint);
	_JC_ASSERT(slot >= 0 && slot < type->u.nonarray.num_methods);
	method = type->u.nonarray.methods[slot];

	/* Sanity check */
	_JC_ASSERT(*method->name != '<');

	/* Done */
	return method;
}

/*
 * Find the _jc_method structure corresponding to a Constructor object.
 */
_jc_method *
_jc_get_constructor(_jc_env *env, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	_jc_method *method;
	_jc_object *cl;
	_jc_type *type;
	jint slot;

	/* Sanity check */
	_JC_ASSERT(obj != NULL && obj->type == vm->boot.types.Constructor);

	/* Locate declaring class */
	cl = *_JC_VMFIELD(vm, obj, Constructor, clazz, _jc_object *);
	type = *_JC_VMFIELD(vm, cl, Class, vmdata, _jc_type *);
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));

	/* Locate constructor method */
	slot = *_JC_VMFIELD(vm, obj, Constructor, slot, jint);
	_JC_ASSERT(slot >= 0 && slot < type->u.nonarray.num_methods);
	method = type->u.nonarray.methods[slot];

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC) && *method->name == '<');

	/* Done */
	return method;
}

/*
 * Find the _jc_field structure corresponding to a Field object.
 */
_jc_field *
_jc_get_field(_jc_env *env, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *cl;
	_jc_type *type;
	jint slot;

	/* Sanity check */
	_JC_ASSERT(obj != NULL && obj->type == vm->boot.types.Field);

	/* Locate declaring class */
	cl = *_JC_VMFIELD(vm, obj, Field, declaringClass, _jc_object *);
	type = *_JC_VMFIELD(vm, cl, Class, vmdata, _jc_type *);
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));

	/* Locate field */
	slot = *_JC_VMFIELD(vm, obj, Field, slot, jint);
	_JC_ASSERT(slot >= 0 && slot < type->u.nonarray.num_fields);
	return type->u.nonarray.fields[slot];
}


/*
 * Invoke a method or constructor via reflection.
 *
 * This function is used by both Method.invoke() and Constructor.newInstance().
 */
jint
_jc_reflect_invoke(_jc_env *env, _jc_method *method,
	_jc_object *this, _jc_object_array *pobjs)
{
	const jboolean is_static = _JC_ACC_TEST(method, STATIC);
	const jboolean is_constructor = !is_static && *method->name == '<';
	_jc_method *imethod = NULL;
	jobject this_ref = NULL;
	_jc_word *params;
	jint status;
	int nwords;
	int pi;
	int i;

	/* Check class is instantiable (if constructor) */
	if (is_constructor
	   && (method->class->access_flags
	    & (_JC_ACC_ABSTRACT|_JC_ACC_INTERFACE)) != 0) {
		_jc_post_exception(env, _JC_InstantiationException);
		goto fail;
	}

	/* Check for null 'this' */
	if (!is_static && !is_constructor && this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto fail;
	}

	/* Check that 'this' is the right kind of instance */
	if (!is_static && !is_constructor) {
		switch (_jc_instance_of(env, this, method->class)) {
		case 1:
			break;
		case 0:
			_jc_post_exception_msg(env,
			    _JC_IllegalArgumentException,
			    "`%s' object is not an instance of `%s'",
			    this->type->name, method->class->name);
			goto fail;
		case -1:
			goto fail;
		default:
			_JC_ASSERT(JNI_FALSE);
		}
	}

	/* Resolve interface methods using hash table lookup */
	if (!is_static && !is_constructor
	    && _JC_ACC_TEST(method->class, INTERFACE)) {
	      	const int bucket = method->signature_hash_bucket;
		_jc_type *const type = this->type;
		_jc_method *const *methodp;

		_JC_ASSERT(type->imethod_hash_table != NULL);
		for (methodp = type->imethod_hash_table[bucket];
		    *methodp != NULL; methodp++) {
		    	_jc_method *const entry = *methodp;

			if (strcmp(entry->name, method->name) == 0
			    && strcmp(entry->signature, method->signature) == 0) {
				imethod = method;
				method = entry;
				break;
			}
		}
		_JC_ASSERT(*methodp != NULL);
	}

	/* Check for exceptions */
	if (method->num_parameters > 0 && pobjs == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto fail;
	}
	if (pobjs != NULL && pobjs->length != method->num_parameters) {
		_jc_post_exception_msg(env, _JC_IllegalArgumentException,
		    "number of parameters (%d) does not match method (%d)",
		    pobjs->length, method->num_parameters);
		goto fail;
	}

	/* Count parameter words */
	nwords = method->num_parameters;
	for (i = 0; i < method->num_parameters; i++) {
		if (_jc_dword_type[method->param_ptypes[i]])
			nwords++;
	}

	/* Allocate parameter array */
	if ((params = _JC_STACK_ALLOC(env, nwords * sizeof(*params))) == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}

	/* Convert parameters */
	for (pi = i = 0; i < method->num_parameters; i++) {
		_jc_type *const ptype = method->param_types[i];
		const int dtype = (ptype->flags & _JC_TYPE_MASK);
		_jc_value value;

		/* Convert parameter object into a _jc_value */
		if (dtype == _JC_TYPE_REFERENCE) {
			_jc_object *const obj = pobjs->elems[~i];

			/* Check parameter has the proper type */
			if (obj != NULL) {
				switch (_jc_instance_of(env, obj, ptype)) {
				case 1:
					break;
				case 0:
					_jc_post_exception(env,
					    _JC_IllegalArgumentException);
					goto fail;
				case -1:
					goto fail;
				default:
					_JC_ASSERT(JNI_FALSE);
				}
			}
			value.l = obj;
		} else {
			int stype;

			if ((stype = _jc_unwrap_primitive(env,
			    pobjs->elems[~i], &value)) == _JC_TYPE_INVALID)
				goto fail;
			if (_jc_convert_primitive(env,
			    dtype, stype, &value) != JNI_OK)
				goto fail;
		}

		/* Add value to parameter list */
		switch (dtype) {
		case _JC_TYPE_BOOLEAN:
			params[pi++] = (jint)value.z;
			break;
		case _JC_TYPE_BYTE:
			params[pi++] = (jint)value.b;
			break;
		case _JC_TYPE_CHAR:
			params[pi++] = (jint)value.c;
			break;
		case _JC_TYPE_SHORT:
			params[pi++] = (jint)value.s;
			break;
		case _JC_TYPE_INT:
			params[pi++] = value.i;
			break;
		case _JC_TYPE_FLOAT:
		    {
			const jfloat param = value.f;

			memcpy(params + pi, &param, sizeof(param));
			pi++;
			break;
		    }
		case _JC_TYPE_LONG:
		    {
			const jlong param = value.j;

			memcpy(params + pi, &param, sizeof(param));
			pi += 2;
			break;
		    }
		case _JC_TYPE_DOUBLE:
		    {
			const jdouble param = value.d;

			memcpy(params + pi, &param, sizeof(param));
			pi += 2;
			break;
		    }
		case _JC_TYPE_REFERENCE:
			params[pi++] = (_jc_word)value.l;
			break;
		default:
			_JC_ASSERT(JNI_FALSE);
			break;
		}
	}

	/* Create new instance (if constructor) */
	if (is_constructor) {
		if ((this_ref = _jc_new_local_native_ref(env,
		    _jc_new_object(env, method->class))) == NULL)
			goto fail;
		this = *this_ref;
	}

	/* For JDK compatibility, interface methods cause initialization */
	if (imethod != NULL
	    && !_JC_FLG_TEST(imethod->class, INITIALIZED)
	    && _jc_initialize_type(env, imethod->class) != JNI_OK)
		goto fail;

	/* Invoke method */
	if (is_static) {
		if (!_JC_FLG_TEST(method->class, INITIALIZED)
		    && _jc_initialize_type(env, method->class) != JNI_OK)
			goto fail;
		status = _jc_invoke_static_a(env, method, params);
	} else if (is_constructor)
		status = _jc_invoke_nonvirtual_a(env, method, this, params);
	else
		status = _jc_invoke_virtual_a(env, method, this, params);

	/* Handle exception thrown by method */
	if (status != JNI_OK) {
		_jc_object *e;
		_jc_word param;

		/* Get thrown exception */
		e = _jc_retrieve_exception(env, NULL);
		_JC_ASSERT(e != NULL);
		param = (_jc_word)e;

		/* Wrap it in a InvocationTargetException */
		_jc_post_exception_params(env,
		    _JC_InvocationTargetException, &param);
		goto fail;
	}

	/* For constructors, return value is new object */
	if (is_constructor) {
		_JC_ASSERT(this != NULL);
		env->retval.l = this;
	}

	/* Done */
	_jc_free_local_native_ref(&this_ref);
	return JNI_OK;

fail:
	/* Clean up after failure */
	_jc_free_local_native_ref(&this_ref);
	return JNI_ERR;
}

/*
 * Return the parameter types of a method or constructor as a Class[] array.
 */
_jc_object_array *
_jc_get_parameter_types(_jc_env *env, _jc_method *method)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array;
	int i;

	/* Create Class[] array */
	if ((array = (_jc_object_array *)_jc_new_array(env,
	    vm->boot.types.Class_array, method->num_parameters)) == NULL)
		return NULL;

	/* Fill array */
	for (i = 0; i < method->num_parameters; i++)
		array->elems[~i] = method->param_types[i]->instance;

	/* Done */
	return array;
}

/*
 * Return the exception types of a method or constructor as a Class[] array.
 */
_jc_object_array *
_jc_get_exception_types(_jc_env *env, _jc_method *method)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array;
	int i;

	/* Create Class[] array */
	if ((array = (_jc_object_array *)_jc_new_array(env,
	    vm->boot.types.Class_array, method->num_exceptions)) == NULL)
		return NULL;

	/* Fill array */
	for (i = 0; i < method->num_exceptions; i++)
		array->elems[~i] = method->exceptions[i]->instance;

	/* Done */
	return array;
}

/*
 * Wrap a primitive value in an instance of the corresponding
 * java.lang.X wrapper class.
 *
 * Returns NULL and posts an exception on error.
 */
_jc_object *
_jc_wrap_primitive(_jc_env *env, int ptype, _jc_value *value)
{
	_jc_jvm *const vm = env->vm;
	jobject ref;

	/* Sanity check */
	_JC_ASSERT(ptype >= _JC_TYPE_BOOLEAN && ptype <= _JC_TYPE_DOUBLE);

	/* Create new wrapper object */
	if ((ref = _jc_new_local_native_ref(env,
	    _jc_new_object(env, vm->boot.types.prim_wrapper[ptype]))) == NULL)
		return NULL;

	/* Invoke constructor */
	if (_jc_invoke_nonvirtual_a(env,
	    vm->boot.methods.prim_wrapper[ptype].init,
	    *ref, (_jc_word *)value) != JNI_OK) {
		_jc_free_local_native_ref(&ref);
		return NULL;
	}

	/* Done */
	return _jc_free_local_native_ref(&ref);
}

/*
 * Extract the primitive value from a wrapper class object,
 * put the result in *value, and return the primitive type.
 *
 * If an exception is thrown, _JC_TYPE_INVALID is returned.
 */
int
_jc_unwrap_primitive(_jc_env *env, _jc_object *obj, _jc_value *value)
{
	_jc_jvm *const vm = env->vm;
	jint status;
	int ptype;

	/* Check for null */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_IllegalArgumentException);
		return _JC_TYPE_INVALID;
	}

	/* Check that wrapper object is really a wrapper object */
	for (ptype = _JC_TYPE_BOOLEAN;
	    ptype <= _JC_TYPE_DOUBLE
	      && obj->type != vm->boot.types.prim_wrapper[ptype];
	    ptype++);
	if (ptype > _JC_TYPE_DOUBLE) {
		_jc_post_exception_msg(env, _JC_IllegalArgumentException,
		    "not a primitive wrapper class instance");
		return _JC_TYPE_INVALID;
	}

	/* Extract the primitive value from the wrapper object */
	if ((status = _jc_invoke_virtual(env,
	    vm->boot.methods.prim_wrapper[ptype].value, obj)) != JNI_OK)
		return status;

	/* Convert Java return type to actual Java type */
	switch (ptype) {
	case _JC_TYPE_BOOLEAN:
		value->z = (jboolean)env->retval.i;
		break;
	case _JC_TYPE_BYTE:
		value->b = (jbyte)env->retval.i;
		break;
	case _JC_TYPE_CHAR:
		value->c = (jchar)env->retval.i;
		break;
	case _JC_TYPE_SHORT:
		value->s = (jshort)env->retval.i;
		break;
	case _JC_TYPE_INT:
		value->i = env->retval.i;
		break;
	case _JC_TYPE_LONG:
		value->j = env->retval.j;
		break;
	case _JC_TYPE_FLOAT:
		value->f = env->retval.f;
		break;
	case _JC_TYPE_DOUBLE:
		value->d = env->retval.d;
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
		break;
	}

	/* Done */
	return ptype;
}

/*
 * Convert one primitive type to another by a widening conversion.
 * If the conversion is illegal, throw an IllegalArgumentException.
 */
jint
_jc_convert_primitive(_jc_env *env, int dtype, int stype, _jc_value *value)
{
	static const void *const
	    convert_table[_JC_TYPE_DOUBLE + 1][_JC_TYPE_DOUBLE + 1] = {
		[_JC_TYPE_SHORT]= {
			[_JC_TYPE_BYTE]=	&&b2s,
		},
		[_JC_TYPE_INT]= {
			[_JC_TYPE_BYTE]=	&&b2i,
			[_JC_TYPE_CHAR]=	&&c2i,
			[_JC_TYPE_SHORT]=	&&s2i,
		},
		[_JC_TYPE_LONG]= {
			[_JC_TYPE_BYTE]=	&&b2j,
			[_JC_TYPE_CHAR]=	&&c2j,
			[_JC_TYPE_SHORT]=	&&s2j,
			[_JC_TYPE_INT]=		&&i2j,
		},
		[_JC_TYPE_FLOAT]= {
			[_JC_TYPE_BYTE]=	&&b2f,
			[_JC_TYPE_CHAR]=	&&c2f,
			[_JC_TYPE_SHORT]=	&&s2f,
			[_JC_TYPE_INT]=		&&i2f,
			[_JC_TYPE_LONG]=	&&j2f,
		},
		[_JC_TYPE_DOUBLE]= {
			[_JC_TYPE_BYTE]=	&&b2d,
			[_JC_TYPE_CHAR]=	&&c2d,
			[_JC_TYPE_SHORT]=	&&s2d,
			[_JC_TYPE_INT]=		&&i2d,
			[_JC_TYPE_LONG]=	&&j2d,
			[_JC_TYPE_FLOAT]=	&&f2d,
		},
	};
	const void *target;
	_jc_value svalue;

	/* Sanity check */
	_JC_ASSERT(dtype >= _JC_TYPE_BOOLEAN && dtype <= _JC_TYPE_DOUBLE);
	_JC_ASSERT(stype >= _JC_TYPE_BOOLEAN && stype <= _JC_TYPE_DOUBLE);

	/* Handle the trivial case */
	if (dtype == stype)
		return JNI_OK;

	/* Check that the conversion is allowed */
	if ((target = convert_table[dtype][stype]) == NULL) {
		_jc_post_exception_msg(env, _JC_IllegalArgumentException,
		    "illegal widening conversion");
		return JNI_ERR;
	}

	/* Convert the value by a widening conversion */
	svalue = *value;		/* XXX is this step necessary? */
	goto *target;

b2s:	value->s = svalue.b; return JNI_OK;
b2i:	value->i = svalue.b; return JNI_OK;
c2i:	value->i = svalue.c; return JNI_OK;
s2i:	value->i = svalue.s; return JNI_OK;
b2j:	value->j = svalue.b; return JNI_OK;
c2j:	value->j = svalue.c; return JNI_OK;
s2j:	value->j = svalue.s; return JNI_OK;
i2j:	value->j = svalue.i; return JNI_OK;
b2f:	value->f = svalue.b; return JNI_OK;
c2f:	value->f = svalue.c; return JNI_OK;
s2f:	value->f = svalue.s; return JNI_OK;
i2f:	value->f = svalue.i; return JNI_OK;
j2f:	value->f = svalue.j; return JNI_OK;
b2d:	value->d = svalue.b; return JNI_OK;
c2d:	value->d = svalue.c; return JNI_OK;
s2d:	value->d = svalue.s; return JNI_OK;
i2d:	value->d = svalue.i; return JNI_OK;
j2d:	value->d = svalue.j; return JNI_OK;
f2d:	value->d = svalue.f; return JNI_OK;
}

/*
 * Determine whether a class member is accessible from the calling method.
 * The "calling method" is the first non-reflection method on the stack.
 *
 * Returns:
 *	 1	Yes
 *	 0	No (and *calling_classp points to the calling class)
 *	-1	Exception posted
 */
int
_jc_reflect_accessible(_jc_env *env, _jc_type *member_class,
	_jc_uint16 access, _jc_type **calling_classp)
{
	_jc_type *calling_class = NULL;
	_jc_java_stack *jstack;
	int rtn;

	/* If member is public, we're done */
	if ((access & _JC_ACC_PUBLIC) != 0)
		return 1;

	/* Crawl the stack until we see a non-bootstrap ClassLoader */
	for (jstack = env->java_stack; jstack != NULL; jstack = jstack->next) {

		/* Skip java.lang.reflect methods */
		if (strncmp(jstack->method->class->name, "java/lang/reflect/",
		    sizeof("java/lang/reflect/") - 1) == 0)
			continue;

		/* Found calling class */
		calling_class = jstack->method->class;
		*calling_classp = calling_class;
		break;
	}

	/* Sanity check */
	_JC_ASSERT(calling_class != NULL);

	/* Compare calling class with member class and flags */
	switch (access & (_JC_ACC_PRIVATE|_JC_ACC_PROTECTED)) {
	case _JC_ACC_PRIVATE:
		return calling_class == member_class;
		break;
	case _JC_ACC_PROTECTED:
		if ((rtn = _jc_assignable_from(env,
		    calling_class, member_class)) != 0)
			return rtn;
		/* FALLTHROUGH */
	case 0:				/* package access */
	    {
		int calling_plen = 0;
		int member_plen = 0;
		const char *s;

		/* Check for same class loader */
		if (member_class->loader != calling_class->loader)
			return JNI_FALSE;

		/* Check for same package */
		if ((s = strrchr(member_class->name, '/')) != NULL)
			member_plen = s - member_class->name;
		if ((s = strrchr(calling_class->name, '/')) != NULL)
			calling_plen = s - calling_class->name;
		return calling_plen == member_plen
		    && strncmp(calling_class->name,
		      member_class->name, member_plen) == 0;
	    }
	default:
		_JC_ASSERT(JNI_FALSE);
		return -1;			/* silence compiler warning */
	}
}


