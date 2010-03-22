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
 * @author Alexander T. Simbirtsev
 */
package javax.swing.text;

import javax.swing.BasicSwingTestCase;

public class ElementIteratorTest extends BasicSwingTestCase {
    protected ElementIterator iterator;

    protected AbstractDocument doc;

    protected Element root;

    protected Element branch1;

    protected Element branch2;

    protected Element leaf1;

    protected Element leaf2;

    protected Element leaf3;

    protected Element leaf4;

    protected Element leaf5;

    protected Element leaf6;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new DefaultStyledDocument();
        MutableAttributeSet attr1 = new SimpleAttributeSet();
        MutableAttributeSet attr2 = new SimpleAttributeSet();
        StyleConstants.setBold(attr1, true);
        StyleConstants.setItalic(attr2, true);
        doc.insertString(0, "00", attr1);
        doc.insertString(2, "XX", attr2);
        doc.insertString(4, "\n00", attr1);
        doc.insertString(7, "XX", attr2);
        root = doc.getRootElements()[0];
        branch1 = root.getElement(0);
        leaf1 = branch1.getElement(0);
        leaf2 = branch1.getElement(1);
        leaf3 = branch1.getElement(2);
        branch2 = root.getElement(1);
        leaf4 = branch2.getElement(0);
        leaf5 = branch2.getElement(1);
        leaf6 = branch2.getElement(2);
        iterator = new ElementIterator(doc);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.text.ElementIterator.ElementIterator(Document)'
     */
    public void testElementIteratorDocument() {
        assertSame(root, iterator.current());
        assertNull(iterator.previous());
        iterator.next();
        assertSame(root, iterator.first());
        assertNull(iterator.previous());
        assertSame(root, iterator.current());

        try { // Regression test for HARMONY-1811
            new ElementIterator((Document) null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    /*
     * Test method for 'javax.swing.text.ElementIterator.ElementIterator(Element)'
     */
    public void testElementIteratorElement() {
        iterator = new ElementIterator(branch1);
        assertSame(branch1, iterator.current());
        assertSame(leaf1, iterator.next());
        assertSame(leaf2, iterator.next());
        assertSame(leaf3, iterator.next());
        assertNull(iterator.next());
        assertSame(branch1, iterator.first());
        iterator = new ElementIterator(leaf1);
        assertSame(leaf1, iterator.current());
        assertNull(iterator.next());
        assertNull(iterator.previous());
        iterator = new ElementIterator(leaf1);
        assertSame(leaf1, iterator.next());
        assertNull(iterator.current());
        assertNull(iterator.next());
        assertNull(iterator.previous());
    }

    /*
     * Test method for 'javax.swing.text.ElementIterator.first()'
     */
    public void testFirst() {
        Element first = iterator.current();
        assertNotSame(first, iterator.next());
        assertNotSame(first, iterator.next());
        assertNotSame(first, iterator.next());
        assertSame(first, iterator.first());
    }

    /*
     * Test method for 'javax.swing.text.ElementIterator.current()'
     */
    public void testCurrent() throws BadLocationException {
        assertSame(doc.getRootElements()[0], iterator.current());
        assertSame(iterator.current(), iterator.current());
        iterator.next();
        assertSame(iterator.current(), iterator.current());
    }

    /*
     * Test method for 'javax.swing.text.ElementIterator.next()'
     */
    public void testNext() {
        assertSame(root, iterator.next());
        assertSame(branch1, iterator.next());
        assertSame(leaf1, iterator.next());
        assertSame(leaf2, iterator.next());
        assertSame(leaf3, iterator.next());
        assertSame(branch2, iterator.next());
        assertSame(leaf4, iterator.next());
        assertSame(leaf5, iterator.next());
        assertSame(leaf6, iterator.next());
        assertNull(iterator.next());
    }

    /*
     * Test method for 'javax.swing.text.ElementIterator.previous()'
     */
    public void testPrevious() {
        assertNull(iterator.previous());
        assertSame(root, iterator.next());
        assertNull(iterator.previous());
        assertSame(branch1, iterator.next());
        assertSame(root, iterator.previous());
        assertSame(leaf1, iterator.next());
        assertSame(branch1, iterator.previous());
        assertSame(leaf2, iterator.next());
        assertSame(leaf1, iterator.previous());
        assertSame(leaf3, iterator.next());
        assertSame(leaf2, iterator.previous());
        assertSame(branch2, iterator.next());
        assertSame(leaf3, iterator.previous());
        assertSame(leaf4, iterator.next());
        assertSame(branch2, iterator.previous());
        assertSame(leaf5, iterator.next());
        assertSame(leaf4, iterator.previous());
        assertSame(leaf6, iterator.next());
        assertSame(leaf5, iterator.previous());
        assertNull(iterator.next());
        assertNull(iterator.previous());
    }

    /*
     * Test method for 'javax.swing.text.ElementIterator.depth()'
     */
    public void testDepth() throws BadLocationException {
        assertEquals(0, iterator.depth());
        iterator.current();
        assertEquals(1, iterator.depth());
        iterator.next();
        assertEquals(2, iterator.depth());
        iterator.next();
        assertEquals(3, iterator.depth());
        iterator.next();
        assertEquals(3, iterator.depth());
        iterator.next();
        assertEquals(3, iterator.depth());
        iterator.next();
        assertEquals(2, iterator.depth());
        iterator.next();
        assertEquals(3, iterator.depth());
        iterator.next();
        assertEquals(3, iterator.depth());
        iterator.next();
        assertEquals(3, iterator.depth());
        iterator.next();
        assertEquals(0, iterator.depth());
        iterator.first();
        assertEquals(1, iterator.depth());
        iterator = new ElementIterator(doc.getBidiRootElement());
        assertEquals(0, iterator.depth());
        iterator.next();
        assertEquals(1, iterator.depth());
        iterator.next();
        assertEquals(2, iterator.depth());
        iterator.next();
        assertEquals(0, iterator.depth());
    }

    /*
     * Test method for 'javax.swing.text.ElementIterator.clone()'
     */
    public void testClone() {
        iterator = new ElementIterator(branch2);
        iterator.current();
        iterator.next();
        ElementIterator cloned = (ElementIterator) iterator.clone();
        assertSame(leaf4, cloned.current());
        assertSame(branch2, cloned.previous());
        assertSame(leaf5, cloned.next());
        assertSame(leaf6, cloned.next());
        assertNull(cloned.next());
        assertSame(leaf4, iterator.current());
        assertSame(branch2, cloned.first());
    }
}
