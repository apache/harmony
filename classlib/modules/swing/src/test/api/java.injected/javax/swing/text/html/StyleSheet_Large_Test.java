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
package javax.swing.text.html;

import java.util.Enumeration;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS.Attribute;

/**
 * Tests <code>MutableAttributeSet</code> returned by
 * <code>StyleSheet.addAttribute</code> method. The returned attribute set
 * contains <code>CSS.Attribute</code>. It converts the value to
 * <code>StyleConstants</code> when a <code>StyleConstants</code> attribute
 * is requested.
 * <p>
 * The attribute used for testing is <code>StyleConstants.Italic</code>.
 * It corresponds to <code>Attribute.FONT_STYLE</code>.
 *
 * @see StyleSheet
 * @see StyleSheet#addAttribute(Object, Object)
 * @see StyleSheet#createLargeAttributeSet(AttributeSet)
 * @see CSS.Attribute
 * @see javax.swing.text.StyleContext.SmallAttributeSet
 */
public class StyleSheet_Large_Test extends BasicSwingTestCase {
    private static final Object scAttribute  = StyleConstants.Italic;
    private static final Object scValue      = Boolean.TRUE;
    private static final Object cssAttribute = Attribute.FONT_STYLE;

    private StyleSheet ss;
    private AttributeSet empty;
    private AttributeSet attr;
    private MutableAttributeSet mutable;
    private int count;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        empty = ss.getEmptySet();
        attr = empty;

        int no = 0;
        while (!(attr instanceof MutableAttributeSet)) {
            String number = String.valueOf(no++);
            attr = ss.addAttribute(attr, "key" + number, "value" + number);
        }
        attr = ss.addAttribute(attr, scAttribute, scValue);
        assertTrue(attr instanceof MutableAttributeSet);
        count = no + 1;

