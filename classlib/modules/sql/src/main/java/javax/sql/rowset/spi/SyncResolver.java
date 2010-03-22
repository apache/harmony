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

package javax.sql.rowset.spi;

import java.sql.SQLException;
import javax.sql.RowSet;

public interface SyncResolver extends RowSet {
    int UPDATE_ROW_CONFLICT = 0;

    int DELETE_ROW_CONFLICT = 1;

    int INSERT_ROW_CONFLICT = 2;

    int NO_ROW_CONFLICT = 3;

    int getStatus();

    Object getConflictValue(int index) throws SQLException;

    Object getConflictValue(String columnName) throws SQLException;

    void setResolvedValue(int index, Object obj) throws SQLException;

    void setResolvedValue(String columnName, Object obj) throws SQLException;

    boolean nextConflict() throws SQLException;

    boolean previousConflict() throws SQLException;
}
