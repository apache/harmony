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
 * @author Sergey Burlak, Anton Avtamonov, Vadim Bogdanov
 */

package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JSlider;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.Utilities;


public class MetalIconFactory implements Serializable {
    public static class FileIcon16 implements Icon, Serializable {
        private static final Color FILE_COLOR = new Color(240, 240, 255);

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            int[] envelopeXs = new int[] { x + 2, x + 7, x + 14, x + 14, x + 2, x + 2 };
            int[] envelopeYs = new int[] { y + 7, y + 2, y + 2, y + 16, y + 16, y + 7 };
            g.setColor(FILE_COLOR);
            g.fillPolygon(envelopeXs, envelopeYs, 6);
            g.setColor(Color.GRAY);
            g.drawPolygon(envelopeXs, envelopeYs, 6);

            int[] cornerXs = new int[] { x + 2, x + 7, x + 7, x + 2 };
            int[] cornerYs = new int[] { y + 7, y + 2, y + 7, y + 7 };
            g.setColor(Color.LIGHT_GRAY);
            g.fillPolygon(cornerXs, cornerYs, 4);
            g.setColor(Color.GRAY);
            g.drawPolygon(cornerXs, cornerYs, 4);

            g.setColor(oldColor);
        }

        public int getShift() {
            return 0;
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 16;
        }

