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
 * Created on 27.04.2005

 */
package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import org.apache.harmony.x.swing.ButtonCommons;


public class MetalToggleButtonUI extends BasicToggleButtonUI {

    protected Color focusColor;
    protected Color selectColor;
    protected Color disabledTextColor;

    private static MetalToggleButtonUI metalToggleButtonUI;

    public static ComponentUI createUI(final JComponent b) {
        if (metalToggleButtonUI == null) {
            metalToggleButtonUI = new MetalToggleButtonUI();
        }
        return metalToggleButtonUI;
    }

    public void installDefaults(final AbstractButton b) {
        super.installDefaults(b);

        if ((selectColor == null) || (selectColor instanceof UIResource)) {
            selectColor = UIManager.getColor(getPropertyPrefix() + "select");
        }
        if ((disabledTextColor == null) || (disabledTextColor instanceof UIResource)) {
            disabledTextColor = UIManager.getColor(getPropertyPrefix() + "disabledText");
        }
        if ((focusColor == null) || (focusColor instanceof UIResource)) {
            focusColor = UIManager.getColor(getPropertyPrefix() + "focus");
        }
    }

    protected void uninstallDefaults(final AbstractButton b) {
        LookAndFeel.uninstallBorder(b);
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

    protected void paintButtonPressed(final Graphics g,
                                      final AbstractButton button) {
        if ((button.getModel().isArmed() || button.getModel().isSelected())
                && button.isContentAreaFilled()) {
            Color color = (getSelectColor() != null) ? getSelectColor() :
                button.getBackground().darker();
            ButtonCommons.paintPressed(g, button, color);
        }
    }

    protected void paintText(final Graphics g, final JComponent c,
                             final Rectangle textRect, final String text) {
        AbstractButton b = (AbstractButton) c;
        Color color = b.isEnabled() ? b.getForeground() :
            b.isSelected() ? UIManager.getColor("Button.background").darker() :
                getDisabledTextColor();
        ButtonCommons.paintText(g, b, textRect, text, color);
    }

    protected void paintFocus(final Graphics g, final AbstractButton b,
                              final Rectangle viewRect, final Rectangle textRect,
                              final Rectangle iconRect) {
        Color color = (getFocusColor() != null) ? getFocusColor() :
            UIManager.getDefaults().getColor("activeCaptionBorder");
        ButtonCommons.paintFocus(g,
                                 ButtonCommons.getFocusRect(viewRect, textRect, iconRect),
                                 color);
    }

}
