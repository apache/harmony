
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

/* Number of GC cycles to perform before giving up */
#define _JC_HEAP_MAX_GC_CYCLES		3

/* Internal functions */
static int	_jc_heap_gen_block_sizes(_jc_jvm *vm);
static int	_jc_heap_roundup_block_size(_jc_jvm *vm, int size, int *nbp);

/*
 * Initialize a VM's heap.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_heap_init(_jc_env *env, _jc_jvm *vm)
{
	_jc_heap *const heap = &vm->heap;

	/* Round up heap size to page size */
	heap->size = _JC_ROUNDUP2(heap->size, _JC_PAGE_SIZE);

	/* Allocate heap */
	if ((heap->mem = _jc_vm_alloc(env, heap->size)) == NULL) {
		_JC_EX_STORE(env, OutOfMemoryError,
		    "can't allocate %lu byte heap", (unsigned long)heap->size);
		goto fail;
	}

	/* Find an array of (page aligned) pages in there */
	heap->pages = (void *)_JC_ROUNDUP2((_jc_word)heap->mem, _JC_PAGE_SIZE);
	heap->max_pages = (heap->size - ((char *)heap->pages
	    - (char *)heap->mem)) / _JC_PAGE_SIZE;
	heap->num_pages = 0;

	/* Generate the list of available block sizes */
	heap->num_sizes = _jc_heap_gen_block_sizes(vm);
	if ((heap->sizes = _jc_vm_alloc(env,
	    heap->num_sizes * sizeof(*heap->sizes))) == NULL)
		goto fail;
	_jc_heap_gen_block_sizes(vm);

	/* Sanity check values */
	if (heap->max_pages >= _JC_HEAP_MAX(NEXT)) {
		_JC_EX_STORE(env, OutOfMemoryError,
		    "heap is too large: it can't have more than %d pages",
		    _JC_HEAP_MAX(NPAGES) - 1);
		goto fail;
	}
	if (heap->num_sizes > _JC_HEAP_MAX(BSI)) {
		_JC_EX_STORE(env, OutOfMemoryError,
		    "heap granularity is too small: heap can't have more"
		    " than %d different block sizes", _JC_HEAP_MAX(BSI) - 1);
		goto fail;
	}

	/* Done */
	return JNI_OK;

fail:
	/* Clean up after failure */
	_jc_vm_free(&heap->mem);
	_jc_vm_free(&heap->sizes);
	memset(heap, 0, sizeof(*heap));
	return JNI_ERR;
}

/*
 * Free a VM's heap.
 */
void
_jc_heap_destroy(_jc_jvm *vm)
{
	_jc_heap *const heap = &vm->heap;

	_jc_vm_free(&heap->mem);
	_jc_vm_free(&heap->sizes);
	memset(heap, 0, sizeof(*heap));
}

/*
 * Allocate one or more pages from the heap.
 */
