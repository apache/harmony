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

HY_BIN=$(HY_TARGET)\build\jdktools\jpda\native\jdwp\# trailing slash
!include <$(HY_TARGET)\hdk\build\make\defines.mak>

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
  $(HY_BIN)ArrayReference.obj $(HY_BIN)ArrayType.obj \
  $(HY_BIN)ClassLoaderReference.obj $(HY_BIN)ClassObjectReference.obj \
  $(HY_BIN)ClassType.obj $(HY_BIN)EventRequest.obj $(HY_BIN)Method.obj \
  $(HY_BIN)ObjectReference.obj $(HY_BIN)ReferenceType.obj \
  $(HY_BIN)StackFrame.obj $(HY_BIN)StringReference.obj \
  $(HY_BIN)ThreadGroupReference.obj $(HY_BIN)ThreadReference.obj \
  $(HY_BIN)VirtualMachine.obj $(HY_BIN)Agent.obj \
  $(HY_BIN)AgentEventRequest.obj $(HY_BIN)AgentMonitor.obj \
  $(HY_BIN)ClassManager.obj $(HY_BIN)CommandDispatcher.obj \
  $(HY_BIN)CommandHandler.obj $(HY_BIN)EventDispatcher.obj \
  $(HY_BIN)LogManager.obj $(HY_BIN)MemoryManager.obj \
  $(HY_BIN)ObjectManager.obj $(HY_BIN)OptionParser.obj \
  $(HY_BIN)PacketDispatcher.obj $(HY_BIN)PacketParser.obj \
  $(HY_BIN)RequestManager.obj $(HY_BIN)RequestModifier.obj \
  $(HY_BIN)ThreadManager.obj $(HY_BIN)TransportManager.obj \
  $(HY_BIN)AgentManager.obj $(HY_BIN)TransportManager_pd.obj

VIRTFILES = 

SYSLIBFILES = 

MDLLIBFILES = 

DLLBASE=0x13200000
COMMENT=/comment:"JDWP agent native code. (c) Copyright 2005 - 2006 The Apache Software Foundation or its licensors, as applicable."

!include <$(HY_HDK)\build\make\rules.mak>

{core}.cpp{$(HY_BIN)}.obj:
        -mkdir $(*D)
        $(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(CMNAGENT)core}.cpp{$(HY_BIN)}.obj:
        -mkdir $(*D)
        $(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(CMNAGENT)commands}.cpp{$(HY_BIN)}.obj:
        -mkdir $(*D)
        $(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<
