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
# Makefile for module 'interpreter'
#

HY_BIN=$(HY_TARGET)/build/drlvm/vm/native/interpreter/
include $(HY_TARGET)/hdk/build/make/defines.mk
VM_HOME=../../../../../../../vm/

DEFINES += \
  -DREFS_USE_UNCOMPRESSED -DPLATFORM_POSIX \
  -D__SMP__ -DLINUX_TLS_OPT -D_LARGEFILE64_SOURCE

OPT += -fexceptions
CXXFLAGS += -Wno-deprecated

INCLUDES := -I$(VM_HOME)include -I$(VM_HOME)port/include \
            -I$(VM_HOME)vmcore/include $(INCLUDES)

BUILDFILES := \
  interp_exports.o interp_native.o interp_stack_trace.o interp_vm_helpers.o \
  interpreter.o interpreter_ti.o invokeJNI.o

DLLNAME = $(DLLPATH)default/libinterpreter$(HY_SHLIB_SUFFIX)
EXPNAME = INTERPRETER_0.1

AS := $(CXX)
ASFLAGS := -Wa,--fatal-warnings -c -x assembler-with-cpp -g

include $(HY_HDK)/build/make/rules.mk
