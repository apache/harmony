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
import javax.swing.BasicSwingTestCase;
import javax.swing.text.StyleContext.SmallAttributeSet;
import junit.framework.TestCase;

/**
 * Tests for removeAttribute, removeAttributes(Enumeration),
 * removeAttributes(AttributeSet) methods.
 *
 */
public class StyleContext_RemoveTest extends TestCase {
    /**
     * Class supporting testing of removeAttributes(Enumeration) method
     * of StyleContext.
     */
    @SuppressWarnings("unchecked")
    private class AttributeNameEnumeration implements Enumeration {
        private int count;

        private int index;

        /**
         * Creates a new enumeration with the specified number of
         * attribute names, starting at index 0.
         *
         * @param count the number of elements
         */
        public AttributeNameEnumeration(final int count) {
            this.index = 0;
            this.count = count;
        }

        /**
         * Creates a new enumeration with the specified number of
         * attribute names, starting at index specified.
         *
         * @param start the start index in internal attribute array
         * @param count the number of elements
         */
        public AttributeNameEnumeration(final int start, final int count) {
            this.index = start;
            this.count = count + start;
        }

        public boolean hasMoreElements() {
            return index < count;
        }

        public Object nextElement() {
            return StyleContextTest.attr[2 * index++];
        }
    }

    private StyleContext sc;

    /**
     * Just removes an attribute.
     */
    public void testRemoveAttribute() {
        AttributeSet as = StyleContextTest.addAttribute(5);
        as = sc.removeAttribute(as, StyleContextTest.attr[2]);
        assertEquals(4, as.getAttributeCount());
    }

    /**
     * Removes an attribute, and the new set should be from cache.
     */
    public void testRemoveAttributeCache() {
        AttributeSet as = StyleContextTest.addAttribute(4);
        SimpleAttributeSet sas = new SimpleAttributeSet(as);
        sas.addAttribute(StyleContextTest.attr[5 * 2], StyleContextTest.attr[5 * 2 + 1]);
        AttributeSet test = sc.removeAttribute(sas, StyleContextTest.attr[5 * 2]);
        assertEquals(4, test.getAttributeCount());
        assertSame(as, test);
    }

    /**
     * Removes 1 attribute from a set, and the set is converted to
     * SmallAttributeSet
     *
     */
    public void testRemoveAttributeCollapse() {
        AttributeSet as = StyleContextTest.addAttribute(10);
        assertTrue(as instanceof SimpleAttributeSet);
        as = sc.removeAttribute(as, StyleContextTest.attr[0]);
        assertEquals(9, as.getAttributeCount());
        assertTrue(as instanceof SmallAttributeSet);
    }

    /**
     * Removes the last attribute from a set.
     */
    public void testRemoveAttributeFromOne() {
        AttributeSet as = StyleContextTest.addAttribute(1);
        as = sc.removeAttribute(as, StyleContextTest.attr[0]);
        assertEquals(0, as.getAttributeCount());
        if (!BasicSwingTestCase.isHarmony()) {
            assertTrue(as instanceof SmallAttributeSet);
        } else {
            assertSame(as, sc.getEmptySet());
        }
    }

    /**
     * Removes an attribute, the new set being placed in the cache.
     * Check that the set is in the cached with creation another
     * set with the same attributes.
     */
    public void testRemoveAttributeNoCache() {
        SimpleAttributeSet sas = new SimpleAttributeSet();
        for (int i = 0; i < 6; i += 2) {
            sas.addAttribute(StyleContextTest.attr[i], StyleContextTest.attr[i + 1]);
        }
        AttributeSet test = sc.removeAttribute(sas, StyleContextTest.attr[4]);
        // Create the same set as test
        AttributeSet as = StyleContextTest.addAttribute(2);
        assertSame(test, as);
    }

    public void testRemoveAttributeNoKey() {
        AttributeSet as = StyleContextTest.addAttribute(2);
        AttributeSet test = sc.removeAttribute(as, StyleContextTest.attr[4]);
        assertSame(as, test);
    }

    /**
     * Removes some attributes from the set. The result should be
     * from the cache.
     */
    public void testRemoveAttributesEnumCache() {
        AttributeSet cached = StyleContextTest.addAttribute(1);
        AttributeSet as = StyleContextTest.addAttribute(cached, 1, 2);
        AttributeSet test = sc.removeAttributes(as, new AttributeNameEnumeration(1, 2));
        assertEquals(1, test.getAttributeCount());
        assertSame(cached, test);
    }

    /**
     * Removes all the attributes from a set.
     * The result should be EmptyAttributeSet.
     */
    public void testRemoveAttributesEnumEmpty() {
        AttributeSet as = StyleContextTest.addAttribute(3);
        AttributeSet test = sc.removeAttributes(as, new AttributeNameEnumeration(3));
        assertEquals(0, test.getAttributeCount());
        if (BasicSwingTestCase.isHarmony()) {
            assertSame(test, sc.getEmptySet());
        }
    }

    /**
     * Removes some attributes from a set. The result shouldn't be
     * in the cache.
     */
    public void testRemoveAttributesEnumNoCache() {
        AttributeSet as = StyleContextTest.addAttribute(4);
        AttributeSet test = sc.removeAttributes(as, new AttributeNameEnumeration(1, 2));
        assertEquals(2, test.getAttributeCount());
        // Create the same set as test
        AttributeSet cached = StyleContextTest.addAttribute(1);
        cached = StyleContextTest.addAttribute(cached, 3, 1);
        assertSame(test, cached);
    }

