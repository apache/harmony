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
import java.util.Collection;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.JoinRowSet;

public class JoinRowSetOtherTest extends JoinRowSetTestCase {

    /**
     * @throws SQLException
     * @tests javax.sql.rowset.JoinRowSet.supportsInnerJoin();
     */
    public void testSupportsInnerJoin() throws SQLException {
        boolean isSupported = jrs.supportsInnerJoin();
        assertTrue(isSupported);

        jrs.addRowSet(crset, 1);
        isSupported = jrs.supportsInnerJoin();
        assertTrue(isSupported);
    }

    /**
     * @throws SQLException
     * @tests javax.sql.rowset.JoinRowSet.supportsCrossJoin();
     */
    public void testSupportsCrossJoin() throws SQLException {
        boolean isSupported = jrs.supportsCrossJoin();
        assertFalse(isSupported);

        jrs.addRowSet(crset, 1);
        isSupported = jrs.supportsCrossJoin();
        assertFalse(isSupported);
    }

    /**
     * @throws SQLException
     * @tests javax.sql.rowset.JoinRowSet.supportsLeftOuterJoin();
     */
    public void testSupportsLeftOuterJoin() throws SQLException {
        boolean isSupported = jrs.supportsLeftOuterJoin();
        assertFalse(isSupported);

        jrs.addRowSet(crset, 1);
        isSupported = jrs.supportsLeftOuterJoin();
        assertFalse(isSupported);
    }

    /**
     * @throws SQLException
     * @tests javax.sql.rowset.JoinRowSet.supportsRightOuterJoin();
     */
    public void testSupportsRightOuterJoin() throws SQLException {
        boolean isSupported = jrs.supportsRightOuterJoin();
        assertFalse(isSupported);

        jrs.addRowSet(crset, 1);
        isSupported = jrs.supportsRightOuterJoin();
        assertFalse(isSupported);
    }

    /**
     * @throws SQLException
     * @tests javax.sql.rowset.JoinRowSet#supportsFullJoin();
     */
    public void testSupportsFullJoin() throws SQLException {
        boolean isSupported = jrs.supportsFullJoin();
        assertFalse(isSupported);

        jrs.addRowSet(crset, 1);
        isSupported = jrs.supportsFullJoin();
        assertFalse(isSupported);
    }

    /**
     * @tests java.sql.rowset.joinRowSet#setJoinType(int) and getJoinType();
     * @throws SQLException
     */
    public void testSetJoinTypeAndGetJoinType() throws SQLException {
        int joinType;

        // Tests default join type.
        if (System.getProperty("Testing Harmony") == "true") {
            joinType = jrs.getJoinType();
            assertEquals(JoinRowSet.INNER_JOIN, joinType);
        } else {
            try {
                joinType = jrs.getJoinType();
                fail("Should throw ArrayIndexOutOfBoundsException");
            } catch (ArrayIndexOutOfBoundsException e) {
                // Expected.
            }
        }

        // Adds a rowset, then tests getJoinType().
        jrs.addRowSet(crset, 1);

        if (System.getProperty("Testing Harmony") == "true") {
            joinType = jrs.getJoinType();
            assertEquals(JoinRowSet.INNER_JOIN, joinType);
        } else {
            try {
                joinType = jrs.getJoinType();
                fail("Should throw ArrayIndexOutOfBoundsException");
            } catch (ArrayIndexOutOfBoundsException e) {
                // Expected.
            }
        }

        // Tests setJoinType(CROSS_JOIN)
        try {
            jrs.setJoinType(JoinRowSet.CROSS_JOIN);
            fail("Should throw SQLException according to spec.");
        } catch (SQLException e) {
            // Expected.
        }

        // Tests setJoinType(INNER_JOIN)
        jrs.setJoinType(JoinRowSet.INNER_JOIN);
        joinType = jrs.getJoinType();
        assertEquals(JoinRowSet.INNER_JOIN, joinType);

        // Tests setJoinType(FULL_JOIN)
        try {
            jrs.setJoinType(JoinRowSet.FULL_JOIN);
            fail("Should throw SQLException according to spec.");
        } catch (SQLException e) {
            // Expected.
        }

        // Tests setJoinType(LEFT_OUTER_JOIN)
        try {
            jrs.setJoinType(JoinRowSet.LEFT_OUTER_JOIN);
            fail("Should throw SQLException according to spec.");
        } catch (SQLException e) {
            // Expected.
        }

        // Tests setJoinType(RIGHT_OUTER_JOIN)
        try {
            jrs.setJoinType(JoinRowSet.RIGHT_OUTER_JOIN);
            fail("Should throw SQLException according to spec.");
        } catch (SQLException e) {
            // Expected.
        }

        // Tests setJoinType(-1)
        try {
            jrs.setJoinType(-1);
            fail("Should throw SQLException since -1 is a invalid type");
        } catch (SQLException e) {
            // Expected.
        }
    }

