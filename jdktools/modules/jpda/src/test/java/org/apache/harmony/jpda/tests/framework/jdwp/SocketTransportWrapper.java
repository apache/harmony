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


import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.IOException;

import org.apache.harmony.jpda.tests.framework.jdwp.Packet;

/**
 * This class provides TransportWrapper for row TCP/IP socket connection.
 *  
 */
public class SocketTransportWrapper implements TransportWrapper {

    public static final String HANDSHAKE_STRING = "JDWP-Handshake";

    private ServerSocket serverSocket;
    private Socket transportSocket;
    private InputStream input;
    private OutputStream output;

    /**
     * Starts listening for connection on given or default address.
     * 
     * @param address address to listen or null for default address
     * @return string representation of listening address 
     */
    public String startListening(String address) throws IOException {
        String hostName = null;
        InetAddress hostAddr = null;
        int port = 0;
        if (address != null) {
            String portName = null;
            int i = address.indexOf(':');
            if (i < 0) {
                portName = address;
            } else {
                hostName = address.substring(0, i);
                portName = address.substring(i+1);
            }
            try {
                port = Integer.parseInt(portName);
            } catch (NumberFormatException e) {
                throw new IOException("Illegal port number in socket address: " + address);
            }
        }

        if (hostName != null) {
            hostAddr = InetAddress.getByName(hostName);
            serverSocket = new ServerSocket(port, 0, hostAddr);
        } else {
            serverSocket = new ServerSocket(port);
        }
        
        // use as workaround for unspecified behaviour of isAnyLocalAddress()
        InetAddress iAddress = null;
        if (hostName != null) {
            iAddress = serverSocket.getInetAddress();
        } else {
            iAddress = InetAddress.getLocalHost();
        }
        
        address = iAddress.getHostName() + ":" + serverSocket.getLocalPort();
        return address;
    }
    
    /**
     * Stops listening for connection on current address.
     */
    public void stopListening() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    /**
     * Accepts transport connection for currently listened address and performs handshaking 
     * for specified timeout.
     * 
     * @param acceptTimeout timeout for accepting in milliseconds
     * @param handshakeTimeout timeout for handshaking in milliseconds
     */
    public void accept(long acceptTimeout, long handshakeTimeout) throws IOException {
        synchronized (serverSocket) {
            serverSocket.setSoTimeout((int) acceptTimeout);
            try {
                transportSocket = serverSocket.accept();
            } finally {
                serverSocket.setSoTimeout(0);
            }
        }
        createStreams();
        handshake(handshakeTimeout);
    }
    
    /**
     * Attaches transport connection to given address and performs handshaking 
     * for specified timeout.
     * 
     * @param address address for attaching
     * @param attachTimeout timeout for attaching in milliseconds
     * @param handshakeTimeout timeout for handshaking in milliseconds
     */
    public void attach(String address, long attachTimeout, long handshakeTimeout) throws IOException {
        if (address == null) {
            throw new IOException("Illegal socket address: " + address);
        }

        String hostName = null;
        int port = 0;
        {
            String portName = null;
            int i = address.indexOf(':');
            if (i < 0) {
                throw new IOException("Illegal socket address: " + address);
            } else {
                hostName = address.substring(0, i);
                portName = address.substring(i+1);
            }
            try {
                port = Integer.parseInt(portName);
            } catch (NumberFormatException e) {
                throw new IOException("Illegal port number in socket address: " + address);
            }
        }

        long finishTime = System.currentTimeMillis() + attachTimeout;
        long sleepTime = 4 * 1000; // millesecinds
        IOException exception = null;
        try {
            do {
                try {
                    transportSocket = new Socket(hostName, port);
                    break;
                } catch (IOException e) {
                    Thread.sleep(sleepTime);
                }
            } while (attachTimeout == 0 || System.currentTimeMillis() < finishTime);
        } catch (InterruptedException e) {
            throw new InterruptedIOException("Interruption in attaching to " + address);
        }
        
        if (transportSocket == null) {
            if (exception != null) {
                throw exception;
            } else {
                throw new SocketTimeoutException("Timeout exceeded in attaching to " + address);
            }
        }
        
        createStreams();
        handshake(handshakeTimeout);
    }

    /**
     * Closes transport connection.
     */
    public void close() throws IOException {
        if (input != null) {
            input.close();
        }
        if (output != null) {
            output.close();
        }
        
        if (transportSocket != null && input == null && output == null && !transportSocket.isClosed()) {
            transportSocket.close();
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    /**
     * Checks if transport connection is open.
     * 
     * @return true if transport connection is open
     */
    public boolean isOpen() {
        return (transportSocket != null 
                    && transportSocket.isConnected() 
                    && !transportSocket.isClosed());
    }

    /**
     * Reads packet bytes from transport connection.
     * 
     * @return packet as byte array or null or empty packet if connection was closed
     */
    public byte[] readPacket() throws IOException {

        // read packet header
        byte[] header = new byte[Packet.HEADER_SIZE];
        int off = 0;

        while (off < Packet.HEADER_SIZE) {
            try {
                int bytesRead = input.read(header, off, Packet.HEADER_SIZE - off);
                if (bytesRead < 0) {
                    break;
                }
                off += bytesRead;
            } catch (IOException e) {
                // workaround for "Socket Closed" exception if connection was closed
                break;
            }
        }

        if (off == 0) {
            return null;
        }
        if (off < Packet.HEADER_SIZE) {
            throw new IOException("Connection closed in reading packet header");
        }

        // extract packet length
        int len = Packet.getPacketLength(header);
        if (len < Packet.HEADER_SIZE) {
            throw new IOException("Wrong packet size detected: " + len);
        }
        
        // allocate packet bytes and store header there 
        byte[] bytes = new byte[len];
        System.arraycopy(header, 0, bytes, 0, Packet.HEADER_SIZE);

        // read packet data
        while (off < len) {
            int bytesRead = input.read(bytes, off, len - off);
            if (bytesRead < 0) {
                break;
            }
            off += bytesRead;
        }
        if (off < len) {
            throw new IOException("Connection closed in reading packet data");
        }

        return bytes;
    }

    /**
     * Writes packet bytes to transport connection.
     * 
     * @param packet
     *            packet as byte array
     */
    public void writePacket(byte[] packet) throws IOException {
        output.write(packet);
        output.flush();
    }

    /**
     * Performs handshaking for given timeout.
     * 
     * @param handshakeTimeout timeout for handshaking in milliseconds
     */
    protected void handshake(long handshakeTimeout) throws IOException {
        transportSocket.setSoTimeout((int) handshakeTimeout);
        
        try {
            output.write(HANDSHAKE_STRING.getBytes());
            output.flush();
            int len = HANDSHAKE_STRING.length();
            byte[] bytes = new byte[len];
            int off = 0;
            while (off < len) {
                int bytesRead = input.read(bytes, off, len - off);
                if (bytesRead < 0) {
                    break;
                }
                off += bytesRead;
            }
            String response = new String(bytes, 0, off);
            if (!response.equals(HANDSHAKE_STRING)) {
                throw new IOException("Unexpected handshake response: " + response);
            }
        } finally {
            transportSocket.setSoTimeout(0);
        }
    }

    /**
     * Creates input/output streams for connection socket.
     */
    protected void createStreams() throws IOException {
        input = transportSocket.getInputStream();
        output = transportSocket.getOutputStream();
    }
}
