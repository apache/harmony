
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
#include "gnu_classpath_VMStackWalker.h"
#include "gnu_classpath_VMSystemProperties.h"
#include "java_lang_Thread.h"
#include "java_lang_VMClass.h"
#include "java_lang_VMClassLoader.h"
#include "java_lang_VMCompiler.h"
#include "java_lang_VMObject.h"
#include "java_lang_VMRuntime.h"
#include "java_lang_VMSystem.h"
#include "java_lang_VMThread.h"
#include "java_lang_VMThrowable.h"
#include "java_lang_reflect_Constructor.h"
#include "java_lang_reflect_Field.h"
#include "java_lang_reflect_Method.h"
#include "org_dellroad_jc_vm_DebugThread.h"
#include "org_dellroad_jc_vm_FinalizerThread.h"

typedef jint	jni_onload_t(JavaVM *jvm, void *reserved);

/* Internal functions */
static void	*_jc_dlsym(void *handle, const char *name);
static int	_jc_ilib_compare(const void *item1, const void *item2);

/*
 * Internal JCNI native methods.
 *
 * NOTE: This table must be kept up to date and sorted!
 */
#define _JC_ILIB_ENTRY(method)	{ "JCNI_" #method, JCNI_ ## method }
static const _jc_ilib_entry _jc_ilib_table[] = {
	_JC_ILIB_ENTRY(gnu_classpath_VMStackWalker_getClassContext),
	_JC_ILIB_ENTRY(gnu_classpath_VMStackWalker_getClassLoader),
	_JC_ILIB_ENTRY(gnu_classpath_VMSystemProperties_preInit),
	_JC_ILIB_ENTRY(java_lang_VMClassLoader_defineClass),
	_JC_ILIB_ENTRY(java_lang_VMClassLoader_findLoadedClass),
	_JC_ILIB_ENTRY(java_lang_VMClassLoader_getPrimitiveClass),
	_JC_ILIB_ENTRY(java_lang_VMClassLoader_loadClass),
	_JC_ILIB_ENTRY(java_lang_VMClassLoader_resolveClass),
	_JC_ILIB_ENTRY(java_lang_VMClass_forName),
	_JC_ILIB_ENTRY(java_lang_VMClass_getClassLoader),
	_JC_ILIB_ENTRY(java_lang_VMClass_getComponentType),
	_JC_ILIB_ENTRY(java_lang_VMClass_getDeclaredClasses),
	_JC_ILIB_ENTRY(java_lang_VMClass_getDeclaredConstructors),
	_JC_ILIB_ENTRY(java_lang_VMClass_getDeclaredFields),
	_JC_ILIB_ENTRY(java_lang_VMClass_getDeclaredMethods),
	_JC_ILIB_ENTRY(java_lang_VMClass_getDeclaringClass),
	_JC_ILIB_ENTRY(java_lang_VMClass_getInterfaces),
	_JC_ILIB_ENTRY(java_lang_VMClass_getModifiers),
	_JC_ILIB_ENTRY(java_lang_VMClass_getName),
	_JC_ILIB_ENTRY(java_lang_VMClass_getSuperclass),
	_JC_ILIB_ENTRY(java_lang_VMClass_isArray),
	_JC_ILIB_ENTRY(java_lang_VMClass_isAssignableFrom),
	_JC_ILIB_ENTRY(java_lang_VMClass_isInstance),
	_JC_ILIB_ENTRY(java_lang_VMClass_isInterface),
	_JC_ILIB_ENTRY(java_lang_VMClass_isPrimitive),
	_JC_ILIB_ENTRY(java_lang_VMClass_throwException),
	_JC_ILIB_ENTRY(java_lang_VMObject_clone),
	_JC_ILIB_ENTRY(java_lang_VMObject_getClass),
	_JC_ILIB_ENTRY(java_lang_VMObject_notify),
	_JC_ILIB_ENTRY(java_lang_VMObject_notifyAll),
	_JC_ILIB_ENTRY(java_lang_VMObject_wait),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_availableProcessors),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_exit),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_freeMemory),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_gc),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_mapLibraryName),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_maxMemory),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_nativeLoad),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_runFinalization),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_runFinalizationForExit),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_runFinalizersOnExit),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_totalMemory),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_traceInstructions),
	_JC_ILIB_ENTRY(java_lang_VMRuntime_traceMethodCalls),
	_JC_ILIB_ENTRY(java_lang_VMSystem_arraycopy),
	_JC_ILIB_ENTRY(java_lang_VMSystem_currentTimeMillis),
	_JC_ILIB_ENTRY(java_lang_VMSystem_getenv),
	_JC_ILIB_ENTRY(java_lang_VMSystem_identityHashCode),
	_JC_ILIB_ENTRY(java_lang_VMSystem_setErr),
	_JC_ILIB_ENTRY(java_lang_VMSystem_setIn),
	_JC_ILIB_ENTRY(java_lang_VMSystem_setOut),
	_JC_ILIB_ENTRY(java_lang_VMThread_countStackFrames),
	_JC_ILIB_ENTRY(java_lang_VMThread_currentThread),
	_JC_ILIB_ENTRY(java_lang_VMThread_interrupt),
	_JC_ILIB_ENTRY(java_lang_VMThread_interrupted),
	_JC_ILIB_ENTRY(java_lang_VMThread_isInterrupted),
	_JC_ILIB_ENTRY(java_lang_VMThread_nativeSetPriority),
	_JC_ILIB_ENTRY(java_lang_VMThread_nativeStop),
	_JC_ILIB_ENTRY(java_lang_VMThread_resume),
	_JC_ILIB_ENTRY(java_lang_VMThread_start),
	_JC_ILIB_ENTRY(java_lang_VMThread_suspend),
	_JC_ILIB_ENTRY(java_lang_VMThread_yield),
	_JC_ILIB_ENTRY(java_lang_VMThrowable_fillInStackTrace),
	_JC_ILIB_ENTRY(java_lang_VMThrowable_getStackTrace),
	_JC_ILIB_ENTRY(java_lang_reflect_Constructor_constructNative),
	_JC_ILIB_ENTRY(java_lang_reflect_Constructor_getExceptionTypes),
	_JC_ILIB_ENTRY(java_lang_reflect_Constructor_getModifiers),
	_JC_ILIB_ENTRY(java_lang_reflect_Constructor_getParameterTypes),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_get),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getBoolean),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getByte),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getChar),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getDouble),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getFloat),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getInt),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getLong),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getModifiers),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getShort),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_getType),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_set),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_setBoolean),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_setByte),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_setChar),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_setDouble),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_setFloat),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_setInt),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_setLong),
	_JC_ILIB_ENTRY(java_lang_reflect_Field_setShort),
	_JC_ILIB_ENTRY(java_lang_reflect_Method_getExceptionTypes),
	_JC_ILIB_ENTRY(java_lang_reflect_Method_getModifiers),
	_JC_ILIB_ENTRY(java_lang_reflect_Method_getParameterTypes),
	_JC_ILIB_ENTRY(java_lang_reflect_Method_getReturnType),
	_JC_ILIB_ENTRY(java_lang_reflect_Method_invokeNative),
	_JC_ILIB_ENTRY(org_dellroad_jc_vm_DebugThread_dumpDebugInfo),
	_JC_ILIB_ENTRY(org_dellroad_jc_vm_FinalizerThread_finalizeObjects),
};
#undef _JC_ILIB_ENTRY
#define NUM_ILIB_ENTRIES    (sizeof(_jc_ilib_table) / sizeof(*_jc_ilib_table))

