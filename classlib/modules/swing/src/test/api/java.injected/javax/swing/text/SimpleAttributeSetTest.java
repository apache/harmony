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

public class SimpleAttributeSetTest extends MutableAttributeSetTest {
    public SimpleAttributeSetTest() {
        super();
    }

    public SimpleAttributeSetTest(final String name) {
        super(name);
    }

    /**
     * The instance of SimpleAttributeSet.
     */
    protected SimpleAttributeSet sas;

    /*
     * @see MutableAttributeSetTest#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sas = (SimpleAttributeSet) as;
    }

    public void testHashCode() {
        SimpleAttributeSet newSet = (SimpleAttributeSet) sas.clone();
        assertTrue("Hash codes must be equal for equal objects", newSet.hashCode() == sas
                .hashCode());
        newSet.setResolveParent(SimpleAttributeSet.EMPTY);
        assertFalse("Hash codes should not be equal for not equal objects",
                newSet.hashCode() == sas.hashCode());
        sas.setResolveParent(new SimpleAttributeSet());
        assertTrue("Hash codes must be equal for equal objects", newSet.hashCode() == sas
                .hashCode());
        SimpleAttributeSet parent = new SimpleAttributeSet();
        parent.addAttribute("key", "value");
        newSet.setResolveParent(parent);
        sas.setResolveParent(parent);
        assertTrue("Hash codes must be equal for equal objects", newSet.hashCode() == sas
                .hashCode());
        sas.removeAttribute(keys[1]);
        assertFalse("Hash codes should not be equal for not equal objects",
                newSet.hashCode() == sas.hashCode());
    }

    public void testClone() {
        SimpleAttributeSet parent = new SimpleAttributeSet();
        parent.addAttribute("key", "value");
        sas.setResolveParent(parent);
        Object clone = sas.clone();
        assertSame(sas.getClass(), clone.getClass());
        assertNotSame(sas, clone);
        assertEquals(sas, clone);
        assertEquals(sas.getResolveParent(), ((SimpleAttributeSet) clone).getResolveParent());
        sas.removeAttribute(keys[1]);
        assertFalse(sas.equals(clone));
    }

    public void testEquals() {
        SimpleAttributeSet newSet = (SimpleAttributeSet) sas.clone();
        assertEquals(sas, newSet);
        newSet.setResolveParent(SimpleAttributeSet.EMPTY);
        assertFalse(sas.equals(newSet));
        sas.setResolveParent(new SimpleAttributeSet());
        assertEquals(sas, newSet);
        SimpleAttributeSet parent = new SimpleAttributeSet();
        parent.addAttribute("key", "value");
        newSet.setResolveParent(new SimpleAttributeSet(parent));
        sas.setResolveParent(new SimpleAttributeSet(parent));
        assertEquals(sas, newSet);
        sas.removeAttribute(keys[1]);
        assertFalse(sas.equals(newSet));
        StyleContext.SmallAttributeSet eq = StyleContext.getDefaultStyleContext().new SmallAttributeSet(
                new Object[] { keys[0], values[0], keys[2], values[2],
                        AttributeSet.ResolveAttribute, parent });
        assertEquals(sas, eq);
    }

    public void testToString() {
        assertFalse(sas.toString().equals(""));
    }

    /*
     * Class under test for void SimpleAttributeSet()
     */
    public void testSimpleAttributeSet() {
        sas = new SimpleAttributeSet();
        assertEquals(SimpleAttributeSet.EMPTY, sas);
    }

    public void testIsEmpty() {
        assertFalse(sas.isEmpty());
        SimpleAttributeSet clone = (SimpleAttributeSet) sas.clone();
        sas.removeAttributes(clone);
        assertTrue(sas.isEmpty());
        sas.setResolveParent(clone);
        assertFalse(sas.isEmpty());
    }

    /*
     * SimpleAttributeSet(AttributeSet)
     */
    public void testSimpleAttributeSetAttributeSet() {
        SimpleAttributeSet parent = new SimpleAttributeSet();
        parent.addAttribute("key", "value");
        sas.setResolveParent(parent);
        SimpleAttributeSet initSet = (SimpleAttributeSet) sas.clone();
        sas = new SimpleAttributeSet(initSet);
        assertEquals(initSet, sas);
        assertEquals(initSet.getResolveParent(), sas.getResolveParent());
    }

    public void testSerializable() throws Exception {
        sas.setResolveParent(SimpleAttributeSet.EMPTY);
        SimpleAttributeSet readSet = (SimpleAttributeSet) BasicSwingTestCase
                .serializeObject(sas);
        assertEquals(sas, readSet);
    }
}
