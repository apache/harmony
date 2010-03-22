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

import javax.swing.text.DefaultStyledDocument.ElementBuffer;
import junit.framework.TestCase;

/**
 * Tests DefaultStyledDocument.ElementBuffer class. This test-case covers
 * only simple methods of ElementBuffer class: ElementBuffer,
 * clone(Element, Element), getRootElement.
 *
 */
public class DefaultStyledDocument_ElementBufferTest extends TestCase {
    private DefaultStyledDocument doc;

    private ElementBuffer buf;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefStyledDoc_Helpers.DefStyledDocWithLogging();
        final Element root = doc.getDefaultRootElement();
        buf = new DefStyledDoc_Helpers.ElementBufferWithLogging(doc, root);
        doc.buffer = buf;
    }

    public void testElementBuffer() throws BadLocationException {
        Element leaf = doc.new LeafElement(null, null, 0, 1);
        buf = doc.new ElementBuffer(leaf);
        assertSame(leaf, buf.getRootElement());
    }

    /*
     * Element clone(Element, Element)
     *
     * Clones a LeafElement.
     */
    public void testCloneLeaf() throws BadLocationException {
        doc.writeLock();
        try {
            MutableAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setBold(attrs, true);
            attrs.addAttribute(StyleConstants.Bold, Boolean.TRUE);
            final AttributeSet bold = attrs.copyAttributes();
            doc.insertString(0, "one\n", bold);
            attrs = new SimpleAttributeSet();
            StyleConstants.setAlignment(attrs, 0);
            doc.insertString(doc.getLength(), "two\nthree", attrs);
            final Element root = doc.getDefaultRootElement();
            Element par1 = root.getElement(0);
            Element par2 = root.getElement(1);
            Element line = par1.getElement(0);
            Element cloned = buf.clone(par2, line);
            assertNotSame(line, cloned);
            assertEquals(line.getStartOffset(), cloned.getStartOffset());
            assertEquals(line.getEndOffset(), cloned.getEndOffset());
            assertEquals(bold, cloned.getAttributes());
            assertSame(par1, line.getParentElement());
            assertSame(par2, cloned.getParentElement());
        } finally {
            doc.writeUnlock();
        }
    }

    /*
     * Element clone(Element, Element)
     *
     * Clones a BranchElement.
     */
    public void testCloneBranch() throws BadLocationException {
        doc.writeLock();
        try {
            MutableAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setBold(attrs, true);
            final AttributeSet bold = attrs.copyAttributes();
            doc.insertString(0, "one\n", bold);
            attrs = new SimpleAttributeSet();
            StyleConstants.setAlignment(attrs, 0);
            doc.insertString(doc.getLength(), "two\nthree", attrs);
            attrs = new SimpleAttributeSet();
            StyleConstants.setFontSize(attrs, 25);
            doc.setParagraphAttributes(0, 1, attrs, false);
            final AttributeSet fontSize = attrs.copyAttributes();
            final Element root = doc.getDefaultRootElement();
            final Element par1 = root.getElement(0);
            final Element par2 = root.getElement(1);
            final Element cloned = buf.clone(par2, par1);
            assertNotSame(par1, cloned);
            assertEquals(par1.getStartOffset(), cloned.getStartOffset());
            assertEquals(par1.getEndOffset(), cloned.getEndOffset());
            final AttributeSet clonedAttrs = cloned.getAttributes();
            assertTrue(clonedAttrs.containsAttributes(fontSize));
            assertTrue(clonedAttrs.isDefined(AttributeSet.ResolveAttribute));
            assertSame(root, par1.getParentElement());
            assertSame(par2, cloned.getParentElement());
            assertSame(root, par2.getParentElement());
        } finally {
            doc.writeUnlock();
        }
    }

    public void testGetRootElement() {
        Element docRoot = doc.getDefaultRootElement();
        Element bufRoot = buf.getRootElement();
        assertSame(docRoot, bufRoot);
        bufRoot = doc.new BranchElement(null, null);
        buf = doc.new ElementBuffer(bufRoot);
        doc.buffer = buf;
        // The previously fetched root element of the document is distinct
        // from the newly created (no doubt).
        assertNotSame(docRoot, bufRoot);
        // But document returns the newly created element about which it didn't
        // known when it was constructed itself. Thus the document doesn't
        // store default root element but gets it from the associated
        // ElementBuffer.
        assertSame(bufRoot, doc.getDefaultRootElement());
    }
}
