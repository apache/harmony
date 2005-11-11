
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
 * $Id: java_lang_VMClass.c,v 1.11 2005/11/09 18:14:22 archiecobbs Exp $
 */

#include "libjc.h"
#include "java_lang_VMClass.h"

/* Internal functions */
static _jc_type		*_jc_get_type(_jc_env *env, _jc_object *this,
				jboolean resolve);

/*
 * static final native Class forName(String, boolean, ClassLoader)
 *	throws java/lang/ClassNotFoundException
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_forName(_jc_env *env, _jc_object *name_string,
	jboolean initialize, _jc_object *loader_obj)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *clobj;

	/* Load class */
	clobj = _jc_internal_load_class(env,
	    name_string, loader_obj, JNI_FALSE);

	/* Initialize if desired */
	if (initialize) {
		_jc_type *type;

		/* Get type */
		type = *_JC_VMFIELD(vm, clobj, Class, vmdata, _jc_type *);
		_JC_ASSERT(type != NULL);

		/* Initialize type */
		if (_jc_initialize_type(env, type) != JNI_OK)
			_jc_throw_exception(env);
	}

	/* Done */
	return clobj;
}

/*
 * static final native ClassLoader getClassLoader(Class)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getClassLoader(_jc_env *env, _jc_object *this)
{
	_jc_type *type;

	/* Get type */
	type = _jc_get_type(env, this, JNI_FALSE);

	/* Return loader */
	return type->loader->instance;
}

/*
 * static final native Class getComponentType(Class)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getComponentType(_jc_env *env, _jc_object *this)
{
	_jc_type *type;

	/* Get type */
	type = _jc_get_type(env, this, JNI_FALSE);

	/* Return NULL for non-array types */
	if (!_JC_FLG_TEST(type, ARRAY))
		return NULL;

	/* Return component type */
	return type->u.array.element_type->instance;
}

/*
 * static final native Class[] getDeclaredClasses(Class, boolean)
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getDeclaredClasses(_jc_env *env, _jc_object *this,
	jboolean public_only)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array;
	_jc_type *type;
	int num;
	int i;

	/* Get type */
	type = _jc_get_type(env, this, JNI_TRUE);

	/* Handle array types */
	if (_JC_FLG_TEST(type, ARRAY)) {
		if ((array = (_jc_object_array *)_jc_new_array(env,
		    vm->boot.types.Class_array, 0)) == NULL)
			_jc_throw_exception(env);
		return array;
	}

	/* Count number of classes */
	for (num = i = 0; i < type->u.nonarray.num_inner_classes; i++) {
		_jc_inner_class *const ic = &type->u.nonarray.inner_classes[i];

		if (!public_only || _JC_ACC_TEST(ic, PUBLIC))
			num++;
	}

	/* Create array */
	if ((array = (_jc_object_array *)_jc_new_array(env,
	    vm->boot.types.Class_array, num)) == NULL)
		_jc_throw_exception(env);

	/* Fill in array */
	for (num = i = 0; i < type->u.nonarray.num_inner_classes; i++) {
		_jc_inner_class *const ic = &type->u.nonarray.inner_classes[i];

		if (!public_only || _JC_ACC_TEST(ic, PUBLIC))
			array->elems[~(num++)] = ic->type->instance;
	}
	_JC_ASSERT(num == array->length);

	/* Done */
	return array;
}

/*
 * static final native Constructor[] getDeclaredConstructors(Class, boolean)
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getDeclaredConstructors(_jc_env *env,
	_jc_object *this, jboolean public_only)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array;
	_jc_type *type;
	jobject ref;
	jint num;
	int i;

	/* Get type */
	type = _jc_get_type(env, this, JNI_TRUE);

	/* Handle array types */
	if (_JC_FLG_TEST(type, ARRAY)) {
		if ((array = (_jc_object_array *)_jc_new_array(env,
		    vm->boot.types.Constructor_array, 0)) == NULL)
			_jc_throw_exception(env);
		return array;
	}

	/* Count constructors */
	for (num = i = 0; i < type->u.nonarray.num_methods; i++) {
		_jc_method *const method = type->u.nonarray.methods[i];

		if (public_only && !_JC_ACC_TEST(method, PUBLIC))
			continue;
		if (_JC_ACC_TEST(method, STATIC) || *method->name != '<')
			continue;
		num++;
	}

	/* Create new array and wrap it temporarily */
	if ((ref = _jc_new_local_native_ref(env,
	    (_jc_object *)_jc_new_array(env,
	      vm->boot.types.Constructor_array, num))) == NULL)
		_jc_throw_exception(env);
	array = (_jc_object_array *)*ref;

	/* Add constructors */
	for (num = i = 0; i < type->u.nonarray.num_methods; i++) {
		_jc_method *const method = type->u.nonarray.methods[i];

		if (public_only && !_JC_ACC_TEST(method, PUBLIC))
			continue;
		if (_JC_ACC_TEST(method, STATIC) || *method->name != '<')
			continue;
		if ((array->elems[~(num++)]
		    = _jc_get_reflected_constructor(env, method)) == NULL) {
			_jc_free_local_native_ref(&ref);
			_jc_throw_exception(env);
		}
	}

	/* Return result */
	return (_jc_object_array *)_jc_free_local_native_ref(&ref);
}

