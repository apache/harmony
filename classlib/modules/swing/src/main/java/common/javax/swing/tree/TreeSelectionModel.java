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
package javax.swing.tree;

import java.beans.PropertyChangeListener;

import javax.swing.event.TreeSelectionListener;

public interface TreeSelectionModel {
    static final int SINGLE_TREE_SELECTION = 1;
    static final int CONTIGUOUS_TREE_SELECTION = 2;
    static final int DISCONTIGUOUS_TREE_SELECTION = 4;

    void setSelectionMode(int mode);
    int getSelectionMode();
    void setSelectionPath(TreePath path);
    void setSelectionPaths(TreePath[] paths);
    void addSelectionPath(TreePath path);
    void addSelectionPaths(TreePath[] paths);
    void removeSelectionPath(TreePath path);
    void removeSelectionPaths(TreePath[] paths);
    TreePath getSelectionPath();
    TreePath[] getSelectionPaths();
    int getSelectionCount();
    boolean isPathSelected(TreePath path);
    boolean isSelectionEmpty();
    void clearSelection();
    void setRowMapper(RowMapper mapper);
    RowMapper getRowMapper();
    int[] getSelectionRows();
    int getMinSelectionRow();
    int getMaxSelectionRow();
    boolean isRowSelected(int row);
    void resetRowSelection();
    int getLeadSelectionRow();
    TreePath getLeadSelectionPath();
    void addPropertyChangeListener(PropertyChangeListener l);
    void removePropertyChangeListener(PropertyChangeListener l);
    void addTreeSelectionListener(TreeSelectionListener l);
    void removeTreeSelectionListener(TreeSelectionListener l);
}
