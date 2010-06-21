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
 * @author Intel, Mikhail Y. Fursov
 *
 */

#ifndef _JITRINO_METHOD_TABLE_H
#define _JITRINO_METHOD_TABLE_H

#include "MemoryManager.h"
#include "Stl.h"

namespace Jitrino {

class MethodDesc;

class Method_Table
{
public:
    Method_Table(MemoryManager& mm, const char *default_envvar, const char *envvarname, bool accept_by_default);
    ~Method_Table() {}
    
    bool accept_this_method(MethodDesc &md);
    bool accept_this_method(const char* classname, const char *methodname, const char *signature);
    bool is_in_list_generation_mode();
    
    enum Decision {
        mt_rejected,
        mt_undecided,
        mt_accepted
    };

    class method_record {
    public:
        method_record() : class_name(NULL), method_name(NULL), signature(NULL), decision(mt_undecided){}
        ~method_record(){}

        char *class_name;
        char *method_name;
        char *signature;
        Decision decision;
    };

    void add_method_record(const char* className, const char* methodName, const char* signature, Decision decision, bool copyVals);
    
private:
    MemoryManager& _mm;

    typedef StlVector<method_record*> Records;
    Records _method_table;
    Records _decision_table;
    Decision _default_decision;
    bool _dump_to_file;
    char *_method_file;

    void init(const char *default_envvar, const char *envvarname);
    void make_filename(char *str, int len);
    bool read_method_table();
};

} //namespace Jitrino 

#endif //_JITRINO_METHOD_TABLE_H
