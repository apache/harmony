
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
 * These functions invoke a Java method. The thread status must already
 * be set to _JC_THRDSTAT_RUNNING_NORMAL or _JC_THRDSTAT_HALTING_NORMAL.
 *
 * Reference method parameters are either "raw" object references or
 * "wrapped" native object references. "Normal" Java code executes
 * using unwrapped references, while JNI functions use "wrapped"
 * references, both when being invoked and when invoking other methods.
 *
 * So there are three flavors of invocation functions defined here:
 *
 * (1) Accepts unwrapped parameters, invokes with unwrapped parameters
 * (2) Accepts wrapped parameters, invokes with unwrapped parameters
 * (3) Accepts unwrapped parameters, invokes with wrapped parameters
 *
 * (1) is used for normal Java, (2) for the JNI Call<Type>XXXMethodX()
 * functions, and (3) for when normal Java needs to invoke a JNI native
 * function. The (2) functions have "unwrap" in their names while the
 * (3) functions have "jni" in their names. In cases (1) and (2) we
 * must catch thrown exceptions. In case (3) exceptions are only posted,
 * not thrown.
 *
 * There are three kinds of Java method invocation: non-virtual, virtual,
 * and static. We don't need virtual for case (3) because native methods
 * have already been virtually resolved by the time we get here.
 *
 * Finally, there are three types of C function that can be used: a
 * variadic funtion, a function that takes a va_list, and a function that
 * takes an array of _jc_word or jvalues.
 *
 * All (1) or (2) functions go through _jc_invoke_jcni_a() which is the
 * "gateway" function between internal libjc code and "normal" JCNI Java
 * object code. Note that it must catch all thrown exceptions (because
 * no exceptions may be thrown within libjc), and so requires a trap table,
 * line number table, etc. All (3) functions go through _jc_invoke_jni_a().
 *
 * If the method returns normally, JNI_OK is returned and the return
 * value from the method is stored in env->retval. Note that the return
 * value is always a _jc_value, even if for the "wrapped" functions.
 * This means that callers of "wrapped" functions returning object
 * references must do the "wrapping" themselves (e.g., by calling
 * "_jc_new_local_native_ref(env, env->retval.l)") before doing anything
 * else that could cause a GC cycle, etc.
 *
 * Otherwise, JNI_ERR is returned and an exception is posted to the thread.
 *
 * Note that it is not necessary for the caller to retain native references
 * to 'this' or any object parameters, as they will be visible to the GC
 * scan via the normal stack scanning mechanism.
 */

/* Internal functions */
static jint	_jc_invoke_unwrap_v(_jc_env *env, _jc_method *method,
			const void *func, jobject obj, va_list args);
static jint	_jc_invoke_unwrap_a(_jc_env *env, _jc_method *method,
			const void *func, jobject obj, jvalue *jparams);

/************************************************************************
 *			Non-virtual method invocation			*
 ************************************************************************/

jint
_jc_invoke_nonvirtual(_jc_env *env, _jc_method *method, _jc_object *this, ...)
{
	va_list args;
	jint status;

	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	va_start(args, this);
	status = _jc_invoke_v(env, method,
	    method->function, this, args, JNI_FALSE);
	va_end(args);
	return status;
}

jint
_jc_invoke_unwrap_nonvirtual(_jc_env *env, _jc_method *method,
	jobject this, ...)
{
	va_list args;
	jint status;

	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	va_start(args, this);
	status = _jc_invoke_unwrap_v(env, method, method->function, this, args);
	va_end(args);
	return status;
}

jint
_jc_invoke_nonvirtual_v(_jc_env *env, _jc_method *method,
	_jc_object *this, va_list args)
{
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	return _jc_invoke_v(env, method,
	    method->function, this, args, JNI_FALSE);
}

jint
_jc_invoke_unwrap_nonvirtual_v(_jc_env *env, _jc_method *method,
	jobject this, va_list args)
{
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	return _jc_invoke_unwrap_v(env, method, method->function, this, args);
}

jint
_jc_invoke_nonvirtual_a(_jc_env *env, _jc_method *method,
	_jc_object *this, _jc_word *params)
{
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	return _jc_invoke_jcni_a(env, method, method->function, this, params);
}

