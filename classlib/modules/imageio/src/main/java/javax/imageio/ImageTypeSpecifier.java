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

package javax.imageio;

import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.color.ColorSpace;

import org.apache.harmony.luni.util.NotImplementedException;
import org.apache.harmony.x.imageio.internal.nls.Messages;

public class ImageTypeSpecifier {
    
    protected ColorModel colorModel;
    protected SampleModel sampleModel;

    public ImageTypeSpecifier(ColorModel colorModel, SampleModel sampleModel) {
        if (colorModel == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.24"));
        }
        if (sampleModel == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.25"));
        }
        if (!colorModel.isCompatibleSampleModel(sampleModel)) {
            throw new IllegalArgumentException(Messages.getString("imageio.26"));
        }

        this.colorModel = colorModel;
        this.sampleModel = sampleModel;
    }

    public ImageTypeSpecifier(RenderedImage renderedImage) {
        if (renderedImage == null) {
            throw new IllegalArgumentException(Messages.getString("imageio.27"));
        }
        this.colorModel = renderedImage.getColorModel();
        this.sampleModel = renderedImage.getSampleModel();
    }

    public static ImageTypeSpecifier createPacked(final ColorSpace colorSpace,
                    final int redMask, final int greenMask, final int blueMask,
                    final int alphaMask, final int transferType,
                    final boolean isAlphaPremultiplied) {
        final ColorModel model = new DirectColorModel(colorSpace, 32, redMask,
                        greenMask, blueMask, alphaMask, isAlphaPremultiplied,
                        transferType);
        return new ImageTypeSpecifier(model, model.createCompatibleSampleModel(
            1, 1));
    }

    public static ImageTypeSpecifier createInterleaved(ColorSpace colorSpace,
                                                       int[] bandOffsets,
                                                       int dataType,
                                                       boolean hasAlpha,
                                                       boolean isAlphaPremultiplied) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }


    public static ImageTypeSpecifier createBanded(ColorSpace colorSpace,
                                                  int[] bankIndices,
                                                  int[] bandOffsets,
                                                  int dataType,
                                                  boolean hasAlpha,
                                                  boolean isAlphaPremultiplied) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public static ImageTypeSpecifier createGrayscale(int bits,
            int dataType,
            boolean isSigned) {
        return createGrayscale(bits, dataType, isSigned, false, false);
    }

    public static ImageTypeSpecifier createGrayscale(int bits,
            int dataType,
            boolean isSigned,
            boolean isAlphaPremultiplied) {
        return createGrayscale(bits, dataType, isSigned, true, isAlphaPremultiplied);
    }

    private static ImageTypeSpecifier createGrayscale(int bits,
             int dataType,
             boolean isSigned,
             boolean hasAlpha,
             boolean isAlphaPremultiplied) {

        if ((bits != 1) && (bits != 2) && (bits != 4) && (bits != 8) && (bits != 16)) {
            throw new IllegalArgumentException();
        }

        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);

        int numComponent = hasAlpha ? 2 : 1;
        int numBits[] = new int[numComponent];
        numBits[0] = bits;
        if (numComponent ==2) {
            numBits[1] = bits;
        }
        int transparency = hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE;
        ColorModel model = new ComponentColorModel(colorSpace, numBits, hasAlpha, isAlphaPremultiplied, transparency, dataType);

        return new ImageTypeSpecifier(model, model.createCompatibleSampleModel(1, 1));
    }


    public static ImageTypeSpecifier createIndexed(byte[] redLUT,
                                                   byte[] greenLUT,
                                                   byte[] blueLUT,
                                                   byte[] alphaLUT,
                                                   int bits,
                                                   int dataType) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public static ImageTypeSpecifier createFromBufferedImageType(int bufferedImageType) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public static ImageTypeSpecifier createFromRenderedImage(RenderedImage image) {
        if (null == image) {
            throw new IllegalArgumentException(Messages.getString("imageio.27"));
        }
        return new ImageTypeSpecifier(image);
    }

    public int getBufferedImageType() throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    public int getNumComponents() {
        return colorModel.getNumComponents();
    }

    public int getNumBands() {
        return sampleModel.getNumBands();
    }

    public int getBitsPerBand(int band) {
        if (band < 0 || band >= getNumBands()) {
            throw new IllegalArgumentException();
        }
        return sampleModel.getSampleSize(band);
    }

    public SampleModel getSampleModel() {
        return sampleModel;
    }

    public SampleModel getSampleModel(int width, int height) {
        if ((long)width*height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(Messages.getString("imageio.28"));
        }
        return sampleModel.createCompatibleSampleModel(width, height);
    }

    public ColorModel getColorModel() {
        return colorModel;
    }

    public BufferedImage createBufferedImage(int width, int height) throws NotImplementedException {
        // TODO: implement
        throw new NotImplementedException();
    }

    @Override
    public boolean equals(Object o) {
        boolean rt = false;
        if (o instanceof ImageTypeSpecifier) {
            ImageTypeSpecifier ts = (ImageTypeSpecifier) o;
            rt = colorModel.equals(ts.colorModel) && sampleModel.equals(ts.sampleModel);
        }
        return rt;
    }

    @Override
    public int hashCode() {
        return colorModel.hashCode() + sampleModel.hashCode();
    }
}