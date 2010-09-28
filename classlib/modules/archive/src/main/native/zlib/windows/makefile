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
# Makefile for module 'zlib'
#

HY_BIN=$(HY_TARGET)\build\classlib\archive\native\zlib\# trailing slash
!include <$(HY_TARGET)\hdk\build\make\defines.mak>

LIBBASE=hyzlib
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib

OSS_DIST = $(HY_TARGET)\depends\native\zlib_dist\# trailing slash

HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

BUILDFILES = \
  $(HY_BIN)zlib_copyright.obj $(HY_BIN)adler32.obj $(HY_BIN)compress.obj \
  $(HY_BIN)crc32.obj $(HY_BIN)deflate.obj $(HY_BIN)infback.obj \
  $(HY_BIN)inffast.obj $(HY_BIN)inflate.obj $(HY_BIN)inftrees.obj \
  $(HY_BIN)trees.obj $(HY_BIN)uncompr.obj $(HY_BIN)zutil.obj

VIRTFILES = $(LIBBASE).res
DLLBASE=0x11700000

!include <$(HY_HDK)\build\make\rules.mak>
