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

package org.apache.harmony.nio.tests;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import junit.framework.TestCase;

import org.apache.harmony.nio.AddressUtil;

public class AddressUtilTest extends TestCase {
    
    /**
     * @tests AddressUtil#getDirectBufferAddress
     */
    public void test_getDirectBufferAddress() throws Exception {
        ByteBuffer buf = ByteBuffer.allocateDirect(10);
        assertTrue(AddressUtil.getDirectBufferAddress(buf) != 0);
    }
    
    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getFileChannelAddress() throws Exception {
//        FileChannel fc = new FileInputStream("src/main/java/org/apache/harmony/nio/AddressUtil.java").getChannel();
//        assertTrue(AddressUtil.getChannelAddress(fc) > 0);
    }
    
    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getSocketChannelAddress() throws Exception {
        SocketChannel sc = SocketChannel.open();
        assertTrue(AddressUtil.getChannelAddress(sc)>0);
    }
    
    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getDatagramChannelAddress() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        assertTrue(AddressUtil.getChannelAddress(dc)>0);
    }
    
    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getServerSocketChannelAddress() throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        assertTrue(AddressUtil.getChannelAddress(ssc)>0);
    }  
    
    /**
     * @tests AddressUtil#getChannelAddress
     */
    public void test_getNonNativeChannelAddress() throws Exception{
        Channel channel = new MockChannel();
        assertEquals(0, AddressUtil.getChannelAddress(channel));
    }
    
    private static class MockChannel implements Channel{
        public boolean isOpen() {
            return false;
        }
        public void close() throws IOException {
        }
    }
}
    
