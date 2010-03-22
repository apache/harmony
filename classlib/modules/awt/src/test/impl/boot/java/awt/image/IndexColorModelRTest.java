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
 * @author ivstolya
 */
package java.awt.image;

import java.awt.Transparency;

import junit.framework.TestCase;

public class IndexColorModelRTest extends TestCase {
    IndexColorModel icm1, icm2, icm3, icm4, icm5, icm6;
    byte r, g, b, a;
    byte cmap1[], cmap2[];

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IndexColorModelRTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        r = (byte)0x1f;
        g = (byte)0x5f;
        b = (byte)0xff;
        a = (byte)0x7f;
        
        cmap1 = new byte[6];
        cmap1[3] = r;
        cmap1[4] = g;
        cmap1[5] = b;
        
        cmap2 = new byte[8];
        cmap2[4] = r;
        cmap2[5] = g;
        cmap2[6] = b;
        cmap2[7] = a;
        
        icm1 = new IndexColorModel(1 ,2, cmap1, 0, false, -1);  
        icm2 = new IndexColorModel(1 ,2, cmap1, 0, false, 0);  
        icm3 = new IndexColorModel(1 ,2, cmap2, 0, true, 1);  
        icm4 = new IndexColorModel(1 ,2, cmap2, 0, true, 0);  
        icm5 = new IndexColorModel(1 ,2, cmap2, 0, true, -1);  
        
        cmap2[3] = a;
        icm6 = new IndexColorModel(1 ,2, cmap2, 0, true, 1);  
    }
    
    public final void testIndexColorModelConstructor(){
        int map[] = new int[2];
        icm1.getRGBs(map);
        assertEquals(0xff000000, map[0]);
        assertEquals(0xff1f5fff, map[1]);
        assertEquals(Transparency.OPAQUE, icm1.getTransparency());
        assertEquals(-1, icm1.getTransparentPixel());
        
        icm2.getRGBs(map);
        assertEquals(0x00000000, map[0]);
        assertEquals(0xff1f5fff, map[1]);
        assertEquals(Transparency.BITMASK, icm2.getTransparency());
        assertEquals(0, icm2.getTransparentPixel());
        
        icm3.getRGBs(map);
        assertEquals(0x00000000, map[0]);
        assertEquals(0x001f5fff, map[1]);
        assertEquals(Transparency.BITMASK, icm3.getTransparency());
        assertEquals(1, icm3.getTransparentPixel());
        
        icm4.getRGBs(map);
        assertEquals(0x00000000, map[0]);
        assertEquals(0x7f1f5fff, map[1]);
        assertEquals(Transparency.TRANSLUCENT, icm4.getTransparency());
        assertEquals(0, icm4.getTransparentPixel());
        
        icm5.getRGBs(map);
        assertEquals(0x00000000, map[0]);
        assertEquals(0x7f1f5fff, map[1]);
        assertEquals(Transparency.TRANSLUCENT, icm5.getTransparency());
        assertEquals(0, icm5.getTransparentPixel());

        icm6.getRGBs(map);
        assertEquals(0x7f000000, map[0]);
        assertEquals(0x001f5fff, map[1]);
        assertEquals(Transparency.TRANSLUCENT, icm6.getTransparency());
        assertEquals(1, icm6.getTransparentPixel());
        
    }
    
    public void testGetDataElementOffset() {
        // Regression test for harmony-2799
        int bits = 4;
        int size = 166;
        IndexColorModel localIndexColorModel = new IndexColorModel(bits, size,
                new byte[size], new byte[size], new byte[size]);
        int[] components = new int[] { 0, 0, 0, 0, 0, 0, 0 };
        int offset = 6;

        try {
            localIndexColorModel.getDataElement(components, offset);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            // valid
        }

    }
}
