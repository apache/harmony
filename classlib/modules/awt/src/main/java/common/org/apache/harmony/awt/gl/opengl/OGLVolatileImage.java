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

package org.apache.harmony.awt.gl.opengl;

import org.apache.harmony.awt.gl.GLVolatileImage;
import org.apache.harmony.awt.gl.Surface;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.wtk.NativeWindow;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.lang.reflect.InvocationTargetException;

public class OGLVolatileImage extends GLVolatileImage {
    //private static final GL gl = GL.getInstance();
    private static final ImageCapabilities ic = new ImageCapabilities(true);

    private int w;
    private int h;
    private OGLOffscreenWindow win;
    private OGLContextManager ctxmgr;
    private OGLGraphics2D lastGraphics = null;

    private final Disposer disposer = new Disposer();

    /**
     * Helps us use OGL graphics in the uniform way
     */
    public class OGLOffscreenWindow implements NativeWindow {
        private Rectangle bounds;
        private OGLContextManager.OffscreenBufferObject pbuffer;

        OGLOffscreenWindow(OGLContextManager.OffscreenBufferObject pbuffer, Rectangle bounds) {
            this.pbuffer = pbuffer;
            this.bounds = bounds;
        }

        private OGLContextManager.OffscreenBufferObject getOffscreenBuffer() {
            return pbuffer;
        }

        public long getId() {
            return pbuffer.id;
        }

        public long getHdc() {
            return pbuffer.hdc;
        }

        public Rectangle getBounds() {
            return bounds;
        }

        // Following methods are placeholders - not used
        public void setVisible(boolean v) {
        }
        public void setBounds(int x, int y, int w, int h, int boundsMask) {
        }
        public Insets getInsets() {
            return null;
        }
        public void setEnabled(boolean value) {
        }
        public void setFocusable(boolean value) {
        }
        public boolean isFocusable() {
            return false;
        }
        public boolean setFocus(boolean focus) {
            return false;
        }
        public void dispose() {
        }
        public void placeAfter(NativeWindow w) {
        }
        public void toFront() {
        }
        public void toBack() {
        }
        public void setResizable(boolean value) {
        }
        public void setTitle(String title) {
        }
        public void grabMouse() {
        }
        public void ungrabMouse() {
        }
        public void setState(int state) {
        }
        public void setIconImage(Image image) {
        }
        public void setAlwaysOnTop(boolean value) {
        }
        public void setMaximizedBounds(Rectangle bounds) {
        }
        public Point getScreenPos() {
            return null;
        }
        public void setPacked(boolean packed) {
        }
        public Surface getSurface() {
            return null;
        }
        public MultiRectArea getObscuredRegion(Rectangle part) {
            return null;
        }

        public void setIMStyle() {
        }
    }

    public OGLVolatileImage(OGLContextManager ctxmgr, int w, int h) {
        this.ctxmgr = ctxmgr;
        this.w = w;
        this.h = h;
        this.win = new OGLOffscreenWindow(
                ctxmgr.createOffscreenBuffer(w, h),
                new Rectangle(0, 0, w, h)
        );
    }


    @Override
    public Surface getImageSurface() {
        return lastGraphics.getSurface();
    }

    @Override
    public boolean contentsLost() {
        return false;
    }

    @Override
    public Graphics2D createGraphics() {/*
        boolean firstTime = false; // First time we need to clear buffer
        if (lastGraphics == null) {
            firstTime = true;
        }*/

        lastGraphics = new OGLGraphics2D(win, 0, 0, w, h);
        /*
        if (firstTime) {
            lastGraphics.makeCurrent();
            gl.glClear(GLDefs.GL_COLOR_BUFFER_BIT);
        }
        */
        return lastGraphics;
    }

    @Override
    public ImageCapabilities getCapabilities() {
        return ic;
    }

    @Override
    public int getHeight() {
        return h;
    }

    @Override
    public BufferedImage getSnapshot() {
        Surface s = getImageSurface();
        return new BufferedImage(s.getColorModel(), s.getRaster(), true, null);
    }

    @Override
    public int getWidth() {
        return w;
    }

    @Override
    public int validate(GraphicsConfiguration gc) {
        if (gc.equals(ctxmgr)) {
            return IMAGE_OK;
        }
        return IMAGE_INCOMPATIBLE;
    }

    @Override
    public Object getProperty(String name, ImageObserver observer) {
        return UndefinedProperty;
    }

    @Override
    public int getWidth(ImageObserver observer) {
        return w;
    }

    @Override
    public int getHeight(ImageObserver observer) {
        return h;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        disposer.dispose();
    }

    @Override
    public void flush() {
        super.flush();
        disposer.dispose();
    }

    private void disposeImpl() {
        if (win != null) {
            ctxmgr.freeOffscreenBuffer(win.getOffscreenBuffer());
            win = null;
        }
    }

    private final class Disposer {
        boolean createdInEventDispatchThread;
        Thread creatorThread = null;
        boolean objectDisposed = false;

        Disposer() {
            createdInEventDispatchThread = EventQueue.isDispatchThread();
            creatorThread = Thread.currentThread();
        }

        private final void dispose() {
            if (!objectDisposed) {
                Thread disposingThread = Thread.currentThread();
                if (creatorThread == disposingThread) {
                    disposeImpl();
                    objectDisposed = true;
                } else if (createdInEventDispatchThread) {
                    try {
                        EventQueue.invokeAndWait(
                                new Runnable() {
                                    public void run () {
                                        disposeImpl();
                                    }
                                }
                        );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    objectDisposed = true;
                }
            }
            // Don't set objectDisposed to true if can't dispose from the current thread.
            // Threre's still a hope that dispose will be invoked from another thread.
        }
    }
}
