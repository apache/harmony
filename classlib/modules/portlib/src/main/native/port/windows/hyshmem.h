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

#if !defined(hyshmem_h)
#define hyshmem_h

#include <Windows.h>

typedef struct hyshmem_handle
{
  void *region;
  char *rootName;
  I_32 size;
  IDATA mappedFile;
  HANDLE shmHandle;
  DWORD perm;
  PROCESS_INFORMATION *helperpi;
  BOOL creator;
} hyshmem_handle;

I_32 VMCALL hyshmem_startup (struct HyPortLibrary *portLibrary);
IDATA VMCALL hyshmem_open (struct HyPortLibrary *portLibrary,
			   struct hyshmem_handle **handle,
			   const char *rootname, I_32 size, I_32 perm);
void VMCALL hyshmem_close (struct HyPortLibrary *portLibrary,
			   struct hyshmem_handle **handle);
UDATA VMCALL hyshmem_findfirst (struct HyPortLibrary *portLibrary,
				char *resultbuf);
I_32 VMCALL hyshmem_findnext (struct HyPortLibrary *portLibrary,
			      UDATA findHandle, char *resultbuf);
UDATA VMCALL hyshmem_stat (struct HyPortLibrary *portLibrary,
			   const char *name,
			   struct HyPortShmemStatistic *statbuf);
IDATA VMCALL hyshmem_destroy (struct HyPortLibrary *portLibrary,
			      struct hyshmem_handle **handle);
IDATA VMCALL hyshmem_detach (struct HyPortLibrary *portLibrary,
			     struct hyshmem_handle **handle);
void VMCALL hyshmem_findclose (struct HyPortLibrary *portLibrary,
			       UDATA findhandle);
void *VMCALL hyshmem_attach (struct HyPortLibrary *portLibrary,
			     struct hyshmem_handle *handle);
void VMCALL hyshmem_shutdown (struct HyPortLibrary *portLibrary);

#endif /* hyshmem_h */
