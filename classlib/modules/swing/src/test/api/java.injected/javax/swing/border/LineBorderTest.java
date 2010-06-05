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
 * Created on 30.11.2004

 */
package javax.swing.border;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class LineBorderTest extends SwingTestCase {
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(LineBorderTest.class);
    }

    /*
     * Class under test for Insets getBorderInsets(Component, Insets)
     */
    public void testGetBorderInsetsComponentInsets() {
        int thickness1 = 11;
        int thickness2 = 22;
        int thickness3 = 33;
        int thickness4 = 44;
        LineBorder border = new LineBorder(Color.black, thickness1);
        Insets insets = new Insets(1, 1, 1, 1);
        JPanel panel = new JPanel();
        panel.setBorder(new LineBorder(Color.black, thickness2));
        border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", thickness1, insets.top);
        assertEquals("insets values coinside", thickness1, insets.left);
        assertEquals("insets values coinside", thickness1, insets.right);
        assertEquals("insets values coinside", thickness1, insets.bottom);
        insets = new Insets(thickness3, thickness3, thickness3, thickness3);
        panel.setBorder(new LineBorder(Color.black, thickness4));
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
        int thickness = 11;
        LineBorder border = new LineBorder(Color.black, thickness);
        Insets insets = null;
        JPanel panel = new JPanel();
        panel.setBorder(new LineBorder(Color.white));
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", thickness, insets.top);
        assertEquals("insets values coinside", thickness, insets.left);
        assertEquals("insets values coinside", thickness, insets.right);
        assertEquals("insets values coinside", thickness, insets.bottom);
        panel.setBorder(null);
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", thickness, insets.top);
        assertEquals("insets values coinside", thickness, insets.left);
        assertEquals("insets values coinside", thickness, insets.right);
        assertEquals("insets values coinside", thickness, insets.bottom);
    }

    public void testPaintBorder() {
        //        JPanel panel = new JPanel();
        //
        //        panel.setBorder(new LineBorder(Color.red, 10, true));
        //        JFrame frame = new JFrame();
        //        frame.getContentPane().add(panel);
        //        panel.setPreferredSize(new Dimension(100, 50));
        //        frame.pack();
        //        frame.show();
        //        while(!frame.isActive());
        //        while(frame.isActive());
    }

    public void testIsBorderOpaque() {
        LineBorder border1 = new LineBorder(Color.black);
        LineBorder border2 = new LineBorder(Color.black, 11, true);
        LineBorder border3 = new LineBorder(Color.black, 13, false);
        assertTrue("LineBorder is opaque ", border1.isBorderOpaque());
        assertFalse("LineBorder with round corners is not opaque ", border2.isBorderOpaque());
        assertTrue("LineBorder is opaque ", border3.isBorderOpaque());
    }

    /*
     * Class under test for void LineBorder(Color, int, boolean)
     */
    public void testLineBorderColorintboolean() {
        int thickness = 11;
        boolean roundedCorners = true;
        Color color = Color.yellow;
        LineBorder border = new LineBorder(color, thickness, roundedCorners);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
        thickness = 23;
        roundedCorners = false;
        color = Color.red;
        border = new LineBorder(color, thickness, roundedCorners);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
        thickness = 37;
        roundedCorners = true;
        color = Color.cyan;
        border = new LineBorder(color, thickness, roundedCorners);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
    }

    /*
     * Class under test for void LineBorder(Color, int)
     */
    public void testLineBorderColorint() {
        int thickness = 11;
        boolean roundedCorners = false;
        Color color = Color.yellow;
        LineBorder border = new LineBorder(color, thickness);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
        thickness = 23;
        color = Color.red;
        border = new LineBorder(color, thickness);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
        thickness = 37;
        color = Color.cyan;
        border = new LineBorder(color, thickness);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
    }

    /*
     * Class under test for void LineBorder(Color)
     */
    public void testLineBorderColor() {
        int thickness = 1;
        boolean roundedCorners = false;
        Color color = Color.yellow;
        LineBorder border = new LineBorder(color);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
        color = Color.red;
        border = new LineBorder(color);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
        color = Color.cyan;
        border = new LineBorder(color);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
    }

    public void testGetLineColor() {
        Color color = Color.yellow;
        LineBorder border = new LineBorder(color, 1, true);
        assertEquals("Colors coinsides", color, border.getLineColor());
        color = Color.red;
        border = new LineBorder(color, 10, false);
        assertEquals("Colors coinsides", color, border.getLineColor());
        color = Color.cyan;
        border = new LineBorder(color, 110);
        assertEquals("Colors coinsides", color, border.getLineColor());
    }

    public void testGetRoundedCorners() {
        boolean roundedCorners = true;
        LineBorder border = new LineBorder(Color.black, 1, roundedCorners);
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        roundedCorners = false;
        border = new LineBorder(Color.black, 2, roundedCorners);
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        border = new LineBorder(Color.black, 1);
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
    }

    public void testGetThickness() {
        int thickness = 11;
        LineBorder border = new LineBorder(Color.black, thickness);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        thickness = 23;
        border = new LineBorder(Color.black, thickness, true);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        thickness = 37;
        border = new LineBorder(Color.black, thickness, false);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
    }

    public void testCreateGrayLineBorder() {
        LineBorder border = (LineBorder) LineBorder.createGrayLineBorder();
        assertEquals("Thickness coinsides", 1, border.getThickness());
        assertFalse("RoundedCorners coinsides", border.getRoundedCorners());
        assertEquals("Colors coinsides", Color.gray, border.getLineColor());
    }

    public void testCreateBlackLineBorder() {
        LineBorder border = (LineBorder) LineBorder.createBlackLineBorder();
        assertEquals("Thickness coinsides", 1, border.getThickness());
        assertFalse("RoundedCorners coinsides", border.getRoundedCorners());
        assertEquals("Colors coinsides", Color.black, border.getLineColor());
    }

    public void testReadWriteObject() throws Exception {
        LineBorder border1 = new LineBorder(Color.red, 33, false);
        LineBorder border2 = new LineBorder(Color.yellow, 47, true);
        LineBorder resurrectedBorder = (LineBorder) serializeObject(border1);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coinsides", resurrectedBorder.getThickness(), border1
                .getThickness());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getLineColor(), border1
                .getLineColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getRoundedCorners(),
                border1.getRoundedCorners());
        resurrectedBorder = (LineBorder) serializeObject(border2);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coinsides", resurrectedBorder.getThickness(), border2
                .getThickness());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getLineColor(), border2
                .getLineColor());
        assertEquals("Deserialized values coinsides", resurrectedBorder.getRoundedCorners(),
                border2.getRoundedCorners());
    }
}
