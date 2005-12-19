
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

/* Internal functions */
static int	_jc_gc_trace(_jc_env *env, _jc_trace_info *trace);
static int	_jc_gc_mark_object(_jc_trace_info *trace, _jc_object *obj);
static jint	_jc_gc_push_refs(_jc_env *env, _jc_uni_mem *uni,
			_jc_scan_frame **framep, _jc_object **refs, int nrefs);
static int	_jc_get_explicit_refs(_jc_env *env, _jc_type **types,
			int ntypes, _jc_object **list);

/*
 * Collect garbage.
 *
 * This is a "stop the world" mark-sweep collection algorithm.
 *
 * Because we allocate instances of java.lang.Class within the corresponding
 * class loader's memory area instead of the heap, for the purposes of
 * marking memory in use we treat the class loader memory area as a single
 * blob which is either marked or not marked. The "marked bit" is contained
 * in the corresponding _jc_class_loader structure.
 *
 * If unsuccessful, an exception is stored.
 */
jint
_jc_gc(_jc_env *env, jboolean urgent)
{
	_jc_jvm *const vm = env->vm;
	_jc_heap *const heap = &vm->heap;
	_jc_object **root_set = NULL;
	_jc_trace_info *trace = NULL;
	struct timeval start_time;
	_jc_class_loader *loader;
	char *last_small_page;
	int root_set_length;
	_jc_heap_sweep sweep;
	_jc_object *obj;
	jint status = JNI_OK;
	_jc_env *thread;
	jint gc_cycles;
	int bsi;
	int i;

	/*
	 * Sanity check: if an object is recyclable and has its lockword as
	 * the first work in a heap block, then when the block is marked free
	 * it must still look like a recyclable object was there.
	 */
	_JC_ASSERT((_JC_HEAP_BLOCK_FREE
	    & (_JC_LW_LIVE_BIT|_JC_LW_KEEP_BIT|_JC_LW_FINALIZE_BIT)) == 0);

	/* Can't do GC during initial bootstrapping */
	if (vm->initialization != NULL) {
		_JC_EX_STORE(env, OutOfMemoryError, "gc during bootstrap");
		return JNI_ERR;
	}

	/* Record current GC cycle count */
	gc_cycles = vm->gc_cycles;

	/* Stop the world and free any leftover thread stacks */
	_JC_MUTEX_LOCK(env, vm->mutex);
	_jc_stop_the_world(env);
	_jc_free_thread_stacks(vm);
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* If another thread just did a GC cycle, we don't need to do one */
	if (vm->gc_cycles != gc_cycles)
		goto done;

	/* Bump GC cycle counter and visited bit for stack-allocated objects */
	vm->gc_cycles++;
	vm->gc_stack_visited ^= _JC_LW_VISITED_BIT;

	/* Verbosity */
	if ((vm->verbose_flags & (1 << _JC_VERBOSE_GC)) != 0) {
		VERBOSE(GC, vm, "starting garbage collection #%d",
		    vm->gc_cycles);
		gettimeofday(&start_time, NULL);
	}

#if 0
	_jc_heap_check(vm);
#endif

	/* Initialize trace state */
	if ((trace = _JC_STACK_ALLOC(env, sizeof(*trace)
	    + heap->num_sizes * sizeof(*trace->num_small_objects))) == NULL)
		goto fail;
	memset(trace, 0, sizeof(*trace)
	    + heap->num_sizes * sizeof(*trace->num_small_objects));
	trace->heap = &vm->heap;
	trace->follow_soft = !urgent;
	trace->gc_stack_visited = vm->gc_stack_visited;

	/* Reset all small page hints and "use first" page lists */
	for (bsi = 0; bsi < heap->num_sizes; bsi++) {
		heap->sizes[bsi].pages = NULL;
		heap->sizes[bsi].hint = NULL;
	}

	/* Unmark all class loaders */
	LIST_FOREACH(loader, &vm->class_loaders, link)
		loader->gc_mark = 0;

	/*
	 * We must clip the C stack for the current thread. Otherwise,
	 * references could "leak" into subsequent C stack frames and
	 * we'd miss them when scanning this thread's C stack.
	 */
	_jc_stack_clip(env);

	/* Compute the root set */
	_JC_MUTEX_LOCK(env, vm->mutex);
	root_set_length = _jc_root_walk(env, &root_set);
	_JC_MUTEX_UNLOCK(env, vm->mutex);
	if (root_set_length == -1)
		goto fail;

	/* Set all object bits LK -> 00 and count finalizable objects */
	_jc_heap_sweep_init(heap, &sweep);
	while ((obj = _jc_heap_sweep_next(&sweep, JNI_FALSE)) != NULL) {
		obj->lockword &= ~(_JC_LW_LIVE_BIT|_JC_LW_KEEP_BIT);
		if (_JC_LW_TEST(obj->lockword, FINALIZE))
			trace->num_finalizable++;
	}

	/* Trace live objects starting with the root set, setting LK -> 11 */
	trace->mark_bits = _JC_LW_LIVE_BIT | _JC_LW_KEEP_BIT;
	trace->bottom_frame.posn = 0;
	trace->bottom_frame.prev = NULL;
	trace->bottom_frame.next = NULL;
	trace->bottom_frame.lists[0].start = root_set;
	trace->bottom_frame.lists[0].end = root_set + root_set_length;
	if (_jc_gc_trace(env, trace) != JNI_OK)
		goto fail;

	/*
	 * If any objects are finalizable but not reachable, we must
	 * mark them and all other objects reachable from them as keepable.
	 * That is, they are reachable (via finalization) but not live.
	 */
	_JC_ASSERT(trace->num_finalizable >= 0);
	if (trace->num_finalizable > 0) {
		const int num_finalizable = trace->num_finalizable;
		_jc_object **refs;

		/* Ensure the finalizer thread runs */
		trace->wakeup_finalizer = JNI_TRUE;

		/* Reset trace info and re-use root set ref list */
		trace->mark_bits = _JC_LW_KEEP_BIT;	/* note: not LIVE */
		trace->bottom_frame.posn = 0;
		trace->bottom_frame.prev = NULL;
		trace->bottom_frame.next = NULL;
		trace->bottom_frame.lists[0].start = root_set;
		trace->bottom_frame.lists[0].end = root_set
		    + trace->num_finalizable;

		/* Allocate another ref list if needed */
		if (trace->num_finalizable > root_set_length) {
			const int num_more_refs
			    = trace->num_finalizable - root_set_length;
			_jc_object **more_refs;

			/* Use entire root set ref list */
			trace->bottom_frame.lists[0].end = root_set
			    + root_set_length; 

			/* Allocate another list for the remainder */
			if ((more_refs = _JC_STACK_ALLOC(env,
			    + num_more_refs * sizeof(*more_refs))) == NULL)
				goto fail;
			trace->bottom_frame.lists[1].start = more_refs;
			trace->bottom_frame.lists[1].end
			    = more_refs + num_more_refs;
			trace->bottom_frame.posn++;
		}

		/*
		 * Scan heap for finalizable but not reachable objects.
		 * Fill up the first (or first two) ref lists.
		 */
		_jc_heap_sweep_init(heap, &sweep);
		refs = root_set;
		while ((obj = _jc_heap_sweep_next(&sweep, JNI_FALSE)) != NULL) {
			if ((obj->lockword
			      & (_JC_LW_LIVE_BIT|_JC_LW_FINALIZE_BIT))
			    == _JC_LW_FINALIZE_BIT) {
				if (refs == trace->bottom_frame.lists[0].end) {
					refs = trace->bottom_frame
					    .lists[1].start;
				}
				*refs++ = obj;
			}
		}
		_JC_ASSERT(refs == trace->bottom_frame.lists[0].end
		    || refs == trace->bottom_frame.lists[1].end);

		/*
		 * Trace finalizable-reachable objects, setting K -> 1.
		 * Note: we should not encounter any new stack allocated
		 * objects during this scan.
		 */
		if (_jc_gc_trace(env, trace) != JNI_OK)
			goto fail;

		/* Repair count of finalizable objects */
		_JC_ASSERT(trace->num_finalizable == 0);
		trace->num_finalizable = num_finalizable;
	}

	/* Now recycle unreachable blocks and pages */
	_jc_heap_sweep_init(heap, &sweep);
	last_small_page = NULL;
	while ((obj = _jc_heap_sweep_next(&sweep, JNI_TRUE)) != NULL) {
		const _jc_word lockword = obj->lockword;

		/* Sanity check: LIVE implies KEEP */
		_JC_ASSERT(_JC_LW_TEST(lockword, KEEP)
		    || !_JC_LW_TEST(lockword, LIVE));

		/*
		 * Keep keepable objects and free the rest. But for any
		 * keepable weak (and maybe soft) references that point to
		 * objects which are no longer live, clear the reference.
		 */
		switch (lockword & (_JC_LW_KEEP_BIT|_JC_LW_SPECIAL_BIT)) {
		case _JC_LW_SPECIAL_BIT:
		case 0:
			goto free_it;
		case _JC_LW_KEEP_BIT:
			goto keep_it;
		case _JC_LW_SPECIAL_BIT | _JC_LW_KEEP_BIT:
		    {
			_jc_object **referent;

			/* Rule out non-Reference objects */
			if (!_jc_subclass_of(obj, vm->boot.types.Reference))
				goto keep_it;

			/* Rule out referents already cleared or reachable */
			referent = _JC_VMFIELD(vm,
			    obj, Reference, referent, _jc_object *);
			if (*referent == NULL
			    || _JC_LW_TEST((*referent)->lockword, LIVE))
				goto keep_it;

			/*
			 * Sanity check: if we're following soft references,
			 * then a soft reference's referent must be live.
			 */
			_JC_ASSERT(!trace->follow_soft
			    || !_jc_subclass_of(obj,
			      vm->boot.types.SoftReference));

			/*
			 * Rule out phantom references if the referent
			 * object is not recyclable yet.
			 */
			if (_jc_subclass_of(obj,
			      vm->boot.types.PhantomReference)
			    && ((*referent)->lockword
			      & (_JC_LW_FINALIZE_BIT|_JC_LW_KEEP_BIT)) != 0)
				goto keep_it;

			/* Clear the reference */
			trace->num_refs_cleared++;
			*referent = NULL;

			/* Wakeup finalizer if reference needs enqueuing */
			if (*_JC_VMFIELD(vm, obj,
			    Reference, queue, _jc_object *) != NULL)
				trace->wakeup_finalizer = JNI_TRUE;
			break;
		    }
		}

keep_it:
		/* Update stats and mark page in use */
		if (sweep.size != NULL) {
			trace->num_small_objects[sweep.bsi]++;
			if (last_small_page != sweep.page) {
				last_small_page = sweep.page;
				trace->num_small_pages++;
			}
			sweep.blocks_live = 1;
		} else {
			trace->num_large_objects++;
			trace->num_large_pages += sweep.npages;
		}

		/* Leave object in the heap */
		continue;

free_it:
		/* Sanity check */
		_JC_ASSERT((lockword
		    & (_JC_LW_LIVE_BIT|_JC_LW_FINALIZE_BIT)) == 0);

		/* Update stats */
		trace->num_recycled_objects++;

		/* Recycle fat lock */
		if (_JC_LW_TEST(lockword, FAT)) {
			trace->num_fat_locks_recycled++;
			_jc_free_lock(vm, obj);
		}

		/* Mark small page block or large page range free */
		if (sweep.size != NULL)
			*((_jc_word *)sweep.block) = _JC_HEAP_BLOCK_FREE;
		else {
			for (i = 0; i < sweep.npages; i++) {
				*_JC_PAGE_ADDR(sweep.page, i)
				    = _JC_HEAP_PAGE_FREE;
			}
		}
	}

loader_check:
	/* Unload unloadable class loaders */
	LIST_FOREACH(loader, &vm->class_loaders, link) {
		if (loader->gc_mark)
			continue;
		_JC_ASSERT(loader != vm->boot.loader);
		VERBOSE(GC, vm, "unloading class loader %s@%p (%d classes)",
		    loader->instance->type->name, loader->instance,
		    loader->defined_types.size);
		_JC_MUTEX_LOCK(env, vm->mutex);
		_jc_destroy_loader(vm, &loader);
		_JC_MUTEX_UNLOCK(env, vm->mutex);
		goto loader_check;
	}

	/* Reset next free page pointer */
	heap->next_page = 0;

	/* Mark all threads as no longer memory critical (we hope) */
	LIST_FOREACH(thread, &vm->threads.alive_list, link)
		thread->out_of_memory = 0;

	/* Wake up finalizer thread if there is any work for it to do */
	if (trace->wakeup_finalizer)
		_jc_thread_interrupt_instance(vm, *vm->finalizer_thread);

	/* Verbosity */
	if ((vm->verbose_flags & (1 << _JC_VERBOSE_GC)) != 0) {
		struct timeval finish_time;
		int num_small_objects;
		int num_loader_pages;
		int num_pages;
		char buf[80];
		int bsi;

		/* Calculate time spent during GC */
		gettimeofday(&finish_time, NULL);
		finish_time.tv_sec -= start_time.tv_sec;
		finish_time.tv_usec -= start_time.tv_usec;
		if (finish_time.tv_usec < 0) {
			finish_time.tv_sec--;
			finish_time.tv_usec += 1000000;
		}

		/* Sum total number of small objects */
		num_small_objects = 0;
		for (bsi = 0; bsi < heap->num_sizes; bsi++)
			num_small_objects += trace->num_small_objects[bsi];

		/* Sum total number of pages in use */
		num_pages = trace->num_small_pages + trace->num_large_pages;
		num_loader_pages = vm->max_loader_pages
		    - vm->avail_loader_pages;

		/* Print summary info */
		VERBOSE(GC, vm, "heap pages in use: %d/%d (%d%%)",
		    num_pages, heap->num_pages,
		    (num_pages * 100) / heap->num_pages);
		VERBOSE(GC, vm, "class loader pages in use: %d/%d (%d%%)",
		    num_loader_pages, vm->max_loader_pages,
		    (num_loader_pages * 100) / vm->max_loader_pages);
		VERBOSE(GC, vm, "number of small objects: %d in %d pages",
		    num_small_objects, trace->num_small_pages);
		VERBOSE(GC, vm, "number of large objects: %d in %d pages",
		    trace->num_large_objects, trace->num_large_pages);
		VERBOSE(GC, vm, "number of finalizable objects: %d",
		    trace->num_finalizable);
		VERBOSE(GC, vm, "number of references cleared: %d",
		    trace->num_refs_cleared);
		VERBOSE(GC, vm, "number of objects reclaimed: %d",
		    trace->num_recycled_objects);
		VERBOSE(GC, vm, "number of fat locks recycled: %d",
		    trace->num_fat_locks_recycled);
		VERBOSE(GC, vm, "distribution of small object sizes:");
		for (bsi = 0; bsi < heap->num_sizes; bsi++) {
			if (bsi % 4 == 0)
				*buf = '\0';
			snprintf(buf + strlen(buf), sizeof(buf) - strlen(buf),
			    "%5d:%6d", heap->sizes[bsi].size,
			    trace->num_small_objects[bsi]);
			if ((bsi + 1) % 4 == 0 || bsi == heap->num_sizes - 1)
				VERBOSE(GC, vm, "%s", buf);
		}
		VERBOSE(GC, vm, "garbage collection completed in %d.%03d sec",
		    finish_time.tv_sec, finish_time.tv_usec / 1000);
	}

#if 0
    /* This dumps the number of objects of each type on the stack */
    {
	_jc_splay_tree tree;
	_jc_type_node **nodes;

	_jc_splay_init(&tree, _jc_node_cmp, _JC_OFFSETOF(_jc_type_node, node));
	_jc_heap_sweep_init(heap, &sweep);
	while ((obj = _jc_heap_sweep_next(&sweep, JNI_FALSE)) != NULL) {
		_jc_type_node *node;
		_jc_type_node key;

		key.type = obj->type;
		if ((node = _jc_splay_find(&tree, &key)) == NULL) {
			node = _jc_vm_zalloc(env, sizeof(*node));
			node->type = obj->type;
			node->thread = (void *)1;
			_jc_splay_insert(&tree, node);
		} else
			node->thread = (void *)((int)node->thread + 1);
	}
	nodes = alloca(tree.size * sizeof(*nodes));
	_jc_splay_list(&tree, (void **)nodes);
	static int compare(const void *v1, const void *v2) {
		const _jc_type_node *const n1 = *(const _jc_type_node **)v1;
		const _jc_type_node *const n2 = *(const _jc_type_node **)v2;
		return (int)n2->thread - (int)n1->thread;
	}
	qsort(nodes, tree.size, sizeof(*nodes), compare);
	for (i = 0; i < tree.size; i++) {
		_jc_type_node *node = nodes[i];

		printf("%7d %s\n", (int)node->thread, node->type->name);
		_jc_vm_free(&node);
	}
    }
#endif

done:
#if 0
	_jc_heap_check(vm);
#endif

	/* Free root set memory */
	_jc_vm_free(&root_set);

	/* Resume the world */
	_JC_MUTEX_LOCK(env, vm->mutex);
	_jc_resume_the_world(env);
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* If we woke up the finalizer, give it a chance to run */
	if (trace != NULL && trace->wakeup_finalizer)
		sched_yield();

	/* Done */
#ifndef NDEBUG
	if (env->c_stack != NULL)
		env->c_stack->clipped = JNI_FALSE;
#endif
	return status;

fail:
	/* Restore the heap to a normal state */
	_jc_heap_sweep_init(heap, &sweep);
	while ((obj = _jc_heap_sweep_next(&sweep, JNI_FALSE)) != NULL)
		obj->lockword |= _JC_LW_LIVE_BIT|_JC_LW_KEEP_BIT;
	/* XXX need to restore stack-allocated objects XXX */

	/* Report failure */
	VERBOSE(GC, vm, "garbage collection FAILED: %s",
	    _jc_vmex_names[env->ex.num]);

	/* Return failure */
	status = JNI_ERR;
	goto done;
}