    public void testGetRowSets_Empty() throws Exception {
        Collection rowsets = jrs.getRowSets();
        assertEquals(0, rowsets.size());
    }

    /**
     * @throws SQLException
     * @tests java.sql.rowset.joinRowSet#getRowSets()
     */
    public void testGetRowSets_SingleCachedRowSet() throws Exception {
        crset.setMatchColumn(1);
        jrs.addRowSet(crset);
        CachedRowSet returnRowset = (CachedRowSet) jrs.getRowSets().iterator()
                .next();
        // Since the returnRowset is the same with the original one,
        // so we no need to test FilteredRowSet, JoinRowSet, WebRowSet
        // because they are all subInterface of CachedRowSet.
        assertSame(returnRowset, crset);

        jrs.absolute(4);
        jrs.updateString(2, "Updated 4");
        jrs.updateRow();

        jrs.absolute(4);
        assertEquals("Updated 4", jrs.getString(2));

        // TODO It is Strange. According to spec, the returned rowset should
        // maintain
        // the update occured in the joinrowset.
        int pos = findValue("Updated 4", returnRowset, 1);
        assertEquals(-1, pos);

        assertSame(returnRowset, jrs.getRowSets().iterator().next());
        returnRowset = (CachedRowSet) jrs.getRowSets().iterator().next();
        assertSame(returnRowset, crset);
        pos = findValue("Updated 4", returnRowset, 1);
        assertEquals(-1, pos);

        crset.absolute(3);
        crset.updateString(2, "Updated 3");
        crset.updateRow();
        crset.acceptChanges(conn);

        crset.absolute(3);
        assertEquals("Updated 3", crset.getString(2));
        crset.absolute(4);
        assertEquals("test4", crset.getString(2));

        assertSame(returnRowset, jrs.getRowSets().iterator().next());
        assertSame(returnRowset, crset);
    }

    /**
     * @throws SQLException
     * @tests java.sql.rowset.joinRowSet#getRowSets()
     */
    public void testGetRowSets_SingleJdbcRowSet() throws Exception {
        // Creates jdbc rowset.
        JdbcRowSet jdbcRs;
        jdbcRs = newJdbcRowSet();
        jdbcRs.setCommand("SELECT * FROM USER_INFO");
        jdbcRs.setUrl(DERBY_URL);
        jdbcRs.execute();

        jdbcRs.setMatchColumn(1);
        jrs.addRowSet(jdbcRs);

        CachedRowSet returnRowset = (CachedRowSet) jrs.getRowSets().iterator()
                .next();
        assertNotSame(returnRowset, jrs);

        jrs.absolute(4);
        jrs.updateString(2, "Updated 4");
        jrs.updateRow();

        jrs.absolute(4);
        assertEquals("Updated 4", jrs.getString(2));

        // TODO It is Strange. According to spec, the returned rowset should
        // maintain
        // the update occured in the joinrowset.
        returnRowset.absolute(4);
        assertEquals("test4", returnRowset.getString(2));

        jdbcRs.absolute(3);
        jdbcRs.updateString(2, "Updated 3");
        jdbcRs.updateRow();

        returnRowset.absolute(3);
        assertEquals("test3", returnRowset.getString(2));

        jdbcRs.close();
    }

