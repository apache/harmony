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

include $(HY_HDK)/build/make/defines.mk

COMMON=../../common/
CMNAGENT=$(COMMON)agent/

CFLAGS += -fpic
CXXFLAGS += -fpic

INCLUDES += -I$(CMNAGENT)commands -I$(CMNAGENT)core \
            -I$(COMMON)generic -Icore

LDFLAGS += $(STDCLIBS)

BUILDFILES = \
    $(CMNAGENT)commands/ArrayReference.o \
    $(CMNAGENT)commands/ArrayType.o \
    $(CMNAGENT)commands/ClassLoaderReference.o \
    $(CMNAGENT)commands/ClassObjectReference.o \
    $(CMNAGENT)commands/ClassType.o \
    $(CMNAGENT)commands/EventRequest.o \
    $(CMNAGENT)commands/Method.o \
    $(CMNAGENT)commands/ObjectReference.o \
    $(CMNAGENT)commands/ReferenceType.o \
    $(CMNAGENT)commands/StackFrame.o \
    $(CMNAGENT)commands/StringReference.o \
    $(CMNAGENT)commands/ThreadGroupReference.o \
    $(CMNAGENT)commands/ThreadReference.o \
    $(CMNAGENT)commands/VirtualMachine.o \
    $(CMNAGENT)core/Agent.o $(CMNAGENT)core/AgentEventRequest.o \
    $(CMNAGENT)core/AgentMonitor.o $(CMNAGENT)core/ClassManager.o \
    $(CMNAGENT)core/CommandDispatcher.o $(CMNAGENT)core/CommandHandler.o \
    $(CMNAGENT)core/EventDispatcher.o $(CMNAGENT)core/LogManager.o \
    $(CMNAGENT)core/MemoryManager.o $(CMNAGENT)core/ObjectManager.o \
    $(CMNAGENT)core/OptionParser.o $(CMNAGENT)core/PacketDispatcher.o \
    $(CMNAGENT)core/PacketParser.o $(CMNAGENT)core/RequestManager.o \
    $(CMNAGENT)core/RequestModifier.o $(CMNAGENT)core/ThreadManager.o \
    $(CMNAGENT)core/TransportManager.o $(CMNAGENT)core/AgentManager.o \
    core/TransportManager_pd.o

MDLLIBFILES = 

DLLNAME = $(TOOLSDLLPATH)libjdwp.so

include $(HY_HDK)/build/make/rules.mk
