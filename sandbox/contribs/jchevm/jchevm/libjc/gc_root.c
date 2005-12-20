
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

/* Internal definitions */
#define _JC_NUM_REGISTER_OFFS						\
	(sizeof(_jc_register_offs) / sizeof(*_jc_register_offs))

/* Internal functions */
static _jc_object	*_jc_locate_object(_jc_jvm *vm,
				const _jc_word *info, const void *ptr);
static int		_jc_root_walk_thread(_jc_env *thread,
				const _jc_word *info, _jc_object ***refsp);
static int		_jc_root_walk_native_refs(_jc_native_frame_list *list,
				_jc_object ***refsp);
static int		_jc_scan_c_stack(_jc_jvm *vm, _jc_c_stack *cstack,
				const _jc_word *info, _jc_object ***refsp);

/* Internal variables */
static const int	_jc_register_offs[] = _JC_REGISTER_OFFS;

/*
 * Find the head of the object given a pointer into its interior.
 * This is used for conservative GC scan.
 */
static _jc_object *
_jc_locate_object(_jc_jvm *vm, const _jc_word *info, const void *ptr)
{
	_jc_heap *const heap = &vm->heap;
	_jc_word word;
	char *page;
	int pnum;

	/* Does pointer point into the heap? */
	if (!_JC_IN_HEAP(heap, ptr)) {
		_jc_class_loader *loader;

		/* Handle a common case */
		if (ptr == NULL)
			return NULL;

		/* Check each class loader (except boot loader) */
		LIST_FOREACH(loader, &vm->class_loaders, link) {
			if (loader == vm->boot.loader)
				continue;
			if (_jc_uni_contains(&loader->uni, ptr))
				return loader->instance;
		}

		/* Done */
		return NULL;
	}

	/* Get page number */
	pnum = ((char *)ptr - (char *)heap->pages) / _JC_PAGE_SIZE;
	_JC_ASSERT(pnum >= 0 && pnum < heap->num_pages);

	/* If page is an interior page, find the first page */
	while ((info[pnum / _JC_BITS_PER_WORD]
	    & (1 << (pnum % _JC_BITS_PER_WORD))) != 0)
		pnum--;
	page = (char *)heap->pages + pnum * _JC_PAGE_SIZE;

	/* Examine page type to find start of object memory block */
	word = *((_jc_word *)page);
	switch (_JC_HEAP_EXTRACT(word, PTYPE)) {
	case _JC_HEAP_PAGE_FREE:
		return NULL;
	case _JC_HEAP_PAGE_SMALL:
	    {
		char *const first_block = page + _JC_HEAP_BLOCK_OFFSET;
		_jc_heap_size *size;
		int block_num;

		/* Get block size in this page */
		size = &heap->sizes[_JC_HEAP_EXTRACT(word, BSI)];

		/* Find block into which pointer is pointing */
		block_num = ((char *)ptr - first_block) / size->size;
		if (block_num < 0 || block_num >= size->num_blocks)
			return NULL;

		/* Point to start of block */
		ptr = first_block + block_num * size->size;

		/* See if there really is an object in there */
		word = *((_jc_word *)ptr);
		_JC_ASSERT(word != _JC_HEAP_BLOCK_ALLOC);
		if (word == _JC_HEAP_BLOCK_FREE)
			return NULL;
		break;
	    }
	case _JC_HEAP_PAGE_LARGE:
		ptr = page + _JC_HEAP_BLOCK_OFFSET;
		break;
	default:
		_JC_ASSERT(0);
	}

	/*
	 * If we could assume all references really pointed to the
	 * head of the object, then we could optimize further here.
	 */

	/* Find object head */
	return _jc_find_object_head(ptr);
}

/*
 * Compute the root set of references. Returns the number of references,
 * or -1 if there was an error. The caller must free the returned array.
 * Ths stack of the current thread must be clipped.
 *
 * If unsuccessful an exception is stored.
 *
 * NOTE: This assumes the VM mutex is locked and the world is stopped.
 */
