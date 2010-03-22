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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.sql.RowSetEvent;
import javax.sql.RowSetInternal;
import javax.sql.RowSetListener;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.spi.SyncFactory;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncProviderException;
import javax.sql.rowset.spi.SyncResolver;

public class CachedRowSetImplTest extends CachedRowSetTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetOriginalRow() throws Exception {
        try {
            crset.getOriginalRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected: spec throw SQLException
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw ArrayIndexOutOfBoundsException
        }

        assertTrue(crset.absolute(3));
        assertNotSame(crset.getOriginalRow(), crset.getOriginalRow());

        crset.updateString(2, "update3");
        ResultSet originalRow = crset.getOriginalRow();
        assertTrue(originalRow.next());
        assertEquals("test3", originalRow.getString(2));

        // after call acceptChanges()
        crset.updateRow();
        crset.acceptChanges();
        assertTrue(crset.absolute(3));
        assertEquals("update3", crset.getString(2));
        originalRow = crset.getOriginalRow();
        assertTrue(originalRow.next());
        assertEquals("update3", originalRow.getString(2));
    }

    public void testSetOriginalRow() throws Exception {
        /*
         * This method is called internally after the current row have been
         * synchronized with the data source.
         */
        crset.beforeFirst();
        assertTrue(crset.absolute(3));
        crset.updateString(2, "update3");
        // NOTICE: though update the column here, updateRow() isn't called.

        assertTrue(crset.next());
        crset.deleteRow();
        crset.acceptChanges(conn);

        // DB
        rs = st.executeQuery("select * from user_info");
        int index = 0;
        while (rs.next()) {
            index++;
            assertEquals(index, rs.getInt(1));
            if (index == 3) {
                assertEquals("test3", rs.getString(2));
            }
        }
        assertEquals(3, index);
        // CachedRowSet
        crset.beforeFirst();
        index = 0;
        while (crset.next()) {
            index++;
            assertEquals(index, crset.getInt(1));
            if (index == 3) {
                assertEquals("update3", crset.getString(2));
            }
        }
        assertEquals(3, index);

        // move to the third row, call updateRow() again
        assertTrue(crset.absolute(3));
        crset.updateRow();
        crset.acceptChanges(conn);
        // Compare DB and CachedRowSet
        rs = st.executeQuery("select * from user_info");
        // CachedRowSet
        crset.beforeFirst();
        index = 0;
        while (crset.next() && rs.next()) {
            index++;
            assertEquals(index, rs.getInt(1));
            if (index == 3) {
                assertEquals("update3", rs.getString(2));
            }
        }
    }

    public void testSetSyncProvider() throws Exception {
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            String mySyncProvider = "org.apache.harmony.sql.internal.rowset.HYOptimisticProvider";
            crset.setSyncProvider(mySyncProvider);
            assertEquals(crset.getSyncProvider().getClass().getCanonicalName(),
                    mySyncProvider);
        }
    }

    public void testGetPageSize() throws SQLException {
        assertEquals(0, crset.getPageSize());
        crset.setPageSize(1);
        assertEquals(1, crset.getPageSize());
    }

    public void testSetPageSize() throws SQLException {
        try {
            crset.setPageSize(-1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected;
        }
        crset.setPageSize(0);
        crset.setPageSize(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, crset.getPageSize());
    }

    public void testGetTableName() throws SQLException {
        crset.setTableName("USER");
        assertEquals("USER", crset.getTableName());
    }

    public void testSetTableName() throws SQLException {
        try {
            crset.setTableName(null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected;
        }
    }

    public void testSize() throws Exception {
        assertEquals(DEFAULT_ROW_COUNT, crset.size());
        // before populate should return 0
        assertEquals(0, noInitialCrset.size());

        crset.absolute(3);
        assertFalse(crset.getShowDeleted());
        crset.deleteRow();
        assertEquals(DEFAULT_ROW_COUNT, crset.size());

        crset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");

        crset.populate(rs);
        crset.setShowDeleted(true);
        assertTrue(crset.getShowDeleted());
        assertEquals(DEFAULT_ROW_COUNT, crset.size());

        crset.absolute(3);
        crset.deleteRow();
        assertEquals(DEFAULT_ROW_COUNT, crset.size());

        crset.acceptChanges(conn);

        assertEquals(DEFAULT_ROW_COUNT - 1, crset.size());
    }

    public void testAcceptChanges() throws SQLException {
        crset.setTableName("USER_INFO");
        // NOTICE: if the value of column is null, it would go wrong when
        // call acceptChanges(). And if one method in TestCase throws
        // SQLException, the following method will be affected.
        rs.next();
        assertEquals(1, rs.getInt(1));
        assertEquals("hermit", rs.getString(2));

        crset.absolute(3);
        assertEquals(3, crset.getInt(1));
        assertEquals("test3", crset.getString(2));
        crset.updateString(2, "HarmonY");

        crset.moveToInsertRow();
        crset.updateInt(1, 16);
        crset.updateString(2, "Apache");
        crset.insertRow();
        crset.moveToCurrentRow();

        crset.deleteRow();
        /*
         * RI will print the change result
         */
        crset.acceptChanges();

        rs = st.executeQuery("select * from USER_INFO where NAME = 'hermit'");
        rs.next();
        assertEquals("hermit", rs.getString(2));
        rs = st.executeQuery("select * from USER_INFO where NAME = 'test4'");
        rs.next();
        assertEquals("test4", rs.getString(2));

    }

    public void testExecute() throws SQLException {
        crset.setCommand("Update User_INFO set Name= ? Where ID= ? ");
        crset.setString(1, "executed!");
        crset.setInt(2, 1);
        crset.execute();

        crset.setCommand("SELECT ID, NAME FROM USER_INFO" + " WHERE ID = ? ");
        crset.setInt(1, 1);
        crset.execute();

        crset.first();
        assertEquals("executed!", crset.getString(2));

        crset.setCommand("Update User_INFO set Name= ? Where ID= ? ");
        crset.setString(1, "executed!");
        crset.setInt(2, 1);
        crset.execute(DriverManager.getConnection(DERBY_URL));

        crset.setCommand("SELECT ID, NAME FROM USER_INFO" + " WHERE ID = ? ");
        crset.setInt(1, 1);
        crset.execute(DriverManager.getConnection(DERBY_URL));
    }

    public void testExecute2() throws Exception {
        crset.setCommand("SELECT ID, NAME FROM USER_INFO" + " WHERE ID = ? ");
        crset.setInt(1, 1);
        crset.execute();

        crset.first();
        assertEquals("hermit", crset.getString(2));
    }

    public void testExecute3() throws Exception {
        // insert 15 more rows for test
        insertMoreData(15);

        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.setPageSize(5);
        noInitialCrset.setCommand("select * from USER_INFO");
        noInitialCrset.execute();
        rs = st.executeQuery("select * from USER_INFO");
        int cursorIndex = 0;
        while (noInitialCrset.next() && rs.next()) {
            cursorIndex++;
            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; i++) {
                assertEquals(rs.getObject(i), noInitialCrset.getObject(i));
            }
        }
        // The pageSize works here. CachedRowSet only get 5 rows from ResultSet.
        assertEquals(5, cursorIndex);

        // change a command
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setUrl(null);
        // The pageSize still work here
        noInitialCrset.setPageSize(5);
        assertEquals(5, noInitialCrset.getPageSize());
        noInitialCrset.setCommand("select * from USER_INFO where NAME like ?");
        noInitialCrset.setString(1, "test%");
        Connection aConn = DriverManager.getConnection(DERBY_URL);
        noInitialCrset.execute(aConn);
        aConn.close();
        cursorIndex = 1;
        while (noInitialCrset.next()) {
            cursorIndex++;
            assertEquals(cursorIndex, noInitialCrset.getInt(1));
        }
        assertEquals(6, cursorIndex);

        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");

        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.setPageSize(5);
        noInitialCrset.setMaxRows(2);
        noInitialCrset.setCommand("select * from USER_INFO");

        noInitialCrset.execute();

        rs = st.executeQuery("select * from USER_INFO");
        cursorIndex = 0;
        while (noInitialCrset.next() && rs.next()) {
            cursorIndex++;
        }
        // maxRows works here
        assertEquals(2, cursorIndex);
    }

    public void testCreateShared() throws Exception {
        crset.setUsername("testUsername");
        crset.setPassword("testPassword");
        crset.setPageSize(5);
        Listener listener = new Listener(); // a class implements RowSetListener
        crset.addRowSetListener(listener);
        crset.absolute(3); // move to the third row for testing
        assertEquals(CachedRowSetListenerTest.EVENT_CURSOR_MOVED, listener
                .getTag());
        listener.clear();

        CachedRowSet crsetShared = (CachedRowSet) crset.createShared();
        assertEquals("testUsername", crsetShared.getUsername());
        assertEquals("testPassword", crsetShared.getPassword());
        assertEquals(5, crsetShared.getPageSize());
        // check whether modify the attribute of the original is visible to the
        // duplicate
        crset.setUsername("modifyUsername");
        crset.setPageSize(10);
        assertEquals("modifyUsername", crset.getUsername());
        assertEquals("testUsername", crsetShared.getUsername());
        assertEquals(10, crset.getPageSize());
        assertEquals(5, crsetShared.getPageSize());

        // compare the current row, that is the third row
        assertEquals(3, crset.getInt(1));
        assertEquals("test3", crset.getString(2));
        assertEquals(3, crsetShared.getInt(1));
        assertEquals("test3", crsetShared.getString(2));
        // check whether update the duplicate is visible to the original
        crsetShared.updateString(2, "modify3");
        assertEquals("modify3", crsetShared.getString(2));
        assertEquals("modify3", crset.getString(2));
        crsetShared.updateRow();
        listener.clear();
        crsetShared.acceptChanges();
        assertEquals(CachedRowSetListenerTest.EVENT_ROWSET_CHANGED, listener
                .getTag());

        // when move the duplicate's cursor, the original shouldn't be affected
        crsetShared.absolute(1);
        assertEquals(1, crsetShared.getInt(1));
        assertEquals(3, crset.getInt(1));
    }

    public void testcreateCopyNoConstraints() throws Exception {
        crset.setConcurrency(ResultSet.CONCUR_READ_ONLY);
        crset.setType(ResultSet.TYPE_SCROLL_SENSITIVE);
        crset.setEscapeProcessing(false);
        crset.setMaxRows(10);
        crset.setTransactionIsolation(Connection.TRANSACTION_NONE);
        crset.setQueryTimeout(10);
        crset.setPageSize(10);
        crset.setShowDeleted(true);
        crset.setUsername("username");
        crset.setPassword("password");
        crset.setTypeMap(new HashMap<String, Class<?>>());
        crset.setMaxFieldSize(10);
        crset.setFetchDirection(ResultSet.FETCH_UNKNOWN);

        /*
         * NOTICE: when run on RI, if add the listener first, then it will go
         * wrong when call createCopySchema().It's said that clone failed.
         */
        CachedRowSet copy = crset.createCopyNoConstraints();

        // default is ResultSet.CONCUR_UPDATABLE
        assertEquals(ResultSet.CONCUR_UPDATABLE, copy.getConcurrency());
        // default is ResultSet.TYPE_SCROLL_INSENSITIVE
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, copy.getType());
        // default is true
        assertTrue(copy.getEscapeProcessing());
        // default is 0
        assertEquals(0, copy.getMaxRows());
        // default is Connection.TRANSACTION_READ_COMMITTED
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, copy
                .getTransactionIsolation());
        // default is 0
        assertEquals(0, copy.getQueryTimeout());
        // default is false
        assertFalse(copy.getShowDeleted());
        // default is 0
        assertEquals(0, copy.getMaxFieldSize());
        // default is null
        assertNull(copy.getPassword());
        // default is null
        assertNull(copy.getUsername());
        // default is null
        assertNull(copy.getTypeMap());

        if (crset.getKeyColumns() != null && crset.getKeyColumns().length != 0) {

            int[] keyColumns = crset.getKeyColumns();
            int[] copyKeyColumns = copy.getKeyColumns();

            assertEquals(keyColumns.length, copyKeyColumns.length);
            for (int i = 0; i < keyColumns.length; i++) {
                assertEquals(keyColumns[i], copyKeyColumns[i]);
            }
            assertEquals(crset.getKeyColumns(), copy.getKeyColumns());
        }

        assertEquals(crset.getFetchDirection(), copy.getFetchDirection());
        assertEquals(crset.getPageSize(), copy.getPageSize());

        assertEquals(crset.isBeforeFirst(), copy.isBeforeFirst());
        assertEquals(crset.isAfterLast(), copy.isAfterLast());
        assertEquals(crset.isFirst(), copy.isFirst());
        assertEquals(crset.isLast(), copy.isLast());
        assertEquals(crset.getRow(), copy.getRow());

        assertEquals(crset.isReadOnly(), copy.isReadOnly());
        assertEquals(crset.size(), copy.size());

        // different metaData object
        assertNotSame(crset.getMetaData(), copy.getMetaData());

        isMetaDataEquals(crset.getMetaData(), copy.getMetaData());

        assertEquals(crset.getCommand(), copy.getCommand());

        // check SyncProvider
        assertEquals(crset.getSyncProvider().getProviderID(), copy
                .getSyncProvider().getProviderID());
        assertEquals(crset.getSyncProvider().getProviderGrade(), copy
                .getSyncProvider().getProviderGrade());
        assertEquals(crset.getSyncProvider().getDataSourceLock(), copy
                .getSyncProvider().getDataSourceLock());
        assertEquals(crset.getSyncProvider().getVendor(), copy
                .getSyncProvider().getVendor());
        assertEquals(crset.getSyncProvider().getVersion(), copy
                .getSyncProvider().getVersion());

        assertEquals(crset.getTableName(), copy.getTableName());
        assertEquals(crset.getUrl(), copy.getUrl());

    }

    public void testcreateCopyNoConstraints2() throws Exception {

        // the default value
        assertNull(crset.getCommand());
        assertEquals(ResultSet.CONCUR_UPDATABLE, crset.getConcurrency());
        assertNull(crset.getDataSourceName());
        assertEquals(ResultSet.FETCH_FORWARD, crset.getFetchDirection());
        assertEquals(0, crset.getFetchSize());
        assertEquals(0, crset.getMaxFieldSize());
        assertEquals(0, crset.getMaxRows());
        assertEquals(0, crset.getPageSize());
        assertNull(crset.getPassword());
        assertEquals(0, crset.getQueryTimeout());
        assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, crset
                .getTransactionIsolation());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, crset.getType());
        assertEquals(DERBY_URL, crset.getUrl());
        assertNull(crset.getUsername());
        assertTrue(crset.getEscapeProcessing());
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertNotNull(crset.getKeyColumns());
            assertEquals(0, crset.getKeyColumns().length);
        } else {
            assertNull(crset.getKeyColumns());
        }

        // set value
        crset.setCommand("testCommand");
        crset.setConcurrency(ResultSet.CONCUR_READ_ONLY);
        crset.setDataSourceName("testDataSourceName");
        crset.setFetchDirection(ResultSet.FETCH_REVERSE);
        crset.setMaxFieldSize(100);
        crset.setMaxRows(10);
        crset.setPageSize(10);
        crset.setPassword("passwo");
        crset.setQueryTimeout(100);
        crset.setTableName("testTable");
        crset.setTransactionIsolation(ResultSet.HOLD_CURSORS_OVER_COMMIT);
        crset.setType(ResultSet.TYPE_SCROLL_SENSITIVE);
        crset.setTypeMap(new HashMap<String, Class<?>>());
        crset.setUsername("testUserName");
        crset.setEscapeProcessing(false);
        crset.setKeyColumns(new int[] { 1 });

        // check the changed value
        assertEquals("testCommand", crset.getCommand());
        assertEquals(ResultSet.CONCUR_READ_ONLY, crset.getConcurrency());
        assertEquals("testDataSourceName", crset.getDataSourceName());
        assertEquals(ResultSet.FETCH_REVERSE, crset.getFetchDirection());
        assertEquals(0, crset.getFetchSize());
        assertEquals(100, crset.getMaxFieldSize());
        assertEquals(10, crset.getMaxRows());
        assertEquals(10, crset.getPageSize());
        assertEquals("passwo", crset.getPassword());
        assertEquals(100, crset.getQueryTimeout());
        assertEquals("testTable", crset.getTableName());
        assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, crset
                .getTransactionIsolation());
        assertEquals(ResultSet.TYPE_SCROLL_SENSITIVE, crset.getType());
        assertNotNull(crset.getTypeMap());
        assertNull(crset.getUrl());
        assertEquals("testUserName", crset.getUsername());
        assertFalse(crset.getEscapeProcessing());
        assertTrue(Arrays.equals(new int[] { 1 }, crset.getKeyColumns()));

        // after call createCopyNoConstraints
        CachedRowSet copy = crset.createCopyNoConstraints();
        assertEquals("testCommand", copy.getCommand());
        assertEquals(ResultSet.CONCUR_UPDATABLE, copy.getConcurrency());
        assertEquals("testDataSourceName", copy.getDataSourceName());
        assertEquals(ResultSet.FETCH_REVERSE, copy.getFetchDirection());
        assertEquals(0, copy.getFetchSize());
        assertEquals(0, copy.getMaxFieldSize());
        assertEquals(0, copy.getMaxRows());
        assertEquals(10, copy.getPageSize());
        assertNull(copy.getPassword());
        assertEquals(0, copy.getQueryTimeout());
        assertEquals("testTable", copy.getTableName());
        assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, copy
                .getTransactionIsolation());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, copy.getType());
        assertNull(copy.getTypeMap());
        assertNull(copy.getUrl());
        assertNull(copy.getUsername());
        assertTrue(copy.getEscapeProcessing());
        assertTrue(Arrays.equals(new int[] { 1 }, copy.getKeyColumns()));
    }

    public void testCopySchema() throws Exception {
        // the original's addtribute and meta data
        crset.setCommand("testCommand");
        crset.setConcurrency(ResultSet.CONCUR_UPDATABLE);
        crset.setDataSourceName("testDataSource");
        crset.setFetchDirection(ResultSet.FETCH_UNKNOWN);
        crset.setPageSize(20);
        crset.setMaxRows(20);
        crset.setTableName("USER_INFO");
        /*
         * NOTICE: spec say copy must not has any content, but when run on RI,
         * if call next() before call createCopySchema(), the copy can get the
         * current row's data
         */

        /*
         * NOTICE: when run on RI, if add the listener first, then it will go
         * wrong when call createCopySchema().It's said that clone failed.
         */
        // Listener listener = new Listener();
        // crset.addRowSetListener(listener);
        RowSetMetaData rsmd = (RowSetMetaData) crset.getMetaData();
        // the copy
        CachedRowSet crsetCopySchema = crset.createCopySchema();
        RowSetMetaData rsmdCopySchema = (RowSetMetaData) crsetCopySchema
                .getMetaData();

        // compare the meta data between the duplicate and the original
        assertNotSame(crset.getMetaData(), crsetCopySchema.getMetaData());
        assertNotSame(crset.getOriginal(), crsetCopySchema.getOriginal());

        assertEquals("USER_INFO", crset.getTableName());
        assertEquals("USER_INFO", rsmdCopySchema.getTableName(1));
        assertEquals(DEFAULT_COLUMN_COUNT, rsmdCopySchema.getColumnCount());
        assertEquals(rsmd.getColumnName(1), rsmdCopySchema.getColumnName(1));
        // check the primary key
        // TODO: RI doesn't evalute the keyColumns. The value of
        // crset.getKeyColumns() is null.
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertNotNull(crset.getKeyColumns());
            assertEquals(0, crset.getKeyColumns().length);
        } else {
            assertNull(crset.getKeyColumns());
        }

        // check the attributes in the duplicate. These are supposed to be the
        // same as the original
        assertFalse(crsetCopySchema.next());
        assertEquals("testCommand", crsetCopySchema.getCommand());
        assertEquals(ResultSet.CONCUR_UPDATABLE, crsetCopySchema
                .getConcurrency());
        assertEquals("testDataSource", crsetCopySchema.getDataSourceName());
        assertEquals(ResultSet.FETCH_UNKNOWN, crsetCopySchema
                .getFetchDirection());
        assertEquals(20, crsetCopySchema.getPageSize());
        assertEquals(20, crsetCopySchema.getMaxRows());

        // fill the duplicate CachedRowSet with data, check the listener
        Listener listener = new Listener();
        crsetCopySchema.addRowSetListener(listener);
        assertNull(listener.getTag());
        rs = st.executeQuery("select * from USER_INFO");
        crsetCopySchema.populate(rs);
        assertEquals("rowSetChanged", listener.getTag());
        listener.clear();
        // the move of the original's cursor shouldn't affect the duplicate
        crset.next();
        assertNull(listener.getTag());
    }

    public void testCopySchema2() throws Exception {

        // set value
        crset.setCommand("testCommand");
        crset.setConcurrency(ResultSet.CONCUR_READ_ONLY);
        crset.setDataSourceName("testDataSourceName");
        crset.setFetchDirection(ResultSet.FETCH_REVERSE);
        crset.setMaxFieldSize(100);
        crset.setMaxRows(10);
        crset.setPageSize(10);
        crset.setPassword("passwo");
        crset.setQueryTimeout(100);
        crset.setTableName("testTable");
        crset.setTransactionIsolation(ResultSet.HOLD_CURSORS_OVER_COMMIT);
        crset.setType(ResultSet.TYPE_SCROLL_SENSITIVE);
        crset.setTypeMap(new HashMap<String, Class<?>>());
        crset.setEscapeProcessing(false);
        crset.setKeyColumns(new int[] { 1 });

        // call createCopySchema()
        CachedRowSet copy = crset.createCopySchema();
        assertFalse(copy.next());
        assertEquals(crset.getCommand(), copy.getCommand());
        assertEquals(crset.getConcurrency(), copy.getConcurrency());
        assertEquals(crset.getDataSourceName(), copy.getDataSourceName());
        assertEquals(crset.getFetchDirection(), copy.getFetchDirection());
        assertEquals(crset.getMaxFieldSize(), copy.getMaxFieldSize());
        assertEquals(crset.getMaxRows(), copy.getMaxRows());
        assertEquals(crset.getPageSize(), copy.getPageSize());
        assertEquals(crset.getQueryTimeout(), copy.getQueryTimeout());
        assertEquals(crset.getTableName(), copy.getTableName());
        assertEquals(crset.getTransactionIsolation(), copy
                .getTransactionIsolation());
        assertEquals(crset.getType(), copy.getType());
        assertEquals(crset.getUrl(), copy.getUrl());
        assertEquals(crset.getEscapeProcessing(), copy.getEscapeProcessing());
        assertTrue(Arrays.equals(crset.getKeyColumns(), copy.getKeyColumns()));

        // compare the object reference
        assertNotSame(crset.getKeyColumns(), copy.getKeyColumns());
        assertNotSame(crset.getMetaData(), copy.getMetaData());
        assertNotSame(crset.getOriginal(), copy.getOriginal());
        assertNotSame(crset.getTypeMap(), copy.getTypeMap());
    }

    public void testCreateCopy() throws Exception {

        // TODO: lack of the test for CachedRowSet.getOriginal() and
        // CachedRowSet.getOriginalRow()

        crset.absolute(3);

        CachedRowSet crsetCopy = crset.createCopy();

        crsetCopy.updateString(2, "copyTest3");
        crsetCopy.updateRow();
        crsetCopy.acceptChanges();

        assertEquals("copyTest3", crsetCopy.getString(2));

        assertEquals("test3", crset.getString(2));

        rs = st.executeQuery("select * from USER_INFO");
        rs.next();
        rs.next();
        rs.next();

        assertEquals("copyTest3", rs.getString(2));

        reloadCachedRowSet();
        crset.absolute(2);

        crsetCopy = crset.createCopy();

        assertEquals(crset.isReadOnly(), crsetCopy.isReadOnly());
        assertEquals(crset.isBeforeFirst(), crsetCopy.isBeforeFirst());
        assertEquals(crset.isAfterLast(), crsetCopy.isAfterLast());
        assertEquals(crset.isFirst(), crsetCopy.isFirst());
        assertEquals(crset.isLast(), crsetCopy.isLast());

        assertEquals(crset.size(), crsetCopy.size());
        // different metaData object
        assertNotSame(crset.getMetaData(), crsetCopy.getMetaData());

        isMetaDataEquals(crset.getMetaData(), crsetCopy.getMetaData());

        assertEquals(crset.getCommand(), crsetCopy.getCommand());
        assertEquals(crset.getConcurrency(), crsetCopy.getConcurrency());

        try {
            assertEquals(crset.getCursorName(), crsetCopy.getCursorName());
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            assertEquals(crset.getMatchColumnIndexes(), crsetCopy
                    .getMatchColumnIndexes());
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            assertEquals(crset.getMatchColumnNames(), crsetCopy
                    .getMatchColumnNames());
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        assertEquals(crset.getRow(), crsetCopy.getRow());
        assertEquals(crset.getStatement(), crsetCopy.getStatement());

        assertEquals(crset.getEscapeProcessing(), crsetCopy
                .getEscapeProcessing());
        assertEquals(crset.getFetchDirection(), crsetCopy.getFetchDirection());
        assertEquals(crset.getFetchSize(), crsetCopy.getFetchSize());
        if (crset.getKeyColumns() != null && crset.getKeyColumns().length != 0) {
            int[] keyColumns = crset.getKeyColumns();
            int[] copyKeyColumns = crsetCopy.getKeyColumns();

            assertEquals(keyColumns.length, copyKeyColumns.length);
            for (int i = 0; i < keyColumns.length; i++) {
                assertEquals(keyColumns[i], copyKeyColumns[i]);
            }
            assertEquals(crset.getKeyColumns(), crsetCopy.getKeyColumns());
        }

        assertEquals(crset.getMaxFieldSize(), crsetCopy.getMaxFieldSize());
        assertEquals(crset.getMaxRows(), crsetCopy.getMaxRows());

        assertEquals(crset.getPageSize(), crsetCopy.getPageSize());
        assertEquals(crset.getPassword(), crsetCopy.getPassword());
        assertEquals(crset.getQueryTimeout(), crsetCopy.getQueryTimeout());
        assertEquals(crset.getShowDeleted(), crsetCopy.getShowDeleted());

        assertEquals(crset.getSyncProvider().getProviderID(), crsetCopy
                .getSyncProvider().getProviderID());
        assertEquals(crset.getSyncProvider().getProviderGrade(), crsetCopy
                .getSyncProvider().getProviderGrade());
        assertEquals(crset.getSyncProvider().getDataSourceLock(), crsetCopy
                .getSyncProvider().getDataSourceLock());
        assertEquals(crset.getSyncProvider().getVendor(), crsetCopy
                .getSyncProvider().getVendor());
        assertEquals(crset.getSyncProvider().getVersion(), crsetCopy
                .getSyncProvider().getVersion());

        assertEquals(crset.getTableName(), crsetCopy.getTableName());
        assertEquals(crset.getTransactionIsolation(), crsetCopy
                .getTransactionIsolation());
        assertEquals(crset.getType(), crsetCopy.getType());

        assertEquals(crset.getUrl(), crsetCopy.getUrl());
        assertEquals(crset.getUsername(), crsetCopy.getUsername());

    }

    public void testCreateCopy2() throws Exception {

        CachedRowSet copy = crset.createCopy();

        copy.absolute(3);
        crset.absolute(3);

        copy.updateString(2, "updated");
        assertEquals("updated", copy.getString(2));
        assertEquals("test3", crset.getString(2));
        copy.updateRow();
        copy.acceptChanges();

        assertEquals("updated", copy.getString(2));
        assertEquals("test3", crset.getString(2));

        crset.updateString(2, "again");

        assertEquals("updated", copy.getString(2));
        assertEquals("again", crset.getString(2));

        crset.updateRow();
        try {
            /*
             * seems ri doesn't release lock when exception throw from
             * acceptChanges(), which will cause test case block at insertData()
             * when next test case setUp, so we must pass current connection to
             * it, and all resource would be released after connection closed.
             */
            crset.acceptChanges(conn);
            fail("Should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            assertTrue(resolver.nextConflict());
            assertEquals(3, resolver.getRow());

            assertEquals(SyncResolver.UPDATE_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                // all values are null
                assertNull(resolver.getConflictValue(i));
            }

            assertFalse(resolver.nextConflict());
        }

        assertEquals("updated", copy.getString(2));

        crset.absolute(3);
        // data doesn't change
        assertEquals("again", crset.getString(2));
    }

    public void testCreateCopy3() throws Exception {
        crset.setCommand("SELECT * FROM USER_INFO WHERE ID = ?");
        crset.setInt(1, 3);
        crset.execute();

        assertEquals(DEFAULT_COLUMN_COUNT, crset.getMetaData().getColumnCount());
        assertTrue(crset.next());
        assertEquals("test3", crset.getString(2));
        assertFalse(crset.next());

        CachedRowSet crsetCopy = crset.createCopy();
        crsetCopy.execute();
        assertEquals(DEFAULT_COLUMN_COUNT, crsetCopy.getMetaData()
                .getColumnCount());
        assertTrue(crsetCopy.next());
        assertEquals("test3", crsetCopy.getString(2));
        assertFalse(crsetCopy.next());

        crsetCopy.setCommand("SELECT * FROM USER_INFO WHERE NAME = ?");
        crsetCopy.setString(1, "test4");
        crsetCopy.execute();
        assertTrue(crsetCopy.next());
        assertEquals(4, crsetCopy.getInt(1));
        assertFalse(crsetCopy.next());

        crset.execute();
        assertTrue(crset.next());
        assertEquals("test3", crset.getString(2));
        assertFalse(crset.next());
    }

    public void testCreateCopy4() throws Exception {
        crset.setCommand("SELECT * FROM USER_INFO WHERE ID = ?");
        crset.setInt(1, 3);
        crset.execute();
        // check data
        assertTrue(crset.next());
        assertEquals(3, crset.getInt(1));
        assertFalse(crset.next());

        // deep copy
        CachedRowSet copyCrset = crset.createCopy();
        copyCrset.beforeFirst();
        assertTrue(copyCrset.next());
        assertEquals(3, copyCrset.getInt(1));
        assertFalse(copyCrset.next());
        copyCrset.execute();
        assertTrue(copyCrset.next());
        assertEquals(3, copyCrset.getInt(1));
        assertFalse(copyCrset.next());

        crset.setInt(1, 4);
        crset.execute();
        crset.beforeFirst();
        assertTrue(crset.next());
        assertEquals(4, crset.getInt(1));
        assertFalse(crset.next());

        copyCrset.beforeFirst();
        assertTrue(copyCrset.next());
        assertEquals(3, copyCrset.getInt(1));
        assertFalse(copyCrset.next());

        copyCrset.execute();
        copyCrset.beforeFirst();
        assertTrue(copyCrset.next());
        assertEquals(3, copyCrset.getInt(1));
        assertFalse(copyCrset.next());

        copyCrset.setInt(1, 1);
        copyCrset.execute();
        assertTrue(copyCrset.next());
        assertEquals(1, copyCrset.getInt(1));
        assertFalse(copyCrset.next());

        crset.beforeFirst();
        assertTrue(crset.next());
        assertEquals(4, crset.getInt(1));
        assertFalse(crset.next());

        crset.execute();
        crset.beforeFirst();
        assertTrue(crset.next());
        assertEquals(4, crset.getInt(1));
        assertFalse(crset.next());
    }

    public void testAfterLast() throws Exception {
        try {
            rs.afterLast();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        crset.afterLast();
        crset.previous();
        assertEquals(4, crset.getInt(1));
    }

    public void testNextandPreviousPage() throws Exception {

        st.executeUpdate("delete from USER_INFO");
        st.executeUpdate("insert into USER_INFO(ID,NAME) values (1,'1')");
        st.executeUpdate("insert into USER_INFO(ID,NAME) values (2,'2')");
        st.executeUpdate("insert into USER_INFO(ID,NAME) values (3,'3')");
        st.executeUpdate("insert into USER_INFO(ID,NAME) values (4,'4')");
        rs = st.executeQuery("select * from USER_INFO");

        crset.setPageSize(2);
        crset.setCommand("SELECT ID FROM USER_INFO");
        crset.execute();

        for (int j = 0; j < 2; j++)
            crset.next();
        assertFalse(crset.next());

        int i = 0;

        /*
         * TODO In RI there are before first page and after last page, according
         * spec, there shouldn't be, Harmony follow spec
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            i = 2;
        }

        crset.beforeFirst();
        while (crset.nextPage()) {
            while (crset.next()) {
                assertEquals(++i, crset.getInt(1));
            }
        }

        /*
         * TODO In RI there are before first page and after last page, according
         * spec, there shouldn't be, Harmony follow spec
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            i = 2;
        }

        while (crset.previousPage()) {
            crset.afterLast();
            while (crset.previous()) {
                assertEquals(i--, crset.getInt(1));
            }
        }

        while (crset.previousPage()) {
            i = i - crset.getPageSize();
            int j = i;
            while (crset.next()) {
                assertEquals(++j, crset.getInt(1));
            }
        }
    }

    public void testPopulate_LResultSet() throws Exception {
        // insert 15 more rows for test
        insertMoreData(15);

        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.setMaxRows(15);
        assertEquals(15, noInitialCrset.getMaxRows());

        noInitialCrset.populate(rs);

        assertTrue(noInitialCrset.isBeforeFirst());
        int cursorIndex = 0;
        while (noInitialCrset.next()) {
            cursorIndex++;
        }
        // setMaxRows no effect, we follow ri
        assertEquals(19, cursorIndex);

        /*
         * The pageSize won't work when call method populate(ResultSet) without
         * second parameter
         */
        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");

        noInitialCrset.setMaxRows(15);
        assertEquals(15, noInitialCrset.getMaxRows());

        noInitialCrset.setPageSize(5);
        assertEquals(5, noInitialCrset.getPageSize());

        noInitialCrset.populate(rs);

        assertTrue(noInitialCrset.isBeforeFirst());
        rs = st.executeQuery("select * from USER_INFO");
        cursorIndex = 0;
        while (noInitialCrset.next() && rs.next()) {
            cursorIndex++;
        }
        /*
         * It's supposed to only get five rows in CachedRowSet as the
         * CachedRowSet's pageSize is 5. However, the pageSize doesn't work in
         * RI. The CachedRowSet gets all the data from ResultSet. We follow ri.
         */
        assertEquals(19, cursorIndex);

        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        // cursor move two rows
        rs.next();
        rs.next();

        noInitialCrset.populate(rs);
        assertTrue(noInitialCrset.isBeforeFirst());
        cursorIndex = 0;
        while (noInitialCrset.next()) {
            cursorIndex++;
        }
        assertEquals(17, cursorIndex);
    }

    public void testPopulate_LResultSet_I() throws Exception {
        // insert 15 more rows for test
        insertMoreData(15);

        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.setPageSize(5);
        try {
            noInitialCrset.populate(rs, 1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            noInitialCrset.populate(null, 1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, we follow spec
        } catch (NullPointerException e) {
            // ri throw NullPointerException
        }

        // create a scrollable and updatable ResultSet
        st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        rs = st.executeQuery("select * from USER_INFO");

        noInitialCrset = newNoInitialInstance();
        noInitialCrset.setPageSize(6);
        noInitialCrset.populate(rs, 6);
        int cursorIndex = 5;
        while (noInitialCrset.next()) {
            cursorIndex++;
            assertEquals(cursorIndex, noInitialCrset.getInt(1));
        }
        assertEquals(11, cursorIndex);

        st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        rs = st.executeQuery("select * from USER_INFO");

        noInitialCrset = newNoInitialInstance();

        noInitialCrset.setPageSize(6);
        noInitialCrset.setMaxRows(5);

        noInitialCrset.populate(rs, 6);
        cursorIndex = 0;
        while (noInitialCrset.next()) {
            cursorIndex++;
            assertEquals(cursorIndex + 5, noInitialCrset.getInt(1));
        }
        // only get MaxRows
        assertEquals(5, cursorIndex);

        st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        rs = st.executeQuery("select * from USER_INFO");

        noInitialCrset = newNoInitialInstance();

        noInitialCrset.setMaxRows(5);
        try {
            noInitialCrset.setPageSize(6);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Page size cannot be greater than maxRows
        }

    }

    public void testPopulate_use_copy() throws Exception {
        // insert 15 more rows for test
        insertMoreData(15);

        rs = st.executeQuery("select * from USER_INFO");
        crset.close();
        crset.populate(rs);

        CachedRowSet crsetCopy = crset.createCopy();
        assertEquals(0, crsetCopy.getPageSize());
        noInitialCrset.setPageSize(5);
        // if it doesn't specify the startRow for method populate(), then the
        // pageSize wouldn't work.
        assertTrue(crsetCopy.isBeforeFirst());
        noInitialCrset.populate(crsetCopy);
        assertTrue(crsetCopy.isAfterLast());
        int cursorIndex = 0;
        while (noInitialCrset.next()) {
            cursorIndex++;
            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; i++) {
                assertEquals(cursorIndex, noInitialCrset.getInt(1));
            }
        }
        assertEquals(19, cursorIndex);

        try {
            noInitialCrset.populate(crsetCopy, 0);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // invalid cursor position
        }

        try {
            noInitialCrset.populate(crsetCopy, -1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // invalid cursor position
        }

        try {
            noInitialCrset.populate(crsetCopy, 100);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // invalid cursor position
        }

        // specify the startRow, then the noInitialCrset will get only 5 rows
        noInitialCrset.populate(crsetCopy, 1);
        assertEquals(5, noInitialCrset.getPageSize());
        assertTrue(noInitialCrset.isBeforeFirst());
        cursorIndex = 0;
        rs = st.executeQuery("select * from USER_INFO");
        while (noInitialCrset.next() && rs.next()) {
            cursorIndex++;
            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; i++) {
                assertEquals(cursorIndex, noInitialCrset.getInt(1));
                assertEquals(rs.getObject(i), noInitialCrset.getObject(i));
            }
        }
        // the pageSize works here
        assertEquals(5, cursorIndex);

        // the noInitialCrset would fetch data from the eleventh row
        noInitialCrset.populate(crsetCopy, 11);
        cursorIndex = 10;
        while (noInitialCrset.next()) {
            cursorIndex++;
            assertEquals(cursorIndex, noInitialCrset.getInt(1));
        }
        assertEquals(15, cursorIndex);
    }

    public void testConstructor() throws Exception {

        assertTrue(noInitialCrset.isReadOnly());
        assertEquals(0, noInitialCrset.size());
        assertNull(noInitialCrset.getMetaData());

        assertNull(noInitialCrset.getCommand());
        assertEquals(ResultSet.CONCUR_UPDATABLE, noInitialCrset
                .getConcurrency());
        assertEquals(0, crset.getRow());

        try {
            crset.getCursorName();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            crset.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        assertNull(crset.getStatement());

        assertTrue(noInitialCrset.getEscapeProcessing());
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, noInitialCrset
                .getTransactionIsolation());

        assertEquals(ResultSet.FETCH_FORWARD, noInitialCrset
                .getFetchDirection());
        assertEquals(0, noInitialCrset.getFetchSize());
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                noInitialCrset.getKeyColumns();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        } else {
            assertNull(noInitialCrset.getKeyColumns());
        }

        assertEquals(0, noInitialCrset.getMaxFieldSize());
        assertEquals(0, noInitialCrset.getMaxRows());

        assertEquals(0, noInitialCrset.getPageSize());
        assertNull(noInitialCrset.getPassword());
        assertEquals(0, noInitialCrset.getQueryTimeout());
        assertFalse(noInitialCrset.getShowDeleted());

        assertNull(noInitialCrset.getTableName());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, noInitialCrset
                .getType());

        assertNull(noInitialCrset.getUrl());
        assertNull(noInitialCrset.getUsername());

    }

    public void testRelative() throws Exception {
        /*
         * ri throw SQLException, but spec say relative(1) is identical to next
         */
        try {
            crset.relative(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        assertTrue(crset.next());
        assertEquals("hermit", crset.getString(2));

        assertTrue(crset.relative(2));
        assertEquals("test3", crset.getString(2));

        assertTrue(crset.relative(-1));
        assertEquals("test", crset.getString(2));

        assertTrue(crset.relative(0));
        assertEquals("test", crset.getString(2));

        assertFalse(crset.relative(-5));
        assertEquals(0, crset.getRow());

        assertTrue(crset.next());
        assertEquals("hermit", crset.getString(2));
        assertTrue(crset.relative(3));
        assertEquals("test4", crset.getString(2));

        assertFalse(crset.relative(3));
        assertEquals(0, crset.getRow());

        assertTrue(crset.isAfterLast());
        assertTrue(crset.previous());

        // TODO RI's bug
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(DEFAULT_ROW_COUNT, crset.getRow());
            assertEquals("test4", crset.getString(2));
        } else {
            assertEquals(-1, crset.getRow());
            assertEquals("test4", crset.getString(2));

            assertTrue(crset.previous());
            assertEquals(-2, crset.getRow());
            assertEquals("test3", crset.getString(2));
        }
    }

    public void testAbsolute() throws Exception {
        // TODO non-bug different
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertFalse(crset.absolute(0));
        } else {
            try {
                assertTrue(crset.absolute(0));
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // invalid cursor position
            }
        }

        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, crset.getType());
        assertTrue(crset.absolute(1));
        assertEquals(1, crset.getInt(1));
        assertTrue(crset.absolute(4));
        assertEquals(4, crset.getInt(1));
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));

        // when position the cursor beyond the first/last row in the result set
        assertFalse(crset.absolute(10));
        assertTrue(crset.isAfterLast());
        assertTrue(crset.previous());
        assertFalse(crset.absolute(-10));
        assertTrue(crset.isBeforeFirst());
        assertTrue(crset.next());

        /*
         * TODO when the given row number is negative, spec says absolute(-1)
         * equals last(). However, the return value of absolute(negative) is
         * false when run on RI. The Harmony follows the spec.
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(crset.absolute(-1));
            assertEquals(4, crset.getInt(1));
            assertTrue(crset.absolute(-3));
            assertEquals(2, crset.getInt(1));
            assertFalse(crset.absolute(-5));
        } else {
            assertFalse(crset.absolute(-1));
            assertEquals(0, crset.getRow());
            assertTrue(crset.isBeforeFirst());
        }

        crset.moveToInsertRow();
        try {
            crset.first();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        crset.updateInt(1, 60);
        crset.updateString(2, "abc");
        crset.insertRow();
        try {
            crset.absolute(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        crset.moveToCurrentRow();
        assertTrue(crset.absolute(2));

        crset.setType(ResultSet.TYPE_FORWARD_ONLY);
        try {
            crset.absolute(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.absolute(-1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    public void testNextAndPrevious() throws Exception {
        /*
         * This method is also used to test isBeforeFirst(), isAfterLast(),
         * isFirst(),isLast()
         */
        // Test for next()
        assertTrue(crset.isBeforeFirst());
        assertFalse(crset.isAfterLast());
        assertFalse(crset.isFirst());
        assertFalse(crset.isLast());
        assertTrue(crset.next());
        assertTrue(crset.isFirst());
        assertEquals(1, crset.getInt(1));

        assertTrue(crset.next());
        assertFalse(crset.isFirst());
        assertTrue(crset.next());
        assertTrue(crset.next());
        assertTrue(crset.isLast());
        assertEquals(4, crset.getInt(1));
        assertFalse(crset.next());
        // assertFalse(crset.next());
        assertFalse(crset.isBeforeFirst());
        assertTrue(crset.isAfterLast());

        // Test for previous()
        assertTrue(crset.previous());
        assertEquals(4, crset.getInt(1));
        assertTrue(crset.isLast());
        assertTrue(crset.previous());
        assertTrue(crset.previous());
        assertTrue(crset.previous());
        assertEquals(1, crset.getInt(1));
        assertTrue(crset.isFirst());
        assertFalse(crset.previous());
        assertTrue(crset.isBeforeFirst());
        // assertFalse(crset.previous());

        assertTrue(crset.next());
        assertTrue(crset.next());
        assertEquals(2, crset.getInt(1));

        // Test for previous()'s Exception
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, crset.getType());
        crset.setType(ResultSet.TYPE_FORWARD_ONLY);
        assertEquals(ResultSet.TYPE_FORWARD_ONLY, crset.getType());
        try {
            crset.previous();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    public void testFirstAndLast() throws Exception {
        /*
         * This method is used to test afterLast(), beforeFist(), first(),
         * last()
         */
        assertTrue(crset.isBeforeFirst());
        assertTrue(crset.first());
        assertTrue(crset.isFirst());
        assertFalse(crset.isBeforeFirst());
        crset.beforeFirst();
        assertTrue(crset.isBeforeFirst());
        assertTrue(crset.last());
        assertTrue(crset.isLast());

        assertTrue(crset.first());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, crset.getType());
        crset.setType(ResultSet.TYPE_FORWARD_ONLY);
        assertEquals(ResultSet.TYPE_FORWARD_ONLY, crset.getType());

        try {
            crset.beforeFirst();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.first();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.last();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        assertTrue(crset.isFirst());
    }

    public void testAcceptChanges_Insert() throws Exception {
        /*
         * Insert a new row one time
         */
        crset.moveToInsertRow();
        crset.updateInt(1, 5);
        crset.updateString(2, "test5");
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
        // check the new row in CachedRowSet
        crset.beforeFirst();
        String newRowValue = "";
        while (crset.next()) {
            if (crset.getInt(1) == 5) {
                newRowValue = "test5";
            }
        }
        assertEquals("test5", newRowValue);
        // check the new row in DB
        rs = st.executeQuery("select * from USER_INFO where ID = 5");
        assertTrue(rs.next());
        assertEquals(5, rs.getInt(1));

        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.populate(rs);
        noInitialCrset.moveToInsertRow();
        for (int i = 6; i <= 20; i++) {
            noInitialCrset.updateInt(1, i);
            noInitialCrset.updateString(2, "test" + i);
            noInitialCrset.insertRow();
        }
        noInitialCrset.moveToCurrentRow();
        noInitialCrset.setTableName("USER_INFO");
        noInitialCrset.acceptChanges(conn);
        // check the new rows in CachedRowSet
        assertEquals(20, noInitialCrset.size());
        // check the new rows in DB
        rs = st.executeQuery("select * from USER_INFO");
        int cursorIndex = 0;
        while (rs.next()) {
            cursorIndex++;
        }
        assertEquals(20, cursorIndex);
    }

    public void testAcceptChanges_InsertException() throws Exception {
        /*
         * Insert a new row. One given column's value exceeds the max range.
         * Therefore, it should throw SyncProviderException.
         */
        crset.moveToInsertRow();
        crset.updateInt(1, 4);
        crset.updateString(2, "test5");
        crset.updateLong(3, 555555L);
        crset.updateInt(4, 200000); // 200000 exceeds the NUMERIC's range
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateFloat(8, 4.888F);
        crset.insertRow();
        crset.moveToCurrentRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            assertTrue(resolver.nextConflict());
            assertEquals(1, resolver.getRow());

            assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                // all values are null
                assertNull(resolver.getConflictValue(i));
            }

            assertFalse(resolver.nextConflict());
        }

        /*
         * Insert a new row. The new row's primary key has existed. Therefore,
         * it should throw SyncProviderException.
         */
        crset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        crset.populate(rs);
        crset.moveToInsertRow();
        crset.updateInt(1, 4); // The ID valued 4 has existed in db.
        crset.updateString(2, "test5");
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            assertTrue(resolver.nextConflict());
            assertEquals(1, resolver.getRow());
            assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                // all values are null
                assertNull(resolver.getConflictValue(i));
            }

            assertFalse(resolver.nextConflict());
        }

        /*
         * Insert a new row. Before inserting the new row, another new row which
         * has the same data is inserted into the DB. However, the current
         * CachedRowSet doesn't know it. In this situation, it should throw
         * SyncProviderException.
         */
        crset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        crset.populate(rs);
        String insertSQL = "INSERT INTO USER_INFO(ID, NAME, BIGINT_T, NUMERIC_T,DECIMAL_T, SMALLINT_T, "
                + "FLOAT_T, REAL_T, DOUBLE_T, DATE_T, TIME_T, TIMESTAMP_T) VALUES(?, ?, ?, ?, ?, ?,"
                + "?, ?, ?, ?, ?, ? )";
        PreparedStatement preStmt = conn.prepareStatement(insertSQL);
        preStmt.setInt(1, 80);
        preStmt.setString(2, "test" + 80);
        preStmt.setLong(3, 444423L);
        preStmt.setBigDecimal(4, new BigDecimal(12));
        preStmt.setBigDecimal(5, new BigDecimal(23));
        preStmt.setInt(6, 41);
        preStmt.setFloat(7, 4.8F);
        preStmt.setFloat(8, 4.888F);
        preStmt.setDouble(9, 4.9999);
        preStmt.setDate(10, new Date(965324512));
        preStmt.setTime(11, new Time(452368512));
        preStmt.setTimestamp(12, new Timestamp(874532105));
        preStmt.executeUpdate();
        if (preStmt != null) {
            preStmt.close();
        }
        // check the new row in DB
        rs = st.executeQuery("select * from USER_INFO where ID = 80");
        assertTrue(rs.next());
        assertEquals(80, rs.getInt(1));
        assertEquals("test80", rs.getString(2));

        // now call CachedRowSet.insertRow()
        crset.moveToInsertRow();
        crset.updateInt(1, 80);
        crset.updateString(2, "test" + 80);
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
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            assertTrue(resolver.nextConflict());
            assertEquals(1, resolver.getRow());

            assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                // all values are null
                assertNull(resolver.getConflictValue(i));
            }

            assertFalse(resolver.nextConflict());
        }
    }

    public void testAcceptChanges_Delete() throws Exception {
        /*
         * Delete all the row. On the first and second row, only two columns
         * have value, all the others are NULL. When run on RI, deleteRow() will
         * go wrong and throw Exception. According to the spec, deleteRow() is
         * supposed to ok.
         */
        crset.beforeFirst();
        while (crset.next()) {
            crset.deleteRow();
        }

        crset.acceptChanges(conn);
        // check DB
        rs = st.executeQuery("select count(*) from USER_INFO");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
    }

    public void testAcceptChanges_DeleteException() throws Exception {
        /*
         * Delete a row which has been deleted from database
         */
        int result = st.executeUpdate("delete from USER_INFO where ID = 3");
        assertEquals(1, result);
        // move to the third row which doesn't exist in database
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        crset.deleteRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            assertTrue(resolver.nextConflict());
            assertEquals(3, resolver.getRow());

            assertEquals(SyncResolver.DELETE_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                // all values are null
                assertNull(resolver.getConflictValue(i));
            }

            assertFalse(resolver.nextConflict());
        }

        /*
         * Delete a row which has been updated in database
         */
        crset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        crset.populate(rs);
        result = st
                .executeUpdate("update USER_INFO set NAME = 'update44' where ID = 4");
        assertEquals(1, result);
        // move to the updated row
        crset.absolute(3);
        assertEquals(4, crset.getInt(1));
        assertEquals("test4", crset.getString(2));
        crset.deleteRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            // TODO resolver doesn't contian conflict in RI, maybe RI's bug
            if ("true".equals(System.getProperty("Testing Harmony"))) {
                assertTrue(resolver.nextConflict());

                for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                    // all values are null
                    assertNull(resolver.getConflictValue(i));
                }

            } else {
                assertFalse(resolver.nextConflict());
            }
        }
    }

    public void testAcceptChanges_Update() throws Exception {
        // update the first row
        assertTrue(crset.absolute(1));
        crset.updateInt(1, 11);
        crset.updateString(2, "test11");
        crset.updateRow();
        crset.acceptChanges(conn);
        // check DB
        rs = st.executeQuery("select * from USER_INFO where ID = 11");
        assertTrue(rs.next());
        assertEquals(11, rs.getInt(1));
        assertEquals("test11", rs.getString(2));

        // update the third row
        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.populate(rs);
        assertTrue(noInitialCrset.absolute(1));
        noInitialCrset.updateInt(1, 111);
        noInitialCrset.updateString(2, "update111");
        noInitialCrset.updateRow();
        assertTrue(noInitialCrset.absolute(3));
        noInitialCrset.updateInt(1, 333);
        noInitialCrset.updateString(2, "update333");
        noInitialCrset.updateLong(3, 33333L);
        noInitialCrset.updateRow();
        noInitialCrset.acceptChanges(conn);
        // check DB
        rs = st.executeQuery("select * from USER_INFO where ID = 111");
        assertTrue(rs.next());
        assertEquals(111, rs.getInt(1));
        assertEquals("update111", rs.getString(2));
        rs = st.executeQuery("select * from USER_INFO where ID = 333");
        assertTrue(rs.next());
        assertEquals(333, rs.getInt(1));
        assertEquals("update333", rs.getString(2));
        assertEquals(33333L, rs.getLong(3));
    }

    public void testAcceptChanges_UpdateException() throws Exception {
        /*
         * Update a row which has been deleted from database
         */
        int result = st.executeUpdate("delete from USER_INFO where ID = 3");
        assertEquals(1, result);
        // move to the third row which doesn't exist in database
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        crset.updateString(2, "update33");
        crset.updateRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            assertTrue(resolver.nextConflict());
            assertEquals(3, resolver.getRow());

            assertEquals(SyncResolver.UPDATE_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                // all values are null
                assertNull(resolver.getConflictValue(i));
            }

            assertFalse(resolver.nextConflict());
        }

        /*
         * Update a row which has been updated in database
         */
        crset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        crset.populate(rs);
        result = st
                .executeUpdate("update USER_INFO set NAME = 'update44' where ID = 4");
        assertEquals(1, result);
        // move to the updated row
        assertTrue(crset.absolute(3));
        assertEquals(4, crset.getInt(1));
        assertEquals("test4", crset.getString(2));
        crset.updateString(2, "change4");
        crset.updateRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            assertTrue(resolver.nextConflict());
            assertEquals(3, resolver.getRow());

            assertEquals(SyncResolver.UPDATE_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                try {
                    resolver.getConflictValue(i);
                } catch (SQLException ex) {
                    // TODO RI throw SQLException here, maybe RI's bug
                }
            }

            assertFalse(resolver.nextConflict());
        }

        /*
         * Update a row in which one column's value is out of range
         */
        crset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");
        crset.populate(rs);
        assertEquals(3, crset.size());
        assertTrue(crset.absolute(3));
        assertEquals(4, crset.getInt(1));
        crset.updateString(2, "update4");
        crset.updateLong(3, 555555L);
        crset.updateInt(4, 200000); // 200000 exceeds the NUMERIC's range
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateFloat(8, 4.888F);
        crset.updateRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            assertTrue(resolver.nextConflict());
            assertEquals(3, resolver.getRow());

            assertEquals(SyncResolver.UPDATE_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                // all values are null
                assertNull(resolver.getConflictValue(i));
            }

            assertFalse(resolver.nextConflict());
        }
    }

    public void testAcceptChanges_MultiConflicts() throws Exception {
        /*
         * Update a row in which one column's value is out of range
         */
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        crset.updateString(2, "update4");
        crset.updateLong(3, 555555L);
        crset.updateInt(4, 200000); // 200000 exceeds the NUMERIC's range
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateFloat(8, 4.888F);
        crset.updateRow();

        /*
         * Delete a row which has been deleted from database
         */
        int result = st.executeUpdate("delete from USER_INFO where ID = 1");
        assertEquals(1, result);
        // move to the first row which doesn't exist in database
        assertTrue(crset.absolute(1));
        assertEquals(1, crset.getInt(1));
        crset.deleteRow();

        /*
         * Insert a new row. One given column's value exceeds the max range.
         * Therefore, it should throw SyncProviderException.
         */
        assertTrue(crset.last());
        crset.moveToInsertRow();
        crset.updateInt(1, 5);
        crset.updateString(2, "test5");
        crset.updateLong(3, 555555L);
        crset.updateInt(4, 200000); // 200000 exceeds the NUMERIC's range
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateFloat(8, 4.888F);
        crset.insertRow();
        crset.moveToCurrentRow();

        try {
            crset.acceptChanges(conn);
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            HashMap<Integer, Integer> conflicts = new HashMap<Integer, Integer>();
            conflicts.put(SyncResolver.INSERT_ROW_CONFLICT, 5);
            conflicts.put(SyncResolver.UPDATE_ROW_CONFLICT, 3);
            conflicts.put(SyncResolver.DELETE_ROW_CONFLICT, 1);

            assertTrue(resolver.nextConflict());

            assertTrue(conflicts.containsKey(resolver.getStatus()));
            assertEquals(((Integer) conflicts.get(resolver.getStatus()))
                    .intValue(), resolver.getRow());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                // all values are null
                assertNull(resolver.getConflictValue(i));
            }

            assertTrue(resolver.nextConflict());

            assertTrue(conflicts.containsKey(resolver.getStatus()));
            assertEquals(((Integer) conflicts.get(resolver.getStatus()))
                    .intValue(), resolver.getRow());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                // all values are null
                assertNull(resolver.getConflictValue(i));
            }

            // TODO RI only contains two conflicts, maybe RI's bug
            if ("true".equals(System.getProperty("Testing Harmony"))) {
                assertTrue(resolver.nextConflict());

                assertTrue(conflicts.containsKey(resolver.getStatus()));
                assertEquals(((Integer) conflicts.get(resolver.getStatus()))
                        .intValue(), resolver.getRow());

                for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                    // all values are null
                    assertNull(resolver.getConflictValue(i));
                }

                assertFalse(resolver.nextConflict());

            } else {
                assertFalse(resolver.nextConflict());
            }

        }
    }

    public void testFindColumn() throws SQLException {
        try {
            noInitialCrset.findColumn(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            noInitialCrset.findColumn("ID");
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            noInitialCrset.findColumn("not exist name");
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            crset.findColumn("not exist name");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid column name
        }

        try {
            crset.findColumn(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        assertEquals(1, crset.findColumn("ID"));

        assertEquals(1, crset.findColumn("id"));

        assertEquals(7, crset.findColumn("FLOAT_T"));

        assertEquals(7, crset.findColumn("FloaT_T"));
    }

    public void testRestoreOriginal() throws Exception {
        // update
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        assertEquals("test3", crset.getString(2));

        crset.updateString(2, "update3");
        assertEquals("update3", crset.getString(2));

        crset.updateRow();
        crset.restoreOriginal();

        /*
         * TODO seems RI put the cursor out of rowset after restoreOriginal, RI'
         * bug
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(crset.isFirst());
            assertEquals(1, crset.getRow());
            assertTrue(crset.absolute(3));
        } else {
            assertEquals(0, crset.getRow());
            crset.beforeFirst();
            assertTrue(crset.next());
            assertTrue(crset.next());
            assertTrue(crset.next());
        }

        assertEquals(3, crset.getInt(1));
        assertEquals("test3", crset.getString(2));

        // insert
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);
        crset.next();

        crset.moveToInsertRow();
        crset.updateInt(1, 5);
        crset.updateString(2, "test5");
        crset.updateLong(3, 555555L);
        crset.updateInt(4, 200); // 200000 exceeds the NUMERIC's range
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateFloat(8, 4.888F);
        crset.insertRow();

        crset.restoreOriginal();

        /*
         * TODO seems RI put the cursor out of rowset after restoreOriginal, RI'
         * bug
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(crset.isFirst());
            assertEquals(1, crset.getRow());

            int count = 0;
            crset.beforeFirst();
            while (crset.next()) {
                count++;
            }
            assertEquals(4, count);
        } else {
            assertEquals(0, crset.getRow());

            // assertTrue(crset.next()); throws SQLException
            crset.beforeFirst();
            assertTrue(crset.isBeforeFirst());
            // assertTrue(crset.next()); throws SQLException
            /*
             * I can't move cursor to a valid position, so can't do any check
             */
        }

        // delete
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        crset.absolute(3);
        crset.deleteRow();

        crset.restoreOriginal();

        /*
         * TODO seems RI put the cursor out of rowset after restoreOriginal, RI'
         * bug
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(crset.isFirst());
            assertEquals(1, crset.getRow());
            assertTrue(crset.absolute(3));
            assertEquals(3, crset.getInt(1));
        } else {
            assertEquals(0, crset.getRow());
            crset.beforeFirst();
            assertTrue(crset.absolute(3));
            assertEquals(3, crset.getInt(1));
        }

        crset = newNoInitialInstance();

        crset.restoreOriginal();

        // invoke restoreOriginal cursor out of rowset
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        assertEquals("test3", crset.getString(2));

        crset.updateString(2, "update3");
        assertEquals("update3", crset.getString(2));

        crset.updateRow();

        crset.beforeFirst();

        crset.restoreOriginal();

        /*
         * TODO seems RI put the cursor out of rowset after restoreOriginal, RI'
         * bug
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(crset.isFirst());
            assertEquals(1, crset.getRow());
            assertTrue(crset.absolute(3));
        } else {
            assertEquals(0, crset.getRow());
            crset.beforeFirst();
            assertTrue(crset.next());
            assertTrue(crset.next());
            assertTrue(crset.next());
        }

        assertEquals(3, crset.getInt(1));
        assertEquals("test3", crset.getString(2));
    }

    public void testRestoreOriginal_MultiChanges() throws Exception {
        insertMoreData(5);

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);

        // row 3 not call updateRow
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        assertEquals("test3", crset.getString(2));

        crset.updateString(2, "update3");
        assertEquals("update3", crset.getString(2));

        // row 4 call updateRow
        assertTrue(crset.next());
        assertEquals(4, crset.getInt(1));
        assertEquals("test4", crset.getString(2));

        crset.updateString(2, "update4");
        assertEquals("update4", crset.getString(2));
        crset.updateRow();

        // delete row 5
        assertTrue(crset.next());
        assertEquals(5, crset.getInt(1));
        crset.deleteRow();

        crset.restoreOriginal();

        /*
         * TODO seems RI put the cursor out of rowset after restoreOriginal, RI'
         * bug
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(crset.isFirst());
            assertEquals(1, crset.getRow());
        } else {
            assertEquals(0, crset.getRow());
            crset.beforeFirst();
        }

        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        assertEquals("update3", crset.getString(2));

        assertTrue(crset.next());
        assertEquals("test4", crset.getString(2));

        assertTrue(crset.next());
        assertEquals(5, crset.getInt(1));
    }

    public void testRefreshRow() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.refreshRow();
            fail("should throw exception");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw ArrayIndexOutOfBoundsException
        } catch (SQLException e) {
            // expected
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        assertTrue(noInitialCrset.isBeforeFirst());
        try {
            noInitialCrset.refreshRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected: Invalid cursor position
        }

        // when the cursor is on the insert row
        noInitialCrset.moveToInsertRow();
        try {
            noInitialCrset.refreshRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected: Invalid cursor position
        }

        // move the cursor to the third row
        noInitialCrset.moveToCurrentRow();
        assertTrue(noInitialCrset.absolute(3));
        // no effect
        noInitialCrset.refreshRow();

        /*
         * Update the third row in database. Then call refreshRow().
         */
        int result = st
                .executeUpdate("UPDATE USER_INFO SET NAME = 'update33' WHERE ID = 3");
        assertEquals(1, result);

        // still no effect.
        noInitialCrset.refreshRow();
        assertEquals(3, noInitialCrset.getInt(1));
        assertEquals("test3", noInitialCrset.getString(2));

        noInitialCrset.updateString(2, "update33");
        noInitialCrset.refreshRow();
        assertEquals("test3", noInitialCrset.getString(2));

        noInitialCrset.updateString(2, "update33");
        noInitialCrset.updateRow();
        noInitialCrset.beforeFirst();
        assertTrue(noInitialCrset.absolute(3));
        assertEquals("update33", noInitialCrset.getString(2));
        assertTrue(noInitialCrset.rowUpdated());
        noInitialCrset.refreshRow();
        assertEquals("test3", noInitialCrset.getString(2));
        assertFalse(noInitialCrset.rowUpdated());
    }

    public void testToCollection() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.toCollection();
            fail("should throw exception");
        } catch (NullPointerException e) {
            // RI throw NullPointerException
        } catch (SQLException e) {
            // expected
        }

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            rs = st.executeQuery("select * from USER_INFO");
            noInitialCrset.populate(rs);
            rs = st.executeQuery("select * from USER_INFO");

            Collection<?> collection = noInitialCrset.toCollection();
            Iterator iter = collection.iterator();
            while (iter.hasNext()) {
                Vector vector = (Vector) iter.next();
                assertTrue(rs.next());
                for (int i = 1; i <= DEFAULT_COLUMN_COUNT; i++) {
                    assertEquals(rs.getObject(i), vector.get(i - 1));
                }
            }
        } else {
            rs = st.executeQuery("select * from USER_INFO");
            noInitialCrset.populate(rs);

            Collection<?> collection = noInitialCrset.toCollection();
            assertEquals("class java.util.TreeMap$Values", collection.getClass()
                    .toString());
            Iterator iter = collection.iterator();
            assertTrue(iter.hasNext());
            assertEquals("class com.sun.rowset.internal.Row", iter.next()
                    .getClass().toString());
        }
    }

    public void testToCollectionInt() throws Exception {
        noInitialCrset = newNoInitialInstance();
        assertEquals(0, noInitialCrset.toCollection(-1).size());
        assertEquals(0, noInitialCrset.toCollection(0).size());
        assertEquals(Vector.class, noInitialCrset.toCollection(1).getClass());

        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.populate(rs);

        try {
            noInitialCrset.toCollection(0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            noInitialCrset.toCollection(DEFAULT_COLUMN_COUNT + 1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        Vector vector = (Vector) noInitialCrset.toCollection(1);
        Iterator iter = vector.iterator();
        int index = 0;
        while (iter.hasNext()) {
            index++;
            assertEquals(index, iter.next());
        }
        assertEquals(4, index);

        vector = (Vector) noInitialCrset.toCollection(3);
        iter = vector.iterator();
        index = 0;
        while (iter.hasNext()) {
            index++;
            if (index == 1 || index == 2) {
                assertNull(iter.next());
            } else if (index == 3) {
                assertEquals("3333", iter.next().toString());
            } else if (index == 4) {
                assertEquals("444423", iter.next().toString());
            }
        }
        assertEquals(4, index);
    }

    public void testToCollectionString() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            assertEquals(0, noInitialCrset.toCollection("ID"));
            fail("should throw exception");
        } catch (NullPointerException e) {
            // expected
        }

        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.populate(rs);

        try {
            noInitialCrset.toCollection("valid");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        Vector vector = (Vector) noInitialCrset.toCollection("ID");
        Iterator iter = vector.iterator();
        int index = 0;
        while (iter.hasNext()) {
            index++;
            assertEquals(index, iter.next());
        }
        assertEquals(4, index);

        vector = (Vector) noInitialCrset.toCollection("BIGINT_T");
        iter = vector.iterator();
        index = 0;
        while (iter.hasNext()) {
            index++;
            if (index == 1 || index == 2) {
                assertNull(iter.next());
            } else if (index == 3) {
                assertEquals("3333", iter.next().toString());
            } else if (index == 4) {
                assertEquals("444423", iter.next().toString());
            }
        }
        assertEquals(4, index);
    }

    public void testGetCursorName() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.getCursorName();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        rs = st.executeQuery("select * from USER_INFO");
        assertTrue(rs.next());
        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.populate(rs);

        try {
            noInitialCrset.getCursorName();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        while (noInitialCrset.next()) {
            try {
                noInitialCrset.getCursorName();
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        }
    }

    public void testGetStatement() throws Exception {
        noInitialCrset = newNoInitialInstance();
        assertNull(noInitialCrset.getStatement());

        rs = st.executeQuery("select * from USER_INFO");
        assertNotNull(rs.getStatement());
        noInitialCrset.populate(rs);
        assertNull(noInitialCrset.getStatement());

        noInitialCrset.setUrl(DERBY_URL);
        assertNull(noInitialCrset.getStatement());
    }

    public void testWasNull() throws Exception {
        noInitialCrset = newNoInitialInstance();
        assertFalse(noInitialCrset.wasNull());

        rs = st.executeQuery("select * from USER_INFO");
        noInitialCrset.populate(rs);
        assertFalse(noInitialCrset.wasNull());

        assertTrue(noInitialCrset.next());
        assertFalse(noInitialCrset.wasNull());
        assertNull(noInitialCrset.getObject(3));
        assertTrue(noInitialCrset.wasNull());

        assertNotNull(noInitialCrset.getObject(1));
        assertFalse(noInitialCrset.wasNull());

        assertNull(noInitialCrset.getObject(5));
        assertTrue(noInitialCrset.wasNull());

        assertTrue(noInitialCrset.absolute(3));
        noInitialCrset.updateString(2, "x");
        assertTrue(noInitialCrset.wasNull());

        assertTrue(noInitialCrset.first());
        for (int i = 1; i <= DEFAULT_COLUMN_COUNT; i++) {
            if (noInitialCrset.getObject(i) == null) {
                assertTrue(noInitialCrset.wasNull());
            } else {
                assertFalse(noInitialCrset.wasNull());
            }
        }
    }

    public void testGetKeyColumns() throws Exception {
        int[] columns = null;
        /*
         * TODO spec says SQLException should be thrown when CachedRowSet object
         * is empty, while RI return null, Harmony follow spec
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                columns = noInitialCrset.getKeyColumns();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected
            }

        } else {
            columns = noInitialCrset.getKeyColumns();
            assertNull(columns);
        }

        columns = crset.getKeyColumns();

        /*
         * TODO spec says empty array should be return when on key columns is
         * setted, while RI return null, Harmony follow spec
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertNotNull(columns);
            assertEquals(0, columns.length);
        } else {
            assertNull(columns);
        }

    }

    public void testSetKeyColumns() throws Exception {
        try {
            noInitialCrset.setKeyColumns(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        int[] columns = null;

        noInitialCrset.setKeyColumns(new int[0]);
        noInitialCrset.setKeyColumns(new int[] { 1, 100 });

        rs = st.executeQuery("select * from user_info");

        noInitialCrset.populate(rs);

        /*
         * populate doesn't initial keyColumns, it's not reasonable, but we
         * follow RI here
         */
        columns = noInitialCrset.getKeyColumns();
        assertNotNull(columns);
        assertEquals(2, columns.length);
        assertEquals(1, columns[0]);
        assertEquals(100, columns[1]);

        try {
            crset.setKeyColumns(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        crset.setKeyColumns(new int[0]);
        columns = crset.getKeyColumns();
        assertNotNull(columns);
        assertEquals(0, columns.length);

        try {
            crset.setKeyColumns(new int[] { 1, 100 });
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid column index 100
        }

        crset.setKeyColumns(new int[] { 1, 2 });
        columns = crset.getKeyColumns();
        assertNotNull(columns);
        assertEquals(2, columns.length);
        assertEquals(1, columns[0]);
        assertEquals(2, columns[1]);

        try {
            crset.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        crset.setMatchColumn(3);
        int[] matchColumns = crset.getMatchColumnIndexes();
        assertEquals(3, matchColumns[0]);
        for (int i = 1; i < 10; ++i) {
            assertEquals(-1, matchColumns[i]);
        }

        crset.setKeyColumns(new int[] { 4, 5 });
        columns = crset.getKeyColumns();
        assertNotNull(columns);
        assertEquals(2, columns.length);
        assertEquals(4, columns[0]);
        assertEquals(5, columns[1]);

        // setKeyColumns doesn't effect match columns
        matchColumns = crset.getMatchColumnIndexes();
        assertEquals(3, matchColumns[0]);
        for (int i = 1; i < 10; ++i) {
            assertEquals(-1, matchColumns[i]);
        }

        int[] args = new int[] { 6, 7 };
        crset.setKeyColumns(args);
        columns = crset.getKeyColumns();
        assertNotNull(columns);
        assertEquals(2, columns.length);
        assertEquals(6, columns[0]);
        assertEquals(7, columns[1]);

        assertNotSame(args, columns);

        // getKeyColumns doesn't do clone
        assertSame(crset.getKeyColumns(), crset.getKeyColumns());
        columns = crset.getKeyColumns();
        assertEquals(6, columns[0]);
        assertEquals(7, columns[1]);

        columns[1] = 8;

        args = crset.getKeyColumns();
        assertEquals(8, args[1]);

    }

    public void testProvider() throws Exception {
        SyncProvider provider = null;
        if (System.getProperty("Testing Harmony") == "true") {
            provider = SyncFactory
                    .getInstance("Apache Harmony HYOptimisticProvider");
        } else {
            provider = SyncFactory
                    .getInstance("com.sun.rowset.providers.RIOptimisticProvider");
        }

        assertEquals(SyncProvider.GRADE_CHECK_MODIFIED_AT_COMMIT, provider
                .getProviderGrade());
        assertEquals(SyncProvider.DATASOURCE_NO_LOCK, provider
                .getDataSourceLock());
        assertEquals(SyncProvider.NONUPDATABLE_VIEW_SYNC, provider
                .supportsUpdatableView());

        try {
            provider.setDataSourceLock(SyncProvider.DATASOURCE_TABLE_LOCK);
            fail("Should throw SyncProviderException");
        } catch (SyncProviderException e) {
            // expected
        }

        try {
            provider.setDataSourceLock(SyncProvider.DATASOURCE_ROW_LOCK);
            fail("Should throw SyncProviderException");
        } catch (SyncProviderException e) {
            // expected
        }

        try {
            provider.setDataSourceLock(SyncProvider.DATASOURCE_DB_LOCK);
            fail("Should throw SyncProviderException");
        } catch (SyncProviderException e) {
            // expected
        }

        provider.setDataSourceLock(SyncProvider.DATASOURCE_NO_LOCK);
    }

    public void testGetConnection() throws Exception {
        RowSetInternal rowset = (RowSetInternal) noInitialCrset;
        assertNull(rowset.getConnection());

        noInitialCrset.setUsername("test");
        noInitialCrset.setPassword("pwd");

        assertNull(noInitialCrset.getUrl());
        assertNull(noInitialCrset.getDataSourceName());
        assertNull(rowset.getConnection());

        noInitialCrset.setUrl(DERBY_URL);
        assertNull(noInitialCrset.getDataSourceName());
        assertNull(rowset.getConnection());

        noInitialCrset.setUsername(null);
        noInitialCrset.setPassword(null);
        noInitialCrset.setUrl(DERBY_URL);
        assertNull(noInitialCrset.getDataSourceName());
        assertNull(rowset.getConnection());

        // test acceptChange
        crset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");

        crset.populate(rs);

        rowset = (RowSetInternal) crset;

        assertNull(crset.getUsername());
        assertNull(crset.getPassword());
        assertNull(crset.getUrl());
        assertNull(crset.getDataSourceName());
        assertNull(rowset.getConnection());

        crset.setUrl(DERBY_URL);
        assertNull(crset.getUsername());
        assertNull(crset.getPassword());
        assertEquals(DERBY_URL, crset.getUrl());
        assertNull(rowset.getConnection());

        crset.absolute(3);
        crset.updateString(2, "update2");
        assertEquals(DERBY_URL, crset.getUrl());
        crset.acceptChanges();
        assertNull(rowset.getConnection());

        crset.acceptChanges(conn);

        assertNotNull(rowset.getConnection());
        assertSame(conn, rowset.getConnection());

        crset.acceptChanges();
        assertSame(conn, rowset.getConnection());

        Connection connection = DriverManager.getConnection(DERBY_URL);
        crset.acceptChanges(connection);
        assertSame(connection, rowset.getConnection());

    }

    public void testRetrieveConnection() throws Exception {
        crset = newNoInitialInstance();
        rs = st.executeQuery("select * from USER_INFO");

        crset.populate(rs);

        assertNull(crset.getUrl());
        assertNull(crset.getDataSourceName());
        try {
            crset.acceptChanges();
            fail("Should throw SyncProviderException");
        } catch (SyncProviderException e) {
            // expected, Unable to get connection
        }

        // wrong user and password
        crset.setUsername("testusername");
        crset.setPassword("testpassword");
        crset.setUrl(DERBY_URL);
        crset.absolute(3);
        crset.updateString(2, "update3");
        crset.updateRow();

        crset.acceptChanges();

        crset.setDataSourceName("testDataSource");

        try {
            crset.acceptChanges();
            fail("Should throw SyncProviderException");
        } catch (SyncProviderException e) {
            // expected, (JNDI)Unable to get connection
        }
    }

    public void testSerializable() throws Exception {

        crset.absolute(3);
        crset.updateString(2, "update3");

        assertEquals(3, crset.getRow());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(crset);

        out.close();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
                bout.toByteArray()));

        CachedRowSet another = (CachedRowSet) in.readObject();
        in.close();

        isMetaDataEquals(crset.getMetaData(), another.getMetaData());

        assertEquals(crset.getRow(), another.getRow());
        assertEquals(crset.getString(2), another.getString(2));

        crset = newNoInitialInstance();
        crset.setCommand("SELECT * FROM USER_INFO");
        crset.setUrl(DERBY_URL);
        crset.execute();

        crset.absolute(3);

        bout = new ByteArrayOutputStream();
        out = new ObjectOutputStream(bout);
        out.writeObject(crset);

        out.close();

        in = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));

        another = (CachedRowSet) in.readObject();

        isMetaDataEquals(crset.getMetaData(), another.getMetaData());

        assertEquals(crset.getRow(), another.getRow());

        try {
            another.commit();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetOriginal() throws Exception {
        crset = newNoInitialInstance();
        try {
            crset.getOriginal();
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

}

class Listener implements RowSetListener, Cloneable {

    private String tag = null;

    private boolean isPrint = false;

    private Object eventSource = null;

    public void cursorMoved(RowSetEvent theEvent) {
        if (isPrint) {
            System.out.println("cursorMoved");
        }
        tag = CachedRowSetListenerTest.EVENT_CURSOR_MOVED;
    }

    public void rowChanged(RowSetEvent theEvent) {
        if (isPrint) {
            System.out.println("rowChanged");
        }
        tag = CachedRowSetListenerTest.EVENT_ROW_CHANGED;
    }

    public void rowSetChanged(RowSetEvent theEvent) {
        if (isPrint) {
            System.out.println("rowSetChanged");
        }
        tag = CachedRowSetListenerTest.EVENT_ROWSET_CHANGED;
        eventSource = theEvent.getSource();
    }

    public String getTag() {
        return tag;
    }

    public Object getEventSource() {
        return eventSource;
    }

    public void clear() {
        tag = null;
        eventSource = null;
    }

    public void setPrint(boolean isPrint) {
        this.isPrint = isPrint;
    }

    public Listener clone() throws CloneNotSupportedException {
        Listener listener = (Listener) super.clone();
        return listener;
    }
}
