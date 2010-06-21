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
# Makefile for module 'gc_gen_uncomp'
#

HY_BIN=$(HY_TARGET)/build/drlvm/vm/native/gc_gen_uncomp/
include $(HY_TARGET)/hdk/build/make/defines.mk
VM_HOME=../../../../../../../vm/

DEFINES += \
  -DREFS_USE_UNCOMPRESSED -D_IA32_ -DPLATFORM_POSIX \
  -D__SMP__ -DLINUX_TLS_OPT -D_LARGEFILE64_SOURCE \
  -DBUILDING_GC -DREFS_USE_COMPRESSED

OPT += -fexceptions

INCLUDES := \
  -I$(VM_HOME)port/include \
  -I$(VM_HOME)include -I$(VM_HOME)vmcore/include \
  -I$(SHAREDSUB)common \
  $(INCLUDES)

BUILDFILES := \
  common/collection_scheduler.o common/compressed_ref.o \
  common/concurrent_collection_scheduler.o common/gc_block.o \
  common/gc_common.o common/gc_concurrent.o common/gc_for_barrier.o \
  common/gc_for_class.o common/gc_for_vm.o common/gc_metadata.o \
  common/gc_options.o common/hashcode.o common/interior_pointer.o \
  common/large_pages.o common/mark_scan_pool.o common/space_tuner.o \
  common/weak_roots.o \
  finalizer_weakref/finalizer_weakref.o \
  finalizer_weakref/finalizer_weakref_metadata.o \
  gen/gen.o gen/gen_adapt.o gen/gen_stats.o gen/gen_utils.o \
  jni/java_natives.o jni/java_support.o \
  los/free_area_pool.o los/lspace.o los/lspace_alloc_collect.o \
  mark_compact/fallback_mark_scan.o mark_compact/mspace.o \
  mark_compact/mspace_alloc.o mark_compact/mspace_collect_compact.o \
  mark_compact/mspace_extend_compact.o mark_compact/mspace_move_compact.o \
  mark_compact/mspace_slide_compact.o mark_compact/space_tune_mark_scan.o \
  mark_sweep/gc_ms.o mark_sweep/sspace_compact.o \
  mark_sweep/sspace_mark_concurrent.o mark_sweep/wspace.o \
  mark_sweep/wspace_alloc.o mark_sweep/wspace_chunk.o \
  mark_sweep/wspace_compact.o mark_sweep/wspace_concurrent_gc_stats.o \
  mark_sweep/wspace_fallback_mark.o mark_sweep/wspace_mark.o \
  mark_sweep/wspace_mark_mostly_concurrent.o \
  mark_sweep/wspace_mark_otf_concurrent.o mark_sweep/wspace_mark_sweep.o \
  mark_sweep/wspace_sweep.o mark_sweep/wspace_sweep_concurrent.o \
  mark_sweep/wspace_verify.o \
  move_compact/gc_mc.o \
  semi_space/sspace.o semi_space/sspace_alloc.o semi_space/sspace_forward.o \
  semi_space/sspace_gen_ss_pool.o semi_space/sspace_nongen_ss_pool.o \
  thread/collector.o thread/conclctor.o thread/gc_thread.o thread/mutator.o \
  thread/mutator_alloc.o \
  trace_forward/fspace.o trace_forward/fspace_alloc.o \
  trace_forward/fspace_gen_forward_pool.o \
  trace_forward/fspace_nongen_forward_pool.o trace_forward/sspace_temp.o \
  verify/verifier_common.o verify/verifier_metadata.o \
  verify/verifier_scanner.o verify/verify_blocked_space.o \
  verify/verify_concurrent_mark.o verify/verify_gc_effect.o \
  verify/verify_live_heap.o verify/verify_mutator_effect.o

LDFLAGS += -Bsymbolic

DLLNAME = $(DLLPATH)default/libgc_gen_uncomp$(HY_SHLIB_SUFFIX)
EXPNAME = GC_GEN_0.1

include $(HY_HDK)/build/make/rules.mk
