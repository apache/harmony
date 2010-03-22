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
 * @author Sergey Burlak
 */
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ToolTipUI;

import org.apache.harmony.x.swing.Utilities;


public class BasicToolTipUI extends ToolTipUI {
    private static BasicToolTipUI toolTipUI;

    private JToolTip tooltip;
    private Color foregroundInactiveColor;
    private Color backgroundInactiveColor;

    public static ComponentUI createUI(final JComponent c) {
        if (toolTipUI == null) {
            toolTipUI = new BasicToolTipUI();
        }

        return toolTipUI;
    }

    public void installUI(final JComponent c) {
        tooltip = (JToolTip)c;

        installDefaults(tooltip);
        installListeners(tooltip);
    }

    public void uninstallUI(final JComponent c){
        installDefaults(tooltip);
        installListeners(tooltip);

        tooltip = null;
    }

    protected void installDefaults(final JComponent c) {
        LookAndFeel.installColorsAndFont(c, "ToolTip.background", "ToolTip.foreground", "ToolTip.font");
        LookAndFeel.installBorder(c, "ToolTip.border");

        backgroundInactiveColor = UIManager.getColor("ToolTip.backgroundInactive");
        foregroundInactiveColor = UIManager.getColor("ToolTip.foregroundInactive");
    }

    protected void uninstallDefaults(final JComponent c) {
        Utilities.uninstallColorsAndFont(c);
        LookAndFeel.uninstallBorder(c);

        backgroundInactiveColor = null;
        foregroundInactiveColor = null;
    }

    protected void installListeners(final JComponent c) {
    }

    protected void uninstallListeners(final JComponent c) {
    }

    public void paint(final Graphics g, final JComponent c) {
        JComponent component = tooltip.getComponent();
        if (component != null && component.isEnabled() || backgroundInactiveColor == null) {
            g.setColor(tooltip.getBackground());
            LookAndFeel.installBorder(c, "ToolTip.border");
        } else {
            g.setColor(backgroundInactiveColor);
            LookAndFeel.installBorder(c, "ToolTip.borderInactive");
        }
        g.fillRect(0, 0, tooltip.getWidth(), tooltip.getHeight());

        String tipText = tooltip.getTipText();
        FontMetrics fm = Utilities.getFontMetrics(tooltip);
        Dimension stringSize = Utilities.getStringSize(tipText, fm);
        int textX = component instanceof AbstractButton && ((AbstractButton)component).getMnemonic() != 0
                        ? 4 : (tooltip.getWidth() - stringSize.width) / 2;
        int textY = fm.getAscent();
        Color foreground = component != null && component.isEnabled() || foregroundInactiveColor == null
                                ? tooltip.getForeground()
                                : foregroundInactiveColor;
        Utilities.drawString(g, tipText, textX, textY, fm, foreground, -1);
    }

    public Dimension getPreferredSize(final JComponent c) {
        if (tooltip == null) {
            tooltip = (JToolTip)c;
        }
        Font f = c.getFont();
        if (f == null) {
            return new Dimension();
        }
        Dimension result = Utilities.getStringSize(tooltip.getTipText(), c.getFontMetrics(f));
        result.width += 6;

        return Utilities.addInsets(result, tooltip.getInsets());
    }

    public Dimension getMinimumSize(final JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMaximumSize(final JComponent c) {
        return getPreferredSize(c);
    }
}
