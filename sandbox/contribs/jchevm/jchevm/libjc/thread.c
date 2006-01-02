
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

/* Internal variables */
static pthread_key_t	_jc_env_key;

/* Internal functions */
static void		_jc_set_current_env(_jc_jvm *vm, _jc_env *env);
static void		_jc_thread_suspend(_jc_env *env);

/*
 * Initialize threading.
 */
jint
_jc_thread_init(void)
{
	if ((errno = pthread_key_create(&_jc_env_key, NULL)) != 0) {
		fprintf(stderr, "jc: %s: %s",
		    "pthread_key_create", strerror(errno));
		return JNI_ERR;
	}
	return JNI_OK;
}

/*
 * Get the _jc_env corresponding to the current thread.
 */
_jc_env *
_jc_get_current_env(void)
{
	return (_jc_env *)pthread_getspecific(_jc_env_key);
}

/*
 * Set the _jc_env corresponding to the current thread.
 */
static void
_jc_set_current_env(_jc_jvm *vm, _jc_env *env)
{
	int error;

	if ((error = pthread_setspecific(_jc_env_key, env)) != 0) {
		_jc_fatal_error(vm, "%s: %s\n",
		    "pthread_setspecific", strerror(error));
	}
}

/*
 * Check whether another thread has notified us to take some action.
 * There must not be any currently posted exception.
 *
 * Returns JNI_ERR if there was a cross-posted exception.
 */
jint
_jc_thread_check(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	jint status = JNI_OK;
	_jc_object *ex;

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL
	    || env->status == _JC_THRDSTAT_HALTING_NORMAL);
	_JC_ASSERT(env->pending == NULL);

	/* Check for halt requested */
	if (env->status == _JC_THRDSTAT_HALTING_NORMAL) {
		_JC_MUTEX_LOCK(env, vm->mutex);
		snprintf(env->text_status, sizeof(env->text_status),
		    "executing Java code");
		_jc_halt_if_requested(env);
		_JC_MUTEX_UNLOCK(env, vm->mutex);
	}

	/* Check for being suspended */
	if (env->suspended)
		_jc_thread_suspend(env);

	/* Check for an exception posted by another thread */
	if (env->cross_exception != NULL
	    && (ex = _jc_retrieve_cross_exception(env)) != NULL) {
		VERBOSE(EXCEPTIONS, vm, "cross-posting `%s' in thread %p",
		    ex->type->name, env);
		env->pending = ex;
		status = JNI_ERR;
	}

	/* Done */
	return status;
}

/*
 * Suspend the current thread.
 */
static void
_jc_thread_suspend(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL
	    || env->status == _JC_THRDSTAT_HALTING_NORMAL);

	/* Initialize resume condition variable if not already done */
	if (!env->resumption_initialized) {
		if (_jc_cond_init(env, &env->resumption) != JNI_OK) {
			_jc_post_exception_info(env);
			_jc_throw_exception(env);
		}
		env->resumption_initialized = JNI_TRUE;
	}

	/* Exit Java mode */
	_jc_stopping_java(env, NULL, "Suspended by Thread.suspend()");

	/* Lock VM */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Clip the C stack */
	_jc_stack_clip(env);

	/* Suspend while suspended */
	while (env->suspended)
		_JC_COND_WAIT(env, env->resumption, vm->mutex);

#ifndef NDEBUG
	/* Mark the C stack as not clipped */
	if (env->c_stack != NULL)
		env->c_stack->clipped = JNI_FALSE;
#endif

	/* Unock VM */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Return to Java mode */
	_jc_resuming_java(env, NULL);
}

/*
 * Interrupt another thread given its Thread instance.
 *
 * This method may be called by a thread running in non-Java mode.
 */
void
_jc_thread_interrupt_instance(_jc_jvm *vm, _jc_object *instance)
{
	_jc_env *thread;
	_jc_object *vmt;

	/* Sanity check */
	_JC_ASSERT(_jc_subclass_of(instance, vm->boot.types.Thread));

	/* Lock VM to keep thread from disappearing */
	_JC_MUTEX_LOCK(_jc_get_current_env(), vm->mutex);

	/* Get VMThread */
	vmt = *_JC_VMFIELD(vm, instance, Thread, vmThread, _jc_object *);
	if (vmt == NULL)
		goto done;

	/* Get internal thread structure */
	thread = _jc_get_vm_pointer(vm, vmt, vm->boot.fields.VMThread.vmdata);
	if (thread == NULL)
		goto done;

	/* Interrupt thread */
	_jc_thread_interrupt(vm, thread);

done:
	/* Unlock VM */
	_JC_MUTEX_UNLOCK(_jc_get_current_env(), vm->mutex);
}

