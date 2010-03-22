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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.beans.tests.support.mock;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Mock of ExceptionListener
 */
@SuppressWarnings("unchecked")
public class MockExceptionListener implements ExceptionListener {

    public ArrayList<Object> exHistory = new ArrayList<Object>();

    public void exceptionThrown(Exception ex) {
        exHistory.add(ex);
    }

    /**
     * @param arg0
     * @param arg1
     */
    public void add(int arg0, Object arg1) {
        exHistory.add(arg0, arg1);
    }

    /**
     * @param arg0
     * @return
     */
    public boolean add(Object arg0) {
        return exHistory.add(arg0);
    }

    /**
     * @param arg0
     * @param arg1
     * @return
     */
    public boolean addAll(int arg0, Collection arg1) {
        return exHistory.addAll(arg0, arg1);
    }

    /**
     * @param arg0
     * @return
     */
    public boolean addAll(Collection arg0) {
        return exHistory.addAll(arg0);
    }

    /**
     * 
     */
    public void clear() {
        exHistory.clear();
    }

    /**
     * @param arg0
     * @return
     */
    public boolean contains(Object arg0) {
        return exHistory.contains(arg0);
    }

    /**
     * @param arg0
     * @return
     */
    public boolean containsAll(Collection arg0) {
        return exHistory.containsAll(arg0);
    }

    /**
     * @param arg0
     */
    public void ensureCapacity(int arg0) {
        exHistory.ensureCapacity(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object arg0) {
        return exHistory.equals(arg0);
    }

    /**
     * @param arg0
     * @return
     */
    public Object get(int arg0) {
        return exHistory.get(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return exHistory.hashCode();
    }

    /**
     * @param arg0
     * @return
     */
    public int indexOf(Object arg0) {
        return exHistory.indexOf(arg0);
    }

    /**
     * @return
     */
    public boolean isEmpty() {
        return exHistory.isEmpty();
    }

    /**
     * @return
     */
    public Iterator<Object> iterator() {
        return exHistory.iterator();
    }

    /**
     * @param arg0
     * @return
     */
    public int lastIndexOf(Object arg0) {
        return exHistory.lastIndexOf(arg0);
    }

    /**
     * @return
     */
    public ListIterator<Object> listIterator() {
        return exHistory.listIterator();
    }

    /**
     * @param arg0
     * @return
     */
    public ListIterator<Object> listIterator(int arg0) {
        return exHistory.listIterator(arg0);
    }

    /**
     * @param arg0
     * @return
     */
    public Object remove(int arg0) {
        return exHistory.remove(arg0);
    }

    /**
     * @param arg0
     * @return
     */
    public boolean remove(Object arg0) {
        return exHistory.remove(arg0);
    }

    /**
     * @param arg0
     * @return
     */
    public boolean removeAll(Collection arg0) {
        return exHistory.removeAll(arg0);
    }

    /**
     * @param arg0
     * @return
     */
    public boolean retainAll(Collection arg0) {
        return exHistory.retainAll(arg0);
    }

    /**
     * @param arg0
     * @param arg1
     * @return
     */
    public Object set(int arg0, Object arg1) {
        return exHistory.set(arg0, arg1);
    }

    /**
     * @return
     */
    public int size() {
        return exHistory.size();
    }

    /**
     * @param arg0
     * @param arg1
     * @return
     */
    public List<Object> subList(int arg0, int arg1) {
        return exHistory.subList(arg0, arg1);
    }

    /**
     * @return
     */
    public Object[] toArray() {
        return exHistory.toArray();
    }

    /**
     * @param arg0
     * @return
     */
    public Object[] toArray(Object[] arg0) {
        return exHistory.toArray(arg0);
    }
}
