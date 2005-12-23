
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

static jobject
GetObjectField(JNIEnv *jenv, jobject obj, jfieldID field)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_object *fobj = NULL;
	_jc_c_stack cstack;
	jobject ref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*obj != NULL);

	/* Sanity check */
	_JC_ASSERT(_jc_subclass_of(*obj, field->class));

	/* Get object field */
	fobj = *(_jc_object **)((char *)(*obj) + field->offset);

done:
	/* Returning to native code */
	ref = _jc_pop_local_native_frame(env, fobj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

#define GetField(_type, Type)						\
static j ## _type							\
Get ## Type ## Field(JNIEnv *jenv, jobject obj, jfieldID field)		\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
	j ## _type value = 0;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Check for null */						\
	if (obj == NULL) {						\
		_jc_post_exception(env, _JC_NullPointerException);	\
		goto done;						\
	}								\
	_JC_ASSERT(*obj != NULL);					\
									\
	/* Sanity check */						\
	_JC_ASSERT(_jc_subclass_of(*obj, field->class));		\
									\
	/* Get field */							\
	value = *(j ## _type *)((char *)(*obj) + field->offset);	\
									\
done:									\
	/* Returning to native code */					\
	_jc_pop_local_native_frame(env, NULL);				\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Done */							\
	return value;							\
}
GetField(boolean, Boolean)
GetField(byte, Byte)
GetField(short, Short)
GetField(char, Char)
GetField(int, Int)
GetField(long, Long)
GetField(float, Float)
GetField(double, Double)

static void
SetObjectField(JNIEnv *jenv, jobject obj, jfieldID field, jobject value)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*obj != NULL);

	/* Sanity check */
	_JC_ASSERT(_jc_subclass_of(*obj, field->class));
	_JC_ASSERT(value == NULL || *value != NULL);

	/* Set field */
	*(_jc_object **)((char *)(*obj) + field->offset)
	    = (value == NULL) ? NULL : *value;

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

#define SetField(_type, Type)						\
static void								\
Set ## Type ## Field(JNIEnv *jenv, jobject obj,				\
	jfieldID field, j ## _type value)				\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Check for null */						\
	if (obj == NULL) {						\
		_jc_post_exception(env, _JC_NullPointerException);	\
		goto done;						\
	}								\
	_JC_ASSERT(*obj != NULL);					\
									\
	/* Sanity check */						\
	_JC_ASSERT(_jc_subclass_of(*obj, field->class));		\
									\
	/* Set field */							\
	*(j ## _type *)((char *)(*obj) + field->offset) = value;	\
									\
done:									\
	/* Returning to native code */					\
	_jc_pop_local_native_frame(env, NULL);				\
	_jc_stopping_java(env, &cstack, NULL);				\
}
SetField(boolean, Boolean)
SetField(byte, Byte)
SetField(short, Short)
SetField(char, Char)
SetField(int, Int)
SetField(long, Long)
SetField(float, Float)
SetField(double, Double)

static jobject
GetStaticObjectField(JNIEnv *jenv, jclass class, jfieldID field)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_c_stack cstack;
	_jc_type *type;
	jobject ref = NULL;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Sanity check */
	_JC_ASSERT((*class)->type == vm->boot.types.Class);

	/* Get class type */
	type = *_JC_VMFIELD(vm, *class, Class, vmdata, _jc_type *);

	/* Sanity check */
	_JC_ASSERT(type == field->class);

	/* Get field wrapped in a native reference */
	ref = _jc_new_local_native_ref(env,
	    *(_jc_object **)((char *)type->u.nonarray.class_fields
	      + field->offset));

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

#define GetStaticField(_type, Type)					\
static j ## _type							\
GetStatic ## Type ## Field(JNIEnv *jenv, jclass class, jfieldID field)	\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_jvm *const vm = env->vm;					\
	_jc_c_stack cstack;						\
	_jc_type *type;							\
	j ## _type value;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
									\
	/* Sanity check */						\
	_JC_ASSERT((*class)->type == vm->boot.types.Class);		\
									\
	/* Get class type */						\
	type = *_JC_VMFIELD(vm, *class, Class, vmdata, _jc_type *);	\
									\
	/* Sanity check */						\
	_JC_ASSERT(type == field->class);				\
									\
	/* Get field */							\
	value = *(j ## _type *)((char *)type->u.nonarray.class_fields	\
	    + field->offset);						\
									\
	/* Returning to native code */					\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Done */							\
	return value;							\
}
GetStaticField(boolean, Boolean)
GetStaticField(byte, Byte)
GetStaticField(short, Short)
GetStaticField(char, Char)
GetStaticField(int, Int)
GetStaticField(long, Long)
GetStaticField(float, Float)
GetStaticField(double, Double)

static void
SetStaticObjectField(JNIEnv *jenv, jclass class, jfieldID field, jobject value)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_c_stack cstack;
	_jc_type *type;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Sanity check */
	_JC_ASSERT((*class)->type == vm->boot.types.Class);
	_JC_ASSERT(value == NULL || *value != NULL);

	/* Get class type */
	type = *_JC_VMFIELD(vm, *class, Class, vmdata, _jc_type *);

	/* Sanity check */
	_JC_ASSERT(type == field->class);

	/* Set field */
	*(_jc_object **)((char *)type->u.nonarray.class_fields
	    + field->offset) = (value == NULL) ? NULL : *value;

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);
}

#define SetStaticField(_type, Type)					\
static void								\
SetStatic ## Type ## Field(JNIEnv *jenv, jclass class,			\
	jfieldID field, j ## _type value)				\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_jvm *const vm = env->vm;					\
	_jc_c_stack cstack;						\
	_jc_type *type;							\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
									\
	/* Sanity check */						\
	_JC_ASSERT((*class)->type == vm->boot.types.Class);		\
									\
	/* Get class type */						\
	type = *_JC_VMFIELD(vm, *class, Class, vmdata, _jc_type *);	\
									\
	/* Sanity check */						\
	_JC_ASSERT(type == field->class);				\
									\
	/* Set field */							\
	*(j ## _type *)((char *)type->u.nonarray.class_fields		\
	    + field->offset) = value;					\
									\
	/* Returning to native code */					\
	_jc_stopping_java(env, &cstack, NULL);				\
}
SetStaticField(boolean, Boolean)
SetStaticField(byte, Byte)
SetStaticField(short, Short)
SetStaticField(char, Char)
SetStaticField(int, Int)
SetStaticField(long, Long)
SetStaticField(float, Float)
SetStaticField(double, Double)

static void
CallNonvirtualVoidMethod(JNIEnv *jenv, jobject obj,
	jclass class, jmethodID method, ...)
{
	va_list args;

	va_start(args, method);
	(*jenv)->CallNonvirtualVoidMethodV(jenv, obj, class, method, args);
	va_end(args);
}

static void
CallNonvirtualVoidMethodA(JNIEnv *jenv, jobject obj,
	jclass class, jmethodID method, jvalue *args)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Invoke method */
	_jc_invoke_unwrap_nonvirtual_a(env, method, obj, args);

	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

static void
CallNonvirtualVoidMethodV(JNIEnv *jenv, jobject obj,
	jclass class, jmethodID method, va_list args)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Invoke method */
	_jc_invoke_unwrap_nonvirtual_v(env, method, obj, args);

	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

#define CallNonvirtualMethod(_type, Type)				\
static j ## _type							\
CallNonvirtual ## Type ## Method(JNIEnv *jenv, jobject obj,		\
	jclass class, jmethodID method, ...)				\
{									\
	va_list args;							\
	j ## _type rtn;							\
									\
	va_start(args, method);						\
	rtn = (*jenv)->CallNonvirtual ## Type ## MethodV(jenv,		\
	    obj, class, method, args);					\
	va_end(args);							\
	return rtn;							\
}
CallNonvirtualMethod(boolean, Boolean)
CallNonvirtualMethod(byte, Byte)
CallNonvirtualMethod(short, Short)
CallNonvirtualMethod(char, Char)
CallNonvirtualMethod(int, Int)
CallNonvirtualMethod(long, Long)
CallNonvirtualMethod(float, Float)
CallNonvirtualMethod(double, Double)
CallNonvirtualMethod(object, Object)

