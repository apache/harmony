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
 * Created on 02.12.2004

 */
package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;

public class TitledBorderTest extends SwingTestCase {
    protected JComponent panel;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            public FontMetrics getFontMetrics(Font font) {
                return TitledBorderTest.this.getFontMetrics(font);
            }
        };
    }

    @Override
    protected void tearDown() throws Exception {
        panel = null;
    }

    /*
     * Class under test for void TitledBorder(Border, String, int, int, Font, Color)
     */
    public void testTitledBorderBorderStringintintFontColor() {
        Color color1 = Color.GREEN;
        Color color2 = null;
        Font font1 = new Font(null, Font.TRUETYPE_FONT, 30);
        Font font2 = null;
        String string1 = "string1";
        String string2 = null;
        Border border3 = new EmptyBorder(1, 1, 1, 1);
        Border border4 = null;
        int just1 = 1;
        int just2 = 2;
        int pos1 = 1;
        int pos2 = 2;
        TitledBorder border1 = new TitledBorder(border3, string1, just1, pos1, font1, color1);
        TitledBorder border2 = new TitledBorder(border4, string2, just2, pos2, font2, color2);
        assertEquals("title field initialized correctly ", string1, border1.getTitle());
        assertEquals("border field initialized correctly ", border3, border1.getBorder());
        assertEquals("color field initialized correctly ", color1, border1.getTitleColor());
        assertEquals("font field initialized correctly ", font1, border1.getTitleFont());
        assertEquals("position field initialized correctly ", pos1, border1.getTitlePosition());
        assertEquals("justification field initialized correctly ", just1, border1
                .getTitleJustification());
        assertEquals("title field initialized correctly ", string2, border2.getTitle());
        assertEquals("border field initialized correctly ", UIManager.getDefaults().getBorder(
                "TitledBorder.border"), border2.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border2.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border2.getTitleFont());
        assertEquals("position field initialized correctly ", pos2, border2.getTitlePosition());
        assertEquals("justification field initialized correctly ", just2, border2
                .getTitleJustification());
    }

    /*
     * Class under test for void TitledBorder(Border, String, int, int, Font)
     */
    public void testTitledBorderBorderStringintintFont() {
        Font font1 = new Font(null, Font.TRUETYPE_FONT, 30);
        Font font2 = null;
        String string1 = "string1";
        String string2 = null;
        Border border3 = new EmptyBorder(1, 1, 1, 1);
        Border border4 = null;
        int just1 = 1;
        int just2 = 2;
        int pos1 = 1;
        int pos2 = 2;
        TitledBorder border1 = new TitledBorder(border3, string1, just1, pos1, font1);
        TitledBorder border2 = new TitledBorder(border4, string2, just2, pos2, font2);
        assertEquals("title field initialized correctly ", string1, border1.getTitle());
        assertEquals("border field initialized correctly ", border3, border1.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border1.getTitleColor());
        assertEquals("font field initialized correctly ", font1, border1.getTitleFont());
        assertEquals("position field initialized correctly ", pos1, border1.getTitlePosition());
        assertEquals("justification field initialized correctly ", just1, border1
                .getTitleJustification());
        assertEquals("title field initialized correctly ", string2, border2.getTitle());
        assertEquals("border field initialized correctly ", UIManager.getDefaults().getBorder(
                "TitledBorder.border"), border2.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border2.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border2.getTitleFont());
        assertEquals("position field initialized correctly ", pos2, border2.getTitlePosition());
        assertEquals("justification field initialized correctly ", just2, border2
                .getTitleJustification());
    }

    /*
     * Class under test for void TitledBorder(Border, String, int, int)
     */
    public void testTitledBorderBorderStringintint() {
        String string1 = "string1";
        String string2 = null;
        Border border3 = new EmptyBorder(1, 1, 1, 1);
        Border border4 = null;
        int just1 = 1;
        int just2 = 2;
        int pos1 = 1;
        int pos2 = 2;
        TitledBorder border1 = new TitledBorder(border3, string1, just1, pos1);
        TitledBorder border2 = new TitledBorder(border4, string2, just2, pos2);
        assertEquals("title field initialized correctly ", string1, border1.getTitle());
        assertEquals("border field initialized correctly ", border3, border1.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border1.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border1.getTitleFont());
        assertEquals("position field initialized correctly ", pos1, border1.getTitlePosition());
        assertEquals("justification field initialized correctly ", just1, border1
                .getTitleJustification());
        assertEquals("title field initialized correctly ", string2, border2.getTitle());
        assertEquals("border field initialized correctly ", UIManager.getDefaults().getBorder(
                "TitledBorder.border"), border2.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border2.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border2.getTitleFont());
        assertEquals("position field initialized correctly ", pos2, border2.getTitlePosition());
        assertEquals("justification field initialized correctly ", just2, border2
                .getTitleJustification());
    }

    /*
     * Class under test for void TitledBorder(Border, String)
     */
    public void testTitledBorderBorderString() {
        String string1 = "string1";
        String string2 = null;
        Border border3 = new EmptyBorder(1, 1, 1, 1);
        Border border4 = null;
        TitledBorder border1 = new TitledBorder(border3, string1);
        TitledBorder border2 = new TitledBorder(border4, string2);
        assertEquals("title field initialized correctly ", string1, border1.getTitle());
        assertEquals("border field initialized correctly ", border3, border1.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border1.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border1.getTitleFont());
        assertEquals("position field initialized correctly ", TitledBorder.TOP, border1
                .getTitlePosition());
        assertEquals("justification field initialized correctly ", TitledBorder.LEADING,
                border1.getTitleJustification());
        assertEquals("title field initialized correctly ", string2, border2.getTitle());
        assertEquals("border field initialized correctly ", UIManager.getDefaults().getBorder(
                "TitledBorder.border"), border2.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border2.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border2.getTitleFont());
        assertEquals("position field initialized correctly ", TitledBorder.TOP, border2
                .getTitlePosition());
        assertEquals("justification field initialized correctly ", TitledBorder.LEADING,
                border2.getTitleJustification());
    }

    /*
     * Class under test for void TitledBorder(Border)
     */
    public void testTitledBorderBorder() {
        Border border3 = new EmptyBorder(1, 1, 1, 1);
        Border border4 = null;
        TitledBorder border1 = new TitledBorder(border3);
        TitledBorder border2 = new TitledBorder(border4);
        assertEquals("title field initialized correctly ", "", border1.getTitle());
        assertEquals("border field initialized correctly ", border3, border1.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border1.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border1.getTitleFont());
        assertEquals("position field initialized correctly ", TitledBorder.TOP, border1
                .getTitlePosition());
        assertEquals("justification field initialized correctly ", TitledBorder.LEADING,
                border1.getTitleJustification());
        assertEquals("title field initialized correctly ", "", border2.getTitle());
        assertEquals("border field initialized correctly ", UIManager.getDefaults().getBorder(
                "TitledBorder.border"), border2.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border2.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border2.getTitleFont());
        assertEquals("position field initialized correctly ", TitledBorder.TOP, border2
                .getTitlePosition());
        assertEquals("justification field initialized correctly ", TitledBorder.LEADING,
                border2.getTitleJustification());
    }

    /*
     * Class under test for void TitledBorder(String)
     */
    public void testTitledBorderString() {
        String string1 = "string1";
        String string2 = null;
        TitledBorder border1 = new TitledBorder(string1);
        TitledBorder border2 = new TitledBorder(string2);
        assertEquals("title field initialized correctly ", string1, border1.getTitle());
        assertEquals("border field initialized correctly ", UIManager.getDefaults().getBorder(
                "TitledBorder.border"), border1.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border1.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border1.getTitleFont());
        assertEquals("position field initialized correctly ", TitledBorder.TOP, border1
                .getTitlePosition());
        assertEquals("justification field initialized correctly ", TitledBorder.LEADING,
                border1.getTitleJustification());
        assertEquals("title field initialized correctly ", string2, border2.getTitle());
        assertEquals("border field initialized correctly ", UIManager.getDefaults().getBorder(
                "TitledBorder.border"), border2.getBorder());
        assertEquals("color field initialized correctly ", UIManager.getDefaults().getColor(
                "TitledBorder.titleColor"), border2.getTitleColor());
        assertEquals("font field initialized correctly ", UIManager.getDefaults().getFont(
                "TitledBorder.font"), border2.getTitleFont());
        assertEquals("position field initialized correctly ", TitledBorder.TOP, border2
                .getTitlePosition());
        assertEquals("justification field initialized correctly ", TitledBorder.LEADING,
                border2.getTitleJustification());
    }

    /*
     * Class under test for Insets getBorderInsets(Component, Insets)
     */
    public void testGetBorderInsetsComponentInsets() {
        Insets insets = new Insets(0, 0, 0, 0);
        Font font1 = new Font(null, Font.BOLD, 10);
        Font font2 = new Font(null, Font.ITALIC, 20);
        Border border1 = new EmptyBorder(10, 10, 10, 10);
        Border border2 = new EmptyBorder(20, 20, 20, 20);
        TitledBorder titledBorder = new TitledBorder("");
        assertEquals("insets sizes coincide ", new Insets(5, 5, 5, 5), titledBorder
                .getBorderInsets(panel, insets));
        titledBorder.setTitle("_");
        assertEquals("insets sizes coincide ", new Insets(41, 5, 5, 5), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitle("_____");
        assertEquals("insets sizes coincide ", new Insets(41, 5, 5, 5), titledBorder
                .getBorderInsets(panel));
        titledBorder.setBorder(border1);
        assertEquals("insets sizes coincide ", new Insets(50, 14, 14, 14), titledBorder
                .getBorderInsets(panel));
        titledBorder.setBorder(border2);
        assertEquals("insets sizes coincide ", new Insets(60, 24, 24, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitleFont(font1);
        assertEquals("insets sizes coincide ", new Insets(54, 24, 24, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitleFont(font2);
        assertEquals("insets sizes coincide ", new Insets(84, 24, 24, 24), titledBorder
                .getBorderInsets(panel));
    }

    /*
     * Class under test for Insets getBorderInsets(Component)
     */
    public void testGetBorderInsetsComponent() {
        Font font1 = new Font(null, Font.BOLD, 10);
        Font font2 = new Font(null, Font.ITALIC, 20);
        Border border1 = new EmptyBorder(10, 10, 10, 10);
        Border border2 = new EmptyBorder(20, 20, 20, 20);
        TitledBorder titledBorder = new TitledBorder("");
        assertEquals("insets sizes coincide ", new Insets(5, 5, 5, 5), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitle("_");
        assertEquals("insets sizes coincide ", new Insets(41, 5, 5, 5), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitle("_____");
        assertEquals("insets sizes coincide ", new Insets(41, 5, 5, 5), titledBorder
                .getBorderInsets(panel));
        titledBorder.setBorder(border1);
        assertEquals("insets sizes coincide ", new Insets(50, 14, 14, 14), titledBorder
                .getBorderInsets(panel));
        titledBorder.setBorder(border2);
        assertEquals("insets sizes coincide ", new Insets(60, 24, 24, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitleFont(font1);
        assertEquals("insets sizes coincide ", new Insets(54, 24, 24, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitleFont(font2);
        assertEquals("insets sizes coincide ", new Insets(84, 24, 24, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitlePosition(TitledBorder.ABOVE_TOP);
        assertEquals("insets sizes coincide ", new Insets(86, 24, 24, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitlePosition(TitledBorder.BELOW_TOP);
        assertEquals("insets sizes coincide ", new Insets(86, 24, 24, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitlePosition(TitledBorder.TOP);
        assertEquals("insets sizes coincide ", new Insets(84, 24, 24, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitlePosition(TitledBorder.ABOVE_BOTTOM);
        assertEquals("insets sizes coincide ", new Insets(24, 24, 86, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitlePosition(TitledBorder.BELOW_BOTTOM);
        assertEquals("insets sizes coincide ", new Insets(24, 24, 86, 24), titledBorder
                .getBorderInsets(panel));
        titledBorder.setTitlePosition(TitledBorder.BOTTOM);
        assertEquals("insets sizes coincide ", new Insets(24, 24, 84, 24), titledBorder
                .getBorderInsets(panel));
    }

    public void testPaintBorder() {
        //        int numJust = 6;
        //        int numPos = 7;
        //        String title = "Title";
        //        JPanel mainPanel = new JPanel();
        //
        //        JFrame frame = new JFrame();
        //        mainPanel.setLayout(new GridLayout(numJust, numPos));
        //        for (int iPanel = 0; iPanel < numJust*numPos; iPanel++) {
        //            Border border = new TitledBorder(new LineBorder(Color.RED, 10), title, iPanel/numPos, iPanel%numPos);
        //            JPanel panel = new JPanel();
        //            panel.setBorder(border);
        //            panel.setPreferredSize(new Dimension(150, 150));
        //            mainPanel.add(panel);
        //        }
        //        frame.getContentPane().add(mainPanel);
        //        frame.pack();
        //        frame.show();
        //        while(!frame.isActive());
        //        while(frame.isActive());
    }

    public void testIsBorderOpaque() {
        Border border1 = new TitledBorder("Text");
        Border border2 = new TitledBorder(new LineBorder(Color.WHITE));
        Border border3 = new TitledBorder(new LineBorder(Color.WHITE), "Text");
        assertFalse("TitledBorder is not opaque ", border1.isBorderOpaque());
        assertFalse("TitledBorder is not opaque ", border2.isBorderOpaque());
        assertFalse("TitledBorder is not opaque ", border3.isBorderOpaque());
    }

    public void testGetFont() {
        class PublicTitledBorder extends TitledBorder {
            private static final long serialVersionUID = 1L;

            PublicTitledBorder(final String title) {
                super(title);
            }

            @Override
            public Font getFont(final Component c) {
                return super.getFont(c);
            }
        }
        ;
        PublicTitledBorder titledBorder = new PublicTitledBorder("hello, border!!");
        Font newFont1 = new Font(null, Font.BOLD, 10);
        Font newFont2 = new Font(null, Font.ITALIC, 20);
        Font newFont3 = new Font(null, Font.TRUETYPE_FONT, 30);
        Font newFont4 = null;
        titledBorder.setTitleFont(newFont1);
        assertEquals("fonts coincide ", newFont1, titledBorder.getFont(null));
        titledBorder.setTitleFont(newFont2);
        assertEquals("fonts coincide ", newFont2, titledBorder.getFont(null));
        titledBorder.setTitleFont(newFont3);
        assertEquals("fonts coincide ", newFont3, titledBorder.getFont(null));
        titledBorder.setTitleFont(newFont4);
        assertFalse("font cannot be null ", titledBorder.getFont(null) == null);
        assertEquals("font cannot be null ", "class javax.swing.plaf.FontUIResource",
                titledBorder.getFont(null).getClass().toString());
    }

    public void testGetMinimumSize() {
        Font font1 = new Font(null, Font.BOLD, 10);
        Font font2 = new Font(null, Font.ITALIC, 20);
        Border border1 = new EmptyBorder(10, 10, 10, 10);
        Border border2 = new EmptyBorder(20, 20, 20, 20);
        TitledBorder titledBorder = new TitledBorder("");
        assertEquals("minimum sizes coincide ", new Dimension(10, 10), titledBorder
                .getMinimumSize(panel));
        titledBorder.setTitle("_");
        assertEquals("minimum sizes coincide ", new Dimension(22, 46), titledBorder
                .getMinimumSize(panel));
        titledBorder.setTitle("_____");
        assertEquals("minimum sizes coincide ", new Dimension(70, 46), titledBorder
                .getMinimumSize(panel));
        titledBorder.setBorder(border1);
        assertEquals("minimum sizes coincide ", new Dimension(88, 64), titledBorder
                .getMinimumSize(panel));
        titledBorder.setBorder(border2);
        assertEquals("minimum sizes coincide ", new Dimension(108, 84), titledBorder
                .getMinimumSize(panel));
        titledBorder.setTitleFont(font1);
        assertEquals("minimum sizes coincide ", new Dimension(98, 78), titledBorder
                .getMinimumSize(panel));
        titledBorder.setTitleFont(font2);
        assertEquals("minimum sizes coincide ", new Dimension(148, 108), titledBorder
                .getMinimumSize(panel));
    }

    /**
     * This method is being tested in testGetBorder()
     */
    public void testSetBorder() {
    }

    public void testGetBorder() {
        TitledBorder titledBorder = new TitledBorder("hello, border!!");
        Border border1 = new EmptyBorder(1, 1, 1, 1);
        Border border2 = new EmptyBorder(2, 2, 2, 2);
        Border border3 = new EmptyBorder(3, 3, 3, 3);
        titledBorder.setBorder(border1);
        assertEquals("borders coincide ", border1, titledBorder.getBorder());
        titledBorder.setBorder(border2);
        assertEquals("borders coincide ", border2, titledBorder.getBorder());
        titledBorder.setBorder(border3);
        assertEquals("borders coincide ", border3, titledBorder.getBorder());
        titledBorder.setBorder(null);
        assertFalse("border cannot be null ", titledBorder.getBorder() == null);
        assertEquals("border cannot be null ",
                "class javax.swing.plaf.BorderUIResource$LineBorderUIResource", titledBorder
                        .getBorder().getClass().toString());
    }

    /**
     * This method is being tested in testGetTitle()
     */
    public void testSetTitle() {
    }

    public void testGetTitle() {
        TitledBorder titledBorder = new TitledBorder("hello, border!!");
        String string1 = "string1";
        String string2 = "string2";
        String string3 = "string3";
        String string4 = null;
        titledBorder.setTitle(string1);
        assertEquals("titles coincide ", string1, titledBorder.getTitle());
        titledBorder.setTitle(string2);
        assertEquals("titles coincide ", string2, titledBorder.getTitle());
        titledBorder.setTitle(string3);
        assertEquals("titles coincide ", string3, titledBorder.getTitle());
        titledBorder.setTitle(string4);
        assertEquals("titles coincide ", string4, titledBorder.getTitle());
    }

    /**
     * This method is being tested in testGetTitleFont()
     */
    public void testSetTitleFont() {
    }

    public void testGetTitleFont() {
        TitledBorder titledBorder = new TitledBorder("hello, border!!");
        Font newFont1 = new Font(null, Font.BOLD, 10);
        Font newFont2 = new Font(null, Font.ITALIC, 20);
        Font newFont3 = new Font(null, Font.TRUETYPE_FONT, 30);
        Font newFont4 = null;
        titledBorder.setTitleFont(newFont1);
        assertEquals("fonts coincide ", newFont1, titledBorder.getTitleFont());
        titledBorder.setTitleFont(newFont2);
        assertEquals("fonts coincide ", newFont2, titledBorder.getTitleFont());
        titledBorder.setTitleFont(newFont3);
        assertEquals("fonts coincide ", newFont3, titledBorder.getTitleFont());
        titledBorder.setTitleFont(newFont4);
        assertFalse("font cannot be null ", titledBorder.getTitleFont() == null);
        assertEquals("font cannot be null ", "class javax.swing.plaf.FontUIResource",
                titledBorder.getTitleFont().getClass().toString());
    }

    /**
     * This method is being tested in testGetTitleColor()
     */
    public void testSetTitleColor() {
    }

    public void testGetTitleColor() {
        TitledBorder titledBorder = new TitledBorder("hello, border!!");
        Color color1 = Color.RED;
        Color color2 = Color.YELLOW;
        Color color3 = Color.GREEN;
        Color color4 = null;
        titledBorder.setTitleColor(color1);
        assertEquals("colors coincide ", color1, titledBorder.getTitleColor());
        titledBorder.setTitleColor(color2);
        assertEquals("colors coincide ", color2, titledBorder.getTitleColor());
        titledBorder.setTitleColor(color3);
        assertEquals("colors coincide ", color3, titledBorder.getTitleColor());
        titledBorder.setTitleColor(color4);
        assertFalse("color cannot be null ", titledBorder.getTitleColor() == null);
        assertEquals("color cannot be null", "class javax.swing.plaf.ColorUIResource",
                titledBorder.getTitleColor().getClass().toString());
    }

    /**
     * This method is being tested in testGetTitlePosition()
     */
    public void testSetTitlePosition() {
    }

    public void testGetTitlePosition() {
        TitledBorder titledBorder = new TitledBorder("hello, border!!");
        int pos1 = 0;
        int pos2 = 3;
        int pos3 = 6;
        int pos4 = 7;
        int pos5 = -1;
        titledBorder.setTitlePosition(pos1);
        assertEquals("title positions coincide ", pos1, titledBorder.getTitlePosition());
        titledBorder.setTitlePosition(pos2);
        assertEquals("title positions coincide ", pos2, titledBorder.getTitlePosition());
        titledBorder.setTitlePosition(pos3);
        assertEquals("title positions coincide ", pos3, titledBorder.getTitlePosition());
        String exText = null;
        try {
            titledBorder.setTitlePosition(pos4);
        } catch (IllegalArgumentException e) {
            exText = e.getMessage();
        }
        assertEquals(pos4 + " is not a valid title position.", exText);
        exText = null;
        try {
            titledBorder.setTitlePosition(pos5);
        } catch (IllegalArgumentException e) {
            exText = e.getMessage();
        }
        assertEquals(pos5 + " is not a valid title position.", exText);
    }

    /**
     * This method is being tested in testGetTitleJustification()
     */
    public void testSetTitleJustification() {
    }

    public void testGetTitleJustification() {
        TitledBorder titledBorder = new TitledBorder("hello, border!!");
        int just1 = 0;
        int just2 = 3;
        int just3 = 5;
        int just4 = -1;
        int just5 = 6;
        titledBorder.setTitleJustification(just1);
        assertEquals("title justifications coincide ", just1, titledBorder
                .getTitleJustification());
        titledBorder.setTitleJustification(just2);
        assertEquals("title justifications coincide ", just2, titledBorder
                .getTitleJustification());
        titledBorder.setTitleJustification(just3);
        assertEquals("title justifications coincide ", just3, titledBorder
                .getTitleJustification());
        String exText = null;
        try {
            titledBorder.setTitleJustification(just4);
        } catch (IllegalArgumentException e) {
            exText = e.getMessage();
        }
        assertEquals(just4 + " is not a valid title justification.", exText);
        exText = null;
        try {
            titledBorder.setTitleJustification(just5);
        } catch (IllegalArgumentException e) {
            exText = e.getMessage();
        }
        assertEquals(just5 + " is not a valid title justification.", exText);
    }

    public void testReadWriteObject() throws Exception {
        Border border3 = new LineBorder(Color.red, 33, false);
        String title4 = "new LineBorder(Color.yellow, 47, true);";
        TitledBorder border1 = new TitledBorder(border3);
        TitledBorder border2 = new TitledBorder(title4);
        TitledBorder resurrectedBorder = (TitledBorder) serializeObject(border1);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coincides", resurrectedBorder.getTitle(), border1
                .getTitle());
        assertEquals("Deserialized values coincides", resurrectedBorder.getTitleColor(),
                border1.getTitleColor());
        assertEquals("Deserialized values coincides", resurrectedBorder.getBorder().getClass(),
                border1.getBorder().getClass());
        resurrectedBorder = (TitledBorder) serializeObject(border2);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coincides", resurrectedBorder.getTitle(), border2
                .getTitle());
        assertEquals("Deserialized values coincides", resurrectedBorder.getTitleColor(),
                border2.getTitleColor());
        assertEquals("Deserialized values coincides", resurrectedBorder.getBorder().getClass(),
                border2.getBorder().getClass());
    }
}
