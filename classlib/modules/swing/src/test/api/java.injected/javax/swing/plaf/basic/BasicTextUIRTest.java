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
package javax.swing.plaf.basic;

import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import javax.swing.JEditorPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingTestCase;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;

public class BasicTextUIRTest extends SwingTestCase {
    private void checkEditableFTK(final JTextComponent comp, final int hashCode1,
            final int hashCode2) {
        Set<?> keys = comp.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        assertEquals(1, keys.size());
        assertEquals(hashCode1, keys.hashCode());
        assertTrue(keys.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
                InputEvent.CTRL_DOWN_MASK)));
        keys = comp.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
        assertEquals(1, keys.size());
        assertEquals(hashCode2, keys.hashCode());
        assertTrue(keys.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
    }

    private void checkNotEditableFTK(final JTextComponent comp) {
        Set<?> keys = comp.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        assertEquals(2, keys.size());
        assertTrue(keys.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
                InputEvent.CTRL_DOWN_MASK)));
        assertTrue(keys.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        keys = comp.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
        assertEquals(2, keys.size());
        assertTrue(keys.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
        assertTrue(keys.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
                InputEvent.SHIFT_DOWN_MASK)));
    }

    public void testFocusTraversalKeys() {
        JTextComponent textComp = new JTextArea();
        int forwardFTKHashCode = textComp.getFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS).hashCode();
        int backwardFTKHashCode = textComp.getFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS).hashCode();
        checkEditableFTK(textComp, forwardFTKHashCode, backwardFTKHashCode);
        textComp.setEditable(false);
        checkNotEditableFTK(textComp);
        textComp.setEditable(true);
        checkEditableFTK(textComp, forwardFTKHashCode, backwardFTKHashCode);
        textComp = new JEditorPane();
        forwardFTKHashCode = textComp.getFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS).hashCode();
        backwardFTKHashCode = textComp.getFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS).hashCode();
        checkEditableFTK(textComp, forwardFTKHashCode, backwardFTKHashCode);
        textComp.setEditable(false);
        checkNotEditableFTK(textComp);
        textComp.setEditable(true);
        checkEditableFTK(textComp, forwardFTKHashCode, backwardFTKHashCode);
        textComp = new JTextField();
        forwardFTKHashCode = textComp.getFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS).hashCode();
        backwardFTKHashCode = textComp.getFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS).hashCode();
        checkNotEditableFTK(textComp);
        textComp.setEditable(false);
        checkNotEditableFTK(textComp);
        textComp.setEditable(true);
        checkNotEditableFTK(textComp);
    }

    public void testUninstallUI() {
        // Regression for HARMONY-1475
        JTextComponent textComp = new JTextField();
        AbstractDocument doc = ((AbstractDocument) textComp.getDocument());
        assertTrue("listeners installed", doc.getDocumentListeners().length > 0);
        textComp.getUI().uninstallUI(textComp);
        assertEquals("no listeners installed", 0, doc.getDocumentListeners().length);
    }
}