#define CallNonvirtualMethodA(_type, Type, expr1, expr2)		\
static j ## _type							\
CallNonvirtual ## Type ## MethodA(JNIEnv *jenv, jobject obj,		\
	jclass class, jmethodID method, jvalue *args)			\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
	j ## _type result;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Invoke method */						\
	_jc_invoke_unwrap_nonvirtual_a(env, method, obj, args);		\
									\
	/* Get result & pop native frame */				\
	expr1 _jc_pop_local_native_frame(env, expr2);			\
									\
	/* Returning to native code */					\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Done */							\
	return result;							\
}
CallNonvirtualMethodA(boolean, Boolean, result = (jboolean)env->retval.i;, NULL)
CallNonvirtualMethodA(byte, Byte, result = (jbyte)env->retval.i;, NULL)
CallNonvirtualMethodA(short, Short, result = (jshort)env->retval.i;, NULL)
CallNonvirtualMethodA(char, Char, result = (jchar)env->retval.i;, NULL)
CallNonvirtualMethodA(int, Int, result = env->retval.i;, NULL)
CallNonvirtualMethodA(long, Long, result = env->retval.j;, NULL)
CallNonvirtualMethodA(float, Float, result = env->retval.f;, NULL)
CallNonvirtualMethodA(double, Double, result = env->retval.d;, NULL)
CallNonvirtualMethodA(object, Object, result =, env->retval.l)

#define CallNonvirtualMethodV(_type, Type, expr1, expr2)		\
static j ## _type							\
CallNonvirtual ## Type ## MethodV(JNIEnv *jenv, jobject obj,		\
	jclass class, jmethodID method, va_list args)			\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
	j ## _type result;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Invoke method */						\
	_jc_invoke_unwrap_nonvirtual_v(env, method, obj, args);		\
									\
	/* Get result & pop native frame */				\
	expr1 _jc_pop_local_native_frame(env, expr2);			\
									\
	/* Returning to native code */					\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Done */							\
	return result;							\
}
CallNonvirtualMethodV(boolean, Boolean, result = (jboolean)env->retval.i;, NULL)
CallNonvirtualMethodV(byte, Byte, result = (jbyte)env->retval.i;, NULL)
CallNonvirtualMethodV(short, Short, result = (jshort)env->retval.i;, NULL)
CallNonvirtualMethodV(char, Char, result = (jchar)env->retval.i;, NULL)
CallNonvirtualMethodV(int, Int, result = env->retval.i;, NULL)
CallNonvirtualMethodV(long, Long, result = env->retval.j;, NULL)
CallNonvirtualMethodV(float, Float, result = env->retval.f;, NULL)
CallNonvirtualMethodV(double, Double, result = env->retval.d;, NULL)
CallNonvirtualMethodV(object, Object, result =, env->retval.l)

static void
CallVoidMethod(JNIEnv *jenv, jobject obj, jmethodID method, ...)
{
	va_list args;

	va_start(args, method);
	(*jenv)->CallVoidMethodV(jenv, obj, method, args);
	va_end(args);
}

static void
CallVoidMethodA(JNIEnv *jenv, jobject obj, jmethodID method, jvalue *args)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Invoke method */
	_jc_invoke_unwrap_virtual_a(env, method, obj, args);

	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

static void
CallVoidMethodV(JNIEnv *jenv, jobject obj, jmethodID method, va_list args)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Invoke method */
	_jc_invoke_unwrap_virtual_v(env, method, obj, args);

	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

#define CallMethod(_type, Type)						\
static j ## _type							\
Call ## Type ## Method(JNIEnv *jenv, jobject obj,			\
	jmethodID method, ...)						\
{									\
	va_list args;							\
	j ## _type rtn;							\
									\
	va_start(args, method);						\
	rtn = (*jenv)->Call ## Type ## MethodV(jenv,			\
	    obj, method, args);						\
	va_end(args);							\
	return rtn;							\
}
CallMethod(boolean, Boolean)
CallMethod(byte, Byte)
CallMethod(short, Short)
CallMethod(char, Char)
CallMethod(int, Int)
CallMethod(long, Long)
CallMethod(float, Float)
CallMethod(double, Double)
CallMethod(object, Object)

#define CallMethodA(_type, Type, expr1, expr2)				\
static j ## _type							\
Call ## Type ## MethodA(JNIEnv *jenv, jobject obj,			\
	jmethodID method, jvalue *args)					\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
	j ## _type result;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Invoke method */						\
	_jc_invoke_unwrap_virtual_a(env, method, obj, args);		\
									\
	/* Get result & pop native frame */				\
	expr1 _jc_pop_local_native_frame(env, expr2);			\
									\
	/* Returning to native code */					\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Done */							\
	return result;							\
}
CallMethodA(boolean, Boolean, result = (jboolean)env->retval.i;, NULL)
CallMethodA(byte, Byte, result = (jbyte)env->retval.i;, NULL)
CallMethodA(short, Short, result = (jshort)env->retval.i;, NULL)
CallMethodA(char, Char, result = (jchar)env->retval.i;, NULL)
CallMethodA(int, Int, result = env->retval.i;, NULL)
CallMethodA(long, Long, result = env->retval.j;, NULL)
CallMethodA(float, Float, result = env->retval.f;, NULL)
CallMethodA(double, Double, result = env->retval.d;, NULL)
CallMethodA(object, Object, result =, env->retval.l)

#define CallMethodV(_type, Type, expr1, expr2)				\
static j ## _type							\
Call ## Type ## MethodV(JNIEnv *jenv, jobject obj,			\
	jmethodID method, va_list args)					\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
	j ## _type result;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Invoke method */						\
	_jc_invoke_unwrap_virtual_v(env, method, obj, args);		\
									\
	/* Get result & pop native frame */				\
	expr1 _jc_pop_local_native_frame(env, expr2);			\
									\
	/* Returning to native code */					\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Done */							\
	return result;							\
}
CallMethodV(boolean, Boolean, result = (jboolean)env->retval.i;, NULL)
CallMethodV(byte, Byte, result = (jbyte)env->retval.i;, NULL)
CallMethodV(short, Short, result = (jshort)env->retval.i;, NULL)
CallMethodV(char, Char, result = (jchar)env->retval.i;, NULL)
CallMethodV(int, Int, result = env->retval.i;, NULL)
CallMethodV(long, Long, result = env->retval.j;, NULL)
CallMethodV(float, Float, result = env->retval.f;, NULL)
CallMethodV(double, Double, result = env->retval.d;, NULL)
CallMethodV(object, Object, result =, env->retval.l)

static void
CallStaticVoidMethod(JNIEnv *jenv, jclass class, jmethodID method, ...)
{
	va_list args;

	va_start(args, method);
	(*jenv)->CallStaticVoidMethodV(jenv, class, method, args);
	va_end(args);
}

static void
CallStaticVoidMethodA(JNIEnv *jenv, jclass class,
	jmethodID method, jvalue *args)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Invoke method */
	_jc_invoke_unwrap_static_a(env, method, args);

	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

static void
CallStaticVoidMethodV(JNIEnv *jenv, jclass class,
	jmethodID method, va_list args)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Invoke method */
	_jc_invoke_unwrap_static_v(env, method, args);

	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

#define CallStaticMethod(_type, Type)					\
static j ## _type							\
CallStatic ## Type ## Method(JNIEnv *jenv,				\
	jclass class, jmethodID method, ...)				\
{									\
	va_list args;							\
	j ## _type rtn;							\
									\
	va_start(args, method);						\
	rtn = (*jenv)->CallStatic ## Type ## MethodV(jenv,		\
	    class, method, args);					\
	va_end(args);							\
	return rtn;							\
}
CallStaticMethod(boolean, Boolean)
CallStaticMethod(byte, Byte)
CallStaticMethod(short, Short)
CallStaticMethod(char, Char)
CallStaticMethod(int, Int)
CallStaticMethod(long, Long)
CallStaticMethod(float, Float)
CallStaticMethod(double, Double)
CallStaticMethod(object, Object)

