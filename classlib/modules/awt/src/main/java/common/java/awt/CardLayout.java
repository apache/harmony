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

import java.io.Serializable;
import java.util.Hashtable;

import org.apache.harmony.awt.internal.nls.Messages;

public class CardLayout implements LayoutManager2, Serializable {
    private static final long serialVersionUID = -4328196481005934313L;

    private static final int DEFAULT_GAP = 0;

    private int vGap;
    private int hGap;

    private Hashtable<String, Component> nameTable;        //Name to component
    private Hashtable<Component, String> compTable;        //Component to name
    private int curComponent;

    private final Toolkit toolkit = Toolkit.getDefaultToolkit();

    public CardLayout(int hgap, int vgap) {
        toolkit.lockAWT();
        try {
            vGap = vgap;
            hGap = hgap;

            nameTable = new Hashtable<String, Component>();
            compTable = new Hashtable<Component, String>();
            curComponent = 0;
        } finally {
            toolkit.unlockAWT();
        }
    }

    public CardLayout() {
        this(DEFAULT_GAP, DEFAULT_GAP);
        toolkit.lockAWT();
        try {
        } finally {
            toolkit.unlockAWT();
        }
    }

    @Override
    public String toString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new CardLayout());
         */

        toolkit.lockAWT();
        try {
            return getClass().getName() + "[hgap=" + hGap + ",vgap=" +vGap + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

    /**
     * @deprecated
     */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
        toolkit.lockAWT();
        try {
            if (name == null) {
                if (compTable.get(comp) != null) {
                    return;
                }
                name = comp.toString();
            }            

            if (!nameTable.isEmpty()){
                comp.setVisible(false);
            }
            nameTable.put(name, comp);
            compTable.put(comp, name);
            
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void addLayoutComponent(Component comp, Object constraints) {
        toolkit.lockAWT();
        try {
            if (!String.class.isInstance(constraints)) {
                // awt.131=AddLayoutComponent: constraint object must be String
                throw new IllegalArgumentException(Messages.getString("awt.131")); //$NON-NLS-1$
            }
            addLayoutComponent((String) constraints, comp);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void removeLayoutComponent(Component comp) {
        toolkit.lockAWT();
        try {
            if (!compTable.containsKey(comp)) {
                return;
            }
            Container parent = comp.getParent();
            if (parent != null) {
                int idx = parent.getComponentZOrder(comp);
                if (idx == curComponent) {
                    next(parent);
                }
            }

            String name = compTable.get(comp);
            if (name != null) {
                nameTable.remove(name);
            }
            compTable.remove(comp);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void invalidateLayout(Container target) {
        toolkit.lockAWT();
        try {
            //Nothing to invalidate
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void layoutContainer(Container parent) {
        toolkit.lockAWT();
        try {
            if (parent.getComponentCount() == 0) {
                return;
            }

            showCurrent(parent);
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void showCurrent(Container parent) {
        toolkit.lockAWT();
        try {
            if (curComponent >= parent.getComponentCount()) {
                curComponent = 0;
            }
            Rectangle clientRect = parent.getClient();
            Component comp = parent.getComponent(curComponent);
            Rectangle bounds = new Rectangle(clientRect.x + hGap, 
                                             clientRect.y + vGap, 
                                             clientRect.width - 2 * hGap, 
                                             clientRect.height - 2 * vGap);

            comp.setBounds(bounds);
            comp.setVisible(true);

        } finally {
            toolkit.unlockAWT();
        }
    }

    public void first(Container parent) {
        toolkit.lockAWT();
        try {
            check(parent);
            int size = parent.getComponentCount(); 
            if (size == 0) {
                return;
            }

            hideCurrent(parent);
            curComponent = 0;

            showCurrent(parent);
        } finally {
            toolkit.unlockAWT();
        }
    }

    private void hideCurrent(Container parent) {
        if ((curComponent >= 0) && (curComponent < parent.getComponentCount())) {
            parent.getComponent(curComponent).setVisible(false);
        }
    }

    private void check(Container parent) {
        if (parent.getLayout() != this) {
            // awt.132=wrong parent for CardLayout
            throw new IllegalArgumentException(Messages.getString("awt.132")); //$NON-NLS-1$
        }
    }

    public void last(Container parent) {
        toolkit.lockAWT();
        try {
            check(parent);
            int size = parent.getComponentCount(); 
            if ( size == 0) {
                return;
            }

            hideCurrent(parent);
            curComponent = size - 1;

            showCurrent(parent);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void next(Container parent) {
        toolkit.lockAWT();
        try {
            check(parent);
            int size = parent.getComponentCount(); 
            if ( size == 0) {
                return;
            }

            hideCurrent(parent);
            curComponent++;
            if (curComponent >= size) {
                curComponent = 0;
            }

            showCurrent(parent);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void previous(Container parent) {
        toolkit.lockAWT();
        try {
            check(parent);
            int size = parent.getComponentCount(); 
            if ( size == 0) {
                return;
            }

            hideCurrent(parent);
            curComponent --;
            if (curComponent < 0) {
                curComponent = size - 1;
            }

            showCurrent(parent);
        } finally {
            toolkit.unlockAWT();
        }
    }

    public void show(Container parent, String name) {
        toolkit.lockAWT();
        try {
            check(parent);
            int size = parent.getComponentCount();
            if (size == 0) {
                return;
            }

            Component comp = nameTable.get(name);

            if (comp == null) {
                return;
            }

            hideCurrent(parent);
            curComponent = parent.getComponentZOrder(comp);
            showCurrent(parent);
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

    public Dimension minimumLayoutSize(Container parent) {
        toolkit.lockAWT();
        try {
            if (parent.getComponentCount() == 0) {
                return parent.addInsets(new Dimension(0, 0));
            }

            return parent.addInsets(layoutSize(parent, false));
        } finally {
            toolkit.unlockAWT();
        }
    }

    public Dimension preferredLayoutSize(Container parent) {
        toolkit.lockAWT();
        try {
            if (parent.getComponentCount() == 0) {
                return parent.addInsets(new Dimension(0, 0));
            }

            return parent.addInsets(layoutSize(parent, true));
        } finally {
            toolkit.unlockAWT();
        }
    }

    private Dimension layoutSize(Container parent, boolean preferred) {
        int maxWidth = 0;
        int maxHeight = 0;

        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component comp = parent.getComponent(i); 
            Dimension compSize = (preferred ? comp.getPreferredSize() :
                                              comp.getMinimumSize());

            maxWidth = Math.max(maxWidth, compSize.width);
            maxHeight = Math.max(maxHeight, compSize.height);
        }

        return new Dimension(maxWidth + 2 * hGap, maxHeight + 2 * vGap);
    }

}
