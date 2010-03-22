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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;

import javax.sql.rowset.spi.SyncProviderException;

public class CachedRowSetUpdateTest extends CachedRowSetTestCase {

    public void testColumnUpdated() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.columnUpdated(1);
            fail("should throw exception");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw ArrayIndexOutOfBoundsException
        } catch (SQLException e) {
            // According to spec, it's supposed to throw SQLException
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        // the cursor is before the first row
        assertTrue(noInitialCrset.isBeforeFirst());
        try {
            noInitialCrset.columnUpdated("ID");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        // the cursor is after the last row
        noInitialCrset.afterLast();
        assertTrue(noInitialCrset.isAfterLast());
        try {
            noInitialCrset.columnUpdated(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        assertTrue(noInitialCrset.first());
        noInitialCrset.moveToInsertRow();
        try {
            noInitialCrset.columnUpdated("NAME");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.moveToCurrentRow();
        assertTrue(noInitialCrset.absolute(3));
        assertEquals(3, noInitialCrset.getInt(1));
        assertFalse(noInitialCrset.columnUpdated(2));
        noInitialCrset.updateString(2, "update3");
        assertTrue(noInitialCrset.columnUpdated(2));
        noInitialCrset.acceptChanges(conn);

        assertTrue(noInitialCrset.absolute(3));
        assertEquals(3, noInitialCrset.getInt(1));
        assertTrue(noInitialCrset.columnUpdated("NAME"));

        noInitialCrset.updateRow();
        noInitialCrset.acceptChanges(conn);
        assertTrue(noInitialCrset.absolute(3));
        assertEquals("update3", noInitialCrset.getString(2));
        assertFalse(noInitialCrset.columnUpdated(2));

        assertTrue(noInitialCrset.absolute(4));
        try {
            noInitialCrset.columnUpdated(0);
            fail("should throw exception");
        } catch (IndexOutOfBoundsException e) {
            // RI throw IndexOutOfBoundsException
        } catch (SQLException e) {
            // expected
        }

        try {
            noInitialCrset.columnUpdated("abc");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    public void testRowUpdated() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.rowUpdated();
            fail("should throw exception");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw exception here
        } catch (SQLException e) {
            // expected
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        assertTrue(noInitialCrset.isBeforeFirst());
        try {
            noInitialCrset.rowUpdated();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.afterLast();
        assertTrue(noInitialCrset.isAfterLast());
        try {
            noInitialCrset.rowUpdated();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        assertTrue(noInitialCrset.absolute(3));
        assertFalse(noInitialCrset.rowUpdated());
        noInitialCrset.updateString(2, "update3");
        assertFalse(noInitialCrset.rowUpdated());
        noInitialCrset.updateRow();
        assertTrue(noInitialCrset.rowUpdated());
        noInitialCrset.acceptChanges(conn);
        assertTrue(noInitialCrset.absolute(3));
        assertEquals("update3", noInitialCrset.getString(2));
        assertFalse(noInitialCrset.rowUpdated());

        noInitialCrset.moveToInsertRow();
        try {
            noInitialCrset.rowUpdated();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.moveToCurrentRow();
        assertTrue(noInitialCrset.absolute(4));
        assertFalse(noInitialCrset.rowUpdated());
        noInitialCrset.updateRow();
        assertFalse(noInitialCrset.rowUpdated());
        noInitialCrset.updateString(2, "abc");
        noInitialCrset.updateRow();
        assertTrue(noInitialCrset.rowUpdated());
    }

    public void testCancelRowUpdates() throws Exception {
        noInitialCrset = newNoInitialInstance();
        Listener listener = new Listener();
        noInitialCrset.addRowSetListener(listener);
        try {
            noInitialCrset.cancelRowUpdates();
            fail("should throw exception");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw exception here
        } catch (SQLException e) {
            // expected
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        assertTrue(noInitialCrset.isBeforeFirst());
        try {
            noInitialCrset.cancelRowUpdates();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.afterLast();
        assertTrue(noInitialCrset.isAfterLast());
        try {
            noInitialCrset.cancelRowUpdates();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.moveToInsertRow();
        try {
            noInitialCrset.cancelRowUpdates();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.moveToCurrentRow();
        assertTrue(noInitialCrset.absolute(2));
        // no effect here
        noInitialCrset.cancelRowUpdates();

        assertTrue(noInitialCrset.absolute(3));
        noInitialCrset.updateString(2, "update3");
        // call cancelRowUpdates() before updateRow(), no effect here
        listener.clear();
        noInitialCrset.cancelRowUpdates();
        assertNull(listener.getTag());
        assertEquals("update3", noInitialCrset.getString(2));
        noInitialCrset.updateRow();
        noInitialCrset.acceptChanges(conn);
        assertEquals("update3", noInitialCrset.getString(2));

        assertTrue(noInitialCrset.absolute(4));
        noInitialCrset.updateString(2, "update4");
        assertEquals("update4", noInitialCrset.getString(2));
        noInitialCrset.updateRow();
        assertEquals("update4", noInitialCrset.getString(2));
        // call cancelRowUpdates() after updateRow(), it works here

        listener.clear();
        noInitialCrset.cancelRowUpdates();
        assertEquals(CachedRowSetListenerTest.EVENT_ROW_CHANGED, listener
                .getTag());
        assertEquals("test4", noInitialCrset.getString(2));
        noInitialCrset.acceptChanges(conn);
        assertEquals("test4", noInitialCrset.getString(2));
    }

    public void testUndoUpdate() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.undoUpdate();
            fail("should throw exception");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw exception here
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        assertTrue(noInitialCrset.isBeforeFirst());
        try {
            noInitialCrset.undoUpdate();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.afterLast();
        assertTrue(noInitialCrset.isAfterLast());
        try {
            noInitialCrset.undoUpdate();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        assertTrue(noInitialCrset.absolute(3));
        try {
            noInitialCrset.undoUpdate();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // RI throw SQLException here.
        }

        noInitialCrset.updateString(2, "update3");
        noInitialCrset.updateLong(3, 3333333L);
        noInitialCrset.updateFloat(7, 3.3F);
        try {
            noInitialCrset.undoUpdate();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // RI throw SQLException here.
        }

        noInitialCrset.updateRow();
        try {
            noInitialCrset.undoUpdate();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // RI throw SQLException here.
        }

        noInitialCrset.acceptChanges(conn);
        assertEquals("update3", noInitialCrset.getString(2));
        assertEquals(3333333L, noInitialCrset.getLong(3));
        assertEquals(3.3F, noInitialCrset.getFloat(7));
        try {
            noInitialCrset.undoUpdate();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // RI throw SQLException here.
        }

        noInitialCrset.moveToInsertRow();
        noInitialCrset.updateInt(1, 10);
        noInitialCrset.updateString(2, "update10");
        noInitialCrset.insertRow();
        noInitialCrset.moveToCurrentRow();
        try {
            noInitialCrset.undoUpdate();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // RI throw SQLException here.
        }

        // move to the inserted row
        noInitialCrset.absolute(4);
        assertTrue(noInitialCrset.rowInserted());
        assertEquals(10, noInitialCrset.getInt(1));
        // undoUpdate() work. It undo the insert operation.
        noInitialCrset.undoUpdate();
        // The cursor moves to the next row.
        assertFalse(noInitialCrset.rowInserted());
        assertEquals(4, noInitialCrset.getInt(1));
        noInitialCrset.acceptChanges(conn);

        // check db
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 10");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));

        // check CachedRowSet
        noInitialCrset.beforeFirst();
        int index = 0;
        while (noInitialCrset.next()) {
            index++;
            assertEquals(index, noInitialCrset.getInt(1));
        }
        assertEquals(4, index);
    }

    public void testUpdateValue() throws Exception {

        try {
            crset.updateString(1, "");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid cursor position
        }

        try {
            crset.updateString(-1, "");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid column index
        }

        crset.moveToInsertRow();

        try {
            crset.updateDate(1, new Date(10000));
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateInt(1, 100);
        crset.updateString(2, "test8");

        try {
            crset.updateBytes(2, "hello".getBytes());
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateInt(6, 41);
        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));

        crset.insertRow();
        crset.moveToCurrentRow();
        crset.setTableName("USER_INFO");
        crset.acceptChanges(conn);
    }

    public void testUpdateLong() throws SQLException {
        crset.moveToInsertRow();

        crset.updateInt(1, 100);
        crset.updateString(2, "update");

        crset.updateLong(3, 444423L);
        crset.updateInt(3, 100);
        crset.updateShort(3, (short) 100);
        crset.updateByte(3, (byte) 10);
        crset.updateBigDecimal(3, new BigDecimal(Long.MAX_VALUE));
        crset.updateString(3, "1000");

        try {
            crset.updateBigDecimal(3, new BigDecimal(Double.MAX_VALUE));
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateFloat(3, 80.98F);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateDouble(3, 80.98);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateBoolean(3, false);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateInt(6, 41);
        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        crset.setTableName("USER_INFO");
        crset.acceptChanges(conn);
    }

    public void testUpdateInt() throws SQLException {
        crset.moveToInsertRow();

        crset.updateInt(1, 50);
        crset.updateByte(1, (byte) 10);
        crset.updateShort(1, (short) 60);
        crset.updateLong(1, 100L);
        crset.updateBigDecimal(1, new BigDecimal(50));
        crset.updateString(1, "100");

        try {
            crset.updateBigDecimal(1, new BigDecimal(Double.MAX_VALUE));
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateLong(1, 100000000000L);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateFloat(1, 80.98F);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateDouble(1, 80.98);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateBoolean(1, false);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateString(2, "test100");
        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateInt(6, 41);
        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        crset.setTableName("USER_INFO");
        crset.acceptChanges(conn);

        ResultSet rs = st
                .executeQuery("SELECT * FROM USER_INFO WHERE ID = 100");
        assertTrue(rs.next());
    }

    public void testUpdateShort() throws SQLException {
        crset.moveToInsertRow();

        crset.updateInt(1, 50);
        crset.updateString(2, "test100");
        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));

        crset.updateShort(6, (short) 10);
        crset.updateByte(6, (byte) 10);
        crset.updateInt(6, 41);
        crset.updateLong(6, 33);
        crset.updateBigDecimal(6, new BigDecimal(23));

        try {
            crset.updateBigDecimal(6, new BigDecimal(Double.MAX_VALUE));
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateLong(6, 100000000000L);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateInt(6, Integer.MAX_VALUE);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateFloat(6, 80.98F);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateDouble(6, 80.98);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateBoolean(6, false);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        crset.setTableName("USER_INFO");
        crset.acceptChanges(conn);
    }

    public void testUpdateFloat() throws SQLException {
        crset.moveToInsertRow();

        crset.updateInt(1, 50);
        crset.updateString(2, "test100");
        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateInt(6, 41);

        crset.updateFloat(7, 4.8F);
        crset.updateDouble(7, 80.98);

        crset.updateInt(7, 10000);
        crset.updateLong(7, 10000);
        crset.updateShort(7, (short) 10);
        crset.updateByte(7, (byte) 10);
        crset.updateBigDecimal(7, new BigDecimal(100));
        crset.updateDouble(7, Float.MAX_VALUE);

        // throw exception when acceptChange
        // crset.updateDouble(7, Double.MAX_VALUE);
        // crset.updateBigDecimal(7, new BigDecimal(Double.MAX_VALUE));

        try {
            crset.updateBoolean(1, false);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        crset.setTableName("USER_INFO");
        crset.acceptChanges(conn);
    }

    public void testUpdateFloat_Exception() throws SQLException {
        crset.moveToInsertRow();
        crset.updateInt(1, 50);
        crset.updateString(2, "test100");
        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateInt(6, 41);

        // over range
        crset.updateDouble(7, Double.MAX_VALUE);

        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();

        try {
            crset.acceptChanges(conn);
            fail("Should throw SyncProviderException");
        } catch (SyncProviderException e) {
            // expected
        }
    }

    public void testUpdateBigDecimal() throws SQLException {
        crset.moveToInsertRow();
        crset.updateInt(1, 50);
        crset.updateString(2, "test100");
        crset.updateLong(3, 444423L);

        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateByte(4, (byte) 10);
        crset.updateShort(4, (short) 10);
        crset.updateInt(4, 300);
        crset.updateLong(4, 2000);
        crset.updateFloat(4, 3.59F);
        crset.updateDouble(4, 3.4994);

        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateInt(6, 41);
        crset.updateDouble(7, Float.MAX_VALUE);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));

        crset.insertRow();
        crset.moveToCurrentRow();
        crset.setTableName("USER_INFO");
        crset.acceptChanges(conn);
    }

    public void testUpdateObject() throws SQLException {
        crset.moveToInsertRow();

        // updateObject doesn't check type compatibility
        crset.updateObject(1, new Date(1000));
        crset.updateObject(2, new ArrayList<Object>());

        try {
            crset.updateDate(1, new Date(1000));
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateObject(1, null);

        try {
            crset.updateBigDecimal(7, null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        crset.updateString(2, null);

        // TODO test Array, Blob, Clob
        // crset.updateArray(3, null);
    }

    public void testUpdateObject_I_LObject_I() throws Exception {
        crset.moveToInsertRow();

        crset.updateInt(1, 50);
        crset.updateString(2, "test100");
        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));

        crset.updateObject(5, new BigDecimal("3.1200"), 3);

        BigDecimal bigDecimal = new BigDecimal("3.1200");
        BigDecimal scaled = bigDecimal.setScale(3);

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(scaled, crset.getBigDecimal(5));
            assertEquals(scaled, crset.getObject(5));
        } else {
            /*
             * TODO seems ri doesn't do scale
             */
            assertEquals(bigDecimal, crset.getBigDecimal(5));
            assertEquals(bigDecimal, crset.getObject(5));
        }

        try {
            crset.updateObject(5, new BigDecimal("3.1200"), 1);
            fail("Should throw ArithmeticException");
        } catch (ArithmeticException e) {
            // expected
        }

        try {
            crset.updateObject(5, null, 1);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            crset.updateObject(5, Double.valueOf(3.0000), 1);
            fail("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // ri throw ClassCastException, we follow ri
        }

        try {
            crset.updateObject(5, Float.valueOf(3.0000F), 3);
            fail("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // ri throw ClassCastException, we follow ri
        }

        try {
            crset.updateObject(5, Integer.valueOf(3), 3);
            fail("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // ri throw ClassCastException, we follow ri
        }

        try {
            crset.updateObject(5, new ArrayList<Object>(), 3);
            fail("Should throw ClassCastException");
        } catch (ClassCastException e) {
            // ri throw ClassCastException, we follow ri
        }

        crset.updateInt(6, 41);
        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        crset.setTableName("USER_INFO");
        crset.acceptChanges(conn);
    }

    public void testUpdateString() throws Exception {
        crset.moveToInsertRow();

        crset.updateInt(1, 50);
        crset.updateString(2, "test100");
        crset.updateInt(2, 2);
        assertEquals("2", crset.getObject(2));

        try {
            crset.updateBytes(2, new byte[] { 1, 2 });
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateDate(2, new Date(965324512));
        assertEquals(new Date(965324512).toString(), crset.getObject(2));

        crset.updateTime(2, new Time(452368512));
        assertEquals(new Time(452368512).toString(), crset.getObject(2));

        crset.updateTimestamp(2, new Timestamp(874532105));
        assertEquals(new Timestamp(874532105).toString(), crset.getObject(2));

        crset.updateBigDecimal(2, new BigDecimal(12));
        assertEquals(new BigDecimal(12).toString(), crset.getObject(2));

        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateLong(6, 33);
        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));
    }

    public void testUpdateDate() throws Exception {
        crset.moveToInsertRow();

        crset.updateInt(1, 50);
        crset.updateString(2, "test100");
        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateLong(6, 33);
        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);

        crset.updateDate(10, new Date(965324512));
        try {
            crset.updateInt(10, 12345);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateLong(10, 123456789L);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateDouble(10, 123456789.2398);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateTime(10, new Time(452368512));
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateString(10, "test");
        assertTrue(crset.getObject(10) instanceof String);
        assertEquals("test", crset.getObject(10));

        crset.updateTimestamp(10, new Timestamp(874532105));
        assertTrue(crset.getObject(10) instanceof Date);
        assertEquals(new Timestamp(874532105).getTime(), crset.getDate(10)
                .getTime());

        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));
    }

    public void testUpdateTime() throws Exception {
        crset.moveToInsertRow();

        crset.updateInt(1, 50);
        crset.updateString(2, "test100");
        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateLong(6, 33);
        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));

        crset.updateTime(11, new Time(452368512));

        try {
            crset.updateInt(11, 12345);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateLong(11, 123456789L);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateDouble(11, 123456789.2398);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateDate(11, new Date(452368512));
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateString(11, "test");
        assertTrue(crset.getObject(11) instanceof String);
        assertEquals("test", crset.getObject(11));

        crset.updateTimestamp(11, new Timestamp(874532105));
        assertTrue(crset.getObject(11) instanceof Time);
        assertEquals(new Timestamp(874532105).getTime(), crset.getTime(11)
                .getTime());

        crset.updateTimestamp(12, new Timestamp(874532105));
    }

    public void testUpdateTimestamp() throws Exception {
        crset.moveToInsertRow();

        crset.updateInt(1, 50);
        crset.updateString(2, "test100");
        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateLong(6, 33);
        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));

        crset.updateTimestamp(12, new Timestamp(874532105));

        try {
            crset.updateInt(12, 12345);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateLong(12, 123456789L);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        try {
            crset.updateDouble(12, 123456789.2398);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Data Type Mismatch
        }

        crset.updateString(12, "test");
        assertTrue(crset.getObject(12) instanceof String);
        assertEquals("test", crset.getObject(12));

        crset.updateDate(12, new Date(452368512));
        assertTrue(crset.getObject(12) instanceof Timestamp);
        assertEquals(new Date(452368512).getTime(), crset.getTimestamp(12)
                .getTime());

        crset.updateTime(12, new Time(874532105));
        assertTrue(crset.getObject(12) instanceof Timestamp);
        assertEquals(new Timestamp(874532105).getTime(), crset.getTimestamp(12)
                .getTime());
    }
}
