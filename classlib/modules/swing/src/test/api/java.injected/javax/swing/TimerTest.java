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
 * Created on 23.12.2004

 */
package javax.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;

public class TimerTest extends SwingTestCase {
    Timer timer = null;

    static int counter = 0;

    class ConcreteActionListener implements ActionListener {
        public String name;

        public ActionEvent action = null;

        ConcreteActionListener(final String name) {
            this.name = name;
        }

        public void actionPerformed(final ActionEvent action) {
            this.action = action;
        }
    };

    protected boolean find(final Object[] array, final Object value) {
        boolean found = false;
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(value)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        timer = new Timer(100, new ActionListener() {
            public void actionPerformed(final ActionEvent action) {
            }
        });
    }

    public void testTimer() {
        ActionListener listener1 = new ConcreteActionListener("1");
        timer = new Timer(100, listener1);
        ActionListener[] listeners = timer.getActionListeners();
        assertTrue("Initially there's one listener ", listeners != null
                && listeners.length == 1);
        assertTrue("Initially there's one listener ", listeners[0] == listener1);
        assertTrue("repeats ", timer.isRepeats());
        assertEquals("delay is ", 100, timer.getDelay());
        assertEquals("Initial delay is ", 100, timer.getInitialDelay());
        assertTrue("Coalesces ", timer.isCoalesce());
        timer = new Timer(-100, listener1);
        listeners = timer.getActionListeners();
        assertTrue("Initially there's one listener ", listeners != null
                && listeners.length == 1);
        assertTrue("Initially there's one listener ", listeners[0] == listener1);
        assertTrue("repeats ", timer.isRepeats());
        assertEquals("delay is ", -100, timer.getDelay());
        assertEquals("Initial delay is ", -100, timer.getInitialDelay());
        assertTrue("Coalesces ", timer.isCoalesce());
    }

    public void testGetListeners() {
        ActionListener listener1 = new ConcreteActionListener("1");
        ActionListener listener2 = new ConcreteActionListener("2");
        ActionListener listener3 = new ConcreteActionListener("3");
        timer = new Timer(100, listener1);
        timer.addActionListener(listener2);
        timer.addActionListener(listener3);
        EventListener[] listeners = timer.getListeners(ActionListener.class);
        assertTrue("Now there are 3 listeners ", listeners != null && listeners.length == 3);
        assertTrue("Listener 1 is found", find(listeners, listener1));
        assertTrue("Listener 2 is found", find(listeners, listener2));
        assertTrue("Listener 3 is found", find(listeners, listener3));
        listeners = timer.getListeners(ConcreteActionListener.class);
        assertTrue("No listeners found", listeners != null && listeners.length == 0);
    }

    public void testRemoveActionListener() {
        ActionListener listener1 = new ConcreteActionListener("1");
        ActionListener listener2 = new ConcreteActionListener("2");
        ActionListener listener3 = new ConcreteActionListener("3");
        ActionListener listener4 = new ConcreteActionListener("3");
        timer = new Timer(100, listener1);
        ActionListener[] listeners = timer.getActionListeners();
        assertTrue("Initially there's one listener ", listeners != null
                && listeners.length == 1);
        assertTrue("Initially there's one listener ", listeners[0] == listener1);
        timer.removeActionListener(listener1);
        listeners = timer.getActionListeners();
        assertTrue("now there are no listeners ", listeners != null && listeners.length == 0);
        timer.addActionListener(listener2);
        timer.addActionListener(listener3);
        timer.addActionListener(listener4);
        timer.addActionListener(listener1);
        listeners = timer.getActionListeners();
        assertTrue("Now there are 4 listeners ", listeners != null && listeners.length == 4);
        assertTrue("Listener 1 is found", find(listeners, listener1));
        assertTrue("Listener 2 is found", find(listeners, listener2));
        assertTrue("Listener 3 is found", find(listeners, listener3));
        assertTrue("Listener 4 is found", find(listeners, listener4));
        timer.removeActionListener(listener4);
        listeners = timer.getActionListeners();
        assertTrue("Now there are 3 listeners ", listeners != null && listeners.length == 3);
        assertTrue("Listener 1 is found", find(listeners, listener1));
        assertTrue("Listener 2 is found", find(listeners, listener2));
        assertTrue("Listener 3 is found", find(listeners, listener3));
        timer.removeActionListener(listener2);
        listeners = timer.getActionListeners();
        assertTrue("Now there are 2 listeners ", listeners != null && listeners.length == 2);
        assertTrue("Listener 1 is found", find(listeners, listener1));
        assertTrue("Listener 3 is found", find(listeners, listener3));
    }

