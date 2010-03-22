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
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.AbstractDocument.LeafElement;
import junit.framework.TestCase;

/**
 * Tests functionality of PlainDocument class.
 *
 */
public class PlainDocumentTest extends TestCase {
    private static final String filterNewLinesProperty = "filterNewlines";

    private static final String paragraphs = "first\nsecond\nthird\n";

    //                                        01234 5678901 234567 8
    /**
     * Shared document used in tests.
     */
    private PlainDocument doc;

    /**
     * Event for text insert.
     */
    private DefaultDocumentEvent insert;

    /**
     * Event for text remove.
     */
    private DefaultDocumentEvent remove;

    /**
     * Root element where changes are tracked.
     */
    private Element root;

    /**
     * Changes of the root element
     */
    private ElementChange change;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void insertUpdate(final DefaultDocumentEvent event,
                    final AttributeSet attrs) {
                insert = event;
                super.insertUpdate(event, attrs);
            }

            @Override
            protected void removeUpdate(final DefaultDocumentEvent event) {
                remove = event;
                super.removeUpdate(event);
            }
        };
        root = doc.getDefaultRootElement();
    }

    /**
     * General text insertion checks.
     * Includes checks for lineLimitAttribute property.
     */
    public void testInsertString01() throws BadLocationException {
        doc.putProperty(PlainDocument.lineLimitAttribute, new Integer(10));
        StyleContext context = StyleContext.getDefaultStyleContext();
        AttributeSet attrs = context.addAttribute(context.getEmptySet(), "key", "value");
        final String text = "0123456789abcdefghij";
        doc.insertString(0, text, attrs);
        assertEquals(text, doc.getText(0, doc.getLength()));
        assertEquals(3, doc.getDocumentProperties().size());
        attrs = doc.getDefaultRootElement().getAttributes();
        assertEquals(0, root.getAttributes().getAttributeCount());
        assertEquals(1, root.getElementCount());
        Element line1 = root.getElement(0);
        assertTrue(line1.isLeaf());
        assertEquals(0, line1.getElementCount());
        assertEquals(0, line1.getAttributes().getAttributeCount());
    }

    /**
     * Tests handling of filterNewLine property.
     */
    public void testInsertString02() throws BadLocationException {
        final String content = "one\ntwo\nthree";
        doc.insertString(0, content, null);
        assertNull(getNewLineProperty());
        assertEquals(content, getText());
        doc.remove(0, doc.getLength());
        doc.putProperty(filterNewLinesProperty, Boolean.TRUE);
        doc.insertString(0, content, null);
        assertSame(Boolean.TRUE, getNewLineProperty());
        assertEquals(content.replace('\n', ' '), getText());
        doc.remove(0, doc.getLength());
        doc.putProperty(filterNewLinesProperty, Boolean.FALSE);
        doc.insertString(0, content, null);
        assertSame(Boolean.FALSE, getNewLineProperty());
        assertEquals(content, getText());
    }

    /**
     * Inserts three lines of text into empty document.
     */
    public void testInsertUpdate01() throws BadLocationException {
        doc.insertString(0, "text", null);
        assertNull(insert.getChange(root));
        doc.insertString(0, paragraphs, null);
        change = insert.getChange(root);
        assertEquals(4, change.getChildrenAdded().length);
        assertEquals(1, change.getChildrenRemoved().length);
        assertEquals(4, root.getElementCount());
        Element[] children = change.getChildrenAdded();
        checkOffsets(children[0], 0, 6);
        checkOffsets(children[1], 6, 13);
        checkOffsets(children[2], 13, 19);
        checkOffsets(children[3], 19, 24);
    }

    /**
     * Inserts text (not contains line breaks) into start of a line.
     */
    public void testInsertUpdate02() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        doc.insertString(13, "123", null);
        change = insert.getChange(root);
        assertEquals(2, change.getChildrenAdded().length);
        assertEquals(2, change.getChildrenRemoved().length);
        assertEquals(1, change.getIndex());
        assertEquals(4, root.getElementCount());
        Element[] children = change.getChildrenAdded();
        checkOffsets(children[0], 6, 13);
        checkOffsets(children[1], 13, 22);
        children = change.getChildrenRemoved();
        checkOffsets(children[0], 6, 16);
        checkOffsets(children[1], 16, 22);
    }

    /**
     * Inserts text (does not contain line breaks) into end
     * (just before the break) of a line and into a position in a line.
     */
    public void testInsertUpdate03() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        doc.insertString(12, "123", null);
        assertNull(insert.getChange(root));
        doc.insertString(10, "middle", null);
        assertNull(insert.getChange(root));
    }

    /**
     * Inserts text (contains text only before line break)
     * into a position in a line (neither start, nor end).
     */
    public void testInsertUpdate04() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        // Insert more text
        doc.insertString(15, "two and a half\n", null);
        change = insert.getChange(root);
        assertEquals(2, change.getChildrenAdded().length);
        assertEquals(1, change.getChildrenRemoved().length);
        assertEquals(2, change.getIndex());
        assertEquals(5, root.getElementCount());
        Element[] children = change.getChildrenAdded();
        checkOffsets(children[0], 13, 30);
        checkOffsets(children[1], 30, 34);
        children = change.getChildrenRemoved();
        checkOffsets(children[0], 13, 34);
    }

    /**
     * Inserts text (contains text before and after line break)
     * into a position in a line (neither start, nor end).
     */
    public void testInsertUpdate05() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        doc.insertString(13, "123", null);
        // Insert more text
        doc.insertString(16, "two and a half\n321", null);
        change = insert.getChange(root);
        assertEquals(2, change.getChildrenAdded().length);
        assertEquals(1, change.getChildrenRemoved().length);
        assertEquals(2, change.getIndex());
        assertEquals(5, root.getElementCount());
        Element[] children = change.getChildrenAdded();
        checkOffsets(children[0], 13, 31);
        checkOffsets(children[1], 31, 40);
        children = change.getChildrenRemoved();
        checkOffsets(children[0], 13, 40);
    }

    /**
     * Inserts text (contains text only after line break)
     * into a position in a line (neither start, nor end).
     */
    public void testInsertUpdate06() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        // Insert more text
        doc.insertString(15, "\n321", null);
        change = insert.getChange(root);
        assertEquals(2, change.getChildrenAdded().length);
        assertEquals(1, change.getChildrenRemoved().length);
        assertEquals(2, change.getIndex());
        assertEquals(5, root.getElementCount());
        Element[] children = change.getChildrenAdded();
        checkOffsets(children[0], 13, 16);
        checkOffsets(children[1], 16, 23);
        children = change.getChildrenRemoved();
        checkOffsets(children[0], 13, 23);
    }

    /**
     * Inserts text (contains text only after line break)
     * into start a line (neither start, nor end).
     */
    public void testInsertUpdate07() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        doc.insertString(13, "\n321", null);
        change = insert.getChange(root);
        assertEquals(3, change.getChildrenAdded().length);
        assertEquals(2, change.getChildrenRemoved().length);
        assertEquals(1, change.getIndex());
        assertEquals(5, root.getElementCount());
        Element[] children = change.getChildrenAdded();
        checkOffsets(children[0], 6, 13);
        checkOffsets(children[1], 13, 14);
        checkOffsets(children[2], 14, 23);
        children = change.getChildrenRemoved();
        checkOffsets(children[0], 6, 17);
        checkOffsets(children[1], 17, 23);
    }

    public void testInsertUpdate08() throws BadLocationException {
        final int[] createCalled = new int[2];
        doc = new PlainDocument() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void insertUpdate(final DefaultDocumentEvent event,
                    final AttributeSet attrs) {
                insert = event;
                super.insertUpdate(event, attrs);
            }

            @Override
            protected Element createBranchElement(final Element parent, final AttributeSet attrs) {
                ++createCalled[0];
                return super.createBranchElement(parent, attrs);
            }

            @Override
            protected Element createLeafElement(final Element parent, final AttributeSet as,
                    final int start, final int end) {
                ++createCalled[1];
                return super.createLeafElement(parent, as, start, end);
            }
        };
        root = doc.getDefaultRootElement();
        createCalled[0] = createCalled[1] = 0;
        doc.insertString(0, "test", null);
        assertNull(insert.getChange(root));
        assertEquals("createBranchElement", 0, createCalled[0]);
        assertEquals("createLeafElement", 0, createCalled[1]);
        doc.insertString(0, "\n", null);
        assertNotNull(insert.getChange(root));
        assertEquals("createBranchElement", 0, createCalled[0]);
        assertEquals("createLeafElement", 2, createCalled[1]);
    }

    /**
     * Removes several lines at once and then portion of text within
     * a line (not on line boundary)
     */
    public void testRemoveUpdate01() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        // Remove "second\nthird\n" portion
        doc.remove(6, 13);
        change = remove.getChange(root);
        assertEquals(1, change.getChildrenAdded().length);
        assertEquals(3, change.getChildrenRemoved().length);
        assertEquals(2, root.getElementCount());
        checkOffsets(root.getElement(0), 0, 6);
        checkOffsets(root.getElement(1), 6, 7);
        // Remove "irs" from "first"
        doc.remove(1, 3);
        assertNull(remove.getChange(root));
        assertEquals("ft\n", doc.getText(0, doc.getLength()));
    }

    /**
     * Removes text just after line break.
     */
    public void testRemoveUpdate02() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        // Remove "se" from "second"
        doc.remove(6, 2);
        assertNull(remove.getChange(root));
    }

    /**
     * Removes text just before line break.
     */
    public void testRemoveUpdate03() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        // Remove "st" from "first\n"
        doc.remove(3, 2);
        assertNull(remove.getChange(root));
    }

    /**
     * Test calling remove with invalid parameters.
     */
    public void testRemoveUpdate04() {
        try {
            doc.remove(3, 5);
            fail("BadLocationException should be thrown");
        } catch (BadLocationException e) {
        }
        assertNull(remove);
    }

    public void testRemoveUpdate05() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        // Remove the first line break
        doc.remove(5, 1);
        change = remove.getChange(root);
        assertEquals(1, change.getChildrenAdded().length);
        assertEquals(2, change.getChildrenRemoved().length);
        assertEquals(3, root.getElementCount());
        assertEquals("firstsecond\nthird\n", doc.getText(0, doc.getLength()));
        //            01234567890 123456 7
        checkOffsets(root.getElement(0), 0, 12);
        checkOffsets(root.getElement(1), 12, 18);
        checkOffsets(root.getElement(2), 18, 19);
    }

    // Regression for HARMONY-1797
    public void testRemoveUpdate06() throws Exception {
        doc = new PlainDocument();
        insert = doc.new DefaultDocumentEvent(3, 13, EventType.INSERT);

        doc.removeUpdate(insert); // No exception should be thrown 
    }

    /**
     * Tests getParagraphElement when calling on empty document.
     */
    public void testGetParagraphElement01() {
        checkOffsets(doc.getParagraphElement(-1), 0, 1);
        checkOffsets(doc.getParagraphElement(0), 0, 1);
        checkOffsets(doc.getParagraphElement(1), 0, 1);
        checkOffsets(doc.getParagraphElement(2), 0, 1);
        checkOffsets(doc.getParagraphElement(9), 0, 1);
    }

    /**
     * Tests getParagraphElement when calling on non-empty document.
     */
    public void testGetParagraphElement02() throws BadLocationException {
        doc.insertString(0, paragraphs, null);
        checkOffsets(doc.getParagraphElement(-1), 0, 6);
        checkOffsets(doc.getParagraphElement(0), 0, 6);
        checkOffsets(doc.getParagraphElement(2), 0, 6);
        checkOffsets(doc.getParagraphElement(8), 6, 13);
        checkOffsets(doc.getParagraphElement(15), 13, 19);
        checkOffsets(doc.getParagraphElement(18), 13, 19);
        checkOffsets(doc.getParagraphElement(19), 19, 20);
        checkOffsets(doc.getParagraphElement(20), 19, 20);
        checkOffsets(doc.getParagraphElement(25), 19, 20);
    }

    public void testGetDefaultRootElement() {
        Element root = doc.getDefaultRootElement();
        assertTrue(root instanceof BranchElement);
        assertSame(AbstractDocument.ParagraphElementName, root.getName());
    }

    /*
     * Class under test for void PlainDocument(AbstractDocument.Content)
     */
    public void testPlainDocumentContent() {
        doc = new PlainDocument(new GapContent(30));
        Content content = doc.getContent();
        assertTrue(content instanceof GapContent);
        GapContent gapContent = (GapContent) content;
        assertEquals(29, gapContent.getGapEnd() - gapContent.getGapStart());
        Object tabSize = doc.getProperty(PlainDocument.tabSizeAttribute);
        assertEquals(8, ((Integer) tabSize).intValue());
        assertNull(doc.getProperty(PlainDocument.lineLimitAttribute));
        assertNotNull(doc.getDefaultRootElement());
    }

    /*
     * Class under test for void PlainDocument()
     */
    public void testPlainDocument() {
        doc = new PlainDocument();
        Content content = doc.getContent();
        assertTrue(content instanceof GapContent);
        GapContent gapContent = (GapContent) content;
        assertEquals(9, gapContent.getGapEnd() - gapContent.getGapStart());
        Object tabSize = doc.getProperty(PlainDocument.tabSizeAttribute);
        assertEquals(8, ((Integer) tabSize).intValue());
        assertNull(doc.getProperty(PlainDocument.lineLimitAttribute));
        assertNotNull(doc.getDefaultRootElement());
    }

    /**
     * Generic assertions upon element return by createDefaultRoot.
     */
    public void testCreateDefaultRoot01() throws BadLocationException {
        Element root = doc.createDefaultRoot();
        assertTrue(root instanceof BranchElement);
        assertEquals(AbstractDocument.ParagraphElementName, root.getName());
        assertEquals(1, root.getElementCount());
        checkOffsets(root.getElement(0), 0, 1);
        doc.insertString(0, paragraphs, null);
        root = doc.createDefaultRoot();
        assertEquals(1, root.getElementCount());
        if (BasicSwingTestCase.isHarmony()) {
            checkOffsets(root.getElement(0), doc.getStartPosition().getOffset(), doc
                    .getEndPosition().getOffset());
        } else {
            checkOffsets(root.getElement(0), 0, 1);
        }
    }

    /**
     * Tests what will happen if <code>createDefaultRoot</code> will not
     * return instance of <code>AbstractDocument.BranchElement</code> but
     * something else, for example <code>AbstractDocument.LeafElement</code>.
     */
    public void testCreateDefaultRoot02() throws BadLocationException {
        PlainDocument testDoc = null;
        try {
            testDoc = new PlainDocument() {
                private static final long serialVersionUID = 1L;

                @Override
                protected AbstractElement createDefaultRoot() {
                    AbstractElement root = new LeafElement(null, null, 0, 1);
                    return root;
                }
            };
            if (BasicSwingTestCase.isHarmony()) {
                fail("ClassCastException must be thrown");
            }
        } catch (ClassCastException e) {
        }
        if (BasicSwingTestCase.isHarmony()) {
            assertNull(testDoc);
            return;
        }
        AbstractElement root = (AbstractElement) testDoc.getDefaultRootElement();
        assertTrue(root instanceof LeafElement);
        assertEquals(AbstractDocument.ContentElementName, root.getName());
        assertEquals(0, root.getElementCount());
        assertFalse(root.getAllowsChildren());
        // Try to insert some text
        try {
            testDoc.insertString(0, "text", null);
            fail("ClassCastException must be thrown");
        } catch (ClassCastException e) {
        }
    }

    public void testCreateDefaultRoot03() {
        final boolean[] createCalled = new boolean[2];
        doc = new PlainDocument() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Element createBranchElement(final Element parent, final AttributeSet attrs) {
                createCalled[0] = true;
                return super.createBranchElement(parent, attrs);
            }

            @Override
            protected Element createLeafElement(final Element parent, final AttributeSet as,
                    final int start, final int end) {
                createCalled[1] = true;
                return super.createLeafElement(parent, as, start, end);
            }
        };
        createCalled[0] = createCalled[1] = false;
        doc.createDefaultRoot();
        assertTrue("createBranchElement is NOT called", createCalled[0]);
        assertTrue("createLeafElement is NOT called", createCalled[1]);
    }

    private Object getNewLineProperty() {
        return doc.getProperty(filterNewLinesProperty);
    }

    private String getText() throws BadLocationException {
        return doc.getText(0, doc.getLength());
    }

    private static void checkOffsets(final Element par, final int start, final int end) {
        assertEquals(start, par.getStartOffset());
        assertEquals(end, par.getEndOffset());
    }
}
