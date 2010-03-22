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
#include <apr_env.h>
#include <apr_portable.h>
#include <time.h>
#include "logger.h"
#include "port_atomic.h"
#include "hyport.h"

/**
 * A hook for <code>int vfprintf(va_list)</code> function.
 */
typedef int (*VfprintfHook)(FILE *fp, const char *format,  va_list args);

/**
 * A hook for <code>void exit(int)</code> function.
 */
typedef void (*ExitHook)(int code);

/**
 * A hook for <code>void abort()</code> function.
 */
typedef void (*AbortHook)();

struct LogCategory {
    const char* name;
    int len;
    LogState enabled;
    struct LogCategory* next;
};

/**
 * Keeps a logger instance.
 */
struct Logger {
    /**
     * Serves as a primary source for a dynamic memory allocation.
     */
    apr_pool_t* pool;

    /**
     * A pointer to vfprintf implementation. This pointer may be received via 
     * JNI_CreateJavaVM arguments and is used as a base for all VM
     * logging. A static pointer is used when a logger instance cannot be
     * accessed.
     */
    VfprintfHook p_vfprintf;

    /**
     * A pointer to exit implementation. This pointer may be received via 
     * JNI_CreateJavaVM arguments and is used for VM graceful termination.
     */
    ExitHook p_exit;

    /**
     * A pointer to abort implementation. This pointer may be received via 
     * JNI_CreateJavaVM arguments and is used for abnormal VM termination.
     */
    AbortHook p_abort;

    /**
     * A file to write log, <code>stdout</code> by default.
     */
    FILE* out;

    /**
     * A reference to localization library.
     */
    struct HyPortLibrary* portlib;

    /**
     * Format mask.
     */
    LogFormat format;

    /**
     * A head of a list of enabled info domains.
     */
    struct LogCategory* info;

    /**
     * A head of a list of enabled trace domains.
     */
    struct LogCategory* trace;

    /**
     * A head of a list of encountered log sites.
     */
    struct LogSite* log_site;
}  light_logger = {
    NULL, &vfprintf, &exit, &abort, stdout, NULL, LOG_EMPTY, NULL, NULL, NULL };

Logger default_logger = {
    NULL, &vfprintf, &exit, &abort, stdout, NULL, LOG_EMPTY, NULL, NULL, NULL };

static Logger* get()
{
    return &default_logger;
}

/**
 * Clears cached sites. If more sites are added in process from
 * other threads, they won't be cleared.
 */
static void clear_cached_sites()
{
    LogSite *site = get()->log_site;
    while(site) {
        site->state = LOG_UNKNOWN;
        site = site->next;
    }
}

static int add_category(const char* category, LogCategory** p_log_category_head,
                        int copy, LogState enabled)
{
    Logger* logger = get();
    if (!logger->pool) {
        return 0;
    }

    LogCategory* log_category = *p_log_category_head;
    while (log_category) {
        if (strcmp(log_category->name, category) == 0) {
            if (log_category->enabled == enabled) {
                return 0;
            }
            log_category->enabled = enabled;
            clear_cached_sites();
            return 1;
        }
        log_category = log_category->next;
    }

    log_category = (LogCategory*) apr_palloc(logger->pool, sizeof(LogCategory));
    if (!log_category) {
        return 0;
    }

    log_category->len = (int) strlen(category);
    log_category->enabled = enabled;
    if (copy) {
        char* name = (char*) apr_palloc(logger->pool, log_category->len + 1);
        if (!name) {
            return 0;
        }
        strncpy(name, category, log_category->len + 1);
        log_category->name = name;
    } else {
        log_category->name = category;
    }

    LogCategory* old_value = *p_log_category_head;
    do {
        log_category->next = (LogCategory*) old_value;
        old_value = (LogCategory*) port_atomic_casptr(
            (volatile void **) p_log_category_head, log_category,
            log_category->next);
    } while (old_value != log_category->next);

    clear_cached_sites();
    return 1;
}

int log_enable_info_category(const char* category, int copy)
{
    return add_category(category, &get()->info, copy, LOG_ENABLED);
}

int log_enable_trace_category(const char* category, int copy)
{
    return add_category(category, &get()->trace, copy, LOG_ENABLED);
}

int log_disable_info_category(const char* category, int copy)
{
    return add_category(category, &get()->info, copy, LOG_DISABLED);
}

int log_disable_trace_category(const char* category, int copy)
{
    return add_category(category, &get()->trace, copy, LOG_DISABLED);
}

void log_init(apr_pool_t* parent_pool) 
{
    apr_pool_t *pool;
    apr_status_t status = apr_pool_create(&pool, parent_pool);
    if (APR_SUCCESS != status) {
        return;
    }

    Logger* logger = get();
    *logger = light_logger;
    logger->pool = pool;

    log_enable_info_category(LOG_INFO, 0);
}

static void log_close()
{
    Logger* logger = (Logger*) get();
    if (stdout != logger->out) {
        fclose(logger->out);
        logger->out = stdout;
    }
}

void log_shutdown()
{
    Logger* logger = (Logger*) get();
    apr_pool_t* pool = logger->pool;
    log_close();
    *logger = light_logger;
    clear_cached_sites();
    apr_pool_destroy(pool);
}

