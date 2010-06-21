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
  * @brief Main Jitrino.JET's interface implementation.
  */
  
/**
 * @mainpage
 * @section sec_intro Introduction
 * <center>
 * Jitrino.JET: <b>J</b>itrino's <b>e</b>xpress compilation pa<b>t</b>h
 * </center>
 * 
 * Jitrino.JET is a simple baseline compiler for %Jitrino JIT.
 *
 * It's primarily targeted for fast compilation to ensure quick start for 
 * client applications and to support optimizing engine of main %Jitrino 
 * (instrumentation, profile generation, etc.).
 *
 * Jitrino.JET performs 2 passes over bytecode. On the first pass, basic 
 * blocks boundaries are found. On the second pass code is generated.
 *
 * The code is generated targeting an abstract CPU that is armed with 
 * registers, memory, memory stack and can perform several operations like
 * move between memory/register, ALU operations, branch and call, etc 
 * (class Encoder generates primitive operations of this abstract CPU).
 *
 * Technically, Jitrino.JET generates a code that simulates stack-based 
 * operations of Java byte code using a register CPU. Every method 
 * compiled by Jitrino.JET creates a stack frame with a structure similar 
 * to the frame described by JVM spec (3.6). The stack frame contains local
 * variables array and an area for operand stack plus some other auxilary
 * items. Class StackFrame describes the stack frame structure.
 * 
 * The compilation engine consists of two main classes - Compiler and 
 * CodeGen.
 *
 * Though the separation is quite relative, the main idea is that class 
 * Compiler processes high level method items, like basic blocks, whole 
 * method's data, code layout etc. The class CodeGen is more targeted to 
 * process instruction-level things (e.g. the byte code instructions 
 * themselves). Compiler organizes pass over the byte code, instruments
 * CodeGen's fields with actual data (current compiler state, PC, etc) and 
 * invokes CodeGen's methods to generate code for each instruction.<br>
 * Some bytecode instruction (like GOTO) need to deal with basic blocks
 * information, thus their generation is placed into Compiler rather than
 * in CodeGen.
 *
 *
 * Class Compiler is based on CodeGen which in turn inherits from classes 
 * StaticConsts, Encoder and MethInfo.  The StaticConsts class has no 
 * funtional interface, it's just used to keep static constants separately.
 * Class Encoder represents generation of CPU operations.
 * MethInfo provides an interface to obtain various info about method being
 * compiled.
 * 
 * As it was stated above, two passes over the byte code is performed.
 *
 * The first pass is linear scan of byte code (Compiler::comp_parse_bytecode) 
 * - it decodes all byte code instructions, finds boundaries of basic 
 * blocks, counts references for instructions and collects statistics about 
 * usage of local variabled. The reference count for instruction is number
 * of incoming edges of control flow. For instructions inside basic block, 
 * the reference count is 1. For instructions that are basic block leaders, 
 * it may be more than 1. Reference count of zero means dead code.
 *
 * Then, a simple global register allocation performed 
 * (Compiler::comp_alloc_regs) basing on the info collected in 
 * Compiler::comp_parse_bytecode(). Local (per-basic block) register 
 * allocation is done during code generation (pass 2) via 
 * CodeGen::valloc(jtype) calls.
 * 
 * The second pass is performed in depth-first search (DFS) order, and the 
 * code is generated (Compiler::comp_gen_code_bb). The code at first is 
 * generated into internal buffer (CodeGen::m_codeStream), and then a code 
 * layout is performed (Compiler::comp_layout_code) so the layout of 
 * generated code becomes exactly the same as byte code layout.
 * 
 * During compilation, Jitrino.JET mimics Java operand stack operations 
 * (class JFrame), so its state is known for every byte code instruction.
 * This mimic stack is used to get a GC info and eliminate many unnecessary 
 * temporary operations caused by stack-based nature of byte code (e.g. 
 * ICONST_0, ISTORE_1 => mov [local#1], 0). The JFrame class also contains 
 * local variables array. Both operand stack and local variables array in 
 * JFrame are used to track item locations (register, memory), known 
 * attributes (i.e. tested against null).
 *
 * Instance of JFrame is part of BBState class which is used to maintain 
 * per-basic block state of compiler.
 * 
 * A special code is generated for GC support. During runtime 2 GC maps 
 * are maintained. GC map is a set of bits which shows whether an item 
 * contains a reference. GC map for local variables is build complitely 
 * during runtime. That is when a write to a local variable is generated,
 * the code to set or clear approprate bit in GC map is also generated (see 
 * CodeGen::gen_gc_mark_local()).
 * 
 * GC map for operand stack is semi-runtime. That is the operand stack 
 * state is maintaned duirng compilation. Before generate code for a call 
 * site special code is generated that updates GC map and operand stack 
 * depth at the given point (CodeGen::gen_gc_stack).
 *
 * To be accurate, there is on more GC map maintained - the GC map for
 * callee-save registers. The code for this is also generated in 
 * CodeGen::gen_gc_mark_local(), as only local variables may reside on
 * such regisers with current approach.
 *
 * When GC enumeration start, the runtime support code extracts these GC 
 * maps method's stack frame, and then objects are reported accordingly (
 * see rt_enum()).
 *
 * 
 */