void *
_jc_heap_alloc_pages(_jc_env *env, int npages)
{
	_jc_jvm *const vm = env->vm;
	_jc_heap *const heap = &vm->heap;
	int reserved = _JC_HEAP_RESERVED_PAGES;
	int num_cycles = 0;
	_jc_word *ptr;
	int i;
	int j;

	/* Sanity check */
	_JC_ASSERT(npages < _JC_HEAP_MAX(NPAGES));

try_again:
	/* Start looking at where we left off last */
	i = heap->next_page;

	/* Leave some special reserved memory for posting OutOfMemoryError's */
	if (env->out_of_memory)
		reserved = 0;

check_avail:
	/* Check for out of memory */
	if (i + npages > heap->num_pages - reserved) {

		/* Initialize pages the first time through the heap */
		if (heap->num_pages != heap->max_pages) {
			int increase;
			int i;

			_JC_MUTEX_LOCK(env, vm->mutex);
			increase = _JC_HEAP_INIT_PAGES(heap);
			if (heap->num_pages + increase > heap->max_pages)
				increase = heap->max_pages - heap->num_pages;
			for (i = 0; i < increase; i++) {
				*_JC_PAGE_ADDR(heap->pages,
				    heap->num_pages + i) = 0;
			}
			heap->num_pages += increase;
			_JC_MUTEX_UNLOCK(env, vm->mutex);
			goto check_avail;
		}

		/* Dip into reserve memory when really out of memory */
		if (num_cycles == _JC_HEAP_MAX_GC_CYCLES) {
			env->out_of_memory = 1;
			_jc_post_exception(env, _JC_OutOfMemoryError);
			return NULL;
		}

		/* Do a GC cycle */
		if (_jc_gc(env, num_cycles > 0) != JNI_OK) {
			_jc_post_exception_info(env);
			return NULL;
		}
		num_cycles++;

		/* After second try, yield so the finalizer thread can run */
		if (num_cycles > 1)
			sched_yield();

		/* Try again */
		goto try_again;
	}

	/* Point at first available page */
	ptr = _JC_PAGE_ADDR(heap->pages, i);

	/* Try to allocate 'npages' contiguous pages */
	for (j = 0; j < npages; j++) {
		volatile _jc_word *const page_info = _JC_PAGE_ADDR(ptr, j);
		_jc_word word;

		/* Try to quickly grab the next page */
		if (_jc_compare_and_swap(page_info, _JC_HEAP_PAGE_FREE,
		    (_JC_HEAP_PAGE_ALLOC << _JC_HEAP_PTYPE_SHIFT)
		      | ((npages - j) << _JC_HEAP_NPAGES_SHIFT)))
			continue;

		/* Page was not free; skip over it and any subsequent pages */
		word = *page_info;
		switch (_JC_HEAP_EXTRACT(word, PTYPE)) {
		case _JC_HEAP_PAGE_FREE:
			break;
		case _JC_HEAP_PAGE_SMALL:
			i += j + 1;
			break;
		case _JC_HEAP_PAGE_LARGE:
		case _JC_HEAP_PAGE_ALLOC:
			i += j + _JC_HEAP_EXTRACT(word, NPAGES);
			break;
		}

		/* Un-do our partial reservation */
		while (j-- > 0)
			*_JC_PAGE_ADDR(ptr, j) = _JC_HEAP_PAGE_FREE;

		/* Try again */
		goto check_avail;
	}

	/*
	 * Reset the next free page pointer. Note there is a race
	 * condition here which may lead to some free pages remaining
	 * unallocatable until the next garbage collection.
	 */
	heap->next_page = i + j;

	/* Done */
	return ptr;
}

/*
 * Allocate a small block of memory with size index 'bsi' from the heap.
 *
 * The caller is responsible for making the memory look like
 * an object and maintaining an (indirect) reference to it.
 */
