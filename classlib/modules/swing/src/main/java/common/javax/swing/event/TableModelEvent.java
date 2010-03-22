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

/**
 * @author Anton Avtamonov
 */
package javax.swing.event;

import java.util.EventObject;

import javax.swing.table.TableModel;

public class TableModelEvent extends EventObject {
    public static final int UPDATE = 0;
    public static final int INSERT = 1;
    public static final int DELETE = -1;
    public static final int HEADER_ROW = -1;
    public static final int ALL_COLUMNS = -1;

    protected int type;
    protected int firstRow;
    protected int lastRow;
    protected int column;

    public TableModelEvent(final TableModel source) {
        this(source, 0, Integer.MAX_VALUE, ALL_COLUMNS, UPDATE);
    }

    public TableModelEvent(final TableModel source, final int row) {
        this(source, row, row, ALL_COLUMNS, UPDATE);
    }

    public TableModelEvent(final TableModel source, final int firstRow, final int lastRow) {
        this(source, firstRow, lastRow, ALL_COLUMNS, UPDATE);
    }

    public TableModelEvent(final TableModel source, final int firstRow, final int lastRow, final int column) {
        this(source, firstRow, lastRow, column, UPDATE);
    }

    public TableModelEvent(final TableModel source, final int firstRow, final int lastRow, final int column, final int type) {
        super(source);
        this.firstRow = firstRow;
        this.lastRow = lastRow;
        this.column = column;
        this.type = type;
    }

    public int getFirstRow() {
        return firstRow;
    }

    public int getLastRow() {
        return lastRow;
    }

    public int getColumn() {
        return column;
    }

    public int getType() {
        return type;
    }
}