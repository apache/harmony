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

// Provide mapping between JDWP IDs and corresponding JVMTI, JNI data types
#include "jni.h"
#include "jvmti.h"
#include "jdwp.h"
#include "jdwpTypes.h"
#include "AgentBase.h"
#include "AgentEnv.h"
#include "MemoryManager.h"
#include "ExceptionManager.h"
#include "Log.h"

#include "ObjectManager.h"

#include <string.h>

using namespace jdwp;

// =============================================================================

inline jvmtiError ObjectManager::GetObjectHashCode(jobject object, jint * hash_code_ptr) {
    return GetJvmtiEnv()->GetObjectHashCode(object, hash_code_ptr);
}

/* Mapping: ObjectID <-> jobject
 * Includes JDWP types: objectID, threadID, threadGroupID, stringID,
 *                      classLoaderID, classObjectID, arrayID
*/

// Constants defining kind of object reference for ObjectID:
// (normal) global reference or weak global reference
const jshort NORMAL_GLOBAL_REF = 1;
const jshort WEAK_GLOBAL_REF = 2;

// Constant defining initial size of m_objectIDTable
const jlong OBJECTID_TABLE_INIT_SIZE = 512; // in ObjectIDItem

// Constant (for 'objectID' field in ObjectIDItem structure) as sign
// that ObjectIDItem describes free item in m_objectIDTable
const ObjectID FREE_OBJECTID_SIGN = -1;
const ObjectID OBJECTID_MINIMUM = 1;

jboolean ObjectManager::FindObjectID(JNIEnv* JNIEnvPtr, jobject jvmObject, ObjectID objectID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "FindObjectID(%p,%p,%lld)", JNIEnvPtr, jvmObject, objectID));

    if (jvmObject == NULL) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "## FindObjectID: find NULL jobject"));
        return false;
    }

    // get object HASH CODE
    jint hashCode = -1;
    if (GetObjectHashCode(jvmObject, &hashCode) != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "## FindObjectID: GetObjectHashCode failed"));
        return false;
    }

    // get HASH INDEX
    size_t idx = size_t(hashCode) & HASH_TABLE_MSK;

     // LOCK objectID table
    MonitorAutoLock objectIDTableLock(m_objectIDTableMonitor JDWP_FILE_LINE);

    // find EXISTING objectID
    ObjectIDItem* objectIDItem = m_objectIDTable[idx];
    ObjectIDItem* objectIDItemEnd = objectIDItem + m_maxAllocatedObjectID[idx];
    while (objectIDItem != objectIDItemEnd) {
        if (objectIDItem->objectID != FREE_OBJECTID_SIGN &&
            JNIEnvPtr->IsSameObject(objectIDItem->mapObjectIDItem.jvmObject, jvmObject) == JNI_TRUE &&
            objectID == objectIDItem->objectID) {
            JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "FindObjectID: find object, it is a valid object id"));
            return true;
        }
        objectIDItem++;
    }
    return false;
}


ObjectID ObjectManager::MapToObjectID(JNIEnv* JNIEnvPtr, jobject jvmObject) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapToObjectID(%p,%p)", JNIEnvPtr, jvmObject));

    if (jvmObject == NULL) {
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapToObjectID: map NULL jobject"));
        return JDWP_OBJECT_ID_NULL;
    }

    // get object HASH CODE
    jint hashCode = -1;
    if (GetObjectHashCode(jvmObject, &hashCode) != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapToObjectID: GetObjectHashCode failed"));
        return JDWP_OBJECT_ID_NULL;
    }

    // get HASH INDEX
    size_t idx = size_t(hashCode) & HASH_TABLE_MSK;

    ObjectID objectID = 0;

    { // LOCK objectID table
    MonitorAutoLock objectIDTableLock(m_objectIDTableMonitor JDWP_FILE_LINE);

    // find EXISTING objectID
    ObjectIDItem* objectIDItem = m_objectIDTable[idx];
    ObjectIDItem* objectIDItemEnd = objectIDItem + m_maxAllocatedObjectID[idx];
    while (objectIDItem != objectIDItemEnd) {
        if (objectIDItem->objectID != FREE_OBJECTID_SIGN &&
            JNIEnvPtr->IsSameObject(objectIDItem->mapObjectIDItem.jvmObject, jvmObject) == JNI_TRUE) {
            objectID = objectIDItem->objectID;
            break;
        }
        objectIDItem++;
    }

    // map NEW objectID if not found existing
    if (objectID == 0) {
        JNIEnvPtr->ExceptionClear();
        jobject newWeakGlobRef = JNIEnvPtr->NewWeakGlobalRef(jvmObject);
        if (newWeakGlobRef == NULL) {
            /* NewWeakGlobalRef() returns NULL for two cases:
             * - requested jobject is garbage collected: here it is not possibly,
             *   as passed jvmObject is local reference and jvmObject can NOT be
             *   garbage collected as long as "live" local reference exists.
             * - the VM runs out of memory and OutOfMemoryExceptionError is thrown - 
             *   suppose just this case is here
            */
            JNIEnvPtr->ExceptionClear();
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapToObjectID: NewWeakGlobalRef returned NULL"));
            return JDWP_OBJECT_ID_NULL;
        }
        if (m_freeObjectIDItems[idx] == NULL) {
            // expand table
            size_t objectIDTableOldSize = m_objectIDTableSize[idx];
            m_objectIDTableSize[idx]+= HASH_TABLE_GROW;
            m_objectIDTable[idx] = reinterpret_cast<ObjectIDItem*>
                (AgentBase::GetMemoryManager().Reallocate(m_objectIDTable[idx],
                    OBJECTID_ITEM_SIZE * objectIDTableOldSize,
                    OBJECTID_ITEM_SIZE * m_objectIDTableSize[idx] JDWP_FILE_LINE));
            objectIDItem = m_freeObjectIDItems[idx] = m_objectIDTable[idx] + objectIDTableOldSize;
            objectIDItemEnd = objectIDItem + HASH_TABLE_GROW - 1;
            while (objectIDItem != objectIDItemEnd) {
                objectIDItem->objectID = FREE_OBJECTID_SIGN;
                objectIDItem->nextFreeObjectIDItem = objectIDItem + 1;
                objectIDItem++;
            }
            objectIDItem->objectID = FREE_OBJECTID_SIGN;
            objectIDItem->nextFreeObjectIDItem = NULL;
        }
        objectIDItem = m_freeObjectIDItems[idx];
        m_freeObjectIDItems[idx] = objectIDItem->nextFreeObjectIDItem;
        objectID = objectIDItem - m_objectIDTable[idx] + 1;
        m_maxAllocatedObjectID[idx] = objectID > m_maxAllocatedObjectID[idx] ? objectID : m_maxAllocatedObjectID[idx];
        objectIDItem->objectID = objectID = (objectID << HASH_TABLE_IDX) | idx;
        objectIDItem->mapObjectIDItem.globalRefKind = WEAK_GLOBAL_REF;
        objectIDItem->mapObjectIDItem.jvmObject = newWeakGlobRef;
        objectIDItem->mapObjectIDItem.referencesCount = 0;
    }

    } // UNLOCK objectID table

    return objectID;
} // MapToObjectID()

