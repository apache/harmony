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

import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Time;
import java.sql.Timestamp;

import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.spi.SyncProviderException;
import javax.sql.rowset.spi.SyncResolver;

import org.apache.harmony.sql.internal.rowset.CachedRow;
import org.apache.harmony.sql.internal.rowset.SyncResolverImpl;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockBlob;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockClob;
import org.apache.harmony.sql.tests.javax.sql.rowset.MockNClob;

public class SyncResolverTest extends CachedRowSetTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNotSupportMethods() throws Exception {
        CachedRowSet copy = crset.createCopy();

        copy.absolute(3);
        crset.absolute(3);

        copy.updateString(2, "updated");
        assertEquals("updated", copy.getString(2));
        assertEquals("test3", crset.getString(2));

        copy.updateRow();
        copy.acceptChanges();

        assertEquals(copy.getString(2), "updated");
        assertEquals(crset.getString(2), "test3");

        crset.updateString(2, "again");

        assertEquals(copy.getString(2), "updated");
        assertEquals(crset.getString(2), "again");

        crset.updateRow();

        SyncProviderException ex = null;
        try {
            crset.acceptChanges(conn);
        } catch (SyncProviderException e) {
            ex = e;
        }

        SyncResolver resolver = ex.getSyncResolver();

        try {
            resolver.absolute(1);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.relative(1);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.next();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.previous();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.isAfterLast();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.isBeforeFirst();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.isFirst();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.isLast();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getMetaData();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getString(2);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getCursorName();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateString(2, "hello");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.rowDeleted();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.rowInserted();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.rowUpdated();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getWarnings();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getStatement();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.findColumn("ID");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.wasNull();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.moveToCurrentRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.moveToInsertRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.refreshRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.execute();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.clearWarnings();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.deleteRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.insertRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateRow();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.cancelRowUpdates();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.close();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getHoldability();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getNCharacterStream(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getNClob(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getNString(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getRowId(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.getSQLXML(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.isClosed();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        resolver.updateAsciiStream(100, null, 100L);
        resolver.updateAsciiStream("not exist", null, 100L);

        try {
            resolver.updateAsciiStream(100, null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        resolver.updateBinaryStream(100, null, 100L);
        resolver.updateBinaryStream("not exist", null, 100L);

        try {
            resolver.updateBinaryStream(100, null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            resolver.updateBlob(100, null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            resolver.updateBlob(100, new StringBufferInputStream("test"));
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateCharacterStream(100, null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateCharacterStream("not", null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateCharacterStream(100, null, 100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateCharacterStream("not", null, 100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateCharacterStream(100, null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateCharacterStream("not", null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            resolver.updateClob(100, null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateClob("not", null, 100L);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            resolver.updateClob(100, new StringReader("test"));
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateClob("not", new StringReader("test"));
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            resolver.updateNCharacterStream(100, null, 100L);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateNCharacterStream("not", null, 100L);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateNCharacterStream(100, null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateNCharacterStream("not", null);
            fail("Should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            resolver.updateNClob(2, new StringReader("readstr"));
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateNClob("not", new StringReader("readstr"));
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateNClob(100, new StringReader("readstr"), 1000L);
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateNClob("not", new StringReader("readstr"), 1000L);
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }

        try {
            resolver.updateNString(100, "test");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateNString("not", "test");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateRowId(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateRowId("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateSQLXML(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateSQLXML("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.afterLast();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.beforeFirst();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.first();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getArray("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getArray(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getAsciiStream("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getAsciiStream(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBigDecimal(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBigDecimal("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBigDecimal(100, 100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBigDecimal("not", 100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBinaryStream(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBinaryStream("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBlob(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBlob("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBoolean(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBoolean("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getByte(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getByte("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBytes(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getBytes("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        // //////
        try {
            resolver.getCharacterStream(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getCharacterStream("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getClob(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getClob("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getDate(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getDate("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getDate(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getDate("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getDouble(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getDouble("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getFloat(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getFloat("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getInt(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getInt("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getLong(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getLong("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getNCharacterStream("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getNClob("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getNString("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getObject("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getObject(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getObject("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getObject(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getRef("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getRef(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getRowId("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getSQLXML("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getShort("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getShort(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getString("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getTime(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getTime("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getTime(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getTime("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getTimestamp(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getTimestamp("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getTimestamp(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getTimestamp("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getURL(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getURL("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getUnicodeStream(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.getUnicodeStream("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.last();
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateArray(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateArray("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        resolver.updateAsciiStream("not", null);
        try {
            resolver.updateAsciiStream("not", null, 100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateAsciiStream(100, null, 100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateBigDecimal(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateBigDecimal("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateBinaryStream("not", null);
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateBinaryStream("not", null, 100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateBinaryStream(100, null, 100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateBlob("not", new StringBufferInputStream("test"));
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateBlob("not", new StringBufferInputStream("test"),
                    100L);
            fail("should throw SQLFeatureNotSupportedException");
        } catch (SQLFeatureNotSupportedException e) {
            // expected
        }
        try {
            resolver.updateBoolean(100, false);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateBoolean("not", false);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        byte aByte = 0;
        try {
            resolver.updateByte(100, aByte);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateByte("not", aByte);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateBytes(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateBytes("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateDate(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateDate("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateDouble(100, 0);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateDouble("not", 0);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateFloat(100, 0);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateFloat("not", 0);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateInt(100, 0);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateInt("not", 0);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateLong(100, 0);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateLong("not", 0);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateNull(100);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateNull("not");
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateObject(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateObject("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateObject(100, null, 10);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateObject("not", null, 10);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateRef(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateRef("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        short aShort = 0;
        try {
            resolver.updateShort(100, aShort);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateShort("not", aShort);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateString("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateTime("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateTime(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateTimestamp("not", null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateTimestamp(100, null);
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            resolver.updateBlob(100, new MockBlob());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateBlob("not", new MockBlob());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateClob(100, new MockClob());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateClob("not", new MockClob());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateNClob(100, new MockNClob());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            resolver.updateNClob("not", new MockNClob());
            fail("should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    public void testGetConflictValue() throws Exception {

        RowSetMetaData metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(DEFAULT_COLUMN_COUNT);

        SyncResolverImpl resolver = new SyncResolverImpl(metadata);
        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 1,
                SyncResolver.INSERT_ROW_CONFLICT);

        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 2,
                SyncResolver.INSERT_ROW_CONFLICT);

        // before call nextConflict
        try {
            resolver.getConflictValue(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid cursor position
        }

        try {
            resolver.getConflictValue(-1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid column index
        }
        try {
            resolver.getConflictValue("not exist");
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected, Invalid column name
        }

        assertTrue(resolver.nextConflict());

        for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
            assertNull(resolver.getConflictValue(i));
        }

        assertTrue(resolver.nextConflict());

        for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
            assertNull(resolver.getConflictValue(i));
        }

        assertFalse(resolver.nextConflict());
        assertEquals(0, resolver.getRow());

        /*
         * ri throw SQLException after call nextConflict again, it's not
         * reasonable
         */
        try {
            resolver.getConflictValue(1);
            fail("Should throw SQLException");
        } catch (SQLException e) {
            // expected Invalid cursor position
        }

    }

    public void testNextPreviousConflict() throws Exception {

        RowSetMetaData metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(DEFAULT_COLUMN_COUNT);

        SyncResolverImpl resolver = new SyncResolverImpl(metadata);
        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 1,
                SyncResolver.INSERT_ROW_CONFLICT);

        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 2,
                SyncResolver.INSERT_ROW_CONFLICT);

        assertTrue(resolver.nextConflict());
        assertTrue(resolver.nextConflict());
        assertFalse(resolver.nextConflict());
        assertFalse(resolver.nextConflict());

        assertTrue(resolver.previousConflict());
        assertTrue(resolver.previousConflict());
        assertFalse(resolver.previousConflict());
        assertFalse(resolver.previousConflict());
    }

    public void testGetStatus() throws Exception {

        RowSetMetaData metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(DEFAULT_COLUMN_COUNT);

        SyncResolverImpl resolver = new SyncResolverImpl(metadata);
        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 1,
                SyncResolver.INSERT_ROW_CONFLICT);

        resolver.addConflictRow(
                new CachedRow(new Object[DEFAULT_COLUMN_COUNT]), 2,
                SyncResolver.INSERT_ROW_CONFLICT);

        try {
            resolver.getStatus();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        assertTrue(resolver.nextConflict());
        assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());
        assertTrue(resolver.nextConflict());
        assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());
        assertFalse(resolver.nextConflict());

        try {
            resolver.getStatus();
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

    }

    public void testAcceptChanges_InsertException_Two() throws Exception {
        /*
         * Insert a new row. The new row's primary key has existed. Therefore,
         * it should throw SyncProviderException.
         */
        crset = newNoInitialInstance();
        crset.populate(st.executeQuery("select * from USER_INFO"));
        crset.setTableName("USER_INFO");
        crset.moveToInsertRow();
        crset.updateInt(1, 4); // The ID valued 4 has existed in db.
        crset.updateString(2, "test5");
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());
            assertTrue(resolver.nextConflict());
            assertEquals(1, resolver.getRow());
            assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());
            for (int i = 1; i <= crset.getMetaData().getColumnCount(); i++) {
                assertNull(resolver.getConflictValue(i));
                try {
                    resolver.setResolvedValue(i, null);
                    fail("should throw SQLException");
                } catch (SQLException ex) {
                    // expected
                }
            }
            assertFalse(resolver.nextConflict());
        }
    }

    public void testAcceptChanges_InsertException_Three() throws Exception {
        /*
         * Insert a new row. Before inserting the new row, another new row which
         * has the same data is inserted into the DB. However, the current
         * CachedRowSet doesn't know it.
         */
        crset = newNoInitialInstance();
        crset.populate(st.executeQuery("select * from USER_INFO"));
        String insertSQL = "INSERT INTO USER_INFO(ID, NAME, BIGINT_T, NUMERIC_T,DECIMAL_T, SMALLINT_T, "
                + "FLOAT_T, REAL_T, DOUBLE_T, DATE_T, TIME_T, TIMESTAMP_T) VALUES(?, ?, ?, ?, ?, ?,"
                + "?, ?, ?, ?, ?, ? )";
        PreparedStatement preStmt = conn.prepareStatement(insertSQL);
        preStmt.setInt(1, 80);
        preStmt.setString(2, "test" + 80);
        preStmt.setLong(3, 444423L);
        preStmt.setBigDecimal(4, new BigDecimal(12));
        preStmt.setBigDecimal(5, new BigDecimal(23));
        preStmt.setInt(6, 41);
        preStmt.setFloat(7, 4.8F);
        preStmt.setFloat(8, 4.888F);
        preStmt.setDouble(9, 4.9999);
        preStmt.setDate(10, new Date(965324512));
        preStmt.setTime(11, new Time(452368512));
        preStmt.setTimestamp(12, new Timestamp(874532105));
        preStmt.executeUpdate();
        if (preStmt != null) {
            preStmt.close();
        }
        // check the new row in DB
        rs = st.executeQuery("select COUNT(*) from USER_INFO where ID = 80");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));

        // now call CachedRowSet.insertRow()
        crset.setTableName("USER_INFO");
        assertTrue(crset.absolute(3));
        crset.moveToInsertRow();
        crset.updateInt(1, 80);
        crset.updateString(2, "test" + 80);
        crset.updateLong(3, 444423L);
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateInt(6, 41);
        crset.updateFloat(7, 4.8F);
        crset.updateFloat(8, 4.888F);
        crset.updateDouble(9, 4.9999);
        crset.updateDate(10, new Date(965324512));
        crset.updateTime(11, new Time(452368512));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());
            assertTrue(resolver.nextConflict());
            assertEquals(4, resolver.getRow());
            assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());
            for (int i = 1; i <= crset.getMetaData().getColumnCount(); i++) {
                assertNull(resolver.getConflictValue(i));
                try {
                    resolver.setResolvedValue(i, null);
                    fail("should throw SQLException");
                } catch (SQLException ex) {
                    // expected
                }
            }
            assertFalse(resolver.nextConflict());
        }
    }

    public void testAcceptChanges_DeleteException_One() throws Exception {
        /*
         * Delete a row which has been deleted from database
         */
        int result = st.executeUpdate("delete from USER_INFO where ID = 3");
        assertEquals(1, result);
        // move to the third row which doesn't exist in database
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        crset.deleteRow();
        assertFalse(crset.getShowDeleted());
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());
            assertTrue(resolver.nextConflict());
            if ("true".equals(System.getProperty("Testing Harmony"))) {
                assertEquals(3, resolver.getRow());
            } else {
                assertEquals(0, resolver.getRow());
            }
            assertEquals(SyncResolver.DELETE_ROW_CONFLICT, resolver.getStatus());
            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                assertNull(resolver.getConflictValue(i));
                try {
                    resolver.setResolvedValue(i, null);
                    fail("should throw SQLException");
                } catch (SQLException ex) {
                    // expected
                }
            }
            assertFalse(resolver.nextConflict());
        }

        assertEquals(0, crset.getRow());
        try {
            crset.getObject(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        // check database
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 3");
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));

        // check CachedRowSet
        assertFalse(crset.getShowDeleted());
        crset.beforeFirst();
        assertTrue(crset.absolute(3));
        assertEquals(4, crset.getInt(1));
        assertFalse(crset.next());

        crset.setShowDeleted(true);
        assertTrue(crset.absolute(3));
        assertTrue(crset.rowDeleted());
        assertEquals(3, crset.getInt(1));
    }

    public void testAcceptChanges_DeleteException_Two() throws Exception {
        /*
         * Delete a row which has been updated in database
         */
        crset = newNoInitialInstance();
        crset.setCommand("SELECT * FROM USER_INFO");
        crset.setUrl(DERBY_URL);
        crset.execute();

        int result = st
                .executeUpdate("update USER_INFO set NAME = 'update44' where ID = 4");
        assertEquals(1, result);
        // move to the updated row
        crset.absolute(4);
        assertEquals(4, crset.getInt(1));
        assertEquals("test4", crset.getString(2));
        crset.deleteRow();
        assertFalse(crset.getShowDeleted());
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());
            assertTrue(resolver.nextConflict());
            assertEquals(SyncResolver.DELETE_ROW_CONFLICT, resolver.getStatus());
            if ("true".equals(System.getProperty("Testing Harmony"))) {
                assertEquals(4, resolver.getRow());
                assertNull(resolver.getConflictValue(1));
            } else {
                assertEquals(0, resolver.getRow());
                try {
                    resolver.getConflictValue(1);
                    fail("should throw SQLException");
                } catch (SQLException ex) {
                    // expected
                }
            }
            assertFalse(resolver.nextConflict());
        }

        // check database
        rs = st.executeQuery("SELECT COUNT(*) FROM USER_INFO WHERE ID = 4");
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));

        // check CachedRowSet
        assertFalse(crset.getShowDeleted());
        crset.beforeFirst();
        assertFalse(crset.absolute(4));

        crset.setShowDeleted(true);
        assertTrue(crset.absolute(4));
        assertEquals(4, crset.getInt(1));
        assertTrue(crset.rowDeleted());
    }

    public void testAcceptChanges_UpdateException_One() throws Exception {
        /*
         * Update a row which has been deleted from database
         */
        int result = st.executeUpdate("delete from USER_INFO where ID = 3");
        assertEquals(1, result);
        // move to the third row which doesn't exist in database
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        crset.updateString(2, "update33");
        crset.updateRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            assertTrue(resolver.nextConflict());
            assertEquals(3, resolver.getRow());
            assertEquals(SyncResolver.UPDATE_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                assertNull(resolver.getConflictValue(i));
                try {
                    resolver.setResolvedValue(i, null);
                    fail("should throw SQLException");
                } catch (SQLException ex) {
                    // expected
                }
            }
            assertFalse(resolver.nextConflict());
        }
    }

    public void testAcceptChanges_UpdateException_Two() throws Exception {
        /*
         * Update a row which has been updated in database
         */
        crset = newNoInitialInstance();
        crset.populate(st.executeQuery("select * from USER_INFO"));
        int result = st
                .executeUpdate("update USER_INFO set NAME = 'update44' where ID = 4");
        assertEquals(1, result);

        // move to the updated row
        assertTrue(crset.absolute(4));
        assertEquals(4, crset.getInt(1));
        assertEquals("test4", crset.getString(2));
        crset.updateString(2, "change4");
        crset.updateRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());
            assertTrue(resolver.nextConflict());
            assertEquals(4, resolver.getRow());
            assertEquals(SyncResolver.UPDATE_ROW_CONFLICT, resolver.getStatus());

            if ("true".equals(System.getProperty("Testing Harmony"))) {
                resolver.getConflictValue(1);
            } else {
                try {
                    resolver.getConflictValue(1);
                    fail("should throw SQLException");
                } catch (SQLException ex) {
                    // TODO RI throw SQLException here, maybe RI's bug
                }
            }
            assertFalse(resolver.nextConflict());
        }
    }

    public void testAcceptChanges_UpdateException_Three() throws Exception {
        /*
         * Update a row in which one column's value is out of range
         */
        crset = newNoInitialInstance();
        crset.populate(st.executeQuery("select * from USER_INFO"));
        assertEquals(4, crset.size());
        assertTrue(crset.absolute(4));
        assertEquals(4, crset.getInt(1));
        crset.updateString(2, "update4");
        crset.updateLong(3, 555555L);
        crset.updateInt(4, 200000); // 200000 exceeds the NUMERIC's range
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateFloat(8, 4.888F);
        crset.updateRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());
            assertTrue(resolver.nextConflict());
            assertEquals(4, resolver.getRow());
            assertEquals(SyncResolver.UPDATE_ROW_CONFLICT, resolver.getStatus());

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                assertNull(resolver.getConflictValue(i));
                try {
                    resolver.setResolvedValue(i, null);
                    fail("should throw SQLException");
                } catch (SQLException ex) {
                    // expected
                }
            }
            assertFalse(resolver.nextConflict());
        }
    }

    public void testAcceptChanges_MultiConflicts() throws Exception {
        /*
         * Update a row in which one column's value is out of range
         */
        assertTrue(crset.absolute(3));
        assertEquals(3, crset.getInt(1));
        crset.updateString(2, "update4");
        crset.updateLong(3, 555555L);
        crset.updateInt(4, 200000); // 200000 exceeds the NUMERIC's range
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateFloat(8, 4.888F);
        crset.updateRow();

        /*
         * Delete a row which has been deleted from database
         */
        int result = st.executeUpdate("delete from USER_INFO where ID = 1");
        assertEquals(1, result);
        // move to the first row which doesn't exist in database
        assertTrue(crset.absolute(1));
        assertEquals(1, crset.getInt(1));
        crset.deleteRow();

        /*
         * Insert a new row. One given column's value exceeds the max range.
         */
        assertTrue(crset.last());
        crset.setTableName("USER_INFO");
        crset.moveToInsertRow();
        crset.updateInt(1, 5);
        crset.updateString(2, "test5");
        crset.updateLong(3, 555555L);
        crset.updateInt(4, 200000); // 200000 exceeds the NUMERIC's range
        crset.updateBigDecimal(5, new BigDecimal(23));
        crset.updateFloat(8, 4.888F);
        crset.insertRow();
        crset.moveToCurrentRow();

        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();
            assertEquals(0, resolver.getRow());

            try {
                resolver.getConflictValue(1);
                fail("should throw SQLException");
            } catch (SQLException ex) {
                // expected
            }

            assertTrue(resolver.nextConflict());
            assertEquals(SyncResolver.DELETE_ROW_CONFLICT, resolver.getStatus());
            if ("true".equals(System.getProperty("Testing Harmony"))) {
                assertEquals(1, resolver.getRow());
            } else {
                assertEquals(0, resolver.getRow());
            }
            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                assertNull(resolver.getConflictValue(i));
            }

            assertTrue(resolver.nextConflict());
            assertEquals(SyncResolver.UPDATE_ROW_CONFLICT, resolver.getStatus());
            assertEquals(3, resolver.getRow());
            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                assertNull(resolver.getConflictValue(i));
            }

            assertTrue(resolver.nextConflict());
            assertEquals(SyncResolver.INSERT_ROW_CONFLICT, resolver.getStatus());
            assertEquals(5, resolver.getRow());
            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                assertNull(resolver.getConflictValue(i));
            }

            assertFalse(resolver.nextConflict());
        }
    }

    public void testSetResolvedValue_ILObject() throws Exception {

        /*
         * Insert a new row. The new row's primary key has existed. Therefore,
         * it should throw SyncProviderException.
         */
        crset = newNoInitialInstance();
        crset.populate(st.executeQuery("select * from USER_INFO"));
        crset.setTableName("USER_INFO");
        crset.moveToInsertRow();
        crset.updateInt(1, 4); // The ID valued 4 has existed in db.
        crset.updateString(2, "test5");
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();

            // before call nextConflict
            try {
                resolver.setResolvedValue(1, 1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid cursor position
            }

            try {
                resolver.setResolvedValue(-1, 1);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected, Invalid column index
            }

            assertTrue(resolver.nextConflict());

            try {
                resolver.setResolvedValue(-1, 10);
                fail("Should throw SQLException");
            } catch (SQLException ex) {
                // expected
            }

            for (int i = 1; i <= DEFAULT_COLUMN_COUNT; ++i) {
                try {
                    resolver.setResolvedValue(i, i);
                    fail("Should throw SQLException");
                } catch (SQLException ex) {
                    // expected
                }
            }
        }
    }

    public void testSetResolvedValue_LStringLObject() throws Exception {
        /*
         * Insert a new row. The new row's primary key has existed. Therefore,
         * it should throw SyncProviderException.
         */
        crset = newNoInitialInstance();
        crset.populate(st.executeQuery("select * from USER_INFO"));
        crset.setTableName("USER_INFO");
        crset.moveToInsertRow();
        crset.updateInt(1, 4); // The ID valued 4 has existed in db.
        crset.updateString(2, "test5");
        crset.updateBigDecimal(4, new BigDecimal(12));
        crset.updateTimestamp(12, new Timestamp(874532105));
        crset.insertRow();
        crset.moveToCurrentRow();
        try {
            crset.acceptChanges(conn);
            fail("should throw SyncProviderException");
        } catch (SyncProviderException e) {
            SyncResolver resolver = e.getSyncResolver();

            // before call nextConflict
            resolver.setResolvedValue(null, 1);
            resolver.setResolvedValue("ID", 1);
            resolver.setResolvedValue("not exist", 1);

            assertTrue(resolver.nextConflict());
            
            resolver.setResolvedValue(null, 1);
            resolver.setResolvedValue("ID", 1);
            assertNull(resolver.getConflictValue("ID"));
            resolver.setResolvedValue("NAME", "hello");
            assertNull(resolver.getConflictValue("NAME"));
            resolver.setResolvedValue("not exist", 1);
        }
    }
}
