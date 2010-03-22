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
 * @author Anton Avtamonov, Sergey Burlak, Vadim Bogdanov, Alexander Simbirtsev
 */

package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicBorders.MarginBorder;

import org.apache.harmony.x.swing.Utilities;


public class MetalBorders {
    public static class ButtonBorder extends AbstractBorder implements UIResource {
        protected static Insets borderInsets = new Insets(3, 3, 3, 3);

        public Insets getBorderInsets(final Component component, final Insets insets) {
            return initBorderInsets(insets, borderInsets);
        }

        public Insets getBorderInsets(final Component component) {
            return borderInsets;
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            if (c.isEnabled()) {
                final AbstractButton button = (AbstractButton)c;
                if ((button instanceof JButton) && ((JButton)button).isDefaultButton()) {
                    Utilities.draw3DRect(g, x + 2, y + 2, w - 3, h - 3, MetalLookAndFeel.getControlShadow(), MetalLookAndFeel.getControlHighlight(), true);
                    Utilities.draw3DRect(g, x, y, w - 1, h - 1, MetalLookAndFeel.getControlDarkShadow(), MetalLookAndFeel.getControlDarkShadow(), true);
                    Utilities.draw3DRect(g, x + 1, y + 1, w - 3, h - 3, MetalLookAndFeel.getControlDarkShadow(), MetalLookAndFeel.getControlDarkShadow(), true);
                } else {
                    Utilities.draw3DRect(g, x, y, w, h, MetalLookAndFeel.getControlDarkShadow(), MetalLookAndFeel.getControlHighlight(), false);
                    if (!button.getModel().isArmed()) {
                        Utilities.draw3DRect(g, x + 1, y + 1, w - 2, h - 2, MetalLookAndFeel.getControlShadow(), MetalLookAndFeel.getControlHighlight(), true);
                    }
                }
            } else {
                Color oldColor = g.getColor();
                g.setColor(MetalLookAndFeel.getControlShadow());
                g.drawRect(x, y, w - 1, h - 1);
                g.setColor(oldColor);
            }
        }
    }

    public static class Flush3DBorder extends AbstractBorder implements UIResource {
        private static final Insets BORDER_INSETS = new Insets(2, 2, 2, 2);

        public Insets getBorderInsets(final Component component, final Insets insets) {
            return initBorderInsets(insets, BORDER_INSETS);
        }

        public Insets getBorderInsets(final Component component) {
            return BORDER_INSETS;
        }

        public void paintBorder(final Component c,
                                final Graphics g,
                                final int x,
                                final int y,
                                final int w,
                                final int h) {
            Color shadow = MetalLookAndFeel.getControlShadow();
            Color highlight = MetalLookAndFeel.getControlHighlight();
            Utilities.draw3DRect(g, x, y, w, h, shadow, highlight, false);
            Utilities.draw3DRect(g, x + 1, y + 1, w - 2, h - 2, shadow,
                                 highlight, true);
        }
    }

    public static class InternalFrameBorder extends AbstractBorder implements UIResource {
        private static final int width = 5;
        private static final int corner = 15 + width;
        private static final Insets BORDER_INSETS = new Insets(width, width, width, width);

        Color activeColor;
        Color activeHighlight;
        Color activeDarkShadow;
        Color activeShadow;

        public InternalFrameBorder() {
            installColors();
        }

        public Insets getBorderInsets(final Component c, final Insets insets) {
            return initBorderInsets(insets, BORDER_INSETS);
        }

        public Insets getBorderInsets(final Component c) {
            return BORDER_INSETS;
        }

        public void paintBorder(final Component c, final Graphics g,
                                final int x, final int y, final int w,
                                final int h) {
            boolean isActive = isActive(c);

            Color color;
            Color highlight;
            Color darkShadow;
            Color shadow;
            if (isActive) {
                color = activeColor;
                highlight = activeHighlight;
                darkShadow = activeDarkShadow;
                shadow = activeShadow;
            } else {
                color = MetalLookAndFeel.getControlDarkShadow();
                highlight = MetalLookAndFeel.getControlHighlight();
                darkShadow = MetalLookAndFeel.getControlDarkShadow();
                shadow = MetalLookAndFeel.getControlShadow();
            }

            fillBorder(g, x + 1, y + 1, w - 1, h - 1, color, width - 2);
            Utilities.draw3DRect(g, x, y, w, h, darkShadow, highlight, true);
            Utilities.draw3DRect(g, x + width - 1, y + width - 1, w - 2 * width
                    + 2, h - 2 * width + 2, darkShadow, highlight, false);

            if (!canBeResized(c)) {
                return;
            }

            // paint corners (only for resizable frame)
            Color saveColor = g.getColor();
            g.setColor(shadow);
            g.fillRect(x + 1, y + 1, corner - 2, 3);
            g.fillRect(x + 1, y + 1, 3, corner - 2);
            g.fillRect(w - corner + 1, y + 1, corner - 2, 3);
            g.fillRect(w - 4, y + 1, 3, corner - 2);
            g.fillRect(x + 1, h - 4, corner - 2, 3);
            g.fillRect(x + 1, h - corner + 1, 3, corner - 2);
            g.fillRect(w - corner + 1, h - 4, corner - 2, 3);
            g.fillRect(w - 4, h - corner + 1, 3, corner - 2);

            g.setColor(saveColor);
        }

