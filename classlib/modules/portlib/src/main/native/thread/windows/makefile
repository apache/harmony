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
# Makefile for module 'thread'
#

!include <$(HY_HDK)\build\make\defines.mak>

LIBBASE=hythr
DLLNAME=..\$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib

BUILDFILES = \
  $(SHAREDSUB)thread_copyright.obj $(HY_PLATFORM)\thrhelp.obj $(HY_PLATFORM)\thrspinlock.obj \
  $(SHAREDSUB)hythread.obj $(SHAREDSUB)hythreadinspect.obj $(SHAREDSUB)rwmutex.obj thrdsup.obj \
  $(SHAREDSUB)thrprof.obj

# TOFIX $(LIBBASE).def should be a dependency on all libs
VIRTFILES = $(LIBBASE).res
HYCFLAGS = $(HYCFLAGS) /I$(SHAREDSUB)
HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def 

MDLLIBFILES = $(LIBPATH)hypool$(HY_LINKLIB_SUFFIX) $(LIBPATH)hycommon$(HY_LINKLIB_SUFFIX)

DLLBASE=0x11500000

!include <$(HY_HDK)\build\make\rules.mak>
