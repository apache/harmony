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

import java.util.Enumeration;

import javax.swing.BasicSwingTestCase;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS.Attribute;

/**
 * Tests <code>SmallAttributeSet</code> returned by
 * <code>StyleSheet.addAttribute</code> method. The returned attribute set
 * contains <code>CSS.Attribute</code>. It converts the value to
 * <code>StyleConstants</code> when a <code>StyleConstants</code> attribute
 * is requested.
 * <p>
 * The attribute used for testing is <code>StyleConstants.Bold</code>.
 * It corresponds to <code>Attribute.FONT_WEIGHT</code>.
 *
 * @see StyleSheet
 * @see StyleSheet#addAttribute(Object, Object)
 * @see StyleSheet#createSmallAttributeSet(AttributeSet)
 * @see CSS.Attribute
 * @see javax.swing.text.StyleContext.SmallAttributeSet
 */
public class StyleSheet_Small_Test extends BasicSwingTestCase {
    private static final Object scAttribute  = StyleConstants.Bold;
    private static final Object scValue      = Boolean.TRUE;
    private static final Object cssAttribute = Attribute.FONT_WEIGHT;

    private StyleSheet ss;
    private AttributeSet empty;
    private AttributeSet attr;

    protected void setUp() throws Exception {
        super.setUp();
        ss = new StyleSheet();
        empty = ss.getEmptySet();

        attr = ss.addAttribute(empty, scAttribute, scValue);
    }

    public void testGetAttribute() throws Exception {
        Object value = attr.getAttribute(cssAttribute);
        assertNotSame(Boolean.class, value.getClass());
        assertNotSame(String.class, value.getClass());
        assertEquals("bold", value.toString());

        value = attr.getAttribute(scAttribute);
        assertSame(Boolean.class, value.getClass());
        assertTrue(((Boolean)value).booleanValue());
        assertSame(scValue, value);
    }

    public void testIsDefined() throws Exception {
        assertTrue(attr.isDefined(scAttribute));
        assertTrue(attr.isDefined(cssAttribute));
    }

    public void testGetNames() throws Exception {
        final Enumeration keys = attr.getAttributeNames();
        final Object key = keys.nextElement();
        assertSame(cssAttribute, key);
        assertFalse(keys.hasMoreElements());
    }

    public void testGetAttributeCount() throws Exception {
        assertEquals(1, attr.getAttributeCount());
    }

    public void testIsEqualSame() throws Exception {
        assertTrue(attr.isEqual(attr));

        final MutableAttributeSet simple = new SimpleAttributeSet(attr);
        assertTrue(attr.isEqual(simple));
        assertTrue(simple.isEqual(attr));
    }

    public void testIsEqualMutable() throws Exception {
        final MutableAttributeSet simple = new SimpleAttributeSet();
        simple.addAttribute(scAttribute, scValue);

        assertTrue(attr.isEqual(simple));
        if (isHarmony()) {
            assertTrue(simple.isEqual(attr));
        } else {
            assertFalse(simple.isEqual(attr));
        }
    }

    public void testCopyAttributes() throws Exception {
        assertSame(attr, attr.copyAttributes());
    }

    public void testContainsAttribute() throws Exception {
        assertTrue(attr.containsAttribute(cssAttribute,
                                          attr.getAttribute(cssAttribute)));
        assertTrue(attr.containsAttribute(scAttribute, scValue));
    }

    public void testContainsAttributeAnother() throws Exception {
        final AttributeSet another = ss.addAttribute(empty,
                                                     scAttribute, scValue);

        assertEquals(attr.getAttribute(cssAttribute).toString(),
                     another.getAttribute(cssAttribute).toString());
        if (isHarmony()) {
            assertTrue(attr.containsAttribute(cssAttribute,
                                              another.getAttribute(cssAttribute)));
        } else {
            assertFalse(attr.containsAttribute(cssAttribute,
                                               another.getAttribute(cssAttribute)));
        }
    }

    public void testContainsAttributesSame() throws Exception {
        assertTrue(attr.containsAttributes(attr));

        final MutableAttributeSet simple = new SimpleAttributeSet(attr);

        assertTrue(attr.containsAttributes(simple));
        assertTrue(simple.containsAttributes(attr));
    }

    public void testContainsAttributesMutable() throws Exception {
        final MutableAttributeSet simple = new SimpleAttributeSet();
        simple.addAttribute(scAttribute, scValue);

        assertTrue(attr.containsAttributes(simple));
        assertFalse(simple.containsAttributes(attr));
    }

    public void testContainsAttributesSmall() throws Exception {
        final AttributeSet another = ss.addAttribute(empty,
                                                     scAttribute, scValue);

        if (isHarmony()) {
            assertTrue(attr.containsAttributes(another));
            assertTrue(another.containsAttributes(attr));
        } else {
            assertFalse(attr.containsAttributes(another));
            assertFalse(another.containsAttributes(attr));
        }
    }
}
