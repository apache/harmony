
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
static void	_jc_bootstrap_exception(_jc_env *env, int num, const char *msg);

/*
 * Post one of the special, pre-instatiated VM exceptions.
 *
 * 'env' may be NULL.
 */
void
_jc_post_exception(_jc_env *env, int num)
{
	_jc_post_exception_msg(env, num, NULL);
}

/*
 * Post an exception using an exception info structure.
 */
void
_jc_post_exception_info(_jc_env *env)
{
	_JC_ASSERT(env->ex.num >= 0 && env->ex.num < _JC_VMEXCEPTION_MAX);
	if (*env->ex.msg != '\0')
		_jc_post_exception_msg(env, env->ex.num, "%s", env->ex.msg);
	else
		_jc_post_exception_msg(env, env->ex.num, NULL);
#ifndef NDEBUG
	env->ex.num = -1;
#endif
}

/*
 * Post one of the special VM exceptions with the optional message string.
 *
 * 'env' may be NULL.
 */
void
_jc_post_exception_msg(_jc_env *env, int num, const char *fmt, ...)
{
	_jc_jvm *const vm = env->vm;
	jobject sref = NULL;
	char *msgbuf = NULL;
	_jc_word param;
	va_list args;

	/* Sanity check */
	_JC_ASSERT(num >= 0 && num < _JC_VMEXCEPTION_MAX);

	/* Format the message string */
	if (fmt != NULL) {
		va_start(args, fmt);
		msgbuf = _JC_FORMAT_STRINGV(env, fmt, args);
		va_end(args);
	}

	/* Handle an exception thrown during the bootstrap process */
	if (vm == NULL || vm->initialization != NULL) {
		if (vm == NULL || !vm->initialization->may_execute) {
			_jc_bootstrap_exception(env, num, msgbuf);
			return;
		}
		if (vm->boot.objects.vmex[num] == NULL) {
			_JC_ASSERT(vm->initialization != NULL);
			_jc_fatal_error(vm, "%s thrown during bootstrap",
			    _jc_vmex_names[num]);
			return;
		}
	}

	/* Create message String object */
	if (msgbuf != NULL) {
		if ((sref = _jc_new_local_native_ref(env,
		    _jc_new_string(env, msgbuf, strlen(msgbuf)))) == NULL)
			return;
		param = (_jc_word)*sref;
	} else
		param = (_jc_word)NULL;

	/* Create and post the exception */
	_jc_post_exception_params(env, num, &param);

	/* Clean up */
	_jc_free_local_native_ref(&sref);
}

/*
 * Instantiate and post one of the special VM exceptions with the
 * supplied constructor parameters. The parameters must match the
 * constructor resolved for this exception during bootstrapping.
 *
 * This assumes env != NULL and that we are past the point during
 * bootstrap where code execution is enabled.
 *
 * If we are already trying to post it, use one the pre-instantiated
 * one to avoid infinite recursion.
 */
void
_jc_post_exception_params(_jc_env *env, const int num, _jc_word *params)
{
	_jc_jvm *const vm = env->vm;
	_jc_method *const cons = vm->boot.methods.vmex[num].init;
	_jc_object *e;

	/* Sanity check */
	_JC_ASSERT(env != NULL);
	_JC_ASSERT(env->pending == NULL);
	_JC_ASSERT(num >= 0 && num < _JC_VMEXCEPTION_MAX);
	_JC_ASSERT(vm->initialization == NULL
	    || vm->initialization->may_execute);

	/* Don't recursively try throw the same exception */
	if ((env->in_vmex & (1 << num)) != 0) {

		/* Pre-instantiated exception may not exist during bootstrap */
		if ((e = vm->boot.objects.vmex[num]) == NULL) {
			_JC_ASSERT(vm->initialization != NULL);
			_jc_fatal_error(vm, "%s recursively thrown"
			    " during bootstrap", _jc_vmex_names[num]);
			return;
		}

		/* Print verbose message */
		if ((env->vm->verbose_flags
		    & (1 << _JC_VERBOSE_EXCEPTIONS)) != 0) {
			_jc_printf(vm, "[verbose %s: %s recursively thrown]\n",
			    _jc_verbose_names[_JC_VERBOSE_EXCEPTIONS],
			    _jc_vmex_names[num]);
		}

		/* Just post the pre-instantiated version of this exception */
		goto post_it;
	}

	/* Enable recursion detection */
	env->in_vmex |= (1 << num);

	/* Release extra stack space during stack overflows */
	if (num == _JC_StackOverflowError && env->stack_data != NULL) {
		_JC_ASSERT(env->stack_data_end != NULL);
		env->stack_data_end += _JC_JAVA_STACK_MARGIN;
	}

	/* Verbosity */
	if ((env->vm->verbose_flags & (1 << _JC_VERBOSE_EXCEPTIONS)) != 0) {
		char *buf = NULL;
		size_t len;

		/* Decode first paramater if it's a message string */
		if (strncmp(cons->signature, "(Ljava/lang/String;", 19) == 0
		    && params[0] != (_jc_word)NULL) {
			_jc_object *const string = (_jc_object *)params[0];

			len = _jc_decode_string_utf8(env, string, NULL);
			if ((buf = _jc_vm_alloc(env, len + 1)) != NULL)
				_jc_decode_string_utf8(env, string, buf);
		}

		/* Print verbose message */
		_jc_printf(vm, "[verbose %s: posting in thread %p: %s%s%s]\n",
		    _jc_verbose_names[_JC_VERBOSE_EXCEPTIONS], env,
		    _jc_vmex_names[num], buf != NULL ? ": " : "",
		    buf != NULL ? buf : "");
		_jc_vm_free(&buf);
	}

	/* Create new exception object */
	if ((e = _jc_new_object(env, vm->boot.types.vmex[num])) == NULL)
		goto done;

	/* Invoke constructor with supplied parameters */
	if (_jc_invoke_nonvirtual_a(env, cons, e, params) != JNI_OK)
		goto done;

post_it:
	/* Post the exception to this thread */
	env->pending = e;

done:
	/* Clean up */
	env->in_vmex &= ~(1 << num);
	if (num == _JC_StackOverflowError && env->stack_data != NULL)
		env->stack_data_end -= _JC_JAVA_STACK_MARGIN;
}

