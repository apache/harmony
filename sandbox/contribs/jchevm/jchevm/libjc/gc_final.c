
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
 * $Id: gc_final.c,v 1.3 2005/03/16 15:31:12 archiecobbs Exp $
 */

#include "libjc.h"

/* Do a periodic check after scanning this many objects */
#define _JC_FINALIZE_CHECK		16

/*
 * Finalize finalizable objects and enqueue enqueable references.
 */
jint
_jc_gc_finalize(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_heap *const heap = &vm->heap;
	_jc_heap_sweep sweep;
	_jc_object *obj;
	int i = 0;

start_over:
	_jc_heap_sweep_init(heap, &sweep);
	while ((obj = _jc_heap_sweep_next(&sweep, JNI_FALSE)) != NULL) {
		_jc_word lockword = obj->lockword;
		jint gc_cycles;
		jobject ref;

		/* Sanity check */
		_JC_ASSERT((lockword & _JC_LW_KEEP_BIT) != 0);

		/* Check for live, enqueable references */
		if ((lockword & (_JC_LW_SPECIAL_BIT|_JC_LW_LIVE_BIT))
		      == (_JC_LW_SPECIAL_BIT|_JC_LW_LIVE_BIT)
		    && _jc_subclass_of(obj, vm->boot.types.Reference)
		    && *_JC_VMFIELD(vm, obj,
		      Reference, queue, _jc_object *) != NULL) {
			_jc_object *referent;

			/* Get referent */
			referent = *_JC_VMFIELD(vm, obj,
			    Reference, referent, _jc_object *);

			/* Sanity check: non-null referent should be kept */
			_JC_ASSERT(referent == NULL
			    || _JC_LW_TEST(referent->lockword, KEEP));

			/* References are enqueable if the referent is null */
			if (referent == NULL) {

				/* Add native reference to reference */
				ref = _jc_new_local_native_ref(env, obj);
				_JC_ASSERT(ref != NULL);

				/* Invoke enqueue(), ignoring any exception */
				gc_cycles = vm->gc_cycles;
				if (_jc_invoke_virtual(env,
				      vm->boot.methods.Reference.enqueue, obj)
				    != JNI_OK)
					_jc_retrieve_exception(env, NULL);

				/* Free local native reference */
				_jc_free_local_native_ref(&ref);

				/* If a GC cycle happened, start over */
				if (gc_cycles != vm->gc_cycles)
					goto start_over;

				/* Sanity check queue field is now null */
				_JC_ASSERT(*_JC_VMFIELD(vm, obj,
				    Reference, queue, _jc_object *) == NULL);
			}
		}

		/* Check for unreachable but finalizable objects */
		if ((lockword & (_JC_LW_LIVE_BIT|_JC_LW_FINALIZE_BIT))
		    != _JC_LW_FINALIZE_BIT)
			goto next_object;

		/* Add native reference to object */
		ref = _jc_new_local_native_ref(env, obj);
		_JC_ASSERT(ref != NULL);

		/* Mark object as no longer finalizable */
		while (!_jc_compare_and_swap(&obj->lockword,
		    lockword, lockword & ~_JC_LW_FINALIZE_BIT))
			lockword = obj->lockword;

		/*
		 * Finalize object, ignoring any exceptions. Note that a GC
		 * cycle can happen here; our native reference keeps this
		 * object (and any objects it references) alive.
		 */
		gc_cycles = vm->gc_cycles;
		if (_jc_invoke_virtual(env,
		    vm->boot.methods.Object.finalize, obj) != JNI_OK)
			_jc_retrieve_exception(env, NULL);

		/* Free local native reference */
		_jc_free_local_native_ref(&ref);

		/*
		 * If a GC cycle happened, more objects may have become
		 * finalizable, so start over.
		 */
		if (gc_cycles != vm->gc_cycles)
			goto start_over;

		/* Do a periodic check after finalization */
		i = -1;

next_object:
		/* Periodic check */
		if ((++i % _JC_FINALIZE_CHECK) == 0
		    && _jc_thread_check(env) != JNI_OK)
			return JNI_ERR;
	}

	/* Done */
	return JNI_OK;
}

