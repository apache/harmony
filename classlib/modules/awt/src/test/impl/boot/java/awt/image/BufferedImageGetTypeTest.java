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

import junit.framework.TestCase;

public class BufferedImageGetTypeTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DataBufferByteTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Constructor for DataBufferByteTest.
     * @param name
     */
    public BufferedImageGetTypeTest(String name) {
        super(name);
    }

    public final void testGetTypeICM_1_1_opaque_byte(){
        int cmap[] = new int[256];
        int pixelBits = 1;
        int colorMapSize = 1;
        int startIdx = 0;
        boolean hasAlpha = false;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_BYTE;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_BYTE_BINARY, bi.getType());
    }

    public final void testGetTypeICM_1_2_opaque_byte(){
        int cmap[] = new int[256];
        int pixelBits = 1;
        int colorMapSize = 2;
        int startIdx = 0;
        boolean hasAlpha = false;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_BYTE;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_BYTE_BINARY, bi.getType());
    }

    public final void testGetTypeICM_1_2_alpha_byte(){
        int cmap[] = new int[256];
        int pixelBits = 1;
        int colorMapSize = 2;
        int startIdx = 0;
        boolean hasAlpha = true;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_BYTE;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_CUSTOM, bi.getType());
    }

    public final void testGetTypeICM_2_3_opaque_byte(){
        int cmap[] = new int[256];
        int pixelBits = 2;
        int colorMapSize = 3;
        int startIdx = 0;
        boolean hasAlpha = false;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_BYTE;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_BYTE_BINARY, bi.getType());
    }

    public final void testGetTypeICM_2_4_opaque_byte(){
        int cmap[] = new int[256];
        int pixelBits = 2;
        int colorMapSize = 4;
        int startIdx = 0;
        boolean hasAlpha = false;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_BYTE;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_BYTE_BINARY, bi.getType());
    }

    public final void testGetTypeICM_2_4_alpha_byte(){
        int cmap[] = new int[256];
        int pixelBits = 2;
        int colorMapSize = 4;
        int startIdx = 0;
        boolean hasAlpha = true;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_BYTE;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_CUSTOM, bi.getType());
    }

    public final void testGetTypeICM_8_10_opaque_byte(){
        int cmap[] = new int[256];
        int pixelBits = 8;
        int colorMapSize = 10;
        int startIdx = 0;
        boolean hasAlpha = false;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_BYTE;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_BYTE_INDEXED, bi.getType());
    }

    public final void testGetTypeICM_8_256_opaque_byte(){
        int cmap[] = new int[256];
        int pixelBits = 8;
        int colorMapSize = 256;
        int startIdx = 0;
        boolean hasAlpha = false;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_BYTE;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_BYTE_INDEXED, bi.getType());
    }

    public final void testGetTypeICM_8_256_alpha_byte(){
        int cmap[] = new int[256];
        int pixelBits = 8;
        int colorMapSize = 256;
        int startIdx = 0;
        boolean hasAlpha = true;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_BYTE;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);
        hasAlpha = true;
        icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        wr = icm.createCompatibleWritableRaster(1,1);
        bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_BYTE_INDEXED, bi.getType());
    }

    public final void testGetTypeICM_16_256_opaque_ushort(){
        int cmap[] = new int[256];
        int pixelBits = 16;
        int colorMapSize = 256;
        int startIdx = 0;
        boolean hasAlpha = false;
        int transpPixel = -1;
        int transferType = DataBuffer.TYPE_USHORT;

        IndexColorModel icm = new IndexColorModel(pixelBits, colorMapSize, cmap, startIdx, 
            hasAlpha, transpPixel, transferType);
        WritableRaster wr = icm.createCompatibleWritableRaster(1,1);
        BufferedImage bi = new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);

        assertEquals(BufferedImage.TYPE_CUSTOM, bi.getType());
    }
    
}
