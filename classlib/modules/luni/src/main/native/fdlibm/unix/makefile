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

include $(HY_HDK)/build/make/defines.mk

FDLIBM_DIST=../../fdlibm_dist/

BUILDFILES = \
	$(FDLIBM_DIST)e_acos.o $(FDLIBM_DIST)e_acosh.o $(FDLIBM_DIST)e_asin.o $(FDLIBM_DIST)e_atan2.o \
	$(FDLIBM_DIST)e_atanh.o $(FDLIBM_DIST)e_cosh.o $(FDLIBM_DIST)e_exp.o $(FDLIBM_DIST)e_fmod.o \
	$(FDLIBM_DIST)e_gamma.o $(FDLIBM_DIST)e_gamma_r.o $(FDLIBM_DIST)e_hypot.o $(FDLIBM_DIST)e_j0.o \
	$(FDLIBM_DIST)e_j1.o $(FDLIBM_DIST)e_jn.o $(FDLIBM_DIST)e_lgamma.o $(FDLIBM_DIST)e_lgamma_r.o \
	$(FDLIBM_DIST)e_log.o $(FDLIBM_DIST)e_log10.o $(FDLIBM_DIST)e_pow.o $(FDLIBM_DIST)e_rem_pio2.o \
	$(FDLIBM_DIST)e_remainder.o $(FDLIBM_DIST)e_scalb.o $(FDLIBM_DIST)e_sinh.o $(FDLIBM_DIST)e_sqrt.o \
	$(FDLIBM_DIST)k_cos.o $(FDLIBM_DIST)k_rem_pio2.o $(FDLIBM_DIST)k_sin.o $(FDLIBM_DIST)k_standard.o \
	$(FDLIBM_DIST)k_tan.o $(FDLIBM_DIST)s_asinh.o $(FDLIBM_DIST)s_atan.o $(FDLIBM_DIST)s_cbrt.o $(FDLIBM_DIST)s_ceil.o \
	$(FDLIBM_DIST)s_copysign.o $(FDLIBM_DIST)s_cos.o $(FDLIBM_DIST)s_erf.o $(FDLIBM_DIST)s_expm1.o \
	$(FDLIBM_DIST)s_fabs.o $(FDLIBM_DIST)s_finite.o $(FDLIBM_DIST)s_floor.o $(FDLIBM_DIST)s_frexp.o \
	$(FDLIBM_DIST)s_ilogb.o $(FDLIBM_DIST)s_isnan.o $(FDLIBM_DIST)s_ldexp.o \
	$(SHAREDSUB)s_lib_version.o \
	$(FDLIBM_DIST)s_log1p.o $(FDLIBM_DIST)s_logb.o $(FDLIBM_DIST)s_matherr.o $(FDLIBM_DIST)s_modf.o \
	$(FDLIBM_DIST)s_nextafter.o $(FDLIBM_DIST)s_rint.o $(FDLIBM_DIST)s_scalbn.o $(FDLIBM_DIST)s_signgam.o \
	$(FDLIBM_DIST)s_significand.o $(FDLIBM_DIST)s_sin.o $(FDLIBM_DIST)s_tan.o $(FDLIBM_DIST)s_tanh.o \
	$(FDLIBM_DIST)w_acos.o $(FDLIBM_DIST)w_acosh.o $(FDLIBM_DIST)w_asin.o $(FDLIBM_DIST)w_atan2.o \
	$(FDLIBM_DIST)w_atanh.o $(FDLIBM_DIST)w_cosh.o $(FDLIBM_DIST)w_exp.o $(FDLIBM_DIST)w_fmod.o \
	$(FDLIBM_DIST)w_gamma.o $(FDLIBM_DIST)w_gamma_r.o $(FDLIBM_DIST)w_hypot.o $(FDLIBM_DIST)w_j0.o \
	$(FDLIBM_DIST)w_j1.o $(FDLIBM_DIST)w_jn.o $(FDLIBM_DIST)w_lgamma.o $(FDLIBM_DIST)w_lgamma_r.o \
	$(FDLIBM_DIST)w_log.o $(FDLIBM_DIST)w_log10.o $(FDLIBM_DIST)w_pow.o $(FDLIBM_DIST)w_remainder.o \
	$(FDLIBM_DIST)w_scalb.o $(FDLIBM_DIST)w_sinh.o $(FDLIBM_DIST)w_sqrt.o

DEFINES += -D_IEEE_LIBM

LIBNAME = $(LIBPATH)libhyfdlibm.a

# OPT is overriden except on zOS (fpic, for example, is not an option on zOS)
ifneq ($(HY_OS),zos)
ifneq ($(HY_OS),aix)
OPT = -O0 -ffloat-store
endif
endif
PLATFORM := $(subst -fpic,-fPIC,$(PLATFORM))

include $(HY_HDK)/build/make/rules.mk
