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
 */

#ifndef _LOGSTREAM_H_
#define _LOGSTREAM_H_

#include "mkernel.h"
#include <iostream>
#include <iomanip>
#include <fstream>


namespace Jitrino
{

class HPipeline;


class LogStream
{
protected:

    static bool ison;
    static LogStream  logsink;
    static LogStream* logrtp;

    char* fname;
    bool enabled, 
         append,
         pending_open,
         was_open;

    std::ofstream os;
    Mutex* mutexp;
    int ref_count;

    bool lazy_open ();

public:

    enum SID
    {// numbering must starts with 0
        INFO = 0,
        RT,
        CT,
        IRDUMP,
        DOTDUMP,
        DBG

    //  add corresponding entry to the 'knownstreams' table
    };


    LogStream();
    LogStream(/*const*/ char* fname);
    LogStream(const LogStream&);
    ~LogStream();

    void open (/*const*/ char* fname);
    void close ();
    void addRef ();
    bool releaseRef ();

    bool isEnabled () const                     {return enabled;}
    std::ostream& out ()                        {lazy_open(); return os;}
    const char* getFileName () const            {return fname;}

    static LogStream& log (SID, HPipeline* pipe = 0);
    static LogStream& log (SID, const char* pipename = 0);
    static LogStream& log (SID, const char* pipename, size_t namesz);
    static LogStream& log (const char* streamname, const char* pipename = 0);
    static LogStream& log (const char* streamname, const char* pipename, size_t namesz);
    static LogStream& log_sink ()               {return logsink;}
    static LogStream& log_rt ()                 {return *logrtp;}
    static bool isOn ()                         {return ison;}

    template <class T>
    LogStream& operator << (const T& value) 
    {
#ifndef _NOLOG
        AutoUnlock lock(mutexp);
        if (lazy_open())
            os << value;
#endif
        return *this;
    }

    LogStream& operator << (std::ostream& (*f) (std::ostream&))     
    {
#ifndef _NOLOG
        AutoUnlock lock(mutexp);
        if (lazy_open())
            f(os);
#endif
        return *this;
    }

    int printf (const char*, ...)   
#ifdef _NOLOG
    {return 0;}     // will be removed during compilation
#else
    ;               // implementation in the corresponding .cpp file
#endif

    LogStream& flush ()
    {
#ifndef _NOLOG
        AutoUnlock lock(mutexp);
        if (lazy_open())
            os.flush();
#endif
        return *this;
    }

friend class LogStreams;
};


} //namespace Jitrino

#endif //#ifndef _LOGSTREAM_H_
