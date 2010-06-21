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
 * @author Intel, Mikhail Y. Fursov
 */
#include "CompilationContext.h"

#include "MemoryManager.h"
#include "EMInterface.h"
#include "JITInstanceContext.h"
#include "Type.h"
#include "PMF.h"
#include "PMFAction.h"
#include "mkernel.h"

namespace Jitrino {

static TlsStack<CompilationContext> ccTls;

CompilationContext::CompilationContext(MemoryManager& _mm, CompilationInterface* ci, JITInstanceContext* _jitContext) 
: mm(_mm), compilationInterface(ci), jitContext(_jitContext)
{
    init();
}

void CompilationContext::init()
{
    compilationFailed = compilationFinished = false;
    currentLogStreams = 0;
    pipeline = 0;
    stageId = 0;

    hirm = NULL;
#ifdef _IPF_
#else 
    lirm = NULL;
#endif
    currentSessionAction = NULL;
    currentSessionNum = 0;
    inliningContext = NULL;
    ccTls.push((CompilationContext*)this);
}

CompilationContext::~CompilationContext() {
#ifdef _DEBUG
    CompilationContext* last = ccTls.pop();
    assert(this == last);
#else 
    ccTls.pop();
#endif
}

ProfilingInterface* CompilationContext::getProfilingInterface() const {
    return getCurrentJITContext()->getProfilingInterface();
}

bool CompilationContext::hasDynamicProfileToUse() const  {
    ProfilingInterface* pi = getProfilingInterface();
    return pi->hasMethodProfile(ProfileType_Edge, *compilationInterface->getMethodToCompile()) 
        || pi->hasMethodProfile(ProfileType_EntryBackedge, *compilationInterface->getMethodToCompile());
}

CompilationContext* CompilationContext::getCurrentContext() {
    CompilationContext* currentCC = ccTls.get();
    return currentCC;
}

static int thread_nb = 0;

struct TlsLogStreams {

    int threadnb;
    MemoryManager mm;

    typedef std::pair<JITInstanceContext*, LogStreams*> Jit2Log;

    typedef StlVector<Jit2Log> Jit2Logs;
    Jit2Logs jit2logs;

    TlsLogStreams ()   
        :threadnb(thread_nb), mm("TlsLogStreams"), jit2logs(mm) {}

    ~TlsLogStreams ();
};


TlsLogStreams::~TlsLogStreams () 
{
    Jit2Logs::iterator ptr = jit2logs.begin(),
                       end = jit2logs.end();
    for (; ptr != end; ++ptr)
        ptr->second->~LogStreams();
}


static TlsStore<TlsLogStreams> tlslogstreams;


/*
    Because CompilationContext is a transient object (it created on start of compilation 
    and destroyed on end of compilation for every method), LogStreams table cannot reside
    in it. Thread-local storage (TLS) is used to keep LogStreams.
    On the other hand, different Jits can run on the same thread, so several LogStreams
    have to be keept for single thread.
    To optimize access, pointer to LogStreams is cached in CompilationContext.

 */
LogStreams& LogStreams::current(JITInstanceContext* jitContext) {

    CompilationContext* ccp = CompilationContext::getCurrentContext()->getVMCompilationInterface()->getCompilationContext();
    LogStreams* cls = ccp->getCurrentLogs();
    if (cls != 0) 
        return *cls;

//  No cached pointer is available for this CompilationContext.
//  Find TLS for this thread.

    TlsLogStreams* sp = tlslogstreams.get();
    if (sp == 0)
    {   // new thread
        ++thread_nb;
        sp = new TlsLogStreams();
        tlslogstreams.put(sp);
    }

//  Find which Jit is running now.

    if (jitContext == 0)
        jitContext = ccp->getCurrentJITContext(); 

//  Was LogStreams created for this Jit already?

    TlsLogStreams::Jit2Logs::iterator ptr = sp->jit2logs.begin(),
                                      end = sp->jit2logs.end();
    for (; ptr != end; ++ptr)
        if (ptr->first == jitContext) {
        //  yes, it was - store pointer in the CompilationContext
            ccp->setCurrentLogs(cls = ptr->second);
            return *cls; 
        }

//  This is the first logger usage by the running Jit in the current thread. 
//  Create LogStreams now.

    cls = new (sp->mm) LogStreams(sp->mm, jitContext->getPMF(), sp->threadnb);
    sp->jit2logs.push_back(TlsLogStreams::Jit2Log(jitContext, cls));
    ccp->setCurrentLogs(cls);

    return *cls;
}


LogStream& LogStream::log (SID sid, HPipeline* hp)
{
    if (hp == 0)
        hp = CompilationContext::getCurrentContext()->getPipeline();
    Str name = ((PMF::Pipeline*)hp)->name;
    return LogStream::log(sid, name.ptr, name.count);
}



} //namespace
