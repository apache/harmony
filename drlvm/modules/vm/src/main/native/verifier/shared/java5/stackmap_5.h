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
#ifndef __STACKMAP5_H__
#define __STACKMAP5_H__

#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "../base/stackmap_x.h"

struct StackmapElement_5;
struct WorkmapElement_5;

//possible relations between verificaton types
enum ConstraintType {
    CT_GENERIC = 0,         // sub-defined type A is assignable to sub-defined type B
    CT_ARRAY2REF = 1,       // A is a known-type array. element of A is assignable to sub-defined type B
    CT_EXPECTED_TYPE = 2,   // sub-defined type A is assignable to known-type B
    CT_INCOMING_VALUE = 3   // known-type A is assignable to sub-defined type B
};

//list constant verification type (i.e. known-type) that are assignable to some sub-definite type (i.e. StackMapElement)
//see StackmapElement
struct IncomingType {
    //next in the list
    IncomingType *nxt;

    //value of the verification type recorded as int
    _SmConstant value;

    //simple next in the list
    IncomingType *next() {
        return nxt;
    }
};

//list of constraints for some sub-definite verification type (i.e. StackMapElement)
//see StackmapElement
struct Constraint {
    //next in the list
    Constraint *nxt;

    //either
    union {
        StackmapElement_5 *variable; // sub-definite verificarion type
        int value;                 // or constant (known) verification type rcorded as int
    };

    //consatrint type
    ConstraintType type;

    //next constrait of type 't'
    static Constraint *next(Constraint *cur, int t) {
        while( cur && cur->type != t ) {
            cur = (Constraint*)cur->next();
        }
        return cur;
    }

    //simple next in the list
    Constraint *next() {
        return nxt;
    }
};

//constraint of the CT_EXPECTED_TYPE type: sub-defined type A is assignable to known-type B
struct ExpectedType : Constraint {
    ExpectedType *next() {
        return (ExpectedType *) Constraint::next(Constraint::next(), CT_EXPECTED_TYPE);
    }
};

//constraint of the CT_GENERIC type: sub-defined type A is assignable to sub-defined type B
struct GenericCnstr : Constraint {
    GenericCnstr *next() {
        return (GenericCnstr *) Constraint::next(Constraint::next(), CT_GENERIC);
    }
};

//constraint of the CT_ARRAY2REF type: A is a known-type array. element of A is assignable to sub-defined type B
struct ArrayCnstr : Constraint {
    //there can be only one CT_ARRAY2REF per StackMap Element
    ArrayCnstr *next() {
        assert(0);
        return 0;
    }
};

//StackMapElement structure represens sub-definite verification type: we don't know what type is it, but
//we know about instructions that expect ExpectedTypes here and we know that IncomingValues can be here
//we also know that this type must be assignable to other sub-defenite types as indicated by CT_GENERIC
//constrains and there can be special limitations represented by CT_ARRAY2REF constraints
struct StackmapElement_5 { //TODO: should be rewritten to save footprint
    //list of IncomingType constraint
    IncomingType *incoming;

    //list of all the conatraints of other types
    Constraint *others;

    //return value from any IncomingType constraint
    //when we need to compae to some unmergable type we don;t need to interate thru the list
    //also used to assert that an IncomingValue constraint exists
    SmConstant getAnyIncomingValue() {
        assert(firstIncoming());
        return firstIncoming()->value;
    }

    //return first IncomingType constraint
    IncomingType *firstIncoming() {
        //TODO: I have to store somewhere the "modified" bit. Sorry.
        return (IncomingType*)( (POINTER_SIZE_INT)incoming & ~3 );
    }

    //return first conatrint of any type except IncomingType
    Constraint *firstOthers() {
        return others;
    }

    //return first CT_EXPECTED_TYPE constraint
    ExpectedType *firstExpected() {
        return (ExpectedType*)Constraint::next(others, CT_EXPECTED_TYPE);
    }

    //return first CT_GENERIC constraint
    GenericCnstr *firstGenericCnstr() {
        return (GenericCnstr*)Constraint::next(others, CT_GENERIC);
    }

    //return first (and the only) CT_ARRAY2REF constraint
    ArrayCnstr *firstArrayCnstr() {
        return (ArrayCnstr*)Constraint::next(others, CT_ARRAY2REF);
    }

    //clean-up
    void init() {
        incoming = 0;
        others = 0;
    }

    //add incoming type with the 'value' value
    void newIncomingType(Memory *mem, SmConstant value) {
        IncomingType *in = (IncomingType *)mem->malloc(sizeof(IncomingType));

        POINTER_SIZE_INT mask = (POINTER_SIZE_INT)incoming & 3;
        incoming = (IncomingType *) ((POINTER_SIZE_INT)incoming & ~3);

        in->nxt = value == SM_BOGUS ? 0 : incoming;
        //in->type = CT_INCOMING_VALUE;
        in->value = value;

        incoming = in;

        incoming = (IncomingType *) ((POINTER_SIZE_INT)incoming | mask);
    }

    //add expected type with the 'value' value
    void newExpectedType(Memory *mem, SmConstant value) {
        newConstraint(mem, CT_EXPECTED_TYPE)->value = value.c;
    }

    Constraint *newConstraint(Memory *mem, int type) {
        Constraint *o = (Constraint *)mem->malloc(sizeof(Constraint));

        o->nxt = others;
        o->type = (ConstraintType)type;

        others = o;
        return o;
    }

    //add generic constraint ('this' is assignable to 'to')
    void newGenericConstraint(Memory *mem, StackmapElement_5 *to) {
        newConstraint(mem, CT_GENERIC)->variable = to;
    }

