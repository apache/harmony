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

#include <apr_file_io.h>
#include <apr_file_info.h>
#include <apr_strings.h>
#include "port_dso.h"
#include "port_filepath.h"
#include "port_sysinfo.h"

#define LOG_DOMAIN "init.properties"
#include "cxxlog.h"
#include "properties.h"
#include "vm_properties.h"
#include "init.h"
#include "port_modules.h"
#include "version.h"
#if defined(FREEBSD)
#include <dlfcn.h>
#endif

inline char* unquote(char *str)
{
    const char *tokens = " \t\n\r\'\"";
    size_t i = strspn(str, tokens);
    str += i;
    char *p = str + strlen(str) - 1;
    while(strchr(tokens, *p) && p >= str)
        *(p--) = '\0';
    return str;
}

// local memory pool for temporary allocation
static apr_pool_t *prop_pool;

static const char *api_dll_files[] =
{
    "harmonyvm",
    "hythr",
    "hyprt",
#if defined(HY_LOCAL_ZLIB)
    "z",
#else
    "hyzlib",
#endif
    "hynio",
    "vmi",
    "hyluni",
    "hyarchive"
};

/**
 *  Compose a string of file names each of them beginning with path,
 *  names separated by PORT_PATH_SEPARATOR.  If patch is NULL, no path
 *  or separator will be prefixed
 */
static const char *compose_full_files_path_names_list(const char *path,
                                                const char **dll_names,
                                                const int names_number, 
                                                bool is_dll)
{
    const char* full_name = "";
    for (int iii = 0; iii < names_number; iii++)
    {
        const char *tmp = dll_names[iii];
        if (is_dll) {
            tmp = port_dso_name_decorate(tmp, prop_pool);
        }
        
        /*
         *  if the path is non-null, prefix, otherwise do nothing
         *  to avoid the problem of "/libfoo.so" when we don't want
         *  a path attached
         */
         
        if (path != NULL) { 
            tmp = port_filepath_merge(path, tmp, prop_pool);
        }
        
        full_name = apr_pstrcat(prop_pool, full_name, tmp, 
            (iii + 1 < names_number) ? PORT_PATH_SEPARATOR_STR : "", NULL);
    }

    return full_name;
}

static char* get_module_filename(void* code_ptr)
{
    native_module_t* modules;
    int modules_count;

#ifdef _IPF_
    // On IPF function pointer is not a pointer to a function, it is a pointer
    // to a table with first element of it the address of the funtion
    void **ptr = (void**)code_ptr;
    code_ptr = ptr[0];
#endif
#if defined(FREEBSD)
    Dl_info info;
    if (dladdr( (const void*)code_ptr, &info) == 0) {
        return NULL;
    }
    return apr_pstrdup(prop_pool, info.dli_fname);
#else
    if (! port_get_all_modules(&modules, &modules_count))
        return NULL;

    native_module_t* module = port_find_module(modules, code_ptr);

    char* filename = NULL;

    if (NULL != module) {
        filename = apr_pstrdup(prop_pool, module->filename);
    }

    port_clear_modules(&modules);

    return filename;
#endif
}

