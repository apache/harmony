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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import javax.swing.BasicSwingTestCase;
import javax.swing.text.AbstractDocument.AbstractElement;
import junit.framework.TestCase;

/**
 * Tests AbstractDocument.AbstractElement class.
 *
 */
public class AbstractDocument_AbstractElementTest extends TestCase {
    /**
     * Shared document object.
     */
    protected static DisAbstractedDocument aDocument;

    /**
     * Shared element object.
     */
    protected static DisAbstractedDocument.DisAbstractedElement aElement;

    /**
     * Shared element with parent (= aElement).
     */
    protected static AbstractElement parented;

    /**
     * Attribute set used to initialize <code>parented</code>.
     */
    protected static AttributeSet aSet;

    /**
     * Implementor of abtract methods in AbstractDocument.
     */
    protected static class DisAbstractedDocument extends AbstractDocument {
        private static final long serialVersionUID = 1L;

        protected DisAbstractedDocument(final Content content) {
            super(content);
        }

        /**
         * Implementor of abstract methods in AbstractElement.
         *
         */
        class DisAbstractedElement extends AbstractDocument.AbstractElement {
            private static final long serialVersionUID = 1L;

            public DisAbstractedElement(final Element initParent,
                    final AttributeSet initAttributes) {
                super(initParent, initAttributes);
            }

            @Override
            public Element getElement(final int a0) {
                return null;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Enumeration<?> children() {
                return null;
            }

            @Override
            public int getElementIndex(final int a0) {
                return 0;
            }

            @Override
            public boolean isLeaf() {
                return false;
            }

            @Override
            public boolean getAllowsChildren() {
                return false;
            }

            @Override
            public int getStartOffset() {
                return 0;
            }

            @Override
            public int getEndOffset() {
                return 0;
            }

            @Override
            public int getElementCount() {
                return 0;
            }
        }

        @Override
        public Element getParagraphElement(final int a0) {
            return null;
        }

        @Override
        public Element getDefaultRootElement() {
            return null;
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        init();
        aDocument.writeLock();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        aDocument.writeUnlock();
    }

    /**
     * Helper method used from <code>setUp</code> to initialize
     * shared objects which are declared static.
     */
    protected static void init() {
        StyleContextTest.sc = StyleContext.getDefaultStyleContext();
        aSet = StyleContextTest.addAttribute(null, 5, 3);
        aDocument = new DisAbstractedDocument(new GapContent());
        aDocument.writeLock();
        aElement = aDocument.new DisAbstractedElement(null, StyleContextTest.addAttribute(2));
        parented = aDocument.new DisAbstractedElement(aElement, aSet);
        aDocument.writeUnlock();
    }

    /**
     * Simple test of constructor.
     */
    public void testAbstractElement01() {
        assertSame(aElement, parented.getParentElement());
        assertTrue(parented.containsAttributes(aSet));
        assertSame(aElement, parented.getResolveParent());
        // Test with null attribute set
        aElement = aDocument.new DisAbstractedElement(null, null);
        assertEquals(0, aElement.getAttributeCount());
        // Following assertion fails, the reason being Element contains method
        // doesn't take into account parent element
        //assertTrue(parented.containsAttributes(aElement));
    }

    /**
     * Test non-null attributes argument when there's no writeLock.
     */
    public void testAbstractElement02() {
        aDocument.writeUnlock();
        // When attributes are null, the exception isn't thrown
        aElement = aDocument.new DisAbstractedElement(null, null);
        try {
            // When attributes are not null, though empty, the exception
            // is thrown.
            aElement = aDocument.new DisAbstractedElement(null, StyleContext
                    .getDefaultStyleContext().getEmptySet());
            fail("Error 'Illegal cast to MutableAttributeSet' " + "should be thrown");
        } catch (Error e) {
            // Do nothing
        } finally {
            // Lock the document again, so there'll be no error when
            // unlocking from tearDown
            aDocument.writeLock();
        }
    }

    public void testCopyAttributes() {
        AttributeSet copy = aElement.copyAttributes();
        assertNotSame(copy, aElement);
        assertFalse(copy instanceof Element);
        assertNull(aElement.getParentElement());
        assertNull(copy.getResolveParent());
        assertNull(aElement.getResolveParent());
        assertTrue(copy.isEqual(aElement));
        copy = parented.copyAttributes();
        assertNotSame(copy, parented);
        assertFalse(copy instanceof Element);
        assertNotNull(parented.getParentElement());
        assertNull(copy.getResolveParent());
        assertNotNull(parented.getResolveParent());
        assertTrue(copy.isEqual(parented));
    }

    public void testGetParentElement() {
        assertNull(aElement.getParentElement());
        assertNotNull(parented.getParentElement());
        assertSame(aElement, parented.getParentElement());
    }

    public void testGetDocument() {
        assertSame(aDocument, aElement.getDocument());
    }

    public void testGetName() {
        assertNull(aElement.getName());
        assertNull(parented.getName());
    }

