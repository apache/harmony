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
#include "open/vm_method_access.h"

#include "verifier.h"
#include "context_5.h"

/*
This method makes the first pass through the instruction set.
On that pass we check that all instruction have valid opcode, that no
jumps, no exception handlers lead to the middle of instruction nor 
out of the method. it checks that control does not flow out of the method.

It also finds all instructions that have multiple predecessors 
(like goto tagrtes), this information will be used on the second Pass

Method starts with the instruction <code>instr</code> for each it was invoked and go down 
filling the mask array with the flags. On this pass it distignushes
4 types of instructions:
0 - non-passed instruction or dead code
1 - passed instruction
2 - middle of passed instruction
3 - passed multiway instruction (having many predecessors)

If the method comes to a return, ret, athrow, or an already passed instruction, it terminates
If it comes to a switch, an if, or a jsr then it push all branches onto the stack
If it comes to a goto then it continues from the jump target
*/

vf_Result vf_Context_5::parse(Address instr, int dead_code_parsing, FastStack<Address>* deadstack) {
    // instruction is out of the method or in the middle of another instruction
    if( instr > m_code_length || props.isOperand(instr) ) {
        return error(VF_ErrorCodeEnd, "jump to the middle of instruction or out of the method");
    }

    while( instr < m_code_length ) {
        if( props.isParsePassed(instr) ) {
            // more than one branch leads to this instruction
            if( !dead_code_parsing ) {
                props.setMultiway(instr);
            }
            return VF_OK;
        }

        OpCode opcode = (OpCode)m_bytecode[instr];
        processed_instruction = instr;

        // does code correspond to any valid instruction?
        if( !instr_is_valid_bytecode(opcode) ) {
            return error(VF_ErrorInstruction, "invalid opcode");
        }

        // keep all nessesary information about instruction
        ParseInfo &pi = instr_get_parse_info(opcode);

        // get MINIMAL length of the instruction with operands
        unsigned instr_len = instr_get_minlen(pi);

        // code does not correspond to any valid instruction or method length is less than required
        if( instr + instr_len > m_code_length ) {
            return error(VF_ErrorInstruction, "method length is less than required");
        }

        if( instr_is_compound(opcode, pi) ) {
            // get ACTUAL length for variable length insgtructions
            instr_len = instr_get_len_compound(instr, opcode);

            // method length is less than required
            if( instr + instr_len > m_code_length ) {
                return error(VF_ErrorInstruction, "compound instruction: method length is less than required");
            }
        }

        // mark this instruction as processed
        assert( !props.isParsePassed(instr) );
        props.setParsePassed(instr);

        // check that no other instruction jumps to the middle of the current instruction
        for( Address i = instr + 1; i < instr + instr_len; i++ ) {
            if( !props.setOperand(i) ) {
                return error(VF_ErrorUnknown, "jump to the middle of instruction");
            }
        }



        if( instr_is_regular(pi) ) {
            //regular instruction - go to the next instruction
            instr += instr_len;
        } else if( instr_is_jump(pi) ) {
            // goto, goto_w, if*

            Address target = instr_get_jump_target(pi, m_bytecode, instr);

            // jump out of method or to the middle of an instruction
            if( target >= m_code_length || props.isOperand(target) ) {
                return error(VF_ErrorBranch, "jump out of method or to the middle of an instruction");
            }
            
            mark_stackmap_point(target);

            if( instr_direct(pi, opcode, m_bytecode, instr) ) {
                if( instr + instr_len < m_code_length ) deadstack->push(instr + instr_len);

                instr = target; // it is not an if* - go to jump target
            } else {
                // process conditional jump target or jsr
                stack.push(target);

                // go to the next instruction
                instr += instr_len;
            }
        } else if( instr_direct(pi, opcode, m_bytecode, instr) ) {
            // it is not a jump ==> it is return or throw or ret
            if( instr + instr_len < m_code_length ) deadstack->push(instr + instr_len);
            return VF_OK;
        } else {
            assert( instr_is_switch(pi) );

            Address next_target_adr = (instr & (~3) ) + 4;

            //default target
            Address target = instr_get_int32_target(instr, m_bytecode + next_target_adr);
            stack.push(target);

            mark_stackmap_point(target);

            // in tableswitch instruction target offsets are stored with shift = 4,
            // in lookupswitch with shift = 8
            int shift = (opcode == OP_TABLESWITCH) ? 4 : 8;

            for (next_target_adr += 12;
                next_target_adr < instr + instr_len;
                next_target_adr += shift)
            {
                target = instr_get_int32_target(instr, m_bytecode + next_target_adr);
                // jump out of method or to the middle of an instruction
                if( target >= m_code_length || props.isOperand(target) ) {
                    return error(VF_ErrorBranch, "jump out of method or to the middle of an instruction");
                }
                // process conditional jump target
                stack.push(target);
                mark_stackmap_point(target);
            }

            if( instr + instr_len < m_code_length ) deadstack->push(instr + instr_len);
            return VF_OK;
        }
    }

    //it might be a dead code -- code followed by JSR which never returns
    //if it's a dead code - it's OK, if it's not - we will catch it on the second pass
    return VF_OK;
}


