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
#include "StackFrame.h"
#include "PacketParser.h"
#include "ClassManager.h"
#include "ThreadManager.h"

using namespace jdwp;
using namespace StackFrame;

void
StackFrame::GetValuesHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jthread thread = m_cmdParser->command.ReadThreadID(jni);
    jint frame = m_cmdParser->command.ReadFrameID(jni);
    jint slots = m_cmdParser->command.ReadInt();

    JDWP_TRACE_DATA("GetValues: received: frameID=" << frame
        << ", threadID=" << thread 
        << ", slots=" << slots);

    if (thread == 0) {
        throw AgentException(JDWP_ERROR_INVALID_THREAD);
    }
    if (slots < 0) {
        throw AgentException(JDWP_ERROR_ILLEGAL_ARGUMENT);
    }

    m_cmdParser->reply.WriteInt(slots);
    for (int i = 0; i < slots; i++) {
        jint slot = m_cmdParser->command.ReadInt();
        jdwpTag sigbyte = static_cast<jdwpTag>(m_cmdParser->command.ReadByte());
        jvmtiError err = JVMTI_ERROR_NONE;
        jvalue resValue;
        switch (sigbyte) {
            case JDWP_TAG_BOOLEAN:
            case JDWP_TAG_BYTE:
            case JDWP_TAG_CHAR:
            case JDWP_TAG_SHORT:
            case JDWP_TAG_INT:
                jint ivalue;
                JVMTI_TRACE(err, GetJvmtiEnv()->GetLocalInt(thread, frame, slot, &ivalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                switch (sigbyte) {
                    case JDWP_TAG_BOOLEAN:
                        resValue.z = static_cast<jboolean>(ivalue);
                        JDWP_TRACE_DATA("GetValues: slot#=" << i 
                            << ", value=(boolean)" << resValue.z);
                        break;
                    case JDWP_TAG_BYTE:
                        resValue.b = static_cast<jbyte>(ivalue);
                        JDWP_TRACE_DATA("GetValues: slot#=" << i 
                            << ", value=(byte)" << resValue.b);
                        break;
                    case JDWP_TAG_CHAR:
                        resValue.c = static_cast<jchar>(ivalue);
                        JDWP_TRACE_DATA("GetValues: slot#=" << i 
                            << ", value=(char)" << resValue.c);
                        break;
                    case JDWP_TAG_SHORT:
                        resValue.s = static_cast<jshort>(ivalue);
                        JDWP_TRACE_DATA("GetValues: slot#=" << i 
                            << ", value=(short)" << resValue.s);
                        break;
                    case JDWP_TAG_INT:
                        resValue.i = ivalue;
                        JDWP_TRACE_DATA("GetValues: slot#=" << i 
                            << ", value=(int)" << resValue.i);
                        break;
                }
                m_cmdParser->reply.WriteValue(jni, sigbyte, resValue);
                break;
            case JDWP_TAG_LONG:
                jlong lvalue;
                JVMTI_TRACE(err, GetJvmtiEnv()->GetLocalLong(thread, frame, slot, &lvalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                resValue.j = lvalue;
                JDWP_TRACE_DATA("GetValues: slot#=" << i 
                    << ", value=(long)" << resValue.j);
                m_cmdParser->reply.WriteValue(jni, sigbyte, resValue);
                break;
            case JDWP_TAG_FLOAT:
                jfloat fvalue;
                JVMTI_TRACE(err, GetJvmtiEnv()->GetLocalFloat(thread, frame, slot, &fvalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                resValue.f = fvalue;
                JDWP_TRACE_DATA("GetValues: slot#=" << i 
                    << ", value=(float)" << resValue.f);
                m_cmdParser->reply.WriteValue(jni, sigbyte, resValue);
                break;
            case JDWP_TAG_DOUBLE:
                jdouble dvalue;
                JVMTI_TRACE(err, GetJvmtiEnv()->GetLocalDouble(thread, frame, slot, &dvalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                resValue.d = dvalue;
                JDWP_TRACE_DATA("GetValues: slot#=" << i 
                    << ", value=(double)" << resValue.d);
                m_cmdParser->reply.WriteValue(jni, sigbyte, resValue);
                break;
            case JDWP_TAG_OBJECT:
            case JDWP_TAG_ARRAY:
            case JDWP_TAG_STRING:
            case JDWP_TAG_THREAD:
            case JDWP_TAG_THREAD_GROUP:
            case JDWP_TAG_CLASS_LOADER:
            case JDWP_TAG_CLASS_OBJECT: {
                jobject ovalue;
                JVMTI_TRACE(err, GetJvmtiEnv()->GetLocalObject(thread, frame, slot, &ovalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                jdwpTag tag = GetClassManager().GetJdwpTag(jni, ovalue);
                if ((sigbyte != JDWP_TAG_OBJECT) && (sigbyte != tag)) {
                    throw AgentException(JDWP_ERROR_INVALID_TAG);
                }
                resValue.l = ovalue;
                JDWP_TRACE_DATA("GetValues: slot#=" << i 
                    << ", tag=" << tag
                    << ", value=(object)" << resValue.l);
                m_cmdParser->reply.WriteValue(jni, tag, resValue);
                break;
            }
            default:
                JDWP_TRACE_DATA("GetValues: bad slot tag: slot#=" << i 
                    << ", tag=" << sigbyte);
                throw AgentException(JDWP_ERROR_INVALID_TAG);
        }
        if (err != JVMTI_ERROR_NONE) {
            jdwpError error;
            if (err == JVMTI_ERROR_TYPE_MISMATCH) {
                error = JDWP_ERROR_INVALID_TAG;
            } else if (err == JVMTI_ERROR_OPAQUE_FRAME) {
                error = JDWP_ERROR_INVALID_FRAMEID;
            } else if (err == JVMTI_ERROR_THREAD_NOT_ALIVE) {
                error = JDWP_ERROR_INVALID_THREAD;
            } else if (err == JVMTI_ERROR_ILLEGAL_ARGUMENT) {
                error = JDWP_ERROR_INVALID_FRAMEID;
            } else if (err == JVMTI_ERROR_NO_MORE_FRAMES) {
                error = JDWP_ERROR_INVALID_FRAMEID;
            } else {
                error = (jdwpError)err;
            }
            throw AgentException(error);
        }
    }
}

void
StackFrame::SetValuesHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jthread thread = m_cmdParser->command.ReadThreadID(jni);
    jint frame = m_cmdParser->command.ReadFrameID(jni);
    jint slotValues = m_cmdParser->command.ReadInt();

    JDWP_TRACE_DATA("SetValues: received: frameID=" << frame
        << ", threadID=" << thread 
        << ", slots=" << slotValues);

    if (thread == 0) {
        throw AgentException(JDWP_ERROR_INVALID_THREAD);
    }
    if (slotValues < 0) {
        throw AgentException(JDWP_ERROR_ILLEGAL_ARGUMENT);
    }

    for (int i = 0; i < slotValues; i++) {
        jint slot = m_cmdParser->command.ReadInt();
        jdwpTaggedValue taggedValue = m_cmdParser->command.ReadValue(jni);
        JDWP_TRACE_DATA("SetValues: slot#" << i << ": taggedValue=" << taggedValue.tag);
//        jdwpTag sigbyte = taggedValue.tag;
        jvmtiError err = JVMTI_ERROR_NONE;
        switch (taggedValue.tag) {
            case JDWP_TAG_BOOLEAN:
            case JDWP_TAG_BYTE:
            case JDWP_TAG_CHAR:
            case JDWP_TAG_SHORT:
            case JDWP_TAG_INT:
                jint ivalue;
                switch (taggedValue.tag) {
                    case JDWP_TAG_BOOLEAN:
                        ivalue = static_cast<jint>(taggedValue.value.z);
                        JDWP_TRACE_DATA("SetValues: slot#=" << i 
                            << ", value=(boolean)" << taggedValue.value.z);
                        break;
                    case JDWP_TAG_BYTE:
                        ivalue = static_cast<jint>(taggedValue.value.b);
                        JDWP_TRACE_DATA("SetValues: slot#=" << i 
                            << ", value=(byte)" << taggedValue.value.b);
                        break;
                    case JDWP_TAG_CHAR:
                        ivalue = static_cast<jint>(taggedValue.value.c);
                        JDWP_TRACE_DATA("SetValues: slot#=" << i 
                            << ", value=(char)" << taggedValue.value.c);
                        break;
                    case JDWP_TAG_SHORT:
                        ivalue = static_cast<jint>(taggedValue.value.s);
                        JDWP_TRACE_DATA("SetValues: slot#=" << i 
                            << ", value=(short)" << taggedValue.value.s);
                        break;
                    case JDWP_TAG_INT:
                        ivalue = taggedValue.value.i;
                        JDWP_TRACE_DATA("SetValues: slot#=" << i 
                            << ", value=(int)" << taggedValue.value.i);
                        break;
                }
                JVMTI_TRACE(err, GetJvmtiEnv()->SetLocalInt(thread, frame, slot, ivalue));
                break;
            case JDWP_TAG_LONG: {
                jlong lvalue = taggedValue.value.j;
                JDWP_TRACE_DATA("SetValues: slot#=" << i 
                    << ", value=(long)" << taggedValue.value.j);
                JVMTI_TRACE(err, GetJvmtiEnv()->SetLocalLong(thread, frame, slot, lvalue));
                break;
            }
            case JDWP_TAG_FLOAT: {
                jfloat fvalue = taggedValue.value.f;
                JDWP_TRACE_DATA("SetValues: slot#=" << i 
                    << ", value=(float)" << taggedValue.value.f);
                JVMTI_TRACE(err, GetJvmtiEnv()->SetLocalFloat(thread, frame, slot, fvalue));
                break;
            }
            case JDWP_TAG_DOUBLE: {
                jdouble dvalue = taggedValue.value.d;
                JDWP_TRACE_DATA("SetValues: slot#=" << i 
                    << ", value=(double)" << taggedValue.value.d);
                JVMTI_TRACE(err, GetJvmtiEnv()->SetLocalDouble(thread, frame, slot, dvalue));
                break;
            }
            case JDWP_TAG_OBJECT:
            case JDWP_TAG_ARRAY:
            case JDWP_TAG_STRING:
            case JDWP_TAG_THREAD:
            case JDWP_TAG_THREAD_GROUP:
            case JDWP_TAG_CLASS_LOADER:
            case JDWP_TAG_CLASS_OBJECT: {
                jobject ovalue = taggedValue.value.l;
                JDWP_TRACE_DATA("SetValues: slot#=" << i 
                    << ", value=(object)" << taggedValue.value.l);
                JVMTI_TRACE(err, GetJvmtiEnv()->SetLocalObject(thread, frame, slot, ovalue));
                break;
            }
            default:
                JDWP_TRACE_DATA("SetValues: bad value tag: slot#=" << i 
                    << ", tag=" << taggedValue.tag);
                throw AgentException(JDWP_ERROR_INVALID_TAG);
        }
        if (err != JVMTI_ERROR_NONE) {
            jdwpError error;
            if (err == JVMTI_ERROR_TYPE_MISMATCH) {
                error = JDWP_ERROR_INVALID_TAG;
            } else if (err == JVMTI_ERROR_OPAQUE_FRAME) {
                error = JDWP_ERROR_INVALID_FRAMEID;
            } else if (err == JVMTI_ERROR_THREAD_NOT_ALIVE) {
                error = JDWP_ERROR_INVALID_THREAD;
            } else if (err == JVMTI_ERROR_ILLEGAL_ARGUMENT) {
                error = JDWP_ERROR_INVALID_FRAMEID;
            } else if (err == JVMTI_ERROR_NO_MORE_FRAMES) {
                error = JDWP_ERROR_INVALID_FRAMEID;
            } else {
                error = (jdwpError)err;
            }
            throw AgentException(error);
        }
    }
}

void 
StackFrame::ThisObjectHandler::CheckErr(jvmtiError err) throw(AgentException)
{
    if (err != JVMTI_ERROR_NONE) {
        jdwpError error;
        if (err == JVMTI_ERROR_OPAQUE_FRAME) {
            error = JDWP_ERROR_INVALID_FRAMEID;
        } else if (err == JVMTI_ERROR_THREAD_NOT_ALIVE) {
            error = JDWP_ERROR_INVALID_THREAD;
        } else if (err == JVMTI_ERROR_ILLEGAL_ARGUMENT) {
            error = JDWP_ERROR_INVALID_FRAMEID;
        } else if (err == JVMTI_ERROR_NO_MORE_FRAMES) {
            error = JDWP_ERROR_INVALID_FRAMEID;
        } else {
            error = (jdwpError)err;
        }
        throw AgentException(error);
    }
}

void
StackFrame::ThisObjectHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jthread thread = m_cmdParser->command.ReadThreadID(jni);
    
    if (thread == 0) {
        throw AgentException(JDWP_ERROR_INVALID_THREAD);
    }
    if (!GetThreadManager().IsSuspended(thread))
        throw AgentException(JVMTI_ERROR_THREAD_NOT_SUSPENDED);

    jint frame = m_cmdParser->command.ReadFrameID(jni); // frame == depth
    JDWP_TRACE_DATA("ThisObject: received: threadID=" << thread 
        << ", frameID=" << frame);

    jvmtiError err;
    jint allCount;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetFrameCount(thread, &allCount));
    CheckErr(err);
    JDWP_ASSERT(allCount > 0);

    jvmtiFrameInfo* frames = static_cast<jvmtiFrameInfo*>
        (GetMemoryManager().Allocate(allCount * sizeof(jvmtiFrameInfo) JDWP_FILE_LINE));
    AgentAutoFree af(frames JDWP_FILE_LINE);

    jint count;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetStackTrace(thread, 0, allCount, frames, &count));
    CheckErr(err);
    JDWP_ASSERT(count <= allCount);
    JDWP_ASSERT(frame <= count);

    jvmtiFrameInfo& frameInfo = frames[frame];

    jint modifiers;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetMethodModifiers(frameInfo.method, &modifiers));
    CheckErr(err);

    jvalue resValue;
    if ((modifiers & (ACC_STATIC | ACC_NATIVE)) != 0) {
        JDWP_TRACE_DATA("ThisObject: null this for method: "
            << "modifiers=" << hex << modifiers
            << ", static=" << hex << (modifiers & ACC_STATIC)
            << ", native=" << hex << (modifiers & ACC_NATIVE));
        resValue.l = 0;
        m_cmdParser->reply.WriteValue(jni, JDWP_TAG_OBJECT, resValue);
        return;
    }

    jobject ovalue = 0;
    JVMTI_TRACE(err, GetJvmtiEnv()->GetLocalObject(thread, frame, 0, &ovalue));
    CheckErr(err);
    JDWP_ASSERT(ovalue != 0);

    jdwpTag tag = GetClassManager().GetJdwpTag(jni, ovalue);
    resValue.l = ovalue;

    JDWP_TRACE_DATA("ThisObject: send: tag=" << tag 
        << ", object=" << resValue.l);
    m_cmdParser->reply.WriteValue(jni, tag, resValue);
}

void
StackFrame::PopFramesHandler::Execute(JNIEnv *jni) throw(AgentException)
{
    jdwpCapabilities caps = GetCapabilities();
    if (caps.canPopFrames != 1) {
        throw AgentException(JDWP_ERROR_NOT_IMPLEMENTED);
    }

    jthread thread = m_cmdParser->command.ReadThreadID(jni);
    if (thread == 0) {
        throw AgentException(JDWP_ERROR_INVALID_THREAD);
    }

    jint frame = m_cmdParser->command.ReadFrameID(jni);
    jint framesToPop = frame + 1;

    JDWP_TRACE_DATA("PopFrames: received: threadID=" << thread 
        << ", framesToPop=" << framesToPop);

    GetThreadManager().PerformPopFrames(jni, framesToPop, thread);
}
