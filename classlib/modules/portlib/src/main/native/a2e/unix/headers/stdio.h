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
 * Replace the system header file "stdio.h" so that we can redefine
 * the i/o functions that take/produce character strings
 * with our own ATOE functions.
 *
 * The compiler will find this header file in preference to the system one.
 * ===========================================================================
 */

#if __TARGET_LIB__ == 0X22080000                  
#include <//'PP.ADLE370.OS39028.SCEEH.H(stdio)'>  
#else                                             
#include </usr/include/stdio.h>                   
#endif                                            

#if defined(HY_ATOE)

	#if !defined(HY_ATOE_STDIO)
		#define HY_ATOE_STDIO

		#ifdef __cplusplus
            extern "C" {
		#endif

        FILE *     atoe_fopen     (const char*, const char*);
        int        atoe_fprintf   (FILE*, const char*, ...);
        size_t     atoe_fread     (void*, size_t, size_t, FILE*);
        FILE *     atoe_freopen   (const char*, const char*, FILE*);
        size_t     atoe_fwrite    (const void*, size_t, size_t, FILE*);
        char      *atoe_fgets (char *, int, FILE *);
        char *     atoe_gets      (char *);
        void       atoe_perror    (const char*);
        int        atoe_printf    (const char*, ...);
        int        atoe_putchar   (int);
        int        atoe_rename    (const char*, const char*);
        int        atoe_sprintf   (const char*, char*, ...);
        int        std_sprintf    (const char*, char*, ...);
        int        atoe_sscanf    (const char*, const char*, ...); 
        char *     atoe_tempnam   (const char *, char *);
        int        atoe_vprintf   (const char *, va_list);
        int        atoe_vfprintf  (FILE *, const char *, va_list);
        int        atoe_vsprintf  (char *, const char *, va_list); 
	int        atoe_vsnprintf (char *, size_t, const char *, va_list);

		#ifdef __cplusplus
            }
		#endif

		#undef fopen
		#undef fprintf
		#undef fread
		#undef freopen
		#undef fwrite
		#undef fgets
		#undef gets
		#undef perror
		#undef printf
		#undef putchar
		#undef rename
		#undef sprintf
		#undef sscanf                                 
		#undef tempnam
		#undef vfprintf
		#undef vsprintf                               
		#undef vsnprintf


		#define fopen           atoe_fopen
		#define fprintf         atoe_fprintf
		#define fread           atoe_fread
		#define freopen         atoe_freopen
		#define fwrite          atoe_fwrite
		#define fgets           atoe_fgets
		#define gets            atoe_gets
		#define perror          atoe_perror
		#define printf          atoe_printf
		#define putchar         atoe_putchar
		#define rename          atoe_rename
		#define sprintf         atoe_sprintf
		#define sscanf          atoe_sscanf           
		#define tempnam         atoe_tempnam
		#define vfprintf        atoe_vfprintf
		#define vprintf         atoe_vprintf
		#define vsprintf        atoe_vsprintf         
		#define vsnprintf	atoe_vsnprintf
	#endif

#endif
