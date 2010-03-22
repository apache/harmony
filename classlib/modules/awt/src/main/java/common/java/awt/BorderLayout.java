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
 * @author Michael Danilov
 */
package java.awt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.harmony.awt.FieldsAccessor;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.wtk.NativeWindow;


public class BorderLayout implements LayoutManager2, Serializable {
    private static final long serialVersionUID = -8658291919501921765L;

    public static final String BEFORE_LINE_BEGINS = "Before"; //$NON-NLS-1$
    public static final String BEFORE_FIRST_LINE = "First"; //$NON-NLS-1$
    public static final String AFTER_LINE_ENDS = "After"; //$NON-NLS-1$
    public static final String AFTER_LAST_LINE = "Last"; //$NON-NLS-1$
    public static final String LINE_START = "Before"; //$NON-NLS-1$
    public static final String PAGE_START = "First"; //$NON-NLS-1$
    public static final String LINE_END = "After"; //$NON-NLS-1$
    public static final String PAGE_END = "Last"; //$NON-NLS-1$
    public static final String CENTER = "Center"; //$NON-NLS-1$
    public static final String NORTH = "North"; //$NON-NLS-1$
    public static final String SOUTH = "South"; //$NON-NLS-1$
    public static final String EAST = "East"; //$NON-NLS-1$
    public static final String WEST = "West"; //$NON-NLS-1$

    private static final Set<String> supportedConstraints = new HashSet<String>();

    private static final int DEFAULT_GAP = 0;
    private static final int MAX_COMPONENTS = 5;

    private static final int C = 0;
    private static final int N = 1;
    private static final int S = 2;
    private static final int E = 3;
    private static final int W = 4;

    private static final Dimension dummyDimension = new Dimension(0, 0);

    private final transient Toolkit toolkit;

    private final LinkedList<Component> components;
    private final Map<Component, Object> components2Constraints;
    private final Map<Object, Component> constraints2Components;

    private int hGap;
    private int vGap;

    private boolean valid;
    /* Cached data */
    private final Component[] visibleComponents;
    private final Dimension[] minCompSizes;
    private final Dimension[] prefCompSizes;
    private int vGapOverhead;
    private int hGapOverhead;
    private int visibleComponentsNumber;

    static {
        supportedConstraints.add(LINE_START);
        supportedConstraints.add(PAGE_START);
        supportedConstraints.add(LINE_END);
        supportedConstraints.add(PAGE_END);
        supportedConstraints.add(CENTER);
        supportedConstraints.add(NORTH);
        supportedConstraints.add(SOUTH);
        supportedConstraints.add(WEST);
        supportedConstraints.add(EAST);
    }

