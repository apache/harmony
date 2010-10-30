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
# Makefile for module jdwp
#

HY_BIN=$(HY_TARGET)/build/jdktools/jpda/native/agent/
include $(HY_TARGET)/hdk/build/make/defines.mk

DLL_LD=$(CXX)
COMMON=../../common/
CMNAGENT=$(COMMON)agent/

INCLUDES := -I../../../include -I$(CMNAGENT)commands -I$(CMNAGENT)core \
            -I$(COMMON)generic -Icore $(INCLUDES)

BUILDFILES = ArrayReference.o ArrayType.o ClassLoaderReference.o \
  ClassObjectReference.o ClassType.o EventRequest.o Method.o \
  ObjectReference.o ReferenceType.o StackFrame.o StringReference.o \
  ThreadGroupReference.o ThreadReference.o VirtualMachine.o \
  Agent.o AgentEventRequest.o AgentMonitor.o ClassManager.o \
  CommandDispatcher.o CommandHandler.o EventDispatcher.o LogManager.o \
  MemoryManager.o ObjectManager.o OptionParser.o PacketDispatcher.o \
  PacketParser.o RequestManager.o RequestModifier.o ThreadManager.o \
  TransportManager.o AgentManager.o CallBacks.o ExceptionManager.o \
  TransportManager_pd.o

ifneq ($(HY_OS),zos)
  # !zOS
  OPT += -fno-exceptions -fno-rtti 
  ifneq ($(HY_OS),aix)
    # !AIX and !zOS
    MDLLIBFILES = $(LIBPATH)libvmi.so
    DLL_LDFLAGS += -fno-exceptions -fno-rtti
    LDFLAGS += $(STDCLIBS)
  else
    # AIX
    MDLLIBFILES = 
    LDFLAGS += -L$(LIBPATH). -L$(LIBPATH)../. $(STDCLIBS) -lvmi
  endif
else
  # zOS
  MDLLIBFILES += $(LIBPATH)libvmi.x
  LDFLAGS += $(STDCLIBS)
endif

ifeq ($(HY_NO_THR),true)
MDLLIBFILES += $(LIBPATH)libhythr$(HY_LINKLIB_SUFFIX)
endif

DLLNAME = $(TOOLSDLLPATH)libjdwp.so

STDCLIBS = 

CCVERSION=$(shell $(CC) -v 2>&1 | sed -e '1,3d; s/gcc version \([^ ]\+\).*/\1/')
ifneq ($(HY_OS),zos)
  ifeq (1,$(shell expr "$(CCVERSION)" \>= "3.4.6"))
    OPT += -fno-threadsafe-statics
  endif
endif

include $(HY_HDK)/build/make/rules.mk

$(HY_BIN)%.o: core/%.cpp
	@mkdir -p ${@D}
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(CMNAGENT)core/%.cpp
	@mkdir -p ${@D}
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(CMNAGENT)commands/%.cpp
	@mkdir -p ${@D}
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<
