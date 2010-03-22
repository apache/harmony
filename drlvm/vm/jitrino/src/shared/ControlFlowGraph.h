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

/**
* @file
* The baseline implementation of control flow graph (CFG) and 
* CFG level routines.
* 
* The implementation can be reused and extended by different
* components as a base level for IR representation.
*/


#ifndef _CONTROLFLOWGRAPH_
#define _CONTROLFLOWGRAPH_ 


#include <assert.h>
#include <iostream>

#include "Dlink.h"
#include "Stl.h"

#include "open/types.h"

namespace Jitrino {

class Edge;
class Node;
class CFGInst;
class ControlFlowGraph;
class Type;
class DominatorTree;
class LoopTree;

typedef StlVector<Edge*> Edges;
typedef StlVector<Node*> Nodes;

  /**
   * The edge connecting two nodes in CFG and having a direction and type.
   * To extend the given class with a custom properties,
   * override the <code>createEdge</code> method 
   * of the ControlFlowGraphFactory class.
   */
class Edge {
  /** 
   * The ControlFlowGraph class that requires additional privileges to 
   * initialize protected fields of the Edge instance.
   */
    friend class ControlFlowGraph;
    
  /** 
   * The ControlFlowGraphFactory class that requires additional access privileges 
   * to access the protected constructor.
   */
    friend class ControlFlowGraphFactory;
public:
  /** 
   * The edge kind.
   * The edge kind depends on the <code>Source</code> and the
   * <code>Target</code> node kinds and is calculated
   * every time the <code>getKind</code> method is invoked.
   */
    enum Kind {        
        /// All edges to the <code>Dispatch</code> node (DN) are <code>Dispatch</code> edges.
        Kind_Dispatch = 1,       
        /// Jump, fall-through with no branch, indirect jump or edge to the <code>Exit</code> node.
        Kind_Unconditional = 2,  
        /// Fall-through after a not-taken branch, default target of switches (max one out-edge per node).
        Kind_False = 4,          
        /// A taken branch.
        Kind_True = 8,           
        /// All edges from DN to <code>Block</code> node (BNCFGInst::next() == CFGInst::prev() ) are <code>Catch</code> edges.
        Kind_Catch = 16          
    };
    
    /// The destructor. Does nothing.
    virtual ~Edge() {}


  /** 
   * ID is constant for the lifetime of the edge.
   *
   * @return The unique edge ID that is constant for lifetime of this edge.
   */
    U_32 getId() const {return id;}

  /** 
   * Returns the edge kind.
   *
   * @return The kind of the given edge.
   */

    virtual Kind getKind() const;

  /** 
   * Checks whether the edge is a <code>Dispatch</code> one, that is if the  
   * edge is of the Edge::Kind_Dispatch kind.
   *
   * @return <code>TRUE</code> for the Edge::Kind_Dispatch
   *         edge; otherwise, <code>FALSE</code>.
   */
    bool isDispatchEdge() const {return getKind() == Edge::Kind_Dispatch;}

  /** 
   * Checks whether the edge is an <code>Unconditional</code> one, that is if 
   * the edge is of the Edge::Kind_Unconditional kind.
   *
   * @return <code>TRUE</code> for the Edge::Kind_Unconditional
   *         edge; otherwise, <code>FALSE</code>.
   */
    bool isUnconditionalEdge() const {return getKind() == Edge::Kind_Unconditional;}

  /** 
   * Checks whether the edge is a <code>False</code> one, that is if the   
   * edge is of the Edge::Kind_False kind.
   *
   * @return <code>TRUE</code> for the Edge::Kind_False
   *         edge; otherwise, <code>FALSE</code>.
   */
    bool isFalseEdge() const {return getKind() == Edge::Kind_False;}

  /** 
   * Checks whether the edge is a <code>True</code> one, that is if the edge is  
   * of the Edge::Kind_True kind.
   *
   * @return <code>TRUE</code> for the Edge::Kind_True
   *         edge; otherwise, <code>FALSE</code>.
   */
    bool isTrueEdge() const {return getKind() == Edge::Kind_True;}

  /** 
   * Checks whether the edge is a <code>Catch</code> one, that is if the edge 
   * is of the Edge::Kind_Catch kind.
   *
   * @return <code>TRUE</code> for the Edge::Kind_Catch
   *         edge; otherwise, <code>FALSE</code>.
   */
    bool isCatchEdge() const {return getKind() == Edge::Kind_Catch;}

  /** 
   * Returns the <code>Source</code> node of the edge. The <code>Source</code>  
   * node is also called the <code><code>Tail</code></code> or <code>From</code> 
   * node.
   * 
   * @return The <code>Source</code> node of the edge.
   */
    Node* getSourceNode() const {return source;}

  /** 
   * Returns the <code>Target</code> node of the edge. The <code>Target</code>  
   * node is also called the <code>Head</code> or <code>To</code> node.
   * 
   * @return The <code>Target</code> node of the edge.
   */
    Node* getTargetNode() const {return target;}

  /** 
   * Returns the <code>Target</code> or the <code>Source</code> node depending  
   * on the <code>isForward</code> parameter.
   *
   * @param[in] isForward - tells which node to return
   *
   * @return If the <code>isForward</code> parameter is <code>True</code>, 
   *         returns the <code>Target</code> node; otherwise, 
   *         the <code>Source</code> node. 
   */
    Node* getNode(bool isForward) const {return isForward ? target : source;}


  /**
   * @return The probability of the edge.
   */
    double getEdgeProb() const {return prob;}
    
  /**
   * Sets the probability of the edge.
   *
   * @param[in] val - a new edge probability
   */
    void setEdgeProb(double val) {prob = val;}

protected:
   /** 
    * Initializes a new Edge instance.
    * The ID field is initialized with <code>MAX_UINT32</code>,
    * all other fields are initialized with 0.
    */
    Edge() : id (MAX_UINT32), source(NULL), target(NULL), prob(0) {}
    
   /** 
    * The helper method to set ID to the edge
    *
    * @param[in] _id - a new edge ID 
    */
    void setId(U_32 _id) {id = _id;}

    /// The unique edge ID within CFG.
    U_32 id;

    /// The <code>Source</code> or the <code>Tail</code> node of the edge.
    Node *source;
    
    /// The <code>Target</code> or the <code>Head</code> node of the edge.
    Node *target;

   /** The edge probability. 
     * If the ControlFlowGraph edge profile is valid, the sum of all 
     * outgoing edge probabilities for a node is equal to <code>1</code>.
     */
    double prob;
};


#define ILLEGAL_BC_MAPPING_VALUE 0xFFFF
  /** 
   * The base abstract class for all CFG instructions.
   * The given class keeps information about the node this instruction belongs
   * to, about previous and next instructions in the node and about instruction 
   * properties needed to perform CFG validity checks and to detect edge types  
   * for block-to-block edges correctly.
   * 
   * @note The CFGInst class behaves like <code>DLink</code>, 
   *       when none node is assigned to the instruction.
   */
class CFGInst : protected Dlink {
  /** 
   * The Node class needs additional privileges to 
   * initialize protected fields of the CFGInst instance.
   */
    friend class Node;

  /** 
   * The Edge class needs additional privileges to 
   * initialize protected fields of the CFGInst instance.
   */
    friend class Edge;

  /** 
   * The ControlFlowGraph class needs additional privileges to 
   * initialize protected fields of the CFGInst instance.
   */
    friend class ControlFlowGraph;
public:
    virtual ~CFGInst(){}

  /** 
   * Gets the current node the instruction belongs to. 
   *
   * @return The node instance the given instruction belongs to or   
   *         <code>NULL</code> if the instruction was unlinked or never added 
   *         to any node.
   */
    Node* getNode() const {return node;}
    
