
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
 * $Id: java_lang_VMThread.c,v 1.7 2005/11/09 18:14:22 archiecobbs Exp $
 */

#include "libjc.h"
#include "java_lang_Thread.h"
#include "java_lang_VMThread.h"

/* Linux pthread_create() with supplied attributes is completely broken! */
#ifdef __linux__
#define _JC_NO_THREAD_ATTRIBUTES	1
#endif

/* Internal functions */
static int	_jc_convert_java_prio(_jc_jvm *vm, jint jprio);

/*
 * final native int countStackFrames()
 */
jint _JC_JCNI_ATTR
JCNI_java_lang_VMThread_countStackFrames(_jc_env *env, _jc_object *this)
{
	_jc_jvm *const vm = env->vm;
	_jc_env *thread;
	jint num = 0;

	/* Lock VM so thread doesn't disappear */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Get internal thread structure */
	thread = _jc_get_vm_pointer(vm, this, vm->boot.fields.VMThread.vmdata);
	if (thread == NULL)
		goto done;

	/* Thread must be suspended */
	if (!thread->suspended)
		goto done;

	/* Count stack frames */
	num = _jc_save_stack_frames(env, thread, 0, NULL);

done:
	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Done */
	return num;
}

/*
 * static final native Thread currentThread()
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMThread_currentThread(_jc_env *env)
{
	_JC_ASSERT(env->instance != NULL);
	return env->instance;
}

/*
 * final native void interrupt()
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMThread_interrupt(_jc_env *env, _jc_object *vmthread)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *thisThread;

	/* Get Thread object */
	thisThread = *_JC_VMFIELD(vm, vmthread, VMThread, thread, _jc_object *);
	_JC_ASSERT(thisThread != NULL);

	/* Interrupt thread */
	_jc_thread_interrupt_instance(vm, thisThread);
}

/*
 * static final native boolean interrupted()
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_VMThread_interrupted(_jc_env *env)
{
	switch (env->interrupt_status) {
	case _JC_INTERRUPT_CLEAR:
		return JNI_FALSE;
	case _JC_INTERRUPT_SET:
		env->interrupt_status = _JC_INTERRUPT_CLEAR;    /* no race */
		return JNI_TRUE;
	default:
		_JC_ASSERT(JNI_FALSE);
		return JNI_FALSE;
	}
}

/*
 * final native boolean isInterrupted()
 */
jboolean _JC_JCNI_ATTR
JCNI_java_lang_VMThread_isInterrupted(_jc_env *env, _jc_object *this)
{
	_jc_jvm *const vm = env->vm;
	jboolean result = JNI_FALSE;
	_jc_env *thread;

	/* Lock VM so thread doesn't disappear */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Get thread's thread structure */
	thread = _jc_get_vm_pointer(vm, this, vm->boot.fields.VMThread.vmdata);
	if (thread == NULL)
		goto done;

	/* Return thread's current interrupt status */
	switch (thread->interrupt_status) {
	case _JC_INTERRUPT_CLEAR:
	case _JC_INTERRUPT_INTERRUPTIBLE:
		break;
	case _JC_INTERRUPT_SET:
	case _JC_INTERRUPT_INTERRUPTED:
		result = JNI_TRUE;
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
	}

done:
	/* Done */
	_JC_MUTEX_UNLOCK(env, vm->mutex);
	return result;
}

/*
 * final native void nativeSetPriority(int)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMThread_nativeSetPriority(_jc_env *env, _jc_object *this,
	jint jprio)
{
	_jc_jvm *const vm = env->vm;
	_jc_env *thread;
	int prio;

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Get internal priority value */
	prio = _jc_convert_java_prio(vm, jprio);

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Get internal thread structure */
	thread = _jc_get_vm_pointer(vm, this, vm->boot.fields.VMThread.vmdata);
	if (thread == NULL)
		goto done;

	/* Adjust thread's priority XXX how do we implement this?? */
	//_jc_eprintf(vm, "%s: warning: unimplemented\n", __FUNCTION__);

done:
	/* Done */
	_JC_MUTEX_UNLOCK(env, vm->mutex);
}

/*
 * final native void nativeStop(Throwable)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMThread_nativeStop(_jc_env *env,
	_jc_object *this, _jc_object *throwable)
{
	_jc_jvm *const vm = env->vm;
	_jc_env *thread;

	/* Check for null */
	if (throwable == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Sanity check */
	_JC_ASSERT(_jc_subclass_of(throwable, vm->boot.types.Throwable));

	/* Lock VM so thread doesn't disappear */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Get internal thread structure */
	thread = _jc_get_vm_pointer(vm, this, vm->boot.fields.VMThread.vmdata);
	if (thread == NULL)
		goto done;

	/* Handle case where target is this thread */
	if (thread == env) {
		_JC_MUTEX_UNLOCK(env, vm->mutex);
		_jc_post_exception_object(env, throwable);
		_jc_throw_exception(env);
	}

	/* Cross-post exception; race condition with other threads is OK */
	thread->cross_exception = throwable;

	/* Wake up thread if its sleeping */
	_jc_thread_interrupt(vm, thread);

done:
	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);
}

