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

package org.apache.harmony.sql.tests.javax.sql.rowset.serial;

import java.net.URL;

import javax.sql.rowset.serial.SerialDatalink;
import javax.sql.rowset.serial.SerialException;

import junit.framework.TestCase;

public class SerialDatalinkTest extends TestCase {
    public void testConstructor() throws Exception {

        URL url = new URL("http://www.someurl.com");
        new SerialDatalink(url);

        try {
            new SerialDatalink(null);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
    }

    public void testGetDatalink() throws Exception {
        URL url = new URL("http://www.someurl.com");
        SerialDatalink sdl = new SerialDatalink(url);
        URL dataLink = sdl.getDatalink();
        assertEquals(url, dataLink);
        assertNotSame(url, dataLink);
    }
}
