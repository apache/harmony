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

import java.applet.Applet;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleExtendedComponent;
import javax.accessibility.AccessibleKeyBinding;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.ComponentUI;
import org.apache.harmony.awt.ClipRegion;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.x.swing.internal.nls.Messages;
import org.apache.harmony.x.swing.StringConstants;
import org.apache.harmony.x.swing.Utilities;

public abstract class JComponent extends Container implements Serializable {
    private class AncestorComponentNotifier extends HierarchyBoundsAdapter implements
            HierarchyListener, ComponentListener {
        public void componentMoved(ComponentEvent e) {
            final Container ancestor = (Container) e.getComponent();
            fireAncestorEvent(AncestorEvent.ANCESTOR_MOVED, ancestor, ancestor.getParent());
        }

        @Override
        public void ancestorMoved(HierarchyEvent e) {
            if (!isVisible()) {
                return;
            }
            final Container ancestor = (Container) e.getChanged();
            fireAncestorEvent(AncestorEvent.ANCESTOR_MOVED, ancestor, ancestor.getParent());
        }

        public void hierarchyChanged(HierarchyEvent e) {
            if (((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
                    || ((e.getChangeFlags() & HierarchyEvent.HIERARCHY_CHANGED) != 0)) {
                fireAncestorEvent(isShowing() ? AncestorEvent.ANCESTOR_ADDED
                        : AncestorEvent.ANCESTOR_REMOVED, (Container) e.getChanged(), e
                        .getChangedParent());
            }
        }

        public void componentResized(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }

        private void fireAncestorEvent(int eventID, Container ancestor, Container ancestorParent) {
            final AncestorListener[] listeners = getAncestorListeners();
            AncestorEvent ancestorEvent = new AncestorEvent(JComponent.this, eventID, ancestor,
                    ancestorParent);
            for (int i = 0; i < listeners.length; i++) {
                if (eventID == AncestorEvent.ANCESTOR_ADDED) {
                    listeners[i].ancestorAdded(ancestorEvent);
                } else if (eventID == AncestorEvent.ANCESTOR_REMOVED) {
                    listeners[i].ancestorRemoved(ancestorEvent);
                } else if (eventID == AncestorEvent.ANCESTOR_MOVED) {
                    listeners[i].ancestorMoved(ancestorEvent);
                }
            }
        }
    };

    public abstract class AccessibleJComponent extends Container.AccessibleAWTContainer
            implements AccessibleExtendedComponent {
        protected class AccessibleContainerHandler implements ContainerListener {
            public void componentAdded(ContainerEvent event) {
                firePropertyChange(ACCESSIBLE_CHILD_PROPERTY, null, event.getChild()
                        .getAccessibleContext());
            }

            public void componentRemoved(ContainerEvent event) {
                firePropertyChange(ACCESSIBLE_CHILD_PROPERTY, event.getChild()
                        .getAccessibleContext(), null);
            }
        }

        protected class AccessibleFocusHandler implements FocusListener {
            public void focusGained(FocusEvent event) {
                firePropertyChange(ACCESSIBLE_STATE_PROPERTY, null, AccessibleState.FOCUSED);
            }

            public void focusLost(FocusEvent event) {
                firePropertyChange(ACCESSIBLE_STATE_PROPERTY, AccessibleState.FOCUSED, null);
            }
        }

        protected ContainerListener accessibleContainerHandler;

        protected FocusListener accessibleFocusHandler;

        protected AccessibleJComponent() {
        }

        protected String getBorderTitle(Border border) {
            if (border instanceof TitledBorder) {
                return ((TitledBorder) border).getTitle();
            } else if (border instanceof CompoundBorder) {
                CompoundBorder compoundBorder = (CompoundBorder) border;
                String title = getBorderTitle(compoundBorder.getInsideBorder());
                return (title != null) ? title : getBorderTitle(compoundBorder
                        .getOutsideBorder());
            }
            return null;
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet stateSet = super.getAccessibleStateSet();
            if (isShowing()) {
                stateSet.add(AccessibleState.SHOWING);
            }
            return stateSet;
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SWING_COMPONENT;
        }

        public AccessibleKeyBinding getAccessibleKeyBinding() {
            return null;
        }

        @Override
        public Accessible getAccessibleChild(int childIndex) {
            return super.getAccessibleChild(childIndex);
        }

        public String getToolTipText() {
            return null;
        }

        public String getTitledBorderText() {
            Border border = getBorder();
            return (border instanceof TitledBorder) ? ((TitledBorder) border).getTitle() : null;
        }

        @Override
        public String getAccessibleName() {
            return super.getAccessibleName();
        }

        @Override
        public String getAccessibleDescription() {
            String description = super.getAccessibleDescription();
            return (description == null) ? getToolTipText() : description;
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            super.removePropertyChangeListener(listener);
        }

        final AccessibleExtendedComponent getAccessibleExtendedComponent() {
            return this;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            super.addPropertyChangeListener(listener);
            if (accessibleContainerHandler == null) {
                accessibleContainerHandler = new AccessibleContainerHandler();
                addContainerListener(accessibleContainerHandler);
            }
            if (accessibleFocusHandler == null) {
                accessibleFocusHandler = new AccessibleFocusHandler();
                addFocusListener(accessibleFocusHandler);
            }
        }

        @Override
        public int getAccessibleChildrenCount() {
            return super.getAccessibleChildrenCount();
        }
    }

    private static final int FIRST_INPUT_MAP_INDEX = 0;

    public static final int WHEN_FOCUSED = 0;

    public static final int WHEN_ANCESTOR_OF_FOCUSED_COMPONENT = 1;

    public static final int WHEN_IN_FOCUSED_WINDOW = 2;

    private static final int LAST_INPUT_MAP_INDEX = 2;

    public static final int UNDEFINED_CONDITION = -1;

    public static final String TOOL_TIP_TEXT_KEY = "ToolTipText";

    static final String INHERITS_POPUP_MENU_PROPERTY_NAME = "inheritsPopupMenu";

    static final String COMPONENT_POPUP_MENU_PROPERTY_NAME = "componentPopupMenu";

    static final String UI_PROPERTY_NAME = "UI";

    static final String AUTOSCROLLS_PROPERTY_NAME = "autoscrolls";

    static final String VERIFY_INPUT_PROPERTY_NAME = "verifyInputWhenFocusTarget";

    static final String NEXT_FOCUSABLE_PROPERTY_NAME = "nextFocus";

    private static final String INPUT_VERIFIER_PROPERTY_NAME = "inputVerifier";

    protected AccessibleContext accessibleContext;

    private final static String UI_CLASS_ID = "ComponentUI";

    private static Locale defaultLocale;

    protected transient ComponentUI ui;

    private Float alignmentX;

    private Float alignmentY;

    private Border border;

    private int debugGraphicsOptions;

    private final HashMap<Object, Object> clientProperties = new HashMap<Object, Object>();

    private TransferHandler transferHandler;

    private ActionMap actionMap;

    private boolean actionMapCreated;

    private final InputMap[] inputMaps = new InputMap[3];

    private final boolean[] inputMapsCreated = new boolean[3];

    private transient AncestorComponentNotifier ancestorComponentNotifier;

    private final VetoableChangeSupport vetoableChangeSupport = new VetoableChangeSupport(this);;

    protected EventListenerList listenerList = new EventListenerList();

    private boolean autoscrolls;

    private Component nextFocusableComponent;

    private boolean verifyInputWhenFocusTarget = true;

    private InputVerifier inputVerifier;

    private boolean requestFocusEnabled = true;

    private boolean inheritsPopupMenu;

    private JPopupMenu componentPopupMenu;

    private static final String ILLEGAL_CONDITION_MESSAGE = 
            Messages.getString("swing.B3","JComponent.WHEN_IN_FOCUSED_WINDOW, JComponent.WHEN_FOCUSED ", //$NON-NLS-1$ //$NON-NLS-2$
                    "JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT"); //$NON-NLS-1$ 
    
    private static final String NULL_MAP_MESSAGE =
            Messages.getString("swing.03", "map"); //$NON-NLS-1$ //$NON-NLS-2$
    
    private boolean opaque;

    private boolean doubleBuffered;

    private boolean doubleBufferingRoot;

    private static final Rectangle auxRectangle = new Rectangle();

    final Set<String> installablePropertiesExcluded = new HashSet<String>();

    private static JToolTip toolTip;
    static {
        setDefaultLocale(Locale.getDefault());
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(
                new KeyEventPostProcessor() {
                    public boolean postProcessKeyEvent(KeyEvent event) {
                        if (event.isConsumed()) {
                            return false;
                        }
                        final Component source = event.getComponent();
                        Container parent = SwingUtilities.getRootPane(source);
                        while (parent != null) {
                            if (SwingUtilities.processKeyEventOnComponent(parent, event)) {
                                return true;
                            }
                            if (SwingUtilities.processKeyEventOnChildren(parent, event)) {
                                return true;
                            }
                            parent = SwingUtilities.getAncestorOfClass(JRootPane.class, parent);
                        }
                        return false;
                    }
                });
    }

    public JComponent() {
        setLocale(getDefaultLocale());
        enableEvents(AWTEvent.KEY_EVENT_MASK);
    }

    @Override
    public Rectangle getBounds(Rectangle bounds) {
        if (bounds == null) {
            return getBounds();
        }
        bounds.setBounds(getX(), getY(), getWidth(), getHeight());
        return bounds;
    }

    @Override
    public Point getLocation(Point extLocation) {
        if (extLocation == null) {
            return getLocation();
        }
        extLocation.x = getX();
        extLocation.y = getY();
        return extLocation;
    }

    @Override
    public Insets getInsets() {
        return getInsets(null);
    }

    public Insets getInsets(Insets insets) {
        Border border = getBorder();
        if (border instanceof AbstractBorder && insets != null) {
            return ((AbstractBorder) border).getBorderInsets(this, insets);
        }
        Insets originalInsets;
        if (border == null) {
            originalInsets = super.getInsets();
        } else {
            originalInsets = border.getBorderInsets(this);
        }
        if (insets != null) {
            insets.set(originalInsets.top, originalInsets.left, originalInsets.bottom,
                    originalInsets.right);
            return insets;
        }
        return originalInsets;
    }

    @Override
    public Dimension getSize(Dimension extSize) {
        if (extSize == null) {
            return getSize();
        }
        extSize.setSize(getWidth(), getHeight());
        return extSize;
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        if (ui != null) {
            Dimension size = ui.getMinimumSize(this);
            if (size != null) {
                return new Dimension(size);
            }
        }
        LayoutManager layout = getLayout();
        if (layout != null) {
            return layout.minimumLayoutSize(this);
        }
        return new Dimension();
    }

    @Override
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        if (ui != null) {
            Dimension size = ui.getMaximumSize(this);
            if (size != null) {
                return new Dimension(size);
            }
        }
        LayoutManager layout = getLayout();
        if (layout != null && layout instanceof LayoutManager2) {
            return ((LayoutManager2) layout).maximumLayoutSize(this);
        }
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        if (ui != null) {
            Dimension size = ui.getPreferredSize(this);
            if (size != null) {
                return new Dimension(size);
            }
        }
        if (getLayout() != null) {
            return getLayout().preferredLayoutSize(this);
        }
        return new Dimension();
    }

    @Override
    public boolean contains(int x, int y) {
        return (ui != null) ? ui.contains(this, x, y) : super.contains(x, y);
    }

    public final void putClientProperty(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }

        Object oldValue = (value != null) ? clientProperties.put(key, value) : clientProperties
                .remove(key);
        if (oldValue != value) {
            firePropertyChange((key != null) ? key.toString() : null, oldValue, value);
        }
    }

