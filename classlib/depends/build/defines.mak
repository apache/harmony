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

!ifndef APPVER
APPVER=4.0 #Default Windows version
!endif

!ifndef TARGETOS
TARGETOS=WIN95 #Compile for Win95 by default
!endif

_WIN32_IE=0x0500
SEHMAP = TRUE
!include <win32.mak>

LIBPATH=$(HY_HDK)\lib\# comment to avoid \ being treated as continuation
DLLPATH=$(HY_HDK)\jdk\jre\bin\# ditto
EXEPATH=$(HY_HDK)\jdk\jre\bin\# ditto
DBGPATH=$(HY_HDK)\jdk\lib\# ditto
SHAREDSUB=..\shared\# ditto

!IF "$(HY_OS)-$(HY_ARCH)" == "windows-x86_64" 
WARNING_LEVEL=W0
PLATFORM_64=-DHYX86_64
!ELSE
WARNING_LEVEL=WX
!ENDIF

HYCOMMONCFLAGS = \
  -$(WARNING_LEVEL) -GF -Gs -MD -Zm400 $(PLATFORM_64) \
  -D_CRT_SECURE_NO_DEPRECATE \
  -D_CRT_SECURE_NO_WARNINGS \
  -D_SCL_SECURE_NO_DEPRECATE \
  -D_DLL -D_MT -D_WINSOCKAPI_ \
  /I$(HY_HDK)\include /I$(HY_HDK)\jdk\include /I.

!ifndef HYDEBUGCFLAGS
HYDEBUGCFLAGS = \
  -Zi -Od
!endif

!ifndef HYRELEASECFLAGS  
HYRELEASECFLAGS = \
  -Ogityb1 -DNDEBUG
!endif

!IF "$(HY_CFG)" == "debug"
HYCFLAGS = $(HYDEBUGCFLAGS) $(HYCOMMONCFLAGS)
!ELSE  
HYCFLAGS = $(HYRELEASECFLAGS) $(HYCOMMONCFLAGS)
!ENDIF
  
MDLLIBFILES = $(LIBPATH)hycommon.lib
  
!IF "$(HY_NO_THR)" == "false"
MDLLIBFILES = $(MDLLIBFILES) $(LIBPATH)hythr.lib
!ELSE
HYCFLAGS = $(HYCFLAGS) -DHY_NO_THR
!ENDIF
  
!IF "$(HY_ZIP_API)" == "true"
HYCFLAGS = $(HYCFLAGS) -DHY_ZIP_API
!ENDIF

