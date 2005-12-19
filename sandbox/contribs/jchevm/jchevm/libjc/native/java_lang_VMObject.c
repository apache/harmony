
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
#include "java_lang_VMObject.h"

/* Internal functions */
static void	do_notify(_jc_env *env, _jc_object *this, jboolean broadcast);

/*
 * static final native Object clone(Cloneable)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMObject_clone(_jc_env *env, _jc_object *this)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *clone;
	_jc_type *type;

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Check for Cloneable */
	switch (_jc_instance_of(env, this, vm->boot.types.Cloneable)) {
	case 1:
		break;
	case 0:
		_jc_post_exception(env, _JC_CloneNotSupportedException);
		/* FALLTHROUGH */
	case -1:
		_jc_throw_exception(env);
	default:
		_JC_ASSERT(JNI_FALSE);
	}

	/* Clone object */
	type = this->type;
	if (_JC_FLG_TEST(type, ARRAY)) {
		_jc_type *const etype = type->u.array.element_type;
		_jc_array *const array = (_jc_array *)this;
		_jc_array *acopy;

		/* Create a new array having the same length */
		if ((acopy = _jc_new_array(env, type, array->length)) == NULL)
			_jc_throw_exception(env);

		/* Copy the array elements */
		switch ((etype->flags & _JC_TYPE_MASK)) {
		case _JC_TYPE_BOOLEAN:
		    {
			_jc_boolean_array *const src
			    = (_jc_boolean_array *)array;
			_jc_boolean_array *const dst
			    = (_jc_boolean_array *)acopy;

			memcpy(dst->elems, src->elems,
			    src->length * sizeof(*src->elems));
			break;
		    }
		case _JC_TYPE_BYTE:
		    {
			_jc_byte_array *const src = (_jc_byte_array *)array;
			_jc_byte_array *const dst = (_jc_byte_array *)acopy;

			memcpy(dst->elems, src->elems,
			    src->length * sizeof(*src->elems));
			break;
		    }
		case _JC_TYPE_CHAR:
		    {
			_jc_char_array *const src = (_jc_char_array *)array;
			_jc_char_array *const dst = (_jc_char_array *)acopy;

			memcpy(dst->elems, src->elems,
			    src->length * sizeof(*src->elems));
			break;
		    }
		case _JC_TYPE_SHORT:
		    {
			_jc_short_array *const src = (_jc_short_array *)array;
			_jc_short_array *const dst = (_jc_short_array *)acopy;

			memcpy(dst->elems, src->elems,
			    src->length * sizeof(*src->elems));
			break;
		    }
		case _JC_TYPE_INT:
		    {
			_jc_int_array *const src = (_jc_int_array *)array;
			_jc_int_array *const dst = (_jc_int_array *)acopy;

			memcpy(dst->elems, src->elems,
			    src->length * sizeof(*src->elems));
			break;
		    }
		case _JC_TYPE_LONG:
		    {
			_jc_long_array *const src = (_jc_long_array *)array;
			_jc_long_array *const dst = (_jc_long_array *)acopy;

			memcpy(dst->elems, src->elems,
			    src->length * sizeof(*src->elems));
			break;
		    }
		case _JC_TYPE_FLOAT:
		    {
			_jc_float_array *const src = (_jc_float_array *)array;
			_jc_float_array *const dst = (_jc_float_array *)acopy;

			memcpy(dst->elems, src->elems,
			    src->length * sizeof(*src->elems));
			break;
		    }
		case _JC_TYPE_DOUBLE:
		    {
			_jc_double_array *const src = (_jc_double_array *)array;
			_jc_double_array *const dst = (_jc_double_array *)acopy;

			memcpy(dst->elems, src->elems,
			    src->length * sizeof(*src->elems));
			break;
		    }
		case _JC_TYPE_REFERENCE:
		    {
			_jc_object_array *const src = (_jc_object_array *)array;
			_jc_object_array *const dst = (_jc_object_array *)acopy;

			memcpy(dst->elems - src->length,
			    src->elems - dst->length,
			    src->length * sizeof(*src->elems));
			break;
		    }
		default:
			_JC_ASSERT(JNI_FALSE);
		}

		/* Now we have our clone */
		clone = (_jc_object *)acopy;
	} else {
		const int instance_offset
		    = type->u.nonarray.num_virtual_refs * sizeof(void *);

		/* Create a new object having the same type */
		if ((clone = _jc_new_object(env, type)) == NULL)
			_jc_throw_exception(env);

		/* Copy type pointer and all reference and primitive fields */
		memcpy((char *)clone - instance_offset,
		    (char *)this - instance_offset,
		    type->u.nonarray.instance_size);

		/* Initialize lockword in the new object */
		clone->lockword = type->initial_lockword;
	}

	/* Done */
	return clone;
}

/*
 * static final native Class getClass(Object)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMObject_getClass(_jc_env *env, _jc_object *obj)
{
	/* Check for null */
	if (obj == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Return object type's Class instance */
	return obj->type->instance;
}

