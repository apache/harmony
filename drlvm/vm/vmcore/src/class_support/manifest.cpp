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
 * @author Pavel Pervov
 */  
#include "manifest.h"
#include "jarfile_support.h"

#include <string>
#include <strstream>

/**
 * Checks if string begins with substr
**/
static inline bool begins_with(const char* string, const char* substr)
{
    return ( 0 == strncmp(string, substr, strlen(substr)) );
}

/**
 * Checks if string begins continuation separator, see JAR File Spec.
 * @return  0 - if string doesn't begin with separator; n > 0 - if string 
 * begins with separator consisting of n chars
**/
static inline int skip_continuation_separator(char* string)
{
    if (begins_with(string, "\n "))
        return 2;

    if (begins_with(string, "\r "))
        return 2;

    if (begins_with(string, "\r\n "))
        return 3;

    return 0;
}

/**
 * Removes continuation separators, see JAR File Spec.
 * @return  new length of string, representing manifest.
**/
static inline long cut_continuation_separators(char* manifest)
{
    char* src = manifest - 1;
    char* dst = manifest - 1;

    do
    {
        dst++;
        src++;
        int skip;
        while ( 0 != (skip = skip_continuation_separator(src)) )
        {
            src += skip;
        }

        *dst = *src;
    } while (*src != '\0');

    return (long)(dst - manifest);
}


bool Manifest::Parse(const JarEntry* manifestJe)
{
    assert(manifestJe != NULL);
    m_parsed = false;
    char* manifest;
    long manifestSize;

    manifestSize = manifestJe->GetContentSize();
    if( !manifestSize ) {
        return false;
    }
    manifest = new char[manifestSize + 1];
    if( !manifest ) {
        return false;
    }
    if(!manifestJe->GetContent(reinterpret_cast<unsigned char*>(manifest))) {
        delete[] manifest;
        return false;
    }
    manifest[manifestSize] = '\0';

    manifestSize = cut_continuation_separators(manifest);

    char* pointer = manifest;
    const char WHITESPACE_CHARS[] = " \t\n\r";
    const char PROPERTY_NAME_SEPARATOR[] = ": ";
    const char PROPERTY_VALUE_SEPARATOR[] = "\r\n";

    // parse manifest line by line
    do
    {
        // parse property name
        // skip useless characters
        size_t skipSpace = strspn( pointer, WHITESPACE_CHARS );
        pointer += skipSpace;
        // set property name
        char *propName = pointer;
        // find property value
        char *nextPart = strstr( pointer, PROPERTY_NAME_SEPARATOR );
        if( !nextPart ) {
            // no property value found - skip manifest tail
            break;
        }
        // find first whitespace character after property name, cut tail
        size_t findSpace = strcspn( pointer, WHITESPACE_CHARS );
        if( findSpace < (size_t)(nextPart - pointer) ) {
            pointer[findSpace] = '\0';
        } else {
            *nextPart = '\0';
        }
        pointer = nextPart + strlen(PROPERTY_NAME_SEPARATOR);

        // parse property value
        // find first non-whitespace character
        skipSpace = strspn( pointer, WHITESPACE_CHARS );
        pointer += skipSpace;
        // set property value starts here
        char *propValue = pointer;
        // set next property
        nextPart = strpbrk( pointer, PROPERTY_VALUE_SEPARATOR );
        if( !nextPart ) {
            // last property
            nextPart = manifest + manifestSize;
        }
        // find end of value
        pointer = nextPart - 1;
        do {
            if( !isspace( *pointer ) ) {
                break;
            }
            pointer--;
        } while( pointer > propValue );
        // find first whitespace character after property value, cut tail
        *(pointer + 1) = '\0';
        pointer = nextPart + 1;

        // set property
        if( IsMainProperty( propName ) ) {
            m_main.set((const char*)propName, (const char*)propValue);
        }
    } while( pointer - manifest <  manifestSize );

    delete[] manifest;

    m_parsed = true;
    return true;
}


Manifest::~Manifest()
{
}

bool Manifest::IsMainProperty( const char* prop )
{
#define RET_IF_PROP( prop_name ) \
    if( strstr( prop, prop_name ) == prop ) return true

    RET_IF_PROP("Manifest-Version");
    RET_IF_PROP("Created-By");
    RET_IF_PROP("Signature-Version");
    RET_IF_PROP("Class-Path");
    RET_IF_PROP("Main-Class");
    RET_IF_PROP("Extension-List");
    RET_IF_PROP("Implementation-Title");
    RET_IF_PROP("Implementation-Version");
    RET_IF_PROP("Implementation-Vendor");
    RET_IF_PROP("Implementation-Vendor-Id");
    RET_IF_PROP("Implementation-URL");
    RET_IF_PROP("Specification-Title");
    RET_IF_PROP("Specification-Version");
    RET_IF_PROP("Specification-Vendor");
    RET_IF_PROP("Sealed");
    return false;
}
