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
#include <string.h>
#include "ArrayType.h"
#include "PacketParser.h"
#include "ClassManager.h"

using namespace jdwp;
using namespace ArrayType;

void
ArrayType::NewInstanceHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jclass cls = m_cmdParser->command.ReadReferenceTypeID(jni);
    jint length = m_cmdParser->command.ReadInt();

    JDWP_ASSERT(cls != 0);
    char* signature = 0;
    jvmtiError err;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetClassSignature(cls, &signature, 0));
    JvmtiAutoFree afv1(signature);
    if (err != JVMTI_ERROR_NONE) {
        throw AgentException(err);
    }

    JDWP_TRACE_DATA("NewInstance: received: refTypeID=" << cls
        << ", length=" << length
        << ", signature=" << JDWP_CHECK_NULL(signature));

    if ((signature == 0) || (strlen(signature) < 2)) {
        throw AgentException(JDWP_ERROR_INVALID_OBJECT);
    }
    if(signature[0] != '[') {
        throw AgentException(JDWP_ERROR_INVALID_ARRAY);
    }

    jarray arr = 0;
    switch (signature[1]) {
        case 'Z': {
            JDWP_TRACE_DATA("NewInstance: new boolean array");
            arr = jni->NewBooleanArray(length);
            break;
        }
        case 'B': {
            JDWP_TRACE_DATA("NewInstance: new byte array");
            arr = jni->NewByteArray(length);
            break;
        }
        case 'C': {
            JDWP_TRACE_DATA("NewInstance: new char array");
            arr = jni->NewCharArray(length);
            break;
        }
        case 'S': {
            JDWP_TRACE_DATA("NewInstance: new short array");
            arr = jni->NewShortArray(length);
            break;
        }
        case 'I': {
            JDWP_TRACE_DATA("NewInstance: new int array");
            arr = jni->NewIntArray(length);
            break;
        }
        case 'J': {
            JDWP_TRACE_DATA("NewInstance: new long array");
            arr = jni->NewLongArray(length);
            break;
        }
        case 'F': {
            arr = jni->NewFloatArray(length);
            JDWP_TRACE_DATA("NewInstance: new float array");
            break;
        }
        case 'D': {
            JDWP_TRACE_DATA("NewInstance: new double array");
            arr = jni->NewDoubleArray(length);
            break;
        }
        case 'L':
        case '[': {
            char* name = GetClassManager().GetClassName(&signature[1]);
            JvmtiAutoFree jafn(name);
            jobject loader;
            JVMTI_TRACE(err, GetJvmtiEnv()->GetClassLoader(cls, &loader));
            if (err != JVMTI_ERROR_NONE) {
                throw AgentException(err);
            }
            jclass elementClass =
                GetClassManager().GetClassForName(jni, name, loader);
            JDWP_TRACE_DATA("NewInstance: new object array: "
                "class=" << JDWP_CHECK_NULL(name));
            arr = jni->NewObjectArray(length, elementClass, 0);
            break;
        }
        default:
            JDWP_TRACE_DATA("NewInstance: bad type signature: "
                << JDWP_CHECK_NULL(signature));
            throw AgentException(JDWP_ERROR_INVALID_ARRAY);
    }
    GetClassManager().CheckOnException(jni);
    if (arr == 0) {
        throw AgentException(JDWP_ERROR_OUT_OF_MEMORY);
    }

    JDWP_TRACE_DATA("NewInstance: send: tag=" << JDWP_TAG_ARRAY
                << ", newArray=" << arr);
    m_cmdParser->reply.WriteByte(JDWP_TAG_ARRAY);
    m_cmdParser->reply.WriteArrayID(jni, arr);
}
