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
 *
 */
#include "Jitrino.h"
#include "irmanager.h"
#include "FlowGraph.h"
#include "Log.h"
#include "CountWriters.h"
#include "XTimer.h"
#include "VMInterface.h"

#ifdef _WIN32
    #pragma pack(push)
    #include <windows.h>
    #define vsnprintf _vsnprintf
    #pragma pack(pop)
#else
    #include <stdarg.h>
    #include <sys/stat.h>
    #include <sys/types.h>
#endif 

#include "PlatformDependant.h"
#include "JITInstanceContext.h"
#include "PMF.h"
#include "PMFAction.h"

#if defined(_IPF_)
    #include "IpfRuntimeInterface.h"
#else
    #include "ia32/Ia32RuntimeInterface.h"
#endif

#include <ostream>

namespace Jitrino {


// the JIT runtime interface
RuntimeInterface* Jitrino::runtimeInterface = NULL;

JITInstances* Jitrino::jitInstances = NULL;

struct Jitrino::Flags Jitrino::flags;

static TlsKey recursionKey = 0;


// some demo parameters
bool print_hashtable_info = false;
char which_char = 'a';

// read in some flags to test mechanism
bool initialized_parameters = false;
void initialize_parameters(CompilationContext* compilationContext, MethodDesc &md)
{
    // BCMap Info Required
    compilationContext->getVMCompilationInterface()->setBCMapInfoRequired(true);

    // do onetime things
    if (!initialized_parameters) {
        initialized_parameters = true;
    }
}

MemoryManager* Jitrino::global_mm = 0; 

static CountWriter* countWriter = 0;
static CountTime globalTimer("total-compilation time");
static SummTimes summtimes("action times");

void Jitrino::crash (const char* msg)
{
    std::cerr << std::endl << "Jitrino crashed" << std::endl;
    if (msg != 0)
        std::cerr << msg << std::endl;

    exit(11);
}
void crash (const char* fmt, ...)
{
    va_list args;
    va_start(args, fmt);

    char buff[1024];
    vsnprintf(buff, sizeof(buff), fmt, args);

    std::cerr << buff;
    exit(11);
}


bool Jitrino::Init(JIT_Handle jh, const char* name)
{
    //check for duplicate initialization
    JITInstanceContext* jitInstance = getJITInstanceContext(jh);
    if (jitInstance!=NULL) {
        assert(0);
        return false;
    }
    // check if jitName is already in use

    if (jitInstances) {
        for (JITInstances::const_iterator it = jitInstances->begin(), end = jitInstances->end(); it!=end; ++it) {
            JITInstanceContext* jitContext = *it;
            if (jitContext->getJITName() == name) {
                assert(0);
                return false;
            }
        }
    }else {
        global_mm = new MemoryManager("Jitrino::Init.global_mm"); 
#if defined(_IPF_)
        runtimeInterface = new IPF::RuntimeInterface;
        flags.codegen = CG_IPF;
#else
        runtimeInterface = new Ia32::RuntimeInterface;
        flags.codegen = CG_IA32;
#endif
        jitInstances = new (*global_mm) JITInstances(*global_mm);

        flags.time=false;

        recursionKey = Tls::allocKey();
    }

    jitInstance = new (*global_mm) JITInstanceContext(*global_mm, jh, name);
    jitInstances->push_back(jitInstance);

    jitInstance->getPMF().init(jitInstances->size() == 1);

    if (countWriter == 0 && jitInstance->getPMF().getBoolArg(0, "time", false)) {
        countWriter = new CountWriterFile(0);
        XTimer::initialize(true);
    }

    return true;
}

void Jitrino::DeInit(JIT_Handle jh)
{
    JITInstanceContext* jitInstance = getJITInstanceContext(jh);
    if (jitInstance==NULL) {
        assert(0);
        return;
    }

    if (countWriter != 0) {
        jitInstance->getPMF().summTimes(summtimes);
    }
        jitInstance->getPMF().deinit();

    killJITInstanceContext(jitInstance);

    if (jitInstances->empty()) {
        if (countWriter != 0)  {
        delete countWriter;
        countWriter = 0;
    }
    }
}

class FalseSessionAction: public SessionAction {
public:
    virtual void run () {getCompilationContext()->setCompilationFailed(true);}

};
static ActionFactory<FalseSessionAction> _false("false");

class LockMethodSessionAction : public SessionAction {
public:
    virtual void run () {
        CompilationContext* cc = getCompilationContext();
        CompilationInterface* ci = cc->getVMCompilationInterface();
        ci->lockMethodData();
        MethodDesc* methDesc = ci->getMethodToCompile();
        if (methDesc->getCodeBlockSize(0) > 0 || methDesc->getCodeBlockSize(1) > 0){
            cc->setCompilationFinished(true);
            ci->unlockMethodData();
        }
    }
};
static ActionFactory<LockMethodSessionAction> _lock_method("lock_method");

class UnlockMethodSessionAction : public SessionAction {
public:
    virtual void run () {
        getCompilationContext()->getVMCompilationInterface()->unlockMethodData();
    }
};
static ActionFactory<UnlockMethodSessionAction> _unlock_method("unlock_method");


void runPipeline(CompilationContext* c) {

    globalTimer.start();

    PMF::PipelineIterator pit((PMF::Pipeline*)c->getPipeline());
    while (pit.next()) {
        SessionAction* sa = pit.getSessionAction();
        sa->setCompilationContext(c);
        c->setCurrentSessionAction(sa);
        c->stageId++;
        sa->start();
        sa->run();
        sa->stop();
        c->setCurrentSessionAction(0);
        if (c->isCompilationFailed() || c->isCompilationFinished()) {
            break;
        }
    }

    globalTimer.stop();
}

bool compileMethod(CompilationContext* cc) {
    if(Jitrino::flags.skip) {
        return false;
    }


    runPipeline(cc);
    
    bool success = !cc->isCompilationFailed();
    return success;
}


bool Jitrino::CompileMethod(CompilationContext* cc) {
    CompilationInterface* compilationInterface = cc->getVMCompilationInterface();
    bool success = false;
    MethodDesc& methodDesc = *compilationInterface->getMethodToCompile();
    initialize_parameters(cc, methodDesc);
    
    if (methodDesc.getByteCodeSize() <= 0) {
        Log::out() << " ... Skipping because of 0 byte codes ..." << ::std::endl;
        assert(0);
    } else {
        success = compileMethod(cc);
    }
    return success;
}

JITInstanceContext* Jitrino::getJITInstanceContext(JIT_Handle jitHandle) {
    if (jitInstances)
        for (JITInstances::const_iterator it = jitInstances->begin(), end = jitInstances->end(); it!=end; ++it) {
            JITInstanceContext* jit= *it;
            if (jit->getJitHandle() == jitHandle) {
                return jit;
            }
        }
    return NULL;
}

void Jitrino::killJITInstanceContext(JITInstanceContext* jit) {
    for (JITInstances::iterator it = jitInstances->begin(), end = jitInstances->end(); it!=end; ++it) {
        if (*it == jit) {
            jitInstances->erase(it);
            return;
        }
    }
}



int Jitrino::getCompilationRecursionLevel() {
    return (int)(POINTER_SIZE_INT)Tls::get(recursionKey);
}

void Jitrino::incCompilationRecursionLevel() {
    int recursion = (int)(POINTER_SIZE_INT)Tls::get(recursionKey);
    Tls::put(recursionKey, (void*)(POINTER_SIZE_INT)(recursion+1));
}

void Jitrino::decCompilationRecursionLevel() {
    int recursion = (int)(POINTER_SIZE_INT)Tls::get(recursionKey);
    Tls::put(recursionKey, (void*)(POINTER_SIZE_INT)(recursion-1));
}


} //namespace Jitrino 
/*--------------------------------------------------------------------
* DllMain definition (Windows only) - DLL entry point function which
* is called whenever a new thread/process which executes the DLL code
* is started/terminated.
* Currently it is used to notify log system only.
*--------------------------------------------------------------------
*/

#if defined(_WIN32) || defined(_WIN64)

extern "C" bool __stdcall DllMain(void *dll_handle, U_32 reason, void *reserved) {

    switch (reason) { 
    case DLL_PROCESS_ATTACH: 
        // allocate a TLS index.
        // fall through, the new process creates a new thread

    case DLL_THREAD_ATTACH: 
        // notify interested parties (only one now)
        break; 

    case DLL_THREAD_DETACH: 
        // notify interested parties (only one now)
        Jitrino::Tls::threadDetach();
        break; 

    case DLL_PROCESS_DETACH: 
        // notify interested parties (only one now)
        // release the TLS index
        Jitrino::Tls::threadDetach();
        break; 

    default:
        break; 
    } 
    return TRUE; 
}

#endif // defined(_WIN32) || defined(_WIN64)
