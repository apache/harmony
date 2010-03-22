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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

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
            virtual int Execute(JNIEnv *jni);

        };//SuspendCountHandler

        // New command for Java 6
        /**
         * The class implements the <code>OwnedMonitorsStackDepthInfo</code> command from the
         * ThreadReference command set.
         */
        class OwnedMonitorsStackDepthInfoHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>OwnedMonitorsStackDepthInfo</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        };//OwnedMonitorsStackDepthInfoHandler
        
        /**
         * The class implements the <code>ForceEarlyReturn</code> command from the
         * ThreadReference command set.
         */
        class ForceEarlyReturnHandler : public SyncCommandHandler {
        protected:

            /**
             * Executes the <code>ForceEarlyReturn</code> JDWP command for the
             * ThreadReference command set.
             *
             * @param jni - the JNI interface pointer
             */
            virtual int Execute(JNIEnv *jni);

        };//ForceEarlyReturnHandler

    }//ThreadReference

}//jdwp

#endif //_THREAD_REFERENCE_H_
