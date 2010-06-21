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
 * @brief Implementation of tracing and logging utilities.
 */
#include "trace.h"
#include "compiler.h"

#include "../shared/mkernel.h"
#include "open/vm_class_manipulation.h"
#include "open/vm_ee.h"
#include "jit_intf.h"

#ifdef _WIN32
    #include <windows.h>
    #define snprintf    _snprintf
#else
    #include <dlfcn.h>
#endif // defined(_WIN32)

#include <stdio.h>
#include <stdarg.h>
#include "enc.h"
#include "Log.h"


namespace Jitrino
{
namespace Jet
{

unsigned g_tbreak_id = NOTHING;


/**
 * @defgroup LWDIS Disassembling routine for Jitrino.JET
 *
 * Jitrino.JET does not include disassembler. Instead, it may call 
 * external disassembling routine provided via exported function in 
 * dynamic library.
 *
 * To add such possibility, the library must export \e stdcall (where 
 * applicable) routine with the given signature.
 * 
 * The library must be named lwdis (which is lwdis.dll on Windows and 
 * <b>lib</b>lwdis.so on Linux). The library must be accessible via regular
 * library loading routines. On Windows it's normally PATH, System32 or 
 * application main module's folder. On Linux it's LD_LIBRARY_PATH.
 * 
 * Also, on Linux an attempt will be made to load the library from the 
 * application main module's folder first (regardless of LD_LIBRARY_PATH).
 *
 * lwdis stands for <b>l</b>ight-<b>w</b>eight <b>dis</b>assembler.
 * 
 * @see DISFUNC
 * @{
 * @}
 */
/**
 * @brief Disassembling function prototype.
 * 
 * @param[in] kode - code buffer to disassemble
 * @param[out] buf - buffer to write disassembled string into
 * @param buf_len - max length of the \c buf
 * @return length of disassembled instruction, or 0 to indicate error.
 * @see LWDIS
 */
typedef unsigned (*DISFUNC)(const char *kode, char *buf, unsigned buf_len);


/**
 * @brief Returns directory of currently running module, ended with file 
 *        separator.
 *
 * The function returns path to a directory, where the currently running 
 * module is located. The path guaranteed to end with file separator for 
 * current platform (that is '/' on Linux and '\\' on Windows).
 *
 * The function never fails. If it fails to find true path for the module, 
 * it returns a path pointing to current directory - './' or '.\\'.
 *
 * The function always returns the same address which points to internal 
 * static buffer. The buffer get initialized once, on the first call. 
 *
 * @note If called from within dynamic library, the function returns path
 *       for the 'main' executable module, not for the library itself.
 * 
 * @return directory of currently running module, ended with file separator
 */
const char * get_exe_dir(void)
{
#ifdef PLATFORM_POSIX
    static char buf[PATH_MAX+1];
    static const char FSEP = '/';
#else
    static char buf[_MAX_PATH+1];
    static const char FSEP = '\\';
#endif // ~PLATFORM_POSIX
    
    static bool path_done = false;
    if (path_done) {
        return buf;
    }
    
#ifdef PLATFORM_POSIX
    static const char self_path[] = "/proc/self/exe";
    // Resolve full path to module
    memset(buf, 0, sizeof(buf));
    if (readlink(self_path, buf, sizeof(buf)-1) < 0) {
#else
    if (!GetModuleFileName(NULL, buf, sizeof(buf))) {
#endif
        // Something wrong happened. Trying last resort path.
        // buf <= "./"
        buf[0] = '.'; buf[1] = FSEP; buf[2] = '\0';
    }
    else {
        // Extract directory path
        char * slash = strrchr(buf, FSEP);
        if (NULL == slash) { // Huh ? How comes ?
            // Trying last resort path.
            // buf <= "./"
            buf[0] = '.'; buf[1] = FSEP; buf[2] = '\0';
        }
        else {
            *(slash+1) = '\0';
        }
    }
    path_done = true;
    return buf;
}

void dbg(const char *frmt, ...)
{
    /* due to PMF integration
    static FILE *fout = fopen(LOG_FILE_NAME, "w");
    assert(fout != NULL);
    va_list valist;
    va_start(valist, frmt);
    vfprintf(fout, frmt, valist);
    fflush(fout);
    */
    char buf[1024];
    va_list valist;
    va_start(valist, frmt);
    vsnprintf(buf, sizeof(buf)-1, frmt, valist);
    LogStream& log = Log::log(LogStream::CT);
    log.printf(buf);
    log.flush();
}

/* due to PMF integration
static FILE *rtf = NULL;
static Mutex rtf_mutex;
*/
static int cnt = 0;
static int depth = 0;

void dbg_rt(const char * frmt, ...) 
{
    char buf[1024];
    va_list valist;
    va_start(valist, frmt);
    int len = vsnprintf(buf, sizeof(buf)-1, frmt, valist);
    
    if (buf[len-1] < ' ') {
        buf[len-1] = ' ';
    }
    if (buf[len-2] < ' ') {
        buf[len-2] = ' ';
    }
    dbg_rt_out(buf);
}

void __stdcall dbg_rt_out(const char *msg)
{
    /* due to PMF integration
    if (rtf == NULL) {
        rtf_mutex.lock();
        rtf = fopen(RUNTIME_LOG_FILE_NAME, "w");
        rtf_mutex.unlock();
        assert(rtf);
    }
    */
    ++cnt;
    if (!strncmp(msg, "enter", 5)) {
        ++depth;
    }
    // print out:
    // [call depth] message <total count>
    // precede the whole string with several spaces, so elements 
    // with big differences in the depth will be visually separated
    /* due to PMF integration
    fprintf(rtf, "%*c [%u] %s <%u>\n", depth%10, ' ', depth, msg, cnt);
    fflush(rtf);
    */
    char buf[1024*5];
    snprintf(buf, sizeof(buf)-1, "%*c [%u] %s <%u>\n", 
             depth%10, ' ', depth, msg, cnt);
    LogStream& log = Log::log(LogStream::RT);
    log.printf(buf);
    log.flush();
        
    if (!strncmp(msg, "exit", 4)) {
        --depth;
    }
    if (depth<0) {
        depth = 0;
    }
    if (g_tbreak_id != NOTHING && g_tbreak_id == (unsigned)cnt) {
        Encoder::debug();
    }
}

/**
 * @brief Returns a pointer to a disassembling function.
 * @return - pointer to disassembling function, or NULL if disassembling 
 *           is not supported/presented.
 * @todo document lwdis
 */
static DISFUNC get_disfunc(void)
{
#if !defined(PLATFORM_POSIX)
    static HMODULE hDll = LoadLibrary("lwdis.dll");
    static void *disfunc =
    hDll == NULL ? NULL : GetProcAddress(hDll, "disasm");
#else // if !platform_posix
    static bool load_done = false;
    static void *disfunc = NULL;
    if (!load_done) {
        char buf[PATH_MAX+1];
        snprintf(buf, sizeof(buf), "%sliblwdis.so", get_exe_dir());
        void * handle = dlopen(buf, RTLD_NOW);
        // An access with full path failed - let's try LD_LIBRARY_PATH.
        if (handle==NULL) {
            handle = dlopen("liblwdis.so", RTLD_NOW);
        }
        disfunc = handle == NULL ? NULL : dlsym(handle, "disasm");
        load_done = true;
    }
#endif
    return (DISFUNC)disfunc;
}

static ::std::string toStr(int i)
{
    char buf[20];
    snprintf(buf, sizeof(buf)-1, "%d", i);
    return buf;
}

::std::string CodeGen::toStr2(const Val& s, bool is_stack) const
{
    ::std::string str;
    if (false && s.jt() == jvoid) {
        str = "[x]";
        return str;
    }
    str += "[";

    if (s.has(VA_NOT_NEG)) {
        str += '+';
    };
    if (s.jt() != jvoid) {
        str += jtypes[s.jt()].name;
    }
    
    if (s.is_imm()) {
        str += ",imm=";
        char buf[50] = {0};
        if (s.jt()<=i32) { snprintf(buf, sizeof(buf), "%d(0x%X)", s.ival(), s.ival()); }
        if (s.jt()==flt32) { snprintf(buf, sizeof(buf), "%g", (double)s.fval()); }
        if (s.jt()==dbl64) { snprintf(buf, sizeof(buf), "%g", s.dval()); }
        if (s.jt()==jobj) { snprintf(buf, sizeof(buf), "%p", s.pval()); }
        if (s.jt()==i64) {
            if (is_big(i64)) {
                snprintf(buf, sizeof(buf), "%d(0x%X)", s.ival(), s.ival());
            }
            else {
                snprintf(buf, sizeof(buf), "%"FMT64"d(0x%"FMT64"X)", s.lval(), s.lval());
            }
        }
        str += buf;
        if (s.caddr() != NULL) {
            snprintf(buf, sizeof(buf), "[@%p]", s.caddr());
            str += buf;
        }
    }
    else if (s.is_reg()) {
        str += "@";
        str += Encoder::to_str(s.reg());
    }
    else {
        assert(s.is_mem());
        int l = vvar_idx(s);
        if (s.is_dummy()) {
            str += "----";
        }
        if (is_stack && l != -1) {
            str += "@var#" + toStr(l);
        }
        else if (vis_stack(s) && is_stack) {
            // no op - do not printout stack's offset
        }
        else if (l != -1 && !is_stack) {
            // no op - do not printout local's offset
        }
        else {
            str += "@";
            str += Encoder::to_str(s.base(), s.disp(), s.index(), s.scale());
        }
    }
        
    if (s.has(VA_NZ)) {
        str += ",nz";
    }
    
    str += "]";
    return str;
}

::std::string trim(const char *p) {
    const char *start = NULL, *end = NULL;
    for (const char *pp = p; *pp; pp++) {
        if (!isspace(*pp)) {
            if (start == NULL) {
                start = pp;
            }
            end = pp;
        }
    }
    ::std::string s;
    if (!start) {
        return s;
    }
    s.assign(start, end - start + 1);
    return s;
}

void dump_frame(const JitFrameContext* ctx, const MethodInfoBlock& info)
{
    assert(false); // obsolete, need update
    StackFrame sframe(info.get_num_locals(),
                      info.get_stack_max(), 
                      info.get_in_slots());
    // Not yet implemented
    assert(!(info.get_flags() & JMF_SP_FRAME));
    
    void *** pbp = devirt(bp, ctx);
    char * bp_val = (char*)(**pbp);
    
    dbg_rt("****************************************");
#define PRN(nam, frmt, type, meth_nam)  \
    dbg_rt(#nam "= " #frmt "\n", *(type*)(bp_val+sframe.meth_nam()))
    
    PRN(retIP, %p, void*, ret);
    PRN(info_gc_stack_depth, %d, int, info_gc_stack_depth);
    PRN(info_gc_locals, %d, int, info_gc_locals);
    PRN(thiz, %p, void*, thiz);
    
    unsigned num_locals = info.get_num_locals();
    for (unsigned i=0; i<num_locals; i++) {
        //dbg_rt("local#%d = %p (%d)\n", i, )
    }
    
#undef PRN
    //int ret(void) const
    //unsigned size(void) const
    //int spill(unsigned i) const
    //int (void) const
    //int (void) const
    //int info_gc_args(void) const
    //int info_gc_stack(void) const
    //int info_gc_regs(void) const
    //int (void) const
    //int scratch(void) const
    //int scratch(AR ar) const
    //int dbg_sp(void) const
    //int local(unsigned i) const
    //int stack_bot(void) const
    //int stack_slot(unsigned Val) const
    //int stack_max(void) const
    //int native_stack_bot(void) const
#if 0
    unsigned num_locals = rtinfo.get_num_locals();
    unsigned stack_max = rtinfo.get_stack_max();

    unsigned stack_depth = *(p + stackframe.info_gc_stack_depth());

    dbg("EBP             = %p\n", p);
    dbg("NUM_LOCALS      = %d\n", num_locals);
    dbg("MAX_STACK_DEPTH = %d\n", stack_max);
    dbg("NUM_IN_SLOTS    = %d\n", rtinfo.get_in_slots());
    dbg("GC_STACK_DEPTH  = %d\n", stack_depth);
    dbg("counted.esp     = %X (*ESP=%d/%X)\n",
    p + stackframe.native_stack_bot(),
    *(int *) (p + stackframe.native_stack_bot()),
    *(int *) (p + stackframe.native_stack_bot()));

    dbg("GC.Locals: %X @ %X =>", *(p + stackframe.info_gc_locals()),
    p + stackframe.info_gc_locals());
    for (unsigned i = 0; i < num_locals; i++) {
        unsigned bit = bit_no(i);
        unsigned off = word_no(i);
        //unsigned data = *(p+frame.info_gc_locals() + off);
        bool b = 0 != ((1 << bit) & *(p + stackframe.info_gc_locals() + off));
        dbg(b ? "*" : ".");
        if (i && !(i % 5)) {
            dbg(" | ");
        }
        if (i > 65536) {
            assert(false);
        }
    }
    dbg("\n");
    dbg("GC.Stack:       =>");
    for (unsigned i = 0; i < stack_depth; i++) {
        unsigned bit = bit_no(i);
        unsigned off = word_no(i);
        //unsigned data = *(p+frame.info_gc_stack() + off);
        bool b = (1 << bit) & *(p + stackframe.info_gc_stack() + off);
        dbg(b ? "*" : ".");
        if (i && !(i % 5)) {
            dbg(" | ");
        };
        if (i > 65536) {
            assert(false);
        }
    }
    dbg("\n");
    //
    //
    dbg("===========================\n");
    for (unsigned i = 0; i < num_locals; i++) {
        dbg("local.%d = %d (%X)\n", 
                i, *(p + stackframe.local(i)), *(p + stackframe.local(i)));
    }
    dbg("===========================\n");
    dbg("note: the stack is printed from bottom (=0) to top(=max_stack)\n");
    for (unsigned i = 0; i < stack_depth; i++) {
        dbg("(%d) = %d (%X)\n", 
                i, 
                *(p + stackframe.native_stack_bot() + i), 
                *(p + stackframe.native_stack_bot() + i));
    }
    dbg("===========================\n");
#endif
}

void Compiler::dbg_trace_comp_start(void)
{
    // start ; counter ; klass::method ; bytecode size ; signature
    dbg("start |%5d| %s::%s | bc.size=%d | %s\n",
        m_methID,
        class_get_name(m_klass), method_get_name(m_method),
        method_get_bytecode_length(m_method),
        method_get_descriptor(m_method));
}

void Compiler::dbg_trace_comp_end(bool success, const char * reason)
{
    // end   ; counter ; klass::method ; 
    //          [REJECTED][code start ; code end ; code size] ; signature

    dbg("end   |%5d| %s::%s | ", 
        m_methID, class_get_name(m_klass), method_get_name(m_method));
        
    if (success) {
        unsigned size = method_get_code_block_size_jit(m_method, m_hjit);
        void * start = size ? method_get_code_block_jit(m_method, m_hjit) : NULL;

        dbg("code.begin=%p | code.end=%p | code.size=%d",
             start, (char*)start + size, size);
    }
    else {
        dbg("[REJECTED:%s]", reason);
    }
    dbg("| %s\n", method_get_descriptor(m_method));
}


::std::string Compiler::toStr(const JInst & jinst, bool show_names)
{
    char tmp0[1024] = { 0 }, tmp1[1024] = { 0 };
    
    if (jinst.op0 != (int) NOTHING) {
        const char * lpClass = NULL;
        const char * lpItem = NULL;
        const char * lpDesc = NULL;
        if (show_names) {
            const JavaByteCodes opc = jinst.opcode;
            
            switch(opc) {
            case OPCODE_INVOKESPECIAL:
            case OPCODE_INVOKESTATIC:
            case OPCODE_INVOKEVIRTUAL:
            case OPCODE_INVOKEINTERFACE:
            case OPCODE_GETFIELD:
            case OPCODE_PUTFIELD:
            case OPCODE_GETSTATIC:
            case OPCODE_PUTSTATIC:
                lpClass = class_cp_get_entry_class_name(m_klass, jinst.op0);
                lpItem = class_cp_get_entry_name(m_klass, jinst.op0);
                lpDesc = class_cp_get_entry_descriptor(m_klass, jinst.op0);
                break;
            case OPCODE_NEW:
            case OPCODE_INSTANCEOF:
            case OPCODE_CHECKCAST:
                lpClass = class_cp_get_class_name(m_klass, jinst.op0);
                break;
            default: break;
            }
        }
        if (lpClass || lpItem || lpDesc) {
            snprintf(tmp0, sizeof(tmp0)-1, "%-2d {%s%s%s %s}",
                jinst.op0, 
                lpClass ? lpClass : "", lpClass ? "::" : "",
                lpItem ? lpItem : "", 
                lpDesc ? lpDesc : "");
        }
        else if (jinst.is_branch()) {
            sprintf(tmp0, "->%d<-", jinst.get_target(0));
        }
        else {
            sprintf(tmp0, "%-2d", jinst.op0);
        }
    }

    if (jinst.op1 != (int) NOTHING) {
        sprintf(tmp1, " %d ", jinst.op1);
    }
    
    char total[10240] = {0}, buf0[10240] = {0}, buf1[100] = {0};
    snprintf(buf0, sizeof(buf0)-1, "%c%2d) %-15s %-6s %-6s",
        jinst.flags & OPF_STARTS_BB ? '@' : ' ', 
        jinst.pc, instrs[jinst.opcode].name, tmp0, tmp1);
        
    if (jinst.ref_count != 1 && jinst.ref_count != 0) {
        //snprintf(buf1, sizeof(buf1)-1, "| id=%2d, rc=%d", jinst.id, jinst.ref_count);
        snprintf(buf1, sizeof(buf1)-1, "| rc=%d", jinst.ref_count);
    }
    else {
        //snprintf(buf1, sizeof(buf1)-1, "| id=%2d", jinst.id);
        //snprintf(buf1, sizeof(buf1)-1, "| id=%2d", jinst.id);
    }
    snprintf(total, sizeof(total)-1, "%-40s %s", buf0, buf1);
    return ::std::string(total);
}

void Compiler::dbg_dump_bbs(void)
{
    static const char *bb_delim = "======================================\n";

    const unsigned bc_size = m_infoBlock.get_bc_size();
    for (unsigned pc=0; pc<bc_size; pc++) {
        JInst& jinst = m_insts[pc];
        if (jinst.id == 0) continue;
        bool bb_head = jinst.flags & OPF_STARTS_BB;
        bool jsr_target = false;
        if (bb_head) {
            assert(m_bbs.find(jinst.pc) != m_bbs.end());
            dbg(bb_delim);
            jsr_target = m_bbs[jinst.pc].jsr_target;
        }
        if (bb_head) {
            dbg("ref.count=%d, %s\n", jinst.ref_count, 
                                      jsr_target ? "#JSR#" : "");
        }
        dbg("%s\n", toStr(jinst, true).c_str());
    }
    dbg(bb_delim);
}

void CodeGen::dbg_dump_state(const char *name, BBState * pState)
{
    JFrame& jframe = pState->jframe;
    dbg("\n;;State: %s\n;;\n", name);
    //
    unsigned num_locals = jframe.num_vars();
    dbg(";; locals.total=%d\n;; ", num_locals);
    
    // Dump local variables first - by 5 in a row ...
    for (unsigned i = 0; i < num_locals; i++) {
        if (i && !(i % 5)) {
            dbg("\n;;  ");
        }
        Val& s = jframe.var(i);
        dbg(" %d)%s", i, toStr2(s, false).c_str());
        if (i>=128) {
            dbg("\ntoo many locals, enough to dump.\n");
            break;
        }
    }
    
    dbg("\n;;\n");
    // ... and stack after that
    dbg(";; stack.size=%d\n;;  ", jframe.size());
    for (unsigned i = 0; i < jframe.size(); i++) {
        //if (i && !(i % 5)) {
        //    dbg("\n;;  ");
        //}
        const Val& s = jframe.at(i);
        dbg(" %s", toStr2(s, true).c_str());
        dbg("\n;;  ");
    }
    if (m_bbstate == pState) {
        dbg("\n;;\n");
        dbg(";; regs: ");
        bool not_first = false;
        for (unsigned i=0; i<ar_num; i++) {
            AR ar = _ar(i);
            if (rrefs(ar) != 0 || rlocks(ar) != 0) {
                if (not_first) {
                    dbg(",");
                }
                if (rlocks(ar)!=0) { dbg(">"); }
                dbg("%s[%d]", Encoder::to_str(ar).c_str(), rrefs(ar));
                if (rlocks(ar)!=0) { dbg("<,%d", rlocks(ar)); }
                not_first = true;
            }
        }
    }
    dbg("\n;;\n");
}

void Compiler::dbg_dump_code(const char *code, unsigned length, 
                             const char *name)
{
    if (name != NULL) {
        dbg("; .%s.start\n", name);
    }

    DISFUNC disf = get_disfunc();
    if (disf != NULL) {
    for (unsigned i = 0; i < length; /**/) {
        char buf[1024];
        unsigned bytes = (disf) (code + i, buf, sizeof(buf));
        if (bytes==0) {
            // unknown instruction
            unsigned char b = *(unsigned char*)(code+i);
            dbg("\tdb 0x%x (%d, %c)\n", b, b, b<33 || b>127 ? '.' : b);
            i += 1; // cant be 0, or we'll fall into infinite loop
        }
        else {
            dbg("\t%s\n", buf);
            i += bytes;
        }
    }
    }
    else {
        // if disassembler shared library not present, simply 
        // dump out the code
        dbg("\t");
        for (unsigned i = 0; i < length; i++) {
            dbg(" %02X", (unsigned) (unsigned char) code[i]);
            unsigned pos = (i + 1) % 16;
            if (0 == pos) {
                // output by 16 bytes per line
                dbg("\n\t");
            }
            else if (7 == pos) {
                // additional space in the middle of the output
                dbg(" ");
            }
        }
    }
    dbg("\n");
    if (name != NULL) {
        dbg("; .%s.end\n", name);
    }
}

void Compiler::dbg_dump_code_bc(const char * code, unsigned codeLen)
{
    if (codeLen == 0) {
        dbg("no code\n");
        return;
    }
    unsigned pc = NOTHING;
    DISFUNC disf = get_disfunc();
    const char * first_inst = m_infoBlock.get_ip(0);
        
    for (unsigned i=0; i<codeLen; /**/) {
        const char * ip = code + i;
        unsigned tmp = m_infoBlock.get_pc(ip);
        if (ip>=first_inst && pc != tmp) {
            // we just passed to a new byte code instruction, print it out
            pc = tmp;
            JInst jinst;
            memset(&jinst, 0, sizeof(jinst));
            unsigned next_pc = fetch(pc, jinst);
            dbg(";; %s\n", toStr(jinst, true).c_str());
            // Dump all instructions that do have the same IP.
            // check whether we have a code for this instruction - read next
            // item and compare its IP. If they are the same, then 'jinst' 
            // represents an empty instruction with no real code - NOP, POP, 
            // etc. In this case, switch to the nearest instruction with a 
            // code.
            for(;next_pc<m_infoBlock.get_bc_size();) {
                const char * next_ip = m_infoBlock.get_ip(next_pc);
                if (next_ip != ip) {
                    break;
                }
                memset(&jinst, 0, sizeof(jinst));
                next_pc = fetch(next_pc, jinst);
                dbg(";; %s\n", toStr(jinst, true).c_str());
            }
        }
        if (disf) {
            char buf[1024];
            unsigned bytes = disf(code + i, buf, sizeof(buf));
            if (bytes==0) {
                // unknown instruction
                unsigned char b = *(unsigned char*)(code+i);
                dbg("0x%p\tdb 0x%x (%d, %c)\n", code+i, b, b, b<33 || b>127 ? '.' : b);
                i += 1; // cant be 0, or we'll fall into infinite loop
            }
            else {
                dbg("0x%p\t%s\n", code + i, buf);
                i += bytes;
            }
            
        }
        else {
            i += 1;
        }
    }
}

}
};  // ~namespace Jitrino::Jet
