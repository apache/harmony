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
 * @author Roman S. Bushmanov
 */

package java.lang;

import junit.framework.TestCase;
import java.util.*;

/**
 * Unit test for java.lang.Object class.
 */

public class ObjectTest extends TestCase {

    private Object obj1 = new Object();

    private Object obj2 = new Object();

    private Object obj3 = obj1;

    public void testGetClass1() {
        assertEquals("java.lang.Object", obj1.getClass().getName());
    }

    public void testGetClass2() {
        Object o = new Boolean(true);
        assertEquals("java.lang.Boolean", o.getClass().getName());
    }

    /**
     * Verify 1.5 getClass()
     */
    public void testGetClass3() {
        Object o = new Boolean(true);
        Class<? extends Object> c = o.getClass();
        assertEquals("java.lang.Boolean", c.getName());
    }

    /**
     * Verify 1.5 getClass()
     */
    public void testGetClass4() {
        AbstractMap<Integer, String> hm = new HashMap<Integer, String>();
        Class<? extends AbstractMap> c = hm.getClass();
        assertEquals("java.util.HashMap", c.getName());
    }

    public void testHashCode1() {
        assertEquals("Hash code for the same object must be the same!", 
                obj1.hashCode(), obj3.hashCode());
    }

    public void testEquals1() {
        assertTrue("Equilvalence relation must be reflexive!", 
                obj1.equals(obj1));
    }

    public void testEguals2() {
        assertFalse("Different objects must be not equal!", obj1.equals(obj2));
    }

    public void testClone1() {
        try {
            ObjectDescendant o = new ObjectDescendant();
            o.testClone();
        } catch (CloneNotSupportedException e) {
            return;
        }
        fail("CloneNotSupported exception must be thrown if an object " +
                "doesn't implement Cloneabe interface!");
    }

    public void testClone2() {
        try {
            CloneableObjectDescendant o = new CloneableObjectDescendant(1, obj1);
            CloneableObjectDescendant clone = (CloneableObjectDescendant) o
                    .testClone();
            assertEquals("Assert 0:", 1, clone.getPrimitiveVal());
            assertEquals("Assert 1:", obj1, clone.getReferenceVal());
        } catch (CloneNotSupportedException e) {
            fail("Should not throw CloneNotSupported exception!");
        }
    }

    public void testToString() {
        Object o = new int[0];
        assertEquals("[I@" + Integer.toHexString(o.hashCode()), o.toString());
    }

