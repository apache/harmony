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
 * @author Vadim L. Bogdanov
 */
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.DebugGraphics;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingTestCase;

public class BasicGraphicsUtilsTest extends SwingTestCase {
    private JFrame frame;

    private Graphics g;

    private int x = 0;

    private final int y = 0;

    private final int w = 10;

    private final int h = 7;

    private final Color shadow = Color.RED;

    private final Color darkShadow = Color.BLACK;

    private final Color highlight = Color.GREEN;

    private final Color lightHighlight = Color.ORANGE;

    public BasicGraphicsUtilsTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        frame = new JFrame();
        frame.setSize(30, 30);
        frame.setVisible(true);
        g = frame.getContentPane().getGraphics();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        frame.dispose();
    }

    public void testBasicGraphicsUtils() {
        new BasicGraphicsUtils();
    }

    public void testDrawEtchedRect() {
        Color color = g.getColor();
        BasicGraphicsUtils.drawEtchedRect(g, x, y, w, h, shadow, darkShadow, highlight,
                lightHighlight);
        assertSame(color, g.getColor());
    }

    public void testGetEtchedInsets() {
        assertEquals(new Insets(2, 2, 2, 2), BasicGraphicsUtils.getEtchedInsets());
    }

    public void testDrawGroove() {
        Color color = g.getColor();
        BasicGraphicsUtils.drawGroove(g, x, y, w, h, shadow, highlight);
        assertSame(color, g.getColor());
    }

    public void testGetGrooveInsets() {
        assertEquals(new Insets(2, 2, 2, 2), BasicGraphicsUtils.getGrooveInsets());
    }

    public void testDrawBezel() {
        Color color = g.getColor();
        BasicGraphicsUtils.drawBezel(g, x, y, w, h, true, true, shadow, darkShadow, highlight,
                lightHighlight);
        BasicGraphicsUtils.drawBezel(g, x, y, w, h, true, false, shadow, darkShadow, highlight,
                lightHighlight);
        BasicGraphicsUtils.drawBezel(g, x, y, w, h, false, true, shadow, darkShadow, highlight,
                lightHighlight);
        BasicGraphicsUtils.drawBezel(g, x, y, w, h, false, false, shadow, darkShadow,
                highlight, lightHighlight);
        assertSame(color, g.getColor());
    }

    public void testDrawLoweredBezel() {
        Color color = g.getColor();
        BasicGraphicsUtils.drawLoweredBezel(g, x, y, w, h, shadow, darkShadow, highlight,
                lightHighlight);
        if (isHarmony()) {
            assertSame(color, g.getColor());
        }
    }

    public void testDrawString() {
        // TODO: implement
    }

    public void testDrawStringUnderlineCharAt() {
        // TODO: implement
    }

    public void testDrawDashedRect() {
        g = new DebugGraphics(g);
        Color color = g.getColor();
        BasicGraphicsUtils.drawDashedRect(g, x, y, w, h);
        assertSame(color, g.getColor());
    }

    public void testGetPreferredButtonSize() {
        Icon icon = new ImageIcon(new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB));
        JMenuItem item = new JMenuItem() {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("deprecation")
            @Override
            public FontMetrics getFontMetrics(Font font) {
                return Toolkit.getDefaultToolkit().getFontMetrics(font);
            }
        };
        item.setBorder(BorderFactory.createEmptyBorder(10, 20, 30, 40));
        item.setIcon(icon);
        assertEquals(new Dimension(70, 60), BasicGraphicsUtils.getPreferredButtonSize(item, 3));
    }
}
