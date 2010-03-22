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
//import java.awt.Dimension;
import java.awt.Insets;
//import javax.swing.BoxLayout;
//import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class SoftBevelBorderTest extends SwingTestCase {
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(SoftBevelBorderTest.class);
    }

    /*
     * Class under test for void SoftBevelBorder(int, Color, Color, Color, Color)
     */
    public void testSoftBevelBorderintColorColorColorColor() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightOuterColor = Color.RED;
        Color highlightInnerColor = Color.YELLOW;
        Color shadowOuterColor = Color.GREEN;
        Color shadowInnerColor = Color.BLACK;
        SoftBevelBorder border = new SoftBevelBorder(bevelType, highlightOuterColor,
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
        border = new SoftBevelBorder(bevelType, highlightOuterColor, highlightInnerColor,
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
     * Class under test for void SoftBevelBorder(int, Color, Color)
     */
    public void testSoftBevelBorderintColorColor() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightColor = Color.RED;
        Color shadowColor = Color.GREEN;
        SoftBevelBorder border = new SoftBevelBorder(bevelType, highlightColor, shadowColor);
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
        border = new SoftBevelBorder(bevelType, highlightColor, shadowColor);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
    }

    /*
     * Class under test for void SoftBevelBorder(int)
     */
    public void testSoftBevelBorderint() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightColor = null;
        Color shadowColor = null;
        SoftBevelBorder border = new SoftBevelBorder(bevelType);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
        bevelType = BevelBorder.RAISED;
        border = new SoftBevelBorder(bevelType);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
    }

    /*
     * Class under test for Insets getBorderInsets(Component, Insets)
     */
    public void testGetBorderInsetsComponentInsets() {
        int thickness1 = 3;
        int thickness2 = 22;
        int thickness3 = 33;
        SoftBevelBorder border = new SoftBevelBorder(BevelBorder.RAISED, Color.black,
                Color.white);
        Insets insets = new Insets(10, 10, 10, 10);
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
        panel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
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
        int thickness1 = 3;
        int thickness2 = 22;
        int thickness3 = 33;
        SoftBevelBorder border = new SoftBevelBorder(BevelBorder.RAISED, Color.black,
                Color.white);
        Insets insets = new Insets(10, 10, 10, 10);
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
        panel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", thickness1, insets.top);
        assertEquals("insets values coinside", thickness1, insets.left);
        assertEquals("insets values coinside", thickness1, insets.right);
        assertEquals("insets values coinside", thickness1, insets.bottom);
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
        //      SoftBevelBorder border1 = new SoftBevelBorder(EtchedBorder.LOWERED, highlightedOuterColor, highlightedInnerColor,
        //                                                                  shadowOuterColor, shadowInnerColor);
        //      SoftBevelBorder border2 = new SoftBevelBorder(EtchedBorder.RAISED, highlightedOuterColor, highlightedInnerColor,
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
        SoftBevelBorder border = new SoftBevelBorder(BevelBorder.LOWERED, highlightedColor,
                shadowColor);
        assertFalse("SoftBevelBorder is not opaque", border.isBorderOpaque());
        border = new SoftBevelBorder(BevelBorder.RAISED);
        assertFalse("SoftBevelBorder is not opaque", border.isBorderOpaque());
    }
}
