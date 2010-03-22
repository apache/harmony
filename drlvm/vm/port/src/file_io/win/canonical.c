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

#include <direct.h>
#include <apr_strings.h>

#include "port_filepath.h"

#include <windows.h>

APR_DECLARE(char *) port_filepath_canonical(const char* original,
												 apr_pool_t* pool)
{
    char ch;
    char *path_name = (char*)original;
    char cano_path[_MAX_PATH];
	char *cp = cano_path;

	/* if the original is a relative path, prepend current dir to it */
    if (*original != '/' &&
        *original != '\\' &&    
        *(original + 1) != ':' )
	{
        _getcwd(cp, _MAX_PATH);
        while(*cp)cp++;
        *(cp++) = PORT_FILE_SEPARATOR;
    }

    while((ch = *(path_name++))){
        switch(ch){
        case '\\':
        case '/':
            *(cp++) = PORT_FILE_SEPARATOR;
            break;
        case '.':{
            switch(*path_name){
            case '.': // we encounter '..'
                path_name += 2; //skip the following "./"
                if (*(cp - 1) == PORT_FILE_SEPARATOR) //if cano_path now is "/xx/yy/"
                    --cp; //skip backward the tail '/'
                *cp = '\0';
                cp = strrchr(cano_path, PORT_FILE_SEPARATOR); //up a level
                ++cp;
                break;
            case '\\':
            case '/':
                ++path_name;
                break; 
            default: 
                *(cp++) = ch;
            }
            break; }
        default:
            *(cp++) = ch;
        }
    }
    *cp = '\0';
	return apr_pstrdup(pool, strlwr(cano_path));
}
