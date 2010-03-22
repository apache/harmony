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
 * ThreadGroupReference.h
 *
 */

#ifndef _THREAD_GROUP_REFERENCE_H_
#define _THREAD_GROUP_REFERENCE_H_

#include "AgentException.h"
#include "CommandHandler.h"

namespace jdwp {

    /**
     * The namespace includes the declaration of the classes implementing 
     * commands from the ThreadGroupReference command set.
     */
    namespace ThreadGroupReference {
        
        /**
         * The class implements the <code>Name</code> command from the
         * ThreadGroupReference command set.
         */
        class NameHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Name</code> JDWP command for the
             * ThreadGroupReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni) ;

        };//NameHandler

        /**
         * The class implements the <code>Parent</code> command from the
         * ThreadGroupReference command set.
         */
        class ParentHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Parent</code> JDWP command for the
             * ThreadGroupReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni) ;

        };//ParentHandler

        /**
         * The class implements the <code>Children</code> command from the
         * ThreadGroupReference command set.
         */
        class ChildrenHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Children</code> JDWP command for the
             * ThreadGroupReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni) ;

        };//ChildrenHandler

    }//ThreadGroupReference

}//jdwp

#endif //_THREAD_GROUP_REFERENCE_H_