/*
 * Interrupt another thread.
 *
 * This method may be called by a thread running in non-Java mode.
 *
 * NOTE: The caller must have acquire the VM mutex.
 */
void
_jc_thread_interrupt(_jc_jvm *vm, _jc_env *thread)
{
	/* Sanity check */
	_JC_MUTEX_ASSERT(_jc_get_current_env(), vm->mutex);

	/*
	 * Atomically set other thread's interrupted flag. If
	 * the thread is in interruptible sleep, wake it up.
	 */
	while (JNI_TRUE) {
		_jc_word old_status;

		switch ((old_status = thread->interrupt_status)) {
		case _JC_INTERRUPT_CLEAR:
			if (_jc_compare_and_swap(&thread->interrupt_status,
			    old_status, _JC_INTERRUPT_SET))
				return;
			break;
		case _JC_INTERRUPT_INTERRUPTIBLE:
		    {
			_jc_fat_lock *lock;

			/* Change state to interrupted */
			if (!_jc_compare_and_swap(
			    &thread->interrupt_status,
			    old_status, _JC_INTERRUPT_INTERRUPTED))
				break;

			/*
			 * Wake up the other thread. If the lock is NULL then
			 * thread has already woken up and left. Otherwise,
			 * we must grab the mutex to ensure the thread has
			 * started sleeping on the condition variable.
			 */
			if ((lock = thread->interruptible_lock) != NULL) {
				_JC_MUTEX_LOCK(_jc_get_current_env(),
				    lock->mutex);
				_JC_COND_BROADCAST(lock->notify);
				_JC_MUTEX_UNLOCK(_jc_get_current_env(),
				    lock->mutex);
			}
			return;
		    }
		case _JC_INTERRUPT_SET:
		case _JC_INTERRUPT_INTERRUPTED:
			return;
		default:
			_JC_ASSERT(JNI_FALSE);
		}
	}
}

/*
 * Check if another thread has requested that this thread halt.
 *
 * NOTE: The calling thread must ensure vm->mutex is acquired
 * and update env->text_status with the thread's status.
 */
void
_jc_halt_if_requested(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;

	/* Sanity check */
	_JC_MUTEX_ASSERT(env, vm->mutex);
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL
	    || env->status == _JC_THRDSTAT_HALTING_NORMAL);

	/* Stop while requested to */
	while (env->status == _JC_THRDSTAT_HALTING_NORMAL) {

		/* Update status */
		env->status = _JC_THRDSTAT_HALTED;

		/* The last thread to halt wakes up the requesting thread */
		_JC_ASSERT(vm->pending_halt_count > 0);
		if (--vm->pending_halt_count == 0)
			_JC_COND_SIGNAL(vm->all_halted);

		/* Clip the top of the C stack */
		_jc_stack_clip(env);

		/* Wait for the thread requesting the halt to finish */
		while (env->status == _JC_THRDSTAT_HALTED)
			_JC_COND_WAIT(env, vm->world_restarted, vm->mutex);

#ifndef NDEBUG
		/* Mark the C stack not clipped */
		if (env->c_stack != NULL)
			env->c_stack->clipped = JNI_FALSE;
#endif
	}

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL);
}

/*
 * Stop all other threads except this one.
 *
 * NOTE: The calling thread should acquire vm->mutex. Once the world
 * is stopped, the calling thread should release the lock until just
 * prior to the call to calling _jc_resume_the_world().
 */
