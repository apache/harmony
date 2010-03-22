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
 * @author Alexey A. Petrenko
 */
package java.awt.image;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Transparency;

/**
 * Volatile image implementation
 */
public abstract class VolatileImage extends Image
    // Volatile image implements Transparency since 1.5
    implements Transparency {
    /***************************************************************************
    *
    *  Constants
    *
    ***************************************************************************/

    public static final int IMAGE_INCOMPATIBLE = 2;

    public static final int IMAGE_OK = 0;

    public static final int IMAGE_RESTORED = 1;

    protected int transparency = OPAQUE;

    /***************************************************************************
    *
    *  Constructors
    *
    ***************************************************************************/

    public VolatileImage() {
        super();
    }



    /***************************************************************************
    *
    *  Abstract methods
    *
    ***************************************************************************/

    public abstract boolean contentsLost();

    public abstract Graphics2D createGraphics();

    public abstract ImageCapabilities getCapabilities();

    public abstract int getHeight();

    public abstract BufferedImage getSnapshot();

    public abstract int getWidth();

    public abstract int validate(GraphicsConfiguration gc);


    /***************************************************************************
    *
    *  Public methods
    *
    ***************************************************************************/

    @Override
    public void flush() {
    }

    @Override
    public Graphics getGraphics() {
        return createGraphics();
    }

    @Override
    public ImageProducer getSource() {
        return getSnapshot().getSource();
    }

    public int getTransparency() {
        return transparency;
    }
}