void *
_jc_heap_alloc_small_block(_jc_env *env, int bsi)
{
	_jc_jvm *const vm = env->vm;
	_jc_heap *const heap = &vm->heap;
	_jc_heap_size *const bs = &heap->sizes[bsi];
	volatile _jc_word *ptr;
	jboolean gotit;
	_jc_word *next;
	int i;

	/* Sanity check */
	_JC_ASSERT(bsi >= 0 && bsi < heap->num_sizes);
	_JC_ASSERT(bs->hint == NULL
	    || (((_jc_word)bs->hint & (_JC_PAGE_SIZE - 1)) % bs->size)
	      == _JC_HEAP_BLOCK_OFFSET);
	_JC_ASSERT(_JC_HEAP_SAME_PAGE(bs->hint,
	    (char *)bs->hint + bs->size - 1));

try_again:
	/* Do we have a hint for the next free block to try? */
	if ((ptr = bs->hint) == NULL)
		goto need_hint;

	/* Try to grab hinted at block */
	gotit = _jc_compare_and_swap(ptr,
	    _JC_HEAP_BLOCK_FREE, _JC_HEAP_BLOCK_ALLOC);

	/* Update hint to point to the next block in the page (if any) */
	next = (_jc_word *)((char *)ptr + bs->size);
	bs->hint = _JC_HEAP_SAME_PAGE(ptr, (char *)next + bs->size - 1) ?
	    next : NULL;

	/* Return the block we got if we got it */
	if (gotit)
		return (void *)ptr;

	/* Try again */
	goto try_again;

need_hint:
	/* Does this blocksize have any pages on its "use first" list? */
	if ((ptr = bs->pages) == NULL)
		goto get_page;

	/* Sanity check */
	_JC_ASSERT(((_jc_word)ptr & (_JC_PAGE_SIZE - 1)) == 0);
	_JC_ASSERT(_JC_HEAP_EXTRACT(*ptr, PTYPE) == _JC_HEAP_PAGE_SMALL);
	_JC_ASSERT(_JC_HEAP_EXTRACT(*ptr, BSI) == bsi);
	_JC_ASSERT(_JC_HEAP_EXTRACT(*ptr, NEXT) < heap->num_pages
	    || _JC_HEAP_EXTRACT(*ptr, NEXT) == _JC_HEAP_MAX(NEXT) - 1);

	/* Get the page following the first page in the list, if any */
	next = (i = _JC_HEAP_EXTRACT(*ptr, NEXT)) == _JC_HEAP_MAX(NEXT) - 1 ?
	    NULL : _JC_PAGE_ADDR(heap->pages, i);
	_JC_ASSERT(next == NULL || _JC_IN_HEAP(heap, next));

	/*
	 * Pop page off the page list; if we lose the race, another thread
	 * popped it off first. At worst, page will remain as is until
	 * the next GC cycle.
	 */
	if (_jc_compare_and_swap((_jc_word *)&bs->pages,
	    (_jc_word)ptr, (_jc_word)next))
		bs->hint = (_jc_word *)((char *)ptr + _JC_HEAP_BLOCK_OFFSET);
	goto try_again;

get_page:
	/* Allocate a new small page for this blocksize */
	if ((ptr = _jc_heap_alloc_pages(env, 1)) == NULL)
		return NULL;
	_JC_ASSERT(_JC_HEAP_EXTRACT(*ptr, PTYPE) == _JC_HEAP_PAGE_ALLOC);

	/* Initialize page and allocate first block for ourselves */
	ptr = (_jc_word *)((char *)ptr + _JC_HEAP_BLOCK_OFFSET);
	*ptr = _JC_HEAP_BLOCK_ALLOC;

	/* Mark all subsquent blocks as free */
	for (i = bs->num_blocks - 1; i > 0; i--) {
		ptr = (_jc_word *)((char *)ptr + bs->size);
		*ptr = _JC_HEAP_BLOCK_FREE;
	}

	/* Now mark the page as small and available */
	ptr = (_jc_word *)((_jc_word)ptr & ~(_JC_PAGE_SIZE - 1));
	*ptr = (_JC_HEAP_PAGE_SMALL << _JC_HEAP_PTYPE_SHIFT)
	    | (bsi << _JC_HEAP_BSI_SHIFT)
	    | _JC_HEAP_NEXT_MASK;		/* invalid value not used */

	/* Point the hint at the second block in the page */
	ptr = (_jc_word *)((char *)ptr + _JC_HEAP_BLOCK_OFFSET);
	bs->hint = (_jc_word *)((char *)ptr + bs->size);

	/* Return pointer to the first block */
	return (void *)ptr;
}

/*
 * Generate the list of block sizes.
 *
 * Return the number of sizes.
 */
static int
_jc_heap_gen_block_sizes(_jc_jvm *vm)
{
	_jc_heap *const heap = &vm->heap;
	int nblocks;
	int size;
	int i;

	for (i = 0, size = sizeof(_jc_object); ; i++) {
		_jc_heap_size *const bs = &heap->sizes[i];
		int new_size;

		/* Get rounded up size */
		if ((size = _jc_heap_roundup_block_size(vm,
		    size, &nblocks)) == -1)
			break;

		/* Initialize this block size descriptor */
		if (heap->sizes != NULL) {
			memset(bs, 0, sizeof(*bs));
			bs->size = size;
			bs->num_blocks = nblocks;
		}

		/* Compute the next bigger size */
		new_size = (size * (200 - heap->granularity)) / 100;
		if (new_size - size < _JC_FULL_ALIGNMENT)
			new_size = size + _JC_FULL_ALIGNMENT;
		size = new_size;
	}
	return i;
}

/*
 * Round up a block size to the largest possible valid size such
 * that we still get the same number of blocks out of a single page.
 */
static int
_jc_heap_roundup_block_size(_jc_jvm *vm, int size, int *nbp)
{
	const int psize = _JC_HOWMANY(_JC_PAGE_SIZE, _JC_FULL_ALIGNMENT);
	const int hdrsize = _JC_HOWMANY(sizeof(_jc_word), _JC_FULL_ALIGNMENT);
	int nblocks;

	/* Do math in multples of _JC_FULL_ALIGNMENT */
	size = _JC_HOWMANY(size, _JC_FULL_ALIGNMENT);

	/* How many blocks can we get? */
	nblocks = (psize - hdrsize) / size;

	/* Check for overflow */
	if (nblocks <= 1)
		return -1;

	/* Increase block size until 'nblocks' blocks will no longer fit */
	while (hdrsize + (nblocks * (size + 1)) <= psize)
		size++;

	/* Done */
	*nbp = nblocks;
	return size * _JC_FULL_ALIGNMENT;
}

