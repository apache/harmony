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
 * @author Pavel Dolgov
 */
package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import junit.framework.TestCase;

public class ButtonRTest extends TestCase {
  
    public void testShowAndPack() {
        Frame frm = new Frame("Test");
        Button btnClose = new Button("MyClose");
        btnClose.setBounds(10, 10, 80, 22);
        frm.add(btnClose);
        frm.setVisible(true);
        frm.pack();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        frm.dispose();
    }
    
    // Regression test for HARMONY-2305
    // Currently fails on Linux version of RI
    public void testHarmony2305() throws Exception {
        final Frame f = new Frame();
        final Button b1 = new Button("B1"); //$NON-NLS-1$
        final Button b2 = new Button("B2"); //$NON-NLS-1$
        final AL l1 = new AL();
        final AL l2 = new AL();
        final Robot r = new Robot();

        try {
            b1.addActionListener(l1);
            b2.addActionListener(l2);
            f.add(BorderLayout.WEST, b1);
            f.add(BorderLayout.EAST, b2);
            f.setSize(100, 100);
            f.setVisible(true);

            r.setAutoWaitForIdle(true);
            r.setAutoDelay(500);
            r.mouseMove(b1.getLocation().x + 3, b1.getLocation().y + 3);
            r.mousePress(InputEvent.BUTTON1_MASK);
            r.mouseRelease(InputEvent.BUTTON1_MASK);
            assertNotNull(l1.e);
            assertEquals("B1", l1.e.getActionCommand()); //$NON-NLS-1$

            assertNull(l2.e);
            r.keyPress(KeyEvent.VK_SPACE);
            r.mouseMove(b2.getLocation().x + 3, b2.getLocation().y + 3);
            r.mousePress(InputEvent.BUTTON1_MASK);
            r.mouseRelease(InputEvent.BUTTON1_MASK);
            assertNull(l2.e);
        } finally {
            f.dispose();
        }
    }
    
    // Regression test for HARMONY-3701
    public void testHarmony3701() throws Exception {
        setName("testHarmony3701"); //$NON-NLS-1$
        testHarmony2305();
        testHarmony2305();
    }

    public void testDeadLoop4887() {
        final int count[] = new int[1];
        Button b = new Button() {
            public void paint(Graphics g) {
                count[0]++;
                setEnabled(true);
                setLabel("button1");
            }
        };
        
        Tools.checkDeadLoop(b, count);
    }
    
    static class AL implements ActionListener {
        ActionEvent e;

        public void actionPerformed(final ActionEvent e) {
            this.e = e;
        }
    }
    
}