/*
 * static final native Field[] getDeclaredFields(Class, boolean)
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getDeclaredFields(_jc_env *env, _jc_object *this,
	jboolean public_only)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array;
	_jc_type *type;
	jobject ref;
	jint num;
	int i;

	/* Get type */
	type = _jc_get_type(env, this, JNI_TRUE);

	/* Handle array types */
	if (_JC_FLG_TEST(type, ARRAY)) {
		if ((array = (_jc_object_array *)_jc_new_array(env,
		    vm->boot.types.Field_array, 0)) == NULL)
			_jc_throw_exception(env);
		return array;
	}

	/* Count fields */
	if (!public_only)
		num = type->u.nonarray.num_fields;
	else {
		for (num = i = 0; i < type->u.nonarray.num_fields; i++) {
			_jc_field *const field = type->u.nonarray.fields[i];

			if (_JC_ACC_TEST(field, PUBLIC))
				num++;
		}
	}

	/* Create new array and wrap it temporarily */
	if ((ref = _jc_new_local_native_ref(env,
	    (_jc_object *)_jc_new_array(env,
	      vm->boot.types.Field_array, num))) == NULL)
		_jc_throw_exception(env);
	array = (_jc_object_array *)*ref;

	/* Add fields */
	for (num = i = 0; i < type->u.nonarray.num_fields; i++) {
		_jc_field *const field = type->u.nonarray.fields[i];

		if (public_only && !_JC_ACC_TEST(field, PUBLIC))
			continue;
		if ((array->elems[~(num++)]
		    = _jc_get_reflected_field(env, field)) == NULL) {
			_jc_free_local_native_ref(&ref);
			_jc_throw_exception(env);
		}
	}

	/* Return result */
	return (_jc_object_array *)_jc_free_local_native_ref(&ref);
}

/*
 * static final native Method[] getDeclaredMethods(Class, boolean)
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getDeclaredMethods(_jc_env *env, _jc_object *this,
	jboolean public_only)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array;
	_jc_type *type;
	jobject ref;
	jint num;
	int i;

	/* Get type */
	type = _jc_get_type(env, this, JNI_TRUE);

	/* Handle array types */
	if (_JC_FLG_TEST(type, ARRAY)) {
		if ((array = (_jc_object_array *)_jc_new_array(env,
		    vm->boot.types.Method_array, 0)) == NULL)
			_jc_throw_exception(env);
		return array;
	}

	/* Count methods */
	for (num = i = 0; i < type->u.nonarray.num_methods; i++) {
		_jc_method *const method = type->u.nonarray.methods[i];

		if (public_only && !_JC_ACC_TEST(method, PUBLIC))
			continue;
		if (*method->name == '<')
			continue;
		num++;
	}

	/* Create new array and wrap it temporarily */
	if ((ref = _jc_new_local_native_ref(env,
	    (_jc_object *)_jc_new_array(env,
	      vm->boot.types.Method_array, num))) == NULL)
		_jc_throw_exception(env);
	array = (_jc_object_array *)*ref;

	/* Add methods */
	for (num = i = 0; i < type->u.nonarray.num_methods; i++) {
		_jc_method *const method = type->u.nonarray.methods[i];

		if (public_only && !_JC_ACC_TEST(method, PUBLIC))
			continue;
		if (*method->name == '<')
			continue;
		if ((array->elems[~(num++)]
		    = _jc_get_reflected_method(env, method)) == NULL) {
			_jc_free_local_native_ref(&ref);
			_jc_throw_exception(env);
		}
	}

	/* Return result */
	return (_jc_object_array *)_jc_free_local_native_ref(&ref);
}

/*
 * static final native Class getDeclaringClass(Class)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getDeclaringClass(_jc_env *env, _jc_object *this)
{
	_jc_type *type;

	/* Get type */
	type = _jc_get_type(env, this, JNI_TRUE);

	/* Return declaring class, if any */
	if (!_JC_FLG_TEST(type, ARRAY)
	    && type->u.nonarray.outer_class != NULL)
	    	return type->u.nonarray.outer_class->instance;
	return NULL;
}

/*
 * static final native Class[] getInterfaces(Class)
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getInterfaces(_jc_env *env, _jc_object *this)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array;
	_jc_type *type;
	jobject ref;
	int i;

	/* Get type */
	type = _jc_get_type(env, this, JNI_TRUE);

	/* Create new array and wrap it temporarily */
	if ((ref = _jc_new_local_native_ref(env,
	    (_jc_object *)_jc_new_array(env,
	      vm->boot.types.Class_array, type->num_interfaces))) == NULL)
		_jc_throw_exception(env);
	array = (_jc_object_array *)*ref;

	/* Add interfaces */
	for (i = 0; i < type->num_interfaces; i++) {
		_jc_type *const itype = type->interfaces[i];

		array->elems[~i] = itype->instance;
	}

	/* Return result */
	return (_jc_object_array *)_jc_free_local_native_ref(&ref);
}

