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

import javax.swing.SwingTestCase;

public class ListSelectionEventTest extends SwingTestCase {
    private ListSelectionEvent event;

    public ListSelectionEventTest(final String name) {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception {
        event = null;
    }

    public void testListSelectionEvent() throws Exception {
        Object source = new Object();
        event = new ListSelectionEvent(source, 50, 10, false);
        assertEquals(source, event.getSource());
    }

    public void testGetFirstIndex() throws Exception {
        event = new ListSelectionEvent(new Object(), 5, 10, false);
        assertEquals(5, event.getFirstIndex());
    }

    public void testGetLastIndex() throws Exception {
        event = new ListSelectionEvent(new Object(), 5, 10, false);
        assertEquals(10, event.getLastIndex());
    }

    public void testGetValueIsAdjusting() throws Exception {
        event = new ListSelectionEvent(new Object(), 5, 10, false);
        assertFalse(event.getValueIsAdjusting());
        event = new ListSelectionEvent(new Object(), 5, 10, true);
        assertTrue(event.getValueIsAdjusting());
    }

    public void testToString() throws Exception {
        event = new ListSelectionEvent(new Object(), 5, 10, true);
        String stringRepresentation = event.toString();
        assertNotNull(stringRepresentation);
        assertTrue(stringRepresentation
                .indexOf("firstIndex= 5 lastIndex= 10 isAdjusting= true") > 0);
    }
}