jobject ObjectManager::MapFromObjectID(JNIEnv* JNIEnvPtr, ObjectID objectID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapFromObjectID(%p,%lld)", JNIEnvPtr, objectID));

    // decode object ID
    size_t idx = (size_t)objectID & HASH_TABLE_MSK;
    objectID = objectID >> HASH_TABLE_IDX;

    // check decoded object ID
    if (objectID <= 0 || objectID > m_maxAllocatedObjectID[idx]) {
        // It is DEBUGGER ERROR: request for ObjectID which was never allocated
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapFromObjectID: invalid object ID: %lld", objectID));
        return NULL;
    }

    // take object from table
    jobject jvmObject;

    { // synchronized block: objectIDTableLock
    MonitorAutoLock objectIDTableLock(m_objectIDTableMonitor JDWP_FILE_LINE);
    ObjectIDItem* objectIDItem = m_objectIDTable[idx] + objectID - 1;
    if (objectIDItem->objectID == FREE_OBJECTID_SIGN) {
        // It is DEBUGGER ERROR: Corresponding jobject is DISPOSED
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapFromObjectID: corresponding jobject has been disposed: %lld", objectID));
        return NULL;
    }
    jvmObject = objectIDItem->mapObjectIDItem.jvmObject;
    } // synchronized block: objectIDTableLock

    // Check if corresponding jobject has been Garbage collected*/
    if (JNIEnvPtr->IsSameObject(jvmObject, NULL) == JNI_TRUE) {
        // Corresponding jobject is Garbage collected
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapFromObjectID: corresponding jobject has been Garbage collected: %lld", objectID));
        return NULL;
    }

    return jvmObject;
} // MapFromObjectID()

jboolean ObjectManager::IsValidObjectID(JNIEnv* JNIEnvPtr,ObjectID objectID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "IsValidObjectID(%lld)", objectID));

    // decode object ID
    size_t idx = (size_t)objectID & HASH_TABLE_MSK;
    objectID = objectID >> HASH_TABLE_IDX;

    // check decoded object ID
    if (objectID <= 0 || objectID > m_maxAllocatedObjectID[idx]) {
        // such ObjectID was never allocated
        return JNI_FALSE;
    }

    jobject jvmObject;
    { // synchronized block: objectIDTableLock
        MonitorAutoLock objectIDTableLock(m_objectIDTableMonitor JDWP_FILE_LINE);
        ObjectIDItem* objectIDItem = m_objectIDTable[idx] + objectID - 1;
        if (objectIDItem->objectID == FREE_OBJECTID_SIGN) {
            // this ObjectID is DISPOSED
            return JNI_FALSE;
        }
        jvmObject = objectIDItem->mapObjectIDItem.jvmObject;
    } // synchronized block: objectIDTableLock
    
    // Check if corresponding jobject has been Garbage collected*/
    if (JNIEnvPtr->IsSameObject(jvmObject, NULL) == JNI_TRUE) {
        // Corresponding jobject is Garbage collected
        JDWP_TRACE(LOG_RELEASE, (LOG_DATA_FL, "## IsValidObjectID: corresponding jobject has been Garbage collected: %lld", objectID));
        return JNI_FALSE;
    }
        
    return JNI_TRUE;
} // IsValidObjectID() 

int ObjectManager::DisableCollection(JNIEnv* JNIEnvPtr, ObjectID objectID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "DisableCollection(%p,%lld)", JNIEnvPtr, objectID));

    // decode object ID
    size_t idx = (size_t)objectID & HASH_TABLE_MSK;
    objectID = objectID >> HASH_TABLE_IDX;

    // check decoded object ID
    if (objectID <= 0 || objectID > m_maxAllocatedObjectID[idx]) {
        // It is DEBUGGER ERROR: request for ObjectID which was never allocated
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## DisableCollection: invalid object ID: %lld", objectID));
        AgentException ex(JDWP_ERROR_INVALID_OBJECT);
        JDWP_SET_EXCEPTION(ex);
        return JDWP_ERROR_INVALID_OBJECT;
    }

    { // synchronized block: objectIDTableLock
        MonitorAutoLock objectIDTableLock(m_objectIDTableMonitor JDWP_FILE_LINE);
        ObjectIDItem* objectIDItem = m_objectIDTable[idx] + objectID - 1;
        if (objectIDItem->objectID == FREE_OBJECTID_SIGN) {
            // It is DEBUGGER ERROR: Corresponding jobject is DISPOSED
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## DisableCollection: corresponding jobject has been disposed: %lld", objectID));
            AgentException ex(JDWP_ERROR_INVALID_OBJECT);
            JDWP_SET_EXCEPTION(ex);
            return JDWP_ERROR_INVALID_OBJECT;
        }
    
        jobject jvmObject = objectIDItem->mapObjectIDItem.jvmObject;
        if (JNIEnvPtr->IsSameObject(jvmObject, NULL) == JNI_TRUE) {
            // Corresponding jobject is Garbage collected
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## DisableCollection: corresponding jobject has been Garbage collected: %lld", objectID));
            AgentException ex(JDWP_ERROR_INVALID_OBJECT);
            JDWP_SET_EXCEPTION(ex);
            return JDWP_ERROR_INVALID_OBJECT;
        }
        if (objectIDItem->mapObjectIDItem.globalRefKind == NORMAL_GLOBAL_REF) {
            // Repeated request for DisableCollection
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "<= DisableCollection: corresponding jobject has a global reference"));
            return JDWP_ERROR_NONE;
        }
    
        jobject newGlobRef = JNIEnvPtr->NewGlobalRef(jvmObject);
        if (newGlobRef == NULL) {
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## DisableCollection: NewGlobalRef returned NULL"));
            AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
            JDWP_SET_EXCEPTION(ex);
            return JDWP_ERROR_OUT_OF_MEMORY;
        }
        JNIEnvPtr->DeleteWeakGlobalRef((jweak)jvmObject);
        objectIDItem->mapObjectIDItem.globalRefKind = NORMAL_GLOBAL_REF;
        objectIDItem->mapObjectIDItem.jvmObject = newGlobRef;
    } // synchronized block: objectIDTableLock

    return JDWP_ERROR_NONE;
} // DisableCollection() 

