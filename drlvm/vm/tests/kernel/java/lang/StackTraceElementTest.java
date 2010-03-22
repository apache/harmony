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
 * @author Dmitry B. Yershov
 */
package java.lang;

import junit.framework.TestCase;

public class StackTraceElementTest extends TestCase {

    /**
     * Constructor under test StackTraceElement(String, int, String,
     * String, boolean)
     */
    public void testStackTraceElement() {
        String[] fileName = new String[] {null, "", "file"};
        int[] lineNumber = new int[] {-10, 0, 10};
        String declaringClass = "class";
        String methodName = "method";
        try {
            for (int i = 0;  i < 3; i++)
                for (int j = 0;  j < 3; j++)
                        new StackTraceElement(declaringClass, methodName, fileName[i],
                        lineNumber[j]);
        } catch (Throwable th) {
            fail("Constructor should work with all types of data");
        }
    }

    /**
     * Method under test boolean Equals(Object)
     */
    public void testEquals() {
        StackTraceElement ste = new StackTraceElement("class", "method", "file", 2);
        StackTraceElement ste1 = new StackTraceElement("class", "method", "file", -2);
        StackTraceElement ste2 = new StackTraceElement("class1", "method", "file", 2);
        StackTraceElement ste3 = new StackTraceElement("class", "method1", "file", 2);
        StackTraceElement ste4 = new StackTraceElement("class", "method", "file1", 2);
        StackTraceElement ste5 = new StackTraceElement("class", "method", "file", 2);
        assertFalse("Assert 0: objects should differ", ste.equals(new Object()));
        assertFalse("Assert 1: objects should differ", ste.equals(ste1));
        assertFalse("Assert 2: objects should differ", ste.equals(ste2));
        assertFalse("Assert 3: objects should differ", ste.equals(ste3));
        assertFalse("Assert 4: objects should differ", ste.equals(ste4));
        assertTrue("Assert 5: objects should equal", ste.equals(ste5));
    }

    /**
     * Method under test String getClassName()
     */
    public void testGetClassName() {
        StackTraceElement ste = new StackTraceElement("class", "method", "file", 1);
        assertEquals("incorrect class name", "class", ste.getClassName());
    }

    /**
     * Method under test String getFileName()
     */
    public void testGetFileName() {
        StackTraceElement ste = new StackTraceElement("class", "method", "file", 1);
        assertEquals("incorrect file name", "file", ste.getFileName());
    }

    /**
     * Method under test int getLineNumber()
     */
    public void testGetLineNumber() {
        StackTraceElement ste = new StackTraceElement("class", "method", "file", 1);
        assertEquals("incorrect line number", 1, ste.getLineNumber());
    }

    /**
     * Method under test String getMethodName()
     */
    public void testGetMethodName() {
        StackTraceElement ste = new StackTraceElement("class", "method", "file", 1);
        assertEquals("incorrect file name", "method", ste.getMethodName());
    }

    /**
     * Method under test int hashCode()
     */
    public void testHashCode() {
        StackTraceElement ste = new StackTraceElement("class", "method", "file", 1);
        StackTraceElement ste1 = new StackTraceElement("class", "method", "file", 1);
        assertEquals("hash codes should equal", ste.hashCode(), ste1.hashCode());
    }

    /**
     * Method under test boolean isNativeMethod()
     * when file name is null
     */
    public void testIsNativeMethod() {
        StackTraceElement ste = new StackTraceElement("class", "method", null, -2);
        assertTrue("method should be native", ste.isNativeMethod());
        ste = new StackTraceElement("class", "method", "file", 1);
        assertFalse("method should not be native", ste.isNativeMethod());
    }

    /**
     * Method under test boolean isNativeMethod()
     * when file name is not null
     */
    public void testIsNativeMethod_FileNotNull() {
        StackTraceElement ste = new StackTraceElement("class", "method", "file", -2);
        assertTrue("method should be native", ste.isNativeMethod());
        ste = new StackTraceElement("class", "method", "file", 1);
        assertFalse("method should not be native", ste.isNativeMethod());
    }

    /**
     * Method under test String toString()
     */
    public void testToString() {
        StackTraceElement ste = new StackTraceElement("class", "method", "file", 1);
        assertEquals("Assert 0: incorrect output", "class.method(file:1)", ste.toString());
        ste = new StackTraceElement("class", "method", "file", -1);
        assertEquals("Assert 0: incorrect output", "class.method(file)", ste.toString());
        ste = new StackTraceElement("class", "method", null, 1);
        assertEquals("Assert 0: incorrect output", "class.method(Unknown Source)", ste.toString());
        ste = new StackTraceElement("class", "method", null, -1);
        assertEquals("Assert 0: incorrect output", "class.method(Unknown Source)", ste.toString());
        ste = new StackTraceElement("class", "method", null, -2);
        assertEquals("Assert 0: incorrect output", "class.method(Native Method)", ste.toString());
    }

    /**
     * Method under test String toString()
     * when file name is null and line number is -2
     */
    public void testToString_FileNotNullLineMinus2() {
        StackTraceElement ste = new StackTraceElement("class", "method", "file", -2);
        assertEquals("incorrect output", "class.method(file)", ste.toString());
    }
}
