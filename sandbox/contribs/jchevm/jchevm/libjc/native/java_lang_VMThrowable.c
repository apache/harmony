
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
#include "java_lang_VMThrowable.h"

/* Used to align byte[] array bytes to _JC_FULL_ALIGNMENT */
#define _JC_BYTE_ARRAY_PAD	(_JC_ROUNDUP2(_JC_OFFSETOF(		\
				    _jc_byte_array, elems),		\
				      _JC_FULL_ALIGNMENT)		\
				    - _JC_OFFSETOF(_jc_byte_array, elems))

/*
 * static final native VMThrowable fillInStackTrace(Throwable)
 */
_jc_object * _JC_JCNI_ATTR
JCNI_java_lang_VMThrowable_fillInStackTrace(_jc_env *env, _jc_object *throwable)
{
	_jc_jvm *const vm = env->vm;
	_jc_byte_array *bytes = NULL;
	jobject bytes_ref = NULL;
	_jc_saved_frame *frames;
	_jc_object *vmt;
	int num_frames;

	/* Count the number of frames to save */
	num_frames = _jc_save_stack_frames(env, env, 0, NULL);

	/* Create byte[] array big enough to hold aligned frame array */
	if ((bytes_ref = _jc_new_local_native_ref(env,
	    (_jc_object *)_jc_new_array(env,
	      vm->boot.types.prim_array[_JC_TYPE_BYTE],
	      num_frames * sizeof(*frames) + _JC_BYTE_ARRAY_PAD))) == NULL)
		_jc_throw_exception(env);
	bytes = (_jc_byte_array *)*bytes_ref;
	frames = (_jc_saved_frame *)_JC_ROUNDUP2(
	    (_jc_word)bytes->elems, _JC_FULL_ALIGNMENT);

	/* Fill in trace */
	_jc_save_stack_frames(env, env, num_frames, frames);

	/* Create VMThrowable */
	if ((vmt = _jc_new_object(env, vm->boot.types.VMThrowable)) == NULL) {
		_jc_free_local_native_ref(&bytes_ref);
		_jc_throw_exception(env);
	}

	/* Save byte[] array in 'vmdata' field of VMThrowable */
	*_JC_VMFIELD(vm, vmt, VMThrowable, vmdata, _jc_byte_array *) = bytes;

	/* Clean up */
	_jc_free_local_native_ref(&bytes_ref);

	/* Done */
	return vmt;
}

/*
 * final native StackTraceElement[] getStackTrace(Throwable)
 */
_jc_object_array * _JC_JCNI_ATTR
JCNI_java_lang_VMThrowable_getStackTrace(_jc_env *env, _jc_object *this,
	_jc_object *throwable)
{
	_jc_jvm *const vm = env->vm;
	_jc_object_array *array = NULL;
	_jc_saved_frame *frames;
	jobject array_ref = NULL;
	jboolean ok = JNI_FALSE;
	_jc_byte_array *bytes;
	int num_frames;
	int i;

	/* Get saved frames from 'vmdata' byte[] array */
	bytes = *_JC_VMFIELD(vm, this, VMThrowable, vmdata, _jc_byte_array *);
	if (bytes == NULL) {
		num_frames = 0;
		frames = NULL;
	} else {
		num_frames = bytes->length / sizeof(*frames);
		frames = (_jc_saved_frame *)_JC_ROUNDUP2(
		    (_jc_word)bytes->elems, _JC_FULL_ALIGNMENT);
	}

	/* Create array */
	if ((array_ref = _jc_new_local_native_ref(env,
	    (_jc_object *)_jc_new_array(env,
	      vm->boot.types.StackTraceElement_array, num_frames))) == NULL)
		goto done;
	array = (_jc_object_array *)*array_ref;

	/* Fill the array with StackTraceElement's */
	for (i = 0; i < num_frames; i++) {
		_jc_saved_frame *frame = &frames[i];
		jboolean loop_ok = JNI_FALSE;
		jobject file_ref;
		jobject class_ref;
		jobject method_ref;
		jobject ste_ref;
		char *cname = NULL;
		_jc_method *method;
		_jc_type *class;
		int jline;
		char *s;

		/* Get method and class */
		method = frame->method;
		class = method->class;
		_JC_ASSERT(class != NULL);

		/* Initialize references */
		file_ref = NULL;
		class_ref = NULL;
		method_ref = NULL;
		ste_ref = NULL;

		/* Create String for source file name */
		if (class->u.nonarray.source_file != NULL
		    && (file_ref = _jc_new_local_native_ref(env,
		      _jc_new_string(env, class->u.nonarray.source_file,
		       strlen(class->u.nonarray.source_file)))) == NULL)
			goto fail;

		/* Get Java line number, if known */
		jline = frame->ipc == -1 ?
		    0 : _jc_interp_pc_to_jline(method, frame->ipc);

		/* Create String for Class name, converting '/' -> '.' */
		if ((cname = _jc_vm_strdup(env, class->name)) == NULL) {
			_jc_post_exception_info(env);
			goto fail;
		}
		for (s = cname; *s != '\0'; s++) {
			if (*s == '/')
				*s = '.';
		}
		if ((class_ref = _jc_new_local_native_ref(env,
		    _jc_new_string(env, cname, strlen(cname)))) == NULL)
			goto fail;

		/* Create String for Method name */
		if ((method_ref = _jc_new_local_native_ref(env,
		    _jc_new_string(env, method->name,
		      strlen(method->name)))) == NULL)
			goto fail;

		/* Allocate new StackTraceElement */
		if ((ste_ref = _jc_new_local_native_ref(env,
		    _jc_new_object(env,
		      vm->boot.types.StackTraceElement))) == NULL)
			goto fail;

		/* Invoke constructor */
		if (_jc_invoke_nonvirtual(env,
		    vm->boot.methods.StackTraceElement.init, *ste_ref,
		    file_ref != NULL ? *file_ref : NULL,
		    (jint)(jline != 0 ? jline : -1), *class_ref,
		    *method_ref, _JC_ACC_TEST(method, NATIVE)) != JNI_OK)
			goto fail;

		/* Set array element */
		array->elems[~i] = *ste_ref;

		/* OK */
		loop_ok = JNI_TRUE;

	fail:
		/* Free temporary stuff */
		_jc_free_local_native_ref(&file_ref);
		_jc_free_local_native_ref(&class_ref);
		_jc_free_local_native_ref(&method_ref);
		_jc_free_local_native_ref(&ste_ref);
		_jc_vm_free(&cname);

		/* Bail out if we got an exception */
		if (!loop_ok)
			goto done;
	}

	/* OK */
	ok = JNI_TRUE;

done:
	/* Free local native reference */
	_jc_free_local_native_ref(&array_ref);

	/* Bail if we got an exception */
	if (!ok)
		_jc_throw_exception(env);

	/* Done */
	return array;
}

