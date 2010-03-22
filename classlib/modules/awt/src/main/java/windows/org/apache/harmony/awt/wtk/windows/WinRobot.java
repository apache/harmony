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
package org.apache.harmony.awt.wtk.windows;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import org.apache.harmony.awt.internal.nls.Messages;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.windows.Win32;
import org.apache.harmony.awt.nativebridge.windows.WindowsDefs;
import org.apache.harmony.awt.wtk.NativeRobot;


/**
 * WinRobot
 */
public class WinRobot implements NativeRobot {
    private static final int[] upFlags = {
                      WindowsDefs.MOUSEEVENTF_LEFTUP,
                      WindowsDefs.MOUSEEVENTF_MIDDLEUP,
                      WindowsDefs.MOUSEEVENTF_RIGHTUP
                      };
    private static final int[] dnFlags = {
                      WindowsDefs.MOUSEEVENTF_LEFTDOWN,
                      WindowsDefs.MOUSEEVENTF_MIDDLEDOWN,
                      WindowsDefs.MOUSEEVENTF_RIGHTDOWN
                      };

    private final NativeBridge nb = NativeBridge.getInstance();
    private final Win32 win32 = Win32.getInstance();

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#createScreenCapture(java.awt.Rectangle)
     */
    public BufferedImage createScreenCapture(Rectangle screenRect) {

        int w = screenRect.width;
        int h = screenRect.height;
        int size = w * h;
        long compBMP = createScreenBMP(screenRect);
        if (compBMP == 0l) {
            return null;
        }

        Win32.BITMAPINFO bmi = createAndFillBMI(w, h);

        // now get device independent bitmap bits from
        // compBMP and use them to create BufferedImage
        Int32Pointer pData = nb.createInt32Pointer(size, false);
        win32.DeleteObject(compBMP);
        long screenDC = win32.GetDC(0l);
        int nLines = win32.GetDIBits(screenDC, compBMP, 0, h, pData,
                        bmi, WindowsDefs.DIB_RGB_COLORS);
        win32.ReleaseDC(0, screenDC);
        
        if (nLines != h) {
            return null;
        }
        BufferedImage bufImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] data = new int[size];
        pData.get(data, 0, data.length);
        bufImg.setRGB(0, 0, w, h, data, 0, w);
        return bufImg;
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#getPixel(int, int)
     */
    public Color getPixel(int x, int y) {
        long screenDC = win32.GetDC(0l);
        long bgr = win32.GetPixel(screenDC, x, y);
        win32.ReleaseDC(0, screenDC);
        return fromBGR(bgr);
    }

