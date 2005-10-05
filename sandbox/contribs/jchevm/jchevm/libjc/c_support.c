
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
 * $Id: c_support.c,v 1.10 2005/05/14 21:58:24 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * This file contains all C functions called directly by the generated
 * C code. These functions are special because they are allowed to throw
 * exceptions, whereas most other functions in JC indicate exceptions by
 * posting them and returning an error.
 */

/*
 * Return the (possibly newly created) intern'ed String object
 * containing the supplied characters which are encoded in UTF-8.
 *
 * The caller may optionally pass a non-NULL pointer "ref" to a
 * cache location for the result; if "*ref" is not null, it is
 * returned; otherwise, it is set to point to the intern'd String
 * object and an additional implicit reference is added to the
 * class loader that defined the class whose method is calling
 * this function to represent the cached reference.
 */
_jc_object * _JC_JCNI_ATTR
_jc_cs_intern_string_utf8(_jc_env *env, _jc_object **ref, const char *utf)
{
	_jc_object *string;

	/* Does caller already have the reference cached? */
	if (ref != NULL && *ref != NULL)
		return *ref;

	/* Create/get intern'd string */
	if ((string = _jc_new_intern_string(env, utf, strlen(utf))) == NULL)
		_jc_throw_exception(env);

	/* If cache provided, cache reference and add to implicit references */
	if (ref != NULL) {
		_jc_jvm *const vm = env->vm;
		_jc_stack_crawl crawl;
		_jc_resolve_info info;

		/* Create reference list with one reference */
		memset(&info, 0, sizeof(info));
		info.implicit_refs = &string;
		info.num_implicit_refs = 1;

		/* Lock VM */
		_JC_MUTEX_LOCK(env, vm->mutex);

		/* Find which Java method called me and get its loader */
		_jc_stack_crawl_first(env, &crawl);
		_JC_ASSERT(crawl.method != NULL && crawl.method->class != NULL);

		/* Unlock VM */
		_JC_MUTEX_UNLOCK(env, vm->mutex);

		/* Associate reference with calling class' loader */
		info.loader = crawl.method->class->loader;

		/* Add implicit reference from class loader -> String */
		if (_jc_merge_implicit_refs(env, &info) != JNI_OK) {
			_jc_post_exception_info(env);
			_jc_throw_exception(env);
		}

		/* Cache reference in caller-supplied cache location */
		*ref = string;
	}

	/* Done */
	return string;
}

/*
 * Create a new non-array object instance.
 */
_jc_object * _JC_JCNI_ATTR
_jc_cs_new_object(_jc_env *env, _jc_type *type)
{
	_jc_object *obj;

	/* Create object */
	if ((obj = _jc_new_object(env, type)) == NULL)
		_jc_throw_exception(env);

	/* Done */
	return obj;
}

/*
 * Initialize a stack-allocated non-array object instance.
 */
_jc_object * _JC_JCNI_ATTR
_jc_cs_init_object(_jc_env *env, void *mem, _jc_type *type)
{
	_jc_object *obj = mem;

	/* Initialize object */
	if ((obj = _jc_init_object(env, mem, type)) == NULL)
		_jc_throw_exception(env);

	/* Done */
	return obj;
}

/*
 * Create a new array instance.
 */
_jc_array * _JC_JCNI_ATTR
_jc_cs_new_array(_jc_env *env, _jc_type *type, jint len)
{
	_jc_array *array;

	/* Create array */
	if ((array = _jc_new_array(env, type, len)) == NULL)
		_jc_throw_exception(env);

	/* Done */
	return array;
}

/*
 * Initialize a stack-allocated array.
 */
_jc_array * _JC_JCNI_ATTR
_jc_cs_init_array(_jc_env *env, void *mem, _jc_type *type, jint len)
{
	_jc_array *array;

	/* Initialize array */
	if ((array = _jc_init_array(env, mem, type, len)) == NULL)
		_jc_throw_exception(env);

	/* Done */
	return array;
}

/*
 * Create a new multi-dimensional array instance.
 */
_jc_array * _JC_JCNI_ATTR
_jc_cs_new_multiarray(_jc_env *env, _jc_type *type,
	jint num_sizes, const jint *sizes)
{
	_jc_array *array;

	/* Create array */
	if ((array = _jc_new_multiarray(env, type, num_sizes, sizes)) == NULL)
		_jc_throw_exception(env);

	/* Done */
	return array;
}

/*
 * Initialize a stack-allocated multi-dimensional array instance.
 */
_jc_array * _JC_JCNI_ATTR
_jc_cs_init_multiarray(_jc_env *env, void *mem, _jc_type *type,
	jint num_sizes, const jint *sizes)
{
	_jc_array *array;

	/* Initialize array */
	if ((array = _jc_init_multiarray(env,
	    mem, type, num_sizes, sizes)) == NULL)
		_jc_throw_exception(env);

	/* Done */
	return array;
}

/*
 * Perform class initialization on type.
 */
