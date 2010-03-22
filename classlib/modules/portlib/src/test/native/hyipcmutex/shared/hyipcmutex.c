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
#include <stdio.h>
#include "hycunit.h"

int test_hyipcmutex_startup(struct HyPortLibrary *hyportLibrary);
int test_hyipcmutex_shutdown(struct HyPortLibrary *hyportLibrary);
int test_hyipcmutex_acquire_and_release(struct HyPortLibrary *hyportLibrary);

int main (int argc, char **argv, char **envp)
{
    HyPortLibrary hyportLibrary;
    HyPortLibraryVersion portLibraryVersion;
    int ret;

    printf("hyipcmutex:\n");

    HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
    if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
    {
        fprintf(stderr, "portlib init failed\n");
        return 1;
    }

    printf("  portlib initialized\n");
  
    Hytest_init(&hyportLibrary, "Portlib.Hyipcmutex");
    Hytest_func(&hyportLibrary, test_hyipcmutex_startup, "hyipcmutex_startup");
    Hytest_func(&hyportLibrary, test_hyipcmutex_shutdown, "hyipcmutex_shutdown");
    Hytest_func(&hyportLibrary, test_hyipcmutex_acquire_and_release, "hyipcmutex_acquire_and_release");
    
    ret = Hytest_close_and_output(&hyportLibrary);
  
    if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
        fprintf(stderr, "portlib shutdown failed\n");
        return 1;
    }
  
    printf("  portlib shutdown\n");
    return ret;
}

int test_hyipcmutex_startup(struct HyPortLibrary *hyportLibrary) {
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
    rc = hyportLibrary2.ipcmutex_startup(&hyportLibrary2);
    if (0 != rc)
    {
        Hytest_setErrMsg(hyportLibrary, "ipcmutex startup failed: %s (%s)\n",
                         hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
        return -1;
    }
    return 0;
}

int test_hyipcmutex_shutdown(struct HyPortLibrary *hyportLibrary) {
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
    rc = hyportLibrary2.ipcmutex_startup(&hyportLibrary2);
    if (0 != rc)
    {
        Hytest_setErrMsg(hyportLibrary, "ipcmutex shutdown failed: %s (%s)\n",
                         hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
        return -1;
    }
    hyportLibrary2.ipcmutex_shutdown(hyportLibrary);
    return 0;
}

int test_hyipcmutex_acquire_and_release(struct HyPortLibrary *hyportLibrary) {
    I_32 ret;

    /* Attempt to acquire a mutex that does not already exist */
    ret = hyportLibrary->ipcmutex_acquire(hyportLibrary, "myTestMutex");
    if (ret != 0) {
        Hytest_setErrMsg(hyportLibrary, "ipcmutex_acquire failed with return value: %d (%s)\n", 
                         ret, HY_GET_CALLSITE());
        return -1;
    }

    /* Now release the mutex we just created */
    ret = hyportLibrary->ipcmutex_release(hyportLibrary, "myTestMutex");
    if (ret != 0) {
        Hytest_setErrMsg(hyportLibrary, "ipcmutex_release failed with return value: %d (%s)\n", 
                         ret, HY_GET_CALLSITE());
        return -1;
    }

    /* Attempt to release a mutex that does not exist */
    ret = hyportLibrary->ipcmutex_release(hyportLibrary, "myTestMutex2");
    if (ret != -1) {
        Hytest_setErrMsg(hyportLibrary, "ipcmutex_release returned an unexpected value: %d (%s)\n", 
                         ret, HY_GET_CALLSITE());
        return -1;
    }

    return 0;
}