#include "open/vm_method_access.h"
#include "jet.h"
#include "compiler.h"
#include "stats.h"

#include <jit_export_jpda.h>

#include <assert.h>
#include "trace.h"
#include "mkernel.h"
#include "PlatformDependant.h"
#include "version.h"

#include <set>
#include <string>
using std::set;
using std::string;


namespace Jitrino {
namespace Jet {


static void process_global_args(void);

static PMF* g_pmf = NULL;

void setup(JIT_Handle jit, const char * name)
{
    g_pmf = &JITInstanceContext::getContextForJIT(jit)->getPMF();
    process_global_args();
}

void cleanup(void)
{
#ifdef JIT_STATS
    const char * lp = g_pmf->getArg(NULL, "stats");
    if (lp != NULL && to_bool(lp)) {
        Stats::dump();
    }
#endif  // ~JIT_STATS
    g_pmf->deinit();
}


bool supports_compresed_refs(void)
{
#if defined(_EM64T_) || defined(_IPF_)
    return true;
#else
    return false;
#endif
}

const char *args[][2] = 
{
//-------------------------------------------------------------------------
{"show", 
 "    =help - prints out this text                                      \n"
 "    =info                                                             \n"
 "    =id                                                               \n"
 "    =version - prints out build info                                  \n"
 "    =all     - help,version                                           \n"
 "    =<empty> - same as all                                            \n"
},
//-------------------------------------------------------------------------
{"log", 
 "The following categories are supported:                               \n"
 "Compilation:                                                          \n"
 "(don't forget to add 'log=ct'. default is code+sum)                   \n"
 " sum    - prints out short summary about compiled method              \n"
 " cg     - trace every stage of code generation                        \n"
 " code   - dump resulting code                                         \n"
 " layout - addresses of generated basic blocks                         \n"
 "Runtime:                                                              \n"
 "(don't forget to add 'log=rt'!)                                       \n"
 " rtsupp - prints out runtime support events (unwind, GC enum, etc)    \n"
 " ee     - logs method's enter and exit events                         \n"
 " bc     - logs execution of each bytecode instruction                 \n"
 "                                                                      \n"
 "Examples:                                                             \n"
 "        -Djit.jet.arg.log=ct,sum,code                                 \n"
 "        -Djit.jet.arg.log=ct   (same as log=ct,sum,cg)                \n"
 "        -Djit.jet.arg.log=rt -Djit.jet.filterName.arg.log=ee          \n"
},
//-------------------------------------------------------------------------
{"break", 
 NULL
},
{"brk", 
 "type: bool or uint; default: off; scope: method                       \n"
 "brk=on - triggers software breakpoint at the beginning of the method  \n"
 "brk=PC - triggers software breakpoint at bytecode @ PC                \n"
},
//-------------------------------------------------------------------------
{"tbreak", 
 "type: uint; default: none; scope: global                              \n"
 "Break into debugger when counter in dbg_rt reaches the specified      \n"
 "value.                                                                \n"
},
//-------------------------------------------------------------------------
{"checkstack", 
 "type: bool; default: off; scope: method                               \n"
 "generates code to verify stack integrity before and after each        \n"
 "bytecode instruction.                                                 \n"
},
//-------------------------------------------------------------------------
{"checkmem", 
 "type: bool; default: off; scope: method                               \n"
 "Enforces memory checks before and after compilation of a method.      \n"
 "Implementation is platform-dependent and may be no op on some         \n"
 "platforms/build configurations. Currently, only Windows/debug build   \n"
 "the check implemented.                                                \n"
},
//-------------------------------------------------------------------------
{"emulate", 
 "type: bool; default: off; scope: method                               \n"
 "Performs compilation, but do not register compiled code in VM.        \n"
 "Return JIT_FAILURE after compilation.                                 \n"
},
//-------------------------------------------------------------------------
{"accept", 
 NULL,
},
{"reject", 
 "both accept and reject:\n"
 "type: range of [uint][-uint]; default: none; scope: global            \n"
 "reject only:                                                          \n"
 "type: bool; default: off; scope: method                               \n"
 "On global scope defines a range of compilation ids of methods to be   \n"
 "accepted or rejected for compilation. Any part of range may be omitted.\n"
},
//-------------------------------------------------------------------------
{"list", 
 "type: string; default: none; scope: global                            \n"
 "Sets name of file with list of fully qualified names of methods.      \n"
 "Any method not in the list will be rejected.                          \n"
},
//-------------------------------------------------------------------------
{"bbp", 
 "type: bool; default: on; scope: method                                \n"
 "turns on and off generation of back branches polling code               "
},

//-------------------------------------------------------------------------
{"hwnpe",
 "type: bool; default: on; scope: method                                \n"
 "Controls whether to generate hardware NPE checks instead of explicit  \n"
 "software checks                                                       \n"
},
   
#ifdef JIT_STATS
{"stats", 
 "type: bool; default: off; scope: global                               \n"
 "Collects and shows various statistics about compiled methods.         \n"
},
#endif  // ~JIT_STATS

{"wb4c", 
 "type: bool; default: off; scope: method                               \n"
 "Generates code with write barriers (C-based GC).                      \n"

},
{"wb4c.skip_statics", 
"type: bool; default: TRUE; scope: method                               \n"
"If true (default) then do NOT report PUTSTATIC into write barrier.     \n"
},

{"wb4j", 
"type: bool; default: off; scope: method                                \n"
"Generates code with write barriers (Java-based GC).                    \n"
},

};

static void print_id(void);

static void print_help(void)
{
    static bool help_printed = false;
    if (help_printed) return;
    print_id();
    for (unsigned i=0; i<COUNTOF(args); i++) {
        printf("%s\n", args[i][0]);
        if (args[i][1] != NULL) {
            printf("%s\n", args[i][1]);
        }
    }
    help_printed = true;
}
#ifdef _DEBUG
static std::set<string> checked;
static Mutex checkedLock;
#endif
void check_arg_has_doc(const char* key)
{
#ifdef _DEBUG
    //
    checkedLock.lock();
    if (checked.find(key) == checked.end()) {
        checkedLock.unlock();
        return;
    }
    checked.insert(key);
    checkedLock.unlock();
    //
    bool found = false;
    for (unsigned i=0; i<COUNTOF(args); i++) {
        if (!strcmp(args[i][0], key)) {
            found = true;
            break;
        }
    }
    if (!found) {
        printf(
          "WARNING: an argument named '%s' has no documentation !\n", key);
        printf(
            "Please, add appropriate description into \n\t" __FILE__ "\n");
    }
#endif  // ifdef _DEBUG
}

static const char *get_id_string(void)
{
    static char buf[80] = {0};
    if (buf[0] != 0) return buf;
    unsigned len = sizeof(buf)-1;
    
    const char revision[] = VERSION_SVN_TAG;

#ifdef PROJECT_JET
    #define ALONE_STR   ", alone"
#else
    #define ALONE_STR   ""
#endif

#ifdef _DEBUG
    #define DBG_STR ", dbg"
#else
    #define DBG_STR ""
#endif

#ifdef __INTEL_COMPILER
    #define COMP_STR    ", icl"
#else
    #define COMP_STR    ""
#endif

    char revision_buf[80] = {0};
    if (revision[0] != 'u') { /* ignore 'u'nknown */
        snprintf(revision_buf, sizeof(revision_buf)-1, " Rev.: %s.",
                 revision);
    }

    snprintf(buf, len, 
        "Jitrino.JET" DBG_STR COMP_STR ALONE_STR ": "
        "Built: " __DATE__ " " __TIME__ 
        ".%s", revision_buf);
        
    return buf;
}

static void print_id(void)
{
    static bool id_printed = false;
    if (id_printed) return;
    puts(get_id_string());
    id_printed = true;
}

static void parse_range(const char* str, unsigned* pStart, unsigned *pEnd)
{
    if (str[0] != '-' && !isdigit(str[0])) {
        // wrong or empty params. print warning ?
        return;
    }
    if (str[0] != '-') {
        //'0-1' or '0'  version
        sscanf(str, "%u", pStart);
    }
    const char * pdash = strchr(str, '-');
    if (pdash != NULL) {
        sscanf(pdash+1, "%u", pEnd);
    }
}

static void process_global_args(void)
{
    const char * lp;
    lp = g_pmf->getArg(NULL, "show");
    if (lp != NULL) {
        // empty string means 'all';
        bool show_all = lp[0] == 0 || !strcmpi(lp, "all"); 
        bool show_help = show_all || (NULL!=strstr(lp, "help"));
        bool show_id = show_all || 
                        (NULL!=strstr(lp, "id")) || 
                        (NULL!=strstr(lp, "info")) || 
                        (NULL!=strstr(lp, "version"));
        if (show_help) {
            print_help();
        }
        if (show_id) {
            print_id();
        }
    }
    lp = g_pmf->getArg(NULL, "accept");
    if (lp != NULL) {
        parse_range(lp, &Compiler::g_acceptStartID, &
                    Compiler::g_acceptEndID);
    }
    lp = g_pmf->getArg(NULL, "reject");
    if (lp != NULL) {
        parse_range(lp, &Compiler::g_rejectStartID, &
                    Compiler::g_rejectEndID);
    }
    lp = g_pmf->getArg(NULL, "tbreak");
    if (lp != NULL) {
        g_tbreak_id = NOTHING;
        sscanf(lp, "%u", &g_tbreak_id);
    }
}


void cmd_line_arg(JIT_Handle jit, const char* name, const char* arg)
{
    if (!strcmp(arg, "jet::help")) {
        print_help();
    }
    else if (!strcmp(arg, "jet::id")) {
        print_id();
    }
    else if (!strcmp(arg, "jet::info")) {
        print_id();
        print_help();
    }
    else if (!strncmp(arg, "jet::", 5)) {
        static bool warning_printed = false;
        if (!warning_printed) {
        puts(
"*********************************************************************\n"
"*                            WARNING !                              *\n"
"* Command line options in form of -Xjit jet::arg are deprecated !   *\n"
"*                                                                   *\n"
"* The jet:: parameters are IGNORED                                  *\n"
"*                                                                   *\n"
"* To pass arguments to Jitrino.JET use                              *\n"
"*          -Djit.<jit_name>.arg=value                               *\n"
"* Use                                                               *\n"
"*          -Djit.<jit_name>.show=help                               *\n"
"* to get the list of supported args.                                *\n"
"*********************************************************************\n"
        );
        warning_printed = true;
        }
    }
}



OpenMethodExecutionParams get_exe_capabilities()
{
    OpenMethodExecutionParams supported = {0};
    supported.exe_notify_method_entry = true;
    supported.exe_notify_method_exit = true;
    supported.exe_do_code_mapping = true;
    supported.exe_do_local_var_mapping = true;
    supported.exe_restore_context_after_unwind = true;
    supported.exe_provide_access_to_this = true;
    return supported;
}

JIT_Result compile_with_params(JIT_Handle jit_handle, Compile_Handle ch, 
                               Method_Handle method, 
                               OpenMethodExecutionParams params)
{
    ::Jitrino::Jet::Compiler jit(jit_handle);
    if (!CPUID::isSSE2Supported()) {
        //TODO: return FAILURE only of method contains double ops!
        return JIT_FAILURE;
    }
    return jit.compile(ch, method, params);
}


}}; // ~ namespace Jitrino::Jet

