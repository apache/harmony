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
 * @author Alexey A. Petrenko, Igor V. Stolyarov
 */
package org.apache.harmony.awt.gl;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

import org.apache.harmony.awt.gl.image.OffscreenImage;
import org.apache.harmony.awt.nativebridge.NativeBridge;
import org.apache.harmony.misc.accessors.AccessorFactory;
import org.apache.harmony.misc.accessors.ArrayAccessor;
import org.apache.harmony.misc.accessors.MemoryAccessor;
import org.apache.harmony.misc.accessors.StringAccessor;

/**
 * This class includes widely useful static instances and methods.
 *
 */
public class Utils {
    public static final MemoryAccessor memaccess = AccessorFactory.getMemoryAccessor();

    public static final ArrayAccessor arraccess = AccessorFactory.getArrayAccessor();

    public static final StringAccessor straccess = AccessorFactory.getStringAccessor();

    public static final NativeBridge nativeBridge = NativeBridge.getInstance();

    public static BufferedImage getBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        } else if (img instanceof VolatileImage) {
            return ((VolatileImage)img).getSnapshot();
        } else{
            OffscreenImage offImg;
            if (img instanceof OffscreenImage) {
                offImg = (OffscreenImage)img;
            }else{
                offImg = new OffscreenImage(img.getSource());
            }
            if(offImg.prepareImage(null)) {
                return offImg.getBufferedImage();
            }
            return null;
        }
    }
}
