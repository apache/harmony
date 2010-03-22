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
 * @author Euguene Ostrovsky
 */  
#define LOG_DOMAIN "vmls"
#include "cxxlog.h"
#include "hyvmls.h"

UDATA JNICALL 
HYVMLSAllocKeys(JNIEnv *env, UDATA *pInitCount,...)
{
    TRACE("vmls->HYVMLSAllocKeys(): pInitCount = " << pInitCount <<
            "\t*pInitCount = " << *pInitCount)
    return 0;
}

void JNICALL 
HYVMLSFreeKeys(JNIEnv *env, UDATA *pInitCount,...)
{
    TRACE("vmls->HYVMLSFreeKeys(): pInitCount = " << pInitCount <<
        "\t*pInitCount = " << *pInitCount);
    return;
}

void* JNICALL 
J9VMLSGet(JNIEnv *env, void *key)
{
    TRACE("vmls->J9VMLSGet(): key = " << key << "\t*key = " << 
            ((key) ? *(int *)key : 0 ) );
    return key;
}

void* JNICALL
J9VMLSSet(JNIEnv *env, void **pKey, void *value)
{
    TRACE("vmls->J9VMLSSet(): pKey = " << pKey << "\t*pKey = " << *pKey <<
        "\n\tvalue = " << value << "\t*value = " << *(unsigned *)value);
    *pKey = value;
    return value;
}
