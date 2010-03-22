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
package org.apache.harmony.awt.wtk.linux;

import java.awt.AWTError;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;

import org.apache.harmony.awt.gl.linux.XGraphicsConfiguration;
import org.apache.harmony.awt.gl.linux.XGraphicsDevice;
import org.apache.harmony.awt.gl.linux.XVolatileImage;
import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.wtk.NativeRobot;

import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.Int8Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.X11Defs;

/**
 * XTestRobot
 */
public class XTestRobot implements NativeRobot {
    private static final int[] BTNS = {
                                           X11Defs.Button1,
                                           X11Defs.Button2,
                                           X11Defs.Button3,
                                           X11Defs.Button4,
                                           X11Defs.Button5,
                                           };
    private final long AllPlanes;
    private final int screen;
    private final X11 x11 = X11.getInstance();
    private final NativeBridge nb = NativeBridge.getInstance();
    private final long dpy;
    private final XGraphicsConfiguration gc;
    public XTestRobot(long display, XGraphicsDevice scrn) {
        // get screen number
        // from GraphicsDevice
        screen = scrn.getScreen();
        gc = (XGraphicsConfiguration) scrn.getDefaultConfiguration();
        dpy = display;
        AllPlanes = x11.XAllPlanes();
        queryExtension();
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#createScreenCapture(java.awt.Rectangle)
     */
    public BufferedImage createScreenCapture(Rectangle screenRect) {
        int w = screenRect.width;
        int h = screenRect.height;
        int fmt = X11Defs.ZPixmap;
        long root = x11.XRootWindow(dpy, screen);
        X11.XImage ximg = createXImage(w, h);
        if (ximg == null) {
            return null;
        }

        ximg = x11.XGetSubImage(dpy, root, screenRect.x, screenRect.y, w, h,
                         AllPlanes, fmt, ximg, 0, 0);
        BufferedImage bufImg = XVolatileImage.biFromXImage(ximg, gc);
        x11.XDestroyImage(ximg);

        return bufImg;
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#getPixel(int, int)
     */
    public Color getPixel(int x, int y) {
        Rectangle rect = new Rectangle(x, y, 1, 1);
        X11.XImage ximg = getScreenImage(rect);
        long pixel = ximg.get_f().get_pixel(ximg, 0, 0);
        // this only works if window containing (x, y)
        // point uses default color map:
        // ideally we should find window at (x,y)
        // and take its own color map
        long cmap = x11.XDefaultColormap(dpy, screen);
        return getColor(cmap, pixel);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#keyEvent(int, boolean)
     */
    public void keyEvent(int keycode, boolean press) {
        int xKeySym = KeyCodeTranslator.VK2XK(keycode);
        if (xKeySym == X11Defs.NoSymbol) {
            // awt.11=Invalid key code
            throw new IllegalArgumentException(Messages.getString("awt.11")); //$NON-NLS-1$
        }
        int xKeyCode = x11.XKeysymToKeycode(dpy, xKeySym);
        x11.XTestFakeKeyEvent(dpy, xKeyCode, getBool(press),
                                               X11Defs.CurrentTime);
        x11.XFlush(dpy);

    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#mouseButton(int, boolean)
     */
    public void mouseButton(int buttons, boolean press) {
        int isPress = (getBool(press));
        if ((buttons & InputEvent.BUTTON1_MASK) != 0) {
            mouseButton(BTNS[0], isPress);
        }
        if ((buttons & InputEvent.BUTTON2_MASK) != 0) {
            mouseButton(BTNS[1], isPress);
        }
        if ((buttons & InputEvent.BUTTON3_MASK) != 0) {
            mouseButton(BTNS[2], isPress);
        }
        x11.XFlush(dpy);

    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#mouseMove(int, int)
     */
    public void mouseMove(int x, int y) {
        x11.XTestFakeMotionEvent(dpy, screen, x, y, X11Defs.CurrentTime);
        x11.XFlush(dpy);

    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#mouseWheel(int)
     */
    public void mouseWheel(int wheelAmt) {
        int btn = (BTNS[wheelAmt > 0 ? 4 : 3]);
        mouseButton(btn, X11Defs.ButtonPress);
        mouseButton(btn, X11Defs.ButtonRelease);
        x11.XFlush(dpy);

    }


    private void queryExtension() {
        Int32Pointer dummyPtr = nb.createInt32Pointer(1, false);
        Int32Pointer majorPtr = nb.createInt32Pointer(1, false);
        Int32Pointer minorPtr = nb.createInt32Pointer(1, false);
        int res = 0;
        res = x11.XTestQueryExtension(dpy, dummyPtr, dummyPtr,
                                      majorPtr, minorPtr);
        if (res != X11Defs.True) {
            // awt.12=XTest is not supported by your X server\!
            throw new AWTError(Messages.getString("awt.12")); //$NON-NLS-1$
        }
    }

    private void mouseButton(int btn, int isPress) {
        int res = x11.XTestFakeButtonEvent(dpy, btn, isPress,
                                           X11Defs.CurrentTime);
    }

    private X11.XImage getScreenImage(Rectangle r) {
        long root = x11.XRootWindow(dpy, screen);
        long ptr = x11.XGetImage(dpy, root, r.x, r.y, r.width, r.height,
                                 AllPlanes, X11Defs.ZPixmap);
        return x11.createXImage(ptr);
    }

    private Color getColor(long cmap, long pixel) {
        X11.XColor xcolor = x11.createXColor(false);
        xcolor.set_pixel(pixel);
        x11.XQueryColor(dpy, cmap, xcolor);
        int r = xcolor.get_red() & 0xFF;
        int g = xcolor.get_green() & 0xFF;
        int b = xcolor.get_blue() & 0xFF;
        return new Color(r, g, b);
    }

    private int getBool(boolean b) {
        return (b ? X11Defs.True : X11Defs.False);
    }

    private X11.XImage createXImage(int w, int h) {

        X11.Visual vis = x11.createVisual(x11.XDefaultVisual(dpy, screen));

        int size = w * h;
        int depth = x11.XDefaultDepth(dpy, screen);
        // MUST use native(not Java!) memory here:
        Int8Pointer dataPtr = nb.createInt8Pointer(size * 4, true);
        X11.XImage ximg = x11.XCreateImage(dpy, vis, depth, X11Defs.ZPixmap, 0,
                                           dataPtr, w, h, 32, 0);

        int order = X11Defs.LSBFirst;
        ximg.set_byte_order(order);
        return ximg;
    }

}
