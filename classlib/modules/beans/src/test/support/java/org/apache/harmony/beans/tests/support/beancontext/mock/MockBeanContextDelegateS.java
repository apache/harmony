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

package org.apache.harmony.beans.tests.support.beancontext.mock;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextChild;
import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextServiceAvailableEvent;
import java.beans.beancontext.BeanContextServiceRevokedEvent;
import java.beans.beancontext.BeanContextSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

/**
 * Mock of BeanContext
 */
@SuppressWarnings("unchecked")
public class MockBeanContextDelegateS implements BeanContext, Serializable {

    private static final long serialVersionUID = 3636087892485726792L;

    public BeanContextSupport support = new BeanContextSupport(this, Locale.US);

    public String id;

    public MockBeanContextDelegateS(String id) {
        this.id = id;
    }

    public boolean add(Object arg0) {
        return support.add(arg0);
    }

    public boolean addAll(Collection arg0) {
        return support.addAll(arg0);
    }

    public void addBeanContextMembershipListener(BeanContextMembershipListener arg0) {
        support.addBeanContextMembershipListener(arg0);
    }

    public void addPropertyChangeListener(String arg0, PropertyChangeListener arg1) {
        support.addPropertyChangeListener(arg0, arg1);
    }

    public void addVetoableChangeListener(String arg0, VetoableChangeListener arg1) {
        support.addVetoableChangeListener(arg0, arg1);
    }

    public boolean avoidingGui() {
        return support.avoidingGui();
    }

    public void clear() {
        support.clear();
    }

    public boolean contains(Object arg0) {
        return support.contains(arg0);
    }

    public boolean containsAll(Collection arg0) {
        return support.containsAll(arg0);
    }

    public boolean containsKey(Object arg0) {
        return support.containsKey(arg0);
    }

    public void dontUseGui() {
        support.dontUseGui();
    }

    public void firePropertyChange(String arg0, Object arg1, Object arg2) {
        support.firePropertyChange(arg0, arg1, arg2);
    }

    public void fireVetoableChange(String arg0, Object arg1, Object arg2)
            throws PropertyVetoException {
        support.fireVetoableChange(arg0, arg1, arg2);
    }

    public BeanContext getBeanContext() {
        return support.getBeanContext();
    }

    public BeanContextChild getBeanContextChildPeer() {
        return support.getBeanContextChildPeer();
    }

    public BeanContext getBeanContextPeer() {
        return support.getBeanContextPeer();
    }

    public Locale getLocale() {
        return support.getLocale();
    }

    public URL getResource(String arg0, BeanContextChild arg1) throws IllegalArgumentException {
        return support.getResource(arg0, arg1);
    }

    public InputStream getResourceAsStream(String arg0, BeanContextChild arg1)
            throws IllegalArgumentException {
        return support.getResourceAsStream(arg0, arg1);
    }

    public Object instantiateChild(String arg0) throws IOException, ClassNotFoundException {
        return support.instantiateChild(arg0);
    }

    public boolean isDelegated() {
        return support.isDelegated();
    }

    public boolean isDesignTime() {
        return support.isDesignTime();
    }

    public boolean isEmpty() {
        return support.isEmpty();
    }

    public boolean isSerializing() {
        return support.isSerializing();
    }

    public Iterator iterator() {
        return support.iterator();
    }

    public boolean needsGui() {
        return support.needsGui();
    }

    public void okToUseGui() {
        support.okToUseGui();
    }

    public void propertyChange(PropertyChangeEvent arg0) {
        support.propertyChange(arg0);
    }

    public void readChildren(ObjectInputStream arg0) throws IOException, ClassNotFoundException {
        support.readChildren(arg0);
    }

    public boolean remove(Object arg0) {
        return support.remove(arg0);
    }

    public boolean removeAll(Collection arg0) {
        return support.removeAll(arg0);
    }

    public void removeBeanContextMembershipListener(BeanContextMembershipListener arg0) {
        support.removeBeanContextMembershipListener(arg0);
    }

    public void removePropertyChangeListener(String arg0, PropertyChangeListener arg1) {
        support.removePropertyChangeListener(arg0, arg1);
    }

    public void removeVetoableChangeListener(String arg0, VetoableChangeListener arg1) {
        support.removeVetoableChangeListener(arg0, arg1);
    }

    public boolean retainAll(Collection arg0) {
        return support.retainAll(arg0);
    }

    public void serviceAvailable(BeanContextServiceAvailableEvent arg0) {
        support.serviceAvailable(arg0);
    }

    public void serviceRevoked(BeanContextServiceRevokedEvent arg0) {
        support.serviceRevoked(arg0);
    }

    public void setBeanContext(BeanContext arg0) throws PropertyVetoException {
        support.setBeanContext(arg0);
    }

    public void setDesignTime(boolean arg0) {
        support.setDesignTime(arg0);
    }

    public void setLocale(Locale arg0) throws PropertyVetoException {
        support.setLocale(arg0);
    }

    public int size() {
        return support.size();
    }

    public Object[] toArray() {
        return support.toArray();
    }

    public Object[] toArray(Object[] arg0) {
        return support.toArray(arg0);
    }

    public boolean validatePendingSetBeanContext(BeanContext arg0) {
        return support.validatePendingSetBeanContext(arg0);
    }

    public void vetoableChange(PropertyChangeEvent arg0) throws PropertyVetoException {
        support.vetoableChange(arg0);
    }

    public void writeChildren(ObjectOutputStream arg0) throws IOException {
        support.writeChildren(arg0);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        support.writeChildren(oos);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        support.readChildren(ois);
    }

}
