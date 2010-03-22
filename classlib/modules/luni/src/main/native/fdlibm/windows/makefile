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

#
# Makefile for module 'fdlibm'
#

!include <$(HY_HDK)\build\make\defines.mak>

LIBNAME=$(LIBPATH)hyfdlibm.lib

FDLIBM_DIST=..\..\fdlibm_dist\# Comment to stop line continuation

BUILDFILES = \
  $(FDLIBM_DIST)e_acos.obj $(FDLIBM_DIST)e_acosh.obj $(FDLIBM_DIST)e_asin.obj $(FDLIBM_DIST)e_atan2.obj \
  $(FDLIBM_DIST)e_atanh.obj $(FDLIBM_DIST)e_cosh.obj $(FDLIBM_DIST)e_exp.obj $(FDLIBM_DIST)e_fmod.obj \
  $(FDLIBM_DIST)e_gamma.obj $(FDLIBM_DIST)e_gamma_r.obj $(FDLIBM_DIST)e_hypot.obj $(FDLIBM_DIST)e_j0.obj \
  $(FDLIBM_DIST)e_j1.obj $(FDLIBM_DIST)e_jn.obj $(FDLIBM_DIST)e_lgamma.obj $(FDLIBM_DIST)e_lgamma_r.obj \
  $(FDLIBM_DIST)e_log.obj $(FDLIBM_DIST)e_log10.obj $(FDLIBM_DIST)e_pow.obj $(FDLIBM_DIST)e_rem_pio2.obj \
  $(FDLIBM_DIST)e_remainder.obj $(FDLIBM_DIST)e_scalb.obj $(FDLIBM_DIST)e_sinh.obj $(FDLIBM_DIST)e_sqrt.obj \
  $(FDLIBM_DIST)k_cos.obj $(FDLIBM_DIST)k_rem_pio2.obj $(FDLIBM_DIST)k_sin.obj $(FDLIBM_DIST)k_standard.obj \
  $(FDLIBM_DIST)k_tan.obj $(FDLIBM_DIST)s_asinh.obj $(FDLIBM_DIST)s_atan.obj $(FDLIBM_DIST)s_cbrt.obj \
  $(FDLIBM_DIST)s_ceil.obj $(FDLIBM_DIST)s_copysign.obj $(FDLIBM_DIST)s_cos.obj $(FDLIBM_DIST)s_erf.obj \
  $(FDLIBM_DIST)s_expm1.obj $(FDLIBM_DIST)s_fabs.obj $(FDLIBM_DIST)s_finite.obj $(FDLIBM_DIST)s_floor.obj \
  $(FDLIBM_DIST)s_frexp.obj $(FDLIBM_DIST)s_ilogb.obj $(FDLIBM_DIST)s_isnan.obj $(FDLIBM_DIST)s_ldexp.obj \
  $(SHAREDSUB)s_lib_version.obj \
  $(FDLIBM_DIST)s_log1p.obj $(FDLIBM_DIST)s_logb.obj $(FDLIBM_DIST)s_matherr.obj \
  $(FDLIBM_DIST)s_modf.obj $(FDLIBM_DIST)s_nextafter.obj $(FDLIBM_DIST)s_rint.obj $(FDLIBM_DIST)s_scalbn.obj \
  $(FDLIBM_DIST)s_signgam.obj $(FDLIBM_DIST)s_significand.obj $(FDLIBM_DIST)s_sin.obj $(FDLIBM_DIST)s_tan.obj \
  $(FDLIBM_DIST)s_tanh.obj $(FDLIBM_DIST)w_acos.obj $(FDLIBM_DIST)w_acosh.obj $(FDLIBM_DIST)w_asin.obj \
  $(FDLIBM_DIST)w_atan2.obj $(FDLIBM_DIST)w_atanh.obj $(FDLIBM_DIST)w_cosh.obj $(FDLIBM_DIST)w_exp.obj \
  $(FDLIBM_DIST)w_fmod.obj $(FDLIBM_DIST)w_gamma.obj $(FDLIBM_DIST)w_gamma_r.obj $(FDLIBM_DIST)w_hypot.obj \
  $(FDLIBM_DIST)w_j0.obj $(FDLIBM_DIST)w_j1.obj $(FDLIBM_DIST)w_jn.obj $(FDLIBM_DIST)w_lgamma.obj \
  $(FDLIBM_DIST)w_lgamma_r.obj $(FDLIBM_DIST)w_log.obj $(FDLIBM_DIST)w_log10.obj $(FDLIBM_DIST)w_pow.obj \
  $(FDLIBM_DIST)w_remainder.obj $(FDLIBM_DIST)w_scalb.obj $(FDLIBM_DIST)w_sinh.obj $(FDLIBM_DIST)w_sqrt.obj

HYCFLAGS = -Oityb1 -W3 -Fd$(LIBPATH)hyfdlibm.pdb -D_IEEE_LIBM \
	   /I$(SHAREDSUB) /I$(HY_HDK)\include /I$(HY_HDK)\jdk\include

.c.obj:
        $(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $*.c

$(LIBPATH)$(LIBNAME): $(BUILDFILES)
	@echo /NOLOGO -out:$(LIBNAME) >templrf
	@echo $(BUILDFILES) >>templrf
	$(implib) @templrf
	@del templrf

clean:
	-del *.obj dist\*.obj
	-del $(LIBNAME)
	-del $(LIBPATH)hyfdlibm.pdb
        -del $(SHAREDSUB)*.obj

