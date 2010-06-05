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

COMMON=../../common/
CMNAGENT=$(COMMON)agent/

CFLAGS += -fpic
CXXFLAGS += -fpic

INCLUDES += -I$(CMNAGENT)commands -I$(CMNAGENT)core \
            -I$(COMMON)generic -Icore

LDFLAGS += $(STDCLIBS)

BUILDFILES = ArrayReference.o ArrayType.o ClassLoaderReference.o \
  ClassObjectReference.o ClassType.o EventRequest.o Method.o \
  ObjectReference.o ReferenceType.o StackFrame.o StringReference.o \
  ThreadGroupReference.o ThreadReference.o VirtualMachine.o \
  Agent.o AgentEventRequest.o AgentMonitor.o ClassManager.o \
  CommandDispatcher.o CommandHandler.o EventDispatcher.o LogManager.o \
  MemoryManager.o ObjectManager.o OptionParser.o PacketDispatcher.o \
  PacketParser.o RequestManager.o RequestModifier.o ThreadManager.o \
  TransportManager.o AgentManager.o TransportManager_pd.o

MDLLIBFILES = 

DLLNAME = $(TOOLSDLLPATH)libjdwp.so

include $(HY_HDK)/build/make/rules.mk

$(HY_BIN)%.o: core/%.cpp
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(CMNAGENT)core/%.cpp
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<

$(HY_BIN)%.o: $(CMNAGENT)commands/%.cpp
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<
