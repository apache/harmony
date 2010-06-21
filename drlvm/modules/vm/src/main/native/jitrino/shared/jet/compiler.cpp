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
  * @brief Compiler class' main methods implementation.
  */
#include <assert.h>
#include <algorithm>
#ifdef WIN32
#include <malloc.h>
#else
#include <stdlib.h>
#endif

#include "open/vm_class_loading.h"
#include "open/vm.h"
#include "open/vm_properties.h"
#include "open/hythread_ext.h"
#include "open/vm_class_info.h"
#include "open/vm_type_access.h"
#include "open/vm_method_access.h"
#include "open/vm_ee.h"
#include "jit_import.h"
#include "jit_runtime_support.h"
#include "jit_intf.h"


#include "mkernel.h"
//FIXME: needed for NOPs fix only, to be removed
#include "enc_ia32.h"

#include "jet.h"

#include "compiler.h"
#include "trace.h"
#include "stats.h"
#include "port_threadunsafe.h"

#include <stack>
using std::stack;

#if !defined(PROJECT_JET)
    #include "Jitrino.h"
    #include "EMInterface.h"
    #include "JITInstanceContext.h"
#endif
/**
* A lock used to protect method's data in multi-threaded compilation.
* See VMInterface.h 
* CompilationInterface::lockMethodData/unlockMethodData for details.
*/
static Jitrino::Mutex g_compileLock;


#include "../main/Log.h"
using Jitrino::Log;
using Jitrino::LogStream;

#ifndef PLATFORM_POSIX
    #define snprintf _snprintf
#endif