    public void testNotifyAll() {
        Object o = new Object();
        //create and start two test threads
        TestThread t1 = new TestThread1(o);
        TestThread t2 = new TestThread1(o);
        t1.start();
        t2.start();
        //wait until both threads are started
        for (int i = 0; !(t1.flag && t2.flag) && i < 60; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail("The main thread was interrupted.");
            }
        }
        assertTrue("Test threads did not start for a long time!", 
                t1.flag && t2.flag);
        //now both test threads in the wait set of object o
        //notify both waiting threads
        synchronized (o) {
            o.notifyAll();
        }
        //wait until both threads are notified
        for (int i = 0; t1.flag || t2.flag && i < 60; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail("The main thread was interrupted.");
            }
        }
        assertFalse("At least one of two test threads is not notified " +
                    "for a long time!",
                t1.flag || t2.flag);
    }

    public void testNotify1() {
        Object o = new Object();
        try {
            o.notify();
        } catch (IllegalMonitorStateException e) {
            return;
        }
        fail("An IllegalMonitorStateException should be thrown!");
    }
    
    public void testNotify() {
        Object o = new Object();
        //create and start two test threads
        TestThread t1 = new TestThread1(o);
        TestThread t2 = new TestThread1(o);
        t1.start();
        t2.start();
        //wait until both threads are started
        for (int i = 0; !(t1.flag && t2.flag) && i < 60; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail("The main thread was interrupted.");
            }
        }
        assertTrue("Test threads did not start for a long time!",
                t1.flag && t2.flag);
        //now both test threads wait for object o
        //notify one of two waiting threads
        synchronized (o) {
            o.notify();
        }
        //wait until one of two threads is notified
        for (int i = 0; t1.flag && t2.flag && i < 60; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail("The main thread was interrupted.");
            }
        }
        for (int i = 0; t1.flag && t2.flag && i < 60; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail("The main thread was interrupted.");
            }
        }
        assertFalse("None of two test threads is notified for a long time!",
                t1.flag && t2.flag);
        assertFalse("Both test threads were notified instead of one!",
                !t1.flag && !t2.flag);
    }

    public void testWaitlongint() {
        long millis = 500;
        int nanos = 500;
        long start = 0;
        long finish = 0;
        synchronized (obj1) {
            try {
                start = System.currentTimeMillis();
                obj1.wait(millis, nanos);
                finish = System.currentTimeMillis();
            } catch (InterruptedException e) {
                fail("The main thread was interrupted!");
            }
        }
        long atLeastWait = millis - 40;
        long actualWait = finish - start;
        assertTrue("Current thread hasn't slept enough: " + 
                "expected at least " + atLeastWait + " but was " + actualWait,
                actualWait >= atLeastWait);
    }

    public void testWaitlongint1() {
        try {
            obj1.wait(500, 1000000);
        } catch (IllegalArgumentException e) {
            return;
        } catch (InterruptedException e) {
            fail("The main thread was interrupted!");
        }
        fail("An IllegalArgumentException must be thrown!");
    }

    public void testWaitlong() {
        long timeout = 500;
        long start = 0;
        long finish = 0;
        Object o = new Object();
        synchronized (o) {
            try {
                start = System.currentTimeMillis();
                o.wait(timeout);
                finish = System.currentTimeMillis();
            } catch (InterruptedException e) {
                fail("The main thread was interrupted!");
            }
        }
        // the sleeping time is more or less equal to timeout
        // so we allow 10% less time (there was bug filed on this issue)
        assertTrue("Current thread hasn't slept enough!",
                finish - start + 1 > timeout - timeout/10);
    }

    public void testWait() {
        TestThread t = new TestThread() {
            public void run() {
                try {
                    (new Object()).wait();
                } catch (InterruptedException e) {
                } catch (IllegalMonitorStateException e) {
                    flag = true;
                }
            }
        };
        t.start();
        int i = 0;
        while (t.isAlive() && i < 300) {
            try {
                t.join(10);
                i++;
            } catch (Exception e) {
                fail("The main thread was interrupted!");
            }
        }
        assertTrue("An IllegalMonitorStateException must be thrown "
                + "in test thread!", t.flag);
        if (t.isAlive()) {
            fail("thread has not finished!");
        }
    }

    public void testWait1() {
        final Object o = new Object();
        TestThread t = new TestThread() {
            public void run() {
                try {
                    synchronized (o) {
                        threadStarted = true;
                        o.notify();
                        o.wait();
                    }
                } catch (InterruptedException e) {
                    flag = true;
                }
            }
        };
        try {
        	synchronized (o) {
                t.start();
                while (!t.threadStarted) {
                	o.wait();
                }
        	} 
        } catch (InterruptedException e) {
    		fail("The main thread was interrupted!");
        }
        assertTrue("thread must be alive", t.isAlive());
        t.interrupt();
        for (int i = 0; !t.flag && i < 300; i++) {
            try {
                t.join(10);
            } catch (Exception e) {
                fail("The main thread was interrupted_2!");
            }
        }
        assertTrue("An InterruptedException must be thrown in test thread!",
                t.flag);
    }

    private class ObjectDescendant {
        void testClone() throws CloneNotSupportedException {
            clone();
        }
    }

    private class CloneableObjectDescendant implements Cloneable {

        private int primitiveField = -1;

        private Object objectField = null;

        CloneableObjectDescendant(int primitiveVal, Object referenceVal) {
            primitiveField = primitiveVal;
            objectField = referenceVal;
        }

        int getPrimitiveVal() {
            return primitiveField;
        }

        Object getReferenceVal() {
            return objectField;
        }

        Object testClone() throws CloneNotSupportedException {
            return clone();
        }
    }

    private class TestThread extends Thread {
        boolean flag = false;
        volatile boolean threadStarted = false;
    }

    private class TestThread1 extends TestThread {

        private Object lock;

        TestThread1(Object lock) {
            this.lock = lock;
        }

        public void run() {
            try {
                synchronized (lock) {
                    flag = true;
                    lock.wait();
                }
                flag = false;
            } catch (InterruptedException e) {
            }
        }
    }
}