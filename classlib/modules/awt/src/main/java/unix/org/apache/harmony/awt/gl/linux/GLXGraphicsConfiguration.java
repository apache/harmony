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

package org.apache.harmony.awt.gl.linux;

import org.apache.harmony.awt.nativebridge.linux.X11;
import org.apache.harmony.awt.nativebridge.linux.GLX;
import org.apache.harmony.awt.gl.opengl.GLDefs;
import org.apache.harmony.awt.nativebridge.*;
import org.apache.harmony.awt.gl.opengl.OGLContextManager;
import org.apache.harmony.awt.gl.opengl.OGLVolatileImage;

import java.util.ArrayList;
import java.awt.image.VolatileImage;

public class GLXGraphicsConfiguration extends XGraphicsConfiguration implements OGLContextManager {
    private static final GLX glx = GLX.getInstance();

    private VoidPointer glxFBConfig; // Cached GLX FB configuration

    private static long currentOGLContext; // Stored to eliminate unneccessary native calls
    private static long currentDrawable; // Stored to eliminate unneccessary native calls
    private static ArrayList existingContexts = new ArrayList();

    //private long oglContext; // Last OGL context created for this configuration
    private static final ThreadLocal oglContextThreadLocal = new ThreadLocal();

    private long getLocalOGLContext() {
        Long ctxId = (Long) oglContextThreadLocal.get();
        return ctxId == null ? 0 : ctxId.longValue();
    }


    public GLXGraphicsConfiguration(XGraphicsDevice dev, X11.XVisualInfo info) {
        super(dev, info);
    }

    static final class GlxConfigsRec { // Represent configs for one dpy/scr combination
        // Glx configs for all dpy/scr combinations
        private static final ArrayList glxConfigs = new ArrayList(1);

        private PointerPointer configs;
        private int nConfigs;
        private long display;
        private int screen;

        private static final GlxConfigsRec findConfigRec(long display, int screen) {
            for (int i = 0; i < glxConfigs.size(); i++) {
                GlxConfigsRec rec = (GlxConfigsRec) glxConfigs.get(i);
                if (rec.display == display && rec.screen == screen)
                    return rec;
            }

            // Not found
            return addConfigRec(display, screen);
        }

        private static final GlxConfigsRec addConfigRec(long display, int screen) {
            Int32Pointer nElements = NativeBridge.getInstance().createInt32Pointer(1, true);
            Int32Pointer attribList = NativeBridge.getInstance().createInt32Pointer(9, true);

            // Fill in FB config attributes
            attribList.set(0, GLDefs.GLX_DRAWABLE_TYPE);
            attribList.set(1, GLDefs.GLX_WINDOW_BIT | GLDefs.GLX_PBUFFER_BIT);
            attribList.set(2, GLDefs.GLX_RENDER_TYPE);
            attribList.set(3, GLDefs.GLX_RGBA_BIT);
            attribList.set(4, GLDefs.GLX_STENCIL_SIZE);
            attribList.set(5, 1);
            attribList.set(6, GLDefs.GLX_ALPHA_SIZE);
            attribList.set(7, 8);
            attribList.set(8, 0);

            GlxConfigsRec res = new GlxConfigsRec();
            res.configs = glx.glXChooseFBConfig(display, screen, attribList, nElements);
            res.nConfigs = nElements.get(0);
            res.display = display;
            res.screen = screen;

            nElements.free();
            attribList.free();

            glxConfigs.add(res);

            return res;
        }

        static final long getBestGLXVisualId(long display, int screen) {
            long defVisualPtr = x11.XDefaultVisual(display, screen);
            long defVisId = x11.XVisualIDFromVisual(defVisualPtr);

            GlxConfigsRec rec = findConfigRec(display, screen);

            if (rec.nConfigs <= 0) {
                return 0; // No GLX configs found
            }

            // Check if default visual is ok
            Int32Pointer visId = NativeBridge.getInstance().createInt32Pointer(1, true);
            VoidPointer ptr = rec.configs.get(0);
            for (int i = 0; i < rec.nConfigs; i++) {
                Int8Pointer fbConfig = ptr.byteBase.getElementPointer(i*NativeBridge.ptrSize);
                glx.glXGetFBConfigAttrib(display, fbConfig.lock(), GLDefs.GLX_VISUAL_ID, visId);
                fbConfig.unlock();
                if (visId.get(0) == defVisId) {
                    visId.free();
                    return defVisId;
                }
            }

            // Get visual id from the first (best) FB config
            VoidPointer fbConfig = rec.configs.get(0);
            glx.glXGetFBConfigAttrib(display, fbConfig.lock(), GLDefs.GLX_VISUAL_ID, visId);
            fbConfig.unlock();
            long bestVisId = visId.get(0);
            visId.free();

            return bestVisId;
        }

