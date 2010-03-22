/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.sql.rowset;

import java.io.Serializable;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.sql.RowSetMetaData;

import org.apache.harmony.sql.internal.nls.Messages;

/**
 * This class is a concrete implementation of javax.sql.RowSetMetatData, which
 * provides methods that get and set column information.
 * 
 * A RowSetMetaDataImpl object can be obtained by the getMetaData() method in
 * javax.sql.RowSet.
 * 
 */
public class RowSetMetaDataImpl implements RowSetMetaData, Serializable {

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    private static final int DEFAULT_COLUMN_COUNT = 5;

    private static final long serialVersionUID = 6893806403181801867L;

    private int colCount;

    private ColInfo[] colInfo;

    /**
     * The default constructor.
     */
    public RowSetMetaDataImpl() {
        // do nothing
    }

    private void checkNegativeValue(int value, String msg) throws SQLException {
        if (value < 0) {
            throw new SQLException(Messages.getString(msg));
        }
    }

    private void checkColumnIndex(int columnIndex) throws SQLException {
        if (null == colInfo || columnIndex < 1 || columnIndex >= colInfo.length) {
            throw new SQLException(Messages
                    .getString("sql.27", columnIndex + 1)); //$NON-NLS-1$
        }
        // lazy initialization
        if (null == colInfo[columnIndex]) {
            colInfo[columnIndex] = new ColInfo();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setColumnCount(int)
     */
    public void setColumnCount(int columnCount) throws SQLException {
        if (columnCount <= 0) {
            throw new SQLException(Messages.getString("sql.26")); //$NON-NLS-1$
        }
        try {
            if (columnCount + 1 > 0) {
                colInfo = new ColInfo[columnCount + 1];
            } else {
                colInfo = new ColInfo[DEFAULT_COLUMN_COUNT];
            }
        } catch (OutOfMemoryError e) {
            // For compatibility, use same default value as RI
            colInfo = new ColInfo[DEFAULT_COLUMN_COUNT];
        }
        colCount = columnCount;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setAutoIncrement(int, boolean)
     */
    public void setAutoIncrement(int columnIndex, boolean property)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].autoIncrement = property;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setCaseSensitive(int, boolean)
     */
    public void setCaseSensitive(int columnIndex, boolean property)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].caseSensitive = property;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setSearchable(int, boolean)
     */
    public void setSearchable(int columnIndex, boolean property)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].searchable = property;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setCurrency(int, boolean)
     */
    public void setCurrency(int columnIndex, boolean property)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].currency = property;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setNullable(int, int)
     */
    public void setNullable(int columnIndex, int property) throws SQLException {
        if (property != ResultSetMetaData.columnNoNulls
                && property != ResultSetMetaData.columnNullable
                && property != ResultSetMetaData.columnNullableUnknown) {
            throw new SQLException(Messages.getString("sql.29")); //$NON-NLS-1$
        }

        checkColumnIndex(columnIndex);
        colInfo[columnIndex].nullable = property;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setSigned(int, boolean)
     */
    public void setSigned(int columnIndex, boolean property)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].signed = property;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setColumnDisplaySize(int, int)
     */
    public void setColumnDisplaySize(int columnIndex, int size)
            throws SQLException {
        checkNegativeValue(size, "sql.30"); //$NON-NLS-1$

        checkColumnIndex(columnIndex);
        colInfo[columnIndex].columnDisplaySize = size;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setColumnLabel(int, String)
     */
    public void setColumnLabel(int columnIndex, String label)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].columnLabel = label == null ? EMPTY_STRING : label;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setColumnName(int, String)
     */
    public void setColumnName(int columnIndex, String columnName)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].columnName = columnName == null ? EMPTY_STRING
                : columnName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setSchemaName(int, String)
     */
    public void setSchemaName(int columnIndex, String schemaName)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].schemaName = schemaName == null ? EMPTY_STRING
                : schemaName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setPrecision(int, int)
     */
    public void setPrecision(int columnIndex, int precision)
            throws SQLException {
        checkNegativeValue(precision, "sql.31"); //$NON-NLS-1$

        checkColumnIndex(columnIndex);
        colInfo[columnIndex].precision = precision;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setScale(int, int)
     */
    public void setScale(int columnIndex, int scale) throws SQLException {
        checkNegativeValue(scale, "sql.32"); //$NON-NLS-1$

        checkColumnIndex(columnIndex);
        colInfo[columnIndex].scale = scale;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setTableName(int, String)
     */
    public void setTableName(int columnIndex, String tableName)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].tableName = tableName == null ? EMPTY_STRING
                : tableName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setCatalogName(int, String)
     */
    public void setCatalogName(int columnIndex, String catalogName)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].catalogName = catalogName == null ? EMPTY_STRING
                : catalogName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setColumnType(int, int)
     */
    public void setColumnType(int columnIndex, int SQLType) throws SQLException {
        SqlUtil.validateType(SQLType);

        checkColumnIndex(columnIndex);
        colInfo[columnIndex].colType = SQLType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.sql.RowSetMetaData#setColumnTypeName(int, String)
     */
    public void setColumnTypeName(int columnIndex, String typeName)
            throws SQLException {
        checkColumnIndex(columnIndex);
        colInfo[columnIndex].colTypeName = typeName == null ? EMPTY_STRING
                : typeName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnCount()
     */
    public int getColumnCount() throws SQLException {
        return colCount;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isAutoIncrement(int)
     */
    public boolean isAutoIncrement(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].autoIncrement;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isCaseSensitive(int)
     */
    public boolean isCaseSensitive(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].caseSensitive;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isSearchable(int)
     */
    public boolean isSearchable(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].searchable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isCurrency(int)
     */
    public boolean isCurrency(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].currency;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isNullable(int)
     */
    public int isNullable(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].nullable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isSigned(int)
     */
    public boolean isSigned(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].signed;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnDisplaySize(int)
     */
    public int getColumnDisplaySize(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].columnDisplaySize;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnLabel(int)
     */
    public String getColumnLabel(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].columnLabel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnName(int)
     */
    public String getColumnName(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].columnName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getSchemaName(int)
     */
    public String getSchemaName(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].schemaName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getPrecision(int)
     */
    public int getPrecision(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].precision;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getScale(int)
     */
    public int getScale(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].scale;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getTableName(int)
     */
    public String getTableName(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].tableName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getCatalogName(int)
     */
    public String getCatalogName(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].catalogName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnType(int)
     */
    public int getColumnType(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].colType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnTypeName(int)
     */
    public String getColumnTypeName(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].colTypeName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isReadOnly(int)
     */
    public boolean isReadOnly(int columnIndex) throws SQLException {
        return !isWritable(columnIndex);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isWritable(int)
     */
    public boolean isWritable(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].writeable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isDefinitelyWritable(int)
     */
    public boolean isDefinitelyWritable(int columnIndex) throws SQLException {
        checkColumnIndex(columnIndex);
        return colInfo[columnIndex].definiteWritable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnClassName(int)
     */
    public String getColumnClassName(int columnIndex) throws SQLException {
        return SqlUtil.getClassNameByType(getColumnType(columnIndex));
    }

    /**
     * The inner class to store meta information of columns.
     */
    private class ColInfo implements Serializable {

        private static final long serialVersionUID = 5490834817919311283L;

        public boolean autoIncrement;

        public boolean caseSensitive;

        public boolean currency;

        public boolean signed;

        public boolean searchable;

        public boolean writeable = true;

        public boolean definiteWritable = true;

        public String columnLabel;

        public String columnName;

        public String schemaName = EMPTY_STRING;

        public String colTypeName;

        public int colType;

        public int nullable;

        public int columnDisplaySize;

        public int precision;

        public int scale;

        public String tableName = EMPTY_STRING;

        public String catalogName = EMPTY_STRING;
    }
}
