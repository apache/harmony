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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.SwingTestCase;
import javax.swing.plaf.basic.BasicTextUI;

public class PasswordViewTest extends SwingTestCase {
    PasswordView view;

    JPasswordField pf;

    String content = "abcd\tefg";

    JFrame jf;

    int x = 123;

    int y = 234;

    Segment text;

    FontMetrics fm;

    Segment echoChars;

    int textWidth;

    int echoCharsWidth;

    int height;

    char zero = '\0';

    int length;

    boolean bWasException;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bWasException = false;
        length = content.length();
        jf = new JFrame();
        pf = new JPasswordField(content);
        view = (PasswordView) ((BasicTextUI) pf.getUI()).getRootView(pf).getView(0);
        fm = pf.getFontMetrics(pf.getFont());
        assertNotNull(fm);
        text = new Segment();
        pf.getDocument().getText(0, length, text);
        echoChars = new Segment(new char[] { '*', '*', '*', '*', '*', '*', '*', '*' }, 0,
                length);
        textWidth = Utilities.getTabbedTextWidth(text, fm, 0, view, 0);
        echoCharsWidth = Utilities.getTabbedTextWidth(echoChars, fm, 0, view, 0);
        height = fm.getHeight();
        assertNotNull(text);
        jf.getContentPane().add(pf);
        pf.setForeground(Color.RED);
        pf.setSelectedTextColor(Color.GREEN);
        jf.setSize(200, 300);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        jf.dispose();
    }

    public void testGetPreferredSpan() {
        assertEquals(echoCharsWidth, (int) view.getPreferredSpan(View.X_AXIS));
        assertEquals(height, (int) view.getPreferredSpan(View.Y_AXIS));
        pf.setColumns(3);
        assertEquals(echoCharsWidth, (int) view.getPreferredSpan(View.X_AXIS));
        pf.setColumns(0);
        pf.setEchoChar(zero);
        assertEquals(textWidth, (int) view.getPreferredSpan(View.X_AXIS));
        assertEquals(height, (int) view.getPreferredSpan(View.Y_AXIS));
        pf.setColumns(3);
        assertEquals(textWidth, (int) view.getPreferredSpan(View.X_AXIS));
    }

    public void testDrawEchoCharacter() {
        char c = '&';
        int m_width = fm.charWidth(c);
        Graphics g = pf.getGraphics();
        Color color = g.getColor();
        assertEquals(x + m_width, view.drawEchoCharacter(g, x, y, c));
        assertEquals(color, g.getColor());
    }

    public void testDrawSelectedText() {
        Graphics g = pf.getGraphics();
        Color color = g.getColor();
        try {
            assertEquals(echoCharsWidth + x, view.drawSelectedText(g, x, y, 0, length));
            pf.setEchoChar(zero);
            assertEquals(textWidth, view.drawSelectedText(g, 0, y, 0, length));
        } catch (BadLocationException e) {
            assertTrue("Unexpected exception: ", false);
        }
        if (isHarmony()) {
            assertEquals(color, g.getColor());
        }
    }

    public void testDrawUnselectedText() {
        Graphics g = pf.getGraphics();
        Color old = g.getColor();
        try {
            assertEquals(echoCharsWidth + x, view.drawUnselectedText(g, x, y, 0, length));
            pf.setEchoChar(zero);
            assertEquals(textWidth, view.drawSelectedText(g, 0, y, 0, length));
        } catch (BadLocationException e) {
            assertTrue("Unexpected exception: ", false);
        }
        if (isHarmony()) {
            assertEquals(old, g.getColor());
        }
    }

    public void testModelToView() throws Exception {
        Shape shape = new Rectangle(2, 20, 30, 40);
        Rectangle adjustRect = view.adjustAllocation(shape).getBounds();
        Rectangle rect = (Rectangle) view.modelToView(1, shape, Position.Bias.Forward);
        assertEquals(adjustRect.x + fm.stringWidth("*"), rect.x);
        assertEquals(adjustRect.y, rect.y);
        assertEquals(1, rect.width);
        assertEquals(height, rect.height);
        rect = (Rectangle) view.modelToView(5, shape, Position.Bias.Forward);
        assertEquals(adjustRect.x + fm.stringWidth("*****"), rect.x);
        assertEquals(adjustRect.y, rect.y);
        assertEquals(1, rect.width);
        assertEquals(height, rect.height);
        try {
            view.modelToView(-1000, shape, Position.Bias.Forward);
            if (isHarmony()) {
                fail("Should produce am exception");
            }
        } catch (BadLocationException ble) {
        }
    }

    public void testViewToModel() {
        Shape shape = new Rectangle(10, 20, 30, 40);
        int px = 10 + fm.stringWidth("***");
        int py = 20 + fm.getAscent() - 1;
        Position.Bias[] biasRet = new Position.Bias[1];
        assertEquals(3, view.viewToModel(px, py, shape, biasRet));
        px = 10 + fm.stringWidth("****");
        py = 20 + fm.getAscent() - 1;
        biasRet = new Position.Bias[1];
        assertEquals(4, view.viewToModel(px, py, shape, biasRet));
    }
}
