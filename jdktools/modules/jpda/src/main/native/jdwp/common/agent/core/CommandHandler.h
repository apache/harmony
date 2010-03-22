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
 * @author Vitaly A. Provodin, Viacheslav G. Rybalov
 */

/**
 * @file
 * CommandHandler.h
 *
 */

#ifndef _COMMAD_HANDLER_H_
#define _COMMAD_HANDLER_H_

#include "jni.h"
#include "jvmti.h"

#include "AgentBase.h"
#include "AgentException.h"
#include "AgentMonitor.h"

namespace jdwp {

    class CommandParser;

    /**
     * A command handler is an object responsible for executing a
     * JDWP command and composing a reply.
     * The class <code>CommandHandler</code> is abstract and defines an
     * interface for every command handlers.
     * Two implementations of the given interface exist:
     * <ul>
     *   <li>The <code>SyncCommandHandler</code> class handles commands, which
     *       are executed synchronously.
     *   <li>The <code>AsyncCommandHandler</code> class handles commands, which
     *       are executed asynchronously.
     * </ul>
     * All command handlers must be inherited from one of the mentioned 
     * implementations.
     *
     * @see SyncCommandHandler
     * @see AsyncCommandHandler
     */
    class CommandHandler : public AgentBase
    {
    public:
        /**
         * Constructs a new <code>PacketDispatcher</code> object.
         */
        CommandHandler() {m_cmdParser = 0;}

        /**
         * Destroys the given <code>PacketDispatcher</code> object.
         */
        virtual ~CommandHandler() {}

        /**
         * Starts an execution of the JDWP command passed with the
         * <code>cmd</code> parameter.
         * All command handlers inherited from <code>CommandHandler</code> must
         * implement the given method.
         *
         * @param jni - the JNI interface pointer
         * @param cmd - points to the <code>CommandParser</code> object
         *
         * @exception The implementations of the given interface may throw
         *            <code>AgentException</code>.
         */
        virtual void Run(JNIEnv *jni, CommandParser *cmd) throw (AgentException) = 0;

        /**
         * Retuns the internal <code>CommandParser</code> instance.
         *
         * @return The internal <code>CommandParser</code> instance.
         */
        CommandParser* GetCommandParser() {return m_cmdParser;}

        /**
         * Identifies if the command handler performs the execution synchronously.
         *
         * @return Returns <code>TRUE</code> if JDWP-commands are executed
         *         synchronously, otherwise <code>FALSE</code>.
         */
        virtual bool IsSynchronous() = 0;

    protected:
        CommandParser *m_cmdParser;

        /**
         * Defines the interface for all derived classes. The method 
         * is intended to execute the JDWP command.
         *
         * @param jni - the JNI interface pointer
         *
         * @exception  If the packet data integrity gets broken,
         * InternalErrorException is thrown.
         *
         * @exception The implementations of the given interface
         * may throw AgentException.         
         */
        virtual void Execute(JNIEnv* jni) throw (AgentException) = 0;

        /**
         * Makes up reply with error based on the <code>AgentException<code> 
         * object.
         */
        virtual void ComposeError(const AgentException &e);

    };//class CommandHandler


    /**
     * Base class for synchronous command handlers.
     */
    class SyncCommandHandler : public CommandHandler
    {
    public:
        /**
         * Starts an execution of the JDWP-command passed with the
         * <code>cmd</code> parameter.
         *
         * @param jni - the JNI interface pointer
         * @param cmd - points to the object of <code>CommandParser</code>
         *
         * @exception If the reply to the given command can not be sent,
         * TransportException is thrown.
         */
        virtual void Run(JNIEnv *jni, CommandParser *cmd) throw (AgentException);

        /**
         * Identifies if the command handler does execution synchronously.
         *
         * @return Always returns <code>TRUE</code>.
         */
        virtual bool IsSynchronous() {return true;}

    };//class SyncCommandHandler


    /**
     * Base class for asynchronous command handlers.
     */
    class AsyncCommandHandler : public CommandHandler
    {
    public:
        ~AsyncCommandHandler();

