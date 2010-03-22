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
 * @author Anton Avtamonov
 */
package javax.swing;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class GrayFilterTest extends SwingTestCase {
    private GrayFilter filter;

    public GrayFilterTest(final String name) {
        super(name);
    }

    @Override
    protected void tearDown() throws Exception {
        filter = null;
    }

    public void testGrayFilter() throws Exception {
        new GrayFilter(true, 0);
        new GrayFilter(false, 100);
        checkIncorrectGrayPercentage(-1);
        checkIncorrectGrayPercentage(101);
    }

    public void testFilterRGB() throws Exception {
        checkRange(0, 85);
        checkRange(10, 76);
        checkRange(20, 68);
        checkRange(30, 59);
        checkRange(40, 51);
        checkRange(50, 42);
        checkRange(60, 34);
        checkRange(70, 25);
        checkRange(80, 17);
        checkRange(90, 8);
        checkRange(100, 0);
    }

    public void testCreateDisabledImage() throws Exception {
        Image original = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Image grayed = GrayFilter.createDisabledImage(original);
        assertNotNull(grayed);
        assertEquals(original.getHeight(null), grayed.getHeight(null));
        assertEquals(original.getWidth(null), grayed.getWidth(null));
        assertEquals(20, grayed.getWidth(null));
        assertEquals(20, grayed.getHeight(null));
        assertNotSame(original, grayed);
    }

    private void checkIncorrectGrayPercentage(final int percentage) {
        try {
            new GrayFilter(false, percentage);
            if (isHarmony()) {
                fail("Incorrect gray percentage is not detected");
            }
        } catch (final IllegalArgumentException iae) {
        }
    }

    private void checkRange(final int percentage, final int expectedInterval) {
        filter = new GrayFilter(false, percentage);
        checkIsClosedTo(0, filter.filterRGB(0, 0, new Color(0, 0, 0).getRGB()));
        checkIsClosedTo(expectedInterval, filter.filterRGB(0, 0, new Color(255, 255, 255)
                .getRGB()));
        int lowBound = 255 * percentage / 100;
        filter = new GrayFilter(true, percentage);
        checkIsClosedTo(lowBound, filter.filterRGB(0, 0, new Color(0, 0, 0).getRGB()));
        checkIsClosedTo(lowBound + expectedInterval, filter.filterRGB(0, 0, new Color(255, 255,
                255).getRGB()));
    }

    private void checkIsClosedTo(final int expected, final int actualRGB) {
        Color c = new Color(actualRGB);
        assertEquals(c.getRed(), c.getGreen());
        assertEquals(c.getRed(), c.getBlue());
        assertTrue(Math.abs(c.getRed() - expected) <= 1);
    }
}
