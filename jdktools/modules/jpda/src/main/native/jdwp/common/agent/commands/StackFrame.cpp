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
#include "StackFrame.h"
#include "PacketParser.h"
#include "ClassManager.h"
#include "ThreadManager.h"
#include "ExceptionManager.h"

using namespace jdwp;
using namespace StackFrame;

int
StackFrame::GetValuesHandler::Execute(JNIEnv *jni) 
{
    jthread thread = m_cmdParser->command.ReadThreadID(jni);
    jint frame = m_cmdParser->command.ReadFrameID(jni);
    jint slots = m_cmdParser->command.ReadInt();

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: received: frameID=%d, threadID=%p, slots=%d", frame, thread, slots));

    if (thread == 0) {
        AgentException e(JDWP_ERROR_INVALID_THREAD);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_THREAD;
    }
    if (slots < 0) {
        AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_ILLEGAL_ARGUMENT;
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
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLocalInt(thread, frame, slot, &ivalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                switch (sigbyte) {
                    case JDWP_TAG_BOOLEAN:
                        resValue.z = static_cast<jboolean>(ivalue);
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: slot#=%d, value(boolean)%d", i, resValue.z));
                        break;
                    case JDWP_TAG_BYTE:
                        resValue.b = static_cast<jbyte>(ivalue);
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: slot#=%d, value(byte)%d", i, resValue.b));
                        break;
                    case JDWP_TAG_CHAR:
                        resValue.c = static_cast<jchar>(ivalue);
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: slot#=%d, value(char)%d", i, resValue.c));
                        break;
                    case JDWP_TAG_SHORT:
                        resValue.s = static_cast<jshort>(ivalue);
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: slot#=%d, value(short)%d", i, resValue.s));
                        break;
                    case JDWP_TAG_INT:
                        resValue.i = ivalue;
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: slot#=%d, value(int)%d", i, resValue.i));
                        break;
                }
                m_cmdParser->reply.WriteValue(jni, sigbyte, resValue);
                break;
            case JDWP_TAG_LONG:
                jlong lvalue;
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLocalLong(thread, frame, slot, &lvalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                resValue.j = lvalue;
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: slot#=%d, value(long)%lld", i, resValue.j));
                m_cmdParser->reply.WriteValue(jni, sigbyte, resValue);
                break;
            case JDWP_TAG_FLOAT:
                jfloat fvalue;
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLocalFloat(thread, frame, slot, &fvalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                resValue.f = fvalue;
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: slot#=%d, value(float)%f", i, resValue.f));
                m_cmdParser->reply.WriteValue(jni, sigbyte, resValue);
                break;
            case JDWP_TAG_DOUBLE:
                jdouble dvalue;
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLocalDouble(thread, frame, slot, &dvalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                resValue.d = dvalue;
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: slot#=%d, value(double)%Lf", i, resValue.d));
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
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLocalObject(thread, frame, slot, &ovalue));
                if (err != JVMTI_ERROR_NONE) {
                    break;
                }
                jdwpTag tag = GetClassManager().GetJdwpTag(jni, ovalue);
                if ((sigbyte != JDWP_TAG_OBJECT) && (sigbyte != tag) && ovalue != 0) {
                    AgentException e(JDWP_ERROR_INVALID_TAG);
					JDWP_SET_EXCEPTION(e);
                    return JDWP_ERROR_INVALID_TAG;
                }
                resValue.l = ovalue;
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: slot#=%d, tag=%d, value(object)=%p", i, tag, resValue.l));
                m_cmdParser->reply.WriteValue(jni, tag, resValue);
		jni->DeleteLocalRef(ovalue);
                break;
            }
            default:
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "GetValues: bad slot tag: slot#=%d, tag=%d", i, sigbyte));
                AgentException e(JDWP_ERROR_INVALID_TAG);
				JDWP_SET_EXCEPTION(e);
                return JDWP_ERROR_INVALID_TAG;
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
            AgentException e(error);
		    JDWP_SET_EXCEPTION(e);
            return error;
        }
    }

    return JDWP_ERROR_NONE;
}

