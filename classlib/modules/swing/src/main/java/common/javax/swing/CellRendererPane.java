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

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * <p>
 * <i>CellRendererPane</i>
 * </p>
 * <h3>Implementation Notes:</h3>
 * <ul>
 * <li>The <code>serialVersionUID</code> fields are explicitly declared as a performance
 * optimization, not as a guarantee of serialization compatibility.</li>
 * </ul>
 */
public class CellRendererPane extends Container implements Accessible {
    private static final long serialVersionUID = -7642183829532984273L;

    protected AccessibleContext accessibleContext;

    protected class AccessibleCellRendererPane extends AccessibleAWTContainer {
        private static final long serialVersionUID = 1L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PANEL;
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleCellRendererPane();
        }
        return accessibleContext;
    }

    @Override
    public void invalidate() {
    }

    @Override
    public void paint(Graphics g) {
    }

    @Override
    public void update(Graphics g) {
    }

    public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h) {
        paintComponent(g, c, p, x, y, w, h, false);
    }

    public void paintComponent(Graphics g, Component c, Container p, Rectangle r) {
        paintComponent(g, c, p, r.x, r.y, r.width, r.height);
    }

    public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w,
            int h, boolean shouldValidate) {
        if(g == null || c == null) return;
        add(c);
        c.setBounds(x, y, w, h);
        if (shouldValidate) {
            c.validate();
        }
        Graphics newGraphics = g.create(x, y, w, h);
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            boolean isDoubleBuffered = jc.isDoubleBuffered();
            jc.setDoubleBuffered(false);
            jc.paint(jc.getComponentGraphics(newGraphics));
            jc.setDoubleBuffered(isDoubleBuffered);
        } else {
            c.paint(newGraphics);
        }
        newGraphics.dispose();
        c.setBounds(-w, -h, 0, 0);
    }

    @Override
    protected void addImpl(Component c, Object constraints, int index) {
        Component[] components = getComponents();
        for (int i = 0; i < components.length; i++) {
            if (c == components[i]) {
                return;
            }
        }
        super.addImpl(c, constraints, index);
    }
}
