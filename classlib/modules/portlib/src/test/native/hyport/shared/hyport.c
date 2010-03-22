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
#include "hycomp.h"
#include "hyport.h"
#include "hycunit.h"
#include <stdlib.h>
#include <stdio.h>

int test_hyport_init_library(struct HyPortLibrary *hyportLibrary);
int test_hyport_create_library(struct HyPortLibrary *hyportLibrary);
int test_hyport_shutdown_library(struct HyPortLibrary *hyportLibrary);
int test_hyport_getSize(struct HyPortLibrary *hyportLibrary);
int test_hyport_allocate_library(struct HyPortLibrary *hyportLibrary);
int test_hyport_getVersion(struct HyPortLibrary *hyportLibrary);
int test_hyport_isCompatible(struct HyPortLibrary *hyportLibrary);
int test_hyport_isFunctionOverridden(struct HyPortLibrary *hyportLibrary);

int main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  int ret;
  
  printf("hyport:\n");

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }
  
  printf("  portlib initialized\n");
  
  Hytest_init(&hyportLibrary, "Portlib.Hyport");
  Hytest_func(&hyportLibrary, test_hyport_init_library, "hyport_init_library");
  Hytest_func(&hyportLibrary, test_hyport_shutdown_library, "hyport_shutdown_library");
  Hytest_func(&hyportLibrary, test_hyport_getSize, "hyport_getSize");
  Hytest_func(&hyportLibrary, test_hyport_create_library, "hyport_create_library");
  Hytest_func(&hyportLibrary, test_hyport_allocate_library, "hyport_allocate_library");
  Hytest_func(&hyportLibrary, test_hyport_getVersion, "hyport_getVersion");
  Hytest_func(&hyportLibrary, test_hyport_isCompatible, "hyport_isCompatible");
  Hytest_func(&hyportLibrary, test_hyport_isFunctionOverridden, "hyport_isFunctionOverridden");
  
  ret = Hytest_close_and_output(&hyportLibrary);
    
  if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
    fprintf(stderr, "portlib shutdown failed\n");
    return 1;
  }
  printf("  portlib shutdown\n");

  return ret;
}

int test_hyport_init_library(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibraryVersion portLibraryVersion;
  HyPortLibrary hyportLibrary2;
  
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary2, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    Hytest_setErrMsg(hyportLibrary, "portlib init failed(%s) \n",HY_GET_CALLSITE());
    return -1;
  }
  if (0 != hyportLibrary->port_shutdown_library (&hyportLibrary2)) {
    Hytest_setErrMsg(hyportLibrary, "portlib shutdown failed (%s)\n",HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyport_shutdown_library(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibraryVersion portLibraryVersion;
  HyPortLibrary hyportLibrary2;
  
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary2, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    Hytest_setErrMsg(hyportLibrary, "portlib init failed (%s)\n",HY_GET_CALLSITE());
    return -1;
  }
  if (0 != hyportLibrary->port_shutdown_library (&hyportLibrary2)) {
    Hytest_setErrMsg(hyportLibrary, "portlib shutdown failed (%s)\n",HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyport_getSize(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibraryVersion portLibraryVersion;
  UDATA size;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  size = hyport_getSize(&portLibraryVersion);
  printf("size:\t%d\n",size);
  if(size == 0)
  {
    Hytest_setErrMsg(hyportLibrary, "fail to get the size of portLibrary (%s)\n",HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyport_create_library(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibrary hyportLibrary2;
  HyPortLibraryVersion portLibraryVersion;
  UDATA size;
  I_32 ret;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  size = hyport_getSize(&portLibraryVersion);
  if(size == 0)
  {
    Hytest_setErrMsg(hyportLibrary, "fail to get the size of portLibrary (%s)\n",HY_GET_CALLSITE());
    return -1;
  }
  ret = hyport_create_library(&hyportLibrary2, &portLibraryVersion, size);
  
  if(ret!=0)
  {
    Hytest_setErrMsg(hyportLibrary, "fail to create the portLibrary (%s)\n",HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyport_allocate_library(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibrary *portLibraryPointer = NULL;
  HyPortLibraryVersion portLibraryVersion;
  I_32 ret;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  ret = hyport_allocate_library(&portLibraryVersion, &portLibraryPointer);
  if(ret != 0||!portLibraryPointer)
  {
    Hytest_setErrMsg(hyportLibrary, "fail to allocate memory to the portLibrary (%s)\n",HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyport_getVersion(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibrary portLibrary2;
  HyPortLibraryVersion portLibraryVersion;
  I_32 ret;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&portLibrary2, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    Hytest_setErrMsg(hyportLibrary, "portlib init failed(%s) \n",HY_GET_CALLSITE());
    return -1;
  }
  ret = hyport_getVersion(&portLibrary2,&portLibraryVersion);
  if(ret!=0)
  {
    Hytest_setErrMsg(hyportLibrary, "hyport getVersion failed(%s) \n",HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyport_isCompatible(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibraryVersion portLibraryVersion;
  I_32 ret;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  ret = hyport_isCompatible(&portLibraryVersion);
  if(ret != 1)
  {
    Hytest_setErrMsg(hyportLibrary, "hyport_isCompatible Output shoule be [%d] but [%d] (%s) \n",1,ret,HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hyport_isFunctionOverridden(struct HyPortLibrary *hyportLibrary)
{
  HyPortLibrary portLibrary2;
  HyPortLibraryVersion portLibraryVersion;
  I_32 ret;
  UDATA offset;
  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  
  if (0 != hyport_init_library (&portLibrary2, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    Hytest_setErrMsg(hyportLibrary, "portlib init failed(%s) \n",HY_GET_CALLSITE());
    return -1;
  }
  offset = offsetof(HyPortLibrary,port_isFunctionOverridden);
  printf("offset:\t%d\n",offset);
  ret = hyportLibrary->port_isFunctionOverridden(&portLibrary2,offset);
  if(ret != 0)
  {
    Hytest_setErrMsg(hyportLibrary, "hyport_isFunctionOverridden Output shoule be [%d] but [%d] (%s) \n",0,ret,HY_GET_CALLSITE());
    return -1;
  }
  portLibrary2.port_isFunctionOverridden = NULL;
  
  ret = hyportLibrary->port_isFunctionOverridden(&portLibrary2,offset);
  
  if(ret != 1)
  {
    Hytest_setErrMsg(hyportLibrary, "hyport_isFunctionOverridden Output shoule be [%d] but [%d] (%s) \n",1,ret,HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

