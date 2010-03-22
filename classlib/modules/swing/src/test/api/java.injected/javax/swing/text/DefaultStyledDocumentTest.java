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
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.LeafElement;
import javax.swing.text.DefaultStyledDocument.SectionElement;

public class DefaultStyledDocumentTest extends BasicSwingTestCase {
    protected DefaultStyledDocument doc;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument(new StyleContext());
    }

    /**
     * Tests <code>addDocumentListener</code> and
     * <code>removeDocumentListener</code> methods.
     */
    public void testAddRemoveDocumentListener() throws BadLocationException {
        final class Listener implements DocumentListener {
            private boolean insert;

            private boolean remove;

            private boolean change;

            public void insertUpdate(DocumentEvent e) {
                insert = true;
            }

            public void removeUpdate(DocumentEvent e) {
                remove = true;
            }

            public void changedUpdate(DocumentEvent e) {
                change = true;
            }

            public void check(final boolean insert, final boolean remove, final boolean change) {
                assertEquals("Insert", insert, this.insert);
                assertEquals("Remove", remove, this.remove);
                assertEquals("Change", change, this.change);
                this.insert = this.remove = this.change = false;
            }
        }
        ;
        final Listener listener = new Listener();
        final AttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontSize((MutableAttributeSet) attrs, 36);
        // The listener has not been added to the document yet
        doc.insertString(0, "1", null);
        listener.check(false, false, false);
        doc.remove(0, 1);
        listener.check(false, false, false);
        doc.setCharacterAttributes(0, 1, attrs, false);
        listener.check(false, false, false);
        // The listener has been added to the document
        doc.addDocumentListener(listener);
        doc.insertString(0, "1", null);
        listener.check(true, false, false);
        doc.remove(0, 1);
        listener.check(false, true, false);
        doc.setCharacterAttributes(0, 1, attrs, false);
        listener.check(false, false, true);
        // The listener has been removed from the document
        doc.removeDocumentListener(listener);
        doc.insertString(0, "1", null);
        listener.check(false, false, false);
        doc.remove(0, 1);
        listener.check(false, false, false);
        doc.setCharacterAttributes(0, 1, attrs, false);
        listener.check(false, false, false);
    }

    public void testGetDefaultRootElement() {
        assertTrue(doc.getDefaultRootElement() instanceof SectionElement);
    }

    /*
     * DefaultStyledDocument()
     */
    public void testDefaultStyledDocument() {
        doc = new DefaultStyledDocument();
        assertEquals(DefaultStyledDocument.BUFFER_SIZE_DEFAULT, ((GapContent) doc.getContent())
                .getArrayLength());
    }

    /*
     * DefaultStyledDocument(AbstractDocument.Content, StyleContext)
     */
    public void testDefaultStyledDocumentContentStyleContext() {
        StyleContext styles = new StyleContext();
        doc = new DefaultStyledDocument(new GapContent(10), styles);
        assertEquals(10, ((GapContent) doc.getContent()).getArrayLength());
        Element root = doc.getDefaultRootElement();
        assertTrue(root instanceof SectionElement);
        assertEquals(1, root.getElementCount());
        Element child = root.getElement(0);
        assertTrue(child instanceof BranchElement);
        assertEquals(1, child.getElementCount());
        assertTrue(child.getElement(0) instanceof LeafElement);
        assertSame(styles, doc.getAttributeContext());
        assertSame(styles.getStyle(StyleContext.DEFAULT_STYLE), child.getAttributes()
                .getResolveParent());
    }

    /*
     * DefaultStyledDocument(StyleContext)
     */
    public void testDefaultStyledDocumentStyleContext() {
        StyleContext styles = new StyleContext();
        doc = new DefaultStyledDocument(styles);
        DefaultStyledDocument anotherDoc = new DefaultStyledDocument(styles);
        assertSame(doc.getAttributeContext(), anotherDoc.getAttributeContext());
    }

    public void testCreateDefaultRoot() {
        AbstractElement defRoot = doc.createDefaultRoot();
        assertTrue(defRoot instanceof SectionElement);
        assertEquals(0, defRoot.getAttributeCount());
        assertEquals(1, defRoot.getElementCount());
        assertTrue(defRoot.getElement(0) instanceof BranchElement);
    }

    /**
     * Tests AbstractDocument.insertString() in respect to handling
     * of filterNewLine property.
     * <p><i>The property has no effect.</i></p>
     */
    public void testInsertString() throws BadLocationException {
        final String content = "one\ntwo\nthree";
        final String filterNewLinesProperty = "filterNewlines";
        doc.insertString(0, content, null);
        assertNull(getNewLineProperty());
        assertEquals(content, getText());
        doc.remove(0, doc.getLength());
        doc.putProperty(filterNewLinesProperty, Boolean.TRUE);
        doc.insertString(0, content, null);
        assertSame(Boolean.TRUE, getNewLineProperty());
        assertEquals(content, getText());
        doc.remove(0, doc.getLength());
        doc.putProperty(filterNewLinesProperty, Boolean.FALSE);
        doc.insertString(0, content, null);
        assertSame(Boolean.FALSE, getNewLineProperty());
        assertEquals(content, getText());
    }

    public void testSerializable() throws Exception {
        final String text = "some sample text";
        doc.insertString(0, text, DefStyledDoc_Helpers.bold);
        doc = (DefaultStyledDocument) BasicSwingTestCase.serializeObject(doc);
        final Element root = doc.getDefaultRootElement();
        assertEquals(1, root.getElementCount());
        final Element paragraph = root.getElement(0);
        assertEquals(2, paragraph.getElementCount());
        final Element character = paragraph.getElement(0);
        assertEquals(0, character.getStartOffset());
        assertEquals(text.length(), character.getEndOffset());
        assertTrue(character.getAttributes().isEqual(DefStyledDoc_Helpers.bold));
        assertEquals(text.length() + 1, paragraph.getEndOffset());
        assertEquals(text, doc.getText(0, doc.getLength()));
        // Check that ElementBuffer is also functional
        doc.writeLock();
        try {
            doc.buffer.change(2, 6, doc.new DefaultDocumentEvent(2, 6, EventType.CHANGE));
            assertEquals(4, paragraph.getElementCount());
            doc.buffer.remove(2, 6, doc.new DefaultDocumentEvent(2, 6, EventType.REMOVE));
            assertEquals(3, paragraph.getElementCount());
        } finally {
            doc.writeUnlock();
        }
        doc.insertString(0, "1\n2\n3\n", null);
        for (int i = 0; i < root.getElementCount(); i++) {
            Element branch = root.getElement(i);
            assertEquals("root.children[" + i + "].attributes.count", 1, branch.getAttributes()
                    .getAttributeCount());
            assertNotNull("root.children[" + i + "].attributes.getResolver()", branch
                    .getAttributes().getResolveParent());
        }
    }

    private Object getNewLineProperty() {
        return doc.getProperty("filterNewlines");
    }

    private String getText() throws BadLocationException {
        return doc.getText(0, doc.getLength());
    }
    /*
     These methods are not tested because they are tests by their side effects:
     insertUpdate prepares ElementSpecs and calls buffer.insert()
     removeUpdate has nothing to do except calling buffer.remove()
     public void testInsertUpdate() {
     }

     public void testRemoveUpdate() {
     }
     */
}
