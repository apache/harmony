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
 * @brief Declaration of global static constants.
 */

#if !defined(__SCONSTS_H_INCLUDED__)
#define __SCONSTS_H_INCLUDED__

#include "enc.h"
#include <vector>
using std::vector;

namespace Jitrino {
namespace Jet {

/**
 * @brief Static constants that are available at runtime - helper addresses, 
 *        offsets, etc, and are persistent during the whole JIT lifetime.
 *
 * The class acts mostly as a namespace around the constants. It's not 
 * supposed to create instances of the class, and all its members are static.
 *
 * The names are quite self-descriptive.
 * 
 * rt_ prefix stands for 'runtime'.
 */
class StaticConsts {
public:
    static char *       rt_helper_throw;
    static char *       rt_helper_throw_lazy;
    static char *       rt_helper_throw_linking_exc;

    static char *       rt_helper_new;
    static char *       rt_helper_new_array;
    static char *       rt_helper_aastore;

    static char *       rt_helper_monitor_enter;
    static char *       rt_helper_monitor_exit;

    static char *       rt_helper_class_2_jlc;

    static char *       rt_helper_ldc_string;
    static char *       rt_helper_init_class;
    static char *       rt_helper_multinewarray;
    static char *       rt_helper_get_vtable;
    static char *       rt_helper_checkcast;
    static char *       rt_helper_instanceof;

    static char*        rt_helper_ti_method_exit;
    static char*        rt_helper_ti_method_enter;

    static char*        rt_helper_ti_field_access;
    static char*        rt_helper_ti_field_modification;

    static char*        rt_helper_gc_safepoint;

    static char*        rt_helper_new_withresolve;
    static char*        rt_helper_new_array_withresolve;
    static char*        rt_helper_get_class_withresolve;
    static char*        rt_helper_checkcast_withresolve;
    static char*        rt_helper_instanceof_withresolve;
    static char*        rt_helper_field_get_offset_withresolve;
    static char*        rt_helper_field_get_address_withresolve;
    static char*        rt_helper_get_invokevirtual_addr_withresolve;
    static char*        rt_helper_get_invokespecial_addr_withresolve;
    static char*        rt_helper_get_invokestatic_addr_withresolve;
    static char*        rt_helper_get_invokeinterface_addr_withresolve;

    /**
     * @brief An offset of 'thread suspend requiest' flag in TIB.
     * @see rt_helper_get_tls_base_ptr
     * @see rt_helper_gc_safepoint
     * @todo seems unused after the recent ThreadManager changes.
     */
    static unsigned     rt_suspend_req_flag_offset;
    
    /**
     * @brief An address of 'method entry flag'.
     * @see exe_notify_method_enter
     */
    static char*        rt_method_entry_flag_address;
    
    /**
     * @brief An address of 'method exit flag'.
     * @see exe_notify_method_exit
     */
    static char*        rt_method_exit_flag_address;
    
    /**
     * @brief Address of helper that returns a pointer to 
        thread local struct.
     * @param none
     * @return read-only pointer to an I_32 flag.
     */
    static char*        rt_helper_get_tls_base_ptr;

    /**
     * @brief An offset of vtable in the object's header.
     */
    static int          rt_vtable_offset;
    
    /**
     * @brief An offset of array's length in the object's header.
     */
    static unsigned     rt_array_length_offset;
    
    /**
     * @brief \b true when running under debug.
     */
    static bool g_jvmtiMode;
    
    /**
     * @brief A value of null reference (aka 'managed null').
     */
    static const char*  NULL_REF;
    
    /**
     * @brief Objects base for compressed references.
     */
    static const char*  OBJ_BASE;
    
    /**
     * @brief Base for compressed vtables.
     */
    static const char*  VTBL_BASE;
    
    /**
     * @brief true if references are compressed on current platform.
     */
    static bool         g_refs_squeeze;
    
    /**
     * @brief \c true if vtables are compressed on current platform.
     */
    static bool         g_vtbl_squeeze;
    
    /** @brief Predefined integer constant, -1.*/
    static const int                g_iconst_m1;
    /** @brief Predefined integer constant, 0.*/
    static const int                g_iconst_0;
    /** @brief Predefined integer constant, 1.*/
    static const int                g_iconst_1;
    /** @brief Predefined float constant, 0.0.*/
    static const float              g_fconst_0;
    /** @brief Predefined float constant, 1.0.*/
    static const float              g_fconst_1;
    /** @brief Predefined float constant, 2.0.*/
    static const float              g_fconst_2;
    /** @brief Predefined double constant, 0.0.*/
    static const double             g_dconst_0;
    /** @brief Predefined double constant, 1.0.*/
    static const double             g_dconst_1;
    
    /**
     * @brief List of GR-s dedicated for global allocation.
     */
    static vector<AR>               g_global_grs;
    /**
     * @brief List of FR-s dedicated for global allocation.
     */
    static vector<AR>               g_global_frs;
    
protected:
    /**
     * @brief Noop.
     */
    StaticConsts(void) {};
};


}}; // ~namespace Jitrino::Jet

#endif      // ~__SCONSTS_H_INCLUDED__

