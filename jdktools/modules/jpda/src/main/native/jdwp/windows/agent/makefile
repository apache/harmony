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

!include <$(HY_HDK)\build\make\defines.mak>

COMMON=..\..\common\# comment to avoid \ being treated as continuation
CMNAGENT=$(COMMON)agent\# comment to avoid \ being treated as continuation

LIBBASE=jdwp
DLLNAME=$(TOOLSDLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib

INCLUDES = $(INCLUDES) \
         -I$(CMNAGENT)commands -I$(CMNAGENT)core \
         -I$(COMMON)generic -Icore

HYCFLAGS = $(HYCFLAGS) $(INCLUDES) -GX -wd4290

HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

BUILDFILES = \
    $(CMNAGENT)commands\ArrayReference.obj \
    $(CMNAGENT)commands\ArrayType.obj \
    $(CMNAGENT)commands\ClassLoaderReference.obj \
    $(CMNAGENT)commands\ClassObjectReference.obj \
    $(CMNAGENT)commands\ClassType.obj \
    $(CMNAGENT)commands\EventRequest.obj \
    $(CMNAGENT)commands\Method.obj \
    $(CMNAGENT)commands\ObjectReference.obj \
    $(CMNAGENT)commands\ReferenceType.obj \
    $(CMNAGENT)commands\StackFrame.obj \
    $(CMNAGENT)commands\StringReference.obj \
    $(CMNAGENT)commands\ThreadGroupReference.obj \
    $(CMNAGENT)commands\ThreadReference.obj \
    $(CMNAGENT)commands\VirtualMachine.obj \
    $(CMNAGENT)core\Agent.obj $(CMNAGENT)core\AgentEventRequest.obj $(CMNAGENT)core\AgentMonitor.obj \
    $(CMNAGENT)core\ClassManager.obj $(CMNAGENT)core\CommandDispatcher.obj $(CMNAGENT)core\CommandHandler.obj \
    $(CMNAGENT)core\EventDispatcher.obj $(CMNAGENT)core\LogManager.obj $(CMNAGENT)core\MemoryManager.obj \
    $(CMNAGENT)core\ObjectManager.obj $(CMNAGENT)core\OptionParser.obj $(CMNAGENT)core\PacketDispatcher.obj \
    $(CMNAGENT)core\PacketParser.obj $(CMNAGENT)core\RequestManager.obj $(CMNAGENT)core\RequestModifier.obj \
    $(CMNAGENT)core\ThreadManager.obj $(CMNAGENT)core\TransportManager.obj $(CMNAGENT)core\AgentManager.obj \
    core\TransportManager_pd.obj

VIRTFILES = 

SYSLIBFILES = 

MDLLIBFILES = 
  
DLLBASE=0x13200000
COMMENT=/comment:"JDWP agent native code. (c) Copyright 2005 - 2006 The Apache Software Foundation or its licensors, as applicable."

!include <$(HY_HDK)\build\make\rules.mak>