int
_jc_root_walk(_jc_env *env, _jc_object ***refsp)
{
	const int nrefs_slop = 32;
	_jc_jvm *const vm = env->vm;
	_jc_heap *const heap = &vm->heap;
	_jc_object **refs_start = NULL;
	_jc_object **refs = NULL;
	_jc_env *thread;
	_jc_word *info;
	int max_nrefs = 0;
	int nrefs = 0;
	int pnum;

	/* Sanity check */
	_JC_MUTEX_ASSERT(env, vm->mutex);
	_JC_ASSERT(vm->world_stopped);

	/* Allocate heap page info bit array */
	if ((info = _jc_vm_zalloc(env, sizeof(_jc_word)
	    * _JC_HOWMANY(heap->num_pages, _JC_BITS_PER_WORD))) == NULL)
		return -1;

	/* Mark heap pages which are interior pages of a large object */
	for (pnum = 0; pnum < heap->num_pages; ) {
		_jc_word word;

		word = *((_jc_word *)((char *)heap->pages
		    + pnum * _JC_PAGE_SIZE));
		switch (_JC_HEAP_EXTRACT(word, PTYPE)) {
		case _JC_HEAP_PAGE_ALLOC:
			_JC_ASSERT(0);
		case _JC_HEAP_PAGE_FREE:
		case _JC_HEAP_PAGE_SMALL:
			pnum++;
			break;
		case _JC_HEAP_PAGE_LARGE:
		    {
			const int npages = _JC_HEAP_EXTRACT(word, NPAGES);
			int i;

			for (i = pnum + 1; i < pnum + npages; i++) {
				info[i / _JC_BITS_PER_WORD]
				    |= 1 << (i % _JC_BITS_PER_WORD);
			}
			pnum += npages;
			break;
		    }
		}
		_JC_ASSERT(pnum <= heap->num_pages);
	}

again:
	/* First count the references, then allocate array and record them */
	nrefs = 0;

	/* Walk threads */
	LIST_FOREACH(thread, &vm->threads.alive_list, link)
		nrefs += _jc_root_walk_thread(thread, info, &refs);

	/* Walk global native references */
	nrefs += _jc_root_walk_native_refs(&vm->native_globals, &refs);

	/*
	 * After counting references, allocate array and walk them again.
	 * Because the current thread's stack may change, allow some slop.
	 */
	if (refs == NULL) {
		max_nrefs = nrefs + nrefs_slop;
		if ((refs = _jc_vm_alloc(env,
		    max_nrefs * sizeof(*refs))) == NULL) {
			nrefs = -1;
			goto done;
		}
		refs_start = refs;
		goto again;
	}

	/* Sanity check we didn't somehow get a bunch more refs */
	_JC_ASSERT(nrefs <= max_nrefs);

done:
	/* Done */
	_jc_vm_free(&info);
	*refsp = refs_start;
	return nrefs;
}

/*
 * Walk all objects in one thread's root set. The thread's Java stack
 * must already be clipped.
 *
 * NOTE: This assumes caller has acquired the VM mutex.
 */
static int
_jc_root_walk_thread(_jc_env *thread, const _jc_word *info, _jc_object ***refsp)
{
	_jc_jvm *const vm = thread->vm;
	_jc_object **refs = *refsp;
	_jc_java_stack *jstack;
	_jc_c_stack *cstack;
	_jc_word *sp;
	int count = 0;

	/* Scan each contiguous C stack chunk */
	for (cstack = thread->c_stack; cstack != NULL; cstack = cstack->next) {

		/* Sanity check */
		_JC_ASSERT(cstack->clipped);

		/* Scan references */
		count += _jc_scan_c_stack(vm, cstack, info, &refs);
	}

	/* Scan the Java stack */
	_JC_ASSERT(thread->sp >= thread->stack_data
	    && thread->sp <= thread->stack_data + vm->java_stack_size);
	for (sp = thread->stack_data; sp < thread->sp; sp++) {
		_jc_object *const ref = (_jc_object *)*sp;
		_jc_object *obj;

		/* Find object pointed to, if any */
		if ((obj = _jc_locate_object(vm, info, ref)) == NULL)
			continue;

		/* Add object to list */
		if (refs != NULL)
			*refs++ = obj;
		count++;
	}

	/* Get implicit references from Java methods to their classes */
	for (jstack = thread->java_stack;
	    jstack != NULL; jstack = jstack->next) {
		if (refs != NULL)
			*refs++ = jstack->method->class->instance;
		count++;
	}

	/* Walk thread's local native references */
	count += _jc_root_walk_native_refs(&thread->native_locals, &refs);

	/* Walk thread's Thread instance (if any) */
	if (thread->instance != NULL) {
		if (refs != NULL)
			*refs++ = thread->instance;
		count++;
	}

	/* Walk thread's pending exception (if any) */
	if (thread->pending != NULL) {
		if (refs != NULL)
			*refs++ = thread->pending;
		count++;
	}

	/* Walk any exception posted in this thread by another thread */
	if (thread->cross_exception != NULL) {
		if (refs != NULL)
			*refs++ = thread->cross_exception;
		count++;
	}

	/* Done */
	*refsp = refs;
	return count;
}

