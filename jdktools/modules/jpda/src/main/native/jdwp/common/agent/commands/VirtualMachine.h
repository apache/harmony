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
 * @author Vitaly A. Provodin
 */

/**
 * @file
 * VirtualMachine.h
 *
 */

#ifndef _VIRTUAL_MACHINE_H_
#define _VIRTUAL_MACHINE_H_

#include "AgentException.h"
#include "CommandHandler.h"

namespace jdwp {

    /**
     * The namespace includes declaration of the classes implementing commands
     * from the VirtualMachine command set.
     */
    namespace VirtualMachine {
        
        /**
         * The class implements the <code>Version</code> command from the
         * VirtualMachine command set.
         */
        class VersionHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Version</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };

        /**
         * The class implements the <code>ClassesBySignature</code> command from the
         * VirtualMachine command set.
         */
        class ClassesBySignatureHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ClassesBySignature</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        private:
            bool IsSignatureMatch(jclass klass, const char *signature)
                throw(AgentException);

        };//ClassesBySignatureHandler

        /**
         * The class implements the <code>AllClasses</code> command from the
         * VirtualMachine command set.
         */
        class AllClassesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>AllClasses</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

            /**
             * If the passed class (klass parameter) has status
             * JVMTI_CLASS_STATUS_PREPARED then writes class type tag,
             * reference id, signature and status
             * to the reply packet and reutrn 0 - success sign.
             * Otherwise (class not prepared) doesn't write class info
             * to the reply packet and reutrn 1 - unsuccess sign.
             *
             * @param jni   - the JNI interface pointer
             * @param jvmti - the JVMTI interface pointer
             * @param klass - the Java class
             *
             * @return 0 on success,
             *         1 otherwise.
             */
            virtual int Compose41Class(JNIEnv *jni, jvmtiEnv* jvmti, jclass klass)
                                            throw (AgentException);

        };//AllClassesHandler

        /**
         * The class implements the <code>AllThreads</code> command from the
         * VirtualMachine command set.
         */
        class AllThreadsHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>AllThreads</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//AllThreadsHandler

        /**
         * The class implements the <code>TopLevelThreadGroups</code> command
         * from the VirtualMachine command set.
         */
        class TopLevelThreadGroupsHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>TopLevelThreadGroups</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//TopLevelThreadGroupsHandler

        /**
         * The class implements the <code>Dispose</code> command from the
         * VirtualMachine command set.
         */
        class DisposeHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Dispose</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//DisposeHandler

        /**
         * The class implements the <code>IDSizes</code> command from the
         * VirtualMachine command set.
         */
        class IDSizesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>IDSizes</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//IDSizesHandler

        /**
         * The class implements the <code>Suspend</code> command from the
         * VirtualMachine command set.
         */
        class SuspendHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Suspend</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//SuspendHandler

        /**
         * The class implements the <code>Resume</code> command from the
         * VirtualMachine command set.
         */
        class ResumeHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Resume</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//ResumeHandler

        /**
         * The class implements the <code>Exit</code> command from the
         * VirtualMachine command set.
         */
        class ExitHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Exit</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//ExitHandler

        /**
         * The class implements the <code>CreateString</code> command from the
         * VirtualMachine command set.
         */
        class CreateStringHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>CreateString</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//CreateStringHandler

        /**
         * The class implements the <code>Capabilities</code> command from the
         * VirtualMachine command set.
         */
        class CapabilitiesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Capabilities</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//CapabilitiesHandler

        /**
         * The class implements the <code>ClassPaths</code> command from the
         * VirtualMachine command set.
         */
        class ClassPathsHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ClassPaths</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        private:
            void WritePathStrings(char *str, char pathSeparator)
                throw(AgentException);

        };//ClassPathsHandler

        /**
         * The class implements the <code>DisposeObjects</code> command from the
         * VirtualMachine command set.
         */
        class DisposeObjectsHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>DisposeObjects</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//DisposeObjectsHandler

        /**
         * The class implements the <code>HoldEvents</code> command from the
         * VirtualMachine command set.
         */
        class HoldEventsHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>HoldEvents</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//HoldEventsHandler

        /**
         * The class implements the <code>ReleaseEvents</code> command from the
         * VirtualMachine command set.
         */
        class ReleaseEventsHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ReleaseEvents</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//ReleaseEventsHandler

        /**
         * The class implements the <code>CapabilitiesNew</code> command from the
         * VirtualMachine command set.
         */
        class CapabilitiesNewHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>CapabilitiesNew</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//CapabilitiesNewHandler

        /**
         * The class implements the <code>RedefineClasses</code> command from the
         * VirtualMachine command set.
         */
        class RedefineClassesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>RedefineClasses</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//RedefineClassesHandler

        /**
         * The class implements the <code>SetDefaultStratum</code> command from the
         * VirtualMachine command set.
         */
        class SetDefaultStratumHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>SetDefaultStratum</code> JDWP command for the
             * VirtualMachine command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//SetDefaultStratumHandler

        /**
         * The class implements the <code>AllClassesWithGeneric</code> command
         * from the VirtualMachine command set.
         */
        class AllClassesWithGenericHandler : public AllClassesHandler {
        protected:

            /**
             * If the passed class (klass parameter) has status
             * JVMTI_CLASS_STATUS_PREPARED then writes class type tag,
             * reference id, signature, generic signature and status
             * to the reply packet and reutrn 0 - success sign.
             * Otherwise (class not prepared) doesn't write class info
             * to the reply packet and reutrn 1 - unsuccess sign.
             *
             * @param jni   - the JNI interface pointer
             * @param jvmti - the JVMTI interface pointer
             * @param klass - Java class
             *
             * @return 0 on success,
             *         1 otherwise.
             */
            virtual int Compose41Class(JNIEnv *jni, jvmtiEnv* jvmti, jclass klass)
                                            throw (AgentException);

        };//AllClassesWithGenericHandler

    } // VirtualMachine

} //jdwp

#endif //_VIRTUAL_MACHINE_H_