        mutable = (MutableAttributeSet)attr;
    }

    public void testGetAttribute() throws Exception {
        Object value = attr.getAttribute(cssAttribute);
        assertNotSame(Boolean.class, value.getClass());
        assertNotSame(String.class, value.getClass());
        assertEquals("italic", value.toString());

        value = attr.getAttribute(scAttribute);
        assertSame(Boolean.class, value.getClass());
        assertTrue(((Boolean)value).booleanValue());
        assertSame(scValue, value);
    }

    public void testIsDefined() throws Exception {
        assertTrue(attr.isDefined(scAttribute));
        assertTrue(attr.isDefined(cssAttribute));
    }

    public void testGetNames() throws Exception {
        final Enumeration keys = attr.getAttributeNames();
        int stringKeyCount = 0;
        int nonStringKeyCount = 0;
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String) {
                stringKeyCount++;
            } else {
                nonStringKeyCount++;
                assertSame(cssAttribute, key);
            }
        }

        assertEquals(count - 1, stringKeyCount);
        assertEquals(1, nonStringKeyCount);
    }

    public void testGetAttributeCount() throws Exception {
        assertEquals(count, attr.getAttributeCount());
    }

    public void testIsEqualSame() throws Exception {
        assertTrue(attr.isEqual(attr));

        final MutableAttributeSet simple = new SimpleAttributeSet(attr);
        assertTrue(attr.isEqual(simple));
        assertTrue(simple.isEqual(attr));
    }

    public void testIsEqualAnother() throws Exception {
        final MutableAttributeSet simple = new SimpleAttributeSet();
        fillAttributeSet(simple);

        if (isHarmony()) {
            assertTrue(simple.isEqual(attr));
            assertFalse(attr.isEqual(simple));
        } else {
            assertFalse(simple.isEqual(attr));
            assertTrue(attr.isEqual(simple));
        }
    }

    public void testCopyAttributes() throws Exception {
        final AttributeSet copy = attr.copyAttributes();
        assertNotSame(attr, copy);
        assertTrue(attr.isEqual(copy));
        assertTrue(copy.isEqual(attr));
        assertTrue(copy instanceof MutableAttributeSet);
    }

    public void testContainsAttribute() throws Exception {
        assertTrue(attr.containsAttribute(cssAttribute,
                                          attr.getAttribute(cssAttribute)));
        assertTrue(attr.containsAttribute(scAttribute, scValue));
    }

    public void testContainsAttributeAnother() throws Exception {
        final AttributeSet another = ss.addAttribute(empty,
                                                     scAttribute, scValue);

        assertEquals(attr.getAttribute(cssAttribute).toString(),
                     another.getAttribute(cssAttribute).toString());
        if (isHarmony()) {
            assertTrue(attr.containsAttribute(cssAttribute,
                                           another.getAttribute(cssAttribute)));
        } else {
            assertFalse(attr.containsAttribute(cssAttribute,
                                           another.getAttribute(cssAttribute)));
        }
    }

    public void testContainsAttributesSame() throws Exception {
        assertTrue(attr.containsAttributes(attr));

        final MutableAttributeSet simple = new SimpleAttributeSet(attr);

        assertTrue(attr.containsAttributes(simple));
        assertTrue(simple.containsAttributes(attr));
    }

    public void testContainsAttributesMutable() throws Exception {
        final MutableAttributeSet simple = new SimpleAttributeSet();
        simple.addAttribute(scAttribute, scValue);

        assertTrue(attr.containsAttributes(simple));
        assertFalse(simple.containsAttributes(attr));
    }

    public void testContainsAttributesSmall() throws Exception {
        final AttributeSet another = ss.addAttribute(empty,
                                                     scAttribute, scValue);

        if (isHarmony()) {
            assertSame(attr.getAttribute(Attribute.FONT_STYLE),
                       another.getAttribute(Attribute.FONT_STYLE));
            assertTrue(attr.containsAttributes(another));
            assertFalse(another.containsAttributes(attr));
        } else {
            assertNotSame(attr.getAttribute(Attribute.FONT_STYLE),
                          another.getAttribute(Attribute.FONT_STYLE));
            assertFalse(attr.containsAttributes(another));
            assertFalse(another.containsAttributes(attr));
        }
    }

    public void testAddAttribute() throws Exception {
        mutable.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        assertEquals(count + 1, mutable.getAttributeCount());
        assertItalicButNotBold();
    }

    public void testAddAttributes() throws Exception {
        MutableAttributeSet simple = new SimpleAttributeSet();
        simple.addAttribute(StyleConstants.Bold, Boolean.TRUE);

        mutable.addAttributes(simple);
        assertEquals(count + 1, mutable.getAttributeCount());
        assertItalicButNotBold();
    }

    public void testRemoveAttribute() throws Exception {
        mutable.removeAttribute(scAttribute);
        assertEquals(count, mutable.getAttributeCount()); // Didn't change

        mutable.removeAttribute(cssAttribute);
        assertEquals(count - 1, mutable.getAttributeCount());
    }

    public void testRemoveAttributesEnumeration() throws Exception {
        mutable.removeAttributes(new Enumeration() {
            private int returnedCount = 0;

            public boolean hasMoreElements() {
                return returnedCount == 0;
            }

            public Object nextElement() {
                return returnedCount++ == 0 ? scAttribute : null;
            }
        });
        assertEquals(count, mutable.getAttributeCount()); // Didn't change

        mutable.removeAttributes(new Enumeration() {
            private int returnedCount = 0;

            public boolean hasMoreElements() {
                return returnedCount == 0;
            }

            public Object nextElement() {
                return returnedCount++ == 0 ? cssAttribute : null;
            }
        });
        assertEquals(count - 1, mutable.getAttributeCount());
    }

    public void testRemoveAttributesAttributeSet() throws Exception {
        final MutableAttributeSet sc = new SimpleAttributeSet();
        sc.addAttribute(scAttribute, scValue);

        mutable.removeAttributes(sc);
        assertEquals(count, mutable.getAttributeCount()); // Didn't change

        final MutableAttributeSet css = new SimpleAttributeSet();
        css.addAttribute(cssAttribute, mutable.getAttribute(cssAttribute));
        mutable.removeAttributes(css);
        assertEquals(count - 1, mutable.getAttributeCount());
    }

    private void fillAttributeSet(final MutableAttributeSet simple) {
        for (int i = 0; i < count - 1; i++) {
            simple.addAttribute("key" + i, "value" + i);
        }
        simple.addAttribute(scAttribute, scValue);
    }

    private void assertItalicButNotBold() {
        final Enumeration keys = attr.getAttributeNames();
        int stringKeyCount = 0;
        int nonStringKeyCount = 0;
        boolean cssItalic = false;
        boolean cssBold = false;
        boolean scBold = false;
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String) {
                stringKeyCount++;
            } else {
                nonStringKeyCount++;
                cssItalic |= cssAttribute == key;
                cssBold |= Attribute.FONT_WEIGHT == key;
                scBold |= StyleConstants.Bold == key;
            }
        }

        assertEquals(count - 1, stringKeyCount);
        assertEquals(2, nonStringKeyCount);
        assertTrue(cssItalic);
        assertFalse(cssBold);       // No convertion is performed when used
        assertTrue(scBold);         // as mutable set
    }
}
