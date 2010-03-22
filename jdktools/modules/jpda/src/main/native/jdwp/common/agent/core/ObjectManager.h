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
 * @author Anatoly F. Bondarenko
 */

/**
 * @file
 * ObjectManager.h
 *
 * Provides mapping between JDWP IDs and corresponding JVMTI, JNI data types.
 */

#ifndef _OBJECT_MANAGER_H
#define _OBJECT_MANAGER_H

#include "AgentBase.h"
#include "AgentException.h"
#include "jni.h"
#include "jdwp.h"
#include "jdwpTypes.h"
#include "AgentMonitor.h"

namespace jdwp {

    // hash table parameters
    enum {
        // hash table buffer incrementation value
        HASH_TABLE_GROW = 8,

        // number of bits to hold (hash) index in hash table
        HASH_TABLE_IDX = 10,

        // hash table size
        HASH_TABLE_SIZE = 1 << HASH_TABLE_IDX,

        // value for masking hash index in ID
        HASH_TABLE_MSK = HASH_TABLE_SIZE - 1,
    };

    /** 
     * The ObjectManager class provides mapping between JDWP IDs 
     * and corresponding JVMTI, JNI data types. It includes the following mappings:
     * - <code>ObjectID</code> <-> <code>jobject</code>
     *   Covers the following JDWP types: <code>objectID</code>, <code>threadID</code>, 
     *                                    <code>threadGroupID</code>, <code>stringID</code>,
     *                                    <code>classLoaderID</code>, <code>classObjectID</code>, 
     *                                    <code>arrayID</code>.
     * - <code>ReferenceTypeID</code> <-> <code>jclass</code> (=> <code>jobject</code>)
     *   Covers the following JDWP types: <code>referenceTypeID</code>, <code>classID</code>, 
     *                                    <code>interfaceID</code>, <code>arrayTypeID</code>.
     * - <code>FieldID</code> <-> <code>jfieldID</code>
     *   Covers the following JDWP type: <code>fieldID</code>.
     * - <code>MethodID</code> <-> <code>jmethodID</code>
     *   Covers the following JDWP type: <code>methodID</code>.
     * - <code>FrameID</code> <-> <code>jthread</code> + <code>depth (jint)</code>
     *   Covers the following JDWP type: <code>frameID</code>.
     */

    class ObjectManager : public AgentBase {

    // =========================================================================
        // Public Primitives of Mapping 
        // Public primitives of Mapping 
    // =========================================================================
    public:

        // Mapping: <code>ObjectID</code> <-> <code>jobject</code>
        // Includes the following JDWP types: <code>objectID</code>, <code>threadID</code>, 
        //                                    <code>threadGroupID</code>, <code>stringID</code>,
        //                                    <code>classLoaderID</code>, <code>classObjectID</code>,
        //                                    <code>arrayID</code>.
 
        /** 
         * Maps the JVM object of the type <code>jobject</code> to the JDWP 
         * identifier of the <code>ObjectID</code> type including the following 
         * JDWP types: <code>objectID</code>, <code>threadID</code>, 
         *             <code>threadGroupID</code>, <code>stringID</code>, 
         *             <code>classLoaderID</code>, <code>classObjectID</code>, 
         *             <code>arrayID</code>.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param jvmObject - the JVM object to be mapped. If its value
         *                    <code>NULL</code>, the <code>JDWP_OBJECT_ID_NULL</code> value is returned.
         */