    /**
     * Removes several attributes from a set. The name enumeration
     * contains names that are not in the set.
     */
    public void testRemoveAttributesEnumNoKey() {
        AttributeSet as = StyleContextTest.addAttribute(2);
        as = StyleContextTest.addAttribute(as, 4, 2);
        assertEquals(4, as.getAttributeCount());
        AttributeSet test = sc.removeAttributes(as, new AttributeNameEnumeration(1, 4));
        assertEquals(2, test.getAttributeCount());
    }

    /**
     * Removes one attribute from a set. The resulting set should become
     * SmallAttributeSet.
     */
    public void testRemoveAttributesEnumSmall() {
        AttributeSet as = StyleContextTest.addAttribute(10);
        assertTrue(as instanceof SimpleAttributeSet);
        // Remove one element
        AttributeSet test = sc.removeAttributes(as, new AttributeNameEnumeration(1));
        assertEquals(9, test.getAttributeCount());
        if (BasicSwingTestCase.isHarmony()) {
            assertTrue(test instanceof SmallAttributeSet);
        }
    }

    /**
     * Removes some attributes from the set. The result should be
     * from the cache.
     */
    public void testRemoveAttributesSetCache() {
        AttributeSet cached = StyleContextTest.addAttribute(1);
        AttributeSet as = StyleContextTest.addAttribute(cached, 1, 2);
        AttributeSet test = sc.removeAttributes(as, StyleContextTest.addAttribute(null, 1, 2));
        assertEquals(1, test.getAttributeCount());
        assertSame(cached, test);
    }

    /**
     * Removes some attributes from the set. The result should be
     * from the cache. The set to remove contains a key with a different
     * value.
     */
    public void testRemoveAttributesSetCacheSameKeys() {
        AttributeSet cached = StyleContextTest.addAttribute(2);
        AttributeSet as = StyleContextTest.addAttribute(cached, 2, 3);
        AttributeSet test = sc.removeAttributes(as, sc.addAttribute(StyleContextTest
                .addAttribute(null, 1, 4), StyleConstants.Italic, Boolean.FALSE));
        assertEquals(2, test.getAttributeCount());
        assertSame(cached, test);
    }

    /**
     * Removes all the attributes from a set. The set to be removed is
     * the same as the set to remove from.
     * The result should be EmptyAttributeSet.
     */
    public void testRemoveAttributesSetEmpty() {
        AttributeSet as = StyleContextTest.addAttribute(3);
        AttributeSet test = sc.removeAttributes(as, as);
        assertEquals(0, test.getAttributeCount());
        if (BasicSwingTestCase.isHarmony()) {
            assertSame(test, sc.getEmptySet());
        }
    }

    /**
     * Removes all the attributes from a set. The set to be removed
     * contains the same attribute key but has different values.
     * The result should be EmptyAttributeSet.
     */
    public void testRemoveAttributesSetEmptySameKey() {
        AttributeSet as = StyleContextTest.addAttribute(3);
        AttributeSet test = sc.removeAttributes(as, sc.addAttribute(as, StyleConstants.Bold,
                Boolean.FALSE));
        assertEquals(1, test.getAttributeCount());
    }

    /**
     * Removes some attributes from a set. The result shouldn't be
     * in the cache. The set to remove contains a key with a different
     * value.
     */
    public void testRemoveAttributesSetNoCacheSameKeys() {
        AttributeSet as = StyleContextTest.addAttribute(4);
        AttributeSet test = sc.removeAttributes(as, sc.addAttribute(StyleContextTest
                .addAttribute(null, 1, 2), StyleConstants.Underline, Boolean.FALSE));
        assertEquals(3, test.getAttributeCount());
        // Create the same set as test
        AttributeSet cached = StyleContextTest.addAttribute(1);
        cached = StyleContextTest.addAttribute(cached, 2, 2);
        assertSame(test, cached);
    }

    /**
     * Removes several attributes from a set. The set to be removed
     * contains attributes with names that are not in the set from which
     * attributes are to be removed.
     */
    public void testRemoveAttributesSetNoKey() {
        AttributeSet as = StyleContextTest.addAttribute(2);
        as = StyleContextTest.addAttribute(as, 4, 2);
        assertEquals(4, as.getAttributeCount());
        AttributeSet test = sc.removeAttributes(as, StyleContextTest.addAttribute(null, 1, 4));
        assertEquals(2, test.getAttributeCount());
    }

    /**
     * Removes one attribute from a set. The resulting set should become
     * SmallAttributeSet.
     */
    public void testRemoveAttributesSetSmall() {
        AttributeSet as = StyleContextTest.addAttribute(10);
        assertTrue(as instanceof SimpleAttributeSet);
        // Remove one element
        AttributeSet test = sc.removeAttributes(as, StyleContextTest.addAttribute(1));
        assertEquals(9, test.getAttributeCount());
        if (BasicSwingTestCase.isHarmony()) {
            assertTrue(test instanceof SmallAttributeSet);
        }
    }

    @Override
    protected void setUp() throws Exception {
        sc = StyleContextTest.sc = new StyleContext();
    }
}