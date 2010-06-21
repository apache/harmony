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

#ifndef _ESCAPEANALYSIS_H_
#define _ESCAPEANALYSIS_H_

#include "open/types.h"
#include "Stl.h"
#include "optpass.h"

namespace Jitrino {

class IRManager;
class ControlFlowGraph;
class Inst;


//DEFINE_OPTPASS(EscapeAnalysisPass)

//
// Simple escape analyzer
//
class EscapeAnalyzer {
public:
    EscapeAnalyzer(IRManager& irm) : irManager(irm) {};
    //
    // Performs escape analysis and returns the number of instructions
    // that do not escape.
    //
    U_32 doAnalysis();
    U_32 doAggressiveAnalysis();
private:
    IRManager& irManager;
};

//
// Def-use chains
//
class DefUseLink;

class DefUseLink {
public:
    Inst*       getUseInst()    {return useInst;}
    U_32      getSrcIndex()   {return srcIndex;}
    DefUseLink* getNext()       {return next;}
private:
    DefUseLink(Inst* use,U_32 index,DefUseLink* n) 
        : useInst(use), srcIndex(index), next(n) 
    {
    }
    Inst*       useInst;
    U_32      srcIndex;
    DefUseLink* next;
    friend class DefUseBuilder;
};

class DefUseBuilder {
public:
    DefUseBuilder(MemoryManager& mm) : memoryManager(mm), defUseTable(mm) {}

    void initialize(ControlFlowGraph& fg);

    DefUseLink* getDefUseLinks(Inst* defInst) {
        return defUseTable[defInst];
    }

    void    addUses(Inst* useInst);
    void    addDefUse(Inst* defInst,Inst* srcInst,U_32 srcIndex);

    DefUseLink* getDefUse(Inst* defInst,Inst* srcInst,U_32 srcIndex);

    void    removeDef(Inst* defInst);
    void    removeDefUse(Inst* defInst,Inst* srcInst,U_32 srcIndex);
private:
    MemoryManager&                              memoryManager;
    StlHashMap<Inst*,DefUseLink*>    defUseTable;
};

} //namespace Jitrino 

#endif // _ESCAPEANALYSIS_H_
