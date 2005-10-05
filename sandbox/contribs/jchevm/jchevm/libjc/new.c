
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
 * $Id: new.c,v 1.12 2005/03/20 23:33:28 archiecobbs Exp $
 */

#include "libjc.h"

/* Internal functions */
static _jc_object	*_jc_initialize_object(void *mem, _jc_type *type,
				_jc_word lockword, int nrefs, int prim_size,
				jboolean skipword);

/*
 * Initialize a heap block containing an object, optionally including
 * a "skip word" at the beginning. To ensure that the finalizer thread
 * never sees a partially constructed object, we modify the first word last.
 */
static inline _jc_object *
_jc_initialize_object(void *mem, _jc_type *type, _jc_word lockword,
	int nrefs, int prim_size, jboolean skipword)
{
	_jc_object *obj;

	/* Handle skipword vs. no skipword cases */
	if (skipword) {
		_jc_word *const obj_start = (_jc_word *)mem + 1;

		/* Zero object's reference fields */
		memset(obj_start, 0, nrefs * sizeof(void *));

		/* Initialize object header */
		obj = (_jc_object *)(obj_start + nrefs);
		obj->lockword = lockword;
		obj->type = type;

		/* Zero non-reference fields */
		memset((char *)obj + sizeof(*obj), 0, prim_size);

		/* Lastly, set the skip word: skip refs and skip word itself */
		_JC_ASSERT(nrefs + 1 < _JC_HEAP_MAX(SKIP));
		*((volatile _jc_word *)mem)
		    = ((nrefs + 1) << _JC_HEAP_SKIP_SHIFT)
		      | (_JC_HEAP_BLOCK_SKIP << _JC_HEAP_BTYPE_SHIFT);
	} else {
		_jc_word *const obj_start = (_jc_word *)mem;

		/* Zero all reference fields except the first */
		if (nrefs > 1)
			memset(obj_start + 1, 0, (nrefs - 1) * sizeof(void *));

		/*
		 * Initialize object header. In case the lockword is the
		 * first word in the block, we must initialize it last so
		 * the finalizer thread never sees an incomplete object.
		 */
		obj = (_jc_object *)(obj_start + nrefs);
		obj->type = type;
		obj->lockword = lockword;

		/* Zero non-reference fields */
		memset((char *)obj + sizeof(*obj), 0, prim_size);

		/* Lastly, set the first reference field */
		if (nrefs > 0)
			*obj_start = 0;
	}

	/* Done */
	return obj;
}

/************************************************************************
 *			Object creation					*
 ************************************************************************/

/*
 * Allocate a new non-array object from the heap and initialize it.
 * This only initializes the memory, it doesn't execute any constructor.
 * 'type' must not be an array type, abstract type, or interface.
 *
 * NOTE: The caller is responsible for ensuring that type->loader does
 * not get unloaded during this function. I.e., it must be the boot loader
 * or else a reference to the type's Class or ClassLoader object must be
 * retained somehow.
 */
