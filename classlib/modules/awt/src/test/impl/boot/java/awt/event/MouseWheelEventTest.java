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

import junit.framework.TestCase;

public class MouseWheelEventTest extends TestCase {

    public final void testMouseWheelEvent() {
        Button button = new Button("Button");
        MouseWheelEvent event = new MouseWheelEvent(button, MouseEvent.MOUSE_WHEEL, 1000000000,
                InputEvent.BUTTON2_DOWN_MASK, 100, 200,
                10, true, MouseWheelEvent.WHEEL_UNIT_SCROLL, 2, 3);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), MouseEvent.MOUSE_WHEEL);
        assertEquals(event.getScrollAmount(), 2);
        assertEquals(event.getScrollType(), MouseWheelEvent.WHEEL_UNIT_SCROLL);
        assertEquals(event.getWheelRotation(), 3);
        assertEquals(event.getUnitsToScroll(), 3 * 2);
    }

    public final void testParamString() {
        Button button = new Button("Button");
        MouseWheelEvent event = new MouseWheelEvent(button, MouseEvent.MOUSE_WHEEL, 1000000000,
                InputEvent.BUTTON2_DOWN_MASK, 100, 200,
                10, true, MouseWheelEvent.WHEEL_UNIT_SCROLL, 2, 3);

        assertEquals(event.paramString(),
                "MOUSE_WHEEL,(100,200),button=0,modifiers=Button2,extModifiers=Button2,clickCount=10,scrollType=WHEEL_UNIT_SCROLL,scrollAmount=2,wheelRotation=3");
        event = new MouseWheelEvent(button, MouseEvent.MOUSE_WHEEL + 1024, 1000000000,
                InputEvent.BUTTON2_DOWN_MASK, 100, 200,
                10, true, MouseWheelEvent.WHEEL_UNIT_SCROLL, 2, 3);
        assertTrue(event.paramString().startsWith("unknown type"));
    }

}
