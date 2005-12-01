/* Copyright 2004, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
/* Header for class com_ibm_platform_IFileSystem */

#if !defined(_Included_com_ibm_platform_IFileSystem)
#define _Included_com_ibm_platform_IFileSystem
#if defined(__cplusplus)
extern "C"
{
#endif
#undef com_ibm_platform_IFileSystem_SHARED_LOCK_TYPE
#define com_ibm_platform_IFileSystem_SHARED_LOCK_TYPE 1L
#undef com_ibm_platform_IFileSystem_EXCLUSIVE_LOCK_TYPE
#define com_ibm_platform_IFileSystem_EXCLUSIVE_LOCK_TYPE 2L
#undef com_ibm_platform_IFileSystem_SEEK_SET
#define com_ibm_platform_IFileSystem_SEEK_SET 1L
#undef com_ibm_platform_IFileSystem_SEEK_CUR
#define com_ibm_platform_IFileSystem_SEEK_CUR 2L
#undef com_ibm_platform_IFileSystem_SEEK_END
#define com_ibm_platform_IFileSystem_SEEK_END 4L
#undef com_ibm_platform_IFileSystem_MMAP_READ_ONLY
#define com_ibm_platform_IFileSystem_MMAP_READ_ONLY 1L
#undef com_ibm_platform_IFileSystem_MMAP_READ_WRITE
#define com_ibm_platform_IFileSystem_MMAP_READ_WRITE 2L
#undef com_ibm_platform_IFileSystem_MMAP_WRITE_COPY
#define com_ibm_platform_IFileSystem_MMAP_WRITE_COPY 4L
#undef com_ibm_platform_IFileSystem_O_RDONLY
#define com_ibm_platform_IFileSystem_O_RDONLY 0L
#undef com_ibm_platform_IFileSystem_O_WRONLY
#define com_ibm_platform_IFileSystem_O_WRONLY 1L
#undef com_ibm_platform_IFileSystem_O_RDWR
#define com_ibm_platform_IFileSystem_O_RDWR 16L
#undef com_ibm_platform_IFileSystem_O_APPEND
#define com_ibm_platform_IFileSystem_O_APPEND 256L
#undef com_ibm_platform_IFileSystem_O_CREAT
#define com_ibm_platform_IFileSystem_O_CREAT 4096L
#undef com_ibm_platform_IFileSystem_O_EXCL
#define com_ibm_platform_IFileSystem_O_EXCL 65536L
#undef com_ibm_platform_IFileSystem_O_NOCTTY
#define com_ibm_platform_IFileSystem_O_NOCTTY 1048576L
#undef com_ibm_platform_IFileSystem_O_NONBLOCK
#define com_ibm_platform_IFileSystem_O_NONBLOCK 16777216L
#undef com_ibm_platform_IFileSystem_O_TRUNC
#define com_ibm_platform_IFileSystem_O_TRUNC 268435456L
#if defined(__cplusplus)
}
#endif
#endif
