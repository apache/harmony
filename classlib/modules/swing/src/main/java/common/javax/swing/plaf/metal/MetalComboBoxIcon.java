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

package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.apache.harmony.x.swing.Utilities;


public class MetalComboBoxIcon implements Icon, Serializable {
    private static final long serialVersionUID = 3760558693938247476L;

    public int getIconWidth() {
        return 10;
    }

    public int getIconHeight() {
        return 5;
    }

    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        Color color = c.isEnabled() ? c.getForeground() : UIManager.getColor("ComboBox.disabledForeground");
        Utilities.fillArrow(g, x, y, SwingConstants.SOUTH, 10, true, color);
    }
}
