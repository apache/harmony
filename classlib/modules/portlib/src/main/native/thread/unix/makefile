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

HY_BIN=$(HY_TARGET)/build/classlib/portlib/native/thread/
include $(HY_TARGET)/hdk/build/make/defines.mk

BUILDFILES = thread_copyright.o thrhelp.o thrspinlock.o hythread.o \
  hythreadinspect.o linuxonexit.o priority.o rasthrsup.o rwmutex.o \
  thrcreate.o thrdsup.o thrprof.o

OSLIBS += -lpthread
MDLLIBFILES = $(LIBPATH)libhypool.a $(LIBPATH)libhycommon.a
DLLNAME = $(HY_BIN)libhythr$(HY_SHLIB_SUFFIX)
EXPNAME = HYTHR_0.1
HY_CAN_LINK_DEBUG=no

include $(HY_TARGET)/hdk/build/make/rules.mk
