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

/**
 * TODO In RI, nextPage is similar next, first call nextPage move to first page,
 * while in spec, first call nextPage should move to second page, Harmony follow
 * spec.
 * 
 */
public class CachedRowSetPagingTest extends CachedRowSetTestCase {

    public void testPagingInMemory() throws Exception {
        insertMoreData(10);
        crset = newNoInitialInstance();
        Listener listener = new Listener();
        crset.addRowSetListener(listener);

        st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        rs = st.executeQuery("SELECT * FROM USER_INFO");

        // the max rows load into memory
        crset.setMaxRows(5);
        crset.setPageSize(3);

        crset.populate(rs, 1);

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(crset.nextPage());
        }

        for (int i = 1; i <= 3; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        listener.clear();
        assertTrue(crset.nextPage());
        assertEquals(CachedRowSetListenerTest.EVENT_ROWSET_CHANGED, listener
                .getTag());

        crset.beforeFirst();

        for (int i = 4; i <= 5; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }
        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        assertFalse(crset.nextPage());

    }

    public void testPagingInMemory2() throws Exception {
        insertMoreData(4);

        crset = newNoInitialInstance();
        Listener listener = new Listener();
        crset.addRowSetListener(listener);

        st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        rs = st.executeQuery("SELECT * FROM USER_INFO");

        crset.setPageSize(3);

        crset.populate(rs, 1);

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(crset.nextPage());
        }

        for (int i = 1; i <= 3; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        crset.setPageSize(2);
        assertTrue(crset.nextPage());

        assertTrue(crset.isBeforeFirst());

        for (int i = 4; i <= 5; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        crset.setPageSize(5);
        assertTrue(crset.nextPage());

        assertTrue(crset.isBeforeFirst());

        for (int i = 6; i <= 8; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());
        listener.clear();
        assertFalse(crset.nextPage());
        assertNull(listener.getTag());

        listener.clear();
        assertTrue(crset.previousPage());
        assertEquals(CachedRowSetListenerTest.EVENT_ROWSET_CHANGED, listener
                .getTag());

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need previousPage one more time
            assertTrue(crset.previousPage());
        }

        assertTrue(crset.isBeforeFirst());
        for (int i = 1; i <= 5; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        assertFalse(crset.previousPage());

    }

    public void testPagingInMemory3() throws Exception {
        insertMoreData(10);

        crset = newNoInitialInstance();

        st = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
        rs = st.executeQuery("SELECT * FROM USER_INFO");

        crset.setPageSize(4);

        crset.populate(rs, 1);

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(crset.nextPage());
        }

        for (int i = 1; i <= 4; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());
        assertFalse(crset.previousPage());
        assertFalse(crset.previousPage());

        assertTrue(crset.nextPage());

        assertTrue(crset.isBeforeFirst());
        for (int i = 5; i <= 8; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        crset.setPageSize(3);
        assertTrue(crset.previousPage());

        assertTrue(crset.isBeforeFirst());

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            for (int i = 2; i <= 4; ++i) {
                assertTrue(crset.next());
                assertEquals(i, crset.getInt(1));
            }

            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            assertTrue(crset.previousPage());

            assertTrue(crset.isBeforeFirst());

            assertTrue(crset.next());
            assertEquals(1, crset.getInt(1));
            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            assertFalse(crset.previousPage());
            assertFalse(crset.previousPage());

            assertTrue(crset.nextPage());

            assertTrue(crset.isBeforeFirst());
            for (int i = 2; i <= 4; ++i) {
                assertTrue(crset.next());
                assertEquals(i, crset.getInt(1));
            }

            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            assertTrue(crset.nextPage());

            assertTrue(crset.isBeforeFirst());
            for (int i = 5; i <= 7; ++i) {
                assertTrue(crset.next());
                assertEquals(i, crset.getInt(1));
            }

            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            crset.setPageSize(4);
            assertEquals(4, crset.getPageSize());

            assertTrue(crset.previousPage());

            assertTrue(crset.isBeforeFirst());
            for (int i = 1; i <= 4; ++i) {
                assertTrue(crset.next());
                assertEquals(i, crset.getInt(1));
            }

            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            assertFalse(crset.previousPage());
            assertFalse(crset.previousPage());

        } else {
            // seems RI lost one row
            assertTrue(crset.next());
            assertEquals(1, crset.getInt(1));
            assertTrue(crset.next());
            assertEquals(2, crset.getInt(1));
            assertTrue(crset.next());
            assertEquals(3, crset.getInt(1));
            assertFalse(crset.next());
        }

    }

    public void testException() throws Exception {
        insertMoreData(4);

        crset = newNoInitialInstance();

        try {
            crset.nextPage();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Populate data before calling
        }

        crset.setCommand("select * from USER_INFO");
        crset.setUrl(DERBY_URL);

        crset.setPageSize(0);
        crset.execute();

        try {
            crset.nextPage();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Populate data before calling
        }
    }

