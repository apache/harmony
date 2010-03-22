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

package org.apache.harmony.drlvm.tests.regression.h4514;
import junit.framework.*;
public class Test extends TestCase {
    
    public void testTrace1() {
        //some useless code
        int i=0;
        int j=1;
        try {
            assertEquals(0, 1); //the number of this line must be in tracktracelement
        } catch (Throwable e) {
            StackTraceElement thisFrame = findThisFrame(e);
            assertEquals(27, thisFrame.getLineNumber());
        }
    }

    public void testTrace2() {
        //some useless code
        int i=0;
        int j=1;
        try {
            fail();//the number of this line must be in tracktracelement
        } catch (Throwable e) {
            StackTraceElement thisFrame = findThisFrame(e);
            assertEquals(39, thisFrame.getLineNumber());
        }
    }

    public void testTrace3() {
        //some useless code
        int i=0;
        int j=1;
        try {
            assertEquals(true, false);//the number of this line must be in tracktracelement
        } catch (Throwable e) {
            StackTraceElement thisFrame = findThisFrame(e);
            assertEquals(51, thisFrame.getLineNumber());
        }

    }

    public void testTrace4() {
        //some useless code
        int i=0;
        int j=1;
        try {
            assertNotNull(null);//the number of this line must be in tracktracelement
        } catch (Throwable e) {
            StackTraceElement thisFrame = findThisFrame(e);
            assertEquals(64, thisFrame.getLineNumber());
        }

    }

    public void testTrace5() {
        //some useless code
        int i=0;
        int j=1;
        try {
            assertEquals("", "fail");//the number of this line must be in tracktracelement
        } catch (Throwable e) {
            StackTraceElement thisFrame = findThisFrame(e);
            assertEquals(77, thisFrame.getLineNumber());
        }

    }

    static StackTraceElement findThisFrame(Throwable e) {
        StackTraceElement[] frames =  e.getStackTrace();
        for (StackTraceElement frame : frames) {
            if (frame.getClassName().equals(Test.class.getName())) {
                return frame;
            }
        }
        return null;
    }
}
