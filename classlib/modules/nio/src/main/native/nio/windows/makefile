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
# Makefile for module 'nio'
#

!include <$(HY_HDK)\build\make\defines.mak>

LIBBASE=hynio
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib
HYCFLAGS = $(HYCFLAGS) /I$(SHAREDSUB) /I$(SHARED)common /I$(SHARED)fdlibm
HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

BUILDFILES = \
  $(SHAREDSUB)DirectBufferUtil.obj $(SHAREDSUB)AddressUtil.obj
VIRTFILES = hynio.res

SYSLIBFILES = ws2_32.lib Iphlpapi.lib

!IF "$(HY_ZIP_API)" != "true"
MDLLIBFILES = \
  $(LIBPATH)hyzip$(HY_LINKLIB_SUFFIX) $(LIBPATH)hyzlib$(HY_LINKLIB_SUFFIX)
!ENDIF

MDLLIBFILES = $(LIBPATH)hycommon$(HY_LINKLIB_SUFFIX) \
  $(LIBPATH)hypool$(HY_LINKLIB_SUFFIX) \
  $(LIBPATH)hythr$(HY_LINKLIB_SUFFIX) $(LIBPATH)vmi$(HY_LINKLIB_SUFFIX)

DLLBASE=0x13200000

!include <$(HY_HDK)\build\make\rules.mak>
