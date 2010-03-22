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
 * ReferenceType.h
 *
 */

#ifndef _REFERENCE_TYPE_H_
#define _REFERENCE_TYPE_H_

#include "AgentException.h"
#include "CommandHandler.h"

namespace jdwp {

    /**
     * The namespace includes declaration of the classes implementing commands
     * from the <code>ReferenceType</code> command set.
     */
    namespace ReferenceType {
        
    // =========================================================================
        /**
         * The class implements the <code>Signature (1)</code> command and 
         * it is the base class for the <code>SignatureWithGeneric (13)</code>
         * command, both from the <code>ReferenceType</code> command set.
         */
        class SignatureHandler : public SyncCommandHandler {
        public:

            /**
             * A constructor.
             */
            SignatureHandler() { m_withGeneric = false; }

        protected:

            /**
             * Executes the <code>Signature</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        protected:

            /**
             * The field defines if it is  the <code>Signature (1)</code> command or 
             * the <code>SignatureWithGeneric (13)</code> command.
             */
            bool m_withGeneric;

        }; // SignatureHandler class

    // =========================================================================
        /**
         * The class implements the <code>ClassLoader (2)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class ClassLoaderHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ClassLoader</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; // ClassLoaderHandler class

    // =========================================================================
        /**
         * The class implements the <code>Modifiers (3)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class ModifiersHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Modifiers</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer.
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; // ModifiersHandler class

    // =========================================================================
        /**
         * The class implements the <code>Fields (4)</code> command and 
         * it is the base class for the <code>FieldsWithGeneric (14)</code>
         * command, both from the <code>ReferenceType</code> command set.
         */
        class FieldsHandler : public SyncCommandHandler {
        public:

            /**
             * A constructor.
             */
            FieldsHandler() { m_withGeneric = false; }

        protected:

            /**
             * Executes the <code>Fields</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        protected:
        /**
         * The field defines if it is  the <code>Fields (4)</code> command or 
         * the <code>FieldsWithGeneric (14)</code> command.
         */
            bool m_withGeneric;
        }; // FieldsHandler class

    // =========================================================================
        /**
         * The class implements the <code>Methods (5)</code> command and 
         * it is the base class for the <code>MethodsWithGeneric (15)</code>
         * command, both from the <code>ReferenceType</code> command set.
         */
        class MethodsHandler : public SyncCommandHandler {
        public:

            /**
             * A constructor.
             */
            MethodsHandler() { m_withGeneric = false; }

        protected:

            /**
             * Executes the <code>Methods</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer.
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        protected:
        /**
         * The field defines if it is  the <code>Methods (5)</code> command or 
         * the <code>MethodsWithGeneric (15)</code> command.
         */
            bool m_withGeneric;
        }; // MethodsHandler class

    // =========================================================================
        /**
         * The class implements the <code>GetValues (6)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class GetValuesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>GetValues</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; // GetValuesHandler class

    // =========================================================================
        /**
         * The class implements the <code>SourceFile (7)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class SourceFileHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>SourceFile</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer.
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; // SourceFileHandler class

    // =========================================================================
        /**
         * The class implements the <code>NestedTypes (8)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class NestedTypesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>NestedTypes</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; // NestedTypesHandler class

    // =========================================================================
        /**
         * The class implements the <code>Status (9)</code> command from the
         * <code>ReferenceType</code> command set.
         */
        class StatusHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Status</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; // StatusHandler class

    // =========================================================================
        /**
         * The class implements the <code>Interfaces (10)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class InterfacesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Interfaces</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; // InterfacesHandler class

    // =========================================================================
        /**
         * The class implements the <code>ClassObject (11)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class ClassObjectHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ClassObject</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer.
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; // ClassObjectHandler class

    // =========================================================================
        /**
         * The class implements the <code>SourceDebugExtension (12) </code>
         * command from the <code>ReferenceType</code> command set.
         */
        class SourceDebugExtensionHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>SourceDebugExtension</code> JDWP command for the
             * <code>ReferenceType</code> command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        }; // SourceDebugExtensionHandler class

    // =========================================================================
        /**
         * The class implements the <code>SignatureWithGeneric (13)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class SignatureWithGenericHandler : public SignatureHandler {
        public:

            /**
             * A constructor.
             */
            SignatureWithGenericHandler() { m_withGeneric = true; }

        }; // SignatureWithGenericHandler class

    // =========================================================================
        /**
         * The class implements the <code>FieldsWithGeneric (14)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class FieldsWithGenericHandler : public FieldsHandler {
        public:

            /**
             * A constructor.
             */
            FieldsWithGenericHandler() { m_withGeneric = true; }

        }; // FieldsWithGenericHandler class

    // =========================================================================
        /**
         * The class implements the <code>MethodsWithGeneric (15)</code>
         * command from the <code>ReferenceType</code> command set.
         */
        class MethodsWithGenericHandler : public MethodsHandler {
        public:

            /**
             * A constructor.
             */
            MethodsWithGenericHandler() { m_withGeneric = true; }

        }; // MethodsWithGenericHandler class

    // =========================================================================

    } // ReferenceType namespace

} // jdwp namesoace

#endif //_REFERENCE_TYPE_H_
