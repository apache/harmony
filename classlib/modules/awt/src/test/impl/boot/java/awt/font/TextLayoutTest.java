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
/*
 * @author Oleg V. Khaschansky
 */

package java.awt.font;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.geom.*;
import java.text.AttributedString;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashMap;
import java.util.Map;

public class TextLayoutTest extends TestCase
{
    private final int width = 500;
    private final int height = 200;
    private final BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    TextLayout tl;

    String strings[] = new String [] {
        "String 1",
        "String 2",
        "String 3"
    };

    TextLayout equals[] = new TextLayout[strings.length];

    // For tl layout
    String s = "I TestItalic TestPlain I";
    Font f = new Font("times new roman", Font.ITALIC, 24);
    Font f1 = new Font("times new roman", Font.PLAIN, 60);
    FontRenderContext frc = ((Graphics2D) im.getGraphics()).getFontRenderContext();

    private final int layoutStartX = 1;
    private int layoutStartY;

    public TextLayoutTest(String name)
    {
        super(name);
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        AttributedString as = new AttributedString(s);
        as.addAttribute(TextAttribute.FONT, f, 0, 12 );
        as.addAttribute(TextAttribute.FONT, f1, 12, s.length());

        AttributedCharacterIterator aci = as.getIterator();
        tl = new TextLayout(aci, frc);

        // Init layouts
        for(int i=0; i<strings.length; i++) {
            as = new AttributedString(strings[i]);
            aci = as.getIterator();
            equals[i] = new TextLayout(aci, frc);
        }

        // To be able to measure advance and other metrics
        Graphics2D g2 = (Graphics2D) im.getGraphics();
        g2.setPaint(Color.WHITE);
        g2.fillRect(0, 0, im.getWidth(), im.getHeight());
        g2.setPaint(Color.BLACK);
        layoutStartY = (int) tl.getBounds().getHeight() + 1;
        tl.draw(g2, 1, layoutStartY);
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testHashCode() throws Exception
    {
        for(int i=0; i<strings.length; i++) {
            for(int j=0; j<strings.length; j++) {
                if(i == j) {
                    assertTrue("HashCode " + equals[i] + " " + equals[j], equals[i].hashCode() == equals[j].hashCode());
                } else {
                    assertTrue("HashCode " + equals[i] + " " + equals[j], equals[i].hashCode() != equals[j].hashCode());
                }
            }
        }
    }

    public void testClone() throws Exception
    {
        assertTrue(tl.equals(tl.clone()));
    }

    public void testEquals() throws Exception
    {
        for(int i=0; i<strings.length; i++) {
            for(int j=0; j<strings.length; j++) {
                if(i == j) {
                    assertTrue(equals[i].equals(equals[j]));
                } else {
                    assertFalse(equals[i].equals(equals[j]));
                }
            }
        }
    }

    public void testToString() throws Exception
    {
        tl.toString();
    }

    public void testDraw() throws Exception
    {
        tl.draw((Graphics2D) im.getGraphics(), 1, (float) tl.getBounds().getHeight() + 1);
    }

    public void testGetAdvance() throws Exception
    {
        int left = im.getWidth(), right = 0;

        for(int i=0; i<im.getWidth(); i++) {
            for(int j=0; j<im.getHeight(); j++) {
                if(im.getRGB(i,j) != 0xFFFFFFFF) {
                    left = Math.min(i, left);
                    right = Math.max(i, right);
                }
            }
        }
        assertEquals((int) tl.getAdvance(), right - left, 3);
    }

    public void testGetAscent() throws Exception
    {
        assertEquals((int) tl.getAscent(), (int) f1.getLineMetrics(s, frc).getAscent());
    }

    public void testGetBaseline() throws Exception
    {
        assertEquals(tl.getBaseline(), Font.ROMAN_BASELINE);
    }

    public void testGetBaselineOffsets() throws Exception
    {
        LineMetrics lm = f.getLineMetrics("A", frc);
        float correctBaselineOffsets[] = lm.getBaselineOffsets();
        float compatibleBaselineOffsets[] = {0, -9, -21};

        float baselineOffsets[] = tl.getBaselineOffsets();
        assertEquals((int) baselineOffsets[tl.getBaseline()], 0);

        for(int i=0; i<baselineOffsets.length; i++) {
            assertEquals(correctBaselineOffsets[i], baselineOffsets[i], 0.01);
            assertEquals(compatibleBaselineOffsets[i], baselineOffsets[i], 1);
        }
    }

    private int[] findFirstLetterBounds() {
        int left = im.getWidth(), top = im.getHeight(), right = 0, bottom = 0;

        boolean pointsStarted = false;
        for(int i=0; i<im.getWidth(); i++) {
            boolean hasPoint = false;
            for(int j=0; j<im.getHeight(); j++) {
                if(im.getRGB(i, j) != 0xFFFFFFFF) {
                    hasPoint = true;
                    pointsStarted = true;
                    left = Math.min(i, left);
                    right = Math.max(right, i);
                    top = Math.min(top, j);
                    bottom = Math.max(bottom, j);
                }
            }
            // Want to get only first letter
            if(!hasPoint && pointsStarted) {
                break;
            }
        }

        return new int[] {left, top, right, bottom};
    }

    public void testGetBlackBoxBounds() throws Exception
    {
        Shape bounds = tl.getBlackBoxBounds(2, 7);
        bounds = tl.getBlackBoxBounds(0, 1);
        Rectangle2D rect = bounds.getBounds2D();

        int letterBounds[] = findFirstLetterBounds();

        assertEquals(rect.getMinX() + layoutStartX, letterBounds[0], 1);
        assertEquals(rect.getMaxX() + layoutStartX, letterBounds[2], 1);
        assertEquals(rect.getMaxY() + layoutStartY, letterBounds[3], 1);
        assertEquals(rect.getMinY() + layoutStartY, letterBounds[1], 1);
    }

    public void testGetBounds() throws Exception
    {
        int left = im.getWidth(), right = 0, top = 0, bottom = im.getHeight();

        for(int i=0; i<im.getWidth(); i++) {
            for(int j=0; j<im.getHeight(); j++) {
                if(im.getRGB(i,j) != 0xFFFFFFFF) {
                    left = Math.min(i, left);
                    right = Math.max(i, right);
                    top = Math.max(j, top);
                    bottom = Math.min(j, bottom);
                }
            }
        }

        Rectangle2D rect = tl.getBounds();
        Rectangle2D intRect =
                new Rectangle2D.Float((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
        assertEquals(intRect.getX(), 0, 2);
        assertEquals(intRect.getY(), bottom-top, 2);
        assertEquals(intRect.getWidth(), right - left, 2);
        assertEquals(intRect.getHeight(), top - bottom, 2);
    }

    public void testGetCaretInfo() throws Exception
    {
        float cInfo[] = tl.getCaretInfo(TextHitInfo.beforeOffset(0));
        assertEquals(cInfo[0], 0f, 0.1f);
        assertEquals(cInfo[1], f.getItalicAngle(), 0.1f);

        cInfo = tl.getCaretInfo(TextHitInfo.afterOffset(s.length()));
        assertEquals(cInfo[0], tl.getAdvance(), 3);
        assertEquals(cInfo[1], 0, 0.1f);

        cInfo = tl.getCaretInfo(TextHitInfo.afterOffset(3));
        assertTrue(cInfo[0] > 0);
        assertTrue(cInfo[0] < tl.getAdvance());
        assertEquals(cInfo[1], f.getItalicAngle(), 0.1f);
    }

    public void testGetCaretInfo1() throws Exception
    {
        testGetCaretInfo(); // Same thing
    }

    public void testGetCaretShape() throws Exception
    {
        Shape cShape = tl.getCaretShape(TextHitInfo.trailing(0));

        int letterBounds[] = findFirstLetterBounds();

        Point2D.Float p1 = new Point2D.Float(letterBounds[2], letterBounds[1]); // Top right of first letter

        boolean started = false;
        int i;
        for(i=0; i<im.getWidth();i++) {
            if(im.getRGB(i, letterBounds[3]) != 0xFFFFFFFF) {
                started = true;
            } else {
                if(started) {
                    break;
                }
            }
        }
        int bottomRight = i - 1;

        Point2D.Float p2 = new Point2D.Float(bottomRight, letterBounds[3]); // Bottom right of first letter

        Rectangle2D rect1 = new Rectangle2D.Double(p1.getX()-3, p1.getY(), 3, 2);
        Rectangle2D rect2 = new Rectangle2D.Double(p2.getX()+3, p2.getY(), 3, 2);

        cShape = AffineTransform.getTranslateInstance(layoutStartX, layoutStartY).createTransformedShape(cShape);

        Rectangle2D bounds = cShape.getBounds2D();
        Point2D.Double pShape1 = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
        Point2D.Double pShape2 = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
        Line2D cursor = new Line2D.Double(pShape1, pShape2);

        assertTrue(rect1.intersectsLine(cursor));
        assertTrue(rect2.intersectsLine(cursor));

    }

    public void testGetCaretShape1() throws Exception
    {
        testGetCaretShape(); // Same
    }

    public void testGetCaretShapes() throws Exception
    {
        Shape[] shapes = tl.getCaretShapes(1);
        assertNull(shapes[1]);

        PathIterator it1 = shapes[0].getPathIterator(null);
        PathIterator it2 = tl.getCaretShape(TextHitInfo.trailing(0)).getPathIterator(null);

        while(!it1.isDone()) {
            float arr1[] = new float[6];
            float arr2[] = new float[6];
            int seg1 = it1.currentSegment(arr1);
            int seg2 = it2.currentSegment(arr2);

            assertEquals(seg1, seg2);
            for (int i = 0; i < arr2.length; i++) {
                assertEquals(arr1[i], arr2[i], 0.01);
            }

            it1.next();
            it2.next();
        }
    }

    public void testGetCaretShapes1() throws Exception
    {
        testGetCaretShapes();
    }

    public void testGetCaretShapes2() throws Exception
    {
        testGetCaretShapes();
    }

    public void testGetCharacterCount() throws Exception
    {
        assertEquals(tl.getCharacterCount(), s.length());
    }

    public void testGetCharacterLevel() throws Exception
    {
        assertEquals(tl.getCharacterLevel(5), 0);
    }

    public void testGetDescent() throws Exception
    {
        assertEquals((int) tl.getDescent(), (int) f1.getLineMetrics(s, frc).getDescent());
    }

    public void testGetJustifiedLayout() throws Exception
    {
        TextLayout j1 = tl.getJustifiedLayout(500);
        TextLayout j2 = tl.getJustifiedLayout(200);

        assertNotNull(j1);
        assertNotNull(j2);

        assertEquals(500, j1.getAdvance(), 7);
        assertEquals(200, j2.getAdvance(), 7);
    }

    public void testGetLeading() throws Exception
    {
        assertEquals((int) tl.getLeading(), (int) f1.getLineMetrics(s, frc).getLeading());
    }

    public void testGetLogicalHighlightShape() throws Exception
    {
        Shape highlight = tl.getLogicalHighlightShape(7,3);
        Rectangle2D bbb = tl.getBlackBoxBounds(3, 7).getBounds2D();
        Rectangle2D bounds = highlight.getBounds2D();

        bounds.add(bounds.getMaxX()+2, bounds.getMaxY());
        assertTrue(bounds.contains(bbb));

        Rectangle2D smallBounds =
                new Rectangle2D.Double(bounds.getX() + 3, bounds.getY(), bounds.getWidth() - 4, bounds.getHeight());
        assertFalse(smallBounds.contains(bbb));

        assertEquals(tl.getAscent()+tl.getDescent()+tl.getLeading(), bounds.getHeight(), 5);
    }

    public void testGetLogicalHighlightShape1() throws Exception
    {
        Shape highlight = tl.getLogicalHighlightShape(7, 3, tl.getBounds());
        Rectangle2D bbb = tl.getBlackBoxBounds(3, 7).getBounds2D();
        Rectangle2D bounds = highlight.getBounds2D();

        bounds.add(bounds.getMaxX()+2, bounds.getMaxY());
        assertTrue(bounds.contains(bbb));

        Rectangle2D smallBounds =
                new Rectangle2D.Double(bounds.getX() + 3, bounds.getY(), bounds.getWidth() - 4, bounds.getHeight());
        assertFalse(smallBounds.contains(bbb));

        assertEquals(tl.getBounds().getHeight(), bounds.getHeight(), 0);
    }

    public void testGetLogicalRangesForVisualSelection() throws Exception
    {
        TextHitInfo i1 = TextHitInfo.leading(0);
        TextHitInfo i2 = TextHitInfo.leading(4);
        TextHitInfo i3 = TextHitInfo.trailing(9);

        int res1[] = tl.getLogicalRangesForVisualSelection(i1, i2);
        int res2[] = tl.getLogicalRangesForVisualSelection(i2, i1);

        assertEquals(2, res1.length);
        assertEquals(2, res2.length);

        assertEquals(0, res1[0]);
        assertEquals(4, res1[1]);

        assertEquals(res1[0], res2[0]);
        assertEquals(res1[1], res2[1]);

        int res3[] = tl.getLogicalRangesForVisualSelection(i3, i2);

        assertEquals(2, res3.length);
        assertEquals(4, res3[0]);
        assertEquals(10, res3[1]);
    }

    public void testGetNextLeftHit() throws Exception
    {
        TextHitInfo i1 = tl.getNextLeftHit(0);
        assertNull(i1);

        TextHitInfo i2 = tl.getNextLeftHit(4);
        assertEquals(3, i2.getCharIndex());
        assertTrue(i2.isLeadingEdge());
    }

    public void testGetNextLeftHit1() throws Exception
    {
        TextHitInfo i1 = tl.getNextLeftHit(TextHitInfo.leading(0));
        assertNull(i1);

        TextHitInfo i2 = tl.getNextLeftHit(TextHitInfo.trailing(3));
        assertEquals(3, i2.getCharIndex());
        assertTrue(i2.isLeadingEdge());
    }

    public void testGetNextLeftHit2() throws Exception
    {
        TextHitInfo i1 = tl.getNextLeftHit(0, TextLayout.DEFAULT_CARET_POLICY);
        assertNull(i1);

        TextHitInfo i2 = tl.getNextLeftHit(4, TextLayout.DEFAULT_CARET_POLICY);
        assertEquals(3, i2.getCharIndex());
        assertTrue(i2.isLeadingEdge());
    }

    public void testGetNextRightHit() throws Exception
    {
        TextHitInfo i1 = tl.getNextRightHit(tl.getCharacterCount());
        assertNull(i1);

        TextHitInfo i2 = tl.getNextRightHit(4);
        assertEquals(5, i2.getCharIndex());
        assertTrue(i2.isLeadingEdge());
    }

    public void testGetNextRightHit1() throws Exception
    {
        TextHitInfo i1 = tl.getNextRightHit(TextHitInfo.trailing(tl.getCharacterCount()-1));
        assertNull(i1);

        TextHitInfo i2 = tl.getNextRightHit(TextHitInfo.leading(4));
        assertEquals(5, i2.getCharIndex());
        assertTrue(i2.isLeadingEdge());
    }

    public void testGetNextRightHit2() throws Exception
    {
        TextHitInfo i1 = tl.getNextRightHit(tl.getCharacterCount(), TextLayout.DEFAULT_CARET_POLICY);
        assertNull(i1);

        TextHitInfo i2 = tl.getNextRightHit(4, TextLayout.DEFAULT_CARET_POLICY);
        assertEquals(5, i2.getCharIndex());
        assertTrue(i2.isLeadingEdge());
    }

    public void testGetOutline() throws Exception
    {
        Shape outline1 = equals[0].getOutline(null);
        assertFalse(outline1.getBounds2D().getWidth() == 0);

        Shape outline = tl.getOutline(AffineTransform.getTranslateInstance(layoutStartX, layoutStartY));
        PathIterator pi = outline.getPathIterator(null);

        while(!pi.isDone()) {
            double seg[] = new double[6];
            int segType = pi.currentSegment(seg);

            if(segType != PathIterator.SEG_CLOSE) {
                int x = 1, y = 1;
                switch(segType) {
                    case PathIterator.SEG_LINETO:
                    case PathIterator.SEG_MOVETO:
                        x += (int) seg[0];
                        y += (int) seg[1];
                        break;
                    case PathIterator.SEG_QUADTO:
                        //x += (int) seg[2];
                        //y += (int) seg[3];
                        //break;
                        pi.next();
                        continue;
                    case PathIterator.SEG_CUBICTO:
                        //x += (int) seg[4];
                        //y += (int) seg[5];
                        //break;
                        pi.next();
                        continue;
                }

                x = Math.max(x,1);
                y = Math.max(y,1);

                //System.out.println("x= " + x + "; y= "+ y + " Seg type= " + segType);

                // Now check if outlines are close enough to real letters.
                boolean passed = false;

                for(int dx=-3; dx<4; dx++) {
                    for(int dy=-2; dy<3; dy++) {
                        if(x+dx < 1 || x+dx > im.getWidth()) {
                            continue;
                        }
                        if(y+dy < 1 || y+dy > im.getHeight()) {
                            continue;
                        }

                        if(im.getRGB(x+dx, y+dy) != 0xFFFFFFFF) {
                            passed = true;
                            break;
                        }
                    }
                }

                if(!passed) {
                    fail("Outline contains point located too far from glyph");
                }
            }

            pi.next();
        }
    }

    public void testGetVisibleAdvance() throws Exception
    {
        assertEquals(tl.getBounds().getWidth(), tl.getVisibleAdvance(), 1f);
    }

    public void testGetVisualHighlightShape() throws Exception
    {
        Shape highlight = tl.getVisualHighlightShape(TextHitInfo.trailing(7), TextHitInfo.leading(3));
        Rectangle2D bbb = tl.getBlackBoxBounds(3, 7).getBounds2D();
        Rectangle2D bounds = highlight.getBounds2D();

        bounds.add(bounds.getMaxX()+2, bounds.getMaxY());
        assertTrue(bounds.contains(bbb));

        Rectangle2D smallBounds =
                new Rectangle2D.Double(bounds.getX() + 3, bounds.getY(), bounds.getWidth() - 4, bounds.getHeight());
        assertFalse(smallBounds.contains(bbb));

        assertEquals(tl.getAscent()+tl.getDescent()+tl.getLeading(), bounds.getHeight(), 5);
    }

    public void testGetVisualHighlightShape1() throws Exception
    {
        Shape highlight = tl.getVisualHighlightShape(TextHitInfo.trailing(7), TextHitInfo.leading(3), tl.getBounds());
        Rectangle2D bbb = tl.getBlackBoxBounds(3, 7).getBounds2D();
        Rectangle2D bounds = highlight.getBounds2D();

        bounds.add(bounds.getMaxX()+2, bounds.getMaxY());
        assertTrue(bounds.contains(bbb));

        Rectangle2D smallBounds =
                new Rectangle2D.Double(bounds.getX() + 3, bounds.getY(), bounds.getWidth() - 4, bounds.getHeight());
        assertFalse(smallBounds.contains(bbb));

        assertEquals(tl.getBounds().getHeight(), bounds.getHeight(), 0);
    }

    public void testGetVisualOtherHit() throws Exception
    {
        TextHitInfo i1 = tl.getVisualOtherHit(TextHitInfo.leading(3));
        assertEquals(TextHitInfo.trailing(2), i1);

        TextHitInfo i2 = tl.getVisualOtherHit(TextHitInfo.leading(0));
        assertEquals(TextHitInfo.trailing(-1), i2);

        TextHitInfo i3 = tl.getVisualOtherHit(TextHitInfo.trailing(tl.getCharacterCount()-1));
        assertEquals(TextHitInfo.leading(tl.getCharacterCount()), i3);
    }

    public void testHandleJustify() throws Exception
    {
        tl.handleJustify(500);
        assertEquals(500, tl.getAdvance(), 7);

        try {
            tl.handleJustify(200);
            fail("Handle justify should throw an exception");
        } catch(IllegalStateException e) {
        }

        assertEquals(500, tl.getAdvance(), 7);
    }

    public void testHitTestChar() throws Exception
    {
        Rectangle2D bounds = tl.getBlackBoxBounds(3, 4).getBounds2D();

        TextHitInfo i1 = tl.hitTestChar((float) bounds.getCenterX()+2, (float) bounds.getCenterY()+2, tl.getBounds());
        assertEquals(TextHitInfo.trailing(3), i1);

        TextHitInfo i2 = tl.hitTestChar(-20, 10, tl.getBounds());
        assertEquals(TextHitInfo.leading(0), i2);

        TextHitInfo i3 = tl.hitTestChar(700, 10, tl.getBounds());
        assertEquals(TextHitInfo.trailing(tl.getCharacterCount()-1), i3);
    }

    public void testHitTestChar1() throws Exception
    {
        Rectangle2D bounds = tl.getBlackBoxBounds(3, 4).getBounds2D();

        TextHitInfo i1 = tl.hitTestChar((float) bounds.getCenterX()+2, (float) bounds.getCenterY()+2);
        assertEquals(TextHitInfo.trailing(3), i1);

        TextHitInfo i2 = tl.hitTestChar(-20, 10);
        assertEquals(TextHitInfo.leading(0), i2);

        TextHitInfo i3 = tl.hitTestChar(700, 10);
        assertEquals(TextHitInfo.trailing(tl.getCharacterCount()-1), i3);
    }

    public void testIsLeftToRight() throws Exception
    {
        assertTrue(tl.isLeftToRight());
    }

    public void testIsVertical() throws Exception
    {
        assertFalse(tl.isVertical());
    }

    public void testGetStrongCaret() throws Exception
    {
        TextHitInfo i = TextLayout.DEFAULT_CARET_POLICY.getStrongCaret(TextHitInfo.trailing(4), TextHitInfo.leading(5), tl);
        assertEquals(TextHitInfo.leading(5), i);
    }

    public void testTextLayoutConstructorConstraints() throws Exception{
        // regression test for Harmony-1464
        try{
            new TextLayout(null, (Font)null, null);
        } catch (IllegalArgumentException e) {
                // expected
        }

        try{
            new TextLayout(null, f, null);
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        try{
            new TextLayout("", f, null);
        } catch (IllegalArgumentException e) {
            // expected
        }

        try{
            new TextLayout("aa", f, null);
        } catch (NullPointerException e) {
            // expected
        }

        try{
            new TextLayout(null, null);
        } catch (IllegalArgumentException e) {
            // expected
        }

        AttributedString as = new AttributedString("test");
        as.addAttribute(TextAttribute.FONT, f, 0, 2 );

        try{
            new TextLayout(as.getIterator(), null);
        } catch (NullPointerException e) {
            // expected
        }
        
        try {
            new TextLayout(null, (Map<? extends Attribute,?>)null, (FontRenderContext) null);
        } catch (IllegalArgumentException e) {
            System.out.println("success: " + e.getMessage());
            // as expected
        }

        try {
            new TextLayout(null, new HashMap<Attribute, Object>(), (FontRenderContext) null);
        } catch (IllegalArgumentException e) {
            // as expected
        }

        try {
            new TextLayout("aa", new HashMap<Attribute, Object>(), (FontRenderContext) null);
        } catch (NullPointerException e) {
            // as expected
        }

        
        try{
            new TextLayout("", new HashMap<Attribute, Object>(), (FontRenderContext) null);
        } catch (IllegalArgumentException e) {
            // as expected
            System.out.println("success: " + e.getMessage());
        }
        
    }

    public static Test suite()
    {
        return new TestSuite(TextLayoutTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TextLayoutTest.class);
    }
}
