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
 * @author Viacheslav G. Rybalov
 */
#include "ClassObjectReference.h"
#include "PacketParser.h"
#include "ClassManager.h"

using namespace jdwp;
using namespace ClassObjectReference;

void
ClassObjectReference::ReflectedTypeHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jclass classObject = static_cast<jclass>(m_cmdParser->command.ReadObjectID(jni));
    JDWP_TRACE_DATA("ReflectedType: received: classObject=" << classObject);

    jdwpTypeTag typeTag = AgentBase::GetClassManager().GetJdwpTypeTag(classObject); 

#ifndef NDEBUG
    if (JDWP_TRACE_ENABLED(LOG_KIND_DATA)) {
        jvmtiError err;
        char* signature = 0;
        JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(classObject, &signature, 0));
        JvmtiAutoFree afs(signature);
        JDWP_TRACE_DATA("ReflectedType: send: typeTag=" << typeTag
            << ", typeID=" << classObject
            << ", signature=" << JDWP_CHECK_NULL(signature));
    }
#endif

    m_cmdParser->reply.WriteByte(typeTag);
    m_cmdParser->reply.WriteReferenceTypeID(jni, classObject);
}