// standalone interface ?
#ifdef PROJECT_JET

/**
 * @defgroup JITRINO_JET_STANDALONE Standalone interface
 * 
 * Jitrino.JET may be compiled as a small separate JIT, mostly for 
 * debugging/testing purposes.
 *
 * To do this, a preprocessor macro PROJECT_JET must be defined. Also, some 
 * files are used from src/share - namely, mkernel.* and PlatformDependant.h.
 * 
 * These exported functions represent inetrface required to interact with VM.
 *
 * @{
 */
 
#include <map>
using std::map;
#include "../main/Log.h"
#include "../main/LogStream.h"
#include "../main/PMF.h"

//
// PMF and JitInstanceContext things uses various stuff from Jitrino::.
// Define it here to allow standalone .jet build.
// 


namespace Jitrino {
typedef map<JIT_Handle, JITInstanceContext*> JITCTXLIST;
static JITCTXLIST jitContextList;
static MemoryManager g_mm("global MM");

//
// CompilationContext stub
//
static TlsStack<CompilationContext> ccTls;

CompilationContext* CompilationContext::getCurrentContext() {
    CompilationContext* currentCC = ccTls.get();
    return currentCC;
}

CompilationContext::CompilationContext(MemoryManager& _mm,
                                       CompilationInterface * ci,
                                       JITInstanceContext * jtx) : mm(_mm)
{
    compilationInterface = ci;
    compilationFailed = false;
    compilationFinished = false;

    jitContext = jtx;
    hirm = NULL;
    lirm = NULL;
    currentSessionAction = NULL;
    currentSessionNum = 0;
    currentLogStreams = NULL;
    pipeline = NULL;
    //
    //
    ccTls.push((CompilationContext*)this);
}

CompilationContext::~CompilationContext()
{
    assert(this == ccTls.get());
    ccTls.pop();
}

static int thread_nb = 0;

struct TlsLogStreams {

