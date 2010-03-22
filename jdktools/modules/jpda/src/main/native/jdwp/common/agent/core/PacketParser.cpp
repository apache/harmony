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
#include "PacketParser.h"
#include "jdwpTypes.h"
#include "hycomp.h"
#include "MemoryManager.h"
#include "ObjectManager.h"
#include "TransportManager.h"
#include "ClassManager.h"
#include "ExceptionManager.h"

#include <string.h>

// Defined to parse bytes order for x86 platform
#ifdef HY_LITTLE_ENDIAN
#define IS_LITTLE_ENDIAN_PLATFORM 1
#else
#define IS_LITTLE_ENDIAN_PLATFORM 0
#endif

using namespace jdwp;

// Constant defining increment for m_registeredObjectIDTable
const int REGISTERED_OBJECTID_TABLE_STEP = 0x10;

const size_t ALLOCATION_STEP = 0x10;
const jbyte PACKET_IS_UNINITIALIZED = 0x03;

// Constant defining initial value for ReferenceTypeID to be different from  ObjectID values
extern const ReferenceTypeID REFTYPEID_MINIMUM;

// GCList
PacketWrapper::GCList::GCList() :
    m_memoryRefAllocatedSize(0),
    m_memoryRef(0),
    m_memoryRefPosition(0),
    m_globalRefAllocatedSize(0),
    m_globalRef(0),
    m_globalRefPosition(0)
{
}

void PacketWrapper::GCList::Reset(JNIEnv *jni) {
    if (m_memoryRef != 0) {
        while (m_memoryRefPosition-- > 0) {
            GetMemoryManager().Free(m_memoryRef[m_memoryRefPosition] JDWP_FILE_LINE);
        }
        m_memoryRefPosition = 0;
    }

    if (m_globalRef != 0) {
        while (m_globalRefPosition-- > 0) {
            jni->DeleteGlobalRef(m_globalRef[m_globalRefPosition]);
        }
        m_globalRefPosition = 0;
    }
}

void PacketWrapper::GCList::ReleaseData() {
    Reset(AgentBase::GetJniEnv());
    if (m_memoryRef != 0) {
        GetMemoryManager().Free(m_memoryRef JDWP_FILE_LINE);
        m_memoryRef = 0;
        m_memoryRefAllocatedSize = 0;
    }

    if (m_globalRef != 0) {
        GetMemoryManager().Free(m_globalRef JDWP_FILE_LINE);
        m_globalRef = 0;
        m_globalRefAllocatedSize = 0;
    }
}

void PacketWrapper::GCList::StoreStringRef(char* ref){
    if (m_memoryRefPosition >= m_memoryRefAllocatedSize) {
        // then reallocate buffer
        size_t oldAllocatedSize = this->m_memoryRefAllocatedSize;
        if (m_memoryRefAllocatedSize<ALLOCATION_STEP)
            m_memoryRefAllocatedSize += ALLOCATION_STEP;
        else
            m_memoryRefAllocatedSize *= 2;

        m_memoryRef = static_cast<char**>
            (GetMemoryManager().Reallocate(m_memoryRef,
                 oldAllocatedSize * sizeof(char*),
                 m_memoryRefAllocatedSize * sizeof(char*) JDWP_FILE_LINE));
    }
    m_memoryRef[m_memoryRefPosition++] = ref;
}

void PacketWrapper::GCList::StoreGlobalRef(jobject globalRef){
    if (m_globalRefPosition >= m_globalRefAllocatedSize) {
        // then reallocate buffer
        size_t oldAllocatedSize = m_globalRefAllocatedSize;
        if (m_globalRefAllocatedSize < ALLOCATION_STEP) {
            m_globalRefAllocatedSize += ALLOCATION_STEP;
        } else {
            m_globalRefAllocatedSize *= 2;
        }

        m_globalRef = static_cast<jobject*>
            (GetMemoryManager().Reallocate(m_globalRef,
                 oldAllocatedSize * sizeof(jobject),
                 m_globalRefAllocatedSize * sizeof(jobject) JDWP_FILE_LINE));
    }
    m_globalRef[m_globalRefPosition++] = globalRef;
}

void PacketWrapper::GCList::MoveData(GCList* to) {
    to->m_memoryRefAllocatedSize = m_memoryRefAllocatedSize;
    to->m_memoryRef = m_memoryRef;
    to->m_memoryRefPosition = m_memoryRefPosition;

    m_memoryRefAllocatedSize = 0;
    m_memoryRef = 0;
    m_memoryRefPosition = 0;

    to->m_globalRefAllocatedSize = m_globalRefAllocatedSize;
    to->m_globalRef = m_globalRef;
    to->m_globalRefPosition = m_globalRefPosition;

    m_globalRefAllocatedSize = 0;
    m_globalRef = 0;
    m_globalRefPosition = 0;
}

