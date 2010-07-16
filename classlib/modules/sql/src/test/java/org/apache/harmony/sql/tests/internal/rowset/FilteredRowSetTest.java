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
import java.sql.SQLException;

import javax.sql.RowSet;
import javax.sql.rowset.FilteredRowSet;
import javax.sql.rowset.Predicate;

public class FilteredRowSetTest extends CachedRowSetTestCase {

    public final static int EVALUATE_DEFAULT = 0;

    public final static int EVALUATE_ROWSET = 1;

    public final static int EVALUATE_INSERT = 2;

    public void testCreateShared() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        Predicate range = new RangeOne();
        filteredRowSet.setFilter(range);

        FilteredRowSet copyFilteredRs = (FilteredRowSet) filteredRowSet
                .createShared();
        assertSame(range, copyFilteredRs.getFilter());
    }

    public void testCreateCopy() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        Predicate range = new RangeOne();
        filteredRowSet.setFilter(range);

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            filteredRowSet.createCopy();
        } else {
            try {
                filteredRowSet.createCopy();
                fail("should throw SQLException");
            } catch (SQLException e) {
                // expected
            }
        }
    }

    public void testSetFilter() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        /*
         * Set Filter: RangeOne
         */
        Predicate range = new RangeOne();
        filteredRowSet.setFilter(range);
        filteredRowSet.beforeFirst();
        int index = 0;
        while (filteredRowSet.next()) {
            index++;
            assertTrue(filteredRowSet.getString(2).indexOf("test") != -1);
        }
        assertEquals(3, index);
        assertSame(range, filteredRowSet.getFilter());

        /*
         * Set another Filter: RangeTwo
         */
        range = new RangeTwo();
        filteredRowSet.setFilter(range);
        filteredRowSet.beforeFirst();
        index = 0;
        boolean isSecondRowFiltered = true;
        while (filteredRowSet.next()) {
            index++;
            if ("test2".equals(filteredRowSet.getString(2))) {
                isSecondRowFiltered = false;
            }
        }
        assertEquals(3, index);
        assertTrue(isSecondRowFiltered);
        assertSame(range, filteredRowSet.getFilter());

        /*
         * Remove Filter
         */
        filteredRowSet.setFilter(null);
        filteredRowSet.beforeFirst();
        index = 0;
        while (filteredRowSet.next()) {
            index++;
            assertEquals(index, filteredRowSet.getInt(1));
        }
        assertEquals(4, index);
        assertNull(filteredRowSet.getFilter());
    }

    public void testSetFilterOnFilteredRow() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        filteredRowSet.next();
        assertEquals(1, filteredRowSet.getRow());
        Predicate range = new RangeOne();
        filteredRowSet.setFilter(range);

        // not filtered
        assertEquals(1, filteredRowSet.getRow());
        assertEquals("hermit", filteredRowSet.getString(2));

        filteredRowSet.updateString(2, "update");
        assertEquals("update", filteredRowSet.getString(2));

        filteredRowSet.next();
        assertEquals(2, filteredRowSet.getRow());
        assertEquals("test", filteredRowSet.getString(2));

        filteredRowSet.previous();
        assertTrue(filteredRowSet.isBeforeFirst());

        try {
            filteredRowSet.getString(2);
            fail("should should SQLException");
        } catch (SQLException e) {
            // expected, Not a valid cursor
        }

        filteredRowSet.setFilter(null);

        assertTrue(filteredRowSet.isBeforeFirst());

        assertTrue(filteredRowSet.next());
        assertEquals("update", filteredRowSet.getString(2));

        filteredRowSet.setFilter(range);
        assertEquals("update", filteredRowSet.getString(2));
        filteredRowSet.updateString(2, "testUpdate");

        filteredRowSet.next();
        assertEquals(2, filteredRowSet.getRow());
        assertEquals("test", filteredRowSet.getString(2));

        filteredRowSet.previous();
        assertEquals(1, filteredRowSet.getRow());
        assertEquals("testUpdate", filteredRowSet.getString(2));

    }

    public void testPopulate() throws Exception {
        /*
         * Set Filter before populate()
         */
        RangeOne range = new RangeOne();
        FilteredRowSet filteredRowSet = newFilterRowSet();
        filteredRowSet.setFilter(range);
        assertSame(range, filteredRowSet.getFilter());

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        assertTrue(filteredRowSet.first());
        assertEquals(2, filteredRowSet.getInt(1));

        filteredRowSet.setFilter(null);
        assertTrue(filteredRowSet.first());
        assertEquals(1, filteredRowSet.getInt(1));
    }

    public void testAbsolute() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);
        /*
         * When running on RI, filteredRowSet.absolute(1) is false here.
         * However, the cursor moves to the first row already.
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(filteredRowSet.absolute(1));
        } else {
            assertFalse(filteredRowSet.absolute(1));
        }
        assertEquals("hermit", filteredRowSet.getString(2));

        RangeOne range = new RangeOne();
        filteredRowSet.setFilter(range);

        /*
         * When running on RI, filteredRowSet.absolute(1) is false.
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(filteredRowSet.absolute(1));
        } else {
            assertFalse(filteredRowSet.absolute(1));
        }
        assertEquals("test", filteredRowSet.getString(2));
        range.clear();
        assertTrue(filteredRowSet.absolute(2));
        assertEquals(3, filteredRowSet.getRow());
        assertEquals(3, filteredRowSet.getInt(1));
        assertEquals(EVALUATE_ROWSET, range.getTag());
        assertEquals(3, range.getCount());

        range.clear();
        assertTrue(filteredRowSet.absolute(2));
        assertEquals(3, filteredRowSet.getInt(1));
        assertEquals(EVALUATE_ROWSET, range.getTag());
        assertEquals(3, range.getCount());

        range.clear();
        assertTrue(filteredRowSet.absolute(3));
        assertEquals(4, filteredRowSet.getInt(1));
        assertEquals(EVALUATE_ROWSET, range.getTag());
        assertEquals(4, range.getCount());

        assertFalse(filteredRowSet.absolute(4));

        assertTrue(filteredRowSet.absolute(-2));
        assertEquals(3, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.absolute(-3));
        assertEquals(2, filteredRowSet.getInt(1));

        assertFalse(filteredRowSet.absolute(-4));
        assertTrue(filteredRowSet.isBeforeFirst());
    }

    public void testRelative() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        assertTrue(filteredRowSet.absolute(3));
        assertEquals(3, filteredRowSet.getInt(1));
        assertFalse(filteredRowSet.relative(2));

        assertTrue(filteredRowSet.absolute(3));
        assertEquals(3, filteredRowSet.getInt(1));
        assertTrue(filteredRowSet.relative(1));
        assertEquals(4, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.absolute(3));
        assertEquals(3, filteredRowSet.getInt(1));
        assertTrue(filteredRowSet.relative(-1));
        assertEquals(2, filteredRowSet.getInt(1));
        assertFalse(filteredRowSet.relative(-2));

        RangeOne range = new RangeOne();
        filteredRowSet.setFilter(range);

        filteredRowSet.beforeFirst();
        assertTrue(filteredRowSet.relative(2));
        assertEquals(3, filteredRowSet.getInt(1));
        assertEquals(3, filteredRowSet.getRow());
        assertFalse(filteredRowSet.relative(-2));
        assertTrue(filteredRowSet.isBeforeFirst());
    }

    public void testCursorMove() throws Exception {
        insertMoreData(30);
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);
        RangeThree range = new RangeThree();
        filteredRowSet.setFilter(range);

        assertTrue(filteredRowSet.first());
        assertEquals("hermit", filteredRowSet.getString(2));
        /*
         * TODO It's really strange. When running on RI, filteredRowSet.first()
         * is true. The cursor stays on the first row. However,
         * filteredRowSet.absolute(1) is false. But the cursor still stays on
         * the first row.
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            assertTrue(filteredRowSet.absolute(1));
        } else {
            assertFalse(filteredRowSet.absolute(1));
        }
        assertEquals("hermit", filteredRowSet.getString(2));
        assertTrue(filteredRowSet.relative(1));
        assertEquals("test4", filteredRowSet.getString(2));
        /*
         * First call absolute(2), then call relative(-1), the cursor returns
         * the first row.
         */
        assertTrue(filteredRowSet.absolute(2));
        assertEquals("test4", filteredRowSet.getString(2));
        assertTrue(filteredRowSet.relative(-1));
        assertEquals("hermit", filteredRowSet.getString(2));

        assertTrue(filteredRowSet.last());
        assertEquals("test34", filteredRowSet.getString(2));

        assertTrue(filteredRowSet.previous());
        assertEquals("test32", filteredRowSet.getString(2));

        assertTrue(filteredRowSet.relative(-1));
        assertEquals("test30", filteredRowSet.getString(2));

        assertTrue(filteredRowSet.absolute(5));
        assertEquals("test10", filteredRowSet.getString(2));

        assertTrue(filteredRowSet.relative(3));
        assertEquals("test16", filteredRowSet.getString(2));

        assertTrue(filteredRowSet.relative(-3));
        assertEquals("test10", filteredRowSet.getString(2));

        assertTrue(filteredRowSet.relative(4));
        assertEquals("test18", filteredRowSet.getString(2));

        assertTrue(filteredRowSet.relative(13));
        assertEquals("test34", filteredRowSet.getString(2));

        assertFalse(filteredRowSet.next());
        assertTrue(filteredRowSet.isAfterLast());

        assertTrue(filteredRowSet.absolute(22));
        assertEquals("test34", filteredRowSet.getString(2));

        assertTrue(filteredRowSet.relative(-21));
        assertEquals("hermit", filteredRowSet.getString(2));

        assertFalse(filteredRowSet.relative(-1));
        assertTrue(filteredRowSet.isBeforeFirst());
    }

    public void testNextAndPrevious() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        RangeOne range = new RangeOne();
        filteredRowSet.setFilter(range);

        assertTrue(filteredRowSet.next());
        assertEquals(2, filteredRowSet.getInt(1));
        assertEquals(EVALUATE_ROWSET, range.getTag());
        assertEquals(2, range.getCount());
        range.clear();
        assertTrue(filteredRowSet.next());
        assertEquals(3, filteredRowSet.getInt(1));
        assertEquals(EVALUATE_ROWSET, range.getTag());
        assertEquals(1, range.getCount());

        range.clear();
        assertTrue(filteredRowSet.previous());
        assertEquals(2, filteredRowSet.getInt(1));
        assertEquals(1, range.getCount());
        range.clear();
        assertFalse(filteredRowSet.previous());
        assertEquals(EVALUATE_ROWSET, range.getTag());
    }

    public void testNoFilter_Insert() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        /*
         * TODO Call updateXXX() would throw NullPointerException on insert row
         * when running on RI.
         */
        filteredRowSet.moveToInsertRow();
        filteredRowSet.updateInt(1, 10);
        filteredRowSet.updateString(2, "insert10");
        filteredRowSet.insertRow();
        filteredRowSet.moveToCurrentRow();
    }

    public void testFilter_Insert() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        Predicate range = new RangeOne();
        filteredRowSet.setFilter(range);

        /*
         * Insert a row. when call updateXXX(), evaluate(Object value, int
         * column) is called to check first.
         */
        filteredRowSet.afterLast();
        filteredRowSet.moveToInsertRow();
        filteredRowSet.updateInt(1, 200);
        try {
            filteredRowSet.updateString("NAME", "test200");
            fail("should throw SQLException");
        } catch (SQLException e) {
            filteredRowSet.updateString("NAME", "insert200");
        }
        filteredRowSet.insertRow();
        filteredRowSet.moveToCurrentRow();

        /*
         * Although the new row is inserted, it is invalid through
         * evaluate(RowSet rs). Therefore, the inserted row is not visible.
         */
        filteredRowSet.beforeFirst();
        int index = 0;
        while (filteredRowSet.next()) {
            index++;
            assertEquals(index + 1, filteredRowSet.getInt(1));
        }
        assertEquals(3, index);

        /*
         * Remove filter. See the inserted row. Then set again, and commit to
         * database.
         */
        filteredRowSet.setFilter(null);
        assertTrue(filteredRowSet.last());
        assertEquals(200, filteredRowSet.getInt(1));
        assertTrue(filteredRowSet.rowInserted());
        filteredRowSet.setFilter(range);
        filteredRowSet.acceptChanges(conn);
        // check database: the inserted row isn't commited to database
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        index = 0;
        while (rs.next()) {
            index++;
            assertEquals(index, rs.getInt(1));
        }
        assertEquals(4, index);

        /*
         * Remove filter
         */
        filteredRowSet.setFilter(null);
        filteredRowSet.beforeFirst();
        index = 0;
        while (filteredRowSet.next()) {
            index++;
            if (index == 5) {
                /*
                 * Though the new row isn't inserted into database, the inserted
                 * row lost it's status after acceptChanges().
                 */
                assertEquals(200, filteredRowSet.getInt(1));
                assertFalse(filteredRowSet.rowInserted());
            } else {
                assertEquals(index, filteredRowSet.getInt(1));
            }
        }
        assertEquals(5, index);
    }

    public void testFilter_Update() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        Predicate range = new RangeOne();
        filteredRowSet.setFilter(range);

        /*
         * Update the third row. Filter has no effect here.
         */
        assertTrue(filteredRowSet.last());
        assertEquals("test4", filteredRowSet.getString(2));
        filteredRowSet.updateString(2, "update4");
        filteredRowSet.updateRow();
        assertEquals("update4", filteredRowSet.getString(2));
        // the updated row becomes not visible through filter
        assertTrue(filteredRowSet.last());
        assertEquals("test3", filteredRowSet.getString(2));

        // commit to database
        filteredRowSet.acceptChanges(conn);
        rs = st
                .executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE NAME = 'update4'");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));

        /*
         * Remove filter
         */
        filteredRowSet.setFilter(null);
        assertTrue(filteredRowSet.last());
        assertEquals("update4", filteredRowSet.getString(2));
        /*
         * The forth row isn't updated to database, and it lost it's status
         * after acceptChanges().
         */
        assertFalse(filteredRowSet.rowUpdated());
    }

    public void testFilter_Delete() throws Exception {
        FilteredRowSet filteredRowSet = newFilterRowSet();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        filteredRowSet.populate(rs);

        /*
         * Mark the first row as delete.
         */
        assertTrue(filteredRowSet.first());
        assertEquals(1, filteredRowSet.getInt(1));
        filteredRowSet.deleteRow();

        Predicate range = new RangeOne();
        filteredRowSet.setFilter(range);

        assertTrue(filteredRowSet.first());
        assertEquals(2, filteredRowSet.getInt(1));

        filteredRowSet.acceptChanges(conn);
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 1");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));

        /*
         * Remove filter
         */
        filteredRowSet.setFilter(null);
        filteredRowSet.acceptChanges(conn);
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 1");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
        /*
         * The first row has been deleted from FilteredRowSet. However, it isn't
         * deleted from database.
         */
        filteredRowSet.setShowDeleted(true);
        assertTrue(filteredRowSet.first());
        assertEquals(2, filteredRowSet.getInt(1));
    }

    public void testPaging() throws Exception {
        insertMoreData(4);

        FilteredRowSet filteredRowSet = newFilterRowSet();

        filteredRowSet.setCommand("select * from USER_INFO");
        filteredRowSet.setUrl(DERBY_URL);

        filteredRowSet.setPageSize(3);
        filteredRowSet.execute();

        Predicate filter = new OddRowFilter();

        filteredRowSet.setFilter(filter);

        assertEquals("select * from USER_INFO", filteredRowSet.getCommand());

        assertFalse(filteredRowSet.previousPage());

        assertTrue(filteredRowSet.next());
        assertEquals(1, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.next());
        assertEquals(3, filteredRowSet.getInt(1));

        assertFalse(filteredRowSet.next());
        assertTrue(filteredRowSet.isAfterLast());

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(filteredRowSet.nextPage());
        }

        int index = 5;
        while (filteredRowSet.nextPage()) {
            while (filteredRowSet.next()) {
                assertEquals(index, filteredRowSet.getInt(1));
                index += 2;
            }
        }

        assertEquals(9, index);

        filteredRowSet = newFilterRowSet();
        filteredRowSet.setCommand("select * from USER_INFO");
        filteredRowSet.setUrl(DERBY_URL);

        filteredRowSet.setPageSize(3);
        filteredRowSet.execute();

        filteredRowSet.setFilter(filter);

        assertTrue(filteredRowSet.next());
        assertEquals(1, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.nextPage());
        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(filteredRowSet.nextPage());
        }

        assertTrue(filteredRowSet.next());

        assertEquals(5, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.nextPage());

        assertTrue(filteredRowSet.next());
        assertEquals(7, filteredRowSet.getInt(1));

        assertFalse(filteredRowSet.nextPage());

    }

    public void testPagingInMemory() throws Exception {
        insertMoreData(10);
        FilteredRowSet filteredRowSet = newFilterRowSet();

        st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        rs = st.executeQuery("SELECT * FROM USER_INFO");

        // the max rows load into memory
        filteredRowSet.setMaxRows(5);
        filteredRowSet.setPageSize(3);

        filteredRowSet.populate(rs, 1);

        OddRowFilter filter = new OddRowFilter();
        filteredRowSet.setFilter(filter);

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(filteredRowSet.nextPage());
        }

        assertTrue(filteredRowSet.next());
        assertEquals(1, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.next());
        assertEquals(3, filteredRowSet.getInt(1));

        assertFalse(filteredRowSet.next());
        assertTrue(filteredRowSet.isAfterLast());

        assertTrue(filteredRowSet.nextPage());

        filteredRowSet.beforeFirst();

        assertTrue(filteredRowSet.next());
        assertEquals(5, filteredRowSet.getInt(1));

        assertFalse(filteredRowSet.next());
        assertTrue(filteredRowSet.isAfterLast());

        assertFalse(filteredRowSet.nextPage());

        st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        rs = st.executeQuery("SELECT * FROM USER_INFO");

        filteredRowSet = newFilterRowSet();
        filteredRowSet.setFilter(filter);
        filteredRowSet.setPageSize(3);

        filteredRowSet.populate(rs, 1);

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(filteredRowSet.nextPage());
        }

        assertTrue(filteredRowSet.next());
        assertEquals(1, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.next());
        assertEquals(3, filteredRowSet.getInt(1));

        assertFalse(filteredRowSet.next());
        assertTrue(filteredRowSet.isAfterLast());

        filteredRowSet.setPageSize(2);
        assertTrue(filteredRowSet.nextPage());

        assertTrue(filteredRowSet.isBeforeFirst());

        assertTrue(filteredRowSet.next());
        assertEquals(5, filteredRowSet.getInt(1));

        assertFalse(filteredRowSet.next());
        assertTrue(filteredRowSet.isAfterLast());

        filteredRowSet.setPageSize(5);
        assertTrue(filteredRowSet.nextPage());

        assertTrue(filteredRowSet.isBeforeFirst());

        assertTrue(filteredRowSet.next());
        assertEquals(7, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.next());
        assertEquals(9, filteredRowSet.getInt(1));

        assertFalse(filteredRowSet.next());
        assertTrue(filteredRowSet.isAfterLast());

        assertTrue(filteredRowSet.previousPage());

        assertTrue(filteredRowSet.isBeforeFirst());

        assertTrue(filteredRowSet.next());
        assertEquals(1, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.next());
        assertEquals(3, filteredRowSet.getInt(1));

        assertTrue(filteredRowSet.next());
        assertEquals(5, filteredRowSet.getInt(1));

        assertFalse(filteredRowSet.next());
        assertTrue(filteredRowSet.isAfterLast());

        assertFalse(filteredRowSet.previousPage());
    }

    protected FilteredRowSet newFilterRowSet() throws Exception {
        try {
            return (FilteredRowSet) Class.forName(
                    "com.sun.rowset.FilteredRowSetImpl").newInstance();
        } catch (ClassNotFoundException e) {
            return (FilteredRowSet) Class
                    .forName(
                            "org.apache.harmony.sql.internal.rowset.FilteredRowSetImpl")
                    .newInstance();
        }
    }
}

