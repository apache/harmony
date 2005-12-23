
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
#include "java_lang_reflect_Field.h"

/* Internal functions */
static _jc_field	*_jc_check_field(_jc_env *env, _jc_object *this);
static jint		_jc_field_get(_jc_env *env, _jc_object *this,
				_jc_object *obj, int dtype, _jc_value *value);
static jint		_jc_field_set(_jc_env *env, _jc_object *this,
				_jc_object *obj, int stype, _jc_value *value);
static void		*_jc_field_validate(_jc_env *env, _jc_object *this,
				_jc_field **fieldp, _jc_object *obj);

/*
 * public final native Object get(Object)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_get(_jc_env *env, _jc_object *this,
	_jc_object *obj)
{
	_jc_value value;

	/* Get field */
	if (_jc_field_get(env, this, obj, _JC_TYPE_REFERENCE, &value) != JNI_OK)
		_jc_throw_exception(env);

	/* Return value */
	return value.l;
}

/*
 * public final native boolean getBoolean(Object)
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getBoolean(_jc_env *env, _jc_object *this,
	_jc_object *obj)
{
	_jc_value value;

	/* Get field */
	if (_jc_field_get(env, this, obj, _JC_TYPE_BOOLEAN, &value) != JNI_OK)
		_jc_throw_exception(env);

	/* Return value */
	return value.z;
}

/*
 * public final native byte getByte(Object)
 */
jbyte _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getByte(_jc_env *env, _jc_object *this,
	_jc_object *obj)
{
	_jc_value value;

	/* Get field */
	if (_jc_field_get(env, this, obj, _JC_TYPE_BYTE, &value) != JNI_OK)
		_jc_throw_exception(env);

	/* Return value */
	return value.b;
}

/*
 * public final native char getChar(Object)
 */
jchar _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getChar(_jc_env *env, _jc_object *this,
	_jc_object *obj)
{
	_jc_value value;

	/* Get field */
	if (_jc_field_get(env, this, obj, _JC_TYPE_CHAR, &value) != JNI_OK)
		_jc_throw_exception(env);

	/* Return value */
	return value.c;
}

/*
 * public final native double getDouble(Object)
 */
jdouble _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getDouble(_jc_env *env, _jc_object *this,
	_jc_object *obj)
{
	_jc_value value;

	/* Get field */
	if (_jc_field_get(env, this, obj, _JC_TYPE_DOUBLE, &value) != JNI_OK)
		_jc_throw_exception(env);

	/* Return value */
	return value.d;
}

/*
 * public final native float getFloat(Object)
 */
jfloat _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getFloat(_jc_env *env, _jc_object *this,
	_jc_object *obj)
{
	_jc_value value;

	/* Get field */
	if (_jc_field_get(env, this, obj, _JC_TYPE_FLOAT, &value) != JNI_OK)
		_jc_throw_exception(env);

	/* Return value */
	return value.f;
}

/*
 * public final native int getInt(Object)
 */
jint _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getInt(_jc_env *env, _jc_object *this,
	_jc_object *obj)
{
	_jc_value value;

	/* Get field */
	if (_jc_field_get(env, this, obj, _JC_TYPE_INT, &value) != JNI_OK)
		_jc_throw_exception(env);

	/* Return value */
	return value.i;
}

/*
 * public final native long getLong(Object)
 */
jlong _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getLong(_jc_env *env, _jc_object *this,
	_jc_object *obj)
{
	_jc_value value;

	/* Get field */
	if (_jc_field_get(env, this, obj, _JC_TYPE_LONG, &value) != JNI_OK)
		_jc_throw_exception(env);

	/* Return value */
	return value.j;
}

/*
 * public final native int getModifiers()
 */
jint _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getModifiers(_jc_env *env, _jc_object *this)
{
	_jc_field *field;

	/* Get field */
	field = _jc_check_field(env, this);

	/* Return flags */
	return field->access_flags & _JC_ACC_MASK;
}

/*
 * public final native short getShort(Object)
 */