int
StackFrame::SetValuesHandler::Execute(JNIEnv *jni) 
{
    jthread thread = m_cmdParser->command.ReadThreadID(jni);
    jint frame = m_cmdParser->command.ReadFrameID(jni);
    jint slotValues = m_cmdParser->command.ReadInt();

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: received: frameID=%d, threadId=%p, slots=%d", frame, thread, slotValues));

    if (thread == 0) {
        AgentException e(JDWP_ERROR_INVALID_THREAD);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_THREAD;
    }
    if (slotValues < 0) {
        AgentException e(JDWP_ERROR_ILLEGAL_ARGUMENT);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_ILLEGAL_ARGUMENT;
    }

    for (int i = 0; i < slotValues; i++) {
        jint slot = m_cmdParser->command.ReadInt();
        jdwpTaggedValue taggedValue = m_cmdParser->command.ReadValue(jni);
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#%d: taggedValue=%d", i, taggedValue.tag));
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
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#=%d, value=(boolean)%d", i, taggedValue.value.z));
                        break;
                    case JDWP_TAG_BYTE:
                        ivalue = static_cast<jint>(taggedValue.value.b);
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#=%d, value=(byte)%d", i, taggedValue.value.b));
                        break;
                    case JDWP_TAG_CHAR:
                        ivalue = static_cast<jint>(taggedValue.value.c);
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#=%d, value=(char)%d", i, taggedValue.value.c));
                        break;
                    case JDWP_TAG_SHORT:
                        ivalue = static_cast<jint>(taggedValue.value.s);
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#=%d, value=(short)%d", i, taggedValue.value.s));
                        break;
                    case JDWP_TAG_INT:
                        ivalue = taggedValue.value.i;
                        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#=%d, value=(int)%d", i, taggedValue.value.i));
                        break;
                }
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetLocalInt(thread, frame, slot, ivalue));
                break;
            case JDWP_TAG_LONG: {
                jlong lvalue = taggedValue.value.j;
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#=%d, value=(long)%lld", i, taggedValue.value.j));
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetLocalLong(thread, frame, slot, lvalue));
                break;
            }
            case JDWP_TAG_FLOAT: {
                jfloat fvalue = taggedValue.value.f;
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#=%d, value=(float)%f", i, taggedValue.value.f));
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetLocalFloat(thread, frame, slot, fvalue));
                break;
            }
            case JDWP_TAG_DOUBLE: {
                jdouble dvalue = taggedValue.value.d;
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#=%d, value=(double)%Lf", i, taggedValue.value.d));
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetLocalDouble(thread, frame, slot, dvalue));
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
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: slot#=%d, value=(object)%p", i, taggedValue.value.l));
                JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->SetLocalObject(thread, frame, slot, ovalue));
                break;
            }
            default:
                JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "SetValues: bad value tag: slot#=%d, tag=%d", i, taggedValue.tag));
                AgentException e(JDWP_ERROR_INVALID_TAG);
				JDWP_SET_EXCEPTION(e);
                return JDWP_ERROR_INVALID_TAG;
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
            AgentException e(error);
		    JDWP_SET_EXCEPTION(e);
            return error;
        }
    }

    return JDWP_ERROR_NONE;
}

int
StackFrame::ThisObjectHandler::CheckErr(jvmtiError err) 
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
        AgentException e(error);
		JDWP_SET_EXCEPTION(e);
        return error;
    }
    return JDWP_ERROR_NONE;
}

int
StackFrame::ThisObjectHandler::Execute(JNIEnv *jni) 
{
    jthread thread = m_cmdParser->command.ReadThreadID(jni);
    
    if (thread == 0) {
        AgentException e(JDWP_ERROR_INVALID_THREAD);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_THREAD;
    }
    if (!GetThreadManager().IsSuspended(thread)){
        AgentException e(JVMTI_ERROR_THREAD_NOT_SUSPENDED);
	    JDWP_SET_EXCEPTION(e);
        return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
    }

    jint frame = m_cmdParser->command.ReadFrameID(jni); // frame == depth
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ThisObject: received: threadID=%p, frameID=%d", thread, frame));

    jvmtiError err;
    int ret;
    jint allCount;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetFrameCount(thread, &allCount));
    ret = CheckErr(err);
    JDWP_CHECK_RETURN(ret);
    JDWP_ASSERT(allCount > 0);

    jvmtiFrameInfo* frames = static_cast<jvmtiFrameInfo*>
        (GetMemoryManager().Allocate(allCount * sizeof(jvmtiFrameInfo) JDWP_FILE_LINE));
    AgentAutoFree af(frames JDWP_FILE_LINE);

    jint count;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetStackTrace(thread, 0, allCount, frames, &count));
    ret = CheckErr(err);
    JDWP_CHECK_RETURN(ret);
    JDWP_ASSERT(count <= allCount);
    JDWP_ASSERT(frame <= count);

    jvmtiFrameInfo& frameInfo = frames[frame];

    jint modifiers;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetMethodModifiers(frameInfo.method, &modifiers));
    ret = CheckErr(err);
    JDWP_CHECK_RETURN(ret);

    jvalue resValue;
    if ((modifiers & (ACC_STATIC | ACC_NATIVE)) != 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ThisObject: null this for method: modifiers=%x, static=%x, native=%x",
                        modifiers, (modifiers & ACC_STATIC), (modifiers & ACC_NATIVE)));
        resValue.l = 0;
        m_cmdParser->reply.WriteValue(jni, JDWP_TAG_OBJECT, resValue);
        return JDWP_ERROR_NONE;
    }

    jobject ovalue = 0;
    JVMTI_TRACE(LOG_DEBUG, err, GetJvmtiEnv()->GetLocalObject(thread, frame, 0, &ovalue));
    ret = CheckErr(err);
    JDWP_CHECK_RETURN(ret);
    JDWP_ASSERT(ovalue != 0);

    jdwpTag tag = GetClassManager().GetJdwpTag(jni, ovalue);
    resValue.l = ovalue;

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ThisObject: send: tag=%d, object=%p", tag, resValue.l));
    m_cmdParser->reply.WriteValue(jni, tag, resValue);

    return JDWP_ERROR_NONE;
}

int
StackFrame::PopFramesHandler::Execute(JNIEnv *jni) 
{
    jdwpCapabilities caps = GetCapabilities();
    if (caps.canPopFrames != 1) {
        AgentException e(JDWP_ERROR_NOT_IMPLEMENTED);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_NOT_IMPLEMENTED;
    }

    jthread thread = m_cmdParser->command.ReadThreadID(jni);
    if (thread == 0) {
        AgentException e(JDWP_ERROR_INVALID_THREAD);
		JDWP_SET_EXCEPTION(e);
        return JDWP_ERROR_INVALID_THREAD;
    }

    jint frame = m_cmdParser->command.ReadFrameID(jni);
    jint framesToPop = frame + 1;

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "PopFrames: received: threadID=%p, framesToPop=%d", thread, framesToPop));

    int ret = GetThreadManager().PerformPopFrames(jni, framesToPop, thread);
    JDWP_CHECK_RETURN(ret);

    return JDWP_ERROR_NONE;
}
