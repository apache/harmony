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
#include <string.h>
#include "hycunit.h"

int test_hymmap_startup(struct HyPortLibrary *hyportLibrary);
int test_hymmap_shutdown(struct HyPortLibrary *hyportLibrary);
int test_hymmap_capabilities(struct HyPortLibrary *hyportLibrary);
int test_hymmap_map_and_unmap_file(struct HyPortLibrary *hyportLibrary);

int main (int argc, char **argv, char **envp)
{
    HyPortLibrary hyportLibrary;
    HyPortLibraryVersion portLibraryVersion;
    int ret;

    printf("hymmap:\n");

    HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
    if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
    {
        fprintf(stderr, "portlib init failed\n");
        return 1;
    }

    printf("  portlib initialized\n");
  
    Hytest_init(&hyportLibrary, "Portlib.Hymmap");
    Hytest_func(&hyportLibrary, test_hymmap_startup, "hymmap_startup");
    Hytest_func(&hyportLibrary, test_hymmap_shutdown, "hymmap_shutdown");
    Hytest_func(&hyportLibrary, test_hymmap_capabilities, "hymmap_capabilities");
    Hytest_func(&hyportLibrary, test_hymmap_map_and_unmap_file, "hymmap_map_and_unmap_file");
    
    ret = Hytest_close_and_output(&hyportLibrary);
  
    if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
        fprintf(stderr, "portlib shutdown failed\n");
        return 1;
    }
  
    printf("  portlib shutdown\n");
    return ret;
}

int test_hymmap_startup(struct HyPortLibrary *hyportLibrary) {
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
    rc = hyportLibrary2.mmap_startup(&hyportLibrary2);
    if (0 != rc)
    {
        Hytest_setErrMsg(hyportLibrary, "mmap startup failed: %s (%s)\n",
                         hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
        return -1;
    }
    return 0;
}

int test_hymmap_shutdown(struct HyPortLibrary *hyportLibrary) {
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
    rc = hyportLibrary2.mmap_startup(&hyportLibrary2);
    if (0 != rc)
    {
        Hytest_setErrMsg(hyportLibrary, "mmap shutdown failed: %s (%s)\n",
                         hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
        return -1;
    }
    hyportLibrary2.mmap_shutdown(hyportLibrary);
    return 0;
}

int test_hymmap_capabilities(struct HyPortLibrary *hyportLibrary) {
    I_32 capabilities = hyportLibrary->mmap_capabilities(hyportLibrary);

    if (capabilities < 0) {
        Hytest_setErrMsg(hyportLibrary, "mmap_capabilities returned an unexpected value: %d (%s)\n",
                         capabilities, HY_GET_CALLSITE());
        return -1;
    }
    return 0;
}

int test_hymmap_map_and_unmap_file(struct HyPortLibrary *hyportLibrary) {
    //I_32 ret;
    void *handle;
    char *mappedFile, *execPath, *execName;
    char *emptyFile, *testFile;
    char testString[] = "This is a test file!";
    int pathLen;

    /* Create the path to the test files relative to the location of the test executable */
    hyportLibrary->sysinfo_get_executable_name(hyportLibrary, "unused", &execPath);
    execName = strrchr(execPath, DIR_SEPARATOR) + 1; /* Find the rightmost slash character */
    pathLen = strlen(execPath) - strlen(execName);

    emptyFile = hyportLibrary->mem_allocate_memory(hyportLibrary, pathLen + strlen("shared") + 2 + strlen("emptyFile")); /* +2 for the extra slash and null terminator */
    testFile = hyportLibrary->mem_allocate_memory(hyportLibrary, pathLen + strlen("shared") + 2 + strlen("testFile"));

    strncpy(emptyFile, execPath, pathLen);
    emptyFile[pathLen] = '\0';
    strcat(emptyFile, "shared");
    strcat(emptyFile, DIR_SEPARATOR_STR);
    strcpy(testFile, emptyFile);
    strcat(emptyFile, "emptyFile");
    strcat(testFile, "testFile");

    /* Attempt to map an empty file. Should return NULL and create the file */
    mappedFile = hyportLibrary->mmap_map_file(hyportLibrary, emptyFile, &handle);
    if (mappedFile != NULL) {
        Hytest_setErrMsg(hyportLibrary, "mmap_map_file returned an unexpected value: %p (%s)\n",
                         mappedFile, HY_GET_CALLSITE());
        return -1;
    }

    /* Now call again with the newly created file - should be successful this time */
    mappedFile = hyportLibrary->mmap_map_file(hyportLibrary, testFile, &handle);
    if (mappedFile == NULL) {
        Hytest_setErrMsg(hyportLibrary, "mmap_map_file returned NULL unexpectedly (%s)\n", HY_GET_CALLSITE());
        return -1;
    }

    if (strncmp(testString, mappedFile, strlen(testString)) != 0) {
        Hytest_setErrMsg(hyportLibrary, "Mapped file has incorrect contents: %s (%s)\n", 
                         mappedFile, HY_GET_CALLSITE());
        return -1;
    }

    /* Unmap the file */
    hyportLibrary->mmap_unmap_file(hyportLibrary, handle);

    hyportLibrary->mem_free_memory(hyportLibrary, execPath);
    hyportLibrary->mem_free_memory(hyportLibrary, emptyFile);
    hyportLibrary->mem_free_memory(hyportLibrary, testFile);


    return 0;
}

