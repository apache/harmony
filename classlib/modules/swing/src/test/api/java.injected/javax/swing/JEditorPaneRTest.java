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

import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

public class JEditorPaneRTest extends SwingTestCase {
    public void testPlainViewFactory() {
        JEditorPane jp = new JEditorPane();
        String name = jp.getUI().getRootView(jp).getView(0).getClass().getName();
        assertEquals("javax.swing.text.WrappedPlainView", name);
    }

    public void testCreateDefaultEditorKit() {
        JEditorPane pane = new JEditorPane() {
            private static final long serialVersionUID = 1L;

            @Override
            protected EditorKit createDefaultEditorKit() {
                return new StyledEditorKit();
            }
        };
        assertTrue(pane.getEditorKit() instanceof StyledEditorKit);
    }

    public boolean wasInstallCall;

    public void testCreateDefaultEditorKit_installCall() {
        JEditorPane pane = new JEditorPane() {
            private static final long serialVersionUID = 1L;
        };
        pane.setEditorKit(new StyledEditorKit() {
            private static final long serialVersionUID = 1L;

            @Override
            public void install(JEditorPane component) {
                wasInstallCall = true;
                super.install(component);
            }
        });
        assertTrue(wasInstallCall);
    }

    public void testInputAttributes() {
        JEditorPane pane = new JEditorPane() {
            private static final long serialVersionUID = 1L;

            @Override
            protected EditorKit createDefaultEditorKit() {
                return new StyledEditorKit();
            }
        };
        pane.setEditorKit(new StyledEditorKit());
        StyledDocument doc = (StyledDocument) pane.getDocument();
        StyledEditorKit kit = (StyledEditorKit) pane.getEditorKit();
        try {
            MutableAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setUnderline(attrs, true);
            doc.insertString(0, "Hello word!", attrs);
            pane.setCaretPosition(4);
            attrs = new SimpleAttributeSet();
            StyleConstants.setIcon(attrs, MetalIconFactory.getTreeFolderIcon());
            doc.insertString(4, " ", attrs);
            pane.setCaretPosition(4);
            doc.insertString(4, "\n", kit.getInputAttributes());
            assertFalse(StyleConstants.isUnderline(kit.getInputAttributes()));
            pane.setCaretPosition(5);
            assertFalse(StyleConstants.isUnderline(kit.getInputAttributes()));
        } catch (BadLocationException e) {
        }
    }
}
