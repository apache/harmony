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

package java.awt;

import java.awt.dnd.DropTarget;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.InvocationEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.awt.peer.ComponentPeer;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

import org.apache.harmony.awt.ClipRegion;
import org.apache.harmony.awt.FieldsAccessor;
import org.apache.harmony.awt.gl.CommonGraphics2D;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.state.State;
import org.apache.harmony.awt.text.TextFieldKit;
import org.apache.harmony.awt.text.TextKit;
import org.apache.harmony.awt.wtk.NativeWindow;
import org.apache.harmony.luni.util.NotImplementedException;

public abstract class Component implements ImageObserver, MenuContainer, Serializable {
    private static final long serialVersionUID = -7644114512714619750L;

    public static final float TOP_ALIGNMENT = 0.0f;

    public static final float CENTER_ALIGNMENT = 0.5f;

    public static final float BOTTOM_ALIGNMENT = 1.0f;

    public static final float LEFT_ALIGNMENT = 0.0f;

    public static final float RIGHT_ALIGNMENT = 1.0f;

    private static final Hashtable<Class<?>, Boolean> childClassesFlags = new Hashtable<Class<?>, Boolean>();

    private static final ComponentPeer peer = new ComponentPeer() {
    };

    private static final boolean incrementalImageUpdate;

    final transient Toolkit toolkit = Toolkit.getDefaultToolkit();

    protected abstract class AccessibleAWTComponent extends AccessibleContext implements
            Serializable, AccessibleComponent {
        private static final long serialVersionUID = 642321655757800191L;

        protected class AccessibleAWTComponentHandler implements ComponentListener {
            protected AccessibleAWTComponentHandler() {
            }

            public void componentHidden(ComponentEvent e) {
                if (behaviour.isLightweight()) {
                    return;
                }
                firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        AccessibleState.VISIBLE, null);
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentResized(ComponentEvent e) {
            }

            public void componentShown(ComponentEvent e) {
                if (behaviour.isLightweight()) {
                    return;
                }
                firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY, null,
                        AccessibleState.VISIBLE);
            }
        }

        protected class AccessibleAWTFocusHandler implements FocusListener {
            public void focusGained(FocusEvent e) {
                if (behaviour.isLightweight()) {
                    return;
                }
                firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY, null,
                        AccessibleState.FOCUSED);
            }

