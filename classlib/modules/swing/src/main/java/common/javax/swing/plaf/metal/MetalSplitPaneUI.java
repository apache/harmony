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

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class MetalSplitPaneUI extends BasicSplitPaneUI {
    public static ComponentUI createUI(final JComponent c) {
        return new MetalSplitPaneUI();
    }

    public BasicSplitPaneDivider createDefaultDivider() {
        return new MetalSplitPaneDivider(this);
    }

    private class MetalSplitPaneDivider extends BasicSplitPaneDivider {
        public MetalSplitPaneDivider(final BasicSplitPaneUI ui) {
            super(ui);
        }

        public boolean isOpaque() {
            return true;
        }

        public void paint(final Graphics g) {
            g.setColor(getForeground());
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paint(g);

            if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                int leftButtonHeight = leftButton == null ? 0 : leftButton.getHeight();
                int rightButtonHeight = rightButton == null ? 0 : rightButton.getHeight();
                final int buttonsSize = leftButtonHeight + rightButtonHeight + 6;
                MetalBumps.paintBumps(g, 1, buttonsSize + 4,
                                      getWidth() - 3, getHeight() - buttonsSize - 8,
                                      getForeground().darker(),
                                      getForeground().brighter());
            } else {
                int leftButtonWidth = leftButton == null ? 0 : leftButton.getWidth();
                int rightButtonWidth = rightButton == null ? 0 : rightButton.getWidth();
                final int buttonsSize = leftButtonWidth + rightButtonWidth + 6;
                MetalBumps.paintBumps(g, buttonsSize + 4, 1,
                                      getWidth() - 8 - buttonsSize, getHeight() - 3,
                                      getForeground().darker(),
                                      getForeground().brighter());
            }
        }
    }
}
