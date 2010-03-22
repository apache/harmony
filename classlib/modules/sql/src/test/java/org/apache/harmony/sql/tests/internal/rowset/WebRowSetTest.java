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

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.WebRowSet;
import javax.sql.rowset.spi.SyncProviderException;

public class WebRowSetTest extends CachedRowSetTestCase {
    protected WebRowSet webRs;

    protected WebRowSet another;

    public void testCreateCopy() throws Exception {
        webRs = newWebRowSet();
        webRs.setUrl(DERBY_URL);
        webRs.setCommand("SELECT * FROM USER_INFO WHERE ID = ?");
        webRs.setInt(1, 3);
        webRs.execute();

        CachedRowSet copyWebRs = webRs.createCopy();
        assertTrue(copyWebRs instanceof WebRowSet);
        assertEquals("SELECT * FROM USER_INFO WHERE ID = ?", copyWebRs
                .getCommand());
        assertEquals(DERBY_URL, copyWebRs.getUrl());
    }

    public void testCreateCopy2() throws Exception {
        webRs = newWebRowSet();
        webRs.setUrl(DERBY_URL);
        webRs.setCommand("SELECT * FROM USER_INFO WHERE ID = ? AND NAME = ?");
        webRs.setInt(1, 3);
        webRs.setString(2, "test3");
        webRs.execute();
        // check data
        assertTrue(webRs.next());
        assertEquals(3, webRs.getInt(1));
        assertFalse(webRs.next());

        // deep copy
        CachedRowSet copyWebRs = webRs.createCopy();
        copyWebRs.beforeFirst();
        assertTrue(copyWebRs.next());
        assertEquals(3, copyWebRs.getInt(1));
        assertFalse(copyWebRs.next());
        copyWebRs.execute();
        assertTrue(copyWebRs.next());
        assertEquals(3, copyWebRs.getInt(1));
        assertFalse(copyWebRs.next());

        webRs.setInt(1, 4);
        webRs.setString(2, "test4");
        webRs.execute();
        webRs.beforeFirst();
        assertTrue(webRs.next());
        assertEquals(4, webRs.getInt(1));
        assertFalse(webRs.next());

        copyWebRs.beforeFirst();
        assertTrue(copyWebRs.next());
        assertEquals(3, copyWebRs.getInt(1));
        assertFalse(copyWebRs.next());

        copyWebRs.execute();
        copyWebRs.beforeFirst();
        assertTrue(copyWebRs.next());
        assertEquals(3, copyWebRs.getInt(1));
        assertFalse(copyWebRs.next());

        copyWebRs.setInt(1, 1);
        copyWebRs.setString(2, "hermit");
        copyWebRs.execute();
        assertTrue(copyWebRs.next());
        assertEquals(1, copyWebRs.getInt(1));
        assertFalse(copyWebRs.next());

        webRs.beforeFirst();
        assertTrue(webRs.next());
        assertEquals(4, webRs.getInt(1));
        assertFalse(webRs.next());

        webRs.execute();
        webRs.beforeFirst();
        assertTrue(webRs.next());
        assertEquals(4, webRs.getInt(1));
        assertFalse(webRs.next());
    }

    public void testWriteAndRead() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs = newWebRowSet();
        webRs.populate(rs);

        StringWriter writer = new StringWriter();
        webRs.writeXml(writer);

        another = newWebRowSet();
        another.readXml(new StringReader(writer.getBuffer().toString()));

