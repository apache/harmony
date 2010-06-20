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


#ifndef __STACK_DUMP_H__
#define __STACK_DUMP_H__

#include "open/platform_types.h"
#include "port_general.h"
#include "port_modules.h"
#include "port_crash_handler.h"


#define SD_MNAME_LENGTH 2048

/**
 * Symbolic method info:
 * method name, source file name and a line number of an instruction
 * within the method
 */
struct CFunInfo {
    char name[SD_MNAME_LENGTH];
    char filename[PORT_PATH_MAX];
    int line;
};

#ifdef __cplusplus
extern "C" {
#endif

void sd_init_crash_handler();
void sd_cleanup_crash_handler();
void sd_get_c_method_info(CFunInfo* info, native_module_t* module, void* ip);
void sd_print_cmdline_cwd();
void sd_print_environment();

unsigned port_crash_handler_get_flags();
int initialize_signals();
int shutdown_signals();

void sd_print_crash_info(int signum, Registers* regs, port_unwind_compiled_frame unwind);

void print_reg_state(Registers* regs);

/* Returns 0  when execution should be continued with (updated) Registers
   Returns 1  when crash occured and process should invoke a debugger
   Returns -1 when crash occured and process should be terminated */
int port_process_signal(port_sigtype signum, Registers *regs, void* fault_addr, Boolean iscrash);

#ifdef WIN32
void create_minidump(LPEXCEPTION_POINTERS exp);
#endif


Boolean sd_is_handler_registered(port_sigtype signum);


#ifdef __cplusplus
}
#endif

#endif /* !__STACK_DUMP_H__ */
