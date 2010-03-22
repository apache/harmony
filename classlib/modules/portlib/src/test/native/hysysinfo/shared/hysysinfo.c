/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include "hyport.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "hycunit.h"

int test_hysysinfo_startup(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_shutdown(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_pid(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_physical_memory(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_OS_version(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_env(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_CPU_architecture(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_OS_type(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_classpathSeparator(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_executable_name(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_number_CPUs(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_username(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_DLPAR_enabled(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_DLPAR_max_CPUs(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_weak_memory_consistency(struct HyPortLibrary *hyportLibrary);
int test_hysysinfo_get_processing_capacity(struct HyPortLibrary *hyportLibrary);


int main (int argc, char **argv, char **envp)
{
    HyPortLibrary hyportLibrary;
    HyPortLibraryVersion portLibraryVersion;
    int ret;

    printf("hysysinfo:\n");

    HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
    if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
    {
        fprintf(stderr, "portlib init failed\n");
        return 1;
    }

    printf("  portlib initialized\n");
  
    Hytest_init(&hyportLibrary, "Portlib.Hysysinfo");
    Hytest_func(&hyportLibrary, test_hysysinfo_startup, "hysysinfo_startup");
    Hytest_func(&hyportLibrary, test_hysysinfo_shutdown, "hysysinfo_shutdown");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_pid, "hysysinfo_get_pid");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_physical_memory, "hysysinfo_get_physical_memory");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_OS_version, "hysysinfo_get_OS_version");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_env, "hysysinfo_get_env");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_CPU_architecture, "hysysinfo_get_CPU_architecture");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_OS_type, "hysysinfo_get_OS_type");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_classpathSeparator, "hysysinfo_get_classpathSeparator");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_executable_name, "hysysinfo_get_executable_name");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_number_CPUs, "hysysinfo_get_number_CPUs");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_username, "hysysinfo_get_username");
    Hytest_func(&hyportLibrary, test_hysysinfo_DLPAR_enabled, "hysysinfo_DLPAR_enabled");
    Hytest_func(&hyportLibrary, test_hysysinfo_DLPAR_max_CPUs, "hysysinfo_DLPAR_max_CPUs");
    Hytest_func(&hyportLibrary, test_hysysinfo_weak_memory_consistency, "hysysinfo_weak_memory_consistency");
    Hytest_func(&hyportLibrary, test_hysysinfo_get_processing_capacity, "hysysinfo_get_processing_capacity");
    
    ret = Hytest_close_and_output(&hyportLibrary);
  
    if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
        fprintf(stderr, "portlib shutdown failed\n");
        return 1;
    }
  
    printf("  portlib shutdown\n");
    return ret;
}

int test_hysysinfo_startup(struct HyPortLibrary *hyportLibrary) {
    HyPortLibrary hyportLibrary2;
    HyPortLibraryVersion portLibraryVersion;
    I_32 rc;
    HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
    if (0 != hyport_init_library (&hyportLibrary2, &portLibraryVersion,
                                  sizeof (HyPortLibrary)))
    {
        fprintf(stderr, "portlib init failed\n");
        return -1;
    }
    rc = hyportLibrary2.sysinfo_startup(&hyportLibrary2);
    if (0 != rc)
    {
        Hytest_setErrMsg(hyportLibrary, "sysinfo startup failed: %s (%s)\n",
                         hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
        return -1;
    }
    return 0;
}

int test_hysysinfo_shutdown(struct HyPortLibrary *hyportLibrary) {
    HyPortLibrary hyportLibrary2;
    HyPortLibraryVersion portLibraryVersion;
    I_32 rc;
    HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
    if (0 != hyport_init_library (&hyportLibrary2, &portLibraryVersion,
                                  sizeof (HyPortLibrary)))
    {
        fprintf(stderr, "portlib init failed\n");
        return -1;
    }
    rc = hyportLibrary2.sysinfo_startup(&hyportLibrary2);
    if (0 != rc)
    {
        Hytest_setErrMsg(hyportLibrary, "sysinfo shutdown failed: %s (%s)\n",
                         hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
        return -1;
    }
    hyportLibrary2.sysinfo_shutdown(hyportLibrary);
    return 0;
}

int test_hysysinfo_get_pid(struct HyPortLibrary *hyportLibrary) {
    UDATA pid = hyportLibrary->sysinfo_get_pid(hyportLibrary);
    return 0;
}

int test_hysysinfo_get_physical_memory(struct HyPortLibrary *hyportLibrary) {
    U_64 physicalMem = hyportLibrary->sysinfo_get_physical_memory(hyportLibrary);
    if (physicalMem <= 0) {
        Hytest_setErrMsg(hyportLibrary, "hysysinfo_get_physical_memory returned an unexpected value: %d (%s)\n", 
                         physicalMem, HY_GET_CALLSITE());
        return -1;
    }
    return 0;
}

int test_hysysinfo_get_OS_version(struct HyPortLibrary *hyportLibrary) {
    const char *osVersion = hyportLibrary->sysinfo_get_OS_version(hyportLibrary);
    return 0;
}

int test_hysysinfo_get_env(struct HyPortLibrary *hyportLibrary) {
    char singleChar;
    char *buffer = NULL;
    IDATA ret;

    /* Try to get a non-existent environment variable. Should return -1 */
    ret = hyportLibrary->sysinfo_get_env(hyportLibrary, "madeUpEnvVar", &singleChar, 1);
    if (ret != -1) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_env returned an unexpected value: %d (%s)\n", ret, HY_GET_CALLSITE());
        return -1;
    }

    /* Now try to get an existing variable (PATH) with a buffer that is too small. Should return the real size of the buffer required */
    ret = hyportLibrary->sysinfo_get_env(hyportLibrary, "PATH", buffer, 0);
    if (ret <= 0) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_env returned an unexpected value: %d (%s)\n", ret, HY_GET_CALLSITE());
        return -1;
    }

    /* Allocate the required buffer size and get the PATH. This time we should be successful */
    buffer = hyportLibrary->mem_allocate_memory(hyportLibrary, ret);
    if (!buffer) {
        Hytest_setErrMsg(hyportLibrary, "Could not allocate memory for buffer (%s)\n", HY_GET_CALLSITE());
        return -1;
    }
    ret = hyportLibrary->sysinfo_get_env(hyportLibrary, "PATH", buffer, ret);
    if (ret != 0) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_env returned an unexpected value: %d (%s)\n", ret, HY_GET_CALLSITE());
        hyportLibrary->mem_free_memory(hyportLibrary, buffer);
        return -1;
    }

    hyportLibrary->mem_free_memory(hyportLibrary, buffer);

    return 0;
}

int test_hysysinfo_get_CPU_architecture(struct HyPortLibrary *hyportLibrary) {
    const char *cpuArch = hyportLibrary->sysinfo_get_CPU_architecture(hyportLibrary);
    return 0;
}

int test_hysysinfo_get_OS_type(struct HyPortLibrary *hyportLibrary) {
    const char *osType = hyportLibrary->sysinfo_get_OS_type(hyportLibrary);
    return 0;
}

int test_hysysinfo_get_classpathSeparator(struct HyPortLibrary *hyportLibrary) {
    U_16 separator = hyportLibrary->sysinfo_get_classpathSeparator(hyportLibrary);

    if (separator != PATH_SEPARATOR) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_classpathSeparator returned an unexpected path separator: %c (%s)\n", 
                         (char)separator, HY_GET_CALLSITE());
        return -1;
    }

    return 0;
}

