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

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.spi.SyncProvider;

public class JoinRowSetJoinTest extends JoinRowSetTestCase {

    public void testJoin_DataEqual() throws Exception {
        boolean dataEquals;
        jrs.addRowSet(crset, 1);
        dataEquals = dataEqualsIgnoreOrder(jrs, crset);
        if (!dataEquals) {
            fail("The data in jrs and crset should be equal.");
        }

        // Creates a new CachedRowSet from BOOKs
        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from BOOKS");
        crset2.populate(rs);
        jrs.addRowSet(crset2, 1);

        CachedRowSet dbJoinCrset = newNoInitialInstance();
        rs = st
                .executeQuery("select USER_INFO.ID, USER_INFO.NAME, USER_INFO.BIGINT_T, USER_INFO.NUMERIC_T, USER_INFO.DECIMAL_T, "
                        + "USER_INFO.SMALLINT_T, USER_INFO.FLOAT_T, USER_INFO.REAL_T, USER_INFO.DOUBLE_T, USER_INFO.DATE_T, USER_INFO.TIME_T, USER_INFO.TIMESTAMP_T, "
                        + "BOOKS.SN, BOOKS.NAME"
                        + " from USER_INFO, BOOKS "
                        + "where USER_INFO.ID = BOOKS.AUTHORID");
        dbJoinCrset.populate(rs);

        dataEquals = dataEqualsIgnoreOrder(jrs, dbJoinCrset);
        if (!dataEquals) {
            fail("The data is jrs and dbJoinCrset should be equal.");
        }

        jrs = newJoinRowSet();
        jrs.addRowSet(crset2, 1);
        dataEquals = dataEqualsIgnoreOrder(crset2, jrs);
        if (!dataEquals) {
            fail("The data is jrs and crset2 should be equal.");
        }

        rs = st
                .executeQuery("select BOOKS.AUTHORID, BOOKS.SN, BOOKS.NAME, USER_INFO.NAME, USER_INFO.BIGINT_T, USER_INFO.NUMERIC_T, USER_INFO.DECIMAL_T, "
                        + "USER_INFO.SMALLINT_T, USER_INFO.FLOAT_T, USER_INFO.REAL_T, USER_INFO.DOUBLE_T, USER_INFO.DATE_T, USER_INFO.TIME_T, USER_INFO.TIMESTAMP_T "
                        + " from USER_INFO, BOOKS "
                        + "where USER_INFO.ID = BOOKS.AUTHORID");
        dbJoinCrset = newNoInitialInstance();
        dbJoinCrset.populate(rs);

        jrs.addRowSet(crset, 1);
        dataEquals = dataEqualsIgnoreOrder(jrs, dbJoinCrset);
        if (!dataEquals) {
            fail("The data is jrs and dbJoinCrset should be equal.");
        }
    }

    public void testJoin_DataEqual_StringColumn() throws Exception {
        boolean dataEquals;
        jrs.addRowSet(crset, 2);

        dataEquals = dataEqualsIgnoreOrder(jrs, crset);
        if (!dataEquals) {
            fail("The data in jrs and crset should be equal.");
        }

        // Creates a new CachedRowSet from BOOKs
        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from BOOKS");
        crset2.populate(rs);

        jrs.addRowSet(crset2, 3);

        CachedRowSet dbJoinCrset = newNoInitialInstance();
        rs = st
                .executeQuery("select USER_INFO.ID, USER_INFO.NAME, USER_INFO.BIGINT_T, USER_INFO.NUMERIC_T, USER_INFO.DECIMAL_T, "
                        + "USER_INFO.SMALLINT_T, USER_INFO.FLOAT_T, USER_INFO.REAL_T, USER_INFO.DOUBLE_T, USER_INFO.DATE_T, USER_INFO.TIME_T, USER_INFO.TIMESTAMP_T, "
                        + "BOOKS.AUTHORID, BOOKS.SN"
                        + " from USER_INFO, BOOKS "
                        + "where USER_INFO.NAME = BOOKS.NAME");
        dbJoinCrset.populate(rs);

        dataEquals = dataEqualsIgnoreOrder(jrs, dbJoinCrset);
        if (!dataEquals) {
            fail("The data is jrs and dbJoinCrset should be equal.");
        }

        jrs = newJoinRowSet();
        jrs.addRowSet(crset2, 3);
        dataEquals = dataEqualsIgnoreOrder(crset2, jrs);
        if (!dataEquals) {
            fail("The data is jrs and crset2 should be equal.");
        }

        rs = st
                .executeQuery("select BOOKS.AUTHORID, BOOKS.SN, BOOKS.NAME, USER_INFO.ID, USER_INFO.BIGINT_T, USER_INFO.NUMERIC_T, USER_INFO.DECIMAL_T, "
                        + "USER_INFO.SMALLINT_T, USER_INFO.FLOAT_T, USER_INFO.REAL_T, USER_INFO.DOUBLE_T, USER_INFO.DATE_T, USER_INFO.TIME_T, USER_INFO.TIMESTAMP_T "
                        + " from USER_INFO, BOOKS "
                        + "where USER_INFO.NAME = BOOKS.NAME");
        dbJoinCrset = newNoInitialInstance();
        dbJoinCrset.populate(rs);

        jrs.addRowSet(crset, 2);
        dataEquals = dataEqualsIgnoreOrder(jrs, dbJoinCrset);
        if (!dataEquals) {
            fail("The data is jrs and dbJoinCrset should be equal.");
        }
    }

