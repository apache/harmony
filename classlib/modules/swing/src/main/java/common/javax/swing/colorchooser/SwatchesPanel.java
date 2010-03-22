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
package javax.swing.colorchooser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;

class SwatchesPanel extends AbstractColorChooserPanel {
    private final static int MAIN_SWATCH_WIDTH = 31;
    private final static int MAIN_SWATCH_HEIGHT = 9;
    private final static int RECENT_SWATCH_WIDTH = 5;
    private final static int RECENT_SWATCH_HEIGHT = 7;

    private final static Color[][] MAIN_SWATCH_COLORS = createColors();
    private Dimension swatchSize;
    private Dimension recentSwatchSize;
    private SwatchPanel recentPanel;
    private SwatchPanel swatchPanel;

    private final class SwatchPanelMouseInputAdapter extends MouseInputAdapter {
    	private SwatchPanel panel;

    	SwatchPanelMouseInputAdapter(final SwatchPanel panel) {
    		this.panel = panel;
    	}

		public void mouseClicked(final MouseEvent e) {
		    getColorSelectionModel().setSelectedColor(panel.getColorAtLocation(e.getX(), e.getY(), true));
		}

		public void mouseMoved(final MouseEvent e) {
		    Color color = panel.getColorAtLocation(e.getX(), e.getY(), false);
		    panel.setToolTipText(color.getRed() + ", " + color.getGreen() + ", " + color.getBlue());
		}
	}

	private static class SwatchPanel extends JPanel {
        int oneColorWidth;
        int oneColorHeight;

        private Color[][] colors;
        private SwatchPanel recentPanel;

        public SwatchPanel(final Color[][] colors, final Dimension oneColorSize) {
            this(colors, null, oneColorSize);
        }

        public SwatchPanel(final Color[][] colors, final SwatchPanel recentPanel,
                           final Dimension oneColorSize) {
            this.colors = colors;
            this.recentPanel = recentPanel;
            this.oneColorHeight = oneColorSize.height;
            this.oneColorWidth = oneColorSize.width;
        }

        public Dimension getPreferredSize() {
            return new Dimension((oneColorWidth + 1) * colors.length + 1,
                                 (oneColorHeight + 1) * colors[0].length + 1);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Color getColorAtLocation(final int x, final int y, final boolean updateRecent) {
            int colorsX = x / (oneColorWidth + 1);
            colorsX = colorsX < colors.length ? colorsX : colors.length - 1;
            int colorsY = y / (oneColorHeight + 1);
            colorsY = colorsY < colors[0].length ? colorsY : colors[0].length - 1;
            Color result = colors[colorsX][colorsY];
            if (updateRecent && recentPanel != null) {
                recentPanel.addColor(result);
            }
            return result;
        }

        protected void paintComponent(final Graphics graphics) {
            Color oldColor = graphics.getColor();

            for (int i = 0; i < colors.length; i++) {
                for (int j = 0; j < colors[i].length; j++) {
                    Color paintColor = colors[i][j] != null ? colors[i][j] : this.getBackground();
                    graphics.setColor(paintColor);
                    graphics.fillRect((oneColorWidth + 1) * i, (oneColorHeight + 1)* j, oneColorWidth, oneColorHeight);
                    graphics.setColor(Color.WHITE);
                    graphics.draw3DRect((oneColorWidth + 1) * i, (oneColorHeight + 1)* j, oneColorWidth - 1, oneColorHeight - 1, true);
                }
            }
            graphics.setColor(oldColor);
        }

        private void addColor(final Color color) {
            for (int i = colors.length * colors[0].length - 2; i >= 0; i--) {
                colors[(i + 1) % colors.length][(i + 1) / colors.length] = colors[i % colors.length][i / colors.length];
            }
            colors[0][0] = color;
            repaint();
        }
    }

    public String getDisplayName() {
        return UIManager.getString("ColorChooser.swatchesNameText");
    }

    public Icon getSmallDisplayIcon() {
        return null;
    }

    public Icon getLargeDisplayIcon() {
        return null;
    }

    public void updateChooser() {

    }

    protected void buildChooser() {
        mnemonic = Integer.parseInt(UIManager.getString("ColorChooser.swatchesMnemonic"));
        displayedMnemonicIndex = Integer.parseInt(UIManager.getString("ColorChooser.swatchesDisplayedMnemonicIndex"));

        swatchSize = UIManager.getDimension("ColorChooser.swatchesSwatchSize");
        recentSwatchSize = UIManager.getDimension("ColorChooser.swatchesRecentSwatchSize");

        JPanel right = new JPanel(new BorderLayout());
        recentPanel = new SwatchPanel(createRecentColors(), null, recentSwatchSize);
        MouseInputAdapter swatchMouseAdapter = new SwatchPanelMouseInputAdapter(recentPanel);
        recentPanel.addMouseListener(swatchMouseAdapter);
        recentPanel.addMouseMotionListener(swatchMouseAdapter);

        right.add(BorderLayout.CENTER, new JLabel(UIManager.getString("ColorChooser.swatchesRecentText")));
        right.add(BorderLayout.SOUTH, recentPanel);

        swatchPanel = new SwatchPanel(MAIN_SWATCH_COLORS, recentPanel, swatchSize);
        swatchMouseAdapter = new SwatchPanelMouseInputAdapter(swatchPanel);
        swatchPanel.addMouseListener(swatchMouseAdapter);
        swatchPanel.addMouseMotionListener(swatchMouseAdapter);

        JPanel fullPanel = new JPanel();
        fullPanel.add(swatchPanel);
        fullPanel.add(right);
        this.add(fullPanel);
    }

    // The intention of this algorithm is to cover the entire color-spectrum
    // to fill swatcher cells uniformely
    private static Color[][] createColors() {
        Color[][] colors = new Color[MAIN_SWATCH_WIDTH][MAIN_SWATCH_HEIGHT];
        for (int i = 0; i < colors[0].length; i++) {
            colors[0][i] = Color.getHSBColor(1.f, 0.f, 1.f - (float)i / colors[0].length);
        }
        for (int i = 1; i < colors.length; i++) {
            for (int j = 0; j < colors[i].length / 2; j++) {
                colors[i][j] = Color.getHSBColor((.5f + (float)(i - 1 - colors.length) / (colors.length - 1)),
                                                 2.f * (j + 1.5f) / colors[i].length, 1.f );
            }
            for (int j = colors[i].length / 2; j < colors[i].length; j++) {
                colors[i][j] = Color.getHSBColor((.5f + (float)(i - 1 - colors.length) / (colors.length - 1)),
                                                 1.f, 1.5f * (colors[i].length - j + .3f) / (colors[i].length - 1));
            }
        }
        return colors;
    }

    private static Color[][] createRecentColors() {
        final Color[][] colors = new Color[RECENT_SWATCH_WIDTH][RECENT_SWATCH_HEIGHT];
        final Object propertyValue = UIManager.get("ColorChooser.swatchesDefaultRecentColor");
        final Color defaultColor = propertyValue instanceof Color ? (Color)propertyValue : Color.BLACK;
        for (Color[] cols : colors)
            Arrays.fill(cols, defaultColor);
            
        return colors;
    }
}