        assertCachedRowSetEquals(webRs, another);
    }

    public void testWriteAndRead_Insert() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs = newWebRowSet();
        webRs.populate(rs);

        assertTrue(webRs.next());
        webRs.moveToInsertRow();
        webRs.updateInt(1, 5);
        webRs.updateString(2, "insertrow");
        webRs.insertRow();
        webRs.moveToCurrentRow();
        webRs.beforeFirst();

        StringWriter writer = new StringWriter();
        webRs.writeXml(writer);

        another = newWebRowSet();
        another.readXml(new StringReader(writer.getBuffer().toString()));

        assertCachedRowSetEquals(webRs, another);
    }

    public void testWriteAndRead_Update() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs = newWebRowSet();
        webRs.populate(rs);

        assertTrue(webRs.absolute(3));
        webRs.updateString(2, "updateRow");
        webRs.updateRow();

        assertTrue(webRs.next());
        webRs.updateString(2, "anotherUpdateRow");
        webRs.updateRow();

        StringWriter writer = new StringWriter();

        webRs.writeXml(writer);

        another = newWebRowSet();
        another.readXml(new StringReader(writer.getBuffer().toString()));

        assertCachedRowSetEquals(webRs, another);

    }

    public void testWriteAndRead_Delete() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs = newWebRowSet();
        webRs.populate(rs);

        assertTrue(webRs.absolute(3));
        webRs.deleteRow();
        webRs.beforeFirst();

        StringWriter writer = new StringWriter();

        webRs.writeXml(writer);

        another = newWebRowSet();
        another.readXml(new StringReader(writer.getBuffer().toString()));

        webRs.setShowDeleted(true);
        another.setShowDeleted(true);

        assertCachedRowSetEquals(webRs, another);

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        webRs = newWebRowSet();
        webRs.populate(rs);

        assertTrue(webRs.absolute(4));
        // set show deleted to true
        webRs.setShowDeleted(true);
        webRs.deleteRow();
        webRs.absolute(3);
        webRs.deleteRow();

        writer = new StringWriter();
        webRs.writeXml(writer);

        another = newWebRowSet();
        another.readXml(new StringReader(writer.getBuffer().toString()));

        webRs.setShowDeleted(true);
        another.setShowDeleted(true);

        assertCachedRowSetEquals(webRs, another);

    }

    protected WebRowSet newWebRowSet() throws Exception {
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            return (WebRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.WebRowSetImpl")
                    .newInstance();
        }
        return (WebRowSet) Class.forName("com.sun.rowset.WebRowSetImpl")
                .newInstance();
    }

    protected void assertCachedRowSetEquals(CachedRowSet expected,
            CachedRowSet actual) throws Exception {
        isMetaDataEquals(expected.getMetaData(), actual.getMetaData());
        assertProperties(expected, actual);
        assertData(expected, actual);
    }

    private void assertData(CachedRowSet expected, CachedRowSet actual)
            throws SQLException {
        assertEquals(expected.size(), actual.size());
        expected.beforeFirst();
        actual.beforeFirst();

        int columnCount = expected.getMetaData().getColumnCount();
        while (expected.next()) {
            assertTrue(actual.next());
            // TODO RI's bug: read deleted row from xml would become current row
            if ("true".equals(System.getProperty("Testing Harmony"))) {
                assertEquals(expected.rowDeleted(), actual.rowDeleted());
            }
            assertEquals(expected.rowInserted(), actual.rowInserted());
            assertEquals(expected.rowUpdated(), actual.rowUpdated());
            for (int i = 1; i <= columnCount; ++i) {
                if (expected.getObject(i) == null) {
                    assertNull(actual.getObject(i));
                } else {
                    assertEquals(expected.getObject(i).hashCode(), actual
                            .getObject(i).hashCode());
                }
            }
        }
    }

    private void assertProperties(CachedRowSet expected, CachedRowSet actual)
            throws SQLException, SyncProviderException {
        assertEquals(expected.getCommand(), actual.getCommand());
        assertEquals(expected.getConcurrency(), actual.getConcurrency());

        try {
            assertEquals(expected.getCursorName(), actual.getCursorName());
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            assertEquals(expected.getMatchColumnIndexes(), actual
                    .getMatchColumnIndexes());
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            assertEquals(expected.getMatchColumnNames(), actual
                    .getMatchColumnNames());
        } catch (SQLException e) {
            // expected
        }

        assertEquals(expected.getStatement(), actual.getStatement());

        assertEquals(expected.getEscapeProcessing(), actual
                .getEscapeProcessing());
        assertEquals(expected.getFetchDirection(), actual.getFetchDirection());
        assertEquals(expected.getFetchSize(), actual.getFetchSize());
        if (expected.getKeyColumns() != null
                && expected.getKeyColumns().length != 0) {
            int[] keyColumns = expected.getKeyColumns();
            int[] copyKeyColumns = actual.getKeyColumns();

            assertEquals(keyColumns.length, copyKeyColumns.length);
            for (int i = 0; i < keyColumns.length; i++) {
                assertEquals(keyColumns[i], copyKeyColumns[i]);
            }
            assertEquals(expected.getKeyColumns(), actual.getKeyColumns());
        }

        assertEquals(expected.getMaxFieldSize(), actual.getMaxFieldSize());
        assertEquals(expected.getMaxRows(), actual.getMaxRows());

        assertEquals(expected.getPageSize(), actual.getPageSize());
        assertEquals(expected.getPassword(), actual.getPassword());
        assertEquals(expected.getQueryTimeout(), actual.getQueryTimeout());

        // TODO RI doesn't set show deleted row correctly after writeXml
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(expected.getShowDeleted(), actual.getShowDeleted());
        }

        assertEquals(expected.getSyncProvider().getProviderID(), actual
                .getSyncProvider().getProviderID());
        assertEquals(expected.getSyncProvider().getProviderGrade(), actual
                .getSyncProvider().getProviderGrade());
        assertEquals(expected.getSyncProvider().getDataSourceLock(), actual
                .getSyncProvider().getDataSourceLock());
        assertEquals(expected.getSyncProvider().getVendor(), actual
                .getSyncProvider().getVendor());
        assertEquals(expected.getSyncProvider().getVersion(), actual
                .getSyncProvider().getVersion());

        assertEquals(expected.getTableName(), actual.getTableName());
        assertEquals(expected.getTransactionIsolation(), actual
                .getTransactionIsolation());
        assertEquals(expected.getType(), actual.getType());

        assertEquals(expected.getUsername(), actual.getUsername());
    }
}
