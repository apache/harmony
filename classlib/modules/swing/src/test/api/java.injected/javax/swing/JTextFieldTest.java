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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
/*
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 */
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.View;

public class JTextFieldTest extends SwingTestCase {
    String sLTR = "abcd";

    String sRTL = "\u05dc" + "\u05dc" + "\u05dc" + "\u05dc";

    String bidiContent = sLTR + sRTL + sLTR + sRTL + sLTR + sRTL;

    boolean bWasException;

    String content = "British Air Line Pilots Association " + "British Academy "
            + "British Airways ";

    String fireOrder;

    JFrame jf;

    ExtJTextField jtf;

    JTextField jtfBidi;

    JTextField jtfScroll;

    SimplePropertyChangeListener listener;

    String message;

    class ExtJTextField extends JTextField {
        private static final long serialVersionUID = 1L;

        Action configureFrom = null;

        Action createFrom = null;

        boolean wasCallInvalidate = false;

        boolean wasCallRevalidate = false;

        boolean wasConfigureProperties = false;

        boolean wasCreateListeners = false;

        public ExtJTextField(final String s) {
            super(s);
        }

        @Override
        protected void configurePropertiesFromAction(final Action a) {
            wasConfigureProperties = true;
            configureFrom = a;
            super.configurePropertiesFromAction(a);
        }

        @Override
        protected PropertyChangeListener createActionPropertyChangeListener(final Action a) {
            wasCreateListeners = true;
            createFrom = a;
            return super.createActionPropertyChangeListener(a);
        }

        @Override
        public void invalidate() {
            wasCallInvalidate = true;
            super.invalidate();
        }

        void resetTestedFields() {
            wasConfigureProperties = false;
            wasCreateListeners = false;
            createFrom = null;
            configureFrom = null;
        }

        @Override
        public void revalidate() {
            wasCallRevalidate = true;
            super.revalidate();
        }
    }

    class SimpleActionListener implements ActionListener {
        ActionEvent actionEvent = null;

        String name;

        SimpleActionListener(final String s) {
            name = s;
        }

        public void actionPerformed(final ActionEvent ae) {
            actionEvent = ae;
            fireOrder += name;
        }
    }

    class SimpleChangeListener implements ChangeListener {
        public void stateChanged(final ChangeEvent arg0) {
        }
    }

    class SimplePropertyChangeListener implements PropertyChangeListener {
        PropertyChangeEvent event;

        PropertyChangeEvent getEvent() {
            PropertyChangeEvent e = event;
            event = null;
            return e;
        }

        public void propertyChange(final PropertyChangeEvent e) {
            if (e.getPropertyName() != "ancestor") {
                event = e;
            }
        }
    }

    private Dimension getPrefferedSize(final JTextField c) {
        int widthColumn = c.getColumns() * c.getColumnWidth();
        Dimension dim = c.getPreferredScrollableViewportSize();
        int width = Math.max(dim.width, widthColumn);
        return new Dimension(width, dim.height);
    }

