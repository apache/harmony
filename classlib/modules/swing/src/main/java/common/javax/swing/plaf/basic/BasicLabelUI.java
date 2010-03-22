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
 * @author Anton Avtamonov
 */

package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.LabelUI;

import org.apache.harmony.x.swing.Utilities;


public class BasicLabelUI extends LabelUI implements PropertyChangeListener {
    protected static BasicLabelUI labelUI;

    private final Rectangle viewRect = new Rectangle();
    private final Rectangle iconRect = new Rectangle();
    private final Rectangle textRect = new Rectangle();

    public static ComponentUI createUI(final JComponent c) {
        if (labelUI == null) {
            labelUI = new BasicLabelUI();
        }

        return labelUI;
    }

    public void installUI(final JComponent c) {
        JLabel label = (JLabel)c;

        installDefaults(label);
        installComponents(label);
        installListeners(label);
        installKeyboardActions(label);
    }

    public void uninstallUI(final JComponent c) {
        JLabel label = (JLabel)c;

        uninstallKeyboardActions(label);
        uninstallListeners(label);
        uninstallComponents(label);
        uninstallDefaults(label);
    }

    public void paint(final Graphics g, final JComponent c) {
        JLabel label = (JLabel)c;

        FontMetrics fm = Utilities.getFontMetrics(label);
        if (fm == null) {
            return;
        }
        iconRect.setBounds(0, 0, 0, 0);
        textRect.setBounds(0, 0, 0, 0);
        SwingUtilities.calculateInnerArea(c, viewRect);

        Icon icon = label.isEnabled() ? label.getIcon() : label
                                      .getDisabledIcon();

        String clippedText = layoutCL(label, fm, label.getText(), icon,
                                      viewRect, iconRect, textRect);

        int textY = Utilities.getTextY(fm, textRect);

        if (icon != null) {
            icon.paintIcon(label, g, iconRect.x, iconRect.y);
        }
        if (label.isEnabled()) {
            paintEnabledText(label, g, clippedText, textRect.x, textY);
        } else {
            paintDisabledText(label, g, clippedText, textRect.x, textY);
        }
    }

    public Dimension getPreferredSize(final JComponent c) {
        JLabel label = (JLabel)c;

        Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();

        Dimension size = Utilities.getCompoundLabelSize(label, label.getText(),
                icon, label.getVerticalTextPosition(), label
                        .getHorizontalTextPosition(), label.getIconTextGap());

        return Utilities.addInsets(size, label.getInsets());
    }

    public void propertyChange(final PropertyChangeEvent e) {
        JLabel label = (JLabel)e.getSource();
        label.revalidate();
        label.repaint();
    }


    protected String layoutCL(final JLabel label, final FontMetrics fontMetrics, final String text, final Icon icon, final Rectangle viewR, final Rectangle iconR, final Rectangle textR) {
        return SwingUtilities.layoutCompoundLabel(label, fontMetrics, text, icon, label.getVerticalAlignment(), label.getHorizontalAlignment(), label.getVerticalTextPosition(), label.getHorizontalTextPosition(), viewR, iconR, textR, label.getIconTextGap());
    }

    protected void paintEnabledText(final JLabel label, final Graphics g, final String clippedText, final int textX, final int textY) {
        int underscore = Utilities.getClippedUnderscoreIndex(label.getText(), clippedText,
                label.getDisplayedMnemonicIndex());
        Utilities.drawString(g, clippedText, textX, textY, label
                .getFontMetrics(label.getFont()), label.getForeground(),
                underscore);
    }

    protected void paintDisabledText(final JLabel label, final Graphics g, final String clippedText, final int textX, final int textY) {
        int underscore = Utilities.getClippedUnderscoreIndex(label.getText(), clippedText,
                label.getDisplayedMnemonicIndex());
        Utilities.drawString(g, clippedText, textX, textY, label
                .getFontMetrics(label.getFont()), label.getBackground()
                .darker(), underscore);
        Utilities.drawString(g, clippedText, textX + 1, textY + 1, label
                             .getFontMetrics(label.getFont()), label.getBackground()
                             .brighter(), underscore);
    }

    protected void installDefaults(final JLabel label) {
        label.setInheritsPopupMenu(true);
        LookAndFeel.installColorsAndFont(label, "Label.background", "Label.foreground", "Label.font");
        LookAndFeel.installBorder(label, "Label.border");
        LookAndFeel.installProperty(label, "alignmentX", new Float(0));
    }

    protected void uninstallDefaults(final JLabel label) {
        if (label == null) {
            return;
        }
        Utilities.uninstallColorsAndFont(label);
        LookAndFeel.uninstallBorder(label);
    }

    protected void installListeners(final JLabel label) {
        label.addPropertyChangeListener(this);
    }

    protected void uninstallListeners(final JLabel label) {
        label.removePropertyChangeListener(this);
    }

    protected void installComponents(final JLabel label) {
    }

    protected void uninstallComponents(final JLabel label) {
    }

    protected void installKeyboardActions(final JLabel label) {
    }

    protected void uninstallKeyboardActions(final JLabel label) {
    }
}


