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
package javax.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

public class Timer_MultithreadedTest extends TestCase {
    private Timer timer;
    static class ConcreteActionListener implements ActionListener {
        class Delay {
            private boolean wasStopped;

            public synchronized void stopWaiting() {
                wasStopped = true;
                notifyAll();
            }

            public synchronized void waitForDelay(final int delay) {
                wasStopped = false;
                try {
                    wait(delay);
                    if (wasStopped) {
                        return;
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            stopWaiting();
                        }
                    });
                    wait();
                } catch (Exception e) {
                }
            }
        }

        private final Delay delay = new Delay();

        private final String name;

        private ActionEvent action;

        private boolean performed;

        private int counter;

        private boolean debug;

        ConcreteActionListener() {
            name = "NoName";
        }

        ConcreteActionListener(final String name) {
            this.name = name;
        }

        ConcreteActionListener(final String name, final boolean isDebug) {
            this.name = name;
            debug = isDebug;
        }

        public void reset() {
            action = null;
            performed = false;
            counter = 0;
        }

        public void actionPerformed(final ActionEvent action) {
            this.action = action;
            counter++;
            performed = true;
            if (debug) {
                System.out.println(name);
            }
            delay.stopWaiting();
        }

        public int getCounter() {
            return counter;
        }

        public boolean waitNumActions(final int maxWaitTime, final int numActions) {
            int startNumActions = counter;
            while (counter - startNumActions < numActions) {
                delay.waitForDelay(maxWaitTime);
                if (performed == false) {
                    break;
                }
                performed = false;
            }
            return (counter - startNumActions == numActions);
        }

        public boolean waitAction(final int maxWaitTime) {
            delay.waitForDelay(maxWaitTime);
            return action != null;
        }
    };

    public Timer_MultithreadedTest() {
        // As the first timer starts new thread, and how much time will it takes is
        // system dependent and unpredictable, for tests stability we should run
        // first timer ahead of running our testcases
        runHare();
    }

    @Override
    protected void tearDown() throws Exception {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        super.tearDown();
    }

    public void testFireActionPerformed() {
        ConcreteActionListener listener1 = new ConcreteActionListener("1");
        ConcreteActionListener listener2 = new ConcreteActionListener("2");
        ConcreteActionListener listener3 = new ConcreteActionListener("3");
        ConcreteActionListener listener4 = new ConcreteActionListener("4");
        timer = new Timer(10, listener1);
        timer.addActionListener(listener2);
        timer.addActionListener(listener3);
        timer.addActionListener(listener4);
        timer.start();
        listener1.waitAction(500);
        timer.stop();
        assertNotNull("[1] ActionListener's ActionPerformed invoked ", listener1.action);
        assertSame("[2] ActionListener's ActionPerformed invoked ", listener1.action,
                listener2.action);
        assertSame("[3] ActionListener's ActionPerformed invoked ", listener1.action,
                listener3.action);
        assertSame("[4] ActionListener's ActionPerformed invoked ", listener1.action,
                listener4.action);
    }

    public void testSetRepeats() {
        ConcreteActionListener listener1 = new ConcreteActionListener("1");
        timer = new Timer(10, listener1);
        timer.setRepeats(true);
        assertTrue("repeats ", timer.isRepeats());
        timer.setRepeats(false);
        assertFalse("doesn't repeat ", timer.isRepeats());
        timer.setRepeats(true);
        assertTrue("repeats ", timer.isRepeats());
        timer.start();
        listener1.waitNumActions(500, 5);
        assertTrue("ActionListener's ActionPerformed invoked ", listener1.action != null);
        timer.stop();

        // checking does setRepeats actually affects the work of timer
        ConcreteActionListener listener2 = new ConcreteActionListener("2");
        timer = new Timer(10, listener2);
        timer.start();
        listener2.waitAction(1500);
        assertNotNull("ActionListener's ActionPerformed did invoke ", listener2.action);
        listener2.reset();
        listener2.waitAction(1500);
        assertNotNull("ActionListener's ActionPerformed did invoke ", listener2.action);
        timer.setRepeats(false);
        listener2.reset();
        listener2.waitAction(100);
        listener2.reset();
        listener2.waitAction(100);
        assertNull("ActionListener's ActionPerformed didn't invoke ", listener2.action);
        timer.stop();

        ConcreteActionListener listener3 = new ConcreteActionListener("3");
        timer = new Timer(100, listener3);
        timer.setRepeats(false);
        timer.start();
        timer.setRepeats(true);
        listener3.waitAction(1500);
        assertNotNull("ActionListener's ActionPerformed did invoke ", listener3.action);
        listener3.reset();
        listener3.waitAction(1500);
        assertNotNull("ActionListener's ActionPerformed did invoke ", listener3.action);
        timer.stop();

        ConcreteActionListener listener4 = new ConcreteActionListener("4");
        timer = new Timer(100, listener4);
        timer.setRepeats(false);
        timer.start();
        listener4.waitAction(1500);
        assertNotNull("ActionListener's ActionPerformed did invoke ", listener4.action);
        listener4.reset();
        listener4.waitAction(10);
        timer.setRepeats(true);
        listener4.reset();
        listener4.waitAction(500);
        assertNull("ActionListener's ActionPerformed did invoke ", listener4.action);
    }

    public void testSetInitialDelay() {
        ConcreteActionListener listener1 = new ConcreteActionListener("1");
        timer = new Timer(10, listener1);
        timer.setInitialDelay(100);
        assertEquals("Initial delay is ", 100, timer.getInitialDelay());
        timer.setInitialDelay(300);
        assertEquals("Initial delay is ", 300, timer.getInitialDelay());

        // checking does SetInitialDelay actually affects the work of timer
        ConcreteActionListener listener2 = new ConcreteActionListener("2");
        timer = new Timer(10, listener2);
        timer.setInitialDelay(700);
        timer.start();
        listener2.waitAction(500);
        assertNull("ActionListener's ActionPerformed didn't invoke ", listener2.action);
        listener2.reset();
        listener2.waitAction(1000);
        assertTrue("ActionListener's ActionPerformed invoked ", listener2.action != null);
        timer.stop();

        timer = new Timer(10, listener2);
        timer.setInitialDelay(200);
        timer.start();
        timer.setInitialDelay(500);
        listener2.waitAction(500);
        assertTrue("ActionListener's ActionPerformed invoked ", listener2.action != null);
    }

    public void testSetDelay() {
        ConcreteActionListener listener = new ConcreteActionListener();
        timer = new Timer(10, listener);
        timer.setDelay(100);
        assertEquals("delay is ", 100, timer.getDelay());
        timer.setDelay(300);
        assertEquals("delay is ", 300, timer.getDelay());

        // checking does SetDelay affects working timer
        listener = new ConcreteActionListener();
        timer = new Timer(4000, listener);
        timer.start();
        listener.waitAction(500);
        timer.setDelay(100);
        listener.waitAction(200);
        assertNull("ActionListener's ActionPerformed didn't invoke ", listener.action);
        timer.stop();

        listener = new ConcreteActionListener();
        timer = new Timer(150, listener);
        timer.start();
        listener.waitAction(5);
        timer.setDelay(5000);
        listener.waitAction(500);
        assertNotNull("ActionListener's ActionPerformed invoked ", listener.action);
    }

    public void testIsRunning() {
        ConcreteActionListener listener = new ConcreteActionListener();
        timer = new Timer(10, listener);
        listener.waitAction(200);
        assertFalse("timer is not running ", timer.isRunning());
        timer.start();
        assertTrue("timer is running ", timer.isRunning());
        listener.waitAction(1500);
        assertTrue("timer is running ", timer.isRunning());
        timer.stop();
        assertFalse("timer is not running ", timer.isRunning());
        listener.reset();
        listener.waitAction(100);
        assertFalse("timer is not running ", timer.isRunning());

        timer.setRepeats(false);
        timer.setDelay(100);
        listener.reset();
        timer.start();
        assertTrue("timer is running ", timer.isRunning());
        listener.waitAction(1500);
        assertTrue("timer hasn't rung", listener.performed);
        listener.reset();
        listener.waitAction(100);
        assertFalse("timer must not ring", listener.performed);
        assertFalse("timer is running ", timer.isRunning());
    }

    public void testStop() {
        ConcreteActionListener listener = new ConcreteActionListener();
        timer = new Timer(10, listener);
        listener.waitAction(200);
        assertNull("ActionListener's ActionPerformed didn't invoke ", listener.action);
        timer.start();
        listener.waitAction(1500);
        assertTrue("ActionListener's ActionPerformed invoked ", listener.action != null);
        timer.stop();
        listener.reset();
        listener.waitAction(100);
        assertNull("ActionListener's ActionPerformed didn't invoke ", listener.action);
    }

    public void testStart() {
        ConcreteActionListener listener = new ConcreteActionListener("listener");
        timer = new Timer(10, listener);
        listener.waitAction(200);
        assertNull("ActionListener's ActionPerformed didn't invoke ", listener.action);
        timer.start();
        listener.waitAction(500);
        assertTrue("ActionListener's ActionPerformed invoked ", listener.action != null);
        timer.stop();
        listener.action = null;
        timer.setInitialDelay(1000);
        timer.start();
        listener.waitAction(500);
        assertNull("ActionListener's ActionPerformed didn't invoke ", listener.action);

        // testing the right order of timers being triggered
        ConcreteActionListener listener1 = new ConcreteActionListener("1");
        ConcreteActionListener listener2 = new ConcreteActionListener("2");
        ConcreteActionListener listener3 = new ConcreteActionListener("3");
        Timer timer1 = new Timer(50, listener1);
        Timer timer2 = new Timer(60, listener2);
        Timer timer3 = new Timer(70, listener3);
        Timer timer4 = new Timer(1, new ConcreteActionListener());
        Timer timer5 = new Timer(2, new ConcreteActionListener());
        Timer timer6 = new Timer(3, new ConcreteActionListener());
        Timer timer7 = new Timer(4, new ConcreteActionListener());
        timer4.start();
        timer5.start();
        timer6.start();
        timer7.start();
        try {
            timer1.setRepeats(false);
            timer2.setRepeats(false);
            timer3.setRepeats(false);
            timer1.start();
            timer2.start();
            timer3.start();
            listener3.waitAction(500);
            long when1 = getWhen(listener1);
            long when2 = getWhen(listener2);
            long when3 = getWhen(listener3);
            assertTrue("The order of timers alerts is correct", when2 >= when1);
            assertTrue("The order of timers alerts is correct", when3 >= when2);
        } finally {
            timer1.stop();
            timer2.stop();
            timer3.stop();
            timer4.stop();
            timer5.stop();
            timer6.stop();
            timer7.stop();
        }
    }

    public void testRestart() {
        ConcreteActionListener listener = new ConcreteActionListener();
        timer = new Timer(10, listener);
        listener.waitAction(200);
        assertNull("ActionListener's ActionPerformed didn't invoke ", listener.action);
        timer.start();
        listener.waitAction(1500);
        assertTrue("ActionListener's ActionPerformed invoked ", listener.action != null);
        timer.setInitialDelay(500);
        timer.restart();
        listener.reset();
        listener.waitAction(250);
        assertNull("ActionListener's ActionPerformed didn't invoke ", listener.action);
        listener.waitAction(500);
        assertTrue("ActionListener's ActionPerformed invoked ", listener.action != null);
    }

    public void testSetLogTimers() {
        class LogOutputStream extends OutputStream {
            public boolean written = false;

            @Override
            public void write(int b) throws IOException {
                written = true;
            }
        }
        ;
        PrintStream oldOut = System.out;
        LogOutputStream logOut = new LogOutputStream();
        PrintStream newOut = new PrintStream(logOut);
        System.setOut(newOut);
        try {
            Timer.setLogTimers(false);
            ConcreteActionListener listener = new ConcreteActionListener();
            timer = new Timer(10, listener);
            timer.start();
            assertFalse("[1] doesn't log timers", Timer.getLogTimers());
            listener.waitAction(250);
            assertTrue("[1] action performed ", listener.performed);
            assertFalse("[1] log's not written", logOut.written);
            timer.stop();
            listener.reset();

            Timer.setLogTimers(true);
            timer.start();
            listener.waitAction(250);
            assertTrue("[2] logs timers ", Timer.getLogTimers());
            assertTrue("[2] action performed", listener.performed);
            assertTrue("[2] log's written", logOut.written);
            timer.stop();
            listener.waitAction(200);
            listener.reset();

            Timer.setLogTimers(false);
            logOut.written = false;
            timer.start();
            listener.waitAction(50);
            assertFalse("[3] doesn't log timers ", Timer.getLogTimers());
            assertTrue("[3] action performed", listener.performed);
            assertFalse("[3] log's not written", logOut.written);
        } finally {
            System.setOut(oldOut);
            Timer.setLogTimers(false);
        }
    }

    private long getWhen(ConcreteActionListener listener) {
        return (listener.action != null) ? listener.action.getWhen() : 0;
    }

    private void runHare() {
        ConcreteActionListener listener = new ConcreteActionListener();
        Timer hare = new Timer(1, listener);
        hare.setRepeats(false);
        hare.start();
        listener.waitAction(1000);
    }
}
