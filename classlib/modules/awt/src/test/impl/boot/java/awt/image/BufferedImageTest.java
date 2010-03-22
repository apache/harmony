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

import java.awt.Image;
import java.awt.image.BufferedImage;

import junit.framework.TestCase;

public class BufferedImageTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BufferedImageTest.class);
    }
    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Constructor for BufferedImageTest.
     * @param name
     */
    public BufferedImageTest(String name) {
        super(name);
    }
    
    public final void testGetWritableTile(){
        BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        bi.getWritableTile(1, 1);
        
        //Regression test for HARMONY-1658
        BufferedImage img = new BufferedImage(10, 16, BufferedImage.TYPE_4BYTE_ABGR);
        try {
            img.isTileWritable(1,1);
            fail("IllegalArgumentException is expected");
        } catch (IllegalArgumentException iae) {
        }
    }

    public void testGetProperty() {
        // Regression test HARMONY-1656
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        assertEquals("Image.UndefinedProperty",
                     Image.UndefinedProperty, img.getProperty("XXX"));
    }

    //Regression tests for HARMONY-5066
    public final void testH5066_INT_RGB(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

    public final void testH5066_INT_ARGB(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

    public final void testH5066_INT_ARGB_PRE(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB_PRE);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

    public final void testH5066_INT_BGR(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_BGR);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

    public final void testH5066_3BYTE_BGR(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }


    public final void testH5066_4BYTE_ABGR(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }


    public final void testH5066_4BYTE_ABGR_PRE(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

    public final void testH5066_USHORT_565_RGB(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_USHORT_565_RGB);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

    public final void testH5066_USHORT_555_RGB(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_USHORT_555_RGB);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

    public final void testH5066_BYTE_GRAY(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

    public final void testH5066_USHORT_GRAY(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_USHORT_GRAY);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

    static byte[] cm1 = new byte[] {0, (byte)255 };
    static byte[] cm2 = new byte[] {0, (byte)85, (byte)170, (byte)255};
    static byte[] cm4 = new byte[] {0, (byte)17, (byte)34, (byte)51,
                                  (byte)68, (byte)85,(byte) 102, (byte)119,
                                  (byte)136, (byte)153, (byte)170, (byte)187,
                                  (byte)204, (byte)221, (byte)238, (byte)255};

    public final void testH5066_BYTE_BINARY(){
        WritableRaster wr = null;
        IndexColorModel icm = null;

        try {
            icm = new IndexColorModel(1, cm1.length, cm1, cm1, cm1);
            wr = icm.createCompatibleWritableRaster(10, 10);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException (pixel bits = 1): " + e.getMessage());
        }

        try {
            icm = new IndexColorModel(2, cm2.length, cm2, cm2, cm2);
            wr = icm.createCompatibleWritableRaster(10, 10);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException (pixel bits = 2): " + e.getMessage());
        }

        try {
            icm = new IndexColorModel(4, cm4.length, cm4, cm4, cm4);
            wr = icm.createCompatibleWritableRaster(10, 10);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException (pixel bits = 4): " + e.getMessage());
        }
    }

    public final void testH5066_BYTE_INDEXED(){
        BufferedImage bi = null;
        try {
            bi = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);
        } catch (RasterFormatException e) {
            fail("Unexpected RasterFormatException: " + e.getMessage());
        }
    }

}
