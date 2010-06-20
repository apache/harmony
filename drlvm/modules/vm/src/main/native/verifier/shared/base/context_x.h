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
#ifndef __CONTEXT_X_H__
#define __CONTEXT_X_H__

#include <assert.h>
#include <string.h>
#include "context_base.h"
#include "stackmap_x.h"

//
// Context - main class of Type Checker
//

template<typename ActualClass, typename WorkmapElement, typename _WorkmapElement, typename StackmapElement>
class vf_Context_x : public vf_Context_Base {
public:
    vf_Context_x(SharedClasswideData &classwide) : vf_Context_Base(classwide) {}

    typedef MapHead<WorkmapElement> WorkmapHead;
    typedef MapHead<StackmapElement> StackmapHead;
protected:

    // current set of derived types
    WorkmapHead *workmap;

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    //create an uninitialized workmap vector for the given size sz (max_locals <= sz <= max_locals+max_stack)
    WorkmapHead *newWorkmap(int sz) {
        return (WorkmapHead*)mem.malloc(sizeof(WorkmapHead) + sizeof(WorkmapElement) * sz);
    }

    //create workmap for zero instruction from method's arguments (second pass in case of java5 verification)
    vf_Result create_method_initial_workmap();

    //check type-safety of a single instruction (second pass in case of java5 verification)
    vf_Result dataflow_instruction(Address instr);

    //check type-safety for exception handlers of a single instruction (second pass in case of java5 verification)
    vf_Result dataflow_handlers(Address instr);

    //specail care for <init> calls in try blocks
    vf_Result propagate_bogus_to_handlers(Address instr, SmConstant uninit_value);

    //create constraint vector in case of a branch 
    //simple conatraints are created for pairs of both locals and stack (current must be assignable to target)
    vf_Result new_generic_vector_constraint_impl(StackmapHead *target);

    //create constraint vector for exception handler
    //simple conatraints are created for pairs of local variable (current must be assignable to start of exception handler)
    vf_Result new_handler_vector_constraint(Address handler);

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    //check conditions for accessing protected non-static fields in different package
    vf_Result popFieldRef(SmConstant expected_ref, unsigned short cp_idx);

    //check conditions for accessing protected virtual methods in different package
    vf_Result popVirtualRef(SmConstant expected_ref, unsigned short cp_idx);

    /////////////// set, get locals; push, pop stack; check... //////////////

    //when exercizing instructions: POP operand from the stack
    WorkmapElement workmap_pop() {
        assert( workmap_can_pop(1) );
        return workmap->elements[ (--workmap->depth) + m_stack_start ];
    }

    //looking the operand stack
    WorkmapElement &workmap_stackview(int depth) {
        assert( depth >= 0 && workmap_can_pop(depth+1) );
        return workmap->elements[ workmap->depth + m_stack_start - depth - 1];
    }

    //when exercizing instructions: PUSH operand to the stack
    void workmap_push(WorkmapElement el) {
        assert( workmap_can_push(1) );
        workmap->elements[ (workmap->depth++) + m_stack_start ] = el;
    }

    //when exercizing instructions: PUSH a const (known) type to the stack (except long and double)
    void workmap_push_const(SmConstant value) {
        assert( workmap_can_push(1) );
        workmap->elements[ workmap->depth + m_stack_start ] = _WorkmapElement(value);
        workmap->depth++;
    }

    //when exercizing instructions: PUSH a const (known) long or double type to the stack
    void workmap_2w_push_const(SmConstant value) {
        workmap_push_const(value);
        workmap_push_const(SM_HIGH_WORD);
    }

    //when exercizing instructions: check if the local idx is valid for long and double
    bool workmap_valid_2w_local(unsigned idx) {
        return workmap_valid_local(idx + 1);
    }

    //when exercizing instructions: check if the local idx is valid (except long and double)
    bool workmap_valid_local(unsigned idx) {
        return idx < m_max_locals;
    }

    //get local type by idx
    WorkmapElement workmap_get_local(unsigned idx) {
        assert( workmap_valid_local(idx) );
        return workmap->elements[ idx ];
    }

    //set local type
    void workmap_set_local(unsigned idx, WorkmapElement &el) {
        assert( workmap_valid_local(idx) );

        changed_locals[ idx ] = 1;
        locals_changed = true;

        el.setJsrModified();
        workmap->elements[ idx ] = el;		
    }

    //set local to a const (known) type except long and double
    void workmap_set_local_const(unsigned idx, SmConstant value) {
        assert( workmap_valid_local(idx) );

        changed_locals[ idx ] = 1;
        locals_changed = true;

        workmap->elements[idx] = _WorkmapElement(value);

        //don't need to set "jsr modified" flag for constants
        //because they are already odd
        assert(workmap->elements[idx].isJsrModified());
    }                                                              

    //set local to a const (known) long or double type
    void workmap_set_2w_local_const(unsigned idx, SmConstant value) {
        assert( workmap_valid_2w_local(idx) );
        workmap_set_local_const(idx, value);
        workmap_set_local_const(idx + 1, SM_HIGH_WORD);
    }

    //check whether we can pop 'number' elements from the operand stack
    int workmap_can_pop(unsigned number) {
        return workmap->depth >= number;
    }

    //check whether we can push 'number' elements to the operand stack
    int workmap_can_push(unsigned number) {
        return workmap->depth + number <= m_max_stack;
    }

    //////////////// get constant SM_ELEMENTs ///////////////////////

    //for given uninit_value create SmConstant for initialized value
    //this function is used when <init>s and invoked
    SmConstant sm_convert_to_initialized(SmConstant uninit_value) {
        if( uninit_value == SM_THISUNINIT ) {
            return tpool.sm_get_const_this();
        }

        if( uninit_value.isNewObject() ) {
            Address addr = uninit_value.getNewInstr();

            unsigned cp_idx = read_uint16(m_bytecode + addr + 1);
            SmConstant new_type;
            if( !tpool.cpool_get_class(cp_idx, &new_type) ) {
                assert(0);
                return SM_BOGUS;
            }
            return new_type;
        }

        assert(0);
        return SM_BOGUS;
    }
};

#endif