void _JC_JCNI_ATTR
_jc_cs_initialize_type(_jc_env *env, _jc_type *type)
{
	/* Initialize type */
	if (_jc_initialize_type(env, type) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * Determine if 'obj' is an instance of type 'type'.
 */
jboolean _JC_JCNI_ATTR
_jc_cs_instanceof(_jc_env *env, _jc_object *obj, _jc_type *type)
{
	/* Sanity check */
	_JC_ASSERT(obj == NULL || _JC_LW_TEST(obj->lockword, ODD));

	/* Compute instanceofness */
	switch (_jc_instance_of(env, obj, type)) {
	case 0:
		return JNI_FALSE;
	case 1:
		return JNI_TRUE;
	case -1:
		_jc_throw_exception(env);
	default:
		_JC_ASSERT(JNI_FALSE);
		return JNI_FALSE;
	}
}

/*
 * Lookup an interface method.
 */
const void * _JC_JCNI_ATTR
_jc_cs_lookup_interface(_jc_env *env, _jc_object *obj, jlong sig_hash)
{
	_jc_type *const type = obj->type;
	_jc_method *const *methodp;
	int bucket;

	/* Sanity check */
	_JC_ASSERT(obj != NULL && _JC_LW_TEST(obj->lockword, ODD));

	/* Seach object's interface method hash table */
	if (type->imethod_hash_table == NULL)
		goto fail;
	bucket = (int)sig_hash & (_JC_IMETHOD_HASHSIZE - 1);
	methodp = type->imethod_hash_table[bucket];
	if (methodp == NULL)
		goto fail;
	while (*methodp != NULL) {
		if ((*methodp)->signature_hash == sig_hash)
			return (*methodp)->function;
		methodp++;
	}

fail:
	/* Not found; throw exception */
	_jc_post_exception(env, _JC_AbstractMethodError);
	_jc_throw_exception(env);
}

/*
 * Invoke a native method.
 *
 * This function is called from the method "wrapper" function for
 * a native method to invoke the actual native implementation function.
 */
void _JC_JCNI_ATTR
_jc_cs_invoke_native_method(_jc_env *env,
	_jc_method *method, _jc_value *retval, ...)
{
	va_list args;
	jint status;

	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(method, NATIVE));

	/* Invoke method */
	va_start(args, retval);
	status = _jc_invoke_native_method(env, method, JNI_FALSE, args);
	va_end(args);

	/* Throw any posted exception */
	if (status != JNI_OK)
		_jc_throw_exception(env);

	/* Return return value */
	*retval = env->retval;
}

/*
 * Throw an AbstractMethodError for the given abstract method.
 */
void _JC_JCNI_ATTR
_jc_cs_throw_abstract_method_error(_jc_env *env, _jc_method *method)
{
	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(method, ABSTRACT));

	/* Throw exception */
	_jc_post_exception_msg(env, _JC_AbstractMethodError,
	    "%s.%s%s", method->class->name, method->name, method->signature);
	_jc_throw_exception(env);
}

/*
 * Enter object monitor.
 */
void _JC_JCNI_ATTR
_jc_cs_monitorenter(_jc_env *env, _jc_object *obj)
{
	/* Check for NULL */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Sanity check */
	_JC_ASSERT(_JC_LW_TEST(obj->lockword, ODD));

	/* Enter object monitor */
	if (_jc_lock_object(env, obj) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * Exit object monitor.
 */
void _JC_JCNI_ATTR
_jc_cs_monitorexit(_jc_env *env, _jc_object *obj)
{
	/* Check for NULL */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Sanity check */
	_JC_ASSERT(_JC_LW_TEST(obj->lockword, ODD));

	/* Exit object monitor */
	if (_jc_unlock_object(env, obj) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * Throw an ArrayIndexOutOfBoundsException.
 */
void _JC_JCNI_ATTR
_jc_cs_throw_array_index_exception(_jc_env *env, jint indx)
{
	_jc_post_exception_msg(env,
	    _JC_ArrayIndexOutOfBoundsException, "%d", (int)indx);
	_jc_throw_exception(env);
}

/*
 * Throw an ArrayStoreException.
 */
void _JC_JCNI_ATTR
_jc_cs_throw_array_store_exception(_jc_env *env,
	_jc_object *obj, _jc_type *type)
{
	_jc_post_exception_msg(env, _JC_ArrayStoreException,
	    "can't store object of type `%s' into array of `%s'",
	    obj->type->name, type->name);
	_jc_throw_exception(env);
}

/*
 * Throw a ClassCastException.
 */
void _JC_JCNI_ATTR
_jc_cs_throw_class_cast_exception(_jc_env *env, _jc_object *obj, _jc_type *type)
{
	_JC_ASSERT(obj != NULL && _JC_LW_TEST(obj->lockword, ODD));
	_jc_post_exception_msg(env, _JC_ClassCastException,
	    "can't cast `%s' to `%s'", obj->type->name, type->name);
	_jc_throw_exception(env);
}

/*
 * Throw a NullPointerException.
 */
void _JC_JCNI_ATTR
_jc_cs_throw_null_pointer_exception(_jc_env *env)
{
	_jc_post_exception(env, _JC_NullPointerException);
	_jc_throw_exception(env);
}

/*
 * Throw an exception.
 */
void _JC_JCNI_ATTR
_jc_cs_throw(_jc_env *env, _jc_object *obj)
{
	/* Check for NULL */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Sanity check */
	_JC_ASSERT(_jc_subclass_of(obj, env->vm->boot.types.Throwable));

	/* Throw exception */
	_jc_post_exception_object(env, obj);
	_jc_throw_exception(env);
}

/*
 * Panic.
 */
void _JC_JCNI_ATTR
_jc_cs_panic(_jc_env *env, const char *fmt, ...)
{
	va_list args;

	va_start(args, fmt);
	_jc_fatal_error_v(env->vm, fmt, args);
	va_end(args);
}

/*
 * Compute the floating point remainder.
 */
jdouble _JC_JCNI_ATTR
_jc_cs_fmod(jdouble x, jdouble y)
{
	return fmod(x, y);		// XXX corner cases are probably wrong
}


