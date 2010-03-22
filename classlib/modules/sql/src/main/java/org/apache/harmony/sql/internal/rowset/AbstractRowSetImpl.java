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
package org.apache.harmony.sql.internal.rowset;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.sql.RowSet;
import javax.sql.rowset.BaseRowSet;

import org.apache.harmony.sql.internal.nls.Messages;

public class AbstractRowSetImpl extends BaseRowSet implements RowSet {

    protected ResultSet resultSet;

    protected Connection connection;

    protected PreparedStatement statement;

    private boolean isClosed = false;

    public AbstractRowSetImpl() {
        initialProperties();
        initParams();
    }

    public void execute() throws SQLException {
        if (isClosed) {
            throw new SQLException(Messages.getString("rowset.31"));
        }

        if (connection != null) {
            connection.close();
        }

        connection = retrieveConnection();
        String localCommand = getCommand();
        if (localCommand == null || getParams() == null) {
            // rowset.16=Not a valid command
            throw new SQLException(Messages.getString("rowset.16")); //$NON-NLS-1$
        }

        statement = connection.prepareStatement(localCommand,
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        setParameter(statement);

        resultSet = statement.executeQuery();
    }

    private Connection retrieveConnection() throws SQLException {
        if (getUrl() == null && getDataSourceName() == null) {
            throw new NullPointerException();
        }

        if (getUrl() != null) {
            return DriverManager.getConnection(getUrl(), getUsername(), getPassword());
        } else if (getDataSourceName() != null) {
            try {
                Context contex = new InitialContext();
                DataSource ds = (DataSource) contex.lookup(getDataSourceName());
                return ds.getConnection();
            } catch (Exception e) {
                // rowset.25=(JNDI)Unable to get connection
                SQLException ex = new SQLException(Messages
                        .getString("rowset.25")); //$NON-NLS-1$
                throw ex;
            }
        }
        // rowset.24=Unable to get connection
        throw new SQLException(Messages.getString("rowset.24")); //$NON-NLS-1$
    }

    private void setParameter(PreparedStatement ps) throws SQLException {
        Object[] params = getParams();
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof Object[]) {
                Object[] objs = (Object[]) params[i];
                // character stream
                if (objs.length == 2) {
                    ps.setCharacterStream(i + 1, (Reader) objs[0],
                            ((Integer) objs[1]).intValue());
                } else {
                    int type = ((Integer) objs[2]).intValue();
                    switch (type) {
                    case BaseRowSet.ASCII_STREAM_PARAM:
                        ps.setAsciiStream(i + 1, (InputStream) objs[0],
                                ((Integer) objs[1]).intValue());
                        break;
                    case BaseRowSet.BINARY_STREAM_PARAM:
                        ps.setBinaryStream(i + 1, (InputStream) objs[0],
                                ((Integer) objs[1]).intValue());
                        break;
                    case BaseRowSet.UNICODE_STREAM_PARAM:
                        ps.setUnicodeStream(i + 1, (InputStream) objs[0],
                                ((Integer) objs[1]).intValue());
                        break;
                    }
                }
            } else {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    public boolean absolute(int row) throws SQLException {
        checkValid();
        return resultSet.absolute(row);
    }

    public void afterLast() throws SQLException {
        checkValid();
        resultSet.afterLast();
    }

    public void beforeFirst() throws SQLException {
        checkValid();
        resultSet.beforeFirst();
    }

    public void cancelRowUpdates() throws SQLException {
        checkValid();
        resultSet.cancelRowUpdates();
    }

    public void clearWarnings() throws SQLException {
        checkValid();
        resultSet.clearWarnings();
    }

    public void close() throws SQLException {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

            if (statement != null) {
                statement.close();
            }

            if (connection != null) {
                connection.close();
            }

        } finally {
            isClosed = true;
        }
    }

    public void deleteRow() throws SQLException {
        checkValid();
        resultSet.deleteRow();
    }

    public int findColumn(String columnName) throws SQLException {
        checkValid();
        return resultSet.findColumn(columnName);
    }

    public boolean first() throws SQLException {
        checkValid();
        return resultSet.first();
    }

    public Array getArray(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getArray(columnIndex);
    }

    public Array getArray(String colName) throws SQLException {
        checkValid();
        return resultSet.getArray(colName);
    }

    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getAsciiStream(columnIndex);
    }

