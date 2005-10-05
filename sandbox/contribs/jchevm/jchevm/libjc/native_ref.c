
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
 * $Id: native_ref.c,v 1.3 2004/07/05 21:03:27 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * This file manages local and global native references.
 * A native reference is a pointer to a pointer to an object.
 * The inner pointer lives in a native reference frame, which
 * itself lives in either the local (per-thread) or global
 * (per VM) native reference frame list. These lists are
 * scanned during garbage collection.
 */ 

/*
 * Internal functions
 */
static void		_jc_pop_native_frame(_jc_native_frame_list *list);
static jobject		_jc_new_native_ref(_jc_native_frame *frame);
static void		_jc_free_native_ref(jobject obj);

/*
 * Get a new local native reference.
 *
 * Returns NULL if 'obj' is NULL or there are no more local references.
 * In the latter case, an InternalError is posted as well.
 */
jobject
_jc_new_local_native_ref(_jc_env *env, _jc_object *obj)
{
	_jc_native_frame *frame;
	jobject ref = NULL;

	/* Return NULL for NULL */
	if (obj == NULL)
		return NULL;
	_JC_ASSERT((obj->lockword & _JC_LW_ODD_BIT) != 0);

	/* Find a free reference in the current local native reference frame */
	SLIST_FOREACH(frame, &env->native_locals, link) {
		if ((ref = _jc_new_native_ref(frame)) != NULL)
			break;
		if ((frame->flags & _JC_NATIVE_REF_EXTENSION) == 0)
			break;
	}

	/* If none found, bail, but avoid infinite recursion */
	if (ref == NULL) {
		_jc_native_frame stack_frame;

		_jc_push_stack_local_native_frame(env, &stack_frame);
		_jc_post_exception_msg(env, _JC_InternalError,
		    "max number of local native references exceeded");
		_jc_pop_local_native_frame(env, NULL);
		return NULL;
	}

	/* Sanity check */
	_JC_ASSERT(*ref == NULL);

	/* Done */
	*ref = obj;
	return ref;
}

/*
 * Free a local native reference and return the wrapped object.
 *
 * This function must not allow this thread to be blocked & GC'd.
 */
_jc_object *
_jc_free_local_native_ref(jobject *refp)
{
	const jobject ref = *refp;
	_jc_object *obj;

	if (ref == NULL)
		return NULL;
	obj = *ref;
	_JC_ASSERT(obj != NULL);
	_jc_free_native_ref(ref);
	*refp = NULL;
	return obj;
}

/*
 * Free all native locals. This is used when detaching a thread.
 */
void
_jc_free_all_native_local_refs(_jc_env *env)
{
	while (!SLIST_EMPTY(&env->native_locals))
		_jc_pop_native_frame(&env->native_locals);
}

/*
 * Get a new global native reference.
 *
 * This causes a new native reference frame to be added if necessary.
 * If unsuccessful, an exception is stored and NULL is returned.
 *
 * NOTE: The global VM mutex should not be acquired when calling this.
 */
jobject
_jc_new_global_native_ref(_jc_env *env, _jc_object *obj)
{
	_jc_jvm *const vm = env->vm;
	_jc_native_frame *frame;
	jobject ref = NULL;

	/* Return NULL for NULL */
	if (obj == NULL)
		return NULL;
	_JC_ASSERT((obj->lockword & _JC_LW_ODD_BIT) != 0);

	/* Acquire VM global mutex */
	_JC_MUTEX_LOCK(env, vm->mutex);

	/* Find a free reference in any global native reference frame */
	SLIST_FOREACH(frame, &vm->native_globals, link) {
		if ((ref = _jc_new_native_ref(frame)) != NULL)
			break;
	}

	/* If none found, create a new frame */
	if (ref == NULL) {
		if ((frame = _jc_add_native_frame(env,
		    &vm->native_globals)) == NULL)
			goto fail;
		ref = _jc_new_native_ref(frame);
		_JC_ASSERT(ref != NULL);
	}

	/* Sanity check */
	_JC_ASSERT(*ref == NULL);

	/* Point reference at object */
	*ref = obj;

fail:
	/* Release VM global mutex */
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Done */
	return ref;
}