    /**
     * @throws Exception
     * @throws SQLException
     * @tests java.sql.rowset.joinRowSet#addRowSet()
     */
    public void testAddRowSet_NoInitialCachedRowSet() throws Exception {
        CachedRowSet crs = newNoInitialInstance();

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                jrs.addRowSet(crs, 1);
                fail("Should throw SQLException in harmony.");
            } catch (SQLException e) {
                // Expected.
            }
        } else {
            try {
                jrs.addRowSet(crs, 1);
                fail("should throw NullPointerException");
            } catch (NullPointerException e) {
                // Expected.
            }
        }
    }

    public void testGetRowSets_MultipleCachedRowSet() throws Exception {
        Collection collection;

        rs = st.executeQuery("select * from USER_INFO");
        CachedRowSet crset2 = newNoInitialInstance();
        crset2.populate(rs);

        jrs.addRowSet(crset, 1);
        jrs.addRowSet(crset2, 1);

        collection = jrs.getRowSets();
        assertEquals(2, collection.size());
        assertTrue(collection.contains(crset2));
        assertTrue(collection.contains(crset));

        crset.absolute(1);
        crset.updateString(2, "Updated");
        crset.updateRow();
        int pos = findValue("Updated", jrs, 2);
        assertEquals(-1, pos);
    }

    /**
     * @throws Exception
     * @tests java.sql.rowset.joinRowSet#getWhereClause()
     */
    public void testGetWhereClause_Empty() throws Exception {
        String whereClause;

        if (System.getProperty("Testing Harmony") == "true") {
            whereClause = jrs.getWhereClause();
            assertEquals("", whereClause);
        } else {
            try {
                whereClause = jrs.getWhereClause();
                fail("Should throw StringIndexOutOfBoundsException.");
            } catch (StringIndexOutOfBoundsException e) {
                // Expected.
            }
        }
    }

    /**
     * @throws Exception
     * @tests java.sql.rowset.joinRowSet#getWhereClause()
     */
    public void testGetWhereClause_SingleCachedRowSet() throws Exception {
        String whereClause;

        jrs.addRowSet(crset, 1);
        if (System.getProperty("Testing Harmony") == "true") {

            whereClause = jrs.getWhereClause();
            assertNotNull(whereClause);
        }

        else {
            try {
                whereClause = jrs.getWhereClause();
                fail("Should throw NullPointerException.");
            } catch (NullPointerException e) {
                // Expected.
            }
        }

        crset.setTableName("table1");

        if (System.getProperty("Testing Harmony") == "true") {
            whereClause = jrs.getWhereClause();
            assertNotNull(whereClause);
        } else {
            try {
                whereClause = jrs.getWhereClause();
                fail("Should throw SQLException.");
            } catch (SQLException e) {
                // Expected.
            }
        }

        crset.setMatchColumn("ID");

        whereClause = jrs.getWhereClause();
        assertNotNull(whereClause);
    }

    /**
     * @throws Exception
     * @tests java.sql.rowset.joinRowSet#getWhereClause()
     */
    public void testGetWhereClause_SingleJdbcRowSet() throws Exception {
        String whereClause;

        // Creates jdbc rowset.
        JdbcRowSet jdbcRs;
        jdbcRs = newJdbcRowSet();
        jdbcRs.setCommand("SELECT * FROM USER_INFO");
        jdbcRs.setUrl(DERBY_URL);
        jdbcRs.execute();

        jdbcRs.setMatchColumn("ID");
        jrs.addRowSet(jdbcRs);

        if (System.getProperty("Testing Harmony") == "true") {
            whereClause = jrs.getWhereClause();
            assertNotNull(whereClause);
        } else {
            try {
                whereClause = jrs.getWhereClause();
                fail("Should throw NullPointerException.");
            } catch (NullPointerException e) {
                // Expected.
            }
        }

        CachedRowSet rowSetInJrs = (CachedRowSet) jrs.getRowSets().iterator()
                .next();
        rowSetInJrs.setTableName("Table1");

        whereClause = jrs.getWhereClause();
        assertNotNull(whereClause);
    }

    /**
     * @throws Exception
     * @tests java.sql.rowset.joinRowSet#getWhereClause()
     */
    public void testGetWhereClause_MultipleCachedRowSet() throws Exception {
        String whereClause;

        // Creates another cached rowset.
        CachedRowSet crset2;
        crset2 = newNoInitialInstance();
        crset2.setCommand("SELECT * FROM BOOKS");
        crset2.setUrl(DERBY_URL);
        crset2.execute();

        // Tests when there are on jdbcRowSet and one CachedRowSet.
        jrs.addRowSet(crset2, 1);
        jrs.addRowSet(crset, 1);

        if (System.getProperty("Testing Harmony") == "true") {
            whereClause = jrs.getWhereClause();
            assertNotNull(whereClause);
        } else {
            try {
                whereClause = jrs.getWhereClause();
                fail("Should throw NullPointerException.");
            } catch (NullPointerException e) {
                // Expected.
            }
        }
        crset.setTableName("Table1");
        crset2.setTableName("Table2");
        if (System.getProperty("Testing Harmony") == "true") {
            whereClause = jrs.getWhereClause();
            assertNotNull(whereClause);
        } else {
            try {
                whereClause = jrs.getWhereClause();
                fail("Should throw SQLException.");
            } catch (SQLException e) {
                // Expected.
            }
        }

        crset.setMatchColumn("ID");
        crset2.setMatchColumn("AUTHORID");
        whereClause = jrs.getWhereClause();
        assertNotNull(whereClause);

        jrs = newJoinRowSet();
        jrs.addRowSet(crset2, "AUTHORID");
        jrs.addRowSet(crset, "ID");
        whereClause = jrs.getWhereClause();
        assertNotNull(whereClause);

        crset2.close();
    }

    /**
     * @throws Exception
     * @tests java.sql.rowset.joinRowSet#getWhereClause()
     */
    public void testGetWhereClause_MoreRowSets() throws Exception {
        crset.setTableName("Table1");
        jrs.addRowSet(crset, "ID");

        // Creates another cached rowset.
        CachedRowSet crset2;
        crset2 = newNoInitialInstance();
        crset2.setCommand("SELECT * FROM BOOKS");
        crset2.setUrl(DERBY_URL);
        crset2.execute();
        crset2.setTableName("Table2");
        jrs.addRowSet(crset2, "AUTHORID");

        crset2 = newNoInitialInstance();
        crset2.setCommand("SELECT * FROM BOOKS");
        crset2.setUrl(DERBY_URL);
        crset2.execute();
        crset2.setTableName("Table3");
        jrs.addRowSet(crset2, "AUTHORID");

        String whereClause = jrs.getWhereClause();
        assertNotNull(whereClause);
    }
}
