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
 * @author Evgeniya G. Maenkova
 */
package javax.swing;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import javax.swing.plaf.UIResource;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NavigationFilter;
import javax.swing.text.TextAction;

public class JFormattedTextField_AbstractFormatterTest extends SwingTestCase {
    static class AbstractFormatter extends JFormattedTextField.AbstractFormatter implements
            Cloneable {
        private static final long serialVersionUID = 1L;

        Action[] actions;

        NavigationFilter navigationFilter;

        DocumentFilter documentFilter;

        @Override
        protected Action[] getActions() {
            return actions == null ? super.getActions() : actions;
        }

        @Override
        protected DocumentFilter getDocumentFilter() {
            return documentFilter == null ? super.getDocumentFilter() : documentFilter;
        }

        @Override
        protected NavigationFilter getNavigationFilter() {
            return navigationFilter == null ? super.getNavigationFilter() : navigationFilter;
        }

        final void setActions(final Action[] newActions) {
            actions = newActions;
        }

        final void setDocumentFilter(final DocumentFilter filter) {
            documentFilter = filter;
        }

        final void setNavigationFilter(final NavigationFilter filter) {
            navigationFilter = filter;
        }

        @Override
        public Object stringToValue(final String string) throws ParseException {
            return null;
        }

        @Override
        public String valueToString(final Object value) throws ParseException {
            return null;
        }
    }

    class TextActionImpl extends TextAction {
        private static final long serialVersionUID = 1L;

        TextActionImpl(final String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent e) {
        }
    }

    AbstractFormatter formatter;

