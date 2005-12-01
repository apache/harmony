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
# Makefile for module 'fdlibm'
#

include ../makefile.include

LIBNAME=libhyfdlibm.a# declaration

LIBPATH=../lib/# declaration

BUILDFILES1 = e_acos.o e_acosh.o e_asin.o e_atan2.o e_atanh.o
BUILDFILES2 = e_cosh.o e_exp.o e_fmod.o e_gamma.o e_gamma_r.o
BUILDFILES3 = e_hypot.o e_j0.o e_j1.o e_jn.o e_lgamma.o
BUILDFILES4 = e_lgamma_r.o e_log.o e_log10.o e_pow.o e_rem_pio2.o
BUILDFILES5 = e_remainder.o e_scalb.o e_sinh.o e_sqrt.o k_cos.o
BUILDFILES6 = k_rem_pio2.o k_sin.o k_standard.o k_tan.o s_asinh.o
BUILDFILES7 = s_atan.o s_cbrt.o s_ceil.o s_copysign.o s_cos.o
BUILDFILES8 = s_erf.o s_expm1.o s_fabs.o s_finite.o s_floor.o
BUILDFILES9 = s_frexp.o s_ilogb.o s_isnan.o s_ldexp.o s_lib_version.o
BUILDFILES10 = s_log1p.o s_logb.o s_matherr.o s_modf.o s_nextafter.o
BUILDFILES11 = s_rint.o s_scalbn.o s_signgam.o s_significand.o s_sin.o
BUILDFILES12 = s_tan.o s_tanh.o w_acos.o w_acosh.o w_asin.o
BUILDFILES13 = w_atan2.o w_atanh.o w_cosh.o w_exp.o w_fmod.o
BUILDFILES14 = w_gamma.o w_gamma_r.o w_hypot.o w_j0.o w_j1.o
BUILDFILES15 = w_jn.o w_lgamma.o w_lgamma_r.o w_log.o w_log10.o
BUILDFILES16 = w_pow.o w_remainder.o w_scalb.o w_sinh.o w_sqrt.o

CFLAGS=-O0 -DLINUX -D_REENTRANT -DIPv6_FUNCTION_SUPPORT -DHYX86 -D_IEEE_LIBM -DHY_PLATFORM_DOUBLE_ORDER -I../include -ffloat-store $(VMDEBUG)


.SUFFIXES:.cpp
.cpp.o:
	$(CXX) -O0 -fno-exceptions -fno-rtti -DLINUX -D_REENTRANT -DIPv6_FUNCTION_SUPPORT -DHYX86 -D_IEEE_LIBM -DHY_PLATFORM_DOUBLE_ORDER -I../include -ffloat-store $(VMDEBUG) -c $<

ASFLAGS= -o $@

.SUFFIXES:.asm
.asm.o:
	perl ../masm2gas/masm2gas.pl   -I../include $*.asm
	$(AS) $(ASFLAGS) -o $*.o $*.s
	-rm $*.s

all: $(LIBPATH)$(LIBNAME)

$(LIBPATH)$(LIBNAME):\
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(BUILDFILES4) $(BUILDFILES5) \
	$(BUILDFILES6) $(BUILDFILES7) $(BUILDFILES8) $(BUILDFILES9) $(BUILDFILES10) \
	$(BUILDFILES11) $(BUILDFILES12) $(BUILDFILES13) $(BUILDFILES14) $(BUILDFILES15) \
	$(BUILDFILES16) 
	$(AR) rcv $(LIBPATH)$(LIBNAME) \
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(BUILDFILES4) $(BUILDFILES5) \
	$(BUILDFILES6) $(BUILDFILES7) $(BUILDFILES8) $(BUILDFILES9) $(BUILDFILES10) \
	$(BUILDFILES11) $(BUILDFILES12) $(BUILDFILES13) $(BUILDFILES14) $(BUILDFILES15) \
	$(BUILDFILES16) 

clean:
	-rm -f *.o
	-rm -f ../lib/libhyfdlibm.a
