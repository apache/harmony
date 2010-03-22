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
 * Replace the system header file "unistd.h" so that we can redefine
 * the i/o functions that take/produce character strings
 * with our own ATOE functions.
 *
 * The compiler will find this header file in preference to the system one.
 * ===========================================================================
 */

#if __TARGET_LIB__ == 0X22080000                    
#include <//'PP.ADLE370.OS39028.SCEEH.H(unistd)'>   
#else                                               
#include </usr/include/unistd.h>                    
#endif                                              

#if defined(HY_ATOE)

   #if !defined(HY_ATOE_UNISTD)
      #define HY_ATOE_UNISTD

      /******************************************************************/
      /*  Define prototypes for replacement functions.                  */
      /******************************************************************/

      #ifdef __cplusplus
         extern "C" {
      #endif

      int        atoe_access     (const char*, int);
      char *     atoe_getcwd     (char*, size_t);
      extern int atoe_gethostname(char *, int);
      char *     atoe_getlogin   (void);
      int        atoe_unlink     (const char*);
      int        atoe_chdir (const char*);                
      int        atoe_chown      (const char*, uid_t, gid_t);
      int        atoe_rmdir (const char *);
      int        atoe_execv (const char *, char *const []);
      int        atoe_execvp (const char *, char *const []);

      #ifdef __cplusplus
         }
      #endif

      /******************************************************************/
      /*  Undefine the functions                                        */
      /******************************************************************/

      #undef access
      #undef getcwd
      #undef gethostname
      #undef getlogin
      #undef unlink
      #undef chown
      #undef rmdir
      #undef chdir
      #undef execv
      #undef execvp

      /******************************************************************/
      /*  Redefine the functions                                        */
      /******************************************************************/

      #define access(a,b)     atoe_access(a,b)
      #define getcwd          atoe_getcwd
      #define gethostname     atoe_gethostname
      #define getlogin        atoe_getlogin
      #define unlink          atoe_unlink
      #define chdir           atoe_chdir                          
      #define chown           atoe_chown
      #define rmdir           atoe_rmdir      
      #define execv           atoe_execv
      #define execvp           atoe_execvp

   #endif
#endif
