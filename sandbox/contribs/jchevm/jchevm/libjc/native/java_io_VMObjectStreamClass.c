
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
 * $Id: java_io_VMObjectStreamClass.c,v 1.4 2005/05/15 21:41:01 archiecobbs Exp $
 */

#include "libjc.h"
#include "java_io_VMObjectStreamClass.h"

/*
 * static final native boolean hasClassInitializer(Class)
 */
jboolean _JC_JCNI_ATTR
JCNI_java_io_VMObjectStreamClass_hasClassInitializer(_jc_env *env,
	_jc_object *class)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *type;

	/* Check for null and get type */
	if (class == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}
	type = _jc_get_vm_pointer(class, vm->boot.fields.Class.vmdata);

	/* Look for class initializer */
	return _jc_get_declared_method(env, type, "<clinit>",
	    "()V", _JC_ACC_STATIC, _JC_ACC_STATIC) != NULL;
}

