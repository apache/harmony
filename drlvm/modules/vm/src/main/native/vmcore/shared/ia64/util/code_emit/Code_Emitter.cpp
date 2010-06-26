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
#define LOG_DOMAIN "emitter.ipf"
#include "cxxlog.h"

#include <memory.h> 
#include "open/types.h"
#include "Code_Emitter.h"

/////////////////////////////////////////////////////////////////////////////
//               Statistics gathering
/////////////////////////////////////////////////////////////////////////////

#ifdef ENABLE_ENCODER_STATS
static unsigned n_new_bundles=0;
static unsigned n_dbl_slots=0, n_have_dbl_slots=0;
static unsigned n_st_m=0, n_st_i=0, n_st_f=0, n_st_b=0;

// compute number of double slots

void Merced_Code_Emitter::compute_n_dbl_slots() {
    bool have_dbl_slot=false;
    int n_dbl_slots_here=0, n_st_m_here=0, n_st_i_here=0, n_st_f_here=0,
        n_st_b_here=0;
    for (Bundle_IR * bdl = wbuf_first; bdl!= wbuf_last; bdl = incr_wbuf_ptr(bdl)) {
        int n_empty = 0;
        int k_full=0;
        bool branch=false;
        for (int i=0;i<ENC_N_SLOTS; i++) {
            if (bdl->slots[i].is_empty()) {
                n_empty ++;
                have_dbl_slot=true;
                }
            else
                k_full=i;
            if (bdl->slots[i].syl_type == ST_b) 
                branch=true;
        }
        assert (n_empty >=0 && n_empty < ENC_N_SLOTS);
        if (branch) {
            n_dbl_slots_here=0;
            have_dbl_slot=false;
            n_st_m_here=0;
            n_st_i_here=0;
            n_st_f_here=0;
            n_st_b_here=0;
        }
        else if (n_empty ==2) {
            n_dbl_slots_here++;
            switch(bdl->slots[k_full].syl_type) {
            case ST_m: n_st_m++; break;
            case ST_i: n_st_i++; break;
            case ST_f: n_st_f++; break;
            case ST_b: n_st_b++; break;
            }
        }
    }
    if (have_dbl_slot)
        n_have_dbl_slots++;
    n_dbl_slots+=n_dbl_slots_here;
    n_st_m+=n_st_m_here;
    n_st_i+=n_st_i_here;
    n_st_f+=n_st_f_here;
    n_st_b+=n_st_b_here;
}
#endif

/////////////////////////////////////////////////////////////////////////////
//               Mappings
/////////////////////////////////////////////////////////////////////////////

const U_8 Merced_Code_Emitter::cmp_ext_to_special_instr[cmp_last] = {
    ENC_SI_none, ENC_SI_none, ENC_SI_cmp_and, ENC_SI_cmp_or, ENC_SI_none
};

// Unavailable templates mapping:
//    Syl_Type * stop bit * instr slot # -> templates that became unavailabe
// Answers the question:
// What templates become unavailable as a result of assigning an instruction with 
// a given Syl_type to a given instr slot #;
// stop bit indicates wheather an instruction requires a stop bit in front of it.
// Each template takes one bit. Available templates are marked by 1, unavailable - by 0.

#define NO_STOP  0
#define STOP     1 

// call it eligible templates
const uint16 Merced_Code_Emitter::unavailable_tmplts[ST_last_type+1][2][ENC_N_SLOTS] = {
    // ST_null
    { {0,0,0},{0,0,0}},  // unused in encoder - error
    // ST_n
    { {0,0,0},{0,0,0}},  // unused in encoder - error
    // ST_a
    { // no stop bit
        { 0x53f7, // bbb-9
          0x1193, // mlx-2,mSmi-4,mfi-5,mbb-8,bbb-9,mfb-11
          0x0071  // miSi-1,mlx-2,mmf-6,**b-7:11
        }, 
      // stop bit
        { 0x0000, // error
          0x0020, // all but mSmi-4
          0x0002  // all but miSi-1
        }
    },
    // ST_m
    { // no stop bit
        { 0x53f7, // bbb-9 
          0x1090, // all but mmi-3,mmf-6,mmb-10
          0x0000  // error
        }, 
      // stop bit
        { 0x0000, // error
          0x0020, // all but mSmi-4
          0x0000  // error
        }
    },
    // ST_i
    { // no stop bit
        { 0x0000, // error
          0x0103, // all but mii-0, miSi-1, mib-7
          0x0071  // all but mii-0, mmi-3,mSmi-4,mfi-5
        }, 
      // stop bit
        { 0x0000, // error
          0x0000, // error
          0x0002  // all but miSi-1
        }
    },
    // ST_f
    { // no stop bit
        { 0x0000, // error
          0x4040, // all but mfi-5, mfb-11
          0x0080  // all but mmf-6 
        }, 
      // stop bit
        { 0x0000, // error
          0x0000, // error
          0x0000  // error
        }
    },
    // ST_b
    { // no stop bit
        { 0x0800, // all but bbb-9
          0x0a00, // all but mbb-8,bbb-9
          0x5b00  // all but mib,mbb,bbb,mmb,mfb - 7:11
        }, 
      // stop bit
        { 0x0000, // error
          0x0000, // error
          0x0000  // error
        }
    },
    // ST_il
    { // no stop bit
        { 0x0000, // error
          0x0004, // all but mlx-2
          0x0000  // error
        }, 
      // stop bit
        { 0x0000, // error
          0x0000, // error
          0x0000  // error
        }
    },
    // ST_bl
    { // no stop bit
        { 0x0000, // error
          0x0000, // error
          0x5b00  // all but mib,mbb,bbb,mmb,mfb - 7:11
        }, 
      // stop bit
        { 0x0000, // error
          0x0000, // error
          0x0000  // error
        }
    },
    // ST_bn
    {{0,0,0},{0,0,0}}, // unused in encoder - error
    // ST_ma
    { // no stop bit
        { 0x53f7, // bbb-9 
          0x0000, // error
          0x0000  // error
        }, 
      // stop bit
        { 0x0000, // error
          0x0000, // error
          0x0000  // error
        }
    },
    // ST_br
    {{0,0,0},{0,0,0}}, // unused in encoder - error
    // ST_fc
    {{0,0,0},{0,0,0}}, // unused in encoder - error
    // ST_is
    {{0,0,0},{0,0,0}}, // unused in encoder - error
};


///////////////////////////////////////////////////////////////////
//               Constructor
///////////////////////////////////////////////////////////////////

Merced_Code_Emitter::Merced_Code_Emitter (tl::MemoryPool & m, unsigned byteCodeSize,
                                          unsigned nTargets) :
                     mem_pool(m),
                     wbuf_first(wbuf), wbuf_last(wbuf), wbuf_end(wbuf+ENC_WBUF_LEN),
                     slot_end(slots+ENC_GL_N_SLOTS), last_empty_slot(0),
                     gl_first_empty_slot(0), 
                     n_targets(nTargets), next_instr_is_target(false),
                     needs_patching(false), curr_offset(0), patches(0),
                     ireg_map(reg_map), 
                     freg_map(reg_map+ENC_N_GEN_REG), 
                     breg_map(reg_map+ENC_N_GEN_REG+ENC_N_FLOAT_REG),
                     areg_map(reg_map+ENC_N_GEN_REG+ENC_N_FLOAT_REG+ENC_N_BRANCH_REG+ENC_N_PRED_REG), 
                     preg_map(reg_map+ENC_N_GEN_REG+ENC_N_FLOAT_REG+ENC_N_BRANCH_REG), 
                     n_fast_reg(0),
                     curr_wr_reg_vector(false), curr_rd_reg_vector(false), 
                     fast_reg_dep_check(true),
                     curr_bc_addr((unsigned)-1), curr_is_mem_access(false),
                     wbuf_is_empty(true),
                     coupled_instr_state(ENC_single_instr),
                     known_mem_type(true), exch_instr(true), 
                     encoder(&encoder0)
{
    assert(ENC_WBUF_LEN >=4);    
    // first bit of each register map entry is set to 1 to indicate that the entry is empty
    memset(reg_map,0x80,ENC_N_REG);  

    // FIXME cannot use passed memory pool 
    VERIFY_SUCCESS(apr_allocator_create(&allocator));
    arena = apr_allocator_alloc(allocator, estimate_mem_size(byteCodeSize));
    arena->next = NULL;

    // set up pointers to the slots within bundles
    Instr_IR * instr=slots;
    for (Bundle_IR * p=wbuf; p!=wbuf_end; p++,instr+=ENC_N_SLOTS) 
        p->slots=instr;

    wbuf_last->init(fast_reg_dep_check);
    reset_mem_type();
    
    // create a table of branch targets
    target_offset=(uint64 *)mem_pool.alloc(sizeof(uint64) * n_targets);
    for (unsigned i=0;i<n_targets; i++)
        target_offset[i]=ENC_NOT_A_TARGET;


#ifdef _DEBUG
    emit_after_get_code_size=false;
    target_offset_is_set = new bool[n_targets];
    // gashiman - added "i" declaration because it is required by modern C++
    for (unsigned i=0;i<n_targets;i++)
        target_offset_is_set[i]=false;
#endif
#ifdef ENC_SLOW_REG_DEP
    start_slow_dep();
#endif
    brl_patches = new MCE_brl_patch_list();
}

