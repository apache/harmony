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

package org.apache.harmony.beans.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("serial")
public class ColorEditor extends Panel implements PropertyEditor {

    List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();

    private Color value;

    private Object source;

    public ColorEditor(Object source) {
        if (null == source) {
            throw new NullPointerException();
        }
        this.source = source;
    }

    public ColorEditor() {
        super();
    }

    public Component getCustomEditor() {
        return this;
    }

    public boolean supportsCustomEditor() {
        return true;
    }

    public String getJavaInitializationString() {
        String result = null;
        Color color = (Color) getValue();
        if (color != null) {
            int red = color.getRed();
            int green = color.getGreen();
            int blue = color.getBlue();
            result = "new java.awt.Color(" + red + "," + green + "," + blue + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        return result;
    }

    public String[] getTags() {
        return null;
    }

    public void setValue(Object value) {
        if (null == value) {
            return;
        }
        Object oldValue = this.value;
        this.value = (Color) value;
        PropertyChangeEvent changeAllEvent = new PropertyChangeEvent(this,
                "value", oldValue, value); //$NON-NLS-1$
        PropertyChangeListener[] copy = new PropertyChangeListener[listeners
                .size()];
        listeners.toArray(copy);
        for (PropertyChangeListener listener : copy) {
            listener.propertyChange(changeAllEvent);
        }
    }

    @SuppressWarnings("nls")
    public String getAsText() {
        Color c = (Color) getValue();
        if (null == c) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(14);
        sb.append(c.getRed());
        sb.append(",");
        sb.append(c.getGreen());
        sb.append(",");
        sb.append(c.getBlue());
        return sb.toString();
    }

    @SuppressWarnings("nls")
    public void setAsText(String text) {
        if (null == text) {
            throw new NullPointerException();
        }

        int r = 0;
        int g = 0;
        int b = 0;
        String aText = text;
        try {
            int index = text.indexOf(",");
            r = Integer.parseInt(text.substring(0, index));
            aText = text.substring(index + 1);
            index = aText.indexOf(",");
            g = Integer.parseInt(aText.substring(0, index));
            aText = aText.substring(index + 1);
            b = Integer.parseInt(aText);
            setValue(new Color(r, g, b));
        } catch (Exception e) {
            throw new IllegalArgumentException(aText);
        }
    }

    public boolean isPaintable() {
        return true;
    }

    public void paintValue(Graphics gfx, Rectangle box) {
        Color color = (Color) getValue();
        if (color != null) {
            gfx.setColor(color);
            gfx.drawRect(box.x, box.y, box.x + box.width, box.y + box.height);
        }
    }

    public Object getValue() {
        return value;
    }

    @Override
    public synchronized void removePropertyChangeListener(
            PropertyChangeListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public synchronized void addPropertyChangeListener(
            PropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void firePropertyChange() {
        if (listeners.isEmpty()) {
            return;
        }

        List<PropertyChangeListener> copy = new ArrayList<PropertyChangeListener>(
                listeners.size());
        synchronized (listeners) {
            copy.addAll(listeners);
        }

        PropertyChangeEvent changeAllEvent = new PropertyChangeEvent(source,
                null, null, null);
        for (Iterator<PropertyChangeListener> listenersItr = copy.iterator(); listenersItr
                .hasNext();) {
            PropertyChangeListener listna = listenersItr.next();
            listna.propertyChange(changeAllEvent);
        }
    }
}