        private static final VoidPointer getGLXFBConfig(long display, int screen, long visid) {
            VoidPointer retval = null;
            GlxConfigsRec rec = findConfigRec(display, screen);

            Int32Pointer visId = NativeBridge.getInstance().createInt32Pointer(1, true);
            VoidPointer ptr = rec.configs.get(0);
            for (int i = 0; i < rec.nConfigs; i++) {
                Int8Pointer fbConfig = ptr.byteBase.getElementPointer(i*NativeBridge.ptrSize);
                glx.glXGetFBConfigAttrib(display, fbConfig.lock(), GLDefs.GLX_VISUAL_ID, visId);
                fbConfig.unlock();
                if (visId.get(0) == visid) {
                    retval = fbConfig;
                    break;
                }
            }
            visId.free();

            return retval;
        }

        protected void finalize() throws Throwable {
            super.finalize();
            if (configs != null)
                x11.XFree(configs);
        }
    }

    public final long getOGLContext(long drawable, long hdc) {
        if (glxFBConfig == null) {
            glxFBConfig = GlxConfigsRec.getGLXFBConfig(dev.display, dev.screen, info.get_visualid());
        }

        long oglContext = getLocalOGLContext();
        if (oglContext == 0) {
            oglContext =
                    glx.glXCreateNewContext(
                            dev.display,
                            glxFBConfig.lock(),
                            GLDefs.GLX_RGBA_TYPE,
                            existingContexts.isEmpty() ?
                                0 : ((Long) existingContexts.get(0)).longValue(),
                            GLDefs.True
                    );
            glxFBConfig.unlock();

            existingContexts.add(new Long(oglContext));

            oglContextThreadLocal.set(new Long(oglContext));
        }

        return oglContext;
    }

    protected void finalize() throws Throwable {
        super.finalize();

        long oglContext = getLocalOGLContext();
        if (oglContext != 0) {
            destroyOGLContext(oglContext);
            //oglContext = 0;
            oglContextThreadLocal.set(null);

            // Remove from the common list of contexts
            existingContexts.remove(new Long(oglContext));
        }

        OffscreenBufferObject.clearCache();
    }

    public final void destroyOGLContext(long oglContext) {
        if (oglContext == currentOGLContext) {
            //gl.glXMakeCurrent(dev.display, 0, 0);
            currentOGLContext = 0;
        }

        glx.glXDestroyContext(dev.display, oglContext);
    }

    public final boolean makeCurrent(long oglContext, long drawable, long hdc) {
        if (oglContext != currentOGLContext || drawable != currentDrawable) {

            glx.glXMakeCurrent(dev.display, drawable, oglContext);

            currentOGLContext = oglContext;
            currentDrawable = drawable;

            return true;
        }

        return false;
    }

    public final boolean makeContextCurrent(
            long oglContext,
            long draw, long read,
            long drawHDC, long readHDC
    ) {
        if (read == draw) {
            return makeCurrent(oglContext, read, 0);
        }

        glx.glXMakeContextCurrent(dev.display, draw, read, oglContext);
        // Always step into makeCurrent to set same read/draw drawables
        // after calling this method.
        currentOGLContext = 0;

        return true;
    }

    public final void swapBuffers(long drawable, long hdc) {
        glx.glXSwapBuffers(dev.display, drawable);
    }

    public final OffscreenBufferObject createOffscreenBuffer(int w, int h) {
        if (glxFBConfig == null) {
            glxFBConfig = GlxConfigsRec.getGLXFBConfig(dev.display, dev.screen, info.get_visualid());
        }

        // Try to get pbuffer from cache
        OffscreenBufferObject pbuffer = OffscreenBufferObject.getCachedBuffer(w, h, this);
        if (pbuffer != null) {
            return pbuffer;
        }

        Int32Pointer attribList = NativeBridge.getInstance().createInt32Pointer(7, true);

        // Fill in FB config attributes
        attribList.set(0, GLDefs.GLX_PBUFFER_WIDTH);
        attribList.set(1, w);
        attribList.set(2, GLDefs.GLX_PBUFFER_HEIGHT);
        attribList.set(3, h);
        attribList.set(4, GLDefs.GLX_PRESERVED_CONTENTS);
        attribList.set(5, GLDefs.True);
        attribList.set(6, 0);

        long pbufferId = glx.glXCreatePbuffer(dev.display, glxFBConfig.lock(), attribList);
        attribList.free();
        glxFBConfig.unlock();

        return new OffscreenBufferObject(pbufferId, 0, w, h, this);
    }

    public void freeOffscreenBuffer(OffscreenBufferObject pbuffer) {
        pbuffer = OffscreenBufferObject.freeCachedBuffer(pbuffer);

        if (pbuffer != null) {
            glx.glXDestroyPbuffer(dev.display, pbuffer.id);
        }
    }

    public void freeOffscreenBuffer(long id, long hdc) {
        glx.glXDestroyPbuffer(dev.display, id);
    }

    public VolatileImage createCompatibleVolatileImage(int width, int height) {
        return new OGLVolatileImage(this, width, height);
    }
}
