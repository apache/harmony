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
 * @author Anton Avtamonov, Sergey Burlak
 */
package javax.swing.plaf.metal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicFileChooserUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position.Bias;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;


public class MetalFileChooserUI extends BasicFileChooserUI {

    protected class DirectoryComboBoxAction extends AbstractAction {
        public void actionPerformed(final ActionEvent e) {
            JComboBox source = (JComboBox)e.getSource();
            File selected = (File)source.getSelectedItem();
            getFileChooser().setCurrentDirectory(selected);
        }
    }

    protected class DirectoryComboBoxModel extends AbstractListModel implements ComboBoxModel {
        private final List files = new LinkedList();
        private final List predefinedStructure = new ArrayList();
        private Object selectedItem;
        private File currentDirectory;

        public DirectoryComboBoxModel() {
            setCurrentDirectory(getFileChooser().getCurrentDirectory());
        }

        public int getDepth(final int i) {
            if (i < 0 || i >= files.size()) {
                return 0;
            }
            File file = (File)files.get(i);
            int result = 0;
            while (file != null && !getFileChooser().getFileSystemView().isRoot(file)) {
                file = getFileChooser().getFileSystemView().getParentDirectory(file);
                result++;
            }

            return result;
        }

        public void setSelectedItem(final Object selectedDirectory) {
            if (selectedItem != selectedDirectory) {
                selectedItem = selectedDirectory;
                fireContentsChanged(this, 0, files.size() - 1);
            }
        }

        public Object getSelectedItem() {
            return selectedItem;
        }

        public int getSize() {
            return files.size();
        }

        public Object getElementAt(final int index) {
            return files.get(index);
        }

