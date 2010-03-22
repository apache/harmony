/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.harmony.unpack200.tests;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.harmony.unpack200.Segment;

public class ClassVersionTest extends TestCase {

    private static final int JAVA_15 = 49;

    public void testCorrectVersionOfSegment() throws IOException {
        InputStream in = Segment.class
                .getResourceAsStream("/org/apache/harmony/unpack200/Segment.class");
        DataInputStream din = new DataInputStream(in);

        assertEquals(0xCAFEBABE, din.readInt());
        din.readShort(); // MINOR -- don't care
        assertTrue("Class file has been compiled with Java 1.5 compatibility"
                + " instead of 1.4 or lower", din.readShort() < JAVA_15);
    }

    public void testCorrectVersionOfTest() throws IOException {
        InputStream in = Segment.class
                .getResourceAsStream("/org/apache/harmony/unpack200/tests/ClassVersionTest.class");
        DataInputStream din = new DataInputStream(in);

        assertEquals(0xCAFEBABE, din.readInt());
        din.readShort(); // MINOR -- don't care
        assertTrue("Class file has been compiled with Java 1.5 compatibility"
                + " instead of 1.4 or lower", din.readShort() < JAVA_15);
        din.close();
    }

    public void testCorrectVersionOfAdapter() throws IOException {
        // tests that both the file is on the classpath and that it's been
        // compiled correctly, but without actually loading the class
        InputStream in = Segment.class
                .getResourceAsStream("/org/apache/harmony/unpack200/Pack200Adapter.class");
        if (in != null) { // If running in Eclipse and Java5 stuff not
            // built/available
            DataInputStream din = new DataInputStream(in);

            assertEquals(0xCAFEBABE, din.readInt());
            din.readShort(); // MINOR -- don't care
            assertTrue("Class file needs 1.5 compatibility",
                    din.readShort() >= JAVA_15);
            din.close();
        }
    }
}
