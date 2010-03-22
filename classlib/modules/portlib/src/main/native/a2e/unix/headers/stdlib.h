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
 * Replace the system header file "stdlib.h" so that we can redefine
 * the i/o functions that take/produce character strings
 * with our own ATOE functions.
 *
 * The compiler will find this header file in preference to the system one.
 * ===========================================================================
 */

#if __TARGET_LIB__ == 0X22080000               
#include <//'PP.ADLE370.OS39028.SCEEH.H(stdlib)'>
#else                                            
#include </usr/include/stdlib.h>                 
#endif                                           

#if defined(HY_ATOE)

	#if !defined(HY_ATOE_STDLIB)
		#define HY_ATOE_STDLIB

		#ifdef __cplusplus
            extern "C" {
		#endif

        double     atoe_atof      (const char *);            
        int        atoe_atoi      (const char *);
        int        atoe_atol      (const char *);            
        char*      atoe_getenv    (const char*);
        int        atoe_putenv    (const char*);
        char *     atoe_realpath  (const char*, char*);
        double     atoe_strtod    (const char *,char **);
        int        atoe_strtol    (const char *,char **, int);
        unsigned long atoe_strtoul (const char *,char **, int);
	unsigned long long atoe_strtoull( const char *, char **, int );
        int        atoe_system    (const char *);

		#ifdef __cplusplus
            }
		#endif

        #undef atof                                    
		#undef atoi
        #undef atol                                    
		#undef getenv
		#undef putenv
		#undef realpath
		#undef strtod
        #undef strtol                                  
        #undef strtoul                                 
		#undef strtoull
		#undef system

        #define atof	 atoe_atof                     
		#define atoi	 atoe_atoi
        #define atol	 atoe_atol                     
		#define getenv	 atoe_getenv
		#define putenv	 atoe_putenv
		#define realpath atoe_realpath
		#define strtod	 atoe_strtod
        #define strtol   atoe_strtol               
        #define strtoul  atoe_strtoul                    
		#define strtoull atoe_strtoull
		#define system	 atoe_system

	#endif

#endif
