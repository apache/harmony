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
 * @author Sergey Burlak
 */
package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import org.apache.harmony.x.swing.Utilities;


class BasicSplitPaneKeyboardActions {
    private static Action newNegativeIncrementAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                JSplitPane splitPane = (JSplitPane)e.getSource();
                if (splitPane.isFocusOwner()) {
                    splitPane.setDividerLocation(splitPane.getDividerLocation() - 1);
                    splitPane.revalidate();
                }
            }
        };
    }

    private static Action newPositiveIncrementAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                JSplitPane splitPane = (JSplitPane)e.getSource();
                if (splitPane.isFocusOwner()) {
                    splitPane.setDividerLocation(splitPane.getDividerLocation() + 1);
                    splitPane.revalidate();
                }
            }
        };
    }

    private static Action newSelectMinAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                JSplitPane splitPane = (JSplitPane)e.getSource();
                if (splitPane.isFocusOwner()) {
                    int splitPaneSize = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT
                                        ? splitPane.getInsets().left
                                        : splitPane.getInsets().top;
                    splitPane.setDividerLocation(splitPaneSize);
                    splitPane.revalidate();
                }
            }
        };
    }

    private static Action newSelectMaxAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                JSplitPane splitPane = (JSplitPane)e.getSource();
                if (splitPane.isFocusOwner()) {
                    int splitPaneSize = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT
                                            ? splitPane.getWidth() - splitPane.getInsets().right
                                            : splitPane.getHeight() - splitPane.getInsets().bottom;
                    splitPane.setDividerLocation(splitPaneSize - splitPane.getDividerSize());
                    splitPane.revalidate();
                }
            }
        };
    }

    private static Action newToggleFocusAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                JSplitPane splitPane = (JSplitPane)e.getSource();
                isFocusRequested = false;
                if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                    if (splitPane.getLeftComponent() instanceof Container 
                        && isHierarchyFocused((Container)splitPane.getLeftComponent()) 
                        || splitPane.getLeftComponent().isFocusOwner()) {
                        
                        requestFocusInHierarchy((Container)splitPane.getRightComponent());
                    } else {
                        requestFocusInHierarchy((Container)splitPane.getLeftComponent());
                    }
                } else {
                    if (splitPane.getTopComponent() instanceof Container 
                        && isHierarchyFocused((Container)splitPane.getLeftComponent()) 
                        || splitPane.getTopComponent().isFocusOwner()) {

                        requestFocusInHierarchy((Container)splitPane.getBottomComponent());
                    } else {
                        requestFocusInHierarchy((Container)splitPane.getTopComponent());
                    }
                }
            }
        };
    }
    
    private static boolean isHierarchyFocused(final Container root) {
        if (root.isFocusOwner()) {
            return root.isFocusOwner();
        }

        for (int i = 0; i < root.getComponentCount(); i++) {
            Component child = root.getComponent(i);
            if (child instanceof Container) {
                return isHierarchyFocused((Container)child);
            } else {
                return child.isFocusOwner();
            }
        }

        return false;
    }

    private static boolean isFocusRequested;
    private static void requestFocusInHierarchy(final Container root) {
        if (isFocusRequested) {
            return;
        }
        
        if (root.getComponentCount() == 0) {
            root.requestFocus();
            isFocusRequested = true;
            
            return;
        }

        for (int i = 0; i < root.getComponentCount(); i++) {
            Component child = root.getComponent(i);
            if (!(child instanceof JComponent)) {
                root.requestFocus();
                isFocusRequested = true;
                
                return;
            }
            if (child instanceof Container) {
                requestFocusInHierarchy((Container)child);
                if (isFocusRequested) {
                    return;
                }
            }
        }
    }

    private static Action newStartResizeAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                JSplitPane splitPane = (JSplitPane)e.getSource();
                splitPane.requestFocus();
                ((BasicSplitPaneUI)splitPane.getUI()).getDivider().repaint();
            }
        };
    }

    private static Action newFocusOutForwardAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                JSplitPane splitPane = (JSplitPane)e.getSource();
                Container ancestor = splitPane.getFocusCycleRootAncestor();
                if (ancestor == null) {
                    return;
                }
                Component rightComponent = splitPane.getComponentOrientation().isLeftToRight()
                                                     ? splitPane.getRightComponent()
                                                     : splitPane.getLeftComponent();
                Component result = ancestor.getFocusTraversalPolicy().getComponentAfter(ancestor, rightComponent);
                if (result == null) {
                    return;
                }

                result.requestFocus();
            }
        };
    }

    private static Action newFocusOutBackwardAction() {
        return new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                JSplitPane splitPane = (JSplitPane)e.getSource();
                Container ancestor = splitPane.getFocusCycleRootAncestor();
                if (ancestor == null) {
                    return;
                }
                Component leftComponent = splitPane.getComponentOrientation().isLeftToRight()
                                                     ? splitPane.getLeftComponent()
                                                     : splitPane.getRightComponent();
                Component result = ancestor.getFocusTraversalPolicy().getComponentBefore(ancestor, leftComponent);
                if (result == null) {
                    return;
                }

                result.requestFocus();
            }
        };
    }

    public static void installKeyboardActions(final JSplitPane splitPane) {
        Set forwardSet = new HashSet();
        forwardSet.add(KeyStroke.getKeyStroke("pressed TAB"));
        splitPane.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardSet);

        Set backwardSet = new HashSet();
        backwardSet.add(KeyStroke.getKeyStroke("shift pressed TAB"));
        splitPane.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardSet);

        Utilities.installKeyboardActions(splitPane, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "SplitPane.ancestorInputMap", null);

        splitPane.getActionMap().put("negativeIncrement", newNegativeIncrementAction());
        splitPane.getActionMap().put("positiveIncrement", newPositiveIncrementAction());
        splitPane.getActionMap().put("selectMin", newSelectMinAction());
        splitPane.getActionMap().put("selectMax", newSelectMaxAction());
        splitPane.getActionMap().put("startResize", newStartResizeAction());
        splitPane.getActionMap().put("toggleFocus", newToggleFocusAction());
        splitPane.getActionMap().put("focusOutForward", newFocusOutForwardAction());
        splitPane.getActionMap().put("focusOutBackward", newFocusOutBackwardAction());
    }
}
