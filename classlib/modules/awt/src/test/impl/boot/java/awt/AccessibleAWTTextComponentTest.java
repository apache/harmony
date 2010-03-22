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
 * @author Dmitry A. Durnev
 */
package java.awt;

import java.awt.TextComponent.AccessibleAWTTextComponent;

import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleText;

import junit.framework.TestCase;

public class AccessibleAWTTextComponentTest extends TestCase {
    TextComponent textComp;
    AccessibleAWTTextComponent aTextComp;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        textComp = new TextField();
        aTextComp = textComp.new AccessibleAWTTextComponent();
        assertTrue(textComp.getAccessibleContext()
                   instanceof AccessibleAWTTextComponent);
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getAccessibleRole()'
     */
    public void testGetAccessibleRole() {
        assertSame(AccessibleRole.TEXT, aTextComp.getAccessibleRole());
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getAccessibleStateSet()'
     */
    public void testGetAccessibleStateSet() {
        AccessibleState state = AccessibleState.EDITABLE;
        assertTrue(aTextComp.getAccessibleStateSet().contains(state));
        textComp.setEditable(false);
        assertFalse(aTextComp.getAccessibleStateSet().contains(state));
        textComp.setEditable(true);
        assertTrue(aTextComp.getAccessibleStateSet().contains(state));
    }

    public void testGetAccessibleText() {
        assertSame(aTextComp, aTextComp.getAccessibleText());
    }
    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.AccessibleAWTTextComponent(TextComponent)'
     */
    public void testAccessibleAWTTextComponent() {
        assertSame(aTextComp, textComp.getTextListeners()[0]);
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getCaretPosition()'
     */
    public void testGetCaretPosition() {
        assertEquals(0, aTextComp.getCaretPosition());
        textComp.setText("Text.");
        assertEquals(0, aTextComp.getCaretPosition());
        textComp.setCaretPosition(3);
        assertEquals(3, aTextComp.getCaretPosition());
        textComp.setCaretPosition(13);
        assertEquals(5, aTextComp.getCaretPosition());

    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getCharCount()'
     */
    public void testGetCharCount() {
        assertEquals(0, aTextComp.getCharCount());
        String text = "text";
        textComp.setText(text);
        assertEquals(text.length(), aTextComp.getCharCount());
        textComp.setText(null);
        assertEquals(0, aTextComp.getCharCount());
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getSelectionEnd()'
     */
    public void testGetSelectionEnd() {
        assertEquals(0, aTextComp.getSelectionEnd());
        String text = "text";
        textComp.setText(text);
        assertEquals(0, aTextComp.getSelectionEnd());
        textComp.select(1, 2);
        assertEquals(2, aTextComp.getSelectionEnd());
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getSelectionStart()'
     */
    public void testGetSelectionStart() {
        assertEquals(0, aTextComp.getSelectionStart());
        String text = "text";
        textComp.setText(text);
        assertEquals(0, aTextComp.getSelectionStart());
        textComp.select(1, 2);
        assertEquals(1, aTextComp.getSelectionStart());
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getIndexAtPoint(Point)'
     */
    public void testGetIndexAtPoint() {
        assertEquals(-1, aTextComp.getIndexAtPoint(new Point()));
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getCharacterBounds(int)'
     */
    public void testGetCharacterBounds() {
        assertNull(aTextComp.getCharacterBounds(0));
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getSelectedText()'
     */
    public void testGetSelectedText() {
        assertNull(aTextComp.getSelectedText());
        String text = "text";
        textComp.setText(text);
        textComp.selectAll();
        assertEquals(text, aTextComp.getSelectedText());
        textComp.select(0, 1);
        assertEquals(text.substring(0, 1), aTextComp.getSelectedText());

    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getAfterIndex(int, int)'
     */
    public void testGetAfterIndex() {
        assertNull(aTextComp.getAfterIndex(AccessibleText.WORD, 5));
        textComp.setText("This is some text.\n Second line.");
        assertEquals("s", aTextComp.getAfterIndex(AccessibleText.CHARACTER, 2));
        assertEquals("Second", aTextComp.getAfterIndex(AccessibleText.WORD, 13));
        assertNull("no sentences", aTextComp.getAfterIndex(AccessibleText.SENTENCE, 0));

    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getAtIndex(int, int)'
     */
    public void testGetAtIndex() {
        assertNull(aTextComp.getAtIndex(AccessibleText.CHARACTER, 0));
        textComp.setText("This is some text.\nSecond sentence.");
        assertEquals("s", aTextComp.getAtIndex(AccessibleText.CHARACTER, 3));
        assertEquals("Second", aTextComp.getAtIndex(AccessibleText.WORD, 19));
        assertEquals("Second sentence.", aTextComp.getAtIndex(AccessibleText.SENTENCE, 19));
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getBeforeIndex(int, int)'
     */
    public void testGetBeforeIndex() {
        assertNull(aTextComp.getAtIndex(AccessibleText.SENTENCE, 1));
        textComp.setText("This is some text.\nSecond sentence. Third sentence.");
        assertEquals("s", aTextComp.getBeforeIndex(AccessibleText.CHARACTER, 4));
        assertEquals("Second", aTextComp.getBeforeIndex(AccessibleText.WORD, 26));
        assertEquals("This is some text.\n", aTextComp.getBeforeIndex(AccessibleText.SENTENCE,
                                                                  20));
        assertEquals("Second sentence. ", aTextComp.getBeforeIndex(AccessibleText.SENTENCE,
                                                                      38));
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.getCharacterAttribute(int)'
     */
    public void testGetCharacterAttribute() {
        assertNull(aTextComp.getCharacterAttribute(0));
    }

    /*
     * Test method for 'java.awt.TextComponent.AccessibleAWTTextComponent.textValueChanged(TextEvent)'
     */
    public void testTextValueChanged() {
        // nothing to check...

    }

}