int test_hysysinfo_get_executable_name(struct HyPortLibrary *hyportLibrary) {
    char *execName;
    IDATA ret = hyportLibrary->sysinfo_get_executable_name(hyportLibrary, "unused", &execName);
    if (ret != 0) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_executable_name returned an error value: %d (%s)\n", 
                         ret, HY_GET_CALLSITE());
        return -1;
    }

    if (strstr(execName, "hysysinfo") == NULL) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_executable_name returned an incorrect executable name: %s (%s)\n", 
                         execName, HY_GET_CALLSITE());
        hyportLibrary->mem_free_memory(hyportLibrary, execName);
        return -1;
    }

    /* It is our responsibility to free the allocated memory here */
    hyportLibrary->mem_free_memory(hyportLibrary, execName);

    return 0;
}

int test_hysysinfo_get_number_CPUs(struct HyPortLibrary *hyportLibrary) {
    UDATA numCPUs = hyportLibrary->sysinfo_get_number_CPUs(hyportLibrary);

    if (numCPUs <= 0) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_number_CPUs returned an invalid number of CPUs: %d (%s)\n", 
                         numCPUs, HY_GET_CALLSITE());
        return -1;
    }
    return 0;
}

int test_hysysinfo_get_username(struct HyPortLibrary *hyportLibrary) {
    char *buffer = NULL;
    IDATA ret;

    /* Try to get the username with a 0 size buffer. Should return the real size of the buffer required */
    ret = hyportLibrary->sysinfo_get_username(hyportLibrary, buffer, 0);
    if (ret <= 0) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_username returned an unexpected value: %d (%s)\n", ret, HY_GET_CALLSITE());
        return -1;
    }

    /* Allocate the required buffer size and get the PATH. This time we should be successful */
    buffer = hyportLibrary->mem_allocate_memory(hyportLibrary, ret);
    if (!buffer) {
        Hytest_setErrMsg(hyportLibrary, "Could not allocate memory for buffer (%s)\n", HY_GET_CALLSITE());
        return -1;
    }
    ret = hyportLibrary->sysinfo_get_username(hyportLibrary, buffer, ret);
    if (ret != 0) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_username returned an unexpected value: %d (%s)\n", ret, HY_GET_CALLSITE());
        hyportLibrary->mem_free_memory(hyportLibrary, buffer);
        return -1;
    }

    hyportLibrary->mem_free_memory(hyportLibrary, buffer);

    return 0;
}

