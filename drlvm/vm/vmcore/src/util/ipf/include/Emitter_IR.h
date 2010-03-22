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
#ifndef _EMITTER_IR_H
#define _EMITTER_IR_H

#include "tl/memory_pool.h"
#include "merced.h"
#include "open/types.h"

// defines

#define Patch Enc_Patch
#define Code_Patch Enc_Code_Patch
#define Branch_Patch Enc_Branch_Patch
#define Movl_Patch   Enc_Movl_Patch

//////////////////////////////////////////////////////////////////
//               class IPF_Encoder
//////////////////////////////////////////////////////////////////

class IPF_Encoder : public Merced_Encoder {
  public:
    IPF_Encoder() : Merced_Encoder(0) {
        assert(*(U_32*)"JIT1"==0x3154494A);
        // assure little-endian because get_slot01_code_image() needs it
    }

    void code_emit() { LDIE(51, "Not implemented"); }     // shouldn't be called

    void get_slot01_code_image(uint64 *p_code_image1, uint64 *p_code_image2) {
        uint64& code_image1 = *p_code_image1;
        uint64& code_image2 = *p_code_image2;

        code_image1 = *(uint64 *)_char_value; // 5..45, little-endian
        assert((slot_num==1 && (code_image1 & 0xFFFFC0000000001Full)==0) || // gashiman - Make constants gcc friendly
               (slot_num==2 && (code_image1 & 0x000000000000001Full)==0) ); // gashiman - Make constants gcc friendly
        code_image1 >>= 5;

        if (slot_num!=1) {
            assert(slot_num==2);
            code_image2 = ((*((uint64 *)_char_value) >> 46)|(*((uint64 *)_char_value + 1) << 18));
            assert((code_image2 & 0xFFFFFE0000000000ull)==0); // gashiman - Make constants gcc friendly
        } else
            code_image2 = 0;

        slot_num = 0;               // reset slot_num
        {   ((uint64 *)_char_value)[0] = 0;
            ((uint64 *)_char_value)[1] = 0;
        }
    }
};

//////////////////////////////////////////////////////////////////
//                 class Encoder_All_Reg_BV
//////////////////////////////////////////////////////////////////

// number of registers

#define ENC_N_GEN_REG      128
#define ENC_N_FLOAT_REG    128
#define ENC_N_BRANCH_REG   8
#define ENC_N_PRED_REG     64
#define ENC_N_APPL_REG     128
#define ENC_N_REG          (ENC_N_GEN_REG + ENC_N_FLOAT_REG + ENC_N_BRANCH_REG + ENC_N_PRED_REG + ENC_N_APPL_REG)


// A bit vector for representing all registers
// Logically, this class is a result of partial evaluation of the class
// Bit_Vector, for a fixed vector length equal to ENC_N_REG.

#define ENC_WORD_SIZE 64
#define ENC_ALL_REG_WORDS (ENC_N_REG/ENC_WORD_SIZE + 1)
#define ENC_UINT64_ONE    ((uint64)0x01)

class Enc_All_Reg_BV {
    uint64 _words[ENC_ALL_REG_WORDS];
public:
    Enc_All_Reg_BV() {}
    Enc_All_Reg_BV(bool init) {
        if (init)  set_all();
        else       reset_all();
    }
    void *operator new(size_t sz,tl::MemoryPool &mm) {return mm.alloc(sz);}
    void operator delete (void *p, tl::MemoryPool& m) { }

    void set(unsigned bit) { 
        assert(bit<ENC_N_REG);        
        _words[bit/ENC_WORD_SIZE] |=  (ENC_UINT64_ONE << (bit % ENC_WORD_SIZE));
    }
    void reset(unsigned bit) {
        assert(bit<ENC_N_REG);
        _words[bit/ENC_WORD_SIZE] &= ~(ENC_UINT64_ONE << (bit % ENC_WORD_SIZE)); 
    }
    uint64  is_set(unsigned bit) { 
        assert(bit<ENC_N_REG);
        return _words[bit/ENC_WORD_SIZE] &   (ENC_UINT64_ONE << (bit % ENC_WORD_SIZE)); 
    }
    void set_all       () { 
        memset(_words, 0xff, ENC_ALL_REG_WORDS * (ENC_WORD_SIZE/8));
    }
    void reset_all     () {
        memset(_words, 0x00, ENC_ALL_REG_WORDS * (ENC_WORD_SIZE/8));
    }
    int  does_intersect(Enc_All_Reg_BV * bv) {
        for (int i=0; i<ENC_ALL_REG_WORDS; i++) {
            if (_words[i] & bv->_words[i])
                return true;
        }
        return false;
    }
};
  