class RangeOne implements Predicate, Cloneable {

    private boolean isPrint;

    private int tag;

    private int count;

    public void setPrint(boolean isPrint) {
        this.isPrint = isPrint;
    }

    public int getTag() {
        return tag;
    }

    public int getCount() {
        return count;
    }

    public void clear() {
        tag = FilteredRowSetTest.EVALUATE_DEFAULT;
        count = 0;
    }

    public boolean evaluate(RowSet rs) {
        tag = FilteredRowSetTest.EVALUATE_ROWSET;
        count++;
        if (isPrint) {
            System.out.println("RangeOne.evaluate(RowSet rs)");
        }
        try {
            if (rs.getString(2).indexOf("test") != -1) {
                return true;
            }
        } catch (SQLException e) {
            // e.printStackTrace();
        }
        return false;
    }

    public boolean evaluate(Object value, int column) throws SQLException {
        /*
         * This method is internally called by FilteredRowSet while inserting
         * new rows.
         */
        tag = FilteredRowSetTest.EVALUATE_INSERT;
        count++;
        if (column == 2) {
            return value.toString().indexOf("insert") != -1;
        }
        return true;
    }

    public boolean evaluate(Object value, String columnName)
            throws SQLException {
        /*
         * This method is internally called by FilteredRowSet while inserting
         * new rows. However, even the second parameter is columnName,
         * FilteredRowSet still calls evaluate(Object value, int column).
         */
        tag = FilteredRowSetTest.EVALUATE_INSERT;
        count++;
        return false;
    }

