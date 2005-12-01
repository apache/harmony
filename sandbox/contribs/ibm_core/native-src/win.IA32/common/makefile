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
# Makefile for module 'common'
#

APPVER=4.0
TARGETOS=WIN95
_WIN32_IE=0x0500
SEHMAP = TRUE
!include <win32.mak>

LIBNAME=hycommon.lib# declaration

LIBPATH=..\lib\# declaration

BUILDFILES1 = libglob.obj iohelp.obj locklbl.obj lock386.obj utf8decode.obj utf8encode.obj

SYSLIBFILES1 = ws2_32.lib Iphlpapi.lib

MDLLIBFILES1 = ..\lib\hysig.lib ..\lib\hypool.lib
MDLLIBFILES2 = ..\lib\hyfdlibm.lib ..\lib\hythr.lib ..\lib\vmi.lib

.c.obj:
	$(cc) -DWINVER=0x0400 -D_WIN32_WINNT=0x0400 $(cflags) -D_MT -D_DLL -MD -D_WINSOCKAPI_ -DWIN32 -Ogityb1 -Gs -GF -Zm400 -WX -Zi -Fd$(LIBPATH)hycommon.pdb /I..\include /I..\common /I..\zlib /I..\zip /I..\fdlibm  $(VMDEBUG) $*.c

.cpp.obj:
	$(cc) -DWINVER=0x0400 -D_WIN32_WINNT=0x0400 $(cflags) -D_MT -D_DLL -MD -D_WINSOCKAPI_ -DWIN32 -Ogityb1 -Gs -GF -Zm400 -WX -Zi -Fd$(LIBPATH)hycommon.pdb /I..\include /I..\common /I..\zlib /I..\zip /I..\fdlibm  $(VMDEBUG) $*.cpp

.asm.obj:
	ml /c /Cp /W3 /nologo /coff /Zm /Zd /Zi /Gd  $(VMASMDEBUG) -DWIN32  $<

all: $(LIBPATH)$(LIBNAME)

$(LIBPATH)$(LIBNAME): $(BUILDFILES1) 
	$(implib) /NOLOGO -out:$(LIBPATH)$(LIBNAME) $(BUILDFILES1)

clean:
	-del *.obj
	-del ..\lib\$(LIBNAME)
	-del ..\lib\hycommon.pdb