//////////////////////////////////////////////////////////////////////
// PacketWrapper
//////////////////////////////////////////////////////////////////////

PacketWrapper::PacketWrapper() :
    m_packet(),
    m_garbageList()
{
    this->m_packet.type.cmd.flags = PACKET_IS_UNINITIALIZED;
}

void PacketWrapper::Reset(JNIEnv *jni) {
    // clear links
    m_garbageList.Reset(jni);

    // free packet's data
    if (m_packet.type.cmd.data!=0) {
        GetMemoryManager().Free(m_packet.type.cmd.data JDWP_FILE_LINE);
        m_packet.type.cmd.data = 0;
    }

    m_packet.type.cmd.flags = PACKET_IS_UNINITIALIZED;
}

void PacketWrapper::ReleaseData() {
    m_garbageList.ReleaseData();
    // free packet's data
    if (m_packet.type.cmd.data!=0) {
        GetMemoryManager().Free(m_packet.type.cmd.data JDWP_FILE_LINE);
        m_packet.type.cmd.data = 0;
    }

    m_packet.type.cmd.flags = PACKET_IS_UNINITIALIZED;
}

bool PacketWrapper::IsPacketInitialized() {
    return ((m_packet.type.cmd.flags & PACKET_IS_UNINITIALIZED) == 0);
}

void PacketWrapper::MoveData(JNIEnv *jni, PacketWrapper* to) {
    to->Reset(jni);

    // move m_garbageList data to other PacketWrapper
    to->m_garbageList.MoveData(&to->m_garbageList);

    // move m_packet to packetWrapper
    memcpy(&to->m_packet, &m_packet, sizeof(m_packet));
    m_packet.type.cmd.data = 0;
    m_packet.type.cmd.len = 0;
    m_packet.type.cmd.flags = PACKET_IS_UNINITIALIZED;
}


//////////////////////////////////////////////////////////////////////
// InputPacketParser - sequential reading of m_packet data
//////////////////////////////////////////////////////////////////////

int InputPacketParser::ReadPacketFromTransport() {
    JDWP_ASSERT(!IsPacketInitialized());
    return GetTransportManager().Read(&m_packet);
}

void InputPacketParser::ReadBigEndianData(void* data, int len) {
    JDWP_ASSERT(IsPacketInitialized());

    if (m_position+len>m_packet.type.cmd.len-JDWP_MIN_PACKET_LENGTH) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Error reading data - attempting to read past end of packet"));
        return;
    }
  
    #if IS_LITTLE_ENDIAN_PLATFORM
    jbyte* from = static_cast<jbyte*>(&m_packet.type.cmd.data[m_position]);
    jbyte* to = static_cast<jbyte*>(data);
    for (int i=0; i<len; i++) {
        to[i] = from[len-i-1];
    }    
    #else
    memcpy(data, &m_packet.type.cmd.data[m_position], len);
    #endif
    m_position += len;
}

jbyte InputPacketParser::ReadByte() {
    jbyte data = 0;
    ReadBigEndianData(&data, sizeof(jbyte));
    return data;
}

jboolean InputPacketParser::ReadBoolean() {
    jboolean data = 0;
    ReadBigEndianData(&data, sizeof(jboolean));
    return data;
}

jint InputPacketParser::ReadInt() {
    jint res = 0;
    ReadBigEndianData(&res, sizeof(jint));
    return res;
}

jlong InputPacketParser::ReadLong() {
    jlong data = 0;
    ReadBigEndianData(&data, sizeof(jlong));
    return data;
}

ObjectID InputPacketParser::ReadRawObjectID() {
    ObjectID data = 0;
    ReadBigEndianData(&data, OBJECT_ID_SIZE);
    return data;
}

jobject InputPacketParser::ReadObjectIDOrNull(JNIEnv *jni) {
    // read raw ObjectID and check for null
    ObjectID oid = ReadRawObjectID();
    if (oid == 0) {
        return 0;
    }

    // convert to jobject (actually to WeakReference or GlobalReference)
    jobject obj = GetObjectManager().MapFromObjectID(jni, oid);
    if (NULL == obj) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "MapFromObjectID returned NULL"));
        AgentException ex(JDWP_ERROR_INVALID_OBJECT);
        JDWP_SET_EXCEPTION(ex);
        return 0;
    }

    // make GlobalReference and check if WeakReference was freed
    jobject ref = jni->NewGlobalRef(obj);
    if (ref == 0) {
        if (jni->IsSameObject(obj, 0)) {
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Invalid object calling NewGlobalRef"));
            AgentException ex(JDWP_ERROR_INVALID_OBJECT);
            JDWP_SET_EXCEPTION(ex);
            return 0;
        } else {
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Out of memory calling NewGlobalRef"));
            AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
            JDWP_SET_EXCEPTION(ex);
            return 0;
        }
    }

    // store GlobalReference for futher disposal
    m_garbageList.StoreGlobalRef(ref);
    return ref;
}

