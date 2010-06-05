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
 * Created on 12.10.2004
 * * Window - Preferences - Java - Code Style - Code Templates
 *
 */
package javax.swing;

import java.awt.Button;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;

public class SwingUtilitiesTest extends SwingTestCase implements SwingConstants {
    class UIActionMap extends ActionMap implements UIResource {
        private static final long serialVersionUID = 1L;
    }

    class UIInputMap extends InputMap implements UIResource {
        private static final long serialVersionUID = 1L;
    }

    /*
     * Class under test for String layoutCompoundLabel(JComponent, FontMetrics, String, Icon, int, int, int, int, Rectangle, Rectangle, Rectangle, int)
     */
    public void testLayoutCompoundLabelJComponentFontMetricsStringIconintintintintRectangleRectangleRectangleint() {
        JComponent panel = new JPanel();
        Font font = new Font("Fixed", Font.PLAIN, 12);
        FontMetrics metrics = getFontMetrics(font);
        Rectangle viewR = new Rectangle(0, 0, 150, 150);
        Rectangle iconR = new Rectangle(0, 0, 0, 0);
        Rectangle textR = new Rectangle(0, 0, 0, 0);
        String initialString = "Long enough text for this label, can you see that it is clipped now?";
        Icon icon = new ImageIcon(new BufferedImage(30, 40, BufferedImage.TYPE_4BYTE_ABGR));
        String clippedStr = null;
        clippedStr = SwingUtilities.layoutCompoundLabel(panel, metrics, initialString, icon,
                TOP, CENTER, BOTTOM, RIGHT, viewR, iconR, textR, 30);
        assertEquals("clipped string ", "Long...", clippedStr);
        assertEquals("icon rectangle ", new Rectangle(3, 0, 30, 40), iconR);
        assertEquals("text rectangle ", new Rectangle(63, 2, 84, 38), textR);
        viewR = new Rectangle(20, 20, 100, 100);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(panel, metrics, initialString, icon,
                CENTER, CENTER, TOP, RIGHT, viewR, iconR, textR, 30);
        assertEquals("clipped string ", "...", clippedStr);
        assertEquals("icon rectangle ", new Rectangle(22, 50, 30, 40), iconR);
        assertEquals("text rectangle ", new Rectangle(82, 50, 36, 38), textR);
        panel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        viewR = new Rectangle(20, 20, 130, 100);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(panel, metrics, initialString, icon,
                CENTER, CENTER, TOP, LEADING, viewR, iconR, textR, 30);
        assertEquals("clipped string ", "Lo...", clippedStr);
        assertEquals("icon rectangle ", new Rectangle(25, 50, 30, 40), iconR);
        assertEquals("text rectangle ", new Rectangle(85, 50, 60, 38), textR);
        viewR = new Rectangle(0, 0, 292, 200);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        JLabel label = new JLabel();
        icon = new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR));
        metrics = getFontMetrics(font, 34);
        clippedStr = SwingUtilities.layoutCompoundLabel(label, metrics,
                "Hello world you are great!", icon, BOTTOM, CENTER, BOTTOM, TRAILING, viewR,
                iconR, textR, label.getIconTextGap());
        assertEquals("clipped string ", "Hello...", clippedStr);
        assertEquals("icon rectangle ", new Rectangle(0, 184, 16, 16), iconR);
        assertEquals("text rectangle ", new Rectangle(20, 96, 272, 104), textR);
        if (isHarmony()) {
            icon = new ImageIcon(new BufferedImage(52, 64, BufferedImage.TYPE_4BYTE_ABGR));
            metrics = getFontMetrics(font, 12);
            viewR = new Rectangle(120, 120, 1280, 280);
            clippedStr = SwingUtilities.layoutCompoundLabel(panel, metrics, initialString,
                    icon, CENTER, CENTER, CENTER, LEADING, viewR, iconR, textR, 30);
            assertEquals("clipped string ",
                    "Long enough text for this label, can you see that it is clipped now?",
                    clippedStr);
            assertEquals("icon rectangle ", new Rectangle(311, 228, 52, 64), iconR);
            assertEquals("text rectangle ", new Rectangle(393, 241, 816, 38), textR);
            panel.applyComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            viewR = new Rectangle(120, 120, 40, 40);
            clippedStr = SwingUtilities.layoutCompoundLabel(panel, metrics, initialString,
                    icon, CENTER, CENTER, CENTER, LEADING, viewR, iconR, textR, 30);
            assertEquals("clipped string ", "...", clippedStr);
            assertEquals("icon rectangle ", new Rectangle(147, 108, 52, 64), iconR);
            assertEquals("text rectangle ", new Rectangle(81, 121, 36, 38), textR);
        }
    }

    /*
     * Class under test for String layoutCompoundLabel(FontMetrics, String, Icon, int, int, int, int, Rectangle, Rectangle, Rectangle, int)
     */
    public void testLayoutCompoundLabelFontMetricsStringIconintintintintRectangleRectangleRectangleint() {
        Font font = new Font("Fixed", Font.PLAIN, 12);
        FontMetrics metrics = getFontMetrics(font);
        Rectangle viewR = new Rectangle(0, 0, 10, 10);
        Rectangle iconR = new Rectangle(0, 0, 0, 0);
        Rectangle textR = new Rectangle(0, 0, 0, 0);
        String clippedStr = SwingUtilities.layoutCompoundLabel(metrics, null, null, CENTER,
                LEADING, CENTER, LEADING, viewR, iconR, textR, 2);
        assertEquals("clipped string ", "", clippedStr);
        assertTrue("icon rectangle ", iconR.isEmpty());
        assertTrue("text rectangle ", textR.isEmpty());
        viewR = new Rectangle(0, 0, 150, 150);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        String initialString = "Long enough text for this label, can you see that it is clipped now?";
        Icon icon = null;
        metrics = getFontMetrics(font, 5);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon, TOP,
                CENTER, BOTTOM, RIGHT, viewR, iconR, textR, 30);
        assertEquals("clipped string ", "Long enough text for this l...", clippedStr);
        assertTrue("icon rectangle ", iconR.isEmpty());
        assertEquals("text rectangle ", new Rectangle(0, 0, 150, 17), textR);
        viewR = new Rectangle(0, 0, 150, 150);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        metrics = getFontMetrics(font, 5);
        icon = new ImageIcon(new BufferedImage(30, 40, BufferedImage.TYPE_4BYTE_ABGR));
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon, TOP,
                CENTER, BOTTOM, RIGHT, viewR, iconR, textR, 30);
        assertEquals("clipped string ", "Long enough tex...", clippedStr);
        assertEquals("icon rectangle ", new Rectangle(0, 0, 30, 40), iconR);
        assertEquals("text rectangle ", new Rectangle(60, 23, 90, 17), textR);
        viewR = new Rectangle(20, 20, 100, 100);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        metrics = getFontMetrics(font, 5);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon, CENTER,
                CENTER, TOP, RIGHT, viewR, iconR, textR, 30);
        assertEquals("clipped string ", "Long ...", clippedStr);
        assertEquals("icon rectangle ", new Rectangle(20, 50, 30, 40), iconR);
        assertEquals("text rectangle ", new Rectangle(80, 50, 40, 17), textR);
        icon = new ImageIcon(new BufferedImage(52, 64, BufferedImage.TYPE_4BYTE_ABGR));
        metrics = getFontMetrics(font, 24);
        viewR = new Rectangle(100, 100, 1500, 300);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon, CENTER,
                CENTER, CENTER, RIGHT, viewR, iconR, textR, 30);
        assertEquals("clipped string ",
                "Long enough text for this label, can you see that it is ...", clippedStr);
        assertEquals("icon rectangle ", new Rectangle(101, 218, 52, 64), iconR);
        assertEquals("text rectangle ", new Rectangle(183, 213, 1416, 74), textR);
        icon = new ImageIcon(new BufferedImage(52, 64, BufferedImage.TYPE_4BYTE_ABGR));
        viewR = new Rectangle(100, 100, 1500, 300);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, null, icon, CENTER, CENTER,
                CENTER, RIGHT, viewR, iconR, textR, 30);
        assertEquals("clipped string ", "", clippedStr);
        assertEquals("icon rectangle ", new Rectangle(824, 218, 52, 64), iconR);
        assertTrue("text rectangle ", textR.isEmpty());
        icon = null;
        metrics = getFontMetrics(font, 6);
        viewR = new Rectangle(0, 0, 4, 300);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon, CENTER,
                CENTER, CENTER, RIGHT, viewR, iconR, textR, 0);
        assertEquals("clipped string ", "...", clippedStr);
        icon = null;
        viewR = new Rectangle(0, 0, 2, 300);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon, CENTER,
                CENTER, CENTER, LEADING, viewR, iconR, textR, 0);
        assertEquals("clipped string ", "...", clippedStr);
        icon = null;
        metrics = getFontMetrics(font, 30);
        viewR = new Rectangle(0, 0, 500, 300);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon, CENTER,
                CENTER, CENTER, RIGHT, viewR, iconR, textR, 0);
        assertEquals("clipped string ", "Long enough t...", clippedStr);
        icon = null;
        metrics = getFontMetrics(font, 16);
        viewR = new Rectangle(0, 0, 500, 300);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics,
                "WWWWWWWWWWWWWWWWWWWW,,,,,,,,,,,,,,,,,,,,,,", icon, CENTER, CENTER, CENTER,
                RIGHT, viewR, iconR, textR, 0);
        assertEquals("clipped string ", "WWWWWWWWWWWWWWWWWWWW,,,,,,,,...", clippedStr);
        icon = new ImageIcon(new BufferedImage(52, 64, BufferedImage.TYPE_4BYTE_ABGR));
        metrics = getFontMetrics(font, 30);
        viewR = new Rectangle(100, 100, 1500, 300);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon, CENTER,
                CENTER, TOP, CENTER, viewR, iconR, textR, 30);
        assertEquals("clipped string ", "Long enough text for this label, can you see th...",
                clippedStr);
        assertEquals("icon rectangle ", new Rectangle(824, 279, 52, 64), iconR);
        assertEquals("text rectangle ", new Rectangle(100, 157, 1500, 92), textR);
        icon = new ImageIcon(new BufferedImage(52, 64, BufferedImage.TYPE_4BYTE_ABGR));
        metrics = getFontMetrics(font, 28);
        viewR = new Rectangle(100, 100, 1500, 300);
        iconR = new Rectangle(0, 0, 0, 0);
        textR = new Rectangle(0, 0, 0, 0);
        clippedStr = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon, CENTER,
                CENTER, TOP, RIGHT, viewR, iconR, textR, 30);
        assertEquals("clipped string ", "Long enough text for this label, can you see th...",
                clippedStr);
        assertEquals("icon rectangle ", new Rectangle(109, 207, 52, 64), iconR);
        assertEquals("text rectangle ", new Rectangle(191, 207, 1400, 86), textR);
        if (isHarmony()) {
            Rectangle iconR2 = new Rectangle();
            Rectangle textR2 = new Rectangle();
            String clippedStr2 = SwingUtilities.layoutCompoundLabel(metrics, initialString,
                    icon, CENTER, LEADING, TOP, TRAILING, viewR, iconR2, textR2, 30);
            assertEquals(clippedStr, clippedStr2);
            assertEquals(iconR, iconR2);
            assertEquals(textR, textR2);
            clippedStr2 = SwingUtilities.layoutCompoundLabel(metrics, initialString, icon,
                    CENTER, TRAILING, TOP, LEADING, viewR, iconR2, textR2, 30);
            assertEquals(clippedStr, clippedStr2);
            assertEquals(iconR, iconR2);
            assertEquals(textR, textR2);
        }
    }

    public void testNotifyAction() {
        class NATestAction implements Action {
            public boolean enabled = true;

            protected ActionEvent eventHappened = null;

            public Object valueSaved = null;

            public void actionPerformed(final ActionEvent event) {
                eventHappened = event;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(final boolean e) {
                enabled = e;
            }

            public void addPropertyChangeListener(final PropertyChangeListener arg0) {
            }

            public void removePropertyChangeListener(final PropertyChangeListener arg0) {
            }

            public Object getValue(final String valueName) {
                if (valueName == Action.ACTION_COMMAND_KEY) {
                    return valueSaved;
                }
                return null;
            }

            public void putValue(final String valueName, final Object value) {
                if (valueName == Action.ACTION_COMMAND_KEY) {
                    valueSaved = value;
                }
            }
        }
        ;
        NATestAction action = new NATestAction();
        JComponent source = new JPanel();
        int modifiers = InputEvent.CTRL_DOWN_MASK;
        KeyEvent event = new KeyEvent(source, 0, 0, modifiers, KeyEvent.VK_CUT, 'a');
        boolean result = false;
        try {
            SwingUtilities.notifyAction(null, KeyStroke.getKeyStroke('a'), event, this,
                    modifiers);
        } catch (NullPointerException e) {
            fail("NPE should not be thrown");
        }
        result = SwingUtilities.notifyAction(action, KeyStroke.getKeyStroke('a'), event, this,
                modifiers);
        assertTrue(action.eventHappened != null);
        assertFalse(event.isConsumed());
        assertTrue(result);
        action.setEnabled(false);
        action.eventHappened = null;
        result = SwingUtilities.notifyAction(action, KeyStroke.getKeyStroke('a'), event, this,
                modifiers);
        assertFalse(result);
        assertFalse(event.isConsumed());
        assertNull(action.eventHappened);
        action.setEnabled(true);
        result = SwingUtilities.notifyAction(action, KeyStroke.getKeyStroke('a'), event, this,
                modifiers);
        assertTrue(result);
        assertTrue(action.eventHappened != null);
        assertFalse(event.isConsumed());
        assertTrue(action.eventHappened.getActionCommand().equals("a"));
        action.valueSaved = "Yo!";
        result = SwingUtilities.notifyAction(action, KeyStroke.getKeyStroke('a'), event, this,
                modifiers);
        assertTrue(result);
        assertTrue(action.eventHappened != null);
        assertFalse(event.isConsumed());
        assertTrue(action.eventHappened.getActionCommand().equals(action.valueSaved));
        event = new KeyEvent(source, 0, 0, modifiers, KeyEvent.VK_UNDEFINED,
                KeyEvent.CHAR_UNDEFINED);
        action.valueSaved = null;
        result = SwingUtilities.notifyAction(action, KeyStroke.getKeyStroke('a'), event, this,
                modifiers);
        assertTrue(result);
        assertFalse(event.isConsumed());
        assertTrue(action.eventHappened != null);
        assertNull(action.eventHappened.getActionCommand());
        event = new KeyEvent(source, 0, 0, modifiers, KeyEvent.VK_UNDEFINED,
                KeyEvent.CHAR_UNDEFINED);
        action.setEnabled(false);
        result = SwingUtilities.notifyAction(action, KeyStroke.getKeyStroke('a'), event, this,
                modifiers);
        assertFalse(result);
        assertFalse(event.isConsumed());
        assertTrue(action.eventHappened != null);
        assertNull(action.eventHappened.getActionCommand());
    }

    @SuppressWarnings("deprecation")
    public void testConvertMouseEvent() {
        MouseEvent eventBefore, eventToPass, eventAfter;
        JWindow window1 = new JWindow();
        JWindow window2 = new JWindow();
        final JComponent panel1 = new JPanel();
        final JComponent panel2 = new JPanel();
        final JComponent panel3 = new JPanel();
        panel1.setPreferredSize(new Dimension(100, 100));
        panel2.setPreferredSize(new Dimension(100, 100));
        panel3.setPreferredSize(new Dimension(100, 100));
        panel1.add(panel2);
        panel1.setBorder(new EmptyBorder(15, 15, 15, 15));
        window1.getContentPane().add(panel1);
        window1.setLocation(100, 100);
        window1.pack();
        window1.show();
        window2.getContentPane().add(panel3);
        window2.setLocation(200, 200);
        window2.pack();
        window2.show();
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.convertMouseEvent(null, null, null);
            }
        });
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.convertMouseEvent(panel1, null, panel2);
            }
        });
        int id = 111;
        int when = 200;
        int x = 0;
        int y = 0;
        int clickCount = 3;
        int modifiers = InputEvent.CTRL_DOWN_MASK;
        boolean isPopupTrigger = false;
        int button = 2;
        eventBefore = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        eventToPass = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        final MouseEvent illegalEvent = eventBefore;
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.convertMouseEvent(null, illegalEvent, null);
            }
        });
        eventBefore = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        eventToPass = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        eventAfter = SwingUtilities.convertMouseEvent(panel1, eventToPass, panel3);
        assertTrue(eventBefore.getX() == eventToPass.getX()
                && eventBefore.getY() == eventToPass.getY());
        assertEquals(-100, eventAfter.getX());
        assertEquals(-100, eventAfter.getY());
        assertEquals(panel3, eventAfter.getSource());
        eventBefore = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        eventToPass = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        eventAfter = SwingUtilities.convertMouseEvent(panel3, eventToPass, panel2);
        assertTrue(eventBefore.getX() == eventToPass.getX()
                && eventBefore.getY() == eventToPass.getY());
        assertEquals(100, eventAfter.getX());
        assertEquals(80, eventAfter.getY());
        assertEquals(panel2, eventAfter.getSource());
        eventBefore = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        eventToPass = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        eventAfter = SwingUtilities.convertMouseEvent(panel2, eventToPass, null);
        assertTrue(eventBefore.getX() == eventToPass.getX()
                && eventBefore.getY() == eventToPass.getY());
        assertEquals(0, eventAfter.getX());
        assertEquals(20, eventAfter.getY());
        assertEquals(panel2, eventAfter.getSource());
        eventBefore = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        eventToPass = new MouseEvent(panel1, id, when, modifiers, x, y, clickCount,
                isPopupTrigger, button);
        eventAfter = SwingUtilities.convertMouseEvent(null, eventToPass, panel2);
        assertTrue(eventBefore.getX() == eventToPass.getX()
                && eventBefore.getY() == eventToPass.getY());
        assertEquals(0, eventAfter.getX());
        assertEquals(-20, eventAfter.getY());
        assertEquals(panel2, eventAfter.getSource());
    }

    @SuppressWarnings("deprecation")
    public void testConvertRectangle() {
        int width = 100;
        int height = 200;
        Rectangle rectBefore, rectToPass, rectAfter;
        JWindow window1 = new JWindow();
        JWindow window2 = new JWindow();
        final JComponent panel1 = new JPanel();
        final JComponent panel2 = new JPanel();
        final JComponent panel3 = new JPanel();
        panel1.setPreferredSize(new Dimension(100, 100));
        panel2.setPreferredSize(new Dimension(100, 100));
        panel3.setPreferredSize(new Dimension(100, 100));
        //panel1.setLocation(50, 50);
        //panel2.setLocation(70, 70);
        //panel3.setLocation(500, 500);
        panel1.add(panel2);
        panel1.setBorder(new EmptyBorder(15, 15, 15, 15));
        //panel2.setBorder(new EmptyBorder(27, 27, 27, 27));
        window1.getContentPane().add(panel1);
        window1.setLocation(100, 100);
        window1.pack();
        window1.show();
        window2.getContentPane().add(panel3);
        window2.setLocation(200, 200);
        window2.pack();
        window2.show();
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.convertRectangle(null, null, null);
            }
        });
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.convertRectangle(panel1, null, panel2);
            }
        });
        rectBefore = new Rectangle(0, 0, width, height);
        rectToPass = new Rectangle(rectBefore);
        rectAfter = SwingUtilities.convertRectangle(null, rectBefore, null);
        assertEquals(rectBefore, rectAfter);
        assertEquals(rectBefore, rectToPass);
        rectBefore = new Rectangle(0, 0, width, height);
        rectToPass = new Rectangle(rectBefore);
        rectAfter = SwingUtilities.convertRectangle(panel1, rectToPass, panel3);
        assertEquals(rectBefore, rectToPass);
        assertFalse(rectAfter.equals(rectBefore));
        assertEquals(rectAfter, new Rectangle(-100, -100, width, height));
        rectBefore = new Rectangle(0, 0, width, height);
        rectToPass = new Rectangle(rectBefore);
        rectAfter = SwingUtilities.convertRectangle(panel3, rectToPass, panel2);
        assertEquals(rectBefore, rectToPass);
        assertFalse(rectAfter.equals(rectBefore));
        assertEquals(new Rectangle(100, 80, width, height), rectAfter);
        rectBefore = new Rectangle(0, 0, width, height);
        rectToPass = new Rectangle(rectBefore);
        rectAfter = SwingUtilities.convertRectangle(panel2, rectToPass, null);
        assertEquals(rectBefore, rectToPass);
        assertFalse(rectAfter.equals(rectBefore));
        assertEquals(new Rectangle(0, 20, width, height), rectAfter);
        rectBefore = new Rectangle(0, 0, width, height);
        rectToPass = new Rectangle(rectBefore);
        rectAfter = SwingUtilities.convertRectangle(null, rectToPass, panel2);
        assertEquals(rectBefore, rectToPass);
        assertFalse(rectAfter.equals(rectBefore));
        assertEquals(new Rectangle(0, -20, width, height), rectAfter);
    }

    /*
     * Class under test for Point convertPoint(Component, int, int, Component)
     * this function is being tested by testConvertPointComponentPointComponent()
     */
    public void testConvertPointComponentintintComponent() {
    }

    /*
     * Class under test for Point convertPoint(Component, Point, Component)
     */
    @SuppressWarnings("deprecation")
    public void testConvertPointComponentPointComponent() {
        Point pointBefore, pointToPass, pointAfter;
        JWindow window1 = new JWindow();
        JWindow window2 = new JWindow();
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        panel1.setPreferredSize(new Dimension(150, 150));
        panel2.setPreferredSize(new Dimension(100, 100));
        panel3.setPreferredSize(new Dimension(100, 100));
        panel1.add(panel2);
        panel1.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel2.setBorder(new EmptyBorder(25, 25, 25, 25));
        panel3.setBorder(new EmptyBorder(35, 35, 35, 35));
        window1.getContentPane().add(panel1);
        window1.setLocation(100, 100);
        window1.pack();
        window1.show();
        window2.getContentPane().add(panel3);
        window2.setLocation(200, 200);
        window2.pack();
        window2.show();
        pointAfter = new Point(1, 1);
        try {
            pointAfter = SwingUtilities.convertPoint(null, null, null);
        } catch (NullPointerException e) {
            fail("NPE should not be thrown");
        }
        assertNull(pointAfter);
        pointBefore = new Point(0, 0);
        pointToPass = new Point(pointBefore);
        pointAfter = SwingUtilities.convertPoint(null, pointBefore, null);
        assertTrue(pointBefore.equals(pointAfter));
        assertTrue(pointBefore.equals(pointToPass));
        pointBefore = new Point(0, 0);
        pointToPass = new Point(pointBefore);
        pointAfter = SwingUtilities.convertPoint(panel1, pointToPass, panel3);
        assertTrue(pointBefore.equals(pointToPass));
        assertFalse(pointAfter.equals(pointBefore));
        assertTrue(pointAfter.equals(new Point(-100, -100)));
        pointBefore = new Point(0, 0);
        pointToPass = new Point(pointBefore);
        pointAfter = SwingUtilities.convertPoint(panel3, pointToPass, panel2);
        assertEquals(pointBefore, pointToPass);
        assertFalse(pointAfter.equals(pointBefore));
        assertEquals(new Point(75, 80), pointAfter);
        pointBefore = new Point(0, 0);
        pointToPass = new Point(pointBefore);
        pointAfter = SwingUtilities.convertPoint(panel2, pointToPass, null);
        assertEquals(pointBefore, pointToPass);
        assertFalse(pointAfter.equals(pointBefore));
        assertEquals(new Point(25, 20), pointAfter);
        pointBefore = new Point(0, 0);
        pointToPass = new Point(pointBefore);
        pointAfter = SwingUtilities.convertPoint(null, pointToPass, panel2);
        assertEquals(pointBefore, pointToPass);
        assertFalse(pointAfter.equals(pointBefore));
        assertEquals(new Point(-25, -20), pointAfter);
    }

    public void testGetAccessibleAt() {
        // TODO uncomment when Accessibility is implemented
        /*
         class JComponentInaccessible extends JComponent {
         public String getUIClassID() {
         return "PanelUI";
         }
         public void updateUI() {
         setUI((PanelUI)UIManager.getUI(this));
         }
         public JComponentInaccessible() {
         setDoubleBuffered(true);
         setOpaque(true);
         }
         };
         JComponentInaccessible inaccessible = new JComponentInaccessible();
         Point checkPoint = null;
         JWindow window1 = new JWindow();
         JComponent panel1 = new JPanel();
         JComponent panel2 = new JPanel();
         JComponent panel3 = new JPanel();
         panel1.setBackground(Color.GREEN);
         panel2.setPreferredSize(new Dimension(101, 101));
         panel2.setBackground(Color.YELLOW);
         panel3.setPreferredSize(new Dimension(110, 110));
         panel3.setBackground(Color.WHITE);
         inaccessible.setBackground(Color.RED);
         inaccessible.setForeground(Color.RED);

         inaccessible.setPreferredSize(new Dimension(51, 51));

         panel1.add(panel3);
         panel1.add(panel2);
         panel1.add(inaccessible);
         panel1.setBorder(new EmptyBorder(15, 15, 15, 15));
         panel2.setBorder(new EmptyBorder(26, 26, 26, 26));
         panel3.setBorder(new EmptyBorder(37, 37, 37, 37));
         window1.getContentPane().add(panel1);
         window1.pack();
         window1.show();
         window1.hide();
         window1.show();

         boolean thrown = false;
         try {
         SwingUtilities.getAccessibleAt(null, checkPoint);
         } catch (NullPointerException e) {
         thrown = true;
         }
         assertFalse(thrown);
         thrown = false;
         try {
         SwingUtilities.getAccessibleAt(panel1, null);
         } catch (NullPointerException e) {
         thrown = true;
         }
         assertTrue(thrown);

         checkPoint = SwingUtilities.convertPoint(panel2, 5, 5, panel1);
         assertEquals(panel1, SwingUtilities.getAccessibleAt(panel1, checkPoint));

         checkPoint = SwingUtilities.convertPoint(panel3, 5, 5, panel1);
         assertEquals(panel3, SwingUtilities.getAccessibleAt(panel1, checkPoint));

         checkPoint = SwingUtilities.convertPoint(inaccessible, 5, 5, panel1);
         assertEquals(panel1, SwingUtilities.getAccessibleAt(panel1, checkPoint));
         */
    }

    public void testCalculateInnerArea() {
        JWindow window = new JWindow();
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        Rectangle rect = new Rectangle();
        Rectangle rect1 = new Rectangle(20, 10, 260, 110);
        Rectangle rect2 = new Rectangle(0, 0, 100, 100);
        Rectangle rect3 = new Rectangle(0, 0, 50, 50);
        panel1.setBorder(new EmptyBorder(10, 20, 30, 40));
        panel2.setPreferredSize(new Dimension(100, 100));
        component1.setPreferredSize(new Dimension(50, 50));
        component2.setPreferredSize(new Dimension(60, 60));
        component3.setPreferredSize(new Dimension(70, 70));
        panel1.add(panel2);
        panel2.add(component1);
        panel3.add(component3);
        panel1.add(component2);
        panel1.add(panel3);
        window.getContentPane().add(panel1);
        window.pack();
        assertNull(SwingUtilities.calculateInnerArea(null, rect));
        assertNull(SwingUtilities.calculateInnerArea(null, null));
        assertEquals(rect2, SwingUtilities.calculateInnerArea(panel2, rect));
        assertEquals(rect2, SwingUtilities.calculateInnerArea(panel2, rect));
        assertEquals(rect3, SwingUtilities.calculateInnerArea(component1, rect));
        if (isHarmony()) {
            assertEquals(rect1, SwingUtilities.calculateInnerArea(panel1, null));
            assertEquals(rect1, SwingUtilities.calculateInnerArea(panel1, rect));
        }
    }

    protected boolean contains(final Rectangle[] rects, final Point point) {
        if (rects != null) {
            for (int i = 0; i < rects.length; i++) {
                if (rects[i].contains(point)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void testComputeDifference() {
        Rectangle rect11 = new Rectangle(0, 0, 300, 300);
        Rectangle rect12 = new Rectangle(100, 100, 100, 100);
        // different null testcases
        Rectangle rects[] = null;
        rects = SwingUtilities.computeDifference(rect12, null);
        assertTrue(rects != null && rects.length == 0);
        rects = SwingUtilities.computeDifference(null, null);
        assertTrue(rects != null && rects.length == 0);
        // real recatangles testcases
        rects = SwingUtilities.computeDifference(rect12, rect11);
        assertTrue(rects != null && rects.length == 0);
        rects = SwingUtilities.computeDifference(rect11, rect12);
        assertTrue(rects != null && rects.length == 4);
        assertTrue(contains(rects, new Point(0, 0)));
        assertTrue(contains(rects, new Point(10, 150)));
        assertTrue(contains(rects, new Point(10, 270)));
        assertTrue(contains(rects, new Point(150, 20)));
        assertFalse(contains(rects, new Point(150, 120)));
        assertTrue(contains(rects, new Point(150, 220)));
        assertTrue(contains(rects, new Point(220, 0)));
        assertTrue(contains(rects, new Point(210, 150)));
        assertTrue(contains(rects, new Point(210, 280)));
        Rectangle rect21 = new Rectangle(0, 0, 300, 300);
        Rectangle rect22 = new Rectangle(0, 310, 300, 300);
        rects = SwingUtilities.computeDifference(rect21, rect22);
        assertTrue(rects != null && (rects.length == 0));
        Rectangle rect31 = new Rectangle(100, 100, 100, 100);
        Rectangle rect32 = new Rectangle(50, 50, 100, 100);
        rects = SwingUtilities.computeDifference(rect31, rect32);
        assertTrue(rects != null && rects.length == 2);
        assertFalse(contains(rects, new Point(60, 50)));
        assertFalse(contains(rects, new Point(60, 140)));
        assertFalse(contains(rects, new Point(60, 190)));
        assertFalse(contains(rects, new Point(110, 50)));
        assertFalse(contains(rects, new Point(110, 140)));
        assertTrue(contains(rects, new Point(110, 190)));
        assertFalse(contains(rects, new Point(160, 60)));
        assertTrue(contains(rects, new Point(160, 140)));
        assertTrue(contains(rects, new Point(160, 190)));
        Rectangle rect41 = new Rectangle(50, 50, 100, 100);
        Rectangle rect42 = new Rectangle(100, 100, 100, 100);
        rects = SwingUtilities.computeDifference(rect41, rect42);
        assertTrue(rects != null && rects.length == 2);
        assertTrue(contains(rects, new Point(60, 50)));
        assertTrue(contains(rects, new Point(60, 140)));
        assertFalse(contains(rects, new Point(60, 190)));
        assertTrue(contains(rects, new Point(110, 50)));
        assertFalse(contains(rects, new Point(110, 140)));
        assertFalse(contains(rects, new Point(110, 190)));
        assertFalse(contains(rects, new Point(160, 60)));
        assertFalse(contains(rects, new Point(160, 140)));
        assertFalse(contains(rects, new Point(160, 190)));
    }

    public void testGetAncestorNamed() {
        String name1 = "name1";
        String name2 = "name2";
        String name3 = "name3";
        String name4 = "name4";
        String name5 = "name5";
        String name6 = "name6";
        Window window = new Window(new Frame(name1));
        window.setName(name2);
        JPanel panel1 = new JPanel();
        panel1.setName(name3);
        JPanel panel2 = new JPanel();
        panel2.setName(name4);
        JPanel panel3 = new JPanel();
        panel3.setName(name5);
        Component component1 = new Canvas();//new Button();
        component1.setName(name6);
        Component component2 = new JPanel();
        Component component3 = new JPanel();
        JPanel panel4 = new JPanel();
        assertNull(SwingUtilities.getAncestorNamed(name1, null));
        assertNull(SwingUtilities.getAncestorNamed(null, component3));
        assertNull(SwingUtilities.getAncestorNamed(null, null));
        panel4.add(panel1);
        panel1.add(panel2);
        panel2.add(component1);
        panel3.add(component3);
        window.add(panel4);
        window.add(component2);
        assertTrue(SwingUtilities.getAncestorNamed(name3, component1) == panel1);
        assertNull(SwingUtilities.getAncestorNamed(name6, component1));
        assertTrue(SwingUtilities.getAncestorNamed(name2, component1) == window);
        assertNull(SwingUtilities.getAncestorNamed(name5, component1));
        assertTrue(SwingUtilities.getAncestorNamed(name2, panel1) == window);
        assertTrue(SwingUtilities.getAncestorNamed(name2, component2) == window);
        assertTrue(SwingUtilities.getAncestorNamed(name5, component3) == panel3);
        assertNull(SwingUtilities.getAncestorNamed(null, component1));
    }

    public void testGetAncestorOfClass() {
        Window window = new Window(new Frame());
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        JPanel panel3 = new JPanel();
        Component component1 = new Canvas();
        Component component2 = new Panel();
        Component component3 = new Panel();
        assertNull(SwingUtilities.getAncestorOfClass(JPanel.class, null));
        assertNull(SwingUtilities.getAncestorOfClass(null, component3));
        assertNull(SwingUtilities.getAncestorOfClass(null, null));
        panel1.add(panel2);
        panel2.add(component1);
        panel3.add(component3);
        window.add(panel1);
        window.add(component2);
        assertTrue(SwingUtilities.getAncestorOfClass(Window.class, component1) == window);
        assertTrue(SwingUtilities.getAncestorOfClass(JPanel.class, component1) == panel2);
        assertTrue(SwingUtilities.getAncestorOfClass(JPanel.class, panel2) == panel1);
        assertTrue(SwingUtilities.getAncestorOfClass(Window.class, component2) == window);
        assertNull(SwingUtilities.getAncestorOfClass(Window.class, component3));
        assertTrue(SwingUtilities.getAncestorOfClass(JPanel.class, component3) == panel3);
        class CustomPanel extends JPanel {
            private static final long serialVersionUID = 1L;
        }
        JPanel customPanel = new CustomPanel();
        JPanel childPanel = new JPanel();
        customPanel.add(childPanel);
        assertTrue(SwingUtilities.getAncestorOfClass(JPanel.class, childPanel) == customPanel);
    }

    /*
     * this method is being tested by testPaintComponentGraphicsComponentContainerintintintint()
     */
    public void testPaintComponentGraphicsComponentContainerRectangle() {
    }

    /*
     * Class under test for void paintComponent(Graphics, Component, Container, int, int, int, int)
     */
    public void testPaintComponentGraphicsComponentContainerintintintint() {
        //        final JComponent component = new JButton("JButton");
        //        JFrame window = new JFrame() {
        //            public void paint(Graphics g) {
        //                SwingUtilities.paintComponent(g, component, new Container(), 25, 25, 50, 50);
        //            }
        //        };
        //        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //        window.setSize(150, 150);
        //        window.show();
        //        component.setPreferredSize(new Dimension(70, 70));
        //        component.setForeground(Color.RED);
        //        component.setBackground(Color.YELLOW);
        //        JPanel panel =  new JPanel();
        //        window.getContentPane().add(panel);
        //        while (!window.isActive());
        //        while (window.isActive());
    }

    public void testGetRootPane() {
        assertNull(SwingUtilities.getRootPane(null));
        assertNull(SwingUtilities.getRootPane(new JButton()));
        JWindow window = new JWindow(new Frame());
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        JPanel panel3 = new JPanel();
        JRootPane pane = new JRootPane();
        Component component1 = new Button();
        Component component2 = new JButton();
        Component component3 = new JButton();
        panel1.add(panel2);
        panel2.add(component1);
        panel3.add(component3);
        window.getContentPane().add(panel1);
        window.getContentPane().add(component2);
        assertEquals(window.getRootPane(), SwingUtilities.getRootPane(component1));
        assertNull(SwingUtilities.getRootPane(component3));
        assertEquals(window.getRootPane(), SwingUtilities.getRootPane(component2));
        assertEquals(window.getRootPane(), SwingUtilities.getRootPane(window));
        assertSame(pane, SwingUtilities.getRootPane(pane));
    }

    public void testReplaceUIInputMap() {
        UIInputMap uiInputMap1 = new UIInputMap();
        UIInputMap uiInputMap2 = new UIInputMap();
        JComponent component = new JPanel();
        InputMap initialMap = component.getInputMap(JComponent.WHEN_FOCUSED);
        InputMap anotherMap = new InputMap();
        component.setInputMap(JComponent.WHEN_FOCUSED, null);
        component.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
        assertNull(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED));
        SwingUtilities.replaceUIInputMap(component, JComponent.WHEN_FOCUSED, uiInputMap1);
        SwingUtilities.replaceUIInputMap(component,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, uiInputMap2);
        assertNull(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED));
        assertNull(SwingUtilities.getUIInputMap(component,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        component.setInputMap(JComponent.WHEN_FOCUSED, initialMap);
        component.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, anotherMap);
        SwingUtilities.replaceUIInputMap(component, JComponent.WHEN_FOCUSED, uiInputMap1);
        assertTrue(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED) == uiInputMap1);
        SwingUtilities.replaceUIInputMap(component, JComponent.WHEN_FOCUSED, uiInputMap2);
        assertTrue(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED) == uiInputMap2);
        SwingUtilities.replaceUIInputMap(component,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, uiInputMap2);
        assertTrue(SwingUtilities.getUIInputMap(component,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) == uiInputMap2);
        anotherMap.setParent(null);
        initialMap.setParent(anotherMap);
        assertNull(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED));
        SwingUtilities.replaceUIInputMap(component, JComponent.WHEN_FOCUSED, uiInputMap2);
        assertTrue(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED) == uiInputMap2);
    }

    public void testGetUIInputMap() {
        UIInputMap uiInputMap1 = new UIInputMap();
        UIInputMap uiInputMap2 = new UIInputMap();
        JComponent component = new JPanel();
        InputMap initialMap = component.getInputMap();
        InputMap anotherMap = new InputMap();
        component.setInputMap(JComponent.WHEN_FOCUSED, null);
        assertNull(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED));
        assertNull(SwingUtilities.getUIInputMap(component,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        component.setInputMap(JComponent.WHEN_FOCUSED, uiInputMap1);
        assertNull(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED));
        component.setInputMap(JComponent.WHEN_FOCUSED, anotherMap);
        component.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, anotherMap);
        assertNull(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED));
        anotherMap.setParent(initialMap);
        assertNull(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED));
        anotherMap.setParent(uiInputMap1);
        assertTrue(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED) == uiInputMap1);
        assertTrue(SwingUtilities.getUIInputMap(component,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) == uiInputMap1);
        uiInputMap1.setParent(uiInputMap2);
        assertTrue(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED) == uiInputMap1);
        assertTrue(SwingUtilities.getUIInputMap(component,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) == uiInputMap1);
        anotherMap.setParent(initialMap);
        initialMap.setParent(uiInputMap1);
        assertTrue(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED) == uiInputMap1);
        initialMap.setParent(uiInputMap2);
        assertTrue(SwingUtilities.getUIInputMap(component, JComponent.WHEN_FOCUSED) == uiInputMap2);
    }

    public void testReplaceUIActionMap() {
        UIActionMap uiActionMap1 = new UIActionMap();
        UIActionMap uiActionMap2 = new UIActionMap();
        JComponent component = new JPanel();
        ActionMap initialMap = component.getActionMap();
        ActionMap anotherMap = new ActionMap();
        component.setActionMap(null);
        assertNull(SwingUtilities.getUIActionMap(component));
        SwingUtilities.replaceUIActionMap(component, uiActionMap1);
        assertNull(SwingUtilities.getUIActionMap(component));
        component.setActionMap(initialMap);
        SwingUtilities.replaceUIActionMap(component, uiActionMap1);
        assertTrue(SwingUtilities.getUIActionMap(component) == uiActionMap1);
        SwingUtilities.replaceUIActionMap(component, uiActionMap2);
        assertTrue(SwingUtilities.getUIActionMap(component) == uiActionMap2);
        initialMap.setParent(anotherMap);
        assertNull(SwingUtilities.getUIActionMap(component));
        SwingUtilities.replaceUIActionMap(component, uiActionMap2);
        assertTrue(SwingUtilities.getUIActionMap(component) == uiActionMap2);
    }

    public void testGetUIActionMap() {
        UIActionMap uiActionMap1 = new UIActionMap();
        UIActionMap uiActionMap2 = new UIActionMap();
        JComponent component = new JPanel();
        ActionMap initialMap = component.getActionMap();
        ActionMap anotherMap = new ActionMap();
        component.setActionMap(null);
        assertNull(SwingUtilities.getUIActionMap(component));
        component.setActionMap(uiActionMap1);
        assertNull(SwingUtilities.getUIActionMap(component));
        component.setActionMap(anotherMap);
        assertNull(SwingUtilities.getUIActionMap(component));
        anotherMap.setParent(initialMap);
        assertNull(SwingUtilities.getUIActionMap(component));
        anotherMap.setParent(uiActionMap1);
        assertTrue(SwingUtilities.getUIActionMap(component) == uiActionMap1);
        uiActionMap1.setParent(uiActionMap2);
        assertTrue(SwingUtilities.getUIActionMap(component) == uiActionMap1);
        anotherMap.setParent(initialMap);
        initialMap.setParent(uiActionMap1);
        assertTrue(SwingUtilities.getUIActionMap(component) == uiActionMap1);
        initialMap.setParent(uiActionMap2);
        assertTrue(SwingUtilities.getUIActionMap(component) == uiActionMap2);
    }

    public void testGetAccessibleStateSet() {
        Component component1 = new JPanel();
        Component component3 = new JDialog();
        AccessibleStateSet stateSet = SwingUtilities.getAccessibleStateSet(component1);
        assertTrue(stateSet.contains(AccessibleState.ENABLED));
        assertTrue(stateSet.contains(AccessibleState.FOCUSABLE));
        assertTrue(stateSet.contains(AccessibleState.VISIBLE));
        assertTrue(stateSet.contains(AccessibleState.OPAQUE));
        assertTrue(stateSet.toArray().length == 4);
        stateSet = SwingUtilities.getAccessibleStateSet(component3);
        assertTrue(stateSet.contains(AccessibleState.ENABLED));
        assertTrue(stateSet.contains(AccessibleState.FOCUSABLE));
        assertTrue(stateSet.contains(AccessibleState.RESIZABLE));
        //        assertTrue(stateSet.toArray().length == 3);
    }

    public void testGetAccessibleChild() {
        assertTrue(SwingUtilities.getAccessibleIndexInParent(new JButton()) == -1);
        JPanel panel = new JPanel();
        assertTrue(SwingUtilities.getAccessibleIndexInParent(panel) == -1);
        Component component1 = new JPanel();
        panel.add(component1);
        assertTrue("Accessible component found in parent", SwingUtilities.getAccessibleChild(
                panel, 0) == component1);
        Component component2 = new Container();
        panel.add(component2);
        assertNull("inAccessible component not found in parent", SwingUtilities
                .getAccessibleChild(panel, 1));
        Component component3 = new JPanel();
        panel.add(component3);
        assertTrue("Accessible component found in parent", SwingUtilities.getAccessibleChild(
                panel, 1) == component3);
        assertTrue("Accessible component found in parent", SwingUtilities.getAccessibleChild(
                panel, 0) == component1);
    }

    public void testComputeStringWidth() {
        JFrame frame = new JFrame();
        JWindow window = new JWindow(frame);
        window.setVisible(true);
        final FontMetrics metrics = window.getGraphics().getFontMetrics();
        //        String string1 = "string1";
        //        String string2 = "string1string1";
        //        String string3 = "";
        //        String string4 = "    ";
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.computeStringWidth(metrics, null);
            }
        });
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.computeStringWidth(null, "string");
            }
        });
        //        assertTrue(SwingUtilities.computeStringWidth(metrics, string1) == 38);
        //        assertTrue(SwingUtilities.computeStringWidth(metrics, string2) == 76);
        //        assertTrue(SwingUtilities.computeStringWidth(metrics, string3) == 0);
        //        assertTrue(SwingUtilities.computeStringWidth(metrics, string4) == 12);
        frame.dispose();
    }

    public void testWindowForComponent() {
        Window window = new Window(new Frame());
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        JPanel panel3 = new JPanel();
        Component component1 = new Button();
        Component component2 = new JButton();
        Component component3 = new JButton();
        assertNull(SwingUtilities.windowForComponent(component1));
        panel1.add(panel2);
        panel2.add(component1);
        panel3.add(component3);
        window.add(panel1);
        window.add(component2);
        assertTrue(SwingUtilities.windowForComponent(component1) == window);
        assertTrue(SwingUtilities.windowForComponent(component2) == window);
        assertNull(SwingUtilities.windowForComponent(component3));
        assertTrue(SwingUtilities.windowForComponent(window) != window);
    }

    public void testGetWindowAncestor() {
        Window window = new Window(new Frame());
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        JPanel panel3 = new JPanel();
        Component component1 = new Button();
        Component component2 = new JButton();
        Component component3 = new JButton();
        /**
         boolean thrown = false;
         try {
         SwingUtilities.getWindowAncestor(null);
         } catch (NullPointerException e) {
         thrown = true;
         }
         assertTrue(thrown);
         */
        assertNull(SwingUtilities.getWindowAncestor(component1));
        panel1.add(panel2);
        panel2.add(component1);
        panel3.add(component3);
        window.add(panel1);
        window.add(component2);
        assertTrue(SwingUtilities.getWindowAncestor(component1) == window);
        assertTrue(SwingUtilities.getWindowAncestor(component2) == window);
        assertNull(SwingUtilities.getWindowAncestor(component3));
        assertTrue(SwingUtilities.getWindowAncestor(window) != window);
    }

    public void testIsRectangleContainingRectangle() {
        Rectangle rect1 = new Rectangle(100, 100, 100, 100);
        Rectangle rect2 = new Rectangle(150, 150, 30, 30);
        Rectangle rect3 = new Rectangle(150, 150, 50, 50);
        Rectangle rect4 = new Rectangle(0, 0, 150, 150);
        Rectangle rect5 = new Rectangle(100, 100, 1, 1);
        assertTrue(SwingUtilities.isRectangleContainingRectangle(rect1, rect2));
        assertTrue(SwingUtilities.isRectangleContainingRectangle(rect1, rect5));
        assertFalse(SwingUtilities.isRectangleContainingRectangle(rect2, rect1));
        assertTrue(SwingUtilities.isRectangleContainingRectangle(rect1, rect3));
        assertTrue(SwingUtilities.isRectangleContainingRectangle(rect3, rect2));
        assertTrue(SwingUtilities.isRectangleContainingRectangle(rect3, rect3));
        assertFalse(SwingUtilities.isRectangleContainingRectangle(rect1, rect4));
        assertFalse(SwingUtilities.isRectangleContainingRectangle(rect4, rect1));
    }

    public void testComputeUnion() {
        assertTrue(SwingUtilities.computeUnion(0, 0, 100, 100,
                new Rectangle(100, 100, 100, 100)).equals(new Rectangle(0, 0, 200, 200)));
        assertTrue(SwingUtilities.computeUnion(100, 100, 100, 100,
                new Rectangle(0, 0, 100, 100)).equals(new Rectangle(0, 0, 200, 200)));
        assertTrue(SwingUtilities.computeUnion(0, 0, 100, 100,
                new Rectangle(200, 200, 100, 100)).equals(new Rectangle(0, 0, 300, 300)));
        assertTrue(SwingUtilities.computeUnion(200, 200, 100, 100,
                new Rectangle(0, 0, 100, 100)).equals(new Rectangle(0, 0, 300, 300)));
        assertTrue(SwingUtilities.computeUnion(200, 200, 100, 100,
                new Rectangle(220, 220, 10, 10)).equals(new Rectangle(200, 200, 100, 100)));
        assertTrue(SwingUtilities.computeUnion(200, 200, 100, 100,
                new Rectangle(180, 220, 10, 100)).equals(new Rectangle(180, 200, 120, 120)));
    }

    public void testComputeIntersection() {
        assertEquals(new Dimension(0, 0), SwingUtilities.computeIntersection(0, 0, 100, 100,
                new Rectangle(100, 100, 100, 100)).getSize());
        assertEquals(new Rectangle(150, 150, 20, 20), SwingUtilities.computeIntersection(100,
                100, 100, 100, new Rectangle(150, 150, 20, 20)));
        assertEquals(new Rectangle(150, 150, 20, 20), SwingUtilities.computeIntersection(150,
                150, 20, 20, new Rectangle(100, 100, 100, 100)));
        assertEquals(new Rectangle(0, 0, 0, 0), SwingUtilities.computeIntersection(0, 0, 100,
                100, new Rectangle(200, 200, 100, 100)));
        assertEquals(new Rectangle(0, 0, 0, 0), SwingUtilities.computeIntersection(0, 0, 100,
                100, new Rectangle(0, 101, 100, 100)));
        assertEquals(new Rectangle(0, 0, 0, 0), SwingUtilities.computeIntersection(0, 0, 100,
                100, new Rectangle(101, 0, 100, 100)));
        assertEquals(new Rectangle(200, 200, 50, 50), SwingUtilities.computeIntersection(200,
                200, 100, 100, new Rectangle(150, 150, 100, 100)));
        assertEquals(new Rectangle(200, 200, 50, 50), SwingUtilities.computeIntersection(150,
                150, 100, 100, new Rectangle(200, 200, 100, 100)));
    }

    public void testGetLocalBounds() {
        int width = 200;
        int height = 200;
        Component component = new JPanel();
        component.setSize(width, height);
        Rectangle bounds = SwingUtilities.getLocalBounds(component);
        assertTrue(bounds.x == 0);
        assertTrue(bounds.y == 0);
        assertTrue(bounds.width == width);
        assertTrue(bounds.height == height);
        component.setBounds(width, height, width, height);
        bounds = SwingUtilities.getLocalBounds(component);
        assertTrue(bounds.x == 0);
        assertTrue(bounds.y == 0);
        assertTrue(bounds.width == width);
        assertTrue(bounds.height == height);
    }

    public void testConvertPointToScreen() {
        Point point;
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        panel1.setSize(100, 100);
        panel1.setLocation(50, 50);
        panel1.add(panel2);
        panel2.setSize(200, 200);
        panel2.setLocation(70, 70);
        point = new Point(0, 0);
        SwingUtilities.convertPointToScreen(point, panel1);
        assertTrue(point.equals(new Point(50, 50)));
        point = new Point(0, 0);
        SwingUtilities.convertPointToScreen(point, panel2);
        assertTrue(point.equals(new Point(120, 120)));
        point = new Point(66, 66);
        SwingUtilities.convertPointToScreen(point, panel2);
        assertTrue(point.equals(new Point(186, 186)));
    }

    public void testConvertPointFromScreen() {
        Point point;
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        panel1.setSize(100, 100);
        panel1.setLocation(50, 50);
        panel1.add(panel2);
        panel2.setSize(200, 200);
        panel2.setLocation(70, 70);
        point = new Point(50, 50);
        SwingUtilities.convertPointFromScreen(point, panel1);
        assertTrue(point.equals(new Point(0, 0)));
        point = new Point(120, 120);
        SwingUtilities.convertPointFromScreen(point, panel2);
        assertTrue(point.equals(new Point(0, 0)));
        point = new Point(186, 186);
        SwingUtilities.convertPointFromScreen(point, panel2);
        assertTrue(point.equals(new Point(66, 66)));
    }

    public void testIsDescendingFrom() {
        final Window window = new Window(new Frame());
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        JPanel panel3 = new JPanel();
        Component component1 = new Button();
        Component component2 = new JButton();
        Component component3 = new JButton();
        panel1.add(panel2);
        panel2.add(component1);
        panel3.add(component3);
        window.add(panel1);
        window.add(component2);
        assertTrue(SwingUtilities.isDescendingFrom(null, null));
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.isDescendingFrom(null, window);
            }
        });
        assertFalse(SwingUtilities.isDescendingFrom(component1, null));
        assertTrue(SwingUtilities.isDescendingFrom(window, window));
        assertTrue(SwingUtilities.isDescendingFrom(component1, window));
        assertTrue(SwingUtilities.isDescendingFrom(component2, window));
        assertFalse(SwingUtilities.isDescendingFrom(component3, window));
        assertTrue(SwingUtilities.isDescendingFrom(panel1, window));
        assertTrue(SwingUtilities.isDescendingFrom(panel2, window));
        assertFalse(SwingUtilities.isDescendingFrom(panel3, window));
    }

    @SuppressWarnings("deprecation")
    public void testGetDeepestComponentAt() {
        if (isHarmony()) {
            return;
        }
        JDialog window = new JDialog();
        JPanel panel1 = new JPanel();
        JPanel panel2 = new JPanel();
        Component component1 = new JButton("1");
        Component component2 = new JButton("2");
        Component component3 = new JButton("3");
        panel1.add(panel2);
        panel2.add(component1);
        panel1.add(component3);
        panel1.add(component2);
        window.getContentPane().add(panel1);
        window.pack();
        window.show();
        assertTrue(SwingUtilities.getDeepestComponentAt(window.getContentPane(), 0, 0) == panel1);
        assertTrue(SwingUtilities.getDeepestComponentAt(window.getContentPane(), 5, 5) == panel2);
        assertTrue(SwingUtilities.getDeepestComponentAt(window.getContentPane(), 10, 10) == component1);
        assertTrue(SwingUtilities.getDeepestComponentAt(window.getContentPane(), 100, 20) == component3);
        assertTrue(SwingUtilities.getDeepestComponentAt(window.getContentPane(), 110, 20) == component2);
        assertNull(SwingUtilities.getDeepestComponentAt(window.getContentPane(), 210, 20));
        assertTrue(SwingUtilities.getDeepestComponentAt(window, 100, 20) == window);
    }

    public void testGetRoot() {
        JWindow window1 = new JWindow();
        // Code should be enabled when JApplet is supported.
        //JApplet window2 = new JApplet();
        JWindow window2 = new JWindow();
        JComponent panel1 = new JPanel();
        JComponent panel2 = new JPanel();
        JComponent panel3 = new JPanel();
        JComponent panel4 = new JPanel();
        panel1.setSize(new Dimension(100, 100));
        panel1.setBackground(Color.GREEN);
        panel2.setSize(new Dimension(101, 101));
        panel2.setBackground(Color.YELLOW);
        panel3.setPreferredSize(new Dimension(110, 110));
        panel3.setBackground(Color.WHITE);
        panel1.setLocation(50, 50);
        panel2.setLocation(70, 70);
        panel3.setLocation(150, 150);
        panel1.add(panel2);
        panel1.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel2.setBorder(new EmptyBorder(27, 27, 27, 27));
        panel3.setBorder(new EmptyBorder(17, 17, 17, 17));
        window1.setLocation(100, 100);
        window1.getContentPane().add(panel1);
        window2.getContentPane().add(panel4);
        assertEquals(window1, SwingUtilities.getRoot(panel1));
        assertEquals(window1, SwingUtilities.getRoot(panel2));
        assertNull(SwingUtilities.getRoot(panel3));
        assertEquals(window2, SwingUtilities.getRoot(panel4));
        assertEquals(window1, SwingUtilities.getRoot(window1));
        assertEquals(window2, SwingUtilities.getRoot(window2));
    }

    /**
     * this method is supposed to be tested by FocusManager.getCurrentManager().getFocusOwner()
     */
    public void testFindFocusOwner() {
    }

    /**
     * this method is supposed to be tested by EventQueue.invokeLater()
     */
    public void testInvokeLater() {
    }

    /**
     * this method is supposed to be tested by EventQueue.invokeAndWait()
     */
    public void testInvokeAndWait() {
    }

    public void testIsRightMouseButton() {
        JComponent panel = new JPanel();
        panel.setPreferredSize(new Dimension(100, 100));
        MouseEvent event1 = new MouseEvent(panel, 100, 100, InputEvent.BUTTON1_DOWN_MASK, 50,
                50, 1, false);
        MouseEvent event2 = new MouseEvent(panel, 100, 100, InputEvent.BUTTON2_DOWN_MASK, 50,
                50, 1, false);
        MouseEvent event3 = new MouseEvent(panel, 100, 100, InputEvent.BUTTON3_DOWN_MASK, 50,
                50, 1, false);
        assertFalse(SwingUtilities.isRightMouseButton(event1));
        assertFalse(SwingUtilities.isRightMouseButton(event2));
        assertTrue(SwingUtilities.isRightMouseButton(event3));
    }

    public void testIsMiddleMouseButton() {
        JComponent panel = new JPanel();
        panel.setPreferredSize(new Dimension(100, 100));
        MouseEvent event1 = new MouseEvent(panel, 100, 100, InputEvent.BUTTON1_DOWN_MASK, 50,
                50, 1, false);
        MouseEvent event2 = new MouseEvent(panel, 100, 100, InputEvent.BUTTON2_DOWN_MASK, 50,
                50, 1, false);
        MouseEvent event3 = new MouseEvent(panel, 100, 100, InputEvent.BUTTON3_DOWN_MASK, 50,
                50, 1, false);
        assertFalse(SwingUtilities.isMiddleMouseButton(event1));
        assertTrue(SwingUtilities.isMiddleMouseButton(event2));
        assertFalse(SwingUtilities.isMiddleMouseButton(event3));
    }

    public void testIsLeftMouseButton() {
        JComponent panel = new JPanel();
        panel.setPreferredSize(new Dimension(100, 100));
        MouseEvent event1 = new MouseEvent(panel, 100, 100, InputEvent.BUTTON1_DOWN_MASK, 50,
                50, 1, false);
        MouseEvent event2 = new MouseEvent(panel, 100, 100, InputEvent.BUTTON2_DOWN_MASK, 50,
                50, 1, false);
        MouseEvent event3 = new MouseEvent(panel, 100, 100, InputEvent.BUTTON3_DOWN_MASK, 50,
                50, 1, false);
        assertTrue(SwingUtilities.isLeftMouseButton(event1));
        assertFalse(SwingUtilities.isLeftMouseButton(event2));
        assertFalse(SwingUtilities.isLeftMouseButton(event3));
    }

    public void testProcessKeyBindings() {
        class ActionListenerDummy implements ActionListener {
            public ActionEvent event = null;

            public void actionPerformed(final ActionEvent e) {
                event = e;
            }
        }
        ;
        ActionListenerDummy action1 = new ActionListenerDummy();
        ActionListenerDummy action2 = new ActionListenerDummy();
        ActionListenerDummy action3 = new ActionListenerDummy();
        ActionListenerDummy action41 = new ActionListenerDummy();
        ActionListenerDummy action42 = new ActionListenerDummy();
        ActionListenerDummy action51 = new ActionListenerDummy();
        ActionListenerDummy action52 = new ActionListenerDummy();
        ActionListenerDummy action53 = new ActionListenerDummy();
        ActionListenerDummy action54 = new ActionListenerDummy();
        JComponent component1 = new JPanel();
        Component component2 = new Panel();
        JComponent component3 = new JButton("3");
        JWindow component4 = new JWindow();
        component4.getContentPane().add(component1);
        component1.add(component2);
        component1.add(component3);
        KeyEvent event1 = new KeyEvent(component1, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_A,
                'a');
        KeyEvent event2 = new KeyEvent(component2, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_B,
                'b');
        KeyEvent event3 = new KeyEvent(component3, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_C,
                'c');
        KeyEvent event4 = new KeyEvent(component2, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_D,
                'd');
        KeyEvent event5 = new KeyEvent(component1, KeyEvent.KEY_PRESSED, 0, 0,
                KeyEvent.VK_ENTER, '\n');
        KeyStroke keyStroke1 = KeyStroke.getKeyStrokeForEvent(event1);
        KeyStroke keyStroke2 = KeyStroke.getKeyStrokeForEvent(event2);
        KeyStroke keyStroke3 = KeyStroke.getKeyStrokeForEvent(event3);
        KeyStroke keyStroke4 = KeyStroke.getKeyStrokeForEvent(event4);
        KeyStroke keyStroke5 = KeyStroke.getKeyStrokeForEvent(event5);
        component1.registerKeyboardAction(action1, keyStroke1, JComponent.WHEN_FOCUSED);
        component1.registerKeyboardAction(action2, keyStroke2,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        component3.registerKeyboardAction(action3, keyStroke3,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        component1.registerKeyboardAction(action41, keyStroke4,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        component3.registerKeyboardAction(action42, keyStroke4,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        component3.registerKeyboardAction(action53, keyStroke5, JComponent.WHEN_FOCUSED);
        component1.registerKeyboardAction(action51, keyStroke5,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        boolean result = SwingUtilities.processKeyBindings(event1);
        assertTrue(result);
        assertTrue("event1: actionPerformed called for component", action1.event != null);
        assertFalse(event1.isConsumed());
        action1.event = null;
        result = SwingUtilities.processKeyBindings(event2);
        assertTrue(result);
        assertNull("event2: wrong actionPerformed called for parent", action1.event);
        assertTrue("event2: right actionPerformed called for parent", action2.event != null);
        assertFalse(event2.isConsumed());
        action2.event = null;
        result = SwingUtilities.processKeyBindings(event3);
        assertTrue(result);
        assertNull("event3: actionPerformed called for parent", action1.event);
        assertNull("event3: actionPerformed called for brother", action2.event);
        assertTrue("event3: actionPerformed called for component", action3.event != null);
        assertFalse(event3.isConsumed());
        action3.event = null;
        result = SwingUtilities.processKeyBindings(event4);
        assertTrue(result);
        assertNull("event4: actionPerformed called for parent", action1.event);
        assertNull("event4: actionPerformed called for brother", action2.event);
        assertNull("event4: actionPerformed called for component", action3.event);
        assertTrue("event4: actionPerformed called for brother", action41.event != null);
        assertNull("event4: actionPerformed called for brother", action42.event);
        assertFalse(event4.isConsumed());
        result = SwingUtilities.processKeyBindings(event5);
        assertTrue(result);
        assertTrue("event5: actionPerformed called for parent", action51.event != null);
        assertNull("event5: actionPerformed called for parent", action53.event);
        assertFalse(event5.isConsumed());
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
        result = SwingUtilities.processKeyBindings(event6);
        assertTrue(result);
        assertTrue("event5: actionPerformed called for parent", action52.event != null);
        assertNull("event5: actionPerformed called for parent", action54.event);
        assertFalse(event5.isConsumed());
    }

    public void testUpdateComponentTreeUI() throws Exception {
        LookAndFeel laf = UIManager.getLookAndFeel();
        try {
            JPanel panel1 = new JPanel();
            JPanel panel2 = new JPanel();
            JButton button1 = new JButton("1");
            JButton button2 = new JButton("2");
            panel1.add(button1);
            panel1.add(button2);
            panel2.add(panel1);
            String lookAndFeel1 = "org.apache.harmony.x.swing.plaf.metal.MetalLookAndFeel";
            try {
                UIManager.setLookAndFeel(lookAndFeel1);
            } catch (ClassNotFoundException e) {
            } catch (UnsupportedLookAndFeelException e) {
            } catch (Exception e) {
            }
            ComponentUI ui1 = button1.getUI();
            ComponentUI ui2 = button2.getUI();
            ComponentUI ui3 = panel1.getUI();
            ComponentUI ui4 = panel2.getUI();
            assertTrue(ui1.getClass().getName().endsWith("MetalButtonUI"));
            assertTrue(ui2.getClass().getName().endsWith("MetalButtonUI"));
            assertTrue(ui3.getClass().getName().endsWith("BasicPanelUI"));
            assertTrue(ui4.getClass().getName().endsWith("BasicPanelUI"));
            button1.setUI(null);
            button2.setUI(null);
            panel1.setUI(null);
            panel2.setUI(null);
            SwingUtilities.updateComponentTreeUI(panel1);
            ui1 = button1.getUI();
            ui2 = button2.getUI();
            ui3 = panel1.getUI();
            ui4 = panel2.getUI();
            assertTrue(ui1.getClass().getName().endsWith("MetalButtonUI"));
            assertTrue(ui2.getClass().getName().endsWith("MetalButtonUI"));
            assertTrue(ui3.getClass().getName().endsWith("BasicPanelUI"));
            assertNull(ui4);
        } finally {
            UIManager.setLookAndFeel(laf);
        }
    }

    public void testGetAccessibleIndexInParent() {
        assertTrue(SwingUtilities.getAccessibleIndexInParent(new JButton()) == -1);
        JPanel panel = new JPanel();
        assertTrue(SwingUtilities.getAccessibleIndexInParent(panel) == -1);
        Component component = new JPanel();
        panel.add(component);
        assertTrue(SwingUtilities.getAccessibleIndexInParent(component) == 0);
        final Component container = new Container();
        panel.add(container);
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.getAccessibleIndexInParent(container);
            }
        });
        component = new JPanel();
        panel.add(component);
        assertTrue(SwingUtilities.getAccessibleIndexInParent(component) == 1);
    }

    public void testGetAccessibleChildrenCount() {
        testExceptionalCase(new NullPointerCase() {
            @Override
            public void exceptionalAction() throws Exception {
                SwingUtilities.getAccessibleChildrenCount(null);
            }
        });
        assertTrue(SwingUtilities.getAccessibleChildrenCount(new JButton()) == 0);
        JPanel panel = new JPanel();
        assertTrue(SwingUtilities.getAccessibleChildrenCount(panel) == 0);
        panel.add(new JPanel());
        assertTrue(SwingUtilities.getAccessibleChildrenCount(panel) == 1);
        panel.add(new Container());
        assertTrue(SwingUtilities.getAccessibleChildrenCount(panel) == 1);
        panel.add(new JPanel());
        assertTrue(SwingUtilities.getAccessibleChildrenCount(panel) == 2);
        panel.add(new Container());
        assertTrue(SwingUtilities.getAccessibleChildrenCount(panel) == 2);
    }

    public void testIsEventDispatchThread() {
        assertTrue(SwingUtilities.isEventDispatchThread());
    }
}
