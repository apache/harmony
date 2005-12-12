
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

typedef jint	jni_onload_t(JavaVM *jvm, void *reserved);

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

	/* Try to open the shared library */
	if ((handle = dlopen(dlname, RTLD_NOW)) == NULL) {
		_JC_EX_STORE(env, UnsatisfiedLinkError,
		    "failed to open native library `%s': %s", name, dlerror());
		goto fail;
	}

	/* Invoke JNI_OnLoad() (if any) */
	if ((on_load = dlsym(handle, "JNI_OnLoad")) != NULL) {
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
		if ((on_unload = dlsym(lib->handle, "JNI_OnUnload")) != NULL) {
			VERBOSE(JNI, vm, "invoking JNI_OnUnload() in native"
			    " library `%s'", lib->name);
			(*on_unload)(_JC_JVM2JNI(vm), NULL);
		}

		/* Close shared library */
		if (dlclose(lib->handle) == -1) {
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
		if ((func = dlsym(lib->handle, jni_name)) != NULL) {
			method->access_flags &= ~_JC_ACC_JCNI;
			goto found;
		}
	}

	/* Try short JCNI function name */
	strncpy(jni_name, "JCNI", 4);
	STAILQ_FOREACH(lib, &loader->native_libs, link) {
		if ((func = dlsym(lib->handle, jni_name)) != NULL) {
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
		if ((func = dlsym(lib->handle, jni_name)) != NULL) {
			method->access_flags &= ~_JC_ACC_JCNI;
			goto found;
		}
	}

	/* Try long JCNI function name */
	strncpy(jni_name, "JCNI", 4);
	STAILQ_FOREACH(lib, &loader->native_libs, link) {
		if ((func = dlsym(lib->handle, jni_name)) != NULL) {
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


