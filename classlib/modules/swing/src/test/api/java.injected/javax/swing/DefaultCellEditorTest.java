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

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.BasicSwingTableTestCase;

public class DefaultCellEditorTest extends BasicSwingTableTestCase {
    private DefaultCellEditor editor;

    public DefaultCellEditorTest(final String name) {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception {
        editor = null;
    }

    public void testDefaultCellEditor() throws Exception {
        JTextField field = new JTextField();
        editor = new DefaultCellEditor(field);
        assertEquals(field, editor.editorComponent);
        assertNotNull(editor.delegate);
        assertEquals(2, editor.clickCountToStart);
        assertEquals("", editor.delegate.getCellEditorValue());
    }

    @SuppressWarnings("deprecation")
    public void testEditorDelegate_TextField() throws Exception {
        JTextField field = new JTextField();
        editor = new DefaultCellEditor(field);
        assertTrue(Arrays.asList(field.getActionListeners()).contains(editor.delegate));
        assertEquals(2, editor.getClickCountToStart());
        assertEquals(field.getText(), editor.delegate.getCellEditorValue());
        assertEquals(field.getText(), editor.getCellEditorValue());
        field.setText("text1");
        assertEquals("text1", editor.delegate.getCellEditorValue());
        assertEquals("text1", editor.getCellEditorValue());
        editor.delegate.setValue("text2");
        assertEquals("text2", editor.delegate.getCellEditorValue());
        assertEquals("text2", editor.getCellEditorValue());
        assertEquals(field.getText(), editor.getCellEditorValue());
        editor.delegate.setValue(new Integer(4));
        assertEquals("4", editor.delegate.getCellEditorValue());
        assertEquals("4", editor.getCellEditorValue());
        assertEquals(field.getText(), editor.getCellEditorValue());
        assertTrue(editor.delegate.isCellEditable(null));
        assertTrue(editor.isCellEditable(null));
        assertTrue(editor.delegate.isCellEditable(new MouseEvent(field, MouseEvent.BUTTON2, 0,
                0, 0, 0, 2, true)));
        assertFalse(editor.delegate.isCellEditable(new MouseEvent(field, MouseEvent.BUTTON2, 0,
                0, 0, 0, 1, true)));
        assertTrue(editor.isCellEditable(null));
        assertTrue(editor.delegate.isCellEditable(new KeyEvent(field, KeyEvent.KEY_RELEASED, 0,
                0, 0)));
        assertTrue(editor.isCellEditable(new KeyEvent(field, KeyEvent.KEY_TYPED, 0, 0, 0)));
        assertTrue(editor.delegate.shouldSelectCell(null));
        assertTrue(editor.shouldSelectCell(null));
        TestCellEditorListener listener = new TestCellEditorListener();
        editor.addCellEditorListener(listener);
        assertTrue(editor.delegate.startCellEditing(null));
        assertFalse(listener.isOccured());
        editor.delegate.setValue("any");
        assertTrue(editor.delegate.stopCellEditing());
        assertTrue(listener.isOccured(TestCellEditorListener.STOPPPED));
        assertEquals("any", editor.getCellEditorValue());
        listener.reset();
        editor.delegate.setValue("another");
        editor.delegate.cancelCellEditing();
        assertTrue(listener.isOccured(TestCellEditorListener.CANCELED));
        assertEquals("another", editor.getCellEditorValue());
        listener.reset();
        field.fireActionPerformed();
        assertTrue(listener.isOccured(TestCellEditorListener.STOPPPED));
    }

    public void testEditorDelegate_CheckBox() throws Exception {
        JCheckBox check = new JCheckBox();
        editor = new DefaultCellEditor(check);
        assertTrue(Arrays.asList(check.getActionListeners()).contains(editor.delegate));
        assertEquals(1, editor.getClickCountToStart());
        assertEquals(Boolean.FALSE, editor.delegate.getCellEditorValue());
        assertEquals(Boolean.FALSE, editor.getCellEditorValue());
        check.setSelected(true);
        assertEquals(Boolean.TRUE, editor.delegate.getCellEditorValue());
        assertEquals(Boolean.TRUE, editor.getCellEditorValue());
        editor.delegate.setValue("text2");
        assertEquals(Boolean.FALSE, editor.delegate.getCellEditorValue());
        editor.delegate.setValue(Boolean.TRUE);
        assertEquals(Boolean.TRUE, editor.delegate.getCellEditorValue());
        assertEquals(Boolean.TRUE, editor.getCellEditorValue());
        assertTrue(check.isSelected());
        assertTrue(editor.delegate.isCellEditable(null));
        assertTrue(editor.isCellEditable(null));
        assertTrue(editor.delegate.isCellEditable(new MouseEvent(check, MouseEvent.BUTTON2, 0,
                0, 0, 0, 1, true)));
        assertFalse(editor.delegate.isCellEditable(new MouseEvent(check, MouseEvent.BUTTON2, 0,
                0, 0, 0, 0, true)));
        assertTrue(editor.delegate.shouldSelectCell(null));
        assertTrue(editor.shouldSelectCell(null));
        TestCellEditorListener listener = new TestCellEditorListener();
        editor.addCellEditorListener(listener);
        assertTrue(editor.delegate.startCellEditing(null));
        assertFalse(listener.isOccured());
        editor.delegate.setValue(Boolean.TRUE);
        assertTrue(editor.delegate.stopCellEditing());
        assertTrue(listener.isOccured(TestCellEditorListener.STOPPPED));
        assertEquals(Boolean.TRUE, editor.getCellEditorValue());
        listener.reset();
        editor.delegate.setValue("any");
        editor.delegate.cancelCellEditing();
        assertTrue(listener.isOccured(TestCellEditorListener.CANCELED));
        assertEquals(Boolean.FALSE, editor.getCellEditorValue());
        listener.reset();
        check.fireActionPerformed(new ActionEvent(this, 0, "cmd"));
        assertTrue(listener.isOccured(TestCellEditorListener.STOPPPED));
    }

    public void testEditorDelegate_ComboBox() throws Exception {
        JComboBox cb = new JComboBox();
        editor = new DefaultCellEditor(cb);
        assertTrue(Arrays.asList(cb.getActionListeners()).contains(editor.delegate));
        assertFalse(Arrays.asList(cb.getItemListeners()).contains(editor.delegate));
        assertEquals(1, editor.getClickCountToStart());
        assertNull(editor.delegate.getCellEditorValue());
        assertTrue(editor.delegate.isCellEditable(null));
        assertTrue(editor.delegate.shouldSelectCell(null));
        cb.getModel().setSelectedItem("selected");
        assertEquals("selected", cb.getSelectedItem());
        assertEquals("selected", editor.delegate.getCellEditorValue());
        assertEquals("selected", editor.getCellEditorValue());
        editor.delegate.setValue("any");
        assertEquals("selected", editor.delegate.getCellEditorValue());
        ((DefaultComboBoxModel) cb.getModel()).addElement("elem1");
        ((DefaultComboBoxModel) cb.getModel()).addElement("elem2");
        assertEquals("selected", editor.delegate.getCellEditorValue());
        editor.delegate.setValue("elem1");
        assertEquals("elem1", editor.delegate.getCellEditorValue());
        assertEquals("elem1", editor.getCellEditorValue());
        assertTrue(editor.delegate.isCellEditable(null));
        assertTrue(editor.isCellEditable(null));
        assertTrue(editor.delegate.isCellEditable(new MouseEvent(cb, MouseEvent.BUTTON2, 0, 0,
                0, 0, 1, true)));
        assertFalse(editor.delegate.isCellEditable(new MouseEvent(cb, MouseEvent.BUTTON2, 0, 0,
                0, 0, 0, true)));
        assertTrue(editor.delegate.shouldSelectCell(null));
        assertTrue(editor.shouldSelectCell(null));
        TestCellEditorListener listener = new TestCellEditorListener();
        editor.addCellEditorListener(listener);
        assertTrue(editor.delegate.startCellEditing(null));
        assertFalse(listener.isOccured());
        editor.delegate.setValue("elem2");
        assertTrue(editor.delegate.stopCellEditing());
        assertTrue(listener.isOccured(TestCellEditorListener.STOPPPED));
        assertEquals("elem2", editor.getCellEditorValue());
        listener.reset();
        editor.delegate.setValue("elem1");
        editor.delegate.cancelCellEditing();
        assertTrue(listener.isOccured(TestCellEditorListener.CANCELED));
        assertEquals("elem1", editor.getCellEditorValue());
        listener.reset();
        cb.fireActionEvent();
        assertTrue(listener.isOccured(TestCellEditorListener.STOPPPED));
        listener.reset();
        cb.fireItemStateChanged(new ItemEvent(cb, 0, "elem2", 0));
        assertFalse(listener.isOccured());
    }

    public void testGetComponent() throws Exception {
        JComboBox cb = new JComboBox();
        editor = new DefaultCellEditor(cb);
        assertEquals(cb, editor.getComponent());
    }

    public void testGetSetClickCountToStart() throws Exception {
        editor = new DefaultCellEditor(new JTextField());
        assertEquals(2, editor.getClickCountToStart());
        editor = new DefaultCellEditor(new JCheckBox());
        assertEquals(1, editor.getClickCountToStart());
        editor = new DefaultCellEditor(new JComboBox());
        assertEquals(1, editor.getClickCountToStart());
        editor.setClickCountToStart(-5);
        assertEquals(-5, editor.getClickCountToStart());
        editor.setClickCountToStart(10);
        assertEquals(10, editor.getClickCountToStart());
    }

    public void testGetTreeCellEditorComponent() throws Exception {
        editor = new DefaultCellEditor(new JTextField());
        assertEquals(editor.getComponent(), editor.getTreeCellEditorComponent(new JTree(),
                "any", false, false, false, 0));
        assertEquals("any", editor.getCellEditorValue());
    }

    public void testGetTableCellEditorComponent() throws Exception {
        editor = new DefaultCellEditor(new JTextField());
        assertEquals(editor.getComponent(), editor.getTableCellEditorComponent(new JTable(),
                "any", false, 0, 0));
        assertEquals("any", editor.getCellEditorValue());
    }

    private class TestCellEditorListener implements CellEditorListener {
        public static final int CANCELED = 1;

        public static final int STOPPPED = 2;

        private ChangeEvent event;

        private int eventType = -1;

        public void editingCanceled(final ChangeEvent e) {
            event = e;
            eventType = CANCELED;
        }

        public void editingStopped(ChangeEvent e) {
            event = e;
            eventType = STOPPPED;
        }

        public void reset() {
            event = null;
            eventType = -1;
        }

        public boolean isOccured(final int expectedType) {
            return isOccured() && eventType == expectedType;
        }

        public boolean isOccured() {
            return event != null;
        }
    }
}