/*
 * Mark an object in the heap. Returns 1 if object is already marked.
 */
static inline int
_jc_gc_mark_object(_jc_trace_info *trace, _jc_object *obj)
{
	_jc_word lockword = obj->lockword;

	/* Sanity checks */
	_JC_ASSERT(_JC_LW_TEST(lockword, ODD));

	/* If object is stack-allocated, initialize it for this GC cycle */
	if (!_JC_IN_HEAP(trace->heap, obj)) {

		/* Is object already marked? */
		if ((lockword & _JC_LW_VISITED_BIT)
		    == trace->gc_stack_visited)
		    	return 1;

		/* Sanity check that this is the first GC cycle */
		_JC_ASSERT(trace->mark_bits
		    == (_JC_LW_KEEP_BIT | _JC_LW_LIVE_BIT));

		/* Sanity check LK = 11 already */
		_JC_ASSERT((lockword & (_JC_LW_KEEP_BIT | _JC_LW_LIVE_BIT))
		    == (_JC_LW_KEEP_BIT | _JC_LW_LIVE_BIT));

		/* Mark object visited this GC cycle */
		obj->lockword = lockword ^ _JC_LW_VISITED_BIT;
		return 0;
	}

	/* Is object already marked? */
	if (_JC_LW_TEST(lockword, KEEP))
		return 1;

	/* Keep track of finalizable but unreachable objects */
	if (_JC_LW_TEST(lockword, FINALIZE))
		trace->num_finalizable--;

	/* Mark object */
	obj->lockword = lockword | trace->mark_bits;
	return 0;
}

