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
 * CommandDispatcher.h
 *
 */

#ifndef _COMMAD_DISPATCHER_H_
#define _COMMAD_DISPATCHER_H_

#include "AgentBase.h"

namespace jdwp {

    class CommandParser;
    class CommandHandler;

    /**
     * Identifies a command passed by <code>PacketDispatcher</code>, finds
     * an appropriate <code>CommandHandler</code> and starts execution of the
     * given command.
     * After getting the JDWP command from <code>PacketDispatcher</code>,
     * <code>CommandDispatcher</code> starts an appropriate
     * <code>CommandHandler</code>. If any errors occur while starting
     * <code>CommandHandler</code>, <code>CommandDispatcher</code>
     * composes and sends a reply and returns
     * the control to <code>PacketDispatcher</code>.
     *
     * @see PacketDispatcher
     * @see CommandHandler
     */
    class CommandDispatcher : public AgentBase
    {
    public:

        /**
         * Creates an object of the <code>CommandHandler</code> class according
         * to the obtained JDWP command.
         * The method extracts <code>jdwpCommandSet</code> and
         * <code>jdwpCommand</code> from the <code>cmdParser</code> parameter
         * and creates the proper <code>CommandHandler</code>, based on the current 
         * values.
         * Then the given handler is started.
         *
         * @param jni       - the JNI interface pointer
         * @param cmdParser - a wrapper of the JDWP command to be executed
         *
         * @exception If a transport error occurs, <code>TransportException</code>
         *            is thrown.
         *
         * @see CommandParser
         */
        void ExecCommand(JNIEnv* jni, CommandParser *cmdParser)
            throw(AgentException);

        /**
         * Returns the name corresponding to the given JDWP command set.
         *
         * @param cmdSet - command set identifier
         *
         * @return Zero terminated string.
         */
        static const char* GetCommandSetName(jdwpCommandSet cmdSet);

        /**
         * Returns the name corresponding to the specific command of the 
         * given JDWP command set.
         *
         * @param cmdSet  - command set identifier
         * @param cmdKind - command kind
         *
         * @return Zero terminated string.
         */
        static const char* GetCommandName(jdwpCommandSet cmdSet, jdwpCommand cmdKind);

    private:

        static CommandHandler* CreateCommandHandler(jdwpCommandSet cmdSet, jdwpCommand cmdKind)
            throw (NotImplementedException, OutOfMemoryException);

    };//class CommandDispatcher

}//namespace jdwp

#endif //_COMMAD_DISPATCHER_H_