#define CallStaticMethodA(_type, Type, expr1, expr2)			\
static j ## _type							\
CallStatic ## Type ## MethodA(JNIEnv *jenv,				\
	jclass class, jmethodID method,	jvalue *args)			\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
	j ## _type result;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Invoke method */						\
	_jc_invoke_unwrap_static_a(env, method, args);			\
									\
	/* Get result & pop native frame */				\
	expr1 _jc_pop_local_native_frame(env, expr2);			\
									\
	/* Returning to native code */					\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Done */							\
	return result;							\
}
CallStaticMethodA(boolean, Boolean, result = (jboolean)env->retval.i;, NULL)
CallStaticMethodA(byte, Byte, result = (jbyte)env->retval.i;, NULL)
CallStaticMethodA(short, Short, result = (jshort)env->retval.i;, NULL)
CallStaticMethodA(char, Char, result = (jchar)env->retval.i;, NULL)
CallStaticMethodA(int, Int, result = env->retval.i;, NULL)
CallStaticMethodA(long, Long, result = env->retval.j;, NULL)
CallStaticMethodA(float, Float, result = env->retval.f;, NULL)
CallStaticMethodA(double, Double, result = env->retval.d;, NULL)
CallStaticMethodA(object, Object, result =, env->retval.l)

#define CallStaticMethodV(_type, Type, expr1, expr2)			\
static j ## _type							\
CallStatic ## Type ## MethodV(JNIEnv *jenv,				\
	jclass class, jmethodID method,	va_list args)			\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
	j ## _type result;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Invoke method */						\
	_jc_invoke_unwrap_static_v(env, method, args);			\
									\
	/* Get result & pop native frame */				\
	expr1 _jc_pop_local_native_frame(env, expr2);			\
									\
	/* Returning to native code */					\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Done */							\
	return result;							\
}
CallStaticMethodV(boolean, Boolean, result = (jboolean)env->retval.i;, NULL)
CallStaticMethodV(byte, Byte, result = (jbyte)env->retval.i;, NULL)
CallStaticMethodV(short, Short, result = (jshort)env->retval.i;, NULL)
CallStaticMethodV(char, Char, result = (jchar)env->retval.i;, NULL)
CallStaticMethodV(int, Int, result = env->retval.i;, NULL)
CallStaticMethodV(long, Long, result = env->retval.j;, NULL)
CallStaticMethodV(float, Float, result = env->retval.f;, NULL)
CallStaticMethodV(double, Double, result = env->retval.d;, NULL)
CallStaticMethodV(object, Object, result =, env->retval.l)

static jfieldID
GetEitherFieldID(JNIEnv *jenv, jclass class, const char *name,
	const char *sig, int is_static)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_field *field = NULL;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	_jc_type *type;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Get class info */
	type = *_JC_VMFIELD(vm, *class, Class, vmdata, _jc_type *);

	/* Initialize class */
	if (_jc_initialize_type(env, type) != JNI_OK)
		goto done;

	/* Search for field */
	if ((field = _jc_resolve_field(env,
	    type, name, sig, is_static)) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return field;
}

static jfieldID
GetFieldID(JNIEnv *jenv, jclass class, const char *name, const char *sig)
{
	return GetEitherFieldID(jenv, class, name, sig, JNI_FALSE);
}

static jfieldID
GetStaticFieldID(JNIEnv *jenv, jclass class, const char *name, const char *sig)
{
	return GetEitherFieldID(jenv, class, name, sig, JNI_TRUE);
}

static jmethodID
GetEitherMethodID(JNIEnv *jenv, jclass class,
	const char *name, const char *sig, int flags_mask, int flags)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_method *method = NULL;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	_jc_type *type;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Get class info */
	type = *_JC_VMFIELD(vm, *class, Class, vmdata, _jc_type *);

	/* Initialize class */
	if (_jc_initialize_type(env, type) != JNI_OK)
		goto done;

	/* Search for method */
	if ((method = _jc_resolve_method(env, type, name, sig)) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}

	/* Check method's flags */
	if ((method->access_flags & flags_mask) != flags) {
		_jc_post_exception_msg(env, _JC_NoSuchMethodError,
		    "%s%s.%s", type->name, name, sig);
		method = NULL;
		goto done;
	}

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return method;
}

static jmethodID
GetMethodID(JNIEnv *jenv, jclass class, const char *name, const char *sig)
{
	return GetEitherMethodID(jenv, class, name, sig, _JC_ACC_STATIC, 0);
}

static jmethodID
GetStaticMethodID(JNIEnv *jenv, jclass class, const char *name, const char *sig)
{
	return GetEitherMethodID(jenv,
	    class, name, sig, _JC_ACC_STATIC, _JC_ACC_STATIC);
}

static jclass
GetObjectClass(JNIEnv *jenv, jobject ref)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_object *clobj = NULL;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jclass class;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (ref == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*ref != NULL);

	/* Get object class */
	clobj = (*ref)->type->instance;

done:
	/* Returning to native code */
	class = _jc_pop_local_native_frame(env, clobj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Return class */
	return class;
}

#define GetArrayElements(_type, Type)					\
static j ## _type *							\
Get ## Type ## ArrayElements(JNIEnv *jenv, j ## _type ## Array array,	\
	jboolean *isCopy)						\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_ ## _type ## _array *a;					\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
	jvalue *buf = NULL;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Check for null */						\
	if (array == NULL) {						\
		_jc_post_exception(env, _JC_NullPointerException);	\
		goto done;						\
	}								\
	_JC_ASSERT(*array != NULL);					\
	a = *array;							\
									\
	/* Allocate buffer */						\
	if ((buf = _jc_vm_alloc(env, sizeof(*buf)			\
	    + (a->length * sizeof(*a->elems)))) == NULL) {		\
		_jc_post_exception_info(env);				\
		goto done;						\
	}								\
									\
	/*								\
	 * Add a global reference to the array secretly stored		\
	 * at the beginning of the buffer.				\
	 */								\
	if ((buf->l = _jc_new_global_native_ref(env,			\
	    (_jc_object *)a)) == NULL) {				\
		_jc_post_exception_info(env);				\
		_jc_vm_free(&buf);					\
		goto done;						\
	}								\
									\
	/* Copy array elements */					\
	memcpy(buf + 1, a->elems, a->length * sizeof(*a->elems));	\
	if (isCopy != NULL)						\
		*isCopy = JNI_TRUE;					\
									\
done:									\
	/* Returning to native code */					\
	_jc_pop_local_native_frame(env, NULL);				\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Return elements */						\
	return (j ## _type *)(buf != NULL ? buf + 1 : NULL);		\
}
GetArrayElements(boolean, Boolean)
GetArrayElements(byte, Byte)
GetArrayElements(char, Char)
GetArrayElements(short, Short)
GetArrayElements(int, Int)
GetArrayElements(long, Long)
GetArrayElements(float, Float)
GetArrayElements(double, Double)

#define ReleaseArrayElements(_type, Type)				\
static void								\
Release ## Type ## ArrayElements(JNIEnv *jenv,				\
	j ## _type ## Array array, j ## _type *elems, jint mode)	\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	jvalue *buf = (jvalue *)elems - 1;				\
	_jc_ ## _type ## _array *a;					\
	_jc_c_stack cstack;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
									\
	/* Sanity check */						\
	_JC_ASSERT(elems != NULL && array != NULL && *array != NULL);	\
	_JC_ASSERT(buf->l != NULL && *buf->l == (_jc_object *)*array);	\
	a = *array;							\
									\
	/* Copy back array elements */					\
	if (mode != JNI_ABORT)						\
		memcpy(a->elems, elems, a->length * sizeof(*a->elems));	\
									\
	/* Free the buffer */						\
	if (mode != JNI_COMMIT) {					\
		_jc_free_global_native_ref(&buf->l);			\
		_jc_vm_free(&buf);					\
	}								\
									\
	/* Returning to native code */					\
	_jc_stopping_java(env, &cstack, NULL);				\
}
ReleaseArrayElements(boolean, Boolean)
ReleaseArrayElements(byte, Byte)
ReleaseArrayElements(char, Char)
ReleaseArrayElements(short, Short)
ReleaseArrayElements(int, Int)
ReleaseArrayElements(long, Long)
ReleaseArrayElements(float, Float)
ReleaseArrayElements(double, Double)

