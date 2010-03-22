/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#ifndef _EM_PROFILE_ACCESS_H_
#define _EM_PROFILE_ACCESS_H_

#include "open/types.h"
#include "open/em.h"
#include <iostream>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Known profiler types. Each of the profilers
 * is represented with separate interface to 
 * create and to access to profiles.
 */
enum EM_PCTYPE {
  
/** 
 * Entry-backedge profiler.
 * Collects number execution counts for
 * methods entries and backedges if present.
 */
    EM_PCTYPE_ENTRY_BACKEDGE=1,
/** 
 * Edge profiler.
 * Collects profile for method entry and 
 * all edges in <code>IR Control Flow Graph</code>.
 */
    EM_PCTYPE_EDGE=2,

/** 
 * Value profiler.
 * Collects profile for each given instruction.
 */
    EM_PCTYPE_VALUE=3
    
};

/** 
 * A EM interface used to access to profile collectors.
 */
typedef struct EM_ProfileAccessInterface {

/** 
 * Request profile collector type for specified profile collector handle.
 *
 *  @param  _this  - EM instance profile collector belongs to
 *  @param  pc     - profile collector handle we interested in
 *
 *  @return The type of profile collector.
 */
    EM_PCTYPE               (*get_pc_type) (
                                EM_Handle _this,
                                PC_Handle pc
                            );

/** 
 * Request method profile from profile collector.
 *
 * @param _this     - EM instance profile collector belongs to
 * @param pc        - profile collector used to collect profile
 * @param mh        - method we asking profile for
 *
 * @return Method profile handle, that can be used to access
 *         to custom method profile properties with specialized 
 *         profile collector interface.
 */
    Method_Profile_Handle   (*get_method_profile)(
                                EM_Handle _this,
                                PC_Handle pc,
                                Method_Handle mh
                            );

 /** 
  * Request profile collector of specified type and role for a JIT.
  *
  * @param _this          - EM instance profile collector belongs to
  * @param profile_type   - the type of profile collector
  * @param jh             - handle to JIT, profile collector created for
  * @param jit_role       - the role of JIT: the user or supplier of profile
  *
  * @return  The handle to profile collector instance. 
  */

    PC_Handle               (*get_pc)(
                                EM_Handle _this,
                                EM_PCTYPE profile_type,
                                JIT_Handle jh,
                                EM_JIT_PC_Role jit_role
                            );



    // Here follows entry-backedge and edge profilers interfaces.
    // All methods below could be moved into separate EB and Edge 
    // profiler collectors specific files.

/** 
 * Create new entry-backedge profile for a method.
 * Only one profile per method can be created for a single 
 * profile collector instance. 
 */
    Method_Profile_Handle (*eb_profiler_create_profile) (PC_Handle ph, Method_Handle mh);

/** 
 * Request the address of entry counter.
 * JIT configured to generate entry-backedge profile must 
 * emit the code to increment this counter every time a method called.
 */
    void* (*eb_profiler_get_entry_counter_addr)(Method_Profile_Handle mph);

/** 
 * Request the address of backedge counter.
 * JIT configured to generate entry-backedge profile must 
 * emit the code to increment this counter every time any backedge in
 * a method is called.
 */
    void* (*eb_profiler_get_backedge_counter_addr)(Method_Profile_Handle mph);

/** 
 * Check if entry-backedge profiler is configured to work in synchronous mode
 * In synchronous mode JIT is responsible to emit checks that counter's limit
 * is reached for both entry and backedge counters. If limit is reached 
 * <code>eb_profiler_sync_mode_callback</code> must be called directly from 
 * managed code.
 * In asynchronous mode counters checks are done by profile collector in a 
 * separate thread.
 *
 * @sa eb_profiler_sync_mode_callback()
 */
    char (*eb_profiler_is_in_sync_mode)(PC_Handle pch);

/** 
 * If profile collector is in <code>sync</code> mode 
 * JIT must call this method every time the counter limit is reached.
 *
 * @sa eb_profiler_is_in_sync_mode()
 */
    void (*eb_profiler_sync_mode_callback)(Method_Profile_Handle mph);

/** 
 * @return The counter's limit for entry threshold for a given
 *         profile collector.
 */
    U_32 (*eb_profiler_get_entry_threshold)(PC_Handle pch);

/** 
 * @return The counter's limit for backedge threshold for a given
 *         profile collector.
 */
    U_32 (*eb_profiler_get_backedge_threshold)(PC_Handle pch);



    //EDGE profiler interface

    /** 
     * Create an edge profile for a method. 
     * Only one profile per method can be created for a single 
     * profile collector instance.
     *
     * @param ph               - edge profile collector handle
     * @param mh               - method handle to create profile for
     * @param numEdgeCounters  - number of edge counters in a method
     * @param counterKeys      - the keys, or numbers, will be associated with 
     *                           each counter. The key must be used to access to
     *                           counter value
     * @param checksum         - profile checksum
     *
     * @return A handle to access method profile data.
     */
    Method_Profile_Handle (*edge_profiler_create_profile) (PC_Handle ph, Method_Handle mh, U_32 numEdgeCounters, U_32* counterKeys, U_32 checkSum);


    /** 
     * Return number of edge counters in profile.
     */
    U_32 (*edge_profiler_get_num_counters)(Method_Profile_Handle mph);

    /** 
     * Return profile checksum.
     */
    U_32 (*edge_profiler_get_checksum)(Method_Profile_Handle mph);

    /** 
     * Return the address of counter associated with key.
     */
    void* (*edge_profiler_get_counter_addr)(Method_Profile_Handle mph, U_32 key);

    /** 
     * Return the address of entry counter.
     */
    void* (*edge_profiler_get_entry_counter_addr)(Method_Profile_Handle mph);

    /** 
     * Return the entry threshold for profile collector.
     */
    U_32 (*edge_profiler_get_entry_threshold)(PC_Handle pch);
    
    /** 
     * Return the edge threshold for profile collector.
     */

    U_32 (*edge_profiler_get_backedge_threshold)(PC_Handle pch);
    
    // Value profiler interface

    /** 
     * Create an value profile for a method. 
     * Only one profile per method can be created for a single 
     * profile collector instance.
     *
     * @param pch              - value profile collector handle
     * @param mh               - method handle to create profile for
     * @param numKeys          - number of instructions to be profiled
     * @param keys             - the keys, or numbers, will be associated with 
     *                           each instruction. The key must be used to access to
     *                           instruction value
     */
    Method_Profile_Handle (*value_profiler_create_profile) (PC_Handle pch, Method_Handle mh, U_32 numKeys, U_32* keys);
    
    /** 
     * Update frequency or insert the new value of given instruction.
     */
    void (*value_profiler_add_value)(Method_Profile_Handle mph, U_32 instructionKey, POINTER_SIZE_INT valueToAdd);
    
    /** 
     * @return The maximum value(by frequency) of give instruction.
     */
    POINTER_SIZE_INT (*value_profiler_get_top_value) (Method_Profile_Handle mph, U_32 instructionKey);

    void (*value_profiler_dump_values) (Method_Profile_Handle mph, std::ostream& os);

} EM_ProfileAccessInterface;


#ifdef __cplusplus
}
#endif


#endif