  /** 
   * Gets the next instruction.
   *
   * @return The next instruction in the node, or <code>NULL</code> if the 
   *         instruction is the last instruction in the node.
   *
   * @note The CFGInst class behaves like <code>DLink</code> when none 
   *       node is assigned to the instruction.
   */
    CFGInst* next() const; 
    
  /** 
   * Gets the previous instruction. 
   *
   * @return Returns the previous instruction in the node; if the instruction 
   *         is the first one in the node, returns <code>NULL</code>.
   *
   * @note The CFGInst class behaves like <code>DLinkM</code> when none node 
   *       is assigned to the instruction.
   */
    CFGInst* prev() const;

  /** 
   * Removes the instruction from the node.
   *
   * @note The CFGInst class behaves like <code>DLink</code> when none node 
   *       is assigned to the instruction.<br>
   *       CFGInst::next() == CFGInst::prev() == this after the call.
   */
    void unlink() {node = NULL; Dlink::unlink();}
    
  /** 
   * Inserts the instruction before <code>instBefore</code>. 
   * Assigns the node from <code>instBefore</code> to the instruction.
   *
   * @param[in] instBefore - the insertion point, before which the instruction 
   *                         goes 
   *                     
   * @note If the instruction is followed by other instructions, the call 
   *       inserts and assigns them to the node. Only instructions with the 
   *       <code>NULL</code> node can be inserted. 
   */
    void insertBefore(CFGInst* instBefore);
    
  /** 
   * Inserts the instruction after <code>instAfter</code>. 
   * Assigns the node from <code>instAfter</code> to the instruction.
   *
   * @param[in] instAfter - the insertion point, before which the instruction 
   *                        goes 
   *
   * @note If the instruction is followed by other instructions, the call 
   *       inserts and assigns them to the node.
   *       Only instructions with the <code>NULL</code> node can 
   *       be inserted.
   */
    void insertAfter(CFGInst* instAfter);

  /** 
   * Checks whether the instruction is the header critical one.
   * Only another header critical instruction can precede the 
   * header critical instruction.
   * CFG uses the given property to check validity when adding 
   * or removing instructions to the node.
   *
   * @return <code>TRUE</code> for the header critical instruction.
   */
    virtual bool isHeaderCriticalInst() const {return isLabel();}

  /** 
   * Checks whether the instruction is the label one.
   * Only one label instruction per node can exist.
   * CFG uses the given property to check validity when adding 
   * or removing instructions to the node.
   *
   * @return <code>TRUE</code> for the label instruction.
   *
   * @note The label instruction is also a header critical instruction.
   */
    virtual bool isLabel() const {return false;}

    /// Returns byte-code offset the inst
    uint16 getBCOffset() const { return bcOffset;}

    /// Sets byte-code offset the inst
    void setBCOffset(uint16 newVal) {bcOffset = newVal;}

protected:
    /// Called from CFG to detect edge kinds for BN to BN edges.
    virtual Edge::Kind getEdgeKind(const Edge* edge) const = 0;
    /// Called from CFG, when the edge target is replaced.
    virtual void updateControlTransferInst(Node* oldTarget, Node* newTarget) {}
    /// Called from CFG when two blocks are merging and one of the branches is redundant.
    virtual void removeRedundantBranch(){};

protected:
  /** 
   * The default constructor.
   * Initializes the Node instance field with the <code>NULL</code> 
   * value.
   */
    CFGInst() : node(NULL), bcOffset(ILLEGAL_BC_MAPPING_VALUE) {}

    /// The owner node of the instruction.
    Node* node;

    ///offset in byte-code
    uint16 bcOffset;
};

  /** 
   * The node representation and the base class for all nodes in CFG.
   * Three node classes in the control flow graph exist:
   * <code>Block</code> nodes, <code>Dispatch</code> nodes, and the 
   * <code>Exit</code> node.
   * All of them can contain an instruction.
   */
class Node {
  /** 
   * The ControlFlowGraph class that requires additional privileges to 
   * initialize protected fields of the <code>Node</code> instance.
   */
    friend class ControlFlowGraph;

  /** 
   * The ControlFlowGraphFactory class that requires additional privileges to 
   * access to the protected constructor of the <code>Node</code> instance.
   */
    friend class ControlFlowGraphFactory;

  /** 
   * The CFGInst class that requires additional privileges to 
   * initialize protected fields of the <code>Node</code> instance.
   */
    friend class CFGInst;
public:
  /** 
   * The kind of the node.
   * CFG has three node kinds:<br>
   * <ul><li>
   * <code>Kind_Block</code> - used for usual nodes with instructions<br>
   * <li><code>Kind_Dispatch</code> - used for exceptions paths. The 
   *                 <code>Dispatch</code> node connects the <code>Block</code> 
   *                 node throwing exception with another <code>Block</code> 
   *                 node catching the exception. 
   *                 If none <code>Block</code> node catches all possible   
   *                 exception types, the <code>Dispatch</code> node can also 
   *                 be connected with another <code>Dispatch</code> node.
   *                 To represent a path when an exception is not caught in
   *                 a current method, the <code>Unwind</code> node is used.<br> 
   * <li><code>Kind_Exit</code> - CFG has only one <code>Exit</code> node. The 
   *                 <code>Exit</code> node postdominates any exit path from
   *                 the methods. Only CFG without exits, like 
   *                 <code>While(True);</code> patterns, can have no
   *                 <code>Exit</code> node at all.</ul>
   */
    enum Kind {
        /// Basic <code>Block</code> node
        Kind_Block,      
        /// <code>Dispatch</code> node
        Kind_Dispatch,   
        /// <code>Exit</code> node
        Kind_Exit        
    };

    virtual ~Node() {}

  /**
   * Gets the kind of the node.
   *
   * @return The kind of the node.
   */
    Kind getKind() const {return kind;}

  /** 
   * Gets the unique node ID. The ID is unique for all nodes in the containing 
   * flow graph and is constant for the lifetime of this node. 
   * This method provides a sparse ID between zero
   * and the graph <code>getMaxNodeId()</code> value.
   *
   * @return The unique node ID in the containing flow graph.
   */
    U_32 getId() const {return id;}

  /** 
   * Gets the depth-first numbering of the given node computed during the last 
   * pass.
   * Use this number as a dense ID of the node until CFG is not modified.
   *
   * @return The depth of the first numbering of the given node.
   *
   * @note Df-numbering differes from pre-numbering by being recalculated only  
   *      after the direct graph ordering, that is when the  
   *      <code>isForward</code> parameter is <code>TRUE</code> in the 
   *      <code>orderNodes(isForward)</code> method call.
   */
    U_32 getDfNum() const {return dfNumber;}
    
  /** 
   * Gets the pre-num numbering of the given node computed during the last pass.
   * Use this number as a dense ID of the node until CFG is not modified.
   *
   * @return The pre-num numbering of the given node.
   */
    U_32 getPreNum() const {return preNumber;}
    
  /** 
   * Gets the post-num numbering of the given node computed during the last pass.
   * Use this number as a dense ID of the node until CFG is not modified.
   *
   * @return The post-num numbering of the given node.
   */
    U_32 getPostNum() const {return postNumber;}

    
  /**
   * Checks whether the node is a <code>Block</code> one.
   *
   * @return <code>TRUE</code> for the Node::Kind_Block node.
   */
    bool isBlockNode() const {return getKind() == Node::Kind_Block;}
    
  /** 
   * Checks whether the node is a <code>Dispatch</code> one.
   *
   * @return <code>TRUE</code> for the Node::Kind_Dispatch node.
   */
    bool isDispatchNode() const {return getKind() == Node::Kind_Dispatch;}
    
  /** 
   * Checks whether the node is an <code>Exit</code> one.
   *
   * @return <code>TRUE</code> for the Node::Kind_Exit node.
   */
    bool isExitNode() const {return getKind() == Node::Kind_Exit;}
    
