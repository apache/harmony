
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
 * $Id: javah.h,v 1.1 2005/01/08 23:25:06 archiecobbs Exp $
 */

#ifndef _JAVAH_H_
#define _JAVAH_H_

#include "libjc.h"
#include <sys/param.h>
#include <err.h>
#include <assert.h>

#define EXCEPTION_MSG_MAX	128

extern int	exception;
extern char	exception_msg[EXCEPTION_MSG_MAX];

extern _jc_env		*_jc_support_init(void);

extern void		javah_header(_jc_classfile *cfile,
				const char *dir, jboolean jni);
extern void		javah_source(_jc_classfile *cfile,
				const char *dir, jboolean jni);
extern _jc_classbytes	*read_classbytes(_jc_env *env,
				const char *dir, const char *name);

#endif	/* _JAVAH_H_ */