/*
 * Push a new list of references onto the scanning stack.
 */
static inline jint
_jc_gc_push_refs(_jc_env *env, _jc_uni_mem *uni, _jc_scan_frame **framep,
	_jc_object **refs, int nrefs)
{
	_jc_scan_frame *frame = *framep;

	/* Find room in this, next, or new scan frame */
	if (frame->posn == (sizeof(frame->lists) / sizeof(*frame->lists)) - 1) {
		if (frame->next != NULL) {
			frame = frame->next;
			_JC_ASSERT(frame->posn == -1);
		} else {
			_jc_scan_frame *next;

			if ((next = _jc_uni_alloc(env,
			    uni, sizeof(*next))) == NULL)
				return JNI_ERR;
			next->posn = -1;
			next->prev = frame;
			next->next = NULL;
			frame->next = next;
			frame = next;
		}
		*framep = frame;
	}

	/* Push new reference list onto current frame */
	frame->posn++;
	frame->lists[frame->posn].start = refs;
	frame->lists[frame->posn].end = refs + nrefs;
	return JNI_OK;
}

/*
 * Scan all objects reachable from the supplied root set.
 */
static int
_jc_gc_trace(_jc_env *env, _jc_trace_info *trace)
{
	_jc_jvm *const vm = env->vm;
	const int referent_index
	    = -vm->boot.fields.Reference.referent->offset / sizeof(void *);
	_jc_scan_frame *frame = &trace->bottom_frame;
	_jc_class_loader *loader = NULL;
	_jc_class_loader *other_loader = NULL;
	_jc_uni_mem uni;

	/* Initialize uni-allocator */
	_JC_ASSERT(frame->next == NULL);
	_jc_uni_alloc_init(&uni, 0, NULL);

	/* Recursively scan references */
	while (1) {
		_jc_object *referent;
		_jc_object *obj;
		int nrefs;

		/* Sanity check */
		_JC_ASSERT(frame->next == NULL || frame->next->posn == -1);
		_JC_ASSERT(frame != NULL && frame->posn >= 0);
		_JC_ASSERT(frame->lists[frame->posn].start
		     <= frame->lists[frame->posn].end);

		/* Check for end of reference list */
		if (frame->lists[frame->posn].start
		    == frame->lists[frame->posn].end) {
			if (frame->posn-- == 0 && (frame = frame->prev) == NULL)
				break;
			continue;
		}

		/*
		 * Extract the next object pointer in the reference list
		 * sitting on the top of the scan stack. Ignore null's.
		 */
		if ((obj = *frame->lists[frame->posn].start++) == NULL)
			continue;

		/*
		 * Handle the common case of a normal (non-special) object.
		 * We need to follow these references:
		 *
		 *  (a) explicit references, i.e., object reference fields
		 *  (b) implicit reference to the object's class loader
		 */
		if (!_JC_LW_TEST(obj->lockword, SPECIAL)) {

			/* Mark object; if already marked, skip it */
			if (_jc_gc_mark_object(trace, obj))
				continue;

do_normal_object:
			/* Push object's explicit references onto the stack */
			if ((obj->lockword & _JC_LW_REF_COUNT_MASK) != 0) {
				nrefs = _jc_num_refs(obj);
				if (_jc_gc_push_refs(env, &uni, &frame,
				      (_jc_object **)obj - nrefs, nrefs)
				    != JNI_OK)
					goto fail;
			}

			/* Now handle object's implicit Class reference */
			if (!(loader = obj->type->loader)->gc_mark)
				goto do_class_loader;
			continue;
		}

		/*
		 * Special case: java.lang.Class objects. They are allocated
		 * from class loader memory rather than the heap, so we don't
		 * mark them, but we do mark their associated class loaders.
		 *
		 * The Class class itself is loaded by the bootstrap loader.
		 */
		if (obj->type == vm->boot.types.Class) {
			_jc_type *type;

			/* Sanity check */
			_JC_ASSERT(!_JC_IN_HEAP(&vm->heap, obj));

			/* Get class associated with this Class object */
			type = *_JC_VMFIELD(vm, obj, Class, vmdata, _jc_type *);

			/* Handle class' class loader */
			if (!(loader = type->loader)->gc_mark)
				goto do_class_loader;
			continue;
		}

		/* Non-Class special object: first, mark it normally */
		if (_jc_gc_mark_object(trace, obj))
			continue;

		/*
		 * Special case: soft/weak/phantom references.
		 * We have to handle the 'referent' field specially.
		 */
		if (!_jc_subclass_of(obj, vm->boot.types.Reference))
			goto not_reference_object;

		/* Sanity check */
		_JC_ASSERT(_jc_subclass_of(obj, vm->boot.types.WeakReference)
		    || _jc_subclass_of(obj, vm->boot.types.SoftReference)
		    || _jc_subclass_of(obj, vm->boot.types.PhantomReference));

		/* If "referent" is null, treat object normally */
		if ((referent = *_JC_VMFIELD(vm, obj,
		    Reference, referent, _jc_object *)) == NULL)
			goto do_normal_object;

		/* If we're following soft references, treat them normally */
		if (trace->follow_soft
		    && _jc_subclass_of(obj, vm->boot.types.SoftReference))
			goto do_normal_object;

		/* Follow all object references except "referent" */
		if (referent_index > 1) {
			if (_jc_gc_push_refs(env, &uni, &frame,
			    (_jc_object **)obj - (referent_index - 1),
			    referent_index - 1) != JNI_OK)
				goto fail;
		}
		if ((nrefs = _jc_num_refs(obj)) > referent_index) {
			if (_jc_gc_push_refs(env, &uni, &frame,
			    (_jc_object **)obj - nrefs,
			    nrefs - referent_index) != JNI_OK)
				goto fail;
		}

		/* Now handle object's class loader */
		if (!(loader = obj->type->loader)->gc_mark)
			goto do_class_loader;
		continue;

not_reference_object:
		/*
		 * Not a Class or Reference object. Push the object's
		 * explicit references (reference fields) onto the stack.
		 * Then all we have left are its implicit references.
		 */
		if ((obj->lockword & _JC_LW_REF_COUNT_MASK) != 0) {
			nrefs = _jc_num_refs(obj);
			if (_jc_gc_push_refs(env, &uni, &frame,
			    (_jc_object **)obj - nrefs, nrefs) != JNI_OK)
				goto fail;
		}

		/*
		 * Special case: exceptions. They can contain the stack trace
		 * as an array of _jc_saved_frame structures in VMThrowable.
		 * We must mark the classes of methods in the stack trace.
		 */
		if (obj->type == vm->boot.types.VMThrowable) {
			_jc_saved_frame *frames;
			_jc_byte_array *bytes;
			_jc_object **refs;
			int num_frames;
			int num_refs;
			int i;

			/* Get saved stack frames from 'vmdata' byte[] array */
			bytes = *_JC_VMFIELD(vm, obj,
			    VMThrowable, vmdata, _jc_byte_array *);
			if (bytes == NULL)
				goto do_exception_loader;
			frames = (_jc_saved_frame *)_JC_ROUNDUP2(
			    (_jc_word)bytes->elems, _JC_FULL_ALIGNMENT);
			num_frames = (bytes->length -
			      ((_jc_word)frames - (_jc_word)bytes->elems))
			    / sizeof(*frames);
			if (num_frames == 0)
				goto do_exception_loader;

			/* Count unmarked class loaders referred to by stack */
			for (num_refs = i = 0; i < num_frames; i++) {
				_jc_type *const class = frames[i].method->class;

				if (!class->loader->gc_mark
				    && class->loader != vm->boot.loader)
					num_refs++;
			}
			if (num_refs == 0)
				goto do_exception_loader;

			/* Allocate an array of references for stack classes */
			if ((refs = _jc_uni_alloc(env, &uni,
			      num_refs * sizeof(*refs))) == NULL)
				goto fail;

			/* Fill in array using stack trace classes */
			for (num_refs = i = 0; i < num_frames; i++) {
				_jc_type *const class = frames[i].method->class;

				if (!class->loader->gc_mark
				    && class->loader != vm->boot.loader)
					refs[num_refs++] = class->instance;
			}

			/* Push stack trace references onto the scan stack */
			if (_jc_gc_push_refs(env, &uni,
			    &frame, refs, num_refs) != JNI_OK)
				goto fail;

do_exception_loader:
			/* Do exception object's class loader */
			if (!(loader = obj->type->loader)->gc_mark)
				goto do_class_loader;
			continue;
		}

		/*
		 * Special case: ClassLoaders. We have to mark both the
		 * implicitly associated class loader (if any) and also
		 * the class loader associated with the ClassLoader object;
		 * they may be different and neither be the boot loader.
		 */
		if (_jc_subclass_of(obj, vm->boot.types.ClassLoader)) {
			_jc_class_loader *cl_loader;

			/* Get ClassLoader loader and normal object loader */
			cl_loader = _jc_get_vm_pointer(vm,
			    obj, vm->boot.fields.ClassLoader.vmdata);
			loader = obj->type->loader;

			/* Do both class loaders */
			if (loader->gc_mark) {
				if (cl_loader == NULL || cl_loader->gc_mark)
					continue;
				loader = cl_loader;
			} else if (cl_loader != NULL && !cl_loader->gc_mark)
				other_loader = cl_loader;
			goto do_class_loader;
		}

		/* Special object that we didn't recognize - impossible! */
		_JC_ASSERT(JNI_FALSE);

do_class_loader:
	    {
		_jc_type **loader_types;
		int num_loader_types;
		_jc_object **erefs;
		int num_erefs;

		/*
		 * Mark a class loader "object", then then push all references
		 * (both explicit and implicit) from all of its Class instances
		 * onto the scan stack.
		 */
		_JC_ASSERT(!loader->gc_mark);
		loader->gc_mark = JNI_TRUE;

		/*
		 * Compute size of the list of this loader's defined types.
		 * The boot loader is a special case: the primitive types
		 * are not in the derived types tree, so we have to account
		 * for them manually.
		 */
		num_loader_types = loader->defined_types.size;
		if (loader == vm->boot.loader)
		      num_loader_types += _JC_TYPE_VOID - _JC_TYPE_BOOLEAN + 1;

		/* Populate the list of this loader's defined types */
		if ((loader_types = _jc_vm_alloc(env,
		    num_loader_types * sizeof(*loader_types))) == NULL)
			goto fail;
		_jc_splay_list(&loader->defined_types, (void **)loader_types);
		if (loader == vm->boot.loader) {
			_jc_type **typep;
			int i;

			typep = loader_types + loader->defined_types.size;
			for (i = _JC_TYPE_BOOLEAN; i <= _JC_TYPE_VOID; i++)
				*typep++ = vm->boot.types.prim[i];
			_JC_ASSERT(typep - loader_types == num_loader_types);
		}

		/* Generate list of explicit references from all loaded types */
		num_erefs = _jc_get_explicit_refs(env,
		    loader_types, num_loader_types, NULL);
		if ((erefs = _jc_uni_alloc(env, &uni, 
		      num_erefs * sizeof(*erefs))) == NULL) {
			_jc_vm_free(&loader_types);
			goto fail;
		}
		_jc_get_explicit_refs(env,
		    loader_types, num_loader_types, erefs);

		/* Push loader's explicit references onto the stack */
		if (num_erefs > 0) {
			if (_jc_gc_push_refs(env, &uni,
			    &frame, erefs, num_erefs) != JNI_OK) {
				_jc_vm_free(&loader_types);
				goto fail;
			}
		}

		/* Push loader's implicit references onto the stack */
		if (loader->num_implicit_refs > 0) {
			if (_jc_gc_push_refs(env, &uni, &frame,
			      loader->implicit_refs, loader->num_implicit_refs)
			    != JNI_OK) {
				_jc_vm_free(&loader_types);
				goto fail;
			}
		}

		/* Free loader types list */
		_jc_vm_free(&loader_types);

		/* If there was another loader to handle, do it now */
		if (other_loader != NULL) {
			loader = other_loader;
			other_loader = NULL;
			goto do_class_loader;
		}
	    }
	}

	/* Done */
	_jc_uni_alloc_free(&uni);
	return JNI_OK;

fail:
	/* Failed */
	_jc_uni_alloc_free(&uni);
	return JNI_ERR;
}

