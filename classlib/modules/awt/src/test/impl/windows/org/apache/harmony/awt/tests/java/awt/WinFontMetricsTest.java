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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import junit.framework.TestCase;

/*
 *  This test is valid only on Windows!
 */

public class WinFontMetricsTest extends TestCase {
    Font physicalFont;
    Font dialogFont;
    FontRenderContext frc = new FontRenderContext(null, false, false);
    Graphics g;
    FontMetrics fm;
    
    @SuppressWarnings("deprecation")
    @Override
    public void setUp() throws Exception{
        super.setUp();
        physicalFont = new Font("Arial", Font.PLAIN, 12);
        dialogFont = new Font("dialog", Font.PLAIN, 12);
        
        fm = Toolkit.getDefaultToolkit().getFontMetrics(physicalFont);
        g = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB).getGraphics();
        g.setFont(physicalFont);
    }

    /*
     * Test method for 'java.awt.FontMetrics.FontMetrics(Font)'
     */
    @SuppressWarnings("deprecation")
    public final void testFontMetrics() {
        FontMetrics fMetrics = Toolkit.getDefaultToolkit().getFontMetrics(physicalFont);
        assertNotNull(fMetrics);
    }

    /*
     * Test method for 'java.awt.FontMetrics.getFont()'
     */
    public final void testGetFont() {
        assertEquals(physicalFont, fm.getFont());
    }

    /*
     * Test method for 'java.awt.FontMetrics.getHeight()'
     */
    public final void testGetHeight() {
        int height = 15; 
        assertEquals(height, fm.getHeight());
    }

    /*
     * Test method for 'java.awt.FontMetrics.getAscent()'
     */
    public final void testGetAscent() {
        int ascent = 12; 
        assertEquals(ascent, fm.getAscent());
    }

    /*
     * Test method for 'java.awt.FontMetrics.getDescent()'
     */
    public final void testGetDescent() {
        int descent = 3; 
        assertEquals(descent, fm.getDescent());
    }

    /*
     * Test method for 'java.awt.FontMetrics.getLeading()'
     */
    public final void testGetLeading() {
        int leading = 0; 
        assertEquals(leading, fm.getLeading());
    }

    /*
     * Test method for 'java.awt.FontMetrics.getLineMetrics(CharacterIterator, int, int, Graphics)'
     */
    public final void testGetLineMetricsCharacterIteratorIntIntGraphics() {
        CharacterIterator ci = new StringCharacterIterator("Only Hello world! metrics");

        LineMetrics lm = fm.getLineMetrics(ci, 5, 17, g);

        lmEquals(physicalFont.getLineMetrics(ci, 5, 17, ((Graphics2D)g).getFontRenderContext()), lm);
    }

    /*
     * Test method for 'java.awt.FontMetrics.getLineMetrics(String, Graphics)'
     */
    public final void testGetLineMetricsStringGraphics() {
        String str = "Hello world!";
        LineMetrics lm = fm.getLineMetrics(str, g);

        lmEquals(physicalFont.getLineMetrics(str, ((Graphics2D)g).getFontRenderContext()), lm);
    }

    /*
     * Test method for 'java.awt.FontMetrics.getLineMetrics(char[], int, int, Graphics)'
     */
    public final void testGetLineMetricsCharArrayIntIntGraphics() {
        char[] chars = new char[]{'H','e','l','l','o',' ','w','o','r','l','d','!'};

        LineMetrics lm = fm.getLineMetrics(chars, 0, chars.length, g);

        lmEquals(physicalFont.getLineMetrics(chars, 0, chars.length, ((Graphics2D) g)
                .getFontRenderContext()), lm);

    }

    /*
     * Test method for 'java.awt.FontMetrics.getLineMetrics(String, int, int, Graphics)'
     */
    public final void testGetLineMetricsStringIntIntGraphics() {
        String str = "Only Hello world! metrics";

        LineMetrics lm = fm.getLineMetrics(str, 5, 17, g);

        lmEquals(physicalFont.getLineMetrics(str, 5, 17, ((Graphics2D) g)
                .getFontRenderContext()), lm);

    }

    /*
     * Test method for 'java.awt.FontMetrics.getStringBounds(CharacterIterator, int, int, Graphics)'
     */
    public final void testGetStringBoundsCharacterIteratorIntIntGraphics() {
        String str = "toremove-This is a very long string for getting bounds!-toremove";
        int width = 237;
        CharacterIterator ci = new StringCharacterIterator(str);

        Rectangle2D rect = fm.getStringBounds(ci, 9, 55, g);
        LineMetrics lm = fm.getLineMetrics(" ", g);
        assertNotNull(rect);
        assertEquals(rect, new Rectangle2D.Float(0, -lm.getAscent(), width, lm.getHeight()));
        
        try{
            rect = fm.getStringBounds(ci, 10, 155, g);
            fail("IndexOutOfBoundsException wasn't thrown, when end index is more than count of elements in CharacterIterator");
        } catch (IndexOutOfBoundsException e){
            // expected
        }
        
        try{
            rect = fm.getStringBounds(ci, -1, 55, g);
            fail("IndexOutOfBoundsException wasn't thrown, when initial offset < 0");
        } catch (IndexOutOfBoundsException e){
            // expected
        }

        try{
            rect = fm.getStringBounds(ci, 45, 40, g);
            fail("IndexOutOfBoundsException wasn't thrown, when end index is lesser than start index");
        } catch (IndexOutOfBoundsException e){
            // expected
        }


    }

    /*
     * Test method for 'java.awt.FontMetrics.getStringBounds(String, int, int, Graphics)'
     */
    public final void testGetStringBoundsStringIntIntGraphics() {
        String str = "toremove-This is a very long string for getting bounds!-toremove";
        int width = 237;
        Rectangle2D rect = fm.getStringBounds(str, 9, 55, g);
        LineMetrics lm = fm.getLineMetrics(" ", g);
        assertNotNull(rect);
        assertEquals(rect, new Rectangle2D.Float(0, -lm.getAscent(), width, lm.getHeight()));

        try{
            rect = fm.getStringBounds(str, 10, 65, g);
            fail("IndexOutOfBoundsException wasn't thrown, when end index is more than number of chars in string");
        } catch (IndexOutOfBoundsException e){
            // expected
        }
        
        try{
            rect = fm.getStringBounds(str, -1, 55, g);
            fail("IndexOutOfBoundsException wasn't thrown, when initial offset < 0");
        } catch (IndexOutOfBoundsException e){
            // expected
        }

        try{
            rect = fm.getStringBounds(str, 45, 10, g);
            fail("IndexOutOfBoundsException wasn't thrown, when end index is less than start index");
        } catch (IndexOutOfBoundsException e){
            // expected
        }
    }

    /*
     * Test method for 'java.awt.FontMetrics.getStringBounds(char[], int, int, Graphics)'
     */
    public final void testGetStringBoundsCharArrayIntIntGraphics() {
        String str = "toremove-This is a very long string for getting bounds!-toremove";
        char[] chars = str.toCharArray();
        int width = 237;
        Rectangle2D rect = fm.getStringBounds(chars, 9, 55, g);
        LineMetrics lm = fm.getLineMetrics(" ", g);
        assertNotNull(rect);
        assertEquals(rect, new Rectangle2D.Float(0, -lm.getAscent(), width, lm.getHeight()));
        
        try{
            rect = fm.getStringBounds(chars, 10, 155, g);
            fail("IndexOutOfBoundsException wasn't thrown, when end index is more than count of elements in CharacterIterator");
        } catch (IndexOutOfBoundsException e){
            // expected
        }
        
        try{
            rect = fm.getStringBounds(chars, -1, 55, g);
            fail("IndexOutOfBoundsException wasn't thrown, when initial offset < 0");
        } catch (IndexOutOfBoundsException e){
            // expected
        }

        try{
            rect = fm.getStringBounds(chars, 45, 40, g);
            fail("IndexOutOfBoundsException wasn't thrown, when end index is less than start index");
        } catch (IndexOutOfBoundsException e){
            // expected
        }
    }

    /*
     * Test method for 'java.awt.FontMetrics.getStringBounds(String, Graphics)'
     */
    public final void testGetStringBoundsStringGraphics() {
        String str = "This is a very long string for getting bounds!";
        int width = 237;
        Rectangle2D rect = fm.getStringBounds(str, g);
        LineMetrics lm = fm.getLineMetrics(" ", g);
        assertNotNull(rect);
        assertEquals(new Rectangle2D.Float(0, -lm.getAscent(), width, lm.getHeight()), rect);
    }

    /*
     * Test method for 'java.awt.FontMetrics.hasUniformLineMetrics()'
     */
    public final void testHasUniformLineMetrics() {
        // !! fails on RI, seems it is a bug in RI, see the spec
        assertTrue(fm.hasUniformLineMetrics());
    }

    /*
     * Test method for 'java.awt.FontMetrics.bytesWidth(byte[], int, int)'
     */
    public final void testBytesWidth() {
        byte[] chars = new byte[]{'M','P'};
        int width = fm.bytesWidth(chars, 0, 2);
        assertEquals(17, width);

        assertEquals(9, fm.bytesWidth(chars, 0, 1));
    }

    /*
     * Test method for 'java.awt.FontMetrics.charsWidth(char[], int, int)'
     */
    public final void testCharsWidth() {
        char[] chars = new char[]{'M','P'};
        int width = fm.charsWidth(chars, 0, 2);
        assertEquals(17, width);

        assertEquals(9, fm.charsWidth(chars, 0, 1));
    }

    /*
     * Test method for 'java.awt.FontMetrics.charWidth(int)'
     */
    public final void testCharWidthInt() {
        int widthOf_a = 7;
        int chr = 'a';
        assertEquals(widthOf_a, fm.charWidth(chr));
        chr = '\u0003';
        int defaultWidth = 9;
        assertEquals(defaultWidth, fm.charWidth(chr));

    }

    /*
     * Test method for 'java.awt.FontMetrics.charWidth(char)'
     */
    public final void testCharWidthChar() {
        int widthOf_a = 7;
        char chr = 'a';
        assertEquals(widthOf_a, fm.charWidth(chr));
        chr = '\u0003';
        int defaultWidth = 9;
        assertEquals(defaultWidth, fm.charWidth(chr));

    }

    /*
     * Test method for 'java.awt.FontMetrics.getMaxAscent()'
     */
    public final void testGetMaxAscent() {
        int maxAscent = 12;
        assertEquals(maxAscent, fm.getMaxAscent());
    }

    /*
     * Test method for 'java.awt.FontMetrics.getMaxDecent()'
     */
    @SuppressWarnings("deprecation")
    public final void testGetMaxDecent() {
        int maxDecent = 3;
        assertEquals(maxDecent, fm.getMaxDecent());
    }

    /*
     * Test method for 'java.awt.FontMetrics.getMaxDescent()'
     */
    public final void testGetMaxDescent() {
        int maxDescent = 3;
        assertEquals(maxDescent, fm.getMaxDescent());
    }

    /*
     * Test method for 'java.awt.FontMetrics.getWidths()'
     */
    public final void testGetWidths() {
        int[] widths;
        int[] values = new int[]{3,3,4,7,7,11,8,2,4,4,
                        5,7,3,4,3,3,7,7,7,7};
        widths = fm.getWidths();
        
        assertNotNull(widths);
        assertEquals(widths.length, 256);
        
        for (int i = 0x20; i < 0x34; i++){
            assertEquals("widths[" + i + "]=" + widths[i]+ "!=" +values[i-0x20], widths[i], values[i-0x20]);
        }
        
    }

    /*
     * Test method for 'java.awt.FontMetrics.stringWidth(String)'
     */
    public final void testStringWidth() {
        int width = 131;
        String str = "Hello world string width!";
        assertEquals(width, fm.stringWidth(str));

    }
    
    @SuppressWarnings("boxing")
    private boolean lmEquals(LineMetrics lm1, LineMetrics lm2){
        assertEquals("Ascent", lm1.getAscent(), lm2.getAscent());
        assertEquals("Baseline Index", lm1.getBaselineIndex(), lm2.getBaselineIndex());
        float[] offsets = lm2.getBaselineOffsets(); 
        assertNotNull(offsets);
        for (int i=0; i < offsets.length; i++){
            assertEquals("Baseline offset[" + i + "]", lm1.getBaselineOffsets()[i], offsets[i]);
        }
        assertEquals("Descent", lm1.getDescent(), lm2.getDescent());
        assertEquals("Height", lm1.getHeight(), lm2.getHeight());
        assertEquals("Leading", lm1.getLeading(), lm2.getLeading());
        assertEquals("NumChars", lm1.getNumChars(), lm2.getNumChars());
        assertEquals("Strikethrough offset", lm1.getStrikethroughOffset(), lm2.getStrikethroughOffset());
        assertEquals("Strikethrough thickness", lm1.getStrikethroughThickness(), lm2.getStrikethroughThickness());
        assertEquals("Underline offset", lm1.getUnderlineOffset(), lm2.getUnderlineOffset());
        assertEquals("Underline thickness", lm1.getUnderlineThickness(), lm2.getUnderlineThickness());

        return true;
    }
}