/*
 * Scan a contiguous executable stack chunk.
 */
static int
_jc_scan_c_stack(_jc_jvm *vm, _jc_c_stack *cstack,
	const _jc_word *info, _jc_object ***refsp)
{
	const size_t stack_step = (_JC_STACK_ALIGN < sizeof(void *)) ?
	    _JC_STACK_ALIGN : sizeof(void *);
	_jc_object **refs = *refsp;
	const char *stack_bot = NULL;
	const char *stack_top = NULL;
	_jc_object *obj;
	const char *ptr;
	int count = 0;
	int regnum;

	/* Get references from saved registers */
	for (regnum = 0; regnum < _JC_NUM_REGISTER_OFFS; regnum++) {

		/* Find object pointed to by register, if any */
		if ((obj = _jc_locate_object(vm, info,
		    *(_jc_word **)((char *)&cstack->regs
		      + _jc_register_offs[regnum]))) == NULL)
			continue;

		/* Add object to list */
		if (refs != NULL)
			*refs++ = obj;
		count++;
	}

	/*
	 * Find the bottom of this Java stack segment.
	 *
	 * Note: we assume 'cstack' is allocated at the bottom!
	 */
	stack_bot = (const char *)cstack;

	/* Find the top of this Java stack segment */
	stack_top = _jc_mcontext_sp(&cstack->regs);

	/* Sanity check stack alignment */
	_JC_ASSERT(((_jc_word)stack_top & (_JC_STACK_ALIGN - 1)) == 0);
	_JC_ASSERT(((_jc_word)stack_bot & (_JC_STACK_ALIGN - 1)) == 0);

	/* Conservatively find references in this Java stack segment */
#if _JC_DOWNWARD_STACK
	for (ptr = stack_top; ptr < stack_bot; ptr += stack_step)
#else
	for (ptr = stack_bot; ptr < stack_top; ptr += stack_step)
#endif
	{
		/* Find object pointed into, if any */
		if ((obj = _jc_locate_object(vm,
		    info, *(_jc_word **)ptr)) == NULL)
			continue;

		/* Add object to list */
		if (refs != NULL)
			*refs++ = obj;
		count++;
	}

	/* Done */
	*refsp = refs;
	return count;
}

/*
 * Walk all objects in a native reference list.
 */
static int
_jc_root_walk_native_refs(_jc_native_frame_list *list, _jc_object ***refsp)
{
	_jc_object **refs = *refsp;
	_jc_native_frame *frame;
	int count = 0;
	int i;

	/* Iterate through each frame */
	SLIST_FOREACH(frame, list, link) {

		/* Skip empty frames */
		if (!_JC_NATIVE_REF_ANY_USED(frame))
			continue;

		/* Iterate references in this frame */
		for (i = 0; i < _JC_NATIVE_REFS_PER_FRAME; i++) {
			if (!_JC_NATIVE_REF_IS_FREE(frame, i)) {
				_jc_object *const obj = frame->refs[i];

				if (obj != NULL) {
					if (refs != NULL)
						*refs++ = obj;
					count++;
				}
			}
		}
	}

	/* Done */
	*refsp = refs;
	return count;
}

