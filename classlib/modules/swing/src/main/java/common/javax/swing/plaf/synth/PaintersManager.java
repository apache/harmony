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

import java.awt.Graphics;
import java.util.LinkedList;
import java.util.List;

/**
 * PaintersManager is a SynthPainter used to combine all the painters described
 * in XML file. This class is similar to ColorInfo and FontInfo (inner classes
 * in XMLSynthStyle) but placed separately because contains a lot of methods and
 * the functionality is differs from just "info" functionality
 */
@SuppressWarnings("nls")
class PaintersManager extends SynthPainter {

    public static final int NO_DIRECTION = -1;

    /**
     * PainterInfo for the search.
     */
    private static class SynthPainterInfo {

        private final String method;

        private final int direction;

        private final int state;

        private final SynthPainter painter;

        SynthPainterInfo(String method, int direction, int state,
                SynthPainter painter) {
            this.method = method;
            this.direction = direction;
            this.state = state;
            this.painter = painter;
        }

        String getMethod() {
            return method;
        }

        int getDirection() {
            return direction;
        }

        int getState() {
            return state;
        }

        SynthPainter getPainter() {
            return painter;
        }

        boolean betterThan(SynthPainterInfo candidate, int refState,
                String refMethod, int refDirection) {

            if (this.method.equalsIgnoreCase(refMethod)
                    || this.method.equals("default")) {

                if (stateBetterThan(candidate.getState(), refState)) {
                    if ((this.direction == refDirection)
                            || (this.direction == -1)) {
                        return true;
                    }

                }

            }

            return false;
        }

        boolean stateBetterThan(int candidateState, int refState) {
            if (((~refState) & (this.state)) == 0) {
                if (((~refState) & (candidateState)) == 0) {
                    return refState >= candidateState;
                }
                return true;
            }
            return false;
        }
    }

    private final List<SynthPainterInfo> painters = new LinkedList<SynthPainterInfo>();

    private final SynthPainterInfo firstCandidate = new SynthPainterInfo(
            "default", -1, 0, new ColorPainter()); //$NON-NLS-1$

    public SynthPainter findPainter(int state, String method, int direction) {
        SynthPainterInfo bestCandidate = firstCandidate;
        for (SynthPainterInfo candidate : painters) {
            if (candidate.betterThan(bestCandidate, state, method, direction)) {
                bestCandidate = candidate;
            }
        }
        return bestCandidate.getPainter();
    }

    public void setPainter(SynthPainter painter, int state, String method,
            int direction) {
        painters.add(new SynthPainterInfo(method, direction, state, painter));
    }