int ObjectManager::EnableCollection(JNIEnv* JNIEnvPtr, ObjectID objectID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "EnableCollection(%p,%lld)", JNIEnvPtr, objectID));

    // decode object ID
    size_t idx = (size_t)objectID & HASH_TABLE_MSK;
    objectID = objectID >> HASH_TABLE_IDX;

    // check decoded object ID
    if (objectID <= 0 || objectID > m_maxAllocatedObjectID[idx]) {
        /* It is DEBUGGER ERROR: request for ObjectID which was never allocated
         * JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## EnableCollection: throw AgentException(JDWP_ERROR_INVALID_OBJECT)#1"));
         * throw AgentException(JDWP_ERROR_INVALID_OBJECT);
         * EnableCollection Command (ObjectReference Command Set) does not 
         * assume INVALID_OBJECT reply - so simply return:
        */
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## EnableCollection: invalid object ID: %lld", objectID));
        return JDWP_ERROR_NONE;
    }

    { // synchronized block: objectIDTableLock
        MonitorAutoLock objectIDTableLock(m_objectIDTableMonitor JDWP_FILE_LINE);
        ObjectIDItem* objectIDItem = m_objectIDTable[idx] + objectID - 1;
        if (objectIDItem->objectID == FREE_OBJECTID_SIGN) {
            /* It is DEBUGGER ERROR: Corresponding jobject is DISPOSED
             * It should be JDWP_ERROR_INVALID_OBJECT, but:;
             * EnableCollection Command (ObjectReference Command Set) does not 
             * assume INVALID_OBJECT reply - so simply return:
            */
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## EnableCollection: corresponding jobject has been disposed: %lld", objectID));
            return JDWP_ERROR_NONE;;
        }
        
        if (objectIDItem->mapObjectIDItem.globalRefKind == WEAK_GLOBAL_REF) {
            // Incorrect request for EnableCollection: 
            // ObjectID is in EnableCollection state
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "<= EnableCollection: corresponding jobject has a weak reference"));
            return JDWP_ERROR_NONE;;
        }
        
        jobject jvmObject = objectIDItem->mapObjectIDItem.jvmObject;
        jobject newWeakGlobRef = JNIEnvPtr->NewWeakGlobalRef(jvmObject);
        if (newWeakGlobRef == NULL) {
            /* NewWeakGlobalRef() returns NULL for two cases:
             * - requested jobject is garbage collected
             * - the VM runs out of memory and OutOfMemoryExceptionError is thrown
            */
            if (JNIEnvPtr->ExceptionCheck() == JNI_TRUE) {
                JNIEnvPtr->ExceptionClear();
                JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## EnableCollection: NewWeakGlobalRef returned NULL due to OutOfMemoryException"));
                AgentException ex(JDWP_ERROR_OUT_OF_MEMORY);
                JDWP_SET_EXCEPTION(ex);
                return JDWP_ERROR_OUT_OF_MEMORY;
            }
            /* else requested jobject is garbage collected
             * As EnableCollection Command (ObjectReference Command Set) does not 
             * assume INVALID_OBJECT reply - so simply return:
            */
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## EnableCollection: NewWeakGlobalRef returned NULL"));
            return JDWP_ERROR_NONE;
        }
        JNIEnvPtr->DeleteGlobalRef(jvmObject);
        objectIDItem->mapObjectIDItem.globalRefKind = WEAK_GLOBAL_REF;
        objectIDItem->mapObjectIDItem.jvmObject = newWeakGlobRef;
    } // synchronized block: objectIDTableLock

    return JDWP_ERROR_NONE;
} // EnableCollection()

jboolean ObjectManager::IsCollected(JNIEnv* JNIEnvPtr, ObjectID objectID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "IsCollected(%p,%lld)", JNIEnvPtr, objectID));

    // decode object ID
    size_t idx = (size_t)objectID & HASH_TABLE_MSK;
    objectID = objectID >> HASH_TABLE_IDX;

    // check decoded object ID
    if (objectID <= 0 || objectID > m_maxAllocatedObjectID[idx]) {
        // It is DEBUGGER ERROR: request for ObjectID which was never allocated
        JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "## IsCollected: invalid object ID: %lld", objectID));
        return JNI_FALSE;
    }

    jobject jvmObject;

    { // synchronized block: objectIDTableLock
    MonitorAutoLock objectIDTableLock(m_objectIDTableMonitor JDWP_FILE_LINE);
    ObjectIDItem* objectIDItem = m_objectIDTable[idx] + objectID - 1;
    if ( objectIDItem->objectID == FREE_OBJECTID_SIGN) {
        // It is DEBUGGER ERROR: Corresponding jobject is DISPOSED
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## IsCollected: corresponding jobject has been disposed: %lld", objectID));
        return JNI_FALSE;
    }

    jvmObject = objectIDItem->mapObjectIDItem.jvmObject;
    } // synchronized block: objectIDTableLock

    if (JNIEnvPtr->IsSameObject(jvmObject, NULL) == JNI_TRUE) {
        // Corresponding jobject has been Garbage collected
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "<= IsCollected: JNI_TRUE"));
        return JNI_TRUE;
    }

    return JNI_FALSE;
} // IsCollected() 

