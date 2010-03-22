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
 * @author Salikh Zakirov
 */
#ifndef _TESTFRAME_H_
#define _TESTFRAME_H_

#include <stdlib.h>

#ifdef  __cplusplus
extern "C" {
#endif

    /** \mainpage Testframe testing framework
     *
     * \section intro_sec Introduction
     *
     * Testframe is testing framework developed in C language. It will be used (hopely) for testing DRL VM internals and PVL interfaces.
     *
     * \section usage_sec Usage
     * You need to include files testframe.h and testframe.c in your project.
     * 
     *  
     * 
     */



    /**
       @defgroup testframe Testframe testing framework
    */

    /**
       @file testframe.h
       Main include for Testframe testing framework
       @ingroup testframe
    */

#ifndef NULL 
#define NULL 0
#endif

    /* status codes */
    /**
     * Status code to indicate test success
     @ingroup testframe
    */
#define TEST_PASSED 0
    /**
     * Status code to indicate test failure
     @ingroup testframe
    */
#define TEST_FAILED 1
    /**
     * Status code to indicate error
     @ingroup testframe
    */
#define TEST_ERROR 2

    /**
       @defgroup assertions Assertion checks
    */

    /* helper functions */
    /**
       A helper macro to indicate test failure with given message. Source file name and line number will be appended to the message.
       @param message - text reason of test failure
       @ingroup assertions
    */
#define tf_fail(message) log_error("Test failed: %s (%s:%d)", message, __FILE__, __LINE__); return TEST_FAILED
    /**
       A helper macro to indicate test success.
       @ingroup assertions
    */
#define tf_pass() return TEST_PASSED
    /**
       A helper macro to check test assertion 
       @param expression - a boolean expression to check, if it results in false then test will be failed.
       @ingroup assertions
    */
#define tf_assert(expression) if (!(expression)) { log_error("Assertion '%s' failed at %s:%d", #expression, __FILE__, __LINE__); return TEST_FAILED; }
    /**
       A helper macro to check test assertion (for use in void function) 
       @param expression - a boolean expression to check, if it results in false then test will be failed.
       @ingroup assertions
    */
#define tf_assert_v(expression) if (!(expression)) { log_error("Assertion '%s' failed at %s:%d", #expression, __FILE__, __LINE__); return; }
    /**
       A helper macro to check test assertion and report message in case of check failure.
       Source file name and line nimber will be appended to the message.
       @param message - text reason of test failure.
       @param expression - a boolean expression to check, if it results in false then test will be failed.
       @ingroup assertions
    */
#define tf_assert_message(message, expression) if (!(expression)) { log_error("Assertion '%s' failed, message: %s at %s:%d", #expression, #message, __FILE__, __LINE__); return TEST_FAILED; }
    /**
       A helper macro to check whether supplied expression is NULL
       @param expression - a boolean expression to check, if it results in false then test will be failed.
       @ingroup assertions
    */
#define tf_assert_null(expression) tf_assert((expression)==NULL)
    /**
       A helper macro to check whether supplied expression is NULL and report message in case of check failure.
       Source file name and line nimber will be appended to the message.

       @param message - text reason of test failure.
       @param expression - a boolean expression to check, if it results in false then test will be failed.
       @ingroup assertions
    */
#define tf_assert_null_message(message, expression) tf_assert_message(message, (expression)==NULL)
    /**
       A helper macro to check whether supplied expression is not NULL
       @param expression - a boolean expression to check, if it results in false then test will be failed.
       @ingroup assertions
    */
#define tf_assert_not_null(expression) tf_assert((expression)!=NULL)
    /**
       A helper macro to check whether supplied expression is not NULL and report message in case of check failure.
       Source file name and line nimber will be appended to the message.

       @param message - text reason of test failure.
       @param expression - a boolean expression to check, if it results in false then test will be failed.
       @ingroup assertions
    */
#define tf_assert_not_null_message(message, expression) tf_assert(message, (expression)!=NULL)
    /**
       A helper macro to compare values of two expressions
       @param expected - expected value
       @param actual - actual value
       @ingroup assertions
    */
#define tf_assert_same(expected, actual) tf_assert((expected)==(actual))
    /**
       A helper macro to compare values of two expressions (for use in void function)
       @param expected - expected value
       @param actual - actual value
       @ingroup assertions
    */
#define tf_assert_same_v(expected, actual) tf_assert_v((expected)==(actual))
    /**
       A helper macro to compare values of two expressions and report message in case of mismatch
       Source file name and line nimber will be appended to the message.
       @param message - text reason of test failure.
       @param expected - expected value
       @param actual - actual value
       @ingroup assertions
    */
#define tf_assert_same_message(message, expected, actual) tf_assert_message(message, (expected)==(actual))


    /* test management macros */
    /* 
       @struct TestDescriptor
       Structure describing test, holds test name and pointer to test function
       for internal use only
    */
    typedef struct {
        char *name;
        int (*func)(void);
    } TestDescriptor;


    /**
       Helper macro to start test list declarion. This list is necessary for test case execution engine.
       @ingroup testframe
    */
#define TEST_LIST_START \
    TestDescriptor testDescriptor[] = {

        /**
           Helper macro to include a test case into test list for execution engine. 

           @param name of function, which implements test case
           @ingroup testframe
        */
#define TEST(name) {#name , name},

        /**
           Helper macro to end test list declarion. This list is necessary for test case execution engine.
           @ingroup testframe
        */
#define TEST_LIST_END {NULL, NULL}};



    /* these functions must be implemented by test */
    /**
       Setup function for test case. Must be implemented by test writer.
       @ingroup testframe
    */
    extern void setup();
    /**
       Clean up function for test case. Must be implemented by test writer
       @ingroup testframe
    */
    extern void teardown();


    /* these structure must be implemented by test via TEST_LIST_* macros */
    extern TestDescriptor testDescriptor[];
    /**
       Engin for running test case. Should be included in the main() in form<br>
       return default_main(int argc, char *argv[]);<br>

       @param argc - number of command line arguments
       @param argv - command line arguments (list of names of test cases to run)

       @return TEST_PASSED if all test cases passed, TEST_FAIL or TEST_ERROR otherwise 

       @ingroup testframe
    */
    extern int default_main(int argc, char *argv[]);


    /**
       @defgroup logging Logging functions
    */

    /* logging functions */
    /**
       logs error, arguments are the same as for printf()
       prints to standard error

       @ingroup logging
    */
    extern void log_error(char *format, ...);
    /**
       logs message, arguments are the same as for printf()
       prints to standard output

       @ingroup logging
    */
    extern void log_info(char *format, ...);
    /**
       logs message, arguments are the same as for printf()
       prints to standard output

       @ingroup logging
    */
    extern void log_debug(char *format, ...);

    /**
       Defines error-only log level
       @ingroup logging
    */
#define LOG_LEVEL_ERROR 0
    /**
       Defines error and info only log level
       @ingroup logging
    */
#define LOG_LEVEL_INFO 1
    /**
       Defines debug  log level
       @ingroup logging
    */
#define LOG_LEVEL_DEBUG 2

    /**
       Sets logging level
    
       @param level - controls which messages to log <UL><LI>LOG_LEVEL_ERROR - log error only<LI>LOG_LEVEL_INFO - log errors and info<LI>LOG_LEVEL_INFO - logs everything</UL> 
       @ingroup logging
    */

    extern void log_set_level(int level);

    /**
       logs debug output 
       TODO - decide how to enable/disable it
    */


#ifdef  __cplusplus
}
#endif

#endif
