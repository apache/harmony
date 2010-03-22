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
 * Generic ASCII to EBCDIC character conversion header file. This file defines
 * the base conversion functions used by the atoe_* functions.
 * ===========================================================================
 */

#include <stdlib.h> /* For malloc */

#if !defined(_HY_ATOE_H_)
    #define _HY_ATOE_H_

    #pragma map(sysTranslateASM, "SYSXLATE")
    #ifdef __cplusplus         
        extern char* sysTranslateASM(const char *source, int length, char *trtable, char* xlate_buf);
        extern char* sysTranslate(const char *source, int length, char *trtable, char* xlate_buf);
    #else
        extern char* sysTranslateASM(const char *source, int length, char *trtable, char* xlate_buf);
        extern char* sysTranslate(const char *source, int length, char *trtable, char* xlate_buf);
    #endif

    extern int iconv_init(void);

    #if !defined(MAXPATHLEN)
        #define MAXPATHLEN     1023+1
    #endif

    #if !defined(CONV_TABLE_SIZE)
        #define CONV_TABLE_SIZE 256
        extern char a2e_tab[CONV_TABLE_SIZE];
        extern char e2a_tab[CONV_TABLE_SIZE];
    #endif

      #define a2e(str, len) sysTranslate(str, abs(len), a2e_tab, (char *)malloc(abs(len)+1))
      #define e2a(str, len) sysTranslate(str, abs(len), e2a_tab, (char *)malloc(abs(len)+1))

      #define a2e_string(str) sysTranslate(str, strlen(str), a2e_tab, (char *)malloc(strlen(str)+1)) 
      #define e2a_string(str) sysTranslate(str, strlen(str), e2a_tab, (char *)malloc(strlen(str)+1)) 

    char *a2e_func(char *str, int len);    
    char *e2a_func(char *str, int len);    

    void atoe_enableFileTagging(void);
    void atoe_setFileTaggingCcsid(void *pccsid);

#endif  /* _HY_ATOE_H_ */