_jc_object *
_jc_new_object(_jc_env *env, _jc_type *type)
{
	_jc_object *obj;
	void *mem;
	int bsi;

	/* Sanity checks */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));
	_JC_ASSERT((type->flags & _JC_TYPE_MASK) == _JC_TYPE_REFERENCE);

	/* Check object type */
	if ((type->access_flags & (_JC_ACC_ABSTRACT|_JC_ACC_INTERFACE)) != 0) {
		_jc_post_exception_msg(env, _JC_InstantiationError,
		    "class %s is %s", type->name,
		    _JC_ACC_TEST(type, INTERFACE) ? "interface" : "abstract");
		return NULL;
	}

	/* Initialize type */
	if (!_JC_FLG_TEST(type, INITIALIZED)
	    && _jc_initialize_type(env, type) != JNI_OK)
		return NULL;
	_JC_ASSERT((type->initial_lockword & _JC_LW_ODD_BIT) != 0);

	/* Allocate memory from the heap and initialize it */
	if ((bsi = type->u.nonarray.block_size_index) >= 0) {

		/* Get a small page block */
		if ((mem = _jc_heap_alloc_small_block(env, bsi)) == NULL)
			return NULL;

		/* Initialize object */
		obj = _jc_initialize_object(mem, type, type->initial_lockword,
		    type->u.nonarray.num_virtual_refs,
		    type->u.nonarray.instance_size
		      - (type->u.nonarray.num_virtual_refs * sizeof(void *))
		      - sizeof(*obj), _JC_FLG_TEST(type, SKIPWORD));
	} else {

		/* Get a contiguous range of large pages */
		if ((mem = _jc_heap_alloc_pages(env, -bsi)) == NULL)
			return NULL;

		/* Initialize object */
		obj = _jc_initialize_object((char *)mem + _JC_HEAP_BLOCK_OFFSET,
		    type, type->initial_lockword,
		    type->u.nonarray.num_virtual_refs,
		    type->u.nonarray.instance_size
		      - (type->u.nonarray.num_virtual_refs * sizeof(void *))
		      - sizeof(*obj), _JC_FLG_TEST(type, SKIPWORD));

		/* Mark large page range as in use */
		*((volatile _jc_word *)mem)
		    = (_JC_HEAP_PAGE_LARGE << _JC_HEAP_PTYPE_SHIFT)
		      | (-bsi << _JC_HEAP_NPAGES_SHIFT);
	}

	/* Done */
	return obj;
}

/*
 * Initialize a non-array object allocated on the stack.
 */
_jc_object *
_jc_init_object(_jc_env *env, void *mem, _jc_type *type)
{
	_jc_object *obj;

	/* Sanity checks */
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));
	_JC_ASSERT((type->flags & _JC_TYPE_MASK) == _JC_TYPE_REFERENCE);
	_JC_ASSERT(!_JC_IN_HEAP(&env->vm->heap, mem));

	/* Check object type */
	if ((type->access_flags & (_JC_ACC_ABSTRACT|_JC_ACC_INTERFACE)) != 0) {
		_jc_post_exception_msg(env, _JC_InstantiationError,
		    "class %s is %s", type->name,
		    _JC_ACC_TEST(type, INTERFACE) ? "interface" : "abstract");
		return NULL;
	}

	/* Initialize type */
	if (!_JC_FLG_TEST(type, INITIALIZED)
	    && _jc_initialize_type(env, type) != JNI_OK)
		return NULL;
	_JC_ASSERT(_JC_LW_TEST(type->initial_lockword, ODD));
	_JC_ASSERT(!_JC_LW_TEST(type->initial_lockword, FINALIZE));

	/* Zero memory */
	memset(mem, 0, type->u.nonarray.instance_size);

	/* Initialize object header */
	obj = (_jc_object *)((void **)mem + type->u.nonarray.num_virtual_refs);
	obj->lockword = type->initial_lockword;
	obj->type = type;

	/* Another sanity check */
	_JC_ASSERT(!_jc_subclass_of(obj, env->vm->boot.types.Reference));

	/* Done */
	return obj;
}

/*
 * Initialize a new Class object previously allocated in loader memory.
 * This only initializes the memory, it doesn't execute any constructor.
 * We don't include skip words for Class objects because they are only
 * useful for heap-allocated objects.
 */
_jc_object *
_jc_initialize_class_object(_jc_env *env, void *mem)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *const type = vm->boot.types.Class;
	_jc_object *obj;

	/* Sanity checks */
	_JC_ASSERT((type->initial_lockword & _JC_LW_ODD_BIT) != 0);
	_JC_ASSERT(!_JC_FLG_TEST(type, ARRAY));
	_JC_ASSERT(!_JC_IN_HEAP(&vm->heap, mem));

	/* Initialize object */
	return _jc_initialize_object(mem, type, type->initial_lockword,
	    type->u.nonarray.num_virtual_refs,
	    type->u.nonarray.instance_size
	      - (type->u.nonarray.num_virtual_refs * sizeof(void *))
	      - sizeof(*obj), JNI_FALSE);
}

/************************************************************************
 *			Array creation					*
 ************************************************************************/

/*
 * Create a new array instance.
 */
