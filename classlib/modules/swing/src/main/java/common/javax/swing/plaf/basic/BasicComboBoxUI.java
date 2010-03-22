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

package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.swing.CellRendererPane;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.ExtendedListElement;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class BasicComboBoxUI extends ComboBoxUI {

    public class ComboBoxLayoutManager implements LayoutManager {
        public void addLayoutComponent(final String name, final Component component) {
        }

        public void removeLayoutComponent(final Component component) {

        }

        public Dimension preferredLayoutSize(final Container parent) {
            if (parent == null) {
                throw new NullPointerException(Messages.getString("swing.03","Parent")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return new Dimension(0, 0);
        }

        public Dimension minimumLayoutSize(final Container parent) {
            if (parent == null) {
                throw new NullPointerException(Messages.getString("swing.03","Parent")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return new Dimension(0, 0);
        }

        public void layoutContainer(final Container parent) {
            Rectangle bounds = comboBox.getBounds();
            Insets insets = getInsets();
            int arrowButtonSize = bounds.height - insets.top - insets.bottom;
            if (comboBox.getComponentOrientation().isLeftToRight()) {
                arrowButton.setBounds(bounds.width - arrowButtonSize - insets.right, insets.top, arrowButtonSize, arrowButtonSize);
                cachedTextPartBounds.setBounds(insets.left, insets.top, bounds.width - insets.left - insets.right - arrowButtonSize, arrowButtonSize);
                if (comboBox.isEditable()) {
                    editor.setBounds(cachedTextPartBounds);
                }
            } else {
                arrowButton.setBounds(insets.left, insets.top, arrowButtonSize, arrowButtonSize);
                cachedTextPartBounds.setBounds(insets.left + arrowButtonSize, insets.top, bounds.width - insets.left - insets.right - arrowButtonSize, arrowButtonSize);
                if (comboBox.isEditable()) {
                    editor.setBounds(cachedTextPartBounds);
                }
            }
        }
    }

    public class FocusHandler implements FocusListener {
        public void focusGained(final FocusEvent e) {
            hasFocus = true;
            comboBox.repaint();
        }

        public void focusLost(final FocusEvent e) {
            hasFocus = false;
            comboBox.repaint();
        }
    }


    public class ItemHandler implements ItemListener {
        public void itemStateChanged(final ItemEvent e) {

        }
    }

    public class KeyHandler extends KeyAdapter {
        public void keyPressed(final KeyEvent e) {
            if (!isNavigationKey(e.getKeyCode())) {
                comboBox.selectWithKeyChar(e.getKeyChar());
            }
        }
    }

    public class ListDataHandler implements ListDataListener {
        public void contentsChanged(final ListDataEvent e) {
            if (comboBox.isEditable()) {
                comboBox.configureEditor(comboBox.getEditor(), comboBox.getSelectedItem());
            }
            listChanged();
        }

        public void intervalAdded(final ListDataEvent e) {
            listChanged();
        }

        public void intervalRemoved(final ListDataEvent e) {
            listChanged();
        }

        private void listChanged() {
            isMinimumSizeDirty = true;
            cachedDisplaySize = null;
            comboBox.revalidate();
            comboBox.repaint();
        }
    }

    public class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
            if (StringConstants.ENABLED_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                if (arrowButton != null) {
                arrowButton.setEnabled(((Boolean)event.getNewValue()).booleanValue());
                }
                if (comboBox.isEditable() && (editor != null)) {
                    editor.setEnabled(((Boolean)event.getNewValue()).booleanValue());
                }
            } else if (StringConstants.TOOLTIP_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                if (arrowButton != null) {
                arrowButton.setToolTipText((String)event.getNewValue());
                }
                if (comboBox.isEditable() && (editor instanceof JComponent)) {
                    ((JComponent)editor).setToolTipText((String)event.getNewValue());
                }
            } else if (StringConstants.FONT_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                if (arrowButton != null) {
                arrowButton.setFont((Font)event.getNewValue());
                }
                if (comboBox.isEditable() && (editor != null)) {
                    editor.setFont((Font)event.getNewValue());
                }
            } else if (StringConstants.EDITABLE_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                if (((Boolean)event.getNewValue()).booleanValue()) {
                    addEditor();
                } else {
                    removeEditor();
                }
            } else if (StringConstants.MODEL_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                ComboBoxModel newModel = (ComboBoxModel)event.getNewValue();
                listBox.setModel(newModel);
                comboBox.configureEditor(comboBox.getEditor(), newModel.getSelectedItem());

                ComboBoxModel oldModel = (ComboBoxModel)event.getOldValue();
                oldModel.removeListDataListener(listDataListener);
                newModel.addListDataListener(listDataListener);
            } else if (StringConstants.COMPONENT_ORIENTATION.equals(event.getPropertyName())) {
                listBox.setComponentOrientation((ComponentOrientation) event.getNewValue());
            } else if (StringConstants.EXTENDED_SUPPORT_ENABLED_PROPERTY
                    .equals(event.getPropertyName())) {
                listBox.putClientProperty(
                        StringConstants.EXTENDED_SUPPORT_ENABLED_PROPERTY,
                        event.getNewValue());
                if (((Boolean) event.getNewValue()).booleanValue()) {
                    for (int i = 0; i < comboBox.getModel().getSize(); i++) {
                        Object element = comboBox.getModel().getElementAt(i);
                        if (!(element instanceof ExtendedListElement)
                                || ((ExtendedListElement) element)
                                        .isChoosable()) {

                            comboBox.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            } else if (StringConstants.IS_TABLE_EDITOR.equals(event.getPropertyName())) {
                isTableEditor = ((Boolean)event.getNewValue()).booleanValue();
            } else if (StringConstants.EDITOR_PROPERTY_CHANGED.equals(event.getPropertyName())) {
                removeEditor();
                addEditor();
            }

            isMinimumSizeDirty = true;
            cachedDisplaySize = null;
            comboBox.revalidate();
            comboBox.repaint();
        }
    }


    private class EditorFocusHandler extends FocusHandler {
        public void focusLost(final FocusEvent e) {
            super.focusLost(e);
            ActionEvent actionEvent = new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null);
            editorActionListener.actionPerformed(actionEvent);
            comboBox.actionPerformed(actionEvent);
        }
    }

    private class DefaultKeySelectionManager implements JComboBox.KeySelectionManager {
        private final StringBuffer keySequence = new StringBuffer();
        private final Timer timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                clearKeySequence();
                timer.stop();
            }
        });

        public int selectionForKey(final char keyChar, final ComboBoxModel model) {
            keySequence.append(keyChar);
            timer.start();
            int result = findNextOccurence(model);
            if (result != -1) {
                return result;
            } else {
                clearKeySequence();
                keySequence.append(keyChar);
                return findNextOccurence(model);
            }
        }

        private void clearKeySequence() {
            keySequence.delete(0, keySequence.length());
        }

        private int findNextOccurence(final ComboBoxModel model) {
            String beginPart = keySequence.toString().toUpperCase();
            int selectedIndex = getIndex(model.getSelectedItem(), model);
            for (int i = selectedIndex + 1; i < model.getSize(); i++) {
                Object item = model.getElementAt(i);
                if (item.toString().toUpperCase().startsWith(beginPart)
                        && isChoosable(item)) {

                    return i;
                }
            }

            for (int i = 0; i <= selectedIndex; i++) {
                Object item = model.getElementAt(i);
                if (item.toString().toUpperCase().startsWith(beginPart)
                        && isChoosable(item)) {

                    return i;
                }
            }

            return -1;
        }

        private int getIndex(final Object element, final ComboBoxModel model) {
            if (element == null) {
                return -1;
            }

            for (int i = 0; i < model.getSize(); i++) {
                if (element.equals(model.getElementAt(i))) {
                    return i;
                }
            }

            return -1;
        }

        private boolean isChoosable(final Object element) {
            return !((BasicListUI) listBox.getUI()).extendedSupportEnabled
                    || !(element instanceof ExtendedListElement)
                    || ((ExtendedListElement) element).isChoosable();
        }

    }


    protected JButton arrowButton;
    protected JList listBox;
    protected JComboBox comboBox;
    protected ComboPopup popup;
    protected CellRendererPane currentValuePane;
    protected Component editor;
    protected boolean hasFocus;
    protected Dimension cachedMinimumSize = new Dimension();
    protected boolean isMinimumSizeDirty = true;
    protected FocusListener focusListener;
    protected ItemListener itemListener;
    protected KeyListener keyListener;
    protected ListDataListener listDataListener;
    protected KeyListener popupKeyListener;
    protected MouseListener popupMouseListener;
    protected MouseMotionListener popupMouseMotionListener;
    protected PropertyChangeListener propertyChangeListener;

    boolean isTableEditor;

    private Dimension cachedDisplaySize;
    private Rectangle cachedTextPartBounds = new Rectangle();
    private Insets cachedInsets = new Insets(0, 0, 0, 0);

    private FocusListener editorFocusListener;
    private ActionListener editorActionListener;
    private Object selectedValue;

    private static final String PROTOTYPE_VALUE_FOR_EDITABLE_COMBOBOX = "wwwwwwwwww";

    public static ComponentUI createUI(final JComponent comboBox) {
        return new BasicComboBoxUI();
    }

    public BasicComboBoxUI() {
        currentValuePane = new CellRendererPane();
        currentValuePane.setVisible(false);
    }

    public void installUI(final JComponent c) {
        comboBox = (JComboBox)c;
        comboBox.setLayout(createLayoutManager());

        if (comboBox.getRenderer() == null || comboBox.getRenderer() instanceof UIResource) {
            comboBox.setRenderer(createRenderer());
        }

        if (comboBox.getEditor() == null || comboBox.getEditor() instanceof UIResource) {
            comboBox.setEditor(createEditor());
        }

        popup = createPopup();
        listBox = popup.getList();

        installListeners();
        installComponents();
        installKeyboardActions();
        installDefaults();
    }

    public void uninstallUI(final JComponent c) {
        uninstallDefaults();
        uninstallKeyboardActions();
        uninstallComponents();
        uninstallListeners();

        popup.uninstallingUI();

        if (comboBox.getRenderer() instanceof UIResource) {
            comboBox.setRenderer(null);
        }
        if (comboBox.getEditor() instanceof UIResource) {
            comboBox.setEditor(null);
        }

        comboBox.remove(currentValuePane);

        comboBox.setLayout(null);

        comboBox = null;
        listBox = null;
        popup = null;
        arrowButton = null;
    }

    public void addEditor() {
        ComboBoxEditor cbe = comboBox.getEditor();
        if (cbe == null)
            return;
            
        editor = cbe.getEditorComponent();
        if (editor == null)
            return;
        
        configureEditor();
        comboBox.add(editor);
    }

    public void removeEditor() {
        if (editor != null){
            comboBox.remove(editor);
            unconfigureEditor();
            editor = null;
        }
    }

    public boolean isPopupVisible(final JComboBox c) {
        return popup.isVisible();
    }

    public void setPopupVisible(final JComboBox c, final boolean isVisible) {
        if (isVisible == isPopupVisible(c)) {
            return;
        }
        if (isVisible) {
            popup.show();
            selectedValue = popup.getList().getSelectedValue();
        } else {
            popup.hide();
        }
    }

    public boolean isFocusTraversable(final JComboBox c) {
        if (comboBox.isEditable()) {
            return false;
        } else {
            return true;
        }
    }

    public void paint(final Graphics g, final JComponent c) {
        paintCurrentValueBackground(g, cachedTextPartBounds, hasFocus);
        paintCurrentValue(g, cachedTextPartBounds, hasFocus);
    }

    public void paintCurrentValue(final Graphics g, final Rectangle bounds, final boolean hasFocus) {
        Component renderer = comboBox.getRenderer().getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, hasFocus, hasFocus);
        if (!comboBox.isEnabled()) {
            renderer.setForeground(UIManager.getColor("ComboBox.disabledForeground"));
            renderer.setBackground(UIManager.getColor("ComboBox.disabledBackground"));
        } else if (hasFocus) {
            renderer.setForeground(UIManager.getColor("ComboBox.selectionForeground"));
            renderer.setBackground(UIManager.getColor("ComboBox.selectionBackground"));
        }

        currentValuePane.paintComponent(g, renderer, comboBox, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void paintCurrentValueBackground(final Graphics g, final Rectangle bounds, final boolean hasFocus) {
        Color oldColor = g.getColor();
        if (comboBox == null) {
            throw new NullPointerException(Messages.getString("swing.03","ComboBox")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (hasFocus) {
            g.setColor(UIManager.getColor("ComboBox.selectionBackground"));
        } else {
            if (comboBox.isEnabled()) {
                g.setColor(comboBox.getBackground());
            } else {
                g.setColor(UIManager.getColor("ComboBox.disabledBackground"));
            }
        }
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        g.setColor(oldColor);
    }

    public Dimension getPreferredSize(final JComponent c) {
        return getMinimumSize(c);
    }

    public Dimension getMinimumSize(final JComponent c) {
        Dimension result;
        if (isMinimumSizeDirty) {
            result = getDisplaySize();
            result.width += result.height;
            Utilities.addInsets(result, getInsets());

            cachedMinimumSize.setSize(result);
            isMinimumSizeDirty = false;
        } else {
            result = new Dimension(cachedMinimumSize);
        }
        return result;
    }

    public Dimension getMaximumSize(final JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public void configureArrowButton() {
        if (arrowButton == null) {
            return;
        }
        arrowButton.addMouseListener(popup.getMouseListener());
        arrowButton.addMouseMotionListener(popup.getMouseMotionListener());
    }

    public void unconfigureArrowButton() {
        if (arrowButton == null) {
            return;
        }
        arrowButton.removeMouseListener(popup.getMouseListener());
        arrowButton.removeMouseMotionListener(popup.getMouseMotionListener());
    }


    public int getAccessibleChildrenCount(final JComponent c) {
        if (comboBox == null) {
            throw new NullPointerException(Messages.getString("swing.03","ComboBox")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return 0;
    }

    public Accessible getAccessibleChild(final JComponent c, final int i) {
        return null;
    }

    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(comboBox, "ComboBox.background", "ComboBox.foreground", "ComboBox.font");
    }

    protected void uninstallDefaults() {
        Utilities.uninstallColorsAndFont(comboBox);
    }

    protected void installListeners() {
        focusListener = createFocusListener();
        comboBox.addFocusListener(focusListener);

        keyListener = createKeyListener();
        comboBox.addKeyListener(keyListener);

        listDataListener = createListDataListener();
        comboBox.getModel().addListDataListener(listDataListener);

        propertyChangeListener = createPropertyChangeListener();
        comboBox.addPropertyChangeListener(propertyChangeListener);


        popupMouseListener = popup.getMouseListener();
        comboBox.addMouseListener(popupMouseListener);

        popupMouseMotionListener = popup.getMouseMotionListener();
        comboBox.addMouseMotionListener(popupMouseMotionListener);

        popupKeyListener = popup.getKeyListener();
        if (popupKeyListener != null) {
            comboBox.addKeyListener(popupKeyListener);
        }
    }

    protected void uninstallListeners() {
        comboBox.removeFocusListener(focusListener);
        focusListener = null;

        comboBox.removeKeyListener(keyListener);
        keyListener = null;

        popup.getList().getModel().removeListDataListener(listDataListener);
        listDataListener = null;

        comboBox.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;

        comboBox.removeMouseListener(popupMouseListener);
        popupMouseListener = null;

        comboBox.removeMouseMotionListener(popupMouseMotionListener);
        popupMouseMotionListener = null;

        if (popupKeyListener != null) {
            comboBox.removeKeyListener(popupKeyListener);
            popupKeyListener = null;
        }
    }

    protected void installKeyboardActions() {
        comboBox.setKeySelectionManager(new DefaultKeySelectionManager());
        BasicComboBoxKeyboardActions.installKeyboardActions(comboBox);
    }

    protected void uninstallKeyboardActions() {
        comboBox.setKeySelectionManager(null);
        BasicComboBoxKeyboardActions.uninstallKeyboardActions(comboBox);
    }

    protected void installComponents() {
        arrowButton = createArrowButton();
        configureArrowButton();
        comboBox.add(arrowButton);

        comboBox.add(currentValuePane);

        if (comboBox.isEditable()) {
            addEditor();
        }
    }

    protected void uninstallComponents() {
        unconfigureArrowButton();
        if (comboBox.isEditable()) {
            removeEditor();
        }
        comboBox.removeAll();
        arrowButton = null;
    }


    protected Dimension getDefaultSize() {
        return comboBox.getRenderer().getListCellRendererComponent(listBox, null, 0, false, false).getPreferredSize();
    }

    protected Dimension getDisplaySize() {
        if (cachedDisplaySize != null) {
            return cachedDisplaySize;
        }

        Dimension result = null;
        if (comboBox.getPrototypeDisplayValue() != null) {
            result = comboBox.getRenderer().getListCellRendererComponent(listBox, comboBox.getPrototypeDisplayValue(), -1, false, false).getPreferredSize();
        } else {
            if (comboBox.getItemCount() > 0) {
                for (int i = 0; i < comboBox.getItemCount(); i++) {
                    Dimension itemPart = comboBox.getRenderer().getListCellRendererComponent(listBox, comboBox.getItemAt(i), i, false, false).getPreferredSize();
                    if (result == null) {
                        result = itemPart;
                    } else {
                        if (result.height < itemPart.height) {
                            result.height = itemPart.height;
                        }
                        if (result.width < itemPart.width) {
                            result.width = itemPart.width;
                        }
                    }
                }
            } else {
                result = getDefaultSize();
            }
            if (comboBox.isEditable()) {
                Dimension minEditableSize = result = comboBox.getRenderer().getListCellRendererComponent(listBox, PROTOTYPE_VALUE_FOR_EDITABLE_COMBOBOX, 0, false, false).getPreferredSize();
                if (result.width < minEditableSize.width) {
                    result = minEditableSize;
                }
            }
        }

        cachedDisplaySize = result;

        return result;
    }

    protected boolean isNavigationKey(final int keyCode) {
        return keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN;
    }

    protected void selectNextPossibleValue() {
        int selectedIndex = comboBox.getSelectedIndex();
        if (!isTableEditor && selectedIndex != -1 && selectedIndex + 1 < comboBox.getItemCount()) {
            comboBox.setSelectedIndex(selectedIndex + 1);
        }
    }

    protected void selectPreviousPossibleValue() {
        int selectedIndex = comboBox.getSelectedIndex();
        if (!isTableEditor && selectedIndex != -1 && selectedIndex > 0) {
            comboBox.setSelectedIndex(selectedIndex - 1);
        }
    }

    protected void toggleOpenClose() {
        setPopupVisible(comboBox, !isPopupVisible(comboBox));
    }

    protected Rectangle rectangleForCurrentValue() {
        return new Rectangle(0, 0, 0, 0);
    }

    protected Insets getInsets() {
        return comboBox.getInsets(cachedInsets);
    }

    protected void configureEditor() {
        editorFocusListener = new EditorFocusHandler();
        editor.addFocusListener(editorFocusListener);
        editor.setFont(comboBox.getFont());
        comboBox.getEditor().setItem(comboBox.getSelectedItem());
        
        editorActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (selectedValue != null && !selectedValue.equals(popup.getList().getSelectedValue())) {
                    comboBox.getEditor().setItem(popup.getList().getSelectedValue());
                }
            }
        };
        comboBox.getEditor().addActionListener(editorActionListener);
    }

    protected void unconfigureEditor() {
        editor.removeFocusListener(editorFocusListener);
        if (comboBox.getEditor() != null) {
            comboBox.getEditor().removeActionListener(editorActionListener);
        }
        editorFocusListener = null;
    }

    protected JButton createArrowButton() {
        return new BasicArrowButton(BasicArrowButton.SOUTH,
                                              UIManager.getColor("ComboBox.buttonBackground"),
                                              UIManager.getColor("ComboBox.buttonShadow"),
                                              UIManager.getColor("ComboBox.buttonDarkShadow"),
                                              UIManager.getColor("ComboBox.buttonHighlight"));
    }


    protected ComboPopup createPopup() {
        return new BasicComboPopup(comboBox);
    }

    protected KeyListener createKeyListener() {
        return new KeyHandler();
    }

    protected FocusListener createFocusListener() {
        return new FocusHandler();
    }

    protected ListDataListener createListDataListener() {
        return new ListDataHandler();
    }

    protected ItemListener createItemListener() {
        return null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected LayoutManager createLayoutManager() {
        return new ComboBoxLayoutManager();
    }

    protected ListCellRenderer createRenderer() {
        return new BasicComboBoxRenderer.UIResource();
    }

    protected ComboBoxEditor createEditor() {
        return new BasicComboBoxEditor.UIResource();
    }
}
