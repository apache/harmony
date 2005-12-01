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
# Makefile for module 'jsig'
#

include ../makefile.include

DLLFILENAME=libhysig.so# declaration

DLLNAME=../libhysig.so# declaration

LIBNAME=hysig# declaration

LIBPATH=../lib/# declaration

CFLAGS=-fpic -O1 -march=pentium3 -DLINUX -D_REENTRANT -DIPv6_FUNCTION_SUPPORT -DHYX86 -I../include $(VMDEBUG)

.SUFFIXES:.cpp
.cpp.o:
	$(CXX) -fpic -O1 -march=pentium3 -fno-exceptions -fno-rtti -DLINUX -D_REENTRANT -DIPv6_FUNCTION_SUPPORT -DHYX86 -I../include $(VMDEBUG) -c $<

ASFLAGS= -o $@

.SUFFIXES:.asm
.asm.o:
	perl ../masm2gas/masm2gas.pl   -I../include  $*.asm
	$(AS) $(ASFLAGS) -o $*.o $*.s
	-rm $*.s

BUILDFILES1 = jsig_copyright.o jsiglinux.o jsigunix.o

all: \
	 $(DLLNAME)

BUILDLIB: $(DLLNAME)

$(DLLNAME): $(BUILDFILES1) 
	 $(DLL_LD) -shared -Wl,--version-script,$(LIBNAME).exp -Wl,-soname=$(DLLFILENAME) $(VMLINK) -L.  -o $(DLLNAME) \
	$(BUILDFILES1) -Xlinker --start-group -Xlinker --end-group  -lc -lm -ldl

clean:
	-rm -f *.o 
	-rm -f ../libhysig.so