vf_Result vf_Context_5::StartLinearDataflow(Address instr) {

    vf_Result tcr;
    int workmap_is_a_copy_of_stackmap;

    if( props.isDataflowPassed(instr) ) {
        //passed since it was added to the stack
        assert(instr);
        return VF_OK;
    }

    if (instr) {
        workmap_is_a_copy_of_stackmap = true;
        fill_workmap(instr);
    } else {
        //for the first instruction it does not matter if it is multiway or not
        workmap_is_a_copy_of_stackmap = false;
        // may return error in case of method's wrong signature
        if((tcr = create_method_initial_workmap()) != VF_OK ) {
            return tcr;
        }
    }

    //list of handlers unknown
    next_start_pc = 0;

    return DataflowLoop(instr, workmap_is_a_copy_of_stackmap);
}

vf_Result vf_Context_5::SubroutineDone(Address subr) {
    SubroutineData *subrdata = ((PropsHead_5*)props.getInstrProps(subr)->next)->getSubrData(m_max_stack + m_stack_start);
    subrdata->subrDataflowed = 1;

    if( !subrdata->retCount ) {
        //no ret from subroutine -- dead code follows
        return VF_OK;
    }

    Address jsr = subrdata->caller;

    OpCode opcode = (OpCode)m_bytecode[jsr];
    ParseInfo &pi = instr_get_parse_info(opcode);

    processed_instruction = jsr;
    if (jsr || props.isMultiway(jsr)) {
        //note that in SubroutineDone unlike StartLinearDataflow we get workmap from stackmap
        //in case of the first instruction of the method
        fill_workmap(jsr);
    } else {
        vf_Result tcr = create_method_initial_workmap();
        assert(tcr == VF_OK); // method's signature was already verified in StartLinearDataflow
    }

    //list of handlers unknown
    next_start_pc = 0;

    restore_workmap_after_jsr(subr);

    //make a shift to the instr following jsr
    Address instr = jsr + (opcode == OP_JSR_W ? 5 : 3);
    assert(opcode == OP_JSR || opcode == OP_JSR_W);


    return DataflowLoop(instr, 0);
}