    public RangeOne clone() throws CloneNotSupportedException {
        return (RangeOne) super.clone();
    }
}

class RangeTwo implements Predicate {
    public boolean evaluate(RowSet rs) {
        try {
            if (rs.getInt(1) > 2 || "hermit".equals(rs.getString(2))) {
                return true;
            }
        } catch (SQLException e) {
            // e.printStackTrace();
        }
        return false;
    }

    public boolean evaluate(Object value, int column) throws SQLException {
        return false;
    }

    public boolean evaluate(Object value, String columnName)
            throws SQLException {
        return false;
    }
}

class RangeThree implements Predicate {

    public boolean evaluate(RowSet rs) {
        try {
            String name = rs.getString(2);
            if ("hermit".equals(name) || name.indexOf("2") != -1
                    || name.indexOf("4") != -1 || name.indexOf("6") != -1
                    || name.indexOf("8") != -1 || name.indexOf("0") != -1) {
                return true;
            }
        } catch (SQLException e) {
            // do nothing
        }
        return false;
    }

    public boolean evaluate(Object value, int column) throws SQLException {
        return false;
    }

    public boolean evaluate(Object value, String columnName)
            throws SQLException {
        return false;
    }

}

class OddRowFilter implements Predicate {
    public boolean evaluate(RowSet rs) {
        try {
            return (rs.getInt(1) & 1) == 1;
        } catch (SQLException e) {
            // do nothing
        }
        return false;
    }

    public boolean evaluate(Object value, int column) throws SQLException {
        return false;
    }

    public boolean evaluate(Object value, String columnName)
            throws SQLException {
        return false;
    }
}
