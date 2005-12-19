
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

/*
 * Invocation methods
 */
static jint	DestroyJavaVM(JavaVM *vm);
static jint	AttachCurrentThread(JavaVM *vm, void **envp, void *args);
static jint	AttachCurrentThreadAsDaemon(JavaVM *vm,
			void **envp, void *args);
static jint	AttachCurrentThreadInternal(JavaVM *vm,
			void **envp, void *args, jboolean daemon);
static jint	DetachCurrentThread(JavaVM *vm);
static jint	GetEnv(JavaVM *vm, void **envp, jint interface_id);

/* JNI invocation interface table */
const struct JNIInvokeInterface _jc_invoke_interface = {
	NULL,
	NULL,
	NULL,
	DestroyJavaVM,
	AttachCurrentThread,
	DetachCurrentThread,
	GetEnv,
	AttachCurrentThreadAsDaemon
};

/* Linked list of all VMs, protected by a mutex */
static LIST_HEAD(, _jc_jvm)	_jc_vms_list
				    = LIST_HEAD_INITIALIZER(&_jc_vms_list);
static pthread_mutex_t		_jc_vms_mutex = PTHREAD_MUTEX_INITIALIZER;
#ifndef NDEBUG
static _jc_env			*_jc_vms_mutex_owner;
#endif

/* Signal handler state */
static jboolean			_jc_signals_initialized;

/*
 * Get default setup for creating a new VM.
 */
jint
JNI_GetDefaultJavaVMInitArgs(void *_vm_args)
{
	JavaVMInitArgs *vm_args = (JavaVMInitArgs *)_vm_args;
	int error;

	/* Initialize */
	if ((error = _jc_init()) != JNI_OK)
		return error;

	/* We only communicate using the 1.2 interface version */
	if (vm_args->version != JNI_VERSION_1_2)
		return JNI_EVERSION;

	/* Done */
	return JNI_OK;
}

/*
 * Get a list of all VMs that exist.
 */
jint
JNI_GetCreatedJavaVMs(JavaVM **vmBuf, jsize bufLen, jsize *nVMs)
{
	_jc_jvm *vm;
	jint error;
	jsize count;

	/* Initialize */
	if ((error = _jc_init()) != JNI_OK)
		return error;

	/* Acquire lock on the VM list */
	_JC_MUTEX_LOCK(NULL, _jc_vms_mutex);

	/* Add entries */
	count = 0;
	LIST_FOREACH(vm, &_jc_vms_list, link) {
		if (count >= bufLen)
			break;
		vmBuf[count] = _JC_JVM2JNI(vm);
		count++;
	}
	*nVMs = count;

	/* Release lock on the VM list */
	_JC_MUTEX_UNLOCK(NULL, _jc_vms_mutex);

	/* Done */
	return JNI_OK;
}

/*
 * Create a new Java VM.
 */
jint
JNI_CreateJavaVM(JavaVM **jvmp, void **envp, void *args)
{
	_jc_env *env;
	_jc_jvm *vm;
	int status;

	/* Do one-time initialization */
	if ((status = _jc_init()) != JNI_OK)
		return status;

	/* Initialize signals when creating the first VM */
	_JC_MUTEX_LOCK(NULL, _jc_vms_mutex);
	if (!_jc_signals_initialized && _jc_init_signals() == JNI_OK)
		_jc_signals_initialized = JNI_TRUE;
	_JC_MUTEX_UNLOCK(NULL, _jc_vms_mutex);
	if (!_jc_signals_initialized)
		goto fail;

	/* Create VM */
	if ((status = _jc_create_vm(args, &vm, &env)) != JNI_OK)
		goto fail;
	VERBOSE(JNI, vm, "JNI_CreateJavaVM invoked");

	/* Add VM to global VM list */
	_JC_MUTEX_LOCK(NULL, _jc_vms_mutex);
	LIST_INSERT_HEAD(&_jc_vms_list, vm, link);
	_JC_MUTEX_UNLOCK(NULL, _jc_vms_mutex);

	/* Done */
	*envp = _JC_ENV2JNI(env);
	*jvmp = _JC_JVM2JNI(vm);
	return JNI_OK;

fail:
	/* Clean up after failure */
	_JC_ASSERT(status != JNI_OK);
	_JC_MUTEX_LOCK(NULL, _jc_vms_mutex);
	if (LIST_EMPTY(&_jc_vms_list) && _jc_signals_initialized) {
		_jc_restore_signals();
		_jc_signals_initialized = JNI_FALSE;
	}
	_JC_MUTEX_UNLOCK(NULL, _jc_vms_mutex);
	return status;
}

/*
 * Destroy a VM.
 */
