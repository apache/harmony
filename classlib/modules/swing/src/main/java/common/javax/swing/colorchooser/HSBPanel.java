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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

import org.apache.harmony.x.swing.Utilities;

class HSBPanel extends AbstractColorChooserPanel {
    private class SelectorIcon implements Icon {
        private final Dimension size = new Dimension(SELECTOR_WIDTH, COMPONENTS_HEIGHT);

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            Color color = getColorSelectionModel().getSelectedColor();
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            float fixed = hsb[selectionMode];

            for (int i = 0; i < SELECTOR_WIDTH; i++) {
                for(int j = 0; j < COMPONENTS_HEIGHT; j++) {
                    switch (selectionMode) {
                    case HUE: g.setColor(Color.getHSBColor(fixed, (float)(SELECTOR_WIDTH - i)  / SELECTOR_WIDTH,
                                                                  (float)(COMPONENTS_HEIGHT - j) / COMPONENTS_HEIGHT));
                                     break;
                    case SATURATION: g.setColor(Color.getHSBColor((float)(SELECTOR_WIDTH - i)  / SELECTOR_WIDTH, fixed,
                                                                  (float)(COMPONENTS_HEIGHT - j) / COMPONENTS_HEIGHT));
                                     break;
                    case BRIGHTNESS: g.setColor(Color.getHSBColor((float)(SELECTOR_WIDTH - i)  / SELECTOR_WIDTH,
                                                                  (float)(COMPONENTS_HEIGHT - j) / COMPONENTS_HEIGHT,
                                                                  fixed));
                                     break;
                    }
                    g.drawLine(i, j, i, j);
                }
            }
        }

        public int getIconWidth() {
            return size.width;
        }

