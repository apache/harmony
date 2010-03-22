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

#ifndef UNIT_TEST_MAIN_H
#define UNIT_TEST_MAIN_H

/** \file
 * Unit test framework: main function.
 */

#include "unit_test.h"

/// Log level
int log_level = 2;

/// Current test case status.
int status = 0;

/// Global list of all test cases.
std::list<TestCaseDescriptor*> test_cases;

/// Global setup descriptor. None by default.
SetupDescriptor* setup_descriptor = NULL;

/// Global teardown descriptor. None by default.
TeardownDescriptor* teardown_descriptor = NULL;

static inline bool begins_with(const char* str, const char* beginning)
{
    return strncmp(str, beginning, strlen(beginning)) == 0;
}

static void set_log_levels_from_cmd(int argc, char* argv[])
{
    int arg_num;
    for (arg_num = 1; arg_num < argc; arg_num++) {
        char *option = argv[arg_num];

        if (begins_with(option, "-Xwarn")) {
            log_level = 1;
        } else if (begins_with(option, "-Xinfo")) {
            log_level = 2;
        } else if (begins_with(option, "-Xlog")) {
            log_level = 3;
        } else if (begins_with(option, "-Xtrace")) {
            log_level = 4;
        }
    }
}

/// Prints the summary of the unit test
inline void summary(int failed) {
    if (0 == failed) {
        std::cerr << std::endl << "OK" << std::endl << std::flush;
    } else {
        std::cerr << std::endl << "FAILED, " << failed
            << (failed > 1 ? " failures" : " failure") << std::endl << std::flush;
    }
}

/// Configures and runs unit tests.
inline void run(int argc, char** argv) {
    set_log_levels_from_cmd(argc, argv);
    std::list<TestCaseDescriptor*>::iterator i;
    int failed = 0;
    for (i = test_cases.begin(); i != test_cases.end(); i++) {
        TestCaseDescriptor* test_case = *i;
        int status = test_case->run();
        if (0 != status) {
            ++failed;
        }
    }
    summary(failed);
    exit(failed);
}

#endif // UNIT_TEST_MAIN_H
