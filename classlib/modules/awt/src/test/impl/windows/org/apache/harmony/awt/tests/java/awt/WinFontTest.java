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

package org.apache.harmony.awt.tests.java.awt;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.font.TransformAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;

/*
 *  This test is valid only on Windows!
 */
public class WinFontTest extends TestCase {

    Font physicalFont;
    Font dialogFont;
    FontRenderContext frc = new FontRenderContext(null, false, false);
    
    final double ARIAL_ITALIC_ANGLE = 0.2121;
    final int ARIAL_NUM_GLYPHS = 1674;
    
    final String ARIAL_NAME = "Arial";
    final String ARIAL_FACE_NAME = "Arial";
    final String ARIAL_DE_FACE_NAME = "Arial Fett Kursiv";
    final String ARIAL_PS_NAME = "ArialMT";
    final String DIALOG_NAME = "dialog"; 
    final String DIALOG_FAMILY_NAME = "Dialog"; 
    final String DIALOG_FACE_NAME = "Dialog.plain"; 
    
    @Override
    public void setUp() throws Exception{
        super.setUp();
        physicalFont = new Font("Arial", Font.PLAIN, 12);
        dialogFont = new Font("dialog", Font.PLAIN, 12);
        
    }

    /*
     * Test method for 'java.awt.Font.Font(Map<? extends Attribute, ?>)'
     */
    public final void testFontMapOfQextendsAttributeQ() {
        // Font created from attributes
        Hashtable <Attribute, Object> attributes = new Hashtable <Attribute, Object>();
        attributes.put(TextAttribute.FAMILY, "dialog");
        attributes.put(TextAttribute.SIZE, new Float(12));
        attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        
        Font font = new Font(attributes);
        assertNotNull(font);
        assertEquals(font.getSize(), 12);
        assertEquals(font.getStyle(), Font.PLAIN);
        assertEquals(font.getName(), "dialog");
    }

    /*
     * Test method for 'java.awt.Font.Font(String, int, int)'
     */
    public final void testFontStringIntInt() {
        // logical font
        assertNotNull(dialogFont);
        assertEquals(dialogFont.getSize(), 12);
        assertEquals(dialogFont.getStyle(), Font.PLAIN);
        assertEquals(dialogFont.getName(), "dialog");
        
        // physical font
        assertNotNull(physicalFont);
        assertEquals(physicalFont.getSize(), 12);
        assertEquals(physicalFont.getStyle(), Font.PLAIN);
        assertEquals(physicalFont.getName(), "Arial");
        
    }

    /*
     * Test method for 'java.awt.Font.canDisplay(char)'
     */
    public final void testCanDisplayChar() {
        assertTrue(dialogFont.canDisplay('\u0020'));
        assertFalse(dialogFont.canDisplay('\uFFFF'));

        assertTrue(physicalFont.canDisplay('\u0020'));
        assertFalse(physicalFont.canDisplay('\uFFFF'));
    }

