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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.sql.RowSet;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.JoinRowSet;
import javax.sql.rowset.Joinable;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.spi.SyncFactoryException;
import javax.sql.rowset.spi.SyncProviderException;

import org.apache.harmony.sql.internal.nls.Messages;

public class JoinRowSetImpl extends WebRowSetImpl implements JoinRowSet {

    private List<RowSet> rsList;

    private CachedRowSet copyFirstRs;

    private List<Integer> matchColIndexs;

    private List<String> matchColNames;

    private int joinType;

    private static String MERGED_COLUMN_NAME = "MergedCol"; //$NON-NLS-1$

    // Whether the rows can be sorted using object in matched index.
    private boolean isSortable;

    public JoinRowSetImpl() throws SyncFactoryException {
        super();
        initProperties();
    }

    @Override
    public void acceptChanges() throws SyncProviderException {
        acceptChanges(conn);
    }

    @Override
    protected boolean doAbsolute(int row, boolean checkType)
            throws SQLException {
        return super.doAbsolute(row, false);
    }

    private void initProperties() {
        rsList = new ArrayList<RowSet>();
        matchColIndexs = new ArrayList<Integer>();
        matchColNames = new ArrayList<String>();
        joinType = INNER_JOIN;
        setIsNotifyListener(false);
        rows = new ArrayList<CachedRow>();
    }