void
_jc_stop_the_world(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_env *thread;

	/* Sanity check */
	_JC_MUTEX_ASSERT(env, vm->mutex);
	_JC_ASSERT(!vm->world_stopped);

	/* Update thread's debugging status */
	snprintf(env->text_status, sizeof(env->text_status),
	    "trying to stop the world");

	/* If another thread beat us to it, halt for it first */
	_jc_halt_if_requested(env);

	/* Sanity checks */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL);
	_JC_ASSERT(vm->pending_halt_count == 0);

	/*
	 * Put all other threads in 'halting' mode.
	 * They will halt at the next available opportunity.
	 */
	LIST_FOREACH(thread, &vm->threads.alive_list, link) {
		jboolean succeeded;

		/* Skip the current thread */
		if (thread == env)
			continue;

		/* Set thread's status to halting */
		for (succeeded = JNI_FALSE; !succeeded; ) {
			switch (thread->status) {
			case _JC_THRDSTAT_RUNNING_NORMAL:
				succeeded = _jc_compare_and_swap(
				    &thread->status,
				    _JC_THRDSTAT_RUNNING_NORMAL,
				    _JC_THRDSTAT_HALTING_NORMAL);
				if (succeeded)
					vm->pending_halt_count++;
				break;
			case _JC_THRDSTAT_RUNNING_NONJAVA:
				succeeded = _jc_compare_and_swap(
				    &thread->status,
				    _JC_THRDSTAT_RUNNING_NONJAVA,
				    _JC_THRDSTAT_HALTING_NONJAVA);
				break;
			default:
				_JC_ASSERT(JNI_FALSE);
				break;
			}
		}
	}

	/* Clip the C stack */
	_jc_stack_clip(env);

	/* Now wait for the threads running in Java mode to actually halt */
	while (vm->pending_halt_count > 0)
		_JC_COND_WAIT(env, vm->all_halted, vm->mutex);

#ifndef NDEBUG
	/* Mark the C stack not clipped */
	if (env->c_stack != NULL)
		env->c_stack->clipped = JNI_FALSE;
#endif

	/* Update flags */
	vm->world_stopped = JNI_TRUE;
}

/*
 * Wake up other threads previously halted by _jc_stop_the_world().
 *
 * NOTE: The calling thread should acquire vm->mutex before
 * calling this function. Of course, _jc_stop_the_world()
 * must have been called previously by this thread as well.
 */
void
_jc_resume_the_world(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_env *thread;

	/* Sanity check */
	_JC_MUTEX_ASSERT(env, vm->mutex);
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL);
	_JC_ASSERT(vm->pending_halt_count == 0);
	_JC_ASSERT(vm->world_stopped);

	/*
	 * Wake up all other threads previously halted by us.
	 */
	LIST_FOREACH(thread, &vm->threads.alive_list, link) {

		/* Skip the current thread */
		if (thread == env)
			continue;

		/* Mark thread as running */
		switch (thread->status) {
		case _JC_THRDSTAT_HALTED:
			thread->status = _JC_THRDSTAT_RUNNING_NORMAL;
			break;
		case _JC_THRDSTAT_HALTING_NONJAVA:
			thread->status = _JC_THRDSTAT_RUNNING_NONJAVA;
			break;
		default:
			_JC_ASSERT(JNI_FALSE);
			break;
		}
	}

	/* Wake all halted threads */
	_JC_COND_BROADCAST(vm->world_restarted);

	/* Update flag */
	vm->world_stopped = JNI_FALSE;
}

/*
 * Transition from Java code to non-Java code.
 * Halt if requested to do so by another thread.
 *
 * There are two cases:
 *
 * 1. We are about to invoke some JNI/native code (cstack == NULL)
 *
 *    Clip the current C call stack so no references in registers
 *    "leak" into the upcoming C stack frames, causing missed GC refs.
 *
 * 2. We are about to return to some JNI/native code (cstack != NULL)
 *
 *    _jc_resuming_java() was previously invoked with cstack.
 *    Pop the cstack frame that we previously pushed.
 */
void
_jc_stopping_java(_jc_env *env, _jc_c_stack *cstack, const char *fmt, ...)
{
	_jc_jvm *const vm = env->vm;

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL
	    || env->status == _JC_THRDSTAT_HALTING_NORMAL);
	_JC_ASSERT(env->c_stack == NULL || !env->c_stack->clipped);

	/* Update debug status */
	if (fmt == NULL) {
		snprintf(env->text_status, sizeof(env->text_status),
		    "running native code");
	} else {
		va_list args;

		va_start(args, fmt);
		vsnprintf(env->text_status, sizeof(env->text_status),
		    fmt, args);
		va_end(args);
	}

	/* Change this thread's status, but first halt if requested */
	if (!_jc_compare_and_swap(&env->status,
	    _JC_THRDSTAT_RUNNING_NORMAL, _JC_THRDSTAT_RUNNING_NONJAVA)) {

		/* Lock the VM */
		_JC_MUTEX_LOCK(env, vm->mutex);

		/* Halt if requested */
		_jc_halt_if_requested(env);

		/* Sanity check */
		_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL);

		/* Update status */
		env->status = _JC_THRDSTAT_RUNNING_NONJAVA;

		/* Unlock the VM */
		_JC_MUTEX_UNLOCK(env, vm->mutex);
	}

	/* Pop C stack chunk if desired, else clip current C stack */
	if (cstack != NULL) {
		_JC_ASSERT(cstack == env->c_stack);
		env->c_stack = cstack->next;
	} else
		_jc_stack_clip(env);
}

