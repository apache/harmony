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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Collection;
import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncProviderException;

public interface CachedRowSet extends Joinable, ResultSet, RowSet {
    boolean COMMIT_ON_ACCEPT_CHANGES = true;

    void populate(ResultSet data) throws SQLException;

    void execute(Connection conn) throws SQLException;

    void acceptChanges() throws SyncProviderException;

    void acceptChanges(Connection con) throws SyncProviderException;

    void restoreOriginal() throws SQLException;

    void release() throws SQLException;

    void undoDelete() throws SQLException;

    void undoInsert() throws SQLException;

    void undoUpdate() throws SQLException;

    boolean columnUpdated(int idx) throws SQLException;

    boolean columnUpdated(String columnName) throws SQLException;

    Collection<?> toCollection() throws SQLException;

    Collection<?> toCollection(int column) throws SQLException;

    Collection<?> toCollection(String column) throws SQLException;

    SyncProvider getSyncProvider() throws SQLException;

    void setSyncProvider(String provider) throws SQLException;

    int size();

    void setMetaData(RowSetMetaData md) throws SQLException;

    ResultSet getOriginal() throws SQLException;

    ResultSet getOriginalRow() throws SQLException;

    void setOriginalRow() throws SQLException;

    String getTableName() throws SQLException;

    void setTableName(String tabName) throws SQLException;

    int[] getKeyColumns() throws SQLException;

    void setKeyColumns(int[] keys) throws SQLException;

    RowSet createShared() throws SQLException;

    CachedRowSet createCopy() throws SQLException;

    CachedRowSet createCopySchema() throws SQLException;

    CachedRowSet createCopyNoConstraints() throws SQLException;

    RowSetWarning getRowSetWarnings() throws SQLException;

    boolean getShowDeleted() throws SQLException;

    void setShowDeleted(boolean b) throws SQLException;

    void commit() throws SQLException;

    void rollback() throws SQLException;

    void rollback(Savepoint s) throws SQLException;

    void rowSetPopulated(RowSetEvent event, int numRows) throws SQLException;

    void populate(ResultSet rs, int startRow) throws SQLException;

    void setPageSize(int size) throws SQLException;

    int getPageSize();

    boolean nextPage() throws SQLException;

    boolean previousPage() throws SQLException;
}
