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

# Use cc for assembly compiles on z/OS
AS = cc

# Use cxx/c++ for c++ compiles on z/OS
CXX = cxx

DEFINES += -DZOS -DZOS_S390X -DHYS390X -DHY_ATOE
PLATFORM = -Wc,lp64,xplink,convlit\(ISO8859-1\),FLOAT\(IEEE,FOLD,AFP\) \
           -Wa,goff -Wc,NOANSIALIAS -Wc,DLL,EXPORTALL

CFLAGS += -Wc,"SSCOMM" -Wc,"langlvl(commonc)"
CXXFLAGS += -+ -Wc,"langlvl(extended)"

ASFLAGS += -Wc,lp64,xplink,convlit\(ISO8859-1\)  -Wa,goff -Wc,NOANSIALIAS \
           -Wc,DLL,EXPORTALL -Wa,SYSPARM\(BIT64\) -c
LDFLAGS += -Wl,lp64 -Wl,xplink,dll

# No need for --start-group and --end-group tags here
MDLLIBPREFIX =
MDLLIBSUFFIX =

# No need to specify STDC libs on z/OS
STDCLIBS =

# Don't use these flags on z/OS
DLL_LDFLAGS =

# We can't use the -Xlinker options on z/OS
EXERPATHPREFIX = 

# Different compiler on zOS
WARNFLAGS =

# z/OS has different debug flags
HYDEBUGCFLAGS = -g -O0

# On z/OS set DLLPATH to LIBPATH so we link against .x export files in
# $(HY_HDK)/lib instead of directly against the .so libraries.
DLLPATH=$(LIBPATH)
