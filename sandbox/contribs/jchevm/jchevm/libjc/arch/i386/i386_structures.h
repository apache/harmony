
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
 * $Id: i386_structures.h,v 1.1.1.1 2004/02/20 05:15:49 archiecobbs Exp $
 */

#ifndef _ARCH_I386_STRUCTURES_H_
#define _ARCH_I386_STRUCTURES_H_

/*
 * i386 stack frame:
 *
 *	| ...         |
 *	+-------------+
 *	| param2      |
 *	+-------------+
 *	| param1      |
 *	+-------------+
 *	| param0      |
 *	+-------------+
 *	| return addr |
 *	+-------------+
 *	| saved %epb  |    <== %ebp
 *	+-------------+
 *	| locals...   |
 *
 * So all we need is a pointer to the saved %epb register.
 */

typedef _jc_word	*_jc_stack_frame;	/* pointer to saved %epb */

#endif	/* _ARCH_I386_STRUCTURES_H_ */

