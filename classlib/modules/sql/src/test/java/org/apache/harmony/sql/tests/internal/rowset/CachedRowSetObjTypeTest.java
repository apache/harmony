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

import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

public class CachedRowSetObjTypeTest extends CachedRowSetTestCase {

    public final static int DEFAULT_COLUMN_COUNT = 3;

    public final static int DEFAULT_ROW_COUNT = 1;

    public void setUp() throws Exception {
        super.setUp();
        st = conn.createStatement();
        rs = conn.getMetaData().getTables(null, "APP", "OBJ_TABLE", null);
        String createTableSQL = "create table OBJ_TABLE (ID INTEGER NOT NULL, BLOB_T BLOB, CLOB_T CLOB)";
        String alterTableSQL = "ALTER TABLE OBJ_TABLE  ADD CONSTRAINT OBJ_TABLE_PK Primary Key (ID)";

        if (!rs.next()) {
            st.execute(createTableSQL);
            st.execute(alterTableSQL);
        }

        st.executeUpdate("delete from OBJ_TABLE");

        String insertSQL = "INSERT INTO OBJ_TABLE(ID, BLOB_T, CLOB_T) VALUES(?, ?, ?)";
        PreparedStatement preStmt = conn.prepareStatement(insertSQL);
        preStmt.setInt(1, 1);
        String sBlob = "It is a BLOB object. The sql type is BLOB.";
        preStmt.setBytes(2, sBlob.getBytes());
        preStmt.setString(3, "It is a CLOB object. The sql type is CLOB.");
        assertEquals(1, preStmt.executeUpdate());

        try {
            crset = (CachedRowSet) Class.forName(
                    "com.sun.rowset.CachedRowSetImpl").newInstance();
            noInitialCrset = (CachedRowSet) Class.forName(
                    "com.sun.rowset.CachedRowSetImpl").newInstance();
        } catch (ClassNotFoundException e) {

            crset = (CachedRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.CachedRowSetImpl")
                    .newInstance();
            noInitialCrset = (CachedRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.CachedRowSetImpl")
                    .newInstance();

            System.setProperty("Testing Harmony", "true");
        }

        rs = st.executeQuery("SELECT * FROM OBJ_TABLE");
        crset.populate(rs);
        crset.setUrl(DERBY_URL);
        assertTrue(crset.next());
    }

    public void testGet() throws Exception {
        Object objBlob = crset.getObject(2);
        Object objClob = crset.getObject(3);
        boolean isBlob = false;
        boolean isClob = false;
        if (objBlob instanceof SerialBlob) {
            isBlob = true;
        }
        if (objClob instanceof SerialClob) {
            isClob = true;
        }
        assertTrue(isBlob);
        assertTrue(isClob);

        isBlob = false;
        isClob = false;
        Blob blob = crset.getBlob(2);
        Clob clob = crset.getClob(3);
        if (blob instanceof SerialBlob) {
            isBlob = true;
        }
        if (clob instanceof SerialClob) {
            isClob = true;
        }
        assertTrue(isBlob);
        assertTrue(isClob);
    }
}
