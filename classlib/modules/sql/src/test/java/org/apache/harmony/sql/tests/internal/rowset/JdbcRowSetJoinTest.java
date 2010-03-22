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

import java.sql.SQLException;

import javax.sql.rowset.JdbcRowSet;

public class JdbcRowSetJoinTest extends CachedRowSetTestCase {

    public void testSetMatchColumn_Name() throws Exception {
        String name = null;
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        JdbcRowSet jrs = newJdbcRowSet();
        /*
         * TODO spec: throw SQLException, RI throw NullPointerException, Harmony
         * follow spec
         */
        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            try {
                noInitalJrs.setMatchColumn(name);
                fail("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            }
        } else {

            try {
                noInitalJrs.setMatchColumn(name);
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, Match columns should not be empty or null string
            }
        }

        try {
            noInitalJrs.setMatchColumn("");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Match columns should not be empty or null string
        }

        noInitalJrs.setMatchColumn("not exist");
        String[] names = noInitalJrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(10, names.length);
        assertEquals("not exist", names[0]);
        for (int i = 1; i < names.length; i++) {
            assertNull(names[i]);
        }

        noInitalJrs.setMatchColumn("id");
        names = noInitalJrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(10, names.length);
        assertEquals("id", names[0]);
        for (int i = 1; i < names.length; i++) {
            assertNull(names[i]);
        }

        noInitalJrs.setMatchColumn(new String[] { "ID", "NAME" });

        names = null;

        try {
            noInitalJrs.setMatchColumn(names);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        names = noInitalJrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(12, names.length);
        assertEquals("ID", names[0]);
        assertEquals("NAME", names[1]);
        assertEquals("id", names[2]);

        for (int i = 3; i < names.length; i++) {
            assertNull(names[i]);
        }

        try {
            noInitalJrs.setMatchColumn(new String[] { "ID", "" });
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Match columns should not be empty or null string
        }

        try {
            noInitalJrs.setMatchColumn(new String[] { "ID", null });
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Match columns should not be empty or null string
        }

        noInitalJrs.setMatchColumn("NAME");
        names = noInitalJrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(12, names.length);
        assertEquals("NAME", names[0]);
        assertEquals("NAME", names[1]);
        assertEquals("id", names[2]);

        for (int i = 3; i < names.length; i++) {
            assertNull(names[i]);
        }

        if (!"true".equals(System.getProperty("Testing Harmony"))) {
            try {
                jrs.setMatchColumn(name);
                fail("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            }
        } else {

            try {
                jrs.setMatchColumn(name);
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, Match columns should not be empty or null string
            }
        }

        try {
            jrs.setMatchColumn("");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Match columns should not be empty or null string
        }

        jrs.setMatchColumn("not exist");
        names = jrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(10, names.length);
        assertEquals("not exist", names[0]);
        for (int i = 1; i < names.length; i++) {
            assertNull(names[i]);
        }

        jrs.setMatchColumn("id");
        names = jrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(10, names.length);
        assertEquals("id", names[0]);
        for (int i = 1; i < names.length; i++) {
            assertNull(names[i]);
        }

        jrs.setMatchColumn(new String[] { "ID", "NAME" });
        names = jrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(12, names.length);
        assertEquals("ID", names[0]);
        assertEquals("NAME", names[1]);
        assertEquals("id", names[2]);

        for (int i = 3; i < names.length; i++) {
            assertNull(names[i]);
        }

        try {
            jrs.setMatchColumn(new String[] { "ID", "" });
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Match columns should not be empty or null string
        }

        try {
            jrs.setMatchColumn(new String[] { "ID", null });
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Match columns should not be empty or null string
        }

        jrs.setMatchColumn("NAME");
        names = jrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(12, names.length);
        assertEquals("NAME", names[0]);
        assertEquals("NAME", names[1]);
        assertEquals("id", names[2]);

        for (int i = 3; i < names.length; i++) {
            assertNull(names[i]);
        }

    }

    public void testSetMatchColumn_Index() throws Exception {
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        JdbcRowSet jrs = newJdbcRowSet();
        try {
            noInitalJrs.setMatchColumn(-2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Match columns should be greater than 0
        }

        int[] indexes = null;

        try {
            noInitalJrs.setMatchColumn(indexes);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        // TODO 0 is valid index 0f column?
        noInitalJrs.setMatchColumn(0);
        indexes = noInitalJrs.getMatchColumnIndexes();

        assertNotNull(indexes);
        assertEquals(10, indexes.length);
        for (int i = 0; i < indexes.length; i++) {
            if (i == 0) {
                assertEquals(0, indexes[i]);
            } else {
                assertEquals(-1, indexes[i]);
            }
        }

        noInitalJrs.setMatchColumn(1);
        indexes = noInitalJrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(10, indexes.length);
        for (int i = 0; i < indexes.length; i++) {
            if (i == 0) {
                assertEquals(1, indexes[i]);
            } else {
                assertEquals(-1, indexes[i]);
            }
        }

        noInitalJrs.setMatchColumn(new int[] { 3, 4, 5 });
        indexes = noInitalJrs.getMatchColumnIndexes();

        try {
            noInitalJrs.setMatchColumn(new int[] { 3, -3 });
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Match columns should be greater than 0
        }

        assertNotNull(indexes);
        assertEquals(13, indexes.length);
        assertEquals(3, indexes[0]);
        assertEquals(4, indexes[1]);
        assertEquals(5, indexes[2]);
        assertEquals(1, indexes[3]);

        for (int i = 4; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        noInitalJrs.setMatchColumn(6);
        indexes = noInitalJrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(13, indexes.length);
        assertEquals(6, indexes[0]);
        assertEquals(4, indexes[1]);
        assertEquals(5, indexes[2]);
        assertEquals(1, indexes[3]);

        noInitalJrs.setMatchColumn(new int[] { 7, 8 });
        indexes = noInitalJrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(15, indexes.length);
        assertEquals(7, indexes[0]);
        assertEquals(8, indexes[1]);
        assertEquals(6, indexes[2]);
        assertEquals(4, indexes[3]);
        assertEquals(5, indexes[4]);
        assertEquals(1, indexes[5]);

        for (int i = 6; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        noInitalJrs.setMatchColumn(9);
        indexes = noInitalJrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(15, indexes.length);
        assertEquals(9, indexes[0]);
        assertEquals(8, indexes[1]);
        assertEquals(6, indexes[2]);
        assertEquals(4, indexes[3]);
        assertEquals(5, indexes[4]);
        assertEquals(1, indexes[5]);

        for (int i = 6; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        try {
            jrs.setMatchColumn(-2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Match columns should be greater than 0
        }

        indexes = null;

        try {
            jrs.setMatchColumn(indexes);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        jrs.setMatchColumn(0);
        indexes = jrs.getMatchColumnIndexes();

        assertNotNull(indexes);
        assertEquals(10, indexes.length);
        for (int i = 0; i < indexes.length; i++) {
            if (i == 0) {
                assertEquals(0, indexes[i]);
            } else {
                assertEquals(-1, indexes[i]);
            }
        }

        jrs.setMatchColumn(1);
        indexes = jrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(10, indexes.length);
        for (int i = 0; i < indexes.length; i++) {
            if (i == 0) {
                assertEquals(1, indexes[i]);
            } else {
                assertEquals(-1, indexes[i]);
            }
        }

        jrs.setMatchColumn(new int[] { 3, 4, 5 });
        indexes = jrs.getMatchColumnIndexes();

        assertNotNull(indexes);
        assertEquals(13, indexes.length);
        assertEquals(3, indexes[0]);
        assertEquals(4, indexes[1]);
        assertEquals(5, indexes[2]);
        assertEquals(1, indexes[3]);

        for (int i = 4; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        jrs.setMatchColumn(6);
        indexes = jrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(13, indexes.length);
        assertEquals(6, indexes[0]);
        assertEquals(4, indexes[1]);
        assertEquals(5, indexes[2]);
        assertEquals(1, indexes[3]);

        for (int i = 4; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        jrs.setMatchColumn(new int[] { 7, 8 });
        indexes = jrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(15, indexes.length);
        assertEquals(7, indexes[0]);
        assertEquals(8, indexes[1]);
        assertEquals(6, indexes[2]);
        assertEquals(4, indexes[3]);
        assertEquals(5, indexes[4]);
        assertEquals(1, indexes[5]);

        for (int i = 6; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        jrs.setMatchColumn(9);
        indexes = jrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(15, indexes.length);
        assertEquals(9, indexes[0]);
        assertEquals(8, indexes[1]);
        assertEquals(6, indexes[2]);
        assertEquals(4, indexes[3]);
        assertEquals(5, indexes[4]);
        assertEquals(1, indexes[5]);

        for (int i = 6; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        // exceed column count
        jrs.setMatchColumn(100);
        assertEquals(100, jrs.getMatchColumnIndexes()[0]);

        noInitalJrs = noInitalJdbcRowSet();

        noInitalJrs.setMatchColumn(new int[] { 1, 2, 3 });
        indexes = noInitalJrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(13, indexes.length);
        assertEquals(1, indexes[0]);
        assertEquals(2, indexes[1]);
        assertEquals(3, indexes[2]);
        for (int i = 3; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        noInitalJrs = noInitalJdbcRowSet();

    }

    public void testGetMatchColumn() throws Exception {
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        JdbcRowSet jrs = newJdbcRowSet();
        try {
            noInitalJrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        try {
            noInitalJrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        // mantains match column indexes and match column names separately
        noInitalJrs.setMatchColumn(0);
        int[] indexes = noInitalJrs.getMatchColumnIndexes();

        try {
            noInitalJrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        noInitalJrs.setMatchColumn("test");
        String[] names = noInitalJrs.getMatchColumnNames();

        assertNotSame(noInitalJrs.getMatchColumnIndexes(), noInitalJrs
                .getMatchColumnIndexes());
        assertNotSame(noInitalJrs.getMatchColumnNames(), noInitalJrs
                .getMatchColumnNames());

    }

    public void testUnsetMatchColumn_Index_Unpopulate() throws Exception {
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        JdbcRowSet jrs = newJdbcRowSet();
        int[] indexes = null;
        try {
            noInitalJrs.unsetMatchColumn(indexes);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        indexes = new int[0];

        noInitalJrs.unsetMatchColumn(indexes);

        indexes = new int[] { 1, 2, 3 };
        try {
            noInitalJrs.unsetMatchColumn(indexes);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        try {
            noInitalJrs.unsetMatchColumn(-2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        try {
            noInitalJrs.unsetMatchColumn(0);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        noInitalJrs.setMatchColumn(1);
        try {
            noInitalJrs.unsetMatchColumn(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        noInitalJrs.unsetMatchColumn(1);

        try {
            noInitalJrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        noInitalJrs.setMatchColumn(new int[] { 1, 2, 3 });

        try {
            noInitalJrs.unsetMatchColumn(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        noInitalJrs.unsetMatchColumn(1);

        try {
            noInitalJrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        noInitalJrs.setMatchColumn(4);
        indexes = noInitalJrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(13, indexes.length);
        assertEquals(4, indexes[0]);
        assertEquals(2, indexes[1]);
        assertEquals(3, indexes[2]);
        for (int i = 3; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        try {
            noInitalJrs.unsetMatchColumn(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        noInitalJrs.setMatchColumn(new int[] { 5, 6 });
        try {
            noInitalJrs.unsetMatchColumn(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn(4);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn(6);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        noInitalJrs.unsetMatchColumn(5);

        try {
            noInitalJrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        noInitalJrs.setMatchColumn(7);
        indexes = noInitalJrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(15, indexes.length);
        assertEquals(7, indexes[0]);
        assertEquals(6, indexes[1]);
        assertEquals(4, indexes[2]);
        assertEquals(2, indexes[3]);
        assertEquals(3, indexes[4]);

        for (int i = 6; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        noInitalJrs.unsetMatchColumn(new int[] { 7, 6 });
        try {
            noInitalJrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        noInitalJrs.setMatchColumn(new int[] { 7, 6 });
        indexes = noInitalJrs.getMatchColumnIndexes();

        assertNotNull(indexes);
        assertEquals(17, indexes.length);
        assertEquals(7, indexes[0]);
        assertEquals(6, indexes[1]);
        assertEquals(-1, indexes[2]);
        assertEquals(-1, indexes[3]);
        assertEquals(4, indexes[4]);
        assertEquals(2, indexes[5]);
        assertEquals(3, indexes[6]);
    }

    public void testUnsetMatchColumn_Index() throws Exception {
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        JdbcRowSet jrs = newJdbcRowSet();
        int[] indexes = null;
        try {
            jrs.unsetMatchColumn(indexes);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        indexes = new int[0];

        jrs.unsetMatchColumn(indexes);

        indexes = new int[] { 1, 2, 3 };
        try {
            jrs.unsetMatchColumn(indexes);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        try {
            jrs.unsetMatchColumn(-2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        try {
            jrs.unsetMatchColumn(0);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        jrs.setMatchColumn(1);
        try {
            jrs.unsetMatchColumn(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        jrs.unsetMatchColumn(1);

        try {
            indexes = jrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        jrs.setMatchColumn(new int[] { 1, 2, 3 });

        try {
            jrs.unsetMatchColumn(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        jrs.unsetMatchColumn(1);

        try {
            jrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        jrs.setMatchColumn(4);
        indexes = jrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(13, indexes.length);
        assertEquals(4, indexes[0]);
        assertEquals(2, indexes[1]);
        assertEquals(3, indexes[2]);
        for (int i = 3; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        try {
            jrs.unsetMatchColumn(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        jrs.setMatchColumn(new int[] { 5, 6 });
        try {
            jrs.unsetMatchColumn(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn(3);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn(4);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn(6);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        jrs.unsetMatchColumn(5);

        try {
            jrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        jrs.setMatchColumn(7);
        indexes = jrs.getMatchColumnIndexes();
        assertNotNull(indexes);
        assertEquals(15, indexes.length);
        assertEquals(7, indexes[0]);
        assertEquals(6, indexes[1]);
        assertEquals(4, indexes[2]);
        assertEquals(2, indexes[3]);
        assertEquals(3, indexes[4]);

        for (int i = 6; i < indexes.length; i++) {
            assertEquals(-1, indexes[i]);
        }

        jrs.unsetMatchColumn(new int[] { 7, 6 });
        try {
            jrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        jrs.setMatchColumn(new int[] { 7, 6 });
        indexes = jrs.getMatchColumnIndexes();

        assertNotNull(indexes);
        assertEquals(17, indexes.length);
        assertEquals(7, indexes[0]);
        assertEquals(6, indexes[1]);
        assertEquals(-1, indexes[2]);
        assertEquals(-1, indexes[3]);
        assertEquals(4, indexes[4]);
        assertEquals(2, indexes[5]);
        assertEquals(3, indexes[6]);

        jrs.unsetMatchColumn(new int[] { 7, 6, -1, -1, 4, 2, 3 });
        try {
            indexes = jrs.getMatchColumnIndexes();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }
    }

    public void testUnsetMatchColumn_Name_Unpopulate() throws Exception {
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        JdbcRowSet jrs = newJdbcRowSet();
        String[] names = null;
        try {
            noInitalJrs.unsetMatchColumn(names);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        names = new String[0];

        noInitalJrs.unsetMatchColumn(names);

        names = new String[] { "1", "2", "3" };
        try {
            noInitalJrs.unsetMatchColumn(names);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        /*
         * TODO behavior of unsetMatchColumn(String) is not the same with
         * unsetMatchColumn(int) in RI, we think throw SQLException is more
         * reasonable
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                noInitalJrs.unsetMatchColumn("");
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, Columns being unset are not the same as set
            }
            try {
                noInitalJrs.unsetMatchColumn("0");
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, Columns being unset are not the same as set
            }
        } else {
            try {
                noInitalJrs.unsetMatchColumn("");
                fail("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            }

            try {
                noInitalJrs.unsetMatchColumn("0");
                fail("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            }

        }

        noInitalJrs.setMatchColumn("1");
        try {
            noInitalJrs.unsetMatchColumn("2");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        noInitalJrs.unsetMatchColumn("1");

        try {
            noInitalJrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        noInitalJrs.setMatchColumn(new String[] { "1", "2", "3" });

        try {
            noInitalJrs.unsetMatchColumn("2");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn("3");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        noInitalJrs.unsetMatchColumn("1");

        try {
            noInitalJrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        noInitalJrs.setMatchColumn("4");
        names = noInitalJrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(13, names.length);
        assertEquals("4", names[0]);
        assertEquals("2", names[1]);
        assertEquals("3", names[2]);
        for (int i = 3; i < names.length; i++) {
            assertNull(names[i]);
        }

        try {
            noInitalJrs.unsetMatchColumn("2");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn("3");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        noInitalJrs.setMatchColumn(new String[] { "5", "6" });
        try {
            noInitalJrs.unsetMatchColumn("2");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn("3");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn("4");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            noInitalJrs.unsetMatchColumn("6");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        noInitalJrs.unsetMatchColumn("5");

        try {
            noInitalJrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        noInitalJrs.setMatchColumn("7");
        names = noInitalJrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(15, names.length);
        assertEquals("7", names[0]);
        assertEquals("6", names[1]);
        assertEquals("4", names[2]);
        assertEquals("2", names[3]);
        assertEquals("3", names[4]);

        for (int i = 6; i < names.length; i++) {
            assertNull(names[i]);
        }

        noInitalJrs.unsetMatchColumn(new String[] { "7", "6" });
        try {
            noInitalJrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        noInitalJrs.setMatchColumn(new String[] { "7", "6" });
        names = noInitalJrs.getMatchColumnNames();

        assertNotNull(names);
        assertEquals(17, names.length);
        assertEquals("7", names[0]);
        assertEquals("6", names[1]);
        assertNull(names[2]);
        assertNull(names[3]);
        assertEquals("4", names[4]);
        assertEquals("2", names[5]);
        assertEquals("3", names[6]);
    }

    public void testUnsetMatchColumn_Name() throws Exception {
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        JdbcRowSet jrs = newJdbcRowSet();
        String[] names = null;
        try {
            jrs.unsetMatchColumn(names);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        names = new String[0];

        jrs.unsetMatchColumn(names);

        names = new String[] { "1", "2", "3" };
        try {
            jrs.unsetMatchColumn(names);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        /*
         * TODO behavior of unsetMatchColumn(String) is not the same with
         * unsetMatchColumn(int) in RI, we think throw SQLException is more
         * reasonable
         */
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            try {
                jrs.unsetMatchColumn("");
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, Columns being unset are not the same as set
            }
            try {
                jrs.unsetMatchColumn("0");
                fail("Should throw SQLException");
            } catch (SQLException e) {
                // expected, Columns being unset are not the same as set
            }
        } else {
            try {
                jrs.unsetMatchColumn("");
                fail("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            }

            try {
                jrs.unsetMatchColumn("0");
                fail("Should throw NullPointerException");
            } catch (NullPointerException e) {
                // expected
            }

        }

        jrs.setMatchColumn("1");
        try {
            jrs.unsetMatchColumn("2");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        jrs.unsetMatchColumn("1");

        try {
            jrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        jrs.setMatchColumn(new String[] { "1", "2", "3" });

        try {
            jrs.unsetMatchColumn("2");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn("3");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        jrs.unsetMatchColumn("1");

        try {
            jrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        jrs.setMatchColumn("4");
        names = jrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(13, names.length);
        assertEquals("4", names[0]);
        assertEquals("2", names[1]);
        assertEquals("3", names[2]);
        for (int i = 3; i < names.length; i++) {
            assertNull(names[i]);
        }

        try {
            jrs.unsetMatchColumn("2");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn("3");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        jrs.setMatchColumn(new String[] { "5", "6" });
        try {
            jrs.unsetMatchColumn("2");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn("3");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn("4");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
        try {
            jrs.unsetMatchColumn("6");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }

        jrs.unsetMatchColumn("5");

        try {
            jrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        jrs.setMatchColumn("7");
        names = jrs.getMatchColumnNames();
        assertNotNull(names);
        assertEquals(15, names.length);
        assertEquals("7", names[0]);
        assertEquals("6", names[1]);
        assertEquals("4", names[2]);
        assertEquals("2", names[3]);
        assertEquals("3", names[4]);

        for (int i = 6; i < names.length; i++) {
            assertNull(names[i]);
        }

        jrs.unsetMatchColumn(new String[] { "7", "6" });
        try {
            jrs.getMatchColumnNames();
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Set Match columns before getting them
        }

        jrs.setMatchColumn(new String[] { "7", "6" });
        names = jrs.getMatchColumnNames();

        assertNotNull(names);
        assertEquals(17, names.length);
        assertEquals("7", names[0]);
        assertEquals("6", names[1]);
        assertNull(names[2]);
        assertNull(names[3]);
        assertEquals("4", names[4]);
        assertEquals("2", names[5]);
        assertEquals("3", names[6]);

        jrs = noInitalJdbcRowSet();

        // test whether column name is case sensitive
        jrs.setMatchColumn("TesT");
        assertEquals("TesT", jrs.getMatchColumnNames()[0]);
        try {
            jrs.unsetMatchColumn("test");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
    }

    public void testSetMatchColumn_Initial() throws Exception {
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        JdbcRowSet jrs = newJdbcRowSet();
        String[] names = { "1", "2", "3" };
        jrs.setMatchColumn(names);

        names = jrs.getMatchColumnNames();
        assertEquals(13, names.length);
        assertEquals("1", names[0]);
        assertEquals("2", names[1]);
        assertEquals("3", names[2]);
        try {
            jrs.unsetMatchColumn(new String[] { "3", "2", "1" });
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
    }

    public void testUnSetMatchColumn() throws Exception {
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        JdbcRowSet jrs = newJdbcRowSet();
        int[] indexs = { 1, 2, 3 };
        jrs.setMatchColumn(indexs);

        try {
            jrs.unsetMatchColumn(new int[] { 3, 2, 1 });
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Columns being unset are not the same as set
        }
    }

    protected JdbcRowSet noInitalJdbcRowSet() throws Exception {
        try {
            return (JdbcRowSet) Class.forName("com.sun.rowset.JdbcRowSetImpl")
                    .newInstance();
        } catch (ClassNotFoundException e) {
            return (JdbcRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.JdbcRowSetImpl")
                    .newInstance();
        }
    }

    protected JdbcRowSet newJdbcRowSet() throws Exception {
        JdbcRowSet noInitalJrs = noInitalJdbcRowSet();
        noInitalJrs.setUrl(DERBY_URL);
        noInitalJrs.setCommand("SELECT * FROM USER_INFO");
        noInitalJrs.execute();
        return noInitalJrs;
    }

}