/*
 * Transition from non-Java code to Java code.
 * Halt if requested to do so by another thread.
 *
 * There are two cases:
 *
 * 1. We're about to invoke VM code from JNI/native code (cstack != NULL)
 *
 *    Start a new C stack chunk by pushing "cstack" onto the C call stack.
 *
 * 2. We're about to return from JNI/native to VM code (cstack == NULL)
 *
 *    Do nothing; the previous top of the C stack becomes "active" again.
 */
void
_jc_resuming_java(_jc_env *env, _jc_c_stack *cstack)
{
	_jc_jvm *const vm = env->vm;

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NONJAVA
	    || env->status == _JC_THRDSTAT_HALTING_NONJAVA);

	/* Change this thread's status, but first halt if requested */
	if (!_jc_compare_and_swap(&env->status,
	     _JC_THRDSTAT_RUNNING_NONJAVA, _JC_THRDSTAT_RUNNING_NORMAL)) {
		_JC_MUTEX_LOCK(env, vm->mutex);
		while (env->status == _JC_THRDSTAT_HALTING_NONJAVA)
			_JC_COND_WAIT(env, vm->world_restarted, vm->mutex);
		_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NONJAVA);
		env->status = _JC_THRDSTAT_RUNNING_NORMAL;
		_JC_MUTEX_UNLOCK(env, vm->mutex);
	}

	/* Start a new C stack chunk if desired */
	if (cstack != NULL) {
		_JC_ASSERT(env->c_stack == NULL || env->c_stack->clipped);
		cstack->next = env->c_stack;
		env->c_stack = cstack;
	} else {
		_JC_ASSERT(env->c_stack != NULL);
		_JC_ASSERT(env->c_stack->clipped);
	}

#ifndef NDEBUG
	/* Mark the C stack as not clipped */
	env->c_stack->clipped = JNI_FALSE;
#endif
}

/*
 * Create the java.lang.Thread instance associated with a native thread.
 */
jint
_jc_thread_create_instance(_jc_env *env, _jc_object *group,
	const char *name, jint priority, jboolean daemon)
{
	_jc_jvm *const vm = env->vm;
	jobject sref = NULL;
	jobject vtref = NULL;

	/* Sanity check */
	_JC_ASSERT(group != NULL);
	_JC_ASSERT(env->instance == NULL);

	/* Create String from supplied name, if any */
	if (name != NULL
	    && (sref = _jc_new_local_native_ref(env,
	      _jc_new_string(env, name, strlen(name)))) == NULL)
		goto fail;

	/* Create new Thread object for this thread */
	if ((env->instance = _jc_new_object(env,
	    vm->boot.types.Thread)) == NULL)
		goto fail;

	/* Create new VMThread object for this thread */
	if ((vtref = _jc_new_local_native_ref(env,
	    _jc_new_object(env, vm->boot.types.VMThread))) == NULL)
		goto fail;

	/* Set VMThread private data */
	if (_jc_set_vm_pointer(env, *vtref,
	    vm->boot.fields.VMThread.vmdata, env) != JNI_OK)
	    	goto fail;

	/* Invoke VMThread constructor */
	if (_jc_invoke_nonvirtual(env, vm->boot.methods.VMThread.init,
	    *vtref, env->instance) != JNI_OK) {
		_jc_print_stack_trace(env, stderr);
		goto fail;
	}

	/* Invoke the Thread() constructor used for native threads */
	if (_jc_invoke_nonvirtual(env, vm->boot.methods.Thread.init,
	    env->instance, *vtref, (sref != NULL) ? *sref : NULL,
	    priority, daemon) != JNI_OK) {
		_jc_print_stack_trace(env, stderr);
		goto fail;
	}

	/* Add thread to the group */
	*_JC_VMFIELD(vm, env->instance, Thread, group, _jc_object *) = group;
	if (_jc_invoke_virtual(env, vm->boot.methods.ThreadGroup.addThread,
	    group, env->instance) != JNI_OK)
		goto fail;

	/* XXX FIXME do InheritableThreadLocal */

	/* Done */
	_jc_free_local_native_ref(&sref);
	_jc_free_local_native_ref(&vtref);
	return JNI_OK;

fail:
	/* Clean up after failure */
	_jc_free_local_native_ref(&sref);
	_jc_free_local_native_ref(&vtref);
	env->instance = NULL;
	return JNI_ERR;
}

