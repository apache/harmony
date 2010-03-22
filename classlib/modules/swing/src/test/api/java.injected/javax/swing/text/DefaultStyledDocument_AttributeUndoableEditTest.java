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

import java.util.Iterator;
import java.util.List;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.AttributeUndoableEdit;
import junit.framework.TestCase;

/**
 * Tests DefaultStyledDocument.AttributeUndoableEdit class
 *
 */
public class DefaultStyledDocument_AttributeUndoableEditTest extends TestCase implements
        UndoableEditListener {
    private AttributeUndoableEdit undoEdit;

    private DefaultStyledDocument doc;

    private static final AttributeSet italic = DefStyledDoc_Helpers.italic;

    private static final AttributeSet bold = DefStyledDoc_Helpers.bold;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        doc.insertString(0, "a text portion", italic);
        //                   01234567890123
        doc.addUndoableEditListener(this);
    }

    /**
     * Tests the functionality of <code>undo()</code>/<code>redo()</code>
     * when attributes are applied in non-replacing way.
     */
    public void testUndoRedoNoReplace() {
        doc.setCharacterAttributes(2, 4, bold, false);
        doc.writeLock();
        try {
            undoEdit.undo();
            AttributeSet attrs = doc.getCharacterElement(4).getAttributes();
            assertEquals(1, attrs.getAttributeCount());
            assertFalse(attrs.containsAttributes(bold));
            assertTrue(attrs.containsAttributes(italic));
            undoEdit.redo();
            attrs = doc.getCharacterElement(4).getAttributes();
            assertEquals(2, attrs.getAttributeCount());
            assertTrue(attrs.containsAttributes(bold));
            assertTrue(attrs.containsAttributes(italic));
        } finally {
            doc.writeUnlock();
        }
    }

    /**
     * Tests the functionality of <code>undo()</code>/<code>redo()</code>
     * when attribute added has another value of the same attribute.
     */
    public void testUndoRedoItalicFalse() {
        final MutableAttributeSet italicFalse = new SimpleAttributeSet();
        StyleConstants.setItalic(italicFalse, false);
        doc.setCharacterAttributes(2, 4, italicFalse, false);
        doc.writeLock();
        try {
            undoEdit.undo();
            AttributeSet attrs = doc.getCharacterElement(4).getAttributes();
            assertEquals(1, attrs.getAttributeCount());
            assertTrue(attrs.containsAttributes(italic));
            undoEdit.redo();
            attrs = doc.getCharacterElement(4).getAttributes();
            assertEquals(1, attrs.getAttributeCount());
            assertTrue(attrs.containsAttributes(italicFalse));
        } finally {
            doc.writeUnlock();
        }
    }

    /**
     * Tests the functionality of <code>undo()</code>/<code>redo()</code>
     * when attributes are replaced.
     */
    public void testUndoRedoWithReplace() {
        doc.setCharacterAttributes(2, 4, bold, true);
        doc.writeLock();
        try {
            undoEdit.undo();
            AttributeSet attrs = doc.getCharacterElement(4).getAttributes();
            assertEquals(1, attrs.getAttributeCount());
            assertFalse(attrs.containsAttributes(bold));
            assertTrue(attrs.containsAttributes(italic));
            undoEdit.redo();
            attrs = doc.getCharacterElement(4).getAttributes();
            assertEquals(1, attrs.getAttributeCount());
            assertTrue(attrs.containsAttributes(bold));
            assertFalse(attrs.containsAttributes(italic));
        } finally {
            doc.writeUnlock();
        }
    }

    /**
     * Tests the constructor of the class. The element has no attributes
     * in this test.
     */
    public void testAttributeUndoableEdit() {
        Element elem = new PlainDocument().getDefaultRootElement();
        AttributeSet attrs = new SimpleAttributeSet();
        undoEdit = new AttributeUndoableEdit(elem, attrs, true);
        assertSame(elem, undoEdit.element);
        assertSame(attrs, undoEdit.newAttributes);
        assertTrue(undoEdit.isReplacing);
        assertSame(SimpleAttributeSet.EMPTY, undoEdit.copy);
    }

    /**
     * Tests the constructor of the class. The element has
     * <code>bidiLevel</code> attributes in this test.
     */
    public void testAttributeUndoableEditBidiElement() {
        Element elem = new PlainDocument().getBidiRootElement().getElement(0);
        AttributeSet attrs = new SimpleAttributeSet();
        undoEdit = new AttributeUndoableEdit(elem, attrs, true);
        assertSame(elem, undoEdit.element);
        assertSame(attrs, undoEdit.newAttributes);
        assertTrue(undoEdit.isReplacing);
        assertEquals(1, undoEdit.copy.getAttributeCount());
        assertTrue(undoEdit.copy.containsAttribute(StyleConstants.BidiLevel, new Integer(0)));
    }

    /**
     * Adds attributes to a portion of text.
     * Asserts the AttributeUndoableEdit instance will have the expected values.
     * <p>It also checks the attributes of that text portion.
     */
    public void testChangeNoReplace() {
        doc.setCharacterAttributes(2, 4, bold, false);
        assertNotNull(undoEdit);
        assertSame(doc.getCharacterElement(4), undoEdit.element);
        assertTrue(undoEdit.newAttributes.isEqual(bold));
        assertFalse(undoEdit.isReplacing);
        assertTrue(undoEdit.copy.isEqual(italic));
        AttributeSet attrs = doc.getCharacterElement(4).getAttributes();
        assertEquals(2, attrs.getAttributeCount());
        assertTrue(attrs.containsAttributes(bold));
        assertTrue(attrs.containsAttributes(italic));
    }

    /**
     * Replaces attributes in a portion of text.
     * Asserts the AttributeUndoableEdit instance will have the expected values.
     * <p>It also checks the attributes of that text portion.
     */
    public void testChangeWithReplace() {
        doc.setCharacterAttributes(2, 4, bold, true);
        assertNotNull(undoEdit);
        assertSame(doc.getCharacterElement(4), undoEdit.element);
        assertTrue(undoEdit.newAttributes.isEqual(bold));
        assertTrue(undoEdit.isReplacing);
        assertTrue(undoEdit.copy.isEqual(italic));
        AttributeSet attrs = doc.getCharacterElement(4).getAttributes();
        assertEquals(1, attrs.getAttributeCount());
        assertTrue(attrs.containsAttributes(bold));
        assertFalse(attrs.containsAttributes(italic));
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        searchForAttributeUE((DefaultDocumentEvent) e.getEdit());
    }

    private void searchForAttributeUE(DefaultDocumentEvent e) {
        List<?> edits = DefStyledDoc_Helpers.getEdits(e);
        for (Iterator<?> i = edits.iterator(); i.hasNext();) {
            Object edit = i.next();
            if (edit instanceof AttributeUndoableEdit) {
                undoEdit = (AttributeUndoableEdit) edit;
                break;
            }
        }
    }
}