    public InputStream getAsciiStream(String columnName) throws SQLException {
        checkValid();
        return resultSet.getAsciiStream(columnName);
    }

    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getBigDecimal(columnIndex);
    }

    public BigDecimal getBigDecimal(int columnIndex, int scale)
            throws SQLException {
        checkValid();
        return resultSet.getBigDecimal(columnIndex, scale);
    }

    public BigDecimal getBigDecimal(String columnName) throws SQLException {
        checkValid();
        return resultSet.getBigDecimal(columnName);
    }

    public BigDecimal getBigDecimal(String columnName, int scale)
            throws SQLException {
        checkValid();
        return resultSet.getBigDecimal(columnName, scale);
    }

    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getBinaryStream(columnIndex);
    }

    public InputStream getBinaryStream(String columnName) throws SQLException {
        checkValid();
        return resultSet.getBinaryStream(columnName);
    }

    public Blob getBlob(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getBlob(columnIndex);
    }

    public Blob getBlob(String columnName) throws SQLException {
        checkValid();
        return resultSet.getBlob(columnName);
    }

    public boolean getBoolean(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getBoolean(columnIndex);
    }

    public boolean getBoolean(String columnName) throws SQLException {
        checkValid();
        return resultSet.getBoolean(columnName);
    }

    public byte getByte(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getByte(columnIndex);
    }

    public byte getByte(String columnName) throws SQLException {
        checkValid();
        return resultSet.getByte(columnName);
    }

    public byte[] getBytes(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getBytes(columnIndex);
    }

    public byte[] getBytes(String columnName) throws SQLException {
        checkValid();
        return resultSet.getBytes(columnName);
    }

    public Reader getCharacterStream(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getCharacterStream(columnIndex);
    }

    public Reader getCharacterStream(String columnName) throws SQLException {
        checkValid();
        return resultSet.getCharacterStream(columnName);
    }

    public Clob getClob(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getClob(columnIndex);
    }

    public Clob getClob(String colName) throws SQLException {
        checkValid();
        return resultSet.getClob(colName);
    }

    public String getCursorName() throws SQLException {
        checkValid();
        return resultSet.getCursorName();
    }

    public Date getDate(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getDate(columnIndex);
    }

    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        checkValid();
        return resultSet.getDate(columnIndex, cal);
    }

    public Date getDate(String columnName) throws SQLException {
        checkValid();
        return resultSet.getDate(columnName);
    }

    public Date getDate(String columnName, Calendar cal) throws SQLException {
        checkValid();
        return resultSet.getDate(columnName, cal);
    }

    public double getDouble(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getDouble(columnIndex);
    }

    public double getDouble(String columnName) throws SQLException {
        checkValid();
        return resultSet.getDouble(columnName);
    }

    public float getFloat(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getFloat(columnIndex);
    }

    public float getFloat(String columnName) throws SQLException {
        checkValid();
        return resultSet.getFloat(columnName);
    }

    public int getInt(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getInt(columnIndex);
    }

    public int getInt(String columnName) throws SQLException {
        checkValid();
        return resultSet.getInt(columnName);
    }

    public long getLong(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getLong(columnIndex);
    }

    public long getLong(String columnName) throws SQLException {
        checkValid();
        return resultSet.getLong(columnName);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        checkValid();
        return resultSet.getMetaData();
    }

    public Object getObject(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getObject(columnIndex);
    }

    public Object getObject(int columnIndex, Map<String, Class<?>> map)
            throws SQLException {
        checkValid();
        return resultSet.getObject(columnIndex, map);
    }

    public Object getObject(String columnName) throws SQLException {
        checkValid();
        return resultSet.getObject(columnName);
    }

    public Object getObject(String columnName, Map<String, Class<?>> map)
            throws SQLException {
        checkValid();
        return resultSet.getObject(columnName, map);
    }

    public Ref getRef(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getRef(columnIndex);
    }

    public Ref getRef(String colName) throws SQLException {
        checkValid();
        return resultSet.getRef(colName);
    }

    public int getRow() throws SQLException {
        checkValid();
        return resultSet.getRow();
    }

    public short getShort(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getShort(columnIndex);
    }

    public short getShort(String columnName) throws SQLException {
        checkValid();
        return resultSet.getShort(columnName);
    }

    public Statement getStatement() throws SQLException {
        if (statement != null && isClosed) {
            throw new SQLException();
        }
        return statement;
    }

    public String getString(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getString(columnIndex);
    }

    public String getString(String columnName) throws SQLException {
        checkValid();
        return resultSet.getString(columnName);
    }

    public Time getTime(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getTime(columnIndex);
    }

    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        checkValid();
        return resultSet.getTime(columnIndex, cal);
    }

    public Time getTime(String columnName) throws SQLException {
        checkValid();
        return resultSet.getTime(columnName);
    }

    public Time getTime(String columnName, Calendar cal) throws SQLException {
        checkValid();
        return resultSet.getTime(columnName, cal);
    }

    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getTimestamp(columnIndex);
    }

    public Timestamp getTimestamp(int columnIndex, Calendar cal)
            throws SQLException {
        checkValid();
        return resultSet.getTimestamp(columnIndex, cal);
    }

    public Timestamp getTimestamp(String columnName) throws SQLException {
        checkValid();
        return resultSet.getTimestamp(columnName);
    }

    public Timestamp getTimestamp(String columnName, Calendar cal)
            throws SQLException {
        checkValid();
        return resultSet.getTimestamp(columnName, cal);
    }

    public java.net.URL getURL(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getURL(columnIndex);
    }

    public java.net.URL getURL(String columnName) throws SQLException {
        checkValid();
        return resultSet.getURL(columnName);
    }

    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        checkValid();
        return resultSet.getUnicodeStream(columnIndex);
    }

    public InputStream getUnicodeStream(String columnName) throws SQLException {
        checkValid();
        return resultSet.getUnicodeStream(columnName);
    }

    public SQLWarning getWarnings() throws SQLException {
        checkValid();
        return resultSet.getWarnings();
    }

    public void insertRow() throws SQLException {
        checkValid();
        resultSet.insertRow();
    }

    public boolean isAfterLast() throws SQLException {
        checkValid();
        return resultSet.isAfterLast();
    }

    public boolean isBeforeFirst() throws SQLException {
        checkValid();
        return resultSet.isBeforeFirst();
    }

    public boolean isFirst() throws SQLException {
        checkValid();
        return resultSet.isFirst();
    }

    public boolean isLast() throws SQLException {
        checkValid();
        return resultSet.isLast();
    }

    public boolean last() throws SQLException {
        checkValid();
        return resultSet.last();
    }

    public void moveToCurrentRow() throws SQLException {
        checkValid();
        resultSet.moveToCurrentRow();
    }

    public void moveToInsertRow() throws SQLException {
        checkValid();
        resultSet.moveToInsertRow();
    }

    public boolean next() throws SQLException {
        checkValid();
        return resultSet.next();
    }

    public boolean previous() throws SQLException {
        checkValid();
        return resultSet.previous();
    }

    public void refreshRow() throws SQLException {
        checkValid();
        resultSet.refreshRow();
    }

    public boolean relative(int rows) throws SQLException {
        checkValid();
        return resultSet.relative(rows);
    }

    public boolean rowDeleted() throws SQLException {
        checkValid();
        return resultSet.rowDeleted();
    }

    public boolean rowInserted() throws SQLException {
        checkValid();
        return resultSet.rowInserted();
    }

    public boolean rowUpdated() throws SQLException {
        checkValid();
        return resultSet.rowUpdated();
    }

    public void updateArray(int columnIndex, Array x) throws SQLException {
        checkValid();
        resultSet.updateArray(columnIndex, x);
    }

    public void updateArray(String columnName, Array x) throws SQLException {
        checkValid();
        resultSet.updateArray(columnName, x);
    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length)
            throws SQLException {
        checkValid();
        resultSet.updateAsciiStream(columnIndex, x, length);
    }

    public void updateAsciiStream(String columnName, InputStream x, int length)
            throws SQLException {
        checkValid();
        resultSet.updateAsciiStream(columnName, x, length);
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x)
            throws SQLException {
        checkValid();
        resultSet.updateBigDecimal(columnIndex, x);
    }

    public void updateBigDecimal(String columnName, BigDecimal x)
            throws SQLException {
        checkValid();
        resultSet.updateBigDecimal(columnName, x);
    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length)
            throws SQLException {
        checkValid();
        resultSet.updateBinaryStream(columnIndex, x, length);
    }

    public void updateBinaryStream(String columnName, InputStream x, int length)
            throws SQLException {
        checkValid();
        resultSet.updateBinaryStream(columnName, x, length);
    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        checkValid();
        resultSet.updateBlob(columnIndex, x);
    }

    public void updateBlob(String columnName, Blob x) throws SQLException {
        checkValid();
        resultSet.updateBlob(columnName, x);
    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        checkValid();
        resultSet.updateBoolean(columnIndex, x);
    }

    public void updateBoolean(String columnName, boolean x) throws SQLException {
        checkValid();
        resultSet.updateBoolean(columnName, x);
    }

    public void updateByte(int columnIndex, byte x) throws SQLException {
        checkValid();
        resultSet.updateByte(columnIndex, x);
    }

    public void updateByte(String columnName, byte x) throws SQLException {
        checkValid();
        resultSet.updateByte(columnName, x);
    }

    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        checkValid();
        resultSet.updateBytes(columnIndex, x);
    }

    public void updateBytes(String columnName, byte[] x) throws SQLException {
        checkValid();
        resultSet.updateBytes(columnName, x);
    }

    public void updateCharacterStream(int columnIndex, Reader x, int length)
            throws SQLException {
        checkValid();
        resultSet.updateCharacterStream(columnIndex, x, length);
    }

    public void updateCharacterStream(String columnName, Reader reader,
            int length) throws SQLException {
        checkValid();
        resultSet.updateCharacterStream(columnName, reader, length);
    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {
        checkValid();
        resultSet.updateClob(columnIndex, x);
    }

    public void updateClob(String columnName, Clob x) throws SQLException {
        checkValid();
        resultSet.updateClob(columnName, x);
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        checkValid();
        resultSet.updateDate(columnIndex, x);
    }

    public void updateDate(String columnName, Date x) throws SQLException {
        checkValid();
        resultSet.updateDate(columnName, x);
    }

    public void updateDouble(int columnIndex, double x) throws SQLException {
        checkValid();
        resultSet.updateDouble(columnIndex, x);
    }

    public void updateDouble(String columnName, double x) throws SQLException {
        checkValid();
        resultSet.updateDouble(columnName, x);
    }

    public void updateFloat(int columnIndex, float x) throws SQLException {
        checkValid();
        resultSet.updateFloat(columnIndex, x);
    }

    public void updateFloat(String columnName, float x) throws SQLException {
        checkValid();
        resultSet.updateFloat(columnName, x);
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        checkValid();
        resultSet.updateInt(columnIndex, x);
    }

    public void updateInt(String columnName, int x) throws SQLException {
        checkValid();
        resultSet.updateInt(columnName, x);
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        checkValid();
        resultSet.updateLong(columnIndex, x);
    }

    public void updateLong(String columnName, long x) throws SQLException {
        checkValid();
        resultSet.updateLong(columnName, x);
    }

    public void updateNull(int columnIndex) throws SQLException {
        checkValid();
        resultSet.updateNull(columnIndex);
    }

    public void updateNull(String columnName) throws SQLException {
        checkValid();
        resultSet.updateNull(columnName);
    }

    public void updateObject(int columnIndex, Object x) throws SQLException {
        checkValid();
        resultSet.updateObject(columnIndex, x);
    }

    public void updateObject(int columnIndex, Object x, int scale)
            throws SQLException {
        checkValid();
        resultSet.updateObject(columnIndex, x, scale);
    }

    public void updateObject(String columnName, Object x) throws SQLException {
        checkValid();
        resultSet.updateObject(columnName, x);
    }

    public void updateObject(String columnName, Object x, int scale)
            throws SQLException {
        checkValid();
        resultSet.updateObject(columnName, x, scale);
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {
        checkValid();
        resultSet.updateRef(columnIndex, x);
    }

    public void updateRef(String columnName, Ref x) throws SQLException {
        checkValid();
        resultSet.updateRef(columnName, x);
    }

    public void updateRow() throws SQLException {
        checkValid();
        resultSet.updateRow();
    }

    public void updateShort(int columnIndex, short x) throws SQLException {
        checkValid();
        resultSet.updateShort(columnIndex, x);
    }

    public void updateShort(String columnName, short x) throws SQLException {
        checkValid();
        resultSet.updateShort(columnName, x);
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        checkValid();
        resultSet.updateString(columnIndex, x);
    }

    public void updateString(String columnName, String x) throws SQLException {
        checkValid();
        resultSet.updateString(columnName, x);
    }

    public void updateTime(int columnIndex, Time x) throws SQLException {
        checkValid();
        resultSet.updateTime(columnIndex, x);
    }

    public void updateTime(String columnName, Time x) throws SQLException {
        checkValid();
        resultSet.updateTime(columnName, x);
    }

    public void updateTimestamp(int columnIndex, Timestamp x)
            throws SQLException {
        checkValid();
        resultSet.updateTimestamp(columnIndex, x);
    }

    public void updateTimestamp(String columnName, Timestamp x)
            throws SQLException {
        checkValid();
        resultSet.updateTimestamp(columnName, x);
    }

    public boolean wasNull() throws SQLException {
        checkValid();
        return resultSet.wasNull();
    }

    public int getConcurrency() throws SQLException {
        if (resultSet == null) {
            throw new NullPointerException();
        }

        return resultSet.getConcurrency();
    }

    public int getFetchDirection() throws SQLException {
        if (resultSet == null) {
            throw new NullPointerException();
        }

        return resultSet.getFetchDirection();
    }

    private void checkValid() throws SQLException {
        if (resultSet == null && connection == null) {
            throw new SQLException(Messages.getString("rowset.30")); //$NON-NLS-1$
        }

        if (resultSet == null && connection != null) {
            throw new NullPointerException();
        }
    }

    private void initialProperties() {
        try {
            setEscapeProcessing(true);
            setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            setConcurrency(ResultSet.CONCUR_UPDATABLE);
            setType(ResultSet.TYPE_SCROLL_INSENSITIVE);
            setMaxRows(0);
            setQueryTimeout(0);
            setShowDeleted(false);
            setUsername(null);
            setPassword(null);
            setMaxFieldSize(0);
            setTypeMap(null);
            setFetchSize(0);
        } catch (SQLException e) {
            // ignore, never reached
        }

    }
}
