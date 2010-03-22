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

public class InvocationEventTest extends TestCase {

    InvocationEvent event = null;
    boolean runnableCalled = false;

    public final void testInvocationEventObjectRunnable() {
        Button button = new Button("Button");
        Runnable runnable = new Runnable() {
            public void run() {
            }
        };
        event = new InvocationEvent(button, runnable);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), InvocationEvent.INVOCATION_DEFAULT);
        assertNull(event.getException());
        assertFalse(event.getWhen() == 0l);
    }

    public final void testInvocationEventObjectRunnableObjectboolean() {
        Button button = new Button("Button");
        Runnable runnable = new Runnable() {
            public void run() {
            }
        };
        event = new InvocationEvent(button, runnable, button, false);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), InvocationEvent.INVOCATION_DEFAULT);
        assertNull(event.getException());
        assertFalse(event.getWhen() == 0l);
    }

    public final void testInvocationEventObjectintRunnableObjectboolean() {
        Button button = new Button("Button");
        Runnable runnable = new Runnable() {
            public void run() {
            }
        };
        event = new InvocationEvent(button, InvocationEvent.INVOCATION_DEFAULT,
                runnable, button, false);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), InvocationEvent.INVOCATION_DEFAULT);
        assertNull(event.getException());
        assertFalse(event.getWhen() == 0l);
    }

    public final void testDispatch() {
        Button button = new Button("Button");
        Runnable runnable = new Runnable() {
            public void run() {
                runnableCalled = true;
                ((Button) null).setVisible(true);
            }
        };

        event = new InvocationEvent(button, runnable, button, false);
        boolean exceptionOccured = false;
        try {
            event.dispatch();
        } catch (Throwable t) {
            exceptionOccured = true;
        }
        assertTrue(exceptionOccured);

        event = new InvocationEvent(button, runnable, button, true);
        exceptionOccured = false;
        runnableCalled = false;
        try {
            event.dispatch();
        } catch (Throwable t) {
            exceptionOccured = true;
        }
        assertFalse(exceptionOccured);
        assertTrue(runnableCalled);


        event = new InvocationEvent(button, runnable, button, true);
        runnableCalled = false;
        synchronized (button) {
            new Thread() {
                @Override
                public void run() {
                    event.dispatch();
                }
            }.start();
            try {
                button.wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertTrue(runnableCalled);
    }

    public final void testParamString() {
        Button button = new Button("Button");
        Runnable runnable = new Runnable() {
            public void run() {
            }
        };
        event = new InvocationEvent(button, InvocationEvent.INVOCATION_DEFAULT,
                runnable, button, false);

        assertTrue(event.paramString().startsWith("INVOCATION_DEFAULT,runnable=java.awt.event.InvocationEventTest$"));
        assertTrue(event.paramString().indexOf(",notifier=java.awt.Button[") != -1);
        assertTrue(event.paramString().indexOf("],catchExceptions=false,when=") != -1);

        event = new InvocationEvent(button, InvocationEvent.INVOCATION_DEFAULT + 1024,
                runnable, button, false);
        assertTrue(event.paramString().startsWith("unknown type,runnable=java.awt.event.InvocationEventTest$"));
    }

}
