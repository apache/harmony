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

package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import junit.framework.TestCase;

public class SynthStyleTest extends TestCase {

    static final String ICON_KEY = "TestIcon"; //$NON-NLS-1$

    static final String STRING_KEY = "TestString"; //$NON-NLS-1$

    static final String INT_KEY = "TestInteger"; //$NON-NLS-1$

    static final String BOOLEAN_KEY = "TestBoolean"; //$NON-NLS-1$

    static final Icon ICON_VALUE = new ImageIcon("test"); //$NON-NLS-1$

    static final String STRING_VALUE = "test"; //$NON-NLS-1$

    @SuppressWarnings("boxing")
    static final Integer INT_VALUE = Integer.MAX_VALUE; //$NON-NLS-1$

    @SuppressWarnings("boxing")
    static final Boolean BOOLEAN_VALUE = true; //$NON-NLS-1$

    static final Color COLOR_VALUE = Color.GREEN;

    static final Font FONT_VALUE = new Font("Times", Font.BOLD, 15); //$NON-NLS-1$

    private static JLabel currentComponent = new JLabel("Test_component"); //$NON-NLS-1$

    private static SynthStyle currentStyle = new SynthStyleForTest();

    private static SynthContext currentContext = new SynthContext(
            currentComponent, Region.LABEL, currentStyle,
            SynthConstants.ENABLED);

    /*
     * Test method for 'javax.swing.plaf.synth.SynthStyle.get(SynthContext,
     * Object)'
     */
    public void testGet() {
        assertEquals(currentStyle.get(currentContext, ICON_KEY), ICON_VALUE);
        assertEquals(currentStyle.get(currentContext, INT_KEY), INT_VALUE);
        assertEquals(currentStyle.get(currentContext, STRING_KEY), STRING_VALUE);
        assertEquals(currentStyle.get(currentContext, BOOLEAN_KEY),
                BOOLEAN_VALUE);
    }

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthStyle.getBoolean(SynthContext, Object,
     * boolean)'
     */
    public void testGetBoolean() {
        assertTrue(currentStyle.getBoolean(currentContext, BOOLEAN_KEY, true));
        assertTrue(currentStyle.getBoolean(currentContext, BOOLEAN_KEY, false));
        assertTrue(currentStyle.getBoolean(currentContext,
                "Not_found_boolean", true)); //$NON-NLS-1$
        assertFalse(currentStyle.getBoolean(currentContext,
                "Not_found_boolean", false)); //$NON-NLS-1$
    }

    /*
     * Test method for 'javax.swing.plaf.synth.SynthStyle.getColor(SynthContext,
     * ColorType)'
     */
    public void testGetColor() {
        currentComponent.setBackground(Color.RED);
        assertEquals(currentStyle
                .getColor(currentContext, ColorType.BACKGROUND), Color.RED);
        assertEquals(currentStyle
                .getColor(currentContext, ColorType.FOREGROUND),
                currentComponent.getForeground());
        assertEquals(currentStyle.getColor(currentContext, ColorType.FOCUS),
                COLOR_VALUE);

    }

    /*
     * Test method for 'javax.swing.plaf.synth.SynthStyle.getFont(SynthContext)'
     */
    public void testGetFont() {
        Font newFont = new Font("Dialog", Font.BOLD, 15); //$NON-NLS-1$
        currentComponent.setFont(newFont);
        assertEquals(currentStyle.getFont(currentContext), newFont);
        currentContext.setState(SynthConstants.DISABLED);
        assertEquals(currentStyle.getFont(currentContext), FONT_VALUE);
    }

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthStyle.getInsets(SynthContext, Insets)'
     */
    public void testGetInsets() {
        Insets insets = new Insets(1, 2, 3, 4);
        currentStyle.getInsets(currentContext, insets);
        assertTrue((insets.top == 0) && (insets.left == 0)
                && (insets.right == 0) && (insets.bottom == 0));
    }

    /*
     * Test method for 'javax.swing.plaf.synth.SynthStyle.getInt(SynthContext,
     * Object, int)'
     */
    @SuppressWarnings("boxing")
    public void testGetInt() {
        assertTrue(currentStyle.getInt(currentContext, INT_KEY, INT_VALUE - 1) == INT_VALUE);
        assertTrue(currentStyle.getInt(currentContext,
                "KEY_NOT_FOUND", INT_VALUE - 1) == INT_VALUE - 1); //$NON-NLS-1$
    }

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthStyle.getString(SynthContext, Object,
     * String)'
     */
    public void testGetString() {
        String anotherString = "KEY_NOT_FOUND"; //$NON-NLS-1$
        assertEquals(currentStyle.getString(currentContext, STRING_KEY,
                anotherString), STRING_VALUE);
        assertEquals(currentStyle.getString(currentContext, anotherString,
                anotherString), anotherString);
    }

    /*
     * Test method for
     * 'javax.swing.plaf.synth.SynthStyle.isOpaque(SynthContext)'
     */
    public void testIsOpaque() {
        assertTrue(currentStyle.isOpaque(currentContext));
    }

    private static class SynthStyleForTest extends SynthStyle {

        Hashtable propertiesMap = new Hashtable();

        SynthStyleForTest() {
            addProperty(ICON_KEY, ICON_VALUE);
            addProperty(STRING_KEY, STRING_VALUE);
            addProperty(INT_KEY, INT_VALUE);
            addProperty(BOOLEAN_KEY, BOOLEAN_VALUE);
        }

        @Override
        public Object get(SynthContext context, Object key) {
            return propertiesMap.get(key);
        }

        @Override
        @SuppressWarnings("unused")
        protected Font getFontForState(SynthContext context) {
            if (context.getComponentState() == SynthConstants.DISABLED) {
                return FONT_VALUE;
            }
            return null;
        }

        @Override
        @SuppressWarnings("unused")
        protected Color getColorForState(SynthContext context, ColorType type) {
            if (type == ColorType.FOCUS) {
                return COLOR_VALUE;
            }
            return null;
        }

        public void addProperty(String key, Object value) {
            propertiesMap.put(key, value);
        }
    }

}
