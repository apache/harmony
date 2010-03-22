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

import java.awt.Point;
import java.awt.Rectangle;

import org.apache.harmony.awt.internal.nls.Messages;

public class WritableRaster extends Raster {

    protected WritableRaster(SampleModel sampleModel, DataBuffer dataBuffer,
            Rectangle aRegion, Point sampleModelTranslate,
            WritableRaster parent) {
        super(sampleModel, dataBuffer, aRegion, sampleModelTranslate, parent);
    }

    protected WritableRaster(SampleModel sampleModel, DataBuffer dataBuffer,
            Point origin) {
        this(sampleModel, dataBuffer, new Rectangle(origin.x, origin.y,
                sampleModel.width, sampleModel.height), origin, null);
    }

    protected WritableRaster(SampleModel sampleModel, Point origin) {
        this(sampleModel, sampleModel.createDataBuffer(), new Rectangle(
                origin.x, origin.y, sampleModel.width, sampleModel.height),
                origin, null);
    }

    public void setDataElements(int x, int y, Object inData) {
        sampleModel.setDataElements(x - sampleModelTranslateX,
                y - sampleModelTranslateY, inData, dataBuffer);
    }

    public void setDataElements(int x, int y, int w, int h, Object inData) {
        sampleModel.setDataElements(x - sampleModelTranslateX,
                y - sampleModelTranslateY, w, h, inData, dataBuffer);
    }

    public WritableRaster createWritableChild(int parentX, int parentY, int w,
            int h, int childMinX, int childMinY, int bandList[]) {
        if (w <= 0 || h <= 0) {
            // awt.244=Width or Height of child Raster is less than or equal to zero
            throw new RasterFormatException(Messages.getString("awt.244")); //$NON-NLS-1$
        }

        if (parentX < this.minX || parentX + w > this.minX + this.width) {
            // awt.245=parentX disposes outside Raster
            throw new RasterFormatException(Messages.getString("awt.245")); //$NON-NLS-1$
        }

        if (parentY < this.minY || parentY + h > this.minY + this.height) {
            // awt.246=parentY disposes outside Raster
            throw new RasterFormatException(Messages.getString("awt.246")); //$NON-NLS-1$
        }

        if ((long) parentX + w > Integer.MAX_VALUE) {
            // awt.247=parentX + w results in integer overflow
            throw new RasterFormatException(Messages.getString("awt.247")); //$NON-NLS-1$
        }

        if ((long) parentY + h > Integer.MAX_VALUE) {
            // awt.248=parentY + h results in integer overflow
            throw new RasterFormatException(Messages.getString("awt.248")); //$NON-NLS-1$
        }

        if ((long) childMinX + w > Integer.MAX_VALUE) {
            // awt.249=childMinX + w results in integer overflow
            throw new RasterFormatException(Messages.getString("awt.249")); //$NON-NLS-1$
        }

        if ((long) childMinY + h > Integer.MAX_VALUE) {
            // awt.24A=childMinY + h results in integer overflow
            throw new RasterFormatException(Messages.getString("awt.24A")); //$NON-NLS-1$
        }

        SampleModel childModel;

        if (bandList == null) {
            childModel = sampleModel;
        } else {
            childModel = sampleModel.createSubsetSampleModel(bandList);
        }

        int childTranslateX = childMinX - parentX;
        int childTranslateY = childMinY - parentY;

        return new WritableRaster(childModel, dataBuffer,
                new Rectangle(childMinX, childMinY, w, h),
                new Point(childTranslateX + sampleModelTranslateX,
                        childTranslateY + sampleModelTranslateY),
                this);
    }

    public WritableRaster createWritableTranslatedChild(int childMinX,
            int childMinY) {
        return createWritableChild(minX, minY, width, height, childMinX,
                childMinY, null);
    }

    public WritableRaster getWritableParent() {
        return (WritableRaster) parent;
    }

    public void setRect(Raster srcRaster) {
        setRect(0, 0, srcRaster);
    }

