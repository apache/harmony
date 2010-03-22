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
 * @author Intel, Evgueni Brevnov
 */  
#ifndef _PLATFORM_LOWLEVEL_H_
#define _PLATFORM_LOWLEVEL_H_

#include "stdlib.h"
#include <limits.h>
//#include <ctype.h>

#ifdef __cplusplus
extern "C" {
#endif

#define __fastcall
#if defined(_IPF_) || defined(_EM64T_)
#define __stdcall
#else
#define __stdcall  __attribute__ ((__stdcall__))
#endif
#define __cdecl

#define dllexport
#define __declspec(junk)


#ifdef __cplusplus
}
#endif
#endif // _PLATFORM_LOWLEVEL_H_
