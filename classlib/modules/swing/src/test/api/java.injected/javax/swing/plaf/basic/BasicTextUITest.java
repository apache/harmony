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

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InputMapUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.PlainView;
import javax.swing.text.Position;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;

import junit.framework.AssertionFailedError;

public class BasicTextUITest extends SwingTestCase {
    MyBasicTextUI basicTextUI;

    JTextField tf;

    JExtTextArea jta;

    JFrame jf;

    String s;

    final Dimension PREF_SIZE = new Dimension(184, 20);

    Position.Bias forward = Position.Bias.Forward;

    Position.Bias backward = Position.Bias.Backward;

    private View view;

    boolean wasCallInvalidate = false;

    final ColorUIResource RED = new ColorUIResource(255, 0, 0);

    final ColorUIResource BLUE = new ColorUIResource(0, 0, 255);

    final ColorUIResource GREEN = new ColorUIResource(0, 255, 255);

    final ColorUIResource YELLOW = new ColorUIResource(255, 255, 0);

    final ColorUIResource BLACK = new ColorUIResource(0, 0, 0);

    final Font FONT = new Font("SimSun", 8, 8);

    boolean bWasException;

    class MyBasicTextUI extends BasicTextUI {
        @Override
        protected String getPropertyPrefix() {
            return null;
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
        }

        @Override
        protected void installKeyboardActions() {
            super.installKeyboardActions();
        }

        @Override
        protected void installListeners() {
            super.installListeners();
        }

        @Override
        public void installUI(final JComponent c) {
            super.installUI(c);
        }

        @Override
        protected void propertyChange(final PropertyChangeEvent e) {
            super.propertyChange(e);
        }
    }

    class JExtTextArea extends JTextArea {
        private static final long serialVersionUID = 1L;

        public boolean wasCallInvalidate = false;

        JExtTextArea(final String s) {
            super(s);
        }

        @Override
        public void invalidate() {
            wasCallInvalidate = true;
            super.invalidate();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bWasException = false;
        s = null;
        basicTextUI = new MyBasicTextUI();
        tf = new JTextField("JTextField\n JTextField \n JTextField");
        UIManager.put("TextAreaUI", "javax.swing.plaf.basic.TextAreaUI");
        jf = new JFrame();
        jta = new JExtTextArea("");
        //jta.setText(sLTR + sRTL + sLTR + "\n" + sLTR + sRTL + sLTR + "\n"
        //        + sLTR + sRTL + sLTR + "\n" + "JTextArea \n JTextArea\n"
        //        + "\u05dc");
        jta.setText("aaa" + "\n" + "bbb" + "\n" + "ccc" + "\n" + "JTextArea \n JTextArea\n"
                + "e");
        jf.getContentPane().add(jta);
        jf.setSize(200, 300);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        UIManager.put("TextAreaUI", "javax.swing.plaf.basic.BasicTextAreaUI");
        super.tearDown();
    }

//    public void testBasicTextUI() {
//    }

    private void getPos(final BasicTextUI ui, final int start, final Position.Bias bias,
            final int direction, final int samplePos, final Position.Bias sample,
            final boolean cond) {
        int p = 0;
        Position.Bias b[] = new Position.Bias[1];
        try {
            p = ui.getNextVisualPositionFrom(jta, start, bias, direction, b);
        } catch (BadLocationException e) {
        }
        assertEquals(samplePos, p);
    }

    public void testGetNextVisualPosition() throws Exception {
        BasicTextUI ui = (BasicTextUI) jta.getUI();
        try {
            ui.getNextVisualPositionFrom(jta, 5, Position.Bias.Forward,
                    SwingConstants.SOUTH_WEST, new Position.Bias[1]);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            s = e.getMessage();
        } catch (BadLocationException e) {
        }
        if (isHarmony()) {
            assertTrue(bWasException);
            assertEquals("Invalid direction", s);
            getPos(ui, 6, forward, SwingConstants.WEST, 5, forward, true);
            //This tests depends on MagicCaretPosition (timer)
            //getPos(ui, 6, forward, SwingConstants.NORTH, 0, forward);
            //getPos(ui, 6, forward, SwingConstants.SOUTH, 8, forward);
            getPos(ui, 6, forward, SwingConstants.EAST, 7, forward, false);
            getPos(ui, 6, backward, SwingConstants.WEST, 5, backward, true);
            //This tests depends on MagicCaretPosition (timer)
            //getPos(ui, 4, backward, SwingConstants.NORTH, 4, backward); //?? 6
            //getPos(ui, 36, backward, SwingConstants.SOUTH, 36, backward); //?? 6
            getPos(ui, 6, backward, SwingConstants.EAST, 7, forward, true);
        }
    }

