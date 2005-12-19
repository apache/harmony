
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
 * Enter an object monitor.
 *
 * It is assumed that 'object' has already been verified != null
 * and that the caller ensures a reference to 'obj' will be
 * maintained somehow (i.e., either as a parameter on the Java
 * stack or via an explicit native reference).
 */
jint
_jc_lock_object(_jc_env *env, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	_jc_word old_lockword;
	_jc_fat_lock *lock;
	jint status;

	/* Sanity check */
	_JC_ASSERT(obj != NULL);

retry:
	/* Get current lockword */
	old_lockword = obj->lockword;

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL
	    || env->status == _JC_THRDSTAT_HALTING_NORMAL);

	/* Try to acquire the thinlock by glomming in our thinlock ID */
	if (_jc_compare_and_swap(&obj->lockword,
	    old_lockword & _JC_LW_INFO_MASK,
	    env->thinlock_id | (old_lockword & _JC_LW_INFO_MASK)))
		return JNI_OK;			/* thinlock acquired! */

	/*
	 * Either the object is already locked, or the lock is inflated.
	 * First, handle the case of an already-locked thinlock.
	 */
	if (!_JC_LW_TEST(old_lockword, FAT)) {
		_jc_word old_flag;
		jboolean notified;
		_jc_env *owner;

		/*
		 * Check if we are already the owner of the thinlock.
		 * If so, just increment the recursion count.
		 */
		if ((old_lockword & _JC_LW_THIN_TID_MASK) == env->thinlock_id) {
			jint count = _JC_LW_EXTRACT(old_lockword, THIN_COUNT);

			/* Sanity check */
			_JC_ASSERT(count < _JC_MAX_THIN_RECURSION);

			/*
			 * Increment the recursion counter. If the counter
			 * doesn't overflow, we're done. Otherwise, we have
			 * to inflate the lock.
			 */
			if (++count < _JC_MAX_THIN_RECURSION) {
				obj->lockword = env->thinlock_id
				    | (count << _JC_LW_THIN_COUNT_SHIFT)
				    | (old_lockword & _JC_LW_INFO_MASK);
				return JNI_OK;
			}

			/*
			 * Overflow: we have to inflate the thinlock.
			 */
			if ((status = _jc_inflate_lock(env, obj)) != JNI_OK)
				return status;

			/* OK, now retry locking with the new fat lock */
			goto retry;
		}

		/*
		 * Contention: the thinlock is owned by another thread.
		 */
		owner = vm->threads.by_id[
		    _JC_LW_EXTRACT(old_lockword, THIN_TID)];
		_JC_ASSERT(owner->jni_interface == &_jc_native_interface);

		/* Acquire mutex associated with the owner's waiter queue */
		_JC_MUTEX_LOCK(env, owner->lock.owner.mutex);

		/* Update the contention flag */
		old_flag = owner->lock.owner.contention;
		owner->lock.owner.contention = 1;

		/*
		 * If the thinlock owner has not changed, then this thread
		 * has just notified it that there is contention. Add this
		 * thread to the waiting list. Otherwise, the owner has
		 * changed, so restore the contention flag to its original
		 * value.
		 */
		if ((obj->lockword & _JC_LW_THIN_TID_MASK)
		    == owner->thinlock_id) {
			SLIST_INSERT_HEAD(&owner->lock.owner.waiters,
			    env, lock.waiter.link);
			env->lock.waiter.object = obj;
			notified = JNI_TRUE;
		} else {
			owner->lock.owner.contention = old_flag;
			notified = JNI_FALSE;
		}

		/* Release mutex associated with the owner's waiter queue */
		_JC_MUTEX_UNLOCK(env, owner->lock.owner.mutex);

		/* If the owner changed, retry from scratch */
		if (!notified)
			goto retry;

		/* Enter non-Java mode */
		_jc_stopping_java(env, NULL, "waiting for thinlock on %s@%p",
		    obj->type->name, obj);

		/* Transition made; reacquire lock */
		_JC_MUTEX_LOCK(env, owner->lock.owner.mutex);

		/* Sleep as long as we're on the waiting list */
		while (JNI_TRUE) {
			_jc_env *thread;

			/* Look for this thread in the waiters list */
			SLIST_FOREACH(thread, &owner->lock.owner.waiters,
			    lock.waiter.link) {
				if (thread == env)
					break;
			}
			if (thread == NULL)
				break;		/* no longer in list! */

			/* Sanity check */
			_JC_ASSERT(owner->lock.owner.contention == 1);

			/* Sleep some more */
			_JC_COND_WAIT(env, env->lock.waiter.cond,
			    owner->lock.owner.mutex);
		}

		/* Release mutex associated with the fat lock */
		_JC_MUTEX_UNLOCK(env, owner->lock.owner.mutex);

		/* Back to running normal Java */
		_jc_resuming_java(env, NULL);

		/* Retry locking */
		env->lock.waiter.object = NULL;
		goto retry;
	}

	/* The object lock is already inflated, i.e., it's a fat lock */
	lock = vm->fat_locks.by_id[_JC_LW_EXTRACT(old_lockword, FAT_ID)];
	_JC_ASSERT(lock != NULL);

	/* Enter non-Java mode */
	_jc_stopping_java(env, NULL, "waiting for fatlock on %s@%p",
	    obj->type->name, obj);

	/* Acquire mutex associated with the fat lock */
	_JC_MUTEX_LOCK(env, lock->mutex);

	/* Wait until no other thread owns the lock */
	while (lock->recursion_count != 0 && lock->u.owner != env)
		_JC_COND_WAIT(env, lock->cond, lock->mutex);

	/* Bump recursion count */
	status = JNI_OK;
	if (lock->recursion_count == 0) {
		lock->recursion_count = 1;
		lock->u.owner = env;
	} else {
      
		/* Sanity check */
		_JC_ASSERT(lock->u.owner == env);

		/* Bump counter but check for overflow */
		if (++lock->recursion_count < 0) {
		      lock->recursion_count--;
		      status = JNI_ERR;
		}
	}

	/* Release the mutex associated with the fat lock */
	_JC_MUTEX_UNLOCK(env, lock->mutex);

	/* Return to normal java */
	_jc_resuming_java(env, NULL);

	/* Throw an error if recursion count overflowed */
	if (status != JNI_OK) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "max locking recursion (%u) exceeded",
		    lock->recursion_count);
	}

	/* Done */
	return status;
}

