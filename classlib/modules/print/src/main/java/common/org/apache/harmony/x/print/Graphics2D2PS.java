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

package org.apache.harmony.x.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.print.PageFormat;
import java.io.PrintStream;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.harmony.awt.gl.CommonGraphics2D;

public class Graphics2D2PS extends CommonGraphics2D {

    private static final Font                DEF_FONT;
    private static final Map<String, String> FONT_MAP;

    private final PrintStream                out_stream;
    private final PageFormat                 format;
    private final Rectangle                  defaultClip;
    private double                           yscale = 1;

    static {
        DEF_FONT = new Font("Dialog", Font.PLAIN, 12); //$NON-NLS-1$
        FONT_MAP = new HashMap<String, String>();
        FONT_MAP.put("Serif", "Times"); //$NON-NLS-1$ //$NON-NLS-2$
        FONT_MAP.put("SansSerif", "Helvetica"); //$NON-NLS-1$ //$NON-NLS-2$
        FONT_MAP.put("Monospaced", "Courier"); //$NON-NLS-1$ //$NON-NLS-2$
        FONT_MAP.put("Dialog", "Helvetica"); //$NON-NLS-1$ //$NON-NLS-2$
        FONT_MAP.put("DialogInput", "Courier"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public Graphics2D2PS(final PrintStream stream, final PageFormat format) {
        super();
        if (stream == null) {
            throw new IllegalArgumentException("stream is null"); //$NON-NLS-1$
        }

        out_stream = stream;
        this.format = format != null ? format : new PageFormat();
        defaultClip = new Rectangle((int) this.format.getImageableX(),
                        (int) this.format.getImageableY(),
                        (int) this.format.getImageableWidth(),
                        (int) this.format.getImageableHeight());
        PS.printHeader(stream);
        resetGraphics();
        setColor(fgColor);
        setFont(DEF_FONT);
        setClip(defaultClip);
        ps(PS.setDefGstate);
    }

    public Graphics2D2PS(final PrintStream stream) {
        this(stream, null);
    }

    public void finish() {
        PS.printFooter(out_stream);
        out_stream.close();
    }

    public void startPage(final int number) {
        ps(PS.comment, "Page: " + number + " " + number); //$NON-NLS-1$ //$NON-NLS-2$
        ps(PS.restoreDefGstate);
        resetGraphics();
    }

    public void endOfPage(final int number) {
        ps(PS.showpage);
        ps(PS.comment, "EndPage: " + number + " " + number); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean drawImage(final Image image, final int x, final int y,
                    final ImageObserver imageObserver) {
        drawImage(image, x, convY(y));
        return true;
    }

    public boolean drawImage(final Image image, final int x, final int y,
                    final int width, final int height,
                    final ImageObserver imageObserver) {
        final BufferedImage imageGIF = (BufferedImage) image;
        final float w = (float) imageGIF.getWidth();
        final float h = (float) imageGIF.getHeight();

        drawImage(image, x, convY(y), true, ((float) width) / w,
            ((float) height) / h);
        return true;
    }

    public boolean drawImage(final Image image, final int x, final int y,
                    final Color bbgcolor, final ImageObserver imageObserver) {
        final BufferedImage imageGIF = (BufferedImage) image;
        final int iw = imageGIF.getWidth();
        final int ih = imageGIF.getHeight();
        final Color cur_color = getColor();

        setColor(bbgcolor);
        fillRect(x, y, iw, ih);
        setColor(cur_color);
        drawImage(image, x, convY(y));
        return true;
    }

    public boolean drawImage(final Image image, final int x, final int y,
                    final int width, final int height, final Color bbgcolor,
                    final ImageObserver imageObserver) {
        final BufferedImage imageGIF = (BufferedImage) image;
        final float w = (float) imageGIF.getWidth();
        final float h = (float) imageGIF.getHeight();
        final Color cur_color = getColor();

        setColor(bbgcolor);
        fillRect(x, y, width, height);
        setColor(cur_color);
        drawImage(image, x, convY(y), true, ((float) width) / w,
            ((float) height) / h);
        return true;
    }

    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2,
                    int sx1, int sy1, int sx2, int sy2,
                    ImageObserver imageObserver) {
        int sx;
        int sy;
        int width;
        int height;
        int dx;
        int dy;
        int d;
        int comp;
        BufferedImage newImage;
        BufferedImage imageGIF;

        // TODO: this method have to be improved to flip image if dx2 < dx1 or
        // dy2<dy1
        if (dx2 < dx1) {
            d = dx2;
            dx2 = dx1;
            dx1 = d;
        }
        if (dy2 < dy1) {
            d = dy2;
            dy2 = dy1;
            dy1 = d;
        }
        dx = dx2 - dx1 + 1;
        dy = dy2 - dy1 + 1;

        imageGIF = (BufferedImage) image;
        width = imageGIF.getWidth();
        height = imageGIF.getHeight();
        if (dx2 > width || dy2 > height) {
            return false;
        }
        newImage = new BufferedImage(dx, dy, BufferedImage.TYPE_INT_ARGB);

        sy = 0;
        for (int iy = dy1; iy <= dy2; iy++) {
            sx = 0;
            for (int ix = dx1; ix <= dx2; ix++) {
                comp = imageGIF.getRGB(ix, iy);
                newImage.setRGB(sx++, sy, comp);
            }
            sy++;
        }
        drawImage(newImage, sx1, sy1, sx2 - sx1 + 1, sy2 - sy1 + 1, null);

        return true;
    }

    public boolean drawImage(final Image image, final int dx1, final int dy1,
                    final int dx2, final int dy2, final int sx1, final int sy1,
                    final int sx2, final int sy2, final Color bbgcolor,
                    final ImageObserver imageObserver) {
        final Color cur_color = getColor();

        setColor(bbgcolor);
        fillRect(sx1, sy1, sx2 - sx1 + 1, sy2 - sy1 + 1);
        setColor(cur_color);

        return drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
            imageObserver);

    }

    public boolean drawImage(final Image image,
                    final AffineTransform transform,
                    final ImageObserver imageObserver) {
        // TODO: Implement
        return false;
    }

    public void drawImage(final BufferedImage image,
                    final BufferedImageOp arg1, final int arg2, final int arg3) {
        // TODO: Implement
    }

    public void drawString(final String text, final float x, final float y) {
        drawString(text, (int) x, (int) y);
    }

    public void drawString(final String text, final int x, final int y) {
        if (text == null) {
            return;
        }

        StringBuilder sb = new StringBuilder(text.length());
        int lastX = x;

        for (int i = 0; i < text.length(); i++) {
            if (text.codePointAt(i) < 256) {
                sb.append(text.charAt(i));
            } else {
                if (sb.length() > 0) {
                    lastX += drawPSString(sb.toString(), lastX, y);
                    sb = new StringBuilder(text.length() - i);
                }

                lastX += drawStringShape(String.valueOf(text.charAt(i)), lastX,
                    y);
            }
        }

        if (sb.length() > 0) {
            drawPSString(sb.toString(), lastX, y);
        }
    }

    public void drawString(final AttributedCharacterIterator iterator,
                    final float x, final float y) {
        drawString(iterator, (int) x, (int) y);
    }

    public void drawString(final AttributedCharacterIterator iterator,
                    final int x, final int y) {
        final int n = iterator.getEndIndex();
        final char[] cc = new char[n];
        int i = 0;

        for (char c = iterator.first(); c != CharacterIterator.DONE; c = iterator.next()) {
            cc[i++] = c;
        }
        drawChars(cc, 0, n, x, y);
    }

    public void drawLine(final int x1, final int y1, final int x2, final int y2) {
        ps(PS.newpath);
        ps(PS.moveto, x1, convY(y1));
        ps(PS.lineto, x2, convY(y2));
        ps(PS.stroke);
    }

    public void drawOval(final int x, final int y, final int width,
                    final int height) {
        drawArc(x, y, width, height, 0, 360, false);
    }

    public void fillOval(final int x, final int y, final int width,
                    final int height) {
        drawArc(x, y, width, height, 0, 360, true);
    }

    public void drawArc(final int x, final int y, final int width,
                    final int height, final int startAngle, final int arcAngle) {
        drawArc(x, y, width, height, startAngle, arcAngle, false);
    }

    public void fillArc(final int x, final int y, final int width,
                    final int height, final int startAngle, final int arcAngle) {
        drawArc(x, y, width, height, startAngle, arcAngle, true);
    }

    public void drawRoundRect(final int x, final int y, final int width,
                    final int height, final int arcWidth, final int arcHeight) {
        drawRoundRect(x, y, width, height, arcWidth, arcHeight, false);
    }

    public void fillRoundRect(final int x, final int y, final int width,
                    final int height, final int arcWidth, final int arcHeight) {
        drawRoundRect(x, y, width, height, arcWidth, arcHeight, true);
    }

    public void drawRect(final int x, final int y, final int width,
                    final int height) {
        int x2 = x + width;
        int y1 = convY(y);
        int y2 = convY(y + height);
        int[] xPoints = { x, x2, x2, x };
        int[] yPoints = { y1, y1, y2, y2 };
        drawPolyline(xPoints, yPoints, 4, true, false);
    }

    public void fillRect(final int x, final int y, final int width,
                    final int height) {
        int x2 = x + width;
        int y1 = convY(y);
        int y2 = convY(y + height);
        int[] xPoints = { x, x2, x2, x };
        int[] yPoints = { y1, y1, y2, y2 };
        drawPolyline(xPoints, yPoints, 4, true, true);
    }

    public void clearRect(final int x, final int y, final int width,
                    final int height) {
        final Color savecolor = getColor();
        setColor(bgColor);
        fillRect(x, y, width, height);
        setColor(savecolor);
    }

    public void drawPolygon(final int[] xPoints, final int[] yPoints,
                    final int nPoints) {
        for (int i = 0; i < nPoints; i++) {
            yPoints[i] = convY(yPoints[i]);
        }
        drawPolyline(xPoints, yPoints, nPoints, true, false);
    }

    public void drawPolyline(final int[] xPoints, final int[] yPoints,
                    final int nPoints) {
        for (int i = 0; i < nPoints; i++) {
            yPoints[i] = convY(yPoints[i]);
        }
        drawPolyline(xPoints, yPoints, nPoints, false, false);
    }

    public void fillPolygon(final int[] xPoints, final int[] yPoints,
                    final int nPoints) {
        for (int i = 0; i < nPoints; i++) {
            yPoints[i] = convY(yPoints[i]);
        }
        drawPolyline(xPoints, yPoints, nPoints, true, true);
    }

    public void draw(final Shape shape) {
        drawShape(shape, false, true);
    }

    public void fill(final Shape shape) {
        drawShape(shape, true, true);
    }

    private void drawShape(final Shape shape, final boolean fill,
                    final boolean stroke) {
        final float[] coords = new float[6];
        final PathIterator pathIterator = shape.getPathIterator((AffineTransform) null);
        float x = 0;
        float y = 0;

        ps(PS.newpath);

        while (!pathIterator.isDone()) {
            switch (pathIterator.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO: {
                ps(PS.moveto, (int) coords[0], convY((int) coords[1]));
                x = coords[0];
                y = coords[1];
                break;
            }
            case PathIterator.SEG_LINETO: {
                ps(PS.lineto, (int) coords[0], convY((int) coords[1]));
                x = coords[0];
                y = coords[1];
                break;
            }
            case PathIterator.SEG_QUADTO: {
                final float x1 = (x + 2 * coords[0]) / 3;
                final float y1 = (y + 2 * coords[1]) / 3;
                final float x2 = (2 * coords[2] + coords[0]) / 3;
                final float y2 = (2 * coords[3] + coords[1]) / 3;

                x = coords[2];
                y = coords[3];
                ps(PS.curveto, x1, convY((int) y1), x2, convY((int) y2), x,
                    convY((int) y));
                break;
            }
            case PathIterator.SEG_CUBICTO: {
                ps(PS.curveto, (int) coords[0], convY((int) coords[1]),
                    (int) coords[2], convY((int) coords[3]), (int) coords[4],
                    convY((int) coords[5]));
                x = coords[4];
                y = coords[5];
                break;
            }
            case PathIterator.SEG_CLOSE: {
                ps(PS.closepath);
                break;
            }
            }
            pathIterator.next();
        }

        if (fill) {
            ps(PS.fill);
        }

        if (stroke) {
            ps(PS.stroke);
        }
    }

    public void setColor(final Color color) {
        super.setColor(color);
        final float[] rgb = fgColor.getRGBColorComponents((float[]) null);
        ps(PS.setcolor, rgb[0], rgb[1], rgb[2]);
    }

    public void setFont(final Font font) {
        // looking for direct mapping of <name>.<style> to PostScript name
        String psName = FONT_MAP.get(font.getName() + "." + font.getStyle()); //$NON-NLS-1$

        if (psName == null) {
            // looking for font name mapping
            final String name = FONT_MAP.get(font.getName());
            if (name != null) {
                psName = PSFont.getPSName(name, font.getStyle());
            }
        }

        if (psName == null) {
            psName = PSFont.Helvetica.psName;
        }

        ps(PS.setfnt, psName, font.getSize());
        super.setFont(font);
    }

    public void translate(final int x, final int y) {
        ps(PS.translate, x, -y);
    }

    public void translate(final double x, final double y) {
        translate((int) x, (int) y);
    }

    public void rotate(final double theta) {
        rotate(theta, 0d, 0d);
    }

    public void rotate(final double theta, final double x, final double y) {
        final double alfa = -theta * 180 / java.lang.Math.PI;
        final int x0 = (int) x;
        final int y0 = convY((int) y);

        ps(PS.translate, x0, y0);
        ps(PS.rotate, alfa);
        ps(PS.translate, -x0, -y0);
    }

    public void scale(final double sx, final double sy) {
        ps(PS.scale, sx, sy);
        yscale = yscale / sy;
    }

    public void setClip(final int x, final int y, final int width,
                    final int height) {
        setClip(new Rectangle(x, y, width, height));
    }

    @Override
    public void setClip(final Shape s) {
        super.setClip(s);
        drawShape(s, false, false);
        ps(PS.clip);
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        // TODO Implement
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        // TODO Implement
        return null;
    }

    @Override
    public void copyArea(int sx, int sy, int width, int height, int dx, int dy) {
        // TODO Implement
    }

    @Override
    public Graphics create() {
        return this;
    }

    @Override
    public void setTransform(final AffineTransform transform) {
        super.setTransform(transform);
        ps(PS.concat, matrix[0], matrix[1], matrix[2], matrix[3], matrix[4],
            matrix[5]);
    }

    private static String wrapString(final String str) {
        return str.replace("\\", "\\\\").replace("\n", "\\\n").replace("\r", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$
            "\\\r").replace("(", "\\(").replace(")", "\\)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    private static String threebytes2Hex(int b) {
        final char[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                        'A', 'B', 'C', 'D', 'E', 'F' };
        final char[] ret = new char[6];

        for (int i = 0; i < 6; i++) {
            ret[5 - i] = hex[b & 0x0F];
            b = b >> 4;
        }
        return new String(ret);
    }

    private void drawImage(final Image image, final int x, final int y) {
        drawImage(image, x, y, false, 0f, 0f);
    }

    /**
     * common private method for image drawing
     */
    private void drawImage(final Image image, final int x, final int y,
                    final boolean scale, final float sx, final float sy) {
        if (image != null) {
            final int imageHeight = image.getHeight(null);
            final int imageWidth = image.getWidth(null);
            final BufferedImage imageGIF = (BufferedImage) image;
            int line = 0;
            int comp;

            ps(PS.translate, x, y);

            if (scale) {
                ps(PS.scale, sx, sy);
            }

            out_stream.print(imageWidth);
            out_stream.println(imageHeight + " 8"); //$NON-NLS-1$

            out_stream.println(" [1 0 0 -1 0 1]"); //$NON-NLS-1$
            out_stream.println("{ currentfile"); //$NON-NLS-1$

            out_stream.println(" 32 string readhexstring pop"); //$NON-NLS-1$
            out_stream.println("}"); //$NON-NLS-1$
            out_stream.println("false 3"); //$NON-NLS-1$
            out_stream.println("colorimage"); //$NON-NLS-1$

            for (int iy = 0; iy < imageHeight; iy++) {
                for (int ix = 0; ix < imageWidth; ix++) {
                    comp = imageGIF.getRGB(ix, iy);
                    out_stream.print(threebytes2Hex(comp));
                    if (line++ == 30) {
                        out_stream.println();
                        line = 0;
                    }
                }
                if (line != 0) {
                    line = 0;
                    out_stream.println();
                }
            }

            if (scale) {
                ps(PS.scale, 1 / sx, 1 / sy);
            }
            ps(PS.translate, -x, -y);
            ps(PS.stroke);
        }
    }

    /**
     * common private method for drawOval, fillOval, drawArc and fillArc
     * methods.
     */
    private void drawArc(final int x, final int y, final int width,
                    final int height, final int startAngle, final int arcAngle,
                    final boolean fill) {
        final int cx = x + width / 2;
        final int cy = convY(y + height / 2);
        final float scale1 = (float) width / (float) height;
        final float scale2 = (float) height / (float) width;

        ps(PS.newpath);
        ps(PS.scale, scale1, 1);
        ps(PS.arc, (cx * scale2), cy, (height / 2), startAngle, arcAngle);
        if (fill) {
            ps(PS.lineto, (cx * scale2), cy);
            ps(PS.fill);
        }
        ps(PS.scale, scale2, 1);
        ps(PS.stroke);
    }

    /**
     * common private method for drawRoundRect and fillRoundRect methods.
     */
    private void drawRoundRect(final int x, final int y, final int width,
                    final int height, final int arcWidth, final int arcHeight,
                    final boolean fill) {

        final int x1 = x + arcWidth;
        final int x2 = x + width - arcWidth;
        final int y1 = convY(y + arcHeight);
        final int y2 = convY(y + height - arcHeight);
        final float scale1 = (float) arcWidth / (float) arcHeight;
        final float scale2 = (float) arcHeight / (float) arcWidth;

        ps(PS.newpath);
        ps(PS.moveto, x, y1);
        ps(PS.scale, scale1, 1);
        ps(PS.arc, (x1 * scale2), y2, arcHeight, 180, 270);
        ps(PS.arc, (x2 * scale2), y2, arcHeight, 270, 0);
        ps(PS.arc, (x2 * scale2), y1, arcHeight, 0, 90);
        ps(PS.arc, (x1 * scale2), y1, arcHeight, 90, 180);
        ps(PS.scale, scale2, 1);

        if (fill) {
            ps(PS.fill);
        }
        ps(PS.stroke);
    }

    /**
     * common private method for drawPolyline, drawPolygon, drawRect, clearRect,
     * fillPolyline, fillPolygon and fillRect methods.
     */
    private void drawPolyline(final int[] xPoints, final int[] yPoints,
                    final int nPoints, final boolean close, final boolean fill) {
        ps(PS.moveto, xPoints[0], yPoints[0]);

        for (int i = 1; i < nPoints; i++) {
            ps(PS.lineto, xPoints[i], yPoints[i]);
        }
        if (close) {
            ps(PS.closepath);
        }
        if (fill) {
            ps(PS.fill);
        }
        ps(PS.stroke);
    }

    private int drawPSString(final String text, final int x, final int y) {
        final Rectangle2D r = font.getStringBounds(text, frc);
        final double w = r.getWidth();

        ps(PS.show, "(" + wrapString(text) + ")", w, x, convY(y)); //$NON-NLS-1$ //$NON-NLS-2$
        return (int) w;
    }

    private int drawStringShape(final String str, final int x, final int y) {
        final TextLayout l = new TextLayout(str, font, frc);

        drawShape(l.getOutline(AffineTransform.getTranslateInstance(x, y)),
            true, true);
        return (int) font.getStringBounds(str, frc).getWidth();
    }

    /**
     * Generates PostScript procedure call with the specified arguments.
     * 
     * @param ps procedure name
     * @param args procedure arguments
     */
    private void ps(final PS ps, final Object... args) {
        ps.print(out_stream, args);
    }

    private int convY(final int y) {
        return (int) (format.getHeight() * yscale) - y;
    }

    private void resetGraphics() {
        super.setTransform(new AffineTransform());
        super.setClip(defaultClip);
        super.setFont(DEF_FONT);
        super.setColor(Color.BLACK);
        super.setBackground(Color.WHITE);
    }

    private enum PS {
            arc,
            clip,
            closepath,
            curveto,
            def,
            exch,
            fill,
            grestore,
            gsave,
            lineto,
            moveto,
            newpath,
            rlineto,
            rmoveto,
            rotate,
            scale,
            scalefont,
            setfont,
            setlinewidth,
            show(null, "%s %s %s %s S"), //$NON-NLS-1$
            showpage,
            stroke,
            translate,
            comment(null, "%%%%%s"), //$NON-NLS-1$
            concat(null, "[%s %s %s %s %s %s] concat"), //$NON-NLS-1$
            setcolor("C", null), //$NON-NLS-1$
            setfnt(null, "/%s %s F"), //$NON-NLS-1$
            setDefGstate(null, "/DEF_GSTATE gstate def"), //$NON-NLS-1$
            restoreDefGstate(null, "DEF_GSTATE setgstate"); //$NON-NLS-1$

        final String name;
        final String format;

        PS() {
            this(null, null);
        }

        PS(final String name, final String format) {
            this.name = (name != null) ? name : name();
            this.format = format;
        }

        static void printHeader(final PrintStream out) {
            out.println("%!PS-Adobe-3"); //$NON-NLS-1$
            out.println("%%Title: G2D generated document"); //$NON-NLS-1$
            out.println("%%Creator: Apache Harmony"); //$NON-NLS-1$
            out.println("%%CreationDate: " + new Date()); //$NON-NLS-1$
            out.println("%%EndComments"); //$NON-NLS-1$
            out.println("/F {exch findfont exch scalefont setfont} def"); //$NON-NLS-1$
            out.println("/C {setrgbcolor} bind def"); //$NON-NLS-1$
            out.println("/S {gsave moveto 1 index stringwidth pop div 1 scale " //$NON-NLS-1$
                + "show grestore} def"); //$NON-NLS-1$
        }

        static void printFooter(final PrintStream out) {
            out.println("%%EOF"); //$NON-NLS-1$
        }

        void print(final PrintStream out, final Object... args) {
            if (format != null) {
                out.printf(format, args);
                out.println();
            } else {
                for (Object arg : args) {
                    out.print(arg + " "); //$NON-NLS-1$
                }
                out.println(name);
            }
        }
    }

    private enum PSFont {
            Times_Roman("Times", null, Font.PLAIN), //$NON-NLS-1$
            Times_Italic("Times", null, Font.ITALIC), //$NON-NLS-1$
            Times_Bold("Times", null, Font.BOLD), //$NON-NLS-1$
            Times_BoldItalic("Times", null, Font.BOLD + Font.ITALIC), //$NON-NLS-1$
            Helvetica("Helvetica", null, Font.PLAIN), //$NON-NLS-1$
            Helvetica_Oblique("Helvetica", null, Font.ITALIC), //$NON-NLS-1$
            Helvetica_Bold("Helvetica", null, Font.BOLD), //$NON-NLS-1$
            Helvetica_BoldOblique("Helvetica", null, Font.BOLD + Font.ITALIC), //$NON-NLS-1$
            Courier("Courier", null, Font.PLAIN), //$NON-NLS-1$
            Courier_Oblique("Courier", null, Font.ITALIC), //$NON-NLS-1$
            Courier_Bold("Courier", null, Font.BOLD), //$NON-NLS-1$
            Courier_BoldOblique("Courier", null, Font.BOLD + Font.ITALIC); //$NON-NLS-1$

        final String name;
        final String psName;
        final int    style;

        PSFont(final String name, final String psName, final int style) {
            this.name = name;
            this.psName = (psName != null) ? psName : name().replace('_', '-');
            this.style = style;
        }

        static String getPSName(final String name, final int style) {
            for (PSFont f : values()) {
                if ((f.style == style) && f.name.equalsIgnoreCase(name)) {
                    return f.psName;
                }
            }
            return null;
        }
    }
}
