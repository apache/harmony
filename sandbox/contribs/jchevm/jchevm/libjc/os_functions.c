
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

#include "libjc.h"

/*

This file contains O/S dependent (but architecture independent) functions.
The following functions must be defined here:

    int
    _jc_num_cpus(_jc_env *env)

	This function should return the number of CPU's. It is used
	to implement the Runtime.availableProcessors() method.

 */

/************************************************************************
 *				FreeBSD					*
 ************************************************************************/

#if defined(__FreeBSD__)

#include <sys/sysctl.h>

int
_jc_num_cpus(_jc_env *env)
{
	static const char *const node = "hw.ncpu";
	_jc_jvm *const vm = env->vm;
	int num;

        if (sysctlbyname(node, NULL, 0, &num, sizeof(num)) == -1) {
		_jc_eprintf(vm, "sysctl(%s): %s", node, strerror(errno));
		return 1;
	}
	return num;
}

/************************************************************************
 *				OS X					*
 ************************************************************************/

/** @todo is this really the right way to detect OS X? */
#elif defined(__APPLE__)

#include <sys/sysctl.h>

int
_jc_num_cpus(_jc_env *env)
{
	static const char *const node = "hw.ncpu";
	_jc_jvm *const vm = env->vm;
	int num;

        if (sysctlbyname(node, NULL, 0, &num, sizeof(num)) == -1) {
		_jc_eprintf(vm, "sysctl(%s): %s", node, strerror(errno));
		return 1;
	}
	return num;
}

/************************************************************************
 *				Linux					*
 ************************************************************************/

#elif defined(__linux__)

int
_jc_num_cpus(_jc_env *env)
{
	_jc_jvm *const vm = env->vm;
        static const char *const file = "/proc/cpuinfo";
	char buf[64];
	size_t len;
	FILE *fp;
	int num;
	int ch;

        if ((fp = fopen("/proc/cpuinfo", "r")) == NULL) {
		_jc_eprintf(vm, "%s: %s", file, strerror(errno));
		return 1;
	}
	for (num = 0; fgets(buf, sizeof(buf), fp) != NULL; ) {
		len = strlen(buf);
		if (len > 0 && buf[len - 1] != '\n')
			while ((ch = getc(fp)) != '\n' && ch != EOF);
		if (strncmp(buf, "processor", 9) == 0)
			num++;
	}
	fclose(fp);
	return num;
}

/************************************************************************
 *				Others					*
 ************************************************************************/

#else
#error "Unsupported operating system"
#endif

