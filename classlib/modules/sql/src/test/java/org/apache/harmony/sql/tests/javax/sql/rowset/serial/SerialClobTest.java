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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;

import org.apache.harmony.sql.tests.javax.sql.rowset.MockClob;

import junit.framework.TestCase;

public class SerialClobTest extends TestCase {

    public void testSerialClob$C() throws Exception {
        SerialClob serialClob = new SerialClob(new char[8]);
        assertEquals(8, serialClob.length());

        try {
            new SerialClob((char[]) null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testSerialClobLClob() throws Exception {
        SerialClob serialClob;
        MockSerialClob mockClob = new MockSerialClob();

        mockClob.characterStreamReader = new CharArrayReader(mockClob.buf);
        mockClob.asciiInputStream = new ByteArrayInputStream(new byte[] { 1 });
        serialClob = new SerialClob(mockClob);
        assertEquals(mockClob.buf.length, serialClob.length());

        mockClob.characterStreamReader = null;
        mockClob.asciiInputStream = new ByteArrayInputStream(new byte[] { 1 });
        mockClob.characterStreamReader = new CharArrayReader(new char[] { 1 });
        mockClob.asciiInputStream = null;
        mockClob.characterStreamReader = new MockAbnormalReader();
        mockClob.asciiInputStream = new ByteArrayInputStream(new byte[] { 1 });
        try {
            new SerialClob(mockClob);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            new SerialClob((Clob) null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

    }

    public void testLength() {
        // length() has been tested in constructor test.
    }

    public void testGetAsciiStream() throws Exception {
        MockSerialClob mockClob = new MockSerialClob();
        mockClob.characterStreamReader = new CharArrayReader(mockClob.buf);
        mockClob.asciiInputStream = new ByteArrayInputStream(new byte[] { 1 });
        SerialClob serialClob = new SerialClob(mockClob);
        InputStream is = serialClob.getAsciiStream();
        assertTrue(mockClob.isGetAsciiStreamInvoked);
        assertEquals(mockClob.asciiInputStream, is);

        try {
            serialClob = new SerialClob("helloo".toCharArray());
            serialClob.getAsciiStream();
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

    }

    public void testGetCharacterStream() throws Exception {
        char[] buf = "helloo".toCharArray();
        SerialClob serialClob = new SerialClob(buf);

        Reader reader = serialClob.getCharacterStream();
        char[] data = new char[buf.length];
        int read = reader.read(data);
        assertEquals(buf.length, read);
        assertTrue(Arrays.equals(buf, data));
        assertFalse(reader.ready());

        MockSerialClob mockClob = new MockSerialClob();
        mockClob.characterStreamReader = new CharArrayReader(mockClob.buf);
        mockClob.asciiInputStream = new ByteArrayInputStream(new byte[] { 1 });
        mockClob.asciiOutputStream = new ByteArrayOutputStream();
        serialClob = new SerialClob(mockClob);
        reader = serialClob.getCharacterStream();
        data = new char[mockClob.buf.length];
        read = reader.read(data);
        assertEquals(mockClob.buf.length, read);
        assertTrue(Arrays.equals(mockClob.buf, data));
        assertFalse(reader.ready());
    }

    public void testGetSubString() throws Exception {
        SerialClob serialClob = new SerialClob("hello".toCharArray());

        String sub = serialClob.getSubString(1, 5);
        assertEquals("hello", sub);

        sub = serialClob.getSubString(2, 3);
        assertEquals("ell", sub);

        try {
            sub = serialClob.getSubString(0, 6);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            sub = serialClob.getSubString(7, 1);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            sub = serialClob.getSubString(1, 7);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            sub = serialClob.getSubString(1, -2);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
        try {
            sub = serialClob.getSubString(3, 4);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        LongLengthClob longClob = new LongLengthClob();
        serialClob = new SerialClob(longClob);

        try {
            serialClob.getSubString(1, 3);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

    }

    public void testPositionLClobJ() throws Exception {
        SerialClob serialClob = new SerialClob("helloo".toCharArray());
        SerialClob searchClob = new SerialClob("llo".toCharArray());
        long pos = serialClob.position(searchClob, 1);
        assertEquals(3, pos);

        pos = serialClob.position(searchClob, 3);
        assertEquals(3, pos);

        searchClob = new SerialClob("o".toCharArray());
        pos = serialClob.position(searchClob, 6);
        assertEquals(6, pos);

        searchClob = new SerialClob("ooooooo".toCharArray());
        pos = serialClob.position(searchClob, 1);
        assertEquals(-1, pos);

        searchClob = new SerialClob("llo".toCharArray());
        pos = serialClob.position(searchClob, 4);
        assertEquals(-1, pos);

        pos = serialClob.position(searchClob, 0);
        assertEquals(-1, pos);

        pos = serialClob.position(searchClob, -1);
        assertEquals(-1, pos);

        pos = serialClob.position(searchClob, 10);
        assertEquals(-1, pos);
    }

    public void testPositionLStringJ() throws Exception {
        SerialClob serialClob = new SerialClob("helloo".toCharArray());

        long pos = serialClob.position("llo", 1);
        assertEquals(3, pos);

        pos = serialClob.position("llo", 3);
        assertEquals(3, pos);

        pos = serialClob.position("o", 6);
        assertEquals(6, pos);

        pos = serialClob.position("ooooooo", 1);
        assertEquals(-1, pos);

        pos = serialClob.position("llo", 4);
        assertEquals(-1, pos);

        pos = serialClob.position("llo", 0);
        assertEquals(-1, pos);

        pos = serialClob.position("llo", -1);
        assertEquals(-1, pos);

        pos = serialClob.position("llo", 10);
        assertEquals(-1, pos);
    }

    public void testSetAsciiStream() throws Exception {
        MockSerialClob mockClob = new MockSerialClob();
        mockClob.characterStreamReader = new CharArrayReader(mockClob.buf);
        mockClob.asciiInputStream = new ByteArrayInputStream(new byte[] { 1 });
        SerialClob serialClob = new SerialClob(mockClob);
        OutputStream os = null;
        try {
            os = serialClob.setAsciiStream(1);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
        mockClob.asciiOutputStream = new ByteArrayOutputStream();
        os = serialClob.setAsciiStream(1);
        assertNotNull(os);
        assertTrue(mockClob.isSetAsciiStreamInvoked);
        assertEquals(mockClob.asciiOutputStream, os);

        try {
            serialClob = new SerialClob("helloo".toCharArray());
            // Harmony-3491, non bug difference from RI
            serialClob.setAsciiStream(1);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
    }

    public void testSetCharacterStream() throws Exception {
        MockSerialClob mockClob = new MockSerialClob();
        mockClob.characterStreamReader = new CharArrayReader(mockClob.buf);
        mockClob.asciiInputStream = new ByteArrayInputStream(new byte[] { 1 });
        mockClob.characterStreamWriter = new CharArrayWriter();
        SerialClob serialClob = new SerialClob(mockClob);
        Writer writer = serialClob.setCharacterStream(1);
        assertTrue(mockClob.isSetCharacterStreamInvoked);
        assertEquals(mockClob.characterStreamWriter, writer);

        try {
            serialClob = new SerialClob("helloo".toCharArray());
            // Harmony-3491, non bug difference from RI
            serialClob.setCharacterStream(1);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
    }

    public void testSetStringJLString() throws Exception {
        String s = "hello";
        char[] buf = s.toCharArray();
        SerialClob serialClob = new SerialClob(buf);

        int count = serialClob.setString(1, "olleh");
        String sub = serialClob.getSubString(1, 5);
        assertEquals("olleh", sub);
        assertEquals(5, count);

        count = serialClob.setString(2, "mm");
        sub = serialClob.getSubString(1, 5);
        assertEquals("ommeh", sub);
        assertEquals(2, count);

        try {
            serialClob.setString(-1, "hello");
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            serialClob.setString(6, "hello");
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        // Harmony-3335, non bug difference from RI
        try {
            serialClob.setString(2, "hello");
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
    }

    public void testSetStringJLStringII() throws Exception {
        SerialClob serialClob = new SerialClob("hello".toCharArray());
        int count = serialClob.setString(1, "olleh", 0, 5);
        String sub = serialClob.getSubString(1, 5);
        assertEquals("olleh", sub);
        assertEquals(5, count);

        count = serialClob.setString(2, "mmnn", 1, 2);
        sub = serialClob.getSubString(1, 5);
        // RI's bug
        assertEquals(2, count);
        assertEquals("omneh", sub);

        try {
            serialClob.setString(-1, "hello", 0, 5);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            serialClob.setString(6, "hello", 0, 5);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            serialClob.setString(1, "hello", 0, 6);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        // Harmony-3335, non bug difference from RI
        try {
            serialClob.setString(2, "hello", 0, 5);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            // Harmony-3335
            serialClob.setString(1, "hello", -1, 5);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
    }

    public void testTruncate() throws Exception {
        SerialClob serialClob = new SerialClob("hello".toCharArray());
        serialClob.truncate(3);
        assertEquals(3, serialClob.length());
        String s = serialClob.getSubString(1, 3);
        assertEquals("hel", s);
        serialClob.truncate(0);
        assertEquals(0, serialClob.length());

        serialClob = new SerialClob("hello".toCharArray());
        try {
            serialClob.truncate(10);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }

        try {
            serialClob.truncate(-1);
            fail("should throw SerialException");
        } catch (SerialException e) {
            // expected
        }
    }

    private static class LongLengthClob extends MockClob {
        @Override
        public long length() throws SQLException {
            return (long) Integer.MAX_VALUE * (long) 2 + 4;
        }
    }

    static class MockSerialClob implements Clob {

        public char[] buf = { 1, 2, 3 };

        public Reader characterStreamReader;

        public Writer characterStreamWriter;

        public InputStream asciiInputStream;

        public OutputStream asciiOutputStream;

        public boolean isGetAsciiStreamInvoked;

        public boolean isSetAsciiStreamInvoked;

        public boolean isSetCharacterStreamInvoked;

        public MockSerialClob() {
        }

        public InputStream getAsciiStream() throws SQLException {
            isGetAsciiStreamInvoked = true;
            return asciiInputStream;
        }

        public Reader getCharacterStream() throws SQLException {
            return characterStreamReader;
        }

        public String getSubString(long pos, int length) throws SQLException {
            return null;
        }

        public long length() throws SQLException {
            return buf.length;
        }

        public long position(Clob searchstr, long start) throws SQLException {
            return 0;
        }

        public long position(String searchstr, long start) throws SQLException {
            return 0;
        }

        public OutputStream setAsciiStream(long pos) throws SQLException {
            isSetAsciiStreamInvoked = true;
            return asciiOutputStream;
        }

        public Writer setCharacterStream(long pos) throws SQLException {
            isSetCharacterStreamInvoked = true;
            return characterStreamWriter;
        }

        public int setString(long pos, String str) throws SQLException {
            return 0;
        }

        public int setString(long pos, String str, int offset, int len)
                throws SQLException {
            return 0;
        }

        public void truncate(long len) throws SQLException {

        }
    }

    static class MockAbnormalReader extends java.io.Reader {
        public int read(char[] cbuf, int off, int len) throws IOException {
            throw new IOException();
        }

        public void close() throws IOException {

        }
    }
}