//////////////////////////////////////////////////////////////////
//            Enumerated types
//////////////////////////////////////////////////////////////////

// memory location types

enum Encoder_Memory_Type {
 ENC_MT_unknown       =0,
 ENC_MT_field_handle  =1,
 ENC_MT_sp_offset     =2,
 ENC_MT_array_element =3,
 ENC_MT_vt_entry      =4,
 ENC_MT_switch_entry  =5,
 ENC_MT_array_entry   =6,
 ENC_MT_object_vt     =7,
 ENC_MT_vt_class      =8,
 ENC_MT_quick_thread  =9,
};

// special instructions that have exceptions for RAW and WAW orderings 

#define ENC_SI_none             0x00
#define ENC_SI_brcall           0x01
#define ENC_SI_mtbr             0x02
#define ENC_SI_icmp             0x04
#define ENC_SI_cmp_and          0x08
#define ENC_SI_cmp_or           0x10
#define ENC_SI_mtip             0x20
#define ENC_SI_start_igroup     0x40
#define ENC_SI_end_igroup       0x80

// instruction flags

#define ENC_IF_mem_access         0x01  // instr is a memory access
#define ENC_IF_possible_exc       0x02  // instr might throw exception
#define ENC_IF_target             0x04  // instr is a target
#define ENC_IF_to_be_patched      0x08  // instr needs patching
#define ENC_IF_filled             0x10  // slot is filled 
#define ENC_IF_should_stay_empty  0x20  // slot should stay empty
#define ENC_IF_instr_start        0x40  // slot is start of instr,i.e., not 2nd slot of ST_il

// target

#define ENC_NOT_A_TARGET 0xFFFFFFFFFFFFFFFF

// reference track
#define ENC_REF_dontcare   0x00
#define ENC_REF_set        '1' 
#define ENC_REF_reset      '0'

//////////////////////////////////////////////////////////////////
//                 struct Encoder_Instr_IR
//////////////////////////////////////////////////////////////////


struct Encoder_Instr_IR {
    uint64              code_image1;
    // data for register dependency checking
    union {
        uint64              fast; // bit vector for 64 registers
        Enc_All_Reg_BV *    slow; // pointer to a bit vector for all registers
    } read_regs;
    union {
        uint64              fast; // bit vector for 64 registers
        Enc_All_Reg_BV *    slow; // pointer to a bit vector for all registers
    } written_regs;

    uint16              bytecode_addr;
    EM_Syllable_Type    syl_type;
    U_8                 flags;
    U_8                 special_instr;
    U_8                 def_ref;
    Encoder_Memory_Type mem_type;
    uint64              mem_value;
    unsigned            patch_target_id;

    Encoder_Instr_IR () : code_image1(0), syl_type(ST_null),
        flags(0) {
        read_regs.fast=written_regs.fast=0;
    }

    void init(bool fast_dep_check, int slot_number) {
        code_image1=0;
        bytecode_addr=0;
        syl_type=ST_null;
        flags = 0;
        def_ref=ENC_REF_dontcare;
        if (fast_dep_check)
            read_regs.fast=written_regs.fast=0;
        special_instr=ENC_SI_none;
    }        
    int  is_filled()  { return (flags & ENC_IF_filled);}
    int  is_empty()   { return !is_filled();}
    int  should_stay_empty() { return (flags & ENC_IF_should_stay_empty);}
    int  is_instr_head() { return (flags & ENC_IF_instr_start);}
    int  can_be_filled() { 
        return !(flags & (ENC_IF_filled | ENC_IF_should_stay_empty));
    }
    void filled()      {flags |= ENC_IF_filled | ENC_IF_instr_start;}
    void filled_tail() { flags |= ENC_IF_filled;} 
    void set_should_stay_empty() {flags |= ENC_IF_should_stay_empty;}
    int  is_mem_access() { return (flags & ENC_IF_mem_access);}
    int  may_throw_exc() { return (flags & ENC_IF_possible_exc);}
    int  is_target ()    { return (flags & ENC_IF_target);}
    int  needs_patching(){ return (flags & ENC_IF_to_be_patched);}
    void set_is_mem_access() { flags |= ENC_IF_mem_access;}
    void set_may_throw_exc() { flags |= ENC_IF_possible_exc;}
    void set_is_target()     { flags |= ENC_IF_target;}
    void reset_is_target()   { flags &= ~ENC_IF_target;}
    void set_needs_patching(){ flags |= ENC_IF_to_be_patched;}
    int  is_call() { return special_instr == ENC_SI_brcall;}
    int  starts_inst_group() {return special_instr & ENC_SI_start_igroup;}
    int  ends_inst_group() {return special_instr & ENC_SI_end_igroup;}
};