    JFormattedTextField tf;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        formatter = new AbstractFormatter();
        tf = new JFormattedTextField();
    }

    class PropertyChangeListenerImpl implements PropertyChangeListener {
        String name;

        Object oldValue;

        Object newValue;

        String interestingPropertyName;

        public void propertyChange(final PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (propertyName.equals(interestingPropertyName)) {
                name = e.getPropertyName();
                oldValue = e.getOldValue();
                newValue = e.getNewValue();
            }
        }

        final void setInterestingPropertyName(final String propertyName) {
            interestingPropertyName = propertyName;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testClone() {
        tf.setFormatter(formatter);
        Object clone = null;
        try {
            clone = formatter.clone();
        } catch (CloneNotSupportedException e) {
            assertTrue("UnexpectedException: " + e.getMessage(), false);
        }
        assertTrue(clone instanceof JFormattedTextField.AbstractFormatter);
        assertNull(((JFormattedTextField.AbstractFormatter) clone).getFormattedTextField());
    }

    public void testGetActions() {
        assertNull(formatter.getActions());
    }

    final void printActionMap(final ActionMap actionMap) {
        if (actionMap == null) {
            return;
        }
        for (int i = 0; i < actionMap.size(); i++) {
            Object key = actionMap.keys()[i];
            System.out.println(i + " " + key + " " + actionMap.get(key));
        }
    }

    private void checkActionMap(final ActionMap map, final Action[] actions) {
        if (actions == null) {
            assertEquals(0, map.size());
            return;
        }
        assertEquals(actions.length, map.size());
        Object[] keys = map.keys();
        for (int i = 0; i < keys.length; i++) {
            String name = (String) keys[i];
            Action action = map.get(name);
            boolean contains = false;
            for (int j = 0; j < actions.length; j++) {
                Action a = actions[j];
                if (a.getValue(Action.NAME).equals(name) && action == a) {
                    contains = true;
                    break;
                }
            }
            assertTrue(contains);
        }
    }

    public void testInstallUninstall_Filters() {
        NavigationFilter navFilter = new NavigationFilter();
        formatter.setNavigationFilter(navFilter);
        DocumentFilter docFilter = new DocumentFilter();
        formatter.setDocumentFilter(docFilter);
        AbstractDocument doc = (AbstractDocument) tf.getDocument();
        assertNull(tf.getNavigationFilter());
        assertNull(doc.getDocumentFilter());
        formatter.install(tf);
        assertEquals(navFilter, tf.getNavigationFilter());
        assertEquals(docFilter, doc.getDocumentFilter());
        formatter.uninstall();
        assertNull(tf.getNavigationFilter());
        assertNull(doc.getDocumentFilter());
        formatter.install(tf);
        assertEquals(navFilter, tf.getNavigationFilter());
        assertEquals(docFilter, doc.getDocumentFilter());
        formatter.install(null);
        assertNull(tf.getNavigationFilter());
        assertNull(doc.getDocumentFilter());
    }

    public void testInstallUninstall_Actions() {
        Action[] actions = new Action[] { new TextActionImpl("1"), new TextActionImpl("2") };
        formatter.setActions(actions);
        AbstractDocument doc = (AbstractDocument) tf.getDocument();
        ActionMap map1 = tf.getActionMap();
        ActionMap map2 = map1.getParent(); //keymap
        ActionMap map3 = map2.getParent(); //uiActionMap
        assertTrue(map3 instanceof UIResource);
        assertNull(tf.getNavigationFilter());
        assertNull(doc.getDocumentFilter());
        formatter.install(tf);
        ActionMap _map1 = tf.getActionMap();
        ActionMap _map2 = _map1.getParent(); //keymap
        ActionMap _map3 = _map2.getParent(); //formatter
        ActionMap _map4 = _map3.getParent();
        assertEquals(map1, _map1);
        assertEquals(map2, _map2);
        assertEquals(map3, _map4);
        checkActionMap(_map3, actions);
        //TODO: Decide if uninstall() & install(null) should reset remove actions
        //installed by formatter.
        formatter.install(null);
        _map1 = tf.getActionMap();
        _map2 = _map1.getParent();
        _map3 = _map2.getParent();
        _map4 = _map3.getParent();
        assertEquals(map1, _map1);
        assertEquals(map2, _map2);
        if (isHarmony()) {
            assertEquals(map3, _map3);
        } else {
            assertEquals(map3, _map4);
            checkActionMap(_map3, null);
        }
        formatter.install(tf);
        _map1 = tf.getActionMap();
        _map2 = _map1.getParent();
        _map3 = _map2.getParent();
        _map4 = _map3.getParent();
        assertEquals(map1, _map1);
        assertEquals(map2, _map2);
        assertEquals(map3, _map4);
        checkActionMap(_map3, actions);
        formatter.uninstall();
        _map1 = tf.getActionMap();
        _map2 = _map1.getParent();
        _map3 = _map2.getParent();
        _map4 = _map3.getParent();
        assertEquals(map1, _map1);
        assertEquals(map2, _map2);
        if (isHarmony()) {
            assertEquals(map3, _map3);
        } else {
            assertEquals(map3, _map4);
            checkActionMap(_map3, null);
        }
    }

    public void testSetEditValid() {
        tf.setFormatter(formatter);
        PropertyChangeListenerImpl listener = new PropertyChangeListenerImpl();
        listener.setInterestingPropertyName("editValid");
        tf.addPropertyChangeListener(listener);
        formatter.setEditValid(false);
        formatter.setEditValid(true);
        assertEquals(Boolean.TRUE, listener.newValue);
        assertEquals(Boolean.FALSE, listener.oldValue);
    }

    public void testGetNavigationFilter() {
        assertNull(formatter.getNavigationFilter());
    }

    public void testGetDocumentFilter() {
        assertNull(formatter.getDocumentFilter());
    }

    public void testGetFormattedTextField() {
        assertNull(formatter.getFormattedTextField());
        formatter.install(tf);
        assertEquals(tf, formatter.getFormattedTextField());
        formatter.uninstall();
        //Perhaps, uninstall should reset getFormattedTextField.
        assertEquals(tf, formatter.getFormattedTextField());
        formatter.install(null);
        assertNull(formatter.getFormattedTextField());
    }
}