/*
 * Map a size into the appropriate block size index.
 *
 * Returns the block size index, or -N if the size is big enough
 * to require one or more whole pages where N is the number of pages.
 */
int
_jc_heap_block_size(_jc_jvm *vm, size_t size)
{
	_jc_heap *const heap = &vm->heap;
	int bsi;
	int lim;

	/* Sanity check */
	_JC_ASSERT(size > 0);

	/* Check whether size require large page(s) */
	if (size > heap->sizes[heap->num_sizes - 1].size) {
		return -_JC_HOWMANY(size + _JC_HEAP_BLOCK_OFFSET,
		    _JC_PAGE_SIZE);
	}

	/* Determine which block size to use with binary search */
	for (bsi = 0, lim = heap->num_sizes; lim != 0; lim >>= 1) {
		const int j = bsi + (lim >> 1);

		if (size > heap->sizes[j].size) {
			bsi = j + 1;
			lim--;
		} else if (j == 0 || size > heap->sizes[j - 1].size) {
			bsi = j;
			break;
		}
	}

	/* Return block size index */
	return bsi;
}

#ifndef NDEBUG

/* Internal functions */
static void	_jc_heap_check_block(_jc_jvm *vm, _jc_word *block,
			char *page, int bsi);
static void	_jc_heap_check_alloc(_jc_jvm *vm, _jc_object *obj);

/*
 * Sanity check the heap. This should be called with the world halted.
 */
void
_jc_heap_check(_jc_jvm *vm)
{
	_jc_class_loader *loader;
	_jc_heap *const heap = &vm->heap;
	int i;

	/* World must be halted */
	_JC_ASSERT(vm->world_stopped);

	/* Check each page */
	for (i = 0; i < heap->num_pages; i++) {
		char *const page = (char *)heap->pages + i * _JC_PAGE_SIZE;
		_jc_word word = *((_jc_word *)page);

		/* Check page depending on type */
		switch (_JC_HEAP_EXTRACT(word, PTYPE)) {
		case _JC_HEAP_PAGE_FREE:
			break;
		case _JC_HEAP_PAGE_SMALL:
		    {
			const int bsi = _JC_HEAP_EXTRACT(word, BSI);
			_jc_heap_size *const size = &heap->sizes[bsi];
			char *const blocks = page + _JC_HEAP_BLOCK_OFFSET;
			int num_free = 0;
			int j;

			/* Check heap block size */
			_JC_ASSERT(bsi >= 0 && bsi < heap->num_sizes);

			/* Check all allocated blocks in this page */
			for (j = 0; j < size->num_blocks; j++) {
				_jc_word *const block
				    = (_jc_word *)(blocks + j * size->size);

				switch (*block) {
				case _JC_HEAP_BLOCK_FREE:
					num_free++;
					break;
				case _JC_HEAP_BLOCK_ALLOC:
					_JC_ASSERT(JNI_FALSE);
				default:
					_jc_heap_check_block(vm,
					    block, page, bsi);
					break;
				}
			}

			/* Impossible for all blocks to be free */
			_JC_ASSERT(num_free < size->num_blocks);
			break;
		    }
		case _JC_HEAP_PAGE_LARGE:
		    {
			const int npages = _JC_HEAP_EXTRACT(word, NPAGES);
			_jc_word *const block
			    = (_jc_word *)(page + _JC_HEAP_BLOCK_OFFSET);

			_JC_ASSERT(npages > 0 && i + npages <= heap->num_pages);
			_jc_heap_check_block(vm, block, page, -npages);
			i += npages - 1;
			break;
		    }
		case _JC_HEAP_PAGE_ALLOC:
			_JC_ASSERT(JNI_FALSE);
		}
	}

	/* Check small page "use first" lists */
	for (i = 0; i < heap->num_sizes; i++) {
		_jc_heap_size *const size = &heap->sizes[i];
		_jc_word *page;
		int j;

		for (j = 0, page = (_jc_word *)size->pages; page != NULL; j++) {
			_jc_word word = *page;
			int next;

			/* Sanity check loops */
			_JC_ASSERT(j <= heap->num_pages);

			/* Sanity check page type */
			_JC_ASSERT(_JC_HEAP_EXTRACT(word, PTYPE)
			    == _JC_HEAP_PAGE_SMALL);
			_JC_ASSERT(_JC_HEAP_EXTRACT(word, BSI) == i);

			/* Go to next page on list */
			if ((next = _JC_HEAP_EXTRACT(word, NEXT))
			      == _JC_HEAP_MAX(NEXT) - 1)
				page = NULL;
			else
				page = _JC_PAGE_ADDR(heap->pages, next);
		}
	}

	/* Check objects pointed to by class loader memory */
	LIST_FOREACH(loader, &vm->class_loaders, link) {
		for (i = 0; i < loader->num_implicit_refs; i++) {
			_jc_object *const obj = loader->implicit_refs[i];

			_jc_heap_check_object(vm, obj, 1);
		}
	}
}

