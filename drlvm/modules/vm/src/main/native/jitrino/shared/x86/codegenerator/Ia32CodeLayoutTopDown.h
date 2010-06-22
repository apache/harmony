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

#ifndef _IA32_CODE_LAYOUT_TOP_DOWN
#define _IA32_CODE_LAYOUT_TOP_DOWN

#include "Ia32CodeLayout.h"
namespace Jitrino
{
namespace Ia32 {

/**
* Class to perform top-down block layout. 
* The layout algorithm is similar to the one described in the paper
* "Profile Guided Code Positioning" by Hansen & Pettis. 
*/
class TopDownLayout : public Linearizer {
    friend class Linearizer;
    // To choose a candidate for layout the choice is made from one of two
    // sources. The first source is a control flow successor of the last block
    // to be laid-out assuming this successor can be a fall through successor.
    // The second source is a map containing blocks that have not yet been laid
    // out but have an immediate predecessor that has been laid out.
    // LayoutValue is used to order blocks in this map of blocks yet to be
    // laid-out but that are successors of already laid-out blocks (i.e. 
    // only blocks which are directly connected to already placed blocks 
    // are kept in the set). Hence the set will start out empty before block 
    // layout. During layout when we choose one successor of a block with 
    // multiple successors that have yet to be placed, the successors that 
    // are not chosen will be inserted into the map which is presumed to be 
    // implemented as a RedBlack tree and is hence O(lgN) for insertion, 
    // deletion, and search operations. Two methods to compute the LayoutValue
    // are described below:
    // Option 1: 
    //   LayoutValue is a measure of the connectivity of a block to already 
    //   placed blocks. Connectivity is measured as the sum of the execution  
    //   counts on direct OUT edges from one or more blocks already placed,
    //   to the block in question.
    // Option 2: 
    //   LayoutValue is a measure of the cache locality benefit of placing the
    //   block, and a measure of any opportunity lost from not placing the block
    //   after its most likely predecessor that is yet to be laid-out.
    //
    class TopDownLayoutBlockInfo {
    public:
        BasicBlock* block;
        enum State { NOT_ESTIMATED, LAYOUT_NEIGHBOUR, LAYOUTED} state;
        double layoutValue;
        TopDownLayoutBlockInfo(BasicBlock* b = NULL) : block(b), state(NOT_ESTIMATED), layoutValue(0){};

        struct compare {
            bool operator() (const TopDownLayoutBlockInfo* c1, const TopDownLayoutBlockInfo* c2) const {  //c1 is first
                return less(c2, c1);
            }                    
        };
        bool isLayouted() const {return state == LAYOUTED;}
        bool isLayoutNeighbour() const {return state == LAYOUT_NEIGHBOUR;}
        bool isNotEstimated() const {return state == NOT_ESTIMATED;}

        static bool less(const TopDownLayoutBlockInfo* c1, const TopDownLayoutBlockInfo* c2)  { 
            assert(c1->isLayoutNeighbour() && c2->isLayoutNeighbour());
            double v1 = c1->layoutValue, v2 = c2->layoutValue;
            //topological if ==, Nan,Inf
            return v1 < v2 ? true: (v1 > v2 ? false: c1->block->getId() < c2->block->getId());
        }

    };
    typedef StlSet<TopDownLayoutBlockInfo*, TopDownLayoutBlockInfo::compare>  SortedBlockInfoSet;//type constructor
    typedef StlVector<TopDownLayoutBlockInfo*>  TDBlockInfos;

protected:
    TopDownLayout(IRManager* irManager);
    virtual ~TopDownLayout() {}

    void            linearizeCfgImpl();
private:
    BasicBlock *    pickLayoutCandidate();
    void            layoutBlock(BasicBlock *blk);

    void    processSuccLayoutValue(Node *node, BasicBlock *layoutSucc);
    void    printConnectedBlkMap(::std::ostream & os);


    /* for use in doing top down layout */
    MemoryManager memManager; 
    /** Last block that was laid out*/
    BasicBlock*     lastBlk;        
    /**Set of blocks with TopDownLayoutBlockInfo::state==LAYOUT_NEIGHBOUR */
    SortedBlockInfoSet   neighboursBlocks; 
    /** Array indexed by block id*/
    TDBlockInfos blockInfos;
    
};

#endif

}

}

