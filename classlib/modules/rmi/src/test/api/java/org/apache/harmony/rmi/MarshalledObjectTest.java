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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author  Mikhail A. Markov
 */
package org.apache.harmony.rmi;

import java.rmi.MarshalledObject;

import java.io.Serializable;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Unit test for java.rmi.MarshalledObject class.
 *
 * @author  Mikhail A. Markov
 */
public class MarshalledObjectTest extends TestCase {

    /**
     * No-arg constructor to enable serialization.
     */
    public MarshalledObjectTest() {
        super();
    }

    /**
     * Constructs this test case with the given name.
     *
     * @param   name
     *          Name for this test case.
     */
    public MarshalledObjectTest(String name) {
        super(name);
    }

    /**
     * Tests {@link MarshalledObject#equals(Object)} method.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    public void testEquals() throws Exception {
        String str = new String("TEST");
        String str1 = new String("TEST");
        String str2 = new String("TEST2");

        assertTrue(new MarshalledObject(str).equals(
                new MarshalledObject(str1)));
        assertTrue(! new MarshalledObject(str).equals(
                new MarshalledObject(str2)));
    }

    /**
     * Tests {@link MarshalledObject#get()} method.
     *
     * @throws  Exception
     *          If some error occurs.
     */
    public void testGet() throws Exception {
        Hashtable ht = new Hashtable();
        String str = new String("TEST");

        assertNull(new MarshalledObject(null).get());
        assertEquals(str, new MarshalledObject(str).get());
        ht.put(new Integer(1), str);
        ht.put(new Integer(2), "TEST1");
        MarshalledObject mo = new MarshalledObject(ht);
        assertEquals(ht, mo.get());
        ht.put(new Integer(2), "TEST2");
        assertTrue(! ht.equals(mo.get()));
    }

    /**
     * Tests if RMI runtime is able to load classes if thread context classloader
     * is null (regression test for HARMONY-1967)
     */
    public void testNullLoader() throws Exception {
        ClassLoader old_cl = Thread.currentThread().getContextClassLoader();

        try {
            MarshalledObject mo = new MarshalledObject(new TestClass());
            Object obj = null;

            // 1-st get: thread context classloader is equal to system classloader
            obj = mo.get();

            if (obj.getClass().getClassLoader() != old_cl) {
                fail("1-st get failed: loaded through: "
                        + obj.getClass().getClassLoader() + ", expected: " + old_cl);
            } else {
                System.out.println("1-st get passed.");
            }

            // 2-nd get: thread context classloader is equal to null
            Thread.currentThread().setContextClassLoader(null);

            obj = mo.get();

            if (obj.getClass().getClassLoader() != old_cl) {
                fail("2-nd get failed: loaded through: "
                        + obj.getClass().getClassLoader() + ", expected: " + old_cl);
            } else {
                System.out.println("2-nd get passed.");
            }
        } finally {
            // restore thread context classloader
            Thread.currentThread().setContextClassLoader(old_cl);
        }
    }

    /**
     * Returns test suite for this class.
     *
     * @return  Test suite for this class.
     */
    public static Test suite() {
        return new TestSuite(MarshalledObjectTest.class);
    }

    /**
     * Starts the testing from the command line.
     *
     * @param   args
     *          Command line parameters.
     */
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    /**
     * Auxiliary empty class.
     */
    static class TestClass implements Serializable {
    }
}
