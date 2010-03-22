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
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;

public class JTextAreaTest extends SwingTestCase {
    JFrame jf;

    ExtJTextArea jta;

    JTextArea bidiJta;

    JTextArea jt;

    String sLTR = "aaaa";

    String sRTL = "\u05dc" + "\u05dc" + "\u05dc" + "\u05dc";

    String content = "Edison accumul\tator, Edison base: Edison battery"
            + " Edison cap, \tEdison effect\n"
            + "Edison screw, Edison screw cap, Edison screw \n"
            + "holder, Edison screw lampholder, Edison screw " + "plug\n"
            + "Edison screw terminal, Edison storage battery" + "Edison storage \t\tcell";

    String bidiContent = sLTR + sRTL + sRTL + " \t" + sLTR + sRTL + sLTR + "\n" + sRTL + "."
            + sLTR + sRTL + "\t" + sRTL + "\n" + sLTR + sLTR + sRTL + sRTL + sRTL + sLTR + sLTR
            + sLTR + sRTL + sLTR + sRTL + sLTR;

    String str1 = "jazz band";

    String str2 = "jazz model";

    boolean bWasException;

    String message;

    SimplePropertyChangeListener listener;

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

    void assertEqualsPropertyChangeEvent(final String name, final Object oldValue,
            final Object newValue, final PropertyChangeEvent e) {
        assertEquals(name, e.getPropertyName());
        assertEquals(oldValue, e.getOldValue());
        assertEquals(newValue, e.getNewValue());
    }

