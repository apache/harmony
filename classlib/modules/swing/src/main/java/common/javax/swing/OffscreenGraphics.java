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
 * @author Anton Avtamonov
 */
package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

class OffscreenGraphics extends Graphics {
    private final Graphics offscreenGraphics;
    private final Image offscreenImage;

    public OffscreenGraphics(final Image image) {
        offscreenImage = image;
        offscreenGraphics = offscreenImage.getGraphics();
    }

    public Image getImage() {
        return offscreenImage;
    }

    public void setColor(final Color color) {
        offscreenGraphics.setColor(color);
    }

    public Graphics create() {
        return new OffscreenGraphics(offscreenImage);
    }

    public void dispose() {
        offscreenGraphics.dispose();
    }

    public Shape getClip() {
        return offscreenGraphics.getClip();
    }

    public Font getFont() {
        return offscreenGraphics.getFont();
    }

    public FontMetrics getFontMetrics(final Font font) {
        return offscreenGraphics.getFontMetrics(font);
    }

    public void setClip(final Shape clip) {
        offscreenGraphics.setClip(clip);
    }

    public void setClip(final int x, final int y, final int width, final int height) {
        offscreenGraphics.setClip(x, y, width, height);
    }

    public void setFont(final Font font) {
        offscreenGraphics.setFont(font);
    }

    public void translate(final int x, final int y) {
        offscreenGraphics.translate(x, y);
    }

    public void clearRect(final int x, final int y, final int width, final int height) {
        offscreenGraphics.clearRect(x, y, width, height);
    }

    public boolean drawImage(final Image img, final int dx1, final int dy1, final int dx2, final int dy2, final int sx1, final int sy1, final int sx2, final int sy2, final Color bgcolor, final ImageObserver observer) {
        return offscreenGraphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    public boolean drawImage(final Image img, final int x, final int y, final ImageObserver observer) {
        return offscreenGraphics.drawImage(img, x, y, observer);
    }

    public boolean drawImage(final Image img, final int x, final int y, final int width, final int height, final ImageObserver observer) {
        return offscreenGraphics.drawImage(img, x, y, width, height, observer);
    }

    public boolean drawImage(final Image img, final int dx1, final int dy1, final int dx2, final int dy2, final int sx1, final int sy1, final int sx2, final int sy2, final ImageObserver observer) {
        return offscreenGraphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    public boolean drawImage(final Image img, final int x, final int y, final int width, final int height, final Color bgcolor, final ImageObserver observer) {
        return offscreenGraphics.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    public boolean drawImage(final Image img, final int x, final int y, final Color bgcolor, final ImageObserver observer) {
        return offscreenGraphics.drawImage(img, x, y, bgcolor, observer);
    }

    public void fillRect(final int x, final int y, final int width, final int height) {
        offscreenGraphics.fillRect(x, y, width, height);
    }

    public void clipRect(final int x, final int y, final int width, final int height) {
        offscreenGraphics.clipRect(x, y, width, height);
    }

    public void copyArea(final int x, final int y, final int width, final int height, final int dx, final int dy) {
        offscreenGraphics.copyArea(x, y, width, height, dx, dy);
    }

    public void drawArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
        offscreenGraphics.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawLine(final int x1, final int y1, final int x2, final int y2) {
        offscreenGraphics.drawLine(x1, y1, x2, y2);
    }

    public void drawOval(final int x, final int y, final int width, final int height) {
        offscreenGraphics.drawOval(x, y, width, height);
    }

    public void drawPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
        offscreenGraphics.drawPolygon(xPoints, yPoints, nPoints);
    }

    public void drawPolyline(final int[] xPoints, final int[] yPoints, final int nPoints) {
        offscreenGraphics.drawPolyline(xPoints, yPoints, nPoints);
    }

    public void drawRoundRect(final int x, final int y, final int width, final int height, final int arcWidth, final int arcHeight) {
        offscreenGraphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void drawString(final String str, final int x, final int y) {
        offscreenGraphics.drawString(str, x, y);
    }

    public void drawString(final AttributedCharacterIterator iterator, final int x, final int y) {
        offscreenGraphics.drawString(iterator, x, y);
    }

    public void fillArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
        offscreenGraphics.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void fillOval(final int x, final int y, final int width, final int height) {
        offscreenGraphics.fillOval(x, y, width, height);
    }

    public void fillPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
        offscreenGraphics.fillPolygon(xPoints, yPoints, nPoints);
    }

    public void fillRoundRect(final int x, final int y, final int width, final int height, final int arcWidth, final int arcHeight) {
        offscreenGraphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public Rectangle getClipBounds() {
        return offscreenGraphics.getClipBounds();
    }

    public Color getColor() {
        return offscreenGraphics.getColor();
    }

    public void setPaintMode() {
        offscreenGraphics.setPaintMode();
    }

    public void setXORMode(final Color color) {
        offscreenGraphics.setXORMode(color);
    }
}
