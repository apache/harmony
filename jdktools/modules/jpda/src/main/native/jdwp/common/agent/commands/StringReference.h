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
 * StringReference.h
 *
 */

#ifndef _STRING_REFERENCE_H_
#define _STRING_REFERENCE_H_

#include "AgentException.h"
#include "CommandHandler.h"

namespace jdwp {

    /**
     * The namespace includes declaration of the classes implementing commands
     * from the StringReference command set.
     */
    namespace StringReference {

        /**
         * The class implements the <code>Superclass</code> command from the
         * StringReference command set.
         */
        class ValueHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Value</code> JDWP command for the
             * StringReference command set.
             *
             * @param jni - the JNI interface pointer
             *
             * @exception If packet data integrity gets broken,
             *            <code>InternalErrorException</code> is thrown.
             * @exception If the system runs out of memory,
             *            <code>OutOfMemoryException</code> is thrown.
             *
             * following @exception The following implementations of the given interface
             *                      may throw <code>AgentException</code>: 
             *                      <code>INVALID_STRING</code>, 
             *                      <code>INVALID_OBJECT</code>, <code>VM_DEAD</code>.
             */
            virtual int Execute(JNIEnv *jni);

        }; //ValueHandler

    }; // StringReference

} //jdwp

#endif //_STRING_REFERENCE_H_
