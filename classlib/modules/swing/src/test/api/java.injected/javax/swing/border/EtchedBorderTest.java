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

public class EtchedBorderTest extends SwingTestCase {
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(EtchedBorderTest.class);
    }

    /*
     * Class under test for void EtchedBorder()
     */
    public void testEtchedBorder() {
        int etchType = EtchedBorder.LOWERED;
        Color shadowColor = null;
        Color highlightedColor = null;
        EtchedBorder border = new EtchedBorder();
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
    }

    /*
     * Class under test for void EtchedBorder(int)
     */
    public void testEtchedBorderint() {
        int etchType = EtchedBorder.LOWERED;
        Color shadowColor = null;
        Color highlightedColor = null;
        EtchedBorder border = new EtchedBorder(etchType);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
        etchType = EtchedBorder.RAISED;
        border = new EtchedBorder(etchType);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
    }

    /*
     * Class under test for void EtchedBorder(Color, Color)
     */
    public void testEtchedBorderColorColor() {
        int etchType = EtchedBorder.LOWERED;
        Color shadowColor = Color.YELLOW;
        Color highlightedColor = Color.RED;
        EtchedBorder border = new EtchedBorder(highlightedColor, shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
        shadowColor = Color.GREEN;
        highlightedColor = Color.WHITE;
        border = new EtchedBorder(highlightedColor, shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
    }

    /*
     * Class under test for void EtchedBorder(int, Color, Color)
     */
    public void testEtchedBorderintColorColor() {
        int etchType = EtchedBorder.LOWERED;
        Color shadowColor = Color.YELLOW;
        Color highlightedColor = Color.RED;
        EtchedBorder border = new EtchedBorder(etchType, highlightedColor, shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
        etchType = EtchedBorder.RAISED;
        shadowColor = Color.GREEN;
        highlightedColor = Color.WHITE;
        border = new EtchedBorder(etchType, highlightedColor, shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
    }

    /*
     * Class under test for Insets getBorderInsets(Component, Insets)
     */
    public void testGetBorderInsetsComponentInsets() {
        int thickness1 = 2;
        int thickness2 = 22;
        int thickness3 = 33;
        EtchedBorder border = new EtchedBorder(Color.black, Color.white);
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
        panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
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
        EtchedBorder border = new EtchedBorder(Color.black, Color.white);
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
        panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", thickness1, insets.top);
        assertEquals("insets values coinside", thickness1, insets.left);
        assertEquals("insets values coinside", thickness1, insets.right);
        assertEquals("insets values coinside", thickness1, insets.bottom);
    }

    public void testPaintBorder() {
        //        JPanel panel1 = new JPanel();
        //        JPanel panel2 = new JPanel();
        //        JPanel panel3 = new JPanel();
        //
        //        EtchedBorder border1 = new EtchedBorder(EtchedBorder.LOWERED, Color.red, Color.yellow);
        //        EtchedBorder border2 = new EtchedBorder(EtchedBorder.RAISED, Color.red, Color.yellow);
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
        Color shadowColor = Color.GREEN;
        Color highlightedColor = Color.RED;
        EtchedBorder border = new EtchedBorder(highlightedColor, shadowColor);
        assertTrue("EtchedBorder is opaque", border.isBorderOpaque());
        border = new EtchedBorder();
        assertTrue("EtchedBorder is opaque", border.isBorderOpaque());
    }

    /*
     * Class under test for Color getShadowColor(Component)
     */
    public void testGetShadowColorComponent() {
        JComponent c1 = new JPanel();
        JComponent c2 = new JPanel();
        c1.setBackground(new Color(110, 110, 110));
        c1.setForeground(new Color(210, 210, 210));
        c2.setBackground(new Color(10, 10, 10));
        c2.setForeground(new Color(110, 110, 110));
        Color shadowColor = Color.GREEN;
        Color highlightedColor = Color.RED;
        EtchedBorder border = new EtchedBorder(highlightedColor, shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor(c1));
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor(c2));
        border = new EtchedBorder();
        assertEquals("Shadow color coinsides", new Color(77, 77, 77), border.getShadowColor(c1));
        assertEquals("Shadow color coinsides", new Color(7, 7, 7), border.getShadowColor(c2));
    }

    /*
     * Class under test for Color getHighlightColor(Component)
     */
    public void testGetHighlightColorComponent() {
        JComponent c1 = new JPanel();
        JComponent c2 = new JPanel();
        c1.setBackground(new Color(110, 110, 110));
        c1.setForeground(new Color(210, 210, 210));
        c2.setBackground(new Color(10, 10, 10));
        c2.setForeground(new Color(110, 110, 110));
        Color shadowColor = Color.GREEN;
        Color highlightedColor = Color.RED;
        EtchedBorder border = new EtchedBorder(highlightedColor, shadowColor);
        assertEquals("Highlight color coinsides", highlightedColor, border
                .getHighlightColor(c1));
        assertEquals("Highlight color coinsides", highlightedColor, border
                .getHighlightColor(c2));
        border = new EtchedBorder();
        assertEquals("Highlight color coinsides", new Color(157, 157, 157), border
                .getHighlightColor(c1));
        assertEquals("Highlight color coinsides", new Color(14, 14, 14), border
                .getHighlightColor(c2));
    }

    /*
     * Class under test for Color getShadowColor()
     */
    public void testGetShadowColor() {
        Color shadowColor = Color.YELLOW;
        Color highlightedColor = Color.RED;
        EtchedBorder border = new EtchedBorder(highlightedColor, shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        shadowColor = Color.GREEN;
        highlightedColor = Color.WHITE;
        border = new EtchedBorder(highlightedColor, shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
    }

    /*
     * Class under test for Color getHighlightColor()
     */
    public void testGetHighlightColor() {
        Color shadowColor = Color.YELLOW;
        Color highlightedColor = Color.RED;
        EtchedBorder border = new EtchedBorder(highlightedColor, shadowColor);
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        shadowColor = Color.GREEN;
        highlightedColor = Color.WHITE;
        border = new EtchedBorder(highlightedColor, shadowColor);
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
    }

    public void testGetEtchType() {
        int etchType = EtchedBorder.LOWERED;
        EtchedBorder border = new EtchedBorder(etchType);
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
        etchType = EtchedBorder.RAISED;
        border = new EtchedBorder(etchType);
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
    }

    public void testReadWriteObject() throws Exception {
        Color shadowColor1 = Color.RED;
        Color shadowColor2 = Color.BLUE;
        Color highlightedColor1 = Color.YELLOW;
        Color highlightedColor2 = Color.GREEN;
        int etchType1 = EtchedBorder.LOWERED;
        int etchType2 = EtchedBorder.RAISED;
        EtchedBorder border1 = new EtchedBorder(etchType1, highlightedColor1, shadowColor1);
        EtchedBorder border2 = new EtchedBorder(etchType2, highlightedColor2, shadowColor2);
        EtchedBorder resurrectedBorder = (EtchedBorder) serializeObject(border1);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coinsides", resurrectedBorder.getEtchType(), border1
                .getEtchType());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getShadowColor(),
                border1.getShadowColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getHighlightColor(),
                border1.getHighlightColor());
        resurrectedBorder = (EtchedBorder) serializeObject(border2);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coinsides", resurrectedBorder.getEtchType(), border2
                .getEtchType());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getShadowColor(),
                border2.getShadowColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getHighlightColor(),
                border2.getHighlightColor());
    }
}