    public void testGetToolTipTextJTextComponentPoint() {
        Point p = new Point(0, 0);
        for (int i = 0; i < 200; i++) {
            for (int j = 0; j < 200; j++) {
                p.x = i;
                p.y = j;
                assertNull(getBasicTextUI(jta).getToolTipText(tf, p));
                assertNull(getBasicTextUI(jta).getToolTipText(null, p));
                assertNull(getBasicTextUI(jta).getToolTipText(jta, p));
            }
        }
    }

    public void testModelToView() {
        BasicTextUI ui = (BasicTextUI) jta.getUI();
        View view = ui.getRootView(jta).getView(0);
        Rectangle r1 = null, r2 = null;
        try {
            int length = jta.getDocument().getLength();
            for (int i = 0; i < length; i++) {
                Rectangle visibleRect = ui.getVisibleEditorRect();
                r1 = ui.modelToView(null, i, forward);
                r2 = view.modelToView(i, visibleRect, forward).getBounds();
                assertEquals(r2, r1);
                r1 = ui.modelToView(null, i);
                assertEquals(r2, r1);
                r1 = ui.modelToView(null, i, backward);
                r2 = view.modelToView(i, visibleRect, backward).getBounds();
                assertEquals(r2, r1);
            }
        } catch (BadLocationException e) {
        }
    }

    public void testGetRootView() {
        BasicTextUI ui = getBasicTextUI(jta);
        View rootView = ui.getRootView(jta);
        AbstractDocument doc = (AbstractDocument) jta.getDocument();
        Element rootElement = jta.getDocument().getDefaultRootElement();
        assertEquals(1, rootView.getViewCount());
        View sonRootView = rootView.getView(0);
        assertNotNull(sonRootView);
        assertEquals(0, sonRootView.getViewCount());
        assertEquals(rootElement, rootView.getElement());
        assertEquals(sonRootView.getElement(), rootElement);
        assertEquals(rootView.getView(0).getElement(), rootElement);
        assertEquals(doc, rootView.getDocument());
        assertEquals(jta, rootView.getContainer());
        assertEquals(doc.getLength() + 1, rootView.getEndOffset());
        assertEquals(ui, rootView.getViewFactory());
        assertNull(rootView.getParent());
        /*
         * Next Functionality is not required try { rootView.setParent(new
         * PlainView(rootElement)); } catch (Error error) { bWasException =
         * true; s = error.getMessage(); } assertTrue(bWasException);
         * assertEquals("Can't set parent on root view", s);
         *
         * assertEquals(rootView, rootView.createFragment(2, 5));
         * assertTrue(jta.getAlignmentX() ==
         * rootView.getAlignment(View.X_AXIS)); assertTrue(jta.getAlignmentY() ==
         * rootView.getAlignment(View.Y_AXIS));
         * assertNull(rootView.getAttributes());
         *
         * //has not null AttributeSet Dimension dimension =
         * ui.getMaximumSize(jta);
         *
         * //assertTrue(rintSpec(dimension.width) == //
         * (int)(rootView.getMaximumSpan(View.X_AXIS) / 10));
         *
         */
    }

    public void testCreateElementintint() {
        Element elem = jta.getDocument().getDefaultRootElement();
        assertNull(basicTextUI.create(elem));
        assertNull(basicTextUI.create(elem, 0, 5));
    }

    public void testGetEditorKit() {
        EditorKit editorKit = basicTextUI.getEditorKit(jta);
        assertTrue(editorKit instanceof DefaultEditorKit);
        assertFalse(editorKit instanceof StyledEditorKit);
        //JEditorPane is not support yet
        //assertEquals(editorKit, basicTextUI.getEditorKit(new JTextPane()));
        //assertEquals(editorKit, basicTextUI.getEditorKit(new JEditorPane()));
        assertEquals(editorKit, basicTextUI.getEditorKit(new JTextField()));
        MyBasicTextUI basicTextUI2 = new MyBasicTextUI();
        assertEquals(editorKit, basicTextUI2.getEditorKit(new JTextArea()));
    }