jobject InputPacketParser::ReadObjectID(JNIEnv *jni) {
    jobject obj = ReadObjectIDOrNull(jni);

    // if the ID was invalid we already have an exception so we just
    // need to check the case when it is null
    if (!obj && !JDWP_HAS_EXCEPTION) {
      JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "ReadObjectID returned null"));
      AgentException ex(JDWP_ERROR_INVALID_OBJECT);
      JDWP_SET_EXCEPTION(ex);
    }
    return obj;
}

jclass InputPacketParser::ReadReferenceTypeIDOrNull(JNIEnv *jni) {
    ReferenceTypeID rtid = 0;
    ReadBigEndianData(&rtid, REFERENCE_TYPE_ID_SIZE);
	JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReadReferenceTypeIDOrNul: read : ReferenceTypeID=%p", rtid));

    if (rtid == 0) {
        return 0;
    }

    jclass cls = 0;
    if(rtid < REFTYPEID_MINIMUM) {
        // For a ObjectID, we should convert it to ReferenceTypeID 
        // if it is a ClassObjectID
        jobject obj = GetObjectManager().MapFromObjectID(jni, rtid);
        if (obj) {
          obj = jni->NewLocalRef(obj);
          if (!obj) {
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Out of memory calling NewLocalRef"));
            AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
            JDWP_SET_EXCEPTION(ex);
            return 0;
          }
          jclass clsType = jni->GetObjectClass(obj);
          jboolean isClass = jni->IsAssignableFrom(clsType, jni->GetObjectClass(clsType)); 
          if (isClass) {
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "## ReadReferenceTypeIDOrNul: read : ObjectID is a ClassObjectID"));
            cls = static_cast<jclass> (obj);
            jni->DeleteLocalRef(obj);
            
            jboolean isValidID = GetObjectManager().FindObjectID(jni, cls, rtid); 
            if(!isValidID) {
              JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "## ReadReferenceTypeIDOrNul: read : ID is an invalid ObjectID"));
              AgentException ex(JDWP_ERROR_INVALID_OBJECT);
              JDWP_SET_EXCEPTION(ex);
              return 0;
            }
          } else {
            jni->DeleteLocalRef(obj);
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "## ReadReferenceTypeIDOrNul: read : ID is not a ClassObjectID"));
            AgentException ex(JDWP_ERROR_INVALID_CLASS);
            JDWP_SET_EXCEPTION(ex);
            return 0;
          }
        }
    }

    if (cls == 0) {
        // convert to jclass (actually to WeakReference or GlobalReference)
        cls = GetObjectManager().MapFromReferenceTypeID(jni, rtid);
    }

    if (!cls) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "## ReadReferenceTypeIDOrNul: read : ID is invalid"));
        AgentException ex(JDWP_ERROR_INVALID_OBJECT);
        JDWP_SET_EXCEPTION(ex);
        return 0;
    }
    
    // make GlobalReference and check if WeakReference was freed
    jclass ref = static_cast<jclass>(jni->NewGlobalRef(cls));
    if (ref == 0) {
        if (jni->IsSameObject(cls, 0)) {
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Invalid object calling NewGlobalRef"));
            AgentException ex(JDWP_ERROR_INVALID_OBJECT);
            JDWP_SET_EXCEPTION(ex);
            return 0;
        } else {
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Out of memory calling NewGlobalRef"));
            AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
            JDWP_SET_EXCEPTION(ex);
            return 0;
        }
    }

    // store GlobalReference for futher disposal
    m_garbageList.StoreGlobalRef(ref);
    return ref;
}

jclass InputPacketParser::ReadReferenceTypeID(JNIEnv *jni) {
    jclass cls = ReadReferenceTypeIDOrNull(jni);

    // if the ID was invalid we already have an exception so we just
    // need to check the case when it is null
    if (!cls && !JDWP_HAS_EXCEPTION) {
      JDWP_TRACE(LOG_RELEASE,
                 (LOG_ERROR_FL, "ReadReferenceTypeID returned null"));
      AgentException ex(JDWP_ERROR_INVALID_OBJECT);
      JDWP_SET_EXCEPTION(ex);
    }
    return cls;
}

