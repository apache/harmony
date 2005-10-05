
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
 * $Id: java_lang_VMRuntime.c,v 1.7 2005/05/15 21:41:01 archiecobbs Exp $
 */

#include "libjc.h"
#include "java_lang_VMRuntime.h"

/*
 * static final native int availableProcessors()
 */
jint _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_availableProcessors(_jc_env *env)
{
	return _jc_num_cpus(env);
}

/*
 * static final native void exit(int)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_exit(_jc_env *env, jint status)
{
	_jc_jvm *const vm = env->vm;

	(*vm->exit)(status);
	_jc_fatal_error(vm, "exit() returned!");
}

/*
 * static final native long freeMemory()
 */
jlong _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_freeMemory(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;

	return (jlong)(vm->heap.num_pages - vm->heap.next_page)
	    * (jlong)_JC_PAGE_SIZE;
}

/*
 * static final native void gc()
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_gc(_jc_env *env)
{
	if (_jc_gc(env, JNI_FALSE) != JNI_OK) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}
}

/*
 * static final native String mapLibraryName(String)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_mapLibraryName(_jc_env *env, _jc_object *string)
{
	size_t name_len;
	char *fname;
	char *name;

	/* Check for null */
	if (string == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Decode string */
	name_len = _jc_decode_string_utf8(env, string, NULL);
	if ((name = _JC_STACK_ALLOC(env, name_len + 1)) == NULL) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}
	_jc_decode_string_utf8(env, string, name);

	/* Format filename */
	if ((fname = _JC_FORMAT_STRING(env, _JC_LIBRARY_FMT, name)) == NULL) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}

	/* Create new String object */
	if ((string = _jc_new_string(env, fname, strlen(fname))) == NULL)
		_jc_throw_exception(env);

	/* Done */
	return string;
}

/*
 * static final native long maxMemory()
 */
jlong _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_maxMemory(_jc_env *env)
{
	return _JC_JLONG(0x7fffffffffffffff);
}

/*
 * static final native int nativeLoad(String, ClassLoader)
 */
jint _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_nativeLoad(_jc_env *env,
	_jc_object *string, _jc_object *clobj)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *loader;
	char *filename;
	size_t len;

	/* Check for null */
	if (string == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Convert String to UTF-8 */
	len = _jc_decode_string_utf8(env, string, NULL);
	if ((filename = _JC_STACK_ALLOC(env, len + 1)) == NULL) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}
	_jc_decode_string_utf8(env, string, filename);

	/* Get class loader */
	if (clobj == NULL)
		loader = vm->boot.loader;
	else if ((loader = _jc_get_loader(env, clobj)) == NULL)
		_jc_throw_exception(env);

	/* Open native library */
	if (_jc_load_native_library(env, loader, filename) != JNI_OK) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}

	/* OK */
	return 1;
}

/*
 * static final native void runFinalization()
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_runFinalization(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;

	_jc_thread_interrupt_instance(vm, *vm->finalizer_thread);
}

/*
 * static final native void runFinalizationForExit()
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_runFinalizationForExit(_jc_env *env)
{
	/* we don't support doing this so just ignore the request */
}

/*
 * static final native void runFinalizersOnExit(boolean)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_runFinalizersOnExit(_jc_env *env, jboolean enable)
{
	/* we don't support doing this so just ignore the request */
}

/*
 * static final native long totalMemory()
 */
jlong _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_totalMemory(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;

	return (jlong)vm->heap.num_pages * (jlong)_JC_PAGE_SIZE;
}

/*
 * static final native void traceInstructions(boolean)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_traceInstructions(_jc_env *env, jboolean param1)
{
	/* we don't support doing this so just ignore the request */
}

/*
 * static final native void traceMethodCalls(boolean)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMRuntime_traceMethodCalls(_jc_env *env, jboolean enable)
{
	/* we don't support doing this so just ignore the request */
}

