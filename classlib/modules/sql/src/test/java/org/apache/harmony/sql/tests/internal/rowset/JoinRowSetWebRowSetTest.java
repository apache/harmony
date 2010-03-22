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

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.JoinRowSet;
import javax.sql.rowset.WebRowSet;
import javax.sql.rowset.spi.SyncProviderException;

public class JoinRowSetWebRowSetTest extends JoinRowSetTestCase {

    public void testWriteXml_Empty() throws Exception {
        StringWriter writer = new StringWriter();
        try {
            jrs.writeXml(writer);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // Expected.
        }
    }

    public void testReadXml_Empty() throws Exception {
        jrs = newJoinRowSet();
        jrs.addRowSet(crset, 1);
        StringWriter writer = new StringWriter();
        jrs.writeXml(writer);

        JoinRowSet another = newJoinRowSet();
        another.readXml(new StringReader(writer.getBuffer().toString()));

        assertCachedRowSetEquals(jrs, another);
    }

    public void testWriteXml() throws Exception {
        StringWriter writer = new StringWriter();

        jrs.addRowSet(crset, 1);
        jrs.writeXml(writer);

        WebRowSet another = newWebRowSet();
        another.readXml(new StringReader(writer.getBuffer().toString()));
        assertCachedRowSetEquals(jrs, another);
    }

    public void testWriteAndRead_Insert() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.beforeFirst();
        assertTrue(jrs.next());
        jrs.moveToInsertRow();
        jrs.updateInt(1, 5);
        jrs.updateString(2, "insertrow");
        jrs.insertRow();
        jrs.moveToCurrentRow();
        jrs.beforeFirst();

        jrs.absolute(2);
        assertTrue(jrs.rowInserted());

        StringWriter writer = new StringWriter();
        jrs.writeXml(writer);

        JoinRowSet another = newJoinRowSet();
        another.readXml(new StringReader(writer.getBuffer().toString()));

        if (System.getProperty("Testing Harmony") == "true") {
            assertCachedRowSetEquals(jrs, another);
        } else {
            // TODO why the output xml has no insert information.
            another.absolute(2);
            assertFalse(another.rowInserted());
            jrs.absolute(2);
            assertTrue(jrs.rowInserted());
        }
    }

    public void testWriteAndRead_Update() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.beforeFirst();

        assertTrue(jrs.absolute(3));
        jrs.updateString(2, "updateRow");
        jrs.updateRow();

        assertTrue(jrs.next());
        jrs.updateString(2, "anotherUpdateRow");
        jrs.updateRow();

        StringWriter writer = new StringWriter();

        jrs.writeXml(writer);

        JoinRowSet another = newJoinRowSet();
        another.readXml(new StringReader(writer.getBuffer().toString()));


        if (System.getProperty("Testing Harmony") == "true") {
            assertCachedRowSetEquals(jrs, another);
        } else {
            another.absolute(3);
            assertFalse(another.rowUpdated());
            jrs.absolute(3);
            assertTrue(jrs.rowUpdated());
            
            // TODO why the output xml has no update information.
            another.absolute(4);
            assertFalse(another.rowUpdated());
            jrs.absolute(4);
            assertTrue(jrs.rowUpdated());
        }
    }

    public void testWriteAndRead_Delete() throws Exception {
        jrs.addRowSet(crset, 1);
        jrs.beforeFirst();

        crset.setShowDeleted(true);
        crset.absolute(3);
        crset.deleteRow();
        crset.absolute(3);
        assertTrue(crset.rowDeleted());

        jrs.setShowDeleted(true);
        assertTrue(jrs.absolute(3));
        jrs.deleteRow();

        jrs.absolute(3);
        assertTrue(jrs.rowDeleted());
        
        assertTrue(jrs.absolute(4));
    }
    
    public void testWriteXmlLResultSet() throws Exception {
        StringWriter writer = new StringWriter();
        rs = st.executeQuery("select * from user_info");
        jrs.writeXml(rs, writer);
        
        JoinRowSet jrs2 = newJoinRowSet();
        jrs2.readXml(new StringReader(writer.getBuffer().toString()));
        assertCachedRowSetEquals(crset, jrs2);
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
