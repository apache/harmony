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

import javax.swing.BasicSwingTestCase;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class TableModelEventTest extends BasicSwingTestCase {
    private TableModelEvent event;

    public TableModelEventTest(final String name) {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception {
        event = null;
    }

    public void testTableModelEvent() throws Exception {
        TableModel model = new DefaultTableModel();
        event = new TableModelEvent(model);
        assertEquals(model, event.getSource());
        assertEquals(0, event.getFirstRow());
        assertEquals(Integer.MAX_VALUE, event.getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, event.getColumn());
        assertEquals(TableModelEvent.UPDATE, event.getType());
        event = new TableModelEvent(model, 3);
        assertEquals(model, event.getSource());
        assertEquals(3, event.getFirstRow());
        assertEquals(3, event.getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, event.getColumn());
        assertEquals(TableModelEvent.UPDATE, event.getType());
        event = new TableModelEvent(model, 6, 2);
        assertEquals(model, event.getSource());
        assertEquals(6, event.getFirstRow());
        assertEquals(2, event.getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, event.getColumn());
        assertEquals(TableModelEvent.UPDATE, event.getType());
        event = new TableModelEvent(model, 6, 2, 5);
        assertEquals(model, event.getSource());
        assertEquals(6, event.getFirstRow());
        assertEquals(2, event.getLastRow());
        assertEquals(5, event.getColumn());
        assertEquals(TableModelEvent.UPDATE, event.getType());
        event = new TableModelEvent(model, 6, 2, 5, TableModelEvent.INSERT);
        assertEquals(model, event.getSource());
        assertEquals(6, event.getFirstRow());
        assertEquals(2, event.getLastRow());
        assertEquals(5, event.getColumn());
        assertEquals(TableModelEvent.INSERT, event.getType());
    }
}