static jobject
GetObjectArrayElement(JNIEnv *jenv, jobjectArray array, jsize indx)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_object *obj = NULL;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jobject ref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (array == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*array != NULL);

	/* Check index */
	if (indx < 0 || indx >= (*array)->length) {
		_jc_post_exception(env, _JC_ArrayIndexOutOfBoundsException);
		goto done;
	}

	/* Get array element */
	obj = (*array)->elems[~indx];

done:
	/* Returning to native code */
	ref = _jc_pop_local_native_frame(env, obj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

static void
SetObjectArrayElement(JNIEnv *jenv, jobjectArray array,
	jsize indx, jobject value)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (array == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*array != NULL);

	/* Check index */
	if (indx < 0 || indx >= (*array)->length) {
		_jc_post_exception(env, _JC_ArrayIndexOutOfBoundsException);
		goto done;
	}

	/* Set element */
	_JC_ASSERT(value == NULL || *value != NULL);
	(*array)->elems[~indx] = (value != NULL) ? *value : NULL;

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

#define GetArrayRegion(_type, Type)					\
static void								\
Get ## Type ## ArrayRegion(JNIEnv *jenv, j ## _type ## Array array,	\
	jsize start, jsize len, j ## _type *buf)			\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Check for null */						\
	if (array == NULL) {						\
		_jc_post_exception(env, _JC_NullPointerException);	\
		goto done;						\
	}								\
	_JC_ASSERT(*array != NULL);					\
									\
	/* Check bounds */						\
	if (!_JC_BOUNDS_CHECK(*array, start, len)) {			\
		_jc_post_exception(env,					\
		    _JC_ArrayIndexOutOfBoundsException);		\
		goto done;						\
	}								\
									\
	/* Copy out elements */						\
	memcpy(buf, (*array)->elems + start,				\
	    len * sizeof(*(*array)->elems));				\
									\
done:									\
	/* Returning to native code */					\
	_jc_pop_local_native_frame(env, NULL);				\
	_jc_stopping_java(env, &cstack, NULL);				\
}
GetArrayRegion(boolean, Boolean)
GetArrayRegion(byte, Byte)
GetArrayRegion(char, Char)
GetArrayRegion(short, Short)
GetArrayRegion(int, Int)
GetArrayRegion(long, Long)
GetArrayRegion(float, Float)
GetArrayRegion(double, Double)

#define SetArrayRegion(_type, Type)					\
static void								\
Set ## Type ## ArrayRegion(JNIEnv *jenv, j ## _type ## Array array,	\
	jsize start, jsize len, const j ## _type *buf)			\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Check for null */						\
	if (array == NULL) {						\
		_jc_post_exception(env, _JC_NullPointerException);	\
		goto done;						\
	}								\
	_JC_ASSERT(*array != NULL);					\
									\
	/* Check bounds */						\
	if (!_JC_BOUNDS_CHECK(*array, start, len)) {			\
		_jc_post_exception(env,					\
		    _JC_ArrayIndexOutOfBoundsException);		\
		goto done;						\
	}								\
									\
	/* Copy in elements */						\
	memcpy((*array)->elems + start,					\
	    buf, len * sizeof(*(*array)->elems));			\
									\
done:									\
	/* Returning to native code */					\
	_jc_pop_local_native_frame(env, NULL);				\
	_jc_stopping_java(env, &cstack, NULL);				\
}
SetArrayRegion(boolean, Boolean)
SetArrayRegion(byte, Byte)
SetArrayRegion(char, Char)
SetArrayRegion(short, Short)
SetArrayRegion(int, Int)
SetArrayRegion(long, Long)
SetArrayRegion(float, Float)
SetArrayRegion(double, Double)

static void *
GetPrimitiveArrayCritical(JNIEnv *jenv, jarray array, jboolean *isCopy)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	void *elems = NULL;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (array == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*array != NULL);

	/* Sanity check that this is a primitive array */
	_JC_ASSERT(*array != NULL);
	_JC_ASSERT(_JC_LW_TEST((*array)->lockword, ARRAY));

	/* Get array elements; no need to copy */
	switch (_JC_LW_EXTRACT((*array)->lockword, TYPE)) {
	case _JC_TYPE_BOOLEAN:
		elems = ((_jc_boolean_array *)(*array))->elems;
		break;
	case _JC_TYPE_BYTE:
		elems = ((_jc_byte_array *)(*array))->elems;
		break;
	case _JC_TYPE_CHAR:
		elems = ((_jc_char_array *)(*array))->elems;
		break;
	case _JC_TYPE_SHORT:
		elems = ((_jc_short_array *)(*array))->elems;
		break;
	case _JC_TYPE_INT:
		elems = ((_jc_int_array *)(*array))->elems;
		break;
	case _JC_TYPE_LONG:
		elems = ((_jc_long_array *)(*array))->elems;
		break;
	case _JC_TYPE_FLOAT:
		elems = ((_jc_float_array *)(*array))->elems;
		break;
	case _JC_TYPE_DOUBLE:
		elems = ((_jc_double_array *)(*array))->elems;
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
		break;
	}
	if (isCopy != NULL)
		*isCopy = JNI_FALSE;

	/* XXX grab VM mutex to block GC?? */

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return elems;
}

static void
ReleasePrimitiveArrayCritical(JNIEnv *jenv, jarray array,
	void *carray, jint mode)
{
	/* XXX release VM mutex to unblock GC?? */
}

static jint
ThrowNew(JNIEnv *jenv, jclass class, const char *message)
{
	jmethodID method;
	jobject string = NULL;
	jobject obj;
	jint status;

	/* Find constructor */
	if ((method = (*jenv)->GetMethodID(jenv, class, "<init>",
	    (message != NULL) ? "(Ljava/lang/String;)V" : "()V")) == NULL)
		return JNI_ERR;

	/* Instantiate string parameter */
	if (message != NULL
	    && (string = (*jenv)->NewStringUTF(jenv, message)) == NULL)
		return JNI_ERR;

	/* Instantiate object */
	obj = (*jenv)->NewObject(jenv, class, method, string);
	(*jenv)->DeleteLocalRef(jenv, string);
	if (obj == NULL)
		return JNI_ERR;

	/* Throw object */
	status = (*jenv)->Throw(jenv, obj);
	(*jenv)->DeleteLocalRef(jenv, obj);

	/* Done */
	return status;
}

static jboolean
ExceptionCheck(JNIEnv *jenv)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;
	jboolean result;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Get result */
	result = env->pending != NULL;

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return result;
}

static jobject
NewGlobalRef(JNIEnv *jenv, jobject obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;
	jobject ref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Get new reference */
	if ((ref = _jc_new_global_native_ref(env, *obj)) == NULL)
		_jc_post_exception_info(env);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

static void
DeleteGlobalRef(JNIEnv *jenv, jobject obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Delete reference */
	_jc_free_global_native_ref(&obj);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);
}

static jobject
NewLocalRef(JNIEnv *jenv, jobject obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;
	jobject ref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Get new reference */
	ref = _jc_new_local_native_ref(env, *obj);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

static void
DeleteLocalRef(JNIEnv *jenv, jobject obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Delete reference */
	_jc_free_local_native_ref(&obj);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);
}

static jweak
NewWeakGlobalRef(JNIEnv *jenv, jobject obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	_jc_fatal_error(env->vm, "%s: unimplemented", __FUNCTION__);
}

static void
DeleteWeakGlobalRef(JNIEnv *jenv, jweak obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	_jc_fatal_error(env->vm, "%s: unimplemented", __FUNCTION__);
}

static jint
EnsureLocalCapacity(JNIEnv *jenv, jint capacity)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;
	jint status;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Extend capacity */
	if (capacity < 0)
		status = JNI_ERR;
	else if (capacity == 0)
		status = JNI_OK;
	else
		status =_jc_extend_local_native_frame(env, capacity);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return status;
}

