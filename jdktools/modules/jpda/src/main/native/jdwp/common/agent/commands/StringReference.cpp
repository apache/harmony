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
#include "StringReference.h"
#include "PacketParser.h"
#include "ClassManager.h"
#include "ExceptionManager.h"


using namespace jdwp;
using namespace StringReference;

//-----------------------------------------------------------------------------
// StringReference -----------------------------------------------------------------

int 
StringReference::ValueHandler::Execute(JNIEnv *jni) 
{

    //INVALID_OBJECT can be thrown below
    jstring stringObject = m_cmdParser->command.ReadStringID(jni);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Value: received: stringID=%p", stringObject));

    // get length of the string
    jsize len = jni->GetStringLength(stringObject);
    jsize utfLen = jni->GetStringUTFLength(stringObject);

    // allocate memory for getting string value
    char* p_string = reinterpret_cast<char*>(GetMemoryManager().Allocate(utfLen + 1 JDWP_FILE_LINE));
    // create auto-free pointer for p_string
    AgentAutoFree autoFree_p_string(p_string JDWP_FILE_LINE);

    // obtain the string's value
    jni->GetStringUTFRegion(stringObject, 0, len, p_string);
    AgentBase::GetClassManager().CheckOnException(jni);

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "Value: send: utfLen=%d, string=%s", utfLen, JDWP_CHECK_NULL(p_string)));

    // write string to the reply
    m_cmdParser->reply.WriteString(p_string, utfLen);

    return JDWP_ERROR_NONE;
}
