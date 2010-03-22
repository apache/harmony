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
package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.ButtonCommons;


public class MetalCheckBoxIcon implements Icon, UIResource, Serializable {

    protected int getControlSize() {
        return 13;
    }

    protected void drawCheck(final Component c, final Graphics g,
                             final int x, final int y) {

        Color markColor = c.isEnabled() ? MetalLookAndFeel.getControlTextColor() :
            MetalLookAndFeel.getControlShadow();
        ButtonCommons.drawCheck(g, markColor, x + 1, x + 2);
    }

    public void paintIcon(final Component c, final Graphics g,
                          final int x, final int y) {
        ButtonModel model = ((AbstractButton)c).getModel();
        int height = getIconHeight() - 1;
        int width = getIconWidth() - 1;
        boolean enabled = c.isEnabled();

        Color oldColor = g.getColor();
        if (enabled) {
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawRect(x + 1, y + 1, width - 1, height - 1);
            g.drawRect(x, y + 2, width - 1, height - 1);

            if (model.isPressed()) {
                g.setColor(MetalLookAndFeel.getControlShadow());
                g.fillRect(x, y + 1, width, height);
            }
        }

        if (model.isSelected()) {
            drawCheck(c, g, x, y);
        }

        g.setColor(enabled ? MetalLookAndFeel.getControlDarkShadow() :
            MetalLookAndFeel.getControlShadow());
        g.drawRect(x, y + 1, width - 1, height - 1);

        g.setColor(oldColor);
    }

    public int getIconWidth() {
        return getControlSize();
    }

    public int getIconHeight() {
        return getControlSize();
    }

}
