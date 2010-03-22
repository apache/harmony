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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Mikhail A. Markov
 */
package org.apache.harmony.rmi.transport;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;


/**
 * Wrapper for already opened socket.
 * It just translates all requests to the underlying socket.
 *
 * @author  Mikhail A. Markov
 */
public class SocketWrapper extends Socket {

    protected Socket s;
    protected InputStream in;
    protected OutputStream out;

    protected SocketWrapper(Socket s) throws IOException {
        this(s, null, null);
    }

    public SocketWrapper(Socket s, InputStream in, OutputStream out)
            throws IOException {
        this.s = s;
        this.in = (in == null) ? s.getInputStream() : in;
        this.out = (out == null) ? s.getOutputStream() : out;
    }

    public void connect(SocketAddress endpoint) throws IOException {
        s.connect(endpoint);
    }

    public void connect(SocketAddress endpoint, int timeout)
            throws IOException {
        s.connect(endpoint, timeout);
    }

    public void bind(SocketAddress bindpoint) throws IOException {
        s.bind(bindpoint);
    }

    public InetAddress getInetAddress() {
        return s.getInetAddress();
    }

    public InetAddress getLocalAddress() {
        return s.getLocalAddress();
    }

    public int getPort() {
        return s.getPort();
    }

    public int getLocalPort() {
        return s.getLocalPort();
    }

    public SocketAddress getRemoteSocketAddress() {
        return s.getRemoteSocketAddress();
    }

    public SocketAddress getLocalSocketAddress() {
        return s.getLocalSocketAddress();
    }

    //public SocketChannel getChannel() {
    //    return s.getChannel();
    //}

    public InputStream getInputStream() throws IOException {
        return in;
    }

    public OutputStream getOutputStream() throws IOException {
        return out;
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        s.setTcpNoDelay(on);
    }

    public boolean getTcpNoDelay() throws SocketException {
        return s.getTcpNoDelay();
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        s.setSoLinger(on, linger);
    }

    public int getSoLinger() throws SocketException {
        return s.getSoLinger();
    }

    public void sendUrgentData(int data) throws IOException {
        s.sendUrgentData(data);
    }

    public void setOOBInline(boolean on) throws SocketException {
        s.setOOBInline(on);
    }

    public boolean getOOBInline() throws SocketException {
        return s.getOOBInline();
    }

    public void setSoTimeout(int timeout) throws SocketException {
        s.setSoTimeout(timeout);
    }

    public int getSoTimeout() throws SocketException {
        return s.getSoTimeout();
    }

    public void setSendBufferSize(int size) throws SocketException {
        s.setSendBufferSize(size);
    }

    public int getSendBufferSize() throws SocketException {
        return s.getSendBufferSize();
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        s.setReceiveBufferSize(size);
    }

    public int getReceiveBufferSize() throws SocketException {
        return s.getReceiveBufferSize();
    }

    public void setKeepAlive(boolean on) throws SocketException {
        s.setKeepAlive(on);
    }

    public boolean getKeepAlive() throws SocketException {
        return s.getKeepAlive();
    }

    public void setTrafficClass(int tc) throws SocketException {
        s.setTrafficClass(tc);
    }

    public int getTrafficClass() throws SocketException {
        return s.getTrafficClass();
    }

    public void setReuseAddress(boolean on) throws SocketException {
        s.setReuseAddress(on);
    }

    public boolean getReuseAddress() throws SocketException {
        return s.getReuseAddress();
    }

    public void close() throws IOException {
        s.close();
    }

    public void shutdownInput() throws IOException {
        s.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        s.shutdownOutput();
    }

    public String toString() {
        return s.toString();
    }

    public boolean isConnected() {
        return s.isConnected();
    }

    public boolean isBound() {
        return s.isBound();
    }

    public boolean isClosed() {
        return s.isClosed();
    }

    public boolean isInputShutdown() {
        return s.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return s.isOutputShutdown();
    }
}
