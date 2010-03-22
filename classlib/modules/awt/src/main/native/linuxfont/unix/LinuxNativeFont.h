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
#ifndef _Included_LinuxNativeFont
#define _Included_LinuxNativeFont

#define FONT_PLAIN      0       /*  The plain style constant.       */
#define FONT_BOLD       1       /*  The bold style constant.        */
#define FONT_ITALIC     2       /*  The italicized style constant.  */

#define FONT_TYPE_TT    4       /* TrueType                         */
#define FONT_TYPE_T1    2       /* Type1                            */
#define FONT_TYPE_UNDEF 0   /* Undefined type                   */

typedef char BYTE;
typedef unsigned short WORD;
typedef unsigned int DWORD;

typedef unsigned long ULONG;
typedef unsigned short USHORT;
typedef short SHORT;
typedef char    Tag[4]; /* Array of four bytes                  */


/* Reverses DWORD bytes order */
unsigned long dwReverse(unsigned int data)
{
    unsigned char *dataElems = (unsigned char *) &data;
    return (unsigned long)((dataElems[0]<<24) | (dataElems[1]<<16) | (dataElems[2]<<8) | dataElems[3]);
}

/* Reverses WORD bytes order */
unsigned short wReverse(unsigned short data)
{
    return (unsigned short)(((data<<8) & 0xFF00) | ((data>>8) & 0x00FF));
}

#define _O_RDONLY       0x0000  /* open for reading only */
#define _O_BINARY       0x8000  /* file mode is binary (untranslated) */


typedef struct _GlyphBitmap{
    FT_Int       left;
    FT_Int       top;
    FT_Bitmap    bitmap;
} GlyphBitmap;

#endif // _Included_LinuxNativeFont