// iterate thru the instructions starting with 'instr'
vf_Result vf_Context_5::DataflowLoop (Address instr, int workmap_is_a_copy_of_stackmap) {

    vf_Result tcr;

    while( instr < m_code_length ) {
        if( !workmap_is_a_copy_of_stackmap && props.isMultiway(instr) ) {
            //if instruction has a stackmap and workmap was not just obtained from that stackmap
            // add constraint: workmap is assignable to stackmap(instr)
            if( (tcr=new_generic_vector_constraint(instr)) != VF_OK ) {
                return tcr;
            }

            if( props.isDataflowPassed(instr) ) {
                return VF_OK;
            }

            fill_workmap(instr);
        }
        workmap_is_a_copy_of_stackmap = false;

        OpCode opcode = (OpCode)m_bytecode[instr];
        processed_instruction = instr;
        // keep all nessesary information about instruction
        ParseInfo &pi = instr_get_parse_info(opcode);

        //check IN types, create OUT types, check exception
        if( (tcr=dataflow_instruction(instr)) != VF_OK ) {
            return tcr;
        }

        props.setDataflowPassed(instr);

        unsigned instr_len = instr_get_minlen(pi);
        if( instr_is_compound(opcode, pi) ) {
            // get ACTUAL length for variable length insgtructions
            instr_len = instr_get_len_compound(instr, opcode);
        }

        if( instr_is_jump(pi) ) {
            Address target = instr_get_jump_target(pi, m_bytecode, instr);

            if( props.isMultiway(target) || instr_is_jsr(opcode) ) {
                //TODO: need to test commented out optimization
                //&& (!instr_direct(pi, opcode, m_bytecode, instr) || props.isDataflowPassed(target))
                if( (tcr=new_generic_vector_constraint(target)) != VF_OK ) {
                    return tcr;
                }
            }

            if( instr_direct(pi, opcode, m_bytecode, instr) ) {
                //goto, goto_w
                if( !props.isDataflowPassed(target) ) {
                    if( target < instr ) next_start_pc = 0;
                    instr = target;
                    continue;
                } else {
                    return VF_OK;
                }
            }


            //TODO: makes sense to move the block into dataflow_instruction??
            if( instr_is_jsr(opcode) ) {
                PropsHead_5 *target_pro = (PropsHead_5*)props.getInstrProps(target);

                if( !props.isDataflowPassed(target) ) {
                    for( unsigned i = 0; i < m_stack_start; i++ ) {
                        StackmapElement_5 &el = target_pro->stackmap.elements[i];
                        el.clearJsrModified();
                    }

                    //create vector for storing ret types coming out of subroutine
                    PropsHead_5 *retpro = newRetData();
                    retpro->instr = 0xFFFF;
                    assert(!target_pro->next || target_pro->next->instr != 0xFFFF );
                    retpro->next = target_pro->next;
                    target_pro->next = retpro;

                    SubroutineData *subrdata = retpro->getSubrData(m_stack_start+m_max_stack);

                    if( !props.getInstrProps(instr) && instr) {
                        //if jsr instruction does not have workmap copy or stackmap, associated with it - create it
                        assert(workmap->depth);
                        workmap->depth--; // undo PUSH(SM_RETADDR)
                        storeWorkmapCopy(instr);
                    }

                    //need to return to that JSR instr later, when finish subroutine processing
                    subrdata->caller = instr;

                    //need to postpone some finalizing stuff
                    stack.xPush(target, MARK_SUBROUTINE_DONE);

                    //process subroutine
                    stack.xPush(target);

                    return VF_OK;
                } else {
                    SubroutineData *subrdata = ((PropsHead_5*)target_pro->next)->getSubrData(m_stack_start+m_max_stack);

                    if( !subrdata->subrDataflowed ) {
                        //recursive call?
                        return error(VF_ErrorDataFlow, "recursive subroutine");
                    }

                    restore_workmap_after_jsr(target);

                    if( !subrdata->retCount ) {
                        //no ret from subroutine -- dead code follows
                        return VF_OK;
                    } 

                    instr += instr_len;
                    continue;
                }
            }

            if( !props.isMultiway(target) ) {
                //if* with no stackmap at branch
                storeWorkmapCopy(target);
                assert( !props.isDataflowPassed(target) );
            }

            if( !props.isDataflowPassed(target) ) {
                stack.xPush(target);
            }

            instr += instr_len;
        } else if( instr_direct(pi, opcode, m_bytecode, instr) ) {
            // it is not a jump ==> it is ret, return or throw
            return VF_OK;
        } else if( instr_is_switch(pi) ) {

            Address next_target_adr = (instr & (~3) ) + 4;

            //default target
            Address target = instr_get_int32_target(instr, m_bytecode + next_target_adr);
            processSwitchTarget(target);

            // in tableswitch instruction target offsets are stored with shift = 4,
            // in lookupswitch with shift = 8
            int shift = (opcode == OP_TABLESWITCH) ? 4 : 8;

            // process conditional jump target
            for (next_target_adr += 12;
                next_target_adr < instr + instr_len;
                next_target_adr += shift)
            {
                target = instr_get_int32_target(instr, m_bytecode + next_target_adr);
                processSwitchTarget(target);
            }

            return VF_OK;
        } else {
            assert( instr_is_regular(pi) );
            instr += instr_len;
        }

    }

    // control went out of method bounds
    return error(VF_ErrorCodeEnd, "control went out of method bounds");
}

