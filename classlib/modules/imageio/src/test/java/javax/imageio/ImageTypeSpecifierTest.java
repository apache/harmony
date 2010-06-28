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
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;

import junit.framework.TestCase;

public class ImageTypeSpecifierTest extends TestCase {
    public void testCreateGrayscale() {        
        // create a 8-bit grayscale ImageTypeSpecifier
        ImageTypeSpecifier type = ImageTypeSpecifier.createGrayscale(8, DataBuffer.TYPE_BYTE, true);
        
        ColorModel model = type.getColorModel();
        assertEquals("Failed to return the colorspace type", model.getColorSpace().getType(), ColorSpace.TYPE_GRAY);
        assertEquals("Failed to return the transparency", model.getTransparency(), Transparency.OPAQUE);
        assertEquals("Failed to return the transfer type", model.getTransferType(), DataBuffer.TYPE_BYTE);
        assertEquals("Failed to return the pixel size", model.getPixelSize(), 8);
        
        // create a 16-bit grayscale AlphaPremultiplied ImageTypeSpecifier
        type = ImageTypeSpecifier.createGrayscale(16, DataBuffer.TYPE_USHORT, true, false);
        
        model = type.getColorModel();
        assertEquals("Failed to return the colorspace type", model.getColorSpace().getType(), ColorSpace.TYPE_GRAY);
        assertEquals("Failed to return the transparency", model.getTransparency(), Transparency.TRANSLUCENT);
        assertEquals("Failed to return the transfer type", model.getTransferType(), DataBuffer.TYPE_USHORT);
        assertEquals("Failed to return the pixel size", model.getPixelSize(), 32);        
    }
    
    public void testCreateIndexed() {
        byte[] redLUT = new byte[]{1, 10};
        byte[] greenLUT = new byte[]{2,20};
        byte[] blueLUT = new byte[]{3,30};
        byte[] alphaLUT = new byte[]{4,40};
        
        ImageTypeSpecifier type = ImageTypeSpecifier.createIndexed(redLUT, greenLUT, blueLUT, alphaLUT, 1, DataBuffer.TYPE_BYTE);
        ColorModel model = type.getColorModel();
        
        assertEquals("Failed to return the colorspace", model.getColorSpace().getType(), ColorSpace.TYPE_RGB);
        assertEquals("Failed to return the transparency", model.getTransparency(), Transparency.TRANSLUCENT);
        assertEquals("Failed to return the tranfer type", model.getTransferType(), DataBuffer.TYPE_BYTE);
        assertEquals("Failed to return the red color component", model.getRed(0), 1);
        assertEquals("Failed to return the red color component", model.getRed(1), 10);
        assertEquals("Failed to return the green color component", model.getGreen(0), 2);
        assertEquals("Failed to return the green color component", model.getGreen(1), 20);
        assertEquals("Failed to return the blue color component", model.getBlue(0), 3);
        assertEquals("Failed to return the blue color component", model.getBlue(1), 30);
        assertEquals("Failed to return the alpha color component", model.getAlpha(0), 4);
        assertEquals("Failed to return the alpha color component", model.getAlpha(1), 40);
    }
}