/*
 * Invoke a native method.
 *
 * This function is called from the method "wrapper" function for
 * a native method to invoke the actual native implementation function.
 * It is also called by the interpreter to invoke native methods.
 *
 * The first parameter should be the instance object iff the method
 * is non-static.
 *
 * 'values' is one of:
 *	0	next parameter is a va_list
 *	1	next parameter is a _jc_word *
 *
 * Posts an exception on failure.
 */
jint
_jc_invoke_native_method(_jc_env *env, _jc_method *method, int values, ...)
{
	_jc_jvm *const vm = env->vm;
	const jboolean is_static = _JC_ACC_TEST(method, STATIC);
	_jc_object *this = NULL;
	jboolean is_jni;
	va_list args;
	jint status;

	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(method, NATIVE));

	/* Resolve method */
	if (method->native_function == NULL
	    && _jc_resolve_native_method(env, method) != JNI_OK)
		return JNI_ERR;
	_JC_ASSERT(method->native_function != NULL);
	is_jni = !_JC_ACC_TEST(method, JCNI);

	/* Initialize the class if necessary */
	if (is_static
	    && !_JC_FLG_TEST(method->class, INITIALIZED)
	    && (status = _jc_initialize_type(env, method->class)) != JNI_OK)
		return JNI_ERR;

	/* Verbosity */
	VERBOSE(JNI_INVOKE, vm, "invoking native method %s.%s%s (%s)",
	    method->class->name, method->name, method->signature,
	    is_jni ? "JNI" : "JCNI");

	/* Invoke the native method */
	va_start(args, values);
	if (values) {
		_jc_word *params;

		/* Get parameters */
		params = va_arg(args, _jc_word *);

		/* Get instance object */
		if (!is_static)
			this = (_jc_object *)*params++;

		/* Invoke method */
		status = is_jni ?
		    _jc_invoke_jni_a(env, method,
		      method->native_function, this, params) :
		    _jc_invoke_jcni_a(env, method,
		      method->native_function, this, params);
	} else {
		va_list params;

		/* Get parameters */
		params = va_arg(args, va_list);

		/* Get instance object */
		if (!is_static)
			this = va_arg(params, _jc_object *);

		/* Invoke method */
		status = _jc_invoke_v(env, method,
		    method->native_function, this, params, is_jni); 
	}
	va_end(args);

	/* Verbosity */
	VERBOSE(JNI_INVOKE, vm, "%s from native method %s.%s%s",
	    status == JNI_OK ? "returned" : env->pending->type->name,
	    method->class->name, method->name, method->signature);

	/* Done */
	return status;
}

