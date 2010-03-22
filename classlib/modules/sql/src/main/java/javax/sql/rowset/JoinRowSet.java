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

import java.sql.SQLException;
import java.util.Collection;
import javax.sql.RowSet;

public interface JoinRowSet extends WebRowSet {
    int CROSS_JOIN = 0;

    int INNER_JOIN = 1;

    int LEFT_OUTER_JOIN = 2;

    int RIGHT_OUTER_JOIN = 3;

    int FULL_JOIN = 4;

    void addRowSet(Joinable rowset) throws SQLException;

    void addRowSet(RowSet rowset, int columnIdx) throws SQLException;

    void addRowSet(RowSet rowset, String columnName) throws SQLException;

    void addRowSet(RowSet[] rowset, int[] columnIdx) throws SQLException;

    void addRowSet(RowSet[] rowset, String[] columnName) throws SQLException;

    Collection<?> getRowSets() throws SQLException;

    String[] getRowSetNames() throws SQLException;

    CachedRowSet toCachedRowSet() throws SQLException;

    boolean supportsCrossJoin();

    boolean supportsInnerJoin();

    boolean supportsLeftOuterJoin();

    boolean supportsRightOuterJoin();

    boolean supportsFullJoin();

    void setJoinType(int joinType) throws SQLException;

    String getWhereClause() throws SQLException;

    int getJoinType() throws SQLException;
}
