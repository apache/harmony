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

#include <ctype.h>
#include <assert.h>
#include <string.h>
// offsetof macro
#include <stddef.h>

#include "port_malloc.h"
#include "port_unwind.h"
#include "port_modules.h"

#include "port_crash_handler.h"
#include "port_frame_info.h"
#include "stack_dump.h"


static native_module_t* g_modules = NULL;

// Is called to fill modules list (should be called under lock)
static void sd_fill_modules()
{
    if (g_modules)
        return;

    int count;
    bool res = port_get_all_modules(&g_modules, &count);
    assert(res && g_modules && count);
}


// "%s::%s (%s): %s:%d" - class::name (signature): file:line
static const char* vm_fmt_tbl[] = {
// method_class_name is present
    "%s::%s (%s): %s:%d\n",
    "%s::%s (%s)\n", // No sourcefile
    "%s::%s: %s:%d\n", // No signature
    "%s::%s\n", // No signature no sourcefile
// method_class_name is absent
    "%s (%s): %s:%d\n",
    "%s (%s)\n", // No sourcefile
    "%s: %s:%d\n", // No signature
    "%s\n", // No signature no sourcefile
};

static void sd_print_vm_line(FILE* file, int count, Registers* regs, port_stack_frame_info* fi)
{
    static char pi_blanks[2*sizeof(void*) + 3];
    static void* pi_blanks_set_res = memset(pi_blanks, ' ', sizeof(pi_blanks));
    static char pi_blanks_zero_res = (pi_blanks[sizeof(pi_blanks) - 1] = 0);

    if (!fi->method_name)
        return;

    if (count >= 0)
        fprintf(file, "%3d: ", count);
    else
        fprintf(file, "     "); // additional VM info - indent

    if (regs)
        fprintf(file, "0x%"W_PI_FMT"  ", regs->get_ip());
    else
        fprintf(file, "%s  ", pi_blanks); // additional VM info - indent

    const void* args[5] = {fi->method_class_name, fi->method_name,
                     fi->method_signature, fi->source_file_name,
                     (const void*)(size_t)fi->source_line_number};
    const void** pargs = args;
    int fmt_num = 0;

    if (!fi->method_class_name)
    {
        fmt_num += 4;
        pargs++;
    }
    if (!fi->method_signature)
    {
        fmt_num += 2;
        args[2] = args[3];
        args[3] = args[4];
    }
    if (!fi->source_file_name)
        fmt_num += 1;

    fprintf(file, vm_fmt_tbl[fmt_num],
                  pargs[0], pargs[1], pargs[2], pargs[3], pargs[4]);
}


static void sd_print_c_line(FILE* file, int count, Registers* regs, CFunInfo* cfi)
{
    if (!cfi->name)
        return;

    fprintf(file, "%3d: 0x%"W_PI_FMT"  %s (%s:%d)\n",
        count, regs->get_ip(), cfi->name,
        cfi->filename ? cfi->filename : "??",
        cfi->line);
}


static void sd_print_modules()
{
    sd_fill_modules(); // Fill modules table if needed
    fprintf(stderr, "\nLoaded modules:\n\n");
    port_dump_modules(g_modules, stderr);
}


