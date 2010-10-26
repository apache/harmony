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
# Makefile for module 'nio_char'
#

HY_BIN=$(HY_TARGET)\build\classlib\nio_char\native\niochar\# trailing slash
!include <$(HY_TARGET)\hdk\build\make\defines.mak>

LIBBASE=hyniochar
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib
HYCFLAGS = $(HYCFLAGS) /I$(SHAREDSUB) /I$(SHARED)common /I$(SHARED)fdlibm
HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def 
ADDITIONAL = $(SHAREDSUB)\additional\#

BUILDFILES = \
  $(HY_BIN)CP_1250.obj \
  $(HY_BIN)CP_1251.obj \
  $(HY_BIN)CP_1252.obj \
  $(HY_BIN)CP_1253.obj \
  $(HY_BIN)CP_1254.obj \
  $(HY_BIN)CP_1257.obj \
  $(HY_BIN)ISO_8859_1.obj \
  $(HY_BIN)ISO_8859_2.obj \
  $(HY_BIN)ISO_8859_4.obj \
  $(HY_BIN)ISO_8859_5.obj \
  $(HY_BIN)ISO_8859_7.obj \
  $(HY_BIN)ISO_8859_9.obj \
  $(HY_BIN)ISO_8859_13.obj \
  $(HY_BIN)ISO_8859_15.obj \
  $(HY_BIN)KOI8_R.obj \
  $(HY_BIN)US_ASCII.obj \
  $(HY_BIN)IBM866.obj \
  $(HY_BIN)IBM00858.obj \
  $(HY_BIN)IBM01140.obj \
  $(HY_BIN)IBM01141.obj \
  $(HY_BIN)IBM01142.obj \
  $(HY_BIN)IBM01143.obj \
  $(HY_BIN)IBM01144.obj \
  $(HY_BIN)IBM01145.obj \
  $(HY_BIN)IBM01146.obj \
  $(HY_BIN)IBM01147.obj \
  $(HY_BIN)IBM01148.obj \
  $(HY_BIN)IBM01149.obj \
  $(HY_BIN)IBM037.obj \
  $(HY_BIN)IBM1026.obj \
  $(HY_BIN)IBM1047.obj \
  $(HY_BIN)IBM273.obj \
  $(HY_BIN)IBM277.obj \
  $(HY_BIN)IBM278.obj \
  $(HY_BIN)IBM280.obj \
  $(HY_BIN)IBM284.obj \
  $(HY_BIN)IBM285.obj \
  $(HY_BIN)IBM297.obj \
  $(HY_BIN)IBM420.obj \
  $(HY_BIN)IBM424.obj \
  $(HY_BIN)IBM437.obj \
  $(HY_BIN)IBM500.obj \
  $(HY_BIN)IBM775.obj \
  $(HY_BIN)IBM850.obj \
  $(HY_BIN)IBM852.obj \
  $(HY_BIN)IBM855.obj \
  $(HY_BIN)IBM857.obj \
  $(HY_BIN)IBM860.obj \
  $(HY_BIN)IBM861.obj \
  $(HY_BIN)IBM862.obj \
  $(HY_BIN)IBM863.obj \
  $(HY_BIN)IBM864.obj \
  $(HY_BIN)IBM865.obj \
  $(HY_BIN)IBM868.obj \
  $(HY_BIN)IBM869.obj \
  $(HY_BIN)IBM870.obj \
  $(HY_BIN)IBM871.obj \
  $(HY_BIN)IBM918.obj \
  $(HY_BIN)IBM_Thai.obj \
  $(HY_BIN)ISO_8859_3.obj \
  $(HY_BIN)ISO_8859_6.obj \
  $(HY_BIN)ISO_8859_8.obj \
  $(HY_BIN)windows_1255.obj \
  $(HY_BIN)windows_1256.obj \
  $(HY_BIN)x_IBM1006.obj \
  $(HY_BIN)x_IBM1025.obj \
  $(HY_BIN)x_IBM1097.obj \
  $(HY_BIN)x_IBM1098.obj \
  $(HY_BIN)x_IBM1112.obj \
  $(HY_BIN)x_IBM1122.obj \
  $(HY_BIN)x_IBM1123.obj \
  $(HY_BIN)x_IBM1124.obj \
  $(HY_BIN)x_IBM737.obj \
  $(HY_BIN)x_IBM856.obj \
  $(HY_BIN)x_IBM874.obj \
  $(HY_BIN)x_IBM875.obj \
  $(HY_BIN)x_IBM922.obj \
  $(HY_BIN)x_MacCyrillic.obj \
  $(HY_BIN)x_MacGreek.obj \
  $(HY_BIN)x_MacTurkish.obj \
  $(HY_BIN)Big5.obj \
  $(HY_BIN)Big5_HKSCS.obj \
  $(HY_BIN)EUC_KR.obj \
  $(HY_BIN)GB2312.obj \
  $(HY_BIN)GBK.obj \
  $(HY_BIN)GB18030.obj \
  $(HY_BIN)windows_31j.obj \
  $(HY_BIN)x_MS950_HKSCS.obj \
  $(HY_BIN)x_windows_949.obj 

VIRTFILES = hynio_char.res

MDLLIBFILES = $(MDLLIBFILES) $(LIBPATH)vmi$(HY_LINKLIB_SUFFIX)

DLLBASE=0x13200000

!include <$(HY_HDK)\build\make\rules.mak>