  /** 
   * Checks whether the node is a <code>Catch</code> one.
   *
   * @return <code>TRUE</code> if the node is of the Node::Kind_Block kind and 
   *         the only incoming edge of the node is of the Edge::Kind_CatchEdge 
   *         kind.
   */
   bool isCatchBlock() const {return getInDegree() >=1 && getInEdges().front()->isCatchEdge();}

  /** 
   * Gets the incoming edges to the node.
   *
   * @return The collection of the incoming edges to the node.
   *
   * @note The ordering of edges in the collection is not specified.
   */
    const Edges& getInEdges() const {return inEdges;}
    
  /**
   * Gets the outgoing edges to the node.
   *
   * @return The collection of the outgoing edges to the node.
   *
   * @note The ordering of edges in the collection is not specified.
   */
    const Edges& getOutEdges() const {return outEdges;}

  /** 
   * Gets the incoming or outgoing edges to the node.
   *
   * @param[in] isForward - tells what kind of edges, incoming or outgoing, 
   *                        to return
   *
   * @return  If the <code>isForwarisForward</code> parameter is <code>TRUE</code>,
   *          returns the collection of incoming edges;
   *          otherwise, the collection of outgoing ones.
   *         
   * @note The ordering of edges in the collection is not specified.
   */
    const Edges& getEdges(bool isForward) const {return isForward ? outEdges : inEdges;}
    
  /** 
   * Gets the first outgoing edge that matches the <code>edgeKind</code> kind.  
   *
   * @param[in] edgeKind - the kind of the edge to find
   * 
   * @return The first outgoing edge matching the <code>edgeKind</code> parameter; 
   *         <code>NULL</code> if no edge of the specified kind has been found.
   *
   */
    Edge* getOutEdge(Edge::Kind edgeKind) const;

  /** 
   * Gets the first unconditional outgoing edge.  
   *
   * @return The first outgoing edge matching the Edge::Kind_Unconditional  
   *         parameter; <code>NULL</code> if no edge of the specified 
   *         kind has been found.
   *
   * @note The edge collections ordering is not specified.
   */
    Edge* getUnconditionalEdge() const {return getOutEdge(Edge::Kind_Unconditional);}

  /** 
   * Gets the outgoing edge with the Edge::Kind_False kind.  
   *
   * @return The first outgoing edge matching the Edge::Kind_False
   *         parameter; <code>NULL</code> if no edge of the specified 
   *         kind has been found.
   *
   * @note The only one outgoing edge of the Edge::Kind_False kind is 
   *       allowed per node.
   */
    Edge* getFalseEdge() const {return getOutEdge(Edge::Kind_False);}

  /** 
   * Gets the first <code>True</code> outgoing edge.
   *
   * @return The first outgoing edge matching the Edge::Kind_True 
   *        parameter; <code>NULL</code> if no edge of the specified 
   *        kind has been found.
   *
   * @note The edge collections ordering is not specified.
   */
    Edge* getTrueEdge() const {return getOutEdge(Edge::Kind_True);}
    
  /** 
   * Gets the outgoing edge of the Edge::Kind_Dispatch kind.
   *
   * @return The first outgoing edge matching the Edge::Kind_Dispatch
   *         parameter; <code>NULL</code> if no edge of the specified 
   *         kind has been found.
   *
   * @note The only one outgoing edge of the Edge::Kind_Dispatch kind is 
   *       allowed per node.
   */
    Edge* getExceptionEdge() const {return getOutEdge(Edge::Kind_Dispatch);}

  /** 
   * Checks whether the node is connected to <code>anotherNode</code>.
   *
   * @param[in] isForward   - the direction of the connection. If <code>TRUE</code> 
   *                          the outgoing edges are checked, if <code>FALSE</code> 
   *                          the incoming edges are checked.
   *
   * @param[in] anotherNode - the node to check connection with
   *
   * @return <code>TRUE</code> if the node is connected with <code>anotherNode</code>  
   *         using <code>isForward</code> edges; otherwise, returns <code>FALSE</code>, 
   *         or <code>TRUE</code>. 
   */
    bool  isConnectedTo(bool isForward, Node* anotherNode) const {return findEdgeTo(isForward, anotherNode)!=NULL;}


  /** 
   * Finds the edge connected to <code>anotherNode</code>.
   *
   * @param[in] isForward - the direction of the connection. If <code>TRUE</code> 
   *                        the outgoing edges are checked, if <code>FALSE</code> 
   *                        the incoming edges are checked.
   * 
   * @param[in] anotherNode - the node that the edge connects to
   *
   * @return The edge connects the node and the <code>anotherNode</code> with 
   *         the direction <code>isForwarisForward</code>; 
   *         otherwise <code>NULL</code>. 
   */
    Edge*  findEdgeTo(bool isForward, Node* anotherNode) const;
    
  /** 
   * Finds the first outgoing edge with the specified kind 
   * and returns its <code>Target</code> node.
   *
   * @param[in] edgeKind - the kind of the edge to find
   *
   * @return The <code>Target</code> node of the outgoing edge of the 
   *         <code>edgeKind</code> kind; <code>NULL</code> if such an
   *         edge has not beed found.
   *
   */
    Node* getEdgeTarget(Edge::Kind edgeKind) const {Edge* edge = getOutEdge(edgeKind); return edge == NULL ? NULL : edge->getTargetNode();}

  /** 
   * Finds the first unconditional outgoing edge and returns its 
   * <code>Target</code> node.
   *
   * @return The <code>Target</code> node of the outgoing edge of 
   *         the Edge::Kind_Unconditional kind; <code>NULL</code>
   *         if such an edge has not beed found.
   *
   */
    Node* getUnconditionalEdgeTarget() const {return getEdgeTarget(Edge::Kind_Unconditional);}

  /** 
   * Finds the <code>False</code> outgoing edge and returns its 
   * <code>Target</code> node.
   *
   * @return The <code>Target</code> node of the outgoing edge of 
   *         the Edge::Kind_False kind; <code>NULL</code> if such an 
   *         edge has not beed found.
   */
    Node* getFalseEdgeTarget() const  {return getEdgeTarget(Edge::Kind_False);}

  /** 
   * Finds the first <code>True</code> outgoing edge and returns its 
   * <code>Target</code> node.
   *
   * @return The <code>Target</code> node of the outgoing edge of the 
   *         Edge::Kind_True kind; <code>NULL</code> if such an edge 
   *         has not beed found.
   *
   */
    Node* getTrueEdgeTarget() const  {return getEdgeTarget(Edge::Kind_True);}

  /** 
   * Finds the <code>Dispatch</code> edge and returns its <code>Target</code> 
   * node.
   *
   * @return The <code>Target</code> node of the outgoing edge of the 
   *         Edge::Kind_Dispatch kind; <code>NULL</code> if such an edge
   *         has not beed found.
   */
    Node* getExceptionEdgeTarget() const  {return getEdgeTarget(Edge::Kind_Dispatch);}
    
  /** 
   * Finds the edge connecting the node with <code>anotherNode</code>.
   *
   * @param[in] isForward   - the search in the outgoing edges, if  
                              <code>TRUE</code>; otherwise, in the incoming edges 
   * @param[in] anotherNode - the node connected with the given node by the edge
   *
   * @return The edge connecting the node with <code>anotherNode</code>;
   *         otherwise, <code>NULL</code>.
   */
    Edge* findEdge(bool isForward, const Node* anotherNode) const;

  /** 
   * Finds the incoming edge connecting the node with <code>anotherNode</code>.
   *
   * @param[in] anotherNode - the node connected with the given node by the edge
   *
   * @return The incoming edge connecting the node with <code>anotherNode</code>;
   *         otherwise, <code>NULL</code>.
   */
    Edge* findSourceEdge(const Node* source) const {return findEdge(false, source);}

