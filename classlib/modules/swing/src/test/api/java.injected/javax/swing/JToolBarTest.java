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
 * @author Vadim L. Bogdanov
 */
package javax.swing;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;

public class JToolBarTest extends SwingTestCase {
    private static final int INVALID_ORIENTATION = 4;

    private JToolBar toolBar;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        toolBar = new JToolBar();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public JToolBarTest(final String name) {
        super(name);
    }

    public void testGetAccessibleContext() {
        assertTrue(toolBar.getAccessibleContext() instanceof JToolBar.AccessibleJToolBar);
    }

    public void testAddImpl() {
        JButton b = new JButton("b");
        b.setEnabled(true);
        toolBar.addImpl(b, null, 0);
        assertTrue(toolBar.isAncestorOf(b));
        assertTrue(b.isEnabled());
    }

    public void testSetLayout() {
        assertNotNull(toolBar.getLayout());
        FlowLayout layout = new FlowLayout();
        toolBar.setLayout(layout);
        assertSame(layout, toolBar.getLayout());
        toolBar.setLayout(null);
        assertNull(toolBar.getLayout());
    }

    public void testGetUIClassID() {
        assertEquals("ToolBarUI", toolBar.getUIClassID());
    }

    public void testPaintBorder() {
        // Note: painting code, cannot test
    }

    public void testUpdateUI() {
        toolBar.updateUI();
        ComponentUI ui1 = toolBar.getUI();
        ComponentUI ui2 = UIManager.getUI(toolBar);
        // at least names of classes must be the same
        assertEquals(ui2.getClass().getName(), ui1.getClass().getName());
    }

    public void testJToolBar() {
        toolBar = new JToolBar();
        toolBar.setName(null);
        assertEquals(SwingConstants.HORIZONTAL, toolBar.getOrientation());
        assertNull(toolBar.getName());
        assertNotNull(toolBar.getUI());
        assertNotNull(toolBar.getLayout());
    }