    int threadnb;
    MemoryManager mm;

    typedef std::pair<JITInstanceContext*, LogStreams*> Jit2Log;

    typedef StlVector<Jit2Log> Jit2Logs;
    Jit2Logs jit2logs;

    TlsLogStreams ()
        :threadnb(thread_nb), mm(0, "TlsLogStreams"), jit2logs(mm) {}

    ~TlsLogStreams ();
};


TlsLogStreams::~TlsLogStreams ()
{
    Jit2Logs::iterator ptr = jit2logs.begin(),
                       end = jit2logs.end();
    for (; ptr != end; ++ptr)
        ptr->second->~LogStreams();
}


static TlsStore<TlsLogStreams> tlslogstreams;
static Mutex logInfoLock;

/*
    Because CompilationContext is a transient object (it created on start of compilation
    and destroyed on end of compilation for every method), LogStreams table cannot reside
    in it. Thread-local storage (TLS) is used to keep LogStreams.
    On the other hand, different Jits can run on the same thread, so several LogStreams
    have to be kept for single thread.
    To optimize access, pointer to LogStreams is cached in CompilationContext.

 */
LogStreams& LogStreams::current(JITInstanceContext* jitContext) {

    CompilationContext* ccp = CompilationContext::getCurrentContext();
    LogStreams* cls = ccp->getCurrentLogs();
    if (cls != 0)
        return *cls;

//  No cached pointer is available for this CompilationContext.
//  Find TLS for this thread.

    TlsLogStreams* sp = tlslogstreams.get();
    if (sp == 0)
    {   // new thread
        logInfoLock.lock();
        ++thread_nb;
        logInfoLock.unlock();
        sp = new TlsLogStreams();
        tlslogstreams.put(sp);
    }

//  Find which Jit is running now.

    if (jitContext == 0)
        jitContext = ccp->getCurrentJITContext();

//  Was LogStreams created for this Jit already?

    TlsLogStreams::Jit2Logs::iterator ptr = sp->jit2logs.begin(),
                                      end = sp->jit2logs.end();
    for (; ptr != end; ++ptr)
        if (ptr->first == jitContext) {
        //  yes, it was - store pointer in the CompilationContext
            ccp->setCurrentLogs(cls = ptr->second);
            return *cls;
        }

//  This is the first logger usage by the running Jit in the current thread.
//  Create LogStreams now.

    cls = new (sp->mm) LogStreams(sp->mm, jitContext->getPMF(), sp->threadnb);
    sp->jit2logs.push_back(TlsLogStreams::Jit2Log(jitContext, cls));
    ccp->setCurrentLogs(cls);

    return *cls;
}


LogStream& LogStream::log (SID sid, HPipeline* hp)
{
    if (hp == 0)
        hp = CompilationContext::getCurrentContext()->getPipeline();
    Str name = ((PMF::Pipeline*)hp)->name;
    return LogStream::log(sid, name.ptr, name.count);
}

//
// JITInstanceContex stub
//
JITInstanceContext::JITInstanceContext(MemoryManager& _mm,
                                       JIT_Handle _jitHandle,
                                       const char* _jitName) : mm(_mm)
{
    jitHandle = _jitHandle;
    jitName = _jitName;
    pmf = new (mm) PMF(mm, *this);
    profInterface = NULL;
    useJet = true;
}


JITInstanceContext* JITInstanceContext::getContextForJIT(JIT_Handle jitHandle)
{
    assert(jitContextList.find(jitHandle) != jitContextList.end());
    return jitContextList[jitHandle];
}

JITInstanceContext* Jitrino::getJITInstanceContext(JIT_Handle jitHandle)
{
    return JITInstanceContext::getContextForJIT(jitHandle);
}


//
// Fake XTimer stuff
double XTimer::getSeconds(void)const { return 0.0; }
void SummTimes::add(char const *,double) {}

//
// Crash handler
void crash(const char* fmt, ...)
{
    va_list valist;
    va_start(valist, fmt);
    vprintf(fmt, valist);
    exit(0);
}

}; // ~namespace Jitrino

