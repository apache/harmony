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

import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.spi.SyncProviderException;
import javax.sql.rowset.spi.SyncResolver;

import org.apache.harmony.sql.internal.rowset.CachedRow;
import org.apache.harmony.sql.internal.rowset.SyncResolverImpl;

public class SyncResolverTest extends CachedRowSetTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNotSupportMethods() throws Exception {
        CachedRowSet copy = crset.createCopy();

        copy.absolute(3);
        crset.absolute(3);

        copy.updateString(2, "updated");
        assertEquals("updated", copy.getString(2));
        assertEquals("test3", crset.getString(2));

        copy.updateRow();
        copy.acceptChanges();

        assertEquals(copy.getString(2), "updated");
        assertEquals(crset.getString(2), "test3");

        crset.updateString(2, "again");

        assertEquals(copy.getString(2), "updated");
        assertEquals(crset.getString(2), "again");

        crset.updateRow();

        SyncProviderException ex = null;
        try {
            crset.acceptChanges(conn);
        } catch (SyncProviderException e) {
            ex = e;
        }

        SyncResolver resolver = ex.getSyncResolver();

        try {
            resolver.absolute(1);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.relative(1);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.next();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.previous();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.isAfterLast();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.isBeforeFirst();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.isFirst();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.isLast();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getMetaData();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getString(2);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getCursorName();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateString(2, "hello");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.rowDeleted();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.rowInserted();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.rowUpdated();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getWarnings();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getStatement();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.findColumn("ID");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.wasNull();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.moveToCurrentRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.moveToInsertRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.refreshRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.execute();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.clearWarnings();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.deleteRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.insertRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.cancelRowUpdates();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.close();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetConflictValue() throws Exception {

        RowSetMetaData metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(DEFAULT_COLUMN_COUNT);

        SyncResolverImpl resolver = new SyncResolverImpl(metadata);
        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 1,
                SyncResolver.INSERT_ROW_CONFLICT);

        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 2,
                SyncResolver.INSERT_ROW_CONFLICT);

        // before call nextConflict
        try {
            resolver.getConflictValue(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid cursor position
        }

        try {
            resolver.getConflictValue(-1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid column index
        }
        try {
            resolver.getConflictValue("not exist");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid column name
        }

        assertTrue(resolver.nextConflict());

        for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
            assertNull(resolver.getConflictValue(i));
        }

        assertTrue(resolver.nextConflict());

        for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
            assertNull(resolver.getConflictValue(i));
        }

        assertFalse(resolver.nextConflict());
        assertEquals(0, resolver.getRow());

        /*
         * ri throw SQLException after call nextConflict again, it's not
         * reasonable
         */
        try {
            resolver.getConflictValue(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected Invalid cursor position
        }

    }

    public void testNextPreviousConflict() throws Exception {

        RowSetMetaData metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(DEFAULT_COLUMN_COUNT);

        SyncResolverImpl resolver = new SyncResolverImpl(metadata);
        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 1,
                SyncResolver.INSERT_ROW_CONFLICT);

        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 2,
                SyncResolver.INSERT_ROW_CONFLICT);

        assertTrue(resolver.nextConflict());
        assertTrue(resolver.nextConflict());
        assertFalse(resolver.nextConflict());
        assertFalse(resolver.nextConflict());

        assertTrue(resolver.previousConflict());
        assertTrue(resolver.previousConflict());
        assertFalse(resolver.previousConflict());
        assertFalse(resolver.previousConflict());
    }

    public void testGetStatus() throws Exception {

        RowSetMetaData metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(DEFAULT_COLUMN_COUNT);

        SyncResolverImpl resolver = new SyncResolverImpl(metadata);
        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 1,
                SyncResolver.INSERT_ROW_CONFLICT);

        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 2,
                SyncResolver.INSERT_ROW_CONFLICT);

        try {
            resolver.getStatus();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        assertTrue(resolver.nextConflict());
        assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());
        assertTrue(resolver.nextConflict());
        assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());
        assertFalse(resolver.nextConflict());

        try {
            resolver.getStatus();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

    }
}