/*
 * Entry point for new java.lang.Thread threads.
 * 
 * This function marks the bottom of the call stack of threads created
 * via Thread.start(). Threads attached to the VM using the JNI invocation
 * API do not go through this function.
 */
void *
_jc_thread_start(void *arg)
{
	_jc_env *env = arg;
	_jc_jvm *const vm = env->vm;
	_jc_c_stack cstack;
	_jc_object *vmt;

	/* Sanity checks */
	_JC_ASSERT(env->instance != NULL);
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL
	    || env->status == _JC_THRDSTAT_HALTING_NORMAL);

	/* Set thread-local pointer to my thread structure */
	_jc_set_current_env(vm, env);

	/* Push first C stack chunk */
	memset(&cstack, 0, sizeof(cstack));
	env->c_stack = &cstack;

	/* Grab pointer to the VMThread */
	vmt = env->retval.l;
	_JC_ASSERT(_jc_subclass_of(vmt, vm->boot.types.VMThread));

	/* Invoke VMThread.run() */
	if (_jc_invoke_virtual(env,
	    vm->boot.methods.VMThread.run, vmt) != JNI_OK)
		(void)_jc_retrieve_exception(env, NULL);

	/* Detach thread */
	_JC_MUTEX_LOCK(env, vm->mutex);
	_jc_detach_thread(&env);
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Okaybye */
	return NULL;
}

/*
 * Create a thread structure and attach it to the currently running thread.
 * If provided, use cstack as the first C stack chunk.
 *
 * If unsuccessful an exception is stored in the supplied pointers.
 *
 * NOTE: This assumes the VM global mutex is held.
 */
_jc_env *
_jc_attach_thread(_jc_jvm *vm, _jc_ex_info *ex, _jc_c_stack *cstack)
{
	_jc_env temp_env;
	_jc_env *env;

	/* Initialize phoney current thread structure */
	memset(&temp_env, 0, sizeof(temp_env));
	temp_env.ex.num = -1;
	temp_env.vm = vm;

	/* Ensure this thread isn't already attached to a VM */
	if (_jc_get_current_env() != NULL) {
		_JC_EX_STORE(&temp_env, InternalError,
		    "current thread is already attached");
		goto fail;
	}

	/* Sanity check */
	_JC_MUTEX_ASSERT(NULL, vm->mutex);
#ifndef NDEBUG
	vm->mutex_owner = &temp_env;
#endif

	/* Get a new thread structure */
	if ((env = _jc_allocate_thread(&temp_env)) == NULL)
		goto fail;

	/* Remember that this thread structure goes with the current thread */
	_jc_set_current_env(vm, env);

#ifndef NDEBUG
	/* Update mutex owner */
	vm->mutex_owner = env;
#endif

	/* If the world is stopping or stopped, we wait for it to restart */
	if (vm->world_stopped || vm->pending_halt_count > 0) {

		/* Mark this thread as halted */
		env->status = _JC_THRDSTAT_HALTED;
		snprintf(env->text_status, sizeof(env->text_status),
		    "newly attached thread");

		/* Wait for other thread to wake me back up */
		while (env->status == _JC_THRDSTAT_HALTED)
			_JC_COND_WAIT(env, vm->world_restarted, vm->mutex);
	}

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL);
	_JC_ASSERT(env->c_stack == NULL);

	/* Push first C stack chunk */
	_JC_ASSERT(cstack != NULL);
	memset(cstack, 0, sizeof(*cstack));
	env->c_stack = cstack;

	/* Done */
	return env;

fail:
	/* Failed, copy stored exception */
	if (ex != NULL)
		*ex = temp_env.ex;