/*
 * static final native void wait(Object, long, int)
 *	throws java/lang/IllegalMonitorStateException,
 *		java/lang/InterruptedException
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMObject_wait(_jc_env *env, _jc_object *this,
	jlong millis, jint nanos)
{
	_jc_jvm *const vm = env->vm;
	const jboolean timed_wait = (millis != 0 || nanos != 0);
	jboolean interrupted;
	struct timespec wakeup;
	jint recursion_count;
	_jc_word old_status;
	_jc_fat_lock *lock;
	_jc_word lockword;
	jchar notify_count;
	_jc_object *ex;

	/* Check for invalid parameters */
	if (millis < 0 || nanos < 0 || nanos >= 1000000) {
		_jc_post_exception(env, _JC_IllegalArgumentException);
		_jc_throw_exception(env);
	}

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}
	lockword = this->lockword;

	/* Compute our wakeup time (if any) */
	if (timed_wait) {
		struct timeval now;

		/* Get current time */
		gettimeofday(&now, NULL);

		/* Add delay to get wakeup time */
		wakeup.tv_sec = now.tv_sec + (millis / 1000);
		wakeup.tv_nsec = (now.tv_usec * 1000)
		    + ((millis % 1000) * 1000000) + nanos;

		/* Handle nanosecond overflow */
		if (wakeup.tv_nsec >= 1000000000L) {
			wakeup.tv_sec += 1;
			wakeup.tv_nsec -= 1000000000L;
		}

		/* Handle second overflow */
		if (wakeup.tv_sec < now.tv_sec) {
			wakeup.tv_sec = LONG_MAX;
			wakeup.tv_nsec = 0;
		}
	}

	/* Thinlocks must be inflated because we need a condition variable */
	if (!_JC_LW_TEST(lockword, FAT)) {

		/* Verify that this thread owns the lock */
		if ((lockword & _JC_LW_THIN_TID_MASK) != env->thinlock_id) {
			_jc_post_exception(env,
			    _JC_IllegalMonitorStateException);
			_jc_throw_exception(env);
		}

		/* Inflate the lock */
		if (_jc_inflate_lock(env, this) != JNI_OK)
			_jc_throw_exception(env);

		/* Update object lockword */
		lockword = this->lockword;
		_JC_ASSERT(_JC_LW_TEST(lockword, FAT));

		/* Handle any contention from the thinlock */
		if (env->lock.owner.contention)
			_jc_lock_contention(env, this);

		/* Get the fat lock we just inflated */
		lock = vm->fat_locks.by_id[_JC_LW_EXTRACT(lockword, FAT_ID)];
		_JC_ASSERT(lock->u.owner == env);
	} else {

		/* Get the object's fat lock */
		lock = vm->fat_locks.by_id[_JC_LW_EXTRACT(lockword, FAT_ID)];

		/* Verify that this thread owns the lock */
		if (lock->u.owner != env) {
			_jc_post_exception(env,
			    _JC_IllegalMonitorStateException);
			_jc_throw_exception(env);
		}
	}

	/* Enter non-java mode */
	_jc_stopping_java(env, NULL, "Object.wait() on %s@%p",
	    this->type->name, this);

	/* Acquire the fat lock mutex */
	_JC_MUTEX_LOCK(env, lock->mutex);

	/* Unlock the object lock and wake up other fat lock waiters */
	recursion_count = lock->recursion_count;
	_JC_ASSERT(recursion_count != 0);
	lock->recursion_count = 0;
	lock->u.owner = NULL;
	_JC_COND_BROADCAST(lock->cond);

	/* Check and update this thread's interrupt status */
	while (JNI_TRUE) {
		switch ((old_status = env->interrupt_status)) {
		case _JC_INTERRUPT_CLEAR:
			env->interruptible_lock = lock;
			if (_jc_compare_and_swap(&env->interrupt_status,
			    old_status, _JC_INTERRUPT_INTERRUPTIBLE))
				goto sleep;
			break;
		case _JC_INTERRUPT_SET:
			env->interrupt_status = _JC_INTERRUPT_INTERRUPTED;
			goto finished;
		default:
			_JC_ASSERT(JNI_FALSE);
		}
	}

sleep:
	/* Record current notify counter */
	notify_count = lock->notify_count;