    //add generic constraint ('this' is an array, which element is assignable to 'to')
    void newArrayConversionConstraint(Memory *mem, StackmapElement_5 *to) {
        assert(!firstArrayCnstr());
        newConstraint(mem, CT_ARRAY2REF)->variable = to;
    }

    // return 'modified' flag for the stackmap. the flag is stored in the first bit of the 'incoming' pointer
    // "modified" is about subroutines: you have to track which locals were changed
    int isJsrModified() {
        return (int)(POINTER_SIZE_INT)incoming & 1;
    }

    //set 'modified' flag for the stackmap. the flag is stored in the first bit of the 'incoming' pointer
    // "modified" is about subroutines: you have to track which locals were changed
    void setJsrModified() {
        incoming = (IncomingType *) ((POINTER_SIZE_INT)incoming | 1);
    }

    //clear 'modified' flag for the stackmap. the flag is stored in the first bit of the 'incoming' pointer
    // "modified" is about subroutines: you have to track which locals were changed
    void clearJsrModified() {
        incoming = (IncomingType *) ((POINTER_SIZE_INT)incoming & ~1);
    }
};

//WorkMapElement structure represent an element of the workmap vector -- vector of the derived types
//a type might be either constant (or known) (e.g. if some previous instruction has put something on stack or locals)
//or sub-definite (e.g. if we've recently passed a branch target and don't know which types were on stack or locals)
struct WorkmapElement_5 {
    //value. two low bits a used to store flags
    union {
        _SmConstant const_val;        //either a constant (known-type)
        StackmapElement_5 *var_ptr;   //or a variable (sub-definite type)
    };

    //is it a sub-definite (not constant) type?
    int isVariable() {
        assert(const_val != SM_NONE);
        return !((POINTER_SIZE_INT)var_ptr & 1);
    }

    //get value for the constant (known) verification type
    SmConstant getConst() {
        return const_val;
    }

    //get variable representing sub-definite verification type
    StackmapElement_5 *getVariable() {
        return (StackmapElement_5 *) ((POINTER_SIZE_INT)var_ptr & ~3);
    }

    //when we need to compae to some unmergable type we don;t need to interate thru the list
    //also used to assert that an IncomingValue constraint exists
    SmConstant getAnyPossibleValue() {
        SmConstant ret = isVariable() ? getVariable()->getAnyIncomingValue() : (SmConstant) const_val;
        assert(ret != SM_NONE);
        return ret;
    }

    // return 'modified' flag for the workmap element. the flag is stored in the second bit of the union
    //"modified" is about subroutines: you have to track which locals were changed
    //it's easier to think of all the constants as "modified"
    int isJsrModified() {
        return (int)(POINTER_SIZE_INT)var_ptr & 3;
    }

    // set 'modified' flag for the workmap element. the flag is stored in the second bit of the union
    void setJsrModified() {
        if( isVariable() ) {
            var_ptr = (StackmapElement_5*)((POINTER_SIZE_INT)var_ptr | 2);
        }
    }
};

//WorkmapElement type with some constructors
struct _WorkmapElement_5 : WorkmapElement_5 {
    _WorkmapElement_5(WorkmapElement_5 other) {
        const_val = other.const_val;
    }

    _WorkmapElement_5(StackmapElement_5 *s) {
        var_ptr = s;
        if( s->isJsrModified() ) {
            setJsrModified();
        }
    }

    _WorkmapElement_5(SmConstant c) {
        const_val = c;
    }
};

//possible flag value
static const short FF_ISWORKMAP = 1;

//structure for maintaining subroutine-specific data
//until subroutine is passed with the second (dataflow) pass we record to the wait list all JSR instructions
//calling this subroutine. Once the subroutine is over we continue 2nd pass for each wait-listed instruction
//see vf_Context_Base::SubroutineDone
struct SubroutineData {
    Address caller;         //first JSR instruction that called this subroutine
    short retCount;         //number of ret instructions for this subroutine
    U_8  subrDataflowed;   // =1 if dataflow pass for the subroutine is over
};

//Store various data for the given instruction. Possible data are: StackMap vector, WorkMap vector,
//Subroutine-specific data
//for a single instruction it might be either
// 1) no data
// 2) workmap only
// 3) stackmap only
// 4) stackmap and subroutine data. in this case two PropsHead structures are created the first one for the StackMap,
//    it's 'next' points to the second PropsHead containing Subroutine info. In this case second PropsHead keeps 0xFFFF
//    instead of 'instr'
// the list is used to organize storing Props as a HashTable
struct PropsHead_5 : public PropsHeadBase {
    typedef MapHead<WorkmapElement_5> WorkmapHead;
    typedef MapHead<StackmapElement_5> StackmapHead;

    // really one bit is used: FF_ISWORKMAP. TODO: merge with (Stack|Work)map->flags
    unsigned short instr_flags; 

    //actual properties
    union {
        WorkmapHead workmap;
        StackmapHead stackmap;
    };

    //get workmap stored here
    WorkmapHead *getWorkmap() {
        assert(is_workmap());
        return &workmap;
    }

    //get stackmap stored here
    StackmapHead *getStackmap() {
        assert(!is_workmap());
        return &stackmap;
    }

    //get subroutine data stored here
    SubroutineData *getSubrData(int el_cnt) {
        assert(instr == 0xFFFF);
        return (SubroutineData *) &stackmap.elements[el_cnt];
    }

    //is it a workmap?
    int is_workmap() {
        return instr_flags & FF_ISWORKMAP;
    }

    //set 'is workmap' flag
    void set_as_workmap() {
        instr_flags |= FF_ISWORKMAP;
    }

    //clear flag
    void clearInstrFlag(short flag) {
        instr_flags &= ~flag;
    }
};

#endif