    @Override
    public void paintArrowButtonBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ArrowButtonBorder", -1)
                .paintArrowButtonBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintArrowButtonForeground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int direction) {

        findPainter(context.getComponentState(), "ArrowButtonForeground",
                direction).paintArrowButtonForeground(context, g, x, y, w, h,
                direction);

    }

    @Override
    public void paintButtonBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ButtonBackground", -1)
                .paintButtonBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintButtonBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ButtonBorder", -1)
                .paintButtonBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintCheckBoxBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "CheckBoxBackground", -1)
                .paintCheckBoxBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintCheckBoxBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "CheckBoxBorder", -1)
                .paintCheckBoxBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintCheckBoxMenuItemBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "CheckBoxMenuItemBackground",
                -1).paintCheckBoxMenuItemBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintCheckBoxMenuItemBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "CheckBoxMenuItemBorder", -1)
                .paintCheckBoxMenuItemBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintColorChooserBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ColorChooserBackground", -1)
                .paintColorChooserBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintColorChooserBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ColorChooserBorder", -1)
                .paintColorChooserBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintComboBoxBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ComboBoxBackground", -1)
                .paintComboBoxBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintComboBoxBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ComboBoxBorder", -1)
                .paintComboBoxBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopIconBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "DesktopIconBackground", -1)
                .paintDesktopIconBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopIconBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "DesktopIconBorder", -1)
                .paintDesktopIconBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "DesktopPaneBackground", -1)
                .paintDesktopPaneBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintDesktopPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "DesktopPaneBorder", -1)
                .paintDesktopPaneBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintEditorPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "EditorPaneBackground", -1)
                .paintEditorPaneBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintEditorPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "EditorPaneBorder", -1)
                .paintEditorPaneBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintFileChooserBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "FileChooserBackground", -1)
                .paintFileChooserBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintFileChooserBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "FileChooserBorder", -1)
                .paintFileChooserBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintFormattedTextFieldBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(),
                "FormattedTextFieldBackground", -1)
                .paintFormattedTextFieldBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintFormattedTextFieldBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "FormattedTextFieldBorder", -1)
                .paintFormattedTextFieldBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "InternalFrameBackground", -1)
                .paintInternalFrameBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "InternalFrameBorder", -1)
                .paintInternalFrameBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameTitlePaneBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(),
                "InternalFrameTitlePaneBackground", -1)
                .paintInternalFrameTitlePaneBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintInternalFrameTitlePaneBorder(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(),
                "InternalFrameTitlePaneBorder", -1)
                .paintInternalFrameTitlePaneBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintLabelBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "LabelBackground", -1)
                .paintLabelBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintLabelBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "LabelBorder", -1)
                .paintLabelBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintListBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ListBackground", -1)
                .paintListBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintListBorder(SynthContext context, Graphics g, int x, int y,
            int w, int h) {

        findPainter(context.getComponentState(), "ListBorder", -1)
                .paintListBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "MenuBackground", -1)
                .paintMenuBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBarBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "MenuBarBackground", -1)
                .paintMenuBarBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBarBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "MenuBarBorder", -1)
                .paintMenuBarBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuBorder(SynthContext context, Graphics g, int x, int y,
            int w, int h) {

        findPainter(context.getComponentState(), "MenuBorder", -1)
                .paintMenuBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuItemBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "MenuItemBackground", -1)
                .paintMenuItemBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintMenuItemBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "MenuItemBorder", -1)
                .paintMenuItemBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintOptionPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "OptionPaneBackground", -1)
                .paintOptionPaneBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintOptionPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "OptionPaneBorder", -1)
                .paintOptionPaneBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintPanelBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "PanelBackground", -1)
                .paintPanelBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintPanelBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "PanelBorder", -1)
                .paintPanelBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintPasswordFieldBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "PasswordFieldBackground", -1)
                .paintPasswordFieldBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintPasswordFieldBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "PasswordFieldBorder", -1)
                .paintPasswordFieldBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintPopupMenuBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "PopupMenuBackground", -1)
                .paintPopupMenuBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintPopupMenuBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "PopupMenuBorder", -1)
                .paintPopupMenuBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintProgressBarBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ProgressBarBackground", -1)
                .paintProgressBarBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintProgressBarBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ProgressBarBorder", -1)
                .paintProgressBarBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintProgressBarForeground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {

        findPainter(context.getComponentState(),
                "ProgressBarForegroundPainter", orientation)
                .paintProgressBarForeground(context, g, x, y, w, h, orientation);

    }

    @Override
    public void paintRadioButtonBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "RadioButtonBackground", -1)
                .paintRadioButtonBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintRadioButtonBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "RadioButtonBorder", -1)
                .paintRadioButtonBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintRadioButtonMenuItemBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(),
                "RadioButtonMenuItemBackground", -1)
                .paintRadioButtonMenuItemBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintRadioButtonMenuItemBorder(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "RadioButtonMenuItemBorder",
                -1).paintRadioButtonMenuItemBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintRootPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "RootPaneBackground", -1)
                .paintRootPaneBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintRootPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "RootPaneBorder", -1)
                .paintRootPaneBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ScrollBarBackground", -1)
                .paintScrollBarBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ScrollBarBorder", -1)
                .paintScrollBarBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarThumbBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {

        findPainter(context.getComponentState(),
                "ScrollBarThumbBackgroundPainter", orientation)
                .paintScrollBarThumbBackground(context, g, x, y, w, h,
                        orientation);

    }

    @Override
    public void paintScrollBarThumbBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {

        findPainter(context.getComponentState(), "ScrollBarThumbBorderPainter",
                orientation).paintScrollBarThumbBorder(context, g, x, y, w, h,
                orientation);

    }

    @Override
    public void paintScrollBarTrackBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ScrollBarTrackBackground", -1)
                .paintScrollBarTrackBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollBarTrackBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ScrollBarTrackBorder", -1)
                .paintScrollBarTrackBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ScrollPaneBackground", -1)
                .paintScrollPaneBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintScrollPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ScrollPaneBorder", -1)
                .paintScrollPaneBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintSeparatorBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "SeparatorBackground", -1)
                .paintSeparatorBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSeparatorBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "SeparatorBorder", -1)
                .paintSeparatorBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintSeparatorForeground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {
        findPainter(context.getComponentState(), "SeparatorForegroundPainter",
                orientation).paintSeparatorForeground(context, g, x, y, w, h,
                orientation);

    }

    @Override
    public void paintSliderBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "SliderBackground", -1)
                .paintSliderBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "SliderBorder", -1)
                .paintSliderBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderThumbBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {

        findPainter(context.getComponentState(),
                "SliderThumbBackgroundPainter", orientation)
                .paintSliderThumbBackground(context, g, x, y, w, h, orientation);

    }

    @Override
    public void paintSliderThumbBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h, int orientation) {

        findPainter(context.getComponentState(), "SliderThumbBorderPainter",
                orientation).paintSliderThumbBorder(context, g, x, y, w, h,
                orientation);

    }

    @Override
    public void paintSliderTrackBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "SliderTrackBackground", -1)
                .paintSliderTrackBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSliderTrackBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "SliderTrackBorder", -1)
                .paintSliderTrackBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintSpinnerBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "SpinnerBackground", -1)
                .paintSpinnerBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSpinnerBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "SpinnerBorder", -1)
                .paintSpinnerBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "SplitPaneBackground", -1)
                .paintSplitPaneBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "SplitPaneBorder", -1)
                .paintSplitPaneBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneDividerBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "SplitPaneDividerBackground",
                -1).paintSplitPaneDividerBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintSplitPaneDividerForeground(SynthContext context,
            Graphics g, int x, int y, int w, int h, int orientation) {

        findPainter(context.getComponentState(),
                "SplitPaneDividerForegroundPainter", orientation)
                .paintSplitPaneDividerForeground(context, g, x, y, w, h,
                        orientation);

    }

    @Override
    public void paintSplitPaneDragDivider(SynthContext context, Graphics g,
            int x, int y, int w, int h, int orientation) {

        findPainter(context.getComponentState(), "SplitPaneDragDividerPainter",
                orientation).paintSplitPaneDragDivider(context, g, x, y, w, h,
                orientation);

    }

    @Override
    public void paintTabbedPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TabbedPaneBackground", -1)
                .paintTabbedPaneBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TabbedPaneBorder", -1)
                .paintTabbedPaneBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneContentBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TabbedPaneContentBackground",
                -1).paintTabbedPaneContentBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneContentBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TabbedPaneContentBorder", -1)
                .paintTabbedPaneContentBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneTabAreaBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TabbedPaneTabBackground", -1)
                .paintTabbedPaneTabAreaBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneTabAreaBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TabbedPaneTabAreaBorder", -1)
                .paintTabbedPaneTabAreaBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTabbedPaneTabBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h, int tabIndex) {

        findPainter(context.getComponentState(), "TabbedPaneTabBackground", -1)
                .paintTabbedPaneTabBackground(context, g, x, y, w, h, tabIndex);
    }

    @Override
    public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h, int tabIndex) {

        findPainter(context.getComponentState(), "TabbedPaneTabBorder", -1)
                .paintTabbedPaneTabBorder(context, g, x, y, w, h, tabIndex);
    }

    @Override
    public void paintTableBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TableBackground", -1)
                .paintTableBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTableBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TableBorder", -1)
                .paintTableBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTableHeaderBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TableHeaderBackground", -1)
                .paintTableHeaderBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTableHeaderBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TableHeaderBorder", -1)
                .paintTableHeaderBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTextAreaBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TextAreaBackground", -1)
                .paintTextAreaBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTextAreaBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TextAreaBorder", -1)
                .paintTextAreaBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTextFieldBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TextFieldBackground", -1)
                .paintTextFieldBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTextFieldBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TextFieldBorder", -1)
                .paintTextFieldBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTextPaneBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TextPaneBackground", -1)
                .paintTextPaneBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTextPaneBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TextPaneBorder", -1)
                .paintTextPaneBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintToggleButtonBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ToggleButtonBackground", -1)
                .paintToggleButtonBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToggleButtonBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ToggleButtonBorder", -1)
                .paintToggleButtonBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ToolBarBackground", -1)
                .paintToolBarBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ToolBarBorder", -1)
                .paintToolBarBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarContentBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ToolBarContentBackground", -1)
                .paintToolBarContentBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarContentBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ToolBarContentBorder", -1)
                .paintToolBarContentBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarDragWindowBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ToolBarDragWindowBackground",
                -1).paintToolBarDragWindowBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToolBarDragWindowBorder(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ToolBarDragWindowBorder", -1)
                .paintToolBarDragWindowBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintToolTipBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ToolTipBackground", -1)
                .paintToolTipBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintToolTipBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ToolTipBorder", -1)
                .paintToolTipBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeBackground(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TreeBackground", -1)
                .paintTreeBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeBorder(SynthContext context, Graphics g, int x, int y,
            int w, int h) {

        findPainter(context.getComponentState(), "TreeBorder", -1)
                .paintTreeBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeCellBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "TreeCellBackground", -1)
                .paintTreeCellBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeCellBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TreeCellBorder", -1)
                .paintTreeCellBorder(context, g, x, y, w, h);
    }

    @Override
    public void paintTreeCellFocus(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "TreeCellFocus", -1)
                .paintTreeCellFocus(context, g, x, y, w, h);
    }

    @Override
    public void paintViewportBackground(SynthContext context, Graphics g,
            int x, int y, int w, int h) {

        findPainter(context.getComponentState(), "ViewportBackground", -1)
                .paintViewportBackground(context, g, x, y, w, h);
    }

    @Override
    public void paintViewportBorder(SynthContext context, Graphics g, int x,
            int y, int w, int h) {

        findPainter(context.getComponentState(), "ViewportBorder", -1)
                .paintViewportBorder(context, g, x, y, w, h);
    }

}