    private Point rectToPoint(final Point p, final Rectangle r) {
        p.x = r.x;
        p.y = r.y;
        return p;
    }

    public void testViewToModel() {
        BasicTextUI ui = (BasicTextUI) jta.getUI();
        Position.Bias b[] = new Position.Bias[1];
        Rectangle r1 = null;
        Rectangle r2 = null;
        Point p = new Point(0, 0);
        try {
            r1 = ui.modelToView(null, 6, forward);
        } catch (BadLocationException e) {
        }
        try {
            r2 = ui.modelToView(null, 6, backward);
        } catch (BadLocationException e) {
        }
        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals(6, ui.viewToModel(null, rectToPoint(p, r1)));
        assertEquals(6, ui.viewToModel(null, rectToPoint(p, r2)));
        assertEquals(6, ui.viewToModel(null, rectToPoint(p, r1), b));
        assertEquals(6, ui.viewToModel(null, rectToPoint(p, r2), b));
    }

    private BasicTextUI getBasicTextUI(final JTextComponent c) {
        return (BasicTextUI) c.getUI();
    }

    public void testGetPreferredSizeJComponent() throws Exception {
        tf = new JTextField("JTextField\n JTextField \n JTextField");
        View view = tf.getUI().getRootView(tf);
        Insets insets = tf.getInsets();
        int prefX = (int) view.getPreferredSpan(View.X_AXIS);
        int prefY = (int) view.getPreferredSpan(View.Y_AXIS);
        int hrz = insets.left + insets.right;
        int vrt = insets.top + insets.bottom;
        int uiPrefWidth = prefX + hrz;
        int uiPrefHeight = prefY + vrt;
        assertEquals(new Dimension(uiPrefWidth, uiPrefHeight), getBasicTextUI(tf)
                .getPreferredSize(tf));
    }

    public void testGetMinimumSizeJComponent() {
        tf = new JTextField("JTextField\n JTextField \n JTextField");
        View view = tf.getUI().getRootView(tf);
        Insets insets = tf.getInsets();
        int minX = (int) view.getMinimumSpan(View.X_AXIS);
        int minY = (int) view.getMinimumSpan(View.Y_AXIS);
        int hrz = insets.left + insets.right;
        int vrt = insets.top + insets.bottom;
        int uiMinWidth = minX + hrz;
        int uiMinHeight = minY + vrt;
        assertEquals(new Dimension(uiMinWidth, uiMinHeight), getBasicTextUI(tf).getMinimumSize(
                tf));
    }

    public void testGetMaximumSizeJComponent() {
        assertEquals(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE), ((BasicTextUI) jta
                .getUI()).getMaximumSize(jta));
    }

    public void testSetView() throws Exception {
        jta.wasCallInvalidate = false;
        view = new PlainView(jta.getDocument().getDefaultRootElement());
        ((BasicTextUI) jta.getUI()).setView(view);
        wasCallInvalidate = jta.wasCallInvalidate;
        assertTrue(wasCallInvalidate);
        assertEquals(view, ((BasicTextUI) jta.getUI()).getRootView(jta).getView(0));
    }

    private String getKeymapName(final JTextComponent c) {
        BasicTextUI ui = (BasicTextUI) jta.getUI();
        String className = ui.getClass().getName();
        int start = className.lastIndexOf('.');
        int end = className.length();
        String keymapName = className.substring(start + 1, end);
        return keymapName;
    }

    public void testCreateKeymap() {
        JTextComponent.removeKeymap("BasicTextAreaUI");
        TextUI ui = jta.getUI();
        Keymap keymap = ((BasicTextUI) ui).createKeymap();
        assertEquals(getKeymapName(jta), keymap.getName());
        assertTrue(keymap.getDefaultAction() instanceof DefaultEditorKit.DefaultKeyTypedAction);
        assertEquals(0, keymap.getBoundActions().length);
        assertEquals(0, keymap.getBoundKeyStrokes().length);
        assertEquals(JTextComponent.DEFAULT_KEYMAP, keymap.getResolveParent().getName());
    }

