
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

#ifndef _ARCH_STRUCTURES_H_
#define _ARCH_STRUCTURES_H_

/************************************************************************
 *				Overview				*
 ************************************************************************/

/*

This purpose of this file is to define architecture-specific structures.
Currently there are none.

*/

/************************************************************************
 *			Architecture-specific structures		*
 ************************************************************************/

#if defined(__i386__)
#include "i386/i386_structures.h"
#elif defined(__sparc__)
#include "sparc/sparc_structures.h"
#elif defined(__alpha__)
#include "alpha/alpha_structures.h"
#elif defined(__ia64__)
#include "ia64/ia64_structures.h"
#elif defined(__powerpc__)
#include "powerpc/powerpc_structures.h"
#elif defined(__ppc__)
#include "ppc/ppc_structures.h"
#else
#error "Unsupported architecture for architecture-specific structures"
#endif

#endif	/* _ARCH_STRUCTURES_H_ */

