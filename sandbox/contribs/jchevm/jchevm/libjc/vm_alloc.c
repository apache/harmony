
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
 * $Id: vm_alloc.c,v 1.3 2005/02/27 23:29:45 archiecobbs Exp $
 */

#include "libjc.h"

/************************************************************************
 *			VM memory pool allocation			*
 ************************************************************************/

char *
_jc_vm_strndup(_jc_env *env, const char *s, size_t len)
{
	char *result;

	if ((result = malloc(len + 1)) == NULL) {
		_JC_EX_STORE(env, OutOfMemoryError, "system heap");
		return NULL;
	}
	memcpy(result, s, len);
	result[len] = '\0';
	return result;
}

char *
_jc_vm_strdup(_jc_env *env, const char *s)
{
	char *result;

	if ((result = strdup(s)) == NULL) {
		_JC_EX_STORE(env, OutOfMemoryError, "system heap");
		return NULL;
	}
	return result;
}

void *
_jc_vm_zalloc(_jc_env *env, size_t size)
{
	void *mem;

	if ((mem = malloc(size)) == NULL) {
		_JC_EX_STORE(env, OutOfMemoryError, "system heap");
		return NULL;
	}
	memset(mem, 0, size);
	return mem;
}

void *
_jc_vm_alloc(_jc_env *env, size_t size)
{
	void *mem;

	if ((mem = malloc(size)) == NULL) {
		_JC_EX_STORE(env, OutOfMemoryError, "system heap");
		return NULL;
	}
	return mem;
}

void *
_jc_vm_realloc(_jc_env *env, void *mem, size_t size)
{
	void *new_mem;

	if ((new_mem = realloc(mem, size)) == NULL) {
		_JC_EX_STORE(env, OutOfMemoryError, "system heap");
		return NULL;
	}
	return new_mem;
}

/*
 * This function must not allow this thread to be blocked & GC'd.
 */
void
_jc_vm_free(void *pointerp)
{
	void **const ptrp = pointerp;

	if (*ptrp != NULL) {
		free(*ptrp);
		*ptrp = NULL;
	}
}

