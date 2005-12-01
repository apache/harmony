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
# Makefile for module 'math'
#

include ../makefile.include

DLLFILENAME=libhymath.so# declaration

DLLNAME=../libhymath.so# declaration

LIBNAME=hymath# declaration

LIBPATH=../lib/# declaration

CFLAGS= -fpic  -DLINUX -D_REENTRANT -O1 -march=pentium3 -DIPv6_FUNCTION_SUPPORT  -DHYX86 -I../include -I../math -I../common -I../zlib -I../zip -I../fdlibm  $(VMDEBUG)


.SUFFIXES:.cpp
.cpp.o:
	$(CXX)  -fpic  -DLINUX -D_REENTRANT -O1 -march=pentium3 -fno-exceptions -fno-rtti -DIPv6_FUNCTION_SUPPORT  -DHYX86 -I../include -I../math -I../common -I../zlib -I../zip -I../fdlibm  $(VMDEBUG) -c $<

ASFLAGS= -o $@

.SUFFIXES:.asm
.asm.o:
	perl ../masm2gas/masm2gas.pl   -I../include -I../math -I../common -I../zlib -I../zip -I../fdlibm $*.asm
	$(AS) $(ASFLAGS) -o $*.o $*.s
	-rm $*.s

BUILDFILES1 = math_copyright.o fltparse.o mathglob.o dblparse.o bigint.o
BUILDFILES2 = cbigint.o

MDLLIBFILES1 = ../lib/libhycommon.a ../libhysig.so ../lib/libhyzip.a ../libhyzlib.so
MDLLIBFILES2 = ../lib/libhypool.a ../lib/libhyfdlibm.a ../libhythr.so ../libvmi.so

all: \
	 $(DLLNAME)

BUILDLIB: $(DLLNAME)

$(DLLNAME):\
	$(BUILDFILES1) $(BUILDFILES2) $(MDLLIBFILES1) $(MDLLIBFILES2) 
	 $(DLL_LD) -shared -Wl,--version-script,$(LIBNAME).exp -Wl,-soname=$(DLLFILENAME) $(VMLINK) -L.  -L../lib -L.. -o $(DLLNAME) \
	$(BUILDFILES1) $(BUILDFILES2) -Xlinker --start-group \
	-lhycommon \
	-lhysig \
	-lhyzip \
	-lhyzlib \
	-lhycommon \
	-lhypool \
	-lhyfdlibm \
	-lhythr \
	-lvmi -Xlinker --end-group  -lc -lm -ldl

clean:
	-rm -f *.o
	-rm -f ../libhymath.so
