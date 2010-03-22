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
#ifndef _LOGGER_STRING_H
#define _LOGGER_STRING_H

#include "logger.h"
#include "open/platform_types.h"

class LoggerString {
public:
    LoggerString& operator<<(const char* message) {
        log_printf("%s", message);
        return *this;
    }

    LoggerString& operator<<(const void* pointer) {
        log_printf("%p", pointer);
        return *this;
    }

    LoggerString& operator<<(char c) {
        log_printf("%c", c);
        return *this;
    }

    LoggerString& operator<<(int i) {
        log_printf("%d", i);
        return *this;
    }

    LoggerString& operator<<(long i) {
        log_printf("%ld", i);
        return *this;
    }

    LoggerString& operator<<(unsigned i) {
        log_printf("%u", i);
        return *this;
    }

    LoggerString& operator<<(unsigned long i) {
        log_printf("%lu", i);
        return *this;
    }

    LoggerString& operator<<(int64 i) {
        log_printf("%" FMT64 "d", i);
        return *this;
    }

    LoggerString& operator<<(uint64 i) {
        log_printf("%" FMT64 "u", i);
        return *this;
    }

    LoggerString& operator<<(double d) {
        log_printf("%lf", d);
        return *this;
    }
};

#endif /* _LOGGER_STRING_H */
 
