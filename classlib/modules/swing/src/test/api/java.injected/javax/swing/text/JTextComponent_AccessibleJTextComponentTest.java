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
package javax.swing.text;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Vector;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingTestCase;
import javax.swing.text.JTextComponentTest.JTextComp;
import javax.swing.text.JTextComponentTest.SimplePropertyChangeListener;

public class JTextComponent_AccessibleJTextComponentTest extends SwingTestCase {
    JFrame jf;

    JTextComponent jtc;

    SimplePropertyChangeListener pChListener;

    JTextComponent.AccessibleJTextComponent accessible;

    boolean bWasException;

    String str;

    AccessiblePropertyChangeListener listener;

    AbstractDocument doc;

    JTextComp jtComp;

    AbstractDocument docXXX;

    JTextField jep;

    Rectangle rect;

    Object obj_old[] = new Object[] { null };

    Object obj_new[] = new Object[1];

    String s[] = new String[] { AccessibleContext.ACCESSIBLE_TEXT_PROPERTY };

    boolean was[] = new boolean[] { true };

    String text;

    class AccessiblePropertyChangeListener implements PropertyChangeListener {
        Vector<String> PropertyNames = new Vector<String>();

        Object oldValues[] = new Object[5];

        Object newValues[] = new Object[5];

        boolean wasPropertyChange[] = new boolean[5];

        int index = 0;

        public void InterestProperty(final String s) {
            PropertyNames.add(s);
            wasPropertyChange[index++] = false;
        }

        public void propertyChange(final PropertyChangeEvent e) {
            for (int i = 0; i < PropertyNames.size(); i++) {
                if (e.getPropertyName() == PropertyNames.get(i)) {
                    oldValues[i] = e.getOldValue();
                    newValues[i] = e.getNewValue();
                    wasPropertyChange[i] = true;
                }
            }
        }

        public void reset() {
            for (int i = 0; i < index; i++) {
                oldValues[i] = null;
                newValues[i] = null;
                wasPropertyChange[i] = false;
            }
            index = 0;
            PropertyNames.clear();
        }
    }

