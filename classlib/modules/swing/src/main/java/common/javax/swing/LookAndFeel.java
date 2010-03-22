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
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentInputMapUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.InputMapUIResource;
import javax.swing.text.JTextComponent;

import org.apache.harmony.x.swing.Utilities;

import org.apache.harmony.x.swing.internal.nls.Messages;


public abstract class LookAndFeel {
    private static Map classToSettersMap = new HashMap();

    private UIDefaults uiDefaults;

    public abstract boolean isSupportedLookAndFeel();
    public abstract boolean isNativeLookAndFeel();
    public abstract String getName();
    public abstract String getID();
    public abstract String getDescription();

    public UIDefaults getDefaults() {
        return uiDefaults;
    }

    public String toString() {
        return "[" + getDescription() + " - " + this.getClass().getName() + "]";
    }

    public void provideErrorFeedback(final Component c) {
        Toolkit.getDefaultToolkit().beep();
    }

    public boolean getSupportsWindowDecorations() {
        return false;
    }

    public void initialize() {
    }

    public void uninitialize() {
    }

    public static void installColorsAndFont(final JComponent comp, final String background,
                                            final String foreground, final String fontName) {
        installColors(comp, background, foreground);
        Font font = comp.getFont();
        if (Utilities.isUIResource(font)) {
            comp.setFont(UIManager.getFont(fontName));
        }
    }

    public static ComponentInputMap makeComponentInputMap(final JComponent comp, final Object[] keys) {
        ComponentInputMapUIResource result = new ComponentInputMapUIResource(comp);
        loadKeyBindings(result, keys);

        return result;
    }

    public static void installColors(final JComponent comp, final String background, final String foreground) {
        Color foregroundColor = comp.getForeground();
        if (Utilities.isUIResource(foregroundColor)) {
            comp.setForeground(UIManager.getColor(foreground));
        }
        Color backgroundColor = comp.getBackground();
        if (Utilities.isUIResource(backgroundColor)) {
            comp.setBackground(UIManager.getColor(background));
        }
    }

    public static Object makeIcon(final Class<?> c, final String path) {
        return new UIDefaults.LazyValue() {
            public Object createValue(final UIDefaults uiDefaults) {
                URL resource = c.getResource(path);
                return resource != null ? new IconUIResource(new ImageIcon(resource))
                                        : null;
            }
            public String toString() {
                return "Lazy Value: (lf) " + c.getName() + "  " + path;
            }
        };
    }

    public static Object getDesktopPropertyValue(final String propertyName, final Object defaultValue) {
        Object result = Toolkit.getDefaultToolkit().getDesktopProperty(propertyName);

        if (result instanceof Color) {
            return new ColorUIResource((Color)result);
        }
        if (result instanceof Font) {
            return new FontUIResource((Font)result);
        }

        return (result == null) ? defaultValue : result;
    }

    public static JTextComponent.KeyBinding[] makeKeyBindings(final Object[] bindings) {
        JTextComponent.KeyBinding[] result = new JTextComponent.KeyBinding[bindings.length / 2];
        for (int i = 0; i < bindings.length; i += 2) {
            result[i / 2] = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke((String)bindings[i]),
                                                      (String)bindings[i + 1]);
        }

        return result;
    }

    public static InputMap makeInputMap(final Object[] bindings) {
        InputMapUIResource result = new InputMapUIResource();
        loadKeyBindings(result, bindings);

        return result;
    }

    public static void installBorder(final JComponent comp, final String borderName) {
        if (Utilities.isUIResource(comp.getBorder())) {
            comp.setBorder((Border)UIManager.get(borderName));
        }
    }

    public static void uninstallBorder(final JComponent comp) {
        if (comp != null && Utilities.isUIResource(comp.getBorder())) {
            comp.setBorder(null);
        }
    }

    public static void loadKeyBindings(final InputMap resultMap, final Object[] array) {
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.length; i += 2) {
            if (array[i] instanceof String) {
                resultMap.put(KeyStroke.getKeyStroke((String)array[i]), array[i + 1]);
            } else {
                resultMap.put((KeyStroke)array[i], array[i + 1]);
            }
        }
    }

    public static void installProperty(final JComponent c, final String propertyName, final Object propertyValue) {
        if (c.installablePropertiesExcluded.contains(propertyName)) {
            return;
        }
        Method setter = getSetter(propertyName, c.getClass());
        if (setter == null) {
            throw new IllegalArgumentException(Messages.getString("swing.51") + propertyName); //$NON-NLS-1$
        }
        try {
            setter.invoke(c, new Object[] {propertyValue});
        } catch (IllegalArgumentException e) {
            throw new ClassCastException();
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(Messages.getString("swing.51") + propertyName); //$NON-NLS-1$
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(Messages.getString("swing.51") + propertyName); //$NON-NLS-1$
        }
        c.installablePropertiesExcluded.remove(propertyName);
    }

    private static Method getSetter(final String propName, final Class c) {
        Map setters = (Map)classToSettersMap.get(c);
        if (setters == null) {
            setters = new HashMap();
            classToSettersMap.put(c, setters);
        }
        Method setter = (Method)setters.get(propName);
        if (setter == null) {
            setter = getUncachedSetter(propName, c);
            setters.put(propName, setter);
        }
        return setter;
    }

    private static Method getUncachedSetter(final String propName, final Class c) {
        return (Method)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    return new PropertyDescriptor(propName, c).getWriteMethod();
                } catch (IntrospectionException e) {
                    return null;
                }
            }
        });
    }

    static void markPropertyInstallable(final JComponent c, final String propertyName) {
        c.installablePropertiesExcluded.remove(propertyName);
    }

    static void markPropertyNotInstallable(final JComponent c, final String propertyName) {
        c.installablePropertiesExcluded.add(propertyName);
    }
}


