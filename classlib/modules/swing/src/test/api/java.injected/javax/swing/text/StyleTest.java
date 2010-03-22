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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class StyleTest extends MutableAttributeSetTest implements ChangeListener {
    protected Style style;

    /*
     * @see MutableAttributeSetTest#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        style = new StyleContext().new NamedStyle();
        for (int i = 0; i < keys.length; i++) {
            style.addAttribute(keys[i], values[i]);
        }
        mas = style;
        as = style;
        Style styleWithResolver = new StyleContext().new NamedStyle();
        for (int i = 0; i < keys.length; i++) {
            styleWithResolver.addAttribute(keys[i], values[i]);
        }
        Style resolverStyle = new StyleContext().new NamedStyle();
        resolverStyle.addAttribute(keyInResolver, valueInResolver);
        styleWithResolver.setResolveParent(resolverStyle);
        asWithResolver = styleWithResolver;
        resolverSet = resolverStyle;
    }

    // Tests both methods: add and remove.
    public void testChangeListener() {
        style.addChangeListener(this);
        bStateChanged = false;
        style.addAttribute("key", "value");
        assertTrue(bStateChanged);
        style.removeChangeListener(this);
        bStateChanged = false;
        style.removeAttribute("key");
        assertFalse(bStateChanged);
    }

    // addAttribute that is already in the set
    public void testChangeListenerAddAttrSameValue() {
        style.addChangeListener(this);
        bStateChanged = false;
        AttributeSet copy = style.copyAttributes();
        style.addAttribute(keys[0], values[0]);
        // Actually no change happened
        assertTrue(style.isEqual(copy));
        // The listener is to be called
        assertTrue(bStateChanged);
    }

    // addAttribute whose key is in the set but with different value
    public void testChangeListenerAddAttrDiffValue() {
        style.addChangeListener(this);
        bStateChanged = false;
        style.addAttribute(keys[0], values[1]);
        // The listener is to be called
        assertTrue(bStateChanged);
    }

    // removeAttribute which is defined
    public void testChangeListenerRemoveAttrDef() {
        style.addChangeListener(this);
        bStateChanged = false;
        style.removeAttribute(keys[0]);
        // The listener is to be called
        assertTrue(bStateChanged);
    }

    // removeAttribute which is NOT defined
    public void testChangeListenerRemoveAttrNotDef() {
        style.addChangeListener(this);
        bStateChanged = false;
        AttributeSet copy = style.copyAttributes();
        style.removeAttribute("key");
        // Actually no change happened
        assertTrue(style.isEqual(copy));
        // The listener is to be called
        assertTrue(bStateChanged);
    }

    // addAttributes
    public void testChangeListenerAddAttrs() {
        style.addChangeListener(this);
        bStateChanged = false;
        AttributeSet copy = style.copyAttributes();
        style.addAttributes(copy);
        // The listener is to be called
        assertTrue(bStateChanged);
    }

    // removeAttributes(AttributeSet)
    public void testChangeListenerRemoveAttrsSet() {
        style.addChangeListener(this);
        bStateChanged = false;
        AttributeSet copy = style.copyAttributes();
        style.removeAttributes(copy);
        // The listener is to be called
        assertTrue(bStateChanged);
    }

    // removeAttributes(AttributeSet)
    public void testChangeListenerRemoveAttrsEnum() {
        style.addChangeListener(this);
        bStateChanged = false;
        style.removeAttributes(style.getAttributeNames());
        // The listener is to be called
        assertTrue(bStateChanged);
    }

    public void testGetName() {
        String name = "style name";
        style.addAttribute(AttributeSet.NameAttribute, name);
        assertEquals(name, style.getName());
    }

    protected boolean bStateChanged;

    public void stateChanged(final ChangeEvent e) {
        bStateChanged = true;
    }
}
