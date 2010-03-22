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

public class CachedRowSetDeleteTest extends CachedRowSetTestCase {
    public void testDeleteRow_CursorPos() throws Exception {
        /*
         * This method is mainly used to test cursor position after delete
         */
        insertMoreData(5);
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.populate(rs);

        assertFalse(noInitialCrset.getShowDeleted());

        /*
         * move to the fifth row, then delete it, check the cursor position
         */
        assertTrue(noInitialCrset.absolute(5));
        assertEquals(5, noInitialCrset.getInt(1));
        noInitialCrset.deleteRow();
        assertEquals(5, noInitialCrset.getInt(1));
        assertEquals(0, noInitialCrset.getRow());

        noInitialCrset.acceptChanges(conn);
        // the cursor is on the sixth row now, that is move to the next row
        assertEquals(6, noInitialCrset.getInt(1));

        // cursor index is no change
        assertTrue(noInitialCrset.absolute(5));
        assertEquals(6, noInitialCrset.getInt(1));

        /*
         * Delete the sixth row. Then move the cursor to the seventh row. Check
         * the cursor position after delete.
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            // TODO RI move cursor incorrectly, we follow spec
            assertEquals(5, noInitialCrset.getRow());
            noInitialCrset.deleteRow();
            assertTrue(noInitialCrset.next());

            assertEquals(5, noInitialCrset.getRow());
            assertEquals(7, noInitialCrset.getInt(1));

            noInitialCrset.acceptChanges(conn);

            // cursor index is no change
            assertEquals(5, noInitialCrset.getRow());
            assertEquals(7, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.absolute(5));
            assertEquals(7, noInitialCrset.getInt(1));
        } else {
            noInitialCrset.deleteRow();
            assertTrue(noInitialCrset.next());
            assertEquals(7, noInitialCrset.getInt(1));
            noInitialCrset.acceptChanges(conn);
            /*
             * We can see the cursor is on the eighth row now.
             */
            assertEquals(8, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.absolute(6));
            assertEquals(8, noInitialCrset.getInt(1));
        }
        /*
         * Delete the row before the last row. Then move the cursor to the last
         * row before call acceptChanges(). Check the cursor position after
         * delete.
         */
        assertTrue(noInitialCrset.last());
        assertTrue(noInitialCrset.previous());
        assertEquals(8, noInitialCrset.getInt(1));
        noInitialCrset.deleteRow();
        assertTrue(noInitialCrset.last());
        assertEquals(9, noInitialCrset.getInt(1));
        noInitialCrset.acceptChanges(conn);

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            // TODO RI move cursor incorrectly, we follow spec
            assertTrue(noInitialCrset.isLast());