    /*
     * Test method for 'java.awt.Font.canDisplay(int)'
     */
    public final void testCanDisplayInt() {
        assertTrue(dialogFont.canDisplay((int)'\u0020'));
        assertFalse(dialogFont.canDisplay((int)'\uFFFF'));
        
        try{
            dialogFont.canDisplay(Integer.parseInt("10FFFF", 16) + 1);
            fail("IllegalArgumentException expected but wasn't thrown!");
        } catch (IllegalArgumentException e) {
            // expected
        }

        assertTrue(physicalFont.canDisplay((int)'\u0020'));
        assertFalse(physicalFont.canDisplay((int)'\uFFFF'));

        try{
            physicalFont.canDisplay(Integer.parseInt("10FFFF", 16) + 1);
            fail("IllegalArgumentException expected but wasn't thrown!");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /*
     * Test method for 'java.awt.Font.canDisplayUpTo(char[], int, int)'
     */
    public final void testCanDisplayUpToCharArrayIntInt() {
        char chars[] = new char[]{'1', 'q', '\u0434'};
        assertEquals(-1, physicalFont.canDisplayUpTo(chars, 0, chars.length));

        chars = new char[]{'1', '\u001F', '\u0434'};
        assertEquals(1, physicalFont.canDisplayUpTo(chars, 0, chars.length));

    }

    /*
     * Test method for 'java.awt.Font.canDisplayUpTo(CharacterIterator, int, int)'
     */
    public final void testCanDisplayUpToCharacterIteratorIntInt() {
        CharacterIterator iter = new StringCharacterIterator("1q\u0434");
        assertEquals(-1, physicalFont.canDisplayUpTo(iter, 0, iter.getEndIndex()));

        iter = new StringCharacterIterator("1\u001F\u0434");
        assertEquals(1, physicalFont.canDisplayUpTo(iter, 0, iter.getEndIndex()));
    }

    /*
     * Test method for 'java.awt.Font.canDisplayUpTo(String)'
     */
    public final void testCanDisplayUpToString() {
        String str = "1q\u0434";
        assertEquals(-1, physicalFont.canDisplayUpTo(str));

        str = "1\u001F\u0434";
        assertEquals(1, physicalFont.canDisplayUpTo(str));
    }

    /*
     * Test method for 'java.awt.Font.createGlyphVector(FontRenderContext, char[])'
     */
    public final void testCreateGlyphVectorFontRenderContextCharArray() {
        char chars[] = new char[]{'1', 'q', 0x0434};
        int goldenCodes[] = new int[]{20, 84, 606};
        GlyphVector gv = physicalFont.createGlyphVector(frc, chars); 

        assertNotNull( gv);
        
        assertEquals(3, gv.getNumGlyphs());
        assertEquals(frc, gv.getFontRenderContext());
        assertEquals(physicalFont, gv.getFont());
        
        assertEquals( goldenCodes[0], gv.getGlyphCode(0));
        assertEquals( goldenCodes[1], gv.getGlyphCode(1));
        assertEquals( goldenCodes[2], gv.getGlyphCode(2));
    }

    /*
     * Test method for 'java.awt.Font.createGlyphVector(FontRenderContext, CharacterIterator)'
     */
    public final void testCreateGlyphVectorFontRenderContextCharacterIterator() {
        CharacterIterator ci = new StringCharacterIterator("1q\u0434"); 
        int goldenCodes[] = new int[]{20, 84, 606};
        GlyphVector gv = physicalFont.createGlyphVector(frc, ci); 

        assertNotNull( gv);
        
        assertEquals(3, gv.getNumGlyphs());
        assertEquals(frc, gv.getFontRenderContext());
        assertEquals(physicalFont, gv.getFont());
        
        assertEquals( goldenCodes[0], gv.getGlyphCode(0));
        assertEquals( goldenCodes[1], gv.getGlyphCode(1));
        assertEquals( goldenCodes[2], gv.getGlyphCode(2));
    }

    /*
     * Test method for 'java.awt.Font.createGlyphVector(FontRenderContext, String)'
     */
    public final void testCreateGlyphVectorFontRenderContextString() {
        String str = "1q\u0434";
        int goldenCodes[] = new int[]{20, 84, 606};
        GlyphVector gv = physicalFont.createGlyphVector(frc, str); 

        assertNotNull( gv);
        assertEquals(frc, gv.getFontRenderContext());
        assertEquals(physicalFont, gv.getFont());
        
        assertEquals(3, gv.getNumGlyphs());
        
        assertEquals( goldenCodes[0], gv.getGlyphCode(0));
        assertEquals( goldenCodes[1], gv.getGlyphCode(1));
        assertEquals( goldenCodes[2], gv.getGlyphCode(2));
    }

    /*
     * Test method for 'java.awt.Font.deriveFont(AffineTransform)'
     */
    @SuppressWarnings("unchecked")
    public final void testDeriveFontAffineTransform() {
        AffineTransform transform = new AffineTransform(1, 1, 1, 1, 0, 0);
        Font derivedFont = physicalFont.deriveFont(transform);
        assertNotNull(derivedFont);

        // Font created from attributes
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>)physicalFont.getAttributes();
        attributes.put(TextAttribute.TRANSFORM, transform);
        Font font = new Font(attributes);
        assertEquals(font, derivedFont);
    }

    /*
     * Test method for 'java.awt.Font.deriveFont(float)'
     */
    public final void testDeriveFontFloat() {
        float size = 18;
        Font derivedFont = physicalFont.deriveFont(size);
        assertNotNull(derivedFont);
        assertEquals(new Font(physicalFont.getName(), 
                physicalFont.getStyle(), (int)size), derivedFont);

    }

    /*
     * Test method for 'java.awt.Font.deriveFont(int)'
     */
    public final void testDeriveFontInt() {
        int style = Font.ITALIC;
        Font derivedFont = physicalFont.deriveFont(style);
        assertNotNull(derivedFont);

        assertEquals(new Font(physicalFont.getName(), 
                style, physicalFont.getSize()), derivedFont);


    }

    /*
     * Test method for 'java.awt.Font.deriveFont(int, AffineTransform)'
     */
    @SuppressWarnings("unchecked")
    public final void testDeriveFontIntAffineTransform() {
        AffineTransform transform = new AffineTransform(1, 1, 1, 1, 0, 0);
        int style = Font.ITALIC;
        Font derivedFont = physicalFont.deriveFont(style, transform);
        assertNotNull(derivedFont);
        
        // Font created from attributes
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>)physicalFont.getAttributes();
        attributes.put(TextAttribute.TRANSFORM, transform);
        attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        
        Font font = new Font(attributes);
        assertEquals(font, derivedFont);
    }

