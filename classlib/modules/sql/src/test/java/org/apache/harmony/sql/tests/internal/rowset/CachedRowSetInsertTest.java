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

public class CachedRowSetInsertTest extends CachedRowSetTestCase {

    public void testRowInserted() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.rowInserted();
            fail("should throw exception");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw exception here
        } catch (SQLException e) {
            // expected
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        assertTrue(noInitialCrset.isBeforeFirst());
        try {
            noInitialCrset.rowInserted();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        noInitialCrset.afterLast();
        assertTrue(noInitialCrset.isAfterLast());
        try {
            noInitialCrset.rowInserted();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        assertTrue(noInitialCrset.absolute(1));
        assertFalse(noInitialCrset.rowInserted());

        noInitialCrset.moveToInsertRow();
        try {
            noInitialCrset.rowInserted();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        noInitialCrset.updateInt(1, 10);
        noInitialCrset.updateString(2, "insert10");
        try {
            noInitialCrset.rowInserted();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        noInitialCrset.insertRow();
        try {
            noInitialCrset.rowInserted();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        noInitialCrset.moveToCurrentRow();

        noInitialCrset.beforeFirst();
        boolean isInserted = false;
        while (noInitialCrset.next()) {
            if (noInitialCrset.getInt(1) == 10) {
                isInserted = true;
                assertTrue(noInitialCrset.rowInserted());
            } else {
                assertFalse(noInitialCrset.rowInserted());
            }
        }
        assertTrue(isInserted);
    }

    public void testInsertRow_Single() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.moveToInsertRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        try {
            noInitialCrset.insertRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        // if the cursor is not on the insert row, no effect
        noInitialCrset.moveToCurrentRow();

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        assertTrue(noInitialCrset.absolute(3));
        noInitialCrset.moveToInsertRow();

        // getRow() return 0 when the cursor is on the insert row
        assertEquals(0, noInitialCrset.getRow());

        try {
            // call insertRow() without call any update method
            noInitialCrset.insertRow();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        noInitialCrset.updateInt(1, 10);
        assertEquals(10, noInitialCrset.getInt(1));
        noInitialCrset.updateString(2, "insert10");
        assertEquals("insert10", noInitialCrset.getString(2));

        assertEquals(0, noInitialCrset.getRow());
        noInitialCrset.insertRow();
        assertEquals(0, noInitialCrset.getRow());

        noInitialCrset.moveToCurrentRow();
        assertEquals(3, noInitialCrset.getInt(1));

        // the inserted row is after the current row immediately
        assertTrue(noInitialCrset.next());
        assertEquals("insert10", noInitialCrset.getString(2));

        assertTrue(noInitialCrset.next());
        assertEquals("test4", noInitialCrset.getString(2));
    }

    public void testInsertRow_Multi() throws Exception {
        noInitialCrset = newNoInitialInstance();
        // only when the cursor is on the insert row, it will have effect
        noInitialCrset.moveToCurrentRow();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        // move the cursor to the third row
        assertTrue(noInitialCrset.absolute(3));

        /*
         * Insert one row
         */
        noInitialCrset.moveToInsertRow();
        noInitialCrset.updateInt(1, 10);
        assertEquals(10, noInitialCrset.getInt(1));
        noInitialCrset.updateString(2, "update10");
        assertEquals("update10", noInitialCrset.getString(2));
        noInitialCrset.insertRow();
        // the cursor is still on the insert row after call insertRow()
        assertEquals(10, noInitialCrset.getInt(1));
        assertEquals("update10", noInitialCrset.getString(2));

        /*
         * Insert another row. Here call moveToInsertRow() causes the original
         * current row index lose.
         */
        noInitialCrset.moveToInsertRow();
        noInitialCrset.updateInt(1, 11);
        assertEquals(11, noInitialCrset.getInt(1));
        noInitialCrset.updateString(2, "update11");
        assertEquals("update11", noInitialCrset.getString(2));
        noInitialCrset.insertRow();
        assertEquals(11, noInitialCrset.getInt(1));
        assertEquals("update11", noInitialCrset.getString(2));
        noInitialCrset.moveToCurrentRow();

        // commit to database
        noInitialCrset.acceptChanges(conn);

        // check db
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        int index = 0;
        while (rs.next()) {
            index++;
        }
        assertEquals(6, index);

        // check CahcedRowSet
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next()) {
            index++;
            assertFalse(noInitialCrset.rowInserted());
        }
        assertEquals(6, index);

        // move the cursor to the third row
        assertTrue(noInitialCrset.absolute(3));
        assertEquals(3, noInitialCrset.getInt(1));
        // insert a row
        noInitialCrset.moveToInsertRow();
        noInitialCrset.updateInt(1, 12);
        noInitialCrset.updateString(2, "update12");
        noInitialCrset.insertRow();
        assertEquals(12, noInitialCrset.getInt(1));
        assertEquals("update12", noInitialCrset.getString(2));
        noInitialCrset.moveToCurrentRow(); // notice here
        // check current row
        assertEquals(3, noInitialCrset.getInt(1));

        // insert another row
        noInitialCrset.moveToInsertRow();
        noInitialCrset.updateInt(1, 13);
        noInitialCrset.updateString(2, "update13");
        noInitialCrset.insertRow();
        assertEquals(13, noInitialCrset.getInt(1));
        assertEquals("update13", noInitialCrset.getString(2));
        noInitialCrset.moveToCurrentRow(); // notice here
        // check current row
        assertEquals(3, noInitialCrset.getInt(1));

        // commit to database
        noInitialCrset.acceptChanges(conn);
        assertEquals(3, noInitialCrset.getInt(1));

        // check db
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        index = 0;
        while (rs.next()) {
            index++;
        }
        assertEquals(8, index);

        // check CahcedRowSet
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next()) {
            index++;
            assertFalse(noInitialCrset.rowInserted());
        }
        assertEquals(8, index);

        // move the cursor to the third row
        assertTrue(noInitialCrset.absolute(3));
        assertEquals(3, noInitialCrset.getInt(1));
        noInitialCrset.moveToInsertRow();
        noInitialCrset.updateInt(1, 14);
        assertEquals(14, noInitialCrset.getInt(1));
        noInitialCrset.updateString(2, "update14");
        noInitialCrset.insertRow();
        assertEquals("update14", noInitialCrset.getString(2));
        noInitialCrset.updateInt(1, 15);
        assertEquals(15, noInitialCrset.getInt(1));
        noInitialCrset.updateString(2, "update15");
        noInitialCrset.insertRow();
        assertEquals("update15", noInitialCrset.getString(2));
        noInitialCrset.moveToCurrentRow();
        assertEquals(3, noInitialCrset.getInt(1));
        // commit to database
        noInitialCrset.acceptChanges(conn);
        assertEquals(3, noInitialCrset.getInt(1));

        // check db
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        index = 0;
        while (rs.next()) {
            index++;
        }
        assertEquals(10, index);

        // check CahcedRowSet
        noInitialCrset.beforeFirst();
        index = 0;
        while (noInitialCrset.next()) {
            index++;
            assertFalse(noInitialCrset.rowInserted());
        }
        assertEquals(10, index);
    }

    public void testInsertRow_MultiTwo() throws Exception {
        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        assertTrue(noInitialCrset.first());
        noInitialCrset.moveToInsertRow();
        for (int i = 11; i <= 15; i++) {
            noInitialCrset.updateInt(1, i);
            noInitialCrset.updateString(2, "insert" + i);
            noInitialCrset.insertRow();
        }
        noInitialCrset.moveToCurrentRow();
        assertEquals(1, noInitialCrset.getInt(1));

        int index = 1;
        do {
            if (index == 1) {
                assertEquals(1, noInitialCrset.getInt(1));
            } else if (index == 2) {
                assertEquals(15, noInitialCrset.getInt(1));
            } else if (index == 3) {
                assertEquals(14, noInitialCrset.getInt(1));
            } else if (index == 4) {
                assertEquals(13, noInitialCrset.getInt(1));
            } else if (index == 5) {
                assertEquals(12, noInitialCrset.getInt(1));
            } else if (index == 6) {
                assertEquals(11, noInitialCrset.getInt(1));
            } else if (index == 7) {
                assertEquals(2, noInitialCrset.getInt(1));
            }
            index++;
        } while (noInitialCrset.next());
        assertEquals(10, index);
    }

    public void testUndoInsert() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.undoInsert();
            fail("should throw exception");
        } catch (ArrayIndexOutOfBoundsException e) {
            // RI throw ArrayIndexOutOfBoundsException
        } catch (SQLException e) {
            // expected
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        // the cursor is before the first row
        assertTrue(noInitialCrset.isBeforeFirst());
        try {
            noInitialCrset.undoInsert();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        // the cursor is after the last row
        noInitialCrset.afterLast();
        assertTrue(noInitialCrset.isAfterLast());
        try {
            noInitialCrset.undoInsert();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        // the cursor is on the insert row
        assertTrue(noInitialCrset.absolute(3));
        try {
            noInitialCrset.undoInsert();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        assertEquals(3, noInitialCrset.getInt(1));
        noInitialCrset.moveToInsertRow();
        try {
            noInitialCrset.undoInsert();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        noInitialCrset.updateInt(1, 10);
        noInitialCrset.updateString(2, "update10");
        try {
            noInitialCrset.undoInsert();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        noInitialCrset.insertRow();
        try {
            noInitialCrset.undoInsert();
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        noInitialCrset.moveToCurrentRow();
        noInitialCrset.acceptChanges(conn);
        assertEquals(3, noInitialCrset.getInt(1));

        // check db
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 10");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));

        // check CachedRowSet
        boolean isInserted = false;
        noInitialCrset.beforeFirst();
        while (noInitialCrset.next()) {
            if (noInitialCrset.getInt(1) == 10) {
                isInserted = true;
            }
        }
        assertTrue(isInserted);

        /*
         * call undoInsert() successfully
         */
        assertTrue(noInitialCrset.absolute(3));
        assertEquals(3, noInitialCrset.getInt(1));
        noInitialCrset.moveToInsertRow();
        noInitialCrset.updateInt(1, 20);
        noInitialCrset.updateString(2, "update20");
        noInitialCrset.insertRow();
        noInitialCrset.moveToCurrentRow();
        // move to the inserted row
        noInitialCrset.beforeFirst();
        while (noInitialCrset.next()) {
            if (noInitialCrset.rowInserted()) {
                break;
            }
        }
        assertEquals(20, noInitialCrset.getInt(1));
        assertTrue(noInitialCrset.rowInserted());
        /*
         * When run on RI, undoInsert() has no effect.
         */
        noInitialCrset.undoInsert();

        // commit to database
        noInitialCrset.acceptChanges(conn);

        // check db
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 20");
        assertTrue(rs.next());
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertEquals(0, rs.getInt(1));
        } else {
            // undoInsert() has no effect when run RI.
            assertEquals(1, rs.getInt(1));
        }
    }
}
