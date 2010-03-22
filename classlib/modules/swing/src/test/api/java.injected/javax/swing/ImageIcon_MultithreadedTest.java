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
 * @author Alexander T. Simbirtsev
 * Created on 05.04.2005

 */
package javax.swing;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.net.URL;
import junit.framework.TestCase;

public class ImageIcon_MultithreadedTest extends TestCase {
    class MyImageObserver implements ImageObserver {
        public boolean updated = false;

        public boolean installed = false;

        public Object lock = new Object();

        public void reset() {
            updated = false;
        }

        public boolean imageUpdate(final Image img, final int infoflags, final int x,
                final int y, final int width, final int height) {
            updated = true;
            synchronized (lock) {
                lock.notifyAll();
            }
            return true;
        }
    };

    protected ImageIcon icon = null;

    private JFrame frame = null;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(ImageIcon_MultithreadedTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        super.tearDown();
    }

    public void testPaintIcon() {
        //      JFrame frame = new JFrame();
        //      String fileName1 = "images/animated.gif";
        //      URL url1 = getClass().getResource(fileName1);
        //      icon = new ImageIcon(url1);
        //      JButton button = new JButton(icon);
        //      frame.getContentPane().add(button);
        //      frame.pack();
        //      frame.show();
        //      InternalTests.isRealized(frame);
        //      MyImageObserver observer1 = new MyImageObserver();
        //      icon.setImageObserver(observer1);
        //      int timeToWait = 15000;
        //      while(timeToWait > 0) {
        //          try {
        //                Thread.sleep(10);
        //                timeToWait -= 10;
        //            } catch (InterruptedException e) {
        //                e.printStackTrace();
        //            }
        //      }
    }

    @SuppressWarnings("deprecation")
    public void testSetImageObserver() {
        final MyImageObserver observer1 = new MyImageObserver();
        String fileName1 = "images/animated.gif";
        final URL url = getClass().getResource(fileName1);
        assertTrue("file is found", url != null);
        assertFalse("observer is not notified", observer1.updated);
        frame = new JFrame();
        ImageIcon icon1 = new ImageIcon(url);
        JButton button = new JButton(icon1);
        button.setPreferredSize(new Dimension(30, 30));
        icon1.setImageObserver(observer1);
        synchronized (observer1.lock) {
            observer1.installed = true;
            observer1.lock.notifyAll();
        }
        frame.getContentPane().add(button);
        frame.pack();
        frame.show();
        SwingWaitTestCase.isRealized(frame);
        waitTillObserverNotified(observer1, 3000);
        assertTrue("observer is notified", observer1.updated);
        observer1.reset();
        waitTillObserverNotified(observer1, 3000);
        assertTrue("observer is notified", observer1.updated);
        observer1.reset();
        waitTillObserverNotified(observer1, 3000);
        assertTrue("observer is notified", observer1.updated);
        observer1.reset();
    }

    /**
     * waits till observer change his state to 'installed' and then to 'updated'
     * quits waiting when maxWaitTime is over
     */
    private void waitTillObserverNotified(final MyImageObserver observer, final int maxWaitTime) {
        int timeRemains = maxWaitTime;
        while (timeRemains > 0 && !observer.installed) {
            try {
                synchronized (observer.lock) {
                    observer.lock.wait(10);
                    timeRemains -= 10;
                }
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }
        assertTrue("observer did manage to be installed", observer.installed);
        timeRemains = maxWaitTime;
        while (timeRemains > 0 && !observer.updated) {
            try {
                synchronized (observer.lock) {
                    observer.lock.wait(10);
                }
                timeRemains -= 10;
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }
    }
}