void ObjectManager::DisposeObject(JNIEnv* JNIEnvPtr, ObjectID objectID, jint refCount) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "DisposeObject(%p,%lld,%d)", JNIEnvPtr, objectID, refCount));

    // decode object ID
    size_t idx = (size_t)objectID & HASH_TABLE_MSK;
    objectID = objectID >> HASH_TABLE_IDX;

    // check decoded object ID
    if (objectID <= 0 || objectID > m_maxAllocatedObjectID[idx]) {
        /* It is DEBUGGER ERROR: request for ObjectID which was never allocated
         * JDWP spec does NOT provide to return reply for this command
         * so do nothing
        */
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## DisposeObject: invalid object ID: %lld", objectID));
        return;
    }

    { // synchronized block: objectIDTableLock
        MonitorAutoLock objectIDTableLock(m_objectIDTableMonitor JDWP_FILE_LINE);
        ObjectIDItem* objectIDItem = m_objectIDTable[idx] + objectID - 1;
        if (objectIDItem->objectID == FREE_OBJECTID_SIGN) {
            // It may be DEBUGGER ERROR: Corresponding jobject has been disposed already
            // - do nothing
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## DisposeObject: corresponding jobject has been disposed: %lld", objectID));
            return;
        }
        
        jint newRefCount = objectIDItem->mapObjectIDItem.referencesCount - refCount;
        if (newRefCount > 0) {
            // Still early to dispose ObjectID 
            objectIDItem->mapObjectIDItem.referencesCount = newRefCount;
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "<= DisposeObject: still positive ref count: %d", newRefCount));
            return;
        }
        
        jobject jvmObject = objectIDItem->mapObjectIDItem.jvmObject;
        if (objectIDItem->mapObjectIDItem.globalRefKind == NORMAL_GLOBAL_REF) {
            JNIEnvPtr->DeleteGlobalRef(jvmObject);
        } else {
            JNIEnvPtr->DeleteWeakGlobalRef((jweak)jvmObject);
        }
        objectIDItem->objectID = FREE_OBJECTID_SIGN;
        objectIDItem->nextFreeObjectIDItem = m_freeObjectIDItems[idx];
        m_freeObjectIDItems[idx] = objectIDItem;
    } // synchronized block: objectIDTableLock

} // DisposeObject() 

jint ObjectManager::IncreaseIDRefCount(ObjectID objectID, jint incrementValue) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "IncreaseIDRefCount(%lld,%d)", objectID, incrementValue));

    // decode object ID
    size_t idx = (size_t)objectID & HASH_TABLE_MSK;
    objectID = objectID >> HASH_TABLE_IDX;

    JDWP_ASSERT(objectID >= 0);
    JDWP_ASSERT(objectID <= m_maxAllocatedObjectID[idx]);
    if (objectID == JDWP_OBJECT_ID_NULL) {
        // returned objectID is not real - it is possibly, so do nothing:
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## IncreaseIDRefCount: invalid object ID: %lld", objectID));
        return 0;
    }

    // check decoded object ID
    if (objectID <= 0 || objectID > m_maxAllocatedObjectID[idx]) {
        /* It is DEBUGGER ERROR: request for ObjectID which was never allocated
         * JDWP spec does NOT provide to return reply for this command
         * so do nothing
        */
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## IncreaseIDRefCount: invalid object ID: %lld", objectID));
        return 0;
    }

    jint newRefCount;
    { // synchronized block: objectIDTableLock
    MonitorAutoLock objectIDTableLock(m_objectIDTableMonitor JDWP_FILE_LINE);
    ObjectIDItem* objectIDItem = m_objectIDTable[idx] + objectID - 1;
    if (objectIDItem->objectID == FREE_OBJECTID_SIGN) {
        // Corresponding jobject is DISPOSED - unlikely but possibly theoretically
        // so do nothing
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## IncreaseIDRefCount: corresponding jobject has been disposed: %lld", objectID));
        return 0;
    }
    newRefCount = objectIDItem->mapObjectIDItem.referencesCount + incrementValue;
    objectIDItem->mapObjectIDItem.referencesCount = newRefCount;
    } // synchronized block: objectIDTableLock

    return newRefCount;
} // IncreaseIDRefCount() 

void ObjectManager::InitObjectIDMap() {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "InitObjectIDMap()"));

    memset(m_objectIDTableSize, 0, sizeof(m_objectIDTableSize));
    memset(m_maxAllocatedObjectID, 0, sizeof(m_maxAllocatedObjectID));
    memset(m_objectIDTable, 0, sizeof(m_objectIDTable));
    memset(m_freeObjectIDItems, 0, sizeof(m_freeObjectIDItems));
} // InitObjectIDMap()

void ObjectManager::ResetObjectIDMap(JNIEnv* JNIEnvPtr) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ResetObjectIDMap(%p)", JNIEnvPtr));

    for (size_t idx = 0; idx < HASH_TABLE_SIZE; idx++) {
        if (m_objectIDTable[idx]) {
            ObjectIDItem* objectIDItem = m_objectIDTable[idx];
            ObjectIDItem* objectIDItemEnd = objectIDItem + m_maxAllocatedObjectID[idx];
            while (objectIDItem != objectIDItemEnd) {
                if (objectIDItem->objectID != FREE_OBJECTID_SIGN) {
                    if (objectIDItem->mapObjectIDItem.globalRefKind == NORMAL_GLOBAL_REF) {
                        JNIEnvPtr->DeleteGlobalRef(objectIDItem->mapObjectIDItem.jvmObject);
                    } else {
                        JNIEnvPtr->DeleteWeakGlobalRef((jweak)objectIDItem->mapObjectIDItem.jvmObject);
                    }
                }
                objectIDItem++;
            }
            AgentBase::GetMemoryManager().Free(m_objectIDTable[idx] JDWP_FILE_LINE);
        }
    }
    InitObjectIDMap();
} // ResetObjectIDMap()