        /**
         * Starts an execution of the JDWP-command passed with the
         * <code>cmd</code> parameter.
         * Before the execution the given method copies the
         * <code>cmd</code> parameter into the private object and starts the
         * execution of the JDWP command in a separate thread.
         *
         * @param jni - the JNI interface pointer
         * @param cmd - points to the object of <code>CommandParser</code>
         *
         * @exception If the system runs out of memory,
         *            <code>OutOfMemoryException</code> is thrown.
         * @exception <code>InternalErrorException</code> is thrown in any 
         * other cases.
         */
        virtual void Run(JNIEnv *jni, CommandParser *cmd) throw (AgentException);

        /**
         * Identifies if the command handler does execution synchronously.
         *
         * @return Always returns <code>FALSE</code>.
         */
        virtual bool IsSynchronous() {return false;}

        /**
         * Gets a thread name for asynchronous execution of the given command
         * handler.
         *
         * @return The thread name string.
         */
        virtual const char* GetThreadName();

        /**
         * Destroys the asynchronous command handler after execution.
         */
        virtual void Destroy() {delete this;}

    protected:
        /**
         * The given method is passed as a parameter to the 
         * <code>RunAgentThread()</code> method of the 
         * <code>ThreadManager</code>class.
         * The arg parameter is a pointer to an object of the corresponding
         * <code>CommandHandler</code> class. At the end of the 
         * <code>StartExecution</code> method the memory used by the given 
         * object is released.
         */
        static void JNICALL StartExecution(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg);

    };//class AsyncCommandHandler

    /**
     * Base class for special asynchronous command handlers for the deferred 
     * method invocation.
     */
    class SpecialAsyncCommandHandler : public AsyncCommandHandler
    {
    public:
        /**
         * Creates a new instance.
         */
        SpecialAsyncCommandHandler();

        /**
         * Destroys the given instance.
         */
        ~SpecialAsyncCommandHandler();

        /**
         * Does not delete the given asynchronous command after execution.
         * <code>ThreadManager</code> or <code>EventHandler</code> deletes it.
         */
        virtual void Destroy() {}

        /**
         * Returns method invocation options.
         */
        jint GetOptions() {
            return m_invokeOptions;
        }

        /**
         * Returns the thread for method invocation.
         */
        jthread GetThread() {
            return m_thread;
        }

        /**
         * Checks the flag if method invocation is completed.
         */
        bool IsInvoked() {
            return m_isInvoked;
        }

        /**
         * Sets the flag that method invocation is completed.
         */
        void SetInvoked(bool invoked) {
            m_isInvoked = invoked;
        }

        /**
         * Checks the flag if the thread is released after method invocation.
         */
        bool IsReleased() {
            return m_isReleased;
        }

        /**
         * Sets the flag that the thread is released after method invocation.
         */
        void SetReleased(bool released) {
            m_isReleased = released;
        }

        /**
         * Executes deferred method invocation.
         */
        void ExecuteDeferredInvoke(JNIEnv *jni);

    protected:
        
        /**
         * Initiates deferred invocation and waits for its completion.
         */
        void WaitDeferredInvocation(JNIEnv *jni);

        /**
         * The function to execute in deferred method invocation.
         */
        virtual void ExecuteDeferredFunc(JNIEnv *jni) = 0;

        /**
         * Calculates number of arguments for the given method signature.
         */
        jint getArgsNumber(char* methodSig);

        /**
         * Checks if provided method arguments are valid for the given method
         * signature.
         */
        jboolean IsArgValid(JNIEnv *jni, jint index, jdwpTaggedValue value, char* methodSig) throw(AgentException);

        /**
         * The error occurred in method invocation.
         */
        jdwpError m_returnError;

        /**
         * Options for method invocation.
         */
        jint m_invokeOptions;
        
        /**
         * The thread for method invocation.
         */
        jthread m_thread;
        
    private:

        /**
         * Returns a tag for a return value for the given method signature.
         */
        jdwpTag getTag(jint index, char* methodSig);
        
        /**
         * Extracts the argument name for the given method signature.
         */
        bool getClassNameArg(jint index, char* sig, char* name);

        /**
         * The flag for synchronization of method invocation.
         */
        volatile bool m_isInvoked;
        
        /**
         * The flag for synchronization of thread suspension after method 
         * invocation.
         */
        volatile bool m_isReleased;

    };//class SpecialAsyncCommandHandler

}//namespace jdwp


#endif //_COMMAD_HANDLER_H_
