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

import java.io.Serializable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.harmony.x.swing.internal.nls.Messages;

/**
 * <p>
 * <i>DefaultBoundedRangeModel</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class DefaultBoundedRangeModel implements BoundedRangeModel, Serializable {
    private static final long serialVersionUID = -3186121964369712936L;

    private static final int DEFAULT_MAX = 100;

    protected ChangeEvent changeEvent;

    protected EventListenerList listenerList;

    private int max;

    private int min;

    private int extent;

    private int value;

    private boolean valueIsAdjusting;

    public DefaultBoundedRangeModel() {
        this(0, 0, 0, DEFAULT_MAX);
    }

    public DefaultBoundedRangeModel(int value, int extent, int min, int max) {
        if (min > value || value > value + extent || value + extent > max) {
            throw new IllegalArgumentException(Messages.getString("swing.07")); //$NON-NLS-1$
        }
        this.min = min;
        this.max = max;
        this.extent = extent;
        this.value = value;
        changeEvent = new ChangeEvent(this);
        valueIsAdjusting = false;
        listenerList = new EventListenerList();
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    protected void fireStateChanged() {
        ChangeListener[] listeners = getChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].stateChanged(changeEvent);
        }
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    public int getExtent() {
        return extent;
    }

    public <T extends java.util.EventListener> T[] getListeners(Class<T> c) {
        return listenerList.getListeners(c);
    }

    public int getMaximum() {
        return max;
    }

    public int getMinimum() {
        return min;
    }

    public int getValue() {
        return value;
    }

    public boolean getValueIsAdjusting() {
        return valueIsAdjusting;
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    public void setExtent(int n) {
        int newExtent = Math.min(Math.max(n, 0), max - value);
        if (newExtent != extent) {
            extent = newExtent;
            fireStateChanged();
        }
    }

    public void setMaximum(int n) {
        if (max == n) {
            return;
        }
        max = n;
        min = Math.min(n, min);
        extent = Math.min(max - min, extent);
        value = Math.min(value, max - extent);
        fireStateChanged();
    }

    public void setMinimum(int n) {
        if (n == min) {
            return;
        }
        min = n;
        max = Math.max(n, max);
        extent = Math.min(max - min, extent);
        value = Math.max(value, min);
        fireStateChanged();
    }

    public void setRangeProperties(int newValue, int newExtent, int newMin, int newMax,
            boolean adjusting) {
        if (newValue == value && newExtent == extent && newMin == min && newMax == max
                && adjusting == valueIsAdjusting) {
            return;
        }
        value = newValue;
        min = Math.min(newMin, value);
        max = Math.max(newMax, value);
        extent = Math.min(newExtent, max - value);
        valueIsAdjusting = adjusting;
        fireStateChanged();
    }

    public void setValue(int n) {
        int newValue = Math.min(Math.max(n, min), max - extent);
        if (newValue != value) {
            value = newValue;
            fireStateChanged();
        }
    }

    public void setValueIsAdjusting(boolean b) {
        if (b != valueIsAdjusting) {
            valueIsAdjusting = b;
            fireStateChanged();
        }
    }

    /*
     * The format of the string is based on 1.5 release behavior
     * which can be revealed using the following code:
     *
     *     Object obj = new DefaultBoundedRangeModel();
     *     System.out.println(obj.toString());
     */
    @Override
    public String toString() {
        return getClass().getName() + "[" + "value=" + value + ", " + "extent=" + extent + ", "
                + "min=" + min + ", " + "max=" + max + ", " + "adj=" + valueIsAdjusting + "]";
    }
}
