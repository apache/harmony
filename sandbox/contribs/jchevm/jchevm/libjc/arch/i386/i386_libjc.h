
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
 * $Id: i386_libjc.h,v 1.7 2005/05/08 21:12:07 archiecobbs Exp $
 */

#ifndef _ARCH_I386_LIBJC_H_
#define _ARCH_I386_LIBJC_H_

/*
 * Architecture-specific functions for i386 architecture.
 */

extern inline int
_jc_compare_and_swap(volatile _jc_word *word, _jc_word old, _jc_word new)
{
	_jc_word previous;

	asm __volatile__ (
	    "lock\n"
	    "\tcmpxchgl %1,%2\n"
	    : "=a" (previous)
	    : "q" (new), "m" (*word), "0" (old)
	    : "memory");
	return previous == old;
}

extern inline void
_jc_iflush(const void *mem, size_t len)
{
	/* nothing to do: i386 has coherent data and instruction caches */
}

extern inline void
_jc_stack_frame_init(_jc_stack_frame *framep)
{
	*framep = NULL;
}

#define _jc_stack_frame_current(framep)					\
    do {								\
	*(framep) = __builtin_frame_address(0);				\
    } while (0)

extern inline void
_jc_stack_frame_next(_jc_stack_frame *framep, const void **pcp)
{
	_jc_word *const ebp = *framep;

	*pcp = (const void *)ebp[1];	/* saved %eip is one slot above %ebp */
	*framep = (_jc_word *)ebp[0];	/* %ebp points to saved %ebp */
}

extern inline jboolean
_jc_stack_frame_valid(_jc_stack_frame frame)
{
	return frame != NULL;
}

extern inline jboolean
_jc_stack_frame_equal(_jc_stack_frame frame1, _jc_stack_frame frame2)
{
	return frame1 == frame2;
}

extern inline const void *
_jc_stack_frame_sp(_jc_stack_frame frame)
{
	return (const void *)frame;
}

#ifdef __FreeBSD__

extern inline const void *
_jc_mcontext_sp(const mcontext_t *mctx)
{
	return (const void *)mctx->mc_esp;
}

extern inline const void *
_jc_mcontext_pc(const mcontext_t *mctx)
{
	return (const void *)mctx->mc_eip;
}

extern inline _jc_stack_frame
_jc_mcontext_frame(const mcontext_t *mctx)
{
	return (_jc_word *)mctx->mc_ebp;
}

extern inline const void *
_jc_signal_fault_address(int sig_num, siginfo_t *info, ucontext_t *uctx)
{
	return (const void *)uctx->uc_mcontext.mc_err;
}

#elif defined(__linux__)

extern inline const void *
_jc_mcontext_sp(const mcontext_t *mctx)
{
	return (const void *)mctx->gregs[REG_ESP];
}

extern inline const void *
_jc_mcontext_pc(const mcontext_t *mctx)
{
	return (const void *)mctx->gregs[REG_EIP];
}

extern inline _jc_stack_frame
_jc_mcontext_frame(const mcontext_t *mctx)
{
	return (_jc_word *)mctx->gregs[REG_EBP];
}

extern inline const void *
_jc_signal_fault_address(int sig_num, siginfo_t *info, ucontext_t *uctx)
{
	return (const void *)info->si_addr;
}

#else
#error "Unsupported O/S for i386 machine context functions"
#endif

#endif	/* _ARCH_I386_LIBJC_H_ */

