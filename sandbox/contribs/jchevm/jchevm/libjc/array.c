
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
static jint	_jc_gen_array_hash_tables(_jc_env *env);

/*
 * Do one-time initialization for array types.
 *
 * If unsuccessful an exception is stored.
 */
jint
_jc_setup_array_types(_jc_env *env)
{
	jint status;

	/* Sanity check */
	_JC_ASSERT(env->vm->initialization != NULL);

	/* Generate array type's hash and "quick" lookup tables */
	if ((status = _jc_gen_array_hash_tables(env)) != JNI_OK)
		return status;

	/* Done */
	return JNI_OK;
}

/*
 * Generate interface list and hash tables for array types.
 *
 * If unsuccessful an exception is stored.
 */
static jint
_jc_gen_array_hash_tables(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
	_jc_boot_array *const array = &vm->boot.array;
	_jc_type *const interfaces[] = {	/* arrays implement these */
		vm->boot.types.Cloneable,
		vm->boot.types.Serializable,
	};
	const int num_interfaces = sizeof(interfaces) / sizeof(*interfaces);
	_jc_method *clone;
	int bucket;

	/* Sanity check */
	_JC_ASSERT(array->interfaces == NULL);
	_JC_ASSERT(array->imethod_hash_table == NULL);
	_JC_ASSERT(array->imethod_quick_table == NULL);

	/* Lock boot loader */
	_JC_MUTEX_LOCK(env, vm->boot.loader->mutex);

	/* Allocate interface list */
	if ((array->interfaces = _jc_cl_zalloc(env, vm->boot.loader,
	    num_interfaces * sizeof(*array->interfaces))) == NULL)
		goto fail;

	/* Fill in interface list */
	array->num_interfaces = num_interfaces;
	memcpy(array->interfaces, interfaces,
	    num_interfaces * sizeof(*interfaces));

	/* Allocate hash table and quick lookup table */
	if ((array->imethod_hash_table = _jc_cl_zalloc(env, vm->boot.loader,
	    _JC_IMETHOD_HASHSIZE * sizeof(*array->imethod_hash_table))) == NULL)
		goto fail;
	if ((array->imethod_quick_table = _jc_cl_zalloc(env, vm->boot.loader,
	    _JC_IMETHOD_HASHSIZE * sizeof(*array->imethod_quick_table)))
	      == NULL)
		goto fail;

	/* Find Object.clone() method */
	if ((clone = _jc_get_declared_method(env, vm->boot.types.Object,
	    "clone", "()Ljava/lang/Object;", _JC_ACC_STATIC, 0)) == NULL)
		goto fail;

	/* Get hash table index for clone() */
	bucket = clone->signature_hash_bucket;

	/* Create hash table bucket */
	if ((array->imethod_hash_table[bucket] = _jc_cl_zalloc(env,
	    vm->boot.loader, 2 * sizeof(*array->imethod_hash_table[bucket])))
	      == NULL)
		goto fail;

	/* Put clone() in the bucket */
	array->imethod_hash_table[bucket][0] = clone;
	array->imethod_hash_table[bucket][1] = NULL;

	/*
	 * Put clone() in the "quick" method lookup table. It's the only
	 * interface method implemented by arrays so we trivially know
	 * there are no other methods in the same hash bucket.
	 */
	array->imethod_quick_table[bucket] = clone;

	/* Unlock boot loader */
	_JC_MUTEX_UNLOCK(env, vm->boot.loader->mutex);

	/* Done */
	return JNI_OK;

fail:
	/* Clean up after failure */
	_jc_cl_unalloc(vm->boot.loader, &array->imethod_quick_table,
	    _JC_IMETHOD_HASHSIZE * sizeof(*array->imethod_quick_table));
	_jc_cl_unalloc(vm->boot.loader, &array->imethod_hash_table,
	    _JC_IMETHOD_HASHSIZE * sizeof(*array->imethod_hash_table));
	_jc_cl_unalloc(vm->boot.loader, &array->interfaces,
	    num_interfaces * sizeof(*array->interfaces));
	_JC_MUTEX_UNLOCK(env, vm->boot.loader->mutex);
	return JNI_ERR;
}


