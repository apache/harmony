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
 * @author Alexey A. Ivanov
 */
package javax.swing.text;

import javax.swing.BasicSwingTestCase;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.ElementChange;

public class PlainDocumentRTest extends BasicSwingTestCase implements DocumentListener {
    private static final String filterNewLinesProperty = "filterNewlines";

    private PlainDocument doc;

    private Element root;

    private DocumentEvent insertEvent;

    private ElementChange change;

    /**
     * Tests handling of <code>filterNewLine</code> property
     * while using <code>replace</code>.
     */
    public void testReplace() throws BadLocationException {
        final String content = "one\ntwo\nthree";
        doc.replace(0, 0, content, null);
        assertNull(getNewLineProperty());
        assertEquals(content, getText());
        setNewLineProperty(true);
        doc.replace(0, doc.getLength(), content, null);
        assertSame(Boolean.TRUE, getNewLineProperty());
        assertEquals(content.replace('\n', ' '), getText());
        doc.remove(0, doc.getLength());
        setNewLineProperty(false);
        doc.replace(0, doc.getLength(), content, null);
        assertSame(Boolean.FALSE, getNewLineProperty());
        assertEquals(content, getText());
    }

    /**
     * Tests the use of <code>filterNewlines</code> property set on
     * a <code>PlainDocument</code> in the presence of
     * a <code>DocumentFilter</code>.
     */
    public void testReplaceDF() throws BadLocationException {
        DocumentFilter filter = new DocumentFilter() {
            @Override
            public void insertString(final FilterBypass fb, final int offset,
                    final String text, final AttributeSet attrs) throws BadLocationException {
                super.insertString(fb, offset, "###\n^^^", attrs);
            }

            @Override
            public void remove(final FilterBypass fb, final int offset, final int length)
                    throws BadLocationException {
                super.remove(fb, offset, length);
            }

            @Override
            public void replace(final FilterBypass fb, final int offset, final int length,
                    final String text, final AttributeSet attrs) throws BadLocationException {
                super.replace(fb, offset, length, "!" + text + "!", attrs);
            }
        };
        doc.insertString(0, "plain text", null);
        doc.setDocumentFilter(filter);
        setNewLineProperty(true);
        doc.replace(2, 6, "++\n--", null);
        assertEquals(isHarmony() ? "pl!++ --!xt" : "pl!++\n--!xt", getText());
        doc.remove(0, doc.getLength());
        doc.insertString(0, "^^^\n###", null);
        assertEquals(isHarmony() ? "### ^^^" : "###\n^^^", getText());
    }

    public void testInsertUpdate09() throws BadLocationException {
        doc.insertString(0, "line1\n\n\n\n", null);
        //                   012345 6 7 8
        doc.addDocumentListener(this);
        doc.insertString(7, "\n", null);
        change = insertEvent.getChange(root);
        assertEquals(1, change.getChildrenRemoved().length);
        assertEquals(2, change.getChildrenAdded().length);
        checkOffsets(change.getChildrenRemoved()[0], 6, 8);
        checkOffsets(change.getChildrenAdded()[0], 6, 7);
        checkOffsets(change.getChildrenAdded()[1], 7, 8);
    }

    public void testInsertUpdate10() throws BadLocationException {
        doc.insertString(0, "line1\n\n1\n\n", null);
        //                   012345 6 78 9
        doc.addDocumentListener(this);
        doc.insertString(7, "\n", null);
        change = insertEvent.getChange(root);
        assertEquals(1, change.getChildrenRemoved().length);
        assertEquals(2, change.getChildrenAdded().length);
        checkOffsets(change.getChildrenRemoved()[0], 6, 8);
        checkOffsets(change.getChildrenAdded()[0], 6, 7);
        checkOffsets(change.getChildrenAdded()[1], 7, 8);
    }

    public void testInsertUpdate11() throws BadLocationException {
        doc.insertString(0, "line1\n\n1\n\n", null);
        //                   012345 6 78 9
        doc.addDocumentListener(this);
        doc.insertString(8, "\n", null);
        change = insertEvent.getChange(root);
        assertEquals(1, change.getChildrenRemoved().length);
        assertEquals(2, change.getChildrenAdded().length);
        checkOffsets(change.getChildrenRemoved()[0], 7, 10);
        checkOffsets(change.getChildrenAdded()[0], 7, 9);
        checkOffsets(change.getChildrenAdded()[1], 9, 10);
    }

    public void insertUpdate(DocumentEvent e) {
        insertEvent = e;
    }

    public void removeUpdate(DocumentEvent e) {
    }

    public void changedUpdate(DocumentEvent e) {
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        root = doc.getDefaultRootElement();
    }

    private static void checkOffsets(final Element line, final int start, final int end) {
        assertEquals(start, line.getStartOffset());
        assertEquals(end, line.getEndOffset());
    }

    private void setNewLineProperty(final boolean state) {
        doc.putProperty(filterNewLinesProperty, Boolean.valueOf(state));
    }

    private Object getNewLineProperty() {
        return doc.getProperty(filterNewLinesProperty);
    }

    private String getText() throws BadLocationException {
        return doc.getText(0, doc.getLength());
    }
}