    /*
     * Test method for 'java.awt.Font.deriveFont(int, float)'
     */
    public final void testDeriveFontIntFloat() {
        int style = Font.ITALIC;
        float size = 18;
        Font derivedFont = physicalFont.deriveFont(style, size);
        assertNotNull(derivedFont);
        
        Font font = new Font(physicalFont.getName(), style, (int)size);
        
        assertEquals(font, derivedFont);
    }

    /*
     * Test method for 'java.awt.Font.deriveFont(Map<? extends Attribute, ?>)'
     */
    public final void testDeriveFontMapOfQextendsAttributeQ() {
        Hashtable<TextAttribute, Object> attributes = new Hashtable<TextAttribute, Object>();
        attributes.put(TextAttribute.SIZE, new Float(20));
        attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        Font derivedFont = physicalFont.deriveFont(attributes);
        
        assertNotNull(derivedFont);
        
        assertEquals(new Font(physicalFont.getName(), Font.ITALIC | Font.BOLD, 20), derivedFont);
    }

    /*
     * Test method for 'java.awt.Font.equals(Object)'
     */
    public final void testEqualsObject() {
        assertTrue(physicalFont.equals(new Font("Arial", Font.PLAIN, 12)));
        assertFalse(physicalFont.equals(dialogFont));
    }

    /*
     * Test method for 'java.awt.Font.getAttributes()'
     */
    public final void testGetAttributes() {
        Map<TextAttribute, ?> attributes = physicalFont.getAttributes();

        // size
        assertEquals(physicalFont.getSize(), ((Float)attributes.get(TextAttribute.SIZE)).floatValue(), 0F);
        
        // style
        Float posture = (Float)attributes.get(TextAttribute.POSTURE);
        assertEquals(TextAttribute.POSTURE_REGULAR.doubleValue(), posture.doubleValue(), 0F);
        
        Float weight = (Float)attributes.get(TextAttribute.WEIGHT);
        assertEquals(TextAttribute.WEIGHT_REGULAR.floatValue(), weight.floatValue(), 0F);
        
        // family
        String family = (String)attributes.get(TextAttribute.FAMILY);
        assertEquals(ARIAL_NAME, family);
        
        // transform
        TransformAttribute trans = (TransformAttribute)attributes.get(TextAttribute.TRANSFORM);
        assertNotNull(trans);
        assertTrue(trans.isIdentity());

    }

