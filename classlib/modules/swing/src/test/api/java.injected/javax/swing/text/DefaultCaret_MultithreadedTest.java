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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.text;

import javax.swing.BasicSwingTestCase;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWaitTestCase;

public class DefaultCaret_MultithreadedTest extends BasicSwingTestCase {
    AbstractDocument ad;

    boolean bWasException = false;

    DefaultCaret dc = null;

    JFrame jf = null;

    JTextArea jta = null;

    String s = null;

    int newDot;

    public DefaultCaret_MultithreadedTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        jta = new JTextArea("JTextArea for DefaultCaret Testing\n***\n*%%%**");
        dc = new DefaultCaret();
        jf = new JFrame();
        bWasException = false;
        s = null;
        ad = (AbstractDocument) jta.getDocument();
        jf.getContentPane().add(jta);
        jta.getCaret().deinstall(jta);
        jta.setCaret(dc);
        jf.setLocation(100, 100);
        jf.setSize(350, 200);
        jf.setVisible(true);
    }

    @Override
    protected void tearDown() throws Exception {
        jf.dispose();
        super.tearDown();
    }

    Runnable setDot = new Runnable() {
        public void run() {
            dc.setDot(newDot);
        }
    };

    public void testAsyncUpdate() throws Exception {
        waitForIdle();
        SwingWaitTestCase.isRealized(jf);
        ad.writeLock();
        try {
            ad.insertString(0, "insert", null);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        ad.writeUnlock();
        waitForIdle();
        assertFalse("Unexpected exception: " + s, bWasException);
        assertEquals(0, dc.getDot());
        newDot = 4;
        SwingUtilities.invokeAndWait(setDot);
        waitForIdle();
        ad.writeLock();
        try {
            ad.remove(0, 3);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        ad.writeUnlock();
        waitForIdle();
        assertEquals(4, dc.getDot());
        //dc.setAsynchronousMovement(true); //1.4.2
        if (isHarmony()) {
            dc.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        } else {
            dc.setAsynchronousMovement(true);
        }
        newDot = 0;
        SwingUtilities.invokeAndWait(setDot);
        waitForIdle();
        ad.writeLock();
        try {
            ad.insertString(0, "insert", null);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        ad.writeUnlock();
        assertEquals(6, dc.getDot());
        SwingWaitTestCase.isRealized(jf);
        assertFalse("Unexpected exception: " + s, bWasException);
        assertEquals(6, dc.getDot());
        newDot = 4;
        SwingUtilities.invokeAndWait(setDot);
        waitForIdle();
        ad.writeLock();
        try {
            ad.remove(0, 3);
        } catch (BadLocationException e) {
            bWasException = true;
            s = e.getMessage();
        }
        ad.writeUnlock();
        waitForIdle();
        assertFalse("Unexpected exception: " + s, bWasException);
        assertEquals(1, dc.getDot());
    }
}