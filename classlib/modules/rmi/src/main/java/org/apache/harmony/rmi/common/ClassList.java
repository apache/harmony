/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Vasily Zakharov
 */
package org.apache.harmony.rmi.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;


/**
 * Stores list of classes ({@link Class} instances), making sure
 * no duplicates are stored, and taking inheritance into account.
 *
 * The classes are stores in order they were added, so that no class
 * in the list inherits any other class in the list. When each new class
 * is added to the list, all its superclasses (or subclasses, if
 * <em>inverse</em> flag was set in constructor) are removed from the list.
 *
 * @author  Vasily Zakharov
 */
public final class ClassList {

    /**
     * Storage vector.
     */
    private Vector vector;

    /**
     * Inverse flag.
     */
    private boolean inverse;

    /**
     * Creates empty list, equivalent to
     * {@link #ClassList(boolean) ClassList(false)}.
     */
    public ClassList() {
        this(false);
    }

    /**
     * Creates empty list.
     *
     * @param   inverse
     *          Inverse flag, see {@linkplain ClassList class description}.
     */
    public ClassList(boolean inverse) {
        this.inverse = inverse;
        vector = new Vector();
    }

    /**
     * Creates list and all the elements from the specified array, equivalent to
     * {@link #ClassList(Class[], boolean) ClassList(Class[], false)}.
     *
     * @param   classes
     *          Initial classes to put to the list.
     */
    public ClassList(Class[] classes) {
        this(false);
        addAll(classes);
    }

    /**
     * Creates list and all the elements from the specified array.
     *
     * @param   classes
     *          Initial classes to put to the list.
     *
     * @param   inverse
     *          Inverse flag, see {@linkplain ClassList class description}.
     */
    public ClassList(Class[] classes, boolean inverse) {
        this(inverse);
        addAll(classes);
    }

    /**
     * Creates list and all the elements from the specified collection,
     * equivalent to
     * {@link #ClassList(Collection, boolean) ClassList(Collection, false)}.
     *
     * @param   classes
     *          Initial classes to put to the list.
     */
    public ClassList(Collection classes) {
        this(false);
        addAll(classes);
    }

    /**
     * Creates list and all the elements from the specified collection.
     *
     * @param   classes
     *          Initial classes to put to the list.
     *
     * @param   inverse
     *          Inverse flag, see {@linkplain ClassList class description}.
     */
    public ClassList(Collection classes, boolean inverse) {
        this(inverse);
        addAll(classes);
    }

    /**
     * Creates list and all the elements from the specified list, equivalent to
     * {@link #ClassList(ClassList, boolean) ClassList(ClassList, false)}.
     *
     * @param   classes
     *          Initial classes to put to the list.
     */
    public ClassList(ClassList classes) {
        this(false);
        addAll(classes);
    }

    /**
     * Creates list and all the elements from the specified list.
     *
     * @param   classes
     *          Initial classes to put to the list.
     *
     * @param   inverse
     *          Inverse flag, see {@linkplain ClassList class description}.
     */
    public ClassList(ClassList classes, boolean inverse) {
        this(inverse);
        addAll(classes);
    }

    /**
     * Return <code>true</code>
     * if <code>cls2</code> is assignable from <code>cls1</code>
     * and {@linkplain ClassList inverse flag} is <code>true</code>
     * or if <code>cls1</code> is assignable from <code>cls2</code>
     * and {@linkplain ClassList inverse flag} is <code>false</code>,
     * <code>false</code> otherwise.
     *
     * @param   cls1
     *          Class to check.
     *
     * @param   cls2
     *          Class to check.
     *
     * @return  <code>true</code> if <code>cls2</code> is assignable from
     *          <code>cls1</code> and {@linkplain ClassList inverse flag}
     *          is <code>true</code> or if <code>cls1</code> is assignable
     *          from <code>cls2</code> and {@linkplain ClassList inverse flag}
     *          is <code>false</code>, <code>false</code> otherwise.
     */
    private boolean checkAssign(Class cls1, Class cls2) {
        return (inverse
                ? cls2.isAssignableFrom(cls1)
                : cls1.isAssignableFrom(cls2));
    }

    /**
     * Returns <code>true</code> if the list contains the specified
     * class or any of its subclasses/superclasses (depending on
     * {@linkplain ClassList inverse flag}), <code>false</code> otherwise.
     *
     * @param   cls
     *          Class to check.
     *
     * @return  <code>true</code> if the list contains the specified
     *          class or any of its subclasses/superclasses (depending
     *          on {@linkplain ClassList inverse flag}), <code>false</code>
     *          otherwise.
     */
    public boolean contains(Class cls) {
        int size = vector.size();

        for (int i = 0; i < size; i++) {
            if (checkAssign(cls, (Class) vector.elementAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the specified class to the end of the list. All its
     * superclasses/subclasses
     * (depending on {@linkplain ClassList inverse flag}),
     * if present, are removed. If class itself or any of its
     * subclasses/superclasses is already present, nothing is done.
     *
     * @param   cls
     *          Class to add.
     */
    public void add(Class cls) {
        boolean exist = false;

        // Do not pre-calculate size, it will change.
        for (int i = 0; i < vector.size(); i++) {
            Class element = (Class) vector.elementAt(i);

            // If subclass/superclass is present, do nothing, return.
            if (checkAssign(cls, element)) {
                return;
            }

            // If superclass/subclass is present, make sure class is present,
            // remove any other other superclasses/subclasses.
            if (checkAssign(element, cls)) {
                if (!exist) {
                    vector.setElementAt(cls, i);
                    exist = true;
                } else {
                    vector.removeElementAt(i);
                }
            }
        }

        if (!exist) {
            vector.addElement(cls);
        }
    }

    /**
     * Adds the all classes in the specified array to the list.
     * See {@link #add(Class)} for details.
     *
     * @param   classes
     *          Classes to add.
     */
    public void addAll(Class[] classes) {
        for (int i = 0; i < classes.length; i++) {
            add(classes[i]);
        }
    }

    /**
     * Adds the all classes in the specified collection to the list.
     * See {@link #add(Class)} for details.
     *
     * @param   classes
     *          Classes to add.
     */
    public void addAll(Collection classes) {
        for (Iterator i = classes.iterator(); i.hasNext(); ) {
            add((Class) i.next());
        }
    }

    /**
     * Adds the all classes in the specified list to this list.
     * See {@link #add(Class)} for details.
     *
     * @param   classes
     *          Classes to add.
     */
    public void addAll(ClassList classes) {
        addAll(classes.vector);
    }

    /**
     * Removes the specified class from the list.
     * If class itself is not present, but its superclasses/subclasses
     * (depending on {@linkplain ClassList inverse flag}) are, they are removed.
     *
     * @param   cls
     *          Class to remove.
     */
    public void remove(Class cls) {
        boolean changed = false;

        // Do not pre-calculate size, it will change.
        for (int i = 0; i < vector.size(); i++) {
            Class element = (Class) vector.elementAt(i);

            // If superclass/subclass is found, remove it.
            if (checkAssign(element, cls)) {
                vector.removeElementAt(i);

                // If class itself is found, return.
                if (cls == element) {
                    return;
                }
            }
        }
    }

    /**
     * Returns an iterator over the elements
     * in this list in proper sequence.
     *
     * @return  Iterator over the elements in this list
     *          in proper sequence.
     */
    public Iterator iterator() {
        return vector.iterator();
    }

    /**
     * Returns string representation of this list.
     *
     * @return  String representation of this list.
     */
    public String toString() {
        return vector.toString();
    }
}