        /**
         * IMPORTANT! Please take into account that the <code>NewWeakGlobalRef</code> 
         * is requested for passed <code>jvmObject</code> and it is used in the future.
         * That is the MapFromObjectID() function returns this 
         * <code>NewWeakGlobalRef</code>. 
         * The caller's responsibility is to deal with passed <code>jvmObject</code>, 
         * that is to delete if it is a global ref.
         *
         * @return Returns the JDWP identifier of the type <code>ObjectID</code>
         *         to which <code>jvmObject</code> is mapped. The <code>JDWP_OBJECT_ID_NULL</code> 
         *         value can be returned, if the passed <code>jvmObject</code> has a 
         *         <code>NULL</code> value.
         *
         * @exception OutOfMemoryException 
         *            is the same as <code>AgentException(JDWP_ERROR_OUT_OF_MEMORY)</code> - 
         *            if out-of-memory error has occurred during execution of the given 
         *            function.
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        ObjectID MapToObjectID(JNIEnv* JNIEnvPtr, jobject jvmObject)
            throw (AgentException);

        /** 
         * Maps the JDWP identifier of the type <code>ObjectID</code> to the 
         * JVM object of the type <code>jobject</code>.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param objectID  - the JDWP identifier of the <code>ObjectID</code>
         *                    type to be mapped
         *
         * @return Returns the <code>jvmObject</code> of the type 
         *         <code>jobject</code> to which <code>ObjectID</code> is mapped.
         *
         * @exception AgentException(JDWP_ERROR_INVALID_OBJECT) - 
         *            if the given <code>ObjectID</code> was never allocated 
         *            or was disposed; or corresponding <code>jvmObject</code>
         *            has been unloaded and garbage-collected.
         */
        jobject MapFromObjectID(JNIEnv* JNIEnvPtr, ObjectID objectID)
            throw (AgentException);

        /** 
         * Changes a condition of corresponding <code>jvmObject</code> so, 
         * that it cannot be garbage-collected.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param objectID  - the JDWP identifier of the type <code>ObjectID</code>
         *                    defining corresponding jvmObject
         *
         * @exception AgentException(JDWP_ERROR_INVALID_OBJECT) - 
         *            if the given <code>ObjectID</code> was never allocated 
         *            or was disposed; or corresponding <code>jvmObject</code> 
         *            has been already unloaded and garbage collected.
         *
         * @exception OutOfMemoryException is the same as 
         *            <code>AgentException(JDWP_ERROR_OUT_OF_MEMORY)</code> - 
         *            if out-of-memory error has occurred during execution 
         *            of the given function.
         */
        void DisableCollection(JNIEnv* JNIEnvPtr, ObjectID objectID)
            throw (AgentException);

        /** 
         * Changes a condition of corresponding <code>jvmObject</code> so, that
         * it may be garbage-collected. Usually the given function is used if garbage 
         * collection was previously disabled for given <code>jvmObject</code>.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param objectID  - the JDWP identifier of the type <code>ObjectID</code>
         *                    defining corresponding <code>jvmObject</code>
         *
         * @exception OutOfMemoryException is the same as 
         *            <code>AgentException(JDWP_ERROR_OUT_OF_MEMORY)</code> - if 
         *            out-of-memory error has occurred during execution of the 
         *            given function.
         */
        void EnableCollection(JNIEnv* JNIEnvPtr, ObjectID objectID)
            throw (AgentException);

        /** 
         * Checks if garbage collection is disabled for corresponding 
         * <code>jvmObject</code> mapped to given <code>ObjectID</code>.
         *
         * @param objectID - the JDWP identifier of the type <code>ObjectID</code>
         *                   defining corresponding <code>jvmObject</code>
         *
         * @return Returns the <code>JNI_TRUE</code> value if garbage collection is 
         *         disabled for corresponding <code>jvmObject</code>, 
         *         <code>JNI_FALSE</code> otherwise.
         *
         * @exception AgentException(JDWP_ERROR_INVALID_OBJECT) - 
         *            if the given <code>ObjectID</code> was never allocated
         *            or was disposed.
         */
        jboolean IsCollectionDisabled(ObjectID objectID) throw (AgentException);
  
        /** 
         * Checks if corresponding <code>jvmObject</code> has been 
         * garbage-collected.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param objectID  - the JDWP identifier of the type <code>ObjectID</code> 
         *                    defining corresponding <code>jvmObject</code>
         *
         * @return Returns the <code>JNI_TRUE</code> value if corresponding 
         *         <code>jvmObject</code> has been garbage-collected, 
         *         <code>JNI_FALSE</code> otherwise.
         *
         * @exception AgentException(JDWP_ERROR_INVALID_OBJECT) - 
         *            if the given <code>ObjectID</code> was never allocated 
         *            or was disposed.
         */
        jboolean IsCollected(JNIEnv* JNIEnvPtr, ObjectID objectID)
            throw (AgentException);

