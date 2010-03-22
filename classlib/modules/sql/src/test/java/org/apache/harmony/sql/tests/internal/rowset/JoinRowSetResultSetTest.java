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

package org.apache.harmony.sql.tests.internal.rowset;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;

import junit.framework.TestCase;

public class JoinRowSetResultSetTest extends JoinRowSetTestCase {

    public void testAbsolute_Empty() throws SQLException {
        assertFalse(jrs.absolute(1));

        // TODO non-bug different
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertFalse(jrs.absolute(0));
        } else {
            try {
                assertTrue(jrs.absolute(0));
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // invalid cursor position
            }
        }
    }

    public void testAbsolute() throws Exception {
        jrs.addRowSet(crset, 1);

        // TODO non-bug different
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertFalse(jrs.absolute(0));
        } else {
            try {
                assertTrue(jrs.absolute(0));
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // invalid cursor position
            }
        }

        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, jrs.getType());
        assertTrue(jrs.absolute(1));
        assertEquals(1, jrs.getInt(1));
        assertTrue(jrs.absolute(4));
        assertEquals(4, jrs.getInt(1));
        assertTrue(jrs.absolute(3));
        assertEquals(3, jrs.getInt(1));

        // when position the cursor beyond the first/last row in the result set
        assertFalse(jrs.absolute(10));
        assertTrue(jrs.isAfterLast());
        assertTrue(jrs.previous());
        assertFalse(jrs.absolute(-10));
        assertTrue(jrs.isBeforeFirst());
        assertTrue(jrs.next());

        /*
         * TODO when the given row number is negative, spec says absolute(-1)
         * equals last(). However, the return value of absolute(negative) is
         * false when run on RI. The Harmony follows the spec.
         */
        if (System.getProperty("Testing Harmony") == "true") {
            assertTrue(jrs.absolute(-1));
            assertEquals(4, jrs.getInt(1));
            assertTrue(jrs.absolute(-3));
            assertEquals(2, jrs.getInt(1));
            assertFalse(jrs.absolute(-5));
        } else {
            assertFalse(jrs.absolute(-1));
            assertEquals(0, jrs.getRow());
            assertTrue(jrs.isBeforeFirst());
        }

        jrs.moveToInsertRow();
        try {
            jrs.first();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.updateInt(1, 60);
        jrs.updateString(2, "abc");
        jrs.insertRow();
        try {
            jrs.absolute(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.moveToCurrentRow();
        assertTrue(jrs.absolute(2));

        jrs.setType(ResultSet.TYPE_FORWARD_ONLY);
        assertEquals(jrs.getType(), ResultSet.TYPE_FORWARD_ONLY);

        crset.setType(ResultSet.TYPE_FORWARD_ONLY);
        assertEquals(crset.getType(), ResultSet.TYPE_FORWARD_ONLY);

        // In CachedRowSet, it will throw SQLException, however, in JoinRowSet,
        // it doesn't throw.
        // TODO
        assertTrue(jrs.absolute(1));
        assertEquals(60, jrs.getInt(1));

        if (System.getProperty("Testing Harmony") == "true") {
            assertTrue(jrs.absolute(-1));
            assertTrue(jrs.absolute(-3));
            assertFalse(jrs.absolute(-5));
        } else {
            assertFalse(jrs.absolute(-1));
            assertEquals(0, jrs.getRow());
            assertTrue(jrs.isBeforeFirst());
        }
    }

    public void testAbsolute_SetForwardOnly() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.setType(ResultSet.TYPE_FORWARD_ONLY);
        assertEquals(ResultSet.TYPE_FORWARD_ONLY, jrs.getType());

        jrs.first();
        jrs.last();
        jrs.beforeFirst();
        jrs.afterLast();
        jrs.previous();
        jrs.relative(1);

        jrs.absolute(2);
        assertEquals(2, jrs.getInt(1));
    }

    public void testCachedRowSet_SetForwardOnly() throws Exception {
        crset.setType(ResultSet.TYPE_FORWARD_ONLY);
        assertEquals(ResultSet.TYPE_FORWARD_ONLY, crset.getType());

        try {
            crset.first();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }

        try {
            crset.absolute(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }
    }

    public void testAbsolute_AfterDeleteRow() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.first();
        jrs.deleteRow();
        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);
        jrs.beforeFirst();
        assertFalse(jrs.absolute(4));
        assertTrue(jrs.isAfterLast());

        jrs.setShowDeleted(true);
        assertTrue(jrs.getShowDeleted());

        // In RI, set show deleted will not take effect.
        // In harmony, it takes.
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(jrs.absolute(4));
        } else {
            assertFalse(jrs.absolute(4));
        }
        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(4, rowNum);
        } else {
            assertEquals(3, rowNum);
        }
    }

    public void testFirstAndLast_Empty() throws Exception {
        assertFalse(jrs.isBeforeFirst());
        // Different in RI and Harmony.
        if (System.getProperty("Testing Harmony") == "true") {
            assertFalse(jrs.isFirst());
            assertFalse(jrs.isLast());
        } else {
            assertTrue(jrs.isFirst());
            assertTrue(jrs.isLast());
        }
        assertFalse(jrs.isAfterLast());

        jrs.beforeFirst();
        assertFalse(jrs.first());
        assertFalse(jrs.last());
        jrs.afterLast();

    }

    public void testFirstAndLast() throws Exception {
        jrs.addRowSet(crset, 1);
        /*
         * This method is used to test afterLast(), beforeFist(), first(),
         * last()
         */
        assertFalse(jrs.isFirst());
        assertTrue(jrs.isBeforeFirst());
        assertTrue(jrs.first());
        assertTrue(jrs.isFirst());
        assertFalse(jrs.isBeforeFirst());
        jrs.beforeFirst();
        assertTrue(jrs.isBeforeFirst());
        assertTrue(jrs.last());
        assertTrue(jrs.isLast());

        assertTrue(jrs.first());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, jrs.getType());
    }

    public void testAfterLast_Empty() throws Exception {
        jrs.afterLast();
        assertFalse(jrs.isAfterLast());
    }

    public void testAfterLast() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.afterLast();
        jrs.previous();
        assertEquals(4, jrs.getInt(1));
    }

    public void testRelative_Empty() throws Exception {
        try {
            jrs.relative(1);
            fail("Should throw SQLException.");
        } catch (SQLException e) {
            // Expected.
        }
    }

    public void testRelative() throws Exception {
        jrs.addRowSet(crset, 1);
        /*
         * ri throw SQLException, but spec say relative(1) is identical to next
         */
        try {
            jrs.relative(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        assertTrue(jrs.next());
        assertEquals("hermit", jrs.getString(2));

        assertTrue(jrs.relative(2));
        assertEquals("test3", jrs.getString(2));

        assertTrue(jrs.relative(-1));
        assertEquals("test", jrs.getString(2));

        assertTrue(jrs.relative(0));
        assertEquals("test", jrs.getString(2));

        assertFalse(jrs.relative(-5));
        assertEquals(0, jrs.getRow());

        assertTrue(jrs.next());
        assertEquals("hermit", jrs.getString(2));
        assertTrue(jrs.relative(3));
        assertEquals("test4", jrs.getString(2));

        assertFalse(jrs.relative(3));
        assertEquals(0, jrs.getRow());

        assertTrue(jrs.isAfterLast());
        assertTrue(jrs.previous());

        // TODO RI's bug
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(DEFAULT_ROW_COUNT, jrs.getRow());
            assertEquals("test4", jrs.getString(2));
        } else {
            assertEquals(-1, jrs.getRow());
            assertEquals("test4", jrs.getString(2));

            assertTrue(jrs.previous());
            assertEquals(-2, jrs.getRow());
            assertEquals("test3", jrs.getString(2));
        }
    }

    public void testSetShowDeleted() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.first();
        jrs.deleteRow();
        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);
        jrs.first();
        assertEquals(2, jrs.getInt(1));

        rowNum = 0;
        jrs.afterLast();
        while (jrs.previous()) {
            rowNum++;
        }
        assertEquals(3, rowNum);

        jrs.setShowDeleted(true);
        assertTrue(jrs.getShowDeleted());

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        jrs.first();

        // In RI, set show deleted will not take effect.
        // In harmony, it takes.
        if (System.getProperty("Testing Harmony") == "true") {
            assertEquals(1, jrs.getInt(1));
            assertEquals(4, rowNum);
        } else {
            assertEquals(2, jrs.getInt(1));
            assertEquals(3, rowNum);
        }

        rowNum = 0;
        jrs.afterLast();
        while (jrs.previous()) {
            rowNum++;
        }
        if (System.getProperty("Testing Harmony") == "true") {
            assertEquals(4, rowNum);
        } else {
            assertEquals(3, rowNum);
        }
    }

    public void testDeleteRow() throws Exception {
        try {
            jrs.deleteRow();
            fail("Should throw SQLException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw ArrayIndexOutOfBoundsException.
        } catch (SQLException e) {
            // Expected.
        }

        jrs.addRowSet(crset, 1);
        jrs.first();
        jrs.deleteRow();

        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);

        rowNum = 0;
        crset.beforeFirst();
        while (crset.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);

        crset.absolute(2);
        crset.deleteRow();

        rowNum = 0;
        crset.beforeFirst();
        while (crset.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);

        jrs.first();
        assertEquals(2, jrs.getInt(1));
    }

    public void testDeleteRow_MultipleRowSets() throws Exception {
        // Creates a new CachedRowSet from BOOKs
        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from BOOKS");
        crset2.populate(rs);

        jrs.addRowSet(crset, 1);
        jrs.addRowSet(crset2, 1);

        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }

        assertEquals(6, rowNum);

        jrs.first();
        jrs.deleteRow();

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }

        assertEquals(5, rowNum);
    }

    public void testUpdateRow_Empty() throws Exception {
        // TODO
        // It's the problem of CachedRowSetImpl.
        try {
            jrs.updateRow();
            fail("Should throw SQLException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw ArrayIndexOutOfBoundsException.
        } catch (SQLException e) {
            // Expected.
        }
    }

    public void testUpdateRow() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.first();
        jrs.updateString(2, "Updated");
        jrs.updateRow();
        assertEquals("Updated", jrs.getString(2));

        crset.first();
        assertEquals("hermit", crset.getString(2));

        crset.next();
        crset.updateString(2, "Updated2");
        crset.updateRow();
        assertEquals("Updated2", crset.getString(2));

        jrs.first();
        jrs.next();
        assertEquals("test", jrs.getString(2));
    }

    public void testInsertRow_Empty() throws Exception {
        try {
            jrs.moveToInsertRow();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }
    }

    public void testInsertRow() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.moveToInsertRow();
        jrs.updateInt(1, 10);
        jrs.updateString(2, "Inserted");
        jrs.insertRow();

        jrs.moveToCurrentRow();
        assertTrue(jrs.next());
        assertEquals(10, jrs.getInt(1));
        assertEquals("Inserted", jrs.getString(2));

        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(5, rowNum);

        rowNum = 0;
        crset.beforeFirst();
        while (crset.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);

        crset.moveToInsertRow();
        crset.updateInt(1, 20);
        crset.updateString(2, "Inserted2");
        crset.insertRow();
        crset.moveToCurrentRow();

        rowNum = 0;
        crset.beforeFirst();
        while (crset.next()) {
            rowNum++;
        }
        assertEquals(5, rowNum);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(5, rowNum);
    }

    public void testInsertRow_MultipleRowSets() throws Exception {
        // Creates a new CachedRowSet from BOOKs
        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from BOOKS");
        crset2.populate(rs);

        jrs.addRowSet(crset2, 1);

        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(7, rowNum);

        jrs.moveToInsertRow();
        jrs.updateInt(1, 20);
        jrs.updateString(2, "Updated");
        jrs.insertRow();
        jrs.moveToCurrentRow();
        assertEquals(20, jrs.getInt(1));
        assertEquals("Updated", jrs.getString(2));
    }

    public void testGetInt() throws Exception {

        try {
            crset.getInt(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }

        try {
            jrs.getInt(1);
            fail("Should throw SQLException.");
        } catch (NullPointerException e) {
            // RI throw NullPointerException.
        } catch (SQLException e) {
            // Expected.
        }

        jrs.addRowSet(crset, 1);
        jrs.first();
        assertEquals(1, jrs.getInt(1));

        crset.first();
        crset.updateInt(1, 60);
        crset.updateRow();

        crset.first();
        assertEquals(60, crset.getInt(1));

        jrs.first();
        assertEquals(1, jrs.getInt(1));

        jrs.absolute(2);
        jrs.updateString(2, "Updated");
        jrs.updateRow();

        jrs.absolute(2);
        assertEquals("Updated", jrs.getString(2));

        crset.absolute(2);
        assertEquals("test", crset.getString(2));
    }

    public void testRefreshRow_Empty() throws Exception {
        try {
            jrs.refreshRow();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        } catch (IndexOutOfBoundsException e) {
            // RI throw it.
        }
    }

    public void testRefreshRow() throws Exception {
        jrs.addRowSet(crset, 1);

        /*
         * Update the third row in database. Then call refreshRow().
         */
        int result = st
                .executeUpdate("UPDATE USER_INFO SET NAME = 'update33' WHERE ID = 3");
        assertEquals(1, result);

        // still no effect.
        jrs.absolute(3);
        jrs.refreshRow();
        assertEquals(3, jrs.getInt(1));
        assertEquals("test3", jrs.getString(2));

        jrs.updateString(2, "update33");
        jrs.refreshRow();
        assertEquals("test3", jrs.getString(2));

    }

    public void testCancelRowUpdates_Empty() throws Exception {
        try {
            jrs.cancelRowUpdates();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        } catch (IndexOutOfBoundsException e) {
            // RI throws it.
        }
    }
}
