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
 * @author Alexander Astapchuk
 */
 
/**
 * @file
 * @brief Implementation of statistics utilities declared in stats.h.
 */
 
#include <stdio.h>
#include <assert.h>
#include <vector>
#include <algorithm>
#include "stats.h"

#include "trace.h"

namespace Jitrino {
namespace Jet {

// Variables placed on new line to avoid Doxygen warning " 
// 'Found ';' while parsing initializer list!'

STATS_ITEM(unsigned)    
        Stats::methodsCompiled = 0;
STATS_ITEM(unsigned)
        Stats::methodsCompiledSeveralTimes = 0;
STATS_ITEM(unsigned)
        Stats::methodsWOCatchHandlers = 0;
STATS_ITEM(unsigned)
        Stats::opcodesSeen[OPCODE_COUNT];
STATS_ITEM(unsigned)
        Stats::npesEliminated = 0;
STATS_ITEM(unsigned)
        Stats::npesPerformed = 0;


const char * Stats::g_name_filter = NULL;

DEF_MIN_MAX_VALUE(bc_size);
DEF_MIN_MAX_VALUE(code_size);
DEF_MIN_MAX_VALUE(native_per_bc_ratio);
DEF_MIN_MAX_VALUE(jstack);
DEF_MIN_MAX_VALUE(locals);
DEF_MIN_MAX_VALUE(bbs);
DEF_MIN_MAX_VALUE(bb_size);
DEF_MIN_MAX_VALUE(patchItemsToBcSizeRatioX1000);


#ifdef JIT_STATS

bool opcode_stat_comparator(const ::std::pair<unsigned, unsigned>& one,
                            const ::std::pair<unsigned, unsigned>& two)
{
    return one.first > two.first;
}


void Stats::dump(void)
{
    dbg("\n");
#if 0 // takes too many space on console, rarely used. uncomment when needed
    //
    // Dump how many opcode were seen
    //

    // a .first is counter, .second is JavaByteCodes value
    ::std::vector< ::std::pair<unsigned, unsigned> > bclist;
    for( unsigned i=0; i<OPCODE_COUNT; i++ ) {
        if( opcodesSeen[i] == 0 )   continue;
        bclist.push_back( ::std::pair<unsigned, unsigned>(opcodesSeen[i], i) );
    };
    ::std::sort( bclist.begin(), bclist.end(), opcode_stat_comparator );

    const unsigned COLS = 3;
    const unsigned ROWS = (bclist.size()+COLS-1)/COLS;

    unsigned totalInsts = 0;
    
    for( unsigned i=0; i<ROWS; i++ ) {
        char buf[1024] = {0}, tmp[100];
        const unsigned row_start = i;
        for( unsigned j=0; j<COLS; j++ ) {
            tmp[0] = '\0';
            unsigned idx = row_start + ROWS*j;
            if( idx >= OPCODE_COUNT || idx >= bclist.size() ) continue;
            unsigned opcode = bclist[idx].second;
            sprintf(tmp, "|%-15s%10u", instrs[opcode].name, bclist[idx].first);
            strcat(buf, tmp);
            totalInsts += bclist[idx].first;
        }
        dbg("%s|\n", buf);
    }
    dbg("Instructions total: %u\n", totalInsts);
#endif // if 0

    //
    //
    //
    dbg("Total methods compiled : %d\n", methodsCompiled);
    dbg("compiled several times : %d\n", methodsCompiledSeveralTimes);
    dbg("Methods without catch handlers: %u\n", methodsWOCatchHandlers);
    
    if (!methodsCompiled) {
        methodsCompiled = 1; // to avoid many checks for div-by-zero below
    }
    dbg("NPE checks eliminated : %d\n", npesEliminated);
    dbg("NPE checks performed  : %d\n", npesPerformed);
    
    
#define PRINT_MIN_MAX_VALUE( what ) \
    dbg("%-20s max=%9u | average=%9u | min=%9u | total=%9u\n", \
    #what ":", what##_max, what##_total/methodsCompiled, what##_min, what##_total);

    PRINT_MIN_MAX_VALUE(bc_size);
    PRINT_MIN_MAX_VALUE(code_size);
    PRINT_MIN_MAX_VALUE(native_per_bc_ratio);
    PRINT_MIN_MAX_VALUE(jstack);
    PRINT_MIN_MAX_VALUE(locals);
    PRINT_MIN_MAX_VALUE(bbs);
    PRINT_MIN_MAX_VALUE(bb_size);
    PRINT_MIN_MAX_VALUE(patchItemsToBcSizeRatioX1000);
    
    if (NULL == g_name_filter) {
        dbg("no name filtering\n");
    }
    else {
        dbg("name filter: %s\n", g_name_filter);
    }
};

#endif  //#ifdef JIT_STATS


}}; // ~namespace Jitrino::Jet