        /** 
         * Disposes corresponding <code>jvmObject</code> defining by given 
         * <code>ObjectID</code>, namely: the count of references held for 
         * the given <code>ObjectID</code> is decremented by
         * given <code>refCount</code> and if thereafter the count is less 
         * than or equal to zero, the <code>ObjectID</code> is removed from 
         * the allocated IDs list and the global reference for corresponding 
         * <code>jvmObject</code> is deleted. So, <code>jvmObject</code>
         * may be garbage-collected. The freed <code>ObjectID</code> may be 
         * re-used for other <code>jvmObject</code>.
         *
         * @param JNIEnvPtr  - the JNI interface pointer used to call
         *                     necessary JNI functions
         * @param objectID   - the JDWP identifier of the type <code>ObjectID</code>
         *                     defining corresponding <code>jvmObject</code> to be 
         *                     disposed
         * @param refCount   - the count defining how many times the debugger, 
         *                     or some of its components, received the given
         *                     <code>ObjectID</code> from the JDWP agent as 
         *                     a part of the reply data in the reply packet.
         *                     Careful calculation of this count both on the part 
         *                     of the debugger and on the part of the JDWP agent 
         *                     enables to avoid a situation when the used  
         *                     <code>ObjectID</code> can be disposed.
         */
        void DisposeObject(JNIEnv* JNIEnvPtr, ObjectID objectID, jint refCount)
             throw ();

        /** 
         * Increases the count of references to given <code>ObjectID</code> 
         * defining how many times the given <code>ObjectID</code> was sent 
         * to the debugger by the JDWP agent as a part of the reply data 
         * in the reply packet. This count is used for correct
         * disposing of given <code>objectID</code>.
         *
         * @param objectID       - the JDWP identifier of the type <code>ObjectID</code>
         * @param incrementValue - the integer value to increase the count.
         *                         It is not required, default value is 1.
         *
         * @return Returns the new value of the references count after
         *         increasing.
         */
        jint IncreaseIDRefCount(ObjectID objectID, jint incrementValue = 1)
            throw ();

    // =========================================================================
        // Mapping: <code>ReferenceTypeID</code> <-> <code>jclass</code> (=> <code>jobject</code>)
        // Includes JDWP types: <code>referenceTypeID</code>, <code>classID</code>,
        // <code>interfaceID</code>, <code>arrayTypeID</code>.

        /** 
         * Maps the JVM object of the type <code>jclass</code> to the JDWP 
         * identifier of the type <code>ReferenceTypeID</code>, which includes 
         * the following JDWP types: <code>referenceTypeID</code>,
         * <code>classID</code>, <code>interfaceID</code>, <code>arrayTypeID</code>.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param jvmClass  - the JVM <code>jclass</code> object to be mapped
         *
         * If its value is <code>NULL</code>, the <code>JDWP_OBJECT_ID_NULL</code> 
         * value is returned.
         */
 
        /**
         * IMPORTANT! Please take into account that the <code>NewWeakGlobalRef</code> 
         * is requested for passed <code>jvmClass</code> and it is used in the future.
         * That is the MapFromReferenceTypeID() function returns this 
         * <code>NewWeakGlobalRef</code>.
         * The caller's responsibility is to deal with passed <code>jvmClass</code>, 
         * that is to delete if it is a global ref.
         *
         * @return Returns the JDWP identifier of the type <code>ReferenceTypeID</code>
         *         to which <code>jvmClass</code> is mapped. 
         *         The <code>JDWP_OBJECT_ID_NULL</code> value can be returned, if the 
         *         passed <code>jvmClass</code> has a value <code>NULL</code>.
         *
         * @exception OutOfMemoryException is the same as 
         *            <code>AgentException(JDWP_ERROR_OUT_OF_MEMORY)</code> - if 
         *            out-of-memory error has occurred during execution of the given 
         *            function.
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        ReferenceTypeID MapToReferenceTypeID(JNIEnv* JNIEnvPtr, jclass jvmClass)
            throw (AgentException);

        /** 
         * Maps the JDWP identifier of the type <code>ReferenceTypeID</code> to 
         * the JVM object of the type <code>jclass</code>. 
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param refTypeID - the JDWP identifier of the type 
         *                    <code>ReferenceTypeID</code> to be mapped
         *
         * @return Returns the <code>jvmObject</code> of the type 
         *         <code>jclass</code> to which <code>refTypeID</code> is mapped.
         *
         * @exception AgentException(JDWP_ERROR_INVALID_OBJECT) - if the
         *            passed <code>refTypeID</code> is not a known ID.
         * @exception AgentException JDWP_ERROR_INVALID_CLASS - if the 
         *            passed <code>refTypeID</code> is not the ID of a reference 
         *            type or a corresponding JVM <code>jclass</code> has been unloaded and 
         *            garbage-collected.
         */
        jclass MapFromReferenceTypeID(JNIEnv* JNIEnvPtr, ReferenceTypeID refTypeID)
            throw (AgentException);

