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

import java.util.ArrayList;
import java.util.List;

public interface OGLContextManager {
    public static class OffscreenBufferObject {
        private static final int MAX_CACHED_BUFFERS = 10;
        private static final List<OffscreenBufferObject> availableBuffers = new ArrayList<OffscreenBufferObject>();

        public final long id;
        public final int width;
        public final int height;
        public final OGLContextManager config;
        public final long hdc;

        public OffscreenBufferObject(long id, long hdc, int width, int height, OGLContextManager config) {
            this.id = id;
            this.hdc = hdc;
            this.width = width;
            this.height = height;
            this.config = config;
        }

        public static final OffscreenBufferObject getCachedBuffer(int w, int h, OGLContextManager config) {
            for (int i = 0; i < availableBuffers.size(); i++) { // First try to find cached pbuffer
                OffscreenBufferObject pbuffer = availableBuffers.get(i);
                if (pbuffer.width >= w && pbuffer.height >= h && pbuffer.config == config) {
                    availableBuffers.remove(i);
                    return pbuffer;
                }
            }

            return null;
        }

        public static final OffscreenBufferObject freeCachedBuffer(OffscreenBufferObject pbuffer) {
            if (availableBuffers.size() <= MAX_CACHED_BUFFERS) {
                availableBuffers.add(pbuffer);
                return null;
            }

            // Try to find smaller pbuffer in the cache and replace it
            for (int i=0; i<availableBuffers.size(); i++) {
                OffscreenBufferObject cached = availableBuffers.get(i);
                if (
                        cached.width < pbuffer.width ||
                        cached.height < pbuffer.height ||
                        cached.config != pbuffer.config
                ) {
                    availableBuffers.remove(i);
                    availableBuffers.add(pbuffer);
                    pbuffer = cached;
                    return pbuffer;
                }
            }

            return pbuffer;
        }

        public static final void clearCache() {
            for (int i=0; i<availableBuffers.size(); i++) {
                OffscreenBufferObject cached = availableBuffers.get(i);
                cached.config.freeOffscreenBuffer(cached.id, cached.hdc);
            }
            availableBuffers.clear();
        }
    }

    /**
     * Creates OpenGL context based on GraphicsConfiguration
     * @param drawable - window handle, pbuffer, or linux drawable
     * @param hdc - if drawable is offscreen
     * @return context handle
     */
    public long getOGLContext(long drawable, long hdc);

    /**
     * Destroys existing OpenGL context
     * @param oglContext - context
     */
    public void destroyOGLContext(long oglContext);

    /**
     * Makes OpenGL context current
     * @param oglContext - OpenGL context handle
     * @param drawable - window handle, pbuffer, or linux drawable
     * @param hdc - if drawable is offscreen
     * @return false if context was alrady current, true otherwise
     */
    public boolean makeCurrent(long oglContext, long drawable, long hdc);

    public boolean makeContextCurrent(long oglContext, long draw, long read, long drawHDC, long readHDC);

    public void swapBuffers(long drawable, long hdc);

    public OffscreenBufferObject createOffscreenBuffer(int w, int h);
    
    public void freeOffscreenBuffer(OffscreenBufferObject buffer);

    public void freeOffscreenBuffer(long id, long hdc);
}
