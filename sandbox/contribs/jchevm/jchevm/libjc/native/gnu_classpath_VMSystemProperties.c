
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
 * $Id: gnu_classpath_VMSystemProperties.c,v 1.2 2005/05/15 21:41:01 archiecobbs Exp $
 */

#include "libjc.h"
#include "gnu_classpath_VMSystemProperties.h"

/*
 * static native void preInit(Properties)
 */
void _JC_JCNI_ATTR
JCNI_gnu_classpath_VMSystemProperties_preInit(_jc_env *env, _jc_object *props)
{
	static const char *const method_name = "setProperty";
	static const char *const method_sig
	    = "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;";
	_jc_jvm *const vm = env->vm;
	jobject name_ref = NULL;
	jobject value_ref = NULL;
	jint status = JNI_ERR;
	_jc_method *method;
	_jc_type *type;
	int i;

	/* Find Properties class */
	if ((type = _jc_load_type(env, vm->boot.loader,
	    "java/util/Properties")) == NULL)
		goto done;

	/* Sanity check */
	_JC_ASSERT(_jc_subclass_of(props, type));
	_JC_ASSERT(_JC_FLG_TEST(type, INITIALIZED));

	/* Find Properties.setProperty() method */
	if ((method = _jc_get_declared_method(env, type,
	    method_name, method_sig, _JC_ACC_STATIC, 0)) == NULL) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}

	/* Insert properties */
	for (i = 0; i < vm->system_properties.length; i++) {
		_jc_property *const prop = &vm->system_properties.elems[i];

		/* Create Strings from name and value */
		if ((name_ref = _jc_new_local_native_ref(env,
		    _jc_new_string(env, prop->name,
		      strlen(prop->name)))) == NULL)
			goto done;
		if ((value_ref = _jc_new_local_native_ref(env,
		    _jc_new_string(env, prop->value,
		      strlen(prop->value)))) == NULL)
			goto done;

		/* Invoke method */
		if (_jc_invoke_virtual(env, method,
		    props, *name_ref, *value_ref) != JNI_OK)
			goto done;

		/* Free local references */
		_jc_free_local_native_ref(&name_ref);
		_jc_free_local_native_ref(&value_ref);
	}

	/* OK */
	status = JNI_OK;

done:
	/* Clean up and return */
	_jc_free_local_native_ref(&name_ref);
	_jc_free_local_native_ref(&value_ref);

	/* Bail out if exception */
	if (status != JNI_OK)
		_jc_throw_exception(env);
}