    public void testDamageRangeJTextComponentintint() {
    }

    public void testGetComponent() {
        assertEquals(jta, ((BasicTextUI) jta.getUI()).getComponent());
    }

    public void testCreateHighlighter() {
        Highlighter highlighter = basicTextUI.createHighlighter();
        assertTrue(highlighter instanceof BasicTextUI.BasicHighlighter);
    }

    public void testCreateCaret() {
        Caret caret = basicTextUI.createCaret();
        assertTrue(caret instanceof BasicTextUI.BasicCaret);
    }

    private String findAndRemoveSubstring(final String str, final String subStr) {
        int index = str.indexOf(subStr);
        assertTrue(index >= 0);
        return str.replaceFirst(subStr, "");
    }

    public void testUninstallUI() throws Exception {
        TextUI ui = jta.getUI();
        assertTrue(ui instanceof TextAreaUI);
        TextAreaUI.callOrder = "";
        jta.setVisible(false);
        jta.getUI().uninstallUI(jta);
        String tmp = TextAreaUI.callOrder;
        tmp = findAndRemoveSubstring(tmp, "uninstallUI::");
        tmp = findAndRemoveSubstring(tmp, "uninstallDefaults::");
        tmp = findAndRemoveSubstring(tmp, "uninstallKeyboardActions::");
        tmp = findAndRemoveSubstring(tmp, "uninstallListeners::");
        assertEquals("", tmp);

        // regression for HARMONY-2521          
        new javax.swing.JTextPane().updateUI();            
    }

    public void testInstallUI() throws Exception {
        Caret caret = jta.getCaret();
        Highlighter highlighter = jta.getHighlighter();
        String prefix = ((BasicTextUI) tf.getUI()).getPropertyPrefix();
        (jta.getUI()).uninstallUI(jta);
        TextUI ui = jta.getUI();
        assertTrue(ui instanceof TextAreaUI);
        TextAreaUI.callOrder = "";
        (jta.getUI()).installUI(jta);
        String tmp = TextAreaUI.callOrder;
        tmp = findAndRemoveSubstring(tmp, "installUI::");
        tmp = findAndRemoveSubstring(tmp, "installDefaults::");
        tmp = findAndRemoveSubstring(tmp, "installKeyboardActions::");
        tmp = findAndRemoveSubstring(tmp, "installListeners::");
        tmp = findAndRemoveSubstring(tmp, "modelChanged::");
        tmp = findAndRemoveSubstring(tmp, "createCaret::");
        tmp = findAndRemoveSubstring(tmp, "createHighlighter::");
        tmp = tmp.replaceAll("create::", "");
        assertEquals("", tmp);
        assertNotSame(caret, jta.getCaret());
        assertTrue(jta.getCaret() instanceof UIResource);
        assertNotSame(highlighter, jta.getHighlighter());
        assertTrue(jta.getHighlighter() instanceof UIResource);
        assertTrue(jta.getTransferHandler() instanceof UIResource);
        assertTrue(jta.isOpaque());
        int caretBlinkRate = ((Integer) getProperty(prefix, "caretBlinkRate")).intValue();
        assertEquals(caretBlinkRate, caret.getBlinkRate());
        assertEquals(jta.getDocument().getDefaultRootElement(), jta.getUI().getRootView(jta)
                .getElement());
    }

    // Regression test for HARMONY-1779
    public void testInstallUINull() {
        basicTextUI = new MyBasicTextUI();
        try {
            basicTextUI.installUI(null);
            fail("Error is expected (\"TextUI needs JTextComponent\")");
        } catch (AssertionFailedError e) {
            // Let JUnit handle its exceptions
            throw e;
        } catch (Error e) {
            // expected
        }
    }

    // Regression test for HARMONY-1779
    public void testInstallUINonTextComponent() {
        basicTextUI = new MyBasicTextUI();
        try {
            basicTextUI.installUI(new JMenuItem());
            fail("Error is expected (\"TextUI needs JTextComponent\")");
        } catch (AssertionFailedError e) {
            // Let JUnit handle its exceptions
            throw e;
        } catch (Error e) {
            // expected
        }
    }

