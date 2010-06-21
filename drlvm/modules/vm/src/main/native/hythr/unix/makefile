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
# Makefile for module 'hythr'
#

HY_BIN=$(HY_TARGET)/build/drlvm/port/native/hythr/
include $(HY_TARGET)/hdk/build/make/defines.mk
VM_HOME=../../../../../../../vm/

DEFINES += \
  -DREFS_USE_UNCOMPRESSED -D_IA32_ -DPLATFORM_POSIX \
  -D__SMP__ -DLINUX_TLS_OPT -D_LARGEFILE64_SOURCE \
  -DAPR_DECLARE_EXPORT

OPT += -fexceptions
CXXFLAGS += -Wno-deprecated

INCLUDES := -I$(VM_HOME)include -I$(VM_HOME)port/include \
            -I$(VM_HOME)port/src/thread/include $(INCLUDES)

BUILDFILES := \
  os_condvar.o \
  thread_init.o thread_native_attrs.o thread_native_basic.o \
  thread_native_condvar.o thread_native_fat_monitor.o \
  thread_native_groups.o thread_native_interrupt.o thread_native_iterator.o \
  thread_native_latch.o thread_native_park.o thread_native_semaphore.o \
  thread_native_state.o thread_native_suspend.o \
  thread_native_thin_monitor.o thread_native_tls.o thread_ncai_common.o

DLLNAME = $(DLLPATH)default/libhythr$(HY_SHLIB_SUFFIX)
ifeq ($(HY_NO_THR),false)
EXPNAME = HYTHR_0.1
else
EXPNAME = HYTHR_0.2
endif

OSLIBS += -lpthread -lrt
MDLLIBFILES += $(LIBPATH)libapr-1.a $(LIBPATH)libport.a
LDFLAGS += -Wl,-init -Wl,hythread_library_init

include $(HY_HDK)/build/make/rules.mk
