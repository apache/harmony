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

import java.awt.font.TextAttribute;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.AbstractDocument.ElementEdit;
import junit.framework.TestCase;

public class AbstractDocument_UpdateTest extends TestCase {
    /**
     * Event for last insertString.
     */
    DefaultDocumentEvent insert;

    /**
     * Event for last remove.
     */
    DefaultDocumentEvent remove;

    /**
     * Element edits for <code>root</code> ocurred when text was inserted.
     */
    ElementEdit insertEdit;

    /**
     * Element edits for <code>root</code> ocurred when text was removed.
     */
    ElementEdit removeEdit;

    /**
     * Root element for which element edits are tracked.
     */
    Element root;

    private AbstractDocument doc;

    /**
     * String with three characters with right-to-left reading order.
     */
    static final String RTL = "\u05DC\u05DD\u05DE";

    /**
     * String with three characters with left-to-right reading order.
     */
    static final String LTR = "abc";

    /**
     * String with three digits.
     */
    static final String DIG = "012";

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
                assertTrue(event.isInProgress());
                // Get edits for root (bidi) element
                insertEdit = (ElementEdit) insert.getChange(root);
            }

            /**
             * Overridden to catch first phase of remove update.
             */
            @Override
            protected void removeUpdate(final DefaultDocumentEvent event) {
                remove = event;
                // Assert there's no paragraph changes so far
                assertNull(remove.getChange(doc.getDefaultRootElement()));
                // Call PlainDocument.removeUpdate to fulfil processing
                super.removeUpdate(event);
                assertTrue(event.isInProgress());
            }

            @Override
            protected void postRemoveUpdate(final DefaultDocumentEvent event) {
                // Assert the event passed here is the same passed
                //     to removeUpdate
                assertSame(remove, event);
                // Assert there's no bidi structure changes so far
                assertNull(remove.getChange(root));
                super.postRemoveUpdate(event);
                assertTrue(event.isInProgress());
                // Get edit for root (bidi) element
                removeEdit = (ElementEdit) remove.getChange(root);
            }
        };
        // Use bidiRoot by default however it may be changed but
        // all the test-methods must be updated
        root = doc.getBidiRootElement();
    }

    /**
     * Returns bidi level of the element.
     * @param e element to get bidi level from
     * @return the bidi level
     */
    private static int getBidiLevel(final Element e) {
        return StyleConstants.getBidiLevel(e.getAttributes());
    }

    /**
     * LTR text is inserted while default direction of doc is LTR
     */
    public void testInsertUpdate01() throws BadLocationException {
        assertNull(doc.getProperty(TextAttribute.RUN_DIRECTION));
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        doc.insertString(0, LTR, null);
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        assertNull(insertEdit);
    }

    /**
     * RTL text is inserted while default direction of doc is LTR
     */
    public void testInsertUpdate02() throws BadLocationException {
        assertNull(doc.getProperty(TextAttribute.RUN_DIRECTION));
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        doc.insertString(0, RTL, null);
        assertEquals(1, root.getElementCount());
        assertEquals(1, getBidiLevel(root.getElement(0)));
        assertEquals(1, insertEdit.getChildrenAdded().length);
        assertEquals(1, getBidiLevel(insertEdit.getChildrenAdded()[0]));
        assertEquals(1, insertEdit.getChildrenRemoved().length);
        assertEquals(0, getBidiLevel(insertEdit.getChildrenRemoved()[0]));
        assertEquals(0, insertEdit.getIndex());
    }

    /**
     * DIG is inserted while default direction of doc is LTR
     */
    public void testInsertUpdate03() throws BadLocationException {
        assertNull(doc.getProperty(TextAttribute.RUN_DIRECTION));
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        doc.insertString(0, DIG, null);
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        assertNull(insertEdit);
    }

    /**
     * LTR+RTL text is inserted while default direction of doc is LTR
     */
    public void testInsertUpdate04() throws BadLocationException {
        assertNull(doc.getProperty(TextAttribute.RUN_DIRECTION));
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        doc.insertString(0, LTR + RTL, null);
        // LTR...RTL...\n (the latter is LTR either)
        assertEquals(3, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        assertEquals(1, getBidiLevel(root.getElement(1)));
        assertEquals(0, getBidiLevel(root.getElement(2)));
        assertEquals(3, insertEdit.getChildrenAdded().length);
        assertEquals(1, insertEdit.getChildrenRemoved().length);
    }

    /**
     * RTL+LTR text is inserted while default direction of doc is LTR
     */
    public void testInsertUpdate05() throws BadLocationException {
        assertNull(doc.getProperty(TextAttribute.RUN_DIRECTION));
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        doc.insertString(0, RTL + LTR, null);
        assertEquals(3, root.getElementCount());
        assertEquals(1, getBidiLevel(root.getElement(0)));
        assertEquals(2, getBidiLevel(root.getElement(1)));
        assertEquals(1, getBidiLevel(root.getElement(2)));
        assertEquals(3, insertEdit.getChildrenAdded().length);
        assertEquals(1, insertEdit.getChildrenRemoved().length);
    }

    /**
     * DIG+RTL text is inserted while default direction of doc is LTR
     */
    public void testInsertUpdate06() throws BadLocationException {
        assertNull(doc.getProperty(TextAttribute.RUN_DIRECTION));
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        doc.insertString(0, DIG + RTL, null);
        assertEquals(2, root.getElementCount());
        assertEquals(2, getBidiLevel(root.getElement(0)));
        assertEquals(1, getBidiLevel(root.getElement(1)));
        assertEquals(2, insertEdit.getChildrenAdded().length);
        assertEquals(1, insertEdit.getChildrenRemoved().length);
    }

    /**
     * RTL+DIG text is inserted while default direction of doc is LTR
     */
    public void testInsertUpdate07() throws BadLocationException {
        assertNull(doc.getProperty(TextAttribute.RUN_DIRECTION));
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        doc.insertString(0, RTL + DIG, null);
        assertEquals(3, root.getElementCount());
        assertEquals(1, getBidiLevel(root.getElement(0)));
        assertEquals(2, getBidiLevel(root.getElement(1)));
        assertEquals(1, getBidiLevel(root.getElement(2)));
        assertEquals(3, insertEdit.getChildrenAdded().length);
        assertEquals(1, insertEdit.getChildrenRemoved().length);
    }

    /**
     * Checks that bidi levels, and start and end offsets are equal to
     * the expected ones
     * @param root root element whose children are to compare
     * @param levels expected bidi levels
     * @param bounds expected start and end offsets
     */
    private static void checkLevelsAndBounds(final Element root, final int[] levels,
            final int[] bounds) {
        for (int i = 0; i < levels.length; i++) {
            Element element = root.getElement(i);
            int level = getBidiLevel(element);
            assertEquals("Levels different at " + i, levels[i], level);
            int start = element.getStartOffset();
            int end = element.getEndOffset();
            assertEquals("Start offset different at " + i, bounds[i], start);
            assertEquals("End offset different at " + i, bounds[i + 1], end);
        }
    }

    /**
     * LTR + RTL + DIG\nRTL + DIG + LTR\nDIG + RTL text is inserted
     * while default direction of doc is LTR
     */
    public void testInsertUpdate08() throws BadLocationException {
        assertNull(doc.getProperty(TextAttribute.RUN_DIRECTION));
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        // Init document with two paragraphs of text
        doc.insertString(0, LTR + RTL + DIG + "\n" + RTL + DIG + LTR + "\n", null);
        // Check the document has the expected structure
        assertEquals(8, root.getElementCount());
        checkLevelsAndBounds(root, new int[] { 0, 1, 2, 0, 1, 2, 1, 0 }, new int[] { 0, 3, 6,
                9, 10, 13, 19, 20, 21 });
        assertEquals(8, insertEdit.getChildrenAdded().length);
        assertEquals(1, insertEdit.getChildrenRemoved().length);
        assertEquals(0, insertEdit.getIndex());
        // Add some more text at the end of document content
        doc.insertString(doc.getLength(), DIG + RTL, null);
        // Check the new document structure
        assertEquals(9, root.getElementCount());
        checkLevelsAndBounds(root, new int[] { 0, 1, 2, 0, 1, 2, 1, 2, 1 }, new int[] { 0, 3,
                6, 9, 10, 13, 19, 20, 23, 27 });
        // Elements added "\n" of level 1 [19,20]
        //                DIG  of level 2 [20,23]
        //                RTL  of level 1 [23,27]
        assertEquals(3, insertEdit.getChildrenAdded().length);
        // Elements removed "\n" of level 1 [19,20]
        //                  "\n" of level 0 [20,21]
        assertEquals(2, insertEdit.getChildrenRemoved().length);
        assertEquals(6, insertEdit.getIndex());
        // Removed children thorough analysis (taking into account
        // marks were moved when text was inserted)
        Element[] removed = insertEdit.getChildrenRemoved();
        assertEquals(1, getBidiLevel(removed[0]));
        assertEquals(19, removed[0].getStartOffset());
        assertEquals(26, removed[0].getEndOffset());
        assertEquals(0, getBidiLevel(removed[1]));
        assertEquals(26, removed[1].getStartOffset());
        assertEquals(27, removed[1].getEndOffset());
    }

    /**
     * LTR+RTL+"\n"+LTR text is inserted while default direction of doc is LTR
     */
    public void testInsertUpdate09() throws BadLocationException {
        doc.insertString(0, LTR + RTL + "\n" + LTR, null);
        assertEquals(3, root.getElementCount());
        checkLevelsAndBounds(root, new int[] { 0, 1, 0 }, new int[] { 0, 3, 6, 11 });
    }

    /**
     * Tests if RUN_DIRECTION property has any influence on
     * bidirectional algorithm in AbstractDocument.
     */
    public void testInsertUpdate10() throws BadLocationException {
        doc.putProperty(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL);
        doc.insertString(0, LTR, null);
        assertEquals(2, root.getElementCount());
        assertEquals(2, getBidiLevel(root.getElement(0)));
        assertEquals(1, getBidiLevel(root.getElement(1)));
        assertEquals(2, insertEdit.getChildrenAdded().length);
        assertEquals(1, insertEdit.getChildrenRemoved().length);
    }

    /**
     * Tests if RUN_DIRECTION attribute set on text inserted has any
     * influence on bidirectional algorithm in AbstractDocument.
     */
    public void testInsertUpdate11() throws BadLocationException {
        StyleContext context = (StyleContext) doc.getAttributeContext();
        doc.insertString(0, LTR, context.addAttribute(context.getEmptySet(),
                TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL));
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        assertNull(insertEdit);
        /*
         // The assert section would be like this if document supported
         // properties for paragraphs
         assertEquals(2, root.getElementCount());
         assertEquals(2, getBidiLevel(root.getElement(0)));
         assertEquals(1, getBidiLevel(root.getElement(1)));

         assertEquals(2, insertEdit.getChildrenAdded().length);
         assertEquals(1, insertEdit.getChildrenRemoved().length);
         */
    }

    /**
     * Tests if RUN_DIRECTION property has any influence on
     * bidirectional algorithm in AbstractDocument.
     */
    public void testInsertUpdate12() throws BadLocationException {
        doc.insertString(0, "kkk", null);
        doc.putProperty(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL);
        doc.insertString(1, "rrr", null);
        doc.replace(0, 6, "kkk", null);
        assertEquals(2, root.getElementCount());
        assertEquals(2, getBidiLevel(root.getElement(0)));
        assertEquals(1, getBidiLevel(root.getElement(1)));
        assertEquals(2, insertEdit.getChildrenAdded().length);
        assertEquals(1, insertEdit.getChildrenRemoved().length);
    }

    /**
     * Tests that bidi information is updated whatever position text is
     * inserted to.
     */
    public void testInsertUpdate13() throws BadLocationException {
        assertEquals(0, doc.getLength());
        doc.insertString(0, LTR, null);
        assertEquals(1, root.getElementCount());
        doc.insertString(doc.getLength(), RTL, null);
        assertEquals(3, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        assertEquals(1, getBidiLevel(root.getElement(1)));
        assertEquals(0, getBidiLevel(root.getElement(2)));
    }

    public void testRemoveUpdate01() throws BadLocationException {
        doc.insertString(0, LTR, null);
        doc.remove(0, 3);
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        assertNull(removeEdit);
    }

    public void testRemoveUpdate02() throws BadLocationException {
        doc.insertString(0, RTL, null);
        doc.remove(0, 3);
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        assertEquals(1, removeEdit.getChildrenAdded().length);
        assertEquals(1, removeEdit.getChildrenRemoved().length);
        assertEquals(0, removeEdit.getIndex());
        assertEquals(0, getBidiLevel(removeEdit.getChildrenAdded()[0]));
        assertEquals(1, getBidiLevel(removeEdit.getChildrenRemoved()[0]));
    }

    public void testRemoveUpdate03() throws BadLocationException {
        doc.insertString(0, DIG, null);
        doc.remove(0, 3);
        assertEquals(1, root.getElementCount());
        assertEquals(0, getBidiLevel(root.getElement(0)));
        assertNull(removeEdit);
    }

    /**
     * Test-method number isn't changed to correpond with
     * testInsertUpdate08
     */
    public void testRemoveUpdate08() throws BadLocationException {
        // Init document with three paragraphs of text
        doc.insertString(0, LTR + RTL + DIG + "\n" + RTL + DIG + LTR + "\n" + DIG + RTL, null);
        assertEquals(9, root.getElementCount());
        checkLevelsAndBounds(root, new int[] { 0, 1, 2, 0, 1, 2, 1, 2, 1 }, new int[] { 0, 3,
                6, 9, 10, 13, 19, 20, 23, 27 });
        doc.remove(10, 3);
        // Check the document has the expected structure
        assertEquals(6, root.getElementCount());
        checkLevelsAndBounds(root, new int[] { 0, 1, 2, 0, 2, 1 }, new int[] { 0, 3, 6, 9, 17,
                20, 24 });
        // Elements added: "\n" + DIG + LTR + "\n" of level 0 [9,17]
        assertEquals(1, removeEdit.getChildrenAdded().length);
        // Elements removed: "\n"      of level 0 [ 9,10]
        //                   RTL       of level 1 [10,13]
        //                   DIG + LTR of level 2 [13,19]
        //                   "\n"      of level 1 [19,20]
        assertEquals(4, removeEdit.getChildrenRemoved().length);
        assertEquals(3, removeEdit.getIndex());
        // Removed children thorough analysis (taking into account
        // marks were moved when text was inserted)
        Element[] removed = removeEdit.getChildrenRemoved();
        assertEquals(0, getBidiLevel(removed[0]));
        assertEquals(1, getBidiLevel(removed[1]));
        assertEquals(2, getBidiLevel(removed[2]));
        assertEquals(1, getBidiLevel(removed[3]));
    }

    public void testRemoveUpdate09() throws BadLocationException {
        doc.insertString(0, LTR + RTL + DIG + RTL + DIG + LTR + DIG + RTL, null);
        assertEquals(8, root.getElementCount());
        checkLevelsAndBounds(root, new int[] { 0, 1, 2, 1, 2, 0, 1, 0 }, new int[] { 0, 3, 6,
                9, 12, 15, 21, 24, 25 });
        doc.remove(0, doc.getLength());
        assertEquals(1, root.getElementCount());
        checkLevelsAndBounds(root, new int[] { 0 }, new int[] { 0, 1 });
    }

    /**
     * Tests that when text is removed from document, the paragraph where the
     * change occurred is completely reanalized despite the fact that the text
     * removed has only one direction and this operation actually doesn't
     * cause structure change.
     */
    public void testRemoveUpdate10() throws BadLocationException {
        doc.insertString(0, LTR + LTR + RTL + RTL + LTR + LTR, null);
        assertEquals(3, root.getElementCount());
        doc.remove(LTR.length() * 2, RTL.length());
        assertEquals(3, root.getElementCount());
        assertNotNull(removeEdit);
        assertEquals(3, removeEdit.getChildrenAdded().length);
        assertEquals(3, removeEdit.getChildrenRemoved().length);
    }
}
