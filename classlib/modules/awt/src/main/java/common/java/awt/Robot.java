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
 * @author Pavel Dolgov, Dmitry A. Durnev
 */
package java.awt;

import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.wtk.NativeRobot;


public class Robot {

    private int autoDelay;
    private boolean autoWaitForIdle;

    private final NativeRobot nativeRobot;

    public Robot() throws AWTException {
        this(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
    }

    public Robot(GraphicsDevice screen) throws AWTException {
        Toolkit.checkHeadless();
        if ((screen == null)
                || (screen.getType() != GraphicsDevice.TYPE_RASTER_SCREEN)) {
            // awt.129=Not a screen device
            throw new IllegalArgumentException(Messages.getString("awt.129")); //$NON-NLS-1$
        }
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new AWTPermission("createRobot")); //$NON-NLS-1$
        }
        // create(or get) native robot instance
        // for the specified screen
        Toolkit tk = Toolkit.getDefaultToolkit();
        nativeRobot = tk.getWTK().getNativeRobot(screen);
    }

    @Override
    public String toString() {
        /* The format is based on 1.5 release behavior 
         * which can be revealed by the following code:
         * System.out.println(new Robot());
         */

        return getClass().getName() + "[" + "autoDelay = " + autoDelay + //$NON-NLS-1$ //$NON-NLS-2$
        ", autoWaitForIdle = " + autoWaitForIdle + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public BufferedImage createScreenCapture(Rectangle screenRect) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new AWTPermission("readDisplayPixels")); //$NON-NLS-1$
        }
        if (screenRect.isEmpty()) {
            // awt.13D=Rectangle width and height must be > 0
            throw new IllegalArgumentException(Messages.getString("awt.13D")); //$NON-NLS-1$
        }

        return nativeRobot.createScreenCapture(screenRect);
    }

    public void delay(int ms) {
        checkDelay(ms);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    public int getAutoDelay() {
        return autoDelay;
    }

    public Color getPixelColor(int x, int y) {
        return nativeRobot.getPixel(x, y);
    }

    public boolean isAutoWaitForIdle() {
        return autoWaitForIdle;
    }

    public void keyPress(int keycode) {
        nativeRobot.keyEvent(keycode, true);
        doWait();
    }

    public void keyRelease(int keycode) {
        nativeRobot.keyEvent(keycode, false);
        doWait();
    }

    public void mouseMove(int x, int y) {
        nativeRobot.mouseMove(x, y);
        doWait();
    }

    public void mousePress(int buttons) {
        checkButtons(buttons);
        nativeRobot.mouseButton(buttons, true);
        doWait();
    }

    public void mouseRelease(int buttons) {
        checkButtons(buttons);
        nativeRobot.mouseButton(buttons, false);
        doWait();
    }

    public void mouseWheel(int wheelAmt) {
        nativeRobot.mouseWheel(wheelAmt);
        doWait();
    }

    public void setAutoDelay(int ms) {
        checkDelay(ms);
        autoDelay = ms;
    }

    public void setAutoWaitForIdle(boolean isOn) {
        autoWaitForIdle = isOn;
    }

    public void waitForIdle() {
        if (EventQueue.isDispatchThread()) {
            // awt.13E=Cannot call method from the event dispatcher thread
            throw new IllegalThreadStateException(Messages.getString("awt.13E")); //$NON-NLS-1$
        }
        try {
            EventQueue.invokeAndWait(new Runnable() {

                public void run() {
                    // just do nothing
                }

            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void checkDelay(int ms) {
        if ((ms < 0) || (ms > 60000)) {
            // awt.13F=Delay must be to 0 to 60,000ms
            throw new IllegalArgumentException(Messages.getString("awt.13F")); //$NON-NLS-1$
        }
    }

    private void checkButtons(int buttons) {
        int mask = (InputEvent.BUTTON1_MASK | InputEvent.BUTTON2_MASK |
                    InputEvent.BUTTON3_MASK);
        if ((buttons & mask) != buttons) {
            // awt.140=Invalid combination of button flags
            throw new IllegalArgumentException(Messages.getString("awt.140")); //$NON-NLS-1$
        }
    }

    private void doWait() {
        // first wait for idle if necessary:
        if (isAutoWaitForIdle()) {
            waitForIdle();
        }
        // now sleep if autoDelay is > 0
        int delay = getAutoDelay();
        if (delay > 0) {
            delay(delay);
        }
    }
}

