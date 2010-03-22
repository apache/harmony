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
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import junit.framework.TestCase;

public class StyleConstantsTest extends TestCase {
    protected StyleConstants sc;

    protected SimpleAttributeSet attr = new SimpleAttributeSet();

    String message = "Test for StyleConstants";

    private Component component;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sc = new StyleConstants(message);
    }

    public void testToString() {
        assertEquals(message, sc.toString());
    }

    protected void putAttribute(final Object key, final Object value) {
        attr.removeAttributes(attr);
        attr.addAttribute(key, value);
    }

    public void testFirstLineIndent() {
        assertEquals(StyleConstants.ParagraphConstants.class, StyleConstants.FirstLineIndent
                .getClass());
        assertEquals("FirstLineIndent", StyleConstants.FirstLineIndent.toString());
    }

    public void testGetFirstLineIndent() {
        assertTrue(0.0f == StyleConstants.getFirstLineIndent(SimpleAttributeSet.EMPTY));
        float value = 1.23f;
        putAttribute(StyleConstants.FirstLineIndent, new Float(value));
        assertTrue(value == StyleConstants.getFirstLineIndent(attr));
    }

    public void testLeftIndent() {
        assertEquals(StyleConstants.ParagraphConstants.class, StyleConstants.LeftIndent
                .getClass());
        assertEquals("LeftIndent", StyleConstants.LeftIndent.toString());
    }

    public void testGetLeftIndent() {
        assertTrue(0.0f == StyleConstants.getLeftIndent(SimpleAttributeSet.EMPTY));
        float value = 1.234f;
        putAttribute(StyleConstants.LeftIndent, new Float(value));
        assertTrue(value == StyleConstants.getLeftIndent(attr));
    }

    public void testLineSpacing() {
        assertEquals(StyleConstants.ParagraphConstants.class, StyleConstants.LineSpacing
                .getClass());
        assertEquals("LineSpacing", StyleConstants.LineSpacing.toString());
    }

    public void testGetLineSpacing() {
        assertTrue(0.0f == StyleConstants.getLineSpacing(SimpleAttributeSet.EMPTY));
        float value = 1.2345f;
        putAttribute(StyleConstants.LineSpacing, new Float(value));
        assertTrue(value == StyleConstants.getLineSpacing(attr));
    }

    public void testRightIndent() {
        assertEquals(StyleConstants.ParagraphConstants.class, StyleConstants.RightIndent
                .getClass());
        assertEquals("RightIndent", StyleConstants.RightIndent.toString());
    }

    public void testGetRightIndent() {
        assertTrue(0.0f == StyleConstants.getRightIndent(SimpleAttributeSet.EMPTY));
        float value = 1.23456f;
        putAttribute(StyleConstants.RightIndent, new Float(value));
        assertTrue(value == StyleConstants.getRightIndent(attr));
    }

    public void testSpaceAbove() {
        assertEquals(StyleConstants.ParagraphConstants.class, StyleConstants.SpaceAbove
                .getClass());
        assertEquals("SpaceAbove", StyleConstants.SpaceAbove.toString());
    }

    public void testGetSpaceAbove() {
        assertTrue(0.0f == StyleConstants.getSpaceAbove(SimpleAttributeSet.EMPTY));
        float value = 1.234567f;
        putAttribute(StyleConstants.SpaceAbove, new Float(value));
        assertTrue(value == StyleConstants.getSpaceAbove(attr));
    }

    public void testSpaceBelow() {
        assertEquals(StyleConstants.ParagraphConstants.class, StyleConstants.SpaceBelow
                .getClass());
        assertEquals("SpaceBelow", StyleConstants.SpaceBelow.toString());
    }

    public void testGetSpaceBelow() {
        assertTrue(0.0f == StyleConstants.getSpaceBelow(SimpleAttributeSet.EMPTY));
        float value = 1.2345678f;
        putAttribute(StyleConstants.SpaceBelow, new Float(value));
        assertTrue(value == StyleConstants.getSpaceBelow(attr));
    }

    public void testALIGN_CENTER() {
        assertEquals(1, StyleConstants.ALIGN_CENTER);
    }

    public void testALIGN_JUSTIFIED() {
        assertEquals(3, StyleConstants.ALIGN_JUSTIFIED);
    }

    public void testALIGN_LEFT() {
        assertEquals(0, StyleConstants.ALIGN_LEFT);
    }

    public void testALIGN_RIGHT() {
        assertEquals(2, StyleConstants.ALIGN_RIGHT);
    }

    public void testComponentElementName() {
        assertEquals("component", StyleConstants.ComponentElementName);
    }

    public void testIconElementName() {
        assertEquals("icon", StyleConstants.IconElementName);
    }

    public void testAlignment() {
        assertEquals(StyleConstants.ParagraphConstants.class, StyleConstants.Alignment
                .getClass());
        assertEquals("Alignment", StyleConstants.Alignment.toString());
    }

    public void testGetAlignment() {
        assertTrue(StyleConstants.ALIGN_LEFT == StyleConstants
                .getAlignment(SimpleAttributeSet.EMPTY));
        putAttribute(StyleConstants.Alignment, new Integer(StyleConstants.ALIGN_JUSTIFIED));
        assertTrue(StyleConstants.ALIGN_JUSTIFIED == StyleConstants.getAlignment(attr));
    }

    public void testBidiLevel() {
        assertEquals(StyleConstants.CharacterConstants.class, StyleConstants.BidiLevel
                .getClass());
        assertEquals("bidiLevel", StyleConstants.BidiLevel.toString());
    }

    public void testGetBidiLevel() {
        assertTrue(0 == StyleConstants.getBidiLevel(SimpleAttributeSet.EMPTY));
        int val = 1;
        putAttribute(StyleConstants.BidiLevel, new Integer(val));
        assertTrue(val == StyleConstants.getBidiLevel(attr));
    }

    public void testFontSize() {
        assertEquals(StyleConstants.FontConstants.class, StyleConstants.FontSize.getClass());
        assertEquals("size", StyleConstants.FontSize.toString());
    }

    public void testGetFontSize() {
        assertTrue(12 == StyleConstants.getFontSize(SimpleAttributeSet.EMPTY));
        int val = 2;
        putAttribute(StyleConstants.FontSize, new Integer(val));
        assertTrue(val == StyleConstants.getFontSize(attr));
    }

    public void testBold() {
        assertEquals(StyleConstants.FontConstants.class, StyleConstants.Bold.getClass());
        assertEquals("bold", StyleConstants.Bold.toString());
    }

    public void testIsBold() {
        assertFalse(StyleConstants.isBold(SimpleAttributeSet.EMPTY));
        putAttribute(StyleConstants.Bold, Boolean.TRUE);
        assertTrue(StyleConstants.isBold(attr));
        putAttribute(StyleConstants.Bold, Boolean.FALSE);
        assertFalse(StyleConstants.isBold(attr));
    }

    public void testItalic() {
        assertEquals(StyleConstants.FontConstants.class, StyleConstants.Italic.getClass());
        assertEquals("italic", StyleConstants.Italic.toString());
    }

    public void testIsItalic() {
        assertFalse(StyleConstants.isItalic(SimpleAttributeSet.EMPTY));
        putAttribute(StyleConstants.Italic, Boolean.TRUE);
        assertTrue(StyleConstants.isItalic(attr));
        putAttribute(StyleConstants.Italic, Boolean.FALSE);
        assertFalse(StyleConstants.isItalic(attr));
    }

    public void testStrikeThrough() {
        assertEquals(StyleConstants.CharacterConstants.class, StyleConstants.StrikeThrough
                .getClass());
        assertEquals("strikethrough", StyleConstants.StrikeThrough.toString());
    }

    public void testIsStrikeThrough() {
        assertFalse(StyleConstants.isStrikeThrough(SimpleAttributeSet.EMPTY));
        putAttribute(StyleConstants.StrikeThrough, Boolean.TRUE);
        assertTrue(StyleConstants.isStrikeThrough(attr));
        putAttribute(StyleConstants.StrikeThrough, Boolean.FALSE);
        assertFalse(StyleConstants.isStrikeThrough(attr));
    }

    public void testSubscript() {
        assertEquals(StyleConstants.CharacterConstants.class, StyleConstants.Subscript
                .getClass());
        assertEquals("subscript", StyleConstants.Subscript.toString());
    }

    public void testIsSubscript() {
        assertFalse(StyleConstants.isSubscript(SimpleAttributeSet.EMPTY));
        putAttribute(StyleConstants.Subscript, Boolean.TRUE);
        assertTrue(StyleConstants.isSubscript(attr));
        putAttribute(StyleConstants.Subscript, Boolean.FALSE);
        assertFalse(StyleConstants.isSubscript(attr));
    }

    public void testSuperscript() {
        assertEquals(StyleConstants.CharacterConstants.class, StyleConstants.Superscript
                .getClass());
        assertEquals("superscript", StyleConstants.Superscript.toString());
    }

    public void testIsSuperscript() {
        assertFalse(StyleConstants.isSuperscript(SimpleAttributeSet.EMPTY));
        putAttribute(StyleConstants.Superscript, Boolean.TRUE);
        assertTrue(StyleConstants.isSuperscript(attr));
        putAttribute(StyleConstants.Superscript, Boolean.FALSE);
        assertFalse(StyleConstants.isSuperscript(attr));
    }

    public void testUnderline() {
        assertEquals(StyleConstants.CharacterConstants.class, StyleConstants.Underline
                .getClass());
        assertEquals("underline", StyleConstants.Underline.toString());
    }

    public void testIsUnderline() {
        assertFalse(StyleConstants.isUnderline(SimpleAttributeSet.EMPTY));
        putAttribute(StyleConstants.Underline, Boolean.TRUE);
        assertTrue(StyleConstants.isUnderline(attr));
        putAttribute(StyleConstants.Underline, Boolean.FALSE);
        assertFalse(StyleConstants.isUnderline(attr));
    }

    public void testSetFirstLineIndent() {
        attr.removeAttributes(attr);
        float val = 1.2f;
        StyleConstants.setFirstLineIndent(attr, val);
        assertTrue(val == ((Float) attr.getAttribute(StyleConstants.FirstLineIndent))
                .floatValue());
    }

    public void testSetLeftIndent() {
        attr.removeAttributes(attr);
        float val = 1.23f;
        StyleConstants.setLeftIndent(attr, val);
        assertTrue(val == ((Float) attr.getAttribute(StyleConstants.LeftIndent)).floatValue());
    }

    public void testSetLineSpacing() {
        attr.removeAttributes(attr);
        float val = 1.234f;
        StyleConstants.setLineSpacing(attr, val);
        assertTrue(val == ((Float) attr.getAttribute(StyleConstants.LineSpacing)).floatValue());
    }

    public void testSetRightIndent() {
        attr.removeAttributes(attr);
        float val = 1.2345f;
        StyleConstants.setRightIndent(attr, val);
        assertTrue(val == ((Float) attr.getAttribute(StyleConstants.RightIndent)).floatValue());
    }

    public void testSetSpaceAbove() {
        attr.removeAttributes(attr);
        float val = 1.23456f;
        StyleConstants.setSpaceAbove(attr, val);
        assertTrue(val == ((Float) attr.getAttribute(StyleConstants.SpaceAbove)).floatValue());
    }

    public void testSetSpaceBelow() {
        attr.removeAttributes(attr);
        float val = 1.234567f;
        StyleConstants.setSpaceBelow(attr, val);
        assertTrue(val == ((Float) attr.getAttribute(StyleConstants.SpaceBelow)).floatValue());
    }

    public void testSetAlignment() {
        attr.removeAttributes(attr);
        StyleConstants.setAlignment(attr, StyleConstants.ALIGN_JUSTIFIED);
        assertTrue(StyleConstants.ALIGN_JUSTIFIED == ((Integer) attr
                .getAttribute(StyleConstants.Alignment)).intValue());
    }

    public void testSetBidiLevel() {
        attr.removeAttributes(attr);
        int val = 2;
        StyleConstants.setBidiLevel(attr, val);
        assertTrue(val == ((Integer) attr.getAttribute(StyleConstants.BidiLevel)).intValue());
    }

    public void testSetFontSize() {
        attr.removeAttributes(attr);
        int val = 10;
        StyleConstants.setFontSize(attr, val);
        assertTrue(val == ((Integer) attr.getAttribute(StyleConstants.FontSize)).intValue());
    }

    public void testSetBold() {
        attr.removeAttributes(attr);
        StyleConstants.setBold(attr, true);
        assertTrue(((Boolean) attr.getAttribute(StyleConstants.Bold)).booleanValue());
        StyleConstants.setBold(attr, false);
        assertFalse(((Boolean) attr.getAttribute(StyleConstants.Bold)).booleanValue());
    }

    public void testSetItalic() {
        attr.removeAttributes(attr);
        StyleConstants.setItalic(attr, true);
        assertTrue(((Boolean) attr.getAttribute(StyleConstants.Italic)).booleanValue());
        StyleConstants.setItalic(attr, false);
        assertFalse(((Boolean) attr.getAttribute(StyleConstants.Italic)).booleanValue());
    }

    public void testSetStrikeThrough() {
        attr.removeAttributes(attr);
        StyleConstants.setStrikeThrough(attr, true);
        assertTrue(((Boolean) attr.getAttribute(StyleConstants.StrikeThrough)).booleanValue());
        StyleConstants.setStrikeThrough(attr, false);
        assertFalse(((Boolean) attr.getAttribute(StyleConstants.StrikeThrough)).booleanValue());
    }

    public void testSetSubscript() {
        attr.removeAttributes(attr);
        StyleConstants.setSubscript(attr, true);
        assertTrue(((Boolean) attr.getAttribute(StyleConstants.Subscript)).booleanValue());
        StyleConstants.setSubscript(attr, false);
        assertFalse(((Boolean) attr.getAttribute(StyleConstants.Subscript)).booleanValue());
    }

    public void testSetSuperscript() {
        attr.removeAttributes(attr);
        StyleConstants.setSuperscript(attr, true);
        assertTrue(((Boolean) attr.getAttribute(StyleConstants.Superscript)).booleanValue());
        StyleConstants.setSuperscript(attr, false);
        assertFalse(((Boolean) attr.getAttribute(StyleConstants.Superscript)).booleanValue());
    }

    public void testSetUnderline() {
        attr.removeAttributes(attr);
        StyleConstants.setUnderline(attr, true);
        assertTrue(((Boolean) attr.getAttribute(StyleConstants.Underline)).booleanValue());
        StyleConstants.setUnderline(attr, false);
        assertFalse(((Boolean) attr.getAttribute(StyleConstants.Underline)).booleanValue());
    }

    public void testBackground() {
        assertEquals(StyleConstants.ColorConstants.class, StyleConstants.Background.getClass());
        assertEquals("background", StyleConstants.Background.toString());
    }

    public void testGetBackground() {
        assertTrue(Color.black == StyleConstants.getBackground(SimpleAttributeSet.EMPTY));
        Color val = new Color(10, 11, 12);
        putAttribute(StyleConstants.Background, val);
        assertEquals(val, StyleConstants.getBackground(attr));
    }

    public void testForeground() {
        assertEquals(StyleConstants.ColorConstants.class, StyleConstants.Foreground.getClass());
        assertEquals("foreground", StyleConstants.Foreground.toString());
    }

    public void testGetForeground() {
        assertTrue(Color.black == StyleConstants.getForeground(SimpleAttributeSet.EMPTY));
        Color val = new Color(11, 12, 13);
        putAttribute(StyleConstants.Foreground, val);
        assertEquals(val, StyleConstants.getForeground(attr));
    }

    public void testSetBackground() {
        attr.removeAttributes(attr);
        Color val = new Color(13, 14, 15);
        StyleConstants.setBackground(attr, val);
        assertEquals(val, attr.getAttribute(StyleConstants.Background));
    }

    public void testSetForeground() {
        attr.removeAttributes(attr);
        Color val = new Color(15, 16, 17);
        StyleConstants.setForeground(attr, val);
        assertEquals(val, attr.getAttribute(StyleConstants.Foreground));
    }

    public void testComponentAttribute() {
        assertEquals(StyleConstants.CharacterConstants.class, StyleConstants.ComponentAttribute
                .getClass());
        assertEquals("component", StyleConstants.ComponentAttribute.toString());
    }

    public void testGetComponent() throws Exception {
        assertNull(StyleConstants.getComponent(SimpleAttributeSet.EMPTY));
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    component = new JLabel("test component");
                }
            });
        putAttribute(StyleConstants.ComponentAttribute, component);
        assertEquals(component, StyleConstants.getComponent(attr));
    }

    public void testGetComponent_Null() {
        // Regression test for HARMONY-1767
        try {
            StyleConstants.getComponent(null);
            fail("NullPointerException should be thrown");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testSetComponent() throws Exception {
        attr.removeAttributes(attr);
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    component = new JLabel("test component");
                }
            });
        StyleConstants.setComponent(attr, component);
        assertEquals(2, attr.getAttributeCount());
        assertEquals(component,
                     attr.getAttribute(StyleConstants.ComponentAttribute));
        assertEquals(StyleConstants.ComponentElementName,
                     attr.getAttribute(AbstractDocument.ElementNameAttribute));
    }

    public void testFontFamily() {
        assertEquals(StyleConstants.FontConstants.class, StyleConstants.FontFamily.getClass());
        assertEquals("family", StyleConstants.FontFamily.toString());
    }

    public void testGetFontFamily() {
        assertEquals("Monospaced", StyleConstants.getFontFamily(SimpleAttributeSet.EMPTY));
        String val = "arial";
        putAttribute(StyleConstants.FontFamily, val);
        assertEquals(val, StyleConstants.getFontFamily(attr));
    }

    public void testSetFontFamily() {
        attr.removeAttributes(attr);
        String val = "arial";
        StyleConstants.setFontFamily(attr, val);
        assertEquals(val, attr.getAttribute(StyleConstants.FontFamily));
    }

    public void testGetIcon() {
        assertNull(StyleConstants.getIcon(SimpleAttributeSet.EMPTY));
        Icon val = new Icon() {
            public int getIconHeight() {
                return 0;
            }

            public int getIconWidth() {
                return 0;
            }

            public void paintIcon(final Component arg0, final Graphics arg1, final int arg2,
                    final int arg3) {
            }
        };
        putAttribute(StyleConstants.IconAttribute, val);
        assertEquals(val, StyleConstants.getIcon(attr));
    }

    public void testSetIcon() {
        attr.removeAttributes(attr);
        Icon val = new Icon() {
            public int getIconHeight() {
                return 0;
            }

            public int getIconWidth() {
                return 0;
            }

            public void paintIcon(final Component arg0, final Graphics arg1, final int arg2,
                    final int arg3) {
            }
        };
        StyleConstants.setIcon(attr, val);
        assertEquals(2, attr.getAttributeCount());
        assertEquals(val, attr.getAttribute(StyleConstants.IconAttribute));
        assertEquals(StyleConstants.IconElementName, attr
                .getAttribute(AbstractDocument.ElementNameAttribute));
    }

    public void testTabSet() {
        assertEquals(StyleConstants.ParagraphConstants.class, StyleConstants.TabSet.getClass());
        assertEquals("TabSet", StyleConstants.TabSet.toString());
    }

    public void testGetTabSet() {
        assertNull(StyleConstants.getTabSet(SimpleAttributeSet.EMPTY));
        TabSet val = new TabSet(new TabStop[] { new TabStop(0.1f) });
        putAttribute(StyleConstants.TabSet, val);
        assertEquals(val, StyleConstants.getTabSet(attr));
    }

    public void testSetTabSet() {
        attr.removeAttributes(attr);
        TabSet val = new TabSet(new TabStop[] { new TabStop(0.2f) });
        StyleConstants.setTabSet(attr, val);
        assertEquals(val, attr.getAttribute(StyleConstants.TabSet));
    }

    public void testNameAttribute() {
        assertEquals(StyleConstants.class, StyleConstants.NameAttribute.getClass());
        assertEquals("name", StyleConstants.NameAttribute.toString());
    }

    public void testResolveAttribute() {
        assertEquals(StyleConstants.class, StyleConstants.ResolveAttribute.getClass());
        assertEquals("resolver", StyleConstants.ResolveAttribute.toString());
    }

    public void testModelAttribute() {
        assertEquals(StyleConstants.class, StyleConstants.ModelAttribute.getClass());
        assertEquals("model", StyleConstants.ModelAttribute.toString());
    }

    public void testCharacterConstants() {
        assertEquals(StyleConstants.ColorConstants.class, StyleConstants.Background.getClass());
        assertEquals("background", StyleConstants.Background.toString());
        assertEquals(StyleConstants.ColorConstants.class, StyleConstants.Foreground.getClass());
        assertEquals("foreground", StyleConstants.Foreground.toString());
        assertEquals(StyleConstants.FontConstants.class, StyleConstants.Family.getClass());
        assertEquals("family", StyleConstants.Family.toString());
        assertEquals(StyleConstants.FontConstants.class, StyleConstants.Size.getClass());
        assertEquals("size", StyleConstants.Size.toString());
        assertEquals(StyleConstants.FontConstants.class, StyleConstants.Bold.getClass());
        assertEquals("bold", StyleConstants.Bold.toString());
        assertEquals(StyleConstants.FontConstants.class, StyleConstants.Italic.getClass());
        assertEquals("italic", StyleConstants.Italic.toString());
    }

    public void testComposedTextAttribute() {
        assertEquals(StyleConstants.class, StyleConstants.ComposedTextAttribute.getClass());
        assertEquals("composed text", StyleConstants.ComposedTextAttribute.toString());
    }
}
