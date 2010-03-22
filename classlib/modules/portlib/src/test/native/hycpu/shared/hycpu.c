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

int test_hycpu_flush_icache(struct HyPortLibrary *hyportLibrary);
int test_hycpu_startup(struct HyPortLibrary *hyportLibrary);
int test_hycpu_shutdown(struct HyPortLibrary *hyportLibrary);

int main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;
  int ret;

  printf("hycpu:\n");

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }

  printf("  portlib initialized\n");
  
  Hytest_init(&hyportLibrary, "Portlib.Hycpu");
  Hytest_func(&hyportLibrary, test_hycpu_flush_icache, "hycpu_flush_icache");
  Hytest_func(&hyportLibrary, test_hycpu_startup, "hycpu_startup");
  Hytest_func(&hyportLibrary, test_hycpu_shutdown, "hycpu_shutdown");
  ret = Hytest_close_and_output(&hyportLibrary);
  
  if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
    fprintf(stderr, "portlib shutdown failed\n");
    return 1;
  }
  
  printf("  portlib shutdown\n");
  return ret;
}

int test_hycpu_flush_icache(struct HyPortLibrary *hyportLibrary)
{
  hyportLibrary->cpu_flush_icache(hyportLibrary,NULL,512);
  return 0;
}

int test_hycpu_startup(struct HyPortLibrary *hyportLibrary)
{
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
  rc =
    hyportLibrary2.cpu_startup (&hyportLibrary2);
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "cpu startup failed: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  return 0;
}

int test_hycpu_shutdown(struct HyPortLibrary *hyportLibrary)
{
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
  rc =
    hyportLibrary2.cpu_startup (&hyportLibrary2);
  if (0 != rc)
  {
    Hytest_setErrMsg(hyportLibrary, "cpu startup failed: %s (%s)\n",
    hyportLibrary->error_last_error_message(hyportLibrary),HY_GET_CALLSITE());
    return -1;
  }
  hyportLibrary2.cpu_shutdown(hyportLibrary);
  return 0;
}

