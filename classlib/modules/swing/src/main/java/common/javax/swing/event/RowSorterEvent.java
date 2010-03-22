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

package javax.swing.event;

import java.util.EventObject;
import javax.swing.RowSorter;

public class RowSorterEvent extends EventObject {
    public static enum Type {
        SORT_ORDER_CHANGED, SORTED
    }

    private RowSorter source;
    private RowSorterEvent.Type eventType;
    private int[] previousRowIndexToModel;

    public RowSorterEvent(RowSorter source) {
        this(source, Type.SORT_ORDER_CHANGED, null);
    }

    public RowSorterEvent(RowSorter source, RowSorterEvent.Type type, int[] previousRowIndexToModel) {
        super(source);

        if (source == null) {
            throw new IllegalArgumentException("Source row sorter cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Row sorter event type cannot be null");
        }

        this.source = source;
        this.eventType = type;
        this.previousRowIndexToModel = previousRowIndexToModel;
    }

    public RowSorter getSource() {
        return source;
    }

    public RowSorterEvent.Type getType() {
        return eventType;
    }

    public int convertPreviousRowIndexToModel(int index) {
        if ((previousRowIndexToModel == null) || (index > previousRowIndexToModel.length - 1)) {
            return -1;
        }

        return previousRowIndexToModel[index];
    }

    public int getPreviousRowCount() {
        return previousRowIndexToModel.length;
    }
}
