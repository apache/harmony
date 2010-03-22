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

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Calendar;

public class CachedRowSetGetTest extends CachedRowSetTestCase {

    public void testGetObject() throws Exception {
        noInitialCrset = newNoInitialInstance();
        try {
            noInitialCrset.getObject(1);
            fail("should throw exception");
        } catch (NullPointerException e) {
            // RI throw NullPointerException
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getObject(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        assertTrue(crset.next());
        for (int i = 1; i <= DEFAULT_COLUMN_COUNT; i++) {
            crset.getObject(i);
        }
    }

    public void testGetInt() throws Exception {
        assertTrue(crset.next());
        assertEquals(1, crset.getInt(1));

        int result = st
                .executeUpdate("UPDATE USER_INFO SET ID = 10000 WHERE NAME = 'hermit'");
        assertEquals(1, result);

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        crset = newNoInitialInstance();
        crset.populate(rs);
        while (crset.next()) {
            if (crset.getInt(1) == 10000) {
                break;
            }
        }
        assertEquals(10000, crset.getInt(1));

        crset.getBigDecimal(1);
        crset.getBoolean(1);
        try {
            crset.getByte(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        crset.getDouble(1);
        crset.getFloat(1);
        crset.getInt(1);
        crset.getLong(1);
        crset.getObject(1);
        crset.getShort(1);
        crset.getString(1);

        try {
            crset.getArray(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getBlob(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getBytes(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getClob(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getDate(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getRef(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTimestamp(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getURL(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    public void testGetDate() throws Exception {
        assertTrue(crset.absolute(3));

        try {
            crset.getDate(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getDate(2);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getDate(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getDate(4);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getDate(5);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getDate(6);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getDate(7);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getDate(8);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getDate(9);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        Date date = crset.getDate(10);
        Calendar cal = Calendar.getInstance();
        date = crset.getDate(10, cal);
        assertEquals(date.getTime(), cal.getTimeInMillis());

        try {
            crset.getDate(11);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        date = crset.getDate(12);
        assertEquals(52365412356663L, date.getTime());
        date = crset.getDate(12, cal);
        assertNotSame(52365412356663L, date.getTime());
    }

    public void testGetTime() throws Exception {
        assertTrue(crset.absolute(3));

        try {
            crset.getTime(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(2);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(4);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(5);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(6);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(7);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(8);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(9);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTime(10);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        Time time = crset.getTime(11);
        Calendar cal = Calendar.getInstance();
        time = crset.getTime(11, cal);

        time = crset.getTime(12);
        assertEquals(52365412356663L, time.getTime());
        cal = Calendar.getInstance();
        time = crset.getTime(12, cal);
    }

    public void testGetTimestamp() throws Exception {
        assertTrue(crset.absolute(3));

        try {
            crset.getTimestamp(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTimestamp(2);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTimestamp(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTimestamp(4);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTimestamp(5);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTimestamp(6);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTimestamp(7);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTimestamp(8);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            crset.getTimestamp(9);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        crset.getTimestamp(10);
        Calendar cal = Calendar.getInstance();
        crset.getTimestamp(10, cal);

        crset.getTimestamp(11);
        cal = Calendar.getInstance();
        crset.getTimestamp(11, cal);

        crset.getTimestamp(12);
        cal = Calendar.getInstance();
        crset.getTimestamp(12, cal);
    }

    public void testGetBigDecimal() throws Exception {
        noInitialCrset = newNoInitialInstance();
        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        assertTrue(noInitialCrset.absolute(3));
        noInitialCrset.getBigDecimal(4);
        noInitialCrset.getBigDecimal(4, 4);
        noInitialCrset.getBigDecimal(5);
        noInitialCrset.getBigDecimal(5, 4);
    }

    public void testGetBoolean() throws Exception {
        crset.first();
        assertTrue(crset.getBoolean(1));
        crset.updateInt(1, 0);
        assertFalse(crset.getBoolean(1));
        crset.updateInt(1, -12);
        assertTrue(crset.getBoolean(1));
        crset.updateInt(1, 12);
        assertTrue(crset.getBoolean(1));

        try {
            crset.getBoolean(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }

        crset.updateString(2, "true");
        try {
            crset.getBoolean(2);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }

        assertNull(crset.getObject(3));
        assertFalse(crset.getBoolean(3));

        crset.absolute(4);
        assertEquals(444423L, crset.getLong(3));
        assertTrue(crset.getBoolean(3));
        crset.updateLong(3, 0);
        assertFalse(crset.getBoolean(3));

        assertTrue(crset.getBoolean(4));
        assertTrue(crset.getBoolean(5));
        assertTrue(crset.getBoolean(6));
        assertTrue(crset.getBoolean(7));
        crset.updateFloat(7, 0.0f);
        assertFalse(crset.getBoolean(7));
        crset.updateFloat(7, -23.1f);
        assertTrue(crset.getBoolean(7));
        assertTrue(crset.getBoolean(8));
        assertTrue(crset.getBoolean(9));
        crset.updateDouble(9, 0);
        assertFalse(crset.getBoolean(9));

        try {
            crset.getBoolean(10);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }

        try {
            crset.getBoolean(11);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }

        try {
            crset.getBoolean(12);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // Expected.
        }
    }
}