    // =========================================================================
        // Mapping: <code>FieldID</code> <-> <code>jfieldID</code>
        // Includes JDWP types: <code>fieldID</code>

        /** 
         * Maps the JVM identifier of the type <code>jfieldID</code> to the JDWP
         * identifier of the type <code>FieldID</code> including the JDWP 
         * type <code>fieldID</code>.
         *
         * @param JNIEnvPtr  - the JNI interface pointer used to call
         *                     necessary JNI functions
         * @param jvmFieldID - the JVM identifier of the type <code>jfieldID</code>
         *                     to be mapped
         *
         * @return Returns the JDWP identifier of the type <code>FieldID</code>
         *         to which <code>jvmFieldID</code> is mapped.
         */
        FieldID MapToFieldID(JNIEnv* JNIEnvPtr, jfieldID jvmFieldID) throw ();

        /** 
         * Maps the JDWP identifier of the type <code>FieldID</code> to the 
         * JVM identifier of the type <code>jfieldID</code>.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param fieldID   - the JDWP identifier of the type <code>FieldID</code> 
         *                    to be mapped
         *
         * @return Returns the JVM identifier of the type <code>jfieldID</code>
         *         to which <code>fieldID</code> is mapped.
         */
        jfieldID MapFromFieldID(JNIEnv* JNIEnvPtr, FieldID fieldID) throw ();

    // =========================================================================
        // Mapping: <code>MethodID</code> <-> <code>jmethodID</code>
        // Includes JDWP types: <code>methodID</code>

        /** 
         * Maps the JVM identifier of the type <code>jmethodID</code> to 
         * the JDWP identifier of the type <code>MethodID</code> including 
         * the JDWP type <code>methodID</code>.
         *
         * @param JNIEnvPtr   - the JNI interface pointer used to call
         *                      necessary JNI functions
         * @param jvmMethodID - the JVM identifier of the type <code>jmethodID</code>
         *                      to be mapped
         *
         * @return Returns the JDWP identifier of the type <code>MethodID</code> to which
         *         <code>jvmMethodID</code> is mapped.
         */
        MethodID MapToMethodID(JNIEnv* JNIEnvPtr, jmethodID jvmMethodID)
            throw ();

        /** 
         * Maps the JDWP identifier of the type <code>MethodID</code> to the 
         * JVM identifier of the type <code>jmethodID</code>.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param methodID  - the JDWP identifier of the type <code>MethodID</code>
         *                    to be mapped
         *
         * @return Returns the JVM identifier of the type <code>jmethodID</code> to 
         *         which methodID is mapped to.
         */
        jmethodID MapFromMethodID(JNIEnv* JNIEnvPtr, MethodID methodID)
            throw ();

    // =========================================================================
        // Mapping: <code>FrameID</code> <-> <code>jthread</code> + <code>depth (jint)</code>
        // Includes JDWP types: <code>frameID</code>
        // The <code>FrameID</code> uniquely identifies the frame within the 
        // entire VM 

