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
 * @brief Statistics utilities declarations.
 */

#if !defined(__STATS_H_INCLUDED__)
#define __STATS_H_INCLUDED__

#include "jdefs.h"
#include <string>


namespace Jitrino {
namespace Jet {

/**
 * Mostly a namespace to keep statistics routines and stuff all together.
 */
class Stats {
private: // only statics
    Stats();
    Stats(const Stats&);
public:
    /**
     * Dummy type whose methods are no-ops.
     *
     * Used to substitute a type of a variable, so any manipulation with 
     * the variable may be left in the code without \#ifdef/\#endif, but 
     * will be removed by an optimizing compiler.
     */
    class EmptyType {
    public:
        // various ctors
        EmptyType() {};
        //EmptyType(const char *) {};
        EmptyType(unsigned) {};
        //EmptyType(jlong) {};
        //EmptyType(float) {};
        //EmptyType(double) {};
        EmptyType(const EmptyType&) {};
        //
        EmptyType& operator=(const EmptyType&) { return *this; };
        EmptyType& operator=(unsigned) { return *this; };
        EmptyType& operator=(const char*) { return *this; };
        //
        bool operator==(const EmptyType&) { return false; };
        bool operator!=(const EmptyType&) { return false; };
        //bool operator<(const EmptyType&) { return false; };
        //bool operator>(const EmptyType&) { return false; };
        bool operator<=(const EmptyType&) { return false; };
        bool operator>=(const EmptyType&) { return false; };
        EmptyType& operator++(void) { return *this; };
        EmptyType& operator++(int) { return *this; };
        //EmptyType& operator+(const EmptyType&) { return *this; };
        EmptyType& operator+=(const EmptyType&) { return *this; };
        //EmptyType& operator-(const EmptyType&) { return *this; };
        //EmptyType& operator-=(const EmptyType&) { return *this; };
        //EmptyType& operator*(const EmptyType&) { return *this; };
        //EmptyType& operator*=(const EmptyType&) { return *this; };
        //EmptyType& operator/(const EmptyType&) { return *this; };
        //EmptyType& operator/=(const EmptyType&) { return *this; };
        operator unsigned() { return 0; };
        //operator int() { return 0; };
    };
#ifdef JIT_STATS
    #define STATS_ITEM(typ) typ
    static void dump(void);
#else
    #define STATS_ITEM(typ) Stats::EmptyType
    static void init( void ) {};
    static void dump( void ) {};
#endif
    /** How many times each opcode was seen during compilation. */
    static STATS_ITEM(unsigned)         opcodesSeen[OPCODE_COUNT];
    /** How many methods compiled. */
    static STATS_ITEM(unsigned)         methodsCompiled;
    /** How many methods were compiled several times. */
    static STATS_ITEM(unsigned)         methodsCompiledSeveralTimes;
    /** Some methods statistics. */
    static STATS_ITEM(unsigned)         methodsWOCatchHandlers;
    /** How many null checks are eliminated. */
    static STATS_ITEM(unsigned)         npesEliminated;
    /** How many null checks were generated. */
    static STATS_ITEM(unsigned)         npesPerformed;
    
    // introduces a filter (as trough the strstr(fully-qualified-name, filter)
    // to collect statistics about
    static const char *                 g_name_filter;
        
#define DEF_MIN_MAX_VALUE(what)    \
    STATS_ITEM(unsigned)        Stats::what##_total = (unsigned)0;    \
    STATS_ITEM(unsigned)        Stats::what##_min = (unsigned)0;      \
    STATS_ITEM(::std::string)   Stats::what##_min_name; \
    STATS_ITEM(unsigned)        Stats::what##_max = (unsigned)0;      \
    STATS_ITEM(::std::string)   Stats::what##_max_name;

#define DECL_MIN_MAX_VALUE(what)    \
    static STATS_ITEM(unsigned)         what##_total;    \
    static STATS_ITEM(unsigned)         what##_min;      \
    static STATS_ITEM(::std::string)    what##_min_name; \
    static STATS_ITEM(unsigned)         what##_max;      \
    static STATS_ITEM(::std::string)    what##_max_name;

#ifdef JIT_STATS
    #define STATS_SET_NAME_FILER(nam) Stats::g_name_filter = nam
    #define STATS_INC(what, howmany)    \
        if ( (NULL == Stats::g_name_filter) || (NULL != strstr(meth_fname(), Stats::g_name_filter)) ) { \
            what += howmany; \
        }
        
    #define STATS_MEASURE_MIN_MAX_VALUE( what, value, nam ) \
    { \
        if ( (NULL == Stats::g_name_filter) || (NULL != strstr(meth_fname(), Stats::g_name_filter)) ) { \
        Stats::what##_total += (value); \
        if( Stats::what##_max < (value) ) { Stats::what##_max = (value); Stats::what##_max_name = nam; }; \
        static bool what##_done = false; \
        if( !what##_done && Stats::what##_min > (value) ) { what##_done = true; Stats::what##_min = (value); Stats::what##_min_name = nam; }; \
        }\
    }
#else   // JIT_STATS
    #define STATS_SET_NAME_FILER(nam)
    #define STATS_INC(what, howmany)    
    #define STATS_MEASURE_MIN_MAX_VALUE( what, value, nam )
#endif  // JIT_STATS
   
    
    // byte code size
    DECL_MIN_MAX_VALUE( bc_size );
    // native code size
    DECL_MIN_MAX_VALUE( code_size );
    // native code size / byte code size ratio
    DECL_MIN_MAX_VALUE( native_per_bc_ratio );
    // stack depth
    DECL_MIN_MAX_VALUE( jstack );
    // number of local slots
    DECL_MIN_MAX_VALUE( locals );
    // number of basic blocks
    DECL_MIN_MAX_VALUE( bbs );
    // size of the basic blocks
    DECL_MIN_MAX_VALUE( bb_size );
    //
    DECL_MIN_MAX_VALUE( patchItemsToBcSizeRatioX1000 );
};


}
} // ~namespace Jitrino::Jet


#endif  // ~__STATS_H_INCLUDED__

