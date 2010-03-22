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
#ifndef _LOGGER_H
#define _LOGGER_H

#include <assert.h>
#include <stdio.h>
#include <apr_pools.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Header format mask.
 */
typedef int LogFormat;

/**
 * Formats.
 */
#define LOG_EMPTY       0
#define LOG_TIMESTAMP   1
#define LOG_FILELINE    2
#define LOG_CATEGORY    4
#define LOG_THREAD_ID   8
#define LOG_FUNCTION    16
#define LOG_WARN        32

/**
 * Predefined classloader filter.
 */
#define LOG_CLASS_INFO  "class"
/**
 * Predefined gc filter.
 */
#define LOG_GC_INFO     "gc"
/**
 * Predefined jni filter.
 */
#define LOG_JNI_INFO    "jni"
/**
 * A domain, which is enabled by default.
 */
#define LOG_INFO        "info"

typedef enum {
    LOG_DISABLED = 0,
    LOG_ENABLED,
    LOG_UNKNOWN
} LogState;

struct LogSite {
    LogState state;
    struct LogSite *next;
    char* log_domain;
};

/**
 * Adds and enables an info category to log.
 * @param category a category name
 * @param copy true if the name should be stored internally
 * @return true on success, false if the category already exists
 * and is enabled, or the logger pool is not set.
 */
int log_enable_info_category(const char* category, int copy);

/**
 * Adds and enables a trace category to log.
 * @param category a category name
 * @param copy true if the name should be stored internally
 * @return true on success, false if the category already exists
 * and is enabled, or the logger pool is not set.
 */
int log_enable_trace_category(const char* category, int copy);

/**
 * Adds and disables a given info category.
 * @param category a category name
 * @param copy true if the name should be stored internally
 * @return true on success, false if the category already exists
 * and is disabled, or the logger pool is not set.
 */
int log_disable_info_category(const char* category, int copy);

/**
 * Adds and disables a given trace category.
 * @param category a category name
 * @param copy true if the name should be stored internally
 * @return true on success, false if the category already exists
 * and is disabled, or the logger pool is not set.
 */
int log_disable_trace_category(const char* category, int copy);

/**
 * Inits log system. Allows further dynamic configuration of categories.
 */
void log_init(apr_pool_t* parent_pool);

/**
 * Frees dynamic memory associated with the logger.
 */
void log_shutdown();

/**
 * A native porting layer.
 */
struct HyPortLibrary;

/**
 * Sets a porting library for log system localization.
 */
void log_set_portlib(struct HyPortLibrary* portlib);

/**
 * Gets a porting library for log system localization.
 */
struct HyPortLibrary* log_get_portlib();

/**
 * Sets a <code>vfprintf</code> hook.
 */
void log_set_vfprintf(void* p_vfprintf);

/**
 * Sets a <code>exit</code> hook.
 */
void log_set_exit(void* p_exit);

/**
 * Sets a <code>abort</code> hook.
 */
void log_set_abort(void* p_abort);

/**
 * Sets the header format for the logger.
 */
void log_set_header_format(LogFormat format);

/**
 * Redirects output to a file. 
 */
void log_set_out(FILE* file);

/**
 * Terminates the program using specified return code.
 */
APR_DECLARE(void) log_exit(int code);

/**
 * Terminates the program abnormally.
 */
APR_DECLARE(void) log_abort();

/**
 * Writes formatted data to stdout via specified vfprinf function.
 */
APR_DECLARE_NONSTD(int) log_printf(const char* format, ...);

/**
 * Writes formatted header to stdout via specified vfprinf function.
 */
APR_DECLARE(void) log_header(const char* category, const char* file_line, const char* function_name);

/**
 * Checks if the warnings are enabled. 
 */
APR_DECLARE(LogState) log_is_warn_enabled();

/**
 * Checks if the info domain is enabled for the given category. 
 */
APR_DECLARE(LogState) log_is_info_enabled(const char *category);

/**
 * Checks if the trace is enabled for the given category. 
 */
APR_DECLARE(LogState) log_is_trace_enabled(const char *category);

/**
 * Caches a check in the corresponding log site.
 * @return enabled
 */
APR_DECLARE(LogState) log_cache(LogState enabled, struct LogSite* p_log_site);

#ifdef __cplusplus
}
#endif

#endif /* _LOGGER_H */

