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

#ifndef _COMPILATION_CONTEXT_H_
#define _COMPILATION_CONTEXT_H_
#include <assert.h>
namespace Jitrino {

class MemoryManager;
class CompilationInterface;
class JITInstanceContext;
class ProfilingInterface;
class IRManager;
class SessionAction;
class LogStreams;
class HPipeline;
class InliningContext;

#ifdef _IPF_
#else 
namespace Ia32{
    class IRManager;
}
#endif

class CompilationContext {
public:
    // Context of the current compilation
    // Exists only during a compilation of method
    // This class uses destructor and must not be created on MemoryManager
    CompilationContext(MemoryManager& mm, CompilationInterface* ci, JITInstanceContext* jit);
    ~CompilationContext();
    
    CompilationInterface* getVMCompilationInterface() const  {return compilationInterface;}
    MemoryManager& getCompilationLevelMemoryManager() const {return mm;}

    JITInstanceContext* getCurrentJITContext() const {return jitContext;}
    ProfilingInterface* getProfilingInterface() const;

    bool hasDynamicProfileToUse() const;

    bool isCompilationFailed() const {return compilationFailed;}
    void setCompilationFailed(bool f) {compilationFailed = f;}

    bool isCompilationFinished() const {return compilationFinished;}
    void setCompilationFinished(bool f) {compilationFinished = f;}
    
    IRManager* getHIRManager() const {return hirm;}
    void setHIRManager(IRManager* irm) {hirm = irm;}

    SessionAction* getCurrentSessionAction() const {return currentSessionAction;}
    void setCurrentSessionAction(SessionAction* sa) {currentSessionAction = sa;}

    void setCurrentSessionNum(int num) {currentSessionNum = num;}
    int getCurrentSessionNum() const {return currentSessionNum;}

    void setCurrentLogs(LogStreams* lsp) {currentLogStreams = lsp;}
    LogStreams* getCurrentLogs() const {return currentLogStreams;}

    void setPipeline(HPipeline* pipe) {pipeline = pipe;}
    HPipeline* getPipeline () const {return pipeline;}

    void setInliningContext(InliningContext* c) {inliningContext = c;}
    InliningContext* getInliningContext() const {return inliningContext;}

    static CompilationContext* getCurrentContext();

#ifdef _IPF_
#else
    Ia32::IRManager* getLIRManager() const {return lirm;}
    void setLIRManager(Ia32::IRManager* irm) {lirm = irm;}
#endif

private:
    MemoryManager&          mm;
    CompilationInterface*   compilationInterface;
    bool                    compilationFailed;
    bool                    compilationFinished;

    JITInstanceContext*     jitContext;
    IRManager*              hirm;
#ifdef _IPF_
#else
    Ia32::IRManager*        lirm;
#endif
    SessionAction*          currentSessionAction;
    int                     currentSessionNum;
    LogStreams*             currentLogStreams;
    HPipeline*              pipeline;
    InliningContext*        inliningContext;

    void init();
    void initCompilationMode();

public:
    int stageId;
};
}//namespace
#endif