    void isElement(final Object[] a, final Object b, final int count) {
        assertNotNull(a);
        boolean cond = false;
        int k = 0;
        for (int j = 0; j < a.length; j++) {
            if (a[j] == b) {
                cond = true;
                k += 1;
            }
        }
        assertTrue(cond);
        assertEquals(count, k);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        listener = new SimplePropertyChangeListener();
        bWasException = false;
        message = null;
        jf = new JFrame();
        bidiJta = new JTextArea(bidiContent);
        jta = new ExtJTextArea(content);
        jt = new JTextArea(bidiContent);
        jta.addPropertyChangeListener(listener);
        Container cont = jf.getContentPane();
        cont.setLayout(new GridLayout(1, 2, 4, 4));
        cont.add(jta);
        cont.add(bidiJta);
        jf.setLocation(200, 300);
        jf.setSize(350, 400);
        jf.pack();
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    public void testGetScrollableTracksViewportWidth() throws Exception {
        assertFalse(jta.getScrollableTracksViewportWidth());
        jta.setLineWrap(true);
        assertTrue(jta.getScrollableTracksViewportWidth());
    }

    public void testJTextAreaDocumentStringintint() {
        Document doc = new PlainDocument();
        try {
            doc.insertString(0, str2, null);
            ExtJTextArea ta = new ExtJTextArea(doc, str1, 3, 8);
            assertEquals(str1, ta.getText());
            assertEquals(doc, ta.getDocument());
            assertEquals(3, ta.getRows());
            assertEquals(8, ta.getColumns());
            assertFalse(ta.getLineWrap());
            assertFalse(ta.getWrapStyleWord());
            doc = new PlainDocument();
            doc.insertString(0, str2, null);
            ta = new ExtJTextArea(doc, null, 5, 6);
            assertEquals(str2, ta.getText());
            ta = new ExtJTextArea(doc, "", 5, 6);
            assertEquals("", ta.getText());
            try {
                ta = new ExtJTextArea(doc, str1, -1, 4);
            } catch (IllegalArgumentException e) {
                bWasException = true;
                message = e.getMessage();
            }
            assertTrue(bWasException);
            assertEquals("rows: -1", message);
            bWasException = false;
            message = null;
            try {
                ta = new ExtJTextArea(doc, str2, 1, -3);
            } catch (IllegalArgumentException e) {
                bWasException = true;
                message = e.getMessage();
            }
            assertTrue(bWasException);
            assertEquals("columns: -3", message);
        } catch (BadLocationException e) {
            assertTrue("Unexpected exception: " + e.getMessage(), false);
        }
    }

    public void testJTextAreaDocument() {
        Document doc = new PlainDocument();
        try {
            doc.insertString(0, str2, null);
        } catch (BadLocationException e) {
            assertTrue("Unexpected exception: " + e.getMessage(), false);
        }
        ExtJTextArea ta = new ExtJTextArea(doc);
        assertEquals(str2, ta.getText());
        assertEquals(doc, ta.getDocument());
        assertEquals(0, ta.getRows());
        assertEquals(0, ta.getColumns());
        assertFalse(ta.getLineWrap());
        assertFalse(ta.getWrapStyleWord());
    }

    public void testJTextAreaStringintint() {
        ExtJTextArea ta = new ExtJTextArea(str1, 3, 6);
        assertEquals(str1, ta.getText());
        assertTrue(ta.getDocument() instanceof PlainDocument);
        assertEquals(3, ta.getRows());
        assertEquals(6, ta.getColumns());
        assertFalse(ta.getLineWrap());
        assertFalse(ta.getWrapStyleWord());
        ta = new ExtJTextArea(null, 5, 6);
        assertEquals("", ta.getText());
        try {
            ta = new ExtJTextArea(str1, -1, 4);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("rows: -1", message);
        bWasException = false;
        message = null;
        try {
            ta = new ExtJTextArea(str2, 1, -3);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("columns: -3", message);
    }

    public void testJTextAreaString() {
        ExtJTextArea ta = new ExtJTextArea(str1);
        assertEquals(str1, ta.getText());
        assertTrue(ta.getDocument() instanceof PlainDocument);
        assertEquals(0, ta.getRows());
        assertEquals(0, ta.getColumns());
        assertFalse(ta.getLineWrap());
        assertFalse(ta.getWrapStyleWord());
    }

    public void testJTextAreaintint() {
        ExtJTextArea ta = new ExtJTextArea(5, 6);
        assertEquals("", ta.getText());
        assertTrue(ta.getDocument() instanceof PlainDocument);
        assertEquals(5, ta.getRows());
        assertEquals(6, ta.getColumns());
        assertFalse(ta.getLineWrap());
        assertFalse(ta.getWrapStyleWord());
        try {
            ta = new ExtJTextArea(-1, 4);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("rows: -1", message);
        bWasException = false;
        message = null;
        try {
            ta = new ExtJTextArea(1, -3);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("columns: -3", message);
    }

    public void testJTextArea() {
        ExtJTextArea ta = new ExtJTextArea();
        assertEquals("", ta.getText());
        assertTrue(ta.getDocument() instanceof PlainDocument);
        assertEquals(0, ta.getRows());
        assertEquals(0, ta.getColumns());
        assertFalse(ta.getLineWrap());
        assertFalse(ta.getWrapStyleWord());
    }

    public void testCreateDefaultModel() {
        Document doc = jta.createDefaultModel();
        Document doc1 = jta.createDefaultModel();
        assertTrue(doc instanceof PlainDocument);
        assertNotSame(jta.getDocument(), doc);
        assertNotSame(doc1, doc);
    }

    public void testGetAccessibleContext() {
        AccessibleContext accessible = jta.getAccessibleContext();
        assertTrue(accessible instanceof JTextArea.AccessibleJTextArea);
        assertEquals(jta.getAccessibleContext(), accessible);
        JTextArea.AccessibleJTextArea access = (JTextArea.AccessibleJTextArea) accessible;
        AccessibleStateSet ass = access.getAccessibleStateSet();
        assertNotSame(ass, access.getAccessibleStateSet());
        assertTrue(ass.contains(AccessibleState.MULTI_LINE));
    }

    String replaceRange(final String s1, final String s2, final int start, final int end) {
        String tmp = s2 == null ? "" : s2;
        return s1.substring(0, start) + tmp + s1.substring(end, s1.length());
    }

    public void testReplaceRange() throws Exception {
        String tmp;
        jta.replaceRange(str1, 5, 10);
        tmp = replaceRange(content, str1, 5, 10);
        assertEquals(tmp, jta.getText());
        jta.replaceRange(null, 5, 10);
        tmp = replaceRange(tmp, null, 5, 10);
        assertEquals(tmp, jta.getText());
        jta.replaceRange("", 5, 10);
        tmp = replaceRange(tmp, "", 5, 10);
        assertEquals(tmp, jta.getText());
        try {
            jta.replaceRange(str2, -1, 5);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid remove", message);
        bWasException = false;
        message = null;
        try {
            jta.replaceRange(str2, 1, tmp.length() + 1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid remove", message);
        bWasException = false;
        message = null;
        try {
            jta.replaceRange(str2, 10, 5);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("end before start", message);
        jta.replaceRange(str2, 7, 14);
        tmp = replaceRange(tmp, str2, 7, 14);
        assertEquals(tmp, jta.getText());
        jta.replaceRange(null, 8, 17);
        tmp = replaceRange(tmp, null, 8, 17);
        assertEquals(tmp, jta.getText());
        jta.replaceRange("", 5, 12);
        tmp = replaceRange(tmp, "", 5, 12);
        assertEquals(tmp, jta.getText());
    }

    String insertString(final String s1, final String s2, final int index) {
        return s1.substring(0, index) + s2 + s1.substring(index, s1.length());
    }

    public void testInsert() throws Exception {
        String tmp;
        jta.insert(str1, 5);
        tmp = insertString(content, str1, 5);
        assertEquals(tmp, jta.getText());
        jta.insert(null, 5);
        assertEquals(tmp, jta.getText());
        jta.insert("", 5);
        assertEquals(tmp, jta.getText());
        try {
            jta.insert(str2, -1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid insert", message);
        bWasException = false;
        message = null;
        try {
            jta.insert(str2, tmp.length() + 1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid insert", message);
        bWasException = false;
        message = null;
        try {
            jta.insert(null, tmp.length() + 1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertFalse(bWasException);
        assertNull(message);
        jta.insert(str2, 7);
        tmp = insertString(tmp, str2, 7);
        assertEquals(tmp, jta.getText());
        jta.insert(null, 7);
        assertEquals(tmp, jta.getText());
        jta.insert("", 7);
        assertEquals(tmp, jta.getText());
    }

    public void testAppend() throws Exception {
        String tmp;
        jta.append(str1);
        tmp = content + str1;
        assertEquals(tmp, jta.getText());
        jta.append(null);
        assertEquals(tmp, jta.getText());
        jta.append("");
        assertEquals(tmp, jta.getText());
        jta.append(str2);
        tmp = content + str1 + str2;
        assertEquals(tmp, jta.getText());
        jta.append(null);
        assertEquals(tmp, jta.getText());
        jta.append("");
        assertEquals(tmp, jta.getText());
    }

    // Implementation dependent
    /*
     public void testParamString() {
     String str = "," + jta.getX() + "," + jta.getY() + ","
     + jta.getSize().width + "x" + jta.getSize().height + ","
     + "layout=" + jta.getLayout() + ",";
     str = str.replaceFirst("@[^,}]*", "");
     str +=
     "alignmentX=" + "null"+ "," + //1.4.2
     "alignmentY=" + "null"+ "," + //1.4.2
     //"alignmentX=" + "0.0" + "," + //1.5.0
     //        "alignmentY=" + "0.0" + "," + //1.5.0
     "border=" + jta.getBorder() + "," + "flags=296" + ","
     + "maximumSize=,minimumSize=,preferredSize=," + "caretColor="
     + jta.getCaretColor() + "," + "disabledTextColor="
     + jta.getDisabledTextColor() + "," + "editable="
     + jta.isEditable() + "," + "margin=" + jta.getMargin() + ","
     + "selectedTextColor=" + jta.getSelectedTextColor() + ","
     + "selectionColor=" + jta.getSelectionColor() + ","
     + "columns=" + jta.getColumns() + "," + "columnWidth="
     + jta.getColumnWidth() + "," + "rows=" + jta.getRows() + ","
     + "rowHeight=" + jta.getRowHeight() + "," + "word="
     + jta.getWrapStyleWord() + "," + "wrap=" + jta.getLineWrap();
     assertEquals(changeString(str), changeString(jta.paramString()));
     } */
    String changeString(final String s) {
        return s.replaceFirst("layout[^,]*,", "").replaceFirst("flag[^,]*,", "");
    }

    public void testGetUIClassID() {
        assertEquals("TextAreaUI", jta.getUIClassID());
        assertEquals("TextAreaUI", bidiJta.getUIClassID());
    }

    public void testGetScrollableUnitIncrement() throws Exception {
        assertEquals(jta.getColumnWidth(), jta.getScrollableUnitIncrement(null,
                SwingConstants.HORIZONTAL, -1));
        assertEquals(jta.getColumnWidth(), jta.getScrollableUnitIncrement(null,
                SwingConstants.HORIZONTAL, 0));
        assertEquals(jta.getColumnWidth(), jta.getScrollableUnitIncrement(null,
                SwingConstants.HORIZONTAL, 1));
        assertEquals(jta.getRowHeight(), jta.getScrollableUnitIncrement(null,
                SwingConstants.VERTICAL, -1));
        assertEquals(jta.getRowHeight(), jta.getScrollableUnitIncrement(null,
                SwingConstants.VERTICAL, 0));
        assertEquals(jta.getRowHeight(), jta.getScrollableUnitIncrement(null,
                SwingConstants.VERTICAL, 1));
        try {
            jta.getScrollableUnitIncrement(null, 3, 1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Invalid orientation: 3", message);
        Font newFont = new java.awt.Font("SimSun", 0, 12);
        jta.setFont(newFont);
        assertEquals(jta.getColumnWidth(), jta.getScrollableUnitIncrement(null,
                SwingConstants.HORIZONTAL, -1));
        assertEquals(jta.getColumnWidth(), jta.getScrollableUnitIncrement(null,
                SwingConstants.HORIZONTAL, 0));
        assertEquals(jta.getColumnWidth(), jta.getScrollableUnitIncrement(null,
                SwingConstants.HORIZONTAL, 1));
        assertEquals(jta.getRowHeight(), jta.getScrollableUnitIncrement(null,
                SwingConstants.VERTICAL, -1));
        assertEquals(jta.getRowHeight(), jta.getScrollableUnitIncrement(null,
                SwingConstants.VERTICAL, 0));
        assertEquals(jta.getRowHeight(), jta.getScrollableUnitIncrement(null,
                SwingConstants.VERTICAL, 1));
    }

    public void testSetFont() throws Exception {
        Font oldFont = jta.getFont();
        FontMetrics fm = jta.getFontMetrics(oldFont);
        assertEquals(fm.getHeight(), jta.getRowHeight());
        assertEquals(fm.charWidth('m'), jta.getColumnWidth());
        jta.wasCallRevalidate = false;
        Font newFont = new java.awt.Font("SimSun", 0, 12);
        jta.setFont(newFont);
        assertTrue(jta.wasCallRevalidate);
        assertEqualsPropertyChangeEvent("font", oldFont, newFont, listener.event);
        fm = jta.getFontMetrics(newFont);
        assertEquals(fm.getHeight(), jta.getRowHeight());
        assertEquals(fm.charWidth('m'), jta.getColumnWidth());
    }

    Dimension getPrefferedSize(final JTextArea jta) {
        Dimension dim1 = jta.getPreferredScrollableViewportSize();
        int width1 = dim1.width;
        int height1 = dim1.height;
        Dimension dim2 = jta.getUI().getPreferredSize(jta);
        int width2 = dim2.width;
        int height2 = dim2.height;
        return new Dimension(Math.max(width1, width2), Math.max(height1, height2));
    }

    public void testGetPreferredSize() throws Exception {
        assertEquals(jta.getPreferredSize(), jta.getPreferredScrollableViewportSize());
        jta.setColumns(5);
        jta.setRows(2);
        assertEquals(getPrefferedSize(jta), jta.getPreferredSize());
        jta.setColumns(500);
        jta.setRows(1);
        assertEquals(getPrefferedSize(jta), jta.getPreferredSize());
        jta.setColumns(1);
        jta.setRows(500);
        assertEquals(getPrefferedSize(jta), jta.getPreferredSize());
        jta.setColumns(500);
        jta.setRows(200);
        assertEquals(getPrefferedSize(jta), jta.getPreferredSize());
    }

    public void testGetPreferredScrollableViewportSize() throws Exception {
        assertEquals(jta.getPreferredSize(), jta.getPreferredScrollableViewportSize());
        jta.setColumns(5);
        jta.setRows(2);
        Dimension dim = new Dimension(5 * jta.getColumnWidth(), 2 * jta.getRowHeight());
        assertEquals(dim, jta.getPreferredScrollableViewportSize());
        jta.setColumns(500);
        jta.setRows(200);
        dim = new Dimension(500 * jta.getColumnWidth(), 200 * jta.getRowHeight());
        assertEquals(dim, jta.getPreferredScrollableViewportSize());
        jta.setColumns(0);
        jta.setRows(200);
        assertEquals(new Dimension(jta.getPreferredSize().width, 200 * jta.getRowHeight()), jta
                .getPreferredScrollableViewportSize());
        jta.setColumns(1);
        jta.setRows(0);
        assertEquals(new Dimension(1 * jta.getColumnWidth(), jta.getPreferredSize().height),
                jta.getPreferredScrollableViewportSize());
    }

    public void testGetLineStartEndOffset() {
        AbstractDocument doc = (AbstractDocument) jta.getDocument();
        int count = jta.getLineCount();
        doc.readLock();
        Element root = doc.getDefaultRootElement();
        for (int j = 0; j < count; j++) {
            Element currentElement = root.getElement(j);
            try {
                assertEquals(currentElement.getStartOffset(), jta.getLineStartOffset(j));
                int end = currentElement.getEndOffset();
                if (j == count - 1) {
                    end--;
                }
                assertEquals(end, jta.getLineEndOffset(j));
            } catch (BadLocationException e) {
            }
        }
        doc.readUnlock();
        try {
            jta.getLineStartOffset(count);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("No such line", message);
        bWasException = false;
        message = null;
        try {
            jta.getLineStartOffset(5000);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("No such line", message);
        bWasException = false;
        message = null;
        try {
            jta.getLineStartOffset(-1);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Negative line", message);
        bWasException = false;
        message = null;
        try {
            jta.getLineEndOffset(count);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("No such line", message);
        bWasException = false;
        message = null;
        try {
            jta.getLineEndOffset(5000);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("No such line", message);
        bWasException = false;
        message = null;
        try {
            jta.getLineEndOffset(-1);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Negative line", message);
    }

    public void testGetLineOfOffset() {
        AbstractDocument doc = (AbstractDocument) jta.getDocument();
        int length = doc.getLength();
        doc.readLock();
        Element root = doc.getDefaultRootElement();
        for (int j = 0; j < length; j++) {
            try {
                assertEquals(root.getElementIndex(j), jta.getLineOfOffset(j));
            } catch (BadLocationException e) {
            }
        }
        doc.readUnlock();
        try {
            jta.getLineOfOffset(length + 1);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Can't translate offset to line", message);
        bWasException = false;
        message = null;
        try {
            jta.getLineOfOffset(5000);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Can't translate offset to line", message);
        bWasException = false;
        message = null;
        try {
            jta.getLineOfOffset(-1);
        } catch (BadLocationException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("Can't translate offset to line", message);
        bWasException = false;
        message = null;
    }

    public void testSerialization() throws Exception {
        /*
         jt.setRows(6);
         jt.setColumns(8);
         jt.setLineWrap(true);
         jt.setWrapStyleWord(true);
         jt.setFont(new java.awt.Font("SimSun", 0, 12));

         JTextArea jta1 = new JTextArea();
         FileOutputStream fo = new FileOutputStream("tmp");
         ObjectOutputStream so = new ObjectOutputStream(fo);
         so.writeObject(jt);
         so.flush();
         so.close();
         FileInputStream fi = new FileInputStream("tmp");
         ObjectInputStream si = new ObjectInputStream(fi);
         jta1 = (JTextArea) si.readObject();
         si.close();

         assertTrue(jta1.getLineWrap());
         assertTrue(jta1.getWrapStyleWord());
         assertEquals(bidiContent, jta1.getText());
         assertEquals(6, jta1.getRows());
         assertEquals(8, jta1.getColumns());
         assertEquals(new java.awt.Font("SimSun", 0, 12), jta1.getFont());
         */
    }

    public void testSetGetWrapStyleWord() throws Exception {
        assertFalse(jta.getWrapStyleWord());
        jta.setWrapStyleWord(true);
        assertTrue(jta.getWrapStyleWord());
        assertEqualsPropertyChangeEvent("wrapStyleWord", Boolean.FALSE, Boolean.TRUE,
                listener.event);
    }

    public void testSetGetLineWrap() throws Exception {
        assertFalse(jta.getLineWrap());
        jta.setLineWrap(true);
        assertTrue(jta.getLineWrap());
        assertEqualsPropertyChangeEvent("lineWrap", Boolean.FALSE, Boolean.TRUE, listener.event);
    }

    public void testSetGetTabSize() throws Exception {
        assertEquals(8, jta.getTabSize());
        assertEquals(new Integer(8), jta.getDocument().getProperty("tabSize"));
        jta.setTabSize(5);
        assertEquals(5, jta.getTabSize());
        assertEquals(new Integer(5), jta.getDocument().getProperty("tabSize"));
        assertEquals(new Integer(5), jta.getDocument().getProperty("tabSize"));
        assertEqualsPropertyChangeEvent("tabSize", new Integer(8), new Integer(5),
                listener.event);
        jta.setTabSize(-2);
        assertEquals(new Integer(-2), jta.getDocument().getProperty("tabSize"));
        assertEquals(-2, jta.getTabSize());
    }

    public void testSetGetRows() throws Exception {
        assertEquals(0, jta.getRows());
        jta.wasCallInvalidate = false;
        jta.setRows(6);
        assertEquals(6, jta.getRows());
        assertTrue(jta.wasCallInvalidate);
        try {
            jta.setRows(-1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("rows less than zero.", message);
    }

    public void testGetLineCount() {
        AbstractDocument doc_jta = (AbstractDocument) jta.getDocument();
        doc_jta.readLock();
        assertEquals(jta.getLineCount(), doc_jta.getDefaultRootElement().getElementCount());
        doc_jta.readUnlock();
        doc_jta = (AbstractDocument) bidiJta.getDocument();
        doc_jta.readLock();
        assertEquals(bidiJta.getLineCount(), doc_jta.getDefaultRootElement().getElementCount());
        doc_jta.readUnlock();
    }

    public void testSetGetColumns() throws Exception {
        assertEquals(0, jta.getColumns());
        jta.wasCallInvalidate = false;
        jta.setColumns(6);
        assertEquals(6, jta.getColumns());
        assertTrue(jta.wasCallInvalidate);
        try {
            jta.setColumns(-1);
        } catch (IllegalArgumentException e) {
            bWasException = true;
            message = e.getMessage();
        }
        assertTrue(bWasException);
        assertEquals("columns less than zero.", message);
    }
}