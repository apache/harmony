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
#ifndef _EM_INTF_CPP_H_
#define _EM_INTF_CPP_H_


#include "open/types.h"
#include "em_intf.h"
#include "jni_types.h"

class EM {

public:

    virtual ~EM() {};

    virtual bool init() = 0;    
    
    virtual void execute_method(jmethodID methodID, jvalue *return_value, jvalue *args) = 0;

    virtual JIT_Result compile_method(Method_Handle method) = 0;

    virtual bool need_profiler_thread_support() const =0;

    virtual void profiler_thread_timeout()=0;

    virtual int get_profiler_thread_timeout() const =0;

}; //EM

#endif
