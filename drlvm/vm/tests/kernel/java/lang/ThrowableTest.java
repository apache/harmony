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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import junit.framework.TestCase;

public class ThrowableTest extends TestCase {

    private static final boolean isLinux = System.getProperty("os.name")
    .toLowerCase().indexOf("linux") != -1;
    
    private static final String printOutput = 
        "java.lang.Throwable: Throwable 0\n" +
        "\tat d.d(d:1)\n\tat c.c(c:1)\n\tat b.b(Native Method)\n\tat a.a(a:1)\n" +
        "Caused by: java.lang.Throwable: Throwable 1\n" +
        "\tat f.f(f:1)\n\tat e.e(Native Method)\n\tat d.d(d:1)\n\t... 3 more\n" +
        "Caused by: java.lang.Throwable: Throwable 2\n" +
        "\tat d.d(d:1)\n\t... 3 more" + (isLinux ? "\n" : "\r\n");

    /**
     * Constructor under test Throwable(String)
     */
    public void testThrowableString() {
        Throwable th = new Throwable("aaa");
        assertEquals("incorrect message", "aaa", th.getMessage());
        assertNull("cause should be null", th.getCause());
        assertTrue("empty stack trace", th.getStackTrace().length > 0);
    }

    /**
     * Constructor under test Throwable(String, Throwable)
     */
    public void testThrowableStringThrowable() {
        Throwable th1 = new Throwable();
        Throwable th = new Throwable("aaa", th1);
        assertEquals("incorrect message", "aaa", th.getMessage());
        assertSame("incorrect cause", th1, th.getCause());
        assertTrue("empty stack trace", th.getStackTrace().length > 0);
    }

    /**
     * Constructor under test Throwable(Throwable)
     */
    public void testThrowableThrowable() {
        Throwable th1 = new Throwable("aaa");
        Throwable th = new Throwable(th1);
        assertEquals("incorrect message", 
                     "java.lang.Throwable: aaa", th.getMessage());
        assertSame("incorrect cause", th1, th.getCause());
        assertTrue("empty stack trace", th.getStackTrace().length > 0);
    }

    /**
     * fillInStackTrace() should return reference to <code>this</code>
     * of the target Throwable.
     */
    public void testFillInStackTrace_return() {
        
        Throwable th = new Throwable();
        assertSame(th, th.fillInStackTrace());
    }

    private Throwable makeThrowable() {
        return new Throwable();
    }
    
    private void updateThrowable(Throwable t, int depth) {
        if (depth-- > 0) {
            updateThrowable(t, depth);
        } else {
            t.fillInStackTrace();
        }
    }
    
    /**
     * fillInStackTrace() should not change recorded stack info 
     * on subsequent invocations.
     */
    public void testFillInStackTrace_manytimes() {
        Throwable th = makeThrowable();
        int len1 = th.getStackTrace().length;
        th.fillInStackTrace();
        assertEquals("case 1", len1, th.getStackTrace().length);
        updateThrowable(th, 10);
        assertEquals("case 2", len1, th.getStackTrace().length);
    }

    /**
     * Method under test Throwable getCause()
     */
    public void testGetCause() {
        Throwable th = new Throwable();
        Throwable th1 = new Throwable();
        assertNull("cause should be null", th.getCause());
        th = new Throwable(th1);
        assertSame("incorrect cause", th1, th.getCause());
    }

    /**
     * Method under test String getLocalizedMessage()
     */
    public void testGetLocalizedMessage() {
        Throwable th = new Throwable("aaa");
        assertEquals("incorrect localized message",
                     "aaa", th.getLocalizedMessage());
    }

    /**
     * Method under test String getMessage()
     */
    public void testGetMessage() {
        Throwable th = new Throwable("aaa");
        assertEquals("incorrect message", "aaa", th.getMessage());
    }

    /**
     * Method under test StackTraceElement[] getStackTrace()
     */
    public void testGetStackTrace() {
        StackTraceElement[] ste = new StackTraceElement[1]; 
        ste[0] = new StackTraceElement("class", "method", "file", -2);
        Throwable th = new Throwable("message");
        th.setStackTrace(ste);
        ste = th.getStackTrace();
        assertEquals("incorrect length", 1, ste.length);
        assertEquals("incorrect file name", "file", ste[0].getFileName());
        assertEquals("incorrect line number", -2, ste[0].getLineNumber());
        assertEquals("incorrect class name", "class", ste[0].getClassName());
        assertEquals("incorrect method name", "method", ste[0].getMethodName());
        assertTrue("native method should be reported", ste[0].isNativeMethod());
    }

    /**
     * Method under test Throwable initCause(Throwable)
     */
    public void testInitCause() {
        Throwable th = new Throwable();
        Throwable th1 = new Throwable();
        th.initCause(th1);
        assertSame("incorrect cause", th1, th.getCause());
        th = new Throwable();
        th.initCause(null);
        assertNull("cause should be null", th.getCause());
        th = new Throwable();
        try {
            th.initCause(th);
            fail("Throwable initialized by itself");
        } catch (IllegalArgumentException ex) {
        }
        th = new Throwable((Throwable)null);
        try {
            th.initCause(th1);
            fail("Second initialization");
        } catch (IllegalStateException ex) {
        }
        th = new Throwable(th1);
        try {
            th.initCause(th1);
            fail("Second initialization");
        } catch (IllegalStateException ex) {
        }
        th = new Throwable();
        th1 = th.initCause(th1);
        assertSame("incorrect value returned from initCause", th, th1);
    }

