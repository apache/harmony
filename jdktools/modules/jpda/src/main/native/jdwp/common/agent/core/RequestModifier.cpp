/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
#include "RequestModifier.h"

#include <string.h>

using namespace jdwp;

// match signature with pattern omitting first 'L' and last ";"
bool RequestModifier::MatchPattern(const char *signature, const char *pattern)
    const
{
    if (signature == 0) {
        return false;
    }

    const size_t signatureLength = strlen(signature);
    if (signatureLength < 2) {
        return false;
    }

    const size_t patternLength = strlen(pattern);
    if (pattern[0] == '*') {
        return (signatureLength > patternLength &&
            strncmp(&pattern[1], &signature[signatureLength-patternLength],
                patternLength-1) == 0);
    } else if (pattern[patternLength-1] == '*') {
        return (strncmp(pattern, &signature[1], patternLength-1) == 0);
    } else {
        return (patternLength == signatureLength-2 &&
            strncmp(pattern, &signature[1], patternLength) == 0);
    }
}

bool SourceNameMatchModifier::Apply(JNIEnv* jni, EventInfo &eInfo)
{
    JDWP_ASSERT(eInfo.cls != 0);
    jclass jvmClass = eInfo.cls;

    char* sourceDebugExtension = 0;
    char* sourceFileName = 0;
    jvmtiError err;
    // Get source name determined by SourceDebugExtension
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetSourceDebugExtension(jvmClass,
        &sourceDebugExtension));

    if (err != JVMTI_ERROR_NONE) {
        // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY,JVMTI_ERROR_ABSENT_INFORMATION,
        // JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
        if(err == JVMTI_ERROR_ABSENT_INFORMATION) {                   
            // SourceDebugExtension is absent, get source name from SourceFile
            JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetSourceFileName(jvmClass,
                &sourceFileName));

            if (err != JVMTI_ERROR_NONE) {
                // Can be: JVMTI_ERROR_MUST_POSSESS_CAPABILITY, JVMTI_ERROR_ABSENT_INFORMATION,
                // JVMTI_ERROR_INVALID_CLASS, JVMTI_ERROR_NULL_POINTER
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "JDWP error in SourceNameMatchModifier.Apply calling GetSourceFileName: %d", err));
                return false;
            }
            JvmtiAutoFree autoFreeFieldName(sourceFileName);
            bool result =  MatchPatternSourceName(sourceFileName, m_pattern);

            if(!result) {
                char *p_orig = (char*) GetMemoryManager().Allocate(strlen(m_pattern)+1 JDWP_FILE_LINE);
                char *p = p_orig;
                strcpy(p, m_pattern);
                // replace '.' with '/' to be matched with signature
                for (; *p != '\0'; p++) {
                    if (*p == '.') {
                        *p = '/';
                    }
                }
                JDWP_ASSERT(eInfo.signature != 0);
                result = MatchPattern(eInfo.signature, p_orig);
                GetMemoryManager().Free(p_orig JDWP_FILE_LINE);
                return result;
            } else {
                return true;
            }
         } else { 
             JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "JDWP error in SourceNameMatchModifier.Apply calling GetSourceDebugExtension: %d", err));
             return false;
         }
    }
    JvmtiAutoFree autoFreeDebugExtension(sourceDebugExtension);            
      
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "JDWP sourceDebugExtension: %s", sourceDebugExtension));

    // We want to get the 2nd token here, split by '\n'
    char *tok = NULL;
    tok = strtok(sourceDebugExtension, "\n"); // 1st token
    if (tok == NULL) return false;
    tok = strtok(NULL, "\n");
    if (tok == NULL) return false;
    if (MatchPatternSourceName(tok, m_pattern)) return true;
    while(tok = strtok(NULL, "\n")) {
        if (strlen(tok) >= 2) {
            if (tok[0] == '*' && tok[1] == 'F' && tok[2] == '\0') {
                tok = strtok(NULL, "\n");
                if (tok == NULL) return false;
                while (tok[0] != '*') {
                    if (tok[0] == '+') {
                        //format: + 1 HelloWorld.java\npath/HelloWorld.java
                        // skip plus
                        tok++;
                        // skip spaces
                        while (tok[0] == ' ' && tok[0] != 0) tok++;
                        // skip int
                        while (tok[0] >= '0' && tok[0] <= '9'
                               && tok[0] != 0) tok++;
                        // skip spaces
                        while (tok[0] == ' ' && tok[0] != 0) tok++;
                        if (tok[0] == 0) break;
                        if (MatchPatternSourceName(tok, m_pattern)) {
                            return true;
                        }
                        tok = strtok(NULL, "\n");
                        if (tok == NULL) return false;
                        if (MatchPatternSourceName(tok, m_pattern)) {
                            return true;
                        }
                    } else if (tok[0] >= '0' && tok[0] <= '9') {
                        // format: 1 HelloWorld.java
                        // skip the int
                        while (tok[0] >= '0' && tok[0] <= '9'
                               && tok[0] != 0) tok++;
                        // skip spaces
                        while (tok[0] == ' ' && tok[0] != 0) tok++;
                        if (tok[0] == 0) break;
                        if (MatchPatternSourceName(tok, m_pattern)) {
                            return true;
                        }
                    }
                    tok = strtok(NULL, "\n");
                    if (tok == NULL) return false;
                }
            }
        }
    }
    return false;
}

// match source name with pattern
bool SourceNameMatchModifier::MatchPatternSourceName(const char *sourcename, const char *pattern)
    const 
{
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "JDWP in SourceNameMatchModifier::MatchPatternSourceName(%s, %s)", sourcename, pattern));
   if(sourcename == 0) {
        return false;
    }

    const size_t sourcenameLength = strlen(sourcename);
    const size_t patternLength = strlen(pattern);

    if((sourcenameLength -  patternLength + 1) < 0) { 
        return false;
    }
    if (pattern[0] == '*') {
        return (strcmp(&pattern[1], &sourcename[sourcenameLength-patternLength+1]) == 0);
    } else if (pattern[patternLength-1] == '*') {
        return (strncmp(pattern, &sourcename[0], patternLength-1) == 0);
    } else {
         return (patternLength == sourcenameLength &&
            strncmp(pattern, &sourcename[0], patternLength) == 0);
    }
}