Merced_Code_Emitter::~Merced_Code_Emitter() {
    apr_allocator_destroy(allocator);
}

///////////////////////////////////////////////////////////////////
//               Memory allocation
///////////////////////////////////////////////////////////////////

void * Merced_Code_Emitter::_alloc_space(size_t size) {
    void * mem;
    apr_memnode_t * new_arena;

    if (size < (size_t)(arena->endp - arena->first_avail)) {
        // we have enough space
        mem = arena->first_avail;
        arena->first_avail += size;
        return mem;
    }
    new_arena = apr_allocator_alloc(allocator, size);
    // insert new arena into chain
    new_arena->next = arena;
    arena = new_arena;
    // allocate memory from new arena
    mem = arena->first_avail;
    arena->first_avail += size;
    
    return mem;
}
// Estimate the memory size.
// IA32 heuristics:
//   The estimated size needed for emitting code is 2/3 N * 12
//   which is 8 * N.
//   The reasoning is that there are approximately one 12 byte ia32 
//   instruction  per 2/3 bytes of byte code.  Also, we add 12 bytes
//   for the arena header size.
// IPF heuristics:
//   ???

void Merced_Code_Emitter::_free_arena(apr_memnode_t * node) {
    if (node->next) {
        apr_allocator_free(allocator, node->next);
        node->next = NULL;
    }
    node->first_avail = (char *)node + APR_MEMNODE_T_SIZE;
}

unsigned Merced_Code_Emitter::estimate_mem_size(unsigned byteCodeSize) {
    return (byteCodeSize * 8);
}

// Calculate the code size

unsigned Merced_Code_Emitter::get_size() {
#ifdef _DEBUG
    emit_after_get_code_size=false;
#endif
    if (!wbuf_is_empty)
        emit_all();
    unsigned size = 0;
    for (apr_memnode_t *a = arena; a != NULL; a = a->next) {
        char * mem_begin = (char *)a + APR_MEMNODE_T_SIZE;
        size += (unsigned) (a->first_avail - mem_begin);
    }
    return size;
}

// Copy one arena to a buffer

char * Merced_Code_Emitter::_copy(char *buffer, apr_memnode_t *a) {
    if (a == 0)
        return buffer;
    buffer = _copy(buffer,a->next);
    char * mem_begin = (char *)a + APR_MEMNODE_T_SIZE;
    unsigned size = (unsigned) (a->first_avail - mem_begin);
    memcpy(buffer,mem_begin,size);
    return buffer + size;
}

void Merced_Code_Emitter::copy(char *buffer) {
    assert(!emit_after_get_code_size);
    assert (coupled_instr_state == ENC_single_instr);
    if (!wbuf_is_empty)
        emit_all();

    // copy arenas starting from first one
    // arenas are organized in a LIFO order

    _copy(buffer,arena);

    // do code patching
    apply_patches(buffer,target_offset);
    apply_brl_patches(buffer);
    _free_arena(arena);
}

uint64 Merced_Code_Emitter::code_check_sum() {
    uint64 s=0;
    for (apr_memnode_t * a = arena; a != 0; a = a->next) {
        char * mem_begin = (char *)a + APR_MEMNODE_T_SIZE;
        for (char * bp = mem_begin; bp != a->first_avail; bp++) 
            s += *bp;
    }
    return s;
}


///////////////////////////////////////////////////////////////////
//        Naive code emission - one instruction per bundle
///////////////////////////////////////////////////////////////////


// Encode instruction slot by a given instruction
// Assumes that the instruction slot contains zeros

static void encode_slot(U_8* bundle, uint64 instr, int slot)
{
  // Instr is in bits 0:40, little endian.
    uint64 * u;
    switch(slot) {
    case 0:
         // Slot 0 takes bits 5:45
        u=(uint64 *)bundle;
        *u |= instr << 5;
        break;
    case 1:
         // Slot 1 takes bits 46:86, i.e. 46:63 and 0:22
        u=(uint64 *)bundle;
        *u |= instr << 46;
        u++;
        *u |= instr >> 18;
        break;
    case 2:
        // Slot 2 takes bits 87:127, i.e. 23:63
        u=(uint64 *)(bundle + 8);
        *u |= instr << 23;
        break;
    default: DIE(("Unexpected slot"));
    }
}
 
  
static inline void encode_template(U_8* bundle, EM_Templates tmplt, bool stop_bit) 
{
    *bundle |= (tmplt << 1);
    if (stop_bit)
        *bundle |= 0x1;
}

static inline void reset_bundle(U_8* bundle) {
    memset(bundle,0,IPF_INSTRUCTION_LEN);
}    

// Encode nop with 0 immediate.
// nop instruction should be contained in bits 63:23

static uint64 encode_nop_wo_imm(EM_Syllable_Type syl_type) {
    uint64 nop_code=0;
    switch (syl_type) {
    case ST_m:  
    case ST_i:
    case ST_f:
        nop_code = 0x0008000000;    
        break;
    case ST_b:
        nop_code = 0x4000000000;
        break;
    default:
        DIE(("Unexpected syllable type"));
    }
    return nop_code;
}

//////////////////////////////////////////////////////////////////////
//        Miscelaneous definitions
//////////////////////////////////////////////////////////////////////


static uint64 
    nop_i = encode_nop_wo_imm(ST_i),
    nop_m = encode_nop_wo_imm(ST_m),
    nop_f = encode_nop_wo_imm(ST_f),
    nop_b = encode_nop_wo_imm(ST_b);


static inline uint64 typed_nop(EM_Syllable_Type syl_type) 
{
    switch (syl_type) {
    case ST_m: return nop_m;
    case ST_i: return nop_i;
    case ST_b: return nop_b;
    case ST_f: return nop_f;
    default: DIE(("Unexpected syllable type"));
    }
    return 0;
}

/////////////////////////////////////////////////////////////////////////////
//              Dependency checks
/////////////////////////////////////////////////////////////////////////////

// switch to a slow (but universal) mode of the register dependency check

#ifdef ENC_SLOW_REG_DEP
void Merced_Code_Emitter::start_slow_dep() {
    fast_reg_dep_check=false;
    // place pointers to the bit vectors in the instructions
    for (Bundle_IR * p=wbuf; p!=wbuf_end; p++) {
        for (int sl_n=0; sl_n<ENC_N_SLOTS; sl_n++) {
            Instr_IR * const instr = &(p->slots[sl_n]);
            instr->written_regs.slow=new(mem_pool) Enc_All_Reg_BV(false);
            instr->read_regs.slow=new(mem_pool) Enc_All_Reg_BV(false);
        }
    }
}
#else

