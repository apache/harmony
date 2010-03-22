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
#include "ClassObjectReference.h"
#include "PacketParser.h"
#include "ClassManager.h"
#include "ExceptionManager.h"


using namespace jdwp;
using namespace ClassObjectReference;

int
ClassObjectReference::ReflectedTypeHandler::Execute(JNIEnv *jni)
{
    jclass classObject = static_cast<jclass>(m_cmdParser->command.ReadObjectID(jni));
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReflectedType: received: classObject=%p", classObject));

    jdwpTypeTag typeTag = AgentBase::GetClassManager().GetJdwpTypeTag(classObject); 

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetClassSignature(classObject, &signature, 0));
        JvmtiAutoFree afs(signature);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReflectedType: send: typeTag=%d, typeID=%d, signature=%s", typeTag, classObject, JDWP_CHECK_NULL(signature)));
    }
#endif

    m_cmdParser->reply.WriteByte((jbyte)typeTag);
    m_cmdParser->reply.WriteReferenceTypeID(jni, classObject);

    return JDWP_ERROR_NONE;
}
