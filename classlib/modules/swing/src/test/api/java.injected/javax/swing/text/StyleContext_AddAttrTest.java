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
import javax.swing.text.StyleContext.SmallAttributeSet;
import junit.framework.TestCase;

/**
 * Tests for addAttribute method.
 *
 */
public class StyleContext_AddAttrTest extends TestCase {
    private StyleContext sc;

    /**
     * Add two same attributes into two sets using different order.
     * The cache should return the same objects.
     */
    public void testAddAttributeCacheDiffOrder() {
        AttributeSet as1 = sc.addAttribute(sc.getEmptySet(), StyleConstants.Bold, Boolean.TRUE);
        AttributeSet as2 = sc.addAttribute(as1, StyleConstants.Italic, Boolean.FALSE);
        AttributeSet as12 = sc.addAttribute(sc.getEmptySet(), StyleConstants.Italic,
                Boolean.FALSE);
        AttributeSet as22 = sc.addAttribute(as12, StyleConstants.Bold, Boolean.TRUE);
        assertSame(as2, as22);
        assertEquals(2, as2.getAttributeCount());
    }

    /**
     * Add two same attributes into two sets using the same order.
     * The cache should return the same objects.
     */
    public void testAddAttributeCacheSameOrder() {
        AttributeSet as11 = sc
                .addAttribute(sc.getEmptySet(), StyleConstants.Bold, Boolean.TRUE);
        AttributeSet as21 = sc.addAttribute(as11, StyleConstants.Italic, Boolean.FALSE);
        AttributeSet as12 = sc
                .addAttribute(sc.getEmptySet(), StyleConstants.Bold, Boolean.TRUE);
        AttributeSet as22 = sc.addAttribute(as12, StyleConstants.Italic, Boolean.FALSE);
        assertSame(as11, as12);
        assertSame(as21, as22);
        assertEquals(1, as11.getAttributeCount());
        assertEquals(2, as21.getAttributeCount());
    }

    /**
     * Add two different key/value pair to an empty set.
     * Check the return instances are different.
     */
    public void testAddAttributeDiff() {
        AttributeSet as1 = sc.addAttribute(sc.getEmptySet(), StyleConstants.Bold, Boolean.TRUE);
        AttributeSet as2 = sc
                .addAttribute(sc.getEmptySet(), StyleConstants.Bold, Boolean.FALSE);
        assertNotSame(as1, as2);
    }

    /**
     * Add nine attributes into a set.
     */
    public void testAddAttributeNine() {
        AttributeSet as = StyleContextTest.addAttribute(9);
        assertEquals(9, as.getAttributeCount());
        assertTrue(as instanceof SmallAttributeSet);
    }

    /**
     * Add nine attributes into a set, and try to add the tenth one
     * which is already in the set.
     */
    public void testAddAttributeNineSameValue() {
        AttributeSet as = StyleContextTest.addAttribute(9);
        as = sc.addAttribute(as, StyleConstants.Bold, Boolean.TRUE);
        assertEquals(9, as.getAttributeCount());
        if (!BasicSwingTestCase.isHarmony()) {
            assertTrue(as instanceof SimpleAttributeSet);
        } else {
            assertTrue(as instanceof SmallAttributeSet);
        }
    }

    /**
     * Add the same key/value pair to an empty set.
     * Check the returned instances are the same.
     */
    public void testAddAttributeSame() {
        AttributeSet as1 = StyleContextTest.addAttribute(null, 1);
        AttributeSet as2 = StyleContextTest.addAttribute(null, 1);
        assertSame(as1, as2);
    }

    /**
     * Add the same attribute value but with different objects.
     * Check the returned sets are the same.
     */
    public void testAddAttributeSimilar() {
        AttributeSet as1 = sc.addAttribute(sc.getEmptySet(), StyleConstants.FontSize,
                new Integer(10));
        AttributeSet as2 = sc.addAttribute(sc.getEmptySet(), StyleConstants.FontSize,
                new Integer(10));
        assertSame(as1, as2);
    }

    /**
     * Add the value into an attribute set twice using two
     * different objects.
     */
    public void testAddAttributeSimilarTwice() {
        AttributeSet as1 = sc.addAttribute(sc.getEmptySet(), StyleConstants.FontSize,
                new Integer(10));
        AttributeSet as2 = sc.addAttribute(as1, StyleConstants.FontSize, new Integer(10));
        assertSame(as1, as2);
        assertEquals(1, as1.getAttributeCount());
    }

    /**
     * Add ten attributes into a set.
     */
    public void testAddAttributeTen() {
        AttributeSet as = StyleContextTest.addAttribute(10);
        assertEquals(10, as.getAttributeCount());
        assertTrue(as instanceof SimpleAttributeSet);
    }

    /**
     * Add three attributes into a set.
     */
    public void testAddAttributeThree() {
        AttributeSet as = StyleContextTest.addAttribute(3);
        assertEquals(3, as.getAttributeCount());
        assertTrue(as instanceof SmallAttributeSet);
    }

    /**
     * Add four attributes into a set, but one of them is used twice,
     * so the returned attribute set should contain only three ones.
     */
    public void testAddAttributeThreeSameKey() {
        AttributeSet as = StyleContextTest.addAttribute(3);
        as = sc.addAttribute(as, StyleConstants.Bold, Boolean.FALSE);
        assertEquals(3, as.getAttributeCount());
        assertTrue(as instanceof SmallAttributeSet);
    }

    /**
     * Add two attributes into a set.
     */
    public void testAddAttributeTwo() {
        AttributeSet as = StyleContextTest.addAttribute(2);
        assertEquals(2, as.getAttributeCount());
        assertTrue(as instanceof SmallAttributeSet);
    }

    /**
     * Create an attribute set of two elements. Then create the same
     * set using MutableAttributeSet implementor.
     */
    public void testAddMutable() {
        AttributeSet as = StyleContextTest.addAttribute(3);
        SimpleAttributeSet sas = new SimpleAttributeSet();
        int i;
        for (i = 0; i < 4; i += 2) {
            sas.addAttribute(StyleContextTest.attr[i], StyleContextTest.attr[i + 1]);
        }
        AttributeSet set = sc.addAttribute(sas, StyleContextTest.attr[i],
                StyleContextTest.attr[i + 1]);
        assertEquals(3, set.getAttributeCount());
        assertSame(as, set);
    }

    @Override
    protected void setUp() {
        sc = StyleContextTest.sc = new StyleContext();
    }
}