  /** 
   * Finds the outgoing edge connecting the node with <code>anotherNode</code>.
   *
   * @param[in] anotherNode - the node connected with the given node by the edge
   *
   * @return The outgoing edge connecting the node with <code>anotherNode</code>;
   *         otherwise, <code>NULL</code>.
   */
    Edge* findTargetEdge(const Node* target) const {return findEdge(true, target);}
    
  /** 
   * Gets the number of outgoing edges of the node. 
   *
   * @return The number of outgoing edges of the node.
   */
    U_32 getOutDegree() const {return (U_32)getOutEdges().size();}
    
  /** 
   * Gets the number of incoming edges to the node. 
   *
   * @return The number of incoming edges to the node.
   */
    U_32 getInDegree() const {return (U_32)getInEdges().size();}

  /** 
   * Checks whether the node has only one outgoing edge. 
   * 
   * @return <code>TRUE</code> if the node has only one outgoing edge;
   *         otherwise, <code>FALSE</code>.
   */
    bool hasOnlyOneSuccEdge() const {return getOutDegree() == 1;}
    
  /** 
   * Checks whether the node has only one incoming edge. 
   * 
   * @return <code>TRUE</code> if the node has only one incoming edge;
   *         otherwise, <code>FALSE</code>.
   */
    bool hasOnlyOnePredEdge() const {return getInDegree() == 1;}

  /** 
   * Checks whether the node has two or more outgoing edges.
   * 
   * @return <code>TRUE</code> if the node has two or more outgoing edges;
   *         otherwise, <code>FALSE</code>.
   */
    bool hasTwoOrMoreSuccEdges() const {return getOutDegree() >= 2;}

  /** 
   * Checks whether the node has two or more incoming edges.
   * 
   * @return <code>TRUE</code> if the node has two or more incoming edges;
   *         otherwise, <code>FALSE</code>.
   */
    bool hasTwoOrMorePredEdges() const {return getInDegree() >= 2;}


    
    
  /** 
   * Appends the instruction to the node instruction list after the 
   * <code>instBefore</code> instruction.
   * If the instruction is a list, then the whole list is added.
   *
   * @param[in] newInst    - a new instruction/instructions list to add
   * @param[in] instBefore - the location where to insert new instructions. 
   *                          If <code>instBefore</code> is <code>NULL</code>, 
   *                          new instructions are appended to the end of the 
   *                          node list; otherwise, they are inserted right  
   *                          after <code>instBefore</code>.
   */
    void appendInst(CFGInst* newInst, CFGInst* instBefore = NULL);

  /** 
   * Prepends the instruction to the node instruction list before the 
   * <code>instAfter</code> instruction.
   * If the instruction is a list, then the whole list is added.
   *
   * @param[in] newInst   - a new instruction/instructions list to add
   * @param[in] instAfter - the location where to insert new instructions. 
   *                        If <code>instAfter</code> is <code>NULL</code>, new 
   *                        instructions are prepended to the beginning of the 
   *                        node list; otherwise, they are inserted right  
   *                        before <code>instBefore</code>.
   */
    void prependInst(CFGInst* newInst, CFGInst* instAfter = NULL);
    
  /** 
   * Counts the number of instructions in the node. The complexity of 
   * the algorithm is proportional to the number of instructions.
   *
   * @return  The number of instructions in the node. 
   */
    U_32 getInstCount(bool ignoreLabels = true) const;

  /** 
   * Checks whether not a single instruction is in the node.
   *
   * @param[in] ignoreLabels - tells whether to count label instructions
   * 
   * @return  <code>TRUE</code> not a single instruction is in the node.
   */
    bool isEmpty(bool ignoreLabels = true) const {CFGInst* inst = getFirstInst(); return inst == NULL || (ignoreLabels && inst->isLabel() && inst->next() == NULL);}

  /** 
   * Gets the first instruction in the node.
   *
   * @return  The first instruction in the node;
   *          <code>NULL</code> if the node is empty.
   */
    CFGInst* getFirstInst() const {return instsHead->getNext() != instsHead ? (CFGInst*)instsHead->getNext() : NULL;}

  /** 
   * Gets the last instruction in the node.
   *
   * @return  The last instruction in the node;
   *          <code>NULL</code> if the node is empty.
   */
    CFGInst* getLastInst() const {return instsHead->getPrev() != instsHead ? (CFGInst*)instsHead->getPrev() : NULL;}

  /** 
   * Gets the execution count of the node. Execution count of the node is 
   * a number of times the code of this node was executed at run time. 
   * This number can be the result of profiling of static estimation.
   * 
   * @return  The number of times the code of the given node was executed.
   */
    double getExecCount() const {return execCount;}

  /** 
   * Sets the execution count for the node.
   *
   * @param[in] val - a new execution count value
   */
    void setExecCount(double val) {execCount = val;}


  /**
   * Gets the current node traversal number used by internal and external 
   * algorithms to mark node as visited.
   *
   * @return The traversal number for the node.
   */
    U_32 getTraversalNum() const {return traversalNumber;}
    
  /** 
   * Sets a new traversal number of the node, usually 
   * <code>oldTraversalNumber</code> + 1.
   *
   * @param[in] num - a new traversal number
   */
    void setTraversalNum(U_32 num) {traversalNumber = num;}

  /** 
   * Gets the second instruction in the node.
   *
   * @return  The second instruction in the node;
   *          <code>NULL</code> if the node is empty or has only one 
   *          instruction. 
   */
    CFGInst* getSecondInst() const {return isEmpty() ? NULL : getFirstInst()->next();}
    
  /** 
   * Gets the label instruction. The label instruction is always the first 
   * instruction in the node. Check additionally whether the first instruction 
   * is a label one.
   * 
   * @return  The first instruction or <code>NULL</code>. 
   *          Checks whether the first instruction is a label one.
   */
    CFGInst* getLabelInst() const {CFGInst* first = getFirstInst(); assert(first==NULL || first->isLabel()); return first;}


    /** 
    * Gets bytecode offset of the first inst with bc-mapping in the node
    * 
    * @return  bytecode offset of the first inst with bc-mapping in the node
    */
    uint16 getNodeStartBCOffset() const;

    /** 
    * Gets bytecode offset of the last inst with bc-mapping in the node
    * 
    * @return  bytecode offset of the last inst with bc-mapping in the node
    */
    uint16 getNodeEndBCOffset() const;


protected:
  /** 
   * The constructor of Node.
   * Initializes all instance fields with default values.
   * 
   * @param[in] mm   - the memory manager to use
   * @param[in] kind - the node kind
   */
    Node(MemoryManager& mm, Kind kind);
    
  /**
   * Sets the node ID.
   * 
   * @param[in] _id - a new node ID 
   */
    void setId(U_32 _id) {id = _id;}


  /**
   * Inserts the <code>newInst</code> instruction right after 
   * the <code>prev</code> instruction.
   * Assigns the node to the <code>newInst</code>.
   */
    void insertInst(CFGInst* prev, CFGInst* newInst);

    /// The unique ID of the node in CFG.
    U_32 id;
    
  /** 
   * The depth-first number of the node in CFG. 
   * It is updated every time CFG is ordered.
   */
    U_32 dfNumber;

  /** 
   * The pre-number of the node in CFG. 
   * It is updated every time CFG is ordered.
   */
    U_32 preNumber;
    
  /** 
   * The post-number of the node in CFG. 
   * It is updated every time CFG is ordered.
   */
    U_32 postNumber;

  /** 
   * The number of times the node was traversed by the 
   * ordering algorithms.
   * It is updated every time CFG is ordered.
   */
    U_32 traversalNumber;

    /// The type of the node.
    Kind   kind;

  /** 
   * The collection of all incoming edges to the node. 
   * The order of edges in this collection if not specified.
   */
    Edges inEdges;

  /** 
   * The collection of all outgoing edges from the node. 
   * The order of edges in this collection if not specified.
   */
    Edges outEdges;
    
