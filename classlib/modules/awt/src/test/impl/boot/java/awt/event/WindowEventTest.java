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

import java.awt.Frame;
import java.awt.Window;

import junit.framework.TestCase;

public class WindowEventTest extends TestCase {

    public final void testWindowEventWindowint() {
        Window window = new Window(new Frame());
        WindowEvent event = new WindowEvent(window, WindowEvent.WINDOW_ACTIVATED);

        assertEquals(event.getSource(), window);
        assertEquals(event.getID(), WindowEvent.WINDOW_ACTIVATED);
        assertEquals(event.getWindow(), window);
        assertNull(event.getOppositeWindow());
        assertEquals(event.getOldState(), Frame.NORMAL);
        assertEquals(event.getNewState(), Frame.NORMAL);
    }

    public final void testWindowEventWindowintWindow() {
        Window window = new Window(new Frame());
        Window opposite = new Window(new Frame());
        WindowEvent event = new WindowEvent(window, WindowEvent.WINDOW_ACTIVATED, opposite);

        assertEquals(event.getSource(), window);
        assertEquals(event.getID(), WindowEvent.WINDOW_ACTIVATED);
        assertEquals(event.getWindow(), window);
        assertEquals(event.getOppositeWindow(), opposite);
        assertEquals(event.getOldState(), Frame.NORMAL);
        assertEquals(event.getNewState(), Frame.NORMAL);
    }

    public final void testWindowEventWindowintintint() {
        Window window = new Window(new Frame());
        WindowEvent event = new WindowEvent(window, WindowEvent.WINDOW_ACTIVATED,
                Frame.MAXIMIZED_BOTH, Frame.MAXIMIZED_HORIZ);

        assertEquals(event.getSource(), window);
        assertEquals(event.getID(), WindowEvent.WINDOW_ACTIVATED);
        assertEquals(event.getWindow(), window);
        assertNull(event.getOppositeWindow());
        assertEquals(event.getOldState(), Frame.MAXIMIZED_BOTH);
        assertEquals(event.getNewState(), Frame.MAXIMIZED_HORIZ);
    }

    public final void testWindowEventWindowintWindowintint() {
        Window window = new Window(new Frame());
        Window opposite = new Window(new Frame());
        WindowEvent event = new WindowEvent(window, WindowEvent.WINDOW_ACTIVATED, opposite,
                Frame.MAXIMIZED_BOTH, Frame.MAXIMIZED_HORIZ);

        assertEquals(event.getSource(), window);
        assertEquals(event.getID(), WindowEvent.WINDOW_ACTIVATED);
        assertEquals(event.getWindow(), window);
        assertEquals(event.getOppositeWindow(), opposite);
        assertEquals(event.getOldState(), Frame.MAXIMIZED_BOTH);
        assertEquals(event.getNewState(), Frame.MAXIMIZED_HORIZ);
    }

    public final void testParamString() {
        Window window = new Window(new Frame());
        Window opposite = new Window(new Frame());
        WindowEvent event = new WindowEvent(window, WindowEvent.WINDOW_ACTIVATED, opposite,
                Frame.MAXIMIZED_BOTH, Frame.MAXIMIZED_HORIZ);

        assertTrue(event.paramString().startsWith("WINDOW_ACTIVATED,opposite=java.awt.Window["));
        assertTrue(event.paramString().endsWith("],oldState=6,newState=2"));
        event = new WindowEvent(window, WindowEvent.WINDOW_ACTIVATED + 1024, opposite,
                Frame.MAXIMIZED_BOTH, Frame.MAXIMIZED_HORIZ);
        assertTrue(event.paramString().startsWith("unknown type"));
    }

}
