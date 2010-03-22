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
 * @author Pavel Dolgov
 */
package java.awt;

import org.apache.harmony.awt.wtk.NativeWindow;

/**
 * The root of the component hierarchy 
 * embedded into a native application's window
 */
class EmbeddedWindow extends Window {
    private static final long serialVersionUID = -572384690015061225L;

    final long nativeWindowId;

    EmbeddedWindow(long nativeWindowId) {
        super(null);
        this.nativeWindowId = nativeWindowId;

        addNotify();
        Rectangle bounds = behaviour.getNativeWindow().getBounds();
        x = bounds.x;
        y = bounds.y;
        w = bounds.width;
        h = bounds.height;
    }

    @Override
    ComponentBehavior createBehavior() {
        return new EmbeddedBehavior();
    }


    /**
     * The component behavior for the embedded window
     */
    private class EmbeddedBehavior extends HWBehavior {

        EmbeddedBehavior() {
            super(EmbeddedWindow.this);
        }

        @Override
        protected NativeWindow createNativeWindow() {
            return toolkit.createEmbeddedNativeWindow(EmbeddedWindow.this);
        }

    }
}