int test_hysysinfo_DLPAR_enabled(struct HyPortLibrary *hyportLibrary) {
    UDATA enabled = hyportLibrary->sysinfo_DLPAR_enabled(hyportLibrary);
    if ((enabled < 0) || (enabled > 1)) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_DLPAR_enabled returned an invalid value: %d (%s)\n", enabled, HY_GET_CALLSITE());
        return -1;
    }

    return 0;
}

int test_hysysinfo_DLPAR_max_CPUs(struct HyPortLibrary *hyportLibrary) {
    UDATA maxCPUs = hyportLibrary->sysinfo_DLPAR_max_CPUs(hyportLibrary);
    if (maxCPUs <= 0) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_DLPAR_max_CPUs returned an invalid value: %d (%s)\n", maxCPUs, HY_GET_CALLSITE());
        return -1;
    }

    return 0;
}

int test_hysysinfo_weak_memory_consistency(struct HyPortLibrary *hyportLibrary) {
    UDATA weakMemCon = hyportLibrary->sysinfo_weak_memory_consistency(hyportLibrary);
    if ((weakMemCon < 0) || (weakMemCon > 1)) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_weak_memory_consistency returned an invalid value: %d (%s)\n", weakMemCon, HY_GET_CALLSITE());
        return -1;
    }

    return 0;
}

int test_hysysinfo_get_processing_capacity(struct HyPortLibrary *hyportLibrary) {
    UDATA procCap = hyportLibrary->sysinfo_get_processing_capacity(hyportLibrary);
    if (procCap <= 0) {
        Hytest_setErrMsg(hyportLibrary, "sysinfo_get_processing_capacity returned an invalid value: %d (%s)\n", procCap, HY_GET_CALLSITE());
        return -1;
    }

    return 0;
}
