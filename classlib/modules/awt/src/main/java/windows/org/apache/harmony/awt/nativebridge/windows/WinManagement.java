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
package org.apache.harmony.awt.nativebridge.windows;

import java.awt.Rectangle;

import org.apache.harmony.awt.gl.MultiRectArea;

public class WinManagement {

    public static MultiRectArea getObscuredRegion(long hwnd, Rectangle part) {
        int rects[] = getObscuredRegionImpl(hwnd, part.x, part.y,
                                            part.width, part.height);
        MultiRectArea mra = new MultiRectArea();
        for (int i=0; i<rects.length; i+=4) {
            mra.addRect(rects[i], rects[i+1], rects[i+2]-1, rects[i+3]-1);
        }
        return mra;
    }
    private static native int[] getObscuredRegionImpl(long hwnd,
            int x, int y, int w, int h);
}