static void sd_print_stack(Registers* regs, port_unwind_compiled_frame unwind)
{
    if (!regs)
    {
        fprintf(stderr, "\nNo stack trace due to registers info absence\n");
        return;
    }

    fprintf(stderr, "\nStack trace:\n");

    Registers locregs = *regs;
    Registers tmpregs = locregs;
    UnwindContext uwcontext;
    bool hasnative = false;

    if (port_init_unwind_context(&uwcontext, g_modules, &locregs))
        hasnative = true;

    port_stack_frame_info uwinfo;

    int framenum = 0;
    int uwresult = -1;

    // Prepare VM-specific struct for stack iteration
    if (unwind)
    {
        memset(&uwinfo, 0, sizeof(port_stack_frame_info));
        uwresult = unwind(&locregs, &uwinfo);
    }

    if (uwresult > 0)
    {
        uwinfo.iteration_state = STD_ALLOCA(uwresult);
        memset(uwinfo.iteration_state, 0, uwresult);
        uwresult = unwind(&locregs, &uwinfo);
        assert(uwresult <= 0);
    }

    while (true)
    {
        // Unwinding with VM callback
        if (unwind && uwresult == 0)
        {
            sd_print_vm_line(stderr, framenum++, &tmpregs, &uwinfo);
            // Cleanup frame info except 'iteration_state'
            memset(&uwinfo, 0, offsetof(port_stack_frame_info, iteration_state));
            // Try unwinding by the callback for next iteration
            tmpregs = locregs;
            uwresult = unwind(&locregs, &uwinfo);
            continue;
        }

        // Print native frame info
        CFunInfo cfi = {0};
        native_module_t* module = port_find_module(uwcontext.modules, locregs.get_ip());
        sd_get_c_method_info(&cfi, module, locregs.get_ip());
        sd_print_c_line(stderr, framenum++, &locregs, &cfi);

        if (unwind && uwresult < 0 && uwinfo.method_name)
        { // VM has not unwound but has provided additional frame info
            sd_print_vm_line(stderr, -1, NULL, &uwinfo); // Print as additional info
        }

        if (!hasnative) // Native unwinding is not initialized
            break;

        // Try native unwinding
        bool nativeres = port_unwind_frame(&uwcontext, &locregs);

        if (!nativeres)
            break; // Neither VM calback nor native unwinding can unwind the frame

        // Cleanup frame info except 'iteration_state'
        memset(&uwinfo, 0, offsetof(port_stack_frame_info, iteration_state));
        // Try unwinding by the callback for next iteration
        tmpregs = locregs;
        uwresult = unwind(&locregs, &uwinfo);
    }

    if (hasnative)
        port_clean_unwind_context(&uwcontext);

    fprintf(stderr, "<end of stack trace>\n");
    fflush(stderr);
}


struct sig_name_t
{
    int          num;
    const char*  name;
};

static sig_name_t sig_names[] =
{
    {PORT_SIGNAL_GPF,            "GENERAL_PROTECTION_FAULT"},
    {PORT_SIGNAL_STACK_OVERFLOW, "STACK_OVERFLOW"},
    {PORT_SIGNAL_ABORT,          "ABORT" },
    {PORT_SIGNAL_QUIT,           "QUIT"},
    {PORT_SIGNAL_CTRL_C,         "CTRL_C" },
    {PORT_SIGNAL_CTRL_BREAK,     "CTRL_BREAK" },
    {PORT_SIGNAL_BREAKPOINT,     "BREAKPOINT"},
    {PORT_SIGNAL_ARITHMETIC,     "ARITHMETIC_EXCEPTION" },
    {PORT_SIGNAL_UNKNOWN,        "UNKNOWN"}
};

static const char* get_sig_name(int signum)
{
    for (int i = 0; i < sizeof(sig_names)/sizeof(sig_names[0]); i++)
    {
        if (signum == sig_names[i].num)
            return sig_names[i].name;
    }

    return "unknown signal";
}

static void print_name_and_context(int signum, Registers* regs)
{
    // Print signal name
    fprintf(stderr, "\nSignal reported: %s\n", get_sig_name(signum));

    // Print register state
    if (regs)
        print_reg_state(regs);
    else
        fprintf(stderr, "\nRegisters info is absent\n");
}


void sd_print_crash_info(int signum, Registers* regs, port_unwind_compiled_frame unwind)
{
    // Print signal name and register info
    if (port_crash_handler_get_flags() & PORT_CRASH_PRINT_REGISTERS)
        print_name_and_context(signum, regs);

    // Print command line and working directory
    if (port_crash_handler_get_flags() & PORT_CRASH_PRINT_COMMAND_LINE)
        sd_print_cmdline_cwd();

    // Print program environment info
    if (port_crash_handler_get_flags() & PORT_CRASH_PRINT_ENVIRONMENT)
        sd_print_environment();

    // Print the whole list of modules
    if (port_crash_handler_get_flags() & PORT_CRASH_PRINT_MODULES)
        sd_print_modules();

    // Print stack of crashed module
    if (port_crash_handler_get_flags() & PORT_CRASH_STACK_DUMP)
        sd_print_stack(regs, unwind);
}