void Merced_Code_Emitter::switch_to_slow_reg_dep_check() {
    fast_reg_dep_check=false;

    // create an array of bit vectors
    Enc_All_Reg_BV * wr_reg_vec[ENC_N_WBUF_INSTR], * rd_reg_vec[ENC_N_WBUF_INSTR];
    int i;
    for (i=0; i<ENC_N_WBUF_INSTR; i++) {
        wr_reg_vec[i]=new(mem_pool) Enc_All_Reg_BV(false);
        rd_reg_vec[i]=new(mem_pool) Enc_All_Reg_BV(false);
    }
    
    // copy the register info from fast scheme storage to full bit vectors

    for (int r=0;r<ENC_N_REG; r++) { // for all registers
        U_8 pos = reg_map[r];
        if (!(pos & 0x80)) { // if register is fast one
            uint64 z=((uint64)0x1)<<pos;
            i=0;
            for (Bundle_IR * p=wbuf; p!=wbuf_end; p++) {
                for (int sl_n=0;sl_n<ENC_N_SLOTS; sl_n++, i++) {
                    Instr_IR * const instr=&(p->slots[sl_n]);
                    if (instr->is_filled()) {
                        if (instr->written_regs.fast & z)
                            wr_reg_vec[i]->set(r);
                        if (instr->read_regs.fast & z)
                            rd_reg_vec[i]->set(r);
                    }
                }   
            }
        }
    }

    // place pointers to the bit vectors in the instructions
    i=0;
    for (Bundle_IR * p=wbuf; p!=wbuf_end; p++) {
        for (int sl_n=0; sl_n<ENC_N_SLOTS; sl_n++, i++) {
            Instr_IR * const instr = &(p->slots[sl_n]);
            instr->written_regs.slow=wr_reg_vec[i];
            instr->read_regs.slow=rd_reg_vec[i];
        }
    }
}
#endif

// switch one instruction IR to slow register dependency check mode

void Merced_Code_Emitter::switch_ir_to_slow_reg_dep_check(Instr_IR &ir, 
                        Enc_All_Reg_BV* read_regs, Enc_All_Reg_BV* written_regs) {
    
    // copy the register info from fast scheme storage to full bit vectors

    for (int r=0;r<ENC_N_REG; r++) { // for all registers
        U_8 pos = reg_map[r];
        if (!(pos & 0x80)) { // if register is fast one
            uint64 z=((uint64)0x1)<<pos;
            if (ir.written_regs.fast & z)
                written_regs->set(r);
            if (ir.read_regs.fast & z)
                read_regs->set(r);
        }
    }
    ir.read_regs.slow=read_regs;
    ir.written_regs.slow=written_regs;
}

// check if there is register dependency between two instructions

inline static bool exists_reg_dep_fast(Instr_IR * instr1, Instr_IR * instr2) {
    assert(instr1->is_instr_head() && instr2->is_instr_head());
    return ((instr1->written_regs.fast & instr2->written_regs.fast)  ||
            (instr1->written_regs.fast & instr2->read_regs.fast) ||
            (instr1->read_regs.fast & instr2->written_regs.fast));
}

inline static bool exists_reg_dep_slow(Instr_IR * instr1, Instr_IR * instr2) {
    assert (instr1->is_instr_head() && instr2->is_instr_head());
    return (instr1->written_regs.slow->does_intersect(instr2->written_regs.slow) ||
            instr1->written_regs.slow->does_intersect(instr2->read_regs.slow) ||
            instr1->read_regs.slow->does_intersect(instr2->written_regs.slow));
}

inline static bool exists_reg_dep(Instr_IR * instr1, Instr_IR * instr2, int fast_mode) {
#ifdef ENC_SLOW_REG_DEP
    return exists_reg_dep_slow(instr1,instr2);
#else
    return (fast_mode ? exists_reg_dep_fast(instr1,instr2) :
                        exists_reg_dep_slow(instr1,instr2));
#endif
}

// check if there is w-r register dependency between instructions

inline static uint64 exists_wr_reg_dep_fast(Instr_IR * instr1, Instr_IR * instr2) {
    return (instr1->written_regs.fast & instr2->read_regs.fast);
}
inline static uint64 exists_wr_reg_dep_slow(Instr_IR * instr1, Instr_IR * instr2) {
    return instr1->written_regs.slow->does_intersect(instr2->read_regs.slow);
}
inline static uint64 exists_wr_reg_dep(Instr_IR * instr1, Instr_IR * instr2,int fast_mode) {
#ifdef ENC_SLOW_REG_DEP
    return exists_wr_reg_dep_slow(instr1,instr2);
#else
    return (fast_mode ? exists_wr_reg_dep_fast(instr1,instr2) :
                        exists_wr_reg_dep_slow(instr1,instr2));
#endif
}

// check if thre is w-w register dependency between instructions

inline static uint64 exists_ww_reg_dep_fast(Instr_IR * instr1, Instr_IR * instr2) {
    return (instr1->written_regs.fast & instr2->written_regs.fast);
}
inline static uint64 exists_ww_reg_dep_slow(Instr_IR * instr1, Instr_IR * instr2) {
    return instr1->written_regs.slow->does_intersect(instr2->written_regs.slow);
}

inline static uint64 exists_ww_reg_dep(Instr_IR * instr1, Instr_IR * instr2, int fast_mode) {
#ifdef ENC_SLOW_REG_DEP
    return exists_ww_reg_dep_slow(instr1,instr2);
#else
    return (fast_mode ? exists_ww_reg_dep_fast(instr1,instr2) :
                        exists_ww_reg_dep_slow(instr1,instr2));
#endif
}


// check if two instructions should belong to different instruction groups

inline static bool need_diff_instr_groups(Instr_IR * instr1, Instr_IR * instr2, int fast_mode) {
    assert (instr1->is_instr_head() && instr2->is_instr_head());
    // Special cases for RAW & WAW
    // 1. no branch reg RAW dependency from mt-BR to BR
    // 2. no pred.reg RAW dependency from icmp to BR
    // 3. no branch reg WAW from br.call to mt-BR
    // 4. no pred. reg WAW between compare instructions that are
    //    either all OR-type or all AND-type 

    bool dep = exists_wr_reg_dep(instr1,instr2,fast_mode) &&
             !(((instr1->special_instr  & ENC_SI_mtbr) || 
                (instr1->special_instr  & ENC_SI_icmp))
                && instr2->syl_type == ST_b);

    if (!dep)
        dep = exists_ww_reg_dep(instr1,instr2,fast_mode) &&
            !(((instr1->special_instr & ENC_SI_brcall) && 
                (instr2->special_instr & ENC_SI_mtbr))
            || (instr1->special_instr & instr2->special_instr &
                  (ENC_SI_cmp_and | ENC_SI_cmp_or))
            );
    return dep;

}


// check if one of the instructions accesses IP register

inline static bool exists_ip_dep(Instr_IR * instr1, Instr_IR * instr2) {
    assert (instr1->is_instr_head() && instr2->is_instr_head());
    return (instr1->special_instr & ENC_SI_mtip ||
            instr2->special_instr & ENC_SI_mtip);
}

// check if one of the instructions is branch

inline static bool exists_br_dep(Instr_IR * instr1, Instr_IR * instr2) {
    assert (instr1->is_instr_head() && instr2->is_instr_head());
    return (instr1->syl_type == ST_b || instr2->syl_type == ST_b);
}

// check if there is a memory dependency

inline static bool exists_mem_dep(Instr_IR * instr1, Instr_IR * instr2) {
   assert  (instr1->is_instr_head() && instr2->is_instr_head());
   return  (instr1->is_mem_access() &&  instr2->is_mem_access() &&
            instr1->mem_value == instr2->mem_value &&
            instr1->mem_type  == instr2->mem_type);
}

// check if there is an exception dependency

inline static bool exists_exception_dep(Instr_IR * instr1, Instr_IR * instr2) {
    assert (instr1->is_instr_head() && instr2->is_instr_head());
    return (instr1->may_throw_exc() || instr2->may_throw_exc());
}

