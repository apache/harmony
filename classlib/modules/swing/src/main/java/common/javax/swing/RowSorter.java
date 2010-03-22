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

package javax.swing;

import java.util.List;
import java.util.Vector;

import javax.swing.SortOrder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;

public abstract class RowSorter<M> {

    private Vector<RowSorterListener> listeners = new Vector<RowSorterListener>();

    public static class SortKey {
        private int column;
        private SortOrder sortOrder;

        public SortKey(int column, SortOrder sortOrder) {
            if (sortOrder == null) {
                throw new IllegalArgumentException("Sort order cannot be null");
            }

            this.column = column;
            this.sortOrder = sortOrder;
        }

        public final int getColumn() {
            return column;
        }

        public final SortOrder getSortOrder() {
            return sortOrder;
        }

        public int hashCode() {
            return column + sortOrder.hashCode();
        }

        public boolean equals(Object o) {
            if ((o != null) && (o instanceof SortKey)) {
                SortKey sortKey = (SortKey)o;
                if ((column == sortKey.getColumn()) && (sortOrder.equals(sortKey.getSortOrder()))) {
                    return true;
                }
            }            
            return false;
        }
    }

    public RowSorter() {
        super();
    }

    public abstract M getModel();
    public abstract void toggleSortOrder(int column);
    public abstract int convertRowIndexToModel(int index);
    public abstract int convertRowIndexToView(int index);
    public abstract void setSortKeys(List<? extends RowSorter.SortKey> keys);
    public abstract List<? extends RowSorter.SortKey> getSortKeys();
    public abstract int getViewRowCount();
    public abstract int getModelRowCount();
    public abstract void modelStructureChanged();
    public abstract void allRowsChanged();
    public abstract void rowsInserted(int firstRow, int endRow);
    public abstract void rowsDeleted(int firstRow, int endRow);
    public abstract void rowsUpdated(int firstRow, int endRow);
    public abstract void rowsUpdated(int firstRow, int endRow, int column);

    public void addRowSorterListener(RowSorterListener l) {
        if (l == null) return;
        listeners.add(l);
    }

    public void removeRowSorterListener(RowSorterListener l) {
        if (l == null) return;
        listeners.remove(l);
    }

    protected void fireSortOrderChanged() {
        RowSorterEvent event = new RowSorterEvent(this);

        RowSorterListener[] listenerArray = (RowSorterListener[]) listeners.toArray();
        for (int i=0; i<listenerArray.length; i++) {
            listenerArray[i].sorterChanged(event);
        }        
    }

    protected void fireRowSorterChanged(int[] lastRowIndexToModel) {
        RowSorterEvent event = new RowSorterEvent(this, RowSorterEvent.Type.SORTED, lastRowIndexToModel);

        RowSorterListener[] listenerArray = (RowSorterListener[]) listeners.toArray();
        for (int i=0; i<listenerArray.length; i++) {
            listenerArray[i].sorterChanged(event);
        }   
    }
}
