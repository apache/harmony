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
import javax.swing.BasicSwingTestCase;
import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.UIManager;

public class ColorChooserComponentFactoryTest extends BasicSwingTestCase {
    private JColorChooser colorChooser;

    @Override
    public void setUp() throws Exception {
        colorChooser = new JColorChooser(Color.GREEN);
    }

    @Override
    public void tearDown() throws Exception {
        colorChooser = null;
    }

    public void testSwatchPanel() throws Exception {
        AbstractColorChooserPanel swatchPanel = ColorChooserComponentFactory
                .getDefaultChooserPanels()[0];
        UIManager.put("ColorChooser.swatchesSwatchSize", new Dimension(25, 25));
        assertEquals("Swatches", swatchPanel.getDisplayName());
        assertNull(swatchPanel.getSmallDisplayIcon());
        assertNull(swatchPanel.getLargeDisplayIcon());
    }

    public void testRGBPanel() throws Exception {
        AbstractColorChooserPanel rgbPanel = ColorChooserComponentFactory
                .getDefaultChooserPanels()[2];
        assertEquals("RGB", rgbPanel.getDisplayName());
        assertNull(rgbPanel.getSmallDisplayIcon());
        assertNull(rgbPanel.getLargeDisplayIcon());
        rgbPanel.installChooserPanel(colorChooser);
        assertNull(rgbPanel.getSmallDisplayIcon());
        assertNull(rgbPanel.getLargeDisplayIcon());
        Component[] components = ((JComponent) rgbPanel.getComponent(0)).getComponents();
        int slidersCount = 0;
        // different layout on panels
        if (!isHarmony()) {
            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof JSlider) {
                    JSlider slider = (JSlider) components[i];
                    slider.setValue(35 * slidersCount);
                    assertTrue(slider.getPaintLabels());
                    assertTrue(slider.getPaintTicks());
                    slidersCount++;
                }
            }
        } else {
            for (int i = 0; i < components.length; i++) {
                JPanel panel = (JPanel) components[i];
                if (panel.getComponent(1) instanceof JSlider) {
                    JSlider slider = (JSlider) panel.getComponent(1);
                    slider.setValue(35 * slidersCount);
                    assertTrue(slider.getPaintLabels());
                    assertTrue(slider.getPaintTicks());
                    slidersCount++;
                }
            }
        }
        assertEquals(3, slidersCount);
        assertEquals(new Color(0, 35, 70), colorChooser.getColor());
        int spinnersCount = 0;
        // different layout on panels
        if (!isHarmony()) {
            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof JPanel) {
                    JSpinner spinner = (JSpinner) ((JPanel) components[i]).getComponent(0);
                    spinner.setValue(new Integer(66 * spinnersCount));
                    spinnersCount++;
                }
            }
        } else {
            for (int i = 0; i < components.length; i++) {
                JPanel panel = (JPanel) (((JPanel) components[i]).getComponent(2));
                if (panel.getComponent(0) instanceof JSpinner) {
                    JSpinner spinner = (JSpinner) panel.getComponent(0);
                    spinner.setValue(new Integer(66 * spinnersCount));
                    spinnersCount++;
                }
            }
        }
        assertEquals(3, spinnersCount);
        assertEquals(new Color(0, 66, 132), colorChooser.getColor());
    }

    public void testHSBPanel() throws InterruptedException {
        AbstractColorChooserPanel hsbPanel = ColorChooserComponentFactory
                .getDefaultChooserPanels()[1];
        assertEquals("HSB", hsbPanel.getDisplayName());
        assertNull(hsbPanel.getSmallDisplayIcon());
        assertNull(hsbPanel.getLargeDisplayIcon());
        hsbPanel.installChooserPanel(colorChooser);
        assertNull(hsbPanel.getSmallDisplayIcon());
        assertNull(hsbPanel.getLargeDisplayIcon());
        Component[] components = ((JComponent) hsbPanel.getComponent(0)).getComponents();
        JSlider slider;
        //      different layout on panels
        if (!isHarmony()) {
            slider = ((JSlider) components[1]);
        } else {
            slider = (JSlider) ((JComponent) components[0]).getComponent(1);
        }
        assertFalse(slider.getPaintTrack());
        assertTrue(slider.getInverted());
        slider.setValue(35);
        assertEquals(colorChooser.getColor(), new Color(255, 149, 0));
        int spinnerCount = 0;
        // different layout on panels
        if (!isHarmony()) {
            components = ((JComponent) ((JComponent) hsbPanel.getComponent(1)).getComponent(0))
                    .getComponents();
            for (int i = 0; i < components.length; i++) {
                ((JComponent) components[i]).setBorder(BorderFactory.createLineBorder(
                        Color.GREEN, 1));
                if (components[i] instanceof JSpinner) {
                    JSpinner spinner = (JSpinner) components[i];
                    spinner.setValue(new Integer(100));
                    spinnerCount++;
                }
            }
        } else {
            components = ((JComponent) hsbPanel.getComponent(0)).getComponents();
            components = ((JComponent) components[1]).getComponents();
            for (int i = 0; i < components.length; i++) {
                Component component = components[i];
                if (component instanceof JSpinner) {
                    JSpinner spinner = (JSpinner) component;
                    spinner.setValue(new Integer(100));
                    spinnerCount++;
                }
            }
        }
        assertEquals(3, spinnerCount);
        assertEquals(colorChooser.getColor(), new Color(84, 255, 0));
    }

    public void testPreviewPanel() throws Exception {
        JComponent panel = ColorChooserComponentFactory.getPreviewPanel();
        assertTrue(panel instanceof JPanel);
        assertEquals(colorChooser.getPreviewPanel().getForeground(), Color.GREEN);
        colorChooser.setColor(Color.RED);
        assertEquals(colorChooser.getPreviewPanel().getForeground(), Color.RED);
    }
}