    /*
     * Test method for 'java.awt.Font.getAvailableAttributes()'
     */
    public final void testGetAvailableAttributes() {
        ArrayList<TextAttribute> attributes = new ArrayList<TextAttribute>(7);
        attributes.add(TextAttribute.SIZE);
        attributes.add(TextAttribute.POSTURE);
        attributes.add(TextAttribute.WEIGHT);
        attributes.add(TextAttribute.FAMILY);
        attributes.add(TextAttribute.TRANSFORM);
        attributes.add(TextAttribute.SUPERSCRIPT);
        attributes.add(TextAttribute.WIDTH);
        Attribute[] attribs = physicalFont.getAvailableAttributes();
        assertNotNull(attribs);
        assertEquals(7, attribs.length);
        for (Attribute element : attribs) {
            assertTrue(element + " attribute missed", attributes.indexOf(element)!= -1);
        }

    }

    /*
     * Test method for 'java.awt.Font.getBaselineFor(char)'
     */
    public final void testGetBaselineFor() {
        assertEquals(Font.ROMAN_BASELINE, physicalFont.getBaselineFor('m'));
    }

    /*
     * Test method for 'java.awt.Font.getFamily()'
     */
    public final void testGetFamily() {
        assertEquals(ARIAL_NAME, physicalFont.getFamily());
    }

    /*
     * Test method for 'java.awt.Font.getFamily(Locale)'
     */
    public final void testGetFamilyLocale() {
        String familyName = physicalFont.deriveFont(Font.BOLD | Font.ITALIC).getFamily(Locale.GERMANY);
        assertNotNull(familyName);
        assertEquals(ARIAL_FACE_NAME, familyName);
    }

    /*
     * Test method for 'java.awt.Font.getFont(Map<? extends Attribute, ?>)'
     */
    public final void testGetFontMapOfQextendsAttributeQ() {
        // Font created from attributes
        Hashtable <Attribute, Object> attributes = new Hashtable <Attribute, Object>();
        attributes.put(TextAttribute.FAMILY, "Arial");
        attributes.put(TextAttribute.SIZE, new Float(12));
        attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        
        Font font = Font.getFont(attributes);
        assertNotNull(font);
        assertEquals(physicalFont, font);

        attributes = new Hashtable <Attribute, Object>();
        attributes.put(TextAttribute.FONT, new Font("Arial", Font.PLAIN, 12));
        attributes.put(TextAttribute.FAMILY, "dialog");
        attributes.put(TextAttribute.SIZE, new Float(18));
        attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        
        font = Font.getFont(attributes);
        assertNotNull(font);
        assertEquals(physicalFont, font);
        
        try{
            font = Font.getFont((Map<? extends TextAttribute,?>)null);
        } catch (Exception e) {
            // expected
        }
    }

    /*
     * Test method for 'java.awt.Font.getFont(String, Font)'
     */
    public final void testGetFontStringFont() {
        System.setProperty("arial.font", "Arial-BOLD-18");
        Font fnt = Font.getFont("arial.font");
        
        assertNotNull(fnt);
        assertEquals(new Font("Arial", Font.BOLD, 18), fnt);

        fnt = Font.getFont("non.existing.property", physicalFont);
        
        assertNotNull(fnt);
        assertEquals(physicalFont, fnt);

    }

    /*
     * Test method for 'java.awt.Font.getFont(String)'
     */
    public final void testGetFontString() {
        System.setProperty("arial.font", "Arial-BOLD-18");
        Font fnt = Font.getFont("arial.font");
        
        assertNotNull(fnt);
        assertEquals(new Font("Arial", Font.BOLD, 18), fnt);

        fnt = Font.getFont("non.existing.property");
        
        assertNull(fnt);
    }

    /*
     * Test method for 'java.awt.Font.getFontName()'
     */
    public final void testGetFontName() {
        assertEquals(ARIAL_FACE_NAME, physicalFont.getFontName());
    }

