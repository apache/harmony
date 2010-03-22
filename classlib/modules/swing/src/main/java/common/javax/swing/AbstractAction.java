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

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.HashMap;
import javax.swing.event.SwingPropertyChangeSupport;
import org.apache.harmony.x.swing.StringConstants;

public abstract class AbstractAction implements Action, Cloneable, Serializable {
    protected boolean enabled = true;

    protected SwingPropertyChangeSupport changeSupport = new SwingPropertyChangeSupport(this);

    private HashMap<String, Object> properties;

    public AbstractAction() {
        super();
    }

    public AbstractAction(final String name) {
        super();
        properties = new HashMap<String, Object>();
        properties.put(Action.NAME, name);
    }

    public AbstractAction(final String name, final Icon icon) {
        super();
        properties = new HashMap<String, Object>();
        properties.put(Action.NAME, name);
        properties.put(Action.SMALL_ICON, icon);
    }

    public void putValue(final String name, final Object value) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        Object oldValue = properties.get(name);
        if (value != oldValue) {
            properties.put(name, value);
            firePropertyChange(name, oldValue, value);
        }
    }

    public Object getValue(final String name) {
        return (properties != null) ? properties.get(name) : null;
    }

    public Object[] getKeys() {
        return (properties != null) ? properties.keySet().toArray() : new Object[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object clone() throws CloneNotSupportedException {
        AbstractAction cloned = (AbstractAction) super.clone();
        if (properties != null) {
            cloned.properties = (HashMap<String, Object>) properties.clone();
        }
        return cloned;
    }

    public synchronized void removePropertyChangeListener(final PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public synchronized void addPropertyChangeListener(final PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        return changeSupport.getPropertyChangeListeners();
    }

    protected void firePropertyChange(final String propertyName, final Object oldValue,
            final Object newValue) {
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void setEnabled(final boolean enabled) {
        boolean oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange(StringConstants.ENABLED_PROPERTY_CHANGED, Boolean.valueOf(oldValue),
                Boolean.valueOf(enabled));
    }

    public boolean isEnabled() {
        return enabled;
    }
}
