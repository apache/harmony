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

package org.apache.harmony.luni.tests.java.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import junit.framework.TestCase;


public class WinNetworkInterfaceTest extends TestCase {
    // Regression Test for Harmony-2290
    public void test_isReachableLjava_net_NetworkInterfaceII_loopbackInterface()
            throws IOException {
        final int TTL = 20;
        final int TIME_OUT = 3000;

        NetworkInterface loopbackInterface = null;
        ArrayList<InetAddress> localAddresses = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                .getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> addresses = networkInterface
                    .getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address.isLoopbackAddress()) {
                    loopbackInterface = networkInterface;
                } else {
                    localAddresses.add(address);
                }
            }
        }

        // loopbackInterface can reach local address
        if (null != loopbackInterface) {
            for (InetAddress destAddress : localAddresses) {
                System.out.println(destAddress);
                assertTrue(destAddress.isReachable(loopbackInterface, TTL,
                        TIME_OUT));
            }
        }

        // loopback Interface cannot reach outside address
        // InetAddress destAddress = InetAddress.getByName("www.google.com");
        // assertFalse(destAddress.isReachable(loopbackInterface, TTL,
        // TIME_OUT));
    }
}
