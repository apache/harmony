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
 * Created on 01.12.2004

 */
package javax.swing.border;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class CompoundBorderTest extends SwingTestCase {
    /*
     * Class under test for Insets getBorderInsets(Component, Insets)
     */
    public void testGetBorderInsetsComponentInsets() {
        LineBorder border1 = new LineBorder(Color.red, 10, false);
        LineBorder border2 = new LineBorder(Color.red, 3, false);
        CompoundBorder border4 = new CompoundBorder(border1, border2);
        Insets insets = new Insets(1, 1, 1, 1);
        JPanel panel = new JPanel();
        panel.setBorder(new LineBorder(Color.black, 10001));
        border4.getBorderInsets(panel, insets);
        assertEquals("insets values coincide", 13, insets.top);
        assertEquals("insets values coincide", 13, insets.left);
        assertEquals("insets values coincide", 13, insets.right);
        assertEquals("insets values coincide", 13, insets.bottom);
        insets = new Insets(1001, 1002, 1003, 1004);
        panel.setBorder(new LineBorder(Color.black, 100001));
        Insets newInsets = border4.getBorderInsets(panel, insets);
        assertEquals("insets values coincide", 13, newInsets.top);
        assertEquals("insets values coincide", 13, newInsets.left);
        assertEquals("insets values coincide", 13, newInsets.right);
        assertEquals("insets values coincide", 13, newInsets.bottom);
        assertEquals("insets values coincide", 13, insets.top);
        assertEquals("insets values coincide", 13, insets.left);
        assertEquals("insets values coincide", 13, insets.right);
        assertEquals("insets values coincide", 13, insets.bottom);
    }

    /*
     * Class under test for Insets getBorderInsets(Component)
     */
    public void testGetBorderInsetsComponent() {
        LineBorder border1 = new LineBorder(Color.red, 10, false);
        LineBorder border2 = new LineBorder(Color.red, 3, false);
        EmptyBorder border3 = new EmptyBorder(101, 102, 103, 104);
        CompoundBorder border4 = new CompoundBorder(border1, border2);
        CompoundBorder border5 = new CompoundBorder(border2, border3);
        CompoundBorder border6 = new CompoundBorder();
        CompoundBorder border7 = new CompoundBorder(border2, null);
        CompoundBorder border8 = new CompoundBorder(null, border3);
        Insets insets1 = new Insets(13, 13, 13, 13);
        Insets insets2 = new Insets(104, 105, 106, 107);
        Insets insets3 = new Insets(0, 0, 0, 0);
        Insets insets4 = new Insets(3, 3, 3, 3);
        Insets insets5 = new Insets(101, 102, 103, 104);
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(1000, 1001, 1002, 1003));
        assertEquals("Insets coincide ", insets1, border4.getBorderInsets(null));
        assertEquals("Insets coincide ", insets1, border4.getBorderInsets(panel));
        assertEquals("Insets coincide ", insets2, border5.getBorderInsets(null));
        assertEquals("Insets coincide ", insets2, border5.getBorderInsets(panel));
        assertEquals("Insets coincide ", insets3, border6.getBorderInsets(null));
        assertEquals("Insets coincide ", insets3, border6.getBorderInsets(panel));
        assertEquals("Insets coincide ", insets4, border7.getBorderInsets(null));
        assertEquals("Insets coincide ", insets4, border7.getBorderInsets(panel));
        assertEquals("Insets coincide ", insets5, border8.getBorderInsets(null));
        assertEquals("Insets coincide ", insets5, border8.getBorderInsets(panel));
    }

    public void testPaintBorder() {
        //        JPanel panel1 = new JPanel();
        //        JPanel panel2 = new JPanel();
        //        JPanel panel3 = new JPanel();
        //        JPanel panel4 = new JPanel();
        //
        //        LineBorder border1 = new LineBorder(Color.red, 10, false);
        //        LineBorder border2 = new LineBorder(Color.yellow, 30, true);
        //        CompoundBorder border3 = new CompoundBorder(border1, border2);
        //        CompoundBorder border4 = new CompoundBorder(null, border2);
        //        CompoundBorder border5 = new CompoundBorder(border1, null);
        //        panel2.setBorder(border3);
        //        panel3.setBorder(border4);
        //        panel4.setBorder(border5);
        //        panel2.setPreferredSize(new Dimension(200, 150));
        //        panel3.setPreferredSize(new Dimension(200, 150));
        //        panel4.setPreferredSize(new Dimension(200, 150));
        //
        //        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        //        panel1.add(panel2);
        //        panel1.add(panel3);
        //        panel1.add(panel4);
        //
        //        JFrame frame = new JFrame();
        //        frame.getContentPane().add(panel1);
        //        frame.pack();
        //        frame.show();
        //        while(!frame.isActive());
        //        while(frame.isActive());
    }

    public void testIsBorderOpaque() {
        LineBorder border1 = new LineBorder(Color.red, 33, false);
        LineBorder border2 = new LineBorder(Color.red, 33, false);
        EmptyBorder border3 = new EmptyBorder(1, 1, 1, 1);
        CompoundBorder border4 = new CompoundBorder(border1, border2);
        CompoundBorder border5 = new CompoundBorder(border2, border3);
        CompoundBorder border6 = new CompoundBorder();
        CompoundBorder border7 = new CompoundBorder(border2, null);
        CompoundBorder border8 = new CompoundBorder(null, border3);
        assertTrue("This border is opaque", border4.isBorderOpaque());
        assertFalse("This border is not opaque", border5.isBorderOpaque());
        assertTrue("This border is opaque", border6.isBorderOpaque());
        assertTrue("This border is opaque", border7.isBorderOpaque());
        assertFalse("This border is not opaque", border8.isBorderOpaque());
    }

    /*
     * Class under test for void CompoundBorder(Border, Border)
     */
    public void testCompoundBorderBorderBorder() {
        LineBorder border1 = new LineBorder(Color.red, 33, false);
        LineBorder border2 = new LineBorder(Color.red, 33, true);
        EmptyBorder border3 = new EmptyBorder(1, 1, 1, 1);
        CompoundBorder border4 = new CompoundBorder(border1, border2);
        assertSame(border1, border4.getOutsideBorder());
        assertSame(border2, border4.getInsideBorder());
        CompoundBorder border5 = new CompoundBorder(border2, border3);
        assertSame(border2, border5.getOutsideBorder());
        assertSame(border3, border5.getInsideBorder());
        CompoundBorder border7 = new CompoundBorder(border2, null);
        assertSame(border2, border7.getOutsideBorder());
        assertNull(border7.getInsideBorder());
        CompoundBorder border8 = new CompoundBorder(null, border3);
        assertNull(border8.getOutsideBorder());
        assertSame(border3, border8.getInsideBorder());
    }

    /*
     * Class under test for void CompoundBorder()
     */
    public void testCompoundBorder() {
        CompoundBorder border = new CompoundBorder();
        assertNull(border.getInsideBorder());
        assertNull(border.getOutsideBorder());
    }

    public void testGetOutsideBorder() {
        LineBorder border1 = new LineBorder(Color.red, 33, false);
        LineBorder border2 = new LineBorder(Color.red, 33, true);
        EmptyBorder border3 = new EmptyBorder(1, 1, 1, 1);
        CompoundBorder border4 = new CompoundBorder(border1, border2);
        CompoundBorder border5 = new CompoundBorder(border2, border3);
        CompoundBorder border6 = new CompoundBorder();
        CompoundBorder border7 = new CompoundBorder(border2, null);
        CompoundBorder border8 = new CompoundBorder(null, border3);
        assertEquals("Borders coincide", border1, border4.getOutsideBorder());
        assertEquals("Borders coincide", border2, border5.getOutsideBorder());
        assertNull("Borders coincide", border6.getOutsideBorder());
        assertEquals("Borders coincide", border2, border7.getOutsideBorder());
        assertNull("Borders coincide", border8.getOutsideBorder());
    }

    public void testGetInsideBorder() {
        LineBorder border1 = new LineBorder(Color.red, 33, false);
        LineBorder border2 = new LineBorder(Color.red, 33, true);
        EmptyBorder border3 = new EmptyBorder(1, 1, 1, 1);
        CompoundBorder border4 = new CompoundBorder(border1, border2);
        CompoundBorder border5 = new CompoundBorder(border2, border3);
        CompoundBorder border6 = new CompoundBorder();
        CompoundBorder border7 = new CompoundBorder(border2, null);
        CompoundBorder border8 = new CompoundBorder(null, border3);
        assertEquals("Borders coincide", border2, border4.getInsideBorder());
        assertEquals("Borders coincide", border3, border5.getInsideBorder());
        assertNull("Borders coincide", border6.getInsideBorder());
        assertNull("Borders coincide", border7.getInsideBorder());
        assertEquals("Borders coincide", border3, border8.getInsideBorder());
    }

    public void testReadWriteObject() throws Exception {
        LineBorder border1 = new LineBorder(Color.red, 33, false);
        EmptyBorder border2 = new EmptyBorder(1, 1, 1, 1);
        CompoundBorder border3 = new CompoundBorder(border1, border2);
        CompoundBorder border4 = new CompoundBorder();
        CompoundBorder resurrectedBorder = (CompoundBorder) serializeObject(border3);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coincides", resurrectedBorder.getOutsideBorder()
                .getClass(), border1.getClass());
        assertEquals("Deserialized values coincides", resurrectedBorder.getInsideBorder()
                .getClass(), border2.getClass());
        resurrectedBorder = (CompoundBorder) serializeObject(border4);
        assertNotNull(resurrectedBorder);
        assertNull("Deserialized values coincides", resurrectedBorder.getOutsideBorder());
        assertNull("Deserialized values coincides", resurrectedBorder.getInsideBorder());
    }
}
