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
 * Replace the system header file "stat.h" so that we can redefine
 * the i/o functions that take/produce character strings
 * with our own ATOE functions.
 *
 * The compiler will find this header file in preference to the system one.
 * ===========================================================================
 */

#if !defined(HY_STAT_INCLUDED)  
#define HY_STAT_INCLUDED        

#include </usr/include/sys/stat.h>       

#if defined(HY_ATOE)

	#if !defined(HY_ATOE_SYS_STAT)
		#define HY_ATOE_SYS_STAT

		#ifdef __cplusplus
            extern "C" {
		#endif

        int atoe_mkdir (const char*, mode_t);
        int atoe_remove (const char*);                
        int atoe_chmod (const char*, mode_t);         

		#ifdef __cplusplus
            }
		#endif

		#undef mkdir
        #undef remove                                 
		#undef stat
		#undef chmod                                  

		#define mkdir           atoe_mkdir
        #define remove          atoe_remove             
		#define stat(a,b)       atoe_stat(a,b)
		#define lstat(a,b)      atoe_lstat(a,b)
        #define chmod           atoe_chmod              

	#endif

#endif
#endif 