    private void composeMetaData(ResultSetMetaData rsmd, int matchColumn)
            throws SQLException {
        if (getMetaData() == null) {
            if (rsmd instanceof RowSetMetaData) {
                setMetaData((RowSetMetaData) rsmd);
            } else {
                setMetaData(copyMetaData(rsmd));
            }
        } else {
            int colCount = getMetaData().getColumnCount()
                    + rsmd.getColumnCount() - 1;
            RowSetMetaData rowSetMetaData = new RowSetMetaDataImpl();
            rowSetMetaData.setColumnCount(colCount);
            for (int i = 1; i <= getMetaData().getColumnCount(); i++) {
                doCopyMetaData(rowSetMetaData, i, getMetaData(), i);
                if (i == matchColIndexs.get(0).intValue()) {
                    rowSetMetaData.setColumnName(i, MERGED_COLUMN_NAME);
                }
            }
            int index = 0;
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                if (j == matchColumn) {
                    continue;
                }
                index++;
                doCopyMetaData(rowSetMetaData, getMetaData().getColumnCount()
                        + index, rsmd, j);
            }
            setMetaData(rowSetMetaData);
        }
    }

    public void addRowSet(Joinable rowset) throws SQLException {
        if (rowset == null || !(rowset instanceof RowSet)) {
            // rowset.33=Not a rowset
            throw new SQLException(Messages.getString("rowset.33")); //$NON-NLS-1$
        }

        RowSet currentRs = (RowSet) rowset;
        if (currentRs.getMetaData() == null) {
            // rowset.32=The given rowset is empty
            throw new SQLException(Messages.getString("rowset.32")); //$NON-NLS-1$
        }

        int matchCol = -1;
        try {
            if (rowset.getMatchColumnIndexes() != null
                    && rowset.getMatchColumnIndexes().length > 0) {
                matchCol = rowset.getMatchColumnIndexes()[0];
                if (matchCol <= 0
                        || matchCol > currentRs.getMetaData().getColumnCount()) {
                    matchCol = -2;
                }
            }
        } catch (SQLException e) {
            try {
                if (rowset.getMatchColumnNames() != null
                        && rowset.getMatchColumnNames().length > 0) {
                    try {
                        matchCol = currentRs.findColumn(rowset
                                .getMatchColumnNames()[0]);
                    } catch (SQLException e1) {
                        matchCol = -3;
                    }
                }
            } catch (SQLException e2) {
                // ignore
            }
        } finally {
            if (matchCol == -1) {
                // rowset.34=Not set a match column
                throw new SQLException(Messages.getString("rowset.34")); //$NON-NLS-1$
            } else if (matchCol == -2) {
                // rowset.35=Not a valid match olumn index
                throw new SQLException(Messages.getString("rowset.35")); //$NON-NLS-1$
            } else if (matchCol == -3) {
                // rowset.1=Not a valid column name
                throw new SQLException(Messages.getString("rowset.1")); //$NON-NLS-1$
            }
        }
        addRowSet(currentRs, matchCol);
    }

    public void addRowSet(RowSet rowset, int columnIdx) throws SQLException {
        if (rowset == null) {
            throw new NullPointerException();
        }
        if (rowset.getMetaData() == null) {
            // rowset.32=The given rowset is empty
            throw new SQLException(Messages.getString("rowset.32")); //$NON-NLS-1$
        }
        if (columnIdx <= 0 || columnIdx > rowset.getMetaData().getColumnCount()) {
            // rowset.35=Not a valid match olumn index
            throw new SQLException(Messages.getString("rowset.35")); //$NON-NLS-1$
        }

        int type = rowset.getMetaData().getColumnType(columnIdx);
        if (getMetaData() != null
                && getMetaData().getColumnType(getMatchColumnIndexes()[0]) != type) {
            setMetaData(null);
            rows = null;
            // rowset.10=Data Type Mismatch
            throw new SQLException(Messages.getString("rowset.10")); //$NON-NLS-1$
        }

        if (getMetaData() == null
                && (type == Types.BINARY || type == Types.LONGVARBINARY
                        || type == Types.VARBINARY || type == Types.BLOB
                        || type == Types.CLOB || type == Types.ARRAY
                        || type == Types.LONGVARCHAR
                        || type == Types.JAVA_OBJECT || type == Types.OTHER || type == Types.NULL)) {
            // rowset.10=Data Type Mismatch
            throw new SQLException(Messages.getString("rowset.10")); //$NON-NLS-1$
        }

        composeMetaData(rowset.getMetaData(), columnIdx);

        columnCount = getMetaData().getColumnCount();
        // Reassign columnTypes.
        columnTypes = new Class[columnCount];
        for (int i = 1; i <= columnTypes.length; ++i) {
            columnTypes[i - 1] = TYPE_MAPPING.get(Integer.valueOf(meta
                    .getColumnType(i)));
        }

        matchColIndexs.add(Integer.valueOf(columnIdx));

        if (rowset instanceof JdbcRowSet) {
            CachedRowSet convertRowset = new CachedRowSetImpl();
            convertRowset.populate(rowset);
            rsList.add(convertRowset);
        } else {
            rsList.add(rowset);
        }

        if (rsList.size() == 1) {
            initSortable();
        }
        try {
            innerJoin();
        } catch (CloneNotSupportedException e) {
            SQLException sqlException = new SQLException();
            sqlException.initCause(e);
            throw sqlException;
        }

        if (rsList.size() == 1) {
            copyFirstRs = ((CachedRowSet) rsList.get(0)).createCopy();
            setMatchColumn(columnIdx);
        }
    }

    public void addRowSet(RowSet rowset, String columnName) throws SQLException {
        if (rowset == null) {
            throw new NullPointerException();
        }
        if (rowset.getMetaData() == null) {
            // rowset.32=The given rowset is empty
            throw new SQLException(Messages.getString("rowset.32")); //$NON-NLS-1$
        }

        int columnIdx = -1;
        try {
            columnIdx = rowset.findColumn(columnName);
        } catch (SQLException e) {
            throw e;
        }
        addRowSet(rowset, columnIdx);
    }

    public void addRowSet(RowSet[] rowset, int[] columnIdx) throws SQLException {
        if (rowset == null || columnIdx == null || rowset.length == 0
                || columnIdx.length == 0) {
            throw new NullPointerException();
        }
        if (rowset.length != columnIdx.length) {
            // rowset.36=Number of elements of two arrays don't equal
            throw new SQLException(Messages.getString("rowset.36")); //$NON-NLS-1$
        }
        for (int i = 0; i < rowset.length; i++) {
            addRowSet(rowset[i], columnIdx[i]);
        }
    }

    public void addRowSet(RowSet[] rowset, String[] columnName)
            throws SQLException {
        if (rowset == null || columnName == null || rowset.length == 0
                || columnName.length == 0) {
            throw new NullPointerException();
        }
        if (rowset.length != columnName.length) {
            // rowset.36=Number of elements of two arrays don't equal
            throw new SQLException(Messages.getString("rowset.36")); //$NON-NLS-1$
        }
        for (int i = 0; i < rowset.length; i++) {
            addRowSet(rowset[i], columnName[i]);
        }
    }

    public int getJoinType() throws SQLException {
        return joinType;
    }

    public String[] getRowSetNames() throws SQLException {
        if (rsList == null || rsList.size() == 0) {
            return new String[0];
        }
        String[] rowsetNames = new String[rsList.size()];
        for (int i = 0; i < rsList.size(); i++) {
            if (rsList.get(i) instanceof CachedRowSet) {
                CachedRowSet cachedRs = (CachedRowSet) rsList.get(i);
                if (cachedRs.getTableName() == null) {
                    // rowset.37=The RowSet doesn't set the table name
                    throw new SQLException(Messages.getString("rowset.37")); //$NON-NLS-1$
                }
                rowsetNames[i] = cachedRs.getTableName();
            } else {
                // rowset.37=The RowSet doesn't set the table name
                throw new SQLException(Messages.getString("rowset.37")); //$NON-NLS-1$
            }
        }
        return rowsetNames;
    }

    public Collection<?> getRowSets() throws SQLException {
        return rsList;
    }

    /**
     * Gets a sql clause which specify the join action.
     */
    public String getWhereClause() throws SQLException {
        int size = rsList.size();

        // If 0 rowSets in it, return "".
        if (size == 0) {
            return ""; //$NON-NLS-1$
        }

        String whereClause;
        String tableName;
        int metaColumnCount;
        int matchIndex;
        if (size == 1) {
            tableName = ((CachedRowSet) rsList.get(0)).getTableName();
            if (tableName == null) {
                throw new SQLException(Messages.getString("rowset.39")); //$NON-NLS-1$
            }
            metaColumnCount = meta.getColumnCount();
            whereClause = "Select"; //$NON-NLS-1$
            for (int i = 1; i <= metaColumnCount; i++) {
                whereClause += " " + tableName + "." + meta.getColumnName(i); //$NON-NLS-1$ //$NON-NLS-2$
                if (i < metaColumnCount) {
                    whereClause += ","; //$NON-NLS-1$
                } else {
                    whereClause += " from " + tableName; //$NON-NLS-1$
                }
            }
        } else {
            whereClause = "Select"; //$NON-NLS-1$
            CachedRowSet rowSet;
            String matchName;

            for (int i = 0; i < size; i++) {
                rowSet = (CachedRowSet) rsList.get(i);
                tableName = rowSet.getTableName();
                if (tableName == null) {
                    throw new SQLException(Messages.getString("rowset.39")); //$NON-NLS-1$
                }

                matchIndex = matchColIndexs.get(0).intValue();
                if (i == 0) {
                    matchName = rowSet.getMetaData().getColumnName(matchIndex);
                    whereClause += " " + tableName + "." + matchName + ","; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }

                metaColumnCount = rowSet.getMetaData().getColumnCount();
                for (int j = 1; j < matchIndex; j++) {
                    matchName = rowSet.getMetaData().getColumnName(j);
                    whereClause += " " + tableName + "." + matchName; //$NON-NLS-1$ //$NON-NLS-2$

                    if (j != metaColumnCount - 1 || i != size - 1) {
                        whereClause += ","; //$NON-NLS-1$
                    } else {
                        whereClause += " "; //$NON-NLS-1$
                    }
                }

                for (int j = matchIndex + 1; j <= metaColumnCount; j++) {
                    matchName = rowSet.getMetaData().getColumnName(j);
                    whereClause += " " + tableName + "." + matchName; //$NON-NLS-1$ //$NON-NLS-2$

                    if (j != metaColumnCount || i != size - 1) {
                        whereClause += ","; //$NON-NLS-1$
                    } else {
                        whereClause += " "; //$NON-NLS-1$
                    }
                }
            }
            whereClause += "from "; //$NON-NLS-1$
            for (int i = 0; i < size; i++) {
                rowSet = (CachedRowSet) rsList.get(i);
                tableName = rowSet.getTableName();

                whereClause += tableName;
                if (i != size - 1) {
                    whereClause += ", "; //$NON-NLS-1$
                } else {
                    whereClause += " "; //$NON-NLS-1$
                }
            }

            whereClause += "where "; //$NON-NLS-1$
            CachedRowSet firstRowSet = (CachedRowSet) rsList.get(0);
            String firstTableName = firstRowSet.getTableName();
            String firstMatchName = firstRowSet.getMetaData().getColumnName(
                    matchColIndexs.get(0).intValue());
            for (int i = 1; i < size; i++) {
                rowSet = (CachedRowSet) rsList.get(i);
                tableName = rowSet.getTableName();
                matchIndex = matchColIndexs.get(i).intValue();
                matchName = rowSet.getMetaData().getColumnName(matchIndex);

                whereClause += firstTableName + "." + firstMatchName + " = "; //$NON-NLS-1$ //$NON-NLS-2$
                whereClause += tableName + "." + matchName; //$NON-NLS-1$
                if (i != size - 1) {
                    whereClause += " and "; //$NON-NLS-1$
                }
            }
        }

        return whereClause;
    }

    public void setJoinType(int joinType) throws SQLException {
        if (supportsJoinType(joinType)) {
            this.joinType = joinType;
        } else {
            // rowset.38=This type of join is not supported
            throw new SQLException(Messages.getString("rowset.38")); //$NON-NLS-1$
        }
    }

    public boolean supportsCrossJoin() {
        return supportsJoinType(CROSS_JOIN);
    }

    public boolean supportsFullJoin() {
        return supportsJoinType(FULL_JOIN);
    }

    public boolean supportsInnerJoin() {
        return supportsJoinType(INNER_JOIN);
    }

    public boolean supportsLeftOuterJoin() {
        return supportsJoinType(LEFT_OUTER_JOIN);
    }

    public boolean supportsRightOuterJoin() {
        return supportsJoinType(RIGHT_OUTER_JOIN);
    }

    private boolean supportsJoinType(int type) {
        if (type == INNER_JOIN) {
            return true;
        }
        return false;
    }

    public CachedRowSet toCachedRowSet() throws SQLException {
        if (rsList.size() == 0) {
            CachedRowSetImpl toCrset = new CachedRowSetImpl();
            toCrset.setRows(new ArrayList<CachedRow>(), 0);
            return toCrset;
        } else if (rsList.size() == 1) {
            CachedRowSet toCrset = ((CachedRowSet) rsList.get(0)).createCopy();
            toCrset.setMetaData(meta);
            return toCrset;
        } else {
            CachedRowSetImpl toCrset = new CachedRowSetImpl();
            toCrset.setRows(rows, meta.getColumnCount());
            toCrset.setMetaData(meta);
            toCrset.columnTypes = columnTypes;
            toCrset.setTypeMap(rsList.get(0).getTypeMap());
            if (rsList.get(0).getUrl() != null) {
                toCrset.setUrl(rsList.get(0).getUrl());
                toCrset.setUsername(rsList.get(0).getUsername());
                toCrset.setPassword(rsList.get(0).getPassword());
            } else if (rsList.get(0).getDataSourceName() != null) {
                toCrset.setDataSourceName(rsList.get(0).getDataSourceName());
            }
            return toCrset;
        }
    }

    @Override
    public void populate(ResultSet rs) throws SQLException {
        // do nothing
    }

    @Override
    public void populate(ResultSet rs, int startRow) throws SQLException {
        // do nothing
    }

    @Override
    public void execute() throws SQLException {
        if (rsList.size() == 0) {
            throw new SQLException();
        } else if (rsList.size() == 1) {
            try {
                copyFirstRs.execute();
                super.populate(copyFirstRs);
            } catch (SQLException e) {
                setRows(new ArrayList<CachedRow>(), getMetaData()
                        .getColumnCount());
                throw e;
            }
        } else {
            setRows(new ArrayList<CachedRow>(), getMetaData().getColumnCount());
            throw new SQLException();
        }
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        if (rsList.size() == 0) {
            throw new SQLException();
        } else if (rsList.size() == 1) {
            try {
                copyFirstRs.execute(connection);
                super.populate(copyFirstRs);
            } catch (SQLException e) {
                setRows(new ArrayList<CachedRow>(), getMetaData()
                        .getColumnCount());
                throw e;
            }
        } else {
            setRows(new ArrayList<CachedRow>(), getMetaData().getColumnCount());
            throw new SQLException();
        }
    }

    /**
     * Join the data with another CachedRowSet. It updates rows,
     * currentRowIndex, currentRow and columnCount. It doesn't update its
     * RowSetMetaData or any other status.
     * 
     * @throws SQLException
     * @throws CloneNotSupportedException
     */
    private void innerJoin() throws SQLException, CloneNotSupportedException {
        CachedRowSetImpl rowSetToAdd = (CachedRowSetImpl) rsList.get(rsList
                .size() - 1);

        // If it is the first RowSet added, then just copy the data of
        // rowSetToAdd.
        // Since here the RowSet has been added to rsList, so it is 2 to
        // compared with, not 1.
        if (rsList.size() < 2) {
            // Create a new arrayList to store the copied data.
            // Since the size will be the same with the rowSetToAdd, so
            // initialing the list with this size will improve performance.
            ArrayList<CachedRow> newRows = new ArrayList<CachedRow>(
                    rowSetToAdd.rows.size());

            // Make clones for all the CachedRows in rowSetToAdd, regardless of
            // whether it is deleted.
            CachedRow row;
            for (int i = 0; i < rowSetToAdd.rows.size(); i++) {
                row = rowSetToAdd.rows.get(i);
                if (!row.isDelete() || rowSetToAdd.getShowDeleted()) {
                    newRows.add(row.createClone());
                }
            }

            // Sets the rows and columnCount.
            setRows(newRows, rowSetToAdd.columnCount);

            // Set the currentRow and currentRowIndex.
            currentRowIndex = rowSetToAdd.currentRowIndex;
            if (currentRowIndex > 0 && currentRowIndex <= rows.size()) {
                currentRow = rows.get(currentRowIndex - 1);
            } else {
                currentRow = null;
            }

            // Inits other properties.
            originalResultSet = rowSetToAdd.originalResultSet;
            if (rowSetToAdd.conn != null) {
                conn = rowSetToAdd.conn;
            }
            setTypeMap(rowSetToAdd.getTypeMap());
            this.setTableName(rowSetToAdd.getTableName());
            setShowDeleted(rowSetToAdd.getShowDeleted());
        } else {
            // Get the match index of itself and the rowSet to added.
            int matchIndex = matchColIndexs.get(0).intValue();
            // Here we can sure rsList.size() > 1
            int matchIndexOfToAdd = matchColIndexs.get(rsList.size() - 1)
                    .intValue();

            // The comparator used to sort the rows of itself (When it can be
            // sorted), and to compare
            // the rows between these two rowSets.
            CachedRowComparator comparator;

            // If the rows can be sorted on the object of match index, call
            // sortJoinRows.
            // Otherwise call iterativeJoinRows.
            if (isSortable()) {
                comparator = new CachedRowComparator(matchIndex,
                        matchIndexOfToAdd, true);
                sortJoinRows(rowSetToAdd, matchIndex, matchIndexOfToAdd,
                        comparator);
            } else {
                comparator = new CachedRowComparator(matchIndex,
                        matchIndexOfToAdd, false);
                iterativeJoinRows(rowSetToAdd, matchIndex, matchIndexOfToAdd,
                        comparator);
            }

            // Set the cursor of rowSetToAdd to the last.
            rowSetToAdd.last();

            // Set the cursor of itself to beforeFirst.
            beforeFirst();
        }
    }

    private boolean isSortable() {
        return isSortable;
    }

    private void initSortable() {
        Class<?> clazz = columnTypes[matchColIndexs.get(0).intValue() - 1];
        Class[] classes = clazz.getInterfaces();
        isSortable = false;
        for (Class<?> c : classes) {
            if (c.equals(Comparable.class)) {
                isSortable = true;
                break;
            }
        }
    }

    private void iterativeJoinRows(CachedRowSetImpl rowSetToAdd,
            int matchColumnIndex, int matchColumnIndexOfToAdd,
            CachedRowComparator comparator) throws SQLException {
        // The row from itself.
        CachedRow row;
        // The row from rowSet to add.
        CachedRow rowToAdd;
        // The row will be created to join the row and rowToAdd.
        CachedRow newRow;

        /*
         * Create a new arrayList to store the copied data. Since the size will
         * surely less then the min of the two rowSets, so initialing the list
         * with half of the min will improve performance.
         */
        ArrayList<CachedRow> newRows = new ArrayList<CachedRow>(Math.min(rows
                .size(), rowSetToAdd.rows.size()) / 2);

        /*
         * Computes the column count of rowSetToAdd, the result rowSet, the
         * original rowSet.
         */
        int addedColumnCount = rowSetToAdd.getMetaData().getColumnCount();
        int resultColumnCount = this.getMetaData().getColumnCount();

        /*
         * Since only one matched index is supported, and these two matched
         * columns will merge to one column, so the original column count is
         * just the minus of result comlumn count and added column count plus 1.
         */
        int originalColumnCount = resultColumnCount + 1 - addedColumnCount;

        /*
         * Iterates two rowsets, compare rowNum1 * rowNum2 times. If match,
         * construct a new row, add it to the row list.
         */
        this.beforeFirst();
        while (this.next()) {
            row = this.getCurrentRow();
            // If the value is null, just jump to next row.
            // Since null won't match anything, even null.
            if (row.getObject(matchColumnIndex) != null) {
                rowSetToAdd.beforeFirst();
                while (rowSetToAdd.next()) {
                    rowToAdd = rowSetToAdd.getCurrentRow();
                    if (comparator.compare(row, rowToAdd) == 0) {
                        // It match, construct a new row, add it to list.
                        newRow = constructNewRow(row, rowToAdd,
                                matchColumnIndex, matchColumnIndexOfToAdd,
                                resultColumnCount, originalColumnCount);
                        newRows.add(newRow);
                    }
                }
            }
        }

        // Sets the rows and column count.
        setRows(newRows, resultColumnCount);
    }

    private void sortJoinRows(CachedRowSetImpl rowSetToAdd,
            int matchColumnIndex, int matchColumnIndexOfToAdd,
            CachedRowComparator comparator) throws SQLException {
        // The row from itself.
        CachedRow row;
        // The row from rowSet to add.
        CachedRow rowToAdd;
        // The row will be created to join the row and rowToAdd.
        CachedRow newRow;

        /*
         * Create a new arrayList to store the copied data. Since the size will
         * surely less then the min of the two rowSets, so initialing the list
         * with half of the min will improve performance.
         */
        ArrayList<CachedRow> newRows = new ArrayList<CachedRow>(Math.min(rows
                .size(), rowSetToAdd.rows.size()) / 2);

        /*
         * Computes the column count of rowSetToAdd, the result rowSet, the
         * original rowSet.
         */
        int addedColumnCount = rowSetToAdd.getMetaData().getColumnCount();
        int resultColumnCount = this.getMetaData().getColumnCount();

        /*
         * Since only one matched index is supported, and these two matched
         * columns will merge to one column, so the original column count is
         * just the minus of result comlumn count and added column count plus 1.
         */
        int originalColumnCount = resultColumnCount + 1 - addedColumnCount;

        /*
         * Sort the original rows. Set both the column to compared to the match
         * index of original rows, since the comprasion will happened inside the
         * original rows.
         */
        comparator.setFirstIndex(matchColumnIndex);
        comparator.setSecondIndex(matchColumnIndex);
        Collections.sort(rows, comparator);

        /*
         * Then comparator will be used to compare the object from two rowSets,
         * so set firstIndex of comparator to match index of itself, and second
         * index to match index of rowSet to add.
         */
        comparator.setFirstIndex(matchColumnIndex);
        comparator.setSecondIndex(matchColumnIndexOfToAdd);

        int position;
        /*
         * Iterates the rows of rowSetToAdd, find matched row in original rows
         * using binary search.(It has been sorted).
         */
        rowSetToAdd.beforeFirst();
        while (rowSetToAdd.next()) {
            rowToAdd = rowSetToAdd.getCurrentRow();

            /*
             * If the value is null, just jump to next row. Since null won't
             * match anything, even null.
             */
            if (rowToAdd.getObject(matchColumnIndexOfToAdd) == null) {
                continue;
            }

            // Find the position of the matched row in original rows.
            position = Collections.binarySearch(rows, rowToAdd, comparator);

            // Not found, jump to next row.
            if (position < 0) {
                continue;
            }

            row = rows.get(position);
            /*
             * If row is deleted and showDeleted is false, it will not be
             * joined.
             */
            if (getShowDeleted() || !row.isDelete()) {
                // Construct a new row, add it to list.
                newRow = constructNewRow(row, rowToAdd, matchColumnIndex,
                        matchColumnIndexOfToAdd, resultColumnCount,
                        originalColumnCount);
                newRows.add(newRow);

                /*
                 * Since there may be other rows also match, so we have to
                 * examine the before positions and after positions until they
                 * don't match. Remember, Collections.binarySearch does NOT
                 * guarantee which row will be found if multiple element are
                 * equal to the search value.
                 */
                for (int i = position - 1; i >= 0; i--) {
                    row = rows.get(i);
                    if (comparator.compare(row, rowToAdd) == 0) {
                        newRow = constructNewRow(row, rowToAdd,
                                matchColumnIndex, matchColumnIndexOfToAdd,
                                resultColumnCount, originalColumnCount);
                        newRows.add(newRow);
                    } else {
                        break;
                    }
                }

                for (int i = position + 1; i < rows.size(); i++) {
                    row = rows.get(i);
                    if (comparator.compare(row, rowToAdd) == 0) {
                        newRow = constructNewRow(row, rowToAdd,
                                matchColumnIndex, matchColumnIndexOfToAdd,
                                resultColumnCount, originalColumnCount);
                        newRows.add(newRow);
                    } else {
                        break;
                    }
                }
            }

        }

        // Sets the rows and column count.
        setRows(newRows, resultColumnCount);
    }

    /**
     * Construct a new CachedRow which will contain the data from both rows,
     * excluding matched column, which will only appear once in the result row..
     * 
     * @param row
     *            The first row to join.
     * @param rowToAdd
     *            The second row to join.
     * @param matchColumnIndex
     *            The match index of first row.
     * @param matchColumnIndexOfToAdd
     *            The match index of second row.
     * @param resultColumnCount
     *            The column count of the result row.
     * @param originalColumnCount
     *            The column count of original row.
     * @return The new created CachedRow.
     */
    private CachedRow constructNewRow(CachedRow row, CachedRow rowToAdd,
            int matchColumnIndex, int matchColumnIndexOfToAdd,
            int resultColumnCount, int originalColumnCount) {
        Object[] rowData;

        rowData = new Object[resultColumnCount];

        int i = 0;
        for (; i < matchColumnIndex; i++) {
            rowData[i] = row.getObject(i + 1);
        }

        for (; i < originalColumnCount; i++) {
            rowData[i] = row.getObject(i + 1);
        }

        int j = 1;
        for (; i < originalColumnCount + matchColumnIndexOfToAdd - 1; i++, j++) {
            rowData[i] = rowToAdd.getObject(j);
        }
        for (; i < resultColumnCount; i++, j++) {
            rowData[i] = rowToAdd.getObject(j + 1);
        }
        return new CachedRow(rowData);
    }

    private static class CachedRowComparator implements Comparator<CachedRow> {

        private int firstIndex;

        private int secondIndex;

        private boolean isComparable;

        public CachedRowComparator(int firstIndex, int secondIndex,
                boolean isComparable) {
            this.firstIndex = firstIndex;
            this.secondIndex = secondIndex;
            this.isComparable = isComparable;
        }

        public void setFirstIndex(int firstIndex) {
            this.firstIndex = firstIndex;
        }

        public void setComparable(boolean isComparable) {
            this.isComparable = isComparable;
        }

        public void setSecondIndex(int secondIndex) {
            this.secondIndex = secondIndex;
        }

        public int compare(CachedRow object1, CachedRow object2) {
            Object first = object1.getObject(firstIndex);
            Object second = object2.getObject(secondIndex);

            if (first == null && second == null) {
                return 0;
            }

            if (first == null && second != null) {
                return -1;
            }

            if (first != null && second == null) {
                return 1;
            }

            if (isComparable) {
                return ((Comparable<Object>) first).compareTo(second);
            }

            if (first.equals(second)) {
                return 0;
            }
            return -1;
        }
    }
}
