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
# Makefile for module 'jitrino'
#

HY_BIN=$(HY_TARGET)/build/drlvm/vm/native/jitrino/
include $(HY_TARGET)/hdk/build/make/defines.mk
VM_HOME=../../../../../../../vm/

DEFINES += \
  -DREFS_USE_UNCOMPRESSED -D_IA32_ -DPLATFORM_POSIX \
  -D__SMP__ -DLINUX_TLS_OPT -D_LARGEFILE64_SOURCE \
  -DPROJECT_JITRINO

ifeq ($(HY_CFG),debug)
DEFINES += -DJIT_LOGS -DJIT_STATS
endif

OPT += -fexceptions
CXXFLAGS += -Wno-deprecated -fmessage-length=0 -Wall -Werror -Wno-uninitialized

ifeq ($(HY_ARCH),ia64)
JIT_ARCH := ia64
else
JIT_ARCH := x86_or_x86_64
endif

INCLUDES := -I$(VM_HOME)include -I$(VM_HOME)vmcore/include \
  -I$(VM_HOME)port/include \
  -I$(SHAREDSUB)main -I$(SHAREDSUB)shared -I$(SHAREDSUB)vm \
  -I$(SHAREDSUB)codegenerator -I$(SHAREDSUB)$(JIT_ARCH)/codegenerator \
  -I$(SHAREDSUB)optimizer -I$(SHAREDSUB)dynopt \
  -I$(SHAREDSUB)translator -I$(SHAREDSUB)translator/java \
  $(INCLUDES)