/*
 * final native void resume()
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMThread_resume(_jc_env *env, _jc_object *this)
{
	_jc_jvm *const vm = env->vm;
	_jc_env *thread;

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Get internal thread structure */
	thread = _jc_get_vm_pointer(vm, this, vm->boot.fields.VMThread.vmdata);
	if (thread == NULL)
		goto done;

	/* Not suspended? */
	if (!thread->suspended)
		goto done;

	/* Resume thread */
	thread->suspended = JNI_FALSE;
	_JC_COND_SIGNAL(thread->resumption);

done:
	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);
}

/*
 * final native void start(long)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMThread_start(_jc_env *env, _jc_object *this, jlong stack_size)
{
	_jc_jvm *const vm = env->vm;
#if _JC_NO_THREAD_ATTRIBUTES
	pthread_attr_t *const attrp = NULL;
#else
	pthread_attr_t attr;
	pthread_attr_t *const attrp = &attr;
	jboolean attr_initialized = JNI_FALSE;
	struct sched_param sparam;
	void *guard_page;
	int prio;
#endif
	pthread_t pthread;
	_jc_object *tobj;
	_jc_env *thread;
	int error;

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/*
	 * Create a new thread structure, but also first check if another
	 * thread is trying to stop the world. This check is required to
	 * avoid the new thread starting up while the world is stopped.
	 */
	snprintf(env->text_status, sizeof(env->text_status), "Thread.start()");
	_jc_halt_if_requested(env);
	thread = _jc_allocate_thread(env);

	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Check result */
	if (thread == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}

	/* Save Thread object associated with the VMThread */
	tobj = *_JC_VMFIELD(vm, this, VMThread, thread, _jc_object *);
	_JC_ASSERT(tobj != NULL);
	thread->instance = tobj;

	/* Thread must have a group */
	if (*_JC_VMFIELD(vm, tobj, Thread, group, _jc_object *) == NULL) {
		_jc_post_exception_msg(env, _JC_IllegalThreadStateException,
		    "thread is not in any thread group");
		goto fail;
	}

	/* Sanity check thread is not already started */
	_JC_ASSERT(_jc_get_vm_pointer(vm,
	    this, vm->boot.fields.VMThread.vmdata) == NULL);

	/* Get requested stack size; use default if zero */
	thread->stack_size = stack_size;
	if (thread->stack_size == 0)
		thread->stack_size = vm->threads.stack_default;

	/* Clip the stack size to be within limits */
	if (vm->threads.stack_maximum > 0
	    && thread->stack_size > vm->threads.stack_maximum)
		thread->stack_size = vm->threads.stack_maximum;
	if (thread->stack_size < vm->threads.stack_minimum)
		thread->stack_size = vm->threads.stack_minimum;
	if (thread->stack_size < PTHREAD_STACK_MIN)
		thread->stack_size = PTHREAD_STACK_MIN;
	if (thread->stack_size < _JC_STACK_OVERFLOW_MARGIN)
		thread->stack_size = _JC_STACK_OVERFLOW_MARGIN;

	/* Add an extra guard page and round up to a full page size */
	thread->stack_size += _JC_PAGE_SIZE;
	thread->stack_size = _JC_ROUNDUP2(thread->stack_size, _JC_PAGE_SIZE);

#if !_JC_NO_THREAD_ATTRIBUTES
	/* Get requested priority (converted to scheduler priority) */
	prio = _jc_convert_java_prio(vm,
	    *_JC_VMFIELD(vm, tobj, Thread, priority, jint));

	/* Allocate stack */
	if ((thread->stack = mmap(NULL, thread->stack_size,
	    PROT_READ|PROT_WRITE, MAP_PRIVATE|MAP_ANON, -1, 0)) == MAP_FAILED) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: %s", "mmap", strerror(errno));
		thread->stack_size = 0;
		thread->stack = NULL;
		goto fail;
	}

	/* Invalidate guard page */
#if _JC_DOWNWARD_STACK
	guard_page = thread->stack;
#else
	guard_page = (char *)thread->stack + thread->stack_size - _JC_PAGE_SIZE;
