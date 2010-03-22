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
 * @author Intel
 *
 */

#ifndef _LOOP_UTILS_H_
#define _LOOP_UTILS_H_

#include "open/types.h"

#include "Opnd.h"
#include "LoopTree.h"

namespace Jitrino {

class OpndLoopInfo;
class InductiveOpndLoopInfo;
class InvariantOpndLoopInfo;
class ConstOpndLoopInfo;
class InductionDetector;

enum IVDetectionMode {
    CHOOSE_MAX_IN_BRANCH = 0,
    IGNORE_BRANCH
};

enum BoundType {
    UNKNOWN_BOUND = 0,
    EQ_BOUND,
    NE_BOUND,
    LOW_BOUND,
    LOW_EQ_BOUND,
    UPPER_BOUND,   
    UPPER_EQ_BOUND
};

class OpndLoopInfo {

    friend class InductionDetector;
    
public:
    virtual SsaOpnd* getOpnd() const { return mainOpnd; }
    
    virtual bool isInductive() const = 0;
    virtual bool isInvariant() const = 0;
    virtual bool isConstant() const = 0;

    virtual InductiveOpndLoopInfo* asInductive() {
        return isInductive() ? (InductiveOpndLoopInfo*)this : NULL; 
    }
    
    virtual InvariantOpndLoopInfo* asInvarinat() {
        return isInvariant() ? (InvariantOpndLoopInfo*)this : NULL;
    }

    virtual ConstOpndLoopInfo* asConstant() {
        return isConstant() ? (ConstOpndLoopInfo*)this : NULL;
    }
    
protected:
    OpndLoopInfo(InductionDetector* id, SsaOpnd* opnd):
         mainOpnd(opnd), inductionDetector(id) {};
    virtual ~OpndLoopInfo() {}
    
    SsaOpnd*            mainOpnd;
    InductionDetector*  inductionDetector;
}; 

class InductiveOpndLoopInfo : public OpndLoopInfo {

    friend class InductionDetector;

public:  
    virtual bool isInductive() const { return true; }    
    virtual bool isInvariant() const { return false; }
    virtual bool isConstant() const { return false; }
    
    /**
     * @returns scale info or null if scale operand is equal to 1.
     * @see getBase
     */
    InvariantOpndLoopInfo* getScale() { return scale; } 

    /**
     * An induction variable is a variable that gets increased or decreased
     * by a fixed amount on every iteration of a loop, or is a linear function
     * of another induction variable. Thus it can be represented in general form
     * as follows scale * base + stride, where scale and stride are loop invariants,
     * base is loop induction variable.
     *  
     * @returns info about dependent induction variable or null if induction variable
     * depends on itself.
     */
    InductiveOpndLoopInfo* getBase() { return base; }

    /**
     * @returns stride info or null if stride operand is equal to 0.
     * @see getBase
     */
    InvariantOpndLoopInfo* getStride() { return stride; }

    /**
     * Any induction variable takes values between its bounds. Inductive variable is equal to
     * its initial value at the first loop iteration.
     *
     * @returns info about initial value of the inductive variable or NULL if it cant be determined.
     * @note if initial value isnt proven to be loop invariant NULL will be returned.
     */
    InvariantOpndLoopInfo* getStartValue();

    /**
     * Any induction variable takes values between its bounds. End value determines maximum
     * possible range where induction variable may vary.
     *
     * @returns info about ending value of the inductive variable or NULL if it cant be determined.
     * @note if ending value isnt proven to be loop invariant NULL will be returned.
     */
    InvariantOpndLoopInfo* getEndValue();

    /**
     * Sets up start value of inductive variable.
     */
    void setStartValue(InvariantOpndLoopInfo* start);

    /**
     * Sets up end value of inductive variable.
     */
    void setEndValue(InvariantOpndLoopInfo* end);