    /*
     * Test method for 'java.awt.Font.getStringBounds(CharacterIterator, int, int, FontRenderContext)'
     */
    public final void testGetStringBoundsCharacterIteratorIntIntFontRenderContext() {
        final int WIN_WIDTH = 237;
        String str = "toremove-This is a very long string for getting bounds!-toremove";
        CharacterIterator ci = new StringCharacterIterator(str);

        Rectangle2D rect = physicalFont.getStringBounds(ci, 9, 55, frc);
        LineMetrics lm = physicalFont.getLineMetrics(" ", frc);
        assertNotNull(rect);

        assertEquals(new Rectangle2D.Float(0, -lm.getAscent(), WIN_WIDTH, lm
                .getHeight()), rect);
    }

    /*
     * Test method for 'java.awt.Font.getStringBounds(String, FontRenderContext)'
     */
    public final void testGetStringBoundsStringFontRenderContext() {
        final int WIN_WIDTH = 237;

        String str = "This is a very long string for getting bounds!";
        Rectangle2D rect = physicalFont.getStringBounds(str, frc);
        LineMetrics lm = physicalFont.getLineMetrics(" ", frc);
        assertNotNull(rect);
        assertEquals( new Rectangle2D.Float(0, -lm.getAscent(), WIN_WIDTH, lm
                .getHeight()), rect);

    }

    /*
     * Test method for 'java.awt.Font.getStringBounds(String, int, int, FontRenderContext)'
     */
    public final void testGetStringBoundsStringIntIntFontRenderContext() {
        final int WIN_WIDTH = 237;

        String str = "toremove-This is a very long string for getting bounds!-toremove";
        Rectangle2D rect = physicalFont.getStringBounds(str, 9, 55, frc);
        LineMetrics lm = physicalFont.getLineMetrics(" ", frc);
        assertNotNull(rect);
        assertEquals(new Rectangle2D.Float(0, -lm.getAscent(), WIN_WIDTH, lm
                .getHeight()), rect);

    }

    /*
     * Test method for 'java.awt.Font.getStringBounds(char[], int, int, FontRenderContext)'
     */
    public final void testGetStringBoundsCharArrayIntIntFontRenderContext() {
        final int WIN_WIDTH = 237;

        String str = "toremove-This is a very long string for getting bounds!-toremove";
        char[] chars = str.toCharArray();
        Rectangle2D rect = physicalFont.getStringBounds(chars, 9, 55, frc);
        LineMetrics lm = physicalFont.getLineMetrics(" ", frc);
        assertNotNull(rect);
        assertEquals(new Rectangle2D.Float(0, -lm.getAscent(), WIN_WIDTH, lm
                .getHeight()), rect);
        
    }

    /*
     * Test method for 'java.awt.Font.layoutGlyphVector(FontRenderContext, char[], int, int, int)'
     */
    public final void testLayoutGlyphVector() {
        int goldenCodes[] = new int[]{606, 84, 20};
        char[] chars = (new String("1q\u0434")).toCharArray();
        GlyphVector gv = physicalFont.layoutGlyphVector(frc, chars, 0, chars.length, Font.LAYOUT_RIGHT_TO_LEFT);
        assertNotNull( gv);
        
        assertEquals(3, gv.getNumGlyphs());
        assertEquals(frc, gv.getFontRenderContext());
        assertEquals(physicalFont, gv.getFont());
        
        assertEquals( goldenCodes[0], gv.getGlyphCode(0));
        assertEquals( goldenCodes[1], gv.getGlyphCode(1));
        assertEquals( goldenCodes[2], gv.getGlyphCode(2));
    }

    /*
     * Test method for 'java.awt.Font.getName()'
     */
    public final void testGetName() {
        assertEquals(ARIAL_NAME, physicalFont.getName());

        assertEquals(DIALOG_NAME, dialogFont.getName());
    }
    
    /*
     * Test method for 'java.awt.Font.getPeer()'
     */
    @SuppressWarnings("deprecation")
    public final void testGetPeer() {
        assertNotNull(physicalFont.getPeer());
    }