jfieldID InputPacketParser::ReadFieldID(JNIEnv *jni) {
    FieldID fid = 0;
    ReadBigEndianData(&fid, FIELD_ID_SIZE);
    jfieldID res = GetObjectManager().MapFromFieldID(jni, fid);
    return res;
}

jmethodID InputPacketParser::ReadMethodID(JNIEnv *jni) {
    MethodID mid = 0;
    ReadBigEndianData(&mid, METHOD_ID_SIZE);
    jmethodID res = GetObjectManager().MapFromMethodID(jni, mid);
    return res;
}

jint InputPacketParser::ReadFrameID(JNIEnv *jni) {
    FrameID fid;
    ReadBigEndianData(&fid, FRAME_ID_SIZE);
    jint res = GetObjectManager().MapFromFrameID(jni, fid);
    return res;
}

jdwpLocation InputPacketParser::ReadLocation(JNIEnv *jni) {
    jdwpLocation res;
    res.typeTag = static_cast<jdwpTypeTag>(ReadByte());
    res.classID = ReadReferenceTypeID(jni);
    res.methodID = ReadMethodID(jni);
    res.loc = ReadLong();
    return res;
}

jthread InputPacketParser::ReadThreadIDOrNull(JNIEnv *jni) {
    // read raw ThreadID and check for null
    ObjectID oid = ReadRawObjectID();
    if (oid == 0) {
        AgentException ex(JDWP_ERROR_INVALID_THREAD);
        JDWP_SET_EXCEPTION(ex);
        return 0;
    }
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReadThreadIDOrNull: read : ThreadID=%lld", oid));
    
    // if oid is a reference type, throw INVALID_THREAD exception
    // Bug fix, INVALID_THREAD error reply is required
    if( GetObjectManager().IsValidReferenceTypeID(jni, oid) ) {
        AgentException ex(JDWP_ERROR_INVALID_THREAD);
        JDWP_SET_EXCEPTION(ex);
        return 0;
    }    

    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReadObjectIDOrNull: read : ObjectID=%lld", oid));
    
    // convert to jobject (actually to WeakReference or GlobalReference)
    jobject obj = GetObjectManager().MapFromObjectID(jni, oid);
    JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "ReadObjectIDOrNull: read : jobject=%p", obj));
    JDWP_ASSERT(obj !=  NULL);

    // make GlobalReference and check if WeakReference was freed
    jobject ref = jni->NewGlobalRef(obj);
    if (ref == 0) {
        if (jni->IsSameObject(obj, 0)) {
            AgentException ex(JDWP_ERROR_INVALID_OBJECT);
            JDWP_SET_EXCEPTION(ex);
            return 0;
        } else {
            AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
            JDWP_SET_EXCEPTION(ex);
            return 0;
        }
    }
 
    jthread thrd = static_cast<jthread>(ref);
    // store GlobalReference for futher disposal
    m_garbageList.StoreGlobalRef(thrd);
    return thrd;
}
    
jthread InputPacketParser::ReadThreadID(JNIEnv *jni) {
    jthread thrd = ReadThreadIDOrNull(jni);

    // if the ID was invalid we already have an exception so we just
    // need to check the case when it is null
    if (!thrd && !JDWP_HAS_EXCEPTION) {
      JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "ReadThreadID returned null"));
      AgentException ex(JDWP_ERROR_INVALID_OBJECT);
      JDWP_SET_EXCEPTION(ex);
    }
    return thrd;
}

jthreadGroup InputPacketParser::ReadThreadGroupID(JNIEnv *jni) {
    return static_cast<jthreadGroup>(ReadObjectID(jni));
}

jstring InputPacketParser::ReadStringID(JNIEnv *jni) {
    return static_cast<jstring>(ReadObjectID(jni));
}

jarray InputPacketParser::ReadArrayID(JNIEnv *jni) {
    return static_cast<jarray>(ReadObjectID(jni));
}

char* InputPacketParser::ReadStringNoFree() {
    jint len = ReadInt();
    if (m_position+len>m_packet.type.cmd.len) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Attempting to read past end of packet"));
        return NULL;
    }
    char* res = static_cast<char*>(GetMemoryManager().Allocate(len+1 JDWP_FILE_LINE));
    memcpy(res, reinterpret_cast<char*>(&m_packet.type.cmd.data[m_position]), len);
    res[len] = '\0';
    m_position += len;
    return res;
}

char* InputPacketParser::ReadString() {
    char* res = ReadStringNoFree();
    m_garbageList.StoreStringRef(res);
    return res;
}

