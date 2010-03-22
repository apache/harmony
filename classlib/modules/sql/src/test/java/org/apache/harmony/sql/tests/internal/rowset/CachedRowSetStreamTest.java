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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.sql.tests.internal.rowset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;

import javax.sql.rowset.CachedRowSet;

public class CachedRowSetStreamTest extends CachedRowSetTestCase {

    public final static int DEFAULT_COLUMN_COUNT = 3;

    public final static int DEFAULT_ROW_COUNT = 2;

    public void setUp() throws Exception {
        super.setUp();

        st = conn.createStatement();
        rs = conn.getMetaData().getTables(null, "APP", "STREAM", null);
        String createTableSQL = "create table STREAM (ID INTEGER NOT NULL, LONGVARCHAR_T LONG VARCHAR, "
                + "VARCHAR_FOR_BIT_T VARCHAR(100) FOR BIT DATA)";
        String alterTableSQL = "ALTER TABLE STREAM  ADD CONSTRAINT STREAM_PK Primary Key (ID)";

        if (!rs.next()) {
            st.execute(createTableSQL);
            st.execute(alterTableSQL);
        }

        st.executeUpdate("delete from STREAM");

        insertRow(1, "test1", null);
        insertRow(2, "test2", null);

        rs = st.executeQuery("select * from STREAM");
        try {
            crset = (CachedRowSet) Class.forName(
                    "com.sun.rowset.CachedRowSetImpl").newInstance();
            noInitialCrset = (CachedRowSet) Class.forName(
                    "com.sun.rowset.CachedRowSetImpl").newInstance();
        } catch (ClassNotFoundException e) {

            crset = (CachedRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.CachedRowSetImpl")
                    .newInstance();
            noInitialCrset = (CachedRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.CachedRowSetImpl")
                    .newInstance();

            System.setProperty("Testing Harmony", "true");
        }
        crset.populate(rs);

        rs = st.executeQuery("select * from STREAM");
        crset.setUrl(DERBY_URL);
    }

    private void insertRow(int id, String longVarchar, byte[] bs)
            throws SQLException {
        String insertSQL = "INSERT INTO STREAM(ID, LONGVARCHAR_T, VARCHAR_FOR_BIT_T) VALUES(?, ?, ?)";
        PreparedStatement preStmt = conn.prepareStatement(insertSQL);

        preStmt.setInt(1, id);
        preStmt.setString(2, longVarchar);
        preStmt.setBytes(3, bs);
        preStmt.executeUpdate();
        if (preStmt != null) {
            preStmt.close();
        }
    }

    /**
     * RI convert all no ascii char to char 0x3F
     * 
     * @throws Exception
     */
    public void testGetAsciiStream_Not_Ascii() throws Exception {
        String value = new String(new char[] { (char) 0xA4 });
        insertRow(100, value, null);
        rs = st.executeQuery("SELECT * FROM STREAM WHERE ID = 100");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.next();

        assertEquals(value, crset.getString(2));
        InputStream in = crset.getAsciiStream(2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }

        byte[] actual = out.toByteArray();

        assertEquals(1, actual.length);
        assertEquals(63, actual[0]);

        value = new String("\u4f60\u597d");
        insertRow(101, value, null);
        rs = st.executeQuery("SELECT * FROM STREAM WHERE ID = 101");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.next();

        assertEquals(value, crset.getString(2));
        in = crset.getAsciiStream(2);
        out = new ByteArrayOutputStream();
        i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }

        actual = out.toByteArray();

        assertEquals(2, actual.length);
        assertEquals(63, actual[0]);
        assertEquals(63, actual[1]);
    }

    public void testGetAsciiStream_Type_Mismatch() throws Exception {

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.absolute(3);

        try {
            crset.getAsciiStream(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getAsciiStream(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getAsciiStream(4);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getAsciiStream(5);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getAsciiStream(6);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getAsciiStream(7);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getAsciiStream(8);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getAsciiStream(9);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getAsciiStream(10);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getAsciiStream(11);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getAsciiStream(12);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
    }

    public void testGetAsciiStream_Longvarchar() throws Exception {
        String value = "It's is a very very very long long long story";
        insertRow(100, value, null);

        rs = st.executeQuery("SELECT * FROM STREAM WHERE ID = 100");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.next();

        InputStream in = crset.getAsciiStream(2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }

        byte[] expected = value.getBytes("ISO8859-1");

        byte[] actual = out.toByteArray();

        assertEquals(expected.length, actual.length);
        for (int j = 0; j < actual.length; j++) {
            assertEquals(expected[j], actual[j]);
        }

        crset.getInt(1);

        InputStream anotherIn = crset.getAsciiStream(2);
        assertNotNull(anotherIn);
        assertNotSame(in, anotherIn);

        out = new ByteArrayOutputStream();
        i = -1;
        while ((i = anotherIn.read()) != -1) {
            out.write(i);
        }

        actual = out.toByteArray();
        assertEquals(expected.length, actual.length);
        for (int j = 0; j < actual.length; j++) {
            assertEquals(expected[j], actual[j]);
        }
    }

    public void testGetAsciiStream_varchar() throws Exception {
        crset.absolute(1);

        String value = crset.getString(2);

        InputStream in = crset.getAsciiStream(2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }

        byte[] expected = value.getBytes("ISO8859-1");

        byte[] actual = out.toByteArray();

        assertEquals(expected.length, actual.length);
        for (int j = 0; j < actual.length; j++) {
            assertEquals(expected[j], actual[j]);
        }

        crset.getInt(1);

        InputStream anotherIn = crset.getAsciiStream(2);
        assertNotNull(anotherIn);
        assertNotSame(in, anotherIn);

        out = new ByteArrayOutputStream();
        i = -1;
        while ((i = anotherIn.read()) != -1) {
            out.write(i);
        }

        actual = out.toByteArray();
        assertEquals(expected.length, actual.length);
        for (int j = 0; j < actual.length; j++) {
            assertEquals(expected[j], actual[j]);
        }
    }

    public void testGetCharacterStream_Not_Ascii() throws Exception {
        String value = new String("\u548c\u8c10");
        insertRow(100, value, null);
        rs = st.executeQuery("SELECT * FROM STREAM WHERE ID = 100");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.next();

        assertEquals(value, crset.getString(2));

        Reader reader = crset.getCharacterStream(2);
        StringWriter writer = new StringWriter();
        int i = -1;
        while ((i = reader.read()) != -1) {
            writer.write(i);
        }
        assertEquals(value, writer.toString());

    }

    public void testGetUnicodeStream() throws Exception {
        byte[] bs = new byte[] { 1, 2, 3, 4, 5 };
        insertRow(100, "test", bs);

        rs = st.executeQuery("SELECT * FROM STREAM WHERE ID = 100");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.next();

        assertEquals("test", crset.getString(2));
        InputStream in = crset.getUnicodeStream(2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }

        byte[] expected = "test".getBytes("ISO8859-1");
        byte[] actual = out.toByteArray();

        assertEquals(expected.length, actual.length);

        for (int j = 0; j < actual.length; j++) {
            assertEquals(expected[j], actual[j]);
        }

        byte[] bytes = crset.getBytes(3);
        assertEquals(bs.length, bytes.length);
        for (int j = 0; j < bytes.length; j++) {
            assertEquals(bs[j], bytes[j]);
        }

        in = crset.getUnicodeStream(3);
        /*
         * TODO RI just using byte[].toString to construct
         * StringBufferInputStream, while Harmony using new String(byte[])
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            String value = new String(bs);
            StringBufferInputStream expectedIn = new StringBufferInputStream(
                    value);
            InputStream actualIn = crset.getUnicodeStream(3);

            i = -1;
            while ((i = expectedIn.read()) != -1) {
                assertEquals(i, actualIn.read());
            }
        }

        assertNotSame(crset.getCharacterStream(2), crset.getCharacterStream(2));
    }

    public void testGetUnicodeStream_Type_Mismatch() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        assertTrue(crset.absolute(3));

        try {
            crset.getUnicodeStream(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.getUnicodeStream(2);

        try {
            crset.getUnicodeStream(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getUnicodeStream(4);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getUnicodeStream(5);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getUnicodeStream(6);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getUnicodeStream(7);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getUnicodeStream(8);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getUnicodeStream(9);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getUnicodeStream(10);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getUnicodeStream(11);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getUnicodeStream(12);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
    }

    public void testGetUnicodeStream_Not_Ascii() throws Exception {
        String value = new String("\u548c\u8c10");
        insertRow(100, value, null);
        rs = st.executeQuery("SELECT * FROM STREAM WHERE ID = 100");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.next();

        assertTrue(crset.getObject(2) instanceof String);
        assertEquals(value, crset.getString(2));

        InputStream in = crset.getUnicodeStream(2);
        StringBufferInputStream sin = new StringBufferInputStream(value);

        int i = -1;
        while ((i = in.read()) != -1) {
            assertEquals(sin.read(), i);
        }

    }

    public void testGetCharacterStream() throws Exception {
        byte[] bs = new byte[] { 1, 2, 3, 4, 5 };
        insertRow(100, "test", bs);

        rs = st.executeQuery("SELECT * FROM STREAM WHERE ID = 100");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.next();

        assertEquals("test", crset.getString(2));
        Reader reader = crset.getCharacterStream(2);
        StringWriter writer = new StringWriter();
        int i = -1;
        while ((i = reader.read()) != -1) {
            writer.write(i);
        }
        assertEquals("test", writer.toString());

        reader = crset.getCharacterStream(3);
        writer = new StringWriter();
        i = -1;
        while ((i = reader.read()) != -1) {
            writer.write(i);
        }

        String value = new String(bs);

        assertEquals(value, writer.toString());

        assertNotSame(crset.getCharacterStream(2), crset.getCharacterStream(2));
    }

    public void testGetCharacterStream_Type_Mismatch() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        assertTrue(crset.absolute(3));

        try {
            crset.getCharacterStream(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.getCharacterStream(2);

        try {
            crset.getCharacterStream(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getCharacterStream(4);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getCharacterStream(5);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getCharacterStream(6);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getCharacterStream(7);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getCharacterStream(8);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getCharacterStream(9);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getCharacterStream(10);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getCharacterStream(11);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getCharacterStream(12);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
    }

    public void testGetBinaryStream() throws Exception {
        byte[] bs = new byte[] { 1, 2, 3, 4, 5 };
        insertRow(100, "test", bs);

        rs = st.executeQuery("SELECT * FROM STREAM WHERE ID = 100");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.next();

        try {
            crset.getBinaryStream(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        InputStream in = crset.getBinaryStream(3);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i = -1;
        while ((i = in.read()) != -1) {
            out.write(i);
        }

        byte[] actual = out.toByteArray();
        out.close();

        assertEquals(bs.length, actual.length);

        for (int j = 0; j < actual.length; j++) {
            assertEquals(bs[j], actual[j]);
        }
    }

    public void testGetBinaryStream_Type_Mismatch() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        assertTrue(crset.absolute(3));

        try {
            crset.getBinaryStream(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getBinaryStream(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getBinaryStream(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getBinaryStream(4);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getBinaryStream(5);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getBinaryStream(6);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.getBinaryStream(7);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getBinaryStream(8);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getBinaryStream(9);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getBinaryStream(10);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getBinaryStream(11);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.getBinaryStream(12);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
    }

    public void testUpdateAsciiStream() throws Exception {
        crset.next();

        try {
            crset.updateAsciiStream(3, new ByteArrayInputStream(
                    new byte[] { 1 }), 10);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            crset.updateAsciiStream(3, new ByteArrayInputStream(new byte[0]),
                    -3);
            fail("should throw NegativeArraySizeException");
        } catch (NegativeArraySizeException e) {
            // expected
        }

        try {
            crset
                    .updateAsciiStream(3,
                            new ByteArrayInputStream(new byte[0]), 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        crset.updateAsciiStream(3, new ByteArrayInputStream(new byte[] { 1, 2,
                3, 4, 5 }), 0);
        assertEquals(0, crset.getString(3).length());

        crset.updateAsciiStream(3, new ByteArrayInputStream(new byte[] { 1, 2,
                3, 4, 5 }), 2);

        Object obj = crset.getObject(3);
        assertTrue(obj instanceof String);
        char[] chars = ((String) obj).toCharArray();
        assertEquals(2, chars.length);
        for (int i = 0; i < chars.length; i++) {
            assertEquals(i + 1, chars[i]);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2,
                3, 4, 5 });
        crset.updateAsciiStream(2, in, 5);

        obj = crset.getObject(2);
        assertTrue(obj instanceof String);
        chars = ((String) obj).toCharArray();
        assertEquals(5, chars.length);
        for (int i = 0; i < chars.length; i++) {
            assertEquals(i + 1, chars[i]);
        }

        String value = new String("\u548c\u8c10");
        in = new ByteArrayInputStream(value.getBytes("ISO-8859-1"));
        crset.updateAsciiStream(2, in, in.available());

        obj = crset.getObject(2);
        assertTrue(obj instanceof String);

        byte[] bytes = ((String) obj).getBytes("ISO-8859-1");
        byte[] expected = value.getBytes("ISO-8859-1");
        assertEquals(expected.length, bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(expected[i], bytes[i]);
        }

        value = new String("\u548d\u8c1a");
        in = new ByteArrayInputStream(value.getBytes("ISO-8859-1"));
        crset.updateAsciiStream(3, in, in.available());

        obj = crset.getObject(3);
        assertTrue(obj instanceof String);
        bytes = ((String) obj).getBytes("ISO-8859-1");
        expected = value.getBytes("ISO-8859-1");
        assertEquals(expected.length, bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(expected[i], bytes[i]);
        }
    }

    public void testUpdateAsciiStream_Type_Mismatch() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.absolute(3);

        InputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        try {
            crset.updateAsciiStream(1, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateAsciiStream(3, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateAsciiStream(4, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateAsciiStream(5, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateAsciiStream(6, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateAsciiStream(7, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateAsciiStream(8, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateAsciiStream(9, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateAsciiStream(10, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateAsciiStream(11, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateAsciiStream(12, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
    }

    public void testUpdateBinaryStream() throws Exception {
        crset.next();

        try {
            crset.updateBinaryStream(3, new ByteArrayInputStream(
                    new byte[] { 0 }), -3);
            fail("should throw NegativeArraySizeException");
        } catch (NegativeArraySizeException e) {
            // expected
        }

        crset.updateBinaryStream(3, new ByteArrayInputStream(new byte[] { 1 }),
                10);

        byte[] actual = crset.getBytes(3);
        assertEquals(10, actual.length);
        assertEquals(1, actual[0]);
        for (int i = 1; i < 10; ++i) {
            assertEquals(0, actual[i]);
        }

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            /*
             * TODO RI will block here
             */
            crset.updateBinaryStream(3, new ByteArrayInputStream(new byte[] {
                    1, 2, 3 }), 1);
            actual = crset.getBytes(3);
            assertEquals(1, actual.length);
            assertEquals(1, actual[0]);
        }

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2,
                3, 4, 5 });

        try {
            crset.updateBinaryStream(2, in, 5);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        String value = new String("\u548c\u8c10");
        in = new ByteArrayInputStream(value.getBytes());
        crset.updateBinaryStream(3, in, in.available());

        Object obj = crset.getObject(3);
        assertTrue(obj instanceof byte[]);

        byte[] bs = ((byte[]) obj);
        byte[] expected = value.getBytes();

        assertEquals(expected.length, bs.length);
        for (int i = 0; i < bs.length; i++) {
            assertEquals(expected[i], bs[i]);
        }
    }

    public void testUpdateCharacterStream_Type_Mismatch() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.absolute(3);

        Reader in = new StringReader("Harmony");

        try {
            crset.updateCharacterStream(1, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateCharacterStream(2, in, 3);

        try {
            crset.updateCharacterStream(3, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateCharacterStream(4, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateCharacterStream(5, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateCharacterStream(6, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateCharacterStream(7, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateCharacterStream(8, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateCharacterStream(9, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateCharacterStream(10, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateCharacterStream(11, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateCharacterStream(12, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
    }

    public void testUpdateCharacterStream() throws Exception {
        crset.next();

        try {
            crset.updateCharacterStream(2, new StringReader("test"), 10);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        try {
            crset.updateCharacterStream(2, new StringReader("test"), -3);
            fail("should throw NegativeArraySizeException");
        } catch (NegativeArraySizeException e) {
            // expected
        }

        crset.updateCharacterStream(2, new StringReader("test"), 1);

        Object obj = crset.getObject(2);
        assertTrue(obj instanceof String);

        assertEquals("t", obj);

        Reader in = new StringReader("test");

        crset.updateCharacterStream(3, in, 4);

        obj = crset.getObject(3);
        assertTrue(obj instanceof String);
        assertEquals("test", obj);

        String value = new String("\u548c\u8c10");
        in = new StringReader(value);
        crset.updateCharacterStream(3, in, value.length());

        obj = crset.getObject(3);
        assertTrue(obj instanceof String);
        assertEquals(value, obj);
    }

    public void testUpdateBinaryStream_Type_Mismatch() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.absolute(3);

        InputStream in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        try {
            crset.updateBinaryStream(1, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateBinaryStream(2, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateBinaryStream(3, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateBinaryStream(4, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateBinaryStream(5, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateBinaryStream(6, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateBinaryStream(7, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateBinaryStream(8, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateBinaryStream(9, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateBinaryStream(10, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateBinaryStream(11, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
        try {
            crset.updateBinaryStream(12, in, 3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }
    }

    public void testSetBinaryStream() throws Exception {

        insertRow(3, "test3", new byte[] { 1, 2, 3 });
        crset = newNoInitialInstance();

        crset.setCommand("select * from STREAM where VARCHAR_FOR_BIT_T= ?");

        crset.setBinaryStream(1, new ByteArrayInputStream(
                new byte[] { 1, 2, 3 }), 3);
        if ("true".equals(System.getProperty("Testing Harmony"))) {

            crset.execute(conn);

            assertTrue(crset.next());

            assertEquals(3, crset.getInt(1));

            byte[] bs = crset.getBytes(3);
            assertEquals(1, bs[0]);
            assertEquals(2, bs[1]);
            assertEquals(3, bs[2]);
        } else {
            try {
                crset.execute(conn);
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // error message: An attempt was made to get a data value of
                // type 'VARCHAR ()
                // FOR BIT DATA' from a data value of type
                // 'java.io.InputStream(ASCII)'.
                /*
                 * TODO It seems RI invoke wrong method when it's
                 * setBinaryStream
                 */
            }
        }

    }

    public void testSetAsciiStream() throws Exception {

        crset = newNoInitialInstance();

        crset.setCommand("update STREAM set LONGVARCHAR_T=? where ID= ?");

        String value = "It's    is a very very very long long long story";
        byte[] bytes = value.getBytes("ISO-8859-1");
        crset.setAsciiStream(1, new ByteArrayInputStream(bytes), bytes.length);
        crset.setInt(2, 1);

        if ("true".equals(System.getProperty("Testing Harmony"))) {

            crset.execute(conn);
            rs = st.executeQuery("select * from STREAM where ID = 1");

            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
            assertEquals(value, rs.getString(2));
        } else {
            try {
                crset.execute(conn);
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // Unable to deduce param type
                /*
                 * TODO It seems RI doesn't support stream
                 */
            }
        }
    }

    public void testSetCharacterStream() throws Exception {

        crset = newNoInitialInstance();

        String value = new String("\u548c\u8c10");
        crset.setCommand("update STREAM set LONGVARCHAR_T=? where ID= ?");

        crset.setCharacterStream(1, new StringReader(value), value
                .toCharArray().length);
        crset.setInt(2, 1);

        crset.execute(conn);

        rs = st.executeQuery("select * from STREAM where ID = 1");

        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
        assertEquals(value, rs.getString(2));

    }
}
