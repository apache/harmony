# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Makefile for module 'nio'
#

HY_BIN=$(HY_TARGET)/build/classlib/nio/native/nio/
include $(HY_TARGET)/hdk/build/make/defines.mk

BUILDFILES = DirectBufferUtil.o AddressUtil.o
ifeq ($(HY_OS),linux)
BUILDFILES += EpollSelectorImpl.o
endif

MDLLIBFILES = $(LIBPATH)libhycommon.a

DLLNAME = $(DLLPATH)libhynio$(HY_SHLIB_SUFFIX)
EXPNAME = HYNIO_0.1

include $(HY_TARGET)/hdk/build/make/rules.mk
