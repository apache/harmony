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
 * @author Igor V. Stolyarov
 */
package java.awt;

import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.ReplicateScaleFilter;

import org.apache.harmony.awt.internal.nls.Messages;

public abstract class Image {

    public static final Object UndefinedProperty = new Object();  //$NON-LOCK-1$

    public static final int SCALE_DEFAULT = 1;

    public static final int SCALE_FAST = 2;

    public static final int SCALE_SMOOTH = 4;

    public static final int SCALE_REPLICATE = 8;

    public static final int SCALE_AREA_AVERAGING = 16;

    protected float accelerationPriority = 0.5f;

    private static final ImageCapabilities capabilities = new ImageCapabilities(false);

    public abstract Object getProperty(String name, ImageObserver observer);

    public abstract ImageProducer getSource();

    public abstract int getWidth(ImageObserver observer);

    public abstract int getHeight(ImageObserver observer);

    public Image getScaledInstance(int width, int height, int hints) {
        ImageFilter filter;
        if ((hints & (SCALE_SMOOTH | SCALE_AREA_AVERAGING)) != 0) {
            filter = new AreaAveragingScaleFilter(width, height);
        } else {
            filter = new ReplicateScaleFilter(width, height);
        }
        ImageProducer producer = new FilteredImageSource(getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(producer);
    }

    public abstract Graphics getGraphics();

    public abstract void flush();

    public float getAccelerationPriority() {
        return accelerationPriority;
    }

    public void setAccelerationPriority(float priority) {
        if (priority < 0 || priority > 1) {
            // awt.10A=Priority must be a value between 0 and 1, inclusive
            throw new IllegalArgumentException(Messages.getString("awt.10A")); //$NON-NLS-1$
        }
        accelerationPriority = priority;
    }

    public ImageCapabilities getCapabilities(GraphicsConfiguration gc) {
        // Note: common image is not accelerated.
        return capabilities;
    }
}