            public void focusLost(FocusEvent e) {
                if (behaviour.isLightweight()) {
                    return;
                }
                firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        AccessibleState.FOCUSED, null);
            }
        }

        protected ComponentListener accessibleAWTComponentHandler;

        protected FocusListener accessibleAWTFocusHandler;

        /**
         * Number of registered property change listeners
         */
        int listenersCount;

        public void addFocusListener(FocusListener l) {
            Component.this.addFocusListener(l);
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            toolkit.lockAWT();
            try {
                super.addPropertyChangeListener(listener);
                listenersCount++;
                if (accessibleAWTComponentHandler == null) {
                    accessibleAWTComponentHandler = new AccessibleAWTComponentHandler();
                    Component.this.addComponentListener(accessibleAWTComponentHandler);
                }
                if (accessibleAWTFocusHandler == null) {
                    accessibleAWTFocusHandler = new AccessibleAWTFocusHandler();
                    Component.this.addFocusListener(accessibleAWTFocusHandler);
                }
            } finally {
                toolkit.unlockAWT();
            }
        }

        public boolean contains(Point p) {
            toolkit.lockAWT();
            try {
                return Component.this.contains(p);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public Accessible getAccessibleAt(Point arg0) {
            toolkit.lockAWT();
            try {
                return null;
            } finally {
                toolkit.unlockAWT();
            }
        }

        public Color getBackground() {
            toolkit.lockAWT();
            try {
                return Component.this.getBackground();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public Rectangle getBounds() {
            toolkit.lockAWT();
            try {
                return Component.this.getBounds();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public Cursor getCursor() {
            toolkit.lockAWT();
            try {
                return Component.this.getCursor();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public Font getFont() {
            toolkit.lockAWT();
            try {
                return Component.this.getFont();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public FontMetrics getFontMetrics(Font f) {
            toolkit.lockAWT();
            try {
                return Component.this.getFontMetrics(f);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public Color getForeground() {
            toolkit.lockAWT();
            try {
                return Component.this.getForeground();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public Point getLocation() {
            toolkit.lockAWT();
            try {
                return Component.this.getLocation();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public Point getLocationOnScreen() {
            toolkit.lockAWT();
            try {
                return Component.this.getLocationOnScreen();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public Dimension getSize() {
            toolkit.lockAWT();
            try {
                return Component.this.getSize();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public boolean isEnabled() {
            toolkit.lockAWT();
            try {
                return Component.this.isEnabled();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public boolean isFocusTraversable() {
            toolkit.lockAWT();
            try {
                return Component.this.isFocusTraversable();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public boolean isShowing() {
            toolkit.lockAWT();
            try {
                return Component.this.isShowing();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public boolean isVisible() {
            toolkit.lockAWT();
            try {
                return Component.this.isVisible();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void removeFocusListener(FocusListener l) {
            Component.this.removeFocusListener(l);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            toolkit.lockAWT();
            try {
                super.removePropertyChangeListener(listener);
                listenersCount--;
                if (listenersCount > 0) {
                    return;
                }
                // if there are no more listeners, remove handlers:
                Component.this.removeFocusListener(accessibleAWTFocusHandler);
                Component.this.removeComponentListener(accessibleAWTComponentHandler);
                accessibleAWTComponentHandler = null;
                accessibleAWTFocusHandler = null;
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void requestFocus() {
            toolkit.lockAWT();
            try {
                Component.this.requestFocus();
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void setBackground(Color color) {
            toolkit.lockAWT();
            try {
                Component.this.setBackground(color);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void setBounds(Rectangle r) {
            toolkit.lockAWT();
            try {
                Component.this.setBounds(r);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void setCursor(Cursor cursor) {
            toolkit.lockAWT();
            try {
                Component.this.setCursor(cursor);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void setEnabled(boolean enabled) {
            toolkit.lockAWT();
            try {
                Component.this.setEnabled(enabled);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void setFont(Font f) {
            toolkit.lockAWT();
            try {
                Component.this.setFont(f);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void setForeground(Color color) {
            toolkit.lockAWT();
            try {
                Component.this.setForeground(color);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void setLocation(Point p) {
            toolkit.lockAWT();
            try {
                Component.this.setLocation(p);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void setSize(Dimension size) {
            toolkit.lockAWT();
            try {
                Component.this.setSize(size);
            } finally {
                toolkit.unlockAWT();
            }
        }

        public void setVisible(boolean visible) {
            toolkit.lockAWT();
            try {
                Component.this.setVisible(visible);
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public Accessible getAccessibleParent() {
            toolkit.lockAWT();
            try {
                Accessible aParent = super.getAccessibleParent();
                if (aParent != null) {
                    return aParent;
                }
                Container parent = getParent();
                return (parent instanceof Accessible ? (Accessible) parent : null);
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public Accessible getAccessibleChild(int i) {
            toolkit.lockAWT();
            try {
                return null;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public int getAccessibleChildrenCount() {
            toolkit.lockAWT();
            try {
                return 0;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public AccessibleComponent getAccessibleComponent() {
            return this;
        }

        @Override
        public String getAccessibleDescription() {
            return super.getAccessibleDescription(); // why override?
        }

        @Override
        public int getAccessibleIndexInParent() {
            toolkit.lockAWT();
            try {
                if (getAccessibleParent() == null) {
                    return -1;
                }
                int count = 0;
                Container parent = getParent();
                for (int i = 0; i < parent.getComponentCount(); i++) {
                    Component aComp = parent.getComponent(i);
                    if (aComp instanceof Accessible) {
                        if (aComp == Component.this) {
                            return count;
                        }
                        ++count;
                    }
                }
                return -1;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            toolkit.lockAWT();
            try {
                return AccessibleRole.AWT_COMPONENT;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            toolkit.lockAWT();
            try {
                AccessibleStateSet set = new AccessibleStateSet();
                if (isEnabled()) {
                    set.add(AccessibleState.ENABLED);
                }
                if (isFocusable()) {
                    set.add(AccessibleState.FOCUSABLE);
                }
                if (hasFocus()) {
                    set.add(AccessibleState.FOCUSED);
                }
                if (isOpaque()) {
                    set.add(AccessibleState.OPAQUE);
                }
                if (isShowing()) {
                    set.add(AccessibleState.SHOWING);
                }
                if (isVisible()) {
                    set.add(AccessibleState.VISIBLE);
                }
                return set;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public Locale getLocale() throws IllegalComponentStateException {
            toolkit.lockAWT();
            try {
                return Component.this.getLocale();
            } finally {
                toolkit.unlockAWT();
            }
        }
    }

    protected class BltBufferStrategy extends BufferStrategy {
        protected VolatileImage[] backBuffers;

        protected BufferCapabilities caps;

        protected int width;

        protected int height;

        protected boolean validatedContents;

        protected BltBufferStrategy(int numBuffers, BufferCapabilities caps) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public boolean contentsLost() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public boolean contentsRestored() throws NotImplementedException {
            throw new NotImplementedException();
        }

        protected void createBackBuffers(int numBuffers) throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public BufferCapabilities getCapabilities() {
            return (BufferCapabilities) caps.clone();
        }

        @Override
        public Graphics getDrawGraphics() throws NotImplementedException {
            throw new NotImplementedException();
        }

        protected void revalidate() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public void show() throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    protected class FlipBufferStrategy extends BufferStrategy {
        protected BufferCapabilities caps;

        protected Image drawBuffer;

        protected VolatileImage drawVBuffer;

        protected int numBuffers;

        protected boolean validatedContents;

        protected FlipBufferStrategy(int numBuffers, BufferCapabilities caps)
                throws AWTException {
            if (!(Component.this instanceof Window) && !(Component.this instanceof Canvas)) {
                // awt.14B=Only Canvas or Window is allowed
                throw new ClassCastException(Messages.getString("awt.14B")); //$NON-NLS-1$
            }
            // TODO: throw new AWTException("Capabilities are not supported");
            this.numBuffers = numBuffers;
            this.caps = (BufferCapabilities) caps.clone();
        }

        @Override
        public boolean contentsLost() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public boolean contentsRestored() throws NotImplementedException {
            throw new NotImplementedException();
        }

        protected void createBuffers(int numBuffers, BufferCapabilities caps)
                throws AWTException,NotImplementedException {
            if (numBuffers < 2) {
                // awt.14C=Number of buffers must be greater than one
                throw new IllegalArgumentException(Messages.getString("awt.14C")); //$NON-NLS-1$
            }
            if (!caps.isPageFlipping()) {
                // awt.14D=Buffer capabilities should support flipping
                throw new IllegalArgumentException(Messages.getString("awt.14D")); //$NON-NLS-1$
            }
            if (!Component.this.behaviour.isDisplayable()) {
                // awt.14E=Component should be displayable
                throw new IllegalStateException(Messages.getString("awt.14E")); //$NON-NLS-1$
            }
            // TODO: throw new AWTException("Capabilities are not supported");
            throw new NotImplementedException();
        }

        protected void destroyBuffers() throws NotImplementedException {
            throw new NotImplementedException();
        }

        protected void flip(BufferCapabilities.FlipContents flipAction) throws NotImplementedException {
            throw new NotImplementedException();
        }

        protected Image getBackBuffer() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public BufferCapabilities getCapabilities() {
            return (BufferCapabilities) caps.clone();
        }

        @Override
        public Graphics getDrawGraphics() throws NotImplementedException {
            throw new NotImplementedException();
        }

        protected void revalidate() throws NotImplementedException {
            throw new NotImplementedException();
        }

        @Override
        public void show() throws NotImplementedException {
            throw new NotImplementedException();
        }
    }

    /**
     * The internal component's state utilized by the visual theme
     */
    class ComponentState implements State {
        private Dimension defaultMinimumSize = new Dimension();

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isVisible() {
            return visible;
        }

        public boolean isFocused() {
            return isFocusOwner();
        }

        public Font getFont() {
            return Component.this.getFont();
        }

        public boolean isFontSet() {
            return font != null;
        }

        public Color getBackground() {
            Color c = Component.this.getBackground();
            return (c != null) ? c : getDefaultBackground();
        }

        public boolean isBackgroundSet() {
            return backColor != null;
        }

        public Color getTextColor() {
            Color c = getForeground();
            return (c != null) ? c : getDefaultForeground();
        }

        public boolean isTextColorSet() {
            return foreColor != null;
        }

        @SuppressWarnings("deprecation")
        public FontMetrics getFontMetrics() {
            return toolkit.getFontMetrics(Component.this.getFont());
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, w, h);
        }

        public Dimension getSize() {
            return new Dimension(w, h);
        }

        public long getWindowId() {
            NativeWindow win = getNativeWindow();
            return (win != null) ? win.getId() : 0;
        }

        public Dimension getDefaultMinimumSize() {
            if (defaultMinimumSize == null) {
                calculate();
            }
            return defaultMinimumSize;
        }

        public void setDefaultMinimumSize(Dimension size) {
            defaultMinimumSize = size;
        }

        public void reset() {
            defaultMinimumSize = null;
        }

        public void calculate() {
            // to be overridden
        }
    }

    private transient AccessibleContext accessibleContext;

    final transient ComponentBehavior behaviour;

    Container parent;

    private String name;

    private boolean autoName = true;

    private Font font;

    private Color backColor;

    private Color foreColor;

    boolean deprecatedEventHandler = true;

    private long enabledEvents;

    private long enabledAWTEvents;

    private final AWTListenerList<ComponentListener> componentListeners = new AWTListenerList<ComponentListener>(
            this);

    private final AWTListenerList<FocusListener> focusListeners = new AWTListenerList<FocusListener>(
            this);

    private final AWTListenerList<HierarchyListener> hierarchyListeners = new AWTListenerList<HierarchyListener>(
            this);

    private final AWTListenerList<HierarchyBoundsListener> hierarchyBoundsListeners = new AWTListenerList<HierarchyBoundsListener>(
            this);

    private final AWTListenerList<KeyListener> keyListeners = new AWTListenerList<KeyListener>(
            this);

    private final AWTListenerList<MouseListener> mouseListeners = new AWTListenerList<MouseListener>(
            this);

    private final AWTListenerList<MouseMotionListener> mouseMotionListeners = new AWTListenerList<MouseMotionListener>(
            this);

    private final AWTListenerList<MouseWheelListener> mouseWheelListeners = new AWTListenerList<MouseWheelListener>(
            this);

    private final AWTListenerList<InputMethodListener> inputMethodListeners = new AWTListenerList<InputMethodListener>(
            this);

    int x;

    int y;

    int w;

    int h;

    private Dimension maximumSize;

    private Dimension minimumSize;

    private Dimension preferredSize;

    private int boundsMaskParam;

    private boolean ignoreRepaint;

    private boolean enabled = true;

    private boolean inputMethodsEnabled = true;

    transient boolean dispatchToIM = true;

    private boolean focusable = true; // By default, all Components return

    // true from isFocusable() method
    boolean visible = true;

    private boolean calledSetFocusable;

    private boolean overridenIsFocusable = true;

    private boolean focusTraversalKeysEnabled = true;

    /**
     * Possible keys are: FORWARD_TRAVERSAL_KEYS, BACKWARD_TRAVERSAL_KEYS,
     * UP_CYCLE_TRAVERSAL_KEYS
     */
    private final Map<Integer, Set<? extends AWTKeyStroke>> traversalKeys = new HashMap<Integer, Set<? extends AWTKeyStroke>>();

    int[] traversalIDs;

    private Locale locale;

    private ComponentOrientation orientation;

    private PropertyChangeSupport propertyChangeSupport;

    private ArrayList<PopupMenu> popups;

    private boolean coalescer;

    private Hashtable<Integer, LinkedList<AWTEvent>> eventsTable;

    /** Cashed reference used during EventQueue.postEvent() */
    private LinkedList<AWTEvent> eventsList;

    private int hierarchyChangingCounter;

    private boolean wasShowing;

    private boolean wasDisplayable;

    Cursor cursor;

    DropTarget dropTarget;

    private boolean mouseExitedExpected;

    transient MultiRectArea repaintRegion;

    transient RedrawManager redrawManager;

    private boolean valid;

    private HashMap<Image, ImageParameters> updatedImages;

    /**
     * The lock object for private component's data which don't affect the
     * component hierarchy
     */
    private class ComponentLock {
    }

    private final transient Object componentLock = new ComponentLock();
    static {
        PrivilegedAction<String[]> action = new PrivilegedAction<String[]>() {
            public String[] run() {
                String properties[] = new String[2];
                properties[0] = org.apache.harmony.awt.Utils.getSystemProperty("awt.image.redrawrate", "100"); //$NON-NLS-1$ //$NON-NLS-2$
                properties[1] = org.apache.harmony.awt.Utils.getSystemProperty("awt.image.incrementaldraw", "true"); //$NON-NLS-1$ //$NON-NLS-2$
                return properties;
            }
        };
        String properties[] = AccessController.doPrivileged(action);
        // FIXME: rate is never used, can this code and the get property above
        // be removed?
        // int rate;
        //
        // try {
        // rate = Integer.decode(properties[0]).intValue();
        // } catch (NumberFormatException e) {
        // rate = 100;
        // }
        incrementalImageUpdate = properties[1].equals("true"); //$NON-NLS-1$
    }

    protected Component() {
        toolkit.lockAWT();
        try {
            orientation = ComponentOrientation.UNKNOWN;
            redrawManager = null;
            traversalIDs = this instanceof Container ? KeyboardFocusManager.contTraversalIDs
                    : KeyboardFocusManager.compTraversalIDs;
            for (int element : traversalIDs) {
                traversalKeys.put(new Integer(element), null);
            }
            behaviour = createBehavior();
            deriveCoalescerFlag();
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * Determine that the class inherited from Component declares the method
     * coalesceEvents(), and put the results to the childClassesFlags map
     * 
     */
    private void deriveCoalescerFlag() {
        Class<?> thisClass = getClass();
        boolean flag = true;
        synchronized (childClassesFlags) {
            Boolean flagWrapper = childClassesFlags.get(thisClass);
            if (flagWrapper == null) {
                Method coalesceMethod = null;
                for (Class<?> c = thisClass; c != Component.class; c = c.getSuperclass()) {
                    try {
                        coalesceMethod = c.getDeclaredMethod("coalesceEvents", new Class[] { //$NON-NLS-1$
                                Class.forName("java.awt.AWTEvent"), //$NON-NLS-1$
                                Class.forName("java.awt.AWTEvent") }); //$NON-NLS-1$
                    } catch (Exception e) {
                    }
                    if (coalesceMethod != null) {
                        break;
                    }
                }
                flag = (coalesceMethod != null);
                childClassesFlags.put(thisClass, Boolean.valueOf(flag));
            } else {
                flag = flagWrapper.booleanValue();
            }
        }
        coalescer = flag;
        if (flag) {
            eventsTable = new Hashtable<Integer, LinkedList<AWTEvent>>();
        } else {
            eventsTable = null;
        }
    }

    public void setName(String name) {
        String oldName;
        toolkit.lockAWT();
        try {
            autoName = false;
            oldName = this.name;
            this.name = name;
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("name", oldName, name); //$NON-NLS-1$
    }

    public String getName() {
        toolkit.lockAWT();
        try {
            if ((name == null) && autoName) {
                name = autoName();
            }
            return name;
        } finally {
            toolkit.unlockAWT();
        }
    }

    String autoName() {
        String name = getClass().getName();
        if (name.indexOf('$') != -1) {
            return null;
        }
        int number = toolkit.autoNumber.nextComponent++;
        name = name.substring(name.lastIndexOf('.') + 1) + Integer.toString(number);
        return name;
    }

    @Override
    public String toString() {
        /*
         * The format is based on 1.5 release behavior which can be revealed by
         * the following code:
         * 
         * Component c = new Component(){}; c.setVisible(false);
         * System.out.println(c);
         */
        toolkit.lockAWT();
        try {
            return getClass().getName() + "[" + paramString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void add(PopupMenu popup) {
        toolkit.lockAWT();
        try {
            if (popup.getParent() == this) {
                return;
            }
            if (popups == null) {
                popups = new ArrayList<PopupMenu>();
            }
            popup.setParent(this);
            popups.add(popup);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean contains(Point p) {
        toolkit.lockAWT();
        try {
            return contains(p.x, p.y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean contains(int x, int y) {
        toolkit.lockAWT();
        try {
            return inside(x, y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Dimension size() {
        toolkit.lockAWT();
        try {
            return new Dimension(w, h);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Container getParent() {
        toolkit.lockAWT();
        try {
            return parent;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @return the nearest heavyweight ancestor in hierarchy or
     *         <code>null</code> if not found
     */
    Component getHWAncestor() {
        return (parent != null ? parent.getHWSurface() : null);
    }

    /**
     * @return heavyweight component that is equal to or is a nearest
     *         heavyweight container of the current component, or
     *         <code>null</code> if not found
     */
    Component getHWSurface() {
        Component parent;
        for (parent = this; (parent != null) && (parent.isLightweight()); parent = parent
                .getParent()) {
            ;
        }
        return parent;
    }

    Window getWindowAncestor() {
        Component par;
        for (par = this; par != null && !(par instanceof Window); par = par.getParent()) {
            ;
        }
        return (Window) par;
    }

    /** To be called by container */
    void setParent(Container parent) {
        this.parent = parent;
        setRedrawManager();
    }

    void setRedrawManager() {
        redrawManager = getRedrawManager();
    }

    public void remove(MenuComponent menu) {
        toolkit.lockAWT();
        try {
            if (menu.getParent() == this) {
                menu.setParent(null);
                popups.remove(menu);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void list(PrintStream out, int indent) {
        toolkit.lockAWT();
        try {
            out.println(getIndentStr(indent) + this);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void list(PrintWriter out) {
        toolkit.lockAWT();
        try {
            list(out, 1);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void list(PrintWriter out, int indent) {
        toolkit.lockAWT();
        try {
            out.println(getIndentStr(indent) + this);
        } finally {
            toolkit.unlockAWT();
        }
    }

    String getIndentStr(int indent) {
        char[] ind = new char[indent];
        for (int i = 0; i < indent; ind[i++] = ' ') {
            ;
        }
        return new String(ind);
    }

    public void list(PrintStream out) {
        toolkit.lockAWT();
        try {
            // default indent = 1
            list(out, 1);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void list() {
        toolkit.lockAWT();
        try {
            list(System.out);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void print(Graphics g) {
        toolkit.lockAWT();
        try {
            paint(g);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void printAll(Graphics g) {
        toolkit.lockAWT();
        try {
            paintAll(g);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setSize(int width, int height) {
        toolkit.lockAWT();
        try {
            resize(width, height);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setSize(Dimension d) {
        toolkit.lockAWT();
        try {
            resize(d);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void resize(int width, int height) {
        toolkit.lockAWT();
        try {
            boundsMaskParam = NativeWindow.BOUNDS_NOMOVE;
            setBounds(x, y, width, height);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void resize(Dimension size) {
        toolkit.lockAWT();
        try {
            setSize(size.width, size.height);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isOpaque() {
        toolkit.lockAWT();
        try {
            return behaviour.isOpaque();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void disable() {
        toolkit.lockAWT();
        try {
            setEnabledImpl(false);
        } finally {
            toolkit.unlockAWT();
        }
        fireAccessibleStateChange(AccessibleState.ENABLED, false);
    }

    @Deprecated
    public void enable() {
        toolkit.lockAWT();
        try {
            setEnabledImpl(true);
        } finally {
            toolkit.unlockAWT();
        }
        fireAccessibleStateChange(AccessibleState.ENABLED, true);
    }

    @Deprecated
    public void enable(boolean b) {
        toolkit.lockAWT();
        try {
            if (b) {
                enable();
            } else {
                disable();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Point getLocation(Point rv) {
        toolkit.lockAWT();
        try {
            if (rv == null) {
                rv = new Point();
            }
            rv.setLocation(getX(), getY());
            return rv;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Point getLocation() {
        toolkit.lockAWT();
        try {
            return location();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getSize() {
        toolkit.lockAWT();
        try {
            return size();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getSize(Dimension rv) {
        toolkit.lockAWT();
        try {
            if (rv == null) {
                rv = new Dimension();
            }
            rv.setSize(getWidth(), getHeight());
            return rv;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isValid() {
        toolkit.lockAWT();
        try {
            return valid && behaviour.isDisplayable();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Point location() {
        toolkit.lockAWT();
        try {
            return new Point(x, y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void addNotify() {
        toolkit.lockAWT();
        try {
            prepare4HierarchyChange();
            behaviour.addNotify();
            finishHierarchyChange(this, parent, 0);
            if (dropTarget != null) {
                dropTarget.addNotify(peer);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    void mapToDisplay(boolean b) {
        if (b && !isDisplayable()) {
            if ((this instanceof Window) || ((parent != null) && parent.isDisplayable())) {
                addNotify();
            }
        } else if (!b && isDisplayable()) {
            removeNotify();
        }
    }

    /**
     * @return accessible context specific for particular component
     */
    AccessibleContext createAccessibleContext() {
        return null;
    }

    public AccessibleContext getAccessibleContext() {
        toolkit.lockAWT();
        try {
            if (accessibleContext == null) {
                accessibleContext = createAccessibleContext();
            }
            return accessibleContext;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Toolkit getToolkit() {
        return toolkit;
    }

    public final Object getTreeLock() {
        return toolkit.awtTreeLock;
    }

    @Deprecated
    public boolean action(Event evt, Object what) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    private PropertyChangeSupport getPropertyChangeSupport() {
        synchronized (componentLock) {
            if (propertyChangeSupport == null) {
                propertyChangeSupport = new PropertyChangeSupport(this);
            }
            return propertyChangeSupport;
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getPropertyChangeSupport().addPropertyChangeListener(propertyName, listener);
    }

    public void applyComponentOrientation(ComponentOrientation orientation) {
        toolkit.lockAWT();
        try {
            setComponentOrientation(orientation);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean areFocusTraversalKeysSet(int id) {
        toolkit.lockAWT();
        try {
            Integer Id = new Integer(id);
            if (traversalKeys.containsKey(Id)) {
                return traversalKeys.get(Id) != null;
            }
            // awt.14F=invalid focus traversal key identifier
            throw new IllegalArgumentException(Messages.getString("awt.14F")); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Rectangle bounds() {
        toolkit.lockAWT();
        try {
            return new Rectangle(x, y, w, h);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int checkImage(Image image, int width, int height, ImageObserver observer) {
        toolkit.lockAWT();
        try {
            return toolkit.checkImage(image, width, height, observer);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int checkImage(Image image, ImageObserver observer) {
        toolkit.lockAWT();
        try {
            return toolkit.checkImage(image, -1, -1, observer);
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected AWTEvent coalesceEvents(AWTEvent existingEvent, AWTEvent newEvent) {
        toolkit.lockAWT();
        try {
            // Nothing to do:
            // 1. Mouse events coalesced at WTK level
            // 2. Paint events handled by RedrawManager
            // This method is for overriding only
            return null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    boolean isCoalescer() {
        return coalescer;
    }

    AWTEvent getRelativeEvent(int id) {
        Integer idWrapper = new Integer(id);
        eventsList = eventsTable.get(idWrapper);
        if (eventsList == null) {
            eventsList = new LinkedList<AWTEvent>();
            eventsTable.put(idWrapper, eventsList);
            return null;
        }
        if (eventsList.isEmpty()) {
            return null;
        }
        return eventsList.getLast();
    }

    void addNewEvent(AWTEvent event) {
        eventsList.addLast(event);
    }

    void removeRelativeEvent() {
        eventsList.removeLast();
    }

    void removeNextEvent(int id) {
        eventsTable.get(new Integer(id)).removeFirst();
    }

    public Image createImage(ImageProducer producer) {
        toolkit.lockAWT();
        try {
            return toolkit.createImage(producer);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Image createImage(int width, int height) {
        toolkit.lockAWT();
        try {
            if (!isDisplayable()) {
                return null;
            }
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc == null) {
                return null;
            }
            ColorModel cm = gc.getColorModel(Transparency.OPAQUE);
            WritableRaster wr = cm.createCompatibleWritableRaster(width, height);
            Image image = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
            fillImageBackground(image, width, height);
            return image;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public VolatileImage createVolatileImage(int width, int height, ImageCapabilities caps)
            throws AWTException {
        toolkit.lockAWT();
        try {
            if (!isDisplayable()) {
                return null;
            }
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc == null) {
                return null;
            }
            VolatileImage image = gc.createCompatibleVolatileImage(width, height, caps);
            fillImageBackground(image, width, height);
            return image;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public VolatileImage createVolatileImage(int width, int height) {
        toolkit.lockAWT();
        try {
            if (!isDisplayable()) {
                return null;
            }
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc == null) {
                return null;
            }
            VolatileImage image = gc.createCompatibleVolatileImage(width, height);
            fillImageBackground(image, width, height);
            return image;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * Fill the image being created by createImage() or createVolatileImage()
     * with the component's background color to prepare it for double-buffered
     * painting
     */
    private void fillImageBackground(Image image, int width, int height) {
        Graphics gr = image.getGraphics();
        gr.setColor(getBackground());
        gr.fillRect(0, 0, width, height);
        gr.dispose();
    }

    @Deprecated
    public void deliverEvent(Event evt) {
        postEvent(evt);
    }

    public void doLayout() {
        toolkit.lockAWT();
        try {
            layout();
        } finally {
            toolkit.unlockAWT();
        }
        // Implemented in Container
    }

    private void firePropertyChangeImpl(String propertyName, Object oldValue, Object newValue) {
        PropertyChangeSupport pcs;
        synchronized (componentLock) {
            if (propertyChangeSupport == null) {
                return;
            }
            pcs = propertyChangeSupport;
        }
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
        firePropertyChangeImpl(propertyName, new Integer(oldValue), new Integer(newValue));
    }

    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        firePropertyChangeImpl(propertyName, Boolean.valueOf(oldValue), Boolean
                .valueOf(newValue));
    }

    protected void firePropertyChange(final String propertyName, final Object oldValue,
            final Object newValue) {
        firePropertyChangeImpl(propertyName, oldValue, newValue);
    }

    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
        firePropertyChangeImpl(propertyName, new Byte(oldValue), new Byte(newValue));
    }

    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
        firePropertyChangeImpl(propertyName, new Character(oldValue), new Character(newValue));
    }

    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
        firePropertyChangeImpl(propertyName, new Short(oldValue), new Short(newValue));
    }

    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
        firePropertyChangeImpl(propertyName, new Long(oldValue), new Long(newValue));
    }

    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
        firePropertyChangeImpl(propertyName, new Float(oldValue), new Float(newValue));
    }

    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
        firePropertyChangeImpl(propertyName, new Double(oldValue), new Double(newValue));
    }

    public float getAlignmentX() {
        toolkit.lockAWT();
        try {
            return CENTER_ALIGNMENT;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public float getAlignmentY() {
        toolkit.lockAWT();
        try {
            return CENTER_ALIGNMENT;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Color getBackground() {
        toolkit.lockAWT();
        try {
            if ((backColor == null) && (parent != null)) {
                return parent.getBackground();
            }
            return backColor;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Rectangle getBounds() {
        toolkit.lockAWT();
        try {
            return bounds();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Rectangle getBounds(Rectangle rv) {
        toolkit.lockAWT();
        try {
            if (rv == null) {
                rv = new Rectangle();
            }
            rv.setBounds(x, y, w, h);
            return rv;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public ColorModel getColorModel() {
        toolkit.lockAWT();
        try {
            return getToolkit().getColorModel();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component getComponentAt(Point p) {
        toolkit.lockAWT();
        try {
            return getComponentAt(p.x, p.y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component getComponentAt(int x, int y) {
        toolkit.lockAWT();
        try {
            return locate(x, y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public ComponentOrientation getComponentOrientation() {
        toolkit.lockAWT();
        try {
            return orientation;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Cursor getCursor() {
        toolkit.lockAWT();
        try {
            if (cursor != null) {
                return cursor;
            } else if (parent != null) {
                return parent.getCursor();
            }
            return Cursor.getDefaultCursor();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public DropTarget getDropTarget() {
        toolkit.lockAWT();
        try {
            return dropTarget;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Container getFocusCycleRootAncestor() {
        toolkit.lockAWT();
        try {
            for (Container c = parent; c != null; c = c.getParent()) {
                if (c.isFocusCycleRoot()) {
                    return c;
                }
            }
            return null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @SuppressWarnings("unchecked")
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        toolkit.lockAWT();
        try {
            Integer kId = new Integer(id);
            KeyboardFocusManager.checkTraversalKeysID(traversalKeys, kId);
            Set<? extends AWTKeyStroke> keys = traversalKeys.get(kId);
            if (keys == null && parent != null) {
                keys = parent.getFocusTraversalKeys(id);
            }
            if (keys == null) {
                keys = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getDefaultFocusTraversalKeys(id);
            }
            return (Set<AWTKeyStroke>) keys;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean getFocusTraversalKeysEnabled() {
        toolkit.lockAWT();
        try {
            return focusTraversalKeysEnabled;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @SuppressWarnings("deprecation")
    public FontMetrics getFontMetrics(Font f) {
        return toolkit.getFontMetrics(f);
    }

    public Color getForeground() {
        toolkit.lockAWT();
        try {
            if (foreColor == null && parent != null) {
                return parent.getForeground();
            }
            return foreColor;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Graphics getGraphics() {
        toolkit.lockAWT();
        try {
            if (!isDisplayable()) {
                return null;
            }
            Graphics g = behaviour.getGraphics(0, 0, w, h);
            if(g instanceof Graphics2D) {
                ((Graphics2D)g).setBackground(this.getBackground());
            }
            g.setColor(foreColor);
            g.setFont(getFont());
            return g;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public GraphicsConfiguration getGraphicsConfiguration() {
        toolkit.lockAWT();
        try {
            Window win = getWindowAncestor();
            if (win == null) {
                return null;
            }
            return win.getGraphicsConfiguration();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getHeight() {
        toolkit.lockAWT();
        try {
            return h;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean getIgnoreRepaint() {
        toolkit.lockAWT();
        try {
            return ignoreRepaint;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public InputContext getInputContext() {
        toolkit.lockAWT();
        try {
            Container parent = getParent();
            if (parent != null) {
                return parent.getInputContext();
            }
            return null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public InputMethodRequests getInputMethodRequests() {
        return null;
    }

    public Locale getLocale() {
        toolkit.lockAWT();
        try {
            if (locale == null) {
                if (parent == null) {
                    if (this instanceof Window) {
                        return Locale.getDefault();
                    }
                    // awt.150=no parent
                    throw new IllegalComponentStateException(Messages.getString("awt.150")); //$NON-NLS-1$
                }
                return getParent().getLocale();
            }
            return locale;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Point getLocationOnScreen() throws IllegalComponentStateException {
        toolkit.lockAWT();
        try {
            Point p = new Point();
            if (isShowing()) {
                Component comp;
                for (comp = this; comp != null && !(comp instanceof Window); comp = comp
                        .getParent()) {
                    p.translate(comp.getX(), comp.getY());
                }
                if (comp instanceof Window) {
                    p.translate(comp.getX(), comp.getY());
                }
                return p;
            }
            // awt.151=component must be showing on the screen to determine its location
            throw new IllegalComponentStateException(Messages.getString("awt.151")); //$NON-NLS-1$
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public ComponentPeer getPeer() {
        toolkit.lockAWT();
        try {
            if (behaviour.isDisplayable()) {
                return peer;
            }
            return null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return getPropertyChangeSupport().getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return getPropertyChangeSupport().getPropertyChangeListeners(propertyName);
    }

    public int getWidth() {
        toolkit.lockAWT();
        try {
            return w;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getX() {
        toolkit.lockAWT();
        try {
            return x;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getY() {
        toolkit.lockAWT();
        try {
            return y;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public boolean gotFocus(Event evt, Object what) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public boolean handleEvent(Event evt) {
        switch (evt.id) {
            case Event.ACTION_EVENT:
                return action(evt, evt.arg);
            case Event.GOT_FOCUS:
                return gotFocus(evt, null);
            case Event.LOST_FOCUS:
                return lostFocus(evt, null);
            case Event.MOUSE_DOWN:
                return mouseDown(evt, evt.x, evt.y);
            case Event.MOUSE_DRAG:
                return mouseDrag(evt, evt.x, evt.y);
            case Event.MOUSE_ENTER:
                return mouseEnter(evt, evt.x, evt.y);
            case Event.MOUSE_EXIT:
                return mouseExit(evt, evt.x, evt.y);
            case Event.MOUSE_MOVE:
                return mouseMove(evt, evt.x, evt.y);
            case Event.MOUSE_UP:
                return mouseUp(evt, evt.x, evt.y);
            case Event.KEY_ACTION:
            case Event.KEY_PRESS:
                return keyDown(evt, evt.key);
            case Event.KEY_ACTION_RELEASE:
            case Event.KEY_RELEASE:
                return keyUp(evt, evt.key);
        }
        return false;// event not handled
    }

    public boolean hasFocus() {
        toolkit.lockAWT();
        try {
            return isFocusOwner();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void hide() {
        toolkit.lockAWT();
        try {
            if (!visible) {
                return;
            }
            prepare4HierarchyChange();
            visible = false;
            moveFocusOnHide();
            behaviour.setVisible(false);
            postEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_HIDDEN));
            finishHierarchyChange(this, parent, 0);
            notifyInputMethod(null);
            invalidateRealParent();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public boolean inside(int x, int y) {
        toolkit.lockAWT();
        try {
            return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void invalidate() {
        toolkit.lockAWT();
        try {
            valid = false;
            resetDefaultSize();
            invalidateRealParent();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isBackgroundSet() {
        toolkit.lockAWT();
        try {
            return backColor != null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isCursorSet() {
        toolkit.lockAWT();
        try {
            return cursor != null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isDisplayable() {
        toolkit.lockAWT();
        try {
            return behaviour.isDisplayable();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isDoubleBuffered() {
        toolkit.lockAWT();
        try {
            // false by default
            return false;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isEnabled() {
        toolkit.lockAWT();
        try {
            return enabled;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * "Recursive" isEnabled().
     * 
     * @return true if not only component itself is enabled but its heavyweight
     *         parent is also "indirectly" enabled
     */
    boolean isIndirectlyEnabled() {
        Component comp = this;
        while (comp != null) {
            if (!comp.isLightweight() && !comp.isEnabled()) {
                return false;
            }
            comp = comp.getRealParent();
        }
        return true;
    }

    boolean isKeyEnabled() {
        if (!isEnabled()) {
            return false;
        }
        return isIndirectlyEnabled();
    }

    /**
     * Gets only parent of a child component, but not owner of a window.
     * 
     * @return parent of child component, null if component is a top-level
     *         (Window instance)
     */
    Container getRealParent() {
        return (!(this instanceof Window) ? getParent() : null);
    }

    public boolean isFocusCycleRoot(Container container) {
        toolkit.lockAWT();
        try {
            return getFocusCycleRootAncestor() == container;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isFocusOwner() {
        toolkit.lockAWT();
        try {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == this;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public boolean isFocusTraversable() {
        toolkit.lockAWT();
        try {
            overridenIsFocusable = false;
            return focusable; // a Component must either be both focusable and
            // focus traversable, or neither
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isFocusable() {
        toolkit.lockAWT();
        try {
            return isFocusTraversable();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isFontSet() {
        toolkit.lockAWT();
        try {
            return font != null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isForegroundSet() {
        toolkit.lockAWT();
        try {
            return foreColor != null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isLightweight() {
        toolkit.lockAWT();
        try {
            return behaviour.isLightweight();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isShowing() {
        toolkit.lockAWT();
        try {
            return (isVisible() && isDisplayable() && (parent != null) && parent.isShowing());
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isVisible() {
        toolkit.lockAWT();
        try {
            return visible;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public boolean keyDown(Event evt, int key) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public boolean keyUp(Event evt, int key) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public void layout() {
        toolkit.lockAWT();
        try {
            // Implemented in Container
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Component locate(int x, int y) {
        toolkit.lockAWT();
        try {
            if (contains(x, y)) {
                return this;
            }
            return null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public boolean lostFocus(Event evt, Object what) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public boolean mouseDown(Event evt, int x, int y) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public boolean mouseDrag(Event evt, int x, int y) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public boolean mouseEnter(Event evt, int x, int y) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public boolean mouseExit(Event evt, int x, int y) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public boolean mouseMove(Event evt, int x, int y) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public boolean mouseUp(Event evt, int x, int y) {
        // to be overridden: do nothing,
        // just return false to propagate event up to the parent container
        return false;
    }

    @Deprecated
    public void move(int x, int y) {
        toolkit.lockAWT();
        try {
            boundsMaskParam = NativeWindow.BOUNDS_NOSIZE;
            setBounds(x, y, w, h);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void nextFocus() {
        toolkit.lockAWT();
        try {
            transferFocus(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected String paramString() {
        /*
         * The format is based on 1.5 release behavior which can be revealed by
         * the following code:
         * 
         * Component c = new Component(){}; c.setVisible(false);
         * System.out.println(c);
         */
        toolkit.lockAWT();
        try {
            return getName() + "," + getX() + "," + getY() + "," + getWidth() + "x" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    + getHeight() + (!isVisible() ? ",hidden" : ""); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    public boolean postEvent(Event evt) {
        boolean handled = handleEvent(evt);
        if (handled) {
            return true;
        }
        // propagate non-handled events up to parent
        Component par = parent;
        // try to call postEvent only on components which
        // override any of deprecated method handlers
        // while (par != null && !par.deprecatedEventHandler) {
        // par = par.parent;
        // }
        // translate event coordinates before posting it to parent
        if (par != null) {
            evt.translate(x, y);
            par.postEvent(evt);
        }
        return false;
    }

    public boolean prepareImage(Image image, ImageObserver observer) {
        toolkit.lockAWT();
        try {
            return toolkit.prepareImage(image, -1, -1, observer);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
        toolkit.lockAWT();
        try {
            return toolkit.prepareImage(image, width, height, observer);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void removeNotify() {
        toolkit.lockAWT();
        try {
            if (dropTarget != null) {
                dropTarget.removeNotify(peer);
            }
            prepare4HierarchyChange();
            moveFocus();
            behaviour.removeNotify();
            finishHierarchyChange(this, parent, 0);
            removeNotifyInputContext();
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * Calls InputContext.removeNotify
     */
    private void removeNotifyInputContext() {
        if (!inputMethodsEnabled) {
            return;
        }
        InputContext ic = getInputContext();
        if (ic != null) {
            ic.removeNotify(this);
        }
    }

    /**
     * This method is called when some property of a component changes, making
     * it unfocusable, e. g. hide(), removeNotify(), setEnabled(false),
     * setFocusable(false) is called, and therefore automatic forward focus
     * traversal is necessary
     */
    void moveFocus() {
        // don't use transferFocus(), but query focus traversal policy directly
        // and if it returns null, transfer focus up cycle
        // and find next focusable component there
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Container root = kfm.getCurrentFocusCycleRoot();
        Component nextComp = this;
        boolean success = !isFocusOwner();
        while (!success) {
            if (root != nextComp.getFocusCycleRootAncestor()) {
                // component was probably removed from container
                // so focus will be lost in some time
                return;
            }
            nextComp = root.getFocusTraversalPolicy().getComponentAfter(root, nextComp);
            if (nextComp == this) {
                nextComp = null; // avoid looping
            }
            if (nextComp != null) {
                success = nextComp.requestFocusInWindow();
            } else {
                nextComp = root;
                root = root.getFocusCycleRootAncestor();
                // if no acceptable component is found at all - clear global
                // focus owner
                if (root == null) {
                    if (nextComp instanceof Window) {
                        Window wnd = (Window) nextComp;
                        wnd.setFocusOwner(null);
                        wnd.setRequestedFocus(null);
                    }
                    kfm.clearGlobalFocusOwner();
                    return;
                }
            }
        }
    }

    /**
     * For Container there's a difference between moving focus when being made
     * invisible or made unfocusable in some other way, because when container
     * is made invisible, component still remains visible, i. e. its hide() or
     * setVisible() is not called.
     */
    void moveFocusOnHide() {
        moveFocus();
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        getPropertyChangeSupport().removePropertyChangeListener(propertyName, listener);
    }

    public void repaint(long tm, int x, int y, int width, int height) {
        toolkit.lockAWT();
        try {
            if (width <= 0 || height <= 0 || (redrawManager == null) || !isShowing()) {
                return;
            }
            if (behaviour instanceof LWBehavior) {
                if (parent == null || !parent.visible || !parent.behaviour.isDisplayable()) {
                    return;
                }
                if (repaintRegion == null) {
                    repaintRegion = new MultiRectArea(new Rectangle(x, y, width, height));
                }
                repaintRegion.intersect(new Rectangle(0, 0, this.w, this.h));
                repaintRegion.translate(this.x, this.y);
                parent.repaintRegion = repaintRegion;
                repaintRegion = null;
                parent.repaint(tm, x + this.x, y + this.y, width, height);
            } else {
                if (repaintRegion != null) {
                    redrawManager.addUpdateRegion(this, repaintRegion);
                    repaintRegion = null;
                } else {
                    redrawManager.addUpdateRegion(this, new Rectangle(x, y, width, height));
                }
                toolkit.getSystemEventQueueCore().notifyEventMonitor(toolkit);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    void postEvent(AWTEvent e) {
        getToolkit().getSystemEventQueueImpl().postEvent(e);
    }

    public void repaint(int x, int y, int width, int height) {
        toolkit.lockAWT();
        try {
            repaint(0, x, y, width, height);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void repaint() {
        toolkit.lockAWT();
        try {
            if (w > 0 && h > 0) {
                repaint(0, 0, 0, w, h);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void repaint(long tm) {
        toolkit.lockAWT();
        try {
            repaint(tm, 0, 0, w, h);
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected boolean requestFocus(boolean temporary) {
        toolkit.lockAWT();
        try {
            return requestFocusImpl(temporary, true, false);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void requestFocus() {
        toolkit.lockAWT();
        try {
            requestFocus(false);
        } finally {
            toolkit.unlockAWT();
        }
    }

    protected boolean requestFocusInWindow(boolean temporary) {
        toolkit.lockAWT();
        try {
            Window wnd = getWindowAncestor();
            if ((wnd == null) || !wnd.isFocused()) {
                return false;
            }
            return requestFocusImpl(temporary, false, false);
        } finally {
            toolkit.unlockAWT();
        }
    }

    boolean requestFocusImpl(boolean temporary, boolean crossWindow, boolean rejectionRecovery) {
        if (!rejectionRecovery && isFocusOwner()) {
            return true;
        }
        Window wnd = getWindowAncestor();
        Container par = getRealParent();
        if ((par != null) && par.isRemoved) {
            return false;
        }
        if (!isShowing() || !isFocusable() || !wnd.isFocusableWindow()) {
            return false;
        }
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().requestFocus(this,
                temporary, crossWindow, true);
    }

    public boolean requestFocusInWindow() {
        toolkit.lockAWT();
        try {
            return requestFocusInWindow(false);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void reshape(int x, int y, int w, int h) {
        toolkit.lockAWT();
        try {
            setBounds(x, y, w, h, boundsMaskParam, true);
            boundsMaskParam = 0;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setBounds(int x, int y, int w, int h) {
        toolkit.lockAWT();
        try {
            reshape(x, y, w, h);
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * Update the component bounds and post the appropriate events
     */
    void setBounds(int x, int y, int w, int h, int bMask, boolean updateBehavior) {
        int oldX = this.x;
        int oldY = this.y;
        int oldW = this.w;
        int oldH = this.h;
        setBoundsFields(x, y, w, h, bMask);
        // Moved
        if ((oldX != this.x) || (oldY != this.y)) {
            invalidateRealParent();
            postEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_MOVED));
            spreadHierarchyBoundsEvents(this, HierarchyEvent.ANCESTOR_MOVED);
        }
        // Resized
        if ((oldW != this.w) || (oldH != this.h)) {
            invalidate();
            postEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_RESIZED));
            spreadHierarchyBoundsEvents(this, HierarchyEvent.ANCESTOR_RESIZED);
        }
        if (updateBehavior) {
            behaviour.setBounds(this.x, this.y, this.w, this.h, bMask);
        }
        notifyInputMethod(new Rectangle(x, y, w, h));
    }

    /**
     * Calls InputContextImpl.notifyClientWindowChanged.
     */
    void notifyInputMethod(Rectangle bounds) {
        // only Window actually notifies IM of bounds change
    }

    private void setBoundsFields(int x, int y, int w, int h, int bMask) {
        if ((bMask & NativeWindow.BOUNDS_NOSIZE) == 0) {
            this.w = w;
            this.h = h;
        }
        if ((bMask & NativeWindow.BOUNDS_NOMOVE) == 0) {
            this.x = x;
            this.y = y;
        }
    }

    Insets getNativeInsets() {
        return new Insets(0, 0, 0, 0);
    }

    Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    boolean isMouseExitedExpected() {
        return mouseExitedExpected;
    }

    void setMouseExitedExpected(boolean expected) {
        mouseExitedExpected = expected;
    }

    public void setBounds(Rectangle r) {
        toolkit.lockAWT();
        try {
            setBounds(r.x, r.y, r.width, r.height);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setComponentOrientation(ComponentOrientation o) {
        ComponentOrientation oldOrientation;
        toolkit.lockAWT();
        try {
            oldOrientation = orientation;
            orientation = o;
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("componentOrientation", oldOrientation, orientation); //$NON-NLS-1$
        invalidate();
    }

    public void setCursor(Cursor cursor) {
        toolkit.lockAWT();
        try {
            this.cursor = cursor;
            setCursor();
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * Set current cursor shape to Component's Cursor
     */
    void setCursor() {
        if (isDisplayable() && isShowing()) {
            Rectangle absRect = new Rectangle(getLocationOnScreen(), getSize());
            Point absPointerPos = toolkit.dispatcher.mouseDispatcher.getPointerPos();
            if (absRect.contains(absPointerPos)) {
                // set Cursor only on top-level Windows(on X11)
                Window topLevelWnd = getWindowAncestor();
                if (topLevelWnd != null) {
                    Point pointerPos = MouseDispatcher.convertPoint(null, absPointerPos,
                            topLevelWnd);
                    Component compUnderCursor = topLevelWnd.findComponentAt(pointerPos);
                    // if (compUnderCursor == this ||
                    // compUnderCursor.getCursorAncestor() == this) {
                    NativeWindow wnd = topLevelWnd.getNativeWindow();
                    if (compUnderCursor != null && wnd != null) {
                        compUnderCursor.getRealCursor().getNativeCursor()
                                .setCursor(wnd.getId());
                    }
                    // }
                }
            }
        }
    }

    /**
     * Gets the ancestor Cursor if Component is disabled (directly or via an
     * ancestor) even if Cursor is explicitly set
     * 
     * @return actual Cursor to be displayed
     */
    Cursor getRealCursor() {
        Component cursorAncestor = getCursorAncestor();
        return cursorAncestor != null ? cursorAncestor.getCursor() : Cursor.getDefaultCursor();
    }

    /**
     * Gets the ancestor(or component itself) whose cursor is set when pointer
     * is inside component
     * 
     * @return actual Cursor to be displayed
     */
    Component getCursorAncestor() {
        Component comp;
        for (comp = this; comp != null; comp = comp.getParent()) {
            if (comp instanceof Window || comp.isCursorSet() && comp.isKeyEnabled()) {
                return comp;
            }
        }
        return null;
    }

    public void setDropTarget(DropTarget dt) {
        toolkit.lockAWT();
        try {
            if (dropTarget == dt) {
                return;
            }
            DropTarget oldDropTarget = dropTarget;
            dropTarget = dt;
            if (oldDropTarget != null) {
                if (behaviour.isDisplayable()) {
                    oldDropTarget.removeNotify(peer);
                }
                oldDropTarget.setComponent(null);
            }
            if (dt != null) {
                dt.setComponent(this);
                if (behaviour.isDisplayable()) {
                    dt.addNotify(peer);
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setEnabled(boolean value) {
        toolkit.lockAWT();
        try {
            enable(value);
        } finally {
            toolkit.unlockAWT();
        }
    }

    void setEnabledImpl(boolean value) {
        if (enabled != value) {
            enabled = value;
            setCursor();
            if (!enabled) {
                moveFocusOnHide();
            }
            behaviour.setEnabled(value);
        }
    }

    private void fireAccessibleStateChange(AccessibleState state, boolean value) {
        if (behaviour.isLightweight()) {
            return;
        }
        AccessibleContext ac = getAccessibleContext();
        if (ac != null) {
            AccessibleState oldValue = null;
            AccessibleState newValue = null;
            if (value) {
                newValue = state;
            } else {
                oldValue = state;
            }
            ac.firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY, oldValue,
                    newValue);
        }
    }

    public void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
        Set<? extends AWTKeyStroke> oldTraversalKeys;
        String propName = "FocusTraversalKeys"; //$NON-NLS-1$
        toolkit.lockAWT();
        try {
            Integer kId = new Integer(id);
            KeyboardFocusManager.checkTraversalKeysID(traversalKeys, kId);
            Map<Integer, Set<? extends AWTKeyStroke>> keys = new HashMap<Integer, Set<? extends AWTKeyStroke>>();
            for (int kid : traversalIDs) {
                Integer key = new Integer(kid);
                keys.put(key, getFocusTraversalKeys(kid));
            }
            KeyboardFocusManager.checkKeyStrokes(traversalIDs, keys, kId, keystrokes);
            oldTraversalKeys = traversalKeys.get(new Integer(id));
            // put a copy of keystrokes object into map:
            Set<? extends AWTKeyStroke> newKeys = keystrokes;
            if (keystrokes != null) {
                newKeys = new HashSet<AWTKeyStroke>(keystrokes);
            }
            traversalKeys.put(kId, newKeys);
            String direction = ""; //$NON-NLS-1$
            switch (id) {
                case KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS:
                    direction = "forward"; //$NON-NLS-1$
                    break;
                case KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS:
                    direction = "backward"; //$NON-NLS-1$
                    break;
                case KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS:
                    direction = "upCycle"; //$NON-NLS-1$
                    break;
                case KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS:
                    direction = "downCycle"; //$NON-NLS-1$
                    break;
            }
            propName = direction + propName;
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange(propName, oldTraversalKeys, keystrokes);
    }

    public void setFocusTraversalKeysEnabled(boolean value) {
        boolean oldFocusTraversalKeysEnabled;
        toolkit.lockAWT();
        try {
            oldFocusTraversalKeysEnabled = focusTraversalKeysEnabled;
            focusTraversalKeysEnabled = value;
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("focusTraversalKeysEnabled", oldFocusTraversalKeysEnabled, //$NON-NLS-1$
                focusTraversalKeysEnabled);
    }

    public void setFocusable(boolean focusable) {
        boolean oldFocusable;
        toolkit.lockAWT();
        try {
            calledSetFocusable = true;
            oldFocusable = this.focusable;
            this.focusable = focusable;
            if (!focusable) {
                moveFocus();
            }
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("focusable", oldFocusable, focusable); //$NON-NLS-1$
    }

    public Font getFont() {
        toolkit.lockAWT();
        try {
            return (font == null) && (parent != null) ? parent.getFont() : font;
        } finally {
            toolkit.unlockAWT();
        }
    }
    

    public void setFont(Font f) {
        Font oldFont;
        toolkit.lockAWT();
        try {
            oldFont = font;
            setFontImpl(f);
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("font", oldFont, font); //$NON-NLS-1$
    }

    void setFontImpl(Font f) {
        if (!(f == font ||
             (f != null && f.equals(font)))) {// to avoid dead loop in repaint()
            font = f;
            invalidate();
            if (isShowing()) {
                repaint();
            }
        }
    }


    /**
     * Invalidate the component if it inherits the font from the parent. This
     * method is overridden in Container.
     * 
     * @return true if the component was invalidated, false otherwise
     */
    boolean propagateFont() {
        if (font == null) {
            invalidate();
            return true;
        }
        return false;
    }

    public void setForeground(Color c) {
        Color oldFgColor;

        toolkit.lockAWT();
        try {
            oldFgColor = foreColor;
            foreColor = c;
        } finally {
            toolkit.unlockAWT();
        }
        
        // Update only if new color differs from the old one.
        // It is needed to avoid dead loops in repaint().
        if (!(oldFgColor == c ||
              (c != null && c.equals(oldFgColor)))) {
            firePropertyChange("foreground", oldFgColor, c); //$NON-NLS-1$
            repaint();
        }
    }

    public void setBackground(Color c) {
        Color oldBgColor;
        
        toolkit.lockAWT();
        try {
            oldBgColor = backColor;
            backColor = c;
        } finally {
            toolkit.unlockAWT();
        }
        
        // update only if new color differs from the old one
        // to avoid dead loop in repaint()
        if (!(c == oldBgColor || 
                 (c != null && c.equals(oldBgColor)))) {
            firePropertyChange("background", oldBgColor, c); //$NON-NLS-1$
            repaint();
        }
    }

    public void setIgnoreRepaint(boolean value) {
        toolkit.lockAWT();
        try {
            ignoreRepaint = value;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setLocale(Locale locale) {
        Locale oldLocale;
        toolkit.lockAWT();
        try {
            oldLocale = this.locale;
            this.locale = locale;
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("locale", oldLocale, locale); //$NON-NLS-1$
    }

    public void setLocation(Point p) {
        toolkit.lockAWT();
        try {
            setLocation(p.x, p.y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setLocation(int x, int y) {
        toolkit.lockAWT();
        try {
            move(x, y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setVisible(boolean b) {
        // show() & hide() are not deprecated for Window,
        // so have to call them from setVisible()
        show(b);
    }

    @Deprecated
    public void show() {
        toolkit.lockAWT();
        try {
            if (visible) {
                return;
            }
            prepare4HierarchyChange();
            mapToDisplay(true);
            validate();
            visible = true;
            behaviour.setVisible(true);
            postEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_SHOWN));
            finishHierarchyChange(this, parent, 0);
            notifyInputMethod(new Rectangle(x, y, w, h));
            invalidateRealParent();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public void show(boolean b) {
        if (b) {
            show();
        } else {
            hide();
        }
    }

    void transferFocus(int dir) {
        Container root = null;
        if (this instanceof Container) {
            Container cont = (Container) this;
            if (cont.isFocusCycleRoot()) {
                root = cont.getFocusTraversalRoot();
            }
        }
        if (root == null) {
            root = getFocusCycleRootAncestor();
        }
        // transfer focus up cycle if root is unreachable
        Component comp = this;
        while ((root != null)
                && !(root.isFocusCycleRoot() && root.isShowing() && root.isEnabled() && root
                        .isFocusable())) {
            comp = root;
            root = root.getFocusCycleRootAncestor();
        }
        if (root == null) {
            return;
        }
        FocusTraversalPolicy policy = root.getFocusTraversalPolicy();
        Component nextComp = null;
        switch (dir) {
            case KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS:
                nextComp = policy.getComponentAfter(root, comp);
                break;
            case KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS:
                nextComp = policy.getComponentBefore(root, comp);
                break;
        }
        if (nextComp != null) {
            nextComp.requestFocus(false);
        }
    }

    public void transferFocus() {
        toolkit.lockAWT();
        try {
            nextFocus();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void transferFocusBackward() {
        toolkit.lockAWT();
        try {
            transferFocus(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void transferFocusUpCycle() {
        toolkit.lockAWT();
        try {
            KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            Container root = kfm.getCurrentFocusCycleRoot();
            
            if(root == null) {
                return;
            }
            
            boolean success = false;
            Component nextComp = null;
            Container newRoot = root;
            do {
                nextComp = newRoot instanceof Window ? newRoot.getFocusTraversalPolicy()
                        .getDefaultComponent(newRoot) : newRoot;
                newRoot = newRoot.getFocusCycleRootAncestor();
                if (nextComp == null) {
                    break;
                }
                success = nextComp.requestFocusInWindow();
                if (newRoot == null) {
                    break;
                }
                kfm.setGlobalCurrentFocusCycleRoot(newRoot);
            } while (!success);
            if (!success && root != newRoot) {
                kfm.setGlobalCurrentFocusCycleRoot(root);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void validate() {
        toolkit.lockAWT();
        try {
            if (!behaviour.isDisplayable()) {
                return;
            }
            validateImpl();
        } finally {
            toolkit.unlockAWT();
        }
    }

    void validateImpl() {
        valid = true;
    }

    NativeWindow getNativeWindow() {
        return behaviour.getNativeWindow();
    }

    public boolean isMaximumSizeSet() {
        toolkit.lockAWT();
        try {
            return maximumSize != null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isMinimumSizeSet() {
        toolkit.lockAWT();
        try {
            return minimumSize != null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isPreferredSizeSet() {
        toolkit.lockAWT();
        try {
            return preferredSize != null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getMaximumSize() {
        toolkit.lockAWT();
        try {
            return isMaximumSizeSet() ? new Dimension(maximumSize) : new Dimension(
                    Short.MAX_VALUE, Short.MAX_VALUE);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getMinimumSize() {
        toolkit.lockAWT();
        try {
            return minimumSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Dimension minimumSize() {
        toolkit.lockAWT();
        try {
            if (isMinimumSizeSet()) {
                return (Dimension)minimumSize.clone();
            }
            Dimension defSize = getDefaultMinimumSize();
            if (defSize != null) {
                return (Dimension)defSize.clone();
            }
            return isDisplayable()? new Dimension(1, 1) : new Dimension(w, h);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension getPreferredSize() {
        toolkit.lockAWT();
        try {
            return preferredSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    public Dimension preferredSize() {
        toolkit.lockAWT();
        try {
            if (isPreferredSizeSet()) {
                return new Dimension(preferredSize);
            }
            Dimension defSize = getDefaultPreferredSize();
            if (defSize != null) {
                return new Dimension(defSize);
            }
            return new Dimension(getMinimumSize());
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setMaximumSize(Dimension maximumSize) {
        Dimension oldMaximumSize;
        toolkit.lockAWT();
        try {
            oldMaximumSize = this.maximumSize;
            if (oldMaximumSize != null) {
                oldMaximumSize = oldMaximumSize.getSize();
            }
            if (this.maximumSize == null) {
                if (maximumSize != null) {
                    this.maximumSize = new Dimension(maximumSize);
                }
            } else {
                if (maximumSize != null) {
                    this.maximumSize.setSize(maximumSize);
                } else {
                    this.maximumSize = null;
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("maximumSize", oldMaximumSize, this.maximumSize); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
            invalidateRealParent();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setMinimumSize(Dimension minimumSize) {
        Dimension oldMinimumSize;
        toolkit.lockAWT();
        try {
            oldMinimumSize = this.minimumSize;
            if (oldMinimumSize != null) {
                oldMinimumSize = oldMinimumSize.getSize();
            }
            if (this.minimumSize == null) {
                if (minimumSize != null) {
                    this.minimumSize = new Dimension(minimumSize);
                }
            } else {
                if (minimumSize != null) {
                    this.minimumSize.setSize(minimumSize);
                } else {
                    this.minimumSize = null;
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("minimumSize", oldMinimumSize, this.minimumSize); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
            invalidateRealParent();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setPreferredSize(Dimension preferredSize) {
        Dimension oldPreferredSize;
        toolkit.lockAWT();
        try {
            oldPreferredSize = this.preferredSize;
            if (oldPreferredSize != null) {
                oldPreferredSize = oldPreferredSize.getSize();
            }
            if (this.preferredSize == null) {
                if (preferredSize != null) {
                    this.preferredSize = new Dimension(preferredSize);
                }
            } else {
                if (preferredSize != null) {
                    this.preferredSize.setSize(preferredSize);
                } else {
                    this.preferredSize = null;
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("preferredSize", oldPreferredSize, this.preferredSize); //$NON-NLS-1$
        toolkit.lockAWT();
        try {
            invalidateRealParent();
        } finally {
            toolkit.unlockAWT();
        }
    }

    RedrawManager getRedrawManager() {
        if (parent == null) {
            return null;
        }
        return parent.getRedrawManager();
    }

    /**
     * @return true if component has a focusable peer
     */
    boolean isPeerFocusable() {
        // The recommendations for Windows and Unix are that
        // Canvases, Labels, Panels, Scrollbars, ScrollPanes, Windows,
        // and lightweight Components have non-focusable peers,
        // and all other Components have focusable peers.
        if (this instanceof Canvas || this instanceof Label || this instanceof Panel
                || this instanceof Scrollbar || this instanceof ScrollPane
                || this instanceof Window || isLightweight()) {
            return false;
        }
        return true;
    }

    /**
     * @return true if focusability was explicitly set via a call to
     *         setFocusable() or via overriding isFocusable() or
     *         isFocusTraversable()
     */
    boolean isFocusabilityExplicitlySet() {
        return calledSetFocusable || overridenIsFocusable;
    }

    public void paintAll(Graphics g) {
        toolkit.lockAWT();
        try {
            paint(g);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void update(Graphics g) {
        toolkit.lockAWT();
        try {
            if (!isLightweight() && !isPrepainter()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, w, h);
                g.setColor(getForeground());
            }
            paint(g);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void paint(Graphics g) {
        toolkit.lockAWT();
        try {
            // Just to nothing
        } finally {
            toolkit.unlockAWT();
        }
    }

    void prepaint(Graphics g) {
        // Just to nothing. For overriding.
    }

    boolean isPrepainter() {
        return false;
    }

    void prepare4HierarchyChange() {
        if (hierarchyChangingCounter++ == 0) {
            wasShowing = isShowing();
            wasDisplayable = isDisplayable();
            prepareChildren4HierarchyChange();
        }
    }

    void prepareChildren4HierarchyChange() {
        // To be inherited by Container
    }

    void finishHierarchyChange(Component changed, Container changedParent, int ancestorFlags) {
        if (--hierarchyChangingCounter == 0) {
            int changeFlags = ancestorFlags;
            if (wasShowing != isShowing()) {
                changeFlags |= HierarchyEvent.SHOWING_CHANGED;
            }
            if (wasDisplayable != isDisplayable()) {
                changeFlags |= HierarchyEvent.DISPLAYABILITY_CHANGED;
            }
            if (changeFlags > 0) {
                postEvent(new HierarchyEvent(this, HierarchyEvent.HIERARCHY_CHANGED, changed,
                        changedParent, changeFlags));
            }
            finishChildrenHierarchyChange(changed, changedParent, ancestorFlags);
        }
    }

    void finishChildrenHierarchyChange(Component changed, Container changedParent,
            int ancestorFlags) {
        // To be inherited by Container
    }

    void postHierarchyBoundsEvents(Component changed, int id) {
        postEvent(new HierarchyEvent(this, id, changed, null, 0));
    }

    void spreadHierarchyBoundsEvents(Component changed, int id) {
        // To be inherited by Container
    }

    public final void dispatchEvent(AWTEvent e) {
        if (e.isConsumed()) {
            return;
        }
        if (e instanceof PaintEvent) {
            toolkit.dispatchAWTEvent(e);
            processPaintEvent((PaintEvent) e);
            return;
        }
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (!e.dispatchedByKFM && kfm.dispatchEvent(e)) {
            return;
        }
        if (e instanceof KeyEvent) {
            KeyEvent ke = (KeyEvent) e;
            // consumes KeyEvent which represents a focus traversal key
            if (getFocusTraversalKeysEnabled()) {
                kfm.processKeyEvent(this, ke);
                if (ke.isConsumed()) {
                    return;
                }
            }
        }
        if (inputMethodsEnabled && dispatchToIM && e.isPosted && dispatchEventToIM(e)) {
            return;
        }
        if (e.getID() == WindowEvent.WINDOW_ICONIFIED) {
            notifyInputMethod(null);
        }
        AWTEvent.EventDescriptor descriptor = toolkit.eventTypeLookup.getEventDescriptor(e);
        toolkit.dispatchAWTEvent(e);
        if (descriptor != null) {
            if (isEventEnabled(descriptor.eventMask)
                    || (getListeners(descriptor.listenerType).length > 0)) {
                processEvent(e);
            }
            // input events can be consumed by user listeners:
            if (!e.isConsumed() && ((enabledAWTEvents & descriptor.eventMask) != 0)) {
                postprocessEvent(e, descriptor.eventMask);
            }
        }
        postDeprecatedEvent(e);
    }

    private void postDeprecatedEvent(AWTEvent e) {
        if (deprecatedEventHandler) {
            Event evt = e.getEvent();
            if (evt != null) {
                postEvent(evt);
            }
        }
    }

    void postprocessEvent(AWTEvent e, long eventMask) {
        toolkit.lockAWT();
        try {
            // call system listeners under AWT lock
            if (eventMask == AWTEvent.FOCUS_EVENT_MASK) {
                preprocessFocusEvent((FocusEvent) e);
            } else if (eventMask == AWTEvent.KEY_EVENT_MASK) {
                preprocessKeyEvent((KeyEvent) e);
            } else if (eventMask == AWTEvent.MOUSE_EVENT_MASK) {
                preprocessMouseEvent((MouseEvent) e);
            } else if (eventMask == AWTEvent.MOUSE_MOTION_EVENT_MASK) {
                preprocessMouseMotionEvent((MouseEvent) e);
            } else if (eventMask == AWTEvent.COMPONENT_EVENT_MASK) {
                preprocessComponentEvent((ComponentEvent) e);
            } else if (eventMask == AWTEvent.MOUSE_WHEEL_EVENT_MASK) {
                preprocessMouseWheelEvent((MouseWheelEvent) e);
            } else if (eventMask == AWTEvent.INPUT_METHOD_EVENT_MASK) {
                preprocessInputMethodEvent((InputMethodEvent) e);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void preprocessInputMethodEvent(InputMethodEvent e) {
        processInputMethodEventImpl(e, inputMethodListeners.getSystemListeners());
    }

    private void preprocessMouseWheelEvent(MouseWheelEvent e) {
        processMouseWheelEventImpl(e, mouseWheelListeners.getSystemListeners());
    }

    private void processMouseWheelEventImpl(MouseWheelEvent e, Collection<MouseWheelListener> c) {
        for (MouseWheelListener listener : c) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_WHEEL:
                    listener.mouseWheelMoved(e);
                    break;
            }
        }
    }

    private void preprocessComponentEvent(ComponentEvent e) {
        processComponentEventImpl(e, componentListeners.getSystemListeners());
    }

    void preprocessMouseMotionEvent(MouseEvent e) {
        processMouseMotionEventImpl(e, mouseMotionListeners.getSystemListeners());
    }

    void preprocessMouseEvent(MouseEvent e) {
        processMouseEventImpl(e, mouseListeners.getSystemListeners());
    }

    void preprocessKeyEvent(KeyEvent e) {
        processKeyEventImpl(e, keyListeners.getSystemListeners());
    }

    void preprocessFocusEvent(FocusEvent e) {
        processFocusEventImpl(e, focusListeners.getSystemListeners());
    }

    protected void processEvent(AWTEvent e) {
        long eventMask = toolkit.eventTypeLookup.getEventMask(e);
        if (eventMask == AWTEvent.COMPONENT_EVENT_MASK) {
            processComponentEvent((ComponentEvent) e);
        } else if (eventMask == AWTEvent.FOCUS_EVENT_MASK) {
            processFocusEvent((FocusEvent) e);
        } else if (eventMask == AWTEvent.KEY_EVENT_MASK) {
            processKeyEvent((KeyEvent) e);
        } else if (eventMask == AWTEvent.MOUSE_EVENT_MASK) {
            processMouseEvent((MouseEvent) e);
        } else if (eventMask == AWTEvent.MOUSE_WHEEL_EVENT_MASK) {
            processMouseWheelEvent((MouseWheelEvent) e);
        } else if (eventMask == AWTEvent.MOUSE_MOTION_EVENT_MASK) {
            processMouseMotionEvent((MouseEvent) e);
        } else if (eventMask == AWTEvent.HIERARCHY_EVENT_MASK) {
            processHierarchyEvent((HierarchyEvent) e);
        } else if (eventMask == AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK) {
            processHierarchyBoundsEvent((HierarchyEvent) e);
        } else if (eventMask == AWTEvent.INPUT_METHOD_EVENT_MASK) {
            processInputMethodEvent((InputMethodEvent) e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (ComponentListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getComponentListeners();
        } else if (FocusListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getFocusListeners();
        } else if (HierarchyBoundsListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getHierarchyBoundsListeners();
        } else if (HierarchyListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getHierarchyListeners();
        } else if (InputMethodListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getInputMethodListeners();
        } else if (KeyListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getKeyListeners();
        } else if (MouseWheelListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getMouseWheelListeners();
        } else if (MouseMotionListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getMouseMotionListeners();
        } else if (MouseListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getMouseListeners();
        } else if (PropertyChangeListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getPropertyChangeListeners();
        }
        return (T[]) Array.newInstance(listenerType, 0);
    }

    private void processPaintEvent(PaintEvent event) {
        if (redrawManager == null || getIgnoreRepaint()) {
            return;
        }
        Rectangle clipRect = event.getUpdateRect();
        if ((clipRect.width <= 0) || (clipRect.height <= 0)) {
            return;
        }
        Graphics g = getGraphics();
        if (g == null) {
            return;
        }

        initGraphics(g, event);

        if (event.getID() == PaintEvent.PAINT) {
            paint(g);
        } else {
            update(g);
        }
        ((CommonGraphics2D)g).flush();
        g.dispose();
    }

    void initGraphics(Graphics g, PaintEvent e) {
        Rectangle clip = e.getUpdateRect();
        if (clip instanceof ClipRegion) {
            g.setClip(((ClipRegion) clip).getClip());
        } else {
            g.setClip(clip);
        }
        if (isPrepainter()) {
            prepaint(g);
        } else if (!isLightweight() && (e.getID() == PaintEvent.PAINT)) {
            g.setColor(getBackground());
            g.fillRect(0, 0, w, h);
        }
        g.setFont(getFont());
        g.setColor(getForeground());
    }

    protected final void enableEvents(long eventsToEnable) {
        toolkit.lockAWT();
        try {
            enabledEvents |= eventsToEnable;
            deprecatedEventHandler = false;
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void enableAWTEvents(long eventsToEnable) {
        enabledAWTEvents |= eventsToEnable;
    }

    protected final void disableEvents(long eventsToDisable) {
        toolkit.lockAWT();
        try {
            enabledEvents &= ~eventsToDisable;
        } finally {
            toolkit.unlockAWT();
        }
    }

    /*
     * For use in MouseDispatcher only. Really it checks not only mouse events.
     */
    boolean isMouseEventEnabled(long eventMask) {
        return (isEventEnabled(eventMask) || (enabledAWTEvents & eventMask) != 0);
    }

    boolean isEventEnabled(long eventMask) {
        return ((enabledEvents & eventMask) != 0);
    }

    public void enableInputMethods(boolean enable) {
        toolkit.lockAWT();
        try {
            if (!enable) {
                removeNotifyInputContext();
            }
            inputMethodsEnabled = enable;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public ComponentListener[] getComponentListeners() {
        return componentListeners.getUserListeners(new ComponentListener[0]);
    }

    public void addComponentListener(ComponentListener l) {
        componentListeners.addUserListener(l);
    }

    public void removeComponentListener(ComponentListener l) {
        componentListeners.removeUserListener(l);
    }

    protected void processComponentEvent(ComponentEvent e) {
        processComponentEventImpl(e, componentListeners.getUserListeners());
    }

    private void processComponentEventImpl(ComponentEvent e, Collection<ComponentListener> c) {
        for (ComponentListener listener : c) {
            switch (e.getID()) {
                case ComponentEvent.COMPONENT_HIDDEN:
                    listener.componentHidden(e);
                    break;
                case ComponentEvent.COMPONENT_MOVED:
                    listener.componentMoved(e);
                    break;
                case ComponentEvent.COMPONENT_RESIZED:
                    listener.componentResized(e);
                    break;
                case ComponentEvent.COMPONENT_SHOWN:
                    listener.componentShown(e);
                    break;
            }
        }
    }

    public FocusListener[] getFocusListeners() {
        return focusListeners.getUserListeners(new FocusListener[0]);
    }

    public void addFocusListener(FocusListener l) {
        focusListeners.addUserListener(l);
    }

    void addAWTFocusListener(FocusListener l) {
        enableAWTEvents(AWTEvent.FOCUS_EVENT_MASK);
        focusListeners.addSystemListener(l);
    }

    public void removeFocusListener(FocusListener l) {
        focusListeners.removeUserListener(l);
    }

    protected void processFocusEvent(FocusEvent e) {
        processFocusEventImpl(e, focusListeners.getUserListeners());
    }

    private void processFocusEventImpl(FocusEvent e, Collection<FocusListener> c) {
        for (FocusListener listener : c) {
            switch (e.getID()) {
                case FocusEvent.FOCUS_GAINED:
                    listener.focusGained(e);
                    break;
                case FocusEvent.FOCUS_LOST:
                    listener.focusLost(e);
                    break;
            }
        }
    }

    public HierarchyListener[] getHierarchyListeners() {
        return hierarchyListeners.getUserListeners(new HierarchyListener[0]);
    }

    public void addHierarchyListener(HierarchyListener l) {
        hierarchyListeners.addUserListener(l);
    }

    public void removeHierarchyListener(HierarchyListener l) {
        hierarchyListeners.removeUserListener(l);
    }

    protected void processHierarchyEvent(HierarchyEvent e) {
        for (HierarchyListener listener : hierarchyListeners.getUserListeners()) {
            switch (e.getID()) {
                case HierarchyEvent.HIERARCHY_CHANGED:
                    listener.hierarchyChanged(e);
                    break;
            }
        }
    }

    public HierarchyBoundsListener[] getHierarchyBoundsListeners() {
        return hierarchyBoundsListeners.getUserListeners(new HierarchyBoundsListener[0]);
    }

    public void addHierarchyBoundsListener(HierarchyBoundsListener l) {
        hierarchyBoundsListeners.addUserListener(l);
    }

    public void removeHierarchyBoundsListener(HierarchyBoundsListener l) {
        hierarchyBoundsListeners.removeUserListener(l);
    }

    protected void processHierarchyBoundsEvent(HierarchyEvent e) {
        for (HierarchyBoundsListener listener : hierarchyBoundsListeners.getUserListeners()) {
            switch (e.getID()) {
                case HierarchyEvent.ANCESTOR_MOVED:
                    listener.ancestorMoved(e);
                    break;
                case HierarchyEvent.ANCESTOR_RESIZED:
                    listener.ancestorResized(e);
                    break;
            }
        }
    }

    public KeyListener[] getKeyListeners() {
        return keyListeners.getUserListeners(new KeyListener[0]);
    }

    public void addKeyListener(KeyListener l) {
        keyListeners.addUserListener(l);
    }

    void addAWTKeyListener(KeyListener l) {
        enableAWTEvents(AWTEvent.KEY_EVENT_MASK);
        keyListeners.addSystemListener(l);
    }

    public void removeKeyListener(KeyListener l) {
        keyListeners.removeUserListener(l);
    }

    protected void processKeyEvent(KeyEvent e) {
        processKeyEventImpl(e, keyListeners.getUserListeners());
    }

    private void processKeyEventImpl(KeyEvent e, Collection<KeyListener> c) {
        for (KeyListener listener : c) {
            switch (e.getID()) {
                case KeyEvent.KEY_PRESSED:
                    listener.keyPressed(e);
                    break;
                case KeyEvent.KEY_RELEASED:
                    listener.keyReleased(e);
                    break;
                case KeyEvent.KEY_TYPED:
                    listener.keyTyped(e);
                    break;
            }
        }
    }

    public MouseListener[] getMouseListeners() {
        return mouseListeners.getUserListeners(new MouseListener[0]);
    }

    public void addMouseListener(MouseListener l) {
        mouseListeners.addUserListener(l);
    }

    void addAWTMouseListener(MouseListener l) {
        enableAWTEvents(AWTEvent.MOUSE_EVENT_MASK);
        mouseListeners.addSystemListener(l);
    }

    void addAWTMouseMotionListener(MouseMotionListener l) {
        enableAWTEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
        mouseMotionListeners.addSystemListener(l);
    }

    void addAWTComponentListener(ComponentListener l) {
        enableAWTEvents(AWTEvent.COMPONENT_EVENT_MASK);
        componentListeners.addSystemListener(l);
    }

    void addAWTInputMethodListener(InputMethodListener l) {
        enableAWTEvents(AWTEvent.INPUT_METHOD_EVENT_MASK);
        inputMethodListeners.addSystemListener(l);
    }

    void addAWTMouseWheelListener(MouseWheelListener l) {
        enableAWTEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        mouseWheelListeners.addSystemListener(l);
    }

    public void removeMouseListener(MouseListener l) {
        mouseListeners.removeUserListener(l);
    }

    protected void processMouseEvent(MouseEvent e) {
        processMouseEventImpl(e, mouseListeners.getUserListeners());
    }

    private void processMouseEventImpl(MouseEvent e, Collection<MouseListener> c) {
        for (MouseListener listener : c) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_CLICKED:
                    listener.mouseClicked(e);
                    break;
                case MouseEvent.MOUSE_ENTERED:
                    listener.mouseEntered(e);
                    break;
                case MouseEvent.MOUSE_EXITED:
                    listener.mouseExited(e);
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    listener.mousePressed(e);
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    listener.mouseReleased(e);
                    break;
            }
        }
    }

    private void processMouseMotionEventImpl(MouseEvent e, Collection<MouseMotionListener> c) {
        for (MouseMotionListener listener : c) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_DRAGGED:
                    listener.mouseDragged(e);
                    break;
                case MouseEvent.MOUSE_MOVED:
                    listener.mouseMoved(e);
                    break;
            }
        }
    }

    public MouseMotionListener[] getMouseMotionListeners() {
        return mouseMotionListeners.getUserListeners(new MouseMotionListener[0]);
    }

    public void addMouseMotionListener(MouseMotionListener l) {
        mouseMotionListeners.addUserListener(l);
    }

    public void removeMouseMotionListener(MouseMotionListener l) {
        mouseMotionListeners.removeUserListener(l);
    }

    protected void processMouseMotionEvent(MouseEvent e) {
        processMouseMotionEventImpl(e, mouseMotionListeners.getUserListeners());
    }

    public MouseWheelListener[] getMouseWheelListeners() {
        return mouseWheelListeners.getUserListeners(new MouseWheelListener[0]);
    }

    public void addMouseWheelListener(MouseWheelListener l) {
        mouseWheelListeners.addUserListener(l);
    }

    public void removeMouseWheelListener(MouseWheelListener l) {
        mouseWheelListeners.removeUserListener(l);
    }

    protected void processMouseWheelEvent(MouseWheelEvent e) {
        processMouseWheelEventImpl(e, mouseWheelListeners.getUserListeners());
    }

    public InputMethodListener[] getInputMethodListeners() {
        return inputMethodListeners.getUserListeners(new InputMethodListener[0]);
    }

    public void addInputMethodListener(InputMethodListener l) {
        inputMethodListeners.addUserListener(l);
    }

    public void removeInputMethodListener(InputMethodListener l) {
        inputMethodListeners.removeUserListener(l);
    }

    protected void processInputMethodEvent(InputMethodEvent e) {
        processInputMethodEventImpl(e, inputMethodListeners.getUserListeners());
    }

    private void processInputMethodEventImpl(InputMethodEvent e,
            Collection<InputMethodListener> c) {
        for (InputMethodListener listener : c) {
            switch (e.getID()) {
                case InputMethodEvent.CARET_POSITION_CHANGED:
                    listener.caretPositionChanged(e);
                    break;
                case InputMethodEvent.INPUT_METHOD_TEXT_CHANGED:
                    listener.inputMethodTextChanged(e);
                    break;
            }
        }
    }

    public Point getMousePosition() throws HeadlessException {
        Point absPointerPos = MouseInfo.getPointerInfo().getLocation();
        Window winUnderPtr = toolkit.dispatcher.mouseDispatcher.findWindowAt(absPointerPos);
        Point pointerPos = MouseDispatcher.convertPoint(null, absPointerPos, winUnderPtr);
        boolean isUnderPointer = false;
        if (winUnderPtr == null) {
            return null;
        }
        isUnderPointer = winUnderPtr.isComponentAt(this, pointerPos);
        if (isUnderPointer) {
            return MouseDispatcher.convertPoint(null, absPointerPos, this);
        }
        return null;
    }

    /**
     * Set native caret at the given position <br>
     * Note: this method takes AWT lock inside because it walks through the
     * component hierarchy
     */
    void setCaretPos(final int x, final int y) {
        Runnable r = new Runnable() {
            public void run() {
                toolkit.lockAWT();
                try {
                    setCaretPosImpl(x, y);
                } finally {
                    toolkit.unlockAWT();
                }
            }
        };
        if (Thread.currentThread() instanceof EventDispatchThread) {
            r.run();
        } else {
            toolkit.getSystemEventQueueImpl().postEvent(new InvocationEvent(this, r));
        }
    }

    /**
     * This method should be called only at event dispatch thread
     */
    void setCaretPosImpl(int x, int y) {
        Component c = this;
        while ((c != null) && c.behaviour.isLightweight()) {
            x += c.x;
            y += c.y;
            c = c.getParent();
        }
        if (c == null) {
            return;
        }
        if (c instanceof Window) {
            Insets insets = c.getNativeInsets();
            x -= insets.left;
            y -= insets.top;
        }
        toolkit.getWindowFactory().setCaretPosition(x, y);
    }

    // to be overridden in standard components such as Button and List
    Dimension getDefaultMinimumSize() {
        return null;
    }

    // to be overridden in standard components such as Button and List
    Dimension getDefaultPreferredSize() {
        return null;
    }

    // to be overridden in standard components such as Button and List
    void resetDefaultSize() {
    }

    ComponentBehavior createBehavior() {
        return new LWBehavior(this);
    }

    Color getDefaultBackground() {
        return getWindowAncestor().getDefaultBackground();
    }

    Color getDefaultForeground() {
        return getWindowAncestor().getDefaultForeground();
    }

    /**
     * Called when native resource for this component is created (for
     * heavyweights only)
     */
    void nativeWindowCreated(NativeWindow win) {
        // to be overridden
    }

    /**
     * Determine the component's area hidden behind the windows that have higher
     * Z-order, including windows of other applications
     * 
     * @param part - the part of the component to determine its hidden area, or
     *        null for the whole component
     * @return the calculated region, or null if it cannot be determined
     */
    MultiRectArea getObscuredRegion(Rectangle part) {
        if (!visible || parent == null || !parent.visible) {
            return null;
        }
        Rectangle r = new Rectangle(0, 0, w, h);
        if (part != null) {
            r = r.intersection(part);
        }
        if (r.isEmpty()) {
            return null;
        }
        r.translate(x, y);
        MultiRectArea ret = parent.getObscuredRegion(r);
        if (ret != null) {
            parent.addObscuredRegions(ret, this);
            ret.translate(-x, -y);
            ret.intersect(new Rectangle(0, 0, w, h));
        }
        return ret;
    }

    private void readObject(ObjectInputStream stream) throws IOException,
            ClassNotFoundException {
        stream.defaultReadObject();
        FieldsAccessor accessor = new FieldsAccessor(Component.class, this);
        accessor.set("toolkit", Toolkit.getDefaultToolkit()); //$NON-NLS-1$
        accessor.set("behaviour", createBehavior()); //$NON-NLS-1$
        accessor.set("componentLock", new Object()); // $NON-LOCK-1$ //$NON-NLS-1$
    }

    final void onDrawImage(Image image, Point destLocation, Dimension destSize, Rectangle source) {
        ImageParameters imageParams;
        if (updatedImages == null) {
            updatedImages = new HashMap<Image, ImageParameters>();
        }
        imageParams = updatedImages.get(image);
        if (imageParams == null) {
            imageParams = new ImageParameters();
            updatedImages.put(image, imageParams);
        }
        imageParams.addDrawing(destLocation, destSize, source);
    }

    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        toolkit.lockAWT();
        try {
            boolean done = false;
            if ((infoflags & (ALLBITS | FRAMEBITS)) != 0) {
                done = true;
            } else if ((infoflags & SOMEBITS) != 0 && incrementalImageUpdate) {
                done = true;
            }
            if (done) {
                repaint();
            }
            return (infoflags & (ABORT | ALLBITS)) == 0;
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void invalidateRealParent() {
        Container realParent = getRealParent();
        if ((realParent != null) && realParent.isValid()) {
            realParent.invalidate();
        }
    }

    private class ImageParameters {
        private final LinkedList<DrawingParameters> drawingParams = new LinkedList<DrawingParameters>();

        Dimension size = new Dimension(Component.this.w, Component.this.h);

        void addDrawing(Point destLocation, Dimension destSize, Rectangle source) {
            drawingParams.add(new DrawingParameters(destLocation, destSize, source));
        }

        Iterator<DrawingParameters> drawingParametersIterator() {
            return drawingParams.iterator();
        }

        class DrawingParameters {
            Point destLocation;

            Dimension destSize;

            Rectangle source;

            DrawingParameters(Point destLocation, Dimension destSize, Rectangle source) {
                this.destLocation = new Point(destLocation);
                if (destSize != null) {
                    this.destSize = new Dimension(destSize);
                } else {
                    this.destSize = null;
                }
                if (source != null) {
                    this.source = new Rectangle(source);
                } else {
                    this.source = null;
                }
            }
        }
    }

    /**
     * TextComponent support
     */
    private TextKit textKit = null;

    TextKit getTextKit() {
        return textKit;
    }

    void setTextKit(TextKit kit) {
        textKit = kit;
    }

    /**
     * TextField support
     */
    private TextFieldKit textFieldKit = null;

    TextFieldKit getTextFieldKit() {
        return textFieldKit;
    }

    void setTextFieldKit(TextFieldKit kit) {
        textFieldKit = kit;
    }

    /**
     * Dispatches input & focus events to input method
     * context.
     * @param e event to pass to InputContext.dispatchEvent()
     * @return true if event was consumed by IM, false otherwise
     */
    private boolean dispatchEventToIM(AWTEvent e) {
        InputContext ic = getInputContext();
        if (ic == null) {
            return false;
        }
        int id = e.getID();
        boolean isInputEvent = ((id >= KeyEvent.KEY_FIRST) && (id <= KeyEvent.KEY_LAST))
                || ((id >= MouseEvent.MOUSE_FIRST) && (id <= MouseEvent.MOUSE_LAST));
        if (((id >= FocusEvent.FOCUS_FIRST) && (id <= FocusEvent.FOCUS_LAST)) || isInputEvent) {
            ic.dispatchEvent(e);
        }
        return e.isConsumed();
    }
}
