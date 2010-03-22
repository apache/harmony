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
import javax.swing.table.DefaultTableColumnModel;

public class TableColumnModelEventTest extends BasicSwingTestCase {
    private TableColumnModelEvent event;

    public TableColumnModelEventTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        event = new TableColumnModelEvent(new DefaultTableColumnModel(), 5, 10);
    }

    @Override
    protected void tearDown() throws Exception {
        event = null;
    }

    public void testTableColumnModelEvent() throws Exception {
        assertTrue(event.getSource() instanceof DefaultTableColumnModel);
        assertEquals(5, event.getFromIndex());
        assertEquals(10, event.getToIndex());
        event = new TableColumnModelEvent(new DefaultTableColumnModel(), 10, 5);
        assertEquals(10, event.getFromIndex());
        assertEquals(5, event.getToIndex());
    }
}