/*
 * Post an exception using an already-instatiated Throwable object.
 *
 * Any previously posted exception overrides 'exception'.
 */
void
_jc_post_exception_object(_jc_env *env, _jc_object *exception)
{
	_jc_jvm *const vm = env->vm;

	/* Sanity check */
	_JC_ASSERT(_jc_subclass_of(exception, env->vm->boot.types.Throwable));

	/* Verbosity */
	if ((env->vm->verbose_flags & (1 << _JC_VERBOSE_EXCEPTIONS)) != 0) {
		_jc_printf(vm, "[verbose %s: posting in thread %p: ",
		    _jc_verbose_names[_JC_VERBOSE_EXCEPTIONS], env);
		_jc_fprint_exception_headline(env, stdout, exception);
		_jc_printf(vm, "]\n");
	}

	/* Post the exception to this thread */
	_JC_ASSERT(env->pending == NULL);
	env->pending = exception;
}

/*
 * Handle an exception during VM creation. Instead of trying to create
 * an actual Throwable object, we just store the exception type, message,
 * and stack trace in the vm->initialization structure.
 */
static void
_jc_bootstrap_exception(_jc_env *env, int num, const char *msg)
{
	_jc_jvm *const vm = env->vm;
	_jc_initialization *const init = vm->initialization;
	int num_frames;

	/* Sanity check */
	_JC_ASSERT(vm == NULL || vm->initialization != NULL);

	/* Discard any previous bootstrap exception */
	init->ex.num = -1;
	init->num_frames = 0;
	_jc_vm_free(&init->frames);

	/* "Post" new exception */
	init->ex.num = num;

	/* Verbosity */
	VERBOSE(EXCEPTIONS, env->vm, "bootstrap exception: %s: %s",
	    _jc_vmex_names[num], msg);

	/* Avoid infinite recursion */
	if ((env->in_vmex & (1 << _JC_OutOfMemoryError)) != 0)
		return;

	/* Save message */
	if (msg != NULL) {
		strncpy(init->ex.msg, msg, sizeof(init->ex.msg) - 1);
		init->ex.msg[sizeof(init->ex.msg) - 1] = '\0';
	} else
		*init->ex.msg = '\0';

	/* Save stack trace */
	num_frames = _jc_save_stack_frames(env, env, 0, NULL);
	if ((init->frames = _jc_vm_alloc(env,
	    num_frames * sizeof(*init->frames))) == NULL)
		return;
	init->num_frames = num_frames;
	_jc_save_stack_frames(env, env, init->num_frames, init->frames);
}

/*
 * Throw an exception.
 *
 * It must have already been posted.
 */
