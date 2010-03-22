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
 * @file
 * ObjectReference.h
 *
 */

#ifndef _OBJECT_REFERENCE_H_
#define _OBJECT_REFERENCE_H_

#include "AgentException.h"
#include "CommandHandler.h"
#include "ClassManager.h"
#include "ThreadManager.h"

namespace jdwp {

    /**
     * The namespace includes declaration of the classes implementing commands
     * from the <code>ObjectReference</code> command set.
     */
    namespace ObjectReference {
        
    // =========================================================================
        /**
         * The class implements the <code>ReferenceType (1)</code>
         * command from the <code>ObjectReference</code> command set.
         */
        class ReferenceTypeHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ReferenceType</code> JDWP command for the
             * <code>ObjectReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        }; // ReferenceTypeHandler class

    // =========================================================================
        /**
         * The class implements the <code>GetValues (2)</code>
         * command from the <code>ObjectReference</code> command set.
         */
        class GetValuesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>GetValues</code> JDWP command for the
             * <code>ObjectReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        }; // GetValuesHandler class

    // =========================================================================
        /**
         * The class implements the <code>SetValues (3) </code>
         * command from the <code>ObjectReference</code> command set.
         */
        class SetValuesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>SetValues</code> JDWP command for the
             * <code>ObjectReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        }; // SetValuesHandler class

    // =========================================================================
        /**
         * The class implements the <code>MonitorInfo (5)</code>
         * command from the <code>ObjectReference</code> command set.
         */
        class MonitorInfoHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>MonitorInfo</code> JDWP command for the
             * <code>ObjectReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        }; // MonitorInfoHandler class

    // =========================================================================
        /**
         * The class implements the <code>DisableCollection (7)</code>
         * command from the <code>ObjectReference</code> command set.
         */
        class DisableCollectionHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>DisableCollection</code> JDWP command for the
             * <code>ObjectReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        }; // DisableCollectionHandler class

    // =========================================================================
        /**
         * The class implements the <code>EnableCollection (8) </code>
         * command from the <code>ObjectReference</code> command set.
         */
        class EnableCollectionHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>EnableCollection</code> JDWP command for the
             * <code>ObjectReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        }; // EnableCollectionHandler class

    // =========================================================================
        /**
         * The class implements the <code>IsCollected (9) </code>
         * command from the <code>ObjectReference</code> command set.
         */
        class IsCollectedHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>IsCollected</code> JDWP command for the
             * <code>ObjectReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        }; // IsCollectedHandler class

    // =========================================================================
        /**
         * The class implements the <code>InvokeMethod</code> command from the
         * <code>ObjectReference</code> command set.
         */
        class InvokeMethodHandler : public SpecialAsyncCommandHandler {
        public:

            /**
             * The given method makes deferred checks, executes 
             * <code>InvokeMethod</code> and should be called from 
             * the appropriate thread.
             */
            virtual void ExecuteDeferredFunc(JNIEnv *jni);

            /**
             * Gets a thread name for asynchronous execution of the given command handler.
             *
             * @return Thread name string.
             */
            virtual const char* GetThreadName();

        protected:

            /**
             * Executes the <code>InvokeMethod</code> JDWP command for the
             * <code>ObjectReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        private:
            jclass m_clazz;
            jobject m_object;
            jmethodID m_methodID;
            jvalue* m_methodValues;

            jdwpTaggedValue m_returnValue;
            jthrowable m_returnException;

        };//InvokeMethodHandler

         //New commands for Java 6
        // =========================================================================
        /**
         * The class implements the <code>ReferringObjects (10) </code>
         * command from the <code>ObjectReference</code> command set.
         */
        class ReferringObjectsHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ReferringObjects</code> JDWP command for the
             * <code>ObjectReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        }; // ReferringObjectsHandler class

    } // ObjectReference namespace

} // jdwp namespace

#endif //_OBJECT_REFERENCE_H_
