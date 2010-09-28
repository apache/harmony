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

HY_BIN=$(HY_TARGET)\build\classlib\awt\native\fontlib\# trailing slash
!include <$(HY_TARGET)\hdk\build\make\defines.mak>

LIBBASE=FL
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib
HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

HYCFLAGS = $(HYCFLAGS) /I$(SHAREDSUB)include /I$(SHAREDSUB) \
	/EHsc

BUILDFILES = \
  $(HY_BIN)fljni.obj \
  $(HY_BIN)EncodedValue.obj \
  $(HY_BIN)Environment.obj \
  $(HY_BIN)Font.obj \
  $(HY_BIN)Glyph.obj \
  $(HY_BIN)Outline.obj \
  $(HY_BIN)ParsingTables.obj \
  $(HY_BIN)T1Font.obj \
  $(HY_BIN)T1Glyph.obj \
  $(HY_BIN)TTCurve.obj \
  $(HY_BIN)TTFont.obj \
  $(HY_BIN)TypeDefinition.obj \
  $(HY_BIN)FontLibExports.obj

VIRTFILES = $(LIBBASE).res
SYSLIBFILES = ws2_32.lib Iphlpapi.lib

MDLLIBFILES = $(MDLLIBFILES) $(LIBPATH)hyzlib.lib 

DLLBASE=0x13300000

!include <$(HY_HDK)\build\make\rules.mak>