//
// Symbols from local 'Jitrino::'
using Jitrino::JITInstanceContext;
using Jitrino::MemoryManager;
using Jitrino::jitContextList;
using Jitrino::g_mm;
//
//
using Jitrino::CompilationContext;
using Jitrino::PMF;
using Jitrino::HPipeline;
using Jitrino::LogStreams;

/**
 * @see setup
 */
extern "C" JITEXPORT
void JIT_init(JIT_Handle jit, const char* name, vm_adaptor_t adaptor)
{
    JITInstanceContext* jic = 
                new(g_mm) JITInstanceContext(g_mm, jit, name);
    assert(jitContextList.find(jit) == jitContextList.end());
    jitContextList[jit] = jic;
    jic->getPMF().init();
    Jitrino::Jet::setup(jit, name);
}

/**
 * @see cleanup
 */
extern "C" JITEXPORT
void JIT_deinit(JIT_Handle jit)
{
    Jitrino::Jet::cleanup();
}

/**
 * @see cmd_line_arg
 */
extern "C" JITEXPORT
void JIT_next_command_line_argument(JIT_Handle jit, const char *name, 
                                    const char *arg)
{
    Jitrino::Jet::cmd_line_arg(jit, name, arg);
}


extern "C" JITEXPORT 
JIT_Result JIT_compile_method_with_params(JIT_Handle jit, 
                                          Compile_Handle ch, 
                                          Method_Handle method, 
                                          OpenMethodExecutionParams params)
{
    //Jitrino::CompilationContext ctx(jit, method);
    MemoryManager memManager(1024, "JIT_compile_method.memManager");
    JITInstanceContext* jitContext = Jitrino::Jitrino::getJITInstanceContext(jit);
    assert(jitContext!= NULL);
    //DrlVMCompilationInterface
    //        compilationInterface(ch, method, jit, memManager, params, NULL);
    Jitrino::CompilationInterface* pci = NULL;
    CompilationContext cs(memManager, pci/*&compilationInterface*/, jitContext);
    //compilationInterface.setCompilationContext(&cs);

    static int method_seqnb = 0;
    int current_nb = method_seqnb++;

    Class_Handle klass = method_get_class(method);
    const char* methodTypeName = class_get_name(klass);
    const char* methodName = method_get_name(method);
    const char* methodSig = method_get_descriptor(method);
    PMF::Pipeline* pipep =
        jitContext->getPMF().selectPipeline(methodTypeName, methodName, methodSig);
    cs.setPipeline((HPipeline*)pipep);
    LogStreams::current(jitContext).beginMethod(methodTypeName, methodName, methodSig, current_nb);
    JIT_Result result = Jitrino::Jet::compile_with_params(jit, ch, method, params);
    LogStreams::current(jitContext).endMethod();
    return result;
}