// =============================================================================
// Mapping: ReferenceTypeID <-> jclass (=> jobject)
// Includes JDWP types: referenceTypeID, classID, interfaceID, arrayID

ReferenceTypeID ObjectManager::MapToReferenceTypeID(JNIEnv* JNIEnvPtr, jclass jvmClass) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapToReferenceTypeID(%p,%p)", JNIEnvPtr, jvmClass));

    if (jvmClass == NULL) {
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapToReferenceTypeID: map NULL jclass"));
        return JDWP_OBJECT_ID_NULL;
    }

    // get object HASH CODE
    jint hashCode = -1;
    if (GetObjectHashCode(jvmClass, &hashCode) != JVMTI_ERROR_NONE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapToReferenceTypeID: GetObjectHashCode failed"));
        return JDWP_OBJECT_ID_NULL;
    }

    // get HASH INDEX
    size_t idx = size_t(hashCode) & HASH_TABLE_MSK;

    ReferenceTypeID refTypeID = -1;

    { // LOCK ReferenceTypeID table
    MonitorAutoLock refTypeIDTableLock(m_refTypeIDTableMonitor JDWP_FILE_LINE);

    // find EXISTING 'same' class object
    for (size_t item = 0; item < m_refTypeIDTableUsed[idx]; item++) {
        if (JNIEnvPtr->IsSameObject(m_refTypeIDTable[idx][item], jvmClass) == JNI_TRUE) {
            refTypeID = (item << HASH_TABLE_IDX) | idx;
            break;
        }
    }

    // add NEW class object if not found existing
    if (-1 == refTypeID) {
        JNIEnvPtr->ExceptionClear();
        // make global WEAK REFERENCE
        jclass newWeakGlobRef = reinterpret_cast<jclass>(JNIEnvPtr->NewWeakGlobalRef(jvmClass));
        if (newWeakGlobRef == NULL) {
            /* NewWeakGlobalRef() returns NULL for two cases:
             * - requested jclass object is garbage collected: here it is not possibly,
             *   as passed jvmClass is local reference and jvmClass can NOT be
             *   garbage collected as long as "live" local reference exists.
             * - the VM runs out of memory and OutOfMemoryExceptionError is thrown - 
             *   suppose just this case is here
            */
            JNIEnvPtr->ExceptionClear();
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapToReferenceTypeID: NewWeakGlobalRef returned NULL due to OutOfMemoryException"));
            return JDWP_OBJECT_ID_NULL;
        }
        // expand table if needed
        if (m_refTypeIDTableUsed[idx] == m_refTypeIDTableSize[idx])
        {
            size_t oldSize = m_refTypeIDTableSize[idx];
            m_refTypeIDTableSize[idx]+= HASH_TABLE_GROW;
            // Reallocate => can throw OutOfMemoryException, InternalErrorException 
            m_refTypeIDTable[idx] = (jclass*)(GetMemoryManager().Reallocate(
                m_refTypeIDTable[idx], sizeof(jclass)*oldSize, sizeof(jclass)*m_refTypeIDTableSize[idx] JDWP_FILE_LINE));
        }
        refTypeID = (m_refTypeIDTableUsed[idx] << HASH_TABLE_IDX) | idx;
        m_refTypeIDTable[idx][m_refTypeIDTableUsed[idx]] = newWeakGlobRef;
        m_refTypeIDTableUsed[idx]++;
    } // if (-1 == refTypeID)

    } // UNLOCK ReferenceTypeID table

    return refTypeID + REFTYPEID_MINIMUM;
} // MapToReferenceTypeID() 

jclass ObjectManager::MapFromReferenceTypeID(JNIEnv* JNIEnvPtr,
                                             ReferenceTypeID refTypeID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapFromReferenceTypeID(%p,%lld)", JNIEnvPtr, refTypeID));

    refTypeID-= REFTYPEID_MINIMUM;
    jclass jvmClass;

    { // LOCK ReferenceTypeID table
    MonitorAutoLock refTypeIDTableLock(m_refTypeIDTableMonitor JDWP_FILE_LINE);

    // calculate hash index
    // masking guarantees the index will be in the range [0..HASH_TABLE_SIZE-1]
    size_t idx = (size_t)refTypeID & HASH_TABLE_MSK;

    // calculate buffer index
    size_t item = (size_t)refTypeID >> HASH_TABLE_IDX;

    // check buffer index
    if (item >= m_refTypeIDTableUsed[idx]) {
        if ( IsValidObjectID(JNIEnvPtr, refTypeID + REFTYPEID_MINIMUM) ) {
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "## MapFromReferenceTypeID: class is invalid"));
            return NULL;
        } else {
            JDWP_TRACE(LOG_RELEASE, (LOG_ERROR_FL, "## MapFromReferenceTypeID: object is invalid"));
            return NULL;
        }
    }

    jvmClass = m_refTypeIDTable[idx][item];

    // check if corresponding jclass has been Garbage collected
    if (JNIEnvPtr->IsSameObject(jvmClass, NULL) == JNI_TRUE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapFromReferenceTypeID: corresponding jclass has been Garbage collected"));
        return NULL;
    }

    } // UNLOCK ReferenceTypeID table

    return jvmClass;
} // MapFromReferenceTypeID()