/*
 * Free a global native reference and return the wrapped object.
 *
 * This function must not allow this thread to be blocked & GC'd.
 *
 * We could attempt to free unused global reference frames;
 * instead, we just choose to cache them for possible re-use.
 */
_jc_object *
_jc_free_global_native_ref(jobject *refp)
{
	const jobject ref = *refp;
	_jc_object *obj;

	if (ref == NULL)
		return NULL;
	obj = *ref;
	_JC_ASSERT(obj != NULL);
	_jc_free_native_ref(ref);
	*refp = NULL;
	return obj;
}

/*
 * Free all native globals. This is used when destroying the VM.
 */
void
_jc_free_all_native_global_refs(_jc_jvm *vm)
{
	while (!SLIST_EMPTY(&vm->native_globals))
		_jc_pop_native_frame(&vm->native_globals);
}

/*
 * Add a new native reference frame with the given number
 * of references (or default if zero) to the current thread.
 *
 * Posts OutOfMemoryError if unable.
 */
jint
_jc_push_local_native_frame(_jc_env *env, int num_refs)
{
	_jc_native_frame *frame;

	/* Zero refs means "use the default" */
	if (num_refs == 0)
		num_refs = _JC_NATIVE_REFS_PER_FRAME;
	_JC_ASSERT(num_refs > 0);

	/* Allocate first frame */
	if (_jc_add_native_frame(env, &env->native_locals) == NULL) {
		_jc_post_exception_info(env);
		return JNI_ERR;
	}

	/* Allocate additional extension frames as necessary */
	while ((num_refs -= _JC_NATIVE_REFS_PER_FRAME) > 0) {
		if ((frame = _jc_add_native_frame(env,
		    &env->native_locals)) == NULL) {
			_jc_pop_local_native_frame(env, NULL);	/* clean up */
			_jc_post_exception_info(env);
			return JNI_ERR;
		}
		frame->flags |= _JC_NATIVE_REF_EXTENSION;
	}

	/* Done */
	return JNI_OK;
}

/*
 * "Extend" the current local native reference frame by adding
 * additional frame(s) with the _JC_NATIVE_REF_EXTENSION flag set.
 *
 * Posts OutOfMemoryError if unable.
 */
jint
_jc_extend_local_native_frame(_jc_env *env, int num_refs)
{
	_jc_native_frame *frame;
	int num_frames;

	/* Add extension frames until we have enough new references */
	for (num_frames = 0; num_refs > 0;
	    num_frames++, num_refs -= _JC_NATIVE_REFS_PER_FRAME) {
		if ((frame = _jc_add_native_frame(env,
		    &env->native_locals)) == NULL) {
			while (num_frames-- > 0)	/* clean up */
				_jc_pop_native_frame(&env->native_locals);
			_jc_post_exception_info(env);
			return JNI_ERR;
		}
		frame->flags |= _JC_NATIVE_REF_EXTENSION;
	}

	/* Done */
	return JNI_OK;
}

/*
 * Initialize a new local native reference frame that is allocated
 * on the stack of the caller. The calling function must free the frame
 * by calling _jc_pop_local_native_frame() before returning.
 */
void
_jc_push_stack_local_native_frame(_jc_env *env, _jc_native_frame *frame)
{
	memset(frame, 0, sizeof(*frame));
	frame->flags = _JC_NATIVE_REF_FREE_BITS
	    | _JC_NATIVE_REF_STACK_ALLOC | _JC_NATIVE_REF_ODD_BIT;
	SLIST_INSERT_HEAD(&env->native_locals, frame, link);
}

/*
 * Free the top-most (i.e., last) local native reference frame.
 * If 'obj' is non-NULL, attempt to create a reference for the object
 * in the local native reference frame one below the one we're popping
 * and return it. Otherwise, return NULL.
 *
 * Note: this function must not allow this thread to be blocked & GC'd
 * when 'obj' is NULL.
 */
