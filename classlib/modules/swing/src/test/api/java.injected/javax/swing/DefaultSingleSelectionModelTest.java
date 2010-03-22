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
package javax.swing;

import java.awt.event.KeyListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class DefaultSingleSelectionModelTest extends SwingTestCase {
    private DefaultSingleSelectionModel model;

    private TestChangeListener listener;

    public DefaultSingleSelectionModelTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        model = new DefaultSingleSelectionModel();
        listener = new TestChangeListener();
    }

    @Override
    protected void tearDown() throws Exception {
        model = null;
    }

    public void testAddRemoveGetChangeListener() throws Exception {
        assertEquals(0, model.getChangeListeners().length);
        model.addChangeListener(new TestChangeListener());
        model.addChangeListener(listener);
        model.addChangeListener(new TestChangeListener());
        assertEquals(3, model.getChangeListeners().length);
        model.removeChangeListener(listener);
        assertEquals(2, model.getChangeListeners().length);
    }

    public void testGetSetIsClearSelection() throws Exception {
        assertEquals(-1, model.getSelectedIndex());
        assertFalse(model.isSelected());
        TestChangeListener listener = new TestChangeListener();
        model.addChangeListener(listener);
        model.setSelectedIndex(4);
        assertEquals(4, model.getSelectedIndex());
        assertTrue(model.isSelected());
        assertNotNull(listener.getChangeEvent());
        listener.reset();
        model.clearSelection();
        assertEquals(-1, model.getSelectedIndex());
        assertFalse(model.isSelected());
        assertNotNull(listener.getChangeEvent());
    }

    public void testGetListeners() throws Exception {
        assertEquals(0, model.getListeners(ChangeListener.class).length);
        assertEquals(0, model.getListeners(KeyListener.class).length);
        model.addChangeListener(listener);
        assertEquals(1, model.getListeners(ChangeListener.class).length);
        assertEquals(0, model.getListeners(KeyListener.class).length);
    }

    public void testFireStateChanged() throws Exception {
        TestChangeListener listener2 = new TestChangeListener();
        model.addChangeListener(listener);
        model.addChangeListener(listener2);
        assertNull(model.changeEvent);
        model.fireStateChanged();
        assertNotNull(model.changeEvent);
        assertEquals(model, model.changeEvent.getSource());
        assertEquals(model.changeEvent, listener.getChangeEvent());
        assertEquals(model.changeEvent, listener2.getChangeEvent());
    }

    private class TestChangeListener implements ChangeListener {
        private ChangeEvent changeEvent;

        public void stateChanged(final ChangeEvent e) {
            changeEvent = e;
        }

        public ChangeEvent getChangeEvent() {
            return changeEvent;
        }

        public void reset() {
            changeEvent = null;
        }
    }
}
