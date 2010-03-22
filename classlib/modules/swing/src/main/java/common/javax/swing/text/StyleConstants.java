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
import javax.swing.Icon;

public class StyleConstants {

    public static class CharacterConstants extends StyleConstants
        implements AttributeSet.CharacterAttribute {

        private CharacterConstants(final String key) {
            super(key);
        }
    }

    public static class ColorConstants extends StyleConstants
        implements AttributeSet.ColorAttribute,
                   AttributeSet.CharacterAttribute {
        private ColorConstants(final String key) {
            super(key);
        }
    }

    public static class FontConstants extends StyleConstants
        implements AttributeSet.FontAttribute, AttributeSet.CharacterAttribute {

        private FontConstants(final String key) {
            super(key);
        }
    }

    public static class ParagraphConstants extends StyleConstants
        implements AttributeSet.ParagraphAttribute {

        private ParagraphConstants(final String key) {
            super(key);
        }
    }

    public static final String ComponentElementName = "component";
    public static final String IconElementName = "icon";


    // CharacterConstants

    public static final Object Underline =
        new CharacterConstants("underline");

    public static final Object StrikeThrough =
        new CharacterConstants("strikethrough");

    public static final Object Superscript =
        new CharacterConstants("superscript");

    public static final Object Subscript =
        new CharacterConstants("subscript");

    public static final Object ComponentAttribute =
        new CharacterConstants("component");

    public static final Object IconAttribute =
        new CharacterConstants("icon");

    public static final Object BidiLevel =
        new CharacterConstants("bidiLevel");


    // ColorConstants

    public static final Object Foreground = new ColorConstants("foreground");
    public static final Object Background = new ColorConstants("background");


    // FontConstants

    public static final Object Family = new FontConstants("family");
    public static final Object Size   = new FontConstants("size");
    public static final Object Bold   = new FontConstants("bold");
    public static final Object Italic = new FontConstants("italic");

    public static final Object FontFamily = Family;
    public static final Object FontSize = Size;


    // ParagraphConstants

    public static final Object FirstLineIndent =
        new ParagraphConstants("FirstLineIndent");

    public static final Object LeftIndent =
        new ParagraphConstants("LeftIndent");

    public static final Object RightIndent =
        new ParagraphConstants("RightIndent");

    public static final Object LineSpacing =
        new ParagraphConstants("LineSpacing");

    public static final Object SpaceAbove =
        new ParagraphConstants("SpaceAbove");

    public static final Object SpaceBelow =
        new ParagraphConstants("SpaceBelow");

    public static final Object Alignment =
        new ParagraphConstants("Alignment");

    public static final Object TabSet =
        new ParagraphConstants("TabSet");

    public static final Object Orientation =
        new ParagraphConstants("orientation");



    public static final Object NameAttribute =
        new StyleConstants("name");

    public static final Object ResolveAttribute =
        new StyleConstants("resolver");

    public static final Object ModelAttribute =
        new StyleConstants("model");

    public static final Object ComposedTextAttribute =
        new StyleConstants("composed text");

    public static final int ALIGN_LEFT      = 0;
    public static final int ALIGN_CENTER    = 1;
    public static final int ALIGN_RIGHT     = 2;
    public static final int ALIGN_JUSTIFIED = 3;

    /**
     * Contains attribute name.
     */
    private String attrKey;

    StyleConstants(final String key) {
        this.attrKey = key;
    }

    public String toString() {
        return attrKey;
    }

    public static void setTabSet(final MutableAttributeSet a,
                                 final TabSet tabs) {
        a.addAttribute(TabSet, tabs);
    }

    public static TabSet getTabSet(final AttributeSet a) {
        return (TabSet)a.getAttribute(TabSet);
    }

    public static void setIcon(final MutableAttributeSet a, final Icon c) {
        a.addAttribute(IconAttribute, c);
        a.addAttribute(AbstractDocument.ElementNameAttribute, IconElementName);
    }

    public static Icon getIcon(final AttributeSet a) {
        return (Icon)a.getAttribute(IconAttribute);
    }

    public static void setFontFamily(final MutableAttributeSet a,
                                     final String fam) {
        a.addAttribute(FontFamily, fam);
    }

    public static String getFontFamily(final AttributeSet a) {
        String val = (String)a.getAttribute(FontFamily);
        return (val == null) ? "Monospaced" : val;
    }

    public static void setComponent(final MutableAttributeSet a,
                                    final Component c) {
        a.addAttribute(ComponentAttribute, c);
        a.addAttribute(AbstractDocument.ElementNameAttribute,
                       ComponentElementName);
    }

    public static Component getComponent(final AttributeSet a) {
        return (Component)a.getAttribute(ComponentAttribute);
    }

    public static void setForeground(final MutableAttributeSet a,
                                     final Color fg) {
        a.addAttribute(Foreground, fg);
    }

    public static void setBackground(final MutableAttributeSet a,
                                     final Color bg) {
        a.addAttribute(Background, bg);
    }

    public static Color getForeground(final AttributeSet a) {
        Color val = (Color)a.getAttribute(Foreground);
        return (val == null) ? Color.black : val;
    }

    public static Color getBackground(final AttributeSet a) {
        Color val = (Color)a.getAttribute(Background);
        return (val == null) ? Color.black : val;
    }

