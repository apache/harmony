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
import java.util.*;

import org.apache.harmony.awt.FieldsAccessor;


public class FlowLayout implements LayoutManager, Serializable {
    private static final long serialVersionUID = -7262534875583282631L;

    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;
    public static final int LEADING = 3;
    public static final int TRAILING = 4;

    private static final int DEFAULT_GAP = 5;

    private final transient Toolkit toolkit = Toolkit.getDefaultToolkit();

    private int hGap;
    private int vGap;
    private int alignment;

    private transient Component[] components;

    public FlowLayout(int align) {
        this(align, DEFAULT_GAP, DEFAULT_GAP);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public FlowLayout() {
        this(CENTER, DEFAULT_GAP, DEFAULT_GAP);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    public FlowLayout(int align, int hgap, int vgap) {
        toolkit.lockAWT();
        try {
            hGap = hgap;
            vGap = vgap;
            alignment = align;
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public String toString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new FlowLayout());
         */

        toolkit.lockAWT();
        try {
            String alignStr;
            switch (alignment) {
            case LEFT:
                alignStr = "left"; //$NON-NLS-1$
                break;
            case RIGHT:
                alignStr = "right"; //$NON-NLS-1$
                break;
            case CENTER:
                alignStr = "center"; //$NON-NLS-1$
                break;
            case TRAILING:
                alignStr = "trailing"; //$NON-NLS-1$
                break;
            case LEADING:
            default:
                alignStr = "leading"; //$NON-NLS-1$
            }
            return (getClass().getName() + "[hgap=" + hGap + ",vgap=" +vGap +  //$NON-NLS-1$ //$NON-NLS-2$
                    ",align=" + alignStr + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void addLayoutComponent(String name, Component comp) {
        // do nothing here according to spec
    }

    public int getAlignment() {
        toolkit.lockAWT();
        try {
            return alignment;
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

    public int getVgap() {
        toolkit.lockAWT();
        try {
            return vGap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void layoutContainer(Container target) {
        toolkit.lockAWT();
        try {
            components = target.getComponents();
            if (getComponentsNumber() == 0) {
                return;
            }
            doLayoutContainer(target);
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void doLayoutContainer(Container target) {
        Rectangle clientRect = target.getClient();
        Insets insets = target.getInsets();
        boolean l2r = target.getComponentOrientation().isLeftToRight();
        ArrayList<Component> rowComponents = new ArrayList<Component>();
        int initW = 2 * hGap;
        int w = initW;
        int y = insets.top + vGap;
        int maxH = 0;
        boolean first = true;

        for (Component component : getComponentsZOrder(target)) {
            if ((component == null) || !component.isVisible()) {
                continue;
            }

            Dimension cd = component.getPreferredSize();

            if (!first && ((w + cd.width) > clientRect.width)) {
                layoutRow(rowComponents, clientRect.width - (w - hGap),
                        insets.left, y, maxH, l2r);
                rowComponents.clear();

                w = initW;
                y += maxH + vGap;
                maxH = 0;
            }
            first = false;
            rowComponents.add(component);
            w += cd.width + hGap;
            maxH = Math.max(maxH, cd.height);
        }
        layoutRow(rowComponents, clientRect.width - (w - hGap), insets.left, y, maxH, l2r);
    }

    public Dimension minimumLayoutSize(Container target) {
        toolkit.lockAWT();
        try {
            components = target.getComponents();
            if (getComponentsNumber() == 0) {
                // not (0,0) for compatibility!
                return target.addInsets(new Dimension(10, 10));
            }

            return target.addInsets(layoutSize(false));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension preferredLayoutSize(Container target) {
        toolkit.lockAWT();
        try {
            components = target.getComponents();

            if (getComponentsNumber() == 0) {
                // not (0,0) for compatibility!
                return target.addInsets(new Dimension(10, 10));
            }

            return target.addInsets(layoutSize(true));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void removeLayoutComponent(Component comp) {
        // do nothing here according to spec        
    }

    public void setAlignment(int align) {
        toolkit.lockAWT();
        try {
            alignment = align;
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

    public void setVgap(int vgap) {
        toolkit.lockAWT();
        try {
            vGap = vgap;
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void layoutRow(ArrayList<Component> rowComponents, int freeW, int x, int y,
                           int maxH, boolean l2r) {
        x += hGap;
        switch (alignment) {
        case LEFT:
            break;
        case RIGHT:
            x += freeW;
            break;
        case CENTER:
            x += freeW/2;
            break;
        case TRAILING:
            x += !l2r ? 0 : freeW;
            break;
        case LEADING:
        default: // any invalid alignment is treated as LEADING
            x += l2r ? 0 : freeW;
            break;
        }

        int lastInd = rowComponents.size() - 1;
        for (int i = 0; i <= lastInd ; i++) {
            Component c = rowComponents.get(l2r ? i : lastInd - i);
            Dimension d = c.getPreferredSize();
            c.setBounds(x, y + (maxH - d.height) / 2, d.width, d.height);
            x += d.width + hGap;
        }
    }

    private int getComponentsNumber() {
        int componentsNumber = 0;
        int i = 0;
        while (i < components.length) {
            if (components[i].isVisible()) {
                componentsNumber++;
            }
            i++;
        }

        return componentsNumber;
    }

    private Dimension layoutSize(boolean preferred) {
        int w = hGap;
        int maxH = 0;

        for (Component component : components) {
            if (component.isVisible()) {
                Dimension cd = preferred ?
                        component.getPreferredSize() :
                        component.getMinimumSize();
                maxH = Math.max(maxH, cd.height);
                w += cd.width + hGap;
            }
        }

        return new Dimension(w, maxH + 2 * vGap);
    }

    private ArrayList<Component> getComponentsZOrder(Container target) {
        int capacity = target.getComponentCount();
        ArrayList<Component> zComponents = new ArrayList<Component>(capacity);

        for (int i = 0; i < capacity; i++) {
            zComponents.add(null);
        }

        for (Component component : components) {
            if (component.getParent() == target) {
                zComponents.set(target.getComponentZOrder(component), component);
            }
        }

        return zComponents;
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {

        stream.defaultReadObject();

        FieldsAccessor accessor = new FieldsAccessor(FlowLayout.class, this);
        accessor.set("toolkit", Toolkit.getDefaultToolkit()); //$NON-NLS-1$
    }
}
