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
#ifndef __CONTEXT5_H__
#define __CONTEXT5_H__

#include <assert.h>
#include <string.h>
#include "../base/context_x.h"
#include "stackmap_5.h"
#include "instr_props_5.h"

static const short MARK_SUBROUTINE_DONE = -1;

//
// Context - main class of Type Checker
//
class vf_Context_5 : public vf_Context_x<vf_Context_5, WorkmapElement_5, _WorkmapElement_5, StackmapElement_5> {
public:
    vf_Context_5(SharedClasswideData &classwide) :
      vf_Context_x<vf_Context_5, WorkmapElement_5, _WorkmapElement_5, StackmapElement_5>(classwide) {
          stackmapattr_calculation = false;
      }

      vf_Result verify_method(Method_Handle method);
protected:
    // various flags for all the method's bytecode instructions
    InstrProps props;

    // stack to push instructions like branch targets, etc to go thru the method. the stack is method-wide.
    MarkableStack stack;

    //we would like to flush StackMapTable attribute from this method
    bool      stackmapattr_calculation;

    void mark_stackmap_point(Address target) {
        //in case we prepare for flushing stackmaptable attrribute
        //make sure we avoid optimization and mark all targets as multiway
        if( stackmapattr_calculation ) stack.push(target);
    }

    //init method-wide data
    void init(Method_Handle _m_method) {
        vf_Context_x<vf_Context_5, WorkmapElement_5, _WorkmapElement_5, StackmapElement_5>::init(_m_method);
        stack.init();
        props.init(mem, m_code_length);
    }

    // load derived types previously stored for the given instruction
    void fill_workmap(Address instr) {
        PropsHead_5 *head = (PropsHead_5*)props.getInstrProps(instr);
        if( head->is_workmap() ) {
            tc_memcpy(workmap, head->getWorkmap(), sizeof(WorkmapHead) + sizeof(WorkmapElement_5) * (m_stack_start + head->workmap.depth));
        } else {
            StackmapHead *stackmap = head->getStackmap();

            workmap->depth = stackmap->depth;

            for( unsigned i = 0; i < m_stack_start + stackmap->depth; i++) {
                workmap->elements[i] = _WorkmapElement_5(&stackmap->elements[i]);
                assert( workmap->elements[i].getAnyPossibleValue() != SM_NONE );
            }
        }
        no_locals_info = 1;
    }

    //store a copy of the current workmap for another instruction (such as a branch target)
    void storeWorkmapCopy(Address target) {
        int sz = m_stack_start + workmap->depth;
        PropsHead_5* copy = newWorkmapProps(sz);
        tc_memcpy(copy->getWorkmap(), workmap, sizeof(WorkmapHead) + sizeof(WorkmapElement_5) * sz);

        props.setInstrProps(target, copy);
    }

    //create a stackmap vector of the given size sz (max_locals <= sz <= max_locals+max_stack)
    PropsHead_5* newStackmap(int sz) {
        return (PropsHead_5*)mem.calloc(sizeof(PropsHead_5) + sizeof(StackmapElement_5) * sz);
    }

    //create a vector that will be used for JSR procesing. 
    //It contains ether stackmap or workmap vector, SubrouitineData, and flags vector indicating 
    //changed locals
    PropsHead_5 *newRetData() {
        assert( sizeof(StackmapElement_5) >= sizeof(WorkmapElement_5) );

        int sz = sizeof(PropsHead_5) + sizeof(StackmapElement_5) * (m_max_stack + m_stack_start) + //stackmap
            ((sizeof(SubroutineData)+ m_stack_start) & (~3)) + 4; // fixed data and changed locals vector

        PropsHead_5 * ret = (PropsHead_5 *) mem.calloc(sz);
        ret->set_as_workmap();
        return ret;
    }

