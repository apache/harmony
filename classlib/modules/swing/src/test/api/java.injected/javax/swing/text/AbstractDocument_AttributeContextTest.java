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

import junit.framework.TestCase;

public class AbstractDocument_AttributeContextTest extends TestCase {
    private AbstractDocument.AttributeContext context;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = StyleContext.getDefaultStyleContext();
    }

    public void testGetEmptySet() {
        AttributeSet as = context.getEmptySet();
        assertFalse(as.containsAttribute("key", "value"));
        MutableAttributeSet sas = new SimpleAttributeSet();
        sas.addAttribute("key", "value");
        assertFalse(as.containsAttributes(sas));
        assertEquals(as, SimpleAttributeSet.EMPTY);
        assertNull(as.getAttribute("key"));
        assertFalse(as.isDefined("key"));
        assertTrue(as.isEqual(SimpleAttributeSet.EMPTY));
        assertNotNull(as.toString());
        assertEquals(SimpleAttributeSet.EMPTY, as.copyAttributes());
        assertEquals(0, as.getAttributeCount());
        assertFalse(as.getAttributeNames().hasMoreElements());
        assertNull(as.getResolveParent());
    }

    public void testReclaim() {
        AttributeSet as = context.getEmptySet();
        context.reclaim(as);
    }

    public void testRemoveAttribute() {
        AttributeSet as = context.getEmptySet();
        String key = "key", value = "value";
        context.addAttribute(as, key, value);
        AttributeSet attr = context.removeAttribute(as, key);
        assertEquals(0, attr.getAttributeCount());
        assertEquals(context.getEmptySet(), attr);
    }

    /*
     * AttributeSet removeAttributes(AttributeSet, Enumeration)
     */
    public void testRemoveAttributesAttributeSetEnumeration() {
        AttributeSet as = getFilledAttributeSet();
        AttributeSet result = context.removeAttributes(as, as.getAttributeNames());
        assertEquals(context.getEmptySet(), result);
    }

    /**
     * Returns non-empty attribute set.
     * @return
     */
    private AttributeSet getFilledAttributeSet() {
        AttributeSet as = context.getEmptySet();
        context.addAttribute(as, "key", "value");
        context.addAttribute(as, "key1", "value1");
        context.addAttribute(as, "key2", "value2");
        return as;
    }

    public void testAddAttributes() {
        AttributeSet as = getFilledAttributeSet();
        AttributeSet test = context.getEmptySet();
        AttributeSet result = context.addAttributes(test, as);
        assertEquals(test, result);
    }

    /*
     * AttributeSet removeAttributes(AttributeSet, AttributeSet)
     */
    public void testRemoveAttributesAttributeSetAttributeSet() {
        AttributeSet as = getFilledAttributeSet();
        AttributeSet result = context.removeAttributes(as, as);
        assertEquals(context.getEmptySet(), result);
    }

    public void testAddAttribute() {
        AttributeSet as = context.getEmptySet();
        String key = "key", value = "value";
        AttributeSet result = context.addAttribute(as, key, value);
        assertTrue(result.containsAttribute(key, value));
        assertEquals(1, result.getAttributeCount());
    }
}