inline static bool may_exchange_instr(Instr_IR * instr1, Instr_IR * instr2, int fast_mode) {
    assert (instr1->is_instr_head() && instr2->is_instr_head());
    return ( !exists_ip_dep(instr1,instr2) &&
             !exists_exception_dep(instr1,instr2) &&
             !exists_br_dep(instr1,instr2) &&
             !exists_reg_dep(instr1,instr2,fast_mode) &&
             !exists_mem_dep(instr1,instr2)  &&
             !instr1->is_target() && !instr2->is_target()
           );
}

inline static bool is_unordered_couple(Instr_IR * instr1, Instr_IR * instr2, int fast_mode) {
    assert (instr1->is_instr_head() && instr2->is_instr_head());
    return ( !exists_ip_dep(instr1,instr2) &&
             !exists_exception_dep(instr1,instr2) &&
             !exists_br_dep(instr1,instr2) &&
             !exists_reg_dep(instr1,instr2,fast_mode) &&
             !exists_mem_dep(instr1,instr2)
           );
}


/////////////////////////////////////////////////////////////////////////
//        Scheduling a single instruction
/////////////////////////////////////////////////////////////////////////

// Check if instruction fits in a slot
// Return true if success, false if instr does not fit.
// Also find out if we need a stop bit before this slot

bool Merced_Code_Emitter::instr_fits_into_slot(Bundle_IR * bundle, int slot, 
                                               Unsch_Instr_IR& instr, unsigned& need_stop) 
{
    Instr_IR * UNUSED slot_instr = &(bundle->slots[slot]);
    assert (slot >=0 && slot < ENC_N_SLOTS);
    assert (slot_instr->is_empty());
    
    // Can we place an instruction of a given type into this slot?
    unsigned u=unavailable_tmplts[instr.syl_type][NO_STOP][slot];
    if ((bundle->avail_tmplts & u) == 0) {
        return false;
    }

    // Do we need a stop before this slot?
    need_stop=0;
    for (int sl_n=0; sl_n<slot; sl_n++) {
        if (!bundle->slots[sl_n].is_empty() &&
            need_diff_instr_groups(&(bundle->slots[sl_n]),&instr, fast_reg_dep_check)) {
            need_stop = 1;
            break;
        }
    }
    if ((slot > 0) && (instr.special_instr & ENC_SI_start_igroup) != 0)
        need_stop = 1;

    // Can we place a stop bit?
    if (need_stop && 
        ((bundle->avail_tmplts & unavailable_tmplts[instr.syl_type][STOP][slot]) == 0)) {

            return false;
    }
    return true;
}

// Place instruction into a slot 

void Merced_Code_Emitter::place_instr_into_slot(Bundle_IR * bundle, int slot,
                                                Unsch_Instr_IR& instr, unsigned need_stop) 
{
    Instr_IR * slot_instr = &(bundle->slots[slot]);
    assert (slot >=0 && slot < ENC_N_SLOTS);
    assert (slot_instr->is_empty());
    assert (instr.syl_type != ST_br);

    copy_ir_into_slot(*slot_instr,instr);
    
    // mark unavailable templates

    uint16 u=unavailable_tmplts[instr.syl_type][need_stop][slot];
    assert ((bundle->avail_tmplts  & u) != 0);
    bundle->avail_tmplts &= u;
    
    // copy target information
    if (instr.is_target()) {
        assert (!(bundle->flags & ENC_BDL_is_target)); // bundle is not a target yet
        bundle->flags |= ENC_BDL_is_target;
        bundle->target_id=instr.target_id;
    }

    // handle ST_il
    if (instr.syl_type == ST_il) {
        assert (slot == 1);
        bundle->slots[2].code_image1=instr.code_image2;
        bundle->slots[2].filled_tail();
    }

    // adjust last empty slot information
    if (bundle == wbuf_last) {
        if (instr.syl_type == ST_il)
            slot++;
        if (slot >= last_empty_slot) {
            last_empty_slot=slot+1;
            if (instr.special_instr & ENC_SI_brcall) // no instr after call
                last_empty_slot = ENC_N_SLOTS;
        }
        assert(last_empty_slot <= ENC_N_SLOTS);
    }
}             


// Try to place an instruction in a current bundle
// Return true if success, false if instruction does not
// fit.

bool Merced_Code_Emitter::place_instr_into_bundle(Bundle_IR * bundle, Unsch_Instr_IR& instr) {
    int i;
    for (i=last_empty_slot; i<ENC_N_SLOTS; i++) {
        unsigned need_stop;
        if (instr_fits_into_slot(bundle,i,instr,need_stop)) {
            place_instr_into_slot(bundle,i,instr,need_stop);
            return true;
        }
    }
    return false;
}


// Schedule an instruction without reordering

void Merced_Code_Emitter::schedule_an_IR_ne (Unsch_Instr_IR& ir) {
    bool fitted=false;

    if (last_empty_slot < ENC_N_SLOTS) 
        fitted = place_instr_into_bundle(wbuf_last,ir);

    if (!fitted) {
        new_bundle();
        bool UNUSED result=place_instr_into_bundle(wbuf_last,ir);
        assert(result);
    }
}

// Schedule an instruction with reordering

void Merced_Code_Emitter::schedule_an_IR_ex (Unsch_Instr_IR& ir) {

    // Create a list of empty slots in which the instruction might be put.
    // Start the list from *all* empty slots in the last bundle

    int n_empty_slots=0;
    Instr_IR * sl;
    int stop_search=false;
#ifdef _DEBUG
    bool started_new_bundle=false;
#endif
    if (last_empty_slot == ENC_N_SLOTS) {
        new_bundle();
#ifdef _DEBUG
        started_new_bundle=true;
#endif
    }
    
    Instr_IR * gl_last_empty_slot = wbuf_last->slots + ENC_N_SLOTS - 1;
    if (!gl_first_empty_slot)
        gl_first_empty_slot=wbuf_last->slots + last_empty_slot;
    for (sl=gl_last_empty_slot; !stop_search; sl=decr_slot_ptr(sl)) {
        if (sl == gl_first_empty_slot)
            stop_search=true;
        if (sl->can_be_filled()) {
             // add to the list of empty slots
              empty_slots[n_empty_slots++]=sl;
        }
        else if (sl->is_instr_head()){
            // check if scheduling ir before this instruction is legal
            if (!may_exchange_instr(&ir,sl,fast_reg_dep_check))
                break;
            else {
            }
        }
    } // for

    // Try to fit instruction as early as possible

    int i;
    for (i=n_empty_slots-1; i>=0; i--) {
        sl=empty_slots[i];
        int gl_sl_number= (int) (sl - slots);
        Bundle_IR * wbuf_curr = wbuf + (gl_sl_number/ENC_N_SLOTS);
        int sl_n = gl_sl_number % ENC_N_SLOTS;
        unsigned need_stop;

        if (instr_fits_into_slot(wbuf_curr, sl_n,ir,need_stop)) {
            place_instr_into_slot(wbuf_curr, sl_n, ir, need_stop);
            // adjust first empty slot pointer
            if (sl == gl_first_empty_slot) 
                gl_first_empty_slot = (i > 0 ? empty_slots[i-1] : 0);
            break;
        }
    }
    
    // Instruction has been fitted if (i>=0)

    if (i<0) {
#ifdef _DEBUG
       assert(!started_new_bundle);
#endif
       new_bundle();
       bool UNUSED result=place_instr_into_bundle(wbuf_last,ir);
       assert(result);
    }
}

////////////////////////////////////////////////////////////////////////
//             Scheduling two instructions in the same bundle
////////////////////////////////////////////////////////////////////////


// Check if two instructions fit in two given slots in the same bundle
// Return true if success, false if instr does not fit.

