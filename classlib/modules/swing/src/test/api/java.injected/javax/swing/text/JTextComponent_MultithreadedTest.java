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
import javax.swing.BasicSwingTestCase;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWaitTestCase;
import junit.framework.TestCase;

public class JTextComponent_MultithreadedTest extends TestCase {
    JFrame jf;

    JTextArea jtc;

    boolean bWasException;

    String s;

    String sRTL = "\u05DC";

    String sLTR = "\u0061";

    JTextField jep;

    Rectangle rect;

    @Override
    protected void setUp() throws Exception {
        jf = new JFrame();
        bWasException = false;
        s = null;
        jtc = new JTextArea("just test");
        jf.getContentPane().add(jtc);
        jf.setLocation(200, 300);
        jf.setSize(300, 200);
        jf.pack();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testReplaceSelection() throws Exception {
        Runnable test = new Runnable() {
            public void run() {
                jtc.setText("JTextComponent");
                jtc.select(5, 8);
            }
        };
        SwingUtilities.invokeAndWait(test);
        jtc.replaceSelection(null);
        assertEquals("JTextponent", jtc.getText());
        assertEquals("pon", jtc.getSelectedText());
        jtc.replaceSelection("XXX");
        assertEquals("XXX", jtc.getSelectedText());
        assertEquals("JTextXXXent", jtc.getText());
        setCaretsAlwaysUpdatePolicy();
        jtc.replaceSelection("XXX");
        assertEquals("JTextXXXent", jtc.getText());
        assertEquals(8, jtc.getCaretPosition());
        Runnable test1 = new Runnable() {
            public void run() {
                jtc.setText("JTextComponent");
                jtc.setCaretPosition(2);
            }
        };
        SwingUtilities.invokeAndWait(test1);
        jtc.replaceSelection("XXX");
        assertNull(jtc.getSelectedText());
        assertEquals("JTXXXextComponent", jtc.getText());
    }

    private void setCaretsAlwaysUpdatePolicy() {
        DefaultCaret caret = (DefaultCaret) jtc.getCaret();
        if (BasicSwingTestCase.isHarmony()) {
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        } else {
            caret.setAsynchronousMovement(true);
        }
    }

    public void testModelToView() throws Exception {
        Runnable test = new Runnable() {
            public void run() {
                jtc.setText(sLTR + sRTL + sLTR + sRTL + sLTR);
            }
        };
        SwingUtilities.invokeAndWait(test);
        Rectangle sample = null;
        rect = null;
        try {
            rect = jtc.modelToView(2);
            sample = jtc.getUI().modelToView(jtc, 2);
            assertNotNull(sample);
            assertNotNull(rect);
            assertEquals(sample, rect);
            rect = jtc.modelToView(3);
            sample = jtc.getUI().modelToView(jtc, 3);
        } catch (BadLocationException e) {
            assertFalse("Unexpected exception: " + e.getMessage(), true);
        }
        assertNotNull(sample);
        assertNotNull(rect);
        assertEquals(sample, rect);
    }

    public void testViewToModel() throws Exception {
        Runnable test = new Runnable() {
            public void run() {
                jtc.setText("test View To Model");
            }
        };
        SwingUtilities.invokeAndWait(test);
        Rectangle r = null;
        try {
            r = jtc.modelToView(2);
            assertNotNull(r);
            assertEquals(2, jtc.viewToModel(new Point(r.x, r.y)));
            r = jtc.modelToView(4);
            assertNotNull(r);
            assertEquals(4, jtc.viewToModel(new Point(r.x, r.y)));
        } catch (BadLocationException e) {
        }
    }

    void scrollableIncrementTest(final JTextComponent jtc, final Rectangle rect) {
        assertEquals(rect.width / 10, jtc.getScrollableUnitIncrement(rect,
                SwingConstants.HORIZONTAL, -1));
        assertEquals(rect.width / 10, jtc.getScrollableUnitIncrement(rect,
                SwingConstants.HORIZONTAL, 0));
        assertEquals(rect.width / 10, jtc.getScrollableUnitIncrement(rect,
                SwingConstants.HORIZONTAL, 1));
        assertEquals(rect.height / 10, jtc.getScrollableUnitIncrement(rect,
                SwingConstants.VERTICAL, -1));
        assertEquals(rect.height / 10, jtc.getScrollableUnitIncrement(rect,
                SwingConstants.VERTICAL, 0));
        assertEquals(rect.height / 10, jtc.getScrollableUnitIncrement(rect,
                SwingConstants.VERTICAL, 1));
        assertEquals(rect.width, jtc.getScrollableBlockIncrement(rect,
                SwingConstants.HORIZONTAL, -1));
        assertEquals(rect.width, jtc.getScrollableBlockIncrement(rect,
                SwingConstants.HORIZONTAL, 0));
        assertEquals(rect.width, jtc.getScrollableBlockIncrement(rect,
                SwingConstants.HORIZONTAL, 1));
        assertEquals(rect.height, jtc.getScrollableBlockIncrement(rect,
                SwingConstants.VERTICAL, -1));
        assertEquals(rect.height, jtc.getScrollableBlockIncrement(rect,
                SwingConstants.VERTICAL, 0));
        assertEquals(rect.height, jtc.getScrollableBlockIncrement(rect,
                SwingConstants.VERTICAL, 1));
    }

    private String bigString(final String s, final int k) {
        String str = "";
        for (int i = 0; i < k; i++) {
            str += s;
        }
        return str;
    }

    public void testScrollable() throws Exception {
        jf.dispose();
        jf = new JFrame();
        jep = new JTextField();
        jf.getContentPane().add(jep);
        jf.setLocation(200, 300);
        jf.setSize(500, 500);
        Runnable test = new Runnable() {
            public void run() {
                jep
                        .setText(bigString(
                                (bigString("a", 10) + bigString("\u05DC", 10) + "\n"), 10));
            }
        };
        SwingUtilities.invokeAndWait(test);
        if (!BasicSwingTestCase.isHarmony()) {
            jf.setVisible(true);
            SwingWaitTestCase.isRealized(jf);
        } else {
            jf.pack();
        }
        assertFalse(jep.getScrollableTracksViewportHeight());
        assertFalse(jep.getScrollableTracksViewportWidth());
        assertEquals(jep.getPreferredSize(), jep.getPreferredScrollableViewportSize());
        Rectangle rect = null;
        try {
            rect = jep.modelToView(20);
            assertNotNull(rect);
            scrollableIncrementTest(jep, rect);
            rect = jep.modelToView(101);
        } catch (BadLocationException e) {
            assertFalse("Unexpected exception :" + e.getMessage(), true);
        }
        scrollableIncrementTest(jep, rect);
        rect.x = rect.x + 2;
        scrollableIncrementTest(jep, rect);
        rect.y = rect.y + 14;
        rect.x = rect.x - 2;
        scrollableIncrementTest(jep, rect);
        rect.height = rect.height + 6;
        scrollableIncrementTest(jep, rect);
        rect.height = rect.height - 10;
        scrollableIncrementTest(jep, rect);
        rect.width = rect.width + 3;
        scrollableIncrementTest(jep, rect);
        rect.width = rect.width - 7;
        scrollableIncrementTest(jep, rect);
        rect.x = rect.x + 1000;
        rect.y = rect.y + 4000;
        rect.height = rect.height + 1013;
        scrollableIncrementTest(jep, rect);
        rect.height = rect.height - 3013;
        scrollableIncrementTest(jep, rect);
        rect.width = rect.width + 5011;
        scrollableIncrementTest(jep, rect);
        rect.width = rect.width - 8011;
        scrollableIncrementTest(jep, rect);
        try {
            jep.getScrollableBlockIncrement(rect, 4, 1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid orientation: 4", s);
        bWasException = false;
        s = null;
        try {
            jep.getScrollableUnitIncrement(rect, 4, 1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid orientation: 4", s);
    }
}