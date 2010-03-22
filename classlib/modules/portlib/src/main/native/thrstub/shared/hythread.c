/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @file
 * @ingroup Thread
 * @brief Threading and synchronization support
 */

#include "hythread.h"
#include <stdlib.h>

/**
 * @name Thread library startup and shutdown functions
 * @anchor ThreadStartup
 * Create, initialize, startup and shutdow the thread library
 * @{
 */
/** Standard startup and shutdown (thread library allocated on stack or by application)  */
HY_CFUNC I_32 VMCALL 
hythread_create_library (struct HyThreadLibrary *threadLibrary,
                                                     struct HyThreadLibraryVersion
                                                     *version, UDATA size)
{
  return -1;
}


HY_CFUNC I_32 VMCALL 
hythread_init_library (struct HyThreadLibrary *threadLibrary,
                       struct HyThreadLibraryVersion *version, UDATA size)
{
  return -1;
}

HY_CFUNC I_32 VMCALL
hythread_shutdown_library (struct HyThreadLibrary *threadLibrary)
{
	return -1;
}

HY_CFUNC I_32 VMCALL
hythread_startup_library (struct HyThreadLibrary *threadLibrary)
{
  return -1;
}

/** Thread library self allocation routines */
HY_CFUNC I_32 VMCALL
hythread_allocate_library (struct HyThreadLibraryVersion*expectedVersion,
                           struct HyThreadLibrary **threadLibrary)
{
  return -1;
}

/** @} */
/**
 * @name Thread library version and compatability queries
 * @anchor ThreadVersionControl
 * Determine thread library compatability and version.
 * @{
 */
HY_CFUNC UDATA VMCALL
hythread_getSize (struct HyThreadLibraryVersion *version)
{
  return 0;
}

HY_CFUNC I_32 VMCALL
hythread_getVersion (struct HyThreadLibrary *threadLibrary,
                     struct HyThreadLibraryVersion *version) 
{
  return -1;
}

HY_CFUNC I_32 VMCALL
hythread_isCompatible (struct HyThreadLibraryVersion *expectedVersion)
{
  return -1;
}