jint
_jc_invoke_unwrap_nonvirtual_a(_jc_env *env, _jc_method *method,
	jobject this, jvalue *params)
{
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	return _jc_invoke_unwrap_a(env, method, method->function, this, params);
}

/************************************************************************
 *			Virtual method invocation			*
 ************************************************************************/

jint
_jc_invoke_virtual(_jc_env *env, _jc_method *method, _jc_object *this, ...)
{
	va_list args;
	jint status;

	va_start(args, this);
	status = _jc_invoke_virtual_v(env, method, this, args);
	va_end(args);
	return status;
}

jint
_jc_invoke_unwrap_virtual(_jc_env *env, _jc_method *method, jobject this, ...)
{
	va_list args;
	jint status;

	va_start(args, this);
	status = _jc_invoke_unwrap_virtual_v(env, method, this, args);
	va_end(args);
	return status;
}

jint
_jc_invoke_virtual_v(_jc_env *env, _jc_method *method,
	_jc_object *this, va_list args)
{
	const void *func;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	_JC_ASSERT(*method->name != '<');

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		return JNI_ERR;
	}

	/* Virtual method lookup */
	func = this->type->vtable[method->vtable_index];

	/* Invoke it */
	return _jc_invoke_v(env, method, func, this, args, JNI_FALSE);
}

jint
_jc_invoke_unwrap_virtual_v(_jc_env *env, _jc_method *method,
	jobject this, va_list args)
{
	const void *func;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	_JC_ASSERT(strcmp(method->name, "<init>") != 0);

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		return JNI_ERR;
	}
	_JC_ASSERT(*this != NULL);

	/* Virtual method lookup */
	func = (*this)->type->vtable[method->vtable_index];

	/* Invoke it */
	return _jc_invoke_unwrap_v(env, method, func, this, args);
}

jint
_jc_invoke_virtual_a(_jc_env *env, _jc_method *method,
	_jc_object *this, _jc_word *params)
{
	const void *func;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	_JC_ASSERT(strcmp(method->name, "<init>") != 0);

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		return JNI_ERR;
	}

	/* Virtual method lookup */
	func = this->type->vtable[method->vtable_index];

	/* Invoke it */
	return _jc_invoke_jcni_a(env, method, func, this, params);
}

jint
_jc_invoke_unwrap_virtual_a(_jc_env *env, _jc_method *method,
	jobject this, jvalue *params)
{
	const void *func;

	/* Sanity check */
	_JC_ASSERT(!_JC_ACC_TEST(method, STATIC));
	_JC_ASSERT(strcmp(method->name, "<init>") != 0);

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		return JNI_ERR;
	}
	_JC_ASSERT(*this != NULL);

	/* Virtual method lookup */
	func = (*this)->type->vtable[method->vtable_index];

	/* Invoke it */
	return _jc_invoke_unwrap_a(env, method, func, this, params);
}

/************************************************************************
 *			Static method invocation			*
 ************************************************************************/

jint
_jc_invoke_static(_jc_env *env, _jc_method *method, ...)
{
	va_list args;
	jint status;

	va_start(args, method);
	status = _jc_invoke_static_v(env, method, args);
	va_end(args);
	return status;
}

jint
_jc_invoke_unwrap_static(_jc_env *env, _jc_method *method, ...)
{
	va_list args;
	jint status;

	va_start(args, method);
	status = _jc_invoke_unwrap_static_v(env, method, args);
	va_end(args);
	return status;
}

jint
_jc_invoke_static_v(_jc_env *env, _jc_method *method, va_list args)
{
	jint status;

	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(method, STATIC));

	/* Initialize class if necessary */
	if (!_JC_FLG_TEST(method->class, INITIALIZED)
	    && (status = _jc_initialize_type(env, method->class)) != JNI_OK)
		return status;

	/* Invoke method */
	return _jc_invoke_v(env, method,
	    method->function, NULL, args, JNI_FALSE);
}