/*
 * Sanity check object belongs where it is in the heap.
 */
static void
_jc_heap_check_alloc(_jc_jvm *vm, _jc_object *obj)
{
	_jc_heap *const heap = &vm->heap;
	_jc_type *const type = obj->type;
	_jc_word *block_start;
	_jc_word *obj_start;
	int object_size;
	int block_size;
	_jc_word pginfo;
	char *page;
	int ptype;
	int bsi;

	/* Get object size */
	if (_JC_LW_TEST(obj->lockword, ARRAY)) {
		const int elem_type = _JC_LW_EXTRACT(obj->lockword, TYPE);
		_jc_array *const array = (_jc_array *)obj;

		object_size = _jc_array_head_sizes[elem_type]
		    + array->length * _jc_type_sizes[elem_type];
	} else
		object_size = type->u.nonarray.instance_size;

	/* Find object start */
	obj_start = ((_jc_word *)obj) - _jc_num_refs(obj);
	bsi = _jc_heap_block_size(vm, object_size);

	/* Get page info for page containing start of object */
	page = (char *)((_jc_word)obj_start & ~(_JC_PAGE_SIZE - 1));
	pginfo = *((_jc_word *)page);

	/* Check page type */
	ptype = _JC_HEAP_EXTRACT(pginfo, PTYPE);
	if (bsi < 0) {
		_JC_ASSERT(ptype == _JC_HEAP_PAGE_LARGE);
		_JC_ASSERT(_JC_HEAP_EXTRACT(pginfo, NPAGES) == -bsi);
		block_size = -bsi * _JC_PAGE_SIZE - _JC_HEAP_BLOCK_OFFSET;
	} else {
		_JC_ASSERT(ptype == _JC_HEAP_PAGE_SMALL);
		_JC_ASSERT(_JC_HEAP_EXTRACT(pginfo, BSI) == bsi);
		block_size = heap->sizes[bsi].size;
	}
	_JC_ASSERT(object_size <= block_size);

	/* Determine if there is a skip word */
	block_start = (block_size >= object_size + sizeof(_jc_word)
	      && _jc_num_refs(obj) >= _JC_SKIPWORD_MIN_REFS) ?
	    obj_start - 1 : obj_start;

	/* Check alignment of object start */
	if (bsi < 0) {
		_JC_ASSERT(((_jc_word)block_start & (_JC_PAGE_SIZE - 1))
		    == _JC_HEAP_BLOCK_OFFSET);
	} else {
		_JC_ASSERT((((_jc_word)block_start & (_JC_PAGE_SIZE - 1))
		    % block_size) == _JC_HEAP_BLOCK_OFFSET);
	}
}

/*
 * Sanity check one allocated heap block.
 */