        public int getAdditionalHeight() {
            return 0;
        }
    }

    public static class FolderIcon16 implements Icon, Serializable {
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            int[] folderXs = new int[] { x + 2, x + 7, x + 8, x + 14, x + 14, x + 2, x + 2 };
            int[] folderYs = new int[] { y + 3, y + 3, y + 5, y + 5, y + 13, y + 13, y + 3 };
            g.setColor(Color.YELLOW);
            g.fillPolygon(folderXs, folderYs, 7);
            g.setColor(Color.GRAY);
            g.drawPolygon(folderXs, folderYs, 7);

            int[] cornerXs = new int[] { x + 2, x + 7, x + 8, x + 2, x + 2 };
            int[] cornerYs = new int[] { y + 3, y + 3, y + 5, y + 5, y + 3 };
            g.setColor(Color.LIGHT_GRAY);
            g.fillPolygon(cornerXs, cornerYs, 5);
            g.setColor(Color.GRAY);
            g.drawPolygon(cornerXs, cornerYs, 5);

            g.setColor(oldColor);
        }

        public int getShift() {
            return 0;
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 16;
        }

        public int getAdditionalHeight() {
            return 0;
        }

    }

    public static class PaletteCloseIcon implements Icon, UIResource, Serializable {
        private static final int size = 8;

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            AbstractButton b = (AbstractButton)c;
            Color saveColor = g.getColor();
            Color shadow = MetalLookAndFeel.getControlDarkShadow();
            Color highlight = MetalLookAndFeel.getControlHighlight();
            Color foreground = b.getModel().isArmed() ? shadow : highlight;
            Color background = !b.getModel().isArmed() ? shadow : highlight;

            g.setColor(foreground);
            g.drawPolyline(new int[] {0, 1, size / 2 - 1, size / 2, size - 2, size - 1},
                           new int[] {0, 0, size / 2 - 2, size / 2 - 2, 0, 0}, 6);
            g.drawLine(0, size - 2, size / 2 - 2, size / 2);
            g.drawLine(size / 2 + 1, size / 2, size - 1, size - 2);
            g.setColor(background);
            g.drawPolyline(new int[] {0, 1, size / 2 - 1, size / 2, size - 2, size - 1},
                           new int[] {size - 1, size - 1, size / 2 + 1, size / 2 + 1, size - 1, size - 1}, 6);
            g.drawLine(0, 1, size / 2 - 2, size / 2 - 1);
            g.drawLine(size / 2 + 1, size / 2 - 1, size - 1, 1);
            g.setColor(saveColor);
        }
    }

    public static class TreeControlIcon implements Icon, Serializable {
        protected boolean isLight;

        public TreeControlIcon(final boolean isCollapsed) {
            this.isLight = isCollapsed;
        }

        public void paintMe(final Component c, final Graphics g, final int x, final int y) {
            JTree tree = (JTree)c;
            g.setColor(tree.getBackground());
            g.fillRect(x + getIconWidth() / 4, y + getIconHeight() / 4 + 1,
                       getIconWidth() / 2 - 1, getIconHeight() / 2 - 1);
            g.setColor(tree.getForeground());
            g.drawRect(x + getIconWidth() / 4, y + getIconHeight() / 4 + 1,
                       getIconWidth() / 2 - 1, getIconHeight() / 2 - 1);
            g.drawLine(x + getIconWidth() / 2 - 3, y + getIconWidth() / 2,
                       x + getIconWidth() / 2 + 1, y + getIconWidth() / 2);
            if (isLight) {
                g.drawLine(x + getIconWidth() / 2 - 1, y + getIconWidth() / 2 - 2,
                           x + getIconWidth() / 2 - 1, y + getIconWidth() / 2 + 2);
            }
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            paintMe(c, g, x, y);
        }

        public int getIconWidth() {
            return 18;
        }

        public int getIconHeight() {
            return 18;
        }
    }

    public static class TreeFolderIcon extends MetalIconFactory.FolderIcon16 {
        public int getShift() {
            return -1;
        }

        public int getAdditionalHeight() {
            return 2;
        }

        public int getIconHeight() {
            return 18;
        }
    }

    public static class TreeLeafIcon extends MetalIconFactory.FileIcon16 {
        public int getShift() {
            return 2;
        }

        public int getAdditionalHeight() {
            return 4;
        }

        public int getIconHeight() {
            return 20;
        }
    }

    private static class InternalFrameAltMaximizeIcon implements Icon, UIResource {
        private int size;

        public InternalFrameAltMaximizeIcon(final int size) {
            this.size = size;
        }

        public int getIconHeight() {
            return size;
        }

        public int getIconWidth() {
            return size;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            AbstractButton b = (AbstractButton)c;
            Color saveColor = g.getColor();
            Color shadow = MetalLookAndFeel.getControlDarkShadow();
            Color highlight = MetalLookAndFeel.getControlHighlight();
            Utilities.draw3DRect(g, x, y + 5, size - 6, size - 6, shadow, highlight, !b.getModel().isArmed());
            Utilities.draw3DRect(g, x + 1, y + 6, size - 8, size - 8, shadow, highlight, b.getModel().isArmed());
            Utilities.draw3DRect(g, x + 5, y, size - 6, size - 6, shadow, highlight, !b.getModel().isArmed());
            Utilities.draw3DRect(g, x + 6, y + 1, size - 8, size - 8, shadow, highlight, b.getModel().isArmed());

            g.setColor(saveColor);
        }
    }

    private static class InternalFrameMinimizeIcon implements Icon, UIResource {
        private int size;

        public InternalFrameMinimizeIcon(final int size) {
            this.size = size;
        }

        public int getIconHeight() {
            return size;
        }

        public int getIconWidth() {
            return size;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            AbstractButton b = (AbstractButton)c;
            Color shadow = MetalLookAndFeel.getControlDarkShadow();
            Color highlight = MetalLookAndFeel.getControlHighlight();
            Utilities.draw3DRect(g, x, y + size - 6, size - 1, 5, shadow, highlight, !b.getModel().isArmed());
        }
    }

    private static class InternalFrameCloseIcon implements Icon, UIResource {
        private final int size;

        public InternalFrameCloseIcon(final int size) {
            this.size = size & -2;
        }

        public int getIconHeight() {
            return size;
        }

        public int getIconWidth() {
            return size;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            AbstractButton b = (AbstractButton)c;
            Color saveColor = g.getColor();
            Color shadow = MetalLookAndFeel.getControlDarkShadow();
            Color highlight = MetalLookAndFeel.getControlHighlight();
            Color foreground = b.getModel().isArmed() ? shadow : highlight;
            Color background = !b.getModel().isArmed() ? shadow : highlight;

            g.setColor(foreground);
            g.drawPolyline(new int[]{0, 2, size / 2 - 1, size / 2, size - 3, size - 2},
                    new int[] {2, 0, size / 2 - 3, size / 2 - 3, 0, 1}, 6);
            g.drawLine(0, size - 3, size / 2 - 3, size / 2);
            g.drawLine(size / 2 + 2, size / 2, size - 2, size - 4);
            g.setColor(background);
            g.drawPolyline(new int[]{1, 2, size / 2 - 1, size / 2, size - 3, size - 1},
                    new int[] {size - 2, size - 1, size / 2 + 2, size / 2 + 2, size - 1, size - 3}, 6);
            g.drawLine(1, 3, size / 2 - 3, size / 2 - 1);
            g.drawLine(size / 2 + 2, size / 2 - 1, size - 1, 2);
            g.setColor(saveColor);
        }
    }

    private static class InternalFrameMaximizeIcon implements Icon, UIResource {
        private int size;

        public InternalFrameMaximizeIcon(final int size) {
            this.size = size;
        }

        public int getIconHeight() {
            return size;
        }

        public int getIconWidth() {
            return size;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            AbstractButton b = (AbstractButton)c;
            Color shadow = MetalLookAndFeel.getControlDarkShadow();
            Color highlight = MetalLookAndFeel.getControlHighlight();
            Utilities.draw3DRect(g, x, y, size - 1, size - 1, shadow, highlight, !b.getModel().isArmed());
            Utilities.draw3DRect(g, x + 1, y + 1, size - 3, size - 3, shadow, highlight, b.getModel().isArmed());
        }
    }

    private static class TreeHardDriveIcon implements Icon, UIResource {
        public int getIconHeight() {
            return 16;
        }

        public int getIconWidth() {
            return 16;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            g.setColor(Color.LIGHT_GRAY);
            g.fillOval(x + 2, y + 8, 12, 6);
            g.setColor(Color.DARK_GRAY);
            g.drawOval(x + 2, y + 8, 12, 6);

            g.setColor(Color.LIGHT_GRAY);
            g.fillOval(x + 2, y + 5, 12, 6);
            g.setColor(Color.DARK_GRAY);
            g.drawOval(x + 2, y + 5, 12, 6);

            g.setColor(Color.LIGHT_GRAY);
            g.fillOval(x + 2, y + 2, 12, 6);
            g.setColor(Color.DARK_GRAY);
            g.drawOval(x + 2, y + 2, 12, 6);

            g.setColor(oldColor);
        }
    }

    private static class TreeFloppyDriveIcon implements Icon, UIResource {
        private static final Color FLOPPY_COLOR = new Color(200, 200, 255);

        public int getIconHeight() {
            return 16;
        }

        public int getIconWidth() {
            return 16;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            int[] floppyXs = new int[] { x + 2, x + 3, x + 13, x + 14, x + 14, x + 13, x + 3, x + 2, x + 2 };
            int[] floppyYs = new int[] { y + 2, y + 1, y + 1, y + 2, y + 12, y + 13, y + 13, y + 13, y + 2 };
            g.setColor(FLOPPY_COLOR);
            g.fillPolygon(floppyXs, floppyYs, 9);
            g.setColor(Color.GRAY);
            g.drawPolygon(floppyXs, floppyYs, 9);

            int[] labelXs = new int[] { x + 4, x + 12, x + 12, x + 4, x + 4 };
            int[] labelYs = new int[] { y + 1, y + 1, y + 8, y + 8, y + 1 };
            g.setColor(Color.WHITE);
            g.fillPolygon(labelXs, labelYs, 5);
            g.setColor(Color.GRAY);
            g.drawPolygon(labelXs, labelYs, 5);

            int[] bootXs = new int[] { x + 5, x + 12, x + 12, x + 5, x + 5 };
            int[] bootYs = new int[] { y + 10, y + 10, y + 13, y + 13, y + 10 };
            g.setColor(Color.BLUE);
            g.fillPolygon(bootXs, bootYs, 5);

            g.setColor(oldColor);
        }
    }

    private static class TreeComputerIcon implements Icon, UIResource {
        private static final Color SCREEN_COLOR = new Color(176, 221, 244);

        public int getIconHeight() {
            return 16;
        }

        public int getIconWidth() {
            return 16;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            g.setColor(SCREEN_COLOR);
            g.fillRect(x + 2, y + 2, 12, 8);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x + 2, y + 2, 11, 8);

            g.setColor(Color.CYAN);
            g.drawRect(x + 4, y + 5, 2, 1);

            g.setColor(Color.DARK_GRAY);
            g.fillRect(x + 7, y + 10, 2, 2);
            g.fillRect(x + 2, y + 12, 12, 2);

            g.setColor(oldColor);
        }
    }

    private static class InternalFrameDefaultMenuIcon implements Icon, UIResource {
        public int getIconHeight() {
            return 16;
        }

        public int getIconWidth() {
            return 16;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color highlight = MetalLookAndFeel.getControlHighlight();
            Color shadow = MetalLookAndFeel.getControlDarkShadow();
            Utilities.draw3DRect(g, x, y, 16, 16, shadow, highlight, true);
            Utilities.draw3DRect(g, x + 1, y + 1, 14, 14, shadow, highlight, false);
            Color oldColor = g.getColor();
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.fillRect(x + 2, y + 2, 12, 12);
            g.setColor(oldColor);
        }
    }

    private static class HorizontalSliderThumbIcon implements Icon, UIResource {
        public int getIconHeight() {
            return 16;
        }

        public int getIconWidth() {
            return 15;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            int bottom = y + getIconHeight() - 1;
            int right = x + getIconWidth() - 1;
            g.fillPolygon(new int[] { x, right, right, x + getIconWidth() / 2, x },
                          new int[] { y, y, bottom - getIconHeight() / 2, bottom, bottom - getIconHeight() / 2 }, 5);

            Color shadow = Color.gray;
            Color highlight = Color.white;

            g.setColor(highlight);
            g.drawLine(x, y, right, y);
            g.drawLine(right, y, right, bottom - getIconHeight() / 2 + 1);
            g.drawLine(right, bottom - getIconHeight() / 2 + 1, x + getIconWidth() / 2, bottom);

            g.setColor(shadow);
            g.drawLine(x, y, x, bottom - getIconHeight() / 2 + 1);
            g.drawLine(x, bottom - getIconHeight() / 2 + 1, x + getIconWidth() / 2, bottom);
        }
    }

    private static class VerticalSliderThumbIcon implements Icon, UIResource {
        public int getIconHeight() {
            return 15;
        }

        public int getIconWidth() {
            return 16;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            JSlider slider = (JSlider)c;
            int bottom = y + getIconHeight() - 1;
            int right = x + getIconWidth() - 1;
            if (slider.getComponentOrientation().isLeftToRight()) {
                g.fillPolygon(new int[] { x, x + getIconWidth() / 2, right, x + getIconWidth() / 2, x },
                        new int[] { y, y, bottom - getIconHeight() / 2, bottom, bottom }, 5);

                Color shadow = Color.gray;
                Color highlight = Color.white;

                g.setColor(highlight);
                g.drawLine(x, y, x + getIconWidth() / 2, y);
                g.drawLine(x + getIconWidth() / 2, y, right, bottom - getIconHeight() / 2);

                g.setColor(shadow);
                g.drawLine(x, y, x, bottom);
                g.drawLine(x, bottom, x + getIconWidth() / 2, bottom);
                g.drawLine(x + getIconWidth() / 2, bottom, right, bottom - getIconHeight() / 2);
            } else {
                g.fillPolygon(new int[] { x, x + getIconWidth() / 2, right, right, x + getIconWidth() / 2, x },
                        new int[] { bottom - getIconHeight() / 2, y, y, bottom, bottom, bottom - getIconHeight() / 2 }, 5);

                Color shadow = Color.gray;
                Color highlight = Color.white;

                g.setColor(highlight);
                g.drawLine(x + getIconWidth() / 2 - 1, bottom, right, bottom);
                g.drawLine(right, bottom, right, y);

                g.setColor(shadow);
                g.drawLine(right, y, x + getIconWidth() / 2 - 1, y);
                g.drawLine(x + getIconWidth() / 2 - 1, y, x, bottom - getIconHeight() / 2);
                g.drawLine(x, bottom - getIconHeight() / 2, x + getIconWidth() / 2 - 1, bottom);
            }
        }
    }

    private static class FileChooserUpFolderIcon implements Icon, UIResource {
        public int getIconHeight() {
            return 18;
        }

        public int getIconWidth() {
            return 18;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            int[] folderXs = new int[] { x + 2, x + 9, x + 10, x + 16, x + 16, x + 2, x + 2 };
            int[] folderYs = new int[] { y + 3, y + 3, y + 5, y + 5, y + 13, y + 13, y + 3 };
            g.setColor(Color.YELLOW);
            g.fillPolygon(folderXs, folderYs, 7);
            g.setColor(Color.GRAY);
            g.drawPolygon(folderXs, folderYs, 7);

            int[] cornerXs = new int[] { x + 2, x + 9, x + 10, x + 2, x + 2 };
            int[] cornerYs = new int[] { y + 3, y + 3, y + 5, y + 5, y + 3 };
            g.setColor(Color.LIGHT_GRAY);
            g.fillPolygon(cornerXs, cornerYs, 5);
            g.setColor(Color.GRAY);
            g.drawPolygon(cornerXs, cornerYs, 5);

            g.setColor(Color.GREEN.darker());
            int[] arrowXs = new int[] { x + 9, x + 13, x + 17, x + 9 };
            int[] arrowYs = new int[] { y + 6, y + 2, y + 6, y + 6 };
            g.fillPolygon(arrowXs, arrowYs, 4);
            g.drawPolygon(arrowXs, arrowYs, 4);
            g.drawLine(x + 12, y + 6, x + 12, y + 11);
            g.drawLine(x + 13, y + 6, x + 13, y + 11);
            g.drawLine(x + 14, y + 6, x + 14, y + 11);

            g.setColor(oldColor);
        }
    }

    private static class FileChooserNewFolderIcon implements Icon, UIResource {
        private static final Color STAR_COLOR = new Color(191, 62, 6);

        public int getIconHeight() {
            return 18;
        }

        public int getIconWidth() {
            return 18;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            int[] folderXs = new int[] { x + 2, x + 9, x + 10, x + 16, x + 16, x + 2, x + 2 };
            int[] folderYs = new int[] { y + 3, y + 3, y + 5, y + 5, y + 13, y + 13, y + 3 };
            g.setColor(Color.YELLOW);
            g.fillPolygon(folderXs, folderYs, 7);
            g.setColor(Color.GRAY);
            g.drawPolygon(folderXs, folderYs, 7);

            int[] cornerXs = new int[] { x + 2, x + 9, x + 10, x + 2, x + 2 };
            int[] cornerYs = new int[] { y + 3, y + 3, y + 5, y + 5, y + 3 };
            g.setColor(Color.LIGHT_GRAY);
            g.fillPolygon(cornerXs, cornerYs, 5);
            g.setColor(Color.GRAY);
            g.drawPolygon(cornerXs, cornerYs, 5);

            g.setColor(STAR_COLOR);
            g.drawLine(x, y + 12, x + 6, y + 12);
            g.drawLine(x + 3, y + 9, x + 3, y + 15);
            g.drawLine(x + 1, y + 10, x + 5, y + 14);
            g.drawLine(x + 1, y + 14, x + 5, y + 10);

            g.setColor(oldColor);
        }
    }

    private static class FileChooserListViewIcon implements Icon, UIResource {
        private static final Color ICON_COLOR = new Color(50, 100, 250);

        public int getIconHeight() {
            return 18;
        }

        public int getIconWidth() {
            return 18;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            drawItem(g, x + 1, y + 4);
            drawItem(g, x + 10, y + 4);
            drawItem(g, x + 1, y + 10);
            drawItem(g, x + 10, y + 10);

            g.setColor(oldColor);
        }

        private void drawItem(final Graphics g, final int x, final int y) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(x, y, 3, 3);
            g.setColor(ICON_COLOR);
            g.drawRect(x, y, 3, 3);

            g.setColor(Color.BLACK);
            g.drawLine(x + 5, y + 1, x + 7, y + 1);
            g.setColor(Color.GRAY);
            g.drawLine(x + 6, y + 2, x + 8, y + 2);
        }
    }

    private static class FileChooserHomeFolderIcon implements Icon, UIResource {
        private static final Color ROOF_COLOR = new Color(191, 62, 6);

        public int getIconHeight() {
            return 18;
        }

        public int getIconWidth() {
            return 18;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            int[] roofXs = new int[] { x + 1, x + 9, x + 17, x + 2 };
            int[] roofYs = new int[] { y + 9, y + 1, y + 9, y + 9 };
            g.setColor(ROOF_COLOR);
            g.fillPolygon(roofXs, roofYs, 4);
            g.setColor(Color.DARK_GRAY);
            g.drawPolygon(roofXs, roofYs, 4);

            g.setColor(Color.YELLOW);
            g.fillRect(x + 3, y + 8, 12, 8);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x + 3, y + 8, 12, 8);

            g.setColor(Color.DARK_GRAY);
            g.fillRect(x + 5, y + 11, 3, 5);

            g.setColor(Color.DARK_GRAY);
            g.fillRect(x + 10, y + 11, 3, 3);

            g.setColor(oldColor);
        }
    }

    private static class FileChooserDetailViewIcon implements Icon, UIResource {
        private static final Color ICON_COLOR = new Color(50, 100, 250);

        public int getIconHeight() {
            return 18;
        }

        public int getIconWidth() {
            return 18;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color oldColor = g.getColor();

            drawItem(g, x + 1, y + 4);
            drawItem(g, x + 1, y + 10);

            g.setColor(oldColor);
        }

        private void drawItem(final Graphics g, final int x, final int y) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(x, y, 3, 3);
            g.setColor(ICON_COLOR);
            g.drawRect(x, y, 3, 3);

            g.setColor(Color.BLACK);
            g.drawLine(x + 5, y + 1, x + 7, y + 1);
            g.drawLine(x + 9, y + 1, x + 11, y + 1);
            g.drawLine(x + 13, y + 1, x + 15, y + 1);
            g.setColor(Color.GRAY);
            g.drawLine(x + 6, y + 2, x + 8, y + 2);
            g.drawLine(x + 10, y + 2, x + 12, y + 2);
            g.drawLine(x + 14, y + 2, x + 16, y + 2);
        }
    }

    private static class RadioButtonIcon implements Icon, UIResource {
        private static final int SIZE = 13;

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color selectionColor = c.isEnabled() ? c.getForeground() : MetalLookAndFeel.getControlDisabled();
            drawRadioButtonIcon(g, (AbstractButton)c, selectionColor, MetalLookAndFeel.getControlDisabled(), c.getBackground(), x, y, SIZE, SIZE);
        }

        public int getIconWidth() {
            return SIZE;
        }

        public int getIconHeight() {
            return SIZE;
        }
    }

    private static class RadioButtonMenuItemIcon implements Icon, UIResource {
        private static final int SIZE = 10;

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {

            AbstractButton ab = (AbstractButton)c;
            Color selectionColor = c.isEnabled() ? ab.isSelected() ? MetalLookAndFeel.getMenuSelectedForeground() : c.getForeground() : MetalLookAndFeel.getMenuDisabledForeground();
            Color backgroundColor = ab.getModel().isArmed() ? MetalLookAndFeel.getMenuSelectedBackground() : c.getBackground();

            drawRadioButtonIcon(g, ab, selectionColor, MetalLookAndFeel.getMenuDisabledForeground(), backgroundColor, x, y, SIZE, SIZE);
        }

        public int getIconWidth() {
            return SIZE;
        }

        public int getIconHeight() {
            return SIZE;
        }
    }

    private static class CheckBoxIcon implements Icon, UIResource {
        private static final int SIZE = 13;

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color selectionColor = c.isEnabled() ? c.getForeground() : MetalLookAndFeel.getControlDisabled();
            drawCheckBoxIcon(g, (AbstractButton)c, selectionColor, MetalLookAndFeel.getControlDisabled(), x, y, SIZE, SIZE);
        }

        public int getIconWidth() {
            return SIZE;
        }

        public int getIconHeight() {
            return SIZE;
        }
    }

    private static class CheckBoxMenuItemIcon implements Icon, UIResource {
        private static final int SIZE = 10;

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color selectionColor = c.isEnabled() ? ((AbstractButton)c).isSelected() ? MetalLookAndFeel.getMenuSelectedForeground() : c.getForeground() : MetalLookAndFeel.getMenuDisabledForeground();
            drawCheckBoxIcon(g, (AbstractButton)c, selectionColor, MetalLookAndFeel.getMenuDisabledForeground(), x, y, SIZE, SIZE);
        }

        public int getIconWidth() {
            return SIZE;
        }

        public int getIconHeight() {
            return SIZE;
        }
    }


    private static class MenuArrowIcon implements Icon, UIResource {
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            final boolean leftToRight = c.getComponentOrientation().isLeftToRight();
            final int direction = leftToRight ? SwingConstants.EAST : SwingConstants.WEST;
            final Color color = c.isEnabled() ? c.getForeground() : c.getBackground().darker();
            Utilities.fillArrow(g, x, y + 1, direction, 8, true, color);
        }

        public int getIconWidth() {
            return 4;
        }

        public int getIconHeight() {
            return 8;
        }
    }

    private static class MenuItemArrowIcon implements Icon, UIResource {
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        }

        public int getIconWidth() {
            return 4;
        }

        public int getIconHeight() {
            return 8;
        }
    }


    public static final boolean DARK = false;
    public static final boolean LIGHT = true;

    private static Icon treeHardDriveIcon;
    private static Icon treeFloppyDriveIcon;
    private static Icon treeComputerIcon;
    private static Icon radioButtonMenuItemIcon;
    private static Icon radioButtonIcon;
    private static Icon menuItemArrowIcon;
    private static Icon menuArrowIcon;
    private static Icon internalFrameDefaultMenuIcon;
    private static Icon fileChooserUpFolderIcon;
    private static Icon fileChooserNewFolderIcon;
    private static Icon fileChooserListViewIcon;
    private static Icon fileChooserHomeFolderIcon;
    private static Icon fileChooserDetailViewIcon;
    private static Icon checkBoxMenuItemIcon;
    private static Icon checkBoxIcon;
    private static Icon verticalSliderThumbIcon;
    private static Icon horizontalSliderThumbIcon;

    public static Icon getTreeControlIcon(final boolean isLight) {
        return new TreeControlIcon(isLight);
    }

    public static Icon getInternalFrameMinimizeIcon(final int size) {
        return new InternalFrameMinimizeIcon(size);
    }

    public static Icon getInternalFrameMaximizeIcon(final int size) {
        return new InternalFrameMaximizeIcon(size);
    }

    public static Icon getInternalFrameCloseIcon(final int size) {
        return new InternalFrameCloseIcon(size);
    }

    public static Icon getInternalFrameAltMaximizeIcon(final int size) {
        return new InternalFrameAltMaximizeIcon(size);
    }

    public static Icon getVerticalSliderThumbIcon() {
        if (verticalSliderThumbIcon == null) {
            verticalSliderThumbIcon = new VerticalSliderThumbIcon();
        }
        return verticalSliderThumbIcon;
    }

    public static Icon getTreeLeafIcon() {
        return new TreeLeafIcon();
    }

    public static Icon getTreeHardDriveIcon() {
        if (treeHardDriveIcon == null) {
            treeHardDriveIcon = new TreeHardDriveIcon();
        }
        return treeHardDriveIcon;
    }

    public static Icon getTreeFolderIcon() {
        return new TreeFolderIcon();
    }

    public static Icon getTreeFloppyDriveIcon() {
        if (treeFloppyDriveIcon == null) {
            treeFloppyDriveIcon = new TreeFloppyDriveIcon();
        }
        return treeFloppyDriveIcon;
    }

    public static Icon getTreeComputerIcon() {
        if (treeComputerIcon == null) {
            treeComputerIcon = new TreeComputerIcon();
        }
        return treeComputerIcon;
    }

    public static Icon getRadioButtonMenuItemIcon() {
        if (radioButtonMenuItemIcon == null) {
            radioButtonMenuItemIcon = new RadioButtonMenuItemIcon();
        }
        return radioButtonMenuItemIcon;
    }

    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    public static Icon getMenuItemCheckIcon() {
        return null;
    }

    public static Icon getMenuItemArrowIcon() {
        if (menuItemArrowIcon == null) {
            menuItemArrowIcon = new MenuItemArrowIcon();
        }
        return menuItemArrowIcon;
    }

    public static Icon getMenuArrowIcon() {
        if (menuArrowIcon == null) {
            menuArrowIcon = new MenuArrowIcon();
        }
        return menuArrowIcon;
    }

    public static Icon getInternalFrameDefaultMenuIcon() {
        if (internalFrameDefaultMenuIcon == null) {
            internalFrameDefaultMenuIcon = new InternalFrameDefaultMenuIcon();
        }
        return internalFrameDefaultMenuIcon;
    }

    public static Icon getHorizontalSliderThumbIcon() {
        if (horizontalSliderThumbIcon == null) {
            horizontalSliderThumbIcon = new HorizontalSliderThumbIcon();
        }
        return horizontalSliderThumbIcon;
    }

    public static Icon getFileChooserUpFolderIcon() {
        if (fileChooserUpFolderIcon == null) {
            fileChooserUpFolderIcon = new FileChooserUpFolderIcon();
        }
        return fileChooserUpFolderIcon;
    }

    public static Icon getFileChooserNewFolderIcon() {
        if (fileChooserNewFolderIcon == null) {
            fileChooserNewFolderIcon = new FileChooserNewFolderIcon();
        }
        return fileChooserNewFolderIcon;
    }

    public static Icon getFileChooserListViewIcon() {
        if (fileChooserListViewIcon == null) {
            fileChooserListViewIcon = new FileChooserListViewIcon();
        }
        return fileChooserListViewIcon;
    }

    public static Icon getFileChooserHomeFolderIcon() {
        if (fileChooserHomeFolderIcon == null) {
            fileChooserHomeFolderIcon = new FileChooserHomeFolderIcon();
        }
        return fileChooserHomeFolderIcon;
    }

    public static Icon getFileChooserDetailViewIcon() {
        if (fileChooserDetailViewIcon == null) {
            fileChooserDetailViewIcon = new FileChooserDetailViewIcon();
        }
        return fileChooserDetailViewIcon;
    }

    public static Icon getCheckBoxMenuItemIcon() {
        if (checkBoxMenuItemIcon == null) {
            checkBoxMenuItemIcon = new CheckBoxMenuItemIcon();
        }
        return checkBoxMenuItemIcon;
    }

    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }


    private static void drawRadioButtonIcon(final Graphics g, final AbstractButton rb,
                                            final Color selectionColor, final Color disableColor,
                                            final Color backgroundColor,
                                            final int x, final int y, final int w, final int h) {

        Color oldColor = g.getColor();

        if (rb.isEnabled()) {
            if (rb.getModel().isArmed()) {
                g.setColor(MetalLookAndFeel.getControlShadow());
                g.fillRoundRect(x, y + 1, w - 1, h - 1, w - 1, h - 1);
            }
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawRoundRect(x + 1, y + 2, w - 2, h - 2, w - 2, h - 2);
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRoundRect(x, y + 1, w - 2, h - 2, w - 2, h - 2);
        } else {
            g.setColor(disableColor);
            g.drawRoundRect(x, y + 1, w - 2, h - 2, w - 2, h - 2);
        }

        if (rb.isSelected()) {
            g.setColor(rb.isEnabled() ? selectionColor : disableColor);
            g.fillRoundRect(x + w / 4, y + w / 4 + 1, w / 2, h / 2, w / 4, h / 4);
        }

        g.setColor(oldColor);
    }

    private static void drawCheckBoxIcon(final Graphics g, final AbstractButton cb,
                                         final Color selectionColor, final Color disableColor,
                                         final int x, final int y, final int w, final int h) {

        Color oldColor = g.getColor();

        if (cb.isEnabled()) {
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawRect(x + 1, y + 2, w - 2, h - 2);
        }

        if (cb.getModel().isArmed()) {
            g.setColor(MetalLookAndFeel.getControlShadow());
            g.fillRect(x, y + 1, w - 2, h - 2);
        }

        g.setColor(cb.isEnabled() ? MetalLookAndFeel.getControlDarkShadow() : disableColor);
        g.drawRect(x, y + 1, w - 2, h - 2);

        if (cb.isSelected()) {
            g.setColor(cb.isEnabled() ? selectionColor : disableColor);
            g.drawLine(x + 3, y + h / 2, x + 3, y + h - 2);
            g.drawLine(x + 4, y + h / 2, x + 4, y + h - 2);
            int lineLength = h - h / 2 - 2;
            g.drawLine(x + 4, y + h - 3, x + 4 + lineLength, y + h - 3 - lineLength);
        }

        g.setColor(oldColor);
    }
}

