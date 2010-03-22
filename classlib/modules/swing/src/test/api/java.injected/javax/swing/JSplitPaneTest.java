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
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.metal.MetalSplitPaneUI;

public class JSplitPaneTest extends SwingTestCase {
    private JSplitPane pane;

    public JSplitPaneTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        pane = new JSplitPane();
        propertyChangeController = new PropertyChangeController();
        pane.addPropertyChangeListener(propertyChangeController);
    }

    @Override
    protected void tearDown() throws Exception {
        pane = null;
    }

    public void testJSplitPane() throws Exception {
        assertTrue(pane.getLeftComponent() instanceof JButton);
        assertTrue(pane.getRightComponent() instanceof JButton);
        assertFalse(pane.isContinuousLayout());
        assertEquals(0, pane.getLastDividerLocation());
        assertEquals(-1, pane.getDividerLocation());
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, pane.getOrientation());
        pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        assertNull(pane.getLeftComponent());
        assertNull(pane.getRightComponent());
        assertFalse(pane.isContinuousLayout());
        assertEquals(0, pane.getLastDividerLocation());
        assertEquals(-1, pane.getDividerLocation());
        assertEquals(JSplitPane.VERTICAL_SPLIT, pane.getOrientation());
        pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, pane.getOrientation());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JSplitPane(2);
            }
        });
        pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        assertTrue(pane.isContinuousLayout());
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, pane.getOrientation());
        pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false);
        assertFalse(pane.isContinuousLayout());
        assertEquals(JSplitPane.VERTICAL_SPLIT, pane.getOrientation());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JSplitPane(-1, true);
            }
        });
        Component left = new JButton();
        Component right = new JButton();
        pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, left, right);
        assertEquals(left, pane.getLeftComponent());
        assertEquals(right, pane.getRightComponent());
        assertFalse(pane.isContinuousLayout());
        assertEquals(JSplitPane.VERTICAL_SPLIT, pane.getOrientation());
        pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, left, right);
        assertEquals(left, pane.getLeftComponent());
        assertEquals(right, pane.getRightComponent());
        assertTrue(pane.isContinuousLayout());
        assertEquals(JSplitPane.VERTICAL_SPLIT, pane.getOrientation());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new JSplitPane(2, true, new JButton(), new JButton());
            }
        });
    }

    public void testGetSetUpdateUI() throws Exception {
        assertNotNull(pane.getUI());
        SplitPaneUI ui = new MetalSplitPaneUI();
        pane.setUI(ui);
        assertEquals(ui, pane.getUI());
        pane.updateUI();
        assertNotSame(ui, pane.getUI());
    }

    public void testGetUIClassID() throws Exception {
        assertEquals("SplitPaneUI", pane.getUIClassID());
    }

    public void testGetSetDividerSize() throws Exception {
        assertEquals(UIManager.getInt("SplitPane.dividerSize"), pane.getDividerSize());
        pane.setDividerSize(20);
        assertEquals(20, pane.getDividerSize());
        assertTrue(propertyChangeController.isChanged("dividerSize"));
    }

    public void testGetSetLeftTopComponent() throws Exception {
        assertTrue(pane.getLeftComponent() instanceof JButton);
        Component left = new JPanel();
        pane.setLeftComponent(left);
        assertEquals(left, pane.getLeftComponent());
        assertEquals(left, pane.getTopComponent());
        assertEquals(left, pane.getComponent(2));
        Component top = new JPanel();
        pane.setTopComponent(top);
        assertEquals(top, pane.getLeftComponent());
        assertEquals(top, pane.getTopComponent());
        assertEquals(top, pane.getComponent(2));
    }

    public void testGetSetRightBottomComponent() throws Exception {
        assertTrue(pane.getRightComponent() instanceof JButton);
        Component right = new JPanel();
        pane.setRightComponent(right);
        assertEquals(right, pane.getRightComponent());
        assertEquals(right, pane.getBottomComponent());
        assertEquals(right, pane.getComponent(2));
        Component bottom = new JPanel();
        pane.setBottomComponent(bottom);
        assertEquals(bottom, pane.getRightComponent());
        assertEquals(bottom, pane.getBottomComponent());
        assertEquals(bottom, pane.getComponent(2));
    }

    public void testIsSetOneTouchExpandable() throws Exception {
        assertFalse(pane.isOneTouchExpandable());
        pane.setOneTouchExpandable(true);
        assertTrue(pane.isOneTouchExpandable());
        assertTrue(propertyChangeController.isChanged("oneTouchExpandable"));
    }

    public void testGetSetLastDividerLocation() throws Exception {
        assertEquals(0, pane.getLastDividerLocation());
        pane.setLastDividerLocation(20);
        assertEquals(20, pane.getLastDividerLocation());
        assertTrue(propertyChangeController.isChanged("lastDividerLocation"));
    }

    public void testGetSetOrientation() throws Exception {
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, pane.getOrientation());
        pane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        assertEquals(JSplitPane.VERTICAL_SPLIT, pane.getOrientation());
        assertTrue(propertyChangeController.isChanged("orientation"));
    }

    public void testIsSetContinuousLayout() throws Exception {
        assertFalse(pane.isContinuousLayout());
        pane.setContinuousLayout(true);
        assertTrue(pane.isContinuousLayout());
        assertTrue(propertyChangeController.isChanged("continuousLayout"));
    }

    public void testGetSetResizeWeight() throws Exception {
        assertEquals(0, 0, pane.getResizeWeight());
        pane.setResizeWeight(0.4);
        assertEquals(0, 0.4, pane.getResizeWeight());
        assertTrue(propertyChangeController.isChanged("resizeWeight"));
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                pane.setResizeWeight(-1);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                pane.setResizeWeight(1.5);
            }
        });
    }

    public void testResetToPreferredSizes() throws Exception {
        if (isHarmony()) {
            ((JComponent) pane.getLeftComponent()).setPreferredSize(new Dimension(100, 50));
            ((JComponent) pane.getRightComponent()).setPreferredSize(new Dimension(100, 50));
            pane.setSize(300, 100);
            pane.setBorder(BorderFactory.createEmptyBorder(10, 20, 30, 40));
            pane.setDividerLocation(40);
            assertEquals(40, pane.getDividerLocation());
            pane.resetToPreferredSizes();
            assertEquals(120, pane.getDividerLocation());
        }
    }

    public void testGetSetDividerLocation() throws Exception {
        assertEquals(-1, pane.getDividerLocation());
        ((JComponent) pane.getLeftComponent()).setPreferredSize(new Dimension(100, 50));
        ((JComponent) pane.getRightComponent()).setPreferredSize(new Dimension(100, 50));
        pane.setSize(300, 100);
        pane.setBorder(BorderFactory.createEmptyBorder(10, 20, 30, 40));
        pane.setDividerLocation(40);
        assertTrue(propertyChangeController.isChanged("dividerLocation"));
        assertTrue(propertyChangeController.isChanged("lastDividerLocation"));
        assertEquals(40, pane.getDividerLocation());
        assertEquals(0, pane.getUI().getDividerLocation(pane));
        pane.getUI().setDividerLocation(pane, 50);
        assertEquals(0, pane.getUI().getDividerLocation(pane));
        assertEquals(40, pane.getDividerLocation());
        pane.getLayout().layoutContainer(pane);
        assertEquals(40, pane.getDividerLocation());
        assertEquals(40, pane.getUI().getDividerLocation(pane));
        pane.setDividerLocation(0.3);
        assertEquals((300 - 10) * 0.3, pane.getDividerLocation(), 0);
        pane.setDividerLocation(0.5);
        assertEquals((300 - 10) * 0.5, pane.getDividerLocation(), 0);
        pane.setDividerLocation(0.6);
        assertEquals((300 - 10) * 0.6, pane.getDividerLocation(), 0);
    }

    public void testGetMinimumMaximumDividerLocation() throws Exception {
        SplitPaneUI ui = new BasicSplitPaneUI() {
            @Override
            public int getMinimumDividerLocation(final JSplitPane sp) {
                return 20;
            }

            @Override
            public int getMaximumDividerLocation(final JSplitPane sp) {
                return 40;
            }
        };
        pane.setUI(ui);
        assertEquals(20, pane.getMinimumDividerLocation());
        assertEquals(40, pane.getMaximumDividerLocation());
    }

    public void testRemove() throws Exception {
        assertNotNull(pane.getLeftComponent());
        pane.remove(pane.getLeftComponent());
        assertNull(pane.getLeftComponent());
        assertNotNull(pane.getRightComponent());
        pane.remove(pane.getRightComponent());
        assertNull(pane.getRightComponent());
        pane = new JSplitPane();
        assertNotNull(pane.getRightComponent());
        pane.remove(1);
        assertNull(pane.getRightComponent());
        assertNotNull(pane.getLeftComponent());
        pane.remove(0);
        assertNull(pane.getLeftComponent());
        pane = new JSplitPane();
        assertNotNull(pane.getLeftComponent());
        assertNotNull(pane.getRightComponent());
        pane.removeAll();
        assertNull(pane.getLeftComponent());
        assertNull(pane.getRightComponent());
    }

    public void testIsValidateRoot() throws Exception {
        assertTrue(pane.isValidateRoot());
    }

    public void testAddImpl() throws Exception {
        pane.removeAll();
        assertEquals(0, pane.getComponentCount());
        Component left = new JButton();
        pane.add(left, JSplitPane.LEFT);
        assertEquals(1, pane.getComponentCount());
        assertEquals(left, pane.getLeftComponent());
        assertEquals(left, pane.getTopComponent());
        Component top = new JButton();
        pane.add(top, JSplitPane.TOP);
        assertEquals(1, pane.getComponentCount());
        assertEquals(top, pane.getLeftComponent());
        assertEquals(top, pane.getTopComponent());
        Component right = new JButton();
        pane.add(right, JSplitPane.RIGHT);
        assertEquals(2, pane.getComponentCount());
        assertEquals(right, pane.getRightComponent());
        assertEquals(right, pane.getBottomComponent());
        Component bottom = new JButton();
        pane.add(bottom, JSplitPane.BOTTOM);
        assertEquals(2, pane.getComponentCount());
        assertEquals(bottom, pane.getRightComponent());
        assertEquals(bottom, pane.getBottomComponent());
        Component divider = new JButton();
        pane.add(divider, JSplitPane.DIVIDER);
        assertEquals(3, pane.getComponentCount());
        pane.removeAll();
        left = new JButton();
        right = new JButton();
        pane.addImpl(right, JSplitPane.RIGHT, 1);
        pane.addImpl(left, JSplitPane.LEFT, 0);
        assertSame(right, pane.getComponent(0));
        assertSame(left, pane.getComponent(1));
        pane.removeAll();
        left = new JButton();
        pane.add(left);
        assertEquals(left, pane.getLeftComponent());
        right = new JButton();
        pane.add(right);
        assertEquals(right, pane.getRightComponent());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                pane.add(new JButton());
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                pane.add(new JButton(), "wrong");
            }
        });
    }

    public void testPaintChildren() throws Exception {
        final Marker m = new Marker();
        SplitPaneUI ui = new BasicSplitPaneUI() {
            @Override
            public void finishedPaintingChildren(final JSplitPane sp, final Graphics g) {
                m.mark();
            }
        };
        pane.setUI(ui);
        pane.paintChildren(createTestGraphics());
        assertTrue(m.isMarked());
    }

    public void testGetAccessibleContext() throws Exception {
        assertTrue(pane.getAccessibleContext() instanceof JSplitPane.AccessibleJSplitPane);
    }

    public void testIsOpaque() throws Exception {
        assertTrue(pane.isOpaque());
    }

    private class Marker {
        private boolean marked;

        public void mark() {
            marked = true;
        }

        public boolean isMarked() {
            return marked;
        }
    }
}
