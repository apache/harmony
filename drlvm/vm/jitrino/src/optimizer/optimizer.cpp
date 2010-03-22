/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Intel, Pavel A. Ozhdikhin
 *
 */

#include <assert.h>
#include <iostream>

#include "open/types.h"
#include "optimizer.h"
#include "Inst.h"
#include "irmanager.h"
#include "Dominator.h"
#include "Loop.h"

#include "ssa/SSA.h"

#include "Log.h"
#include "deadcodeeliminator.h"
#include "hashvaluenumberer.h"
//#include "escapeanalyzer.h"
#include "escanalyzer.h"
#include "globalopndanalyzer.h"
#include "simplifier.h"
#include "inliner.h"
#include "devirtualizer.h"

#include "dabce.h"

#include "Jitrino.h"
#include "codelowerer.h"
#include "globalcodemotion.h"
#include "tailduplicator.h"
#include "gcmanagedpointeranalyzer.h"
#include "memoryopt.h"
#include "aliasanalyzer.h"
#include "reassociate.h"
#include "syncopt.h"
#include "simplifytaus.h"
#include "pidgenerator.h"
#include "StaticProfiler.h"
#include "lazyexceptionopt.h"
#include "CompilationContext.h"
#include "PMFAction.h"

namespace Jitrino {

#define HIR_NODE_THRESHOLD 1300
#define INLINE_NODE_QUOTA 60  // percents

class OptInitAction : public Action {
public:
    void init() {readFlags();}
    OptimizerFlags optimizerFlags;
private:
    void readFlags();
    void showFlags();
};

class OptInitSession : public SessionAction {
public:
    virtual void run () {
        CompilationContext* cc = getCompilationContext();
        assert(cc->getHIRManager() == NULL);
        MemoryManager& mm = cc->getCompilationLevelMemoryManager();
        OptInitAction* myAction =  (OptInitAction*)getAction();
        OptimizerFlags& flags = myAction->optimizerFlags;
        IRManager* irm = new (mm) IRManager(mm, *cc->getVMCompilationInterface(), flags);
        cc->setHIRManager(irm);
    }

};

static void showFlags(std::ostream& os);

#define OPT_INIT_NAME "opt_init"

class OptInitFactory : public ActionFactory<OptInitSession, OptInitAction> {
public:
    OptInitFactory(const char* name) : ActionFactory<OptInitSession, OptInitAction>(name, NULL){}
    virtual void showHelp (std::ostream& os) {showFlags(os);}
};

static OptInitFactory _opt_init(OPT_INIT_NAME);


void OptInitAction::readFlags()
{
    MemoryManager& mm = getJITInstanceContext().getGlobalMemoryManager();
    memset( &optimizerFlags, 0, sizeof(OptimizerFlags));

    optimizerFlags.dumpdot= getBoolArg("dumpdot", false);
    optimizerFlags.hir_node_threshold = getIntArg("hir_node_threshold", HIR_NODE_THRESHOLD);
    optimizerFlags.inline_node_quota = getIntArg("inline_node_quota", INLINE_NODE_QUOTA);

    optimizerFlags.cse_final = getBoolArg("cse_final", true);

    optimizerFlags.hash_init_factor = getIntArg("hash_init_factor", 1);
    optimizerFlags.hash_resize_factor = getIntArg("hash_resize_factor", 2);
    optimizerFlags.hash_resize_to = getIntArg("hash_resize_to", 3);
    optimizerFlags.hash_node_var_factor = getIntArg("hash_node_var_factor", 1);
    optimizerFlags.hash_node_tmp_factor = getIntArg("hash_node_tmp_factor", 2);
    optimizerFlags.hash_node_constant_factor = getIntArg("hash_node_constant_factor", 1);

    optimizerFlags.sink_constants = getBoolArg("sink_constants", true);
    optimizerFlags.sink_constants1 = getBoolArg("sink_constants1", false);

    
     //simplifier flags
    optimizerFlags.elim_cmp3 = getBoolArg("elim_cmp3", true);
    optimizerFlags.use_mulhi = getBoolArg("use_mulhi", false); 
    optimizerFlags.lower_divconst = getBoolArg("lower_divconst", true);
    optimizerFlags.ia32_code_gen = Jitrino::flags.codegen != Jitrino::CG_IPF;
    optimizerFlags.do_sxt = getBoolArg("do_sxt", true);
    optimizerFlags.reduce_compref = getBoolArg("reduce_compref", false);


    //hvn flags
    optimizerFlags.elim_checks = getBoolArg("elim_checks", true);
    optimizerFlags.gvn_exceptions = getBoolArg("gvn_exceptions", false);
    optimizerFlags.gvn_aggressive = getBoolArg("gvn_aggressive", false);
    optimizerFlags.hvn_constants = getBoolArg("hvn_constants", true);

    //profiler flags    
    optimizerFlags.profile_threshold = getIntArg("profile_threshold", 5000);
    optimizerFlags.use_average_threshold = getBoolArg("use_average_threshold", false);
    optimizerFlags.use_minimum_threshold = getBoolArg("use_minimum_threshold", false);
    optimizerFlags.use_fixed_threshold = getBoolArg("use_fixed_threshold", false);

    
    //dce flags
    optimizerFlags.fixup_ssa = getBoolArg("fixup_ssa", true);
    optimizerFlags.dce2 = getBoolArg("dce2", true);
    optimizerFlags.preserve_critical_edges = getBoolArg("preserve_critical_edges", true);

    //ssa
    optimizerFlags.better_ssa_fixup  = getBoolArg("better_ssa_fixup", false);

    //statprof
    optimizerFlags.statprof_do_loop_heuristics_override = getBoolArg("statprof_do_loop_heuristics_override", true);
    optimizerFlags.statprof_heuristics = getStringArg("statprof_heuristics", NULL);
    
    //gcmptranalyzer
    optimizerFlags.gc_build_var_map = getBoolArg("gc_build_var_map", true);

    //devirtualizer flags
    optimizerFlags.devirt_do_aggressive_guarded_devirtualization = getBoolArg("devirt_aggressive", false);
    optimizerFlags.devirt_skip_exception_path = getBoolArg("devirt_skip_exception_path", true);
    optimizerFlags.devirt_block_hotness_multiplier= (float)getIntArg("devirt_block_hotness_multiplier", 10);
    optimizerFlags.devirt_skip_object_methods = getBoolArg("devirt_skip_object_methods", false);
    optimizerFlags.devirt_intf_calls = getBoolArg("devirt_intf_calls", false);
 
    //unguard
    optimizerFlags.unguard_dcall_percent = getIntArg("unguard_dcall_percent", 30);
    optimizerFlags.unguard_dcall_percent_of_entry= getIntArg("unguard_dcall_percent_of_entry", 10);

    //classic_abcd
    optimizerFlags.dump_abcd_stats = getBoolArg("dump_abcd_stats", false);

    optimizerFlags.gcmFlags = new (mm) GcmFlags;
    memset(optimizerFlags.gcmFlags, 0, sizeof(GcmFlags));
    
    optimizerFlags.memOptFlags = new (mm) MemoptFlags;
    memset(optimizerFlags.memOptFlags, 0, sizeof(MemoptFlags));

    optimizerFlags.syncOptFlags = new (mm) SyncOptFlags;
    memset(optimizerFlags.syncOptFlags, 0, sizeof(SyncOptFlags));

    optimizerFlags.loopBuilderFlags = new (mm) LoopBuilderFlags;
    memset(optimizerFlags.loopBuilderFlags, 0, sizeof(LoopBuilderFlags));

    optimizerFlags.dabceFlags = new (mm) DynamicABCEFlags;
    memset(optimizerFlags.dabceFlags, 0, sizeof(DynamicABCEFlags));

    GlobalCodeMotion::readFlags(this, optimizerFlags.gcmFlags);
    MemoryOpt::readFlags(this, optimizerFlags.memOptFlags);
    SyncOpt::readFlags(this, optimizerFlags.syncOptFlags);
    LoopBuilder::readFlags(this, optimizerFlags.loopBuilderFlags);
    DynamicABCE::readFlags(this, optimizerFlags.dabceFlags);
}


void showFlags(std::ostream& os) {
    os << "\n"<<OPT_INIT_NAME<<std::endl;
    os << "  global optimizer flags:"<<std::endl;
    os << "    hir_node_threshold=<n>      - max number of CFG nodes for inlining" << std::endl;
    os << "    elim_cmp3[={ON|off}]        - eliminate cmp3 tests" << std::endl;
    os << "    elim_checks[={ON|off}]      - try to eliminate some checks using branch conditions" << std::endl;
    os << "    use_mulhi{ON|off}]          - use MulHi opcode" << std::endl;
    os << "    lower_divconst[={ON|off}]   - lower div by constant to mul" << std::endl;
    os << "    cse_final[={ON|off}]        - do cse of final fields " << std::endl;
    os << "    fixup_ssa[={on|OFF}]        - fixup SSA form after code deletion" << std::endl;
    os << "    number_dots[={on|OFF}]      - use a counter in dot file names to show order" << std::endl;
    os << "    dce2[={ON|off}]             - use new version of DCE pass";
    os << "    split_ssa[={ON|off}]        - rename nonoverlapping SSA var versions";
    os << "    better_ssa_fixup[={on|OFF}] - defer ssa fixup until graph change";
    os << "    hvn_exceptions[={ON|off}]   - do value-numbering on exception paths" << std::endl;
    os << "    hvn_constants[={ON|off}]    - value-number constants from equality tests" << std::endl;
    os << "    sink_constants[={ON|off}]   - eliminate globals whose values are constant" << std::endl;
    os << "    sink_constants1[={on|OFF}]  - make sink_constants more aggressive" << std::endl;
    os << "    dump_abcd_stats[={on|OFF}]  - dump (to bounds_checks.log) how many bounds checks "
                                         << "were eliminated per method" << std::endl;

    GlobalCodeMotion::showFlags(os);
    MemoryOpt::showFlags(os);
    SyncOpt::showFlags(os);
    LoopBuilder::showFlags(os);
    DynamicABCE::showFlags(os);
}

} //namespace Jitrino 
