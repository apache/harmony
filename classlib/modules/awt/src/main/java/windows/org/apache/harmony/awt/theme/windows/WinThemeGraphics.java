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
package org.apache.harmony.awt.theme.windows;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.gl.windows.WinGDIPGraphics2D;


/**
 * Native painting of standard components
 */
public final class WinThemeGraphics {

    private long hOldClipRgn;
    private long hTheme;
    private final long gi;
    private final int trX;
    private final int trY;

    public WinThemeGraphics(Graphics gr) {
        WinGDIPGraphics2D wgr = (WinGDIPGraphics2D)gr;
        this.gi = wgr.getGraphicsInfo();
        trX = Math.round((float)wgr.getTransform().getTranslateX());
        trY = Math.round((float)wgr.getTransform().getTranslateY());

        int clip[];
        Shape clipShape = gr.getClip();
        if (clipShape instanceof MultiRectArea) {
            clip = ((MultiRectArea) clipShape).rect;
            if (trX != 0 || trY != 0) {
                int rect[] = clip;
                int len = clip[0];
                clip = new int[len];
                System.arraycopy(rect, 0, clip, 0, len);
                for (int i = 1; i < len; i += 2) {
                    clip[i] += trX;
                    clip[i+1] += trY;
                }
            }
        } else {
            Rectangle r = clipShape.getBounds();
            clip = new int[] { 5, trX + r.x, trY + r.y,
                    trX + r.x + r.width - 1, trY + r.x + r.height - 1 };
        }

        hOldClipRgn = setGdiClip(gi, clip, clip[0]-1);
    }

    public void dispose() {
        restoreGdiClip(gi, hOldClipRgn);
        hOldClipRgn = 0;
    }

    public void setTheme(long hTheme) {
        this.hTheme = hTheme;
    }

    public void drawXpBackground(Rectangle r, int type, int state) {
        drawXpBackground(r.x, r.y, r.width, r.height, type, state);
    }

    public void drawXpBackground(Dimension size, int type, int state) {
        drawXpBackground(0, 0, size.width, size.height, type, state);
    }

    public void drawXpBackground(int x, int y, int w, int h,
            int type, int state) {
        drawXpBackground(gi, trX + x, trY + y, w, h, hTheme, type, state);
    }

    public void drawClassicBackground(Rectangle r, int type, int state) {
        drawClassicBackground(r.x, r.y, r.width, r.height, type, state);
    }

    public void drawClassicBackground(Dimension size, int type, int state) {
        drawClassicBackground(0, 0, size.width, size.height, type, state);
    }

    public void drawClassicBackground(int x, int y, int w, int h,
            int type, int state) {
        drawClassicBackground(gi, trX + x, trY + y, w, h, type, state);
    }

    public void fillBackground(Dimension size, Color color, boolean solid) {
        fillBackground(0, 0, size.width, size.height, color, solid);
    }

    public void fillBackground(Rectangle r, Color color, boolean solid) {
        fillBackground(r.x, r.y, r.width, r.height, color, solid);
    }

    public void fillBackground(int x, int y, int w, int h,
            Color color, boolean solid) {
        fillBackground(gi, trX + x, trY + y, w, h, getRGB(color), solid);
    }

    public void drawFocusRect(Rectangle r, int offset) {
        drawFocusRect(r.x + offset, r.y + offset,
                r.width - 2 * offset, r.height - 2 * offset);
    }

    public void drawFocusRect(Dimension size, int offset) {
        drawFocusRect(offset, offset,
                size.width - 2 * offset, size.height - 2 * offset);
    }

    public void drawFocusRect(int x, int y, int w, int h, int offset) {
        drawFocusRect(x + offset, y + offset,
                w - 2 * offset, h - 2 * offset);
    }

    public void drawFocusRect(int x, int y, int w, int h) {
        drawFocusRect(gi, trX + x, trY + y, w, h);
    }

    public void drawEdge(Rectangle r, int type) {
        drawEdge(r.x, r.y, r.width, r.height, type);
    }

    public void drawEdge(Dimension size, int type) {
        drawEdge(0, 0, size.width, size.height, type);
    }

    public void drawEdge(int x, int y, int w, int h, int type) {
        drawEdge(gi, trX + x, trY + y, w, h, type);
    }

    public void fillHatchedSysColorRect(Rectangle r,
            int sysColor1, int sysColor2) {
        fillHatchedSysColorRect(r.x, r.y, r.width, r.height,
                sysColor1, sysColor2);
    }

    public void fillHatchedSysColorRect(Dimension size,
            int sysColor1, int sysColor2) {
        fillHatchedSysColorRect(0, 0, size.width, size.height,
                sysColor1, sysColor2);
    }

    public void fillHatchedSysColorRect(int x, int y, int w, int h,
            int sysColor1, int sysColor2) {
        fillHatchedSysColorRect(gi, trX + x, trY + y, w, h,
                sysColor1, sysColor2);
    }

    private static int getRGB(Color c) {
        return (c != null) ? c.getRGB() : 0xFFFFFFFF;
    }

    public static native long setGdiClip(long gi, int clip[], int clipLength);

    public static native void restoreGdiClip(long gi, long hOldClipRgn);

    private static native void drawXpBackground(long gi, int x, int y, int w,
            int h, long hTheme, int type, int state);

    private static native void drawClassicBackground(long gi, int x, int y,
            int w, int h, int type, int state);

    private static native void fillBackground(long gi, int x, int y,
            int w, int h, int backColorRGB, boolean solidBack);

    private static native void drawFocusRect(long gi,
            int x, int y, int w, int h);

    private static native void drawEdge(long gi,
            int x, int y, int w, int h, int type);

    private static native void fillHatchedSysColorRect(long gi,
            int x, int y, int w, int h, int sysColor1, int sysColor2);

}