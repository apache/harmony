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

import static org.junit.Assert.assertArrayEquals;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;

import junit.framework.TestCase;

public class ImageTypeSpecifierTest extends TestCase {
    public void testCreateFromBufferedImageType() {
        for (int i = BufferedImage.TYPE_INT_RGB; i <= BufferedImage.TYPE_BYTE_INDEXED; i ++ ) {
            BufferedImage bi = new BufferedImage(1, 1, i);
            ColorModel expected = bi.getColorModel();
            
            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(i);
            ColorModel actual = typeSpecifier.getColorModel();
            String msg = "Failed to create correct ImageTypeSpecifier, bufferedImageType = " + Integer.toString(i);
            assertEquals(msg, expected.getClass(), actual.getClass());
            assertEquals(msg, expected.getColorSpace(), actual.getColorSpace());
            assertEquals(msg, expected.getTransferType(), actual.getTransferType());
            assertEquals(msg, expected.getTransparency(), actual.getTransparency());
        }
    }
    
    public void testCreateBanded() {
        int[] bankIndices = new int[]{0, 1, 2};
        int[] bandOffsets = new int[]{1, 1, 1};
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int dataType = DataBuffer.TYPE_BYTE;
        boolean hasAlpha = false;
        
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createBanded(colorSpace,
                                                                           bankIndices, 
                                                                           bandOffsets, 
                                                                           dataType, 
                                                                           hasAlpha, 
                                                                           false);
        
        ColorModel colorModel = typeSpecifier.getColorModel();
        assertEquals("Failed to create with the correct colorspace type",
                     ColorSpace.TYPE_RGB, colorModel.getColorSpace().getType());
        assertEquals("Failed to create with the correct transparency",
                     Transparency.OPAQUE, colorModel.getTransparency());
        assertEquals("Failed to create with the correcttransfer type",
                     DataBuffer.TYPE_BYTE, colorModel.getTransferType());
        
        BandedSampleModel sampleModel = (BandedSampleModel) typeSpecifier.getSampleModel();
        assertArrayEquals("Failed to create with the correct bankIndices",
                          bankIndices, sampleModel.getBankIndices());
        assertArrayEquals("Failed to create with the correct bankIndices",
                          bandOffsets, sampleModel.getBandOffsets());  
    }

    public void testCreateBufferedImage() {
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createGrayscale(8, 
                                                                              DataBuffer.TYPE_BYTE, 
                                                                              true);
        
        int width = 10;
        int height = 10;
        BufferedImage image = typeSpecifier.createBufferedImage(width, height);
        assertEquals("Failed to create with the correct ColorModel",
                     typeSpecifier.getColorModel(), image.getColorModel());
        assertEquals("Failed to create with the correct SampleModel",
                     typeSpecifier.getSampleModel().getClass(), image.getSampleModel().getClass());
        assertEquals("Failed to create with the correct width",
                     width, image.getWidth());
        assertEquals("Failed to create with the correct height",
                     height, image.getHeight());
    }
    
    public void testGetBufferedImageType() {
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createGrayscale(8, 
                                                                              DataBuffer.TYPE_BYTE, 
                                                                              true);

        assertEquals("Failed to return the correct type",
                     BufferedImage.TYPE_BYTE_GRAY, typeSpecifier.getBufferedImageType());        
    }
    
    public void testCreateInterleaved() {
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] bandOffsets = new int[] { 1, 2, 3 };
        int dataType = DataBuffer.TYPE_BYTE;

        ImageTypeSpecifier type = ImageTypeSpecifier.createInterleaved(
                colorSpace, bandOffsets, dataType, false, false);
        ColorModel colorModel = type.getColorModel();
        PixelInterleavedSampleModel sampleModel = (PixelInterleavedSampleModel) type
                .getSampleModel();

        // validate the colorModel
        assertEquals("Failed to create with the correct colorspace type",
                ColorSpace.TYPE_RGB, colorModel.getColorSpace().getType());
        assertEquals("Failed to create with the correct transparency",
                Transparency.OPAQUE, colorModel.getTransparency());
        assertEquals("Failed to create with the correct transfer type",
                DataBuffer.TYPE_BYTE, colorModel.getTransferType());

        // validate the sampleModel
        assertTrue("The sampleModel and colorModel are not compatible",
                colorModel.isCompatibleSampleModel(sampleModel));
        assertArrayEquals("Failed to create with the correct bandOffsets",
                bandOffsets, sampleModel.getBandOffsets());
        assertEquals("Failed to create with the correct pixel stride", 3,
                sampleModel.getPixelStride());
    }

    public void testCreateGrayscale() {        
        // create a 8-bit grayscale ImageTypeSpecifier
        ImageTypeSpecifier type =
            ImageTypeSpecifier.createGrayscale(8, DataBuffer.TYPE_BYTE, true);
        
        ColorModel model = type.getColorModel();
        assertEquals("Failed to return the colorspace type",
                     ColorSpace.TYPE_GRAY, model.getColorSpace().getType());
        assertEquals("Failed to return the transparency",
                     Transparency.OPAQUE, model.getTransparency());
        assertEquals("Failed to return the transfer type",
                     DataBuffer.TYPE_BYTE, model.getTransferType());
        assertEquals("Failed to return the pixel size",
                     8, model.getPixelSize());
        
        // create a 16-bit grayscale AlphaPremultiplied ImageTypeSpecifier
        type = ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_USHORT,
                                                  true, false);
        
        model = type.getColorModel();
        assertEquals("Failed to return the colorspace type",
                     ColorSpace.TYPE_GRAY, model.getColorSpace().getType());
        assertEquals("Failed to return the transparency",
                     Transparency.TRANSLUCENT, model.getTransparency());
        assertEquals("Failed to return the transfer type",
                     DataBuffer.TYPE_USHORT, model.getTransferType());
        assertEquals("Failed to return the pixel size",
                     32, model.getPixelSize());        
    }
    
    public void testCreateIndexed() {
        byte[] redLUT = new byte[]{1, 10};
        byte[] greenLUT = new byte[]{2,20};
        byte[] blueLUT = new byte[]{3,30};
        byte[] alphaLUT = new byte[]{4,40};
        
        ImageTypeSpecifier type =
            ImageTypeSpecifier.createIndexed(redLUT, greenLUT, blueLUT,
                                             alphaLUT, 1, DataBuffer.TYPE_BYTE);
        ColorModel model = type.getColorModel();
        
        assertEquals("Failed to return the colorspace",
                     ColorSpace.TYPE_RGB, model.getColorSpace().getType());
        assertEquals("Failed to return the transparency",
                     Transparency.TRANSLUCENT, model.getTransparency());
        assertEquals("Failed to return the tranfer type",
                     DataBuffer.TYPE_BYTE, model.getTransferType());
        assertEquals("Failed to return the red color component",
                     1, model.getRed(0));
        assertEquals("Failed to return the red color component",
                     10, model.getRed(1));
        assertEquals("Failed to return the green color component",
                     2, model.getGreen(0));
        assertEquals("Failed to return the green color component",
                     20, model.getGreen(1));
        assertEquals("Failed to return the blue color component",
                     3, model.getBlue(0));
        assertEquals("Failed to return the blue color component",
                     30, model.getBlue(1));
        assertEquals("Failed to return the alpha color component",
                     4, model.getAlpha(0));
        assertEquals("Failed to return the alpha color component",
                     40, model.getAlpha(1));
    }
}
