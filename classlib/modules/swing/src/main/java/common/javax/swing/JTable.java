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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleExtendedTable;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleTable;
import javax.accessibility.AccessibleTableModelChange;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.TableUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.swing.StringConstants;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JTable</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
public class JTable extends JComponent implements TableModelListener, Scrollable,
        TableColumnModelListener, ListSelectionListener, CellEditorListener, Accessible {
    private static final long serialVersionUID = -506121678825692843L;

    public static final int AUTO_RESIZE_OFF = 0;

    public static final int AUTO_RESIZE_NEXT_COLUMN = 1;

    public static final int AUTO_RESIZE_SUBSEQUENT_COLUMNS = 2;

    public static final int AUTO_RESIZE_LAST_COLUMN = 3;

    public static final int AUTO_RESIZE_ALL_COLUMNS = 4;

    protected class AccessibleJTable extends AccessibleJComponent implements
            AccessibleSelection, ListSelectionListener, TableModelListener,
            TableColumnModelListener, CellEditorListener, PropertyChangeListener,
            AccessibleExtendedTable {
        private static final long serialVersionUID = 1L;

        protected class AccessibleJTableCell extends AccessibleContext implements Accessible,
                AccessibleComponent {
            public AccessibleJTableCell(JTable t, int r, int c, int i)
                    throws NotImplementedException {
                throw new NotImplementedException();
            }

            public AccessibleContext getAccessibleContext() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public String getAccessibleName() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public void setAccessibleName(String s) throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public String getAccessibleDescription() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public void setAccessibleDescription(String s) throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleRole getAccessibleRole() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleStateSet getAccessibleStateSet() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public Accessible getAccessibleParent() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public int getAccessibleIndexInParent() throws NotImplementedException {
                throw new NotImplementedException();
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
            public Locale getLocale() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener l)
                    throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public void removePropertyChangeListener(PropertyChangeListener l)
                    throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleAction getAccessibleAction() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleComponent getAccessibleComponent() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleSelection getAccessibleSelection() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleText getAccessibleText() throws NotImplementedException {
                throw new NotImplementedException();
            }

            @Override
            public AccessibleValue getAccessibleValue() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Color getBackground() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setBackground(Color c) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Color getForeground() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setForeground(Color c) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Cursor getCursor() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setCursor(Cursor c) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Font getFont() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setFont(Font f) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public FontMetrics getFontMetrics(Font f) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean isEnabled() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setEnabled(boolean b) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean isVisible() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setVisible(boolean b) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean isShowing() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean contains(Point p) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Point getLocationOnScreen() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Point getLocation() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setLocation(Point p) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Rectangle getBounds() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setBounds(Rectangle r) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Dimension getSize() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void setSize(Dimension d) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public Accessible getAccessibleAt(Point p) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public boolean isFocusTraversable() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void requestFocus() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void addFocusListener(FocusListener l) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public void removeFocusListener(FocusListener l) throws NotImplementedException {
                throw new NotImplementedException();
            }
        }

        protected class AccessibleJTableModelChange implements AccessibleTableModelChange {
            protected int type;

            protected int firstRow;

            protected int lastRow;

            protected int firstColumn;

            protected int lastColumn;

            protected AccessibleJTableModelChange(int type, int firstRow, int lastRow,
                    int firstColumn, int lastColumn) throws NotImplementedException {
                throw new NotImplementedException();
            }

            public int getType() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public int getFirstRow() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public int getLastRow() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public int getFirstColumn() throws NotImplementedException {
                throw new NotImplementedException();
            }

            public int getLastColumn() throws NotImplementedException {
                throw new NotImplementedException();
            }
        }

        public void propertyChange(PropertyChangeEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void tableChanged(TableModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void tableRowsInserted(TableModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void tableRowsDeleted(TableModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void columnAdded(TableColumnModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void columnRemoved(TableColumnModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void columnMoved(TableColumnModelEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void columnMarginChanged(ChangeEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void columnSelectionChanged(ListSelectionEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void editingStopped(ChangeEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void editingCanceled(ChangeEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void valueChanged(ListSelectionEvent e) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleSelection getAccessibleSelection() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleRole getAccessibleRole() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleAt(Point p) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public int getAccessibleChildrenCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public Accessible getAccessibleChild(int i) throws NotImplementedException {
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

        public int getAccessibleRow(int index) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleColumn(int index) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleIndex(int r, int c) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public AccessibleTable getAccessibleTable() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleCaption() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void setAccessibleCaption(Accessible a) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleSummary() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void setAccessibleSummary(Accessible a) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleRowCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleColumnCount() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleAt(int r, int c) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleRowExtentAt(int r, int c) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleColumnExtentAt(int r, int c) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public AccessibleTable getAccessibleRowHeader() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void setAccessibleRowHeader(AccessibleTable a) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public AccessibleTable getAccessibleColumnHeader() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void setAccessibleColumnHeader(AccessibleTable a) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleRowDescription(int r) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void setAccessibleRowDescription(int r, Accessible a)
                throws NotImplementedException {
            throw new NotImplementedException();
        }

        public Accessible getAccessibleColumnDescription(int c) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public void setAccessibleColumnDescription(int c, Accessible a)
                throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean isAccessibleSelected(int r, int c) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean isAccessibleRowSelected(int r) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public boolean isAccessibleColumnSelected(int c) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int[] getSelectedAccessibleRows() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int[] getSelectedAccessibleColumns() throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleRowAtIndex(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleColumnAtIndex(int i) throws NotImplementedException {
            throw new NotImplementedException();
        }

        public int getAccessibleIndexAt(int r, int c) throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    private class TableEditor extends DefaultCellEditor {
        private static final long serialVersionUID = -1840776223246975853L;

        public TableEditor(JCheckBox checkBox) {
            super(checkBox);
            setNormalEditorView();
        }

        public TableEditor(JTextField textField) {
            super(textField);
            setNormalEditorView();
        }

        public boolean isInputValid() {
            return verifyInput(delegate.getCellEditorValue());
        }

        protected boolean verifyInput(Object value) {
            return true;
        }

        @Override
        protected void fireEditingStopped() {
            if (!isInputValid()) {
                setErrorEditorView();
                return;
            }
            setNormalEditorView();
            super.fireEditingStopped();
        }

        @Override
        protected void fireEditingCanceled() {
            setNormalEditorView();
            super.fireEditingCanceled();
        }

        protected void setNormalEditorView() {
            ((JComponent) getComponent())
                    .setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }

        protected void setErrorEditorView() {
            ((JComponent) getComponent()).setBorder(BorderFactory.createLineBorder(Color.RED));
        }
    }

    private class ObjectEditor extends TableEditor {
        private static final long serialVersionUID = 1L;

        public ObjectEditor() {
            super(new JTextField());
        }

        @Override
        public boolean isCellEditable(EventObject event) {
            return getObjectConstructor() != null && super.isCellEditable(event);
        }

        @Override
        public Object getCellEditorValue() {
            Object value = delegate.getCellEditorValue();
            if (value == null) {
                return null;
            }
            try {
                return getObjectConstructor().newInstance(new Object[] { value.toString() });
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected boolean verifyInput(Object value) {
            if (value == null || value.toString().trim().equals("")) {
                return true;
            }
            try {
                getObjectConstructor().newInstance(new Object[] { value.toString() });
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private Constructor<?> getObjectConstructor() {
            try {
                return getColumnClass().getConstructor(new Class[] { String.class });
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        private Class<?> getColumnClass() {
            Class<?> columnClass = JTable.this.getColumnClass(getEditingColumn());
            return columnClass == Object.class ? String.class : columnClass;
        }
    }

    private class NumberEditor extends ObjectEditor {
        private static final long serialVersionUID = 1L;

        public NumberEditor() {
            ((JTextField) getComponent()).setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    private class BooleanEditor extends TableEditor {
        private static final long serialVersionUID = 1L;

        public BooleanEditor() {
            super(new JCheckBox());
            ((JCheckBox) getComponent()).setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        protected void setNormalEditorView() {
            ((JComponent) getComponent()).setBorder(null);
        }
    }

    protected TableModel dataModel;

    protected TableColumnModel columnModel;

    protected ListSelectionModel selectionModel;

    protected JTableHeader tableHeader;

    protected int rowHeight;

    protected int rowMargin;

    protected Color gridColor;

    protected boolean showHorizontalLines;

    protected boolean showVerticalLines;

    protected int autoResizeMode;

    protected boolean autoCreateColumnsFromModel = true;

    protected Dimension preferredViewportSize;

    protected boolean rowSelectionAllowed;

    protected boolean cellSelectionEnabled;

    protected transient Component editorComp;

    protected transient TableCellEditor cellEditor;

    protected transient int editingColumn = -1;

    protected transient int editingRow = -1;

    protected transient Hashtable defaultEditorsByColumnClass;

    protected transient Hashtable defaultRenderersByColumnClass;

    protected Color selectionBackground;

    protected Color selectionForeground;

    private boolean dragEnabled;

    private final Vector rowHeights = new Vector();

    private boolean surrendersFocusOnKeystroke;

    private boolean wasConsumed;

    private static final String HEADER_PROPERTY = "tableHeader";

    private static final String ROW_HEIGHT_PROPERTY = "rowHeight";

    private static final String ROW_MARGIN_PROPERTY = "rowMargin";

    private static final String GRID_COLOR_PROPERTY = "gridColor";

    private static final String SHOW_HORIZONTAL_LINES_PROPERTY = "showHorizontalLines";

    private static final String SHOW_VERTICAL_LINES_PROPERTY = "showVerticalLines";

    private static final String AUTO_RESIZE_MODE_PROPERTY = "autoResizeMode";

    private static final String AUTO_CREATE_COLUMNS_FROM_MODEL_PROPERTY = "autoCreateColumnsFromModel";

    private static final String ROW_SELECTION_ALLOWED_PROPERTY = "rowSelectionAllowed";

    private static final String COLUMN_SELECTION_ALLOWED_PROPERTY = "columnSelectionAllowed";

    private static final String CELL_SELECTION_ENABLED_PROPERTY = "cellSelectionEnabled";

    private static final String SELECTION_FOREGROUND_PROPERTY = "selectionForeground";

    private static final String SELECTION_BACKGROUND_PROPERTY = "selectionBackground";

    private static final String UI_CLASS_ID = "TableUI";

    @Deprecated
    public static JScrollPane createScrollPaneForTable(JTable table) {
        return new JScrollPane(table);
    }

    public JTable() {
        this(null, null, null);
    }

    public JTable(TableModel model) {
        this(model, null, null);
    }

    public JTable(TableModel model, TableColumnModel columnModel) {
        this(model, columnModel, null);
    }

    public JTable(TableModel model, TableColumnModel columnModel,
            ListSelectionModel selectionModel) {
        setColumnModel(columnModel != null ? columnModel : createDefaultColumnModel());
        setModel(model != null ? model : createDefaultDataModel());
        setSelectionModel(selectionModel != null ? selectionModel
                : createDefaultSelectionModel());
        initializeLocalVars();
        updateUI();
    }

    public JTable(int numRows, int numColumns) {
        this(new DefaultTableModel(numRows, numColumns));
        if (getAutoCreateColumnsFromModel()) {
            createDefaultColumnsFromModel();
        }
    }

    public JTable(Vector rowData, Vector columnNames) {
        this(new DefaultTableModel(rowData, columnNames));
        if (getAutoCreateColumnsFromModel()) {
            createDefaultColumnsFromModel();
        }
    }

    public JTable(Object[][] rowData, Object[] columnNames) {
        this(new DefaultTableModel(rowData, columnNames));
        if (getAutoCreateColumnsFromModel()) {
            createDefaultColumnsFromModel();
        }
    }

    @Override
    public void addNotify() {
        configureEnclosingScrollPane();
        super.addNotify();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        unconfigureEnclosingScrollPane();
    }

    public void setTableHeader(JTableHeader header) {
        JTableHeader oldValue = this.tableHeader;
        if (oldValue != null) {
            oldValue.setTable(null);
        }
        this.tableHeader = header;
        if (header != null) {
            this.tableHeader.setTable(this);
        }
        firePropertyChange(HEADER_PROPERTY, oldValue, header);
    }

    public JTableHeader getTableHeader() {
        return tableHeader;
    }

    public void setRowHeight(int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException(Messages.getString("swing.38")); //$NON-NLS-1$
        }
        int oldValue = this.rowHeight;
        this.rowHeight = rowHeight;
        firePropertyChange(ROW_HEIGHT_PROPERTY, oldValue, rowHeight);
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setRowHeight(int row, int height) {
        if (height <= 0) {
            throw new IllegalArgumentException(Messages.getString("swing.38")); //$NON-NLS-1$
        }
        if (rowHeights.size() <= row) {
            rowHeights.setSize(row + 1);
        }
        if (rowHeights.get(row) == null) {
            rowHeights.set(row, new MutableInteger());
        }
        if (row < getRowCount()) {
            ((MutableInteger) rowHeights.get(row)).setValue(height);
        }
    }

    public int getRowHeight(int row) {
        int result = rowHeights.size() > row && rowHeights.get(row) != null ? ((MutableInteger) rowHeights
                .get(row)).getValue()
                : -1;
        return result != -1 ? result : getRowHeight();
    }

    public void setRowMargin(int margin) {
        int oldValue = this.rowMargin;
        this.rowMargin = margin;
        firePropertyChange(ROW_MARGIN_PROPERTY, oldValue, margin);
    }

    public int getRowMargin() {
        return rowMargin;
    }

    public void setIntercellSpacing(Dimension spacing) {
        setRowMargin(spacing.height);
        columnModel.setColumnMargin(spacing.width);
    }

    public Dimension getIntercellSpacing() {
        return new Dimension(columnModel.getColumnMargin(), getRowMargin());
    }

    public void setGridColor(Color color) {
        Color oldValue = this.gridColor;
        this.gridColor = color;
        firePropertyChange(GRID_COLOR_PROPERTY, oldValue, color);
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setShowGrid(boolean show) {
        setShowHorizontalLines(show);
        setShowVerticalLines(show);
    }

    public void setShowHorizontalLines(boolean show) {
        boolean oldValue = this.showHorizontalLines;
        this.showHorizontalLines = show;
        firePropertyChange(SHOW_HORIZONTAL_LINES_PROPERTY, oldValue, show);
    }

    public boolean getShowHorizontalLines() {
        return showHorizontalLines;
    }

    public void setShowVerticalLines(boolean show) {
        boolean oldValue = this.showVerticalLines;
        this.showVerticalLines = show;
        firePropertyChange(SHOW_VERTICAL_LINES_PROPERTY, oldValue, show);
    }

    public boolean getShowVerticalLines() {
        return showVerticalLines;
    }

    public void setAutoResizeMode(int mode) {
        if (mode < 0 || mode > 4) {
            return;
        }
        int oldValue = this.autoResizeMode;
        this.autoResizeMode = mode;
        firePropertyChange(AUTO_RESIZE_MODE_PROPERTY, oldValue, mode);
    }

    public int getAutoResizeMode() {
        return autoResizeMode;
    }

    public void setAutoCreateColumnsFromModel(boolean autoCreate) {
        boolean oldValue = autoCreateColumnsFromModel;
        autoCreateColumnsFromModel = autoCreate;
        if (getAutoCreateColumnsFromModel()) {
            createDefaultColumnsFromModel();
        }
        firePropertyChange(AUTO_CREATE_COLUMNS_FROM_MODEL_PROPERTY, oldValue, autoCreate);
    }

    public boolean getAutoCreateColumnsFromModel() {
        return autoCreateColumnsFromModel;
    }

    public void createDefaultColumnsFromModel() {
        int columnCount = columnModel.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            columnModel.removeColumn(columnModel.getColumn(0));
        }
        for (int i = 0; i < getModel().getColumnCount(); i++) {
            TableColumn column = new TableColumn(i);
            column.setHeaderValue(getModel().getColumnName(i));
            columnModel.addColumn(column);
        }
    }

    public void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
        if (renderer != null) {
            defaultRenderersByColumnClass.put(columnClass, renderer);
        } else {
            defaultRenderersByColumnClass.remove(columnClass);
        }
    }

    public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
        return (TableCellRenderer) getClosestClass(columnClass, defaultRenderersByColumnClass);
    }

    public void setDefaultEditor(Class<?> columnClass, TableCellEditor editor) {
        if (editor != null) {
            defaultEditorsByColumnClass.put(columnClass, editor);
        } else {
            defaultEditorsByColumnClass.remove(columnClass);
        }
    }

    public TableCellEditor getDefaultEditor(Class<?> columnClass) {
        return (TableCellEditor) getClosestClass(columnClass, defaultEditorsByColumnClass);
    }

    public void setDragEnabled(boolean enabled) {
        if (enabled && GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        dragEnabled = enabled;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    public void setSelectionMode(int mode) {
        getSelectionModel().setSelectionMode(mode);
        getColumnModel().getSelectionModel().setSelectionMode(mode);
    }

    public void setRowSelectionAllowed(boolean allowed) {
        boolean oldValue = rowSelectionAllowed;
        rowSelectionAllowed = allowed;
        firePropertyChange(ROW_SELECTION_ALLOWED_PROPERTY, oldValue, allowed);
    }

    public boolean getRowSelectionAllowed() {
        return rowSelectionAllowed;
    }

    public void setColumnSelectionAllowed(boolean allowed) {
        boolean oldValue = getColumnModel().getColumnSelectionAllowed();
        getColumnModel().setColumnSelectionAllowed(allowed);
        firePropertyChange(COLUMN_SELECTION_ALLOWED_PROPERTY, oldValue, allowed);
    }

    public boolean getColumnSelectionAllowed() {
        return getColumnModel().getColumnSelectionAllowed();
    }

    public void setCellSelectionEnabled(boolean enabled) {
        boolean oldValue = cellSelectionEnabled;
        cellSelectionEnabled = enabled;
        setRowSelectionAllowed(enabled);
        setColumnSelectionAllowed(enabled);
        firePropertyChange(CELL_SELECTION_ENABLED_PROPERTY, oldValue, enabled);
    }

    public boolean getCellSelectionEnabled() {
        return getRowSelectionAllowed() && getColumnSelectionAllowed();
    }

    public void selectAll() {
        int rowLead = getSelectionModel().getLeadSelectionIndex();
        int rowAnchor = getSelectionModel().getAnchorSelectionIndex();
        int columnLead = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        int columnAnchor = getColumnModel().getSelectionModel().getAnchorSelectionIndex();
        getSelectionModel().setValueIsAdjusting(true);
        getColumnModel().getSelectionModel().setValueIsAdjusting(true);
        setRowSelectionInterval(0, getRowCount() - 1);
        getSelectionModel().addSelectionInterval(rowAnchor, rowLead);
        setColumnSelectionInterval(0, getColumnCount() - 1);
        getColumnModel().getSelectionModel().addSelectionInterval(columnAnchor, columnLead);
        getSelectionModel().setValueIsAdjusting(false);
        getColumnModel().getSelectionModel().setValueIsAdjusting(false);
    }

    public void clearSelection() {
        getSelectionModel().clearSelection();
        getColumnModel().getSelectionModel().clearSelection();
    }

    public void setRowSelectionInterval(int start, int end) {
        checkSelectionInterval(start, end, getRowCount());
        getSelectionModel().setSelectionInterval(start, end);
    }

    public void setColumnSelectionInterval(int start, int end) {
        checkSelectionInterval(start, end, getColumnCount());
        getColumnModel().getSelectionModel().setSelectionInterval(start, end);
    }

    public void addRowSelectionInterval(int start, int end) {
        checkSelectionInterval(start, end, getRowCount());
        getSelectionModel().addSelectionInterval(start, end);
    }

    public void addColumnSelectionInterval(int start, int end) {
        checkSelectionInterval(start, end, getColumnCount());
        getColumnModel().getSelectionModel().addSelectionInterval(start, end);
    }

    public void removeRowSelectionInterval(int start, int end) {
        checkSelectionInterval(start, end, getRowCount());
        getSelectionModel().removeSelectionInterval(start, end);
    }

    public void removeColumnSelectionInterval(int start, int end) {
        checkSelectionInterval(start, end, getColumnCount());
        getColumnModel().getSelectionModel().removeSelectionInterval(start, end);
    }

    public int getSelectedRow() {
        return getSelectionModel().getMinSelectionIndex();
    }

    public int getSelectedColumn() {
        return getColumnModel().getSelectionModel().getMinSelectionIndex();
    }

    public int[] getSelectedRows() {
        return getSelectedIndices(getSelectionModel());
    }

    public int[] getSelectedColumns() {
        return getSelectedIndices(getColumnModel().getSelectionModel());
    }

    public int getSelectedRowCount() {
        return getSelectedCount(getSelectionModel());
    }

    public int getSelectedColumnCount() {
        return getSelectedCount(getColumnModel().getSelectionModel());
    }

    public boolean isRowSelected(int row) {
        return getSelectionModel().isSelectedIndex(row);
    }

    public boolean isColumnSelected(int column) {
        return getColumnModel().getSelectionModel().isSelectedIndex(column);
    }

    public boolean isCellSelected(int row, int column) {
        return getRowSelectionAllowed() && isRowSelected(row)
                && (!getColumnSelectionAllowed() || isColumnSelected(column))
                || getColumnSelectionAllowed() && isColumnSelected(column)
                && (!getRowSelectionAllowed() || isRowSelected(row));
    }

    public void changeSelection(int row, int column, boolean toggle, boolean extend) {
        if (!toggle && !extend) {
            setRowSelectionInterval(row, row);
            setColumnSelectionInterval(column, column);
        } else if (!toggle && extend) {
            setRowSelectionInterval(getSelectionModel().getAnchorSelectionIndex(), row);
            setColumnSelectionInterval(getColumnModel().getSelectionModel()
                    .getAnchorSelectionIndex(), column);
        } else if (toggle && !extend) {
            if (isCellSelected(row, column)) {
                removeRowSelectionInterval(row, row);
                removeColumnSelectionInterval(column, column);
            } else {
                addRowSelectionInterval(row, row);
                addColumnSelectionInterval(column, column);
            }
        } else {
            getSelectionModel().setAnchorSelectionIndex(row);
            getColumnModel().getSelectionModel().setAnchorSelectionIndex(column);
        }
        int currentRow = getSelectionModel().getLeadSelectionIndex();
        int currentColumn = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (currentRow != -1 && currentColumn != -1) {
            scrollRectToVisible(getCellRect(currentRow, currentColumn, true));
        }
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    public void setSelectionForeground(Color fg) {
        Color oldValue = selectionForeground;
        selectionForeground = fg;
        firePropertyChange(SELECTION_FOREGROUND_PROPERTY, oldValue, fg);
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionBackground(Color bg) {
        Color oldValue = selectionBackground;
        selectionBackground = bg;
        firePropertyChange(SELECTION_BACKGROUND_PROPERTY, oldValue, bg);
    }

    public TableColumn getColumn(Object identifier) {
        int index = getColumnModel().getColumnIndex(identifier);
        if (index == -1) {
            throw new IllegalArgumentException(Messages.getString("swing.39")); //$NON-NLS-1$
        }
        return getColumnModel().getColumn(index);
    }

    public int convertColumnIndexToModel(int viewIndex) {
        if (viewIndex < 0) {
            return viewIndex;
        }
        return getColumnModel().getColumn(viewIndex).getModelIndex();
    }

    public int convertColumnIndexToView(int modelIndex) {
        if (modelIndex < 0) {
            return modelIndex;
        }
        TableColumnModel columnModel = getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            if (columnModel.getColumn(i).getModelIndex() == modelIndex) {
                return i;
            }
        }
        return -1;
    }

    public int getRowCount() {
        return getModel().getRowCount();
    }

    public int getColumnCount() {
        return getColumnModel().getColumnCount();
    }

    public String getColumnName(int viewIndex) {
        return getModel().getColumnName(convertColumnIndexToModel(viewIndex));
    }

    public Class<?> getColumnClass(int viewIndex) {
        return getModel().getColumnClass(convertColumnIndexToModel(viewIndex));
    }

    public Object getValueAt(int row, int viewColumn) {
        return getModel().getValueAt(row, convertColumnIndexToModel(viewColumn));
    }

    public void setValueAt(Object value, int row, int viewColumn) {
        getModel().setValueAt(value, row, convertColumnIndexToModel(viewColumn));
    }

    public boolean isCellEditable(int row, int viewColumn) {
        return getModel().isCellEditable(row, convertColumnIndexToModel(viewColumn));
    }

    public void addColumn(TableColumn column) {
        if (column.getHeaderValue() == null) {
            column.setHeaderValue(getModel().getColumnName(column.getModelIndex()));
        }
        getColumnModel().addColumn(column);
    }

    public void removeColumn(TableColumn column) {
        getColumnModel().removeColumn(column);
    }

    public void moveColumn(int viewColumn, int targetViewColumn) {
        getColumnModel().moveColumn(viewColumn, targetViewColumn);
    }

    public int columnAtPoint(Point p) {
        int x = p.x;
        if( !getComponentOrientation().isLeftToRight() ) {
          x = getWidth() - x;
        }
        return getColumnModel().getColumnIndexAtX(x);
    }

    public int rowAtPoint(Point p) {
        int previousWidth = 0;
        for (int i = 0; i < getRowCount(); i++) {
            int height = getRowHeight(i);
            if (p.y >= previousWidth && p.y < previousWidth + height) {
                return i;
            }
            previousWidth += height;
        }
        return -1;
    }

    public Rectangle getCellRect(int row, int viewColumn, boolean includeSpacing) {
        Rectangle result = new Rectangle();
        boolean useSpacing = includeSpacing;
        if (row >= 0 && row < getRowCount()) {
            for (int i = 0; i < row; i++) {
                result.y += getRowHeight(i);
            }
            result.height = getRowHeight(row);
        } else {
            useSpacing = true;
        }
        if (viewColumn >= 0 && viewColumn < getColumnCount()) {
            TableColumnModel columnModel = getColumnModel();
            if (getComponentOrientation().isLeftToRight()) {
                for (int i = 0; i < viewColumn; i++) {
                    result.x += columnModel.getColumn(i).getWidth();
                }
            } else {
                for (int i = 0; i < columnModel.getColumnCount(); i++) {
                    result.x += columnModel.getColumn(i).getWidth();
                }
                for (int i = 0; i <= viewColumn; i++) {
                    result.x -= columnModel.getColumn(i).getWidth();
                }
            }
            result.width = columnModel.getColumn(viewColumn).getWidth();
        } else {
            useSpacing = true;
        }
        if (!useSpacing) {
            Dimension spacing = getIntercellSpacing();
            result.x += spacing.width / 2;
            result.width -= spacing.width;
            result.y += spacing.height / 2;
            result.height -= spacing.height;
        }
        return result;
    }

    @Deprecated
    public void sizeColumnsToFit(boolean lastColumnOnly) {
        int previousSetting = getAutoResizeMode();
        if (lastColumnOnly) {
            setAutoResizeMode(AUTO_RESIZE_LAST_COLUMN);
        } else {
            setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
        }
        doLayout();
        setAutoResizeMode(previousSetting);
    }

    public void sizeColumnsToFit(int resizingColumn) {
    }

    @Override
    public String getToolTipText(MouseEvent me) {
        int row = rowAtPoint(me.getPoint());
        int column = columnAtPoint(me.getPoint());
        if (row == -1 || column == -1) {
            return null;
        }
        TableCellRenderer renderer = getCellRenderer(row, column);
        if (renderer == null) {
            return null;
        }
        Component renderingComponent = renderer.getTableCellRendererComponent(this, getValueAt(
                row, column), isCellSelected(row, column), false, row, column);
        return renderer instanceof JComponent ? ((JComponent) renderingComponent)
                .getToolTipText() : null;
    }

    public void setSurrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke) {
        this.surrendersFocusOnKeystroke = surrendersFocusOnKeystroke;
    }

    public boolean getSurrendersFocusOnKeystroke() {
        return surrendersFocusOnKeystroke;
    }

    public boolean editCellAt(int row, int column) {
        return editCellAt(row, column, null);
    }

    public boolean editCellAt(int row, int viewColumn, EventObject e) {
        if (isEditing()) {
            getCellEditor().stopCellEditing();
            if (isEditing()) {
                return false;
            }
        }
        if (row >= getRowCount() || viewColumn > getColumnModel().getColumnCount()) {
            return false;
        }
        if (!isCellEditable(row, viewColumn)) {
            return false;
        }
        TableCellEditor editor = getCellEditor(row, viewColumn);
        if (editor == null) {
            return false;
        }
        editingColumn = viewColumn;
        if (!editor.isCellEditable(e)) {
            editingColumn = -1;
            return false;
        }
        setCellEditor(editor);
        setEditingRow(row);
        setEditingColumn(viewColumn);
        editorComp = prepareEditor(getCellEditor(), row, viewColumn);
        getCellEditor().addCellEditorListener(this);
        add(editorComp);
        editorComp.setBounds(getCellRect(row, viewColumn, false));
        return true;
    }

    public boolean isEditing() {
        return getCellEditor() != null;
    }

    public Component getEditorComponent() {
        return editorComp;
    }

    public int getEditingRow() {
        return editingRow;
    }

    public void setEditingRow(int row) {
        editingRow = row;
    }

    public int getEditingColumn() {
        return editingColumn;
    }

    public void setEditingColumn(int column) {
        editingColumn = column;
    }

    public TableUI getUI() {
        return (TableUI) ui;
    }

    public void setUI(TableUI ui) {
        super.setUI(ui);
    }

    @Override
    public void updateUI() {
        setUI((TableUI) UIManager.getUI(this));
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public void setModel(TableModel model) {
        if (model == null) {
            throw new IllegalArgumentException(Messages.getString("swing.3A")); //$NON-NLS-1$
        }
        TableModel oldValue = dataModel;
        if (oldValue != null) {
            oldValue.removeTableModelListener(this);
        }
        dataModel = model;
        dataModel.addTableModelListener(this);
        firePropertyChange(StringConstants.MODEL_PROPERTY_CHANGED, oldValue, model);
        if (oldValue != dataModel) {
            tableChanged(new TableModelEvent(dataModel, TableModelEvent.HEADER_ROW,
                    TableModelEvent.HEADER_ROW, TableModelEvent.ALL_COLUMNS,
                    TableModelEvent.UPDATE));
        }
    }

    public TableModel getModel() {
        return dataModel;
    }

    public void setColumnModel(TableColumnModel model) {
        if (model == null) {
            throw new IllegalArgumentException(Messages.getString("swing.3B")); //$NON-NLS-1$
        }
        TableColumnModel oldValue = columnModel;
        if (oldValue != null) {
            oldValue.removeColumnModelListener(this);
            oldValue.removeColumnModelListener(getTableHeader());
        }
        columnModel = model;
        JTableHeader header = getTableHeader();
        if (header != null) {
            columnModel.addColumnModelListener(header);
        }
        columnModel.addColumnModelListener(this);
    }

    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    public void setSelectionModel(ListSelectionModel model) {
        if (model == null) {
            throw new IllegalArgumentException(Messages.getString("swing.17")); //$NON-NLS-1$
        }
        ListSelectionModel oldValue = selectionModel;
        if (oldValue != null) {
            oldValue.removeListSelectionListener(this);
        }
        selectionModel = model;
        selectionModel.addListSelectionListener(this);
        alignSelectionModelToRows();
        firePropertyChange(StringConstants.SELECTION_MODEL_PROPERTY, oldValue, model);
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void tableChanged(TableModelEvent e) {
        if (e.getType() == TableModelEvent.UPDATE
                && e.getFirstRow() == TableModelEvent.HEADER_ROW
                && e.getLastRow() == TableModelEvent.HEADER_ROW) {
            if (getAutoCreateColumnsFromModel()) {
                createDefaultColumnsFromModel();
            }
        }
        if (getSelectionModel() != null) {
            updateSelectionModel(getSelectionModel(), e);
        }
        if (getColumnModel().getSelectionModel() != null) {
            updateColumnSelectionModel(getColumnModel().getSelectionModel(), e);
        }
        revalidate();
        repaint();
    }

    public void columnAdded(TableColumnModelEvent e) {
        if (isEditing()) {
            getCellEditor().cancelCellEditing();
        }
        resizeAndRepaint();
    }

    public void columnRemoved(TableColumnModelEvent e) {
        if (isEditing()) {
            getCellEditor().cancelCellEditing();
        }
        resizeAndRepaint();
    }

    public void columnMoved(TableColumnModelEvent e) {
        if (isEditing()) {
            getCellEditor().cancelCellEditing();
        }
        repaint();
    }

    public void columnMarginChanged(ChangeEvent e) {
        if (isEditing()) {
            getCellEditor().cancelCellEditing();
        }
        resizeAndRepaint();
    }

    public void columnSelectionChanged(ListSelectionEvent e) {
        repaint();
    }

    public void valueChanged(ListSelectionEvent e) {
        repaint();
    }

    public void editingStopped(ChangeEvent e) {
        if (isEditing()) {
            setValueAt(getCellEditor().getCellEditorValue(), getEditingRow(),
                    getEditingColumn());
            cleanUpAfterEditing();
        }
    }

    public void editingCanceled(ChangeEvent e) {
        if (isEditing()) {
            cleanUpAfterEditing();
        }
    }

    public void setPreferredScrollableViewportSize(Dimension size) {
        preferredViewportSize = size;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return preferredViewportSize;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return 100;
        }
        return getRowHeight();
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width;
        }
        return visibleRect.height;
    }

    public boolean getScrollableTracksViewportWidth() {
        return getAutoResizeMode() != AUTO_RESIZE_OFF;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void setCellEditor(TableCellEditor editor) {
        cellEditor = editor;
    }

    public TableCellEditor getCellEditor() {
        return cellEditor;
    }

    public TableCellRenderer getCellRenderer(int row, int viewColumn) {
        TableCellRenderer result = getColumnModel().getColumn(viewColumn).getCellRenderer();
        if (result == null) {
            result = getDefaultRenderer(getColumnClass(viewColumn));
        }
        return result;
    }

    public Component prepareRenderer(TableCellRenderer renderer, int row, int viewColumn) {
        boolean hasFocus = false;
        return renderer.getTableCellRendererComponent(this, getValueAt(row, viewColumn),
                isCellSelected(row, viewColumn), hasFocus, row, viewColumn);
    }

    public TableCellEditor getCellEditor(int row, int viewColumn) {
        TableCellEditor result = getColumnModel().getColumn(viewColumn).getCellEditor();
        if (result == null) {
            result = getDefaultEditor(getColumnClass(viewColumn));
        }
        return result;
    }

    public Component prepareEditor(TableCellEditor editor, int row, int viewColumn) {
        return editor.getTableCellEditorComponent(this, getValueAt(row, viewColumn),
                isCellSelected(row, viewColumn), row, viewColumn);
    }

    public void removeEditor() {
        if (!isEditing()) {
            return;
        }
        setCellEditor(null);
        remove(getEditorComponent());
        editorComp = null;
        setEditingRow(-1);
        setEditingColumn(-1);
    }

    @Override
    public void doLayout() {
        if (getAutoResizeMode() == AUTO_RESIZE_OFF) {
            return;
        }
        TableColumn resizingColumn = (getTableHeader() == null) ? null : getTableHeader().getResizingColumn();
        if (resizingColumn == null) {
            ResizableElements resizable = new ResizableElements() {
                public int getElementsCount() {
                    return getColumnCount();
                }

                public TableColumn getElement(int i) {
                    return getColumnModel().getColumn(i);
                }
            };
            adjustColumns(getWidth(), resizable);
        } else {
            int resizingColIndex = getColumnModel().getColumnIndex(
                    resizingColumn.getIdentifier());
            if (resizingColIndex + 1 == getColumnCount()) {
                int remWidth = getWidth();
                for (int i = 0; i < getColumnCount() - 1; i++) {
                    remWidth -= getColumnModel().getColumn(i).getWidth();
                }
                resizingColumn.setWidth(remWidth);
                return;
            }
            if (getAutoResizeMode() == AUTO_RESIZE_NEXT_COLUMN) {
                autoResizeNextColumn(resizingColumn);
                return;
            }
            if (getAutoResizeMode() == AUTO_RESIZE_LAST_COLUMN) {
                autoResizeLastColumn(resizingColumn);
                return;
            }
            if (getAutoResizeMode() == AUTO_RESIZE_SUBSEQUENT_COLUMNS) {
                autoResizeSubsequentColumns(resizingColumn);
                return;
            }
            if (getAutoResizeMode() == AUTO_RESIZE_ALL_COLUMNS) {
                ResizableElements resizable = new ResizableElements() {
                    public int getElementsCount() {
                        return getColumnCount();
                    }

                    public TableColumn getElement(int i) {
                        return getColumnModel().getColumn(i);
                    }
                };
                adjustColumns(getWidth(), resizable);
            }
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJTable();
        }
        return accessibleContext;
    }

    public boolean print() throws PrinterException, NotImplementedException {
        throw new NotImplementedException();
    }

    public boolean print(PrintMode printMode) throws PrinterException, NotImplementedException {
        throw new NotImplementedException();
    }

    public boolean print(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat) throws PrinterException, NotImplementedException {
        throw new NotImplementedException();
    }

    public boolean print(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat, boolean showPrintDialog, PrintRequestAttributeSet attr,
            boolean interactive) throws PrinterException, HeadlessException,
            NotImplementedException {
        throw new NotImplementedException();
    }

    public Printable getPrintable(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat) throws NotImplementedException {
        throw new NotImplementedException();
    }

    public static enum PrintMode {
        NORMAL, FIT_WIDTH
    }

    protected void initializeLocalVars() {
        dragEnabled = false;
        setRowMargin(1);
        setTableHeader(createDefaultTableHeader());
        setRowHeight(16);
        setShowHorizontalLines(true);
        setShowVerticalLines(true);
        setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setPreferredScrollableViewportSize(new Dimension(450, 400));
        setRowSelectionAllowed(true);
        createDefaultRenderers();
        createDefaultEditors();
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    protected void configureEnclosingScrollPane() {
        JScrollPane enclosingScrollPane = getEnclosingScrollPane();
        if (enclosingScrollPane == null) {
            return;
        }
        enclosingScrollPane.setColumnHeaderView(getTableHeader());
        enclosingScrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
    }

    protected void unconfigureEnclosingScrollPane() {
        JScrollPane enclosingScrollPane = getEnclosingScrollPane();
        if (enclosingScrollPane == null) {
            return;
        }
        enclosingScrollPane.setColumnHeaderView(null);
        enclosingScrollPane.setBorder(null);
    }

    protected void createDefaultRenderers() {
        defaultRenderersByColumnClass = new Hashtable();
        defaultRenderersByColumnClass.put(Date.class, new DateTableCellRenderer());
        defaultRenderersByColumnClass.put(ImageIcon.class, new IconTableCellRenderer());
        defaultRenderersByColumnClass.put(Icon.class, new IconTableCellRenderer());
        defaultRenderersByColumnClass.put(Float.class, new NumberTableCellRenderer());
        defaultRenderersByColumnClass.put(Double.class, new NumberTableCellRenderer());
        defaultRenderersByColumnClass.put(Number.class, new NumberTableCellRenderer());
        defaultRenderersByColumnClass.put(Boolean.class, new BooleanTableCellRenderer());
        defaultRenderersByColumnClass.put(Object.class, new DefaultTableCellRenderer());
    }

    protected void createDefaultEditors() {
        defaultEditorsByColumnClass = new Hashtable();
        defaultEditorsByColumnClass.put(Number.class, new NumberEditor());
        defaultEditorsByColumnClass.put(Boolean.class, new BooleanEditor());
        defaultEditorsByColumnClass.put(Object.class, new ObjectEditor());
    }

    protected TableModel createDefaultDataModel() {
        return new DefaultTableModel();
    }

    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel();
    }

    protected ListSelectionModel createDefaultSelectionModel() {
        return new DefaultListSelectionModel();
    }

    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(getColumnModel());
    }

    protected void resizeAndRepaint() {
        revalidate();
        repaint();
    }

    @Override
    protected void processKeyEvent(KeyEvent event) {
        super.processKeyEvent(event);
        if (event.isConsumed()) {
            wasConsumed = true;
        }
        if (event.getID() == KeyEvent.KEY_RELEASED) {
            wasConsumed = false;
            return;
        }
        if (wasConsumed) {
            return;
        }
        if (event.getKeyCode() == KeyEvent.VK_SHIFT || event.getKeyCode() == KeyEvent.VK_ALT
                || event.getKeyCode() == KeyEvent.VK_ALT_GRAPH
                || event.getKeyCode() == KeyEvent.VK_CONTROL
                || event.getKeyCode() == KeyEvent.VK_PRINTSCREEN
                || event.getKeyCode() == KeyEvent.VK_CAPS_LOCK
                || event.getKeyCode() == KeyEvent.VK_NUM_LOCK
                || event.getKeyCode() == KeyEvent.VK_SCROLL_LOCK || event.isAltDown()
                || event.isControlDown()) {
            return;
        }
        if (!isEditing()) {
            int currentRow = getSelectionModel().getLeadSelectionIndex();
            int currentColumn = getColumnModel().getSelectionModel().getLeadSelectionIndex();
            if (currentRow == -1 || currentColumn == -1) {
                return;
            }
            if (!editCellAt(currentRow, currentColumn, event)) {
                return;
            }
            if (isEditing() && getSurrendersFocusOnKeystroke()) {
                getEditorComponent().requestFocus();
            }
        }
        if (isEditing() && getEditorComponent() instanceof JComponent) {
            KeyEvent editorEvent = new KeyEvent(getEditorComponent(), event.getID(), event
                    .getWhen(), event.getModifiers(), event.getKeyCode(), event.getKeyChar(),
                    event.getKeyLocation());
            ((JComponent) getEditorComponent()).processKeyEvent(editorEvent);
        }
    }

    private JScrollPane getEnclosingScrollPane() {
        if (getParent() instanceof JViewport && ((JViewport) getParent()).getView() == this) {
            if (getParent().getParent() instanceof JScrollPane) {
                return (JScrollPane) getParent().getParent();
            }
        }
        return null;
    }

    private Object getClosestClass(Class<?> columnClass, Hashtable classes) {
        Class<?> currentClass = columnClass;
        do {
            Object value = classes.get(currentClass);
            if (value != null) {
                return value;
            }
            currentClass = currentClass.getSuperclass();
        } while (currentClass != null);
        return null;
    }

    private void checkSelectionInterval(int start, int end, int bound) {
        if (start < 0 || end < 0 || start >= bound || end >= bound) {
            throw new IllegalArgumentException(Messages.getString("swing.31", (bound - 1))); //$NON-NLS-1$
        }
    }

    private int[] getSelectedIndices(ListSelectionModel selModel) {
        int size = getSelectedCount(selModel);
        int[] result = new int[size];
        if (size == 0) {
            return result;
        }
        int count = 0;
        for (int i = selModel.getMinSelectionIndex(); i <= selModel.getMaxSelectionIndex(); i++) {
            if (selModel.isSelectedIndex(i)) {
                result[count++] = i;
            }
        }
        return result;
    }

    private int getSelectedCount(ListSelectionModel selModel) {
        if (selModel.isSelectionEmpty()) {
            return 0;
        }
        int result = 0;
        for (int i = selModel.getMinSelectionIndex(); i <= selModel.getMaxSelectionIndex(); i++) {
            if (selModel.isSelectedIndex(i)) {
                result++;
            }
        }
        return result;
    }

    private void updateSelectionModel(ListSelectionModel model, TableModelEvent e) {
        if (e.getType() == TableModelEvent.INSERT) {
            model.insertIndexInterval(e.getFirstRow(), e.getLastRow() - e.getFirstRow() + 1,
                    true);
            alignSelectionModelToRows();
        }
        if (e.getType() == TableModelEvent.DELETE) {
            model.removeIndexInterval(e.getFirstRow(), e.getLastRow());
            alignSelectionModelToRows();
        }
        if (e.getType() == TableModelEvent.UPDATE
                && e.getColumn() == TableModelEvent.ALL_COLUMNS) {
            model.clearSelection();
        }
    }

    private void updateColumnSelectionModel(ListSelectionModel model, TableModelEvent e) {
        if (e.getType() == TableModelEvent.UPDATE
                && e.getFirstRow() == TableModelEvent.HEADER_ROW
                && e.getLastRow() == TableModelEvent.HEADER_ROW) {
            model.setAnchorSelectionIndex(-1);
            model.setLeadSelectionIndex(0);
            model.clearSelection();
        }
    }

    private void autoResizeSubsequentColumns(TableColumn resizingColumn) {
        final int resizingColIndex = getColumnModel().getColumnIndex(
                resizingColumn.getIdentifier());
        ResizableElements resizable = new ResizableElements() {
            public int getElementsCount() {
                return getColumnCount() - (resizingColIndex + 1);
            }

            public TableColumn getElement(int i) {
                return getColumnModel().getColumn(i + (resizingColIndex + 1));
            }
        };
        int width = 0;
        for (int i = 0; i <= resizingColIndex; i++) {
            width += getColumnModel().getColumn(i).getWidth();
        }
        int minSize = 0;
        for (int i = resizingColIndex + 1; i < getColumnCount(); i++) {
            minSize += getColumnModel().getColumn(i).getMinWidth();
        }
        if (getWidth() - width > minSize) {
            adjustColumns(getWidth() - width, resizable);
        } else {
            width = 0;
            for (int i = 0; i < resizingColIndex; i++) {
                width += getColumnModel().getColumn(i).getWidth();
            }
            for (int i = resizingColIndex + 1; i < getColumnCount(); i++) {
                getColumnModel().getColumn(i).setWidth(
                        getColumnModel().getColumn(i).getMinWidth());
            }
            resizingColumn.setWidth(getWidth() - width - minSize);
        }
    }

    private void autoResizeNextColumn(TableColumn resizingColumn) {
        int resColIndex = getColumnModel().getColumnIndex(resizingColumn.getIdentifier());
        TableColumn nextColumn = getColumnModel().getColumn(resColIndex + 1);
        int colsSumWidth = getWidth();
        for (int i = 0; i < getColumnCount(); i++) {
            if (i != resColIndex && i != resColIndex + 1) {
                colsSumWidth -= getColumnModel().getColumn(i).getWidth();
            }
        }
        if (resizingColumn.getWidth() + nextColumn.getMinWidth() < colsSumWidth) {
            nextColumn.setWidth(colsSumWidth - resizingColumn.getWidth());
        } else {
            int resizingColWidth = colsSumWidth - nextColumn.getMinWidth();
            resizingColumn.setWidth(resizingColWidth);
            nextColumn.setWidth(nextColumn.getMinWidth());
        }
    }

    private void autoResizeLastColumn(TableColumn resizingColumn) {
        int resColIndex = getColumnModel().getColumnIndex(resizingColumn.getIdentifier());
        TableColumn lastColumn = getColumnModel().getColumn(getColumnCount() - 1);
        int colsSumWidth = getWidth();
        for (int i = 0; i < getColumnCount(); i++) {
            if (i != resColIndex && i != getColumnCount() - 1) {
                colsSumWidth -= getColumnModel().getColumn(i).getWidth();
            }
        }
        if (resizingColumn.getWidth() + lastColumn.getMinWidth() < colsSumWidth) {
            lastColumn.setWidth(colsSumWidth - resizingColumn.getWidth());
        } else {
            int resizingColWidth = colsSumWidth - lastColumn.getMinWidth();
            resizingColumn.setWidth(resizingColWidth);
            lastColumn.setWidth(lastColumn.getMinWidth());
        }
    }

    private void adjustColumns(long targetSize, ResizableElements resizable) {
        if (resizable.getElementsCount() == 0) {
            return;
        }
        long minColsWidth = 0;
        long maxColsWidth = 0;
        long colsWidth = 0;
        for (int i = 0; i < resizable.getElementsCount(); i++) {
            TableColumn column = resizable.getElement(i);
            minColsWidth += column.getMinWidth();
            maxColsWidth += column.getMaxWidth();
            colsWidth += column.getPreferredWidth();
        }
        long colsDelta = targetSize - colsWidth;
        int[] newWidthes = new int[resizable.getElementsCount()];
        int newTableWidth = 0;
        for (int i = 0; i < resizable.getElementsCount(); i++) {
            TableColumn column = resizable.getElement(i);
            int maxWidth = column.getMaxWidth();
            int minWidth = column.getMinWidth();
            int curWidth = column.getPreferredWidth();
            double multiplier = (colsDelta > 0) ? (double) (maxWidth - curWidth)
                    / (double) (maxColsWidth - colsWidth) : (double) (curWidth - minWidth)
                    / (double) (colsWidth - minColsWidth);
            int delta = (int) (colsDelta * multiplier);
            int newWidth = curWidth + delta;
            if (newWidth > maxWidth) {
                newWidth = maxWidth;
            }
            if (newWidth < minWidth) {
                newWidth = minWidth;
            }
            newWidthes[i] = newWidth;
            newTableWidth += newWidthes[i];
        }
        int diff = (int) targetSize - newTableWidth;
        int absDiff = Math.abs(diff);
        while (absDiff != 0) {
            if (diff > 0) {
                adjustNewWidthesToIncreaseSize(newWidthes);
            } else {
                adjustNewWidthesToDecreaseSize(newWidthes);
            }
            absDiff--;
        }
        for (int i = 0; i < resizable.getElementsCount(); i++) {
            resizable.getElement(i).setWidth(newWidthes[i]);
        }
    }

    private void cleanUpAfterEditing() {
        getCellEditor().removeCellEditorListener(this);
        removeEditor();
        repaint(getCellRect(getEditingRow(), getEditingColumn(), false));
        requestFocus();
    }

    private void adjustNewWidthesToDecreaseSize(int[] widths) {
        int result = widths.length - 1;
        int max = widths[widths.length - 1];
        for (int i = widths.length - 1; i >= 0; i--) {
            if (widths[i] > max) {
                result = i;
            }
        }
        widths[result]--;
    }

    private void adjustNewWidthesToIncreaseSize(int[] widths) {
        int result = widths.length - 1;
        int min = widths[widths.length - 1];
        for (int i = widths.length - 1; i >= 0; i--) {
            if (widths[i] < min) {
                result = i;
            }
        }
        widths[result]++;
    }

    private void alignSelectionModelToRows() {
        if (getRowCount() == 0) {
            if (selectionModel.getAnchorSelectionIndex() >= 0) {
                selectionModel.setValueIsAdjusting(true);
                selectionModel.setAnchorSelectionIndex(-1);
                selectionModel.setLeadSelectionIndex(-1);
                selectionModel.setValueIsAdjusting(false);
            }
        } else if (selectionModel.getLeadSelectionIndex() < 0) {
            selectionModel.removeSelectionInterval(0, 0);
        }
    }

    private interface ResizableElements {
        int getElementsCount();

        TableColumn getElement(int i);
    }

    private class MutableInteger {
        private int value = -1;

        public void setValue(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private class BooleanTableCellRenderer extends JCheckBox implements TableCellRenderer {
        private static final long serialVersionUID = 1L;

        private final Border noFocusBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);

        private Border focusBorder;

        private Color focusCellBackground;

        private Color focusCellForeground;

        public BooleanTableCellRenderer() {
            updateUI();
        }

        @Override
        public void updateUI() {
            super.updateUI();
            focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
            focusCellBackground = UIManager.getColor("Table.focusCellBackground");
            focusCellForeground = UIManager.getColor("Table.focusCellForeground");
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setValue(value);
            setFont(table.getFont());
            setHorizontalAlignment(SwingConstants.CENTER);
            if (hasFocus) {
                setBorder(focusBorder);
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                    setForeground(table.getSelectionForeground());
                } else if (table.isCellEditable(row, column)) {
                    setBackground(focusCellBackground);
                    setForeground(focusCellForeground);
                } else {
                    setBackground(table.getBackground());
                    setForeground(table.getForeground());
                }
            } else {
                setBorder(noFocusBorder);
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                    setForeground(table.getSelectionForeground());
                } else {
                    setBackground(table.getBackground());
                    setForeground(table.getForeground());
                }
            }
            return this;
        }

        @Override
        public boolean isOpaque() {
            return true;
        }

        private void setValue(Object value) {
            if (value == null) {
                setSelected(false);
            } else {
                setSelected(Boolean.valueOf(value.toString()).booleanValue());
            }
        }
    }

    private class DateTableCellRenderer extends DefaultTableCellRenderer.UIResource {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, value != null ? DateFormat
                    .getDateInstance().format((Date) value) : null, isSelected, hasFocus, row,
                    column);
        }
    }

    private class NumberTableCellRenderer extends DefaultTableCellRenderer.UIResource {
        private static final long serialVersionUID = 1L;

        public NumberTableCellRenderer() {
            super();
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, value != null ? NumberFormat
                    .getNumberInstance().format(value) : null, isSelected, hasFocus, row,
                    column);
        }
    }

    private class IconTableCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel result = (JLabel) super.getTableCellRendererComponent(table, null,
                    isSelected, hasFocus, row, column);
            if (value != null) {
                if (value instanceof Icon) {
                    result.setIcon((Icon) value);
                } else {
                    result.setIcon(new ImageIcon(value.toString()));
                }
                result.setHorizontalAlignment(SwingConstants.CENTER);
                result.setVerticalAlignment(SwingConstants.CENTER);
            } else {
                result.setIcon(null);
            }
            return result;
        }
    }
}
