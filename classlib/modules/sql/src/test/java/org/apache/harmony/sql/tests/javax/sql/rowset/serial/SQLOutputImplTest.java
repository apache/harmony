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
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.sql.rowset.serial.SQLOutputImpl;
import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialDatalink;
import javax.sql.rowset.serial.SerialRef;
import javax.sql.rowset.serial.SerialStruct;

import junit.framework.TestCase;

import org.apache.harmony.sql.tests.javax.sql.rowset.MockArray;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockBlob;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockClob;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockNClob;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockRef;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockRowId;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockSQLXML;
import org.apache.harmony.sql.tests.javax.sql.rowset.serial.SQLInputImplTest.MockSQLData;
import org.apache.harmony.sql.tests.javax.sql.rowset.serial.SQLInputImplTest.MockStruct;

public class SQLOutputImplTest extends TestCase {

    private static SQLOutputImpl impl;

    private static Vector attr;

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#SQLOutputImpl(Vector, Map)}
     */
    public void test_ConstructorLjava_util_VectorLjava_util_Map() {
        assertNotNull(impl);

        try {
            new SQLOutputImpl(null, new HashMap());
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            new SQLOutputImpl(null, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            new SQLOutputImpl(new Vector(), null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeArray(Array)}
     */
    public void test_writeArrayLjava_sql_Array() throws SQLException {
        Array array = new MockArray();
        impl.writeArray(array);
        assertEquals(1, attr.size());
        assertTrue(attr.get(0) instanceof SerialArray);

        impl.writeArray(null);
        assertNull(attr.get(1));
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeAsciiStream(InputStream)}
     */
    public void test_writeAsciiStreamLjava_io_InputStream() throws SQLException {
        InputStream stream = new ByteArrayInputStream("abc".getBytes());
        impl.writeAsciiStream(stream);
        assertEquals(1, attr.size());
        assertEquals("abc", attr.get(0));

        try {
            impl.writeAsciiStream(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeBigDecimal(java.math.BigDecimal)}
     */
    public void test_writeBigDecimalLjava_math_BigDecimal() throws SQLException {
        impl.writeBigDecimal(BigDecimal.ONE);
        impl.writeBigDecimal(BigDecimal.ONE);
        assertSame(attr.get(0), attr.get(1));

        impl.writeBigDecimal(null);
        assertNull(attr.get(2));
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeBinaryStream(InputStream)}
     */
    public void test_writeBinaryStreamLjava_io_InputStream()
            throws SQLException {
        InputStream stream = new ByteArrayInputStream("abc".getBytes());
        impl.writeBinaryStream(stream);
        assertEquals(1, attr.size());
        assertEquals("abc", attr.get(0));
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeBlob(Blob)}
     */
    public void test_writeBlobLjava_sql_Blob() throws SQLException {
        Blob blob = new MockBlob();
        impl.writeBlob(blob);
        assertEquals(1, attr.size());
        assertTrue(attr.get(0) instanceof SerialBlob);

        impl.writeBlob(null);
        assertNull(attr.get(1));
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeBoolean(boolean)}
     */
    public void test_writeBooleanZ() throws SQLException {
        impl.writeBoolean(true);
        assertTrue(((Boolean) attr.get(0)).booleanValue());

        impl.writeBoolean(false);
        assertFalse(((Boolean) attr.get(1)).booleanValue());

        impl.writeBoolean(true);
        assertTrue(((Boolean) attr.get(2)).booleanValue());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeByte(byte)}
     */
    public void test_writeByteB() throws SQLException {
        assertEquals(0, attr.size());
        impl.writeByte((byte) 1);
        assertEquals(1, attr.size());
        assertEquals(Byte.parseByte("1"), ((Byte) attr.get(0)).byteValue());

        impl.writeByte((byte) 256);
        assertEquals(2, attr.size());
        assertEquals(Byte.parseByte("0"), ((Byte) attr.get(1)).byteValue());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeBytes(byte[])}
     */
    public void test_writeBytes$B() throws SQLException {
        byte[] bytes = new byte[] { 4, 5, (byte) 256 };
        impl.writeBytes(bytes);
        assertEquals(1, attr.size());

        byte[] attrBytes = (byte[]) attr.get(0);
        assertEquals((byte) 4, attrBytes[0]);
        assertEquals((byte) 0, attrBytes[2]);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeCharacterStream(Reader))}
     */
    public void test_writeCharacterStreamLjava_io_Reader() throws SQLException {
        Reader stream = new StringReader("abc");
        impl.writeCharacterStream(stream);
        assertEquals(1, attr.size());
        assertEquals("abc", attr.get(0));
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeClob(Clob)}
     */
    public void test_writeClobLjava_sql_Clob() throws SQLException {
        Clob clob = new MockClob();
        impl.writeClob(clob);
        assertEquals(1, attr.size());
        assertTrue(attr.get(0) instanceof SerialClob);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeDate(Date)}
     */
    public void test_writeDateLjava_sql_Date() throws SQLException {
        Date date = new Date(200);
        impl.writeDate(date);
        assertEquals(1, attr.size());
        assertEquals(attr.get(0), date);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeDouble(double)}
     */
    public void test_writeDoubleD() throws SQLException {
        Object obj = new Object();
        attr.add(obj);
        impl.writeDouble(3.1415926);
        assertEquals(2, attr.size());

        assertEquals(obj, attr.get(0));
        assertEquals(3.1415926, ((Double) attr.get(1)).doubleValue(), 0);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeFloat(float)}
     */
    public void test_writeFloatF() throws SQLException {
        impl.writeFloat(3.14f);
        assertEquals(3.14f, ((Float) attr.get(0)).floatValue(), 0);

        impl.writeFloat(Float.MAX_VALUE);
        assertEquals(3.14f, ((Float) attr.get(0)).floatValue(), 0);
        assertEquals(Float.MAX_VALUE, ((Float) attr.get(1)).floatValue(), 0);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeInt(int)}
     */
    public void test_writeIntI() throws SQLException {
        impl.writeInt(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, ((Integer) attr.get(0)).intValue());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeNClob(java.sql.NClob)}
     * 
     * @since 1.6
     */
    public void test_writeNClobLjava_sql_NClob() throws SQLException {
        try {
            impl.writeNClob(null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            impl.writeNClob(new MockNClob());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeNString(String)}
     * 
     * @since 1.6
     */
    public void test_writeNStringLjava_sql_String() throws SQLException {
        try {
            impl.writeNString(null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            impl.writeNString("testString");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeLong(long)}
     */
    public void test_writeLongJ() throws SQLException {
        impl.writeInt(Integer.MIN_VALUE);
        impl.writeLong(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, ((Long) attr.get(1)).longValue());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeObject(java.sql.SQLData)}
     */
    public void test_writeObjectLjava_sql_SQLData() throws SQLException {
        MockSQLData sqlData = new MockSQLData();
        sqlData.firstAttribute = "one";
        sqlData.secondAttribute = Boolean.FALSE;
        sqlData.thirdAttribute = "three";
        sqlData.fourthAttribute = Integer.valueOf(4);
        impl.writeObject(sqlData);
        assertTrue(attr.get(0) instanceof SerialStruct);
        SerialStruct struct = (SerialStruct) attr.get(0);
        Object[] attributes = struct.getAttributes();
        assertEquals(sqlData.firstAttribute, attributes[0]);
        assertEquals(sqlData.secondAttribute, attributes[1]);
        assertEquals(sqlData.thirdAttribute, attributes[2]);
        assertEquals(sqlData.fourthAttribute, attributes[3]);
        assertEquals("harmonytests.MockSQLData", struct.getSQLTypeName());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeRef(Ref)}
     */
    public void test_writeRefLjava_sql_Ref() throws SQLException {
        Ref ref = new MockRef();
        impl.writeRef(ref);
        assertEquals(1, attr.size());
        assertTrue(attr.get(0) instanceof SerialRef);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeRowId(java.sql.RowId)}
     * 
     * @since 1.6
     */
    public void test_writeRowIdLjava_sql_RowId() throws SQLException {
        try {
            impl.writeRowId(null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            impl.writeRowId(new MockRowId());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeShort(short)}
     */
    public void test_writeShortS() throws SQLException {
        impl.writeShort((short) 12823);
        assertEquals((short) 12823, ((Short) attr.get(0)).shortValue());
        impl.writeShort((short) 32768);
        assertEquals((short) -32768, ((Short) attr.get(1)).shortValue());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeSQLXML(java.sql.SQLXML)}
     * 
     * @since 1.6
     */
    public void test_writeSQLXMLLjava_sql_SQLXML() throws SQLException {
        try {
            impl.writeSQLXML(null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            impl.writeSQLXML(new MockSQLXML());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeString(String)}
     */
    public void test_writeStringLjava_lang_String() throws SQLException {
        impl.writeString("abc");
        assertEquals("abc", ((String) attr.get(0)));
        impl.writeString("def");
        assertEquals("def", ((String) attr.get(1)));
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeStruct(java.sql.Struct)}
     */
    public void test_writeStructLjava_sql_Struct() throws SQLException {
        Struct struct = new MockStruct(new Object[] {}, "mockStruct1");
        impl.writeStruct(struct);
        assertEquals(1, attr.size());
        assertTrue(attr.get(0) instanceof SerialStruct);
        SerialStruct ss = (SerialStruct) attr.get(0);
        assertEquals(0, ss.getAttributes().length);
        assertEquals("mockStruct1", ss.getSQLTypeName());
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeTime(Time)}
     */
    public void test_writeTimeLjava_sql_Time() throws SQLException {
        Time time = new Time(200);
        impl.writeTime(time);
        assertEquals(1, attr.size());
        assertEquals(attr.get(0), time);
    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeTimestamp(Timestamp)}
     */
    public void test_writeTimestampLjava_sql_Timestamp() throws SQLException {
        Timestamp time = new Timestamp(200);
        impl.writeTimestamp(time);
        assertEquals(1, attr.size());
        assertEquals(attr.get(0), time);

    }

    /**
     * @tests {@link javax.sql.rowset.serial.SQLOutputImpl#writeURL(URL)}
     */
    public void test_writeURLLjava_net_URL() throws MalformedURLException,
            SQLException {
        URL url = new URL("http://www.apache.org");
        impl.writeURL(url);
        assertEquals(1, attr.size());
        assertTrue(attr.get(0) instanceof SerialDatalink);
        assertEquals(url, ((SerialDatalink) attr.get(0)).getDatalink());

        impl.writeURL(null);
        assertNull(attr.get(1));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        attr = new Vector();
        Map<String, Class<?>> map = new HashMap<String, Class<?>>();
        impl = new SQLOutputImpl(attr, map);
    }

    @Override
    protected void tearDown() throws Exception {
        impl = null;
        attr = null;
        super.tearDown();
    }
}
