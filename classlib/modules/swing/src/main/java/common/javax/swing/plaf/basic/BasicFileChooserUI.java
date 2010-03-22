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
 * @author Sergey Burlak
 */
package javax.swing.plaf.basic;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.FileChooserUI;

import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;


public class BasicFileChooserUI extends FileChooserUI {

    protected class AcceptAllFileFilter extends FileFilter {
        public AcceptAllFileFilter() {
        }

        public boolean accept(final File f) {
            return true;
        }
        public String getDescription() {
            return UIManager.getString("FileChooser.acceptAllFileFilterText");
        }
    }

    protected class BasicFileView extends FileView {
        protected Hashtable<java.io.File, javax.swing.Icon> iconCache = new Hashtable<java.io.File, javax.swing.Icon>();

        public BasicFileView() {
        }

        public void clearIconCache() {
            iconCache.clear();
        }

        public String getName(final File f) {
            return fileChooser.getFileSystemView().getSystemDisplayName(f);
        }

        public String getDescription(final File f) {
            return getName(f);
        }

        public String getTypeDescription(final File f) {
            return fileChooser.getFileSystemView().getSystemTypeDescription(f);
        }

        public Icon getCachedIcon(final File f) {
            return f == null ? null : (Icon)iconCache.get(f);
        }

        public void cacheIcon(final File f, final Icon i) {
            if (f == null || i == null) {
                return;
            }
            iconCache.put(f, i);
        }

        public Icon getIcon(final File f) {
            Icon result = getCachedIcon(f);
            if (result == null) {
                result = getIconForFile(f);
                cacheIcon(f, result);
            }

            return result;
        }

        public Boolean isHidden(final File f) {
            return f.isHidden() ? Boolean.TRUE : Boolean.FALSE;
        }

        private Icon getIconForFile(final File file) {
            FileSystemView view = getFileChooser().getFileSystemView();
            if (file == null) {
                return null;
            } else if (view.isComputerNode(file)) {
                return computerIcon;
            } else if (view.isFloppyDrive(file)) {
                return floppyDriveIcon;
            } else if (view.isDrive(file)) {
                return hardDriveIcon;
            } else if (file.isFile()) {
                return fileIcon;
            } else if (file.isDirectory()) {
                return directoryIcon;
            } else {
                return null;
            }
        }
    }

    protected class ApproveSelectionAction extends AbstractAction {
        protected ApproveSelectionAction() {
            putValue(Action.NAME, approveButtonText);
            putValue(Action.SHORT_DESCRIPTION, approveButtonToolTipText);
            putValue(Action.MNEMONIC_KEY, new Integer(approveButtonMnemonic));
        }

        public void actionPerformed(final ActionEvent e) {
            String fileName = getFileName();
            if (Utilities.isEmptyString(fileName)) {
                return;
            }
            if (fileChooser.isMultiSelectionEnabled()) {
                List selectedFiles = new ArrayList();
                String[] fileNames = splitFileNames(fileName);
                for (int i = 0; i < fileNames.length; i++) {
                    selectedFiles.add(translateFile(fileNames[i]));
                }
                fileChooser.setSelectedFiles((File[])selectedFiles.toArray(new File[selectedFiles.size()]));
            } else {
                fileChooser.setSelectedFile(translateFile(fileName));
            }

            fileChooser.approveSelection();
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

                File fileInCurrentDir = fileChooser.getFileSystemView().getChild(fileChooser.getCurrentDirectory(), nakedFileName);
                if (fileInCurrentDir != null
                    && fileInCurrentDir.exists()
                    && fileChooser.getFileSystemView().isFileSystem(fileInCurrentDir)) {

                    return fileInCurrentDir;
                } else {
                    return fileChooser.getFileSystemView().createFileObject(nakedFileName);
                }
            } else {
                return fileChooser.getFileSystemView().createFileObject(fileChooser.getCurrentDirectory(), fileName);
            }
        }

