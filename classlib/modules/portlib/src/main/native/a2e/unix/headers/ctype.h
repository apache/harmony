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
 * Replace the system header file "ctype.h" so that we can redefine
 * the i/o functions that take/produce character strings
 * with our own ATOE functions.
 *
 * The compiler will find this header file in preference to the system one.
 * ===========================================================================
 */

#if __TARGET_LIB__ == 0X22080000              
#include <//'PP.ADLE370.OS39028.SCEEH.H(ctype)'>
#else                                           
#include </usr/include/ctype.h>                 
#endif                                          

#if defined(HY_ATOE)

        #if !defined(HY_ATOE_CTYPE)
                #define HY_ATOE_CTYPE

                #undef isalnum
                #undef isalpha
                #undef iscntrl
                #undef isdigit
                #undef isgraph
                #undef islower
                #undef isprint
                #undef ispunct
                #undef isspace
                #undef isupper
                #undef isxdigit
                #undef toupper
                #undef tolower

                extern int _ascii_is_tab[256];

                #define _ISALNUM_ASCII  0x0001
                #define _ISALPHA_ASCII  0x0002
                #define _ISCNTRL_ASCII  0x0004
                #define _ISDIGIT_ASCII  0x0008
                #define _ISGRAPH_ASCII  0x0010
                #define _ISLOWER_ASCII  0x0020
                #define _ISPRINT_ASCII  0x0040
                #define _ISPUNCT_ASCII  0x0080
                #define _ISSPACE_ASCII  0x0100
                #define _ISUPPER_ASCII  0x0200
                #define _ISXDIGIT_ASCII 0x0400

                #define _XUPPER_ASCII   0xdf                  
                #define _XLOWER_ASCII   0x20                  

                #define _IN_RANGE(c)   ((c >= 0) && (c <= 255))

                #define isalnum(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISALNUM_ASCII) : 0)
                #define isalpha(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISALPHA_ASCII) : 0)
                #define iscntrl(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISCNTRL_ASCII) : 0)
                #define isdigit(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISDIGIT_ASCII) : 0)
                #define isgraph(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISGRAPH_ASCII) : 0)
                #define islower(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISLOWER_ASCII) : 0)
                #define isprint(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISPRINT_ASCII) : 0)
                #define ispunct(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISPUNCT_ASCII) : 0)
                #define isspace(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISSPACE_ASCII) : 0)
                #define isupper(c)     (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISUPPER_ASCII) : 0)
                #define isxdigit(c)    (_IN_RANGE(c) ? (_ascii_is_tab[c] & _ISXDIGIT_ASCII) : 0)

                /*
                 *  In ASCII, upper case characters have the bit off   
                 */
                #define toupper(c)     (islower(c) ? (c & _XUPPER_ASCII) : c)
                #define tolower(c)     (isupper(c) ? (c | _XLOWER_ASCII) : c)

        #endif

#endif
