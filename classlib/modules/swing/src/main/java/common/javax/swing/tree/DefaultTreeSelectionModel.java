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
* @author Alexander T. Simbirtsev
*/
package javax.swing.tree;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class DefaultTreeSelectionModel implements Cloneable, Serializable, TreeSelectionModel {

    public static final String SELECTION_MODE_PROPERTY = "selectionMode";

    protected SwingPropertyChangeSupport changeSupport;
    protected EventListenerList listenerList = new EventListenerList();

    protected DefaultListSelectionModel listSelectionModel = new DefaultListSelectionModel();
    protected TreePath[] selection;
    protected transient RowMapper rowMapper;
    protected int selectionMode = DISCONTIGUOUS_TREE_SELECTION;
    protected TreePath leadPath;
    protected int leadIndex = -1;
    protected int leadRow = -1;

    private static final TreePath[] singlePathArray = new TreePath[] {null};

    private static final int CLEAR_SELECTED_PATHS = 1;
    private static final int REMOVE_SELECTED_PATHS = 2;
    private static final int ADD_SELECTED_PATHS = 3;
    private static final int SET_SELECTED_PATHS = 4;

    public void addPropertyChangeListener(final PropertyChangeListener l) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(final PropertyChangeListener l) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(l);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return (changeSupport != null) ? changeSupport.getPropertyChangeListeners()
                                       : new PropertyChangeListener[0];
    }

    public <T extends EventListener> T[] getListeners(final Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    public void addTreeSelectionListener(final TreeSelectionListener l) {
        listenerList.add(TreeSelectionListener.class, l);
    }

    public void removeTreeSelectionListener(final TreeSelectionListener l) {
        listenerList.remove(TreeSelectionListener.class, l);
    }

    public TreeSelectionListener[] getTreeSelectionListeners() {
        return (TreeSelectionListener[])listenerList.getListeners(TreeSelectionListener.class);
    }

    protected void fireValueChanged(final TreeSelectionEvent e) {
        TreeSelectionListener[] listeners = getTreeSelectionListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].valueChanged(e);
        }
    }

    public void setSelectionPath(final TreePath path) {
        setSelectionPaths(getSinglePathArray(path));
    }

    public void addSelectionPath(final TreePath path) {
        addSelectionPaths(getSinglePathArray(path));
    }

    public TreePath getSelectionPath() {
        return !isSelectionEmpty() ? selection[0] : null;
    }

    public void removeSelectionPath(final TreePath path) {
        removeSelectionPaths(getSinglePathArray(path));
    }

    public void setSelectionPaths(final TreePath[] paths) {
        if (paths == null) {
            clearSelection();
            return;
        }

        if (selectionMode == CONTIGUOUS_TREE_SELECTION && !arePathsContiguous(paths) ||
            selectionMode == SINGLE_TREE_SELECTION && paths.length > 1) {
            setSelectionPath(paths[0]);
            return;
        }

        modifyPathsSelection(paths, SET_SELECTED_PATHS);
        insureUniqueness();
    }

    public void addSelectionPaths(final TreePath[] paths) {
        if (paths == null) {
            return;
        }

        if (!canPathsBeAdded(paths)) {
            setSelectionPaths(getContigousPathsArray(paths));
            return;
        }

        modifyPathsSelection(paths, ADD_SELECTED_PATHS);
        insureUniqueness();
    }

    public TreePath[] getSelectionPaths() {
        return (((selection != null) && (selection.length > 0)) ? selection : null) ;
    }

    public void removeSelectionPaths(final TreePath[] paths) {
        if (paths == null || isSelectionEmpty()) {
            return;
        }

        if (!canPathsBeRemoved(paths)) {
            clearSelection();
            return;
        }

        modifyPathsSelection(paths, REMOVE_SELECTED_PATHS);
    }

    public boolean isPathSelected(final TreePath path) {
        if (path == null || Utilities.isEmptyArray(selection)) {
            return false;
        }

        for (int i = 0; i < selection.length; i++) {
            if (path.equals(selection[i])) {
                return true;
            }
        }

        return false;
    }

    public void clearSelection() {
        modifyPathsSelection(null, CLEAR_SELECTED_PATHS);
    }

    public TreePath getLeadSelectionPath() {
        return leadPath;
    }

    public int getLeadSelectionRow() {
        return leadRow;
    }

    public int getMaxSelectionRow() {
        return listSelectionModel.getMaxSelectionIndex();
    }

    public int getMinSelectionRow() {
        return listSelectionModel.getMinSelectionIndex();
    }

    public int getSelectionCount() {
        return (selection != null) ? selection.length : 0;
    }

    public int[] getSelectionRows() {
        if (rowMapper == null || isSelectionEmpty()) {
            return null;
        }
        int[] ret = getCleanedSortedRowsArray(rowMapper.getRowsForPaths(getSelectionPaths()));
        return (ret.length == 0 ? null : ret);
    }

    public boolean isRowSelected(final int i) {
        return listSelectionModel.isSelectedIndex(i);
    }

    public boolean isSelectionEmpty() {
        return Utilities.isEmptyArray(selection);
    }

    public void resetRowSelection() {
        listSelectionModel.clearSelection();
        leadRow = -1;
        if (rowMapper == null || isSelectionEmpty()) {
            return;
        }
        int[] rows = rowMapper.getRowsForPaths(getSelectionPaths());
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] != -1) {
                listSelectionModel.addSelectionInterval(rows[i], rows[i]);
            }
        }
        if (leadPath != null ) {
            leadRow = getPathIndex(leadPath);
        }
        insureRowContinuity();
    }

    public void setRowMapper(final RowMapper mapper) {
        rowMapper = mapper;
        resetRowSelection();
    }

    public RowMapper getRowMapper() {
        return rowMapper;
    }

    public int getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(final int mode) {
        int oldValue = selectionMode;
        if (mode != SINGLE_TREE_SELECTION && mode != CONTIGUOUS_TREE_SELECTION &&
            mode != DISCONTIGUOUS_TREE_SELECTION) {
            selectionMode = DISCONTIGUOUS_TREE_SELECTION;
        } else {
            selectionMode = mode;
        }

        insureRowContinuity();
        if (changeSupport != null) {
            changeSupport.firePropertyChange(SELECTION_MODE_PROPERTY, oldValue, selectionMode);
        }
    }

    public String toString() {
        String result = getClass().getName() + " " + hashCode();
        String paths = "";
        if (!isSelectionEmpty()) {
            for (int i = 0; i < selection.length; i++) {
                paths += selection[i];
            }
        }
        result += " [ " + paths + " ]";
        return result;
    }

    public Object clone() throws CloneNotSupportedException {
        final DefaultTreeSelectionModel result = new DefaultTreeSelectionModel();
        result.selection = (selection != null) ? (TreePath[])selection.clone() : null;
        result.leadIndex = leadIndex;
        result.leadRow = leadRow;
        result.leadPath = leadPath;
        result.rowMapper = rowMapper;
        result.selectionMode = selectionMode;
        result.listSelectionModel = (listSelectionModel != null) ? (DefaultListSelectionModel)listSelectionModel.clone() : null;

        return result;
    }

    protected void insureRowContinuity() {
        if (selectionMode == SINGLE_TREE_SELECTION) {
            if (getSelectionCount() > 1) {
                setSelectionPath(getSelectionPath());
            }
        } else if (selectionMode == CONTIGUOUS_TREE_SELECTION) {
            if (!arePathsContiguous(getSelectionPaths())) {
                setSelectionPaths(getContigousPathsArray(selection));
            }
        }
    }

    protected boolean arePathsContiguous(final TreePath[] paths) {
        if (rowMapper == null) {
            return true;
        }

        if (paths == null || paths.length <= 1) {
            return true;
        }

        return areRowsContigous(rowMapper.getRowsForPaths(paths));
    }

    protected boolean canPathsBeAdded(final TreePath[] paths) {
        if (paths == null || isSelectionModifiableAnyhow()) {
            return true;
        }

        TreePath[] united = new TreePath[selection.length + paths.length];
        System.arraycopy(selection, 0, united, 0, selection.length);
        System.arraycopy(paths, 0, united, selection.length, paths.length);
        return arePathsContiguous(united);
    }

    protected boolean canPathsBeRemoved(final TreePath[] paths) {
        if (paths == null || isSelectionModifiableAnyhow()) {
            return true;
        }

        HashSet rows = arrayToHashSet(selection);
        for (int i = 0; i < paths.length; i++) {
            rows.remove(paths[i]);
        }
        return arePathsContiguous(hashSetToTreePathArray(rows));
    }

    protected void notifyPathChange(final Vector<PathPlaceHolder> changedPaths,
                                    final TreePath oldLeadSelection) {

        if (changedPaths == null || changedPaths.size() == 0 ){
            return;
        }

        TreeSelectionEvent event = new TreeSelectionEvent(this,
                                                          PathPlaceHolder.getPathsArray(changedPaths),
                                                          PathPlaceHolder.getAreNewArray(changedPaths),
                                                          oldLeadSelection,
                                                          getLeadSelectionPath());
        fireValueChanged(event);
    }

    protected void updateLeadIndex() {
        leadIndex = -1;
        if (leadPath == null || isSelectionEmpty()) {
            return;
        }

        leadIndex = getPathIndex(leadPath);
    }

    protected void insureUniqueness() {
    }

    private void modifyPathsSelection(final TreePath[] paths,
                                      final int modifyMode) {

        final TreePath[] oldSelection = selection;
        final TreePath oldLeadSelection = getLeadSelectionPath();

        updateSelectionArray(paths, modifyMode);
        leadPath = getNewLeadPath(modifyMode);

        updateLeadIndex();
        resetRowSelection();
        notifyPathChange(createPlaceHolders(oldSelection, selection), oldLeadSelection);
    }

    private void updateSelectionArray(final TreePath[] paths, final int modifyMode) {
        switch (modifyMode) {
        case CLEAR_SELECTED_PATHS:
            selection = null;
            break;
        case REMOVE_SELECTED_PATHS:
            final HashSet selectionHashSet = arrayToHashSet(selection);
            selectionHashSet.removeAll(arrayToHashSet(paths));
            selection = hashSetToTreePathArray(selectionHashSet);
            break;
        case SET_SELECTED_PATHS:
            selection = getUniquePathArray(paths);
            break;
        case ADD_SELECTED_PATHS:
            TreePath[] combined = selection != null ? new TreePath[paths.length + selection.length] : paths;
            if (selection != null) {
                System.arraycopy(selection, 0, combined, 0, selection.length);
                System.arraycopy(paths, 0, combined, selection.length, paths.length);
            }
            selection = getUniquePathArray(combined);
            break;
        default:
            throw new IllegalArgumentException(Messages.getString("swing.B0")); //$NON-NLS-1$
        }
    }

    private Vector<PathPlaceHolder> createPlaceHolders(final TreePath[] oldSelection,
                                      final TreePath[] newSelection) {

        final HashSet addedElements = arrayToHashSet(newSelection);
        final HashSet removedElements = arrayToHashSet(oldSelection);
        addedElements.removeAll(removedElements);
        removedElements.removeAll(arrayToHashSet(newSelection));

        Vector<PathPlaceHolder> result = PathPlaceHolder.createPathsPlaceHolders(hashSetToTreePathArray(addedElements), true);
        result.addAll(PathPlaceHolder.createPathsPlaceHolders(hashSetToTreePathArray(removedElements), false));

        return result;
    }

    private TreePath getNewLeadPath(final int mode) {
        return (mode == ADD_SELECTED_PATHS || mode == SET_SELECTED_PATHS) ?
                getLastSelectionPath() : getSelectionPath();
    }

    private TreePath getLastSelectionPath() {
        return !isSelectionEmpty() ? selection[selection.length - 1] : null;
    }

    private TreePath[] getSinglePathArray(final TreePath path) {
        singlePathArray[0] = path;
        return (path != null) ? singlePathArray : null;
    }

    private boolean isSelectionModifiableAnyhow() {
        if (rowMapper == null) {
            return true;
        }
        if (isSelectionEmpty()) {
            return true;
        }
        if (selectionMode == DISCONTIGUOUS_TREE_SELECTION) {
            return true;
        }

        return false;
    }

    private TreePath[] hashSetToTreePathArray(final HashSet set) {
        return (TreePath[])set.toArray(new TreePath[set.size()]);
    }

    private HashSet arrayToHashSet(final Object[] array) {
        if (array == null) {
            return new HashSet();
        }

        final HashSet result = new HashSet(array.length);
        result.addAll(Arrays.asList(array));
        return result;
    }

    private boolean areRowsContigous(final int[] rows) {
        Arrays.sort(rows);
        return areSortedRowsContigous(rows, rows.length);
    }

    private boolean areSortedRowsContigous(final int[] sortedRows, final int checkedRange) {
        for (int i = 0; i < checkedRange - 1; i++) {
            if (sortedRows[i + 1] - sortedRows[i] > 1) {
                return false;
            }
        }
        return true;
    }

    private TreePath[] getContigousPathsArray(final TreePath[] paths) {
        TreePath[] result = new TreePath[getContiguousPathsLength(paths)];
        System.arraycopy(paths, 0, result, 0, result.length);
        return result;
    }

    private int getContiguousPathsLength(final TreePath[] paths) {
        final int[] rows = rowMapper.getRowsForPaths(paths);
        Arrays.sort(rows);
        for (int i = 1; i <= rows.length; i++) {
            if (!areSortedRowsContigous(rows, i)) {
                return i - 1;
            }
        }

        return paths.length;
    }

    private int getPathIndex(final TreePath path) {
        if (rowMapper != null) {
            return getRowForPath(path);
        }
        if (isSelectionEmpty()) {
            return -1;
        }
        for (int i = 0; i < selection.length; i++) {
            if (selection[i].equals(path)) {
                return i;
            }
        }

        return -1;
    }

    private TreePath[] getUniquePathArray(final TreePath[] paths) {
        TreePath[] filtered = hashSetToTreePathArray(arrayToHashSet(paths));
        return (filtered.length == paths.length) ? (TreePath[])paths.clone() : filtered;
    }

    private int getRowForPath(final TreePath path) {
        return rowMapper.getRowsForPaths(getSinglePathArray(path))[0];
    }

    private static int[] getCleanedSortedRowsArray(final int[] array) {
        Arrays.sort(array);
        if (array[0] != -1) {
            return array;
        }

        int numOdds = 1;
        while(numOdds < array.length && (array[numOdds] == -1)) {
            numOdds++;
        }
        int[] result = new int[array.length - numOdds];
        System.arraycopy(array, numOdds, result, 0, result.length);

        return result;
    }
}
