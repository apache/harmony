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

import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLFeatureNotSupportedException;

public class CachedRowSetNewFeatureTest extends CachedRowSetTestCase {

    public void testGetHoldability() throws Exception {
        assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, rs.getHoldability());

        try {
            crset.getHoldability();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetNCharacterStream() throws Exception {
        try {
            crset.getNCharacterStream(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.first());
        try {
            crset.getNCharacterStream(2);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
    
    public void testGetNCharacterStream_StringParam() throws Exception {
        try {
            crset.getNCharacterStream("Invalid");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.first());
        try {
            crset.getNCharacterStream("NAME");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetNClob() throws Exception {
        try {
            crset.getNClob(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.first());
        try {
            crset.getNClob(2);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetNString() throws Exception {
        try {
            crset.getNString(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.first());
        try {
            crset.getNString(2);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetRowId() throws Exception {
        try {
            crset.getRowId(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.first());
        try {
            crset.getRowId(2);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetSQLXML() throws Exception {
        try {
            crset.getSQLXML(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.first());
        try {
            crset.getSQLXML(2);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
    
    public void testGetSQLXML_StringParam() throws Exception {
        try {
            crset.getSQLXML("invalid");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.first());
        try {
            crset.getSQLXML("NAME");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testIsClosed() throws Exception {
        assertFalse(rs.isClosed());
        rs.close();
        assertTrue(rs.isClosed());

        assertTrue(crset.first());
        try {
            crset.isClosed();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testUpdateNCharacterStream() throws Exception {
        try {
            crset.updateNCharacterStream(2, null);
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            crset.updateNCharacterStream(2, null, 100L);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.absolute(3));

        try {
            crset.updateNCharacterStream(2, new StringReader("readstr"));
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            crset.updateNCharacterStream(2, new StringReader("readstr"), 100L);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testUpdateNClob() throws Exception {
        try {
            crset.updateNClob(2, new StringReader("readstr"));
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            crset.updateNClob(100, new StringReader("readstr"), 1000L);
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        assertTrue(crset.absolute(3));
        try {
            crset.updateNClob(2, new StringReader("readstr"));
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            crset.updateNClob(100, new StringReader("readstr"), 1000L);
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
    }

    public void testUpdateNString() throws Exception {
        try {
            crset.updateNString(2, "nstring");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.absolute(3));
        try {
            crset.updateNString(2, "nstring");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testUpdateRowId() throws Exception {
        try {
            crset.updateRowId(2, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.absolute(3));
        try {
            crset.updateRowId(2, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testUpdateSQLXML() throws Exception {
        try {
            crset.updateSQLXML(2, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        assertTrue(crset.absolute(3));
        try {
            crset.updateSQLXML(2, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