struct Encoder_Unscheduled_Instr_IR : public Encoder_Instr_IR {
    uint64              code_image2;
    unsigned            target_id;
    // data for GC1 support
    unsigned            dest_ref;  // destination position in a ref def vector
    unsigned            base_pos;  // base pos if interior ptr. NOT_A_BASE if know that
                                   // it is not an interior pointer
    uint64              ref_info;  // prepared reference info
    
    void init(bool fast_dep_check, int slot_number) {
        Encoder_Instr_IR::init(fast_dep_check, slot_number);
        code_image2=0;
        target_id=unsigned(-1);
        dest_ref = 0;
    }
};    



//////////////////////////////////////////////////////////////////////////
//                    code patching
//////////////////////////////////////////////////////////////////////////

class Patch {
protected:
    Patch *const _next;     // next patch in list
    Patch(Patch *n) : _next(n) {}
public:
    virtual void apply(char *code_buffer, uint64* taret_offset_tbl) = 0;
    Patch *next()  {return _next;}
};

class Code_Patch : public Patch {
public:
    Code_Patch(Patch *n, uint64 o, unsigned s, unsigned p) :
      Patch(n), offset(o), slot(s), patch_target_id(p) {}
    virtual void apply(char *code_buffer, uint64* target_offset_tbl) = 0;
    void *operator new(size_t sz,tl::MemoryPool& m) {return m.alloc(sz);}
    void operator delete (void *p, tl::MemoryPool& m) { }

protected:
    const uint64 offset;        // offset of instruction being patched
    const int    slot;                     // slot of instruction being patched
    const unsigned patch_target_id; // id of a target that needs replacing
};

class Branch_Patch : public Code_Patch {
public:
    Branch_Patch(Patch *n, uint64 o, unsigned s, unsigned p) :
      Code_Patch(n,o,s,p) {}
    void apply(char *code_buffer, uint64* target_offset_tbl);
};

class Movl_Patch : public Code_Patch {
public:
    Movl_Patch (Patch *n, uint64 o, unsigned s, unsigned p) :
      Code_Patch(n,o,s,p) {}
    void apply(char * code_buffer, uint64 * target_offset_tbl);
};
    
class Switch_Patch : public Patch {
public:
    Switch_Patch(Patch *n, unsigned tgt_id, uint64 * addr) :
      Patch(n), switch_target_id(tgt_id), entry_addr(addr) {}
    void apply(char * code_buffer, uint64* target_offset_tbl);
    void *operator new(size_t sz, tl::MemoryPool& m) {return m.alloc(sz);}
    void operator delete (void *p, tl::MemoryPool& m) { }

protected:
    const unsigned switch_target_id; // id of a target that needs patching
    uint64 * const entry_addr;       // address of the data entry with the target
};


//////////////////////////////////////////////////////////////////////////
//                    miscelaneous structures
//////////////////////////////////////////////////////////////////////////

#define  ENC_N_WBUF_INSTR ((ENC_WBUF_LEN) * (ENC_N_SLOTS))

#define ENC_BDL_needs_stop 0x01
#define ENC_BDL_is_target  0x02

struct Encoder_Bundle_IR {
    Encoder_Instr_IR * slots;
    uint16 avail_tmplts; // a bit vector for 12 templates
    EM_Templates  tmplt_number;
    U_8      flags;
    unsigned target_id;

    void init(bool fast_reg_dep_check) {
        int i;
        assert (ENC_N_TMPLT <= 16);
        avail_tmplts = ENC_ALL_AVLB_TMPLTS;
        for (i=0;i<ENC_N_SLOTS; i++)
            slots[i].init(fast_reg_dep_check,i);
        tmplt_number=TMPLT_bad1;
        flags = ENC_BDL_needs_stop;
    }

    int is_not_empty() { 
        return (slots[0].is_filled() || slots[1].is_filled() || slots[2].is_filled());
    }
}; 

   
#endif // EMITTER_IR_H


