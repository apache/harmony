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

#ifndef UNIT_TEST_LOGGER_H
#define UNIT_TEST_LOGGER_H

#include <iostream>

#define EXPAND_(a) #a
#define EXPAND(a) EXPAND_(a)
#define __FILELINE__ __FILE__ ":" EXPAND(__LINE__)

#define TICK(x) do { if (log_level <= 1) std::cerr << x << std::flush; } while(0)

/**
 * holds current unit test status value.
 * 0 is okay, non-zero value means failure.
 */
extern int status;

#undef fail
/// Fails a unit test and prints a message
#define fail(x) \
    ++status; \
    WARN2("test",std::endl << x); \
    throw "failed";

#undef assert
/// Makes an assertion
#define assert(x) \
    if (!(x)) { \
        fail("assertion " #x " failed at " __FILELINE__); \
    }

extern int log_level;

#define ECHO2(x,y) std::cerr << x << ": " << y << std::endl
#define TRACE2(x,y) do { if(log_level > 3) ECHO2(x,y); } while(0)
#define LOG2(x,y) do { if (log_level > 2) ECHO2(x,y); } while(0)
#define INFO2(x,y) do { if (log_level > 1) ECHO2(x,y); } while(0)
#define WARN2(x,y) do { if (log_level > 0) ECHO2(x,y); } while(0)

#define ECHO(x) std::cerr << x << std::endl
#define TRACE(x) do { if (log_level > 3) ECHO(x); } while(0)
#define LOG(x) do { if (log_level > 2) ECHO(x); } while(0)
#define INFO(x) do { if (log_level > 1) ECHO(x); } while(0)
#define WARN(x) do { if (log_level > 0) ECHO(x); } while(0)

#endif // UNIT_TEST_LOGGER_H
