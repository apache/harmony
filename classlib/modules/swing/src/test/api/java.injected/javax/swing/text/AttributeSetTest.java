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
import junit.framework.TestCase;

public class AttributeSetTest extends TestCase {
    /**
     * The interface AttributeSet for test cases.
     */
    protected AttributeSet as;

    /**
     * The flag indicates that setUp method should include resolveParent
     * while initializing test object.
     */
    protected AttributeSet asWithResolver;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(AttributeSetTest.class);
    }

    /**
     * This is array containing keys for test attribute set.
     */
    protected static final String[] keys = new String[] { "key1", "key2", "key3" };

    /**
     * This is array containing values for test attribute set.
     */
    protected static final String[] values = new String[] { "value1", "value2", "value3" };

    /**
     * The attribute key to place in resolver set.
     */
    protected static final String keyInResolver = "keyInResolver";

    /**
     * The attribute value to place in resolver set.
     */
    protected static final String valueInResolver = "valueInResolver";

    /**
     * The attribute set used as a resolve parent.
     */
    protected static AttributeSet resolverSet = new SimpleAttributeSet();
    static {
        // Add the attribute into the set
        ((SimpleAttributeSet) resolverSet).addAttribute(keyInResolver, valueInResolver);
    }

    /**
     * AttributeSet must contain keys&values.
     */
    @Override
    protected void setUp() throws Exception {
        MutableAttributeSet mas = new SimpleAttributeSet();
        ;
        for (int i = 0; i < keys.length; i++) {
            mas.addAttribute(keys[i], values[i]);
        }
        as = mas;
        asWithResolver = mas.copyAttributes();
        ((MutableAttributeSet) asWithResolver).setResolveParent(resolverSet);
    }

    public AttributeSetTest() {
        super();
    }

    public AttributeSetTest(final String name) {
        super(name);
    }

    public void testContainsAttribute() {
        assertTrue(as.containsAttribute(keys[1], values[1]));
        assertFalse(as.containsAttribute(values[1], keys[1]));
        assertFalse(as.containsAttribute(keyInResolver, valueInResolver));
        assertTrue(asWithResolver.containsAttribute(keyInResolver, valueInResolver));
    }

    public void testContainsAttributes() {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        attrs.addAttribute(keys[0], values[0]);
        attrs.addAttribute(keys[2], values[2]);
        assertTrue(as.containsAttributes(attrs));
        attrs.addAttribute(values[1], keys[1]);
        assertFalse(as.containsAttributes(attrs));
        assertFalse(as.containsAttributes(resolverSet));
        assertTrue(asWithResolver.containsAttributes(resolverSet));
    }

    public void testCopyAttributes() {
        AttributeSet copy = as.copyAttributes();
        //copyAttributes returns an attribute set
        //that is guaranteed not to change over time
        if (as instanceof MutableAttributeSet) {
            assertNotSame(copy, as);
        }
        assertEquals(as.getAttributeCount(), copy.getAttributeCount());
        assertTrue(as.isEqual(copy));
    }

    public void testGetAttribute() {
        assertNull(as.getAttribute("key"));
        assertEquals(values[1], as.getAttribute(keys[1]));
        MutableAttributeSet parent = new SimpleAttributeSet();
        String key = "key", value = "value";
        parent.addAttribute(key, value);
        assertNull(as.getAttribute(value));
        assertNull(valueInResolver, as.getAttribute(keyInResolver));
        assertEquals(valueInResolver, asWithResolver.getAttribute(keyInResolver));
    }

    public void testGetAttributeCount() {
        assertEquals(keys.length, as.getAttributeCount());
        assertEquals(keys.length + 1, asWithResolver.getAttributeCount());
    }

    public void testGetAttributeNames() {
        int count = 0;
        for (Enumeration<?> e = as.getAttributeNames(); e.hasMoreElements();) {
            count++;
            assertTrue(as.isDefined(e.nextElement()));
        }
        assertEquals(as.getAttributeCount(), count);
        count = 0;
        for (Enumeration<?> e = asWithResolver.getAttributeNames(); e.hasMoreElements();) {
            count++;
            assertTrue(asWithResolver.isDefined(e.nextElement()));
        }
        assertEquals(asWithResolver.getAttributeCount(), count);
    }

    public void testGetResolveParent() {
        assertNull(as.getResolveParent());
        assertNotNull(asWithResolver.getResolveParent());
    }

    public void testIsDefined() {
        assertTrue(as.isDefined(keys[1]));
        assertFalse(as.isDefined(values[1]));
        assertFalse(asWithResolver.isDefined(keyInResolver));
    }

    public void testIsEqual() {
        AttributeSet copy = as.copyAttributes();
        assertTrue(as.isEqual(copy));
        assertFalse(as.isEqual(SimpleAttributeSet.EMPTY));
        if (copy instanceof MutableAttributeSet) {
            MutableAttributeSet parent = new SimpleAttributeSet();
            ((MutableAttributeSet) copy).setResolveParent(parent);
            assertFalse(as.isEqual(copy));
            String key = "key", value = "value";
            parent.addAttribute(key, value);
            ((MutableAttributeSet) copy).setResolveParent(parent);
            assertFalse(as.isEqual(copy));
            if (as instanceof MutableAttributeSet) {
                ((MutableAttributeSet) as).setResolveParent(parent);
                assertTrue(as.isEqual(copy));
                MutableAttributeSet parent2 = new SimpleAttributeSet();
                parent2.addAttribute(key, value);
                ((MutableAttributeSet) as).setResolveParent(parent2);
                assertTrue(as.isEqual(copy));
            }
        }
    }

    public void testResolveAttribute() {
        assertSame(AttributeSet.ResolveAttribute, StyleConstants.ResolveAttribute);
    }

    public void testNameAttribute() {
        assertSame(AttributeSet.NameAttribute, StyleConstants.NameAttribute);
    }
}
