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
package javax.swing.event;

import java.util.EventObject;

import javax.swing.tree.TreePath;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class TreeSelectionEvent extends EventObject {
    protected TreePath[] paths;
    protected boolean[] areNew;
    protected TreePath oldLeadSelectionPath;
    protected TreePath newLeadSelectionPath;


    public TreeSelectionEvent(final Object source, final TreePath path,
                              final boolean isNew, final TreePath oldLeadSelectionPath,
                              final TreePath newLeadSelectionPath) {

        this(source, new TreePath[] {path}, new boolean[] {isNew},
             oldLeadSelectionPath, newLeadSelectionPath);
    }

    public TreeSelectionEvent(final Object source, final TreePath[] paths,
                              final boolean[] areNew, final TreePath oldLeadSelectionPath,
                              final TreePath newLeadSelectionPath) {

        super(source);
        this.paths = paths;
        this.areNew = areNew;
        this.oldLeadSelectionPath = oldLeadSelectionPath;
        this.newLeadSelectionPath = newLeadSelectionPath;
    }

    public TreePath[] getPaths() {
        return paths != null ? (TreePath[])paths.clone() : null;
    }

    public TreePath getPath() {
        return paths != null ? paths[0] : null;
    }

    public boolean isAddedPath() {
        return areNew != null ? areNew[0] : false;
    }

    public boolean isAddedPath(final TreePath path) {
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] == path) {
                return isAddedPath(i);
            }
        }
        throw new IllegalArgumentException(Messages.getString("swing.69")); //$NON-NLS-1$
    }

    public boolean isAddedPath(final int index) {
        if (index < 0 || index >= areNew.length) {
            throw new IllegalArgumentException(Messages.getString("swing.6A")); //$NON-NLS-1$
        }
        return areNew[index];
    }

    public TreePath getOldLeadSelectionPath() {
        return oldLeadSelectionPath;
    }

    public TreePath getNewLeadSelectionPath() {
        return newLeadSelectionPath;
    }

    public Object cloneWithSource(final Object newSource) {
        return new TreeSelectionEvent(newSource, paths, areNew,
                                      oldLeadSelectionPath, newLeadSelectionPath);
    }
}
