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

package org.apache.harmony.sql.tests.javax.sql.rowset;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import javax.sql.rowset.BaseRowSet;
import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialRef;

import junit.framework.TestCase;

public class BaseRowSetTest extends TestCase {

    public void testGetParams() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(0, params.length);
    }

    /**
     * @tests {@link javax.sql.rowset.BaseRowSet#getFetchDirection()}
     */
    public void testGetFetchDirection() throws SQLException {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        assertEquals(ResultSet.FETCH_FORWARD, brs.getFetchDirection());
    }

    /**
     * @tests {@link javax.sql.rowset.BaseRowSet#getTypeMap()}
     */
    public void testGetTypeMap() {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        assertNull(brs.getTypeMap());
    }

    public void testSetNullintint() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setNull(1, Types.BINARY);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setNull(0, Types.BINARY);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.setNull(1, Types.BINARY);
        Object[] params = brs.getParams();
        assertNotNull(params);
        Object[] param = (Object[]) params[0];
        assertNotNull(param);
        assertEquals(2, param.length);
        assertNull(param[0]);
        assertEquals(Integer.valueOf(Types.BINARY), param[1]);
    }

    public void testSetNullintintString() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setNull(1, Types.BINARY, "java.lang.Boolean");
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setNull(0, Types.BINARY, "java.lang.Boolean");
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.setNull(1, Types.BINARY, "java.lang.Boolean");
        Object[] params = brs.getParams();
        assertNotNull(params);
        Object[] param = (Object[]) params[0];
        assertNotNull(param);
        assertEquals(3, param.length);
        assertNull(param[0]);
        assertEquals(Integer.valueOf(Types.BINARY), param[1]);
        assertEquals("java.lang.Boolean", param[2]);
    }

    public void testSetBoolean() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setBoolean(1, true);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setBoolean(0, true);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setBoolean("Hello", true);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setBoolean(1, true);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(Boolean.TRUE, params[0]);
    }

    public void testSetByte() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setByte(1, (byte) 1);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setByte(0, (byte) 1);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setByte("Hello", (byte) 1);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setByte(1, (byte) 1);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(Byte.valueOf((byte) 1), params[0]);
    }

    public void testSetBytes() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        byte[] theBytes = new byte[] { 1 };
        try {
            brs.setBytes(1, theBytes);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setBytes(0, theBytes);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setBytes("Hello", theBytes);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setBytes(1, theBytes);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(theBytes, params[0]);
    }

    public void testSetShort() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setShort(1, (short) 1);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setShort(0, (short) 1);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setShort("Hello", (short) 1);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setShort(1, (short) 1);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(Short.valueOf((short) 1), params[0]);
    }

    public void testSetInt() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setInt(1, 1);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setInt(0, 1);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setInt("Hello", 1);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setInt(1, 1);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(Integer.valueOf(1), params[0]);
    }

    public void testSetLong() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setLong(1, 1L);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setLong(0, 1L);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setLong("Hello", 1L);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setLong(1, 1L);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(Long.valueOf(1L), params[0]);
    }

    public void testSetFloat() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setFloat(1, 3.5F);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setFloat(0, 3.5F);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setFloat("Hello", 3.5F);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setFloat(1, 3.5F);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(Float.valueOf(3.5F), params[0]);
    }

    public void testSetDouble() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setDouble(1, 3.5);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setDouble(0, 3.5);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setDouble("Hello", 3.5);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setDouble(1, 3.5);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(Double.valueOf(3.5), params[0]);
    }

    public void testSetBigDecimal() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setBigDecimal(1, BigDecimal.TEN);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setBigDecimal(0, BigDecimal.TEN);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setBigDecimal("Hello", BigDecimal.TEN);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setBigDecimal(1, BigDecimal.TEN);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(BigDecimal.TEN, params[0]);
    }

    public void testSetString() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        String theString = "ABC";
        try {
            brs.setString(1, theString);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setString(0, theString);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setString("Hello", theString);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setString(1, theString);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(theString, params[0]);
    }

    public void testSetDate() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        Date theDate = new Date(200);
        try {
            brs.setDate(1, theDate);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setDate(0, theDate);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setDate("Hello", theDate);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setDate(1, theDate);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(theDate, params[0]);
    }

    public void testSetTime() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        Time theTime = new Time(200);
        try {
            brs.setTime(1, theTime);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setTime(0, theTime);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setTime("Hello", theTime);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setTime(1, theTime);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(theTime, params[0]);
    }

    public void testSetTimestamp() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        Timestamp theTimestamp = new Timestamp(200);
        try {
            brs.setTimestamp(1, theTimestamp);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setTimestamp(0, theTimestamp);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        try {
            brs.setTimestamp("Hello", theTimestamp);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setTimestamp(1, theTimestamp);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(theTimestamp, params[0]);
    }

    /**
     * @tests {@link javax.sql.rowset.BaseRowSet#setFetchDirection(int)}
     */
    public void testSetFetchDirectionI() throws SQLException {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        brs.setFetchDirection(ResultSet.FETCH_FORWARD);
        assertEquals(ResultSet.FETCH_FORWARD, brs.getFetchDirection());

        brs.setType(ResultSet.TYPE_SCROLL_SENSITIVE);
        brs.setFetchDirection(ResultSet.FETCH_UNKNOWN);
        assertEquals(ResultSet.FETCH_UNKNOWN, brs.getFetchDirection());

        brs.setType(ResultSet.TYPE_FORWARD_ONLY);
        try {
            brs.setFetchDirection(ResultSet.FETCH_REVERSE);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            brs.setFetchDirection(1100);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.BaseRowSet#setTypeMap(java.util.Map)}
     */
    public void testSetTypeMap() {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        brs.setTypeMap(null);
        assertNull(brs.getTypeMap());
    }

    public void testSetBinaryStream() throws Exception {
        InputStream testStream = new InputStream() {
            public int read() throws IOException {
                return 0;
            }
        };
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setBinaryStream(0, testStream);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setBinaryStream("Hello", testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setBinaryStream(1, testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setBinaryStream(1, testStream, 1);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(testStream, ((Object[]) params[0])[0]);
    }

    public void testSetAsciiStream() throws Exception {
        InputStream testStream = new InputStream() {
            public int read() throws IOException {
                return 0;
            }
        };
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setAsciiStream(0, testStream);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setAsciiStream("Hello", testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setAsciiStream(1, testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setAsciiStream(1, testStream, 1);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(testStream, ((Object[]) params[0])[0]);
    }

    public void testSetCharacterStream() throws Exception {
        Reader testReader = new Reader() {
            public void close() throws IOException {
            }

            public int read(char[] arg0, int arg1, int arg2) throws IOException {
                return 0;
            }
        };
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setCharacterStream(0, testReader);
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setCharacterStream("Hello", testReader);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setCharacterStream(1, testReader);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setCharacterStream(1, testReader, 1);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(testReader, ((Object[]) params[0])[0]);
    }

    public void testSetNCharacterStream() throws Exception {
        Reader testReader = new Reader() {
            public void close() throws IOException {
            }

            public int read(char[] arg0, int arg1, int arg2) throws IOException {
                return 0;
            }
        };
        BaseRowSetImpl brs = new BaseRowSetImpl();
        brs.initParams();

        try {
            brs.setNCharacterStream("Hello", testReader);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setNCharacterStream(1, testReader);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setNCharacterStream(1, testReader, 1);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }
    }

    public void testSetNString() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setNString(0, "hello");
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setNString("Hello", "hello");
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setNString(1, "hello");
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }
    }

    public void testSetObject() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        try {
            brs.setObject(0, "hello");
            fail("sql exception expected");
        } catch (SQLException e) {
        }

        brs.initParams();

        try {
            brs.setObject("Hello", "hello");
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setObject("Hello", "hello", 1);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setObject("Hello", "hello", 1, 1);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        brs.setObject(1, "hello");
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals("hello", params[0]);
        brs.setObject(2, "hello", 37);
        params = brs.getParams();
        assertNotNull(params);
        assertEquals(2, params.length);
        assertEquals("hello", ((Object[]) params[1])[0]);
        assertEquals(Integer.valueOf(37), ((Object[]) params[1])[1]);
        brs.setObject(3, "hello", 37, 54);
        params = brs.getParams();
        assertNotNull(params);
        assertEquals(3, params.length);
        assertEquals("hello", ((Object[]) params[2])[0]);
        assertEquals(Integer.valueOf(37), ((Object[]) params[2])[1]);
        assertEquals(Integer.valueOf(54), ((Object[]) params[2])[2]);
    }

    public void testSetRowID() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        RowId rowid = new RowId() {

            public byte[] getBytes() {
                return null;
            }
        };
        brs.initParams();

        try {
            brs.setRowId("Hello", rowid);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setRowId(1, rowid);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }
    }

    public void testSetSQLXML() throws SQLException {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        SQLXML sqlxml = new MockSQLXML() {

            public byte[] getBytes() {
                return null;
            }
        };
        brs.initParams();

        try {
            brs.setSQLXML("Hello", sqlxml);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setSQLXML(1, sqlxml);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }
    }

    public void testSetNClob() throws Exception {
        Reader testStream = new Reader() {
            public void close() throws IOException {
            }

            public int read(char[] arg0, int arg1, int arg2) throws IOException {
                return 0;
            }
        };
        BaseRowSetImpl brs = new BaseRowSetImpl();
        brs.initParams();

        try {
            brs.setNClob("Hello", testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setNClob(1, testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setNClob(1, testStream, 1);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setNClob(1, (NClob) null);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }
    }

    public void testSetURL() throws Exception {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        brs.initParams();
        try {
            brs.setURL(1, new URL("http:\\www.apache.org"));
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }
    }

    public void testSetArray() throws SQLException {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        brs.initParams();
        Array a = new MockArray();
        brs.setArray(1, a);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertTrue("Should have stored a SerialArray",
                params[0] instanceof SerialArray);
    }

    public void testSetBlob() throws SQLException {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        brs.initParams();
        Blob b = new MockBlob();
        brs.setBlob(1, b);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertTrue("Should have stored a SerialBlob",
                params[0] instanceof SerialBlob);

        InputStream testStream = new InputStream() {
            public int read() throws IOException {
                return 0;
            }
        };
        try {
            brs.setBlob("Hello", testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setBlob(1, testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setBlob(1, testStream, 1);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }
    }

    public void testSetClob() throws SQLException {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        brs.initParams();
        Clob c = new MockClob();
        brs.setClob(1, c);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertTrue(c != params[0]);
        assertTrue("Should have stored a SerialClob",
                params[0] instanceof SerialClob);

        Reader testStream = new Reader() {
            public void close() throws IOException {
            }

            public int read(char[] arg0, int arg1, int arg2) throws IOException {
                return 0;
            }
        };
        try {
            brs.setClob("Hello", testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setClob(1, testStream);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }

        try {
            brs.setClob(1, testStream, 1);
            fail("feature not supported exception expected");
        } catch (SQLFeatureNotSupportedException e) {
        }
    }

    public void testSetRef() throws SQLException {
        BaseRowSetImpl brs = new BaseRowSetImpl();
        brs.initParams();
        Ref r = new MockRef();
        brs.setRef(1, r);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertTrue(r != params[0]);
        assertTrue("Should have stored a SerialRef",
                params[0] instanceof SerialRef);
    }

    // Parameters should be cleared when setCommand()
    public void testSetCommand() throws SQLException {
        BaseRowSetImpl baseRowSet = new BaseRowSetImpl();
        baseRowSet.initParams();
        baseRowSet.setCommand("Test command ? and ?");
        baseRowSet.setString(1, "FirstParameter");
        baseRowSet.setString(2, "SecondParameter");
        Object[] params1 = baseRowSet.getParams();

        assertEquals("The number of parameters should be 2 after setting.", 2,
                params1.length);

        baseRowSet.setCommand("Test command 2 without parameter");
        Object[] params2 = baseRowSet.getParams();

        assertEquals(
                "The number of parameters should be 0 since command has been reset",
                0, params2.length);

    }

    // Since the maxSize is set to 0 by default and 0 represents no limit, we
    // can't just throw exception when we want to set size greater to 0,
    public void testSetFetchSize() throws SQLException {
        BaseRowSetImpl baseRowSet = new BaseRowSetImpl();
        baseRowSet.initParams();
        assertEquals(0, baseRowSet.getMaxRows());
        baseRowSet.setFetchSize(3);
        assertEquals("The fetch size should be set to 3.", 3, baseRowSet.getFetchSize());
        try {
            baseRowSet.setFetchSize(-1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }
    
    private static final class BaseRowSetImpl extends BaseRowSet {
        private static final long serialVersionUID = 1L;

        @Override
        protected void initParams() {
            super.initParams();
        }
    }
}