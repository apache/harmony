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

package java.awt;

import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

public abstract class Graphics2D extends Graphics {
    protected Graphics2D() {
        super();
    }

    public abstract void addRenderingHints(Map<?, ?> hints);

    public abstract void clip(Shape s);

    public abstract void draw(Shape s);

    public abstract void drawGlyphVector(GlyphVector g, float x, float y);

    public abstract void drawImage(BufferedImage img, BufferedImageOp op, int x, int y);

    public abstract boolean drawImage(Image img, AffineTransform xform, ImageObserver obs);

    public abstract void drawRenderableImage(RenderableImage img, AffineTransform xform);

    public abstract void drawRenderedImage(RenderedImage img, AffineTransform xform);

    public abstract void drawString(AttributedCharacterIterator iterator, float x, float y);

    @Override
    public abstract void drawString(AttributedCharacterIterator iterator, int x, int y);

    public abstract void drawString(String s, float x, float y);

    @Override
    public abstract void drawString(String str, int x, int y);

    public abstract void fill(Shape s);

    public abstract Color getBackground();

    public abstract Composite getComposite();

    public abstract GraphicsConfiguration getDeviceConfiguration();

    public abstract FontRenderContext getFontRenderContext();

    public abstract Paint getPaint();

    public abstract Object getRenderingHint(RenderingHints.Key key);

    public abstract RenderingHints getRenderingHints();

    public abstract Stroke getStroke();

    public abstract AffineTransform getTransform();

    public abstract boolean hit(Rectangle rect, Shape s, boolean onStroke);

    public abstract void rotate(double theta);

    public abstract void rotate(double theta, double x, double y);

    public abstract void scale(double sx, double sy);

    public abstract void setBackground(Color color);

    public abstract void setComposite(Composite comp);

    public abstract void setPaint(Paint paint);

    public abstract void setRenderingHint(RenderingHints.Key key, Object value);

    public abstract void setRenderingHints(Map<?, ?> hints);

    public abstract void setStroke(Stroke s);

    public abstract void setTransform(AffineTransform Tx);

    public abstract void shear(double shx, double shy);

    public abstract void transform(AffineTransform Tx);

    public abstract void translate(double tx, double ty);

    @Override
    public abstract void translate(int x, int y);

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        // According to the spec, color should be used instead of paint,
        // so Graphics.fill3DRect resets paint and
        // it should be restored after the call
        Paint savedPaint = getPaint();
        super.fill3DRect(x, y, width, height, raised);
        setPaint(savedPaint);
    }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        // According to the spec, color should be used instead of paint,
        // so Graphics.draw3DRect resets paint and
        // it should be restored after the call
        Paint savedPaint = getPaint();
        super.draw3DRect(x, y, width, height, raised);
        setPaint(savedPaint);
    }
}