BUILDFILES := \
  codegenerator/RuntimeInterface.o \
  codegenerator/Ia32APIMagics.o codegenerator/Ia32BBPolling.o \
  codegenerator/Ia32BranchTrans.o codegenerator/Ia32CFG.o \
  codegenerator/Ia32CallingConvention.o codegenerator/Ia32CgUtils.o \
  codegenerator/Ia32CodeEmitter.o codegenerator/CodeGenerator_arch.o \
  codegenerator/Ia32CodeLayout.o codegenerator/Ia32CodeLayoutBottomUp.o \
  codegenerator/Ia32CodeLayoutTopDown.o codegenerator/Ia32CodeSelector.o \
  codegenerator/Ia32ComplexAddrFormLoader.o codegenerator/Ia32Constraint.o \
  codegenerator/Ia32ConstraintsResolver.o codegenerator/Ia32CopyExpansion.o \
  codegenerator/Ia32DCE.o codegenerator/Ia32EarlyPropagation.o \
  codegenerator/Ia32Encoder.o codegenerator/Ia32FastArrayFilling.o \
  codegenerator/Ia32GCMap.o codegenerator/Ia32GCSafePoints.o \
  codegenerator/Ia32GlobalPropagation.o codegenerator/Ia32I8Lowerer.o \
  codegenerator/Ia32IRConstants.o codegenerator/Ia32IRManager.o \
  codegenerator/Ia32Inst.o codegenerator/Ia32InstCodeSelector.o \
  codegenerator/Ia32InternalProfiler.o codegenerator/Ia32InternalTrace.o \
  codegenerator/Ia32LightJNI.o codegenerator/Ia32PeepHole.o \
  codegenerator/Ia32Printer.o codegenerator/Ia32ProfileUtils.o \
  codegenerator/Ia32RCE.o codegenerator/Ia32RegAlloc0.o \
  codegenerator/Ia32RegAlloc2.o codegenerator/Ia32RegAlloc3.o \
  codegenerator/Ia32RegAllocCheck.o codegenerator/RuntimeInterface_arch.o \
  codegenerator/Ia32SpillGen.o codegenerator/Ia32StackInfo.o \
  codegenerator/Ia32StackLayout.o codegenerator/Ia32Tls.o \
  codegenerator/Ia32WebMaker.o codegenerator/Ia32i586InstsExpansion.o \
  dynopt/EdgeProfiler.o dynopt/StaticProfiler.o dynopt/ValueProfiler.o \
  main/CompilationContext.o main/JITInstanceContext.o main/Jitrino.o \
  main/Log.o main/PMF.o \
  optimizer/CSEHash.o optimizer/CodeGenerator.o optimizer/CodeSelectors.o \
  optimizer/FastArrayFilling.o optimizer/FlowGraph.o optimizer/HLOAPIMagics.o \
  optimizer/IRBuilder.o optimizer/Inst.o optimizer/Loop.o \
  optimizer/LoopUtils.o optimizer/Opcode.o optimizer/Opnd.o \
  optimizer/abcd/abcdbounds.o optimizer/abcd/classic_abcd.o \
  optimizer/abcd/classic_abcd_solver.o optimizer/abcd/insertpi.o \
  optimizer/aliasanalyzer.o optimizer/codelowerer.o \
  optimizer/constantfolder.o optimizer/dabce.o \
  optimizer/deadcodeeliminator.o optimizer/devirtualizer.o \
  optimizer/escanalyzer.o optimizer/escapeanalyzer.o \
  optimizer/gcmanagedpointeranalyzer.o optimizer/globalcodemotion.o \
  optimizer/globalopndanalyzer.o optimizer/hashvaluenumberer.o \
  optimizer/helper_inliner.o optimizer/inliner.o \
  optimizer/lazyexceptionopt.o optimizer/loop_unroll.o optimizer/memoryopt.o \
  optimizer/multiplybyconstant.o optimizer/optimizer.o optimizer/optpass.o \
  optimizer/osr.o optimizer/pidgenerator.o optimizer/reassociate.o \
  optimizer/simplifier.o optimizer/simplifytaus.o optimizer/ssa/SSA.o \
  optimizer/syncopt.o optimizer/tailduplicator.o optimizer/throwopt.o \
  optimizer/walkers.o \
  shared/Algorithms.o shared/Arena.o shared/BitSet.o shared/ControlFlowGraph.o \
  shared/CountWriters.o shared/Dominator.o shared/FixFileName.o \
  shared/Interval.o shared/LoopTree.o shared/MemoryAttribute.o \
  shared/MemoryManager.o shared/PrintDotFile.o shared/Type.o shared/VMMagic.o \
  shared/XTimer.o shared/methodtable.o shared/mkernel.o \
  translator/TranslatorIntfc.o translator/java/JavaByteCodeParser.o \
  translator/java/JavaByteCodeTranslator.o \
  translator/java/JavaFlowGraphBuilder.o \
  translator/java/JavaLabelPrepass.o translator/java/JavaTranslator.o \
  vm/EMInterface.o vm/JITInterface.o vm/VMInterface.o

ifneq ($(HY_ARCH),ia64)
BUILDFILES += \
  jet/arith_rt.o jet/bcproc.o jet/cg.o jet/cg_arith.o jet/cg_br.o \
  jet/cg_dbg.o jet/cg_fld_arr.o jet/cg_ia32.o jet/cg_instr.o jet/cg_meth.o \
  jet/cg_obj.o jet/cg_regs.o jet/cg_stk.o jet/compiler.o jet/csig.o \
  jet/enc.o jet/enc_ia32.o jet/jdefs.o jet/jet.o jet/jframe.o jet/magics.o \
  jet/mib.o jet/rt.o jet/rt_ia32.o jet/sconsts.o jet/stats.o jet/trace.o
endif

DLLNAME = $(DLLPATH)default/libjitrino$(HY_SHLIB_SUFFIX)
EXPNAME = JITRINO_0.1

#OSLIBS += -lm
MDLLIBFILES += $(LIBPATH)libapr-1.a $(LIBPATH)libport.a

include $(HY_HDK)/build/make/rules.mk

$(HY_BIN)%.o: $(SHAREDSUB)$(JIT_ARCH)/%.cpp
	@mkdir -p $(@D)
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c -o $@ $<
