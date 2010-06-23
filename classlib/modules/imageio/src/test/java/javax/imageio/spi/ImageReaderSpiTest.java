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

package javax.imageio.spi;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import junit.framework.TestCase;

public class ImageReaderSpiTest extends TestCase {
    
    private void testIsOwnReader(String format1, String format2) throws Exception {
        ImageReader reader1 = ImageIO.getImageReadersByFormatName(format1).next();
        ImageReaderSpi readerSpi1 = reader1.getOriginatingProvider();

        ImageReader reader2 = ImageIO.getImageReadersByFormatName(format2).next();
        ImageReaderSpi readerSpi2 = reader2.getOriginatingProvider();

        assertTrue(readerSpi1.isOwnReader(reader1));
        assertTrue(readerSpi2.isOwnReader(reader2));
        assertFalse(readerSpi1.isOwnReader(reader2));
        assertFalse(readerSpi2.isOwnReader(reader1));
        
        try {
            readerSpi1.isOwnReader(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected){
            // Ok
        }
    }
    
    public void testJpegOwnReader() throws Exception {
        testIsOwnReader("jpeg", "gif");
    }

    public void testGifOwnReader() throws Exception {
        testIsOwnReader("gif", "png");
    }
    
    public void testPngOwnReader() throws Exception {
        testIsOwnReader("png", "jpeg");
    }
}