static jint
DestroyJavaVM(JavaVM *jvm)
{
	_jc_jvm *vm = _JC_JNI2JVM(jvm);
	_jc_env *env = _jc_get_current_env();
	_jc_c_stack cstack;

	/* This thread must be attached to the jvm */
	if (env == NULL || env->vm != vm)
		return JNI_ERR;

	/* Enter java mode */
	_jc_resuming_java(env, &cstack);
	VERBOSE(JNI, vm, "JNI_DestroyJavaVM invoked");

	/* Kill all other threads */
	_jc_thread_shutdown(&env);

	/* Remove VM from the global list and restore signals after last VM */
	_JC_MUTEX_LOCK(NULL, _jc_vms_mutex);
	LIST_REMOVE(vm, link);
	_JC_ASSERT(_jc_signals_initialized);
	if (LIST_EMPTY(&_jc_vms_list)) {
		_jc_restore_signals();
		_jc_signals_initialized = JNI_FALSE;
	}
	_JC_MUTEX_UNLOCK(NULL, _jc_vms_mutex);

	/* Free VM */
	VERBOSE(JNI, vm, "JNI_DestroyJavaVM: destroying VM");
	_jc_free_vm(&vm);

	/* Done */
	return JNI_OK;
}

/*
 * Attach the current thread to the VM as a user thread.
 */
static jint
AttachCurrentThread(JavaVM *jvm, void **envp, void *args)
{
	return AttachCurrentThreadInternal(jvm, envp, args, JNI_FALSE);
}

/*
 * Attach the current thread to the VM as a daemon thread.
 */
static jint
AttachCurrentThreadAsDaemon(JavaVM *jvm, void **envp, void *args)
{
	return AttachCurrentThreadInternal(jvm, envp, args, JNI_TRUE);
}

/*
 * Attach the current thread to the VM.
 */
static jint
AttachCurrentThreadInternal(JavaVM *jvm,
	void **envp, void *_args, jboolean daemon)
{
	_jc_jvm *const vm = _JC_JNI2JVM(jvm);
	JavaVMAttachArgs *const args = _args;
	_jc_c_stack cstack;
	_jc_ex_info einfo;
	_jc_env *env;

	/* We only communicate using the 1.2 interface version */
	if (args != NULL && args->version != JNI_VERSION_1_2)
		return JNI_EVERSION;

	/* If thread is already attached, just return its pointer */
	if ((env = _jc_get_current_env()) != NULL) {
		*envp = _JC_ENV2JNI(env);
		return JNI_OK;
	}

	/* Create and attach a new thread structure to the current thread */
	_JC_MUTEX_LOCK(NULL, vm->mutex);
	env = _jc_attach_thread(vm, &einfo, &cstack);
	_JC_MUTEX_UNLOCK(NULL, vm->mutex);
	if (env == NULL) {
		_jc_eprintf(vm, "%s: %s: %s\n", __FUNCTION__,
		    _jc_vmex_names[einfo.num], einfo.msg);
		return JNI_ERR;
	}

	/* Sanity check group */
	_JC_ASSERT(args == NULL || args->group == NULL
	    || _jc_subclass_of(*args->group, vm->boot.types.ThreadGroup));

	/* Create java.lang.Thread instance */
	if (_jc_thread_create_instance(env,
	    (args != NULL && args->group != NULL) ?
	      *args->group : vm->boot.objects.systemThreadGroup,
	    (args != NULL && args->name != NULL) ? args->name : NULL,
	    vm->threads.java_prio_norm, daemon) != JNI_OK)
		goto fail;

	/* Returning to native code */
	_jc_stopping_java(env, &cstack, NULL);

	/* Done */
	*envp = _JC_ENV2JNI(env);
	return JNI_OK;

fail:
	/* Clean up and return error */
	_JC_MUTEX_LOCK(NULL, vm->mutex);
	_jc_detach_thread(&env);
	_JC_MUTEX_UNLOCK(NULL, vm->mutex);
	return JNI_ERR;
}

/*
 * Detach the current thread from the VM.
 */
static jint
DetachCurrentThread(JavaVM *jvm)
{
	_jc_jvm *const vm = _JC_JNI2JVM(jvm);
	_jc_env *env = _jc_get_current_env();
	_jc_c_stack cstack;

	/* This thread must be attached to the jvm */
	if (env == NULL || env->vm != vm)
		return JNI_ERR;

	/* Sanity check */
	_JC_ASSERT(env->c_stack == NULL);

	/* Go un-native */
	_jc_resuming_java(env, &cstack);

	/* Grab global mutex */
	_JC_MUTEX_LOCK(NULL, vm->mutex);

	/* Detach this thread */
	_jc_detach_thread(&env);

	/* Unlock global mutex */
	_JC_MUTEX_UNLOCK(NULL, vm->mutex);

	/* Done */
	return JNI_OK;
}

/*
 * Get the JNI environment corresponding to the current thread.
 */
static jint
GetEnv(JavaVM *jvm, void **envp, jint interface_id)
{
	_jc_jvm *const vm = _JC_JNI2JVM(jvm);
	_jc_env *env;
	int error;

	/* Initialize */
	if ((error = _jc_init()) != JNI_OK) {
		*envp = NULL;
		return JNI_EDETACHED;
	}

	/* Check if this thread is attached */
	if ((env = _jc_get_current_env()) == NULL || env->vm != vm) {
		*envp = NULL;
		return JNI_EDETACHED;
	}

	/* Check JNI version */
	if (interface_id != JNI_VERSION_1_2) {
		*envp = NULL;
		return JNI_EVERSION;
	}

	/* OK */
	*envp = _JC_ENV2JNI(env);
	return JNI_OK;
}

