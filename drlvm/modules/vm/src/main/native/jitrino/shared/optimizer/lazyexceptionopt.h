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
 * @author Intel, Natalya V. Golovleva
 *
 */

#ifndef _LAZYEXCEPTION_H_
#define _LAZYEXCEPTION_H_

#include "open/types.h"
#include "optpass.h"
#include "Inst.h"
#include "BitSet.h"
#include "VMInterface.h"
#include "irmanager.h"

namespace Jitrino {


/**
 * The lazy exception optimization pass class.
 */
class LazyExceptionOpt {
public:
/**
 * Creates lazy exception optimization pass class instance.
 * @param ir_manager - optimized method IR manager
 */
    LazyExceptionOpt(IRManager &ir_manager);

/**
 * Executes lazy exception optimization pass.
 */
    void doLazyExceptionOpt();

private:
/**
 * Prints instruction and its source instructions.
 * @param os - output stream
 * @param inst - instruction to print
 * @param txt - string to print before instruction
 */
    void printInst1(::std::ostream& os, Inst* inst, std::string txt);

/**
 * Adds information to optCandidates list for specified exception object.
 * @param id - an exception object operand Id
 * @param inst - call, or throw instructions operating with this exception object
 * @return <code>true</code> if an information is added; 
 *         <code>false<code> if an exception object cannot be optimized.
 */
    bool addOptCandidates(U_32 id, Inst* inst);

/**
 * Checks if there is a side effect between throw_inst and init_inst instructions.
 * @param inst - checked instruction
 * @return <code>true</code> if an instruction has side effect;
 *         <code>false<code> if an instruction has no side effect.
 */
    bool instHasSideEffect(Inst* inst);

/**
 * Checks that exception edges are equal for newobj instruction node and
 * throw instruction node.
 * @param bs - bit set of operands that may be optimized
 */
    void fixOptCandidates(BitSet* bs);

/**
 * Prints information about optimization candidates.
 * @param os - output stream
 */
    void printOptCandidates(::std::ostream& os);

/**
 * Checks a callee method side effect.
 * @param inst - method call instruction
 * @return <code>true</code> if method has side effect;
 *         <code>false<code> if method has no side effect.
 */
    bool methodCallHasSideEffect(Inst* inst); 

  /**
   * Removes node from compiled method flow graph.
   * @param node - removed node
   */
    void removeNode(Node* node);

/**
 * Removes specified instructions if they have the same exception node.
 * @param oinst - exception creating instruction
 * @param iinst - constructor call instruction
 * @return <code>true</code> if instruction were removed;
 *         <code>false<code> otherwise.
 */
    bool removeInsts(Inst* oinst,Inst* iinst);

/**
 * Checks if Op_TauStInd (stind) instruction has a side effect.
 * @param inst - checked instruction
 * @return <code>true</code> if an instruction has side effect;
 *         <code>false<code> if an instruction has no side effect.
 */
    bool fieldUsageHasSideEffect(Inst* inst);

/**
 * Checks that exception edges are equal for newobj instruction node and
 * throw instruction node.
 * @param oi - newobj instruction
 * @param ti - throw instruction
 * @return <code>true</code> if exception edges are equal;
 *         <code>false<code> otherwise.
 */
    bool isEqualExceptionNodes(Inst* oi, Inst* ti);

/**
 * Checks if there is a side effect between throw_inst and init_inst instructions.
 * @param throw_inst - the exception object throw instruction
 * @param init_inst - the exception object constructor call instruction
 * @return <code>true</code> if there is side effect;
 *         <code>false<code> if there is no side effect.
 */
    bool checkInSideEff(Inst* throw_inst, Inst* init_inst);

/**
 * Checks if a callee method agrument may be null.
 * @param call_inst - method call instruction
 * @param arg_n - callee method argument number
 * @return <code>true</code> if a callee method argument may be null
 *         <code>false<code> if a callee method argument is not null
 */
    bool mayBeNullArg(Inst* call_inst, U_32 arg_n);

private:
    /// IR manager recieved by lazyexc optpass.
    IRManager     &irManager;
    /// The memory manager used by lazyexc optpass to allocate its data.
    MemoryManager leMemManager;
    /// The compilation interface of an optimized method.
    CompilationInterface &compInterface;
    /// Sign that an optimized method is an exception initializer.
    bool isExceptionInit;
    /// Sign that an optimized method has check null instruction for a throwable type argument
    bool isArgCheckNull;
#ifdef _DEBUG
    MethodDesc* mtdDesc;
#endif
    typedef StlList<Inst*> ThrowInsts; 
    /// Information about throwable object used in an optimized method.
    struct OptCandidate {
        U_32 opndId;
        Inst* objInst;
        Inst* initInst;
        ThrowInsts* throwInsts;
    };
    typedef StlList<OptCandidate*> OptCandidates;
    /// List of throwable objects that may be optimized.
    OptCandidates* optCandidates;
    static int level;
};

} // namespace Jitrino

#endif // _LAZYEXCEPTION_H_

