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

import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Map;
import javax.swing.BasicSwingTestCase;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.StyleContext.SmallAttributeSet;
import junit.framework.TestCase;

public class StyleContextTest extends TestCase implements ChangeListener {
    /**
     * Shared style context object
     */
    protected static StyleContext sc;

    /**
     * The array of attribute key-value pair that is shared in the tests.
     */
    protected static final Object[] attr = { StyleConstants.Bold, Boolean.TRUE,
            StyleConstants.Italic, Boolean.TRUE, StyleConstants.Underline, Boolean.TRUE,
            StyleConstants.StrikeThrough, Boolean.TRUE, StyleConstants.Alignment,
            new Integer(1), StyleConstants.Background, Color.WHITE,
            StyleConstants.FirstLineIndent, new Float(0.75), StyleConstants.FontFamily,
            new String("monospaced"), StyleConstants.FontSize, new Integer(10),
            StyleConstants.Foreground, Color.BLACK };

    /**
     * Tracks that a change listener was been called.
     */
    private boolean bStateChanged;

    /**
     * Adds some attributes from <code>attr</code> array.
     *
     * @param as old attribute set to add to, may be null
     * @param start index of the first attribute in the array
     * @param count the number of attributes to add
     * @return new attribute set with attributes added
     */
    protected static AttributeSet addAttribute(AttributeSet as, int start, int count) {
        if (as == null) {
            as = sc.getEmptySet();
        }
        start <<= 1;
        count <<= 1;
        count += start;
        for (int i = start; i < count; i += 2) {
            as = sc.addAttribute(as, attr[i], attr[i + 1]);
        }
        return as;
    }

    /**
     * Adds some attributes from <code>attr</code> array, starting at index 0.
     *
     * @param as old attribute set to add to, may be null
     * @param count the number of attributes to add
     * @return new attribute set with attributes added
     * @see StyleContextTest#addAttribute(AttributeSet, int, int)
     */
    protected static AttributeSet addAttribute(final AttributeSet as, final int count) {
        return addAttribute(as, 0, count);
    }

    /**
     * Adds <code>count</code> attributes to a newly created attribute set
     * from <code>attr</code> array, starting at index 0.
     *
     * @param count the number of attributes to add
     * @return new attribute set with attributes added
     * @see StyleContextTest#addAttribute(AttributeSet, int, int)
     */
    protected static AttributeSet addAttribute(final int count) {
        return addAttribute(null, 0, count);
    }

    /**
     * Wrapper to access StyleContext.cache field.
     * @return style context's cache
     */
    protected static Map<?, ?> getSCCache() {
        try {
            Field f = sc.getClass().getDeclaredField("cache");
            f.setAccessible(true);
            return (Map<?, ?>) (f.get(sc));
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        }
        return null;
    }

    /**
     * Wrapper to access StyleContext.fontCache field.
     * @return style context's cache
     */
    protected static Map<?, ?> getSCFontCache() {
        try {
            Field f = sc.getClass().getDeclaredField("fontCache");
            f.setAccessible(true);
            return (Map<?, ?>) (f.get(sc));
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        }
        return null;
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        sc = new StyleContext();
    }

    /**
     * Makes sure that after initialization, any style context will
     * contain exactly one style (default).
     */
    public void testStyleContext() {
        int count = 0;
        Enumeration<?> names = sc.getStyleNames();
        Object name = null;
        while (names.hasMoreElements()) {
            name = names.nextElement();
            count++;
        }
        assertEquals(1, count);
        assertEquals("default", (String) name);
    }

    public void testCollectGarbageInCache() {
        if (!BasicSwingTestCase.isHarmony()) {
            // This is internal test only
            return;
        }
        Map<?, ?> cache = getSCCache();
        assertEquals(2, cache.size());
        // Create an attribute set containing 10 items. This will
        // create sets with items from 1 to 10
        addAttribute(10);
        // The sets with number of items from 1 up to 9 will be cached
        // (Plus attribute set to support styles: the named style itself
        //  with "name" attribute and the reference to this style from
        //  the internal list of styles)
        assertEquals(9 + 2, cache.size());
        // Run the garbage collector several times
        for (int i = 0; i < 20; i++) {
            System.gc();
        }
        // Create an attribute set cotaining one and only item. This will
        // lead to a call to collectGarbageInCache method
        addAttribute(1);
        // The cache has been cleared but the newly created set is cached
        assertEquals(1 + 2, cache.size());
    }