   /** 
    * The stub for the node instructions list.
    * The first user's instruction in the node is the first instruction
    * after the <code>instHead</code> stub.
    * The last user's instruction in the node is the first instruction
    * before the <code>instHead</code> stub.
    */
    CFGInst* instsHead;

    /// Profiling information. The number of times this node was executed.
    double execCount;
};

  /** 
   * The factory class for nodes and edges.
   * Creates nodes and edges at ControlFlowGraph requests.
   * The default implementation of the given class creates default nodes
   * and edges of the <code>Node</code> and <code>Edge</code> classes. 
   * To create custom <code>Node</code> and <code>Edge</code> subclasses,overload
   * the methods of the given factory class.
   */
class ControlFlowGraphFactory {
public:
    ///The default constructor for the ControlFlowGraphFactory class. Does nothing.
    ControlFlowGraphFactory(){}
    ///The default destructor for the ControlFlowGraphFactory class. Does nothing.
    virtual ~ControlFlowGraphFactory(){}
    
  /** 
   * Creates a new Edge of the specified kind using the memory manager as the 
   * allocator.
   * 
   * @param[in] mm   - the memory manager for the newly created node
   * @param[in] kind - the kind of the newly created node
   * 
   * @return A new specified node allocated on the memory manager.
   */
    virtual Node* createNode(MemoryManager& mm, Node::Kind kind);

  /** 
   * Creates a new Edge of the specified kind to connect nodes of  
   * <code>srcKind</code> and <code>dstKind</code> kinds.
   *
   * @param[in] mm      - the memory manager for the newly created edge
   * @param[in] srcKind - the kind of <code>Source</code> node for the edge
   * @param[in] dstKind - the kind of source <code>Target</code> for the edge
   *  
   * @return A new edge allocated on the memory manager to be connected with
   *         nodes of <code>srcKind</code> and <code>dstKind</code> kinds.
   */
    virtual Edge* createEdge(MemoryManager& mm, Node::Kind srcKind, Node::Kind dstKind);
};


  /** 
   * The base class for CFG.
   * The container class for nodes, edges, global states of nodes and edges and 
   * related algorithms.
   */
class ControlFlowGraph {
public:

  /** 
   * Creates new CFG.
   *
   * @param[in] mm      - the memory manager for CFG nodes, edges and internal data
   * @param[in] factory - the factory to create new nodes and edges
   *
   * If no factory is specified, the default factory implementation is used.
   */
    ControlFlowGraph(MemoryManager& mm, ControlFlowGraphFactory* factory = NULL);
    virtual ~ControlFlowGraph(){};
    
  /** 
   * Gets a collection of all nodes in the graph.  
   * Certain nodes in the collection can be unreachable.
   * The order of nodes is arbitrary.
   *
   * @return A collection of nodes in the control flow graph.
   *
   * @note All iterators to the collection become invalid when new nodes 
   *       are added or old ones deleted.
   */
    const Nodes& getNodes() const {return nodes;}
    
  /**  
   * Gets the unique <code>Entry</code> node. The given node dominates all other nodes in 
   * the graph.
   * The <code>Entry</code> node has the <code>Node::Kind_BlockNode</code> kind.
   * 
   * @return  The <code>Entry</code> node.
   */
    Node* getEntryNode() const {return entryNode;}

  /** 
   * Sets the <code>Entry</code> node. The given node dominates all other nodes 
   * in the graph.
   * The <code>Entry</code> node must be a block one.
   * 
   * @param[in] e - a new <code>Entry</code> node
   */
    void setEntryNode(Node* e) {assert(e!=NULL); entryNode = e; lastModifiedTraversalNumber = traversalNumber;}
    
  /**  
   * Gets the unique <code>Exit</code> node. The given node postdominates all other nodes
   * in the graph except child nodes of infinite loops.
   * The <code>Exit</code> node has the Node::Kind_ExitNode kind.
   * 
   * @return  The <code>Exit</code> node.
   */
    Node* getExitNode() const {return exitNode;}

  /** 
   * Sets the <code>Exit</code> node. The given node postdominates all other
   * nodes in the graph except child nodes of infinite loops.
   * The <code>Exit</code> node has the Node::Kind_ExitNode kind.
   *
   * @return  The <code>Exit</code> node.
   */
    void setExitNode(Node* e) {assert(e!=NULL); exitNode = e; lastModifiedTraversalNumber = traversalNumber;}

  /** 
   * Gets the unique block node that postdominates all nodes
   * in all paths that are non-dispatch exits from the method.
   *
   * @return The <code>Return</code> node of the graph.
   */
    Node* getReturnNode() const {return returnNode; }
    
  /**  
   * Sets the <code>Return</code> node for the graph. 
   * The <code>Return</code> node is a block node that postdominates all
   * nodes in all paths that are non-dispatch exits from the method.
   *
   * @return The <code>Return</code> node of the graph.
   */
    void setReturnNode(Node* node) {assert(returnNode==NULL); assert(node->isBlockNode()); returnNode = node;}
        
  /**  
   * Gets the unique dispatch node that postdominates all nodes
   * in all paths that are dispatch exits from the method.
   *
   * @return The <code>Unwind</code> node of the graph.
   */
    Node* getUnwindNode() const {return unwindNode;}

  /**
   * Sets the <code>Unwind</code> node for the graph. 
   * The <code>Unwind</code> node is a dispatch node that postdominates all 
   * nodes in all paths that are dispatch exits from the method.
   *
   * @return The <code>Unwind</code> node of the graph.
   */
    void setUnwindNode(Node* node) {assert(node->isDispatchNode()); unwindNode = node;}

  /** 
   * Gets the maximum node ID. 
   * The given ID is incremented any time the node is added to the graph.
   * 
   * @return The maximum node ID.
   */
    U_32 getMaxNodeId() const {return nodeIDGenerator;}
    
  /** 
   * Creates a new specified node. Add instruction to the node.
   * 
   * @param[in] kind - the kind of the node
   * @param[in] inst - the instruction to add to the node instructions list
   * 
   * @return A newly created node.
   */
    Node* createNode(Node::Kind kind, CFGInst* inst = NULL);

  /** 
   * Creates a new <code>Block</code> node and adds the instruction 
   * to it.
   * 
   * @param[in] inst - the instruction to add to the node instructions list
   * 
   * @return A newly created node.
   */
    Node* createBlockNode(CFGInst* inst = NULL) {return createNode(Node::Kind_Block, inst);}
    
  /** 
   * Creates a new <code>Dispatch</code> node and adds the instruction 
   * to it.
   * 
   * @param[in] inst - the instruction to add to the node instructions list
   * 
   * @return A newly created node.
   */
    Node* createDispatchNode(CFGInst* inst = NULL) {return createNode(Node::Kind_Dispatch, inst);}

  /** 
   * Creates a new <code>Exit</code> node and adds the instruction 
   * to it.
   * 
   * @param[in] inst - the instruction to add to the node instructions list
   *
   * @return A newly created node.
   */
    Node* createExitNode(CFGInst* inst = NULL) {return createNode(Node::Kind_Exit, inst);}
        
  /** 
   * Removes the node and all its incoming and outgoing edges from the graph.
   * 
   * @param[in] node - a node to remove 
   */
    void removeNode(Node* node);

  /** 
   * Gets the number of nodes in the graph that are reachable from the <code>Entry</code> node.
   * Orders nodes and updates traversal and ordering numbers of the graph and all 
   * nodes if the graph has been modified from the last ordering.
   *
   * @return The number of reachable nodes in the graph.
   */
    U_32 getNodeCount() { if(!hasValidOrdering()) orderNodes(); return nodeCount; }
    
  /** 
   * Gets the cached postorder collection of nodes in the graph.
   * 
   * @return The collection of nodes ordered in postorder.
   */
    const Nodes& getNodesPostOrder() {if (!hasValidOrdering()) orderNodes(); return postOrderCache;}
    