bool Merced_Code_Emitter::instr_couple_fits_into_slots(Bundle_IR * bundle,
           unsigned slot1, unsigned slot2, Unsch_Instr_IR& instr1, Unsch_Instr_IR & instr2,
           unsigned& stop_pos) 
{
    Instr_IR * UNUSED slot_instr1 = &(bundle->slots[slot1]);
    Instr_IR * UNUSED slot_instr2 = &(bundle->slots[slot2]);
    assert (slot1 >=0 && slot1 < ENC_N_SLOTS && slot2 >=0 && slot2 < ENC_N_SLOTS);
    assert (slot_instr1->is_empty() && slot_instr2->is_empty());
    
    // Can we place instructions based on their type?
    uint16 avail_tmplts = bundle->avail_tmplts;
    uint16 u = unavailable_tmplts[instr1.syl_type][NO_STOP][slot1];
    u &= unavailable_tmplts[instr2.syl_type][NO_STOP][slot2];
    if ((avail_tmplts & u) == 0) {
        return false;
    }

    // Map instructions to slots

    int sl_n;
    Instr_IR * new_slots[ENC_N_SLOTS];
    for (sl_n=0; sl_n<ENC_N_SLOTS; sl_n++) {
        if (bundle->slots[sl_n].is_filled())
            new_slots[sl_n]=&(bundle->slots[sl_n]);
        else
            new_slots[sl_n]=0;
    }
    new_slots[slot1]=&instr1;
    new_slots[slot2]=&instr2;

    // Do we need a stop in this bundle?
    stop_pos=0; // # of slot after stop, 0 for no stop 
    for (sl_n=0; sl_n<ENC_N_SLOTS; sl_n++) if (new_slots[sl_n]) {
        int sl_n1;
        for (sl_n1=sl_n+1; sl_n1<ENC_N_SLOTS; sl_n1++) if (new_slots[sl_n1]) {
            if (need_diff_instr_groups(new_slots[sl_n],new_slots[sl_n1], fast_reg_dep_check)) {
                if (stop_pos != 0) {
                    return false;
                }
                stop_pos = sl_n1;
            break;
            }
        }
    }
    
    // Does there exist a template that accomodates both instructions with a given
    // stop position?

    unsigned need_stop1 = ((slot1 && stop_pos == slot1) ? 1 : 0);
    unsigned need_stop2 = ((slot2 && stop_pos == slot2) ? 1 : 0);
    u=unavailable_tmplts[instr1.syl_type][need_stop1][slot1];
    u &= unavailable_tmplts[instr2.syl_type][need_stop2][slot2];
    if ((avail_tmplts & u) == 0) {
#ifdef _DEBUG_ENCODEER
        printf(" - rejected based on type/slot combination\n");
#endif
        return false;
    }
    return true;
}
 
// Try to place two instructions in the bundle.
// Returns  pointer to the first used slot if fitteed, 0 otherwise
//

Instr_IR * Merced_Code_Emitter::place_instr_couple_into_bundle(Bundle_IR * bundle, 
            Unsch_Instr_IR& instr1, Unsch_Instr_IR& instr2, bool unord) {

    int i,j;
    for (i=0; i<ENC_N_SLOTS; i++) if (bundle->slots[i].can_be_filled()) {
        for (j=i+1; j<ENC_N_SLOTS; j++) if (bundle->slots[j].can_be_filled()) {
            unsigned stop_pos;
            if (instr_couple_fits_into_slots(bundle,i,j,instr1,instr2, stop_pos)) 
                place_instr_couple_into_slots(bundle,i,j,instr1,instr2,stop_pos);
            else if (unord && instr_couple_fits_into_slots(bundle, i,j, instr2, instr2, stop_pos)) 
                place_instr_couple_into_slots(bundle,i,j,instr2,instr1, stop_pos);
            return &(bundle->slots[i]);
        }
    }        
    return 0;
}



// Schedule two instructions in a bundle without reordering
void Merced_Code_Emitter::schedule_two_IR_ne(Unsch_Instr_IR& ir1, Unsch_Instr_IR& ir2, bool unord) {
    Instr_IR * fitted=0;
    if (last_empty_slot < ENC_N_SLOTS-1)  // need two spaces
        fitted = place_instr_couple_into_bundle(wbuf_last,ir1,ir2,unord);

    if (!fitted) {
        new_bundle();
        fitted=place_instr_couple_into_bundle(wbuf_last,ir1,ir2,unord);
        assert(fitted);
    }
}

// Schedule two instructions in a bundle with reordering

void Merced_Code_Emitter::schedule_two_IR_ex(Unsch_Instr_IR & ir1, Unsch_Instr_IR& ir2, bool unord) {

    // Create a list of bundles in which the pair of instruction might be put
    // Algorithm: 
    //   Scan instruction list and check whether the instructions to be scheduled
    //   can be moved past the instructions that are already in the buffer.
    //   Meanwhile, collect the list of the bundles that have two empty slots.
    //   The scan is over when we checked all the instructions upto the first empty one,
    //   or encountered an instruction that we cannot jump over.
   
    

    int n_empty_bdls=0, n_empty_slots=0;
    int stop_search=false;
#ifdef _DEBUG
    bool started_new_bundle=false;
#endif
    if (last_empty_slot == ENC_N_SLOTS) {
        new_bundle();
#ifdef _DEBUG
        started_new_bundle=true;
#endif
    }
   
    // Loop initialization
    Instr_IR * gl_last_empty_slot = wbuf_last->slots + ENC_N_SLOTS - 1;
    if (!gl_first_empty_slot)
        gl_first_empty_slot=wbuf_last->slots + last_empty_slot;
    Instr_IR * sl;
    int curr_slot=ENC_N_SLOTS - 1;
    Bundle_IR * curr_bundle = wbuf_last;
    unsigned curr_empty_slots=0;
    unsigned move_state=0;

    for (sl=gl_last_empty_slot; !stop_search; sl=decr_slot_ptr(sl)) {
        if (sl == gl_first_empty_slot)
            stop_search=true;
        if (sl->can_be_filled()) { 
            curr_empty_slots++;
            empty_slots[n_empty_slots++]=sl;
        }
        else if (sl->is_instr_head()) {
            // check if scheduling instr before this instruction is legal
            unsigned may1=may_exchange_instr(&ir1,sl,fast_reg_dep_check);
            unsigned may2=may_exchange_instr(&ir2,sl,fast_reg_dep_check);
            if (!may1 || !may2) {
                stop_search=true;
                if (may1 && !may2)
                    move_state=1;
                else if (!may1 && may2)
                    move_state=2;
            }
        } // else

        // If we just passed a bundle with 2 empty slots - add it to the list
        // Note, that it does not matter why we stopped if there are 2 empty slots,
        // the only filled one can be slot 0 and we'll try to schedule in slots 1 and 2.
        if ((curr_slot == 0 || stop_search) && curr_empty_slots >=2) {
            empty_bdls[n_empty_bdls++]=curr_bundle;
            move_state=0;
        }

        // If we stopped because we could not move one of the instructions and
        // there is one empty slot after it - add bundle to the list
        if (stop_search && curr_empty_slots == 1 && 
            (move_state==1 || (move_state==2 && unord)))
            empty_bdls[n_empty_bdls++]=curr_bundle;

        // update variables for the next iteration
        if (!stop_search) {
            if (curr_slot == 0) {
                curr_slot = ENC_N_SLOTS-1;
                curr_bundle = decr_wbuf_ptr(curr_bundle);
                curr_empty_slots=0;
            }
            else
                curr_slot--;
        }
    } // for

    // Try to fit instructions as early as possible

    int ib=n_empty_bdls-1;
    Instr_IR * first_filled_slot=0;

    // Process last bundle specially if we couldn't move one of the instructions
    
    if (move_state == 1) 
        first_filled_slot = place_instr_couple_into_bundle(empty_bdls[ib--],ir1,ir2,false);
    else if (move_state == 2) {
        assert(unord);
        first_filled_slot = place_instr_couple_into_bundle(empty_bdls[ib--],ir2,ir1,false);
    }

    while (!first_filled_slot && ib>=0) 
        first_filled_slot = place_instr_couple_into_bundle(empty_bdls[ib--],ir1,ir2,unord);
        
    if (!first_filled_slot) { // instrs did not fit so far
#ifdef _DEBUG
       assert(!started_new_bundle);
#endif
       new_bundle();
       first_filled_slot=place_instr_couple_into_bundle(wbuf_last,ir1,ir2,unord);
       assert(first_filled_slot);
    }

    // Adjust gl_first_empty_slot pointer
    if (first_filled_slot == gl_first_empty_slot) 
        gl_first_empty_slot= (n_empty_slots >= 3 ? empty_slots[n_empty_slots-3] : 0);       
}

