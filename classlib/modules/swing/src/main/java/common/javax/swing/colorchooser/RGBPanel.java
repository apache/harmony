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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.harmony.x.swing.Utilities;

class RGBPanel extends AbstractColorChooserPanel {
    
    private JSlider[] sliders;
    private JSpinner[] spinners;
    
    public String getDisplayName() {
        return UIManager.getString("ColorChooser.rgbNameText");
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
        float[] rgb = color.getRGBColorComponents(null);
        for (int i = 0; i < 3; i++) {
            int colorComponent = Math.round(rgb[i] * 255);
            sliders[i].setValue(colorComponent);
            spinners[i].setValue(new Integer(colorComponent));
        }
    }

    protected void buildChooser() {
        mnemonic = UIManager.getInt("ColorChooser.rgbMnemonic");
        displayedMnemonicIndex = Integer.parseInt(UIManager.getString("ColorChooser.rgbDisplayedMnemonicIndex"));        
        
        String[] namesRGB = {UIManager.getString("ColorChooser.rgbRedText"), 
                             UIManager.getString("ColorChooser.rgbGreenText"), 
                             UIManager.getString("ColorChooser.rgbBlueText")};
        int[] mnemonics = {UIManager.getInt("ColorChooser.rgbRedMnemonic"),
                           UIManager.getInt("ColorChooser.rgbGreenMnemonic"),
                           UIManager.getInt("ColorChooser.rgbBlueMnemonic")};
        
        JPanel panel = new JPanel(new GridLayout(3, 1));
        Color color = getColorSelectionModel().getSelectedColor();
        float[] rgb = color == null 
                      ? Color.BLACK.getRGBColorComponents(null) 
                      : color.getRGBColorComponents(null);

        JPanel[] panels = {new JPanel(),
                           new JPanel(),
                           new JPanel()};
        
        sliders = new JSlider[3];
        spinners = new JSpinner[3];
        
        for (int i = 0; i < 3; i++) {
            int colorComponent = Math.round(rgb[i] * 255);
            JSlider slider = new JSlider(0, 255, colorComponent);
            sliders[i] = slider;
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.setMajorTickSpacing(255 / 3);
            slider.setMinorTickSpacing(255 / 15);
            
            slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    Color color = getColorSelectionModel().getSelectedColor();
                    float[] rgb = color.getRGBColorComponents(null);
                    
                    for (int i = 0; i < 3; i++) {
                        if (e.getSource() == sliders[i]) {
                            rgb[i] = sliders[i].getValue() / 255f;
                        }
                    }                    
                    getColorSelectionModel().setSelectedColor(new Color(rgb[0], rgb[1], rgb[2]));
                }                
            });
            
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(colorComponent, 0, 255, 1));
            spinners[i] = spinner;
            Dimension stringSize = Utilities.getStringSize("999", spinner.getEditor().getFontMetrics(spinner.getEditor().getFont()));
            Utilities.addInsets(stringSize, spinner.getEditor().getInsets());
            spinner.getEditor().setPreferredSize(new Dimension(stringSize.width, stringSize.height));
            spinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    Color color = getColorSelectionModel().getSelectedColor();
                    float[] rgb = color.getRGBColorComponents(null);
                    
                    for (int i = 0; i < 3; i++) {
                        if (e.getSource() == spinners[i]) {
                            rgb[i] = ((Number)spinners[i].getValue()).floatValue() / 255;
                        }
                    }
                    getColorSelectionModel().setSelectedColor(new Color(rgb[0], rgb[1], rgb[2]));
                }                
            });
            JPanel spinnerPanel = new JPanel();
            spinnerPanel.add(spinner);
            
            JLabel label = new JLabel(namesRGB[i]);
            label.setLabelFor(slider);
            label.setDisplayedMnemonic(mnemonics[i]);
            
            panels[i].add(label);
            panels[i].add(slider);
            panels[i].add(spinnerPanel);
            ((FlowLayout)panels[i].getLayout()).setAlignment(FlowLayout.RIGHT);
            panel.add(panels[i]);
        }
        
        this.add(panel);
    }        
}
