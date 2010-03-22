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

import javax.sql.rowset.FilteredRowSet;
import javax.sql.rowset.Predicate;
import javax.sql.rowset.spi.SyncFactoryException;

import org.apache.harmony.sql.internal.nls.Messages;

public class FilteredRowSetImpl extends WebRowSetImpl implements FilteredRowSet {

    private static final long serialVersionUID = -6532792430142890537L;

    private Predicate predicate;

    public FilteredRowSetImpl() throws SyncFactoryException {
        super();
    }

    public Predicate getFilter() {
        return predicate;
    }

    public void setFilter(Predicate p) throws SQLException {
        this.predicate = p;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        if (predicate == null) {
            return super.absolute(row);
        }

        beforeFirst();
        if (row > 0) {
            if (row > size()) {
                afterLast();
                return false;
            }
            int count = 0;
            for (int i = 0; i < size(); i++) {
                if (next()) {
                    count++;
                    if (count == row) {
                        return true;
                    }
                }
            }
        } else if (row < 0) {
            row = Math.abs(row);
            if (row > size()) {
                return false;
            }
            afterLast();
            int count = 0;
            for (int i = 0; i < size(); i++) {
                if (previous()) {
                    count++;
                    if (count == row) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean relative(int moveRows) throws SQLException {
        if (predicate == null) {
            return super.relative(moveRows);
        }

        if (moveRows > 0) {
            int count = 0;
            int maxCount = size() - getRow();
            for (int i = 0; i < maxCount; i++) {
                if (next()) {
                    count++;
                    if (count == moveRows) {
                        return true;
                    }
                }
            }
        } else if (moveRows < 0) {
            int count = 0;
            int maxCount = getRow();
            for (int i = 0; i < maxCount; i++) {
                if (previous()) {
                    count++;
                    if (count == Math.abs(moveRows)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean next() throws SQLException {
        if (predicate == null) {
            return super.next();
        }
        while (super.next()) {
            if (predicate.evaluate(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean previous() throws SQLException {
        if (predicate == null) {
            return super.previous();
        }
        while (super.previous()) {
            if (predicate.evaluate(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean first() throws SQLException {
        if (predicate == null) {
            return super.first();
        }
        beforeFirst();
        for (int i = 0; i < size(); i++) {
            if (next()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean last() throws SQLException {
        if (predicate == null) {
            return super.last();
        }
        afterLast();
        for (int i = 0; i < size(); i++) {
            if (previous()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void insertRow() throws SQLException {
        if (predicate != null) {
            CachedRow insertRow = getInsertRow();
            if (insertRow != null) {
                for (int i = 0; i < getMetaData().getColumnCount(); i++) {
                    if (insertRow.getUpdateMask(i)
                            && !predicate.evaluate(insertRow.getObject(i + 1),
                                    i + 1)) {
                        // rowset.29=Insert failed
                        throw new SQLException(Messages.getString("rowset.29"));
                    }
                }
            }
        }
        super.insertRow();
    }

    @Override
    protected void initInsertRow(int columnIndex, Object value)
            throws SQLException {
        super.initInsertRow(columnIndex, value);
        if (predicate == null) {
            return;
        }
        if (isCursorOnInsert() && !predicate.evaluate(value, columnIndex)) {
            getInsertRow().updateObject(columnIndex, value);
            // rowset.28=The given value does not lie between the filter
            // criterion
            throw new SQLException(Messages.getString("rowset.28"));
        }
    }
}
