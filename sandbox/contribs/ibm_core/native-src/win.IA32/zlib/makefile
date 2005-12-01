# Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
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

APPVER=4.0
TARGETOS=WIN95
_WIN32_IE=0x0500
SEHMAP = TRUE
!include <win32.mak>

DLLFILENAME=hyzlib.dll# declaration

DLLNAME=..\hyzlib.dll# declaration

LIBNAME=hyzlib# declaration

LIBPATH=..\lib\# declaration

.c.obj:
	$(cc) -DWINVER=0x0400 -D_WIN32_WINNT=0x0400 $(cflags) -D_MT -D_DLL -MD -D_WINSOCKAPI_ -DWIN32 -Ogityb1 -Gs -GF -Zm400 -W3 -Zi  /I..\include /I. $(VMDEBUG) $*.c

.cpp.obj:
	$(cc) -DWINVER=0x0400 -D_WIN32_WINNT=0x0400 $(cflags) -D_MT -D_DLL -MD -D_WINSOCKAPI_ -DWIN32 -Ogityb1 -Gs -GF -Zm400 -W3 -Zi  /I..\include /I. $(VMDEBUG) $*.cpp

.asm.obj:
	ml /c /Cp /W3 /nologo /coff /Zm /Zd /Zi /Gd $(VMASMDEBUG) -DWIN32  $<

.rc.res:
	rc -I..\include $<

BUILDFILES1 = zlib_copyright.obj adler32.obj compress.obj crc32.obj deflate.obj
BUILDFILES2 = gzio.obj infback.obj inffast.obj inflate.obj inftrees.obj trees.obj
BUILDFILES3 = uncompr.obj zutil.obj

VIRTFILES1 = $(LIBNAME).res

all: \
	 ..\lib\$(LIBNAME).lib $(DLLNAME)

BUILDLIB: $(LIBPATH)$(LIBNAME).lib

$(LIBPATH)$(LIBNAME).lib:\
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(VIRTFILES1) 
	$(implib) /NOLOGO -subsystem:windows -out:$(LIBPATH)$(LIBNAME).lib -def:$(LIBNAME).def -machine:$(CPU) \
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(VIRTFILES1) 

$(DLLNAME): $(LIBPATH)$(LIBNAME).lib \
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(VIRTFILES1) 
	link $(VMLINK) /NOLOGO /debug /opt:icf /opt:ref /INCREMENTAL:NO /NOLOGO -entry:_DllMainCRTStartup@12 -dll /BASE:0x11700000 -machine:$(CPU) \
	/comment:"zlib data compression library Copyright (C) 1995-2003 Jean-loup Gailly and Mark Adler" \
	-subsystem:windows -out:$(DLLNAME) -map:$(LIBNAME).map  \
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(VIRTFILES1)  \
	kernel32.lib  ws2_32.lib advapi32.lib user32.lib gdi32.lib comdlg32.lib winspool.lib  $(LIBPATH)$(LIBNAME).exp

clean:
	-del *.map
	-del *.obj
	-del *.res
	-del *.pdb
	-del ..\lib\$(LIBNAME).lib
	-del ..\lib\$(LIBNAME).exp
	-del ..\$(LIBNAME).pdb
	-del $(DLLNAME)
