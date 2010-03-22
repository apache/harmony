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
 * Created on 25.04.2005

 */
package javax.swing.plaf.basic;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

import org.apache.harmony.x.swing.ButtonCommons;
import org.apache.harmony.x.swing.Utilities;


public class BasicRadioButtonUI extends BasicToggleButtonUI {

    protected Icon icon;

    private static final String PROPERTY_PREFIX = "RadioButton.";
    private static BasicRadioButtonUI basicRadioButtonUI;

    public static ComponentUI createUI(final JComponent b) {
        if (basicRadioButtonUI == null) {
            basicRadioButtonUI = new BasicRadioButtonUI();
        }
        return basicRadioButtonUI;
    }

    protected String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

    protected void installDefaults(final AbstractButton b) {
        super.installDefaults(b);

        if (Utilities.isUIResource(icon)) {
            icon = UIManager.getIcon(getPropertyPrefix() + "icon");
        }
    }

    protected void uninstallDefaults(final AbstractButton b) {
        LookAndFeel.uninstallBorder(b);
    }

    public Icon getDefaultIcon() {
        return icon;
    }

    protected void paintFocus(final Graphics g,
                              final Rectangle textRect,
                              final Dimension size) {
    }

    public Dimension getPreferredSize(final JComponent c) {
        if (c instanceof AbstractButton) {
            return ButtonCommons.getPreferredSize((AbstractButton) c, getDefaultIcon());
        } else {
            return null;
        }
    }

    protected void paintIcon(final Graphics g, final JComponent c, final Rectangle iconRect) {
        AbstractButton button = (AbstractButton)c;
        Icon icon = ButtonCommons.getCurrentIcon(button);

        if (icon == null) {
            icon = getDefaultIcon();
        }
        if (icon != null) {
            icon.paintIcon(button, g, iconRect.x, iconRect.y);
        }
    }

}
