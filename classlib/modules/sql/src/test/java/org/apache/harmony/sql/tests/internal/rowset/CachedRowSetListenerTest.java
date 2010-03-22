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

import javax.sql.RowSetEvent;

public class CachedRowSetListenerTest extends CachedRowSetTestCase {

    public static final String EVENT_CURSOR_MOVED = "cursorMoved";

    public static final String EVENT_ROW_CHANGED = "rowChanged";

    public static final String EVENT_ROWSET_CHANGED = "rowSetChanged";

    public void testCursorMoved() throws Exception {
        Listener listener = new Listener();
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.addRowSetListener(listener);
        assertNull(listener.getTag());

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        /*
         * absolute() - cursorMoved
         */
        assertTrue(noInitialCrset.absolute(1));
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        assertTrue(noInitialCrset.absolute(1));
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        /*
         * relative() - cursorMoved
         */
        assertTrue(noInitialCrset.relative(1));
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        assertTrue(noInitialCrset.relative(1));
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        /*
         * next() - cursorMoved
         */
        assertTrue(noInitialCrset.next());
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        /*
         * previous() - cursorMoved
         */
        assertTrue(noInitialCrset.previous());
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        /*
         * first() - cursorMoved
         */
        assertTrue(noInitialCrset.first());
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        /*
         * last() - cursorMoved
         */
        assertTrue(noInitialCrset.last());
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        assertTrue(noInitialCrset.last());
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        /*
         * beforeFirst() - cursorMoved
         */
        noInitialCrset.beforeFirst();
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        noInitialCrset.beforeFirst();
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();

        /*
         * afterLast() - cursorMoved
         */
        noInitialCrset.afterLast();
        assertEquals(EVENT_CURSOR_MOVED, listener.getTag());
        listener.clear();
    }

    public void testRowChanged() throws Exception {
        Listener listener = new Listener();
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.addRowSetListener(listener);
        assertNull(listener.getTag());

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        assertTrue(noInitialCrset.absolute(1));
        listener.clear();
        noInitialCrset.setOriginalRow();
        assertNull(listener.getTag());

        /*
         * updateRow() - rowChanged
         */
        assertTrue(noInitialCrset.absolute(3));
        listener.clear();
        noInitialCrset.updateString(2, "abc");
        assertNull(listener.getTag());
        noInitialCrset.updateRow();
        assertEquals(EVENT_ROW_CHANGED, listener.getTag());

        assertTrue(noInitialCrset.next());
        listener.clear();
        noInitialCrset.updateRow();
        assertEquals(EVENT_ROW_CHANGED, listener.getTag());
        listener.clear();

        noInitialCrset.setOriginalRow();
        assertNull(listener.getTag());

        /*
         * deleteRow() - rowChanged
         */
        noInitialCrset.deleteRow();
        assertEquals(EVENT_ROW_CHANGED, listener.getTag());

        /*
         * insertRow() - rowChanged
         */
        assertTrue(noInitialCrset.first());
        listener.clear();
        noInitialCrset.moveToInsertRow();
        noInitialCrset.updateInt(1, 9);
        noInitialCrset.updateString(2, "insert");
        assertNull(listener.getTag());
        noInitialCrset.insertRow();
        assertEquals(EVENT_ROW_CHANGED, listener.getTag());
        noInitialCrset.moveToCurrentRow();
        listener.clear();

        assertTrue(noInitialCrset.absolute(3));
        listener.clear();
        noInitialCrset.refreshRow();
        noInitialCrset.updateString(2, "update3");
        noInitialCrset.refreshRow();
        assertNull(listener.getTag());
        noInitialCrset.updateString(2, "update3");
        noInitialCrset.updateRow();
        listener.clear();
        noInitialCrset.refreshRow();
        assertNull(listener.getTag());

        /*
         * undoDelete() - rowChanged
         */
        crset.addRowSetListener(listener);
        crset.setShowDeleted(true);
        assertTrue(crset.absolute(3));
        crset.deleteRow();
        assertTrue(crset.rowDeleted());
        assertTrue(crset.absolute(1));
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        assertTrue(crset.rowDeleted());
        listener.clear();
        crset.undoDelete();
        assertEquals(EVENT_ROW_CHANGED, listener.getTag());
        assertFalse(crset.rowDeleted());

        /*
         * undoUpdate() - rowChanged
         */
        assertTrue(crset.absolute(3));
        crset.moveToInsertRow();
        crset.updateInt(1, 10);
        crset.updateString(2, "insert10");
        crset.insertRow();
        crset.moveToCurrentRow();
        assertTrue(crset.next());
        assertEquals("insert10", crset.getString(2));
        assertTrue(crset.rowInserted());
        listener.clear();
        crset.undoUpdate();
        assertEquals(EVENT_ROW_CHANGED, listener.getTag());

        /*
         * undoInsert() - rowChanged
         */
        assertTrue(crset.absolute(3));
        crset.moveToInsertRow();
        crset.updateInt(1, 11);
        crset.updateString(2, "insert11");
        crset.insertRow();
        crset.moveToCurrentRow();
        assertTrue(crset.next());
        assertTrue(crset.rowInserted());
        listener.clear();
        crset.undoInsert();
        assertEquals(EVENT_ROW_CHANGED, listener.getTag());
    }