    public void testAddActionListener() {
        ActionListener listener1 = new ConcreteActionListener("1");
        ActionListener listener2 = new ConcreteActionListener("2");
        ActionListener listener3 = new ConcreteActionListener("3");
        ActionListener listener4 = new ConcreteActionListener("3");
        timer = new Timer(100, listener1);
        ActionListener[] listeners = timer.getActionListeners();
        assertTrue("Initially there's only one listener ", listeners != null
                && listeners.length == 1);
        assertTrue("Initially there's only one listener ", listeners[0] == listener1);
        timer.addActionListener(listener2);
        listeners = timer.getActionListeners();
        assertTrue("Now there are 2 listeners ", listeners != null && listeners.length == 2);
        assertTrue("Listener 1 is found", find(listeners, listener1));
        assertTrue("Listener 2 is found", find(listeners, listener2));
        timer.addActionListener(listener3);
        listeners = timer.getActionListeners();
        assertTrue("Now there are 3 listeners ", listeners != null && listeners.length == 3);
        assertTrue("Listener 1 is found", find(listeners, listener1));
        assertTrue("Listener 2 is found", find(listeners, listener2));
        assertTrue("Listener 3 is found", find(listeners, listener3));
        timer.addActionListener(listener4);
        timer.addActionListener(null);
        timer.addActionListener(null);
        listeners = timer.getActionListeners();
        assertTrue("Now there are 4 listeners ", listeners != null && listeners.length == 4);
        assertTrue("Listener 1 is found", find(listeners, listener1));
        assertTrue("Listener 2 is found", find(listeners, listener2));
        assertTrue("Listener 3 is found", find(listeners, listener3));
        assertTrue("Listener 4 is found", find(listeners, listener4));
    }

    public void testGetActionListeners() {
        ActionListener listener1 = new ConcreteActionListener("1");
        ActionListener listener2 = new ConcreteActionListener("2");
        ActionListener listener3 = new ConcreteActionListener("3");
        timer = new Timer(100, listener1);
        ActionListener[] listeners = timer.getActionListeners();
        assertTrue("Initially there's only one listener ", listeners != null
                && listeners.length == 1);
        assertSame("Initially there's only one listener ", listener1, listeners[0]);
        timer.addActionListener(listener2);
        timer.addActionListener(listener3);
        listeners = timer.getActionListeners();
        assertTrue("Now there are 3 listeners ", listeners != null && listeners.length == 3);
        assertTrue("Listener 1 is found", find(listeners, listener1));
        assertTrue("Listener 2 is found", find(listeners, listener2));
        assertTrue("Listener 3 is found", find(listeners, listener3));
    }

    public void testIsRepeats() {
        ConcreteActionListener listener1 = new ConcreteActionListener("1");
        timer = new Timer(10, listener1);
        assertTrue("repeats ", timer.isRepeats());
        timer.setRepeats(false);
        assertFalse("doesn't repeat ", timer.isRepeats());
        timer.setRepeats(true);
        assertTrue("repeats ", timer.isRepeats());
    }

    public void testSetCoalesce() {
        ConcreteActionListener listener = new ConcreteActionListener("1");
        timer = new Timer(10, listener);
        timer.setCoalesce(true);
        assertTrue("Coalesces ", timer.isCoalesce());
        timer.setCoalesce(false);
        assertFalse("doesn't Coalesce ", timer.isCoalesce());
        timer.setCoalesce(true);
        assertTrue("Coalesce ", timer.isCoalesce());
    }

    public void testIsCoalesce() {
        ConcreteActionListener listener1 = new ConcreteActionListener("1");
        timer = new Timer(10, listener1);
        assertTrue("Coalesces ", timer.isCoalesce());
        timer.setCoalesce(false);
        assertFalse("doesn't Coalesce ", timer.isCoalesce());
        timer.setCoalesce(true);
        assertTrue("Coalesce ", timer.isCoalesce());
    }

    public void testGetInitialDelay() {
        ConcreteActionListener listener1 = new ConcreteActionListener("1");
        timer = new Timer(10, listener1);
        assertEquals("Initial delay ", 10, timer.getInitialDelay());
        timer.setInitialDelay(100);
        assertEquals("Initial delay ", 100, timer.getInitialDelay());
        timer.setInitialDelay(300);
        assertEquals("Initial delay ", 300, timer.getInitialDelay());
        boolean thrown = false;
        try {
            timer.setInitialDelay(-100);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("exception has been thrown", thrown);
        thrown = false;
        try {
            timer.setInitialDelay(0);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertEquals("Initial delay ", 0, timer.getInitialDelay());
    }

    public void testGetDelay() {
        ConcreteActionListener listener1 = new ConcreteActionListener("1");
        timer = new Timer(10, listener1);
        assertEquals("delay ", 10, timer.getDelay());
        timer.setDelay(100);
        assertEquals("delay ", 100, timer.getDelay());
        timer.setDelay(300);
        assertEquals("delay ", 300, timer.getDelay());
        boolean thrown = false;
        try {
            timer.setDelay(-100);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue("exception has been thrown", thrown);
        thrown = false;
        try {
            timer.setDelay(0);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertEquals("delay ", 0, timer.getDelay());
    }

    public void testGetLogTimers() {
        assertFalse("doesn't log timers ", Timer.getLogTimers());
        Timer.setLogTimers(true);
        assertTrue("logs timers ", Timer.getLogTimers());
        Timer.setLogTimers(false);
        assertFalse("doesn't log timers ", Timer.getLogTimers());
    }
}