jdwpTaggedValue InputPacketParser::ReadValue(JNIEnv *jni)  {
    //The first byte is a signature byte which is used to identify the type
    jdwpTaggedValue tv;
    tv.tag = static_cast<jdwpTag>(ReadByte());
    tv.value = ReadUntaggedValue(jni, tv.tag);
    return tv;
}

jvalue InputPacketParser::ReadUntaggedValue(JNIEnv *jni, jdwpTag tagPtr)  {
    jvalue value;

    switch (tagPtr) {
    case JDWP_TAG_ARRAY:
        value.l = ReadObjectIDOrNull(jni);
        break;
    case JDWP_TAG_BYTE:
        value.b = ReadByte();
        break;
    case JDWP_TAG_CHAR:
        value.c = ReadChar();
        break;
    case JDWP_TAG_OBJECT:
        value.l = ReadObjectIDOrNull(jni);
        break;
    case JDWP_TAG_FLOAT:
        value.f = ReadFloat();
        break;
    case JDWP_TAG_DOUBLE:
        value.d = ReadDouble();
        break;
    case JDWP_TAG_INT:
        value.i = ReadInt();
        break;
    case JDWP_TAG_LONG:
        value.j = ReadLong();
        break;
    case JDWP_TAG_SHORT:
        value.s = ReadShort();
        break;
    case JDWP_TAG_VOID:
        // read nothing
        break;
    case JDWP_TAG_BOOLEAN:
        value.z = ReadBoolean();
        break;
    case JDWP_TAG_STRING:
        value.l = ReadObjectIDOrNull(jni);
        break;
    case JDWP_TAG_THREAD:
        value.l = ReadObjectIDOrNull(jni);
        break;
    case JDWP_TAG_THREAD_GROUP:
        value.l = ReadObjectIDOrNull(jni);
        break;
    case JDWP_TAG_CLASS_LOADER:
        value.l = ReadObjectIDOrNull(jni);
        break;
    case JDWP_TAG_CLASS_OBJECT:
        value.l = ReadObjectIDOrNull(jni);
        break;
    default: JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Illegal jdwp-tag value: %d", tagPtr));
    }
    return value;
}

// InputPacket's private methods

jchar InputPacketParser::ReadChar() {
    jchar data = 0;
    ReadBigEndianData(&data, sizeof(jchar));
    return data;
}
jshort InputPacketParser::ReadShort() {
    jshort data = 0;
    ReadBigEndianData(&data, sizeof(jshort));
    return data;
}

jfloat InputPacketParser::ReadFloat() {
    jfloat data = 0;
    ReadBigEndianData(&data, sizeof(jfloat));
    return data;
}
jdouble InputPacketParser::ReadDouble() {
    jdouble data = 0;
    ReadBigEndianData(&data, sizeof(jdouble));
    return data;
}

void InputPacketParser::Reset(JNIEnv *jni) {
    PacketWrapper::Reset(jni);
    m_position = 0;
}

void InputPacketParser::ReleaseData() {
    PacketWrapper::ReleaseData();
    m_position = 0;
}

void InputPacketParser::MoveData(JNIEnv *jni, InputPacketParser* to) {
    PacketWrapper::MoveData(jni, to);
    m_position = 0;
}

//////////////////////////////////////////////////////////////////////
// OutputPacketComposer - sequential writing of m_packet data
//////////////////////////////////////////////////////////////////////
int OutputPacketComposer::WritePacketToTransport() {
    JDWP_ASSERT(IsPacketInitialized());
    int ret = GetTransportManager().Write(&m_packet);
    JDWP_CHECK_RETURN(ret);

    if (GetError() == JDWP_ERROR_NONE) {
       IncreaseObjectIDRefCounts();
    }

    return JDWP_ERROR_NONE;
}

void OutputPacketComposer::AllocateMemoryForData(int length){
    size_t newPosition = m_position + static_cast<size_t>(length);
    if (newPosition >= m_allocatedSize) {
        // then reallocate buffer
        size_t newAllocatedSize = m_allocatedSize + ALLOCATION_STEP;
        while (newPosition >= newAllocatedSize) {
            if (newAllocatedSize < ALLOCATION_STEP)
                newAllocatedSize += ALLOCATION_STEP;
            else
                newAllocatedSize *= 2;
        }
        m_packet.type.cmd.data = static_cast<jbyte*>
            (GetMemoryManager().Reallocate(m_packet.type.cmd.data, 
                m_allocatedSize, newAllocatedSize JDWP_FILE_LINE));
        m_allocatedSize = newAllocatedSize;
    }
}

