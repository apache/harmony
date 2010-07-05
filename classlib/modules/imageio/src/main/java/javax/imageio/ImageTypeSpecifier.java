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

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

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
                                                       boolean isAlphaPremultiplied) {
        if (colorSpace == null) {
            throw new IllegalArgumentException();            
        }
        
        if (bandOffsets == null) {
            throw new IllegalArgumentException();
        }
        
        if (dataType != DataBuffer.TYPE_BYTE &&
            dataType != DataBuffer.TYPE_DOUBLE &&
            dataType != DataBuffer.TYPE_FLOAT &&
            dataType != DataBuffer.TYPE_INT &&
            dataType != DataBuffer.TYPE_SHORT &&
            dataType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException();
        }
        
        int numComponents = colorSpace.getNumComponents();
        if (hasAlpha) {
            numComponents++;
        }
        if (bandOffsets.length != numComponents) {
            throw new IllegalArgumentException();
        }
        
        int transparency = hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE;
        int[] bits = new int[numComponents];
        
        for (int i = 0; i < numComponents; i++) {
            bits[i] = DataBuffer.getDataTypeSize(dataType);
        }
        
        ColorModel colorModel = new ComponentColorModel(colorSpace, 
                                                        bits, 
                                                        hasAlpha, 
                                                        isAlphaPremultiplied,
                                                        transparency,
                                                        dataType);
        
        int minBandOffset = bandOffsets[0];
        int maxBandOffset = bandOffsets[0];
        for (int i = 0; i < bandOffsets.length; i++) {
            if (minBandOffset > bandOffsets[i]) {
                minBandOffset = bandOffsets[i];
            }
            if (maxBandOffset < bandOffsets[i]) {
                maxBandOffset = bandOffsets[i];
            }
        }
        int pixelStride = maxBandOffset - minBandOffset + 1;
        
        SampleModel sampleModel = new PixelInterleavedSampleModel(dataType, 
                                                                  1,
                                                                  1,
                                                                  pixelStride, 
                                                                  pixelStride, 
                                                                  bandOffsets);
               
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }


    public static ImageTypeSpecifier createBanded(ColorSpace colorSpace,
                                                  int[] bankIndices,
                                                  int[] bandOffsets,
                                                  int dataType,
                                                  boolean hasAlpha,
                                                  boolean isAlphaPremultiplied) {
        if (colorSpace == null) {
            throw new IllegalArgumentException();
        }
        
        if (bankIndices == null) {
            throw new IllegalArgumentException();
        }
        
        if (bandOffsets == null) {
            throw new IllegalArgumentException();
        }
        
        if (bankIndices.length != bandOffsets.length) {
            throw new IllegalArgumentException();
        }
        
        int numComponents = colorSpace.getNumComponents();
        if (hasAlpha) {
            numComponents++;
        }
        if (bandOffsets.length != numComponents) {
            throw new IllegalArgumentException();
        }
        
        if (dataType != DataBuffer.TYPE_BYTE &&
            dataType != DataBuffer.TYPE_DOUBLE &&
            dataType != DataBuffer.TYPE_FLOAT &&
            dataType != DataBuffer.TYPE_INT &&
            dataType != DataBuffer.TYPE_SHORT &&
            dataType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException();
        }
        
        int[] bits = new int[numComponents];
        for (int i = 0; i < numComponents; i++) {
            bits[i] = DataBuffer.getDataTypeSize(dataType);
        }
        int transparency = hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE;
        
        ColorModel colorModel = new ComponentColorModel(colorSpace,
                                                        bits,
                                                        hasAlpha,
                                                        isAlphaPremultiplied,
                                                        transparency,
                                                        dataType);
        
        SampleModel sampleModel = new BandedSampleModel(dataType,
                                                        1,
                                                        1,
                                                        1,
                                                        bankIndices,
                                                        bandOffsets);
        
        return new ImageTypeSpecifier(colorModel, sampleModel);
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
                                                   int dataType) {
       if ((redLUT == null) || (greenLUT == null) || blueLUT == null) {
           throw new IllegalArgumentException();
       }
       
       if ((bits != 1) && (bits != 2) && (bits != 4) && (bits != 8) && (bits != 16)) {
           throw new IllegalArgumentException();
       }
       
       int length = 1 << bits;
       if ((redLUT.length != length) || (greenLUT.length != length) || (blueLUT.length != length) ||
               (alphaLUT != null && alphaLUT.length != length)) {
           throw new IllegalArgumentException();
       }
       
       if ((dataType != DataBuffer.TYPE_BYTE) && (dataType != DataBuffer.TYPE_SHORT) &&
               (dataType != DataBuffer.TYPE_USHORT) && (dataType != DataBuffer.TYPE_INT)) {
           throw new IllegalArgumentException();
       }
       
       if ((bits > 8 && dataType == DataBuffer.TYPE_BYTE) || 
               (bits > 16 && dataType == DataBuffer.TYPE_INT)) {
           throw new IllegalArgumentException();
       }
       
       ColorModel model = null;
       int size = redLUT.length;
       if (alphaLUT == null) {
           model = new IndexColorModel(bits, size, redLUT, greenLUT, blueLUT);
       } else {
           model = new IndexColorModel(bits, size, redLUT, greenLUT, blueLUT, alphaLUT);
       }
       
       return new ImageTypeSpecifier(model, model.createCompatibleSampleModel(1, 1));
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

    public int getBufferedImageType() {
        BufferedImage bufferedImage = createBufferedImage(1, 1);
        return bufferedImage.getType();
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
            throw new IllegalArgumentException(Messages.getString("imageio.28")); //$NON-NLS-1$
        }
        return sampleModel.createCompatibleSampleModel(width, height);
    }

    public ColorModel getColorModel() {
        return colorModel;
    }

    public BufferedImage createBufferedImage(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException();
        }
        
        if ((long)width*height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        
        SampleModel sm = sampleModel.createCompatibleSampleModel(width, height);
        WritableRaster writableRaster = Raster.createWritableRaster(sm, new Point(0, 0)); 
        
        return new BufferedImage(colorModel, writableRaster, colorModel.isAlphaPremultiplied(), new Hashtable());
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