        public int getIconHeight() {
            return size.height;
        }
    }

    private class Selector extends JLabel {

        private int selectionMode = -1;

        public Selector() {
            setIcon(new SelectorIcon());
        }

        public void setSelectionMode(final int selectionMode) {
            if (this.selectionMode != selectionMode) {
                this.selectionMode = selectionMode;
                repaint();
            }
        }

        public void paint(final Graphics graphics) {
            Color oldColor = graphics.getColor();
            super.paint(graphics);
            paintCircle(graphics);

            graphics.setColor(oldColor);
        }

        private void paintCircle(final Graphics graphics) {
            Color color = getColorSelectionModel().getSelectedColor();
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

            int x = 0;
            int y = 0;
            int r = 5;
            switch (selectionMode) {
            case HUE: x = Math.round(SELECTOR_WIDTH * (1 - hsb[SATURATION]));
                      y = Math.round(COMPONENTS_HEIGHT * (1 - hsb[BRIGHTNESS])); break;
            case SATURATION: x = Math.round(SELECTOR_WIDTH * (1 - hsb[HUE]));
                             y = Math.round(COMPONENTS_HEIGHT * (1 - hsb[BRIGHTNESS])); break;
            case BRIGHTNESS: x = Math.round(SELECTOR_WIDTH * (1 - hsb[HUE]));
                             y = Math.round(COMPONENTS_HEIGHT * (1 - hsb[SATURATION])); break;
            }

            graphics.setColor(Color.WHITE);
            graphics.drawArc(x - r, y - r, 2 * r, 2 * r, 0, 360);
        }
    }

    private class SliderImage extends JLabel {
        private final Dimension size = new Dimension(SLIDER_IMAGE_WIDTH, COMPONENTS_HEIGHT);
        private int selectionMode = -1;

        public Dimension getPreferredSize() {
            return size;
        }

        public void setSelectionMode(final int selectionMode) {
            if (this.selectionMode != selectionMode) {
                this.selectionMode = selectionMode;
                repaint();
            }
        }

        public void paint(final Graphics graphics) {
            Color color = getColorSelectionModel().getSelectedColor();
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

            for (int i = 0; i < COMPONENTS_HEIGHT; i++) {
                float t = (float)i / COMPONENTS_HEIGHT;
                switch (selectionMode) {
                case HUE: graphics.setColor(Color.getHSBColor(t, 1.f, 1.f)); break;
                case SATURATION: graphics.setColor(Color.getHSBColor(hsb[0], 1.f - t, 1.f)); break;
                case BRIGHTNESS: graphics.setColor(Color.getHSBColor(hsb[0], 1.f, 1.f - t)); break;
                }
                graphics.drawLine(0, i, SLIDER_IMAGE_WIDTH, i);
            }

        }
    }

    private static final int COMPONENTS_HEIGHT = 200;
    private static final int SELECTOR_WIDTH = 200;
    private static final int SLIDER_IMAGE_WIDTH = 16;

    private static final int HUE = 0;
    private static final int SATURATION = 1;
    private static final int BRIGHTNESS = 2;

    private static final int MAX_HUE = 359;
    private static final int MAX_SATURATION = 100;
    private static final int MAX_BRIGHTNESS = 100;
    private static final int[] MAX = {MAX_HUE, MAX_SATURATION, MAX_BRIGHTNESS};
    private static final boolean[] SLIDER_INVERTED = {true, false, false};

    private JSlider slider;
    private SliderImage sliderImage;
    private Selector selector;

    private JSpinner[] spinners;
    private JRadioButton[] radioButtons;
    private JTextField[] rgbText;

    private int selectionMode;
    private boolean internalUpdateDisabled;

    public String getDisplayName() {
        return UIManager.getString("ColorChooser.hsbNameText");
    }

    public Icon getSmallDisplayIcon() {
        return null;
    }

    public Icon getLargeDisplayIcon() {
        return null;
    }

    public void updateChooser() {
        Color color = getColorSelectionModel().getSelectedColor();
        if (color == null) {
            return;
        }
        internalUpdateDisabled = true;
        
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float[] rgb = color.getRGBComponents(null);

        for (int i = 0; i < 3; i++) {
            spinners[i].setValue(new Integer(Math.round(hsb[i] * MAX[i])));
            rgbText[i].setText(Integer.toString(Math.round(rgb[i] * 255)));
        }

        updateSelector();
        internalUpdateDisabled = false;
    }

    protected void buildChooser() {
        String[] namesHSB = {UIManager.getString("ColorChooser.hsbHueText"),
                             UIManager.getString("ColorChooser.hsbSaturationText"),
                             UIManager.getString("ColorChooser.hsbBrightnessText")};

        String[] namesRGB = {UIManager.getString("ColorChooser.hsbRedText"),
                             UIManager.getString("ColorChooser.hsbGreenText"),
                             UIManager.getString("ColorChooser.hsbBlueText")};

        mnemonic = UIManager.getInt("ColorChooser.hsbMnemonic");
        displayedMnemonicIndex = Integer.parseInt(UIManager.getString("ColorChooser.hsbDisplayedMnemonicIndex"));

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;

        JPanel left = buildSelectorPanel();

        ButtonGroup group = new ButtonGroup();
        radioButtons = new JRadioButton[3];
        spinners = new JSpinner[3];
        rgbText = new JTextField[3];

        JPanel right = new JPanel();
        right.setLayout(layout);
        int rgbInset = 2 * UIManager.getInt("RadioButton.textIconGap") + UIManager.getIcon("RadioButton.icon").getIconWidth();

        for (int i = 0; i < 3; i++) {
            JRadioButton button = new JRadioButton(namesHSB[i]);
            installRadioButtonListener(button);
            radioButtons[i] = button;
            group.add(button);

            JSpinner spinner = buildHSBSpinner(i);
            spinners[i] = spinner;

            JPanel spinnerLabelHolder = new JPanel();
            spinnerLabelHolder.add(spinner);
            c.gridx = 0;
            c.gridy = i;
            layout.setConstraints(button, c);
            right.add(button);

            c.gridx = 1;
            c.gridy = i;
            layout.setConstraints(spinner, c);
            right.add(spinner);

            JTextField text = buildRGBTextField();

            JLabel rgbLabel = new JLabel(namesRGB[i]);
            rgbLabel.setLabelFor(text);
            rgbText[i] = text;

            c.gridx = 0;
            c.gridy = i + 4;
            c.weightx = 0;
            c.insets.left = rgbInset;
            c.anchor = GridBagConstraints.WEST;
            layout.setConstraints(rgbLabel, c);
            right.add(rgbLabel);

            c.gridx = 1;
            c.gridy = i + 4;
            c.weightx = 1;
            c.insets.left = 0;
            c.anchor = GridBagConstraints.WEST;
            layout.setConstraints(text, c);
            right.add(text);
        }
        c.gridx = 0;
        c.gridy = 3;
        JPanel separator = new JPanel();
        layout.setConstraints(separator, c);
        right.add(separator);

        radioButtons[0].setSelected(true);

        JPanel fullPanel = new JPanel();
        fullPanel.add(left);
        fullPanel.add(right);
        this.add(fullPanel);
    }

    private void installRadioButtonListener(final JRadioButton button) {
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < 3; i++) {
                    if (radioButtons[i].isSelected()) {
                        selectionMode = i;
                    }
                }
                updateSelector();
            }
        });
    }

    private void updateSelector() {
        ChangeListener listener = slider.getChangeListeners()[0];
        slider.removeChangeListener(listener);
        slider.setMaximum(MAX[selectionMode]);
        slider.setValue(((Number)spinners[selectionMode].getValue()).intValue());
        slider.setInverted(SLIDER_INVERTED[selectionMode]);
        slider.addChangeListener(listener);

        sliderImage.setSelectionMode(selectionMode);
        selector.setSelectionMode(selectionMode);
        sliderImage.repaint();
        selector.repaint();
    }

    private void updateColor(final int x, final int y) {
        Color color = getColorSelectionModel().getSelectedColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        float xColor = (float)(SELECTOR_WIDTH - x) / SELECTOR_WIDTH;
        xColor = (xColor >= 1.f) ? 1.f : xColor;
        xColor = (xColor <= 0.f) ? 0.f : xColor;
        float yColor = (float)(COMPONENTS_HEIGHT - y) / COMPONENTS_HEIGHT;
        yColor = (yColor >= 1.f) ? 1.f : yColor;
        yColor = (yColor <= 0.f) ? 0.f : yColor;

        switch (selectionMode) {
        case HUE: hsb[SATURATION] = xColor;
                  hsb[BRIGHTNESS] = yColor;
                  break;
        case SATURATION: hsb[HUE] = xColor;
                         hsb[BRIGHTNESS] = yColor;
                         break;
        case BRIGHTNESS: hsb[HUE] = xColor;
                         hsb[SATURATION] = yColor;
                         break;
        }
        getColorSelectionModel().setSelectedColor(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
    }

    private JPanel buildSelectorPanel() {
        JPanel selectorPanel = new JPanel();
        selector = new Selector();
        MouseInputAdapter selectorMouseAdapter = new MouseInputAdapter() {
            public void mousePressed(MouseEvent e) {
                updateColor(e.getX(), e.getY());
            }
            public void mouseDragged(MouseEvent e) {
                updateColor(e.getX(), e.getY());
            }
        };
        selector.addMouseListener(selectorMouseAdapter);
        selector.addMouseMotionListener(selectorMouseAdapter);
        selectorPanel.add(selector);

        selectionMode = HUE;
        slider = new JSlider(JSlider.VERTICAL);
        slider.setMinimum(0);
        slider.setPaintTrack(false);
        slider.setInverted(true);
        slider.setPreferredSize(new Dimension(slider.getPreferredSize().width,
                                                    COMPONENTS_HEIGHT + 24));
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (internalUpdateDisabled || slider.getValueIsAdjusting()) {
                    return;
                }
                Color color = getColorSelectionModel().getSelectedColor();
                float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                hsb[selectionMode] = 1.f * slider.getValue() / MAX[selectionMode];
                getColorSelectionModel().setSelectedColor(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
            }
        });
        selectorPanel.add(slider);
        sliderImage = new SliderImage();
        selectorPanel.add(sliderImage);
        return selectorPanel;
    }

    private JSpinner buildHSBSpinner(int hsbComponent) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, MAX[hsbComponent], 1));
        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (internalUpdateDisabled) {
                    return;
                }

                Color color = getColorSelectionModel().getSelectedColor();
                float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

                for (int i = 0; i < 3; i++) {
                    if (e.getSource() == spinners[i]) {
                        hsb[i] = ((Number)spinners[i].getValue()).floatValue() / MAX[i];
                    }
                }
                getColorSelectionModel().setSelectedColor(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
            }
        });
        Dimension stringSize = Utilities.getStringSize("999", spinner.getEditor().getFontMetrics(spinner.getEditor().getFont()));
        Utilities.addInsets(stringSize, spinner.getEditor().getInsets());
        spinner.getEditor().setPreferredSize(new Dimension(stringSize.width, stringSize.height));

        return spinner;
    }

    private static JTextField buildRGBTextField() {
        JTextField text = new JTextField();
        Dimension stringSize = Utilities.getStringSize("999", text.getFontMetrics(text.getFont()));
        Utilities.addInsets(stringSize, text.getInsets());
        text.setPreferredSize(new Dimension(stringSize.width + 2, stringSize.height));
        text.setEditable(false);
        text.setHorizontalAlignment(SwingConstants.RIGHT);
        return text;
    }
}