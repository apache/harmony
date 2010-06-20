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
#ifndef __CONTEXT_BASE_H__
#define __CONTEXT_BASE_H__

#include <assert.h>
#include <string.h>

#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"

#include "verifier.h"
#include "memory.h"
#include "instr_props.h"
#include "tpool.h"

class SharedClasswideData {
public:
    SharedClasswideData(Class_Handle _klass) :
      class_constraints(0),
          k_class(_klass),
          k_major(class_get_version(_klass)),
#pragma warning( push )
#pragma warning( disable : 4355 )
          tpool(this, _klass, 256)
#pragma warning( pop )
      {}

      //basic storage for class-wide data, like mapping from Java classes to SmConstant types
      vf_TypePool tpool;

      //stores constraints in the classloader for reuse in vf_verify_class_constraints()
      vf_TypeConstraint *class_constraints;

      //storage for these constraints. class-wide
      Memory constraintPool;

      //class handler for the current class
      Class_Handle k_class;

      //major version of current class
      unsigned short  k_major;

      //below are actually not shared between differetn methods, 
      //we define large-size variables here to save stack footprint

      // basic storage for most of the method-wide data
      Memory mem;
};


//
// Context - main class of Type Checker
//

class vf_Context_Base {
public:
    vf_Context_Base(SharedClasswideData &_classwide) :

        classwide(_classwide), class_constraints(_classwide.class_constraints), 
        mem(_classwide.mem), constraintPool(_classwide.constraintPool), tpool(_classwide.tpool),
        k_class(_classwide.k_class), k_major(classwide.k_major)
    {}

    //current instruction - used for error message creation
    Address processed_instruction;

    //pass (in case if Java5 verification) - used for error message creation
    int  pass;

    //error message in case of unsuccessful verification
    const char *error_message;

    //store ref to classwide data
    SharedClasswideData &classwide;

    //store constraints on unloaded classes for later verification
    void set_class_constraints();
protected:
    friend class vf_TypePool;

    //class handler for the current class
    Class_Handle k_class;

    //major version of current class
    unsigned short  k_major;

    //method handler for the method being verified
    Method_Handle m_method;

    //method's bytecode
    U_8*          m_bytecode;

    //legth of the code in the method being verified
    unsigned       m_code_length;

    //max number of locals for the method being verified (as recorded in the classfile)
    unsigned       m_max_locals;

    //is the current method construcor (and current class in not a j.l.Object)?
    bool           m_is_constructor;

    //m_max_locals or m_max_locals+1 if it's a constructor
    unsigned       m_stack_start;

    //max stack size for the method being verified (as recorded in the classfile)
    unsigned       m_max_stack;

    //number of exception handlers for the being verified (as recorded in the classfile)
    unsigned short m_handlecount;

    //stores constraints in the classloader for reuse in vf_verify_class_constraints()
    vf_TypeConstraint_p &class_constraints;

    //storage for these constraints. class-wide
    //TODO: makes sense to unite it with tpool containing other class-wide data?
    Memory &constraintPool;

    // basic storage for most of the method-wide data
    Memory &mem;

    // table used to get various info (type, length, etc) about possible bytecode instructions
    static ParseInfo parseTable[255];

    // method's return type
    SmConstant return_type;

    //basic storage for class-wide data, like mapping from Java classes to SmConstant types
    vf_TypePool &tpool;

    /******* exception handling **********/

    //flag array. if a local var i was changed by the previous instruction ==> changed_locals[i]=1, otherwise it's 0
    U_8 *changed_locals;

    //if there is at least one local changed
    int locals_changed;

    //if we don't know whether previous instruction changed locals (like if we are at the branch target)
    int no_locals_info;

    //number of the first handler valid for the given instruction
    int loop_start;

    //<number of the last handler valid for the given instruction> + 1
    int loop_finish;

    //start of the nearest next try block. 0 means "don't know"
    Address next_start_pc;

    /*****************/

    //report verify error and store a message if any
    vf_Result error(vf_Result result, const char* message) {
        //PUT BREAKPOINT HERE!!!
        error_message = message ? message : "";
        //assert(0);
        return result;
    }

