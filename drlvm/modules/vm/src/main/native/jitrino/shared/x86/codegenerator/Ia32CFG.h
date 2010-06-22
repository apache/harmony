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
 * @author Intel, Vyacheslav P. Shakin, Mikhail Y. Fursov
 */

#ifndef _IA32_CFG_H_
#define _IA32_CFG_H_

#include "ControlFlowGraph.h"
#include "MemoryManager.h"
#include "Stl.h"
#include "BitSet.h"
#include "open/types.h"

namespace Jitrino
{
namespace Ia32{

    class IRManager;

    
 
    //=========================================================================================================
    //  edge with the exception information
    //=========================================================================================================
    /** class CatchEdge is specialization of Edge representing an edge 
    from a dispatch node to a handler block 
    */
    class CatchEdge : public Edge {
        friend class IRManager;
    public:
        /** Returns the caught exception type associated with the edge */
        Type *  getType()const {return type;}
        void    setType(Type* _type) {type = _type;}
        /** Returns the priority of the edges during exception handling 

        If the same exception can be handled by several
        exception handlers, the handler with the highest priority handles it.
        The smaller the priority number the hight is the priority.  
        */
        U_32  getPriority()const {return priority;}
        void    setPriority(U_32 p) {priority = p;}

        //---------------------------------------------------------------------------------------------
    protected: 
        CatchEdge()
            : type(NULL), priority(0) { }

            //---------------------------------------------------------------------------------------------
    protected:
        /** The type of a caught exception */
        Type *      type;    

        /**  Priority of the edge. If the same exception can be handled by several
        exception handlers, the handler with the highest priority handles it.
        The smaller the priority number the hight is the priority.
        */
        U_32      priority;

        //---------------------------------------------------------------------------------------------
    };


    //=========================================================================================================
    //  node
    //=========================================================================================================
    /** class Node is a base class for all nodes in the CFG.*/
    class CGNode : public Node {
    public:
        enum OrderType {
            OrderType_Arbitrary=0,
            OrderType_Layout,
            OrderType_Postorder,
            OrderType_Topological, 
            OrderType_ReversePostorder = OrderType_Topological
        };

        /** Returns the persistent id of the node (HIR CFG node id resulting to this node) */
        U_32              getPersistentId()const {return persistentId; }
        /** Sets the persistent id of the node (HIR CFG node id resulting to this node) */
        void                setPersistentId(U_32 persId) { persistentId = persId; }

        IRManager &         getIRManager() { return irm; }
        
        virtual void        verify();

        
        //---------------------------------------------------------------------------------------------
    protected: 

        CGNode(MemoryManager& mm, IRManager& _irm, Node::Kind kind) 
            : Node(mm, kind), irm(_irm), liveAtEntry(new (mm) BitSet(mm, 0)), persistentId(0) {}

        BitSet*        getLiveAtEntry() const {return liveAtEntry;}

        IRManager&      irm;
        BitSet*         liveAtEntry;
        U_32         persistentId;
       
        friend class IRManager;
       
    };


    //=========================================================================================================
    //  Basic block
    //=========================================================================================================
    /** class BasicBlock represents basic blocks of a CFG: nodes which can contain intructions */
    class BasicBlock : public CGNode {
    friend class IRManager;
    public:
        
        /** Returns the basic block which is the layout successor of this one 
        The returned value must be set using setLayoutSucc by a code layout algorithm
        */
        BasicBlock *    getLayoutSucc()const {return layoutSucc;}

        /** Sets the basic block which is the layout successor of this one  */
        void            setLayoutSucc(BasicBlock *bb) {assert(bb!=this); layoutSucc = bb;}

        /** sets the offset of native code for this basic block */
        void            setCodeOffset(U_32 offset) {codeOffset = offset;}
        /** returns the offset of native code for this basic block */
        U_32          getCodeOffset()const    {   return codeOffset;  }
        /** sets the size of native code for this basic block */
        void            setCodeSize(U_32 size) {codeSize = size;}
        /** returns the size of native code for this basic block */
        U_32          getCodeSize()const  {   return codeSize;    }
        
        /** returns the pointer to the native code for this basic block */
        void*           getCodeStartAddr() const;

        void    verify();
    //---------------------------------------------------------------------------------------------
    protected: 
        BasicBlock(MemoryManager& mm, IRManager& irm) 
            : CGNode(mm, irm, Node::Kind_Block), layoutSucc(NULL), codeOffset(0), codeSize(0), codeAddr(0){}

        
    protected:
        BasicBlock *    layoutSucc; 
        U_32          codeOffset;
        U_32          codeSize;
        void*           codeAddr;
    };

}; //namespace Ia32
}
#endif // _IA32_FLOWGRAPH_H