/**
 * Parses locale.
 */
static int set_locale(const char* logger_locale)
{
    HyPortLibrary* portlib = get()->portlib;
    assert(portlib);

    char* lang = strdup(logger_locale);
    if (NULL == lang) {
        return 0; // out of C heap
    }
    char* region = NULL;
    char* variant = NULL;

    char* pos = strchr(lang, '_');
    if (pos == NULL) {
        goto set;
    }
    region = pos;
    region[0] = 0;
    region++;

    pos = strchr(region, '.');
    if (pos == NULL) {
        goto set;
    }
    variant = pos;
    variant[0] = 0;
    variant++;

set:
    portlib->nls_set_locale(portlib, lang, region ? region : "", variant ? variant : "");
    free((void*)lang);
    return 1;
}

void log_set_portlib(HyPortLibrary *portlib)
{
    Logger* logger = get();

    logger->portlib = portlib;
    if (!portlib) {
        return;
    }

    apr_pool_t* tmp_pool;
    apr_status_t status = apr_pool_create(&tmp_pool, logger->pool);
    if (APR_SUCCESS != status) {
        return;
    }
    char* value;
    if (APR_SUCCESS == apr_env_get(&value, "LC_ALL", tmp_pool)) {
        set_locale(value);
    } else if (APR_SUCCESS == apr_env_get(&value, "LC_MESSAGES", tmp_pool)) {
        set_locale(value);
    } else if (APR_SUCCESS == apr_env_get(&value, "LANG", tmp_pool)) {
        set_locale(value);
    }
    apr_pool_destroy(tmp_pool);
}

HyPortLibrary* log_get_portlib() {
    return get()->portlib;
}

void log_set_vfprintf(void* p_vfprintf)
{
    get()->p_vfprintf = (VfprintfHook) p_vfprintf;
}

void log_set_exit(void* p_exit)
{
    get()->p_exit = (ExitHook) p_exit;
}

void log_set_abort(void* p_abort)
{
    get()->p_abort = (AbortHook) p_abort;
}

void log_set_header_format(LogFormat format)
{
    get()->format = format;
}

void log_set_out(FILE* file)
{
    log_close();
    get()->out = file;
}

APR_DECLARE(void) log_exit(int code)
{
    log_shutdown();
    get()->p_exit(code);
}

APR_DECLARE(void) log_abort()
{
    log_shutdown();
    assert(0);
    get()->p_abort();
}

APR_DECLARE_NONSTD(int) log_printf(const char* format, ...)
{
    va_list args;
    int ret;

    va_start(args, format);
    ret = get()->p_vfprintf(get()->out, format, args);
    va_end(args);
    fflush(get()->out);
    return ret;
}

APR_DECLARE(void) log_header(const char* category, const char* file_line, const char* function_name)
{
    LogFormat format = get()->format;
    if (format & LOG_THREAD_ID) {
        log_printf("[%p] ", apr_os_thread_current());
    }
    if (format & LOG_TIMESTAMP) {
        log_printf("[%umus] ", (unsigned) clock());
    }
    if (format & LOG_CATEGORY && strcmp(category, LOG_INFO)) {
        log_printf("[%s] ", category);
    }
    if (format & LOG_FUNCTION) {
        log_printf("%s:", file_line);
    }
    if (format & LOG_FILELINE) {
        log_printf("%s:", function_name);
    }
    fflush(get()->out);
}

/**
 * Checks if the warnings are enabled. 
 */
APR_DECLARE(LogState) log_is_warn_enabled()
{
    return (get()->format & LOG_WARN) ? LOG_ENABLED : LOG_DISABLED;
}

/**
 * Finds the best matching category in the category list.
 * @return if the category is enabled
 */
static LogState is_enabled(const char *category, LogCategory *log_category) {
    int max_size = -1;
    LogState enabled = LOG_DISABLED;
    while (log_category) {
        if (strncmp(log_category->name, category, log_category->len) == 0) {
            if (log_category->len > max_size) {
                max_size = log_category->len;
                enabled = log_category->enabled;
            }
        }
        log_category = log_category->next;
    }
    return enabled;
}

APR_DECLARE(LogState) log_is_info_enabled(const char *category)
{
    return is_enabled(category, get()->info);
}

APR_DECLARE(LogState) log_is_trace_enabled(const char *category)
{
    return is_enabled(category, get()->trace);
}

/**
 * Adds a site to the site list.
 */
static void add_site(LogSite* log_site) {
    LogSite** p_site_head = (LogSite**) &get()->log_site;
    LogSite* old_value = *p_site_head;
    do {
        log_site->next = (LogSite*) old_value;
        old_value = (LogSite*) port_atomic_casptr(
            (volatile void **) p_site_head, log_site, log_site->next);
    } while (old_value != log_site->next);
}

APR_DECLARE(LogState) log_cache(LogState enabled, struct LogSite* p_log_site)
{
    if(!p_log_site->next) {
        add_site(p_log_site);
    }
    p_log_site->state = enabled ? LOG_ENABLED : LOG_DISABLED;
    return enabled;
}

