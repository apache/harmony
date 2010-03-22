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
import javax.swing.BasicSwingTestCase;
import junit.framework.TestCase;

public class EmptyAttributeSetTest extends TestCase {
    /**
     * Shared instance of EmptyAttributeSet class.
     */
    private static final AttributeSet empty = SimpleAttributeSet.EMPTY;

    /**
     * Shared instance of SimpleAttributeSet class.
     */
    private static final SimpleAttributeSet simple = new SimpleAttributeSet();

    /**
     * Shared instance of StyleContext.SmallAttributeSet class.
     */
    private static final StyleContext.SmallAttributeSet small = new StyleContext().new SmallAttributeSet(
            empty);

    public EmptyAttributeSetTest(final String name) {
        super(name);
    }

    public void testEmptySet() {
        // Assert that both classes return the same attribute set
        assertSame(new StyleContext().getEmptySet(), SimpleAttributeSet.EMPTY);
    }

    public void testHashCode() {
        assertEquals(0, empty.hashCode());
    }

    public void testGetAttribute() {
        assertNull(empty.getAttribute("key"));
    }

    public void testGetAttributeCount() {
        assertEquals(0, empty.getAttributeCount());
    }

    public void testContainsAttribute() {
        assertFalse(empty.containsAttribute("key", "value"));
    }

    public void testEqualsObject() {
        assertFalse(empty.equals(new Object()));
        assertTrue(empty.equals(empty));
        assertTrue(empty.equals(simple));
        assertTrue(empty.equals(small));
    }

    public void testIsEqual() {
        assertTrue(empty.isEqual(empty));
        assertTrue(empty.isEqual(simple));
        assertTrue(empty.isEqual(small));
    }

    public void testGetAttributeNames() {
        Enumeration<?> names = empty.getAttributeNames();
        assertNotNull(names);
        assertFalse(names.hasMoreElements());
        try {
            names.nextElement();
            fail("NoSuchElementException should be thrown");
        } catch (NoSuchElementException e) {
        }
    }

    public void testGetResolveParent() {
        assertNull(empty.getResolveParent());
        assertNull(empty.getAttribute(AttributeSet.ResolveAttribute));
    }

    public void testIsDefined() {
        assertFalse(empty.isDefined("key"));
    }

    public void testCopyAttributes() {
        assertSame(empty, empty.copyAttributes());
    }

    public void testContainsAttributes() {
        assertTrue(empty.containsAttributes(empty));
        assertTrue(empty.containsAttributes(small));
        assertTrue(empty.containsAttributes(simple));
    }

    public void testToString() {
        final String expected;
        expected = BasicSwingTestCase.isHarmony() ? "javax.swing.text.EmptyAttributeSet@0"
                : "javax.swing.text.SimpleAttributeSet$EmptyAttributeSet@0";
        assertEquals(expected, empty.toString());
    }
}
