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
 * @author Oleg V. Khaschansky
 */

package org.apache.harmony.awt.gl.windows;

import org.apache.harmony.awt.gl.opengl.OGLContextManager;
import org.apache.harmony.awt.gl.opengl.GLDefs;
import org.apache.harmony.awt.gl.opengl.OGLVolatileImage;
import org.apache.harmony.awt.nativebridge.windows.*;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.awt.nativebridge.Int32Pointer;
import org.apache.harmony.awt.wtk.windows.WindowProcHandler;
import org.apache.harmony.awt.wtk.windows.WinEventQueue;
import org.apache.harmony.awt.ContextStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.awt.image.VolatileImage;

public class WGLGraphicsConfiguration
        extends WinGraphicsConfiguration
        implements OGLContextManager {
    private static final Win32 w32 = Win32.getInstance();
    private static final WGL wgl = WGL.getInstance();

    private static final String WINDOW_CLASS_NAME = "org.apache.harmony.awt.gl.tmpWindow";
    private static final int FORMATTED_DRAWABLES_CACHE_SIZE = 255;

    private static final ThreadLocal oglContextThreadLocal = new ThreadLocal();

    private static long currentOGLContext;
    private static long currentDrawable;

    private static final ArrayList existingContexts = new ArrayList();
    private static final HashSet formattedDrawables = new HashSet(255);

    private static boolean wndClassRegistetred;

    private long hTmpWnd;
    private long hTmpDC;
    private long tmpCtx;

    // XXX - todo - maybe we need different pointers for different configurations?
    //private static boolean resolvedEXT = false; // Are ext funcs resolved?

    private long getLocalOGLContext() {
        Long ctxId = (Long) oglContextThreadLocal.get();
        return ctxId == null ? 0 : ctxId.longValue();
    }

    public WGLGraphicsConfiguration(WinGraphicsDevice device, int index, Win32.PIXELFORMATDESCRIPTOR pfd) {
        super(device, index, pfd);
    }

    public WGLGraphicsConfiguration(long hwnd, long hdc) {
        super(hwnd, hdc);
    }

    public long windowProc(long hwnd, int msg, long wParam, long lParam) {
        return w32.DefWindowProcW(hwnd, msg, wParam, lParam);
    }

    private final long getHWND() {
        if (!wndClassRegistetred) {
            WindowProcHandler.registerWindowClass(WINDOW_CLASS_NAME);
            wndClassRegistetred = true;
        }

        WinEventQueue.Task task = new WinEventQueue.Task () {
            public void perform() {
                returnValue = new Long(w32.CreateWindowExW(
                        0, WINDOW_CLASS_NAME, "TmpWindow",
                        0, 0, 0, 0, 0, 0, 0, 0, null
                ));
            }
        };

        WinEventQueue winEventQueue = ((WinEventQueue) ContextStorage.getNativeEventQueue());
        winEventQueue.performTask(task);

        return ((Long)task.returnValue).longValue();
    }

    private void activateTmpCtx() {
        if (hTmpWnd == 0) {
            hTmpWnd = getHWND();
            hTmpDC = w32.GetDC(hTmpWnd);

            Win32.PIXELFORMATDESCRIPTOR pfd = w32.createPIXELFORMATDESCRIPTOR(false);
            pfd.set_nSize((short)pfd.size());
            pfd.set_nVersion((short)1);
            pfd.set_dwFlags(WindowsDefs.PFD_DRAW_TO_WINDOW | WindowsDefs.PFD_SUPPORT_OPENGL);
            pfd.set_iPixelType((byte)WindowsDefs.PFD_TYPE_RGBA);
            pfd.set_cAlphaBits((byte)8);
            pfd.set_cStencilBits((byte)1);
            pfd.set_iLayerType((byte)WindowsDefs.PFD_MAIN_PLANE);

            int pixelFormat = w32.ChoosePixelFormat(hTmpDC, pfd);
            w32.SetPixelFormat(hTmpDC, pixelFormat, pfd);
            tmpCtx = wgl.wglCreateContext(hTmpDC);
        }

        wgl.wglMakeCurrent(hTmpDC, tmpCtx);
    }

    protected void finalize() throws Throwable {
        wgl.wglDeleteContext(tmpCtx);
        w32.ReleaseDC(hTmpWnd, hTmpDC);
        w32.DestroyWindow(hTmpWnd);
    }

    private final int choosePixelFormatARB(long hdc) {
        /*
        if (!resolvedEXT) {
            resolveEXT();
        }
        */
        activateTmpCtx();

        Int32Pointer nFormats = NativeBridge.getInstance().createInt32Pointer(1, true);
        Int32Pointer formatPtr = NativeBridge.getInstance().createInt32Pointer(1, true);
        Int32Pointer attribList = NativeBridge.getInstance().createInt32Pointer(13, true);

        // Fill in FB config attributes
        attribList.set(0, WGLDefs.WGL_PIXEL_TYPE_ARB);
        attribList.set(1, WGLDefs.WGL_TYPE_RGBA_ARB);
        attribList.set(2, WGLDefs.WGL_DRAW_TO_WINDOW_ARB);
        attribList.set(3, GLDefs.GL_TRUE);
        attribList.set(4, WGLDefs.WGL_DRAW_TO_PBUFFER_ARB);
        attribList.set(5, GLDefs.GL_TRUE);
        attribList.set(6, WGLDefs.WGL_STENCIL_BITS_ARB);
        attribList.set(7, 1);
        attribList.set(8, WGLDefs.WGL_ALPHA_BITS_ARB);
        attribList.set(9, 8);
        attribList.set(10, WGLDefs.WGL_ACCELERATION_ARB);
        attribList.set(11, WGLDefs.WGL_FULL_ACCELERATION_ARB);
        //attribList.set(12, WGLDefs.WGL_DOUBLE_BUFFER_ARB);
        //attribList.set(13, GLDefs.GL_TRUE);
        attribList.set(12, 0);

        wgl.wglChoosePixelFormatARB(hdc, attribList, null, 1, formatPtr, nFormats);

        int res = formatPtr.get(0);

        formatPtr.free();
        nFormats.free();
        attribList.free();

        return res;
    }

    /**
     * This method calls ext (ARB) functions to resolve its addresses.
     * For now it calls only one - wglChoosePixelFormatARB
     */
    /*
    private final void resolveEXT() {
        long hwnd = getHWND();
        long hdc = w32.GetDC(hwnd);

        Win32.PIXELFORMATDESCRIPTOR pfd = w32.createPIXELFORMATDESCRIPTOR(false);
        pfd.set_nSize((short)pfd.size());
        pfd.set_nVersion((short)1);
        pfd.set_dwFlags(WindowsDefs.PFD_DRAW_TO_WINDOW | WindowsDefs.PFD_SUPPORT_OPENGL);
        pfd.set_iPixelType((byte)WindowsDefs.PFD_TYPE_RGBA);
        pfd.set_cColorBits((byte)32);
        pfd.set_cAlphaBits((byte)8);
        pfd.set_cStencilBits((byte)1);
        pfd.set_iLayerType((byte)WindowsDefs.PFD_MAIN_PLANE);

        int pixelFormat = w32.ChoosePixelFormat(hdc, pfd);
        w32.SetPixelFormat(hdc, pixelFormat, pfd);
        // XXX - todo - check for success
        long ctx = wgl.wglCreateContext(hdc);
        wgl.wglMakeCurrent(hdc, ctx);

        WGL.init(); // Initialize function pointers

        wgl.wglMakeCurrent(0, 0);
        wgl.wglDeleteContext(ctx);

        w32.ReleaseDC(hwnd, hdc);
        w32.DestroyWindow(hwnd);

        resolvedEXT = true;
    }
    */
    public long getOGLContext(long drawable, long oshdc) {
        long oglContext = getLocalOGLContext();

        if (oglContext == 0) {
            long hdc = (oshdc == 0) ? w32.GetDC(drawable) : oshdc;

            if (oshdc == 0) {
                int pixelFormat = choosePixelFormatARB(hdc);
                w32.SetPixelFormat(hdc, pixelFormat, w32.createPIXELFORMATDESCRIPTOR(false));
                if (formattedDrawables.size() > FORMATTED_DRAWABLES_CACHE_SIZE) {
                    formattedDrawables.clear();
                }
                formattedDrawables.add(new Long(drawable));
            }

            oglContext = wgl.wglCreateContext(hdc);

            if(oshdc == 0) {
                w32.ReleaseDC(drawable, hdc);
            }

            // Share display lists if there are othe contexts
            if (existingContexts.size() > 0) {
                wgl.wglShareLists(((Long)existingContexts.get(0)).longValue(), oglContext);
            }

            existingContexts.add(new Long(oglContext));
            oglContextThreadLocal.set(new Long(oglContext));
        }

        return oglContext;
    }

    public void destroyOGLContext(long oglContext) {
        if (oglContext == currentOGLContext) {
            currentOGLContext = 0;
        }

        wgl.wglDeleteContext(oglContext);
        existingContexts.remove(new Long(oglContext));
    }

    public boolean makeCurrent(long oglContext, long drawable, long oshdc) {
        if (oglContext != currentOGLContext || drawable != currentDrawable) {

            long hdc = (oshdc == 0) ? w32.GetDC(drawable) : oshdc;

            if (oshdc == 0 && !formattedDrawables.contains(new Long(drawable))) { // Need to set pixel format
                int pixelFormat = choosePixelFormatARB(hdc);
                w32.SetPixelFormat(hdc, pixelFormat, w32.createPIXELFORMATDESCRIPTOR(false));
                if (formattedDrawables.size() > FORMATTED_DRAWABLES_CACHE_SIZE) {
                    formattedDrawables.clear();
                }
                formattedDrawables.add(new Long(drawable));
            }

            if (wgl.wglMakeCurrent(hdc, oglContext) == 0) {
                w32.ReleaseDC(drawable, hdc);
                throw new IllegalStateException("Cannot make opengl context current");
            }

            if(oshdc == 0) {
                w32.ReleaseDC(drawable, hdc);
            }

            currentOGLContext = oglContext;
            currentDrawable = drawable;

            return true;
        }

        return false;
    }

    public boolean makeContextCurrent(
            long oglContext,
            long draw, long read,
            long drawHDC, long readHDC
    ) {
        return makeCurrent(oglContext, draw, drawHDC); // XXX - todo - implement
    }

    public void swapBuffers(long drawable, long oshdc) {
        long hdc = (oshdc == 0) ? w32.GetDC(drawable) : oshdc;
        wgl.SwapBuffers(hdc);
        if(oshdc == 0) {
            w32.ReleaseDC(drawable, hdc);
        }
    }

    public OffscreenBufferObject createOffscreenBuffer(int w, int h) {
        // Try to get pbuffer from cache
        OffscreenBufferObject pbuffer = OffscreenBufferObject.getCachedBuffer(w, h, this);
        if (pbuffer != null) {
            return pbuffer;
        }

        long hwnd = getHWND();
        long hdc = w32.GetDC(hwnd);

        int pixelFormat = choosePixelFormatARB(hdc);

        Int32Pointer attribList = NativeBridge.getInstance().createInt32Pointer(1, true);
        attribList.set(0,0);
        long id = wgl.wglCreatePbufferARB(hdc, pixelFormat, w, h, attribList.lock());
        attribList.unlock();
        attribList.free();

        long buffHdc = wgl.wglGetPbufferDCARB(id);

        w32.ReleaseDC(hwnd, hdc);
        w32.DestroyWindow(hwnd);

        return new OffscreenBufferObject(id, buffHdc, w, h, this);
    }

    public void freeOffscreenBuffer(OffscreenBufferObject pbuffer) {
        pbuffer = OffscreenBufferObject.freeCachedBuffer(pbuffer);

        if (pbuffer != null) {
            wgl.wglReleasePbufferDCARB(pbuffer.id, pbuffer.hdc);
            wgl.wglDestroyPbufferARB(pbuffer.id);
        }
    }

    public void freeOffscreenBuffer(long id, long hdc) {
        wgl.wglReleasePbufferDCARB(id, hdc);
        wgl.wglDestroyPbufferARB(id);
    }

    public VolatileImage createCompatibleVolatileImage(int width, int height) {
        return new OGLVolatileImage(this, width, height);
    }
}