static void init_java_properties(Properties & properties)
{
    //java part
    //!!! java.compiler property must be defined by EM

    char *os_name, *os_version, *path;
    const char *tmp;
    char *path_buf = NULL;

    port_OS_name_version(&os_name, &os_version, prop_pool);
    apr_filepath_get(&path, APR_FILEPATH_NATIVE, prop_pool);
    if (APR_SUCCESS != apr_temp_dir_get(&tmp, prop_pool)) {
        tmp = ".";
    }
    properties.set_new("java.version", JAVA_RUNTIME_VERSION);
    properties.set_new("java.vendor", "Apache Software Foundation");
    properties.set_new("java.vendor.url", "http://harmony.apache.org");
    properties.set_new("java.fullversion", VERSION);

    // java.home initialization, try to find absolute location of the executable and set
    // java.home to the parent directory.
    char *launcher_dir;
    if (port_executable_name(&launcher_dir) != APR_SUCCESS) {
        LDIE(13, "Failed to find executable location");
    }
    STD_FREE(launcher_dir);

    properties.set_new("java.vm.specification.version", "1.0");
    properties.set_new("java.vm.specification.vendor", "Sun Microsystems Inc.");
    properties.set_new("java.vm.specification.name", "Java Virtual Machine Specification");
    properties.set_new("java.vm.version",
                       JAVA_RUNTIME_VERSION "-r" VERSION_SVN_TAG);
    properties.set_new("java.vm.vendor", "Apache Software Foundation");
    properties.set_new("java.vm.name", "DRLVM");
    properties.set_new("java.runtime.name", "Apache Harmony");
    properties.set_new("java.runtime.version", JAVA_RUNTIME_VERSION);
    properties.set_new("java.specification.version",
                       JAVA_SPECIFICATION_VERSION);
    properties.set_new("java.specification.vendor", "Sun Microsystems Inc.");
    properties.set_new("java.specification.name", "Java Platform API Specification");
    properties.set_new("java.class.version", EXPAND(CLASSFILE_MAJOR_MAX) "."
        EXPAND(CLASSFILE_MINOR_MAX));
    properties.set_new("java.class.path", ".");

    /*
    *  it's possible someone forgot to set this property - set to default of location of vm module
    */
    if (! properties.is_set(O_A_H_VM_VMDIR)) {
        char* vm_dir = get_module_filename((void*) &init_java_properties);

        if (NULL == vm_dir)
            LDIE(43, "ERROR: Can't determine vm module location. Please specify {0} property." << O_A_H_VM_VMDIR);

        char *p = strrchr(vm_dir, PORT_FILE_SEPARATOR);
        if (NULL == p)
            LDIE(43, "ERROR: Can't determine vm module location. Please specify {0} property." << O_A_H_VM_VMDIR);

        *p = '\0';

        properties.set(O_A_H_VM_VMDIR, vm_dir);
    }

    char* vm_dir = properties.get(O_A_H_VM_VMDIR);

    // home directory
    char* home_path = apr_pstrdup(prop_pool, vm_dir);
    char* p = strrchr(home_path, PORT_FILE_SEPARATOR);
    if (NULL == p)
        LDIE(15, "Failed to determine java home directory");
    *p = '\0';

    char* lib_path = apr_pstrcat(prop_pool, vm_dir,
            PORT_PATH_SEPARATOR_STR, home_path, NULL);

    p = strrchr(home_path, PORT_FILE_SEPARATOR);
    if (NULL == p)
        LDIE(15, "Failed to determine java home directory");
    *p = '\0';
    properties.set_new("java.home", home_path);

    properties.destroy(vm_dir);
    vm_dir = NULL;

    /*
     * This property is used by java/lang/Runtime#loadLibrary0 as path to
     * system native libraries.
     * The value is the location of launcher executable and vm binary directory
     */
    properties.set_new("vm.boot.library.path", lib_path);

    // Added for compatibility with the external java JDWP agent
    properties.set_new("sun.boot.library.path", lib_path);

    // java.library.path initialization, the value is the same as for
    // vm.boot.library.path appended with OS library search path
    char *env;
    if (APR_SUCCESS == port_dso_search_path(&env, prop_pool))
    {
        lib_path = apr_pstrcat(prop_pool, lib_path, PORT_PATH_SEPARATOR_STR,
                env, NULL);
    }
    properties.set_new("java.library.path", lib_path);
    //java.ext.dirs initialization.
    char *ext_path = port_filepath_merge(home_path, "lib" PORT_FILE_SEPARATOR_STR "ext", prop_pool);
    properties.set_new("java.ext.dirs", ext_path);
    properties.set_new("os.name", os_name);
    properties.set_new("os.arch", port_CPU_architecture());
    properties.set_new("os.version", os_version);
    properties.set_new("file.separator", PORT_FILE_SEPARATOR_STR);
    properties.set_new("path.separator", PORT_PATH_SEPARATOR_STR);
    properties.set_new("line.separator", APR_EOL_STR);
    // user.name initialization, try to get the name from the system
    char *user_buf;
    apr_status_t status = port_user_name(&user_buf, prop_pool);
    if (APR_SUCCESS != status) {
        LDIE(16, "Failed to get user name from the system. Error code {0}" << status);
    }
    properties.set_new("user.name", user_buf);
    // user.home initialization, try to get home from the system.
    char *user_home;
    status = port_user_home(&user_home, prop_pool);
    if (APR_SUCCESS != status) {
        LDIE(17, "Failed to get user home from the system. Error code {0}" << status);
    }
    properties.set_new("user.home", user_home);
    // java.io.tmpdir initialization. 
    const char *tmpdir;
    status = apr_temp_dir_get(&tmpdir, prop_pool);
    if (APR_SUCCESS != status) {
        tmpdir = user_home;
    }
    properties.set_new("java.io.tmpdir", tmpdir);
    properties.set_new("user.dir", path);

    // FIXME??? other (not required by api specification) properties
    
    properties.set_new("java.vm.info", "no info");
    properties.set_new("java.tmpdir", tmp);

    // FIXME user.timezone initialization, required by java.util.TimeZone implementation
    char *user_tz;
    status = port_user_timezone(&user_tz, prop_pool);
    if (APR_SUCCESS != status) {
        INFO("Failed to get user timezone from the system. Error code " << status);
        user_tz = NULL;
    }
    properties.set_new("user.timezone", user_tz ? user_tz : "GMT");

    /*
    *  it's possible someone forgot to set this property - set to default of .
    */
    properties.set_new("java.class.path", ".");
}