    public void testDump() throws BadLocationException {
        String dump1 = "<bidi root>\n" + "  <bidi level\n" + "    bidiLevel=0\n" + "  >\n"
                + "    [0,6][01234\n" + "]\n" + "<content>\n" + "  [0,3][012]\n";
        String dump2 = "<bidi root>\n" + "  <bidi level\n" + "    bidiLevel=0\n" + "  >\n"
                + "    [0,6][01234\n" + "]\n" + "<content>\n" + "  [0,3][012]\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        aDocument.insertString(0, "01234", null);
        ((AbstractElement) aDocument.getBidiRootElement()).dump(ps, 0);
        aDocument.new LeafElement(null, null, 0, 3).dump(ps, 0);
        String dump = BasicSwingTestCase.isHarmony() ? dump2 : dump1;
        assertEquals(dump, AbstractDocumentTest.filterNewLines(out.toString()));
    }

    private static final String dumpTextAtLimit = "123456789\n" + // 10 chars / 10
            "123456789\n" + // 10 chars / 20
            "123456789\n" + // 10 chars / 30
            "aaabbbccc"; //  9 chars / 39 + 1 def '\n'

    private static final String dumpTextBeyondLimit = "123456789\n" + // 10 chars / 10
            "123456789\n" + // 10 chars / 20
            "123456789\n" + // 10 chars / 30
            "aaabbbccc1234567890"; // 19 chars / 49 + 1 def '\n'

    public void testDumpAtTextLimit() throws BadLocationException {
        String dump = "<bidi root>\n" + "  <bidi level\n" + "    bidiLevel=0\n" + "  >\n"
                + "    [0,40][123456789\n" + "123456789\n" + "123456789\n" + "aaabbbccc\n"
                + "]\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        aDocument.insertString(0, dumpTextAtLimit, null);
        ((AbstractElement) aDocument.getBidiRootElement()).dump(ps, 0);
        assertEquals(dump, AbstractDocumentTest.filterNewLines(out.toString()));
    }

    public void testDumpBeyondTextLimit() throws BadLocationException {
        String dump = "<bidi root>\n" + "  <bidi level\n" + "    bidiLevel=0\n" + "  >\n"
                + "    [0,50][123456789\n" + "123456789\n" + "123456789\n" + "aaabbbccc1...]\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        aDocument.insertString(0, dumpTextBeyondLimit, null);
        ((AbstractElement) aDocument.getBidiRootElement()).dump(ps, 0);
        assertEquals(dump, AbstractDocumentTest.filterNewLines(out.toString()));
    }

    public void testGetAttributeThruParent() {
        assertFalse(parented.isDefined(StyleContextTest.attr[0]));
        assertTrue(aElement.isDefined(StyleContextTest.attr[0]));
        // containsAttribute method DOES NOT look up attributes in parent
        assertFalse(parented.containsAttribute(StyleContextTest.attr[1], parented
                .getAttribute(StyleContextTest.attr[0])));
        // getAttribute DOES look up attributes in parent
        assertSame(StyleContextTest.attr[1], parented.getAttribute(StyleContextTest.attr[0]));
    }

    public void testGetResolveParent() {
        assertNull(aElement.getResolveParent());
        assertSame(aElement, parented.getResolveParent());
    }

    public void testGetResolveParentExplicit() throws Exception {
        AttributeSet resolver = new SimpleAttributeSet();
        parented.setResolveParent(resolver);
        assertSame(resolver, parented.getResolveParent());
        assertSame(aElement, parented.getParentElement());
    }

    protected static class OtherElement implements Element {
        public static final String key = "OE_Key";

        public static final String value = "OE_Value";

        private MutableAttributeSet attrs = new SimpleAttributeSet();
        {
            attrs.addAttribute(key, value);
        }

        public boolean isAttrsNull;

        public AttributeSet getAttributes() {
            return isAttrsNull ? null : attrs;
        }

        public Document getDocument() {
            return null;
        }

        public Element getElement(int index) {
            return null;
        }

        public int getElementCount() {
            return 0;
        }

        public int getElementIndex(int offset) {
            return 0;
        }

        public int getEndOffset() {
            return 0;
        }

        public String getName() {
            return null;
        }

        public Element getParentElement() {
            return null;
        }

        public int getStartOffset() {
            return 0;
        }

        public boolean isLeaf() {
            return true;
        }
    }

    public void testGetAttributesOtherParent() throws Exception {
        OtherElement parent = new OtherElement();
        parented = aDocument.new DisAbstractedElement(parent, null);
        assertSame(OtherElement.value, parented.getAttribute(OtherElement.key));
        assertFalse(parented.isDefined(OtherElement.key));
        parent.isAttrsNull = true;
        assertNull(parented.getAttribute(OtherElement.key));
    }

    public void testGetResolveParentOther() throws Exception {
        OtherElement parent = new OtherElement();
        parented = aDocument.new DisAbstractedElement(parent, null);
        assertNotSame(parent, parent.getAttributes());
        assertSame(parent.getAttributes(), parented.getResolveParent());
        parent.isAttrsNull = true;
        assertNull(parented.getResolveParent());
    }
    // These are tests for abstract methods
    /*
     public void testIsLeaf() {

     }

     public void testChildren() {

     }

     public void testGetAllowsChildren() {

     }

     public void testGetStartOffset() {

     }

     public void testGetEndOffset() {

     }

     public void testGetElementCount() {

     }
     */
}
