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

import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.rowset.JdbcRowSet;

public class JdbcRowSetNewFeatureTest extends CachedRowSetTestCase {

    private JdbcRowSet newJdbcRowSet() throws Exception {
        if ("true".equals(System.getProperty("Testing Harmony"))) {
            return (JdbcRowSet) Class.forName(
                    "org.apache.harmony.sql.internal.rowset.JdbcRowSetImpl")
                    .newInstance();
        }
        return (JdbcRowSet) Class.forName("com.sun.rowset.JdbcRowSetImpl")
                .newInstance();
    }

    public void testGetXXX() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();

        try {
            jrs.getHoldability();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getNCharacterStream(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getNCharacterStream("not");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getNClob(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getNClob("not");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getNString(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getNString("not");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getRowId(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getRowId("not");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getSQLXML(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.getSQLXML("not");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testIsClosed() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();
        try {
            jrs.isClosed();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testUpdateXXX() throws Exception {
        JdbcRowSet jrs = newJdbcRowSet();

        try {
            jrs.updateAsciiStream(100, null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateAsciiStream("not exist", null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateAsciiStream(100, null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateAsciiStream("not", null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateBinaryStream(100, null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateBinaryStream("not exist", null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateBinaryStream(100, null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateBinaryStream("not", null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateBlob(100, null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateBlob("not", null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateBlob(100, new StringBufferInputStream("test"));
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateBlob("not", new StringBufferInputStream("test"));
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateCharacterStream(100, null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateCharacterStream("not", null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateCharacterStream(100, null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateCharacterStream("not", null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateClob(100, null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateClob("not", null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateClob(100, new StringReader("test"));
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateClob("not", new StringReader("test"));
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateNCharacterStream(100, null, 100L);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.updateNCharacterStream("not", null, 100L);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.updateNCharacterStream(100, null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateNCharacterStream("not", null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateNClob(2, new StringReader("readstr"));
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateNClob("not", new StringReader("readstr"));
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateNClob(100, new StringReader("readstr"), 1000L);
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateNClob("not", new StringReader("readstr"), 1000L);
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            jrs.updateNString(100, "test");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.updateNString("not", "test");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.updateRowId(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.updateRowId("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.updateSQLXML(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            jrs.updateSQLXML("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