        /** 
         * Maps the stack frame of the JVM thread to the JDWP identifier of the type
         * <code>FrameID</code> including the JDWP type <code>frameID</code>.
         *
         * @param JNIEnvPtr    - the JNI interface pointer used to call
         *                       necessary JNI functions
         * @param jvmThread    - the JVM object of the type <code>jthread</code> 
         *                       defining the JVM thread, the stack frame of  
         *                       which has to be mapped. The object is supposed  
         *                       to be a JNI global reference, so new weak global 
         *                       reference is created and stored. It is caller's 
         *                       responsibility to delete passed global reference.
         * @param frameDepth   - the integer value defining the index of the stack
         *                       frame to be mapped
         * @param framesCount  - the integer value defining the current number of
         *                       all stack frames for given <code>jvmThread</code>
         *
         * @return Returns the JDWP identifier of the type <code>FrameID</code> 
         *         to which the stack frame is mapped.
         *
         * @exception OutOfMemoryException is the same as 
         *            <code>AgentException(JDWP_ERROR_OUT_OF_MEMORY)</code> - if 
         *            out-of-memory error has occurred during execution of the 
         *            given function.
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         * @exception AgentException(JDWP_ERROR_INVALID_LENGTH) - if the 
         *            passed <code>frameDepth</code> is out of bounds of the 
         *            current stack frames.
         */
        FrameID MapToFrameID(JNIEnv* JNIEnvPtr, jthread jvmThread,
                             jint frameDepth, jint framesCount)
            throw (AgentException);

        /** 
         * Maps the JDWP identifier of the type <code>FrameID</code> to the 
         * stack frame of the JVM thread.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         * @param frameID   - the JDWP identifier of the type <code>FrameID</code>
         *                    to be mapped to the stack frame
         *
         * @return Returns the integer value defining the index of the stack
         *                 frame to which <code>FrameID</code> is mapped.
         *
         * @exception AgentException(JDWP_ERROR_INVALID_FRAMEID) - 
         *            if the passed <code>FrameID</code> is not a valid JDWP 
         *            identifier, that is it does not identify any stack frame 
         *            at the current moment.
         */
        jint MapFromFrameID(JNIEnv* JNIEnvPtr, FrameID frameID)
            throw (AgentException);

        /** 
         * Deletes all JDWP frames' identifiers registered at present for the 
         * given JVM thread. Any subsequent references to the deleted 
         * <code>FrameIDs</code> will cause the <code>JDWP_ERROR_INVALID_FRAMEID</code> 
         * error. The given function has to be called when the JDWP command 
         * <code>Resume</code> for given thread is executed.
         *
         * @param JNIEnvPtr  - the JNI interface pointer used to call
         *                     necessary JNI functions
         * @param jvmThread  - the JVM object of the type <code>jthread</code> 
         *                     defining the JVM thread, which <code>FrameIDs</code> 
         *                     have to be deleted for
         */
        void DeleteFrameIDs(JNIEnv* JNIEnvPtr, jthread jvmThread)
            throw ();

    // =========================================================================
        /** 
         * Initializes the fields of the ObjectManager class used 
         * for mapping between JDWP IDs and corresponding JVMTI, JNI data types.
         * Calls the following corresponding functions: 
         * InitObjectIDMap(), InitFrameIDMap().
         * Creates <code>AgentMonitor</code> instances used for synchronization 
         * of access to the ObjectManager data.
         *
         * @param JNIEnvPtr  - the JNI interface pointer used to call
         *                     necessary JNI functions
         *
         * @exception AgentException(jvmtiError) that 
         *            may be thrown by the <code>AgentMonitor</code> constructor.
         */
        void Init(JNIEnv* JNIEnvPtr) throw (AgentException);

        /** 
         * Releases resources of the ObjectManager class instance 
         * not related to JVM. Prepares ObjectManager for the 
         * repeated using. Calls the following corresponding functions: 
         * <code>ResetObjectIDMap</code>(<code>JNIEnvPtr</code>),
         * ResetFrameIDMap().
         *
         * @param JNIEnvPtr  - the JNI interface pointer used to call
         *                     necessary JNI functions
         *
         * @exception InternalErrorException 
         *            is the same as <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        void Reset(JNIEnv* JNIEnvPtr) throw (AgentException);
 
        /** 
         * Releases resources of the ObjectManager class instance  
         * related to JVM, in particular deletes <code>AgentMonitor(s)</code>.
         * Prepares ObjectManager for "death".
         *
         * @param JNIEnvPtr  - the JNI interface pointer used to call
         *                     necessary JNI functions
         */
        void Clean(JNIEnv* JNIEnvPtr) throw ();

        /** 
         * Constructor for the ObjectManager class instance.
         */
        ObjectManager() throw ();

        /** 
         * Destructor for the ObjectManager class instance.
         */
        ~ObjectManager () throw ();


