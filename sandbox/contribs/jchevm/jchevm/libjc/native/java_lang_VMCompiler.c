
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
 * $Id: java_lang_VMCompiler.c,v 1.2 2005/05/15 21:41:01 archiecobbs Exp $
 */

#include "libjc.h"
#include "java_lang_VMCompiler.h"

/*
 * public static final native boolean compileClass(Class)
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_VMCompiler_compileClass(_jc_env *env, _jc_object *cl)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *type;

	/* Check for null */
	if (cl == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Get type */
	type = _jc_get_vm_pointer(cl, vm->boot.fields.Class.vmdata);

	/* Generate ELF object */
	if (_jc_generate_object(env, type->loader, type->name) != JNI_OK)
		_jc_throw_exception(env);
	return JNI_TRUE;
}

/*
 * public static final native boolean compileClasses(String)
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_VMCompiler_compileClasses(_jc_env *env, _jc_object *string)
{
	_jc_jvm *const vm = env->vm;
	size_t slen;
	char *name;

	/* Decode string */
	slen = _jc_decode_string_utf8(env, string, NULL);
	if ((name = _JC_STACK_ALLOC(env, slen + 1)) == NULL) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}
	_jc_decode_string_utf8(env, string, name);

	/* XXX we are supposed to do some kind of pattern matching... */

	/* Generate ELF object */
	if (_jc_generate_object(env, vm->boot.loader, name) != JNI_OK)
		_jc_throw_exception(env);
	return JNI_TRUE;
}

/*
 * public static final native void disable()
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMCompiler_disable(_jc_env *env)
{
	env->vm->compiler_disabled = JNI_TRUE;
}

/*
 * public static final native void enable()
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMCompiler_enable(_jc_env *env)
{
	env->vm->compiler_disabled = JNI_FALSE;
}

