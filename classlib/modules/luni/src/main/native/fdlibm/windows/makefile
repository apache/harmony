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

HY_BIN=$(HY_TARGET)\build\classlib\luni\native\fdlibm\# trailing slash
!include <$(HY_TARGET)\hdk\build\make\defines.mak>

LIBNAME=$(LIBPATH)hyfdlibm.lib

OSS_DIST=$(HY_TARGET)\depends\native\fdlibm_dist\# Path to fdlibm

BUILDFILES = \
  $(HY_BIN)e_acos.obj $(HY_BIN)e_acosh.obj $(HY_BIN)e_asin.obj \
  $(HY_BIN)e_atan2.obj $(HY_BIN)e_atanh.obj $(HY_BIN)e_cosh.obj \
  $(HY_BIN)e_exp.obj $(HY_BIN)e_fmod.obj $(HY_BIN)e_gamma.obj \
  $(HY_BIN)e_gamma_r.obj $(HY_BIN)e_hypot.obj $(HY_BIN)e_j0.obj \
  $(HY_BIN)e_j1.obj $(HY_BIN)e_jn.obj $(HY_BIN)e_lgamma.obj \
  $(HY_BIN)e_lgamma_r.obj $(HY_BIN)e_log.obj $(HY_BIN)e_log10.obj \
  $(HY_BIN)e_pow.obj $(HY_BIN)e_rem_pio2.obj $(HY_BIN)e_remainder.obj \
  $(HY_BIN)e_scalb.obj $(HY_BIN)e_sinh.obj $(HY_BIN)e_sqrt.obj \
  $(HY_BIN)k_cos.obj $(HY_BIN)k_rem_pio2.obj $(HY_BIN)k_sin.obj \
  $(HY_BIN)k_standard.obj $(HY_BIN)k_tan.obj $(HY_BIN)s_asinh.obj \
  $(HY_BIN)s_atan.obj $(HY_BIN)s_cbrt.obj $(HY_BIN)s_ceil.obj \
  $(HY_BIN)s_copysign.obj $(HY_BIN)s_cos.obj $(HY_BIN)s_erf.obj \
  $(HY_BIN)s_expm1.obj $(HY_BIN)s_fabs.obj $(HY_BIN)s_finite.obj \
  $(HY_BIN)s_floor.obj $(HY_BIN)s_frexp.obj $(HY_BIN)s_ilogb.obj \
  $(HY_BIN)s_isnan.obj $(HY_BIN)s_ldexp.obj $(HY_BIN)s_lib_version.obj \
  $(HY_BIN)s_log1p.obj $(HY_BIN)s_logb.obj $(HY_BIN)s_matherr.obj \
  $(HY_BIN)s_modf.obj $(HY_BIN)s_nextafter.obj $(HY_BIN)s_rint.obj \
  $(HY_BIN)s_scalbn.obj $(HY_BIN)s_signgam.obj $(HY_BIN)s_significand.obj \
  $(HY_BIN)s_sin.obj $(HY_BIN)s_tan.obj $(HY_BIN)s_tanh.obj \
  $(HY_BIN)w_acos.obj $(HY_BIN)w_acosh.obj $(HY_BIN)w_asin.obj \
  $(HY_BIN)w_atan2.obj $(HY_BIN)w_atanh.obj $(HY_BIN)w_cosh.obj \
  $(HY_BIN)w_exp.obj $(HY_BIN)w_fmod.obj $(HY_BIN)w_gamma.obj \
  $(HY_BIN)w_gamma_r.obj $(HY_BIN)w_hypot.obj $(HY_BIN)w_j0.obj \
  $(HY_BIN)w_j1.obj $(HY_BIN)w_jn.obj $(HY_BIN)w_lgamma.obj \
  $(HY_BIN)w_lgamma_r.obj $(HY_BIN)w_log.obj $(HY_BIN)w_log10.obj \
  $(HY_BIN)w_pow.obj $(HY_BIN)w_remainder.obj $(HY_BIN)w_scalb.obj \
  $(HY_BIN)w_sinh.obj $(HY_BIN)w_sqrt.obj

HYCFLAGS = -Oityb1 -W3 -Fd$(LIBPATH)hyfdlibm.pdb -D_IEEE_LIBM \
	   /I$(SHAREDSUB) /I$(HY_HDK)\include /I$(HY_HDK)\jdk\include \
	   /I$(OSS_DIST)

all: $(HY_BIN) $(LIBNAME)

$(HY_BIN):
	-mkdir $(HY_BIN)

{$(OSS_DIST)}.c{$(HY_BIN)}.obj:
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

{$(SHAREDSUB)}.c{$(HY_BIN)}.obj:
	$(cc) $(cflags) $(HYCFLAGS) -Fo$*.obj $<

$(LIBNAME): $(BUILDFILES)
	$(implib) /NOLOGO -out:$(LIBNAME) $(BUILDFILES)

clean:
	-del *.obj dist\*.obj
	-del $(LIBNAME)
	-del $(LIBPATH)hyfdlibm.pdb
        -del $(SHAREDSUB)*.obj