size_t OutputPacketComposer::GetPosition() {
    return m_position;
}

void OutputPacketComposer::SetPosition(size_t newPosition) {
    m_position = newPosition;
}

void OutputPacketComposer::WriteRawData(const void* data, int length){
    AllocateMemoryForData(length);
    
    memcpy(&m_packet.type.cmd.data[m_position], data, length);
    
    m_position += length;
    m_packet.type.cmd.len += length;
}


void OutputPacketComposer::WriteBigEndianData(void* data, int length){
    JDWP_ASSERT(length <= sizeof(jlong));
    AllocateMemoryForData(length);
    
    #if IS_LITTLE_ENDIAN_PLATFORM
        const jbyte* from = static_cast<jbyte*>(const_cast<void*>(data));
        jbyte* to = static_cast<jbyte*>(&m_packet.type.cmd.data[m_position]);
        for (int i=0; i<length; i++) {
            to[i] = from[length-i-1];
        }
    
    #else 
        memcpy(&m_packet.type.cmd.data[m_position], data, length);
    #endif
    m_position += length;
    m_packet.type.cmd.len += length;
     
}

void OutputPacketComposer::WriteByte(jbyte value){
    WriteBigEndianData(&value, sizeof(value));
}
void OutputPacketComposer::WriteBoolean(jboolean value){
    WriteBigEndianData(&value, sizeof(value));
}
void OutputPacketComposer::WriteInt(jint value){
    WriteBigEndianData(&value, sizeof(value));
}
void OutputPacketComposer::WriteLong(jlong value){
    WriteBigEndianData(&value, sizeof(value));
}

void OutputPacketComposer::WriteObjectID(JNIEnv *jni, jobject value) {
    ObjectID id = GetObjectManager().MapToObjectID(jni, value);
    WriteBigEndianData(&id, OBJECT_ID_SIZE);
    RegisterObjectID(id);
}

void OutputPacketComposer::WriteReferenceTypeID(JNIEnv *jni, jclass value){
    ReferenceTypeID id = GetObjectManager().MapToReferenceTypeID(jni, value);
    WriteBigEndianData(&id, REFERENCE_TYPE_ID_SIZE);
}

void OutputPacketComposer::WriteFieldID(JNIEnv *jni, jfieldID value){
    FieldID id = GetObjectManager().MapToFieldID(jni, value);
    WriteBigEndianData(&id, FIELD_ID_SIZE);
}

void OutputPacketComposer::WriteMethodID(JNIEnv *jni, jmethodID value){
    MethodID id = GetObjectManager().MapToMethodID(jni, value);
    WriteBigEndianData(&id, METHOD_ID_SIZE);
}

void OutputPacketComposer::WriteFrameID(JNIEnv *jni, jthread jvmThread, jint frameDepth, jint framesCount) {
    FrameID id = GetObjectManager().MapToFrameID(jni, jvmThread, frameDepth, framesCount);
    WriteBigEndianData(&id, FRAME_ID_SIZE);
}

void OutputPacketComposer::WriteLocation(JNIEnv *jni, jdwpTypeTag typeTag, jclass clazz, 
                                         jmethodID method, jlocation location) {
    WriteByte(static_cast<jbyte>(typeTag));
    WriteReferenceTypeID(jni, clazz);
    WriteMethodID(jni, method);
    WriteLong(static_cast<jlong>(location));
}

void OutputPacketComposer::WriteLocation(JNIEnv *jni, jdwpLocation *location){
    WriteLocation(jni, location->typeTag, location->classID, location->methodID, location->loc);
}

void OutputPacketComposer::WriteThreadID(JNIEnv *jni, jthread value){
    WriteObjectID(jni, value);
}

void OutputPacketComposer::WriteThreadGroupID(JNIEnv *jni, jthreadGroup value){
    WriteObjectID(jni, value);
}

void OutputPacketComposer::WriteStringID(JNIEnv *jni, jstring value){
    WriteObjectID(jni, value);
}

void OutputPacketComposer::WriteArrayID(JNIEnv *jni, jarray value){
    WriteObjectID(jni, value);
}

void OutputPacketComposer::WriteString(const char* value){
    jint len = static_cast<jint>((value == 0) ? 0 : strlen(value));
    WriteString(value, len);
}

void OutputPacketComposer::WriteString(const char* value, jint length){
    WriteBigEndianData(&length, sizeof(jint));
    if (length > 0) {
        WriteRawData(value, length);
    }
}

