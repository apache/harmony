#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

#
# Makefile for module 'port_ch'
#

HY_BIN=$(HY_TARGET)/build/drlvm/port/native/port_ch/
include $(HY_TARGET)/hdk/build/make/defines.mk
VM_HOME=../../../../../../../vm/

DEFINES += \
  -DREFS_USE_UNCOMPRESSED -D_IA32_ -DPLATFORM_POSIX \
  -D__SMP__ -DLINUX_TLS_OPT -D_LARGEFILE64_SOURCE \
  -DAPR_DECLARE_EXPORT -DBUILDING_VM

OPT += -fexceptions
CXXFLAGS += -Wno-deprecated

INCLUDES := -I$(VM_HOME)include -I$(VM_HOME)port/include \
            -I$(VM_HOME)port/src/thread/include $(INCLUDES)

BUILDFILES := \
  crash_handler/gdb_crash_handler.o crash_handler/native_unwind_arch.o \
  crash_handler/native_unwind.o crash_handler/native_unwind_os.o \
  crash_handler/port_crash_handler.o crash_handler/reg_state.o \
  crash_handler/stack_dump.o crash_handler/stack_dump_os.o \
  memaccess.o memaccess_asm.o \
  modules/native_modules_procmap.o modules/native_modules.o \
  modules/native_modules_os.o \
  signals/port_signals.o signals/signals_common.o signals/signals.o \
  signals/signals_asm.o \
  thread/port_thread_tls_os.o

MDLLIBFILES += $(LIBPATH)libport.a $(LIBPATH)libencoder.a

ifeq ($(HY_OS),linux)
OSLIBS += -lrt
endif

DLLNAME = $(DLLPATH)default/libch$(HY_SHLIB_SUFFIX)
EXPNAME = PORT_0.1

AS := $(CXX)
ASFLAGS := -Wa,--fatal-warnings -c -x assembler-with-cpp -g

include $(HY_HDK)/build/make/rules.mk
