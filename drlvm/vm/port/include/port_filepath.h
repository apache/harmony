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

#ifndef _PORT_FILEPATH_H_
#define _PORT_FILEPATH_H_

/**
* @file
* Filepath manipulation routines
*
*/

#include "port_general.h"
#include <apr_pools.h>

#ifdef __cplusplus
extern "C" {
#endif
/**
 * @defgroup port_filepath Filepath manipulation routines
 * @ingroup port_apr
 * @{
 */

/**
 * @defgroup file_sep File system separators definitions.
 * @{
 */
#ifdef DOXYGEN
/** The platform-specific file name separator char delimiting entities in a filepath. */
#define PORT_FILE_SEPARATOR
/** The platform-specific file path separator char delimiting filepaths in a path list. */
#define PORT_PATH_SEPARATOR
/** The platform-specific file name separator as a string. */
#define PORT_FILE_SEPARATOR_STR
/** The platform-specific file name separator as a string. */
#define PORT_PATH_SEPARATOR_STR
#endif
/** @} */

#ifdef PLATFORM_POSIX
#   define PORT_FILE_SEPARATOR '/'
#   define PORT_PATH_SEPARATOR ':'
#   define PORT_FILE_SEPARATOR_STR "/"
#   define PORT_PATH_SEPARATOR_STR ":"
#elif PLATFORM_NT
#   define PORT_FILE_SEPARATOR '\\'
#   define PORT_PATH_SEPARATOR ';'
#   define PORT_FILE_SEPARATOR_STR "\\"
#   define PORT_PATH_SEPARATOR_STR ";"
#endif

/**
* Sticks together filepath parts delimiting them by a platform-specific file separator.
* A typical example is obtaining a path to file by the directory path and the file name.
* @param root  - the beginning of the file path
* @param trail - the ending of the file path
* @return The resulting filepath.
*/
APR_DECLARE(char *) port_filepath_merge(const char* root,
                          const char* trail,
                          apr_pool_t* pool);


/**
* Provides the <i>canonical form</i> of the specified path.
* This means that the function returns the standardized absolute path to a resource
* that can be addressed via different file paths.
* The canonical form is reasonable to use as a resource identifier. 
* @note The canonical form cannot be guaranteed to be the only unique name
* due to various file systems specifics.
* @param original - the path to be canonicalized
* @param pool     - the pool to allocate return buffer
* @return The canonical name of the specified path.
*/
APR_DECLARE(char*) port_filepath_canonical(const char* original,
                                      apr_pool_t* pool);

/**
* Finds short file name in the specified filepath.
* Returns the pointer to short file name in the source string.
* If source filepath contains no path separators, returns filepath pointer itself.
* @param root  - the beginning of the file path
* @return The pointer to short file name.
*/
APR_DECLARE(const char*) port_filepath_basename(const char* filepath);

/** @} */


#ifdef __cplusplus
}
#endif
#endif /*_PORT_FILEPATH_H_*/
