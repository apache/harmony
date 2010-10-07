/* Licensed to the Apache Software Foundation (ASF) under one or more
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.nio.tests.java.nio.channels;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import junit.framework.TestCase;

public class UnixSelectorTest extends TestCase {
    static class Server {
        private ServerSocketChannel serverChannel = ServerSocketChannel.open();
        private ServerSocket socket = null;

        Server() throws Exception {
            serverChannel.configureBlocking(false);
        }

        public void initialize() throws Exception {
            this.socket = serverChannel.socket();
            socket.bind(new InetSocketAddress("localhost", 0));
        }

        public int getPort() {
            return socket.getLocalPort();
        }

        public boolean isOpen() {
            return !socket.isClosed();
        }

        public ServerSocketChannel getServerChannel() {
            return serverChannel;
        }

        public void accept() {
            Thread serverThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (serverChannel.accept() == null) {
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {}
                }
            });
            serverThread.start();
        }

        public void close() throws Exception{
            serverChannel.close();
        }
    }

    public void testSelectorAcceptAndRead() throws Exception {
        Selector sel0 = Selector.open();
        Selector sel1 = Selector.open();
        Server server = new Server();
        SelectableChannel serverChannel = server.getServerChannel();
        SelectionKey mkey0 = serverChannel.register(sel0, SelectionKey.OP_ACCEPT);
        serverChannel.register(sel1, SelectionKey.OP_ACCEPT);

        // HUP is treating as acceptable
        assertThat(sel0.select(100), is(1));
        assertThat(sel0.selectedKeys().contains(mkey0), is(true));
        server.initialize();
        // after bind can not accept
        assertThat(sel1.select(100), is(0));
        server.accept();
        Thread.sleep(1000);
        int port = server.getPort();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        Selector sel2 = Selector.open();
        socketChannel.register(sel2, SelectionKey.OP_WRITE);
        boolean isConnected = socketChannel.connect(new InetSocketAddress("localhost", port));
        if (!isConnected) {
            socketChannel.finishConnect();
        }

        assertThat(socketChannel.isConnected(), is(true));
        server.close();
        Thread.sleep(3000);
        assertThat(socketChannel.isConnected(), is(true));
        assertThat(sel2.select(100), is(1));
    }

    public void testSelectUnConnectedChannel() throws Exception {
        SocketChannel socketChannel2 = SocketChannel.open();
        socketChannel2.configureBlocking(false);
        Selector sel3 = Selector.open();
        SelectionKey mkey3 = socketChannel2.register(sel3, SelectionKey.OP_WRITE);
        // HUP is also treating as writable
        assertThat(sel3.select(100), is(1));
        assertThat(mkey3.isConnectable(), is(false));
        // even the channel is not connected, the selector could be writable
        assertThat(socketChannel2.isConnected(), is(false));
        assertThat(mkey3.isWritable(), is(true));

        Selector sel4 = Selector.open();
        SelectionKey mkey4 = socketChannel2.register(sel4, SelectionKey.OP_CONNECT);
        assertThat(sel4.select(100), is(1));
        assertThat(mkey4.isWritable(), is(false));
        assertThat(mkey4.isConnectable(), is(true));

        Selector sel5 = Selector.open();
        SelectionKey mkey5 = socketChannel2.register(sel5, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
        assertThat(sel5.select(100), is(1));
        assertThat(mkey5.isWritable(), is(true));
        assertThat(mkey5.isConnectable(), is(true));
    }
}
