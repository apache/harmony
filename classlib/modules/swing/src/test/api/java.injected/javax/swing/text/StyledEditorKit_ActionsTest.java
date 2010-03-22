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

import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.SwingTestCase;

public class StyledEditorKit_ActionsTest extends SwingTestCase {
    JEditorPane jep;

    int dot;

    StyledEditorKit kit;

    StyledDocument doc;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        jep = new JEditorPane();
        kit = new StyledEditorKit();
        jep.setEditorKit(kit);
        doc = (StyledDocument) jep.getDocument();
        jep.setText("ABCD\nEFGH\n" + "IGKL\nMNOP\n" + "QRST\nUVWX\n");
        dot = 18;
        jep.setCaretPosition(dot);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private String toString(final int i) {
        return (new Integer(i)).toString();
    }

    private void checkAttribute(final int offset, final Object key, final Object value) {
        Object realValue = ((AbstractDocument) jep.getDocument()).getParagraphElement(offset)
                .getAttributes().getAttribute(key);
        assertEquals(value, realValue);
    }

    private void checkAttribute(final int offset, final Object key, final int value) {
        checkAttribute(offset, key, new Integer(value));
    }

    private void checkName(final Action action, final String name) {
        assertEquals(name, action.getValue(Action.NAME));
    }

    private void assertTrue(final Object obj) {
        assertTrue(((Boolean) obj).booleanValue());
    }

    private void assertFalse(final Object obj) {
        assertFalse(((Boolean) obj).booleanValue());
    }

    private void checkProperty(final int start, final int end, final Object attribute,
            final Object value, final Object alternative) {
        for (int i = 0; i < doc.getLength(); i++) {
            Object property = StyledEditorKit_StyledTextActionTest.getAttributeSetByOffset(doc,
                    i).getAttribute(attribute);
            if (i >= start && i <= end) {
                assertEquals(value, property);
            } else {
                assertEquals(alternative, property);
            }
        }
    }

    private void checkToggleAction(final Action action, final String name,
            final Object attribute) {
        checkName(action, name);
        ActionEvent ae = new ActionEvent(jep, ActionEvent.ACTION_PERFORMED, null);
        action.actionPerformed(ae);
        assertTrue(kit.getInputAttributes().getAttribute(attribute));
        action.actionPerformed(ae);
        assertFalse(kit.getInputAttributes().getAttribute(attribute));
        action.actionPerformed(ae);
        assertTrue(kit.getInputAttributes().getAttribute(attribute));
        ae = new ActionEvent(jep, ActionEvent.ACTION_PERFORMED, "true");
        action.actionPerformed(ae);
        assertFalse(kit.getInputAttributes().getAttribute(attribute));
        jep.setSelectionStart(2);
        jep.setSelectionEnd(5);
        action.actionPerformed(ae);
        checkProperty(3, 5, attribute, Boolean.TRUE, null);
        assertTrue(kit.getInputAttributes().getAttribute(attribute));
        action.actionPerformed(null);
    }

    private void checkNonToggleAction(final Action action, final String name,
            final Object attribute, final Object value, final Object defaultValue,
            final Object alternative, final String param1, final String param2) {
        checkName(action, name);
        ActionEvent ae1 = new ActionEvent(jep, ActionEvent.ACTION_PERFORMED, param1);
        action.actionPerformed(ae1);
        checkProperty(100, 100, attribute, alternative, alternative);
        assertEquals(defaultValue, kit.getInputAttributes().getAttribute(attribute));
        ActionEvent ae2 = new ActionEvent(jep, ActionEvent.ACTION_PERFORMED, param2);
        action.actionPerformed(ae2);
        checkProperty(100, 100, attribute, alternative, alternative);
        assertEquals(value, kit.getInputAttributes().getAttribute(attribute));
        jep.setSelectionStart(2);
        jep.setSelectionEnd(5);
        action.actionPerformed(ae1);
        assertEquals(defaultValue, kit.getInputAttributes().getAttribute(attribute));
        checkProperty(3, 5, attribute, defaultValue, alternative);
        action.actionPerformed(ae2);
        assertEquals(value, kit.getInputAttributes().getAttribute(attribute));
        checkProperty(3, 5, attribute, value, alternative);
        action.actionPerformed(null);
    }

    public void testAlignmentAction() {
        Action action = new StyledEditorKit.AlignmentAction(null,
                StyleConstants.ALIGN_JUSTIFIED);
        checkName(action, null);
        action = new StyledEditorKit.AlignmentAction("alignmentName",
                StyleConstants.ALIGN_JUSTIFIED);
        checkName(action, "alignmentName");
        ActionEvent ae = new ActionEvent(jep, ActionEvent.ACTION_PERFORMED,
                toString(StyleConstants.ALIGN_CENTER));
        action.actionPerformed(ae);
        checkAttribute(dot, StyleConstants.Alignment, StyleConstants.ALIGN_CENTER);
        ae = new ActionEvent(jep, ActionEvent.ACTION_PERFORMED, null);
        action.actionPerformed(ae);
        checkAttribute(dot, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
        ae = new ActionEvent(jep, ActionEvent.ACTION_PERFORMED, "aa");
        action.actionPerformed(ae);
        checkAttribute(dot, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
        ae = new ActionEvent(jep, ActionEvent.ACTION_PERFORMED, "");
        action.actionPerformed(ae);
        checkAttribute(dot, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
        action.actionPerformed(null);
    }

    public void testBoldAction() {
        Action action = new StyledEditorKit.BoldAction();
        checkToggleAction(action, "font-bold", StyleConstants.Bold);
    }

    public void testFontFamilyAction() {
        String defaultValue = "SansSerif";
        Object alternative = null;
        String value = "Monospaced";
        Action action = new StyledEditorKit.FontFamilyAction(null, defaultValue);
        checkName(action, null);
        action = new StyledEditorKit.FontFamilyAction("familyActionName", defaultValue);
        Object attribute = StyleConstants.FontFamily;
        checkNonToggleAction(action, "familyActionName", attribute, value, defaultValue,
                alternative, null, value);
    }

    public void testFontSizeAction() {
        Integer defaultValue = new Integer(48);
        Object alternative = null;
        Integer value = new Integer(16);
        Action action = new StyledEditorKit.FontFamilyAction(null, "48");
        checkName(action, null);
        action = new StyledEditorKit.FontSizeAction("sizeActionName", 48);
        Object attribute = StyleConstants.FontSize;
        checkNonToggleAction(action, "sizeActionName", attribute, value, defaultValue,
                alternative, "", "16");
    }

    public void testForegroundAction() {
        Color defaultValue = Color.RED;
        Object alternative = null;
        Object value = Color.decode("425");
        Action action = new StyledEditorKit.ForegroundAction(null, defaultValue);
        checkName(action, null);
        action = new StyledEditorKit.ForegroundAction("foregroundName", defaultValue);
        Object attribute = StyleConstants.Foreground;
        checkNonToggleAction(action, "foregroundName", attribute, value, defaultValue,
                alternative, "", "425");
    }

    public void testItalicAction() {
        Action action = new StyledEditorKit.ItalicAction();
        checkToggleAction(action, "font-italic", StyleConstants.Italic);
    }

    public void testUnderlineAction() {
        Action action = new StyledEditorKit.UnderlineAction();
        checkToggleAction(action, "font-underline", StyleConstants.Underline);
    }
}