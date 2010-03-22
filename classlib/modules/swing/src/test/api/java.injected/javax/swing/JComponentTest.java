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
 * Created on 02.09.2004
 *
 */
package javax.swing;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EventListener;
import java.util.Locale;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.SwingPropertyChangeSupportTest.ConcreteVetoableChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;

@SuppressWarnings("serial")
public class JComponentTest extends SwingTestCase {
    protected JComponent panel = null;

    protected int find(final Object[] array, final Object value) {
        int found = 0;
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(value)) {
                    found++;
                }
            }
        }
        return found;
    }

    /**
     * Constructor for JComponentTest.
     */
    public JComponentTest(final String str) {
        super(str);
        setIgnoreNotImplemented(true);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        panel = new JComponent() {
            public ComponentUI getUI() {
                return ui;
            }
        };
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        panel = null;
    }

    public void testGetHeight() {
        int height = 100;
        int width = 200;
        panel.setSize(width, height);
        assertEquals(panel.getHeight(), height);
    }

    public void testGetWidth() {
        int height = 100;
        int width = 200;
        panel.setSize(width, height);
        assertEquals(panel.getWidth(), width);
    }

    public void testGetX() {
        int x = 100;
        int y = 200;
        panel.setLocation(x, y);
        assertEquals(panel.getX(), x);
    }

    public void testGetY() {
        int x = 100;
        int y = 200;
        panel.setLocation(x, y);
        assertEquals(panel.getY(), y);
    }

    /*
     * Class under test for boolean contains(int, int)
     */
    public void testContainsintint() {
        int x = 100;
        int y = 200;
        int height = 200;
        int width = 400;
        panel.setBounds(x, y, width, height);
        assertTrue(panel.contains(20, 50));
        assertTrue(panel.contains(width - 20, height - 50));
        assertFalse(panel.contains(width + 20, height - 50));
        assertFalse(panel.contains(width - 20, height + 50));
    }

    public void testReshape() {
        int x = 100;
        int y = 200;
        int height = 200;
        int width = 400;
        panel.setBounds(x, y, width, height);
        assertTrue(panel.getLocation().equals(new Point(x, y)));
        assertTrue(panel.getSize().equals(new Dimension(width, height)));
    }

    public void testIsMaximumSizeSet() {
        assertFalse(panel.isMaximumSizeSet());
        panel.setMaximumSize(new Dimension(100, 200));
        assertTrue(panel.isMaximumSizeSet());
    }

    public void testIsMinimumSizeSet() {
        assertFalse(panel.isMinimumSizeSet());
        panel.setMinimumSize(new Dimension(100, 200));
        assertTrue(panel.isMinimumSizeSet());
    }

    public void testIsPreferredSizeSet() {
        assertFalse(panel.isPreferredSizeSet());
        panel.setPreferredSize(new Dimension(100, 200));
        assertTrue(panel.isPreferredSizeSet());
    }

    /*
     * Class under test for Rectangle getBounds(Rectangle)
     */
    public void testGetBoundsRectangle() {
        Rectangle newBounds = new Rectangle(100, 200, 300, 400);
        panel.setBounds(newBounds);
        Rectangle boundsObtained = new Rectangle();
        boundsObtained = panel.getBounds(boundsObtained);
        assertEquals(newBounds, boundsObtained);
    }

    /*
     * Class under test for Point getLocation(Point)
     */
    public void testGetLocationPoint() {
        Point newLocation = new Point(100, 200);
        panel.setLocation(newLocation);
        Point pointObtained = new Point();
        pointObtained = panel.getLocation(pointObtained);
        assertTrue(pointObtained.equals(newLocation));
    }

    /*
     * Class under test for Insets getInsets()
     */
    public void testGetInsets() {
        Insets insetsObtained = panel.getInsets();
        assertTrue(insetsObtained.equals(new Insets(0, 0, 0, 0)));
        int top = 10;
        int left = 20;
        int bottom = 30;
        int right = 40;
        panel.setBorder(new EmptyBorder(top, left, bottom, right));
        insetsObtained = panel.getInsets();
        assertTrue(insetsObtained.equals(new Insets(top, left, bottom, right)));
    }

    /*
     * Class under test for Insets getInsets(Insets)
     */
    public void testGetInsetsInsets() {
        int top = 10;
        int left = 20;
        int bottom = 30;
        int right = 40;
        Insets insetsForParameter = new Insets(right, bottom, left, top);
        Insets insetsObtained = panel.getInsets(insetsForParameter);
        assertTrue(insetsObtained.equals(new Insets(0, 0, 0, 0)));
        insetsForParameter = new Insets(right, bottom, left, top);
        panel.setBorder(new EmptyBorder(top, left, bottom, right));
        insetsObtained = panel.getInsets(insetsForParameter);
        assertTrue(insetsObtained.equals(new Insets(top, left, bottom, right)));
    }

    /*
     * Class under test for Dimension getSize(Dimension)
     */
    public void testGetSizeDimension() {
        Dimension newSize = new Dimension(100, 100);
        panel.setSize(newSize);
        Dimension sizeObtained = new Dimension();
        sizeObtained = panel.getSize(sizeObtained);
        assertTrue(newSize.equals(sizeObtained));
    }

    /*
     * Class under test for Dimension getMinimumSize()
     */
    public void testGetMinimumSize() {
        panel = new JLayeredPane();
        assertEquals(new Dimension(0, 0), panel.getMinimumSize());
        panel = new JPanel();
        panel.setPreferredSize(new Dimension(100, 200));
        //        assertEquals(new Dimension(10 ,10), panel.getMinimumSize());
        panel = new JPanel();
        panel.setMinimumSize(new Dimension(100, 200));
        assertEquals(new Dimension(100, 200), panel.getMinimumSize());
        //        if (InternalTests.isIncluded) {
        //            assertTrue(panel.getMinimumSize() != panel.getMinimumSize());
        //        }
    }

    /*
     * Class under test for void setMinimumSize(Dimension)
     */
    public void testSetMinimumSizeDimension() {
        final Dimension minimumSize1 = new Dimension(100, 200);
        final Dimension minimumSize2 = new Dimension(200, 300);
        class MinimumListener implements PropertyChangeListener {
            boolean caught = false;

            public void propertyChange(final PropertyChangeEvent evt) {
                assertTrue("minimumSize".equals(evt.getPropertyName()));
                assertTrue(minimumSize1.equals(evt.getOldValue()));
                assertTrue(minimumSize2.equals(evt.getNewValue()));
                caught = true;
            }
        }
        ;
        panel.setMinimumSize(minimumSize1);
        assertTrue(panel.getMinimumSize().equals(minimumSize1));
        MinimumListener listener = new MinimumListener();
        panel.addPropertyChangeListener(listener);
        panel.setMinimumSize(minimumSize2);
        assertTrue(listener.caught);
        assertTrue(panel.getMinimumSize().equals(minimumSize2));
    }

    /*
     * Class under test for Dimension getMaximumSize()
     */
    public void testGetMaximumSize() {
        panel = new JPanel();
        panel.setPreferredSize(new Dimension(100, 200));
        assertEquals(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE), panel.getMaximumSize());
        panel = new JPanel();
        panel.setMaximumSize(new Dimension(100, 200));
        assertEquals(new Dimension(100, 200), panel.getMaximumSize());
        //        if (InternalTests.isIncluded) {
        //            assertTrue(panel.getMaximumSize() != panel.getMaximumSize());
        //        }
    }

    /*
     * Class under test for void setMaximumSize(Dimension)
     */
    public void testSetMaximumSizeDimension() {
        final Dimension maximumSize1 = new Dimension(100, 200);
        final Dimension maximumSize2 = new Dimension(200, 300);
        class MaximumListener implements PropertyChangeListener {
            boolean caught = false;

            public void propertyChange(final PropertyChangeEvent evt) {
                assertTrue("maximumSize".equals(evt.getPropertyName()));
                assertTrue(maximumSize1.equals(evt.getOldValue()));
                assertTrue(maximumSize2.equals(evt.getNewValue()));
                caught = true;
            }
        }
        ;
        panel.setMaximumSize(maximumSize1);
        assertTrue(maximumSize1.equals(panel.getMaximumSize()));
        MaximumListener listener = new MaximumListener();
        panel.addPropertyChangeListener(listener);
        panel.setMaximumSize(maximumSize2);
        assertTrue(listener.caught);
        assertTrue(maximumSize2.equals(panel.getMaximumSize()));
    }

    /*
     * Class under test for Dimension getPreferredSize()
     */
    public void testGetPreferredSize() {
        final Dimension preferredSize1 = new Dimension(100, 200);
        final Dimension preferredSize2 = new Dimension(200, 300);
        panel.setPreferredSize(preferredSize1);
        assertTrue(preferredSize1.equals(panel.getPreferredSize()));
        panel.setPreferredSize(preferredSize2);
        assertTrue(preferredSize2.equals(panel.getPreferredSize()));
        panel = new JLayeredPane();
        assertEquals(new Dimension(0, 0), panel.getPreferredSize());
        panel.setPreferredSize(new Dimension(100, 200));
        assertEquals(new Dimension(100, 200), panel.getPreferredSize());
        //        if (InternalTests.isIncluded) {
        //            assertTrue(panel.getPreferredSize() != panel.getPreferredSize());
        //        }
    }

    /*
     * Class under test for void setPreferredSize(Dimension)
     */
    public void testSetPreferredSizeDimension() {
        final Dimension preferredSize1 = new Dimension(100, 200);
        final Dimension preferredSize2 = new Dimension(200, 300);
        class PreferredListener implements PropertyChangeListener {
            boolean caught = false;

            public void propertyChange(final PropertyChangeEvent evt) {
                assertTrue("preferredSize".equals(evt.getPropertyName()));
                assertTrue(preferredSize1.equals(evt.getOldValue()));
                assertTrue(preferredSize2.equals(evt.getNewValue()));
                caught = true;
            }
        }
        ;
        panel.setPreferredSize(preferredSize1);
        assertTrue(preferredSize1.equals(panel.getPreferredSize()));
        PreferredListener listener = new PreferredListener();
        panel.addPropertyChangeListener(listener);
        panel.setPreferredSize(preferredSize2);
        assertTrue(listener.caught);
        assertTrue(preferredSize2.equals(panel.getPreferredSize()));
        panel = new JLayeredPane();
        assertEquals(new Dimension(0, 0), panel.getPreferredSize());
        panel.setPreferredSize(new Dimension(100, 200));
        assertEquals(new Dimension(100, 200), panel.getPreferredSize());
    }

    @SuppressWarnings("deprecation")
    public void testComputeVisibleRect() {
        JWindow window = new JWindow();
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        panel1.setPreferredSize(new Dimension(100, 100));
        panel2.setPreferredSize(new Dimension(100, 100));
        panel3.setPreferredSize(new Dimension(100, 100));
        panel1.setMaximumSize(new Dimension(100, 100));
        panel2.setMaximumSize(new Dimension(100, 100));
        panel3.setMaximumSize(new Dimension(100, 100));
        panel1.setMinimumSize(new Dimension(100, 100));
        panel2.setMinimumSize(new Dimension(100, 100));
        panel3.setMinimumSize(new Dimension(100, 100));
        panel.setPreferredSize(new Dimension(300, 300));
        panel1.setBackground(Color.BLACK);
        panel2.setBackground(Color.RED);
        panel3.setBackground(Color.YELLOW);
        panel.setLayout(new OverlayLayout(panel));
        panel1.setAlignmentX(1.0f);
        panel2.setAlignmentX(0.3f);
        panel3.setAlignmentX(0.0f);
        panel1.setAlignmentY(1.0f);
        panel2.setAlignmentY(0.3f);
        panel3.setAlignmentY(0.0f);
        panel.add(panel1);
        panel.add(panel2);
        panel.add(panel3);
        window.setSize(150, 150);
        panel.setBorder(new EmptyBorder(10, 20, 30, 40));
        window.getContentPane().add(panel);
        window.show();
        Rectangle rect = new Rectangle();
        panel.computeVisibleRect(rect);
        assertEquals("visible rectangle ", new Rectangle(0, 0, 150, 150), rect);
        panel1.computeVisibleRect(rect);
        assertEquals("visible rectangle ", new Rectangle(0, 0, 45, 55), rect);
        panel2.computeVisibleRect(rect);
        assertEquals("visible rectangle ", new Rectangle(0, 0, 75, 85), rect);
        panel3.computeVisibleRect(rect);
        assertEquals("visible rectangle ", new Rectangle(0, 0, 45, 55), rect);
    }

    @SuppressWarnings("deprecation")
    public void testGetVisibleRect() {
        JWindow window = new JWindow();
        JWindow window2 = new JWindow(window);
        JComponent panel1 = new JPanel();
        Container panel2 = new Panel();
        JComponent panel3 = new JPanel();
        JComponent panel4 = new JPanel();
        panel1.setPreferredSize(new Dimension(100, 100));
        panel2.setSize(new Dimension(110, 110));
        panel3.setPreferredSize(new Dimension(120, 120));
        panel1.setMaximumSize(new Dimension(100, 100));
        panel3.setMaximumSize(new Dimension(120, 120));
        panel1.setMinimumSize(new Dimension(100, 100));
        panel3.setMinimumSize(new Dimension(120, 120));
        panel.setPreferredSize(new Dimension(300, 300));
        panel1.setBackground(Color.BLACK);
        panel2.setBackground(Color.RED);
        panel3.setBackground(Color.YELLOW);
        panel.setLayout(new OverlayLayout(panel));
        panel1.setAlignmentX(1.0f);
        panel3.setAlignmentX(0.0f);
        panel1.setAlignmentY(1.0f);
        panel3.setAlignmentY(0.0f);
        panel.add(panel1);
        panel1.add(panel2);
        panel2.add(panel3);
        window.setSize(150, 150);
        panel.setBorder(new EmptyBorder(10, 20, 30, 40));
        window.getContentPane().add(panel);
        window.setVisible(true);
        window2.setBounds(500, 500, 100, 110);
        window2.getContentPane().add(panel4);
        window2.show();
        assertEquals("visible rectangle ", new Rectangle(150, 150), panel.getVisibleRect());
        assertEquals("visible rectangle ", new Rectangle(90, 100), panel1.getVisibleRect());
        assertEquals("visible rectangle ", new Rectangle(15, 0, 90, 90), panel3
                .getVisibleRect());
        assertEquals(new Rectangle(100, 110), panel4.getVisibleRect());
        window.setVisible(false);
        window.setSize(10, 10);
        window.pack();
        assertEquals("visible rectangle ", new Rectangle(300, 300), panel.getVisibleRect());
        assertEquals("visible rectangle ", new Rectangle(100, 100), panel1.getVisibleRect());
        assertEquals("visible rectangle ", new Rectangle(10, 0, 100, 90), panel3
                .getVisibleRect());
    }

    class ActionListenerDummy implements ActionListener {
        public ActionEvent eventHeard = null;

        String str;

        public ActionListenerDummy() {
        }

        public ActionListenerDummy(String s) {
            str = s;
        }

        public void actionPerformed(final ActionEvent e) {
            eventHeard = e;
        }
    };

    /*
     * Class under test for void processKeyEvent(KeyEvent)
     */
    public void testProcessKeyEventKeyEvent() {
        ActionListenerDummy action1 = new ActionListenerDummy("1");
        ActionListenerDummy action2 = new ActionListenerDummy("2");
        ActionListenerDummy action3 = new ActionListenerDummy("3");
        ActionListenerDummy action41 = new ActionListenerDummy("41");
        ActionListenerDummy action43 = new ActionListenerDummy("42");
        ActionListenerDummy action51 = new ActionListenerDummy("51");
        ActionListenerDummy action52 = new ActionListenerDummy("52");
        ActionListenerDummy action53 = new ActionListenerDummy("53");
        ActionListenerDummy action54 = new ActionListenerDummy("54");
        JComponent component1 = new JPanel();
        Component component2 = new Panel();
        JComponent component3 = new JButton("3");
        JWindow component4 = new JWindow();
        component4.getContentPane().add(component1);
        component1.add(component2);
        component1.add(component3);
        KeyEvent event11 = new KeyEvent(component1, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_A,
                'a');
        KeyEvent event22 = new KeyEvent(component2, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_B,
                'b');
        KeyEvent event33 = new KeyEvent(component3, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_C,
                'c');
        KeyEvent event42 = new KeyEvent(component2, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_D,
                'd');
        KeyEvent event51 = new KeyEvent(component1, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_ENTER, '\n');
        KeyStroke keyStroke1 = KeyStroke.getKeyStrokeForEvent(event11);
        KeyStroke keyStroke2 = KeyStroke.getKeyStrokeForEvent(event22);
        KeyStroke keyStroke3 = KeyStroke.getKeyStrokeForEvent(event33);
        KeyStroke keyStroke4 = KeyStroke.getKeyStrokeForEvent(event42);
        KeyStroke keyStroke5 = KeyStroke.getKeyStrokeForEvent(event51);
        component1.registerKeyboardAction(action1, keyStroke1, JComponent.WHEN_FOCUSED);
        component1.registerKeyboardAction(action2, keyStroke2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        component3.registerKeyboardAction(action3, keyStroke3,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        component1.registerKeyboardAction(action41, keyStroke4,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        component3.registerKeyboardAction(action43, keyStroke4,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        component3.registerKeyboardAction(action43, keyStroke4,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        component3.registerKeyboardAction(action53, keyStroke5, JComponent.WHEN_FOCUSED);
        component1.registerKeyboardAction(action51, keyStroke5,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        //        component3.processKeyEvent(event1);
        //        assertFalse(event1.isConsumed());
        //
        //        event1 = new KeyEvent(component1, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_A, 'a');
        //        new JPanel().processKeyEvent(event1);
        //        assertFalse(event1.isConsumed());
        component1.processKeyEvent(event11);
        assertTrue("event1: actionPerformed called for component", action1.eventHeard != null);
        assertTrue(event11.isConsumed());
        action1.eventHeard = null;
        component3.processKeyEvent(event22);
        assertNull("event2: wrong actionPerformed called for parent", action1.eventHeard);
        assertTrue("event2: right actionPerformed called for parent",
                action2.eventHeard != null);
        assertTrue(event22.isConsumed());
        action2.eventHeard = null;
        component3.processKeyEvent(event33);
        assertNull("event3: actionPerformed called for parent", action1.eventHeard);
        assertNull("event3: actionPerformed called for brother", action2.eventHeard);
        assertTrue("event3: actionPerformed called for component", action3.eventHeard != null);
        assertTrue(event33.isConsumed());
        action3.eventHeard = null;
        component3.processKeyEvent(event42);
        assertNull("event4: actionPerformed called for parent", action1.eventHeard);
        assertNull("event4: actionPerformed called for brother", action2.eventHeard);
        assertNull("event4: actionPerformed called for component", action3.eventHeard);
        assertNull("event4: actionPerformed called for brother", action41.eventHeard);
        assertTrue("event4: actionPerformed called for brother", action43.eventHeard != null);
        assertTrue(event42.isConsumed());
        component3.processKeyEvent(event51);
        assertNull("event5: actionPerformed called for parent", action51.eventHeard);
        assertTrue("event5: actionPerformed called for parent", action53.eventHeard != null);
        assertTrue(event51.isConsumed());
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JTextField editor = new JTextField();
        KeyEvent event6 = new KeyEvent(editor, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ENTER,
                '\n');
        panel1.registerKeyboardAction(action52, keyStroke5,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel2.registerKeyboardAction(action54, keyStroke5,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel2.add(panel1);
        panel1.add(editor);
        action52.eventHeard = null;
        action54.eventHeard = null;
        panel2.processKeyEvent(event6);
        assertNull("event6: actionPerformed called for parent", action52.eventHeard);
        assertTrue("event6: actionPerformed called for parent", action54.eventHeard != null);
        assertTrue(event6.isConsumed());
        action52.eventHeard = null;
        action54.eventHeard = null;
        event6 = new KeyEvent(editor, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ENTER, '\n');
        panel1.processKeyEvent(event6);
        assertTrue("event6: actionPerformed called for parent", action52.eventHeard != null);
        assertNull("event6: actionPerformed called for parent", action54.eventHeard);
        assertTrue(event6.isConsumed());
    }

    public void testProcessKeyEvent2() {
        ActionListenerDummy action1 = new ActionListenerDummy();
        JComponent container = new JPanel();
        JComponent button = new JButton();
        JTextField editor = new JTextField();
        KeyEvent event1 = new KeyEvent(editor, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ENTER,
                '\n');
        KeyEvent event2 = new KeyEvent(editor, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED,
                '\n');
        KeyEvent event3 = new KeyEvent(editor, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_ENTER,
                '\n');
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        button.registerKeyboardAction(action1, keyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        container.add(button);
        container.add(editor);
        Action action = (Action) editor.getActionForKeyStroke(keyStroke);
        assertFalse(action.isEnabled());
        editor.processKeyEvent(event1);
        assertFalse(event1.isConsumed());
        editor.processKeyEvent(event2);
        assertTrue(event2.isConsumed());
        editor.processKeyEvent(event3);
        assertFalse(event3.isConsumed());
        editor.addActionListener(action1);
        action = (Action) editor.getActionForKeyStroke(keyStroke);
        assertFalse(action.isEnabled());
        editor.processKeyEvent(event1);
        assertFalse(event1.isConsumed());
    }

    /*
     * Class under test for String getUIClassID()
     */
    public void testGetUIClassID() {
        JComponent component = new JComponent() {
        };
        assertEquals("ComponentUI", component.getUIClassID());
    }

    /*
     * Class under test for void setUI(ComponentUI)
     */
    public void testSetUI() {
        ComponentUI componentUI1 = new BasicPanelUI();
        ComponentUI componentUI2 = new BasicPanelUI();
        panel = new JPanel();
        ((JPanel) panel).getUI().uninstallUI(panel);
        panel.setUI(null);
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        panel.setUI(componentUI1);
        assertEquals(componentUI1, panel.ui);
        changeListener.checkPropertyFired(panel, "UI", null, componentUI1);
        changeListener.reset();
        panel.setUI(componentUI2);
        changeListener.checkPropertyFired(panel, "UI", componentUI1, componentUI2);
        changeListener.reset();
        panel.setUI(componentUI2);
        assertFalse(changeListener.isChanged("UI"));
    }

    public void testSetVisible() {
        panel.setVisible(false);
        assertFalse(panel.isVisible());
        panel.setVisible(true);
        assertTrue(panel.isVisible());
        panel.setVisible(false);
        assertFalse(panel.isVisible());
    }

    public void testSetEnabled() {
        panel.setEnabled(true);
        class PropertyChangeListenerFalse implements PropertyChangeListener {
            public boolean isChanged = false;

            public void propertyChange(final PropertyChangeEvent evt) {
                if ("enabled".equals(evt.getPropertyName()) && (evt.getNewValue() != null)) {
                    assertTrue(evt.getNewValue().equals(Boolean.FALSE));
                    isChanged = true;
                }
            }
        }
        ;
        class PropertyChangeListenerTrue implements PropertyChangeListener {
            public void propertyChange(final PropertyChangeEvent evt) {
                if ("enabled".equals(evt.getPropertyName()) && (evt.getNewValue() != null)) {
                    assertTrue(evt.getNewValue().equals(Boolean.TRUE));
                }
            }
        }
        ;
        PropertyChangeListenerFalse changeListenerFalse = new PropertyChangeListenerFalse();
        PropertyChangeListenerTrue changeListenerTrue = new PropertyChangeListenerTrue();
        panel.addPropertyChangeListener(changeListenerFalse);
        panel.setEnabled(false);
        assertFalse(panel.isEnabled());
        assertTrue(changeListenerFalse.isChanged);
        panel.removePropertyChangeListener(changeListenerFalse);
        panel.addPropertyChangeListener(changeListenerTrue);
        panel.setEnabled(true);
        assertTrue(panel.isEnabled());
        assertTrue(changeListenerFalse.isChanged);
        changeListenerFalse.isChanged = false;
        panel.setEnabled(true);
        assertTrue(panel.isEnabled());
        assertFalse(changeListenerFalse.isChanged);
    }

    public void testSetOpaque() {
        panel.setOpaque(false);
        assertFalse("now JPanel is not opaque ", panel.isOpaque());
        panel = new JRootPane();
        panel.setOpaque(true);
        assertTrue("now JRootPane is opaque ", panel.isOpaque());
    }

    public void testIsOpaque() {
        assertFalse("JComponent isn't opaque ", panel.isOpaque());
        panel = new JPanel();
        assertTrue("JPanel is opaque ", panel.isOpaque());
        panel = new JRootPane();
        assertFalse("JRootPane is not opaque ", panel.isOpaque());
    }

    public void testSetBorder() {
        final Border border1 = BorderFactory.createEmptyBorder(30, 30, 30, 30);
        final Border border2 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        class PropertyChangeListenerBorder1 implements PropertyChangeListener {
            public boolean isChanged = false;

            public void propertyChange(final PropertyChangeEvent evt) {
                if ("border".equals(evt.getPropertyName()) && (evt.getNewValue() != null)) {
                    assertTrue(evt.getNewValue().equals(border1));
                    isChanged = true;
                }
            }
        }
        ;
        class PropertyChangeListenerBorder2 implements PropertyChangeListener {
            public boolean isChanged = false;

            public void propertyChange(final PropertyChangeEvent evt) {
                if ("border".equals(evt.getPropertyName()) && (evt.getNewValue() != null)) {
                    assertTrue(evt.getNewValue().equals(border2));
                    isChanged = true;
                }
            }
        }
        ;
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        assertNull(panel.getBorder());
        Border newBorder1 = new EmptyBorder(10, 10, 10, 10);
        Border newBorder2 = new EmptyBorder(20, 20, 20, 20);
        panel.setBorder(newBorder1);
        assertSame(newBorder1, panel.getBorder());
        changeListener.checkPropertyFired(panel, "border", null, newBorder1);
        changeListener.reset();
        panel.setBorder(newBorder2);
        assertSame(newBorder2, panel.getBorder());
        changeListener.checkPropertyFired(panel, "border", newBorder1, newBorder2);
        changeListener.reset();
        panel.setBorder(newBorder2);
        assertTrue(panel.getBorder() == newBorder2);
        assertFalse(changeListener.isChanged());
        changeListener.reset();
        panel.setBorder(null);
        PropertyChangeListenerBorder1 changeListener1 = new PropertyChangeListenerBorder1();
        PropertyChangeListenerBorder2 changeListener2 = new PropertyChangeListenerBorder2();
        panel.addPropertyChangeListener(changeListener1);
        panel.setBorder(border1);
        assertTrue(panel.getBorder().equals(border1));
        assertTrue(changeListener1.isChanged);
        panel.removePropertyChangeListener(changeListener1);
        panel.addPropertyChangeListener(changeListener2);
        panel.setBorder(border2);
        assertTrue(panel.getBorder().equals(border2));
        assertTrue(changeListener2.isChanged);
        changeListener2.isChanged = false;
        panel.setBorder(border2);
        assertTrue(panel.getBorder().equals(border2));
        assertFalse(changeListener2.isChanged);
    }

    public void testGetBorder() {
        assertNull(panel.getBorder());
    }

    public void testGetAlignmentX() {
        assertEquals("alignment ", panel.getAlignmentX(), 0.5f, 1e-5);
    }

    public void testSetAlignmentX() {
        float value = 0.111f;
        panel.setAlignmentX(value);
        assertEquals("alignment ", value, panel.getAlignmentX(), 1e-5);
        value = 2.5f;
        panel.setAlignmentX(value);
        assertEquals("alignment ", 1.0f, panel.getAlignmentX(), 1e-5);
        value = -2.5f;
        panel.setAlignmentX(value);
        assertEquals("alignment ", 0.0f, panel.getAlignmentX(), 1e-5);
    }

    public void testGetAlignmentY() {
        assertEquals("alignment ", panel.getAlignmentY(), 0.5f, 1e-5);
    }

    public void testSetAlignmentY() {
        float value = 0.111f;
        panel.setAlignmentY(value);
        assertEquals("alignment ", value, panel.getAlignmentY(), 1e-5);
        value = 2.5f;
        panel.setAlignmentY(value);
        assertEquals("alignment ", 1.0f, panel.getAlignmentY(), 1e-5);
        value = -2.5f;
        panel.setAlignmentY(value);
        assertEquals("alignment ", 0.0f, panel.getAlignmentY(), 1e-5);
    }

    /*
     * Class under test for void enable()
     */
    public void testEnable() {
        panel.setEnabled(true);
        assertTrue("panel is enabled now ", panel.isEnabled());
        panel.setEnabled(false);
        assertFalse("panel is disabled now ", panel.isEnabled());
        panel.setEnabled(true);
        assertTrue("panel is enabled now ", panel.isEnabled());
    }

    public void testDisable() {
        panel.setEnabled(false);
        assertFalse("panel is disabled now ", panel.isEnabled());
        panel.setEnabled(true);
        assertTrue("panel is enabled now ", panel.isEnabled());
        panel.setEnabled(false);
        assertFalse("panel is disabled now ", panel.isEnabled());
    }

    public void testIsDoubleBuffered() {
        assertFalse("JComponent isn't DoubleBuffered", panel.isDoubleBuffered());
        panel = new JPanel();
        assertTrue("JPanel is DoubleBuffered", panel.isDoubleBuffered());
        panel = new JRootPane();
        assertTrue("JRootPane is DoubleBuffered", panel.isDoubleBuffered());
    }

    public void testFireVetoableChange() throws PropertyVetoException {
        ConcreteVetoableChangeListener changeListener = new ConcreteVetoableChangeListener();
        VetoableChangeListener[] listenersArray = null;
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray != null && listenersArray.length == 0);
        panel.addVetoableChangeListener(changeListener);
        JPanel button1 = new JPanel();
        JPanel button2 = new JPanel();
        panel.fireVetoableChange("button", button1, button2);
        assertTrue("button".equals(changeListener.valueChangedKey));
        assertTrue(button1.equals(changeListener.valueChangedOld));
        assertTrue(button2.equals(changeListener.valueChangedNew));
    }

    /*
     * Class under test for void firePropertyChange(String, short, short)
     */
    public void testFirePropertyChangeStringshortshort() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        short oldValue = 100;
        short newValue = 200;
        panel.firePropertyChange("short", oldValue, newValue);
        changeListener.checkPropertyFired(panel, "short", new Short(oldValue), new Short(
                newValue));
        changeListener.reset();
        panel.firePropertyChange("short", (short) 1, (short) 1);
        assertFalse(changeListener.isChanged());
    }

    /*
     * Class under test for void firePropertyChange(String, long, long)
     */
    public void testFirePropertyChangeStringlonglong() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        long oldValue = 100;
        long newValue = 200;
        panel.firePropertyChange("long", oldValue, newValue);
        changeListener.checkLastPropertyFired(panel, "long", new Long(oldValue), new Long(
                newValue));
        changeListener.reset();
        panel.firePropertyChange("int", 1L, 1L);
        assertFalse(changeListener.isChanged());
    }

    /*
     * Class under test for void firePropertyChange(String, int, int)
     */
    public void testFirePropertyChangeStringintint() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        int oldValue = 100;
        int newValue = 200;
        panel.firePropertyChange("int", oldValue, newValue);
        changeListener.checkLastPropertyFired(panel, "int", new Integer(oldValue), new Integer(
                newValue));
        changeListener.reset();
        panel.firePropertyChange("int", 1, 1);
        assertFalse(changeListener.isChanged());
    }

    /*
     * Class under test for void firePropertyChange(String, float, float)
     */
    public void testFirePropertyChangeStringfloatfloat() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        float oldValue = 100.01f;
        float newValue = 200.01f;
        panel.firePropertyChange("float", oldValue, newValue);
        changeListener.checkLastPropertyFired(panel, "float", new Float(oldValue), new Float(
                newValue));
        changeListener.reset();
        panel.firePropertyChange("float", 1.0f, 1.0f);
        assertFalse(changeListener.isChanged());
    }

    /*
     * Class under test for void firePropertyChange(String, double, double)
     */
    public void testFirePropertyChangeStringdoubledouble() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        double oldValue = 100.1;
        double newValue = 200.1;
        panel.firePropertyChange("double", oldValue, newValue);
        changeListener.checkLastPropertyFired(panel, "double", new Double(oldValue),
                new Double(newValue));
        changeListener.reset();
        panel.firePropertyChange("double", 1.0, 1);
        assertFalse(changeListener.isChanged());
    }

    /*
     * Class under test for void firePropertyChange(String, char, char)
     */
    public void testFirePropertyChangeStringcharchar() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        char oldValue = 'a';
        char newValue = 'b';
        panel.firePropertyChange("char", oldValue, newValue);
        changeListener.checkLastPropertyFired(panel, "char", new Character(oldValue),
                new Character(newValue));
        changeListener.reset();
        panel.firePropertyChange("char", 'h', 'h');
        assertFalse(changeListener.isChanged());
    }

    /*
     * Class under test for void firePropertyChange(String, byte, byte)
     */
    public void testFirePropertyChangeStringbytebyte() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        byte oldValue = 66;
        byte newValue = 13;
        panel.firePropertyChange("byte", oldValue, newValue);
        changeListener.checkLastPropertyFired(panel, "byte", new Byte(oldValue), new Byte(
                newValue));
        changeListener.reset();
        panel.firePropertyChange("byte", (byte) 2, (byte) 2);
        assertFalse(changeListener.isChanged());
    }

    /*
     * Class under test for EventListener[] getListeners(Class)
     */
    public void testGetListenersClass1() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        class ConcreteAncestorListener implements AncestorListener {
            public void ancestorAdded(final AncestorEvent event) {
            }

            public void ancestorMoved(final AncestorEvent event) {
            }

            public void ancestorRemoved(final AncestorEvent event) {
            }
        }
        ;
        AncestorListener ancestorListener1 = new ConcreteAncestorListener();
        AncestorListener ancestorListener2 = new ConcreteAncestorListener();
        VetoableChangeListener vetoableChangeListener = new VetoableChangeListener() {
            public void vetoableChange(final PropertyChangeEvent evt) {
            }
        };
        EventListener[] listenersArray = null;
        removeListeners(panel, PropertyChangeListener.class);
        listenersArray = panel.getListeners(VetoableChangeListener.class);
        assertEquals(0, listenersArray.length);
        listenersArray = panel.getListeners(AncestorListener.class);
        assertEquals(0, listenersArray.length);
        panel.addPropertyChangeListener(changeListener1);
        panel.addVetoableChangeListener(vetoableChangeListener);
        panel.addPropertyChangeListener(changeListener2);
        panel.addPropertyChangeListener(changeListener2);
        listenersArray = panel.getListeners(PropertyChangeListener.class);
        assertEquals(3, listenersArray.length);
        panel.removePropertyChangeListener(changeListener2);
        panel.removePropertyChangeListener(changeListener2);
        listenersArray = panel.getListeners(PropertyChangeListener.class);
        assertEquals(1, listenersArray.length);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        panel.addAncestorListener(ancestorListener1);
        panel.addAncestorListener(ancestorListener2);
        listenersArray = panel.getListeners(PropertyChangeListener.class);
        assertEquals(1, listenersArray.length);
        listenersArray = panel.getListeners(VetoableChangeListener.class);
        assertEquals(1, listenersArray.length);
        listenersArray = panel.getListeners(AncestorListener.class);
        assertEquals(2, listenersArray.length);
        panel.removeVetoableChangeListener(vetoableChangeListener);
        listenersArray = panel.getListeners(VetoableChangeListener.class);
        assertTrue(listenersArray.length == 0);
        panel.addAncestorListener(ancestorListener2);
        assertEquals("MouseListeners", 0, panel.getListeners(MouseListener.class).length);
        panel.addMouseListener(new MouseAdapter() {
        });
        assertEquals("MouseListeners", 1, panel.getListeners(MouseListener.class).length);
    }

    /*
     * Class under test for EventListener[] getListeners(Class)
     */
    public void testGetListenersClass2() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        EventListener[] listenersArray = null;
        int initialNumber = panel.getListeners(PropertyChangeListener.class).length;
        panel.addPropertyChangeListener(changeListener1);
        panel.addPropertyChangeListener(changeListener2);
        panel.addPropertyChangeListener(changeListener2);
        panel.addPropertyChangeListener("first", changeListener2);
        panel.addPropertyChangeListener("first", changeListener2);
        panel.addPropertyChangeListener("first", changeListener1);
        panel.addPropertyChangeListener("second", changeListener1);
        listenersArray = panel.getListeners(PropertyChangeListener.class);
        assertEquals(7 + initialNumber, listenersArray.length);
        panel.removePropertyChangeListener(changeListener2);
        panel.removePropertyChangeListener(changeListener2);
        panel.removePropertyChangeListener(changeListener2);
        listenersArray = panel.getListeners(PropertyChangeListener.class);
        assertEquals(5 + initialNumber, listenersArray.length);
        panel.removePropertyChangeListener("second", changeListener2);
        panel.removePropertyChangeListener("first", changeListener2);
        panel.removePropertyChangeListener("first", changeListener2);
        listenersArray = panel.getListeners(PropertyChangeListener.class);
        assertEquals(3 + initialNumber, listenersArray.length);
    }

    public void testPutClientProperty() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        Object value1 = "property1";
        Object value2 = "property2";
        String key1 = "value1";
        assertNull(panel.getClientProperty(key1));
        assertFalse("event's not fired", changeListener.isChanged());
        panel.putClientProperty(key1, value1);
        changeListener.checkLastPropertyFired(panel, key1, null, value1);
        assertTrue(panel.getClientProperty(key1).equals(value1));
        changeListener.reset();
        panel.putClientProperty(key1, value2);
        changeListener.checkLastPropertyFired(panel, key1, value1, value2);
        assertTrue(panel.getClientProperty(key1).equals(value2));
        changeListener.reset();
        panel.putClientProperty(key1, null);
        changeListener.checkLastPropertyFired(panel, key1, value2, null);
        assertNull(panel.getClientProperty(key1));
        changeListener.reset();

        try {         
            JComponent jc = new JComponent() {}; 
            jc.putClientProperty(null, new Object());
            fail("NPE should be thrown");               
        } catch (NullPointerException npe) {               
            // PASSED            
        }
    }

    /*
     * Class under test for void removePropertyChangeListener(PropertyChangeListener)
     */
    public void testRemovePropertyChangeListenerPropertyChangeListener() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeController changeListener3 = new PropertyChangeController();
        PropertyChangeController changeListener4 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        int initialNumber = panel.getPropertyChangeListeners().length;
        listenersArray = panel.getPropertyChangeListeners("first");
        assertTrue(listenersArray.length == 0);
        listenersArray = panel.getPropertyChangeListeners("second");
        assertTrue(listenersArray.length == 0);
        panel.addPropertyChangeListener(changeListener1);
        panel.addPropertyChangeListener("first", changeListener2);
        listenersArray = panel.getPropertyChangeListeners("first");
        assertEquals(1, listenersArray.length);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 2, listenersArray.length);
        panel.removePropertyChangeListener("first", changeListener1);
        listenersArray = panel.getPropertyChangeListeners("first");
        assertEquals(1, listenersArray.length);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 2, listenersArray.length);
        panel.removePropertyChangeListener(changeListener1);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 1, listenersArray.length);
        assertFalse(changeListener1.findMe(listenersArray) > 0);
        panel.removePropertyChangeListener(changeListener2);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 1, listenersArray.length);
        panel.removePropertyChangeListener("first", changeListener2);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber, listenersArray.length);
        panel.addPropertyChangeListener("first", changeListener1);
        panel.addPropertyChangeListener(changeListener2);
        panel.addPropertyChangeListener("second", changeListener3);
        panel.addPropertyChangeListener(changeListener4);
        panel.removePropertyChangeListener("asd", null);
        listenersArray = panel.getPropertyChangeListeners("second");
        assertEquals(1, listenersArray.length);
        panel.removePropertyChangeListener(changeListener1);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 4, listenersArray.length);
        panel.removePropertyChangeListener(changeListener2);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 3, listenersArray.length);
        panel.removePropertyChangeListener(changeListener4);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 2, listenersArray.length);
        panel.removePropertyChangeListener(changeListener3);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 2, listenersArray.length);
    }

    /*
     * Class under test for void addPropertyChangeListener(PropertyChangeListener)
     */
    public void testAddPropertyChangeListenerPropertyChangeListener() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        panel.addPropertyChangeListener(changeListener1);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(0, panel.listenerList.getListeners(PropertyChangeListener.class).length);
        panel.addPropertyChangeListener(changeListener2);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(1, changeListener2.findMe(listenersArray));
        panel.addPropertyChangeListener(changeListener2);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(1, changeListener1.findMe(listenersArray));
        assertEquals(2, changeListener2.findMe(listenersArray));
    }

    /*
     * Class under test for PropertyChangeListener[] getPropertyChangeListeners()
     */
    public void testGetPropertyChangeListeners() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        listenersArray = panel.getPropertyChangeListeners();
        int initialNumber = listenersArray.length;
        panel.addPropertyChangeListener(changeListener1);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 1, listenersArray.length);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        panel.addPropertyChangeListener(changeListener2);
        listenersArray = panel.getPropertyChangeListeners();
        assertEquals(initialNumber + 2, listenersArray.length);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertTrue(changeListener2.findMe(listenersArray) == 1);
    }

    /*
     * Class under test for void removePropertyChangeListener(String, PropertyChangeListener)
     */
    public void testRemovePropertyChangeListenerStringPropertyChangeListener() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeController changeListener3 = new PropertyChangeController();
        PropertyChangeController changeListener4 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        listenersArray = panel.getPropertyChangeListeners("first");
        assertTrue(listenersArray.length == 0);
        listenersArray = panel.getPropertyChangeListeners("second");
        assertTrue(listenersArray.length == 0);
        panel.addPropertyChangeListener("first", changeListener1);
        panel.addPropertyChangeListener("first", changeListener2);
        listenersArray = panel.getPropertyChangeListeners("first");
        assertTrue(listenersArray.length == 2);
        panel.removePropertyChangeListener("first", changeListener2);
        listenersArray = panel.getPropertyChangeListeners("first");
        assertTrue(listenersArray.length == 1);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertFalse(changeListener2.findMe(listenersArray) > 0);
        panel.removePropertyChangeListener("first", changeListener1);
        listenersArray = panel.getPropertyChangeListeners("first");
        assertTrue(listenersArray.length == 0);
        assertFalse(changeListener1.findMe(listenersArray) > 0);
        assertFalse(changeListener2.findMe(listenersArray) > 0);
        panel.addPropertyChangeListener("second", changeListener2);
        panel.addPropertyChangeListener("second", changeListener3);
        panel.addPropertyChangeListener("second", changeListener4);
        listenersArray = panel.getPropertyChangeListeners("second");
        assertTrue(listenersArray.length == 3);
        panel.removePropertyChangeListener("second", changeListener3);
        listenersArray = panel.getPropertyChangeListeners("second");
        assertTrue(listenersArray.length == 2);
        assertTrue(changeListener2.findMe(listenersArray) == 1);
        assertFalse(changeListener3.findMe(listenersArray) > 0);
        assertTrue(changeListener4.findMe(listenersArray) == 1);
        panel.removePropertyChangeListener("second", changeListener2);
        panel.addPropertyChangeListener("second", changeListener3);
        listenersArray = panel.getPropertyChangeListeners("second");
        assertTrue(listenersArray.length == 2);
        assertFalse(changeListener2.findMe(listenersArray) > 0);
        assertTrue(changeListener3.findMe(listenersArray) == 1);
        assertTrue(changeListener4.findMe(listenersArray) == 1);
    }

    /*
     * Class under test for void addPropertyChangeListener(String, PropertyChangeListener)
     */
    public void testAddPropertyChangeListenerStringPropertyChangeListener()
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        panel.addPropertyChangeListener("first", changeListener1);
        panel.addPropertyChangeListener("second", changeListener2);
        String oldValue = "old";
        String newValue = "new";
        Method method = Component.class.getDeclaredMethod("firePropertyChange", new Class[] {
                String.class, Object.class, Object.class });
        if (method != null) {
            method.setAccessible(true);
        } else {
            fail("access error");
        }
        method.invoke(panel, new Object[] { "first", oldValue, newValue });
        changeListener1.checkLastPropertyFired(panel, "first", oldValue, newValue);
        assertFalse("event's not fired", changeListener2.isChanged());
        changeListener1.reset();
        changeListener2.reset();
        method.invoke(panel, new Object[] { "second", oldValue, newValue });
        changeListener2.checkLastPropertyFired(panel, "second", oldValue, newValue);
        assertFalse("event's not fired", changeListener1.isChanged());
        changeListener1.reset();
        changeListener2.reset();
        panel.addPropertyChangeListener("first", changeListener2);
        method.invoke(panel, new Object[] { "first", oldValue, newValue });
        changeListener1.checkLastPropertyFired(panel, "first", oldValue, newValue);
        changeListener2.checkLastPropertyFired(panel, "first", oldValue, newValue);
    }

    /*
     * Class under test for PropertyChangeListener[] getPropertyChangeListeners(String)
     */
    public void testGetPropertyChangeListenersString() {
        PropertyChangeController changeListener1 = new PropertyChangeController();
        PropertyChangeController changeListener2 = new PropertyChangeController();
        PropertyChangeController changeListener3 = new PropertyChangeController();
        PropertyChangeController changeListener4 = new PropertyChangeController();
        PropertyChangeListener[] listenersArray = null;
        listenersArray = panel.getPropertyChangeListeners("first");
        assertTrue(listenersArray.length == 0);
        listenersArray = panel.getPropertyChangeListeners("second");
        assertTrue(listenersArray.length == 0);
        panel.addPropertyChangeListener("first", changeListener1);
        panel.addPropertyChangeListener("first", changeListener2);
        listenersArray = panel.getPropertyChangeListeners("first");
        assertTrue(listenersArray.length == 2);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertTrue(changeListener2.findMe(listenersArray) == 1);
        panel.addPropertyChangeListener("second", changeListener2);
        panel.addPropertyChangeListener("second", changeListener3);
        panel.addPropertyChangeListener("second", changeListener4);
        listenersArray = panel.getPropertyChangeListeners("second");
        assertTrue(listenersArray.length == 3);
        assertTrue(changeListener2.findMe(listenersArray) == 1);
        assertTrue(changeListener3.findMe(listenersArray) == 1);
        assertTrue(changeListener4.findMe(listenersArray) == 1);
        listenersArray = panel.getPropertyChangeListeners("first");
        assertTrue(listenersArray.length == 2);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertTrue(changeListener2.findMe(listenersArray) == 1);
    }

    public void testGetComponentGraphics() {
        JWindow window = new JWindow();
        window.setVisible(true);
        Graphics g = window.getGraphics();
        Graphics componentGraphics = null;
        Font font = new Font(null, Font.BOLD, 10);
        Color color = Color.RED;
        panel.setFont(font);
        panel.setForeground(color);
        componentGraphics = panel.getComponentGraphics(g);
        assertTrue("graphics object's created ", componentGraphics != null);
        assertEquals("graphics object's created properly", font, componentGraphics.getFont());
        assertEquals("graphics object's created properly", color, componentGraphics.getColor());
        panel.setDebugGraphicsOptions(DebugGraphics.FLASH_OPTION);
        componentGraphics = panel.getComponentGraphics(g);
        assertTrue("graphics object's created ", componentGraphics != null);
        if (isHarmony()) {
            assertTrue("Debug graphics is created", componentGraphics instanceof DebugGraphics);
        }
        assertEquals("graphics object's created properly", font, componentGraphics.getFont());
        assertEquals("graphics object's created properly", color, componentGraphics.getColor());
    }

    public void testRemoveAncestorListener() {
        class ConcreteAncestorListener implements AncestorListener {
            public void ancestorAdded(final AncestorEvent event) {
            }

            public void ancestorMoved(final AncestorEvent event) {
            }

            public void ancestorRemoved(final AncestorEvent event) {
            }
        }
        ;
        AncestorListener ancestorListener1 = new ConcreteAncestorListener();
        AncestorListener ancestorListener2 = new ConcreteAncestorListener();
        AncestorListener ancestorListener3 = new ConcreteAncestorListener();
        EventListener[] listenersArray = null;
        listenersArray = panel.getAncestorListeners();
        assertTrue(listenersArray.length == 0);
        panel.addAncestorListener(ancestorListener1);
        panel.addAncestorListener(ancestorListener2);
        panel.addAncestorListener(ancestorListener2);
        listenersArray = panel.getAncestorListeners();
        assertTrue(listenersArray.length == 3);
        panel.removeAncestorListener(ancestorListener1);
        panel.addAncestorListener(ancestorListener3);
        panel.addAncestorListener(ancestorListener3);
        listenersArray = panel.getAncestorListeners();
        assertTrue(listenersArray.length == 4);
        panel.removeAncestorListener(ancestorListener3);
        panel.removeAncestorListener(ancestorListener3);
        listenersArray = panel.getAncestorListeners();
        assertTrue(listenersArray.length == 2);
        panel.removeAncestorListener(ancestorListener2);
        panel.removeAncestorListener(ancestorListener2);
        listenersArray = panel.getAncestorListeners();
        assertTrue(listenersArray.length == 0);
    }

    // this function is tested by testGetAncestorListeners() and
    //                            testRemoveAncestorListener()
    public void testAddAncestorListener() {
    }

    public void testGetAncestorListeners() {
        class ConcreteAncestorListener implements AncestorListener {
            public void ancestorAdded(final AncestorEvent event) {
            }

            public void ancestorMoved(final AncestorEvent event) {
            }

            public void ancestorRemoved(final AncestorEvent event) {
            }
        }
        ;
        AncestorListener ancestorListener1 = new ConcreteAncestorListener();
        AncestorListener ancestorListener2 = new ConcreteAncestorListener();
        AncestorListener ancestorListener3 = new ConcreteAncestorListener();
        EventListener[] listenersArray = null;
        listenersArray = panel.getListeners(AncestorListener.class);
        assertTrue(listenersArray.length == 0);
        listenersArray = panel.getAncestorListeners();
        assertTrue(listenersArray.length == 0);
        panel.addAncestorListener(ancestorListener1);
        panel.addAncestorListener(ancestorListener2);
        listenersArray = panel.getListeners(AncestorListener.class);
        assertTrue(listenersArray.length == 2);
        listenersArray = panel.getAncestorListeners();
        assertTrue(listenersArray.length == 2);
        panel.addAncestorListener(ancestorListener3);
        panel.addAncestorListener(ancestorListener2);
        listenersArray = panel.getListeners(AncestorListener.class);
        assertTrue(listenersArray.length == 4);
        listenersArray = panel.getAncestorListeners();
        assertTrue(listenersArray.length == 4);
    }

    public void testSetTransferHandler() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        TransferHandler handler1 = new TransferHandler("1");
        TransferHandler handler2 = new TransferHandler("2");
        panel.setTransferHandler(handler1);
        changeListener.checkLastPropertyFired(panel, "transferHandler", null, handler1);
        assertEquals("transferHandler's set properly ", handler1, panel.getTransferHandler());
        changeListener.reset();
        panel.setTransferHandler(handler2);
        changeListener.checkLastPropertyFired(panel, "transferHandler", handler1, handler2);
        assertEquals("transferHandler's set properly ", handler2, panel.getTransferHandler());
        changeListener.reset();
        panel.setTransferHandler(handler2);
        assertFalse("event's not fired", changeListener.isChanged());
        assertEquals("transferHandler's set properly ", handler2, panel.getTransferHandler());
        changeListener.reset();
        System.setProperty("suppressSwingDropSupport", "false");
        panel1.setTransferHandler(handler1);
        assertNotNull("DropTarget is installed ", panel1.getDropTarget());
        assertEquals("DropTarget is installed properly ",
                panel1.getDropTarget().getComponent(), panel1);
        assertEquals("transferHandler's set properly ", handler1, panel1.getTransferHandler());
        System.setProperty("suppressSwingDropSupport", "true");
        panel2.setTransferHandler(handler2);
        if (isHarmony()) {
            assertNull("DropTarget is not installed ", panel2.getDropTarget());
        } else {
            assertNotNull("DropTarget is installed ", panel2.getDropTarget());
            assertEquals("transferHandler's set properly ", handler2, panel2
                    .getTransferHandler());
        }
    }

    public void testGetTransferHandler() {
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JButton();
        TransferHandler handler1 = new TransferHandler("1");
        TransferHandler handler2 = new TransferHandler("1");
        assertNull("transferHandler is not set by default ", panel.getTransferHandler());
        panel1.setTransferHandler(handler1);
        assertEquals("transferHandler's set properly ", handler1, panel1.getTransferHandler());
        panel2.setTransferHandler(handler2);
        assertEquals("transferHandler's set properly ", handler2, panel2.getTransferHandler());
    }

    /*
     * Class under test for void registerKeyboardAction(ActionListener, String, KeyStroke, int)
     */
    public void testRegisterKeyboardActionActionListenerStringKeyStrokeint() {
        ActionListener action1 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action2 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action3 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        String command1 = "new";
        String command2 = "delete";
        String command3 = "show";
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,
                InputEvent.SHIFT_DOWN_MASK);
        panel.registerKeyboardAction(action3, command3, keyStroke1,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getActionForKeyStroke(keyStroke1) == action3);
        panel.registerKeyboardAction(action1, command2, keyStroke1, JComponent.WHEN_FOCUSED);
        assertTrue(panel.getActionForKeyStroke(keyStroke1) == action1);
        panel.registerKeyboardAction(action1, command1, keyStroke2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.registerKeyboardAction(action2, command2, keyStroke3,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getActionForKeyStroke(keyStroke2) == action1);
        assertTrue(panel.getActionForKeyStroke(keyStroke3) == action2);
        panel.registerKeyboardAction(action3, command3, keyStroke3,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getActionForKeyStroke(keyStroke3) == action3);
        panel.registerKeyboardAction(null, command3, keyStroke3, JComponent.WHEN_FOCUSED);
        assertNull(panel.getActionForKeyStroke(keyStroke3));
    }

    public void testProcessKeyBinding() throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        ActionListenerDummy action1 = new ActionListenerDummy();
        ActionListenerDummy action2 = new ActionListenerDummy();
        ActionListenerDummy action3 = new ActionListenerDummy();
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,
                InputEvent.SHIFT_DOWN_MASK);
        KeyStroke keyStroke4 = KeyStroke.getKeyStroke(KeyEvent.VK_X, 0);
        KeyEvent keyEvent1 = new KeyEvent(panel, 1, 10, 0, 100, '1');
        KeyEvent keyEvent2 = new KeyEvent(panel, 2, 20, 0, 200, '2');
        KeyEvent keyEvent3 = new KeyEvent(panel, 3, 30, 0, 300, '3');
        KeyEvent keyEvent4 = new KeyEvent(panel, 3, 30, 0, 300, '3');
        String command1 = "command1";
        String command2 = "command2";
        String command3 = null;
        boolean result = false;
        panel.registerKeyboardAction(action1, command1, keyStroke1, JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(action2, command2, keyStroke2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.registerKeyboardAction(action3, command3, keyStroke3,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        result = panel.processKeyBinding(keyStroke1, keyEvent1, JComponent.WHEN_FOCUSED, true);
        assertTrue(action1.eventHeard != null
                && action1.eventHeard.getActionCommand().equals(command1));
        assertTrue(result);
        result = panel.processKeyBinding(keyStroke2, keyEvent2, JComponent.WHEN_FOCUSED, true);
        assertNull(action2.eventHeard);
        assertFalse(result);
        result = panel.processKeyBinding(keyStroke2, keyEvent2,
                JComponent.WHEN_IN_FOCUSED_WINDOW, false);
        assertNull(action2.eventHeard);
        assertFalse(result);
        result = panel.processKeyBinding(keyStroke2, keyEvent2,
                JComponent.WHEN_IN_FOCUSED_WINDOW, true);
        assertNull(action2.eventHeard);
        assertFalse(result);
        result = panel.processKeyBinding(keyStroke2, keyEvent2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, false);
        assertTrue(action2.eventHeard != null
                && action2.eventHeard.getActionCommand() == command2);
        assertTrue(result);
        result = panel.processKeyBinding(keyStroke2, keyEvent2,
                JComponent.WHEN_IN_FOCUSED_WINDOW, true);
        assertTrue(action2.eventHeard != null
                && action2.eventHeard.getActionCommand() == command2);
        assertFalse(result);
        result = panel.processKeyBinding(keyStroke3, keyEvent3,
                JComponent.WHEN_IN_FOCUSED_WINDOW, true);
        assertTrue(action3.eventHeard != null
                && action3.eventHeard.getActionCommand() == command3);
        assertTrue(result);
        result = panel.processKeyBinding(keyStroke3, keyEvent3,
                JComponent.WHEN_IN_FOCUSED_WINDOW, true);
        assertTrue(action3.eventHeard != null
                && action3.eventHeard.getActionCommand() == command3);
        assertTrue(result);
        Action action = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke4, "asdasda");
        panel.getActionMap().put("asdasda", action);
        action.setEnabled(false);
        result = panel.processKeyBinding(keyStroke4, keyEvent4,
                JComponent.WHEN_IN_FOCUSED_WINDOW, true);
        assertFalse(result);
        action.setEnabled(true);
        panel.setEnabled(false);
        result = panel.processKeyBinding(keyStroke4, keyEvent4,
                JComponent.WHEN_IN_FOCUSED_WINDOW, true);
        assertFalse(result);
        panel.setEnabled(true);
        panel.setVisible(false);
        result = panel.processKeyBinding(keyStroke4, keyEvent4,
                JComponent.WHEN_IN_FOCUSED_WINDOW, true);
        assertTrue(result);
    }

    public void testGetActionForKeyStroke() {
        ActionListener action1 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action2 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action3 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,
                InputEvent.SHIFT_DOWN_MASK);
        panel.registerKeyboardAction(action3, keyStroke1, JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getActionForKeyStroke(keyStroke1) == action3);
        panel.registerKeyboardAction(action1, keyStroke1, JComponent.WHEN_FOCUSED);
        assertTrue(panel.getActionForKeyStroke(keyStroke1) == action1);
        panel.registerKeyboardAction(action1, keyStroke2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.registerKeyboardAction(action2, keyStroke3, JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getActionForKeyStroke(keyStroke2) == action1);
        assertTrue(panel.getActionForKeyStroke(keyStroke3) == action2);
        panel.registerKeyboardAction(action3, keyStroke3, JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getActionForKeyStroke(keyStroke3) == action3);
        panel.registerKeyboardAction(null, keyStroke3, JComponent.WHEN_FOCUSED);
        assertNull(panel.getActionForKeyStroke(keyStroke3));
    }

    /*
     * Class under test for void registerKeyboardAction(ActionListener, KeyStroke, int)
     * this method is being tested by testGetActionForKeyStroke()
     */
    public void testRegisterKeyboardActionActionListenerKeyStrokeint() {
    }

    public void testUnregisterKeyboardAction() {
        ActionListener action1 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action2 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action3 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,
                InputEvent.SHIFT_DOWN_MASK);
        panel.registerKeyboardAction(action1, keyStroke1, JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(action2, keyStroke2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.registerKeyboardAction(action3, keyStroke3, JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getActionForKeyStroke(keyStroke1) == action1);
        assertTrue(panel.getActionForKeyStroke(keyStroke2) == action2);
        assertTrue(panel.getActionForKeyStroke(keyStroke3) == action3);
        panel.unregisterKeyboardAction(keyStroke1);
        assertNull(panel.getActionForKeyStroke(keyStroke1));
        assertTrue(panel.getActionForKeyStroke(keyStroke2) == action2);
        assertTrue(panel.getActionForKeyStroke(keyStroke3) == action3);
        panel.unregisterKeyboardAction(keyStroke2);
        assertNull(panel.getActionForKeyStroke(keyStroke1));
        assertNull(panel.getActionForKeyStroke(keyStroke2));
        assertTrue(panel.getActionForKeyStroke(keyStroke3) == action3);
        panel.unregisterKeyboardAction(keyStroke3);
        assertNull(panel.getActionForKeyStroke(keyStroke1));
        assertNull(panel.getActionForKeyStroke(keyStroke2));
        assertNull(panel.getActionForKeyStroke(keyStroke3));
        panel.registerKeyboardAction(action1, keyStroke1, JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(action2, keyStroke1,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.unregisterKeyboardAction(keyStroke1);
        assertNull(panel.getActionForKeyStroke(keyStroke1));
        assertTrue(panel.getActionMap().size() == 0);
    }

    public void testGetConditionForKeyStroke() {
        ActionListener action1 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action2 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action3 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_ASTERISK, 0);
        assertTrue(panel.getConditionForKeyStroke(keyStroke1) == JComponent.UNDEFINED_CONDITION);
        assertTrue(panel.getConditionForKeyStroke(keyStroke2) == JComponent.UNDEFINED_CONDITION);
        panel.registerKeyboardAction(action1, keyStroke1, JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getConditionForKeyStroke(keyStroke1) == JComponent.WHEN_IN_FOCUSED_WINDOW);
        panel.registerKeyboardAction(action1, keyStroke1,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        assertTrue(panel.getConditionForKeyStroke(keyStroke1) == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.registerKeyboardAction(action1, keyStroke1, JComponent.WHEN_FOCUSED);
        assertTrue(panel.getConditionForKeyStroke(keyStroke1) == JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(action2, keyStroke2, JComponent.WHEN_FOCUSED);
        assertTrue(panel.getConditionForKeyStroke(keyStroke2) == JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(action2, keyStroke2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        assertTrue(panel.getConditionForKeyStroke(keyStroke2) == JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(action2, keyStroke2, JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getConditionForKeyStroke(keyStroke2) == JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(action3, keyStroke3,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        assertTrue(panel.getConditionForKeyStroke(keyStroke3) == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.registerKeyboardAction(action3, keyStroke3, JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getConditionForKeyStroke(keyStroke3) == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.registerKeyboardAction(action3, keyStroke3, JComponent.WHEN_FOCUSED);
        assertTrue(panel.getConditionForKeyStroke(keyStroke3) == JComponent.WHEN_FOCUSED);
    }

    public void testGetRegisteredKeyStrokes() {
        ActionListener action1 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action2 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action3 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,
                InputEvent.SHIFT_DOWN_MASK);
        KeyStroke keyStroke4 = KeyStroke.getKeyStroke(KeyEvent.VK_0, 0);
        KeyStroke[] strokes = panel.getRegisteredKeyStrokes();
        assertTrue(strokes != null && strokes.length == 0);
        panel.registerKeyboardAction(action3, keyStroke1, JComponent.WHEN_IN_FOCUSED_WINDOW);
        panel.registerKeyboardAction(action1, keyStroke1, JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(action1, keyStroke2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.registerKeyboardAction(action2, keyStroke3, JComponent.WHEN_IN_FOCUSED_WINDOW);
        strokes = panel.getRegisteredKeyStrokes();
        assertTrue(strokes != null && strokes.length == 4);
        assertTrue(find(strokes, keyStroke1) == 2);
        assertTrue(find(strokes, keyStroke2) == 1);
        assertTrue(find(strokes, keyStroke3) == 1);
        panel.registerKeyboardAction(action3, keyStroke3, JComponent.WHEN_IN_FOCUSED_WINDOW);
        strokes = panel.getRegisteredKeyStrokes();
        assertTrue(strokes != null && strokes.length == 4);
        assertTrue(find(strokes, keyStroke1) == 2);
        assertTrue(find(strokes, keyStroke2) == 1);
        assertTrue(find(strokes, keyStroke3) == 1);
        panel.registerKeyboardAction(null, keyStroke3, JComponent.WHEN_FOCUSED);
        strokes = panel.getRegisteredKeyStrokes();
        assertTrue(strokes != null && strokes.length == 5);
        assertTrue(find(strokes, keyStroke1) == 2);
        assertTrue(find(strokes, keyStroke2) == 1);
        assertTrue(find(strokes, keyStroke3) == 2);
        InputMap parentMap = new InputMap();
        parentMap.put(keyStroke4, keyStroke4);
        panel.getInputMap().setParent(parentMap);
        strokes = panel.getRegisteredKeyStrokes();
        assertTrue("KeyStrokes array is not null", strokes != null);
        assertEquals("Number of keyStrokes registered", 6, strokes.length);
        assertEquals("Number of keyStrokes registered", 2, find(strokes, keyStroke1));
        assertEquals("Number of keyStrokes registered", 1, find(strokes, keyStroke2));
        assertEquals("Number of keyStrokes registered", 2, find(strokes, keyStroke3));
        assertEquals("Number of keyStrokes registered", 1, find(strokes, keyStroke4));
    }

    public void testResetKeyboardActions() {
        ActionListener action1 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action2 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        ActionListener action3 = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            }
        };
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
        KeyStroke keyStroke2 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
        KeyStroke keyStroke3 = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,
                InputEvent.SHIFT_DOWN_MASK);
        panel.registerKeyboardAction(action1, keyStroke1, JComponent.WHEN_FOCUSED);
        panel.registerKeyboardAction(action2, keyStroke2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        panel.registerKeyboardAction(action3, keyStroke3, JComponent.WHEN_IN_FOCUSED_WINDOW);
        assertTrue(panel.getActionForKeyStroke(keyStroke1) == action1);
        assertTrue(panel.getActionForKeyStroke(keyStroke2) == action2);
        assertTrue(panel.getActionForKeyStroke(keyStroke3) == action3);
        panel.resetKeyboardActions();
        assertNull(panel.getActionForKeyStroke(keyStroke1));
        assertNull(panel.getActionForKeyStroke(keyStroke2));
        assertNull(panel.getActionForKeyStroke(keyStroke3));
    }

    public void testGetRootPane() {
        JFrame mainFrame = new JFrame();
        JRootPane rootPane = new JRootPane();
        assertNull(panel.getRootPane());
        mainFrame.getContentPane().add(panel);
        assertTrue(panel.getRootPane() != null);
        assertTrue(panel.getRootPane() == mainFrame.getRootPane());
        rootPane.getContentPane().add(panel);
        assertTrue(panel.getRootPane() != null);
        assertTrue(panel.getRootPane() == rootPane);
        rootPane.getContentPane().remove(panel);
        assertNull(panel.getRootPane());
        mainFrame.dispose();
    }

    public void testSetInputVerifier() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        InputVerifier verifier1 = new InputVerifier() {
            @Override
            public boolean verify(final JComponent input) {
                return false;
            }
        };
        InputVerifier verifier2 = new InputVerifier() {
            @Override
            public boolean verify(final JComponent input) {
                return false;
            }
        };
        panel.setInputVerifier(verifier1);
        changeListener.checkPropertyFired(panel, "inputVerifier", null, verifier1);
        changeListener.reset();
        panel.setInputVerifier(verifier2);
        changeListener.checkPropertyFired(panel, "inputVerifier", verifier1, verifier2);
        changeListener.reset();
        panel.setInputVerifier(verifier2);
        assertFalse("event's not fired", changeListener.isChanged());
        changeListener.reset();
    }

    public void testGetInputVerifier() {
        InputVerifier verifier1 = new InputVerifier() {
            @Override
            public boolean verify(final JComponent input) {
                return false;
            }
        };
        InputVerifier verifier2 = new InputVerifier() {
            @Override
            public boolean verify(final JComponent input) {
                return false;
            }
        };
        assertNull("There's not inputVerifier set by default", panel.getInputVerifier());
        panel.setInputVerifier(verifier1);
        assertEquals("InputVerifier set properly: ", verifier1, panel.getInputVerifier());
        panel.setInputVerifier(verifier2);
        assertEquals("InputVerifier set properly: ", verifier2, panel.getInputVerifier());
    }

    /*
     * Class under test for InputMap setInputMap()
     */
    public void testSetInputMap() {
        InputMap map1 = new InputMap();
        InputMap map2 = new InputMap();
        InputMap map3 = new ComponentInputMap(panel);
        panel.setInputMap(JComponent.WHEN_FOCUSED, map1);
        panel.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map2);
        panel.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, map3);
        assertTrue(panel.getInputMap(JComponent.WHEN_FOCUSED) == map1);
        assertTrue(panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) == map2);
        assertTrue(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW) == map3);
        panel.setInputMap(JComponent.WHEN_FOCUSED, map2);
        panel.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map3);
        assertTrue(panel.getInputMap(JComponent.WHEN_FOCUSED) == map2);
        assertTrue(panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) == map3);
        panel.setInputMap(JComponent.WHEN_FOCUSED, null);
        assertNull(panel.getInputMap(JComponent.WHEN_FOCUSED));
        boolean isThrown = false;
        try {
            panel.setInputMap(1945, map2);
        } catch (IllegalArgumentException e) {
            isThrown = true;
        }
        assertTrue("Exception is thrown", isThrown);
        isThrown = false;
        try {
            panel.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, map1);
        } catch (IllegalArgumentException e) {
            isThrown = true;
        }
        assertTrue("Exception is thrown", isThrown);
        if (isHarmony()) {
            isThrown = false;
            try {
                panel.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, null);
            } catch (IllegalArgumentException e) {
                isThrown = true;
            }
            assertTrue("Exception is thrown", isThrown);
        }
    }

    /*
     * Class under test for InputMap getInputMap(int)
     */
    public void testGetInputMapint() {
        assertTrue(panel.getInputMap(JComponent.WHEN_FOCUSED) != null);
        assertTrue(panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) != null);
        assertTrue(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW) != null);
        boolean isThrown = false;
        try {
            panel.getInputMap(1812);
        } catch (IllegalArgumentException e) {
            isThrown = true;
        }
        assertTrue(isThrown);
    }

    /*
     * Class under test for InputMap getInputMap()
     */
    public void testGetInputMap() {
        assertTrue(panel.getInputMap() == panel.getInputMap(JComponent.WHEN_FOCUSED));
        panel.setInputMap(JComponent.WHEN_FOCUSED, new InputMap());
        assertTrue(panel.getInputMap() == panel.getInputMap(JComponent.WHEN_FOCUSED));
    }

    /*
     * Class under test for InputMap getInputMap(int, boolean)
     */
    public void testGetInputMapintboolean() {
        assertNull(panel.getInputMap(JComponent.WHEN_FOCUSED, false));
        assertNotNull(panel.getInputMap(JComponent.WHEN_FOCUSED, true));
        assertNotNull(panel.getInputMap(JComponent.WHEN_FOCUSED, false));
        panel.setInputMap(JComponent.WHEN_FOCUSED, null);
        assertNull(panel.getInputMap(JComponent.WHEN_FOCUSED, true));
        assertNull(panel.getInputMap(JComponent.WHEN_FOCUSED, false));
        assertNull(panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, false));
        assertNotNull(panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, true));
        assertNotNull(panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, false));
        panel.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
        assertNull(panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, true));
        assertNull(panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, false));
        assertNull(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, false));
        assertNotNull(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, true));
        assertNotNull(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, false));
    }

    /*
     * Class under test for ActionMap getActionMap()
     */
    public void testGetActionMap() {
        PropertyChangeController listener = new PropertyChangeController();
        assertTrue(panel.getActionMap() != null);
        ActionMap map = new ActionMap();
        panel.addPropertyChangeListener(listener);
        panel.setActionMap(map);
        assertTrue(panel.getActionMap() == map);
        assertFalse("event's not fired", listener.isChanged());
    }

    // this method is being tested by testGetActionMap()
    public void testSetActionMap() {
    }

    /*
     * Class under test for ActionMap getActionMap(boolean)
     */
    public void testGetActionMapboolean() {
        assertNull(panel.getActionMap(false));
        assertNotNull(panel.getActionMap(true));
        assertNotNull(panel.getActionMap(false));
        panel.setActionMap(null);
        assertNull(panel.getActionMap(true));
        assertNull(panel.getActionMap(false));
    }

    public void testSetToolTipText() {
        PropertyChangeController changeListener = new PropertyChangeController();
        String toolTipText1 = "toolTipText1";
        String toolTipText2 = "toolTipText2";
        panel.addPropertyChangeListener(changeListener);
        panel.setToolTipText(toolTipText1);
        changeListener.checkLastPropertyFired(panel, JComponent.TOOL_TIP_TEXT_KEY, null,
                toolTipText1);
        changeListener.reset();
        panel.setToolTipText(toolTipText1);
        assertFalse("event's not fired", changeListener.isChanged());
        panel.setToolTipText(toolTipText2);
        changeListener.checkLastPropertyFired(panel, JComponent.TOOL_TIP_TEXT_KEY,
                toolTipText1, toolTipText2);
        changeListener.reset();
        panel.setToolTipText(null);
        changeListener.checkLastPropertyFired(panel, JComponent.TOOL_TIP_TEXT_KEY,
                toolTipText2, null);
        panel.putClientProperty(JComponent.TOOL_TIP_TEXT_KEY, toolTipText1);
        assertTrue(panel.getToolTipText().equals(toolTipText1));
        panel.setToolTipText(toolTipText2);
        assertTrue(panel.getToolTipText().equals(toolTipText2));
        panel.setToolTipText(null);
        assertNull(panel.getToolTipText());
    }

    /*
     * Class under test for String getToolTipText()
     */
    public void testGetToolTipText() {
        String toolTipText1 = "toolTipText1";
        String toolTipText2 = "toolTipText2";
        assertNull(panel.getToolTipText());
        panel.setToolTipText(toolTipText1);
        assertTrue(panel.getToolTipText().equals(toolTipText1));
        panel.setToolTipText(toolTipText2);
        assertTrue(panel.getToolTipText().equals(toolTipText2));
        panel.setToolTipText(null);
        assertNull(panel.getToolTipText());
    }

    /*
     * Class under test for String getToolTipText(MouseEvent)
     */
    public void testGetToolTipTextMouseEvent() {
        MouseEvent event = new MouseEvent(panel, 0, 0, 0, 0, 0, 0, false);
        String toolTipText1 = "toolTipText1";
        String toolTipText2 = "toolTipText2";
        assertNull(panel.getToolTipText(event));
        panel.setToolTipText(toolTipText1);
        assertTrue(panel.getToolTipText(event).equals(toolTipText1));
        assertTrue(panel.getToolTipText(null).equals(toolTipText1));
        panel.setToolTipText(toolTipText2);
        assertTrue(panel.getToolTipText(event).equals(toolTipText2));
        assertTrue(panel.getToolTipText(null).equals(toolTipText2));
        panel.setToolTipText(null);
        assertNull(panel.getToolTipText(event));
        assertNull(panel.getToolTipText(null));
    }

    public void testGetToolTipLocation() {
        int x = 100;
        int y = 200;
        MouseEvent event = new MouseEvent(panel, 0, 0, 0, x, y, 0, false);
        assertNull(panel.getToolTipLocation(event));
        assertNull(panel.getToolTipLocation(null));
    }

    public void testCreateToolTip() {
        String toolTipText1 = "toolTipText1";
        JToolTip toolTip = null;
        toolTip = panel.createToolTip();
        assertTrue(toolTip.getComponent() == panel);
        assertNull(toolTip.getTipText());
        panel.setToolTipText(toolTipText1);
        toolTip = panel.createToolTip();
        assertTrue(toolTip.getComponent() == panel);
        assertNull(toolTip.getTipText());
        JPanel panel2 = new JPanel();
        toolTip = panel2.createToolTip();
        assertTrue(toolTip.getComponent() == panel2);
        assertNull(toolTip.getTipText());
    }

    /*
     * Class under test for String paramString()
     */
    public void testParamString() {
        String str = panel.paramString();
        assertTrue(str != null);
        assertTrue(str != "");
    }

    public void testRemoveVetoableChangeListener() {
        ConcreteVetoableChangeListener changeListener1 = new ConcreteVetoableChangeListener();
        ConcreteVetoableChangeListener changeListener2 = new ConcreteVetoableChangeListener();
        ConcreteVetoableChangeListener changeListener3 = new ConcreteVetoableChangeListener();
        VetoableChangeListener[] listenersArray = null;
        panel.addVetoableChangeListener(changeListener1);
        panel.addVetoableChangeListener(changeListener2);
        panel.addVetoableChangeListener(changeListener3);
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray.length == 3);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertTrue(changeListener2.findMe(listenersArray) == 1);
        assertTrue(changeListener3.findMe(listenersArray) == 1);
        panel.removeVetoableChangeListener(changeListener2);
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray.length == 2);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertTrue(changeListener2.findMe(listenersArray) == 0);
        assertTrue(changeListener3.findMe(listenersArray) == 1);
        panel.removeVetoableChangeListener(changeListener1);
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray.length == 1);
        assertTrue(changeListener3.findMe(listenersArray) == 1);
        panel.removeVetoableChangeListener(changeListener3);
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray.length == 0);
    }

    public void testAddVetoableChangeListener() {
        ConcreteVetoableChangeListener changeListener1 = new ConcreteVetoableChangeListener();
        ConcreteVetoableChangeListener changeListener2 = new ConcreteVetoableChangeListener();
        VetoableChangeListener[] listenersArray = null;
        panel.addVetoableChangeListener(changeListener1);
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray.length == 1);
        assertEquals(0, panel.listenerList.getListeners(VetoableChangeListener.class).length);
        assertEquals(1, panel.getListeners(VetoableChangeListener.class).length);
        assertTrue(changeListener1.findMe(listenersArray) > 0);
        panel.addVetoableChangeListener(changeListener2);
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray.length == 2);
        assertTrue(changeListener1.findMe(listenersArray) > 0);
        assertTrue(changeListener2.findMe(listenersArray) > 0);
        panel.addVetoableChangeListener(changeListener2);
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray.length == 3);
    }

    public void testGetVetoableChangeListeners() {
        ConcreteVetoableChangeListener changeListener1 = new ConcreteVetoableChangeListener();
        ConcreteVetoableChangeListener changeListener2 = new ConcreteVetoableChangeListener();
        ConcreteVetoableChangeListener changeListener3 = new ConcreteVetoableChangeListener();
        VetoableChangeListener[] listenersArray = null;
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray != null && listenersArray.length == 0);
        panel.addVetoableChangeListener(changeListener1);
        panel.addVetoableChangeListener(changeListener2);
        panel.addVetoableChangeListener(changeListener3);
        panel.addVetoableChangeListener(changeListener2);
        listenersArray = panel.getVetoableChangeListeners();
        assertTrue(listenersArray.length == 4);
        assertTrue(changeListener1.findMe(listenersArray) == 1);
        assertTrue(changeListener2.findMe(listenersArray) == 2);
        assertTrue(changeListener3.findMe(listenersArray) == 1);
    }

    public void testScrollRectToVisible() {
        class SRTVComponent extends JComponent {
            Rectangle scrollRect = null;

            @Override
            public void scrollRectToVisible(final Rectangle rect) {
                scrollRect = new Rectangle(rect);
            }
        }
        ;
        Container container1 = new Panel();
        Container container2 = new Container();
        Container container3 = new Panel();
        SRTVComponent container4 = new SRTVComponent();
        container1.add(panel);
        panel.setLocation(10, 10);
        container2.add(container1);
        container1.setLocation(10, 10);
        container3.add(container2);
        container2.setLocation(10, 10);
        container4.add(container3);
        container3.setLocation(10, 10);
        panel.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        assertEquals("rectangle's tranformed properly ", new Rectangle(40, 40, 1, 1),
                container4.scrollRect);
    }

    /*
     * this method is supposed to be tested in Component.testGetGraphics()
     */
    public void testGetGraphics() {
    }

    /*
     * Class under test for void setFont(Font)
     */
    public void testSetFontFont() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        Font newFont1 = new Font(null, Font.BOLD, 10);
        Font newFont2 = new Font(null, Font.BOLD, 20);
        Font oldFont = panel.getFont();
        panel.setFont(newFont1);
        assertTrue(panel.getFont() == newFont1);
        changeListener.checkLastPropertyFired(panel, "font", oldFont, newFont1);
        changeListener.reset();
        panel.setFont(newFont2);
        assertTrue(panel.getFont() == newFont2);
        changeListener.checkLastPropertyFired(panel, "font", newFont1, newFont2);
        changeListener.reset();
        panel.setFont(newFont2);
        assertTrue(panel.getFont() == newFont2);
        assertFalse("event's not fired", changeListener.isChanged());
        changeListener.reset();
    }

    public void testGetTopLevelAncestor() {
        JFrame mainFrame = new JFrame();
        JDialog dialog = new JDialog();
        assertNull(panel.getTopLevelAncestor());
        mainFrame.getContentPane().add(panel);
        assertTrue(panel.getTopLevelAncestor() == mainFrame);
        dialog.getContentPane().add(panel);
        assertTrue(panel.getTopLevelAncestor() == dialog);
        dialog.getContentPane().remove(panel);
        assertNull(panel.getTopLevelAncestor());
        mainFrame.dispose();
    }

    public void testSetNextFocusableComponent() {
        JComponent container = new JPanel();
        JComponent panel1 = new JButton();
        JComponent panel2 = new JButton();
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        panel.setNextFocusableComponent(panel1);
        changeListener.checkLastPropertyFired(panel, "nextFocus", null, panel1);
        changeListener.reset();
        panel.setNextFocusableComponent(panel2);
        changeListener.checkLastPropertyFired(panel, "nextFocus", panel1, panel2);
        changeListener.reset();
        panel.setNextFocusableComponent(panel2);
        assertFalse("event's not fired", changeListener.isChanged());
        JFrame frame = new JFrame();
        container.add(panel1);
        container.add(panel2);
        frame.getContentPane().add(container);
        frame.pack();
        container.setFocusCycleRoot(true);
        container.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
        panel1.setNextFocusableComponent(panel2);
        assertNull(container.getFocusTraversalPolicy().getComponentBefore(container, panel1));
        assertEquals(panel2, container.getFocusTraversalPolicy().getComponentAfter(container,
                panel1));
        assertEquals(panel1, container.getFocusTraversalPolicy().getComponentBefore(container,
                panel2));
        assertNull(container.getFocusTraversalPolicy().getComponentAfter(container, panel2));
    }

    public void testGetNextFocusableComponent() {
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        assertNull("default nextFocusableComponent isn't set ", panel
                .getNextFocusableComponent());
        panel.setNextFocusableComponent(panel1);
        assertEquals("nextFocusableComponent set correctly ", panel1, panel
                .getNextFocusableComponent());
        panel.setNextFocusableComponent(panel2);
        assertEquals("nextFocusableComponent set correctly ", panel2, panel
                .getNextFocusableComponent());
    }

    /*
     * Class under test for void setForeground(Color)
     */
    public void testSetForegroundColor() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        Color newColor1 = new Color(10, 10, 10);
        Color newColor2 = new Color(20, 20, 20);
        Color oldColor = panel.getForeground();
        panel.setForeground(newColor1);
        assertSame(newColor1, panel.getForeground());
        changeListener.checkLastPropertyFired(panel, "foreground", oldColor, newColor1);
        changeListener.reset();
        panel.setForeground(newColor2);
        assertSame(newColor2, panel.getForeground());
        changeListener.checkLastPropertyFired(panel, "foreground", newColor1, newColor2);
        changeListener.reset();
        panel.setForeground(newColor2);
        assertTrue(panel.getForeground() == newColor2);
        assertFalse("event's not fired", changeListener.isChanged());
        changeListener.reset();
    }

    /*
     * Class under test for void setBackground(Color)
     */
    public void testSetBackgroundColor() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        Color newColor1 = new Color(10, 10, 10);
        Color newColor2 = new Color(20, 20, 20);
        Color oldColor = panel.getBackground();
        panel.setBackground(newColor1);
        assertSame(newColor1, panel.getBackground());
        changeListener.checkLastPropertyFired(panel, "background", oldColor, newColor1);
        changeListener.reset();
        panel.setBackground(newColor2);
        assertSame(newColor2, panel.getBackground());
        changeListener.checkLastPropertyFired(panel, "background", newColor1, newColor2);
        changeListener.reset();
        panel.setBackground(newColor2);
        assertSame(newColor2, panel.getBackground());
        assertFalse("event's not fired", changeListener.isChanged());
        changeListener.reset();
    }

    public void testSetVerifyInputWhenFocusTarget() {
        PropertyChangeController changeListener = new PropertyChangeController();
        panel.addPropertyChangeListener(changeListener);
        panel.setVerifyInputWhenFocusTarget(false);
        changeListener.checkLastPropertyFired(panel, "verifyInputWhenFocusTarget",
                Boolean.TRUE, Boolean.FALSE);
        changeListener.reset();
        panel.setVerifyInputWhenFocusTarget(true);
        changeListener.checkLastPropertyFired(panel, "verifyInputWhenFocusTarget",
                Boolean.FALSE, Boolean.TRUE);
        changeListener.reset();
        panel.setVerifyInputWhenFocusTarget(true);
        assertFalse("event's not fired", changeListener.isChanged());
        changeListener.reset();
    }

    public void testIsRequestFocusEnabled() {
        assertTrue("default value for requestFocusEnabled ", panel.isRequestFocusEnabled());
    }

    public void testSetRequestFocusEnabled() {
        panel.setRequestFocusEnabled(false);
        assertFalse("value for requestFocusEnabled set correctly ", panel
                .isRequestFocusEnabled());
        panel.setRequestFocusEnabled(true);
        assertTrue("value for requestFocusEnabled set correctly ", panel
                .isRequestFocusEnabled());
    }

    public void testSetDoubleBuffered() {
        panel.setDoubleBuffered(true);
        assertTrue("now panel is doubleBuffered ", panel.isDoubleBuffered());
        panel.setDoubleBuffered(false);
        assertFalse("now panel is not doubleBuffered ", panel.isDoubleBuffered());
        panel.setDoubleBuffered(true);
        assertTrue("now panel is doubleBuffered ", panel.isDoubleBuffered());
    }

    /*
     * Class under test for void paintImmediately(int, int, int, int)
     */
    public void testPaintImmediatelyintintintint() {
        // TODO
    }

    public void testIsPaintingTile() {
        // TODO
    }

    public void testRevalidate() {
        JFrame frame = new JFrame();
        JButton button = new JButton("test");
        frame.getContentPane().add(button);
        frame.setVisible(true);
        assertTrue(button.isValid());
        button.revalidate();
        assertFalse(button.isValid());

        frame.getRootPane().validate();
        assertTrue(button.isValid());

        final Marker rm = new Marker();
        RepaintManager.setCurrentManager(new RepaintManager() {
            public void addInvalidComponent(final JComponent invalidComponent) {
                rm.setAuxiliary(invalidComponent);
            }
        });
        button.revalidate();
        assertFalse(button.isValid());
        assertSame(button, rm.getAuxiliary());

        frame.dispose();
    }

    /*
     * Class under test for void repaint(Rectangle)
     */
    public void testRepaintRectangle() {
        // TODO
    }

    /*
     * Class under test for void paintImmediately(Rectangle)
     */
    public void testPaintImmediatelyRectangle() {
        try {    
            new JComponent(){}.paintImmediately(null);    
            fail("NPE should be thrown"); 
        } catch (NullPointerException npe) {      
            // PASSED            
        }
    }

    /*
     * Class under test for void update(Graphics)
     */
    public void testUpdateGraphics() {
        // TODO
    }

    public void testPrintComponent() {
        // TODO
    }

    public void testPrintChildren() {
        // TODO
    }

    public void testPrintBorder() {
        // TODO
    }

    /*
     * Class under test for void printAll(Graphics)
     */
    public void testPrintAllGraphics() {
        // TODO
    }

    /*
     * Class under test for void print(Graphics)
     */
    public void testPrintGraphics() {
        // TODO
    }

    public void testPaintComponent() {
        // TODO
    }

    public void testPaintChildren() {
        // TODO
    }

    public void testPaintBorder() {
        // TODO
    }

    /*
     * Class under test for void paint(Graphics)
     */
    public void testPaintGraphics() {
        // TODO
    }

    /*
     * Class under test for void repaint(long, int, int, int, int)
     */
    public void testRepaintlongintintintint() {
        // TODO
    }

    public void testSetDebugGraphicsOptions() {
        if (!isHarmony()) {
            panel
                    .setDebugGraphicsOptions(DebugGraphics.LOG_OPTION
                            | DebugGraphics.FLASH_OPTION);
            assertTrue("options are set correctly ",
                    (panel.getDebugGraphicsOptions() & DebugGraphics.FLASH_OPTION) != 0);
            assertTrue("options are set correctly ",
                    (panel.getDebugGraphicsOptions() & DebugGraphics.LOG_OPTION) != 0);
            assertFalse("options are set correctly ",
                    (panel.getDebugGraphicsOptions() & DebugGraphics.BUFFERED_OPTION) != 0);
            panel.setDebugGraphicsOptions(DebugGraphics.BUFFERED_OPTION);
            assertFalse("options are set correctly ",
                    (panel.getDebugGraphicsOptions() & DebugGraphics.FLASH_OPTION) != 0);
            assertFalse("options are set correctly ",
                    (panel.getDebugGraphicsOptions() & DebugGraphics.LOG_OPTION) != 0);
            assertTrue("options are set correctly ",
                    (panel.getDebugGraphicsOptions() & DebugGraphics.BUFFERED_OPTION) != 0);
            panel.setDebugGraphicsOptions(DebugGraphics.NONE_OPTION);
            assertFalse("options are set correctly ",
                    (panel.getDebugGraphicsOptions() & DebugGraphics.FLASH_OPTION) != 0);
            assertFalse("options are set correctly ",
                    (panel.getDebugGraphicsOptions() & DebugGraphics.LOG_OPTION) != 0);
            assertFalse("options are set correctly ",
                    (panel.getDebugGraphicsOptions() & DebugGraphics.BUFFERED_OPTION) != 0);
            assertTrue("options are set correctly ", panel.getDebugGraphicsOptions() == 0);
        }
    }

    public void testGetDebugGraphicsOptions() {
        assertTrue("options are set correctly ", panel.getDebugGraphicsOptions() == 0);
    }

    public void testIsValidateRoot() {
        assertFalse("isValidateRoot always returns false ", panel.isValidateRoot());
    }

    public void testIsOptimizedDrawingEnabled() {
        assertTrue("isOptimizedDrawingEnabled always returns true ", panel
                .isOptimizedDrawingEnabled());
    }

    public void testIsManagingFocus() {
        assertFalse("isManagingFocus always returns false ", panel.isManagingFocus());
    }

    public void testGetVerifyInputWhenFocusTarget() {
        assertTrue("default verifyInputWhenFocusTarget value", panel
                .getVerifyInputWhenFocusTarget());
        panel.setVerifyInputWhenFocusTarget(false);
        assertFalse("verifyInputWhenFocusTarget value set properly ", panel
                .getVerifyInputWhenFocusTarget());
        panel.setVerifyInputWhenFocusTarget(true);
        assertTrue("verifyInputWhenFocusTarget value set properly ", panel
                .getVerifyInputWhenFocusTarget());
    }

    public void testSetAutoscrolls() {
        panel.setAutoscrolls(true);
        assertTrue(panel.getAutoscrolls());
        panel.setAutoscrolls(false);
        assertFalse(panel.getAutoscrolls());
        panel.setAutoscrolls(true);
        assertTrue(panel.getAutoscrolls());
    }

    public void testGetAutoscrolls() {
        assertFalse(panel.getAutoscrolls());
    }

    public void testSetDefaultLocale() {
        Locale locale = JComponent.getDefaultLocale().equals(Locale.CHINA) ? Locale.ITALIAN
                : Locale.CHINA;
        assertFalse(panel.getLocale().equals(locale));
        assertFalse(JComponent.getDefaultLocale().equals(locale));
        JComponent.setDefaultLocale(locale);
        panel = new JPanel();
        assertTrue(panel.getLocale().equals(locale));
        assertTrue(JComponent.getDefaultLocale().equals(locale));
    }

    // this method is being tested by testSetDefaultLocale()
    public void testGetDefaultLocale() {
    }

    public void testIsLightweightComponent() {
        JFrame mainFrame = new JFrame();
        Canvas button = new Canvas();
        JPanel jbutton = new JPanel();
        Panel awtPanel = new Panel();
        //        assertFalse(JComponent.isLightweightComponent(panel));
        //        assertFalse(JComponent.isLightweightComponent(button));
        //        assertFalse(JComponent.isLightweightComponent(jbutton));
        //        assertFalse(JComponent.isLightweightComponent(awtPanel));
        mainFrame.getContentPane().add(panel);
        mainFrame.getContentPane().add(button);
        mainFrame.getContentPane().add(jbutton);
        mainFrame.getContentPane().add(awtPanel);
        mainFrame.pack();
        assertTrue(JComponent.isLightweightComponent(panel));
        assertFalse(JComponent.isLightweightComponent(button));
        assertTrue(JComponent.isLightweightComponent(jbutton));
        assertFalse(JComponent.isLightweightComponent(awtPanel));
        mainFrame.dispose();
    }

    public void testUpdateUI() {
        new JPanel().updateUI();
        new JLayeredPane().updateUI();
    }

    public void testSetInheritsPopupMenu() {
        PropertyChangeController listener1 = new PropertyChangeController();
        panel.addPropertyChangeListener(listener1);
        panel.setInheritsPopupMenu(true);
        if (isHarmony()) {
            listener1.checkPropertyFired(panel, "inheritsPopupMenu", Boolean.FALSE,
                    Boolean.TRUE);
        }
        assertTrue("InheritsPopupMenu", panel.getInheritsPopupMenu());
        listener1.reset();
        panel.setInheritsPopupMenu(false);
        if (isHarmony()) {
            listener1.checkPropertyFired(panel, "inheritsPopupMenu", Boolean.TRUE,
                    Boolean.FALSE);
        }
        assertFalse("InheritsPopupMenu", panel.getInheritsPopupMenu());
        listener1.reset();
        panel.setInheritsPopupMenu(false);
        assertTrue("event's not been fired ", !listener1.isChanged());
    }

    public void testGetInheritsPopupMenu() {
        assertFalse("InheritsPopupMenu", panel.getInheritsPopupMenu());
    }

    public void testSetComponentPopupMenu() {
        PropertyChangeController listener1 = new PropertyChangeController();
        JPopupMenu popup1 = new JPopupMenu();
        JPopupMenu popup2 = new JPopupMenu();
        panel.addPropertyChangeListener(listener1);
        panel.setComponentPopupMenu(popup1);
        if (isHarmony()) {
            listener1.checkPropertyFired(panel, "componentPopupMenu", null, popup1);
        }
        assertEquals("ComponentPopupMenu", popup1, panel.getComponentPopupMenu());
        listener1.reset();
        panel.setComponentPopupMenu(popup2);
        if (isHarmony()) {
            listener1.checkPropertyFired(panel, "componentPopupMenu", popup1, popup2);
        }
        assertEquals("ComponentPopupMenu", popup2, panel.getComponentPopupMenu());
        listener1.reset();
        panel.setInheritsPopupMenu(true);
        listener1.reset();
        panel.setComponentPopupMenu(popup2);
        assertTrue("event's not been fired ", !listener1.isChanged());
    }

    public void testGetComponentPopupMenu() {
        assertNull("ComponentPopupMenu", panel.getComponentPopupMenu());
        JPanel parent = new JPanel();
        parent.add(panel);
        JPopupMenu popup1 = new JPopupMenu();
        JPopupMenu popup2 = new JPopupMenu();
        panel.setComponentPopupMenu(popup1);
        parent.setComponentPopupMenu(popup2);
        panel.setInheritsPopupMenu(false);
        assertEquals("ComponentPopupMenu", popup1, panel.getComponentPopupMenu());
        panel.setInheritsPopupMenu(true);
        assertEquals("ComponentPopupMenu", popup1, panel.getComponentPopupMenu());
        panel.setComponentPopupMenu(null);
        assertEquals("ComponentPopupMenu", popup2, panel.getComponentPopupMenu());
        panel.setInheritsPopupMenu(false);
        assertNull("ComponentPopupMenu", panel.getComponentPopupMenu());
    }

    public void testGetPopupLocation() {
        panel.setPreferredSize(new Dimension(100, 100));
        MouseEvent event = new MouseEvent(panel, 0, 0, 0, 10, 20, 1, true);
        assertNull("PopupLocation", panel.getPopupLocation(event));
    }

    @SuppressWarnings("unchecked")
    private void removeListeners(final JComponent comp, final Class<? extends EventListener> c) {
        EventListener[] listeners = comp.getListeners(c);
        for (int i = 0; i < listeners.length; i++) {
            comp.listenerList.remove((Class<EventListener>) c, listeners[i]);
        }
    }
}