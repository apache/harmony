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
 * @author Alexander T. Simbirtsev
 * Created on 29.11.2004

 */
package javax.swing.border;

import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class AbstractBorderTest extends SwingTestCase {
    /*
     * Class under test for Insets getBorderInsets(Component, Insets)
     */
    public void testGetBorderInsetsComponentInsets() {
        AbstractBorder border = new AbstractBorder() {
            private static final long serialVersionUID = 1L;
        };
        Insets insets = new Insets(1, 1, 1, 1);
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(1, 1, 1, 1));
        border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", 0, insets.top);
        assertEquals("insets values coinside", 0, insets.left);
        assertEquals("insets values coinside", 0, insets.right);
        assertEquals("insets values coinside", 0, insets.bottom);
        insets = new Insets(1, 1, 1, 1);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        Insets newInsets = border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", 0, newInsets.top);
        assertEquals("insets values coinside", 0, newInsets.left);
        assertEquals("insets values coinside", 0, newInsets.right);
        assertEquals("insets values coinside", 0, newInsets.bottom);
        assertEquals("insets values coinside", 0, insets.top);
        assertEquals("insets values coinside", 0, insets.left);
        assertEquals("insets values coinside", 0, insets.right);
        assertEquals("insets values coinside", 0, insets.bottom);
    }

    /*
     * Class under test for Insets getBorderInsets(Component)
     */
    public void testGetBorderInsetsComponent() {
        AbstractBorder border = new AbstractBorder() {
            private static final long serialVersionUID = 1L;
        };
        Insets insets = null;
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(1, 1, 1, 1));
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", 0, insets.top);
        assertEquals("insets values coinside", 0, insets.left);
        assertEquals("insets values coinside", 0, insets.right);
        assertEquals("insets values coinside", 0, insets.bottom);
        panel.setBorder(null);
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", 0, insets.top);
        assertEquals("insets values coinside", 0, insets.left);
        assertEquals("insets values coinside", 0, insets.right);
        assertEquals("insets values coinside", 0, insets.bottom);
    }

    /*
     * Class under test for void paintBorder(Component, Graphics, int, int, int, int)
     *
     * since this method doesn't suppose to do anything,
     * i think the good decision is to leave this testcase empty
     */
    public void testPaintBorder() {
    }

    public void testIsBorderOpaque() {
        AbstractBorder border = new AbstractBorder() {
            private static final long serialVersionUID = 1L;
        };
        assertFalse("AbstractBorder is not opaque ", border.isBorderOpaque());
    }

    /*
     * Class under test for Rectangle getInteriorRectangle(Component, int, int, int, int)
     */
    public void testGetInteriorRectangleComponentintintintint() {
        int top = 10;
        int left = 20;
        int right = 30;
        int bottom = 40;
        int x = 100;
        int y = 200;
        int width = 300;
        int height = 400;
        AbstractBorder border = new AbstractBorder() {
            private static final long serialVersionUID = 1L;
        };
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(top, left, bottom, right));
        Rectangle rect = border.getInteriorRectangle(panel, x, y, width, height);
        assertEquals(new Rectangle(x, y, width, height), rect);
        rect = border.getInteriorRectangle(null, x, y, width, height);
        assertEquals(new Rectangle(x, y, width, height), rect);
        border = new EmptyBorder(top, left, bottom, right);
        rect = border.getInteriorRectangle(null, x, y, width, height);
        assertEquals(new Rectangle(x + left, y + top, width - left - right, height - top
                - bottom), rect);
    }

    /*
     * Class under test for Rectangle getInteriorRectangle(Component, Border, int, int, int, int)
     */
    public void testGetInteriorRectangleComponentBorderintintintint() {
        int top = 10;
        int left = 20;
        int right = 30;
        int bottom = 40;
        int x = 100;
        int y = 200;
        int width = 300;
        int height = 400;

        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(top, left, bottom, right));
        Rectangle rect = AbstractBorder.getInteriorRectangle(panel,
                new EmptyBorder(0, 0, 0, 0), x, y, width, height);
        assertEquals(new Rectangle(x, y, width, height), rect);
        rect = AbstractBorder.getInteriorRectangle(null, new EmptyBorder(0, 0, 0, 0), x, y,
                width, height);
        assertEquals(new Rectangle(x, y, width, height), rect);
        rect = AbstractBorder.getInteriorRectangle(panel, new EmptyBorder(top, left, bottom,
                right), x, y, width, height);
        assertEquals(new Rectangle(x + left, y + top, width - left - right, height - top
                - bottom), rect);
        rect = AbstractBorder.getInteriorRectangle(null, new EmptyBorder(top, left, bottom,
                right), x, y, width, height);
        assertEquals(new Rectangle(x + left, y + top, width - left - right, height - top
                - bottom), rect);
        rect = AbstractBorder.getInteriorRectangle(panel, null, x, y, width, height);
        assertEquals(new Rectangle(x, y, width, height), rect);
        rect = AbstractBorder.getInteriorRectangle(null, null, x, y, width, height);
        assertEquals(new Rectangle(x, y, width, height), rect);
    }
}
