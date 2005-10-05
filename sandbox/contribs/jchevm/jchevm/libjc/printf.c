
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
 * $Id: printf.c,v 1.2 2004/07/18 02:19:12 archiecobbs Exp $
 */

#include "libjc.h"

/*
 * Print something to standard output.
 */
int
_jc_printf(_jc_jvm *vm, const char *fmt, ...)
{
	va_list args;
	int r;

	va_start(args, fmt);
	r = (vm != NULL) ?
	    (*vm->vfprintf)(stdout, fmt, args) :
	    vfprintf(stdout, fmt, args);
	va_end(args);
	return r;
}

/*
 * Print something to standard error.
 */
int
_jc_eprintf(_jc_jvm *vm, const char *fmt, ...)
{
	va_list args;
	int r;

	va_start(args, fmt);
	r = (vm != NULL) ?
	    (*vm->vfprintf)(stderr, fmt, args) :
	    vfprintf(stderr, fmt, args);
	va_end(args);
	return r;
}

/*
 * Print something to wherever.
 */
int
_jc_fprintf(_jc_jvm *vm, FILE *fp, const char *fmt, ...)
{
	va_list args;
	int r;

	va_start(args, fmt);
	r = (vm != NULL) ?
	    (*vm->vfprintf)(fp, fmt, args) :
	    vfprintf(fp, fmt, args);
	va_end(args);
	return r;
}

/*
 * Print out a string (e.g., class name) with dots instead of slashes.
 */
void
_jc_fprint_noslash(_jc_jvm *vm, FILE *fp, const char *s)
{
	while (*s != '\0') {
		_jc_fprintf(vm, fp, "%c", *s == '/' ? '.' : *s);
		s++;
	}
}