// Schedule an instruction
void Merced_Code_Emitter::schedule_an_IR (Unsch_Instr_IR& ir) {
    static Unsch_Instr_IR prev_ir;
    static Enc_All_Reg_BV read_regs, written_regs;
    static bool prev_dep_check_fast;
#ifdef _DEBUG
    emit_after_get_code_size=true;
#endif

    switch (coupled_instr_state) {
    case ENC_single_instr:   // normal instruction
        wbuf_is_empty=false;
        if (exch_instr) 
            schedule_an_IR_ex(ir);
        else
            schedule_an_IR_ne(ir);
        break;

    case ENC_first_coupled_instr: // 1st instr in a pair
        if (!fast_reg_dep_check) {
            prev_ir.read_regs.slow=&read_regs;
            prev_ir.written_regs.slow=&written_regs;
        }
        copy_unscheduled_ir(prev_ir, ir);
        coupled_instr_state = ENC_second_coupled_instr;
        prev_dep_check_fast=fast_reg_dep_check;
        break;

    case ENC_second_coupled_instr: // 2nd instr in a pair
        wbuf_is_empty=false;
        assert (!curr_instr_couple_is_unordered || 
                is_unordered_couple(&prev_ir, &ir, fast_reg_dep_check));
        if (prev_dep_check_fast != fast_reg_dep_check) {
            assert(!fast_reg_dep_check);
            switch_ir_to_slow_reg_dep_check(prev_ir, &read_regs, &written_regs);
        }
        if (exch_instr)
            schedule_two_IR_ex(prev_ir, ir, curr_instr_couple_is_unordered);
        else
            schedule_two_IR_ne(prev_ir, ir, curr_instr_couple_is_unordered);
        coupled_instr_state = ENC_single_instr;
        break;
    default: DIE(("Unknown instruction state"));
    }
}

///////////////////////////////////////////////////////////////////////////
//                    code patching
///////////////////////////////////////////////////////////////////////////

void Branch_Patch::apply(char * code_buf, uint64* target_offset_tbl) {
    unsigned imm21=(unsigned)((target_offset_tbl[patch_target_id] - offset) >> 4);
    // 21.5.1.1 (B1) - imm_20 at 13:32, s at 36
    uint64 imm21_fmt= (0xFFFFF & imm21) | ((0x100000 & imm21)<<3);
    uint64 * u=0;
    char * b =0;
    switch (slot) {
    case 0: // 0:23 -> 18:41
        u=(uint64 *)(code_buf+offset);
        *u |= imm21_fmt << 18;
        break;
    case 1: // 0:23 -> 59:82
        b=code_buf+offset+7;
        *b |= (char)((imm21_fmt & 0x1f)<<3);
        u=(uint64 *)(code_buf+offset+8);
        *u |= imm21_fmt >> 5;
        break;
    case 2: // 0:23 -> 100:123
        u=(uint64*)(code_buf+offset+8);
        *u |= imm21_fmt <<36;
        break;
    default: DIE(("Unexpected slot"));
    }
}

void Movl_Patch::apply(char * code_buf, uint64* target_offset_tbl) {
    assert (slot == 1); // movl is always in 1st slot
    uint64 tgt=(uint64)(code_buf+target_offset_tbl[patch_target_id]);

    // Bundle layout computed from 21.8 - X2 and 21.7.2
    // Word 1:
    //  Bits          Content
    //  0-4           template
    //  5-45          slot0
    //  46-63         18 least significant bits of imm_41
    // Word 2:
    //  0-22          23 most significant bits of imm_41
    //  23-28         qp 
    //  29-35         r_1
    //  36-42         imm_7b
    //  43            v_c
    //  44            i_c
    //  45-49         imm_5c
    //  50-58         imm_9d
    //  59            i
    //  60-63         6

    
    // 64-bit word 1
    uint64* w64 = (uint64*)(code_buf+offset);
    // 18 least sign. bits of imm_41 at 46
    *w64 |= ((tgt & 0x000000ffffc00000) << 24); // << 46, >> 22

    // 64-bit word 2
    w64++;
    // 23 most significant bits of imm_41 starting at 0
    *w64 |= ((tgt & 0x7fffff0000000000) >> 40); // >>18, >> 22
    // imm_7b at 36
    *w64 |= ((tgt & 0x000000000000007f) << 36); // >>0,  <<36
    // i_c at 44
    *w64 |= ((tgt & 0x0000000000200000) << 23); // >>21, <<44
    // imm_5c at 45
    *w64 |= ((tgt & 0x00000000001f0000) << 29); // >>16, <<45
    // imm_9d at 50
    *w64 |= ((tgt & 0x000000000000ff80) << 43); // >>7,  <<50
    // i at 59
    *w64 |= ((tgt & 0x8000000000000000) >> 4);  // >>63, <<59
}

    
void Switch_Patch::apply(char * code_buffer, uint64* target_offset_tbl) {
    *entry_addr=(uint64)(code_buffer+target_offset_tbl[switch_target_id]);
}

///////////////////////////////////////////////////////////////////////////
//                  Emit code from work buffer
///////////////////////////////////////////////////////////////////////////

// find template for a given bundle

static void find_template(Bundle_IR * bundle_ir) {
    int i;
    for (i=0;i<ENC_N_TMPLT; i++) {
        if (bundle_ir->avail_tmplts & (1 << i))
            break;
        
    }
    assert (i<ENC_N_TMPLT && "No template is available");
    bundle_ir->tmplt_number=(EM_Templates)i;
}

// check for end of group between two bundles

static int does_need_stop_between_bundles(Bundle_IR * b1, Bundle_IR * b2, int b1_first_slot,
                                          int b2_n_slots, bool fast_mode) {
   

    for (int i=b1_first_slot; i<ENC_N_SLOTS; i++) {
        Instr_IR * instr1=b1->slots + i;
        if (instr1->is_instr_head()) {
            if (instr1->ends_inst_group())
                return true;
            for (int j=0; j<b2_n_slots; j++) {
                Instr_IR * instr2=b2->slots + j;
                if (instr2->is_instr_head() && 
                    (need_diff_instr_groups(instr1,instr2,fast_mode) || instr2->starts_inst_group()))
                    return true;
            }
        }
    }

    return false;
}

// prepass the bundles to be emitted:
//  - find the templates
//  - place stop bits

#define MURKA_TRACE 0

void Merced_Code_Emitter::prepass_before_emit(Bundle_IR * first, Bundle_IR * last) {
#if MURKA_TRACE
    static FILE * trace_file=0;
    static int count = 0;
    if (count == 0)
        trace_file = fopen ("__trace","w");
    count++;
#endif
    // find templates
    Bundle_IR * p;
    for (p=first; p!=last; p=incr_wbuf_ptr(p)) 
        find_template(p);
    
    // place stop bits
    Bundle_IR * p_next=0, * group_start=first, * last_processed=decr_wbuf_ptr(last);
    int stop1 = tmplt_descr[group_start->tmplt_number].stop_down;
    
    for (p=first; p!=last_processed; p=p_next) {
        p->flags &= ~ENC_BDL_needs_stop;
        p_next=incr_wbuf_ptr(p);
        int stop2=tmplt_descr[p_next->tmplt_number].stop_up;
        
        bool placed_stop=false;
        for (Bundle_IR * q=group_start; q!=p_next; q=incr_wbuf_ptr(q)) {
            if (does_need_stop_between_bundles(q,p_next,stop1,stop2,fast_reg_dep_check)) {
                p->flags |= ENC_BDL_needs_stop;
                placed_stop=true;
                break;
            }
            stop1=0;
        }
        if (placed_stop || tmplt_descr[p_next->tmplt_number].stop_down != 0) {
            group_start = p_next;
            stop1 = tmplt_descr[p_next->tmplt_number].stop_down;
        }
    }
    last_processed->flags |= ENC_BDL_needs_stop;
}

