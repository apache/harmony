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
package java.awt.image;

import java.util.Hashtable;

public interface ImageConsumer {

    public static final int RANDOMPIXELORDER = 1;

    public static final int TOPDOWNLEFTRIGHT = 2;

    public static final int COMPLETESCANLINES = 4;

    public static final int SINGLEPASS = 8;

    public static final int SINGLEFRAME = 16;

    public static final int IMAGEERROR = 1;

    public static final int SINGLEFRAMEDONE = 2;

    public static final int STATICIMAGEDONE = 3;

    public static final int IMAGEABORTED = 4;

    public void setProperties(Hashtable<?, ?> props);

    public void setColorModel(ColorModel model);

    public void setPixels(int x, int y, int w, int h, ColorModel model,
            int[] pixels, int off, int scansize);

    public void setPixels(int x, int y, int w, int h, ColorModel model,
            byte[] pixels, int off, int scansize);

    public void setDimensions(int width, int height);

    public void setHints(int hintflags);

    public void imageComplete(int status);

}

