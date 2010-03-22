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
 * @author Alexey V. Varlamov
 */  

#ifndef _ASSERTIONS_H_
#define _ASSERTIONS_H_

#include "environment.h"

struct Assertion_Record {
    Assertion_Record *next;
    bool status;
    unsigned len;
    char name[1];
};

enum Assertion_Status {ASRT_DISABLED = -1, ASRT_UNSPECIFIED = 0, ASRT_ENABLED = 1};

struct Assertion_Registry {
    bool enable_system;
    Assertion_Status enable_all; 
    Assertion_Record* classes;
    Assertion_Record* packages;

public:
    Assertion_Registry() {
        enable_system = false;
        enable_all = ASRT_UNSPECIFIED;
        classes = NULL;
        packages = NULL;
    }

    void add_class(const Global_Env* genv, const char* name, unsigned len, bool value);
    void add_package(const Global_Env* genv, const char* name, unsigned len, bool value);
    Assertion_Status get_class_status(const char* name) const;
    Assertion_Status get_package_status(const char* name) const;
    
    //assertions status for all but system classes
    Assertion_Status is_enabled(bool system) const {
        return system ? (enable_system ? ASRT_ENABLED : ASRT_DISABLED) : enable_all;
    }
};

#endif // !_ASSERTIONS_H_
