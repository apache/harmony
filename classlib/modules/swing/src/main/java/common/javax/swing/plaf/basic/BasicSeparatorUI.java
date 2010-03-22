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
*/
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SeparatorUI;

import org.apache.harmony.x.swing.Utilities;


public class BasicSeparatorUI extends SeparatorUI {

    private static final String PROPERTY_PREFIX = "Separator.";

    protected Color shadow;
    protected Color highlight;

    public static ComponentUI createUI(final JComponent c) {
        return new BasicSeparatorUI();
    }

    public void installUI(final JComponent c) {
        installDefaults((JSeparator)c);
        installListeners((JSeparator)c);
    }

    public void uninstallUI(final JComponent c) {
        uninstallListeners((JSeparator)c);
        uninstallDefaults((JSeparator)c);
    }

    protected void installDefaults(final JSeparator s) {
        LookAndFeel.installColors(s, PROPERTY_PREFIX + "background",
                                  PROPERTY_PREFIX + "foreground");
    }

    protected void uninstallDefaults(final JSeparator s) {
        if (s == null) {
            return;
        }
        Utilities.uninstallColorsAndFont(s);
    }

    protected void installListeners(final JSeparator s) {
    }

    protected void uninstallListeners(final JSeparator s) {
    }

    public void paint(final Graphics g, final JComponent c) {
        Dimension size = c.getSize();
        Color oldColor = g.getColor();
        if (((JSeparator)c).getOrientation() == SwingConstants.HORIZONTAL) {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, size.width, 0);
            g.setColor(c.getBackground());
            g.drawLine(0, 1, size.width, 1);
        } else {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, 0, size.height);
            g.setColor(c.getBackground());
            g.drawLine(1, 0, 1, size.height);
        }
        g.setColor(oldColor);
    }

    public Dimension getPreferredSize(final JComponent c) {
        return ((JSeparator)c).getOrientation() == SwingConstants.HORIZONTAL
                ? new Dimension(0, 2) : new Dimension(2, 0);
    }

    public Dimension getMinimumSize(final JComponent c) {
        return null;
    }

    public Dimension getMaximumSize(final JComponent c) {
        return null;
    }

}
