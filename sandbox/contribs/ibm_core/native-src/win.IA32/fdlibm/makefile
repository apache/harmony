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

APPVER=4.0
TARGETOS=WIN95
SEHMAP=TRUE
!include <win32.mak>

LIBNAME=hyfdlibm.lib# declaration

LIBPATH=..\lib\# declaration

BUILDFILES1 = e_acos.obj e_acosh.obj e_asin.obj e_atan2.obj e_atanh.obj
BUILDFILES2 = e_cosh.obj e_exp.obj e_fmod.obj e_gamma.obj e_gamma_r.obj
BUILDFILES3 = e_hypot.obj e_j0.obj e_j1.obj e_jn.obj e_lgamma.obj
BUILDFILES4 = e_lgamma_r.obj e_log.obj e_log10.obj e_pow.obj e_rem_pio2.obj
BUILDFILES5 = e_remainder.obj e_scalb.obj e_sinh.obj e_sqrt.obj k_cos.obj
BUILDFILES6 = k_rem_pio2.obj k_sin.obj k_standard.obj k_tan.obj s_asinh.obj
BUILDFILES7 = s_atan.obj s_cbrt.obj s_ceil.obj s_copysign.obj s_cos.obj
BUILDFILES8 = s_erf.obj s_expm1.obj s_fabs.obj s_finite.obj s_floor.obj
BUILDFILES9 = s_frexp.obj s_ilogb.obj s_isnan.obj s_ldexp.obj s_lib_version.obj
BUILDFILES10 = s_log1p.obj s_logb.obj s_matherr.obj s_modf.obj s_nextafter.obj
BUILDFILES11 = s_rint.obj s_scalbn.obj s_signgam.obj s_significand.obj s_sin.obj
BUILDFILES12 = s_tan.obj s_tanh.obj w_acos.obj w_acosh.obj w_asin.obj
BUILDFILES13 = w_atan2.obj w_atanh.obj w_cosh.obj w_exp.obj w_fmod.obj
BUILDFILES14 = w_gamma.obj w_gamma_r.obj w_hypot.obj w_j0.obj w_j1.obj
BUILDFILES15 = w_jn.obj w_lgamma.obj w_lgamma_r.obj w_log.obj w_log10.obj
BUILDFILES16 = w_pow.obj w_remainder.obj w_scalb.obj w_sinh.obj w_sqrt.obj

.c.obj:
	$(cc) -DWINVER=0x0400 -D_WIN32_WINNT=0x0400 $(cflags) -D_MT -D_DLL -MD -D_WINSOCKAPI_ -DWIN32 -Oityb1 -Gs -GF -Zm400 -W3 -Zi -Fd$(LIBPATH)hyfdlibm.pdb  -D_IEEE_LIBM -DHY_PLATFORM_DOUBLE_ORDER /I..\include $(VMDEBUG) $*.c

.cpp.obj:
	$(cc) -DWINVER=0x0400 -D_WIN32_WINNT=0x0400 $(cflags) -D_MT -D_DLL -MD -D_WINSOCKAPI_ -DWIN32 -Oityb1 -Gs -GF -Zm400 -W3 -Zi -Fd$(LIBPATH)hyfdlibm.pdb  -D_IEEE_LIBM -DHY_PLATFORM_DOUBLE_ORDER /I..\include $(VMDEBUG) $*.cpp

.asm.obj:
	ml /c /Cp /W3 /nologo /coff /Zm /Zd /Zi /Gd $(VMASMDEBUG) -DWIN32   $<

all: $(LIBPATH)$(LIBNAME)

$(LIBPATH)$(LIBNAME):\
	$(BUILDFILES1) $(BUILDFILES2) $(BUILDFILES3) $(BUILDFILES4) $(BUILDFILES5) \
	$(BUILDFILES6) $(BUILDFILES7) $(BUILDFILES8) $(BUILDFILES9) $(BUILDFILES10) \
	$(BUILDFILES11) $(BUILDFILES12) $(BUILDFILES13) $(BUILDFILES14) $(BUILDFILES15) \
	$(BUILDFILES16) 
	@echo /NOLOGO -out:$(LIBPATH)$(LIBNAME) >templrf
	@echo $(BUILDFILES1) >>templrf
	@echo $(BUILDFILES2) >>templrf
	@echo $(BUILDFILES3) >>templrf
	@echo $(BUILDFILES4) >>templrf
	@echo $(BUILDFILES5) >>templrf
	@echo $(BUILDFILES6) >>templrf
	@echo $(BUILDFILES7) >>templrf
	@echo $(BUILDFILES8) >>templrf
	@echo $(BUILDFILES9) >>templrf
	@echo $(BUILDFILES10) >>templrf
	@echo $(BUILDFILES11) >>templrf
	@echo $(BUILDFILES12) >>templrf
	@echo $(BUILDFILES13) >>templrf
	@echo $(BUILDFILES14) >>templrf
	@echo $(BUILDFILES15) >>templrf
	@echo $(BUILDFILES16) >>templrf
	$(implib) @templrf
	@del templrf

clean:
	-del *.obj
	-del $(LIBPATH)$(LIBNAME)
	-del $(LIBPATH)hyfdlibm.pdb