_jc_array *
_jc_new_array(_jc_env *env, _jc_type *type, jint len)
{
	_jc_jvm *const vm = env->vm;
	u_char elem_type;
	_jc_array *array;
	int array_size;
	void *mem;
	int bsi;

	/* Sanity check */
	_JC_ASSERT(_JC_FLG_TEST(type, ARRAY));
	elem_type = (type->u.array.element_type->flags & _JC_TYPE_MASK);
	_JC_ASSERT(elem_type != _JC_TYPE_INVALID);

	/* Check for negative length */
	if (len < 0) {
		_jc_post_exception_msg(env,
		    _JC_NegativeArraySizeException, "%d", (int)len);
		return NULL;
	}

	/* Guard against arithmetic overflow */
	if ((jlong)len >= _jc_type_max_array_length[elem_type]) {
		_jc_post_exception_msg(env, _JC_OutOfMemoryError,
		    "array length %d is too big for type `%s'",
		    (int)len, type->name);
		return NULL;
	}

	/* Compute size of the array object */
	array_size = _jc_array_head_sizes[elem_type]
	    + len * _jc_type_sizes[elem_type];

	/* Get corresponding block size index */
	bsi = _jc_heap_block_size(vm, array_size);

	/* Allocate heap memory */
	mem = (bsi >= 0) ?
	    _jc_heap_alloc_small_block(env, bsi) :
	    _jc_heap_alloc_pages(env, -bsi);
	if (mem == NULL)
		return NULL;

	/* Point to start of object; skip page offset for large pages */
	array = (_jc_array *)(bsi < 0 ?
	    (char *)mem + _JC_HEAP_BLOCK_OFFSET : mem);

	/* Initialize heap memory */
	if (elem_type == _JC_TYPE_REFERENCE) {
		const int ref_count_bits = (len < _JC_LW_MAX(REF_COUNT) - 1) ?
		    len : _JC_LW_MAX(REF_COUNT) - 1;
		int block_size;

		/* Determine if we desire and have room for a skip word */
		block_size = bsi < 0 ?
		    ((-bsi * _JC_PAGE_SIZE) - _JC_HEAP_BLOCK_OFFSET) :
		    vm->heap.sizes[bsi].size;

		/* Initialize array object */
		array = (_jc_array *)_jc_initialize_object(array, type,
		    type->initial_lockword
		      | (ref_count_bits << _JC_LW_REF_COUNT_SHIFT),
		    len, 0, 
		    (block_size >= array_size + sizeof(_jc_word)
		      && len >= _JC_SKIPWORD_MIN_REFS));
	} else {
		array = (_jc_array *)_jc_initialize_object(array, type,
		    type->initial_lockword, 0, array_size - sizeof(_jc_object),
		    JNI_FALSE);
	}

	/* Initialize heap pages if we got large pages */
	if (bsi < 0) {
		*((volatile _jc_word *)mem)
		    = (_JC_HEAP_PAGE_LARGE << _JC_HEAP_PTYPE_SHIFT)
		      | (-bsi << _JC_HEAP_NPAGES_SHIFT);
	}

	/* Set length */
	*((jint *)&array->length) = len;

	/* Done */
	return array;
}

/*
 * Initialize an array instance allocated on the stack.
 */
