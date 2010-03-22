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
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.FileChooserUI;
import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>JFileChooser</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class JFileChooser extends JComponent implements Accessible {
    private static final long serialVersionUID = 1049148651561366602L;

    public static final String ACCEPT_ALL_FILE_FILTER_USED_CHANGED_PROPERTY = "acceptAllFileFilterUsedChanged";

    public static final String ACCESSORY_CHANGED_PROPERTY = "AccessoryChangedProperty";

    public static final String APPROVE_BUTTON_MNEMONIC_CHANGED_PROPERTY = "ApproveButtonMnemonicChangedProperty";

    public static final String APPROVE_BUTTON_TEXT_CHANGED_PROPERTY = "ApproveButtonTextChangedProperty";

    public static final String APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY = "ApproveButtonToolTipTextChangedProperty";

    public static final String CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY = "ChoosableFileFilterChangedProperty";

    public static final String CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY = "ControlButtonsAreShownChangedProperty";

    public static final String DIALOG_TITLE_CHANGED_PROPERTY = "DialogTitleChangedProperty";

    public static final String DIALOG_TYPE_CHANGED_PROPERTY = "DialogTypeChangedProperty";

    public static final String DIRECTORY_CHANGED_PROPERTY = "directoryChanged";

    public static final String FILE_FILTER_CHANGED_PROPERTY = "fileFilterChanged";

    public static final String FILE_HIDING_CHANGED_PROPERTY = "FileHidingChanged";

    public static final String FILE_SELECTION_MODE_CHANGED_PROPERTY = "fileSelectionChanged";

    public static final String FILE_SYSTEM_VIEW_CHANGED_PROPERTY = "FileSystemViewChanged";

    public static final String FILE_VIEW_CHANGED_PROPERTY = "fileViewChanged";

    public static final String MULTI_SELECTION_ENABLED_CHANGED_PROPERTY = "MultiSelectionEnabledChangedProperty";

    public static final String SELECTED_FILE_CHANGED_PROPERTY = "SelectedFileChangedProperty";

    public static final String SELECTED_FILES_CHANGED_PROPERTY = "SelectedFilesChangedProperty";

    public static final String APPROVE_SELECTION = "ApproveSelection";

    public static final String CANCEL_SELECTION = "CancelSelection";

    public static final int ERROR_OPTION = -1;

    public static final int APPROVE_OPTION = 0;

    public static final int CANCEL_OPTION = 1;

    public static final int OPEN_DIALOG = 0;

    public static final int SAVE_DIALOG = 1;

    public static final int CUSTOM_DIALOG = 2;

    public static final int FILES_ONLY = 0;

    public static final int DIRECTORIES_ONLY = 1;

    public static final int FILES_AND_DIRECTORIES = 2;

    private static final String UI_CLASS_ID = "FileChooserUI";

    private File currentDirectory;

    private FileSystemView fileSystemView;

    private FileView fileView;

    private boolean dragEnabled;

    private boolean controlButtonsAreShown = true;

    private int dialogType = OPEN_DIALOG;

    private String dialogTitle;

    private boolean fileHidingEnabled = true;

    private FileFilter fileFilter;

    private boolean multiSelectionEnabled;

    private File selectedFile;

    private JComponent accessory;

    private File[] selectedFiles = new File[0];

    private Collection<FileFilter> choosableFileFilters = new LinkedList<FileFilter>();

    private String approveButtonText;

    private String approveButtonToolTipText;

    private int approveButtonMnemonic;

    private int fileSelectionMode = FILES_ONLY;

    private boolean acceptAllFileFilterUsed = true;

    private int chooseResult;

    private JDialog fileChooserDialog;

    protected class AccessibleJFileChooser extends AccessibleJComponent {
        private static final long serialVersionUID = -6919995775059834138L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FILE_CHOOSER;
        }
    }

    public JFileChooser() {
        this((String) null);
    }

    public JFileChooser(final String currentDirectoryPath) {
        this(currentDirectoryPath, null);
    }

    public JFileChooser(final File currentDirectory) {
        this(currentDirectory, null);
    }

    public JFileChooser(final FileSystemView fsv) {
        this((File) null, fsv);
    }

    public JFileChooser(final String currentDirectoryPath, final FileSystemView fsv) {
        this(currentDirectoryPath != null ? new File(currentDirectoryPath) : null, fsv);
    }

    public JFileChooser(final File currentDirectory, final FileSystemView fsv) {
        setup(fsv);
        setCurrentDirectory(currentDirectory);
    }

    public boolean accept(final File file) {
        return fileFilter != null ? fileFilter.accept(file) : true;
    }

    public void addActionListener(final ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(final ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    public void addChoosableFileFilter(final FileFilter filter) {
        FileFilter[] oldValue = toFileFilterArray(choosableFileFilters);
        if (choosableFileFilters.contains(filter)) {
            return;
        }
        choosableFileFilters.add(filter);
        firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, oldValue,
                toFileFilterArray(choosableFileFilters));
        setFileFilter(filter);
    }

    public boolean removeChoosableFileFilter(final FileFilter filter) {
        FileFilter[] oldValue = toFileFilterArray(choosableFileFilters);
        boolean result = choosableFileFilters.remove(filter);
        firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, oldValue,
                toFileFilterArray(choosableFileFilters));
        return result;
    }

    public void resetChoosableFileFilters() {
        FileFilter[] oldValue = toFileFilterArray(choosableFileFilters);
        choosableFileFilters.clear();
        if (isAcceptAllFileFilterUsed()) {
            choosableFileFilters.add(getAcceptAllFileFilter());
        }
        firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, oldValue,
                toFileFilterArray(choosableFileFilters));
    }

    public FileFilter[] getChoosableFileFilters() {
        return toFileFilterArray(choosableFileFilters);
    }

    public void approveSelection() {
        chooseResult = APPROVE_OPTION;
        fireActionPerformed(APPROVE_SELECTION);
        if (fileChooserDialog != null) {
            fileChooserDialog.dispose();
        }
    }

    public void cancelSelection() {
        chooseResult = CANCEL_OPTION;
        fireActionPerformed(CANCEL_SELECTION);
        if (fileChooserDialog != null) {
            fileChooserDialog.dispose();
        }
    }

    public void changeToParentDirectory() {
        if (currentDirectory != null && !fileSystemView.isRoot(currentDirectory)) {
            setCurrentDirectory(fileSystemView.getParentDirectory(currentDirectory));
        }
    }

    public void ensureFileIsVisible(final File f) {
        getUI().ensureFileIsVisible(this, f);
    }

    public FileFilter getAcceptAllFileFilter() {
        return getUI().getAcceptAllFileFilter(this);
    }

    public boolean isAcceptAllFileFilterUsed() {
        return acceptAllFileFilterUsed;
    }

    public void setAcceptAllFileFilterUsed(final boolean accept) {
        boolean oldValue = acceptAllFileFilterUsed;
        acceptAllFileFilterUsed = accept;
        firePropertyChange(ACCEPT_ALL_FILE_FILTER_USED_CHANGED_PROPERTY, oldValue, accept);
        if (!accept) {
            choosableFileFilters.remove(getAcceptAllFileFilter());
        } else if (!choosableFileFilters.contains(getAcceptAllFileFilter())) {
            choosableFileFilters.add(getAcceptAllFileFilter());
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJFileChooser();
        }
        return accessibleContext;
    }

    public JComponent getAccessory() {
        return accessory;
    }

    public void setAccessory(final JComponent accessory) {
        JComponent oldValue = this.accessory;
        this.accessory = accessory;
        firePropertyChange(ACCESSORY_CHANGED_PROPERTY, oldValue, accessory);
    }

    public int getApproveButtonMnemonic() {
        return approveButtonMnemonic;
    }

    public void setApproveButtonMnemonic(final char mnemonic) {
        setApproveButtonMnemonic(Utilities.keyCharToKeyCode(mnemonic));
    }

    public void setApproveButtonMnemonic(final int mnemonic) {
        int oldValue = approveButtonMnemonic;
        approveButtonMnemonic = mnemonic;
        firePropertyChange(APPROVE_BUTTON_MNEMONIC_CHANGED_PROPERTY, oldValue, mnemonic);
    }

    public String getApproveButtonText() {
        return approveButtonText;
    }

    public void setApproveButtonText(final String approveButtonText) {
        String oldValue = this.approveButtonText;
        this.approveButtonText = approveButtonText;
        firePropertyChange(APPROVE_BUTTON_TEXT_CHANGED_PROPERTY, oldValue, approveButtonText);
        if (approveButtonText != null
                && !approveButtonText.equals(getUI().getApproveButtonText(this))) {
            //            setDialogType(CUSTOM_DIALOG);
        }
    }

    public String getApproveButtonToolTipText() {
        return approveButtonToolTipText;
    }

    public void setApproveButtonToolTipText(final String toolTipText) {
        String oldValue = this.approveButtonToolTipText;
        this.approveButtonToolTipText = toolTipText;
        firePropertyChange(APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY, oldValue, toolTipText);
    }

    public boolean getControlButtonsAreShown() {
        return controlButtonsAreShown;
    }

    public void setControlButtonsAreShown(final boolean shown) {
        boolean oldValue = controlButtonsAreShown;
        controlButtonsAreShown = shown;
        firePropertyChange(CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY, oldValue, shown);
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(final File dir) {
        File oldValue = currentDirectory;
        if (dir != null && dir.exists()) {
            if (!dir.isDirectory()) {
                File parent = fileSystemView.getParentDirectory(dir);
                while (parent != null && !isTraversable(parent)
                        && !fileSystemView.isRoot(parent)) {
                    parent = fileSystemView.getParentDirectory(parent);
                }
                if (parent != null) {
                    currentDirectory = parent;
                }
            } else {
                currentDirectory = dir;
            }
        }
        if (currentDirectory == null) {
            currentDirectory = fileSystemView.getDefaultDirectory();
        }
        firePropertyChange(DIRECTORY_CHANGED_PROPERTY, oldValue, currentDirectory);
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(final String dialogTitle) {
        String oldValue = this.dialogTitle;
        this.dialogTitle = dialogTitle;
        firePropertyChange(DIALOG_TITLE_CHANGED_PROPERTY, oldValue, dialogTitle);
    }

    public int getDialogType() {
        return dialogType;
    }

    public void setDialogType(final int dialogType) {
        if (dialogType != OPEN_DIALOG && dialogType != SAVE_DIALOG
                && dialogType != CUSTOM_DIALOG) {
            throw new IllegalArgumentException(Messages.getString("swing.11")); //$NON-NLS-1$
        }
        int oldValue = this.dialogType;
        this.dialogType = dialogType;
        firePropertyChange(DIALOG_TYPE_CHANGED_PROPERTY, oldValue, dialogType);
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    public void setDragEnabled(final boolean enabled) {
        dragEnabled = enabled;
    }

    public FileFilter getFileFilter() {
        return fileFilter;
    }

    public void setFileFilter(final FileFilter filter) {
        FileFilter oldValue = fileFilter;
        fileFilter = filter;
        firePropertyChange(FILE_FILTER_CHANGED_PROPERTY, oldValue, filter);
    }

    public int getFileSelectionMode() {
        return fileSelectionMode;
    }

    public void setFileSelectionMode(final int mode) {
        if (mode != FILES_ONLY && mode != DIRECTORIES_ONLY && mode != FILES_AND_DIRECTORIES) {
            throw new IllegalArgumentException(Messages.getString("swing.12")); //$NON-NLS-1$
        }
        int oldValue = fileSelectionMode;
        fileSelectionMode = mode;
        firePropertyChange(FILE_SELECTION_MODE_CHANGED_PROPERTY, oldValue, mode);
    }

    public FileSystemView getFileSystemView() {
        return fileSystemView;
    }

    public void setFileSystemView(final FileSystemView fsv) {
        FileSystemView oldValue = fileSystemView;
        fileSystemView = fsv;
        firePropertyChange(FILE_SYSTEM_VIEW_CHANGED_PROPERTY, oldValue, fsv);
    }

    public FileView getFileView() {
        return fileView;
    }

    public void setFileView(final FileView fileView) {
        FileView oldValue = this.fileView;
        this.fileView = fileView;
        firePropertyChange(FILE_VIEW_CHANGED_PROPERTY, oldValue, fileView);
    }

    public Icon getIcon(final File f) {
        Icon result = fileView != null ? fileView.getIcon(f) : null;
        return result != null ? result : getUI().getFileView(this).getIcon(f);
    }

    public String getDescription(final File f) {
        String result = fileView != null ? fileView.getDescription(f) : null;
        return result != null ? result : getUI().getFileView(this).getDescription(f);
    }

    public String getTypeDescription(final File f) {
        String result = fileView != null ? fileView.getTypeDescription(f) : null;
        return result != null ? result : getUI().getFileView(this).getTypeDescription(f);
    }

    public String getName(final File f) {
        String result = fileView != null ? fileView.getName(f) : null;
        return result != null ? result : getUI().getFileView(this).getName(f);
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedFile(final File file) {
        File oldValue = selectedFile;
        selectedFile = file;
        firePropertyChange(SELECTED_FILE_CHANGED_PROPERTY, oldValue, file);
    }

    public File[] getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(final File[] selectedFiles) {
        File[] oldValue = this.selectedFiles;
        this.selectedFiles = new File[selectedFiles.length];
        System.arraycopy(selectedFiles, 0, this.selectedFiles, 0, selectedFiles.length);
        if (selectedFiles.length > 0) {
            setSelectedFile(selectedFiles[0]);
        } else {
            setSelectedFile(null);
        }
        firePropertyChange(SELECTED_FILES_CHANGED_PROPERTY, oldValue, selectedFiles);
    }

    public FileChooserUI getUI() {
        return (FileChooserUI) ui;
    }

    @Override
    public void updateUI() {
        setUI(UIManager.getUI(this));
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public boolean isDirectorySelectionEnabled() {
        return fileSelectionMode == DIRECTORIES_ONLY
                || fileSelectionMode == FILES_AND_DIRECTORIES;
    }

    public boolean isFileHidingEnabled() {
        return fileHidingEnabled;
    }

    public void setFileHidingEnabled(final boolean enabled) {
        boolean oldValue = fileHidingEnabled;
        fileHidingEnabled = enabled;
        firePropertyChange(FILE_HIDING_CHANGED_PROPERTY, oldValue, enabled);
    }

    public boolean isFileSelectionEnabled() {
        return fileSelectionMode == FILES_ONLY || fileSelectionMode == FILES_AND_DIRECTORIES;
    }

    public boolean isMultiSelectionEnabled() {
        return multiSelectionEnabled;
    }

    public void setMultiSelectionEnabled(final boolean enabled) {
        boolean oldValue = multiSelectionEnabled;
        multiSelectionEnabled = enabled;
        firePropertyChange(MULTI_SELECTION_ENABLED_CHANGED_PROPERTY, oldValue, enabled);
    }

    public boolean isTraversable(final File f) {
        Boolean result = fileView != null ? fileView.isTraversable(f) : getUI().getFileView(
                this).isTraversable(f);
        return result != null ? result.booleanValue() : true;
    }

    public void rescanCurrentDirectory() {
        getUI().rescanCurrentDirectory(this);
    }

    public int showDialog(final Component parent, final String approveButtonText) {
        setApproveButtonText(approveButtonText);
        setDialogTitle(approveButtonText);
        fileChooserDialog = createDialog(parent);
        chooseResult = ERROR_OPTION;
        fileChooserDialog.setVisible(true);
        return chooseResult;
    }

    public int showOpenDialog(final Component parent) {
        setDialogType(OPEN_DIALOG);
        return showDialog(parent, getUI().getApproveButtonText(this));
    }

    public int showSaveDialog(final Component parent) {
        setDialogType(SAVE_DIALOG);
        return showDialog(parent, getUI().getApproveButtonText(this));
    }

    protected void fireActionPerformed(final String command) {
        ActionListener[] listeners = getActionListeners();
        if (listeners.length > 0) {
            ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].actionPerformed(event);
            }
        }
    }

    protected JDialog createDialog(final Component parent) throws HeadlessException {
        Window ancestingWindow = parent instanceof Window ? (Window) parent : SwingUtilities
                .getWindowAncestor(parent);
        final JDialog result;
        if (ancestingWindow instanceof Frame) {
            result = new JDialog((Frame) ancestingWindow);
        } else if (ancestingWindow instanceof Dialog) {
            result = new JDialog((Dialog) ancestingWindow);
        } else {
            result = new JDialog();
        }
        result.setModal(true);
        result.setLocationRelativeTo(parent);
        result.setTitle(getDialogTitle());
        result.getContentPane().add(this);
        if (JDialog.isDefaultLookAndFeelDecorated()) {
            result.getRootPane().setWindowDecorationStyle(JRootPane.FILE_CHOOSER_DIALOG);
        }
        result.pack();
        return result;
    }

    protected void setup(final FileSystemView view) {
        setFileSystemView(view != null ? view : FileSystemView.getFileSystemView());
        updateUI();
        if (isAcceptAllFileFilterUsed()) {
            addChoosableFileFilter(getAcceptAllFileFilter());
        }
        setName(null);
    }

    private FileFilter[] toFileFilterArray(final Collection<FileFilter> fileFilterList) {
        return fileFilterList.toArray(new FileFilter[fileFilterList.size()]);
    }
}
