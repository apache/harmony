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
 * @author Anton Avtamonov
 */

package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.Point;
import java.io.Serializable;
import java.util.Comparator;

import org.apache.harmony.x.swing.internal.nls.Messages;

public class LayoutFocusTraversalPolicy extends SortingFocusTraversalPolicy implements Serializable {
    private static final long serialVersionUID = 3761404205428127289L;

    private final InternalDefaultFocusTraversalPolicy defaultPolicy = new InternalDefaultFocusTraversalPolicy();

    public LayoutFocusTraversalPolicy() {
        setComparator(new LayoutComparator());
    }

    public Component getComponentBefore(final Container focusCycleRoot, final Component component) {
        if (focusCycleRoot == null) {
            throw new IllegalArgumentException(Messages.getString("swing.4E")); //$NON-NLS-1$
        }
        if (component == null) {
            throw new IllegalArgumentException(Messages.getString("swing.4F")); //$NON-NLS-1$
        }
        ((LayoutComparator)getComparator()).setOrientation(focusCycleRoot.getComponentOrientation());
        return super.getComponentBefore(focusCycleRoot, component);
    }

    public Component getComponentAfter(final Container focusCycleRoot, final Component component) {
        if (focusCycleRoot == null) {
            throw new IllegalArgumentException(Messages.getString("swing.4E")); //$NON-NLS-1$
        }
        if (component == null) {
            throw new IllegalArgumentException(Messages.getString("swing.4F")); //$NON-NLS-1$
        }
        ((LayoutComparator)getComparator()).setOrientation(focusCycleRoot.getComponentOrientation());
        return super.getComponentAfter(focusCycleRoot, component);
    }

    public Component getLastComponent(final Container focusCycleRoot) {
        if (focusCycleRoot == null) {
            throw new IllegalArgumentException(Messages.getString("swing.4E")); //$NON-NLS-1$
        }
        ((LayoutComparator)getComparator()).setOrientation(focusCycleRoot.getComponentOrientation());
        return super.getLastComponent(focusCycleRoot);
    }

    public Component getFirstComponent(final Container focusCycleRoot) {
        if (focusCycleRoot == null) {
            throw new IllegalArgumentException(Messages.getString("swing.4E")); //$NON-NLS-1$
        }
        ((LayoutComparator)getComparator()).setOrientation(focusCycleRoot.getComponentOrientation());
        return super.getFirstComponent(focusCycleRoot);
    }


    protected boolean accept(final Component candidate) {
        if (!super.accept(candidate)) {
            return false;
        }

        if (candidate instanceof JMenu
            || candidate instanceof JMenuBar) {

            return false;
        }

        if (candidate instanceof JTable) {
            return true;
        }

        if (candidate instanceof JComboBox) {
            return ((JComboBox)candidate).getUI().isFocusTraversable((JComboBox)candidate);
        }

        if (candidate instanceof JComponent) {
            InputMap whenFocusedMap = ((JComponent)candidate).getInputMap(JComponent.WHEN_FOCUSED, false);

            if (whenFocusedMap != null && whenFocusedMap.allKeys().length > 0) {
                return true;
            }
        }

        return defaultPolicy.accept(candidate);
    }


    // The comparator intention is to order components geometrically.
    // Provided order must be 'natural' - like components appeared on the screen -
    // so that the user traverses components as they are layed out.
    private static class LayoutComparator implements Comparator {
        private ComponentOrientation orientation;

        public void setOrientation(final ComponentOrientation co) {
            orientation = co;
        }

        public int compare(final Object o1, final Object o2) {
            Component c1 = (Component)o1;
            Component c2 = (Component)o2;

            if (orientation.isHorizontal() && orientation.isLeftToRight()) {
                return LTCompare(c1, c2);
            } else if (orientation.isHorizontal() && !orientation.isLeftToRight()) {
                return RTCompare(c1, c2);
            } else if (!orientation.isHorizontal() && orientation.isLeftToRight()) {
                return TLCompare(c1, c2);
            } else if (!orientation.isHorizontal() && !orientation.isLeftToRight()) {
                return TRCompare(c1, c2);
            }

            throw new IllegalStateException(Messages.getString("swing.50")); //$NON-NLS-1$
        }

        private int LTCompare(final Component c1, final Component c2) {
            if (onTheSameRow(c1, c2)) {
                if (onTheSameColumn(c1, c2)) {
                    return 0;
                } else {
                    Point diff = SwingUtilities.convertPoint(c1, 0, 0, c2);
                    return diff.x;
                }
            } else {
                Point diff = SwingUtilities.convertPoint(c1, 0, 0, c2);
                return diff.y;
            }
        }

        private int RTCompare(final Component c1, final Component c2) {
            if (onTheSameRow(c1, c2)) {
                if (onTheSameColumn(c1, c2)) {
                    return 0;
                } else {
                    Point diff = SwingUtilities.convertPoint(c2, 0, 0, c1);
                    return diff.x;
                }
            } else {
                Point diff = SwingUtilities.convertPoint(c1, 0, 0, c2);
                return diff.y;
            }
        }

        private int TLCompare(final Component c1, final Component c2) {
            if (onTheSameColumn(c1, c2)) {
                if (onTheSameRow(c1, c2)) {
                    return 0;
                } else {
                    Point diff = SwingUtilities.convertPoint(c1, 0, 0, c2);
                    return diff.y;
                }
            } else {
                Point diff = SwingUtilities.convertPoint(c1, 0, 0, c2);
                return diff.x;
            }
        }

        private int TRCompare(final Component c1, final Component c2) {
            if (onTheSameColumn(c1, c2)) {
                if (onTheSameRow(c1, c2)) {
                    return 0;
                } else {
                    Point diff = SwingUtilities.convertPoint(c1, 0, 0, c2);
                    return diff.y;
                }
            } else {
                Point diff = SwingUtilities.convertPoint(c2, 0, 0, c1);
                return diff.x;
            }
        }


        private boolean onTheSameRow(final Component c1, final Component c2) {
            if (c1.getSize().getHeight() > c2.getSize().getHeight()) {
                Point diff = SwingUtilities.convertPoint(c2, 0, (int)(c2.getSize().getHeight() / 2), c1);
                if (diff.y >= 0 && diff.y <= c1.getSize().getHeight()) {
                    return true;
                }
            } else {
                Point diff = SwingUtilities.convertPoint(c1, 0, (int)(c1.getSize().getHeight() / 2), c2);
                if (diff.y >= 0 && diff.y <= c2.getSize().getHeight()) {
                    return true;
                }
            }

            return false;
        }

        private boolean onTheSameColumn(final Component c1, final Component c2) {
            if (c1.getSize().getWidth() > c2.getSize().getWidth()) {
                Point diff = SwingUtilities.convertPoint(c2, (int)(c2.getSize().getWidth() / 2), 0, c1);
                if (diff.x >= 0 && diff.x <= c1.getSize().getWidth()) {
                    return true;
                }
            } else {
                Point diff = SwingUtilities.convertPoint(c1, (int)(c1.getSize().getWidth() / 2), 0, c2);
                if (diff.x >= 0 && diff.x <= c2.getSize().getWidth()) {
                    return true;
                }
            }

            return false;
        }
    }

    private class InternalDefaultFocusTraversalPolicy extends DefaultFocusTraversalPolicy {
        public boolean accept(final Component candidate) {
            return super.accept(candidate);
        }
    }
}

