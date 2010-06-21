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

#include "Log.h"
#include "PMF.h"
#include "PMFAction.h"
#include "CompilationContext.h"

#ifdef _WIN32
#define snprintf _snprintf
#endif


namespace Jitrino 
{


#ifndef _NOLOG
bool Log::isLogEnabled (LogStream::SID sid)
{
    if (LogStream::isOn())
    if (sid == LogStream::RT)
        return LogStream::log_rt().isEnabled();
    else
    {
        CompilationContext* cc = CompilationContext::getCurrentContext();
        SessionAction* session = cc->getCurrentSessionAction();
        return (session != 0) ? session->log(sid).isEnabled()
                              : LogStream::log(sid, cc->getPipeline()).isEnabled();
    }
    else
        return false;
}


LogStream& Log::log (LogStream::SID sid)
{
    if (LogStream::isOn())
    if (sid == LogStream::RT)
        return LogStream::log_rt();
    else
    {
        CompilationContext* cc = CompilationContext::getCurrentContext();
        SessionAction* session = cc->getCurrentSessionAction();
        return (session != 0) ? session->log(sid)
                              : LogStream::log(sid, cc->getPipeline());
    }
    else
        return LogStream::log_sink();
}


LogStream& Log::log_ct ()
{
    if (LogStream::isOn())
    {
    CompilationContext* cc = CompilationContext::getCurrentContext();
    SessionAction* session = cc->getCurrentSessionAction();
    return (session != 0) ? session->log(LogStream::CT)
                          : LogStream::log(LogStream::CT, cc->getPipeline());
    }
    else
        return LogStream::log_sink();
}
#endif


void Log::printStageBegin(std::ostream& out, U_32 stageId, const char * stageGroup, const char * stageName, const char * stageTag)
{
    out
        << "========================================================================" << ::std::endl
        << "__STAGE_BEGIN__:\tstageId="<<stageId<<"\tstageGroup="<<stageGroup<<"\tstageName=" << stageName << "\tstageTag=" << stageTag << ::std::endl
        << "========================================================================" << ::std::endl << ::std::endl ;
}

void Log::printStageEnd(std::ostream& out, U_32 stageId, const char * stageGroup, const char * stageName, const char * stageTag)
{
    out
        << "========================================================================" << ::std::endl
        << "__STAGE_END__:\tstageId="<<stageId<<"\tstageGroup="<<stageGroup<<"\tstageName=" << stageName << "\tstageTag=" << stageTag << ::std::endl
        << "========================================================================" << ::std::endl << ::std::endl ;
}


void Log::printIRDumpBegin(std::ostream& out, U_32 stageId, const char * stageName, const char * subKind)
{
    out
        << "========================================================================" << ::std::endl
        << "__IR_DUMP_BEGIN__:\tstageId="<<stageId<<"\tstageName=" << stageName << "\tsubKind=" << subKind << ::std::endl
        << "Printing IR " << stageName << " - " << subKind << ::std::endl
        << "========================================================================" << ::std::endl << ::std::endl ;
}


void Log::printIRDumpEnd(std::ostream& out, U_32 stageId, const char * stageName, const char * subKind)
{
    out
        << "========================================================================" << ::std::endl
        << "__IR_DUMP_END__:\tstageId="<<stageId<<"\tstageName=" << stageName << "\tsubKind=" << subKind << ::std::endl
        << "========================================================================" << ::std::endl << ::std::endl ;
}


char* Log::makeDotFileName (const char* suffix)
{
    const char* fname = log(LogStream::DOTDUMP).getFileName();

    size_t l0 = strlen(fname),
           l1 = strlen(suffix);

    if (l0 >= 4 && strncmp(fname + l0 - 4, ".dot", 4) == 0)
        l0 -= 4;

    char* dotfilename = new char[l0 + 1 + l1 + 4 + 1];

    char* ptr = dotfilename;
    memcpy(ptr, fname, l0);
    ptr += l0;
    if (ptr[-1] != '.' && ptr[-1] != '/')
        *ptr++ = '.';
    memcpy(ptr, suffix, l1);
    ptr += l1;
    memcpy(ptr, ".dot", 4);
    ptr += 4;
    *ptr = 0;

    return dotfilename;
}


int Log::getStageId()
{
    return CompilationContext::getCurrentContext()->stageId;;
}


} //namespace Jitrino 
