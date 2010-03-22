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
 * ThreadReference.h
 *
 */

#ifndef _THREAD_REFERENCE_H_
#define _THREAD_REFERENCE_H_

#include "AgentException.h"
#include "CommandHandler.h"

namespace jdwp {

    /**
     * The namespace includes declaration of the classes implementing commands
     * from the ThreadReference command set.
     */
    namespace ThreadReference {
        
        /**
         * The class implements the <code>Name</code> command from the
         * ThreadReference command set.
         */
        class NameHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Name</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//NameHandler

        /**
         * The class implements the <code>Suspend</code> command from the
         * ThreadReference command set.
         */
        class SuspendHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Suspend</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//SuspendHandler

        /**
         * The class implements the <code>Resume</code> command from the
         * ThreadReference command set.
         */
        class ResumeHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Resume</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//ResumeHandler

        /**
         * The class implements the <code>Status</code> command from the
         * ThreadReference command set.
         */
        class StatusHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Status</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//StatusHandler

        /**
         * The class implements the <code>ThreadGroup</code> command from the
         * ThreadReference command set.
         */
        class ThreadGroupHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ThreadGroup</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//ThreadGroupHandler

        /**
         * The class implements the <code>Frames</code> command from the
         * ThreadReference command set.
         */
        class FramesHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Frames</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//FramesHandler

        /**
         * The class implements the <code>FrameCount</code> command from the
         * ThreadReference command set.
         */
        class FrameCountHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>FrameCount</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//FrameCountHandler

        /**
         * The class implements the <code>OwnedMonitors</code> command from the
         * ThreadReference command set.
         */
        class OwnedMonitorsHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>OwnedMonitors</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//OwnedMonitorsHandler

        /**
         * The class implements the <code>CurrentContendedMonitor</code> command from the
         * ThreadReference command set.
         */
        class CurrentContendedMonitorHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>CurrentContendedMonitor</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//CurrentContendedMonitorHandler

        /**
         * The class implements the <code>Stop</code> command from the
         * ThreadReference command set.
         */
        class StopHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Stop</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//StopHandler

        /**
         * The class implements the <code>Interrupt</code> command from the
         * ThreadReference command set.
         */
        class InterruptHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>Interrupt</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//InterruptHandler

        /**
         * The class implements the <code>SuspendCount</code> command from the
         * ThreadReference command set.
         */
        class SuspendCountHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>SuspendCount</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual void Execute(JNIEnv *jni) throw(AgentException);

        };//SuspendCountHandler

    }//ThreadReference

}//jdwp

#endif //_THREAD_REFERENCE_H_
