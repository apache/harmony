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

package javax.sql.rowset;

import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetListener;
import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialRef;

public abstract class BaseRowSet implements Cloneable, Serializable {
    private static final long serialVersionUID = 4886719666485113312L;

    public static final int UNICODE_STREAM_PARAM = 0;

    public static final int BINARY_STREAM_PARAM = 1;

    public static final int ASCII_STREAM_PARAM = 2;

    protected InputStream binaryStream;

    protected InputStream unicodeStream;

    protected InputStream asciiStream;

    protected Reader charStream;

    private String command;

    private String URL;

    private String dataSource;

    private int rowSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;

    private boolean showDeleted;

    private int queryTimeout;

    private int maxRows;

    private int maxFieldSize;

    private int concurrency = ResultSet.CONCUR_UPDATABLE;

    //compatiable with RI, default: true
    private boolean readOnly = true;

    private boolean escapeProcessing;

    private int isolation;

    private int fetchDir = ResultSet.FETCH_FORWARD;

    private int fetchSize;

    private Map<String, Class<?>> map;

    private Vector<RowSetListener> listeners;

    private Hashtable<Object, Object> params;

    private transient String username;

    private transient String password;

    public BaseRowSet() {
        super();
        listeners = new Vector<RowSetListener>();
    }

    protected void initParams() {
        params = new Hashtable<Object, Object>();
    }

    public void addRowSetListener(RowSetListener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
    }

    public void removeRowSetListener(RowSetListener listener) {
        if (listener == null) {
            return;
        }
        listeners.remove(listener);
    }

    protected void notifyCursorMoved() throws SQLException {
        if (!(this instanceof RowSet)) {
            throw new SQLException();
        }
        if (listeners.isEmpty()) {
            return;
        }
        for (RowSetListener listener : listeners) {
            listener.cursorMoved(new RowSetEvent((RowSet) this));
        }
    }

    protected void notifyRowChanged() throws SQLException {
        if (!(this instanceof RowSet)) {
            throw new SQLException();
        }
        if (listeners.isEmpty()) {
            return;
        }
        for (RowSetListener listener : listeners) {
            listener.rowChanged(new RowSetEvent((RowSet) this));
        }
    }

    protected void notifyRowSetChanged() throws SQLException {
        if (!(this instanceof RowSet)) {
            throw new SQLException();
        }
        if (listeners.isEmpty()) {
            return;
        }
        for (RowSetListener listener : listeners) {
            listener.rowSetChanged(new RowSetEvent((RowSet) this));
        }
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String cmd) throws SQLException {
        // null is allowed, but empty is not
        if (cmd != null && cmd.length() == 0) {
            throw new SQLException();
        }
        this.command = cmd;
        clearParameters();
    }

    public String getUrl() throws SQLException {
        // TODO interrogate the DataSource
        return URL;
    }

    public void setUrl(String url) throws SQLException {
        // null is allowed, but empty is not
        if (url != null && url.length() == 0) {
            throw new SQLException();
        }
        this.URL = url;
        this.dataSource = null;
    }

    public String getDataSourceName() {
        return dataSource;
    }

