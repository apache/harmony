
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

#ifdef __FreeBSD__

extern inline const void *
_jc_mcontext_sp(const mcontext_t *mctx)
{
	return (const void *)mctx->mc_esp;
}

#elif defined(__linux__)

extern inline const void *
_jc_mcontext_sp(const mcontext_t *mctx)
{
	return (const void *)mctx->gregs[REG_ESP];
}

#else
#error "Unsupported O/S for i386 machine context functions"
#endif

#endif	/* _ARCH_I386_LIBJC_H_ */

