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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import javax.sql.RowSet;
import javax.sql.rowset.BaseRowSet;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetWarning;

public class JdbcRowSetTest extends CachedRowSetTestCase {

    private JdbcRowSet newJdbcRowSet() throws Exception {
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            return (JdbcRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.JdbcRowSetImpl")
                    .newInstance();
        }
        return (JdbcRowSet) Class.forName("com.sun.rowset.JdbcRowSetImpl")
                .newInstance();
    }

    public void testBeforeInitial() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();

        // move cursor
        try {
            jrs.absolute(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        try {
            jrs.beforeFirst();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        try {
            jrs.afterLast();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }
        try {
            jrs.last();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }
        try {
            jrs.first();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }
        try {
            jrs.isAfterLast();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }
        try {
            jrs.isBeforeFirst();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }
        try {
            jrs.isFirst();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }
        try {
            jrs.isLast();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        try {
            jrs.getRow();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        try {
            jrs.getString(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        // get properties

        try {
            jrs.getConcurrency();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            jrs.getFetchDirection();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        assertEquals(0, jrs.getFetchSize());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, jrs.getType());
        assertNull(jrs.getTypeMap());
        assertTrue(jrs.getEscapeProcessing());

        // set properties
        jrs.setConcurrency(RowSet.CONCUR_UPDATABLE);
        jrs.setType(ResultSet.TYPE_FORWARD_ONLY);
        jrs.setEscapeProcessing(false);
        assertFalse(jrs.getEscapeProcessing());

        // JdbcRowSet methods
        try {
            jrs.getAutoCommit();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            jrs.commit();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            jrs.setAutoCommit(false);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            jrs.rollback();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        assertNull(jrs.getRowSetWarnings());

        // update methods
        try {
            jrs.updateString(1, "test");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        try {
            jrs.insertRow();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        jrs.close();

        try {
            jrs.clearWarnings();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        try {
            jrs.findColumn("ID");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        try {
            jrs.wasNull();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        try {
            jrs.getMetaData();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }

        try {
            jrs.getWarnings();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid state
        }
    }

    public void testResult() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        assertTrue(jrs instanceof BaseRowSet);
        jrs.setCommand("select * from USER_INFO");
        jrs.setUrl(DERBY_URL);

        // before execute
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, jrs.getType());
        assertTrue(jrs.getEscapeProcessing());

        assertEquals(Connection.TRANSACTION_READ_COMMITTED, jrs
                .getTransactionIsolation());

        try {
            assertEquals(ResultSet.FETCH_FORWARD, jrs.getFetchDirection());
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        assertEquals(0, jrs.getFetchSize());

        assertEquals(0, jrs.getMaxFieldSize());
        assertEquals(0, jrs.getMaxRows());

        assertEquals(0, jrs.getQueryTimeout());
        assertFalse(jrs.getShowDeleted());

        assertEquals(DERBY_URL, jrs.getUrl());
        assertNull(jrs.getUsername());
        assertNull(jrs.getPassword());

        jrs.execute();

        // after execute
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, jrs.getType());
        assertTrue(jrs.getEscapeProcessing());

        assertEquals(Connection.TRANSACTION_READ_COMMITTED, jrs
                .getTransactionIsolation());

        assertEquals(ResultSet.FETCH_FORWARD, jrs.getFetchDirection());
        assertEquals(0, jrs.getFetchSize());

        assertEquals(0, jrs.getMaxFieldSize());
        assertEquals(0, jrs.getMaxRows());

        assertEquals(0, jrs.getQueryTimeout());
        assertFalse(jrs.getShowDeleted());

        assertEquals(DERBY_URL, jrs.getUrl());
        assertNull(jrs.getUsername());
        assertNull(jrs.getPassword());

        assertTrue(jrs.next());
        assertEquals(1, jrs.getInt(1));

        assertTrue(jrs.absolute(3));

        assertTrue(jrs.absolute(1));

        assertEquals(ResultSet.TYPE_FORWARD_ONLY, rs.getType());

        assertTrue(jrs instanceof BaseRowSet);

        jrs.close();
    }

    public void testCursorMove() throws Exception {
        insertMoreData(6);
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setUrl(DERBY_URL);
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.execute();

        jrs.beforeFirst();
        assertTrue(jrs.isBeforeFirst());
        assertTrue(jrs.next());
        assertTrue(jrs.first());
        assertFalse(jrs.previous());
        assertTrue(jrs.isBeforeFirst());
        assertTrue(jrs.absolute(1));
        assertTrue(jrs.first());
        assertEquals(1, jrs.getInt(1));

        assertTrue(jrs.relative(3));
        assertEquals(4, jrs.getInt(1));
        assertTrue(jrs.previous());
        assertEquals(3, jrs.getInt(1));
        assertTrue(jrs.relative(-2));
        assertTrue(jrs.first());

        assertTrue(jrs.absolute(10));
        assertEquals(10, jrs.getInt(1));
        assertTrue(jrs.last());
        assertFalse(jrs.next());
        assertTrue(jrs.isAfterLast());

        assertTrue(jrs.relative(-2));
        assertEquals(9, jrs.getInt(1));
        jrs.afterLast();
        assertTrue(jrs.isAfterLast());

        assertTrue(jrs.absolute(-3));
        assertEquals(8, jrs.getInt(1));

        jrs.close();
    }

    public void testExecute() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setUrl("bad url");

        try {
            jrs.execute();
            fail("Should SQLException");
        } catch (SQLException e) {
            // expected, No suitable driver
        }

        jrs = newJdbcRowSet();
        try {
            jrs.execute();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        jrs.setUrl(DERBY_URL);
        try {
            jrs.execute();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        /*
         * TODO It's really strange. setUrl(null) no affect here.
         */
        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            jrs.setUrl(null);
            assertEquals(DERBY_URL, jrs.getUrl());
        }

        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.execute();
        assertTrue(jrs.last());
        assertEquals(4, jrs.getInt(1));
        jrs.close();

        /*
         * recall execute() after close()
         */
        assertEquals("SELECT * FROM USER_INFO", jrs.getCommand());
        assertEquals(DERBY_URL, jrs.getUrl());
        try {
            jrs.execute();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected, No current connection.
        }

        jrs.clearParameters();
        assertEquals("SELECT * FROM USER_INFO", jrs.getCommand());
        assertEquals(DERBY_URL, jrs.getUrl());
        try {
            jrs.execute();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        jrs = newJdbcRowSet();
        jrs.setUrl(DERBY_URL);
        jrs.setCommand("SELECT * FROM USER_INFO WHERE ID >= ?");
        jrs.setInt(1, 2);
        jrs.execute();
        assertTrue(jrs.first());
        assertEquals(2, jrs.getInt(1));

        // change the query condition
        jrs.setInt(1, 3);
        jrs.execute();
        assertTrue(jrs.first());
        assertEquals(3, jrs.getInt(1));

        // change the command
        jrs.setCommand("SELECT * FROM USER_INFO WHERE NAME = 'hermit'");
        jrs.execute();
        assertTrue(jrs.first());
        assertEquals(1, jrs.getInt(1));
        assertFalse(jrs.next());

        jrs.close();
    }

    public void testExecute_SelectCmd() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO WHERE ID > ?");
        jrs.setUrl(DERBY_URL);
        jrs.setInt(1, 1);
        jrs.execute();

        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, jrs.getType());
        assertEquals(ResultSet.CONCUR_UPDATABLE, jrs.getConcurrency());
        assertTrue(jrs.getAutoCommit());
        assertTrue(jrs.first());
        assertEquals(2, jrs.getInt(1));
        assertTrue(jrs.absolute(2));
        assertEquals(3, jrs.getInt(1));
        assertTrue(jrs.last());
        assertEquals(4, jrs.getInt(1));
        assertTrue(jrs.previous());
        assertEquals(3, jrs.getInt(1));
        assertTrue(jrs.relative(-1));
        assertEquals(2, jrs.getInt(1));
        jrs.close();
    }

    public void testExecute_UpdateCmd() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("Update User_INFO set Name= ? Where ID= ? ");
        jrs.setUrl(DERBY_URL);
        jrs.setString(1, "update");
        jrs.setInt(2, 3);
        try {
            jrs.execute();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.close();
    }

    public void testExecute_DeleteCmd() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("DELETE FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        try {
            jrs.execute();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.close();
    }

    public void testExecute_InsertCmd() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("insert into USER_INFO(ID,NAME) values (6,'insert6')");
        jrs.setUrl(DERBY_URL);
        try {
            jrs.execute();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.close();
    }

    public void testUpdateRow_Normal() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        assertTrue(jrs.absolute(3));
        assertEquals(3, jrs.getInt(1));
        assertEquals("test3", jrs.getString(2));
        jrs.updateString(2, "update3");
        assertFalse(jrs.rowUpdated());
        jrs.updateRow();
        assertTrue(jrs.rowUpdated());
        assertEquals("update3", jrs.getString(2));

        jrs.close();

        // check db
        rs = st
                .executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE NAME = 'update3'");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
    }

    public void testUpdateRow_Exception() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        assertTrue(jrs.absolute(-2));
        assertEquals("test3", jrs.getString(2));
        jrs.updateRow();
        assertFalse(jrs.rowUpdated());

        jrs.updateString(2, "too looooooooooooooooooooong");
        try {
            jrs.updateRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            assertFalse(jrs.rowUpdated());
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            assertTrue(jrs.absolute(1));
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        jrs.close();
    }

    public void testDeleteRow() throws Exception {
        insertMoreData(6);
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        assertTrue(jrs.first());
        jrs.deleteRow();

        try {
            jrs.getInt(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        jrs.close();

        /*
         * Check database
         */
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 1");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
    }

    public void testResultSet_InsertRow() throws Exception {
        Statement stmt = conn.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        ResultSet rset = stmt.executeQuery("SELECT * FROM USER_INFO");
        assertTrue(rset.first());
        assertEquals(1, rset.getInt(1));
        rset.moveToInsertRow();
        rset.updateInt(1, 5);
        rset.updateString(2, "insert5");
        rset.insertRow();
        rset.moveToCurrentRow();
        assertEquals(1, rset.getInt(1));
        assertTrue(rset.last());
        assertEquals(5, rset.getInt(1));

        /*
         * Because the above called rset.last(), the inserted row which ID is 6
         * becomes invisible to JdbcRowSet.
         */
        rset.moveToInsertRow();
        rset.updateInt(1, 6);
        rset.updateString(2, "insert6");
        rset.insertRow();
        rset.moveToCurrentRow();

        int index = 0;
        rset.beforeFirst();
        while (rset.next()) {
            index++;
            assertEquals(index, rset.getInt(1));
        }
        assertEquals(5, index);

        rset.close();
        stmt.close();

        /*
         * Check database
         */
        rs = st
                .executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE NAME = 'insert5' OR NAME = 'insert6'");
        assertTrue(rs.next());
        assertEquals(2, rs.getInt(1));
    }

    public void testInsertRow_Single() throws Exception {
        /*
         * In JdbcRowSet, the inserted row is the last row. The behavior is the
         * same as ResultSet.
         */
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        jrs.moveToInsertRow();
        jrs.updateInt(1, 5);
        jrs.updateString(2, "insert5");
        jrs.insertRow();
        jrs.moveToCurrentRow();

        // check the inserted row
        assertTrue(jrs.absolute(5));
        /*
         * If uncommenting the following line, then the inserted row which ID is
         * 6 would be invisible to JdbcRowSet.
         */
        // assertTrue(jrs.isLast());
        assertEquals(5, jrs.getInt(1));

        jrs.moveToInsertRow();
        jrs.updateInt(1, 6);
        jrs.updateString(2, "insert6");
        jrs.insertRow();
        jrs.moveToCurrentRow();

        assertTrue(jrs.last());
        assertEquals(6, jrs.getInt(1));
        assertTrue(jrs.previous());
        assertEquals(5, jrs.getInt(1));

        jrs.close();

        // check db
        rs = st
                .executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE NAME = 'insert5' OR NAME = 'insert6'");
        assertTrue(rs.next());
        assertEquals(2, rs.getInt(1));
    }

    public void testInsertRow_Multi() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        jrs.moveToInsertRow();
        jrs.updateInt(1, 5);
        jrs.updateString(2, "insert5");
        jrs.insertRow();
        jrs.updateInt(1, 6);
        jrs.updateString(2, "insert6");
        jrs.insertRow();
        jrs.updateInt(1, 7);
        jrs.updateString(2, "insert7");
        jrs.insertRow();
        jrs.moveToCurrentRow();

        int index = 0;
        jrs.beforeFirst();
        while (jrs.next()) {
            index++;
            assertEquals(index, jrs.getInt(1));
        }
        assertEquals(7, index);

        jrs.close();

        /*
         * Check database
         */
        rs = st
                .executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE NAME LIKE 'insert%'");
        assertTrue(rs.next());
        assertEquals(3, rs.getInt(1));

        /*
         * Change another way to insert multiple rows
         */
        jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        assertTrue(jrs.absolute(7));
        jrs.moveToInsertRow();
        jrs.updateInt(1, 8);
        jrs.updateString(2, "insert8");
        jrs.insertRow();
        jrs.moveToInsertRow();
        jrs.updateInt(1, 9);
        jrs.updateString(2, "insert9");
        jrs.insertRow();
        jrs.moveToInsertRow();
        jrs.updateInt(1, 10);
        jrs.updateString(2, "insert10");
        jrs.insertRow();
        jrs.moveToCurrentRow();

        /*
         * Check JdbcRowSet
         */
        assertEquals(7, jrs.getInt(1));
        index = 7;
        while (jrs.next()) {
            index++;
            assertEquals(index, jrs.getInt(1));
        }
        assertEquals(10, index);

        jrs.close();

        /*
         * Check database
         */
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID > 7");
        assertTrue(rs.next());
        assertEquals(3, rs.getInt(1));
    }

    public void testCancelRowUpdates() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        jrs.moveToInsertRow();
        try {
            jrs.cancelRowUpdates();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.moveToCurrentRow();

        assertTrue(jrs.absolute(3));
        jrs.updateString(2, "update3");
        jrs.updateRow();
        assertTrue(jrs.rowUpdated());
        assertEquals("update3", jrs.getString(2));
        jrs.cancelRowUpdates();
        assertEquals("update3", jrs.getString(2));

        assertTrue(jrs.next());
        assertEquals(4, jrs.getInt(1));
        jrs.updateString(2, "update4");
        assertFalse(jrs.rowUpdated());
        assertEquals("update4", jrs.getString(2));
        jrs.cancelRowUpdates();
        assertEquals("test4", jrs.getString(2));

        jrs.close();

        /*
         * Check database
         */
        rs = st.executeQuery("SELECT * FROM USER_INFO WHERE ID = 3");
        assertTrue(rs.next());
        assertEquals("update3", rs.getString(2));
    }

    public void testFindColumn() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        try {
            jrs.findColumn("abc");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        assertEquals(1, jrs.findColumn("ID"));
        try {
            jrs.findColumn("abc");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        jrs.close();
    }

    public void testGetStatement() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        assertNull(jrs.getStatement());
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();
        assertTrue(jrs.getStatement() instanceof PreparedStatement);

        jrs.close();

        try {
            assertNull(jrs.getStatement());
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    public void testAutoCommit_True() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        assertTrue(jrs.getAutoCommit());
        assertTrue(jrs.absolute(3));
        jrs.updateString(2, "update3");
        jrs.updateRow();
        jrs.rollback();

        // after rollback, resultset is closed
        assertNull(jrs.getStatement());
        try {
            jrs.getString(2);
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            jrs.absolute(1);
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        jrs.close();

        /*
         * TODO Check database. The database is supposed to has been updated.
         * However, it's not.
         */
        rs = st
                .executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE NAME = 'update3'");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
    }

    public void testAutoCommit_False() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        jrs.setAutoCommit(false);
        assertFalse(jrs.getAutoCommit());
        assertTrue(jrs.absolute(3));
        jrs.updateString(2, "update3");
        jrs.updateRow();
        jrs.commit();
        jrs.rollback();

        /*
         * TODO why throw NullPointerException after call rollback()?
         */
        try {
            jrs.absolute(1);
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        jrs.close();

        /*
         * Check database
         */
        rs = st
                .executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE NAME = 'update3'");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
    }

    public void testGetRowSetWarnings() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();

        /*
         * TODO not sure when and how will cause RowSetWarning
         */
        assertNull(jrs.getRowSetWarnings());

        jrs.close();

        jrs = newJdbcRowSet();
        RowSetWarning warning = jrs.getRowSetWarnings();
        super.assertNull(warning);

        // TODO Try to produce warnings.

        jrs.close();
        // Checks for Exception after closing jrs.

        assertNull(rs.getWarnings());
        assertNull(jrs.getRowSetWarnings());
    }

    public void testConstructor() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        try {
            jrs.absolute(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.addRowSetListener(new Listener());
        try {
            jrs.afterLast();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.beforeFirst();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.cancelRowUpdates();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.clearParameters();
        try {
            jrs.clearWarnings();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.close();

        // after close
        try {
            jrs.commit();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            jrs.deleteRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.findColumn("abc");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.first();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.getString(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.getAutoCommit();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        jrs.getCommand();
        try {
            jrs.getConcurrency();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            jrs.getCursorName();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.getDataSourceName();
        jrs.getEscapeProcessing();
        try {
            jrs.getFetchDirection();
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
        jrs.getFetchSize();
        try {
            jrs.getMatchColumnIndexes();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.getMatchColumnNames();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.getMaxFieldSize();
        jrs.getMaxRows();
        try {
            jrs.getMetaData();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.getPassword();
        jrs.getQueryTimeout();
        try {
            jrs.getRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.getRowSetWarnings();
        jrs.getShowDeleted();
        jrs.getStatement();
        jrs.getTransactionIsolation();
        jrs.getType();
        jrs.getTypeMap();
        jrs.getUrl();
        jrs.getUsername();
        try {
            jrs.getWarnings();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.insertRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.isAfterLast();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.isFirst();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        jrs.isReadOnly();
        try {
            jrs.moveToCurrentRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.moveToInsertRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            jrs.next();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    public void testAfterClose() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.setUrl(DERBY_URL);
        jrs.execute();
        jrs.close();

        try {
            jrs.absolute(1);
            fail("Should throw NullPointerException");
        } catch (SQLException e) {
            // expected
        }

        try {
            jrs.getString(1);
            fail("Should throw NullPointerException");
        } catch (SQLException e) {
            // expected
        }

        try {
            jrs.updateString(1, "hello");
            fail("Should throw NullPointerException");
        } catch (SQLException e) {
            // expected
        }

        assertEquals("SELECT * FROM USER_INFO", jrs.getCommand());

        try {
            jrs.getConcurrency();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            jrs.getFetchDirection();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

    }

    /**
     * @test javax.sql.rowset.JdbcRowTest.commit()
     */
    public void testCommit() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setUrl(DERBY_URL);
        jrs.setCommand("SELECT * FROM USER_INFO");

        try {
            jrs.commit();
            fail("Shuld throw NullPointerException since jrs has not been executed.");
        } catch (NullPointerException e) {
            // Expected.
        }

        jrs.execute();
        assertTrue(jrs.absolute(4));
        assertEquals(4, jrs.getInt(1));

        // Inserts a new row.
        jrs.moveToInsertRow();
        jrs.updateInt(1, 5);
        jrs.updateString(2, "test5");
        jrs.insertRow();
        jrs.moveToCurrentRow();

        // Tests if jrs has the same behaviour with connection when commit in
        // auto-commit mode.
        boolean hasCommitException = true;

        try {
            conn.commit();
            hasCommitException = false;
        } catch (SQLException e) {
            // Nothing to do.
        }

        try {
            assertTrue(jrs.getAutoCommit());
            jrs.setAutoCommit(true);
            jrs.commit();
            // The behaviour of jrs should be same with the connection.

            if (hasCommitException) {
                fail("Should throw SQLException");
            }
        } catch (SQLException e) {
            // Expected.
        }

        assertTrue(jrs.absolute(5));
        assertEquals(5, jrs.getInt(1));

        // Set autoCommit to false.
        jrs.setAutoCommit(false);

        // Inserts a row.
        jrs.moveToInsertRow();
        jrs.updateInt(1, 6);
        jrs.updateString(2, "test6");
        jrs.insertRow();
        jrs.moveToCurrentRow();
        assertTrue(jrs.absolute(6));
        assertEquals(6, jrs.getInt(1));

        // Commits
        jrs.commit();
        assertTrue(jrs.absolute(6));
        assertEquals(6, jrs.getInt(1));
        jrs.commit();
        jrs.close();
    }

    /**
     * @test javax.sql.rowset.JdbcRowTest.rollback()
     */
    public void testRollback() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setUrl(DERBY_URL);
        jrs.setCommand("SELECT * FROM USER_INFO");

        try {
            jrs.rollback();
            fail("Shuld throw NullPointerException since jrs has not been executed.");
        } catch (NullPointerException e) {
            // Expected.
        }

        jrs.execute();
        assertTrue(jrs.absolute(4));
        assertEquals(4, jrs.getInt(1));

        // Inserts a new row.
        jrs.moveToInsertRow();
        jrs.updateInt(1, 5);
        jrs.updateString(2, "test5");
        jrs.insertRow();
        jrs.moveToCurrentRow();

        assertTrue(jrs.absolute(5));
        assertEquals(5, jrs.getInt(1));

        // Set autoCommit to false.
        jrs.setAutoCommit(false);

        // Inserts a row.
        jrs.moveToInsertRow();
        jrs.updateInt(1, 6);
        jrs.updateString(2, "test6");
        jrs.insertRow();
        jrs.moveToCurrentRow();
        assertTrue(jrs.absolute(6));
        assertEquals(6, jrs.getInt(1));

        // Rollbacks
        jrs.rollback();
        try {
            jrs.first();
            fail("After rolling back, jrs can't do anything except close.");
        } catch (NullPointerException e) {
            // Expected.
        }

        jrs.close();

        // Reconnect
        jrs = newJdbcRowSet();
        jrs.setUrl(DERBY_URL);
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.execute();
        assertTrue(jrs.last());
        assertEquals(
                "Since the insert action was roll back, so the last row is 5, not 6.",
                5, jrs.getInt(1));

        jrs.close();

    }

    /**
     * @test javax.sql.rowset.JdbcRowTest.rollback() When in auto-commit mode,
     *       the behaviour should be the same with the connection.
     */
    public void testRollbackException() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setUrl(DERBY_URL);
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.execute();

        // Tests if jrs has the same behaviour with connection when commit in
        // auto-commit mode.
        boolean hasRollbackException = true;

        try {
            conn.rollback();
            hasRollbackException = false;
        } catch (SQLException e) {
            // Nothing to do.
        }

        try {
            assertTrue(jrs.getAutoCommit());
            jrs.setAutoCommit(true);
            jrs.rollback();
            // The behaviour of jrs should be same with the connection.

            if (hasRollbackException) {
                fail("Should throw SQLException");
            }
        } catch (SQLException e) {
            // Expected.
        }

        jrs.close();
    }

    /**
     * @tests javax.sql.rowset.JdbcRowTest.rollback(SavePoint)
     */
    public void testRollback_LSavePoint_Exception() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        jrs.setUrl(DERBY_URL);
        jrs.setCommand("SELECT * FROM USER_INFO");

        try {
            jrs.rollback(null);
            fail("Shuld throw NullPointerException since jrs has not been executed.");
        } catch (NullPointerException e) {
            // Expected.
        }
        Savepoint savepoint = new MockSavepoint(1, "mock savepoint 1");
        try {
            jrs.rollback(savepoint);
            fail("Shuld throw NullPointerException since jrs has not been executed.");
        } catch (NullPointerException e) {
            // Expected.
        }

        jrs.execute();

        try {
            jrs.rollback(null);
            fail("Should throw SQLException since autoCommit is on");
        } catch (SQLException e) {
            // Expected
        }
        try {
            jrs.rollback(savepoint);
            fail("Should throw SQLException since autoCommit is on");
        } catch (SQLException e) {
            // Expected
        }

        jrs.setAutoCommit(false);
        try {
            jrs.rollback(savepoint);
            fail("Should throw ClassCastException "
                    + "since the savepoint is not the correct type.");
        } catch (ClassCastException e) {
            // Expected.
        }

        jrs.close();
    }

    /**
     * @tests javax.sql.rowset.JdbcRowTest.setShowDeleted() and
     *        javax.sql.rowset.JdbcRowTest.getShowDeleted().
     */
    public void testShowDeleted() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();

        boolean showDeleted = jrs.getShowDeleted();
        assertFalse("The default value is false.", showDeleted);

        jrs.setUrl(DERBY_URL);
        jrs.setCommand("SELECT * FROM USER_INFO");
        jrs.execute();

        // Deletes the 4th row.
        jrs.absolute(4);
        jrs.deleteRow();
        try {
            jrs.getInt(1);
            fail("Should throw SQLException " + "since no current row.");
        } catch (SQLException e) {
            // Expected.
        }

        jrs.first();
        int index = 0;
        while (jrs.next()) {
            index++;
        }

        assertEquals(3, index);

        jrs.setShowDeleted(true);
        assertTrue(jrs.getShowDeleted());

        jrs.absolute(3);
        jrs.deleteRow();
        jrs.first();
        index = 0;
        while (jrs.next()) {
            index++;
        }

        assertEquals("The deleted row should still be showed", 3, index);

        jrs.close();
    }

    public class MockSavepoint implements Savepoint {
        private int id;

        private String name;

        public MockSavepoint(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getSavepointId() throws SQLException {
            return id;
        }

        public String getSavepointName() throws SQLException {
            return name;
        }

    }
}
