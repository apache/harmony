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

package javax.swing;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>DefaultListModel</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class DefaultListModel extends AbstractListModel {
    private static final long serialVersionUID = 7987079759213724557L;

    private Vector<Object> internalStorage = new Vector<Object>();

    public void add(int index, Object element) {
        internalStorage.add(index, element);
        fireIntervalAdded(this, index, index);
    }

    public void addElement(Object element) {
        internalStorage.addElement(element);
        fireIntervalAdded(this, internalStorage.size() - 1, internalStorage.size() - 1);
    }

    public int capacity() {
        return internalStorage.capacity();
    }

    public void clear() {
        int size = getSize();
        if (size > 0) {
            internalStorage.clear();
            fireIntervalRemoved(this, 0, size - 1);
        }
    }

    public boolean contains(Object element) {
        return internalStorage.contains(element);
    }

    public void copyInto(Object[] array) {
        internalStorage.copyInto(array);
    }

    public Object elementAt(int index) {
        return internalStorage.elementAt(index);
    }

    public Enumeration<?> elements() {
        return internalStorage.elements();
    }

    public void ensureCapacity(int minCapacity) {
        internalStorage.ensureCapacity(minCapacity);
    }

    public Object firstElement() {
        return internalStorage.firstElement();
    }

    public Object get(int index) {
        return internalStorage.get(index);
    }

    public Object getElementAt(int index) {
        return get(index);
    }

    public int getSize() {
        return internalStorage.size();
    }

    public int indexOf(Object element) {
        return internalStorage.indexOf(element);
    }

    public int indexOf(Object element, int index) {
        return internalStorage.indexOf(element, index);
    }

    public void insertElementAt(Object element, int index) {
        internalStorage.insertElementAt(element, index);
        fireIntervalAdded(this, index, index);
    }

    public boolean isEmpty() {
        return internalStorage.isEmpty();
    }

    public Object lastElement() {
        return internalStorage.lastElement();
    }

    public int lastIndexOf(Object element) {
        return internalStorage.lastIndexOf(element);
    }

    public int lastIndexOf(Object element, int index) {
        return internalStorage.lastIndexOf(element, index);
    }

    public Object remove(int index) {
        Object result = internalStorage.remove(index);
        fireIntervalRemoved(this, index, index);
        return result;
    }

    public void removeAllElements() {
        clear();
    }

    public boolean removeElement(Object element) {
        int index = internalStorage.indexOf(element);
        boolean result = internalStorage.removeElement(element);
        if (index != -1) {
            fireIntervalRemoved(this, index, index);
        }
        return result;
    }

    public void removeElementAt(int index) {
        remove(index);
    }

    public void removeRange(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(Messages.getString("swing.01")); //$NON-NLS-1$
        }
        for (int i = 0; i < toIndex - fromIndex + 1; i++) {
            internalStorage.remove(fromIndex);
        }
        fireIntervalRemoved(this, fromIndex, toIndex);
    }

    public Object set(int index, Object element) {
        Object result = internalStorage.set(index, element);
        fireContentsChanged(this, index, index);
        return result;
    }

    public void setElementAt(Object element, int index) {
        set(index, element);
    }

    public void setSize(int size) {
        internalStorage.setSize(size);
    }

    public int size() {
        return internalStorage.size();
    }

    public Object[] toArray() {
        return internalStorage.toArray();
    }

    @Override
    public String toString() {
        return internalStorage.toString();
    }

    public void trimToSize() {
        internalStorage.trimToSize();
    }
}
