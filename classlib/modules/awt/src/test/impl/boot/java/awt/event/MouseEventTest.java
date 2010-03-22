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
package java.awt.event;

import java.awt.Button;
import java.awt.Component;
import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;

import junit.framework.TestCase;

public class MouseEventTest extends TestCase {

    public final void testMouseEventComponentintlongintintintintboolean() {
        Button button = new Button("Button");
        MouseEvent event = new MouseEvent(button, MouseEvent.MOUSE_PRESSED, 1000000000,
                InputEvent.BUTTON2_DOWN_MASK, 100, 200,
                10, true);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), MouseEvent.MOUSE_PRESSED);
        assertEquals(event.getButton(), MouseEvent.NOBUTTON);
        assertEquals(event.getClickCount(), 10);
        assertEquals(event.getPoint(), new Point(100, 200));
        assertEquals(event.getX(), 100);
        assertEquals(event.getY(), 200);
        assertTrue(event.isPopupTrigger());
    }

    public final void testMouseEventComponentintlongintintintintbooleanint() {
        final Button button = new Button("Button");
        MouseEvent event = new MouseEvent(button, MouseEvent.MOUSE_PRESSED,
                1000000000, InputEvent.BUTTON2_DOWN_MASK, 100, 200, 10, true,
                MouseEvent.BUTTON1);

        assertEquals(button, event.getSource());
        assertEquals(MouseEvent.MOUSE_PRESSED, event.getID());
        assertEquals(MouseEvent.BUTTON1, event.getButton());
        assertEquals(10, event.getClickCount());
        assertEquals(new Point(100, 200), event.getPoint());
        assertEquals(100, event.getX());
        assertEquals(200, event.getY());
        assertTrue(event.isPopupTrigger());
        assertEquals(InputEvent.BUTTON1_MASK, event.getModifiers());
        assertEquals(InputEvent.BUTTON2_DOWN_MASK, event.getModifiersEx());

        event = new MouseEvent(button, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.BUTTON1_MASK, 0, 0, 0, true, MouseEvent.NOBUTTON);
        assertEquals(InputEvent.BUTTON1_MASK, event.getModifiers());
        assertEquals(InputEvent.BUTTON1_DOWN_MASK, event.getModifiersEx());
        assertEquals(MouseEvent.BUTTON1, event.getButton());

        event = new MouseEvent(button, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 0,
                true, MouseEvent.BUTTON1);
        assertEquals(InputEvent.BUTTON1_MASK, event.getModifiers());
        assertEquals(MouseEvent.BUTTON1, event.getButton());

        event = new MouseEvent(button, MouseEvent.MOUSE_RELEASED, 1000000000,
                0, 100, 200, 10, true, MouseEvent.BUTTON1);
        assertEquals(InputEvent.BUTTON1_MASK, event.getModifiers());
        assertEquals(0, event.getModifiersEx());

        event = new MouseEvent(button, MouseEvent.MOUSE_CLICKED, 1000000000, 0,
                100, 200, 10, true, MouseEvent.BUTTON1);
        assertEquals(InputEvent.BUTTON1_MASK, event.getModifiers());
        assertEquals(0, event.getModifiersEx());

        event = new MouseEvent(button, MouseEvent.MOUSE_PRESSED, 0,
                InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK
                        | InputEvent.BUTTON3_DOWN_MASK, 0, 0, 0, true,
                MouseEvent.BUTTON3);
        assertEquals(InputEvent.BUTTON3_MASK, event.getModifiers());
        assertEquals(InputEvent.BUTTON1_DOWN_MASK
                | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK,
                event.getModifiersEx());
    }

    public final void testTranslatePoint() {
        Button button = new Button("Button");
        MouseEvent event = new MouseEvent(button, MouseEvent.MOUSE_PRESSED, 1000000000,
                InputEvent.BUTTON2_DOWN_MASK, 100, 200,
                10, true);

        event.translatePoint(10, 10);
        assertEquals(event.getPoint(), new Point(110, 210));
        event.translatePoint(-20, -20);
        assertEquals(event.getPoint(), new Point(90, 190));
    }

    public final void testGetMouseModifiersText() {
        assertTrue(MouseEvent.getMouseModifiersText(InputEvent.ALT_MASK)
                .indexOf("Alt") != -1); //$NON-NLS-1$
        assertTrue(MouseEvent.getMouseModifiersText(InputEvent.ALT_GRAPH_MASK)
                .indexOf("Alt Graph") != -1); //$NON-NLS-1$
        assertTrue(MouseEvent.getMouseModifiersText(InputEvent.CTRL_MASK)
                .indexOf("Ctrl") != -1); //$NON-NLS-1$
        assertTrue(MouseEvent.getMouseModifiersText(InputEvent.SHIFT_MASK)
                .indexOf("Shift") != -1); //$NON-NLS-1$
        assertTrue(MouseEvent.getMouseModifiersText(InputEvent.META_MASK)
                .indexOf("Meta") != -1); //$NON-NLS-1$
        assertTrue(MouseEvent.getMouseModifiersText(InputEvent.BUTTON1_MASK)
                .indexOf("Button1") != -1); //$NON-NLS-1$
        assertTrue(MouseEvent.getMouseModifiersText(InputEvent.BUTTON2_MASK)
                .indexOf("Button2") != -1); //$NON-NLS-1$
        assertTrue(MouseEvent.getMouseModifiersText(InputEvent.BUTTON3_MASK)
                .indexOf("Button3") != -1); //$NON-NLS-1$
        assertTrue(MouseEvent.getMouseModifiersText(InputEvent.BUTTON3_MASK)
                .indexOf("Meta") != -1); //$NON-NLS-1$

        // Regression for HARMONY-2403
        assertEquals("Meta+Shift+Alt Graph+Button1+Button3", MouseEvent //$NON-NLS-1$
                .getMouseModifiersText(MouseEvent.MOUSE_PRESSED));
    }

    public final void testParamString() {
        Button button = new Button("Button");
        MouseEvent event = new MouseEvent(button, MouseEvent.MOUSE_PRESSED,
                1000000000, InputEvent.BUTTON2_DOWN_MASK, 100, 200, 10, true,
                MouseEvent.BUTTON1);

        assertEquals(
                event.paramString(),
                "MOUSE_PRESSED,(100,200),button=1,modifiers=Button1,extModifiers=Button2,clickCount=10");
        event = new MouseEvent(button, MouseEvent.MOUSE_PRESSED + 1024,
                1000000000, InputEvent.BUTTON2_MASK, 100, 200, 10, true,
                MouseEvent.BUTTON1);
        assertEquals(
                event.paramString(),
                "unknown type,(100,200),button=1,modifiers=Alt+Button2,extModifiers=Alt+Button2,clickCount=10");
    }
    
    public void testHarmony5215() throws Exception {
        final Frame f = new Frame();
        final Point l = MouseInfo.getPointerInfo().getLocation();
        final Robot r = new Robot();
        final MouseEvent[] e = new MouseEvent[1];

        f.addMouseListener(new MouseAdapter() {
            public void mouseReleased(final MouseEvent event) {
                synchronized (e) {
                    e[0] = event;
                    e.notify();
                }
            }

            public void mousePressed(final MouseEvent event) {
                synchronized (e) {
                    e[0] = event;
                    e.notify();
                }
            }
        });

        f.setSize(100, 100);
        f.setVisible(true);
        r.mouseMove(f.getX() + 50, f.getY() + 50);

        try {
            synchronized (e) {
                r.mousePress(InputEvent.BUTTON1_MASK);
                e.wait(5000);
                assertEquals(InputEvent.BUTTON1_MASK, e[0].getModifiers());
                assertEquals(InputEvent.BUTTON1_DOWN_MASK,
                    e[0].getModifiersEx());

                r.mouseRelease(InputEvent.BUTTON1_MASK);
                e.wait(5000);
                assertEquals(InputEvent.BUTTON1_MASK, e[0].getModifiers());
                assertEquals(0, e[0].getModifiersEx());

                r.mousePress(InputEvent.BUTTON1_MASK);
                e.wait(5000);
                r.mousePress(InputEvent.BUTTON2_MASK);
                e.wait(5000);
                assertEquals(InputEvent.BUTTON2_MASK, e[0].getModifiers());
                assertEquals(InputEvent.BUTTON1_DOWN_MASK
                    | InputEvent.BUTTON2_DOWN_MASK, e[0].getModifiersEx());

                r.mouseRelease(InputEvent.BUTTON2_MASK);
                e.wait(5000);
                assertEquals(InputEvent.BUTTON2_MASK, e[0].getModifiers());
                assertEquals(InputEvent.BUTTON1_DOWN_MASK,
                    e[0].getModifiersEx());

                r.mousePress(InputEvent.BUTTON2_MASK);
                e.wait(5000);
                r.mousePress(InputEvent.BUTTON3_MASK);
                e.wait(5000);
                assertEquals(InputEvent.BUTTON3_MASK, e[0].getModifiers());
                assertEquals(InputEvent.BUTTON1_DOWN_MASK
                    | InputEvent.BUTTON2_DOWN_MASK
                    | InputEvent.BUTTON3_DOWN_MASK, e[0].getModifiersEx());

                r.mouseRelease(InputEvent.BUTTON3_MASK);
                e.wait(5000);
                assertEquals(InputEvent.BUTTON3_MASK, e[0].getModifiers());
                assertEquals(InputEvent.BUTTON1_DOWN_MASK
                    | InputEvent.BUTTON2_DOWN_MASK, e[0].getModifiersEx());
            }
        } finally {
            r.mouseRelease(InputEvent.BUTTON1_MASK);
            r.mouseRelease(InputEvent.BUTTON2_MASK);
            r.mouseRelease(InputEvent.BUTTON3_MASK);
            r.mouseMove(l.x, l.y);
            f.dispose();
        }
    }
}
