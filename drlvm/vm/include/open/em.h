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
 * @author Mikhail Y. Fursov
 */  
#ifndef _EM_H_
#define _EM_H_

/**
 * @file
 * Data types and handles used in EM interfaces.
 *
 */

#ifdef __cplusplus
extern "C" {
#endif

#define OPEN_EM "em"
#define OPEN_EM_VERSION "1.0"

#include "rt_types.h"

  /**
   * The handle to the EM instance. 
   */
typedef void *EM_Handle;
  /** 
   * The handle to the profile collector instance. 
   */
typedef void *PC_Handle;
  /** 
   * The handle to the method profile collected by a specific collector. 
   */
typedef void *Method_Profile_Handle;

  /**
   *  Enumeration of result values can be returned by JIT
   *  on a method compilation request.
   */
typedef enum JIT_Result {
  /** 
   * The method compilation has finished successfully.
   */
    JIT_SUCCESS,
  /** 
   * The method compilation has failed.
   */
    JIT_FAILURE
} JIT_Result; //JIT_Result

  /**
   *  Enumeration of JIT roles related to profiling.
   *  EM configures JIT during startup to generate or to 
   *  use a specific profile collector by providing
   *  the handle to the profile collector and the role of JIT.
   */
typedef enum EM_JIT_PC_Role {
  /**  
   * The JIT role is to generate the profile defined 
   * by the given profile collector type. 
   */
    EM_JIT_PROFILE_ROLE_GEN=1,
  /**  
   * The JIT role is to use the profile defined 
   * by the given profile collector type.
   */
    EM_JIT_PROFILE_ROLE_USE=2
} EM_JIT_PC_Role;


#ifdef __cplusplus
}
#endif


#endif
