
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
static jint	_jc_initialize_class(_jc_env *env, _jc_type *type);
static jint	_jc_initialize_fields(_jc_env *env, _jc_type *type);

/*
 * Initialize a type.
 */
jint
_jc_initialize_type(_jc_env *env, _jc_type *type)
{
	_jc_jvm *const vm = env->vm;
	jint status;

	/* Already initialized? */
	if (_JC_FLG_TEST(type, INITIALIZED)) {
		_JC_ASSERT(_JC_FLG_TEST(type, VERIFIED));
		_JC_ASSERT(_JC_FLG_TEST(type, PREPARED));
		_JC_ASSERT(_JC_FLG_TEST(type, RESOLVED));
		return JNI_OK;
	}

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));

	/* Defer initialization when doing initial bootstrapping */
	if (vm->initialization != NULL && !vm->initialization->may_execute)
		return JNI_OK;

	/* Resolve the type first */
	if (!_JC_FLG_TEST(type, RESOLVED)
	    && (status = _jc_resolve_type(env, type)) != JNI_OK)
		return status;

	/* Initialize class */
	if ((status = _jc_initialize_class(env, type)) != JNI_OK)
		return status;

	/* Done */
	return JNI_OK;
}

/*
 * Initialize a type per JVM spec, 2nd edition, sec. 2.17.5.
 *
 * During bootstrap initialization, a type's Class instance may
 * not exist yet, in which case we skip the locking stuff.
 */
static jint
_jc_initialize_class(_jc_env *env, _jc_type *type)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *const obj = type->instance;
	jboolean locked = JNI_FALSE;
	_jc_method *method;
	jint status;

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));

	/* Step 1 */
	if (obj != NULL) {
		if ((status = _jc_lock_object(env, obj)) != JNI_OK)
			return status;
		locked = JNI_TRUE;
	}

	/* Step 2 */
	while (type->u.nonarray.initializing_thread != NULL
	    && type->u.nonarray.initializing_thread != env) {
		if ((status = _jc_invoke_virtual(env,
		    vm->boot.methods.Object.wait, obj)) != JNI_OK)
			goto fail;
	}

	/* Step 3 */
	if (type->u.nonarray.initializing_thread == env)
		goto done;

	/* Step 4 */
	if (_JC_FLG_TEST(type, INITIALIZED))
		goto done;

	/* Step 5 */
	if (_JC_FLG_TEST(type, INIT_ERROR)) {
		if (locked) {
			locked = JNI_FALSE;
			if ((status = _jc_unlock_object(env, obj)) != JNI_OK)
				goto fail;
		}
		_jc_post_exception_msg(env, _JC_NoClassDefFoundError,
		    "exception during `%s' class initialization", type->name);
		status = JNI_ERR;
		goto fail;
	}

	/* Step 6 */
	type->u.nonarray.initializing_thread = env;
	if (locked) {
		locked = JNI_FALSE;
		if ((status = _jc_unlock_object(env, obj)) != JNI_OK)
			goto fail;
	}

	/* Step 7 */
	if (type->superclass != NULL
	    && !_JC_FLG_TEST(type->superclass, INITIALIZED)
	    && (status = _jc_initialize_type(env, type->superclass)) != JNI_OK)
		goto step11;

	/* Verbosity */
	if (type->loader == vm->boot.loader) {
		VERBOSE(INIT, vm, "initializing `%s' (in bootstrap loader)",
		    type->name);
	} else {
		VERBOSE(INIT, vm, "initializing `%s' (in %s@%p)",
		    type->name, type->loader->instance->type->name,
		    type->loader->instance);
	}

	/* Initialize lockword, first without using superclass */
	if (!_JC_ACC_TEST(type, INTERFACE)
	    && type != vm->boot.types.Object
	    && type != vm->boot.types.Class)
		_jc_initialize_lockword(env, type, NULL);

	/* Step 8 */
	if ((status = _jc_initialize_fields(env, type)) != JNI_OK)
		goto fail;
	if ((method = _jc_get_declared_method(env, type,
	      "<clinit>", "()V", _JC_ACC_STATIC, _JC_ACC_STATIC)) != NULL
	    && (status = _jc_invoke_static(env, method)) != JNI_OK)
		goto step10;

	/* Step 9 */
	if (obj != NULL) {
		if ((status = _jc_lock_object(env, obj)) != JNI_OK)
			return status;
		locked = JNI_TRUE;
	}
	type->flags |= _JC_TYPE_INITIALIZED;
	type->u.nonarray.initializing_thread = NULL;
	goto done;

