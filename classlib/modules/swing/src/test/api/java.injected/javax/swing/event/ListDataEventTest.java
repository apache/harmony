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

public class ListDataEventTest extends SwingTestCase {
    private ListDataEvent event;

    public ListDataEventTest(final String name) {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception {
        event = null;
    }

    public void testListDataEvent() throws Exception {
        Object source = new Object();
        event = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, 0, 0);
        assertEquals(source, event.getSource());
    }

    public void testGetIndex0() throws Exception {
        event = new ListDataEvent(new Object(), ListDataEvent.CONTENTS_CHANGED, 10, 0);
        assertEquals(0, event.getIndex0());
    }

    public void testGetIndex1() throws Exception {
        event = new ListDataEvent(new Object(), ListDataEvent.CONTENTS_CHANGED, 10, 5);
        assertEquals(10, event.getIndex1());
    }

    public void testToString() throws Exception {
        event = new ListDataEvent(new Object(), ListDataEvent.INTERVAL_ADDED, 1, 2);
        assertTrue(event.toString().indexOf("ListDataEvent") > 0);
        assertTrue(event.toString().indexOf("type=" + ListDataEvent.INTERVAL_ADDED) > 0);
        assertTrue(event.toString().indexOf("index0=1") > 0);
        assertTrue(event.toString().indexOf("index1=2") > 0);
    }
}
