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
package java.awt;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

public class EventDispatchThreadRTest extends TestCase {
    
    /**
     * Regression test for JIRA issue HARMONY-2818
     */
    public final void testHARMONY2818() throws Throwable {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    throw new RuntimeException("expected from EDT"); //$NON-NLS-1$
                }
            }
        );
        EventQueue.invokeAndWait(
            new Runnable() {
                public void run() {
                }
            }
        );
    }
  
// FIXME: Test deemed to be invalid  
//    public void testHarmony2116() throws InterruptedException {
//        final ByteArrayOutputStream out = new ByteArrayOutputStream();
//        final PrintStream err = System.err;
//        final Toolkit tk;
//
//        System.setErr(new PrintStream(out));
//        tk = new ToolkitImpl() {
//            protected EventQueue getSystemEventQueueImpl() {
//                return null;
//            }
//        };
//
//        Thread.sleep(100);
//        tk.dispatchThread.shutdown();
//        tk.dispatchThread.join(3000);
//        System.setErr(err);
//        assertEquals(0, out.size());
//    }
}