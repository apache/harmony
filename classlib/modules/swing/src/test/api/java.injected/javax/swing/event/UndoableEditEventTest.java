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

import javax.swing.JTextField;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import junit.framework.TestCase;

public class UndoableEditEventTest extends TestCase {
    private TestUndoableEditListener listener;

    private Object source;

    public UndoableEditEventTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        listener = new TestUndoableEditListener();
        source = new Object();
    }

    @Override
    protected void tearDown() throws Exception {
        listener = null;
        source = null;
    }

    public void testUndoableEditEvent() throws Exception {
        UndoableEditEvent event = new UndoableEditEvent(source, null);
        assertEquals(source, event.getSource());
    }

    public void testGetEdit() throws Exception {
        UndoableEdit edit = new AbstractUndoableEdit();
        UndoableEditEvent event = new UndoableEditEvent(source, edit);
        assertEquals(edit, event.getEdit());
    }

    public void testEventProperlyInitialized() throws Exception {
        UndoableEditEvent event = new UndoableEditEvent(new Object(),
                new AbstractUndoableEdit());
        assertNotNull(event.getSource());
        assertNotNull(event.getEdit());
    }

    public void testEventOccured() throws Exception {
        JTextField field = new JTextField();
        field.getDocument().addUndoableEditListener(listener);
        field.setText("new text");
        assertNotNull(listener.getEvent());
        assertNotNull(listener.getEvent().getSource());
        assertNotNull(listener.getEvent().getEdit());
    }

    private class TestUndoableEditListener implements UndoableEditListener {
        private UndoableEditEvent event;

        public void undoableEditHappened(final UndoableEditEvent e) {
            event = e;
        }

        public UndoableEditEvent getEvent() {
            return event;
        }
    }
}