#ifndef NDEBUG
	vm->mutex_owner = NULL;
#endif
	return NULL;
}

/*
 * Detach a thread structure from the currently running thread and free it.
 * It must be attached and correspond to the currently running thread.
 *
 * NOTE: This assumes the VM global mutex is held.
 */
void
_jc_detach_thread(_jc_env **envp)
{
	_jc_env *env = *envp;
	_jc_jvm *const vm = env->vm;

	/* Sanity check */
	if (env == NULL)
		return;
	*envp = NULL;

	/* Sanity check */
	_JC_MUTEX_ASSERT(_jc_get_current_env(), vm->mutex);
	_JC_ASSERT(env == vm->threads.by_id[env->thread_id]);
	_JC_ASSERT(env == _jc_get_current_env());
	_JC_ASSERT(env->c_stack != NULL);

	/* Update thread's debugging status */
	snprintf(env->text_status, sizeof(env->text_status), "detaching");

	/*
	 * Check one last time (while mutex held) for halt. This is necessary
	 * because of the use of 'pending_halt_count'. If we didn't check
	 * here, we might leave without being counted as having halted.
	 */
	_jc_halt_if_requested(env);

	/* Pop off the last remaining C stack chunk */
	env->c_stack = env->c_stack->next;
	_JC_ASSERT(env->c_stack == NULL);

	/* Invalidate current thread's reference to thread structure */
	_jc_set_current_env(vm, NULL);

#ifndef NDEBUG
	/* Update mutex owner */
	vm->mutex_owner = NULL;
#endif

	/* Free the thread structure */
	_jc_free_thread(&env, JNI_TRUE);
}

/*
 * Kill all other threads in the VM. This is used during VM shutdown.
 */
void
_jc_thread_shutdown(_jc_env **envp)
{
	_jc_env *env = *envp;
	int last_report = -1;
	_jc_c_stack cstack;
	_jc_env *thread;
	_jc_jvm *vm;

	/* Sanity check */
	if (env == NULL)
		return;
	*envp = NULL;
	vm = env->vm;

	/* Grab VM global mutex */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Detach this thread */
	_jc_detach_thread(&env);

	/* Wait for all non-daemon threads to exit */
	while (JNI_TRUE) {
		int user_thread_count;

		/* Search for remaining non-daemon threads */
		user_thread_count = 0;
		LIST_FOREACH(thread, &vm->threads.alive_list, link) {

			/* Check whether it's a daemon thread */
			_JC_ASSERT(thread->instance != NULL);
			if (!*_JC_VMFIELD(vm, thread->instance,
			    Thread, daemon, jboolean))
				user_thread_count++;
		}

		/* No more user threads? Then stop waiting */
		if (user_thread_count == 0)
			break;

		/* Log progress */
		if (user_thread_count != last_report) {
			VERBOSE(JNI, vm, "JNI_DestroyJavaVM: waiting for %d"
			    " user threads to exit", user_thread_count);
			last_report = user_thread_count;
		}

		/* Wait for the next thread to exit */
		_JC_COND_WAIT(env, vm->vm_destruction, vm->mutex);
	}

	/* Re-attach this thread */
	if ((env = _jc_attach_thread(vm, NULL, &cstack)) == NULL)
		_jc_fatal_error(vm, "can't reattach shutdown thread");

	/* Stop the world */
	_jc_stop_the_world(env);

	/* Disable uncaught exception handling */
	vm->world_ending = JNI_TRUE;

	/* Post a ThreadDeath in all remaining threads */
	LIST_FOREACH(thread, &vm->threads.alive_list, link) {
		_jc_object *const tdeath
		    = vm->boot.objects.vmex[_JC_ThreadDeath];

		if (thread == env)
			continue;
		_JC_ASSERT(tdeath != NULL);
		thread->cross_exception = tdeath;
		_jc_thread_interrupt(vm, thread);
	}

	/* Resume the world */
	_jc_resume_the_world(env);

	/* Detach this thread */
	_jc_detach_thread(&env);

	/* Wait for all other threads to die */
	while (!LIST_EMPTY(&vm->threads.alive_list))
		_JC_COND_WAIT(env, vm->vm_destruction, vm->mutex);

	/* Unlock VM global mutex */
	_JC_MUTEX_UNLOCK(env, vm->mutex);
}