    public void testGetKeymapName() {
        assertEquals("BasicTextUITest$MyBasicTextUI", basicTextUI.getKeymapName());
    }

    public void testPropertyChange() throws Exception {
        TextAreaUI ui = (TextAreaUI) jta.getUI();
        ui.propertyChangeFlag = false;
        ui.eventName = null;
        jta.setCaretColor(null);
        assertTrue(ui.propertyChangeFlag);
        assertEquals("caretColor", ui.eventName);
    }

    public void testGetVisibleEditorRect() throws Exception {
        BasicTextUI ui = (BasicTextUI) jta.getUI();
        jta.setSize(3, 0);
        assertNull(ui.getVisibleEditorRect());
        jta.setBounds(0, 0, 234, 553);
        assertEquals(jta.getBounds(), ui.getVisibleEditorRect());
    }

    public void testUninstallListeners() {
    }

    public void testUninstallKeyboardActions() {
    }

    public void testUninstallDefaults() throws Exception {
        tf.setCaretColor(null);
        tf.setSelectionColor(null);
        tf.setSelectedTextColor(null);
        tf.setDisabledTextColor(null);
        tf.setFont(null);
        tf.setBackground(null);
        tf.setForeground(null);
        tf.setBorder(BorderUIResource.getEtchedBorderUIResource());
        tf.setMargin(null);
        tf.setActionMap(null);
        ((BasicTextUI) tf.getUI()).uninstallDefaults();
        assertNull(tf.getForeground());
        assertNull(tf.getBackground());
        assertNull(tf.getFont());
        assertNull(tf.getCaretColor());
        assertNull(tf.getSelectionColor());
        assertNull(tf.getSelectedTextColor());
        assertNull(tf.getDisabledTextColor());
        //?????
        assertNull(tf.getBorder());
        assertNull(tf.getMargin());
        tf.setCaretColor(RED);
        tf.setSelectionColor(GREEN);
        tf.setSelectedTextColor(BLUE);
        tf.setDisabledTextColor(YELLOW);
        tf.setFont(new FontUIResource("SimSun", 8, 8));
        tf.setBackground(BLACK);
        tf.setForeground(YELLOW);
        tf.setBorder(new BorderUIResource.LineBorderUIResource(Color.RED));
        tf.setMargin(new InsetsUIResource(2, 4, 6, 3));
        ((BasicTextUI) tf.getUI()).uninstallDefaults();
        //TODO specification antogonism
        //assertEquals(YELLOW,tf.getForeground()); //???
        //assertEquals(BLACK,tf.getBackground()); //???
        //assertEquals(new FontUIResource("SimSun", 8, 8),tf.getFont()); //???
        assertNull(tf.getCaretColor());
        assertNull(tf.getSelectionColor());
        assertNull(tf.getSelectedTextColor());
        assertNull(tf.getDisabledTextColor());
        //?????
        assertNull(tf.getBorder());
        assertNull(tf.getMargin());
        tf.setCaretColor(Color.RED);
        tf.setSelectionColor(Color.GREEN);
        tf.setSelectedTextColor(Color.BLUE);
        tf.setDisabledTextColor(Color.YELLOW);
        tf.setFont(FONT);
        tf.setBackground(Color.BLACK);
        tf.setForeground(Color.YELLOW);
        tf.setBorder(new BasicBorders.ButtonBorder(null, null, null, null));
        tf.setMargin(new Insets(2, 4, 6, 3));
        ((BasicTextUI) tf.getUI()).uninstallDefaults();
        assertEquals(Color.YELLOW, tf.getForeground());
        assertEquals(Color.BLACK, tf.getBackground());
        assertEquals(FONT, tf.getFont());
        assertEquals(Color.RED, tf.getCaretColor()); //!!!!! ???
        assertEquals(Color.GREEN, tf.getSelectionColor());
        assertEquals(Color.BLUE, tf.getSelectedTextColor());
        assertEquals(Color.YELLOW, tf.getDisabledTextColor());
        assertNull(tf.getBorder());//????
        assertEquals(new Insets(2, 4, 6, 3), tf.getMargin());
    }

