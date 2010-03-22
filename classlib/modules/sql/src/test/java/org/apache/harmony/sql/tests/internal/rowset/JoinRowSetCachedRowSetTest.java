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

import java.sql.SQLException;

import javax.sql.rowset.spi.SyncProviderException;


public class JoinRowSetCachedRowSetTest extends JoinRowSetTestCase {

    public void testGetCommandAndUrl() throws Exception {
        assertNull(jrs.getCommand());
        assertNull(jrs.getUrl());
        assertNull(jrs.getUsername());
        assertNull(jrs.getPassword());

        String sqlCommand = "select * from USER_INFO";
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand(sqlCommand);
        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.execute();
        assertEquals(sqlCommand, noInitialCrset.getCommand());
        assertEquals(DERBY_URL, noInitialCrset.getUrl());
        assertTrue(noInitialCrset.first());
        assertEquals(1, noInitialCrset.getInt(1));

        // add CachedRowSet to JoinRowSet
        jrs.addRowSet(noInitialCrset, 1);
        assertTrue(jrs.first());
        assertEquals(1, jrs.getInt(1));

        // check command and url
        assertNull(jrs.getCommand());
        assertNull(jrs.getUrl());
        assertNull(jrs.getUsername());
        assertNull(jrs.getPassword());
    }

