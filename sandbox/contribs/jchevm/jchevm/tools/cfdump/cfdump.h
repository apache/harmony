
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
 * $Id: cfdump.h,v 1.2 2004/07/05 21:03:29 archiecobbs Exp $
 */

#ifndef _CFDUMP_H_
#define _CFDUMP_H_

#include "libjc.h"
#include <err.h>
#include <assert.h>

/* Flags */
#define DUMP_ENCODE_NAMES	0x0001
#define DUMP_TRANS_CLOSURE	0x0002
#define DUMP_SUPERS_ONLY	0x0004

extern void	do_deps(_jc_env *env, _jc_classfile *cf, int flags);
extern _jc_env	*_jc_support_init(void);

#endif	/* _CFDUMP_H_ */
