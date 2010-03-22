/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
  
#ifndef _EE_EM_H_
#define _EE_EM_H_

/**
 * @file
 * JIT interface exposed to EM.
 *
 * A just-in-time compiler must implement the given functions to
 * enable EM initialization and de-initialization of JIT compilers or
 * to enable profile collection or profile usage in JIT.
 */

#include "open/types.h"
#include "open/em.h"


#ifdef __cplusplus
extern "C" {
#endif

typedef void* (*vm_adaptor_t)(const char * name);

  /** 
   * Initializes JIT. 
   *
   * The given method is called only once per JIT instance.
   * 
   * @param[in] jit   - the run-time JIT handle used at run-time to refer to 
   *                    the given JIT instance
   * @param[in] name  - the persistent JIT name that the compiler uses to separate 
   *                    its configuration settings from the ones of other JITs 
   */
JITEXPORT void JIT_init(JIT_Handle jit, const char* name, vm_adaptor_t adaptor);

  /**  
   * De-initializes JIT.
   *
   * The given method is called only once per JIT during the system shutdown. 
   *
   * @param[in] jit - the handle of JIT to de-initialize
   *
   * @note The given method is optional.
   */
JITEXPORT void JIT_deinit(JIT_Handle jit);


  /**  
   * Sets the profile access interface to JIT.
   *
   * The EM passes the pointer to the profile access interface 
   * to JIT through the given method.
   *
   * @param[in] jit          - the JIT instance to pass the profile access interface to
   * @param[in] em           - the handle to the EM instance
   * @param[in] pc_interface - the handle to the profile access interface
   *
   * @note The given method is optional. A JIT compiler without profiling
   *        support does not need this method. 
   */
JITEXPORT void JIT_set_profile_access_interface(JIT_Handle jit, EM_Handle em, struct EM_ProfileAccessInterface* pc_interface);
 
  /**
   * Requests JIT to enable profiling of the specified type.
   *
   * EM uses the given method to request JIT to enable profile 
   * collection or profile usage.
   * According to the EM request JIT uses or collects the profile defined 
   * by the <code>role</code> parameter.
   * The profile type and the profile collector are defined by the profile 
   * collector handle.
   *
   * @param[in] jit  - the JIT instance 
   * @param[in] pc   - the handle of the profile collector instance
   * @param[in] role - the role of JIT in profiling defining whether to collect 
   *                   or to use the profile
   *
   * @return  <code>TRUE</code> if JIT does profiling of the <code>pc</code> type 
   *          according to the <code>role</code> parameter; <code>FALSE</code> if 
   *          profiling is not supported.
   *
   * @note The given method is optional. A JIT compiler without profiling
   *       support does not need this method. 
   */
JITEXPORT bool JIT_enable_profiling(JIT_Handle jit, PC_Handle pc, EM_JIT_PC_Role role);



/**
* Notifies JIT that profile is collected.
*
* EM uses this method to notify JIT that profile is collected.
* JIT could use this information to patch profiling counters.
*
* @param[in] jit  - the JIT instance handle
* @param[in] pc   - the handle of the profile collector instance
* @param[in] mh   - the handle of the method with collected profile
*
* @note The given method is optional. Currently only JET supports this method. 
*/
JITEXPORT void JIT_profile_notification_callback(JIT_Handle jit, PC_Handle pc, Method_Handle mh);


#ifdef __cplusplus
}
#endif


#endif


