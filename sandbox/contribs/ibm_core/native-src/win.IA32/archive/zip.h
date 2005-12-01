/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
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

#if !defined(zip_h)
#define zip_h

#include "zipsup.h"

typedef struct JCLZipFile
{
  struct JCLZipFile *last;
  struct JCLZipFile *next;
  HyZipFile hyZipFile;
} JCLZipFile;

/* Fake JCLZipFile entry. last, next must be in the same position as JCLZipFile */
typedef struct JCLZipFileLink
{
  JCLZipFile *last;
  JCLZipFile *next;
} JCLZipFileLink;

#endif /* zip_h */
