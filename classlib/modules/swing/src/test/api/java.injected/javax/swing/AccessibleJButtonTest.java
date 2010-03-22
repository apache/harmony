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
 * @author Alexander T. Simbirtsev
 * Created on 11.02.2005

 */
package javax.swing;

import java.awt.Point;
import java.awt.image.BufferedImage;
import javax.accessibility.AccessibleRelationSet;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleText;

public class AccessibleJButtonTest extends BasicSwingTestCase {
    protected AbstractButton button;

    private JButton.AccessibleJButton aContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        button = new JButton();
        aContext = (JButton.AccessibleJButton) button.getAccessibleContext();
    }

    @Override
    protected void tearDown() throws Exception {
        button = null;
        aContext = null;
        super.tearDown();
    }

    public static void main(final String[] args) {
    }

    public void testGetAccessibleActionCount() {
        assertEquals("AccessibleActionCount", 1, aContext.getAccessibleActionCount());
    }

    public void testGetAccessibleValue() {
        assertEquals("AccessibleAction", aContext, aContext.getAccessibleAction());
    }

    public void testGetAccessibleKeyBinding() {
        assertNull("AccessibleKeyBinding ", aContext.getAccessibleKeyBinding());
    }

    public void testGetAccessibleText() {
        assertNull("AccessibleText ", aContext.getAccessibleText());
    }

    public void testGetToolTipText() {
        String text1 = "do what you feel";
        String text2 = "what you need to do";
        button.setText(text1);
        assertNull("ToolTipText ", aContext.getToolTipText());
        button.setToolTipText(text2);
        assertEquals("ToolTipText ", text2, aContext.getToolTipText());
    }

    public void testGetAccessibleIcon() {
        ImageIcon icon1 = new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY));
        ImageIcon icon2 = new ImageIcon(new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY));
        assertNull("AccessibleIcon", aContext.getAccessibleIcon());
        button.setIcon(icon1);
        assertEquals("number of AccessibleIcons", 1, aContext.getAccessibleIcon().length);
        assertEquals("AccessibleIcon", icon1.getAccessibleContext(), aContext
                .getAccessibleIcon()[0]);
        button.setDisabledIcon(icon2);
        assertEquals("number of AccessibleIcons", 1, aContext.getAccessibleIcon().length);
        assertEquals("AccessibleIcon", icon1.getAccessibleContext(), aContext
                .getAccessibleIcon()[0]);
    }

    public void testGetAccessibleStateSet() {
        assertNotNull("AccessibleStateSet is not null", aContext.getAccessibleStateSet());
        assertTrue("AccessibleStateSet contains ENABLED", aContext.getAccessibleStateSet()
                .contains(AccessibleState.ENABLED));
        assertTrue("AccessibleStateSet contains FOCUSABLE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.FOCUSABLE));
        assertTrue("AccessibleStateSet contains VISIBLE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.VISIBLE));
        assertTrue("AccessibleStateSet contains OPAQUE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.OPAQUE));
        button.setSelected(true);
        assertNotNull("AccessibleStateSet is not null", aContext.getAccessibleStateSet());
        assertTrue("AccessibleStateSet contains ENABLED", aContext.getAccessibleStateSet()
                .contains(AccessibleState.ENABLED));
        assertTrue("AccessibleStateSet contains FOCUSABLE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.FOCUSABLE));
        assertTrue("AccessibleStateSet contains VISIBLE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.VISIBLE));
        assertTrue("AccessibleStateSet contains OPAQUE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.OPAQUE));
        assertTrue("AccessibleStateSet contains CHECKED", aContext.getAccessibleStateSet()
                .contains(AccessibleState.CHECKED));
        button.setEnabled(false);
        assertNotNull("AccessibleStateSet is not null", aContext.getAccessibleStateSet());
        assertFalse("AccessibleStateSet doesn't contain ENABLED", aContext
                .getAccessibleStateSet().contains(AccessibleState.ENABLED));
        assertTrue("AccessibleStateSet contains FOCUSABLE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.FOCUSABLE));
        assertTrue("AccessibleStateSet contains VISIBLE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.VISIBLE));
        assertTrue("AccessibleStateSet contains OPAQUE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.OPAQUE));
        assertTrue("AccessibleStateSet contains CHECKED", aContext.getAccessibleStateSet()
                .contains(AccessibleState.CHECKED));
        button.setVisible(false);
        assertNotNull("AccessibleStateSet is not null", aContext.getAccessibleStateSet());
        assertFalse("AccessibleStateSet doesn't contain ENABLED", aContext
                .getAccessibleStateSet().contains(AccessibleState.ENABLED));
        assertTrue("AccessibleStateSet contains FOCUSABLE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.FOCUSABLE));
        assertFalse("AccessibleStateSet doesn't contain VISIBLE", aContext
                .getAccessibleStateSet().contains(AccessibleState.VISIBLE));
        assertTrue("AccessibleStateSet contains OPAQUE", aContext.getAccessibleStateSet()
                .contains(AccessibleState.OPAQUE));
        assertTrue("AccessibleStateSet contains CHECKED", aContext.getAccessibleStateSet()
                .contains(AccessibleState.CHECKED));
    }

    public void testDoAccessibleAction() {
        assertTrue("AccessibleAction result", aContext.doAccessibleAction(0));
        assertFalse("AccessibleAction result", aContext.doAccessibleAction(1));
        assertFalse("AccessibleAction result", aContext.doAccessibleAction(-1));
    }

    public void testGetAccessibleName() {
        String name = "namemeams";
        assertEquals("AccessibleName", "", aContext.getAccessibleName());
        button.setText(name);
        assertEquals("AccessibleName", name, aContext.getAccessibleName());
    }

    public void testGetAccessibleRelationSet() {
        AccessibleRelationSet relations = aContext.getAccessibleRelationSet();
        assertNotNull("AccessibleRelationSet isn't null", relations);
        assertEquals("AccessibleRelationSet size", 0, relations.size());
    }

    public void testGetAccessibleAction() {
        assertEquals("AccessibleAction", aContext, aContext.getAccessibleAction());
    }

    public void testGetAccessibleActionDescription() {
        assertEquals("AccessibleActionDescription", "click", aContext
                .getAccessibleActionDescription(0));
        assertNull("AccessibleActionDescription", aContext.getAccessibleActionDescription(1));
    }

    public void testGetCurrentAccessibleValue() {
        button.setSelected(false);
        assertEquals("CurrentAccessibleValue", new Integer(0), aContext
                .getCurrentAccessibleValue());
        button.setSelected(true);
        assertEquals("CurrentAccessibleValue", new Integer(1), aContext
                .getCurrentAccessibleValue());
    }

    public void testGetMaximumAccessibleValue() {
        assertEquals("MaximumAccessibleValue", new Integer(1), aContext
                .getMaximumAccessibleValue());
    }

    public void testGetMinimumAccessibleValue() {
        assertEquals("MinimumAccessibleValue", new Integer(0), aContext
                .getMinimumAccessibleValue());
    }

    /**
     * this method now inherits all functionality now from JComponent.AccessibleComponent
     */
    public void testGetTitledBorderText() {
    }

    public void testSetCurrentAccessibleValue() {
        PropertyChangeController listener = new PropertyChangeController();
        aContext.addPropertyChangeListener(listener);
        assertTrue("returned value", aContext.setCurrentAccessibleValue(new Integer(100)));
        assertEquals("CurrentAccessibleValue", new Integer(1), aContext
                .getCurrentAccessibleValue());
        assertTrue("button selected state", button.isSelected());
        listener
                .checkPropertyFired(aContext, "AccessibleState", null, AccessibleState.SELECTED);
        listener
                .checkPropertyFired(aContext, "AccessibleValue", new Integer(0), new Integer(1));
        listener.reset();
        assertTrue("returned value", aContext.setCurrentAccessibleValue(new Integer(0)));
        assertEquals("CurrentAccessibleValue", new Integer(0), aContext
                .getCurrentAccessibleValue());
        assertFalse("button selected state", button.isSelected());
        listener
                .checkPropertyFired(aContext, "AccessibleState", AccessibleState.SELECTED, null);
        listener
                .checkPropertyFired(aContext, "AccessibleValue", new Integer(1), new Integer(0));
        listener.reset();
        assertTrue("returned value", aContext.setCurrentAccessibleValue(new Integer(0)));
        assertFalse("no event's fired ", listener.isChanged());
    }

    public void testGetCaretPosition() {
        assertEquals("CaretPosition", -1, aContext.getCaretPosition());
        button.setText("text");
        assertEquals("CaretPosition", -1, aContext.getCaretPosition());
    }

    public void testGetCharCount() {
        assertEquals("CharCount", 0, aContext.getCharCount());
        button.setText("text");
        assertEquals("CharCount", 4, aContext.getCharCount());
    }

    public void testGetSelectionEnd() {
        assertEquals("SelectionEnd", -1, aContext.getSelectionEnd());
        button.setText("text");
        assertEquals("SelectionEnd", -1, aContext.getSelectionEnd());
        button.setSelected(true);
        assertEquals("SelectionEnd", -1, aContext.getSelectionEnd());
    }

    public void testGetSelectionStart() {
        assertEquals("SelectionStart", -1, aContext.getSelectionStart());
        button.setText("text");
        assertEquals("SelectionStart", -1, aContext.getSelectionStart());
        button.setSelected(true);
        assertEquals("SelectionStart", -1, aContext.getSelectionStart());
    }

    public void testGetIndexAtPoint() {
        assertEquals("IndexAtPoint", -1, aContext.getIndexAtPoint(new Point(0, 0)));
        button.setText("text");
        assertEquals("IndexAtPoint", -1, aContext.getIndexAtPoint(new Point(2, 2)));
    }

    public void testGetCharacterBounds() {
        assertNull("CharacterBounds", aContext.getCharacterBounds(0));
        button.setText("text");
        assertNull("CharacterBounds", aContext.getCharacterBounds(1));
    }

    public void testGetSelectedText() {
        assertNull("SelectedText", aContext.getSelectedText());
        button.setText("text");
        assertNull("SelectedText", aContext.getSelectedText());
        button.setSelected(true);
        assertNull("SelectedText", aContext.getSelectedText());
    }

    public void testGetAfterIndex() {
        assertNull("AfterIndex", aContext.getAfterIndex(AccessibleText.CHARACTER, 0));
        assertNull("AfterIndex", aContext.getAfterIndex(AccessibleText.WORD, 0));
        assertNull("AfterIndex", aContext.getAfterIndex(AccessibleText.SENTENCE, 0));
        button.setText("Just give me the light. and start the show");
        assertNull("AfterIndex", aContext.getAfterIndex(AccessibleText.CHARACTER, 6));
    }

    public void testGetAtIndex() {
        assertNull("AtIndex", aContext.getAtIndex(AccessibleText.CHARACTER, 0));
        assertNull("AtIndex", aContext.getAtIndex(AccessibleText.WORD, 0));
        assertNull("AtIndex", aContext.getAtIndex(AccessibleText.SENTENCE, 0));
        button.setText("Just give me the light. and start the show");
        assertNull("AtIndex", aContext.getAtIndex(AccessibleText.CHARACTER, 6));
    }

    public void testGetBeforeIndex() {
        assertNull("BeforeIndex", aContext.getBeforeIndex(AccessibleText.CHARACTER, 0));
        assertNull("BeforeIndex", aContext.getBeforeIndex(AccessibleText.WORD, 0));
        assertNull("BeforeIndex", aContext.getBeforeIndex(AccessibleText.SENTENCE, 0));
        button.setText("Just give me the light. and start the show");
        assertNull("BeforeIndex", aContext.getBeforeIndex(AccessibleText.CHARACTER, 6));
    }

    public void testGetCharacterAttribute() {
        assertNull("CharacterAttribute", aContext.getCharacterAttribute(0));
        button.setText("text");
        assertNull("CharacterAttribute", aContext.getCharacterAttribute(1));
    }
}
