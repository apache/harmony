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
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class BevelBorderTest extends SwingTestCase {
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(BevelBorderTest.class);
    }

    /*
     * Class under test for void BevelBorder(int)
     */
    public void testBevelBorderint() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightColor = null;
        Color shadowColor = null;
        BevelBorder border = new BevelBorder(bevelType);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
        bevelType = BevelBorder.RAISED;
        border = new BevelBorder(bevelType);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
    }

    /*
     * Class under test for void BevelBorder(int, Color, Color)
     */
    public void testBevelBorderintColorColor() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightColor = Color.RED;
        Color shadowColor = Color.GREEN;
        BevelBorder border = new BevelBorder(bevelType, highlightColor, shadowColor);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
        bevelType = BevelBorder.RAISED;
        highlightColor = Color.YELLOW;
        shadowColor = Color.WHITE;
        border = new BevelBorder(bevelType, highlightColor, shadowColor);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
    }

    /*
     * Class under test for void BevelBorder(int, Color, Color, Color, Color)
     */
    public void testBevelBorderintColorColorColorColor() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightOuterColor = Color.RED;
        Color highlightInnerColor = Color.YELLOW;
        Color shadowOuterColor = Color.GREEN;
        Color shadowInnerColor = Color.BLACK;
        BevelBorder border = new BevelBorder(bevelType, highlightOuterColor,
                highlightInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("highlightOuterColor coinsides", highlightOuterColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightInnerColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowOuterColor, border
                .getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowInnerColor, border
                .getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
        bevelType = BevelBorder.RAISED;
        highlightOuterColor = Color.YELLOW;
        highlightInnerColor = Color.RED;
        shadowOuterColor = Color.WHITE;
        shadowInnerColor = Color.BLUE;
        border = new BevelBorder(bevelType, highlightOuterColor, highlightInnerColor,
                shadowOuterColor, shadowInnerColor);
        assertEquals("highlightOuterColor coinsides", highlightOuterColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightInnerColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowOuterColor, border
                .getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowInnerColor, border
                .getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
    }

    /*
     * Class under test for Insets getBorderInsets(Component, Insets)
     */
    public void testGetBorderInsetsComponentInsets() {
        int thickness1 = 2;
        int thickness2 = 22;
        int thickness3 = 33;
        BevelBorder border = new BevelBorder(BevelBorder.RAISED, Color.black, Color.white);
        Insets insets = new Insets(1, 1, 1, 1);
        JPanel panel = new JPanel();
        border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", thickness1, insets.top);
        assertEquals("insets values coinside", thickness1, insets.left);
        assertEquals("insets values coinside", thickness1, insets.right);
        assertEquals("insets values coinside", thickness1, insets.bottom);
        panel.setBorder(new LineBorder(Color.black, thickness2));
        border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", thickness1, insets.top);
        assertEquals("insets values coinside", thickness1, insets.left);
        assertEquals("insets values coinside", thickness1, insets.right);
        assertEquals("insets values coinside", thickness1, insets.bottom);
        insets = new Insets(thickness3, thickness3, thickness3, thickness3);
        panel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        Insets newInsets = border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", thickness1, newInsets.top);
        assertEquals("insets values coinside", thickness1, newInsets.left);
        assertEquals("insets values coinside", thickness1, newInsets.right);
        assertEquals("insets values coinside", thickness1, newInsets.bottom);
        assertEquals("insets values coinside", thickness1, insets.top);
        assertEquals("insets values coinside", thickness1, insets.left);
        assertEquals("insets values coinside", thickness1, insets.right);
        assertEquals("insets values coinside", thickness1, insets.bottom);
    }

    /*
     * Class under test for Insets getBorderInsets(Component)
     */
    public void testGetBorderInsetsComponent() {
        int thickness1 = 2;
        int thickness2 = 22;
        int thickness3 = 33;
        BevelBorder border = new BevelBorder(BevelBorder.RAISED, Color.black, Color.white);
        Insets insets = new Insets(1, 1, 1, 1);
        JPanel panel = new JPanel();
        insets = border.getBorderInsets(null);
        assertEquals("insets values coinside", thickness1, insets.top);
        assertEquals("insets values coinside", thickness1, insets.left);
        assertEquals("insets values coinside", thickness1, insets.right);
        assertEquals("insets values coinside", thickness1, insets.bottom);
        panel.setBorder(new LineBorder(Color.black, thickness2));
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", thickness1, insets.top);
        assertEquals("insets values coinside", thickness1, insets.left);
        assertEquals("insets values coinside", thickness1, insets.right);
        assertEquals("insets values coinside", thickness1, insets.bottom);
        insets = new Insets(thickness3, thickness3, thickness3, thickness3);
        panel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", thickness1, insets.top);
        assertEquals("insets values coinside", thickness1, insets.left);
        assertEquals("insets values coinside", thickness1, insets.right);
        assertEquals("insets values coinside", thickness1, insets.bottom);
    }

    /**
     * This method is being tested by testPaintBorder()
     */
    public void testPaintRaisedBevel() {
    }

    /**
     * This method is being tested by testPaintBorder()
     */
    public void testPaintLoweredBevel() {
    }

    public void testPaintBorder() {
        //      JPanel panel1 = new JPanel();
        //      JPanel panel2 = new JPanel();
        //      JPanel panel3 = new JPanel();
        //
        //      Color shadowInnerColor = Color.GREEN;
        //      Color shadowOuterColor = Color.BLACK;
        //      Color highlightedInnerColor = Color.RED;
        //      Color highlightedOuterColor = Color.BLUE;
        //
        //      BevelBorder border1 = new BevelBorder(EtchedBorder.LOWERED, highlightedOuterColor, highlightedInnerColor,
        //                                                                  shadowOuterColor, shadowInnerColor);
        //      BevelBorder border2 = new BevelBorder(EtchedBorder.RAISED, highlightedOuterColor, highlightedInnerColor,
        //                                                                  shadowOuterColor, shadowInnerColor);
        //      panel2.setBorder(border1);
        //      panel3.setBorder(border2);
        //      panel2.setPreferredSize(new Dimension(200, 150));
        //      panel3.setPreferredSize(new Dimension(200, 150));
        //
        //      panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        //      panel1.add(panel2);
        //      panel1.add(panel3);
        //
        //      JFrame frame = new JFrame();
        //      frame.getContentPane().add(panel1);
        //      frame.pack();
        //      frame.show();
        //      while(!frame.isActive());
        //      while(frame.isActive());
    }

    public void testIsBorderOpaque() {
        Color shadowColor = Color.GREEN;
        Color highlightedColor = Color.RED;
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED, highlightedColor, shadowColor);
        assertTrue("BevelBorder is opaque", border.isBorderOpaque());
        border = new BevelBorder(BevelBorder.RAISED);
        assertTrue("BevelBorder is opaque", border.isBorderOpaque());
    }

    /*
     * Class under test for Color getShadowOuterColor(Component)
     */
    public void testGetShadowOuterColorComponent() {
        JComponent c1 = new JPanel();
        JComponent c2 = new JPanel();
        c1.setBackground(new Color(110, 110, 110));
        c1.setForeground(new Color(210, 210, 210));
        c2.setBackground(new Color(10, 10, 10));
        c2.setForeground(new Color(110, 110, 110));
        Color shadowInnerColor = Color.GREEN;
        Color shadowOuterColor = Color.CYAN;
        Color highlightedInnerColor = Color.RED;
        Color highlightedOuterColor = Color.YELLOW;
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Shadow Outer color coinsides", shadowOuterColor, border
                .getShadowOuterColor(c1));
        assertEquals("Shadow Outer color coinsides", shadowOuterColor, border
                .getShadowOuterColor(c2));
        border = new BevelBorder(BevelBorder.RAISED);
        assertEquals("Shadow Outer color coinsides", new Color(53, 53, 53), border
                .getShadowOuterColor(c1));
        assertEquals("Shadow Outer color coinsides", new Color(4, 4, 4), border
                .getShadowOuterColor(c2));
    }

    /*
     * Class under test for Color getShadowInnerColor(Component)
     */
    public void testGetShadowInnerColorComponent() {
        JComponent c1 = new JPanel();
        JComponent c2 = new JPanel();
        c1.setBackground(new Color(110, 110, 110));
        c1.setForeground(new Color(210, 210, 210));
        c2.setBackground(new Color(10, 10, 10));
        c2.setForeground(new Color(110, 110, 110));
        Color shadowInnerColor = Color.GREEN;
        Color shadowOuterColor = Color.CYAN;
        Color highlightedInnerColor = Color.RED;
        Color highlightedOuterColor = Color.YELLOW;
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Shadow Inner color coinsides", shadowInnerColor, border
                .getShadowInnerColor(c1));
        assertEquals("Shadow Inner color coinsides", shadowInnerColor, border
                .getShadowInnerColor(c2));
        border = new BevelBorder(BevelBorder.RAISED);
        assertEquals("Shadow Inner color coinsides", new Color(77, 77, 77), border
                .getShadowInnerColor(c1));
        assertEquals("Shadow Inner color coinsides", new Color(7, 7, 7), border
                .getShadowInnerColor(c2));
    }

    /*
     * Class under test for Color getHighlightOuterColor(Component)
     */
    public void testGetHighlightOuterColorComponent() {
        JComponent c1 = new JPanel();
        JComponent c2 = new JPanel();
        c1.setBackground(new Color(110, 110, 110));
        c1.setForeground(new Color(210, 210, 210));
        c2.setBackground(new Color(10, 10, 10));
        c2.setForeground(new Color(110, 110, 110));
        Color shadowInnerColor = Color.GREEN;
        Color shadowOuterColor = Color.CYAN;
        Color highlightedInnerColor = Color.RED;
        Color highlightedOuterColor = Color.YELLOW;
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Highlight Outer color coinsides", highlightedOuterColor, border
                .getHighlightOuterColor(c1));
        assertEquals("Highlight Outer color coinsides", highlightedOuterColor, border
                .getHighlightOuterColor(c2));
        border = new BevelBorder(BevelBorder.RAISED);
        assertEquals("Highlight Outer color coinsides", new Color(224, 224, 224), border
                .getHighlightOuterColor(c1));
        assertEquals("Highlight Outer color coinsides", new Color(20, 20, 20), border
                .getHighlightOuterColor(c2));
    }

    /*
     * Class under test for Color getHighlightInnerColor(Component)
     */
    public void testGetHighlightInnerColorComponent() {
        JComponent c1 = new JPanel();
        JComponent c2 = new JPanel();
        c1.setBackground(new Color(110, 110, 110));
        c1.setForeground(new Color(210, 210, 210));
        c2.setBackground(new Color(10, 10, 10));
        c2.setForeground(new Color(110, 110, 110));
        Color shadowInnerColor = Color.GREEN;
        Color shadowOuterColor = Color.CYAN;
        Color highlightedInnerColor = Color.RED;
        Color highlightedOuterColor = Color.YELLOW;
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Highlight Inner color coinsides", highlightedInnerColor, border
                .getHighlightInnerColor(c1));
        assertEquals("Highlight Inner color coinsides", highlightedInnerColor, border
                .getHighlightInnerColor(c2));
        border = new BevelBorder(BevelBorder.RAISED);
        assertEquals("Highlight Inner color coinsides", new Color(157, 157, 157), border
                .getHighlightInnerColor(c1));
        assertEquals("Highlight Inner color coinsides", new Color(14, 14, 14), border
                .getHighlightInnerColor(c2));
    }

    /*
     * Class under test for Color getShadowOuterColor()
     */
    public void testGetShadowOuterColor() {
        Color shadowInnerColor = Color.YELLOW;
        Color shadowOuterColor = Color.CYAN;
        Color highlightedInnerColor = Color.RED;
        Color highlightedOuterColor = Color.GRAY;
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Shadow Outer color coinsides", shadowOuterColor, border
                .getShadowOuterColor());
        shadowOuterColor = Color.GREEN;
        highlightedOuterColor = Color.WHITE;
        border = new BevelBorder(BevelBorder.RAISED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Shadow Outer color coinsides", shadowOuterColor, border
                .getShadowOuterColor());
    }

    /*
     * Class under test for Color getShadowInnerColor()
     */
    public void testGetShadowInnerColor() {
        Color shadowInnerColor = Color.YELLOW;
        Color shadowOuterColor = Color.CYAN;
        Color highlightedInnerColor = Color.RED;
        Color highlightedOuterColor = Color.GRAY;
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Shadow Inner color coinsides", shadowInnerColor, border
                .getShadowInnerColor());
        shadowInnerColor = Color.GREEN;
        highlightedInnerColor = Color.WHITE;
        border = new BevelBorder(BevelBorder.RAISED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Shadow Inner color coinsides", shadowInnerColor, border
                .getShadowInnerColor());
    }

    /*
     * Class under test for Color getHighlightOuterColor()
     */
    public void testGetHighlightOuterColor() {
        Color shadowInnerColor = Color.YELLOW;
        Color shadowOuterColor = Color.CYAN;
        Color highlightedInnerColor = Color.RED;
        Color highlightedOuterColor = Color.GRAY;
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Highlighted Outer color coinsides", highlightedOuterColor, border
                .getHighlightOuterColor());
        shadowOuterColor = Color.GREEN;
        highlightedOuterColor = Color.WHITE;
        border = new BevelBorder(BevelBorder.RAISED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Highlighted Outer color coinsides", highlightedOuterColor, border
                .getHighlightOuterColor());
    }

    /*
     * Class under test for Color getHighlightInnerColor()
     */
    public void testGetHighlightInnerColor() {
        Color shadowInnerColor = Color.YELLOW;
        Color shadowOuterColor = Color.CYAN;
        Color highlightedInnerColor = Color.RED;
        Color highlightedOuterColor = Color.GRAY;
        BevelBorder border = new BevelBorder(BevelBorder.LOWERED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Highlighted Inner color coinsides", highlightedInnerColor, border
                .getHighlightInnerColor());
        shadowInnerColor = Color.GREEN;
        highlightedInnerColor = Color.WHITE;
        border = new BevelBorder(BevelBorder.LOWERED, highlightedOuterColor,
                highlightedInnerColor, shadowOuterColor, shadowInnerColor);
        assertEquals("Highlighted Inner color coinsides", highlightedInnerColor, border
                .getHighlightInnerColor());
    }

    public void testGetBevelType() {
        int bevelType = BevelBorder.LOWERED;
        BevelBorder border = new BevelBorder(bevelType);
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
        bevelType = BevelBorder.RAISED;
        border = new BevelBorder(bevelType);
        assertEquals("Etch type coinsides", bevelType, border.getBevelType());
    }

    public void testReadWriteObject() throws Exception {
        Color shadowInnerColor1 = Color.RED;
        Color shadowInnerColor2 = Color.BLUE;
        Color shadowOuterColor1 = Color.CYAN;
        Color shadowOuterColor2 = Color.MAGENTA;
        Color highlightedInnerColor1 = Color.YELLOW;
        Color highlightedInnerColor2 = Color.GREEN;
        Color highlightedOuterColor1 = Color.DARK_GRAY;
        Color highlightedOuterColor2 = Color.LIGHT_GRAY;
        int bevelType1 = BevelBorder.LOWERED;
        int bevelType2 = BevelBorder.RAISED;
        BevelBorder border1 = new BevelBorder(bevelType1, highlightedOuterColor1,
                highlightedInnerColor1, shadowOuterColor1, shadowInnerColor1);
        BevelBorder border2 = new BevelBorder(bevelType2, highlightedOuterColor2,
                highlightedInnerColor2, shadowOuterColor2, shadowInnerColor2);
        BevelBorder resurrectedBorder = (BevelBorder) serializeObject(border1);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coinsides", resurrectedBorder.getBevelType(), border1
                .getBevelType());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getShadowInnerColor(),
                border1.getShadowInnerColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getShadowOuterColor(),
                border1.getShadowOuterColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder
                .getHighlightInnerColor(), border1.getHighlightInnerColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder
                .getHighlightOuterColor(), border1.getHighlightOuterColor());
        resurrectedBorder = (BevelBorder) serializeObject(border2);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coinsides", resurrectedBorder.getBevelType(), border2
                .getBevelType());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getShadowInnerColor(),
                border2.getShadowInnerColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getShadowOuterColor(),
                border2.getShadowOuterColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder
                .getHighlightInnerColor(), border2.getHighlightInnerColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder
                .getHighlightOuterColor(), border2.getHighlightOuterColor());
    }

    public void testBevelType() throws Exception {
        // Regression test for HARMONY-2590
        for (int i = -10; i < 10; i++) {
             new BevelBorder(i);
        }
    }
}
