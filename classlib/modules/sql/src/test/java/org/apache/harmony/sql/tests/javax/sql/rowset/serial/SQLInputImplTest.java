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
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.sql.rowset.serial.SQLInputImpl;
import javax.sql.rowset.serial.SerialDatalink;

import junit.framework.TestCase;

import org.apache.harmony.sql.tests.javax.sql.rowset.MockArray;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockBlob;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockClob;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockRef;

public class SQLInputImplTest extends TestCase {

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#SQLInputImpl(Object[], Map)}
     */
    @SuppressWarnings("unchecked")
    public void test_Constructor() {

        try {
            new SQLInputImpl(null, new HashMap());
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            new SQLInputImpl(null, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            new SQLInputImpl(new Object[0], null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readArray()}
     */
    public void testReadArray() throws SQLException {
        Array array = new MockArray();
        Object[] attributes = new Object[] { array };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(array, impl.readArray());

        try {
            impl.readArray();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readArray());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readAsciiStream()}
     */
    public void testReadAsciiStream() throws SQLException {
        InputStream stream = new ByteArrayInputStream("abc".getBytes());
        Object[] attributes = new Object[] { stream };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(stream, impl.readAsciiStream());

        try {
            impl.readAsciiStream();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readAsciiStream());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readBigDecimal()}
     */
    public void testReadBigDecimal() throws SQLException {
        BigDecimal bd = new BigDecimal("12.5");
        Object[] attributes = new Object[] { bd };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(bd, impl.readBigDecimal());

        try {
            impl.readBigDecimal();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readBigDecimal());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readBinaryStream()}
     */
    public void testReadBinaryStream() throws SQLException {
        InputStream stream = new ByteArrayInputStream("abc".getBytes());
        Object[] attributes = new Object[] { stream };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(stream, impl.readBinaryStream());

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readBinaryStream());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readBlob()}
     */
    public void testReadBlob() throws SQLException {
        Blob blob = new MockBlob();
        Object[] attributes = new Object[] { blob };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(blob, impl.readBlob());

        try {
            impl.readBlob();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readBlob());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readBoolean()}
     */
    public void testReadBoolean() throws SQLException {
        Object[] attributes = new Object[] { Boolean.TRUE };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(true, impl.readBoolean());

        try {
            impl.readBoolean();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertFalse(impl.readBoolean());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readByte()}
     */
    public void testReadByte() throws SQLException {
        Object[] attributes = new Object[] { Byte.valueOf("3") };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals((byte) 3, impl.readByte());

        try {
            impl.readByte();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertEquals((byte) 0, impl.readByte());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readBytes()}
     */
    public void testReadBytes() throws SQLException {
        byte[] bytes = new byte[] { 1, 2, 3 };
        Object[] attributes = new Object[] { bytes };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(bytes, impl.readBytes());

        try {
            impl.readBytes();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readBytes());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readCharacterStream()}
     */
    public void testReadCharacterStream() throws SQLException {
        Reader stream = new StringReader("abc");
        Object[] attributes = new Object[] { stream };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(stream, impl.readCharacterStream());

        try {
            impl.readCharacterStream();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readCharacterStream());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readClob()}
     */
    public void testReadClob() throws SQLException {
        Clob clob = new MockClob();
        Object[] attributes = new Object[] { clob };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(clob, impl.readClob());

        try {
            impl.readClob();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readClob());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readDate()}
     */
    public void testReadDate() throws SQLException {
        Date date = new Date(12);
        Object[] attributes = new Object[] { date };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(date, impl.readDate());

        try {
            impl.readDate();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readDate());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readDouble()}
     */
    public void testReadDouble() throws SQLException {
        Object[] attributes = new Object[] { Double.valueOf("3") };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals((double) 3, impl.readDouble());

        try {
            impl.readDouble();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertEquals(0, impl.readDouble(), 0);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readFloat()}
     */
    public void testReadFloat() throws SQLException {
        Object[] attributes = new Object[] { Float.valueOf("3.5") };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals((float) 3.5, impl.readFloat());

        try {
            impl.readFloat();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertEquals(0f, impl.readFloat(), 0);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readInt()}
     */
    public void testReadInt() throws SQLException {
        Object[] attributes = new Object[] { Integer.valueOf("3") };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(3, impl.readInt());

        try {
            impl.readInt();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertEquals(0, impl.readInt());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readLong()}
     */
    public void testReadLong() throws SQLException {
        Object[] attributes = new Object[] { Long.valueOf("3") };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals((long) 3, impl.readLong());

        try {
            impl.readLong();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertEquals(0, impl.readLong());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readObject()}
     */
    public void testReadObject() throws SQLException {
        Object[] structAttributes = { "hello", Boolean.TRUE, "abc",
                Integer.valueOf(99) };
        Struct struct = new MockStruct(structAttributes,
                "harmonytests.MockSQLData");
        Struct struct2 = new MockStruct(structAttributes, "not stored name");
        HashMap<String, Class<?>> types = new HashMap<String, Class<?>>();
        types.put("harmonytests.MockSQLData", MockSQLData.class);
        Object[] attributes = new Object[] { struct, struct2, null, "xyz" };
        SQLInputImpl impl = new SQLInputImpl(attributes, types);
        Object obj = impl.readObject();
        assertTrue(obj instanceof MockSQLData);
        MockSQLData sqlData = (MockSQLData) obj;
        assertEquals(structAttributes[0], sqlData.firstAttribute);
        assertEquals(structAttributes[1], sqlData.secondAttribute);
        assertEquals(structAttributes[2], sqlData.thirdAttribute);
        assertEquals(structAttributes[3], sqlData.fourthAttribute);
        Object obj2 = impl.readObject();
        assertEquals(struct2, obj2);
        Object obj3 = impl.readObject();
        assertNull(obj3);
        Object obj4 = impl.readObject();
        assertEquals(attributes[3], obj4);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readRef()}
     */
    public void testReadRef() throws SQLException {
        Ref ref = new MockRef();
        Object[] attributes = new Object[] { ref };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(ref, impl.readRef());

        try {
            impl.readRef();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readRef());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readShort()}
     */
    public void testReadShort() throws SQLException {
        Object[] attributes = new Object[] { Short.valueOf("3") };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals((short) 3, impl.readShort());

        try {
            impl.readShort();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertEquals((short) 0, impl.readShort());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readString()}
     */
    public void testReadString() throws SQLException {
        Object[] attributes = new Object[] { "hello" };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals("hello", impl.readString());

        try {
            impl.readString();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readString());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readTime()}
     */
    public void testReadTime() throws SQLException {
        Time time = new Time(345);
        Object[] attributes = new Object[] { time };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(time, impl.readTime());

        try {
            impl.readTime();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        attributes = new Object[] { null };
        impl = new SQLInputImpl(attributes, new HashMap<String, Class<?>>());
        assertNull(impl.readTime());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readTimestamp()}
     */
    public void testReadTimestamp() throws SQLException {
        Timestamp time = new Timestamp(345);
        Object[] attributes = new Object[] { time };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertEquals(time, impl.readTimestamp());

        try {
            impl.readTimestamp();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#readURL()}
     */
    public void testReadURL() throws SQLException, MalformedURLException {
        URL url = new URL("http://www.apache.org");
        SerialDatalink link = new SerialDatalink(url);
        Object[] attributes = new Object[] { link };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        try {
            impl.readURL();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            impl.readURL();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLInputImpl#wasNull()}
     */
    public void testWasNull() throws SQLException {
        Object[] attributes = new Object[] { null, "hello" };
        SQLInputImpl impl = new SQLInputImpl(attributes,
                new HashMap<String, Class<?>>());
        assertFalse(impl.wasNull());
        assertEquals(null, impl.readString());
        assertTrue(impl.wasNull());
        assertEquals("hello", impl.readString());
        assertFalse(impl.wasNull());
        try {
            impl.readString();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        assertFalse(impl.wasNull());
        assertFalse(impl.wasNull());
    }

    /*
     * Mock classes used by both this test and SQLOutputImplTest to test
     * readObject and writeObject methods respectively.
     */

    public static class MockStruct implements Struct {

        private String sqlTypeName;

        public MockStruct(Object[] attributes, String sqlTypeName) {
            this.attributes = attributes;
            this.sqlTypeName = sqlTypeName;
        }

        Object[] attributes;

        public Object[] getAttributes() throws SQLException {
            return attributes;
        }

        public Object[] getAttributes(Map<String, Class<?>> theMap)
                throws SQLException {
            return attributes;
        }

        public String getSQLTypeName() throws SQLException {
            return sqlTypeName;
        }
    }

    public static class MockSQLData implements SQLData {

        public String firstAttribute;

        public Boolean secondAttribute;

        public String thirdAttribute;

        public Integer fourthAttribute;

        public String getSQLTypeName() throws SQLException {
            return "harmonytests.MockSQLData";
        }

        public void readSQL(SQLInput stream, String typeName)
                throws SQLException {
            firstAttribute = stream.readString();
            secondAttribute = stream.readBoolean();
            thirdAttribute = stream.readString();
            fourthAttribute = stream.readInt();
        }

        public void writeSQL(SQLOutput stream) throws SQLException {
            stream.writeString(firstAttribute);
            stream.writeBoolean(secondAttribute);
            stream.writeString(thirdAttribute);
            stream.writeInt(fourthAttribute);
        }
    }
}
