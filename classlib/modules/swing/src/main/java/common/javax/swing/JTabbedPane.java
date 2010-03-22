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

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.UIResource;
import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JTabbedPane</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JTabbedPane extends JComponent implements Serializable, Accessible, SwingConstants {
    private static final long serialVersionUID = 1671634173365704280L;
    
    private static final int NOT_FOUND = -1;
    
    // TODO: implement
    protected class AccessibleJTabbedPane extends AccessibleJComponent implements
            AccessibleSelection, ChangeListener {
        private static final long serialVersionUID = 8645220594633986096L;

        public AccessibleJTabbedPane() {
        }

        public void addAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void clearAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleAt(Point p) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleChild(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public int getAccessibleChildrenCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleRole getAccessibleRole() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleSelection getAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleSelectionCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean isAccessibleChildSelected(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void removeAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void selectAllAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void stateChanged(ChangeEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    protected class ModelListener implements ChangeListener, Serializable {
        private static final long serialVersionUID = 1L;

        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    }

    private class JTabInfo extends AbstractButton {
        private static final long serialVersionUID = 1L;

        private Component comp;

        private boolean enabled = true;

        private Icon disabledIcon;

        public JTabInfo(String title, Icon icon, Component comp, String tip) {
            setModel(new DefaultButtonModel());
            setText(title);
            setIcon(icon);
            setComp(comp);
            setToolTipText(tip);
            setBackground(JTabbedPane.this.getBackground());
            setForeground(JTabbedPane.this.getForeground());
            setMnemonic(-1);
        }

        public void setComp(Component comp) {
            this.comp = comp;
        }

        public Component getComp() {
            return comp;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void setDisabledIcon(Icon icon) {
            disabledIcon = icon;
        }

        @Override
        public Icon getDisabledIcon() {
            return disabledIcon;
        }
    }

    public static final int SCROLL_TAB_LAYOUT = 1;

    public static final int WRAP_TAB_LAYOUT = 0;

    protected transient ChangeEvent changeEvent = new ChangeEvent(this);

    protected ChangeListener changeListener;

    protected SingleSelectionModel model;

    protected int tabPlacement = TOP;

    private int tabLayoutPolicy = WRAP_TAB_LAYOUT;

    private Vector<JTabInfo> tabInfos = new Vector<JTabInfo>(2);

    public JTabbedPane() {
        this(TOP, WRAP_TAB_LAYOUT);
    }

    public JTabbedPane(int tabPlacement) {
        this(tabPlacement, WRAP_TAB_LAYOUT);
    }

    public JTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        setModel(new DefaultSingleSelectionModel());
        setTabPlacement(tabPlacement);
        setTabLayoutPolicy(tabLayoutPolicy);
        ToolTipManager.sharedInstance().registerComponent(this);
        updateUI();
    }

    /**
     * If <code>component</code> is <code>instanceof UIResource</code>,
     * it is added to the tabbed pane without creating a new tab. It can be
     * used in UI's to add custom components.
     */
    @Override
    public Component add(Component component) {
        if (component instanceof UIResource) {
            addImpl(component, null, -1);
        } else {
            insertTab(component.getName(), null, component, null, getTabCount());
        }
        return component;
    }

    @Override
    public Component add(Component component, int index) {
        insertTab(component.getName(), null, component, null, index);
        return component;
    }

    @Override
    public void add(Component component, Object constraints) {
        add(component, constraints, getTabCount());
    }

    @Override
    public void add(Component component, Object constraints, int index) {
        Icon icon = null;
        String title = null;
        if (constraints instanceof Icon) {
            icon = (Icon) constraints;
        } else if (constraints instanceof String) {
            title = (String) constraints;
        } else {
            title = component.getName();
        }
        insertTab(title, icon, component, null, index);
    }

    @Override
    public Component add(String title, Component component) {
        insertTab(title, null, component, null, getTabCount());
        return component;
    }

    public void addTab(String title, Component component) {
        insertTab(title, null, component, null, getTabCount());
    }

    public void addTab(String title, Icon icon, Component component) {
        insertTab(title, icon, component, null, getTabCount());
    }

    public void addTab(String title, Icon icon, Component component, String tip) {
        insertTab(title, icon, component, tip, getTabCount());
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    protected ChangeListener createChangeListener() {
        return new ModelListener();
    }

    protected void fireStateChanged() {
        ChangeListener[] listeners = getChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].stateChanged(changeEvent);
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return accessibleContext == null ? (accessibleContext = new AccessibleJTabbedPane())
                : accessibleContext;
    }

    public Rectangle getBoundsAt(int index) {
        return getUI() != null ? getUI().getTabBounds(this, index) : null;
    }

    public void setComponentAt(int index, Component comp) {
        int oldIndex = indexOfComponent(comp);
        if (oldIndex == index) {
            return;
        }
        JTabInfo tabInfo = getTabAt(index);
        if (oldIndex != NOT_FOUND) {
            removeTabAt(oldIndex);
        }
        if (tabInfo.getComp() != comp) {
            removeComponentFromContainer(tabInfo.getComp());
            addComponentToContainer(comp);
        }
        tabInfo.setComp(comp);
    }

    public Component getComponentAt(int index) {
        return getTabAt(index).getComp();
    }

    public int getTabCount() {
        return tabInfos.size();
    }

    public int getTabRunCount() {
        return getUI() != null ? getUI().getTabRunCount(this) : 0;
    }

    public void setTitleAt(int index, String title) {
        getTabAt(index).setText(title);
        repaint();
    }

    public String getTitleAt(int index) {
        return getTabAt(index).getText();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int index = indexAtLocation(event.getX(), event.getY());
        return index > NOT_FOUND ? getToolTipTextAt(index) : super.getToolTipText(event);
    }

    public void setToolTipTextAt(int index, String toolTipText) {
        getTabAt(index).setToolTipText(toolTipText);
    }

    public String getToolTipTextAt(int index) {
        return getTabAt(index).getToolTipText();
    }

    public int indexAtLocation(int x, int y) {
        return getUI() != null ? getUI().tabForCoordinate(this, x, y) : NOT_FOUND;
    }

    public int indexOfComponent(Component comp) {
        for (int i = 0; i < tabInfos.size(); i++) {
            if (comp == getComponentAt(i)) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    public int indexOfTab(Icon icon) {
        for (int i = 0; i < tabInfos.size(); i++) {
            if (icon == getIconAt(i)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfTab(String title) {
        for (int i = 0; i < tabInfos.size(); i++) {
            if (title == getTitleAt(i)) {
                return i;
            }
        }
        return -1;
    }

    public void insertTab(String title, Icon icon, Component comp, String tip, int index) {
        int oldIndex = comp != null ? indexOfComponent(comp) : NOT_FOUND;
        if (oldIndex != NOT_FOUND) {
            tabInfos.remove(oldIndex);
        }
        String realTitle = title == null ? "" : title;
        JTabInfo tabInfo = new JTabInfo(realTitle, icon, comp, tip);
        if (index == -1) {
            index = tabInfos.size();
        }
        tabInfos.add(index, tabInfo);
        if (oldIndex == NOT_FOUND) {
            addComponentToContainer(comp);
        }
        if (getTabCount() == 1) {
            setSelectedIndex(0);
        } else if (index <= getSelectedIndex()
                && (oldIndex == NOT_FOUND || oldIndex > getSelectedIndex())) {
            setSelectedIndex(getSelectedIndex() + 1);
        }
        repaint();
    }

    @Override
    protected String paramString() {
        return super.paramString();
    }

    /**
     * If <code>component</code> is <code>instanceof UIResource</code>,
     * it is removed without removing any tab.
     */
    @Override
    public void remove(Component comp) {
        if (comp instanceof UIResource) {
            removeComponentFromContainer(comp);
            return;
        }
        int index = indexOfComponent(comp);
        if (index != NOT_FOUND) {
            removeTabAt(index);
        }
    }

    /**
     * If <code>component</code> is <code>instanceof UIResource</code>,
     * it is removed without removing any tab.
     */
    @Override
    public void remove(int index) {
        if (getComponent(index) instanceof UIResource) {
            super.remove(index);
            return;
        }
        removeTabAt(index);
    }

    public void removeTabAt(int index) {
        Component comp = getComponentAt(index);
        if (getSelectedIndex() >= getTabCount() - 1) {
            setSelectedIndex(getTabCount() - 2);
        }
        tabInfos.remove(index);
        removeComponentFromContainer(comp);
        comp.setVisible(true);
        repaint();
    }

    @Override
    public void removeAll() {
        super.removeAll();
        tabInfos.removeAllElements();
    }

    public void setBackgroundAt(int index, Color background) {
        Color realBackground = background == null ? getBackground() : background;
        getTabAt(index).setBackground(realBackground);
        repaint();
    }

    public Color getBackgroundAt(int index) {
        return getTabAt(index).getBackground();
    }

    public void setForegroundAt(int index, Color foreground) {
        Color realForeground = foreground == null ? getForeground() : foreground;
        getTabAt(index).setForeground(realForeground);
        repaint();
    }

    public Color getForegroundAt(int index) {
        return getTabAt(index).getForeground();
    }

    public void setDisabledIconAt(int index, Icon disabledIcon) {
        getTabAt(index).setDisabledIcon(disabledIcon);
    }

    public Icon getDisabledIconAt(int index) {
        return getTabAt(index).getDisabledIcon();
    }

    public void setDisplayedMnemonicIndexAt(int tabIndex, int mnemonicIndex) {
        getTabAt(tabIndex).setDisplayedMnemonicIndex(mnemonicIndex);
    }

    public int getDisplayedMnemonicIndexAt(int index) {
        return getTabAt(index).getDisplayedMnemonicIndex();
    }

    public void setEnabledAt(int index, boolean enabled) {
        getTabAt(index).setEnabled(enabled);
        repaint();
    }

    public boolean isEnabledAt(int index) {
        return getTabAt(index).isEnabled();
    }

    public void setIconAt(int index, Icon icon) {
        getTabAt(index).setIcon(icon);
        repaint();
    }

    public Icon getIconAt(int index) {
        return getTabAt(index).getIcon();
    }

    public void setMnemonicAt(int tabIndex, int mnemonic) {
        int oldValue = getMnemonicAt(tabIndex);
        if (oldValue == mnemonic) {
            return;
        }
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW, true);
        inputMap.remove(KeyStroke.getKeyStroke(oldValue, InputEvent.ALT_DOWN_MASK));
        inputMap.put(KeyStroke.getKeyStroke(mnemonic, InputEvent.ALT_DOWN_MASK),
                StringConstants.MNEMONIC_ACTION);
        getTabAt(tabIndex).setMnemonic(mnemonic);
        repaint();
    }

    public int getMnemonicAt(int index) {
        return getTabAt(index).getMnemonic();
    }

    public void setModel(SingleSelectionModel model) {
        if (changeListener == null) {
            changeListener = createChangeListener();
        }
        SingleSelectionModel oldValue = this.model;
        if (oldValue != null) {
            oldValue.removeChangeListener(changeListener);
        }
        this.model = model;
        if (model != null) {
            model.addChangeListener(changeListener);
        }
        firePropertyChange("model", oldValue, model);
    }

    public SingleSelectionModel getModel() {
        return model;
    }

    public void setSelectedComponent(Component comp) {
        int index = indexOfComponent(comp);
        if (index == NOT_FOUND) {
            throw new IllegalArgumentException(Messages.getString("swing.34")); //$NON-NLS-1$
        }
        setSelectedIndex(index);
    }

    public Component getSelectedComponent() {
        return getModel().isSelected() ? getComponentAt(getSelectedIndex()) : null;
    }

    public void setSelectedIndex(int index) {
        if (index < -1 || index >= getTabCount()) {
            throw new IndexOutOfBoundsException(Messages.getString("swing.35")); //$NON-NLS-1$
        }
        getModel().setSelectedIndex(index);
    }

    public int getSelectedIndex() {
        return getModel().getSelectedIndex();
    }

    public void setTabLayoutPolicy(int tabLayoutPolicy) {
        if (tabLayoutPolicy != WRAP_TAB_LAYOUT && tabLayoutPolicy != SCROLL_TAB_LAYOUT) {
            throw new IllegalArgumentException(Messages.getString("swing.36")); //$NON-NLS-1$
        }
        int oldValue = this.tabLayoutPolicy;
        this.tabLayoutPolicy = tabLayoutPolicy;
        firePropertyChange("tabLayoutPolicy", oldValue, tabLayoutPolicy);
    }

    public int getTabLayoutPolicy() {
        return tabLayoutPolicy;
    }

    public void setTabPlacement(int tabPlacement) {
        if (tabPlacement != TOP && tabPlacement != BOTTOM && tabPlacement != LEFT
                && tabPlacement != RIGHT) {
            throw new IllegalArgumentException(Messages.getString("swing.37")); //$NON-NLS-1$
        }
        int oldValue = this.tabPlacement;
        this.tabPlacement = tabPlacement;
        firePropertyChange("tabPlacement", oldValue, tabPlacement);
    }

    public int getTabPlacement() {
        return tabPlacement;
    }

    public void setUI(TabbedPaneUI ui) {
        super.setUI(ui);
    }

    public TabbedPaneUI getUI() {
        return (TabbedPaneUI) ui;
    }

    @Override
    public String getUIClassID() {
        return "TabbedPaneUI";
    }

    @Override
    public void updateUI() {
        setUI((TabbedPaneUI) UIManager.getUI(this));
    }

    private JTabInfo getTabAt(int index) {
        return tabInfos.get(index);
    }

    private void addComponentToContainer(Component comp) {
        if (comp != null) {
            comp.setVisible(false);
            if (getComponentZOrder(comp) == -1) {
                addImpl(comp, null, -1);
            }
        }
    }

    private void removeComponentFromContainer(Component comp) {
        int componentIndex = getComponentZOrder(comp);
        if (componentIndex != -1) {
            super.remove(componentIndex);
        }
    }
}
