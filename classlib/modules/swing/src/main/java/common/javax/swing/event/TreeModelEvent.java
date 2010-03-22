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

public class TreeModelEvent extends EventObject {
    protected TreePath path;
    protected int[] childIndices;
    protected Object[] children;

    public TreeModelEvent(final Object source, final Object[] path) {
        this(source, path, new int[0], null);
    }

    public TreeModelEvent(final Object source, final TreePath path) {
        this(source, path, new int[0], null);
    }

    public TreeModelEvent(final Object source, final Object[] path,
                          final int[] childIndices, final Object[] children) {

        this(source, new TreePath(path), childIndices, children);
    }

    public TreeModelEvent(final Object source, final TreePath path,
                          final int[] childIndices, final Object[] children) {

        super(source);
        this.path = path;
        this.childIndices = childIndices;
        this.children = children;
    }

    public TreePath getTreePath() {
        return path;
    }

    public Object[] getPath() {
        return path != null ? path.getPath() : null;
    }

    public Object[] getChildren() {
        return children != null ? (Object[])children.clone() : null;
    }

    public int[] getChildIndices() {
        return childIndices != null ? (int[])childIndices.clone() : null;
    }
}
