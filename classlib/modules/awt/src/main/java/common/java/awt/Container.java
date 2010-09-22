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

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.HierarchyEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.EventListener;
import java.util.Iterator;
import java.util.Set;

import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;

import org.apache.harmony.awt.internal.nls.Messages;

import org.apache.harmony.awt.gl.MultiRectArea;

public class Container extends Component {
    private static final long serialVersionUID = 4613797578919906343L;

    private final Vector<Component> children = new Vector<Component>();

    private final AWTListenerList<ContainerListener> containerListeners = new AWTListenerList<ContainerListener>(
            this);

    private FocusTraversalPolicy focusTraversalPolicy;

    private LayoutManager layout;

    private Dimension minimumLayoutSize;

    private Dimension preferredLayoutSize;

    Object layoutData;

    boolean focusCycleRoot;

    private boolean focusTraversalPolicyProvider;

    boolean isRemoved; // set to true/false on removeNotify() enter/exit

    protected class AccessibleAWTContainer extends AccessibleAWTComponent {
        private static final long serialVersionUID = 5081320404842566097L;

        protected class AccessibleContainerHandler implements ContainerListener {
            protected AccessibleContainerHandler() {

            }

            public void componentAdded(ContainerEvent e) {
                if (listenersCount <= 0) {
                    return;
                }
                firePropertyChange(AccessibleContext.ACCESSIBLE_CHILD_PROPERTY, null, e
                        .getChild().getAccessibleContext());

            }

            public void componentRemoved(ContainerEvent e) {
                if (listenersCount <= 0) {
                    return;
                }
                firePropertyChange(AccessibleContext.ACCESSIBLE_CHILD_PROPERTY, e.getChild()
                        .getAccessibleContext(), null);

            }

        }

        protected ContainerListener accessibleContainerHandler;

        protected AccessibleAWTContainer() {
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            super.addPropertyChangeListener(listener);
            if (accessibleContainerHandler != null) {
                return;
            }
            accessibleContainerHandler = new AccessibleContainerHandler();
            Container.this.addContainerListener(accessibleContainerHandler);
        }