    // =========================================================================
    // =========================================================================
        // Internal data, structures, functions 

    private:
    // =========================================================================
        // Mapping: <code>ObjectID</code> <-> <code>jobject</code>

        /** 
         * The structure describing the data item part in the 
         * <code>ObjectID</code> values table when the given item describes real 
         * <code>ObjectID</code> to which some JVM objects are mapped.
         * Fields:
         * - <code>globalRefKind</code>   - defines the JNI global reference 
         *                                  kind: weak/normal;
         * - <code>jvmObject</code>       - mapped JVM object;
         * - <code>referencesCount</code> - the count of <code>ObjectID</code> 
         *                                  references defining how many times 
         *                                  the given <code>ObjectID</code>
         *                                  was sent to the debugger by the JDWP 
         *                                  agent as a part of the reply data in 
         *                                  the reply packet. This count is used for 
         *                                  the correct disposing of <code>ObjectID</code>.
         */
        struct MapObjectIDItem {
            jshort globalRefKind;
            jobject jvmObject;  
            jint referencesCount; // the count of references held by back-end
        };

        /** 
         * The structure describing the full data item in the 
         * <code>ObjectID</code> values table. 
         * Fields:
         * - <code>objectID</code>             - the value of the <code>ObjectID</code> 
         *                                       JDWP identifier. It may be a 
         *                                       special value as a sign that 
         *                                       given ObjectIDItem defines a free item;
         * - <code>mapObjectIDItem</code>      - the item describing info for real 
         *                                       <code>ObjectID</code>. For more 
         *                                       information on it refer to the 
         *                                       <code>MapObjectIDItem</code> structure;
         * - <code>nextFreeObjectIDItem</code> - the reference to next free ObjectIDItem 
         *                                       if given ObjectIDItem  defines a free item.
         */
        struct ObjectIDItem {
            ObjectID objectID;
            union {
                MapObjectIDItem mapObjectIDItem; 
                ObjectIDItem* nextFreeObjectIDItem;
            };
        };

        /** 
         * The constant field defining the ObjectIDItem  
         * structure size.
         */
        static const size_t OBJECTID_ITEM_SIZE = sizeof(ObjectIDItem);

        /** 
         * The field defining the <code>ObjectID</code> values tables current sizes.
         */
        size_t          m_objectIDTableSize[HASH_TABLE_SIZE];

        /** 
         * The field defining the current maximal allocated 
         * <code>ObjectID</code> in each hash buffer.
         */
        ObjectID        m_maxAllocatedObjectID[HASH_TABLE_SIZE];

        /** 
         * The field defining the current addresses of the 
         * <code>ObjectID</code> values tables.
         */
        ObjectIDItem*   m_objectIDTable[HASH_TABLE_SIZE];

        /** 
         * The field defining current addresses of the first free items in the
         * <code>ObjectID</code> values tables.
         */
        ObjectIDItem*   m_freeObjectIDItems[HASH_TABLE_SIZE];

        /** 
         * The field defining Monitor used for synchronization of the 
         * <code>ObjectID</code> values table access and to fields describing 
         * state of this table.
         */
        AgentMonitor    *m_objectIDTableMonitor;

        /**
         * Expands the table of <code>ObjectID</code> values if no free items for new 
         * <code>ObjectID</code> exist in the table. 
         * The given function is called by the MapToNewObjectID() 
         * function.
         *
         * @exception OutOfMemoryException is the same as 
         *            <code>AgentException(JDWP_ERROR_OUT_OF_MEMORY)</code> - if 
         *            out-of-memory error has occurred during execution of the 
         *            given function.
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        void ExpandObjectIDTable() throw (AgentException);

        /** 
         * Maps the JVM object of the type <code>jobject</code> to the JDWP 
         * identifier of the type <code>ObjectID</code> when the JVM object 
         * is not mapped yet.
         * The given function is called by the MapToObjectID() 
         * function.
         *
         * @param JNIEnvPtr  - the JNI interface pointer used to call
         *                     necessary JNI functions
         * @param jvmObject  - the JVM object to be mapped
         *
         * @return Returns the JDWP identifier of the type <code>ObjectID</code> 
         *         to which <code>jvmObject</code> is mapped.
         * @exception OutOfMemoryException is the same as 
         *            <code>AgentException(JDWP_ERROR_OUT_OF_MEMORY)</code> - if 
         *            out-of-memory error has occurred during execution of the given
         *            function.
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        ObjectID MapToNewObjectID(JNIEnv* JNIEnvPtr, jobject jvmObject)
            throw (AgentException);

        /** 
         * Checks if the passed <code>ObjectID</code> JDWP identifier is valid
         * <code>ObjectID</code>, that is currently it represents in the JDI 
         * level the JVM object of the <code>jobject</code> type.
         * 
         * @param objectID  - the JDWP identifier of the type 
         *                    <code>ObjectID</code> to be checked
         *
         * @return Returns the <code>JNI_TRUE</code> value if the passed 
         *         <code>objectID</code> is valid, <code>JNI_FALSE</code> 
         *         otherwise.
         */
        jboolean IsValidObjectID(ObjectID objectID) throw ();

