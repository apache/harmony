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

#ifndef __TYPEDEFINITION_H__
#define __TYPEDEFINITION_H__

typedef int fint;
typedef long flong;
typedef unsigned long uflong;
typedef double fdouble;
typedef float ffloat;
typedef unsigned char ufchar;
typedef char fchar;
typedef unsigned short ufshort;
typedef short fshort;
typedef unsigned short fwchar_t;

fint fwcslen(const fwchar_t* str);
fint fwcscmp(const fwchar_t* str1, const fchar* str2);
fint fwcscmp(const fwchar_t* str1, const fwchar_t* str2);

#endif