/*
 * Exit an object monitor.
 *
 * NOTE: It is assumed that 'object' has already been verified != null.
 */
jint
_jc_unlock_object(_jc_env *env, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	_jc_word old_lockword;
	_jc_fat_lock *lock;
	int released;

	/* Sanity check */
	_JC_ASSERT(obj != NULL);

	/* Save current lockword */
	old_lockword = obj->lockword;

	/*
	 * Handle the case where the lock is a thinlock
	 */
	if (!_JC_LW_TEST(old_lockword, FAT)) {

		/* Confirm that this thread actually owns the lock */
		if ((old_lockword & _JC_LW_THIN_TID_MASK) != env->thinlock_id) {
			_jc_post_exception(env,
			    _JC_IllegalMonitorStateException);
			return JNI_ERR;
		}

		/* If recursion count is zero, we're releasing the lock */
		if (_JC_LW_EXTRACT(old_lockword, THIN_COUNT) == 0) {
			obj->lockword = old_lockword & _JC_LW_INFO_MASK;
			goto handle_contention;
		}

		/* Just decrement the recursion count */
		obj->lockword = old_lockword - (1 << _JC_LW_THIN_COUNT_SHIFT);

		/* Done */
		return JNI_OK;
	}

	/*
	 * Handle the case where the lock is a fat lock.
	 */
	lock = vm->fat_locks.by_id[_JC_LW_EXTRACT(old_lockword, FAT_ID)];
	_JC_ASSERT(lock != NULL);

	/* Acquire mutex associated with the fat lock */
	_JC_MUTEX_LOCK(env, lock->mutex);

	/* Sanity check */
	_JC_ASSERT(lock->recursion_count >= 0);

	/* Confirm that this thread actually owns the lock */
	if (lock->recursion_count == 0 || lock->u.owner != env)
		released = -1;
	else if ((released = (--lock->recursion_count == 0))) {
		_JC_COND_BROADCAST(lock->cond);
		lock->u.owner = NULL;
	}

	/* Release the mutex associated with the fat lock */
	_JC_MUTEX_UNLOCK(env, lock->mutex);

	/* Check for non-acquired lock */
	if (released == -1) {
		_jc_post_exception(env, _JC_IllegalMonitorStateException);
		return JNI_ERR;
	}

	/* If we didn't actually release the lock, we're done */
	if (!released)
		return JNI_OK;

handle_contention:
	/*
	 * Handle the case where we are actually releasing the lock,
	 * not just decrementing the number of times we've re-locked it.
	 */

	/* If there is nobody else waiting for the lock, we're done */
	if (!env->lock.owner.contention)
		return JNI_OK;

	/* Handle contention */
	_jc_lock_contention(env, obj);

	/* Done */
	return JNI_OK;
}

