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
import java.sql.ResultSetMetaData;
import java.util.Arrays;
import java.util.HashMap;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.spi.SyncProviderException;

public class CachedRowSetTransactionTest extends CachedRowSetTestCase {

    public void testRelease() throws Exception {
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.release();

        crset.setCommand("testCommand");
        assertEquals(DERBY_URL, crset.getUrl());
        crset.setDataSourceName("testDataSourceName");
        assertNull(crset.getUrl());
        crset.setFetchDirection(ResultSet.FETCH_REVERSE);
        crset.setMaxFieldSize(100);
        crset.setMaxRows(10);
        crset.setPageSize(10);
        crset.setPassword("passwo");
        crset.setQueryTimeout(100);
        crset.setTableName("testTable");
        crset.setTransactionIsolation(ResultSet.HOLD_CURSORS_OVER_COMMIT);
        crset.setType(ResultSet.TYPE_SCROLL_SENSITIVE);
        HashMap<String, Class<?>> map = new HashMap<String, Class<?>>();
        crset.setTypeMap(map);
        crset.setUsername("testUserName");
        crset.setEscapeProcessing(false);
        crset.setKeyColumns(new int[] { 1 });

        assertTrue(crset.absolute(1));
        crset.updateString(2, "update2");

        crset.release();

        assertEquals("testCommand", crset.getCommand());
        assertEquals("testDataSourceName", crset.getDataSourceName());
        assertEquals(ResultSet.FETCH_REVERSE, crset.getFetchDirection());
        assertEquals(100, crset.getMaxFieldSize());
        assertEquals(10, crset.getMaxRows());
        assertEquals(10, crset.getPageSize());
        assertEquals("passwo", crset.getPassword());
        assertEquals(100, crset.getQueryTimeout());
        assertEquals("testTable", crset.getTableName());
        assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, crset
                .getTransactionIsolation());
        assertEquals(ResultSet.TYPE_SCROLL_SENSITIVE, crset.getType());
        assertEquals(map, crset.getTypeMap());
        assertEquals("testUserName", crset.getUsername());
        assertFalse(crset.getEscapeProcessing());
        assertTrue(Arrays.equals(new int[] { 1 }, crset.getKeyColumns()));

        assertFalse(crset.next());
        assertEquals(0, crset.size());

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset.populate(rs);

