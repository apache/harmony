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
# Makefile for module jdwp - transport
#
# export HY_HDK=/home/geir/dev/apache/harmony/enhanced/trunk/classlib/deploy/
# export HY_DRLVM=/home/geir/dev/apache/harmony/enhanced/trunk/drlvm/build/deploy/

#

include $(HY_HDK)/build/make/defines.mk

COMMON=../../../common/
CMNTRANS=$(COMMON)transport/

CFLAGS += -fpic
CXXFLAGS += -fpic

INCLUDES += -I$(CMNTRANS)common -I$(CMNTRANS)dt_socket \
            -I$(COMMON)generic -I. \
            -I../common

LDFLAGS += $(STDCLIBS)

BUILDFILES = \
    $(CMNTRANS)common/LastTransportError.o \
    $(CMNTRANS)dt_socket/SocketTransport.o

MDLLIBFILES = 

DLLNAME = $(TOOLSDLLPATH)libdt_socket.so

include $(HY_HDK)/build/make/rules.mk
