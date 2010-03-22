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
 * @author Vadim L. Bogdanov, Anton Avtamonov
 */
package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;
import javax.swing.plaf.ToolBarUI;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class JToolBar extends JComponent implements SwingConstants, Accessible {
    private static final Insets DEFAULT_MARGIN = new Insets(0, 0, 0, 0);

    // TODO: implement accessibility
    protected class AccessibleJToolBar extends AccessibleJComponent {
        public AccessibleStateSet getAccessibleStateSet() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }

        public AccessibleRole getAccessibleRole() {
            throw new UnsupportedOperationException(Messages.getString("swing.27")); //$NON-NLS-1$
        }
    }

    public static class Separator extends JSeparator {
        private Dimension separatorSize;

        public Separator() {
            setFocusable(false);
        }

        public Separator(final Dimension size) {
            setFocusable(false);
            setSeparatorSize(size);
        }

        public String getUIClassID() {
            return "ToolBarSeparatorUI";
        }

        public void setSeparatorSize(final Dimension size) {
            if (size == null) {
                return;
            }
            separatorSize = size;
        }

        public Dimension getSeparatorSize() {
            return separatorSize;
        }

        public Dimension getMinimumSize() {
            return new Dimension(getSeparatorSize());
        }

        public Dimension getMaximumSize() {
            return new Dimension(getSeparatorSize());
        }

        public Dimension getPreferredSize() {
            return new Dimension(getSeparatorSize());
        }
    }

    private class DefaultLayout extends BoxLayout {
        public DefaultLayout() {
            super(JToolBar.this, LINE_AXIS);
            updateAxis();
        }

        public void updateAxis() {
            if (getOrientation() == HORIZONTAL) {
                setAxis(LINE_AXIS);
            } else {
                setAxis(PAGE_AXIS);
            }
        }
    }

    private boolean borderPainted = true;
    private boolean floatable = true;
    private Insets margin = (Insets)DEFAULT_MARGIN.clone();
    private int orientation;
    private boolean rollover;

    public JToolBar() {
        this(null, HORIZONTAL);
    }

    public JToolBar(final int orientation) {
        this(null, orientation);
    }

    public JToolBar(final String name) {
        this(name, HORIZONTAL);
    }

    public JToolBar(final String name, final int orientation) {
        setName(name);

        setOrientation(orientation);
        setLayout(new DefaultLayout());

        updateUI();
    }

    public void setUI(final ToolBarUI ui) {
        super.setUI(ui);
    }

    public ToolBarUI getUI() {
        return (ToolBarUI)ui;
    }

    public void updateUI() {
        setUI((ToolBarUI)UIManager.getUI(this));
    }

    public String getUIClassID() {
        return "ToolBarUI";
    }

    public int getComponentIndex(final Component c) {
        return getComponentZOrder(c);
    }

    public Component getComponentAtIndex(final int i) {
        if (i >= 0 && i < getComponentCount()) {
            return getComponent(i);
        } else {
            return null;
        }
    }

    public void setMargin(final Insets m) {
        Insets oldValue = margin;
        margin = m != null ? m : (Insets)DEFAULT_MARGIN.clone();
        firePropertyChange("margin", oldValue, margin);
    }

    public Insets getMargin() {
        return margin;
    }

    public void setBorderPainted(final boolean b) {
        boolean oldValue = borderPainted;
        borderPainted = b;
        firePropertyChange("borderPainted", oldValue, borderPainted);
    }

    public boolean isBorderPainted() {
        return borderPainted;
    }

    public void setFloatable(final boolean b) {
        boolean oldValue = floatable;
        floatable = b;
        firePropertyChange("floatable", oldValue, floatable);
    }

    public boolean isFloatable() {
        return floatable;
    }

    public void setOrientation(final int o) {
        checkOrientation(o);
        int oldValue = orientation;
        orientation = o;
        if (getLayout() instanceof DefaultLayout) {
            ((DefaultLayout)getLayout()).updateAxis();
        }
        final int numConponents = getComponentCount();
        for (int i = 0; i < numConponents; i++) {
            if (getComponent(i) instanceof JSeparator) {
                ((JSeparator)getComponent(i)).setOrientation(getSeparatorOrientation());
            }
        }
        firePropertyChange("orientation", oldValue, orientation);
    }

    public int getOrientation() {
        return orientation;
    }

    public void setRollover(final boolean rollover) {
        boolean oldValue = this.rollover;
        this.rollover = rollover;
        firePropertyChange("JToolBar.isRollover", oldValue, this.rollover);
    }

    public boolean isRollover() {
        return rollover;
    }

    public void addSeparator() {
        add(createSeparator());
    }

    public void addSeparator(final Dimension size) {
        Separator separator = createSeparator();
        separator.setSeparatorSize(size);
        add(separator);
    }

    public JButton add(final Action a) {
        JButton b = createActionComponent(a);
        b.setAction(a);
        PropertyChangeListener actionListener = createActionChangeListener(b);
        if (actionListener != null) {
            b.addPropertyChangeListener(actionListener);
        }
        add(b);
        return b;
    }

    public void setLayout(final LayoutManager mgr) {
        super.setLayout(mgr);
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJToolBar();
        }

        return accessibleContext;
    }

    protected void paintBorder(final Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    protected JButton createActionComponent(final Action a) {
        JButton b = new JButton();
        if (a != null) {
            b.configurePropertiesFromAction(a);
        }
        return b;
    }

    protected PropertyChangeListener createActionChangeListener(final JButton b) {
        return null;
    }

    protected void addImpl(final Component comp,
                           final Object constraints, final int index) {
        super.addImpl(comp, constraints, index);
    }

    private void checkOrientation(final int o) {
        if (o != HORIZONTAL && o != VERTICAL) {
            throw new IllegalArgumentException(Messages.getString("swing.47")); //$NON-NLS-1$
        }
    }

    private Separator createSeparator() {
        Separator separator = new Separator();
        separator.setOrientation(getSeparatorOrientation());
        return separator;
    }

    private int getSeparatorOrientation() {
        return (orientation == HORIZONTAL) ? VERTICAL : HORIZONTAL;
    }
}
