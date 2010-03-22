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
package javax.swing.text.html;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleContext;
import javax.swing.text.AbstractDocument.AttributeContext;

public class OptionTest extends BasicSwingTestCase {
    private AttributeSet attrs;
    private Option item;

    protected void setUp() throws Exception {
        super.setUp();
        item = new Option(attrs = new SimpleAttributeSet());
    }

    /**
     * Tests constructor.
     * Empty <code>SimpleAttributeSet</code> is passed
     * as parameter (see initialization in <code>setUp()</code>).
     */
    public void testOption() {
        assertTrue(item.getAttributes() instanceof MutableAttributeSet);
        assertNotSame(attrs, item.getAttributes());
        assertEquals(attrs, item.getAttributes());
        assertNull(item.getLabel());
        assertNull(item.getValue());
        assertFalse(item.isSelected());
        assertNull(item.toString());
    }

    /**
     * Tests constructor.
     * Non-mutable (<code>StyleContext.SmallAttributeSet</code>) is passed
     * as parameter.
     */
    public void testOptionSmallAttrSet() {
        AttributeContext context = new StyleContext();
        attrs = context.addAttribute(context.getEmptySet(), "key", "value");

        item = new Option(attrs);
        final AttributeSet itAttrs = item.getAttributes();
        assertFalse(itAttrs instanceof MutableAttributeSet);
        assertTrue(itAttrs instanceof StyleContext.SmallAttributeSet);
        assertSame(attrs, itAttrs);
        assertEquals(attrs, itAttrs);
        assertNull(item.getLabel());
        assertNull(item.getValue());
        assertFalse(item.isSelected());
        assertNull(item.toString());

        assertEquals("value", itAttrs.getAttribute("key"));
    }

    /**
     * Tests constructor.
     * Attribute set passed contains <em>relevant</em> attributes:
     * <ul>
     *     <li><code>HTML.Attribute.SELECTED</code> with no meaningful
     *         value,</li>
     *     <li><code>HTML.Attribute.VALUE</code>.</li>
     * </ul>
     */
    public void testOptionSelected() {
        MutableAttributeSet attr = (MutableAttributeSet)attrs;
        attr.addAttribute(HTML.Attribute.SELECTED,
                          "no meaning" /*HTML.Attribute.SELECTED.toString()*/);
        final String value = "iVal";
        attr.addAttribute(HTML.Attribute.VALUE, value);

        item = new Option(attrs);
        final AttributeSet itAttrs = item.getAttributes();
        assertEquals(attrs, itAttrs);
        assertNull(item.getLabel());
        assertSame(value, item.getValue());
        assertTrue(item.isSelected());
        assertNull(item.toString());
    }

    /**
     * Tests constructor.
     * Attribute set passed contains <em>relevant</em> attributes:
     * <ul>
     *     <li><code>HTML.Attribute.SELECTED</code> with no meaningful
     *         value,</li>
     *     <li><code>HTML.Attribute.VALUE</code>.</li>
     * </ul>
     */
    public void testOptionNull() {
        testExceptionalCase(new NullPointerCase() {
            public void exceptionalAction() throws Exception {
                new Option(null);
            }
        });
    }

    /**
     * Tests <code>getAttributes</code>.
     * See also tests for constructor: <code>testOption()</code> and
     * <code>testOptionSmallAttrSet()</code>.
     */
    public void testGetAttributes() {
        assertEquals(attrs, item.getAttributes());
    }

    /**
     * Tests three methods:
     * <ul>
     *     <li><code>getLabel</code>,</li>
     *     <li><code>setLabel</code>,</li>
     *     <li><code>toString</code>.</li>
     * </ul>
     */
    public void testGetLabel() {
        assertNull(item.getLabel());
        assertNull(item.toString());

        final String label = "itemLabel";
        item.setLabel(label);

        assertSame(label, item.getLabel());
        assertSame(label, item.toString());

        item.setLabel(null);
        assertNull(item.getLabel());
        assertNull(item.toString());
    }

    /**
     * Tests <code>getValue</code>.
     * See also test for constructor <code>testOptionSelected()</code>.
     */
    public void testGetValue() {
        assertNull(item.getValue());

        final String label = "label";
        item.setLabel(label);
        assertEquals(label, item.getValue());

        final String value = "value";
        ((MutableAttributeSet)attrs).addAttribute(HTML.Attribute.VALUE,
                                                  value);
        item = new Option(attrs);
        assertEquals(value, item.getValue());

        ((MutableAttributeSet)attrs).addAttribute(HTML.Attribute.VALUE,
                                                  new Integer(1012));
        item = new Option(attrs);
        testExceptionalCase(new ClassCastCase() {
            public void exceptionalAction() throws Exception {
                item.getValue();
            }
        });
    }

    /**
     * Tests two methods:
     * <ul>
     *     <li><code>isSelected</code>,</li>
     *     <li><code>setSelection</code>.</li>
     * </ul>
     */
    public void testIsSelected() {
        assertFalse(item.isSelected());

        item.setSelection(true);
        assertTrue(item.isSelected());

        item.setSelection(false);
        assertFalse(item.isSelected());
        // See also testOptionSelected()
    }

    /*
    public void testSetLabel()     { }    ==> testGetLabel()

    public void testSetSelection() { }    ==> testIsSelected()

    public void testToString()     { }    ==> testGetLabel()
    */
}
