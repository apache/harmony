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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleStateSet;
import javax.swing.plaf.MenuBarUI;
import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JMenuBar</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JMenuBar extends JComponent implements Accessible, MenuElement {
    private static final long serialVersionUID = 6620404810314292434L;

    // TODO: implement
    protected class AccessibleJMenuBar extends AccessibleJComponent implements
            AccessibleSelection {
        private static final long serialVersionUID = 1L;

        public void addAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void clearAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleSelection(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleSelectionCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.MENU_BAR;
        }

        @Override
        public AccessibleSelection getAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() throws NotImplementedException {
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
    }

    private final static String UI_CLASS_ID = "MenuBarUI";

    private SingleSelectionModel selectionModel = new DefaultSingleSelectionModel();

    private boolean borderPainted = true;

    private Insets margin;

    public JMenuBar() {
        updateUI();
    }

    public JMenu add(JMenu menu) {
        super.add(menu);
        return menu;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleJMenuBar())
                : accessibleContext;
    }

    public JMenu getMenu(int index) {        
        Component c = getComponent(index);
        return (c instanceof JMenu) ? (JMenu) c : null;
    }

    public int getMenuCount() {
        return getComponentCount();
    }

    @Deprecated
    public Component getComponentAtIndex(int index) {
        return getComponent(index);
    }

    public int getComponentIndex(Component c) {
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) == c) {
                return i;
            }
        }
        return -1;
    }

    public MenuElement[] getSubElements() {
        return Utilities.getSubElements(this);
    }

    public void setSelectionModel(SingleSelectionModel model) {
        SingleSelectionModel oldValue = selectionModel;
        selectionModel = model;
        firePropertyChange(StringConstants.SELECTION_MODEL_PROPERTY, oldValue, selectionModel);
    }

    public SingleSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelected(Component selection) {
        if (selectionModel != null) {
            selectionModel.setSelectedIndex(getComponentIndex(selection));
        }
    }

    public boolean isSelected() {
        return (selectionModel != null) ? selectionModel.isSelected() : false;
    }

    public void setBorderPainted(boolean painted) {
        boolean oldValue = borderPainted;
        borderPainted = painted;
        firePropertyChange(AbstractButton.BORDER_PAINTED_CHANGED_PROPERTY, oldValue,
                borderPainted);
    }

    public boolean isBorderPainted() {
        return borderPainted;
    }

    @Override
    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent event, int condition,
            boolean pressed) {
        MenuSelectionManager.defaultManager().processKeyEvent(event);
        if ((event != null) && event.isConsumed()) {
            return true;
        }
        if (super.processKeyBinding(ks, event, condition, pressed)) {
            return true;
        }
        return SwingUtilities.processKeyEventOnChildren(this, event);
    }

    public void processKeyEvent(KeyEvent e, MenuElement[] path, MenuSelectionManager manager) {
    }

    public void processMouseEvent(MouseEvent event, MenuElement[] path,
            MenuSelectionManager manager) {
    }

    public void menuSelectionChanged(boolean isIncluded) {
    }

    public Component getComponent() {
        return this;
    }

    public void setHelpMenu(JMenu menu) {
        throw new Error(Messages.getString("swing.err.0C", "setHelpMenu()")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public JMenu getHelpMenu() {
        throw new Error(Messages.getString("swing.err.0C", "getHelpMenu()")); //$NON-NLS-1$ //$NON-NLS-2$ 
    }

    public void setMargin(Insets margin) {
        Insets oldValue = this.margin;
        this.margin = margin;
        firePropertyChange(AbstractButton.MARGIN_CHANGED_PROPERTY, oldValue, margin);
    }

    public Insets getMargin() {
        return margin;
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public MenuBarUI getUI() {
        return (MenuBarUI) ui;
    }

    public void setUI(MenuBarUI ui) {
        super.setUI(ui);
    }

    @Override
    public void updateUI() {
        setUI((MenuBarUI) UIManager.getUI(this));
    }
}
