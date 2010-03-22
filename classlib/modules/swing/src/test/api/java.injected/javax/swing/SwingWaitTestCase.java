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
 * @author Alexey A. Ivanov
 */
package javax.swing;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;

/**
 * JUnit test case class which calls <code>setUp</code>, <code>tearDown</code>
 * and tests (<code>testXXX</code> methods) on event-dispatch thread, and
 * waits (stops) after calling <code>setUp</code> until <code>component</code>
 * is displayed on screen.
 *
 */
public abstract class SwingWaitTestCase extends BasicSwingTestCase {
    private static final int MAX_WAIT_TIME = 5000;

    /**
     * Default constructor.
     */
    public SwingWaitTestCase() {
        super();
    }

    /**
     * Parametricized constructor.
     *
     * @param name test name (actually test method name to be run)
     */
    public SwingWaitTestCase(final String name) {
        super(name);
    }

    /**
     * Component to wait on. This component should be drawn correctly for
     * the test to run correctly.
     */
    protected Component component;

    /**
     * Exception thrown during test execution if any.
     */
    protected Throwable exception;

    private static boolean bWasIllegalComponentStateException;

    private static Point next;

    /**
     * This method will be called after setUp and waiting and before
     * test running.
     */
    public void init() {
    }

    void internalRunBare() throws Throwable {
        // Call setUp on event-dispatch thread
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    setUp();
                } catch (Throwable e) {
                    exception = e;
                }
            }
        });
        // Wait for component to be realized (displayed) if any
        if (component != null) {
            isRealized(component);
        }
        // Calls init method to perform some actions after waiting and before
        // test running
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    init();
                    runTest();
                } catch (Throwable e) {
                    if (exception == null) {
                        exception = e;
                    }
                }
            }
        });
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    tearDown();
                } catch (Throwable e) {
                    if (exception == null) {
                        exception = e;
                    }
                }
            }
        });
    }

    @Override
    public void runBare() throws Throwable {
        internalRunBare();
        if (exception != null) {
            rethrow(exception);
        }
    }

    /**
     * Method delays current thread while Component isn't realized.
     *
     * TODO:Replace Component method invokes, as soon as new awtfunction for
     * Component is written (to check, that component is realized by single
     * method).
     */
    public static void isRealized(final Component c) {
        Point prev = null;
        int counter = 50;
        do {
            bWasIllegalComponentStateException = false;
            prev = next;
            next = null;
            try {
                // Get state from the component
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        try {
                            next = c.getLocationOnScreen();
                        } catch (IllegalComponentStateException e) {
                            bWasIllegalComponentStateException = true;
                        }
                    }
                });
                Thread.sleep(100);
            } catch (IllegalArgumentException e) {
            } catch (InterruptedException e) {
            } catch (InvocationTargetException e) {
            }
        } while (!c.isDisplayable() || !c.isVisible() || bWasIllegalComponentStateException
                || (next == null) || !next.equals(prev) || (counter-- <= 0));
        if (bWasIllegalComponentStateException) {
            System.err.println("bWasIllegalComponentStateException");
        }
        assertTrue("frame showing timeout", counter >= 0);
    }

    /*
     * Requests focus for the component and waits until it really
     * becomes focused.
     */
    public static void requestFocusInWindowForComponent(final Component c)
            throws InterruptedException, InvocationTargetException {
        final Component comp = c;
        final Window w = SwingUtilities.getWindowAncestor(comp);
        if (w == null) {
            fail("no window is provided");
            return;
        }
        long startTime = System.currentTimeMillis();
        while (!comp.isShowing() && (System.currentTimeMillis() - startTime) < MAX_WAIT_TIME) {
            Thread.sleep(10);
        }
        if (!comp.isShowing()) {
            fail("component is not showing");
            return;
        }
        startTime = System.currentTimeMillis();
        while (!w.isFocused() && (System.currentTimeMillis() - startTime) < MAX_WAIT_TIME) {
            Thread.sleep(10);
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                final boolean result = comp.requestFocusInWindow();
                assertTrue("focus can be gained", result || comp.isFocusOwner());
            }
        });
        startTime = System.currentTimeMillis();
        while (!comp.isFocusOwner() && (System.currentTimeMillis() - startTime) < MAX_WAIT_TIME) {
            Thread.sleep(10);
        }
        Thread.sleep(10);
    }
}