vf_Result vf_Context_5::verify_method(Method_Handle method) {
    vf_Result tcr;

    //nothing to verify
    if( !method_get_bytecode_length( method ) ) {
        return VF_OK;
    }

    //load memory storage, read variable like max_stack, etc
    init(method);

    //////////////////////////// FIRST PASS /////////////////////////
    pass = 1;
    stack.push(0);

    uint16 idx;
    uint16 start_pc;
    uint16 end_pc;
    uint16 handler_pc;
    uint16 handler_cp_index;

    //check validness of try blocks
    //TODO: is verifier the right place for that?
    for( idx = 0; idx < m_handlecount; idx++ ) {
        method_get_exc_handler_info( m_method, idx, &start_pc, &end_pc, &handler_pc, &handler_cp_index );

        if( start_pc >= end_pc || end_pc > m_code_length ) {
            return error(VF_ErrorHandler, "start_pc >= end_pc OR end_pc > code_length");
        }
    }

    FastStack<Address> deadstack;
    do {
        while( !stack.is_empty() ) {
            vf_Result tcr = parse(stack.pop(), false, &deadstack);
            if( tcr != VF_OK ) {
                return tcr;
            }
        }

        for( idx = 0; idx < m_handlecount; idx++ ) {
            method_get_exc_handler_info( m_method, idx, &start_pc, &end_pc, &handler_pc, &handler_cp_index );

            if( props.isParsePassed(handler_pc)) {
                props.setMultiway(handler_pc);
                continue;
            }

            if( props.isOperand(handler_pc) ) {
                return error(VF_ErrorCodeEnd, "handler_pc at the middle of an instruction");
            }

            for( Address i = start_pc; i < end_pc; i++ ) {
                //check if there was a reachable code in try block
                if( props.isParsePassed(i) ) {
                    //push handler if there was
                    stack.push(handler_pc);
                    break;
                }
            }
        }
    } while (!stack.is_empty());

    //parsing dead code: check that no invalid opcodes are there and no jumps to the middles of the instructions
    //clean up the dead stack, (new pushs will go to the regular stack)
    while( !deadstack.is_empty() ) {
        vf_Result tcr = parse(deadstack.pop(), true, &stack);
        if( tcr != VF_OK ) {
            return tcr;
        }
    }

    //put all handlers, even for the dead try blocks
    for( idx = 0; idx < m_handlecount; idx++ ) {
        method_get_exc_handler_info( m_method, idx, &start_pc, &end_pc, &handler_pc, &handler_cp_index );
        stack.push(handler_pc);
    }

    //clean up the stack, (check jumps to the middle)
    while( !stack.is_empty() ) {
        vf_Result tcr = parse(stack.pop(), true, &stack);
        if( tcr != VF_OK ) {
            return tcr;
        }
    }
    //end of dead code parsing


    for( idx = 0; idx < m_handlecount; idx++ ) {

        method_get_exc_handler_info( m_method, idx, &start_pc, &end_pc, &handler_pc, &handler_cp_index );

        if( end_pc < m_code_length && props.isOperand(end_pc) || props.isOperand(start_pc) ) {
            return error(VF_ErrorCodeEnd, "start_pc or end_pc are at the middle of an instruction");
        }

        SmConstant handler_type;
        if( handler_cp_index ) {
            if( !tpool.cpool_get_class(handler_cp_index, &handler_type) ||
                !tpool.mustbe_assignable(handler_type, tpool.sm_get_const_throwable()) )
            {
                return error(VF_ErrorHandler, "incorrect constantpool entry");
            }
        } else {
            handler_type = tpool.sm_get_const_throwable();
        }

        props.setMultiway(handler_pc);
        createHandlerStackmap(handler_pc, handler_type);
    }

    //////////////////////////// SECOND PASS /////////////////////////
    pass = 2;
    props.pass2started(stackmapattr_calculation, m_code_length);

    stack.xPush(0);
    while( !stack.is_empty() ) {
        Address next;
        short mark;

        stack.xPop(&next, &mark);

        if( !mark ) {
            tcr = StartLinearDataflow(next);
        } else {
            assert(mark == MARK_SUBROUTINE_DONE);
            tcr = SubroutineDone(next);
        }

        if( tcr != VF_OK ) {
            return tcr;
        }
    }

    return VF_OK;
}




vf_Result vf_Context_5::new_ret_vector_constraint(Address jsr_target) {
    PropsHead_5 *inpro = (PropsHead_5*)props.getInstrProps(jsr_target);
    PropsHead_5 *outpro = (PropsHead_5*)inpro->next;
    assert(outpro->instr == 0xFFFF);

    SubroutineData *subrdata = outpro->getSubrData(m_stack_start + m_max_stack);
    subrdata->retCount++;

    //if it is a first ret from the given subroutine (majority of the cases)
    if( subrdata->retCount == 1 ) {
        //remove newly appeared ret addresses: it might happen
        //if non-top subroutine made a ret
        StackmapHead* original = inpro->getStackmap();
        unsigned i;

        for( i = 0; i < m_stack_start + workmap->depth; i++ ) {
            if( i < m_stack_start && !workmap->elements[i].isJsrModified() ) {
                //nothing new here
                continue;
            }

            SmConstant val = workmap->elements[i].getAnyPossibleValue();
            if( val.isRetAddr() ) {
                //check if it's a newly appeared ret addfress

                // '-1' is twice below to exclude top of the stack. 
                // top of the stack contains ret address for the current subroutine
                // it also cleaned up if it's still there

                if( i < m_stack_start + original->depth - 1 && 
                    original->elements[i].getAnyIncomingValue() == val ) 
                {
                    //most likely: this ret address was there before
                    continue;
                }

                //iterate thru original types and look for this ret address
                int found_in_original = 0;
                for( unsigned j = 0; j < m_stack_start + original->depth - 1; j++ ) {
                    if( original->elements[j].getAnyIncomingValue() == val ) {
                        found_in_original = 1;
                        break;
                    }
                }
                if( !found_in_original ) {
                    //original types did not have this ret address
                    workmap->elements[i] = _WorkmapElement_5(SM_BOGUS);
                }
            }
        }

        //TODO make sure incoming was created as JSR transformation
        tc_memcpy(outpro->getWorkmap(), workmap, sizeof(WorkmapHead) + sizeof(WorkmapElement_5) * (m_stack_start + workmap->depth));
        return VF_OK;
    }

    return error(VF_ErrorStackDepth, "Multiple returns to single jsr");
}


