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
#include <stdlib.h>

#include <stdio.h>

int main (int argc, char **argv, char **envp)
{
  HyPortLibrary hyportLibrary;
  HyPortLibraryVersion portLibraryVersion;

  printf("init:\n");

  HYPORT_SET_VERSION (&portLibraryVersion, HYPORT_CAPABILITY_MASK);
  if (0 != hyport_init_library (&hyportLibrary, &portLibraryVersion,
                                sizeof (HyPortLibrary)))
  {
    fprintf(stderr, "portlib init failed\n");
    return 1;
  }

  printf("  portlib initialized\n");
  
  if (0 != hyportLibrary.port_shutdown_library (&hyportLibrary)) {
    fprintf(stderr, "portlib shutdown failed\n");
    return 1;
  }
  printf("  portlib shutdown\n");

  return 0;
}