/*
 * Load the native library with pathname "name" and associate
 * it with the given class loader.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_load_native_library(_jc_env *env,
	_jc_class_loader *loader, const char *name)
{
	_jc_jvm *const vm = env->vm;
	_jc_native_lib *lib = NULL;
	jboolean added_to_list = JNI_FALSE;
	jboolean loader_locked = JNI_FALSE;
	jni_onload_t *on_load;
	void *handle = NULL;
	const char *dlname;

	/* Get dlopen() name */
	dlname = (strcmp(name, _JC_INTERNAL_NATIVE_LIBRARY) == 0) ?
	    NULL : name;

#ifndef NDEBUG
	if (dlname == NULL) {
	    int i;

	    /* Sanity check that _jc_ilib_table is sorted */
	    for (i = 0; i < NUM_ILIB_ENTRIES - 1; i++) {
		    const _jc_ilib_entry *const entry1 = &_jc_ilib_table[i];
		    const _jc_ilib_entry *const entry2 = &_jc_ilib_table[i + 1];

		    _JC_ASSERT(_jc_ilib_compare(entry1, entry2) < 0);
	    }
	}
#endif

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);
	loader_locked = JNI_TRUE;

retry:
	/* See if library is already loaded or being loaded in another thread */
	STAILQ_FOREACH(lib, &loader->native_libs, link) {

		/* Does the name match? */
		if (strcmp(lib->name, name) != 0)
			continue;

		/* Wait for other thread in progress to finish */
		if (lib->handle == NULL) {
			_jc_loader_wait(env, loader);
			goto retry;
		}

		/* Done */
		goto done;
	}

	/* Create new native library entry */
	if ((lib = _jc_cl_zalloc(env, loader, sizeof(*lib))) == NULL
	    || (lib->name = _jc_cl_strdup(env, loader, name)) == NULL)
		goto fail;

	/* Add native library to the list (note: lib->handle is still NULL) */
	STAILQ_INSERT_TAIL(&loader->native_libs, lib, link);
	added_to_list = JNI_TRUE;

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);
	loader_locked = JNI_FALSE;

	/* Verbosity */
	if (loader == vm->boot.loader) {
		VERBOSE(JNI, vm,
		    "loading native library `%s' (boot loader)", name);
	} else {
		VERBOSE(JNI, vm,
		    "loading native library `%s' (%s@%p)", name,
		    loader->instance->type->name, loader->instance);
	}

	/* Try to open the shared library; special case libjc */
	if (dlname == NULL)
		handle = _JC_INTERNAL_LIBRARY_HANDLE;
	else if ((handle = dlopen(dlname, RTLD_NOW)) == NULL) {
		_JC_EX_STORE(env, UnsatisfiedLinkError,
		    "failed to open native library `%s': %s", name, dlerror());
		goto fail;
	}

	/* Invoke JNI_OnLoad() (if any) */
	if ((on_load = _jc_dlsym(handle, "JNI_OnLoad")) != NULL) {
		const char *vname = NULL;
		jint version;

		VERBOSE(JNI, vm, "invoking JNI_OnLoad() in `%s'", name);
		version = (*on_load)(_JC_JVM2JNI(vm), NULL);
		switch (version) {
		case JNI_VERSION_1_1:
			vname = "1.1";
			break;
		case JNI_VERSION_1_2:
			vname = "1.2";
			break;
		case JNI_VERSION_1_4:
			vname = "1.4";
			break;
		default:
			_JC_EX_STORE(env, UnsatisfiedLinkError,
			    "unrecognized JNI version %d required by"
			    " native library `%s'", version, name);
			goto fail;
		}
		VERBOSE(JNI, vm, "native library `%s' supports JNI version %s",
		    name, vname);
	}

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);
	loader_locked = JNI_TRUE;

	/* Set handle, marking the loading of this library as complete */
	lib->handle = handle;

	/* Wake up any waiters */
	if (loader->waiters) {
		loader->waiters = JNI_FALSE;
		_JC_COND_BROADCAST(loader->cond);
	}

