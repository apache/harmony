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
 * EventRequest.h
 *
 */

#ifndef _EVENT_REQUEST_H_
#define _EVENT_REQUEST_H_

#include "AgentException.h"
#include "CommandHandler.h"

namespace jdwp {

    /**
     * The namespace includes declaration of the classes implementing commands
     * from the <code>EventRequest</code> command set.
     */
    namespace EventRequest {

        /**
         * The class implements the <code>Set</code> command from the
         * <code>EventRequest</code> command set.
         */
        class SetHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Set</code> JDWP command for the
             * <code>EventRequest</code> command set.
             *
             * @param jni - the JNI interface pointer
             *
             * @exception If the packet data integrity gets broken,
             *            <code>InternalErrorException</code> is thrown. 
             * @exception <code>NotImplementedException</code> is thrown if
             *            <code>modKind = 2</code>, that is for the future.
             * @exception If the system runs out of memory,
             *            <code>OutOfMemoryException</code> is thrown.
             * @exception The implementations of the given interface
             *            may throw <code>AgentException</code>.
             */
            virtual int Execute(JNIEnv *jni) ;

        };//SetHandler

        /**
         * The class implements the <code>Clear</code> command from the
         * <code>EventRequest</code> command set.
         */
        class ClearHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Clear</code> JDWP command for the
             * <code>EventRequest</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni) ;

        };//ClearHandler

        /**
         * The class implements the <code>ClearAllBreakpoints</code> command
         * from the <code>EventRequest</code> command set.
         */
        class ClearAllBreakpointsHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Clear</code> JDWP command for the
             * <code>EventRequest</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni) ;

        };//ClearHandler

    } // EventRequest

} //jdwp

#endif //_EVENT_REQUEST_H_
