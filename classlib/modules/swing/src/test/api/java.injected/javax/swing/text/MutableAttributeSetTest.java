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
import java.util.NoSuchElementException;

public class MutableAttributeSetTest extends AttributeSetTest {
    public MutableAttributeSetTest() {
        super();
    }

    public MutableAttributeSetTest(final String name) {
        super(name);
    }

    /**
     * The interface MutableAttributeSet for test cases.
     */
    protected MutableAttributeSet mas;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(MutableAttributeSetTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mas = (MutableAttributeSet) as;
    }

    public void testRemoveAttribute() {
        int count = mas.getAttributeCount();
        mas.removeAttribute(keys[1]);
        assertFalse(mas.isDefined(keys[1]));
        mas.removeAttribute(values[1]);
        assertEquals(keys.length - 1, mas.getAttributeCount());
        assertEquals(count - 1, mas.getAttributeCount());
    }

    public void testAddAttributes() {
        MutableAttributeSet forAdd = new SimpleAttributeSet();
        String key1 = "keyforAdd1", value1 = "valueforAdd1";
        String key2 = "keyforAdd2", value2 = "valueforAdd2";
        forAdd.addAttribute(key1, value1);
        forAdd.addAttribute(key2, value2);
        int count = mas.getAttributeCount();
        mas.addAttributes(forAdd);
        assertEquals(count + 2, mas.getAttributeCount());
        assertEquals(value1, mas.getAttribute(key1));
        assertEquals(value2, mas.getAttribute(key2));
    }

    /*
     * Class under test for void removeAttributes(javax.swing.text.AttributeSet)
     */
    public void testRemoveAttributesAttributeSetSame() {
        MutableAttributeSet forRemove = new SimpleAttributeSet();
        // The key/value pair are the same as those in mas
        forRemove.addAttribute(keys[1], values[1]);
        forRemove.addAttribute(keys[2], values[2]);
        int count = mas.getAttributeCount();
        mas.removeAttributes(forRemove);
        // Attributes with keys { keys[1], keys[2] } should be removed
        // from the set
        assertFalse(mas.isDefined(keys[1]));
        assertFalse(mas.isDefined(keys[2]));
        assertEquals(count - 2, mas.getAttributeCount());
    }

    /*
     * Class under test for void removeAttributes(javax.swing.text.AttributeSet)
     */
    public void testRemoveAttributesAttributeSetDiff() {
        MutableAttributeSet forRemove = new SimpleAttributeSet();
        // The keys are the same but, the values are different
        forRemove.addAttribute(keys[1], "one");
        forRemove.addAttribute(keys[2], "two");
        int count = mas.getAttributeCount();
        mas.removeAttributes(forRemove);
        // Attributes with keys { keys[1], keys[2] } should NOT be removed
        // from the set as their values were different
        assertTrue(mas.isDefined(keys[1]));
        assertTrue(mas.isDefined(keys[2]));
        assertEquals(count, mas.getAttributeCount());
    }

    @SuppressWarnings("unchecked")
    public void testRemoveAttributesEnumeration() {
        int count = mas.getAttributeCount();
        assertTrue(mas.isDefined(keys[0]));
        assertTrue(mas.isDefined(keys[1]));
        assertFalse(mas.isDefined(keyInResolver));
        mas.removeAttributes(new Enumeration() {
            private int count = 0;

            public boolean hasMoreElements() {
                return count < 3;
            }

            public Object nextElement() {
                if (count < 2) {
                    return keys[count++];
                } else if (count < 3) {
                    count = 3;
                    return keyInResolver;
                } else {
                    throw new NoSuchElementException("No more elements");
                }
            }
        });
        assertEquals(count - 2, mas.getAttributeCount());
        assertFalse(mas.isDefined(keys[0]));
        assertFalse(mas.isDefined(keys[1]));
        assertFalse(mas.isDefined(keyInResolver));
    }

    public void testSetResolveParent() {
        AttributeSet parent = mas.getResolveParent();
        AttributeSet newParent = new SimpleAttributeSet();
        mas.setResolveParent(newParent);
        assertNotSame(parent, mas.getResolveParent());
        assertSame(newParent, mas.getResolveParent());
    }

    public void testAddAttribute() {
        int count = mas.getAttributeCount();
        String key = "key", value = "value";
        mas.addAttribute(key, value);
        assertTrue(mas.isDefined(key));
        assertEquals(value, mas.getAttribute(key));
        assertEquals(count + 1, mas.getAttributeCount());
    }
}
