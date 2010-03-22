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
 * @author Viskov Nikolay
 */
package org.apache.harmony.x.imageio.plugins.png;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.apache.harmony.x.imageio.internal.nls.Messages;

import org.apache.harmony.luni.util.NotImplementedException;

public class PNGImageWriter extends ImageWriter {
    private static int[][] BAND_OFFSETS = {
            {}, {
                0 }, {
                    0, 1 }, {
                    0, 1, 2 }, {
                    0, 1, 2, 3 } };

    // Each pixel is a grayscale sample.
    private static final int PNG_COLOR_TYPE_GRAY = 0;
    // Each pixel is an R,G,B triple.
    private static final int PNG_COLOR_TYPE_RGB = 2;
    // Each pixel is a palette index, a PLTE chunk must appear.
    private static final int PNG_COLOR_TYPE_PLTE = 3;
    // Each pixel is a grayscale sample, followed by an alpha sample.
    private static final int PNG_COLOR_TYPE_GRAY_ALPHA = 4;
    // Each pixel is an R,G,B triple, followed by an alpha sample.
    private static final int PNG_COLOR_TYPE_RGBA = 6;

    private static native void initIDs(Class<ImageOutputStream> iosClass);

    static {
        System.loadLibrary("pngencoder"); //$NON-NLS-1$
        initIDs(ImageOutputStream.class);
    }

    private native int encode(byte[] input, int bytesInBuffer, int bytePixelSize, Object ios, int imageWidth,
            int imageHeight, int bitDepth, int colorType, int[] palette, int i, boolean b);

    protected PNGImageWriter(ImageWriterSpi iwSpi) {
        super(iwSpi);
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata arg0, ImageWriteParam arg1) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata arg0, ImageTypeSpecifier arg1, ImageWriteParam arg2) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier arg0, ImageWriteParam arg1) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam arg0) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage iioimage, ImageWriteParam param) throws IOException {
        if (output == null) {
            throw new IllegalStateException("Output not been set");
        }
        if (iioimage == null) {
            throw new IllegalArgumentException("Image equals null");
        }
        if (iioimage.hasRaster() && !canWriteRasters()) {
            throw new UnsupportedOperationException("Can't write raster");
        }// ImageOutputStreamImpl

        int imageWidth, imageHeight;
        int colorType = PNG_COLOR_TYPE_GRAY;
        int bitDepth;
        int numBands;

        DataBufferByte dbuffer;

        int[] palette = null;

        boolean isInterlace = true;

        RenderedImage image = iioimage.getRenderedImage();

        imageWidth = image.getWidth();
        imageHeight = image.getHeight();

        numBands = image.getSampleModel().getNumBands();

        ColorModel colorModel = image.getColorModel();

        int pixelSize = colorModel.getPixelSize();

        int bytePixelSize = pixelSize / 8;

        bitDepth = pixelSize / numBands;

        // byte per band
        int bpb = bitDepth > 8 ? 2 : 1;

        if (colorModel instanceof IndexColorModel) {
            if (bitDepth != 1 && bitDepth != 2 && bitDepth != 4 && bitDepth != 8) {
//              Wrong bitDepth-numBands composition
                throw new IllegalArgumentException(Messages.getString("imageio.1"));//$NON-NLS-1$
            }
            if (numBands != 1) {
//              Wrong bitDepth-numBands composition
                throw new IllegalArgumentException(Messages.getString("imageio.1"));//$NON-NLS-1$
            }

            IndexColorModel icm = (IndexColorModel) colorModel;

            palette = new int[icm.getMapSize()];

            icm.getRGBs(palette);

            colorType = PNG_COLOR_TYPE_PLTE;

        }
        else if (numBands == 1) {
            if (bitDepth != 1 && bitDepth != 2 && bitDepth != 4 && bitDepth != 8 && bitDepth != 16) {
//              Wrong bitDepth-numBands composition
                throw new IllegalArgumentException(Messages.getString("imageio.1"));//$NON-NLS-1$
            }
            colorType = PNG_COLOR_TYPE_GRAY;
        }
        else if (numBands == 2) {
            if (bitDepth != 8 && bitDepth != 16) {
//              Wrong bitDepth-numBands composition
                throw new IllegalArgumentException(Messages.getString("imageio.1"));//$NON-NLS-1$
            }
            colorType = PNG_COLOR_TYPE_GRAY_ALPHA;
        }
        else if (numBands == 3) {
            if (bitDepth != 8 && bitDepth != 16) {
//              Wrong bitDepth-numBands composition
                throw new IllegalArgumentException(Messages.getString("imageio.1")); //$NON-NLS-1$
            }
            colorType = PNG_COLOR_TYPE_RGB;
        }
        else if (numBands == 4) {
            if (bitDepth != 8 && bitDepth != 16) {
                //Wrong bitDepth-numBands composition
                throw new IllegalArgumentException(Messages.getString("imageio.1")); //$NON-NLS-1$
            }
            colorType = PNG_COLOR_TYPE_RGBA;
        }

        int dbufferLength = bytePixelSize * imageHeight * imageWidth;

        dbuffer = new DataBufferByte(dbufferLength);

        WritableRaster scanRaster = Raster.createInterleavedRaster(dbuffer, imageWidth, imageHeight, bpb * numBands
                * imageWidth, bpb * numBands, BAND_OFFSETS[numBands], null);

        scanRaster.setRect(((BufferedImage) image).getRaster()// image.getData()
                .createChild(0, 0, imageWidth, imageHeight, 0, 0, null));

        if (param instanceof PNGImageWriterParam) {
            isInterlace = ((PNGImageWriterParam) param).getInterlace();
        }

        try {
            encode(dbuffer.getData(), dbufferLength, bytePixelSize, (ImageOutputStream) getOutput(), imageWidth,
                    imageHeight, bitDepth, colorType, palette, palette == null ? 0 : palette.length, isInterlace);

        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public ImageWriteParam getDefaultWriteParam() {
        return new PNGImageWriterParam();
    }
}