jshort _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getShort(_jc_env *env, _jc_object *this,
	_jc_object *obj)
{
	_jc_value value;

	/* Get field */
	if (_jc_field_get(env, this, obj, _JC_TYPE_SHORT, &value) != JNI_OK)
		_jc_throw_exception(env);

	/* Return value */
	return value.s;
}

/*
 * public final native Class getType()
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_getType(_jc_env *env, _jc_object *this)
{
	_jc_field *field;

	/* Get field */
	field = _jc_check_field(env, this);

	/* Return field's type's Class instance */
	return field->type->instance;
}

/*
 * public final native void set(Object, Object)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_set(_jc_env *env, _jc_object *this,
	_jc_object *obj, _jc_object *valobj)
{
	_jc_field *field;
	_jc_value value;
	int ftype;

	/* Get field */
	field = _jc_check_field(env, this);
	ftype = (field->type->flags & _JC_TYPE_MASK);

	/* Unwrap primitive values */
	if (ftype == _JC_TYPE_REFERENCE)
		value.l = valobj;
	else if ((ftype = _jc_unwrap_primitive(env,
	    valobj, &value)) == _JC_TYPE_INVALID)
		_jc_throw_exception(env);

	/* Set field */
	if (_jc_field_set(env, this, obj, ftype, &value) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * public final native void setBoolean(Object, boolean)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_setBoolean(_jc_env *env, _jc_object *this,
	_jc_object *obj, jboolean to_set)
{
	_jc_value value;

	/* Get value */
	value.z = to_set;

	/* Set field */
	if (_jc_field_set(env, this, obj, _JC_TYPE_BOOLEAN, &value) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * public final native void setByte(Object, byte)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_setByte(_jc_env *env, _jc_object *this,
	_jc_object *obj, jbyte to_set)
{
	_jc_value value;

	/* Get value */
	value.b = to_set;

	/* Set field */
	if (_jc_field_set(env, this, obj, _JC_TYPE_BYTE, &value) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * public final native void setChar(Object, char)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_setChar(_jc_env *env, _jc_object *this,
	_jc_object *obj, jchar to_set)
{
	_jc_value value;

	/* Get value */
	value.c = to_set;

	/* Set field */
	if (_jc_field_set(env, this, obj, _JC_TYPE_CHAR, &value) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * public final native void setDouble(Object, double)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_setDouble(_jc_env *env, _jc_object *this,
	_jc_object *obj, jdouble to_set)
{
	_jc_value value;

	/* Get value */
	value.d = to_set;

	/* Set field */
	if (_jc_field_set(env, this, obj, _JC_TYPE_DOUBLE, &value) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * public final native void setFloat(Object, float)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_setFloat(_jc_env *env, _jc_object *this,
	_jc_object *obj, jfloat to_set)
{
	_jc_value value;

	/* Get value */
	value.f = to_set;

	/* Set field */
	if (_jc_field_set(env, this, obj, _JC_TYPE_FLOAT, &value) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * public final native void setInt(Object, int)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_setInt(_jc_env *env, _jc_object *this,
	_jc_object *obj, jint to_set)
{
	_jc_value value;

	/* Get value */
	value.i = to_set;

	/* Set field */
	if (_jc_field_set(env, this, obj, _JC_TYPE_INT, &value) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * public final native void setLong(Object, long)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_setLong(_jc_env *env, _jc_object *this,
	_jc_object *obj, jlong to_set)
{
	_jc_value value;

	/* Get value */
	value.j = to_set;

	/* Set field */
	if (_jc_field_set(env, this, obj, _JC_TYPE_LONG, &value) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * public final native void setShort(Object, short)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_reflect_Field_setShort(_jc_env *env, _jc_object *this,
	_jc_object *obj, jshort to_set)
{
	_jc_value value;

	/* Get value */
	value.s = to_set;

	/* Set field */
	if (_jc_field_set(env, this, obj, _JC_TYPE_SHORT, &value) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * Find the _jc_field structure corresponding to a Field object.
 */
static _jc_field *
_jc_check_field(_jc_env *env, _jc_object *this)
{
	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Return field */
	return _jc_get_field(env, this);
}

/*
 * Get a field, doing so with the Java atomicity guarantees.
 * Convert the field to destination type 'dtype', or post an
 * IllegalArgumentException if it can't be so converted.
 */
static jint
_jc_field_get(_jc_env *env, _jc_object *this, _jc_object *obj,
	int dtype, _jc_value *value)
{
	_jc_jvm *const vm = env->vm;
	_jc_field *field;
	void *data;
	int stype;

	/* Validate access */
	if ((data = _jc_field_validate(env, this, &field, obj)) == NULL)
		return JNI_ERR;

	/* Get the field's content */
	stype = (field->type->flags & _JC_TYPE_MASK);
	switch (stype) {
	case _JC_TYPE_BOOLEAN:
		value->z = *(jboolean *)data;
		break;
	case _JC_TYPE_BYTE:
		value->b = *(jbyte *)data;
		break;
	case _JC_TYPE_CHAR:
		value->c = *(jchar *)data;
		break;
	case _JC_TYPE_SHORT:
		value->s = *(jshort *)data;
		break;
	case _JC_TYPE_INT:
		value->i = *(jint *)data;
		break;
	case _JC_TYPE_LONG:
		value->j = *(jlong *)data;
		break;
	case _JC_TYPE_FLOAT:
		value->f = *(jfloat *)data;
		break;
	case _JC_TYPE_DOUBLE:
		value->d = *(jdouble *)data;
		break;
	case _JC_TYPE_REFERENCE:
		value->l = *(_jc_object **)data;
		break;
	default:
		_jc_fatal_error(vm, "impossible");
	}

	/* Do any implicit primitive type conversion if necessary */
	if (dtype != _JC_TYPE_REFERENCE && stype != _JC_TYPE_REFERENCE) {
		if (_jc_convert_primitive(env, dtype, stype, value) != JNI_OK)
			return JNI_ERR;
	}

	/* Handle the case where a primitive result is desired */
	if (dtype != _JC_TYPE_REFERENCE) {
		if (stype == _JC_TYPE_REFERENCE) {
			_jc_post_exception_msg(env,
			    _JC_IllegalArgumentException,
			    "can't convert value of type `%s' to %s",
			    field->type->name, _jc_prim_names[dtype]);
			return JNI_ERR;
		}
		return JNI_OK;
	}

	/* Wrap primitive values in instance of wrapper class */
	if (stype != _JC_TYPE_REFERENCE
	    && (value->l = _jc_wrap_primitive(env, stype, value)) == NULL)
		_jc_throw_exception(env);

	/* Done */
	return JNI_OK;
}

/*
 * Set a field, doing so with the Java atomicity guarantees.
 * Convert the field from the primitive type 'stype', or throw
 * an IllegalArgumentException if it can't be so converted.
 */
static jint
_jc_field_set(_jc_env *env, _jc_object *this, _jc_object *obj,
	int stype, _jc_value *value)
{
	_jc_jvm *const vm = env->vm;
	_jc_field *field;
	void *data;
	int status;
	int dtype;

	/* Validate access */
	if ((data = _jc_field_validate(env, this, &field, obj)) == NULL)
		return JNI_ERR;
	dtype = (field->type->flags & _JC_TYPE_MASK);

	/* Check that we're not trying to set a final field */
	if (_JC_ACC_TEST(field, FINAL)) {
		_jc_post_exception_msg(env, _JC_IllegalAccessException,
		    "field `%s' is final", field->name);
		return JNI_ERR;
	}

	/* Check for basic compatibility */
	if ((stype == _JC_TYPE_REFERENCE) != (dtype == _JC_TYPE_REFERENCE)) {
		_jc_post_exception_msg(env, _JC_IllegalArgumentException,
		    "value of type `%s' is not compatible"
		    " with field's type `%s'", stype == _JC_TYPE_REFERENCE ?
		       (value->l == NULL ? "null" : value->l->type->name) :
		      _jc_prim_names[stype], field->type->name);
		return JNI_ERR;
	}

	/* Handle fields of reference type */
	if (stype == _JC_TYPE_REFERENCE) {

		/* Always OK to set field to null */
		if (value->l == NULL) {
			*(_jc_object **)data = NULL;
			return JNI_OK;
		}

		/* Check type compatibility */
		switch (_jc_instance_of(env, value->l, field->type)) {
		case 1:
			break;
		case 0:
			_jc_post_exception_msg(env,
			    _JC_IllegalArgumentException,
			    "value of type `%s' is not compatible with"
			    " field's type `%s'", value->l->type->name,
			    field->type->name);
			/* FALLTHROUGH */
		case -1:
			return JNI_ERR;
		default:
			_JC_ASSERT(JNI_FALSE);
		}

		/* Types are compatible, make the assignment */
		*(_jc_object **)data = value->l;
		return JNI_OK;
	}

	/* Convert primitive value to the field's type */
	if ((status = _jc_convert_primitive(env,
	    dtype, stype, value)) != JNI_OK)
		return status;

	/* Set the field's content */
	switch (dtype) {
	case _JC_TYPE_BOOLEAN:
		*(jboolean *)data = value->z;
		break;
	case _JC_TYPE_BYTE:
		*(jbyte *)data = value->b;
		break;
	case _JC_TYPE_CHAR:
		*(jchar *)data = value->c;
		break;
	case _JC_TYPE_SHORT:
		*(jshort *)data = value->s;
		break;
	case _JC_TYPE_INT:
		*(jint *)data = value->i;
		break;
	case _JC_TYPE_LONG:
		*(jlong *)data = value->j;
		break;
	case _JC_TYPE_FLOAT:
		*(jfloat *)data = value->f;
		break;
	case _JC_TYPE_DOUBLE:
		*(jdouble *)data = value->d;
		break;
	default:
		_jc_fatal_error(vm, "impossible");
	}

	/* Done */
	return JNI_OK;
}

/*
 * Validate the field access and return a pointer to the field itself.
 */
static void *
_jc_field_validate(_jc_env *env, _jc_object *this,
	_jc_field **fieldp, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *calling_class;
	_jc_field *field;
	int is_static;
	int ptype;

	/* Get field info */
	field = _jc_check_field(env, this);
	*fieldp = field;
	ptype = (field->type->flags & _JC_TYPE_MASK);
	is_static = _JC_ACC_TEST(field, STATIC);

	/* Check the validity of the instance object */
	if (!is_static) {
		if (obj == NULL) {
			_jc_post_exception(env, _JC_NullPointerException);
			return NULL;
		}
		switch (_jc_instance_of(env, obj, field->class)) {
		case 1:
			break;
		case 0:
			_jc_post_exception_msg(env,
			    _JC_IllegalArgumentException,
			    "field and target object don't match");
			/* FALLTHROUGH */
		case -1:
			return NULL;
		default:
			_JC_ASSERT(JNI_FALSE);
		}
	}

	/* Check accessibility */
	if (_jc_invoke_virtual(env,
	    vm->boot.methods.AccessibleObject.isAccessible, this) != JNI_OK)
		return NULL;
	if (env->retval.i)
		goto accessible;

	/* Check access */
	switch (_jc_reflect_accessible(env, field->class,
	    field->access_flags, &calling_class)) {
	case -1:
		_jc_throw_exception(env);
	case 1:
		break;
	case 0:
		_jc_post_exception_msg(env, _JC_IllegalAccessException,
		    "`%s.%s' is not accessible from `%s'",
		    field->class->name, field->name, calling_class->name);
		_jc_throw_exception(env);
	}

accessible:
	/* Initialize class if field is static */
	if (is_static && _jc_initialize_type(env, field->class) != JNI_OK)
		return NULL;

	/* Return pointer to the field */
	return is_static ?
	    (char *)field->class->u.nonarray.class_fields + field->offset :
	    (char *)obj + field->offset;
}

