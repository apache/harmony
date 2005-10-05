
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
 * $Id: java_lang_reflect_Method.c,v 1.8 2005/05/15 21:41:01 archiecobbs Exp $
 */

#include "libjc.h"
#include "java_lang_reflect_Method.h"

/* Internal functions */
static _jc_method	*_jc_check_method(_jc_env *env, _jc_object *this);

/*
 * public final native Class[] getExceptionTypes()
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_java_lang_reflect_Method_getExceptionTypes(_jc_env *env, _jc_object *this)
{
	_jc_object_array *array;
	_jc_method *method;

	/* Get method */
	method = _jc_check_method(env, this);

	/* Get exception types */
	if ((array = _jc_get_exception_types(env, method)) == NULL)
		_jc_throw_exception(env);

	/* Return array */
	return array;
}

/*
 * public final native int getModifiers()
 */
jint _JC_JCNI_ATTR
JCNI_java_lang_reflect_Method_getModifiers(_jc_env *env, _jc_object *this)
{
	_jc_method *method;

	/* Get method */
	method = _jc_check_method(env, this);

	/* Return flags */
	return method->access_flags & _JC_ACC_MASK;
}

/*
 * public final native Class[] getParameterTypes()
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_java_lang_reflect_Method_getParameterTypes(_jc_env *env, _jc_object *this)
{
	_jc_object_array *array;
	_jc_method *method;

	/* Get method */
	method = _jc_check_method(env, this);

	/* Get parameter types */
	if ((array = _jc_get_parameter_types(env, method)) == NULL)
		_jc_throw_exception(env);

	/* Return array */
	return array;
}

/*
 * public final native Class getReturnType()
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_reflect_Method_getReturnType(_jc_env *env, _jc_object *this)
{
	_jc_method *method;

	/* Get method */
	method = _jc_check_method(env, this);

	/* Return return type Class instance */
	return method->return_type->instance;
}

/*
 * private final native Object invokeNative(Object, Object[], Class, int)
 *      throws IllegalAccessException, InvocationTargetException
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_reflect_Method_invokeNative(_jc_env *env, _jc_object *this,
	_jc_object *obj, _jc_object_array *params, _jc_object *cl, jint slot)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *calling_class;
	_jc_object *return_obj;
	_jc_method *method;
	int rtype;

	/* Get method */
	method = _jc_check_method(env, this);

	/* Check accessibility */
	if (_jc_invoke_virtual(env,
	    vm->boot.methods.AccessibleObject.isAccessible, this) != JNI_OK)
		_jc_throw_exception(env);
	if (env->retval.z)
		goto accessible;

	/* Check access */
	switch (_jc_reflect_accessible(env, method->class,
	    method->access_flags, &calling_class)) {
	case -1:
		_jc_throw_exception(env);
	case 1:
		break;
	case 0:
		_jc_post_exception_msg(env, _JC_IllegalAccessException,
		    "`%s.%s%s' is not accessible from `%s'",
		    method->class->name, method->name, method->signature,
		    calling_class->name);
		_jc_throw_exception(env);
	}

accessible:
	/* Invoke method */
	if (_jc_reflect_invoke(env, method, obj, params) != JNI_OK)
		_jc_throw_exception(env);

	/* Wrap return value */
	rtype = (method->return_type->flags & _JC_TYPE_MASK);
	switch (rtype) {
	case _JC_TYPE_BOOLEAN:
	case _JC_TYPE_BYTE:
	case _JC_TYPE_CHAR:
	case _JC_TYPE_SHORT:
	case _JC_TYPE_INT:
	case _JC_TYPE_LONG:
	case _JC_TYPE_FLOAT:
	case _JC_TYPE_DOUBLE:
		if ((return_obj = _jc_wrap_primitive(env,
		    rtype, &env->retval)) == NULL)
			_jc_throw_exception(env);
		break;
	case _JC_TYPE_VOID:
		return_obj = NULL;
		break;
	case _JC_TYPE_REFERENCE:
		return_obj = env->retval.l;
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
		return_obj = NULL;		/* silence compiler warning */
		break;
	}

	/* Done */
	return return_obj;
}

/*
 * Find the _jc_method structure corresponding to a Method object.
 */
static _jc_method *
_jc_check_method(_jc_env *env, _jc_object *this)
{
	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Return method */
	return _jc_get_method(env, this);
}