done:
	/* Unlock loader */
	_JC_ASSERT(loader_locked);
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Done */
	return JNI_OK;

fail:
	/* Lock loader */
	if (!loader_locked)
		_JC_MUTEX_LOCK(env, loader->mutex);

	/* Clean up after failure */
	if (added_to_list) {
		_JC_ASSERT(lib->handle == NULL);
		STAILQ_REMOVE(&loader->native_libs, lib, _jc_native_lib, link);
	}
	if (handle != NULL)
		dlclose(handle);
	if (lib != NULL) {
		_jc_cl_unalloc(loader, &lib->name, strlen(lib->name) + 1);
		_jc_cl_unalloc(loader, &lib, sizeof(*lib));
	}

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Done */
	return JNI_ERR;
}

/*
 * Unload all native libraries associated with a class loader.
 *
 * This should only be called when a ClassLoader is being garbage
 * collected. The memory for the _jc_native_lib structures will get
 * freed automatically along with the rest of the per-loader memory.
 */
void
_jc_unload_native_libraries(_jc_jvm *vm, _jc_class_loader *loader)
{
	while (!STAILQ_EMPTY(&loader->native_libs)) {
		_jc_native_lib *const lib = STAILQ_FIRST(&loader->native_libs);
		jni_onload_t *on_unload;

		/* Verbosity */
		VERBOSE(JNI, vm, "unloading native library `%s'", lib->name);

		/* Unlink it */
		STAILQ_REMOVE_HEAD(&loader->native_libs, link);

		/* Invoke JNI_OnUnload() */
		if ((on_unload = _jc_dlsym(lib->handle,
		    "JNI_OnUnload")) != NULL) {
			VERBOSE(JNI, vm, "invoking JNI_OnUnload() in native"
			    " library `%s'", lib->name);
			(*on_unload)(_JC_JVM2JNI(vm), NULL);
		}

		/* Close shared library */
		if (lib->handle != _JC_INTERNAL_LIBRARY_HANDLE
		    && dlclose(lib->handle) == -1) {
			_jc_eprintf(vm, "%s(%s): %s\n",
			    "dlclose", lib->name, dlerror());
		}
	}
}

/* 
 * Resolve a native method.
 *
 * Posts an exception on failure.
 */
