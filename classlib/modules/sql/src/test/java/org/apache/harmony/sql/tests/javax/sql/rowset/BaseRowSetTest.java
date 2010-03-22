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

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
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

        brs.setByte(1, (byte) 1);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(Byte.valueOf((byte) 1), params[0]);
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

        brs.setShort(1, (byte) 1);
        Object[] params = brs.getParams();
        assertNotNull(params);
        assertEquals(1, params.length);
        assertEquals(Short.valueOf((short) 1), params[0]);
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
