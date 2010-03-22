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
package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;

import org.apache.harmony.x.swing.Utilities;


public class MetalToolTipUI extends BasicToolTipUI {
    public static final int padSpaceBetweenStrings = 12;

    private JToolTip tooltip;
    private boolean hideAccelerator;

    public static ComponentUI createUI(final JComponent c) {
        return new MetalToolTipUI();
    }

    public void installUI(final JComponent c) {
        super.installUI(c);
        tooltip = (JToolTip)c;
        hideAccelerator = UIManager.getBoolean("ToolTip.hideAccelerator");
    }

    public void uninstallUI(final JComponent c) {
        super.uninstallUI(c);
        hideAccelerator = false;
    }

    public void paint(final Graphics g, final JComponent c) {
        super.paint(g, c);
        if (hideAccelerator) {
            return;
        }
        JComponent component = tooltip.getComponent();

        String acceleratorText = getAcceleratorString();
        Font f = MetalLookAndFeel.getSubTextFont();
        FontMetrics fm = tooltip.getFontMetrics(f);
        Dimension stringSize = Utilities.getStringSize(acceleratorText, fm);

        int textX = tooltip.getWidth() - stringSize.width - padSpaceBetweenStrings / 2;
        int textY = fm.getAscent();
        Utilities.drawString(g, acceleratorText, textX, textY, fm, MetalLookAndFeel.getAcceleratorForeground(), -1);
    }

    public Dimension getPreferredSize(final JComponent c) {
        Dimension result = super.getPreferredSize(c);

        if (!hideAccelerator && tooltip.getComponent() instanceof AbstractButton) {
            result.width += Utilities.getStringSize(getAcceleratorString(), c.getFontMetrics(c.getFont())).width;
            result.width += padSpaceBetweenStrings;
        }

        return result;
    }

    protected boolean isAcceleratorHidden() {
        return hideAccelerator;
    }

    public String getAcceleratorString() {
        if (!(tooltip.getComponent() instanceof AbstractButton)) {
            return "";
        }
        AbstractButton comp = (AbstractButton)tooltip.getComponent();

        if (comp.getMnemonic() == 0) {
            return "";
        }

        return "Alt-" + Utilities.keyCodeToKeyChar(comp.getMnemonic());
    }
}
