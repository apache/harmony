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

import javax.swing.text.AbstractDocument.AbstractElement;
import javax.swing.tree.TreeNode;
import junit.framework.TestCase;

/**
 * Tests methods of AbstractElement which implement TreeNode interface.
 * This class contains only methods that are not abstract in
 * AbstractElement (they must be overridden and therefore tested in a
 * subclass) and whose behavior may slightly change from a subclass to
 * another.
 *
 */
public class AbstractDocument_AbstractElement_TreeNodeTest extends TestCase {
    protected AbstractDocument doc;

    protected AbstractElement aElement;

    protected AbstractElement parented;

    protected AbstractElement parent;

    @Override
    protected void setUp() throws Exception {
        // Initialize static variables of enclosing class
        AbstractDocument_AbstractElementTest.init();
        // Copy their values to instance variables
        doc = AbstractDocument_AbstractElementTest.aDocument;
        aElement = AbstractDocument_AbstractElementTest.aElement;
        parented = AbstractDocument_AbstractElementTest.parented;
        parent = aElement;
    }

    public void testGetChildCount() {
        assertEquals(aElement.getElementCount(), aElement.getChildCount());
        assertEquals(parented.getElementCount(), parented.getChildCount());
    }

    public void testGetChildAt() {
        assertSame(aElement.getElement(0), aElement.getChildAt(0));
        assertSame(aElement.getElement(1), aElement.getChildAt(1));
        assertSame(aElement.getElement(2), aElement.getChildAt(2));
        assertSame(aElement.getElement(5), aElement.getChildAt(5));
        assertSame(aElement.getElement(10), aElement.getChildAt(10));
    }

    public void testGetIndex() {
        assertEquals(-1, aElement.getIndex(null));
        TreeNode node = aElement.getChildAt(0);
        // Check sanity of the value
        if (aElement.getAllowsChildren() && aElement.getChildCount() > 0) {
            assertNotNull(node);
        }
        // If node == null, the first condition will be true, if not
        // the second one will come into play. Any way we get what we
        // want.
        assertTrue(node == null || (0 == aElement.getIndex(node)));
        assertTrue((node = aElement.getChildAt(1)) == null || (1 == aElement.getIndex(node)));
    }

    public void testGetParent() {
        assertNull(aElement.getParent());
        assertNotNull(parented.getParent());
        assertSame(parent, parented.getParent());
        assertSame(parented.getParentElement(), parented.getParent());
    }
}