static void
_jc_heap_check_block(_jc_jvm *vm, _jc_word *block, char *page, int bsi)
{
	_jc_heap *const heap = &vm->heap;
	_jc_heap_size *size;
	_jc_object *obj;
	_jc_word word;
	int block_size;
	int obj_size;
	int skip;

	/* Sanity check block contains valid object */
	word = *block;
	_JC_ASSERT(word != _JC_HEAP_BLOCK_FREE && word != _JC_HEAP_BLOCK_ALLOC);

	/* Check for possible skip word and find object header */
	if (_JC_HEAP_EXTRACT(word, BTYPE) == _JC_HEAP_BLOCK_SKIP) {
		skip = _JC_HEAP_EXTRACT(word, SKIP);
		_JC_ASSERT(skip >= 1 + _JC_SKIPWORD_MIN_REFS);
		obj = (_jc_object *)(block + skip);
	} else {
		skip = 0;
		while (!_JC_LW_TEST(*block, ODD))
			block++;
		obj = (_jc_object *)block;
	}

	/* Get size of this block */
	if (bsi < 0) {
		size = NULL;
		block_size = -bsi * _JC_PAGE_SIZE - _JC_HEAP_BLOCK_OFFSET;
	} else {
		size = &heap->sizes[bsi];
		block_size = size->size;
	}

	/* Sanity check object belongs in this type of block */
	if (!_JC_LW_TEST(obj->lockword, ARRAY)) {
		obj_size = obj->type->u.nonarray.instance_size;
		_JC_ASSERT(obj->type->u.nonarray.block_size_index == bsi);
	} else {
		const int elem_type = _JC_LW_EXTRACT(obj->lockword, TYPE);
		_jc_array *const array = (_jc_array *)obj;

		obj_size = _jc_array_head_sizes[elem_type]
		    + array->length * _jc_type_sizes[elem_type];
	}
	_JC_ASSERT(block_size >= (skip ? sizeof(_jc_word) : 0) + obj_size);
	if (bsi < 0) {
		_JC_ASSERT(obj_size + _JC_HEAP_BLOCK_OFFSET
		    >= (-bsi - 1) * _JC_PAGE_SIZE);
	} else if (bsi > 0)
		_JC_ASSERT(obj_size > heap->sizes[bsi - 1].size);

	/* Sanity check object itself */
	_jc_heap_check_object(vm, obj, 1);
}

/*
 * Sanity check one object.
 */
void
_jc_heap_check_object(_jc_jvm *vm, _jc_object *obj, int recurse)
{
	_jc_heap *const heap = &vm->heap;
	_jc_type *const type = obj->type;
	int i;

	/* Check allocation */
	if (_JC_IN_HEAP(heap, obj)) {
		_JC_ASSERT(type != vm->boot.types.Class);
		_jc_heap_check_alloc(vm, obj);
	} else {
		_JC_ASSERT(!_JC_LW_TEST(type->initial_lockword, FINALIZE));
		_JC_ASSERT(!_jc_subclass_of(obj, vm->boot.types.Reference));
	}

	/* Check lockword */
	_JC_ASSERT(_JC_LW_TEST(obj->lockword, ODD));
	_JC_ASSERT(_JC_LW_TEST(obj->lockword, ARRAY)
	    == _JC_FLG_TEST(type, ARRAY));
	switch (_JC_LW_EXTRACT(obj->lockword, TYPE)) {
	case _JC_TYPE_BOOLEAN:
	case _JC_TYPE_BYTE:
	case _JC_TYPE_CHAR:
	case _JC_TYPE_SHORT:
	case _JC_TYPE_INT:
	case _JC_TYPE_LONG:
	case _JC_TYPE_FLOAT:
	case _JC_TYPE_DOUBLE:
		_JC_ASSERT(_JC_LW_TEST(obj->lockword, ARRAY));
		break;
	case _JC_TYPE_REFERENCE:
		break;
	default:
		_JC_ASSERT(JNI_FALSE);
	}
	if (_JC_LW_TEST(obj->lockword, ARRAY)) {
		_JC_ASSERT((type->u.array.element_type->flags & _JC_TYPE_MASK)
		    == _JC_LW_EXTRACT(obj->lockword, TYPE));
		_JC_ASSERT(_jc_num_refs(obj) == ((_jc_array *)obj)->length
		    * (_JC_LW_EXTRACT(obj->lockword, TYPE)
		      == _JC_TYPE_REFERENCE));
	} else {
		_JC_ASSERT(_jc_num_refs(obj)
		    == obj->type->u.nonarray.num_virtual_refs);
	}

	/* Recurse (once) on reference fields */
	if (!recurse)
		return;
	for (i = -_jc_num_refs(obj); i < 0; i++) {
		_jc_object *const ref = ((_jc_object **)obj)[i];

		if (ref != NULL)
			_jc_heap_check_object(vm, ref, 0);
	}
}

#endif	/* !NDEBUG */
