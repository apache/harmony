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
 * @author Dmitry A. Durnev
 */
package org.apache.harmony.awt.theme;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.SystemColor;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import org.apache.harmony.awt.ChoiceStyle;
import org.apache.harmony.awt.ComponentInternals;

/**
 * DefaultFileDialog
 * Pure Java Implementation of FileDialog
 * for platforms where native file dialog
 * is not used.
 */
public class DefaultFileDialog implements ActionListener, ItemListener {
    final static int SIZE = 400;
    final static Insets BORDER = new Insets(2, 4, 2, 4);
    private final FileDialog fileDialog;
    boolean shown;

    List folders;
    List files;
    TextField path;
    TextField filter;
    TextField fileName;
    Button okButton;
    Button filterButton;
    Button cancelButton;
    Choice dirChoice;

    String filterStr;

    class Separator extends Canvas {
        private static final long serialVersionUID = -9191946485695242726L;

        @Override
        public void paint(Graphics g) {
            g.setColor(SystemColor.controlDkShadow);
            g.drawLine(0, 0, getWidth(), 0);
            g.setColor(SystemColor.controlHighlight);
            g.drawLine(0, 1, getWidth(), 1);
        }
    }

    public DefaultFileDialog(FileDialog fd) {
        fileDialog = fd;
        filterStr = "*"; //$NON-NLS-1$
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == cancelButton) {
           fileDialog.dispose();
        } else if (src == folders) {
            processFolderAction();
        } else if ((src == files) || (src == okButton) || (src == fileName)) {
            selectAndClose();
        } else if (src == path) {
            String absPath = path.getText();
            File file = new File(absPath);
            resetFilter();
            changeDirectory(file);
            fillChoice();
        } else if ((src == filter) || (src == filterButton)) {
            fillLists();
        }
        // TODO handle other actions here:

    }

    private void processFolderAction() {
        int idx = folders.getSelectedIndex();
        File newFolder = new File(path.getText());
        if (idx > 0) {
            newFolder = new File(newFolder, folders.getItem(idx));
            dirChoice.insert(newFolder.getAbsolutePath(), 0);
        } else {
            newFolder = newFolder.getParentFile();
            if (newFolder != null) {
                dirChoice.remove(0);
            }
        }
        changeDirectory(newFolder);
        files.requestFocus();
    }

    private void selectAndClose() {
        String fName = fileName.getText();
        File folder = new File(path.getText());
        if (fName.endsWith(File.separator)) {
            //this is a directory
            fName = null;
        } else {
            File file = new File(fName);
            fName = file.getName();
        }
        fileDialog.setFile(fName);
        fileDialog.setDirectory(folder.getAbsolutePath());
        fileDialog.dispose();
    }

    private void changeDirectory(File file) {
        if ((file == null) || !file.isDirectory()) {
            return;
        }
        String absPath = file.getAbsolutePath();
        String fName = absPath;
        String sep = File.separator;
        if (!fName.endsWith(sep)) {
            fName += sep;
        }
        fileName.setText(fName);
        path.setText(absPath);
        fillLists();

    }

    public void itemStateChanged(ItemEvent e) {
        Object src = e.getSource();
        if (src == files) {
            fileName.setText(files.getSelectedItem());
        } else if (src == dirChoice) {
            resetFilter();
            changeDirectory(new File(dirChoice.getSelectedItem()));
            int selIdx = dirChoice.getSelectedIndex();
            for (int i = 0; i < selIdx; i++) {
                dirChoice.remove(0);
            }
            path.requestFocus();
        }
    }

    public boolean show() {
        if (!shown) {
            fileDialog.setBackground(SystemColor.control);
            // create components & add listeners here
            createComponents();
            addLayoutComponents();
            addListeners();
            fileDialog.setSize(SIZE, SIZE);
            String file = fileDialog.getFile();
            File curFile = ((file != null) ? new File(file) : null);
            File curFolder = ((curFile != null) ?
                         curFile.getParentFile() : getDefaultFolder());
            changeDirectory(curFolder);
            if (curFile != null) {
                fileName.setText(file);
            }
            fillChoice();
            shown = true;
        }
        return true; // call Dialog's show()
    }

    private void fillLists() {
        clearLists();
        updateFilter();
        final File curFolder = new File(path.getText());
        if (curFolder.isDirectory()) {
            File[] allFiles = curFolder.listFiles(fileDialog.getFilenameFilter());
            int count = allFiles.length;
            for (int i = 0; i < count; i++) {
                File f = allFiles[i];
                String fName = f.getName();
                if (f.isDirectory()) {
                    folders.add(fName);
                }
                if (f.isFile()) {
                    if (applyFilter(fName)) {
                        files.add(fName);
                    }
                }
            }
        }
    }

    private boolean applyFilter(String name) {
        // TODO: apply filter from "filter" text field
        return name.matches(filterStr);
    }

    private void updateFilter() {
        filterStr = filter.getText().replaceAll("\\.", "\\\\."); //$NON-NLS-1$ //$NON-NLS-2$
        filterStr = filterStr.replaceAll("\\*", ".*"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void resetFilter() {
        filterStr = "*"; //$NON-NLS-1$
        filter.setText(filterStr);
        updateFilter();
    }

    private void clearLists() {
        if (folders.getItemCount() > 0) {
            folders.removeAll();
        }
        folders.add(".."); //$NON-NLS-1$
        if (files.getItemCount() > 0) {
            files.removeAll();
        }
    }

    private File getDefaultFolder() {
        return new File(org.apache.harmony.awt.Utils.getSystemProperty("user.dir")); //$NON-NLS-1$
    }

    private void addListeners() {
        folders.addItemListener(this);
        folders.addActionListener(this);

        files.addItemListener(this);
        files.addActionListener(this);
        okButton.addActionListener(this);
        filterButton.addActionListener(this);
        cancelButton.addActionListener(this);
        path.addActionListener(this);
        fileName.addActionListener(this);
        dirChoice.addItemListener(this);
        filter.addActionListener(this);

    }

    private void createComponents() {
        path = new TextField();
        fileName = new TextField();
        dirChoice = createCustomChoice();
        filter = new TextField(filterStr);
        folders = new List();
        files = new List();
        okButton = new Button("OK"); //$NON-NLS-1$
        filterButton = new Button("Filter"); //$NON-NLS-1$
        cancelButton = new Button("Cancel"); //$NON-NLS-1$
    }

    private Choice createCustomChoice() {
        ChoiceStyle style = new ChoiceStyle() {

            public int getPopupX(int x, int width, int choiceWidth,
                                 int screenWidth) {
                int popupX = x;
                if (width > choiceWidth) {
                    popupX -= (width - choiceWidth);
                }
                popupX = Math.max(0, popupX);
                if (popupX + width > screenWidth) {
                    popupX = screenWidth - width;
                }
                return popupX;
            }

            public int getPopupWidth(int choiceWidth) {
                int hGap = ((BorderLayout) path.getParent().getLayout()).getHgap();
                return choiceWidth + path.getWidth() + hGap;
            }

        };
        return ComponentInternals.getComponentInternals().createCustomChoice(style);
    }

    private void addLayoutComponents() {
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = BORDER;
        fileDialog.setLayout(gbl);

        addPath(gbc);

        fileDialog.add(new Label("Filter"), gbc); //$NON-NLS-1$
        fileDialog.add(filter, gbc);

        addLists(gbc);

        gbc.weighty = 0.0;
        gbc.weightx = 0.0;
        fileDialog.add(new Label("Enter file name:"), gbc); //$NON-NLS-1$
        fileDialog.add(fileName, gbc);

        Separator sep = new Separator();
        sep.setMinimumSize(new Dimension(10, 3));
        gbc.insets = new Insets(2, 0, 2, 0);
        fileDialog.add(sep, gbc);

        Panel buttonPanel = new Panel(new GridBagLayout());
        fileDialog.add(buttonPanel, gbc);
        addButtons(buttonPanel);
    }

    private void addPath(GridBagConstraints gbc) {
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        fileDialog.add(new Label("Enter path or folder name:"), gbc); //$NON-NLS-1$
        Panel pathPanel = new Panel(new BorderLayout());
        pathPanel.add(path); // CENTER
        int cSize = DefaultScrollbar.BUTTON_SIZE + BORDER.top;
        dirChoice.setPreferredSize(new Dimension(cSize, cSize));
        pathPanel.add(dirChoice, BorderLayout.EAST);
        gbc.weightx = 0.0;
        fileDialog.add(pathPanel, gbc);
    }

    private void addLists(GridBagConstraints gbc) {
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        fileDialog.add(new Label("Folders"), gbc); //$NON-NLS-1$
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        fileDialog.add(new Label("Files"), gbc); //$NON-NLS-1$
        gbc.gridwidth = 1;
        gbc.weighty = 1.0;
        fileDialog.add(folders, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        fileDialog.add(files, gbc);
    }

    private void addButtons(Panel p) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.insets = BORDER;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.WEST;
        p.add(okButton, gbc);
        gbc.anchor = GridBagConstraints.CENTER;
        p.add(filterButton, gbc);
        gbc.anchor = GridBagConstraints.EAST;
        p.add(cancelButton, gbc);
    }

    private void fillChoice() {
        // fill Choice list with absolute paths
        dirChoice.removeAll();
        File folder = new File(path.getText());
        while ((folder != null) && folder.isDirectory()) {
            dirChoice.add(folder.getAbsolutePath());
            folder = folder.getParentFile();
        }
    }

}