jint
_jc_resolve_native_method(_jc_env *env, _jc_method *method)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *const class = method->class;
	_jc_class_loader *const loader = class->loader;
	const void *func = NULL;
	_jc_native_lib *lib;
	char *jni_name;
	char *s;

	/* Sanity check */
	_JC_ASSERT(_JC_ACC_TEST(method, NATIVE));

	/* Verbosity */
	VERBOSE(JNI, vm, "resolving native method `%s.%s%s'",
	    method->class->name, method->name, method->signature);

	/* Allocate buffer to hold encoded name */
	if ((jni_name = _JC_STACK_ALLOC(env,
	      5 + _jc_jni_encode_length(class->name)
	    + 1 + _jc_jni_encode_length(method->name)
	    + 2 + _jc_jni_encode_length(method->signature) + 1)) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Generate short JNI function name */
	s = jni_name;
	_jc_jni_encode(&s, "Java/");
	_jc_jni_encode(&s, class->name);
	_jc_jni_encode(&s, "/");
	_jc_jni_encode(&s, method->name);
	*s = '\0';

	/* Lock loader */
	_JC_MUTEX_LOCK(env, loader->mutex);

	/* Search native libraries associated with method's class loader */
	STAILQ_FOREACH(lib, &loader->native_libs, link) {
		if ((func = _jc_dlsym(lib->handle, jni_name)) != NULL) {
			method->access_flags &= ~_JC_ACC_JCNI;
			goto found;
		}
	}

	/* Try short JCNI function name */
	strncpy(jni_name, "JCNI", 4);
	STAILQ_FOREACH(lib, &loader->native_libs, link) {
		if ((func = _jc_dlsym(lib->handle, jni_name)) != NULL) {
			method->access_flags |= _JC_ACC_JCNI;
			goto found;
		}
	}

	/* Generate long JNI function name */
	_jc_jni_encode(&s, "//");
	_jc_jni_encode(&s, method->signature);
	*s = '\0';

	/* Search native libraries associated with method's class loader */
	strncpy(jni_name, "Java", 4);
	STAILQ_FOREACH(lib, &loader->native_libs, link) {
		if ((func = _jc_dlsym(lib->handle, jni_name)) != NULL) {
			method->access_flags &= ~_JC_ACC_JCNI;
			goto found;
		}
	}

	/* Try long JCNI function name */
	strncpy(jni_name, "JCNI", 4);
	STAILQ_FOREACH(lib, &loader->native_libs, link) {
		if ((func = _jc_dlsym(lib->handle, jni_name)) != NULL) {
			method->access_flags |= _JC_ACC_JCNI;
			goto found;
		}
	}

	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Verbosity */
	VERBOSE(JNI, vm, "native method `%s.%s%s' not found",
	    method->class->name, method->name, method->signature);

	/* Not found */
	_jc_post_exception_msg(env, _JC_UnsatisfiedLinkError,
	    "failed to resolve native method `%s.%s%s'", method->class->name,
	    method->name, method->signature);
	return JNI_ERR;

found:
	/* Unlock loader */
	_JC_MUTEX_UNLOCK(env, loader->mutex);

	/* Verbosity */
	VERBOSE(JNI, vm, "found native method `%s.%s%s' at %p/%s in `%s'",
	    method->class->name, method->name, method->signature, func,
	    _JC_ACC_TEST(method, JCNI) ? "JCNI" : "JNI", lib->name);

	/* Done */
	method->native_function = func;
	return JNI_OK;
}

/*
 * dlsym() wrapper that special cases the JC internal native library.
 */
static void *
_jc_dlsym(void *handle, const char *name)
{
	_jc_ilib_entry *entry;
	_jc_ilib_entry key;

	/* Handle normal native libraries */
	if (handle != _JC_INTERNAL_LIBRARY_HANDLE)
		return dlsym(handle, name);

	/* Handle JC internal native library */
	key.name = name;
	if ((entry = bsearch(&key, _jc_ilib_table, NUM_ILIB_ENTRIES,
	    sizeof(*_jc_ilib_table), _jc_ilib_compare)) == NULL)
		return NULL;
	return entry->addr;
}

/*
 * Comparison function for _jc_ilib_entry's.
 */
static int
_jc_ilib_compare(const void *item1, const void *item2)
{
	const _jc_ilib_entry *const entry1 = item1;
	const _jc_ilib_entry *const entry2 = item2;

	return strcmp(entry1->name, entry2->name);
}