/*
 * Allocate a new thread structure. The 'env' pointer may be NULL
 * if the current thread is not attached to any VM.
 *
 * This assumes the VM global mutex is held.
 *
 * If unsuccessful an exception is stored.
 */
_jc_env *
_jc_allocate_thread(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_env *new_env;

	/* Sanity check */
	_JC_MUTEX_ASSERT(env, vm->mutex);

	/* Free thread stacks */
	_jc_free_thread_stacks(vm);

	/* Check for too many threads */
	if (vm->threads.next_free_id == 0) {
		_JC_EX_STORE(env, InternalError,
		    "max number of threads (%d) exceeded", _JC_MAX_THREADS - 1);
		return NULL;
	}

	/* Reuse a free thread structure off the free list, if any */
	if ((new_env = LIST_FIRST(&vm->threads.free_list)) != NULL) {
		_JC_ASSERT(vm->threads.num_free > 0);
		LIST_REMOVE(new_env, link);
		vm->threads.num_free--;
		goto set_id;
	}

	/* Allocate and initialize new '_jc_env' struct for this thread */
	if ((new_env = _jc_vm_zalloc(env, sizeof(*new_env))) == NULL)
		return NULL;
	new_env->vm = vm;
	new_env->jni_interface = &_jc_native_interface;
	new_env->status = _JC_THRDSTAT_RUNNING_NORMAL;
	new_env->ex.num = -1;

	/* Create the first local native reference frame */
	SLIST_INIT(&new_env->native_locals);
	if (_jc_add_native_frame(new_env, &new_env->native_locals) == NULL) {
		_jc_vm_free(&new_env);
		return NULL;
	}

	/* Initialize contention state for this thread */
	if (_jc_mutex_init(env, &new_env->lock.owner.mutex) != JNI_OK) {
		_jc_free_all_native_local_refs(new_env);
		_jc_vm_free(&new_env);
		return NULL;
	}
	if (_jc_cond_init(env, &new_env->lock.waiter.cond) != JNI_OK) {
		_jc_mutex_destroy(&new_env->lock.owner.mutex);
		_jc_free_all_native_local_refs(new_env);
		_jc_vm_free(&new_env);
		return NULL;
	}
	SLIST_INIT(&new_env->lock.owner.waiters);

set_id:
	/* Sanity check */
	_JC_ASSERT(new_env->thread_id == 0);
	_JC_ASSERT(!new_env->suspended);
	_JC_ASSERT(!new_env->resumption_initialized);
	_JC_ASSERT(vm->threads.next_free_id > 0
	    && vm->threads.next_free_id < _JC_MAX_THREADS);
	_JC_ASSERT(new_env->sp == NULL);
	_JC_ASSERT(new_env->stack_data == NULL);
	_JC_ASSERT(new_env->stack_data_end == NULL);

	/* Assign next free thread ID to the thread */
	new_env->thread_id = vm->threads.next_free_id;
	vm->threads.next_free_id = (jint)vm->threads.by_id[new_env->thread_id];
	vm->threads.by_id[new_env->thread_id] = new_env;

	/* Initialize thread's thinlock ID */
	new_env->thinlock_id = new_env->thread_id << _JC_LW_THIN_TID_SHIFT;

	/* Add this thread to the VM 'alive' thread list */
	LIST_INSERT_HEAD(&vm->threads.alive_list, new_env, link);

	/* Done */
	return new_env;
}

/*
 * Release a thread structure.
 *
 * This does not free the thread's stack, because we might be running
 * on that stack.
 *
 * NOTE: This assumes the VM global mutex is held.
 */