static jint
PushLocalFrame(JNIEnv *jenv, jint capacity)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;
	jint status;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Push a local native referenc eframe */
	if (capacity < 0)
		status = JNI_ERR;
	if (capacity == 0)
		status = JNI_OK;
	else
		status = _jc_push_local_native_frame(env, capacity);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return status;
}

static jobject
PopLocalFrame(JNIEnv *jenv, jobject ref)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_object *const obj = (ref != NULL) ? *ref : NULL;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Pop frame, but create a new reference for the object */
	ref = _jc_pop_local_native_frame(env, obj);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

static jint
GetVersion(JNIEnv *jenv)
{
	return JNI_VERSION_1_2;
}

static jclass
FindClass(JNIEnv *jenv, const char *name)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_class_loader *loader;
	_jc_object *clobj = NULL;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	_jc_type *type;
	jobject clref;
	jclass class;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Get the appropriate class loader to use */
	if ((loader = _jc_get_jni_loader(env)) == NULL)
		goto done;

	/* Keep a reference on the class loader's instance (if any) */
	if (loader->instance != NULL
	    && (clref = _jc_new_local_native_ref(env,
	      loader->instance)) == NULL)
		goto done;

	/* Load the class using the class loader we just found */
	if ((type = _jc_load_type(env, loader, name)) == NULL)
		goto done;

	/* Initialize the class */
	if (_jc_initialize_type(env, type) != JNI_OK)
		goto done;

	/* Wrap class in native reference */
	clobj = type->instance;

done:
	/* Returning to native code */
	class = _jc_pop_local_native_frame(env, clobj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return class;
}

static jclass
DefineClass(JNIEnv *jenv, const char *name, jobject lref,
	const jbyte *buf, jsize bufLen)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_classbytes *cbytes = NULL;
	_jc_class_loader *loader;
	_jc_object *clobj = NULL;
	_jc_native_frame frame;
	_jc_type *type = NULL;
	_jc_c_stack cstack;
	jclass cref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (lref == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*lref != NULL);
	_JC_ASSERT(name != NULL);
	_JC_ASSERT(buf != NULL);

	/* Get loader */
	if ((loader = _jc_get_loader(env, *lref)) == NULL)
		goto done;

	/* Create '_jc_classbytes' object from bytes */
	if ((cbytes = _jc_copy_classbytes(env, buf, bufLen)) == NULL)
		goto done;

	/* Derive type */
	if ((type = _jc_derive_type_from_classfile(env,
	    loader, name, cbytes)) == NULL)
		goto done;

	/* Get wrapped class instance */
	clobj = type->instance;

done:
	/* Clean up */
	_jc_free_classbytes(&cbytes);

	/* Returning to native code */
	cref = _jc_pop_local_native_frame(env, clobj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return cref;
}

static jclass
GetSuperclass(JNIEnv *jenv, jclass class)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_object *const class_obj = *class;
	_jc_object *clobj = NULL;
	_jc_native_frame frame;
	_jc_type *type = NULL;
	_jc_c_stack cstack;
	jclass cref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Sanity check */
	_JC_ASSERT(class_obj != NULL);
	_JC_ASSERT(class_obj->type == vm->boot.types.Class);

	/* Get class type */
	type = *_JC_VMFIELD(vm, class_obj, Class, vmdata, _jc_type *);

	/* Primitive types have no superclass */
	if ((type->flags & _JC_TYPE_MASK) != _JC_TYPE_REFERENCE)
		goto done;

	/* The JNI spec says interfaces return NULL here */
	if (_JC_ACC_TEST(type, INTERFACE))
		goto done;

	/* Get superclass wrapped in a local native reference */
	clobj = type->superclass->instance;

done:
	/* Returning to native code */
	cref = _jc_pop_local_native_frame(env, clobj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return cref;
}

static jboolean
IsAssignableFrom(JNIEnv *jenv, jclass from_class, jclass to_class)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	jboolean result = JNI_FALSE;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	_jc_type *from_type;
	_jc_type *to_type;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (from_class == NULL || to_class == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}

	/* Sanity check */
	_JC_ASSERT(from_class != NULL && to_class != NULL);
	_JC_ASSERT(*from_class != NULL && *to_class != NULL);
	_JC_ASSERT((*from_class)->type == vm->boot.types.Class
	    && (*to_class)->type == vm->boot.types.Class);

	/* Get class types */
	from_type = *_JC_VMFIELD(vm, *from_class, Class, vmdata, _jc_type *);
	to_type = *_JC_VMFIELD(vm, *to_class, Class, vmdata, _jc_type *);

	/* Check if assignable to */
	result = _jc_assignable_from(env, from_type, to_type);

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return result;
}

static jboolean
IsInstanceOf(JNIEnv *jenv, jobject ref, jclass class)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_object *const obj = (ref != NULL) ? *ref : NULL;
	jboolean result = JNI_FALSE;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	_jc_type *type;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (class == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}

	/* Sanity check */
	_JC_ASSERT(class != NULL && *class != NULL);
	_JC_ASSERT((*class)->type == vm->boot.types.Class);

	/* Get class type */
	type = *_JC_VMFIELD(vm, *class, Class, vmdata, _jc_type *);

	/* Check if instance of */
	result = _jc_instance_of(env, obj, type);

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return result;
}

static void
ExceptionDescribe(JNIEnv *jenv)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Print stack trace */
	_jc_print_stack_trace(env, stderr);

	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

static jthrowable
ExceptionOccurred(JNIEnv *jenv)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;
	jthrowable ref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Get pending exception, if any */
	ref = _jc_new_local_native_ref(env, env->pending);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

static jint
Throw(JNIEnv *jenv, jthrowable obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}

	/* Sanity check */
	_JC_ASSERT(*obj != NULL);
	_JC_ASSERT(_jc_subclass_of(*obj, env->vm->boot.types.Throwable));

	/* Post exception */
	_jc_post_exception_object(env, *obj);

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return JNI_OK;
}

static void
ExceptionClear(JNIEnv *jenv)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Retrieve and clear pending exception, if any */
	env->pending = NULL;

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);
}

static void
FatalError(JNIEnv *jenv, const char *msg)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	_jc_fatal_error(env->vm, "JNI fatal error: %s", msg);
}

static jboolean
IsSameObject(JNIEnv *jenv, jobject ref1, jobject ref2)
{
	return (ref1 == NULL || ref2 == NULL) ?
		ref1 == ref2 : *ref1 == *ref2;
}

