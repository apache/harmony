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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.JFileChooser;
import javax.swing.event.ListDataEvent;
import javax.swing.filechooser.FileFilter;

public class BasicDirectoryModel extends AbstractListModel implements PropertyChangeListener {
    private Vector<java.io.File> fileList;
    private JFileChooser fc;
    private final Comparator<File> fileComparator = new Comparator<File>() {
        public int compare(final File o1, final File o2) {
            return lt(o1, o2) ? -1 : 1;
        }
    };

    public Vector<java.io.File> getFiles() {
        return fileList;
    }

    public BasicDirectoryModel(final JFileChooser filechooser) {
        fc = filechooser;
        fc.addPropertyChangeListener(this);
        fileList = new Vector<java.io.File>();
    }

    public Object getElementAt(final int index) {
        return fileList.get(index);
    }

    public int getSize() {
        return fileList.size();
    }

    public void propertyChange(final PropertyChangeEvent event) {
        String changedProperty = event.getPropertyName();
        if (JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(changedProperty)
            || JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY.equals(changedProperty)
            || JFileChooser.MULTI_SELECTION_ENABLED_CHANGED_PROPERTY.equals(changedProperty)) {

            fc.rescanCurrentDirectory();
        }
    }

    public void invalidateFileCache() {
    }

    public Vector<java.io.File> getDirectories() {
        return null;
    }

    public void validateFileCache() {
        fileList.clear();

        if (fc.getCurrentDirectory() == null || !fc.getCurrentDirectory().isDirectory()) {
            fireContentsChanged();
            return;
        }

        File[] files = fc.getFileSystemView().getFiles(fc.getCurrentDirectory(), fc.isFileHidingEnabled());
        if (files == null || files.length == 0) {
            fireContentsChanged();
            return;
        }

        FileFilter filter = fc.getFileFilter();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if ((file.isDirectory() && (fc.isDirectorySelectionEnabled() || fc.isTraversable(file))
                 || file.isFile() && fc.isFileSelectionEnabled())
                && (filter == null || filter.accept(file))) {

                fileList.add(file);
            }
        }
        sort(fileList);

        fireContentsChanged();
    }

    public boolean renameFile(final File oldFile, final File newFile) {
        if (oldFile.renameTo(newFile)) {
            validateFileCache();
            fc.setSelectedFile(newFile);

            return true;
        }

        return false;
    }

    public void fireContentsChanged() {
        fireContentsChanged(this, -1, -1);
    }

    public boolean contains(final Object o) {
        return fileList.contains(o);
    }

    public int indexOf(final Object o) {
        return fileList.indexOf(o);
    }

    public void intervalAdded(final ListDataEvent e) {
    }

    public void intervalRemoved(final ListDataEvent e) {
    }

    protected void sort(final Vector<? extends java.io.File> v) {
        Collections.sort(v, fileComparator);
    }

    protected boolean lt(final File file1, final File file2) {
        if (file1.isDirectory() && !file2.isDirectory()) {
            return true;
        }
        if (!file1.isDirectory() && file2.isDirectory()) {
            return false;
        }

        return fileList.indexOf(file1) < fileList.indexOf(file2);
    }
}