    public void setRect(int dx, int dy, Raster srcRaster) {
        int w = srcRaster.getWidth();
        int h = srcRaster.getHeight();

        int srcX = srcRaster.getMinX();
        int srcY = srcRaster.getMinY();

        int dstX = srcX + dx;
        int dstY = srcY + dy;

        if (dstX < this.minX) {
            int minOffX = this.minX - dstX;
            w -= minOffX;
            dstX = this.minX;
            srcX += minOffX;
        }

        if (dstY < this.minY) {
            int minOffY = this.minY - dstY;
            h -= minOffY;
            dstY = this.minY;
            srcY += minOffY;
        }

        if (dstX + w > this.minX + this.width) {
            int maxOffX = (dstX + w) - (this.minX + this.width);
            w -= maxOffX;
        }

        if (dstY + h > this.minY + this.height) {
            int maxOffY = (dstY + h) - (this.minY + this.height);
            h -= maxOffY;
        }

        if (w <= 0 || h <= 0) {
            return;
        }

        switch (sampleModel.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_INT:
            int iPixelsLine[] = null;
            for (int i = 0; i < h; i++) {
                iPixelsLine = srcRaster.getPixels(srcX, srcY + i, w, 1,
                        iPixelsLine);
                setPixels(dstX, dstY + i, w, 1, iPixelsLine);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float fPixelsLine[] = null;
            for (int i = 0; i < h; i++) {
                fPixelsLine = srcRaster.getPixels(srcX, srcY + i, w, 1,
                        fPixelsLine);
                setPixels(dstX, dstY + i, w, 1, fPixelsLine);
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double dPixelsLine[] = null;
            for (int i = 0; i < h; i++) {
                dPixelsLine = srcRaster.getPixels(srcX, srcY + i, w, 1,
                        dPixelsLine);
                setPixels(dstX, dstY + i, w, 1, dPixelsLine);
            }
            break;
        }
    }

    public void setDataElements(int x, int y, Raster inRaster) {
        int dstX = x + inRaster.getMinX();
        int dstY = y + inRaster.getMinY();

        int w = inRaster.getWidth();
        int h = inRaster.getHeight();

        if (dstX < this.minX || dstX + w > this.minX + this.width ||
                dstY < this.minY || dstY + h > this.minY + this.height) {
            // awt.63=Coordinates are not in bounds
            throw new ArrayIndexOutOfBoundsException(Messages.getString("awt.63")); //$NON-NLS-1$
        }

        int srcX = inRaster.getMinX();
        int srcY = inRaster.getMinY();
        Object line = null;

        for (int i = 0; i < h; i++) {
            line = inRaster.getDataElements(srcX, srcY + i, w, 1, line);
            setDataElements(dstX, dstY + i, w, 1, line);
        }
    }

    public void setPixel(int x, int y, int iArray[]) {
        sampleModel.setPixel(x - sampleModelTranslateX,
                y - sampleModelTranslateY, iArray, dataBuffer);
    }

    public void setPixel(int x, int y, float fArray[]) {
        sampleModel.setPixel(x - sampleModelTranslateX,
                y - sampleModelTranslateY, fArray, dataBuffer);
    }

    public void setPixel(int x, int y, double dArray[]) {
        sampleModel.setPixel(x - sampleModelTranslateX,
                y - sampleModelTranslateY, dArray, dataBuffer);
    }

    public void setPixels(int x, int y, int w, int h, int iArray[]) {
        sampleModel.setPixels(x - sampleModelTranslateX,
                y - sampleModelTranslateY, w, h, iArray, dataBuffer);
    }

    public void setPixels(int x, int y, int w, int h, float fArray[]) {
        sampleModel.setPixels(x - sampleModelTranslateX,
                y - sampleModelTranslateY, w, h, fArray, dataBuffer);
    }

    public void setPixels(int x, int y, int w, int h, double dArray[]) {
        sampleModel.setPixels(x - sampleModelTranslateX,
                y - sampleModelTranslateY, w, h, dArray, dataBuffer);
    }

    public void setSamples(int x, int y, int w, int h, int b, int iArray[]) {
        sampleModel.setSamples(x - sampleModelTranslateX,
                y - sampleModelTranslateY, w, h, b, iArray, dataBuffer);
    }

    public void setSamples(int x, int y, int w, int h, int b, float fArray[]) {
        sampleModel.setSamples(x - sampleModelTranslateX,
                y - sampleModelTranslateY, w, h, b, fArray, dataBuffer);
    }

    public void setSamples(int x, int y, int w, int h, int b, double dArray[]) {
        sampleModel.setSamples(x - sampleModelTranslateX,
                y - sampleModelTranslateY, w, h, b, dArray, dataBuffer);
    }

    public void setSample(int x, int y, int b, int s) {
        sampleModel.setSample(x - sampleModelTranslateX,
                y - sampleModelTranslateY, b, s, dataBuffer);
    }

    public void setSample(int x, int y, int b, float s) {
        sampleModel.setSample(x - sampleModelTranslateX,
                y - sampleModelTranslateY, b, s, dataBuffer);
    }

    public void setSample(int x, int y, int b, double s) {
        sampleModel.setSample(x - sampleModelTranslateX,
                y - sampleModelTranslateY, b, s, dataBuffer);
    }

}

