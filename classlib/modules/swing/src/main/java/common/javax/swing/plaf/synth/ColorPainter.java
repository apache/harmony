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

package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Graphics;

import org.apache.harmony.x.swing.Utilities;

class ColorPainter extends SynthPainter {

    /**
     * Method that do paints
     */
    private void paintBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        SynthStyle style = context.getStyle();
        if (style.isOpaque(context)) {
            Color oldColor = g.getColor();
            g.setColor(style.getColor(context, ColorType.BACKGROUND));
            g.fillRect(x, y, x + w, y + h);
            g.setColor(oldColor);
        }
    }

    @Override
    public void paintArrowButtonBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintArrowButtonForeground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int direction) {
        SynthStyle style = context.getStyle();
        if (style.isOpaque(context)) {
            Utilities.paintArrow(g, x, y, direction, w, true, style.getColor(
                    context, ColorType.FOREGROUND));
        }
    }

    @Override
    public void paintButtonBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintCheckBoxMenuItemBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintColorChooserBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintComboBoxBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopIconBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintEditorPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintFileChooserBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintFormattedTextFieldBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameTitlePaneBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintLabelBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintListBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBarBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuItemBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintOptionPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintPanelBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintPasswordFieldBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintPopupMenuBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintProgressBarBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintProgressBarForeground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {

        SynthStyle style = context.getStyle();
        if (style.isOpaque(context)) {
            Color oldColor = g.getColor();
            g.setColor(style.getColor(context, ColorType.FOREGROUND));
            g.fillRect(x, y, x + w, y + h);
            g.setColor(oldColor);
        }
    }

    @Override
    public void paintRadioButtonBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintRadioButtonMenuItemBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintRootPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarThumbBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarTrackBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSeparatorBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSeparatorForeground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderThumbBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderTrackBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSpinnerBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneDividerBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneDragDivider(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneContentBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneTabAreaBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneTabBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int tabIndex) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTableBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTableHeaderBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTextAreaBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTextFieldBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTextPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToggleButtonBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarContentBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarDragWindowBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToolTipBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeCellBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintViewportBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {
        paintBackground(context, g, x, y, w, h);
    }

}
