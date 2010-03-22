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

package javax.swing;

import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleStateSet;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComboBoxUI;
import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.internal.nls.Messages;
import org.apache.harmony.x.swing.StringConstants;

/**
 * <p>
 * <i>JComboBox</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JComboBox extends JComponent implements ItemSelectable, ListDataListener,
        ActionListener, Accessible {
    private static final long serialVersionUID = 4884562788864849284L;

    protected class AccessibleJComboBox extends AccessibleJComponent implements
            AccessibleAction, AccessibleSelection {
        private static final long serialVersionUID = 1L;

        public AccessibleJComboBox() {
        }

        @Override
        public int getAccessibleChildrenCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleChild(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.COMBO_BOX;
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleAction getAccessibleAction() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public String getAccessibleActionDescription(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleActionCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean doAccessibleAction(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleSelection getAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleSelectionCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean isAccessibleChildSelected(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void addAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void removeAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void clearAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void selectAllAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    public static interface KeySelectionManager {
        int selectionForKey(char key, ComboBoxModel model);
    }

    private class DefaultKeySelectionManager implements KeySelectionManager {
        public int selectionForKey(char keyChar, ComboBoxModel model) {
            int selectedIndex = getIndex(model.getSelectedItem(), model);
            for (int i = selectedIndex + 1; i < model.getSize(); i++) {
                String item = model.getElementAt(i).toString();
                if (itemStartsWith(item, keyChar)) {
                    return i;
                }
            }
            for (int i = 0; i <= selectedIndex; i++) {
                String item = model.getElementAt(i).toString();
                if (itemStartsWith(item, keyChar)) {
                    return i;
                }
            }
            return -1;
        }

        private boolean itemStartsWith(String item, char keyChar) {
            return Character.toUpperCase(keyChar) == Character.toUpperCase(item.charAt(0));
        }
    }

    private class ActionPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            Action action = (Action) event.getSource();
            if (action != null) {
                String propertyName = event.getPropertyName();
                if (Action.SHORT_DESCRIPTION.equals(propertyName)) {
                    setToolTipText((String) event.getNewValue());
                } else if (StringConstants.ENABLED_PROPERTY_CHANGED.equals(propertyName)) {
                    setEnabled(((Boolean) event.getNewValue()).booleanValue());
                } else if (Action.ACTION_COMMAND_KEY.equals(propertyName)) {
                    setActionCommand((String) action.getValue(Action.ACTION_COMMAND_KEY));
                }
            }
        }
    }

    private static final String UI_CLASS_ID = "ComboBoxUI";

    private static final String MAXIMUM_ROW_COUNT_PROPERTY_CHANGED = "maximumRowCount";

    private static final String PROTOTYPE_DISPLAY_VALUE_PROPERTY_CHANGED = "prototypeDisplayValue";

    protected String actionCommand = "comboBoxChanged";

    protected ComboBoxModel dataModel;

    protected ComboBoxEditor editor;

    protected boolean isEditable;

    protected KeySelectionManager keySelectionManager;

    protected boolean lightWeightPopupEnabled = true;

    protected int maximumRowCount = 8;

    protected ListCellRenderer renderer;

    protected Object selectedItemReminder;

    private Object prototypeDisplayValue;

    private Action action;

    private PropertyChangeListener actionPropertyChangeListener;

    public JComboBox() {
        this(new DefaultComboBoxModel());
    }

    public JComboBox(Object[] items) {
        this(new DefaultComboBoxModel(items));
    }

    public JComboBox(Vector<?> items) {
        this(new DefaultComboBoxModel(items));
    }

    public JComboBox(ComboBoxModel model) {
        dataModel = model;
        dataModel.addListDataListener(this);
        installAncestorListener();
        updateUI();
    }

    public void setUI(ComboBoxUI ui) {
        super.setUI(ui);
    }

    @Override
    public void updateUI() {
        setUI((ComboBoxUI) UIManager.getUI(this));
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public ComboBoxUI getUI() {
        return (ComboBoxUI) ui;
    }

    public void setModel(ComboBoxModel model) {
        if (dataModel != model) {
            ComboBoxModel oldModel = dataModel;
            if (oldModel != null) {
                oldModel.removeListDataListener(this);
            }
            dataModel = model;
            dataModel.addListDataListener(this);
            firePropertyChange(StringConstants.MODEL_PROPERTY_CHANGED, oldModel, model);
        }
    }

    public ComboBoxModel getModel() {
        return dataModel;
    }

    public void setLightWeightPopupEnabled(boolean isEnabled) {
        if (lightWeightPopupEnabled != isEnabled) {
            lightWeightPopupEnabled = isEnabled;
            firePropertyChange(StringConstants.LIGHTWEIGHT_POPUP_ENABLED_PROPERTY_CHANGED,
                    !isEnabled, isEnabled);
        }
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    public void setEditable(boolean isEditable) {
        if (this.isEditable != isEditable) {
            this.isEditable = isEditable;
            firePropertyChange(StringConstants.EDITABLE_PROPERTY_CHANGED, !isEditable,
                    isEditable);
        }
    }

    public boolean isEditable() {
        return isEditable;
    }

    public void setMaximumRowCount(int count) {
        LookAndFeel.markPropertyNotInstallable(this, "maximumRowCount");
        if (maximumRowCount != count) {
            int oldValue = maximumRowCount;
            maximumRowCount = count;
            firePropertyChange(MAXIMUM_ROW_COUNT_PROPERTY_CHANGED, oldValue, count);
        }
    }

    public int getMaximumRowCount() {
        return maximumRowCount;
    }

    public void setRenderer(ListCellRenderer renderer) {
        if (this.renderer != renderer) {
            ListCellRenderer oldValue = this.renderer;
            this.renderer = renderer;
            firePropertyChange(StringConstants.RENDERER_PROPERTY_CHANGED, oldValue, renderer);
        }
    }

    public ListCellRenderer getRenderer() {
        return renderer;
    }

    public void setEditor(ComboBoxEditor editor) {
        if (this.editor != editor) {
            ComboBoxEditor oldValue = this.editor;
            if (oldValue != null) {
                oldValue.removeActionListener(this);
            }
            this.editor = editor;
            if (this.editor != null) {
                this.editor.addActionListener(this);
            }
            firePropertyChange(StringConstants.EDITOR_PROPERTY_CHANGED, oldValue, editor);
        }
    }

    public ComboBoxEditor getEditor() {
        return editor;
    }

    public void setSelectedItem(Object element) {
        selectedItemReminder = dataModel.getSelectedItem();
        if (isEditable || getIndex(element) != -1 || element == null) {
            if (element != getSelectedItem() || element != null
                    && !element.equals(getSelectedItem())) {
                dataModel.setSelectedItem(element);
            } else if (isEditable && element != null && !element.equals(getEditor().getItem())) {
                getEditor().setItem(element);
            } else {
                // fire action event even if selection is not changed
                fireActionEvent();
            }
        }
    }

    public Object getSelectedItem() {
        return dataModel.getSelectedItem();
    }

    public void setSelectedIndex(int index) {
        if (index < -1 || index >= dataModel.getSize()) {
            throw new IllegalArgumentException(Messages.getString("swing.0C")); //$NON-NLS-1$
        }
        if (index == -1) {
            setSelectedItem(null);
        } else {
            setSelectedItem(dataModel.getElementAt(index));
        }
    }

    public Object[] getSelectedObjects() {
        if (getSelectedItem() != null) {
            return new Object[] { getSelectedItem() };
        }
        return new Object[0];
    }

    public int getSelectedIndex() {
        return getIndex(getSelectedItem());
    }

    public Object getPrototypeDisplayValue() {
        return prototypeDisplayValue;
    }

    public void setPrototypeDisplayValue(Object prototypeDisplayValue) {
        if (this.prototypeDisplayValue != prototypeDisplayValue) {
            Object oldValue = this.prototypeDisplayValue;
            this.prototypeDisplayValue = prototypeDisplayValue;
            firePropertyChange(PROTOTYPE_DISPLAY_VALUE_PROPERTY_CHANGED, oldValue,
                    prototypeDisplayValue);
        }
    }

    public void addItem(Object element) {
        selectedItemReminder = getSelectedItem();
        if (dataModel instanceof MutableComboBoxModel) {
            ((MutableComboBoxModel) dataModel).addElement(element);
        } else {
            throw new RuntimeException(Messages.getString("swing.0B")); //$NON-NLS-1$
        }
    }

    public void insertItemAt(Object element, int index) {
        selectedItemReminder = getSelectedItem();
        if (dataModel instanceof MutableComboBoxModel) {
            ((MutableComboBoxModel) dataModel).insertElementAt(element, index);
        } else {
            throw new RuntimeException(Messages.getString("swing.0B")); //$NON-NLS-1$
        }
    }

    public void removeItem(Object element) {
        selectedItemReminder = getSelectedItem();
        if (dataModel instanceof MutableComboBoxModel) {
            ((MutableComboBoxModel) dataModel).removeElement(element);
        } else {
            throw new RuntimeException(Messages.getString("swing.0B")); //$NON-NLS-1$
        }
    }

    public void removeItemAt(int index) {
        selectedItemReminder = getSelectedItem();
        if (dataModel instanceof MutableComboBoxModel) {
            ((MutableComboBoxModel) dataModel).removeElementAt(index);
        } else {
            throw new RuntimeException(Messages.getString("swing.0B")); //$NON-NLS-1$
        }
    }

    public void removeAllItems() {
        selectedItemReminder = getSelectedItem();
        if (dataModel instanceof MutableComboBoxModel) {
            MutableComboBoxModel model = (MutableComboBoxModel) dataModel;
            while (model.getSize() > 0) {
                model.removeElementAt(0);
            }
        } else {
            throw new RuntimeException(Messages.getString("swing.0B")); //$NON-NLS-1$
        }
    }

    public int getItemCount() {
        return dataModel.getSize();
    }

    public Object getItemAt(int index) {
        if (index < 0 || index >= dataModel.getSize()) {
            return null;
        }
        return dataModel.getElementAt(index);
    }

    public void addItemListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }

    public void removeItemListener(ItemListener l) {
        listenerList.remove(ItemListener.class, l);
    }

    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    public void addPopupMenuListener(PopupMenuListener l) {
        listenerList.add(PopupMenuListener.class, l);
    }

    public void removePopupMenuListener(PopupMenuListener l) {
        listenerList.remove(PopupMenuListener.class, l);
    }

    public PopupMenuListener[] getPopupMenuListeners() {
        return listenerList.getListeners(PopupMenuListener.class);
    }

    public void firePopupMenuWillBecomeVisible() {
        PopupMenuEvent event = new PopupMenuEvent(this);
        PopupMenuListener[] listeners = getPopupMenuListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].popupMenuWillBecomeVisible(event);
        }
    }

    public void firePopupMenuWillBecomeInvisible() {
        PopupMenuEvent event = new PopupMenuEvent(this);
        PopupMenuListener[] listeners = getPopupMenuListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].popupMenuWillBecomeInvisible(event);
        }
    }

    public void firePopupMenuCanceled() {
        PopupMenuEvent event = new PopupMenuEvent(this);
        PopupMenuListener[] listeners = getPopupMenuListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].popupMenuCanceled(event);
        }
    }

    public void setActionCommand(String command) {
        actionCommand = command;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public void showPopup() {
        setPopupVisible(true);
    }

    public void hidePopup() {
        setPopupVisible(false);
    }

    public void setPopupVisible(boolean isVisible) {
        getUI().setPopupVisible(this, isVisible);
    }

    public boolean isPopupVisible() {
        return getUI().isPopupVisible(this);
    }

    public void setAction(Action action) {
        if (this.action == action) {
            return;
        }
        Action oldValue = this.action;
        if (oldValue != null) {
            if (hasListener(ActionListener.class, oldValue)) {
                removeActionListener(oldValue);
            }
            if (actionPropertyChangeListener != null) {
                oldValue.removePropertyChangeListener(actionPropertyChangeListener);
            }
        }
        this.action = action;
        if (action != null) {
            if (!hasListener(ActionListener.class, action)) {
                addActionListener(action);
            }
            actionPropertyChangeListener = createActionPropertyChangeListener(this.action);
            this.action.addPropertyChangeListener(actionPropertyChangeListener);
        }
        firePropertyChange(StringConstants.ACTION_PROPERTY_CHANGED, oldValue, action);
        configurePropertiesFromAction(action);
    }

    public Action getAction() {
        return action;
    }

    protected void configurePropertiesFromAction(Action action) {
        if (action != null) {
            setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
            setEnabled(action.isEnabled());
            setActionCommand((String) action.getValue(Action.ACTION_COMMAND_KEY));
        } else {
            setActionCommand(null);
            setEnabled(true);
            setToolTipText(null);
        }
    }

    public void actionPerformed(ActionEvent e) {
        setSelectedItem(editor.getItem());
    }

    public void contentsChanged(ListDataEvent e) {
        selectedItemChanged();
        fireActionEvent();
    }

    public void intervalAdded(ListDataEvent e) {
    }

    public void intervalRemoved(ListDataEvent e) {
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        super.setEnabled(isEnabled);
        if (action != null) {
            action.setEnabled(isEnabled);
        }
    }

    public void configureEditor(ComboBoxEditor editor, Object item) {
        editor.setItem(item);
    }

    @Override
    public void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            hidePopup();
        }
        super.processKeyEvent(e);
    }

    public boolean selectWithKeyChar(char keyChar) {
        if (keySelectionManager == null) {
            keySelectionManager = createDefaultKeySelectionManager();
        }
        int index = keySelectionManager.selectionForKey(keyChar, getModel());
        if (index != -1) {
            setSelectedIndex(index);
            return true;
        }
        return false;
    }

    public void setKeySelectionManager(KeySelectionManager manager) {
        keySelectionManager = manager;
    }

    public KeySelectionManager getKeySelectionManager() {
        return keySelectionManager;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComboBox();
        }
        return accessibleContext;
    }

    protected void installAncestorListener() {
        addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent e) {
            }

            public void ancestorMoved(AncestorEvent e) {
            }

            public void ancestorRemoved(AncestorEvent e) {
                hidePopup();
            }
        });
    }

    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return new ActionPropertyChangeListener();
    }

    protected void fireItemStateChanged(ItemEvent e) {
        ItemListener[] listeners = getItemListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].itemStateChanged(e);
        }
    }

    protected void fireActionEvent() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand);
        ActionListener[] listeners = getActionListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].actionPerformed(event);
        }
    }

    protected void selectedItemChanged() {
        if (selectedItemReminder != null) {
            ItemEvent itemEvent = new ItemEvent(JComboBox.this, ItemEvent.ITEM_STATE_CHANGED,
                    selectedItemReminder, ItemEvent.DESELECTED);
            fireItemStateChanged(itemEvent);
        }
        Object newSelection = dataModel.getSelectedItem();
        if (isEditable || getIndex(newSelection) != -1) {
            ItemEvent itemEvent = new ItemEvent(JComboBox.this, ItemEvent.ITEM_STATE_CHANGED,
                    newSelection, ItemEvent.SELECTED);
            fireItemStateChanged(itemEvent);
        }
    }

    protected KeySelectionManager createDefaultKeySelectionManager() {
        return new DefaultKeySelectionManager();
    }

    private int getIndex(Object element) {
        return getIndex(element, dataModel);
    }

    private int getIndex(Object element, ComboBoxModel model) {
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
}