        void installColors() {
            if (activeColor == null) {
                activeColor = MetalLookAndFeel.getPrimaryControlDarkShadow();
                activeHighlight = MetalLookAndFeel.getPrimaryControlHighlight();
                activeDarkShadow = MetalLookAndFeel
                        .getPrimaryControlDarkShadow();
                activeShadow = MetalLookAndFeel.getPrimaryControlShadow();
            }
        }

        boolean isActive(final Component c) {
            return ((JInternalFrame)c).isSelected();
        }

        boolean canBeResized(final Component c) {
            JInternalFrame frame = (JInternalFrame)c;
            return frame.isResizable() && !frame.isMaximum();
        }
    }

    public static class MenuBarBorder extends AbstractBorder implements UIResource {
        protected static Insets borderInsets = new Insets(1, 0, 1, 0);

        public Insets getBorderInsets(final Component component, final Insets insets) {
            return initBorderInsets(insets, borderInsets);
        }

        public Insets getBorderInsets(final Component component) {
            return borderInsets;
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            Color oldColor = g.getColor();

            g.setColor(MetalLookAndFeel.getControlShadow());
            g.drawLine(x, y + h - 1, x + w, y + h - 1);

            g.setColor(oldColor);
        }
    }

    public static class MenuItemBorder extends AbstractBorder implements UIResource {
        protected static Insets borderInsets = new Insets(2, 2, 2, 2);

        public Insets getBorderInsets(final Component component, final Insets insets) {
            return initBorderInsets(insets, borderInsets);
        }

        public Insets getBorderInsets(final Component component) {
            return borderInsets;
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            JMenuItem item = (JMenuItem)c;
            if ((item.isArmed() || (item instanceof JMenu) && item.isSelected())&& item.isEnabled()) {
                Utilities.draw3DRect(g, x, y, w, h, MetalLookAndFeel.getControlDarkShadow(), MetalLookAndFeel.getControlHighlight(), false);
            }
        }
    }

    public static class OptionDialogBorder extends AbstractBorder implements UIResource {
        private static DialogBorder borderImpl;
        private static final Insets BORDER_INSETS = new Insets(3, 3, 3, 3);

        public OptionDialogBorder() {
            borderImpl = new DialogBorder();
        }

        public Insets getBorderInsets(final Component c, final Insets insets) {
            return initBorderInsets(insets, BORDER_INSETS);
        }

        public Insets getBorderInsets(final Component c) {
            return BORDER_INSETS;
        }

        public void paintBorder(final Component c, final Graphics g,
                                final int x, final int y,
                                final int w, final int h) {
            borderImpl.paintBorder(c, g, x, y, w, h);
        }
    }

    public static class PaletteBorder extends AbstractBorder implements UIResource {
        private static final int width = 1;
        private static final Insets borderInsets = new Insets(width, width, width, width);

        public Insets getBorderInsets(final Component c, final Insets insets) {
            return initBorderInsets(insets, borderInsets);
        }

        public Insets getBorderInsets(final Component c) {
            return borderInsets;
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            Color saveColor = g.getColor();
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());

            g.fillRect(x, y, width, h);
            g.fillRect(w - width, y, width, h);
            g.fillRect(x, y, w, width);
            g.fillRect(x, h - width, w, width);