void vf_Context_5::restore_workmap_after_jsr(Address jsr_target) {
    PropsHead_5 *inpro = (PropsHead_5*)props.getInstrProps(jsr_target);
    PropsHead_5 *outpro = (PropsHead_5*)inpro->next;
    SubroutineData *subrdata = outpro->getSubrData(m_stack_start + m_max_stack);

    if( subrdata->retCount ) {
        assert( subrdata->retCount == 1 );
        WorkmapHead* outcoming = outpro->getWorkmap();
        workmap->depth = outcoming->depth;

        unsigned i;
        for( i = 0; i < m_stack_start; i++ ) {
            if( outcoming->elements[i].isJsrModified() ) {
                workmap->elements[i] = outcoming->elements[i];
            }
        }
        for( ; i < m_stack_start + workmap->depth; i++ ) {
            workmap->elements[i] = outcoming->elements[i];
        }    
    }
}

vf_Result vf_Context_5::new_scalar_constraint(WorkmapElement_5 *from, StackmapElement_5 *to) {
    assert(from->getAnyPossibleValue() != SM_NONE);

    if( from->isJsrModified() ) {
        //JSR overhead
        to->setJsrModified();
    }

    if( !from->isVariable() ) {
        SmConstant inc_val = from->getConst();
        return add_incoming_value( inc_val, to );
    } else {
        GenericCnstr* gen = from->getVariable()->firstGenericCnstr();
        while( gen ) {
            if( gen->variable == to ) return VF_OK;
            gen = gen->next();
        }

        IncomingType *inc = from->getVariable()->firstIncoming();
        from->getVariable()->newGenericConstraint(&mem, to);

        while( inc ) {
            vf_Result vcr = add_incoming_value( inc->value, to );
            if( vcr != VF_OK ) {
                return vcr;
            }
            inc = inc->next();
        }
        return VF_OK;
    }
}

vf_Result vf_Context_5::add_incoming_value(SmConstant new_value, StackmapElement_5 *destination) {
    //check if the node already has such incoming value
    IncomingType *inc = destination->firstIncoming();
    while( inc ) {
        if( new_value == inc->value  || inc->value == SM_BOGUS ) {
            return VF_OK;
        }
        inc = inc->next();
    }

    if( new_value.isNonMergeable() && destination->firstIncoming() ) {
        //uninit value merged to any different value is bogus
        //ret address merged to any different value is bogus
        //assert - incoming value exists is different - we've already checked that new_value is missing in the list of incoming values
        new_value = SM_BOGUS;
    }

    //add incoming value if it does not have
    Constraint* next = destination->firstOthers();
    //TODO: optimize memory footprint for new_value == SM_BOGUS
    destination->newIncomingType(&mem, new_value);

    //check if it contradicts to expected types and further propagate
    while( next ) {
        switch (next->type) {
        case CT_EXPECTED_TYPE:
            if( !tpool.mustbe_assignable(new_value, next->value) ) return error(VF_ErrorUnknown, "unexpected type on stack or local variable");
            break;
        case CT_GENERIC: {
            vf_Result vcr = add_incoming_value(new_value, next->variable);
            if( vcr != VF_OK ) return vcr;
            break;
                         }
        case CT_ARRAY2REF: {
            vf_Result vcr = add_incoming_value( tpool.get_ref_from_array(new_value), next->variable);
            if( vcr != VF_OK ) return vcr;
            break;
                           }
        default:
            assert(0);
            return error(VF_ErrorInternal, "unreachable statement in add_incoming_value");
        }
        next = next->next();
    }
    return VF_OK;
}
