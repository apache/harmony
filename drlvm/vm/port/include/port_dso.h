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
* @author Alexey V. Varlamov
*/  

#ifndef _PORT_DSO_H_
#define _PORT_DSO_H_

/**
* @file
* Dynamic binaries handling
*/

#include "open/types.h"
#include "port_general.h"
#include <apr_pools.h>
#include <apr_dso.h>

/**
 * @defgroup port_dso Dynamic binaries handling
 * @ingroup port_apr
 * @{
 */

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Decorates the shared library name (.dll <-> lib*.so).
 * The <i>name</i> parameter should be double-quoted string.
 */
#ifdef DOXYGEN
#define PORT_DSO_NAME(name)
#endif

#ifdef PLATFORM_POSIX
#   define PORT_DSO_PREFIX "lib"
#   define PORT_DSO_EXT ".so"
#elif PLATFORM_NT
#   define PORT_DSO_PREFIX ""
#   define PORT_DSO_EXT ".dll"
#endif

#define PORT_DSO_NAME(name) PORT_DSO_PREFIX name PORT_DSO_EXT

/**
 * @defgroup dso_modes Shared library binding modes
 * @{
 */
/** The APR-default binding mode flag (?) */
#define PORT_DSO_DEFAULT 0
/** 
* Eager mode flag: the resolution/relocation of dynamic symbols should 
* be performed at library loading.
*/
#define PORT_DSO_BIND_NOW 0x1
/** 
* Lazy mode flag: the resolution/relocation of dynamic symbols should 
* be deferred until reference to the symbol encountered.
*/
#define PORT_DSO_BIND_DEFER 0x2
/** @} */

/**
* Loads the shared binary file, an executable or a library.
* @param[out] handle - a handle to the loaded object
* @param path        - the path to the binary
* @param mode        - the flag to control resolution/relocation of dynamic symbols in
                       the loaded object, see <code>PORT_DSO_BIND_*</code> defines.
* @param pool        - storage to allocate the returned handle
* @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
*/
APR_DECLARE(apr_status_t) port_dso_load_ex(apr_dso_handle_t** handle,
                                      const char* path,
                                      U_32 mode,
                                      apr_pool_t* pool);


/**
* Returns the list of directories wher the OS searches for libraries.
* @param[out] path - the pointer to the path list
* @param pool      - the storage to allocate the returned buffer
* @return <code>APR_SUCCESS</code> if OK; otherwise, an error code.
*/
APR_DECLARE(apr_status_t) port_dso_search_path(char** path,
                                        apr_pool_t* pool);

/**
 * Decorates shared library name (.dll <-> lib*.so).
 * @param dl_name - the name of the shared library
 * @param pool    - storage to allocate the returned handle
 * @return The platform-specific filename for the library.
 */
APR_DECLARE(char *) port_dso_name_decorate(const char* dl_name,
                            apr_pool_t* pool);

/** @} */

#ifdef __cplusplus
}
#endif
#endif /*_PORT_DSO_H_*/
