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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.FilteredRowSet;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.Predicate;
import javax.sql.rowset.WebRowSet;

public class JoinRowSetTest extends JoinRowSetTestCase {

    public void testAddJoinableRowSet_CachedRowSet_Exception() throws Exception {
        try {
            jrs.addRowSet(null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            // CachedRowSet didn't set a match column
            jrs.addRowSet(crset);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        /*
         * Test empty CachedRowSet. According to the spec, it should throw
         * SQLException if the given rowset is empty. However, RI won't.
         */
        noInitialCrset.setMatchColumn(1);
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                jrs.addRowSet(noInitialCrset);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            assertNull(noInitialCrset.getMetaData());
            jrs.addRowSet(noInitialCrset);
        }
        jrs.close();

        /*
         * Test CachedRowSet filled with data, but with an invalid column index.
         * RI doesn't check the column index strictly.
         */
        jrs = newJoinRowSet();
        crset.setMatchColumn(13);
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                jrs.addRowSet(crset);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            jrs.addRowSet(crset);
        }
        jrs.close();

        /*
         * Test CachedRowSet filled with data, but with an invalid column name
         */
        jrs = newJoinRowSet();
        crset = newNoInitialInstance();
        crset.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        crset.setMatchColumn("abc");
        try {
            jrs.addRowSet(crset);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.close();
    }

    public void testAddJoinableRowSet_CachedRowSet_MetaData() throws Exception {
        assertNull(jrs.getMetaData());
        crset.setMatchColumn(1);
        jrs.addRowSet(crset);
        assertSame(jrs.getMetaData(), crset.getMetaData());

        noInitialCrset.setMatchColumn("AUTHORID");
        noInitialCrset.populate(st.executeQuery("SELECT * FROM BOOKS"));
        jrs.addRowSet(noInitialCrset);

        /*
         * The match column is named "MergedCol" in RI.
         */
        assertEquals("MergedCol", jrs.getMetaData().getColumnName(1));
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            isMetaDataEqualsInColCount(noInitialCrset.getMetaData(), 2,
                    noInitialCrset.getMetaData().getColumnCount(), jrs
                            .getMetaData(), 13);
        }
        isMetaDataEqualsInColCount(crset.getMetaData(), 2, crset.getMetaData()
                .getColumnCount(), jrs.getMetaData(), 2);

        /*
         * Add another RowSet
         */
        insertDataToCustomerTable();
        CachedRowSet thirdRs = newNoInitialInstance();
        thirdRs.populate(st.executeQuery("SELECT * FROM CUSTOMER_INFO"));
        jrs.addRowSet(thirdRs, "ID");

        // check metadata
        assertEquals(15, jrs.getMetaData().getColumnCount());
        isMetaDataEqualsInColCount(crset.getMetaData(), 2, crset.getMetaData()
                .getColumnCount(), jrs.getMetaData(), 2);
        assertEquals("NAME", jrs.getMetaData().getColumnName(15));
        jrs.close();
    }

    public void testAddJoinableRowSet_CachedRowSet_MetaData2() throws Exception {
        /*
         * Add the first CachedRowSet, populate with table BOOKS
         */
        noInitialCrset.populate(st.executeQuery("SELECT * FROM BOOKS"));
        jrs.addRowSet(noInitialCrset, "NAME");
        assertEquals(jrs.getMetaData(), noInitialCrset.getMetaData());

        /*
         * Add the second CachedRowSet, populate with table CUSTOMER_INFO
         */
        insertDataToCustomerTable();
        CachedRowSet newCrset = newNoInitialInstance();
        newCrset.populate(st.executeQuery("SELECT * FROM CUSTOMER_INFO"));
        jrs.addRowSet(newCrset, "NAME");

        /*
         * TODO NOTICE The difference between RI and Harmony
         */
        ResultSetMetaData rsmd = jrs.getMetaData();
        assertEquals(4, rsmd.getColumnCount());
        assertEquals("AUTHORID", rsmd.getColumnName(1));
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals("SN", rsmd.getColumnName(2));
            assertEquals("MergedCol", rsmd.getColumnName(3));
        } else {
            assertEquals("MergedCol", rsmd.getColumnName(2));
            assertEquals("NAME", rsmd.getColumnName(3));
        }
        assertEquals("ID", rsmd.getColumnName(4));

