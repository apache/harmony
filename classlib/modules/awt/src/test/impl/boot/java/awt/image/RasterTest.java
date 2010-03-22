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


package java.awt.image;

import junit.framework.TestCase;
import java.awt.Point;

public class RasterTest extends TestCase {

    /**
     * Test method for
     * {@link java.awt.image.Raster#createPackedRaster(int, int, int, int[], java.awt.Point)}.
     */
    public void testCreatePackedRasterIntIntIntIntArrayPoint() {
        // Regression test for HARMONY-2435
        try {
            Raster.createPackedRaster(-1, -1, -1, new int[1], null);
            fail("IllegalArgumentException was not thrown"); //$NON-NLS-1$
        } catch (IllegalArgumentException ex) {
            // expected
        }

        // Regression test for HARMONY-2435
        try {
            Raster.createPackedRaster(DataBuffer.TYPE_BYTE, 1, 1, null, null);
            fail("NullPointerException was not thrown"); //$NON-NLS-1$
        } catch (NullPointerException ex) {
            // expected
        }
    }
    
    public void testCreatePackedRaster() throws RasterFormatException {
        // Regression test for harmony-2717
        try {
            Raster.createPackedRaster(null, -32, Integer.MAX_VALUE, 35, new int[] {}, null);
            fail("Exception expected"); //$NON-NLS-1$
        } catch (NullPointerException expectedException) {
            // Expected
        }
        
        // Regression for HARMONY-2884
        try {
            Raster.createPackedRaster(new DataBufferInt(1), 7, 9, 214,
                    new int[] { 0, 0, 0 }, new Point(10292, 0));
            fail("RasterFormatException expected!"); //$NON-NLS-1$
        } catch (RasterFormatException e) {
            // expected
        }
        
        try {
            Raster.createRaster(new SinglePixelPackedSampleModel(1, 10, 12, 0,
                    new int[431]), new DataBufferUShort(new short[5], 3),
                    new Point());
            fail("RasterFormatException expected!"); //$NON-NLS-1$
        } catch (RasterFormatException e) {
            // expected
        }
    }
    
    // Regression test for harmony-2885
    public void testDataTypes() {
        SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_USHORT, 2, 26, new int[798]);

        try {
            Raster localRaster = Raster.createRaster(sm, new DataBufferShort(1,
                    1), new Point());
            fail("RasterFormatException expected!");
        } catch (RasterFormatException expectedException) {
            // Expected
        }
    }

    // Regression test for Harmony-2743
    public void test_createCompatibleWritableRaster()
            throws IllegalArgumentException {
        MultiPixelPackedSampleModel localMultiPixelPackedSampleModel =
                new MultiPixelPackedSampleModel(0, 5, 22, -1, -25, 40825);
        Point localPoint = new Point();
        Raster localRaster =
            Raster.createWritableRaster(localMultiPixelPackedSampleModel,
                                        localPoint);
        try {
            localRaster.createCompatibleWritableRaster(-32,8);
            fail("Exception expected");
        } catch (RasterFormatException expectedException) {
            // Expected
        }
    }
    
    public void testGetPixels() {
        // Regression test for HARMONY-2875
        try {
            Raster.createRaster(new BandedSampleModel(1, 2, 3, 4),
                    new DataBufferByte(new byte[191], 6),
                    new Point(new Point(28, 43))).getPixels(6,
                    Integer.MAX_VALUE, 1, 0, new int[] {});
            fail("ArrayIndexOutOfBoundsException should be thrown"); //$NON-NLS-1$
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }
}