jobject
_jc_pop_local_native_frame(_jc_env *env, _jc_object *obj)
{
	_jc_native_frame *frame;
	jboolean extension;

again:
	/* Get the top frame */
	_JC_ASSERT(!SLIST_EMPTY(&env->native_locals));
	frame = SLIST_FIRST(&env->native_locals);

	/* Is this an extension of the previous frame? */
	extension = (frame->flags & _JC_NATIVE_REF_EXTENSION) != 0;

	/* Remove frame */
	_jc_pop_native_frame(&env->native_locals);

	/* If it was an extension of previous frame, pop another */
	if (extension)
		goto again;

	/* Create a new reference for 'obj' in underlying frame if needed */
	if (obj != NULL)
		return _jc_new_local_native_ref(env, obj);
	return NULL;
}

/*
 * Add a new native reference frame to the given frame list.
 *
 * If unsuccessful, an exception is stored.
 */
_jc_native_frame *
_jc_add_native_frame(_jc_env *env, _jc_native_frame_list *list)
{
	_jc_native_frame *frame;

	/* Allocate new frame */
	if ((frame = _jc_vm_zalloc(env, sizeof(*frame))) == NULL)
		return NULL;

	/* Initialize frame and add to list */
	frame->flags = _JC_NATIVE_REF_FREE_BITS | _JC_NATIVE_REF_ODD_BIT;
	SLIST_INSERT_HEAD(list, frame, link);

	/* Done */
	return frame;
}

/*
 * Remove the first (i.e., top-most on the stack) native reference frame.
 */
static void
_jc_pop_native_frame(_jc_native_frame_list *list)
{
	_jc_native_frame *frame;
	int stack_alloc;

	/* Sanity check frame list */
	_JC_ASSERT(!SLIST_EMPTY(list));

	/* Get frame */
	frame = SLIST_FIRST(list);

	/* Determine if frame is stack allocated */
	stack_alloc = (frame->flags & _JC_NATIVE_REF_STACK_ALLOC) != 0;

	/* Sanity check stack allocated frames are still on the stack */
#if _JC_DOWNWARD_STACK
	_JC_ASSERT(!stack_alloc || (char *)frame > (char *)&frame);
#else
	_JC_ASSERT(!stack_alloc || (char *)frame < (char *)&frame);
#endif

	/* Remove frame and explicitly free it if necessary */
	SLIST_REMOVE_HEAD(list, link);
	if (!stack_alloc)
		_jc_vm_free(&frame);
}

/*
 * Find a free native reference in the given native reference frame.
 *
 * Returns NULL (without posting any exceptions) if unable.
 */
static jobject
_jc_new_native_ref(_jc_native_frame *frame)
{
	int i;

	/* Any references free at all in this frame? */
	if (!_JC_NATIVE_REF_ANY_FREE(frame))
		return NULL;

	/* Find the first free one */
	i = ffs(frame->flags & _JC_NATIVE_REF_FREE_BITS) - 4;

	/* Sanity check */
	_JC_ASSERT(i >= 0 && i < _JC_NATIVE_REFS_PER_FRAME);

	/* Mark reference in use */
	_JC_NATIVE_REF_MARK_IN_USE(frame, i);

	/* Done */
	return &frame->refs[i];
}

/*
 * Free a non-NULL native reference.
 *
 * We need to find the beginning of the native reference frame
 * containing the given native reference. We can do this by looking
 * backwards in memory.
 *
 * This works because while all native references are aligned pointers
 * and therefore zero as the low-order bit, frame->flags always has
 * one as its low-order bit.
 */
static void
_jc_free_native_ref(jobject obj)
{
	_jc_native_frame *frame = NULL;
	int i;

	/* Invalidate the reference */
	_JC_ASSERT(obj != NULL);
	*obj = NULL;

	/* Find the beginning of this native reference frame */
	for (i = 0; i < _JC_NATIVE_REFS_PER_FRAME; i++) {
		if (((_jc_word)(*--obj) & 0x1) != 0) {
			frame = (_jc_native_frame *)((char *)obj
			    - _JC_OFFSETOF(_jc_native_frame, flags));
			break;
		}
	}
	_JC_ASSERT(i < _JC_NATIVE_REFS_PER_FRAME);

	/* Mark this reference as 'free' */
	_JC_NATIVE_REF_MARK_FREE(frame, i);
}