        @Override
        public Accessible getAccessibleAt(Point p) {
            toolkit.lockAWT();
            try {
                if (!(Container.this instanceof Accessible) || (getAccessibleContext() == null)) {
                    return super.getAccessibleAt(p);
                }
                int count = getAccessibleChildrenCount();
                for (int i = 0; i < count; i++) {
                    Accessible aChild = getAccessibleChild(i);
                    AccessibleContext ac = aChild.getAccessibleContext();
                    if (ac != null) {
                        AccessibleComponent aComp = ac.getAccessibleComponent();
                        Point pos = new Point(p);
                        Point loc = aComp.getLocation();
                        pos.translate(-loc.x, -loc.y);
                        if (aComp.isShowing() && aComp.contains(pos)) {
                            return aChild;
                        }
                    }

                }
                return (Accessible) Container.this;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public Accessible getAccessibleChild(int index) {
            toolkit.lockAWT();
            try {
                int count = 0;
                for (int i = 0; i < children.size(); i++) {
                    if (children.get(i) instanceof Accessible) {
                        if (count == index) {
                            return (Accessible) children.get(i);
                        }
                        ++count;
                    }
                }
                return null;
            } finally {
                toolkit.unlockAWT();
            }
        }

        @Override
        public int getAccessibleChildrenCount() {
            toolkit.lockAWT();
            try {
                int count = 0;
                for (int i = 0; i < children.size(); i++) {
                    if (children.get(i) instanceof Accessible) {
                        ++count;
                    }
                }
                return count;
            } finally {
                toolkit.unlockAWT();
            }
        }
    }

    public Container() {
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    int getComponentIndex(Component comp) {
        return children.indexOf(comp);
    }

    public final int getComponentZOrder(Component comp) {
        toolkit.lockAWT();
        try {
            return children.indexOf(comp);
        } finally {
            toolkit.unlockAWT();
        }
    }

    /*
     * places component at the index pos. of the stacking order (e. g. when it
     * gains focus)
     */
    public final void setComponentZOrder(Component comp, int index) {
        toolkit.lockAWT();
        try {
            int oldIndex = getComponentZOrder(comp);

            if (children.remove(comp)) {
                children.add(index, comp);
            }
            comp.behaviour.setZOrder(index, oldIndex);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component add(Component comp) {
        toolkit.lockAWT();
        try {
            // add component to the bottom of the stacking order
            addImpl(comp, null, -1);

            return comp;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component add(Component comp, int index) {
        toolkit.lockAWT();
        try {
            addImpl(comp, null, index);

            return comp;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void add(Component comp, Object constraints, int index) {
        toolkit.lockAWT();
        try {
            addImpl(comp, constraints, index);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void add(Component comp, Object constraints) {
        toolkit.lockAWT();
        try {
            addImpl(comp, constraints, -1);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component add(String name, Component comp) {
        toolkit.lockAWT();
        try {
            addImpl(comp, name, -1);
            return comp;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void remove(int index) {
        toolkit.lockAWT();
        try {
            Component comp = children.get(index);

            if (layout != null) {
                layout.removeLayoutComponent(comp);
            }
            removeFromContainer(index);
            // container events are synchronous
            dispatchEvent(new ContainerEvent(this, ContainerEvent.COMPONENT_REMOVED, comp));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void remove(Component comp) {
        toolkit.lockAWT();
        try {
            if (comp == null) {
                throw new NullPointerException();
            }

            try {
                remove(children.indexOf(comp));
            } catch (ArrayIndexOutOfBoundsException e) {
                // just silently ignore exception if comp is not found
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void list(PrintWriter out, int indent) {
        toolkit.lockAWT();
        try {
            super.list(out, indent);
            for (int i = 0; i < children.size(); i++) {
                children.get(i).list(out, 2 * indent);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void list(PrintStream out, int indent) {
        toolkit.lockAWT();
        try {
            super.list(out, indent);
            for (int i = 0; i < children.size(); i++) {
                children.get(i).list(out, 2 * indent);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void print(Graphics g) {
        toolkit.lockAWT();
        try {
            paint(g);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void removeAll() {
        toolkit.lockAWT();
        try {
            for (int i = children.size() - 1; i >= 0; i--) {
                remove(i);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void update(Graphics g) {
        toolkit.lockAWT();
        try {
            super.update(g); // calls paint() by default
        } finally {
            toolkit.unlockAWT();
        }
    }

    private int addToContainer(Component comp, int index) {
        int retIndex = index;
        int size = children.size();

        comp.prepare4HierarchyChange();

        retIndex = ((index >= 0) && (index < size)) ? index : size;
        children.add(retIndex, comp);
        comp.setParent(this);
        if (isDisplayable()) {
            comp.mapToDisplay(true);
        }
        invalidate();

        comp.finishHierarchyChange(comp, this, HierarchyEvent.PARENT_CHANGED);

        return retIndex;
    }

    private void removeFromContainer(int index) {
        Component comp = children.get(index);

        comp.prepare4HierarchyChange();

        if (isDisplayable()) {
            comp.mapToDisplay(false);
        }

        children.remove(index);

        comp.setParent(null);
        invalidate();

        comp.finishHierarchyChange(comp, this, HierarchyEvent.PARENT_CHANGED);
    }

    private void addToLayout(Component comp, Object constraints) {
        if (layout != null) {
            if (LayoutManager2.class.isInstance(layout)) {
                ((LayoutManager2) layout).addLayoutComponent(comp, constraints);
            } else {
                layout.addLayoutComponent(
                        (constraints == null) ? null : constraints.toString(), comp);
            }
        }
    }

    protected void addImpl(Component comp, Object constraints, int index)
            throws IllegalArgumentException {
        toolkit.lockAWT();
        try {
            if ((index < -1) || (index > children.size())) {
                // awt.12A=illegal component position
                throw new IllegalArgumentException(Messages.getString("awt.12A")); //$NON-NLS-1$
            }
            if ((comp instanceof Container)) {
                if (comp == this) {
                    // awt.12B=adding container to itself
                    throw new IllegalArgumentException(Messages.getString("awt.12B")); //$NON-NLS-1$
                } else if (((Container) comp).isAncestorOf(this)) {
                    // awt.12C=adding container's parent to itself
                    throw new IllegalArgumentException(Messages.getString("awt.12C")); //$NON-NLS-1$
                } else if (comp instanceof Window) {
                    // awt.12D=adding a window to a container
                    throw new IllegalArgumentException(Messages.getString("awt.12D")); //$NON-NLS-1$
                }
            }

            if (comp.getParent() != null) {
                comp.getParent().remove(comp);
            }
            int trueIndex = addToContainer(comp, index);
            try {
                addToLayout(comp, constraints);
            } catch (IllegalArgumentException e) {
                removeFromContainer(trueIndex);
                throw e;
            }
            comp.behaviour.setZOrder(index, trueIndex);

            // calculated preferred/minimum sizes should be reset
            // because they depend on inherited font
            comp.resetDefaultSize();
            // container events are synchronous
            dispatchEvent(new ContainerEvent(this, ContainerEvent.COMPONENT_ADDED, comp));
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addNotify() {
        toolkit.lockAWT();
        try {
            super.addNotify();
            for (int i = 0; i < children.size(); i++) {
                children.get(i).addNotify();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        toolkit.lockAWT();
        try {
            super.addPropertyChangeListener(listener);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        toolkit.lockAWT();
        try {
            super.addPropertyChangeListener(propertyName, listener);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void applyComponentOrientation(ComponentOrientation o) {
        toolkit.lockAWT();
        try {
            super.applyComponentOrientation(o);
            for (int i = 0; i < children.size(); i++) {
                children.get(i).applyComponentOrientation(o);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public boolean areFocusTraversalKeysSet(int id) {
        toolkit.lockAWT();
        try {
            return super.areFocusTraversalKeysSet(id);
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public int countComponents() {
        toolkit.lockAWT();
        try {
            return children.size();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public void deliverEvent(Event evt) {

        Component comp = getComponentAt(evt.x, evt.y);
        if (comp != null && comp != this) {
            evt.translate(-comp.getX(), -comp.getY());
            comp.deliverEvent(evt);
        } else {
            super.deliverEvent(evt);
        }

    }

    @Override
    public void doLayout() {
        toolkit.lockAWT();
        try {
            layout();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component findComponentAt(Point p) {
        toolkit.lockAWT();
        try {
            return findComponentAt(p.x, p.y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component findComponentAt(int x, int y) {
        toolkit.lockAWT();
        try {
            if (!contains(x, y) || !isShowing()) {
                return null;
            }

            if (!getClient().contains(x, y)) {
                return this;
            }

            Component c = null, fc = null;// = getComponentAt(x,y);
            // if c is not visible, get next component
            // under it, etc. So cannot actually use getComponentAt():
            // have to traverse children manually

            for (int i = 0; i < children.size(); i++) {
                c = children.get(i);
                if (!c.isVisible()) {
                    continue;
                }
                if (c.contains(x - c.getX(), y - c.getY())) {
                    fc = c;
                    break;
                }
            }

            if (fc instanceof Container) {
                fc = ((Container) fc).findComponentAt(x - fc.getX(), y - fc.getY());
            }

            if (fc == null) {
                fc = this;
            }
            return fc;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public float getAlignmentX() {
        toolkit.lockAWT();
        try {
            if ((layout != null) && (LayoutManager2.class.isInstance(layout))) {
                return ((LayoutManager2) layout).getLayoutAlignmentX(this);
            }

            return super.getAlignmentX();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public float getAlignmentY() {
        toolkit.lockAWT();
        try {
            if ((layout != null) && (LayoutManager2.class.isInstance(layout))) {
                return ((LayoutManager2) layout).getLayoutAlignmentY(this);
            }

            return super.getAlignmentY();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component getComponent(int n) throws ArrayIndexOutOfBoundsException {
        toolkit.lockAWT();
        try {
            return children.get(n);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Component getComponentAt(Point p) {
        toolkit.lockAWT();
        try {
            return getComponentAt(p.x, p.y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Component getComponentAt(int x, int y) {
        toolkit.lockAWT();
        try {
            return locate(x, y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getComponentCount() {
        toolkit.lockAWT();
        try {
            return countComponents();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component[] getComponents() {
        toolkit.lockAWT();
        try {
            return children.toArray(new Component[0]);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        toolkit.lockAWT();
        try {
            return super.getFocusTraversalKeys(id);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public FocusTraversalPolicy getFocusTraversalPolicy() {
        toolkit.lockAWT();
        try {
            if (isFocusTraversalPolicyProvider() || focusCycleRoot) {
                if (isFocusTraversalPolicySet()) {
                    return focusTraversalPolicy;
                }
                Container root = getFocusCycleRootAncestor();
                return ((root != null) ? root.getFocusTraversalPolicy() : KeyboardFocusManager
                        .getCurrentKeyboardFocusManager().getDefaultFocusTraversalPolicy());
            }
            return null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public Insets getInsets() {
        toolkit.lockAWT();
        try {
            return insets();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public LayoutManager getLayout() {
        toolkit.lockAWT();
        try {
            return layout;
        } finally {
            toolkit.unlockAWT();
        }
    }

    Dimension addInsets(Dimension size) {
        Insets insets = getInsets();
        size.width += insets.left + insets.right;
        size.height += insets.top + insets.bottom;

        return size;
    }

    Rectangle getClient() {
        Insets insets = getInsets();
        return new Rectangle(insets.left, insets.top, w - insets.right - insets.left, h
                - insets.top - insets.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        toolkit.lockAWT();
        try {
            return minimumSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public Dimension minimumSize() {
        toolkit.lockAWT();
        try {
            return super.minimumSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    Dimension getDefaultMinimumSize() {
        if (layout == null) {
            return null;
        }
        if (minimumLayoutSize == null) {
            minimumLayoutSize = layout.minimumLayoutSize(this);
        }
        return minimumLayoutSize;
    }

    @Override
    public Dimension getPreferredSize() {
        toolkit.lockAWT();
        try {
            return preferredSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public Dimension preferredSize() {
        toolkit.lockAWT();
        try {
            return super.preferredSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    Dimension getDefaultPreferredSize() {
        if (layout == null) {
            return null;
        }
        if (preferredLayoutSize == null) {
            preferredLayoutSize = layout.preferredLayoutSize(this);
        }
        return preferredLayoutSize;
    }

    @Override
    public Dimension getMaximumSize() {
        toolkit.lockAWT();
        try {
            if (!isMaximumSizeSet() && (layout != null) && (layout instanceof LayoutManager2)) {
                return ((LayoutManager2) layout).maximumLayoutSize(this);
            }
            return super.getMaximumSize();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    void resetDefaultSize() {
        minimumLayoutSize = null;
        preferredLayoutSize = null;

        for (int i = 0; i < children.size(); i++) {
            Component c = children.get(i);
            c.resetDefaultSize();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public Insets insets() {
        toolkit.lockAWT();
        try {
            return getNativeInsets();
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isAncestorOf(Component c) {
        toolkit.lockAWT();
        try {
            if (c == null || c instanceof Window) {
                return false;
            }
            Container parent = c.parent;
            while (parent != null) {
                if (parent == this) {
                    return true;
                }
                if (parent instanceof Window) {
                    return false;
                }
                parent = parent.parent;
            }
            return false;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public boolean isFocusCycleRoot(Container container) {
        toolkit.lockAWT();
        try {
            if (focusCycleRoot && container == this) {
                return true;
            }
            return super.isFocusCycleRoot(container);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isFocusCycleRoot() {
        toolkit.lockAWT();
        try {
            return focusCycleRoot;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public boolean isFocusTraversalPolicySet() {
        toolkit.lockAWT();
        try {
            return focusTraversalPolicy != null;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public void layout() {
        toolkit.lockAWT();
        try {
            if (layout != null) {
                layout.layoutContainer(this);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Deprecated
    @Override
    public Component locate(int x, int y) {
        toolkit.lockAWT();
        try {
            return locateImpl(x, y);
        } finally {
            toolkit.unlockAWT();
        }
    }

    Component locateImpl(int x, int y) {
        // return topmost child containing point - search from index 0
        for (int i = 0; i < children.size(); i++) {
            Component c = children.get(i);
            if (c.contains(x - c.getX(), y - c.getY())) {
                return c;
            }
        }
        if (contains(x, y)) {
            return this;
        }
        return null;
    }

    @Override
    void setRedrawManager() {
        super.setRedrawManager();
        for (int i = 0; i < children.size(); i++) {
            children.get(i).setRedrawManager();
        }
    }

    @Override
    public void paint(Graphics g) {
        toolkit.lockAWT();
        try {
            paintComponentsImpl(g);
        } finally {
            toolkit.unlockAWT();
        }
    }

    void propagateRepaint(long tm, int x, int y, int width, int height) {
        for (int i = 0; i < children.size(); i++) {
            Component comp = children.get(i);

            if (comp.isLightweight()) {
                comp.repaint(tm, x - comp.x, y - comp.y, width, height);
            }
        }
    }

    @Override
    protected String paramString() {
        /*
         * The format is based on 1.5 release behavior which can be revealed by
         * the following code:
         * 
         * Container container = new Container(); container.setLayout(new
         * FlowLayout()); System.out.println(container);
         */

        toolkit.lockAWT();
        try {
            return super.paramString() + (!isValid() ? ",invalid" : "") //$NON-NLS-1$ //$NON-NLS-2$
                    + (layout != null ? ",layout=" + layout.getClass().getName() : ""); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void printComponents(Graphics g) {
        toolkit.lockAWT();
        try {
            paintComponents(g);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void paintComponents(Graphics g) {
        toolkit.lockAWT();
        try {
            paintComponentsImpl(g);
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void paintComponentsImpl(Graphics g) {
        Shape clip;
        
        if ((!isShowing()) || (g == null)) {
            return;
        }
        
        clip = g.getClip();

        for (int i = children.size() - 1; i >= 0; i--) {
            Component comp = children.get(i);

            if (comp.isLightweight() && comp.isVisible()) {
                if ((clip != null) && !clip.intersects(comp.getBounds())) {
                    continue;
                }

                Graphics compGr = getComponentGraphics(g, comp);
                comp.paint(compGr);
                compGr.dispose();
            }
        }
    }

    private Graphics getComponentGraphics(Graphics parent, Component c) {
        Graphics g = parent.create(c.x, c.y, c.w, c.h);
        g.setFont(c.getFont());
        g.setColor(c.getForeground());
        return g;
    }

    @Override
    public void removeNotify() {
        toolkit.lockAWT();
        try {
            isRemoved = true;
            // moveFocusOnHide();
            for (int i = 0; i < children.size(); i++) {
                children.get(i).removeNotify();
            }
            super.removeNotify();
        } finally {
            isRemoved = false;
            toolkit.unlockAWT();
        }
    }

    public void setFocusCycleRoot(boolean b) {
        boolean wasFocusCycleRoot;
        toolkit.lockAWT();
        try {
            wasFocusCycleRoot = focusCycleRoot;
            focusCycleRoot = b;
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("focusCycleRoot", wasFocusCycleRoot, focusCycleRoot); //$NON-NLS-1$
    }

    @Override
    public void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
        toolkit.lockAWT();
        try {
            super.setFocusTraversalKeys(id, keystrokes);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setFocusTraversalPolicy(FocusTraversalPolicy policy) {
        FocusTraversalPolicy oldPolicy;
        toolkit.lockAWT();
        try {
            oldPolicy = focusTraversalPolicy;
            focusTraversalPolicy = policy;
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("focusTraversalPolicy", oldPolicy, policy); //$NON-NLS-1$
    }

    @Override
    public void setFont(Font f) {
        toolkit.lockAWT();
        try {
            super.setFont(f);
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    void setFontImpl(Font f) {
        super.setFontImpl(f);

        for (int i = 0; i < children.size(); i++) {
            children.get(i).propagateFont();
        }
    }

    @Override
    boolean propagateFont() {
        if (!super.propagateFont()) {
            return false;
        }

        for (int i = 0; i < children.size(); i++) {
            children.get(i).propagateFont();
        }
        return true;
    }

    public void setLayout(LayoutManager mgr) {
        toolkit.lockAWT();
        try {
            if (layout != null) {
                for (Component component : children) {
                    layout.removeLayoutComponent(component);
                }
            }

            if (mgr != null) {
                for (Component component : children) {
                    mgr.addLayoutComponent(null, component);
                }
            }

            layout = mgr;
            invalidate();
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void transferFocusBackward() {
        toolkit.lockAWT();
        try {
            super.transferFocusBackward(); // TODO: why override?
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void transferFocusDownCycle() {
        toolkit.lockAWT();
        try {
            if (isFocusCycleRoot()) {
                KeyboardFocusManager kfm = KeyboardFocusManager
                        .getCurrentKeyboardFocusManager();
                Container root = kfm.getCurrentFocusCycleRoot();
                FocusTraversalPolicy policy = getFocusTraversalPolicy();
                if (root != this) {
                    root = this;
                    kfm.setGlobalCurrentFocusCycleRoot(root);

                }
                policy.getDefaultComponent(root).requestFocus();
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void invalidate() {
        toolkit.lockAWT();
        try {
            super.invalidate();
            if ((layout != null) && LayoutManager2.class.isInstance(layout)) {
                ((LayoutManager2) layout).invalidateLayout(this);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public void validate() {
        toolkit.lockAWT();
        try {
            if (isValid() || !behaviour.isDisplayable()) {
                return;
            }

            doLayout();
            validateTree();
            super.validate();

        } finally {
            toolkit.unlockAWT();
        }
    }

    protected void validateTree() {
        toolkit.lockAWT();
        try {
            for (int i = 0; i < children.size(); i++) {
                Component c = children.get(i);
                if (!c.isValid()) {
                    c.validate();
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    void mapToDisplay(boolean b) {
        super.mapToDisplay(b);
        // map to display from bottom to top, to get right initial Z-order
        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).mapToDisplay(b);
        }
    }

    @Override
    void moveFocusOnHide() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .getFocusOwner();
        if (focusOwner != null && isAncestorOf(focusOwner)) {
            focusOwner.moveFocus();
        }
        super.moveFocus();
    }

    @Override
    void prepareChildren4HierarchyChange() {
        for (int i = 0; i < children.size(); i++) {
            children.get(i).prepare4HierarchyChange();
        }
    }

    @Override
    void finishChildrenHierarchyChange(Component changed, Container changedParent,
            int ancestorFlags) {
        for (int i = 0; i < children.size(); i++) {
            children.get(i).finishHierarchyChange(changed, changedParent, ancestorFlags);
        }
    }

    @Override
    void postHierarchyBoundsEvents(Component changed, int id) {
        super.postHierarchyBoundsEvents(changed, id);

        spreadHierarchyBoundsEvents(changed, id);
    }

    @Override
    void spreadHierarchyBoundsEvents(Component changed, int id) {
        for (int i = 0; i < children.size(); i++) {
            children.get(i).postHierarchyBoundsEvents(changed, id);
        }
    }

    public ContainerListener[] getContainerListeners() {
        // toolkit.lockAWT();
        // try {
        return containerListeners.getUserListeners(new ContainerListener[0]);
        // } finally {
        // toolkit.unlockAWT();
        // }
    }

    public void addContainerListener(ContainerListener l) {
        // toolkit.lockAWT();
        // try {
        containerListeners.addUserListener(l);
        // } finally {
        // toolkit.unlockAWT();
        // }
    }

    public void removeContainerListener(ContainerListener l) {
        // toolkit.lockAWT();
        // try {
        containerListeners.removeUserListener(l);
        // } finally {
        // toolkit.unlockAWT();
        // }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        // toolkit.lockAWT();
        // try {
        if (ContainerListener.class.isAssignableFrom(listenerType)) {
            return (T[]) getContainerListeners();
        }
        return super.getListeners(listenerType);
        // } finally {
        // toolkit.unlockAWT();
        // }
    }

    @Override
    protected void processEvent(AWTEvent e) {
        // toolkit.lockAWT();
        // try {
        if (toolkit.eventTypeLookup.getEventMask(e) == AWTEvent.CONTAINER_EVENT_MASK) {
            processContainerEvent((ContainerEvent) e);
        } else {
            super.processEvent(e);
        }
        // } finally {
        // toolkit.unlockAWT();
        // }
    }

    protected void processContainerEvent(ContainerEvent e) {
        // toolkit.lockAWT();
        // try {
        for (Iterator<?> i = containerListeners.getUserIterator(); i.hasNext();) {
            ContainerListener listener = (ContainerListener) i.next();

            switch (e.getID()) {
                case ContainerEvent.COMPONENT_ADDED:
                    listener.componentAdded(e);
                    break;
                case ContainerEvent.COMPONENT_REMOVED:
                    listener.componentRemoved(e);
                    break;
            }
        }
        // } finally {
        // toolkit.unlockAWT();
        // }
    }

    /**
     * Determine if comp contains point pos. Comp must be showing and top-most
     * component in Z-order.
     * 
     * @param comp component to look for
     * @param pos point to look component at
     * 
     * @return
     */
    boolean isComponentAt(Component comp, Point pos) {
        toolkit.lockAWT();
        try {
            Point p = new Point(pos);
            Container container = this;
            Component c = null, fc = null;
            if (!contains(p) || !isShowing()) {
                return false;
            }
            while (container != null) {
                if (!container.getClient().contains(p)) {
                    return false;
                }

                Vector<Component> children = container.children;
                for (int i = 0; i < children.size(); i++) {
                    c = children.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    if (c.contains(p.x - c.getX(), p.y - c.getY())) {
                        fc = c;
                        break;
                    }
                }

                if ((fc == comp) || (container == c) || (fc == container)) {
                    break;
                }

                if (fc instanceof Container) {
                    p.translate(-fc.getX(), -fc.getY());
                    container = (Container) fc;
                } else {
                    container = null;
                }

            }

            if (fc == null) {
                fc = this;
            }
            return (fc == comp);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Point getMousePosition(boolean allowChildren) throws HeadlessException {
        toolkit.lockAWT();
        try {
            Point pos = getMousePosition();
            if ((pos != null) && !allowChildren) {
                // check that there's no [visible] child
                // containing point pos:
                Component child = findComponentAt(pos);
                if (child != this) {
                    pos = null;
                }
            }
            return pos;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public final boolean isFocusTraversalPolicyProvider() {
        return focusTraversalPolicyProvider;
    }

    public final void setFocusTraversalPolicyProvider(boolean provider) {
        boolean wasProvider;
        toolkit.lockAWT();
        try {
            wasProvider = focusTraversalPolicyProvider;
            focusTraversalPolicyProvider = provider;
        } finally {
            toolkit.unlockAWT();
        }
        firePropertyChange("focusTraversalPolicyProvider", wasProvider, //$NON-NLS-1$
                focusTraversalPolicyProvider);
    }

    /**
     * Find which focus cycle root to take when doing keyboard focus traversal
     * and focus owner is a container & focus cycle root itself.
     * 
     * @return
     */
    Container getFocusTraversalRoot() {
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Container root = kfm.getCurrentFocusCycleRoot();
        Container container = this;
        while ((root != container) && (container != null)) {
            container = container.getFocusCycleRootAncestor();
        }
        return (container == root) ? root : null;
    }

    /**
     * Adds parts obscured by components which
     * are above the given component
     * in this container to mra
     * @param mra MultiRectArea to add regions to
     * @param component obscured regions of this component are added
     */
    void addObscuredRegions(MultiRectArea mra, Component component) {
        int z = getComponentZOrder(component);
        int i;
        for (i = 0; i < z; i++) {
            Component comp = getComponent(i);
            if (comp.isDisplayable()&& comp.isVisible()&& comp.isOpaque()) {
                mra.add(comp.getBounds());
            }
        }
        for (i = z + 1; i < getComponentCount(); i++) {
            Component comp = getComponent(i);
            if (comp.isVisible() && !comp.isLightweight()) {
                mra.add(comp.getBounds());
            }
        }
    }
}
