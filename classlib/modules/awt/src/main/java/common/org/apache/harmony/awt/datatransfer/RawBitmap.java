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
package org.apache.harmony.awt.datatransfer;

/**
 * RawBitmap
 */
public final class RawBitmap {

    public final int width;
    public final int height;
    public final int stride;
    public final int bits;
    public final int rMask;
    public final int gMask;
    public final int bMask;
    
    /**
     * Array representing bitmap data.
     * Depending on bit count per pixel, it could be
     * int[] (32 bits) or short[] (15 or 16 bits) or byte[] (8 or 24 bits) 
     */
    public final Object buffer;
    
    public RawBitmap(int w, int h, int stride, int bits, 
            int rMask, int gMask, int bMask, Object buffer) {
        this.width = w;
        this.height = h;
        this.stride = stride;
        this.bits = bits;
        this.rMask = rMask;
        this.gMask = gMask;
        this.bMask = bMask;
        this.buffer = buffer;
    }
    
    public RawBitmap(int header[], Object buffer) {
        this.width = header[0];
        this.height = header[1];
        this.stride = header[2];
        this.bits = header[3];
        this.rMask = header[4];
        this.gMask = header[5];
        this.bMask = header[6];
        this.buffer = buffer;
    }
    
    public int[] getHeader() {
        return new int[] { width, height, stride, bits, rMask, gMask, bMask }; 
    }
}
