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

import java.io.Serializable;

import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public class TreePath implements Serializable {

    private Object[] elements;
    private TreePath parent;
    private final int pathCount;

    public TreePath(final Object[] path) {
        if (Utilities.isEmptyArray(path)) {
            throw new IllegalArgumentException(Messages.getString("swing.82")); //$NON-NLS-1$
        }

        pathCount = path.length;
        elements = new Object[pathCount];
        System.arraycopy(path, 0, elements, 0, pathCount);
        parent = null;
    }

    public TreePath(final Object singlePath) {
        if (singlePath == null) {
            throw new IllegalArgumentException(Messages.getString("swing.76")); //$NON-NLS-1$
        }
        elements = new Object[] {singlePath};
        pathCount = 1;
        parent = null;
    }

    protected TreePath() {
        elements = new Object[] {null};
        pathCount = 1;
        parent = null;
    }

    protected TreePath(final Object[] path, final int length) {
        pathCount = length;
        elements = new Object[pathCount];
        System.arraycopy(path, 0, elements, 0, pathCount);
        parent = null;
    }

    protected TreePath(final TreePath parentPath, final Object lastElement) {
        if (lastElement == null) {
            throw new IllegalArgumentException(Messages.getString("swing.76")); //$NON-NLS-1$
        }

        elements = new Object[] {lastElement};
        parent = parentPath;
        pathCount = (parent != null) ? parent.getPathCount() + 1 : 1;
    }

    public boolean equals(final Object o) {
        if (!(o instanceof TreePath)) {
            return false;
        }

        TreePath path = (TreePath)o;
        final int numPathComponents = getPathCount();
        if (path.getPathCount() != numPathComponents) {
            return false;
        }

        for (int i = 0; i < numPathComponents; i++) {
            if (!path.getPathComponent(i).equals(getPathComponent(i))) {
                return false;
            }
        }

        return true;
    }

    public Object getLastPathComponent() {
        return elements[elements.length - 1];
    }

    public TreePath getParentPath() {
        if (parent != null) {
            return parent;
        }

        int numParentPaths = getPathCount() - 1;
        if (numParentPaths <= 0) {
            return null;
        }

        return new TreePath(getPath(), numParentPaths);
    }

    public Object[] getPath() {
        if (parent == null) {
            return elements;
        }

        Object[] parentPath = parent.getPath();
        Object[] result = new Object[parentPath.length + 1];
        System.arraycopy(parentPath, 0, result, 0, parentPath.length);
        result[result.length - 1] = getLastPathComponent();

        elements = (Object[])result.clone();
        parent = null;
        return result;
    }

    public Object getPathComponent(final int element) {
        final int pathCount = getPathCount();
        if (element < 0 || element >= pathCount) {
            throw new IllegalArgumentException(Messages.getString("swing.75", element)); //$NON-NLS-1$
        }
        if (parent == null) {
            return elements[element];
        }

        return (element < pathCount - 1) ? parent.getPathComponent(element) :
                                           getLastPathComponent();
    }

    public int getPathCount() {
        return pathCount;
    }

    public boolean isDescendant(final TreePath child) {
        if (child == null) {
            return false;
        }

        final int numPathComponents = getPathCount();
        if (child.getPathCount() < numPathComponents) {
            return false;
        }

        for (int i = 0; i < numPathComponents; i++) {
            if (!child.getPathComponent(i).equals(getPathComponent(i))) {
                return false;
            }
        }

        return true;
    }

    public TreePath pathByAddingChild(final Object child) {
        if (child == null) {
            throw new NullPointerException(Messages.getString("swing.72")); //$NON-NLS-1$
        }

        return new TreePath(this, child);
    }

    public int hashCode() {
        return getLastPathComponent().hashCode();
    }

    public String toString() {
        String result = null;
        final int numPathComponents = getPathCount();
        for (int i = 0; i < numPathComponents; i++) {
            if (result != null) {
                result += ", ";
            } else {
                result = "";
            }
            result += getPathComponent(i);
        }
        return "[" + result + "]";
    }

}