jboolean ObjectManager::IsValidReferenceTypeID(JNIEnv* JNIEnvPtr,ObjectID refTypeID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "IsValidReferenceTypeID(%p,%lld)", JNIEnvPtr, refTypeID));

    refTypeID-= REFTYPEID_MINIMUM;
    jclass jvmClass;

    { // LOCK ReferenceTypeID table
        
    MonitorAutoLock refTypeIDTableLock(m_refTypeIDTableMonitor JDWP_FILE_LINE);

    // calculate hash index
    // masking guarantees the index will be in the range [0..HASH_TABLE_SIZE-1]
    size_t idx = (size_t)refTypeID & HASH_TABLE_MSK;

    // calculate buffer index
    size_t item = (size_t)refTypeID >> HASH_TABLE_IDX;

    // check buffer index
    if (item < 0 || item >= m_refTypeIDTableUsed[idx]) {
        return JNI_FALSE;
    }

    jvmClass = m_refTypeIDTable[idx][item];

    // check if corresponding jclass has been Garbage collected
    if (JNIEnvPtr->IsSameObject(jvmClass, NULL) == JNI_TRUE) {
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapFromReferenceTypeID: corresponding jclass has been Garbage collected"));
        return JNI_FALSE;
    }

    } // UNLOCK ReferenceTypeID table

    return JNI_TRUE;
} // IsValidReferenceTypeID()   

void ObjectManager::InitRefTypeIDMap() {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "InitRefTypeIDMap()"));

    memset(m_refTypeIDTable, 0, sizeof(m_refTypeIDTable));
    memset(m_refTypeIDTableSize, 0, sizeof(m_refTypeIDTableSize));
    memset(m_refTypeIDTableUsed, 0, sizeof(m_refTypeIDTableUsed));
} // InitRefTypeIDMap()

void ObjectManager::ResetRefTypeIDMap(JNIEnv* JNIEnvPtr) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ResetRefTypeIDMap(%p)", JNIEnvPtr));

    for (size_t idx = 0; idx < HASH_TABLE_SIZE; idx++) {
        if (m_refTypeIDTable[idx]) {
            for (size_t item = 0; item < m_refTypeIDTableUsed[idx]; item++)
                JNIEnvPtr->DeleteWeakGlobalRef((jweak)m_refTypeIDTable[idx][item]);
            GetMemoryManager().Free(m_refTypeIDTable[idx] JDWP_FILE_LINE);
            m_refTypeIDTable[idx] = NULL;
            m_refTypeIDTableUsed[idx] = m_refTypeIDTableSize[idx] = 0;
        }
    }
    InitRefTypeIDMap();
} // ResetRefTypeIDMap()

// =============================================================================
/* Mapping: FieldID <-> jfieldID
 * Includes JDWP types: fieldID
 * don't store any data, simply to convert:
 * FieldID (jlong) <-> jfieldID (_jfieldID*)
*/
   
FieldID ObjectManager::MapToFieldID(JNIEnv* JNIEnvPtr, jfieldID jvmFieldID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapToFieldID(%p,%p)", JNIEnvPtr, jvmFieldID));

    FieldID fieldID = reinterpret_cast<FieldID>(jvmFieldID);
    return fieldID;
} // MapToFieldID() 
 
jfieldID ObjectManager::MapFromFieldID(JNIEnv* JNIEnvPtr, FieldID fieldID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapFromFieldID(%p,%p)", JNIEnvPtr, fieldID));

    jfieldID jvmFieldID = (jfieldID)(fieldID);
    return jvmFieldID;
} // MapFromFieldID() 

// =============================================================================
/* Mapping: MethodID <-> jmethodID
 * Includes JDWP types: methodID
 * don't store any data, simply to convert:
 * MethodID (jlong) <-> jmethodID (_jmethodID*)
*/
   
MethodID ObjectManager::MapToMethodID(JNIEnv* JNIEnvPtr, jmethodID jvmMethodID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapToMethodID(%p,%p)", JNIEnvPtr, jvmMethodID));

    MethodID methodID = reinterpret_cast<MethodID>(jvmMethodID);
    return methodID;
} // MapToMethodID() 
 
jmethodID ObjectManager::MapFromMethodID(JNIEnv* JNIEnvPtr, MethodID methodID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapFromMethodID(%p,%lld)", JNIEnvPtr, methodID));

    jmethodID jvmMethodID = (jmethodID)(methodID);
    return jvmMethodID;
} // MapFromMethodID() 

// =============================================================================

/* Mapping: FrameID <-> jthread + depth (jint)
 * Includes JDWP types: frameID
 * The FrameID uniquely identifies the frame within the entire VM 
*/
   
// Constant (for 'framesCountOfThread' field in ThreadFramesItem structure)
// as sign that ThreadFramesItem describes free item in m_frameIDTable
const jint FRAMES_COUNT_OF_FREE_ITEM = -1;

// Constant defining initial size of m_frameIDTable
const jlong FRAMEID_TABLE_INIT_SIZE = 128; // in ThreadFramesItem

ObjectManager::ThreadFramesItem* ObjectManager::ExpandThreadFramesTable()
        {
    if ( m_frameIDTableSize == 0 ) {
        m_frameIDTable =
            reinterpret_cast<ThreadFramesItem*>
                (AgentBase::GetMemoryManager().Allocate
                (THREAD_FRAMES_ITEM_SIZE * FRAMEID_TABLE_INIT_SIZE JDWP_FILE_LINE));
        m_frameIDTableSize = FRAMEID_TABLE_INIT_SIZE;
    } else {
        jlong framesTableOldSize = m_frameIDTableSize;
        m_frameIDTableSize = m_frameIDTableSize + FRAMEID_TABLE_INIT_SIZE;
        m_frameIDTable = reinterpret_cast<ThreadFramesItem*>
            (AgentBase::GetMemoryManager().Reallocate
            (m_frameIDTable,
            static_cast<size_t>(THREAD_FRAMES_ITEM_SIZE * framesTableOldSize),
            static_cast<size_t>(THREAD_FRAMES_ITEM_SIZE * m_frameIDTableSize) JDWP_FILE_LINE));
    }
    ThreadFramesItem* threadFramesItem
        = m_frameIDTable + m_frameIDTableSize - FRAMEID_TABLE_INIT_SIZE;
    ThreadFramesItem* threadFramesItemToReturn = threadFramesItem;
    jlong tableIndex = 0;
    for (; tableIndex < FRAMEID_TABLE_INIT_SIZE; tableIndex++) {
        threadFramesItem->jvmThread = NULL;
        threadFramesItem->framesCountOfThread = FRAMES_COUNT_OF_FREE_ITEM;
        threadFramesItem++;
    }
    m_freeItemsNumberInFrameIDTable = FRAMEID_TABLE_INIT_SIZE; 
    return threadFramesItemToReturn;
} // ExpandThreadFramesTable() 

