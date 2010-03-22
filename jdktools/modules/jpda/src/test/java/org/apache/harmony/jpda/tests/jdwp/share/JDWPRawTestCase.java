/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Vitaly A. Provodin
 */

/**
 * Created on 01.02.2005
 */
package org.apache.harmony.jpda.tests.jdwp.share;

import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.share.JPDALogWriter;
import org.apache.harmony.jpda.tests.share.JPDATestOptions;

import junit.framework.TestCase;

/**
 * Basic class for all JDWP unit tests based on <code>JUnit</code> framework.
 * <p>
 * This class extends JUnit interface <code>TestCase</code> and implements
 * <code>setUp()</code> and <code>tearDown()</code> being common for all
 * JDWP tests.
 * <p>
 * It also introduces <code>internalSetUp()</code> and
 * <code>internalTearDown()</code> that can be implemented to supply safe
 * start up and shut down of debuggee.
 */
public abstract class JDWPRawTestCase extends TestCase {

    /** Where to print log messages. */
    protected JPDALogWriter logWriter;

    /** Test run options. */
    protected JPDATestOptions settings;

    /**
     * This method should be overridden in derived classes to return full name
     * for debuggee class.
     * 
     * @return full debuggee class name
     */
    protected abstract String getDebuggeeClassName();

    /**
     * This method will be invoked before starting each test.
     * 
     * @throws Exception
     *             if any error occurs
     */
    protected void internalSetUp() throws Exception {
    }

    /**
     * This method will be invoked after each test completed or any error
     * occurred.
     */
    protected void internalTearDown() {
    }

    /**
     * Overrides inherited JUnit method to provide initialization and invocation
     * of internalSetUp() and internalTearDown() methods.
     */
    protected void setUp() throws Exception {
        super.setUp();

        settings = createTestOptions();
        settings.setDebuggeeClassName(getDebuggeeClassName());

        logWriter = new JPDALogWriter(System.out, null, settings.isVerbose());

        logWriter.println("\n=====================================>>>");
        logWriter.println("Run: " + getClass().getName() + "." + getName());
        logWriter.println("----------------------------------------");

        try {
            internalSetUp();
            logWriter.println("----------------------------------------");
        } catch (Throwable e) {
            logWriter.printError(e);
            logWriter.println("----------------------------------------");
            internalTearDown();
            throw new TestErrorException(e);
        }
    }

    /**
     * Creates wrapper object for accessing test options;
     */
    protected JPDATestOptions createTestOptions() {
        return new JPDATestOptions();
    }
    
    /**
     * Overrides inherited JUnit method to provide cleanup and invocation of
     * internalTearDown() method.
     */
    protected void tearDown() throws Exception {
        logWriter.println("----------------------------------------");
        internalTearDown();
        logWriter.println("<<<=====================================\n");

        super.tearDown();
    }
}
