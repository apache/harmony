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
# Makefile for module 'pool'
#

include ../makefile.include

LIBNAME=libhypool.a# declaration

LIBPATH=../lib/# declaration

BUILDFILES1 = pool.o pool_cap.o

CFLAGS= -DLINUX -D_REENTRANT -O1 -march=pentium3 -DIPv6_FUNCTION_SUPPORT  -DHYX86 -I../include  $(VMDEBUG)

.SUFFIXES:.cpp
.cpp.o:
	$(CXX)  -DLINUX -D_REENTRANT -O1 -march=pentium3 -fno-exceptions -fno-rtti -DIPv6_FUNCTION_SUPPORT  -DHYX86 -I../include  $(VMDEBUG) -c $<

ASFLAGS= -o $@

.SUFFIXES:.asm
.asm.o:
	perl ../masm2gas/masm2gas.pl   -I../include $*.asm
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
	-rm -f ../lib/libhypool.a