jint
_jc_invoke_unwrap_static_v(_jc_env *env, _jc_method *method, va_list args)
{
	jobject class;
	jint status;

	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(method, STATIC));
	_JC_ASSERT(method->class->instance != NULL);

	/* Initialize class if necessary */
	if (!_JC_FLG_TEST(method->class, INITIALIZED)
	    && (status = _jc_initialize_type(env, method->class)) != JNI_OK)
		return status;

	/* Wrap class object */
	if ((class = _jc_new_local_native_ref(env,
	    method->class->instance)) == NULL)
		return JNI_ERR;

	/* Invoke method */
	status = _jc_invoke_unwrap_v(env,
	    method, method->function, class, args);

	/* Free native reference to class object */
	_jc_free_local_native_ref(&class);

	/* Done */
	return status;
}

jint
_jc_invoke_static_a(_jc_env *env, _jc_method *method, _jc_word *params)
{
	jint status;

	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(method, STATIC));

	/* Initialize class if necessary */
	if (!_JC_FLG_TEST(method->class, INITIALIZED)
	    && (status = _jc_initialize_type(env, method->class)) != JNI_OK)
		return status;

	/* Invoke method */
	return _jc_invoke_jcni_a(env, method, method->function, NULL, params);
}

jint
_jc_invoke_unwrap_static_a(_jc_env *env, _jc_method *method, jvalue *params)
{
	jobject class;
	jint status;

	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(method, STATIC));
	_JC_ASSERT(method->class->instance != NULL);

	/* Initialize class if necessary */
	if (!_JC_FLG_TEST(method->class, INITIALIZED)
	    && (status = _jc_initialize_type(env, method->class)) != JNI_OK)
		return status;

	/* Wrap class object */
	if ((class = _jc_new_local_native_ref(env,
	    method->class->instance)) == NULL)
		return JNI_ERR;

	/* Invoke method */
	status = _jc_invoke_unwrap_a(env,
	    method, method->function, class, params);

	/* Free native reference to class object */
	_jc_free_local_native_ref(&class);

	/* Done */
	return status;
}

/************************************************************************
 *		Generic 'va_list' method invocation			*
 ************************************************************************/