    public final Object getClientProperty(Object key) {
        return clientProperties.get(key);
    }

    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void fireVetoableChange(String property, Object oldValue, Object newValue)
            throws PropertyVetoException {
        vetoableChangeSupport.fireVetoableChange(property, oldValue, newValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenersClass) {
        T[] result = super.getListeners(listenersClass);
        if (!Utilities.isEmptyArray(result)) {
            return result;
        }
        result = listenerList.getListeners(listenersClass);
        if (!Utilities.isEmptyArray(result)) {
            return result;
        }
        if (VetoableChangeListener.class.isAssignableFrom(listenersClass)) {
            result = (T[]) getVetoableChangeListeners();
        }
        return result;
    }

    public void removeAncestorListener(AncestorListener ancestorListener) {
        listenerList.remove(AncestorListener.class, ancestorListener);
        removeAncestorComponentNotifier();
    }

    public void addAncestorListener(AncestorListener ancestorListener) {
        addAncestorComponentNotifier();
        listenerList.add(AncestorListener.class, ancestorListener);
    }

    public AncestorListener[] getAncestorListeners() {
        return getListeners(AncestorListener.class);
    }

    public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
        vetoableChangeSupport.removeVetoableChangeListener(listener);
    }

    public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
        vetoableChangeSupport.addVetoableChangeListener(listener);
    }

    public synchronized VetoableChangeListener[] getVetoableChangeListeners() {
        return vetoableChangeSupport.getVetoableChangeListeners();
    }

    public void registerKeyboardAction(ActionListener action, String command,
            KeyStroke keyStroke, int condition) {
        String commandKey = (command != null) ? command : ((action != null) ? action.toString()
                : "");
        getInputMap(condition, true).put(keyStroke, commandKey);
        Action actionProxy = action instanceof Action ? (Action) action : new ActionProxy(
                command, action);
        getActionMap(true).put(commandKey, actionProxy);
    }

    public void registerKeyboardAction(ActionListener action, KeyStroke keyStroke, int condition) {
        registerKeyboardAction(action, null, keyStroke, condition);
    }

    public void unregisterKeyboardAction(KeyStroke keyStroke) {
        for (int i = FIRST_INPUT_MAP_INDEX; i <= LAST_INPUT_MAP_INDEX; i++) {
            if (inputMaps[i] != null) {
                Object command = inputMaps[i].get(keyStroke);
                inputMaps[i].remove(keyStroke);
                if (actionMap != null) {
                    actionMap.remove(command);
                }
            }
        }
    }

    public int getConditionForKeyStroke(KeyStroke keyStroke) {
        for (int i = FIRST_INPUT_MAP_INDEX; i <= LAST_INPUT_MAP_INDEX; i++) {
            if (inputMaps[i] != null && inputMaps[i].get(keyStroke) != null) {
                return i;
            }
        }
        return UNDEFINED_CONDITION;
    }

    public ActionListener getActionForKeyStroke(KeyStroke keyStroke) {
        Object command = null;
        for (int i = FIRST_INPUT_MAP_INDEX; i <= LAST_INPUT_MAP_INDEX && (command == null); i++) {
            if (inputMaps[i] != null) {
                command = inputMaps[i].get(keyStroke);
            }
        }
        ActionListener action = (command != null && actionMap != null) ? actionMap.get(command)
                : null;
        return (action instanceof ActionProxy) ? (ActionListener) ((ActionProxy) action)
                .getValue(Action.NAME) : action;
    }

    protected boolean processKeyBinding(KeyStroke keyStroke, KeyEvent keyEvent, int condition,
            boolean pressed) {
        InputMap inputMap = getInputMap(condition, false);
        if (inputMap == null || actionMap == null || !isEnabled()) {
            return false;
        }
        Object command = inputMap.get(keyStroke);
        if (command == null) {
            return false;
        }
        Action action = actionMap.get(command);
        return SwingUtilities.notifyAction(action, keyStroke, keyEvent, this, keyEvent
                .getModifiersEx());
    }

    public KeyStroke[] getRegisteredKeyStrokes() {
        ArrayList<KeyStroke> allKeys = new ArrayList<KeyStroke>();
        for (int i = FIRST_INPUT_MAP_INDEX; i <= LAST_INPUT_MAP_INDEX; i++) {
            if (inputMaps[i] != null) {
                KeyStroke[] keys = inputMaps[i].allKeys();
                if (keys != null) {
                    allKeys.addAll(Arrays.asList(keys));
                }
            }
        }
        return allKeys.toArray(new KeyStroke[allKeys.size()]);
    }

    public void resetKeyboardActions() {
        for (int i = FIRST_INPUT_MAP_INDEX; i <= LAST_INPUT_MAP_INDEX; i++) {
            if (inputMaps[i] != null) {
                inputMaps[i].clear();
            }
        }
        if (actionMap != null) {
            actionMap.clear();
        }
    }

    public void setTransferHandler(TransferHandler newHandler) {
        TransferHandler oldHandler = transferHandler;
        transferHandler = newHandler;
        if (!Boolean.valueOf(System.getProperty("suppressSwingDropSupport")).booleanValue()) {
            new DropTarget(this, null);
        }
        firePropertyChange(StringConstants.TRANSFER_HANDLER_PROPERTY_NAME, oldHandler,
                newHandler);
    }

    public TransferHandler getTransferHandler() {
        return transferHandler;
    }

    public Container getTopLevelAncestor() {
        for (Container parent = getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof Window || parent instanceof Applet) {
                return parent;
            }
        }
        return null;
    }

    public JRootPane getRootPane() {
        return (JRootPane) SwingUtilities.getAncestorOfClass(JRootPane.class, this);
    }

    public void setInputVerifier(InputVerifier verifier) {
        InputVerifier oldValue = inputVerifier;
        inputVerifier = verifier;
        firePropertyChange(INPUT_VERIFIER_PROPERTY_NAME, oldValue, inputVerifier);
    }

    public InputVerifier getInputVerifier() {
        return inputVerifier;
    }

    public final void setInputMap(int condition, InputMap map) {
        if (condition == WHEN_IN_FOCUSED_WINDOW && !(map instanceof ComponentInputMap)
                && map != null) {
            throw new IllegalArgumentException(Messages.getString("swing.0D","WHEN_IN_FOCUSED_WINDOW")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (condition < FIRST_INPUT_MAP_INDEX || condition > LAST_INPUT_MAP_INDEX) {
            throw new IllegalArgumentException(ILLEGAL_CONDITION_MESSAGE);
        }
        if (condition == WHEN_IN_FOCUSED_WINDOW && map == null) {
                	throw new IllegalArgumentException(NULL_MAP_MESSAGE);
        }
        inputMaps[condition] = map;
        inputMapsCreated[condition] = true;
    }

    public final InputMap getInputMap(int condition) {
        return getInputMap(condition, true);
    }

    public final InputMap getInputMap() {
        return getInputMap(WHEN_FOCUSED, true);
    }

    final InputMap getInputMap(int condition, boolean forceCreate) {
        if (condition < FIRST_INPUT_MAP_INDEX || condition > LAST_INPUT_MAP_INDEX) {
            throw new IllegalArgumentException(ILLEGAL_CONDITION_MESSAGE);
        }
        if (!forceCreate || inputMapsCreated[condition]) {
            return inputMaps[condition];
        }
        InputMap result = (condition != WHEN_IN_FOCUSED_WINDOW) ? new InputMap()
                : new ComponentInputMap(this);
        inputMaps[condition] = result;
        inputMapsCreated[condition] = true;
        return result;
    }

    public final void setActionMap(ActionMap newActionMap) {
        actionMapCreated = true;
        actionMap = newActionMap;
    }

    public final ActionMap getActionMap() {
        return getActionMap(true);
    }

    final ActionMap getActionMap(boolean forceCreate) {
        if (forceCreate && !actionMapCreated) {
            actionMapCreated = true;
            actionMap = new ActionMap();
        }
        return actionMap;
    }

    public Border getBorder() {
        return border;
    }

    public void setBorder(Border newBorder) {
        if (newBorder != border) {
            Border oldBorder = border;
            border = newBorder;
            firePropertyChange(StringConstants.BORDER_PROPERTY_CHANGED, oldBorder, newBorder);
            if (oldBorder == null || newBorder == null
                    || !oldBorder.getBorderInsets(this).equals(newBorder.getBorderInsets(this))) {
                revalidate();
            }
            repaint();
        }
    }

    public void setToolTipText(String text) {
        putClientProperty(TOOL_TIP_TEXT_KEY, text);
        if (text != null) {
            ToolTipManager.sharedInstance().registerComponent(this);
        } else {
            ToolTipManager.sharedInstance().unregisterComponent(this);
        }
    }

    public String getToolTipText() {
        return (String) getClientProperty(TOOL_TIP_TEXT_KEY);
    }

    public String getToolTipText(MouseEvent event) {
        return getToolTipText();
    }

    public JToolTip createToolTip() {
        if (toolTip == null) {
            toolTip = new JToolTip();
        }
        toolTip.setComponent(this);
        return toolTip;
    }

    public Point getToolTipLocation(MouseEvent event) {
        return null;
    }

    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    @Override
    protected void processKeyEvent(KeyEvent event) {
        super.processKeyEvent(event);
        if (!event.isConsumed()) {
            processComponentKeyEvent(event);
        }
        if (!event.isConsumed() && processKeyBindings(event, this)) {
            event.consume();
        }
    }

    static boolean processKeyBindings(KeyEvent event, Component source) {
        boolean pressed = (event.getID() == KeyEvent.KEY_PRESSED);
        final KeyStroke ks = KeyStroke.getKeyStrokeForEvent(event);
        Component component = source;
        if (component instanceof JComponent) {
            if (((JComponent) component).processKeyBinding(ks, event, JComponent.WHEN_FOCUSED,
                    pressed)) {
                return true;
            }
        }
        while (component != null) {
            if (component instanceof JComponent) {
                if (((JComponent) component).processKeyBinding(ks, event,
                        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, pressed)) {
                    return true;
                }
            }
            component = component.getParent();
        }
        component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (component == null) {
            component = source;
        }
        Window w = SwingUtilities.getWindowAncestor(component);
        if (w != null) {
            if (SwingUtilities.processKeyEventOnChildren(w, event)) {
                event.consume();
                return true;
            }
        }
        return false;
    }

    final void componentInputMapChanged(ComponentInputMap map) {
    }

    protected void processComponentKeyEvent(KeyEvent event) {
    }

    @Override
    public void addNotify() {
        super.addNotify();
        firePropertyChange(StringConstants.ANCESTOR_PROPERTY_NAME, null, getParent());
    }

    @Override
    public void removeNotify() {
        firePropertyChange(StringConstants.ANCESTOR_PROPERTY_NAME, getParent(), null);
        super.removeNotify();
    }

    public void scrollRectToVisible(Rectangle rect) {
        Container parent = SwingUtilities.getAncestorOfClass(JComponent.class, this);
        if (parent == null) {
            return;
        }
        Rectangle translatedRect = SwingUtilities.convertRectangle(this, rect, parent);
        ((JComponent) parent).scrollRectToVisible(translatedRect);
    }

    public void computeVisibleRect(Rectangle rect) {
        rect.setBounds(0, 0, getWidth(), getHeight());
        getComponentVisibleRect(this, rect);
    }

    public Rectangle getVisibleRect() {
        return getComponentVisibleRect(this, new Rectangle(getWidth(), getHeight()));
    }

    @Override
    public void update(Graphics graphics) {
        paint(graphics);
    }

    protected void printComponent(Graphics g) {
        paintComponent(g);
    }

    protected void printChildren(Graphics g) {
        paintChildren(g);
    }

    protected void printBorder(Graphics g) {
        paintBorder(g);
    }

    @Override
    public void printAll(Graphics g) {
        print(g);
    }

    @Override
    public void print(Graphics g) {
        setDoubleBuffered(false);
        printComponent(g);
        printBorder(g);
        printChildren(g);
        setDoubleBuffered(true);
    }

    protected void paintComponent(Graphics graphics) {
        if (ui != null) {
            Graphics uiGraphics = graphics.create();
            ui.update(uiGraphics, this);
            uiGraphics.dispose();
        }
    }

    protected void paintChildren(Graphics graphics) {
        Rectangle clipBounds = graphics.getClipBounds();
        if (clipBounds != null && clipBounds.isEmpty()) {
            return;
        }
        int cc = getComponentCount();
        if (!isOptimizedDrawingEnabled()) {
            MultiRectArea childrenCoverage = null;
            Component compList[] = new Component[cc];
            Shape clipList[] = new Shape[cc];
            int rc = -1;
            for (int i = 0; i < cc; i++) {
                Component comp = getComponent(i);
                if (comp.isVisible()) {
                    if (!comp.isLightweight()) {
                        continue;
                    }
                    MultiRectArea clip;
                    Rectangle bounds = comp.getBounds();
                    if (childrenCoverage == null && comp.isOpaque()) {
                        childrenCoverage = new MultiRectArea(bounds);
                        clip = new MultiRectArea(bounds);
                    } else {
                        clip = new MultiRectArea(bounds);
                        clip.substract(childrenCoverage);
                        if (clip.isEmpty()) {
                            continue;
                        }
                        if (comp.isOpaque()) {
                            childrenCoverage = MultiRectArea.union(childrenCoverage, clip);
                        }
                    }
                    rc++;
                    if (clipBounds != null) {
                        clip.intersect(clipBounds);
                    }
                    clip.translate(-bounds.x, -bounds.y);
                    compList[rc] = comp;
                    clipList[rc] = clip;
                }
            }
            while (rc >= 0) {
                Component comp = compList[rc];
                Graphics gComp = getChildJComponentGraphics(graphics, comp);
                if (gComp != null) {
                    ((Graphics2D) gComp).clip(clipList[rc]);
                    if (!gComp.getClipBounds().isEmpty()) {
                        comp.paint(gComp);
                    }
                    gComp.dispose();
                }
                rc--;
            }
        } else {
            for (int i = cc - 1; i >= 0; i--) {
                Component comp = getComponent(i);
                if (comp.isVisible()) {
                    if (!comp.isLightweight()) {
                        continue;
                    }
                    Graphics gComp = getChildJComponentGraphics(graphics, comp);
                    if (gComp != null) {
                        Rectangle bounds = comp.getBounds();
                        int x = bounds.x;
                        int y = bounds.y;
                        if (clipBounds != null) {
                            bounds = bounds.intersection(clipBounds);
                        }
                        bounds.translate(-x, -y);
                        gComp.clipRect(bounds.x, bounds.y, bounds.width, bounds.height);
                        if (!gComp.getClipBounds().isEmpty()) {
                            comp.paint(gComp);
                        }
                        gComp.dispose();
                    }
                }
            }
        }
    }

    protected void paintBorder(Graphics graphics) {
        if (border != null) {
            border.paintBorder(this, graphics, 0, 0, getWidth(), getHeight());
        }
    }

    @Override
    public void paint(Graphics graphics) {
        if (RepaintManager.currentManager(this).isDoubleBufferingEnabled()
                && isDoubleBuffered() && isOpaque() && !insideDoubleBuffering()) {
            paintDoubleBuffered(graphics);
        } else {
            paintComponent(graphics);
            paintBorder(graphics);
            paintChildren(graphics);
        }
    }

    public void repaint(Rectangle rect) {
        RepaintManager.currentManager(this).addDirtyRegion(this, rect.x, rect.y, rect.width,
                rect.height);
    }

    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
        RepaintManager.currentManager(this).addDirtyRegion(this, x, y, width, height);
    }

    public void paintImmediately(Rectangle rect) {
        if (rect == null) {
            throw new NullPointerException();
        }
 
        if (!isShowing()) {
            return;
        }
        Component effectiveRoot = Utilities.getDrawingRoot(this, rect);
        while (effectiveRoot != null && !(effectiveRoot instanceof Window)
                && !effectiveRoot.isOpaque()) {
            effectiveRoot = effectiveRoot.getParent();
        }
        if (effectiveRoot == null) {
            return;
        }
        Graphics g = effectiveRoot.getGraphics();
        if (g == null) {
            return;
        }
        Rectangle visibleRect = getComponentVisibleRect(effectiveRoot, new Rectangle(
                effectiveRoot.getWidth(), effectiveRoot.getHeight()));
        if (rect instanceof ClipRegion) {
            ClipRegion repaintRegion = (ClipRegion) rect;
            repaintRegion.convertRegion(this, effectiveRoot);
            repaintRegion.intersect(visibleRect);
            if (repaintRegion.isEmpty()) {
                g.dispose();
                return;
            }
            if (g instanceof Graphics2D) {
                ((Graphics2D) g).clip(repaintRegion.getClip());
            } else {
                g.setClip(repaintRegion.getClip());
            }
        } else {
            Rectangle repaintRect = SwingUtilities.convertRectangle(this, rect, effectiveRoot);
            repaintRect = SwingUtilities.computeIntersection(repaintRect.x, repaintRect.y,
                    repaintRect.width, repaintRect.height, visibleRect);
            if (repaintRect.isEmpty()) {
                g.dispose();
                return;
            }
            g.clipRect(rect.x, rect.y, rect.width, rect.height);
        }
        if (RepaintManager.currentManager(this).isDoubleBufferingEnabled()
                && effectiveRoot instanceof JComponent
                && isDoubleBufferingEnabled(effectiveRoot)) {
            ((JComponent) effectiveRoot).paintDoubleBuffered(g);
        } else {
            effectiveRoot.paint(g);
        }
        g.dispose();
    }

    public void paintImmediately(int x, int y, int width, int height) {
        paintImmediately(new Rectangle(x, y, width, height));
    }

    @Override
    public Graphics getGraphics() {
        Graphics result = super.getGraphics();
        if (result != null) {
            result = getComponentGraphics(result);
        }
        return result;
    }

    protected Graphics getComponentGraphics(Graphics g) {
        Graphics result = null;
        if (debugGraphicsOptions == 0) {
            result = g;
        } else {
            result = new DebugGraphics(g);
            ((DebugGraphics) result).setDebugOptions(debugGraphicsOptions);
        }
        result.setFont(getFont());
        result.setColor(getForeground());
        if (result instanceof Graphics2D) { 		// DebugGraphics class does not have the setBackground method
        	((Graphics2D) result).setBackground(getBackground());
        }
                
        return result;
    }

    @Deprecated
    public void setNextFocusableComponent(Component component) {
        Component oldComponent = nextFocusableComponent;
        nextFocusableComponent = component;
        firePropertyChange(NEXT_FOCUSABLE_PROPERTY_NAME, oldComponent, nextFocusableComponent);
        Container focusCycleRoot = getFocusCycleRootAncestor();
        if (focusCycleRoot == null) {
            return;
        }
        FocusTraversalPolicy newFocusTraversalPolicy = new BequestedFocusTraversalPolicy(
                focusCycleRoot.getFocusTraversalPolicy(), this, nextFocusableComponent);
        focusCycleRoot.setFocusTraversalPolicy(newFocusTraversalPolicy);
    }

    @Deprecated
    public Component getNextFocusableComponent() {
        return nextFocusableComponent;
    }

    public void setVerifyInputWhenFocusTarget(boolean verifyInput) {
        boolean oldValue = verifyInputWhenFocusTarget;
        verifyInputWhenFocusTarget = verifyInput;
        firePropertyChange(VERIFY_INPUT_PROPERTY_NAME, oldValue, verifyInputWhenFocusTarget);
    }

    public boolean getVerifyInputWhenFocusTarget() {
        return verifyInputWhenFocusTarget;
    }

    public void setRequestFocusEnabled(boolean enabled) {
        requestFocusEnabled = enabled;
    }

    public boolean isRequestFocusEnabled() {
        return requestFocusEnabled;
    }

    private boolean checkRequestedFocus() {
        if (!getVerifyInputWhenFocusTarget()) {
            return true;
        }
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getFocusOwner();
        if (focusOwner == this) {
            return true;
        }
        if (!(focusOwner instanceof JComponent)) {
            return true;
        }
        JComponent jFocusOwner = ((JComponent) focusOwner);
        InputVerifier verifier = jFocusOwner.getInputVerifier();
        if (verifier != null) {
            return verifier.verify(jFocusOwner);
        }
        return true;
    }

    @Override
    public void requestFocus() {
        if (checkRequestedFocus()) {
            super.requestFocus();
        }
    }

    @Override
    public boolean requestFocus(boolean temporary) {
        if (!checkRequestedFocus()) {
            return false;
        }
        return super.requestFocus(temporary);
    }

    @Override
    public boolean requestFocusInWindow() {
        if (!checkRequestedFocus()) {
            return false;
        }
        return super.requestFocusInWindow();
    }

    @Override
    protected boolean requestFocusInWindow(boolean temporary) {
        if (!checkRequestedFocus()) {
            return false;
        }
        return super.requestFocusInWindow(temporary);
    }

    @Deprecated
    public boolean requestDefaultFocus() {
        Component defaultComponent = null;
        Container cycleRoot = isFocusCycleRoot() ? this : getFocusCycleRootAncestor();
        if (cycleRoot != null) {
            defaultComponent = cycleRoot.getFocusTraversalPolicy().getDefaultComponent(this);
        }
        if (defaultComponent == null) {
            return false;
        }
        defaultComponent.requestFocus();
        return true;
    }

    @Deprecated
    public boolean isManagingFocus() {
        return false;
    }

    public void grabFocus() {
        requestFocus();
    }

    public void setOpaque(boolean opaque) {
        LookAndFeel.markPropertyNotInstallable(this, StringConstants.OPAQUE_PROPERTY);
        if (this.opaque != opaque) {
            boolean oldOpaque = this.opaque;
            this.opaque = opaque;
            repaint();
            firePropertyChange(StringConstants.OPAQUE_PROPERTY, oldOpaque, opaque);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        boolean oldEnabledValue = isEnabled();
        super.setEnabled(enabled);
        firePropertyChange(StringConstants.ENABLED_PROPERTY_CHANGED, oldEnabledValue, enabled);
        if (oldEnabledValue != enabled) {
            repaint();
        }
    }

    public void setDoubleBuffered(boolean flag) {
        doubleBuffered = flag;
    }

    public void setDebugGraphicsOptions(int newDebugGraphicsOptions) {
        debugGraphicsOptions = (newDebugGraphicsOptions != DebugGraphics.NONE_OPTION) ? newDebugGraphicsOptions
                : 0;
    }

    @Override
    public float getAlignmentY() {
        return (alignmentY != null) ? alignmentY.floatValue() : 0.5f;
    }

    @Override
    public float getAlignmentX() {
        return (alignmentX != null) ? alignmentX.floatValue() : 0.5f;
    }

    public void setAlignmentY(float alignment) {
        alignmentY = new Float((alignment < 0) ? 0 : ((alignment > 1.0f) ? 1.0f : alignment));
    }

    public void setAlignmentX(float alignment) {
        alignmentX = new Float((alignment < 0) ? 0 : ((alignment > 1.0f) ? 1.0f : alignment));
    }

    public boolean isValidateRoot() {
        return false;
    }

    public void revalidate() {
        invalidate();
        RepaintManager.currentManager(this).addInvalidComponent(this);
    }

    public boolean isPaintingTile() {
        return false;
    }

    public boolean isOptimizedDrawingEnabled() {
        return true;
    }

    @Override
    public boolean isOpaque() {
        return opaque;
    }

    @Override
    public boolean isDoubleBuffered() {
        return doubleBuffered;
    }

    public void setAutoscrolls(boolean scrolls) {
        LookAndFeel.markPropertyNotInstallable(this, AUTOSCROLLS_PROPERTY_NAME);
        autoscrolls = scrolls;
    }

    public boolean getAutoscrolls() {
        return autoscrolls;
    }

    protected void setUI(ComponentUI newUI) {
        ComponentUI oldUI = ui;
        if (ui != null) {
            ui.uninstallUI(this);
        }
        ui = newUI;
        if (ui != null) {
            ui.installUI(this);
        }
        revalidate();
        repaint();
        firePropertyChange(UI_PROPERTY_NAME, oldUI, newUI);
    }

    public void updateUI() {
        if (UIManager.get(getUIClassID()) != null) {
            setUI(UIManager.getUI(this));
        }
    }

    public int getDebugGraphicsOptions() {
        return debugGraphicsOptions;
    }

    public static void setDefaultLocale(Locale locale) {
        defaultLocale = locale;
    }

    public static Locale getDefaultLocale() {
        return defaultLocale;
    }

    public static boolean isLightweightComponent(Component component) {
        return component.isLightweight();
    }

    private boolean isOfPrintableType(Object obj) {
        return (obj instanceof Boolean || obj instanceof Point || obj instanceof Insets
                || obj instanceof Rectangle || obj instanceof Dimension
                || obj instanceof Number || obj instanceof String);
    }

    private boolean isDefaultValue(Object obj, Class<?> clazz) {
        if (obj == null) {
            return true;
        }
        if (obj.getClass() == clazz) {
            return false;
        }
        if (obj instanceof Boolean && !((Boolean) obj).booleanValue()) {
            return true;
        }
        if (obj instanceof Number && ((Number) obj).doubleValue() == 0) {
            return true;
        }
        return false;
    }

    @Override
    protected String paramString() {
        String result = "";
        boolean addComma = false;
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(getClass(), Container.class);
            for (int i = 0; i < beanInfo.getPropertyDescriptors().length; i++) {
                PropertyDescriptor descriptor = beanInfo.getPropertyDescriptors()[i];
                if (descriptor.getReadMethod() != null && descriptor.getWriteMethod() != null) {
                    String fieldName = descriptor.getName();
                    Class<?> type = descriptor.getPropertyType();
                    Object fieldValue = descriptor.getReadMethod().invoke(this);
                    if (isDefaultValue(fieldValue, type)) {
                        continue;
                    }
                    if (addComma) {
                        result += ",";
                    }
                    result += fieldName + "=";
                    if (isOfPrintableType(fieldValue)) {
                        result += fieldValue;
                    } else {
                        result += fieldValue.getClass().getName() + '@'
                                + System.identityHashCode(fieldValue);
                    }
                    addComma = true;
                }
            }
        } catch (IntrospectionException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return result;
    }

    public void setInheritsPopupMenu(boolean value) {
        boolean oldValue = inheritsPopupMenu;
        inheritsPopupMenu = value;
        firePropertyChange(INHERITS_POPUP_MENU_PROPERTY_NAME, oldValue, value);
    }

    public boolean getInheritsPopupMenu() {
        return inheritsPopupMenu;
    }

    public void setComponentPopupMenu(JPopupMenu popup) {
        JPopupMenu oldValue = componentPopupMenu;
        componentPopupMenu = popup;
        firePropertyChange(COMPONENT_POPUP_MENU_PROPERTY_NAME, oldValue, popup);
    }

    public JPopupMenu getComponentPopupMenu() {
        JPopupMenu result = componentPopupMenu;
        if (result == null && getInheritsPopupMenu() && (getParent() instanceof JComponent)) {
            result = ((JComponent) getParent()).getComponentPopupMenu();
        }
        return result;
    }

    public Point getPopupLocation(MouseEvent event) {
        return null;
    }

    <T extends EventListener> boolean hasListener(Class<T> listenerType, EventListener l) {
        EventListener[] listeners = listenerList.getListeners(listenerType);
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == l) {
                return true;
            }
        }
        return false;
    }

    void paintDoubleBuffered(Graphics g) {
        Rectangle clipRect = g.getClipBounds();
        if (clipRect == null) {
            clipRect = SwingUtilities.getLocalBounds(this);
        }
        Image image = RepaintManager.currentManager(this).getVolatileOffscreenBuffer(this,
                clipRect.width + 1, clipRect.height + 1);
        if (image == null) {
            return;
        }
        Graphics offscreenGraphics = image.getGraphics();
        offscreenGraphics.translate(-clipRect.x, -clipRect.y);
        offscreenGraphics.setClip(clipRect);
        doubleBufferingRoot = true;
        try {
            paint(getComponentGraphics(offscreenGraphics));
            g.drawImage(image, clipRect.x, clipRect.y, this);
        } finally {
            doubleBufferingRoot = false;
            offscreenGraphics.dispose();
        }
    }

    boolean insideDoubleBuffering() {
        if (doubleBufferingRoot) {
            return true;
        }
        Component currentComponent = getParent();
        while (currentComponent != null) {
            if (currentComponent instanceof JComponent
                    && ((JComponent) currentComponent).doubleBufferingRoot) {
                return true;
            }
            currentComponent = currentComponent.getParent();
        }
        return false;
    }

    private void addAncestorComponentNotifier() {
        if (ancestorComponentNotifier != null) {
            return;
        }
        ancestorComponentNotifier = new AncestorComponentNotifier();
        addHierarchyListener(ancestorComponentNotifier);
        addHierarchyBoundsListener(ancestorComponentNotifier);
        addComponentListener(ancestorComponentNotifier);
    }

    private void removeAncestorComponentNotifier() {
        if (ancestorComponentNotifier == null
                || !Utilities.isEmptyArray(getAncestorListeners())) {
            return;
        }
        removeComponentListener(ancestorComponentNotifier);
        removeHierarchyBoundsListener(ancestorComponentNotifier);
        removeHierarchyListener(ancestorComponentNotifier);
        ancestorComponentNotifier = null;
    }

    private Graphics getChildJComponentGraphics(Graphics g, Component c) {
        Graphics result = g.create(c.getX(), c.getY(), c.getWidth(), c.getHeight());
        return (c instanceof JComponent) ? ((JComponent) c).getComponentGraphics(result)
                : result;
    }

    private static boolean isDoubleBufferingEnabled(Component c) {
        return c != null && (c.isDoubleBuffered() || isDoubleBufferingEnabled(c.getParent()));
    }

    /**
     * @param rect is supposed to be initialized  with (0, 0, getWidth(), getHeight())
     */
    private static Rectangle getComponentVisibleRect(Component c, Rectangle rect) {
        Rectangle result = rect;
        auxRectangle.setBounds(0, 0, 0, 0);
        Container ancestor = Utilities.getNotWindowParent(c);
        while (ancestor != null && ancestor.isVisible()) {
            auxRectangle.setSize(ancestor.getSize());
            Rectangle translatedRect = SwingUtilities.convertRectangle(ancestor, auxRectangle,
                    c);
            result = SwingUtilities.computeIntersection(result.x, result.y, result.width,
                    result.height, translatedRect);
            ancestor = Utilities.getNotWindowParent(ancestor);
        }
        return result;
    }
    //    public void setFocusTraversalKeys(int id,
    //            Set<T extends AWTKeyStroke> keystrokes) {
    //
    //    }
}
