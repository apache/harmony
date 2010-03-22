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
import java.util.EventListener;
import javax.swing.BasicSwingTestCase;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeListener;
import javax.swing.text.StyleContext.NamedStyle;

public class StyleContext_NamedStyleTest extends StyleTest {
    protected NamedStyle ns;

    protected NamedStyle withName;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ns = (NamedStyle) style;
        StyleContext defContext = StyleContext.getDefaultStyleContext();
        withName = defContext.new NamedStyle("styleName", null);
    }

    /*
     * NamedStyle(String, Style)
     */
    public void testNamedStyleStringStyle() {
        Style parent = StyleContext.getDefaultStyleContext().new NamedStyle();
        parent.addAttribute("key", "value");
        String name = "style_name";
        ns = StyleContext.getDefaultStyleContext().new NamedStyle(name, parent);
        assertEquals(parent, ns.getResolveParent());
        assertEquals(name, ns.getName());
    }

    /*
     * NamedStyle(String, Style)
     */
    public void testNamedStyleStringNullStyle() {
        Style parent = StyleContext.getDefaultStyleContext().new NamedStyle("parentName", null);
        parent.addAttribute("key", "value");
        ns = StyleContext.getDefaultStyleContext().new NamedStyle(null, parent);
        assertEquals(parent, ns.getResolveParent());
        assertSame(parent, ns.getResolveParent());
        // Using getName we should have null
        assertNull(ns.getName());
        // But using get attribute, we should get parent name 'cause this
        // method resolves attribute values through parent
        assertEquals("parentName", ns.getAttribute(AttributeSet.NameAttribute));
    }

    /*
     * NamedStyle(String, Style)
     */
    public void testNamedStyleStringStyleNull() {
        ns = StyleContext.getDefaultStyleContext().new NamedStyle("styleName", null);
        assertNull(ns.getResolveParent());
        assertEquals("styleName", ns.getName());
    }

    /*
     * NamedStyle(String, Style)
     */
    public void testNamedStyleStringNullStyleNull() {
        ns = StyleContext.getDefaultStyleContext().new NamedStyle(null, null);
        assertNull(ns.getResolveParent());
        assertNull(ns.getName());
    }

    /*
     * NamedStyle(Style)
     */
    public void testNamedStyleStyle() {
        Style parent = StyleContext.getDefaultStyleContext().new NamedStyle();
        parent.addAttribute("key", "value");
        ns = StyleContext.getDefaultStyleContext().new NamedStyle(parent);
        assertEquals(parent, ns.getResolveParent());
        assertEquals(1, ns.getAttributeCount());
        assertEquals("value", ns.getAttribute("key"));
    }

    /*
     * NamedStyle()
     */
    public void testNamedStyle() {
        ns = StyleContext.getDefaultStyleContext().new NamedStyle();
        assertEquals(0, ns.getAttributeCount());
        assertNull(ns.getAttribute(AttributeSet.NameAttribute));
        assertNull(ns.getAttribute(AttributeSet.ResolveAttribute));
    }

    public void testSetNameNull() {
        ns = StyleContext.getDefaultStyleContext().new NamedStyle("styleName", null);
        ns.setName(null);
        assertEquals("styleName", ns.getName());
    }

    public void testSetName() {
        ns = StyleContext.getDefaultStyleContext().new NamedStyle();
        ns.setName("styleName");
        assertEquals("styleName", ns.getName());
        assertEquals("styleName", ns.getAttribute(AttributeSet.NameAttribute));
    }

    public void testSetNameListeners() {
        ns.addChangeListener(this);
        bStateChanged = false;
        ns.setName("styleName");
        assertTrue(bStateChanged);
    }

    public void testGetNameSetAttribute() {
        assertEquals("styleName", withName.getName());
        assertEquals("styleName", withName.getAttribute(AttributeSet.NameAttribute));
        // Add name attribute
        withName.addAttribute(AttributeSet.NameAttribute, new String("Changed Name"));
        assertEquals("Changed Name", withName.getName());
        assertEquals("Changed Name", withName.getAttribute(AttributeSet.NameAttribute));
    }

    public void testContainsNameAttribute() {
        SimpleAttributeSet as = new SimpleAttributeSet();
        as.addAttribute(AttributeSet.NameAttribute, "styleName");
        assertTrue(withName.containsAttribute(AttributeSet.NameAttribute, "styleName"));
        assertTrue(withName.containsAttributes(as));
    }

    public void testNameNotString() {
        ns = StyleContext.getDefaultStyleContext().new NamedStyle();
        ns.addAttribute(AttributeSet.NameAttribute, new Integer(15));
        assertEquals(new Integer(15), ns.getAttribute(AttributeSet.NameAttribute));
        assertEquals((new Integer(15)).toString(), ns.getName());
    }

    public void testRemoveNameAttribute() {
        withName.removeAttribute(AttributeSet.NameAttribute);
        assertNull(withName.getName());
    }

    public void testGetAttrNames() {
        withName.addAttribute("key", "value");
        boolean wasKey = false;
        boolean wasName = false;
        Enumeration<?> keys = withName.getAttributeNames();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            wasKey = wasKey || key.equals("key");
            wasName = wasName || key.equals(AttributeSet.NameAttribute);
        }
        assertTrue(wasKey);
        assertTrue(wasName);
    }

    public void testGetAttrCountName() {
        assertEquals(1, withName.getAttributeCount());
    }

    public void testGetListeners() {
        ns.addChangeListener(this);
        EventListener[] listeners = ns.getListeners(ChangeListener.class);
        assertEquals(1, listeners.length);
        assertSame(this, listeners[0]);
        listeners = ns.getListeners(CaretListener.class);
        assertEquals(0, listeners.length);
        ns.removeChangeListener(this);
        listeners = ns.getListeners(ChangeListener.class);
        assertEquals(0, listeners.length);
    }

    public void testGetChangeListeners() {
        ChangeListener[] listeners = ns.getChangeListeners();
        assertEquals(0, listeners.length);
        ns.addChangeListener(this);
        listeners = ns.getChangeListeners();
        assertEquals(1, listeners.length);
        assertSame(this, listeners[0]);
        ns.removeChangeListener(this);
        listeners = ns.getChangeListeners();
        assertEquals(0, listeners.length);
    }

    public void testToString() {
        String str = ns.toString();
        assertNotNull(ns.toString());
        assertTrue(str.startsWith("NamedStyle:null {"));
        String[] attrs = str.substring(17, str.length() - 1).split(",");
        String[] expected = { "key1=value1", "key2=value2", "key3=value3" };
        boolean[] found = { false, false, false };
        assertEquals(expected.length, attrs.length);
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < attrs.length && !found[i]; j++) {
                found[i] = found[i] || expected[i].equals(attrs[j]);
            }
        }
        for (int i = 0; i < found.length; i++) {
            if (!found[i]) {
                fail(expected[i] + " was not found");
            }
        }
    }

    public void testFireStateChanged() {
        ns.addChangeListener(this);
        bStateChanged = false;
        ns.fireStateChanged();
        assertTrue(bStateChanged);
        ns.removeChangeListener(this);
        bStateChanged = false;
        ns.fireStateChanged();
        assertFalse(bStateChanged);
    }

    public void testChangeEvent() {
        assertNull(ns.changeEvent);
        ns.fireStateChanged();
        assertNull(ns.changeEvent);
        ns.addChangeListener(this);
        assertNull(ns.changeEvent);
        ns.fireStateChanged();
        assertEquals(ns, ns.changeEvent.getSource());
    }

    public void testSerializable() throws Exception {
        ns.setName("styleName");
        NamedStyle read = (NamedStyle) BasicSwingTestCase.serializeObject(ns);
        assertTrue(ns.isEqual(read));
    }

    public void testListenerList() {
        assertNotNull(ns.listenerList);
        assertTrue(ns.listenerList.getListenerCount() == 0);
        ns.addChangeListener(this);
        assertTrue(ns.listenerList.getListenerCount() == 1);
    }

    public void testSetResolveParentNull() {
        AttributeSet parent = mas.getResolveParent();
        int count = mas.getAttributeCount();
        if (parent == null) {
            // Set parent to a non-null value
            mas.setResolveParent(new SimpleAttributeSet());
            // The number of attributes has increased by one
            assertEquals(++count, mas.getAttributeCount());
        }
        // Set the parent to null
        mas.setResolveParent(null);
        assertNull(mas.getResolveParent());
        assertEquals(count - 1, mas.getAttributeCount());
    }

    /**
     * Test copyAttributesMethod when a NamedStyle has name and parent.
     *
     */
    public void testCopyAttributesWithName() {
        StyleContext def = new StyleContext();
        Style parent = def.addStyle("parentStyle", null);
        Style style = def.addStyle("aStyle", parent);
        // Copy parent
        Style copyParent = (Style) parent.copyAttributes();
        assertTrue(copyParent instanceof StyleContext.NamedStyle);
        assertEquals("parentStyle", copyParent.getName());
        assertNull(copyParent.getResolveParent());
        // Copy style
        Style copyStyle = (Style) style.copyAttributes();
        assertTrue(copyStyle instanceof StyleContext.NamedStyle);
        assertEquals("aStyle", copyStyle.getName());
        assertSame(parent, copyStyle.getResolveParent());
    }
}
