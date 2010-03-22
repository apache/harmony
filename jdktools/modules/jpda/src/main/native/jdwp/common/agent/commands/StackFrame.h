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

/**
 * @file
 * StackFrame.h
 *
 */

#ifndef _STACKFRAME_H_
#define _STACKFRAME_H_

#include "AgentException.h"
#include "CommandHandler.h"

namespace jdwp {

    /**
     * The namespace includes declaration of the classes implementing commands
     * from the StackFrame command set.
     */
    namespace StackFrame {

        /**
         * The class implements the <code>GetValues</code> command from the
         * StackFrame command set.
         */
        class GetValuesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>GetValues</code> JDWP command for the
             * StackFrame command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//GetValuesHandler

        /**
         * The class implements the <code>SetValues</code> command from the
         * StackFrame command set.
         */
        class SetValuesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>SetValues</code> JDWP command for the
             * StackFrame command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//SetValuesHandler

        /**
         * The class implements the <code>ThisObject</code> command from the
         * StackFrame command set.
         */
        class ThisObjectHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ThisObject</code> JDWP command for the
             * StackFrame command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        private:
            void CheckErr(jvmtiError err) throw(AgentException);
        };//ThisObjectHandler

        /**
         * The class implements the <code>PopFrames</code> command from the
         * StackFrame command set.
         */
        class PopFramesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>PopFrames</code> JDWP command for the
             * StackFrame command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//PopFramesHandler

    } // StackFrame

} //jdwp

#endif //_STACKFRAME_H_
