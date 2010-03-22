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
package javax.swing.plaf;

import java.awt.Rectangle;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

public abstract class TreeUI extends ComponentUI {
    public abstract void cancelEditing(final JTree tree);
    public abstract TreePath getClosestPathForLocation(final JTree tree,
                                                       final int x,
                                                       final int y);
    public abstract TreePath getEditingPath(final JTree tree);
    public abstract Rectangle getPathBounds(final JTree tree,
                                            final TreePath path);
    public abstract TreePath getPathForRow(final JTree tree,
                                           final int row);
    public abstract int getRowCount(final JTree tree);
    public abstract int getRowForPath(final JTree tree, final TreePath path);
    public abstract boolean isEditing(final JTree tree);
    public abstract void startEditingAtPath(final JTree tree,
                                            final TreePath path);
    public abstract boolean stopEditing(final JTree path);
}

