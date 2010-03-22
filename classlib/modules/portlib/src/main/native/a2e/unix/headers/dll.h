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

/*
 * DESCRIPTION:
 * Replace the system header file "dll.h" so that we can redefine
 * the i/o functions that take/produce character strings
 * with our own ATOE functions.
 *
 * The compiler will find this header file in preference to the system one.
 * ===========================================================================
 */

#if __TARGET_LIB__ == 0X22080000               
#include <//'PP.ADLE370.OS39028.SCEEH.H(dll)'> 
#else                                          
#include </usr/include/dll.h>                  
#endif                                         

#if defined(HY_ATOE)

	#if !defined(HY_ATOE_DLL)
		#define HY_ATOE_DLL

		#ifdef __cplusplus
                  extern "C" {
		#endif
	        dllhandle* atoe_dllload(char *);
		#ifdef __cplusplus
                  }
		#endif

                #ifdef __cplusplus
                  extern "C" {
                #endif
                void *atoe_dllqueryvar(dllhandle* dllHandle, char* varName);
                #ifdef __cplusplus
                  }
                #endif

                #ifdef __cplusplus
                  extern "C" {
                #endif 
                void (*atoe_dllqueryfn(dllhandle* dllHandle, char* funcName)) ();
                #ifdef __cplusplus
                  }
                #endif

		#undef     dllload
		#undef     dllqueryfn
		#undef     dllqueryvar

		#define    dllload     atoe_dllload
		#define    dllqueryfn  atoe_dllqueryfn
		#define    dllqueryvar  atoe_dllqueryvar

	#endif

#endif
