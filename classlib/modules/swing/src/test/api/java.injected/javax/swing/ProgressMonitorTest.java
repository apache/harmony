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
 * @author Dennis Ushakov
 */
package javax.swing;

import java.awt.event.WindowEvent;

public class ProgressMonitorTest extends BasicSwingTestCase {
    private ProgressMonitor progressMonitor;

    private JFrame window;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        window = new JFrame();
    }

    @Override
    public void tearDown() throws Exception {
        progressMonitor = null;
        window = null;
        super.tearDown();
    }

    public void testProgressMonitor() {
        progressMonitor = new ProgressMonitor(window, "message test", "note test", 0, 100);
        assertEquals(progressMonitor.getMillisToDecideToPopup(), 500);
        assertEquals(progressMonitor.getMillisToPopup(), 2000);
    }

    public void testSetProgress() throws Exception {
        progressMonitor = new ProgressMonitor(window, "message test", "note test", 0, 100);
        assertEquals(0, window.getOwnedWindows().length);
        progressMonitor.setProgress(1);
        Thread.sleep(500 + 100);
        progressMonitor.setProgress(2);
        assertEquals(1, window.getOwnedWindows().length);
        JProgressBar pb = getProgressBar();
        assertEquals(2, pb.getValue());
        progressMonitor.close();
        progressMonitor = new ProgressMonitor(window, "message test", "note test", -100, 100);
        assertEquals(1, window.getOwnedWindows().length);
        progressMonitor.setProgress(1);
        Thread.sleep(500 + 100);
        progressMonitor.setProgress(2);
        assertEquals(1, window.getOwnedWindows().length);
        progressMonitor = new ProgressMonitor(window, "message test", "note test", 0, 300);
        progressMonitor.setProgress(1);
        Thread.sleep(500 + 100);
        progressMonitor.setProgress(2);
        assertEquals(1, window.getOwnedWindows().length);
    }

    public void testGetSetMaximum() throws Exception {
        progressMonitor = new ProgressMonitor(window, "message test", "note test", 0, 300);
        progressMonitor.setProgress(1);
        Thread.sleep(500 + 100);
        progressMonitor.setProgress(20);
        assertEquals(300, getProgressBar().getMaximum());
        progressMonitor.setMaximum(500);
        if (isHarmony()) {
            assertEquals(500, getProgressBar().getMaximum());
        }
        assertEquals(500, progressMonitor.getMaximum());
    }

    public void testGetSetMinimum() throws Exception {
        progressMonitor = new ProgressMonitor(window, "message test", "note test", 0, 300);
        progressMonitor.setProgress(1);
        Thread.sleep(500 + 100);
        progressMonitor.setProgress(20);
        assertEquals(0, getProgressBar().getMinimum());
        progressMonitor.setMinimum(-100);
        if (isHarmony()) {
            assertEquals(-100, getProgressBar().getMinimum());
        }
        assertEquals(-100, progressMonitor.getMinimum());
    }

    public void testIsCancelled() throws Exception {
        progressMonitor = new ProgressMonitor(window, "message test", "note test", 0, 100);
        progressMonitor.setProgress(1);
        Thread.sleep(500 + 100);
        progressMonitor.setProgress(20);
        JDialog dialog = (JDialog) window.getOwnedWindows()[0];
        JOptionPane comp = (JOptionPane) dialog.getContentPane().getComponent(0);
        JPanel bottomPanel = (JPanel) comp.getComponent(1);
        JButton cancelButton = (JButton) bottomPanel.getComponent(0);
        cancelButton.doClick();
        assertTrue(progressMonitor.isCanceled());
        progressMonitor = new ProgressMonitor(window, "message test", "note test", 0, 100);
        progressMonitor.setProgress(1);
        Thread.sleep(500 + 100);
        progressMonitor.setProgress(20);
        dialog = (JDialog) window.getOwnedWindows()[1];
        dialog.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
        assertTrue(progressMonitor.isCanceled());
        progressMonitor.setProgress(98);
        assertEquals(2, window.getOwnedWindows().length);
    }

    public void testClose() throws Exception {
        progressMonitor = new ProgressMonitor(window, "message test", "note test", 0, 100);
        progressMonitor.setProgress(1);
        Thread.sleep(500 + 100);
        progressMonitor.setProgress(20);
        progressMonitor.close();
        assertEquals(1, window.getOwnedWindows().length);
        assertFalse(window.getOwnedWindows()[0].isVisible());
        assertFalse(window.getOwnedWindows()[0].isDisplayable());
        progressMonitor.setProgress(25);
        assertEquals(2, window.getOwnedWindows().length);
        assertTrue(window.getOwnedWindows()[1].isVisible());
    }

    private JProgressBar getProgressBar() {
        JDialog dialog = (JDialog) window.getOwnedWindows()[0];
        JOptionPane comp = (JOptionPane) dialog.getContentPane().getComponent(0);
        JPanel topPanel = (JPanel) comp.getComponent(0);
        JPanel panel = (JPanel) topPanel.getComponent(0);
        JPanel panel2 = (JPanel) panel.getComponent(1);
        JProgressBar pb = (JProgressBar) panel2.getComponent(2);
        return pb;
    }
}
