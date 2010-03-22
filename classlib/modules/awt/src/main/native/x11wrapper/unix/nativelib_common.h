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
 * @author Sergey Kuksenko
 */
#ifndef NATIVELIB_COMMON_H
#define NATIVELIB_COMMON_H

#if defined(LINUX) || defined(FREEBSD) || defined(AIX) || defined(MACOSX) || defined(ZOS)
// common linux section--------------------------------
#ifndef ZOS
#include <stdint.h>
#else
#include <stddef.h>
#ifndef __SUSV3
#define __SUSV3
#endif
#endif
#include <dlfcn.h>

#define mkstr(x) #x

#define LOAD_LIB(res, name) res = dlopen(mkstr(lib##name.so), RTLD_LAZY)
#define LOAD_LIB_VER(res, name, ver) res = dlopen(mkstr(lib##name.so.##ver), RTLD_LAZY)

#define FindFunction(lib, name) (void *)dlsym(lib, name)

#if !defined(__INTEL_COMPILER) && !defined(AIX) && !defined(ZOS)
    typedef long long __int64;
#endif

typedef void* libHandler;

// END common linux section----------------------------
#ifdef           __i386__
// ia32 linux section----------------------------------



// END ia32 linux section------------------------------
#else           
// ipf linux section----------------------------------

// END ipf linux section------------------------------
#endif
#else        //WINDOWS
// common windows section------------------------------
#include <windows.h>

typedef HMODULE libHandler;

#define LOAD_LIB(res, name) res =::LoadLibraryA(name)

#define FindFunction(lib, name) ::GetProcAddress(lib, name)

// END common windows section--------------------------
#ifdef           _WIN32
// ia32 windows section--------------------------------

// END ia32 windows section----------------------------
#else        //ITANIUM
// ipf windows section--------------------------------

// END ipf windows section----------------------------
#endif
#endif


#endif