        assertTrue(crset.first());
        int index = 1;
        while (crset.next()) {
            index++;
            assertEquals(index, crset.getInt(1));
        }
        assertEquals(4, index);
    }

    public void testClose() throws Exception {
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.close();

        crset.setCommand("testCommand");
        assertEquals(DERBY_URL, crset.getUrl());
        crset.setDataSourceName("testDataSourceName");
        assertNull(crset.getUrl());
        crset.setFetchDirection(ResultSet.FETCH_REVERSE);
        crset.setMaxFieldSize(100);
        crset.setMaxRows(10);
        crset.setPageSize(10);
        crset.setPassword("passwo");
        crset.setQueryTimeout(100);
        crset.setTableName("testTable");
        crset.setTransactionIsolation(ResultSet.HOLD_CURSORS_OVER_COMMIT);
        crset.setType(ResultSet.TYPE_SCROLL_SENSITIVE);
        HashMap<String, Class<?>> map = new HashMap<String, Class<?>>();
        crset.setTypeMap(map);
        crset.setUsername("testUserName");
        crset.setEscapeProcessing(false);
        crset.setKeyColumns(new int[] { 1 });
        ResultSetMetaData rsmd = crset.getMetaData();

        crset.close();

        assertEquals(rsmd, crset.getMetaData());
        assertEquals("testCommand", crset.getCommand());
        assertEquals(ResultSet.CONCUR_UPDATABLE, crset.getConcurrency());
        assertEquals("testDataSourceName", crset.getDataSourceName());
        assertEquals(ResultSet.FETCH_REVERSE, crset.getFetchDirection());
        assertEquals(0, crset.getFetchSize());
        assertEquals(0, crset.getMaxFieldSize());
        assertEquals(0, crset.getMaxRows());
        assertEquals(10, crset.getPageSize());
        assertEquals("passwo", crset.getPassword());
        assertEquals(0, crset.getQueryTimeout());
        assertEquals("testTable", crset.getTableName());
        assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, crset
                .getTransactionIsolation());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, crset.getType());
        assertNull(crset.getTypeMap());
        assertNull(crset.getUrl());
        assertEquals("testUserName", crset.getUsername());
        assertTrue(crset.getEscapeProcessing());
        assertTrue(Arrays.equals(new int[] { 1 }, crset.getKeyColumns()));
    }

    public void testCommit() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.commit();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        try {
            noInitialCrset.commit();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        /*
         * test when call execute()
         */
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand("SELECT * FROM USER_INFO");
        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.execute();
        try {
            noInitialCrset.commit();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        assertTrue(CachedRowSet.COMMIT_ON_ACCEPT_CHANGES);
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand("SELECT * FROM USER_INFO");
        assertTrue(conn.getAutoCommit());
        noInitialCrset.execute(conn);
        noInitialCrset.commit();

        /*
         * test when call acceptChanges()
         */
        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        noInitialCrset.setUrl(DERBY_URL);

        noInitialCrset.acceptChanges();
        try {
            noInitialCrset.commit();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        noInitialCrset.acceptChanges(conn);
        noInitialCrset.commit();
    }

    public void testRollback() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.rollback();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        try {
            noInitialCrset.rollback();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        /*
         * test when call execute()
         */
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand("SELECT * FROM USER_INFO");
        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.execute();
        try {
            noInitialCrset.rollback();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        assertTrue(CachedRowSet.COMMIT_ON_ACCEPT_CHANGES);
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setCommand("SELECT * FROM USER_INFO");
        assertTrue(conn.getAutoCommit());
        noInitialCrset.execute(conn);
        noInitialCrset.rollback();

        /*
         * test when call acceptChanges()
         */
        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        noInitialCrset.setUrl(DERBY_URL);

        noInitialCrset.acceptChanges();
        try {
            noInitialCrset.rollback();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        noInitialCrset.acceptChanges(conn);
        noInitialCrset.rollback();
    }

    public void testAcceptChanges_UpdateException() throws Exception {
        assertTrue(crset.absolute(3));
        crset.updateString(2, "update3");
        crset.updateRow();

        assertTrue(crset.next());
        crset.updateString(2, "abcccccccccccccc"); // out of range
        crset.updateRow();

        try {
            crset.acceptChanges(conn);
            fail("should throw exception");
        } catch (SyncProviderException e) {
            // expected
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        int index = 0;
        while (rs.next()) {
            index++;
            if (index == 3) {
                assertEquals("update3", rs.getString(2));
            }
        }
        assertEquals(4, index);

        assertFalse(conn.getAutoCommit());
        crset.rollback();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        index = 0;
        while (rs.next()) {
            index++;
            if (index == 3) {
                assertEquals("test3", rs.getString(2));
            }
        }
        assertEquals(4, index);

        crset.beforeFirst();
        index = 0;
        while (crset.next()) {
            index++;
            if (index == 3) {
                assertEquals("update3", crset.getString(2));
            } else if (index == 4) {
                assertEquals("abcccccccccccccc", crset.getString(2));
            }
        }
        assertEquals(4, index);

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                crset.acceptChanges();
                fail("should throw exception");
            } catch (SyncProviderException e) {
                // expected
            }

            // check db
            rs = st.executeQuery("SELECT * FROM USER_INFO");
            index = 0;
            while (rs.next()) {
                index++;
                if (index == 3) {
                    assertEquals("test3", rs.getString(2));
                }
            }
            assertEquals(4, index);
        }
    }

    public void testAcceptChanges_InsertException() throws Exception {
        assertTrue(crset.absolute(4));
        crset.updateString(2, "update4");
        crset.updateRow();

        /*
         * Insert two rows. The second would throw exception
         */
        crset.moveToInsertRow();
        crset.updateInt(1, 5);
        crset.updateString(2, "insert5");
        crset.insertRow();
        crset.updateInt(1, 6);
        crset.updateString(2, "insert66666666666"); // out of range
        crset.insertRow();
        crset.moveToCurrentRow();

        assertTrue(conn.getAutoCommit());
        try {
            crset.acceptChanges(conn);
            fail("should throw exception");
        } catch (SyncProviderException e) {
            // expected
        }
        assertFalse(conn.getAutoCommit());

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        int index = 0;
        while (rs.next()) {
            index++;
            if (index == 4) {
                assertEquals("update4", rs.getString(2));
            } else if (index == 5) {
                assertEquals("insert5", rs.getString(2));
            }
        }
        assertEquals(5, index);

        crset.rollback();
        assertFalse(conn.getAutoCommit());
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        index = 0;
        while (rs.next()) {
            index++;
            if (index == 4) {
                assertEquals("test4", rs.getString(2));
            }
        }
        assertEquals(4, index);

        crset.beforeFirst();
        index = 0;

        while (crset.next()) {
            index++;
            if (index == 4) {
                assertEquals("update4", crset.getString(2));
            } else if (index == 5) {
                assertEquals("insert66666666666", crset.getString(2));
            } else if (index == 6) {
                assertEquals("insert5", crset.getString(2));
            }
        }
        assertEquals(6, index);

        try {
            crset.acceptChanges();
            fail("should throw exception");
        } catch (SyncProviderException e) {
            // expected
        }
    }
}
