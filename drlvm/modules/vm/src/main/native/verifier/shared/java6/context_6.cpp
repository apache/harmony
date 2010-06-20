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

#include "context_6.h"

/**
 * Does Java6 verification of a method.
 */
vf_Result vf_Context_6::verify_method(Method_Handle method) {
    vf_Result tcr;

    //nothing to verify
    if( !method_get_bytecode_length( method ) ) {
        return VF_OK;
    }

    //load memory storage, read variable like max_stack, etc
    init(method);

    //create workmap for zero instruction from method arguments
    if((tcr = create_method_initial_workmap()) != VF_OK ) {
        return tcr;
    }

    //parse stackmaptable, create Stackmaps for jump targets and exception handlers (catch block starts)
    if((tcr = load_stackmaptable()) != VF_OK ) {
        return tcr;
    }

    //////////////////////////// Check Exception Handlers (catch block starts) /////////////////////////

    for( unsigned short idx = 0; idx < m_handlecount; idx++ ) {
        uint16 start_pc;
        uint16 end_pc;
        uint16 handler_pc;
        uint16 handler_cp_index;

        method_get_exc_handler_info( m_method, idx, &start_pc, &end_pc,
            &handler_pc, &handler_cp_index );

        //just in case: it has been checked in classloader
        if( start_pc >= end_pc || end_pc > m_code_length ) {
            return error(VF_ErrorHandler, "start_pc >= end_pc OR end_pc > code_length");
        }

        SmConstant handler_type;
        if( handler_cp_index ) {
            //check that expected exception is of type throwable
            if( !tpool.cpool_get_class(handler_cp_index, &handler_type) ||
                !tpool.mustbe_assignable(handler_type, tpool.sm_get_const_throwable()) )
            {
                return error(VF_ErrorHandler, "incorrect constantpool entry");
            }
        } else {
            handler_type = tpool.sm_get_const_throwable();
        }

        //check stackmap at the point of exception handler
        if((tcr = checkHandlerStackmap(handler_pc, handler_type)) != VF_OK ) {
            return tcr;
        }
    }

    //use Java5 method to find first applicable try block
    next_start_pc = 0;

    //////////////////////////// Dataflow Pass /////////////////////////

    Address instr = 0;
    int afterJump = false;
    while( instr < m_code_length ) {

        OpCode opcode = (OpCode)m_bytecode[instr];
        processed_instruction = instr;

        // does code correspond to any valid instruction?
        if( !instr_is_valid_bytecode(opcode) || instr_is_jsr(opcode) ) {
            //to check WIDE RET we need to check m_code_length
            //anyway check for JSR is enough
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

        // check that no other instruction jumps to the middle of the current instruction
        for( Address i = instr + 1; i < instr + instr_len; i++ ) {
            if( props.getInstrProps(i) ) {
                //there is a stack map recorded for this instruction
                return error(VF_ErrorStackmap, "StackMapTable at the middle of instruction");
            }
        }


        /////////////////////////////////////////////////////////////////////////

        if( props.getInstrProps(instr) ) {
            //if instruction has a stackmap
            if( !afterJump && (tcr=new_generic_vector_constraint(instr)) != VF_OK ) {
                return tcr;
            }

            fill_workmap(instr);
        } else {
            if( afterJump ) return error(VF_ErrorBranch, "Stackmap expected");
        }
        afterJump = false;

        //check IN types, create OUT types, check exception
        if( (tcr=dataflow_instruction(instr)) != VF_OK ) {
            return tcr;
        }

        if( instr_is_jump(pi) ) {
            afterJump = instr_direct(pi, opcode, m_bytecode, instr);

            Address target = instr_get_jump_target(pi, m_bytecode, instr);

            if( (tcr=new_generic_vector_constraint(target)) != VF_OK ) {
                return tcr;
            }
        } else if( instr_direct(pi, opcode, m_bytecode, instr) ) {
            // it is not a jump ==> it is ret, return or throw
            afterJump = true;
        } else if( instr_is_switch(pi) ) {
            afterJump = true;

            Address next_target_adr = (instr & (~3) ) + 4;

            //default target
            Address target = instr_get_int32_target(instr, m_bytecode + next_target_adr);
            new_generic_vector_constraint(target);

            // in tableswitch instruction target offsets are stored with shift = 4,
            // in lookupswitch with shift = 8
            int shift = (opcode == OP_TABLESWITCH) ? 4 : 8;

            // process conditional jump target
            for (next_target_adr += 12;
                next_target_adr < instr + instr_len;
                next_target_adr += shift)
            {
                target = instr_get_int32_target(instr, m_bytecode + next_target_adr);
                new_generic_vector_constraint(target);
            }
        } else {
            assert( instr_is_regular(pi) );
        }
        instr += instr_len;
    }

    // control went out of method bounds
    return afterJump ? VF_OK : error(VF_ErrorCodeEnd, "control went out of method bounds");
}


/**
 * Parses StackMapTable attribute and store Stackmap vectors for the instructions.
 */
vf_Result vf_Context_6::load_stackmaptable() {
    vf_Result tcr;

    U_8* stackmaptable = method_get_stackmaptable(m_method);

    if(!stackmaptable) return VF_OK;

    U_8* read_ptr = stackmaptable;

    read_ptr+=2; //skip uint16 attribute_name_index

    U_32 attribute_length = read_uint32(read_ptr); 
    read_ptr+=4;
    U_8* attribute_end = stackmaptable + attribute_length + 6;

    if( read_ptr + 2 > attribute_end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
    uint16 number_of_entries = read_uint16(read_ptr);
    read_ptr+=2;

    //create working copy fr previous stackmap frame, offeset, and number of locals
    WorkmapHead *lastWorkmap = newWorkmap(m_stack_start + m_max_stack);
    tc_memcpy(lastWorkmap, workmap, sizeof(WorkmapHead) + sizeof(WorkmapElement_6) * (m_stack_start + workmap->depth));

    unsigned last_maxlocals = m_max_locals;
    while( last_maxlocals > 0 ) {
        if( workmap->elements[last_maxlocals - 1].const_val != SM_BOGUS ) 
        {
            break;
        }
        last_maxlocals--;
    }

    int last_offset = -1;

    for( unsigned entry = 0; entry < number_of_entries; entry++) {
        if( read_ptr + 1 > attribute_end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");

        U_8 frame_type = (*read_ptr++);
        unsigned offset;
        if( frame_type <= 63 ) { // 0-63
            //same locals as previous, stack is empty. offset calculated from frame_type
            offset = frame_type;
            lastWorkmap->depth = 0;

        } else if (frame_type <= 127 ) { //64-127 
            //same locals as previous, stack contains single element specified here. offset calculated from frame_type
            offset = frame_type - 64;

            unsigned k = 1; // k may change in read_types(): if it's LONG or DOUBLE stack size will be '2'
            if( (tcr=read_types(&read_ptr, attribute_end, &lastWorkmap->elements[m_stack_start], &k, m_max_stack)) != VF_OK ) {
                return tcr;
            }
            lastWorkmap->depth = k;

        } else if (frame_type <= 246 ) {
            //reserved
            error(VF_ErrorStackmap, "corrupted StackMapTable");

        } else if (frame_type == 247 ) {
            //same locals as previous, stack contains single element specified here. offset is explicitely specified
            if( read_ptr + 2 > attribute_end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
            offset = read_uint16(read_ptr);
            read_ptr+=2;

            unsigned k = 1; // k may change in read_types(): if it's LONG or DOUBLE stack size will be '2'
            if( (tcr=read_types(&read_ptr, attribute_end, &lastWorkmap->elements[m_stack_start], &k, m_max_stack)) != VF_OK ) {
                return tcr;
            }
            lastWorkmap->depth = k;

        } else if (frame_type <= 250) { // 248-250
            //stack is empty, locals the same (tail is cut)
            unsigned k = 251 - frame_type; // last k locals are missing (k may change if there are LONG or DOUBLE there)

            while ( k ) {
                if( 0 == last_maxlocals-- ) return error(VF_ErrorStackmap, "corrupted StackMapTable");

                if( lastWorkmap->elements[last_maxlocals].const_val == SM_HIGH_WORD) {
                    ++k;
                }

                lastWorkmap->elements[last_maxlocals].const_val = SmConstant(SM_BOGUS);
                --k;
            }

            if( read_ptr + 2 > attribute_end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
            offset = read_uint16(read_ptr);
            read_ptr+=2;

            lastWorkmap->depth = 0;
        } else if (frame_type == 251 ) { // 251
            //same locals as previous, stack is empty. offset is explicitely specified
            if( read_ptr + 2 > attribute_end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
            offset = read_uint16(read_ptr);
            read_ptr+=2;

            lastWorkmap->depth = 0;
        } else if (frame_type <= 254 ) { // 252-254
            //stack is empty, locals are extended
            if( read_ptr + 2 > attribute_end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
            offset = read_uint16(read_ptr);
            read_ptr+=2;

            unsigned k = frame_type - 251; //may change in read_types()
            if( (tcr=read_types(&read_ptr, attribute_end, &lastWorkmap->elements[last_maxlocals], &k, m_max_locals - last_maxlocals)) != VF_OK ) {
                return tcr;
            }
            last_maxlocals += k;

            lastWorkmap->depth = 0;
        } else {
            //all entries are specified explicitely
            assert(frame_type == 255);

            if( read_ptr + 4 > attribute_end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
            offset = read_uint16(read_ptr);
            read_ptr+=2;

            last_maxlocals = read_uint16(read_ptr); //may change in read_types()
            read_ptr+=2;

            if( (tcr=read_types(&read_ptr, attribute_end, &lastWorkmap->elements[0], &last_maxlocals, m_max_locals)) != VF_OK ) {
                return tcr;
            }

            for( unsigned i = last_maxlocals; i < m_stack_start; i++ ) {
                lastWorkmap->elements[i].const_val = SmConstant(SM_BOGUS);
            }

            if( read_ptr + 2 > attribute_end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
            unsigned depth = read_uint16(read_ptr); //may change in read_types()
            read_ptr+=2;

            if( (tcr=read_types(&read_ptr, attribute_end, &lastWorkmap->elements[m_stack_start], &depth, m_max_stack)) != VF_OK ) {
                return tcr;
            }
            lastWorkmap->depth = depth;
        }

        //calculate instruction address
        last_offset = last_offset == -1 ? offset : last_offset + offset + 1;

        //record stackmap for the instruction
        PropsHead_6 *pro = newStackmap(m_stack_start + lastWorkmap->depth);
        props.setInstrProps(last_offset, pro);
        StackmapHead *sm = pro->getStackmap();

        //set stack depth
        sm->depth = lastWorkmap->depth;

        unsigned i = 0;
        SmConstant flag_element = tpool.sm_get_const_this();
        //copy obtained data to stackmap of the instruction, check whether there is SM_THISUNINIT on stack or in locals
        for( ; i < m_max_locals; i++ ) {
            sm->elements[i].const_val = lastWorkmap->elements[i].const_val;
            if( sm->elements[i].const_val == SM_THISUNINIT ) flag_element = SmConstant(SM_THISUNINIT);
        }

        //skip copying workmap->elements[m_max_locals] that in case of constructor contains flags

        for( i = m_stack_start; i < m_stack_start + lastWorkmap->depth; i++ ) {
            sm->elements[i].const_val = lastWorkmap->elements[i].const_val;
            if( sm->elements[i].const_val == SM_THISUNINIT ) flag_element = SmConstant(SM_THISUNINIT);
        }

        //initialize the flags
        if( m_is_constructor ) sm->elements[m_max_locals].const_val = flag_element;

    }

    if( read_ptr < attribute_end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
    return VF_OK;
}

/**
 * Reads cnt types from a row bytearray representing StackMapTable and record to workmap starting at 
 * the specified element. If Long or Double happens in StackMapTable, record SM_HIGH_WORD after SM_LONG or SM_DOUBLE
 * to the workmap and increase cnt. Check space_available when record to the workmap
 */
vf_Result vf_Context_6::read_types(U_8** attr, U_8* end, WorkmapElement_6* element,
                                   unsigned* cnt, unsigned space_available) {
    uint16 idx = 0;
    //read (*cnt) types
    while( idx < *cnt ) {

        //attribute truncated?
        if( (*attr) > end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");

        //more elemens than max_locals or max_stack
        if( idx >= space_available ) return error(VF_ErrorStackmap, "corrupted StackMapTable");

        U_8 tag = *((*attr)++);
        switch (tag) {
            case ITEM_TOP:
                element[idx++].const_val = SmConstant(SM_BOGUS);
                break;
            case ITEM_INTEGER:
                element[idx++].const_val = SmConstant(SM_INTEGER);
                break;
            case ITEM_FLOAT:
                element[idx++].const_val = SmConstant(SM_FLOAT);
                break;
            case ITEM_DOUBLE:
                element[idx++].const_val = SmConstant(SM_DOUBLE);
                if( idx >= space_available ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
                (*cnt)++;
                element[idx++].const_val = SmConstant(SM_HIGH_WORD);
                break;
            case ITEM_LONG:
                element[idx++].const_val = SmConstant(SM_LONG);
                if( idx >= space_available ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
                (*cnt)++;
                element[idx++].const_val = SmConstant(SM_HIGH_WORD);
                break;
            case ITEM_NULL:
                element[idx++].const_val = SmConstant(SM_NULL);
                break;
            case ITEM_UNINITIALIZEDTHIS:
                element[idx++].const_val = SmConstant(SM_THISUNINIT);
                break;
            case ITEM_OBJECT: {
                if( (*attr) + 2 > end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
                uint16 cp_idx = read_uint16(*attr);
                (*attr)+=2;

                SmConstant c;
                if( !tpool.cpool_get_class(cp_idx, &c) ) return error(VF_ErrorStackmap, "incorrect constantpool entry");
                element[idx++].const_val = c;

                break;
                    }
            case ITEM_UNINITIALIZED: {
                if( (*attr) + 2 > end ) return error(VF_ErrorStackmap, "corrupted StackMapTable");
                uint16 address = read_uint16(*attr);
                (*attr)+=2;

                element[idx++].const_val = SmConstant::getNewObject(address);
                break;
                    }
            default:
                return error(VF_ErrorStackmap, "corrupted StackMapTable");
        }
    }
    return VF_OK;
}