    public void testTravel() throws Exception {
        insertMoreData(4);

        crset = newNoInitialInstance();

        crset.setCommand("select * from USER_INFO");
        crset.setUrl(DERBY_URL);

        crset.setPageSize(3);
        crset.execute();

        assertEquals("select * from USER_INFO", crset.getCommand());

        assertFalse(crset.previousPage());

        for (int i = 1; i <= 3; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(crset.nextPage());
        }

        int index = 4;
        while (crset.nextPage()) {
            while (crset.next()) {
                assertEquals(index, crset.getInt(1));
                index++;
            }
        }
        assertEquals(9, index);

        crset = newNoInitialInstance();
        crset.setCommand("select * from USER_INFO");
        crset.setUrl(DERBY_URL);

        crset.setPageSize(3);
        crset.execute();

        assertTrue(crset.next());
        assertEquals(1, crset.getInt(1));

        assertTrue(crset.nextPage());
        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(crset.nextPage());
        }

        assertTrue(crset.next());

        assertEquals(4, crset.getInt(1));

        assertTrue(crset.nextPage());

        assertTrue(crset.next());
        assertEquals(7, crset.getInt(1));

        assertFalse(crset.nextPage());

    }

    public void testTravel2() throws Exception {
        insertMoreData(4);

        crset = newNoInitialInstance();

        crset.setCommand("select * from USER_INFO");
        crset.setUrl(DERBY_URL);

        crset.setPageSize(3);
        crset.execute();

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(crset.nextPage());
        }

        for (int i = 1; i <= 3; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        crset.setPageSize(2);
        assertTrue(crset.nextPage());

        assertTrue(crset.isBeforeFirst());

        for (int i = 4; i <= 5; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        crset.setPageSize(5);
        assertTrue(crset.nextPage());

        assertTrue(crset.isBeforeFirst());

        for (int i = 6; i <= 8; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());
        assertFalse(crset.nextPage());

        assertTrue(crset.previousPage());

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need previousPage one more time
            assertTrue(crset.previousPage());
        }

        assertTrue(crset.isBeforeFirst());
        for (int i = 1; i <= 5; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        assertFalse(crset.previousPage());

    }

    public void testTravel3() throws Exception {
        insertMoreData(10);

        crset = newNoInitialInstance();

        crset.setCommand("select * from USER_INFO");
        crset.setUrl(DERBY_URL);

        crset.setPageSize(4);
        crset.execute();

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            // RI need nextPage one more time
            assertTrue(crset.nextPage());
        }

        for (int i = 1; i <= 4; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());
        assertFalse(crset.previousPage());
        assertFalse(crset.previousPage());

        assertTrue(crset.nextPage());

        assertTrue(crset.isBeforeFirst());
        for (int i = 5; i <= 8; ++i) {
            assertTrue(crset.next());
            assertEquals(i, crset.getInt(1));
        }

        assertFalse(crset.next());
        assertTrue(crset.isAfterLast());

        crset.setPageSize(3);
        assertTrue(crset.previousPage());

        assertTrue(crset.isBeforeFirst());

        if ("true".equals(System.getProperty("Testing Harmony"))) {
            for (int i = 2; i <= 4; ++i) {
                assertTrue(crset.next());
                assertEquals(i, crset.getInt(1));
            }

            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            assertTrue(crset.previousPage());

            assertTrue(crset.isBeforeFirst());

            assertTrue(crset.next());
            assertEquals(1, crset.getInt(1));
            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            assertFalse(crset.previousPage());
            assertFalse(crset.previousPage());

            assertTrue(crset.nextPage());

            assertTrue(crset.isBeforeFirst());
            for (int i = 2; i <= 4; ++i) {
                assertTrue(crset.next());
                assertEquals(i, crset.getInt(1));
            }

            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            assertTrue(crset.nextPage());

            assertTrue(crset.isBeforeFirst());
            for (int i = 5; i <= 7; ++i) {
                assertTrue(crset.next());
                assertEquals(i, crset.getInt(1));
            }

            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            crset.setPageSize(4);
            assertEquals(4, crset.getPageSize());

            assertTrue(crset.previousPage());

            assertTrue(crset.isBeforeFirst());
            for (int i = 1; i <= 4; ++i) {
                assertTrue(crset.next());
                assertEquals(i, crset.getInt(1));
            }

            assertFalse(crset.next());
            assertTrue(crset.isAfterLast());

            assertFalse(crset.previousPage());
            assertFalse(crset.previousPage());

        } else {
            // TODO seems RI lost one row
            assertTrue(crset.next());
            assertEquals(1, crset.getInt(1));
            assertTrue(crset.next());
            assertEquals(2, crset.getInt(1));
            assertTrue(crset.next());
            assertEquals(3, crset.getInt(1));
            assertFalse(crset.next());
        }

    }
}
