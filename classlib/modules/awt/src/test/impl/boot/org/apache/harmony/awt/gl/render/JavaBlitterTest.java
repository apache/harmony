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
package org.apache.harmony.awt.gl.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.WritableRaster;

import org.apache.harmony.awt.gl.MultiRectArea;
import org.apache.harmony.awt.gl.image.OrdinaryWritableRaster;


import junit.framework.TestCase;

public class JavaBlitterTest extends TestCase{
    int w, h;
    BufferedImage src, dst;

    public JavaBlitterTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        w = 100;
        h = 100;
    }

    private BufferedImage createImage(int imageType){
        BufferedImage bi =  new BufferedImage(w, h, imageType);
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(Color.red);
        g2d.fillRect(0, 0, 50, 50);
        g2d.setColor(Color.green);
        g2d.fillRect(50, 0, 50, 50);
        g2d.setColor(Color.blue);
        g2d.fillRect(0, 50, 50, 50);
        g2d.setColor(Color.white);
        g2d.fillRect(50, 50, 50, 50);
        return bi;
    }

    // PART I. Different Color Models with sRGB Color Space
    // Blitting from Direct Color Model to Direct Color Model (INT RGB)
    public final void test_from_INT_RGB_to_INT_ARGB(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_INT_RGB_to_INT_ARGB_PRE(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_INT_RGB_to_INT_RGB(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Direct Color Model to Direct Color Model (INT BGR)
    public final void test_from_INT_RGB_to_INT_BGR(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Direct Color Model to Component Color Model (BYTE RGB)
    public final void test_from_INT_RGB_to_3BYTE_BGR(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_INT_RGB_to_4BYTE_ABGR(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_INT_RGB_to_4BYTE_ABGR_PRE(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Direct Color Model to Direct ColorModel (USHORT RGB)
    public final void test_from_INT_RGB_to_USHORT_555_RGB(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_555_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_INT_RGB_to_USHORT_565_RGB(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_565_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Direct Color Model to Index Color Model (Map size - 256)
    public final void test_from_INT_RGB_to_BYTE_INDEXED(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Direct Color Model to Index Color Model (Map size - 2)
    public final void test_from_INT_RGB_to_BYTE_BINARY(){
        src = createImage(BufferedImage.TYPE_INT_RGB);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h/2; y++){
            for(int x = 0; x < w/2; x++){
                assertEquals(0xff000000, dst.getRGB(x, y));
            }
        }
        for(int y = 0; y < h/2; y++){
            for(int x = w/2; x < w; x++){
                assertEquals(0xffffffff, dst.getRGB(x, y));
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = 0; x < w/2; x++){
                assertEquals(0xff000000, dst.getRGB(x, y));
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = w/2; x < w; x++){
                assertEquals(0xffffffff, dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Component Color Model to Direct Color Model (INT RGB)
    public final void test_from_3BYTE_BGR_to_INT_ARGB(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_3BYTE_BGR_to_INT_ARGB_PRE(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_3BYTE_BGR_to_INT_RGB(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_3BYTE_BGR_to_INT_BGR(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Component Color Model to Direct Color Model (INT BGR)
    public final void test_from_3BYTE_BGR_to_3BYTE_BGR(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Component Color Model to Component Color Model (BYTE RGB)
    public final void test_from_3BYTE_BGR_to_4BYTE_ABGR(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_3BYTE_BGR_to_4BYTE_ABGR_PRE(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Component Color Model to Direct Color Model (USHORT RGB)
    public final void test_from_3BYTE_BGR_to_USHORT_555_RGB(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_555_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_3BYTE_BGR_to_USHORT_565_RGB(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_565_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Component Color Model to Index Color Model (Map size - 256)
    public final void test_from_3BYTE_BGR_to_BYTE_INDEXED(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Component Color Model to Index Color Model (Map size - 2)
    public final void test_from_3BYTE_BGR_to_BYTE_BINARY(){
        src = createImage(BufferedImage.TYPE_3BYTE_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h/2; y++){
            for(int x = 0; x < w/2; x++){
                assertEquals(0xff000000, dst.getRGB(x, y));
            }
        }
        for(int y = 0; y < h/2; y++){
            for(int x = w/2; x < w; x++){
                assertEquals(0xffffffff, dst.getRGB(x, y));
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = 0; x < w/2; x++){
                assertEquals(0xff000000, dst.getRGB(x, y));
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = w/2; x < w; x++){
                assertEquals(0xffffffff, dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Index Color Model to Direct Color Model (INT RGB)
    public final void test_from_BYTE_INDEXED_to_INT_ARGB(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_BYTE_INDEXED_to_INT_ARGB_PRE(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_BYTE_INDEXEDR_to_INT_RGB(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Index Color Model to Direct Color Model (INT BGR)
    public final void test_from_BYTE_INDEXED_to_INT_BGR(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Index Color Model to Component Color Model (BYTE RGB)
    public final void test_from_BYTE_INDEXED_to_3BYTE_BGR(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_BYTE_INDEXED_to_4BYTE_ABGR(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_BYTE_INDEXED_to_4BYTE_ABGR_PRE(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Index Color Model to Direct Color Model (USHORT RGB)
    public final void test_from_BYTE_INDEXED_to_USHORT_555_RGB(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_555_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    public final void test_from_BYTE_INDEXED_to_USHORT_565_RGB(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_USHORT_565_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Index Color Model to Index Color Model (Map size - 256)
    public final void test_from_BYTE_INDEXED_to_BYTE_INDEXED(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // Blitting from Index Color Model to Index Color Model (Map size - 2)
    public final void test_from_BYTE_INDEXED_to_BYTE_BINARY(){
        src = createImage(BufferedImage.TYPE_BYTE_INDEXED);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h/2; y++){
            for(int x = 0; x < w/2; x++){
                assertEquals(0xff000000, dst.getRGB(x, y));
            }
        }
        for(int y = 0; y < h/2; y++){
            for(int x = w/2; x < w; x++){
                assertEquals(0xffffffff, dst.getRGB(x, y));
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = 0; x < w/2; x++){
                assertEquals(0xff000000, dst.getRGB(x, y));
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = w/2; x < w; x++){
                assertEquals(0xffffffff, dst.getRGB(x, y));
            }
        }
    }

    // PART II. Blitting to Different Rasters and Data Buffers

    // Blitting from Buffered Image (INT RGB) to Custom Raster
    // (Float Data Buffer)
    public final void test_from_BuffImg_to_FloatDataBuffer(){
        src = createImage(BufferedImage.TYPE_INT_RGB);

        DataBufferFloat dbf = new DataBufferFloat(w * h * 3);
        int offsets[] = new int[]{0,1,2};
        ComponentSampleModel csm = new ComponentSampleModel(DataBuffer.TYPE_FLOAT,
                w, h, 3, 3 * w, offsets);
        WritableRaster wr = new OrdinaryWritableRaster(csm, dbf, new Point(0, 0));
        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
        BufferedImage dst = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
        Graphics2D g2d = dst.createGraphics();
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }
    }

    // PART III. Affine Tranformation (Translate  only) and various destination
    // coordinates.
    // Destination coordinates test (X coordinate changing)
    public final void test_Dest_Coordinates_X(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        for(int i = -w; i < w; i++){
            dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
            Graphics2D g2d = dst.createGraphics();
            g2d.drawImage(src, i, 0, null);


            for(int y = 0; y < h/2; y++){
                for(int x = 0; x < w/2; x++){
                    if(i + w <= 0) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + w < w/2){
                        if(x < w + i) {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i + w < w){
                        if(x < i + w/2) {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        }
                    }else{
                        if(x < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = 0; y < h/2; y++){
                for(int x = w/2; x < w; x++){
                    if(i + w < w/2) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + w < w){
                        if(x < w + i) {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i < w/2){
                        if(x < i + w/2) {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        }
                    }else{
                        if(x < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = h/2; y < h; y++){
                for(int x = 0; x < w/2; x++){
                    if(i + w <= 0) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + w < w/2){
                        if(x < w + i) {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i + w < w){
                        if(x < i + w/2) {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        }
                    }else{
                        if(x < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = h/2; y < h; y++){
                for(int x = w/2; x < w; x++){
                    if(i + w < w/2) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + w < w){
                        if(x < w + i) {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i < w/2){
                        if(x < i + w/2) {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        }
                    }else{
                        if(x < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        }
                    }
                }
            }

        }
    }

    // Destination coordinates test (Y coordinate changing)
    public final void test_Dest_Coordinates_Y(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        for(int i = -h; i < h; i++){
            dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
            Graphics2D g2d = dst.createGraphics();
            g2d.drawImage(src, 0, i, null);


            for(int y = 0; y < h/2; y++){
                for(int x = 0; x < w/2; x++){
                    if(i + h <= 0) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + h < h/2){
                        if(y < h + i) {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i + h < h){
                        if(y < i + h/2) {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        }
                    }else{
                        if(y < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = 0; y < h/2; y++){
                for(int x = w/2; x < w; x++){
                    if(i + h <= 0) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + h < h/2){
                        if(y < h + i) {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i + h < h){
                        if(y < i + h/2) {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        }
                    }else{
                        if(y < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = h/2; y < h; y++){
                for(int x = 0; x < w/2; x++){
                    if(i + h < h/2) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + h < h){
                        if(y < i + h) {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i < h/2){
                        if(y < i + h/2) {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        }
                    }else if(i < h){
                        if(y < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        }
                    }else{
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    }
                }
            }
            for(int y = h/2; y < h; y++){
                for(int x = w/2; x < w; x++){
                    if(i + h < h/2) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + h < h){
                        if(y < i + h) {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i < h/2){
                        if(y < i + h/2) {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        }
                    }else if(i < h){
                        if(y < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        }
                    }else{
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    }
                }
            }

        }
    }

    // Affine transform test (X translate)
    public final void test_AffineTransform_X(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        for(int i = -w; i < w; i++){
            dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
            Graphics2D g2d = dst.createGraphics();
            AffineTransform t = new AffineTransform();
            t.setToTranslation(i, 0);
            g2d.setTransform(t);
            g2d.drawImage(src, 0, 0, null);


            for(int y = 0; y < h/2; y++){
                for(int x = 0; x < w/2; x++){
                    if(i + w <= 0) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + w < w/2){
                        if(x < w + i) {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i + w < w){
                        if(x < i + w/2) {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        }
                    }else{
                        if(x < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = 0; y < h/2; y++){
                for(int x = w/2; x < w; x++){
                    if(i + w < w/2) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + w < w){
                        if(x < w + i) {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i < w/2){
                        if(x < i + w/2) {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        }
                    }else{
                        if(x < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = h/2; y < h; y++){
                for(int x = 0; x < w/2; x++){
                    if(i + w <= 0) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + w < w/2){
                        if(x < w + i) {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i + w < w){
                        if(x < i + w/2) {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        }
                    }else{
                        if(x < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = h/2; y < h; y++){
                for(int x = w/2; x < w; x++){
                    if(i + w < w/2) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + w < w){
                        if(x < w + i) {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i < w/2){
                        if(x < i + w/2) {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        }
                    }else{
                        if(x < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        }
                    }
                }
            }

        }
    }

    // Affine transform test (Y translate)
    public final void test_AffineTransform_Y(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        for(int i = -h; i < h; i++){
            dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
            Graphics2D g2d = dst.createGraphics();
            AffineTransform t = new AffineTransform();
            t.setToTranslation(0, i);
            g2d.setTransform(t);
            g2d.drawImage(src, 0, 0, null);


            for(int y = 0; y < h/2; y++){
                for(int x = 0; x < w/2; x++){
                    if(i + h <= 0) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + h < h/2){
                        if(y < h + i) {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i + h < h){
                        if(y < i + h/2) {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        }
                    }else{
                        if(y < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = 0; y < h/2; y++){
                for(int x = w/2; x < w; x++){
                    if(i + h <= 0) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + h < h/2){
                        if(y < h + i) {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i + h < h){
                        if(y < i + h/2) {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        }
                    }else{
                        if(y < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        }
                    }
                }
            }
            for(int y = h/2; y < h; y++){
                for(int x = 0; x < w/2; x++){
                    if(i + h < h/2) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + h < h){
                        if(y < i + h) {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i < h/2){
                        if(y < i + h/2) {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff0000ff, dst.getRGB(x, y));
                        }
                    }else if(i < h){
                        if(y < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffff0000, dst.getRGB(x, y));
                        }
                    }else{
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    }
                }
            }
            for(int y = h/2; y < h; y++){
                for(int x = w/2; x < w; x++){
                    if(i + h < h/2) {
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    } else if(i + h < h){
                        if(y < i + h) {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        }
                    }else if(i < h/2){
                        if(y < i + h/2) {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xffffffff, dst.getRGB(x, y));
                        }
                    }else if(i < h){
                        if(y < i) {
                            assertEquals(0xff000000, dst.getRGB(x, y));
                        } else {
                            assertEquals(0xff00ff00, dst.getRGB(x, y));
                        }
                    }else{
                        assertEquals(0xff000000, dst.getRGB(x, y));
                    }
                }
            }

        }
    }

    // PART IV. Clipping
    // Null Clipping test
    public final void test_NullClipping(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.setClip(null);
        g2d.drawImage(src, 0, 0, null);
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(src.getRGB(x, y), dst.getRGB(x, y));
            }
        }

    }

    // One Rectangle Clipping test
    public final void test_OneRectangleClipping(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.setClip(w/4, h/4, w/2, h/2);
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h/2; y++){
            for(int x = 0; x < w/2; x++){
                if(x >= w/4 && y >= h/4) {
                    assertEquals(0xffff0000, dst.getRGB(x, y));
                } else {
                    assertEquals(0xff000000, dst.getRGB(x, y));
                }
            }
        }
        for(int y = 0; y < h/2; y++){
            for(int x = w/2; x < w; x++){
                if(x < w/2 + w/4 && y >= h/4) {
                    assertEquals(0xff00ff00, dst.getRGB(x, y));
                } else {
                    assertEquals(0xff000000, dst.getRGB(x, y));
                }
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = 0; x < w/2; x++){
                if(x >= w/4 && y < h/2 + h/4) {
                    assertEquals(0xff0000ff, dst.getRGB(x, y));
                } else {
                    assertEquals(0xff000000, dst.getRGB(x, y));
                }
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = w/2; x < w; x++){
                if(x < w/2 + w/4 && y < h/2 + h/4) {
                    assertEquals(0xffffffff, dst.getRGB(x, y));
                } else {
                    assertEquals(0xff000000, dst.getRGB(x, y));
                }
            }
        }

    }

    // Two Rectangles Clipping test
    public final void test_TwoRectanglesClipping(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        Rectangle rec[] = new Rectangle[]{new Rectangle(w/4, h/4, w/2, h/2),
                new Rectangle(w/4 + w/2, h/4 + h/2, w/2, h/2)};
        MultiRectArea mra = new MultiRectArea(rec);
        g2d.setClip(mra);
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h/2; y++){
            for(int x = 0; x < w/2; x++){
                if(x >= w/4 && y >= h/4) {
                    assertEquals(0xffff0000, dst.getRGB(x, y));
                } else {
                    assertEquals(0xff000000, dst.getRGB(x, y));
                }
            }
        }
        for(int y = 0; y < h/2; y++){
            for(int x = w/2; x < w; x++){
                if(x < w/2 + w/4 && y >= h/4) {
                    assertEquals(0xff00ff00, dst.getRGB(x, y));
                } else {
                    assertEquals(0xff000000, dst.getRGB(x, y));
                }
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = 0; x < w/2; x++){
                if(x >= w/4 && y < h/2 + h/4) {
                    assertEquals(0xff0000ff, dst.getRGB(x, y));
                } else {
                    assertEquals(0xff000000, dst.getRGB(x, y));
                }
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = w/2; x < w; x++){
                if(x < w/2 + w/4 && y < h/2 + h/4 ||
                        x >= w/2 + w/4 && y >= h/2 + h/4){
                    assertEquals(0xffffffff, dst.getRGB(x, y));
                } else {
                    assertEquals(0xff000000, dst.getRGB(x, y));
                }
            }
        }

    }

    // One Pixel Clipping test
    public final void test_OnePixelClipping(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.setClip(w/4, h/4, 1, 1);
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                if(x == w/4 && y == h/4) {
                    assertEquals(0xffff0000, dst.getRGB(x, y));
                } else {
                    assertEquals(0xff000000, dst.getRGB(x, y));
                }
            }
        }

    }

    // Boundary Clipping test
    public final void test_BoundaryClipping(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        g2d.setClip(-w, 0, w, h);
        g2d.drawImage(src, 0, 0, null);

        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(0xff000000, dst.getRGB(x, y));
            }
        }

    }

    // PART V. Alpha Composite
    // Default rule = SRC_OVER & alpha = 1.0f was tested above
    // Rule = SRC_OVER, Alpha = 0.5f
    public final void test_AlphaComposite_SRC_OVER_05(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        AlphaComposite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g2d.setComposite(comp);
        g2d.drawImage(src, 0, 0, null);
        for(int y = 0; y < h/2; y++){
            for(int x = 0; x < w/2; x++){
                assertEquals(0xff800000, dst.getRGB(x, y));
            }
        }
        for(int y = 0; y < h/2; y++){
            for(int x = w/2; x < w; x++){
                assertEquals(0xff008000, dst.getRGB(x, y));
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = 0; x < w/2; x++){
                assertEquals(0xff000080, dst.getRGB(x, y));
            }
        }
        for(int y = h/2; y < h; y++){
            for(int x = w/2; x < w; x++){
                assertEquals(0xff808080, dst.getRGB(x, y));
            }
        }
    }

    // Rule = DST, Alpha = 1.0f
    public final void test_AlphaComposite_DST_10(){
        src = createImage(BufferedImage.TYPE_INT_BGR);
        dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = dst.createGraphics();
        AlphaComposite comp = AlphaComposite.getInstance(AlphaComposite.DST);
        g2d.setComposite(comp);
        g2d.drawImage(src, 0, 0, null);
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                assertEquals(0xff000000, dst.getRGB(x, y));
            }
        }
    }
}
