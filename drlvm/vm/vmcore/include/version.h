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
#ifndef _VERSION_H
#define _VERSION_H

#include "version_svn_tag.h"

#if defined(__INTEL_COMPILER)
#if defined(__GNUC__)
#define VERSION_COMPILER "icc " EXPAND(__INTEL_COMPILER)
#else
#define VERSION_COMPILER "icl " EXPAND(__INTEL_COMPILER)
#endif
#elif defined(_MSC_VER)
#define VERSION_COMPILER "msvc " EXPAND(_MSC_VER)
#elif defined(__GNUC__)
#define VERSION_COMPILER "gcc " EXPAND(__GNUC__) "." EXPAND(__GNUC_MINOR__) "." EXPAND(__GNUC_PATCHLEVEL__)
#else
#define VERSION_COMPILER "unknown compiler"
#endif

#if defined(_DEBUG)
#define VERSION_DEBUG_STRING "debug"
#elif defined(NDEBUG)
#define VERSION_DEBUG_STRING "release"
#else
#define VERSION_DEBUG_STRING "debugging mode unknown"
#endif

#if defined(PLATFORM_POSIX)
#define VERSION_OS "Linux"
#else
#define VERSION_OS "Windows"
#endif

#if defined(_IPF_)
#define VERSION_ARCH "ipf"
#elif defined(_EM64T_)
#define VERSION_ARCH "em64t"
#else
#define VERSION_ARCH "ia32"
#endif

#define VM_VERSION "Apache Harmony HEAD"

#define VERSION "pre-alpha : not complete or compatible\n" \
    "svn = r" VERSION_SVN_TAG ", (" __DATE__ "), " \
    VERSION_OS "/" VERSION_ARCH "/" VERSION_COMPILER ", " VERSION_DEBUG_STRING " build\n" \
    "http://harmony.apache.org"

#endif /* _VERSION_H */

