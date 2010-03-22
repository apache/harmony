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

#ifndef UNIT_TEST_H
#define UNIT_TEST_H

/** \file
 * Unit test framework.
 */

#include <iostream>
#include <list>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <winsock2.h>
#include <windows.h>
#endif

#ifdef __linux__
#include <unistd.h>
#include <sys/wait.h>
#endif // __linux__

#ifndef LOG_DOMAIN
#define LOG_DOMAIN "test"
#endif

#include "unit_test_logger.h"

#ifdef _WIN32
/// Translates exception code into string message
inline char* get_exception_message(int code) {
    if (EXCEPTION_ACCESS_VIOLATION == code) {
        return "segmentation fault";
    } else {
        return "exception caught";
    }
}
#endif // _WIN32

#ifdef __linux__
void segfault(int signum) {
    switch (signum) {
        case SIGSEGV: WARN("segmentation fault"); break;
        default: WARN("fatal error");
    }
    exit(1);
}
#endif // __linux__

/// Container of a description of a test case, setup or teardown.
class Descriptor {
    protected:
        void (*func)();
        char* name;
        char* file;
        int line;
    public:
        Descriptor(void (*func)(), char* name, char* file, int line)
            : func(func), name(name), file(file), line(line) {}
        friend std::ostream& operator<<(std::ostream& out, Descriptor* descriptor);
};

inline std::ostream& operator<<(std::ostream& out, Descriptor* descriptor) {
    out << descriptor->name << "() at "
        << descriptor->file << ":" << descriptor->line;
    return out;
}

class SetupDescriptor;

/// A setup function
extern SetupDescriptor* setup_descriptor;

/// Container of a setup function description.
class SetupDescriptor : public Descriptor {
    public:
        SetupDescriptor(void (*func)(), char* name, char* file, int line)
            : Descriptor(func, name, file, line)
        {
            if (NULL != setup_descriptor) {
                WARN2("test", "Ignoring duplicate SETUP("
                        << (setup_descriptor->name) << ") function at "
                        << (setup_descriptor->file) << ":"
                        << (setup_descriptor->line));
            }
            setup_descriptor = this;
        }

        int setup() {
#ifdef _WIN32
            __try {
#endif // _WIN32
                TRACE2("test","setting up by " << this);
                func();
                TRACE2("test",this << " setup complete");
#ifdef _WIN32
            } __except(EXCEPTION_EXECUTE_HANDLER) {
                WARN2("test",std::endl << get_exception_message(GetExceptionCode()) << " in " << this);
                return 0;
            }
#endif // _WIN32
            return 1;
        }
};

class TeardownDescriptor;

/// A global teardown descriptor
extern TeardownDescriptor* teardown_descriptor;

/// Container of teardown function description.
class TeardownDescriptor : public Descriptor {
    public:
        TeardownDescriptor(void (*func)(), char* name, char* file, int line)
            : Descriptor(func, name, file, line)
        {
            if (NULL != teardown_descriptor) {
                WARN2("test","Ignoring duplicate TEARDOWN("
                        << (teardown_descriptor->name) << ") function at "
                        << (teardown_descriptor->file) << ":"
                        << (teardown_descriptor->line));
            }
            teardown_descriptor = this;
        }

        int teardown() {
#ifdef _WIN32
            __try {
#endif // _WIN32
                TRACE2("test","tearing down by " << this);
                func();
                TRACE2("test",this << " teardown complete");
#ifdef _WIN32
            } __except(EXCEPTION_EXECUTE_HANDLER) {
                WARN2("test",std::endl << get_exception_message(GetExceptionCode()) << " in " << this);
                return 0;
            }
#endif // _WIN32
            return 1;
        }
};

class TestCaseDescriptor;

/// A global list of all test cases.
extern std::list<TestCaseDescriptor*> test_cases;

/// Container of test case description.
class TestCaseDescriptor : public Descriptor {
    public:
        TestCaseDescriptor(void (*func)(), char* name, char* file, int line)
            : Descriptor(func, name, file, line)
        {
            test_cases.push_back(this);
        }

        int run() {
            status = 0;
            INFO(" ... " << name);
#ifdef _WIN32
            __try {
#endif // _WIN32
#ifdef __linux__
            if (fork() == 0) {
                signal(SIGSEGV, segfault);
#endif // __linux__
                if (NULL != setup_descriptor) {
                    // run the setup and check its result
                    if (!setup_descriptor->setup()) {
                        fail("Setup function " << setup_descriptor << " failed");
                        return status;
                    }
                }
                // run the test case
                func();
                if (NULL != teardown_descriptor) {
                    // run the teardown
                    if (!teardown_descriptor->teardown()) {
                        // print a warning, but do not change test case result
                        WARN2("test","Teardown function " << teardown_descriptor << " failed");
                    }
                }
#ifdef _WIN32
            } __except(EXCEPTION_EXECUTE_HANDLER) {
                ++status;
                WARN2("test", std::endl << get_exception_message(GetExceptionCode())
                        << " in " << name << "() at " << file << ":" << line);
            }
#endif // _WIN32
#ifdef __linux__
                exit(status);
            }
            // we are the parent process
            int error;
            wait(&error);
            if (error != 0) {
                ++status;
            }
#endif // __linux__
            if (0 == status) {
                TICK(".");
                INFO(name << " passed");
            } else {
                TICK("x");
                INFO(this << " failed");
            }
            return status;
        }
};

/// Defines the test case function and adds it to the global test case list.
#define TEST(x) \
    static void x (); \
    TestCaseDescriptor test_case_descriptor_ ## x (&x, #x, __FILE__, __LINE__); \
    static void x ()

/// Defines the excluded test case function (not put to the global test case list).
#define XTEST(x) \
    static void x ()

/// Defines the setup function
#define SETUP(x) \
    static void x (); \
    SetupDescriptor setup_descriptor_ ## x (&x, #x, __FILE__, __LINE__); \
    static void x ()

/// Defines the teardown function
#define TEARDOWN(x) \
    static void x (); \
    TeardownDescriptor teardown_descriptor_ ## x (&x, #x, __FILE__, __LINE__); \
    static void x ()

#endif // UNIT_TEST_H