ObjectManager::ThreadFramesItem* ObjectManager::NewThreadFramesItem
        (JNIEnv* JNIEnvPtr, jthread jvmThread, jint framesCount)
{
    ThreadFramesItem* threadFramesItem = m_frameIDTable;
    if ( m_freeItemsNumberInFrameIDTable == 0 ) {
        threadFramesItem = ExpandThreadFramesTable();
        // can be OutOfMemoryException, InternalErrorException
    } else {
        // searching for free item
        for ( ; ;) {
            if ( threadFramesItem->framesCountOfThread
                    == FRAMES_COUNT_OF_FREE_ITEM ) {
                break;
            }
            threadFramesItem++;
        }
    }

    // create weak global reference for jvmThread
    JNIEnvPtr->ExceptionClear();
    jobject newWeakGlobRef = JNIEnvPtr->NewWeakGlobalRef(jvmThread);
    if ( newWeakGlobRef == NULL ) {
        /* NewWeakGlobalRef() returns NULL for two cases:
         * - requested  jvmThread object is garbage collected: here it is not possibly,
         *   as passed jvmThread is global reference and jvmThread can NOT be
         *   garbage collected as long as "live" global reference exists.
         * - the VM runs out of memory and OutOfMemoryExceptionError is thrown - 
         *   suppose just this case is here
        */
        JNIEnvPtr->ExceptionClear();
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## NewThreadFramesItem: OutOfMemoryException"));
        return NULL;
    }

    threadFramesItem->jvmThread = newWeakGlobRef;
    threadFramesItem->currentFrameID = m_maxAllocatedFrameID + 1;
    threadFramesItem->framesCountOfThread = framesCount;
    m_freeItemsNumberInFrameIDTable--;
    m_maxAllocatedFrameID = m_maxAllocatedFrameID + framesCount;
    return threadFramesItem;
} // NewThreadFramesItem() 

FrameID ObjectManager::MapToFrameID(JNIEnv* JNIEnvPtr, jthread jvmThread,
                                    jint frameDepth, jint framesCount)
                                    {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapToFrameID(%p,%p,%d,%d)", JNIEnvPtr, jvmThread, frameDepth, framesCount));

    /* according to the JDWP agent policy it is supposed that passed jvmThread
     * is JNI global reference so do not check if the jvmThread is garbage
     * collected.
    */

    // searching for given jvmThread item
    FrameID frameID;
    { // synchronized block: frameIDTableLock
    MonitorAutoLock frameIDTableLock(m_frameIDTableMonitor JDWP_FILE_LINE);
    ThreadFramesItem* threadFramesItem = m_frameIDTable;
    jlong tableIndex = 0;
    for (; tableIndex < m_frameIDTableSize; tableIndex++) {
        if ( threadFramesItem->framesCountOfThread
                == FRAMES_COUNT_OF_FREE_ITEM ) {
            threadFramesItem++;
            continue;
        }
        if ( JNIEnvPtr->IsSameObject(jvmThread, threadFramesItem->jvmThread)
                == JNI_TRUE ) {
            break;
        }
        threadFramesItem++;
    }
    if ( tableIndex == m_frameIDTableSize ) { 
        // threadFramesItem for given jvmThread is not found out in table
        if ( (frameDepth < 0) 
                || (frameDepth >= framesCount) ) {
            // passed frameDepth is INVALID ");
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapToFrameID: JDWP_ERROR_INVALID_LENGTH#1"));
            return 0;
        }
        threadFramesItem = NewThreadFramesItem(JNIEnvPtr, jvmThread, framesCount);
        // can be OutOfMemoryException, InternalErrorException
    } else {
        if ( (frameDepth < 0) 
                || (frameDepth >= threadFramesItem->framesCountOfThread) ) {
            // passed frameDepth is INVALID ");
            JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapToFrameID: JDWP_ERROR_INVALID_LENGTH#2"));
            return 0;
        }
    }
    frameID = threadFramesItem->currentFrameID + frameDepth;
    } // synchronized block: frameIDTableLock
    return frameID;

} // MapToFrameID() 

jint ObjectManager::MapFromFrameID(JNIEnv* JNIEnvPtr, FrameID frameID) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "MapFromFrameID(%p,%lld)", JNIEnvPtr, frameID));

    // searching for threadFramesItem for given FrameID
    jint frameIndex;
    { // synchronized block: frameIDTableLock
    MonitorAutoLock frameIDTableLock(m_frameIDTableMonitor JDWP_FILE_LINE);
    ThreadFramesItem* threadFramesItem = m_frameIDTable;
    jlong tableIndex = 0;
    for (; tableIndex < m_frameIDTableSize; tableIndex++) {
        if ( threadFramesItem->framesCountOfThread
                == FRAMES_COUNT_OF_FREE_ITEM ) {
            threadFramesItem++;
            continue;
        }
        if ( (threadFramesItem->currentFrameID <= frameID)
                && (threadFramesItem->currentFrameID
                    + threadFramesItem->framesCountOfThread > frameID ) ) {
            break;
        }
        threadFramesItem++;
    }
    if ( tableIndex == m_frameIDTableSize ) {
        // threadFramesItem for given FrameID is not found out in table*/
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## MapFromFrameID: JDWP_ERROR_INVALID_FRAMEID"));
        return 0;
    }
    frameIndex = static_cast<jint>(frameID - threadFramesItem->currentFrameID);
    } // synchronized block: frameIDTableLock
    return frameIndex;
} // MapFromFrameID() 

