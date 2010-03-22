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

#if !defined(hyshsem_h)
#define hyshsem_h

#include "portpriv.h"

typedef struct hyshsem_handle
{
  char *rootName;
  HANDLE *semHandles;
  HANDLE *mainLock;
  U_32 setSize;
  BOOL creator;
} hyshsem_handle;

I_32 VMCALL hyshsem_startup (struct HyPortLibrary *portLibrary);
void VMCALL hyshsem_close (struct HyPortLibrary *portLibrary,
			   struct hyshsem_handle **handle);
IDATA VMCALL hyshsem_post (struct HyPortLibrary *portLibrary,
			   struct hyshsem_handle *handle, UDATA semset,
			   UDATA flag);
IDATA VMCALL hyshsem_destroy (struct HyPortLibrary *portLibrary,
			      struct hyshsem_handle **handle);
void VMCALL hyshsem_shutdown (struct HyPortLibrary *portLibrary);
IDATA VMCALL hyshsem_setVal (struct HyPortLibrary *portLibrary,
			     struct hyshsem_handle *handle, UDATA semset,
			     IDATA value);
IDATA VMCALL hyshsem_open (struct HyPortLibrary *portLibrary,
			   struct hyshsem_handle **handle,
			   const char *semname, int setSize, int permission);
IDATA VMCALL hyshsem_getVal (struct HyPortLibrary *portLibrary,
			     struct hyshsem_handle *handle, UDATA semset);
IDATA VMCALL hyshsem_wait (struct HyPortLibrary *portLibrary,
			   struct hyshsem_handle *handle, UDATA semset,
			   UDATA flag);

#endif /* hyshsem_h */
