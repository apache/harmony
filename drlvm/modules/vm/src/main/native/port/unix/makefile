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
# Makefile for module 'port'
#

HY_BIN=$(HY_TARGET)/build/drlvm/port/native/port/
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
  thread/thread_os.o thread.o thread/port_thread_tls_find.o \
  misc/timer.o misc/sysencoding.o misc/timezone.o misc/execname.o \
  misc/user.o misc/dso.o misc/sysinfo.o \
  file_io/filepath.o file_io/canonical.o \
  atomic/port_atomic.o disasm/disasm.o vmem/port_vmem.o tl/memory_pool.o \
  malloc/port_malloc_registrar.o malloc/port_malloc.o \
  time/misc_platform_natives.o \
  file_io/filepath_generic.o \
  logger/logparams.o logger/logger.o

LIBNAME = $(LIBPATH)libport.a

include $(HY_HDK)/build/make/rules.mk