static jobject
AllocObject(JNIEnv *jenv, jclass class)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_object *const class_obj = *class;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	_jc_object *obj;
	_jc_type *type;
	jobject ref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Get class type */
	type = *_JC_VMFIELD(vm, class_obj, Class, vmdata, _jc_type *);
	_JC_ASSERT((type->flags & _JC_TYPE_MASK) == _JC_TYPE_REFERENCE);

	/* Instantiate new object */
	obj = _jc_new_object(env, type);

	/* Returning to native code */
	ref = _jc_pop_local_native_frame(env, obj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

static jobject
NewObject(JNIEnv *jenv, jclass class, jmethodID method, ...)
{
	va_list args;
	jobject obj;

	va_start(args, method);
	obj = (*jenv)->NewObjectV(jenv, class, method, args);
	va_end(args);
	return obj;
}

static jobject
NewObjectV(JNIEnv *jenv, jclass class, jmethodID method, va_list args)
{
	jobject obj;

	obj = (*jenv)->AllocObject(jenv, class);
	if ((*jenv)->ExceptionCheck(jenv))
		return NULL;
	(*jenv)->CallNonvirtualVoidMethodV(jenv, obj, class, method, args);
	if ((*jenv)->ExceptionCheck(jenv)) {
		(*jenv)->DeleteLocalRef(jenv, obj);
		return NULL;
	}
	return obj;
}

static jobject
NewObjectA(JNIEnv *jenv, jclass class, jmethodID method, jvalue *args)
{
	jobject obj;

	obj = (*jenv)->AllocObject(jenv, class);
	if ((*jenv)->ExceptionCheck(jenv))
		return NULL;
	(*jenv)->CallNonvirtualVoidMethodA(jenv, obj, class, method, args);
	if ((*jenv)->ExceptionCheck(jenv)) {
		(*jenv)->DeleteLocalRef(jenv, obj);
		return NULL;
	}
	return obj;
}

static jstring
NewString(JNIEnv *jenv, const jchar *unicodeChars, jsize len)
{
	jmethodID constructor;
	jcharArray array;
	jstring string;
	jclass class;

	/* Create char[] array and initialize with supplied chars */
	array = (*jenv)->NewCharArray(jenv, len);
	if ((*jenv)->ExceptionCheck(jenv))
		return NULL;
	(*jenv)->SetCharArrayRegion(jenv, array, 0, len, unicodeChars);
	if ((*jenv)->ExceptionCheck(jenv)) {
		(*jenv)->DeleteLocalRef(jenv, (jobject)array);
		return NULL;
	}

	/* Find and excute String(char[]) constructor */
	class = (*jenv)->FindClass(jenv, "java/lang/String");
	if ((*jenv)->ExceptionCheck(jenv)) {
		(*jenv)->DeleteLocalRef(jenv, (jobject)array);
		return NULL;
	}
	constructor = (*jenv)->GetMethodID(jenv, class, "<init>", "([C)V");
	(*jenv)->DeleteLocalRef(jenv, class);
	if ((*jenv)->ExceptionCheck(jenv)) {
		(*jenv)->DeleteLocalRef(jenv, (jobject)array);
		return NULL;
	}
	string = (*jenv)->NewObject(jenv, class, constructor, array);
	(*jenv)->DeleteLocalRef(jenv, (jobject)array);

	/* Done */
	return string;
}

static jsize
GetStringLength(JNIEnv *jenv, jstring string)
{
	jclass class;
	jmethodID length_method;

	class = (*jenv)->FindClass(jenv, "java/lang/String");
	if ((*jenv)->ExceptionCheck(jenv))
		return 0;
	length_method = (*jenv)->GetMethodID(jenv, class, "length", "()I");
	(*jenv)->DeleteLocalRef(jenv, class);
	if ((*jenv)->ExceptionCheck(jenv))
		return 0;
	return (*jenv)->CallIntMethod(jenv, string, length_method);
}

static const jchar *
GetStringChars(JNIEnv *jenv, jstring string, jboolean *isCopy)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jchar *buf = NULL;
	jint len;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (string == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*string != NULL);

	/* Allocate array */
	len = _jc_decode_string_chars(env, *string, NULL);
	if ((buf = _jc_vm_alloc(env, len * sizeof(*buf))) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}

	/* Get chars */
	len = _jc_decode_string_chars(env, *string, buf);
	if (isCopy != NULL)
		*isCopy = JNI_TRUE;

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return buf;
}

static void
ReleaseStringChars(JNIEnv *jenv, jstring string, const jchar *chars)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Free buffer */
	_jc_vm_free(&chars);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);
}

static void
GetStringRegion(JNIEnv *jenv, jstring string,
	jsize start, jsize len, jchar *buf)
{
	jmethodID toCharArray;
	jcharArray chars;
	jclass class;

	/* Invoke String.toCharArray() */
	class = (*jenv)->FindClass(jenv, "java/lang/String");
	if ((*jenv)->ExceptionCheck(jenv))
		return;
	toCharArray = (*jenv)->GetMethodID(jenv, class, "toCharArray", "()[C");
	(*jenv)->DeleteLocalRef(jenv, class);
	if ((*jenv)->ExceptionCheck(jenv))
		return;
	chars = (jcharArray)(*jenv)->CallObjectMethod(jenv,
	    string, toCharArray);
	if ((*jenv)->ExceptionCheck(jenv))
		return;

	/* Copy out chars */
	(*jenv)->GetCharArrayRegion(jenv, chars, start, len, buf);
	(*jenv)->DeleteLocalRef(jenv, (jobject)chars);
}

static void
GetStringUTFRegion(JNIEnv *jenv, jstring string,
	jsize start, jsize len, char *buf)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	char *full_buf;
	size_t full_len;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (string == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*string != NULL);

	/* Get UTF-8 length */
	full_len = _jc_decode_string_utf8(env, *string, NULL);

	/* Check bounds */
	if (start + len > full_len || start + len < start) {
		_jc_post_exception(env, _JC_ArrayIndexOutOfBoundsException);
		goto done;
	}

	/* Decode UTF-8 into temporary buffer */
	if ((full_buf = _JC_STACK_ALLOC(env, full_len + 1)) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}
	_jc_decode_string_utf8(env, *string, full_buf);

	/* Extract desired portion */
	memcpy(buf, full_buf + start, len);

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
}

static const jchar *
GetStringCritical(JNIEnv *jenv, jstring string, jboolean *isCopy)
{
	return (*jenv)->GetStringChars(jenv, string, isCopy);
}

static void
ReleaseStringCritical(JNIEnv *jenv, jstring string, const jchar *carray)
{
	(*jenv)->ReleaseStringChars(jenv, string, carray);
}

static jsize
GetStringUTFLength(JNIEnv *jenv, jstring string)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jint length = 0;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (string == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*string != NULL);

	/* Get length */
	length = _jc_decode_string_utf8(env, *string, NULL);

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return length;
}

static const char *
GetStringUTFChars(JNIEnv *jenv, jstring string, jboolean *isCopy)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	char *buf = NULL;
	size_t len;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (string == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*string != NULL);

	/* Allocate buffer */
	len = _jc_decode_string_utf8(env, *string, NULL);
	if ((buf = _jc_vm_alloc(env, len + 1)) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}

	/* Copy UTF-8 chars */
	_jc_decode_string_utf8(env, *string, buf);
	if (isCopy != NULL)
		*isCopy = JNI_TRUE;

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return buf;
}

static void
ReleaseStringUTFChars(JNIEnv *jenv, jstring string, const char *utf)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Free buffer */
	_jc_vm_free(&utf);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);
}

static jstring
NewStringUTF(JNIEnv *jenv, const char *bytes)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_object *strobj;
	_jc_c_stack cstack;
	jobject string;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Create string */
	strobj = _jc_new_string(env, bytes, strlen(bytes));

	/* Returning to native code */
	string = _jc_pop_local_native_frame(env, strobj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return (jstring)string;
}

static jobjectArray
NewObjectArray(JNIEnv *jenv, jsize length, jclass class, jobject initValue)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array = NULL;
	_jc_object *const class_obj = *class;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jobject aref = NULL;
	size_t name_len;
	_jc_type *atype;
	_jc_type *type;
	char *aname;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Sanity check */
	_JC_ASSERT(class_obj->type == vm->boot.types.Class);

	/* Get class type */
	type = *_JC_VMFIELD(vm, class_obj, Class, vmdata, _jc_type *);
	_JC_ASSERT((type->flags & _JC_TYPE_MASK) == _JC_TYPE_REFERENCE);

	/* Get the name of the array type for this class */
	name_len = strlen(type->name);
	if ((aname = _JC_STACK_ALLOC(env, name_len + 4)) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}
	aname[0] = '[';
	if (!_JC_FLG_TEST(type, ARRAY)) {
		aname[1] = 'L';
		memcpy(aname + 2, type->name, name_len);
		aname[2 + name_len] = ';';
		aname[3 + name_len] = '\0';
	} else {
		memcpy(aname + 1, type->name, name_len);
		aname[1 + name_len] = '\0';
	}

	/* Load the array type */
	if ((atype = _jc_load_type(env, type->loader, aname)) == NULL)
		goto done;

	/* Create array */
	if ((array = (_jc_object_array *)_jc_new_array(env,
	    atype, length)) == NULL)
		goto done;

	/* Initialize array elements */
	_JC_ASSERT(initValue == NULL || *initValue != NULL);
	if (initValue != NULL) {
		_jc_object *const ivalue = *initValue;
		int i;

		for (i = 0; i < length; i++)
			array->elems[~i] = ivalue;
	}

done:
	/* Returning to native code */
	aref = _jc_pop_local_native_frame(env, (_jc_object *)array);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return (jobjectArray)aref;
}

