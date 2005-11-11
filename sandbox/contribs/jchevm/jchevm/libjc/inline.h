
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
 * $Id: inline.h,v 1.7 2005/11/09 18:14:22 archiecobbs Exp $
 */

/*
 * Find the object head given a pointer to the start of its memory block.
 */
static inline _jc_object *
_jc_find_object_head(volatile const void *block)
{
	const _jc_word word = *(volatile _jc_word *)block;
	_jc_word *ptr;

	/* Check for a skip word or object header */
	if (_JC_LW_TEST(word, ODD)) {
		_JC_ASSERT(word != _JC_HEAP_BLOCK_FREE
		    && word != _JC_HEAP_BLOCK_ALLOC);
		if (_JC_HEAP_EXTRACT(word, BTYPE) == _JC_HEAP_BLOCK_SKIP) {
			_JC_ASSERT(_JC_HEAP_EXTRACT(word, SKIP)
			    >= 1 + _JC_SKIPWORD_MIN_REFS);
			return (_jc_object *)((_jc_word *)block
			    + _JC_HEAP_EXTRACT(word, SKIP));
		}
		return (_jc_object *)block;
	}

	/* Scan over leading references until we get to the object header */
	for (ptr = (_jc_word *)block + 1; !_JC_LW_TEST(*ptr, ODD); ptr++);
	return (_jc_object *)ptr;
}

/*
 * Initialize heap sweep (visiting every object).
 */
static inline void
_jc_heap_sweep_init(_jc_heap *heap, _jc_heap_sweep *sweep)
{
	memset(sweep, 0, sizeof(*sweep));
	sweep->heap = heap;
	sweep->page = heap->pages;
	sweep->end = (char *)heap->pages + heap->num_pages * _JC_PAGE_SIZE;
}

/*
 * Advance to the next object in a heap sweep.
 */
static inline _jc_object *
_jc_heap_sweep_next(_jc_heap_sweep *sweep, jboolean recycle_small)
{
	_jc_word word;

	/* Handle small page vs. large page */
	if (sweep->size != NULL) {

next_block:
		/* If we are out of blocks, go to the next page */
		_JC_ASSERT(sweep->blocks_left > 0);
		if (--sweep->blocks_left == 0) {
			int next_index;

			/* Check if we should recycle this page */
			if (!recycle_small)
				goto next_page;

			/*
			 * If page is empty, mark it free. Otherwise,
			 * put it on the "use me first" list for the
			 * corresponding block size.
			 */
			if (sweep->blocks_live == 0) {
				*((_jc_word *)sweep->page) = _JC_HEAP_PAGE_FREE;
				goto next_page;
			}

			/* Get index of next page in size's list */
			next_index = (sweep->size->pages != NULL) ?
			    _JC_PAGE_INDEX(sweep->heap, sweep->size->pages) :
			    _JC_HEAP_MAX(NEXT) - 1;

			/* Insert this page at the head of the list */
			*((_jc_word *)sweep->page)
			    = (_JC_HEAP_PAGE_SMALL << _JC_HEAP_PTYPE_SHIFT)
			    | (sweep->bsi << _JC_HEAP_BSI_SHIFT)
			    | (next_index << _JC_HEAP_NEXT_SHIFT);
			sweep->size->pages = (_jc_word *)sweep->page;

next_page:
			/* Advance to the next page */
			sweep->page += _JC_PAGE_SIZE;
			goto new_page;
		}

		/* Advance to the next block in the page */
		sweep->block += sweep->block_size;

new_block:
		/* Skip unoccupied blocks */
		word = *((volatile _jc_word *)sweep->block);
		if (word == _JC_HEAP_BLOCK_FREE || word == _JC_HEAP_BLOCK_ALLOC)
			goto next_block;

		/* Done */
		return _jc_find_object_head(sweep->block);
	} else
		sweep->page += sweep->npages * _JC_PAGE_SIZE;

new_page:
	/* Check for end of heap */
	_JC_ASSERT((char *)sweep->page <= sweep->end);
	if ((char *)sweep->page == sweep->end)
		return NULL;

	/* Get descriptor word for this page */
	word = *((volatile _jc_word *)sweep->page);

	/* Handle page based on type */
	switch (_JC_HEAP_EXTRACT(word, PTYPE)) {
	case _JC_HEAP_PAGE_FREE:
	case _JC_HEAP_PAGE_ALLOC:
		sweep->page += _JC_PAGE_SIZE;
		goto new_page;
	case _JC_HEAP_PAGE_SMALL:
		sweep->bsi = _JC_HEAP_EXTRACT(word, BSI);
		_JC_ASSERT(sweep->bsi >= 0
		    && sweep->bsi < sweep->heap->num_sizes);
		sweep->size = &sweep->heap->sizes[sweep->bsi];
		sweep->block_size = sweep->size->size;
		sweep->blocks_left = sweep->size->num_blocks;
		sweep->blocks_live = 0;
		sweep->block = sweep->page + _JC_HEAP_BLOCK_OFFSET;
		goto new_block;
	case _JC_HEAP_PAGE_LARGE:
		sweep->npages = _JC_HEAP_EXTRACT(word, NPAGES);
		_JC_ASSERT(sweep->npages >= 1);
		sweep->bsi = -1;
		sweep->size = NULL;
		sweep->block = sweep->page + _JC_HEAP_BLOCK_OFFSET;
		return _jc_find_object_head(sweep->block);
	default:
		_JC_ASSERT(0);
		return NULL;
	}
}

/*
 * Return the number of references contained in an object.
 */
static inline int
_jc_num_refs(_jc_object *const obj)
{
	int nrefs;

	if ((nrefs = _JC_LW_EXTRACT(obj->lockword, REF_COUNT))
	    == _JC_LW_MAX(REF_COUNT) - 1) {
		_jc_type *const type = obj->type;

		if (!_JC_FLG_TEST(type, ARRAY))
			nrefs = type->u.nonarray.num_virtual_refs;
		else {
			_JC_ASSERT((type->u.array.element_type->flags
			    & _JC_TYPE_MASK) == _JC_TYPE_REFERENCE);
			nrefs = ((_jc_object_array *)obj)->length;
		}
	}
	return nrefs;
}