    /*
     * Test method for 'java.awt.Font.getTransform()'
     */
    public final void testGetTransform() {
        assertEquals(new AffineTransform(), physicalFont.getTransform());
        
        Font trFont = physicalFont.deriveFont(AffineTransform.getScaleInstance(2, 2));
        assertEquals(AffineTransform.getScaleInstance(2, 2), trFont.getTransform());

    }

    /*
     * Test method for 'java.awt.Font.isTransformed()'
     */
    public final void testIsTransformed() {
        assertFalse(physicalFont.isTransformed());
        
        Font trFont = physicalFont.deriveFont(AffineTransform.getScaleInstance(2, 2));
        assertTrue(trFont.isTransformed());
    }

    /*
     * Test method for 'java.awt.Font.isPlain()'
     */
    public final void testIsPlain() {
        // plain style
        assertTrue(physicalFont.isPlain());

        // italic style
        Font itFont = physicalFont.deriveFont(Font.ITALIC);
        assertFalse(itFont.isPlain());

    }

    /*
     * Test method for 'java.awt.Font.isItalic()'
     */
    public final void testIsItalic() {
        // plain style
        assertFalse(physicalFont.isItalic());

        // italic style
        Font itFont = physicalFont.deriveFont(Font.ITALIC);
        assertTrue(itFont.isItalic());

        // bolditalic style
        Font itbdFont = physicalFont.deriveFont(Font.BOLD | Font.ITALIC);
        assertTrue(itbdFont.isItalic());
    }

    /*
     * Test method for 'java.awt.Font.isBold()'
     */
    public final void testIsBold() {
        // plain style
        assertFalse(physicalFont.isBold());

        // bold style
        Font bdFont = physicalFont.deriveFont(Font.BOLD);
        assertTrue(bdFont.isBold());

        // bolditalic style
        Font itbdFont = physicalFont.deriveFont(Font.BOLD | Font.ITALIC);
        assertTrue(itbdFont.isBold());

    }

    /*
     * Test method for 'java.awt.Font.hasUniformLineMetrics()'
     */
    public final void testHasUniformLineMetrics() {
         /*
          * !! Fails on RI. Seems bug in RI, spec says "If the logical Font
          * is a single font then the metrics would be uniform"
          */
        assertTrue(physicalFont.hasUniformLineMetrics());
    }

    /*
     * Test method for 'java.awt.Font.getStyle()'
     */
    public final void testGetStyle() {
        // plain style
        assertEquals(Font.PLAIN, physicalFont.getStyle());
        
        // italic style
        Font itFont = physicalFont.deriveFont(Font.ITALIC);
        assertEquals(Font.ITALIC, itFont.getStyle());
        
        // bold style
        Font bdFont = physicalFont.deriveFont(Font.BOLD);
        assertEquals(Font.BOLD, bdFont.getStyle());

        // bolditalic style
        Font itbdFont = physicalFont.deriveFont(Font.BOLD | Font.ITALIC);
        assertEquals(Font.ITALIC | Font.BOLD, itbdFont.getStyle());
    }

    /*
     * Test method for 'java.awt.Font.getSize()'
     */
    public final void testGetSize() {
        assertEquals(12, physicalFont.getSize());
    }

    /*
     * Test method for 'java.awt.Font.getMissingGlyphCode()'
     */
    public final void testGetMissingGlyphCode() {
        assertEquals(0, physicalFont.getMissingGlyphCode());
    }

    /*
     * Test method for 'java.awt.Font.getSize2D()'
     */
    public final void testGetSize2D() {
        assertEquals(physicalFont.getSize(), physicalFont.getSize2D(), .0);
    }

    /*
     * Test method for 'java.awt.Font.getItalicAngle()'
     */
    public final void testGetItalicAngle() {
        // plain font
        assertEquals(.0, physicalFont.getItalicAngle(), .0);
        
        Font itFont = physicalFont.deriveFont(Font.ITALIC); 
        assertEquals(ARIAL_ITALIC_ANGLE, 
                itFont.getItalicAngle(), 
                .0001);
    }

}
