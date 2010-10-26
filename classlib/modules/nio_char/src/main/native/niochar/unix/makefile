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
# Makefile for module 'nio_char'
#

HY_BIN=$(HY_TARGET)/build/classlib/nio_char/native/nio_char/
include $(HY_TARGET)/hdk/build/make/defines.mk

# TODO: Fix the warnings see HARMONY-4959 and
# http://www.mail-archive.com/dev@harmony.apache.org/msg10469.html 
WARNFLAGS :=

BUILDFILES = CP_1250.o CP_1251.o CP_1252.o CP_1253.o CP_1254.o CP_1257.o \
  ISO_8859_1.o ISO_8859_2.o ISO_8859_4.o ISO_8859_5.o ISO_8859_7.o \
  ISO_8859_9.o ISO_8859_13.o ISO_8859_15.o KOI8_R.o US_ASCII.o IBM866.o

#additional
BUILDFILES += IBM00858.o IBM01140.o IBM01141.o IBM01142.o IBM01143.o \
  IBM01144.o IBM01145.o IBM01146.o IBM01147.o IBM01148.o IBM01149.o \
  IBM037.o IBM1026.o IBM1047.o IBM273.o IBM277.o IBM278.o IBM280.o \
  IBM284.o IBM285.o IBM297.o IBM420.o IBM424.o IBM437.o IBM500.o IBM775.o \
  IBM850.o IBM852.o IBM855.o IBM857.o IBM860.o IBM861.o IBM862.o IBM863.o \
  IBM864.o IBM865.o IBM868.o IBM869.o IBM870.o IBM871.o IBM918.o IBM_Thai.o \
  ISO_8859_3.o ISO_8859_6.o ISO_8859_8.o windows_1255.o windows_1256.o \
  x_IBM1006.o x_IBM1025.o x_IBM1097.o x_IBM1098.o x_IBM1112.o x_IBM1122.o \
  x_IBM1123.o x_IBM1124.o x_IBM737.o x_IBM856.o x_IBM874.o x_IBM875.o \
  x_IBM922.o x_MacCyrillic.o x_MacGreek.o x_MacTurkish.o Big5.o Big5_HKSCS.o \
  EUC_KR.o GB2312.o GBK.o GB18030.o windows_31j.o x_MS950_HKSCS.o \
  x_windows_949.o

DLLNAME = $(DLLPATH)libhyniochar$(HY_SHLIB_SUFFIX)
MDLLIBFILES += $(LIBPATH)libvmi$(HY_LINKLIB_SUFFIX)

include $(HY_TARGET)/hdk/build/make/rules.mk