//vm part
static void init_vm_properties(Properties & properties)
{
#ifdef _DEBUG
        properties.set_new("vm.assert_dialog", "true");
#else
        properties.set_new("vm.assert_dialog", "false");
#endif
        properties.set_new("vm.crash_handler", "false");
        properties.set_new("vm.finalize", "true");
        properties.set_new("vm.jit_may_inline_sync", "true");
        properties.set_new("vm.use_verifier", "true");
        properties.set_new("vm.jvmti.enabled", "false");
        properties.set_new("vm.jvmti.compiled_method_load.inlined", "false");
        properties.set_new("vm.bootclasspath.appendclasspath", "false");
        properties.set_new("thread.soft_unreservation", "false");

#ifdef REFS_USE_RUNTIME_SWITCH
        properties.set_new("vm.compress_references", "true");
#endif

        int n_api_dll_files = sizeof(api_dll_files) / sizeof(char *);
        /*
        *  pass NULL for the pathname as we don't want 
        *  any path pre-pended
        */
        const char* path_buf = compose_full_files_path_names_list(NULL, api_dll_files, n_api_dll_files, true);
        properties.set_new("vm.other_natives_dlls", path_buf);
}

jint initialize_properties(Global_Env * p_env)
{
    jint status = JNI_OK;

    if (!prop_pool) {
        apr_pool_create(&prop_pool, 0);
    }
/*
 * 1. Process command line options.
 * Java properties are set as -Dkey[=value];
 * VM properties are set with the following syntax ("fully compatible" with RI):
 * - options are set with -XX:<option>=<string>
 * - Boolean options may be turned on with -XX:+<option> and turned off with -XX:-<option>
 * - Numeric options are set with -XX:<option>=<number>. 
 *   Numbers can include 'm' or 'M' for megabytes, 'k' or 'K' for kilobytes, and 'g' or
 *   'G' for gigabytes (for example, 32k is the same as 32768).
 */
    char *src, *tok;
    for (int arg_num = 0; arg_num < p_env->vm_arguments.nOptions; arg_num++)
    {
        char *option = p_env->vm_arguments.options[arg_num].optionString;
        if (strncmp(option, "-D", 2) == 0)
        {
            TRACE("setting property " << option + 2);
            src = strdup(option + 2);
            tok = strchr(src, '=');
            if(tok)
            {
                *tok = '\0';
                p_env->JavaProperties()->set(unquote(src), unquote(tok + 1));
            }
            else 
            {
                p_env->JavaProperties()->set(unquote(src), "");
            }
            STD_FREE(src);
        } 
        else if (strncmp(option, "-XD", 3) == 0)
        {
            WARN(("Deprecated syntax to set internal property, use -XX:key=value instead: %s", option));
            TRACE("setting internal property " << option + 3);
            src = strdup(option + 3);
            tok = strchr(src, '=');
            if(tok)
            {
                *tok = '\0';
                p_env->VmProperties()->set(unquote(src), unquote(tok + 1));
            }
            else 
            {
                p_env->VmProperties()->set(unquote(src), "");
            }

            STD_FREE(src);
        }
        else if (strncmp(option, "-XX:", 4) == 0)
        {
            TRACE("setting internal property " << option + 4);
            src = strdup(option + 4);
            char* name = unquote(src);
            char* valptr = strchr(src, '=');
            const char* value;
            if(valptr)
            {
                *valptr = '\0';
                value = valptr + 1;
            }
            else 
            {
                if (name[0] == '-' ) {
                    value = "off";
                    ++name;
                } else if (src[0] == '+') {
                    value = "on";
                    ++name;
                } else {
                    value = "";
                    LECHO(28, "Wrong option format {0}" << option);
                    status = JNI_ERR;
                }
            }

            TRACE("parsed internal property " << name << " = " << value << ";");
            p_env->VmProperties()->set(name, value);

            STD_FREE(src);

            if (status != JNI_OK) break;
        }
    }

/*
 * 2. Set predefined values to properties not defined via vm options.
 */
    if (status == JNI_OK)
    {
        init_java_properties(*p_env->JavaProperties());
        init_vm_properties(*p_env->VmProperties());
    }
    apr_pool_clear(prop_pool);
    return status;
}
