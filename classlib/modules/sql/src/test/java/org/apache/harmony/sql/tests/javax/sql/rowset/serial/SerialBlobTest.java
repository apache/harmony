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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;

import junit.framework.TestCase;

public class SerialBlobTest extends TestCase {

    public void testConstructorLBlob() throws Exception {
        boolean isAbnormal = false;
        MockSerialBlob mockBlob = new MockSerialBlob(isAbnormal);
        SerialBlob serialBlob = new SerialBlob(mockBlob);
        // SerialBlob constructor initiliases with the data of given blob,
        // therefore, blob.getBytes is invoked.
        assertTrue(mockBlob.isGetBytesInvoked);
        assertEquals(1, serialBlob.length());

        isAbnormal = true;
        mockBlob = new MockSerialBlob(isAbnormal);
        try {
            new SerialBlob(mockBlob);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            new SerialBlob((Blob) null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    public void testConstructor$B() throws Exception {
        byte[] buf = new byte[8];
        SerialBlob serialBlob = new SerialBlob(buf);
        assertEquals(8, serialBlob.length());

        try {
            new SerialBlob((byte[]) null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetBinaryStream() throws Exception {
        byte[] buf = { 1, 2, 3, 4, 5, 6, 7, 8 };
        SerialBlob serialBlob = new SerialBlob(buf);
        InputStream is = serialBlob.getBinaryStream();
        int i = 0;
        while (true) {
            int b = is.read();
            if (b == -1) {
                if (i < buf.length) {
                    fail("returned input stream contains too few data");
                }
                break;
            }

            if (i > buf.length) {
                fail("returned input stream contains too much data");
            }
            assertEquals(buf[i++], b);
        }
    }

    public void testGetBytesJI() throws Exception {
        byte[] buf = { 1, 2, 3, 4, 5, 6, 7, 8 };
        SerialBlob serialBlob = new SerialBlob(buf);
        byte[] data = serialBlob.getBytes(1, 1);
        assertEquals(1, data.length);
        assertEquals(1, data[0]);

        data = serialBlob.getBytes(2, 3);
        assertEquals(3, data.length);
        assertEquals(2, data[0]);
        assertEquals(3, data[1]);
        assertEquals(4, data[2]);

        // Harmony-2725 RI's bug : RI throws SerialException here.
        data = serialBlob.getBytes(2, 1);
        assertEquals(1, data.length);
        assertEquals(2, data[0]);

        data = serialBlob.getBytes(1, 10);
        assertEquals(8, data.length);
        assertTrue(Arrays.equals(buf, data));

        try {
            data = serialBlob.getBytes(2, -1);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            data = serialBlob.getBytes(0, 2);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            data = serialBlob.getBytes(-1, 2);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            data = serialBlob.getBytes(10, 11);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
    }

    public void testSetBytesJ$B() throws Exception {
        byte[] buf = { 1, 2, 3, 4, 5, 6, 7, 8 };
        byte[] theBytes = { 9, 10, 11 };
        SerialBlob serialBlob = new SerialBlob(buf);

        int count = serialBlob.setBytes(1, theBytes);
        byte[] res = serialBlob.getBytes(1, buf.length);
        byte[] expected = { 9, 10, 11, 4, 5, 6, 7, 8 };
        assertTrue(Arrays.equals(expected, res));
        assertEquals(3, count);

        count = serialBlob.setBytes(2, theBytes);
        res = serialBlob.getBytes(1, buf.length);
        expected = new byte[] { 9, 9, 10, 11, 5, 6, 7, 8 };
        assertTrue(Arrays.equals(expected, res));
        assertEquals(3, count);

        count = serialBlob.setBytes(6, theBytes);
        res = serialBlob.getBytes(1, buf.length);
        expected = new byte[] { 9, 9, 10, 11, 5, 9, 10, 11 };
        assertTrue(Arrays.equals(expected, res));
        assertEquals(3, count);

        try {
            serialBlob.setBytes(-1, theBytes);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            serialBlob.setBytes(10, theBytes);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        // Non bug difference from RI, Harmony-2836
        try {
            serialBlob.setBytes(7, theBytes);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

    }

    public void testSetBytesJ$BII() throws Exception {
        byte[] buf = { 1, 2, 3, 4, 5, 6, 7, 8 };
        byte[] theBytes = { 9, 10, 11 };
        SerialBlob serialBlob = new SerialBlob(buf);

        int count = serialBlob.setBytes(1, theBytes, 0, 3);
        byte[] res = serialBlob.getBytes(1, buf.length);
        byte[] expected = { 9, 10, 11, 4, 5, 6, 7, 8 };
        assertTrue(Arrays.equals(expected, res));
        assertEquals(3, count);

        count = serialBlob.setBytes(3, theBytes, 1, 2);
        res = serialBlob.getBytes(1, buf.length);
        expected = new byte[] { 9, 10, 10, 11, 5, 6, 7, 8 };
        assertTrue(Arrays.equals(expected, res));
        assertEquals(2, count);

        count = serialBlob.setBytes(6, theBytes, 0, 2);
        res = serialBlob.getBytes(1, buf.length);
        expected = new byte[] { 9, 10, 10, 11, 5, 9, 10, 8 };
        assertTrue(Arrays.equals(expected, res));
        assertEquals(2, count);

        try {
            serialBlob.setBytes(7, theBytes, 0, 10);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            serialBlob.setBytes(-1, theBytes, 0, 2);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            serialBlob.setBytes(10, theBytes, 0, 2);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            serialBlob.setBytes(1, theBytes, -1, 2);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            serialBlob.setBytes(1, theBytes, 0, 10);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        // Non bug difference from RI, Harmony-2836
        try {
            serialBlob.setBytes(7, theBytes, 0, 3);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        // Non bug difference from RI, Harmony-2836
        try {
            serialBlob.setBytes(7, theBytes, 0, -1);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
    }

    public void testPosition$BJ() throws Exception {
        byte[] buf = { 1, 2, 3, 4, 5, 6, 7, 8 };
        SerialBlob serialBlob = new SerialBlob(buf);

        assertBlobPosition_BytePattern(serialBlob);

        MockSerialBlob mockBlob = new MockSerialBlob();
        mockBlob.buf = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        serialBlob = new SerialBlob(mockBlob);
        assertBlobPosition_BytePattern(serialBlob);
    }

    public void testPositionLBlobJ() throws Exception {
        byte[] buf = { 1, 2, 3, 4, 5, 6, 7, 8 };
        SerialBlob serialBlob = new SerialBlob(buf);
        assertBlobPosition_BlobPattern(serialBlob);

        MockSerialBlob mockBlob = new MockSerialBlob();
        mockBlob.buf = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        serialBlob = new SerialBlob(mockBlob);
        assertBlobPosition_BlobPattern(serialBlob);
    }

    public void testTruncateJ() throws Exception {
        byte[] buf = { 1, 2, 3, 4, 5, 6, 7, 8 };
        SerialBlob serialBlob1 = new SerialBlob(buf);
        MockSerialBlob mockBlob = new MockSerialBlob();
        mockBlob.buf = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        SerialBlob serialBlob2 = new SerialBlob(mockBlob);
        SerialBlob[] serialBlobs = { serialBlob1, serialBlob2 };

        for (SerialBlob serialBlob : serialBlobs) {

            serialBlob.truncate(3);
            assertEquals(3L, serialBlob.length());
            byte[] truncatedBytes = serialBlob.getBytes(1, 3);
            assertTrue(Arrays.equals(new byte[] { 1, 2, 3 }, truncatedBytes));

            try {
                serialBlob.truncate(-1);
                fail("should throw SerialException");
            } catch (SerialException e) {
                // expected
            }

            // Non bug difference from RI, Harmony-2937
            assertEquals(3L, serialBlob.length());

            try {
                serialBlob.truncate(10);
                fail("should throw SerialException");
            } catch (SerialException e) {
                // expected
            }

        }
    }

    public void testSetBinaryStreamJ() throws Exception {
        MockSerialBlob mockBlob = new MockSerialBlob();
        mockBlob.binaryStream = new ByteArrayOutputStream();
        SerialBlob serialBlob = new SerialBlob(mockBlob);
        OutputStream os = serialBlob.setBinaryStream(1);
        assertTrue(mockBlob.isSetBinaryStreamInvoked);
        assertEquals(1L, mockBlob.pos);
        assertSame(mockBlob.binaryStream, os);

        mockBlob.binaryStream = null;
        try {
            serialBlob.setBinaryStream(1);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        byte[] buf = new byte[1];
        serialBlob = new SerialBlob(buf);
        try {
            // Non bug difference from RI, Harmony-2971
            serialBlob.setBinaryStream(1);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
    }

    private void assertBlobPosition_BytePattern(Blob blob)
            throws SerialException, SQLException {
        byte[] pattern;
        long pos;

        pattern = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        pos = blob.position(pattern, 1);
        assertEquals(1, pos);

        pattern = new byte[] { 2, 3, 4 };
        pos = blob.position(pattern, 1);
        assertEquals(2, pos);
        pos = blob.position(pattern, 2);
        assertEquals(2, pos);
        pos = blob.position(pattern, 3);
        assertEquals(-1, pos);

        pattern = new byte[] { 2, 4 };
        pos = blob.position(pattern, 1);
        // RI's bug: RI doesn't returns -1 here even if the pattern can not be
        // found
        assertEquals(-1, pos);
        pos = blob.position(pattern, 3);
        assertEquals(-1, pos);

        pattern = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        pos = blob.position(pattern, 1);
        assertEquals(-1, pos);
        pos = blob.position(pattern, 3);
        assertEquals(-1, pos);

        pattern = new byte[] { 2, 3, 4 };
        pos = blob.position(pattern, 0);
        assertEquals(-1, pos);
        pos = blob.position(pattern, -1);
        assertEquals(-1, pos);

        // exceptional case
        pos = blob.position((byte[]) null, -1);
        assertEquals(-1, pos);
        try {
            pos = blob.position((byte[]) null, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    private void assertBlobPosition_BlobPattern(Blob blob)
            throws SerialException, SQLException {
        MockSerialBlob pattern = new MockSerialBlob();
        long pos;

        pattern.buf = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        pos = blob.position(pattern, 1);
        assertEquals(1, pos);

        pattern.buf = new byte[] { 2, 3, 4 };
        pos = blob.position(pattern, 1);
        assertEquals(2, pos);
        pos = blob.position(pattern, 2);
        assertEquals(2, pos);
        pos = blob.position(pattern, 3);
        assertEquals(-1, pos);

        pattern.buf = new byte[] { 2, 4 };
        pos = blob.position(pattern, 1);
        // RI's bug: RI doesn't returns -1 here even if the pattern can not be
        // found
        assertEquals(-1, pos);
        pos = blob.position(pattern, 3);
        assertEquals(-1, pos);

        pattern.buf = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        pos = blob.position(pattern, 1);
        assertEquals(-1, pos);
        pos = blob.position(pattern, 3);
        assertEquals(-1, pos);

        pattern.buf = new byte[] { 2, 3, 4 };
        pos = blob.position(pattern, 0);
        assertEquals(-1, pos);
        pos = blob.position(pattern, -1);
        assertEquals(-1, pos);

        // exceptional case
        try {
            pos = blob.position((Blob) null, -1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            pos = blob.position((Blob) null, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        MockSerialBlob abnormalBlob = new MockSerialBlob(true);
        try {
            blob.position(abnormalBlob, 1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            blob.position(abnormalBlob, -1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    static class MockSerialBlob implements Blob {
        public byte buf[] = new byte[1];

        public boolean isGetBytesInvoked;

        public boolean isSetBinaryStreamInvoked;

        public boolean isAbnormal;

        public OutputStream binaryStream;

        public long pos;

        public MockSerialBlob() {

        }

        public MockSerialBlob(boolean isAbnormal) {
            this.isAbnormal = isAbnormal;
        }

        public InputStream getBinaryStream() throws SQLException {
            return null;
        }

        public byte[] getBytes(long pos, int length) throws SQLException {
            isGetBytesInvoked = true;
            if (isAbnormal) {
                throw new SQLException();
            }
            return buf;
        }

        public long length() throws SQLException {
            return buf.length;
        }

        public long position(Blob pattern, long start) throws SQLException {
            return 0;
        }

        public long position(byte[] pattern, long start) throws SQLException {
            return 0;
        }

        public OutputStream setBinaryStream(long pos) throws SQLException {
            isSetBinaryStreamInvoked = true;
            this.pos = pos;
            return binaryStream;
        }

        public int setBytes(long pos, byte[] theBytes) throws SQLException {
            return 0;
        }

        public int setBytes(long pos, byte[] theBytes, int offset, int len)
                throws SQLException {
            return 0;
        }

        public void truncate(long len) throws SQLException {

        }

    }

}
