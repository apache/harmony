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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.AbstractButton;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;

import org.apache.harmony.x.swing.Utilities;


public class BasicBorders {

    /*
     * All color constants used in this class are selected to be consistent with
     * Basic L&F color pattern.
     */

    public static class ButtonBorder extends AbstractBorder implements UIResource {
        private static Insets borderInsets = new Insets(2, 3, 3, 3);
        
        protected Color shadow;
        protected Color darkShadow;
        protected Color highlight;
        protected Color lightHighlight;

        public ButtonBorder(final Color shadow, final Color darkShadow, final Color highlight, final Color lightHighlight) {
            this.shadow = shadow;
            this.darkShadow = darkShadow;
            this.highlight = highlight;
            this.lightHighlight = lightHighlight;
        }

        public Insets getBorderInsets(final Component c, final Insets insets) {
            Insets borderInsets = getBorderInsets(c);
            insets.bottom = borderInsets.bottom;
            insets.left = borderInsets.left;
            insets.right = borderInsets.right;
            insets.top = borderInsets.top;

            return insets;
        }

        public Insets getBorderInsets(final Component c) {
            return (Insets)borderInsets.clone();
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            if (c.isEnabled()) {
                Utilities.draw3DRect(g, x, y, w, h, darkShadow, highlight, false);
                if (!((AbstractButton)c).getModel().isArmed()) {
                    Utilities.draw3DRect(g, x + 1, y + 1, w - 2, h - 2, shadow, highlight, true);
                }
            } else {
                Color oldColor = g.getColor();
                g.setColor(shadow);
                g.drawRect(x, y, w - 1, h - 1);
                g.setColor(oldColor);
            }
        }
    }

    public static class FieldBorder extends AbstractBorder implements UIResource {
        protected Color shadow;
        protected Color darkShadow;
        protected Color highlight;
        protected Color lightHighlight;

        public FieldBorder(final Color shadow, final Color darkShadow,
                           final Color highlight, final Color lightHighlight) {
            this.shadow = shadow;
            this.darkShadow = darkShadow;
            this.highlight = highlight;
            this.lightHighlight = lightHighlight;
        }

        public Insets getBorderInsets(final Component c, final Insets insets) {
            Insets borderInsets = getBorderInsets(c);
            insets.bottom = borderInsets.bottom;
            insets.left = borderInsets.left;
            insets.right = borderInsets.right;
            insets.top = borderInsets.top;
            return insets;
        }

        public Insets getBorderInsets(final Component c) {
            Insets result;
            if (c instanceof JTextComponent) {
                Insets ins = ((JTextComponent)c).getMargin();
                result = new Insets(2,2,2,2);
                result.left += ins.left;
                result.right += ins.right;
                result.top += ins.top;
                result.bottom += ins.bottom;
            } else {
                result = new Insets(2, 2, 2, 2);
            }
            return result;
        }

        public void paintBorder(final Component c, final Graphics g,
                                final int x, final int y, final int w,
                                final int h) {
            Color oldColor = g.getColor();
            g.setColor(shadow);
            g.drawLine(x, y, x + w - 1, y);
            g.drawLine(x, y, x, y + h - 1);
            g.setColor(darkShadow);
            g.drawLine(x + 1, y + 1, x + w - 3, y + 1);
            g.drawLine(x + 1, y + 1, x + 1, y + h - 2);
            g.setColor(highlight);
            g.drawLine(x + w - 2, y + h - 2,  x + w - 2, y + 1);
            g.drawLine(x + w - 2, y + h - 2,  x + 1, y + h - 2);
            g.setColor(lightHighlight);
            g.drawLine(x + w - 1, y + h - 1,  x + w - 1, y);
            g.drawLine(x + w - 1, y + h - 1,  x, y + h - 1);
            g.setColor(oldColor);
        }
    }