        private void setCurrentDirectory(final File file) {
            createPredefinedStructure();
            if (currentDirectory != null && currentDirectory.equals(file)) {
                return;
            }
            files.clear();
            files.addAll(predefinedStructure);

            appendFileStructure(file, files);
            currentDirectory = file;

            rescanCurrentDirectory(getFileChooser());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (getModel() != null && getModel().getSize() > 0) {
                        list.ensureIndexIsVisible(0);
                    }
                }
            });

            setSelectedItem(file);

            setupListAnchor();
        }

        private void setupListAnchor() {
            ListSelectionModel selectionModel = getViewRepresentation().getSelectionModel();

            int selectionAnchor = selectionModel.getLeadSelectionIndex();
            selectionModel.clearSelection();
            if (selectionAnchor >= 0 && selectionAnchor < getModel().getSize()) {
                selectionModel.setAnchorSelectionIndex(selectionAnchor);
                selectionModel.setLeadSelectionIndex(selectionAnchor);
            }
        }

        private void appendFileStructure(final File file, final List target) {
            List appendFiles = new LinkedList();
            File curFile = file;
            while (curFile != null && !target.contains(curFile)) {
                appendFiles.add(0, curFile);
                curFile = getFileChooser().getFileSystemView().getParentDirectory(curFile);
            }
            if (curFile != null) {
                target.addAll(target.indexOf(curFile) + 1, appendFiles);
            }
        }

        private void createPredefinedStructure() {
            predefinedStructure.clear();
            processRoots();
            appendFileStructure(getFileChooser().getFileSystemView().getDefaultDirectory(), predefinedStructure);
        }

        private void processRoots() {
            FileSystemView fileSystemView = getFileChooser().getFileSystemView();
            File[] roots = fileSystemView.getRoots();
            for (int i = 0; i < roots.length; i++) {
                predefinedStructure.add(roots[i]);
                File[] children = fileSystemView.getFiles(roots[i], false);
                if (children != null) {
                    for (int j = 0; j < children.length; j++) {
                        processRoot(children[j]);
                    }
                }
            }
        }

        private void processRoot(final File root) {
            FileSystemView fileSystemView = getFileChooser().getFileSystemView();
            if (!fileSystemView.isFileSystem(root) || fileSystemView.isFileSystemRoot(root)) {
                predefinedStructure.add(root);
                if (!fileSystemView.isFileSystemRoot(root)) {
                    File[] children = fileSystemView.getFiles(root, false);
                    if (files != null) {
                        for (int i = 0; i < children.length; i++) {
                            if (fileSystemView.isFileSystemRoot(children[i])) {
                                predefinedStructure.add(children[i]);
                            }
                        }
                    }
                }
            }
        }
    }

    protected class FileRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(final JList list,
                                                      final Object value,
                                                      final int index,
                                                      final boolean isSelected,
                                                      final boolean cellHasFocus) {

            File renderingFile = (File)value;
            String displayValue = getFileChooser().getName(renderingFile);

            JLabel result = (JLabel)super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
            result.setIcon(getFileChooser().getIcon(renderingFile));

            return result;
        }
    }

    protected class FilterComboBoxModel extends AbstractListModel implements ComboBoxModel, PropertyChangeListener {
        protected FileFilter[] filters = getFileChooser().getChoosableFileFilters();

        private Object selectedFilter;

        protected FilterComboBoxModel() {
            getFileChooser().addPropertyChangeListener(this);
        }

        public void propertyChange(final PropertyChangeEvent e) {
            String changedProperty = e.getPropertyName();
            if (JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY.equals(changedProperty)) {
                filters = (FileFilter[])e.getNewValue();
                fireContentsChanged(this, 0, filters.length - 1);
            } else if (JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(changedProperty)) {
                setSelectedItem(e.getNewValue());
                if (!Arrays.asList(filters).contains(e.getNewValue())) {
                    getFileChooser().addChoosableFileFilter((FileFilter)e.getNewValue());
                }
            }
        }

        public void setSelectedItem(final Object filter) {
            selectedFilter = filter;
            getViewRepresentation().getSelectionModel().clearSelection();
            fireContentsChanged(this, -1, -1);
        }

        public Object getSelectedItem() {
            if (selectedFilter == null && filters.length > 0) {
                selectedFilter = filters[0];
            }
            return selectedFilter;
        }

        public int getSize() {
            return filters.length;
        }

        public Object getElementAt(final int index) {
            return filters[index];
        }
    }

    public class FilterComboBoxRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(final JList list,
                                                      final Object value,
                                                      final int index,
                                                      final boolean isSelected,
                                                      final boolean cellHasFocus) {

            Object displayableValue = value;
            if (value instanceof FileFilter) {
                FileFilter filter = (FileFilter)value;
                displayableValue = filter.getDescription();
            }
            return super.getListCellRendererComponent(list, displayableValue, index, isSelected, cellHasFocus);
        }
    }

    protected class SingleClickListener extends MouseAdapter {
        private class CellEditor extends JTextField {
            private final int editingIndex;

            public CellEditor(final int index) {
                editingIndex = index;
                installListeners();
            }

            public void configure() {
                Rectangle editorBounds = list.getCellBounds(editingIndex, editingIndex);
                if(list.getCellRenderer()!=null){                
                    JLabel renderer = (JLabel)list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(editingIndex), editingIndex, true, true);
                    String text = renderer.getText();
                    setText(text);
                    Icon icon = renderer.getIcon();
                    if (icon != null) {
                           int offset = icon.getIconWidth() + renderer.getIconTextGap();
                           editorBounds.x += offset;
                           editorBounds.width -= offset;
                    }
                }
                setBounds(editorBounds);
                requestFocus();
                selectAll();
            }

            private void installListeners() {
                final FocusListener focusListener = new FocusAdapter() {
                    public void focusLost(final FocusEvent e) {
                        list.remove(CellEditor.this);
                        list.requestFocus();
                        updateListValue();
                    }
                };
                addFocusListener(focusListener);
                registerKeyboardAction(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        list.remove(CellEditor.this);
                    }
                }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
                registerKeyboardAction(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        removeFocusListener(focusListener);
                        list.remove(CellEditor.this);
                        list.requestFocus();
                    }
                }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
            }

            private void updateListValue() {
                File editingFile = (File)list.getModel().getElementAt(editingIndex);
                if (!Utilities.isEmptyString(getText()) && !getText().equals(editingFile.getName())) {
                    getModel().renameFile(editingFile, new File(editingFile.getParentFile(), getText()));
                }
            }
        }


        private JList list;
        private Object preSelectedValue;

        public SingleClickListener(final JList list) {
            this.list = list;
        }

        public void mouseClicked(final MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            int clickIndex = list.getUI().locationToIndex(list, e.getPoint());
            if (clickIndex == -1
                || e.getClickCount() != 1
                || list.getSelectedValues().length != 1) {

                preSelectedValue = null;
                return;
            }

            Object clickValue = list.getModel().getElementAt(clickIndex);
            if (clickValue != preSelectedValue) {
                preSelectedValue = clickValue;
                return;
            }

            preSelectedValue = null;
            CellEditor editor = new CellEditor(clickIndex);
            list.add(editor);
            editor.configure();
        }
    }

    private class DetailedFileViewModel extends AbstractTableModel {
        private String[] columnNames;

        public int getRowCount() {
            return getModel().getSize();
        }

        public int getColumnCount() {
            return 5;
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            File file = (File)getModel().getElementAt(rowIndex);
            if (columnIndex == 0) {
                return file.getName();
            } else if (columnIndex == 1) {
                return file.isDirectory() ? null : getAdjustedFileSize(file);
            } else if (columnIndex == 2) {
                return getFileChooser().getTypeDescription(file);
            } else if (columnIndex == 3) {
                return getFileChooser().getFileSystemView().isDrive(file) ? null : new Date(file.lastModified());
            } else if (columnIndex == 4) {
                if (file.isDirectory()) {
                    return null;
                }
                if (file.canWrite() && !file.isHidden()) {
                    return null;
                } else {
                    StringBuilder result = new StringBuilder();
                    if (!file.canWrite()) {
                        result.append("R");
                    }
                    if (file.isHidden()) {
                        result.append("H");
                    }

                    return result.toString();
                }
            }

            return null;
        }

        public void setValueAt(final Object value, final int rowIndex, final int columnIndex) {
            File editingFile = (File)list.getModel().getElementAt(rowIndex);
            if (!Utilities.isEmptyString((String)value) && !value.equals(editingFile.getName())) {
                getModel().renameFile(editingFile, new File(editingFile.getParentFile(), (String)value));
            }
        }



        public Class getColumnClass(final int columnIndex) {
            if (columnIndex == 3) {
                return Date.class;
            }
            return String.class;
        }

        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return columnIndex == 0;
        }

        public String getColumnName(final int column) {
            if (columnNames == null) {
                initColumnNames();
            }
            return columnNames[column];
        }

        private void initColumnNames() {
            columnNames = new String[5];
            columnNames[0] = UIManager.getString("FileChooser.fileNameHeaderText");
            columnNames[1] = UIManager.getString("FileChooser.fileSizeHeaderText");
            columnNames[2] = UIManager.getString("FileChooser.fileTypeHeaderText");
            columnNames[3] = UIManager.getString("FileChooser.fileDateHeaderText");
            columnNames[4] = UIManager.getString("FileChooser.fileAttrHeaderText");
        }
    }

    private class DirectoryComboBoxRenderer extends DefaultListCellRenderer {
        private WeakHashMap iconMap = new WeakHashMap();

        public Component getListCellRendererComponent(final JList list,
                                                      final Object value,
                                                      final int index,
                                                      final boolean isSelected,
                                                      final boolean cellHasFocus) {

            File renderingFile = (File)value;
            Object displayValue = getFileChooser().getName(renderingFile);

            JLabel result = (JLabel)super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
            Icon icon;
            if (index == -1) {
                icon = getFileChooser().getIcon(renderingFile);
            } else {
                icon = (Icon)iconMap.get(value);
                if (icon == null) {
                    icon = new IndentedIcon(getFileChooser().getIcon(renderingFile), directoryModel.getDepth(index));
                    iconMap.put(value, icon);
                }
            }
            result.setIcon(icon);

            return result;
        }
    }

    private abstract class ChangeViewAction extends AbstractAction {
        protected void updateFileChooser() {
            if (isDetailedViewActivated) {
                detailedViewButton.setSelected(true);
                detailedViewItem.setSelected(true);
            } else {
                listViewButton.setSelected(true);
                listViewItem.setSelected(true);
            }
            getFileChooser().revalidate();
            getFileChooser().repaint();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (getViewRepresentation().getSelectedValue() != null) {
                        getViewRepresentation().ensureIndexIsVisible(getModel().indexOf(getViewRepresentation().getSelectedValue()));
                    }
                }
            });
        }
    }

    private class ListViewAction extends ChangeViewAction {
        public ListViewAction() {
            putValue(AbstractAction.NAME, listViewButtonText);
            putValue(AbstractAction.SHORT_DESCRIPTION, listViewButtonToolTipText);
            putValue(AbstractAction.SMALL_ICON, listViewIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            getFileChooser().remove(detailedView);
            getFileChooser().add(listView, BorderLayout.CENTER);
            isDetailedViewActivated = false;
            updateFileChooser();
        }
    }

    private class DetailedViewAction extends ChangeViewAction {
        public DetailedViewAction() {
            putValue(AbstractAction.NAME, detailedViewButtonText);
            putValue(AbstractAction.SHORT_DESCRIPTION, detailedViewButtonToolTipText);
            putValue(AbstractAction.SMALL_ICON, detailsViewIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            getFileChooser().remove(listView);
            getFileChooser().add(detailedView, BorderLayout.CENTER);
            isDetailedViewActivated = true;
            updateFileChooser();
        }
    }

    private class OpenDirectoryAction extends AbstractAction {
        public OpenDirectoryAction() {
            putValue(AbstractAction.NAME, openButtonText);
            putValue(AbstractAction.SHORT_DESCRIPTION, openButtonToolTipText);
        }

        public void actionPerformed(final ActionEvent e) {
            File selectedDir = getViewRepresentation().getSelectedValue();
            if (selectedDir == null) {
                selectedDir = translateFile(getFileName());
            }
            getFileChooser().setCurrentDirectory(selectedDir);
        }
    }

    private static class IndentedIcon implements Icon {
        private static final int INDENT_WIDTH = 10;
        private Icon icon;
        private int indent;

        public IndentedIcon(final Icon icon, final int indent) {
            this.icon = icon;
            this.indent = indent * INDENT_WIDTH;
        }

        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            if (icon != null) {
                icon.paintIcon(c, g, c.getComponentOrientation().isLeftToRight() ? x + indent : x, y);
            }
        }

        public int getIconWidth() {
            return indent + (icon != null ? icon.getIconWidth() : 0);
        }

        public int getIconHeight() {
            return icon != null ? icon.getIconHeight() : 1;
        }
    }



    private interface ViewRepresentation {
        public ListSelectionModel getSelectionModel();
        public File getSelectedValue();
        public Object[] getSelectedValues();
        public void setSelectedValue(Object value, boolean scroll);
        public void ensureIndexIsVisible(final int index);
    }

    private class DetailedRepresentation implements ViewRepresentation {
        public ListSelectionModel getSelectionModel() {
            return table.getSelectionModel();
        }

        public File getSelectedValue() {
            return (File)(table.getSelectedRow() >= 0 ? getModel().getElementAt(table.getSelectedRow()) : null);
        }

        public Object[] getSelectedValues() {
            File[] result;
            if (table.getSelectedRow() != -1) {
                int[]  selectedRows = table.getSelectedRows();
                result = new File[selectedRows.length];
                for (int i = 0; i < selectedRows.length; i++) {
                    result[i] = (File)getModel().getElementAt(selectedRows[i]);
                }
            } else {
                result = new File[0];
            }

            return result;
        }

        public void setSelectedValue(final Object value, final boolean scroll) {
            int row = getModel().indexOf(value);
            if (row < 0) {
                return;
            }
            getSelectionModel().setSelectionInterval(row, row);
            if (scroll) {
                ensureIndexIsVisible(row);
            }
        }

        public void ensureIndexIsVisible(final int index) {
            Rectangle cellRect = table.getCellRect(index, 0, false);
            table.scrollRectToVisible(cellRect);
        }
    }

    private class ListRepresentation implements ViewRepresentation {
        public ListSelectionModel getSelectionModel() {
            return list.getSelectionModel();
        }

        public File getSelectedValue() {
            return (File)list.getSelectedValue();
        }

        public Object[] getSelectedValues() {
            return list.getSelectedValues();
        }

        public void setSelectedValue(final Object value, final boolean scroll) {
            list.setSelectedValue(value, scroll);
        }

        public void ensureIndexIsVisible(final int index) {
            list.ensureIndexIsVisible(index);
        }
    }


    private JPanel buttonPanel;
    private JPanel bottomPanel;
    private JLabel actionLabel;
    private JButton approveButton;
    private JList list;
    private JTable table;
    private JPanel listView;
    private JPanel detailedView;
    private DirectoryComboBoxModel directoryModel;
    private JTextField fileNameField;
    private Action approveSelectionAction;
    private Action openDirectoryAction;

    private Action listViewAction;
    private Action detailedViewAction;
    private boolean isDetailedViewActivated;

    private String listViewButtonText;
    private String listViewButtonToolTipText;
    private String detailedViewButtonText;
    private String detailedViewButtonToolTipText;

    private JMenuItem listViewItem;
    private JMenuItem detailedViewItem;
    private JToggleButton listViewButton;
    private JToggleButton detailedViewButton;


    private ViewRepresentation listViewRepresentation = new ListRepresentation();
    private ViewRepresentation detailedViewRepresentation = new DetailedRepresentation();


    public MetalFileChooserUI(final JFileChooser filechooser) {
        super(filechooser);
        buttonPanel = new JPanel(new BorderLayout());
        bottomPanel = new JPanel(new GridBagLayout());
        actionLabel = new JLabel();
    }

    public static ComponentUI createUI(final JComponent c) {
        return new MetalFileChooserUI((JFileChooser)c);
    }

    public void installUI(final JComponent c) {
        c.setLayout(new BorderLayout());
        super.installUI(c);

        openDirectoryAction = new OpenDirectoryAction();
    }

    public void uninstallUI(final JComponent c) {
        super.uninstallUI(c);
        c.setLayout(null);
    }

    public void installComponents(final JFileChooser fc) {
        super.installComponents(fc);

        listViewAction = new ListViewAction();
        detailedViewAction = new DetailedViewAction();

        approveSelectionAction = getApproveSelectionAction();
        approveButton = new JButton(approveSelectionAction);

        fc.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        listView = createList(fc);
        detailedView = createDetailsView(fc);

        fc.add(listView, BorderLayout.CENTER);
        fc.add(createTopPanel(fc), BorderLayout.NORTH);

        JPanel southPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 12, 0));
        southPanel.add(bottomPanel, BorderLayout.CENTER);
        fillBottomPanel();

        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        addControlButtons();
        fc.add(southPanel, BorderLayout.SOUTH);

        JPopupMenu popupMenu = createPopupMenu();
        list.setComponentPopupMenu(popupMenu);
        table.setComponentPopupMenu(popupMenu);

        customizeFileChooserLabels(fc);
    }

    public void uninstallComponents(final JFileChooser fc) {
        super.uninstallComponents(fc);
        list.setTransferHandler(null);
        removeControlButtons();
        list.setComponentPopupMenu(null);
        table.setComponentPopupMenu(null);
    }

    public ListSelectionListener createListSelectionListener(final JFileChooser fc) {
        return new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                MetalFileChooserUI.this.valueChanged(e);
            }
        };
    }

    public PropertyChangeListener createPropertyChangeListener(final JFileChooser fc) {
        return new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent event) {
                String changedProperty = event.getPropertyName();
                if (JFileChooser.DIALOG_TYPE_CHANGED_PROPERTY.equals(changedProperty)
                    || JFileChooser.APPROVE_BUTTON_TEXT_CHANGED_PROPERTY.equals(changedProperty)) {
                    customizeFileChooserLabels(fc);
                } else if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(changedProperty)) {
                    File file = (File)event.getNewValue();
                    directoryModel.setCurrentDirectory(file);
                    getChangeToParentDirectoryAction().setEnabled(!getFileChooser().getFileSystemView().isRoot(file));
                } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(changedProperty)) {
                    File selectedFile = (File)event.getNewValue();
                    if (selectedFile != null) {
                        File parentDirectory = getFileChooser().getFileSystemView().getParentDirectory(selectedFile);
                        if (parentDirectory != null) {
                            directoryModel.setCurrentDirectory(parentDirectory);
                            getViewRepresentation().setSelectedValue(selectedFile, true);
                        }
                    }
                    if (selectedFile != null) {
                        fileNameField.setText(fileToText(selectedFile));
                    }
                } else if (JFileChooser.SELECTED_FILES_CHANGED_PROPERTY.equals(changedProperty)) {
                    File[] selectedFiles = (File[])event.getNewValue();
                    if (Utilities.isEmptyArray(selectedFiles)) {
                        return;
                    }

                    StringBuilder textSelection = new StringBuilder();
                    if (selectedFiles.length == 1) {
                        textSelection.append(fileToText(selectedFiles[0]));
                        getViewRepresentation().setSelectedValue(selectedFiles[0], true);
                    } else {
                        if (!equals(getViewRepresentation().getSelectedValues(), selectedFiles)) {
                            updateListSelection(selectedFiles);
                        }

                        for (int i = 0; i < selectedFiles.length; i++) {
                            if (textSelection.length() > 0) {
                                textSelection.append(" ");
                            }
                            textSelection.append('\"').append(fileToText(selectedFiles[i])).append('\"');
                        }
                    }
                    fileNameField.setText(textSelection.toString());
                } else if (JFileChooser.MULTI_SELECTION_ENABLED_CHANGED_PROPERTY.equals(changedProperty)) {
                    getViewRepresentation().getSelectionModel().setSelectionMode(((Boolean)event.getNewValue()).booleanValue()
                            ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                            : ListSelectionModel.SINGLE_SELECTION);
                } else if (JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY.equals(changedProperty)) {
                    if (((Integer)event.getNewValue()).intValue() == JFileChooser.DIRECTORIES_ONLY) {
                        fileNameField.setText(fileToText(getFileChooser().getCurrentDirectory()));
                    }
                } else if (JFileChooser.ACCESSORY_CHANGED_PROPERTY.equals(changedProperty)) {
                    final JComponent old = (JComponent) event.getOldValue();
                                    
                    if (old != null) {
                        getAccessoryPanel().remove(old);
                    }

                    getAccessoryPanel().add((JComponent)event.getNewValue());
                } else if (StringConstants.TRANSFER_HANDLER_PROPERTY_NAME.equals(changedProperty)) {
                    list.setTransferHandler((TransferHandler)event.getNewValue());
                    table.setTransferHandler((TransferHandler)event.getNewValue());
                }
            }

            private String fileToText(final File file) {
                if (getFileChooser().getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) {
                    return file.getAbsolutePath();
                } else {
                    return file.getName();
                }
            }

            private boolean equals(final Object[] files1, final Object[] files2) {
                if (files1.length != files2.length) {
                    return false;
                }

                for (int i = 0; i < files1.length; i++) {
                    if (!contains(files2, files1[i])) {
                        return false;
                    }
                }

                return true;
            }

            private boolean contains(final Object[] files, final Object file) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].equals(file)) {
                        return true;
                    }
                }

                return false;
            }
        };
    }

    public Dimension getPreferredSize(final JComponent c) {
        return getFileChooser().getLayout().preferredLayoutSize(getFileChooser());
    }

    public Dimension getMinimumSize(final JComponent c) {
        return super.getMinimumSize(c);
    }

    public Dimension getMaximumSize(final JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public void ensureFileIsVisible(final JFileChooser fc, final File f) {
        int index = getModel().indexOf(f);
        if (index != -1) {
            getViewRepresentation().ensureIndexIsVisible(index);
        }
    }

    public void rescanCurrentDirectory(final JFileChooser fc) {
        if (fc.isShowing()) {
            getModel().validateFileCache();
        }
    }

    public String getFileName() {
        return fileNameField.getText();
    }

    public void setFileName(final String fileName) {
        fileNameField.setText(fileName);
    }

    public String getDirectoryName() {
        return super.getDirectoryName();
    }

    public void setDirectoryName(final String name) {
        super.setDirectoryName(name);
    }

    public void valueChanged(final ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        File selectedFile = getViewRepresentation().getSelectedValue();
        Object[] selectedValues = getViewRepresentation().getSelectedValues();

        if (selectedValues.length == 1 && selectedFile.isDirectory()) {
            setDirectorySelected(true);
            setDirectory(selectedFile);
        } else {
            setDirectorySelected(false);
            setDirectory(null);
        }

        
        configureApproveButton(selectedFile, selectedValues.length == 1);

        if (getFileChooser().isMultiSelectionEnabled()) {
            List selectedFiles = new LinkedList();
            for (int i = 0; i < selectedValues.length; i++) {
                File currentFile = (File)selectedValues[i];
                if (isSelectableFile(currentFile)) {
                    selectedFiles.add(currentFile);
                }
            }
            getFileChooser().setSelectedFiles((File[])selectedFiles.toArray(new File[selectedFiles.size()]));
        } else {
            if (selectedFile == null || !isSelectableFile(selectedFile)) {
                selectedFile = null;
            }
            getFileChooser().setSelectedFile(selectedFile);
        }
    }

    protected void setDirectorySelected(final boolean directorySelected) {
        super.setDirectorySelected(directorySelected);
    }

    protected JPanel getButtonPanel() {
        return buttonPanel;
    }

    protected JPanel getBottomPanel() {
        return bottomPanel;
    }

    protected void installStrings(final JFileChooser fc) {
        super.installStrings(fc);
        listViewButtonText = UIManager.getString("FileChooser.listViewButtonAccessibleName");
        listViewButtonToolTipText = UIManager.getString("FileChooser.listViewButtonToolTipText");

        detailedViewButtonText = UIManager.getString("FileChooser.detailsViewButtonAccessibleName");
        detailedViewButtonToolTipText = UIManager.getString("FileChooser.detailsViewButtonToolTipText");
    }

    protected void installListeners(final JFileChooser fc) {
        super.installListeners(fc);
        fc.addPropertyChangeListener(createPropertyChangeListener(fc));

        fc.getActionMap().put("Go Up", new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                if (!(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent)) {
                    getChangeToParentDirectoryAction().actionPerformed(e);
                }
            }
        });
        fc.getActionMap().put("approveSelection", new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                approveButton.getAction().actionPerformed(e);
            }
        });

        fc.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(final HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && fc.isShowing()) {
                    fileNameField.requestFocus();
                    fileNameField.selectAll();

                    rescanCurrentDirectory(getFileChooser());
                    if (getFileChooser().isMultiSelectionEnabled()) {
                        File[] selectedFiles = getFileChooser().getSelectedFiles();
                        if (selectedFiles.length > 0) {
                            updateListSelection(selectedFiles);
                            getViewRepresentation().ensureIndexIsVisible(getModel().indexOf(selectedFiles[selectedFiles.length - 1]));
                        }
                    } else {
                        File selectedFile = getFileChooser().getSelectedFile();
                        if (selectedFile != null) {
                            updateListSelection(new File[] {selectedFile});
                            getViewRepresentation().ensureIndexIsVisible(getModel().indexOf(selectedFile));
                        }
                    }
                    directoryModel.setCurrentDirectory(getFileChooser().getCurrentDirectory());
                }
            }
        });

        MouseListener showPopupListener = new MouseAdapter() {
            public void mousePressed(final MouseEvent e) {
                processPopupMenu(e);
            }

            public void mouseReleased(final MouseEvent e) {
                processPopupMenu(e);
            }

            private void processPopupMenu(final MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }

                ((JComponent)e.getComponent()).getComponentPopupMenu().show(e.getComponent(), e.getX(), e.getY());
            }
        };
        list.addMouseListener(showPopupListener);
        table.addMouseListener(showPopupListener);
    }

    protected ActionMap getActionMap() {
        return createActionMap();
    }

    protected ActionMap createActionMap() {
        ActionMap result = new ActionMapUIResource();
        result.put("approveSelection", getApproveSelectionAction());
        result.put("cancelSelection", getCancelSelectionAction());
        result.put("Go Up", getChangeToParentDirectoryAction());

        return result;
    }

    protected JPanel createList(final JFileChooser fc) {
        JPanel result = new JPanel(new BorderLayout());
        result.setPreferredSize(new Dimension(300, 200));
        result.setBorder(MetalBorders.getTextFieldBorder());

        list = new JList(getModel()) {
            public int getNextMatch(final String prefix, final int startIndex, final Bias bias) {
                return Utilities.getNextMatch(new Utilities.ListModelAccessor() {
                    public Object getElementAt(final int index) {
                        return ((File)getModel().getElementAt(index)).getName();
                    }

                    public int getSize() {
                        return getModel().getSize();
                    }
                    
                }, prefix, startIndex, bias);
            }
        };
        list.setSelectionMode(fc.isMultiSelectionEnabled() ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                : ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        list.setVisibleRowCount(-1);
        list.addListSelectionListener(createListSelectionListener(fc));
        result.add(new JScrollPane(list), BorderLayout.CENTER);
        result.add(getAccessoryPanel(), BorderLayout.LINE_END);

        list.setCellRenderer(new FileRenderer());
        list.addMouseListener(new SingleClickListener(list));
        list.addMouseListener(createDoubleClickListener(fc, list));
        list.setTransferHandler(fc.getTransferHandler());

        return result;
    }

    protected JPanel createDetailsView(final JFileChooser fc) {
        JPanel result = new JPanel(new BorderLayout());
        result.setPreferredSize(new Dimension(300, 200));
        result.setBorder(MetalBorders.getTextFieldBorder());

        TableModel model = new DetailedFileViewModel();
        table = new JTable(model) {
            protected void processKeyEvent(final KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER
                    || event.getKeyCode() == KeyEvent.VK_ESCAPE) {

                    KeyEvent fcEvent = new KeyEvent(getFileChooser(), event.getID(), event.getWhen(), event.getModifiers(), event.getKeyCode(), event.getKeyChar(), event.getKeyLocation());
                    SwingUtilities.processKeyBindings(fcEvent);
                    return;
                }

                super.processKeyEvent(event);
            }
        };
        table.setShowGrid(false);
        table.setSelectionModel(list.getSelectionModel());
        result.add(new JScrollPane(table), BorderLayout.CENTER);
        result.add(getAccessoryPanel(), BorderLayout.LINE_END);

        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(30);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);
        table.getColumnModel().getColumn(4).setPreferredWidth(20);

        table.getColumn(model.getColumnName(0)).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(final JTable table,
                                                           final Object value,
                                                           final boolean isSelected,
                                                           final boolean hasFocus,
                                                           final int row,
                                                           final int column) {

                File renderingFile = (File)getModel().getElementAt(row);
                String displayValue = getFileChooser().getName(renderingFile);

                JLabel result = (JLabel)super.getTableCellRendererComponent(table, displayValue, isSelected, hasFocus, row, column);
                result.setIcon(getFileChooser().getIcon(renderingFile));

                return result;
            }
        });
        table.getColumn(model.getColumnName(1)).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(final JTable table,
                                                           final Object value,
                                                           final boolean isSelected,
                                                           final boolean hasFocus,
                                                           final int row,
                                                           final int column) {

                JLabel result = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                result.setHorizontalAlignment(SwingConstants.RIGHT);
                return result;
            }
        });
        table.getColumn(model.getColumnName(3)).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(final JTable table,
                                                           final Object value,
                                                           final boolean isSelected,
                                                           final boolean hasFocus,
                                                           final int row,
                                                           final int column) {

                Date date = (Date)value;
                if (date != null) {
                    JLabel result = (JLabel)super.getTableCellRendererComponent(table, DateFormat.getDateTimeInstance().format(date), isSelected, hasFocus, row, column);
                    result.setHorizontalAlignment(SwingConstants.RIGHT);
                    return result;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        table.setDefaultEditor(String.class, new DefaultCellEditor(new JTextField()) {
            public boolean isCellEditable(final EventObject event) {
                return event == null;
            }
        });


        getModel().addListDataListener(new ListDataListener() {
            public void intervalAdded(final ListDataEvent e) {
                fireTableChanged();
            }

            public void intervalRemoved(final ListDataEvent e) {
                fireTableChanged();
            }

            public void contentsChanged(final ListDataEvent e) {
                fireTableChanged();
            }

            private void fireTableChanged() {
                if (isDetailedViewActivated) {
                    ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                if (e.getClickCount() != 2) {
                    return;
                }
                File file = getViewRepresentation().getSelectedValue();
                if (file != null && file.isDirectory()) {
                    getFileChooser().setCurrentDirectory(file);
                    return;
                }

                approveSelectionAction.actionPerformed(null);
            }
        });

        return result;
    }

    protected void addControlButtons() {
        JPanel alignPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        buttonPanel.add(alignPanel, BorderLayout.LINE_END);
        alignPanel.add(approveButton);

        JButton cancelButton = new JButton(getCancelSelectionAction());
        cancelButton.setText(UIManager.getString("FileChooser.cancelButtonText"));
        alignPanel.add(cancelButton);
    }

    protected void removeControlButtons() {
        buttonPanel.removeAll();
    }

    protected DirectoryComboBoxRenderer createDirectoryComboBoxRenderer(final JFileChooser fc) {
        return new DirectoryComboBoxRenderer();
    }

    protected DirectoryComboBoxModel createDirectoryComboBoxModel(final JFileChooser fc) {
        return new DirectoryComboBoxModel();
    }

    protected FilterComboBoxRenderer createFilterComboBoxRenderer() {
        return new FilterComboBoxRenderer();
    }

    protected FilterComboBoxModel createFilterComboBoxModel() {
        return new FilterComboBoxModel();
    }

    protected JButton getApproveButton(final JFileChooser fc) {
        return approveButton;
    }

    private void customizeFileChooserLabels(final JFileChooser fc) {
        if (fc.getDialogType() == JFileChooser.SAVE_DIALOG) {
            actionLabel.setText(UIManager.getString("FileChooser.saveInLabelText"));
        } else {
            actionLabel.setText(UIManager.getString("FileChooser.lookInLabelText"));
        }
    }

    private void fillBottomPanel() {
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;

        c.insets = new Insets(0, 0, 6, 0);
        c.gridwidth = 1;
        c.weightx = 0.0;
        JLabel fileNameLabel = new JLabel(UIManager.getString("FileChooser.fileNameLabelText"));
        fileNameLabel.setDisplayedMnemonic(UIManager.getInt("FileChooser.fileNameLabelMnemonic"));
        bottomPanel.add(fileNameLabel, c);
        bottomPanel.add(Box.createHorizontalStrut(6), c);


        c.weightx = 1.0;
        c.gridwidth = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        fileNameField = new JTextField();
        
        fileNameLabel.setLabelFor(fileNameField);
        
        fileNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(final DocumentEvent e) {
                onChange();
            }

            public void removeUpdate(final DocumentEvent e) {
                onChange();
            }

            public void changedUpdate(final DocumentEvent e) {
                onChange();
            }
            
            
            private void onChange() {
                configureApproveButton(translateFile(fileNameField.getText()), true);
            }
        });
        fileNameField.addFocusListener(new FocusAdapter() {
            public void focusGained(final FocusEvent e) {
                getViewRepresentation().getSelectionModel().clearSelection();
                configureApproveButton(translateFile(fileNameField.getText()), true);
            }
        });
        bottomPanel.add(fileNameField, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridwidth = 1;
        c.weightx = 0.0;
        JLabel filesOfTypeLabel = new JLabel(UIManager.getString("FileChooser.filesOfTypeLabelText"));
        filesOfTypeLabel.setDisplayedMnemonic(UIManager.getInt("FileChooser.filesOfTypeLabelMnemonic"));
        bottomPanel.add(filesOfTypeLabel, c);
        bottomPanel.add(Box.createHorizontalStrut(6), c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        final JComboBox fileFilterCombo = new JComboBox(createFilterComboBoxModel());
        
        filesOfTypeLabel.setLabelFor(fileFilterCombo);
        
        fileFilterCombo.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                fileNameField.setText("");
                getFileChooser().setFileFilter((FileFilter)fileFilterCombo.getSelectedItem());
            }
        });
        fileFilterCombo.setRenderer(createFilterComboBoxRenderer());
        bottomPanel.add(fileFilterCombo, c);
    }

    private JPanel createTopPanel(final JFileChooser fc) {
        final int topPanelHeight = 25;
        JPanel result = new JPanel(new BorderLayout());
        result.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        actionLabel = new JLabel();

        directoryModel = createDirectoryComboBoxModel(fc);
        JComboBox directoryComboBox = new JComboBox(directoryModel);
        directoryComboBox.setPreferredSize(new Dimension(300, topPanelHeight));
        directoryComboBox.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        directoryComboBox.setRenderer(createDirectoryComboBoxRenderer(fc));
        directoryComboBox.setAction(new DirectoryComboBoxAction());

        JPanel viewButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton parentDirButton = new JButton(getChangeToParentDirectoryAction());
        parentDirButton.setText(null);
        Dimension buttonPreferredSize = new Dimension(topPanelHeight, topPanelHeight);
        parentDirButton.setPreferredSize(buttonPreferredSize);
        JButton goHomeButton = new JButton(getGoHomeAction());
        goHomeButton.setText(null);
        goHomeButton.setPreferredSize(buttonPreferredSize);
        JButton newFolderButton = new JButton(getNewFolderAction());
        newFolderButton.setText(null);
        newFolderButton.setPreferredSize(buttonPreferredSize);

        viewButtonsPanel.add(parentDirButton);
        viewButtonsPanel.add(Box.createHorizontalStrut(6));
        viewButtonsPanel.add(goHomeButton);
        viewButtonsPanel.add(Box.createHorizontalStrut(6));
        viewButtonsPanel.add(newFolderButton);
        viewButtonsPanel.add(Box.createHorizontalStrut(6));

        listViewButton = new JToggleButton(listViewAction);
        listViewButton.setText(null);
        listViewButton.setPreferredSize(buttonPreferredSize);
        detailedViewButton = new JToggleButton(detailedViewAction);
        detailedViewButton.setText(null);
        detailedViewButton.setPreferredSize(buttonPreferredSize);
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(listViewButton);
        viewGroup.add(detailedViewButton);
        viewButtonsPanel.add(listViewButton);
        viewButtonsPanel.add(detailedViewButton);
        listViewButton.setSelected(true);

        result.add(actionLabel, BorderLayout.LINE_START);
        result.add(directoryComboBox, BorderLayout.CENTER);
        
        actionLabel.setDisplayedMnemonic(UIManager.getInt("FileChooser.lookInLabelMnemonic"));
        actionLabel.setLabelFor(directoryComboBox);
        
        result.add(viewButtonsPanel, BorderLayout.LINE_END);

        return result;
    }


    private void updateListSelection(final File[] files) {
        getViewRepresentation().getSelectionModel().setValueIsAdjusting(true);
        getViewRepresentation().getSelectionModel().clearSelection();
        for (int i = 0; i < files.length; i++) {
            int index = getModel().indexOf(files[i]);
            if (index != -1) {
                getViewRepresentation().getSelectionModel().addSelectionInterval(index, index);
            }
        }
        getViewRepresentation().getSelectionModel().setValueIsAdjusting(false);
    }

    private boolean isSelectableFile(final File file) {
        return (getFileChooser().isDirectorySelectionEnabled() && file.isDirectory()
                && (!getFileChooser().getFileSystemView().isFileSystemRoot(file) || getFileChooser().getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY)
                || file.isFile() && getFileChooser().isFileSelectionEnabled())
               && getFileChooser().getFileSystemView().isFileSystem(file);
    }

    private JPopupMenu createPopupMenu() {
        JMenu viewMenu = new JMenu("View");

        listViewItem = new JRadioButtonMenuItem(listViewAction);
        listViewItem.setIcon(null);
        detailedViewItem = new JRadioButtonMenuItem(detailedViewAction);
        detailedViewItem.setIcon(null);
        ButtonGroup bg = new ButtonGroup();
        bg.add(listViewItem);
        bg.add(detailedViewItem);
        viewMenu.add(listViewItem);
        viewMenu.add(detailedViewItem);

        JMenuItem refreshItem = new JMenuItem(getUpdateAction());
        refreshItem.setIcon(null);
        JMenuItem newFolderItem = new JMenuItem(getNewFolderAction());
        newFolderItem.setIcon(null);

        JPopupMenu result = new JPopupMenu() {
            public void show(final Component invoker, final int x, final int y) {
                if (isDetailedViewActivated) {
                    detailedViewItem.setSelected(true);
                } else {
                    listViewItem.setSelected(true);
                }

                super.show(invoker, x, y);
            }
        };


        result.add(viewMenu);
        result.add(refreshItem);
        result.add(newFolderItem);

        return result;
    }

    private ViewRepresentation getViewRepresentation() {
        return isDetailedViewActivated ? detailedViewRepresentation : listViewRepresentation;
    }

    private static String getAdjustedFileSize(final File file) {
        long length = file.length();
        long num = length >> 30;
        if (num > 0) {
            return num + " GB";
        }
        num = length >> 20;
        if (num > 0) {
            return num + " MB";
        }
        num = length >> 10;
        if (num == 0) {
            num = 1;
        }
        return num + " KB";
    }
    
    private void configureApproveButton(final File selectedFile, final boolean singleFileSelected) {
        Action approveAction;
        if (singleFileSelected
            && selectedFile != null 
            && selectedFile.isDirectory()
            && getFileChooser().isTraversable(selectedFile)
            && (!getFileChooser().isDirectorySelectionEnabled()
                || !getFileChooser().getFileSystemView().isFileSystem(selectedFile)
                || (getFileChooser().getFileSystemView().isFileSystemRoot(selectedFile)
                    && getFileChooser().getFileSelectionMode() != JFileChooser.DIRECTORIES_ONLY))) {

            approveAction = openDirectoryAction;
        } else {
            approveAction = approveSelectionAction;
        }
        if (approveButton.getAction() != approveAction) {
            approveButton.setAction(approveAction);
        }
            
    }
    
    
    private File translateFile(final String fileName) {
        if (Utilities.isEmptyString(fileName)) {
            return null;
        }

        File candidateFile = new File(fileName);
        if (candidateFile.isAbsolute()) {
            return candidateFile;
        }

        if (fileName.startsWith("\"")) {
            String nakedFileName = fileName.endsWith("\"")
                                   ? fileName.substring(1, fileName.length() - 1)
                                   : fileName.substring(1);

            File fileInCurrentDir = getFileChooser().getFileSystemView().getChild(getFileChooser().getCurrentDirectory(), nakedFileName);
            if (fileInCurrentDir != null
                && fileInCurrentDir.exists()
                && getFileChooser().getFileSystemView().isFileSystem(fileInCurrentDir)) {

                return fileInCurrentDir;
            } else {
                return getFileChooser().getFileSystemView().createFileObject(nakedFileName);
            }
        } else {
            return getFileChooser().getFileSystemView().createFileObject(getFileChooser().getCurrentDirectory(), fileName);
        }
    }
    
}
