
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
 */

#ifndef _ARCH_PPC_LIBJC_H_
#define _ARCH_PPC_LIBJC_H_

#ifndef __ppc__
#error "This header file is meant for the PPC architecture only."
#endif /* __ppc__ */

/* stdio.h included to print warning outputs in the stub functions which return
 * NULL for the moment... 
 */
#include <stdio.h>

/*
 * Architecture-specific functions for ppc architecture.
 */

/**
 * @todo Left unimplemented for the moment.
 */
extern inline int
_jc_compare_and_swap(volatile _jc_word *word, _jc_word old, _jc_word new)
{
	if (*word == old) {
		*word = new;
		return JNI_TRUE;
	} else {
		return JNI_FALSE;
	}
}

/**
 * @todo Is there nothing todo on the ppc too?
 */
extern inline void
_jc_iflush(const void *mem, size_t len)
{
	/* From the original header file: "nothing to do: i386 has coherent data and
	 * instruction caches"
	 */
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

/**
 * @todo Returning NULL to be compile-clean for the moment.
 */
extern inline const void *
_jc_mcontext_sp(const mcontext_t *mctx)
{
	fprintf(stderr, "WARNING: call to unimplemented function _jc_mcontext_sp(...)\n");
	exit(0);
	return (const void *) NULL;
}

/**
 * @todo Returning NULL to be compile-clean for the moment.
 */
extern inline const void *
_jc_mcontext_pc(const mcontext_t *mctx)
{
	fprintf(stderr, "WARNING: call to unimplemented function _jc_mcontext_pc(...)\n");
	exit(0);
	return (const void *) NULL;
}

/**
 * @todo Returning NULL to be compile-clean for the moment.
 */
extern inline _jc_stack_frame
_jc_mcontext_frame(const mcontext_t *mctx)
{
	fprintf(stderr, "WARNING: call to unimplemented function _jc_mcontext_frame(...)\n");
	exit(0);
	return (_jc_word *) NULL;
}

/**
 * @todo Returning NULL to be compile-clean for the moment.
 */
extern inline const void *
_jc_signal_fault_address(int sig_num, siginfo_t *info, ucontext_t *uctx)
{
	fprintf(stderr, "WARNING: call to unimplemented function _jc_signal_falut_address(...)\n");
	exit(0);
	return (const void *) NULL;
}

#endif	/* _ARCH_PPC_LIBJC_H_ */

