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
 * Created on 06.12.2004

 */
package javax.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

public class BorderFactoryTest extends SwingTestCase {
    public void testStaticVariablesInitialization() {
        assertTrue("Shared values are initialized", BorderFactory.emptyBorder != null);
        assertTrue("Shared values are initialized", BorderFactory.sharedEtchedBorder != null);
        assertTrue("Shared values are initialized", BorderFactory.sharedLoweredBevel != null);
        assertTrue("Shared values are initialized", BorderFactory.sharedRaisedBevel != null);
        EmptyBorder emptyBorder = (EmptyBorder) BorderFactory.emptyBorder;
        Insets insets = emptyBorder.getBorderInsets();
        assertEquals(insets, new Insets(0, 0, 0, 0));
        int etchType = EtchedBorder.LOWERED;
        Color shadowColor = null;
        Color highlightedColor = null;
        Border border = BorderFactory.sharedEtchedBorder;
        assertEquals("Shadow color coinsides", shadowColor, ((EtchedBorder) border)
                .getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, ((EtchedBorder) border)
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, ((EtchedBorder) border).getEtchType());
        int bevelType = BevelBorder.LOWERED;
        border = BorderFactory.sharedLoweredBevel;
        assertEquals("highlightOuterColor coinsides", highlightedColor, ((BevelBorder) border)
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightedColor, ((BevelBorder) border)
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, ((BevelBorder) border)
                .getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, ((BevelBorder) border)
                .getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, ((BevelBorder) border).getBevelType());
        bevelType = BevelBorder.RAISED;
        border = BorderFactory.sharedRaisedBevel;
        assertEquals("highlightOuterColor coinsides", highlightedColor, ((BevelBorder) border)
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightedColor, ((BevelBorder) border)
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, ((BevelBorder) border)
                .getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, ((BevelBorder) border)
                .getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, ((BevelBorder) border).getBevelType());
    }