void OutputPacketComposer::WriteUntaggedValue(JNIEnv *jni, jdwpTag tag, jvalue value) {
    //The first byte is a signature byte which is used to identify the type
    switch (tag) {
    case JDWP_TAG_ARRAY:
        WriteObjectID(jni, value.l);
        break;
    case JDWP_TAG_BYTE:
        WriteByte(value.b);
        break;
    case JDWP_TAG_CHAR:
        WriteChar(value.c);
        break;
    case JDWP_TAG_OBJECT:
        WriteObjectID(jni, value.l);
        break;
    case JDWP_TAG_FLOAT:
        WriteFloat(value.f);
        break;
    case JDWP_TAG_DOUBLE:
        WriteDouble(value.d);
        break;
    case JDWP_TAG_INT:
        WriteInt(value.i);
        break;
    case JDWP_TAG_LONG:
        WriteLong(value.j);
        break;
    case JDWP_TAG_SHORT:
        WriteShort(value.s);
        break;
    case JDWP_TAG_VOID:
        // read nothing
        break;
    case JDWP_TAG_BOOLEAN:
        WriteBoolean(value.z);
        break;
    case JDWP_TAG_STRING:
        WriteObjectID(jni, value.l);
        break;
    case JDWP_TAG_THREAD:
        WriteObjectID(jni, value.l);
        break;
    case JDWP_TAG_THREAD_GROUP:
        WriteObjectID(jni, value.l);
        break;
    case JDWP_TAG_CLASS_LOADER:
        WriteObjectID(jni, value.l);
        break;
    case JDWP_TAG_CLASS_OBJECT:
        WriteObjectID(jni, value.l);
        break;
    default: JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Illegal jdwp-tag value: %d", tag));
    }
}

void OutputPacketComposer::WriteValue(JNIEnv *jni, jdwpTag tag, jvalue value){
    WriteByte(static_cast<jbyte>(tag));
    WriteUntaggedValue(jni, tag, value);
}

void OutputPacketComposer::WriteTaggedObjectID(JNIEnv *jni, jobject object){
    jdwpTag tag = GetClassManager().GetJdwpTag(jni, object);
    WriteByte(static_cast<jbyte>(tag));
    WriteObjectID(jni, object);
}

void OutputPacketComposer::WriteValues(JNIEnv *jni, jdwpTag tag, 
                           jint length, jvalue* value){
    WriteByte(static_cast<jbyte>(tag));
    WriteInt(length);
    for (int i=0; i<length; i++)
        WriteUntaggedValue(jni, tag, value[i]);
}

void OutputPacketComposer::WriteByteArray(jbyte* byte, jint length){
    WriteInt(length);
    WriteRawData(byte, length);
}

void OutputPacketComposer::Reset(JNIEnv *jni) {
    // clear links
    m_garbageList.Reset(jni);
    m_packet.type.cmd.flags = PACKET_IS_UNINITIALIZED;
    m_position = 0;
    if (m_registeredObjectIDCount != 0) {
        m_registeredObjectIDCount = 0;
    }
}

void OutputPacketComposer::ReleaseData() {
    PacketWrapper::ReleaseData();
    m_allocatedSize = 0;

    if (m_registeredObjectIDTable != 0) {
        GetMemoryManager().Free(m_registeredObjectIDTable JDWP_FILE_LINE);
        m_registeredObjectIDTable = 0;
        m_registeredObjectIDCount = 0;
    }
}

void OutputPacketComposer::MoveData(JNIEnv *jni, OutputPacketComposer* to) {
    PacketWrapper::MoveData(jni, to);
    m_position = 0;
    m_allocatedSize = 0;
}

// OutputPacketComposer's private methods
void OutputPacketComposer::WriteChar(jchar value){
    WriteBigEndianData(&value, sizeof(value));
}
void OutputPacketComposer::WriteShort(jshort value){
    WriteBigEndianData(&value, sizeof(value));
}
void OutputPacketComposer::WriteFloat(jfloat value){
    WriteBigEndianData(&value, sizeof(value));
}
void OutputPacketComposer::WriteDouble(jdouble value){
    WriteBigEndianData(&value, sizeof(value));
}

