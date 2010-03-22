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

DEFINES += -DMACOSX -DMACOSX_X86_64 -DHYX86_64 -DIPv6_FUNCTION_SUPPORT

# Add the default location of the macports.org install to the includes
INCLUDES += -I/opt/local/include
PLATFORM = -fPIC -fno-common -arch x86_64 -mmacosx-version-min=10.5
ASFLAGS += -arch x86_64
# WARNFLAGS += -Wconversion -Wformat -Wshorten-64-to-32
OSLIBS = -L/opt/local/lib -lc -lm -ldl -liconv
XLIBS = -L/usr/X11R6/lib -lX11 -lXft -lXext -lXtst -lXrender -lexpat \
        -lfreetype -lfontconfig
MDLLIBPREFIX =
MDLLIBSUFFIX =
EXERPATHPREFIX = 
DLL_LD = $(CC)
DLL_LDFLAGS = -dynamiclib -dynamic -install_name $(@F)

RANLIB=ranlib