/*
 * Generate the list of all explicit references from Class objects
 * associated with a class loader. Skip null references so the list
 * is as small as possible.
 *
 * Returns the length of the list.
 */
static int
_jc_get_explicit_refs(_jc_env *env, _jc_type **types,
	int ntypes, _jc_object **list)
{
	_jc_jvm *const vm = env->vm;
	int num_class_fields;
	int vmdata_index;
	int len = 0;
	int i;
	int j;

	/* Get index of Class.vmdata so we can ignore it */
	vmdata_index = vm->boot.fields.Class.vmdata->offset
	    / (int)sizeof(_jc_word);

	/* Get number of reference fields in a java.lang.Class object */
	num_class_fields = vm->boot.types.Class->u.nonarray.num_virtual_refs;

	/* Add explicit references from each type */
	for (i = 0; i < ntypes; i++) {
		_jc_type *const type = types[i];

		/* Add references from this Class object's instance fields */
		for (j = -num_class_fields; j < 0; j++) {
			_jc_object *const ref
			    = ((_jc_object **)type->instance)[j];

			/* Ignore null pointers */
			if (ref == NULL)
				continue;

			/* Ignore Class.vmdata which is a native pointer! */
			if (j == vmdata_index)
				continue;

			/* Add reference to the list */
			if (list != NULL)
				list[len] = ref;
			len++;
		}

		/* Arrays don't have static fields */
		if (_JC_FLG_TEST(type, ARRAY))
			continue;

		/*
		 * Add references from this class' static fields. Note that
		 * we are relying here on the fact that the reference fields
		 * appear first in the 'class_fields' structure, and static
		 * reference fields appear first in a type's field list.
		 */
		for (j = 0; j < type->u.nonarray.num_fields; j++) {
			_jc_field *const field = type->u.nonarray.fields[j];
			_jc_object *ref;

			/* Have we done all the static reference fields? */
			if (!_JC_ACC_TEST(field, STATIC)
			    || _jc_sig_types[(u_char)*field->signature]
			      != _JC_TYPE_REFERENCE)
			      	break;

			/* Scan reference */
			ref = ((_jc_object **)type->u.nonarray.class_fields)[j];
			if (ref == NULL)
				continue;
			if (list != NULL)
				list[len] = ref;
			len++;
		}
	}

	/* Done */
	return len;
}

