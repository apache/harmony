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

#ifndef _COUNTWRITERS_H_
#define _COUNTWRITERS_H_

#include "Counter.h"
#include <iostream>


namespace Jitrino 
{


class CountWriterFile : public CountWriter
{
public:

    CountWriterFile (const char* = 0);
    /*virtual*/ ~CountWriterFile ();

    bool open  (const char*);
    void close ();

    /*virtual*/ void write (const char* key, const char*    value);
    /*virtual*/ void write (const char* key, int            value);
    /*virtual*/ void write (const char* key, size_t         value);
    /*virtual*/ void write (const char* key, double         value);

protected:

    std::ofstream* file;
    std::ostream* os;
};



#ifdef _WIN32

class CountWriterMail : public CountWriter
{
public:

    CountWriterMail (const char* = 0);
    /*virtual*/ ~CountWriterMail ();

    bool open  (const char*);
    void close ();

    /*virtual*/ void write (const char* key, const char*    value);
    /*virtual*/ void write (const char* key, int            value);
    /*virtual*/ void write (const char* key, size_t         value);
    /*virtual*/ void write (const char* key, double         value);

protected:

    void mail (const char*, size_t);

    void* sloth;
};

#endif //#ifdef _WIN32


} //namespace Jitrino 


#endif   //#ifndef _COUNTWRITERS_H_
