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
# Makefile for module 'port'
#

APPVER=4.0
TARGETOS=WIN95
_WIN32_IE=0x0500
SEHMAP = TRUE
!include <win32.mak>

DLLFILENAME=hyprt.dll# declaration

DLLNAME=..\hyprt.dll # declaration

LIBNAME=hyprt# declaration

LIBPATH=..\lib\# declaration

.c.obj:
	$(cc) -DWINVER=0x0400 -D_WIN32_WINNT=0x0400 $(cflags) -D_MT -D_DLL -MD -D_WINSOCKAPI_ -DWIN32 -Ogityb1 -Gs -GF -Zm400 -WX -Zi  -DHYPORT_LIBRARY_DEFINE /I..\include $(VMDEBUG) $*.c

.cpp.obj:
	$(cc) -DWINVER=0x0400 -D_WIN32_WINNT=0x0400 $(cflags) -D_MT -D_DLL -MD -D_WINSOCKAPI_ -DWIN32 -Ogityb1 -Gs -GF -Zm400 -WX -Zi  -DHYPORT_LIBRARY_DEFINE /I..\include $(VMDEBUG) $*.cpp

.asm.obj:
	ml /c /Cp /W3 /nologo /coff /Zm /Zd /Zi /Gd $(VMASMDEBUG) -DWIN32  $<

.rc.res:
	rc -I..\include $<

BUILDFILES1 = port_copyright.obj hycpu.obj hyerror.obj hyerrorhelpers.obj hyexit.obj
BUILDFILES2 = hyfile.obj hyfiletext.obj hygp.obj hyipcmutex.obj hymem.obj
BUILDFILES3 = hymmap.obj hynls.obj hynlshelpers.obj hyosdump.obj hyport.obj
BUILDFILES4 = hyportcontrol.obj hyportptb.obj hyshmem.obj hyshsem.obj hysignal.obj
BUILDFILES5 = hysl.obj hysock.obj hystr.obj hystrftime.obj hystsl.obj
BUILDFILES6 = hysysinfo.obj hytime.obj hytlshelpers.obj hytty.obj hyvmem.obj

VIRTFILES1 = hyprt.res

SYSLIBFILES1 = ws2_32.lib Iphlpapi.lib shell32.lib

MDLLIBFILES1 = ..\lib\hythr.lib ..\lib\hycommon.lib ..\lib\hysig.lib

all: \
	 ..\lib\$(LIBNAME).lib $(DLLNAME)

BUILDLIB: $(LIBPATH)$(LIBNAME).lib

$(LIBPATH)$(LIBNAME).lib:\
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(BUILDFILES4) $(BUILDFILES5) \
	$(BUILDFILES6) $(VIRTFILES1) $(MDLLIBFILES1) 
	$(implib) /NOLOGO -subsystem:windows -out:$(LIBPATH)$(LIBNAME).lib -def:$(LIBNAME).def -machine:$(CPU) \
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(BUILDFILES4) $(BUILDFILES5) \
	$(BUILDFILES6) $(VIRTFILES1) $(MDLLIBFILES1) 


$(DLLNAME): $(LIBPATH)$(LIBNAME).lib \
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(BUILDFILES4) $(BUILDFILES5) \
	$(BUILDFILES6) $(VIRTFILES1) $(MDLLIBFILES1) 
	link $(VMLINK) /NOLOGO /debug /opt:icf /opt:ref /INCREMENTAL:NO /NOLOGO -entry:_DllMainCRTStartup@12 \
         -dll /BASE:0x11100000 -machine:$(CPU) \
	/comment:"Platform port library. (c) Copyright 1993, 2005 The Apache Software Foundation or its licensors, as applicable." \
	-subsystem:windows -out:$(DLLNAME) -map:$(LIBNAME).map Delayimp.lib -delayload:shell32.dll -delayload:Iphlpapi.dll \
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(BUILDFILES4) $(BUILDFILES5) \
	$(BUILDFILES6) $(VIRTFILES1) $(MDLLIBFILES1) $(SYSLIBFILES1)  \
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
	
