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
package org.apache.harmony.rmi.server;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.rmi.server.LogStream;
import junit.framework.TestCase;

public class LogStreamTest extends TestCase {
    /**
     * Test for java.rmi.server.LogStream.write(int b)
     */
    public void testWriteI() throws Exception {
        //regression test for HARMONY-1271
        LogStream.log("tst").write((int)'\n');

        //regression test for HARMONY-994
        LogStream.log("tst").write(0);
    }
    
    public void testSetOutputStreamBad() throws Exception {
        // Regression test HARMONY-1198
        try {
            LogStream ls = LogStream.log("proba");
            ls.setOutputStream(null);
            fail("Expected NPE");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * Test for java.rmi.server.LogStream.write(byte[], int, int)
     * testing invalid offsets/lengths. 
     */
    public void testWriteArrInvalidOffLen() throws Exception {
        // Regression test for HARMONY-1691
        // list of invalid offsets/lengths pairs
        int[][] invalidPairs = new int[][] {
            { -2, 1 },
            { 0, -6 },
            { 6, 1 },
            { 0, 6 } };

        // store original default stream for LogStream
        PrintStream prevOut = LogStream.getDefaultStream();

        try {
            // set empty default stream to not print garbage to System.out/err
            LogStream.setDefaultStream(
                    new PrintStream(new ByteArrayOutputStream()));
            LogStream ls = LogStream.log("test");

            for (int i = 0; i < invalidPairs.length; ++i) {
                try {
                    ls.write(new byte[] { 1, 1 },
                            invalidPairs[i][0], invalidPairs[i][1]);
                    fail("IndexOutOfBoundsException "
                            + "is not thrown when off = " + invalidPairs[i][0]
                            + ", len = " + invalidPairs[i][1]);
                } catch (IndexOutOfBoundsException e) {
                    //expected
                }
            }
        } finally {
            // restore original stream
            LogStream.setDefaultStream(prevOut);
        }
    }
}