#define NewArray(_type, Type, TYPE)					\
static j ## _type ## Array						\
New ## Type ## Array(JNIEnv *jenv, jsize length)			\
{									\
	_jc_env *const env = _JC_JNI2ENV(jenv);				\
	_jc_jvm *const vm = env->vm;					\
	_jc_native_frame frame;						\
	_jc_c_stack cstack;						\
	_jc_array *array;						\
	jobject ref;							\
									\
	/* Returning from native code */				\
	_jc_resuming_java(env, &cstack);				\
	_jc_push_stack_local_native_frame(env, &frame);			\
									\
	/* Create array */						\
	array = _jc_new_array(env,					\
	    vm->boot.types.prim_array[_JC_TYPE_ ## TYPE], length);	\
									\
	/* Returning to native code */					\
	ref = _jc_pop_local_native_frame(env, (_jc_object *)array);	\
	_jc_stopping_java(env, &cstack, NULL);				\
									\
	/* Done */							\
	return (j ## _type ## Array)ref;				\
}
NewArray(boolean, Boolean, BOOLEAN)
NewArray(byte, Byte, BYTE)
NewArray(char, Char, CHAR)
NewArray(short, Short, SHORT)
NewArray(int, Int, INT)
NewArray(long, Long, LONG)
NewArray(float, Float, FLOAT)
NewArray(double, Double, DOUBLE)

static jsize
GetArrayLength(JNIEnv *jenv, jarray array)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jsize length = 0;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (array == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}

	/* Get length */
	_JC_ASSERT(*array != NULL);
	length = (*array)->length;

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return length;
}

static jint
RegisterNatives(JNIEnv *jenv, jclass class,
	const JNINativeMethod *methods, jint nMethods)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jint result = JNI_ERR;
	_jc_type *type;
	int i;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Get class info */
	type = *_JC_VMFIELD(vm, *class, Class, vmdata, _jc_type *);
	_JC_ASSERT((type->flags & (_JC_TYPE_ARRAY|_JC_TYPE_REFERENCE))
	    == _JC_TYPE_REFERENCE);

	/* Register supplied methods */
	for (i = 0; i < nMethods; i++) {
		const JNINativeMethod *const m = &methods[i];
		_jc_method *method = NULL;

		/* Search for native method */
		if ((method = _jc_get_declared_method(env,
		      type, m->name, m->signature, 0, 0)) == NULL) {
			_jc_post_exception_info(env);
			goto done;
		}
		if (!_JC_ACC_TEST(method, NATIVE))
			continue;

		/* Is native method already resolved? */
		if (method->native_function != NULL) {
			_jc_post_exception_msg(env, _JC_InternalError,
			    "native method %s.%s%s is already resolved",
			    type->name, m->name, m->signature);
			goto done;
		}

		/* Verbosity */
		VERBOSE(JNI, vm, "registering %s.%s%s at %p",
		    type->name, m->name, m->signature, m->fnPtr);

		/* OK */
		method->native_function = m->fnPtr;
	}

	/* OK */
	result = JNI_OK;

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return result;
}

static jint
UnregisterNatives(JNIEnv *jenv, jclass class)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_c_stack cstack;
	_jc_type *type;
	int i;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Get class info */
	type = *_JC_VMFIELD(vm, *class, Class, vmdata, _jc_type *);
	_JC_ASSERT((type->flags & (_JC_TYPE_ARRAY|_JC_TYPE_REFERENCE))
	    == _JC_TYPE_REFERENCE);

	/* Unregister all native methods */
	for (i = 0; i < type->u.nonarray.num_methods; i++) {
		_jc_method *const method = type->u.nonarray.methods[i];

		method->native_function = NULL;
	}

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return JNI_OK;
}

static jint
MonitorEnter(JNIEnv *jenv, jobject obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jint status = JNI_ERR;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for NULL */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*obj != NULL);

	/* Enter object monitor */
	status = _jc_lock_object(env, *obj);

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return status;
}

static jint
MonitorExit(JNIEnv *jenv, jobject obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jint status = JNI_ERR;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for NULL */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*obj != NULL);

	/* Exit object monitor */
	status = _jc_unlock_object(env, *obj);

done:
	/* Returning to native code */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return status;
}

static jint
GetJavaVM(JNIEnv *jenv, JavaVM **vmp)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);

	*vmp = _JC_JVM2JNI(env->vm);
	return JNI_OK;
}

static jfieldID
FromReflectedField(JNIEnv *jenv, jobject object)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_c_stack cstack;
	_jc_field *field;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Sanity check */
	_JC_ASSERT(object != NULL && *object != NULL);
	_JC_ASSERT((*object)->type == env->vm->boot.types.Field);

	/* Get field */
	field = _jc_get_field(env, *object);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return field;
}

static jmethodID
FromReflectedMethod(JNIEnv *jenv, jobject object)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_c_stack cstack;
	_jc_method *method;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);

	/* Sanity check */
	_JC_ASSERT(object != NULL && *object != NULL);
	_JC_ASSERT((*object)->type == vm->boot.types.Method
	    || (*object)->type == vm->boot.types.Constructor);

	/* Get method */
	method = (*object)->type == vm->boot.types.Method ?
	    _jc_get_method(env, *object) : _jc_get_constructor(env, *object);

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return method;
}

static jobject
ToReflectedField(JNIEnv *jenv, jclass class, jfieldID field)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	_jc_object *obj;
	jobject ref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Get reflected Field */
	obj = _jc_get_reflected_field(env, field);

	/* Returning to native code */
	ref = _jc_pop_local_native_frame(env, obj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

static jobject
ToReflectedMethod(JNIEnv *jenv, jclass class, jmethodID method)
{
	const jboolean is_constructor = strcmp(method->name, "<init>") == 0;
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_native_frame frame;
	_jc_c_stack cstack;
	_jc_object *obj;
	jobject ref;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Get reflected Method or Constructor */
	obj = is_constructor ?
	    _jc_get_reflected_constructor(env, method) :
	    _jc_get_reflected_method(env, method);

	/* Returning to native code */
	ref = _jc_pop_local_native_frame(env, obj);
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	return ref;
}

static jobject
NewDirectByteBuffer(JNIEnv *jenv, void *addr, jlong capacity)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jobject data = NULL;
	jobject buffer = NULL;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Create Pointer object */
	if ((data = _jc_new_local_native_ref(env,
	    _jc_new_object(env, vm->boot.types.Pointer))) == NULL)
		goto done;
	switch (sizeof(void *)) {
	case 4:
		*_JC_VMFIELD(vm, *data, Pointer, data, jint) = (_jc_word)addr;
		break;
	case 8:
		*_JC_VMFIELD(vm, *data, Pointer, data, jlong) = (_jc_word)addr;
		break;
	default:
		_JC_ASSERT(0);
		break;
	}

	/* Create buffer object */
	if ((buffer = _jc_new_local_native_ref(env,
	    _jc_new_object(env, vm->boot.types.ReadWrite))) == NULL)
		goto done;
	if (_jc_invoke_nonvirtual(env,
	    vm->boot.methods.ReadWrite.init, *buffer, NULL,
	    *data, (jint)capacity, (jint)capacity, (jint)0) != JNI_OK) {
		buffer = NULL;
		goto done;
	}

done:
	/* Done */
	_jc_free_local_native_ref(&data);
	buffer = _jc_pop_local_native_frame(env,
	    _jc_free_local_native_ref(&buffer));
	_jc_stopping_java(env, &cstack, NULL);
	return buffer;
}

static void *
GetDirectBufferAddress(JNIEnv *jenv, jobject obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	_jc_object *data;
	void *addr = NULL;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*obj != NULL);

	/* Get Pointer object */
	data = *_JC_VMFIELD(vm, *obj, Buffer, address, _jc_object *);
	if (data == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}

	/* Extract address from Pointer object */
	switch (sizeof(void *)) {
	case 4:
		addr = (void *)(_jc_word)*_JC_VMFIELD(vm,
		    data, Pointer, data, jint);
		break;
	case 8:
		addr = (void *)(_jc_word)*_JC_VMFIELD(vm,
		    data, Pointer, data, jlong);
		break;
	default:
		_JC_ASSERT(0);
		break;
	}