        private String[] splitFileNames(final String fileNames) {
            if (fileNames == null) {
                return new String[0];
            }

            List result = new LinkedList();
            int wordBegin = 0;
            boolean insideWord = false;
            for (int i = 0; i < fileNames.length(); i++) {
                char curChar = fileNames.charAt(i);
                if (Character.isWhitespace(curChar) && !insideWord) {
                    continue;
                }

                if (curChar == '\"') {
                    if (!insideWord) {
                        insideWord = true;
                        wordBegin = i;
                    } else {
                        result.add(fileNames.substring(wordBegin, i + 1));
                        insideWord = false;
                    }
                }
            }
            if (insideWord || result.isEmpty()) {
                return new String[] {fileNames};
            } else {
                return (String[])result.toArray(new String[result.size()]);
            }
        }
    }

    protected class CancelSelectionAction extends AbstractAction {
        protected CancelSelectionAction() {
            putValue(AbstractAction.NAME, cancelButtonText);
            putValue(AbstractAction.SHORT_DESCRIPTION, cancelButtonToolTipText);
        }

        public void actionPerformed(final ActionEvent e) {
            fileChooser.cancelSelection();
        }
    }

    protected class ChangeToParentDirectoryAction extends AbstractAction {
        protected ChangeToParentDirectoryAction() {
            putValue(AbstractAction.NAME, upFolderButtonText);
            putValue(AbstractAction.SHORT_DESCRIPTION, upFolderButtonToolTipText);
            putValue(AbstractAction.SMALL_ICON, upFolderIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            fileChooser.changeToParentDirectory();
        }
    }

    protected class GoHomeAction extends AbstractAction {
        protected GoHomeAction() {
            putValue(AbstractAction.NAME, homeFolderButtonText);
            putValue(AbstractAction.SHORT_DESCRIPTION, homeFolderButtonToolTipText);
            putValue(AbstractAction.SMALL_ICON, homeFolderIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            fileChooser.setCurrentDirectory(fileChooser.getFileSystemView().getHomeDirectory());
        }
    }

    protected class NewFolderAction extends AbstractAction {
        protected NewFolderAction() {
            putValue(AbstractAction.NAME, newFolderButtonText);
            putValue(AbstractAction.SHORT_DESCRIPTION, newFolderButtonToolTipText);
            putValue(AbstractAction.SMALL_ICON, newFolderIcon);
        }

        public void actionPerformed(final ActionEvent e) {
            try {
                File newFolder = fileChooser.getFileSystemView().createNewFolder(fileChooser.getCurrentDirectory());
                fileChooser.rescanCurrentDirectory();
                fileChooser.setSelectedFile(newFolder);
            } catch (IOException ioe) {
            }
        }
    }

    protected class UpdateAction extends AbstractAction {
        protected UpdateAction() {
            putValue(AbstractAction.NAME, updateButtonText);
            putValue(AbstractAction.SHORT_DESCRIPTION, updateButtonToolTipText);
        }

        public void actionPerformed(final ActionEvent e) {
            fileChooser.rescanCurrentDirectory();
        }
    }

    protected class DoubleClickListener extends MouseAdapter {
        private JList list;

        public DoubleClickListener(final JList list) {
            this.list = list;
        }

        public void mouseClicked(final MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }

            if (e.getClickCount() != 2) {
                return;
            }
            File file = (File)list.getSelectedValue();
            if (file != null && file.isDirectory()) {
                getFileChooser().setCurrentDirectory(file);
                return;
            }

            approveSelectionAction.actionPerformed(null);
        }
    }

    //Never used by Metal L&F
    protected class SelectionListener implements ListSelectionListener {
        public void valueChanged(final ListSelectionEvent e) {
        }
    }

    private class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent event) {
            String changedProperty = event.getPropertyName();
            if (JFileChooser.DIALOG_TYPE_CHANGED_PROPERTY.equals(changedProperty)) {
                final int type = ((Integer)event.getNewValue()).intValue();
                setButtonsAttrs(type);
            } else if (JFileChooser.DIALOG_TITLE_CHANGED_PROPERTY.equals(changedProperty)) {
                dialogTitleText = (String)event.getNewValue();
            } else if (JFileChooser.APPROVE_BUTTON_TEXT_CHANGED_PROPERTY.equals(changedProperty)) {
                if (event.getNewValue() == null) {
                    setButtonsAttrs(fileChooser.getDialogType());
                } else {
                    approveButtonText = (String)event.getNewValue();
                }
            } else if (StringConstants.COMPONENT_ORIENTATION.equals(changedProperty)) {
                ComponentOrientation co = (ComponentOrientation)event.getNewValue();
                fileChooser.applyComponentOrientation(co);
            }
        }
    }

    // TODO: file flavor should also be supported
    private class FileChooserTransferHandler extends TransferHandler {
        private final String lineSeparator = System.getProperty("line.separator");

        public int getSourceActions(final JComponent c) {
            return COPY;
        }

        protected Transferable createTransferable(final JComponent c) {
            File[] selectedFiles;
            if (fileChooser.isMultiSelectionEnabled()) {
                selectedFiles = fileChooser.getSelectedFiles();
            } else {
                selectedFiles = fileChooser.getSelectedFile() != null
                                ? new File[] {fileChooser.getSelectedFile()}
                                : null;
            }

            if (selectedFiles == null || selectedFiles.length == 0) {
                if (isDirectorySelected()) {
                    selectedFiles = new File[] {getDirectory()};
                } else {
                    return null;
                }
            }

            final File[] transferingFiles = selectedFiles;
            return new Transferable() {
                public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    if (flavor.equals(DataFlavor.stringFlavor)
                        || flavor.equals(DataFlavor.plainTextFlavor)) {

                        StringBuilder content = new StringBuilder();
                        for (int i = 0; i < transferingFiles.length; i++) {
                            content.append(transferingFiles[i].getAbsolutePath());
                            if (i < transferingFiles.length - 1) {
                                content.append(lineSeparator);
                            }
                        }

                        return flavor.equals(DataFlavor.stringFlavor) ? (Object)content.toString()
                                                                      : new StringBufferInputStream(content.toString());
                    }

                    throw new UnsupportedFlavorException(flavor);
                }

                public boolean isDataFlavorSupported(final DataFlavor flavor) {
                    return flavor.equals(DataFlavor.stringFlavor)
                           || flavor.equals(DataFlavor.plainTextFlavor);
                }

                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[] {DataFlavor.stringFlavor, DataFlavor.plainTextFlavor};
                }
            };
        }
    }


    protected Icon directoryIcon;
    protected Icon fileIcon;
    protected Icon computerIcon;
    protected Icon hardDriveIcon;
    protected Icon floppyDriveIcon;
    protected Icon newFolderIcon;
    protected Icon upFolderIcon;
    protected Icon homeFolderIcon;
    protected Icon listViewIcon;
    protected Icon detailsViewIcon;
    protected int saveButtonMnemonic;
    protected int openButtonMnemonic;
    protected int cancelButtonMnemonic;
    protected int updateButtonMnemonic;
    protected int helpButtonMnemonic;
    protected int directoryOpenButtonMnemonic;
    protected String saveButtonText;
    protected String openButtonText;
    protected String cancelButtonText;
    protected String updateButtonText;
    protected String helpButtonText;
    protected String directoryOpenButtonText;
    protected String saveButtonToolTipText;
    protected String openButtonToolTipText;
    protected String cancelButtonToolTipText;
    protected String updateButtonToolTipText;
    protected String helpButtonToolTipText;
    protected String directoryOpenButtonToolTipText;
    private String newFolderButtonText;
    private String newFolderButtonToolTipText;
    private String upFolderButtonText;
    private String upFolderButtonToolTipText;
    private String homeFolderButtonText;
    private String homeFolderButtonToolTipText;

    private JFileChooser fileChooser;
    private BasicDirectoryModel model;
    private FileView fileView;
    private JPanel accessoryPanel;

    private String openDialogTitleText;
    private String saveDialogTitleText;

    private String dialogTitleText;
    private String approveButtonText;
    private String approveButtonToolTipText;
    private int approveButtonMnemonic;
    private boolean isDirectorySelected;

    private PropertyChangeListener propertyChangeHandler;
    private AcceptAllFileFilter acceptAllFileFilter = new AcceptAllFileFilter();
    private String fileName;
    private File directory;

    private Action approveSelectionAction;
    private Action updateAction;
    private Action cancelSelectionAction;
    private Action changeToParentDirAction;
    private Action goHomeAction;
    private Action newFolderAction;

    public BasicFileChooserUI(final JFileChooser fc) {
        this.fileChooser = fc;
    }

    public void installUI(final JComponent c) {
        fileChooser = (JFileChooser)c;

        fileView = new BasicFileView();
        createModel();

        installDefaults(fileChooser);
        installIcons(fileChooser);
        installStrings(fileChooser);
        installComponents(fileChooser);
        installListeners(fileChooser);

        fileChooser.setTransferHandler(new FileChooserTransferHandler());
    }

    public void uninstallUI(final JComponent c) {
        uninstallListeners(fileChooser);
        uninstallComponents(fileChooser);
        uninstallDefaults(fileChooser);
        uninstallIcons(fileChooser);
        uninstallStrings(fileChooser);
        model = null;
        accessoryPanel = null;
    }

    public void installComponents(final JFileChooser fc) {
        accessoryPanel = new JPanel(new BorderLayout());

        approveSelectionAction = new ApproveSelectionAction();
        updateAction = new UpdateAction();
        cancelSelectionAction = new CancelSelectionAction();
        changeToParentDirAction = new ChangeToParentDirectoryAction();
        goHomeAction = new GoHomeAction();
        newFolderAction = new NewFolderAction();
    }

    public void uninstallComponents(final JFileChooser fc) {
        accessoryPanel = null;
    }

    protected void installListeners(final JFileChooser fc) {
        propertyChangeHandler = new PropertyChangeHandler();
        fileChooser.addPropertyChangeListener(propertyChangeHandler);

        Utilities.installKeyboardActions(fc, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "FileChooser.ancestorInputMap", null);
        fc.getActionMap().put("cancelSelection", cancelSelectionAction);
    }

    protected void uninstallListeners(final JFileChooser fc) {
        fileChooser.removePropertyChangeListener(propertyChangeHandler);

        Utilities.uninstallKeyboardActions(fc, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    protected void installDefaults(final JFileChooser fc) {
        if (fc == null) {
            throw new NullPointerException();
        }
 
        helpButtonMnemonic = UIManager.getInt("FileChooser.helpButtonMnemonic");
        directoryOpenButtonMnemonic = UIManager.getInt("FileChooser.directoryOpenButtonMnemonic");

        openDialogTitleText = UIManager.getString("FileChooser.openDialogTitleText");
        saveDialogTitleText = UIManager.getString("FileChooser.saveDialogTitleText");
    }

    protected void installIcons(final JFileChooser fc) {
        directoryIcon = UIManager.getIcon("FileView.directoryIcon");
        fileIcon = UIManager.getIcon("FileView.fileIcon");
        computerIcon = UIManager.getIcon("FileView.computerIcon");
        hardDriveIcon = UIManager.getIcon("FileView.hardDriveIcon");
        floppyDriveIcon = UIManager.getIcon("FileView.floppyDriveIcon");
        newFolderIcon = UIManager.getIcon("FileChooser.newFolderIcon");
        upFolderIcon = UIManager.getIcon("FileChooser.upFolderIcon");
        homeFolderIcon = UIManager.getIcon("FileChooser.homeFolderIcon");
        listViewIcon = UIManager.getIcon("FileChooser.listViewIcon");
        detailsViewIcon = UIManager.getIcon("FileChooser.detailsViewIcon");

        clearIconCache();

    }

    protected void installStrings(final JFileChooser fc) {
        saveButtonText = UIManager.getString("FileChooser.saveButtonText");
        openButtonText = UIManager.getString("FileChooser.openButtonText");
        cancelButtonText = UIManager.getString("FileChooser.cancelButtonText");
        updateButtonText = UIManager.getString("FileChooser.updateButtonText");
        helpButtonText = UIManager.getString("FileChooser.helpButtonText");
        directoryOpenButtonText = UIManager.getString("FileChooser.directoryOpenButtonText");
        newFolderButtonText = UIManager.getString("FileChooser.newFolderAccessibleName");
        upFolderButtonText = UIManager.getString("FileChooser.upFolderAccessibleName");
        homeFolderButtonText = UIManager.getString("FileChooser.homeFolderAccessibleName");

        saveButtonToolTipText = UIManager.getString("FileChooser.saveButtonToolTipText");
        openButtonToolTipText = UIManager.getString("FileChooser.openButtonToolTipText");
        cancelButtonToolTipText = UIManager.getString("FileChooser.cancelButtonToolTipText");
        updateButtonToolTipText = UIManager.getString("FileChooser.updateButtonToolTipText");
        helpButtonToolTipText = UIManager.getString("FileChooser.helpButtonToolTipText");
        directoryOpenButtonToolTipText = UIManager.getString("FileChooser.directoryOpenButtonToolTipText");
        newFolderButtonToolTipText = UIManager.getString("FileChooser.newFolderToolTipText");
        upFolderButtonToolTipText = UIManager.getString("FileChooser.upFolderToolTipText");
        homeFolderButtonToolTipText = UIManager.getString("FileChooser.homeFolderToolTipText");

        setButtonsAttrs(fileChooser.getDialogType());
    }

    protected void uninstallDefaults(final JFileChooser fc) {
        fileChooser.setTransferHandler(null);
    }

    protected void uninstallIcons(final JFileChooser fc) {
        clearIconCache();
    }

    protected void uninstallStrings(final JFileChooser fc) {
    }

    protected void createModel() {
        model = new BasicDirectoryModel(fileChooser);
    }

    public BasicDirectoryModel getModel() {
        return model;
    }

    public PropertyChangeListener createPropertyChangeListener(final JFileChooser fc) {
        return null;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDirectoryName() {
        return null;
    }

    public void setFileName(final String filename) {
        fileName = filename;
    }

    public void setDirectoryName(final String dirname) {
    }

    public void rescanCurrentDirectory(final JFileChooser fc) {
    }

    public void ensureFileIsVisible(final JFileChooser fc, final File f) {
        if (fileChooser != null) {  
            fileChooser.ensureFileIsVisible(f);
        }
    }

    public JFileChooser getFileChooser() {
        return fileChooser;
    }

    public JPanel getAccessoryPanel() {
        return accessoryPanel;
    }

    public void clearIconCache() {
        if (fileView instanceof BasicFileView) {
            ((BasicFileView)fileView).clearIconCache();
        }
    }

    protected MouseListener createDoubleClickListener(final JFileChooser fc, final JList list) {
        return new DoubleClickListener(list);
    }

    public ListSelectionListener createListSelectionListener(final JFileChooser fc) {
        return new SelectionListener();
    }

    protected boolean isDirectorySelected() {
        return isDirectorySelected;
    }

    protected void setDirectorySelected(final boolean b) {
        isDirectorySelected = b;
    }

    protected File getDirectory() {
        return directory;
    }

    protected void setDirectory(final File f) {
        directory = f;
    }

    public FileFilter getAcceptAllFileFilter(final JFileChooser fc) {
        return acceptAllFileFilter;
    }

    public FileView getFileView(final JFileChooser fc) {
        return fileView;
    }

    public String getDialogTitle(final JFileChooser fc) {
        return dialogTitleText;
    }

    protected JButton getApproveButton(final JFileChooser fc) {
        return null;
    }

    public String getApproveButtonToolTipText(final JFileChooser fc) {
	if (fc == null) {
            throw new NullPointerException();
        }

        return approveButtonToolTipText;
    }

    public int getApproveButtonMnemonic(final JFileChooser fc) {
        return approveButtonMnemonic;
    }

    public String getApproveButtonText(final JFileChooser fc) {
        return approveButtonText;
    }

    public Action getNewFolderAction() {
        return newFolderAction;
    }

    public Action getGoHomeAction() {
        return goHomeAction;
    }

    public Action getChangeToParentDirectoryAction() {
        return changeToParentDirAction;
    }

    public Action getApproveSelectionAction() {
        return approveSelectionAction;
    }

    public Action getCancelSelectionAction() {
        return cancelSelectionAction;
    }

    public Action getUpdateAction() {
        return updateAction;
    }

    private void setButtonsAttrs(final int type) {
        if (type == JFileChooser.OPEN_DIALOG) {
            dialogTitleText = openDialogTitleText;
            approveButtonText = openButtonText;
            approveButtonToolTipText = openButtonToolTipText;
        } else if (type == JFileChooser.SAVE_DIALOG) {
            dialogTitleText = saveDialogTitleText;
            approveButtonText = saveButtonText;
            approveButtonToolTipText = saveButtonToolTipText;
        } else {
            dialogTitleText = null;
            approveButtonText = fileChooser.getApproveButtonText();
            approveButtonToolTipText = null;
            approveButtonMnemonic = 0;
        }

        if (approveSelectionAction != null) {
            approveSelectionAction.putValue(Action.NAME, approveButtonText);
            approveSelectionAction.putValue(Action.SHORT_DESCRIPTION, approveButtonToolTipText);
            approveSelectionAction.putValue(Action.MNEMONIC_KEY, new Integer(approveButtonMnemonic));
        }
    }
}