    void isElement(final Object[] a, final Object b, final int count) {
        assertNotNull(a);
        boolean cond = false;
        int k = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == b) {
                cond = true;
                k += 1;
            }
        }
        assertTrue(cond);
        assertEquals(count, k);
    }

    void assertEqualsPropertyChange(final AccessiblePropertyChangeListener listener,
            final String s[], final Object oldValues[], final Object newValues[],
            final boolean b[]) {
        for (int i = 0; i < listener.PropertyNames.size(); i++) {
            for (int j = 0; j < s.length; j++) {
                if (s[j] == listener.PropertyNames.get(i)) {
                    assertEquals(b[j], listener.wasPropertyChange[i]);
                    if (b[j]) {
                        assertEquals(oldValues[j], listener.oldValues[i]);
                        assertEquals(newValues[j], listener.newValues[i]);
                    }
                }
            }
        }
    }

    void assertEqualsPropertyChangeEvent(final String name, final Object oldValue,
            final Object newValue, final PropertyChangeEvent e) {
        assertEquals(name, e.getPropertyName());
        assertEquals(oldValue, e.getOldValue());
        assertEquals(newValue, e.getNewValue());
    }

    public JTextComponent_AccessibleJTextComponentTest() {
        setIgnoreNotImplemented(true);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jf = new JFrame();
        bWasException = false;
        str = null;
        listener = new AccessiblePropertyChangeListener();
        text = ("JTextComponent\n" + "\u05dc" + "JTextComponent.AccessibleJTextComponent");
        jtc = new JTextArea(text);
        accessible = (JTextComponent.AccessibleJTextComponent) jtc.getAccessibleContext();
        accessible.addPropertyChangeListener(listener);
        jf.getContentPane().add(jtc);
        jf.setLocation(200, 300);
        jf.setSize(200, 300);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testAccessibleJTextComponent() {
        assertNotNull(accessible);
    }

    private AttributeSet getAttributeSetByIndex(final AbstractDocument d, final int offset) {
        AttributeSet as = null;
        Element elem = d.getDefaultRootElement();
        while (elem.getElementCount() > 0) {
            elem = elem.getElement(elem.getElementIndex(offset));
            as = elem.getAttributes();
        }
        return as;
    }

    public void testSetAttributesGetCharacterAttributes() throws Exception {
        AttributeSet samples[] = new AttributeSet[20];
        AttributeSet as;
        String name1 = "ATTRIBUTE_NAME1";
        String value1 = "ATTRIBUTE_VALUE1";
        String name2 = "ATTRIBUTE_NAME2";
        String value2 = "ATTRIBUTE_VALUE2";
        SimpleAttributeSet sas = new SimpleAttributeSet();
        sas.addAttribute(name1, value1);
        sas.addAttribute(name2, value2);
        doc = (AbstractDocument) jtc.getDocument();
        for (int i = 0; i < 20; i++) {
            samples[i] = getAttributeSetByIndex(doc, i);
        }
        accessible.setAttributes(4, 6, sas);
        for (int i = 4; i < 7; i++) {
            as = getAttributeSetByIndex(doc, i);
            assertFalse(as.containsAttribute(name1, value1));
            assertFalse(as.containsAttribute(name2, value2));
        }
        for (int i = 0; i < 20; i++) {
            as = samples[i];
            assertEquals(as, accessible.getCharacterAttribute(i));
            assertEquals(as, getAttributeSetByIndex(doc, i));
        }
        jtc.setDocument(new DefaultStyledDocument());
        listener.reset();
        listener.InterestProperty(AccessibleContext.ACCESSIBLE_CARET_PROPERTY);
        jtc.setText("JTEXT_COMPONENT\n JTEXTCOMPONENT");
        obj_old[0] = new Integer(0);
        obj_new[0] = new Integer(31);
        assertEqualsPropertyChange(listener,
                new String[] { AccessibleContext.ACCESSIBLE_CARET_PROPERTY }, obj_old, obj_new,
                was);
        accessible.setAttributes(5, 8, sas);
        doc = (AbstractDocument) jtc.getDocument();
        for (int i = 0; i < 20; i++) {
            samples[i] = getAttributeSetByIndex(doc, i);
        }
        for (int i = 5; i < 8; i++) {
            as = getAttributeSetByIndex(doc, i);
            assertEquals(sas, as);
            assertEquals(as, accessible.getCharacterAttribute(i));
        }
        for (int i = 0; i < 20; i++) {
            if (i >= 5 && i < 7) {
                continue;
            }
            as = samples[i];
            assertEquals(as, accessible.getCharacterAttribute(i));
            assertEquals(as, getAttributeSetByIndex(doc, i));
        }
    }

    public void testRemoveUpdate() throws Exception {
        listener.InterestProperty(AccessibleContext.ACCESSIBLE_TEXT_PROPERTY);
        try {
            jtc.getDocument().remove(0, 4);
        } catch (BadLocationException e) {
        }
        obj_new[0] = new Integer(0);
        assertEqualsPropertyChange(listener, s, obj_old, obj_new, was);
        listener.reset();
        try {
            jtc.getDocument().remove(5, 3);
        } catch (BadLocationException e) {
        }
        obj_new[0] = new Integer(0);
        assertEqualsPropertyChange(listener, s, obj_old, obj_new, was);
    }

    public void testInsertUpdate() throws Exception {
        listener.InterestProperty(AccessibleContext.ACCESSIBLE_TEXT_PROPERTY);
        try {
            jtc.getDocument().insertString(0, "TEST", null);
        } catch (BadLocationException e) {
        }
        obj_new[0] = new Integer(0);
        assertEqualsPropertyChange(listener,
                new String[] { AccessibleContext.ACCESSIBLE_TEXT_PROPERTY }, obj_old, obj_new,
                was);
        listener.reset();
        try {
            jtc.getDocument().insertString(3, "TEST", null);
        } catch (BadLocationException e) {
        }
        obj_new[0] = new Integer(3);
        assertEqualsPropertyChange(listener,
                new String[] { AccessibleContext.ACCESSIBLE_TEXT_PROPERTY }, obj_old, obj_new,
                was);
    }

    public void testChangedUpdate() throws Exception {
        listener.InterestProperty(AccessibleContext.ACCESSIBLE_TEXT_PROPERTY);
        try {
            ((AbstractDocument) jtc.getDocument()).replace(0, 3, "TEST", null);
        } catch (BadLocationException e) {
        }
        obj_new[0] = new Integer(0);
        assertEqualsPropertyChange(listener, s, obj_old, obj_new, was);
        listener.reset();
        try {
            ((AbstractDocument) jtc.getDocument()).replace(4, 5, "TEST", null);
        } catch (BadLocationException e) {
        }
        obj_new[0] = new Integer(4);
        assertEqualsPropertyChange(listener, s, obj_old, obj_new, was);
    }

    public void testCaretUpdate() throws Exception {
        listener.InterestProperty(AccessibleContext.ACCESSIBLE_CARET_PROPERTY);
        listener.InterestProperty(AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY);
        String s[] = new String[] { AccessibleContext.ACCESSIBLE_CARET_PROPERTY,
                AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY };
        obj_old = new Object[] { null, null };
        obj_new = new Object[2];
        boolean was[] = new boolean[] { true, false };
        jtc.setCaretPosition(4);
        obj_new[0] = new Integer(4);
        obj_old[0] = new Integer(0);
        assertEqualsPropertyChange(listener, s, obj_old, obj_new, was);
        listener.reset();
        jtc.moveCaretPosition(7);
        obj_old[0] = new Integer(4);
        obj_new[0] = new Integer(7);
        was[1] = true;
        obj_new[1] = "tCo";
        assertEqualsPropertyChange(listener, s, obj_old, obj_new, was);
        listener.reset();
        jtc.moveCaretPosition(6);
        obj_old[0] = new Integer(7);
        obj_new[0] = new Integer(6);
        obj_new[1] = "tC";
        assertEqualsPropertyChange(listener, s, obj_old, obj_new, was);
        listener.reset();
        jtc.moveCaretPosition(4);
        obj_old[0] = new Integer(6);
        obj_new[0] = new Integer(4);
        was[1] = false;
        assertEqualsPropertyChange(listener, s, obj_old, obj_new, was);
    }

    public void testGetAccessibleText() {
        assertEquals(accessible, accessible.getAccessibleText());
    }

    private void checkState(final AccessibleStateSet set, final AccessibleState s) {
        assertTrue(set.contains(s));
        set.remove(s);
    }

    private void checkStateSet(final AccessibleStateSet ass) {
        if (jtc.isEnabled()) {
            checkState(ass, AccessibleState.ENABLED);
        }
        if (jtc.isFocusable()) {
            checkState(ass, AccessibleState.FOCUSABLE);
        }
        if (jtc.isVisible()) {
            checkState(ass, AccessibleState.VISIBLE);
        }
        if (jtc.isShowing()) {
            checkState(ass, AccessibleState.SHOWING);
        }
        if (jtc.isFocusOwner()) {
            checkState(ass, AccessibleState.FOCUSED);
        }
        if (jtc.isOpaque()) {
            checkState(ass, AccessibleState.OPAQUE);
        }
        assertEquals(0, ass.toArray().length);
    }

    public void testGetAccessibleStateSet() throws Exception {
        AccessibleStateSet ass = accessible.getAccessibleStateSet();
        assertTrue(ass.contains(AccessibleState.MULTI_LINE));
        assertTrue(ass.contains(AccessibleState.EDITABLE));
        ass.remove(AccessibleState.MULTI_LINE);
        ass.remove(AccessibleState.EDITABLE);
        checkStateSet(ass);
        jtc.setEditable(false);
        ass = accessible.getAccessibleStateSet();
        assertTrue(ass.contains(AccessibleState.MULTI_LINE));
        ass.remove(AccessibleState.MULTI_LINE);
        checkStateSet(ass);
    }

    public void testGetAccessibleRole() {
        assertEquals(AccessibleRole.TEXT, accessible.getAccessibleRole());
    }

    public void testGetAccessibleEditableText() {
        assertEquals(accessible, accessible.getAccessibleEditableText());
    }

    public void testGetAccessibleAction() {
        assertEquals(accessible, accessible.getAccessibleAction());
    }

    public void testSetTextContents() throws Exception {
        accessible.setTextContents("NEW Text");
        assertEquals("NEW Text", jtc.getText());
    }

    public void testInsertTextAtIndex() throws Exception {
        jtc.setCaretPosition(5);
        jtc.setDocument(new DefaultStyledDocument());
        jtc.setText(text);
        accessible.insertTextAtIndex(2, "SSS");
        assertEquals(text.substring(0, 2) + "SSS" + text.substring(2, text.length()), jtc
                .getText());
    }

    public void testReplaceText() throws Exception {
        accessible.replaceText(5, 8, "XXX");
        assertEquals(text.replaceFirst("Com", "XXX"), jtc.getText());
        try {
            accessible.replaceText(8, 5, "ZZZ");
            assertEquals(text.replaceFirst("Com", "XXXZZZ"), jtc.getText());
        } catch (IllegalArgumentException e) {
            bWasException = true;
        }
        if (isHarmony()) {
            assertTrue(bWasException);
        }
    }

    public void testGetTextRange() {
        assertEquals("JTextCo", accessible.getTextRange(0, 7));
        assertEquals("omponent\n" + "\u05dc" + "JTe", accessible.getTextRange(6, 19));
    }

    public void testGetAccessibleActionDescription() {
        for (int i = 0; i < jtc.getActions().length; i++) {
            assertEquals(jtc.getActions()[i].getValue(Action.NAME), accessible
                    .getAccessibleActionDescription(i));
        }
    }

    public void testGetSelectedText() throws Exception {
        jtc.setCaretPosition(5);
        assertNull(accessible.getSelectedText());
        jtc.moveCaretPosition(8);
        assertEquals("Com", accessible.getSelectedText());
    }

    public void testSelectText() throws Exception {
        accessible.selectText(5, 8);
        assertEquals("Com", accessible.getSelectedText());
    }

    public void testDelete() throws Exception {
        accessible.delete(4, 6);
        text = text.replaceFirst("tC", "");
        assertEquals(text, jtc.getText());
        try {
            accessible.delete(6, 4);
            text = text.replaceFirst("om", "");
            assertEquals(text, jtc.getText());
        } catch (IllegalArgumentException e) {
            bWasException = true;
        }
        if (isHarmony()) {
            assertTrue(bWasException);
        }
        bWasException = false;
        try {
            accessible.delete(-6, -4);
            assertEquals(text, jtc.getText());
        } catch (IllegalArgumentException e) {
            bWasException = true;
        }
        if (isHarmony()) {
            assertTrue(bWasException);
        }
    }

    String getClipboardString(final JTextComponent jtc) {
        String content = null;
        Toolkit toolkit = jtc.getToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        DataFlavor dataFlavor = DataFlavor.stringFlavor;
        try {
            content = (String) clipboard.getContents(null).getTransferData(dataFlavor);
        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {
        }
        return content;
    }

    void setClipboardString(final JTextComponent jtc, final String content) {
        Toolkit toolkit = jtc.getToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        StringSelection dataFlavor = new StringSelection(content);
        clipboard.setContents(dataFlavor, dataFlavor);
    }

    public void testCut() throws Exception {
        // TODO: uncomment when System clipboard is properly supported
        //        if (jtc.getToolkit().getSystemClipboard() == null)
        //            return;
        //        setClipboardString(jtc, "XXX");
        //        accessible.cut(2, 2);
        //        assertEquals("XXX", getClipboardString(jtc));
        //        assertEquals(2, jtc.getCaretPosition());
        //        assertEquals(text, jtc.getText());
        //
        //        accessible.cut(5, 8);
        //        assertEquals(5, jtc.getCaretPosition());
        //        assertEquals("Com", getClipboardString(jtc));
        //        assertEquals(text.replaceFirst("Com", ""), jtc.getText());
        //
        //        accessible.cut(9, 4);
        //        assertEquals(9, jtc.getCaretPosition());
        //        assertEquals(text.replaceFirst("Com", ""), jtc.getText());
    }

    public void testDoAccessibleAction() {
        assertTrue(accessible.doAccessibleAction(0));
        assertTrue(accessible.doAccessibleAction(1));
        assertTrue(accessible.doAccessibleAction(2));
    }

    public void testPaste() throws Exception {
        // TODO: uncomment when System clipboard is properly supported
        //        if (jtc.getToolkit().getSystemClipboard() == null)
        //            return;
        //        setClipboardString(jtc, "XXX");
        //        accessible.paste(5);
        //        assertEquals(text.substring(0, 5) + "XXX"
        //                + text.substring(5, text.length()), jtc.getText());
        //        assertEquals(8, jtc.getCaretPosition());
        //        setClipboardString(jtc, "");
        //        accessible.paste(10);
        //        assertEquals(10, jtc.getCaretPosition());
    }

    public void testGetSelectionStartEnd() throws Exception {
        jtc.setSelectionStart(5);
        jtc.setSelectionEnd(8);
        assertEquals(5, accessible.getSelectionStart());
        assertEquals(8, jtc.getSelectionEnd());
    }

    public void testGetCharCount() {
        assertEquals(jtc.getDocument().getLength(), accessible.getCharCount());
    }

    public void testGetCaretPosition() throws Exception {
        jtc.setCaretPosition(5);
        assertEquals(5, accessible.getCaretPosition());
        jtc.setCaretPosition(7);
        assertEquals(7, accessible.getCaretPosition());
    }

    public void testGetAccessibleActionCount() {
        assertEquals(53, accessible.getAccessibleActionCount());
    }
}