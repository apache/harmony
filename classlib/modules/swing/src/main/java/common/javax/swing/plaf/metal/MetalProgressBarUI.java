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
 * @author Dennis Ushakov
 */

package javax.swing.plaf.metal;

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class MetalProgressBarUI extends BasicProgressBarUI {

    public static ComponentUI createUI(final JComponent c) {
        return new MetalProgressBarUI();
    }

    public void paintDeterminate(final Graphics g, final JComponent c) {
        super.paintDeterminate(g, c);

        Rectangle inner = SwingUtilities.calculateInnerArea(progressBar, null);
        paintNonFilledOutline(g, inner);

        g.setColor(getSelectionBackground());
        if (progressBar.getOrientation() == JProgressBar.VERTICAL) {
            if (progressBar.getValue() == progressBar.getMaximum()) {
                g.drawLine(inner.x, inner.y, inner.x + inner.width, inner.y);
            }
            int bottom = inner.y + inner.height;
            g.drawLine(inner.x, bottom - getAmountFull(progressBar.getInsets(), inner.width, inner.height),
                       inner.x, bottom);
        } else {
            g.drawLine(inner.x, inner.y, inner.x, inner.y + inner.height);
            g.drawLine(inner.x, inner.y,
                       inner.x + getAmountFull(progressBar.getInsets(), inner.width, inner.height), inner.y);
        }
    }

    public void paintIndeterminate(final Graphics g, final JComponent c) {
        super.paintIndeterminate(g, c);

        Rectangle inner = SwingUtilities.calculateInnerArea(progressBar, null);
        Rectangle box = getBox(inner);
        paintNonFilledOutline(g, inner);

        g.setColor(getSelectionBackground());
        if (progressBar.getOrientation() == JProgressBar.VERTICAL) {
            g.drawLine(inner.x, box.y, inner.x, box.y + box.height);
        } else {
            g.drawLine(box.x, inner.y, box.x + box.width, inner.y);
        }
    }

    private void paintNonFilledOutline(final Graphics g, final Rectangle inner) {
        g.setColor(MetalLookAndFeel.getControlShadow());
        g.drawLine(inner.x, inner.y, inner.x + inner.width, inner.y);
        g.drawLine(inner.x, inner.y, inner.x, inner.y + inner.height);
    }
}