/**
 * @see get_exe_capabilities
 */
extern "C" JITEXPORT
OpenMethodExecutionParams JIT_get_exe_capabilities(JIT_Handle jit)
{
    return Jitrino::Jet::get_exe_capabilities();
}


/**
 * Noop in Jitrino.JET.
 * @return FALSE
 */
extern "C" JITEXPORT Boolean JIT_recompiled_method_callback(
        JIT_Handle jit, Method_Handle  method, void * callback_data)
{
    return FALSE;
}

/**
 * @see rt_unwind
 */
extern "C" JITEXPORT
void JIT_unwind_stack_frame(JIT_Handle jit, Method_Handle method,
                            ::JitFrameContext *context)
{
    Jitrino::Jet::rt_unwind(jit, method, context);
}

/**
 * @see rt_enum
 */
extern "C" JITEXPORT
void JIT_get_root_set_from_stack_frame(JIT_Handle jit, Method_Handle method,
                                       GC_Enumeration_Handle henum,
                                       ::JitFrameContext *context)
{
    Jitrino::Jet::rt_enum(jit, method, henum, context);
}

/**
 * @see rt_fix_handler_context
 */
extern "C" JITEXPORT
void JIT_fix_handler_context(JIT_Handle jit, Method_Handle method,
                             ::JitFrameContext   *context)
{
    Jitrino::Jet::rt_fix_handler_context(jit, method, context);
}

