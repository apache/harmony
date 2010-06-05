/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance    
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
/**
 * @author Oleg V. Khaschansky
 */

package java.awt.image;

import junit.framework.TestCase;

import java.awt.geom.AffineTransform;
import java.awt.*;

public class BufferedImageFilterTest extends TestCase {
    private BufferedImageFilter filter;
    private BufferedImageOp op;

    private BufferedImageFilter lookupFilter;
    private LookupOp lop;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BufferedImageFilterTest.class);
    }

    public BufferedImageFilterTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //float kernel[] = {0.1f, 0.1f, 0.1f, 0.1f, 0.2f, 0.1f, 0.1f, 0.1f, 0.1f};
        op = new AffineTransformOp(AffineTransform.getRotateInstance(Math.PI/4.), null);
        filter = new BufferedImageFilter(op);

        byte lut[] = new byte[256];
        for (int i = 0; i < lut.length; i++) {
            lut[i] = (byte)(255 - i);
        }
        ByteLookupTable blut = new ByteLookupTable(0, lut);
        lop = new LookupOp(blut, null);
        lookupFilter = new BufferedImageFilter(lop);
    }

    public void testBufferedImageFilter() throws Exception {
        BufferedImageOp op = new AffineTransformOp(
                AffineTransform.getTranslateInstance(0, 0),
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR
        );

        new BufferedImageFilter(op);

        try {
            new BufferedImageFilter(null);
            fail("Should throw NullPointerException, but was not.");
        } catch (NullPointerException e) {
            // Expected
        } 
    }

    public void testGetBufferedImageOp() throws Exception {
        assertEquals(op, filter.getBufferedImageOp());
    }

    public void testFilterRGB() throws Exception {
        // Create source
        BufferedImage im = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        for (int i=0; i<100; i++) {
            im.setRGB(i%10, i/10, i + ((i+1)<<8) + ((i+2)<<16));
        }
        // Create filtered image
        Image img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(im.getSource(), lookupFilter));
        BufferedImage dstIm = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        dstIm.getGraphics().drawImage(img, 0, 0, null);
        // Check the result
        for (int i=0; i<100; i++) {
            int k = 255 - i;
            int rgb = dstIm.getRGB(i%10, i/10);
            assertEquals(k, rgb & 0xFF);
            assertEquals(k-1, (rgb >> 8) & 0xFF);
            assertEquals(k-2, (rgb >> 16) & 0xFF);
        }
    }

    public void testFilterByteIndexed() throws Exception {
        BufferedImage im = new BufferedImage(20, 20, BufferedImage.TYPE_BYTE_INDEXED);
        for (int i=0; i<400; i++) {
            im.getRaster().setPixel(i%20, i/20, new int[]{i});
        }
        Image img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(im.getSource(), filter));

        BufferedImage expIm = op.filter(im, null);
        BufferedImage wasIm = op.filter(im, null);
        wasIm.getGraphics().drawImage(img, 0, 0, null);

        int idata[] = null;
        int exp[] = expIm.getRaster().getPixels(0,0,expIm.getWidth(),expIm.getHeight(),idata);
        int was[] = wasIm.getRaster().getPixels(0,0,wasIm.getWidth(),wasIm.getHeight(),idata);
        for (int i=0; i<exp.length; i++) {
            assertEquals(exp[i], was[i]);
        }
    }

    public void testFilterByte()  throws Exception {
        BufferedImage im = new BufferedImage(20, 20, BufferedImage.TYPE_3BYTE_BGR);
        for (int i=0; i<400; i++) {
            im.getRaster().setPixel(i%20, i/20, new int[]{i,i+1,i+2});
        }
        Image img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(im.getSource(), filter));

        BufferedImage expIm = op.filter(im, null);
        BufferedImage wasIm = op.filter(im, null);
        wasIm.getGraphics().drawImage(img, 0, 0, null);

        int idata[] = null;
        int exp[] = expIm.getRaster().getPixels(0,0,expIm.getWidth(),expIm.getHeight(),idata);
        int was[] = wasIm.getRaster().getPixels(0,0,wasIm.getWidth(),wasIm.getHeight(),idata);
        for (int i=0; i<exp.length; i++) {
            assertEquals(exp[i], was[i]);
        }
    }
}
