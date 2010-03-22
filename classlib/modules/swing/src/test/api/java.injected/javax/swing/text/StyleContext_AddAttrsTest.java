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
 * Tests for addAttributes method of StyleContext.
 *
 */
public class StyleContext_AddAttrsTest extends TestCase {
    private StyleContext sc;

    public void testAddAttributesNine() {
        AttributeSet as1 = StyleContextTest.addAttribute(5);
        AttributeSet as2 = StyleContextTest.addAttribute(null, 5, 4);
        AttributeSet test = sc.addAttributes(as1, as2);
        assertEquals(9, test.getAttributeCount());
        assertTrue(test instanceof SmallAttributeSet);
    }

    public void testAddAttributesNineAndNine() {
        AttributeSet as1 = StyleContextTest.addAttribute(9);
        AttributeSet as2 = StyleContextTest.addAttribute(9);
        AttributeSet test = sc.addAttributes(as1, as2);
        assertEquals(9, test.getAttributeCount());
        if (!BasicSwingTestCase.isHarmony()) {
            assertTrue(test instanceof SimpleAttributeSet);
        } else {
            assertTrue(test instanceof SmallAttributeSet);
        }
    }

    public void testAddAttributesNineFull() {
        AttributeSet as1 = StyleContextTest.addAttribute(8);
        AttributeSet as2 = StyleContextTest.addAttribute(9);
        AttributeSet test = sc.addAttributes(as1, as2);
        assertEquals(9, test.getAttributeCount());
        if (!BasicSwingTestCase.isHarmony()) {
            assertTrue(test instanceof SimpleAttributeSet);
        } else {
            assertTrue(test instanceof SmallAttributeSet);
        }
    }

    public void testAddAttributesSameValues() {
        AttributeSet as1 = StyleContextTest.addAttribute(2);
        AttributeSet as2 = new SimpleAttributeSet(StyleContextTest.addAttribute(2));
        assertNotSame(as1, as2);
        assertEquals(2, as2.getAttributeCount());
        AttributeSet test = sc.addAttributes(as1, as2);
        assertSame(as1, test);
        assertEquals(2, test.getAttributeCount());
    }

    public void testAddAttributesTen() {
        AttributeSet as1 = StyleContextTest.addAttribute(5);
        AttributeSet as2 = StyleContextTest.addAttribute(null, 5, 5);
        AttributeSet test = sc.addAttributes(as1, as2);
        assertEquals(10, test.getAttributeCount());
        assertTrue(test instanceof SimpleAttributeSet);
    }

    public void testAddAttributesTenFull() {
        AttributeSet as1 = StyleContextTest.addAttribute(9);
        AttributeSet as2 = StyleContextTest.addAttribute(9);
        as2 = sc.addAttribute(as2, StyleConstants.LineSpacing, new Float(0.5));
        AttributeSet test = sc.addAttributes(as1, as2);
        assertEquals(10, test.getAttributeCount());
        assertTrue(test instanceof SimpleAttributeSet);
    }

    public void testAddAttributesThreeSame() {
        AttributeSet as = sc.addAttributes(StyleContextTest.addAttribute(3), StyleContextTest
                .addAttribute(1));
        assertEquals(3, as.getAttributeCount());
    }

    public void testAddAttributesThreeSameKey() {
        AttributeSet as = sc.addAttributes(StyleContextTest.addAttribute(3), sc.addAttribute(sc
                .getEmptySet(), StyleContextTest.attr[0], Boolean.FALSE));
        assertEquals(3, as.getAttributeCount());
        assertEquals(Boolean.FALSE, as.getAttribute(StyleContextTest.attr[0]));
    }

    public void testAddAttributesWithoutCache() {
        AttributeSet as1 = StyleContextTest.addAttribute(5);
        AttributeSet as2 = StyleContextTest.addAttribute(null, 5, 3);
        as2 = sc.addAttribute(as2, StyleConstants.LineSpacing, new Float(0.5));
        AttributeSet test = sc.addAttributes(as1, as2);
        assertEquals(9, test.getAttributeCount());
        assertTrue(test instanceof SmallAttributeSet);
    }

    @Override
    protected void setUp() throws Exception {
        sc = StyleContextTest.sc = new StyleContext();
    }
}