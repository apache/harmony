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
 * @author Sergey L. Ivashin
 *
 */


#include <FixFileName.h>
#include <stdio.h>
#include <string.h>
#include <iostream>
#include <iomanip>

#ifdef _WIN32
    #define PATHSEPCHAR '\\'
#else   //PLATFORM_POSIX
    #define PATHSEPCHAR '/'
#endif //ifdef _WIN32


using namespace std;


namespace Jitrino
{


static bool isValid (int c)
{
#ifdef _PMF_LOG_ANY
// On Window any characters expect 0x00 - 0x17 and <>:"/\| are acceptable in file/directory name
    if (c < 0x20)
        return false;
    if (strchr("<>:\"/\\|", c) != 0)
        return false;
    return true;
#else
//  But we are using more safe approach
    return ('a' <= c && c <= 'z') ||
           ('A' <= c && c <= 'Z') ||
           ('0' <= c && c <= '9') ||
           strchr(".-_$()[],;:", c) != 0;
#endif
}


static unsigned int hash (const char* ptr, size_t count)
{
    unsigned int r = 0;

    for (; count != 0; --count)
        r = (r << 1) ^ (*ptr++);

    return r;
}


static void reserve (char*& ptr, char* end, size_t n)
{
    if (ptr + n > end)
        ptr = end - n;
}


void fix_file_name (char* goodname, int goodmax, const char* badname)
{
    size_t badnamesz = strlen(badname),
           badnamemx = badnamesz * 3 + 1;       // in the worst case, 1 char replaced by 3 '~88'
    char* work = new  char[badnamemx];

    char* src  = work;
    for (; *badname != 0; ++badname)
    {
        int c = *badname & 0xFF;

        if (c == '<' || c == '>')
            *src++ = '_';

        else if (c == '/' || c == '\\')
            *src++ = PATHSEPCHAR;

        else if (isValid(c))
            *src++ = (char)c;

        else
            src += sprintf(src, "~%x", c);
    }
    *src = 0;

    src = work;
    char* dst    = goodname;
    char* dstend = goodname + goodmax - 1;

    for (; *src != 0; ++src)
    {
    //  find next part of source file name to process
        const char* srcpart = src;
        while (*src != '/' && *src != '\\' && *src != '.' && *src != 0)
            ++src;

        size_t partsz = src - srcpart;

        if (partsz > MAXFILEPARTSIZE)
        {// will copy MAXFILEPARTSIZE chars
            reserve(dst, dstend, MAXFILEPARTSIZE);
            char temp[10];
            int n = sprintf(temp, "~%x", hash(srcpart, partsz) & 0xFFF);
            memcpy(dst, srcpart, MAXFILEPARTSIZE - n);
            dst += MAXFILEPARTSIZE - n;
            memcpy(dst, temp, n);
            dst += n;
        }
        else
        {// will copy partsz chars
            reserve(dst, dstend, partsz);
            memcpy(dst, srcpart, partsz);
            dst += partsz;
        }

        reserve(dst, dstend, 1);
        *dst++ = *src;
        if (*src == 0)
            break;
    }

    delete [] work;
}

} //namespace Jitrino