    //creates a temporary variable for converting 
    StackmapElement_5 *new_variable() {
        return (StackmapElement_5*) mem.calloc(sizeof(StackmapElement_5));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    //First verification pass thru the method. checks that no jump outside the method or to the middle of instruction
    //checks that opcodes are valid
    vf_Result parse(Address instr, int dead_code_parsing, FastStack<Address> *deadstack);

    //Second pass: dataflow of a piece of the method starting from the beginning or a branch target and finishing
    //on return, athrow or hitting previously passed instruction. 
    //This function initializes workmap and calls DataflowLoop
    vf_Result StartLinearDataflow(Address start);

    //Second pass: Finilize subroutie processing -- once we are here, then all the RETs from achievable for
    //the given subroutine are passed, so we can resume passing for JSRs to the given address
    //This function initializes workmap properly and calls DataflowLoop
    vf_Result SubroutineDone(Address start);

    //Second pass: dataflow of a piece of the method starting from the beginning or a branch target and finishing
    //on return, athrow or hitting previously passed instruction
    vf_Result DataflowLoop(Address start, int workmap_is_a_copy_of_stackmap);

    //constraint propagation
    vf_Result propagate(StackmapElement_5 *changed, SmConstant new_value);

    //update current derived types according to what was changed in subroutine
    void restore_workmap_after_jsr(Address jsr_target);

    //create vector constraints for each target of a switch
    vf_Result processSwitchTarget(Address target) {
        vf_Result tcr;
        if( props.isMultiway(target) ) {
            if( (tcr=new_generic_vector_constraint(target)) != VF_OK ) {
                return tcr;
            }

            if( !props.isDataflowPassed(target) ) {
                stack.xPush(target);
            }
        } else {
            assert( !props.isDataflowPassed(target) );
            storeWorkmapCopy(target);

            stack.xPush(target);
        }
        return VF_OK;
    }

    ///////////////////////////////////  "VIRTUAL" METHODS /////////////////////////////////////////////
public:
    //create constraint vector in case of a branch 
    //simple conatraints are created for pairs of both locals and stack (current must be assignable to target)
    vf_Result new_generic_vector_constraint(Address target_instr) {
        return new_generic_vector_constraint_impl(getStackmap(target_instr, workmap->depth));
    }

    //when we hit RET instruction we update the data for the given subroutine with current derived types
    vf_Result new_ret_vector_constraint(Address target_instr);

    // push catch-block to the stack of branches to pass
    void push_handler(Address handler_pc) {
        if( !props.isDataflowPassed(handler_pc) ) {
            stack.xPush(handler_pc);
        }
    }

    //create simple single constraint: "'from' is assingable to 'to'"
    vf_Result new_scalar_constraint(WorkmapElement_5 *from, StackmapElement_5 *to);

    //add one more possible value (type) that can come to the given point (local or stack)
    vf_Result add_incoming_value(SmConstant new_value, StackmapElement_5 *destination);

    //create stackmap for exception handler start
    void createHandlerStackmap(Address handler_pc, SmConstant type) {
        StackmapHead *map = getStackmap(handler_pc, 1);
        //handler stackmaps are created before any dataflow analysis is done
        assert(map->depth == 0 || map->depth == 1);
        map->depth = 1;

        vf_Result tcr = add_incoming_value(type, &map->elements[m_stack_start]);

        // it is initialization stage
        assert(tcr == VF_OK);
    }


    //create a workmap vector for the given size sz (max_locals <= sz <= max_locals+max_stack)
    PropsHead_5 *newWorkmapProps(int sz) {
        PropsHead_5 * ret = (PropsHead_5*)mem.malloc(sizeof(PropsHead_5) + sizeof(WorkmapElement_5) * sz);
        ret->set_as_workmap();
        return ret;
    }

    //returns stackmap for the 'instr' instruction
    //if it does not exists yet -- create it. When created use 'depth' as stack depth
    StackmapHead *getStackmap(Address instr, int depth) {
        PropsHead_5 *pro = (PropsHead_5*) props.getInstrProps(instr);
        if( !pro ) {
            pro = newStackmap(m_stack_start + depth);
            props.setInstrProps(instr, pro);
            pro->getStackmap()->depth = depth;
        }
        return pro->getStackmap();
    }

    //returns stackmap for the 'instr' instruction. it must exist
    StackmapHead *getStackmap(Address instr) {
        PropsHead_5 *pro = (PropsHead_5*)props.getInstrProps(instr);
        assert(pro);
        return pro->getStackmap();
    }

    /////////////// expect some type //////////////

    //expect exactly this type
    int workmap_expect_strict( WorkmapElement_5 &el, SmConstant type ) {
        assert(type != SM_BOGUS);

        if( !el.isVariable() ) {
            return type == el.getConst();
        }

        IncomingType *in = el.getVariable()->firstIncoming();
        while( in ) {
            if( type != in->value ) {
                return false;
            }
            in = in->next();
        }

        ExpectedType *exp = el.getVariable()->firstExpected();
        while( exp ) {
            if( type == exp->value ) {
                return true;
            }
            exp = exp->next();
        }

        el.getVariable()->newExpectedType(&mem, type);

        return true;
    }

    int workmap_expect( WorkmapElement_5 &el, SmConstant type ) {
        if( !el.isVariable() ) {
            return tpool.mustbe_assignable(el.getConst(), type);
        } else {
            ExpectedType* exp = el.getVariable()->firstExpected();
            while( exp ) {
                if( type == exp->value ) {
                    return true;
                }
                exp = exp->next();
            }

            IncomingType *in = el.getVariable()->firstIncoming();
            //check that all existing incoming type are assignable to the new expected type
            while( in ) {
                if( !tpool.mustbe_assignable(in->value, type) ) {
                    return false;
                }
                in = in->next();
            }
            //add the new expected type
            el.getVariable()->newExpectedType(&mem, type);
        }
        return true;
    }

    //create special type of conatraint: "'from' is an array and it's element is assignable to 'to'"
    vf_Result new_scalar_array2ref_constraint(WorkmapElement_5 *from, WorkmapElement_5 *to) {
        if( !from->isVariable() ) {
            //although new_scalar_conatraint() whould process from constants correctly 
            // we just do not need new variable if it is really a constant
            *to = _WorkmapElement_5( tpool.get_ref_from_array(from->getConst()) );
            return VF_OK;
        }
        assert( from->isVariable() );

        ArrayCnstr* arr = from->getVariable()->firstArrayCnstr();
        //at most one array conversion constraint per variable is possible
        if( arr ) {
            *to = _WorkmapElement_5(arr->variable);
            return VF_OK;
        }

        *to = _WorkmapElement_5( new_variable() );

        IncomingType *inc = from->getVariable()->firstIncoming();
        from->getVariable()->newArrayConversionConstraint(&mem, to->getVariable());

        while( inc ) {
            SmConstant inc_val = tpool.get_ref_from_array(inc->value);
            vf_Result vcr = add_incoming_value( inc_val, to->getVariable() );
            if( vcr != VF_OK ) {
                return vcr;
            }
            inc = inc->next();
        }
        return VF_OK;
    }

    void new_bogus_propagation_constraint(WorkmapElement_5 &wm_el, SmConstant init_val) {
        if( !wm_el.isVariable() ) {
            wm_el = _WorkmapElement_5 (init_val);
        } else {

            WorkmapElement_5 wm_init = _WorkmapElement_5 (new_variable());
            wm_init.var_ptr->newIncomingType(&mem, init_val);
            wm_el.getVariable()->newGenericConstraint(&mem, wm_init.getVariable());
            wm_el = wm_init;
        }
    }
};

#endif
