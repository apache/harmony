# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#  
#      http://www.apache.org/licenses/LICENSE-2.0
#  
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

!IF "$(HY_OS)-$(HY_ARCH)" == "windows-x86_64" 
ml=ml64
DLLENTRY=
!ELSE
ml=ml
DLLENTRY=@12
!ENDIF

.rc.res:
	rc -I..\include $<

all: $(DLLNAME) $(EXENAME) $(LIBNAME) $(HDKINCLUDES)

!ifdef LIBNAME
$(LIBNAME): $(BUILDFILES) $(VIRTFILES) $(MDLLIBFILES)
	$(implib) /NOLOGO -subsystem:windows -out:$(LIBNAME) \
	$(HYLDFLAGS) -machine:$(CPU) \
	/NODEFAULTLIB:libc \
	$(BUILDFILES) $(VIRTFILES) $(MDLLIBFILES)
!endif

!ifdef DLLNAME
$(DLLNAME): $(LIBNAME)
	-mkdir $(DBGPATH)
	link $(VMLINK) /debug /opt:icf /opt:ref /INCREMENTAL:NO /NOLOGO \
	/NODEFAULTLIB:libcmt /NODEFAULTLIB:libc /FORCE:UNRESOLVED \
	-entry:_DllMainCRTStartup$(DLLENTRY) -dll /BASE:$(DLLBASE) -machine:$(CPU) \
	-subsystem:windows -out:$@ \
	-map:$(LIBPATH)$(*F).map -pdb:$(DBGPATH)$(*F).pdb \
	-manifest:no \
	$(BUILDFILES) $(VIRTFILES) $(MDLLIBFILES) $(SYSLIBFILES) \
	kernel32.lib  msvcrt.lib ws2_32.lib advapi32.lib user32.lib gdi32.lib \
	comdlg32.lib winspool.lib $(LIBPATH)$(*F).exp
	if exist $(LIBPATH)$(*F).manifest \
            mt -manifest $(LIBPATH)$(*F).manifest -outputresource:$(DLLNAME);#2
        -del $(LIBPATH)$(*F).manifest >nul 2>&1
!endif

!ifdef EXENAME
$(EXENAME): $(BUILDFILES) $(VIRTFILES) $(MDLLIBFILES)
	-mkdir $(DBGPATH)
	link /NOLOGO $(EXEFLAGS) /debug /opt:icf /opt:ref $(VMLINK) \
	-out:$(EXENAME) -pdb:$(DBGPATH)$(*F).pdb \
	-manifest:no \
	-machine:$(CPU) setargv.obj  \
	$(BUILDFILES) $(VIRTFILES) $(MDLLIBFILES) $(EXEDLLFILES)
	if exist $(LIBPATH)$(*F).manifest \
            mt -manifest $(LIBPATH)$(*F).manifest -outputresource:$(EXENAME);#1
        -del $(LIBPATH)$(*F).manifest >nul 2>&1
!endif

clean:
    -del $(BUILDFILES) >nul 2>&1
    -del *.res >nul 2>&1
    -del *.pdb >nul 2>&1
    -del $(LIBNAME) >nul 2>&1
    -del $(LIBNAME:.lib=.exp) >nul 2>&1
    -del $(LIBNAME:.lib=.map) >nul 2>&1
    -del $(LIBNAME:.lib=.manifest) >nul 2>&1
    -del $(DLLNAME) >nul 2>&1
    -del $(DBGPATH)$(LIBBASE).pdb >nul 2>&1
    -del $(EXENAME) >nul 2>&1
    -del $(DBGPATH)$(EXEBASE).pdb >nul 2>&1
    -del $(LIBPATH)$(EXEBASE).manifest >nul 2>&1
    -del $(CLEANFILES) >nul 2>&1

        -mkdir $(HY_BIN)

# C rules
{$(HY_PLATFORM)/}.c{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(HY_ARCH)/}.c{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(HY_OS)/}.c{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(SHAREDSUB)}.c{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(SHAREDSUB)additional/}.c{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{.}.c{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(OSS_DIST)}.c{$(HY_BIN)}.obj: # for zlib_dist / fdlibm_dist
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

# C++ rules
{$(HY_ARCH)/}.cpp{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(HY_OS)/}.cpp{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(SHAREDSUB)}.cpp{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{.}.cpp{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

# assembler rules
{$(HY_PLATFORM)/}.asm{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(ml) /Fo$*.obj /c /Cp /W3 /nologo /coff /Zm /Zd /Zi /Gd $(VMASMDEBUG) -DWIN32 $<

{$(HY_ARCH)/}.asm{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(ml) /Fo$*.obj /c /Cp /W3 /nologo /coff /Zm /Zd /Zi /Gd $(VMASMDEBUG) -DWIN32 $<

{$(HY_OS)/}.asm{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(ml) /Fo$*.obj /c /Cp /W3 /nologo /coff /Zm /Zd /Zi /Gd $(VMASMDEBUG) -DWIN32 $<

{$(SHAREDSUB)}.asm{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(ml) /Fo$*.obj /c /Cp /W3 /nologo /coff /Zm /Zd /Zi /Gd $(VMASMDEBUG) -DWIN32 $<

{.}.asm{$(HY_BIN)}.obj:
        -mkdir $(*D)
	$(ml) /Fo$*.obj /c /Cp /W3 /nologo /coff /Zm /Zd /Zi /Gd $(VMASMDEBUG) -DWIN32 $<