/*
 * Handle contention on a lock.
 *
 * The current thread must own the lock.
 */
void
_jc_lock_contention(_jc_env *env, _jc_object *obj)
{
	_jc_env *thread;

	/* Acquire mutex associated with this thread's waiter queue */
	_JC_MUTEX_LOCK(env, env->lock.owner.mutex);

	/* Iterate through waiters list and wake them up */
	SLIST_FOREACH(thread, &env->lock.owner.waiters, lock.waiter.link) {
		_jc_object *const wobj = thread->lock.waiter.object;

		/*
		 * If the thread is waiting on another object that
		 * we have locked, then try to inflate that lock.
		 */
		if (wobj != obj && !_JC_LW_TEST(wobj->lockword, FAT)) {

			/* Sanity check that we actually own that lock too */
			_JC_ASSERT((wobj->lockword & _JC_LW_THIN_TID_MASK)
			    == env->thinlock_id);

			/*
			 * Try to inflate the other lock, but if we fail,
			 * don't worry. The other thread will inflate the
			 * lock when it wakes up.
			 */
			if (_jc_inflate_lock(env, wobj) != JNI_OK)
				_jc_retrieve_exception(env, NULL);
		}

		/* Wake up the other thread */
		_JC_COND_SIGNAL(thread->lock.waiter.cond);
	}

	/* Empty the wait list */
	SLIST_INIT(&env->lock.owner.waiters);

	/* No more contention */
	env->lock.owner.contention = 0;

	/* Release mutex associated with this thread's lock owner info */
	_JC_MUTEX_UNLOCK(env, env->lock.owner.mutex);
}

/*
 * Inflate a thin lock, turning it into a fat lock.
 */