        /*
         * Add the third CachedRowSet, populate with table USER_INFO
         */
        jrs.addRowSet(crset, 2);
        rsmd = jrs.getMetaData();
        assertEquals(15, rsmd.getColumnCount());
    }

    public void testAddJoinableRowSet_CachedRowSet_Int_Exception()
            throws Exception {
        try {
            jrs.addRowSet(null, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        /*
         * Test empty CachedRowSet
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                // can't be empty
                jrs.addRowSet(noInitialCrset, 1);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            jrs.addRowSet(noInitialCrset, 1);
        }
        jrs.close();

        /*
         * Test non-empty CachedRowSet with invalid match column
         */
        try {
            jrs.addRowSet(crset, -1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            jrs.addRowSet(crset, 13);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            jrs.addRowSet(crset, "abc");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.close();
    }

    public void testAddJoinableRowSet_CachedRowSet_IntArray_Exception()
            throws Exception {
        RowSet[] rowSets = new RowSet[2];
        int[] matchCols = new int[1];
        try {
            jrs.addRowSet(rowSets, matchCols);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        matchCols = new int[2];
        try {
            jrs.addRowSet(rowSets, matchCols);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        rowSets = new RowSet[] { crset, noInitialCrset };
        matchCols = new int[] { 1, 1 };
        try {
            jrs.addRowSet(rowSets, matchCols);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        rowSets = new RowSet[] { crset, noInitialCrset };
        jrs.addRowSet(rowSets, matchCols);
        jrs.close();
    }

    public void testAddJoinableRowSet_CachedRowSet_StringArray_Exception()
            throws Exception {
        RowSet[] rowSets = new RowSet[2];
        String[] matchCols = new String[1];
        try {
            jrs.addRowSet(rowSets, matchCols);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        matchCols = new String[2];
        try {
            jrs.addRowSet(rowSets, matchCols);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        rowSets = new RowSet[] { crset, noInitialCrset };
        matchCols = new String[] { "ID", "AUTHORID" };
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                jrs.addRowSet(rowSets, matchCols);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            try {
                jrs.addRowSet(rowSets, matchCols);
                fail("should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            }
        }

        noInitialCrset.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        rowSets = new RowSet[] { crset, noInitialCrset };
        try {
            jrs.addRowSet(rowSets, matchCols);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.close();
    }

    public void testAddJoinableRowSet_WebRowSet_Exception() throws Exception {
        /*
         * Test empty WebRowSet
         */
        WebRowSet webRs = newWebRowSet();
        /*
         * Though WebRowSet implements Joinable, addRowSet(WebRowSet) would
         * throw ClassCastException when run on RI.
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                jrs.addRowSet(webRs);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }

            webRs.setMatchColumn(1);
            try {
                jrs.addRowSet(webRs);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            try {
                jrs.addRowSet(webRs);
                fail("should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            } catch (SQLException e) {
                // Expected
            }

            webRs.setMatchColumn(1);
            jrs.addRowSet(webRs);
        }
        jrs.close();

        /*
         * Test WebRowSet filled with data
         */
        jrs = newJoinRowSet();
        webRs = newWebRowSet();
        webRs.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        webRs.setMatchColumn(1);
        jrs.addRowSet(webRs);
        assertTrue(jrs.next());
        assertEquals(1, jrs.getInt(1));
        jrs.close();
    }

    public void testAddJoinableRowSet_WebRowSet_Int_Exception()
            throws Exception {
        /*
         * Test empty WebRowSet. RI would throw ClassCastException.
         */
        WebRowSet webRs = newWebRowSet();
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                // can't be empty
                jrs.addRowSet(webRs, 1);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            jrs.addRowSet(webRs, 1);
        }
        jrs.close();

        /*
         * Test non-empty WebRowSet.
         */
        jrs = newJoinRowSet();
        webRs = newWebRowSet();
        webRs.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        jrs.addRowSet(webRs, 1);
        assertTrue(jrs.next());
        assertEquals(1, jrs.getInt(1));
        jrs.close();
    }

    public void testAddJoinableRowSet_FilteredRowSet_Exception()
            throws Exception {
        /*
         * Test empty FilteredRowSet
         */
        FilteredRowSet filteredRs = newFilterRowSet();
        assertNull(filteredRs.getMetaData());
        try {
            // not set match column
            jrs.addRowSet(filteredRs);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        filteredRs.setMatchColumn(1);
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                // can't be empty
                jrs.addRowSet(filteredRs);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            jrs.addRowSet(filteredRs);
        }
        jrs.close();

        /*
         * Test FilteredRowSet filled with data, but without Filter
         */
        jrs = newJoinRowSet();
        filteredRs = newFilterRowSet();
        filteredRs.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        filteredRs.setMatchColumn(1);
        jrs.addRowSet(filteredRs);
        jrs.close();

        /*
         * Test FilteredRowSet filled with data and Filter
         */
        jrs = newJoinRowSet();
        Predicate range = new RangeOne();
        filteredRs.setFilter(range);
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            jrs.addRowSet(filteredRs);
        } else {
            try {
                jrs.addRowSet(filteredRs);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        }
        jrs.close();
    }

    public void testAddJoinableRowSet_FilteredRowSet_Int_Exception()
            throws Exception {
        /*
         * Test empty FilteredRowSet
         */
        FilteredRowSet filteredRs = newFilterRowSet();
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                // can't be empty
                jrs.addRowSet(filteredRs, 1);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            jrs.addRowSet(filteredRs, 1);
        }
        jrs.close();

        /*
         * Test FilteredRowSet filled with data, but without Filter
         */
        jrs = newJoinRowSet();
        filteredRs = newFilterRowSet();
        filteredRs.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        jrs.addRowSet(filteredRs, 1);
        jrs.close();

        /*
         * Test FilteredRowSet filled with data and Filter
         */
        jrs = newJoinRowSet();
        filteredRs = newFilterRowSet();
        filteredRs.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        Predicate range = new RangeOne();
        filteredRs.setFilter(range);
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            jrs.addRowSet(filteredRs, 1);
        } else {
            try {
                jrs.addRowSet(filteredRs, 1);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        }
        jrs.close();
    }

    public void testAddJoinableRowSet_JdbcRowSet_Exception() throws Exception {
        /*
         * Test empty JdbcRowSet
         */
        JdbcRowSet jdbcRs = newJdbcRowSet();
        try {
            jrs.addRowSet(jdbcRs);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jdbcRs.setMatchColumn(1);
        try {
            jrs.addRowSet(jdbcRs);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.close();

        /*
         * Test JdbcRowSet filled with data
         */
        jrs = newJoinRowSet();
        jdbcRs = newJdbcRowSet();
        jdbcRs.setUrl(DERBY_URL);
        jdbcRs.setCommand("SELECT * FROM USER_INFO");
        jdbcRs.execute();
        try {
            jrs.addRowSet(jdbcRs);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jdbcRs.setMatchColumn(2);
        jrs.addRowSet(jdbcRs);
        jrs.close();
    }

    public void testAddJoinableRowSet_JdbcRowSet_Int_Exception()
            throws Exception {
        /*
         * Test empty JdbcRowSet
         */
        JdbcRowSet jdbcRs = newJdbcRowSet();
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                jrs.addRowSet(jdbcRs, 1);
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            try {
                jrs.addRowSet(jdbcRs, 1);
                fail("should throw ClassCastException");
            } catch (ClassCastException e) {
                // expected
            }
        }
        jrs.close();

        /*
         * Test JdbcRowSet filled with data
         */
        jrs = newJoinRowSet();
        jdbcRs = newJdbcRowSet();
        jdbcRs.setUrl(DERBY_URL);
        jdbcRs.setCommand("SELECT * FROM USER_INFO");
        jdbcRs.execute();
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            jrs.addRowSet(jdbcRs, 1);
        } else {
            try {
                jrs.addRowSet(jdbcRs, 1);
                fail("should throw ClassCastException");
            } catch (ClassCastException e) {
                // expected
            }
        }
        jrs.close();
    }

    public void testTypeMatch() throws Exception {

        for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
            for (int j = 1; j <= DEFAULT_COLUMN_COUNT; ++j) {
                if ((i == 7 && j == 9) || (i == 9 && j == 7)) {
                    /*
                     * TODO column 7 and column 9 map to same JDBC type, while
                     * ri can't join neither
                     */
                    if ("true".equals(System.getProperty("Testing Harmony"))) {
                        assertTrue(isTypeMatch(i, j));
                    } else {
                        assertFalse(isTypeMatch(i, j));
                    }
                    continue;
                }

                if (i == j) {
                    assertTrue(isTypeMatch(i, j));
                } else {
                    assertFalse(isTypeMatch(i, j));
                }
            }
        }

    }

    private boolean isTypeMatch(int first, int second) throws Exception {
        jrs = newJoinRowSet();
        CachedRowSet base = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        rs.next();
        rs.next();
        rs.next();
        base.populate(rs);

        CachedRowSet join = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        rs.next();
        rs.next();
        rs.next();
        join.populate(rs);

        base.setMatchColumn(first);

        join.setMatchColumn(second);

        jrs.addRowSet(base);

        // TODO if type mismatch, harmony throw SQLException, ri is silent
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                jrs.addRowSet(join);
                return true;
            } catch (SQLException e) {
                return false;
            }
        } else {
            jrs.addRowSet(join);
            return jrs.getMetaData().getColumnName(first) != null;
        }
    }

    public void testGetRowSetNames() throws Exception {
        /*
         * Add two CachedRowSet which don't set Table Name
         */
        assertEquals(0, jrs.getRowSetNames().length);
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals("USER_INFO", crset.getTableName());
            jrs.addRowSet(crset, 1);
            assertEquals(1, jrs.getRowSetNames().length);
            assertEquals("USER_INFO", jrs.getRowSetNames()[0]);
        } else {
            assertNull(crset.getTableName());
            jrs.addRowSet(crset, 1);
            assertEquals(0, jrs.getRowSetNames().length);
            noInitialCrset.populate(st.executeQuery("SELECT * FROM BOOKS"));
            noInitialCrset.setMatchColumn(1);
            jrs.addRowSet(noInitialCrset);
            if ("true".equals(System.getProperty("Testing Harmony"))) {
                try {
                    jrs.getRowSetNames();
                    fail("should throw SQLException");
                } catch (SQLException e) {
                    // expected
                }
            } else {
                try {
                    jrs.getRowSetNames();
                    fail("should throw NullPointerException");
                } catch (NullPointerException e) {
                    // expected
                }
            }
        }

        /*
         * Add two CachedRowSet which both have set a table name
         */
        jrs = newJoinRowSet();
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.populate(st.executeQuery("SELECT * FROM USER_INFO"));
        noInitialCrset.setTableName("USER_INFO");
        assertEquals("USER_INFO", noInitialCrset.getTableName());
        noInitialCrset.setMatchColumn(1);
        jrs.addRowSet(noInitialCrset);
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(1, jrs.getRowSetNames().length);
            assertEquals("USER_INFO", jrs.getRowSetNames()[0]);
        } else {
            // RI still return 0
            assertEquals(0, jrs.getRowSetNames().length);
        }
        // add the second CachedRowSet which also has set a table name
        crset = newNoInitialInstance();
        crset.populate(st.executeQuery("SELECT * FROM BOOKS"));
        crset.setMatchColumn(1);
        crset.setTableName("BOOKS");
        assertEquals("BOOKS", crset.getTableName());
        jrs.addRowSet(crset);
        assertEquals(2, jrs.getRowSetNames().length);
        assertEquals("USER_INFO", jrs.getRowSetNames()[0]);
        assertEquals("BOOKS", jrs.getRowSetNames()[1]);
        // add the third CachedRowSet which doesn't set a table name
        CachedRowSet thirdCrset = newNoInitialInstance();
        thirdCrset.populate(st.executeQuery("SELECT * FROM CUSTOMER_INFO"));
        thirdCrset.setMatchColumn(1);
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            jrs.addRowSet(thirdCrset);
            assertEquals(3, jrs.getRowSetNames().length);
            assertEquals("CUSTOMER_INFO", jrs.getRowSetNames()[2]);
        } else {
            assertNull(thirdCrset.getTableName());
            jrs.addRowSet(thirdCrset);
            try {
                jrs.getRowSetNames();
                fail("should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            }
        }

        /*
         * Add a JdbcRowSet which can't set Table Name
         */
        jrs = newJoinRowSet();
        JdbcRowSet jdbcRs = newJdbcRowSet();
        jdbcRs.setCommand("SELECT * FROM USER_INFO");
        jdbcRs.setUrl(DERBY_URL);
        jdbcRs.execute();
        jdbcRs.setMatchColumn(1);
        jrs.addRowSet(jdbcRs);
        // add a CachedRowSet with table name
        crset = newNoInitialInstance();
        crset.populate(st.executeQuery("SELECT * FROM BOOKS"));
        crset.setMatchColumn(1);
        crset.setTableName("BOOKS");
        jrs.addRowSet(crset);
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(2, jrs.getRowSetNames().length);
            assertEquals("USER_INFO", jrs.getRowSetNames()[0]);
            assertEquals("BOOKS", jrs.getRowSetNames()[1]);
        } else {
            try {
                jrs.getRowSetNames();
                fail("should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            }
        }
    }

    public void testToCachedRowSet_CachedRowSet() throws Exception {
        /*
         * Test empty JoinRowSet
         */
        CachedRowSet emptyToCrset = jrs.toCachedRowSet();
        assertNull(emptyToCrset.getMetaData());
        assertFalse(emptyToCrset.first());

        /*
         * The first CachedRowSet
         */
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        CachedRowSet toCrset = jrs.toCachedRowSet();
        // check metadata
        assertSame(crset.getMetaData(), jrs.getMetaData());
        assertSame(crset.getMetaData(), toCrset.getMetaData());
        assertNotSame(crset, toCrset);
        // check data
        int index = 0;
        toCrset.beforeFirst();
        crset.beforeFirst();
        while (toCrset.next() && crset.next()) {
            for (int i = 1; i <= crset.getMetaData().getColumnCount(); i++) {
                assertEquals(toCrset.getObject(i), crset.getObject(i));
            }
            index++;
        }
        assertEquals(4, index);

        /*
         * The second CachedRowSet
         */
        noInitialCrset.populate(st.executeQuery("SELECT * FROM BOOKS"));
        jrs.addRowSet(noInitialCrset, 1);
        toCrset = jrs.toCachedRowSet();
        // check metadata
        assertSame(jrs.getMetaData(), toCrset.getMetaData());
        /*
         * check data. The data order is not the same between RI and HY.
         */
        index = 0;
        rs = st.executeQuery("SELECT * FROM BOOKS");
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            toCrset.beforeFirst();
            while (toCrset.next() && rs.next()) {
                index++;
                assertEquals(toCrset.getObject(14), rs.getObject(3));
            }
        } else {
            toCrset.afterLast();
            while (toCrset.previous() && rs.next()) {
                index++;
                assertEquals(toCrset.getObject(14), rs.getObject(3));
            }
        }
        assertEquals(6, index);

        /*
         * The third CachedRowSet
         */
        insertDataToCustomerTable();
        CachedRowSet thirdCrset = newNoInitialInstance();
        thirdCrset.populate(st.executeQuery("SELECT * FROM CUSTOMER_INFO"));
        jrs.addRowSet(thirdCrset, 1);
        toCrset = jrs.toCachedRowSet();
        // check metadata
        assertSame(jrs.getMetaData(), toCrset.getMetaData());
        // check data
        toCrset.beforeFirst();
        index = 1;
        while (toCrset.next()) {
            if (index == 1) {
                assertEquals(3, toCrset.getInt(1));
                index++;
            } else if (index == 2) {
                assertEquals(3, toCrset.getInt(1));
                index++;
            } else if (index == 3) {
                assertEquals(4, toCrset.getInt(1));
            } else {
                index = -1;
            }
        }
        assertEquals(3, index);
    }

    public void testToCachedRowSet_acceptChanges() throws Exception {
        /*
         * The first CachedRowSet
         */
        jrs.addRowSet(crset, 1);
        CachedRowSet toCrset = jrs.toCachedRowSet();
        // call crset.acceptChanges()
        assertTrue(crset.absolute(3));
        assertEquals("test3", crset.getString(2));
        crset.updateString(2, "update3");
        crset.updateRow();
        crset.acceptChanges();
        assertTrue(crset.absolute(3));
        assertEquals("update3", crset.getString(2));
        // call toCrset.acceptChanges()
        assertTrue(toCrset.absolute(3));
        assertEquals("test3", toCrset.getString(2));
        assertTrue(toCrset.last());
        assertEquals("test4", toCrset.getString(2));
        toCrset.updateString(2, "update4");
        toCrset.updateRow();
        toCrset.acceptChanges();
        assertTrue(toCrset.absolute(4));
        assertEquals("update4", toCrset.getString(2));

        /*
         * The second CachedRowSet
         */
        noInitialCrset.populate(st.executeQuery("SELECT * FROM BOOKS"));
        jrs.addRowSet(noInitialCrset, 1);
        toCrset = jrs.toCachedRowSet();
        // call toCrset.acceptChanges()
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(toCrset.last());
        } else {
            assertTrue(toCrset.first());
        }
        assertEquals(4, toCrset.getInt(1));
        toCrset.updateString(2, "modify4");
        toCrset.updateRow();
        try {
            toCrset.acceptChanges();
            fail("should throw exception");
        } catch (Exception e) {
            // expected
        }
    }

    public void testCancelRowUpdates_Simple() throws Exception {
        jrs.addRowSet(crset, 1);
        Listener listener = new Listener();
        jrs.addRowSetListener(listener);

        jrs.absolute(4);
        jrs.updateString(2, "Updated");
        jrs.updateRow();
        jrs.absolute(4);
        assertEquals("Updated", jrs.getString(2));

        jrs.cancelRowUpdates();
        jrs.absolute(4);
        assertEquals("test4", jrs.getString(2));
        assertNull(listener.getTag());
    }

    public void testCancelRowUpdates() throws Exception {
        jrs.addRowSet(crset, 1);
        Listener listener = new Listener();
        jrs.addRowSetListener(listener);

        assertTrue(jrs.absolute(3));
        jrs.updateString(2, "update3");
        // call cancelRowUpdates() before updateRow(), no effect here
        listener.clear();
        jrs.cancelRowUpdates();
        assertNull(listener.getTag());
        assertEquals("update3", jrs.getString(2));
        jrs.updateRow();
        jrs.absolute(3);
        assertEquals("update3", jrs.getString(2));

        assertTrue(jrs.absolute(4));
        jrs.updateString(2, "update4");
        assertEquals("update4", jrs.getString(2));
        jrs.updateRow();
        assertEquals("update4", jrs.getString(2));
        // call cancelRowUpdates() after updateRow(), it works here
        listener.clear();
        assertEquals("update4", jrs.getString(2));
        jrs.cancelRowUpdates();
        assertEquals("test4", jrs.getString(2));
        assertNull(listener.getTag());
    }
}