  /** 
   * Copies pointers to all nodes of the graph to the container.
   * 
   * @param[in] container - a container to put node pointers to
   *
   * The container must provide the <code>Insert</code>(<code>iterator 
   * insertLocation</code>, <code>iterator start</code>, <code>iterator 
   * end</code>) method.
   */
    template <class Container>
        void getNodes(Container& container) {
            container.insert(container.begin(), nodes.begin(), nodes.end());
        }

  /** 
   * Copies post-ordered nodes sequence to the container.
   * If <code>isForwarisForward</code> is <code>TRUE</code>, the method starts 
   * ordering the DFS algorithm from the <code>Entry</code> node.  
   * If <code>isForwarisForward</code> is <code>FALSE</code>, the method starts 
   * ordering the DFS algorithm from the <code>Exit</code> node.
   *
   * @param[in] container - the container to add nodes
   * @param[in] isForward - whether to start DFS ordering from the <code>Entry</code> 
   *                       (<code>TRUE</code>) or <code>Exit</code> 
   *                       (<code>FALSE</code>) node
   * 
   * @note The container class must provide the <code>push_back(Node*)</code> 
   * method.
   */
    template <class Container>
    void getNodesPostOrder(Container& container, bool isForward=true) {
        runDFS((Container*) NULL, &container,  isForward);
    }


  /** 
   * Copies pre-ordered nodes sequence to the container.
   * If <code>isForwarisForward</code> is <code>TRUE</code>, the method starts 
   * ordering the DFS algorithm from the <code>Entry</code> node.
   * If <code>isForwarisForward</code> is <code>FALSE</code>, the method starts  
   * ordering the DFS algorithm from the <code>Exit</code> node.
   * 
   * @param[in] container - the container to add nodes
   * @param[in] isForward - whether to start DFS ordering from the <code>Entry</code> 
   *                       (<code>TRUE</code>) or <code>Exit</code> 
   *                       (<code>FALSE</code>) node
   *  @note The container class must provide the <code>push_back(Node*)</code> 
   *        method.
   */
    template <class Container>
    void getNodesPreOrder(Container& container, bool isForward=true) {
        runDFS(&container, (Container*) NULL, isForward);
    }

    
  /** 
   * Orders the nodes in the graph. 
   * If <code>isForward</code> is <code>TRUE</code>, updates the  
   * internal CFG collection of nodes and node df-numbers.
   * Affects nodes and graph traversal and ordering numbers.
   *  
   * @param[in] isForward - whether to start DFS ordering from the <code>Entry</code> 
   *                        (<code>TRUE</code>) or <code>Exit</code> 
   *                        (<code>FALSE</code>) node
   * 
   */
    void orderNodes(bool isForward=true) {
        runDFS((Nodes*) NULL, (Nodes*) NULL, isForward);
    }

  /** 
   * Creates a new edge from the <code>Source</code> node to the 
   * <code>Target</code> one. 
   * 
   * @param[in] source   - the <code>Source</code> or <code>Tail</code> 
   *                       node for the edge
   * @param[in] target   - the <code>Target</code> or <code>Head</code> 
   *                       node for the edge
   * @param[in] edgeProb - the probability of the edge
   * 
   * @return The newly created edge.
   */
    Edge* addEdge(Node* source, Node* target, double edgeProb = 1.0);


  /** 
   * Gets the maximum edge ID. 
   * The given ID is incremented every time the edge is added to the graph.
   * 
   * @return The maximum edge ID.
   */
    U_32 getMaxEdgeId() const {return edgeIDGenerator;}

  /** 
   * Removes the edge from CFG. Updates <code>Source</code> and 
   * <code>Target</code> nodes. 
   * Does not affect other edge probabilities.
   * 
   * @param[in] edge - the edge to remove
   */
    void  removeEdge(Edge* edge);

  /** 
   * Removes the edge connecting <code>Source</code> and 
   * <code>Target</code> nodes. 
   * Updates <code>Source</code> and <code>Target</code> nodes. Does not  
   * affect other edge probabilities.
   * 
   * @param[in] source - the <code>Source</code> node for the edge
   * @param[in] target - the <code>Target</code> node for the edge
   */
    void  removeEdge(Node* source, Node* target) {removeEdge(source->findTargetEdge(target));}
    
  /** 
   * Removes the edge, creates a new one and connects it to 
   * the <code>newTarget</code> node.
   *
   * @param[in] edge        - the edge to change the target
   * @param[in] newTarget   - a new <code>Target</code> node
   * @param[in] keepOldEdge - modify old or create a new edge
   * 
   * @return The edge connecting the <code>Source</code> node of the  
   *         edge and the </code>newTarget</code> node.
   *
   * @note The removal of the old edge is needed
   *       while inlining CFG: edge IDs must be renewed.
   */
    Edge* replaceEdgeTarget(Edge* edge, Node *newTarget, bool keepOldEdge = false);

  /** 
   * Checks if CFG is annotated with the edge profile information.
   * 
   * @return <code>TRUE</code> if CFG was annotated with the edge profile.
   */
    bool hasEdgeProfile() const {return annotatedWithEdgeProfile;}

  /** 
   * Sets if CFG is annotated with the edge profile.
   * 
   * @param[in] val - marks CFG as annotated if <code>val</code> is <code>TRUE</code>, 
   *                  CFG is not annotated if <code>val</code> is <code>FALSE</code>.
   */
    void setEdgeProfile(bool val) {annotatedWithEdgeProfile = val;}


  /** 
   * Checks if the edge profile is consistent. 
   * Checks only reachable nodes and recalculates postorder cache if needed.
   * 
   * @param[in] checkEdgeProbs  - enables edge probs consistency check. If for every 
   *                              node the sum of the all outgoing edge probs 
   *                              is equal to 1.0, the edge probs data is consistent.
   * 
   * @param[in] checkExecCounts - enables exec counts consistency check. If for every 
   *                              node the sum of exec counts of incoming edges is 
   *                              equal to the node exec count, the exec count data  
   *                              is consistent.
   * @param[in] doAssert        - asserts if the consistency error is found. Useful 
   *                              to position the debugger to the error.
   */
    bool isEdgeProfileConsistent(bool checkEdgeProbs = true, bool checkExecCounts = true, bool doAssert=false);
    
  /** 
   * Counts the execution counts of nodes using edge probabilities and execution 
   * count of the <code>Entry</code> node.
   */
    void smoothEdgeProfile();


  /** 
   * Gets the traversal number.
   * The traversal number is analogous to a monotonically increasing timestamp.
   * It is updated anytime an ordering traversal is performed on CFG.
   * If a modification was performed after an ordering, the ordering is invalid.
   *
   * @return The traversal number
   */
    U_32 getTraversalNum() const {return traversalNumber;}
    
  /** 
   * Sets the traversal number.
   * The traversal number is analogous to a monotonically increasing timestamp.
   * It is updated anytime an ordering traversal is performed on CFG.
   * If a modification was performed after an ordering, the ordering is invalid.
   *
   * @param[in] newTraversalNum - a new traversal number
   */

    void setTraversalNum(U_32 newTraversalNum) {traversalNumber = newTraversalNum;}

  /** 
   * The modification traversal number is the traversal number of the
   * last add/remove of a node/edge in the graph.
   * 
   * @return The modification traversal number.
   */
    U_32 getModificationTraversalNum() const { return lastModifiedTraversalNumber; }

  /** 
   * The edge removal traversal number is the modification traversal number of 
   * the last remove of an edge in the graph.
   * 
   * @return The edge removal traversal number.
   */
    U_32 getEdgeRemovalTraversalNum() const { return lastEdgeRemovalTraversalNumber; }

