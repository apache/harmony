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
# Makefile for module jdwp - dt_socket transport
#

!include <$(HY_HDK)\build\make\defines.mak>

COMMON=..\..\..\common\# comment to avoid \ being treated as continuation
CMNTRANS=$(COMMON)transport\# comment to avoid \ being treated as continuation

LIBBASE=dt_socket
DLLNAME=$(TOOLSDLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib

INCLUDES = $(INCLUDES) \
         -I$(CMNTRANS)common -I$(CMNTRANS)dt_socket \
         -I$(COMMON)generic -I. \
         -I..\common

HYCFLAGS = $(HYCFLAGS) $(INCLUDES) -GX -wd4290

HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

BUILDFILES = \
    $(CMNTRANS)common\LastTransportError.obj \
    $(CMNTRANS)dt_socket\SocketTransport.obj 

VIRTFILES = 

SYSLIBFILES = 

MDLLIBFILES = 
  
DLLBASE=0x13200000
COMMENT=/comment:"JDWP socket transport native code. (c) Copyright 2005 - 2006 The Apache Software Foundation or its licensors, as applicable."

!include <$(HY_HDK)\build\make\rules.mak>