    public void testModelChanged() throws Exception {
        TextAreaUI ui = (TextAreaUI) jta.getUI();
        ui.flagCreate = false;
        ui.modelChanged();
        assertTrue(ui.flagCreate);
    }

    public void testInstallListeners() throws Exception {
    }

    private InputMap getInputMap(final int generation) {
        InputMap im = jta.getInputMap();
        for (int i = 0; i < generation; i++) {
            im = im.getParent();
        }
        return im;
    }

    private ActionMap getActionMap(final int generation) {
        ActionMap am = jta.getActionMap();
        for (int i = 0; i < generation; i++) {
            am = am.getParent();
        }
        return am;
    }

    public void testInstallKeyboardActions() throws Exception {
        ((BasicTextUI) jta.getUI()).uninstallKeyboardActions();
        assertEquals(0, getInputMap(0).size());
        assertFalse(getInputMap(0) instanceof InputMapUIResource);
        assertEquals(0, getInputMap(1).size());
        assertTrue(getInputMap(1) instanceof InputMapUIResource);
        //assertEquals(55, jta.getInputMap().getParent().getParent().size());1.5.0
        assertTrue(getInputMap(2) instanceof InputMapUIResource);
        assertNull(getInputMap(3));
        assertEquals(0, getActionMap(0).size());
        assertFalse(getActionMap(0) instanceof ActionMapUIResource);
        assertNull(getActionMap(1));
        assertNull(jta.getKeymap());
        ((BasicTextUI) jta.getUI()).installKeyboardActions();
        assertNotNull(jta.getKeymap());
        assertEquals(getKeymapName(jta), jta.getKeymap().getName());
        assertEquals(0, getInputMap(0).size());
        assertEquals(0, getInputMap(1).size());
        assertEquals(0, getInputMap(2).size());
        //assertEquals(55, jta.getInputMap().getParent().getParent().getParent()
        //        .size()); //1.5.0
        assertNull(getInputMap(4));
        assertEquals(0, getActionMap(0).size());
        assertEquals(1, getActionMap(1).size());
        //Note
        //assertEquals(2,jta.getActionMap().getParent().getParent().size());
        assertEquals(56, getActionMap(3).size());
        assertNull(getActionMap(4));
        assertFalse(getInputMap(0) instanceof InputMapUIResource);
        assertFalse(getInputMap(1) instanceof InputMapUIResource);
        assertTrue(getInputMap(2) instanceof InputMapUIResource);
        assertTrue(getInputMap(3) instanceof InputMapUIResource);
        assertFalse(getActionMap(0) instanceof ActionMapUIResource);
        assertFalse(getActionMap(1) instanceof ActionMapUIResource);
        assertTrue(getActionMap(2) instanceof ActionMapUIResource);
        assertTrue(getActionMap(3) instanceof ActionMapUIResource);
    }

    private Object getProperty(final String prefix, final String s) {
        return UIManager.getLookAndFeelDefaults().get(prefix + "." + s);
    }

    private void setProperies(final JTextComponent c, final Color caretColor,
            final Color selectionColor, final Color selectedTextColor,
            final Color disabledTextColor, final Font font, final Color background,
            final Color foreground, final Insets margin, final Border border,
            final int caretBlinkRate) {
        c.setCaretColor(caretColor);
        c.setSelectionColor(selectionColor);
        c.setSelectedTextColor(selectedTextColor);
        c.setDisabledTextColor(disabledTextColor);
        c.setFont(font);
        c.setBackground(background);
        c.setForeground(foreground);
        c.setBorder(border);
        c.setMargin(margin);
        Caret caret = tf.getCaret();
        if (caret != null) {
            caret.setBlinkRate(caretBlinkRate);
        }
    }

    private void checkProperies(final JTextComponent c, final Color caretColor,
            final Color selectionColor, final Color selectedTextColor,
            final Color disabledTextColor, final Font font, final Color background,
            final Color foreground, final Insets margin, final Border border) {
        assertEquals(caretColor, c.getCaretColor());
        assertEquals(selectionColor, c.getSelectionColor());
        assertEquals(selectedTextColor, c.getSelectedTextColor());
        assertEquals(disabledTextColor, c.getDisabledTextColor());
        assertEquals(font, c.getFont());
        assertEquals(background, c.getBackground());
        assertEquals(foreground, c.getForeground());
        assertEquals(border, c.getBorder());
        assertEquals(margin, c.getMargin());
    }

