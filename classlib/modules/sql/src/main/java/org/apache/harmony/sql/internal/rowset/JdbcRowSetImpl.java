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

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Arrays;

import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetWarning;

import org.apache.harmony.sql.internal.nls.Messages;

public class JdbcRowSetImpl extends AbstractRowSetImpl implements JdbcRowSet {

    private String[] matchColumnNames;

    private int[] matchColumnIndexes;

    public void commit() throws SQLException {
        if (connection == null) {
            throw new NullPointerException();
        }
        connection.commit();
    }

    public boolean getAutoCommit() throws SQLException {
        if (connection == null) {
            throw new NullPointerException();
        }
        return connection.getAutoCommit();
    }

    public RowSetWarning getRowSetWarnings() throws SQLException {
        return null;
    }

    public void rollback() throws SQLException {
        if (connection == null) {
            throw new NullPointerException();
        }

        connection.rollback();
        statement = null;
        resultSet = null;
    }

    public void rollback(Savepoint s) throws SQLException {
        if (connection == null) {
            throw new NullPointerException();
        }

        connection.rollback(s);
        statement = null;
        resultSet = null;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (connection == null) {
            throw new NullPointerException();
        }

        connection.setAutoCommit(autoCommit);
    }

    public int[] getMatchColumnIndexes() throws SQLException {
        if (matchColumnIndexes == null || matchColumnIndexes.length == 0
                || matchColumnIndexes[0] == -1) {
            // rowset.13=Set Match columns before getting them
            throw new SQLException(Messages.getString("rowset.13")); //$NON-NLS-1$
        }

        return matchColumnIndexes.clone();
    }

    public String[] getMatchColumnNames() throws SQLException {
        if (matchColumnNames == null || matchColumnNames.length == 0
                || matchColumnNames[0] == null) {
            // rowset.13=Set Match columns before getting them
            throw new SQLException(Messages.getString("rowset.13")); //$NON-NLS-1$
        }
        return matchColumnNames.clone();
    }

    public void setMatchColumn(int columnIdx) throws SQLException {
        if (columnIdx < 0) {
            // TODO why is 0 valid?
            // rowset.20=Match columns should be greater than 0
            throw new SQLException(Messages.getString("rowset.20")); //$NON-NLS-1$
        }

        if (matchColumnIndexes == null) {
            /*
             * FIXME initial match column, the default length of array is 10 in
             * RI, we don't know why, just follow now
             */
            matchColumnIndexes = new int[10];
            Arrays.fill(matchColumnIndexes, -1);
        }

        matchColumnIndexes[0] = columnIdx;
    }

    public void setMatchColumn(int[] columnIdxes) throws SQLException {
        if (columnIdxes == null) {
            throw new NullPointerException();
        }

        for (int i : columnIdxes) {
            if (i < 0) {
                // TODO why is 0 valid?
                // rowset.20=Match columns should be greater than 0
                throw new SQLException(Messages.getString("rowset.20")); //$NON-NLS-1$
            }
        }

        if (matchColumnIndexes == null) {
            /*
             * FIXME initial match column, the default length of array is 10 in
             * RI, we don't know why, just follow now
             */
            matchColumnIndexes = new int[10];
            Arrays.fill(matchColumnIndexes, -1);
        }

        int[] newValue = new int[matchColumnIndexes.length + columnIdxes.length];
        System.arraycopy(columnIdxes, 0, newValue, 0, columnIdxes.length);
        System.arraycopy(matchColumnIndexes, 0, newValue, columnIdxes.length,
                matchColumnIndexes.length);

        matchColumnIndexes = newValue;
    }

    public void setMatchColumn(String columnName) throws SQLException {
        if (columnName == null || columnName.equals("")) { //$NON-NLS-1$
            // rowset.12=Match columns should not be empty or null string
            throw new SQLException(Messages.getString("rowset.12")); //$NON-NLS-1$
        }

        if (matchColumnNames == null) {
            /*
             * FIXME initial match column, the default length of array is 10 in
             * RI, we don't know why, just follow now
             */
            matchColumnNames = new String[10];
        }

        matchColumnNames[0] = columnName;
    }

    public void setMatchColumn(String[] columnNames) throws SQLException {
        if (columnNames == null) {
            throw new NullPointerException();
        }
        for (String name : columnNames) {
            if (name == null || name.equals("")) { //$NON-NLS-1$
                // rowset.12=Match columns should not be empty or null string
                throw new SQLException(Messages.getString("rowset.12")); //$NON-NLS-1$
            }
        }

        if (matchColumnNames == null) {
            /*
             * FIXME initial match column, the default length of array is 10 in
             * RI, we don't know why, just follow now
             */
            matchColumnNames = new String[10];
        }

        String[] newValue = new String[matchColumnNames.length
                + columnNames.length];
        System.arraycopy(columnNames, 0, newValue, 0, columnNames.length);
        System.arraycopy(matchColumnNames, 0, newValue, columnNames.length,
                matchColumnNames.length);

        matchColumnNames = newValue;
    }

    public void unsetMatchColumn(int columnIdx) throws SQLException {

        if (matchColumnIndexes == null || matchColumnIndexes.length == 0
                || matchColumnIndexes[0] != columnIdx) {
            throw new SQLException(Messages.getString("rowset.15")); //$NON-NLS-1$
        }

        matchColumnIndexes[0] = -1;
    }

    public void unsetMatchColumn(int[] columnIdxes) throws SQLException {
        if (columnIdxes == null) {
            throw new NullPointerException();
        }

        if (columnIdxes.length == 0) {
            return;
        }

        if (matchColumnIndexes == null
                || matchColumnIndexes.length < columnIdxes.length) {
            throw new SQLException(Messages.getString("rowset.15")); //$NON-NLS-1$
        }

        for (int i = 0; i < columnIdxes.length; i++) {
            if (matchColumnIndexes[i] != columnIdxes[i]) {
                throw new SQLException(Messages.getString("rowset.15")); //$NON-NLS-1$    
            }
        }

        Arrays.fill(matchColumnIndexes, 0, columnIdxes.length, -1);
    }

    public void unsetMatchColumn(String columnName) throws SQLException {
        if (matchColumnNames == null || matchColumnNames.length == 0
                || !matchColumnNames[0].equals(columnName)) {
            throw new SQLException(Messages.getString("rowset.15")); //$NON-NLS-1$
        }

        matchColumnNames[0] = null;

    }

    public void unsetMatchColumn(String[] columnName) throws SQLException {
        if (columnName == null) {
            throw new NullPointerException();
        }

        if (columnName.length == 0) {
            return;
        }

        if (matchColumnNames == null
                || matchColumnNames.length < columnName.length) {
            throw new SQLException(Messages.getString("rowset.15")); //$NON-NLS-1$
        }

        for (int i = 0; i < columnName.length; i++) {
            if (matchColumnNames[i] != columnName[i]) {
                throw new SQLException(Messages.getString("rowset.15")); //$NON-NLS-1$    
            }
        }

        Arrays.fill(matchColumnNames, 0, columnName.length, null);
    }
}