    /*
     * Class under test for TitledBorder createTitledBorder(Border, String, int, int, Font, Color)
     */
    public void testCreateTitledBorderBorderStringintintFontColor() {
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
        TitledBorder border1 = BorderFactory.createTitledBorder(border3, string1, just1, pos1,
                font1, color1);
        TitledBorder border2 = BorderFactory.createTitledBorder(border4, string2, just2, pos2,
                font2, color2);
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
     * Class under test for TitledBorder createTitledBorder(Border, String, int, int, Font)
     */
    public void testCreateTitledBorderBorderStringintintFont() {
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
        TitledBorder border1 = BorderFactory.createTitledBorder(border3, string1, just1, pos1,
                font1);
        TitledBorder border2 = BorderFactory.createTitledBorder(border4, string2, just2, pos2,
                font2);
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
     * Class under test for TitledBorder createTitledBorder(Border, String, int, int)
     */
    public void testCreateTitledBorderBorderStringintint() {
        String string1 = "string1";
        String string2 = null;
        Border border3 = new EmptyBorder(1, 1, 1, 1);
        Border border4 = null;
        int just1 = 1;
        int just2 = 2;
        int pos1 = 1;
        int pos2 = 2;
        TitledBorder border1 = BorderFactory.createTitledBorder(border3, string1, just1, pos1);
        TitledBorder border2 = BorderFactory.createTitledBorder(border4, string2, just2, pos2);
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
     * Class under test for TitledBorder createTitledBorder(Border, String)
     */
    public void testCreateTitledBorderBorderString() {
        String string1 = "string1";
        String string2 = null;
        Border border3 = new EmptyBorder(1, 1, 1, 1);
        Border border4 = null;
        TitledBorder border1 = BorderFactory.createTitledBorder(border3, string1);
        TitledBorder border2 = BorderFactory.createTitledBorder(border4, string2);
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
     * Class under test for CompoundBorder createCompoundBorder(Border, Border)
     */
    public void testCreateCompoundBorderBorderBorder() {
        LineBorder border1 = new LineBorder(Color.red, 33, false);
        LineBorder border2 = new LineBorder(Color.red, 33, true);
        EmptyBorder border3 = new EmptyBorder(1, 1, 1, 1);
        CompoundBorder border4 = BorderFactory.createCompoundBorder(border1, border2);
        CompoundBorder border5 = BorderFactory.createCompoundBorder(border2, border3);
        CompoundBorder border7 = BorderFactory.createCompoundBorder(border2, null);
        CompoundBorder border8 = BorderFactory.createCompoundBorder(null, border3);
        assertEquals("border fields coinsides", border1, border4.getOutsideBorder());
        assertEquals("border fields coinsides", border2, border4.getInsideBorder());
        assertEquals("border fields coinsides", border2, border5.getOutsideBorder());
        assertEquals("border fields coinsides", border3, border5.getInsideBorder());
        assertEquals("border fields coinsides", border2, border7.getOutsideBorder());
        assertNull("border fields coinsides", border7.getInsideBorder());
        assertNull("border fields coinsides", border8.getOutsideBorder());
        assertEquals("border fields coinsides", border3, border8.getInsideBorder());
    }

    /*
     * Class under test for TitledBorder createTitledBorder(Border)
     */
    public void testCreateTitledBorderBorder() {
        Border border3 = new EmptyBorder(1, 1, 1, 1);
        Border border4 = null;
        TitledBorder border1 = BorderFactory.createTitledBorder(border3);
        TitledBorder border2 = BorderFactory.createTitledBorder(border4);
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
     * Class under test for TitledBorder createTitledBorder(String)
     */
    public void testCreateTitledBorderString() {
        String string1 = "string1";
        String string2 = null;
        TitledBorder border1 = BorderFactory.createTitledBorder(string1);
        TitledBorder border2 = BorderFactory.createTitledBorder(string2);
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
     * Class under test for MatteBorder createMatteBorder(int, int, int, int, Icon)
     */
    public void testCreateMatteBorderintintintintIcon() {
        Icon icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_GRAY));
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        MatteBorder border = BorderFactory.createMatteBorder(top, left, bottom, right, icon);
        Insets insets = border.getBorderInsets(null);
        assertEquals(insets, new Insets(top, left, bottom, right));
        icon = new ImageIcon(new BufferedImage(30, 40, BufferedImage.TYPE_4BYTE_ABGR));
        top = 200;
        left = 300;
        right = 200;
        bottom = 300;
        border = BorderFactory.createMatteBorder(top, left, bottom, right, icon);
        Insets insets2 = border.getBorderInsets(null);
        assertEquals(insets2, new Insets(top, left, bottom, right));
    }

    /*
     * Class under test for MatteBorder createMatteBorder(int, int, int, int, Color)
     */
    public void testCreateMatteBorderintintintintColor() {
        Color color = Color.RED;
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        MatteBorder border = BorderFactory.createMatteBorder(top, left, bottom, right, color);
        Insets insets = border.getBorderInsets(null);
        assertEquals(insets, new Insets(top, left, bottom, right));
        color = Color.YELLOW;
        top = 200;
        left = 300;
        right = 200;
        bottom = 300;
        border = BorderFactory.createMatteBorder(top, left, bottom, right, color);
        Insets insets2 = border.getBorderInsets(null);
        assertEquals(insets2, new Insets(top, left, bottom, right));
    }

    /*
     * Class under test for Border createLineBorder(Color, int)
     */
    public void testCreateLineBorderColorint() {
        int thickness = 11;
        Color color = Color.yellow;
        LineBorder border = (LineBorder) BorderFactory.createLineBorder(color, thickness);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertFalse("RoundedCorners coinsides", border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
    }

    /*
     * Class under test for Border createLineBorder(Color)
     */
    public void testCreateLineBorderColor() {
        int thickness = 1;
        boolean roundedCorners = false;
        Color color = Color.yellow;
        LineBorder border = (LineBorder) BorderFactory.createLineBorder(color);
        assertEquals("Thickness coinsides", thickness, border.getThickness());
        assertEquals("RoundedCorners coinsides", roundedCorners, border.getRoundedCorners());
        assertEquals("Colors coinsides", color, border.getLineColor());
    }

    /*
     * Class under test for CompoundBorder createCompoundBorder()
     */
    public void testCreateCompoundBorder() {
        CompoundBorder border = BorderFactory.createCompoundBorder();
        assertNull(border.getInsideBorder());
        assertNull(border.getOutsideBorder());
    }

    /*
     * Class under test for Border createEmptyBorder(int, int, int, int)
     */
    public void testCreateEmptyBorderintintintint() {
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        EmptyBorder border = (EmptyBorder) BorderFactory.createEmptyBorder(top, left, bottom,
                right);
        Insets insets = border.getBorderInsets(null);
        assertEquals(insets, new Insets(top, left, bottom, right));
    }

    /*
     * Class under test for Border createEtchedBorder(Color, Color)
     */
    public void testCreateEtchedBorderColorColor() {
        int etchType = EtchedBorder.LOWERED;
        Color shadowColor = Color.YELLOW;
        Color highlightedColor = Color.RED;
        EtchedBorder border = (EtchedBorder) BorderFactory.createEtchedBorder(highlightedColor,
                shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
        shadowColor = Color.GREEN;
        highlightedColor = Color.WHITE;
        border = (EtchedBorder) BorderFactory.createEtchedBorder(highlightedColor, shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
    }

    /*
     * Class under test for Border createEtchedBorder(int, Color, Color)
     */
    public void testCreateEtchedBorderintColorColor() {
        int etchType = EtchedBorder.LOWERED;
        Color shadowColor = Color.YELLOW;
        Color highlightedColor = Color.RED;
        EtchedBorder border = (EtchedBorder) BorderFactory.createEtchedBorder(etchType,
                highlightedColor, shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
        etchType = EtchedBorder.RAISED;
        shadowColor = Color.GREEN;
        highlightedColor = Color.WHITE;
        border = (EtchedBorder) BorderFactory.createEtchedBorder(etchType, highlightedColor,
                shadowColor);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
    }

    /*
     * Class under test for Border createEtchedBorder(int)
     */
    public void testCreateEtchedBorderint() {
        int etchType = EtchedBorder.LOWERED;
        Color shadowColor = null;
        Color highlightedColor = null;
        EtchedBorder border = (EtchedBorder) BorderFactory.createEtchedBorder(etchType);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
        etchType = EtchedBorder.RAISED;
        border = (EtchedBorder) BorderFactory.createEtchedBorder(etchType);
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
    }

    /*
     * Class under test for Border createEtchedBorder()
     */
    public void testCreateEtchedBorder() {
        int etchType = EtchedBorder.LOWERED;
        Color shadowColor = null;
        Color highlightedColor = null;
        EtchedBorder border = (EtchedBorder) BorderFactory.createEtchedBorder();
        assertEquals("Shadow color coinsides", shadowColor, border.getShadowColor());
        assertEquals("Highlighted color coinsides", highlightedColor, border
                .getHighlightColor());
        assertEquals("Etch type coinsides", etchType, border.getEtchType());
    }

    /*
     * Class under test for Border createBevelBorder(int, Color, Color, Color, Color)
     */
    public void testCreateBevelBorderintColorColorColorColor() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightOuterColor = Color.RED;
        Color highlightInnerColor = Color.YELLOW;
        Color shadowOuterColor = Color.GREEN;
        Color shadowInnerColor = Color.BLACK;
        BevelBorder border = (BevelBorder) BorderFactory.createBevelBorder(bevelType,
                highlightOuterColor, highlightInnerColor, shadowOuterColor, shadowInnerColor);
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
        border = (BevelBorder) BorderFactory.createBevelBorder(bevelType, highlightOuterColor,
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
    }

    /*
     * Class under test for Border createBevelBorder(int, Color, Color)
     */
    public void testCreateBevelBorderintColorColor() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightColor = Color.RED;
        Color shadowColor = Color.GREEN;
        BevelBorder border = (BevelBorder) BorderFactory.createBevelBorder(bevelType,
                highlightColor, shadowColor);
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
        border = (BevelBorder) BorderFactory.createBevelBorder(bevelType, highlightColor,
                shadowColor);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
    }

    /*
     * Class under test for Border createBevelBorder(int)
     */
    public void testCreateBevelBorderint() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightColor = null;
        Color shadowColor = null;
        BevelBorder border = (BevelBorder) BorderFactory.createBevelBorder(bevelType);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
        bevelType = BevelBorder.RAISED;
        border = (BevelBorder) BorderFactory.createBevelBorder(bevelType);
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
    }

    public void testCreateRaisedBevelBorder() {
        int bevelType = BevelBorder.RAISED;
        Color highlightColor = null;
        Color shadowColor = null;
        BevelBorder border = (BevelBorder) BorderFactory.createRaisedBevelBorder();
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
    }

    public void testCreateLoweredBevelBorder() {
        int bevelType = BevelBorder.LOWERED;
        Color highlightColor = null;
        Color shadowColor = null;
        BevelBorder border = (BevelBorder) BorderFactory.createLoweredBevelBorder();
        assertEquals("highlightOuterColor coinsides", highlightColor, border
                .getHighlightOuterColor());
        assertEquals("highlightInnerColor coinsides", highlightColor, border
                .getHighlightInnerColor());
        assertEquals("shadowOuterColor coinsides", shadowColor, border.getShadowOuterColor());
        assertEquals("shadowInnerColor coinsides", shadowColor, border.getShadowInnerColor());
        assertEquals("Bevel type coinsides", bevelType, border.getBevelType());
    }

    /*
     * Class under test for Border createEmptyBorder()
     */
    public void testCreateEmptyBorder() {
        Border border = BorderFactory.createEmptyBorder();
        Insets insets = border.getBorderInsets(null);
        assertEquals(insets, new Insets(0, 0, 0, 0));
    }
}
