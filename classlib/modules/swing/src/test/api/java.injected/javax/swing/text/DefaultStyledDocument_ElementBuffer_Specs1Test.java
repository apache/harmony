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
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import junit.framework.TestCase;

/**
 * Tests DefaultStyledDocument class, in particular which ElementSpecs are
 * created during insertString with different parameters.
 * <p>
 * These tests insert some text into empty document,
 * i.e. <code>doc.getLength() == 0</code>.
 * But it is important to note that actually document contains the implied
 * newline character. Thus document will have one <em>paragraph</em> (Branch)
 * and one <em>content</em> (Leaf) under the paragraph.
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
public class DefaultStyledDocument_ElementBuffer_Specs1Test extends TestCase implements
        DocumentListener {
    private DefaultStyledDocument doc;

    private Element root;

    private ElementBuffer buf;

    private ElementSpec[] specs;

    private DefaultDocumentEvent insertEvent;

    private static final AttributeSet bold = DefStyledDoc_Helpers.bold;

    private static final AttributeSet italic = DefStyledDoc_Helpers.italic;

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
        doc.addDocumentListener(this);
    }

    /**
     * No attributes, text 'one', doc is empty.
     */
    public void testInsertString01() throws Exception {
        doc.insertString(0, "one", null);
        assertEquals(1, getEdits(insertEvent).size());
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 3);
    }

    /**
     * No attributes, text 'one\n', doc is empty.
     */
    public void testInsertString02() throws Exception {
        doc.insertString(0, "one\n", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(3, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root, 0, 1);
        assertEquals(3, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
    }

    /**
     * No attributes, text '\none', doc is empty.
     */
    public void testInsertString03() throws Exception {
        doc.insertString(0, "\none", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 1, 1);
        assertChange(edits.get(3), root, 0, 1);
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 1);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.JoinNextDirection, 0, 3);
        /*
         * createLeaf(paragraph[0, 5], , 0, 1)
         *      New leaf containing \n in the first paragraph
         *
         * createBranch(section[0, 5], resolver=**AttributeSet** )
         *      New branch as result of Fracture
         * createLeaf(paragraph[N/A], , 4, 5)
         *      As fracture copies elements, this is "the copy" added to
         *      the newly created branch
         *
         * createLeaf(paragraph[4, 5], , 1, 5)
         *      This leaf goes the newly created branch and contains
         *      the right characters. The leaf created as the result
         *      of fracture is removed.
         */
    }

    /**
     * No attributes, text 'one\ntwo', doc is empty.
     */
    public void testInsertString04() throws Exception {
        doc.insertString(0, "one\ntwo", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 1, 1);
        assertChange(edits.get(3), root, 0, 1);
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.JoinNextDirection, 0, 3);
    }

    //---------------------------------------------------------------------------
    /**
     * Bold attribute on paragraph, text 'one' with no attributes, doc is empty.
     */
    public void testInsertString11() throws Exception {
        doc.setParagraphAttributes(0, 1, bold, false);
        doc.insertString(0, "one", null);
        assertEquals(1, getEdits(insertEvent).size());
        assertTrue(root.getElement(0).getAttributes().containsAttributes(bold));
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 3);
    }

    /**
     * Bold attribute on paragraph, text 'one\n' with no attributes, doc is
     * empty.
     */
    public void testInsertString12() throws Exception {
        doc.setParagraphAttributes(0, 1, bold, false);
        doc.insertString(0, "one\n", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(3, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root, 0, 1);
        assertTrue(root.getElement(0).getAttributes().containsAttributes(bold));
        assertTrue(root.getElement(1).getAttributes().containsAttributes(bold));
        assertEquals(3, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        /*
         * Fracture copies attributes of the paragraph:
         *
         * createBranch(section[0, 5], resolver=**AttributeSet** bold=true )
         */
    }

    /**
     * Bold attribute on paragraph, text '\none' with no attributes, doc is
     * empty.
     */
    public void testInsertString13() throws Exception {
        doc.setParagraphAttributes(0, 1, bold, false);
        doc.insertString(0, "\none", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 1, 1);
        assertChange(edits.get(3), root, 0, 1);
        assertTrue(root.getElement(0).getAttributes().containsAttributes(bold));
        assertTrue(root.getElement(1).getAttributes().containsAttributes(bold));
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 1);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.JoinNextDirection, 0, 3);
    }

    /**
     * Bold attribute on paragraph, text 'one\ntwo' with no attributes, doc is
     * empty.
     */
    public void testInsertString14() throws Exception {
        doc.setParagraphAttributes(0, 1, bold, false);
        doc.insertString(0, "one\ntwo", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 1, 1);
        assertChange(edits.get(3), root, 0, 1);
        assertTrue(root.getElement(0).getAttributes().containsAttributes(bold));
        assertTrue(root.getElement(1).getAttributes().containsAttributes(bold));
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.JoinPreviousDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.JoinNextDirection, 0, 3);
    }

    //---------------------------------------------------------------------------
    /**
     * Bold attribute on character, text 'one' with no attributes, doc is empty.
     */
    public void testInsertString21() throws Exception {
        doc.setCharacterAttributes(0, 1, bold, false);
        doc.insertString(0, "one", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(2, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 2);
        Element charElem = doc.getCharacterElement(0);
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        charElem = doc.getCharacterElement(doc.getLength());
        assertTrue(charElem.getAttributes().isEqual(bold));
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
        /*
         * createLeaf(paragraph[0, 4], , 0, 3)
         *      The word 'one'
         * createLeaf(paragraph[0, 4], bold=true , 3, 4)
         *      The attributed newline char
         */
    }

    /**
     * Bold attribute on character, text 'one\n' with no attributes, doc is
     * empty.
     */
    public void testInsertString22() throws Exception {
        doc.setCharacterAttributes(0, 1, bold, false);
        doc.insertString(0, "one\n", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(3, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root, 0, 1);
        Element charElem = doc.getCharacterElement(0);
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(0, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertTrue(charElem.getAttributes().isEqual(bold));
        assertEquals(4, charElem.getStartOffset());
        assertEquals(5, charElem.getEndOffset());
        assertEquals(3, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        /*
         * While copying elements with fracture, it will copy leaf attributes:
         *
         * createBranch(section[0, 5], resolver=**AttributeSet** )
         * createLeaf(paragraph[N/A], bold=true , 4, 5)
         */
    }

    /**
     * Bold attribute on character, text '\none' with no attributes, doc is
     * empty.
     */
    public void testInsertString23() throws Exception {
        doc.setCharacterAttributes(0, 1, bold, false);
        doc.insertString(0, "\none", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 0, 1);
        assertChange(edits.get(3), root, 0, 1);
        Element charElem = doc.getCharacterElement(0);
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(0, charElem.getStartOffset());
        assertEquals(1, charElem.getEndOffset());
        charElem = doc.getCharacterElement(1);
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(1, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertTrue(charElem.getAttributes().isEqual(bold));
        assertEquals(4, charElem.getStartOffset());
        assertEquals(5, charElem.getEndOffset());
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 1);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
        /*
         * createLeaf(paragraph[0, 5], , 0, 1)
         * createBranch(section[0, 5], * resolver=**AttributeSet** )
         * createLeaf(paragraph[N/A], bold=true , 4, 5)
         * createLeaf(paragraph[4, 5], , 1, 4)
         */
    }

    /**
     * Bold attribute on character, text 'one\ntwo' with no attributes, doc is
     * empty.
     */
    public void testInsertString24() throws Exception {
        doc.setCharacterAttributes(0, 1, bold, false);
        doc.insertString(0, "one\ntwo", null);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 0, 1);
        assertChange(edits.get(3), root, 0, 1);
        Element charElem = doc.getCharacterElement(0);
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(0, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(4);
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(4, charElem.getStartOffset());
        assertEquals(7, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertTrue(charElem.getAttributes().isEqual(bold));
        assertEquals(7, charElem.getStartOffset());
        assertEquals(8, charElem.getEndOffset());
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
    }

    //===========================================================================
    /**
     * No attributes, text 'one' with italic, doc is empty.
     */
    public void testInsertString31() throws Exception {
        doc.insertString(0, "one", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(2, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 2);
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        charElem = doc.getCharacterElement(doc.getLength());
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
    }

    /**
     * No attributes, text 'one\n' with italic, doc is empty.
     */
    public void testInsertString32() throws Exception {
        doc.insertString(0, "one\n", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(3, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root, 0, 1);
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(0, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(4, charElem.getStartOffset());
        assertEquals(5, charElem.getEndOffset());
        assertEquals(3, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
    }

    /**
     * No attributes, text '\none' with italic, doc is empty.
     */
    public void testInsertString33() throws Exception {
        doc.insertString(0, "\none", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 0, 1);
        assertChange(edits.get(3), root, 0, 1);
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(0, charElem.getStartOffset());
        assertEquals(1, charElem.getEndOffset());
        charElem = doc.getCharacterElement(1);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(1, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(4, charElem.getStartOffset());
        assertEquals(5, charElem.getEndOffset());
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 1);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
    }

    /**
     * No attributes, text 'one\ntwo' with italic, doc is empty.
     */
    public void testInsertString34() throws Exception {
        doc.insertString(0, "one\ntwo", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 0, 1);
        assertChange(edits.get(3), root, 0, 1);
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(0, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(4);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(4, charElem.getStartOffset());
        assertEquals(7, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(7, charElem.getStartOffset());
        assertEquals(8, charElem.getEndOffset());
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
    }

    //---------------------------------------------------------------------------
    /**
     * Bold attribute on paragraph, text 'one' with italic, doc is empty.
     */
    public void testInsertString41() throws Exception {
        doc.setParagraphAttributes(0, 1, bold, false);
        doc.insertString(0, "one", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(2, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 2);
        assertTrue(root.getElement(0).getAttributes().containsAttributes(bold));
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        charElem = doc.getCharacterElement(doc.getLength());
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
    }

    /**
     * Bold attribute on paragraph, text 'one\n' with italic, doc is empty.
     */
    public void testInsertString42() throws Exception {
        doc.setParagraphAttributes(0, 1, bold, false);
        doc.insertString(0, "one\n", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(3, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root, 0, 1);
        assertTrue(root.getElement(0).getAttributes().containsAttributes(bold));
        assertTrue(root.getElement(1).getAttributes().containsAttributes(bold));
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(0, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(4, charElem.getStartOffset());
        assertEquals(5, charElem.getEndOffset());
        assertEquals(3, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        /*
         * Fracture copies attributes of the paragraph:
         *
         * createBranch(section[0, 5], resolver=**AttributeSet** bold=true )
         */
    }

    /**
     * Bold attribute on paragraph, text '\none' with italic, doc is empty.
     */
    public void testInsertString43() throws Exception {
        doc.setParagraphAttributes(0, 1, bold, false);
        doc.insertString(0, "\none", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 0, 1);
        assertChange(edits.get(3), root, 0, 1);
        assertTrue(root.getElement(0).getAttributes().containsAttributes(bold));
        assertTrue(root.getElement(1).getAttributes().containsAttributes(bold));
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(0, charElem.getStartOffset());
        assertEquals(1, charElem.getEndOffset());
        charElem = doc.getCharacterElement(1);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(1, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(4, charElem.getStartOffset());
        assertEquals(5, charElem.getEndOffset());
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 1);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
    }

    /**
     * Bold attribute on paragraph, text 'one\ntwo' with italic, doc is empty.
     */
    public void testInsertString44() throws Exception {
        doc.setParagraphAttributes(0, 1, bold, false);
        doc.insertString(0, "one\ntwo", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 0, 1);
        assertChange(edits.get(3), root, 0, 1);
        assertTrue(root.getElement(0).getAttributes().containsAttributes(bold));
        assertTrue(root.getElement(1).getAttributes().containsAttributes(bold));
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(0, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(4);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(4, charElem.getStartOffset());
        assertEquals(7, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertEquals(0, charElem.getAttributes().getAttributeCount());
        assertEquals(7, charElem.getStartOffset());
        assertEquals(8, charElem.getEndOffset());
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
    }

    //---------------------------------------------------------------------------
    /**
     * Bold attribute on character, text 'one' with italic, doc is empty.
     */
    public void testInsertString51() throws Exception {
        doc.setCharacterAttributes(0, 1, bold, false);
        doc.insertString(0, "one", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(2, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 2);
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        charElem = doc.getCharacterElement(doc.getLength());
        assertTrue(charElem.getAttributes().isEqual(bold));
        assertEquals(1, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
        /*
         * createLeaf(paragraph[0, 4], , 0, 3)
         *      The word 'one'
         * createLeaf(paragraph[0, 4], bold=true , 3, 4)
         *      The attributed newline char
         */
    }

    /**
     * Bold attribute on character, text 'one\n' with italic, doc is empty.
     */
    public void testInsertString52() throws Exception {
        doc.setCharacterAttributes(0, 1, bold, false);
        doc.insertString(0, "one\n", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(3, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root, 0, 1);
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(0, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertTrue(charElem.getAttributes().isEqual(bold));
        assertEquals(4, charElem.getStartOffset());
        assertEquals(5, charElem.getEndOffset());
        assertEquals(3, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        /*
         * While copying elements with fracture, it will copy leaf attributes:
         *
         * createBranch(section[0, 5], resolver=**AttributeSet** )
         * createLeaf(paragraph[N/A], bold=true , 4, 5)
         */
    }

    /**
     * Bold attribute on character, text '\none' with italic, doc is empty.
     */
    public void testInsertString53() throws Exception {
        doc.setCharacterAttributes(0, 1, bold, false);
        doc.insertString(0, "\none", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 0, 1);
        assertChange(edits.get(3), root, 0, 1);
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(0, charElem.getStartOffset());
        assertEquals(1, charElem.getEndOffset());
        charElem = doc.getCharacterElement(1);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(1, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertTrue(charElem.getAttributes().isEqual(bold));
        assertEquals(4, charElem.getStartOffset());
        assertEquals(5, charElem.getEndOffset());
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 1);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
        /*
         * createLeaf(paragraph[0, 5], , 0, 1)
         * createBranch(section[0, 5], resolver=**AttributeSet** )
         * createLeaf(paragraph[N/A], bold=true , 4, 5)
         * createLeaf(paragraph[4, 5], , 1, 4)
         */
    }

    /**
     * Bold attribute on character, text 'one\ntwo' with italic, doc is empty.
     */
    public void testInsertString54() throws Exception {
        doc.setCharacterAttributes(0, 1, bold, false);
        doc.insertString(0, "one\ntwo", italic);
        List<?> edits = getEdits(insertEvent);
        assertEquals(4, edits.size());
        assertChange(edits.get(1), root.getElement(0), 1, 1);
        assertChange(edits.get(2), root.getElement(1), 0, 1);
        assertChange(edits.get(3), root, 0, 1);
        Element charElem = doc.getCharacterElement(0);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(0, charElem.getStartOffset());
        assertEquals(4, charElem.getEndOffset());
        charElem = doc.getCharacterElement(4);
        assertTrue(charElem.getAttributes().isEqual(italic));
        assertEquals(4, charElem.getStartOffset());
        assertEquals(7, charElem.getEndOffset());
        charElem = doc.getCharacterElement(doc.getLength());
        assertTrue(charElem.getAttributes().isEqual(bold));
        assertEquals(7, charElem.getStartOffset());
        assertEquals(8, charElem.getEndOffset());
        assertEquals(4, specs.length);
        assertSpec(specs[0], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 4);
        assertSpec(specs[1], ElementSpec.EndTagType, ElementSpec.OriginateDirection, 0, 0);
        assertSpec(specs[2], ElementSpec.StartTagType, ElementSpec.JoinFractureDirection, 0, 0);
        assertSpec(specs[3], ElementSpec.ContentType, ElementSpec.OriginateDirection, 0, 3);
    }

    private static void assertChange(final Object object, final Element element,
            final int removed, int added) {
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
