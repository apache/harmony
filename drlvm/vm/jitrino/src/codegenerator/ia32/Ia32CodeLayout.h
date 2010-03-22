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

#ifndef _IA32_CODE_LAYOUT
#define _IA32_CODE_LAYOUT


#include "Ia32IRManager.h"

namespace Jitrino
{
namespace Ia32 {

class BasicBlock;

class Layouter : public SessionAction {
    void runImpl();
    U_32 getSideEffects() const {return 0;}
    U_32 getNeedInfo()const{ return 0;}
};

/**
 *  Base class for code layout 
 */
class Linearizer {
public:
    enum LinearizerType { TOPOLOGICAL, TOPDOWN, BOTTOM_UP};
    virtual ~Linearizer() {}
    static void doLayout(LinearizerType t, IRManager* irManager);
    static void checkLayout(IRManager* irm);

protected:
    Linearizer(IRManager* irMgr);
    void linearizeCfg();
    virtual void linearizeCfgImpl() = 0;
    /** Fix branches to work with the code layout */
    void fixBranches();
    
    
    /** Returns true if edge can be converted to a fall-through edge (i.e. an edge
     * not requiring a branch) assuming the edge's head block is laid out after the tail block. 
     */
    bool canEdgeBeMadeToFallThrough(Edge *edge);
    
    /** checks if CFG has no BB nodes without layout successors*/
    bool isBlockLayoutDone();

    //  Fields
    IRManager* irManager;

private:
   /**  Add block containing jump instruction to the fallthrough successor
    *  after this block
    */
    BasicBlock* addJumpBlock(Edge * jumpEdge);

    /**  Reverse branch predicate. We assume that branch is the last instruction
    *  in the node.
    */
    bool reverseBranchIfPossible(Node * bb);

};


/** 
 *   Reverse post-order (topological) code layout 
 */
class TopologicalLayout : public Linearizer {
    friend class Linearizer;
protected:
    TopologicalLayout(IRManager* irManager) : Linearizer(irManager){};
    virtual ~TopologicalLayout() {}
    void linearizeCfgImpl();
};

}} //namespace

#endif