    void resetBRM(final BoundedRangeModel brm, final int min, final int value, final int ext,
            final int max) {
        brm.setMinimum(min);
        brm.setValue(value);
        brm.setExtent(ext);
        brm.setMaximum(max);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        fireOrder = "";
        bWasException = false;
        message = null;
        listener = new SimplePropertyChangeListener();
        jf = new JFrame();
        Container container = jf.getContentPane();
        container.setLayout(new GridLayout(3, 1, 3, 4));
        jtf = new ExtJTextField(content);
        jtf.addPropertyChangeListener(listener);
        jtfBidi = new JTextField(bidiContent);
        jtfScroll = new JTextField(content);
        container.add(jtf);
        container.add(jtfBidi);
        container.add(new JScrollPane(jtfScroll));
        ((JViewport) jtfScroll.getParent()).setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testAccessibleJTextField() {
        AccessibleContext accessible = jtf.getAccessibleContext();
        assertTrue(accessible instanceof JTextField.AccessibleJTextField);
        assertEquals(jtf.getAccessibleContext(), accessible);
        JTextField.AccessibleJTextField access = (JTextField.AccessibleJTextField) accessible;
        AccessibleStateSet ass = access.getAccessibleStateSet();
        assertNotSame(ass, access.getAccessibleStateSet());
        assertTrue(ass.contains(AccessibleState.SINGLE_LINE));
        assertFalse(ass.contains(AccessibleState.MULTI_LINE));
    }

    public void testAddRemoveGetActionListener() {
        SimpleActionListener listener1 = new SimpleActionListener("first");
        SimpleActionListener listener2 = new SimpleActionListener("second");
        SimpleActionListener listener3 = new SimpleActionListener("third");
        ActionListener[] listeners = jtf.getActionListeners();
        assertEquals(0, listeners.length);
        jtf.addActionListener(listener1);
        listeners = jtf.getActionListeners();
        assertEquals(1, listeners.length);
        assertEquals(listener1, listeners[0]);
        jtf.addActionListener(listener2);
        listeners = jtf.getActionListeners();
        assertEquals(2, listeners.length);
        assertEquals(listener2, listeners[0]);
        assertEquals(listener1, listeners[1]);
        jtf.addActionListener(listener3);
        listeners = jtf.getActionListeners();
        assertEquals(3, listeners.length);
        assertEquals(listener3, listeners[0]);
        assertEquals(listener2, listeners[1]);
        assertEquals(listener1, listeners[2]);
        ActionListener listeners1[] = jtf.listenerList.getListeners(ActionListener.class);
        assertEquals(listener3, listeners1[0]);
        assertEquals(listener2, listeners1[1]);
        assertEquals(listener1, listeners1[2]);
        jtf.addActionListener(listener3);
        listeners = jtf.getActionListeners();
        assertEquals(4, listeners.length);
        assertEquals(listener3, listeners[0]);
        assertEquals(listener3, listeners[1]);
        assertEquals(listener2, listeners[2]);
        assertEquals(listener1, listeners[3]);
        jtf.removeActionListener(listener2);
        listeners = jtf.getActionListeners();
        assertEquals(3, listeners.length);
        assertEquals(listener3, listeners[0]);
        assertEquals(listener3, listeners[1]);
        assertEquals(listener1, listeners[2]);
        jtf.removeActionListener(listener1);
        listeners = jtf.getActionListeners();
        assertEquals(2, listeners.length);
        assertEquals(listener3, listeners[0]);
        assertEquals(listener3, listeners[1]);
        jtf.removeActionListener(listener2);
        listeners = jtf.getActionListeners();
        assertEquals(2, listeners.length);
        assertEquals(listener3, listeners[0]);
        assertEquals(listener3, listeners[1]);
        jtf.removeActionListener(listener3);
        listeners = jtf.getActionListeners();
        assertEquals(1, listeners.length);
        assertEquals(listener3, listeners[0]);
        jtf.removeActionListener(listener3);
        listeners = jtf.getActionListeners();
        assertEquals(0, listeners.length);
    }

    public void testConfigurePropertiesFromAction() {
        Action action = new DefaultEditorKit.CutAction();
        jtf.configurePropertiesFromAction(action);
        assertTrue(jtf.isEnabled());
        assertEquals(action.getValue(Action.SHORT_DESCRIPTION), jtf.getToolTipText());
        action.setEnabled(false);
        action.putValue(Action.SHORT_DESCRIPTION, "THIS IS TOOLTIPTEXT");
        jtf.configurePropertiesFromAction(action);
        assertFalse(jtf.isEnabled());
        assertEquals(action.getValue(Action.SHORT_DESCRIPTION), jtf.getToolTipText());
    }

    public void testCreateActionPropertyChangeListener() {
        Action action = new DefaultEditorKit.CutAction();
        PropertyChangeListener listener = jtf.createActionPropertyChangeListener(action);
        assertNotNull(listener);
        jtf.setAction(action);
        action.setEnabled(false);
        action.putValue(Action.SHORT_DESCRIPTION, "THIS IS TOOLTIPTEXT");
        assertFalse(jtf.isEnabled());
        assertEquals("THIS IS TOOLTIPTEXT", jtf.getToolTipText());
        jtf.setAction(null);
        action.setEnabled(true);
        action.putValue(Action.SHORT_DESCRIPTION, "THIS IS CHANGED TOOLTIPTEXT");
        assertTrue(jtf.isEnabled());
        assertNull(jtf.getToolTipText());
    }

    public void testCreateDefaultModel() {
        Document doc = jtf.createDefaultModel();
        Document doc1 = jtf.createDefaultModel();
        assertTrue(doc instanceof PlainDocument);
        assertNotSame(jtf.getDocument(), doc);
        assertNotSame(doc1, doc);
    }

    public void testFireActionPerformed() {
        SimpleActionListener listener1 = new SimpleActionListener("first");
        SimpleActionListener listener2 = new SimpleActionListener("second");
        SimpleActionListener listener3 = new SimpleActionListener("third");
        jtf.addActionListener(listener1);
        jtf.addActionListener(listener2);
        jtf.addActionListener(listener3);
        fireOrder = "";
        jtf.setAction(new DefaultEditorKit.BeepAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                fireOrder += "Action";
                super.actionPerformed(arg0);
            }
        });
        jtf.fireActionPerformed();
        assertEquals("Actionthirdsecondfirst", fireOrder);
    }

    public void testGetActions() {
        Action editorKitActions[] = jtf.getUI().getEditorKit(jtf).getActions();
        Action jtfActions[] = jtf.getActions();
        assertEquals(editorKitActions.length + 1, jtfActions.length);
        for (int i = 0; i < jtfActions.length; i++) {
            boolean wasEqual = false;
            for (int j = 0; j < editorKitActions.length; j++) {
                if (jtfActions[i] == editorKitActions[j]) {
                    wasEqual = true;
                    break;
                }
            }
            if (jtfActions[i].getValue(Action.NAME) == "notify-field-accept") {
                wasEqual = true;
            }
            assertTrue(wasEqual);
        }
    }

    public void testGetHorizontalVisibility() {
        BoundedRangeModel brm = jtf.getHorizontalVisibility();
        assertTrue(brm instanceof DefaultBoundedRangeModel);
        assertEquals(0, brm.getMinimum());
        int prefWidth = jtf.getPreferredSize().width;
        Insets insets = jtf.getInsets();
        int hrzInsets = insets.left + insets.right;
        int maxValue = prefWidth - hrzInsets;
        assertEquals(0, brm.getValue());
        assertEquals(maxValue, brm.getExtent());
        assertEquals(maxValue, brm.getMaximum());
        assertFalse(brm.getValueIsAdjusting());
        DefaultBoundedRangeModel dbrm = (DefaultBoundedRangeModel) brm;
        assertEquals(1, dbrm.listenerList.getListenerCount());
        dbrm.fireStateChanged();
        assertEquals(dbrm, dbrm.changeEvent.getSource());
        assertEquals(1, dbrm.getChangeListeners().length);
    }

    public void testGetPreferredSize() {
        assertEquals(jtf.getPreferredSize(), jtf.getPreferredScrollableViewportSize());
        jtf.setColumns(10);
        assertEquals(getPrefferedSize(jtf), jtf.getPreferredSize());
        jtf.setColumns(500);
        assertEquals(getPrefferedSize(jtf), jtf.getPreferredSize());
    }

    public void testGetUIClassID() {
        assertEquals("TextFieldUI", jtf.getUIClassID());
        assertEquals("TextFieldUI", jtfBidi.getUIClassID());
        assertEquals("TextFieldUI", jtfScroll.getUIClassID());
    }

    public void testIsValidateRoot() {
        assertTrue(jtf.isValidateRoot());
        assertTrue(jtfBidi.isValidateRoot());
        assertFalse(jtfScroll.isValidateRoot());
    }

    public void testJTextField() {
        JTextField tf = new JTextField();
        assertEquals("", tf.getText());
        assertTrue(tf.getDocument() instanceof PlainDocument);
        assertEquals(0, tf.getColumns());
    }

    public void testJTextFieldDocumentStringInt() {
        String str1 = "AAA";
        String str2 = "testJTextFieldDocumentStringInt()";
        Document doc = new PlainDocument();
        try {
            doc.insertString(0, str2, null);
        } catch (BadLocationException e) {
            assertTrue("Unexpected exception: " + e.getMessage(), false);
        }
        JTextField tf = new JTextField(doc, str2, 8);
        assertEquals(str2, tf.getText());
        assertEquals(doc, tf.getDocument());
        assertEquals(8, tf.getColumns());
        doc = new PlainDocument();
        try {
            doc.insertString(0, str2, null);
        } catch (BadLocationException e) {
            assertTrue("Unexpected exception: " + e.getMessage(), false);
        }
        tf = new JTextField(doc, null, 6);
        assertEquals(str2, tf.getText());
        tf = new JTextField(doc, "", 6);
        assertEquals("", tf.getText());
        try {
            tf = new JTextField(doc, str1, -1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("columns less than zero.", message);
    }

    public void testJTextFieldInt() {
        JTextField tf = new JTextField(5);
        assertEquals("", tf.getText());
        assertTrue(tf.getDocument() instanceof PlainDocument);
        assertEquals(5, tf.getColumns());
        try {
            tf = new JTextField(-1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("columns less than zero.", message);
    }

    public void testJTextFieldString() {
        String str1 = "testJTextFieldString()";
        JTextField tf = new JTextField(str1);
        assertEquals(str1, tf.getText());
        assertTrue(tf.getDocument() instanceof PlainDocument);
        assertEquals(0, tf.getColumns());
    }

    public void testJTextFieldStringInt() {
        String str1 = "testJTextFieldString()";
        JTextField tf = new JTextField(str1, 5);
        assertEquals(str1, tf.getText());
        assertTrue(tf.getDocument() instanceof PlainDocument);
        assertEquals(5, tf.getColumns());
        try {
            tf = new JTextField(-1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("columns less than zero.", message);
    }

    public void testNotifyAction() {
        assertEquals("notify-field-accept", JTextField.notifyAction);
    }

    //implementation dependent
    /*
     public void testParamString() {
     jtf.setActionCommand("ACTION_COMMAND");
     String str = "," +
     jtf.getX() + "," +
     jtf.getY() + "," +
     jtf.getSize().width + "x" + jtf.getSize().height + "," +
     "layout=" + jtf.getLayout() + "," ;
     str =str.replaceFirst("@[^,}]*","");
     str +=
     "alignmentX=" + "null"+ "," + //1.4.2
     "alignmentY=" + "null"+ "," + //1.4.2
     //"alignmentX=" + "0.0"+ "," + //1.5.0
     //"alignmentY=" + "0.0"+ "," + //1.5.0
     "border=" + jtf.getBorder() + "," +
     "flags=296" + "," +
     "maximumSize=,minimumSize=,preferredSize=," +
     "caretColor=" + jtf.getCaretColor() + "," +
     "disabledTextColor=" + jtf.getDisabledTextColor() + "," +
     "editable=" + jtf.isEditable() + "," +
     "margin=" + jtf.getMargin() + "," +
     "selectedTextColor=" + jtf.getSelectedTextColor() + "," +
     "selectionColor=" + jtf.getSelectionColor() +"," +
     "columns=" + jtf.getColumns() + "," +
     "columnWidth=" + jtf.getColumnWidth() + "," +
     "command=ACTION_COMMAND," +
     "horizontalAlignment=LEADING";
     assertEquals(changeString(str), changeString(jtf.paramString()));
     } */
    public void testPostActionEvent() {
        SimpleActionListener listener1 = new SimpleActionListener("first");
        SimpleActionListener listener2 = new SimpleActionListener("second");
        SimpleActionListener listener3 = new SimpleActionListener("third");
        jtf.addActionListener(listener1);
        jtf.addActionListener(listener2);
        jtf.addActionListener(listener3);
        fireOrder = "";
        jtf.setAction(new DefaultEditorKit.BeepAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                fireOrder += "Action";
                super.actionPerformed(arg0);
            }
        });
        jtf.postActionEvent();
        assertEquals("Actionthirdsecondfirst", fireOrder);
    }

    void testPropertyChangeEvent(final String name, final int oldValue, final int newValue,
            final PropertyChangeEvent event) {
        testPropertyChangeEvent(name, new Integer(oldValue), new Integer(newValue), event);
    }

    void testPropertyChangeEvent(final String name, final Object oldValue,
            final Object newValue, final PropertyChangeEvent event) {
        assertEquals(name, event.getPropertyName());
        assertEquals(oldValue, event.getOldValue());
        assertEquals(newValue, event.getNewValue());
    }

    public void testScrollRectToVisible() {
    }

    public void testSetActionCommand() {
        SimpleActionListener listener = new SimpleActionListener("");
        jtf.addActionListener(listener);
        jtf.fireActionPerformed();
        ActionEvent ae = listener.actionEvent;
        assertEquals(jtf.getText(), ae.getActionCommand());
        jtf.setText("LALALA");
        jtf.fireActionPerformed();
        ActionEvent ae1 = listener.actionEvent;
        assertEquals(jtf.getText(), ae1.getActionCommand());
        jtf.setActionCommand("JENJA");
        jtf.fireActionPerformed();
        ae = listener.actionEvent;
        assertEquals("JENJA", ae.getActionCommand());
        jtf.setActionCommand(null);
        jtf.fireActionPerformed();
        ae = listener.actionEvent;
        assertEquals(jtf.getText(), ae.getActionCommand());
        jtf.setActionCommand("");
        jtf.fireActionPerformed();
        ae = listener.actionEvent;
        assertEquals("", ae.getActionCommand());
    }

    public void testSetDocument() {
        Document old = jtf.getDocument();
        Document doc = new PlainDocument();
        jtf.setDocument(doc);
        testPropertyChangeEvent("document", old, doc, listener.event);
        assertEquals(Boolean.TRUE, doc.getProperty("filterNewlines"));
    }

    public void testSetFont() {
        Font oldFont = jtf.getFont();
        FontMetrics fm = jtf.getFontMetrics(oldFont);
        assertEquals(fm.charWidth('m'), jtf.getColumnWidth());
        jtf.wasCallRevalidate = false;
        Font newFont = new java.awt.Font("SimSun", 0, 12);
        jtf.setFont(newFont);
        assertTrue(jtf.wasCallRevalidate);
        fm = jtf.getFontMetrics(newFont);
        assertEquals(fm.charWidth('m'), jtf.getColumnWidth());
        //checks PropertyCchanegEvent
        PropertyChangeEvent event = listener.event;
        assertEquals("font", event.getPropertyName());
        assertEquals(oldFont, event.getOldValue());
        assertEquals(newFont, event.getNewValue());
    }

    public void testSetGetAction() {
        assertNull(jtf.getAction());
        Action action = new DefaultEditorKit.CutAction();
        jtf.setAction(action);
        assertEquals(action, jtf.getAction());
        assertTrue(jtf.wasConfigureProperties);
        assertTrue(jtf.wasCreateListeners);
        assertEquals(action, jtf.configureFrom);
        assertEquals(action, jtf.createFrom);
        assertEquals(1, jtf.getActionListeners().length);
        assertEquals(action, jtf.getActionListeners()[0]);
        jtf.resetTestedFields();
        jtf.setAction(null);
        assertNull(jtf.getAction());
        assertTrue(jtf.wasConfigureProperties);
        assertFalse(jtf.wasCreateListeners);
        assertNull(jtf.configureFrom);
        assertNull(jtf.createFrom);
        assertEquals(0, jtf.getActionListeners().length);
        jtf.resetTestedFields();
        jtf.addActionListener(action);
        jtf.setAction(action);
        assertEquals(action, jtf.getAction());
        assertTrue(jtf.wasConfigureProperties);
        assertTrue(jtf.wasCreateListeners);
        assertEquals(action, jtf.configureFrom);
        assertEquals(action, jtf.createFrom);
        assertEquals(1, jtf.getActionListeners().length);
        assertEquals(action, jtf.getActionListeners()[0]);
    }

    public void testSetGetColumns() {
        assertEquals(0, jtf.getColumns());
        jtf.wasCallInvalidate = false;
        jtf.setColumns(5);
        assertEquals(5, jtf.getColumns());
        assertTrue(jtf.wasCallInvalidate);
        try {
            jtf.setColumns(-5);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("columns less than zero.", message);
    }

    public void testSetGetHorizontalAlignment() {
        assertEquals(SwingConstants.LEADING, jtf.getHorizontalAlignment());
        jtf.setHorizontalAlignment(SwingConstants.LEFT);
        assertEquals(SwingConstants.LEFT, jtf.getHorizontalAlignment());
        testPropertyChangeEvent("horizontalAlignment", SwingConstants.LEADING,
                SwingConstants.LEFT, listener.event);
        jtf.setHorizontalAlignment(SwingConstants.CENTER);
        assertEquals(SwingConstants.CENTER, jtf.getHorizontalAlignment());
        testPropertyChangeEvent("horizontalAlignment", SwingConstants.LEFT,
                SwingConstants.CENTER, listener.event);
        jtf.setHorizontalAlignment(SwingConstants.RIGHT);
        assertEquals(SwingConstants.RIGHT, jtf.getHorizontalAlignment());
        testPropertyChangeEvent("horizontalAlignment", SwingConstants.CENTER,
                SwingConstants.RIGHT, listener.event);
        jtf.setHorizontalAlignment(SwingConstants.LEADING);
        assertEquals(SwingConstants.LEADING, jtf.getHorizontalAlignment());
        testPropertyChangeEvent("horizontalAlignment", SwingConstants.RIGHT,
                SwingConstants.LEADING, listener.event);
        jtf.setHorizontalAlignment(SwingConstants.TRAILING);
        assertEquals(SwingConstants.TRAILING, jtf.getHorizontalAlignment());
        testPropertyChangeEvent("horizontalAlignment", SwingConstants.LEADING,
                SwingConstants.TRAILING, listener.event);
        try {
            jtf.setHorizontalAlignment(5000);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("horizontalAlignment", message);
    }

    int getInitialScrollOffest(final JTextField c) {
        int prefWidth = c.getPreferredSize().width;
        int width = c.getWidth();
        int diff = prefWidth - width;
        return (diff >= 0) ? diff + 1 : 0;
    }

    int getScrollOffset(final JTextField c, final int scrollOffset) {
        int prefWidth = c.getPreferredSize().width;
        int width = c.getWidth();
        int diff = prefWidth - width;
        int maxScrollOffset = (diff >= 0) ? diff + 1 : 0;
        return Math.min(Math.max(0, scrollOffset), maxScrollOffset);
    }

    void brmTest(final BoundedRangeModel model, final int min, final int value,
            final int extent, final int max) {
        assertEquals(min, model.getMinimum());
        assertEquals(value, model.getValue());
        assertEquals(extent, model.getExtent());
        assertEquals(max, model.getMaximum());
    }

    public void testSetGetScrollOffset() {
        assertEquals(getInitialScrollOffest(jtf), jtf.getScrollOffset());
        //assertEquals(getInitialScrollOffest(jtfScroll),jtfScroll.getScrollOffset());
        assertEquals(getInitialScrollOffest(jtfBidi), jtfBidi.getScrollOffset());
        BoundedRangeModel brm = jtf.getHorizontalVisibility();
        int brm_min = brm.getMinimum();
        int brm_extent = brm.getExtent();
        int brm_max = brm.getMaximum();
        BoundedRangeModel brmScroll = jtfScroll.getHorizontalVisibility();
        int brmScroll_min = brmScroll.getMinimum();
        int brmScroll_extent = brmScroll.getExtent();
        int brmScroll_max = brmScroll.getMaximum();
        BoundedRangeModel brmBidi = jtfBidi.getHorizontalVisibility();
        int brmBidi_min = brmBidi.getMinimum();
        int brmBidi_extent = brmBidi.getExtent();
        int brmBidi_max = brmBidi.getMaximum();
        if (!isHarmony()) {
            return;
        }
        for (int i = -3; i < 500; i++) {
            jtf.setScrollOffset(i);
            assertEquals(getScrollOffset(jtf, i), jtf.getScrollOffset());
            brmTest(brm, brm_min, jtf.getScrollOffset(), brm_extent, brm_max);
            jtfScroll.setScrollOffset(i);
            assertEquals(getScrollOffset(jtfScroll, i), jtfScroll.getScrollOffset());
            brmTest(brmScroll, brmScroll_min, jtfScroll.getScrollOffset(), brmScroll_extent,
                    brmScroll_max);
            jtfBidi.setScrollOffset(i);
            assertEquals(getScrollOffset(jtfScroll, i), jtfScroll.getScrollOffset());
            brmTest(brmBidi, brmBidi_min, jtfBidi.getScrollOffset(), brmBidi_extent,
                    brmBidi_max);
        }
    }

    // Regression for HARMONY-2627
    public void testGetScrollOffset() {
        jtf = new ExtJTextField("abc");
        final int viewWidth = (int)jtf.getUI().getRootView(jtf)
                                   .getPreferredSpan(View.X_AXIS);

        assertEquals(viewWidth + 4, jtf.getPreferredSize().width);
        assertEquals(0, jtf.getScrollOffset());
        assertEquals(viewWidth + 4 + 1, getInitialScrollOffest(jtf));
    }

    public void testSerialization() {
        /*
         JTextField jt = new JTextField(bidiContent);
         jt.setColumns(8);
         jt.setFont(new java.awt.Font("SimSun", 0, 12));
         Action action  = new DefaultEditorKit.CopyAction();
         //jt.setAction(action);


         JTextField jt1 = new JTextField();
         try {
         FileOutputStream fo = new FileOutputStream("tmp");
         ObjectOutputStream so = new ObjectOutputStream(fo);
         so.writeObject(jt);
         so.flush();
         so.close();
         FileInputStream fi = new FileInputStream("tmp");
         ObjectInputStream si = new ObjectInputStream(fi);
         jt1 = (JTextField) si.readObject();
         si.close();
         } catch (Exception e) {
         assertTrue("seralization failed" + e.getMessage(),false);
         }
         assertEquals(bidiContent, jt1.getText());
         assertEquals(8,jt1.getColumns());
         assertEquals(new java.awt.Font("SimSun", 0, 12),jt1.getFont());
         assertNotSame(jt.getHorizontalVisibility(), jt1.getHorizontalVisibility());
         assertEquals(jt.getScrollOffset(), jt1.getScrollOffset());
         assertEquals(jt.getColumnWidth(), jt1.getColumnWidth());
         assertNotSame(jt.getAccessibleContext(), jt1.getAccessibleContext());
         */
    }
}
