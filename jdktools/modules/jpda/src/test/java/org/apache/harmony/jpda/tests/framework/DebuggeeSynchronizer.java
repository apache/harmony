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
 * Created on 29.01.2005
 */
package org.apache.harmony.jpda.tests.framework;

/**
 * This interface is aimed to provide another way to synchronize execution of
 * debugger and debuggee beyond the scope of JDWP channel.
 * <p>
 * Synchronization is performed by sending text messages. As an example,
 * it can be implemented via TCP/IP sockets.
 */
public interface DebuggeeSynchronizer {

    /**
     * Sends specified string <code>message</code> to the channel.
     * 
     * @param message message to be sent
     */
    abstract public void sendMessage(String message);

    /**
     * Waits for specified message. If received string is equal to
     * <code>message</code> then <code>true</code> returns.
     *  
     * @param message expected message
     * @return <code>true</code> if received message is equal to
     *         expected or <code>false</code> otherwise
     */
    abstract public boolean receiveMessage(String message);

    /**
     * Waits for any message from synchronized channel. 
     *  
     * @return received message
     */
    abstract public String receiveMessage();
}
