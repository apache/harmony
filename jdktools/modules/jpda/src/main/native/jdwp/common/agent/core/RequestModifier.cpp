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

/**
 * @author Pavel N. Vyssotski
 */
// RequestModifier.cpp

#include <string.h>

#include "RequestModifier.h"

using namespace jdwp;

// match signature with pattern omitting first 'L' and last ";"
bool RequestModifier::MatchPattern(const char *signature, const char *pattern)
    const throw()
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