  /** 
   * The ordering traversal number is the traversal number after the last depth
   * first ordering. Node pre/post numbers are valid, if
   * getOrderingTraversalNum() is greater than getModificationTraversalNum().
   *
   * @return The ordering traversal number.
   */
    U_32 getOrderingTraversalNum() const { return lastOrderingTraversalNumber; }

  /** 
   * Checks if CFG nodes have valid ordering, that is there was no modification
   * in graph from the time of the last ordering.
   *
   * @return <code>TRUE</code> if ordering is valid; otherwise, <code>FALSE</code>.
   */
    bool hasValidOrdering() const { return getOrderingTraversalNum() > getModificationTraversalNum(); }

  /** 
   * Gets the memory manager used by CFG to allocate nodes and edges.
   * 
   * @return The memory manager for given CFG.
   */
    MemoryManager& getMemoryManager() const {return mm;}

  /** 
   * Splits the <code>Return</code> node in CFG, that is creates a new 
   * <code>Block</code> node before the <code>Return</code> node and retarget
   * all incoming edges from the <code>Return</code> node to a new 
   * <code>Block</code> node.
   * After this method call the <code>Return</code> node has only one incoming 
   * edge.
   *
   * @param[in] headerInst  - the instruction to add to a new node
   * 
   * @return The newly created node.
   */
    Node* splitReturnNode(CFGInst* headerInst=NULL) {return splitNode(getReturnNode(), false, headerInst);}

  /** 
   * Splits a node at a particular instruction, leaving the instruction in the
   * same node and moving all instructions before or after the instruction 
   * to a newly created node.
   *
   * @param[in] inst         - the place where to split the node
   * @param[in] splitAfter   - indicates whether to split should appear after 
   *                           <code>TRUE</code> or before
   *                           <code>FLASE</code> instruction
   * @param[in] keepDispatch - indicates if both nodes must have the same dispatch 
   *                           as the <code>Initial</code> node had
   *                           If <code>FLASE</code> the <code>Dispatch</code> node 
   *                           is moved to the node with a higher post-num.
   * 
   * @return The newly created node.
   */
    Node* splitNodeAtInstruction(CFGInst *inst, bool splitAfter, bool keepDispatch, CFGInst* headerInst);

  /** 
   * Creates a new node and targets the edge to it. The old edge target is
   * connected with a new node by new edge after the given method call.
   *
   * @param[in] edge - the edge to splice
   * @param[in] inst - the instruction to add to a new node
   * @param[in] keepOldEdge  - keep old edge and use it to connect source and new nodes
   *
   * @return The newly created node.
   */
    Node* spliceBlockOnEdge(Edge* edge, CFGInst* inst = NULL, bool keepOldEdge=false);

  /** 
   * Inlines <code>inlineFG</code> into this CFG after <code>instAfter</code>, 
   * splits the <code>Instruction</code> node if needed.
   * Moves all nodes from <code>inlineFG</code> except the <code>Exit</code> 
   * node to given CFG.
   * Relies on valid <code>Return</code> and <code>Unwind</code> nodes of 
   * <code>inlinedFG</code>.
   *
   * @param[in] instAfter    - the place to inline. <code>inlinedFG</code> is 
   *                           inlined after this instruction
   * @param[in] inlineFG     - the flow graph to inline. Must have valid unwind 
   *                           and return nodes
   */
    void spliceFlowGraphInline(CFGInst* instAfter, ControlFlowGraph& inlineFG);
    
  /** 
   * Inlines <code>inlineFG</code> info given CFG retargeting the edge to the 
   * <code>inlinedFG</code>'s <code>Entry</code> node.
   * Moves all nodes from <code>inlinedFG</code> except the <code>Exit</code> 
   * node to <code>This</code> CFG.
   * Relies on valid <code>Return</code> and <code>Unwind</code> nodes of the 
   * <code>inlinedFG</code>.
   *
   * @param[in] edge     - the edge to splice the inlined flow graph on.
   * @param[in] inlineFG - the flow graph to inline. Must have valid 
   *                       <code>Return</code> and <code>Unwind</code> nodes.
   */
    void spliceFlowGraphInline(Edge* edge, ControlFlowGraph& inlineFG);


  /** 
   * Removes all critical edges from the graph.
   * Does not split exception edges unless the parameter is <code>TRUE</code>.
   *
   * @param[in] includeExceptionEdges - process exception edges if <code>TRUE</code>
   * @param[in] newNodes              - all newly created nodes
   */
    void splitCriticalEdges(bool includeExceptionEdges, Nodes* newNodes = NULL);

  /** 
   * Moves instructions from one node to another.
   * 
   * @param[in] fromNode - the node to move instructions from
   * @param[in] toNode   - the node to move instructions to
   * @param[in] prepend  - prepends instructions to a new node if <code>TRUE</code>;
   *                       appends instructions if <code>FALSE</code>
   */
    void moveInstructions(Node* fromNode, Node* toNode, bool prepend);

    
  /**  
   * Combines nodes from CFG that can be folded together.
   *
   * @param[in] skipEntry       - does not process the <code>Entry</code> node
   * @param[in] mergeByDispatch - allows merging of nodes with the same dispatch 
   *                              edge target
   */
    void mergeAdjacentNodes(bool skipEntry = false, bool mergeByDispatch= false);

  /**  
   * Combines blocks.
   *
   * @param[in] first     - the first block to merge
   * @param[in] second    - the second block to merger
   * @param[in] keepFirst - tells whether to keep the first or second block
   */
    void mergeBlocks(Node* first, Node* second, bool keepFirst=true);

  /** 
   * Removes all empty nodes from CFG.
   * 
   * @param[in] preserveCriticalEdges  - informs if to preserve critical edges
   * @param[in] removeEmptyCatchBlocks - allows the removal of empty catch blocks 
   *                                     if <code>TRUE</code>
   */
    void purgeEmptyNodes(bool preserveCriticalEdges = false, bool removeEmptyCatchBlocks = false);

  /** 
   * Removes all unreachable nodes from CFG. 
   * If a path connects the node with the <code>Entry</code> node, the node is 
   * reachable.
   */
    void purgeUnreachableNodes() { purgeUnreachableNodes((Nodes*) NULL); }

  /**  
   * Removes all unreachable nodes from CFG and adds them to the container.
   * 
   * @param[in] container - the collection supports the <code>push_back</code>  
   *                        method to store pointers to all removed nodes 
   *                    
   */
    template <class Container> 
        void purgeUnreachableNodes(Container& container) { purgeUnreachableNodes(&container);}


  /**
   * Gets the dominator tree. Updates the tree, if it is in the up-to-date state.
   * 
   * @return The dominator tree; <code>NULL</code> if none dominator tree is 
   *         assigned to the given graph.
   */
    DominatorTree* getDominatorTree() const {return domTree;}

  /** 
   * Gets the post-dominator tree. Updates the tree, if it is in the up-to-date 
   * state.
   * 
   * @return The post-dominator tree; <code>NULL</code> if none post-dominator
   *         tree is assigned to the given graph.
   */
    DominatorTree* getPostDominatorTree() const {return postDomTree;}

  /** 
   * Gets the loop tree. Updates the tree, if it is in the up-to-date state.
   * 
   * @return The loop tree; <code>NULL</code> if none loop tree is
   *         assigned to the given graph.
   */
    LoopTree* getLoopTree() const {return loopTree;}

  /** 
   * Assigns the dominator tree to the graph.
   *
   * param dom - the dominator tree assigned to the graph
   */
    void setDominatorTree(DominatorTree* dom) {domTree = dom;}

  /** 
   * Assigns the post-dominator tree to the graph.
   * 
   * @param[in] dom - the post-dominator tree assigned to the graph
   */
    void setPostDominatorTree(DominatorTree* dom) {domTree = dom;}

