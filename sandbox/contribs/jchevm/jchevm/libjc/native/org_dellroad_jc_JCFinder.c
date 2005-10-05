
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
 * $Id: org_dellroad_jc_JCFinder.c,v 1.5 2005/05/15 21:41:01 archiecobbs Exp $
 */

#include "libjc.h"
#include "org_dellroad_jc_JCFinder.h"

/* Internal functions */
static jint	get_class_info(_jc_env *env, _jc_object *nameString,
			_jc_object *loaderObj, jlong *hashp,
			_jc_classbytes **bytesp);

/*
 * private static native byte[] getClassfile(String, ClassLoader)
 */
_jc_byte_array * _JC_JCNI_ATTR
JCNI_org_dellroad_jc_JCFinder_getClassfile(_jc_env *env,
	_jc_object *nameString, _jc_object *loaderObj)
{
	_jc_jvm *const vm = env->vm;
	_jc_byte_array *bytes = NULL;
	_jc_classbytes *cbytes;

	/* Get class file node */
	if (get_class_info(env, nameString,
	    loaderObj, NULL, &cbytes) != JNI_OK)
		_jc_throw_exception(env);

	/* Create new byte[] array and copy bytes into it */
	if ((bytes = (_jc_byte_array *)_jc_new_array(env,
	    vm->boot.types.prim_array[_JC_TYPE_BYTE], cbytes->length)) == NULL)
		_jc_throw_exception(env);

	/* Copy bytes */
	memcpy(bytes->elems, cbytes->bytes, cbytes->length);

	/* Unreference bytes */
	_jc_free_classbytes(&cbytes);

	/* Done */
	return bytes;
}

/*
 * private static native long getClassfileHash(String, ClassLoader)
 */
jlong _JC_JCNI_ATTR
JCNI_org_dellroad_jc_JCFinder_getClassfileHash(_jc_env *env,
	_jc_object *nameString, _jc_object *loaderObj)
{
	jlong hash;

        /* Get class file node */
	if (get_class_info(env, nameString,
	    loaderObj, &hash, NULL) != JNI_OK)
		_jc_throw_exception(env);

	/* Return hash */
	return hash;
}

/*
 * Get a classfile. Caller must release the reference on it.
 *
 * Posts a NoClassDefFoundError if not found.
 */
static jint
get_class_info(_jc_env *env, _jc_object *nameString, _jc_object *loaderObj,
	jlong *hashp, _jc_classbytes **bytesp)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_node *node = NULL;
	_jc_class_loader *loader;
	jint status = JNI_ERR;
	size_t name_len;
	char *name;
	int i;

	/* Check for null */
	if (nameString == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto fail;
	}

	/* Convert name to UTF-8 */
	name_len = _jc_decode_string_utf8(env, nameString, NULL);
	if ((name = _JC_STACK_ALLOC(env, name_len + 1)) == NULL) {
		_jc_post_exception_info(env);
		goto fail;
	}
	_jc_decode_string_utf8(env, nameString, name);

	/* Code generation must be enabled to retrieve class files */
	if (!vm->generation_enabled) {
		_jc_post_exception_msg(env, _JC_NoClassDefFoundError,
		    "can't get class file bytes for `%s' because object"
		    " file generation is disabled", name);
		goto fail;
	}

	/* Replace dots -> slashes */
	for (i = 0; name[i] != '\0'; i++) {
		if (name[i] == '.')
			name[i] = '/';
	}

        /* Get class loader */
	if (loaderObj == NULL)
		loader = vm->boot.loader;
	else if ((loader = _jc_get_loader(env, loaderObj)) == NULL)
		goto fail;

	/* Acquire the class file and node */
	if ((node = _jc_get_class_node(env, loader, name)) == NULL)
		goto fail;

	/* Copy hash value */
	if (hashp != NULL)
		*hashp = node->hash;

	/*
	 * Check whether the node comes with class file bytes.
	 * If not, try the boot loader classpath as a last resort.
	 */
	if (bytesp != NULL) {
		if (node->bytes != NULL)
			*bytesp = _jc_dup_classbytes(node->bytes);
		else if ((*bytesp = _jc_bootcl_find_classbytes(env,
		    name, NULL)) == NULL) {
			_jc_post_exception_info(env);
			goto fail;
		}
	}

	/* Done */
	status = JNI_OK;

fail:
	/* Clean up */
	_JC_MUTEX_LOCK(env, vm->mutex);
	_jc_unref_class_node(vm, &node);
	_JC_MUTEX_UNLOCK(env, vm->mutex);

	/* Done */
	return status;
}