    public void testSetCommandAndUrl() throws Exception {
        // Test empty JoinRowSet
        String sqlCommand = "select * from CUSTOMER_INFO";
        jrs.setCommand(sqlCommand);
        jrs.setUrl(DERBY_URL);
        try {
            jrs.execute();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        assertEquals(sqlCommand, jrs.getCommand());
        assertEquals(DERBY_URL, jrs.getUrl());

        // Test JoinRowSet filled with CachedRowSet
        jrs = newJoinRowSet();
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand(sqlCommand);
        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.execute();
        jrs.addRowSet(noInitialCrset, 1);
        assertTrue(jrs.first());
        assertEquals(1111, jrs.getInt(1));
        assertNull(jrs.getCommand());
        assertNull(jrs.getUrl());
        // call JoinRowSet.execute()
        jrs.beforeFirst();
        jrs.execute();
        // check data
        int index = 0;
        noInitialCrset.beforeFirst();
        while (jrs.next() && noInitialCrset.next()) {
            index++;
            for (int i = 1; i <= jrs.getMetaData().getColumnCount(); i++) {
                assertEquals(jrs.getObject(i), noInitialCrset.getObject(i));
            }
        }

        // set command and url
        sqlCommand = "select * from USER_INFO";
        jrs.setCommand(sqlCommand);
        jrs.setUrl(DERBY_URL);
        assertEquals(sqlCommand, jrs.getCommand());
        assertEquals(DERBY_URL, jrs.getUrl());
        // call JoinRowSet.execute() after set cmd and url
        jrs.execute();
        /*
         * Check data. Though the sql command is changed, the data of JoinRowSet
         * remains the same.
         */
        jrs.beforeFirst();
        noInitialCrset.beforeFirst();
        while (jrs.next() && noInitialCrset.next()) {
            for (int i = 1; i <= jrs.getMetaData().getColumnCount(); i++) {
                assertEquals(jrs.getObject(i), noInitialCrset.getObject(i));
            }
        }
        assertEquals(sqlCommand, jrs.getCommand());
        assertEquals(DERBY_URL, jrs.getUrl());
    }

    public void testPopulate() throws Exception {
        /*
         * JoinRowSet.populate(ResultSet) won't throw exception. However,
         * JoinRowSet is still empty.
         */
        rs = st.executeQuery("select * from USER_INFO");
        jrs.populate(rs);
        assertNull(jrs.getMetaData());
        jrs.beforeFirst();
        assertFalse(jrs.next());
        assertFalse(jrs.first());

        /*
         * Test JoinRowSet which has added a CachedRowSet
         */
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        assertTrue(jrs.first());
        assertEquals(1, jrs.getInt(1));

        rs = st.executeQuery("select * from BOOKS");
        // populate() still has no effect here
        jrs.populate(rs);
        jrs.beforeFirst();
        rs = st.executeQuery("select * from USER_INFO");
        while (jrs.next() && rs.next()) {
            for (int i = 1; i <= jrs.getMetaData().getColumnCount(); i++) {
                assertEquals(rs.getObject(i), jrs.getObject(i));
            }
        }
    }

    public void testExecuteWithoutConn_Normal() throws Exception {
        /*
         * Add a CachedRowSet which has set command and url but not call execute
         * to JoinRowSet
         */
        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand("select * from BOOKS");
        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.populate(rs);
        assertTrue(noInitialCrset.first());
        assertEquals("hermit", noInitialCrset.getString(2));
        assertEquals("select * from BOOKS", noInitialCrset.getCommand());
        assertEquals(DERBY_URL, noInitialCrset.getUrl());
        // add to JoinRowSet
        jrs = newJoinRowSet();
        jrs.addRowSet(noInitialCrset, 1);
        assertNull(jrs.getStatement());
        // check data
        assertSame(noInitialCrset.getMetaData(), jrs.getMetaData());
        rs = st.executeQuery("select * from USER_INFO");
        jrs.beforeFirst();
        int index = 0;
        while (jrs.next() && rs.next()) {
            index++;
            for (int i = 1; i <= jrs.getMetaData().getColumnCount(); i++) {
                assertEquals(jrs.getObject(i), rs.getObject(i));
            }
        }
        assertEquals(4, index);
        // call JoinRowSet.execute()
        assertNull(jrs.getCommand());
        jrs.execute();
        assertNull(jrs.getStatement());
        assertNull(jrs.getCommand());
        // check JoinRowSet's data
        assertNotSame(noInitialCrset.getMetaData(), jrs.getMetaData());
        rs = st.executeQuery("select * from BOOKS");
        jrs.beforeFirst();
        index = 0;
        while (jrs.next() && rs.next()) {
            index++;
            for (int i = 1; i <= jrs.getMetaData().getColumnCount(); i++) {
                assertEquals(jrs.getObject(i), rs.getObject(i));
            }
        }
        assertEquals(7, index);
        // check noInitialCrset's data
        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next() && rs.next()) {
            index++;
            for (int i = 1; i <= noInitialCrset.getMetaData().getColumnCount(); i++) {
                assertEquals(noInitialCrset.getObject(i), rs.getObject(i));
            }
        }
        assertEquals(4, index);
        // call noInitialCrset's execute()
        assertEquals("select * from BOOKS", noInitialCrset.getCommand());
        noInitialCrset.execute();
        // check noInitialCrset's data
        jrs.beforeFirst();
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next() && jrs.next()) {
            index++;
            for (int i = 1; i <= noInitialCrset.getMetaData().getColumnCount(); i++) {
                assertEquals(noInitialCrset.getObject(i), jrs.getObject(i));
            }
        }
        assertEquals(7, index);

