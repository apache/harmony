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
 * @author Alexei Y. Zakharov
 */

package org.apache.harmony.jndi.provider.dns;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.harmony.jndi.internal.nls.Messages;

/**
 * Contains service methods that are used for transporting DNS messages from DNS
 * client to DNS server and vice versa.
 */
public class TransportMgr {

    /**
     * Sends the packet contained in <code>outBuf</code>, receives the answer
     * and stores it in <code>inBuf</code> array.
     * 
     * @param server
     *            server's IP-address in string form
     * @param serverPort
     *            server port
     * @param outBuf
     *            bytes of the message to send
     * @param outBufLen
     *            length of the <code>outBuf</code>
     * @param inBuf
     *            buffer to store received bytes at
     * @param inBufLen
     *            length of the <code>inBuf</code>
     * @param timeout
     *            time to wait for an answer, in milliseconds; 0 stands for
     *            infinite timeout
     * @return number of received bytes
     * @throws SocketTimeoutException
     *             in case of timeout
     * @throws SecurityException
     *             if security violation error occured; normally this happens if
     *             the access to the network subsystem has not been granted
     * @throws DomainProtocolException
     *             if some configuration or network problem encountered
     */
    public static int sendReceiveUDP(String server, int serverPort,
            byte[] outBuf, int outBufLen, byte[] inBuf, int inBufLen,
            int timeout) throws DomainProtocolException,
            SocketTimeoutException, SecurityException {

        byte srvAddrArr[] = null;
        InetAddress srvAddr = null;
        DatagramSocket dSocket = null;
        DatagramPacket inPacket = null;
        DatagramPacket outPacket = null;

        try {
            srvAddrArr = ProviderMgr.parseIpStr(server);
        } catch (IllegalArgumentException e) {
            // jndi.40=Unable to connect: bad IP address
            throw new DomainProtocolException(Messages.getString("jndi.40")); //$NON-NLS-1$
        }
        try {
            dSocket = new DatagramSocket();
            srvAddr = InetAddress.getByAddress(srvAddrArr);
            dSocket.connect(srvAddr, serverPort);
            outPacket = new DatagramPacket(outBuf, outBufLen, srvAddr,
                    serverPort);
            dSocket.setSoTimeout(timeout);
            dSocket.send(outPacket);
            inPacket = new DatagramPacket(inBuf, inBufLen, srvAddr, serverPort);
            dSocket.receive(inPacket);
        } catch (IllegalStateException e) {
            // jndi.41=Error while querying DNS server
            throw new DomainProtocolException(Messages.getString("jndi.41"), e); //$NON-NLS-1$
        } catch (SocketTimeoutException e) {
            throw (e);
        } catch (IOException e) {
            // jndi.41=Error while querying DNS server
            throw new DomainProtocolException(Messages.getString("jndi.41"), e); //$NON-NLS-1$
        } finally {
            if (dSocket != null) {
                dSocket.close();
            }
        }
        if (inPacket != null) {
            return inPacket.getLength();
        }
        // jndi.42=unknown error
        throw new DomainProtocolException(Messages.getString("jndi.42")); //$NON-NLS-1$
    }

    /**
     * Establishes TCP connection, transmit bytes from <code>inBuf</code>,
     * stores received answer in <code>outBuf</code>.
     * 
     * @param server
     *            the server's IP-address in string form
     * @param serverPort
     *            server port
     * @param outBuf
     *            bytes of the message to send
     * @param outBufLen
     *            length of the <code>outBuf</code>
     * @param inBuf
     *            buffer to store received bytes at
     * @param inBufLen
     *            length of the <code>inBuf</code>
     * @param timeout
     *            time to wait for an answer, in milliseconds; 0 stands for
     *            infinite timeout
     * @return number of received bytes
     * @throws SocketTimeoutException
     *             in case of timeout
     * @throws SecurityException
     *             if security violation error occured; normally this happens if
     *             the access to the network subsystem has not been granted
     * @throws DomainProtocolException
     *             if some configuration or network problem encountered TODO
     *             pool of connections may speed up things
     */
    public static int sendReceiveTCP(String server, int serverPort,
            byte[] outBuf, int outBufLen, byte[] inBuf, int inBufLen,
            int timeout) throws DomainProtocolException,
            SocketTimeoutException, SecurityException {

        byte srvAddrArr[] = null;
        InetAddress srvAddr = null;
        Socket socket = null;
        InputStream iStream = null;
        BufferedOutputStream oStream = null;
        byte tmpArr[] = new byte[2];
        int inLen;
        int actualLen = -1;

        try {
            srvAddrArr = ProviderMgr.parseIpStr(server);
        } catch (IllegalArgumentException e) {
            // jndi.40=Unable to connect: bad IP address
            throw new DomainProtocolException(Messages.getString("jndi.40")); //$NON-NLS-1$
        }
        try {
            srvAddr = InetAddress.getByAddress(srvAddrArr);
            socket = new Socket(srvAddr, serverPort);
            socket.setSoTimeout(timeout);
            oStream = new BufferedOutputStream(socket.getOutputStream());
            ProviderMgr.write16Int(outBufLen, tmpArr, 0);
            oStream.write(tmpArr, 0, 2);
            oStream.write(outBuf, 0, outBufLen);
            oStream.flush();
            iStream = socket.getInputStream();
            iStream.read(tmpArr, 0, 2);
            inLen = ProviderMgr.parse16Int(tmpArr, 0);
            if (inLen > inBufLen) {
                // jndi.43=Output buffer is too small
                throw new DomainProtocolException(Messages.getString("jndi.43")); //$NON-NLS-1$
            }
            actualLen = iStream.read(inBuf, 0, inLen);
            if (actualLen != inLen) {
                // jndi.44=Error while receiving message over TCP
                throw new DomainProtocolException(Messages.getString("jndi.44")); //$NON-NLS-1$
            }
        } catch (IllegalStateException e) {
            // jndi.41=Error while querying DNS server
            throw new DomainProtocolException(Messages.getString("jndi.41"), e); //$NON-NLS-1$
        } catch (SocketTimeoutException e) {
            throw (e);
        } catch (IOException e) {
            // jndi.41=Error while querying DNS server
            throw new DomainProtocolException(Messages.getString("jndi.41"), e); //$NON-NLS-1$
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
        return actualLen;
    }

    /**
     * Tries to determine IP address by hostname using
     * <code>Inet4Address.getByName(String)</code> method.
     * 
     * @return determined address or <code>null</code> if not found
     * @see java.net.InetAddress#getByName(String)
     */
    public static InetAddress getIPByName_OS(String hostname) {
        InetAddress addr = null;

        try {
            addr = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            // ignore it
        }
        return addr;
    }

}