void
_jc_free_thread(_jc_env **envp, int cachable)
{
	_jc_env *env = *envp;
	_jc_jvm *const vm = env->vm;

	/* Invalidate caller's pointer */
	if (env == NULL)
		return;
	*envp = NULL;

	/* Sanity check */
	_JC_MUTEX_ASSERT(_jc_get_current_env(), vm->mutex);
	_JC_ASSERT(env->thread_id > 0 && env->thread_id < _JC_MAX_THREADS);
	_JC_ASSERT(vm->threads.by_id[env->thread_id] == env);

	/* Invalidate the reference from the VMThread object, if any */
	if (env->instance != NULL) {
		_jc_object *vmt;

		vmt = *_JC_VMFIELD(vm, env->instance,
		    Thread, vmThread, _jc_object *);
		if (vmt != NULL) {
			(void)_jc_set_vm_pointer(env, vmt,
			   vm->boot.fields.VMThread.vmdata, NULL);
		}
	}

	/* Remove this thread from the alive thread list */
	LIST_REMOVE(env, link);

	/* Free native local refs */
	_jc_free_all_native_local_refs(env);

	/* Push threads' ID back on the thread ID free list */
	vm->threads.by_id[env->thread_id] = (_jc_env *)vm->threads.next_free_id;
	vm->threads.next_free_id = env->thread_id;
	env->thread_id = 0;

	/* Free suspend/notify condition variable, if any */
	if (env->resumption_initialized) {
		_jc_cond_destroy(&env->resumption);
		env->resumption_initialized = JNI_FALSE;
	}

	/* Notify any threads waiting for this thread to die */
	_JC_COND_SIGNAL(vm->vm_destruction);

	/* Put thread structure on the free list if possible or required */
	if ((cachable && vm->threads.num_free < _JC_MAX_FREE_THREADS)
	    || env->stack != NULL) {
		_jc_free_thread_stacks(vm);
		LIST_INSERT_HEAD(&vm->threads.free_list, env, link);
		vm->threads.num_free++;
		return;
	}

	/* Destroy thread structure */
	_jc_destroy_thread(&env);
}

/*
 * Free C stacks associated with free'd threads.
 *
 * We maintain the invariant that free'd threads whose stacks still
 * need to be free'd are all at the front of the thread free list.
 *
 * NOTE: This assumes the VM global mutex is held.
 */
void
_jc_free_thread_stacks(_jc_jvm *vm)
{
	_jc_env *env;

	/* Free thread's C stack */
	LIST_FOREACH(env, &vm->threads.free_list, link) {
		if (env->stack == NULL)
			return;
		munmap(env->stack, env->stack_size);
		env->stack_size = 0;
		env->stack = NULL;
	}
}

/*
 * Destroy a thread structure.
 */
void
_jc_destroy_thread(_jc_env **envp)
{
	_jc_env *env = *envp;

	/* Invalidate caller's pointer */
	if (env == NULL)
		return;
	*envp = NULL;

	/* C stack must be freed already */
	_JC_ASSERT(env->stack == NULL);

	/* Java stack must be freed already */
	_JC_ASSERT(env->sp == NULL);
	_JC_ASSERT(env->stack_data == NULL);
	_JC_ASSERT(env->stack_data_end == NULL);

	/* Destroy thread structure */
	_jc_cond_destroy(&env->lock.waiter.cond);
	_jc_mutex_destroy(&env->lock.owner.mutex);
	_jc_vm_free(&env);
}

/*
 * Fire up an internal VM daemon thread.
 */
jobject
_jc_internal_thread(_jc_env *env, const char *class)
{
	_jc_jvm *const vm = env->vm;
	_jc_method *constructor;
	_jc_method *start;
	jobject ref = NULL;
	_jc_type *type;

	/* Get the class' type */
	if ((type = _jc_load_type(env, vm->boot.loader, class)) == NULL)
		goto fail;

	/* Find no-arg constructor */
	if ((constructor = _jc_get_declared_method(env, type,
	    "<init>", "()V", _JC_ACC_STATIC, 0)) == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}

	/* Instantiate one and wrap in a global native reference */
	if ((ref = _jc_new_global_native_ref(env,
	    _jc_new_object(env, type))) == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}

	/* Sanity check */
	_JC_ASSERT(_jc_subclass_of(*ref, vm->boot.types.Thread));

	/* Invoke constructor */
	if (_jc_invoke_nonvirtual(env, constructor, *ref) != JNI_OK)
		goto fail;

	/* Make it a daemon thread */
	*_JC_VMFIELD(vm, *ref, Thread, daemon, jboolean) = JNI_TRUE;

	/* Find Thread.start() */
	if ((start = _jc_get_declared_method(env, vm->boot.types.Thread,
	    "start", "()V", _JC_ACC_STATIC, 0)) == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}

	/* Invoke Thread.start() */
	if (_jc_invoke_virtual(env, start, *ref) != JNI_OK)
		goto fail;

	/* Done */
	return ref;

fail:
	/* Clean up after failure */
	_jc_free_global_native_ref(&ref);
	return NULL;
}

