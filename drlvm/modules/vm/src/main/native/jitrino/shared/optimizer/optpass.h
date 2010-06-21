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
 * @author Intel, Pavel A. Ozhdikhin
 *
 */

#ifndef _OPT_PASS_H_
#define _OPT_PASS_H_


#include "CompilationContext.h"
#include "PMFAction.h"

#include <string>

namespace Jitrino {

class IRManager;
class MemoryManager;

 
class OptPass : public SessionAction {
public:
    //
    // The name of this optimization pass.  E.g., "Partial Redundancy Elimination"
    //
    virtual const char* getName() = 0;

    //
    // The short name of this pass.  Should be a single word.  E.g., "pre". 
    //
    virtual const char* getTagName() = 0;

    //
    // Run the pass.
    //
    void run();

  
    //
    // Services to compute dominators, loops, ssa if necessary. 
    // These methods only recompute if a change is detected.
    //
    static void computeDominators(IRManager& irm);
    static void computeLoops(IRManager& irm, bool normalize = true);
    static void computeDominatorsAndLoops(IRManager& irm, bool normalizeLoops = true);
    static void dce(IRManager& irm);
    static void uce(IRManager& irm, bool fixup_ssa);
    static void fixupSsa(IRManager& irm);
    static void splitCriticalEdges(IRManager& irm);
    static bool isProfileConsistent(IRManager& irm);
    static void smoothProfile(IRManager& irm);

    static void printHIR(IRManager& irm);
    static void printDotFile(IRManager& irm, int id, const char* name, const char* suffix);
    static void printDotFile(IRManager& irm, const char* suffix);

    static const char* indent(IRManager& irm);
protected:
    //
    // Virtual dtor - to provide a correct cleanup for derived classes and 
    // to avoid many compiler's warnings 
    //
    virtual ~OptPass() {};
    //
    // Callback to subclass to run optimization.
    //
    virtual void _run(IRManager& irm) = 0;

    void composeDotFileName(char * name, const char * suffix);
    void printHIR(IRManager& irm, const char* when);

    void initialize();

private:
    unsigned id;
};


#define DEFINE_SESSION_ACTION_WITH_ACTION(classname, actionclass, tagname, fullname) \
class classname : public OptPass { \
protected: \
    void _run(IRManager& irm); \
    const char* getName() { return fullname; } \
    const char* getTagName() { return #tagname; } \
}; \
ActionFactory<classname, actionclass> tagname##_(#tagname);


#define DEFINE_SESSION_ACTION(classname, tagname, fullname) \
DEFINE_SESSION_ACTION_WITH_ACTION(classname, Action, tagname, fullname)

} //namespace Jitrino 


#endif //_OPT_PASS_H_