step10:
	/* Step 10 */
	_JC_ASSERT(env->pending != NULL);
	if (!_jc_subclass_of(env->pending, vm->boot.types.Error)) {
		_jc_word param;
		jobject eref;

		if ((eref = _jc_new_local_native_ref(env,
		    _jc_retrieve_exception(env, NULL))) == NULL) {
			status = JNI_ERR;
			goto fail;
		}
		param = (_jc_word)*eref;
		_jc_post_exception_params(env,
		    _JC_ExceptionInInitializerError, &param);
		_jc_free_local_native_ref(&eref);
	}

step11:
	/* Step 11 */
	if (obj != NULL) {
		if ((status = _jc_lock_object(env, obj)) != JNI_OK)
			return status;
		locked = JNI_TRUE;
	}
	type->flags |= _JC_TYPE_INIT_ERROR;
	type->u.nonarray.initializing_thread = NULL;
	if (obj != NULL
	    && (status = _jc_invoke_virtual(env,
	      vm->boot.methods.Object.notifyAll, obj)) != JNI_OK)
		goto fail;
	status = JNI_ERR;
	goto fail;

done:
	/* Initialize lockword again, this time using superclass */
	if (!_JC_ACC_TEST(type, INTERFACE)
	    && type != vm->boot.types.Object
	    && type != vm->boot.types.Class)
		_jc_initialize_lockword(env, type, type->superclass);

	/* Unlock and return */
	if (locked) {
		locked = JNI_FALSE;
		if ((status = _jc_unlock_object(env, obj)) != JNI_OK)
			return status;
	}

	/* Done */
	return JNI_OK;

fail:
	/* Clean up after failure */
	_JC_ASSERT(status != JNI_OK);
	if (locked) {
		jint status2;

		if ((status2 = _jc_unlock_object(env, obj)) != JNI_OK)
			status = status2;
	}
	return status;
}

/*
 * Initializing any static fields that have a "ConstantValue"
 * attribute providing an initial value.
 */
static jint
_jc_initialize_fields(_jc_env *env, _jc_type *type)
{
	int i;

	/* Sanity check */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));

	/* Initialize static fields */
	for (i = 0; i < type->u.nonarray.num_fields; i++) {
		_jc_field *const field = type->u.nonarray.fields[i];
		u_char ptype;
		void *value;

		/* Ignore non-static and uninitialized fields */
		if (!_JC_ACC_TEST(field, STATIC)
		    || field->initial_value == NULL)
			continue;

		/* Get pointer to the field we want to initialize */
		value = ((char *)type->u.nonarray.class_fields + field->offset);

		/* Set the field's value */
		switch ((ptype = _jc_sig_types[(u_char)*field->signature])) {
		case _JC_TYPE_BOOLEAN:
		case _JC_TYPE_BYTE:
		case _JC_TYPE_CHAR:
		case _JC_TYPE_SHORT:
		case _JC_TYPE_INT:
		case _JC_TYPE_LONG:
			memcpy(value, field->initial_value,
			    _jc_type_sizes[ptype]);
			break;
		case _JC_TYPE_FLOAT:
		    {
			const u_char *const b = field->initial_value;

			memcpy(value, b, sizeof(jfloat));
			break;
		    }
		case _JC_TYPE_DOUBLE:
		    {
			const u_char *const b = field->initial_value;

			memcpy(value, b, sizeof(jdouble));
			break;
		    }
		case _JC_TYPE_REFERENCE:
		    {
			const char *const utf8 = field->initial_value;
			_jc_object *string;

			_JC_ASSERT(field->type == env->vm->boot.types.String);
			if ((string = _jc_new_intern_string(env,
			    utf8, strlen(utf8))) == NULL)
				return JNI_ERR;
			*((_jc_object **)value) = string;
			break;
		    }
		default:
			_JC_ASSERT(JNI_FALSE);
			break;
		}
	}

	/* Done */
	return JNI_OK;
}

