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

package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleIcon;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ListUI;
import javax.swing.text.Position;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class JList extends JComponent implements Scrollable, Accessible {
    public static final int VERTICAL = 0;
    public static final int VERTICAL_WRAP = 1;
    public static final int HORIZONTAL_WRAP = 2;

    private static final String UI_CLASS_ID = "ListUI";

    private static final String CELL_RENDERER_CHANGED_PROPERTY = "cellRenderer";
    private static final String FIXED_CELL_HEIGHT_CHANGED_PROPERTY = "fixedCellHeight";
    private static final String FIXED_CELL_WIDTH_CHANGED_PROPERTY = "fixedCellWidth";
    private static final String MODEL_CHANGED_PROPERTY = "model";
    private static final String PROTOTYPE_CELL_VALUE_CHANGED_PROPERTY = "prototypeCellValue";
    private static final String SELECTION_BACKGROUND_CHANGED_PROPERTY = "selectionBackground";
    private static final String SELECTION_FOREGROUND_CHANGED_PROPERTY = "selectionForeground";
    private static final String VISIBLE_ROW_COUNT_CHANGED_PROPERTY = "visibleRowCount";
    private static final String LAYOUT_ORIENTATION_CHANGED_PROPERTY = "layoutOrientation";

    private ListModel model;
    private ListSelectionModel selectionModel;
    private ListCellRenderer cellRenderer;
    private EventListenerList selectionListeners = new EventListenerList();
    private ListSelectionEventPropagator selectionPropagator = new ListSelectionEventPropagator();
    private boolean dragEnabled;
    private int fixedCellHeight = -1;
    private int fixedCellWidth = -1;
    private int layoutOrientation = VERTICAL;
    private Object prototypeCellValue;
    private Color selectionBackground;
    private Color selectionForeground;
    private int visibleRowCount = 8;


    //TODO: implement
    protected class AccessibleJList extends AccessibleJComponent implements AccessibleSelection, PropertyChangeListener, ListSelectionListener, ListDataListener {
        protected class AccessibleJListChild extends AccessibleContext implements Accessible, AccessibleComponent {
            private final JList list;
            private final int indexInParent;


            private final EventListenerList listenerList = new EventListenerList();

            public AccessibleJListChild(final JList list, final int indexInParent) {
                this.list = list;
                this.indexInParent = indexInParent;
            }

            public void addFocusListener(final FocusListener l) {
                listenerList.add(FocusListener.class, l);
            }

            public void removeFocusListener(final FocusListener l) {
                listenerList.remove(FocusListener.class, l);
            }

            public void addPropertyChangeListener(final PropertyChangeListener l) {
                super.addPropertyChangeListener(l);
            }

            public void removePropertyChangeListener(final PropertyChangeListener l) {
                super.removePropertyChangeListener(l);
            }

            public boolean contains(final Point p) {
                return list.getCellBounds(indexInParent, indexInParent).contains(p);
            }

            public AccessibleAction getAccessibleAction() {
                return super.getAccessibleAction();
            }

            public Accessible getAccessibleAt(final Point p) {
                return null;
            }

            public Accessible getAccessibleChild(final int i) {
                return null;
            }

            public int getAccessibleChildrenCount() {
                return 0;
            }

            public AccessibleComponent getAccessibleComponent() {
                return this;
            }

            public AccessibleContext getAccessibleContext() {
                return this;
            }

            public String getAccessibleDescription() {
                return super.getAccessibleDescription();
            }

            public void setAccessibleDescription(final String description) {
                super.setAccessibleDescription(description);
            }

            public AccessibleIcon[] getAccessibleIcon() {
                return new AccessibleIcon[0];
            }

            public int getAccessibleIndexInParent() {
                return indexInParent;
            }

            public String getAccessibleName() {
                return super.getAccessibleName();
            }

            public void setAccessibleName(final String name) {
                super.setAccessibleName(name);
            }

            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.LIST_ITEM;
            }

            public AccessibleSelection getAccessibleSelection() {
                return null;
            }

            public AccessibleStateSet getAccessibleStateSet() {
                return new AccessibleStateSet();
            }

            public AccessibleText getAccessibleText() {
                return null;
            }

            public AccessibleValue getAccessibleValue() {
                return null;
            }

            public Color getBackground() {
                return getRenderingComponent().getBackground();
            }

            public void setBackground(final Color c) {

            }

            public Rectangle getBounds() {
                return list.getCellBounds(indexInParent, indexInParent);
            }

            public void setBounds(final Rectangle r) {

            }

            public Cursor getCursor() {
                return getRenderingComponent().getCursor();
            }

            public void setCursor(final Cursor c) {

            }

            public void setEnabled(final boolean b) {

            }

            public Font getFont() {
                return getRenderingComponent().getFont();
            }

            public void setFont(final Font f) {

            }

            public FontMetrics getFontMetrics(final Font f) {
                return getRenderingComponent().getFontMetrics(f);
            }

            public Color getForeground() {
                return getRenderingComponent().getForeground();
            }

            public void setForeground(final Color c) {

            }

            public Locale getLocale() {
                return list.getLocale();
            }

            public Point getLocation() {
                return ((JComponent)getRenderingComponent()).getLocation();
            }

            public void setLocation(final Point p) {

            }

            public Point getLocationOnScreen() {
                return ((JComponent)getRenderingComponent()).getLocationOnScreen();
            }

            public Dimension getSize() {
                return getRenderingComponent().getSize();
            }

            public void setSize(final Dimension d) {

            }

            public boolean isEnabled() {
                return list.isEnabled();
            }

            public boolean isFocusTraversable() {
                return true;
            }

            public boolean isShowing() {
                return list.isShowing();
            }

            public void requestFocus() {
            }

            public boolean isVisible() {
                return list.isVisible();
            }

            public void setVisible(final boolean b) {

            }



            private Component getRenderingComponent() {
                if(list.getCellRenderer()==null)
                    return null;
                return list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(indexInParent), indexInParent, false, false);
            }
        }

        public AccessibleJList() {
        }

        public void contentsChanged(final ListDataEvent e) {

        }

        public void intervalAdded(final ListDataEvent e) {

        }

        public void intervalRemoved(final ListDataEvent e) {

        }

        public Accessible getAccessibleAt(final Point p) {
            return JList.this;
        }

        public Accessible getAccessibleChild(final int i) {
            return (Accessible)getComponent(i);
        }

        public int getAccessibleChildrenCount() {
            return getComponentCount();
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LIST;
        }

        public void addAccessibleSelection(final int i) {
            getAccessibleSelection().addAccessibleSelection(i);
        }

        public void clearAccessibleSelection() {
            getAccessibleSelection().clearAccessibleSelection();
        }

        public AccessibleSelection getAccessibleSelection() {
            return null;
        }

        public Accessible getAccessibleSelection(final int i) {
            return getAccessibleSelection().getAccessibleSelection(i);
        }

        public int getAccessibleSelectionCount() {
            return getAccessibleSelection().getAccessibleSelectionCount();
        }

        public boolean isAccessibleChildSelected(final int i) {
            return getAccessibleSelection().isAccessibleChildSelected(i);
        }

        public void removeAccessibleSelection(final int i) {
            getAccessibleSelection().removeAccessibleSelection(i);
        }

        public void selectAllAccessibleSelection() {
            getAccessibleSelection().selectAllAccessibleSelection();
        }


        public AccessibleStateSet getAccessibleStateSet() {
            return null;
        }

        public void propertyChange(final PropertyChangeEvent e) {

        }

        public void valueChanged(final ListSelectionEvent e) {

        }
    }


    public JList() {
        this(new DefaultListModel());
    }

    public JList(final Object[] listData) {
        this(new DefaultListModel());
        putListData((DefaultListModel)getModel(), listData);
    }

    public JList(final Vector<?> listData) {
        this(new DefaultListModel());
        putListData((DefaultListModel)getModel(), listData);
    }

    public JList(final ListModel model) {
        if (model == null) {
            throw new IllegalArgumentException(Messages.getString("swing.15")); //$NON-NLS-1$
        }
        this.model = model;
        this.selectionModel = createSelectionModel();
        selectionModel.addListSelectionListener(selectionPropagator);
        ToolTipManager.sharedInstance().registerComponent(this);

        updateUI();
    }

    public void setModel(final ListModel model) {
        if (model == null) {
            throw new IllegalArgumentException(Messages.getString("swing.16")); //$NON-NLS-1$
        }

        if (this.model != model) {
            selectionModel.clearSelection();
            ListModel oldValue = this.model;
            this.model = model;
            firePropertyChange(MODEL_CHANGED_PROPERTY, oldValue, model);
        }
    }

    public ListModel getModel() {
        return model;
    }

    public void setSelectionModel(final ListSelectionModel selectionModel) {
        if (selectionModel == null) {
            throw new IllegalArgumentException(Messages.getString("swing.17")); //$NON-NLS-1$
        }

        if (this.selectionModel != selectionModel) {
            ListSelectionModel oldValue = this.selectionModel;
            if (oldValue != null) {
                oldValue.removeListSelectionListener(selectionPropagator);
            }
            this.selectionModel = selectionModel;
            selectionModel.addListSelectionListener(selectionPropagator);
            firePropertyChange(StringConstants.SELECTION_MODEL_PROPERTY, oldValue, selectionModel);
        }
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }


    public void setListData(final Vector<?> listData) {
        DefaultListModel defaultModel = new DefaultListModel();
        putListData(defaultModel, listData);

        setModel(defaultModel);
    }

    public void setListData(final Object[] listData) {
        DefaultListModel defaultModel = new DefaultListModel();
        putListData(defaultModel, listData);

        setModel(defaultModel);
    }


    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public void setUI(final ListUI ui) {
        super.setUI(ui);
    }

    public ListUI getUI() {
        return (ListUI)ui;
    }

    public void updateUI() {
        setUI((ListUI)UIManager.getUI(this));
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJList();
        }

        return accessibleContext;
    }


    public void setSelectionBackground(final Color selectionBackground) {
        if (this.selectionBackground != selectionBackground) {
            Color oldValue = this.selectionBackground;
            this.selectionBackground = selectionBackground;
            firePropertyChange(SELECTION_BACKGROUND_CHANGED_PROPERTY, oldValue, selectionBackground);
        }
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionForeground(final Color selectionForeground) {
        if (this.selectionForeground != selectionForeground) {
            Color oldValue = this.selectionForeground;
            this.selectionForeground = selectionForeground;
            firePropertyChange(SELECTION_FOREGROUND_CHANGED_PROPERTY, oldValue, selectionForeground);
        }
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }


    public void setCellRenderer(final ListCellRenderer cellRenderer) {
        if (this.cellRenderer != cellRenderer) {
            ListCellRenderer oldValue = this.cellRenderer;
            this.cellRenderer = cellRenderer;
            firePropertyChange(CELL_RENDERER_CHANGED_PROPERTY, oldValue, cellRenderer);
        }
    }

    public ListCellRenderer getCellRenderer() {
        return cellRenderer;
    }

    public void setDragEnabled(final boolean dragEnabled) {
        if (dragEnabled && GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException(Messages.getString("swing.18")); //$NON-NLS-1$
        }

        this.dragEnabled = dragEnabled;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }


    public void setFixedCellHeight(final int height) {
        LookAndFeel.markPropertyNotInstallable(this, "fixedCellHeight");
        if (this.fixedCellHeight != height) {
            int oldValue = this.fixedCellHeight;
            this.fixedCellHeight = height;
            firePropertyChange(FIXED_CELL_HEIGHT_CHANGED_PROPERTY, oldValue, height);
        }
    }

    public int getFixedCellHeight() {
        return fixedCellHeight;
    }

    public void setFixedCellWidth(final int width) {
        LookAndFeel.markPropertyNotInstallable(this, "fixedCellWidth");
        if (this.fixedCellWidth != width) {
            int oldValue = this.fixedCellWidth;
            this.fixedCellWidth = width;
            firePropertyChange(FIXED_CELL_WIDTH_CHANGED_PROPERTY, oldValue, width);
        }
    }

    public int getFixedCellWidth() {
        return fixedCellWidth;
    }

    public void setLayoutOrientation(final int layoutOrientation) {
        if (layoutOrientation != VERTICAL
            && layoutOrientation != VERTICAL_WRAP
            && layoutOrientation != HORIZONTAL_WRAP) {

            throw new IllegalArgumentException(Messages.getString("swing.19")); //$NON-NLS-1$
        }

        if (this.layoutOrientation != layoutOrientation) {
            int oldValue = this.layoutOrientation;
            this.layoutOrientation = layoutOrientation;
            firePropertyChange(LAYOUT_ORIENTATION_CHANGED_PROPERTY, oldValue, layoutOrientation);
        }
    }

    public int getLayoutOrientation() {
        return layoutOrientation;
    }

    public void setPrototypeCellValue(final Object prototypeCellValue) {
        if (this.prototypeCellValue != prototypeCellValue) {
            Object oldValue = this.prototypeCellValue;
            this.prototypeCellValue = prototypeCellValue;
            firePropertyChange(PROTOTYPE_CELL_VALUE_CHANGED_PROPERTY, oldValue, prototypeCellValue);
            Dimension prototypePreferredSize = new Dimension(0,0);
            if (getCellRenderer() != null)
            prototypePreferredSize = getCellRenderer().getListCellRendererComponent(this, prototypeCellValue, 0, false, false).getPreferredSize();
            this.fixedCellWidth = prototypePreferredSize.width;
            this.fixedCellHeight = prototypePreferredSize.height;
        }
    }

    public Object getPrototypeCellValue() {
        return prototypeCellValue;
    }

    public void setSelectionMode(final int selectionMode) {
        selectionModel.setSelectionMode(selectionMode);
    }

    public int getSelectionMode() {
        return selectionModel.getSelectionMode();
    }

    public void setSelectedIndex(final int index) {
        selectionModel.setSelectionInterval(index, index);
    }

    public int getSelectedIndex() {
        return selectionModel.getMinSelectionIndex();
    }

    public void setSelectedIndices(final int[] indices) {
        clearSelection();
        if (indices != null && indices.length > 0) {
            for (int i = 0; i < indices.length; i++) {
                if (indices[i] >= 0 && indices[i] < getModel().getSize()) {
                    selectionModel.addSelectionInterval(indices[i], indices[i]);
                }
            }
        }
    }

    public int[] getSelectedIndices() {
        int selectionCount = 0;
        for (int i = 0; i < model.getSize(); i++) {
            if (isSelectedIndex(i)) {
                selectionCount++;
            }
        }

        int[] result = new int[selectionCount];
        selectionCount = 0;
        for (int i = 0; i < model.getSize(); i++) {
            if (isSelectedIndex(i)) {
                result[selectionCount++] = i;
            }
        }

        return result;
    }

    public void setSelectedValue(final Object element, final boolean shouldScroll) {
        int index = indexOf(element);
        if (index != -1) {
            selectionModel.setSelectionInterval(index, index);
            if (shouldScroll) {
                scrollRectToVisible(getCellBounds(index, index));
            }
        }
    }

    public void setSelectionInterval(final int anchor, final int lead) {
        selectionModel.setSelectionInterval(anchor, lead);
    }

    public void addSelectionInterval(final int anchor, final int lead) {
        selectionModel.addSelectionInterval(anchor, lead);
    }

    public void removeSelectionInterval(final int anchor, final int lead) {
        selectionModel.removeSelectionInterval(anchor, lead);
    }

    public void clearSelection() {
        selectionModel.clearSelection();
    }

    public int getAnchorSelectionIndex() {
        return selectionModel.getAnchorSelectionIndex();
    }

    public int getLeadSelectionIndex() {
        return selectionModel.getLeadSelectionIndex();
    }

    public int getMaxSelectionIndex() {
        return selectionModel.getMaxSelectionIndex();
    }

    public int getMinSelectionIndex() {
        return selectionModel.getMinSelectionIndex();
    }


    public Object getSelectedValue() {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex > -1) {
            return model.getElementAt(selectedIndex);
        }

        return null;
    }

    public Object[] getSelectedValues() {
        int[] selectedIndices = getSelectedIndices();
        Object[] result = new Object[selectedIndices.length];

        for (int i = 0; i < selectedIndices.length; i++) {
            result[i] = model.getElementAt(selectedIndices[i]);
        }

        return result;
    }

    public boolean isSelectedIndex(final int index) {
        return selectionModel.isSelectedIndex(index);
    }


    public boolean isSelectionEmpty() {
        return selectionModel.isSelectionEmpty();
    }


    public void setValueIsAdjusting(final boolean isAdjusting) {
        selectionModel.setValueIsAdjusting(isAdjusting);
    }

    public boolean getValueIsAdjusting() {
        return selectionModel.getValueIsAdjusting();
    }


    public void addListSelectionListener(final ListSelectionListener listener) {
        selectionListeners.add(ListSelectionListener.class, listener);
    }

    public void removeListSelectionListener(final ListSelectionListener listener) {
        selectionListeners.remove(ListSelectionListener.class, listener);
    }

    public ListSelectionListener[] getListSelectionListeners() {
        return (ListSelectionListener[])selectionListeners.getListeners(ListSelectionListener.class);
    }

    public Point indexToLocation(final int index) {
        return ((ListUI)ui).indexToLocation(this, index);
    }

    public int locationToIndex(final Point location) {
        return ((ListUI)ui).locationToIndex(this, location);
    }

    public int getNextMatch(final String prefix, final int startIndex, final Position.Bias bias) {
        return Utilities.getNextMatch(new Utilities.ListModelAccessor() {
            public Object getElementAt(final int index) {
                return model.getElementAt(index);
            }

            public int getSize() {
                return model.getSize();
            }
            
        }, prefix, startIndex, bias);
    }

    public Rectangle getCellBounds(final int index0, final int index1) {
        return ((ListUI)ui).getCellBounds(this, index0, index1);
    }


    public void setVisibleRowCount(final int visibleRowCount) {
        LookAndFeel.markPropertyNotInstallable(this, "visibleRowCount");
        if (this.visibleRowCount != visibleRowCount) {
            int oldValue = this.visibleRowCount;
            this.visibleRowCount = visibleRowCount >= 0 ? visibleRowCount : 0;
            firePropertyChange(VISIBLE_ROW_COUNT_CHANGED_PROPERTY, oldValue, visibleRowCount);
        }
    }

    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    public void ensureIndexIsVisible(final int index) {
        if (model.getSize() <= index) {
            return;
        }

        Rectangle cellBounds = getCellBounds(index, index);
        if (cellBounds != null) {
            scrollRectToVisible(cellBounds);
        }
    }

    public int getFirstVisibleIndex() {
        Rectangle visibleRect = getVisibleRect();
        if (visibleRect.isEmpty()) {
            return -1;
        }

        ComponentOrientation co = getComponentOrientation();
        if (co.isHorizontal()) {
            for (int i = 0; i < model.getSize(); i++) {
                Rectangle bounds = getCellBounds(i, i);
                if (bounds.intersects(visibleRect)) {
                    return i;
                }
            }
        }

        return -1;
    }

    public int getLastVisibleIndex() {
        Rectangle visibleRect = getVisibleRect();
        if (visibleRect.isEmpty()) {
            return -1;
        }

        ComponentOrientation co = getComponentOrientation();
        if (co.isHorizontal()) {
            for (int i = model.getSize() - 1; i >= 0; i--) {
                Rectangle bounds = getCellBounds(i, i);
                if (bounds.intersects(visibleRect)) {
                    return i;
                }
            }
        }

        return -1;
    }

    public Dimension getPreferredScrollableViewportSize() {
        if (layoutOrientation != VERTICAL) {
            return getPreferredSize();
        }

        int height;
        if (fixedCellWidth != -1) {
            height = fixedCellHeight * visibleRowCount;
        } else {
            if (model.getSize() > 0) {
                Rectangle bounds = getCellBounds(0, 0);
                height = bounds.height * visibleRowCount;
            } else {
                height = 16 * visibleRowCount;
            }
        }

        int width;
        if (fixedCellWidth != -1) {
            width = fixedCellWidth;
        } else {
            if (model.getSize() > 0) {
                width = getMaximumCellWidth();
            } else {
                width = 256;
            }
        }

        Insets insets = getInsets();
        return new Dimension(width + insets.top + insets.bottom, height + insets.right + insets.left);
    }

    public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        if (visibleRect == null) {
            throw new IllegalArgumentException(Messages.getString("swing.1A")); //$NON-NLS-1$
        }
        if (orientation != SwingConstants.VERTICAL && orientation != SwingConstants.HORIZONTAL) {
            throw new IllegalArgumentException(Messages.getString("swing.1B")); //$NON-NLS-1$
        }

        if (orientation == SwingConstants.VERTICAL) {
            int result = 0;
            if (model.getSize() == 0) {
                return visibleRect.height;
            }

            if (direction > 0) {
                Rectangle bounds = getCellBounds(getLastVisibleIndex(), getLastVisibleIndex());
                if (bounds != null) {
                    result = visibleRect.height - bounds.intersection(visibleRect).height;
                }
            } else {
                Rectangle bounds = getCellBounds(getFirstVisibleIndex(), getFirstVisibleIndex());
                if (bounds != null) {
                    result = visibleRect.height - bounds.intersection(visibleRect).height;
                }
            }

            return result > 0 ? result : visibleRect.height;
        } else if (orientation == SwingConstants.HORIZONTAL) {
            int result = 0;
            if (model.getSize() == 0) {
                return visibleRect.width;
            }

            if (layoutOrientation == JList.VERTICAL_WRAP || layoutOrientation == JList.HORIZONTAL_WRAP) {
                if (direction > 0) {
                    Rectangle bounds = getCellBounds(getLastVisibleIndex(), getLastVisibleIndex());
                    if (bounds != null) {
                        result = visibleRect.width - bounds.intersection(visibleRect).width;
                    }
                } else {
                    Rectangle bounds = getCellBounds(getFirstVisibleIndex(), getFirstVisibleIndex());
                    if (bounds != null) {
                        result = visibleRect.width - bounds.intersection(visibleRect).width;
                    }
                }
            }

            return result > 0 ? result : visibleRect.width;
        }

        return getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        if (visibleRect == null) {
            throw new IllegalArgumentException(Messages.getString("swing.1A")); //$NON-NLS-1$
        }
        if (orientation != SwingConstants.VERTICAL && orientation != SwingConstants.HORIZONTAL) {
            throw new IllegalArgumentException(Messages.getString("swing.1B")); //$NON-NLS-1$
        }

        if (orientation == SwingConstants.HORIZONTAL) {
            if (getFont() == null) {
                return 1;
            } else {
                return getFont().getSize();
            }
        } else {
            int firstVisibleIndex = getFirstVisibleIndex();
            if (firstVisibleIndex == -1) {
                return 0;
            }
            Rectangle firstVisibleBounds = getCellBounds(firstVisibleIndex, firstVisibleIndex);
            int hiddenPart = visibleRect.y - firstVisibleBounds.y;
            if (direction > 0) {
                return firstVisibleBounds.height - hiddenPart;
            } else {
                if (hiddenPart == 0) {
                    if (firstVisibleIndex == 0) {
                        return 0;
                    } else {
                        Rectangle firstInvisibleBounds = getCellBounds(firstVisibleIndex - 1, firstVisibleIndex - 1);
                        return firstInvisibleBounds.height;
                    }
                } else {
                    return hiddenPart;
                }
            }
        }
    }

    public boolean getScrollableTracksViewportHeight() {
        Container parent = getParent();
        if (!(parent instanceof JViewport)) {
            return false;
        }

        if (parent.getHeight() > getPreferredSize().height) {
            return true;
        }
        if (layoutOrientation == VERTICAL_WRAP && visibleRowCount <= 0) {
            return true;
        }

        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        Container parent = getParent();
        if (!(parent instanceof JViewport)) {
            return false;
        }

        if (parent.getWidth() > getPreferredSize().width) {
            return true;
        }
        if (layoutOrientation == HORIZONTAL_WRAP && visibleRowCount <= 0) {
            return true;
        }

        return false;
    }


    public String getToolTipText(final MouseEvent event) {
        int index = locationToIndex(event.getPoint());
        if (index == -1) {
            return super.getToolTipText();
        }
        if (cellRenderer == null) {
        return super.getToolTipText();
        }
        Component renderer = cellRenderer.getListCellRendererComponent(this, model.getElementAt(index), index, false, false);
        String result = null;
        if (renderer instanceof JComponent) {
            result = ((JComponent)renderer).getToolTipText(SwingUtilities.convertMouseEvent(this, event, renderer));
        }
        return result != null ? result : super.getToolTipText();
    }


    protected ListSelectionModel createSelectionModel() {
        return new DefaultListSelectionModel();
    }

    protected void fireSelectionValueChanged(final int firstIndex, final int lastIndex, final boolean isAdjusting) {
        ListSelectionEvent event = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
        ListSelectionListener[] listeners = getListSelectionListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].valueChanged(event);
        }
    }


    private int indexOf(final Object element) {
        if (model instanceof DefaultListModel) {
            return ((DefaultListModel)model).indexOf(element);
        } else {
            for (int i = 0; i < model.getSize(); i++) {
                if (element == null && model.getElementAt(i) == null
                    || element != null && element.equals(model.getElementAt(i))) {

                    return i;
                }
            }
        }

        return -1;
    }

    private int getMaximumCellWidth() {
        int result = 0;
        if(cellRenderer==null)
            return result;
        for (int i = 0; i < model.getSize(); i++) {
            int width = cellRenderer.getListCellRendererComponent(this, model.getElementAt(i), i, false, false).getPreferredSize().width;
            if (result < width) {
                result = width;
            }
        }

        return result;
    }

    private void putListData(final DefaultListModel model, final Vector listData) {
        if (listData != null) {
            for (Iterator it = listData.iterator(); it.hasNext(); ) {
                model.addElement(it.next());
            }
        }
    }

    private void putListData(final DefaultListModel model, final Object[] listData) {
        if (listData != null) {
            for (int i = 0; i < listData.length; i++) {
                model.addElement(listData[i]);
            }
        }
    }


    private class ListSelectionEventPropagator implements ListSelectionListener {
        public void valueChanged(final ListSelectionEvent event) {
            fireSelectionValueChanged(event.getFirstIndex(), event.getLastIndex(), event.getValueIsAdjusting());
        }
    }
}
