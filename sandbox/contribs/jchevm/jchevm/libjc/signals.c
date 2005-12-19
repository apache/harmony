
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

/* Internal definitions */
enum {
	_JC_SIGNAL_SEGV,
	_JC_SIGNAL_BUS,
	_JC_SIGNAL_FPE,
	_JC_SIGNAL_USR1,
	_JC_SIGNAL_MAX
};

/* Internal functions */
static void	_jc_signal_action(int sig_num,
			siginfo_t *info, ucontext_t *uctx);
static void	_jc_signal_fallthrough(int sig_index,
			siginfo_t *info, ucontext_t *uctx);

/* Internal variables */
static struct	sigaction _jc_previous[_JC_SIGNAL_MAX];
static const	int _jc_signals[_JC_SIGNAL_MAX] = {
	[_JC_SIGNAL_SEGV]=	SIGSEGV,
#ifdef SIGBUS
	[_JC_SIGNAL_BUS]=	SIGBUS,
#endif
	[_JC_SIGNAL_FPE]=	SIGFPE,
	[_JC_SIGNAL_USR1]=	SIGUSR1,
};

/*
 * Initialize signal handlers.
 */
jint
_jc_init_signals(void)
{
	struct sigaction sa;
	int i;

	/* We want all the context no blocking of signals when handled */
	memset(&sa, 0, sizeof(sa));
	sigemptyset(&sa.sa_mask);
	sa.sa_flags = SA_SIGINFO | SA_NODEFER;
	sa.sa_sigaction = (void (*)(int, siginfo_t *, void *))_jc_signal_action;

	/* Redirect the signals but save the previous handlers */
	for (i = 0; i < _JC_SIGNAL_MAX; i++) {
		if (_jc_signals[i] == 0)
			continue;
		if (sigaction(_jc_signals[i], &sa, &_jc_previous[i]) == -1) {
			fprintf(stderr, "jc: sigaction: %s", strerror(errno));
			while (i-- > 0) {
				(void)sigaction(_jc_signals[i],
				    &_jc_previous[i], NULL);
			}
			return JNI_ERR;
		}
	}

	/* Done */
	return JNI_OK;
}

/*
 * Restore original signal handlers.
 */
void
_jc_restore_signals(void)
{
	int i;

	/* Restore previous signal handlers */
	for (i = 0; i < _JC_SIGNAL_MAX; i++) {
		if (_jc_signals[i] == 0)
			continue;
		if (sigaction(_jc_signals[i], &_jc_previous[i], NULL) == -1)
			fprintf(stderr, "jc: sigaction: %s", strerror(errno));
	}
}

/*
 * Handle a signal.
 */
static void
_jc_signal_action(int sig_num, siginfo_t *info, ucontext_t *uctx)
{
	_jc_env *const env = _jc_get_current_env();
	_jc_jvm *vm = NULL;
	int sig_index;

	/* Get signal index */
	for (sig_index = 0; sig_index < _JC_SIGNAL_MAX
	    && sig_num != _jc_signals[sig_index]; sig_index++);
	if (sig_index == _JC_SIGNAL_MAX)
		goto unexpected;

	/*
	 * Handle SIGUSR1 specially
	 * XXX bug: signal could occur while vm mutex already held.
	 */
	if (sig_index == _JC_SIGNAL_USR1) {
		JavaVM *jvm;
		jsize nvms;

		/* We can only debug the first VM */
		if (JNI_GetCreatedJavaVMs(&jvm, 1, &nvms) != JNI_OK) {
			_jc_signal_fallthrough(sig_index, info, uctx);
			return;
		}
		if (nvms < 1)
			return;
		vm = _JC_JNI2JVM(jvm);
		if (vm->debug_thread != NULL)
			_jc_thread_interrupt_instance(vm, *vm->debug_thread);
		return;
	}

	/* Is this thread attached to a VM? */
	if (env == NULL) {
		_jc_signal_fallthrough(sig_index, info, uctx);
		return;
	}
	vm = env->vm;

	/* Check for double signal */
	if (env->handling_signal)
		_jc_fatal_error(vm, "caught double signal %d", sig_num);
	env->handling_signal = JNI_TRUE;

	/* What state is the thread in? */
	switch (env->status) {
	case _JC_THRDSTAT_RUNNING_NONJAVA:
	case _JC_THRDSTAT_HALTING_NONJAVA:
		env->handling_signal = JNI_FALSE;
		_jc_signal_fallthrough(sig_index, info, uctx);
		return;
	case _JC_THRDSTAT_RUNNING_NORMAL:
	case _JC_THRDSTAT_HALTING_NORMAL:
		break;
	default:
		goto unexpected;
	}

	/*
	 * If this signal occured while there is no Java or C stack, then 
	 * then it didn't occur in JCNI code and so was unexpected.
	 */
	if (env->java_stack == NULL || env->c_stack == NULL)
		goto unexpected;

#ifndef NDEBUG
	/* If the signal occurred while interpeting, it was unexpected */
	if (env->interpreting)
		goto unexpected;

	/*
	 * If the signal occurred while the Java stack was clipped,
	 * then occurred in JNI or native code and so was unexpected.
	 */
	if (env->c_stack->clipped)
	       goto unexpected;
#endif

#if !HAVE_GETCONTEXT
	/* Poor man's getcontext() using signals */
	if (env->ctx != NULL) {
		*env->ctx = uctx->uc_mcontext;
		env->handling_signal = JNI_FALSE;
		return;
	}
#endif

	/* Take the appropriate action */
	switch (sig_index) {
	case _JC_SIGNAL_SEGV:
	case _JC_SIGNAL_BUS:
	    {
		env->handling_signal = JNI_FALSE;
		_jc_post_exception(env, _JC_NullPointerException);
		_jc_throw_exception(env);
	    }

	case _JC_SIGNAL_FPE:
		if (info->si_code != FPE_INTDIV)
			break;
		env->handling_signal = JNI_FALSE;
		_jc_post_exception(env, _JC_ArithmeticException);
		_jc_throw_exception(env);
	default:
		break;
	}

unexpected:
	/* Unexpected signal */
	_jc_fatal_error(vm, "caught unexpected signal %d in thread %p",
	    sig_num, env);
}

/*
 * Handle a signal by falling through to the previously defined handler.
 */
static void
_jc_signal_fallthrough(int sig_index, siginfo_t *info, ucontext_t *uctx)
{
	struct sigaction *const sa = &_jc_previous[sig_index];
	const int sig_num = _jc_signals[sig_index];

	/* Sanity check */
	_JC_ASSERT(sa->sa_sigaction != (void *)_jc_signal_action);

	/* Invoke previous handler */
	if ((sa->sa_flags & SA_SIGINFO) != 0) {
		(*sa->sa_sigaction)(sig_num, info, uctx);
		return;
	}
	if (sa->sa_handler == SIG_IGN)
		return;
	if (sa->sa_handler != SIG_DFL) {
		(*sa->sa_handler)(sig_num);
		return;
	}
	_jc_fatal_error(NULL, "caught unexpected signal %d",
	    _jc_signals[sig_index]);
}