_jc_array *
_jc_init_array(_jc_env *env, void *mem, _jc_type *type, jint len)
{
	_jc_word lockword;
	u_char elem_type;
	_jc_array *array;

	/* Sanity check */
	_JC_ASSERT(_JC_FLG_TEST(type, ARRAY));
	elem_type = (type->u.array.element_type->flags & _JC_TYPE_MASK);
	_JC_ASSERT(elem_type != _JC_TYPE_INVALID);
	_JC_ASSERT(!_JC_IN_HEAP(&env->vm->heap, mem));

	/* Check for negative length */
	if (len < 0) {
		_jc_post_exception_msg(env,
		    _JC_NegativeArraySizeException, "%d", (int)len);
		return NULL;
	}

	/* Guard against arithmetic overflow */
	if ((jlong)len >= _jc_type_max_array_length[elem_type]) {
		_jc_post_exception_msg(env, _JC_OutOfMemoryError,
		    "array length %d is too big for type `%s'",
		    (int)len, type->name);
		return NULL;
	}

	/* Initialize array header and zero array elements */
	lockword = type->initial_lockword;
	if (elem_type == _JC_TYPE_REFERENCE) {
		const int ref_count_bits = (len < _JC_LW_MAX(REF_COUNT) - 1) ?
		    len : _JC_LW_MAX(REF_COUNT) - 1;

		memset(mem, 0, len * sizeof(void *));
		array = (_jc_array *)((void **)mem + len);
		array->lockword = type->initial_lockword
		    | (ref_count_bits << _JC_LW_REF_COUNT_SHIFT);
		array->type = type;
	} else {
		array = (_jc_array *)mem;
		array->lockword = type->initial_lockword;
		array->type = type;
		memset((char *)array + _jc_array_head_sizes[elem_type],
		    0, len * _jc_type_sizes[elem_type]);
	}
	*((jint *)&array->length) = len;

	/* Done */
	return array;
}

/*
 * Recursively create a new multi-dimensional reference array.
 */
_jc_array *
_jc_new_multiarray(_jc_env *env, _jc_type *type,
	jint num_sizes, const jint *sizes)
{
	const jint len = sizes[0];
	_jc_array *array;
	jobject ref;
	jint i;

	/* Sanity check */
	_JC_ASSERT(_JC_FLG_TEST(type, ARRAY));
	_JC_ASSERT(num_sizes >= 0 && num_sizes <= type->u.array.dimensions);

	/* Create new array */
	if ((array = _jc_new_array(env, type, len)) == NULL)
		return NULL;

	/* Do we need to create sub-arrays? */
	if (type->u.array.dimensions == 1 || len == 0 || num_sizes < 2) {

		/* Check for any subsequent negative dimensions */
		if (len == 0) {
			for (i = 1; i < num_sizes; i++) {
				if (sizes[i] < 0) {
					_jc_post_exception_msg(env,
					    _JC_NegativeArraySizeException,
					    "%d", (int)sizes[i]);
					return NULL;
				}
			}
		}

		/* Done */
		return array;
	}

	/* Keep a native reference to 'array' while creating sub-arrays */
	if ((ref = _jc_new_local_native_ref(env, (_jc_object *)array)) == NULL)
		return NULL;

	/* Create sub-arrays recursively */
	for (i = 0; i < len; i++) {
		_jc_array *subarray;

		if ((subarray = _jc_new_multiarray(env,
		    type->u.array.element_type,
		    num_sizes - 1, sizes + 1)) == NULL) {
			_jc_free_local_native_ref(&ref);
			return NULL;
		}
		((_jc_object_array *)array)->elems[~i] = (_jc_object *)subarray;
	}

	/* Free native reference */
	_jc_free_local_native_ref(&ref);

	/* Done */
	return array;
}

/*
 * Initialize a stack-allocated multi-dimensional reference array.
 */
_jc_array *
_jc_init_multiarray(_jc_env *env, void *mem, _jc_type *type,
	jint num_sizes, const jint *sizes)
{
	const jint len = sizes[0];
	_jc_array *array;
	jint i;

	/* Sanity check */
	_JC_ASSERT(_JC_FLG_TEST(type, ARRAY));
	_JC_ASSERT(num_sizes >= 0 && num_sizes <= type->u.array.dimensions);
	_JC_ASSERT(!_JC_IN_HEAP(&env->vm->heap, mem));

	/* Initialize new array */
	if ((array = _jc_init_array(env, mem, type, len)) == NULL)
		return NULL;

	/* Do we need to create sub-arrays? */
	if (type->u.array.dimensions == 1 || len == 0 || num_sizes < 2)
		return array;

	/* Create sub-arrays recursively */
	for (i = 0; i < len; i++) {
		_jc_array *subarray;

		if ((subarray = _jc_new_multiarray(env,
		    type->u.array.element_type,
		    num_sizes - 1, sizes + 1)) == NULL)
			return NULL;
		((_jc_object_array *)array)->elems[~i] = (_jc_object *)subarray;
	}

	/* Done */
	return array;
}