#endif
	if (mprotect(guard_page, _JC_PAGE_SIZE, PROT_NONE) == -1) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: %s", "mprotect", strerror(errno));
		goto fail;
	}

	/* Initialize pthread attributes */
	if ((error = pthread_attr_init(&attr)) != 0) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: %s", "pthread_attr_init", strerror(error));
		goto fail;
	}
	attr_initialized = JNI_TRUE;

	/* Set pthread stack address and size attributes */
#if HAVE_PTHREAD_ATTR_SETSTACK
	if ((error = pthread_attr_setstack(&attr,
	    thread->stack, thread->stack_size)) != 0) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: %s", "pthread_attr_setstack", strerror(error));
		goto fail;
	}
#else
	if ((error = pthread_attr_setstackaddr(&attr,
#if _JC_DOWNWARD_STACK && defined(__linux__)
		(char *)thread->stack + thread->stack_size
#else
		thread->stack
#endif
	)) != 0) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: %s", "pthread_attr_setstackaddr", strerror(error));
		goto fail;
	}
	if ((error = pthread_attr_setstacksize(&attr,
	    thread->stack_size)) != 0) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: %s", "pthread_attr_setstacksize", strerror(error));
		goto fail;
	}
#endif

#if 0
	/* Set pthread scheduling policy */
	if ((error = pthread_attr_setschedpolicy(&attr, SCHED_RR)) != 0) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: %s", "pthread_attr_setschedpolicy", strerror(error));
		goto fail;
	}
#endif

	/* Set pthread and priority */
	memset(&sparam, 0, sizeof(sparam));
	sparam.sched_priority = prio;
#if 0
	if ((error = pthread_attr_setschedparam(&attr, &sparam)) != 0) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: %s", "pthread_attr_setschedparam", strerror(error));
		goto fail;
	}
#endif
#endif	/* !_JC_NO_THREAD_ATTRIBUTES */

	/* Set the VMThread's vmdata pointer */
	if (_jc_set_vm_pointer(env, this,
	    vm->boot.fields.VMThread.vmdata, thread) != JNI_OK)
	    	goto fail;

	/* Hack to pass the VMThread's Thread pointer without setting it */
	thread->retval.l = this;

	/* Spawn new pthread to execute this thread */
	if ((error = pthread_create(&pthread,
	    attrp, _jc_thread_start, thread)) != 0) {
		(void)_jc_set_vm_pointer(env, this,
		    vm->boot.fields.VMThread.vmdata, NULL);
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: %s", "pthread_create", strerror(error));
		goto fail;
	}

	/* Detach thread */
	error = pthread_detach(pthread);
	_JC_ASSERT(error == 0);

	/* Done */
#if !_JC_NO_THREAD_ATTRIBUTES
	pthread_attr_destroy(&attr);
#endif
	return;

fail:
	/* Clean up after failure */
	_JC_MUTEX_LOCK(env, vm->mutex);
	_jc_free_thread(&thread, JNI_FALSE);
	_JC_MUTEX_UNLOCK(env, vm->mutex);
#if !_JC_NO_THREAD_ATTRIBUTES
	if (attr_initialized)
		pthread_attr_destroy(&attr);
#endif
	_jc_throw_exception(env);
}

/*
 * final native void suspend()
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMThread_suspend(_jc_env *env, _jc_object *this)
{
	_jc_jvm *const vm = env->vm;
	_jc_env *thread;

	/* Lock VM so thread doesn't disappear */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Get internal thread structure */
	thread = _jc_get_vm_pointer(vm, this, vm->boot.fields.VMThread.vmdata);
	if (thread == NULL)
		goto done;

	/* Already suspended? */
	if (thread->suspended)
		goto done;

	/* Stop the world; a way to force it to call _jc_thread_check() */
	_jc_stop_the_world(env);

	/* Mark thread as suspended */
	thread->suspended = JNI_TRUE;

	/* Resume the world; thread will stay suspended */
	_jc_resume_the_world(env);

done:
	/* Unlock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);
}

/*
 * static final native void yield()
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMThread_yield(_jc_env *env)
{
	sched_yield();
}

/*
 * Convert Java thread priority to scheduler thread priority.
 */
static int
_jc_convert_java_prio(_jc_jvm *vm, jint jprio)
{
	int prio;

	prio = (int)(jprio - vm->threads.java_prio_min);
	prio *= vm->threads.prio_max + 1 - vm->threads.prio_min;
	prio /= vm->threads.java_prio_max + 1 - vm->threads.java_prio_min;
	prio += vm->threads.prio_min;
	_JC_ASSERT(prio >= vm->threads.prio_min
	    && prio <= vm->threads.prio_max);
	return prio;
}