    public static void setUnderline(final MutableAttributeSet a,
                                    final boolean b) {
        a.addAttribute(Underline, Boolean.valueOf(b));
    }

    public static void setSuperscript(final MutableAttributeSet a,
                                      final boolean b) {
        a.addAttribute(Superscript, Boolean.valueOf(b));
    }

    public static void setSubscript(final MutableAttributeSet a,
                                    final boolean b) {
        a.addAttribute(Subscript, Boolean.valueOf(b));
    }

    public static void setStrikeThrough(final MutableAttributeSet a,
                                        final boolean b) {
        a.addAttribute(StrikeThrough, Boolean.valueOf(b));
    }

    public static void setItalic(final MutableAttributeSet a, final boolean b) {
        a.addAttribute(Italic, Boolean.valueOf(b));
    }

    public static void setBold(final MutableAttributeSet a, final boolean b) {
        a.addAttribute(Bold, Boolean.valueOf(b));
    }

    public static void setFontSize(final MutableAttributeSet a, final int s) {
        // TODO in version 1.5.0 use Integer.valueOf instead of constructor
        a.addAttribute(FontSize, new Integer(s));
    }

    public static void setBidiLevel(final MutableAttributeSet a, final int o) {
        a.addAttribute(BidiLevel, new Integer(o));
    }

    public static void setAlignment(final MutableAttributeSet a,
                                    final int align) {
        a.addAttribute(Alignment, new Integer(align));
    }

    public static void setSpaceBelow(final MutableAttributeSet a,
                                     final float i) {
        // TODO in version 1.5.0 use Float.valueOf instead of constructor
        a.addAttribute(SpaceBelow, new Float(i));
    }

    public static void setSpaceAbove(final MutableAttributeSet a,
                                     final float i) {
        a.addAttribute(SpaceAbove, new Float(i));
    }

    public static void setRightIndent(final MutableAttributeSet a,
                                      final float i) {
        a.addAttribute(RightIndent, new Float(i));
    }

    public static void setLineSpacing(final MutableAttributeSet a,
                                      final float i) {
        a.addAttribute(LineSpacing, new Float(i));
    }

    public static void setLeftIndent(final MutableAttributeSet a,
                                     final float i) {
        a.addAttribute(LeftIndent, new Float(i));
    }

    public static void setFirstLineIndent(final MutableAttributeSet a,
                                          final float i) {
        a.addAttribute(FirstLineIndent, new Float(i));
    }

    public static boolean isUnderline(final AttributeSet a) {
        Boolean val = (Boolean)a.getAttribute(Underline);
        return (val == null) ? false : val.booleanValue();
    }

    public static boolean isSuperscript(final AttributeSet a) {
        Boolean val = (Boolean)a.getAttribute(Superscript);
        return (val == null) ? false : val.booleanValue();
    }

    public static boolean isSubscript(final AttributeSet a) {
        Boolean val = (Boolean)a.getAttribute(Subscript);
        return (val == null) ? false : val.booleanValue();
    }

    public static boolean isStrikeThrough(final AttributeSet a) {
        Boolean val = (Boolean)a.getAttribute(StrikeThrough);
        return (val == null) ? false : val.booleanValue();
    }

    public static boolean isItalic(final AttributeSet a) {
        Boolean val = (Boolean)a.getAttribute(Italic);
        return (val == null) ? false : val.booleanValue();
    }

    public static boolean isBold(final AttributeSet a) {
        Boolean val = (Boolean)a.getAttribute(Bold);
        return (val == null) ? false : val.booleanValue();
    }

    public static int getFontSize(final AttributeSet a) {
        Integer size = (Integer)a.getAttribute(FontSize);
        return (size == null) ? 12 : size.intValue();
    }

    public static int getBidiLevel(final AttributeSet a) {
        Integer level = (Integer)a.getAttribute(BidiLevel);
        return (level == null) ? 0 : level.intValue();
    }

    public static int getAlignment(final AttributeSet a) {
        Integer align = (Integer)a.getAttribute(Alignment);
        return (align == null) ? ALIGN_LEFT : align.intValue();
    }

    public static float getSpaceBelow(final AttributeSet a) {
        Float f = (Float)a.getAttribute(SpaceBelow);
        return (f == null) ? 0.f : f.floatValue();
    }

    public static float getSpaceAbove(final AttributeSet a) {
        Float f = (Float)a.getAttribute(SpaceAbove);
        return (f == null) ? 0.f : f.floatValue();
    }

    public static float getRightIndent(final AttributeSet a) {
        Float f = (Float)a.getAttribute(RightIndent);
        return (f == null) ? 0.f : f.floatValue();
    }

    public static float getLineSpacing(final AttributeSet a) {
        Float f = (Float)a.getAttribute(LineSpacing);
        return (f == null) ? 0.f : f.floatValue();
    }

    public static float getLeftIndent(final AttributeSet a) {
        Float f = (Float)a.getAttribute(LeftIndent);
        return (f == null) ? 0.f : f.floatValue();
    }

    public static float getFirstLineIndent(final AttributeSet a) {
        Float f = (Float)a.getAttribute(FirstLineIndent);
        return (f == null) ? 0.f : f.floatValue();
    }
}

