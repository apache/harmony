
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
 * $Id: string.c,v 1.6 2005/03/16 15:31:12 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * Create a new string object from valid UTF-8 data.
 */
_jc_object *
_jc_new_string(_jc_env *env, const void *utf, size_t len)
{
	_jc_jvm *const vm = env->vm;
	_jc_char_array *chars;
	jobject aref = NULL;
	jobject sref = NULL;
	jint clen;

	/* Compute length in characters */
	if ((clen = _jc_utf_decode(utf, len, NULL)) == -1) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "%s: invalid UTF-8 data!", __FUNCTION__);
		goto done;
	}

	/* Create character array and wrap temporarily */
	if ((aref = _jc_new_local_native_ref(env,
	    (_jc_object *)_jc_new_array(env,
	      vm->boot.types.prim_array[_JC_TYPE_CHAR], clen))) == NULL)
		goto done;
	chars = (_jc_char_array *)*aref;

	/* Decode characters into char[] array */
	_jc_utf_decode(utf, len, chars->elems);

	/* Instantiate new string */
	if ((sref = _jc_new_local_native_ref(env,
	    _jc_new_object(env, vm->boot.types.String))) == NULL)
		goto done;
	if (_jc_invoke_nonvirtual(env,
	    vm->boot.methods.String.init, *sref, chars) != JNI_OK) {
		_jc_free_local_native_ref(&sref);
		goto done;
	}

done:
	/* Clean up and return */
	_jc_free_local_native_ref(&aref);
	return _jc_free_local_native_ref(&sref);
}

/*
 * Create a new intern'd string object from valid UTF-8 data.
 */
_jc_object *
_jc_new_intern_string(_jc_env *env, const void *utf, size_t len)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *string = NULL;
	jobject sref;

	/* Create a normal string */
	if ((sref = _jc_new_local_native_ref(env,
	    _jc_new_string(env, utf, len))) == NULL)
		goto done;

	/* Intern it */
	if (_jc_invoke_virtual(env,
	    vm->boot.methods.String.intern, *sref) != JNI_OK)
		goto done;
	string = env->retval.l;
	_JC_ASSERT(string != NULL);

done:
	/* Clean up and return */
	_jc_free_local_native_ref(&sref);
	return string;
}

/*
 * Convert a String object to a character array.
 * The length of the array is returned.
 *
 * If 'chars' is not NULL, the array is copied there.
 */
jint
_jc_decode_string_chars(_jc_env *env, _jc_object *string, jchar *chars)
{
	_jc_jvm *const vm = env->vm;
	_jc_char_array *array;
	jint offset;
	jint count;

	/* Sanity check */
	_JC_ASSERT(string->type == vm->boot.types.String);

	/* Get character array region */
	array = *_JC_VMFIELD(vm, string, String, value, _jc_char_array *);
	offset = *_JC_VMFIELD(vm, string, String, offset, jint);
	count = *_JC_VMFIELD(vm, string, String, count, jint);
	_JC_ASSERT(array != NULL);
	_JC_ASSERT(_JC_BOUNDS_CHECK(array, offset, count));

	/* Copy characters */
	if (chars != NULL)
		memcpy(chars, array->elems + offset, count * sizeof(*chars));

	/* Done */
	return count;
}

/*
 * Convert a String object to a NUL-terminated UTF-8 string.
 * The length of the UTF-8 string (NOT including NUL) is returned.
 * If 'buf' is not NULL, the UTF-8 string PLUS NUL is wrttten there.
 */
size_t
_jc_decode_string_utf8(_jc_env *env, _jc_object *string, char *buf)
{
	_jc_jvm *const vm = env->vm;
	_jc_char_array *array;
	jint offset;
	jint count;
	size_t len;

	/* Sanity check */
	_JC_ASSERT(string->type == vm->boot.types.String);

	/* Get character array region */
	array = *_JC_VMFIELD(vm, string, String, value, _jc_char_array *);
	offset = *_JC_VMFIELD(vm, string, String, offset, jint);
	count = *_JC_VMFIELD(vm, string, String, count, jint);
	_JC_ASSERT(array != NULL);
	_JC_ASSERT(_JC_BOUNDS_CHECK(array, offset, count));

	/* UTF-8 decode and nul-terminate the string */
	len = _jc_utf_encode(array->elems + offset, count, (u_char *)buf);
	if (buf != NULL)
		buf[len] = '\0';

	/* Done */
	return len;
}


