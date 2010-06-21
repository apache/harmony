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
 * @author Alexander Astapchuk
 */
 
 /**
  * @file
  * @brief Contains platform-independent routines for runtime support.
  */
#include "open/vm_method_access.h"
#include "compiler.h"
#include "trace.h"

#include "jet.h"

#include "jit_import_rt.h"
#include "open/vm_ee.h"

#include "port_threadunsafe.h"
#include "EMInterface.h"

#if !defined(_IPF_)
#include "enc_ia32.h"
#endif

namespace Jitrino {
namespace Jet {


bool rt_check_method(JIT_Handle jit, Method_Handle method)
{
    char * pinfo = (char*)method_get_info_block_jit(method, jit);
    return MethodInfoBlock::is_valid_data(pinfo);
}

static JitFrameContext* dummyCTX = NULL;
static POINTER_SIZE_INT sp_off = (char*)devirt(sp, dummyCTX) - (char*)dummyCTX;
static POINTER_SIZE_INT ip_off = (char*)devirt(gr_x, dummyCTX) - (char*)dummyCTX;

static const POINTER_SIZE_INT bp_off = (char*)devirt(bp, dummyCTX) - (char*)dummyCTX;
static const unsigned bp_idx = ar_idx(bp);
static const unsigned bp_bytes = word_no(bp_idx)*WORD_SIZE/CHAR_BIT;
static const unsigned bp_mask  = 1<<bit_no(bp_idx);
static const int bp_spill_off = ((StackFrame*)NULL)->spill(bp);

#ifdef _IA32_
//
static AR ebx = virt(RegName_EBX);
static const unsigned ebx_idx = ar_idx(ebx);
static const unsigned ebx_bytes = word_no(ebx_idx)*WORD_SIZE/CHAR_BIT;
static const unsigned ebx_mask  = 1<<bit_no(ebx_idx);
static const int      ebx_spill_off = ((StackFrame*)NULL)->spill(ebx);
//
static AR esi = virt(RegName_ESI);
static const unsigned esi_idx = ar_idx(esi);
static const unsigned esi_bytes = word_no(esi_idx)*WORD_SIZE/CHAR_BIT;
static const unsigned esi_mask  = 1<<bit_no(esi_idx);
static const int      esi_spill_off = ((StackFrame*)NULL)->spill(esi);
//
static AR edi = virt(RegName_EDI);
static const unsigned edi_idx = ar_idx(edi);
static const unsigned edi_bytes = word_no(edi_idx)*WORD_SIZE/CHAR_BIT;
static const unsigned edi_mask  = 1<<bit_no(edi_idx);
static const int      edi_spill_off = ((StackFrame*)NULL)->spill(edi);
//
#endif // _IA32_


/**
 * @brief Prints out a message to identify a program location.
 *
 * The message includes method name and class, IP and PC of the location.
 * Message is preceded with the specified \c name.
 * The function uses #dbg and does not print out new-line character.
 */
static void rt_trace(const char * name, Method_Handle meth, 
                     const MethodInfoBlock& infoBlock,
                     const JitFrameContext * context)
{
    void *** pip = devirt(gr_x, context);
    char * ip = (char*)**pip;
    char * where = ip;
    if (context->is_ip_past) {
        --where;
    }
    dbg_rt("%s @ %s::%s @ %p/%u: ", 
        name,
        class_get_name(method_get_class(meth)), method_get_name(meth),
        ip, infoBlock.get_pc(where));
}

void rt_unwind(JIT_Handle jit, Method_Handle method, 
               JitFrameContext * context)
{
    char * pinfo = (char*)method_get_info_block_jit(method, jit);
    
    assert(MethodInfoBlock::is_valid_data(pinfo));

    MethodInfoBlock infoBlock(pinfo);
    StackFrame sframe(infoBlock.get_num_locals(),
                      infoBlock.get_stack_max(), 
                      infoBlock.get_in_slots());
                      
    JitFrameContext saveContextForLogs;
    if (infoBlock.get_flags() & DBG_TRACE_RT) {
        saveContextForLogs= *context;
    }
                      
    //void ** psp = (void**)devirt(sp, context);
    void ** psp = (void**)((char*)context + sp_off);
    char * sp_val = (char*)*psp;
    
    // here, gr_x means 'IP'
    //void *** pip = devirt(gr_x, context);
    void *** pip = (void***)((char*)context + ip_off);
    
    char * where = (char*)**pip;
    if (context->is_ip_past) {
        --where;
    }
    
    // A special processing - mostly for StackOverflowError:
    // if something terrible happens during the stack preparation sequence,
    // then the registers are not saved yet, so we can't restore them from 
    // the JitFrameContext. The good news is that the callee-save registers 
    // are also untouched yet, so we only need to restore SP and IP
    char * meth_start = infoBlock.get_code_start();
    unsigned whereLen = (unsigned)(where - meth_start);
    if (whereLen<infoBlock.get_warmup_len()) {
        *psp = sp_val + sframe.size();
        // Now, [sp] = retIP
        sp_val = (char*)*psp;
        *pip = (void**)sp_val;
        sp_val += STACK_SLOT_SIZE; // pop out the retIP
        *psp = sp_val;
        return;
    }
    
    //void *** pbp = devirt(bp, context);
    void *** pbp = (void***)((char*)context + bp_off);
    char * bp_val = (char*)(**pbp);
    
    assert(!(infoBlock.get_flags() & JMF_SP_FRAME)); // not yet 
    
    // restore sp
    sp_val = bp_val;
    // ^^^ now, sp has the same value as it was on method's entrance (points
    // to retIP)
    *pip = (void**)sp_val;
    
    //
    // restore callee-save regs
    //
#ifdef _DEBUG
    UNSAFE_REGION_START
    // presumption: only GP registers can be callee-save
    static bool do_check = true;
    for (unsigned i=0; do_check && i<ar_num; i++) {
        AR ar = _ar(i);
        assert(!is_callee_save(ar) || is_gr(ar));
    }
    do_check = false;
    UNSAFE_REGION_END
#endif

#if defined(_EM64T_) || defined(_IPF_)
    // Common version for all platforms but IA32
    for (unsigned i=0; i<gr_num; i++) {
        AR ar = _gr(i);
        if (infoBlock.is_saved(ar)) {
            void *** preg = devirt(ar, context);
            assert(preg && *preg);
            *preg = (void**)(sp_val+sframe.spill(ar));
            if (infoBlock.get_flags() & DBG_TRACE_RT) {
                dbg_rt("\trt.unwind.%s.%s: [%p]=%p\n", 
                    Encoder::to_str(ar, false).c_str(),
                    Encoder::to_str(ar, true).c_str(), *preg, **preg);
            }
        }
    }
#else
    //
    // Highly optimized version for IA32 - the loop of 4 callee-save 
    // register is unrolled, every constant value is precomputed and 
    // cached.
    //
    unsigned map_off, mask;
    int spill_off;
    //
    const char * map = infoBlock.saved_map();
    //
    //
    map_off = bp_bytes; mask = bp_mask; spill_off = bp_spill_off;
    // bp is always saved
    assert(*(unsigned*)(map+map_off) & mask);
    {
        context->p_ebp = (unsigned*)(sp_val + spill_off);
    }
    //
    //
    map_off = ebx_bytes; mask = ebx_mask; spill_off = ebx_spill_off;
    if (*(unsigned*)(map+map_off) & mask) {
        context->p_ebx = (unsigned*)(sp_val + spill_off);
    }
    //
    //
    map_off = esi_bytes; mask = esi_mask;  spill_off = esi_spill_off;
    if (*(unsigned*)(map+map_off) & mask) {
        context->p_esi = (unsigned*)(sp_val + spill_off);
    }
    //
    //
    map_off = edi_bytes; mask = edi_mask;  spill_off = edi_spill_off;
    if (*(unsigned*)(map+map_off) & mask) {
        context->p_edi = (unsigned*)(sp_val + spill_off);
    }
#endif
    // When needed, restore the whole context including scratch registers
    // (normally, under JVMTI for PopFrame functionality)
    if (infoBlock.get_compile_params().exe_restore_context_after_unwind) {
        for (unsigned i=0; i<ar_num; i++) {
            AR ar = _ar(i);
            if (is_callee_save(ar) || ar==sp) {
                continue;
            }
            void *** preg = devirt(ar, context);
            //FIXME: JVMTI-popframe // assert(preg && *preg);
            // ^^ temporarily disabling the assert, will need to uncomment
            // when VM also supports all the registers to restore
            // Currently, XMM regs are not restored as there are no proper
            // fields in JitFrameContext
            if (preg && *preg) {
                *preg = (void**)(sp_val+sframe.jvmti_register_spill_offset(ar));
                if (infoBlock.get_flags() & DBG_TRACE_RT) {
                    dbg_rt("\trt.JVMTI.unwind.%s.%s: [%p]=%p\n",
                        Encoder::to_str(ar, false).c_str(),
                        Encoder::to_str(ar, true).c_str(), *preg, **preg);
                }
            } // ~if (*preg)
        }
    }

    sp_val += STACK_SLOT_SIZE; // pop out retIP
    *psp = sp_val;

    if (infoBlock.get_flags() & DBG_TRACE_RT) {
        void *** pip = devirt(gr_x, context);
        rt_trace("rt.unwind", method, infoBlock, &saveContextForLogs);
        dbg_rt("\t=>=> %p; SP=%p\n", **pip, sp_val);
    }
}

void rt_enum(JIT_Handle jit, Method_Handle method, 
             GC_Enumeration_Handle henum, JitFrameContext * context)
{
    if (!context->is_ip_past && !StaticConsts::g_jvmtiMode) {
        // The IP points directly to the instructions - this must be a 
        // hardware NPE happened. 
        // A special case is SOE, which is allowed to happen only at the method start.
        // Check the presumptions:
        assert(method_get_exc_handler_number(method) == 0 || rt_is_soe_area(jit, method, context));
#ifdef _DEBUG
        bool sync = method_is_synchronized(method);
        bool inst = !method_is_static(method);
        assert(!(sync && inst));
#endif
        // Nothing to report here - the method is about to exit.
        return;
    }
    char * pinfo = (char*)method_get_info_block_jit(method, jit);
    assert(MethodInfoBlock::is_valid_data(pinfo));

    MethodInfoBlock infoBlock(pinfo);
    assert(!(infoBlock.get_flags() & JMF_SP_FRAME)); // not yet 
    
    void *** pip = (void***)((char*)context + ip_off);
    char * where = (char*)**pip;
    char * meth_start = infoBlock.get_code_start();
    unsigned whereLen = (unsigned)(where - meth_start);
    if (whereLen<infoBlock.get_warmup_len()) {
        return;
    }

    if (DBG_TRACE_RT & infoBlock.get_flags()) {
        rt_trace("rt.enum.start", method, infoBlock, context);
    }
    StackFrame frame(infoBlock.get_num_locals(), infoBlock.get_stack_max(), 
                  infoBlock.get_in_slots() );
    char * ebp = (char*)**devirt(bp, context);
    //
    //
    //
    unsigned * map = (unsigned*)(ebp + frame.info_gc_locals());
    for (unsigned i=0; i<infoBlock.get_num_locals(); i++) {
        if (!tst(map, i)) {
            continue;
        }
        void **p_obj = (void**)(ebp + frame.local(i));
        if (DBG_TRACE_RT & infoBlock.get_flags()) {
            //rt_trace("gc.item", method, infoBlock, context);
            dbg_rt("\tlocal#%d=>%p (%p)\n", i, p_obj, *p_obj);
        }
        vm_enumerate_root_reference(p_obj, FALSE);
    }
    //
    // Report input args with objects in it
    //    
    map = (unsigned*)(ebp + frame.info_gc_args());
    for (unsigned i=0; i<infoBlock.get_in_slots(); i++) {
        if (!tst(map, i)) {
            continue;
        }
        void **p_obj = (void**)(ebp + frame.inargs()+i*STACK_SLOT_SIZE);
        if (DBG_TRACE_RT & infoBlock.get_flags()) {
            dbg_rt("\tin_arg#%d=>%p (%p)\n", i, p_obj, *p_obj);
        }
        vm_enumerate_root_reference(p_obj, FALSE);
    }
    //
    //
    //
    map = (unsigned*)(ebp + frame.info_gc_regs());
    unsigned obj_regs = *map;
    // only one word is allocated to store a GC map for registers
    assert(gr_num < WORD_SIZE);
    for (unsigned i=0; i<WORD_SIZE; i++) {
        if (obj_regs & 1) {
            AR ar = _ar(i);
            assert(is_gr(ar) && is_callee_save(ar));
            void ** p_obj;
            p_obj = *devirt(ar, context);
            if (DBG_TRACE_RT & infoBlock.get_flags()) {
                //rt_trace("gc.item", method, infoBlock, context);
                dbg_rt("\treg#%s#%s=>%p (%p)\n", 
                Encoder::to_str(ar, false).c_str(), 
                Encoder::to_str(ar, true).c_str(),
                p_obj, *p_obj);
            }
            vm_enumerate_root_reference(p_obj, FALSE);
        }
        obj_regs >>= 1;
    }
    
    map = (unsigned*)(ebp + frame.info_gc_stack());
    unsigned rt_stack_depth = *(unsigned*)(ebp+frame.info_gc_stack_depth());
    
    for (unsigned i=0; i<rt_stack_depth; i++) {
        if (!tst(map, i)) {
            continue;
        }
        void ** p_obj;
        p_obj = (void**)(ebp + frame.stack_slot(i));
        if (DBG_TRACE_RT & infoBlock.get_flags()) {
            //rt_trace("gc.item", method, infoBlock, context);
            dbg_rt("\tstack#%d=>%p (%p)\n", i, p_obj, *p_obj);
        }
        vm_enumerate_root_reference(p_obj, FALSE);
    }

    if (infoBlock.get_flags() & JMF_REPORT_THIS) {
        void ** p_obj = (void**)rt_get_address_of_this(jit, method, context);
        if (DBG_TRACE_RT & infoBlock.get_flags()) {
            //rt_trace("gc.item", method, infoBlock, context);
            dbg_rt("\tin.this#=>%p (%p)\n", p_obj, *p_obj);
        }
        vm_enumerate_root_reference(p_obj, FALSE);
    }
    if (DBG_TRACE_RT & infoBlock.get_flags()) {
        rt_trace("rt.enum.done", method, infoBlock, context);
    }
}

void rt_fix_handler_context(JIT_Handle jit, Method_Handle method, 
                            JitFrameContext * context)
{
    char * pinfo = (char*)method_get_info_block_jit(method, jit);

    assert(MethodInfoBlock::is_valid_data(pinfo));
    //
    MethodInfoBlock infoBlock(pinfo);
    assert(!(infoBlock.get_flags() & JMF_SP_FRAME)); // not yet 
    
    void *** pip = (void***)((char*)context + ip_off);
    char * where = (char*)**pip;
    char * meth_start = infoBlock.get_code_start();

#ifdef _DEBUG
    unsigned meth_len = infoBlock.get_code_len();
    assert(meth_start <= where);
    assert(where < meth_start + meth_len);
#endif

    unsigned whereLen = (unsigned)(where - meth_start);
    if (whereLen<infoBlock.get_warmup_len()) {
        return;
    }
    
    StackFrame sframe(infoBlock.get_num_locals(),
                      infoBlock.get_stack_max(),
                      infoBlock.get_in_slots());

    unsigned frameSize = sframe.size();

    void ** psp = (void**)devirt(sp, context);
    void *** pbp = devirt(bp, context);
    char * bp_val = (char*)(**pbp);
    char * sp_val = bp_val - frameSize;
    if (infoBlock.get_flags() & DBG_TRACE_RT) {
        rt_trace("rt.fix_handler", method, infoBlock, context);
        dbg_rt("oldESP=%p ; newESP=%p\n", *psp, sp_val);
    }
    *psp = sp_val;
}

Boolean rt_is_soe_area(JIT_Handle jit, Method_Handle method, const JitFrameContext * context) {
    char * pinfo = (char*)method_get_info_block_jit(method, jit);

    assert(MethodInfoBlock::is_valid_data(pinfo));
    
    MethodInfoBlock infoBlock(pinfo);
    assert(!(infoBlock.get_flags() & JMF_SP_FRAME)); // not yet 

    void *** pip = (void***)((char*)context + ip_off);
    char * where = (char*)**pip;
    char * meth_start = infoBlock.get_code_start();
    unsigned whereLen = (unsigned)(where - meth_start);
    if (whereLen<infoBlock.get_warmup_len()) {
        return 1;
    }
    return 0;

}

void * rt_get_address_of_this(JIT_Handle jit, Method_Handle method,
                              const JitFrameContext * context)
{
    char * pinfo = (char*)method_get_info_block_jit(method, jit);

    assert(MethodInfoBlock::is_valid_data(pinfo));
    //
    MethodInfoBlock infoBlock(pinfo);
    assert(!(infoBlock.get_flags() & JMF_SP_FRAME)); // not yet 
    
    void *** pip = (void***)((char*)context + ip_off);
    char * where = (char*)**pip;
    char * meth_start = infoBlock.get_code_start();
    unsigned whereLen = (unsigned)(where - meth_start);
    if (whereLen<infoBlock.get_warmup_len()) {
        return NULL;
    }
    
    // We did not store 'this' specially
    if (!(infoBlock.get_flags() & JMF_REPORT_THIS)) {
        return NULL;
    }
    StackFrame stackframe(infoBlock.get_num_locals(),
                          infoBlock.get_stack_max(),
                          infoBlock.get_in_slots());

    void *** pbp = devirt(bp, context);
    char * bp_val = (char*)(**pbp);
    void ** p_obj = (void**)(bp_val + stackframe.thiz());
    
    if (infoBlock.get_flags() & DBG_TRACE_RT) {
        void ** psp = (void**)devirt(sp, context);
        char * sp_val = (char*)*psp;
        rt_trace("rt.get_thiz", method, infoBlock, context);
        dbg_rt("p_thiz=%p, thiz=%p (sp=%p)\n", p_obj, p_obj ? *p_obj : NULL, sp_val);
    }
    return p_obj;
}

void rt_bc2native(JIT_Handle jit, Method_Handle method, unsigned short bc_pc,
                  void ** pip)
{
    char * pinfo = (char*)method_get_info_block_jit(method, jit);
    assert(MethodInfoBlock::is_valid_data(pinfo));

    MethodInfoBlock rtinfo(pinfo);
    assert(bc_pc < rtinfo.get_bc_size());
    *pip = (void*)rtinfo.get_ip(bc_pc); 
    if (rtinfo.get_flags() & DBG_TRACE_RT) {
        dbg_rt("rt.bc2ip: @ %u => %p\n", bc_pc, *pip);
    }
}

void rt_native2bc(JIT_Handle jit, Method_Handle method, const void * ip,
                  unsigned short * pbc_pc)
{
    char * pinfo = (char*)method_get_info_block_jit(method, jit);
    assert(MethodInfoBlock::is_valid_data(pinfo));

    MethodInfoBlock rtinfo(pinfo);
#ifdef _DEBUG
    char * where = (char*)ip;
    char * meth_start = rtinfo.get_code_start();
    unsigned meth_len = rtinfo.get_code_len();
    assert(meth_start <= where);
    assert(where < meth_start + meth_len);
#endif

    *pbc_pc = (unsigned short)rtinfo.get_pc((char*)ip);

    if (rtinfo.get_flags() & DBG_TRACE_RT) {
        dbg_rt("rt.ip2bc: @ 0x%p => %d\n", ip, *pbc_pc);
    }
}

::OpenExeJpdaError rt_get_local_var(JIT_Handle jit, Method_Handle method,
                                    const ::JitFrameContext *context,
                                    unsigned var_num, VM_Data_Type var_type,
                                    void *value_ptr)
{
    //
    //FIXME:
    // Current implementation will work incorrectly with local variables
    // cached on registers and with variables that reuse input args slots.
    //
    // Need to store regs=>locals into MethodInfo block and also  
    // locals=>input args (or invent a simple algorithm of how to detect
    // this mapping during runtime).
    //
           
    char * pinfo = (char*)method_get_info_block_jit(method, jit);
    assert(MethodInfoBlock::is_valid_data(pinfo));
    MethodInfoBlock infoBlock(pinfo);
    if (var_num >= infoBlock.get_num_locals()) {
        return EXE_ERROR_INVALID_SLOT;
    }
    StackFrame frame(infoBlock.get_num_locals(),
                     infoBlock.get_stack_max(),
                     infoBlock.get_in_slots());

    char * ebp = (char*)**devirt(bp, context);
    unsigned * map = (unsigned*)(ebp + frame.info_gc_locals());

    uint64* var_ptr_to_64;
    U_32* var_ptr_to32;

    switch(var_type) {
    case VM_DATA_TYPE_INT64:
    case VM_DATA_TYPE_UINT64:
    case VM_DATA_TYPE_F8:
        var_ptr_to_64 = (uint64*)value_ptr;
        *var_ptr_to_64 = *(uint64*)(ebp + frame.local(var_num));
        break;
    case VM_DATA_TYPE_ARRAY:
    case VM_DATA_TYPE_CLASS:
        if (!tst(map, var_num)) {
            return EXE_ERROR_TYPE_MISMATCH;
        }
#ifdef _EM64T_
    case VM_DATA_TYPE_STRING:
        var_ptr_to_64 = (uint64*)value_ptr;
        *var_ptr_to_64 = *(uint64*)(ebp + frame.local(var_num));
        break;
#endif
    default:
        var_ptr_to32 = (U_32*)value_ptr;
        *var_ptr_to32 = *(U_32*)(ebp + frame.local(var_num));
    }
    return EXE_ERROR_NONE;
}

::OpenExeJpdaError rt_set_local_var(JIT_Handle jit, Method_Handle method,
                                    const ::JitFrameContext *context,
                                    unsigned var_num, VM_Data_Type var_type,
                                    void *value_ptr)
{
    char * pinfo = (char*)method_get_info_block_jit(method, jit);
    assert(MethodInfoBlock::is_valid_data(pinfo));
    MethodInfoBlock infoBlock(pinfo);
    if (var_num >= infoBlock.get_num_locals()) {
        return EXE_ERROR_INVALID_SLOT;
    }
    StackFrame frame(infoBlock.get_num_locals(),
                     infoBlock.get_stack_max(),
                     infoBlock.get_in_slots());


    char * ebp = (char*)**devirt(bp, context);
    uint64* var_ptr_to_64;
    U_32* var_ptr_to32;
    
    switch(var_type) {
    case VM_DATA_TYPE_INT64:
    case VM_DATA_TYPE_UINT64:
    case VM_DATA_TYPE_F8:
#ifdef _EM64T_
    case VM_DATA_TYPE_ARRAY:
    case VM_DATA_TYPE_CLASS:
    case VM_DATA_TYPE_STRING:
#endif
        var_ptr_to_64 = (uint64*)(ebp + frame.local(var_num));
        *var_ptr_to_64 = *(uint64*)value_ptr;
        break;
    default:
        var_ptr_to32 = (U_32*)(ebp + frame.local(var_num));
        *var_ptr_to32 = *(U_32*)value_ptr;
    }

    return EXE_ERROR_NONE;
}


void rt_profile_notification_callback(JIT_Handle jit, PC_Handle pch, Method_Handle mh) 
{
   JITInstanceContext* jitContext = JITInstanceContext::getContextForJIT(jit);

   //heck that profiler type is EB, counters are patched only for Entry Backage profile
   if ((jitContext->getProfilingInterface())->getProfileType(pch) == EM_PCTYPE_ENTRY_BACKEDGE) {
       //Get MethodInfoBlock of the method to be patched
       char * pinfo = (char*)method_get_info_block_jit(mh, jit);
       assert(MethodInfoBlock::is_valid_data(pinfo));
       MethodInfoBlock infoBlock(pinfo);

       if (infoBlock.get_flags() & DBG_TRACE_RT) {
           const char* methodName = method_get_name(mh);
           const char* className = class_get_name(method_get_class(mh));
           dbg_rt("rt.patch_eb_counters: patching method %s.%s\n", className, methodName);
       }

       //Replace counters with nops in 3 steps:
       //1. Atomically replace first 2 bytes of counter instruction with jump to the 
       //next instruction
       //2. Replace all the remaining bytes of counter instruction with nops
       //3. Atomically replace jump with 2 nops
       U_8* methodAddr = method_get_code_block_jit(mh, jit);
       for (U_32 i = 0 ; i<infoBlock.num_profiler_counters; i++) {
           U_32 offsetInfo = infoBlock.profiler_counters_map[i];
           U_32 codeOffset = ProfileCounterInfo::getInstOffset(offsetInfo);
           U_32 patchedSize = ProfileCounterInfo::getInstSize(offsetInfo);

           U_8* patchedAddr = methodAddr + codeOffset;
           //1. Generate jump to the next instruction
           char* jmpIP = (char*)patchedAddr;
           char jmpOffset = (char)(patchedSize - 2);
           if (((POINTER_SIZE_INT)jmpIP)&0x1) {
               jmpIP++;
               jmpOffset--;
           }

           //to guarantee that first replacing will be atomic we have to 
           //manually write jmp instruction instead of using encoder
           *(uint16*)jmpIP = (uint16)((0xeb) | jmpOffset << 8);
           assert(((uint16)(int_ptr)(jmpIP) & 0x01)==0);

           //2. Put nops instead of inst, 1 byte nops are used here for asm 
           //readability
           char* ip = jmpIP + 2;
           for (int i=0; i<jmpOffset; i++) {
               EncoderBase::nops(ip+i, 1);
           }

           //3. Atomically replace jump with nop;
           *(uint16*)jmpIP = (uint16)0x9090; //2 nops atomically.
       }
      infoBlock.release();
   }
}

}};    // ~namespace Jitrino::Jet