jint
_jc_inflate_lock(_jc_env *env, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	jboolean tried_gc = JNI_FALSE;
	_jc_fat_lock *lock;

try_again:
	/* Acquire global mutex */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Get a fat lock off the free list if possible */
	if ((lock = SLIST_FIRST(&vm->fat_locks.free_list)) != NULL) {
		SLIST_REMOVE_HEAD(&vm->fat_locks.free_list, u.link);
		goto got_lock;
	}

	/* Can we create any more fat locks? */
	if (vm->fat_locks.next_id >= _JC_MAX_FATLOCKS) {

		/* All id's are allocated: try a GC cycle to free up some */
		if (!tried_gc) {
			_JC_MUTEX_UNLOCK(env, vm->mutex);
			if (_jc_gc(env, JNI_TRUE) != JNI_OK) {
				_jc_post_exception_info(env);
				return JNI_ERR;
			}
			tried_gc = JNI_TRUE;
			goto try_again;
		}

		/* All id's allocated and no free locks: throw an error */
		_jc_post_exception_msg(env, _JC_InternalError,
		    "max number of fat locks (%d) exceeded", _JC_MAX_FATLOCKS);
		goto fail;
	}

	/* Create a new fat lock */
	if ((lock = _jc_vm_zalloc(env, sizeof(*lock))) == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}
	lock->id = vm->fat_locks.next_id;

	/* Initialize fat lock synchronization objects */
	if (_jc_mutex_init(env, &lock->mutex) != JNI_OK) {
		_jc_post_exception_info(env);
		goto fail;
	}
	if (_jc_cond_init(env, &lock->cond) != JNI_OK) {
		_jc_post_exception_info(env);
		_jc_mutex_destroy(&lock->mutex);
		goto fail;
	}
	if (_jc_cond_init(env, &lock->notify) != JNI_OK) {
		_jc_post_exception_info(env);
		_jc_cond_destroy(&lock->cond);
		_jc_mutex_destroy(&lock->mutex);
		goto fail;
	}

	/* Map in this lock's ID */
	vm->fat_locks.by_id[lock->id] = lock;
	vm->fat_locks.next_id++;

got_lock:
	/* Release global mutex */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Sanity check */
	_JC_ASSERT(lock->recursion_count == 0);

	/* Acquire the mutex associated with the fat lock */
	_JC_MUTEX_LOCK(env, lock->mutex);

	/* Mark this thread as the lock's owner */
	lock->u.owner = env;

	/* Get recursion count, adjusted by one for thin -> fat transition */
	lock->recursion_count = _JC_LW_EXTRACT(obj->lockword, THIN_COUNT) + 1;

	/* Convert object lockword to reflect fat lock status */
	obj->lockword = _JC_LW_FAT_BIT
	    | (lock->id << _JC_LW_FAT_ID_SHIFT)
	    | (obj->lockword & _JC_LW_INFO_MASK);

	/* Release the fat lock mutex */
	_JC_MUTEX_UNLOCK(env, lock->mutex);

	/* Done */
	return JNI_OK;

fail:
	/* Clean up after failing to create a new fat lock */
	_jc_vm_free(&lock);
	_JC_MUTEX_UNLOCK(env, vm->mutex);
	return JNI_ERR;
}

/*
 * Put a fat lock back on the free list.
 *
 * NOTE: This should only be called when the world is stopped.
 */
void
_jc_free_lock(_jc_jvm *vm, _jc_object *obj)
{
	const _jc_word lockword = obj->lockword;
	_jc_fat_lock *lock;

	/* Sanity check */
	_JC_ASSERT(_JC_LW_TEST(lockword, FAT));
	_JC_ASSERT(vm->world_stopped);

	/* Get the lock encoded by the lockword */
	lock = vm->fat_locks.by_id[_JC_LW_EXTRACT(lockword, FAT_ID)];
	_JC_ASSERT(lock != NULL);
	_JC_ASSERT(lock->recursion_count == 0);

	/* Return it to the free list */
	SLIST_INSERT_HEAD(&vm->fat_locks.free_list, lock, u.link);
}

/*
 * Destroy a fat lock.
 *
 * This is only done during VM shutdown; during normal operation unused
 * fat locks are always returned to the free list.
 */
void
_jc_destroy_lock(_jc_fat_lock **lockp)
{
	_jc_fat_lock *lock = *lockp;

	/* Sanity check */
	if (lock == NULL)
		return;
	*lockp = NULL;
	_JC_ASSERT(lock->recursion_count == 0);

	/* Destroy lock */
	_jc_cond_destroy(&lock->cond);
	_jc_cond_destroy(&lock->notify);
	_jc_mutex_destroy(&lock->mutex);
	_jc_vm_free(&lock);
}

/*
 * Check whether the current thread holds the object's lock.
 */
jboolean
_jc_lock_held(_jc_env *env, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	_jc_word lockword;
	_jc_fat_lock *lock;

	/* Get object lockword */
	lockword = obj->lockword;

	/* Do the thin lock case */
	if (!_JC_LW_TEST(lockword, FAT))
		return (lockword & _JC_LW_THIN_TID_MASK) == env->thinlock_id;

	/* It's a fat lock */
	lock = vm->fat_locks.by_id[_JC_LW_EXTRACT(lockword, FAT_ID)];
	_JC_ASSERT(lock != NULL);
	return lock->recursion_count > 0 && lock->u.owner == env;
}

