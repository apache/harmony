
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
 * $Id: cl_alloc.c,v 1.6 2005/03/12 04:24:17 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * "Uni-alloc" memory allocator used for class loaders.
 *
 * Class loader memory blocks are assumed to be allocated once and never
 * reallocated or freed, until all memory associated with a class loader
 * is freed at once when the class loader is unloaded.
 *
 * As a special exception, we allow memory blocks to be freed in the
 * reverse order in which they were allocated, to support "undoing"
 * in the case of certain errors.
 *
 * Note also that class loader memory is always manipulated while the
 * loader's lock is held, so synchronization is not an issue here.
 *
 * We provide this same functionality for other users besides class
 * loaders using the "uni" functions.
 */

/* Internal functions */
static jint	_jc_uni_avail_alloc(_jc_uni_mem *uni, int num_pages);

/*
 * Initialize memory associated with a uni-allocator.
 */
void
_jc_uni_alloc_init(_jc_uni_mem *uni, int min_pages,
	volatile _jc_word *avail_pages)
{
	TAILQ_INIT(&uni->pages);
	uni->min_pages = min_pages;
	uni->avail_pages = avail_pages;
}

/*
 * Free all memory associated with a uni-allocator.
 */
void
_jc_uni_alloc_free(_jc_uni_mem *uni)
{
	int num_pages = 0;

	/* Free all pages of memory */
	while (!TAILQ_EMPTY(&uni->pages)) {
		_jc_uni_pages *pages = TAILQ_FIRST(&uni->pages);

		num_pages += pages->num_pages;
		TAILQ_REMOVE(&uni->pages, pages, link);
		_jc_vm_free(&pages);
	}

	/* Update available pages */
	_jc_uni_avail_alloc(uni, -num_pages);
}

/*
 * Extend memory in an uni-allocator.
 *
 * If unsuccessful an exception is stored.
 */
static jint
_jc_uni_ensure(_jc_env *env, _jc_uni_mem *uni, size_t size)
{
	jboolean reserved = JNI_FALSE;
	_jc_uni_pages *pages;
	size_t num_pages;
	int try;

	/* Get current page set */
	pages = TAILQ_LAST(&uni->pages, _jc_uni_page_list);

	/* Is there enough room? */
	if (!TAILQ_EMPTY(&uni->pages)
	    && size < (pages->num_pages * _JC_PAGE_SIZE) - pages->offset)
		return JNI_OK;

	/* Compute how many new pages to allocate */
	num_pages = _JC_HOWMANY(_JC_UNI_HDR_SIZE + size, _JC_PAGE_SIZE);
	if (num_pages < uni->min_pages)
		num_pages = uni->min_pages;

	/* Try hard to find memory */
	for (try = 1; JNI_TRUE; try++) {

		/* Try to reserve pages */
		if (_jc_uni_avail_alloc(uni, num_pages) == JNI_OK) {
			reserved = JNI_TRUE;
			break;
		}

		/* Give up after three tries */
		if (try == 3)
			break;

		/* Try a GC cycle to unload some classes */
		if (_jc_gc(env, JNI_TRUE) != JNI_OK)
			return JNI_ERR;

		/* Yield so the finalizer thread can run */
		sched_yield();
	}

	/* Did we successfully reserve? */
	if (!reserved) {
		_JC_EX_STORE(env, OutOfMemoryError,
		    "reached limit of %u pages allocated"
		    " by class loaders", (int)*uni->avail_pages);
		return JNI_ERR;
	}

	/* Get pages */
	pages = _jc_vm_alloc(env, num_pages * _JC_PAGE_SIZE);

	/* Un-reserve (for class loaders) on error */
	if (pages == NULL) {
		_jc_uni_avail_alloc(uni, -num_pages);
		return JNI_ERR;
	}

	/* Initialize new chunk */
	memset(pages, 0, sizeof(*pages));
	pages->num_pages = num_pages;
	pages->offset = _JC_UNI_HDR_SIZE;

	/* Link it into our list */
	TAILQ_INSERT_TAIL(&uni->pages, pages, link);

	/* Done */
	return JNI_OK;
}

/*
 * Allocate a chunk of memory from an uni-allocator's page list.
 * Grab more pages as necessary.
 *
 * If unsuccessful an exception is stored.
 */
void *
_jc_uni_alloc(_jc_env *env, _jc_uni_mem *uni, size_t size)
{
	_jc_uni_pages *pages;
	void *ptr;

	/* Stay aligned */
	size = _JC_ROUNDUP2(size, _JC_FULL_ALIGNMENT);

	/* Ensure we have room */
	if (_jc_uni_ensure(env, uni, size) != JNI_OK)
		return NULL;

	/* Grab some more memory */
	pages = TAILQ_LAST(&uni->pages, _jc_uni_page_list);
	ptr = (char *)pages + pages->offset;
	pages->offset += size;

	/* Done */
	return ptr;
}

/*
 * If the most recently allocated memory that was not subsequently
 * unallocated was address '*pp' with size 'size', then free it.
 * Otherwise, just leak it.
 *
 * In other words, you can free uni-allocated memory if freed in the
 * reverse order in which it was allocated.
 */
