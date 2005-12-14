
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

/*
 * Determine if an instance of type 'from' can be assigned to
 * a variable of type 'to'.
 *
 * Returns:
 *	 1	Yes
 *	 0	No
 *	-1	Exception posted
 */
int
_jc_assignable_from(_jc_env *env, _jc_type *from, _jc_type *to)
{
	_jc_type *const *entry;

	/* Resolve types */
	if (!_JC_FLG_TEST(from, RESOLVED)
	    && _jc_resolve_type(env, from) != JNI_OK)
		return -1;
	if (!_JC_FLG_TEST(to, RESOLVED)
	    && _jc_resolve_type(env, to) != JNI_OK)
		return -1;

	/* Quick check for a common case (?) XXX */
	if (from == to)
		return 1;

	/* Handle the case where 'from' type is an array type */
	if (_JC_FLG_TEST(from, ARRAY)) {
		if (_JC_FLG_TEST(to, ARRAY)
		    && from->u.array.dimensions > to->u.array.dimensions) {
			to = to->u.array.base_type;
			_JC_ASSERT(!_JC_FLG_TEST(to, ARRAY));
			goto check_array;
		}
		if (!_JC_FLG_TEST(to, ARRAY)) {
check_array:		return to == env->vm->boot.types.Object
			    || to == env->vm->boot.types.Cloneable
			    || to == env->vm->boot.types.Serializable;
		}
		to = to->u.array.base_type;
		from = from->u.array.base_type;
	}

	/* If 'to' is an array type, then we know already 'from' is not */
	if (_JC_FLG_TEST(to, ARRAY))
		return 0;

	/* Check both base types are same primitive, or both reference */
	if ((from->flags & _JC_TYPE_MASK) != (to->flags & _JC_TYPE_MASK))
		return 0;

	/* If both are primitive, they are the same type by the above test */
	if ((from->flags & _JC_TYPE_MASK) != _JC_TYPE_REFERENCE)
		return 1;

	/* Resolve from type so hashtable is valid */
	if (!_JC_FLG_TEST(from, RESOLVED)
	    && _jc_resolve_type(env, from) != JNI_OK)
		return -1;

	/* Search instanceof hash table */
	if ((entry = from->u.nonarray.instanceof_hash_table[
	    _JC_INSTANCEOF_BUCKET(to)]) == NULL)
		return 0;
	while (*entry != NULL) {
		if (*entry++ == to)
			return 1;
	}

	/* Not found - not an instance of */
	return 0;
}

/*
 * Determine if 'obj' is an instance of type 'type'.
 *
 * Returns:
 *	 1	Yes
 *	 0	No
 *	-1	Exception posted
 */
int
_jc_instance_of(_jc_env *env, _jc_object *obj, _jc_type *type)
{
	/* 'null' is not an instance of any type */
	if (obj == NULL)
		return JNI_FALSE;

	/* Return same as 'assignable from' */
	return _jc_assignable_from(env, obj->type, type);
}

/*
 * Determine if 'obj' is an instance of 'type' or any subclass thereof.
 *
 * This assumes that 'obj' is not NULL. If type is an interface
 * or array type then false is always returned.
 */
jboolean
_jc_subclass_of(_jc_object *obj, _jc_type *type)
{
	_jc_type *obj_type;

	/* Sanity check */
	_JC_ASSERT(obj != NULL);

	/* Check superclasses */
	for (obj_type = obj->type;
	    obj_type != NULL; obj_type = obj_type->superclass) {
		if (obj_type == type)
			return JNI_TRUE;
	}

	/* Not found */
	return JNI_FALSE;
}