    public static class MarginBorder extends AbstractBorder implements UIResource {
        public Insets getBorderInsets(final Component c, final Insets insets) {
            Insets borderInsets = getBorderInsets(c);
            insets.bottom = borderInsets.bottom;
            insets.left = borderInsets.left;
            insets.right = borderInsets.right;
            insets.top = borderInsets.top;

            return insets;
        }

        public Insets getBorderInsets(final Component c) {
            Insets result = (Insets)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    try {
                        Method m = c.getClass().getMethod("getMargin", new Class[0]);
                        return m.invoke(c, new Object[0]);
                    } catch (Exception e) {
                        return null;
                    }
                }
            });

            return result != null ? result : super.getBorderInsets(c);
        }
    }

    public static class MenuBarBorder extends AbstractBorder implements UIResource {
        private Color shadow;
        private Color highlight;

        public MenuBarBorder(final Color shadow, final Color highlight) {
            this.shadow = shadow;
            this.highlight = highlight;
        }

        public Insets getBorderInsets(final Component c, final Insets insets) {
            Insets borderInsets = getBorderInsets(c);
            insets.bottom = borderInsets.bottom;
            insets.left = borderInsets.left;
            insets.right = borderInsets.right;
            insets.top = borderInsets.top;

            return insets;
        }

        public Insets getBorderInsets(final Component c) {
            return new Insets(0, 0, 2, 0);
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            Color oldColor = g.getColor();
            g.setColor(highlight);
            g.drawLine(x, y + h - 2, x + w, y + h - 2);
            g.setColor(shadow);
            g.drawLine(x, y + h - 1, x + w, y + h - 1);
            g.setColor(oldColor);
        }
    }

    public static class RadioButtonBorder extends ButtonBorder {
        public RadioButtonBorder(final Color shadow, final Color darkShadow, final Color highlight, final Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }

        public Insets getBorderInsets(final Component c, final Insets insets) {
            Insets borderInsets = getBorderInsets(c);
            insets.bottom = borderInsets.bottom;
            insets.left = borderInsets.left;
            insets.right = borderInsets.right;
            insets.top = borderInsets.top;

            return insets;
        }

        public Insets getBorderInsets(final Component c) {
            return new Insets(2, 2, 2, 2);
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
        }
    }

    public static class RolloverButtonBorder extends ButtonBorder {
        public RolloverButtonBorder(final Color shadow, final Color darkShadow, final Color highlight, final Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }

        public void paintBorder(final Component c, final Graphics g,
                                final int x, final int y,
                                final int w, final int h) {
            if (((AbstractButton)c).getModel().isRollover()) {
                super.paintBorder(c, g, x, y, w, h);
            }
        }
    }

    public static class SplitPaneBorder implements Border, UIResource {
        protected Color highlight;
        protected Color shadow;

        public SplitPaneBorder(final Color highlight, final Color shadow) {
            this.shadow = shadow;
            this.highlight = highlight;
        }

        public Insets getBorderInsets(final Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            Utilities.draw3DRect(g, x, y, w, h, shadow, highlight, true);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    public static class ToggleButtonBorder extends ButtonBorder {
        public ToggleButtonBorder(final Color shadow, final Color darkShadow, final Color highlight, final Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }

        public Insets getBorderInsets(final Component c, final Insets insets) {
            Insets borderInsets = getBorderInsets(c);
            insets.bottom = borderInsets.bottom;
            insets.left = borderInsets.left;
            insets.right = borderInsets.right;
            insets.top = borderInsets.top;

            return insets;
        }

        public Insets getBorderInsets(final Component c) {
            return new Insets(2, 2, 2, 2);
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            super.paintBorder(c, g, x, y, w, h);
        }
    }

    static class ToolBarButtonMarginBorder extends MarginBorder {
        public Insets getBorderInsets(final Component c) {
            Insets result = super.getBorderInsets(c);
            return Utilities.isUIResource(result)
                    ? new Insets(3, 3, 3, 3)
                    : result;
        }
    }

    public static Border getToggleButtonBorder() {
        final String prefix = "ToggleButton.";
        ToggleButtonBorder toggleButtonBorder = new ToggleButtonBorder(getShadow(prefix),
                                                                       getDarkShadow(prefix),
                                                                       getHighlight(prefix),
                                                                       getLightHighlight(prefix));
        MarginBorder marginBorder = new MarginBorder();

        return new BorderUIResource.CompoundBorderUIResource(toggleButtonBorder, marginBorder);
    }

    public static Border getTextFieldBorder() {
        final String prefix = "TextField.";
        return new FieldBorder(getShadow(prefix), getDarkShadow(prefix),
                               getHighlight(prefix), getLightHighlight(prefix));
    }

    public static Border getSplitPaneDividerBorder() {
        return new SplitPaneDividerBorder();
    }

    public static Border getSplitPaneBorder() {
        final String prefix = "SplitPane.";
        return new SplitPaneBorder(getHighlight(prefix), getShadow(prefix));
    }

    public static Border getRadioButtonBorder() {
        final String prefix = "RadioButton.";
        RadioButtonBorder radioButtonBorder = new RadioButtonBorder(getShadow(prefix),
                                                                    getDarkShadow(prefix),
                                                                    getHighlight(prefix),
                                                                    getLightHighlight(prefix));
        MarginBorder marginBorder = new MarginBorder();

        return new BorderUIResource.CompoundBorderUIResource(radioButtonBorder, marginBorder);
    }

    public static Border getProgressBarBorder() {
        return new BorderUIResource.LineBorderUIResource(Color.GREEN, 2);
    }

    public static Border getMenuBarBorder() {
        final String prefix = "MenuBar.";
        return new MenuBarBorder(getShadow(prefix), getHighlight(prefix));
    }

    public static Border getInternalFrameBorder() {
        final String prefix = "InternalFrame.";
        final Color borderColor = UIManager.getColor(prefix + "borderColor");
        final Color shadow = UIManager.getColor(prefix + "borderShadow");
        final Color darkShadow = UIManager.getColor(prefix + "borderDarkShadow");
        final Color highlight = UIManager.getColor(prefix + "borderLight");
        final Color lightHighlight = UIManager.getColor(prefix + "borderHighlight");
        BevelBorder bevelBorder = new BevelBorder(0, highlight, lightHighlight, darkShadow, shadow);
        BorderUIResource.LineBorderUIResource lineBorder = new BorderUIResource.LineBorderUIResource(borderColor, 1);

        return new BorderUIResource.CompoundBorderUIResource(bevelBorder, lineBorder);
    }

    public static Border getButtonBorder() {
        final String prefix = "Button.";
        ButtonBorder buttonBorder = new ButtonBorder(getShadow(prefix),
                                                     getDarkShadow(prefix),
                                                     getHighlight(prefix),
                                                     getLightHighlight(prefix));
        MarginBorder marginBorder = new MarginBorder();

        return new BorderUIResource.CompoundBorderUIResource(buttonBorder, marginBorder);
    }


    /*
     * Border used for divider of JSplitPane
     */
    private static class SplitPaneDividerBorder implements Border, UIResource {
        private static final Color shadow = Color.GRAY;
        private static final Color lightHighlight = Color.WHITE;

        public SplitPaneDividerBorder() {
        }

        public Insets getBorderInsets(final Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
            Utilities.draw3DRect(g, x, y, w, h, shadow, lightHighlight, true);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    private static Color getLightHighlight(final String prefix) {
        return UIManager.getColor(prefix + "light");
    }

    private static Color getHighlight(final String prefix) {
        return UIManager.getColor(prefix + "highlight");
    }

    private static Color getDarkShadow(final String prefix) {
        return UIManager.getColor(prefix + "darkShadow");
    }

    private static Color getShadow(final String prefix) {
        return UIManager.getColor(prefix + "shadow");
    }
}
