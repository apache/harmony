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

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.BitSet;

import org.apache.harmony.sql.internal.nls.Messages;

public class CachedRow implements Cloneable, Serializable {

    private static final long serialVersionUID = 5131958045838461662L;

    private Object[] columnData;

    private Object[] originalColumnData;

    private BitSet mask;

    private boolean isDelete;

    private boolean isInsert;

    private boolean isUpdate;

    private boolean nonUpdateable = false;

    private SQLWarning sqlWarning = null;

    public SQLWarning getSqlWarning() {
        return sqlWarning;
    }

    public void setSqlWarning(SQLWarning sqlWarning) {
        this.sqlWarning = sqlWarning;
    }

    public CachedRow(Object[] columnData) {
        this.columnData = columnData.clone();
        originalColumnData = columnData.clone();
        mask = new BitSet(columnData.length);
    }

    public boolean getUpdateMask(int i) {
        return mask.get(i);
    }

    public void setUpdateMask(int i) {
        mask.set(i);
    }

    public void setNonUpdateable() {
        nonUpdateable = true;
    }

    public boolean getNonUpdateable() {
        return nonUpdateable;
    }

    public void setDelete() {
        isDelete = true;
        isUpdate = false;
        isInsert = false;
    }

    public void undoDelete() {
        isDelete = false;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public void setInsert() {
        isInsert = true;
        isUpdate = false;
        isDelete = false;
    }

    public boolean isInsert() {
        return isInsert;
    }

    public void setUpdate() {
        isUpdate = true;
        isInsert = false;
        isDelete = false;
    }

    public void undoUpdate() {
        isUpdate = false;
        mask.flip(0, columnData.length);
        columnData = originalColumnData.clone();
    }

    public boolean isUpdate() {
        return isUpdate;
    }

    public void updateObject(int columnIndex, Object x) throws SQLException {
        if (nonUpdateable) {
            // rowset.21=Not Updateable of the CurrentRow
            throw new SQLException(Messages.getString("rowset.21")); //$NON-NLS-1$
        }

        columnData[columnIndex - 1] = x;
        setUpdateMask(columnIndex - 1);
    }

    public CachedRow getOriginal() {
        return new CachedRow(originalColumnData);
    }

    public void setOriginal() {
        isUpdate = false;
        isDelete = false;
        isInsert = false;
        mask.flip(0, columnData.length);
        originalColumnData = columnData.clone();
    }

    public void restoreOriginal() {
        isUpdate = false;
        isDelete = false;
        isInsert = false;
        mask.flip(0, columnData.length);
        columnData = originalColumnData.clone();
    }

    public Object getObject(int columnIndex) {
        return columnData[columnIndex - 1];
    }

    // deep clone
    public CachedRow createClone() throws CloneNotSupportedException {
        CachedRow cr = (CachedRow) super.clone();

        Object[] cd = new Object[columnData.length];
        for (int i = 0; i < columnData.length; i++) {
            cd[i] = columnData[i];
        }
        cr.columnData = cd;
        cr.isInsert = isInsert;
        cr.isDelete = isDelete;
        cr.isUpdate = isUpdate;
        cr.mask = (BitSet) mask.clone();
        cr.nonUpdateable = nonUpdateable;
        // cr.originalColumnData
        return cr;
    }
}
