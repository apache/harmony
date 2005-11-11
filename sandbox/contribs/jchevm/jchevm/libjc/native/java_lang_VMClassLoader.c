
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
 * $Id: java_lang_VMClassLoader.c,v 1.15 2005/11/09 18:14:22 archiecobbs Exp $
 */

#include "libjc.h"
#include "java_lang_VMClassLoader.h"

/*
 * static final native Class defineClass(ClassLoader,
 *	String, byte[], int, int, ProtectionDomain) throws ClassFormatError
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClassLoader_defineClass(_jc_env *env, _jc_object *cl,
	_jc_object *name_string, _jc_byte_array *data, jint offset,
	jint len, _jc_object *pd)
{
	_jc_jvm *const vm = env->vm;
	_jc_classbytes *cbytes = NULL;
	_jc_class_loader *loader;
	_jc_type *type = NULL;
	char *name = NULL;

	/* Check for null */
	if (data == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}
	_JC_ASSERT(data->type == env->vm->boot.types.prim_array[_JC_TYPE_BYTE]);

        /* Check byte[] array bounds */
	if (!_JC_BOUNDS_CHECK(data, offset, len)) {
		_jc_post_exception(env, _JC_ArrayIndexOutOfBoundsException);
		goto done;
	}

	/* Get loader */
	if (cl == NULL)
		loader = vm->boot.loader;
	else {
		_JC_ASSERT(_jc_subclass_of(cl,
		    env->vm->boot.types.ClassLoader));
		if ((loader = _jc_get_loader(env, cl)) == NULL)
			goto done;
	}

	/* Convert name string to UTF-8 and convert '.' chars to '/' */
	if (name_string != NULL) {
		size_t name_len;
		char *s;

		name_len = _jc_decode_string_utf8(env, name_string, NULL);
		if ((name = _JC_STACK_ALLOC(env, name_len + 1)) == NULL) {
			_jc_post_exception_info(env);
			goto done;
		}
		_jc_decode_string_utf8(env, name_string, name);
		for (s = name; *s != '\0'; s++) {
			if (*s == '.')
				*s = '/';
		}
	}

	/* Create '_jc_classbytes' object from byte[] range */
	if ((cbytes = _jc_copy_classbytes(env,
	    data->elems + offset, len)) == NULL)
		goto done;

	/* Derive type */
	if ((type = _jc_derive_type_from_classfile(env,
	    loader, name, cbytes)) == NULL)
		goto done;

	/* Set protection domain */
	*_JC_VMFIELD(vm, type->instance, Class, pd, _jc_object *) = pd;

done:
	/* Clean up */
	_jc_free_classbytes(&cbytes);

	/* Bail out if exception */
	if (type == NULL)
		_jc_throw_exception(env);

	/* Return Class object */
	return type->instance;
}

/*
 * static final native Class getPrimitiveClass(char)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClassLoader_getPrimitiveClass(_jc_env *env, jchar pchar)
{
	_jc_jvm *const vm = env->vm;
	int ptype;

	/* Get primitive type */
	ptype = _jc_sig_types[pchar];
	if (ptype == _JC_TYPE_INVALID || ptype == _JC_TYPE_REFERENCE) {
		_jc_post_exception_msg(env, _JC_InternalError,
		    "invalid primitive class type `%c'", pchar);
		_jc_throw_exception(env);
	}

	/* Return class instance */
	return vm->boot.types.prim[ptype]->instance;
}

/*
 * static final native Class loadClass(String, boolean)
 *	throws java/lang/ClassNotFoundException
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClassLoader_loadClass(_jc_env *env,
	_jc_object *string, jboolean resolve)
{
	return _jc_internal_load_class(env, string, NULL, resolve);
}

/*
 * static native Class findLoadedClass(ClassLoader, String)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMClassLoader_findLoadedClass(_jc_env *env,
	_jc_object *loader_obj, _jc_object *name_string)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *loader;
	size_t name_len;
	_jc_type *type;
	char *name;
	char *s;

	/* Get loader */
	if (loader_obj == NULL)
		loader = vm->boot.loader;
	else {
		_JC_ASSERT(_jc_subclass_of(loader_obj,
		    env->vm->boot.types.ClassLoader));
		if ((loader = _jc_get_loader(env, loader_obj)) == NULL)
			_jc_throw_exception(env);
	}

	/* Check for null */
	if (name_string == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}

	/* Convert name string to UTF-8 */
	name_len = _jc_decode_string_utf8(env, name_string, NULL);
	if ((name = _JC_STACK_ALLOC(env, name_len + 1)) == NULL) {
		_jc_post_exception_info(env);
		_jc_throw_exception(env);
	}
	_jc_decode_string_utf8(env, name_string, name);

	/* Replace '.' -> '/' */
	for (s = name; *s != '\0'; s++) {
		if (*s == '.')
			*s = '/';
	}

	/* Find type */
	if ((type = _jc_find_type(env, loader, name)) == NULL)
		return NULL;

	/* Return type's Class instance */
	return type->instance;
}