again:
	/*
	 * Sleep until woken up. According to SUSv2 spec, "sprious wakeups
	 * from pthread_cond_wait() or pthread_cond_timedwait() functions
	 * may occur". So just because we woke up it doesn't necessarily
	 * mean notify() or notifyAll() was called. This is also the case
	 * anyway, because Thread.interrupt() can wake us up also, even
	 * if we're not the target thread.
	 */
	if (timed_wait)
		_JC_COND_TIMEDWAIT(env, lock->notify, lock->mutex, &wakeup);
	else
		_JC_COND_WAIT(env, lock->notify, lock->mutex);

	/* Sanity check */
	_JC_ASSERT(env->interrupt_status == _JC_INTERRUPT_INTERRUPTIBLE
	    || env->interrupt_status == _JC_INTERRUPT_INTERRUPTED);

	/* If we got interrupted, we're free */
	if (env->interrupt_status == _JC_INTERRUPT_INTERRUPTED)
		goto finished;

	/* If there is a cross-posted exception for us, handle it now */
	if (env->cross_exception != NULL)
		goto finished;

	/*
	 * See if we really should have woken up. The object must have
	 * been notify()'d or the wakeup time reached. If the notify
	 * count hasn't changed then we haven't really been notify()'d.
	 */
	if (lock->notify_count == notify_count) {
		struct timespec tsnow;
		struct timeval now;

		/* If there is no timeout, go back to sleep */
		if (!timed_wait)
			goto again;

		/* See if we've passed the wakeup time */
		gettimeofday(&now, NULL);
		tsnow.tv_sec = now.tv_sec;
		tsnow.tv_nsec = now.tv_usec * 1000;
		if (tsnow.tv_sec > wakeup.tv_sec
		    || (tsnow.tv_sec == wakeup.tv_sec
		      && tsnow.tv_nsec >= wakeup.tv_nsec))
			goto finished;

		/* Not time yet, go back to sleep */
		goto again;
	}

	/* See if we are the lucky thread that gets to wake up */
	switch (lock->notify_wakeup) {
	case 1:				/* notify() called, we win */
		lock->notify_wakeup = 0;
		break;
	case 0:				/* notify() called, we lose */
		goto again;
	case -1:			/* notifyAll() called, everyone wins */
		break;
	}

finished:
	/* Wait for the object lock to be released */
	while (lock->recursion_count != 0)
		_JC_COND_WAIT(env, lock->cond, lock->mutex);

	/* OK, now we can grab the fat lock back */
	lock->recursion_count = recursion_count;
	lock->u.owner = env;

	/* Release the fat lock mutex */
	_JC_MUTEX_UNLOCK(env, lock->mutex);

	/* Back to running normal Java */
	_jc_resuming_java(env, NULL);

	/* Check to see if we were interrupted */
	interrupted = JNI_FALSE;
	while (JNI_TRUE) {
		switch ((old_status = env->interrupt_status)) {
		case _JC_INTERRUPT_INTERRUPTED:
			env->interrupt_status = _JC_INTERRUPT_CLEAR;
			env->interruptible_lock = NULL;
			interrupted = JNI_TRUE;
			goto done;
		case _JC_INTERRUPT_INTERRUPTIBLE:
			if (_jc_compare_and_swap(&env->interrupt_status,
			    old_status, _JC_INTERRUPT_CLEAR)) {
				env->interruptible_lock = NULL;
				goto done;
			}
			break;
		default:
			_JC_ASSERT(JNI_FALSE);
		}
	}

done:
	/* If there is a cross-posted exception waiting for us, throw it now */
	if ((ex = _jc_retrieve_cross_exception(env)) != NULL) {
		_jc_post_exception_object(env, ex);
		_jc_throw_exception(env);
	}

	/* Throw an InterruptedException if we were interrupted */
	if (interrupted) {
		_jc_post_exception(env, _JC_InterruptedException);
		_jc_throw_exception(env);
	}
}

/*
 * static final native void notify(Object)
 *	throws java/lang/IllegalMonitorStateException
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMObject_notify(_jc_env *env, _jc_object *this)
{
	do_notify(env, this, JNI_FALSE);
}

/*
 * static final native void notifyAll(Object)
 *	throws java/lang/IllegalMonitorStateException
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMObject_notifyAll(_jc_env *env, _jc_object *this)
{
	do_notify(env, this, JNI_TRUE);
}

/* 
 * Common function doing the work of notify() and notifyAll().
 */
static void
do_notify(_jc_env *env, _jc_object *this, jboolean broadcast)
{
	_jc_jvm *const vm = env->vm;
	_jc_fat_lock *lock;
	_jc_word lockword;

	/* Check for null */
	if (this == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}
	lockword = this->lockword;

	/*
	 * If lock is thin, things are simpler because there can
	 * be no contention so there's nobody to wake up.
	 */
	if (!_JC_LW_TEST(lockword, FAT)) {

		/* Make sure this thread is the owner */
		if ((lockword & _JC_LW_THIN_TID_MASK) != env->thinlock_id)
			goto illegal;

		/* Done */
		return;
	}

	/*
	 * Handle the fat lock case.
	 */
	lock = vm->fat_locks.by_id[_JC_LW_EXTRACT(lockword, FAT_ID)];
	_JC_ASSERT(lock != NULL);
	if (lock->u.owner != env)
		goto illegal;
	_JC_ASSERT(lock->recursion_count > 0);

	/* Wake up waiters and return */
	_JC_MUTEX_LOCK(env, lock->mutex);
	lock->notify_count++;
	if (broadcast) {
		lock->notify_wakeup = -1;
		_JC_COND_BROADCAST(lock->notify);
	} else {
		lock->notify_wakeup = 1;
		_JC_COND_SIGNAL(lock->notify);
	}
	_JC_MUTEX_UNLOCK(env, lock->mutex);
	return;

illegal:
	/* This thread is not the owner of the lock */
	_jc_post_exception(env, _JC_IllegalMonitorStateException);
	_jc_throw_exception(env);
}

