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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.WeakHashMap;
import org.apache.harmony.awt.gl.Surface;
import org.apache.harmony.awt.gl.opengl.OGLBlitter.OGLTextureParams;

// TODO - this class could be a prototype for the common resource cache.
// E.g. opengl contexts could be bounded to the thread that created the context and
// this cache will manage the destruction of the contexts.
public class TextureCache {
    private static final HashMap<WeakReference<Surface>, OGLTextureParams> ref2texture = new HashMap<WeakReference<Surface>, OGLTextureParams>();

    private static final ThreadLocal<TextureCache> localInstance = new ThreadLocal<TextureCache>() {
        @Override
        public TextureCache initialValue() {
            return new TextureCache();
        }
    };

    static TextureCache getInstance() {
        return localInstance.get();
    }

    private final ReferenceQueue<Surface> rq = new ReferenceQueue<Surface>();

    private final WeakHashMap<Surface, WeakReference<Surface>> surface2ref = new WeakHashMap<Surface, WeakReference<Surface>>();

    void add(Surface key, OGLBlitter.OGLTextureParams texture) {
        WeakReference<Surface> ref = new WeakReference<Surface>(key, rq);
        surface2ref.put(key, ref);
        ref2texture.put(ref, texture);
    }

    void cleanupTextures() {
        Reference<? extends Surface> ref;
        while ((ref = rq.poll()) != null) {
            OGLBlitter.OGLTextureParams tp = ref2texture.remove(ref);
            tp.deleteTexture();
        }
    }

    OGLBlitter.OGLTextureParams findTexture(Surface key) {
        OGLBlitter.OGLTextureParams tp = ref2texture.get(surface2ref.get(key));
        return tp;
    }

    void remove(Surface key) {
        WeakReference<Surface> ref = surface2ref.remove(key);
        if (ref != null) {
            ref.clear();
            OGLBlitter.OGLTextureParams tp = ref2texture.remove(ref);
            tp.deleteTexture();
        }
    }
}
