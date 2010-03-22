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
 * @author Sergey Burlak
 */
package javax.swing.plaf.basic;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorChooserComponentFactory;
import javax.swing.colorchooser.ColorSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorChooserUI;
import javax.swing.plaf.ComponentUI;

import org.apache.harmony.x.swing.Utilities;

public class BasicColorChooserUI extends ColorChooserUI {
    public class PropertyHandler extends Object implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
            String propertyName = event.getPropertyName();
            if (JColorChooser.CHOOSER_PANELS_PROPERTY.equals(propertyName)) {
                AbstractColorChooserPanel[] newPanels = (AbstractColorChooserPanel[])event.getNewValue();
                AbstractColorChooserPanel[] oldPanels = (AbstractColorChooserPanel[])event.getOldValue();
                if (newPanels == null || newPanels.length == 0) {
                    return;
                }
                
                for (int i = 0; i < oldPanels.length; i++) {
                    oldPanels[i].uninstallChooserPanel(chooser);
                }
                for (int i = 0; i < newPanels.length; i++) {
                    newPanels[i].installChooserPanel(chooser);
                }
                
                if (newPanels.length == 1) {
                    centerPanel.removeAll();
                    centerPanel.add(newPanels[0]);
                    if (tabbedPane.getParent() != null) {
                        chooser.remove(tabbedPane);
                    }
                    if (centerPanel.getParent() == null) {
                        chooser.add(centerPanel, BorderLayout.CENTER);
                    }
                } else {
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        tabbedPane.removeTabAt(i);
                    }
                    for (int i = 0; i < newPanels.length; i++) {
                        tabbedPane.insertTab(newPanels[i].getDisplayName(), null, newPanels[i], null, i);
                        tabbedPane.setMnemonicAt(i, newPanels[i].getMnemonic());
                        tabbedPane.setDisplayedMnemonicIndexAt(i, newPanels[i].getDisplayedMnemonicIndex());
                    }
                    if (centerPanel.getParent() != null) {
                        chooser.remove(centerPanel);
                    }
                    if (tabbedPane.getParent() == null) {
                        chooser.add(tabbedPane, BorderLayout.CENTER);
                    }
                }
                
                chooser.revalidate();
                chooser.repaint();
            } else if (JColorChooser.PREVIEW_PANEL_PROPERTY.equals(propertyName)) {
                installPreviewPanel();

                chooser.revalidate();
                chooser.repaint();
            } else if (JColorChooser.SELECTION_MODEL_PROPERTY.equals(propertyName)) {
                if (model != null) {
                    model.removeChangeListener(previewListener);
                }
                model = (ColorSelectionModel)event.getNewValue();
                if (model != null) {
                    model.addChangeListener(previewListener);
                }
            }
            
            chooser.revalidate();
            chooser.repaint();
        }
    }
    
    protected JColorChooser chooser;
    protected AbstractColorChooserPanel[] defaultChoosers;
    protected ChangeListener previewListener;
    protected PropertyChangeListener propertyChangeListener;
    
    private JPanel centerPanel;
    private JTabbedPane tabbedPane;
    private ColorSelectionModel model;
    private JPanel previewPanel;
    
    public static ComponentUI createUI(final JComponent c) {
        return new BasicColorChooserUI();
    }

    protected AbstractColorChooserPanel[] createDefaultChoosers() {
        return ColorChooserComponentFactory.getDefaultChooserPanels();
    }

    protected void uninstallDefaultChoosers() {
        AbstractColorChooserPanel[] chooserPanels = chooser.getChooserPanels();
        for (int i = 0; i < chooserPanels.length; i++) {
            chooser.removeChooserPanel(chooserPanels[i]);
        }
    }

    public void installUI(final JComponent c) {
        chooser = (JColorChooser)c;
        model = chooser.getSelectionModel();
        
        installDefaults();
        installListeners();
        
        tabbedPane = new JTabbedPane();
        centerPanel = new JPanel();
        previewPanel = new JPanel();
        
        installPreviewPanel();
        chooser.setChooserPanels(createDefaultChoosers());        
    }

    public void uninstallUI(final JComponent c) {
        uninstallListeners();
        uninstallDefaultChoosers();
        uninstallDefaults();
    }

    protected void installPreviewPanel() {
        if (chooser.getPreviewPanel() == null) {
            chooser.setPreviewPanel(ColorChooserComponentFactory.getPreviewPanel());
            chooser.getPreviewPanel().setForeground(model.getSelectedColor());
            
            return;
        }
        previewPanel.removeAll();
        previewPanel.add(chooser.getPreviewPanel());
        previewPanel.setBorder(BorderFactory.createTitledBorder(UIManager.getString("ColorChooser.previewText")));

        if (previewPanel.getParent() == null) {
            chooser.add(previewPanel, BorderLayout.SOUTH);
        }
    }

    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(chooser, "ColorChooser.background", "ColorChooser.foreground", "ColorChooser.font");
        LookAndFeel.installProperty(chooser, "opaque", Boolean.FALSE);
    }
    
    protected void uninstallDefaults() {
        Utilities.uninstallColorsAndFont(chooser);
    }
    
    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        chooser.addPropertyChangeListener(propertyChangeListener);
        
        previewListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                chooser.getPreviewPanel().setForeground(model.getSelectedColor());
            }
        };
        model.addChangeListener(previewListener);
    }

    protected void uninstallListeners() {
        chooser.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;
        
        model.removeChangeListener(previewListener);
        previewListener = null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyHandler();
    }
}
