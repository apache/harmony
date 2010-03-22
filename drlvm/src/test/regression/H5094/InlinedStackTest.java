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

package org.apache.harmony.drlvm.tests.regression.h5094;

import junit.framework.TestCase;

public class InlinedStackTest extends TestCase {

    private static void a() throws Exception{
        smth();
        b();
    }
    private static void b() throws Exception{
        c();
    }

    static void c() throws Exception{
        smth();
        d();
    }
    static void d() throws Exception{
        smth();
        smth();
        throw new RuntimeException();
    }

    static int i = 0;
    static void smth() {
        i++;
    }

    public static void assertStackFrame(String clsname, String methname, int ln, StackTraceElement stf) {
        assertEquals(clsname, stf.getClassName());
        assertEquals(methname, stf.getMethodName());
        assertEquals(ln, stf.getLineNumber());
    }

    public void test1() throws Exception {
        try {
            a();
        } catch (Throwable t) {
            t.printStackTrace();
            StackTraceElement[] st = t.getStackTrace();
            assertTrue("trace is not deep enough: " + st.length, st.length >= 6);
            assertStackFrame(this.getClass().getName(), "d", 39, st[0]);
            assertStackFrame(this.getClass().getName(), "c", 34, st[1]);
            assertStackFrame(this.getClass().getName(), "b", 29, st[2]);
            assertStackFrame(this.getClass().getName(), "a", 26, st[3]);
            assertStackFrame(this.getClass().getName(), "test1", 55, st[4]);
        }
    }

    public void test2() throws Exception {
        try {
            Q.a();
        } catch (Throwable t) {
            t.printStackTrace();
            StackTraceElement[] st = t.getStackTrace();
            assertTrue("trace is not deep enough: " + st.length, st.length >= 6);
            assertStackFrame(this.getClass().getName(), "d", 39, st[0]);
            assertStackFrame(Q.class.getName(), "c", 108, st[1]);
            assertStackFrame(Q.class.getName(), "b", 104, st[2]);
            assertStackFrame(Q.class.getName(), "a", 100, st[3]);
            //assertStackFrame(this.getClass().getName(), "test2", 70, st[4]); //OPT to be fixed yet
        }
    }

    public void test3() throws Exception {
        try {
            Q.a2();
        } catch (Throwable t) {
            t.printStackTrace();
            StackTraceElement[] st = t.getStackTrace();
            assertTrue("trace is not deep enough: " + st.length, st.length >= 5);
            assertStackFrame(this.getClass().getName(), "d", 39, st[0]);
            assertStackFrame(this.getClass().getName(), "c", 34, st[1]);
            assertStackFrame(Q.class.getName(), "a2", 112, st[2]);
            assertStackFrame(this.getClass().getName(), "test3", 85, st[3]);
        }
    }
}

class Q {
    static void a() throws Exception{
        b();
    }
    private static void b() throws Exception{
        InlinedStackTest.smth();
        c();
    }

    private static void c() throws Exception{
        InlinedStackTest.d();
    }

    static void a2() throws Exception{
        InlinedStackTest.c();
    }
}
