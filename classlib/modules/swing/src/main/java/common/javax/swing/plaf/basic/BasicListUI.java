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
package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ListUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.Position;

import org.apache.harmony.x.swing.ExtendedListElement;
import org.apache.harmony.x.swing.ExtendedListFactory;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class BasicListUI extends ListUI {
    protected int cellHeight = -1;
    protected int cellWidth = -1;
    protected int[] cellHeights;
    protected JList list;
    protected CellRendererPane rendererPane;
    protected int updateLayoutStateNeeded = modelChanged;

    protected static final int modelChanged = 1;
    protected static final int selectionModelChanged = 2;
    protected static final int fontChanged = 4;
    protected static final int fixedCellWidthChanged = 8;
    protected static final int fixedCellHeightChanged = 16;
    protected static final int prototypeCellValueChanged = 32;
    protected static final int cellRendererChanged = 64;
    private static final int otherChanged = 128;

    protected FocusListener focusListener;
    protected ListDataListener listDataListener;
    protected ListSelectionListener listSelectionListener;
    protected MouseInputListener mouseInputListener;
    protected PropertyChangeListener propertyChangeListener;
    private ComponentListener componentListener;
    private KeyListener keyListener;

    boolean extendedSupportEnabled;


    final ListLayouter layouter = new ListLayouter();

    public class FocusHandler implements FocusListener {
        public void focusGained(final FocusEvent e) {
            repaintCellFocus();
        }

        public void focusLost(final FocusEvent e) {
            repaintCellFocus();
        }

        protected void repaintCellFocus() {
            if (list.getLeadSelectionIndex() != -1) {
                Rectangle bounds = getCellBounds(list, list.getLeadSelectionIndex(), list.getLeadSelectionIndex());
                if (bounds != null) {
                    list.repaint(bounds);
                } else {
                    list.repaint();
                }
            }
        }
    }

    public class ListDataHandler implements ListDataListener {
        public void contentsChanged(final ListDataEvent e) {
            layouter.reset();
            list.revalidate();
            list.repaint();
        }

        public void intervalAdded(final ListDataEvent e) {
            list.getSelectionModel().insertIndexInterval(e.getIndex0(), Math.abs(e.getIndex1() - e.getIndex0()) + 1, false);
            layouter.reset();
            list.revalidate();
            list.repaint();
        }

        public void intervalRemoved(final ListDataEvent e) {
            list.getSelectionModel().removeIndexInterval(e.getIndex0(), e.getIndex1());
            layouter.reset();
            list.revalidate();
            list.repaint();
        }
    }

    public class ListSelectionHandler implements ListSelectionListener {
        public void valueChanged(final ListSelectionEvent e) {
            repaintCells(e.getFirstIndex(), e.getLastIndex());
        }
    }

    public class MouseInputHandler implements MouseInputListener {
        private DnDMouseHelper dndhelper = new DnDMouseHelper(list);

        public void mousePressed(final MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            list.requestFocus();

            int cellIndex = locationToIndex(list, e.getPoint());
            dndhelper.mousePressed(e, list.getDragEnabled(), cellIndex != -1,
                                   list.isSelectedIndex(cellIndex));
            if (cellIndex == -1) {
                return;
            }
            if (!list.getDragEnabled() || !list.isSelectedIndex(cellIndex)) {
                list.getSelectionModel().setValueIsAdjusting(true);
                processSelection(e, cellIndex);
            }
        }

        public void mouseReleased(final MouseEvent e) {
            dndhelper.mouseReleased(e);
            if (dndhelper.shouldProcessOnRelease()) {
                int cellIndex = locationToIndex(list, e.getPoint());
                if (cellIndex != -1) {
                    processSelection(e, cellIndex);
                }
            }
            list.getSelectionModel().setValueIsAdjusting(false);
        }

        public void mouseDragged(final MouseEvent e) {
            dndhelper.mouseDragged(e);

            if (!dndhelper.isDndStarted()
                && SwingUtilities.isLeftMouseButton(e)
                && !e.isControlDown()
                && !e.isShiftDown()) {

                int cellIndex = locationToIndex(list, e.getPoint());
                if (cellIndex != -1 && list.getLeadSelectionIndex() != cellIndex) {
                    list.getSelectionModel().setValueIsAdjusting(true);
                    list.setSelectedIndex(cellIndex);
                    list.ensureIndexIsVisible(cellIndex);
                }
            }
        }

        public void mouseClicked(final MouseEvent e) {
        }

        public void mouseEntered(final MouseEvent e) {
        }

        public void mouseExited(final MouseEvent e) {
        }

        public void mouseMoved(final MouseEvent e) {
        }


        private void processSelection(final MouseEvent e, final int cellIndex) {
            if (e.isControlDown()) {
                if (list.isSelectedIndex(cellIndex)) {
                    list.removeSelectionInterval(cellIndex, cellIndex);
                } else {
                    list.addSelectionInterval(cellIndex, cellIndex);
                }
            } else if (e.isShiftDown() && list.getAnchorSelectionIndex() != -1) {
                list.setSelectionInterval(list.getAnchorSelectionIndex(), cellIndex);
            } else {
                list.setSelectionInterval(cellIndex, cellIndex);
            }
        }
    }

    public class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
            String changedProperty = event.getPropertyName();
            if ("cellRenderer".equals(changedProperty)) {
                updateLayoutStateNeeded = updateLayoutStateNeeded | cellRendererChanged;
            } else if ("fixedCellHeight".equals(changedProperty)) {
                updateLayoutStateNeeded = updateLayoutStateNeeded | fixedCellHeightChanged;
            } else if ("fixedCellWidth".equals(changedProperty)) {
                updateLayoutStateNeeded = updateLayoutStateNeeded | fixedCellWidthChanged;
            } else if ("model".equals(changedProperty)) {
                updateLayoutStateNeeded = updateLayoutStateNeeded | modelChanged;
            } else if ("selectionModel".equals(changedProperty)) {
                updateLayoutStateNeeded = updateLayoutStateNeeded | selectionModelChanged;
            } else if ("prototypeCellValue".equals(changedProperty)) {
                updateLayoutStateNeeded = updateLayoutStateNeeded | prototypeCellValueChanged;
            } else if ("font".equals(changedProperty)) {
                updateLayoutStateNeeded = updateLayoutStateNeeded | fontChanged;
            } else if ("visibleRowCount".equals(changedProperty)
                       || "layoutOrientation".equals(changedProperty)
                       || "componentOrientation".equals(changedProperty)
                       || "width".equals(changedProperty)
                       || "height".equals(changedProperty)) {
                updateLayoutStateNeeded = updateLayoutStateNeeded | otherChanged;
            } else if (StringConstants.COMPONENT_ORIENTATION.equals(changedProperty)) {
                uninstallKeyboardActions();
                installKeyboardActions();
            }

            if ("model".equals(changedProperty)) {
                ((ListModel)event.getOldValue()).removeListDataListener(listDataListener);
                ((ListModel)event.getNewValue()).addListDataListener(listDataListener);
            }
            if ("selectionModel".equals(changedProperty)) {
                ((ListSelectionModel)event.getOldValue()).removeListSelectionListener(listSelectionListener);
                ((ListSelectionModel)event.getNewValue()).addListSelectionListener(listSelectionListener);
            }

            if ("enabled".equals(changedProperty)) {
                if (((Boolean)event.getNewValue()).booleanValue()) {
                    list.addMouseListener(mouseInputListener);
                    list.addMouseMotionListener(mouseInputListener);
                } else {
                    list.removeMouseListener(mouseInputListener);
                    list.removeMouseMotionListener(mouseInputListener);
                }
            }

            if (StringConstants.EXTENDED_SUPPORT_ENABLED_PROPERTY.equals(changedProperty)) {
                extendedSupportEnabled = ((Boolean)event.getNewValue()).booleanValue();
                if (extendedSupportEnabled) {
                    list.setCellRenderer(ExtendedListFactory.getFactory().createExtendedRenderer());
                } else {
                    list.setCellRenderer((ListCellRenderer)UIManager.get("List.cellRenderer"));
                }
                updateLayoutStateNeeded = updateLayoutStateNeeded | otherChanged;
            }

            list.revalidate();
            list.repaint();
        }
    }

    private class ComponentReshapeHandler extends ComponentAdapter {
        @Override
        public void componentResized(final ComponentEvent e) {
            if (layouter.getLayoutStrategy().isSizeDependent()) {
                layouter.reset();
                list.repaint();
            }
        }
    }

    private class KeyHandler extends KeyAdapter {
        private StringBuffer searchPrefix = new StringBuffer();
        private Timer searchTimer = new Timer(1000, new AbstractAction() {
            /**
             * This class is not guaranteed to be correctly deserialized.
             */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                resetSearch();
            }
        });

        @Override
        public void keyTyped(final KeyEvent e) {
            if (list.getModel().getSize() == 0) {
                return;
            }

            if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
                return;
            }
            if ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0
                || (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {

                return;
            }

            searchPrefix.append(e.getKeyChar());

            int startIndex = list.getSelectedIndex();
            if (startIndex == -1) {
                startIndex = 0;
            }

            int nextIndex = getNextMatch(startIndex);
            if (nextIndex == -1) {
                if (searchPrefix.length() > 1) {
                    resetSearch();
                    searchPrefix.append(e.getKeyChar());
                    startIndex++;
                    if (startIndex >= list.getModel().getSize()) {
                        startIndex = 0;
                    }
                    nextIndex = getNextMatch(startIndex);
                }
            }
            if (nextIndex != -1) {
                searchTimer.stop();
                list.setSelectedIndex(nextIndex);
                list.ensureIndexIsVisible(nextIndex);
                searchTimer.restart();
            } else {
                resetSearch();
            }
        }

        private int getNextMatch(final int startIndex) {
            int candidateIndex = list.getNextMatch(searchPrefix.toString(), startIndex, Position.Bias.Forward);
            if (candidateIndex == -1 || !extendedSupportEnabled) {
                return candidateIndex;
            }

            int firstFoundIndex = candidateIndex;
            do {
                Object value = list.getModel().getElementAt(candidateIndex);
                if (!(value instanceof ExtendedListElement)
                    || ((ExtendedListElement)value).isChoosable()) {

                    return candidateIndex;
                }
                int nextStartIndex = candidateIndex + 1;
                if (nextStartIndex == list.getModel().getSize()) {
                    nextStartIndex = 0;
                }
                candidateIndex = list.getNextMatch(searchPrefix.toString(), nextStartIndex, Position.Bias.Forward);
            } while(firstFoundIndex != candidateIndex);

            return -1;
        }

        private void resetSearch() {
            searchPrefix.delete(0, searchPrefix.length());
            searchTimer.stop();
        }
    };


    private class ListTransferHandler extends TransferHandler {
        /**
         * This class is not guaranteed to be correctly deserialized.
         */
        private static final long serialVersionUID = -3865101058747175640L;

        private final String lineSeparator = System.getProperty("line.separator");

        @Override
        public int getSourceActions(final JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(final JComponent c) {
            Object[] selectedValues = list.getSelectedValues();
            if (selectedValues == null || selectedValues.length == 0) {
                return null;
            }

            StringBuilder content = new StringBuilder();
            for (int i = 0; i < selectedValues.length; i++) {
                content.append(selectedValues[i]);
                if (i < selectedValues.length - 1) {
                    content.append(lineSeparator);
                }
            }
            return new StringSelection(content.toString());
        }
    }



    public static ComponentUI createUI(final JComponent list) {
        return new BasicListUI();
    }

    @Override
    public Rectangle getCellBounds(final JList list, final int index1, final int index2) {
        layouter.setList(list);
        maybeUpdateLayoutState();

        Rectangle result = null;
        if (index1 < 0 || index1 >= list.getModel().getSize()
            || index2 < 0 || index2 >= list.getModel().getSize()) {

            return result;
        }

        if (index1 <= index2) {
            for (int i = index1; i <= index2; i++) {
                if (result == null) {
                    result = layouter.getLayoutStrategy().getBounds(i);
                } else {
                    result.add(layouter.getLayoutStrategy().getBounds(i));
                }
            }
        } else {
            for (int i = index2; i <= index1; i++) {
                if (result == null) {
                    result = layouter.getLayoutStrategy().getBounds(i);
                } else {
                    result.add(layouter.getLayoutStrategy().getBounds(i));
                }
            }
        }

        return result;
    }

    @Override
    public Dimension getMaximumSize(final JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public Dimension getMinimumSize(final JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public Dimension getPreferredSize(final JComponent c) {
        maybeUpdateLayoutState();

        JList list = (JList)c;
        layouter.setList(list);
        return layouter.getLayoutStrategy().getSize();
    }

    @Override
    public Point indexToLocation(final JList list, final int index) {
        if (index < 0 || index >= list.getModel().getSize()) {
            return null;
        }

        layouter.setList(list);
        maybeUpdateLayoutState();

        Rectangle bounds = getCellBounds(list, index, index);
        return new Point(bounds.x, bounds.y);
    }

    @Override
    public int locationToIndex(final JList list, final Point location) {
        if (location == null) {
            throw new NullPointerException();
        }

        layouter.setList(list);
        maybeUpdateLayoutState();

        return layouter.getNearestIndex(location);
    }

    @Override
    public void paint(final Graphics g, final JComponent c) {
        maybeUpdateLayoutState();

        Rectangle clipRect = g.getClipBounds();
        if (clipRect != null) {
            for (int i = 0; i < list.getModel().getSize(); i++) {
                Rectangle bounds = layouter.getLayoutStrategy().getBounds(i);
                if (bounds.intersects(clipRect)) {
                    paintCell(g, i, bounds, list.getCellRenderer(), list.getModel(), list.getSelectionModel(), i);
                }
            }
        } else {
            for (int i = 0; i < list.getModel().getSize(); i++) {
                Rectangle bounds = layouter.getLayoutStrategy().getBounds(i);
                paintCell(g, i, bounds, list.getCellRenderer(), list.getModel(), list.getSelectionModel(), i);
            }
        }
    }


    @Override
    public void installUI(final JComponent c) {
        list = (JList)c;
        rendererPane = new CellRendererPane();
        rendererPane.setVisible(false);
        list.add(rendererPane);
        layouter.setList(list);

        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    @Override
    public void uninstallUI(final JComponent c) {
        list = (JList)c;
        list.remove(rendererPane);
        rendererPane = null;
        layouter.setList(null);

        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
    }

    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(list, "List.background", "List.foreground", "List.font");
        LookAndFeel.installBorder(list, "List.border");
        LookAndFeel.installProperty(list, "opaque", Boolean.TRUE);

        if ((list.getSelectionBackground() == null) || (list.getSelectionBackground() instanceof UIResource)) {
            list.setSelectionBackground(UIManager.getColor("List.selectionBackground"));
        }
        if ((list.getSelectionForeground() == null) || (list.getSelectionForeground() instanceof UIResource)) {
            list.setSelectionForeground(UIManager.getColor("List.selectionForeground"));
        }

        if ((list.getCellRenderer() == null) || (list.getCellRenderer() instanceof UIResource)) {
            list.setCellRenderer((ListCellRenderer)UIManager.get("List.cellRenderer"));
        }

        list.setTransferHandler(new ListTransferHandler());
    }

    protected void installKeyboardActions() {
        BasicListKeyboardActions.installKeyboardActions(list);
    }

    protected void installListeners() {
        focusListener = createFocusListener();
        list.addFocusListener(focusListener);

        listSelectionListener = createListSelectionListener();
        list.getSelectionModel().addListSelectionListener(listSelectionListener);

        listDataListener = createListDataListener();
        list.getModel().addListDataListener(listDataListener);

        propertyChangeListener = createPropertyChangeListener();
        list.addPropertyChangeListener(propertyChangeListener);

        mouseInputListener = createMouseInputListener();
        list.addMouseListener(mouseInputListener);
        list.addMouseMotionListener(mouseInputListener);

        componentListener = createComponentListener();
        list.addComponentListener(componentListener);

        keyListener = new KeyHandler();
        list.addKeyListener(keyListener);
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(list);
        if (list.getCellRenderer() instanceof UIResource) {
            list.setCellRenderer(null);
        }
        Utilities.uninstallColorsAndFont(list);

        list.setTransferHandler(null);
    }

    protected void uninstallKeyboardActions() {
        BasicListKeyboardActions.uninstallKeyboardActions(list);
    }

    protected void uninstallListeners() {
        list.removeListSelectionListener(listSelectionListener);
        list.getSelectionModel().removeListSelectionListener(listSelectionListener);
        listSelectionListener = null;

        list.getModel().removeListDataListener(listDataListener);
        listDataListener = null;

        list.removeFocusListener(focusListener);
        focusListener = null;

        list.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;

        list.removeMouseListener(mouseInputListener);
        list.removeMouseMotionListener(mouseInputListener);
        mouseInputListener = null;

        list.removeComponentListener(componentListener);
        componentListener = null;

        list.removeKeyListener(keyListener);
        keyListener = null;
    }

    protected void updateLayoutState() {
        layouter.reinit();
    }

    protected void maybeUpdateLayoutState() {
        if (updateLayoutStateNeeded > 0) {
            updateLayoutState();
            updateLayoutStateNeeded = 0;
        }
    }

    protected void paintCell(final Graphics g, final int row, final Rectangle rowBounds, final ListCellRenderer cellRenderer, final ListModel dataModel, final ListSelectionModel selModel, final int leadIndex) {
        if (list.getCellRenderer() != null){
             Component renderer = list.getCellRenderer().getListCellRendererComponent(list, dataModel.getElementAt(row), row, selModel.isSelectedIndex(row), list.isFocusOwner() && selModel.getLeadSelectionIndex() == row);
             rendererPane.paintComponent(g, renderer, list, rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height);
      }
    }

    protected void selectNextIndex() {
        int currectSelection = list.getMinSelectionIndex();

        if (currectSelection < list.getModel().getSize() - 1 && list.getModel().getSize() > 0) {
            list.setSelectedIndex(currectSelection + 1);
            list.ensureIndexIsVisible(currectSelection + 1);
        }
    }

    protected void selectPreviousIndex() {
        int currectSelection = list.getMinSelectionIndex();
        if (currectSelection > 0) {
            list.setSelectedIndex(currectSelection - 1);
            list.ensureIndexIsVisible(currectSelection - 1);
        }
    }

    protected int getRowHeight(final int row) {
        if (row < 0 || row >= list.getModel().getSize()) {
            return -1;
        }

        return layouter.getLayoutStrategy().getBounds(row).height;
    }

    protected int convertRowToY(final int row) {
        if (row < 0 || row >= list.getModel().getSize()) {
            return -1;
        }

        return getCellBounds(list, row, row).y;
    }

    protected int convertYToRow(final int y) {
        for (int row = 0; row < list.getModel().getSize(); row++) {
            Rectangle bounds = getCellBounds(list, row, row);
            if (bounds.y <= y && bounds.y + bounds.height >= y) {
                return row;
            }
        }

        return -1;
    }

    protected FocusListener createFocusListener() {
        return new FocusHandler();
    }

    protected ListDataListener createListDataListener() {
        return new ListDataHandler();
    }

    protected ListSelectionListener createListSelectionListener() {
        return new ListSelectionHandler();
    }

    protected MouseInputListener createMouseInputListener() {
        return new MouseInputHandler();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }


    boolean isChoosable(final int index) {
        if (!extendedSupportEnabled) {
            return true;
        }
        Object value = list.getModel().getElementAt(index);
        return (value instanceof ExtendedListElement) ? ((ExtendedListElement)value).isChoosable() : true;
    }


    private ComponentListener createComponentListener() {
        return new ComponentReshapeHandler();
    }


    private void repaintCells(final int firstIndex, final int lastIndex) {
        if (firstIndex == -1 || lastIndex == -1) {
            return;
        }
        int modelSize = list.getModel().getSize();
        int adjustedFirstIndex = firstIndex;
        if (adjustedFirstIndex >= modelSize) {
            adjustedFirstIndex = modelSize - 1;
        }
        int adjustedLastIndex = lastIndex;
        if (adjustedLastIndex >= modelSize) {
            adjustedLastIndex = modelSize - 1;
        }

        Rectangle bounds = getCellBounds(list, adjustedFirstIndex, adjustedLastIndex);
        if (bounds != null) {
            list.repaint(bounds);
        }
    }


    interface LayoutStrategy {
        Rectangle getBounds(final int index);
        int getRow(final int index);
        int getColumn(final int index);
        int getIndex(final int row, final int column);
        int getRowCount();
        int getColumnCount();
        Dimension getSize();
        boolean isSizeDependent();
    }

    final class ListLayouter {
        private JList list;
        private Insets insets;
        private LayoutStrategy strategy;
        private Insets cachedInsets = new Insets(0, 0, 0, 0);

        public abstract class AbstractLayoutStrategy implements LayoutStrategy {
            public abstract int getRowCount();
            public abstract int getColumnCount();

            public AbstractLayoutStrategy() {
                cellWidth = getMaximumWidth();
                cellHeight = getMaximumHeight();
                cellHeights = null;
            }

            public Rectangle getBounds(final int index) {
                return new Rectangle(insets.left + getColumn(index) * cellWidth, insets.top + getRow(index) * cellHeight, cellWidth, cellHeight);
            }

            public Dimension getSize() {
                Dimension result = new Dimension(getColumnCount() * cellWidth, getRowCount() * cellHeight);
                return Utilities.addInsets(result, list.getInsets(cachedInsets));
            }

            public boolean isSizeDependent() {
                return list.getVisibleRowCount() <= 0;
            }
        }

        public class VerticalLayoutStategy extends AbstractLayoutStrategy {
            public VerticalLayoutStategy() {
                if (list.getFixedCellHeight() <= 0) {
                    cellHeight = -1;

                    cellHeights = new int[getNumberOfElements()];
                    for (int i = 0; i < getNumberOfElements(); i++) {
                        cellHeights[i] = getPreferredSize(i).height;
                    }
                }
            }

            @Override
            public Rectangle getBounds(final int index) {
                int y = 0;
                int height = cellHeight;
                if (cellHeight == -1) {
                    for (int i = 0; i < index; i++) {
                        y += cellHeights[i];
                    }
                    height = cellHeights[index];
                } else if (index > 0) {
                    y = cellHeight * index;
                }

                return new Rectangle(insets.left, insets.top + y, list.getWidth() - insets.left - insets.right, height);
            }

            public int getRow(final int index) {
                return index;
            }

            public int getColumn(final int index) {
                return 0;
            }

            public int getIndex(final int row, final int column) {
                return row;
            }

            @Override
            public int getRowCount() {
                return list.getModel().getSize();
            }

            @Override
            public int getColumnCount() {
                return getNumberOfElements() > 0 ? 1 : 0;
            }

            @Override
            public Dimension getSize() {
                int height = 0;
                if (cellHeight == -1) {
                    for (int i = 0; i < getRowCount(); i++) {
                        height += cellHeights[i];
                    }
                } else {
                    height = cellHeight * getRowCount();
                }

                Dimension result = new Dimension(getColumnCount() * cellWidth, height);
                return Utilities.addInsets(result, list.getInsets(cachedInsets));
            }

            @Override
            public boolean isSizeDependent() {
                return false;
            }
        }

        public class VerticalWrapVisibleSpecifiedLayoutStrategy extends AbstractLayoutStrategy {
            public int getRow(final int index) {
                return index % getRowCount();
            }

            public int getIndex(final int row, final int column) {
                return column * getRowCount() + row;
            }

            public int getColumn(final int index) {
                return index / getRowCount();
            }

            @Override
            public int getRowCount() {
                return list.getVisibleRowCount();
            }

            @Override
            public int getColumnCount() {
                int modelSize = list.getModel().getSize();
                return (modelSize == 0 ? 0 : modelSize - 1) / getRowCount() + 1;
            }
        }

        public class VerticalWrapVisibleUnspecifiedLayoutStrategy extends AbstractLayoutStrategy {
            private int maximumNumOfRows;

            public VerticalWrapVisibleUnspecifiedLayoutStrategy() {
                maximumNumOfRows = (list.getHeight() - insets.top - insets.bottom) / cellHeight;
                if (maximumNumOfRows == 0) {
                    maximumNumOfRows = 1;
                }
            }

            public int getIndex(final int row, final int column) {
                return column * getRowCount() + row;
            }

            public int getRow(final int index) {
                return index % getRowCount();
            }

            public int getColumn(final int index) {
                return index / getRowCount();
            }

            @Override
            public int getRowCount() {
                return maximumNumOfRows;
            }

            @Override
            public int getColumnCount() {
                int modelSize = list.getModel().getSize();
                return (modelSize == 0 ? 0 : modelSize - 1) / getRowCount() + 1;
            }
        }

        public class HorizontalWrapVisibleSpecifiedLayoutStrategy extends AbstractLayoutStrategy {
            public int getRow(final int index) {
                return index / getColumnCount();
            }

            public int getIndex(final int row, final int column) {
                return row * getColumnCount() + column;
            }

            public int getColumn(final int index) {
                return index % getColumnCount();
            }

            @Override
            public int getRowCount() {
                return list.getVisibleRowCount();
            }

            @Override
            public int getColumnCount() {
                int modelSize = list.getModel().getSize();
                return (modelSize == 0 ? 0 : modelSize - 1) / getRowCount() + 1;
            }
        }

        public class HorizontalWrapVisibleUnspecifiedLayoutStrategy extends AbstractLayoutStrategy {
            private int maximumNumOfColumns;

            public HorizontalWrapVisibleUnspecifiedLayoutStrategy() {
                maximumNumOfColumns = (list.getWidth() - insets.left - insets.right) / cellWidth;
                if (maximumNumOfColumns == 0) {
                    maximumNumOfColumns = 1;
                }
            }

            public int getIndex(final int row, final int column) {
                return row * getColumnCount() + column;
            }

            public int getRow(final int index) {
                return index / getColumnCount();
            }

            public int getColumn(final int index) {
                return index % getColumnCount();
            }

            @Override
            public int getRowCount() {
                int modelSize = list.getModel().getSize();
                return (modelSize == 0 ? 0 : modelSize - 1) / getColumnCount() + 1;
            }

            @Override
            public int getColumnCount() {
                return maximumNumOfColumns;
            }
        }

        public class OrientationStrategy implements LayoutStrategy {
            protected final LayoutStrategy strategy;

            public OrientationStrategy(final LayoutStrategy strategy) {
                this.strategy = strategy;
            }

            public int getIndex(final int row, final int column) {
                return strategy.getIndex(row, column);
            }

            public int getRowCount() {
                return strategy.getRowCount();
            }

            public int getColumnCount() {
                return strategy.getColumnCount();
            }

            public int getRow(final int index) {
                return strategy.getRow(index);
            }

            public int getColumn(final int index) {
                return strategy.getColumn(index);
            }

            public Dimension getSize() {
                return strategy.getSize();
            }

            public Rectangle getBounds(final int index) {
                return strategy.getBounds(index);
            }

            public boolean isSizeDependent() {
                return strategy.isSizeDependent();
            }
        }

        public class RightToLeftStrategy extends OrientationStrategy {
            public RightToLeftStrategy(final LayoutStrategy strategy) {
                super(strategy);
            }

            @Override
            public Rectangle getBounds(final int index) {
                Rectangle bounds = strategy.getBounds(index);
                int x = list.getWidth() - bounds.x - bounds.width;

                return new Rectangle(x, bounds.y, bounds.width, bounds.height);
            }
        }

        public ListLayouter() {
            reset();
        }

        public void setList(final JList list) {
            if (this.list != list) {
                this.list = list;
                reset();
            }
        }

        public void reinit() {
            reset();
            getLayoutStrategy();
        }

        public void reset() {
            strategy = null;
        }

        public LayoutStrategy getLayoutStrategy() {
            reinitFields();
            if (strategy == null) {
                if (list.getComponentOrientation().isLeftToRight()) {
                    strategy = getStrategy();
                } else {
                    strategy = new RightToLeftStrategy(getStrategy());
                }
            }

            return strategy;
        }

        private LayoutStrategy getStrategy() {
            LayoutStrategy result;

            if (list.getLayoutOrientation() == JList.VERTICAL) {
                result = new VerticalLayoutStategy();
            } else if (list.getLayoutOrientation() == JList.VERTICAL_WRAP) {
                if (list.getVisibleRowCount() > 0) {
                    result = new VerticalWrapVisibleSpecifiedLayoutStrategy();
                } else {
                    result = new VerticalWrapVisibleUnspecifiedLayoutStrategy();
                }
            } else if (list.getLayoutOrientation() == JList.HORIZONTAL_WRAP) {
                if (list.getVisibleRowCount() > 0) {
                    result = new HorizontalWrapVisibleSpecifiedLayoutStrategy();
                } else {
                    result = new HorizontalWrapVisibleUnspecifiedLayoutStrategy();
                }
            } else {
                throw new IllegalArgumentException(Messages.getString("swing.70")); //$NON-NLS-1$
            }

            return result;
        }

        public int getNumberOfElements() {
            return list.getModel().getSize();
        }

        public JList getList() {
            return list;
        }

        public int getNearestIndex(final Point location) {
            int result = -1;
            int distance = Integer.MAX_VALUE;
            LayoutStrategy s = getLayoutStrategy();
            for (int i = 0; i < getNumberOfElements(); i++) {
                Rectangle bounds = s.getBounds(i);
                int d = getDistance(bounds, location);
                if (distance > d) {
                    distance = d;
                    result = i;
                }
            }

            return result;
        }

        private void reinitFields() {
            insets = list.getInsets(cachedInsets);
        }

        private int getMaximumWidth() {
            if (list.getFixedCellWidth() >= 0) {
                return list.getFixedCellWidth();
            }

            int result = -1;
            for (int i = 0; i < list.getModel().getSize(); i++) {
                Dimension size = getPreferredSize(i);
                if (result < size.width) {
                    result = size.width;
                }
            }
            return result;
        }

        private int getMaximumHeight() {
            if (list.getFixedCellHeight() >= 0) {
                return list.getFixedCellHeight();
            }

            int result = -1;
            for (int i = 0; i < list.getModel().getSize(); i++) {
                Dimension size = getPreferredSize(i);
                if (result < size.height) {
                    result = size.height;
                }
            }
            return result;
        }

        private Dimension getPreferredSize(final int i) {
            if (list.getCellRenderer() == null) {
                return new Dimension(0, 0);
            }
            return list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(i), i, false, false).getPreferredSize();
        }

        private int getDistance(final Rectangle r, final Point p) {
            int dx = 0;
            if (p.x < r.x) {
                dx = r.x - p.x;
            } else if (p.x > r.x + r.width) {
                dx = p.x - r.x - r.width;
            } else {
                dx = 0;
            }

            int dy = 0;
            if (p.y < r.y) {
                dy = r.y - p.y;
            } else if (p.y > r.y + r.height) {
                dy = p.y - r.y - r.height;
            } else {
                dy = 0;
            }

            return dx + dy;
        }
    }
}
