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

import java.util.List;
import java.util.Vector;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import junit.framework.TestCase;

/**
 * Tests DefaultStyledDocument class, in particular which ElementSpecs are
 * created during insertString with different parameters.
 * <p>
 * These tests insert some text into document containing one paragraph of
 * attributed text:
 * <code>"plainbolditalic"</code> plus implied newline character.
 * Thus document has one <em>paragraph</em> (Branch)
 * with three <em>contents</em> (Leaf) under the paragraph.
 * <p>
 * The text is inserted into leaf of <code>leafIndex</code> at
 * offset <code>insertOffset</code>. By default <code>leafIndex = 1</code>
 * (the second style run with bold attributes), and offset is two chars to
 * the right from the start, i.e. the text is inserted after <code>"bo"</code>.
 *
 * <p>
 * Tests are classified as follows:
 * <dl>
 * <dt>0x</dt>
 * <dd>No attributes are set or passed as parameters.</dd>
 *
 * <dt>1x</dt>
 * <dd>The paragraph contains <code>bold</code> attributes;
 *     the text is inserted with <code>null</code>
 *         as <code>attrs</code> parameter.</dd>
 *
 * <dt>2x</dt>
 * <dd>The character attributes set to <code>bold</code>;
 *     the text is inserted with <code>null</code>
 *         as <code>attrs</code> parameter.</dd>
 *
 * <dt>3x</dt>
 * <dd>No attributes are set;
 *     the text is inserted with <code>italic</code> attributes.</dd>
 *
 * <dt>4x</dt>
 * <dd>The paragraph contains <code>bold</code> attributes;
 *     the text is inserted with <code>italic</code> attributes.</dd>
 *
 * <dt>5x</dt>
 * <dd>The character attributes set to <code>bold</code>;
 *     the text is inserted with <code>italic</code> attributes.</dd>
 * </dl>
 * <p>Each test-case region currently contains four tests.
 *
 */
