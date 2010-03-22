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

#if !defined(hyportpg_h)
#define hyportpg_h


/** Number of pageSizes supported.  There is always 1 for the default size,
 *  and 1 for the 0 terminator.
 * The number of large pages supported determines the remaining size.
 * Responsibility of the implementation of hyvmem to initialize this table correctly.
 */
#define HYPORT_VMEM_PAGESIZE_COUNT 3

/* from windef.h */
typedef void *HANDLE;

typedef struct HyPortPlatformGlobals
{
  HANDLE tty_consoleInputHd;
  HANDLE tty_consoleOutputHd;
  HANDLE tty_consoleErrorHd;
  HANDLE mem_heap;
  UDATA vmem_pageSize[HYPORT_VMEM_PAGESIZE_COUNT];	 /** <0 terminated array of supported page sizes */
  BOOLEAN sock_IPv6_function_support;
  char *si_osType;
  char *si_osVersion;
  U_64 time_hiresClockFrequency;
  char *shmem_directory;
  HANDLE shmem_creationMutex;
  HANDLE shsem_creationMutex;
} HyPortPlatformGlobals;

#define PPG_tty_consoleInputHd	(portLibrary->portGlobals->platformGlobals.tty_consoleInputHd)
#define PPG_tty_consoleOutputHd (portLibrary->portGlobals->platformGlobals.tty_consoleOutputHd)
#define PPG_tty_consoleErrorHd	(portLibrary->portGlobals->platformGlobals.tty_consoleErrorHd)
#define PPG_mem_heap (portLibrary->portGlobals->platformGlobals.mem_heap)
#define PPG_vmem_pageSize (portLibrary->portGlobals->platformGlobals.vmem_pageSize)
#define PPG_sock_IPv6_FUNCTION_SUPPORT (portLibrary->portGlobals->platformGlobals.sock_IPv6_function_support)
#define PPG_si_osType (portLibrary->portGlobals->platformGlobals.si_osType)
#define PPG_si_osVersion (portLibrary->portGlobals->platformGlobals.si_osVersion)
#define PPG_time_hiresClockFrequency (portLibrary->portGlobals->platformGlobals.time_hiresClockFrequency)
#define PPG_shmem_directory (portLibrary->portGlobals->platformGlobals.shmem_directory)
#define PPG_shmem_creationMutex (portLibrary->portGlobals->platformGlobals.shmem_creationMutex)
#define PPG_shsem_creationMutex (portLibrary->portGlobals->platformGlobals.shsem_creationMutex)

#endif /* hyportpg_h */