void ObjectManager::DeleteFrameIDs(JNIEnv* JNIEnvPtr, jthread jvmThread) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "DeleteFrameIDs(%p,%p)", JNIEnvPtr, jvmThread));

    if ( JNIEnvPtr->IsSameObject(jvmThread, NULL) == JNI_TRUE ) {
        // jvmThread object is GARBAGE COLLECTED: possible case - do nothing
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "## DeleteFrameIDs: ignore NULL jthread"));
        return;
    }

    // searching for given jvmThread item
    { // synchronized block: frameIDTableLock
    MonitorAutoLock frameIDTableLock(m_frameIDTableMonitor JDWP_FILE_LINE);
    ThreadFramesItem* threadFramesItem = m_frameIDTable;
    jlong tableIndex = 0;
    for (; tableIndex < m_frameIDTableSize; tableIndex++) {
        if ( threadFramesItem->framesCountOfThread
                == FRAMES_COUNT_OF_FREE_ITEM ) {
            threadFramesItem++;
            continue;
        }
        if ( JNIEnvPtr->IsSameObject(jvmThread, threadFramesItem->jvmThread)
                == JNI_TRUE ) {
            break;
        }
        threadFramesItem++;
    }
    if ( tableIndex != m_frameIDTableSize ) {
        // threadFramesItem for given jvmThread is found out in table
        threadFramesItem->jvmThread = NULL;
        threadFramesItem->framesCountOfThread = FRAMES_COUNT_OF_FREE_ITEM;
        m_freeItemsNumberInFrameIDTable++; 
    }
    /* if threadFramesItem for given jvmThread is not found out in table
     * - it is possible case - do nothing
    */
    } // synchronized block: frameIDTableLock
} // DeleteFrameIDs() 

void ObjectManager::InitFrameIDMap() {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "InitFrameIDMap()"));

    m_frameIDTableSize = 0; 
    m_freeItemsNumberInFrameIDTable = 0; 
    m_frameIDTable = 0;
    m_maxAllocatedFrameID = 0;
} // InitFrameIDMap()

void ObjectManager::ResetFrameIDMap(JNIEnv* JNIEnvPtr) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "ResetFrameIDMap(%p)", JNIEnvPtr));

    if ( m_frameIDTable != 0 ) {
        // delete all weak global references from frameIDTable
        ThreadFramesItem* threadFramesItem = m_frameIDTable;
        jlong tableIndex = 0;
        for (; tableIndex < m_frameIDTableSize; tableIndex++) {
            if ( threadFramesItem->framesCountOfThread
                    == FRAMES_COUNT_OF_FREE_ITEM ) {
                threadFramesItem++;
                continue;
            }
            JNIEnvPtr->DeleteWeakGlobalRef((jweak)threadFramesItem->jvmThread);
            threadFramesItem++;
        }
        AgentBase::GetMemoryManager().Free(m_frameIDTable JDWP_FILE_LINE);
        // -> InternalErrorException
    }
    InitFrameIDMap();
} // ResetFrameIDMap()

// =============================================================================

void ObjectManager::Init(JNIEnv* JNIEnvPtr) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Init(%p)", JNIEnvPtr));

    InitObjectIDMap();
    InitRefTypeIDMap();
    InitFrameIDMap();
    m_objectIDTableMonitor = new AgentMonitor("_agent_Object_Manager_objectIDTable"); 
    m_refTypeIDTableMonitor = new AgentMonitor("_agent_Object_Manager_refTypeIDTable"); 
    m_frameIDTableMonitor = new AgentMonitor("_agent_Object_Manager_frameIDTable");
    // can be AgentException(jvmtiError err);
} // Init()

void ObjectManager::Reset(JNIEnv* JNIEnvPtr) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Reset(%p)", JNIEnvPtr));

    if (m_objectIDTableMonitor != 0){
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "=> m_objectIDTableMonitor "));
        m_objectIDTableMonitor->Enter(); 
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "<= m_objectIDTableMonitor"));
        m_objectIDTableMonitor->Exit(); 
        ResetObjectIDMap(JNIEnvPtr); //    can    be InternalErrorException
    }

    if (m_refTypeIDTableMonitor != 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "=> m_refTypeIDTableMonitor"));
        m_refTypeIDTableMonitor->Enter(); 
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "<= m_refTypeIDTableMonitor"));
        m_refTypeIDTableMonitor->Exit(); 
        ResetRefTypeIDMap(JNIEnvPtr); // can be InternalErrorException
    }

    if (m_frameIDTableMonitor != 0) {
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "=> m_frameIDTableMonitor"));
        m_frameIDTableMonitor->Enter(); 
        JDWP_TRACE(LOG_RELEASE, (LOG_MAP_FL, "<= m_frameIDTableMonitor"));
        m_frameIDTableMonitor->Exit(); 
        ResetFrameIDMap(JNIEnvPtr); // can be InternalErrorException
    }
} // Reset()

void ObjectManager::Clean(JNIEnv* JNIEnvPtr) {
    JDWP_TRACE_ENTRY(LOG_RELEASE, (LOG_FUNC_FL, "Clean(%p)", JNIEnvPtr));

    if (m_objectIDTableMonitor != 0)
        delete m_objectIDTableMonitor;
    if (m_refTypeIDTableMonitor!= 0)
        delete m_refTypeIDTableMonitor;
    if (m_frameIDTableMonitor!= 0)
        delete m_frameIDTableMonitor;
} // Clean()

ObjectManager::ObjectManager ()
{
    m_objectIDTableMonitor = 0;
    m_refTypeIDTableMonitor = 0;
    m_frameIDTableMonitor = 0;

    // for debugging only
#ifndef NDEBUG
    m_frameIDTable = &m_DummyFrameIDTable;
#endif // NDEBUG
}

ObjectManager::~ObjectManager () {
}

// =============================================================================