            assertEquals(9, noInitialCrset.getInt(1));
            noInitialCrset.acceptChanges(conn);
        } else {
            assertTrue(noInitialCrset.isAfterLast());
        }
    }

    public void testDeleteRow_MoveCursor() throws Exception {
        insertMoreData(5);
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.populate(rs);

        assertFalse(noInitialCrset.getShowDeleted());

        assertTrue(noInitialCrset.absolute(3));
        assertEquals(3, noInitialCrset.getInt(1));

        assertEquals(3, noInitialCrset.getRow());
        noInitialCrset.deleteRow(); // delete the third row
        assertEquals(0, noInitialCrset.getRow());

        assertTrue(noInitialCrset.next());

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            // TODO RI move cursor incorrectly, we follow spec
            assertEquals(3, noInitialCrset.getRow());
            assertEquals(4, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.absolute(4));
            assertEquals(4, noInitialCrset.getRow());
            assertEquals(5, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.previous());
            assertEquals(3, noInitialCrset.getRow());
            assertEquals(4, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.previous());
            assertEquals(2, noInitialCrset.getRow());
            assertEquals(2, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.previous());
            assertEquals(1, noInitialCrset.getRow());
            assertEquals(1, noInitialCrset.getInt(1));

        } else {
            /*
             * TODO seems RI doesn't adjust cursor index after delete row
             */
            assertEquals(4, noInitialCrset.getRow());
            assertEquals(4, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.absolute(4));
            assertEquals(4, noInitialCrset.getRow());
            assertEquals(4, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.previous());
            assertEquals(3, noInitialCrset.getRow());
            assertEquals(2, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.previous());
            assertEquals(2, noInitialCrset.getRow());
            assertEquals(1, noInitialCrset.getInt(1));

            assertFalse(noInitialCrset.previous());
            assertEquals(0, noInitialCrset.getRow());

            try {
                noInitialCrset.getInt(1);
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, Invalid cursor position
            }
        }
    }

    public void testDeleteRow_MoveCursor2() throws Exception {
        insertMoreData(5);
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.populate(rs);

        assertFalse(noInitialCrset.getShowDeleted());

        assertTrue(noInitialCrset.absolute(3));
        assertEquals(3, noInitialCrset.getInt(1));

        assertEquals(3, noInitialCrset.getRow());
        noInitialCrset.deleteRow(); // delete the third row
        assertEquals(0, noInitialCrset.getRow());

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(noInitialCrset.absolute(3));
            assertEquals(3, noInitialCrset.getRow());
            assertEquals(4, noInitialCrset.getInt(1));
        } else {
            /*
             * TODO RI's behavior is weird, we can't understand, so Harmony will
             * follow spec
             */
            assertFalse(noInitialCrset.getShowDeleted());
            assertTrue(noInitialCrset.absolute(3));
            assertEquals(0, noInitialCrset.getRow());
            // i can see the deleted row!!
            assertEquals(3, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.next());
            assertEquals(4, noInitialCrset.getRow());
            assertEquals(4, noInitialCrset.getInt(1));

            assertTrue(noInitialCrset.previous());
            assertEquals(3, noInitialCrset.getRow());
            // deleted row disappeared
            assertEquals(2, noInitialCrset.getInt(1));

        }
    }

    public void testDeleteRow_MultiDel() throws Exception {
        insertMoreData(5);
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.populate(rs);

        assertFalse(noInitialCrset.getShowDeleted());

        assertTrue(noInitialCrset.absolute(3));
        assertEquals(3, noInitialCrset.getInt(1));
        noInitialCrset.deleteRow(); // delete the third row
        assertEquals(0, noInitialCrset.getRow());

        assertTrue(noInitialCrset.next());
        noInitialCrset.deleteRow(); // delete the fourth row
        assertEquals(0, noInitialCrset.getRow());

        assertTrue(noInitialCrset.next());
        noInitialCrset.deleteRow(); // delete the fifth row
        assertEquals(0, noInitialCrset.getRow());
        noInitialCrset.acceptChanges(conn);

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            // TODO RI move cursor incorrectly, we follow spec
            assertEquals(6, noInitialCrset.getInt(1));
        } else {
            assertEquals(8, noInitialCrset.getInt(1));
        }
    }

    public void testRowDeleted() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.rowDeleted();
            fail("should throw SQLException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI would throw ArrayIndexOutOfBoundsException
        } catch (SQLException e) {
            // according to spec, it's supposed to throw SQLException
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        try {
            noInitialCrset.rowDeleted();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        assertTrue(noInitialCrset.next());
        assertFalse(noInitialCrset.rowDeleted());
        noInitialCrset.deleteRow();
        assertTrue(noInitialCrset.rowDeleted());
    }

    public void testDeleteRow_Exception() throws Exception {
        /*
         * This method is mainly used to test exception
         */
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.deleteRow();
            fail("should throw SQLException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI would throw ArrayIndexOutOfBoundsException
        } catch (SQLException e) {
            // according to spec, it's supposed to throw SQLException
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        try {
            noInitialCrset.deleteRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.moveToInsertRow();
        try {
            noInitialCrset.deleteRow();
            fail("should throw SQLException");
        } catch (ClassCastException e) {
            // RI would throw ClassCastException
        } catch (SQLException e) {
            // expected
        }

        noInitialCrset.moveToCurrentRow();
        assertTrue(noInitialCrset.absolute(4));
        assertEquals(4, noInitialCrset.getInt(1));
        noInitialCrset.deleteRow();
        noInitialCrset.acceptChanges(conn);
        // check the cursor position after delete
        assertTrue(noInitialCrset.isAfterLast());

        // check DB
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 4");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));

        // check CachedRowSet
        noInitialCrset.beforeFirst();
        int index = 0;
        while (noInitialCrset.next()) {
            index++;
            assertEquals(index, noInitialCrset.getInt(1));
        }
        assertEquals(3, index);
    }

    public void testUndoDeleted() throws Exception {
        noInitialCrset = newNoInitialInstance();
        // if no data, nothing would happen when calling undoDelete()
        noInitialCrset.undoDelete();

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        /*
         * Test exception. It would throw SQLException when calling undoDelete()
         * in such conditions.
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            // TODO RI doesn't follow the spec, we follow spec
            noInitialCrset.beforeFirst();
            assertTrue(noInitialCrset.isBeforeFirst());
            try {
                noInitialCrset.undoDelete();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, invalid cursor
            }

            noInitialCrset.afterLast();
            assertTrue(noInitialCrset.isAfterLast());
            try {
                noInitialCrset.undoDelete();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, invalid cursor
            }

            noInitialCrset.moveToInsertRow();
            try {
                noInitialCrset.undoDelete();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, invalid cursor
            }

            noInitialCrset.moveToCurrentRow();
            assertTrue(noInitialCrset.absolute(1));
            assertFalse(noInitialCrset.rowDeleted());
            try {
                noInitialCrset.undoDelete();
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, invalid cursor
            }

        } else {
            // when run on RI, it won't throw SQLException
            noInitialCrset.beforeFirst();
            assertTrue(noInitialCrset.isBeforeFirst());
            noInitialCrset.undoDelete();

            noInitialCrset.afterLast();
            assertTrue(noInitialCrset.isAfterLast());
            noInitialCrset.undoDelete();

            noInitialCrset.moveToInsertRow();
            noInitialCrset.undoDelete();

            noInitialCrset.moveToCurrentRow();
            assertTrue(noInitialCrset.absolute(1));
            assertFalse(noInitialCrset.rowDeleted());
            noInitialCrset.undoDelete();
        }

        // delete the fourth row
        assertTrue(noInitialCrset.absolute(4));
        assertEquals(4, noInitialCrset.getInt(1));
        assertFalse(noInitialCrset.rowDeleted());
        noInitialCrset.deleteRow();
        assertTrue(noInitialCrset.rowDeleted());

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            noInitialCrset.undoDelete();
            assertFalse(noInitialCrset.rowDeleted());
            noInitialCrset.acceptChanges(conn);

            // check DB
            rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 4");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));

            // check CachedRowSet
            noInitialCrset.beforeFirst();
            int index = 0;
            while (noInitialCrset.next()) {
                index++;
                assertEquals(index, noInitialCrset.getInt(1));
            }
            assertEquals(4, index);

        } else {
            // TODO undoDelete() still makes no difference in RI
            noInitialCrset.undoDelete();
            assertTrue(noInitialCrset.rowDeleted());
            noInitialCrset.acceptChanges(conn);

            // check DB
            rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 4");
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));

            // check CachedRowSet
            noInitialCrset.beforeFirst();
            int index = 0;
            while (noInitialCrset.next()) {
                index++;
                assertEquals(index, noInitialCrset.getInt(1));
            }
            assertEquals(3, index);
        }

    }

    public void testShowDeleted_False() throws Exception {
        /*
         * when getShowDeleted() is false
         */
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        assertFalse(noInitialCrset.getShowDeleted());

        // delete the third row
        assertTrue(noInitialCrset.absolute(3));
        assertFalse(noInitialCrset.rowDeleted());
        noInitialCrset.deleteRow();
        assertTrue(noInitialCrset.rowDeleted());
        // call next(), then call previous, to check which row the curosr is on
        assertTrue(noInitialCrset.next());
        assertEquals(4, noInitialCrset.getInt(1));
        assertTrue(noInitialCrset.previous());
        // NOTICE: The cursor is on the second row, not the third row
        assertEquals(2, noInitialCrset.getInt(1));

        // retrieve the data in CachedRowSet after calling deleteRow()
        noInitialCrset.beforeFirst();
        int index = 0;
        while (noInitialCrset.next()) {
            index++;
            if (index == 3) {
                assertEquals(4, noInitialCrset.getInt(1));
            } else {
                assertEquals(index, noInitialCrset.getInt(1));
            }
        }
        assertEquals(3, index);

        // commit to DB
        noInitialCrset.acceptChanges(conn);

        // check DB
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 3");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));

        // check CachedRowSet
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next()) {
            index++;
            if (index == 3) {
                assertEquals(4, noInitialCrset.getInt(1));
            } else {
                assertEquals(index, noInitialCrset.getInt(1));
            }
        }
        assertEquals(3, index);
    }

    public void testAbsolute() throws Exception {
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        assertFalse(noInitialCrset.getShowDeleted());

        // delete the third row
        assertTrue(noInitialCrset.absolute(3));
        assertFalse(noInitialCrset.rowDeleted());
        noInitialCrset.deleteRow();
        assertTrue(noInitialCrset.rowDeleted());

        assertTrue(noInitialCrset.previous());
        assertTrue(noInitialCrset.previous());

        assertEquals(1, noInitialCrset.getInt(1));

        noInitialCrset.absolute(3);
        assertEquals(4, noInitialCrset.getInt(1));
    }

    public void testRelative() throws Exception {
        insertMoreData(10);

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);
        assertFalse(crset.getShowDeleted());

        // delete row 6
        assertTrue(crset.absolute(6));
        assertFalse(crset.rowDeleted());
        crset.deleteRow();
        assertTrue(crset.rowDeleted());

        // delete the third row
        assertTrue(crset.absolute(3));
        assertFalse(crset.rowDeleted());
        assertEquals(3, crset.getInt(1));
        crset.deleteRow();
        assertTrue(crset.rowDeleted());

        assertTrue(crset.relative(3));
        assertEquals(7, crset.getInt(1));

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);
        assertFalse(crset.getShowDeleted());

        // delete the third row
        assertTrue(crset.absolute(1));
        assertEquals(1, crset.getInt(1));
        assertFalse(crset.rowDeleted());
        crset.deleteRow();
        assertTrue(crset.rowDeleted());

        assertFalse(crset.relative(-1));
        assertTrue(crset.isBeforeFirst());
    }

    public void testShowDeleted_True() throws Exception {
        /*
         * when getShowDeleted() is true
         */
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset = newNoInitialInstance();
        noInitialCrset.populate(rs);
        assertFalse(noInitialCrset.getShowDeleted());
        noInitialCrset.setShowDeleted(true);
        assertTrue(noInitialCrset.getShowDeleted());

        // delete the third row
        assertTrue(noInitialCrset.absolute(3));
        assertFalse(noInitialCrset.rowDeleted());
        noInitialCrset.deleteRow();
        assertTrue(noInitialCrset.rowDeleted());
        // call next(), then call previous, to check which row the curosr is on
        assertTrue(noInitialCrset.next());
        assertEquals(4, noInitialCrset.getInt(1));
        assertTrue(noInitialCrset.previous());
        // NOTICE: The cursor is on the third row. There is the difference
        // between getShowDeleted() is TRUE and FALSE
        assertEquals(3, noInitialCrset.getInt(1));

        // retrieve the data in CachedRowSet after calling deleteRow()
        noInitialCrset.beforeFirst();
        int index = 0;
        while (noInitialCrset.next()) {
            index++;
            assertEquals(index, noInitialCrset.getInt(1));
            if (index == 3) {
                assertTrue(noInitialCrset.rowDeleted());
            } else {
                assertFalse(noInitialCrset.rowDeleted());
            }
        }
        assertEquals(4, index);

        /*
         * move to the third row which is marked as deleted, then call
         * undoDelete(), see what will happen
         */
        assertTrue(noInitialCrset.absolute(3));
        assertEquals(3, noInitialCrset.getInt(1));
        assertTrue(noInitialCrset.rowDeleted());
        noInitialCrset.undoDelete();
        assertFalse(noInitialCrset.rowDeleted());

        // commit to DB
        noInitialCrset.acceptChanges(conn);

        // check DB
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 3");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));

        // check CachedRowSet
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next()) {
            index++;
            assertEquals(index, noInitialCrset.getInt(1));
            assertFalse(noInitialCrset.rowDeleted());
        }
        assertEquals(4, index);
    }

    public void testShowDeleted_StateChange() throws Exception {
        /*
         * getShowDeleted() is false in default. First, call deleteRow() on the
         * third row; then setShowDeleted(true), see what will happen; at last,
         * setShowDeleted(false) back.
         */
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        assertFalse(noInitialCrset.getShowDeleted());

        /*
         * Delete the third row when getShowDeleted() is false
         */
        assertTrue(noInitialCrset.absolute(3));
        assertFalse(noInitialCrset.rowDeleted());
        noInitialCrset.deleteRow();
        assertTrue(noInitialCrset.rowDeleted());
        // call next(), then call previous, to check which row the curosr is on
        assertTrue(noInitialCrset.next());
        assertEquals(4, noInitialCrset.getInt(1));
        assertTrue(noInitialCrset.previous());
        // NOTICE: The cursor is on the second row, not the third row
        assertEquals(2, noInitialCrset.getInt(1));
        // retrieve the data in CachedRowSet
        noInitialCrset.beforeFirst();
        int index = 0;
        while (noInitialCrset.next()) {
            index++;
            if (index == 3) {
                assertEquals(4, noInitialCrset.getInt(1));
            } else {
                assertEquals(index, noInitialCrset.getInt(1));
            }
        }
        assertEquals(3, index);

        /*
         * setShowDeleted(true), see what's happen
         */
        noInitialCrset.setShowDeleted(true);
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next()) {
            index++;
            assertEquals(index, noInitialCrset.getInt(1));
            if (index == 3) {
                assertTrue(noInitialCrset.rowDeleted());
            } else {
                assertFalse(noInitialCrset.rowDeleted());
            }
        }
        assertEquals(4, index);

        /*
         * setShowDeleted(false), see what's happen
         */
        noInitialCrset.setShowDeleted(false);
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next()) {
            index++;
            if (index == 3) {
                assertEquals(4, noInitialCrset.getInt(1));
            } else {
                assertEquals(index, noInitialCrset.getInt(1));
            }
        }
        assertEquals(3, index);

        /*
         * setShowDeleted(true), then commit to DB
         */
        noInitialCrset.acceptChanges(conn);
        // check DB
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 3");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));

        // check CachedRowSet
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next()) {
            index++;
            if (index == 3) {
                assertEquals(4, noInitialCrset.getInt(1));
            } else {
                assertEquals(index, noInitialCrset.getInt(1));
            }
        }
        assertEquals(3, index);
    }
}
