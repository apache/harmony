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

public class StyleContext_SmallAttributeSetTest extends AttributeSetTest {
    private StyleContext sc;

    private SmallAttributeSet attrSet;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sc = new StyleContext();
        attrSet = sc.new SmallAttributeSet(as);
        as = attrSet;
        asWithResolver = sc.new SmallAttributeSet(asWithResolver);
    }

    public void testHashCode() {
        SmallAttributeSet aSet = sc.new SmallAttributeSet(as);
        assertNotSame(attrSet, aSet);
        assertEquals("Hash codes must be equal for equal objects", attrSet.hashCode(), aSet
                .hashCode());
        aSet = sc.new SmallAttributeSet(new Object[0]);
        assertEquals(0, aSet.hashCode());
        // Hash codes for equal objects must be equal
        // even if they are implemented by different classes
        SimpleAttributeSet simpleAS = new SimpleAttributeSet(as);
        assertEquals(simpleAS, attrSet);
        if (BasicSwingTestCase.isHarmony()) {
            assertEquals(attrSet.hashCode(), simpleAS.hashCode());
        }
    }

    public void testClone() {
        Object clone = attrSet.clone();
        assertSame(attrSet, clone);
    }

    public void testEquals() {
        AttributeSet newSet = sc.new SmallAttributeSet(new Object[] { keys[0], values[0],
                keys[1], values[1], keys[2], values[2] });
        assertEquals(attrSet, newSet);
        newSet = sc.new SmallAttributeSet(
                new Object[] { keys[0], values[0], keys[1], values[1] });
        assertFalse(attrSet.equals(newSet));
        SimpleAttributeSet sas = new SimpleAttributeSet();
        for (int i = 0; i < keys.length; i++) {
            sas.addAttribute(keys[i], values[i]);
        }
        assertEquals(attrSet, sas);
        assertFalse(attrSet.equals(null));
    }

    public void testToString() {
        assertAttributesSame("{key3=value3,key2=value2,key1=value1,}", attrSet.toString());
        AttributeSet parent = sc.new SmallAttributeSet(new Object[] { "key", "value" });
        attrSet = sc.new SmallAttributeSet(new Object[] { keys[0], values[0], keys[1],
                values[1], AttributeSet.ResolveAttribute, parent });
        assertAttributesSame("{key1=value1,key2=value2,resolver=AttributeSet,}", attrSet
                .toString());
    }

    /*
     * SmallAttributeSet(Object[])
     */
    public void testSmallAttributeSetObjectArray() {
        Object[] array = new Object[] { keys[0], values[0], keys[1], values[1], keys[2],
                values[2] };
        attrSet = sc.new SmallAttributeSet(array);
        assertEquals(keys.length, attrSet.getAttributeCount());
        for (int i = 0; i < keys.length; i++) {
            assertTrue(attrSet.containsAttribute(keys[i], values[i]));
        }
        // AttributeSet must not depend on the passed array
        array[0] = "justKey";
        if (BasicSwingTestCase.isHarmony()) {
            assertFalse(attrSet.isDefined(array[0]));
        }
        array = new Object[] { "key", "value" };
        assertEquals(3, attrSet.getAttributeCount());
        // Empty array
        array = new Object[0];
        attrSet = sc.new SmallAttributeSet(array);
        emptyTest(attrSet);
    }

    public void emptyTest(final AttributeSet emptySet) {
        assertNull(emptySet.getResolveParent());
        assertFalse(emptySet.getAttributeNames().hasMoreElements());
        assertNull(emptySet.getAttribute(keys[1]));
        assertNull(emptySet.getAttribute(AttributeSet.ResolveAttribute));
        assertEquals(emptySet, emptySet.copyAttributes());
        emptySet.toString();
        assertFalse(emptySet.isEqual(as));
        assertFalse(emptySet.isDefined(keys[1]));
        assertFalse(emptySet.equals(as));
        assertFalse(emptySet.containsAttributes(as));
        assertFalse(emptySet.containsAttribute("key", "value"));
        assertEquals(0, emptySet.hashCode());
    }

    /*
     * SmallAttributeSet(AttributeSet)
     */
    public void testSmallAttributeSetStyleContextAttributeSet() {
        assertEquals(keys.length, attrSet.getAttributeCount());
        for (int i = 0; i < keys.length; i++) {
            assertTrue(attrSet.containsAttribute(keys[i], values[i]));
        }
        // Empty attribute set
        attrSet = sc.new SmallAttributeSet(SimpleAttributeSet.EMPTY);
        emptyTest(attrSet);
    }

    @Override
    public void testGetResolveParent() {
        assertNull(attrSet.getResolveParent());
        AttributeSet parent = sc.new SmallAttributeSet(new Object[] { "key", "value" });
        Object[] array = new Object[] { keys[0], values[0], keys[1], values[1],
                AttributeSet.ResolveAttribute, parent };
        attrSet = sc.new SmallAttributeSet(array);
        assertEquals(parent, attrSet.getResolveParent());
    }

    private static void assertAttributesSame(final String expected, final String actual) {
        String[] splitExpected = StyleContextTest.trimEnds(expected).split(",");
        String[] splitActual = StyleContextTest.trimEnds(actual).split(",");
        assertTrue(StyleContextTest.assertAttributes(splitExpected, splitActual));
    }
}