    /**
     * Inductive varible can be limited by different type of constraints:
     * 1) var == endValue or var != endValue is STRICT_BOUND
     * 2) var > endValue or var >= endValue is LOW_BOUND
     * 3) var < endValue or var <= endValue is UPPER_BOUND
     * 
     * @returns type of the bound which limits inductive variable.
     */
    BoundType getBoundType();

protected:
    InductiveOpndLoopInfo(InductionDetector* id, SsaOpnd* opnd,
                          InvariantOpndLoopInfo* scaleInfo,
                          InductiveOpndLoopInfo* baseInfo,
                          InvariantOpndLoopInfo* strideInfo):
        OpndLoopInfo(id, opnd), scale(scaleInfo),
        base(baseInfo), stride(strideInfo),
        startValue(NULL), endValue(NULL), boundType(UNKNOWN_BOUND),
        header(NULL), isPhiSplit(false) {
        base = (base == NULL) ? this : base;         
    }    
    virtual ~InductiveOpndLoopInfo() {}

private:   
    void findBoundInfo();
    
    InvariantOpndLoopInfo*  scale;
    InductiveOpndLoopInfo*  base;
    InvariantOpndLoopInfo*  stride;
    
    InvariantOpndLoopInfo*  startValue;
    InvariantOpndLoopInfo*  endValue;
    BoundType               boundType;
    
    // implementation specific
    SsaOpnd*    header;
    bool        isPhiSplit;
};

class InvariantOpndLoopInfo : public OpndLoopInfo {

    friend class InductionDetector;
public:
    
    virtual bool isInductive() const { return false; }    
    virtual bool isInvariant() const { return true; }
    virtual bool isConstant() const { return false; }
    
protected:
    InvariantOpndLoopInfo(InductionDetector* id, SsaOpnd* opnd): OpndLoopInfo(id, opnd) {}
    virtual ~InvariantOpndLoopInfo() {}
};

class ConstOpndLoopInfo : public InvariantOpndLoopInfo {

    friend class InductionDetector;
public:    
    virtual bool isConstant() const { return true; }

    I_32 getValue() const { return value; }

protected:
    ConstOpndLoopInfo(InductionDetector* id, SsaOpnd* opnd, I_32 val):
        InvariantOpndLoopInfo(id, opnd), value(val) {}
    virtual ~ConstOpndLoopInfo() {}

private:
    I_32 value;
};

// TODO: Should be able to detect linear induction.
class InductionDetector {

    friend class InductiveOpndLoopInfo;
    
public:
    static InductionDetector* create(MemoryManager& mm, LoopNode* loop) {
        return new (mm) InductionDetector(mm, loop);
    }
    

    InductiveOpndLoopInfo*
    createInductiveOpnd(SsaOpnd* opnd,
                        InvariantOpndLoopInfo* scaleInfo,
                        InductiveOpndLoopInfo* baseInfo,
                        InvariantOpndLoopInfo* strideInfo) {
        return new (memoryManager)
            InductiveOpndLoopInfo(this, opnd, scaleInfo, baseInfo, strideInfo);
    }
    
    InvariantOpndLoopInfo* createInvariantOpnd(SsaOpnd* opnd) {
        return new (memoryManager) InvariantOpndLoopInfo(this, opnd); 
    }
    
    ConstOpndLoopInfo* createConstOpnd(SsaOpnd* opnd, I_32 val) {
        return new (memoryManager) ConstOpndLoopInfo(this, opnd, val);
    }

    OpndLoopInfo* getOpndInfo(SsaOpnd * opnd, IVDetectionMode mode = IGNORE_BRANCH);

private:    
    InductionDetector(MemoryManager& mm, LoopNode* loop);
    OpndLoopInfo* processOpnd(SsaOpnd * opnd);
    
    MemoryManager&      memoryManager;
    LoopNode*           loopNode;
    
    StlVector<Inst*>    defStack;
    ConstOpndLoopInfo*  zero;
    ConstOpndLoopInfo*  one;
    IVDetectionMode     ivMode;
};

} // namesapce Jitrino

#endif /*_LOOP_UTILS_H_*/