void OutputPacketComposer::RegisterObjectID(ObjectID objectID) {

    if ( objectID == JDWP_OBJECT_ID_NULL ) {
        return;
    }

    if ( m_registeredObjectIDCount == m_registeredObjectIDTableSise ) {

        if ( m_registeredObjectIDCount == 0 ) {
            m_registeredObjectIDTable = reinterpret_cast<ObjectID*>
                (AgentBase::GetMemoryManager().Allocate(sizeof(ObjectID) 
                * REGISTERED_OBJECTID_TABLE_STEP JDWP_FILE_LINE));
            // can be: OutOfMemoryException, InternalErrorException 
            m_registeredObjectIDTableSise = REGISTERED_OBJECTID_TABLE_STEP;
        } else {
            m_registeredObjectIDTableSise
                = m_registeredObjectIDTableSise + REGISTERED_OBJECTID_TABLE_STEP;
            m_registeredObjectIDTable = reinterpret_cast<ObjectID*>
                (AgentBase::GetMemoryManager().Reallocate(m_registeredObjectIDTable,
                static_cast<size_t>(sizeof(ObjectID) * (m_registeredObjectIDTableSise-REGISTERED_OBJECTID_TABLE_STEP)),
                static_cast<size_t>(sizeof(ObjectID) * m_registeredObjectIDTableSise) JDWP_FILE_LINE));
            // can be: OutOfMemoryException, InternalErrorException
        }
    }
    m_registeredObjectIDTable[m_registeredObjectIDCount++] = objectID;
}

void OutputPacketComposer::IncreaseObjectIDRefCounts() {
    for ( int i=0; i < m_registeredObjectIDCount; i++) {
        GetObjectManager().IncreaseIDRefCount(m_registeredObjectIDTable[i]);
    }
}

// new reply
void OutputPacketComposer::CreateJDWPReply(jint id, jdwpError errorCode) {
    JDWP_ASSERT(!IsPacketInitialized());
    m_packet.type.reply.id = id;
    m_packet.type.reply.len = JDWP_MIN_PACKET_LENGTH;
    m_packet.type.reply.flags = JDWP_FLAG_REPLY_PACKET;
    m_packet.type.reply.errorCode = (jshort)errorCode;
}

// new event
void OutputPacketComposer::CreateJDWPEvent(jint id, jdwpCommandSet commandSet, jdwpCommand command) {
    JDWP_ASSERT(!IsPacketInitialized());
    m_packet.type.cmd.id = id;
    m_packet.type.cmd.len = JDWP_MIN_PACKET_LENGTH;
    m_packet.type.cmd.flags = 0;
    m_packet.type.cmd.cmdSet = (jbyte)commandSet;
    m_packet.type.cmd.cmd = (jbyte)command;
}

///////////////////////////////////////////////////////////////////////////////
// CommandParser
///////////////////////////////////////////////////////////////////////////////

int CommandParser::ReadCommand() {
    int ret = command.ReadPacketFromTransport();
    JDWP_CHECK_RETURN(ret);
    reply.CreateJDWPReply(command.GetId(), JDWP_ERROR_NONE);
    return JDWP_ERROR_NONE;
}

int CommandParser::WriteReply(JNIEnv *jni) {
    int ret = reply.WritePacketToTransport();
    JDWP_CHECK_RETURN(ret);
    Reset(jni);
    return JDWP_ERROR_NONE;
}

void CommandParser::Reset(JNIEnv *jni) {
    command.Reset(jni);
    reply.Reset(jni);
}

void CommandParser::MoveData(JNIEnv *jni, CommandParser* to) {
    command.MoveData(jni, &to->command);
    reply.MoveData(jni, &to->reply);
}

CommandParser::~CommandParser() {
    reply.ReleaseData();
    command.ReleaseData();
}

///////////////////////////////////////////////////////////////////////////////
// EventComposer
///////////////////////////////////////////////////////////////////////////////

EventComposer::EventComposer(jint id,
    jdwpCommandSet commandSet, jdwpCommand command, jdwpSuspendPolicy sp)
{
    event.CreateJDWPEvent(id, commandSet, command);
    m_suspendPolicy = sp;
    m_thread = 0;
    m_isSent = false;
    m_isWaiting = false;
    m_isReleased = false;
    m_isAutoDeathEvent = false;
    event.WriteByte((jbyte)sp);
}

EventComposer::~EventComposer() {
    event.ReleaseData();
}

void EventComposer::WriteThread(JNIEnv *jni, jthread thread)
{
    event.WriteThreadID(jni, thread);
    m_thread = jni->NewGlobalRef(thread);
    if (m_thread == 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "Out of memory calling NewGlobalRef"));
    }
}

void EventComposer::Reset(JNIEnv *jni)
{
    if (m_thread != 0) {
        jni->DeleteGlobalRef(m_thread);
        m_thread = 0;
    }
    event.Reset(jni);
}

int EventComposer::WriteEvent(JNIEnv *jni)
{
    int ret = event.WritePacketToTransport();
    JDWP_CHECK_RETURN(ret);
    m_isSent = true;
    event.Reset(jni);
    return JDWP_ERROR_NONE;
}
