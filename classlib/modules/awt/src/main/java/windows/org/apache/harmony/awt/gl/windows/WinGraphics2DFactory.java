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
package org.apache.harmony.awt.gl.windows;

import java.awt.*;

import java.io.IOException;

import org.apache.harmony.awt.gl.CommonGraphics2DFactory;
import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.gl.opengl.OGLGraphics2D;
import org.apache.harmony.awt.gl.font.FontManager;
import org.apache.harmony.awt.gl.font.WinFontManager;
import org.apache.harmony.awt.gl.font.WindowsFont;
import org.apache.harmony.awt.wtk.NativeWindow;
import org.apache.harmony.awt.wtk.WindowFactory;


/**
 * Graphics2D factory for Windows
 *
 */
public class WinGraphics2DFactory extends CommonGraphics2DFactory {
    static {
        inst = new WinGraphics2DFactory();
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    public Graphics2D getGraphics2D(NativeWindow nw, int tx, int ty, MultiRectArea clip) {
        Insets ins = nw.getInsets();
        if (WinGraphicsDevice.useOpenGL) {
            return new OGLGraphics2D(nw, tx - ins.left, ty - ins.top, clip);
        }
        if (WinGraphicsDevice.useGDI) {
            return new WinGDIGraphics2D(nw, tx - ins.left, ty - ins.top, clip);
        }
        return new WinGDIPGraphics2D(nw, tx - ins.left, ty - ins.top, clip);
    }

    public Graphics2D getGraphics2D(NativeWindow nw, int tx, int ty, int width, int height) {
        Insets ins = nw.getInsets();
        if (WinGraphicsDevice.useOpenGL) {
            return new OGLGraphics2D(nw, tx - ins.left, ty - ins.top, width, height);
        }
        if (WinGraphicsDevice.useGDI) {
            return new WinGDIGraphics2D(nw, tx - ins.left, ty - ins.top, width, height);
        }
        return new WinGDIPGraphics2D(nw, tx - ins.left, ty - ins.top, width, height);
    }

    public GraphicsEnvironment createGraphicsEnvironment(WindowFactory wf) {
        return new WinGraphicsEnvironment(wf);
    }

    public FontManager getFontManager() {
        return WinFontManager.inst;
    }

    @Override
    public Font embedFont(String fontFilePath) throws IOException {
        return WindowsFont.embedFont(fontFilePath);
    }

}