    public void testInstallDefaults() throws Exception {
        String prefix = ((BasicTextUI) tf.getUI()).getPropertyPrefix();
        setProperies(tf, null, null, null, null, null, null, null, null, null, 0);
        tf.setActionMap(null);
        ((BasicTextUI) tf.getUI()).installDefaults();
        Color foreground = (Color) getProperty(prefix, "foreground");
        Color background = (Color) getProperty(prefix, "background");
        Font font = (Font) getProperty(prefix, "font");
        Color caretForeground = (Color) getProperty(prefix, "caretForeground");
        Color selectionBackground = (Color) getProperty(prefix, "selectionBackground");
        Color selectionForeground = (Color) getProperty(prefix, "selectionForeground");
        Color inactiveForeground = (Color) getProperty(prefix, "inactiveForeground");
        Insets margin = (Insets) getProperty(prefix, "margin");
        Border border = (Border) getProperty(prefix, "border");
        checkProperies(tf, caretForeground, selectionBackground, selectionForeground,
                inactiveForeground, font, background, foreground, margin, border);
        setProperies(tf, RED, GREEN, BLUE, YELLOW, new FontUIResource("SimSun", 8, 8), BLACK,
                YELLOW, new InsetsUIResource(2, 4, 6, 3),
                new BorderUIResource.LineBorderUIResource(Color.RED), 56);
        ((BasicTextUI) tf.getUI()).installDefaults();
        checkProperies(tf, caretForeground, selectionBackground, selectionForeground,
                inactiveForeground, font, background, foreground, margin, border);
        Border newBorder = new TitledBorder("KK");
        setProperies(tf, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, FONT, Color.BLACK,
                Color.YELLOW, new Insets(2, 4, 6, 3), newBorder, 0);
        ((BasicTextUI) tf.getUI()).installDefaults();
        checkProperies(tf, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, FONT, Color.BLACK,
                Color.YELLOW, new Insets(2, 4, 6, 3), newBorder);
    }

    public void testBasicCaret() {
        Caret caret = new BasicTextUI.BasicCaret();
        assertTrue(caret instanceof DefaultCaret);
        assertTrue(caret instanceof UIResource);
    }

    public void testBasicHighlighter() {
        Highlighter highlighter = new BasicTextUI.BasicHighlighter();
        assertTrue(highlighter instanceof DefaultHighlighter);
        assertTrue(highlighter instanceof UIResource);
    }

    public void testI18nProperty() throws Exception {
        JTextArea ta = new JTextArea("aaaa");
        TextAreaUI ui = (TextAreaUI) ta.getUI();
        ui.flagModelChanged = false;
        ta.setText("aaaa" + "\u05dc");
        assertTrue(ui.flagModelChanged);
        ui.flagModelChanged = false;
        ta.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        assertTrue(ui.flagModelChanged);
    }

    public void testCallOrder() throws Exception {
        TextAreaUI.callOrder = "";
        jta.getUI().uninstallUI(jta);
        assertEquals("uninstallUI::uninstallDefaults::uninstallKeyboard"
                + "Actions::uninstallListeners::", TextAreaUI.callOrder);
        TextAreaUI.callOrder = "";
        jta.getUI().installUI(jta);
        assertEquals("installUI::installDefaults::createCaret::createHighli"
                + "ghter::modelChanged::create::installListeners::installKeyboardActions::",
                TextAreaUI.callOrder);
    }

    public void testFocusAccelerator() throws Exception {
        jta.setFocusAccelerator('a');
        InputMap im = SwingUtilities.getUIInputMap(jta, JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = jta.getActionMap().getParent().getParent();
        assertNotNull(am);
        assertNotNull(im);
        assertEquals(1, im.size());
        assertTrue(am.size() > 0);
        assertEquals(im.keys()[0], KeyStroke.getKeyStroke('A', InputEvent.ALT_DOWN_MASK));
        Object actionName = im.get(im.keys()[0]);
        assertNotNull(am.get(actionName));
    }
}
