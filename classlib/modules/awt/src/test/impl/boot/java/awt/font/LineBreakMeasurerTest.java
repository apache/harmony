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
import java.text.AttributedString;
import java.text.AttributedCharacterIterator;

public class LineBreakMeasurerTest extends TestCase
{
    LineBreakMeasurer measurer;

    private final int width = 500;
    private final int height = 200;
    private final BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    private final String s = "I TestItalic TestPlain I";
    private final Font f = new Font("times new roman", Font.ITALIC, 24);
    private final Font f1 = new Font("serif", Font.PLAIN, 60);
    private final FontRenderContext frc = ((Graphics2D) im.getGraphics()).getFontRenderContext();

    public LineBreakMeasurerTest(String name)
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

        measurer = new LineBreakMeasurer(aci, frc);
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testDeleteChar() throws Exception
    {
        String s1 = "I TestItalic estPlain I";

        AttributedString as = new AttributedString(s1);
        as.addAttribute(TextAttribute.FONT, f, 0, 12 );
        as.addAttribute(TextAttribute.FONT, f1, 12, s1.length());
        AttributedCharacterIterator aci = as.getIterator();

        int offset = measurer.nextOffset(30); // This won't change

        measurer.setPosition(12);
        int offset1 = measurer.nextOffset(500); // And this should change

        measurer.deleteChar(aci, 13);
        assertEquals(0, measurer.getPosition());

        measurer.setPosition(0);
        assertEquals(offset, measurer.nextOffset(30));

        measurer.setPosition(12);
        assertFalse(offset1 == measurer.nextOffset(500));
    }

    public void testGetPosition() throws Exception
    {
        assertEquals(0, measurer.getPosition());
        measurer.nextLayout(1000, 5, false);
        assertEquals(5, measurer.getPosition());
    }

    public void testInsertChar() throws Exception
    {
        int offset = measurer.nextOffset(30); // This won't change
        measurer.setPosition(12);
        int offset1 = measurer.nextOffset(500); // And this should change

        String s1 = "I TestItalic WTestPlain I";
        AttributedString as = new AttributedString(s1);
        as.addAttribute(TextAttribute.FONT, f, 0, 12 );
        as.addAttribute(TextAttribute.FONT, f1, 12, s1.length());
        AttributedCharacterIterator aci = as.getIterator();

        measurer.insertChar(aci, 13);
        assertEquals(0, measurer.getPosition());

        measurer.setPosition(0);
        assertEquals(offset, measurer.nextOffset(30));

        measurer.setPosition(12);
        assertFalse(offset1 == measurer.nextOffset(500));
    }

    public void testNextLayout() throws Exception
    {
        TextLayout l1 = measurer.nextLayout(100);
        TextLayout l2 = measurer.nextLayout(100);
        TextLayout l3 = measurer.nextLayout(500);
        TextLayout l4 = measurer.nextLayout(500);

        assertEquals(2, l1.getCharacterCount());
        assertEquals(11, l2.getCharacterCount());
        assertEquals(11, l3.getCharacterCount());
        assertNull(l4);
    }

    public void testNextLayout1() throws Exception
    {
        TextLayout l1 = measurer.nextLayout(100, 5, false);
        TextLayout l2 = measurer.nextLayout(15, 20, true);
        TextLayout l3 = measurer.nextLayout(600, 20, true);
        TextLayout l4 = measurer.nextLayout(500, 21, false);
        TextLayout l5 = measurer.nextLayout(500, 25, false);

        assertEquals(2, l1.getCharacterCount());
        assertNull(l2);
        assertEquals(18, l3.getCharacterCount());
        assertEquals(1, l4.getCharacterCount());
        assertEquals(3, l5.getCharacterCount());
    }

    public void testNextOffset() throws Exception
    {
        int o1 = measurer.nextOffset(40);
        int o2 = measurer.nextOffset(60);
        measurer.setPosition(o1);
        int o3 = measurer.nextOffset(60);
        measurer.setPosition(o3);
        int o4 = measurer.nextOffset(60);
        measurer.setPosition(o4);
        int o5 = measurer.nextOffset(500);

        assertEquals(2, o1);
        assertEquals(2, o2);
        assertEquals(8, o3);
        assertEquals(13, o4);
        assertEquals(24, o5);
    }

    public void testNextOffset1() throws Exception
    {
        int o1 = measurer.nextOffset(40, 5, true);
        int o2 = measurer.nextOffset(60, 5, true);
        measurer.setPosition(o1);
        int o3 = measurer.nextOffset(60, 25, true);
        measurer.setPosition(o3);
        int o4 = measurer.nextOffset(60, 8, false);
        int o5 = measurer.nextOffset(60, 9, false);
        measurer.setPosition(o4);
        int o6 = measurer.nextOffset(500);

        assertEquals(2, o1);
        assertEquals(2, o2);
        assertEquals(2, o3);
        assertEquals(8, o4);
        assertEquals(8, o5);
        assertEquals(24, o6);
    }

    public void testSetPosition() throws Exception
    {
        measurer.setPosition(10);
        assertEquals(measurer.getPosition(), 10);
    }

    public static Test suite()
    {
        return new TestSuite(LineBreakMeasurerTest.class);
    }
}