public class DefaultStyledDocument_ElementBuffer_Specs3Test extends TestCase implements
        DocumentListener {
    private DefaultStyledDocument doc;

    private Element root;

    private ElementBuffer buf;

    private ElementSpec[] specs;

    private Element paragraph;

    private Element leaf;

    private static final int leafIndex = 1;

    private int insertOffset;

    private DefaultDocumentEvent insertEvent;

    private static final AttributeSet bold = DefStyledDoc_Helpers.bold;

    private static final AttributeSet italic = DefStyledDoc_Helpers.italic;

    private static final class ElementAssert {
        private final AttributeSet attrs;

        private final int start;

        private final int end;

        public ElementAssert(AttributeSet attrs, int start, int end) {
            this.attrs = attrs;
            this.start = start;
            this.end = end;
        }

        public void check(final Element element) {
            if (attrs == null) {
                assertEquals("Attribute count", 0, element.getAttributes().getAttributeCount());
            } else {
                assertTrue("Attributes", attrs.isEqual(element.getAttributes()));
            }
            assertEquals("Start offset", start, element.getStartOffset());
            assertEquals("End offset", end, element.getEndOffset());
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefStyledDoc_Helpers.DefStyledDocWithLogging();
        root = doc.getDefaultRootElement();
        buf = new DefStyledDoc_Helpers.ElementBufferWithLogging(doc, root) {
            private static final long serialVersionUID = 1L;

            @Override
            public void insert(int offset, int length, ElementSpec[] spec,
                    DefaultDocumentEvent event) {
                super.insert(offset, length, specs = spec, event);
            }
        };
        doc.buffer = buf;
        doc.insertString(doc.getLength(), "plain", null); // 5 chars
        doc.insertString(doc.getLength(), "bold", bold); // 4 chars
        doc.insertString(doc.getLength(), "italic", italic); // 6 chars
        paragraph = root.getElement(0);
        leaf = paragraph.getElement(leafIndex);
        insertOffset = leaf.getStartOffset() + 2;
        doc.addDocumentListener(this);
    }

    /**
     * Adds text with no attributes.
     */
    public void testInsertString01() throws Exception {
        doc.insertString(insertOffset, "^^^", null);
        assertEquals(2, getEdits(insertEvent).size());
        List<?> edits = getEdits(insertEvent);
        assertChange(edits.get(1), paragraph, 1, 3);
        ElementChange change = (ElementChange) edits.get(1);
        assertSame(leaf, change.getChildrenRemoved()[0]);
        final Element[] added = change.getChildrenAdded();
        for (int i = 0; i < added.length; i++) {
            assertSame("@" + i, paragraph.getElement(i + leafIndex), added[i]);
        }
        ElementAssert[] expected = { new ElementAssert(null, 0, 5),
                new ElementAssert(bold, 5, 7), new ElementAssert(null, 7, 10),
                new ElementAssert(bold, 10, 12), new ElementAssert(italic, 12, 18),
                new ElementAssert(null, 18, 19) };
        assertEquals(expected.length, paragraph.getElementCount());
        for (int i = 0; i < expected.length; i++) {
            expected[i].check(paragraph.getElement(i));
        }
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
    }

    /**
     * Adds text with the same attributes.
     */
    public void testInsertString02() throws Exception {
        doc.insertString(insertOffset, "^^^", bold);
        List<?> edits = getEdits(insertEvent);
        assertEquals(1, edits.size());
        assertEquals(5, leaf.getStartOffset());
        assertEquals(5 + 4 + 3, leaf.getEndOffset());
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 3);
        // These actions are performed:
        //      createLeaf(paragraph[0, 19], bold=true , 5, 7)
        //      createLeaf(paragraph[0, 19], , 7, 10)
        //      createLeaf(paragraph[0, 19], bold=true , 10, 12)
    }

    /**
     * Puts non-attributed new line character.
     */
    public void testInsertString03() throws Exception {
        doc.insertString(insertOffset, "\n", null);
        final List<?> edits = getEdits(insertEvent);
        assertEquals(3, edits.size());
        assertSame(paragraph, root.getElement(0));
        assertChange(edits.get(1), root.getElement(0), 3, 2);
        //        assertChange(edits.get(2), root.getElement(1), 1, 1);
        assertChange(edits.get(2), root, 0, 1);
        final ElementAssert[] par1Expected = { new ElementAssert(null, 0, 5),
                new ElementAssert(bold, 5, 7), new ElementAssert(null, 7, 8) };
        assertEquals(par1Expected.length, paragraph.getElementCount());
        for (int i = 0; i < par1Expected.length; i++) {
            par1Expected[i].check(paragraph.getElement(i));
        }
        final ElementAssert[] par2Expected = { new ElementAssert(bold, 8, 10),
                new ElementAssert(italic, 10, 16), new ElementAssert(null, 16, 17) };
        final Element par2 = root.getElement(1);
        assertEquals(par2Expected.length, par2.getElementCount());
        for (int i = 0; i < par2Expected.length; i++) {
            par2Expected[i].check(par2.getElement(i));
        }
        assertEquals(3, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 1);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        // These actions are performed:
        //      createLeaf(paragraph[0, 17], bold=true , 5, 7)
        //      createLeaf(paragraph[0, 17], , 7, 8)
        //      createBranch(section[0, 17], resolver=**AttributeSet** )
        //      createLeaf(paragraph[N/A], bold=true , 8, 10)
        //      createLeaf(paragraph[N/A], italic=true , 10, 16)
        //      createLeaf(paragraph[N/A], , 16, 17)
    }

    /**
     * Puts non-attributed 'one\ntwo'.
     */
    public void testInsertString04() throws Exception {
        doc.insertString(insertOffset, "one\ntwo", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 3, 2);
        assertChange(edits.get(2), root.getElement(1), 0, 1);
        assertChange(edits.get(3), root, 0, 1);
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
        // These actions are performed:
        //      createLeaf(paragraph[0, 23], bold=true , 5, 7)
        //      createLeaf(paragraph[0, 23], , 7, 11)
        //      createBranch(section[0, 23], resolver=**AttributeSet** )
        //      createLeaf(paragraph[N/A], bold=true , 14, 16)
        //      createLeaf(paragraph[N/A], italic=true , 16, 22)
        //      createLeaf(paragraph[N/A], , 22, 23)
        //      createLeaf(paragraph[14, 23], , 11, 14)
    }

    /**
     * Puts 'one\ntwo' with bold attributes (the same as in the portion
     * where text is inserted).
     */
    public void testInsertString05() throws Exception {
        doc.insertString(insertOffset, "one\ntwo", bold);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 3, 1);
        assertChange(edits.get(2), root.getElement(1), 1, 1);
        assertChange(edits.get(3), root, 0, 1);
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.JoinNextDirection, 0, 3);
        // These actions are performed:
        //      createLeaf(paragraph[0, 23], bold=true , 5, 11)
        //      createBranch(section[0, 23], resolver=**AttributeSet** )
        //      createLeaf(paragraph[N/A], bold=true , 14, 16)
        //      createLeaf(paragraph[N/A], italic=true , 16, 22)
        //      createLeaf(paragraph[N/A], , 22, 23)
        //      createLeaf(paragraph[14, 23], bold=true , 11, 16)
    }

    private static void assertChange(final Object object, final Element element,
            final int removed, final int added) {
        DefStyledDoc_Helpers.assertChange(object, element, removed, added);
    }

    private static void assertSpec(final ElementSpec spec, final short type,
            final short direction, final int offset, final int length) {
        DefStyledDoc_Helpers.assertSpec(spec, type, direction, offset, length);
    }

    private static Vector<?> getEdits(final DefaultDocumentEvent event) {
        return DefStyledDoc_Helpers.getEdits(event);
    }

    public void changedUpdate(DocumentEvent e) {
    }

    public void insertUpdate(DocumentEvent e) {
        insertEvent = (DefaultDocumentEvent) e;
    }

    public void removeUpdate(DocumentEvent e) {
    }
}