    public void testJToolBarint() {
        toolBar = new JToolBar(SwingConstants.VERTICAL);
        assertEquals(SwingConstants.VERTICAL, toolBar.getOrientation());
        assertNull(toolBar.getName());
        assertNotNull(toolBar.getUI());
        assertNotNull(toolBar.getLayout());
        toolBar = new JToolBar(SwingConstants.HORIZONTAL);
        assertEquals(SwingConstants.HORIZONTAL, toolBar.getOrientation());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                toolBar = new JToolBar(INVALID_ORIENTATION);
            }
        });
    }

    public void testJToolBarString() {
        toolBar = new JToolBar("tb1");
        assertEquals("tb1", toolBar.getName());
        assertEquals(SwingConstants.HORIZONTAL, toolBar.getOrientation());
        assertNotNull(toolBar.getUI());
        assertNotNull(toolBar.getLayout());
    }

    public void testJToolBarStringint() {
        toolBar = new JToolBar("tb1", SwingConstants.HORIZONTAL);
        assertEquals("tb1", toolBar.getName());
        assertNotNull(toolBar.getUI());
        assertNotNull(toolBar.getLayout());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                toolBar = new JToolBar("tb2", INVALID_ORIENTATION);
            }
        });
    }

    public void testSetGetUI() {
        BasicToolBarUI ui = new BasicToolBarUI();
        toolBar.setUI(ui);
        assertSame(ui, toolBar.getUI());
    }

    public void testGetComponentIndex() {
        JComponent c1 = new JButton("1");
        toolBar.add(c1);
        JComponent c2 = new JLabel("2");
        toolBar.add(c2);
        toolBar.addSeparator();
        assertEquals(0, toolBar.getComponentIndex(c1));
        assertEquals(1, toolBar.getComponentIndex(c2));
        assertEquals(2, toolBar.getComponentIndex(toolBar.getComponentAtIndex(2)));
        assertEquals(-1, toolBar.getComponentIndex(new JLabel()));
    }

    public void testGetComponentAtIndex() {
        JComponent c1 = new JButton("1");
        toolBar.add(c1);
        JComponent c2 = new JLabel("2");
        toolBar.add(c2);
        toolBar.addSeparator();
        assertSame(c1, toolBar.getComponentAtIndex(0));
        assertSame(c2, toolBar.getComponentAtIndex(1));
        assertTrue(toolBar.getComponentAtIndex(2) instanceof JToolBar.Separator);
        assertNull(toolBar.getComponentAtIndex(toolBar.getComponentCount()));
    }

    public void testSetGetMargin() {
        final Insets defaultMagin = new Insets(0, 0, 0, 0);
        assertEquals(defaultMagin, toolBar.getMargin());
        Insets insets = new Insets(1, 2, 3, 4);
        PropertyChangeController controller = new PropertyChangeController();
        toolBar.addPropertyChangeListener("margin", controller);
        toolBar.setMargin(insets);
        assertTrue(controller.isChanged());
        assertEquals(insets, toolBar.getMargin());
        assertSame(insets, toolBar.getMargin());
        toolBar.setMargin(null);
        assertEquals(defaultMagin, toolBar.getMargin());
    }

    public void testSetIsBorderPainted() {
        assertTrue(toolBar.isBorderPainted());
        PropertyChangeController controller = new PropertyChangeController();
        toolBar.addPropertyChangeListener("borderPainted", controller);
        toolBar.setBorderPainted(false);
        assertTrue(controller.isChanged());
        assertFalse(toolBar.isBorderPainted());
    }

    public void testSetIsFloatable() {
        assertTrue(toolBar.isFloatable());
        PropertyChangeController controller = new PropertyChangeController();
        toolBar.addPropertyChangeListener("floatable", controller);
        toolBar.setFloatable(false);
        assertTrue(controller.isChanged());
        assertFalse(toolBar.isFloatable());
    }

    public void testSetGetOrientation() {
        toolBar.addSeparator();
        PropertyChangeController controller = new PropertyChangeController();
        toolBar.addPropertyChangeListener("orientation", controller);
        assertEquals(SwingConstants.VERTICAL, getToolBarSeparatorAtIndex(0).getOrientation());
        toolBar.setOrientation(SwingConstants.VERTICAL);
        assertTrue(controller.isChanged());
        assertEquals(SwingConstants.HORIZONTAL, getToolBarSeparatorAtIndex(0).getOrientation());
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                toolBar.setOrientation(INVALID_ORIENTATION);
            }
        });
    }

    public void testSetIsRollover() {
        assertFalse(toolBar.isRollover());
        PropertyChangeController controller = new PropertyChangeController();
        toolBar.addPropertyChangeListener("JToolBar.isRollover", controller);
        toolBar.setRollover(true);
        assertTrue(controller.isChanged());
        assertTrue(toolBar.isRollover());
    }

    public void testAddSeparator() {
        toolBar.addSeparator();
        assertEquals(1, toolBar.getComponentCount());
        assertEquals(UIManager.get("ToolBar.separatorSize"), getToolBarSeparatorAtIndex(0)
                .getSeparatorSize());
        assertEquals(SwingConstants.VERTICAL, getToolBarSeparatorAtIndex(0).getOrientation());
        toolBar.setOrientation(SwingConstants.VERTICAL);
        toolBar.addSeparator();
        assertEquals(2, toolBar.getComponentCount());
        assertEquals(SwingConstants.HORIZONTAL, getToolBarSeparatorAtIndex(1).getOrientation());
    }

    public void testAddSeparatorDimension() {
        final Dimension size = new Dimension(5, 10);
        toolBar.addSeparator(size);
        assertEquals(1, toolBar.getComponentCount());
        assertEquals(size, getToolBarSeparatorAtIndex(0).getSeparatorSize());
        toolBar.addSeparator(null);
        assertEquals(UIManager.get("ToolBar.separatorSize"), getToolBarSeparatorAtIndex(1)
                .getSeparatorSize());
    }

    public void testAddAction() {
        AbstractAction action = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            {
                putValue(Action.NAME, "action");
            }

            public void actionPerformed(final ActionEvent e) {
            }
        };
        JButton b = toolBar.add(action);
        assertTrue(toolBar.isAncestorOf(b));
        assertEquals(action.getValue(Action.NAME), b.getText());
        assertSame(action, b.getAction());
        action.setEnabled(false);
        assertFalse(b.isEnabled());
    }

    public void testCreateActionComponent() {
        AbstractAction action = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            {
                putValue(Action.NAME, "action");
            }

            public void actionPerformed(final ActionEvent e) {
            }
        };
        JButton b = toolBar.createActionComponent(action);
        assertEquals(action.getValue(Action.NAME), b.getText());
        assertNull(b.getAction());
        b = toolBar.createActionComponent(null);
        assertEquals("", b.getText());
    }

    public void testCreateActionChangeListener() {
        JButton b = new JButton("b");
        assertNull(toolBar.createActionChangeListener(b));
    }

    private JToolBar.Separator getToolBarSeparatorAtIndex(final int i) {
        return (JToolBar.Separator) toolBar.getComponentAtIndex(i);
    }
}