void
_jc_throw_exception(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;

	/* Sanity check */
	_JC_ASSERT(env->status == _JC_THRDSTAT_RUNNING_NORMAL
	    || env->status == _JC_THRDSTAT_HALTING_NORMAL);
	_JC_ASSERT(vm->initialization == NULL
	    || vm->initialization->may_execute);
	_JC_ASSERT(env->pending != NULL);

	/* Retrieve and un-post the exception */
	env->caught = _jc_retrieve_exception(env, NULL);

	/* Verbosity */
	if ((env->vm->verbose_flags & (1 << _JC_VERBOSE_EXCEPTIONS)) != 0) {
		_jc_printf(vm, "[verbose %s: throwing in thread %p: ",
		    _jc_verbose_names[_JC_VERBOSE_EXCEPTIONS], env);
		_jc_fprint_exception_headline(env, stdout, env->caught);
		_jc_printf(vm, "]\n");
	}

	/* Jump back to most recent invocation of _jc_invoke_jcni_a() */
	siglongjmp(env->catch_list->context, 1);
}

/*
 * Retrieve this thread's pending exception and un-post it, if any.
 *
 * If "type" is not NULL, only retrieve the exception if it is an
 * instance of the type (otherwise return NULL and leave it posted).
 */
_jc_object *
_jc_retrieve_exception(_jc_env *env, _jc_type *type)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *const e = env->pending;

	/* Sanity check */
	_JC_ASSERT(env == _jc_get_current_env());
	_JC_ASSERT(e != NULL);

	/* If type doesn't match, return NULL and leave it posted */
	if (type != NULL && !_jc_subclass_of(e, type))
		return NULL;

	/* Verbosity */
	if ((env->vm->verbose_flags & (1 << _JC_VERBOSE_EXCEPTIONS)) != 0) {
		_jc_printf(vm, "[verbose %s: unposting in thread %p: ",
		    _jc_verbose_names[_JC_VERBOSE_EXCEPTIONS], env);
		_jc_fprint_exception_headline(env, stdout, e);
		_jc_printf(vm, "]\n");
	}

	/* Un-post the exception and return it */
	env->pending = NULL;
	return e;
}

/*
 * Atomically retrieve the cross-posted exception, if any.
 */
_jc_object *
_jc_retrieve_cross_exception(_jc_env *env)
{
	_jc_object *e;

	/*
	 * Check for an exception posted by another thread.
	 * Retrieve it atomically so as to not lose any.
	 */
	while ((e = env->cross_exception) != NULL) {
		if (_jc_compare_and_swap((_jc_word *)&env->cross_exception,
		    (_jc_word)e, (_jc_word)NULL))
			break;
	}
	return e;
}

/*
 * Version of _jc_retrieve_exception() suitable for use by code
 * in the bootstrap path, when the exception may not be a real object.
 * The exeception is not retrieved, it's only unposted.
 */
jboolean
_jc_unpost_exception(_jc_env *env, int num)
{
	_jc_jvm *const vm = env->vm;
	_jc_initialization *const init = vm->initialization;

	/* Are we no longer doing special bootstrap exceptions? */
	if (vm->initialization == NULL || vm->initialization->may_execute) {
		return _jc_retrieve_exception(env,
		    vm->boot.types.vmex[num]) != NULL;
	}

	/* Does "posted" exception type match? */
	if (init->ex.num != num)
		return JNI_FALSE;

	/* Verbosity */
	VERBOSE(EXCEPTIONS, env->vm, "unposting bootstrap exception: %s%s%s",
	    _jc_vmex_names[init->ex.num], *init->ex.msg != '\0' ? ": " : "",
	    init->ex.msg);

	/* Unpost exception */
	_jc_vm_free(&init->frames);
	init->ex.num = -1;
	init->num_frames = 0;
	return JNI_TRUE;
}

/*
 * Extract headline from an exception object.
 */
void
_jc_fprint_exception_headline(_jc_env *env, FILE *fp, _jc_object *e)
{
	_jc_jvm *const vm = env->vm;
	_jc_object *message;
	char *msg_buf;
	size_t msg_len;

	/* Sanity check */
	_JC_ASSERT(e != NULL);
	_JC_ASSERT(_jc_subclass_of(e, vm->boot.types.Throwable));

	/* Print exception class */
	_jc_fprint_noslash(vm, fp, e->type->name);

	/* Any detail message? */
	if ((message = *_JC_VMFIELD(vm, e,
	    Throwable, detailMessage, _jc_object *)) == NULL)
		return;

	/* Print exception detail message, if any */
	msg_len = _jc_decode_string_utf8(env, message, NULL);
	if ((msg_buf = _JC_STACK_ALLOC(env, msg_len + 1)) == NULL) {
		_jc_fprintf(vm, fp, ": [error displaying message: %s%s%s]",
		    _jc_vmex_names[env->ex.num],
		    env->ex.msg != '\0' ? ": " : "", env->ex.msg);
	} else {
		_jc_decode_string_utf8(env, message, msg_buf);
		_jc_fprintf(vm, fp, ": %s", msg_buf);
	}
}

