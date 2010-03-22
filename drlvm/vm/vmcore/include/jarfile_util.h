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
 * @author Pavel Rebriy
 */  

#ifndef __JARFILE_UTIL_H__
#define __JARFILE_UTIL_H__

#include "manifest.h"
#include "jarfile_support.h"

//the file's extension
#define ZIP_EXTENSION ".zip"    
#define JAR_EXTENSION ".jar"

inline bool file_is_archive(const char* filename)
{
    size_t len = strlen(filename);
    static size_t len_ext1 = strlen(JAR_EXTENSION);
    static size_t len_ext2 = strlen(ZIP_EXTENSION);

    if(len > len_ext1
#ifdef PLATFORM_NT
        && !memcmp(filename + len - len_ext1, JAR_EXTENSION, len_ext1))
#else
        && !strcasecmp(filename + len - len_ext1, JAR_EXTENSION))
#endif // PLATFORM_NT
    {
        // it is a jar file
        return true;
    } else if( len > len_ext2
#ifdef PLATFORM_NT
        && !memcmp(filename + len - len_ext2, ZIP_EXTENSION, len_ext2))
#else
        && !strcasecmp(filename + len - len_ext2, ZIP_EXTENSION))
#endif // PLATFORM_NT
    {
        // it is a zip file
        return true;
    }
    return false;
} // file_is_archive

inline const char* archive_get_manifest_attr(JarFile *jarfl, const char *manifest_attr)
{
    assert(jarfl);

    // get archive manifest
    Manifest *manifest = jarfl->GetManifest();
    if(!manifest) {
        return NULL;
    }
    // get attribute of manifest
    Properties *prop = manifest->GetMainProperties();
    return prop->get(manifest_attr);
} // archive_get_manifest_attr

inline const char* archive_get_main_class_name(JarFile *jarfl)
{
    return archive_get_manifest_attr(jarfl, "Main-Class");
} // archive_get_main_class_name

inline const char* archive_get_class_path(JarFile *jarfl)
{
    return archive_get_manifest_attr(jarfl, "Class-Path");
} // archive_get_class_path

#endif // __JARFILE_UTIL_H__

