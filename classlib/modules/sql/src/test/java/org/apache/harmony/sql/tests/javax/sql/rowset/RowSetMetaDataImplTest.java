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

package org.apache.harmony.sql.tests.javax.sql.rowset;

import java.io.Serializable;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.rowset.RowSetMetaDataImpl;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test class for javax.sql.rowset.RowSetMetaDataImpl
 * 
 */
public class RowSetMetaDataImplTest extends TestCase {

    private static RowSetMetaDataImpl metaDataImpl = null;

    /**
     * This comparator is designed for RowSetMetaDataImpl objects whose colCount
     * has already been set. Other objects may fail when using it.
     */
    private final static SerializableAssert ROWSET_METADATA_COMPARATOR = new SerializableAssert() {

        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {
            try {
                RowSetMetaDataImpl initialImpl = (RowSetMetaDataImpl) initial;
                RowSetMetaDataImpl deserializedImpl = (RowSetMetaDataImpl) deserialized;

                Assert.assertEquals(initialImpl.getColumnCount(),
                        deserializedImpl.getColumnCount());
                Assert.assertEquals(initialImpl.getColumnType(1),
                        deserializedImpl.getColumnType(1));
            } catch (SQLException e) {
                fail();
            }
        }
    };

    /**
     * @tests javax.sql.rowset.RowSetMetaDataImpl#RowSetMetaDataImpl()
     */
    public void test_Constructor() {
        assertNotNull(metaDataImpl);
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getColumnCount()}
     */
    public void test_getColumnCount() throws SQLException {
        assertEquals(0, metaDataImpl.getColumnCount());
        try {
            metaDataImpl.isAutoIncrement(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(4);
        assertEquals(4, metaDataImpl.getColumnCount());
        assertFalse(metaDataImpl.isAutoIncrement(4));

        metaDataImpl.setColumnCount(Integer.MAX_VALUE);
        assertFalse(metaDataImpl.isAutoIncrement(4));
        // assertEquals(Integer.MAX_VALUE, metaDataImpl.getColumnCount());
        // RI throws ArrayIndexOutOfBoundsException here, which is a RI's bug
        try {
            metaDataImpl.isAutoIncrement(5);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setColumnCount(int)}
     */
    public void test_setColumnCountI() throws SQLException {
        try {
            metaDataImpl.setColumnCount(-1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            metaDataImpl.setColumnCount(0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(18);
        assertEquals(18, metaDataImpl.getColumnCount());
        metaDataImpl.setAutoIncrement(1, true);
        assertTrue(metaDataImpl.isAutoIncrement(1));
        // original records have been overwritten
        metaDataImpl.setColumnCount(19);
        assertEquals(19, metaDataImpl.getColumnCount());
        assertFalse(metaDataImpl.isAutoIncrement(1));
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getCatalogName(int)}
     */
    public void test_getCatalogNameI() throws SQLException {
        try {
            metaDataImpl.getCatalogName(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(2);
        assertEquals("", metaDataImpl.getCatalogName(1));
        metaDataImpl.setCatalogName(1, "catalog");
        assertEquals("catalog", metaDataImpl.getCatalogName(1));
        metaDataImpl.setCatalogName(1, null);
        assertEquals("", metaDataImpl.getCatalogName(1));

        try {
            metaDataImpl.getCatalogName(Integer.MIN_VALUE);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getColumnClassName(int)}
     */
    public void test_getColumnClassNameI() throws SQLException {
        try {
            metaDataImpl.getColumnClassName(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(12);
        assertEquals("java.lang.String", metaDataImpl.getColumnClassName(12));

        metaDataImpl.setColumnTypeName(12, null);
        assertEquals("java.lang.String", metaDataImpl.getColumnClassName(12));
        metaDataImpl.setColumnType(12, Types.BLOB);
        assertEquals("[B", metaDataImpl.getColumnClassName(12));
        metaDataImpl.setColumnType(12, Types.FLOAT);
        assertEquals("java.lang.Double", metaDataImpl.getColumnClassName(12));
        metaDataImpl.setColumnType(12, Types.BIGINT);
        assertEquals("java.lang.Long", metaDataImpl.getColumnClassName(12));
        metaDataImpl.setColumnType(12, Types.BIT);
        assertEquals("java.lang.Boolean", metaDataImpl.getColumnClassName(12));
        metaDataImpl.setColumnType(12, Types.DECIMAL);
        assertEquals("java.math.BigDecimal", metaDataImpl
                .getColumnClassName(12));
        metaDataImpl.setColumnType(12, Types.TINYINT);
        assertEquals("java.lang.Byte", metaDataImpl.getColumnClassName(12));

        try {
            metaDataImpl.getColumnClassName(0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getColumnDisplaySize(int)}
     */
    public void test_getColumnDisplaySizeI() throws SQLException {
        try {
            metaDataImpl.getColumnDisplaySize(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(2);
        assertEquals(0, metaDataImpl.getColumnDisplaySize(1));
        metaDataImpl.setColumnDisplaySize(1, 4);
        assertEquals(4, metaDataImpl.getColumnDisplaySize(1));

        try {
            metaDataImpl.getColumnDisplaySize(-32);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getColumnLabel(int)}
     */
    public void test_getColumnLabelI() throws SQLException {
        try {
            metaDataImpl.getColumnLabel(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(3);
        assertNull(metaDataImpl.getColumnLabel(1));
        metaDataImpl.setColumnLabel(1, null);
        assertEquals("", metaDataImpl.getColumnLabel(1));
        metaDataImpl.setColumnLabel(1, "err");
        assertEquals("err", metaDataImpl.getColumnLabel(1));

        try {
            metaDataImpl.getColumnLabel(11);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getColumnName(int)}
     */
    public void test_getColumnNameI() throws SQLException {
        try {
            metaDataImpl.getColumnName(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(13);
        assertNull(metaDataImpl.getColumnName(12));
        metaDataImpl.setColumnName(12, null);
        assertEquals("", metaDataImpl.getColumnName(12));
        metaDataImpl.setColumnName(12, "ColumnName");
        assertEquals("ColumnName", metaDataImpl.getColumnName(12));

        try {
            metaDataImpl.getColumnName(0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getColumnType(int)}
     */
    public void test_getColumnTypeI() throws SQLException {
        try {
            metaDataImpl.getColumnType(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(13);
        metaDataImpl.setColumnType(13, Types.ARRAY);
        assertEquals(Types.ARRAY, metaDataImpl.getColumnType(13));

        try {
            metaDataImpl.getColumnType(14);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getColumnTypeName(int)}
     */
    public void test_getColumnTypeNameI() throws SQLException {
        try {
            metaDataImpl.getColumnTypeName(223);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(21);
        metaDataImpl.setColumnType(14, Types.BIGINT);
        metaDataImpl.setColumnTypeName(14, null);
        assertEquals("", metaDataImpl.getColumnTypeName(14));
        metaDataImpl.setColumnTypeName(14, "haha");
        assertEquals("haha", metaDataImpl.getColumnTypeName(14));

        try {
            metaDataImpl.getColumnTypeName(22);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getPrecision(int)}
     */
    public void test_getPrecisionI() throws SQLException {
        try {
            metaDataImpl.getPrecision(2);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(1);
        assertEquals(0, metaDataImpl.getPrecision(1));
        metaDataImpl.setPrecision(1, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, metaDataImpl.getPrecision(1));

        try {
            metaDataImpl.getPrecision(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getSchemaName(int)}
     */
    public void test_getScaleI() throws SQLException {
        try {
            metaDataImpl.getScale(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(2);
        assertEquals(0, metaDataImpl.getScale(2));
        metaDataImpl.setScale(2, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, metaDataImpl.getScale(2));

        try {
            metaDataImpl.getScale(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getSchemaName(int)}
     */
    public void test_getSchemaNameI() throws SQLException {
        try {
            metaDataImpl.getSchemaName(352);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(67);
        metaDataImpl.setSchemaName(67, null);
        assertEquals("", metaDataImpl.getSchemaName(67));
        metaDataImpl.setSchemaName(67, "a \u0053");
        assertEquals("a S", metaDataImpl.getSchemaName(67));

        try {
            metaDataImpl.getSchemaName(Integer.MIN_VALUE);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#getTableName(int)}
     */
    public void test_getTableNameI() throws SQLException {
        try {
            metaDataImpl.getTableName(2);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(2);
        assertEquals("", metaDataImpl.getTableName(1));
        assertEquals("", metaDataImpl.getTableName(2));
        metaDataImpl.setTableName(1, "tableName");
        assertEquals("tableName", metaDataImpl.getTableName(1));
        assertEquals("", metaDataImpl.getTableName(2));

        try {
            metaDataImpl.getTableName(Integer.MIN_VALUE);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#isAutoIncrement(int)}
     */
    public void test_isAutoIncrementI() throws SQLException {
        try {
            metaDataImpl.isAutoIncrement(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(3);
        assertFalse(metaDataImpl.isAutoIncrement(1));
        assertFalse(metaDataImpl.isAutoIncrement(3));
        metaDataImpl.setAutoIncrement(3, true);
        assertTrue(metaDataImpl.isAutoIncrement(3));

        try {
            metaDataImpl.isAutoIncrement(-1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            metaDataImpl.isAutoIncrement(4);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#isCaseSensitive(int)}
     */
    public void test_isCaseSensitiveI() throws SQLException {
        metaDataImpl.setColumnCount(5);
        assertFalse(metaDataImpl.isCaseSensitive(2));
        assertFalse(metaDataImpl.isCaseSensitive(5));

        metaDataImpl.setCaseSensitive(2, true);
        assertTrue(metaDataImpl.isCaseSensitive(2));
        metaDataImpl.setCaseSensitive(2, false);
        assertFalse(metaDataImpl.isCaseSensitive(2));

        try {
            metaDataImpl.isCaseSensitive(0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            metaDataImpl.isCaseSensitive(6);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#isCurrency(int)}
     */
    public void test_isCurrencyI() throws SQLException {
        try {
            metaDataImpl.isCurrency(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(5);
        assertFalse(metaDataImpl.isCurrency(1));
        metaDataImpl.setCurrency(1, true);
        assertTrue(metaDataImpl.isCurrency(1));
        metaDataImpl.setCurrency(1, true);
        assertTrue(metaDataImpl.isCurrency(1));

        try {
            metaDataImpl.isCurrency(0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(6);
        assertFalse(metaDataImpl.isCurrency(1));
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#isNullable(int)}
     */
    public void test_isNullableI() throws SQLException {
        try {
            metaDataImpl.isNullable(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(2);
        assertEquals(ResultSetMetaData.columnNoNulls, metaDataImpl
                .isNullable(1));
        metaDataImpl.setNullable(1, ResultSetMetaData.columnNullableUnknown);
        assertEquals(ResultSetMetaData.columnNullableUnknown, metaDataImpl
                .isNullable(1));

        try {
            metaDataImpl.isNullable(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#isReadOnly(int)}
     */
    public void test_isReadOnlyI() throws SQLException {
        try {
            metaDataImpl.isReadOnly(1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(11);
        assertFalse(metaDataImpl.isReadOnly(1));
        assertFalse(metaDataImpl.isReadOnly(11));
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#isWritable(int)}
     */
    public void test_isWritableI() throws SQLException {
        try {
            metaDataImpl.isWritable(3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(3);
        assertTrue(metaDataImpl.isWritable(1));

        assertTrue(metaDataImpl.isWritable(3));
        assertFalse(metaDataImpl.isReadOnly(3));

        try {
            metaDataImpl.isWritable(4);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#isDefinitelyWritable(int)}
     */
    public void test_isDefinitelyWritableI() throws SQLException {
        metaDataImpl.setColumnCount(2);
        assertTrue(metaDataImpl.isDefinitelyWritable(1));
        assertTrue(metaDataImpl.isDefinitelyWritable(2));

        // RI fails here, which does not comply to the spec
        try {
            metaDataImpl.isDefinitelyWritable(-1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#isSearchable(int)}
     */
    public void test_isSearchableI() throws SQLException {
        try {
            metaDataImpl.isSearchable(Integer.MAX_VALUE);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(1);
        assertFalse(metaDataImpl.isSearchable(1));
        metaDataImpl.setSearchable(1, true);
        assertTrue(metaDataImpl.isSearchable(1));
        metaDataImpl.setSearchable(1, false);
        assertFalse(metaDataImpl.isSearchable(1));

        try {
            metaDataImpl.isSearchable(2);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#isSigned(int)}
     */
    public void test_isSignedI() throws SQLException {
        try {
            metaDataImpl.isSigned(2);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(35);
        assertFalse(metaDataImpl.isSigned(35));
        metaDataImpl.setSigned(35, true);
        assertTrue(metaDataImpl.isSigned(35));
        metaDataImpl.setSigned(35, false);
        assertFalse(metaDataImpl.isSigned(35));

        try {
            metaDataImpl.isSigned(36);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setAutoIncrement(int, boolean)}
     */
    public void test_setAutoIncrementIZ() throws SQLException {
        try {
            metaDataImpl.setAutoIncrement(1, true);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(2);
        assertFalse(metaDataImpl.isAutoIncrement(1));
        metaDataImpl.setAutoIncrement(1, false);
        assertFalse(metaDataImpl.isAutoIncrement(1));
        metaDataImpl.setAutoIncrement(1, true);
        assertTrue(metaDataImpl.isAutoIncrement(1));

        try {
            metaDataImpl.setAutoIncrement(-1, false);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setCaseSensitive(int, boolean)}
     */
    public void test_setCaseSensitiveIZ() throws SQLException {
        try {
            metaDataImpl.setCaseSensitive(2, false);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(9);
        assertFalse(metaDataImpl.isCaseSensitive(9));
        metaDataImpl.setCaseSensitive(9, false);
        assertFalse(metaDataImpl.isCaseSensitive(9));
        metaDataImpl.setCaseSensitive(9, true);
        assertTrue(metaDataImpl.isCaseSensitive(9));
        metaDataImpl.setAutoIncrement(9, false);
        assertTrue(metaDataImpl.isCaseSensitive(9));

        try {
            metaDataImpl.setCaseSensitive(10, true);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setCatalogName(int, String)}
     */
    public void test_setCatalogNameILjava_lang_String() throws SQLException {
        try {
            metaDataImpl.setCatalogName(1, "test");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(1);
        metaDataImpl.setCatalogName(1, "AbC");
        assertEquals("AbC", metaDataImpl.getCatalogName(1));
        metaDataImpl.setCatalogName(1, null);
        assertEquals("", metaDataImpl.getCatalogName(1));

        try {
            metaDataImpl.setCatalogName(10, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setColumnDisplaySize(int, int)}
     */
    public void test_setColumnDisplaySizeII() throws SQLException {
        try {
            metaDataImpl.setColumnDisplaySize(1, 2);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(1);
        assertEquals(0, metaDataImpl.getColumnDisplaySize(1));
        metaDataImpl.setColumnDisplaySize(1, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, metaDataImpl.getColumnDisplaySize(1));

        try {
            metaDataImpl.setColumnDisplaySize(2, 0);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            metaDataImpl.setColumnDisplaySize(2, Integer.MIN_VALUE);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setColumnName(int, String)}
     */
    public void test_setColumnNameILjava_lang_String() throws SQLException {
        try {
            metaDataImpl.setColumnName(1, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(4);
        assertNull(metaDataImpl.getColumnName(1));
        metaDataImpl.setColumnName(1, "ate dsW");
        assertEquals("ate dsW", metaDataImpl.getColumnName(1));
        metaDataImpl.setColumnName(1, null);
        assertEquals("", metaDataImpl.getColumnName(1));

        try {
            metaDataImpl.setColumnName(5, "exception");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setColumnLabel(int, String)}
     */
    public void test_setColumnLabelILjava_lang_String() throws SQLException {
        try {
            metaDataImpl.setColumnLabel(1, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(3);
        assertNull(metaDataImpl.getColumnLabel(3));
        metaDataImpl.setColumnLabel(3, null);
        assertEquals("", metaDataImpl.getColumnLabel(3));

        try {
            metaDataImpl.setColumnLabel(4, "exception");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setColumnType(int, int)}
     */
    public void test_setColumnTypeII() throws SQLException {
        try {
            metaDataImpl.setColumnType(1, Types.BIGINT);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(2);
        assertEquals(0, metaDataImpl.getColumnType(1));
        metaDataImpl.setColumnType(1, Types.CLOB);
        assertEquals(Types.CLOB, metaDataImpl.getColumnType(1));
        metaDataImpl.setColumnType(1, Types.BOOLEAN);
        assertEquals(Types.BOOLEAN, metaDataImpl.getColumnType(1));

        try {
            metaDataImpl.setColumnType(1, 66);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            metaDataImpl.setColumnType(3, 58);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
        
        try {
            metaDataImpl.setColumnType(2, 59);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        // types compliant to JDBC4
        metaDataImpl.setColumnType(2, Types.NCHAR);
        metaDataImpl.setColumnType(2, Types.NCLOB);
        metaDataImpl.setColumnType(2, Types.NVARCHAR);
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setColumnTypeName(int, String)}
     */
    public void test_setColumnTypeNameILjava_lang_String() throws SQLException {
        try {
            metaDataImpl.setColumnTypeName(1, "aa");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(2);
        assertNull(metaDataImpl.getColumnTypeName(2));
        metaDataImpl.setColumnTypeName(2, null);
        assertEquals("", metaDataImpl.getColumnTypeName(2));
        metaDataImpl.setColumnTypeName(2, "");
        assertEquals("", metaDataImpl.getColumnTypeName(2));
        metaDataImpl.setColumnTypeName(2, "java.lang.String");
        assertEquals(0, metaDataImpl.getColumnType(2));
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setCurrency(int, boolean)}
     */
    public void test_setCurrencyIZ() throws SQLException {
        try {
            metaDataImpl.setCurrency(12, false);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(7);
        assertFalse(metaDataImpl.isCurrency(4));
        metaDataImpl.setCurrency(4, false);
        assertFalse(metaDataImpl.isCurrency(4));
        metaDataImpl.setCurrency(4, true);
        assertTrue(metaDataImpl.isCurrency(4));

        try {
            metaDataImpl.setCurrency(8, true);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setNullable(int, int)}
     */
    public void test_setNullableII() throws SQLException {
        try {
            metaDataImpl.setNullable(21, 1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(1);
        assertEquals(0, metaDataImpl.isNullable(1));
        metaDataImpl.setNullable(1, ResultSetMetaData.columnNullable);
        assertEquals(ResultSetMetaData.columnNullable, metaDataImpl
                .isNullable(1));

        try {
            metaDataImpl
                    .setNullable(2, ResultSetMetaData.columnNullableUnknown);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            metaDataImpl.setNullable(2, 3);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setPrecision(int, int)}
     */
    public void test_setPrecisionII() throws SQLException {
        try {
            metaDataImpl.setPrecision(12, 1);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(1);
        metaDataImpl.setPrecision(1, 0);
        assertEquals(0, metaDataImpl.getPrecision(1));

        try {
            metaDataImpl.setPrecision(12, Integer.MIN_VALUE);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setScale(int, int)}
     */
    public void test_setScaleII() throws SQLException {
        try {
            metaDataImpl.setScale(34, 5);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(1);
        metaDataImpl.setScale(1, 252);
        assertEquals(252, metaDataImpl.getScale(1));

        try {
            metaDataImpl.setScale(1, -23);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        try {
            metaDataImpl.setScale(2, Integer.MIN_VALUE);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setSchemaName(int, String)}
     */
    public void test_setSchemaNameILjava_lang_String() throws SQLException {
        try {
            metaDataImpl.setSchemaName(-12, "asw");
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(7);
        assertEquals("", metaDataImpl.getSchemaName(3));
        metaDataImpl.setSchemaName(3, "schema name");
        assertEquals("schema name", metaDataImpl.getSchemaName(3));
        metaDataImpl.setSchemaName(3, null);
        assertEquals("", metaDataImpl.getSchemaName(3));

        try {
            metaDataImpl.setSchemaName(Integer.MIN_VALUE, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setSearchable(int, boolean)}
     */
    public void test_setSearchableIZ() throws SQLException {
        try {
            metaDataImpl.setSearchable(-22, true);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(8);
        assertFalse(metaDataImpl.isSearchable(2));
        metaDataImpl.setSearchable(2, true);
        assertTrue(metaDataImpl.isSearchable(2));
        metaDataImpl.setSearchable(2, false);
        assertFalse(metaDataImpl.isSearchable(2));

        try {
            metaDataImpl.setSearchable(Integer.MIN_VALUE, true);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setSigned(int, boolean)}
     */
    public void test_setSignedIZ() throws SQLException {
        try {
            metaDataImpl.setSigned(34, true);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(12);
        assertFalse(metaDataImpl.isSigned(12));
        metaDataImpl.setSigned(12, true);
        assertTrue(metaDataImpl.isSigned(12));
        metaDataImpl.setSigned(12, false);
        assertFalse(metaDataImpl.isSigned(12));

        try {
            metaDataImpl.setSigned(0, true);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests {@link javax.sql.rowset.RowSetMetaDataImpl#setTableName(int, String)}
     */
    public void test_setTableNameILjava_lang_String() throws SQLException {
        try {
            metaDataImpl.setTableName(34, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }

        metaDataImpl.setColumnCount(2);
        metaDataImpl.setTableName(2, "test");
        assertEquals("test", metaDataImpl.getTableName(2));
        metaDataImpl.setTableName(2, null);
        assertEquals("", metaDataImpl.getTableName(2));

        try {
            metaDataImpl.setTableName(-3, null);
            fail("should throw SQLException");
        } catch (SQLException e) {
            // expected
        }
    }

    /**
     * @tests serialization/deserialization.
     */
    public void test_serialization_self() throws Exception {
        RowSetMetaDataImpl impl = new RowSetMetaDataImpl();
        impl.setColumnCount(1);
        impl.setColumnType(1, Types.CHAR);
        SerializationTest.verifySelf(impl, ROWSET_METADATA_COMPARATOR);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void test_serialization_compatibility() throws Exception {
        RowSetMetaDataImpl impl = new RowSetMetaDataImpl();
        impl.setColumnCount(2);
        impl.setColumnType(1, Types.ARRAY);
        impl.setColumnType(2, Types.BIGINT);
        SerializationTest.verifyGolden(this, impl, ROWSET_METADATA_COMPARATOR);
    }

    /**
     *  @test {@link javax.sql.rowset.RowSetMetaDataImpl#unWrap(Class<T>)}
     */
    public void test_unWrap_CClass() throws Exception {
        Object o = metaDataImpl.unwrap(Integer.class);
        assertNotNull(o);
        assertTrue(o instanceof RowSetMetaDataImpl);
        
        o = metaDataImpl.unwrap(Comparable.class);
        assertNotNull(o);
        assertTrue(o instanceof RowSetMetaDataImpl);
        
        o = metaDataImpl.unwrap(null);
        assertNotNull(o);
        assertTrue(o instanceof RowSetMetaDataImpl);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        metaDataImpl = new RowSetMetaDataImpl();
    }

    @Override
    protected void tearDown() throws Exception {
        metaDataImpl = null;
        super.tearDown();
    }

}
