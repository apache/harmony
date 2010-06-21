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
 * @author Nikolay A. Sidelnikov
 */

#ifndef _PLATFORMDEPENDANT_H_
#define _PLATFORMDEPENDANT_H_


#ifndef _MSC_VER
    #define strcmpi strcasecmp
    #define stricmp strcasecmp
    #define strnicmp strncasecmp
#else
    #pragma warning( push, 4 )
    #pragma warning( disable : 4100 4127 4201 4511 4512)
    #pragma conform( forScope, on )

    #define strdup _strdup
    #define strcmpi _strcmpi
    #define stricmp _stricmp

    #define isnan _isnan
    #define finite _finite

#endif //_MSC_VER

#undef stdcall__
#undef cdecl_       
#ifdef PLATFORM_POSIX
#include <limits.h>

#ifndef  __stdcall
   #define __stdcall
#endif

#ifndef  _cdecl
    #define _cdecl
#endif
    
    #define cdecl_       __attribute__ ((__cdecl__))
#ifdef _EM64T_
   #define stdcall__
#else
    #define stdcall__    __attribute__ ((__stdcall__))
#endif

#else
    #define stdcall__
    #define cdecl_      

    //--signbit implementation for windows platform
    #include <float.h>

    inline int signbit(double d) {
        return _copysign(1, d) < 0;
    }

    inline int signbit(float f) {
        return _copysign(1, f) < 0;
    }
    //----

#endif

inline bool fit32(int64 val) {
    return (INT_MIN <= val) && (val <= INT_MAX);
}


#endif // _PLATFORMDEPENDANT_H_
