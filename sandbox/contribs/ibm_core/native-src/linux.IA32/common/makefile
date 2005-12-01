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
# Makefile for module 'common'
#

include ../makefile.include

LIBNAME=libhycommon.a# declaration

LIBPATH=../lib/# declaration

BUILDFILES1 = libglob.o iohelp.o utf8encode.o utf8decode.o

MDLLIBFILES1 = ../libjsig.so ../lib/libhyzip.a ../libhyzlib23.so ../lib/libhycommon.a ../lib/libhypool.a
MDLLIBFILES2 = ../lib/libhyfdlibm.a ../libhythr23.so ../libvmi.so

CFLAGS= -DLINUX -D_REENTRANT -O1 -march=pentium3 -DIPv6_FUNCTION_SUPPORT  -DHYX86 -I../include -I../common -I../zlib -I../zip -I../fdlibm  $(VMDEBUG)

.SUFFIXES:.cpp
.cpp.o:
	$(CXX)  -DLINUX -D_REENTRANT -O1 -march=pentium3 -fno-exceptions -fno-rtti -DIPv6_FUNCTION_SUPPORT  -DHYX86 -I../include -I../common -I../zlib -I../zip -I../fdlibm  $(VMDEBUG) -c $<

ASFLAGS= -o $@

.SUFFIXES:.asm
.asm.o:
	perl ../masm2gas/masm2gas.pl   -I../include -I../common -I../zlib -I../zip -I../fdlibm $*.asm
	$(AS) $(ASFLAGS) -o $*.o $*.s
	-rm $*.s

all: $(LIBPATH)$(LIBNAME)

$(LIBPATH)$(LIBNAME):\
	$(BUILDFILES1) 
	$(AR) rcv $(LIBPATH)$(LIBNAME) \
	$(BUILDFILES1) 

quick:
	$(MAKE)

clean:
	-rm -f *.o
	-rm -f ../lib/libhycommon.a