void
_jc_uni_unalloc(_jc_uni_mem *uni, void *pp, size_t size)
{
	void **const ptrp = pp;
	void *const ptr = *ptrp;
	_jc_uni_pages *pages;

	/* Sanity check */
	if (ptr == NULL)
		return;
	*ptrp = NULL;

	/* Can we un-do? */
	if (TAILQ_EMPTY(&uni->pages))
		return;
	pages = TAILQ_LAST(&uni->pages, _jc_uni_page_list);
	size = _JC_ROUNDUP2(size, _JC_FULL_ALIGNMENT);
	if ((char *)ptr + size != (char *)pages + pages->offset)
		return;

	/* Un-do the previous allocation */
	pages->offset -= size;

	/* Free the page if empty */
	if (pages->offset == _JC_UNI_HDR_SIZE) {
		TAILQ_REMOVE(&uni->pages, pages, link);
		_jc_uni_avail_alloc(uni, -pages->num_pages);
		_jc_vm_free(&pages);
	}
}

/*
 * Allocate and clear a chunk of memory from the uni-allocator memory.
 */
void *
_jc_uni_zalloc(_jc_env *env, _jc_uni_mem *uni, size_t size)
{
	void *mem;

	/* Alloc mem and zero it */
	if ((mem = _jc_uni_alloc(env, uni, size)) != NULL)
		memset(mem, 0, size);
	return mem;
}

/*
 * Copy a string into memory allocated from an uni-allocator.
 */
char *
_jc_uni_strdup(_jc_env *env, _jc_uni_mem *uni, const char *s)
{
	const size_t slen = strlen(s);
	void *mem;

	/* Alloc mem and copy string */
	if ((mem = _jc_uni_alloc(env, uni, slen + 1)) == NULL)
		return NULL;
	memcpy(mem, s, slen + 1);
	return mem;
}

/*
 * Determine if a pointer points into uni-allocator allocated memory.
 */
jboolean
_jc_uni_contains(_jc_uni_mem *uni, const void *ptr)
{
	_jc_uni_pages *pages;

	TAILQ_FOREACH(pages, &uni->pages, link) {
		const char *const start = (char *)pages + _JC_UNI_HDR_SIZE;
		const char *const end = (char *)pages
		    + (pages->num_pages * _JC_PAGE_SIZE);

		if ((char *)ptr >= start && (char *)ptr < end)
		    	return JNI_TRUE;
	}
	return JNI_FALSE;
}

/*
 * Atomically update available pages, but don't let them go negative.
 */
static jint
_jc_uni_avail_alloc(_jc_uni_mem *uni, int num_pages)
{
	_jc_word old_avail_pages;

	if (uni->avail_pages == NULL)
		return JNI_OK;
	do {
		old_avail_pages = *uni->avail_pages;
		if (num_pages > 0 && num_pages > old_avail_pages)
			return JNI_ERR;
	} while (!_jc_compare_and_swap(uni->avail_pages,
	    old_avail_pages, old_avail_pages - num_pages));
	return JNI_OK;
}

/*
 * Mark the current top of the allocation stack.
 */
void *
_jc_uni_mark(_jc_uni_mem *uni)
{
	_jc_uni_pages *pages;

	pages = TAILQ_LAST(&uni->pages, _jc_uni_page_list);
	return pages != NULL ? (char *)pages + pages->offset : NULL;
}

/*
 * Reset the current top of the allocation stack to 'mem'.
 */
void
_jc_uni_reset(_jc_uni_mem *uni, void *mem)
{
	_jc_uni_pages *pages;

	if (mem == NULL) {
		_jc_uni_alloc_free(uni);
		return;
	}
	while (JNI_TRUE) {
		_JC_ASSERT(!TAILQ_EMPTY(&uni->pages));
		pages = TAILQ_LAST(&uni->pages, _jc_uni_page_list);
		if ((char *)mem >= (char *)pages + _JC_UNI_HDR_SIZE
		    && (char *)mem < (char *)pages
		      + pages->num_pages * _JC_PAGE_SIZE) {
			pages->offset = (char *)mem - (char *)pages;
			break;
		}
		TAILQ_REMOVE(&uni->pages, pages, link);
		_jc_uni_avail_alloc(uni, -pages->num_pages);
		_jc_vm_free(&pages);
	}
}

/************************************************************************
 *		Class loader equivalents with assertions		*
 ************************************************************************/

void *
_jc_cl_alloc(_jc_env *env, _jc_class_loader *loader, size_t size)
{
	_JC_MUTEX_ASSERT(env, loader->mutex);
	return _jc_uni_alloc(env, &loader->uni, size);
}

void
_jc_cl_unalloc(_jc_class_loader *loader, void *pp, size_t size)
{
	_JC_MUTEX_ASSERT(_jc_get_current_env(), loader->mutex);
	_jc_uni_unalloc(&loader->uni, pp, size);
}

void *
_jc_cl_zalloc(_jc_env *env, _jc_class_loader *loader, size_t size)
{
	_JC_MUTEX_ASSERT(env, loader->mutex);
	return _jc_uni_zalloc(env, &loader->uni, size);
}

char *
_jc_cl_strdup(_jc_env *env, _jc_class_loader *loader, const char *s)
{
	_JC_MUTEX_ASSERT(env, loader->mutex);
	return _jc_uni_strdup(env, &loader->uni, s);
}

