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
 * Created on 01.05.2005

 */
package org.apache.harmony.x.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

/**
 * The storage of different utility methods used for buttons drawing.
 */
public class ButtonCommons {
    private static final int[][] CHECK_COORDINATES = {{1, 3, 8, 8, 3, 1},
                                                      {4, 6, 1, 3, 8, 6}};

    private static final Insets INSETS = new Insets(0, 0, 0, 0);

    /**
     * Paints focus border at the specified bounds.
     *
     * @param g Graphics to paint on
     * @param focusRect Rectangle which specifies the focus border location
     * @param color Color to paint focus
     */
    public static void paintFocus(final Graphics g,
                                  final Rectangle focusRect,
                                  final Color color) {
        Color oldColor = g.getColor();
        g.setColor(color);
        g.drawRect(focusRect.x - 1, focusRect.y - 1, focusRect.width, focusRect.height);
        g.setColor(oldColor);
    }

    /**
     * Paints button in the pressed state (push buttons and toggle-buttons).
     *
     * @param g Graphics to paint on
     * @param button AbstractButton to be painted as pressed
     * @param color Color of the button in the pressed state
     */
    public static void paintPressed(final Graphics g, final AbstractButton button,
                                    final Color color) {
        Color oldColor = g.getColor();
        Dimension buttonSize = button.getSize();
        g.setColor(color);
        g.fillRect(0, 0, buttonSize.width, buttonSize.height);
        g.setColor(oldColor);
    }

    /**
     * Paints text on a button.
     *
     * @param g Graphics to paint on
     * @param b AbstractButton to be painted
     * @param textRect Rectangle bounding the text location
     * @param clippedText String to be displayed on the button surface (should be trimmed by the bouding rectangle)
     * @param color Color of the text
     */
    public static void paintText(final Graphics g, final AbstractButton b,
                                 final Rectangle textRect, final String clippedText,
                                 final Color color) {
        paintText(g, b.getFontMetrics(b.getFont()), b.getText(),
                  b.getDisplayedMnemonicIndex(), textRect, clippedText,
                  color);
    }

    /**
     * Paints text on the specified Graphics in the specified location.
     *
     * @param g Graphics to paint on
     * @param fm FontMetrics set for the button
     * @param text String representing the original (not-trimmed) button text (AbstractButton.getText())
     * @param displayedMnemonicIndex int value specifing an index of a 'mnemonical char', i.e. index in the text to be underscored.
     *                                 -1 if no mnemonic is set
     * @param textRect Rectangle bounding the text location
     * @param clippedText String to be displayed on the button surface (should be trimmed by the bouding rectangle)
     * @param color Color of the text
     */
    public static void paintText(final Graphics g,
                                 final FontMetrics fm, final String text,
                                 final int displayedMnemonicIndex,
                                 final Rectangle textRect, final String clippedText,
                                 final Color color) {
        int underscore = Utilities.getClippedUnderscoreIndex(text, clippedText,
                displayedMnemonicIndex);
        Utilities.drawString(g, clippedText, textRect.x,
                             Utilities.getTextY(fm, textRect), fm,
                             color, underscore);
    }

    /**
     * Calculates the preferred button size.
     *
     * @param button AbstractButton for which the preferred size to be calculated
     * @param defaultIcon Icon which is set for the button when calculating its preferred size.
     *                      Null if no icon is set. Usually that is AbstractButton.getIcon()
     * @param textIconGap int value representing the gap between text and icon (ususally AbstractButton.getIconTextGap())
     * @return Dimension of the button preferred size
     */
    public static Dimension getPreferredSize(final AbstractButton button,
                                             final Icon defaultIcon,
                                             final int textIconGap) {
        Icon icon = (button.getIcon() != null) ? button.getIcon() :
            defaultIcon;

        Dimension size = Utilities.getCompoundLabelSize(button,
            button.getText(), icon, button.getVerticalTextPosition(),
            button.getHorizontalTextPosition(), textIconGap);

        return Utilities.addInsets(size, button.getInsets(INSETS));
    }

    /**
     * Calculates the preferred button size.
     *
     * @param button AbstractButton for which the preferred size to be calculated
     * @param defaultIcon Icon which is set for the button when calculating its preferred size.
     *                      Null if no icon is set. Usually that is AbstractButton.getIcon()
     * @return Dimension of the button preferred size
     */
    public static Dimension getPreferredSize(final AbstractButton button, final Icon defaultIcon) {
        return getPreferredSize(button, defaultIcon, button.getIconTextGap());
    }

