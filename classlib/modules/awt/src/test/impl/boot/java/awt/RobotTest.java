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

import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.math.BigInteger;
import java.security.Permission;

import junit.framework.TestCase;

/**
 * RobotTest
 */
public class RobotTest extends TestCase {

    Robot robot;
    Frame f;
    Throwable throwable;
    boolean exceptionCaught;
    MouseEvent mouseEvent;
    Button b;
    Object lock;
    KeyEvent keyEvent;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RobotTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setSecurityManager(null);
        robot = new Robot();
        f = new Frame("Robot test");
        throwable = null;
        exceptionCaught = false;
        mouseEvent = null;
        keyEvent = null;
        lock = new Object();
        b = new Button();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (f != null) {
            f.dispose();
        }
    }

    @SuppressWarnings("deprecation")
    private void waitForButton() {
        int timeout = 16, time = 0;
        int nAttempts = 10;        
        f.add(b);
        f.setSize(100, 100);
        int x = 50;
        int y = 50;
        Color bkColor = robot.getPixelColor(x, y);        
        b.setBackground(getNextColor(bkColor));
        f.show();

        for (int i = 0; i < nAttempts; i++) {
            robot.delay(timeout);
            time += timeout;
            Point fLoc = f.getLocation();
            x = fLoc.x + f.getWidth() / 2;
            y = fLoc.y + f.getHeight() / 2;
            if (robot.getPixelColor(x, y).equals(b.getBackground())) {
                break;
            }
            timeout <<= 1;
        }

        assertEquals("button is shown", b.getBackground(), robot.getPixelColor(x, y));
    }

    private int inv(int val) {
        return ~val & 0xFF;
    }
    
    private Color getNextColor(Color bkColor) {
        Color color = bkColor;
        ColorModel cm = f.getGraphicsConfiguration().getColorModel();
        Object pixel = null;
        while (color.equals(bkColor)) {

            color = new Color(inv(color.getRed()), inv(color.getGreen()),
                    inv(color.getBlue()));

            pixel = cm.getDataElements(color.getRGB(), pixel);
            color = new Color(cm.getRGB(pixel));
        }
        return color;
    }

    public final void testToString() {
        String str = robot.toString();
        assertNotNull(str);
        assertTrue(str.startsWith(robot.getClass().getName()));
        assertTrue(str.indexOf("autoDelay = 0,") > 0);
        assertTrue(str.indexOf("autoWaitForIdle = false") > 0);
    }

    /*
     * Class under test for void Robot()
     */
    public final void testRobot() {
        Runnable cons = new Runnable() {
            public void run() {
                try {
                    robot = new Robot();
                } catch (AWTException e) {
                    e.printStackTrace();
                }
            }
        };
        assertTrue(isDenied(new MySecurityManager(), cons));
        assertFalse(isDenied(null, cons));
    }

    private boolean isDenied(SecurityManager sm, Runnable code) {
        exceptionCaught = false;
        System.setSecurityManager(sm);
        try {
            code.run();
        } catch (SecurityException se) {
            exceptionCaught = true;
        }
        return exceptionCaught;
    }

    /*
     * Class under test for void Robot(java.awt.GraphicsDevice)
     */
    public final void testRobotGraphicsDevice() throws AWTException {
        try {
            robot = new Robot(new PrintDevice());
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        } catch (AWTException ae) {
            exceptionCaught = false;
        }
        assertTrue(exceptionCaught);
        exceptionCaught = false;
        assertNotNull(f);
        try {
            robot = new Robot(f.getGraphicsConfiguration().getDevice());
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        } catch (AWTException ae) {
            exceptionCaught = true;
        }
        assertFalse(exceptionCaught);
        
        // Regression test for HARMONY-2442
        try {
            new Robot(null);
            fail("IllegalArgumentException was not thrown"); //$NON-NLS-1$
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public final void testCreateScreenCapture() {
        Runnable capture = new Runnable () {
            public void run() {
                robot.createScreenCapture(new Rectangle(10, 10, 100, 200));
            }
        };
        assertTrue(isDenied(new MySecurityManager(), capture));
        assertFalse(isDenied(null, capture));

        BufferedImage img = null;
        Rectangle rect = new Rectangle();
        try {
            img = robot.createScreenCapture(rect);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
        assertNull(img);
        exceptionCaught = false;
        img = null;
        rect.width = 100;
        try {
            img = robot.createScreenCapture(rect);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
        assertNull(img);
        exceptionCaught = false;
        img = null;
        waitForButton();
        rect.setBounds(f.getBounds());

        try {
            img = robot.createScreenCapture(rect);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertFalse(exceptionCaught);
        assertNotNull(img);
        int rgb = img.getRGB(30, 50);
        assertEquals(rect.width, img.getWidth(null));
        assertEquals(rect.height, img.getHeight(null));

        assertEquals("RGB", b.getBackground().getRGB(), rgb);
    }

    public final void testDelay() {
        int delay = 2000;
        long startTime = System.currentTimeMillis();
        robot.delay(delay);
        long dTime = System.currentTimeMillis() - startTime;
        assertTrue(dTime - delay < delay / 10);
        try {
            robot.delay(delay = 60001);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
        exceptionCaught = false;
        try {
            robot.delay(delay = -1);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

    public final void testGetAutoDelay() {
        assertEquals(0, robot.getAutoDelay());
    }

    public final void testGetPixelColor() {
        b.setBackground(Color.BLUE);
        waitForButton();
        int centerX = b.getX() + b.getWidth() / 2;
        int centerY = b.getY() + b.getHeight() / 2;
        assertEquals(b.getBackground(), robot.getPixelColor(centerX, centerY));
    }

    public final void testIsAutoWaitForIdle() {
        assertFalse(robot.isAutoWaitForIdle());
    }

    public final void testKeyPress() {
        try {
            robot.keyPress(-1000);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);

        waitForButton();
        waitFocus(5000);
        b.addKeyListener(new KeyAdapter() {
           @Override
        public void keyPressed(KeyEvent ke) {
               keyEvent = ke;
           }
        });
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(1000);
        int key = KeyEvent.VK_SPACE;
        robot.delay(500);
        robot.keyPress(key);
        robot.delay(1000);
        assertNotNull("key event was dispatched", keyEvent);
        assertEquals("key pressed", KeyEvent.KEY_PRESSED, keyEvent.getID());
        assertEquals("proper key is pressed", key, keyEvent.getKeyCode());
        robot.keyRelease(key);
    }

    private void waitFocus(int delay) {
        int timeout = 250;
        int time = 0;
        while (!b.isFocusOwner() && time < delay) {
            robot.delay(timeout);
            b.requestFocusInWindow();
            time += timeout;
        }
        assertTrue("button has focus", b.isFocusOwner());
    }

    public final void testKeyRelease() {
        try {
            robot.keyRelease(666);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
        waitForButton();
        waitFocus(5000);
        b.addKeyListener(new KeyAdapter() {
           @Override
        public void keyReleased(KeyEvent ke) {
               keyEvent = ke;
           }
        });
        robot.setAutoDelay(100);
        int key = KeyEvent.VK_SPACE;
        robot.keyPress(key);
        robot.keyRelease(key);
        robot.delay(1000);
        assertNotNull(keyEvent);
        assertEquals(KeyEvent.KEY_RELEASED, keyEvent.getID());
        assertEquals(key, keyEvent.getKeyCode());
    }

    private int sgn(int n) {
        return BigInteger.valueOf(n).signum();
    }

    private void movePointer(Point p1, Point p2) {
        p2.translate(b.getWidth() / 2, b.getHeight() / 2);
        float k = (p2.y - p1.y) / (float) (p2.x - p1.x);
        float b = p2.y - k * p2.x;
        robot.setAutoDelay(20);
        int dx = 3 * sgn(p2.x - p1.x);
        for (int x = p1.x; Math.abs(x - p2.x) >= 5; x+=dx) {
            robot.mouseMove(x, (int) (k * x + b));
        }
    }

    public final void testMouseMove() {
        Point p = new Point(100, 200);
        robot.setAutoDelay(0);
        robot.mouseMove(p.x, p.y);
        assertEquals(p, MouseInfo.getPointerInfo().getLocation());
    }

    public final void testMousePress() {
        try {
            robot.mousePress(-1);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);

        waitForButton();
        Point p1 = MouseInfo.getPointerInfo().getLocation();
        Point p2 = b.getLocationOnScreen();
        movePointer(p1, p2);
        b.addMouseListener(new MouseAdapter() {
           @Override
        public void mousePressed(MouseEvent me) {
               mouseEvent = me;
           }
        });
        robot.setAutoDelay(0);
        int mask = InputEvent.BUTTON1_MASK;
        robot.mousePress(mask);
        robot.delay(5000);
        assertNotNull(mouseEvent);
        assertEquals(MouseEvent.MOUSE_PRESSED, mouseEvent.getID());
        assertEquals(MouseEvent.BUTTON1, mouseEvent.getButton());
        robot.mouseRelease(mask);

    }

    public final void testMouseRelease() {
        try {
            robot.mouseRelease(-1);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue("exception(IAE) is thrown", exceptionCaught);

        waitForButton();
        Point p1 = MouseInfo.getPointerInfo().getLocation();
        Point p2 = b.getLocationOnScreen();
        int mask = InputEvent.BUTTON3_MASK;
        movePointer(p1, p2);
        b.addMouseListener(new MouseAdapter() {
           @Override
        public void mouseReleased(MouseEvent me) {
               mouseEvent = me;
           }
        });
        robot.setAutoDelay(0);
        robot.mousePress(mask);
        robot.mouseRelease(mask);
        robot.delay(5000);
        assertNotNull("event is not null", mouseEvent);
        assertEquals("mouse released", MouseEvent.MOUSE_RELEASED,
                     mouseEvent.getID());
        assertEquals("proper button is released", MouseEvent.BUTTON3,
                     mouseEvent.getButton());
    }

    public final void testMouseWheel() {
        waitForButton();
        Point p1 = MouseInfo.getPointerInfo().getLocation();
        Point p2 = b.getLocationOnScreen();
        int scroll = -15;
        movePointer(p1, p2);
        f.toFront();
        robot.delay(100);
        b.addMouseWheelListener(new MouseWheelListener() {

            public void mouseWheelMoved(MouseWheelEvent mwe) {
                mouseEvent = mwe;
            }
        });
        robot.setAutoDelay(100);
        robot.mouseWheel(scroll);
        robot.delay(1000);
        assertNotNull(mouseEvent);
        assertTrue(mouseEvent instanceof MouseWheelEvent);
        assertEquals(MouseEvent.MOUSE_WHEEL, mouseEvent.getID());
        MouseWheelEvent mwe = (MouseWheelEvent) mouseEvent;
        assertEquals(sgn(scroll), sgn(mwe.getWheelRotation()));
    }

    public final void testSetAutoDelay() {
        int delay = 2000;
        robot.setAutoDelay(delay);
        assertEquals(delay, robot.getAutoDelay());
        try {
            robot.setAutoDelay(-666);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
        assertEquals(delay, robot.getAutoDelay());
        exceptionCaught = false;
        try {
            robot.setAutoDelay(66666);
        } catch (IllegalArgumentException iae) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
        assertEquals(delay, robot.getAutoDelay());


    }

    public final void testSetAutoWaitForIdle() {
        robot.setAutoWaitForIdle(true);
        assertTrue(robot.isAutoWaitForIdle());
        robot.setAutoWaitForIdle(false);
        assertFalse(robot.isAutoWaitForIdle());
    }

    public final void testWaitForIdle() throws Throwable {

        // test exceptional case:
        EventQueue.invokeAndWait(new Runnable() {

            public void run() {
                boolean itseCaught = false;
                try {
                    robot.waitForIdle();
                } catch (IllegalThreadStateException itse) {
                    itseCaught = true;
                }
                try {
                    assertTrue(itseCaught);
                } catch (Throwable thr) {
                    throwable = thr;
                }
            }

        });
        if (throwable != null) {
            throw throwable;
        }
    }

}

/**
 *
 * A modified default security manager which allows to set current
 * security manager and denies Robot operations
 * without changing the security policy
 *
 */
class MySecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission p) {
        if (p.equals(new RuntimePermission("setSecurityManager")) ||
            p.equals(new RuntimePermission("createSecurityManager"))) {
            return;
        }
        if (p.equals(new AWTPermission("createRobot")) ||
            p.equals(new AWTPermission("readDisplayPixels"))) {
            throw new SecurityException("All Robot operations are denied");
        }
        super.checkPermission(p);
    }
}

class PrintDevice extends GraphicsDevice {

    @Override
    public int getType() {
        return GraphicsDevice.TYPE_PRINTER;
    }

    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        return null;
    }

    @Override
    public GraphicsConfiguration[] getConfigurations() {
        return null;
    }

    @Override
    public String getIDstring() {
        return null;
    }

}