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
 * Created on 03.12.2004

 */
package javax.swing.border;

import java.awt.Color;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class MatteBorderTest extends SwingTestCase {
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(MatteBorderTest.class);
    }

    /*
     * Class under test for void MatteBorder(Insets, Icon)
     */
    public void testMatteBorderInsetsIcon() {
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        MatteBorder border = new MatteBorder(new Insets(top, left, bottom, right), icon);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
        assertEquals("icon value coinsides", icon, border.getTileIcon());
        icon = new ImageIcon(new BufferedImage(30, 40, BufferedImage.TYPE_4BYTE_ABGR));
        top = 200;
        left = 300;
        right = 200;
        bottom = 300;
        border = new MatteBorder(new Insets(top, left, bottom, right), icon);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
        assertEquals("icon value coinsides", icon, border.getTileIcon());
    }

    /*
     * Class under test for void MatteBorder(Insets, Color)
     */
    public void testMatteBorderInsetsColor() {
        Color color = Color.RED;
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        MatteBorder border = new MatteBorder(new Insets(top, left, bottom, right), color);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
        assertEquals("color value coinsides", color, border.getMatteColor());
        color = Color.YELLOW;
        top = 200;
        left = 300;
        right = 200;
        bottom = 300;
        border = new MatteBorder(new Insets(top, left, bottom, right), color);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
        assertEquals("color value coinsides", color, border.getMatteColor());
    }

    /*
     * Class under test for void MatteBorder(Icon)
     */
    public void testMatteBorderIcon() {
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        MatteBorder border = new MatteBorder(icon);
        Insets insets;

        assertEquals("icon value coinsides", icon, border.getTileIcon());
        icon = new ImageIcon(new BufferedImage(30, 40, BufferedImage.TYPE_4BYTE_ABGR));
        border = new MatteBorder(icon);
        assertEquals("icon value coinsides", icon, border.getTileIcon());
        
        //Regression test for HARMONY-2589
        border = new MatteBorder(null);
        insets = border.getBorderInsets();
        assertEquals(-1, insets.top);
        assertEquals(-1, insets.bottom);
        assertEquals(-1, insets.left);
        assertEquals(-1, insets.right);
    }

    /*
     * Class under test for void MatteBorder(int, int, int, int, Icon)
     */
    public void testMatteBorderintintintintIcon() {
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        MatteBorder border = new MatteBorder(top, left, bottom, right, icon);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
        assertEquals("icon value coinsides", icon, border.getTileIcon());
        icon = new ImageIcon(new BufferedImage(30, 40, BufferedImage.TYPE_4BYTE_ABGR));
        top = 200;
        left = 300;
        right = 200;
        bottom = 300;
        border = new MatteBorder(top, left, bottom, right, icon);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
        assertEquals("icon value coinsides", icon, border.getTileIcon());
    }

    /*
     * Class under test for void MatteBorder(int, int, int, int, Color)
     */
    public void testMatteBorderintintintintColor() {
        Color color = Color.RED;
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        MatteBorder border = new MatteBorder(top, left, bottom, right, color);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
        assertEquals("color value coinsides", color, border.getMatteColor());
        color = Color.YELLOW;
        top = 200;
        left = 300;
        right = 200;
        bottom = 300;
        border = new MatteBorder(top, left, bottom, right, color);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
        assertEquals("color value coinsides", color, border.getMatteColor());
    }

    public void testPaintBorder() {
        //        JPanel panel1 = new JPanel();
        //        JPanel panel2 = new JPanel();
        //        JPanel panel3 = new JPanel();
        //
        //        Color color1 = Color.GREEN;
        //        Icon icon = null;
        //        icon = new ImageIcon(DefaultMetalTheme.class.getResource("icons/Error.gif"));
        //        Color shadowOuterColor = Color.BLACK;
        //        Color highlightedInnerColor = Color.RED;
        //        Color highlightedOuterColor = Color.BLUE;
        //
        //        Border border1 = new MatteBorder(10, 20, 30, 50, color1);
        //        Border border2 = new MatteBorder(10, 20, 30, 50, icon);
        //        panel2.setBorder(border1);
        //        panel3.setBorder(border2);
        //        panel2.setPreferredSize(new Dimension(200, 150));
        //        panel3.setPreferredSize(new Dimension(200, 150));
        //
        //        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        //        panel1.add(panel2);
        //        panel1.add(panel3);
        //
        //        JFrame frame = new JFrame();
        //        frame.getContentPane().add(panel1);
        //        frame.pack();
        //        frame.show();
        //        while(!frame.isActive());
        //        while(frame.isActive());
    }

    public void testIsBorderOpaque() {
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        Color color = Color.RED;
        MatteBorder border = new MatteBorder(top, left, bottom, right, color);
        assertTrue("MatteBorder without tiles is opaque", border.isBorderOpaque());
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        border = new MatteBorder(top, left, bottom, right, icon);
        assertFalse("MatteBorder with tiles is not opaque", border.isBorderOpaque());
    }

    /*
     * Class under test for Insets getBorderInsets(Component, Insets)
     */
    public void testGetBorderInsetsComponentInsets() {
        int top = 10;
        int left = 20;
        int right = 30;
        int bottom = 40;
        Icon icon = new ImageIcon(new BufferedImage(200, 200, BufferedImage.TYPE_BYTE_GRAY));
        MatteBorder border = new MatteBorder(top, left, bottom, right, icon);
        Insets insets = new Insets(1, 1, 1, 1);
        JPanel panel = new JPanel();
        border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
        panel.setBorder(new LineBorder(Color.black, 100));
        border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
        insets = new Insets(2 * top, 2 * left, 2 * bottom, 2 * right);
        panel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        Insets newInsets = border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", top, newInsets.top);
        assertEquals("insets values coinside", left, newInsets.left);
        assertEquals("insets values coinside", right, newInsets.right);
        assertEquals("insets values coinside", bottom, newInsets.bottom);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
    }

    /*
     * Class under test for Insets getBorderInsets(Component)
     */
    public void testGetBorderInsetsComponent() {
        int top = 10;
        int left = 20;
        int right = 30;
        int bottom = 40;
        Icon icon = new ImageIcon(new BufferedImage(200, 200, BufferedImage.TYPE_BYTE_GRAY));
        MatteBorder border = new MatteBorder(top, left, bottom, right, icon);
        Insets insets = new Insets(1, 1, 1, 1);
        JPanel panel = new JPanel();
        border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
        panel.setBorder(new LineBorder(Color.black, 100));
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
        insets = new Insets(2 * top, 2 * left, 2 * bottom, 2 * right);
        panel.setBorder(new EmptyBorder(insets));
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
    }

    /*
     * Class under test for Insets getBorderInsets()
     */
    public void testGetBorderInsets() {
        int top = 10;
        int left = 20;
        int right = 30;
        int bottom = 40;
        int tileHeight = 30;
        int tileWidth = 40;
        Color color = Color.RED;
        MatteBorder border = new MatteBorder(top, left, bottom, right, color);
        Icon icon = new ImageIcon(new BufferedImage(tileWidth, tileHeight,
                BufferedImage.TYPE_BYTE_GRAY));
        top = left = bottom = right = 30;
        border = new MatteBorder(top, left, bottom, right, icon);
        assertEquals("Insets coinside ", new Insets(top, left, bottom, right), border
                .getBorderInsets());
        top = left = bottom = right = 10;
        border = new MatteBorder(top, left, bottom, right, icon);
        assertEquals("Insets coinside ", new Insets(top, left, bottom, right), border
                .getBorderInsets());
        border = new MatteBorder(icon);
        assertEquals("Insets coinside ", new Insets(tileHeight, tileWidth, tileHeight,
                tileWidth), border.getBorderInsets());
        top = left = bottom = right = 1;
        border = new MatteBorder(top, left, bottom, right, icon);
        assertEquals("Insets coinside ", new Insets(top, left, bottom, right), border
                .getBorderInsets());
        top = left = bottom = right = 0;
        border = new MatteBorder(top, left, bottom, right, icon);
        assertEquals("Insets coinside ", new Insets(top, left, bottom, right), border
                .getBorderInsets());
    }

    public void testGetTileIcon() {
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        Color color = Color.RED;
        Icon icon = null;
        MatteBorder border = new MatteBorder(top, left, bottom, right, color);
        assertEquals("Icon coinsides", icon, border.getTileIcon());
        icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        border = new MatteBorder(top, left, bottom, right, icon);
        assertEquals("Icon coinsides", icon, border.getTileIcon());
    }

    public void testGetMatteColor() {
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        Color color = Color.RED;
        MatteBorder border = new MatteBorder(top, left, bottom, right, color);
        assertEquals("Colors coinside ", color, border.getMatteColor());
        color = Color.YELLOW;
        border = new MatteBorder(top, left, bottom, right, color);
        assertEquals("Colors coinside ", color, border.getMatteColor());
    }    
}