    /**
     * Method under test void setStackTrace(StackTraceElement[])
     */
    public void testSetStackTrace() {
        StackTraceElement[] ste = new StackTraceElement[2]; 
        ste[0] = new StackTraceElement("class", "method", "file", -2);
        ste[1] = new StackTraceElement("class", "method", "file", 1);
        Throwable th = new Throwable();
        th.setStackTrace(ste);
        ste = th.getStackTrace();
        assertEquals("incorrect length", 2, ste.length);
        assertEquals("incorrect file name", "file", ste[0].getFileName());
        assertEquals("incorrect line number", -2, ste[0].getLineNumber());
        assertEquals("incorrect class name", "class", ste[0].getClassName());
        assertEquals("incorrect method name", "method", ste[0].getMethodName());
        assertTrue("native method should be reported", ste[0].isNativeMethod());
        assertEquals("incorrect file name", "file", ste[1].getFileName());
        assertEquals("incorrect line number", 1, ste[1].getLineNumber());
        assertEquals("incorrect class name", "class", ste[1].getClassName());
        assertEquals("incorrect method name", "method", ste[1].getMethodName());
        assertFalse("native method should NOT be reported", ste[1].isNativeMethod());
        ste[1] = null;
        try {
            th.setStackTrace(ste);
        } catch (NullPointerException ex) {
        }
        ste = null;
        try {
            th.setStackTrace(ste);
        } catch (NullPointerException ex) {
        }
    }

    /**
     * Method under test void setStackTrace(StackTraceElement[]).
     * Null arguments are verified
     */
    public void testSetStackTrace_Null() {
        Throwable th = new Throwable();
        StackTraceElement[] ste = null;
        try {
            th.setStackTrace(ste);
            fail("Assert 1: NullPointerException should be thrown");
        } catch (NullPointerException ex) {
        }
        ste = new StackTraceElement[2];
        ste[0] = new StackTraceElement("class", "method", "file", -2);
        ste[1] = null;
        try {
            th.setStackTrace(ste);
            fail("Assert 2: NullPointerException should be thrown");
        } catch (NullPointerException ex) {
        }
    }
    
    
    /**
     * Method under test String toString()
     */
    public void testToString() {
        Throwable th = new Throwable("aaa");
        assertEquals("incorrect String representation", 
                     "java.lang.Throwable: aaa", th.toString());
    }

    /**
     * Method under test:
     *      - void printStackTrace(PrintStream) 
     */
    public void testPrintStackTracePrintStream() {
        Throwable th;
        ByteArrayOutputStream ba = new ByteArrayOutputStream(); 
        PrintStream ps = new PrintStream(ba);
        th = prepareThrowables();
        th.printStackTrace(ps);
        assertEquals("incorrect info printed in stack trace",
                     printOutput, ba.toString());
    }

    /**
     * Methods under test:
     *      - void printStackTrace(PrintWriter),
     * where PrintWriter does not flush automatically.
     */
    public void testPrintStackTracePrintWriter_False() {
        ByteArrayOutputStream ba = new ByteArrayOutputStream(); 
        PrintWriter ps = new PrintWriter(ba, false);
        try {
            throw new Exception("level1", new Exception("level2", new Exception("level3", new NullPointerException())));
        } catch (Exception e) {
            e.printStackTrace(ps);
        }
        assertEquals("the output should be empty until flush",
                     "", ba.toString());
    }

    /**
     * Methods under test:
     *      - void printStackTrace(PrintWriter),
     * where PrintWriter flushes automatically 
     */
    public void testPrintStackTracePrintWriter_True() {
        Throwable th;
        ByteArrayOutputStream ba = new ByteArrayOutputStream(); 
        PrintWriter ps = new PrintWriter(ba, true);
        th = prepareThrowables();
        th.printStackTrace(ps);
        assertEquals("incorrect info in printed stack trace",
                     printOutput, ba.toString());
    }
    
    private Throwable prepareThrowables() {
        Throwable th, th1, th2;
        StackTraceElement[] ste = new StackTraceElement[4];
        StackTraceElement[] ste1 = new StackTraceElement[6];

        ste[0] = new StackTraceElement("d", "d", "d", 1);
        ste[1] = new StackTraceElement("c", "c", "c", 1);
        ste[2] = new StackTraceElement("b", "b", null, -2);
        ste[3] = new StackTraceElement("a", "a", "a", 1);

        ste1[0] = new StackTraceElement("f", "f", "f", 1);
        ste1[1] = new StackTraceElement("e", "e", null, -2);
        ste1[2] = new StackTraceElement("d", "d", "d", 1);
        ste1[3] = new StackTraceElement("c", "c", "c", 1);
        ste1[4] = new StackTraceElement("b", "b", null, -2);
        ste1[5] = new StackTraceElement("a", "a", "a", 1);
        
        th2 = new Throwable("Throwable 2");
        th2.setStackTrace(ste);
        th1 = new Throwable("Throwable 1");
        th1.initCause(th2);
        th1.setStackTrace(ste1);
        th = new Throwable("Throwable 0", th1);
        th.setStackTrace(ste);
        return th;
    }
}
