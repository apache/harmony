
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
 * $Id: init.c,v 1.1.1.1 2004/02/20 05:15:35 archiecobbs Exp $
 */

#include "libjc.h"

/* Internal variables */
static jint	_jc_init_result = JNI_ERR;

/* Internal functions */
static void	_jc_do_init(void);

/*
 * One-time global initialization.
 */
jint
_jc_init(void)
{
	static pthread_once_t once = PTHREAD_ONCE_INIT;

	pthread_once(&once, _jc_do_init);
	return _jc_init_result;
}

static void
_jc_do_init(void)
{
	if (_jc_thread_init() != JNI_OK)
		return;
	if (_jc_init_signals() != JNI_OK)
		return;
	_jc_init_result = JNI_OK;
}

