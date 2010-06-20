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
# Makefile for module 'verifier'
#

HY_BIN=$(HY_TARGET)/build/drlvm/port/native/verifier/
include $(HY_TARGET)/hdk/build/make/defines.mk
VM_HOME=../../../../../../../vm/

DEFINES += \
  -DREFS_USE_UNCOMPRESSED -D_IA32_ -DPLATFORM_POSIX \
  -D__SMP__ -DLINUX_TLS_OPT -D_LARGEFILE64_SOURCE \
  -DBUILDING_VM

OPT += -fexceptions
CXXFLAGS += -Wno-deprecated

INCLUDES := -I$(VM_HOME)vmcore/include -I$(VM_HOME)include $(INCLUDES)

BUILDFILES := \
  base/context_base.o base/context_x.o base/tpool.o \
  base/ver.o base/vf_resolve.o \
  java5/context_5.o \
  java6/context_6.o

ifeq ($(HY_CFG),debug)
BUILDFILES += \
  x_verifier/recompute.o x_verifier/write_attr.o x_verifier/x_verifier.o
endif

LIBNAME = $(LIBPATH)libverifier.a

include $(HY_HDK)/build/make/rules.mk
