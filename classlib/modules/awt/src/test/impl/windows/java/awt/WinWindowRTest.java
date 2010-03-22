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
 * @author Dmitry A. Durnev
 */
package java.awt;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import junit.framework.TestCase;

/**
 * 
 * WinWindowRTest
 * This test is valid only on Windows!
 * 
 */
public class WinWindowRTest extends TestCase {
    private Throwable error;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(WinWindowRTest.class);
    }
    
    public final void testSetVisible() throws Throwable {
        Frame frame = new Frame();
        
        try {
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    try {
                        assertTrue("window is focused before opened", e
                                .getWindow().isFocused());
                    } catch (Throwable err) {
                        error = err;
                    }
                }
            });
            
            frame.pack();
            frame.setVisible(true);
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                // reset interrupt flag, in case someone else cares
                Thread.currentThread().interrupt();
            }
            
            if (error != null) {
                throw error;
            }
        } finally {
            frame.dispose();
        }
    }
}
