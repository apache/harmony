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
 * @author Ilya S. Okomin
 *
 */
#ifndef _Included_winFont
#define _Included_winFont

#include <jni.h>
#include <Windows.h>
#include <GdiPlus.h>
#include "gl_GDIPlus.h"

using namespace Gdiplus;

#define FONTLIB_ERROR 0         /* error code                   */
#define FONTLIB_SUCCESS 1       /* success code                 */

#define FONT_PLAIN      0       /*  The plain style constant.       */
#define FONT_BOLD       1       /*  The bold style constant.        */
#define FONT_ITALIC     2       /*  The italicized style constant.  */

#define FONT_STR_BOLD       "bold"      /*  The bold style string constant.     */
#define FONT_STR_ITALIC     "italic"    /*  The italicized style string constant.   */

#define FONT_TYPE_TT    4   /* TrueType                     */
#define FONT_TYPE_T1    2   /* Type1                        */
#define FONT_TYPE_UNDEF 0   /* Undefined type                   */

typedef char    Tag[4]; /* Array of four bytes                  */

// Font names structure for EnumFontFamEx
typedef struct
{
    int *indices;       /* Array of font families indices in families array.*/
    int *types;         /* Type of font                                     */
    int *styles;        /* Style of font                                    */
    TCHAR **faceNames;  /* Face name of the font                            */
    int size;           /* Size of the array.                               */
    int count;      /* The Number of elements in the array.             */
} FontRecords;

typedef struct
{
    TCHAR names[256][10];   /* Locale name. */
    short lcids[256];       /* LCID value.  */
    int count;
} LCIDS;

#endif // _Included_winFont
