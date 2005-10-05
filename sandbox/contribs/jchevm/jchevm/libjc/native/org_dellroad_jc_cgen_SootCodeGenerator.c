
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
 * $Id: org_dellroad_jc_cgen_SootCodeGenerator.c,v 1.8 2005/05/15 21:41:01 archiecobbs Exp $
 */

#include "libjc.h"
#include "org_dellroad_jc_cgen_SootCodeGenerator.h"

/*
 * private static native void setField(Object, String, String, Object)
 */
void _JC_JCNI_ATTR
JCNI_org_dellroad_jc_cgen_SootCodeGenerator_setField(_jc_env *env,
	_jc_object *obj, _jc_object *nameString, _jc_object *sigString,
	_jc_object *value)
{
	_jc_type *type;
	size_t name_len;
	size_t sig_len;
	char *name;
	char *signature;

	/* Check for null */
	if (obj == NULL || nameString == NULL || sigString == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Convert name and signature to UTF-8 */
	name_len = _jc_decode_string_utf8(env, nameString, NULL);
	sig_len = _jc_decode_string_utf8(env, sigString, NULL);
	if ((name = _JC_STACK_ALLOC(env, name_len + 1 + sig_len + 1)) == NULL) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}
	signature = name + name_len + 1;
	_jc_decode_string_utf8(env, nameString, name);
	_jc_decode_string_utf8(env, sigString, signature);

	/* Search for named field; it must also be compatible */
	for (type = obj->type; type != NULL; type = type->superclass) {
		_jc_field *field;

		/* Search for field */
		if ((field = _jc_get_declared_field(env,
		    type, name, signature, JNI_FALSE)) == NULL)
			continue;

		/* Check compatibility */
		if (_jc_sig_types[(u_char)*field->signature]
		    != _JC_TYPE_REFERENCE)
			continue;
		if (value != NULL) {
			switch (_jc_instance_of(env, value, field->type)) {
			case 1:
				break;
			case 0:
				continue;
			case -1:
				_jc_throw_exception(env);
			default:
				_JC_ASSERT(JNI_FALSE);
			}
		}

		/* Assign new value */
		*((_jc_object **)((char *)obj + field->offset)) = value;
		return;
	}

	/* Not found */
	_jc_post_exception_msg(env, _JC_NoSuchFieldError,
	    "%s.%s, type %s, assignable from %s", obj->type->name, name,
	    signature, value != NULL ? value->type->name : "null");
	_jc_throw_exception(env);
}

