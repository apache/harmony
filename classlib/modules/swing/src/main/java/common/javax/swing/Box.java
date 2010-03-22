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

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class Box extends JComponent implements Accessible {
    private static final long serialVersionUID = 1525417495883046342L;

    protected class AccessibleBox extends Container.AccessibleAWTContainer {
        private static final long serialVersionUID = -7676166747466316885L;

        protected AccessibleBox() {
            super();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FILLER;
        }
    };

    public static class Filler extends JComponent implements Accessible {
        private static final long serialVersionUID = -1204263191910183998L;

        private Dimension minimumBoxSize;

        private Dimension preferredBoxSize;

        private Dimension maximumBoxSize;

        protected class AccessibleBoxFiller extends Component.AccessibleAWTComponent {
            private static final long serialVersionUID = 2256123275413517188L;

            protected AccessibleBoxFiller() {
                super();
            }

            @Override
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.FILLER;
            }
        };

        protected AccessibleContext accessibleContext;

        public Filler(Dimension minimumSize, Dimension preferredSize, Dimension maximumSize) {
            super();
            minimumBoxSize = minimumSize;
            preferredBoxSize = preferredSize;
            maximumBoxSize = maximumSize;
        }

        public void changeShape(Dimension minimumSize, Dimension preferredSize,
                Dimension maximumSize) {
            minimumBoxSize = minimumSize;
            preferredBoxSize = preferredSize;
            maximumBoxSize = maximumSize;
            invalidate();
        }

        @Override
        public AccessibleContext getAccessibleContext() {
            return (accessibleContext == null) ? (accessibleContext = new AccessibleBoxFiller())
                    : accessibleContext;
        }

        @Override
        public Dimension getPreferredSize() {
            return preferredBoxSize;
        }

        @Override
        public Dimension getMinimumSize() {
            return minimumBoxSize;
        }

        @Override
        public Dimension getMaximumSize() {
            return maximumBoxSize;
        }
    }

    protected AccessibleContext accessibleContext;

    public Box(int axisType) {
        super.setLayout(new BoxLayout(this, axisType));
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        return (accessibleContext == null) ? (accessibleContext = new AccessibleBox())
                : accessibleContext;
    }

    @Override
    public void setLayout(LayoutManager layout) {
        throw new AWTError(Messages.getString("swing.err.01")); //$NON-NLS-1$
    }

    public static Component createRigidArea(Dimension size) {
        size = (size == null ? new Dimension(0, 0) : size);
        
        return new Filler(new Dimension(size), new Dimension(size), new Dimension(size));
    }

    public static Box createVerticalBox() {
        return new Box(BoxLayout.Y_AXIS);
    }

    public static Box createHorizontalBox() {
        return new Box(BoxLayout.X_AXIS);
    }

    public static Component createVerticalStrut(int height) {
        return new Filler(new Dimension(0, height), new Dimension(0, height), new Dimension(
                Short.MAX_VALUE, height));
    }

    public static Component createHorizontalStrut(int width) {
        return new Filler(new Dimension(width, 0), new Dimension(width, 0), new Dimension(
                width, Short.MAX_VALUE));
    }

    public static Component createVerticalGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0,
                Short.MAX_VALUE));
    }

    public static Component createHorizontalGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(
                Short.MAX_VALUE, 0));
    }

    public static Component createGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(
                Short.MAX_VALUE, Short.MAX_VALUE));
    }
}
