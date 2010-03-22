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
 * @author Roman I. Chernyatchik
 */
package javax.swing;

import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class JTextPaneRTest extends SwingTestCase {
    private JTextPane textPane;

    private MutableAttributeSet attrs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        textPane = new JTextPane();
        // init character attributeSet
        attrs = new SimpleAttributeSet();
        StyleConstants.setStrikeThrough(attrs, true);
        StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_CENTER);
        StyleConstants.setUnderline(attrs, true);
        textPane.getDocument().insertString(0, "Hello  !", attrs);
        StyleConstants.setUnderline(attrs, false);
        textPane.getDocument().insertString(6, "world", attrs);
    }

    public void testReplaceSelection_AtTheBeginningOfParagraph() {
        StyledDocument doc = textPane.getStyledDocument();
        try {
            attrs = new SimpleAttributeSet();
            StyleConstants.setUnderline(attrs, true);
            doc.insertString(0, "Hello word!", attrs);
            textPane.setCaretPosition(4);
            textPane.insertIcon(MetalIconFactory.getTreeFolderIcon());
            textPane.setCaretPosition(4);
            textPane.replaceSelection("\n");
            textPane.setCaretPosition(5);
            textPane.replaceSelection("a");
            textPane.setCaretPosition(6);
            textPane.replaceSelection("b");
            assertNull(StyleConstants.getIcon(doc.getCharacterElement(5).getAttributes()));
            assertNull(StyleConstants.getIcon(doc.getCharacterElement(6).getAttributes()));
        } catch (BadLocationException e) {
        }
    }

    public void testReplaceSelection_WithSelection() {
        textPane.setCaretPosition(4);
        textPane.insertIcon(MetalIconFactory.getTreeFolderIcon());
        textPane.select(4, 6);
        textPane.replaceSelection("TEXT");
        assertNull(StyleConstants.getIcon(textPane.getStyledDocument().getCharacterElement(4)
                .getAttributes()));
    }

    public void testReplaceSelection_WithNoSelection() {
        assertTrue(StyleConstants.isUnderline(textPane.getStyledDocument().getCharacterElement(
                5).getAttributes()));
        assertFalse(StyleConstants.isUnderline(textPane.getStyledDocument()
                .getCharacterElement(6).getAttributes()));
        textPane.select(6, 6);
        textPane.replaceSelection("TEXT");
        assertTrue(StyleConstants.isUnderline(textPane.getStyledDocument().getCharacterElement(
                6).getAttributes()));
    }
}
