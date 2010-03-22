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
package javax.swing.text;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests AbstractDocument.AbstractElement class - the part which
 * implements MutableAttributeSet interface - when no the document
 * is not write-locked. This class constructs TestSuite to include only
 * those test-methods which modify the attribute set.
 *
 */
public class AbstractDocument_AbstractElement_MASNoLockTest extends
        AbstractDocument_AbstractElement_MASTest {
    public AbstractDocument_AbstractElement_MASNoLockTest(final String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new AbstractDocument_AbstractElement_MASNoLockTest("testAddAttribute"));
        suite.addTest(new AbstractDocument_AbstractElement_MASNoLockTest("testAddAttributes"));
        suite
                .addTest(new AbstractDocument_AbstractElement_MASNoLockTest(
                        "testRemoveAttribute"));
        suite.addTest(new AbstractDocument_AbstractElement_MASNoLockTest(
                "testRemoveAttributesAttributeSetDiff"));
        suite.addTest(new AbstractDocument_AbstractElement_MASNoLockTest(
                "testRemoveAttributesAttributeSetSame"));
        suite.addTest(new AbstractDocument_AbstractElement_MASNoLockTest(
                "testRemoveAttributesEnumeration"));
        suite.addTest(new AbstractDocument_AbstractElement_MASNoLockTest(
                "testAddAttributeAnotherThread"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aDocument.writeUnlock();
    }

    @Override
    protected void tearDown() throws Exception {
        // Lock the document 'cause super tearDown unlocks it
        aDocument.writeLock();
        super.tearDown();
    }

    @Override
    public void testAddAttribute() {
        try {
            super.testAddAttribute();
            fail("Error should be thrown, the reason " + "being no write lock acquired");
        } catch (Error e) {
        }
    }

    @Override
    public void testAddAttributes() {
        try {
            super.testAddAttributes();
            fail("Error should be thrown, the reason " + "being no write lock acquired");
        } catch (Error e) {
        }
    }

    @Override
    public void testRemoveAttribute() {
        try {
            super.testRemoveAttribute();
            fail("Error should be thrown, the reason " + "being no write lock acquired");
        } catch (Error e) {
        }
    }

    @Override
    public void testRemoveAttributesAttributeSetDiff() {
        try {
            super.testRemoveAttributesAttributeSetDiff();
            fail("Error should be thrown, the reason " + "being no write lock acquired");
        } catch (Error e) {
        }
    }

    @Override
    public void testRemoveAttributesAttributeSetSame() {
        try {
            super.testRemoveAttributesAttributeSetSame();
            fail("Error should be thrown, the reason " + "being no write lock acquired");
        } catch (Error e) {
        }
    }

    @Override
    public void testRemoveAttributesEnumeration() {
        try {
            super.testRemoveAttributesEnumeration();
            fail("Error should be thrown, the reason " + "being no write lock acquired");
        } catch (Error e) {
        }
    }

    @Override
    public void testSetResolveParent() {
        try {
            super.testSetResolveParent();
            fail("Error should be thrown, the reason " + "being no write lock acquired");
        } catch (Error e) {
        }
    }

    /**
     * Becomes true when the exception in the Runnable object is thrown.
     * Used in testAddAttributeAnotherThread.
     */
    static boolean exceptionThrown = false;

    /**
     * Tests that a concurrently running thread can't modify an element
     * attributes, while a document is write-locked by another thread.
     *
     * @throws InterruptedException if sleep is interrupted.
     */
    public void testAddAttributeAnotherThread() throws InterruptedException {
        aDocument.writeLock();
        new Thread(new Runnable() {
            public void run() {
                try {
                    aElement.addAttribute(keyInResolver, valueInResolver);
                } catch (Error e) {
                    exceptionThrown = true;
                }
            }
        }).start();
        Thread.sleep(500);
        assertTrue(exceptionThrown);
    }
}