/*
 * static final native void resolveClass(Class)
 */
void _JC_JCNI_ATTR
JCNI_java_lang_VMClassLoader_resolveClass(_jc_env *env, _jc_object *class)
{
	_jc_jvm *const vm = env->vm;
	_jc_type *type;

	/* Check for null and get type */
	if (class == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	}
	_JC_ASSERT(class->type == vm->boot.types.Class);
	type = *_JC_VMFIELD(vm, class, Class, vmdata, _jc_type *);

	/* Link type */
	if (_jc_resolve_type(env, type) != JNI_OK)
		_jc_throw_exception(env);
}

/*
 * Load a class (array or non-array) using the class loader from the
 * supplied ClassLoader object, or the boot loader if null.
 */
_jc_object *
_jc_internal_load_class(_jc_env *env, _jc_object *name_string,
	_jc_object *loader_obj, jboolean resolve)
{
	_jc_jvm *const vm = env->vm;
	_jc_class_loader *loader;
	_jc_type *type = NULL;
	int status = JNI_ERR;
	char *name = NULL;
	size_t name_len;
	_jc_object *e;
	int i;

	/* Check for null */
	if (name_string == NULL) {
		_jc_post_exception(env, _JC_NullPointerException);
		goto done;
	}

	/* Get class loader */
	if (loader_obj == NULL)
		loader = vm->boot.loader;
	else if ((loader = _jc_get_loader(env, loader_obj)) == NULL)
		goto done;

	/* Convert name string to UTF-8 */
	name_len = _jc_decode_string_utf8(env, name_string, NULL);
	if ((name = _JC_STACK_ALLOC(env, name_len + 1)) == NULL) {
		_jc_post_exception_info(env);
		goto done;
	}
	_jc_decode_string_utf8(env, name_string, name);

	/* Disallow empty strings or embedded nul charcaters */
	if (name_len == 0 || strlen(name) != name_len)
		goto invalid;

	/* Disallow '/' characters */
	if (strchr(name, '/') != NULL)
		goto invalid;

	/*
	 * Disallow duplicate, leading, or trailing '.' characters.
	 * Replace '.' characters with '/' characters.
	 */
	if (name[0] == '.' || name[name_len - 1] == '.')
		goto invalid;
	for (i = 0; i < name_len - 1; i++) {
		if (name[i] == '.' && name[i + 1] == '.') {
			_jc_word params[2];
invalid:		
			params[0] = (_jc_word)name_string;
			params[1] = (_jc_word)NULL;
			_jc_post_exception_params(env,
			    _JC_ClassNotFoundException, params);
			goto done;
		}
		if (name[i] == '.')
			name[i] = '/';
	}

	/* Load type using loader */
	if ((type = _jc_load_type(env, loader, name)) == NULL)
		goto done;

	/* Link type if desired */
	if (resolve && _jc_resolve_type(env, type) != JNI_OK)
		goto done;

	/* OK */
	status = JNI_OK;

done:
	/* Convert any NoClassDefFoundError to ClassNotFoundException */
	if (status != JNI_OK
	    && (e = _jc_retrieve_exception(env,
	      vm->boot.types.vmex[_JC_NoClassDefFoundError])) != NULL) {
		jobject eref = NULL;
		_jc_word params[2];

		/* Wrap exception */
		if ((eref = _jc_new_local_native_ref(env, e)) == NULL)
			goto done2;

		/* Extract message string from inner exception */
		params[0] = (_jc_word)*_JC_VMFIELD(vm,
		    e, Throwable, detailMessage, _jc_object *);
		params[1] = (_jc_word)e;

		/* Chain exception inside a new ClassNotFoundException */
		_jc_post_exception_params(env,
		    _JC_ClassNotFoundException, params);

done2:
		/* Clean up */
		_jc_free_local_native_ref(&eref);
	}

	/* Bail out if exception */
	if (status != JNI_OK)
		_jc_throw_exception(env);

	/* Return Class object */
	_JC_ASSERT(type != NULL);
	return type->instance;
}