    /**
     * Calculate button layout and clipping text.
     *
     * @param button AbstractButton for which parameters to be calculated
     * @param viewR Rectangle which will be initialized as button local bounds
     * @param iconR Rectangle which will be initialized as icon bounds
     * @param textR Rectangle which will be initialized as text bounds
     * @param icon Icon which is set to button (ususally that is AbstractButton.getIcon()) or null
     * @param leftInset int value representing button left inset (border) if any
     * @param rightInset int value representing button right inset (border) if any
     * @return String which represent the text to be displayed in the button (button label
     *         clipped and complemented with "..." if required)
     */
    public static String getPaintingParameters(final AbstractButton button, final Rectangle viewR,
                                               final Rectangle iconR, final Rectangle textR,
                                               final Icon icon,
                                               final int leftInset, final int rightInset) {
        Insets borderInsets = button.getInsets(INSETS);
        borderInsets.left += leftInset;
        borderInsets.right += rightInset;

        viewR.setBounds(0, 0, button.getWidth(), button.getHeight());
        Utilities.subtractInsets(viewR, borderInsets);
        FontMetrics fm = button.getFontMetrics(button.getFont());
        return SwingUtilities.layoutCompoundLabel(button, fm, button.getText(), icon,
                                                  button.getVerticalAlignment(),
                                                  button.getHorizontalAlignment(),
                                                  button.getVerticalTextPosition(),
                                                  button.getHorizontalTextPosition(),
                                                  viewR, iconR, textR, button.getIconTextGap());
    }

    /**
     * Calculate button layout and clipping text.
     *
     * @param button AbstractButton for which parameters to be calculated
     * @param viewR Rectangle which will be initialized as button local bounds
     * @param iconR Rectangle which will be initialized as icon bounds
     * @param textR Rectangle which will be initialized as text bounds
     * @param icon Icon which is set to button (ususally that is AbstractButton.getIcon()) or null
     * @return String which represent the text to be displayed in the button (button label clipped and complemented with "..." if required)
     */
    public static String getPaintingParameters(final AbstractButton button, final Rectangle viewR,
                                               final Rectangle iconR, final Rectangle textR,
                                               final Icon icon) {
        return getPaintingParameters(button, viewR, iconR, textR, icon, 0, 0);
    }

    /**
     * Retrieves 'current' button icon - icon which is set for the current button state (enabled/disabled/pressed).
     *
     * @param button AbstractButton for which icon should be calculated.
     * @return Icon or null if there is no icon for the button.
     */
    public static Icon getCurrentIcon(final AbstractButton button) {
        Icon icon = null;

        final ButtonModel model = button.getModel();
        if (model.isEnabled()) {
            if (model.isArmed()) {
                icon = button.getPressedIcon();
            } else if (model.isRollover()){
                icon = model.isSelected() ? button.getRolloverSelectedIcon()
                        : button.getRolloverIcon();
            } else if (model.isSelected()) {
                icon = button.getSelectedIcon();
            }
        } else {
            icon = model.isSelected() ? button.getDisabledSelectedIcon()
                    : button.getDisabledIcon();
        }
        if (icon == null) {
            icon = button.getIcon();
        }

        return icon;
    }

    /**
     * Draws 'check' icon at the specified location. Used for check boxes.
     *
     * @param g Graphics to draw on
     * @param color Color of the check
     * @param x int value representing the icon's x-location
     * @param y int value representing the icon's y-location
     */
    public static void drawCheck(final Graphics g, final Color color, final int x, final int y) {
        g.setColor(color);
        g.translate(x, y);
        g.fillPolygon(CHECK_COORDINATES[0], CHECK_COORDINATES[1], CHECK_COORDINATES[0].length);
        g.translate(-x, -y);
    }

    /**
     * Decides which bounds should be used for drawing focus.
     * Usually the choice is done among button bounds (rect1), button text bounds (rect2) and button icon bounds (rect3).
     * The order is the following: if rect1 is apecified (and not empty) rect1 is returned.
     * Otherwise if rect2 is specified and not empty rect2 is returned.
     * Otherwise rect3 is returned.
     *
     * @param rect1 Rectangle of the most preferrable focus bounds
     * @param rect2 Rectangle of the next preferrable focus bounds
     * @param rect3 Rectangle of the least preferrable focus bounds
     * @return Rectangle of the focus bounds
     */
    public static Rectangle getFocusRect(final Rectangle rect1, final Rectangle rect2, final Rectangle rect3) {
        if (rect1 != null && !rect1.isEmpty()) {
            return rect1;
        }
        if (rect2 != null && !rect2.isEmpty()) {
            return rect2;
        }

        return rect3;
    }
}
