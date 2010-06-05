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
 * Created on 16.12.2004

 */
package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class SwingUtilitiesRTest extends SwingTestCase {
    public void testLayoutCompoundLabel() {
        JComponent c = new JButton();
        FontMetrics fm = c.getFontMetrics(c.getFont());
        Rectangle viewRectangle = new Rectangle(1000, 1000);
        Rectangle iconRectangle = new Rectangle();
        Rectangle textRectangle = new Rectangle();
        SwingUtilities.layoutCompoundLabel(c, fm, "text", null, SwingConstants.CENTER,
                SwingConstants.LEADING, SwingConstants.CENTER, SwingConstants.TRAILING,
                viewRectangle, iconRectangle, textRectangle, 0);
        int textHeight = fm.getHeight();
        assertEquals(new Rectangle(0, (viewRectangle.height - textHeight) / 2 + textHeight / 2,
                0, 0), iconRectangle);
    }

    public void testIsLeftMiddleRightMouseButton() {
        JComponent panel = new JPanel();
        panel.setPreferredSize(new Dimension(100, 100));
        MouseEvent button1DownEvent = new MouseEvent(panel, 100, 0,
                InputEvent.BUTTON1_DOWN_MASK, 50, 50, 1, false);
        MouseEvent button1PressedEvent = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0,
                50, 50, 1, false, MouseEvent.BUTTON1);
        MouseEvent button1Released = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0, 50,
                50, 1, false, MouseEvent.BUTTON1);
        MouseEvent button1Clicked = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 50,
                50, 1, false, MouseEvent.BUTTON1);
        MouseEvent button1DraggedEvent = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, 0, 0,
                50, 50, 1, false, MouseEvent.BUTTON1);
        MouseEvent button2DownEvent = new MouseEvent(panel, 100, 0,
                InputEvent.BUTTON2_DOWN_MASK, 50, 50, 1, false);
        MouseEvent button2PressedEvent = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0,
                50, 50, 1, false, MouseEvent.BUTTON2);
        MouseEvent button2Released = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0, 50,
                50, 1, false, MouseEvent.BUTTON2);
        MouseEvent button2Clicked = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 50,
                50, 1, false, MouseEvent.BUTTON2);
        MouseEvent button2DraggedEvent = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, 0, 0,
                50, 50, 1, false, MouseEvent.BUTTON2);
        MouseEvent button3DownEvent = new MouseEvent(panel, 100, 0,
                InputEvent.BUTTON3_DOWN_MASK, 50, 50, 1, false);
        MouseEvent button3PressedEvent = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0,
                50, 50, 1, false, MouseEvent.BUTTON3);
        MouseEvent button3Released = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, 0, 0, 50,
                50, 1, false, MouseEvent.BUTTON3);
        MouseEvent button3Clicked = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 50,
                50, 1, false, MouseEvent.BUTTON3);
        MouseEvent button3DraggedEvent = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, 0, 0,
                50, 50, 1, false, MouseEvent.BUTTON3);
        assertTrue(SwingUtilities.isLeftMouseButton(button1DownEvent));
        assertFalse(SwingUtilities.isLeftMouseButton(button2DownEvent));
        assertFalse(SwingUtilities.isLeftMouseButton(button3DownEvent));
        assertTrue(SwingUtilities.isLeftMouseButton(button1PressedEvent));
        assertTrue(SwingUtilities.isLeftMouseButton(button1Released));
        assertTrue(SwingUtilities.isLeftMouseButton(button1Clicked));
        assertFalse(SwingUtilities.isLeftMouseButton(button1DraggedEvent));
        assertFalse(SwingUtilities.isMiddleMouseButton(button1DownEvent));
        assertTrue(SwingUtilities.isMiddleMouseButton(button2DownEvent));
        assertFalse(SwingUtilities.isMiddleMouseButton(button3DownEvent));
        assertTrue(SwingUtilities.isMiddleMouseButton(button2PressedEvent));
        assertTrue(SwingUtilities.isMiddleMouseButton(button2Released));
        assertTrue(SwingUtilities.isMiddleMouseButton(button2Clicked));
        assertFalse(SwingUtilities.isMiddleMouseButton(button2DraggedEvent));
        assertFalse(SwingUtilities.isRightMouseButton(button1DownEvent));
        assertFalse(SwingUtilities.isRightMouseButton(button2DownEvent));
        assertTrue(SwingUtilities.isRightMouseButton(button3DownEvent));
        assertTrue(SwingUtilities.isRightMouseButton(button3PressedEvent));
        assertTrue(SwingUtilities.isRightMouseButton(button3Released));
        assertTrue(SwingUtilities.isRightMouseButton(button3Clicked));
        assertFalse(SwingUtilities.isRightMouseButton(button3DraggedEvent));
    }

    public void testGetAncestorOfClass() {
        final Frame f = new Frame();
        final JDialog dialog = new JDialog(f);
        assertSame(f, SwingUtilities.getAncestorOfClass(Frame.class, dialog));
    }
    
    public void testDeadLoop_4820() {
        final int DEAD_LOOP_TIMEOUT = 1000;
        final int VALID_NUMBER_OF_CALLS = 15;
        final int counter[] = {0};
        
        class MFrame extends Frame {
            MFrame() {
                setSize(300,200) ;
                show();
            }
        
            public Component locate(int x, int y) {
                counter[0]++;
                return super.locate(x,y);
            }
            
            public Component getComponentAt(int x, int y) {
                counter[0]++;
                return super.getComponentAt(x, y);
            }
            
            public Component getComponentAt(java.awt.Point p) {
                counter[0]++;
                return super.getComponentAt(p);
            }
        }

        Frame f = new MFrame();
        
        SwingUtilities.getDeepestComponentAt(f, 10, 10);
        try {
            Thread.sleep(DEAD_LOOP_TIMEOUT);
        } catch (Exception e) {}
        
        f.dispose();
        
        assertTrue("Dead loop occured", counter[0] <= VALID_NUMBER_OF_CALLS);
    }

}