done:
	/* Done */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
	return addr;
}

static jlong
GetDirectBufferCapacity(JNIEnv *jenv, jobject obj)
{
	_jc_env *const env = _JC_JNI2ENV(jenv);
	_jc_jvm *const vm = env->vm;
	_jc_native_frame frame;
	_jc_c_stack cstack;
	jlong capacity = 0;

	/* Returning from native code */
	_jc_resuming_java(env, &cstack);
	_jc_push_stack_local_native_frame(env, &frame);

	/* Check for null */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(*obj != NULL);

	/* Get capacity */
	capacity = *_JC_VMFIELD(vm, *obj, Buffer, cap, jint);

done:
	/* Done */
	_jc_pop_local_native_frame(env, NULL);
	_jc_stopping_java(env, &cstack, NULL);
	return capacity;
}

const struct JNINativeInterface _jc_native_interface = {
	NULL,
	NULL,
	NULL,
	NULL,
	GetVersion,
	DefineClass,
	FindClass,
	FromReflectedMethod,
	FromReflectedField,
	ToReflectedMethod,
	GetSuperclass,
	IsAssignableFrom,
	ToReflectedField,
	Throw,
	ThrowNew,
	ExceptionOccurred,
	ExceptionDescribe,
	ExceptionClear,
	FatalError,
	PushLocalFrame,
	PopLocalFrame,
	NewGlobalRef,
	DeleteGlobalRef,
	DeleteLocalRef,
	IsSameObject,
	NewLocalRef,
	EnsureLocalCapacity,
	AllocObject,
	NewObject,
	NewObjectV,
	NewObjectA,
	GetObjectClass,
	IsInstanceOf,
	GetMethodID,
	CallObjectMethod,
	CallObjectMethodV,
	CallObjectMethodA,
	CallBooleanMethod,
	CallBooleanMethodV,
	CallBooleanMethodA,
	CallByteMethod,
	CallByteMethodV,
	CallByteMethodA,
	CallCharMethod,
	CallCharMethodV,
	CallCharMethodA,
	CallShortMethod,
	CallShortMethodV,
	CallShortMethodA,
	CallIntMethod,
	CallIntMethodV,
	CallIntMethodA,
	CallLongMethod,
	CallLongMethodV,
	CallLongMethodA,
	CallFloatMethod,
	CallFloatMethodV,
	CallFloatMethodA,
	CallDoubleMethod,
	CallDoubleMethodV,
	CallDoubleMethodA,
	CallVoidMethod,
	CallVoidMethodV,
	CallVoidMethodA,
	CallNonvirtualObjectMethod,
	CallNonvirtualObjectMethodV,
	CallNonvirtualObjectMethodA,
	CallNonvirtualBooleanMethod,
	CallNonvirtualBooleanMethodV,
	CallNonvirtualBooleanMethodA,
	CallNonvirtualByteMethod,
	CallNonvirtualByteMethodV,
	CallNonvirtualByteMethodA,
	CallNonvirtualCharMethod,
	CallNonvirtualCharMethodV,
	CallNonvirtualCharMethodA,
	CallNonvirtualShortMethod,
	CallNonvirtualShortMethodV,
	CallNonvirtualShortMethodA,
	CallNonvirtualIntMethod,
	CallNonvirtualIntMethodV,
	CallNonvirtualIntMethodA,
	CallNonvirtualLongMethod,
	CallNonvirtualLongMethodV,
	CallNonvirtualLongMethodA,
	CallNonvirtualFloatMethod,
	CallNonvirtualFloatMethodV,
	CallNonvirtualFloatMethodA,
	CallNonvirtualDoubleMethod,
	CallNonvirtualDoubleMethodV,
	CallNonvirtualDoubleMethodA,
	CallNonvirtualVoidMethod,
	CallNonvirtualVoidMethodV,
	CallNonvirtualVoidMethodA,
	GetFieldID,
	GetObjectField,
	GetBooleanField,
	GetByteField,
	GetCharField,
	GetShortField,
	GetIntField,
	GetLongField,
	GetFloatField,
	GetDoubleField,
	SetObjectField,
	SetBooleanField,
	SetByteField,
	SetCharField,
	SetShortField,
	SetIntField,
	SetLongField,
	SetFloatField,
	SetDoubleField,
	GetStaticMethodID,
	CallStaticObjectMethod,
	CallStaticObjectMethodV,
	CallStaticObjectMethodA,
	CallStaticBooleanMethod,
	CallStaticBooleanMethodV,
	CallStaticBooleanMethodA,
	CallStaticByteMethod,
	CallStaticByteMethodV,
	CallStaticByteMethodA,
	CallStaticCharMethod,
	CallStaticCharMethodV,
	CallStaticCharMethodA,
	CallStaticShortMethod,
	CallStaticShortMethodV,
	CallStaticShortMethodA,
	CallStaticIntMethod,
	CallStaticIntMethodV,
	CallStaticIntMethodA,
	CallStaticLongMethod,
	CallStaticLongMethodV,
	CallStaticLongMethodA,
	CallStaticFloatMethod,
	CallStaticFloatMethodV,
	CallStaticFloatMethodA,
	CallStaticDoubleMethod,
	CallStaticDoubleMethodV,
	CallStaticDoubleMethodA,
	CallStaticVoidMethod,
	CallStaticVoidMethodV,
	CallStaticVoidMethodA,
	GetStaticFieldID,
	GetStaticObjectField,
	GetStaticBooleanField,
	GetStaticByteField,
	GetStaticCharField,
	GetStaticShortField,
	GetStaticIntField,
	GetStaticLongField,
	GetStaticFloatField,
	GetStaticDoubleField,
	SetStaticObjectField,
	SetStaticBooleanField,
	SetStaticByteField,
	SetStaticCharField,
	SetStaticShortField,
	SetStaticIntField,
	SetStaticLongField,
	SetStaticFloatField,
	SetStaticDoubleField,
	NewString,
	GetStringLength,
	GetStringChars,
	ReleaseStringChars,
	NewStringUTF,
	GetStringUTFLength,
	GetStringUTFChars,
	ReleaseStringUTFChars,
	GetArrayLength,
	NewObjectArray,
	GetObjectArrayElement,
	SetObjectArrayElement,
	NewBooleanArray,
	NewByteArray,
	NewCharArray,
	NewShortArray,
	NewIntArray,
	NewLongArray,
	NewFloatArray,
	NewDoubleArray,
	GetBooleanArrayElements,
	GetByteArrayElements,
	GetCharArrayElements,
	GetShortArrayElements,
	GetIntArrayElements,
	GetLongArrayElements,
	GetFloatArrayElements,
	GetDoubleArrayElements,
	ReleaseBooleanArrayElements,
	ReleaseByteArrayElements,
	ReleaseCharArrayElements,
	ReleaseShortArrayElements,
	ReleaseIntArrayElements,
	ReleaseLongArrayElements,
	ReleaseFloatArrayElements,
	ReleaseDoubleArrayElements,
	GetBooleanArrayRegion,
	GetByteArrayRegion,
	GetCharArrayRegion,
	GetShortArrayRegion,
	GetIntArrayRegion,
	GetLongArrayRegion,
	GetFloatArrayRegion,
	GetDoubleArrayRegion,
	SetBooleanArrayRegion,
	SetByteArrayRegion,
	SetCharArrayRegion,
	SetShortArrayRegion,
	SetIntArrayRegion,
	SetLongArrayRegion,
	SetFloatArrayRegion,
	SetDoubleArrayRegion,
	RegisterNatives,
	UnregisterNatives,
	MonitorEnter,
	MonitorExit,
	GetJavaVM,
	GetStringRegion,
	GetStringUTFRegion,
	GetPrimitiveArrayCritical,
	ReleasePrimitiveArrayCritical,
	GetStringCritical,
	ReleaseStringCritical,
	NewWeakGlobalRef,
	DeleteWeakGlobalRef,
	ExceptionCheck,
	NewDirectByteBuffer,
	GetDirectBufferAddress,
	GetDirectBufferCapacity
};

