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

#ifndef _IA32_CODE_LAYOUT_BOTTOM_UP
#define _IA32_CODE_LAYOUT_BOTTOM_UP

#include "Ia32CodeLayout.h"
namespace Jitrino
{
namespace Ia32 {

/**
* Class to perform bottom-up block layout. 
* The layout algorithm is similar to the one described in the paper
* "Profile Guided Code Positioning" by Hansen & Pettis. 
*/
class BottomUpLayout : public Linearizer {
    friend class Linearizer;
protected:
    BottomUpLayout(IRManager* irManager);
    virtual ~BottomUpLayout() {}

    void linearizeCfgImpl();
private:

    class Chain {
    public:
        BasicBlock* first;
        BasicBlock* last;
        Chain (BasicBlock* f, BasicBlock* l) : first(f), last(l){}
        
    };
    
    
    typedef StlSet<BasicBlock*> BlocksSet;


    void layoutEdge(Edge *e);
    void putDispatchSuccessorsInChain();
    void combineChains();

    
    
    /* for use in doing bottom up layout */
    MemoryManager     mm; 

    StlVector<bool> firstInChain;
    StlVector<bool> lastInChain;
    StlVector<BasicBlock*> prevInLayoutBySuccessorId;
    

};


}} //namespace

#endif