/**
 * @see rt_get_address_of_this
 */
extern "C" JITEXPORT
void * JIT_get_address_of_this(JIT_Handle jit, Method_Handle method,
                               const ::JitFrameContext *context)
{
    return Jitrino::Jet::rt_get_address_of_this(jit, method, context);
}

/**
 * Inlining is unsupported by Jitrino.JET.
 * @return 0
 */
extern "C" JITEXPORT
U_32 JIT_get_inline_depth(JIT_Handle jit, InlineInfoPtr ptr, U_32 offset)
{
    return 0;
}

/**
 * Inlining is unsupported by Jitrino.JET.
 * @return 0
 */
extern "C" JITEXPORT
Method_Handle JIT_get_inlined_method(JIT_Handle jit, InlineInfoPtr ptr,
                                     U_32 offset, U_32 inline_depth)
{
    return 0;
}

/**
 * Inlining is unsupported by Jitrino.JET.
 * @return 0
 */
extern "C" JITEXPORT
uint16 JIT_get_inlined_bc(JIT_Handle jit, InlineInfoPtr ptr,
                                     U_32 offset, U_32 inline_depth)
{
    return 0;
}


/**
 * @returns \b Whether Jitrino.JET support compressed references on current
 * platform.
 */
extern "C" JITEXPORT
Boolean JIT_supports_compressed_references(JIT_Handle jit)
{
    return ::Jitrino::Jet::supports_compresed_refs();
}


/**
 * @todo need to be implemented
 */
extern "C" JITEXPORT
void JIT_get_root_set_for_thread_dump(JIT_Handle jit, Method_Handle method,
                                      GC_Enumeration_Handle henum,
                                      ::JitFrameContext *context )
{
    assert(false);
}


//**********************************************************************
//* exported routines for JVMTI
//**********************************************************************

extern "C" JITEXPORT
OpenExeJpdaError get_native_location_for_bc(JIT_Handle jit, 
                                            Method_Handle method, 
                                            uint16  bc_pc, 
                                            NativeCodePtr *pnative_pc)
{
    Jitrino::Jet::rt_bc2native(jit, method, bc_pc, pnative_pc);
    return EXE_ERROR_NONE;
}

extern "C" JITEXPORT
OpenExeJpdaError get_bc_location_for_native(JIT_Handle jit,
                                            Method_Handle method,
                                            NativeCodePtr native_pc,
                                            uint16 *pbc_pc)
{
    Jitrino::Jet::rt_native2bc(jit, method, native_pc, pbc_pc);
    return EXE_ERROR_NONE;
}

extern "C" JITEXPORT
OpenExeJpdaError get_local_var(JIT_Handle jit,
                               Method_Handle method,
                               const ::JitFrameContext *context,
                               uint16  var_num,
                               VM_Data_Type var_type,
                               void *value_ptr)
{
    return Jitrino::Jet::rt_get_local_var(jit, method, context,
                                          (unsigned)var_num, var_type,
                                          value_ptr);
}

extern "C" JITEXPORT
OpenExeJpdaError set_local_var(JIT_Handle jit,
                               Method_Handle method,
                               const ::JitFrameContext *context,
                               uint16 var_num, VM_Data_Type var_type,
                               void *value_ptr)
{
    return Jitrino::Jet::rt_set_local_var(jit, method, context,
                                          (unsigned)var_num, var_type,
                                          value_ptr);
}

/// @} // ~ defgroup JITRINO_JET_STANDALONE

#endif  // ~ifdef PROJECT_JET   // standalone interface


