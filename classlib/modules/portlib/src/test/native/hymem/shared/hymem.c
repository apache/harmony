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

int test_hymem_allocate_memory(struct HyPortLibrary *hyportLibrary);
int test_hymem_allocate_memory_callSite(struct HyPortLibrary *hyportLibrary);
int test_hymem_free_memory(struct HyPortLibrary *hyportLibrary);
int test_hymem_reallocate_memory(struct HyPortLibrary *hyportLibrary);
int test_hymem_shutdown(struct HyPortLibrary *hyportLibrary);
int test_hymem_startup(struct HyPortLibrary *hyportLibrary);

int main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  int ret;

  printf("hymem:\n");

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }

  printf("  portlib initialized\n");
  
  Hytest_init(&hyportLibrary, "Portlib.Hymem");
  Hytest_func(&hyportLibrary, test_hymem_allocate_memory, "hymem_allocate_memory");
  Hytest_func(&hyportLibrary, test_hymem_allocate_memory_callSite, "hymem_allocate_memory_callSite");
  Hytest_func(&hyportLibrary, test_hymem_free_memory, "hymem_free_memory");
  Hytest_func(&hyportLibrary, test_hymem_reallocate_memory, "hymem_reallocate_memory");
  Hytest_func(&hyportLibrary, test_hymem_shutdown, "hymem_shutdown");
  Hytest_func(&hyportLibrary, test_hymem_startup, "hymem_startup");
  ret = Hytest_close_and_output(&hyportLibrary);
  
  if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
    fprintf(stderr, "portlib shutdown failed\n");
    return 1;
  }
  
  printf("  portlib shutdown\n");
  return ret;
}

int test_hymem_allocate_memory(struct HyPortLibrary *hyportLibrary)
{
  void* pointer;
  pointer = hyportLibrary->mem_allocate_memory(hyportLibrary,512);
  if(!pointer){
    Hytest_setErrMsg(hyportLibrary, "failed to allocate memory: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary->mem_free_memory(hyportLibrary,pointer);
  return 0;
}

int test_hymem_allocate_memory_callSite(struct HyPortLibrary *hyportLibrary)
{
  void* pointer;
  pointer = hyportLibrary->mem_allocate_memory_callSite(hyportLibrary,512,"testCallSite");
  if(!pointer){
    Hytest_setErrMsg(hyportLibrary, "failed to allocate memory: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary->mem_free_memory(hyportLibrary,pointer);
  return 0;
}

int test_hymem_free_memory(struct HyPortLibrary *hyportLibrary)
{
  void* pointer;
  pointer = hyportLibrary->mem_allocate_memory(hyportLibrary,512);
  if(!pointer){
    Hytest_setErrMsg(hyportLibrary, "failed to allocate memory: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary->mem_free_memory(hyportLibrary,pointer);
  return 0;
}

int test_hymem_reallocate_memory(struct HyPortLibrary *hyportLibrary)
{
  void* pointer;
  void* pointer2;
  pointer = hyportLibrary->mem_allocate_memory(hyportLibrary,512);
  if(!pointer){
    Hytest_setErrMsg(hyportLibrary, "failed to allocate memory: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  } 
  pointer2 = hyportLibrary->mem_reallocate_memory(hyportLibrary,pointer,1024);
  if(!pointer2){
    Hytest_setErrMsg(hyportLibrary, "failed to reallocate memory: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary->mem_free_memory(hyportLibrary,pointer2);
  return 0;
}

int test_hymem_shutdown(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibrary hyportLibrary2;
  HyPortLibraryVersion portLibraryVersion;
  I_32 rc;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary2, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }
  rc =
    hyportLibrary2.mem_startup (&hyportLibrary2, sizeof (hyportLibrary2.portGlobals));
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "failed to reallocate memory: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary2.mem_shutdown (&hyportLibrary2);
  return 0;
}

int test_hymem_startup(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibrary hyportLibrary2;
  HyPortLibraryVersion portLibraryVersion;
  I_32 rc;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary2, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }
  rc =
    hyportLibrary2.mem_startup (&hyportLibrary2, sizeof (hyportLibrary2.portGlobals));
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "failed to reallocate memory: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary2.mem_shutdown (&hyportLibrary2);
  return 0;
}