  /** 
   * Assigns the loop tree to the graph.
   * 
   * param dom - the loop tree assigned to the graph
   */
    void setLoopTree(LoopTree* lt) {loopTree= lt;}
   
protected:
  /** 
   * Adds the node to the graph nodes list. 
   * Assigns new ID to the node, increments the graph modification count.
   */
    void addNode(Node* node);

  /** 
   * Adds the edge to the graph and connects <code>Source</code> and 
   * <code>Target</code> nodes with this edge.
   * Assigns a new edge ID to the edge, increments the graph modification 
   * count.
   */
    void addEdge(Node* source, Node* target, Edge* edge, double edgeProb);
    
  /** 
   * Removes the node from the graph.
   * 
   * @param[in] i     - the location of the node in the graph node collection
   * @param[in] erase - informs whether to erase or just <code>NULL</code> the 
   *                    pointer to the node in the collection
   */
    void removeNode(Nodes::iterator i, bool erase);

  /** 
   * Sets a new ID to the edge.
   */
    void setNewEdgeId(Edge* edge) { edge->setId(edgeIDGenerator++); }

  /** 
   * Moves all incoming edges from <code>oldNode</code> to 
   * <code>newNode</code>. 
   * Increments modification traversal number of the graph.
   */
    void moveInEdges(Node* oldNode, Node* newNode);

  /** 
   * Moves all outgoing edges from <code>oldNode</code> to 
   * <code>newNode</code>. 
   * Increments modification traversal number of the graph.
   */
    void moveOutEdges(Node* oldNode, Node* newNode);
    
  /** 
   * Removes duplicate incoming edges.
   */
    void resetInEdges(Node* node);
    
  /** 
   * Removes duplicate outgoing edges.
   */
    void resetOutEdges(Node* node);

  /** 
   * Splits the node: adds a new block before or after the node.
   * 
   * @warning Low-level helper method.
   * @warning If <code>newBlockAtEnd</code> is <code>TRUE</code>, the given method does not 
   *          keep dispatch on the original block.
   * @warning Does not move instructions.
   */
    Node* splitNode(Node* node, bool newBlockAtEnd, CFGInst* newBlockInst);

  /// Splits the edge and adds <code>newBlockInst</code> to a new node.
   
    Node* splitEdge(Edge* edge, CFGInst* newBlockInst, bool keepOldEdge);
    
  /// Checks whether the <code>Source</code> node can be merged with the <code>Target</code> node.
   
    bool isBlockMergeAllowed(Node* source, Node* target, bool allowMergeDispatch) const;

  /// Helper for public <code>getNodesPre/PostOrder</code> methods above.
   
    template <class Container>
    void runDFS(Container* preOrderContainer, Container* postOrderContainer, bool isForward) {
        Node* startNode;
        traversalNumber++;
        if (isForward) {
            lastOrderingTraversalNumber = traversalNumber;
            postOrderCache.clear();
            if (entryNode==NULL) {
                return;
            }
            startNode = entryNode;
        } else {
            if (exitNode==NULL) {
                return;
            }
            startNode = exitNode;
        }
        currentPreNumber = currentPostNumber = 0;
        getNodesDFS(preOrderContainer, postOrderContainer, startNode, isForward);
        assert(currentPreNumber == currentPostNumber);
        if (isForward) {
            nodeCount = currentPreNumber;
        } else {
            // getNodesDFS changes traversalNum for nodes. But traversalNum assumes direct traversal.
            // so drop the ordering number here to force CFG recompute ordering before using node->traversalNum values again
            lastOrderingTraversalNumber = lastModifiedTraversalNumber-1;
        }
    }
    
  /// Helper for public <code>getNodesPre/PostOrder</code> methods above. Warn: increment cfg traversal num before use.
   
    template <class Container>
    void getNodesDFS(Container* preOrderContainer, Container* postOrderContainer, Node* node, bool isForward=true) {
        U_32 marked = traversalNumber;
        node->setTraversalNum(marked);
        if(isForward) {
            node->dfNumber = currentPreNumber;
        }
        node->preNumber = currentPreNumber++;
        if(preOrderContainer != NULL) {
            preOrderContainer->push_back(node);
        }
        Edges::const_iterator i = node->getEdges(isForward).begin(), iend = node->getEdges(isForward).end();
        for(; i != iend; i++) {
            Edge* edge = *i;
            Node* succ = edge->getNode(isForward);
            if(succ->getTraversalNum() < marked) {
                getNodesDFS(preOrderContainer, postOrderContainer, succ, isForward);
            }
        }
        node->postNumber = currentPostNumber++;
        if (postOrderContainer != NULL) {
            postOrderContainer->push_back(node);
        }
        if (isForward) {
            postOrderCache.push_back(node);
        }
    }

  /// Removes all nodes unreachable from the entry. 
   
    template <class Container>
        void purgeUnreachableNodes(Container* container) {
            if(!hasValidOrdering()) {
                orderNodes();
            }

            Nodes::iterator iter = nodes.begin(), end = nodes.end();
            for(; iter != end;) {
                Nodes::iterator current = iter;
                Node* node = *iter;
                ++iter;
                if(node->traversalNumber < traversalNumber) {
                    removeNode(current, false);
                    if(container != NULL) {
                        container->push_back(node);
                    }
                }
            }
            nodes.erase(std::remove(nodes.begin(), nodes.end(), (Node*)NULL), nodes.end());
        }

    /// The memory manager used by <code>ControlFlowGraph</code> to allocate its data.
    MemoryManager& mm;
    /// The factory used by <code>ControlFlowGraph</code> to create nodes and edges.
    ControlFlowGraphFactory* factory;
    /// The unique <code>Entry</code> node in <code>ControlFlowGraph</code>.
    Node* entryNode;
    /// The unique <code>Return</code> node in <code>ControlFlowGraph</code>.
    Node* returnNode;
    /// The unique <code>Exit</code> node in <code>ControlFlowGraph</code>.
    Node* exitNode;
    /// The unique <code>Unwind</code> node in <code>ControlFlowGraph</code>.
    Node* unwindNode;
    
    /// The collection of nodes. The order of nodes in the collection is not specified.
    Nodes nodes;
    /// The collection of postordered nodes, which is updated every time the graph is ordered.
    Nodes postOrderCache;

    /// The ID generator for nodes, which is incremented every time a new node is added to the graph.
    U_32 nodeIDGenerator;
    /// The ID generator for edges, which is incremented every time a new edge is added to the graph.
    U_32 edgeIDGenerator;
    /// The number of reachable nodes in the graph, which is updated every time the graph is ordered.
    U_32 nodeCount;
    /// The last graph traversal number used to track graph modifications.
    U_32 traversalNumber;
    /// The given field is assigned to the <code>traversalNumber</code> value every time the graph is modified.
    U_32 lastModifiedTraversalNumber;
    /// The given field is assigned to the <code>traversalNumber</code> value every time the graph is ordered.
    U_32 lastOrderingTraversalNumber;
    /// The given field is assigned to the <code>traversalNumber</code> value every time any edge is removed from the graph.
    U_32 lastEdgeRemovalTraversalNumber;
    /// The given field is assigned to the <code>traversalNumber</code> value every time the profile information is recalculated.
    U_32 lastProfileUpdateTraversalNumber;

    /// The temporary field used by ordering algorithms.
    U_32 currentPreNumber;
    /// The temporary field used by ordering algorithms.
    U_32 currentPostNumber;

    /// Tells whether the graph is annotated with the edge profile.
    bool annotatedWithEdgeProfile;

    /// The dominator tree for the graph. <code>NULL</code> if no dominator tree was set.
    DominatorTree* domTree;
    /// The post-dominator tree for the graph. <code>NULL</code> if no post-dominator tree was set.
    DominatorTree* postDomTree;
    /// The loop tree for the graph. <code>NULL</code> if no loop tree was set.
    LoopTree* loopTree;
};


} //namespace Jitrino 

#endif // _CONTROLFLOWGRAPH_
