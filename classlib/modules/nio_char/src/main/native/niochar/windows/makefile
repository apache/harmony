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

!include <$(HY_HDK)\build\make\defines.mak>

LIBBASE=hyniochar
DLLNAME=$(DLLPATH)$(LIBBASE).dll
LIBNAME=$(LIBPATH)$(LIBBASE).lib
HYCFLAGS = $(HYCFLAGS) /I$(SHAREDSUB) /I$(SHARED)common /I$(SHARED)fdlibm
HYLDFLAGS = $(HYLDFLAGS) -def:$(LIBBASE).def 
ADDITIONAL = $(SHAREDSUB)\additional\#

BUILDFILES = \
  $(SHAREDSUB)CP_1250.obj \
  $(SHAREDSUB)CP_1251.obj \
  $(SHAREDSUB)CP_1252.obj \
  $(SHAREDSUB)CP_1253.obj \
  $(SHAREDSUB)CP_1254.obj \
  $(SHAREDSUB)CP_1257.obj \
  $(SHAREDSUB)ISO_8859_1.obj \
  $(SHAREDSUB)ISO_8859_2.obj \
  $(SHAREDSUB)ISO_8859_4.obj \
  $(SHAREDSUB)ISO_8859_5.obj \
  $(SHAREDSUB)ISO_8859_7.obj \
  $(SHAREDSUB)ISO_8859_9.obj \
  $(SHAREDSUB)ISO_8859_13.obj \
  $(SHAREDSUB)ISO_8859_15.obj \
  $(SHAREDSUB)KOI8_R.obj \
  $(SHAREDSUB)US_ASCII.obj \
  $(SHAREDSUB)IBM866.obj \
  $(ADDITIONAL)IBM00858.obj \
  $(ADDITIONAL)IBM01140.obj \
  $(ADDITIONAL)IBM01141.obj \
  $(ADDITIONAL)IBM01142.obj \
  $(ADDITIONAL)IBM01143.obj \
  $(ADDITIONAL)IBM01144.obj \
  $(ADDITIONAL)IBM01145.obj \
  $(ADDITIONAL)IBM01146.obj \
  $(ADDITIONAL)IBM01147.obj \
  $(ADDITIONAL)IBM01148.obj \
  $(ADDITIONAL)IBM01149.obj \
  $(ADDITIONAL)IBM037.obj \
  $(ADDITIONAL)IBM1026.obj \
  $(ADDITIONAL)IBM1047.obj \
  $(ADDITIONAL)IBM273.obj \
  $(ADDITIONAL)IBM277.obj \
  $(ADDITIONAL)IBM278.obj \
  $(ADDITIONAL)IBM280.obj \
  $(ADDITIONAL)IBM284.obj \
  $(ADDITIONAL)IBM285.obj \
  $(ADDITIONAL)IBM297.obj \
  $(ADDITIONAL)IBM420.obj \
  $(ADDITIONAL)IBM424.obj \
  $(ADDITIONAL)IBM437.obj \
  $(ADDITIONAL)IBM500.obj \
  $(ADDITIONAL)IBM775.obj \
  $(ADDITIONAL)IBM850.obj \
  $(ADDITIONAL)IBM852.obj \
  $(ADDITIONAL)IBM855.obj \
  $(ADDITIONAL)IBM857.obj \
  $(ADDITIONAL)IBM860.obj \
  $(ADDITIONAL)IBM861.obj \
  $(ADDITIONAL)IBM862.obj \
  $(ADDITIONAL)IBM863.obj \
  $(ADDITIONAL)IBM864.obj \
  $(ADDITIONAL)IBM865.obj \
  $(ADDITIONAL)IBM868.obj \
  $(ADDITIONAL)IBM869.obj \
  $(ADDITIONAL)IBM870.obj \
  $(ADDITIONAL)IBM871.obj \
  $(ADDITIONAL)IBM918.obj \
  $(ADDITIONAL)IBM_Thai.obj \
  $(ADDITIONAL)ISO_8859_3.obj \
  $(ADDITIONAL)ISO_8859_6.obj \
  $(ADDITIONAL)ISO_8859_8.obj \
  $(ADDITIONAL)windows_1255.obj \
  $(ADDITIONAL)windows_1256.obj \
  $(ADDITIONAL)x_IBM1006.obj \
  $(ADDITIONAL)x_IBM1025.obj \
  $(ADDITIONAL)x_IBM1097.obj \
  $(ADDITIONAL)x_IBM1098.obj \
  $(ADDITIONAL)x_IBM1112.obj \
  $(ADDITIONAL)x_IBM1122.obj \
  $(ADDITIONAL)x_IBM1123.obj \
  $(ADDITIONAL)x_IBM1124.obj \
  $(ADDITIONAL)x_IBM737.obj \
  $(ADDITIONAL)x_IBM856.obj \
  $(ADDITIONAL)x_IBM874.obj \
  $(ADDITIONAL)x_IBM875.obj \
  $(ADDITIONAL)x_IBM922.obj \
  $(ADDITIONAL)x_MacCyrillic.obj \
  $(ADDITIONAL)x_MacGreek.obj \
  $(ADDITIONAL)x_MacTurkish.obj \
  $(ADDITIONAL)Big5.obj \
  $(ADDITIONAL)Big5_HKSCS.obj \
  $(ADDITIONAL)EUC_KR.obj \
  $(ADDITIONAL)GB2312.obj \
  $(ADDITIONAL)GBK.obj \
  $(ADDITIONAL)GB18030.obj \
  $(ADDITIONAL)windows_31j.obj \
  $(ADDITIONAL)x_MS950_HKSCS.obj \
  $(ADDITIONAL)x_windows_949.obj 

VIRTFILES = hynio_char.res

MDLLIBFILES = $(MDLLIBFILES) $(LIBPATH)vmi$(HY_LINKLIB_SUFFIX)
  
DLLBASE=0x13200000

!include <$(HY_HDK)\build\make\rules.mak>
