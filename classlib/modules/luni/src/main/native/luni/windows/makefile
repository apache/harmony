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
# Makefile for module 'luni'
#

HY_BIN=$(HY_TARGET)\build\classlib\luni\native\luni\# trailing slash
!include <$(HY_TARGET)\hdk\build\make\defines.mak>

LIBBASE=hyluni
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib
HYCFLAGS = $(HYCFLAGS) /I$(SHAREDSUB)
HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

BUILDFILES = \
  $(HY_BIN)luni_copyright.obj $(HY_BIN)file.obj $(HY_BIN)procimpl.obj \
  $(HY_BIN)nethelp.obj $(HY_BIN)floatbits.obj \
  $(HY_BIN)helpers.obj $(HY_BIN)math.obj \
  $(HY_BIN)luniglob.obj $(HY_BIN)proxy.obj $(HY_BIN)netif.obj \
  $(HY_BIN)cbigint.obj $(HY_BIN)dblparse.obj $(HY_BIN)fltparse.obj \
  $(HY_BIN)process.obj $(HY_BIN)bigint.obj $(HY_BIN)osc.obj \
  $(HY_BIN)socket.obj $(HY_BIN)inetadds.obj \
  $(HY_BIN)filedesc.obj $(HY_BIN)timezone.obj \
  $(HY_BIN)OSFileSystemWin32.obj $(HY_BIN)hyenv.obj\
  $(HY_BIN)OSFileSystem.obj $(HY_BIN)OSMemoryWin32.obj $(HY_BIN)OSMemory.obj \
  $(HY_BIN)OSNetworkSystem.obj $(HY_BIN)OSNetworkSystemWin32.obj \
  $(HY_BIN)consoleimpl.obj

VIRTFILES = hyluni.res

SYSLIBFILES = ws2_32.lib Iphlpapi.lib Mswsock.lib

MDLLIBFILES = $(MDLLIBFILES) \
  $(LIBPATH)hyfdlibm$(HY_LINKLIB_SUFFIX) $(LIBPATH)vmi$(HY_LINKLIB_SUFFIX)

DLLBASE=0x13200000

!include <$(HY_HDK)\build\make\rules.mak>