        /** 
         * Initializes the fields of the ObjectManager class used 
         * for <code>ObjectID</code> values mapping. It is called by the 
         * Init() function.
         */
        void InitObjectIDMap() throw ();

        /** 
         * Disposes all allocated <code>ObjectID</code> values and releases memory 
         * from under the <code>ObjectID</code> values table. Repeatedly initializes the 
         * fields used for <code>ObjectID</code> values mapping, preparing 
         * ObjectManager for the repeated using.
         * It is called by the Reset() function.
         *
         * @param JNIEnvPtr  - the JNI interface pointer used to call
         *                     necessary JNI functions
         *
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        void ResetObjectIDMap(JNIEnv* JNIEnvPtr) throw (AgentException);

    // =========================================================================
        // Mapping: <code>ReferenceTypeID</code> <-> <code>jclass</code>

        /** 
         * The field defines the allocated buffers size for reference mapping.
         */
        size_t      m_refTypeIDTableSize[HASH_TABLE_SIZE];

        /** 
         * The field defines a used portion of buffers for reference mapping.
         */
        size_t      m_refTypeIDTableUsed[HASH_TABLE_SIZE];

        /** 
         * The field defines a hash table for reference mapping buffers.
         */
        jclass*     m_refTypeIDTable[HASH_TABLE_SIZE];

        /** 
         * The field defining Monitor is used for synchronization of the
         * <code>ReferenceTypeIDs</code> table access and to fields describing 
         * state of the table.
        */
        AgentMonitor *m_refTypeIDTableMonitor;

        /** 
         * Initializes the fields of the ObjectManager class used
         * for <code>ReferenceTypeIDs</code> mapping. It is called by the 
         * Init() function.
         */
        void InitRefTypeIDMap() throw ();

        /** 
         * Deletes all weak Global References for allocated 
         * <code>ReferenceTypeIDs</code> and releases memory from under the table
         * of <code>ReferenceTypeIDs</code>. Repeatedly initializes the fields used 
         * for <code>ReferenceTypeIDs</code> mapping, preparing 
         * ObjectManager for the repeated using.
         * It is called by the Reset() function.
         *
         * @param JNIEnvPtr  - the JNI interface pointer used to call
         *                     necessary JNI functions
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        void ResetRefTypeIDMap(JNIEnv* JNIEnvPtr) throw (AgentException);

    // =========================================================================
        // Mapping: <code>FrameID</code> <-> <code>jthread</code> + <code>depth (jint)</code>

        /** 
         * The structure describing the data item in the <code>FrameIDs</code> 
         * table.
         * Fields:
         * - <code>jvmThread</code>           - the JVM object of the type 
         *                                      <code>jthread</code> defining a JVM 
         *                                      thread to which the given ThreadFramesItem
         *                                      corresponds;
         * - <code>currentFrameID</code>      - the value of the JDWP identifier of the type 
         *                                      <code>FrameID</code>, which corresponds 
         *                                      to the topmost stack frame of the 
         *                                      <code>jvmThread</code> stack;
         * - <code>framesCountOfThread</code> - the number of all stack frames 
         *                                      currently in the <code>jvmThread</code>
         *                                      call stack. It may be a special value 
         *                                      as a sign that ThreadFramesItem describes 
         *                                      free item in the table of FrameIDs.
         */
        struct ThreadFramesItem {
            jthread jvmThread; 
            FrameID currentFrameID; // for frame with depth = 0
            jint framesCountOfThread;  
        };

