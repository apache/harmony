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

#ifndef _EM_METHOD_TABLE_   
#define _EM_METHOD_TABLE_

#include "open/vm.h"

#include <vector>
#include <string>

class MTable {
public:
    MTable(){}
    virtual ~MTable();

    bool addMethodFilter(const std::string& configLine);
    
    //both inclusive
    void addNumRangeMethodFilter(bool accept, size_t start, size_t end);
    void addBCSizeMethodFilter(bool accept, size_t start, size_t end);
    void addNameMethodFilter(bool accept, const std::string& className, const std::string& methodName, const std::string& signature);

    bool acceptMethod(Method_Handle mh, size_t num) const;
 
private:
    class MethodInfo {
    public:
        MethodInfo(Method_Handle mh, size_t num);
        
        Method_Handle mh;
        std::string className, methodName, signature;
        size_t num;
    };

    class MethodFilter {
    public:
        enum Mode   {MODE_DENY, MODE_ACCEPT};

        MethodFilter(Mode _mode) : mode(_mode){}
        virtual ~MethodFilter(){}
        
        virtual bool matchMethod(const MethodInfo& mInfo) const = 0;
        
        Mode getMode() const {return mode;}
    protected:
        Mode mode;
    };

    class NumRangeMethodFilter : public MethodFilter {
    public:
        NumRangeMethodFilter(Mode mode, size_t _start, size_t _end)
            : MethodFilter(mode), start(_start), end(_end){}
        
        virtual bool matchMethod(const MethodInfo& mInfo) const;

    protected:
        size_t start, end;
    };

    class NameMethodFilter : public MethodFilter {
    public:
        NameMethodFilter(Mode mode, const std::string& _className,  
            const std::string& _methodName,  const std::string _signature)
            : MethodFilter(mode), className(_className), methodName(_methodName), signature(_signature){}

            virtual bool matchMethod(const MethodInfo& mInfo) const;

    protected:
        std::string className, methodName, signature;
    };
    
    class BCSizeMethodFilter : public MethodFilter {
    public:
        BCSizeMethodFilter(Mode mode, size_t _start, size_t _end)
            : MethodFilter(mode), start(_start), end(_end){}

            virtual bool matchMethod(const MethodInfo& mInfo) const;

    protected:
        size_t start, end;
    };

    typedef std::vector<MethodFilter*> FiltersQueue;
    FiltersQueue methodFilters;
};

bool startsWith(const std::string& str, const std::string& prefix);
bool isNum(const std::string& str);

#endif
