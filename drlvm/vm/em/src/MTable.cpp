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

#include "MTable.h"
#include <assert.h>
#include <algorithm>
#include "open/vm_method_access.h"
#include "open/vm_class_manipulation.h"

MTable::~MTable() {
    for (FiltersQueue::const_iterator it = methodFilters.begin(), end = methodFilters.end(); it!=end; ++it) {
        MethodFilter* filter = *it;
        delete filter;
    }
    methodFilters.clear();
}

bool isNum(const std::string& str) {
    return str.size() > 0 && str.size() < 10 
        && *std::min_element(str.begin(), str.end()) >= '0'
        && *std::max_element(str.begin(), str.end()) <= '9'; 
}

bool MTable::addMethodFilter(const std::string& configLine) {
    assert(!configLine.empty());
    bool accept = configLine[0]!='-';
    std::string filterString = configLine[0]=='+'||configLine[0]=='-' ? configLine.substr(1): configLine;
    if (filterString[0] >= '0' && filterString[0]<='9') {
        //bytecode size filter or method num filter
        size_t divPos = filterString.find("..");
        std::string start;
        std::string end;
        if (divPos == std::string::npos){
            start = filterString;
            end = filterString;
        } else {;
            start = filterString.substr(0, divPos);
            end = filterString.substr(divPos + 2);
        }
        bool bcFilter = false;
        if (*start.rbegin()=='b' || *start.rbegin()=='B') {
            if (*end.rbegin()!='b' && *end.rbegin()!='B') {
                return false;
            }
            start = start.substr(0, start.length()-1);
            end = end.substr(0, end.length()-1);
            bcFilter = true;
        }
        if (!isNum(start) || !isNum(end)) {
            return false;
        }
        size_t _start = atoi(start.c_str());
        size_t _end= atoi(end.c_str());
        if (_start > _end) {
            return false;
        }
        if (bcFilter) {
            addBCSizeMethodFilter(accept, _start, _end);
        } else {
            addNumRangeMethodFilter(accept, _start, _end);
        }
    } else {
        std::string className, methodName, signature;
        size_t separatorWidth=1;
        size_t classEndPos = filterString.find(".");
        if (classEndPos == std::string::npos) {
            separatorWidth = 2;
            classEndPos = filterString.find("::");
        }
        if (classEndPos == std::string::npos) {
            className = filterString;
        } else {
            className = filterString.substr(0, classEndPos);
            size_t methodEndPos = filterString.find("(", classEndPos+separatorWidth);
            if (methodEndPos==std::string::npos) {
                methodName = filterString.substr(classEndPos+separatorWidth);
            } else {
                methodName = filterString.substr(classEndPos+separatorWidth, methodEndPos - (classEndPos+separatorWidth));
                signature = filterString.substr(methodEndPos);
            }
        }
        addNameMethodFilter(accept, className, methodName, signature);
    }
    return true;
}

void MTable::addNumRangeMethodFilter(bool accept, size_t start, size_t end) {
    assert(start <= end);
    MethodFilter::Mode mode = accept?MethodFilter::MODE_ACCEPT : MethodFilter::MODE_DENY;
    MethodFilter* filter = new NumRangeMethodFilter(mode, start, end);
    methodFilters.push_back(filter);
}

void MTable::addBCSizeMethodFilter(bool accept, size_t start, size_t end) {
    assert(start <= end);
    MethodFilter::Mode mode = accept?MethodFilter::MODE_ACCEPT : MethodFilter::MODE_DENY;
    MethodFilter* filter = new BCSizeMethodFilter(mode, start, end);
    methodFilters.push_back(filter);
}

void MTable::addNameMethodFilter(bool accept, const std::string& className, 
                             const std::string& methodName, const std::string& signature) 
{
    MethodFilter::Mode mode = accept?MethodFilter::MODE_ACCEPT : MethodFilter::MODE_DENY;
    MethodFilter* filter = new NameMethodFilter(mode, className, methodName, signature);
    methodFilters.push_back(filter);
}

bool MTable::acceptMethod(Method_Handle mh, size_t num) const {
    if (!methodFilters.empty()) {
        MethodInfo mInfo(mh, num);
        for (FiltersQueue::const_iterator it = methodFilters.begin(), end = methodFilters.end(); it!=end; ++it) {
            MethodFilter* filter = *it;
            bool matched = filter->matchMethod(mInfo);
            if (!matched) {
                continue;
            }
            return filter->getMode() == MethodFilter::MODE_ACCEPT ? true : false;
        }
    }
    return true;
}

MTable::MethodInfo::MethodInfo(Method_Handle _mh, size_t _num) {
    num = _num;
    mh = _mh;
    Class_Handle ch = method_get_class(mh);
    className = class_get_name(ch);
    methodName = method_get_name(mh);
    signature = method_get_descriptor(mh);
    assert(!className.empty() && !methodName.empty() && !signature.empty());
}


bool MTable::NumRangeMethodFilter::matchMethod(const MTable::MethodInfo& mInfo) const {
    bool matched = mInfo.num >= start && mInfo.num <=end;
    return matched;
}


bool MTable::BCSizeMethodFilter::matchMethod(const MTable::MethodInfo& mInfo) const  {
    size_t size = method_get_bytecode_length(mInfo.mh);
    bool matched = size >=start && size<=end;
    return matched;
}


bool startsWith(const std::string& str, const std::string& prefix) {
    if (str.length() < prefix.length()) {
        return false;
    }
    return std::equal(prefix.begin(), prefix.end(), str.begin());
}

bool MTable::NameMethodFilter::matchMethod(const MTable::MethodInfo& mInfo) const {
    bool matched = true;
    if (!className.empty()) {
        matched = startsWith(mInfo.className, className);
    }
    if (matched && !methodName.empty()) {
        matched = startsWith(mInfo.methodName, methodName);
    }
    if (matched && !signature.empty()) {
        matched = startsWith(mInfo.signature, signature);
    }
    return matched;
}