    public void testRowSetChanged() throws Exception {
        Listener listener = new Listener();
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.addRowSetListener(listener);

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        assertNull(listener.getTag());
        /*
         * populate() - rowSetChanged
         */
        noInitialCrset.populate(rs);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        listener.clear();

        /*
         * acceptChanges(Connection) - rowSetChanged
         */
        noInitialCrset.acceptChanges(conn);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        listener.clear();

        /*
         * acceptChanges() - rowSetChanged
         */
        noInitialCrset.setUrl(DERBY_URL);
        noInitialCrset.setCommand("SELECT * FROM USER_INFO");
        noInitialCrset.acceptChanges();
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        listener.clear();

        /*
         * execute() - rowSetChanged
         */
        noInitialCrset.execute();
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        listener.clear();

        /*
         * execute(Connection) - rowSetChanged
         */
        noInitialCrset.execute(conn);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        listener.clear();

        assertTrue(noInitialCrset.first());
        listener.clear();
        noInitialCrset.getOriginalRow();
        assertNull(listener.getTag());

        /*
         * nextPage() and previousPage() - rowSetChanged. See
         * CachedRowSetPagingTest.
         */

        /*
         * restoreOriginal() - rowSetChanged
         */
        noInitialCrset.restoreOriginal();
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        listener.clear();

        /*
         * release() - rowSetChanged
         */
        noInitialCrset.release();
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        listener.clear();
    }

    public void testRowSetPopulated_One() throws Exception {
        /*
         * There are 24 rows in database.
         */
        insertMoreData(20);
        noInitialCrset = newNoInitialInstance();
        Listener listener = new Listener();
        noInitialCrset.addRowSetListener(listener);

        assertEquals(0, noInitialCrset.getFetchSize());
        noInitialCrset.setMaxRows(10);
        noInitialCrset.setFetchSize(3);
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), 3);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), 4);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), 6);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), 8);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), 12);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), 24);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
    }

    public void testRowSetPopulated_Two() throws Exception {
        /*
         * There are only 4 rows in database.
         */
        noInitialCrset = newNoInitialInstance();
        Listener listener = new Listener();
        noInitialCrset.addRowSetListener(listener);

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(crset), 20);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        assertEquals(noInitialCrset, listener.getEventSource());

        for (int i = 1; i <= 10; i++) {
            listener.clear();
            noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), i);
            assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        noInitialCrset.setMaxRows(10);
        noInitialCrset.setFetchSize(2);

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), 2);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), 3);
        assertNull(listener.getTag());

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(noInitialCrset), 4);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(crset), 4);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        assertEquals(noInitialCrset, listener.getEventSource());
    }

    public void testRowSetPopulated_Three() throws Exception {
        noInitialCrset = newNoInitialInstance();
        Listener listener = new Listener();
        noInitialCrset.addRowSetListener(listener);
        noInitialCrset.setMaxRows(10);
        noInitialCrset.setFetchSize(4);

        try {
            noInitialCrset.rowSetPopulated(new RowSetEvent(crset), 3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        listener.clear();
        noInitialCrset.rowSetPopulated(new RowSetEvent(crset), 4);
        assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());

        for (int i = 5; i <= 30; i++) {
            listener.clear();
            noInitialCrset.rowSetPopulated(new RowSetEvent(crset), i);
            assertEquals(EVENT_ROWSET_CHANGED, listener.getTag());
        }
    }
}