/*
 * static final native int getModifiers(Class, boolean)
 */
jint _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getModifiers(_jc_env *env,
	_jc_object *this, jboolean notInner)
{
	_jc_type *type;
	int flags;

	/* Get type */
	type = _jc_get_type(env, this, JNI_FALSE);
	flags = type->access_flags;

	/* Handle special case for inner classes */
	if (!notInner
	    && !_JC_FLG_TEST(type, ARRAY)
	    && type->u.nonarray.outer_class != NULL) {
	    	_jc_nonarray_type *const otype
		    = &type->u.nonarray.outer_class->u.nonarray;
		int i;

		for (i = 0; i < otype->num_inner_classes; i++) {
			_jc_inner_class *iclass = &otype->inner_classes[i];

			if (iclass->type == type) {
				flags = iclass->access_flags;
				break;
			}
		}
	}

	/* Return modifiers */
	return flags & _JC_ACC_MASK;
}

/*
 * static final native String getName(Class)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getName(_jc_env *env, _jc_object *this)
{
	_jc_object *string;
	size_t name_len;
	_jc_type *type;
	char *name;
	char *s;

	/* Get type */
	type = _jc_get_type(env, this, JNI_FALSE);

	/* Convert from internal name format */
	name_len = strlen(type->name);
	if ((name = _JC_STACK_ALLOC(env, name_len + 1)) == NULL) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}
	memcpy(name, type->name, name_len + 1);
	for (s = name; *s != '\0'; s++) {
		if (*s == '/')
			*s = '.';
	}

	/* Get string */
	if ((string = _jc_new_string(env, name, name_len)) == NULL)
		_jc_throw_exception(env);

	/* Return result */
	return string;
}

/*
 * static final native Class getSuperclass(Class)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClass_getSuperclass(_jc_env *env, _jc_object *this)
{
	_jc_type *type;

	/* Get type */
	type = _jc_get_type(env, this, JNI_FALSE);

	/* Handle special cases */
	if (type->superclass == NULL || _JC_ACC_TEST(type, INTERFACE))
		return NULL;

	/* Return result */
	return type->superclass->instance;
}

/*
 * static final native boolean isArray(Class)
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_VMClass_isArray(_jc_env *env, _jc_object *this)
{
	_jc_type *type;

	/* Get type */
	type = _jc_get_type(env, this, JNI_FALSE);

	/* Return arrayness */
	return _JC_FLG_TEST(type, ARRAY);
}

/*
 * static final native boolean isAssignableFrom(Class, Class)
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_VMClass_isAssignableFrom(_jc_env *env, _jc_object *this,
	_jc_object *other)
{
	_jc_type *from_type;
	_jc_type *to_type;

	/* Get types */
	from_type = _jc_get_type(env, other, JNI_FALSE);
	to_type = _jc_get_type(env, this, JNI_FALSE);

	/* Return assignable */
	return _jc_assignable_from(env, from_type, to_type);
}

/*
 * static final native boolean isInstance(Class, Object)
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_VMClass_isInstance(_jc_env *env,
	_jc_object *this, _jc_object *obj)
{
	_jc_type *type;

	/* Get type */
	type = _jc_get_type(env, this, JNI_FALSE);

	/* Return instanceof */
	return _jc_instance_of(env, obj, type);
}

/*
 * static final native boolean isInterface(Class)
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_VMClass_isInterface(_jc_env *env, _jc_object *this)
{
	_jc_type *type;

	/* Get type */
	type = _jc_get_type(env, this, JNI_FALSE);

	/* Return interfaceness */
	return _JC_ACC_TEST(type, INTERFACE);
}

/*
 * static final native boolean isPrimitive(Class)
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_VMClass_isPrimitive(_jc_env *env, _jc_object *this)
{
	_jc_type *type;

	/* Get type */
	type = _jc_get_type(env, this, JNI_FALSE);

	/* Return primitiveness */
	return (type->flags & _JC_TYPE_MASK) != _JC_TYPE_REFERENCE;
}

/*
 * static final native void throwException(Throwable)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMClass_throwException(_jc_env *env, _jc_object *throwable)
{
	/* Check for null */
	if (throwable == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Post and throw exception */
	_jc_post_exception_object(env, throwable);
	_jc_throw_exception(env);
}

/*
 * Get (and resolve) the type corresponding to the Class object.
 */
static _jc_type *
_jc_get_type(_jc_env *env, _jc_object *this, jboolean resolve)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *type;

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Get type */
	type = *_JC_VMFIELD(vm, this, Class, vmdata, _jc_type *);

	/* Resolve type */
	if (resolve && _jc_resolve_type(env, type) != JNI_OK)
		_jc_throw_exception(env);

	/* Done */
	return type;
}

