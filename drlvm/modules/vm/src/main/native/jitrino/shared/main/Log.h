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
 * @author Intel, Sergey L. Ivashin
 *
 */

#ifndef _LOG_H_
#define _LOG_H_

#include "LogStream.h"
#include "open/types.h"
#include <iostream>


namespace Jitrino 
{


class Log
{
public:

#ifndef _NOLOG

//  deprecated methods to mimicry old behavior

    static bool isEnabled ()            {return LogStream::isOn() && log_ct().isEnabled();}
    static std::ostream& out ()         {return log_ct().out();}
    static LogStream* cat_rt ()         {return &LogStream::log_rt();}

//  new methods

    static LogStream& log_ct ();
    static LogStream& log_rt ()         {return LogStream::log_rt();}

    static bool isLogEnabled (LogStream::SID);
    static LogStream&    log (LogStream::SID);

#else

    static bool isEnabled ()            {return false;}
    static std::ostream& out ()         {return log_ct().out();}
    static LogStream* cat_rt ()         {return &log_rt();}

    static LogStream& log_ct ()         {return LogStream::log_sink();}
    static LogStream& log_rt ()         {return LogStream::log_sink();}

    static bool isLogEnabled (LogStream::SID)   {return false;}
    static LogStream&    log (LogStream::SID)   {return LogStream::log_sink();}

#endif

    static void printStageBegin (std::ostream&, U_32 stageId, const char * stageGroup, const char * stageName, const char * stageTag);
    static void printStageEnd   (std::ostream&, U_32 stageId, const char * stageGroup, const char * stageName, const char * stageTag);
    static void printIRDumpBegin(std::ostream&, U_32 stageId, const char * stageName, const char * subKind);
    static void printIRDumpEnd  (std::ostream&, U_32 stageId, const char * stageName, const char * subKind);

    static char* makeDotFileName (const char* suffix);

    static int getStageId();
};


} // namespace Jitrino

#endif // _LOG_H_

