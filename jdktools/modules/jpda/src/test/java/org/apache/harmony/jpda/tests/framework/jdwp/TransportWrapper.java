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
 * @author Ivan G. Popov
 */

/**
 * Created on 05.23.2004
 */
package org.apache.harmony.jpda.tests.framework.jdwp;

import java.io.IOException;

/**
 * This interface provides wrapper around JDWP transport connection.
 * Particular implementation can interact directly with raw connection
 * like SocketTransportWrapper or use JDI service provider interfaces
 * to support all pluggable JDI transports.
 *  
 */
public interface TransportWrapper {

    /**
     * Starts listening for connection on given or default address.
     * 
     * @param address address to listen or null for default address
     * @return string representation of listening address 
     */
    public String startListening(String address) throws IOException;
    
    /**
     * Stops listening for connection on current address.
     */
    public void stopListening() throws IOException;

    /**
     * Accepts transport connection for currently listened address and performs handshaking 
     * for specified timeout.
     * 
     * @param acceptTimeout timeout for accepting in milliseconds
     * @param handshakeTimeout timeout for handshaking in milliseconds
     */
    public void accept(long acceptTimeout, long handshakeTimeout) throws IOException;
    
    /**
     * Attaches transport connection to given address and performs handshaking 
     * for specified timeout.
     * 
     * @param address address for attaching
     * @param attachTimeout timeout for attaching in milliseconds
     * @param handshakeTimeout timeout for handshaking in milliseconds
     */
    public void attach(String address, long attachTimeout, long handshakeTimeout) throws IOException;

    /**
     * Closes transport connection.
     */
    public void close() throws IOException;

    /**
     * Checks if transport connection is open.
     * 
     * @return true if transport connection is open
     */
    public boolean isOpen();

    /**
     * Reads packet from transport connection.
     * 
     * @return packet as byte array or null or empty packet if connection was closed
     */
    public byte[] readPacket() throws IOException;

    /**
     * Writes packet to transport connection.
     * 
     * @param packet packet as byte array
     */
    public void writePacket(byte[] packet) throws IOException;
}
