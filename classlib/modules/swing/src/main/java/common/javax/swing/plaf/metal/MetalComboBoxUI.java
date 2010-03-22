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
 * @author Anton Avtamonov
 */

package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;


public class MetalComboBoxUI extends BasicComboBoxUI {
    /**
     * @deprecated Do nothing and should not be used.
     */
    public class MetalComboPopup extends BasicComboPopup {
        public MetalComboPopup(final JComboBox comboBox) {
            super(comboBox);
        }

        public void delegateFocus(final MouseEvent e) {
            super.delegateFocus(e);
        }
    }

    public class MetalComboBoxLayoutManager extends ComboBoxLayoutManager {
       public void layoutContainer(final Container parent) {
           layoutComboBox(parent, this);
       }

       public void superLayout(final Container parent) {
           super.layoutContainer(parent);
       }
    }

    public class MetalPropertyChangeListener extends PropertyChangeHandler {
        public void propertyChange(final PropertyChangeEvent event) {
            if (StringConstants.EDITABLE_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                if (arrowButton instanceof MetalComboBoxButton) {
                    ((MetalComboBoxButton)arrowButton).setIconOnly(((Boolean)event.getNewValue()).booleanValue());
                }
            }
            super.propertyChange(event);
        }
    }



    public static ComponentUI createUI(final JComponent c) {
        return new MetalComboBoxUI();
    }

    public void paint(final Graphics g, final JComponent c) {

    }

    public void paintCurrentValue(final Graphics g, final Rectangle bounds, final boolean hasFocus) {
        super.paintCurrentValue(g, bounds, hasFocus);
    }

    public void paintCurrentValueBackground(final Graphics g, final Rectangle bounds, final boolean hasFocus) {
        super.paintCurrentValueBackground(g, bounds, hasFocus);
    }

    public PropertyChangeListener createPropertyChangeListener() {
        return new MetalPropertyChangeListener();
    }

    public void configureEditor() {
        super.configureEditor();
    }

    public void unconfigureEditor() {
        super.unconfigureEditor();
    }

    public void layoutComboBox(final Container parent, final MetalComboBoxLayoutManager manager) {
        if (comboBox.isEditable()) {
            manager.superLayout(parent);
        } else if (arrowButton != null) {
            Rectangle bounds = comboBox.getBounds();
            Insets insets = getInsets();
            arrowButton.setBounds(insets.left, insets.top, bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
        }
    }

    public Dimension getMinimumSize(final JComponent c) {
        if (!isMinimumSizeDirty) {
            return cachedMinimumSize;
        }

        Dimension result;
        if (arrowButton instanceof MetalComboBoxButton) {
            Dimension displaySize = getDisplaySize();
            int arrowIconWidth = ((MetalComboBoxButton)arrowButton).getComboIcon().getIconWidth();
            Insets arrowButtonInsets = arrowButton.getInsets();

            result = new Dimension();
            result.height = displaySize.height + arrowButtonInsets.top + arrowButtonInsets.bottom;
            result.width = displaySize.width + arrowIconWidth + arrowButtonInsets.left + arrowButtonInsets.right + arrowButtonInsets.right;

            Utilities.addInsets(result, comboBox.getInsets());

            cachedMinimumSize.setSize(result);
            isMinimumSizeDirty = false;
        } else {
            result = super.getMinimumSize(c);
        }

        return result;
    }

    /**
     * @deprecated
     */
    protected void removeListeners() {

    }

    /**
     * @deprecated
     */
    protected void editablePropertyChanged(final PropertyChangeEvent e) {

    }

    protected ComboBoxEditor createEditor() {
        return new MetalComboBoxEditor.UIResource();
    }

    protected ComboPopup createPopup() {
        return new MetalComboPopup(comboBox);
    }

    protected JButton createArrowButton() {
        JButton result = new MetalComboBoxButton(comboBox, new MetalComboBoxIcon(), false, currentValuePane, listBox);

        //Looks like a workarround!
        result.setBorder(new BorderUIResource.CompoundBorderUIResource(new MetalBorders.ButtonBorder(), new BasicBorders.MarginBorder() {
            public Insets getBorderInsets(final Component c) {
                return new Insets(0, 1, 1, 3);
            }
        }));

        return result;
    }

    protected LayoutManager createLayoutManager() {
        return new MetalComboBoxLayoutManager();
    }
}
