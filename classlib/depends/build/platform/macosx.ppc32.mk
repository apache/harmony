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

DEFINES += -DMACOSX -DMACOSX_PPC32 -DPPC32 -DIPv6_FUNCTION_SUPPORT
PLATFORM += -fno-common
OSLIBS += -ldl -liconv
XLIBS = -L/usr/X11R6/lib -lX11 -lXft -lXext -lXtst -lXrender -lexpat \
        -L/sw/lib/freetype2/lib -lfreetype -lfontconfig
MDLLIBPREFIX =
MDLLIBSUFFIX =
EXERPATHPREFIX = 
DLL_LD = $(CC)
# TOFIX: exports?
DLL_LDFLAGS = -dynamiclib -dynamic -install_name $(@F)

RANLIB=ranlib
