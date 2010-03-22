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
 *
 * @date: Sep 28, 2005
 */

package java.awt.image;

import org.apache.harmony.awt.internal.nls.Messages;

public class Kernel implements Cloneable {
    private final int xOrigin;
    private final int yOrigin;
    private int width;
    private int height;
    float data[];

    public Kernel(int width, int height, float[] data) {
        int dataLength = width*height;
        if (data.length < dataLength) {
            // awt.22B=Length of data should not be less than width*height
            throw new IllegalArgumentException(Messages.getString("awt.22B")); //$NON-NLS-1$
        }

        this.width = width;
        this.height = height;

        this.data = new float[dataLength];
        System.arraycopy(data, 0, this.data, 0, dataLength);

        xOrigin = (width-1)/2;
        yOrigin = (height-1)/2;
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public final float[] getKernelData(float[] data) {
        if (data == null) {
            data = new float[this.data.length];
        }
        System.arraycopy(this.data, 0, data, 0, this.data.length);

        return data;
    }

    public final int getXOrigin() {
        return xOrigin;
    }

    public final int getYOrigin() {
        return yOrigin;
    }

    @Override
    public Object clone() {
        return new Kernel(width, height, data);
    }
}