    private Color fromBGR(long bgr) {
        return new Color((int) bgr & 0xFF,
                         (int)(bgr >> 8 & 0xFF),
                         (int)(bgr >> 16 & 0xFF));
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#keyEvent(int, boolean)
     */
    public void keyEvent(int keycode, boolean press) {
        win32.keybd_event(translateKeyCode(keycode), (byte) 0,
                          (press ? 0 : WindowsDefs.KEYEVENTF_KEYUP), 0l);
    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#mouseButton(int, boolean)
     */
    public void mouseButton(int buttons, boolean press) {
        int flags = 0;
        int[] allFlags = (press ? dnFlags : upFlags);

        if ((buttons & InputEvent.BUTTON1_MASK) != 0) {
            flags |= allFlags[0];
        }
        if ((buttons & InputEvent.BUTTON2_MASK) != 0) {
            flags |= allFlags[1];
        }
        if ((buttons & InputEvent.BUTTON3_MASK) != 0) {
            flags |= allFlags[2];
        }
        win32.mouse_event(flags, 0, 0, 0, 0l);

    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#mouseMove(int, int)
     */
    public void mouseMove(int x, int y) {
        Dimension screenSize = getScreenSize();
        double k = 65535.;
        double kx = k / (screenSize.width - 1);
        double ky = k / (screenSize.height - 1);
        win32.mouse_event(WindowsDefs.MOUSEEVENTF_ABSOLUTE |
                          WindowsDefs.MOUSEEVENTF_MOVE,
                          (int) (kx * x), (int) (ky * y), 0, 0l);

    }

    /**
     * @see org.apache.harmony.awt.wtk.NativeRobot#mouseWheel(int)
     */
    public void mouseWheel(int wheelAmt) {
        // windows wheel amount is opposite to Java wheel amount
        win32.mouse_event(WindowsDefs.MOUSEEVENTF_WHEEL, 0, 0,
                          -wheelAmt * WindowsDefs.WHEEL_DELTA, 0l);

    }

    private byte translateKeyCode(int keyCode) {
        int vKey = keyCode;
        switch (keyCode) {
        case KeyEvent.VK_ENTER:
            vKey = WindowsDefs.VK_RETURN;
            break;

        case KeyEvent.VK_INSERT:
            vKey = WindowsDefs.VK_INSERT;
            break;
        case KeyEvent.VK_DELETE:
            vKey = WindowsDefs.VK_DELETE;
            break;
        case KeyEvent.VK_SEMICOLON:
            vKey = WindowsDefs.VK_OEM_1;
            break;
        case KeyEvent.VK_SLASH:
            vKey = WindowsDefs.VK_OEM_2;
            break;
        case KeyEvent.VK_OPEN_BRACKET:
            vKey = WindowsDefs.VK_OEM_4;
            break;
        case KeyEvent.VK_BACK_SLASH:
            vKey = WindowsDefs.VK_OEM_5;
            break;
        case KeyEvent.VK_CLOSE_BRACKET:
            vKey = WindowsDefs.VK_OEM_6;
            break;
        case KeyEvent.VK_COMMA:
            vKey = WindowsDefs.VK_OEM_COMMA;
            break;
        case KeyEvent.VK_PERIOD:
            vKey = WindowsDefs.VK_OEM_PERIOD;
            break;
        case KeyEvent.VK_MINUS:
            vKey = WindowsDefs.VK_OEM_MINUS;
            break;
        case KeyEvent.VK_EQUALS:
            vKey = WindowsDefs.VK_OEM_PLUS;
            break;
        }
        if ((keyCode >= KeyEvent.VK_F13) && (keyCode <= KeyEvent.VK_F24)) {
            vKey = WindowsDefs.VK_F13 + keyCode - KeyEvent.VK_F13;
        }
        if ( (vKey < 3) || (vKey > 254) || (vKey >=4) && (vKey <= 7) ||
            (vKey >= 0xA) && (vKey <= 0xB) || (vKey >= 0xE) && (vKey <= 0xF) ||
            (vKey == 0x16) || (vKey == 0x1A) || (vKey >= 0x3A) && (vKey <= 0x40) ||
            (vKey >= 0x5B) && (vKey <= 0x5F) || (vKey >= 0x88) && (vKey <= 0x8F) ||
            (vKey >= 0x92) && (vKey <= 0x9F) || (vKey >= 0xB8) && (vKey <= 0xB9) ||
            (vKey >= 0xC1) && (vKey <= 0xDA) || (vKey >= 0xE0) && (vKey <= 0xE1) ||
            (vKey >= 0xE3) && (vKey <= 0xE4) || (vKey == 0xE6) ||
            (vKey >= 0xE8) && (vKey <= 0xF5)) {
            // awt.1B=Invalid key code
            throw new IllegalArgumentException(Messages.getString("awt.1B")); //$NON-NLS-1$
        }
        return (byte) vKey;
    }

    private long createScreenBMP(Rectangle screenRect) {
        int w = screenRect.width;
        int h = screenRect.height;
        long screenDC = win32.GetDC(0l);
        long compBMP = win32.CreateCompatibleBitmap(screenDC, w, h);
        long compDC = win32.CreateCompatibleDC(screenDC);
        // create a compatible bitmap to copy screen to

        if (compBMP == 0) {
            return 0l; //error
        }
        long hObj = win32.SelectObject(compDC, compBMP);
        if (hObj == 0) {
            return 0l; //error
        }

        // copy color data from the part of the screen into
        // a compatible bitmap
        final int SRCCOPY = 0x00CC0020;
        hObj = win32.BitBlt(compDC, 0, 0, w, h,
                            screenDC, screenRect.x, screenRect.y,
                            SRCCOPY);
        win32.ReleaseDC(0, screenDC);
        win32.ReleaseDC(0, compDC);
        return compBMP;
    }

    private Win32.BITMAPINFO createAndFillBMI(int w, int h) {
        Win32.BITMAPINFO bmi = win32.createBITMAPINFO(false);
        Win32.BITMAPINFOHEADER bmih = bmi.get_bmiHeader();
        bmih.set_biSize(bmih.size());
        bmih.set_biWidth(w);
        bmih.set_biHeight(-h);
        bmih.set_biPlanes((short)1);
        bmih.set_biBitCount((short)32);
        bmih.set_biCompression(WindowsDefs.BI_RGB);
        return bmi;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private Dimension getScreenSize() {
        int w = win32.GetSystemMetrics(WindowsDefs.SM_CXSCREEN);
        int h = win32.GetSystemMetrics(WindowsDefs.SM_CYSCREEN);
        return new Dimension(w, h);
    }
}
