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
package javax.swing.text;

public class ElementIterator implements Cloneable {

    private final Element root;
    private Element cursor;
    private int depth;

    public ElementIterator(final Element root) {
        this.root = root;
    }

    public ElementIterator(final Document doc) {
        this(doc.getRootElements()[0]);
    }

    public Element first() {
        return initCursorWithRoot();
    }

    public Element current() {
        if (cursor == null) {
            return initCursorWithRoot();
        }
        if (cursor == root && root.getElementCount() == 0) {
            depth = 0;
            return null;
        }
        return cursor;
    }

    public Element next() {
        if (cursor == null) {
            return initCursorWithRoot();
        }
        if (cursor == root && root.getElementCount() == 0) {
            depth = 0;
            return null;
        }
        if (cursor.getElementCount() != 0) {
            depth++;
            return (cursor = cursor.getElement(0));
        }

        while (cursor != root && cursor != null) {
            final Element parent = cursor.getParentElement();
            final int currentIndex = getElementIndex(parent, cursor);
            if (currentIndex < parent.getElementCount() - 1) {
                return (cursor = parent.getElement(currentIndex + 1));
            }
            depth--;
            cursor = parent;
        }
        depth = 0;
        return (cursor = null);
    }

    public Element previous() {
        if (cursor == null || cursor == root) {
            return null;
        }

        final Element parent = cursor.getParentElement();
        if (parent == null) {
            return null;
        }
        final int currentIndex = getElementIndex(parent, cursor);
        if (currentIndex == 0) {
            return parent;
        }
        Element result = parent.getElement(currentIndex - 1);
        while (result.getElementCount() != 0) {
            result = result.getElement(result.getElementCount() - 1);
        }
        return result;
    }

    public int depth() {
        return depth;
    }

    public Object clone() {
        final ElementIterator cloned = new ElementIterator(root);
        cloned.cursor = cursor;
        cloned.depth = depth;
        return cloned;
    }

    private int getElementIndex(final Element parent, final Element child) {
        final int numChildren = parent.getElementCount();
        for (int i = 0; i < numChildren; i++) {
            if (parent.getElement(i) == child) {
                return i;
            }
        }

        return -1;
    }

    private Element initCursorWithRoot() {
        cursor = root;
        depth = 1;
        return cursor;
    }
}
