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
 * @author Viacheslav G. Rybalov, Anton V. Karnachuk
 */

/**
 * @file
 * ClassType.h
 *
 */

#ifndef _CLASS_TYPE_H_
#define _CLASS_TYPE_H_

#include "AgentException.h"
#include "CommandHandler.h"

namespace jdwp {

    /**
     * The namespace includes declaration of the classes implementing commands
     * from the <code>ClassType</code> command set.
     */
    namespace ClassType {

        /**
         * The class implements the <code>Superclass</code> command from the
         * <code>ClassType</code> command set.
         */
        class SuperClassHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>SuperClass</code> JDWP command for the
             * <code>ClassType</code> command set.
             *
             * @param jni - the JNI interface pointer
             *
             * @exception <code>InternalErrorException</code> is thrown, if
             *            the packet data integrity gets broken.
             * @exception <code>OutOfMemoryException</code> is thrown, if
             *            the system runs out of memory.
             *
             * following @exception The following implementations of the given 
             *                      interface may throw <code>AgentException</code>:
             *                      INVALID_CLASS, INVALID_OBJECT, VM_DEAD
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; //SuperClassHandler

        /**
         * The class implements the <code>SetValues</code> command from the
         * <code>ClassType</code> command set.
         */
        class SetValuesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>SetValues</code> JDWP command for the
             * <code>ClassType</code> command set.
             *
             * @param jni - the JNI interface pointer
             *
             * @exception  If the packet data integrity gets broken,
             *             <code>InternalErrorException</code> is thrown.
             * @exception  If the system runs out of memory,
             *             <code>OutOfMemoryException</code> is thrown.
             *
             * following @exception The following implementations of the given interface 
             *                      may throw <code>AgentException</code>: INVALID_CLASS, 
             *                      CLASS_NOT_PREPARED,INVALID_OBJECT, INVALID_FIELDID, 
             *                      VM_DEAD.
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; //SetValuesHandler

        /**
         * The class implements the <code>InvokeMethod</code> command from the
         * <code>ClassType</code> command set.
         */
        class InvokeMethodHandler : public SpecialAsyncCommandHandler {
        public:

            /**
             * The given method makes deferred checks and executes 
             * <code>InvokeMethod</code>.
             * It should be called from the appropriate thread.
             */
            virtual void ExecuteDeferredFunc(JNIEnv *jni);

            /**
             * Gets the thread name for asynchronous execution of the current command handler.
             *
             * @return Thread name string.
             */
            virtual const char* GetThreadName();

        protected:

            /**
             * Executes the <code>InvokeMethod</code> JDWP command for the
             * <code>ClassType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        private:
            jclass m_clazz;
            jmethodID m_methodID;
            jvalue* m_methodValues;

            jdwpTaggedValue m_returnValue;
            jthrowable m_returnException;

        };//InvokeMethodHandler

        /**
         * The class implements the <code>NewInstance</code> command from the
         * <code>ClassType</code> command set.
         */
        class NewInstanceHandler : public SpecialAsyncCommandHandler {
        public:

            /**
             * The given method makes deferred checks. It executes 
             * <code>NewInstance</code>.
             * It should be called from the appropriate thread.
             */
            virtual void ExecuteDeferredFunc(JNIEnv *jni);

            /**
             * Gets the thread name for asynchronous execution of the current command handler.
             *
             * @return Thread name string.
             */
            virtual const char* GetThreadName();

        protected:

            /**
             * Executes the <code>NewInstance</code> JDWP command for the
             * <code>ClassType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        private:
            jclass m_clazz;
            jmethodID m_methodID;
            jvalue* m_methodValues;

            jobject m_returnValue;
            jthrowable m_returnException;

        };//NewInstanceHandler

    }; // ClassType

} //jdwp

#endif //_CLASS_TYPE_H_
