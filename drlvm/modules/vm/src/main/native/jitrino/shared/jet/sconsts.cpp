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
 * @brief Definition of constants declared in sconsts.h.
 */
 
#include "sconsts.h"

#include <cstddef>  // NULL lives here
#include "jdefs.h"  // NOTHING lives there

namespace Jitrino {
namespace Jet {

char *  StaticConsts::rt_helper_throw = NULL;
char *  StaticConsts::rt_helper_throw_lazy = NULL;
char *  StaticConsts::rt_helper_throw_linking_exc = NULL;

char *  StaticConsts::rt_helper_monitor_enter = NULL;
char *  StaticConsts::rt_helper_monitor_exit = NULL;

char *  StaticConsts::rt_helper_class_2_jlc = NULL;

char *  StaticConsts::rt_helper_ldc_string = NULL;
char *  StaticConsts::rt_helper_new = NULL;
char *  StaticConsts::rt_helper_new_array = NULL;
char *  StaticConsts::rt_helper_init_class = NULL;
char *  StaticConsts::rt_helper_aastore = NULL;
char *  StaticConsts::rt_helper_multinewarray = NULL;
char *  StaticConsts::rt_helper_get_vtable = NULL;
char *  StaticConsts::rt_helper_checkcast = NULL;
char *  StaticConsts::rt_helper_instanceof = NULL;

char *  StaticConsts::rt_helper_ti_method_exit = NULL;
char *  StaticConsts::rt_helper_ti_method_enter = NULL;

char *  StaticConsts::rt_helper_ti_field_access= NULL;
char *  StaticConsts::rt_helper_ti_field_modification= NULL;


char *  StaticConsts::rt_helper_gc_safepoint = NULL;
char *  StaticConsts::rt_helper_get_tls_base_ptr= NULL;

char*   StaticConsts::rt_helper_new_withresolve= NULL;
char*   StaticConsts::rt_helper_new_array_withresolve= NULL;
char*   StaticConsts::rt_helper_get_class_withresolve= NULL;
char*   StaticConsts::rt_helper_checkcast_withresolve= NULL;
char*   StaticConsts::rt_helper_instanceof_withresolve= NULL;
char*   StaticConsts::rt_helper_field_get_offset_withresolve = NULL;
char*   StaticConsts::rt_helper_field_get_address_withresolve = NULL;
char*   StaticConsts::rt_helper_get_invokevirtual_addr_withresolve = NULL;
char*   StaticConsts::rt_helper_get_invokespecial_addr_withresolve = NULL;
char*   StaticConsts::rt_helper_get_invokestatic_addr_withresolve = NULL;
char*   StaticConsts::rt_helper_get_invokeinterface_addr_withresolve = NULL;



unsigned StaticConsts::rt_array_length_offset = NOTHING;
unsigned StaticConsts::rt_suspend_req_flag_offset = NOTHING;
char*    StaticConsts::rt_method_entry_flag_address = NULL;
char*    StaticConsts::rt_method_exit_flag_address = NULL;
int StaticConsts::rt_vtable_offset = 0;

bool StaticConsts::g_jvmtiMode = false;

const char* StaticConsts::NULL_REF = NULL;
const char* StaticConsts::OBJ_BASE = NULL;
const char* StaticConsts::VTBL_BASE = NULL;
bool StaticConsts::g_refs_squeeze = false;
bool StaticConsts::g_vtbl_squeeze = false;

const int       StaticConsts::g_iconst_m1 = -1;
const int       StaticConsts::g_iconst_0 = 0;
const int       StaticConsts::g_iconst_1 = 1;

const float     StaticConsts::g_fconst_0 = 0.;
const float     StaticConsts::g_fconst_1 = 1.;
const float     StaticConsts::g_fconst_2 = 2.;

const double    StaticConsts::g_dconst_0 = 0.;
const double    StaticConsts::g_dconst_1 = 1.;

vector<AR>      StaticConsts::g_global_grs;
vector<AR>      StaticConsts::g_global_frs;


}}; // ~namespace Jitrino::Jet