    public void setDataSourceName(String name) throws SQLException {
        // null is allowed, but empty is not
        if (name != null && name.length() == 0) {
            throw new SQLException();
        }
        this.dataSource = name;
        this.URL = null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setType(int type) throws SQLException {
        switch (type) {
        case ResultSet.TYPE_FORWARD_ONLY:
        case ResultSet.TYPE_SCROLL_INSENSITIVE:
        case ResultSet.TYPE_SCROLL_SENSITIVE: {
            this.rowSetType = type;
            return;
        }
        default: {
            throw new SQLException();
        }
        }
    }

    public int getType() throws SQLException {
        return rowSetType;
    }

    public void setConcurrency(int concurrency) throws SQLException {
        switch (concurrency) {
        case ResultSet.CONCUR_READ_ONLY:
        case ResultSet.CONCUR_UPDATABLE: {
            this.concurrency = concurrency;
            return;
        }
        default: {
            throw new SQLException();
        }
        }
    }

    public int getConcurrency() throws SQLException {
        return concurrency;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean value) {
        this.readOnly = value;
    }

    public int getTransactionIsolation() {
        return isolation;
    }

    public void setTransactionIsolation(int level) throws SQLException {
        switch (level) {
        case Connection.TRANSACTION_NONE:
        case Connection.TRANSACTION_READ_UNCOMMITTED:
        case Connection.TRANSACTION_READ_COMMITTED:
        case Connection.TRANSACTION_REPEATABLE_READ:
        case Connection.TRANSACTION_SERIALIZABLE: {
            this.isolation = level;
            return;
        }
        default: {
            throw new SQLException();
        }
        }
    }

    public Map<String, Class<?>> getTypeMap() {
        return map;
    }

    public void setTypeMap(Map<String, Class<?>> map) {
        this.map = map;
    }

    public int getMaxFieldSize() throws SQLException {
        return maxFieldSize;
    }

    public void setMaxFieldSize(int max) throws SQLException {
        // TODO test maximum based on field type
        this.maxFieldSize = max;
    }

    public int getMaxRows() throws SQLException {
        return maxRows;
    }

    public void setMaxRows(int max) throws SQLException {
        this.maxRows = max;
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        this.escapeProcessing = enable;
    }

    public boolean getEscapeProcessing() throws SQLException {
        return escapeProcessing;
    }

    public int getQueryTimeout() throws SQLException {
        return queryTimeout;
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        if (seconds < 0) {
            throw new SQLException();
        }
        this.queryTimeout = seconds;
    }

    public boolean getShowDeleted() throws SQLException {
        return showDeleted;
    }

    public void setShowDeleted(boolean value) throws SQLException {
        this.showDeleted = value;
    }

    public void setFetchDirection(int direction) throws SQLException {
        switch (direction) {
        case ResultSet.FETCH_REVERSE:
        case ResultSet.FETCH_UNKNOWN: {
            if (rowSetType == ResultSet.TYPE_FORWARD_ONLY) {
                throw new SQLException();
            }
        }
        case ResultSet.FETCH_FORWARD: {
            this.fetchDir = direction;
            return;
        }
        default: {
            throw new SQLException();
        }
        }
    }

    public int getFetchDirection() throws SQLException {
        return fetchDir;
    }

    public void setFetchSize(int rows) throws SQLException {
        if (rows < 0) {
            throw new SQLException();
        }
        if (maxRows != 0 && rows > maxRows) {
            throw new SQLException();
        }
        this.fetchSize = rows;
    }

    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[2];
        value[1] = Integer.valueOf(sqlType);
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void setNull(int parameterIndex, int sqlType, String typeName)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[3];
        value[1] = Integer.valueOf(sqlType);
        value[2] = typeName;
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), Boolean.valueOf(x));
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), Byte.valueOf(x));
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), Short.valueOf(x));
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), Integer.valueOf(x));
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), Long.valueOf(x));
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), Float.valueOf(x));
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), Double.valueOf(x));
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    public void setTimestamp(int parameterIndex, Timestamp x)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[3];
        value[0] = x;
        value[1] = Integer.valueOf(length);
        value[2] = Integer.valueOf(ASCII_STREAM_PARAM);
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[3];
        value[0] = x;
        value[1] = Integer.valueOf(length);
        value[2] = Integer.valueOf(BINARY_STREAM_PARAM);
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    @Deprecated
    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[3];
        value[0] = x;
        value[1] = Integer.valueOf(length);
        value[2] = Integer.valueOf(UNICODE_STREAM_PARAM);
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[2];
        value[0] = reader;
        value[1] = Integer.valueOf(length);
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType,
            int scale) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[3];
        value[0] = x;
        value[1] = Integer.valueOf(targetSqlType);
        value[2] = Integer.valueOf(scale);
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[2];
        value[0] = x;
        value[1] = Integer.valueOf(targetSqlType);
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    public void setRef(int parameterIndex, Ref ref) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), new SerialRef(ref));
    }

    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), new SerialBlob(x));
    }

    public void setClob(int parameterIndex, Clob x) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), new SerialClob(x));
    }

    public void setArray(int parameterIndex, Array array) throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        params.put(Integer.valueOf(parameterIndex - 1), new SerialArray(array));
    }

    public void setDate(int parameterIndex, Date x, Calendar cal)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[2];
        value[0] = x;
        value[1] = cal;
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[2];
        value[0] = x;
        value[1] = cal;
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
            throws SQLException {
        if (parameterIndex < 1) {
            throw new SQLException();
        }
        if (params == null) {
            throw new SQLException();
        }
        Object[] value = new Object[2];
        value[0] = x;
        value[1] = cal;
        params.put(Integer.valueOf(parameterIndex - 1), value);
    }

    public void clearParameters() throws SQLException {
        if (params == null) {
            return;
        }
        params.clear();
    }

    public Object[] getParams() throws SQLException {
        if (params == null) {
            return new Object[0];
        }
        Object[] result = new Object[params.size()];
        for (int i = 0; i < result.length; i++) {
            Object param = params.get(Integer.valueOf(i));
            if (param == null) {
                throw new SQLException();
            }
            result[i] = param;
        }
        return result;
    }

}
