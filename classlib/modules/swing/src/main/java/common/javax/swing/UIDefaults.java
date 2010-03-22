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
 * @author Sergey Burlak
 */

package javax.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class UIDefaults extends Hashtable<Object, Object> {

    public static interface ActiveValue {
        Object createValue(UIDefaults uiDefaults);
    }

    public static interface LazyValue {
        Object createValue(UIDefaults uiDefaults);
    }

    public static class LazyInputMap implements LazyValue {
        private Object[] objects;

        public LazyInputMap(final Object[] objects) {
            if (objects != null) {
                this.objects = new Object[objects.length];
                System.arraycopy(objects, 0, this.objects, 0, objects.length);
            }
        }

        public Object createValue(final UIDefaults uiDefaults) {
            return LookAndFeel.makeInputMap(objects);
        }

    }

    public static class ProxyLazyValue implements LazyValue {
        private String className;
        private String methodName;
        private Object[] params;

        private Object value;

        public ProxyLazyValue(final String className) {
            this(className, null, null);
        }

        public ProxyLazyValue(final String className, final Object[] params) {
            this(className, null, params);
        }

        public ProxyLazyValue(final String className, final String methodName) {
            this(className, methodName, null);
        }

        public ProxyLazyValue(final String className, final String methodName, final Object[] params) {
            this.className = className;
            this.methodName = methodName;
            if (params != null) {
                this.params = new Object[params.length];
                System.arraycopy(params, 0, this.params, 0, params.length);
            }
        }

        public Object createValue(final UIDefaults uiDefaults) {
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    value = null;
                    try {
                        Class classObj = Class.forName(className, true,
                                Thread.currentThread().getContextClassLoader());
                        if (params == null) {
                            value = (methodName == null)
                                    ? classObj.newInstance()
                                    : classObj.getMethod(methodName, (Class[])null).invoke(null, (Object[])null);
                        } else {
                            Class[] mParams = new Class[params.length];
                            for (int i = 0; i < mParams.length; i++) {
                                mParams[i] = extractClass(params[i]);
                            }

                            value = (methodName == null)
                                    ? classObj.getConstructor(mParams).newInstance(params)
                                    : classObj.getMethod(methodName, mParams).invoke(null, params);
                        }
                    } catch (Exception ignored) {
                    }

                    return value;
                }
            });

            return value;
        }

        /**
         * Extract class from the object
         * @param Object obj
         * @return Class result
         */
        private Class extractClass(final Object obj) {
            if (obj instanceof Integer) {
                return Integer.TYPE;
            }
            if (obj instanceof Boolean) {
                return Boolean.TYPE;
            }
            if (obj instanceof Float) {
                return Float.TYPE;
            }
            if (obj instanceof Character) {
                return Character.TYPE;
            }
            if (obj instanceof Short) {
                return Short.TYPE;
            }
            if (obj instanceof Long) {
                return Long.TYPE;
            }
            if (obj instanceof Double) {
                return Double.TYPE;
            }
            if (obj instanceof Byte) {
                return Byte.TYPE;
            }
            if (obj instanceof Void) {
                return Void.TYPE;
            }
            if (obj instanceof UIResource) {
                return obj.getClass().getSuperclass();
            }

            return obj.getClass();
        }
    }

    private static final String CREATE_UI_METHOD_NAME = "createUI";
    private Locale defaultLocale = Locale.getDefault();
    private SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);
    private List resourceBundles = new ArrayList();

    public UIDefaults() {
    }

    public UIDefaults(final Object[] array) {
        putDefaults(array);
    }

    public void removePropertyChangeListener(final PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public void addPropertyChangeListener(final PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return propertyChangeSupport.getPropertyChangeListeners();
    }

    public void removeResourceBundle(final String name) {
        resourceBundles.remove(ResourceBundle.getBundle(name, getDefaultLocale()));
    }

    public void addResourceBundle(final String name) {
        ResourceBundle bundle = ResourceBundle.getBundle(name, getDefaultLocale());
        if (bundle != null) {
            resourceBundles.add(bundle);
        }
    }

    public Object get(final Object key) {
        return get(key, getDefaultLocale());
    }

    public Object get(final Object key, final Locale locale) {
        Object result = super.get(key);
        if (result == null) {
            result = getFromResourceBundles(key, locale);
            return result;
        }

        if (result instanceof LazyValue) {
            Object value = ((LazyValue)result).createValue(this);
            if (key != null && value != null) {
                super.put(key, value);
            }
            result = value;
        }
        if (result instanceof ActiveValue) {
            result = ((ActiveValue)result).createValue(this);
        }

        return result;
    }

    public boolean getBoolean(final Object obj) {
        return getBoolean(obj, getDefaultLocale());
    }

    public boolean getBoolean(final Object obj, final Locale locale) {
        Object result = get(obj, locale);
        return (result instanceof Boolean) ? ((Boolean)result).booleanValue() : false;
    }

    public Border getBorder(final Object obj) {
        return getBorder(obj, getDefaultLocale());
    }

    public Border getBorder(final Object obj, final Locale locale) {
        Object result = get(obj, locale);
        return (result instanceof Border) ? (Border)result : null;
    }

    public Color getColor(final Object obj) {
        return getColor(obj, getDefaultLocale());
    }

    public Color getColor(final Object obj, final Locale locale) {
        Object result = get(obj, locale);
        return (result instanceof Color) ? (Color)result : null;
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    public Dimension getDimension(final Object obj) {
        return getDimension(obj, getDefaultLocale());
    }

    public Dimension getDimension(final Object obj, final Locale locale) {
        Object result = get(obj, locale);
        return (result instanceof Dimension) ? (Dimension)result : null;
    }

    public Font getFont(final Object obj) {
        return getFont(obj, getDefaultLocale());
    }

    public Font getFont(final Object obj, final Locale locale) {
        Object result = get(obj, locale);
        return (result instanceof Font) ? (Font)result : null;
    }

    public Icon getIcon(final Object obj) {
        return getIcon(obj, getDefaultLocale());
    }

    public Icon getIcon(final Object obj, final Locale locale) {
        Object result = get(obj, locale);
        return (result instanceof Icon) ? (Icon)result : null;
    }

    public Insets getInsets(final Object obj) {
        return getInsets(obj, getDefaultLocale());
    }

    public Insets getInsets(final Object obj, final Locale locale) {
        Object result = get(obj, locale);
        return (result instanceof Insets) ? (Insets)result : null;
    }

    public int getInt(final Object obj) {
        return getInt(obj, getDefaultLocale());
    }

    public int getInt(final Object obj, final Locale locale) {
        Object result = get(obj, locale);
        return (result instanceof Integer) ? ((Integer)result).intValue() : 0;
    }

    public String getString(final Object obj) {
        return getString(obj, getDefaultLocale());
    }

    public String getString(final Object obj, final Locale locale) {
        Object result = get(obj, locale);
        return (result instanceof String) ? (String)result : null;
    }

    public ComponentUI getUI(final JComponent comp) {
        try {
            String classID = comp.getUIClassID();
            String fullClassName = (String)get(classID);
            if (fullClassName == null) {
                getUIError(Messages.getString("swing.err.0D", classID)); //$NON-NLS-1$
                return null;
            }
            Class uiClass = (Class)get(fullClassName);
            Method method = null;
            if (uiClass == null) {
                uiClass = getUIClass(classID, comp.getClass().getClassLoader());
                method = getCreateUIMethodPriveledged(uiClass);

                put(fullClassName, uiClass);
                put(uiClass, method);
            } else {
                method = (Method)get(uiClass);
            }
            return (ComponentUI)method.invoke(null, new Object[] { comp });
        } catch (final Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            getUIError(writer.toString());
            return null;
        }
    }

    private Method getCreateUIMethodPriveledged(final Class uiClass) {
        return (Method)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    return uiClass.getMethod(CREATE_UI_METHOD_NAME, new Class[] { JComponent.class });
                } catch (Exception e) {
                    return null;
                }
            }
        });
    }

    public Class<? extends javax.swing.plaf.ComponentUI> getUIClass(final String name) {
        return getUIClass(name, null);
    }

    public Class<? extends javax.swing.plaf.ComponentUI> getUIClass(final String name, final ClassLoader classLoader) {
        try {
            if (classLoader == null) {
                return (Class<? extends javax.swing.plaf.ComponentUI>)
                        Class.forName((String)get(name), true, Thread.currentThread().getContextClassLoader());
            } else {
                return (Class<? extends javax.swing.plaf.ComponentUI>)
                        classLoader.loadClass((String)get(name));
            }
        } catch (final ClassNotFoundException e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            getUIError(writer.toString());
        }

        return null;
    }

    public Object put(final Object key, final Object value) {
        Object previousValue = super.get(key);
        if (value == null) {
            super.remove(key);
            firePropertyChange(key.toString(), previousValue, value);
        } else if (!value.equals(previousValue)) {
            super.put(key, value);
            firePropertyChange(key.toString(), previousValue, value);
        }

        return previousValue;
    }

    public void putDefaults(final Object[] array) {
        for (int i = 0; i < array.length; i += 2) {
            if (array[i + 1] != null) {
                super.put(array[i], array[i + 1]);
            }
        }

        firePropertyChange("UIDefaults", null, null);
    }

    public void setDefaultLocale(final Locale locale) {
        this.defaultLocale = locale;
    }

    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void getUIError(final String message) {
        System.err.println(Messages.getString("swing.err.06",message)); //$NON-NLS-1$
    }

    private Object getFromResourceBundles(final Object key, final Locale locale) {
        if (key == null || !(key instanceof String)) {
            return null;
        }

        String keyAsString = (String)key;
        if (getDefaultLocale().equals(locale)) {
            for (int i = resourceBundles.size() - 1; i >= 0; i--) {
                ResourceBundle bundle = (ResourceBundle)resourceBundles.get(i);
                try {
                    return bundle.getObject(keyAsString);
                } catch (final MissingResourceException mre) {
                }
            }
        } else {
            for (int i = resourceBundles.size() - 1; i >= 0; i--) {
                ResourceBundle bundle = (ResourceBundle)resourceBundles.get(i);
                try {
                    return ResourceBundle.getBundle(bundle.getClass().getName(), locale).getObject(keyAsString);
                } catch (final MissingResourceException mre) {
                }
            }
        }

        return null;
    }
}
