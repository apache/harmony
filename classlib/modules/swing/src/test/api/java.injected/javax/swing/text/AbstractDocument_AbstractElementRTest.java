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

import java.util.Enumeration;
import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.text.AbstractDocument.BranchElement;
import junit.framework.TestCase;

public class AbstractDocument_AbstractElementRTest extends TestCase {
    private static class AbstractElementImpl extends AbstractElement {
        private static final long serialVersionUID = 1L;

        public static final int START_OFFSET = -101;

        public static final int END_OFFSET = -10;

        public AbstractElementImpl(final AbstractDocument doc, final Element parent,
                final AttributeSet attrs) {
            doc.super(parent, attrs);
        }

        @Override
        public boolean getAllowsChildren() {
            return false;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Enumeration<?> children() {
            return null;
        }

        @Override
        public Element getElement(int index) {
            return null;
        }

        @Override
        public int getElementCount() {
            return 0;
        }

        @Override
        public int getElementIndex(int offset) {
            return 0;
        }

        @Override
        public int getStartOffset() {
            return START_OFFSET;
        }

        @Override
        public int getEndOffset() {
            return END_OFFSET;
        }
    }

    private AbstractDocument doc;

    private Element root;

    private Element element;

    private MutableAttributeSet attrs;

    public void testGetNameAbstract() throws Exception {
        final String name = "Unbekannte";
        attrs.addAttribute(AbstractDocument.ElementNameAttribute, name);
        element = new AbstractElementImpl(doc, root, attrs);
        assertSame(name, element.getName());
        assertSame(root, element.getParentElement());
        attrs.removeAttribute(AbstractDocument.ElementNameAttribute);
        assertEquals(0, attrs.getAttributeCount());
        element = new AbstractElementImpl(doc, root, attrs);
        assertNull(element.getName());
        assertSame(root, element.getParentElement());
    }

    public void testGetNameLeaf() throws Exception {
        final String name = "Blatt";
        attrs.addAttribute(AbstractDocument.ElementNameAttribute, name);
        element = doc.new LeafElement(root, attrs, 15, 23);
        assertSame(name, element.getName());
        assertEquals(15, element.getStartOffset());
        assertEquals(23, element.getEndOffset());
        assertSame(root, element.getParentElement());
    }

    public void testGetNameBranch() throws Exception {
        final String name = "Ast";
        attrs.addAttribute(AbstractDocument.ElementNameAttribute, name);
        element = doc.new BranchElement(root, attrs);
        assertSame(name, element.getName());
        assertSame(root, element.getParentElement());
    }

    public void testGetNameNonString() throws Exception {
        attrs.addAttribute(AbstractDocument.ElementNameAttribute, new Integer(10101));
        element = new AbstractElementImpl(doc, root, attrs);
        try {
            element.getName();
            fail("ClassCastException is expected");
        } catch (ClassCastException e) {
        }
    }

    public void testGetNameParent() throws Exception {
        final String parentName = "parentName";
        attrs.addAttribute(AbstractDocument.ElementNameAttribute, parentName);
        BranchElement parent = doc.new BranchElement(null, attrs);
        AbstractElement element = new AbstractElementImpl(doc, parent, null);
        assertTrue(parent.isDefined(AbstractDocument.ElementNameAttribute));
        assertEquals(parentName, parent.getName());
        assertFalse(element.isDefined(AbstractDocument.ElementNameAttribute));
        assertNull(element.getName());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        doc = new PlainDocument();
        root = doc.getDefaultRootElement();
        attrs = new SimpleAttributeSet();
        doc.writeLock();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        doc.writeUnlock();
    }
}
