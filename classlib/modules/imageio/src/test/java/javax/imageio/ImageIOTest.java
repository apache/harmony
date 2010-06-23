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

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class ImageIOTest extends TestCase {

    public void testReadURL() throws Exception {
        // Regression for HARMONY-3135
        for (URL url : listImages()) {
            assertNotNull("Failed to load image from URL " + url,
                    ImageIO.read(url));
            assertFalse("The current thread has been interrupted! URL: " + url, //$NON-NLS-1$
                    Thread.currentThread().isInterrupted());
        }
    }

    protected List<URL> listImages() {
        final String imgPath = "/images/utest."; //$NON-NLS-1$
        final Class<? extends ImageIOTest> c = getClass();
        final List<URL> img = new LinkedList<URL>();

        img.add(c.getResource(imgPath + "jpg")); //$NON-NLS-1$
        img.add(c.getResource(imgPath + "png")); //$NON-NLS-1$

        return img;
    }
    
    public void testCache() throws Exception {
        ImageIO.setUseCache(true);
        assertTrue("Failed to enable cache", ImageIO.getUseCache());
        ImageIO.setUseCache(false);
        assertFalse("Failed to disable cache", ImageIO.getUseCache());
        
        ImageIO.setCacheDirectory(null);
        assertNull("Failed to set cache directory", ImageIO.getCacheDirectory());
        
        try {
            ImageIO.setCacheDirectory(new File(""));
            fail("IllegalArgumentException expected");
        } 
        catch (IllegalArgumentException expected) {
            //OK
        }
    }

    private void testFormat(String format) {
        ImageReader reader = ImageIO.getImageReadersByFormatName(format).next();
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();

        assertEquals("getImageReader() returns an incorrect reader for " + format,
            ImageIO.getImageReader(writer).getClass(), reader.getClass());
        assertEquals("getImageWriter() returns an incorrect writer for " + format,
            ImageIO.getImageWriter(reader).getClass(), writer.getClass());
    }
    
    // assume we have exactly one reader/writer pair
    public void testGetJpegReaderWriter() throws Exception {
        testFormat("jpeg");
    }

    // assume we have exactly one reader/writer pair
    public void testGetPngReaderWriter() throws Exception {
        testFormat("png");
    }
 	
    // GIF has no writer
    // public void testGetGifReaderWriter() throws Exception {
    //     testFormat("gif");
    // }

    public void testGetNullReaderWriter() throws Exception {
    	try {
            ImageIO.getImageWriter(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            // Ok
        }

        try {
            ImageIO.getImageReader(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            // Ok
        }
    }
        
    public void testGetReaderMIMETypes() {
        Set<String> expectedMIMETypes = new HashSet<String>(Arrays.asList(new String[] {
            "image/gif", "image/x-png", "image/png", "image/jpeg" } ));
        
        Set<String> actualMIMETypes = new HashSet<String>(Arrays.asList(ImageIO.getReaderMIMETypes()));
        assertTrue(actualMIMETypes.containsAll(expectedMIMETypes));
    }
    
    public void testGetWriterMIMETypes() {
        Set<String> expectedMIMETypes = new HashSet<String>(Arrays.asList(new String[] {
            "image/x-png", "image/png", "image/jpeg" } ));
        
        Set<String> actualMIMETypes = new HashSet<String>(Arrays.asList(ImageIO.getWriterMIMETypes()));
        assertTrue(actualMIMETypes.containsAll(expectedMIMETypes));
    }
    
    public void testGetReaderFormatNames() {
        Set<String> expectedFormatNames = new HashSet<String>(Arrays.asList(new String[] {
            "JPG", "jpg", "GIF", "gif", "JPEG", "jpeg", "PNG", "png" } ));
        
        Set<String> actualFormatNames = new HashSet<String>(Arrays.asList(ImageIO.getReaderFormatNames()));
        assertTrue(actualFormatNames.containsAll(expectedFormatNames));
    }
    
    public void testGetWriterFormatNames() {
        Set<String> expectedFormatNames = new HashSet<String>(Arrays.asList(new String[] {
            "JPG", "jpg", "JPEG", "jpeg", "PNG", "png" } ));
        
        Set<String> actualFormatNames = new HashSet<String>(Arrays.asList(ImageIO.getWriterFormatNames()));
        assertTrue(actualFormatNames.containsAll(expectedFormatNames));
    }

}