// emit bundle

void Merced_Code_Emitter::emit_bundle(Bundle_IR * bundle_ir) {

    // assert that only first non-empty slot can be a target
    assert ((!bundle_ir->slots[1].is_target()  || bundle_ir->slots[0].is_empty()) &&
            (!bundle_ir->slots[2].is_target() || 
             (bundle_ir->slots[0].is_empty() && bundle_ir->slots[1].is_empty())));
    
    // set an offset in a target table
    if (bundle_ir->flags & ENC_BDL_is_target)  {
        uint64 tgt_id=bundle_ir->target_id;
        uint64 prev_tgt_id;
        do { 
            prev_tgt_id=target_offset[tgt_id];
            target_offset[tgt_id]=curr_offset;
#ifdef _DEBUG
            target_offset_is_set[tgt_id]=true;
#endif
            tgt_id=prev_tgt_id;
        } while (tgt_id != ENC_NOT_A_TARGET);
    }
        
    // add patches for current bundle instructions to the patch list
    for (int sl_n=0; sl_n<ENC_N_SLOTS; sl_n++) {
        if (bundle_ir->slots[sl_n].needs_patching()) {
            // two kinds of patches: branches (ST_b) and movl (ST_il)
            unsigned patch_target_id=bundle_ir->slots[sl_n].patch_target_id;
            switch(bundle_ir->slots[sl_n].syl_type) {
            case ST_b:
            case ST_bl:
                patches=new(mem_pool) Branch_Patch(patches,curr_offset,sl_n,patch_target_id);
                break;
            case ST_il:
                patches=new(mem_pool) Movl_Patch(patches,curr_offset,sl_n, patch_target_id); 
                break;
            default:
                DIE(("Unexpected syllable type"));
            }
        }
    }

    // prepare space in code buffer

    curr_offset+=IPF_INSTRUCTION_LEN;

    int i;
    unsigned char * cur_bundle = (unsigned char  *) _alloc_space(IPF_INSTRUCTION_LEN);

    reset_bundle(cur_bundle);

    // set the template descriptor

    assert(bundle_ir->tmplt_number >=0 && bundle_ir->tmplt_number < ENC_N_TMPLT);
    EM_Templates tmplt_number = bundle_ir->tmplt_number;
    Template_Descr *td=&(tmplt_descr[tmplt_number]);
    
    // emit the code

    encode_template(cur_bundle,tmplt_number,bundle_ir->flags & ENC_BDL_needs_stop);
    for (i=0;i<ENC_N_SLOTS; i++) {
        Instr_IR * instr=&(bundle_ir->slots[i]);
        if (instr->is_empty())
            encode_slot(cur_bundle, typed_nop(td->syl_type[i]),i);
        else 
            encode_slot(cur_bundle, instr->code_image1,i);
    }   
}

// emit several bundles from a working buffer

void  Merced_Code_Emitter::emit_several_bundles(Bundle_IR * first, Bundle_IR * last) {
    prepass_before_emit(first,last);
    for (Bundle_IR * p=first; p!=last; p=incr_wbuf_ptr(p))
        emit_bundle(p);
}

// emit all bundles from a working buffer

void Merced_Code_Emitter::emit_all() {
    if (!wbuf_is_empty) {
        if (wbuf_last->is_not_empty())
            new_bundle_with_no_emit();
        emit_several_bundles(wbuf_first, wbuf_last);
        wbuf_first=wbuf_last=wbuf;
        wbuf_last->init(fast_reg_dep_check);
        wbuf_is_empty=true;
        gl_first_empty_slot=0;
    }
}




// move to new bundle assuming that there's space

void Merced_Code_Emitter::new_bundle_with_no_emit() {
    wbuf_last=incr_wbuf_ptr(wbuf_last);
    assert (wbuf_last != wbuf_first);
    wbuf_last->init(fast_reg_dep_check);
    last_empty_slot=0;
#ifdef ENABLE_ENCODER_STATS
    n_new_bundles++;
#endif
}

// Handle buffer overflow

void Merced_Code_Emitter::buffer_overflow() {
    Bundle_IR * last=incr_wbuf_ptr_by(wbuf_first,ENC_WBUF_LEN/2);
    emit_several_bundles(wbuf_first,last);
    wbuf_first=last;

    // If working in an exchange instr mode find first empty slot
    gl_first_empty_slot=0;
    if (exch_instr) {
        Instr_IR * gl_last_empty_slot = wbuf_last->slots + last_empty_slot;
        if (gl_last_empty_slot == slot_end) {
            assert (last_empty_slot == ENC_N_SLOTS);
            gl_last_empty_slot = wbuf->slots;
        }
        for (Instr_IR * sl=last->slots; sl != gl_last_empty_slot; sl=incr_slot_ptr(sl)) {
            if (sl->is_empty() && !sl->should_stay_empty()) {
                gl_first_empty_slot=sl;
                break;
            }
        }
    }
}


// Start a new bundle. 
// There always should be one empty bundle in wbuf.
// If our new bundle is the last one, emit half of the buffer.

void Merced_Code_Emitter::new_bundle() {
    if (incr_wbuf_ptr(incr_wbuf_ptr(wbuf_last)) == wbuf_first) 
        buffer_overflow();
    new_bundle_with_no_emit();
}

////////////////////////////////////////////////////////////////////////////////
//         class Merced_Code_Emitter_GC1
////////////////////////////////////////////////////////////////////////////////

void Merced_Code_Emitter_GC1::place_instr_into_slot(Bundle_IR * bundle, int slot,
                                                Unsch_Instr_IR& instr, unsigned need_stop)  {
    Merced_Code_Emitter::place_instr_into_slot(bundle,slot,instr,need_stop);

    // Track reference definitions
    unsigned is_call=instr.is_call();
    uint64 offset = 0;
    if (instr.def_ref != ENC_REF_dontcare || instr.is_call()) 
        offset = curr_offset + ((bundle - wbuf_first)*ENC_N_SLOTS + slot)*IPF_INSTRUCTION_LEN; 


    if (is_call) {
#ifndef _NDEBUG
        unscheduled_call=false;
#endif
        call_site_def_ref.clear_all();
        if (offset < gc_point) {
            for (unsigned i=0;i<gc_point_def_ref.get_size(); i++) {
                if (curr_cleared_refs.is_clear(i))
                    gc_point_def_ref.clear(i);
            }
        }
    }

    unsigned dest_ref=instr.dest_ref;
    if (instr.def_ref == ENC_REF_set) {
        call_site_def_ref.set(dest_ref,ENC_RI_ref);

        // check for interior pointer
        unsigned base_pos=instr.base_pos;
        if (base_pos != ENC_NOT_A_BASE && call_site_def_ref.is_ref(base_pos)) {
            call_site_def_ref.add(base_pos, ENC_RI_base);
            if (instr.ref_info & ENC_RI_copy)
                call_site_def_ref.copy(dest_ref,base_pos);
            else 
                call_site_def_ref.add(dest_ref, (instr.ref_info | (uint64)base_pos));
        }

        if (offset < gc_point) {
            assert(base_pos == ENC_NOT_A_BASE || 
                (call_site_def_ref.is_ref(base_pos) == gc_point_def_ref.is_ref(base_pos)));
            gc_point_def_ref.set(dest_ref,call_site_def_ref.get(dest_ref));
            if (gc_point_def_ref.is_intr_ptr(dest_ref))
                gc_point_def_ref.add(base_pos, ENC_RI_base);
        }
    }
    else if (instr.def_ref == ENC_REF_reset) {
        call_site_def_ref.clear(dest_ref);
        if (offset < gc_point)
            gc_point_def_ref.clear(dest_ref);
    }
}

void Merced_Code_Emitter_GC1::emit_bundle(Bundle_IR * bundle) {
    Merced_Code_Emitter::emit_bundle(bundle);

    if (curr_offset >= gc_point)
        done_upto_GC = true;
}

////////////////////////////////////////////////////////////////////////////////
//         class Merced_Code_Emitter_GC2
////////////////////////////////////////////////////////////////////////////////