            g.setColor(saveColor);
        }
    }

    public static class PopupMenuBorder extends AbstractBorder implements UIResource {
        protected static Insets borderInsets = new Insets(2, 2, 1, 1);

        public Insets getBorderInsets(final Component component, final Insets insets) {
            return initBorderInsets(insets, borderInsets);
        }

        public Insets getBorderInsets(final Component component) {
            return borderInsets;
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            Utilities.draw3DRect(g, x + 1, y + 1, w - 1, h - 1, MetalLookAndFeel.getControlDarkShadow(), MetalLookAndFeel.getControlHighlight(), true);
            Color oldColor = g.getColor();

            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(x, y, w - 1, h - 1);

            g.setColor(oldColor);
        }
    }

    public static class RolloverButtonBorder extends ButtonBorder {
        public void paintBorder(final Component c, final Graphics g,
                                final int x, final int y,
                                final int w, final int h) {
            if (((AbstractButton)c).getModel().isRollover()) {
                super.paintBorder(c, g, x, y, w, h);
            }
        }
    }

    public static class ScrollPaneBorder extends AbstractBorder implements UIResource {
        private static final Insets borderInsets = new Insets(1, 1, 2, 2);

        public Insets getBorderInsets(final Component component) {
            return borderInsets;
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            Utilities.draw3DRect(g, x, y, w, h, MetalLookAndFeel.getControlShadow(), MetalLookAndFeel.getControlHighlight(), false);
        }
    }

    public static class TableHeaderBorder extends AbstractBorder {
        protected Insets editorBorderInsets = new Insets(2, 2, 2, 0);

        public Insets getBorderInsets(final Component c) {
            return editorBorderInsets;
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            Utilities.draw3DRect(g, x, y, w, h, MetalLookAndFeel.getControlShadow(), MetalLookAndFeel.getControlHighlight(), true);
        }
    }

    public static class TextFieldBorder extends Flush3DBorder {

        public void paintBorder(Component c, Graphics g, int x, int y, int w,
                                int h) {
            if (!c.isEnabled()) {
                return;
            }
            super.paintBorder(c, g, x, y, w, h);
        }
    }

    public static class ToggleButtonBorder extends ButtonBorder {
        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            if (((AbstractButton)c).isSelected()) {
                Utilities.draw3DRect(g, x, y, w, h, MetalLookAndFeel.getControlDarkShadow(), MetalLookAndFeel.getControlHighlight(), false);
                Utilities.draw3DRect(g, x + 1, y + 1, w - 2, h - 2, MetalLookAndFeel.getControlShadow(), MetalLookAndFeel.getControlShadow(), true);
            } else {
                super.paintBorder(c, g, x, y, w, h);
            }
        }
    }

    public static class ToolBarBorder extends AbstractBorder
            implements UIResource, SwingConstants {

        private static final int NORMAL_INDENT = 2;
        private static final int BIGGER_INDENT = 16;
        private static final Insets INSETS =
            new Insets(NORMAL_INDENT, NORMAL_INDENT, NORMAL_INDENT, NORMAL_INDENT);

        protected MetalBumps bumps;

        public Insets getBorderInsets(final Component c, final Insets insets) {
            JToolBar toolBar = (JToolBar)c;
            Insets result = initBorderInsets(insets, INSETS);
            if (toolBar.isFloatable()) {
                if (toolBar.getOrientation() == JToolBar.HORIZONTAL) {
                    result.left = BIGGER_INDENT;
                } else {
                    result.top = BIGGER_INDENT;
                }
            }

            Insets margin = toolBar.getMargin();
            result.top += margin.top;
            result.left += margin.left;
            result.bottom += margin.bottom;
            result.right += margin.right;

            return result;
        }

        public Insets getBorderInsets(final Component c) {
            return getBorderInsets(c, null);
        }

        public void paintBorder(final Component c, final Graphics g,
                                final int x, final int y,
                                final int w, final int h) {
            JToolBar toolBar = (JToolBar)c;
            if (!toolBar.isFloatable()) {
                return;
            }

            int bumpsWidth;
            int bumpsHeight;
            if (toolBar.getOrientation() == JToolBar.HORIZONTAL) {
                bumpsWidth = BIGGER_INDENT - NORMAL_INDENT - NORMAL_INDENT;
                bumpsHeight = h - NORMAL_INDENT - NORMAL_INDENT;
            } else {
                bumpsWidth = w - NORMAL_INDENT - NORMAL_INDENT;
                bumpsHeight = BIGGER_INDENT - NORMAL_INDENT - NORMAL_INDENT;
            }
            MetalBumps.paintBumps(g, x + NORMAL_INDENT, y + NORMAL_INDENT,
                                  bumpsWidth, bumpsHeight,
                                  MetalLookAndFeel.getControlHighlight(),
                                  MetalLookAndFeel.getControlDarkShadow());
        }
    }

    /**
     * This class implements the border for top level containers.
     * It is used when <code>JRootPane.windowDecorationStyle</code> is set to
     * <code>JRootPane.FRAME</code>.
     */
    static class FrameBorder extends InternalFrameBorder {
        boolean isActive(final Component c) {
            return SwingUtilities.getWindowAncestor(c).isActive();
        }

        boolean canBeResized(final Component c) {
            Window window = SwingUtilities.getWindowAncestor(c);
            return Utilities.isResizableWindow(window)
                && !Utilities.isMaximumFrame(window);
        }
    }

    /**
     * This class implements the border for top level containers.
     * It is used when <code>JRootPane.windowDecorationStyle</code> is set to
     * <code>JRootPane.PLAIN_DIALOG</code>.
     */
    static class DialogBorder extends FrameBorder {
    }

    /**
     * This class implements the border for top level containers.
     * It is used when <code>JRootPane.windowDecorationStyle</code> is set to
     * <code>JRootPane.QUESTION_DIALOG</code>.
     */
    static final class QuestionDialogBorder extends DialogBorder {
        void installColors() {
            if (activeColor == null) {
                activeColor = UIManager
                        .getColor("OptionPane.questionDialog.border.background");
                activeHighlight = MetalLookAndFeel.getPrimaryControlHighlight();
                activeDarkShadow = UIManager
                        .getColor("OptionPane.questionDialog.border.background");
                activeShadow = UIManager
                        .getColor("OptionPane.questionDialog.titlePane.shadow");
            }
        }
    }

    /**
     * This class implements the border for top level containers.
     * It is used when <code>JRootPane.windowDecorationStyle</code> is set to
     * <code>JRootPane.WARNING_DIALOG</code>.
     */
    static final class WarningDialogBorder extends DialogBorder {
        void installColors() {
            if (activeColor == null) {
                activeColor = UIManager
                        .getColor("OptionPane.warningDialog.border.background");
                activeHighlight = MetalLookAndFeel.getPrimaryControlHighlight();
                activeDarkShadow = UIManager
                        .getColor("OptionPane.warningDialog.border.background");
                activeShadow = UIManager
                        .getColor("OptionPane.warningDialog.titlePane.shadow");
            }
        }
    }

    /**
     * This class implements the border for top level containers.
     * It is used when <code>JRootPane.windowDecorationStyle</code> is set to
     * <code>JRootPane.ERROR_DIALOG</code>.
     */
    static final class ErrorDialogBorder extends DialogBorder {
        void installColors() {
            if (activeColor == null) {
                activeColor = UIManager
                        .getColor("OptionPane.errorDialog.border.background");
                activeHighlight = MetalLookAndFeel.getPrimaryControlHighlight();
                activeDarkShadow = UIManager
                        .getColor("OptionPane.errorDialog.border.background");
                activeShadow = UIManager
                        .getColor("OptionPane.errorDialog.titlePane.shadow");
            }
        }
    }

    static class ToolBarButtonMarginBorder extends MarginBorder {
        /**
         * Return new instance of the Insets class
         * @param Component c
         *
         * @return Insets result
         */
        public Insets getBorderInsets(final Component c) {
            Insets result = super.getBorderInsets(c);
            return Utilities.isUIResource(result)
                    ? new Insets(3, 3, 3, 3)
                    : result;
        }
    }

    public static Border getToggleButtonBorder() {
        return createCompoundBorder(new ToggleButtonBorder());
    }

    public static Border getTextFieldBorder() {
        return createCompoundBorder(new TextFieldBorder());
    }

    public static Border getTextBorder() {
        return createCompoundBorder(new Flush3DBorder());
    }

    public static Border getDesktopIconBorder() {
        LineBorder lineBorder = new LineBorder(new ColorUIResource(122, 138, 153), 1);
        MatteBorder matteBorder = new MatteBorder(new Insets(2, 2, 1, 2), new ColorUIResource(238, 238, 238));

        return new BorderUIResource.CompoundBorderUIResource(lineBorder, matteBorder);
    }

    public static Border getButtonBorder() {
        return createCompoundBorder(new ButtonBorder());
    }

    /**
     * The auxiliary function to implement <code>paintBorder()</code> method.
     * Fills the border area with <code>color</code> color.
     *
     * @param c the component that has the painted border
     * @param g <code>Graphics</code> object to paint
     * @param x position of the border
     * @param y position of the border
     * @param w position of the border
     * @param h position of the border
     * @param color the color to fill the border area
     * @param width the width of the border
     */
    private static void fillBorder(final Graphics g, final int x, final int y, final int w, final int h, final Color color, final int width) {
        Color saveColor = g.getColor();
        g.setColor(color);

        g.fillRect(x, y, width, h);
        g.fillRect(w - width, y, width, h);
        g.fillRect(x, y, w, width);
        g.fillRect(x, h - width, w, width);

        g.setColor(saveColor);
    }

    private static Insets initBorderInsets(final Insets result, final Insets template) {
        if (result == null) {
            return (Insets)template.clone();
        }

        result.bottom = template.bottom;
        result.left = template.left;
        result.right = template.right;
        result.top = template.top;

        return result;
    }

    private static Border createCompoundBorder(final Border b) {
        return new BorderUIResource.CompoundBorderUIResource(b, new MarginBorder());
    }
}

