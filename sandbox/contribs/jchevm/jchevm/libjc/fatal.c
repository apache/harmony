
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
 * $Id: fatal.c,v 1.1.1.1 2004/02/20 05:15:34 archiecobbs Exp $
 */

#include "libjc.h"

void
_jc_fatal_error(_jc_jvm *vm, const char *fmt, ...)
{
	va_list args;

	va_start(args, fmt);
	_jc_fatal_error_v(vm, fmt, args);
	/* va_end(args); */
}

void
_jc_fatal_error_v(_jc_jvm *vm, const char *fmt, va_list args)
{
	int (*printer)(FILE *, const char *, va_list);
	void (*aborter)(void);

	/* Print message */
	if (vm != NULL) {
		printer = vm->vfprintf;
		aborter = vm->abort;
	} else {
		printer = vfprintf;
		aborter = abort;
	}
	(*printer)(stderr, "jc: ", args);
	(*printer)(stderr, fmt, args);
	(*printer)(stderr, "\n", args);

	/* Die */
	(*aborter)();

	/* Should never get here */
	fprintf(stderr, "jc: abort() function returned!\n");
	abort();
}

