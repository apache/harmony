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

HY_BIN=$(HY_TARGET)/build/classlib/luni/native/fdlibm/
include $(HY_TARGET)/hdk/build/make/defines.mk

OSS_DIST=$(HY_TARGET)/depends/native/fdlibm_dist/# Path to fdlibm

BUILDFILES = \
  e_acos.o e_acosh.o e_asin.o e_atan2.o e_atanh.o e_cosh.o e_exp.o e_fmod.o \
  e_gamma.o e_gamma_r.o e_hypot.o e_j0.o e_j1.o e_jn.o e_lgamma.o \
  e_lgamma_r.o e_log.o e_log10.o e_pow.o e_rem_pio2.o e_remainder.o e_scalb.o \
  e_sinh.o e_sqrt.o \
  k_cos.o k_rem_pio2.o k_sin.o k_standard.o k_tan.o \
  s_asinh.o s_atan.o s_cbrt.o s_ceil.o s_copysign.o s_cos.o s_erf.o s_expm1.o \
  s_fabs.o s_finite.o s_floor.o s_frexp.o s_ilogb.o s_isnan.o s_ldexp.o \
  s_lib_version.o s_log1p.o s_logb.o s_matherr.o s_modf.o s_nextafter.o \
  s_rint.o s_scalbn.o s_signgam.o s_significand.o s_sin.o s_tan.o s_tanh.o \
  w_acos.o w_acosh.o w_asin.o w_atan2.o w_atanh.o w_cosh.o w_exp.o w_fmod.o \
  w_gamma.o w_gamma_r.o w_hypot.o w_j0.o w_j1.o w_jn.o w_lgamma.o w_lgamma_r.o \
  w_log.o w_log10.o w_pow.o w_remainder.o w_scalb.o w_sinh.o w_sqrt.o

DEFINES += -D_IEEE_LIBM

LIBNAME = $(LIBPATH)libhyfdlibm.a

# OPT is overriden except on zOS (fpic, for example, is not an option on zOS)
ifneq ($(HY_OS),zos)
ifneq ($(HY_OS),aix)
OPT = -O0 -ffloat-store
endif
endif
PLATFORM := $(subst -fpic,-fPIC,$(PLATFORM))

include $(HY_TARGET)/hdk/build/make/rules.mk
