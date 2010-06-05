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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.text.AttributedCharacterIterator;

public class TextMeasurerTest extends TestCase
{
    TextMeasurer measurer;

    private final int width = 500;
    private final int height = 200;
    private final BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    private final String s = "I TestItalic TestPlain I";
    private final Font f = new Font("times new roman", Font.ITALIC, 24);
    private final Font f1 = new Font("serif", Font.PLAIN, 60);
    private final FontRenderContext frc = ((Graphics2D) im.getGraphics()).getFontRenderContext();

    public TextMeasurerTest(String name)
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

        measurer = new TextMeasurer(aci, frc);
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testInsertChar() throws Exception
    {
        float oldAdvance = measurer.getAdvanceBetween(5, 14);
        float oldAdvanceNoChange = measurer.getAdvanceBetween(0, 5);

        String s1 = "I TestItalic WTestPlain I";
        AttributedString as = new AttributedString(s1);
        as.addAttribute(TextAttribute.FONT, f, 0, 12 );
        as.addAttribute(TextAttribute.FONT, f1, 12, s1.length());
        AttributedCharacterIterator aci = as.getIterator();

        measurer.insertChar(aci, 13);

        assertTrue(measurer.getAdvanceBetween(5, 14) > oldAdvance);
        assertEquals(oldAdvanceNoChange, measurer.getAdvanceBetween(0, 5), 0.01);
    }

    public void testDeleteChar() throws Exception
    {
        String s1 = "I TestItalic estPlain I";

        AttributedString as = new AttributedString(s1);
        as.addAttribute(TextAttribute.FONT, f, 0, 12 );
        as.addAttribute(TextAttribute.FONT, f1, 12, s1.length());
        AttributedCharacterIterator aci = as.getIterator();
        float oldAdvance = measurer.getAdvanceBetween(aci.getBeginIndex(), aci.getEndIndex()+1);
        float oldAdvanceNoChange = measurer.getAdvanceBetween(0, 5);

        measurer.deleteChar(aci, 13);

        assertTrue(measurer.getAdvanceBetween(aci.getBeginIndex(), aci.getEndIndex()) < oldAdvance);
        assertEquals(oldAdvanceNoChange, measurer.getAdvanceBetween(0, 5), 0.01);
    }

    public void testClone() throws Exception
    {
        TextMeasurer m = (TextMeasurer) measurer.clone();
        assertNotNull(m);
        assertTrue(m != measurer);
    }

    public void testGetLayout() throws Exception
    {
        TextLayout l1 = measurer.getLayout(0, 15);
        TextLayout l2 = measurer.getLayout(2, 15);
        TextLayout l3 = measurer.getLayout(2, 4);

        assertTrue(l1.getAdvance() > l2.getAdvance());
        assertTrue(l2.getAdvance() > l3.getAdvance());
    }

    public void testGetAdvanceBetween() throws Exception
    {
        float adv1 = measurer.getAdvanceBetween(1, 4);
        float adv2 = measurer.getAdvanceBetween(1, 5);
        float adv3 = measurer.getAdvanceBetween(0, 5);
        float adv4 = measurer.getAdvanceBetween(1, 4);

        assertTrue(adv1 < adv2);
        assertTrue(adv2 < adv3);
        assertEquals(adv1, adv4, 0.01);
    }

    public void testGetLineBreakIndex() throws Exception
    {
        assertEquals(5, measurer.getLineBreakIndex(2, 35));
        assertEquals(11, measurer.getLineBreakIndex(0, 100));
        assertEquals(4, measurer.getLineBreakIndex(4, 1));
        assertEquals(24, measurer.getLineBreakIndex(0, 1000));
    }

    public static Test suite()
    {
        return new TestSuite(TextMeasurerTest.class);
    }
}