        /** 
         * The constant field defining the ThreadFramesItem 
         * structure size.
         */
        static const size_t THREAD_FRAMES_ITEM_SIZE = sizeof(ThreadFramesItem);

        /** 
         * The field defining the current size of the <code>FrameIDs</code> 
         * table.
         */
        jlong m_frameIDTableSize; 

        /** 
         * The field defining the current number of free items in the 
         * <code>FrameIDs</code> table.
         */
        jlong m_freeItemsNumberInFrameIDTable; 

        /** 
         * The field defining the current address of the <code>FrameIDs</code> 
         * table.
         */
        ThreadFramesItem* m_frameIDTable;

        /** 
         * The field defining the current maximal allocated <code>FrameID</code>.
         */
        FrameID m_maxAllocatedFrameID;

        /** 
         * The field defining Monitor used for synchronization of 
         * <code>FrameIDs</code> table access and to fields describing state of the 
         * table.
         */
        AgentMonitor *m_frameIDTableMonitor;

        /**
         * Expands the <code>FrameIDs</code> table, if there no free items in 
         * table exist.
         * The given function is called by the NewThreadFramesItem()
         * function.
         * 
         * @return Returns the reference to the first free 
         *         ThreadFramesItem.
         *
         * @exception OutOfMemoryException is the same as 
         *            <code>AgentException(JDWP_ERROR_OUT_OF_MEMORY)</code> - if 
         *            out-of-memory error has occurred during execution of the given 
         *            function.
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        ThreadFramesItem* ExpandThreadFramesTable() throw (AgentException);

        /** 
         * Allocates the new ThreadFramesItem to map the stack 
         * frame of <code>jvmThread</code> when stack frames of given 
         * <code>jvmThread</code> are not mapped yet. The given function is 
         * called by the MapToFrameID() function.
         *
         * @param JNIEnvPtr   - the JNI interface pointer used to call
         *                      necessary JNI functions
         * @param jvmThread   - the JVM object of the type <code>jthread</code> 
         *                      defining a JVM thread the stack frame of which 
         *                      has to be mapped
         * @param framesCount - the integer value defining the current number of
         *                      all stack frames for given <code>jvmThread</code>
         *
         * @return Returns the reference to the new allocated 
         *         ThreadFramesItem.
         *
         * @exception OutOfMemoryException is the same as 
         *            <code>AgentException(JDWP_ERROR_OUT_OF_MEMORY)</code> - if 
         *            out-of-memory error has occurred during execution of the given 
         *            function.
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        ThreadFramesItem* NewThreadFramesItem(JNIEnv* JNIEnvPtr, 
            jthread jvmThread, jint framesCount)
            throw (AgentException);

        /** 
         * Initializes the fields of the ObjectManager class used for 
         * <code>FrameIDs</code> mapping. It is called by the Init() function.
         */
        void InitFrameIDMap() throw ();

        /** 
         * Releases memory from under the table of <code>FrameIDs</code>. 
         * Repeatedly initializes the fields used for <code>FrameIDs</code> 
         * mapping, preparing ObjectManager for the repeated using. It is called by  
         * the Reset() function.
         *
         * @param JNIEnvPtr - the JNI interface pointer used to call
         *                    necessary JNI functions
         *
         * @exception InternalErrorException is the same as 
         *            <code>AgentException(JDWP_ERROR_INTERNAL)</code> - if an 
         *            unexpected internal JDWP agent error has occurred.
         */
        void ResetFrameIDMap(JNIEnv* JNIEnvPtr) throw (AgentException);
    // =========================================================================

    // =========================================================================
    // Next for debugging only

    private:
#ifndef NDEBUG
        ThreadFramesItem m_DummyFrameIDTable;
#endif // NDEBUG

        /**
         * Returns a hash code for a JVM object.
         * @param[in] object - the JVM object for which hash will be returned
         * @param[out] hash_code_ptr - the object hash code 
         */
        jvmtiError GetObjectHashCode(jobject object, jint *hash_code_ptr);

    }; // ObjectManager class

} // namespace jdwp

#endif // _OBJECT_MANAGER_H