    public void testCollectGarbageInCacheFont() {
        if (!BasicSwingTestCase.isHarmony()) {
            // This is internal test only
            return;
        }
        Map<?, ?> fontCache = getSCFontCache();
        assertEquals(0, fontCache.size());
        sc.getFont("Arial", Font.BOLD, 14);
        sc.getFont("Tahoma", Font.PLAIN, 8);
        assertEquals(2, fontCache.size());
        // Run the garbage collector several times
        for (int i = 0; i < 5; i++) {
            System.gc();
        }
        // Create an attribute set cotaining one and only item. This will
        // lead to a call to collectGarbageInCache method
        addAttribute(1);
        // The cache has been cleared but the newly created set is cached
        assertEquals(0, fontCache.size());
    }

    public void testAddStyle() {
        Style aStyle = sc.addStyle("aStyle", null);
        Style anotherStyle = sc.addStyle("anotherStyle", aStyle);
        int count = 0;
        boolean[] was = { false, false, false };
        String styleNames = new String();
        Enumeration<?> names = sc.getStyleNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            styleNames += name + " ";
            if (name == "aStyle") {
                assertSame(aStyle, sc.getStyle(name));
                // Should contain only "name" attribute
                assertEquals(1, aStyle.getAttributeCount());
                assertNull(aStyle.getResolveParent());
                was[0] = true;
            } else if (name == "anotherStyle") {
                assertSame(anotherStyle, sc.getStyle(name));
                // Should contain "name" and "parent" attributes
                assertEquals(2, anotherStyle.getAttributeCount());
                assertNotNull(anotherStyle.getResolveParent());
                assertSame(aStyle, anotherStyle.getResolveParent());
                was[1] = true;
            } else if (name == "default") {
                sc.getStyle(name);
                assertEquals(1, aStyle.getAttributeCount());
                assertNull(aStyle.getResolveParent());
                was[2] = true;
            }
            count++;
        }
        assertEquals(3, count);
        for (int i = 0; i < was.length; i++) {
            assertTrue("The style named '"
                    + (i == 0 ? "aStyle" : (i == 1 ? "anotherStyle" : "default"))
                    + "' was not in the list.", was[i]);
        }
    }

    public void testAddStyleMisc() {
        // Add styles with diff parameters
        Object[] styles = { null, null, "one", null, null, sc.new NamedStyle(), "two",
                sc.new NamedStyle() };
        for (int i = 0; i < styles.length; i += 2) {
            Style style = sc.addStyle((String) styles[i], (Style) styles[i + 1]);
            assertEquals("Iteration: " + i, (String) styles[i], style.getName());
            assertSame("Iteration: " + i, styles[i + 1], style.getResolveParent());
        }
    }

    public void testAddStyleTwice() {
        final String styleName = "styleName";
        final Style style = sc.addStyle(styleName, null);
        final Style another = sc.addStyle(styleName, null);
        assertNotSame(style, another);
        assertSame(another, sc.getStyle(styleName));
    }

    public void testCreateSmallAttributeSet() {
        AttributeSet as = sc.createSmallAttributeSet(sc.getEmptySet());
        assertTrue(as instanceof SmallAttributeSet);
        assertEquals(0, as.getAttributeCount());
        assertEquals(sc.getEmptySet(), as);
    }

    public void testGetStyle() {
        Style style = sc.getStyle("default");
        assertEquals("default", style.getName());
        sc.addStyle("aStyle", style);
        style = sc.getStyle("aStyle");
        assertEquals("aStyle", style.getName());
        assertEquals("default", ((Style) style.getResolveParent()).getName());
    }

    public void testCreateLargeAttributeSet() {
        AttributeSet as = sc.createLargeAttributeSet(new SimpleAttributeSet());
        assertTrue(as instanceof SimpleAttributeSet);
    }

    public void testGetEmptySet() {
        assertSame(sc.getEmptySet(), sc.getEmptySet());
        assertEquals(0, sc.getEmptySet().getAttributeCount());
    }

    // test {add,remove}ChangeListener while adding styles
    public void testChangeListenerAddStyle() {
        bStateChanged = false;
        sc.addStyle("one", null);
        assertFalse(bStateChanged);
        sc.addChangeListener(this);
        bStateChanged = false;
        sc.addStyle("two", null);
        assertTrue(bStateChanged);
        sc.removeChangeListener(this);
        bStateChanged = false;
        sc.addStyle("three", null);
        assertFalse(bStateChanged);
    }

    // test if a listener gets called when adding style with null name
    public void testChangeListenerAddStyleNull() {
        sc.addChangeListener(this);
        bStateChanged = false;
        sc.addStyle(null, StyleContext.getDefaultStyleContext().new NamedStyle());
        assertFalse(bStateChanged);
        int count = 0;
        boolean wasNull = false;
        Enumeration<?> names = sc.getStyleNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            wasNull = wasNull || name == null;
            count++;
        }
        assertEquals(1, count);
        assertFalse(wasNull);
    }

    // test {add,remove}ChangeListener while removing styles
    public void testChangeListenerRemoveStyle() {
        sc.addStyle("one", null);
        sc.addStyle("two", null);
        sc.addStyle("three", null);
        bStateChanged = false;
        sc.removeStyle("one");
        assertFalse(bStateChanged);
        sc.addChangeListener(this);
        bStateChanged = false;
        sc.removeStyle("two");
        assertTrue(bStateChanged);
        sc.removeChangeListener(this);
        bStateChanged = false;
        sc.removeStyle("three");
        assertFalse(bStateChanged);
    }

    public void testGetChangeListeners() {
        ChangeListener[] listeners = sc.getChangeListeners();
        assertEquals(0, listeners.length);
        sc.addChangeListener(this);
        listeners = sc.getChangeListeners();
        assertEquals(1, listeners.length);
        sc.removeChangeListener(this);
        listeners = sc.getChangeListeners();
        assertEquals(0, listeners.length);
    }

    public void testGetStyleNamesDef() {
        boolean wasDefault = false;
        int count = 0;
        Enumeration<?> names = sc.getStyleNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            wasDefault = name == "default";
            count++;
        }
        assertEquals(1, count);
        assertTrue(wasDefault);
    }

    public void testGetStyleNames() {
        sc.addStyle("style", null);
        boolean wasDefault = false;
        boolean wasStyle = false;
        int count = 0;
        Enumeration<?> names = sc.getStyleNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            wasDefault = wasDefault || name == "default";
            wasStyle = wasStyle || name == "style";
            count++;
        }
        assertEquals(2, count);
        assertTrue(wasDefault);
        assertTrue(wasStyle);
    }

    public void testRemoveStyle() {
        sc.addStyle("style", null);
        sc.removeStyle("style");
        boolean wasDefault = false;
        boolean wasStyle = false;
        int count = 0;
        Enumeration<?> names = sc.getStyleNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            wasDefault = wasDefault || "default".equals(name);
            wasStyle = wasStyle || "style".equals(name);
            count++;
        }
        assertEquals(1, count);
        assertTrue(wasDefault);
        assertFalse(wasStyle);
    }

    public void testToString() {
        Style style = sc.getStyle(StyleContext.DEFAULT_STYLE);
        style.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        style.addAttribute(StyleConstants.Italic, Boolean.FALSE);
        style = sc.addStyle("aStyle", null);
        style.addAttribute(StyleConstants.FontFamily, "Arial");
        style.addAttribute(StyleConstants.FontSize, new Integer(14));
        sc.getFont(style);
        String[] asStr = new String[] { "{family=Arial,name=aStyle,size=14,}",
                "{default=AttributeSet,}", "{family=Arial,name=aStyle,}",
                "{aStyle=AttributeSet,default=AttributeSet,}",
                "{italic=false,bold=true,name=default,}", "{name=default,}", "{name=aStyle,}",
                "{name=default,bold=true,}", };
        String scStr = sc.toString();
        assertNotNull(scStr);
        String[] splitStrs = scStr.replaceAll("\r", "").split("\n");
        assertEquals(asStr.length, splitStrs.length);
        boolean[] met = new boolean[splitStrs.length];
        for (int i = 0; i < splitStrs.length; i++) {
            for (int j = 0; j < asStr.length; j++) {
                if (asStr[j].length() == splitStrs[i].length()
                        && assertAttributes(trimEnds(asStr[j]).split(","), trimEnds(
                                splitStrs[i]).split(","))) {
                    met[j] = true;
                }
            }
        }
        for (int i = 0; i < met.length; i++) {
            assertTrue("asStr[" + i + "] is missing", met[i]);
        }
    }

    public static boolean assertAttributes(final String[] expAttrs, final String[] realAttrs) {
        if (expAttrs.length != realAttrs.length) {
            return false;
        }
        boolean[] met = new boolean[expAttrs.length];
        for (int i = 0; i < realAttrs.length; i++) {
            for (int j = 0; j < expAttrs.length; j++) {
                if (expAttrs[j].equals(realAttrs[i])) {
                    met[j] = true;
                }
            }
        }
        boolean result = true;
        for (int i = 0; i < met.length && result; i++) {
            result = met[i];
        }
        return result;
    }

    public static String trimEnds(final String str) {
        assertEquals('{', str.charAt(0));
        final int len = str.length();
        assertEquals('}', str.charAt(len - 1));
        assertEquals(',', str.charAt(len - 2));
        return str.substring(1, len - 2);
    }

    public void testGetCompressionThreshold() {
        assertEquals(9, sc.getCompressionThreshold());
    }

    public void testGetDefaultStyleContext() {
        StyleContext def;
        assertSame(def = StyleContext.getDefaultStyleContext(), StyleContext
                .getDefaultStyleContext());
        int count = 0;
        Enumeration<?> names = def.getStyleNames();
        Object name = null;
        while (names.hasMoreElements()) {
            count++;
            name = names.nextElement();
        }
        assertEquals(1, count);
        assertEquals(StyleContext.DEFAULT_STYLE, (String) name);
    }

    /**
     * A class to test caching technique.
     * Stores values returned by createSmallAttributeSet in array.
     */
    private static class StyleContextX extends StyleContext {
        private static final long serialVersionUID = 1L;

        public int count;

        public AttributeSet[] attrSet;

        public StyleContextX() {
            count = 0;
            attrSet = new AttributeSet[20];
        }

        @Override
        public SmallAttributeSet createSmallAttributeSet(final AttributeSet a) {
            if (attrSet == null) {
                // This condition is true in StyleContext's constructor
                return super.createSmallAttributeSet(a);
            }
            attrSet[count] = super.createSmallAttributeSet(a);
            return (SmallAttributeSet) attrSet[count++];
        }

        @Override
        public MutableAttributeSet createLargeAttributeSet(final AttributeSet a) {
            attrSet[count] = super.createLargeAttributeSet(a);
            return (MutableAttributeSet) attrSet[count++];
        }
    }

    public void testCaching() {
        StyleContextX sc = new StyleContextX();
        int addStyle = sc.count;
        sc.addStyle("aStyle", null);
        int addAttr = sc.count;
        AttributeSet as = sc.addAttribute(sc.getEmptySet(), AttributeSet.NameAttribute,
                "aStyle");
        assertFalse(addStyle == addAttr);
        assertSame(sc.attrSet[addStyle], as);
    }

    /*
     public void testReclaim() {
     }
     */
    public void stateChanged(final ChangeEvent event) {
        bStateChanged = true;
    }
}
