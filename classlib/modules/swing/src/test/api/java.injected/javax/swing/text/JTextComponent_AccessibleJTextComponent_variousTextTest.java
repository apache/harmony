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

import java.awt.Point;
import java.awt.Rectangle;
import javax.accessibility.AccessibleText;
import javax.swing.BasicSwingTestCase;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import junit.framework.TestCase;

public class JTextComponent_AccessibleJTextComponent_variousTextTest extends TestCase {
    JFrame jf;

    JTextComponent jtc;

    JTextComponent.AccessibleJTextComponent accessible;

    boolean bWasException;

    String str;

    JTextField jep;

    Rectangle rect;

    String text;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jf = new JFrame();
        bWasException = false;
        jtc = new JTextArea();
        text = ("JTextComponent\n" + "cJTextComponent.AccessibleJTextComponent");
        jtc.setText(text);
        accessible = (JTextComponent.AccessibleJTextComponent) jtc.getAccessibleContext();
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

    private Rectangle getCharBounds(final JTextComponent component, final int ind) {
        Rectangle rect1 = null;
        Rectangle rect2 = null;
        try {
            rect1 = component.getUI().modelToView(component, ind, Position.Bias.Forward);
        } catch (BadLocationException e) {
        }
        try {
            rect2 = component.getUI().modelToView(component, ind + 1, Position.Bias.Backward);
        } catch (BadLocationException e) {
        }
        assertNotNull(rect1);
        assertNotNull(rect2);
        rect1.width = Math.abs(rect1.x - rect2.x) + 1;
        rect1.x = Math.min(rect1.x, rect2.x);
        return rect1;
    }

    public void testGetBeforeAfterAtIndex() throws Exception {
        Runnable test = new Runnable() {
            public void run() {
                jtc.setText("In\n" + " this test. for AccessibleJTextComponent\n" + "LALALA");
            }
        };
        SwingUtilities.invokeAndWait(test);
        assertEquals("t", accessible.getAtIndex(AccessibleText.CHARACTER, 4));
        assertEquals("this", accessible.getAtIndex(AccessibleText.WORD, 4));
        assertEquals(" this test. for AccessibleJTextComponent\n", accessible.getAtIndex(
                AccessibleText.SENTENCE, 4));
        assertEquals(" ", accessible.getAtIndex(AccessibleText.CHARACTER, 3));
        assertEquals(" ", accessible.getAtIndex(AccessibleText.WORD, 3));
        assertEquals(" this test. for AccessibleJTextComponent\n", accessible.getAtIndex(
                AccessibleText.SENTENCE, 3));
        assertEquals(" ", accessible.getAtIndex(AccessibleText.CHARACTER, 8));
        assertEquals(" ", accessible.getAtIndex(AccessibleText.WORD, 8));
        assertEquals(" this test. for AccessibleJTextComponent\n", accessible.getAtIndex(
                AccessibleText.SENTENCE, 8));
        assertEquals("h", accessible.getAfterIndex(AccessibleText.CHARACTER, 4));
        assertEquals("LALALA\n", accessible.getAfterIndex(AccessibleText.SENTENCE, 4));
        assertEquals("t", accessible.getAfterIndex(AccessibleText.CHARACTER, 3));
        assertEquals("this", accessible.getAfterIndex(AccessibleText.WORD, 3));
        assertEquals("LALALA\n", accessible.getAfterIndex(AccessibleText.SENTENCE, 3));
        assertEquals("t", accessible.getAfterIndex(AccessibleText.CHARACTER, 8));
        assertEquals("test", accessible.getAfterIndex(AccessibleText.WORD, 8));
        assertEquals("LALALA\n", accessible.getAfterIndex(AccessibleText.SENTENCE, 8));
        assertNull(accessible.getAfterIndex(AccessibleText.SENTENCE, jtc.getDocument()
                .getLength() - 2));
        assertEquals(" ", accessible.getBeforeIndex(AccessibleText.CHARACTER, 4));
        assertEquals(" ", accessible.getBeforeIndex(AccessibleText.WORD, 4));
        assertEquals("In\n", accessible.getBeforeIndex(AccessibleText.SENTENCE, 4));
        assertEquals("\n", accessible.getBeforeIndex(AccessibleText.CHARACTER, 3));
        assertEquals("\n", accessible.getBeforeIndex(AccessibleText.WORD, 3));
        assertEquals("In\n", accessible.getBeforeIndex(AccessibleText.SENTENCE, 3));
        assertEquals("s", accessible.getBeforeIndex(AccessibleText.CHARACTER, 8));
        assertEquals("this", accessible.getBeforeIndex(AccessibleText.WORD, 8));
        assertEquals("In\n", accessible.getBeforeIndex(AccessibleText.SENTENCE, 8));
        assertNull(accessible.getBeforeIndex(AccessibleText.SENTENCE, 1));
    }

    public void testGetCharacterBounds() throws Exception {
        Runnable test = new Runnable() {
            public void run() {
                jtc.setText("\u05dc" + "\u0061" + "\u05dc" + "\u0061");
            }
        };
        SwingUtilities.invokeAndWait(test);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(getCharBounds(jtc, 1), accessible.getCharacterBounds(1));
            assertEquals(getCharBounds(jtc, 2), accessible.getCharacterBounds(2));
        }
    }

    public void testGetIndexAtPoint() throws Exception {
        Runnable test = new Runnable() {
            public void run() {
                //Bidirectional text is not supported
                //jtc.setText("\u05dc" + "\u0061" + "\u05dc" + "\u0061");
                jtc.setText("testGetIndexAtPoint");
            }
        };
        SwingUtilities.invokeAndWait(test);
        rect = null;
        try {
            rect = jtc.modelToView(1);
        } catch (BadLocationException e) {
        }
        assertNotNull(rect);
        assertEquals(1, accessible.getIndexAtPoint(new Point(rect.x, rect.y)));
        rect = null;
        try {
            rect = jtc.modelToView(2);
        } catch (BadLocationException e) {
        }
        assertNotNull(rect);
        assertEquals(2, accessible.getIndexAtPoint(new Point(rect.x, rect.y)));
    }
}
