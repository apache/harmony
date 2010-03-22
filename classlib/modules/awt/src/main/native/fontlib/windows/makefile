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

!include <$(HY_HDK)\build\make\defines.mak>

LIBBASE=FL
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib
HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

HYCFLAGS = $(HYCFLAGS) /I$(SHAREDSUB)include /I$(SHAREDSUB) \
	/EHsc

BUILDFILES = \
  $(SHAREDSUB)fljni.obj \
  $(SHAREDSUB)EncodedValue.obj \
  $(SHAREDSUB)Environment.obj \
  $(SHAREDSUB)Font.obj \
  $(SHAREDSUB)Glyph.obj \
  $(SHAREDSUB)Outline.obj \
  $(SHAREDSUB)ParsingTables.obj \
  $(SHAREDSUB)T1Font.obj \
  $(SHAREDSUB)T1Glyph.obj \
  $(SHAREDSUB)TTCurve.obj \
  $(SHAREDSUB)TTFont.obj \
  $(SHAREDSUB)TypeDefinition.obj \
  FontLibExports.obj

VIRTFILES = $(LIBBASE).res
SYSLIBFILES = ws2_32.lib Iphlpapi.lib

MDLLIBFILES = $(MDLLIBFILES) \
  $(LIBPATH)hypool.lib $(LIBPATH)vmi.lib \
  $(LIBPATH)hyzlib.lib 

DLLBASE=0x13300000

!include <$(HY_HDK)\build\make\rules.mak>