    //init method-wide data
    void init(Method_Handle _m_method) {
        //store method's parameters
        //TODO: it might be mot slower not to store them
        m_method = _m_method;
        m_max_locals = method_get_max_locals(m_method);
        m_max_stack = method_get_max_stack(m_method);
        m_code_length = method_get_bytecode_length(m_method);
        m_handlecount = method_get_exc_handler_number(m_method);
        m_bytecode = const_cast<U_8*>(method_get_bytecode(m_method));

        m_is_constructor = !strcmp(method_get_name(m_method), "<init>") 
            && class_get_super_class(k_class);

        m_stack_start = m_max_locals + (m_is_constructor ? 1 : 0);

        // initialize own parameters
        mem.init();

        changed_locals = (U_8*)mem.malloc((m_stack_start & ~3) + 4);

        //to correct it later
        return_type = SM_NONE;
    }


    /////////////////////// convinient methods //////////////////////////////////////////

    //get length of variable size instruction (WIDE, *SWITCH)
    int instr_get_len_compound(Address instr, OpCode opcode);

    //read two-byte value
    static uint16 read_uint16(U_8* ptr) {
        return (ptr[0] << 8) | ptr[1];
    }

    //read four-byte value
    static U_32 read_uint32(U_8* ptr) {
        return (ptr[0] << 24) | (ptr[1] << 16) | (ptr[2] << 8) | ptr[3];
    }

    //get a 16-bit jump target
    static Address instr_get_int16_target(Address instr, U_8* ptr) {
        return (Address) (instr + read_uint16(ptr));
    }

    //get a 32-bit jump target
    static Address instr_get_int32_target(Address instr, U_8* ptr) {
        return (Address) (instr + read_uint32(ptr));
    }

    //get properties specific for the given opcode
    static ParseInfo &instr_get_parse_info(OpCode opcode) {
        return parseTable[opcode];
    }

    //get the length of the given instruction or minimal length if unknown
    static U_8 instr_get_minlen(ParseInfo &pi) {
        return pi.instr_min_len;
    }

    //whether this instruction GOTO, IF*, or JSR
    static int instr_is_jump(ParseInfo &pi) {
        return pi.flags & PI_JUMP;
    }

    //whether this instruction GOTO, RETURN, ATHROW, or RET
    static int instr_direct(ParseInfo &pi, OpCode opcode, U_8* code, Address instr) {
        return (pi.flags & PI_DIRECT) || (opcode == OP_WIDE && code[instr + 1] == OP_RET);
    }

    //whether this instruction a *SWITCH
    static int instr_is_switch(ParseInfo &pi) {
        return pi.flags & PI_SWITCH;
    }

    //other types of instructions
    static int instr_is_regular(ParseInfo &pi) {
        return !(pi.flags & (PI_SWITCH|PI_JUMP|PI_DIRECT));
    }

    //whether instruction length is unknown
    static int instr_is_compound(OpCode opcode, ParseInfo &pi) {
        return (pi.flags & PI_SWITCH) || opcode == OP_WIDE;
    }

    //JSR ?
    static int instr_is_jsr(OpCode opcode) {
        return opcode == OP_JSR || opcode == OP_JSR_W;
    }

    //RET ?
    static int instr_is_ret(OpCode opcode, U_8* code, Address instr) {
        return opcode == OP_RET || opcode == OP_WIDE && code[instr + 1] == OP_RET;
    }

    //return the jump target for the given instruction
    static Address instr_get_jump_target(ParseInfo &pi, U_8* code, Address instr) {
        return ( pi.flags & PI_WIDEJUMP ) ? instr_get_int32_target(instr, code + instr + 1) :
            instr_get_int16_target(instr, code + instr + 1);
    }

    //is this opcode valid?
    static int instr_is_valid_bytecode(OpCode opcode) {
        return opcode <= OP_MAXCODE && opcode != OP_XXX_UNUSED_XXX;
    }

};

//check constraints stored in the classloader data. force loading if necessary
vf_Result vf_force_check_constraint(Class_Handle klass,
    vf_TypeConstraint *constraint);

#endif
