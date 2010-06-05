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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.EventListener;
import javax.swing.ExtJTextArea;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingTestCase;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.Position.Bias;

public class DefaultCaretTest extends SwingTestCase {
    JTextArea jta = null;

    JFrame jf = null;

    DefaultCaret dc = null;

    Rectangle r1, r2, r3, r4, r5, r6, r;

    int tCompListenersCount;

    //int tmp;
    Point p;

    Highlighter.HighlightPainter pnt;

    AbstractDocument ad;

    boolean bWasException = false;

    String s = null;

    String fireTest = "";

    String sRTL = new String("\u05DC");

    String sLTR = new String("\u0061");

    Color c1 = new Color(234);

    Color c2 = new Color(235);

    SimpleNavigationFilter filter;

    SimpleChangeListener CHL;

    class SimpleChangeListenerForFire implements ChangeListener {
        int flag = 0;

        String s1;

        SimpleChangeListenerForFire(final String s) {
            s1 = s;
        }

        public void stateChanged(final ChangeEvent e) {
            flag = 1;
            fireTest = fireTest + s1;
            //System.out.println("state changed");
        }
    }

    class SimpleChangeListener implements ChangeListener {
        int flag = 0;

        public void stateChanged(final ChangeEvent e) {
            flag = 1;
            //System.out.println("state changed");
        }
    }

    class SimpleCaretListener implements CaretListener {
        public void caretUpdate(final CaretEvent e) {
            System.out.println("caret update");
        }
    }

    class SimpleNavigationFilter extends NavigationFilter {
        int flagSetDot = 0;

        int flagMoveDot = 0;

        @Override
        public void setDot(final FilterBypass f, final int i, final Bias b) {
            flagSetDot = 1;
            super.setDot(f, i, b);
        }

        @Override
        public void moveDot(final FilterBypass arg0, final int i, final Bias b) {
            flagMoveDot = 1;
            super.moveDot(arg0, i, b);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jta = new ExtJTextArea("JTextArea for DefaultCaret Testing\n***\n*%%%**");
        dc = new DefaultCaret();
        jf = new JFrame();
        p = new Point(5, 16);
        bWasException = false;
        s = null;
        r = null;
        r1 = null;
        r2 = null;
        r3 = null;
        r4 = null;
        r5 = null;
        r6 = null;
        ad = (AbstractDocument) jta.getDocument();
        jf.getContentPane().add(jta);
        jta.getCaret().deinstall(jta);
        tCompListenersCount = jta.getListeners(MouseListener.class).length
                + jta.getListeners(MouseMotionListener.class).length
                + jta.getListeners(FocusListener.class).length
                + jta.getPropertyChangeListeners().length;
        jta.setCaret(dc);
        jf.setLocation(100, 100);
        jf.setSize(350, 200);
        jf.pack();
        init();
    }

