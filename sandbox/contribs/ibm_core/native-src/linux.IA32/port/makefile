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

include ../makefile.include

DLLFILENAME=libhyprt.so# declaration

DLLNAME=../libhyprt.so# declaration

LIBNAME=hyprt# declaration

LIBPATH=../lib/# declaration

CFLAGS= -fpic  -DLINUX -D_REENTRANT -O1 -march=pentium3 -DIPv6_FUNCTION_SUPPORT  -DHYX86 -DHYPORT_LIBRARY_DEFINE -I../include  $(VMDEBUG)

.SUFFIXES:.cpp
.cpp.o:
	$(CXX)  -fpic  -DLINUX -D_REENTRANT -O1 -march=pentium3 -fno-exceptions -fno-rtti -DIPv6_FUNCTION_SUPPORT  -DHYX86 -DHYPORT_LIBRARY_DEFINE -I../include   $(VMDEBUG) -c $<

ASFLAGS= -o $@

.SUFFIXES:.asm
.asm.o:
	perl ../masm2gas/masm2gas.pl   -I../include  $*.asm
	$(AS) $(ASFLAGS) -o $*.o $*.s
	-rm $*.s

BUILDFILES1 = port_copyright.o hycpu.o hyerror.o hyerrorhelpers.o hyexit.o
BUILDFILES2 = hyfile.o hyfiletext.o hygp.o hyipcmutex.o hymem.o
BUILDFILES3 = hymmap.o hynls.o hynlshelpers.o hyosdump.o hyport.o
BUILDFILES4 = hyportcontrol.o hyportptb.o hysharedhelper.o hyshmem.o hyshsem.o
BUILDFILES5 = hysignal.o hysl.o hysock.o hystr.o hystrftime.o
BUILDFILES6 = hystsl.o hysysinfo.o hytime.o hytlshelpers.o hytty.o
BUILDFILES7 = hyvmem.o

MDLLIBFILES1 = ../libhythr.so ../lib/libhycommon.a ../libhysig.so

all: \
	 $(DLLNAME)

BUILDLIB: $(DLLNAME)

$(DLLNAME):\
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(BUILDFILES4) $(BUILDFILES5) \
	$(BUILDFILES6) $(BUILDFILES7) $(MDLLIBFILES1) 
	 $(DLL_LD) -shared -Wl,--version-script,$(LIBNAME).exp -Wl,-soname=$(DLLFILENAME) $(VMLINK) -L.  -L../lib -L.. -o $(DLLNAME) \
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(BUILDFILES4) $(BUILDFILES5) \
	$(BUILDFILES6) $(BUILDFILES7) -Xlinker --start-group \
	-lhythr \
	-lhycommon \
	-lhysig \
	-Xlinker --end-group  -lc -lm -ldl

quick:
	$(MAKE)

clean:
	-rm -f *.o
	-rm -f ../libhyprt.so