    public BorderLayout(int hgap, int vgap) {
        toolkit = Toolkit.getDefaultToolkit();

        toolkit.lockAWT();
        try {
            components = new LinkedList<Component>();
            components2Constraints = new HashMap<Component, Object>();
            constraints2Components = new HashMap<Object, Component>();

            hGap = hgap;
            vGap = vgap;

            valid = false;
            visibleComponents = new Component[MAX_COMPONENTS];
            minCompSizes = new Dimension[MAX_COMPONENTS];
            prefCompSizes = new Dimension[MAX_COMPONENTS];
            vGapOverhead = 0;
            hGapOverhead = 0;
            visibleComponentsNumber = 0;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public BorderLayout() {
        this(DEFAULT_GAP, DEFAULT_GAP);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
        toolkit.lockAWT();
        try {
            Object cons;
            Component comp2Forget;

            if (name == null) {
                cons = CENTER;
            } else {
                cons = name;
                if (!supportedConstraints.contains(cons)) {
                    // awt.91=Unsupported constraints object: {0}
                    throw new IllegalArgumentException(Messages.getString("awt.91", cons)); //$NON-NLS-1$
                }
            }

            if ((constraints2Components.get(cons) == comp)
                    && (components2Constraints.get(comp) == cons))
            {
                return;
            }

            comp2Forget = constraints2Components.get(cons);
            if (comp2Forget != null) {
                forgetComponent(comp2Forget, cons);
            }
            if (components.contains(comp)) {
                forgetComponent(comp, cons);
            }

            components.addFirst(comp);
            components2Constraints.put(comp, cons);
            constraints2Components.put(cons, comp);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void addLayoutComponent(Component comp, Object constraints) {
        toolkit.lockAWT();
        try {
            if (comp == null) {
                throw new NullPointerException("Component is null");
            }
            if ((constraints == null) || (constraints instanceof String)) {
                addLayoutComponent((String) constraints, comp);
            } else {
                // awt.92=Constraints object must be String
                throw new IllegalArgumentException(Messages.getString("awt.92")); //$NON-NLS-1$
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void removeLayoutComponent(Component comp) {
        toolkit.lockAWT();
        try {
            Object cons = components2Constraints.get(comp);

            if (cons != null) {
                forgetComponent(comp, cons);
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component getLayoutComponent(Object constraints) {
        toolkit.lockAWT();
        try {
            if (!supportedConstraints.contains(constraints)) {
                throw new IllegalArgumentException(
                        // awt.91=Unsupported constraints object: {0}
                        Messages.getString("awt.91", constraints)); //$NON-NLS-1$
            }

            return constraints2Components.get(constraints);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Component getLayoutComponent(Container target, Object constraints) {        
        boolean l2r = target.getComponentOrientation().isLeftToRight();
        if (NORTH.equals(constraints)) {
            return getComponent(PAGE_START, NORTH);
        } else if (SOUTH.equals(constraints)) {
            return getComponent(PAGE_END, SOUTH);
        } else if (EAST.equals(constraints)) {
            return getComponent(l2r ? LINE_END : LINE_START, EAST);
        } else if (WEST.equals(constraints)) {
            return getComponent(l2r ? LINE_START : LINE_END, WEST);
        } else if (CENTER.equals(constraints)) {
            return constraints2Components.get(CENTER);
        }
        // awt.93=cannot get component: invalid constraint: {0}
        throw new IllegalArgumentException(Messages.getString("awt.93", //$NON-NLS-1$
                                           constraints));
    }

    private Component getComponent(Object relCons, Object absCons) {
        Component comp = constraints2Components.get(relCons);
        if (comp != null) {
            return comp;
        }
        return constraints2Components.get(absCons);
    }
    
    public Object getConstraints(Component comp) {
        if (comp == null) {
            return comp;
        }

        return components2Constraints.get(comp);
    }

    public void invalidateLayout(Container target) {
        toolkit.lockAWT();
        try {
            valid = false;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension maximumLayoutSize(Container target) {
        toolkit.lockAWT();
        try {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension minimumLayoutSize(Container target) {
        toolkit.lockAWT();
        try {
            boolean wasValid = valid;
            validate(target);
            valid = wasValid;

            if (visibleComponentsNumber == 0) {
                return target.addInsets(new Dimension(0, 0));
            }

            return target.addInsets(calculateLayoutSize(minCompSizes));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension preferredLayoutSize(Container target) {
        toolkit.lockAWT();
        try {
            boolean wasValid = valid;
            validate(target);
            valid = wasValid;

            return target.addInsets(calculateLayoutSize(prefCompSizes));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void layoutContainer(Container target) {
        toolkit.lockAWT();
        try {
            validate(target);

            if (visibleComponentsNumber != 0) {
                Rectangle clientRect = target.getClient();

                if (clientRect.isEmpty()) {
                    resetBounds(clientRect);
                } else {
                    setBounds(clientRect);
                }
            }
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getHgap() {
        toolkit.lockAWT();
        try {
            return hGap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setHgap(int hgap) {
        toolkit.lockAWT();
        try {
            hGap = hgap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public int getVgap() {
        toolkit.lockAWT();
        try {
            return vGap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void setVgap(int vgap) {
        toolkit.lockAWT();
        try {
            vGap = vgap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public String toString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new BorderLayout());
         */

        toolkit.lockAWT();
        try {
            return getClass().getName() + "[hgap=" + hGap + ",vgap=" + vGap + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public float getLayoutAlignmentX(Container parent) {
        toolkit.lockAWT();
        try {
            return Component.CENTER_ALIGNMENT;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public float getLayoutAlignmentY(Container parent) {
        toolkit.lockAWT();
        try {
            return Component.CENTER_ALIGNMENT;
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void validate(Container target) {
        if (valid) {
            return;
        }
        valid = true;

        validateArrays(target);
        if (visibleComponentsNumber != 0) {
            validateGaps();
        }
    }

    private void validateGaps() {
        int h = -1;

        if (visibleComponents[N] != null) {
            h++;
        }
        if (visibleComponents[S] != null) {
            h++;
        }
        if ((visibleComponents[C] != null)
                || (visibleComponents[W] != null)
                || (visibleComponents[E] != null))
        {
            hGapOverhead = (visibleComponentsNumber - h - 2) * hGap;
            h++;
        } else {
            hGapOverhead = 0;
        }
        vGapOverhead = h * vGap;
    }

    private void validateArrays(Container target) {
        Arrays.fill(minCompSizes, dummyDimension);
        Arrays.fill(prefCompSizes, dummyDimension);
        Arrays.fill(visibleComponents, null);
        visibleComponentsNumber = 0;

        for (Component comp : components) {
            int index = constraints2Index(components2Constraints.get(comp),
                    target.getComponentOrientation());

            if (comp.isVisible()) {
                if (visibleComponents[index] == null) {
                    visibleComponents[index] = comp;
                    visibleComponentsNumber++;
                    minCompSizes[index] = comp.getMinimumSize();
                    prefCompSizes[index] = comp.getPreferredSize();
                }
            } else if (visibleComponents[index] == null){
                 minCompSizes[index] = comp.getMinimumSize();
                 prefCompSizes[index] = comp.getPreferredSize();
            }
        }
    }

    private int constraints2Index(Object cons, ComponentOrientation orientation) {
        if (cons.equals(CENTER)) {
            return C;
        } else if (cons.equals(NORTH)) {
            return N;
        } else if (cons.equals(SOUTH)) {
            return S;
        } else if (cons.equals(WEST)) {
            return W;
        } else if (cons.equals(EAST)) {
            return E;
        } else if (cons.equals(LINE_START)) {
            return (!orientation.isLeftToRight() ? E : W);
        } else if (cons.equals(LINE_END)) {
            return (!orientation.isLeftToRight() ? W : E);
        } else if (cons.equals(PAGE_START)) {
            return N;
        } else {//if (cons.equals(PAGE_END))
            return S;
        }
    }

    private Dimension calculateLayoutSize(Dimension[] sizes) {
        int w = Math.max(sizes[N].width,
                Math.max(sizes[S].width,
                sizes[W].width + sizes[C].width + sizes[E].width + hGapOverhead));
        int h = sizes[N].height
                + sizes[S].height
                + Math.max(sizes[C].height, Math.max(sizes[W].height, sizes[E].height))
                + vGapOverhead;

        return new Dimension(w, h);
    }

    private void setBounds(Rectangle clientRect) {
        int centerX = clientRect.x;
        int middleY = clientRect.y;

        if (visibleComponents[N] != null) {
            visibleComponents[N].setSize(clientRect.width, 
                    prefCompSizes[N].height);
            visibleComponents[N].setBounds(clientRect.x, clientRect.y,
                    clientRect.width, prefCompSizes[N].height, 
                    NativeWindow.BOUNDS_NOSIZE, true);
            middleY += prefCompSizes[N].height + vGap;
        }
        int middleHeight = clientRect.height - prefCompSizes[N].height
                - prefCompSizes[S].height - vGapOverhead;
        middleHeight = Math.max(middleHeight, 0);
        if (visibleComponents[W] != null) {
            visibleComponents[W].setSize(prefCompSizes[W].width, middleHeight);
            visibleComponents[W].setBounds(clientRect.x, middleY,
                    prefCompSizes[W].width, middleHeight, 
                    NativeWindow.BOUNDS_NOSIZE, true);
            centerX += prefCompSizes[W].width + hGap;
        }
        int middleWidth = clientRect.width - prefCompSizes[W].width -
                prefCompSizes[E].width - hGapOverhead;
        middleWidth = Math.max(middleWidth, 0);
        if (visibleComponents[C] != null) {
            visibleComponents[C].setBounds(centerX, middleY, middleWidth, middleHeight);
        }
        if (visibleComponents[S] != null) {
            visibleComponents[S].setSize(clientRect.width, 
                    prefCompSizes[S].height);
            visibleComponents[S].setBounds(clientRect.x,
                    clientRect.y + clientRect.height - prefCompSizes[S].height,
                    clientRect.width, prefCompSizes[S].height, 
                    NativeWindow.BOUNDS_NOSIZE, true);
        }
        if (visibleComponents[E] != null) {
            visibleComponents[E].setSize(prefCompSizes[E].width, middleHeight);
            visibleComponents[E].setBounds(
                    clientRect.x + clientRect.width - prefCompSizes[E].width,
                    middleY, prefCompSizes[E].width, middleHeight, 
                    NativeWindow.BOUNDS_NOSIZE, true);
        }
    }

    private void resetBounds(Rectangle clientRect) {
        for (int i = 0; i < MAX_COMPONENTS; i++) {
            Component comp = visibleComponents[i];

            if (comp != null) {
                comp.setBounds(clientRect.x, clientRect.y, 0, 0);
            }
        }
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();

        FieldsAccessor accessor = new FieldsAccessor(BorderLayout.class, this);
        accessor.set("toolkit", Toolkit.getDefaultToolkit()); //$NON-NLS-1$
    }

    private void forgetComponent(Component comp, Object cons) {
        components.remove(comp);
        components2Constraints.remove(comp);
        constraints2Components.remove(cons);
    }

}
