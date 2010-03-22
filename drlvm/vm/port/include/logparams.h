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
* @author Dmitry B. Yershov
*/  

#ifndef _LOG_PARAMS_H
#define _LOG_PARAMS_H

#include <stdio.h>
#include <iostream>
#include <vector>
#include <string.h>
#include "open/platform_types.h"
#include "port_malloc.h"

using std::string;
using std::vector;

class LogParams {
private:
    vector<string> values;
    const char* def_messageId;
    const char* messageId;
    string result_string;
    int prefix, message_number;
public:

    LogParams(int pref, int mess_num) {
        prefix = pref;
        message_number = mess_num;
        def_messageId = NULL;
        messageId = NULL;
    }

    ~LogParams() {
        STD_FREE((void*)def_messageId);
    }

    VMEXPORT const char* release();

    LogParams& operator<<(const char* message) {
        if (!def_messageId) {
            def_messageId = strdup(message);
        } else {
            values.push_back(string(message));
        }
        return *this;
    }

    LogParams& operator<<(const void* pointer) {
        string logger_string;
        char* buf = (char*)STD_MALLOC(21);
        sprintf(buf, "%p", pointer);
        logger_string += buf;
        STD_FREE(buf);
        values.push_back(logger_string);
        return *this;
    }

    LogParams& operator<<(char c) {
        string logger_string;
        char* buf = (char*)STD_MALLOC(2*sizeof(char));
        sprintf(buf, "%c", c);
        logger_string += buf;
        STD_FREE(buf);
        values.push_back(logger_string);
        return *this;
    }

    LogParams& operator<<(int i) {
        string logger_string;
        char* buf = (char*)STD_MALLOC(21);
        sprintf(buf, "%d", i);
        logger_string += buf;
        STD_FREE(buf);
        values.push_back(logger_string);
        return *this;
    }

    LogParams& operator<<(long i) {
        string logger_string;
        char* buf = (char*)STD_MALLOC(50);
        sprintf(buf, "%ld", i);
        logger_string += buf;
        STD_FREE(buf);
        values.push_back(logger_string);
        return *this;
    }

    LogParams& operator<<(unsigned i) {
        string logger_string;
        char* buf = (char*)STD_MALLOC(50);
        sprintf(buf, "%u", i);
        logger_string += buf;
        STD_FREE(buf);
        values.push_back(logger_string);
        return *this;
    }

    LogParams& operator<<(unsigned long i) {
        string logger_string;
        char* buf = (char*)STD_MALLOC(100);
        sprintf(buf, "%lu", i);
        logger_string += buf;
        STD_FREE(buf);
        values.push_back(logger_string);
        return *this;
    }

    LogParams& operator<<(int64 i) {
        string logger_string;
        char* buf = (char*)STD_MALLOC(100);
        sprintf(buf, "%" FMT64 "d", i);
        logger_string += buf;
        STD_FREE(buf);
        values.push_back(logger_string);
        return *this;
    }

    LogParams& operator<<(uint64 i) {
        string logger_string;
        char* buf = (char*)STD_MALLOC(100);
        sprintf(buf, "%" FMT64 "u", i);
        logger_string += buf;
        STD_FREE(buf);
        values.push_back(logger_string);
        return *this;
    }

    LogParams& operator<<(double d) {
        string logger_string;
        char* buf = (char*)STD_MALLOC(100);
        sprintf(buf, "%lf", d);
        logger_string += buf;
        STD_FREE(buf);
        values.push_back(logger_string);
        return *this;
    }

    typedef std::ios_base& (*iomanip)(std::ios_base&);
    LogParams& operator<<(iomanip UNREF i) {
        //FIXME: NYI
        return *this;
    }

    typedef std::ostream& (*iomanip2)(std::ostream&);
    LogParams& operator<<(iomanip2 UNREF i) {
        //FIXME: NYI
        return *this;
    }
};

#endif /* _LOG_PARAMS_H */
 
