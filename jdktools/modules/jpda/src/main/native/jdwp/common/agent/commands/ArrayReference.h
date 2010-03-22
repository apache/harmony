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
 * ArrayReference.h
 *
 */

#ifndef _ARRAYREFERENCE_H_
#define _ARRAYREFERENCE_H_

#include "AgentException.h"
#include "CommandHandler.h"

namespace jdwp {

    /**
     * The namespace includes declaration of the classes implementing commands
     * from the <code>ArrayReference</code> command set.
     */
    namespace ArrayReference {

        /**
         * The class implements the <code>Length</code> command from the
         * <code>ArrayReference</code> command set.
         */
        class LengthHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Length</code> JDWP command for the
             * <code>ArrayReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//LengthHandler

        /**
         * The class implements the <code>GetValues</code> command from the
         * <code>ArrayReference</code> command set.
         */
        class GetValuesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>GetValues</code> JDWP command for the
             * <code>ArrayReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//GetValuesHandler

        /**
         * The class implements the <code>SetValues</code> command from the
         * <code>ArrayReference</code> command set.
         */
        class SetValuesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>SetValues</code> JDWP command for the
             * <code>ArrayReference</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//SetValuesHandler

    } // ArrayReference

} //jdwp

#endif //_ARRAYREFERENCE_H_
