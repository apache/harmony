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
 * @author Alexander T. Simbirtsev
 */
package javax.swing.plaf;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;

import org.apache.harmony.x.swing.internal.nls.Messages;

public abstract class ComponentUI {
    public void update(final Graphics graphics, final JComponent component) {
        if (component.isOpaque()) {
            graphics.setColor(component.getBackground());
            graphics.fillRect(0, 0, component.getWidth(), component.getHeight());
        }
        paint(graphics, component);
    }

    public void paint(final Graphics graphics, final JComponent component) {
    }

    public Accessible getAccessibleChild(final JComponent component, final int childIndex) {
        AccessibleContext context = component.getAccessibleContext();
        return (context != null) ? context.getAccessibleChild(childIndex) : null;
    }

    public Dimension getPreferredSize(final JComponent component) {
        return null;
    }

    public Dimension getMinimumSize(final JComponent component) {
        return getPreferredSize(component);
    }

    public Dimension getMaximumSize(final JComponent component) {
        return getPreferredSize(component);
    }

    public boolean contains(final JComponent component, final int x, final int y) {
        return ((0 <= x && x < component.getWidth()) &&
                (0 <= y && y < component.getHeight()));
    }

    public void uninstallUI(final JComponent component) {
    }

    public void installUI(final JComponent component) {
    }

    public int getAccessibleChildrenCount(final JComponent component) {
        AccessibleContext accessibleContext = component.getAccessibleContext();
        return (accessibleContext != null) ? accessibleContext.getAccessibleChildrenCount() : 0;
    }

    public static ComponentUI createUI(final JComponent component) {
        throw new Error(Messages.getString("swing.err.0C", "ComponentUI.createUI")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}


