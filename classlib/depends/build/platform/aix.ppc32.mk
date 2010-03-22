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

CC = xlc
DEFINES += -DAIX -DAIX_PPC32 -DHYPPC32 -D_Xconst="" -DIPv6_FUNCTION_SUPPORT
PLATFORM += -qcpluscmt -q32 -q mbcs -qlanglvl=extended -qarch=ppc -qinfo=pro \
	    -qalias=noansi -D_XOPEN_SOURCE_EXTENDED=1 -D_ALL_SOURCE \
            -D_LARGE_FILES -qsuppress=1500-010
OSLIBS = -lc_r -lC_r -lm -lpthread -liconv
XLIBS = -L/opt/freeware/lib -lX11 -lXft -lfreetype -lfontconfig -lXext -lXtst
STDCLIBS = 
WARNFLAGS =
HYDEBUGCFLAGS = -g -O0
HYRELEASEFLAGS = -s -O3 -DNDEBUG

ARFLAGS = -X32 rcv
ASFLAGS += -a32 -mppc
DLL_LD = $(LD)
DLL_LDFLAGS = -bE:$(EXPFILE)
LDFLAGS += -G -bnoentry -bernotok
EXELDFLAGS = -brtl
EXERPATHPREFIX =
MDLLIBPREFIX =
MDLLIBSUFFIX =
