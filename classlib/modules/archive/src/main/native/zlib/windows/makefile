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

!include <$(HY_HDK)\build\make\defines.mak>

LIBBASE=hyzlib
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib

ZLIB_DIST=..\..\zlib_dist\# Path to zlib

HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def

BUILDFILES = \
  $(SHAREDSUB)zlib_copyright.obj $(ZLIB_DIST)adler32.obj $(ZLIB_DIST)compress.obj \
  $(ZLIB_DIST)crc32.obj $(ZLIB_DIST)deflate.obj $(ZLIB_DIST)gzio.obj $(ZLIB_DIST)infback.obj \
  $(ZLIB_DIST)inffast.obj $(ZLIB_DIST)inflate.obj $(ZLIB_DIST)inftrees.obj $(ZLIB_DIST)trees.obj \
  $(ZLIB_DIST)uncompr.obj $(ZLIB_DIST)zutil.obj

VIRTFILES = $(LIBBASE).res
DLLBASE=0x11700000

!include <$(HY_HDK)\build\make\rules.mak>