Merced_Code_Emitter_GC2::~Merced_Code_Emitter_GC2() {
    apr_allocator_free(allocator, ref_bit_arena0);
}

void Merced_Code_Emitter_GC2::_alloc_ref_bit_arena(unsigned size) {
    assert(ref_bit_arena0==NULL);
    ref_bit_arena0 = ref_bit_arena = apr_allocator_alloc(allocator, size);
    n_ref_bit = 0;
}

void Merced_Code_Emitter_GC2::copy_ref_bits(char *ref_bit_buffer) {
    if (n_ref_bit) {
        assert(ref_bit_buffer);
        char *rp = ref_bit_buffer;
        for(apr_memnode_t *ap = ref_bit_arena0; ap != NULL; ap = ap->next) {
            char * mem_begin = (char *)ap + APR_MEMNODE_T_SIZE;
            for(char *p = mem_begin; p < ap->first_avail; ) {
                *rp++ = *p++;
            }
        }
    }
    _free_arena(ref_bit_arena0);
}

void Merced_Code_Emitter_GC2::emit_bundle(Bundle_IR * bundle) {
    Instr_IR * instr=bundle->slots;
    for (int i=0;i<ENC_N_SLOTS; i++,instr++) {
        if (instr->def_ref != ENC_REF_dontcare)
            encode_def_ref_bit(instr->def_ref);
    }
    Merced_Code_Emitter::emit_bundle(bundle);
}

void Merced_Code_Emitter_GC2::encode_def_ref_bit(U_8 def_ref) {
    unsigned shift = n_ref_bit++ % 8;
    if (shift==0) {
        if (ref_bit_arena->first_avail >= ref_bit_arena->endp) {
            assert(ref_bit_arena->first_avail == ref_bit_arena->endp);
            assert(!ref_bit_arena->next);
            // allocate new node of default size
            ref_bit_arena->next = apr_allocator_alloc(allocator, 0);
            ref_bit_arena = ref_bit_arena->next;
            assert(ref_bit_arena->next == NULL);
            assert(ref_bit_arena->first_avail < ref_bit_arena->endp);
        }
        *(ref_bit_arena->first_avail++) = ENC_REF_dontcare;
    }
    if (def_ref==ENC_REF_set) {
        *(ref_bit_arena->first_avail - 1) |= (1<<shift);
    } else
        assert(def_ref==ENC_REF_reset);
}



////////////////////////////////////////////////////// begin brl patching


MCE_brl_patch_list::MCE_brl_patch_list()
{
    patches = 0;
} //MCE_brl_patch_list::MCE_brl_patch_list



void MCE_brl_patch_list::add_patch(MCE_brl_patch *patch)
{
    patch->set_next(patches);
    patches = patch;
} //MCE_brl_patch_list::add_patch



MCE_brl_patch::MCE_brl_patch(uint64 br_target, uint64 br_offset)
{
    next = 0;
    target = br_target;
    offset = br_offset;
} //MCE_brl_patch::MCE_brl_patch



void MCE_brl_patch::set_next(MCE_brl_patch *next_patch)
{
    next = next_patch;
} //MCE_brl_patch::set_next



uint64 MCE_brl_patch::get_target()
{
    return target;
} //MCE_brl_patch::get_target



uint64 MCE_brl_patch::get_offset()
{
    return offset;
} //MCE_brl_patch::get_offset



MCE_brl_patch *MCE_brl_patch::get_next()
{
    return next;
} //MCE_brl_patch::get_next




#ifdef DUMP_IPF_INSTRUCTIONS

static int get_bit(uint64 *p, int bit_num)
{
    int word_num = bit_num / 64;
    int bit = bit_num % 64;
    uint64 mask = ((uint64)1) << bit;
    if(mask & p[word_num]) {
        return 1;
    } else {
        return 0;
    }
} //get_bit



static void dump_ipf_instr(void *addr)
{
    uint64 *p = (uint64 *)addr;
    for(int i = 127; i >= 0; i--) {
        printf("%d", get_bit(p, i));
    }
    printf("\n");
} //dump_ipf_instr



static void print_header()
{
    int i;
    for(i = 127; i >= 0; i--) {
        printf("%d", (i % 10));
    }
    printf("\n");
    for(i = 127; i >= 0; i--) {
        if((i % 10) == 0) {
            printf("%d", ((i / 10) % 10));
        } else {
            printf(" ");
        }
    }
    printf("\n");
    printf("                                                                                                                           ***** tmpl\n");
    printf("                                                                                  *****************************************      instr 0\n");
    printf("                                         *****************************************                                               instr 1\n");
    printf("*****************************************                                                                                        instr 2\n");
    printf("\n");
    printf("\n");
} //print_header



static void dump_bytes(U_8* addr, int num_bytes)
{
    for(int i = 0; i < num_bytes; i++) {
        printf("%2d: %2x\n", i, addr[i]);
    }
} //dump_bytes



void dump_ipf_instructions(void* addr, int num_instr)
{
    print_header();
    for(int i = 0; i < num_instr; i++) {
        dump_ipf_instr((void *)(((U_8*)addr) + (16 * i)));
    }
    dump_bytes((U_8*)addr, 16 * num_instr);
} //dump_ipf_instructions

#endif //DUMP_IPF_INSTRUCTIONS




void set_bit(uint64 *p, bool value, int bit_num)
{
    int word_num = bit_num / 64;
    int bit = bit_num % 64;
    uint64 mask = ((uint64)1) << bit;
    if(value) {
        p[word_num] |= mask;
    } else {
        p[word_num] &= ~mask;
    }
} //set_bit



// Set len bits at position pos with the value in the memory stream pointed by p.
void set_bits(uint64 *p, uint64 value, int pos, int len)
{
    for(int i = 0; i < len; i++) {
        uint64 mask = ((uint64)1) << i;
        bool bit_value = (value & mask) != 0;
        set_bit(p, bit_value, pos + i);
    }
} //set_bits



void Merced_Code_Emitter::apply_brl_patch(MCE_brl_patch *patch, char *code_buffer)
{
    uint64 target = patch->get_target();
    uint64 offset = patch->get_offset();
    U_8* curr_ip = (((U_8*)code_buffer) + offset);
#ifdef DUMP_IPF_INSTRUCTIONS
    printf("Merced_Code_Emitter::apply_brl_patch: code_buffer=%p, offset=%p (curr_ip=%p), target=%p\n",
           code_buffer, offset, curr_ip, target);
    dump_ipf_instructions(curr_ip, 1);
#endif //DUMP_IPF_INSTRUCTIONS

    uint64 imm64 = target - (uint64)curr_ip;
    uint64 temp = imm64;
    uint64 i;
    uint64 imm39;
    uint64 imm20b;
    temp >>= 4;  // ignore 4 least significant bits
    imm20b = temp & 0xfFfFf;
    temp >>= 20;
    imm39 = temp & 0x7fFFFFffff;
    temp >>= 39;
    i = temp & 1;
    temp >>= 1;
    assert(temp == 0);

    int offset_instr1 = 46;
    int offset_instr2 = 87;
    set_bits((uint64 *)curr_ip, i, offset_instr2 + 36, 1);
    set_bits((uint64 *)curr_ip, imm20b, offset_instr2 + 13, 20);
    set_bits((uint64 *)curr_ip, imm39, offset_instr1 + 2, 39);

#ifdef DUMP_IPF_INSTRUCTIONS
    printf("imm64=%p, imm20b=%p, imm39=%p, i=%p\n", imm64, imm20b, imm39, i);
    dump_ipf_instructions(curr_ip, 1);
#endif //DUMP_IPF_INSTRUCTIONS
} //Merced_Code_Emitter::apply_brl_patch



void Merced_Code_Emitter::apply_brl_patches(char *code_buffer)
{
    for(MCE_brl_patch *patch = brl_patches->get_patch_list(); patch; patch = patch->get_next()) {
        apply_brl_patch(patch, code_buffer);
    }
} //Merced_Code_Emitter::apply_brl_patches



//////////////////////////////////////////////////////// end brl patching


