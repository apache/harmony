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

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

public class CachedRowSetXlobTest extends JoinRowSetTestCase {

    public void setUp() throws Exception {
        super.setUp();
        createBlobTable();
    }

    public void createBlobTable() throws SQLException {
        st = conn.createStatement();
        rs = conn.getMetaData().getTables(null, "APP", "LOBTABLE", null);
        String createTableSQL = "create table LOBTABLE (ID INTEGER NOT NULL, blobValue blob, clobValue clob)";

        if (rs.next()) {
            st.execute("drop table LOBTABLE");
        }

        st.execute(createTableSQL);
    }
    
    public void testUpdateBlob_NullParameter() throws Exception {
        try {
            crset.updateBlob(100, null, 10);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob(100, null, 10);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
    }

    public void testUpdateBlob_CachedRowSet() throws Exception {
        crset = newNoInitialInstance();
        byte[] buf = { 1, 2, 3, 4 };
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buf);

        try {
            crset.updateBlob(2, inputStream);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateBlob(2, inputStream, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        
        try {
            crset.updateBlob(100, inputStream);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateBlob(100, inputStream, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }

        try {
            crset.updateBlob("blobValue", inputStream);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateBlob("invalidName", inputStream, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        
        try {
            crset.updateBlob("invalidName", inputStream);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateBlob("blobValue", inputStream, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }

        rs = st.executeQuery("select * from LobTable");
        crset.populate(rs);
        crset.moveToInsertRow();
        crset.updateInt(1, 1);

        try {
            crset.updateBlob(2, inputStream);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateBlob(2, inputStream, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        
        try {
            crset.updateBlob(100, inputStream);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateBlob(100, inputStream, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }

        try {
            crset.updateBlob("blobValue", inputStream);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateBlob("blobValue", inputStream, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateBlob("invalidName", inputStream);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateBlob("invalidName", inputStream, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        crset.insertRow();
        crset.moveToCurrentRow();
        crset.first();

        assertEquals(1, crset.getInt(1));
        assertNull(crset.getBlob(2));
    }

    public void testUpdateClob_CachedRowSet() throws Exception {
        crset = newNoInitialInstance();
        char[] buf = { 1, 2, 3, 4 };
        CharArrayReader reader = new CharArrayReader(buf);
        try {
            crset.updateClob(3, reader);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob(3, reader, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob(100, reader);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob(100, reader, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob("clobValue", reader);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob("clobValue", reader, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob("invalidValue", reader);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob("invalidValue", reader, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        rs = st.executeQuery("select * from LobTable");
        crset.populate(rs);
        crset.moveToInsertRow();
        crset.updateInt(1, 1);

        try {
            crset.updateClob(3, reader);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob(3, reader, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob("clobValue", reader);
            fail("Should throw SQLFeatureNotSupportedException");

        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        try {
            crset.updateClob("clobValue", reader, 2);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // Expected.
        }
        crset.insertRow();
        crset.moveToCurrentRow();
        crset.first();

        assertEquals(1, crset.getInt(1));
        assertNull(crset.getClob(3));
    }
}
