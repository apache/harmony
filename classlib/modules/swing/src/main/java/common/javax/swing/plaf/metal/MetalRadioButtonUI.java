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
 * Created on 29.04.2005

 */
package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicRadioButtonUI;

import org.apache.harmony.x.swing.ButtonCommons;


public class MetalRadioButtonUI extends BasicRadioButtonUI {

    protected Color focusColor;
    protected Color selectColor;
    protected Color disabledTextColor;
    private static Color defaultFocusColor;

    private static MetalRadioButtonUI metalRadioButtonUI;

    public static ComponentUI createUI(final JComponent c) {
        if (metalRadioButtonUI == null) {
            metalRadioButtonUI = new MetalRadioButtonUI();
        }
        return metalRadioButtonUI;
    }

    public void installDefaults(final AbstractButton b) {
        super.installDefaults(b);

        if ((disabledTextColor == null) || (disabledTextColor instanceof UIResource)) {
            disabledTextColor = UIManager.getColor(getPropertyPrefix() + "disabledText");
        }
        if ((focusColor == null) || (focusColor instanceof UIResource)) {
            focusColor = UIManager.getColor(getPropertyPrefix() + "focus");
        }
        if ((selectColor == null) || (selectColor instanceof UIResource)) {
            selectColor = UIManager.getColor(getPropertyPrefix() + "select");
        }
        if (defaultFocusColor == null) {
            defaultFocusColor = UIManager.getDefaults().getColor("activeCaptionBorder");
        }
    }

    protected Color getSelectColor() {
        return selectColor;
    }

    protected Color getDisabledTextColor() {
        return disabledTextColor;
    }

    protected Color getFocusColor() {
        return focusColor;
    }

    public void paint(final Graphics g, final JComponent c) {
        AbstractButton button = (AbstractButton)c;
        Rectangle viewR = new Rectangle();
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        Icon icon = ButtonCommons.getCurrentIcon(button);
        if (icon == null) {
            icon = getDefaultIcon();
        }

        String clippedText = ButtonCommons.getPaintingParameters(button, viewR, iconR, textR, icon);
        if (icon != null) {
            icon.paintIcon(button, g, iconR.x, iconR.y);
        }
        textR.x += getTextShiftOffset();
        textR.y += getTextShiftOffset();
        paintText(g, button, textR, clippedText, getDisabledTextColor());
        if (button.isEnabled() && button.isFocusPainted() && button.isFocusOwner()) {
            paintFocus(g, ButtonCommons.getFocusRect(textR, viewR, iconR), null);
        }
    }

    protected void paintFocus(final Graphics g, final Rectangle t, final Dimension d) {
        Color color = (focusColor != null) ? focusColor : defaultFocusColor;
        ButtonCommons.paintFocus(g, t, color);
    }

    private void paintText(final Graphics g, final AbstractButton b,
                                           final Rectangle textRect, final String text, final Color disabledTextColor) {
        Color color = b.isEnabled() ? b.getForeground() : disabledTextColor;
        ButtonCommons.paintText(g, b, textRect, text, color);
    }

}
