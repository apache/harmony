
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
 * $Id: arch_libjc.h,v 1.7 2005/07/10 21:03:55 archiecobbs Exp $
 */

#ifndef _ARCH_LIBJC_H_
#define _ARCH_LIBJC_H_

/************************************************************************
 *				Overview				*
 ************************************************************************/

/*

This purpose of this file is to declare and/or define architecture-specific
functions. Inline functions must be defined as "extern inline".

Here is a summary of functions that need to be defined by this file or
an included file:

    int
    _jc_compare_and_swap(volatile _jc_word *word, _jc_word old, _jc_word new)

	A function that performs the following operation atomically:

	    if (*pword == old) {
		    *pword = new;
		    return JNI_TRUE;
	    } else
		    return JNI_FALSE;

    int
    _jc_build_trampoline(u_char *code, _jc_method *method, const void *func)

	A function that builds a trampoline at location pointed to by "code".
	The trampoline will be invoked using normal JCNI calling conventions
	appropriate for "method". The trampoline should do the following:

	    1. Set env->interp to "method".
	    2. Invoke "func" with the same parameters.
	    3. Return what "func" returns.

	"func" is invoked using normal calling conventions as if declared:

		rtype func(_jc_env *env, ...)

	where "rtype" is the return type appropriate for "method". Note
	that normal C promotion applies: int types smaller than int must
	be promoted to int, and float must be promoted to double.

	_jc_build_trampoline() should return the trampoline's length.

	"code" may be NULL, in which case just the length of the trampoline
	should be returned. Otherwise, "code" will point to aligned memory.

	The generated code does not need to be position-independent.

    void
    _jc_dynamic_invoke(const void *func, int jcni, int nparams,
        const u_char *ptypes, int nwords, _jc_word *words, _jc_value *retval)

	Dynamically constructed C function call. Should invoke the C function
	"func" with supplied parameters. Return value should be stored in
	*retval. Must not break the stack frame pointer chain. "ptypes" has
	length "nparams" + 1 giving the parameter and return value types.
	"words" points to the Java style parameters (i.e., 2 for long/double).

	'jcni' is non-zero for JNCI functions, zero for normal C functions.
	Can be ignored if JNCI calling conventions are the same as normal
	calling conventions for a particular architecture.

    void
    _jc_iflush(const void *mem, size_t len)

	This function should flush the CPU instruction cache for
	the given range of memory (if appropriate), as the memory
	contains newly loaded executable code.

Stack Functions
---------------

The following functions all manipulate '_jc_stack_frame' objects.
A stack frame object is associated with a specific function invocation.
Typically a '_jc_stack_frame' is just functionA()'s saved frame pointer.

These functions must not call any other functions that could allocate
memory, etc. See "arch/arch_structures.h".

    void
    _jc_stack_frame_init(_jc_stack_frame *framep)

	Initialize 'frame' to the invalid stack frame value.

    jboolean
    _jc_stack_frame_valid(_jc_stack_frame frame)

	Returns JNI_TRUE if the frame is not the invalid frame.

    jboolean
    _jc_stack_frame_equal(_jc_stack_frame frame1, _jc_stack_frame frame2)

	Returns JNI_TRUE if the two frames are the same.

    void
    _jc_stack_frame_current(_jc_stack_frame *framep)

	Initialize 'frame' to refer to the stack frame associated with
	the invocation from functionA() -> functionB(), where functionB()
	is the function calling _jc_stack_frame_current().

    void
    _jc_stack_frame_next(_jc_stack_frame *frame, const void **pcp)

	Advance 'frame' to the frame one deeper on the stack
	and update *pcp to be the PC address in the deeper frame.

    const void *
    _jc_stack_frame_sp(_jc_stack_frame frame)

	Convert a stack frame into a stack address. If 'frame' is associated
	with functionA(), then the stack address is only required to include
	the stack variables of the fuction that invoked functionA() (but not
	functionA() itself). The pointer must be aligned to _JC_STACK_ALIGN.

    const void *
    _jc_mcontext_sp(const mcontext_t *mctx)

    	Returns the saved stack pointer from an mcontext_t structure.

    const void *
    _jc_mcontext_pc(const mcontext_t *mctx)

    	Returns the saved PC from an mcontext_t structure. This should
	correspond to the PC address in functionA() where functionA() is
	the function that called getcontext() or caught the signal.

    _jc_stack_frame
    _jc_mcontext_frame(const mcontext_t *mctx)

    	Returns the stack frame from an mcontext_t structure. This
	should correspond to functionA() calling functionB(), where
	functionA() is the function that called getcontext() or
	caught the signal.

    const void *
    _jc_signal_fault_address(int sig_num, siginfo_t *info, ucontext_t *uctx)

	This function is invoked during signal handling. The three
	parameters are copied unchanged by the POSIX SIG_INFO style
	handler. This function should return the fault address, i.e.,
	the illegal memory address which could not be accessed, if any.

*/

/************************************************************************
 *			Function declarations				*
 ************************************************************************/

/* Misc functions */
extern int		_jc_compare_and_swap(volatile _jc_word *word,
				_jc_word old, _jc_word new);
extern void		_jc_iflush(const void *mem, size_t len);
extern int		_jc_build_trampoline(u_char *code, _jc_method *method,
				const void *func);
extern void		_jc_dynamic_invoke(const void *func, int jcni,
				int nparams, const u_char *ptypes, int nwords,
				_jc_word *words, _jc_value *retval);

/* Stack frame functions */
extern void		_jc_stack_frame_init(_jc_stack_frame *framep);
extern void		_jc_stack_frame_next(_jc_stack_frame *prev,
				const void **pcp);
extern jboolean		_jc_stack_frame_valid(_jc_stack_frame frame);
extern jboolean		_jc_stack_frame_equal(_jc_stack_frame frame1,
			_jc_stack_frame frame2);
extern const void	*_jc_stack_frame_sp(_jc_stack_frame frame);
extern const void	*_jc_mcontext_sp(const mcontext_t *mctx);
extern const void	*_jc_mcontext_pc(const mcontext_t *mctx);
extern _jc_stack_frame	_jc_mcontext_frame(const mcontext_t *mctx);
extern const void	*_jc_signal_fault_address(int sig_num,
				siginfo_t *info, ucontext_t *uctx);

/************************************************************************
 *			Architecture-specific functions			*
 ************************************************************************/

#if defined(__i386__)
#include "i386/i386_libjc.h"
#elif defined(__sparc__)
#include "sparc/sparc_libjc.h"
#elif defined(__alpha__)
#include "alpha/alpha_libjc.h"
#elif defined(__ia64__)
#include "ia64/ia64_libjc.h"
#elif defined(__powerpc__)
#include "powerpc/powerpc_libjc.h"
#else
#error "Unsupported architecture for architecture-specific functions"
#endif

#endif	/* _ARCH_LIBJC_H_ */

