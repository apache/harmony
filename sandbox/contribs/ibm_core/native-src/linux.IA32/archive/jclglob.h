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

#if !defined(jclglob_h)
#define jclglob_h

#include "jcl.h"
#include "zip.h"

extern void *Archive_JCL_ID_CACHE;

#define JCL_ID_CACHE Archive_JCL_ID_CACHE

typedef struct ArchiveJniIDCache
{
  jfieldID FID_java_util_zip_ZipFile_descriptor;
  jfieldID FID_java_util_zip_ZipFile_nextEntryPointer;
  jfieldID FID_java_util_zip_Deflater_inRead;
  jfieldID FID_java_util_zip_Deflater_finished;
  jfieldID FID_java_util_zip_Inflater_inRead;
  jfieldID FID_java_util_zip_Inflater_finished;
  jfieldID FID_java_util_zip_Inflater_needsDictionary;
  jmethodID MID_java_util_zip_ZipEntry_init;

  jclass CLS_java_util_zip_ZipEntry;
  JCLZipFileLink *zipfile_handles;
} ArchiveJniIDCache;

#define JniIDCache ArchiveJniIDCache

/* Now that the module-specific defines are in place, include the shared file */
#include "libglob.h"

#endif /* jclglob_h */
