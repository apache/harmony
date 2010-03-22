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
import java.sql.SQLWarning;

import javax.sql.rowset.RowSetWarning;
import javax.sql.rowset.spi.SyncProviderException;

public class CachedRowSetSQLWarningTest extends CachedRowSetTestCase {

    public void testGetWarnings() throws Exception {
        noInitialCrset = newNoInitialInstance();
        SQLWarning sqlWarnings = noInitialCrset.getWarnings();
        assertNotNull(sqlWarnings);

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        assertNull(rs.getWarnings());
        noInitialCrset.populate(rs);

        assertEquals(sqlWarnings, noInitialCrset.getWarnings());
        while (noInitialCrset.next()) {
            assertEquals(sqlWarnings, noInitialCrset.getWarnings());
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        while (rs.next()) {
            assertNull(rs.getWarnings());
        }
    }

    public void testClearWarnings() throws Exception {
        noInitialCrset = newNoInitialInstance();
        SQLWarning sqlWarnings = noInitialCrset.getWarnings();
        assertNotNull(sqlWarnings);

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        assertNull(rs.getWarnings());
        noInitialCrset.populate(rs);
        assertEquals(sqlWarnings, noInitialCrset.getWarnings());

        noInitialCrset.clearWarnings();
        assertNull(noInitialCrset.getWarnings());

        noInitialCrset.beforeFirst();
        int index = 0;
        while (noInitialCrset.next()) {
            index++;
            noInitialCrset.getObject(1);
            assertNull(noInitialCrset.getWarnings());
        }

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);
        assertNull(rs.getWarnings());
        
        assertTrue(noInitialCrset.first());
        assertNull(rs.getWarnings());
    }

    public void testGetRowSetWarnings() throws Exception {
        noInitialCrset = newNoInitialInstance();
        RowSetWarning rsWarning = noInitialCrset.getRowSetWarnings();
        assertNotNull(rsWarning);

        rs = st.executeQuery("SELECT * FROM USER_INFO");
        noInitialCrset.populate(rs);

        int index = 0;
        while (noInitialCrset.next()) {
            index++;
            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; i++) {
                noInitialCrset.getObject(i);
            }
            assertEquals(rsWarning, noInitialCrset.getRowSetWarnings());
        }

        assertTrue(noInitialCrset.absolute(3));
        try {
            noInitialCrset.getInt(7);
            fail("should throw SQLException");
        } catch (SQLException e) {
            assertEquals(rsWarning, noInitialCrset.getRowSetWarnings());
        }

        /*
         * The ninth column in database is double format, value is 3.99999999.
         * It return 4.0F when using getFloat(9).
         */
        float doubleValue = noInitialCrset.getFloat(9);
        assertEquals(4.0F, doubleValue);
        assertEquals(rsWarning, noInitialCrset.getRowSetWarnings());
        assertNull(rsWarning.getSQLState());
        assertEquals(0, rsWarning.getErrorCode());

        noInitialCrset.setMaxFieldSize(3);
        noInitialCrset.moveToInsertRow();
        noInitialCrset.updateInt(1, 6);
        noInitialCrset.updateString(2, "insert");
        noInitialCrset.insertRow();
        noInitialCrset.moveToCurrentRow();
        assertEquals(rsWarning, noInitialCrset.getRowSetWarnings());
        assertNull(rsWarning.getSQLState());
        assertEquals(0, rsWarning.getErrorCode());

        noInitialCrset.acceptChanges(conn);
        assertEquals(rsWarning, noInitialCrset.getRowSetWarnings());
        assertEquals(0, rsWarning.getErrorCode());
    }
}