jint
_jc_invoke_v(_jc_env *env, _jc_method *method,
	const void *func, _jc_object *obj, va_list args, jboolean jni)
{
	_jc_word *params;
	int pi;
	int i;

	/* Allocate array of _jc_word's to hold the parameters */
	if ((params = _JC_STACK_ALLOC(env,
	    method->code.num_params2 * sizeof(*params))) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Copy variadic arguments to the _jc_word array */
	for (pi = i = 0; i < method->num_parameters; i++) {
		const int ptype = method->param_ptypes[i];

		switch (ptype) {
		case _JC_TYPE_BOOLEAN:
			params[pi++] = (jint)(jboolean)va_arg(args, jint);
			break;
		case _JC_TYPE_BYTE:
			params[pi++] = (jint)(jbyte)va_arg(args, jint);
			break;
		case _JC_TYPE_CHAR:
			params[pi++] = (jint)(jchar)va_arg(args, jint);
			break;
		case _JC_TYPE_SHORT:
			params[pi++] = (jint)(jshort)va_arg(args, jint);
			break;
		case _JC_TYPE_INT:
			params[pi++] = va_arg(args, jint);
			break;
		case _JC_TYPE_FLOAT:
		    {
			const jfloat param = va_arg(args, jdouble);

			memcpy(params + pi, &param, sizeof(param));
			pi++;
			break;
		    }
		case _JC_TYPE_LONG:
		    {
			const jlong param = va_arg(args, jlong);

			memcpy(params + pi, &param, sizeof(param));
			pi += 2;
			break;
		    }
		case _JC_TYPE_DOUBLE:
		    {
			const jdouble param = va_arg(args, jdouble);

			memcpy(params + pi, &param, sizeof(param));
			pi += 2;
			break;
		    }
		case _JC_TYPE_REFERENCE:
			params[pi++] = (_jc_word)va_arg(args, _jc_object *);
			break;
		default:
			_JC_ASSERT(JNI_FALSE);
			break;
		}
	}

	/* Invoke method */
	return jni ?
	    _jc_invoke_jni_a(env, method, func, obj, params) :
	    _jc_invoke_jcni_a(env, method, func, obj, params);
}

static jint
_jc_invoke_unwrap_v(_jc_env *env, _jc_method *method,
	const void *func, jobject obj, va_list args)
{
	_jc_word *params;
	_jc_object *this;
	int pi;
	int i;

	/* Allocate array of _jc_word's to hold the parameters */
	if ((params = _JC_STACK_ALLOC(env,
	    method->code.num_params2 * sizeof(*params))) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Copy "unwrapped" variadic arguments to the _jc_word array */
	for (pi = i = 0; i < method->num_parameters; i++) {
		const int ptype = method->param_ptypes[i];

		switch (ptype) {
		case _JC_TYPE_BOOLEAN:
			params[pi++] = (jint)(jboolean)va_arg(args, jint);
			break;
		case _JC_TYPE_BYTE:
			params[pi++] = (jint)(jbyte)va_arg(args, jint);
			break;
		case _JC_TYPE_CHAR:
			params[pi++] = (jint)(jchar)va_arg(args, jint);
			break;
		case _JC_TYPE_SHORT:
			params[pi++] = (jint)(jshort)va_arg(args, jint);
			break;
		case _JC_TYPE_INT:
			params[pi++] = va_arg(args, jint);
			break;
		case _JC_TYPE_FLOAT:
		    {
			const jfloat param = va_arg(args, jdouble);

			memcpy(params + pi, &param, sizeof(param));
			pi++;
			break;
		    }
		case _JC_TYPE_LONG:
		    {
			const jlong param = va_arg(args, jlong);

			memcpy(params + pi, &param, sizeof(param));
			pi += 2;
			break;
		    }
		case _JC_TYPE_DOUBLE:
		    {
			const jdouble param = va_arg(args, jdouble);

			memcpy(params + pi, &param, sizeof(param));
			pi += 2;
			break;
		    }
		case _JC_TYPE_REFERENCE:
		    {
			const jobject param = va_arg(args, jobject);

			params[pi++] = (_jc_word)
			    ((param != NULL) ? *param : NULL);
			break;
		    }
		default:
			_JC_ASSERT(JNI_FALSE);
			break;
		}
	}

	/* Unwrap "this" */
	this = (!_JC_ACC_TEST(method, STATIC) && obj != NULL) ? *obj : NULL;

	/* Invoke method */
	return _jc_invoke_jcni_a(env, method, func, this, params);
}

/************************************************************************
 *		Generic 'value list' method invocation			*
 ************************************************************************/

/*
 * Invoke a JNI method using supplied parameters.
 */
jint
_jc_invoke_jni_a(_jc_env *env, _jc_method *method,
	const void *func, _jc_object *obj, _jc_word *params)
{
	JNIEnv *jenv = _JC_ENV2JNI(env);
	jboolean pushed_frame = JNI_FALSE;
	jboolean got_monitor = JNI_FALSE;
	_jc_java_stack java_stack;
	_jc_native_frame frame;
	jint status = JNI_ERR;
	int num_ref_params;
	_jc_word *params2;
	u_char *ptypes;
	jobject ref;
	int nparams2;
	int i;
	int j;

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL
	    || env->status == _JC_THRDSTAT_HALTING_NORMAL);
	_JC_ASSERT(_JC_ACC_TEST(method, NATIVE));
	_JC_ASSERT(func != NULL);

	/* Count number of method parameters, counting long/double twice */
	nparams2 = method->num_parameters;
	for (i = 0; i < method->num_parameters; i++) {
		if (_jc_dword_type[method->param_ptypes[i]])
			nparams2++;
	}

	/* Check for null */
	if (!_JC_ACC_TEST(method, STATIC) && obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(_JC_ACC_TEST(method, STATIC) == (obj == NULL));

	/* JNI requires Class instance passed for static methods */
	if (_JC_ACC_TEST(method, STATIC))
		obj = method->class->instance;

	/* Push a new local native reference frame (allocated on the stack) */
	_jc_push_stack_local_native_frame(env, &frame);
	pushed_frame = JNI_TRUE;

	/* Ensure enough local native references for all parameters */
	num_ref_params = 1;		/* for the implicit object parameter */
	for (i = 0; i < method->num_parameters; i++) {
		const int ptype = method->param_ptypes[i];

		if (ptype == _JC_TYPE_REFERENCE)
			num_ref_params++;
	}
	if (num_ref_params + _JC_NATIVE_REFS_MIN_PER_FRAME
	      > _JC_NATIVE_REFS_PER_FRAME
	    && _jc_extend_local_native_frame(env, num_ref_params
	      + _JC_NATIVE_REFS_MIN_PER_FRAME
	      - _JC_NATIVE_REFS_PER_FRAME) != JNI_OK)
		goto done;

	/* Create array of C function parameters */
	if ((params2 = _JC_STACK_ALLOC(env, (2 + nparams2) * sizeof(*params2)
	    + (2 + method->num_parameters + 1))) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}
	params2[0] = (_jc_word)jenv;
	ref = _jc_new_local_native_ref(env, obj);
	_JC_ASSERT(ref != NULL);
	params2[1] = (_jc_word)ref;
	for (i = 0, j = 0; i < method->num_parameters; i++) {
		const int ptype = method->param_ptypes[i];

		/* Wrap reference parameters in a local native reference */
		if (ptype == _JC_TYPE_REFERENCE) {
			_jc_object *const pobj = (_jc_object *)*params++;

			ref = _jc_new_local_native_ref(env, pobj);
			_JC_ASSERT(pobj == NULL || ref != NULL);
			params2[2 + j++] = (_jc_word)ref;
		} else {
			params2[2 + j++] = *params++;
			if (_jc_dword_type[ptype])
				params2[2 + j++] = *params++;
		}
	}
	_JC_ASSERT(j == nparams2);
	nparams2 += 2;

	/* Set up parameter types */
	ptypes = (u_char *)(params2 + nparams2);
	ptypes[0] = _JC_TYPE_REFERENCE;		/* JNIEnv parameter */
	ptypes[1] = _JC_TYPE_REFERENCE;		/* 'this' or Class object */
	memcpy(ptypes + 2, method->param_ptypes, method->num_parameters + 1);

	/* Synchronized? */
	if (_JC_ACC_TEST(method, SYNCHRONIZED)) {
		if (_jc_lock_object(env, obj) != JNI_OK)
			goto done;
		got_monitor = JNI_TRUE;
	}

	/* Add a Java stack frame */
	memset(&java_stack, 0, sizeof(java_stack));
	java_stack.next = env->java_stack;
	java_stack.method = method;
	env->java_stack = &java_stack;

	/* Going native */
	_jc_stopping_java(env, NULL, NULL);

	/* Invoke the method */
	_jc_dynamic_invoke(func, JNI_FALSE, 2 + method->num_parameters,
	    ptypes, nparams2, params2, &env->retval);

	/* Returning from native */
	_jc_resuming_java(env, NULL);

	/* Pop Java stack */
	env->java_stack = java_stack.next;

	/* Return an error if an exception was posted */
	status = (env->pending != NULL) ? JNI_ERR : JNI_OK;

done:
	/* Pop local native reference frame */
	if (pushed_frame)
		_jc_pop_local_native_frame(env, NULL);

	/* Synchronized? */
	if (got_monitor) {
		_jc_rvalue retval;
		jint status2;

		retval = env->retval;
		if ((status2 = _jc_unlock_object(env, obj)) != JNI_OK)
			status = status2;
		env->retval = retval;
	}

	/*
	 * If an exception was thrown, reset return value for good measure.
	 * Otherwise, convert jvalue -> _jc_value.
	 */
	if (status != JNI_OK)
		memset(&env->retval, 0, sizeof(env->retval));
	else if (method->param_ptypes[method->num_parameters]
	    == _JC_TYPE_REFERENCE) {
		ref = (jobject)env->retval.l;
		env->retval.l = (ref != NULL) ? *ref : NULL;
	}

	/* Done */
	return status;
}

/*
 * Invoke a JCNI method using supplied 'wrapped' parameters.
 */
static jint
_jc_invoke_unwrap_a(_jc_env *env, _jc_method *method,
	const void *func, jobject obj, jvalue *jparams)
{
	_jc_word *params;
	_jc_object *this;
	int pi;
	int i;

	/* Allocate array of _jc_word's to hold the parameters */
	if ((params = _JC_STACK_ALLOC(env,
	    method->code.num_params2 * sizeof(*params))) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Copy "unwrapped" variadic arguments to the _jc_word array */
	for (pi = i = 0; i < method->num_parameters; i++) {
		const int ptype = method->param_ptypes[i];

		switch (ptype) {
		case _JC_TYPE_BOOLEAN:
			params[pi++] = (jint)jparams[i].z;
			break;
		case _JC_TYPE_BYTE:
			params[pi++] = (jint)jparams[i].b;
			break;
		case _JC_TYPE_CHAR:
			params[pi++] = (jint)jparams[i].c;
			break;
		case _JC_TYPE_SHORT:
			params[pi++] = (jint)jparams[i].s;
			break;
		case _JC_TYPE_INT:
			params[pi++] = jparams[i].i;
			break;
		case _JC_TYPE_FLOAT:
		    {
			const jfloat param = jparams[i].f;

			memcpy(params + pi, &param, sizeof(param));
			pi++;
			break;
		    }
		case _JC_TYPE_LONG:
		    {
			const jlong param = jparams[i].j;

			memcpy(params + pi, &param, sizeof(param));
			pi += 2;
			break;
		    }
		case _JC_TYPE_DOUBLE:
		    {
			const jdouble param = jparams[i].d;

			memcpy(params + pi, &param, sizeof(param));
			pi += 2;
			break;
		    }
		case _JC_TYPE_REFERENCE:
		    {
			const jobject param = jparams[i].l;

			params[pi++] = (_jc_word)
			    ((param != NULL) ? *param : NULL);
			break;
		    }
		default:
			_JC_ASSERT(JNI_FALSE);
			break;
		}
	}

	/* Unwrap "this" */
	this = (!_JC_ACC_TEST(method, STATIC) && obj != NULL) ? *obj : NULL;

	/* Invoke method */
	return _jc_invoke_jcni_a(env, method, func, this, params);
}

/*
 * Invoke a JCNI method using supplied 'value list' parameters.
 *
 * This function is the main "gateway" from the libjc world (where exceptions
 * are posted, not thrown) to the Java world (where exceptions are explicitly
 * thrown by unwinding the stack). Any exceptions thrown by the invoked method
 * cause this function to return JNI_ERR instead of JNI_OK (and the field
 * env->pending contains the exception).
 *
 * In order to catch exceptions thrown by Java code, we rely on the function
 * _jc_throw_exception() specially recognizing this function on the stack.
 * Therefore we have to look somewhat like a compiled Java method, with a
 * method table entry and line number table, etc.
 *
 * If the method is static, 'obj' should be NULL and is not passed as a
 * parameter as JCNI does not pass the Class object to static methods.
 */
jint
_jc_invoke_jcni_a(_jc_env *env, _jc_method *method,
	const void *func, _jc_object *volatile obj, _jc_word *volatile params)
{
	_jc_jvm *const vm = env->vm;
#ifndef NDEBUG
	const volatile jboolean was_interpreting = env->interpreting;
#endif
	volatile jboolean pushed_java_stack = JNI_FALSE;
	volatile jboolean pushed_native_frame = JNI_FALSE;
	volatile jboolean set_stack_limit = JNI_FALSE;
	volatile jboolean got_monitor = JNI_FALSE;
	volatile jint status = JNI_ERR;
	_jc_c_stack *volatile saved_c_stack;
	_jc_word *volatile params2 = NULL;		/* avoid gcc warning */
	_jc_java_stack java_stack;
	_jc_native_frame frame;
	_jc_catch_frame catch;
	u_char *ptypes;
	int nparams2;
	int i;

	/* Catch exceptions here */
	catch.next = env->catch_list;
	env->catch_list = &catch;
	saved_c_stack = env->c_stack;
	if (sigsetjmp(catch.context, 0) != 0) {
		env->c_stack = saved_c_stack;	/* pop C stack chunks */
		goto exception;
	}

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL
	    || env->status == _JC_THRDSTAT_HALTING_NORMAL);

	/* Check for null (non-static methods only) */
	if (!_JC_ACC_TEST(method, STATIC) && obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(_JC_ACC_TEST(method, STATIC) == (obj == NULL));

	/* Push a new local native reference frame (allocated on the stack) */
	_jc_push_stack_local_native_frame(env, &frame);
	pushed_native_frame = JNI_TRUE;

	/* Mark the stack overflow limit */
	if (env->stack_limit == NULL) {
		if (env->stack_size == 0)
			env->stack_size = vm->threads.stack_default;
#if _JC_DOWNWARD_STACK
		env->stack_limit = (char *)&env - env->stack_size
		    + _JC_STACK_OVERFLOW_MARGIN;
#else
		env->stack_limit = (char *)&env + env->stack_size
		    - _JC_STACK_OVERFLOW_MARGIN;
#endif
		set_stack_limit = JNI_TRUE;
	}

	/* Sanity check */
	_JC_ASSERT(func != NULL);

	/* Count number of method parameters, counting long/double twice */
	nparams2 = method->num_parameters;
	for (i = 0; i < method->num_parameters; i++) {
		if (_jc_dword_type[method->param_ptypes[i]])
			nparams2++;
	}

	/* Create array of _jc_word's for the C function parameters */
	if ((params2 = _JC_STACK_ALLOC(env, (2 + nparams2) * sizeof(*params2)
	    + (2 + method->num_parameters + 1))) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}
	if (!_JC_ACC_TEST(method, STATIC)) {
		params2[0] = (_jc_word)env;
		params2[1] = (_jc_word)obj;
		memcpy(params2 + 2, params, nparams2 * sizeof(*params2));
		nparams2 += 2;
		ptypes = (u_char *)(params2 + nparams2);
		ptypes[0] = _JC_TYPE_REFERENCE;
		ptypes[1] = _JC_TYPE_REFERENCE;
		memcpy(ptypes + 2, method->param_ptypes,
		    method->num_parameters + 1);
	} else {
		params2[0] = (_jc_word)env;
		memcpy(params2 + 1, params, nparams2 * sizeof(*params2));
		nparams2++;
		ptypes = (u_char *)(params2 + nparams2);
		ptypes[0] = _JC_TYPE_REFERENCE;
		memcpy(ptypes + 1, method->param_ptypes,
		    method->num_parameters + 1);
	}

	/* Synchronize native methods; non-native code synchronizes itself */
	if (_JC_ACC_TEST(method, SYNCHRONIZED)
	    && _JC_ACC_TEST(method, NATIVE)) {
		if (_JC_ACC_TEST(method, STATIC))
			obj = method->class->instance;
		if (_jc_lock_object(env, obj) != JNI_OK)
			goto done;
		got_monitor = JNI_TRUE;
	}

	/* Add a Java stack frame for native methods */
	if (_JC_ACC_TEST(method, NATIVE)) {
		memset(&java_stack, 0, sizeof(java_stack));
		java_stack.next = env->java_stack;
		java_stack.method = method;
		env->java_stack = &java_stack;
		pushed_java_stack = JNI_TRUE;
	}

#ifndef NDEBUG
	/* Signals are OK now */
	env->interpreting = JNI_FALSE;
#endif

	/* Invoke the method */
	_jc_dynamic_invoke(func, JNI_TRUE,
	    (_JC_ACC_TEST(method, STATIC) ? 1 : 2) + method->num_parameters,
	    ptypes, nparams2, params2, &env->retval);

	/* No exceptions were thrown */
	status = JNI_OK;
	goto done;

exception:
	/* Handle any caught exceptions by re-posting them */
	_jc_post_exception_object(env, env->caught);
	env->caught = NULL;
	status = JNI_ERR;

done:
	/* Pop local native reference frame and Java stack frame segment */
	if (pushed_java_stack)
		env->java_stack = java_stack.next;
	if (pushed_native_frame)
		_jc_pop_local_native_frame(env, NULL);
	if (set_stack_limit)
		env->stack_limit = NULL;

	/* Synchronized? */
	if (got_monitor) {
		_jc_rvalue retval;
		jint status2;

		retval = env->retval;
		if ((status2 = _jc_unlock_object(env, obj)) != JNI_OK)
			status = status2;
		env->retval = retval;
	}

	/* If an exception was thrown, reset return value */
	if (status != JNI_OK)
		memset(&env->retval, 0, sizeof(env->retval));

	/* Unlink exception catcher */
	env->catch_list = catch.next;

#ifndef NDEBUG
	/* Repair debug flag */
	env->interpreting = was_interpreting;
#endif

	/* Done */
	return status;
}

