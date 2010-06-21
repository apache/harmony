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
# Makefile for module 'encoder'
#

HY_BIN=$(HY_TARGET)/build/drlvm/port/native/encoder/
include $(HY_TARGET)/hdk/build/make/defines.mk
VM_HOME=../../../../../../../vm/

DEFINES += \
  -DREFS_USE_UNCOMPRESSED -D_IA32_ -DPLATFORM_POSIX \
  -D__SMP__ -DLINUX_TLS_OPT -D_LARGEFILE64_SOURCE

INCLUDES := -I$(VM_HOME)port/include -I$(VM_HOME)include $(INCLUDES)
PLATFORM = 
OPT += -fno-exceptions -Wno-deprecated -g -O2

HDKINCLUDES = $(HY_HDK)/include/dec_base.h $(HY_HDK)/include/enc_base.h \
  $(HY_HDK)/include/enc_prvt.h $(HY_HDK)/include/enc_defs.h \
  $(HY_HDK)/include/encoder.h $(HY_HDK)/include/encoder.inl
BUILDFILES = dec_base.o enc_base.o encoder.o enc_tabl.o
LIBNAME = $(LIBPATH)libencoder.a

include $(HY_HDK)/build/make/rules.mk

$(HY_BIN)%.o: $(ENCDIR)%.cpp
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_HDK)/include/%.h: $(SHAREDSUB)$(HY_ARCH)/%.h
	mkdir -p $(@D)
	cp -p $< $@

$(HY_HDK)/include/%.inl: $(SHAREDSUB)$(HY_ARCH)/%.inl
	mkdir -p $(@D)
	cp -p $< $@
