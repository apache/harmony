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
package org.apache.harmony.awt.gl.font.fontlib;

import java.awt.Graphics2D;
import java.awt.font.GlyphVector;

import org.apache.harmony.awt.gl.TextRenderer;

public class FLTextRenderer extends TextRenderer {
    
    private static final FLTextRenderer inst = new FLTextRenderer();
    
    public static FLTextRenderer getInstance() {
        return inst;
    }

    /* (non-Javadoc)
     * @see org.apache.harmony.awt.gl.TextRenderer#drawString(java.awt.Graphics2D, java.lang.String, float, float)
     */
    @Override
    public void drawString(Graphics2D g2d, String str, float x, float y) {
        g2d.fill(g2d.getFont().createGlyphVector(g2d.getFontRenderContext(), str).getOutline(x, y));
    }

    /* (non-Javadoc)
     * @see org.apache.harmony.awt.gl.TextRenderer#drawGlyphVector(java.awt.Graphics2D, java.awt.font.GlyphVector, float, float)
     */
    @Override
    public void drawGlyphVector(Graphics2D g2d, GlyphVector gv, float x, float y) {
        g2d.fill(gv.getOutline(x,y));
    }

}
