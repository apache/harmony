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

import java.util.Vector;

class PathPlaceHolder {
    protected boolean isNew;
    protected TreePath path;

    PathPlaceHolder() {
    }

    PathPlaceHolder(final TreePath path, final boolean isNew) {
        this.path = path;
        this.isNew = isNew;
    }

    static TreePath[] getPathsArray(final Vector<PathPlaceHolder> pathPlaceHolders) {
        if (pathPlaceHolders == null) {
            return new TreePath[0];
        }

        TreePath[] result = new TreePath[pathPlaceHolders.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = pathPlaceHolders.get(i).path;
        }
        return result;
    }

    static boolean[] getAreNewArray(final Vector<PathPlaceHolder> pathPlaceHolders) {
        if (pathPlaceHolders == null) {
            return new boolean[0];
        }

        boolean[] result = new boolean[pathPlaceHolders.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = pathPlaceHolders.get(i).isNew;
        }
        return result;
    }

    static Vector<PathPlaceHolder> createPathsPlaceHolders(final TreePath[] paths, final boolean areNew) {
        if (paths == null) {
            return new Vector();
        }
        Vector result = new Vector(paths.length);
        for (int i = 0; i < paths.length; i++) {
            result.add(new PathPlaceHolder(paths[i], areNew));
        }
        return result;
    }
}