    public void testJoin_DataEqual_MoreData() throws Exception {
        insertMoreData(20);
        rs = st.executeQuery("select * from USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        boolean dataEquals;
        jrs.addRowSet(crset, 1);
        dataEquals = dataEqualsIgnoreOrder(jrs, crset);
        if (!dataEquals) {
            fail("The data in jrs and crset should be equal.");
        }

        // Creates a new CachedRowSet from BOOKs
        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from BOOKS");
        crset2.populate(rs);

        jrs.addRowSet(crset2, 1);

        CachedRowSet dbJoinCrset = newNoInitialInstance();
        rs = st
                .executeQuery("select USER_INFO.ID, USER_INFO.NAME, USER_INFO.BIGINT_T, USER_INFO.NUMERIC_T, USER_INFO.DECIMAL_T, "
                        + "USER_INFO.SMALLINT_T, USER_INFO.FLOAT_T, USER_INFO.REAL_T, USER_INFO.DOUBLE_T, USER_INFO.DATE_T, USER_INFO.TIME_T, USER_INFO.TIMESTAMP_T, "
                        + "BOOKS.SN, BOOKS.NAME"
                        + " from USER_INFO, BOOKS "
                        + "where USER_INFO.ID = BOOKS.AUTHORID");
        dbJoinCrset.populate(rs);

        dataEquals = dataEqualsIgnoreOrder(jrs, dbJoinCrset);
        if (!dataEquals) {
            fail("The data is jrs and dbJoinCrset should be equal.");
        }

        jrs = newJoinRowSet();
        jrs.addRowSet(crset2, 1);
        dataEquals = dataEqualsIgnoreOrder(crset2, jrs);
        if (!dataEquals) {
            fail("The data is jrs and crset2 should be equal.");
        }

        rs = st
                .executeQuery("select BOOKS.AUTHORID, BOOKS.SN, BOOKS.NAME, USER_INFO.NAME, USER_INFO.BIGINT_T, USER_INFO.NUMERIC_T, USER_INFO.DECIMAL_T, "
                        + "USER_INFO.SMALLINT_T, USER_INFO.FLOAT_T, USER_INFO.REAL_T, USER_INFO.DOUBLE_T, USER_INFO.DATE_T, USER_INFO.TIME_T, USER_INFO.TIMESTAMP_T "
                        + " from USER_INFO, BOOKS "
                        + "where USER_INFO.ID = BOOKS.AUTHORID");
        dbJoinCrset = newNoInitialInstance();
        dbJoinCrset.populate(rs);

        jrs.addRowSet(crset, 1);
        dataEquals = dataEqualsIgnoreOrder(jrs, dbJoinCrset);
        if (!dataEquals) {
            fail("The data is jrs and dbJoinCrset should be equal.");
        }

    }

    public void testJoin_DataEqual_NullData() throws Exception {
        insertNullDataToBooks();
        insertNullDataToUserInfo();

        boolean dataEquals;
        jrs.addRowSet(crset, 2);
        dataEquals = dataEqualsIgnoreOrder(jrs, crset);
        if (!dataEquals) {
            fail("The data in jrs and crset should be equal.");
        }

        // Creates a new CachedRowSet from BOOKs
        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from BOOKS");
        crset2.populate(rs);

        jrs.addRowSet(crset2, 3);

        CachedRowSet dbJoinCrset = newNoInitialInstance();
        rs = st
                .executeQuery("select USER_INFO.ID, USER_INFO.NAME, USER_INFO.BIGINT_T, USER_INFO.NUMERIC_T, USER_INFO.DECIMAL_T, "
                        + "USER_INFO.SMALLINT_T, USER_INFO.FLOAT_T, USER_INFO.REAL_T, USER_INFO.DOUBLE_T, USER_INFO.DATE_T, USER_INFO.TIME_T, USER_INFO.TIMESTAMP_T, "
                        + "BOOKS.AUTHORID, BOOKS.SN"
                        + " from USER_INFO, BOOKS "
                        + "where USER_INFO.NAME = BOOKS.NAME");
        dbJoinCrset.populate(rs);

        dataEquals = dataEqualsIgnoreOrderAndNullInR2(jrs, dbJoinCrset, 2);
        if (!dataEquals) {
            fail("The data is jrs and dbJoinCrset should be equal.");
        }

        jrs = newJoinRowSet();
        jrs.addRowSet(crset2, 3);
        dataEquals = dataEqualsIgnoreOrder(crset2, jrs);
        if (!dataEquals) {
            fail("The data is jrs and crset2 should be equal.");
        }

        rs = st
                .executeQuery("select BOOKS.AUTHORID, BOOKS.SN, BOOKS.NAME, USER_INFO.ID, USER_INFO.BIGINT_T, USER_INFO.NUMERIC_T, USER_INFO.DECIMAL_T, "
                        + "USER_INFO.SMALLINT_T, USER_INFO.FLOAT_T, USER_INFO.REAL_T, USER_INFO.DOUBLE_T, USER_INFO.DATE_T, USER_INFO.TIME_T, USER_INFO.TIMESTAMP_T "
                        + " from USER_INFO, BOOKS "
                        + "where USER_INFO.NAME = BOOKS.NAME");
        dbJoinCrset = newNoInitialInstance();
        dbJoinCrset.populate(rs);

        jrs.addRowSet(crset, 2);
        dataEquals = dataEqualsIgnoreOrderAndNullInR2(jrs, dbJoinCrset, 3);
        if (!dataEquals) {
            fail("The data is jrs and dbJoinCrset should be equal.");
        }
    }

    public void testJoin_SameTable() throws Exception {
        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from BOOKS");
        crset2.populate(rs);

        CachedRowSet dbJoinCrset = newNoInitialInstance();
        rs = st.executeQuery("select BOOKS1.AUTHORID, BOOKS1.SN, BOOKS1.NAME, "
                + "BOOKS.AUTHORID, BOOKS.NAME"
                + " from BOOKS as BOOKS1, BOOKS "
                + "where BOOKS1.SN = BOOKS.SN");
        dbJoinCrset.populate(rs);

        jrs.addRowSet(crset2, "SN");
        jrs.addRowSet(crset2, "SN");

        boolean dataEquals = dataEqualsIgnoreOrder(jrs, dbJoinCrset);
        if (!dataEquals) {
            fail("The data is jrs and dbJoinCrset should be equal.");
        }

    }

    public void testJoin_Unsortable() throws Exception {
        addUnsortableToBooksTable();

        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from BOOKS");
        crset2.populate(rs);

        boolean dataEquals;
        try {
            jrs.addRowSet(crset2, 4);
            if ("true".equals(System.getProperty("Testing Harmony"))) {
                fail("Should throw SQLException here.(In harmony)");
            } else {

            }
        } catch (SQLException e) {
            if (!"true".equals(System.getProperty("Testing Harmony"))) {
                fail("Should not throw SQLException here.(In RI)");
            }
        }
        /*
         * dataEquals = dataEqualsIgnoreOrder(jrs, crset2); if (!dataEquals) {
         * fail("The data in jrs and crset2 should be equal."); }
         */
        try {
            jrs.addRowSet(crset2, 4);
            fail("Should throw SQLException here.");
        } catch (SQLException e) {
            // Expected.
        }
    }

    // It proves that when the rowset is the first one added to the joinRowSet,
    // it cursor doesn't move.
    // Else, the cursor moves to the last one (not afterLast).
    public void testJoin_OriginalCursorPosition() throws Exception {
        // Create crset2.
        rs = st.executeQuery("select * from BOOKS");
        CachedRowSet crset2 = newNoInitialInstance();
        crset2.populate(rs);

        assertTrue(crset.isBeforeFirst());
        jrs.addRowSet(crset, 1);
        assertTrue(crset.isBeforeFirst());

        crset.absolute(3);

        jrs.addRowSet(crset, 1);
        assertTrue(crset.isLast());

        crset.last();
        jrs.addRowSet(crset, 1);
        assertTrue(crset.isLast());

        crset.afterLast();
        jrs.addRowSet(crset, 1);
        assertTrue(crset.isLast());

        crset.beforeFirst();
        jrs.addRowSet(crset, 1);
        assertTrue(crset.isLast());

        crset.first();
        crset2.beforeFirst();
        jrs.addRowSet(crset2, 1);
        assertTrue(crset2.isLast());
        assertTrue(crset.isFirst());

        jrs = newJoinRowSet();
        crset.first();
        jrs.addRowSet(crset, 1);
        assertTrue(crset.isFirst());

        jrs = newJoinRowSet();
        crset.absolute(2);
        jrs.addRowSet(crset, 1);
        assertEquals(2, crset.getInt(1));
    }

    public void testJoin_CursorPosition() throws Exception {
        rs = st.executeQuery("select * from BOOKS");
        CachedRowSet crset2 = newNoInitialInstance();
        crset2.populate(rs);

        // TODO
        // Tests when there is no rowSet.
        // Not determined in harmony.
        // assertTrue(jrs.isLast());
        // assertTrue(jrs.isFirst());

        // Tests when there is only one rowSet.
        jrs.addRowSet(crset, 1);
        assertTrue(jrs.isBeforeFirst());

        jrs = newJoinRowSet();
        crset.last();
        jrs.addRowSet(crset, 1);
        assertTrue(jrs.isLast());

        // Tests where there are two rowset.
        jrs = newJoinRowSet();
        crset.last();
        jrs.addRowSet(crset, 1);
        assertTrue(jrs.last());
        jrs.addRowSet(crset2, 1);
        assertTrue(jrs.isBeforeFirst());

        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        crset2.absolute(1);
        jrs.addRowSet(crset2, 1);
        assertTrue(jrs.isBeforeFirst());

        crset.last();
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        assertTrue(jrs.isLast());
        jrs.addRowSet(crset2, 1);
        assertTrue(jrs.isBeforeFirst());

        crset.last();
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        assertTrue(jrs.isLast());
        crset2.last();
        jrs.addRowSet(crset2, 1);
        assertTrue(jrs.isBeforeFirst());

        crset.last();
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        assertTrue(jrs.isLast());
        crset2.absolute(1);
        jrs.addRowSet(crset2, 1);
        assertTrue(jrs.isBeforeFirst());
    }

    public void testJoin_Update() throws Exception {
        rs = st.executeQuery("select * from BOOKS");
        CachedRowSet crset2 = newNoInitialInstance();
        crset2.populate(rs);

        crset.first();
        crset.updateString(2, "Updated");

        jrs.addRowSet(crset, 1);
        jrs.beforeFirst();
        int count = 0;
        while (jrs.next()) {
            if (jrs.getInt(1) == 1) {
                count++;
                assertEquals("Updated", jrs.getString(2));
            }
        }
        assertEquals(1, count);

        crset2.first();
        crset2.updateString(2, "Updated2");

    }

    public void testJoin_Delete() throws Exception {
        rs = st.executeQuery("select * from BOOKS");
        CachedRowSet crset2 = newNoInitialInstance();
        crset2.populate(rs);

        jrs.addRowSet(crset, 1);
        jrs.first();
        jrs.deleteRow();

        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);

        jrs.addRowSet(crset2, 1);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);
    }

    public void testJoin_Delete2() throws Exception {
        rs = st.executeQuery("select * from BOOKS");
        CachedRowSet crset2 = newNoInitialInstance();
        crset2.populate(rs);

       crset.absolute(3);
        crset.deleteRow();
        crset.setShowDeleted(true);

        jrs.addRowSet(crset, 1);
        jrs.first();
        jrs.deleteRow();
        
        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);

        jrs.setShowDeleted(true);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);

        
        jrs.beforeFirst();
        while (jrs.next()) {
            if (jrs.getInt(1) == 3) {
                assertTrue(jrs.rowDeleted());
            }
        }

        crset2.absolute(6);
        crset2.deleteRow();

        crset2.beforeFirst();
        while (crset2.next()) {
            if (crset2.getInt(1) == 4) {
                assertTrue(crset2.rowDeleted());
            }
        }

        jrs.addRowSet(crset2, 1);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(5, rowNum);
    }

    public void testProperty_Empty() throws SQLException {
        assertNull(jrs.getCommand());
        assertEquals(ResultSet.CONCUR_UPDATABLE, jrs.getConcurrency());
        try {
            jrs.getCursorName();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }
        assertNull(jrs.getDataSourceName());
        assertTrue(jrs.getEscapeProcessing());
        assertEquals(ResultSet.FETCH_FORWARD, jrs.getFetchDirection());
        assertEquals(0, jrs.getFetchSize());
        assertEquals(0, jrs.getMaxFieldSize());
        assertEquals(0, jrs.getMaxRows());
        assertNull(jrs.getMetaData());
        assertEquals(0, jrs.getPageSize());
        assertNull(jrs.getPassword());
        assertEquals(0, jrs.getQueryTimeout());
        assertFalse(jrs.getShowDeleted());
        assertNull(jrs.getStatement());
        assertNotNull(jrs.getSyncProvider());
        assertNull(jrs.getTableName());
        assertNull(jrs.getUrl());
        assertNull(jrs.getUsername());
        assertNull(jrs.getTypeMap());

        try {
            jrs.getOriginal();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // RI throws it.
        }

        try {
            jrs.getOriginalRow();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        } catch (NullPointerException e) {
            // RI throws it.
        }
    }

    public void testProperty() throws Exception {
        SyncProvider sp = jrs.getSyncProvider();

        crset.setDataSourceName("testDataSource");
        crset.setCommand("testCommand");
        crset.setConcurrency(ResultSet.CONCUR_READ_ONLY);
        crset.setEscapeProcessing(false);
        crset.setFetchDirection(ResultSet.FETCH_REVERSE);
        crset.setMaxRows(80);
        // TODO How to set it.
        // crset.setFetchSize(10);
        crset.setPageSize(20);
        crset.setPassword("testPassword");
        crset.setQueryTimeout(5);
        crset.setShowDeleted(true);
        crset.setTableName("testTableName");
        crset.setUrl("testUrl");
        ;
        crset.setUsername("testUserName");
        crset.setMaxFieldSize(20);
        jrs.addRowSet(crset, 1);

        assertNull(jrs.getCommand());
        assertEquals(ResultSet.CONCUR_UPDATABLE, jrs.getConcurrency());
        assertNull(jrs.getDataSourceName());
        assertTrue(jrs.getEscapeProcessing());
        assertEquals(ResultSet.FETCH_FORWARD, jrs.getFetchDirection());
        assertEquals(crset.getFetchSize(), jrs.getFetchSize());
        assertEquals(0, jrs.getMaxFieldSize());
        assertEquals(0, jrs.getMaxRows());
        assertEquals(0, jrs.getPageSize());
        assertNull(jrs.getPassword());
        assertEquals(0, jrs.getQueryTimeout());
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(jrs.getShowDeleted());
        }
        else {
            assertFalse(jrs.getShowDeleted());
        }
        assertNull(jrs.getStatement());
        assertEquals(sp, jrs.getSyncProvider());
        // TODO
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(crset.getTableName(), jrs.getTableName());
        }
        else {
            assertNull(jrs.getTableName());
        }
        assertNull(jrs.getUrl());
        assertNull(jrs.getUsername());
        assertNotNull(jrs.getOriginal());
        assertEquals(crset.getTypeMap(), jrs.getTypeMap());
    }

    public void testProperty_TwoRowSets() throws Exception {
        jrs.addRowSet(crset, 1);
        rs = st.executeQuery("select * from BOOKS");
        CachedRowSet crset2 = newNoInitialInstance();
        crset2.populate(rs);
        crset2.setDataSourceName("testDataSourceName");
        jrs.addRowSet(crset2, 1);

        assertEquals(crset2.getTypeMap(), jrs.getTypeMap());
        assertEquals(crset.getTypeMap(), crset2.getTypeMap());
    }

    public void testRowProperty_deleteRow() throws Exception {
        crset.first();
        crset.deleteRow();
        int rowNum = 0;
        crset.beforeFirst();
        while (crset.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);

        jrs.addRowSet(crset, 1);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);

        jrs.setShowDeleted(true);
        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);

        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from books");
        crset2.populate(rs);

        crset2.absolute(3);
        crset2.deleteRow();

        jrs.addRowSet(crset2, 1);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);
    }

    public void testRowProperty_deleteRow2() throws Exception {
        crset.first();
        crset.deleteRow();
        crset.setShowDeleted(true);
        int rowNum = 0;
        crset.beforeFirst();
        while (crset.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);

        jrs.addRowSet(crset, 1);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
            if (jrs.getInt(1) == 1) {
                assertTrue(jrs.rowDeleted());
            } else {
                assertFalse(jrs.rowDeleted());
            }
        }
        if (System.getProperty("Testing Harmony") == "true") {
            assertTrue(jrs.getShowDeleted());
        }
        else {
        assertFalse(jrs.getShowDeleted());
        }
        assertEquals(4, rowNum);

        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from books");
        crset2.populate(rs);

        crset2.absolute(3);
        crset2.deleteRow();
        crset2.setShowDeleted(true);

        jrs.addRowSet(crset2, 1);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
            assertFalse(jrs.rowDeleted());
        }
        assertEquals(6, rowNum);
    }

    public void testRowProperty_deleteRow3() throws Exception {
        crset.first();
        crset.deleteRow();
        crset.setShowDeleted(true);
        int rowNum = 0;
        crset.beforeFirst();
        while (crset.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);

        jrs.addRowSet(crset, 1);

        jrs.setShowDeleted(true);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
            if (jrs.getInt(1) == 1) {
                assertTrue(jrs.rowDeleted());
            } else {
                assertFalse(jrs.rowDeleted());
            }
        }
        assertEquals(4, rowNum);
        assertTrue(jrs.getShowDeleted());

        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from books");
        crset2.populate(rs);

        crset2.absolute(3);
        crset2.deleteRow();
        crset2.setShowDeleted(true);

        jrs.addRowSet(crset2, 1);
        jrs.setShowDeleted(false);

        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
            assertFalse(jrs.rowDeleted());
        }
        assertEquals(6, rowNum);
    }

    protected int getVisibleSize(ResultSet rs) throws SQLException {
        rs.beforeFirst();
        int rowNum = 0;
        while (rs.next()) {
            rowNum++;
        }
        return rowNum;
    }
    
    public void testJoin_OriginalShowDeleted() throws SQLException {
        jrs.addRowSet(crset, 1);
        
        jrs.first();
        jrs.deleteRow();
        
        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);
        
        crset.setShowDeleted(true);
        assertEquals(3, rowNum);     
    }
    
    public void testJoin_OriginalShowDeleted2() throws SQLException {
        crset.setShowDeleted(true);
        jrs.addRowSet(crset, 1);
        
        jrs.first();
        jrs.deleteRow();
        
        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);
        assertTrue(jrs.absolute(4));
        
        if (System.getProperty("Testing Harmony") == "true") {
            assertTrue(jrs.getShowDeleted());
        }
        else 
        {
        assertFalse(jrs.getShowDeleted());
        }
        crset.setShowDeleted(false);
        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);
        
        crset.first();
        jrs.beforeFirst();
        assertTrue(crset.isFirst());
        assertFalse(crset.isBeforeFirst());
        assertTrue(jrs.isBeforeFirst());
        
        jrs.next();
        assertTrue(crset.isFirst());
        assertTrue(jrs.isFirst());
        
        jrs.next();
        assertTrue(crset.isFirst());
        assertFalse(jrs.isFirst());
    }
    
    public void testJoin_OriginalShowDeleted3() throws Exception {
        CachedRowSet crset2 = newNoInitialInstance();
        rs = st.executeQuery("select * from books");
        crset2.populate(rs);
        
        jrs.addRowSet(crset, 1);
        jrs.first();
        jrs.deleteRow();
        
        int rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);
        
        crset2.setShowDeleted(true);
        jrs.addRowSet(crset2, 1);
        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(4, rowNum);
        
        jrs.first();
        jrs.deleteRow();
        
        rowNum = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            rowNum++;
        }
        assertEquals(3, rowNum);
        
        
        
    }

}