namespace Jitrino { 
namespace Jet {


unsigned Compiler::defaultFlags = JMF_BBPOLLING;
unsigned Compiler::g_acceptStartID = NOTHING;
unsigned Compiler::g_acceptEndID = NOTHING;
unsigned Compiler::g_rejectStartID = NOTHING;
unsigned Compiler::g_rejectEndID = NOTHING;

/**
 * Simple counter of how much times the Compiler::compile() was called.
 */
static unsigned methodsSeen = 0;


static bool isSOEHandler(Class_Handle cls) {
    if (cls==NULL) return true; //-> finally block
    static const char* soeHandlers[] = {"java/lang/Throwable", "java/lang/Error", "java/lang/StackOverflowError", NULL};
    const char* typeName = class_get_name(cls);
    for (size_t i=0;soeHandlers[i]!=NULL; i++) {
        if (!strcmp(typeName, soeHandlers[i])) {
            return true;
        }
    }
    return false;
}


JIT_Result Compiler::compile(Compile_Handle ch, Method_Handle method,
                             const OpenMethodExecutionParams& params)
{
    compilation_params = params;

    /*
    compilation_params.exe_restore_context_after_unwind = true;
    compilation_params.exe_provide_access_to_this = true;
    //vm_properties_set_value("vm.jvmti.enabled", "true");
    g_jvmtiMode = true;
    */

    m_compileHandle = ch;
    MethInfo::init(method);

    // Currently use bp-based frame
    m_base = bp;
    // Will be used later, with sp-based frame
    m_depth = 0;

    //
    // Check contract with VM
    //
    assert(!method_is_abstract(method) && 
           "VM must not try to compile abstract method!");
    assert(!method_is_native(method) && 
           "VM must not try to compile native method!");
    
    UNSAFE_REGION_START
    // Non-atomic increment of compiled method counter.
    // May affect accuracy of JIT logging or a special debug mode 
    // when a user specifies which range of methods accept/reject from compilation.
    // Can't affect default JET execution mode.
    STATS_SET_NAME_FILER(NULL);
    m_methID = ++methodsSeen;
    UNSAFE_REGION_END
    
    unsigned compile_flags = defaultFlags;
    initProfilingData(&compile_flags);
    //
    // the latest PMF machinery seems working without much overhead,
    // let's try to have tracing functionality on by default
    //
#if 1 //def JET_PROTO 

    // Ensure no memory problems exist on entrance
    if (get_bool_arg("checkmem", false)) {
        dbg_check_mem();
    }

    const char * lp;
    //
    // Process args, update flags if necessary
    //
    if (!get_bool_arg("bbp", true)) {
        compile_flags &= ~JMF_BBPOLLING;
    }

    m_lazy_resolution  = get_bool_arg("lazyResolution", true);

#ifdef _DEBUG
    bool assertOnRecursion = get_bool_arg("assertOnRecursion", false);
    if (assertOnRecursion) {
        assert(Jitrino::getCompilationRecursionLevel() == 1);
    }
#endif


    //
    // Debugging support
    //
    lp = get_arg("log", NULL);
    if (lp != NULL) {
        if (NULL != strstr(lp, "ct")) {
            bool ct = false;
            if (NULL != strstr(lp, "sum")) {
                compile_flags |= DBG_TRACE_SUMM;
                ct = true;
            }
            static const unsigned TRACE_CG = 
                        DBG_DUMP_BBS | DBG_TRACE_CG | 
                        DBG_TRACE_SUMM | DBG_DUMP_CODE;
            if (NULL != strstr(lp, "cg")) {
                compile_flags |= TRACE_CG;
                ct = true;
            }
            if (NULL != strstr(lp, "layout")) {
                compile_flags |= DBG_TRACE_LAYOUT;
                ct = true;
            }
            if (NULL != strstr(lp, "code")) {
                compile_flags |= DBG_DUMP_CODE;
                ct = true;
            }
            if (!ct) {
                // No category means 'code+sum'
                compile_flags |= DBG_DUMP_CODE|DBG_TRACE_SUMM;
            }
        }
        
        if (Log::log_rt().isEnabled()) {
            if (NULL != strstr(lp, "rtsupp")) {
                compile_flags |= DBG_TRACE_RT;
            }
            if (NULL != strstr(lp, "ee")) {
                compile_flags |= DBG_TRACE_EE;
            }
            if (NULL != strstr(lp, "bc")) {
                compile_flags |= DBG_TRACE_BC;
            }
        }
    } // ~compLS.isEnabled()
    
    //
    // Accept or reject the method ?
    // 
    bool accept = true;
    if (g_acceptStartID != NOTHING && m_methID < g_acceptStartID) {
        accept = false;
    }
    if (g_acceptEndID != NOTHING && m_methID > g_acceptEndID) {
        accept = false;
    }
    if (g_rejectStartID != NOTHING && m_methID >= g_rejectStartID) {
        accept = false;
    }
    if (g_rejectEndID != NOTHING && m_methID <= g_rejectEndID) {
        accept = false;
    }
    lp = get_arg("reject", NULL);
    if (lp != NULL && isalpha(lp[0])) {
        accept = !to_bool(lp);
    }
    
    if (!accept) {
        if (compile_flags & DBG_TRACE_SUMM) {
            //dbg_trace_comp_start();
            //dbg_trace_comp_end(false, "Due to accept or reject argument.");
        }
        return JIT_FAILURE;
    }
    //
    // A special way to accept/reject method - list of method in file.
    // May be useful to find problematic method in multi threaded 
    // compilation env when id-s get changed much.
    //    
    static bool loadList = true;
    static vector<string> data;
    if (loadList) {
        lp = get_arg("list", NULL);
        if (lp != NULL) {
            FILE * f = fopen(lp, "r");
            if (f != NULL) {
                char buf[1024*4];
                while(NULL != fgets(buf, sizeof(buf)-1, f)) {
                    // Skip comments
                    if (buf[0] == '#') continue;
                    int len = (int)strlen(buf);
                    // Trim CRLF
                    if (len>=1 && buf[len-1]<=' ') { buf[len-1] = 0; }
                    if (len>=2 && buf[len-2]<=' ') { buf[len-2] = 0; }
                    data.push_back(buf);
                }
                fclose(f);
            } // if f != NULL
            else {
                dbg("WARN: list option - can not open '%s'.\b", lp);
            }
        }
        loadList = false;
    }
    bool found = data.size() == 0;
    for (unsigned i=0; i<data.size(); i++) {
        const string& s = data[i];
        if (!strcmp(meth_fname(), s.c_str())) {
            found = true;
            break;
        }
    }
    if (!found) {
        if (compile_flags & DBG_TRACE_SUMM) {
            //dbg_trace_comp_start();
            //dbg_trace_comp_end(false, "Not in file list.");
        }
        return JIT_FAILURE;
    }
    
    //
    // Only emulate compilation, without registering code in the VM ?
    //
    m_bEmulation = get_bool_arg("emulate", false);
    
    //
    // Check stack integrity ?
    //
    if (get_bool_arg("checkstack", false)) {
        compile_flags |= DBG_CHECK_STACK;
    }
    
    //
    // Insert software breakpoint at specified place of method ?
    //
    dbg_break_pc = NOTHING;
    lp = get_arg("brk", NULL);
    if (lp == NULL) { lp = get_arg("break", NULL); };
    if (lp != NULL) {
        // PC specified where to insert breakpoint into ...
        if (isdigit(lp[0])) {
            dbg_break_pc = atoi(lp);
        }
        else {
            // no PC specified - break at entrance.
            compile_flags |= DBG_BRK;
        }
    }
#endif  // ~JET_PROTO

    if (compile_flags & DBG_TRACE_SUMM) {
        dbg_trace_comp_start();
    }
    
    Encoder::m_trace = (compile_flags & DBG_TRACE_CG);

    if (NULL == rt_helper_throw) {
        // The very first call of ::compile(), initialize
        // runtime constants - addresses of helpers, offsets, etc
        initStatics();
    }

    m_max_native_stack_depth = 0;
    m_bc = method_get_bytecode(m_method);
    unsigned bc_size = (unsigned)method_get_bytecode_length(m_method);
    unsigned num_locals = method_get_max_locals(m_method);
    unsigned max_stack = method_get_max_stack(m_method) + NATIVE_STACK_SIZE_2_THROW_SYN_EXC;
    
    // Input arguments
    ::std::vector<jtype> inargs;
    get_args_info(m_method, inargs, &m_retType);
    m_ci.init(CCONV_MANAGED, inargs);
    unsigned num_input_slots = count_slots(inargs);
    m_argSlots = num_input_slots;
    m_argids.resize(num_input_slots); //[in_slots];
    m_ra.resize(num_locals, ar_x);
    //
    
    m_codeStream.init((unsigned)(bc_size*NATIVE_CODE_SIZE_2_BC_SIZE_RATIO));
    m_stack.init(num_locals, max_stack, num_input_slots);
    // We need to report 'this' additionally for the following cases:
    // - non-static sync methods - to allow VM to call monitor_exit() for
    //      abrupt exit
    // - constructors of classes with class_is_throwable == true
    //      to allow correct handling of stack trace in VM (see 
    //      stack_trace.cpp + com_openintel_drl_vm_VMStack.cpp:
    //      Java_com_openintel_drl_vm_VMStack_getStackState.
    //
    if (!meth_is_static() && (meth_is_sync() || meth_is_exc_ctor())) {
        compile_flags |= JMF_REPORT_THIS;
    }

    // Always report 'this' if we're asked about this explicitly
    if (compilation_params.exe_provide_access_to_this && !meth_is_static()) {
        compile_flags |= JMF_REPORT_THIS;
    }

    m_infoBlock.init(bc_size, max_stack, num_locals, num_input_slots, 
                     compile_flags);
    m_infoBlock.set_compile_params(compilation_params);
    bool eh_ok = comp_resolve_ehandlers();    

    if (!eh_ok) {
        // At least on of the exception handlers classes was not resolved:
        // unable to resolve class of Exception => will be unable to register
        // exception handlers => can't generate code et all => stop here
        // TODO - might want to [re]consider and may generate LinkageError 
        // and throw it at runtime.
        if (is_set(DBG_TRACE_SUMM)) {
            dbg_trace_comp_end(false, "ehandler.resolve");
        }
        m_infoBlock.release();
        return JIT_FAILURE;
    }
    
    //
    // Initialization done, collect statistics
    //
    UNSAFE_REGION_START
    STATS_INC(Stats::methodsCompiled, 1);
    STATS_INC(Stats::methodsWOCatchHandlers, m_handlers.size() ? 0 : 1);
    //
    STATS_MEASURE_MIN_MAX_VALUE(bc_size, m_infoBlock.get_bc_size(), meth_fname());
    STATS_MEASURE_MIN_MAX_VALUE(jstack, max_stack, meth_fname());
    STATS_MEASURE_MIN_MAX_VALUE(locals, num_locals, meth_fname());
    UNSAFE_REGION_END
    //
    // ~Stats
    //
    m_insts.alloc(bc_size, true);
    //
    // Initialization ends, 
    // Phase 1 - decoding instructions, finding basic blocks.
    //
    comp_parse_bytecode();
    comp_alloc_regs();
    // Statistics:: number of basic blocks
    STATS_MEASURE_MIN_MAX_VALUE(bbs, (unsigned)m_bbs.size(), meth_fname());

    if (is_set(DBG_DUMP_BBS)) {
        dbg_dump_bbs();
    }
    //
    // Phase 2 - code generation.
    //
    SmartPtr<BBState> allStates;
    allStates.alloc((unsigned)m_bbs.size());
    unsigned c = 0;
    for (BBMAP::iterator i=m_bbs.begin(); i != m_bbs.end(); i++, c++) {
        m_bbStates[i->first] = &allStates[c];
    }
    // Generate the whole code - will recursively generate all the 
    // reachable code, except the exception handlers ...
    comp_gen_code_bb(0);
    
    // ... now, generate exception handlers ...
    for (unsigned i=0; i<m_handlers.size(); i++) {
        comp_gen_code_bb(m_handlers[i].handler);
        if (isSOEHandler(m_handlers[i].klass)) {
            hasSOEHandlers=true;
        }
    }
    
    // ... and finally, generate prolog.
    // Fake BB data - it's used in gen_prolog()
    BBInfo bbinfo;
    unsigned prolog_ipoff = bbinfo.ipoff = ipoff();
    BBState bbstate;
    bbstate.jframe.init(m_infoBlock.get_stack_max(),
                        m_infoBlock.get_num_locals());
    m_bbstate = &bbstate;
    m_bbinfo = &bbinfo;
    m_jframe = &bbstate.jframe;
    rclear();
    gen_prolog();
    unsigned prolog_size = ipoff() - prolog_ipoff;
    if (is_set(DBG_TRACE_CG)) {
        dbg_dump_code(m_codeStream.data() + prolog_ipoff,
                      prolog_size, "prolog");
    }
    //
    // phase 2 end.
    // Register code and related info in the VM
    //
    
    // *************
    // * LOCK HERE *
    // *************
    g_compileLock.lock();

    if (method_get_code_block_size_jit(m_method, m_hjit) != 0) {
        // the code generated already
        STATS_INC(Stats::methodsCompiledSeveralTimes, 1);
        g_compileLock.unlock(); /* Unlock here */
        m_infoBlock.release();
        if (get_bool_arg("checkmem", false)) {
            dbg_check_mem();
        }
        return JIT_SUCCESS;
    }

    const unsigned total_code_size = m_codeStream.size();

    if (m_bEmulation) {
        m_vmCode = (char*)malloc(total_code_size);
    }
    else {
        m_vmCode = (char*)method_allocate_code_block(m_method, m_hjit,
                                                     total_code_size, 
                                                     16/*fixme aligment*/, 
                                                     CodeBlockHeatDefault,
                                                     0, CAA_Allocate);
        m_infoBlock.set_code_start(m_vmCode);
        m_infoBlock.set_code_len(total_code_size);
    }
    //
    // Copy and reposition code from m_codeStream into the allocated buf.
    //
    comp_layout_code(prolog_ipoff, prolog_size);
    
    STATS_MEASURE_MIN_MAX_VALUE(code_size, total_code_size, meth_fname());
    STATS_MEASURE_MIN_MAX_VALUE(native_per_bc_ratio, 
                                (m_infoBlock.get_bc_size() == 0) ? 
                                0 : total_code_size/m_infoBlock.get_bc_size(),
                                meth_fname());

#ifdef _DEBUG
    // At this point, the codeStream content is completely copied into the
    // 'codeBlock', thus no usage of m_codeStream beyond this point.
    memset(m_codeStream.data(), 0xCC, m_codeStream.size());
#endif

    //
    // runtime data. must be initialized before code patching
    //
    
    //register profiler counters  mapping info if present
    std::vector<U_32> profiler_counters_vec; //will automatically be deleted on exit from this method
    if (!m_profileCountersMap.empty()) {
        m_infoBlock.num_profiler_counters = (U_32)m_profileCountersMap.size();
        profiler_counters_vec.resize(m_infoBlock.num_profiler_counters, 0);
        m_infoBlock.profiler_counters_map = &profiler_counters_vec.front();
        for (size_t i =0; i<m_profileCountersMap.size(); i++) {
            ProfileCounterInfo& info = m_profileCountersMap[i];
            U_32 offset = ProfileCounterInfo::getInstOffset(info.offsetInfo) + (info.bb->addr - m_vmCode);
            U_32 offsetInfo = ProfileCounterInfo::createOffsetInfo(ProfileCounterInfo::getInstSize(info.offsetInfo), offset);
            m_infoBlock.profiler_counters_map[i]=offsetInfo;
        }
    }

    unsigned data_size = m_infoBlock.get_total_size();
    char * pdata;
    if (m_bEmulation) {
        pdata = (char*)malloc(data_size);
    }
    else {
        pdata = (char*)method_allocate_info_block(m_method, m_hjit, 
                                                  data_size);
    }    
    m_infoBlock.save(pdata);
    //
    // Finalize addresses
    //
    comp_patch_code();
    
    //
    // register exception handlers
    //
    if (m_bEmulation) {
        // no op
    }
    else {
        comp_set_ehandlers();
    }
    
    // ***************
    // * UNLOCK HERE *
    // ***************
    g_compileLock.unlock();
    
    STATS_MEASURE_MIN_MAX_VALUE(patchItemsToBcSizeRatioX1000, 
                                patch_count()*1000/bc_size, meth_fname());
    
    if (is_set(DBG_DUMP_CODE)) {
        dbg_dump_code_bc(m_vmCode, total_code_size);
    }
    
    if (is_set(DBG_TRACE_SUMM)) {
        dbg_trace_comp_end(true, "ok");
    }
    m_infoBlock.release();
    
    // Ensure no memory problems appeared during compilation
    if (get_bool_arg("checkmem", false)) {
        dbg_check_mem();
    }
    
    if (m_bEmulation) {
        free(m_vmCode);
        free(pdata);
        return JIT_FAILURE;
    }
    return JIT_SUCCESS;
}

BBInfo& Compiler::comp_create_bb(unsigned pc)
{
    if (m_bbs.find(pc) == m_bbs.end()) {
        BBInfo bbinfo;
        bbinfo.start = bbinfo.last_pc = bbinfo.next_bb = pc;
        m_bbs[pc] = bbinfo;
    }
    return m_bbs[pc];
}


void Compiler::comp_parse_bytecode(void)
{
    const unsigned vars = m_infoBlock.get_num_locals();
    m_defs.resize(vars, 0);
    m_uses.resize(vars, 0);
    // jretAddr here is used as 'not initialized yet' value
    m_staticTypes.resize(vars, jretAddr);
    for (unsigned i=0, var=0; i<m_ci.count(); i++, var++) {
        jtype jt = m_ci.jt(i);
        if (jt < i32) { jt = i32; }
        bool big = is_big(jt);
        m_staticTypes[var] = jt;
        m_argids[var] = m_ci.reg(i) == ar_x && !big? i : NOTHING;
        if (is_wide(jt)) {
            ++var;
            m_argids[var] = m_ci.reg(i) == ar_x && !big ? i : NOTHING;
            m_staticTypes[var] = jt;
        }
    }
    
    // PC=0 always starts new basic block
    comp_create_bb(0);
    JInst& start = m_insts[0];
    start.ref_count = 1;
    start.flags |= OPF_STARTS_BB;
    
    unsigned id = 1;
    const unsigned bc_size = m_infoBlock.get_bc_size();
    for (unsigned pc=0; pc<bc_size; ) {
        JInst& jinst = m_insts[pc];
        pc = fetch(pc, jinst);
        if (jinst.id != 0) {
            continue;
        }
        jinst.id = id++;
        if (jinst.flags & OPF_VAR_DU_MASK) {
            // extract variable index
            unsigned idx = 
                (jinst.flags&OPF_VAR_IDX_MASK) == OPF_VAR_OP0 ? 
                                jinst.op0 : jinst.flags&OPF_VAR_IDX_MASK;
            assert(idx<vars);
            // extract variable type
            jtype jt = 
                (jtype)((jinst.flags&OPF_VAR_TYPE_MASK)>>OPF_VAR_TYPE_SHIFT);
                
            // update def-use info
            if (jinst.flags & OPF_VAR_DEF) {
                ++m_defs[idx];
                if (is_big(jt)) {
                    ++m_defs[idx+1];
                }
            }
            if (jinst.flags & OPF_VAR_USE) {
                ++m_uses[idx];
                if (is_big(jt)) {
                    ++m_uses[idx+1];
                }
            }
                
            // Store a slot's 'static' type - if a slot is used only as 
            // one type (say, only i32). If the slot is used to keep various 
            // types (i.e. we have both ASTORE_1 and DSTORE_1 - then store 
            // 'jvoid' as 'static' type.
            if (m_staticTypes[idx] == jretAddr || m_staticTypes[idx] == jt) {
                m_staticTypes[idx] = jt;
                if (is_wide(jt)) {
                    jtype jt_hi = m_staticTypes[idx+1];
                    if (jt_hi == jretAddr || jt_hi == jt) {
                        m_staticTypes[idx+1] = jt;
                    }
                    else {
                        m_staticTypes[idx+1] = jvoid;
                        m_staticTypes[idx] = jvoid;
                    }
                }
            }
            else {
                m_staticTypes[idx] = jvoid;
                if (is_wide(jt)) {
                    m_staticTypes[idx+1] = jvoid;
                }
            }
        }
        if (jinst.flags & OPF_STARTS_BB) {
            comp_create_bb(jinst.pc);
        }
        //JInst& next = m_insts[pc];
        if (!(jinst.flags&OPF_DEAD_END)) {
            if (pc<bc_size) { ++m_insts[pc].ref_count; }
        }
        if (!(jinst.flags & OPF_ENDS_BB)) {
            continue;
        }
        if (pc<bc_size) {
            m_insts[pc].flags |= OPF_STARTS_BB;
            comp_create_bb(pc);
        }
        for (unsigned i=0, n = jinst.get_num_targets(); i<n; i++) {
            // mark jmp target(s)
            JInst& ji = m_insts[jinst.get_target(i)];
            ji.flags |= OPF_STARTS_BB;
            ++ji.ref_count;
            BBInfo& nbb = comp_create_bb(jinst.get_target(i));
            nbb.jsr_target = nbb.jsr_target || jinst.is_jsr();
        }
        if (jinst.is_switch()) {
            JInst& ji = m_insts[jinst.get_def_target()];
            ji.flags |= OPF_STARTS_BB;
            ++ji.ref_count;
            comp_create_bb(jinst.get_def_target());
        }
    } // ~for
    //
    // mark exception handlers as leads of basic blocks 
    for (unsigned i=0; i<m_handlers.size(); i++) {
        const HandlerInfo& hi = m_handlers[i];
        JInst& ji = m_insts[hi.handler];
        ji.flags |= OPF_STARTS_BB;
        comp_create_bb(hi.handler).ehandler = true;
        ++ji.ref_count;
        if (ji.id == 0) {
            ji.id = id++;
        }
    }
    // Sometimes, Eclipse's javac generates NOPs at the end of method AND in a basic 
    // block which is [unreachable] exception handler. In this case we have an inst which 
    // is last in the method, has fall-through, but no following block. Always set up 'OPF_DEAD_END':
    JInst& lastInst = m_insts[bc_size-1];
    lastInst.flags = lastInst.flags | OPF_DEAD_END;
}

void Compiler::comp_alloc_regs(void)
{
    if (g_jvmtiMode) {
        // Do not allocate regs. Only ensure the m_base will be saved.
        m_global_rusage.set(ar_idx(m_base));
        return;
    }
    const unsigned vars = m_infoBlock.get_num_locals();
    // 1. allocate GP registers
    for (unsigned i=0; i<g_global_grs.size(); i++) {
        AR ar = g_global_grs[i];
        assert(is_callee_save(ar));
        unsigned max_dus = 0;
        unsigned idx = NOTHING;
        for (unsigned j=0; j<vars; j++) {
            jtype jt = m_staticTypes[j];
            bool accept = jt==i32 || (jt==i64 && !is_big(i64)) || jt==jobj;
            if (!accept) continue;
            if (m_ra[j] != ar_x) continue;
            if (m_defs[j]+m_uses[j]>max_dus) {
                max_dus = m_defs[j]+m_uses[j];
                idx = j;
            }
        }
        if (idx == NOTHING) {
            // no more vars remain
            break;
        }
        m_ra[idx] = ar;
    }
    
    // 2. allocate FP registers
    for (unsigned i=0; i<g_global_frs.size(); i++) {
        AR ar = g_global_frs[i];
        unsigned max_dus = 0;
        unsigned idx = NOTHING;
        for (unsigned j=0; j<vars; j++) {
            jtype jt = m_staticTypes[j];
            if (!is_f(jt)) continue;
            if (m_ra[j] != ar_x) continue;
            if (m_defs[j]+m_uses[j]>max_dus) {
                max_dus = m_defs[j]+m_uses[j];
                idx = j;
            }
        }
        if (idx == NOTHING) {
            // no more vars remain
            break;
        }
        m_ra[idx] = ar;
    }
    // Set a bitset of globally used registers
    // m_base is always used.
    m_global_rusage.set(ar_idx(m_base));
    for (unsigned i=0; i<m_ra.size(); i++) {
        AR ar = m_ra[i];
        if (ar == ar_x) { continue; }
        m_global_rusage.set(ar_idx(ar));
    }
    
    // Print out 
    if (is_set(DBG_TRACE_CG)) {
        for (unsigned i=0; i<vars; i++) {
            jtype jt = m_staticTypes[i];
            // common info on var - type (if any), uses, defs ...
            dbg("local#%d (%s): d=%u, u=%u", 
                    i, jt < jvoid ? jtypes[jt].name : "",  
                    m_defs[i], m_uses[i]);
            // ... which input arg it came from ...
            if (i<m_argSlots && m_argids[i] != -1) {
                dbg(" <= arg#%d", m_argids[i]);
            }
            // ... which register it's assigned on ...
            if (m_ra[i] != ar_x) {
                dbg(" => %s", to_str(m_ra[i]).c_str());
            }
            dbg("\n");
        }
    }
}


/**
 * A helper structure to organize recursive compilation.
 */
struct GenItem {
    /**
     * No op.
     */
    GenItem() {}
    /**
     * @param _pc - start of basic block to generate
     * @param _parent - start of predecessor block (to inherit BBState 
     *        from) or equal to _pc if no predecessor
     * @param _jsr - PC of start of JSR subroutine the \c _pc block belongs
     *        to, or #NOTHING if the block lies outside of JSR subroutines.
     */
    GenItem(unsigned _pc, unsigned _parent, unsigned _jsr)
    {
        pc = _pc; parentPC = _parent; jsr_lead = _jsr;
    }
    /** start of basic block to generate */
    unsigned pc;
    /** start of predecessor block */
    unsigned parentPC;
    /** start of JSR subroutine the block belongs to*/
    unsigned jsr_lead;
};


void Compiler::comp_gen_code_bb(unsigned pc)
{
///////////////////////////////////////////////////////////////////////////
    /*
    Ok, here is a bit sophisticated code, but the idea is simple: we
    emulate recursion, without doing real recursive calls of method.
    
    We generate code in depth-first search order. Banal recursion is the 
    most natural, simple and clear way to do so:
        //    
        generate_all_insts_in_bb(head);
        foreach target in targets_of_last_instrution {
            generate_all_insts_in_bb(target)
        }
        if (last_inst_has_fall_through) {
            generate_all_insts_in_bb(last_inst.next)
        }
        //
    Now, let's count. The max bytecode size is 65535, including RETURN at
    the end. GOTO instruction takes 3 bytes, so the hypothetical max number 
    of basic blocks is (65'535-1(return))/3 = 21'844 which is also maximum
    recursion depth for simplest implementation. The default maximum size 
    of thread's stack is 1 MB (at least on Win32). Having all of this in 
    mind: 1024*1024/21'844 = 48 bytes we can only spend in comp_gen_code()
    for local variables, spilled regs, passed args, etc. In addition, in 
    real life there are many calls before comp_gen_code() and comp_gen_code() 
    also do some calls - they all require stack space - and we're in 
    trouble with the direct approach as we may easily exhaust stack on 
    a legal and valid bytecode.
    
    So, here is what we do to avoid this: we emulate recursion, using 
    heap-based stack structure.
    */
    
    stack<GenItem> stk;
    stk.push(GenItem(pc, pc, NOTHING));
    
    while (true) {
        if (stk.empty()) {
            break;
        }
        GenItem gi = stk.top(); stk.pop();
        unsigned head = gi.pc;
        //
        if (!comp_gen_insts(gi.pc, gi.parentPC, gi.jsr_lead)) {
            // Basic block was already seen, nothing to do.
            continue;
        }
        const BBInfo& bbi = m_bbs[head];
        const JInst& lastInst = m_insts[bbi.last_pc];
        // Push fall-through block first, so it's processed at the very end
        if (!lastInst.is_set(OPF_DEAD_END)) {
            unsigned next = lastInst.next;
            // If last inst was JSR, then when generating fall through,
            // specify the JSR leader as 'parent PC', so the BBState 
            // to inherit will be taken from the JSR's one. This also 
            // processed in a special way in comp_gen_insts() - when we see 
            // that a block's parent is JSR lead, we take the state from 
            // m_jsrStates, rather than from the m_bbStates.
            unsigned h = lastInst.is_jsr() ? lastInst.get_target(0): head;
            if (lastInst.single_suc() && !m_bbs[next].processed) {
                // If instruction has only one successor, so its BBState 
                // will be used once for inheritance. May eliminate 
                // copying and in operate directly on this BBState when 
                // generating next block.
                m_bbStates[next] = m_bbStates[head];
            }
            stk.push(GenItem(next, h, gi.jsr_lead));
        }
        // Then, process target blocks - this is match with how code gen 
        // for branches works - it expects to visit branch target first, 
        // and then fall through
        //         
        for (unsigned i=0; !lastInst.is_jsr() && 
                           i<lastInst.get_num_targets(); i++) {
            unsigned next = lastInst.get_target(i);
            if (lastInst.single_suc() && !m_bbs[next].processed) {
                m_bbStates[next] = m_bbStates[head];
            }
            stk.push(GenItem(next, head, gi.jsr_lead));
        }
        if (lastInst.is_switch()) {
            unsigned next = lastInst.get_def_target();
            stk.push(GenItem(next, head, gi.jsr_lead));
        }
        // at the very end push JSR target, so JSR subroutine get processed
        // first
        if (lastInst.is_jsr()) {
            unsigned jsr_lead = lastInst.get_target(0);
            stk.push(GenItem(jsr_lead, head, jsr_lead));
        }
        
    }
}

bool Compiler::comp_gen_insts(unsigned pc, unsigned parentPC, 
                              unsigned jsr_lead)
{
    assert(m_bbs.find(pc) != m_bbs.end());
    BBInfo& bbinfo = m_bbs[pc];
    if (bbinfo.processed) {
        if (bbinfo.jsr_target) {
            // we're processing JSR subroutine
            if (jsr_lead != NOTHING) {
                assert(jsr_lead == pc);
                // Simply load the state back to the parent's
                BBState* prevState = m_bbStates[parentPC];
//                assert(m_jsrStates.find(jsr_lead) != m_jsrStates.end());
                if(m_jsrStates.find(jsr_lead) == m_jsrStates.end()) {
                    // There can be a specific testcase with jsr without respective ret
                    // In this case we can try to continue if there is a bb_State for jsr_lead
                    // This is a temporary solution. HARMONY-4740 is devoted to the complete one.
                    assert(m_bbStates.find(jsr_lead) != m_bbStates.end());
                } else {
                    const BBState* jsrState = m_jsrStates[jsr_lead];
                    //prevState.jframe.init(&jsrState.jframe);
                    *prevState = *jsrState;
                }
            }
            else {
                // we have a fall through (and not through a JSR) path 
                // to a subroutine
                // do nothing here - we only need to return the state 
                // back for JSR
            }
        }
        return false;
    }
    BBState* pState = m_bbStates[pc];
    BBState * parentState;
    {
        const BBInfo& parentBB = m_bbs[parentPC];
        // If we see that parent block was a JSR subroutine, this may mean
        // that the parent block ended with a JSR call, and then 
        // 'parentPC' of this block was substituted (see the appropriate 
        // code in comp_gen_code_bb()).
        // So, in this case we must use the state after the JSR subroutine.
        // The 'jsr_lead != parentPC' prevents from taking state from m_jsrStates
        // when the parentPC is the real parent, that is in a JSR subroutine
        // with several blocks.
        if (parentBB.jsr_target && jsr_lead != parentPC && jsr_lead != pc) {
            // There can be a specific testcase with jsr without respective ret
            // In this case we can try to continue if there is a bb_State for parentPC
            // This is a temporary solution. HARMONY-4740 is devoted to the complete one.
//            assert(m_jsrStates.find(parentPC) != m_jsrStates.end());
            if(m_jsrStates.find(parentPC) != m_jsrStates.end()) {
                parentState = m_jsrStates[parentPC];
            } else {
                parentState = m_bbStates[parentPC];
            }
        }
        else {
            parentState = m_bbStates[parentPC];
        }
    }

    JInst& bbhead = m_insts[pc];
    if (pc != parentPC) {
        if (pState != parentState) {
            if (bbhead.ref_count > 1) {
                pState->jframe = parentState->jframe;
            }
            else {
                *pState = *parentState;
            }
        }
    }
    else {
        pState->jframe.init(m_infoBlock.get_stack_max(),
                            m_infoBlock.get_num_locals());
    }
    
    BBState& bbstate = *pState;
    bbinfo.processed = true;
    m_jframe = &bbstate.jframe;
    m_bbstate = &bbstate;
    m_bbinfo = &bbinfo;
    m_pc = pc;

    unsigned bb_ip_start = bbinfo.ipoff = m_codeStream.ipoff();
    //
    // If there are several execution paths merged on this basic block, 
    // then we can not predict many things, including, but not limited to 
    // type of of local variables, state of stack items, what stack depth 
    // was saved last, etc. So, clearing all this stuff out.
    //
    
    if (bbinfo.ehandler || bbhead.ref_count > 1) {
        bbstate.clear();
    }

    if (is_set(DBG_CHECK_STACK)){
        gen_dbg_check_bb_stack();
    }

    if (bbinfo.ehandler && pc == parentPC) {
        // This is a 'normal' handler, unreached via fall-through so far
        // (otherwise parent bb must leave stack in consistent state).
        // Here, we invoke gen_save_ret() because this is how the idea of
        // exception handlers works in DRL VM: 
        // Loosely speaking, calling 'throw_<whatever>' is like a regular 
        // function call. The only difference is that the return point is 
        // at another address, not at the next instruction - we're 
        // 'returning' to the proper exception handler.
        //
        // That's why the exception object acts like a return value - for 
        // example on IA32 it's in EAX.
        //
        SYNC_FIRST(static const CallSig cs(CCONV_MANAGED, jobj));
        gen_save_ret(cs);
    }
    
    if (is_set(DBG_TRACE_CG)) {
        dbg("\n");
        dbg(";; ======================================================\n");
        dbg(";; bb.ref.count=%d%s%s savedStackDepth=%d stackMask=0x%X %s"
            " jsr_lead=%d\n",
            bbhead.ref_count, bbinfo.ehandler ? " ehandler " : "",
            bbinfo.jsr_target ? " #JSR# " : "",
            bbstate.stack_depth, bbstate.stack_mask,
            bbstate.stack_mask_valid ? "" : "*mask.invalid*", jsr_lead);
        if (bb_ip_start != m_codeStream.ipoff()) {
            dbg_dump_code(m_codeStream.data() + bb_ip_start,
                          m_codeStream.ipoff()-bb_ip_start, "bb.head");
        }
        dbg(";; ======================================================\n");
    }
    gen_bb_enter();
#ifdef _DEBUG
        vcheck();
#endif
    
    const unsigned bc_size = m_infoBlock.get_bc_size();
    
    unsigned next_pc = bbinfo.start;
    bool last = false;
    do {
        // read out instruction to process
        m_pc = next_pc;
        m_curr_inst = &m_insts[m_pc];
        next_pc = m_insts[m_pc].next;
        last = next_pc>=bc_size || (m_insts[next_pc].is_set(OPF_STARTS_BB));
        if (last) {
            bbinfo.last_pc = m_pc;
            bbinfo.next_bb = next_pc;
        }
        unsigned inst_code_start = m_codeStream.ipoff();
        unsigned inst_code_dump_start = inst_code_start;
        
        if (is_set(DBG_TRACE_CG)) {
            dbg_dump_state("before", &bbstate);
            // print an opcode
            dbg(";; %-30s\n", toStr(m_insts[m_pc], true).c_str());
        }

        if (is_set(DBG_TRACE_BC)) {
            gen_dbg_rt(true, "//%s@%u", meth_fname(), m_pc);
            inst_code_dump_start = m_codeStream.ipoff();
        }
        
#ifdef JET_PROTO
        if (dbg_break_pc == m_pc) {
            gen_brk();
        }
#endif
        STATS_INC(Stats::opcodesSeen[m_insts[m_pc].opcode], 1);
        handle_inst();
#ifdef _DEBUG
        vcheck();
#endif
        unsigned inst_code_end = m_codeStream.ipoff();
        if (g_jvmtiMode && (inst_code_end == inst_code_dump_start)) {
            // XXX, FIXME: quick fix for JVMTI testing:
            // if bytecode did not produce any native code, then add a fake
            // NOP, so every BC instruction has its own separate native 
            // address
            ip(EncoderBase::nops(ip(), 1));
            inst_code_end = m_codeStream.ipoff();
        }
        unsigned inst_code_dump_size = inst_code_end - inst_code_dump_start;

        unsigned bb_off = inst_code_start - bb_ip_start;
        // store a native offset inside the basic block for now,
        // this will be adjusted in comp_layout_code(), by adding the BB's
        // start address
        for (unsigned i=m_pc; i<next_pc; i++) {
            m_infoBlock.set_code_info(i, (const char *)(int_ptr)bb_off);
        }
        if (m_infoBlock.get_flags() & DBG_TRACE_CG) {
            // disassemble the code
            dbg_dump_code(m_codeStream.data() + inst_code_dump_start, 
                          inst_code_dump_size, NULL);
        }
        // no one should change m_pc, it's used right after the loop to get
        // the same JInst back again
        //assert(jinst.pc == m_pc);
    } while(!last);

    bbinfo.last_pc = m_pc;
    bbinfo.next_bb = next_pc;
    
    const JInst& jinst = m_insts[m_pc];

    unsigned bb_code_end = m_codeStream.ipoff();
    bbinfo.code_size = bb_code_end - bb_ip_start;
    
    // 
    // We just finished JSR subroutine - store its state for further use
    // 
    if (jinst.opcode == OPCODE_RET) {
        assert(jsr_lead != NOTHING);
        assert(m_insts[jsr_lead].ref_count>0);
        assert(m_bbs[jsr_lead].processed);
        m_jsrStates[jsr_lead] = pState;
    }
    else if (jsr_lead != NOTHING && 
             (jinst.opcode == OPCODE_ATHROW || jinst.is_set(OPF_RETURN))) {
        assert(m_insts[jsr_lead].ref_count>0);
        assert(m_bbs[jsr_lead].processed);
        m_jsrStates[jsr_lead] = pState;
    }
    return true;
}


void Compiler::comp_layout_code(unsigned prolog_ipoff, unsigned prolog_size)
{
    char * p = m_vmCode;
    // Copy prolog
    memcpy(p, m_codeStream.data() + prolog_ipoff, prolog_size);
    p += prolog_size;
    // copy all BBs
    unsigned bc_size = m_infoBlock.get_bc_size();
    for (unsigned pc = 0; pc<bc_size; ) {
        // it's basic block lead.
        assert(m_bbs.find(pc) != m_bbs.end());
        BBInfo& bbinfo = m_bbs[pc];
        const char * pBBStart = p;
        if (bbinfo.processed) {
            // copy the code. 
            memcpy(p, m_codeStream.data()+bbinfo.ipoff, bbinfo.code_size);
            bbinfo.addr = p;
            if (m_infoBlock.get_flags() & DBG_TRACE_LAYOUT) {
                dbg("code.range: [%u - %u] => [%p - %p)\n", 
                    bbinfo.start, bbinfo.last_pc, 
                    pBBStart, pBBStart+bbinfo.code_size);
            }
        }
        else {
            if (m_infoBlock.get_flags() & DBG_TRACE_LAYOUT) {
                dbg("warning - dead code @ %d\n", pc);
            }
            assert(bbinfo.code_size == 0);
            unsigned j=pc+1;
            for (; j<bc_size; j++) {
                if (m_insts[j].flags & OPF_STARTS_BB) {
                    break;
                }
            }
            bbinfo.next_bb = j;
        }        
        // update runtime info with the mapping pc/ip data
        for (unsigned j=pc; j<bbinfo.next_bb; j++) {
            const char * inst_ip = pBBStart + 
                                (int)(int_ptr)m_infoBlock.get_code_info(j);
            m_infoBlock.set_code_info(j, inst_ip);
        }
        p = (char*)(pBBStart+bbinfo.code_size);
        pc = bbinfo.next_bb;
    }
}

bool Compiler::comp_resolve_ehandlers(void)
{
    unsigned num_handlers = method_get_exc_handler_number(m_method);
    m_handlers.resize(num_handlers);

    bool eh_ok = true;
    for (unsigned i=0; i<num_handlers; i++) {
        unsigned short regStart, regEnd, handlerStart, klassType;
        method_get_exc_handler_info(m_method, i,
                                &regStart, &regEnd, &handlerStart,
                                &klassType);
        HandlerInfo& hi = m_handlers[i];
        hi.start = regStart;
        hi.end = regEnd;
        hi.handler = handlerStart;
        hi.type = klassType;
        if (hi.type != 0) {
            hi.klass = resolve_class(m_compileHandle, m_klass, hi.type);
            eh_ok = eh_ok && (hi.klass != NULL);
        }
        if (m_infoBlock.get_flags() & DBG_DUMP_BBS) {
            dbg("handler: [%-02d,%-02d] => %-02d\n", 
                hi.start, hi.end, hi.handler);
        }
    }
    return eh_ok;
}

void Compiler::comp_set_ehandlers(void)
{
    unsigned real_num_handlers = 0;
    for (unsigned i=0, n=(unsigned)m_handlers.size(); i<n; i++) {
        HandlerInfo& hi = m_handlers[i];
        if (comp_hi_to_native(hi)) {
            ++real_num_handlers;
        }
        else {
            if (m_infoBlock.get_flags() & DBG_TRACE_LAYOUT) {
                dbg("warning: unreachable handler [%d;%d)=>%d\n", 
                    hi.start, hi.end, hi.handler);
            }
        }
    }

    method_set_num_target_handlers(m_method, m_hjit, real_num_handlers);
    
    for (unsigned i=0, handlerID=0, n=(unsigned)m_handlers.size(); i<n; i++) {
        const HandlerInfo& hi = m_handlers[i];
        if (hi.handler_ip == NULL) {
            continue;
        }
        // The last param is 'is_exception_object_dead'
        // In many cases, when an exception handler does not use an 
        // exception object, the fist bytecode instruction is 'pop' to 
        // throw the exception object away. Thus, this can used as a quick,
        // cheap but good check whether the 'exception_object_is_dead'. 
        method_set_target_handler_info(m_method, m_hjit, handlerID,
                                       hi.start_ip, hi.end_ip, hi.handler_ip, 
                                       hi.klass, 
                                       m_bc[hi.handler] == OPCODE_POP);
        
        ++handlerID;
        
        if (m_infoBlock.get_flags() & DBG_TRACE_LAYOUT) {
            dbg("native.handler: [%08X,%08X] => %08X"
                " ([%-02d,%-02d] => %-02d)\n",
                hi.start_ip, hi.end_ip, hi.handler_ip,  hi.start, hi.end, 
                hi.handler);
        }
    }
}

void Compiler::get_args_info(bool is_static, unsigned cp_idx, 
                             ::std::vector<jtype>& args, jtype * retType)
{
    const char* cpentry =
        class_cp_get_entry_descriptor(m_klass, (unsigned short)cp_idx);

    // expecting an empty vector
    assert(args.size() == 0);

    if (!is_static) {
        // all but static methods has 'this' as first argument
        args.push_back(jobj);
    }

    if (!cpentry) {
        assert(false);
        *retType = jvoid;
        return;
    }

    // skip '('
    const char *p = cpentry + 1;

    //
    // The presumption (cast of '*p' to VM_Data_Type) below is based on the 
    // VM_Data_Type values - they are equal to the appropriate characters: 
    // i.e. VM_Data_Type's long is 'J' - exactly as it's used in methods'
    // signatures
    //
    for (; *p != ')'; p++) {
        jtype jt = to_jtype((VM_Data_Type)*p);
        if (jt == jvoid) {
            break;
        }
        if (jt==jobj) {
            // skip multi-dimension array
            while(*p == '[') { p++; }
            // skip the full class name
            if (*p == 'L') {
                while(*p != ';') { p++; }
            }
        }
        args.push_back(jt);
    }
    p++;
    *retType = to_jtype((VM_Data_Type)*p);
}

void Compiler::get_args_info(Method_Handle meth, ::std::vector<jtype>& args,
                             jtype * retType)
{
    Method_Signature_Handle hsig = method_get_signature(meth);
    unsigned num_params = method_args_get_number(hsig);
    if (num_params) {
        args.resize(num_params);
    }
    for (unsigned i=0; i<num_params; i++) {
        Type_Info_Handle th = method_args_get_type_info(hsig, i);
        assert(!type_info_is_void(th));
        args[i] = to_jtype(th);
    }
    *retType = to_jtype(method_ret_type_get_type_info(hsig));
}

jtype Compiler::to_jtype(Type_Info_Handle th)
{
    if (type_info_is_void(th)) {
        return jvoid;
    }
    if (type_info_is_primitive(th)) {
        Class_Handle ch = type_info_get_class(th);
        assert(class_is_primitive(ch));
        VM_Data_Type vmdt = class_get_primitive_type_of_class(ch);
        return to_jtype(vmdt);
    }
    assert(type_info_is_reference(th) || type_info_is_vector(th));
    return jobj;
}

bool Compiler::comp_hi_to_native(HandlerInfo& hi)
{
    // Find beginning of the area protected by the handler, 
    // lookup for the first reachable instruction
    unsigned pc = hi.start;
    for (; pc < hi.end; pc++) {
        if ((hi.start_ip = (char*)m_infoBlock.get_ip(pc)) != NULL)   break;
    }

    hi.end_ip = (char*)m_infoBlock.get_ip(hi.end);
    
    // either both are NULLs, or both are not NULLs
    assert(!((hi.start_ip == NULL) ^ (hi.end_ip == NULL)));

    if (hi.start_ip != NULL) { // which also implies '&& end_ip != NULL' 
                               // - see assert() above

        // we must have the handler
        assert(m_infoBlock.get_ip(hi.handler) != NULL);

        const BBInfo& handlerBB = m_bbs[hi.handler];
        assert(handlerBB.ehandler);
        hi.handler_ip = handlerBB.addr;
    }
    else {
        hi.handler_ip = NULL;
    }
    return hi.start_ip != NULL;
}


void Compiler::comp_patch_code(void)
{
    for (void * h=enum_start(); !enum_is_end(h); enum_next(h)) {
        unsigned udata, bb, pid;
        bool done;
        enum_patch_data(&pid, &udata, &bb, &done);
        if (done) {
            continue;
        }
        unsigned inst_ipoff = pid;
        const BBInfo& bbinfo = m_bbs[bb];
        assert(bbinfo.processed);
        void * target;
        if (udata & DATA_SWITCH_TABLE) {
            assert(DATA_SWITCH_TABLE==(udata&0xFFFF0000));
            unsigned pc = udata&0xFFFF;
            assert(bbinfo.start<=pc && pc<=bbinfo.last_pc);
            const JInst& jinst = m_insts[pc];
            assert(jinst.opcode == OPCODE_TABLESWITCH);
            unsigned targets = jinst.get_num_targets();
            char * theTable;
            if (m_bEmulation) {
                theTable = (char*)malloc(targets * sizeof(void*));
            }
            else {
                theTable = (char*)method_allocate_data_block(
                                m_method, m_hjit, 
                                targets * sizeof(void*), 16);
            }
            char ** ptargets = (char**)theTable;
            // Fill out the table with targets
            for (unsigned j=0; j<targets; j++, ptargets++) {
                unsigned pc = jinst.get_target(j);
                *ptargets = (char*)m_infoBlock.get_ip(pc);
            }
            target = theTable;
            if (m_bEmulation) {
                free(theTable);
            }
        }
        else {
            target = (void*)m_infoBlock.get_ip(udata);
        }
        assert(target != NULL);
        inst_ipoff -= bbinfo.ipoff;
        void * inst_addr = bbinfo.addr + inst_ipoff;
        assert(m_infoBlock.get_pc((char*)inst_addr) < m_infoBlock.get_bc_size());
        patch(pid, inst_addr, target);
    }
}

void Compiler::initStatics(void)
{
    // must only be called once
    assert(NULL == rt_helper_throw);

    Encoder::init();
    // Fill out lists of registers for global allocation
    // ... for GP registers, always using callee-save
    for (unsigned i=0; i<gr_num; i++) {
        AR gr = _gr(i);
        if (!is_callee_save(gr)) { continue; };
        // Do not add bp 
        // TODO: need to synchronize somehow with JFM_SP_FRAME
        if (gr == bp) continue;
        g_global_grs.push_back(gr);
    }
	// On a platform with float-point args passed on registers, we need at 
	// least one scratch register in addition to the regs occupied by the args.
	// For example: when all arg regs are occupied and we need to perform mem-mem 
	// move e.g. field from operand stack into a memory stack.
	// When no float-point args are passed on regs, we need at least 3 scratch 
	// to perform computations and mem-mem moves in codegen.
	const unsigned num_of_scratch = MAX_FR_ARGS<3 ? 3 : (MAX_FR_ARGS+1);
    for (unsigned i=num_of_scratch; i<fr_num; i++) {
        AR fr = _fr(i);
        g_global_frs.push_back(fr);
    }
    
    //
    // Collect addresses of runtime helpers
    //
    rt_helper_init_class = 
                (char*)vm_helper_get_addr(VM_RT_INITIALIZE_CLASS);
    rt_helper_ldc_string =
                (char*)vm_helper_get_addr(VM_RT_LDC_STRING);
    rt_helper_new = 
     (char*)vm_helper_get_addr(VM_RT_NEW_RESOLVED_USING_VTABLE_AND_SIZE);

    g_refs_squeeze = vm_is_heap_compressed();
    g_vtbl_squeeze = vm_is_vtable_compressed();
    OBJ_BASE  = (const char*)vm_get_heap_base_address();
    VTBL_BASE = (const char*)vm_get_vtable_base_address();
    NULL_REF  = g_refs_squeeze ? OBJ_BASE : NULL;

    g_jvmtiMode = (bool)vm_property_get_boolean("vm.jvmti.enabled", false, VM_PROPERTIES);
    
    rt_helper_monitor_enter = 
                (char*)vm_helper_get_addr(VM_RT_MONITOR_ENTER);
    rt_helper_monitor_exit  = 
                (char*)vm_helper_get_addr(VM_RT_MONITOR_EXIT);

    rt_helper_class_2_jlc = 
                (char*)vm_helper_get_addr(VM_RT_CLASS_2_JLC);
    
    rt_helper_new_array = 
                (char*)vm_helper_get_addr(VM_RT_NEW_VECTOR_USING_VTABLE);
    rt_helper_aastore = (char*)vm_helper_get_addr(VM_RT_AASTORE);
    
    rt_helper_get_vtable = 
              (char*)vm_helper_get_addr(VM_RT_GET_INTERFACE_VTABLE_VER0);

    rt_helper_throw = 
                (char*)vm_helper_get_addr(VM_RT_THROW);
    rt_helper_throw_lazy = 
                (char*)vm_helper_get_addr(VM_RT_THROW_LAZY);
    rt_helper_throw_linking_exc = 
                (char*)vm_helper_get_addr(VM_RT_THROW_LINKING_EXCEPTION);

    rt_helper_checkcast = 
                (char*)vm_helper_get_addr(VM_RT_CHECKCAST);
    rt_helper_instanceof = 
                (char*)vm_helper_get_addr(VM_RT_INSTANCEOF);

    rt_helper_multinewarray = 
                (char*)vm_helper_get_addr(VM_RT_MULTIANEWARRAY_RESOLVED);

    rt_helper_gc_safepoint = 
                (char*)vm_helper_get_addr(VM_RT_GC_SAFE_POINT);
    rt_helper_get_tls_base_ptr= 
                (char*)vm_helper_get_addr(VM_RT_GC_GET_TLS_BASE);

    rt_helper_new_withresolve =
                    (char*)vm_helper_get_addr(VM_RT_NEWOBJ_WITHRESOLVE);
    rt_helper_new_array_withresolve =
        (char*)vm_helper_get_addr(VM_RT_NEWARRAY_WITHRESOLVE);
    rt_helper_get_class_withresolve =
        (char*)vm_helper_get_addr(VM_RT_INITIALIZE_CLASS_WITHRESOLVE);
    rt_helper_checkcast_withresolve =
                    (char*)vm_helper_get_addr(VM_RT_CHECKCAST_WITHRESOLVE);
    rt_helper_instanceof_withresolve =
                    (char*)vm_helper_get_addr(VM_RT_INSTANCEOF_WITHRESOLVE);
    rt_helper_field_get_offset_withresolve =
                    (char*)vm_helper_get_addr(VM_RT_GET_NONSTATIC_FIELD_OFFSET_WITHRESOLVE);
    rt_helper_field_get_address_withresolve = 
                    (char*)vm_helper_get_addr(VM_RT_GET_STATIC_FIELD_ADDR_WITHRESOLVE);
    rt_helper_get_invokevirtual_addr_withresolve = 
                (char*)vm_helper_get_addr(VM_RT_GET_INVOKEVIRTUAL_ADDR_WITHRESOLVE);
    rt_helper_get_invokespecial_addr_withresolve = 
                    (char*)vm_helper_get_addr(VM_RT_GET_INVOKE_SPECIAL_ADDR_WITHRESOLVE);
    rt_helper_get_invokestatic_addr_withresolve = 
                    (char*)vm_helper_get_addr(VM_RT_GET_INVOKESTATIC_ADDR_WITHRESOLVE);
    rt_helper_get_invokeinterface_addr_withresolve = 
                    (char*)vm_helper_get_addr(VM_RT_GET_INVOKEINTERFACE_ADDR_WITHRESOLVE);

    //
    rt_helper_ti_method_enter = 
            (char*)vm_helper_get_addr(VM_RT_JVMTI_METHOD_ENTER_CALLBACK);
    rt_helper_ti_method_exit = 
            (char*)vm_helper_get_addr(VM_RT_JVMTI_METHOD_EXIT_CALLBACK);

    rt_helper_ti_field_access =
            (char*)vm_helper_get_addr(VM_RT_JVMTI_FIELD_ACCESS_CALLBACK);
    rt_helper_ti_field_modification =
            (char*)vm_helper_get_addr(VM_RT_JVMTI_FIELD_MODIFICATION_CALLBACK);;


    //
    // Collect runtime constants
    //
    rt_array_length_offset = vector_length_offset();
    rt_suspend_req_flag_offset = (unsigned)hythread_tls_get_request_offset();
    rt_method_entry_flag_address = get_method_entry_flag_address();
    rt_method_exit_flag_address = get_method_exit_flag_address();
    rt_vtable_offset = object_get_vtable_offset();
    
    Class_Handle clss;
    clss = class_get_class_of_primitive_type(VM_DATA_TYPE_INT8);
    jtypes[i8].rt_offset = vector_first_element_offset_unboxed(clss);
    
    clss = class_get_class_of_primitive_type(VM_DATA_TYPE_INT16);
    jtypes[i16].rt_offset= vector_first_element_offset_unboxed(clss);

    clss = class_get_class_of_primitive_type(VM_DATA_TYPE_CHAR);
    jtypes[u16].rt_offset= vector_first_element_offset_unboxed(clss);

    clss = class_get_class_of_primitive_type(VM_DATA_TYPE_INT32);
    jtypes[i32].rt_offset= vector_first_element_offset_unboxed(clss);

    clss = class_get_class_of_primitive_type(VM_DATA_TYPE_INT64);
    jtypes[i64].rt_offset= vector_first_element_offset_unboxed(clss);

    clss = class_get_class_of_primitive_type(VM_DATA_TYPE_F4);
    jtypes[flt32].rt_offset= vector_first_element_offset_unboxed(clss);

    clss = class_get_class_of_primitive_type(VM_DATA_TYPE_F8);
    jtypes[dbl64].rt_offset= vector_first_element_offset_unboxed(clss);

    jtypes[jobj].rt_offset= vector_first_element_offset(VM_DATA_TYPE_CLASS);

    jtypes[jvoid].rt_offset = 0xFFFFFFFF;
}

void Compiler::initProfilingData(unsigned * pflags)
{
    m_p_methentry_counter = NULL;
    m_p_backedge_counter = NULL;
#if !defined(PROJECT_JET)
    JITInstanceContext* jitContext = JITInstanceContext::getContextForJIT(m_hjit);
    ProfilingInterface* pi = jitContext->getProfilingInterface();
    if (pi->isProfilingEnabled(ProfileType_EntryBackedge, JITProfilingRole_GEN)) {
        MemoryManager mm("jet_profiling_mm");
        MethodDesc md(m_method, m_hjit);

        g_compileLock.lock();

        EntryBackedgeMethodProfile* mp = 
            (EntryBackedgeMethodProfile*)pi->getMethodProfile(mm, 
                        ProfileType_EntryBackedge, md, JITProfilingRole_GEN);
        if (mp == NULL) {
            mp = pi->createEBMethodProfile(mm, md);
        }
        *pflags |= JMF_PROF_ENTRY_BE;
        m_p_methentry_counter = mp->getEntryCounter();
        m_p_backedge_counter = mp->getBackedgeCounter();
        if (pi->isEBProfilerInSyncMode()) {
            *pflags |= JMF_PROF_SYNC_CHECK;
            m_methentry_threshold = pi->getMethodEntryThreshold();
            m_backedge_threshold = pi->getBackedgeThreshold();
            m_profile_handle = mp->getHandle();
            m_recomp_handler_ptr = (void*)pi->getEBProfilerSyncModeCallback();
        }
        g_compileLock.unlock();
    }
#endif
}



}}; // ~namespace Jitrino::Jet