    public void init() {
        try {
            r1 = jta.modelToView(6);
            r2 = jta.modelToView(10);
            r3 = jta.modelToView(15);
            r4 = jta.modelToView(2);
            TextUI textUI = jta.getUI();
            r5 = textUI.modelToView(jta, 5, Position.Bias.Backward);
            r6 = textUI.modelToView(jta, 7, Position.Bias.Forward);
            assertNotNull(r1);
            assertNotNull(r2);
            assertNotNull(r3);
            assertNotNull(r4);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception " + s, bWasException);
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    /*
     * If or not elem in list
     *
     */
    boolean findListener(final EventListener[] list, final EventListener elem) {
        if (list == null) {
            return false;
        }
        for (int i = 0; i < list.length; i++) {
            if (list[i] == elem) {
                return true;
            }
        }
        return false;
    }

    /**
     * Constructor for DefaultCaretTest.
     *
     * @param arg0
     */
    public DefaultCaretTest(final String arg0) {
        super(arg0);
    }

    public void testDefaultCaret() {
        assertNotNull(dc);
    }

    public void testGetComponent() {
        assertEquals(jta, dc.getComponent());
    }

    /*
     * Tests that listeners was added to JTextComponent. Doesn't test that
     * DocumentListener was added to JTextComponent. Doesn't test that Dot and
     * Mark is 0.
     */
    public void testInstall() {
        int tCompListenersCountCurrent = jta.getListeners(MouseListener.class).length
                + jta.getListeners(MouseMotionListener.class).length
                + jta.getListeners(FocusListener.class).length
                + jta.getPropertyChangeListeners().length;
        assertEquals(tCompListenersCount, tCompListenersCountCurrent - 4);

        try { // Regression test for HARMONY-1750
            new DefaultCaret().install(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /*
     * Tests that listeners was removed from JTextComponent. Doesn't test that
     * Document listener was removed.
     */
    public void testDeinstall() throws Exception {
        int i = jta.getListeners(MouseListener.class).length;
        int j = jta.getListeners(MouseMotionListener.class).length;
        int k = jta.getListeners(FocusListener.class).length;
        int l = jta.getPropertyChangeListeners().length;
        dc.deinstall(jta);
        assertNull(dc.getComponent());
        assertEquals(i, jta.getListeners(MouseListener.class).length + 1);
        assertEquals(j, jta.getListeners(MouseMotionListener.class).length + 1);
        assertEquals(k, jta.getListeners(FocusListener.class).length + 1);
        assertEquals(l, jta.getPropertyChangeListeners().length + 1);
        assertFalse(findListener(jta.getListeners(MouseListener.class), dc));
        assertFalse(findListener(jta.getListeners(MouseListener.class), dc));
        assertFalse(findListener(jta.getListeners(FocusListener.class), dc));
    }

    /*
     * Tests methods IsVisible and SetVisible
     */
    public void testIsVisible() throws Exception {
        dc.setVisible(true);
        assertTrue("dc.isVisible()= false after" + " dc.setVisible(true)", dc.isVisible());
        dc.setVisible(false);
        assertFalse("dc.isVisible()= true after" + " dc.setVisible(false)", dc.isVisible());
    }

    /*
     * Tests methods isSelectionVisible and setSelectionVisible
     */
    public void testIsSelectionVisible() throws Exception {
        assertNotNull(dc);
        dc.setSelectionVisible(true);
        assertTrue("dc.isSelectionVisible()= false after" + " dc.setSelectionVisible(true)", dc
                .isSelectionVisible());
        dc.setSelectionVisible(false);
        assertFalse("dc.isSelectionVisible()= true after" + " dc.setSelectionVisible(false)",
                dc.isSelectionVisible());
    }

    public void testEquals() {
        assertFalse(dc.equals(new DefaultCaret()));
        assertTrue(dc.equals(dc));
        assertTrue(dc.equals(jta.getCaret()));
    }

    /*
     * Tests addChangeListener,removeChangeListener, getChangeListener
     */
    public void testChangeListeners() {
        try {
            SimpleChangeListener t1 = new SimpleChangeListener();
            SimpleChangeListener t2 = new SimpleChangeListener();
            SimpleChangeListener t3 = new SimpleChangeListener();
            dc.addChangeListener(t1);
            assertTrue(findListener(dc.getChangeListeners(), t1));
            assertTrue(findListener(dc.listenerList.getListeners(ChangeListener.class), t1));
            dc.addChangeListener(t2);
            assertTrue(findListener(dc.getChangeListeners(), t1));
            assertTrue(findListener(dc.listenerList.getListeners(ChangeListener.class), t1));
            assertTrue(findListener(dc.getChangeListeners(), t2));
            assertTrue(findListener(dc.listenerList.getListeners(ChangeListener.class), t2));
            dc.addChangeListener(t3);
            assertTrue(findListener(dc.getChangeListeners(), t1));
            assertTrue(findListener(dc.listenerList.getListeners(ChangeListener.class), t1));
            assertTrue(findListener(dc.getChangeListeners(), t2));
            assertTrue(findListener(dc.listenerList.getListeners(ChangeListener.class), t2));
            assertTrue(findListener(dc.getChangeListeners(), t3));
            assertTrue(findListener(dc.listenerList.getListeners(ChangeListener.class), t3));
            dc.removeChangeListener(t1);
            assertFalse(findListener(dc.getChangeListeners(), t1));
            assertFalse(findListener(dc.listenerList.getListeners(ChangeListener.class), t1));
            dc.removeChangeListener(t2);
            assertFalse(findListener(dc.getChangeListeners(), t1));
            assertFalse(findListener(dc.listenerList.getListeners(ChangeListener.class), t1));
            assertFalse(findListener(dc.getChangeListeners(), t2));
            assertFalse(findListener(dc.listenerList.getListeners(ChangeListener.class), t2));
            dc.removeChangeListener(t3);
            assertFalse(findListener(dc.getChangeListeners(), t1));
            assertFalse(findListener(dc.listenerList.getListeners(ChangeListener.class), t1));
            assertFalse(findListener(dc.getChangeListeners(), t2));
            assertFalse(findListener(dc.listenerList.getListeners(ChangeListener.class), t2));
            assertFalse(findListener(dc.getChangeListeners(), t3));
            assertFalse(findListener(dc.listenerList.getListeners(ChangeListener.class), t3));
        } catch (NullPointerException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
    }

    public void testGetBlinkRate() throws Exception {
        assertEquals(0, dc.getBlinkRate());
        dc.setBlinkRate(100);
        assertEquals(100, dc.getBlinkRate());

        try { // Regression test for HARMONY-1795
            dc.setBlinkRate(-1);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testSetDot() throws Exception {
        dc.setDot(8);
        dc.moveDot(10);
        dc.setDot(5);
        assertEquals(5, dc.getDot());
        assertEquals(5, dc.getMark());
    }

    public void testMoveDot() throws Exception {
        dc.setDot(4);
        dc.moveDot(9);
        assertEquals("tArea", jta.getSelectedText());
        dc.moveDot(11);
        assertEquals("tArea f", jta.getSelectedText());
    }

    public void testGetDot() throws Exception {
        jta.setSelectionStart(11);
        jta.setSelectionEnd(14);
        assertEquals(14, dc.getDot());
        assertEquals(11, dc.getMark());
    }

    void checkValues(final int dot, final Position.Bias dotBias) {
        assertEquals(dot, dc.getDot());
        assertEquals(dot, dc.getMark());
        assertEquals(dotBias, dc.getDotBias());
    }

    /*
     * Tryes to set caret position by Mouse Events (dot = mark)
     */
    public void testPositionCaret() throws Exception {
        Position.Bias bias[] = new Position.Bias[1];
        int tmp;
        jta.setText(sLTR + sRTL + sLTR + sRTL + sLTR + sRTL + sLTR + sRTL);
        //dc.setDot(8);
        //dc.moveDot(10);
        dc.positionCaret(new MouseEvent(jta, MouseEvent.MOUSE_CLICKED, 0, 0, jta.getX() + r1.x,
                jta.getY() + r1.y, 0, false));
        tmp = jta.getUI().viewToModel(jta, new Point(r1.x, r1.y), bias);
        checkValues(tmp, bias[0]);
        //checkValues(6,6,Position.Bias.Backward);
        dc.positionCaret(new MouseEvent(jta, MouseEvent.MOUSE_CLICKED, 0, 0, jta.getX() + r5.x,
                jta.getY() + r5.y, 0, false));
        tmp = jta.getUI().viewToModel(jta, new Point(r5.x, r5.y), bias);
        checkValues(tmp, bias[0]);
        //checkValues(4,4,Position.Bias.Forward);
        dc.positionCaret(new MouseEvent(jta, MouseEvent.MOUSE_CLICKED, 0, 0, jta.getX() + r6.x,
                jta.getY() + r6.y, 0, false));
        tmp = jta.getUI().viewToModel(jta, new Point(r6.x, r6.y), bias);
        checkValues(tmp, bias[0]);
        //checkValues(6,6,Position.Bias.Forward);
    }

    /*
     * Tryes to move caret position by Mouse Events (dot = mark)
     */
    public void testMoveCaret() throws Exception {
        dc.positionCaret(new MouseEvent(jta, MouseEvent.MOUSE_CLICKED, 0, 0, jta.getX() + r1.x,
                jta.getY() + r1.y, 0, false));
        dc.moveCaret(new MouseEvent(jta, MouseEvent.MOUSE_CLICKED, 0, 0, jta.getX() + r2.x, jta
                .getY()
                + r2.y, 0, false));
        assertEquals(10, dc.getDot());
        assertEquals(6, dc.getMark());
        assertEquals(10, jta.getCaretPosition());
        dc.moveCaret(new MouseEvent(jta, MouseEvent.MOUSE_CLICKED, 0, 0, jta.getX() + r3.x, jta
                .getY()
                + r3.y, 0, false));
        assertEquals(15, dc.getDot());
        assertEquals(6, dc.getMark());
        assertEquals(15, jta.getCaretPosition());
    }

    /*
     * Tests setMagicCaretPosition and getMagicCaretPosition
     */
    public void testSetMagicCaretPosition() throws Exception {
        dc.setMagicCaretPosition(p);
        assertTrue(dc.getMagicCaretPosition().equals(p));
    }

    // TODO: may be add other listeners (not ChangeListener)
    public void testGetListeners() {
        EventListener[] EvLList = null;
        SimpleChangeListener ChL1 = new SimpleChangeListener();
        SimpleChangeListener ChL2 = new SimpleChangeListener();
        //SimpleCaretListener CL1 = new SimpleCaretListener();
        //SimpleCaretListener CL2 = new SimpleCaretListener();
        dc.addChangeListener(ChL1);
        dc.addChangeListener(ChL2);
        EvLList = dc.getListeners(ChangeListener.class);
        assertTrue(findListener(EvLList, ChL1));
        assertTrue(findListener(EvLList, ChL2));
        //assertEquals(EvLList.length,2);
        if ((EvLList != null) & (dc.getChangeListeners() != null)) {
            assertEquals(EvLList.length, dc.getChangeListeners().length);
            //dc.addChangeListener(ChL1);
            //dc.addChangeListener(ChL2);
        }
    }

    /*
     * To tests adjustVisibility method class ExtJTextArea(ejta) is used. If it
     * was invoked ejta.scrollRectToVisible then ejta.flag would become 1.
     */
    public void testAdjustVisibility() throws Exception {
        try {
            r = jta.modelToView(8);
            if (r == null) {
                assertNull(r);
                return;
            }
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        ((ExtJTextArea) jta).flag = 0;
        dc.adjustVisibility(r1);
        assertEquals(1, ((ExtJTextArea) jta).flag);
    }

    // Regression for HARMONY-2780
    public void testAdjustVisibilityNull() {
        new DefaultCaret().adjustVisibility(null);
        // No exception is expected
    }

    public void testFireStateChanged() {
        SimpleChangeListenerForFire CHL1 = new SimpleChangeListenerForFire("L1");
        SimpleChangeListenerForFire CHL2 = new SimpleChangeListenerForFire("L2");
        SimpleChangeListenerForFire CHL3 = new SimpleChangeListenerForFire("L3");
        dc.addChangeListener(CHL1);
        dc.addChangeListener(CHL2);
        dc.addChangeListener(CHL3);
        assertEquals(0, CHL1.flag);
        assertEquals(0, CHL2.flag);
        assertEquals(0, CHL3.flag);
        assertEquals(fireTest, "");
        dc.fireStateChanged();
        assertEquals(1, CHL1.flag);
        assertEquals(1, CHL2.flag);
        assertEquals(1, CHL3.flag);
        assertEquals(fireTest, "L3L2L1");
    }

    public void testGetSelectionPainter() throws Exception {
        Highlighter.Highlight[] h = jta.getHighlighter().getHighlights();
        pnt = dc.getSelectionPainter();
        assertNotNull("DefaultCaret.getSelectionPainter()=null", pnt);
        if (pnt != null) {
            assertTrue(pnt instanceof DefaultHighlighter.DefaultHighlightPainter);
            try {
                jta.getHighlighter().addHighlight(4, 9, pnt);
            } catch (BadLocationException e) {
                bWasException = true;
                s = e.getMessage();
            }
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        if (pnt == null) {
            return;
        }
        h = jta.getHighlighter().getHighlights();
        assertEquals(1, h.length);
        assertEquals(h[0].getPainter(), dc.getSelectionPainter());
        assertEquals(4, h[0].getStartOffset());
        assertEquals(9, h[0].getEndOffset());
        
        // Regression for HARMONY-1768
        DefaultCaret obj = new DefaultCaret() {
            public  Highlighter.HighlightPainter getSelectionPainter(){
                return super.getSelectionPainter();
            }
        };
        assertNotNull(obj.getSelectionPainter());
    }

    public void testFocusGained() throws Exception {
        jta.setEditable(true);
        dc.focusGained(new FocusEvent(jta, FocusEvent.FOCUS_GAINED));
        assertTrue(dc.isVisible());
        dc.focusLost(new FocusEvent(jta, FocusEvent.FOCUS_LOST));
        jta.setEditable(false);
        dc.focusGained(new FocusEvent(jta, FocusEvent.FOCUS_GAINED));
        assertFalse(dc.isVisible());
        jta.setEditable(true);
        jta.setEnabled(false);
        dc.focusGained(new FocusEvent(jta, FocusEvent.FOCUS_GAINED));
        assertFalse(dc.isVisible());
    }

    public void testFocusLost() throws Exception {
        dc.focusLost(new FocusEvent(jta, FocusEvent.FOCUS_LOST));
        assertFalse(dc.isVisible());
    }

    public void testMouseDragged() throws Exception {
        dc.mousePressed(new MouseEvent(jta, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.BUTTON1_MASK, jta.getX() + r1.x, jta.getY() + r1.y, 0, false,
                MouseEvent.BUTTON1));
        dc.mouseDragged(new MouseEvent(jta, MouseEvent.MOUSE_DRAGGED, 0,
                InputEvent.BUTTON1_MASK, jta.getX() + r3.x, jta.getY() + r3.y, 0, false,
                MouseEvent.BUTTON1));
        assertEquals("rea for D", jta.getSelectedText());
        /*
         * dc.mousePressed(new MouseEvent(jta, MouseEvent.MOUSE_PRESSED, 0,
         * InputEvent.BUTTON1_MASK, jta.getX() + r4.x, jta .getY() + r4.y, 0,
         * false, MouseEvent.BUTTON1));
         */
        dc.mouseDragged(new MouseEvent(jta, MouseEvent.MOUSE_DRAGGED, 0,
                InputEvent.BUTTON1_MASK, jta.getX() + r4.x, jta.getY() + r4.y, 0, false,
                MouseEvent.BUTTON1));
        assertEquals("extA", jta.getSelectedText());
        /*
         * dc.mouseDragged(new MouseEvent(jta, MouseEvent.MOUSE_DRAGGED, 0,
         * InputEvent.BUTTON3_MASK, jta.getX() + r2.x, jta .getY() + r2.y, 0,
         * false)); try { assertEquals("JTextA", jta.getSelectedText()); } catch
         * (AssertionFailedError e) { e3 = e; }
         *
         * dc.mouseDragged(new MouseEvent(jta, MouseEvent.MOUSE_DRAGGED, 0,
         * InputEvent.BUTTON1_MASK, jta.getX() + r3.x, jta .getY() + r3.y, 0,
         * false)); try { assertEquals("JTextArea for D",
         * jta.getSelectedText()); } catch (AssertionFailedError e) { e4 = e; }
         */
    }

    public void testMouseClicked() throws Exception {
        dc.mouseClicked(new MouseEvent(jta, MouseEvent.MOUSE_CLICKED, 0,
                InputEvent.BUTTON1_DOWN_MASK, jta.getX() + r1.x, jta.getY() + r1.y, 2, false,
                MouseEvent.BUTTON1));
        assertEquals("JTextArea", jta.getSelectedText());
        dc.setDot(5);
        dc.mouseClicked(new MouseEvent(jta, MouseEvent.MOUSE_CLICKED, 0,
                InputEvent.BUTTON1_DOWN_MASK, jta.getX() + r1.x, jta.getY() + r1.y, 3, false,
                MouseEvent.BUTTON1));
        assertEquals("JTextArea for DefaultCaret Testing", jta.getSelectedText());
    }

    public void testMousePressed() throws Exception {
        dc.mousePressed(new MouseEvent(jta, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.SHIFT_DOWN_MASK, jta.getX() + r3.x, jta.getY() + r3.y, 0, false,
                MouseEvent.BUTTON1));
        assertEquals("JTextArea for D", jta.getSelectedText());
    }

    public void testDocInsChange_DotEqMark() throws Exception {
        try {
            dc.setDot(5);
            jta.getDocument().insertString(3, "insert", null);
            assertFalse("Unexpected exception: " + s, bWasException);
            assertEquals(11, dc.getDot());
            dc.setDot(5);
            jta.getDocument().insertString(2, "ins", null);
            assertFalse("Unexpected exception: " + s, bWasException);
            assertEquals(8, dc.getDot());
            dc.setDot(5);
            jta.getDocument().insertString(5, "inse", null);
            assertFalse("Unexpected exception: " + s, bWasException);
            assertEquals(9, dc.getDot());
            dc.setDot(5);
            jta.getDocument().insertString(7, "insert", null);
            assertEquals(5, dc.getDot());
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
    }

    public void testDocInsChange_DotNotEqMark() throws Exception {
        try {
            dc.setDot(11);
            dc.moveDot(6);
            jta.getDocument().insertString(2, "in", null);
            assertEquals(8, dc.getDot());
            assertEquals(13, dc.getMark());
            dc.setDot(11);
            dc.moveDot(6);
            jta.getDocument().insertString(6, "ins", null);
            assertEquals(9, dc.getDot());
            assertEquals(14, dc.getMark());
            dc.setDot(11);
            dc.moveDot(6);
            jta.getDocument().insertString(7, "inser", null);
            assertEquals(6, dc.getDot());
            assertEquals(16, dc.getMark());
            dc.setDot(6);
            dc.moveDot(11);
            jta.getDocument().insertString(11, "insert", null);
            assertEquals(17, dc.getDot());
            assertEquals(6, dc.getMark());
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
    }

    public void testDocRemoveChange_DotEqMark() throws Exception {
        dc.setDot(5);
        try {
            jta.getDocument().remove(3, 1);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertEquals(4, dc.getDot());
        dc.setDot(5);
        try {
            jta.getDocument().remove(2, 3);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertEquals(2, dc.getDot());
        dc.setDot(5);
        try {
            jta.getDocument().remove(3, 7);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertEquals(3, dc.getDot());
        dc.setDot(5);
        try {
            jta.getDocument().remove(5, 8);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertEquals(5, dc.getDot());
        dc.setDot(5);
        try {
            jta.getDocument().remove(7, 2);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertEquals(5, dc.getDot());
        assertFalse("Unexpected exception: " + s, bWasException);
    }

    public void testDocRemoveChange_DotNotEqMark() throws Exception {
        dc.setDot(11);
        dc.moveDot(6);
        try {
            jta.getDocument().remove(3, 2);
            assertEquals(4, dc.getDot());
            assertEquals(9, dc.getMark());
            dc.setDot(11);
            dc.moveDot(6);
            jta.getDocument().remove(3, 4);
            assertEquals(3, dc.getDot());
            assertEquals(7, dc.getMark());
            dc.setDot(11);
            dc.moveDot(6);
            jta.getDocument().remove(6, 2);
            assertEquals(6, dc.getDot());
            assertEquals(9, dc.getMark());
            dc.setDot(6);
            dc.moveDot(11);
            jta.getDocument().remove(10, 7);
            assertEquals(10, dc.getDot());
            assertEquals(6, dc.getMark());
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
    }

    /*
     * for 1.5.0
     *
     */
    public void testDocNeverUpdate() throws Exception {
        dc.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        dc.setDot(6);
        try {
            jta.getDocument().remove(3, 10);
            assertEquals(6, dc.getDot());
            dc.setDot(11);
            dc.moveDot(6);
            jta.getDocument().remove(3, 10);
            assertEquals(6, dc.getDot());
            assertEquals(11, dc.getMark());
            dc.setDot(6);
            jta.getDocument().remove(2, 20);
            assertEquals(5, dc.getDot());
            dc.setDot(3);
            dc.moveDot(1);
            jta.getDocument().remove(2, 3);
            assertEquals(1, dc.getDot());
            assertEquals(2, dc.getMark());
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
    }

    /*
     * for 1.5.0
     *
     */
    public void testConstants() {
        assertEquals(DefaultCaret.ALWAYS_UPDATE, 2);
        assertEquals(DefaultCaret.NEVER_UPDATE, 1);
        assertEquals(DefaultCaret.UPDATE_WHEN_ON_EDT, 0);
    }

    /*
     * for 1.5.0
     *
     */
    public void testSetGetUpdatePolicy() {
        dc.setUpdatePolicy(0);
        assertEquals(0, dc.getUpdatePolicy());
        dc.setUpdatePolicy(1);
        assertEquals(1, dc.getUpdatePolicy());
        dc.setUpdatePolicy(2);
        assertEquals(2, dc.getUpdatePolicy());
        //According to documentation there it was Exception
        //but 1.5 never produces it
        try {
            dc.setUpdatePolicy(3);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            s = e.getMessage();
        }
        if (isHarmony()) {
            assertTrue("Not thrown Expected exception: " + s, bWasException);
        }
    }

    public void testCaretColor() throws Exception {
        //TODO Runnable
        jta.setSelectionColor(c1);
        jta.setCaretColor(c2);
        assertEquals(c1, jta.getSelectionColor());
        assertEquals(c2, jta.getCaretColor());
    }

    void checkFlag(final int i1) {
        assertEquals(i1, CHL.flag);
        CHL.flag = 0;
    }

    public void testInvokeFireStateChanged() throws Exception {
        CHL = new SimpleChangeListener();
        dc.addChangeListener(CHL);
        dc.setDot(5);
        checkFlag(1);
        dc.moveDot(8);
        checkFlag(1);
        dc.setVisible(true);
        checkFlag(0);
        dc.setVisible(false);
        checkFlag(0);
        dc.setSelectionVisible(true);
        checkFlag(0);
        dc.setSelectionVisible(false);
        checkFlag(0);
        jta.setCaretColor(new Color(235));
        checkFlag(0);
        jta.setCaretColor(new Color(235));
        checkFlag(0);
        jta.setSelectionColor(new Color(235));
        checkFlag(0);
        dc.setBlinkRate(100);
        checkFlag(0);
        jta.setDocument(new PlainDocument());
        checkFlag(1);
        jta.getCaret().deinstall(jta);
        checkFlag(0);
        dc.install(new JTextArea("JTextArea"));
        checkFlag(0);
    }

    public void testToString() throws Exception {
        dc.setDot(4);
        dc.moveDot(9);
        assertEquals("Dot=(9, Forward) Mark=(4, Forward)", dc.toString());
    }

    //Serialization is not supported now by Swing
    /*public void testSerialization() throws Exception {

     dc.setDot(4, Position.Bias.Backward);
     dc.setBlinkRate(100);
     dc.setVisible(true);
     dc.setSelectionVisible(true);
     dc.setMagicCaretPosition(new Point(200, 300));

     DefaultCaret dc1 = new DefaultCaret();

     try {
     FileOutputStream fo = new FileOutputStream("tmp");
     ObjectOutputStream so = new ObjectOutputStream(fo);
     so.writeObject(dc);
     so.flush();
     so.close();
     FileInputStream fi = new FileInputStream("tmp");
     ObjectInputStream si = new ObjectInputStream(fi);
     dc1 = (DefaultCaret) si.readObject();
     si.close();
     } catch (Exception e) {
     assertTrue("seralization failed" + e.getMessage(),false);
     }

     assertEquals("Dot=(4, Backward) Mark=(4, Backward)", dc1.toString());
     assertTrue(jta.equals(dc.getComponent()));
     assertTrue(dc1.isSelectionVisible());
     assertEquals(100, dc1.getBlinkRate());
     assertTrue(dc1.getMagicCaretPosition().equals(new Point(200, 300)));
     assertTrue(dc1.getSelectionPainter() instanceof DefaultHighlighter.DefaultHighlightPainter);
     assertTrue(dc1.listenerList.toString().equals(
     dc.listenerList.toString()));
     assertNull(dc1.changeEvent);

     } */
    /*
     * for 1.4.2
     */
    /*
     * public void testSetGetAsynchronousMovement(){
     * assertFalse(dc.getAsynchronousMovement());
     * dc.setAsynchronousMovement(true);
     * assertTrue(dc.getAsynchronousMovement()); }
     */
    public void testNavigationFilter() throws Exception {
        assertNull(jta.getNavigationFilter());
        filter = new SimpleNavigationFilter();
        jta.setNavigationFilter(filter);
        dc.setDot(3);
        dc.moveDot(4);
        assertEquals(1, filter.flagMoveDot);
        assertEquals(1, filter.flagSetDot);
    }

    //TODO
    public void testIsActive() throws Exception {
        dc.setVisible(true);
        assertTrue(dc.isActive());
        dc.setVisible(false);
        assertFalse(dc.isActive());
    }

    public void test3820() {
        DefaultCaret dc = new DefaultCaret();
        assertEquals(0, dc.getChangeListeners().length);
        dc.addChangeListener(null);
        assertEquals(0, dc.getChangeListeners().length);
    }

    public void test4208() {
        dc.setSelectionVisible(false);
        jta.selectAll();
        assertEquals(0, jta.getHighlighter().getHighlights().length);
        dc.setSelectionVisible(true);
        assertEquals(1, jta.getHighlighter().getHighlights().length);
    }
}


