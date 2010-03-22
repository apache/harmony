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
import javax.swing.text.AbstractDocument.BranchElement;
import javax.swing.text.AbstractDocument.LeafElement;
import javax.swing.text.AbstractDocumentTest.DisAbstractedDocument;

/**
 * Tests AbstractDocument.LeafElement class.
 *
 */
public class AbstractDocument_LeafElementTest extends BasicSwingTestCase {
    protected AbstractDocument doc;

    protected LeafElement leaf1;

    protected LeafElement leaf2;

    protected AttributeSet[] as;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        StyleContextTest.sc = StyleContext.getDefaultStyleContext();
        as = new AttributeSet[] { StyleContextTest.addAttribute(1),
                StyleContextTest.addAttribute(null, 2, 2),
                StyleContextTest.addAttribute(null, 5, 2) };
        doc = new DisAbstractedDocument(new GapContent());
        doc.insertString(0, "0123456789", as[0]);
        doc.writeLock();
        BranchElement branch = doc.new BranchElement(null, as[1]);
        leaf1 = doc.new LeafElement(null, as[2], 0, 3);
        leaf2 = doc.new LeafElement(branch, as[2], 5, 8);
        doc.writeUnlock();
    }

    public void testGetElement() {
        assertNull(leaf1.getElement(-1));
        assertNull(leaf1.getElement(0));
        assertNull(leaf1.getElement(1));
        assertNull(leaf1.getElement(2));
        assertNull(leaf1.getElement(20));
    }

    public void testChildren() {
        assertNull(leaf1.children());
        assertNull(leaf2.children());
    }

    public void testGetName() {
        assertEquals("content", leaf1.getName());
        assertEquals("content", leaf2.getName());
        assertSame(AbstractDocument.ContentElementName, leaf1.getName());
    }

    public void testGetElementIndex() {
        assertEquals(-1, leaf1.getElementIndex(-1));
        assertEquals(-1, leaf1.getElementIndex(0));
        assertEquals(-1, leaf1.getElementIndex(1));
        assertEquals(-1, leaf1.getElementIndex(2));
        assertEquals(-1, leaf1.getElementIndex(20));
    }

    public void testIsLeaf() {
        assertTrue(leaf1.isLeaf());
    }

    public void testGetAllowsChildren() {
        assertFalse(leaf2.getAllowsChildren());
    }

    public void testGetStartOffset() throws BadLocationException {
        assertEquals(0, leaf1.getStartOffset());
        assertEquals(5, leaf2.getStartOffset());
        doc.insertString(2, "insert", as[2]);
        assertEquals(0, leaf1.getStartOffset());
        assertEquals(11, leaf2.getStartOffset());
    }

    public void testGetEndOffset() throws BadLocationException {
        assertEquals(3, leaf1.getEndOffset());
        assertEquals(8, leaf2.getEndOffset());
        doc.insertString(4, "insert", as[2]);
        assertEquals(3, leaf1.getEndOffset());
        assertEquals(14, leaf2.getEndOffset());
    }

    public void testGetElementCount() {
        assertEquals(0, leaf1.getElementCount());
    }

    public void testLeafElement() {
        doc.writeLock();
        AbstractDocument.LeafElement leaf = doc.new LeafElement(leaf1, as[2], 3, 9);
        assertSame(leaf1, leaf.getParent());
        assertSame(leaf1, leaf.getParentElement());
        assertSame(leaf, leaf.getAttributes());
        assertEquals(as[2].getAttributeCount(), leaf.getAttributeCount());
        assertEquals(3, leaf.getStartOffset());
        assertEquals(9, leaf.getEndOffset());
        int[] start = { -1, 3, 3, 3 }; // start offset
        int[] expStart = { 0, 3, 3, 3 }; // expectations for start offset
        int[] end = { 9, -1, 1, 20 }; // end offset
        int[] expEnd = { 9, 0, 1, 20 }; // expectations for end offset
        int[] intEnd = { 9, 3, 3, 20 }; // expectations for our end offset
        for (int i = 0; i < start.length; i++) {
            leaf = doc.new LeafElement(null, null, start[i], end[i]);
            assertEquals("Start (" + i + ")", expStart[i], leaf.getStartOffset());
            assertEquals("End (" + i + ")", BasicSwingTestCase.isHarmony() ? intEnd[i]
                    : expEnd[i], leaf.getEndOffset());
        }
        doc.writeUnlock();
    }

    public void testToString() {
        assertEquals("LeafElement(content) 0,3\n", leaf1.toString());
        assertEquals("LeafElement(content) 5,8\n", leaf2.toString());
    }
}