        // change noInitialCrset's command
        noInitialCrset.setCommand("select * from CUSTOMER_INFO");
        // call JoinRowSet.execute()
        jrs.execute();
        // check data
        jrs.beforeFirst();
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next() && jrs.next()) {
            index++;
            for (int i = 1; i <= noInitialCrset.getMetaData().getColumnCount(); i++) {
                assertEquals(noInitialCrset.getObject(i), jrs.getObject(i));
            }
        }
        assertEquals(7, index);

        /*
         * Add a CachedRowSet which has set command and url to JoinRowSet
         */
        crset = newNoInitialInstance();
        crset.setCommand("select * from USER_INFO");
        crset.setUrl(DERBY_URL);
        crset.execute();
        assertTrue(crset.absolute(1));
        assertEquals("hermit", crset.getString(2));
        // add to JoinRowSet
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        // check data
        assertSame(crset.getMetaData(), jrs.getMetaData());
        assertTrue(jrs.first());
        assertEquals("hermit", jrs.getString(2));
        // call JoinRowSet.execute()
        jrs.execute();
        // check data
        assertNotSame(crset.getMetaData(), jrs.getMetaData());
        isMetaDataEquals(crset.getMetaData(), jrs.getMetaData());
        assertTrue(jrs.absolute(1));
        assertEquals("hermit", jrs.getString(2));

        // set command and url for JoinRowSet, then call execute() again
        jrs.setCommand("select * from BOOKS");
        jrs.setUrl(DERBY_URL);
        jrs.execute();
        // the data remains the same as crset's
        assertNotSame(crset.getMetaData(), jrs.getMetaData());
        isMetaDataEquals(crset.getMetaData(), jrs.getMetaData());
        assertTrue(jrs.absolute(1));
        assertEquals("hermit", jrs.getString(2));

        // change command for CachedRowSet, then call JoinRowSet.execute()
        crset.setCommand("select * from BOOKS");
        jrs.execute();
        assertNotSame(crset.getMetaData(), jrs.getMetaData());
        isMetaDataEquals(crset.getMetaData(), jrs.getMetaData());
        assertTrue(jrs.absolute(1));
        assertEquals("hermit", jrs.getString(2));
        // call CachedRowSet.execute()
        crset.execute();
        assertTrue(crset.first());
        assertEquals("sn1-1", crset.getString(2));
        assertTrue(jrs.absolute(1));
        assertEquals("hermit", jrs.getString(2));

        /*
         * Add another CachedRowSet which also can call execute()
         * successfully to JoinRowSet
         */
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand("select * from BOOKS");
        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.execute();
        /*
         * TODO The match column will lost after call execute(). Therefore, it
         * would throw exception and prompts that no match column. It's need to
         * test Joinable.
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            // TODO
        } else {
            try {
                jrs.addRowSet(noInitialCrset, 1);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        }
    }

    public void testExecuteWithoutConn_Exception() throws Exception {
        /*
         * An empty JoinRowSet call execute() would throw SQLException even when
         * command and url are set.
         */
        try {
            jrs.execute();
            fail("should throw SQLException.");
        } catch (SQLException e) {
            // expected
        }

        String sqlCommand = "select * from USER_INFO";
        jrs.setCommand(sqlCommand);
        jrs.setUrl(DERBY_URL);
        try {
            jrs.execute();
            fail("should throw SQLException.");
        } catch (SQLException e) {
            // expected
        }

        /*
         * Add a CachedRowSet without set command and url to JoinRowSet
         */
        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.populate(rs);
        assertTrue(noInitialCrset.first());
        assertEquals(1, noInitialCrset.getInt(1));
        // add to JoinRowSet
        jrs = newJoinRowSet();
        jrs.addRowSet(noInitialCrset, 1);
        assertTrue(jrs.last());
        assertEquals(4, jrs.getInt(1));
        // call execute()
        try {
            jrs.execute();
            fail("should throw SQLException.");
        } catch (SQLException e) {
            // expected
        }
        // if execute() fail, then the JoinRowSet's data would lose
        assertNotNull(jrs.getMetaData());
        assertSame(noInitialCrset.getMetaData(), jrs.getMetaData());
        assertFalse(jrs.first());
        assertFalse(jrs.absolute(3));
        assertTrue(noInitialCrset.first());
        assertEquals("hermit", noInitialCrset.getString(2));

        /*
         * Add two CachedRowSets which both can call execute()
         */
        crset = newNoInitialInstance();
        crset.setCommand("select * from USER_INFO");
        crset.setUrl(DERBY_URL);
        crset.execute();
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand("select * from BOOKS");
        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.execute();
        // add to JoinRowSet
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        jrs.addRowSet(noInitialCrset, 1);
        // it would throw SQLException when more than one RowSet is added
        try {
            jrs.execute();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        assertNotNull(jrs.getMetaData());
        assertFalse(jrs.first());
    }

    public void testExecuteWithConn_Normal() throws Exception {
        assertNull(jrs.getStatement());
        /*
         * Add a CachedRowSet which has set command and call execute() to
         * JoinRowSet
         */
        crset = newNoInitialInstance();
        crset.setCommand("select * from USER_INFO");
        crset.execute(conn);
        assertTrue(crset.absolute(1));
        assertEquals("hermit", crset.getString(2));
        assertEquals(1, crset.getRow());
        // add to JoinRowSet
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        assertNull(jrs.getStatement());
        // check data
        assertSame(crset.getMetaData(), jrs.getMetaData());
        assertTrue(jrs.absolute(1));
        assertEquals("hermit", jrs.getString(2));
        assertEquals(1, jrs.getRow());
        assertTrue(jrs.last());
        assertEquals(4, jrs.getRow());
        // call JoinRowSet.execute()
        jrs.execute(conn);
        assertNull(jrs.getStatement());
        // check data
        assertNotSame(crset.getMetaData(), jrs.getMetaData());
        isMetaDataEquals(crset.getMetaData(), jrs.getMetaData());
        rs = st.executeQuery("select * from USER_INFO");
        jrs.beforeFirst();
        int index = 0;
        while (jrs.next() && rs.next()) {
            index++;
            for (int i = 1; i <= jrs.getMetaData().getColumnCount(); i++) {
                assertEquals(jrs.getObject(i), rs.getObject(i));
            }
            assertEquals(index, jrs.getRow());
        }
        assertEquals(4, index);

        // set command and url for JoinRowSet, then call execute() again
        jrs.setCommand("select * from BOOKS");
        jrs.execute(conn);
        assertNull(jrs.getStatement());
        // the data remains the same as crset's
        assertNotSame(crset.getMetaData(), jrs.getMetaData());
        isMetaDataEquals(crset.getMetaData(), jrs.getMetaData());
        rs = st.executeQuery("select * from USER_INFO");
        jrs.beforeFirst();
        index = 0;
        while (jrs.next() && rs.next()) {
            index++;
            for (int i = 1; i <= jrs.getMetaData().getColumnCount(); i++) {
                assertEquals(jrs.getObject(i), rs.getObject(i));
            }
        }
        assertEquals(4, index);
    }

    public void testExecuteWithConn_Exception() throws Exception {
        /*
         * Test empty JoinRowSet
         */
        try {
            jrs.execute(conn);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        jrs.setCommand("select * from USER_INFO");
        try {
            jrs.execute(conn);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        /*
         * Add a CachedRowSet without set command and url to JoinRowSet
         */
        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.populate(rs);
        assertTrue(noInitialCrset.first());
        assertEquals("hermit", noInitialCrset.getString(2));
        // add to JoinRowSet
        jrs = newJoinRowSet();
        jrs.addRowSet(noInitialCrset, 1);
        // check data
        assertSame(noInitialCrset.getMetaData(), jrs.getMetaData());
        assertTrue(jrs.last());
        assertEquals("test4", jrs.getString(2));
        // call execute(conn)
        jrs.setCommand("select * from USER_INFO");
        try {
            jrs.execute(conn);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        // check data
        assertSame(noInitialCrset.getMetaData(), jrs.getMetaData());
        assertFalse(jrs.first());
        assertTrue(noInitialCrset.first());
        assertEquals("hermit", noInitialCrset.getString(2));

        /*
         * Add two CachedRowSets which both can call execute(conn)
         */
        crset = newNoInitialInstance();
        crset.setCommand("select * from USER_INFO");
        crset.execute(conn);
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand("select * from BOOKS");
        noInitialCrset.execute(conn);
        // add to JoinRowSet
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        jrs.addRowSet(noInitialCrset, 1);
        // it would throw SQLException when more than one RowSet is added
        try {
            jrs.execute(conn);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        // check data
        assertNotNull(jrs.getMetaData());
        assertFalse(jrs.first());
    }

    public void testCursorMoved() throws Exception {
        Listener listener = new Listener();
        jrs.addRowSetListener(listener);
        assertNull(listener.getTag());

        jrs.addRowSet(crset, 1);

        assertTrue(jrs.absolute(1));
        assertNull(listener.getTag());

        assertTrue(jrs.next());
        assertNull(listener.getTag());
    }

    public void testRowChanged() throws Exception {
        Listener listener = new Listener();
        jrs.addRowSetListener(listener);
        assertNull(listener.getTag());
        jrs.addRowSet(crset, 1);

        assertTrue(jrs.absolute(1));
        assertNull(listener.getTag());
        jrs.setOriginalRow();
        assertNull(listener.getTag());

        /*
         * updateRow() - rowChanged
         */
        assertTrue(jrs.absolute(3));
        jrs.updateString(2, "abc");
        assertNull(listener.getTag());
        jrs.updateRow();
        assertNull(listener.getTag());

        /*
         * deleteRow() - rowChanged
         */
        assertTrue(jrs.last());
        jrs.deleteRow();
        assertNull(listener.getTag());
    }

    public void testAcceptChanges_Exception() throws Exception {
        try {
            jrs.acceptChanges();
            fail("Should throw SyncProviderException.");
        } catch (SyncProviderException e) {
            // Expected.
        }

        try {
            jrs.acceptChanges(conn);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }

        jrs.addRowSet(crset, 1);

        jrs.acceptChanges();
        jrs.acceptChanges(conn);

        jrs.absolute(4);
        jrs.updateString(2, "Updated");
        jrs.updateRow();

        jrs.acceptChanges();

        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        try {
            jrs.acceptChanges();
            fail("Should throw SyncProviderException.");
        } catch (SyncProviderException e) {
            // Expected.
        }

        jrs.acceptChanges(conn);
        jrs.acceptChanges();
    }

    public void testAcceptChanges_Insert() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.absolute(4);
        jrs.moveToInsertRow();
        jrs.updateString(2, "Inserted");
        jrs.updateInt(1, 5);
        jrs.insertRow();
        jrs.moveToCurrentRow();

        try {
            jrs.acceptChanges();
            fail("Should throw SyncProviderException.");
        } catch (SyncProviderException e) {
            // Expected.
        }
        jrs.setTableName("USER_INFO");
        jrs.acceptChanges(conn);
        rs = st.executeQuery("select * from USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);
        int rowNum = 0;
        crset.beforeFirst();
        while (crset.next()) {
            rowNum++;
        }
        assertEquals(5, rowNum);

        assertTrue(jrs.absolute(5));
        assertEquals("Inserted", jrs.getString(2));
    }

    public void testAcceptChanges_Update() throws Exception {
        jrs.addRowSet(crset, 1);

        jrs.absolute(4);
        jrs.updateString(2, "Updated");
        jrs.updateRow();
        jrs.moveToCurrentRow();

        try {
            jrs.acceptChanges();
            fail("Should throw SyncProviderException.");

        } catch (SyncProviderException e) {
            // Expected.
        }
        jrs.acceptChanges(conn);

        rs = st.executeQuery("select * from USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.absolute(4);
        assertEquals("Updated", crset.getString(2));

        jrs.absolute(4);
        assertEquals("Updated", jrs.getString(2));
    }

    public void testAcceptChanges_Delete() throws Exception {
        jrs.addRowSet(crset, 1);

        jrs.absolute(4);

        jrs.deleteRow();
        jrs.setShowDeleted(true);

        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }

        jrs.acceptChanges(conn);

        rs = st.executeQuery("select * from USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        rowNum = 0;
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
        // In RI, the deletion will not affect database.
        // In harmony, The deletion will happen in database according to spec.
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(3, rowNum);
        } else {
            assertEquals(4, rowNum);
        }
    }

    public void testAcceptChanges_TwoSameRowSets() throws Exception {
        jrs.addRowSet(crset, 1);
        crset.beforeFirst();
        jrs.addRowSet(crset, 1);

        jrs.absolute(4);
        jrs.updateString(2, "Updated");
        jrs.updateRow();

        try {
            jrs.acceptChanges(conn);
            fail("Should throw SyncProviderException.");
        } catch (SyncProviderException e) {
            // Expected.
        }
    }

    public void testAcceptChange_UpdateOriginalRowSet() throws Exception {
        jrs.addRowSet(crset, 1);
        crset.absolute(4);
        crset.updateString(2, "Updated");
        crset.updateRow();

        jrs.acceptChanges(conn);

        rs = st.executeQuery("select * from USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);
        crset.absolute(4);
        assertEquals("test4", crset.getString(2));
    }

    public void testUndoDeleted_Empty() throws Exception {
        // No Exception.
        jrs.undoDelete();
    }

    public void testUndoDeleted() throws Exception {
        jrs.addRowSet(crset, 1);

        /*
         * Test exception. It would throw SQLException when calling undoDelete()
         * in such conditions.
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            // TODO RI doesn't follow the spec, we follow spec
            jrs.beforeFirst();
            assertTrue(jrs.isBeforeFirst());
            try {
                jrs.undoDelete();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, invalid cursor
            }

            jrs.afterLast();
            assertTrue(jrs.isAfterLast());
            try {
                jrs.undoDelete();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, invalid cursor
            }

            jrs.moveToInsertRow();
            try {
                jrs.undoDelete();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, invalid cursor
            }

            jrs.moveToCurrentRow();
            assertTrue(jrs.absolute(1));
            assertFalse(jrs.rowDeleted());
            try {
                jrs.undoDelete();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, invalid cursor
            }

        } else {
            // when run on RI, it won't throw SQLException
            jrs.beforeFirst();
            assertTrue(jrs.isBeforeFirst());
            jrs.undoDelete();

            jrs.afterLast();
            assertTrue(jrs.isAfterLast());
            jrs.undoDelete();

            jrs.moveToInsertRow();
            jrs.undoDelete();

            jrs.moveToCurrentRow();
            assertTrue(jrs.absolute(1));
            assertFalse(jrs.rowDeleted());
            jrs.undoDelete();
        }

        // delete the fourth row
        assertTrue(jrs.absolute(4));
        assertEquals(4, jrs.getInt(1));
        assertFalse(jrs.rowDeleted());
        jrs.deleteRow();
        assertTrue(jrs.rowDeleted());

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            jrs.undoDelete();
            assertFalse(jrs.rowDeleted());
            jrs.acceptChanges(conn);

            // check DB
            rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 4");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));

            // check CachedRowSet
            jrs.beforeFirst();
            int index = 0;
            while (jrs.next()) {
                index++;
                assertEquals(index, jrs.getInt(1));
            }
            assertEquals(4, index);

        } else {
            // TODO undoDelete() still makes no difference in RI
            jrs.undoDelete();
            assertTrue(jrs.rowDeleted());
            jrs.acceptChanges(conn);

            // check DB
            rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 4");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));

            // check CachedRowSet
            jrs.beforeFirst();
            int index = 0;
            while (jrs.next()) {
                index++;
                assertEquals(index, jrs.getInt(1));
            }
            assertEquals(3, index);
        }

    }

    public void testUndoInsert_Empty() throws Exception {
        try {
            jrs.undoInsert();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throws it.
        }
    }

    public void testAcceptChanges_Complex() throws Exception {
        crset.acceptChanges();

        jrs.addRowSet(crset, 1);
        try {
            jrs.acceptChanges();
            fail("Should throw SyncProviderException.");
        } catch (SyncProviderException e) {
            // Expected.
        }
        crset.acceptChanges();

        jrs.acceptChanges(conn);
        jrs.acceptChanges();

        try {
            jrs.acceptChanges(null);
            fail("Should throw SyncProviderException.");
        } catch (SyncProviderException e) {
            // Expected.
        }

        try {
            jrs.acceptChanges();
            fail("Should throw SyncProviderException.");
        } catch (SyncProviderException e) {
            // Expected.
        }
    }
}
