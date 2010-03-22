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

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;

import junit.framework.TestCase;

public class SerialArrayTest extends TestCase {

    private MockArray mock = new MockArray();

    private SerialArray sa;

    Object[] testElements = new Object[4];

    Map<String, Class<?>> map = new HashMap<String, Class<?>>();

    Map<String, Class<?>> badmap = new HashMap<String, Class<?>>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testElements = new Object[4];
        testElements[0] = "test1";
        testElements[1] = "test2";
        testElements[2] = new SQLException();
        testElements[3] = new HashMap();
        sa = new SerialArray(mock);
        map = new HashMap<String, Class<?>>();
        map.put("String", MockStringSQLData.class);
        map.put("Object", null);
        badmap = new HashMap<String, Class<?>>();
        badmap.put("Something", HashMap.class);
    }

    public void testConstructor_ObjectArray() throws SQLException {
        assertNotNull(new SerialArray(new SQLArray()));

        // OK
        sa = new SerialArray(mock);
        // array.getArray should not return null
        try {
            sa = new SerialArray(new MockNullArray());
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        testElements = new Object[5];
        testElements[0] = "test1";
        testElements[1] = "test2";
        testElements[2] = new SQLException();
        testElements[3] = new HashMap();
        try {
            sa = new SerialArray(mock);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        testElements[4] = new Object();
        try {
            sa = new SerialArray(mock);
            // RI fail here
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    public void testConstructor_IntArray() throws SQLException {
        mock.basetype = Types.INTEGER;
        // OK
        sa = new SerialArray(mock);
        // array.getArray should not return null
        try {
            sa = new SerialArray(new MockNullArray());
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        testElements = new Object[5];
        testElements[0] = "test1";
        testElements[1] = "test2";
        testElements[2] = new SQLException();
        testElements[3] = new HashMap();
        // OK for integer
        sa = new SerialArray(mock);

        testElements[4] = new Object();
        sa = new SerialArray(mock);
    }

    public void testConstructor_map() throws SQLException {
        try {
            sa = new SerialArray(mock, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        // array.getArray should not return null
        try {
            sa = new SerialArray(new MockNullArray(), null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        // OK
        sa = new SerialArray(mock, map);
        sa = new SerialArray(mock, badmap);
    }

    public void testGetArray() throws SerialException {
        for (int i = 0; i < testElements.length; i++) {
            assertEquals(testElements[i], ((Object[]) sa.getArray())[i]);
        }
    }

    public void testGetArrayMap() throws SerialException {
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i], ((Object[]) sa.getArray(null))[i]);
        }
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i], ((Object[]) sa.getArray(map))[i]);
        }
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i], ((Object[]) sa.getArray(badmap))[i]);
        }

    }

    public void testGetArrayLongInt() throws SerialException {
        for (int i = 0; i < testElements.length; i++) {
            assertEquals(testElements[i], ((Object[]) sa.getArray(i, 1))[0]);
        }
    }

    public void testGetArrayLongIntMap() throws SerialException {
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i], ((Object[]) sa.getArray(i, 1, null))[0]);
        }
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i], ((Object[]) sa.getArray(i, 1, map))[0]);
        }
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i],
                    ((Object[]) sa.getArray(i, 1, badmap))[0]);
        }
        mock.returnNull = true;
        // elements force deeper copy
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i], ((Object[]) sa.getArray(i, 1, map))[0]);
        }
    }

    public void testGetArrayMapOfStringClassOfQ() throws SerialException {
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i], ((Object[]) sa.getArray(badmap))[i]);
        }
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i], ((Object[]) sa.getArray(map))[i]);
        }
        mock.returnNull = true;
        // elements force deeper copy
        for (int i = 0; i < testElements.length; i++) {
            assertSame(testElements[i], ((Object[]) sa.getArray(map))[i]);
        }
    }

    public void testGetBaseType() throws SerialException {
        assertEquals(Types.JAVA_OBJECT, sa.getBaseType());
    }

    public void testGetBaseTypeName() throws SQLException {
        assertEquals("BaseName", sa.getBaseTypeName());
    }

    public void testGetResultSet() throws SQLException {
        try {
            sa.getResultSet();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetResultSetLongInt() throws SQLException {
        try {
            sa.getResultSet(0, 1);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetResultSetLongIntMapOfStringClassOfQ()
            throws SQLException {
        try {
            sa.getResultSet(0, 1, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetResultSetMapOfStringClassOfQ() throws SerialException {
        try {
            sa.getResultSet(null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    class MockArray implements Array {

        public boolean returnNull = false;

        public int basetype = Types.JAVA_OBJECT;

        public Object getArray() throws SQLException {
            return testElements;
        }

        public Object getArray(long index, int count) throws SQLException {
            Object[] ret = new Object[count];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = testElements[(int) index + i];
            }
            return ret;
        }

        public Object getArray(long index, int count, Map<String, Class<?>> map)
                throws SQLException {
            if (!returnNull) {
                Object[] ret = new Object[count];
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = testElements[(int) index + i];
                }
                return ret;
            } else {
                return null;
            }
        }

        public Object getArray(Map<String, Class<?>> map) throws SQLException {
            if (!returnNull) {
                return testElements;
            } else {
                return null;
            }
        }

        public int getBaseType() throws SQLException {
            return basetype;
        }

        public String getBaseTypeName() throws SQLException {
            return "BaseName";
        }

        public ResultSet getResultSet() throws SQLException {
            return null;
        }

        public ResultSet getResultSet(long index, int count)
                throws SQLException {
            return null;

        }

        public ResultSet getResultSet(long index, int count,
                Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        public ResultSet getResultSet(Map<String, Class<?>> map)
                throws SQLException {
            return null;
        }
    }

    class MockNullArray implements Array {

        public Object getArray() throws SQLException {
            return null;
        }

        public Object getArray(long index, int count) throws SQLException {
            return null;
        }

        public Object getArray(long index, int count, Map<String, Class<?>> map)
                throws SQLException {
            return null;
        }

        public Object getArray(Map<String, Class<?>> map) throws SQLException {
            return testElements;
        }

        public int getBaseType() throws SQLException {
            return Types.ARRAY;
        }

        public String getBaseTypeName() throws SQLException {
            return "BaseName";
        }

        public ResultSet getResultSet() throws SQLException {
            return null;
        }

        public ResultSet getResultSet(long index, int count)
                throws SQLException {
            return null;

        }

        public ResultSet getResultSet(long index, int count,
                Map<String, Class<?>> map) throws SQLException {
            return null;
        }

        public ResultSet getResultSet(Map<String, Class<?>> map)
                throws SQLException {
            return null;
        }
    }

    class MockStringSQLData implements SQLData {

        private String name = "java.lang.String";

        public String getSQLTypeName() throws SQLException {
            return name;
        }

        public void readSQL(SQLInput stream, String typeName)
                throws SQLException {
            return;
        }

        public void writeSQL(SQLOutput stream) throws SQLException {
            return;
        }
    }

    class MockObjectSQLData implements SQLData {

        private String name = "java.lang.Object";

        public String getSQLTypeName() throws SQLException {
            return name;
        }

        public void readSQL(SQLInput stream, String typeName)
                throws SQLException {
            return;
        }

        public void writeSQL(SQLOutput stream) throws SQLException {
            return;
        }
    }

    private static class SQLArray implements java.sql.Array {

        Object[] array;

        SQLArray() throws SQLException {

            char[] chars = { 'a', 'b', 'c', 'd' };
            array = new Object[1];

            array[0] = (Object) new SerialClob(chars);
        }

        public Object getArray() {
            return array;
        }

        public int getBaseType() {
            return java.sql.Types.CLOB;
        }

        /**
         * Everything below here is just supplied to satisfy the interface and
         * is not part of this testcase.
         */

        public Object getArray(long index, int count) {
            return null;
        }

        public Object getArray(long index, int count, Map<String, Class<?>> map) {
            return null;
        }

        public Object getArray(Map<String, Class<?>> map) {
            return null;
        }

        public String getBaseTypeName() {
            return null;
        }

        public ResultSet getResultSet() {
            return null;
        }

        public ResultSet getResultSet(long index, int count) {
            return null;
        }

        public ResultSet getResultSet(Map<String, Class<?>> map) {
            return null;
        }

        public ResultSet getResultSet(long index, int count,
                Map<String, Class<?>> map) {
            return null;
        }
    }
}
