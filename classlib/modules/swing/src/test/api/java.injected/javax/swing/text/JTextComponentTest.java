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
import java.awt.ComponentOrientation;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Vector;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingTestCase;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

public class JTextComponentTest extends SwingTestCase {
    JFrame jf;

    JTextArea jtc;

    SimplePropertyChangeListener pChListener;

    boolean bWasException;

    String s;

    Color color;

    String strOrderFireCaretUpdate;

    String strOrderProcessInputMethodEventCaret;

    String strOrderProcessInputMethodEventText;

    String sRTL = "\u05DC";

    String sLTR = "\u0061";

    NavigationFilter navFilter;

    JTextComp jtComp;

    AbstractDocument docXXX;

    JTextField jep;

    Robot robot;

    Rectangle rect;

    String pattern = "@[^,}]*";

    class SimpleInputMethodListener implements InputMethodListener {
        String name;

        public SimpleInputMethodListener(final String s) {
            name = s;
        }

        public void caretPositionChanged(final InputMethodEvent e) {
            strOrderProcessInputMethodEventCaret += name;
        }

        public void inputMethodTextChanged(final InputMethodEvent e) {
            strOrderProcessInputMethodEventText += name;
        }
    }

    void isElement(final Object[] a, final Object b, final int count) {
        assertNotNull(a);
        boolean cond = false;
        int k = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == b) {
                cond = true;
                k += 1;
            }
        }
        assertTrue(cond);
        assertEquals(count, k);
    }

    void assertEqualsPropertyChangeEvent(final String name, final Object oldValue,
            final Object newValue, final PropertyChangeEvent e) {
        assertEquals(name, e.getPropertyName());
        assertEquals(oldValue, e.getOldValue());
        assertEquals(newValue, e.getNewValue());
    }

    class SimpleKeyMap implements Keymap {
        String name;

        Keymap parent;

        public SimpleKeyMap(final String s) {
            name = s;
        }

        public void addActionForKeyStroke(final KeyStroke k, final Action a) {
        }

        public Action getAction(final KeyStroke k) {
            return null;
        }

        public Action[] getBoundActions() {
            return null;
        }

        public Action getDefaultAction() {
            return null;
        }

        public KeyStroke[] getKeyStrokesForAction(final Action arg0) {
            return null;
        }

        public Keymap getResolveParent() {
            return parent;
        }

        public KeyStroke[] getBoundKeyStrokes() {
            return null;
        }

        public void removeBindings() {
        }

        public String getName() {
            return name;
        }

        public void setDefaultAction(final Action arg0) {
        }

        public void setResolveParent(final Keymap keymap) {
            parent = keymap;
        }

        public boolean isLocallyDefined(final KeyStroke arg0) {
            return false;
        }

        public void removeKeyStrokeBinding(final KeyStroke arg0) {
        }
    }

    class JTextComp extends JTextComponent {
        private static final long serialVersionUID = 1L;

        String UIClassId = "TextCompUIFirst";

        @Override
        public String getUIClassID() {
            return (UIClassId != null) ? UIClassId : "TextCompUIFirst";
        }
    }

    class SimpleCaretListener implements CaretListener {
        String name;

        SimpleCaretListener(final String s) {
            name = s;
        }

        public void caretUpdate(final CaretEvent ce) {
            strOrderFireCaretUpdate = strOrderFireCaretUpdate + name;
        }
    }

    class SimplePropertyChangeListener implements PropertyChangeListener {
        PropertyChangeEvent event;

        public void propertyChange(final PropertyChangeEvent e) {
            if (e.getPropertyName() != "ancestor") {
                event = e;
            }
        }

        PropertyChangeEvent getEvent() {
            PropertyChangeEvent e = event;
            event = null;
            return e;
        }
    }

    class SimpleTransferHandler extends TransferHandler {
        private static final long serialVersionUID = 1L;
    }

    private class SimpleTextAction extends TextAction {
        private static final long serialVersionUID = 1L;

        public SimpleTextAction(final String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent e) {
        }
    }

    public JTextComponentTest() {
        setIgnoreNotImplemented(true);
    }

    @Override
    protected void setUp() throws Exception {
        jf = new JFrame();
        bWasException = false;
        s = null;
        navFilter = new NavigationFilter();
        strOrderFireCaretUpdate = "";
        strOrderProcessInputMethodEventCaret = "";
        strOrderProcessInputMethodEventText = "";
        pChListener = new SimplePropertyChangeListener();
        color = new Color(15, 15, 16);
        jtc = new JTextArea();
        jtc.addPropertyChangeListener(pChListener);
        jf.getContentPane().add(jtc);
        jf.setLocation(200, 300);
        jf.setSize(300, 200);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testRemoveNotify() throws Exception {
    }

    public void testGetToolTipTextMouseEvent() throws Exception {
        jf.dispose();
        jf = new JFrame();
        jtc = new JTextArea("just for prefSize");
        jf.getContentPane().add(jtc);
        jf.setVisible(true);
        jf.pack();
        rect = null;
        try {
            rect = jtc.modelToView(0);
        } catch (BadLocationException e) {
        }
        assertNotNull(rect);
        MouseEvent me = new MouseEvent(jtc, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.BUTTON1_MASK, jtc.getX() + rect.x, jtc.getY() + rect.y, 0, false,
                MouseEvent.BUTTON1);
        assertEquals(jtc.getUI().getToolTipText(jtc, new Point(rect.x, rect.y)), jtc
                .getToolTipText(me));
        String s = "ToolTipText";
        jtc.setToolTipText(s);
        assertEquals(s, jtc.getToolTipText(me));
    }

    public void testSetGetNavigationFilter() throws Exception {
        assertNull(jtc.getNavigationFilter());
        jtc.setNavigationFilter(navFilter);
    }

    public void testSetGetKeymap() throws Exception {
        Keymap keyMap1 = jtc.getKeymap();
        assertNotNull(keyMap1);
        SimpleKeyMap keyMap2 = new SimpleKeyMap("Second");
        jtc.setKeymap(keyMap2);
        assertEqualsPropertyChangeEvent("keymap", keyMap1, keyMap2, pChListener.event);
        assertEquals(keyMap2, jtc.getKeymap());
    }

    public void testSetGetHighlighter() throws Exception {
        DefaultHighlighter dh1 = (DefaultHighlighter) jtc.getHighlighter();
        DefaultHighlighter dh2 = new DefaultHighlighter();
        assertTrue(jtc.getHighlighter() instanceof DefaultHighlighter);
        jtc.setHighlighter(dh2);
        assertEqualsPropertyChangeEvent("highlighter", dh1, dh2, pChListener.event);
        assertEquals(dh2, jtc.getHighlighter());
    }

    public void testSetGetDocument() throws Exception {
        assertNotNull(jtc.getDocument());
        jtc.setText("testSetGetDocument");
        Highlighter highlighter = jtc.getHighlighter();
        Highlighter.HighlightPainter painter = new Highlighter.HighlightPainter() {
            public void paint(Graphics g, int p1, int p2, Shape shape, JTextComponent c) {
            }
        };
        highlighter.addHighlight(0, 3, painter);
        highlighter.addHighlight(2, 5, painter);
        Document oldDoc = jtc.getDocument();
        PlainDocument doc = new PlainDocument();
        jtc.setDocument(doc);
        if (isHarmony()) {
            assertEquals(0, jtc.getHighlighter().getHighlights().length);
        }
        assertEqualsPropertyChangeEvent("document", oldDoc, doc, pChListener.event);
        assertEquals(doc, jtc.getDocument());
    }

    public void testSetGetCaret() throws Exception {
        DefaultCaret dc1 = (DefaultCaret) jtc.getCaret();
        DefaultCaret dc2 = new DefaultCaret();
        assertTrue(jtc.getCaret() instanceof DefaultCaret);
        jtc.setCaret(dc2);
        assertEqualsPropertyChangeEvent("caret", dc1, dc2, pChListener.event);
        assertEquals(dc2, jtc.getCaret());
    }

    public void assertEquals(final Vector<SimpleCaretListener> listeners1, final CaretListener[] listeners2) {
        assertNotNull(listeners1);
        assertNotNull(listeners2);
        assertEquals(listeners1.size(), listeners2.length);
        for (int i = 0; i < listeners1.size(); i++) {
            assertEquals(listeners1.get(i), listeners2[i]);
        }
    }

    public void testCaretListeners() {
        jtc.addCaretListener(null);
        SimpleCaretListener listener1 = new SimpleCaretListener("1");
        SimpleCaretListener listener2 = new SimpleCaretListener("2");
        SimpleCaretListener listener3 = new SimpleCaretListener("3");
        Vector<SimpleCaretListener> listeners = new Vector<SimpleCaretListener>();
        jtc.addCaretListener(listener1);
        listeners.add(listener1);
        assertEquals(listeners, jtc.getCaretListeners());
        jtc.addCaretListener(listener2);
        listeners.add(0, listener2);
        assertEquals(listeners, jtc.getCaretListeners());
        jtc.addCaretListener(listener3);
        listeners.add(0, listener3);
        assertEquals(listeners, jtc.getCaretListeners());
        jtc.removeCaretListener(listener2);
        listeners.remove(listener2);
        assertEquals(listeners, jtc.getCaretListeners());
        jtc.removeCaretListener(listener3);
        listeners.remove(listener3);
        assertEquals(listeners, jtc.getCaretListeners());
        jtc.removeCaretListener(listener1);
        listeners.remove(listener1);
        assertEquals(listeners, jtc.getCaretListeners());
    }

    public void testFireCaretUpdate() throws Exception {
        jtc.setText("JTextComponent");
        SimpleCaretListener listener1 = new SimpleCaretListener("1");
        SimpleCaretListener listener2 = new SimpleCaretListener("2");
        SimpleCaretListener listener3 = new SimpleCaretListener("3");
        jtc.addCaretListener(listener1);
        jtc.addCaretListener(listener2);
        jtc.addCaretListener(listener3);
        jtc.addCaretListener(listener1);
        assertEquals(strOrderFireCaretUpdate, "");
        jtc.setCaretPosition(5);
        assertEquals("1321", strOrderFireCaretUpdate);
    }

    // Regression for HARMONY-2819
    public void testFireCaretUpdateNull() throws Exception {
        new JTextArea().fireCaretUpdate(null);
        // no exception is expected
    }

    public void testSetGetText() throws Exception {
        assertTrue(jtc.getText().equals(""));
        jtc.setText("JTextComponent");
        assertTrue(jtc.getText().equals("JTextComponent"));
        String str = "";
        try {
            str = jtc.getText(5, 3);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertFalse("Unexpected exception: " + s, bWasException);
        assertTrue(str.equals("Com"));
    }

    public void testReplaceSelection() throws Exception {
        jtc.setText("JTextComponent");
        jtc.select(5, 8);
        jtc.replaceSelection(null);
        assertEquals("JTextponent", jtc.getText());
        assertNull(jtc.getSelectedText());
        jtc.select(5, 8);
        jtc.replaceSelection("XXX");
        assertNull(jtc.getSelectedText());
        assertEquals("JTextXXXent", jtc.getText());
        jtc.setText("JTextComponent");
        jtc.setCaretPosition(2);
        jtc.replaceSelection("XXX");
        assertNull(jtc.getSelectedText());
        assertEquals("JTXXXextComponent", jtc.getText());
    }

    public void testReadWrite() throws Exception {
        String s = "JTextComponent\nRead\nWrite\n";
        String sProperty = "CurrentStreamDescriptionProperty";
        StringWriter writer = new StringWriter();
        jtc.setText(s);
        try {
            jtc.write(writer);
        } catch (IOException e) {
        }
        jtc.setText("temporary");
        assertEquals("temporary", jtc.getText());
        StringReader reader = new StringReader(writer.toString());
        try {
            jtc.read(reader, sProperty);
        } catch (IOException e) {
        }
        assertEquals(s, jtc.getText());
        assertEquals(sProperty, jtc.getDocument().getProperty(
                Document.StreamDescriptionProperty));
        assertTrue(jtc.getDocument() instanceof PlainDocument);
    }

    public void testAddInputMethodListener() {
        SimpleInputMethodListener listener1 = new SimpleInputMethodListener("1");
        SimpleInputMethodListener listener2 = new SimpleInputMethodListener("2");
        SimpleInputMethodListener listener3 = new SimpleInputMethodListener("3");
        jtc.addInputMethodListener(listener1);
        jtc.addInputMethodListener(listener2);
        jtc.addInputMethodListener(listener3);
        InputMethodListener listeners[] = jtc.getInputMethodListeners();
        assertEquals(listener1, listeners[0]);
        assertEquals(listener2, listeners[1]);
        assertEquals(listener3, listeners[2]);
        assertEquals(3, listeners.length);
    }

    void imEventTest(final int id, final String s1, final String s2) {
        InputMethodEvent event = new InputMethodEvent(jtc, id, null, null);
        jtc.processInputMethodEvent(event);
        assertEquals(s1, strOrderProcessInputMethodEventCaret);
        assertEquals(s2, strOrderProcessInputMethodEventText);
        strOrderProcessInputMethodEventCaret = "";
        strOrderProcessInputMethodEventText = "";
    }

    public void testProcessInputMethodEventInputMethodEvent() throws Exception {
        SimpleInputMethodListener listener1 = new SimpleInputMethodListener("1");
        SimpleInputMethodListener listener2 = new SimpleInputMethodListener("2");
        SimpleInputMethodListener listener3 = new SimpleInputMethodListener("3");
        jtc.addInputMethodListener(listener1);
        jtc.addInputMethodListener(listener2);
        jtc.addInputMethodListener(listener3);
        TextHitInfo textHitInfo = TextHitInfo.afterOffset(0);
        assertNotNull(textHitInfo);
        jtc.setText("JTextComponent");
        jtc.enableInputMethods(true);
        imEventTest(InputMethodEvent.CARET_POSITION_CHANGED, "123", "");
        imEventTest(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, "", "123");
        imEventTest(InputMethodEvent.INPUT_METHOD_FIRST, "", "123");
        imEventTest(InputMethodEvent.INPUT_METHOD_LAST, "123", "");
    }

    public void testSetGetMargin() throws Exception {
        Insets insets1 = new Insets(0, 0, 0, 0);
        assertEquals(insets1, jtc.getMargin());
        Insets insets2 = new Insets(10, 20, 30, 40);
        jtc.setMargin(insets2);
        assertEqualsPropertyChangeEvent("margin", insets1, insets2, pChListener.event);
        assertEquals(insets2, jtc.getMargin());
    }

    public void testSetGetComponentOrientation() throws Exception {
        assertEquals(ComponentOrientation.UNKNOWN, jtc.getComponentOrientation());
        docXXX = (AbstractDocument) jtc.getDocument();
        jtc.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertEquals(new Boolean(true), docXXX.getProperty(TextAttribute.RUN_DIRECTION));
        jtc.setText("JTextComponent");
        assertEquals(2, docXXX.getBidiRootElement().getElementCount());
        assertEquals(ComponentOrientation.RIGHT_TO_LEFT, jtc.getComponentOrientation());
        jtc.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        assertEquals(new Boolean(false), docXXX.getProperty(TextAttribute.RUN_DIRECTION));
        jtc.setText("JTextComponent");
        assertEquals(ComponentOrientation.LEFT_TO_RIGHT, jtc.getComponentOrientation());
        jtc.setComponentOrientation(ComponentOrientation.UNKNOWN);
        assertEquals(new Boolean(false), docXXX.getProperty(TextAttribute.RUN_DIRECTION));
        jtc.setText("JTextComponent");
        assertEquals(ComponentOrientation.UNKNOWN, jtc.getComponentOrientation());
    }

    public void testSetGetColors() throws Exception {
        Color color1 = getColorProperty("caretForeground");
        assertEquals(color1, jtc.getCaretColor());
        jtc.setCaretColor(color);
        assertEqualsPropertyChangeEvent("caretColor", color1, color, pChListener.event);
        assertEquals(color, jtc.getCaretColor());
        color1 = getColorProperty("selectionBackground");
        assertEquals(color1, jtc.getSelectionColor());
        jtc.setSelectionColor(color);
        assertEqualsPropertyChangeEvent("selectionColor", color1, color, pChListener.event);
        assertEquals(color, jtc.getSelectionColor());
        color1 = getColorProperty("inactiveForeground");
        assertEquals(color1, jtc.getDisabledTextColor());
        jtc.setDisabledTextColor(color);
        assertEqualsPropertyChangeEvent("disabledTextColor", color1, color, pChListener.event);
        assertEquals(color, jtc.getDisabledTextColor());
        color1 = getColorProperty("selectionForeground");
        assertEquals(color1, jtc.getSelectedTextColor());
        jtc.setSelectedTextColor(color);
        assertEqualsPropertyChangeEvent("selectedTextColor", color1, color, pChListener.event);
        assertEquals(color, jtc.getSelectedTextColor());
    }

    public void testSetIsEditable() throws Exception {
        assertTrue(jtc.isEditable());
        jtc.setEditable(false);
        assertEqualsPropertyChangeEvent("editable", new Boolean(true), new Boolean(false),
                pChListener.event);
        assertFalse(jtc.isEditable());
    }

    public void testSetGetDragEnabled() throws Exception {
        /*
         * class RunnableCase0 extends RunnableWrap { public void run() {
         * assertFalse(jtc.getDragEnabled());
         * assertNotNull(jtc.getTransferHandler()); TransferHandler th =
         * jtc.getTransferHandler(); jtc.setDragEnabled(true);
         * assertTrue(jtc.getDragEnabled());
         * assertNotNull(jtc.getTransferHandler());
         * assertEquals(th,jtc.getTransferHandler());
         * jtc.setText("JTextComponent"); jtc.select(5,8); } }
         *
         * SwingUtilities.invokeAndWait(new RunnableCase0());
         * InternalTests.isRealized(jf); throwEx(afe);
         *
         * robot = null; try { robot = new Robot(); } catch(AWTException e){}
         *
         * try { rect = jtc.modelToView(8); }catch(BadLocationException e){}
         *
         * Point p = jtc.getLocationOnScreen();
         *
         * robot.mouseMove(rect.x -14 + p.x,rect.y +2 + p.y);
         * robot.mousePress(InputEvent.BUTTON1_MASK); try { Thread.sleep(500); }
         * catch(Exception e){}
         *
         * try { rect = jtc.modelToView(4); }catch(BadLocationException e){}
         *
         * robot.mouseMove(rect.x + p.x,rect.y+2 + p.y); robot.waitForIdle();
         *
         * try { rect = jtc.modelToView(2); }catch(BadLocationException e){}
         *
         *
         * robot.mouseMove(rect.x + p.x,rect.y+2 + p.y);
         * robot.mouseRelease(InputEvent.BUTTON1_MASK);
         *
         * try { Thread.sleep(500); } catch(Exception e){}
         *
         *
         * assertEquals(5,jtc.getCaretPosition());
         * assertNull(jtc.getSelectedText());
         * assertEquals("JTComextponent",jtc.getText());
         *
         * throwEx(afe);
         */
    }

    public void testSelect() throws Exception {
        jtc.setText("JTextComponent");
        jtc.select(5, 8);
        assertEquals("Com", jtc.getSelectedText());
        assertEquals(5, jtc.getCaret().getMark());
        assertEquals(8, jtc.getCaret().getDot());
        jtc.select(-2, -1);
        assertNull(jtc.getSelectedText());
        assertEquals(0, jtc.getCaret().getMark());
        assertEquals(0, jtc.getCaret().getDot());
        jtc.select(-5, 16);
        assertEquals("JTextComponent", jtc.getSelectedText());
        assertEquals(0, jtc.getCaret().getMark());
        assertEquals(14, jtc.getCaret().getDot());
        jtc.select(17, 18);
        assertNull(jtc.getSelectedText());
        assertEquals(14, jtc.getCaret().getMark());
        assertEquals(14, jtc.getCaret().getDot());
        jtc.select(8, 5);
        assertNull(jtc.getSelectedText());
        assertEquals(8, jtc.getCaret().getMark());
        assertEquals(8, jtc.getCaret().getDot());
        jtc.select(8, 8);
        assertNull(jtc.getSelectedText());
        assertEquals(8, jtc.getCaret().getMark());
        assertEquals(8, jtc.getCaret().getDot());
    }

    public void testSetGetSelectionStartEnd() throws Exception {
        if (!isHarmony()) {
            return;
        }
        assertEquals(0, jtc.getSelectionStart());
        assertEquals(0, jtc.getSelectionEnd());
        jtc.setText("JTextComponent");
        assertEquals(14, jtc.getSelectionStart());
        assertEquals(14, jtc.getSelectionEnd());
        jtc.setSelectionStart(5);
        jtc.setSelectionEnd(8);
        assertEquals("Com", jtc.getSelectedText());
        assertEquals(5, jtc.getSelectionStart());
        assertEquals(8, jtc.getSelectionEnd());
        assertEquals(8, jtc.getCaret().getDot());
        assertEquals(5, jtc.getCaret().getMark());
        jtc.setSelectionStart(8);
        jtc.setSelectionEnd(5);
        assertNull(jtc.getSelectedText());
        assertEquals(5, jtc.getSelectionStart());
        assertEquals(5, jtc.getSelectionEnd());
        assertEquals(5, jtc.getCaret().getDot());
        assertEquals(5, jtc.getCaret().getMark());
        jtc.setCaretPosition(4);
        jtc.moveCaretPosition(6);
        assertEquals(4, jtc.getSelectionStart());
        assertEquals(6, jtc.getSelectionEnd());
        jtc.setCaretPosition(6);
        jtc.moveCaretPosition(3);
        assertEquals(3, jtc.getSelectionStart());
        assertEquals(6, jtc.getSelectionEnd());
        jtc.setSelectionStart(4);
        assertEquals(4, jtc.getCaret().getDot());
        assertEquals(6, jtc.getCaret().getMark());
        assertEquals(4, jtc.getSelectionStart());
        assertEquals(6, jtc.getSelectionEnd());
        jtc.setSelectionEnd(3);
        assertEquals(3, jtc.getCaret().getDot());
        assertEquals(3, jtc.getCaret().getMark());
        assertEquals(3, jtc.getSelectionStart());
        assertEquals(3, jtc.getSelectionEnd());
        assertNull(jtc.getSelectedText());
        jtc.setSelectionStart(8);
        assertEquals(8, jtc.getCaret().getDot());
        assertEquals(8, jtc.getCaret().getMark());
        assertEquals(8, jtc.getSelectionStart());
        assertEquals(8, jtc.getSelectionEnd());
        jtc.setSelectionEnd(10);
        assertEquals(10, jtc.getCaret().getDot());
        assertEquals(8, jtc.getCaret().getMark());
        assertEquals(8, jtc.getSelectionStart());
        assertEquals(10, jtc.getSelectionEnd());
        jtc.setSelectionStart(9);
        assertEquals(9, jtc.getCaret().getDot());
        assertEquals(10, jtc.getCaret().getMark());
        assertEquals(9, jtc.getSelectionStart());
        assertEquals(10, jtc.getSelectionEnd());
        jtc.setCaretPosition(8);
        jtc.moveCaretPosition(4);
        assertEquals("tCom", jtc.getSelectedText());
        jtc.setSelectionEnd(6);
        assertEquals(6, jtc.getCaret().getDot());
        assertEquals(4, jtc.getCaret().getMark());
        assertEquals(4, jtc.getSelectionStart());
        assertEquals(6, jtc.getSelectionEnd());
    }

    public void testSetGetCaretPosition() throws Exception {
        assertEquals(0, jtc.getCaretPosition());
        jtc.setText("JTextComponent");
        assertEquals(14, jtc.getCaretPosition());
        jtc.setCaretPosition(5);
        assertEquals(5, jtc.getCaretPosition());
        bWasException = false;
        s = null;
        try {
            jtc.setCaretPosition(-2);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("bad position: -2", s);
        bWasException = false;
        s = null;
        try {
            jtc.setCaretPosition(20);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("bad position: 20", s);
    }

    public void testMoveCaretPosition() throws Exception {
        jtc.setText("JTextComponent");
        jtc.setCaretPosition(5);
        jtc.moveCaretPosition(8);
        assertEquals(8, jtc.getCaretPosition());
        bWasException = false;
        s = null;
        try {
            jtc.moveCaretPosition(-2);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("bad position: -2", s);
        bWasException = false;
        s = null;
        try {
            jtc.moveCaretPosition(20);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            s = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("bad position: 20", s);
    }

    public void testSetGetFocusAccelerator() throws Exception {
        //TODO It's very strange but in 1.5.0 PropertyChangeEvent's
        //name doesn't equal JTextComponent.FOCUS_ACCELERATOR_KEY
        String name = JTextComponent.FOCUS_ACCELERATOR_KEY;
        //String name = "focusAccelerator";
        assertEquals('\0', jtc.getFocusAccelerator());
        jtc.setFocusAccelerator('a');
        assertSame(name, pChListener.event.getPropertyName());
        assertEqualsPropertyChangeEvent(name, new Character('\0'), new Character('A'),
                pChListener.event);
        assertEquals('A', jtc.getFocusAccelerator());
        jtc.setFocusAccelerator('B');
        assertEqualsPropertyChangeEvent(name, new Character('A'), new Character('B'),
                pChListener.event);
        assertEquals('B', jtc.getFocusAccelerator());
    }

    public void testSelectAll() throws Exception {
        jtc.selectAll();
        assertNull(jtc.getSelectedText());
        jtc.setText("JTextComponent");
        jtc.selectAll();
        assertEquals("JTextComponent", jtc.getSelectedText());
        assertEquals(14, jtc.getCaret().getDot());
        assertEquals(0, jtc.getCaret().getMark());
        jtc.setText("\u05DC" + "\u05DC" + "\u05DC" + "\u05DC");
        jtc.selectAll();
        assertEquals(4, jtc.getCaret().getDot());
        assertEquals(0, jtc.getCaret().getMark());
    }

    public void testPaste() throws Exception {
        // TODO: uncomment when System clipboard is properly supported
        //        if (jtc.getToolkit().getSystemClipboard() == null)
        //            return;
        //        jtc.setText("JTextComponent");
        //        setClipboardString(jtc, "XXX");
        //        jtc.setCaretPosition(5);
        //        jtc.paste();
        //        assertEquals("XXX", getClipboardString(jtc));
        //        assertNull(jtc.getSelectedText());
        //        assertEquals("JTextXXXComponent", jtc.getText());
        //
        //        jtc.select(10, 14);
        //        setClipboardString(jtc, "YYY");
        //        jtc.paste();
        //        assertEquals("YYY", getClipboardString(jtc));
        //        assertNull(jtc.getSelectedText());
        //        assertEquals("JTextXXXCoYYYent", jtc.getText());
        //
        //        jtc.select(14, 16);
        //        setClipboardString(jtc, "");
        //        jtc.paste();
        //        assertEquals("", getClipboardString(jtc));
        //        //TODO ??
        //        assertNull(jtc.getSelectedText());
        //        assertEquals("JTextXXXCoYYYe", jtc.getText());
    }

    public void testCut() throws Exception {
        // TODO: uncomment when System clipboard is properly supported
        //        if (jtc.getToolkit().getSystemClipboard() == null)
        //            return;
        //        jtc.setText("JTextComponent");
        //        setClipboardString(jtc, "XXX");
        //        jtc.cut();
        //        assertEquals("XXX", getClipboardString(jtc));
        //        assertNull(jtc.getSelectedText());
        //        assertEquals("JTextComponent", jtc.getText());
        //
        //        jtc.select(4, 8);
        //        jtc.cut();
        //        assertEquals("tCom", getClipboardString(jtc));
        //        assertNull(jtc.getSelectedText());
        //        assertEquals("JTexponent", jtc.getText());
    }

    String getClipboardString(final JTextComponent jtc) {
        String content = null;
        Toolkit toolkit = jtc.getToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        DataFlavor dataFlavor = DataFlavor.stringFlavor;
        try {
            content = (String) clipboard.getContents(null).getTransferData(dataFlavor);
        } catch (UnsupportedFlavorException e) {
        } catch (IOException e) {
        }
        return content;
    }

    void setClipboardString(final JTextComponent jtc, final String content) {
        Toolkit toolkit = jtc.getToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        StringSelection dataFlavor = new StringSelection(content);
        clipboard.setContents(dataFlavor, dataFlavor);
    }

    public void testCopy() throws Exception {
        // TODO: uncomment when System clipboard is properly supported
        //        if (jtc.getToolkit().getSystemClipboard() == null)
        //            return;
        //        jtc.setText("JTextComponent");
        //        setClipboardString(jtc, "XXX");
        //        jtc.copy();
        //        assertEquals("XXX", getClipboardString(jtc));
        //        assertNull(jtc.getSelectedText());
        //        assertEquals("JTextComponent", jtc.getText());
        //
        //        jtc.select(4, 8);
        //        jtc.copy();
        //        assertEquals("tCom", getClipboardString(jtc));
        //        assertEquals("tCom", jtc.getSelectedText());
        //        assertEquals("JTextComponent", jtc.getText());
    }

    public void testAddRemoveKeymaps() throws Exception {
        Keymap keyMap = jtc.getKeymap();
        Keymap keyMap1 = JTextComponent.addKeymap("First", keyMap);
        assertEquals("First", keyMap1.getName());
        Keymap keyMap2 = JTextComponent.addKeymap("Second", keyMap1);
        assertEquals("Second", keyMap2.getName());
        Keymap keyMap3 = JTextComponent.addKeymap("Third", null);
        Keymap keyMap4 = JTextComponent.addKeymap(null, null);
        assertEquals("Third", keyMap3.getName());
        Keymap keyMap5 = JTextComponent.addKeymap("Fifth", keyMap2);
        assertNotNull(keyMap5);
        assertEquals(keyMap, keyMap1.getResolveParent());
        assertEquals(keyMap2.getResolveParent(), keyMap1);
        assertNull(keyMap3.getResolveParent());
        assertNull(keyMap4.getResolveParent());
        assertEquals(keyMap1, JTextComponent.getKeymap("First"));
        assertEquals(keyMap2, JTextComponent.getKeymap("Second"));
        assertEquals(keyMap3, JTextComponent.getKeymap("Third"));
        assertNull(JTextComponent.getKeymap("Fourth"));
        JTextComponent.removeKeymap("First");
        assertNull(JTextComponent.getKeymap("First"));
        //assertEquals(keyMap,JTextComponent.getKeymap("Second").//TODO
        //      getResolveParent());
        JTextComponent.removeKeymap("Second");
        JTextComponent.removeKeymap("Third");
        JTextComponent.removeKeymap("Fifth");
        assertNull(JTextComponent.getKeymap("Second"));
        assertNull(JTextComponent.getKeymap("Third"));
        assertNull(JTextComponent.getKeymap("Fifth"));
    }

    public void testLoadKeymap() throws Exception {
        JTextArea jta = new JTextArea();
        Keymap k = jta.getKeymap();
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK);
        KeyStroke keyStroke4 = KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK);
        KeyStroke keyStroke5 = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK);
        KeyStroke keyStroke6 = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK);
        JTextComponent.KeyBinding keyBindings[] = {
                new JTextComponent.KeyBinding(keyStroke1, "first"),
                new JTextComponent.KeyBinding(keyStroke2, DefaultEditorKit.copyAction),
                new JTextComponent.KeyBinding(keyStroke3, "second"),
                new JTextComponent.KeyBinding(keyStroke4, DefaultEditorKit.pasteAction),
                new JTextComponent.KeyBinding(keyStroke6, "first"),
                new JTextComponent.KeyBinding(keyStroke5, "first") };
        Action actions[] = { new SimpleTextAction("first"), new SimpleTextAction("second"),
                new DefaultEditorKit.CopyAction(), new DefaultEditorKit.CutAction() };
        JTextComponent.loadKeymap(k, keyBindings, actions);
        assertEquals(5, k.getBoundActions().length);
        assertEquals(5, k.getBoundKeyStrokes().length);
        isElement(k.getBoundActions(), actions[0], 3);
        isElement(k.getBoundActions(), actions[1], 1);
        isElement(k.getBoundActions(), actions[2], 1);
        isElement(k.getBoundKeyStrokes(), keyStroke1, 1);
        isElement(k.getBoundKeyStrokes(), keyStroke2, 1);
        isElement(k.getBoundKeyStrokes(), keyStroke3, 1);
        isElement(k.getBoundKeyStrokes(), keyStroke5, 1);
        isElement(k.getBoundKeyStrokes(), keyStroke6, 1);
        assertEquals(actions[0], k.getAction(keyStroke1));
        assertEquals(actions[2], k.getAction(keyStroke2));
        assertEquals(actions[1], k.getAction(keyStroke3));
        assertEquals(actions[0], k.getAction(keyStroke5));
        assertEquals(actions[0], k.getAction(keyStroke6));
        assertNull(k.getAction(keyStroke4));
        assertEquals(3, k.getKeyStrokesForAction(actions[0]).length);
        assertEquals(1, k.getKeyStrokesForAction(actions[1]).length);
        assertEquals(1, k.getKeyStrokesForAction(actions[2]).length);
        isElement(k.getKeyStrokesForAction(actions[0]), keyStroke1, 1);
        isElement(k.getKeyStrokesForAction(actions[0]), keyStroke5, 1);
        isElement(k.getKeyStrokesForAction(actions[0]), keyStroke6, 1);
        isElement(k.getKeyStrokesForAction(actions[1]), keyStroke3, 1);
        isElement(k.getKeyStrokesForAction(actions[2]), keyStroke2, 1);
        assertNull(k.getKeyStrokesForAction(actions[3]));
    }

    public void testConstants() {
        assertEquals("default", JTextComponent.DEFAULT_KEYMAP);
        assertEquals("focusAcceleratorKey", JTextComponent.FOCUS_ACCELERATOR_KEY);
    }

    public void testJTextComponent_KeyBinding() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK);
        JTextComponent.KeyBinding keyBinding = new JTextComponent.KeyBinding(keyStroke, s);
        assertNotNull(keyBinding);
        assertEquals(keyStroke, keyBinding.key);
        assertEquals(s, keyBinding.actionName);
    }

    // Serialization is not supported now by Swing
    /*public void testSerialization() throws Exception {
     jtc.setText("JTextComponent");
     jtc.setCaretColor(color);
     jtc.setSelectionColor(color);
     jtc.setSelectedTextColor(color);
     jtc.setDisabledTextColor(color);
     //jtc.select(5,8);
     jtc.setMargin(new Insets(10, 20, 30, 40));
     //jtc.setKeymap(new SimpleKeyMap("KeyMap"));
     jtc.setKeymap(JTextComponent.addKeymap("KeyMap", null));
     jtc.setEditable(false);
     jtc.setDragEnabled(true);
     jtc.addInputMethodListener(new SimpleInputMethodListener("1"));
     jtc.addCaretListener(new SimpleCaretListener("1"));
     jtc.setFocusAccelerator('a');
     jtc.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

     JTextArea jtc1 = new JTextArea();

     try {
     FileOutputStream fo = new FileOutputStream("tmp");
     ObjectOutputStream so = new ObjectOutputStream(fo);
     so.writeObject(jtc);
     so.flush();
     so.close();
     FileInputStream fi = new FileInputStream("tmp");
     ObjectInputStream si = new ObjectInputStream(fi);
     jtc1 = (JTextArea) si.readObject();
     si.close();
     } catch (Exception e) {
     assertTrue("seralization failed" + e.getMessage(),false);
     }

     assertEquals("JTextComponent", jtc1.getText());
     assertEquals(color, jtc1.getCaretColor());
     assertEquals(color, jtc1.getSelectionColor());
     assertEquals(color, jtc1.getSelectedTextColor());
     assertEquals(color, jtc1.getDisabledTextColor());
     assertNull(jtc1.getSelectedText());
     assertEquals(0, jtc1.getCaretPosition());
     assertEquals(new Insets(10, 20, 30, 40), jtc1.getMargin());
     assertNotSame("KeyMap", jtc1.getKeymap().getName());
     assertFalse(jtc1.isEditable());
     assertTrue(jtc1.getDragEnabled());
     assertEquals(0, jtc1.getCaretListeners().length);
     assertEquals(0, jtc1.getInputMethodListeners().length);
     assertEquals('A', jtc1.getFocusAccelerator());
     assertNotSame(ComponentOrientation.RIGHT_TO_LEFT, jtc1
     .getComponentOrientation());
     assertEquals(0, jtc1.getHighlighter().getHighlights().length);
     } */
    public void testKeyMap() throws Exception {
        KeyStroke keyStrokeA = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK);
        KeyStroke keyStrokeB = KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK);
        KeyStroke keyStrokeC = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK);
        TextAction A = new DefaultEditorKit.CopyAction();
        TextAction B = new DefaultEditorKit.CutAction();
        TextAction C = new DefaultEditorKit.PasteAction();
        TextAction D = new DefaultEditorKit.DefaultKeyTypedAction();
        Keymap grand_parent = JTextComponent.addKeymap("grand_parent", null);
        grand_parent.addActionForKeyStroke(keyStrokeA, A);
        Keymap parent = JTextComponent.addKeymap("parent", grand_parent);
        parent.addActionForKeyStroke(keyStrokeB, B);
        parent.setDefaultAction(D);
        Keymap keymap = JTextComponent.addKeymap("test", parent);
        keymap.addActionForKeyStroke(keyStrokeC, C);
        assertFalse(keymap.isLocallyDefined(keyStrokeB));
        assertFalse(keymap.isLocallyDefined(keyStrokeA));
        assertTrue(keymap.isLocallyDefined(keyStrokeC));
        assertEquals(A, keymap.getAction(keyStrokeA));
        assertEquals(B, keymap.getAction(keyStrokeB));
        assertEquals(C, keymap.getAction(keyStrokeC));
        assertEquals(D, keymap.getDefaultAction());
    }

    public void testKeymapToString() {
        KeyStroke keyStrokeX = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK);
        KeyStroke keyStrokeY = KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK);
        KeyStroke keyStrokeZ = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK);
        Action a = new SimpleTextAction("a");//DefaultEditorKit.selectWordDoing;
        Action b = new SimpleTextAction("b");//DefaultEditorKit.selectLineDoing;
        jtc.getKeymap().addActionForKeyStroke(keyStrokeX, a);
        jtc.getKeymap().addActionForKeyStroke(keyStrokeY, a);
        jtc.getKeymap().addActionForKeyStroke(keyStrokeZ, b);
        String sample1 = (keyStrokeY + "=" + a).replaceFirst("[/$]", "");
        String sample2 = (keyStrokeX + "=" + a).replaceFirst("[/$]", "");
        String sample3 = (keyStrokeZ + "=" + b).replaceFirst("[/$]", "");
        String test = jtc.getKeymap().toString().replaceAll("[/$]", "");
        assertTrue(test.indexOf(sample1) > 0);
        assertTrue(test.indexOf(sample2) > 0);
        assertTrue(test.indexOf(sample3) > 0);
        test = test.replaceFirst(sample1, "").replaceFirst(sample2, "").replaceFirst(sample3,
                "");
        assertEquals("Keymap[BasicTextAreaUI]{, , }", test);
        jtc.getKeymap().removeKeyStrokeBinding(keyStrokeX);
        jtc.getKeymap().removeKeyStrokeBinding(keyStrokeY);
        jtc.getKeymap().removeKeyStrokeBinding(keyStrokeZ);
    }

    private Color getColorProperty(final String key) {
        final UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();
        return uiDefaults.getColor("TextArea." + key);
    }
}