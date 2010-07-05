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
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;

import junit.framework.TestCase;

public class ImageTypeSpecifierTest extends TestCase {
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
