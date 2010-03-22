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
 * @author Dmitry A. Durnev
 */
package java.awt;

import junit.framework.TestCase;

public class EventTest extends TestCase {

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testParamString() {
        Button button = new Button("Test paramString()");
        int id = Event.GOT_FOCUS;
        Event evt = new Event(button, id, null);
        assertEquals("id="+id+",x=0,y=0,target="+evt.target.toString(),
                evt.paramString() );
        id = Event.ACTION_EVENT;
        Object obj = new Object();
        evt = new Event(button, id, obj);
        assertEquals("id="+id+",x=0,y=0,target="+button+",arg=" + obj,
                evt.paramString() );
        id = Event.KEY_PRESS;
        evt = new Event(button, 1048576l, id, 0, 0, Event.ENTER,
                Event.CTRL_MASK | Event.META_MASK | Event.SHIFT_MASK);
        assertEquals("id="+id+",x=0,y=0,key=" + evt.key + ",shift,control,meta,target="+button,
                evt.paramString() );
    }

    /*
     * Class under test for void Event(java.lang.Object, int, java.lang.Object)
     */
    public final void testEventObjectintObject() {
        String buttonLabel = "Test Event";
        Button button = new Button(buttonLabel);
        int id = Event.ACTION_EVENT;
        Event evt = new Event(button, id, button.getLabel());
        assertSame(button, evt.target);
        assertEquals(id, evt.id);
        assertSame(buttonLabel, evt.arg);
    }

    /*
     * Class under test for void Event(java.lang.Object, long, int, int, int, int, int)
     */
    public final void testEventObjectlongintintintintint() {
        Button button = new Button("Test constructor");
        int id = Event.MOUSE_DOWN;
        long when = 16*1048576l;
        int mod = Event.CTRL_MASK;
        Event evt = new Event(button, when, id, 100, 200, Event.ENTER,
                mod);
        assertSame(button, evt.target);
        assertEquals(id, evt.id);
        assertEquals(100, evt.x);
        assertEquals(200, evt.y);
        assertEquals(mod, evt.modifiers);
        assertEquals(when, evt.when);
        assertEquals(0, evt.clickCount);
    }

    /*
     * Class under test for void Event(java.lang.Object, long, int, int, int, int, int, java.lang.Object)
     */
    public final void testEventObjectlongintintintintintObject() {
        Button button = new Button("Test constructor");
        int id = Event.KEY_ACTION_RELEASE, x =13, y = 666;
        int key = Event.ESCAPE;
        long when = 32*1048576l;
        int mod = Event.SHIFT_MASK | Event.META_MASK;
        Object obj = new Object();
        Event evt = new Event(button, when, id, x, y, key,
                mod, obj);
        assertSame(button, evt.target);
        assertEquals(id, evt.id);
        assertEquals(x, evt.x);
        assertEquals(y, evt.y);
        assertEquals(mod, evt.modifiers);
        assertEquals(when, evt.when);
        assertSame(obj, evt.arg);
    }

    public final void testTranslate() {
        Button button = new Button("Test translate");
        int id = Event.MOUSE_MOVE, x = 20, y = -5;
        int key = 0;
        long when = 32*1048576l;
        int mod = 0;
        Event evt = new Event(button, when, id, x, y, key,
                mod);
        evt.translate(-20, 15);
        assertEquals(0, evt.x);
        assertEquals(10, evt.y);
    }

    public final void testControlDown() {
        Button button = new Button("Test control down");
        int id = Event.KEY_ACTION, x =0, y = -10;
        int key = Event.DOWN;
        long when = 32*1048576l;
        int mod = 0;
        Event evt = new Event(button, when, id, x, y, key,
                mod);
        assertFalse(evt.controlDown());
        evt.modifiers |= Event.SHIFT_MASK | Event.ALT_MASK;
        assertFalse(evt.controlDown());
        evt.modifiers = Event.CTRL_MASK;
        assertTrue(evt.controlDown());
    }

    public final void testMetaDown() {
        Button button = new Button("Test META down");
        int id = Event.MOUSE_DRAG, x = -20, y = 30;
        int key = 0;
        long when = 32*1048576l;
        int mod = 0;
        Event evt = new Event(button, when, id, x, y, key,
                mod);
        assertFalse(evt.metaDown());
        evt.modifiers |= Event.META_MASK;
        assertTrue(evt.metaDown());
        evt.modifiers = Event.ALT_MASK;
        assertFalse(evt.metaDown());
        evt.modifiers |= Event.SHIFT_MASK | Event.CTRL_MASK | Event.META_MASK;
        assertTrue(evt.metaDown());
    }

    public final void testShiftDown() {
        Button button = new Button("Test shift down");
        int id = Event.MOUSE_MOVE, x = 20, y = -30;
        int key = 'a';
        long when = 32*1048576l;
        int mod = 0;
        Event evt = new Event(button, when, id, x, y, key,
                mod);
        assertFalse(evt.shiftDown());
        evt.modifiers |= Event.META_MASK;
        assertFalse(evt.shiftDown());
        evt.modifiers = Event.SHIFT_MASK;
        assertTrue(evt.shiftDown());
        evt.modifiers |= Event.CTRL_MASK | Event.ALT_MASK;
        assertTrue(evt.shiftDown());
    }

    public void testDispatchEvent() {
        // Regression test for HARMONY-2460
        new MenuItem().dispatchEvent(new AWTEventImpl(new Button(), 1));
    }

    class AWTEventImpl extends AWTEvent {
        public AWTEventImpl(Object source, int id) {
            super(source, id);
        }
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(EventTest.class);
    }
}
