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
 * @author Alexander T. Simbirtsev
 * Created on 05.04.2005

 */
package javax.swing;

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleIcon;

public class ImageIconTest extends SwingTestCase {
    private static final String FILE_NAME_1 = "images/Error.gif";

    private static final int FILE_SIZE_1 = 923;

    private static final int ICON_SIZE_1 = 32;

    private static final String FILE_NAME_2 = "ImageIconTest.class";

    class MyImageIcon extends ImageIcon {
        private static final long serialVersionUID = 1L;

        public Component getComponent() {
            return component;
        }

        public MediaTracker getTracker() {
            return tracker;
        }

        @Override
        public void loadImage(final Image image) {
            super.loadImage(image);
        }
    };

    protected ImageIcon icon = null;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        icon = null;
        super.tearDown();
    }

    /*
     * Class under test for void ImageIcon(String)
     */
    public void testImageIconString() {
        URL url1 = getClass().getResource(FILE_NAME_1);
        URL url2 = getClass().getResource(FILE_NAME_2);
        assertNotNull("file is found", url1);
        String filePath1 = url1.getPath();
        // preventing jar failures
        if (filePath1.indexOf(".jar!") != -1) {
            return;
        }
        icon = new ImageIcon(filePath1);
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        assertEquals("description", filePath1, icon.getDescription());
        assertEquals("width", ICON_SIZE_1, icon.getIconWidth());
        assertNotNull("file is found", url2);
        String filePath2 = url2.getPath();
        icon = new ImageIcon(filePath2);
        assertEquals("loaded", MediaTracker.ERRORED, icon.getImageLoadStatus());
        assertEquals("description", filePath2, icon.getDescription());
        assertEquals("width", -1, icon.getIconWidth());
    }

    /*
     * Class under test for void ImageIcon(String, String)
     */
    public void testImageIconStringString() {
        final String description1 = "bullet in your head";
        final String description2 = null;
        URL url1 = getClass().getResource(FILE_NAME_1);
        URL url2 = getClass().getResource(FILE_NAME_2);
        assertNotNull("file is found", url1);
        String filePath1 = url1.getPath();
        // preventing jar failures
        if (filePath1.indexOf(".jar!") != -1) {
            return;
        }
        icon = new ImageIcon(filePath1, description1);
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        assertEquals("description", description1, icon.getDescription());
        assertEquals("width", ICON_SIZE_1, icon.getIconWidth());
        assertNotNull("file is found", url2);
        String filePath2 = url2.getPath();
        icon = new ImageIcon(filePath2, description2);
        assertEquals("loaded", MediaTracker.ERRORED, icon.getImageLoadStatus());
        assertEquals("description", description2, icon.getDescription());
        assertEquals("width", -1, icon.getIconWidth());
    }

    /*
     * Class under test for void ImageIcon(URL)
     */
    public void testImageIconURL() {
        URL url1 = getClass().getResource(FILE_NAME_1);
        URL url2 = getClass().getResource(FILE_NAME_2);
        assertNotNull("file is found", url1);
        icon = new ImageIcon(url1);
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        assertEquals("description", url1.toString(), icon.getDescription());
        assertEquals("width", ICON_SIZE_1, icon.getIconWidth());
        assertNotNull("file is found", url2);
        icon = new ImageIcon(url2);
        assertEquals("loaded", MediaTracker.ERRORED, icon.getImageLoadStatus());
        assertEquals("description", url2.toString(), icon.getDescription());
        assertEquals("width", -1, icon.getIconWidth());
    }

    /*
     * Class under test for void ImageIcon(URL, String)
     */
    public void testImageIconURLString() {
        final String description1 = "bullet in your head";
        final String description2 = null;
        URL url1 = getClass().getResource(FILE_NAME_1);
        URL url2 = getClass().getResource(FILE_NAME_2);
        assertNotNull("file is found", url1);
        icon = new ImageIcon(url1, description1);
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        assertEquals("description", description1, icon.getDescription());
        assertEquals("width", ICON_SIZE_1, icon.getIconWidth());
        assertNotNull("file is found", url2);
        icon = new ImageIcon(url2, description2);
        assertEquals("loaded", MediaTracker.ERRORED, icon.getImageLoadStatus());
        assertEquals("description", description2, icon.getDescription());
        assertEquals("width", -1, icon.getIconWidth());
    }

    /*
     * Class under test for void ImageIcon(Image, String)
     */
    public void testImageIconImageString() {
        final String description1 = "bullet in your head";
        final String description2 = "born without a face";
        Image image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB) {
            @Override
            public Object getProperty(final String name, final ImageObserver observer) {
                return description1;
            }
        };
        icon = new ImageIcon(image1, description2);
        assertEquals("description", description2, icon.getDescription());
        icon = new ImageIcon(image1, null);
        assertNull("description", icon.getDescription());
        assertEquals("image", image1, icon.getImage());
    }

    /*
     * Class under test for void ImageIcon(byte[])
     */
    public void testImageIconbyteArray() throws IOException {
        InputStream stream1 = getClass().getResourceAsStream(FILE_NAME_1);
        assertTrue("file is found", stream1 != null);
        byte[] array1 = new byte[10000];
        icon = new ImageIcon(array1);
        assertEquals("loaded", MediaTracker.ERRORED, icon.getImageLoadStatus());
        assertNull("description", icon.getDescription());
        assertEquals("width", -1, icon.getIconWidth());
        int bytesRead = stream1.read(array1);
        assertEquals("array size", FILE_SIZE_1, bytesRead);
        icon = new ImageIcon(array1);
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        if (isHarmony()) {
            assertNotNull("description", icon.getDescription());
        }
        assertEquals("width", ICON_SIZE_1, icon.getIconWidth());
    }

    /*
     * Class under test for void ImageIcon(byte[], String)
     */
    public void testImageIconbyteArrayString() throws IOException {
        final String description1 = "bullet in your head";
        final String description2 = "born without a face";
        InputStream stream1 = getClass().getResourceAsStream(FILE_NAME_1);
        assertTrue("file is found", stream1 != null);
        byte[] array1 = new byte[10000];
        icon = new ImageIcon(array1, description1);
        assertEquals("loaded", MediaTracker.ERRORED, icon.getImageLoadStatus());
        assertEquals("description", description1, icon.getDescription());
        assertEquals("width", -1, icon.getIconWidth());
        int bytesRead = stream1.read(array1);
        assertEquals("array size", FILE_SIZE_1, bytesRead);
        icon = new ImageIcon(array1, description2);
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        assertEquals("description", description2, icon.getDescription());
        assertEquals("width", ICON_SIZE_1, icon.getIconWidth());
    }

    /*
     * Class under test for void ImageIcon(Image)
     */
    public void testImageIconImage() {
        final String description1 = "bullet in your head";
        Image image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB) {
            @Override
            public Object getProperty(final String name, final ImageObserver observer) {
                return description1;
            }
        };
        Image image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        icon = new ImageIcon(image1);
        assertEquals("description", description1, icon.getDescription());
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        assertEquals("image", image1, icon.getImage());
        icon = new ImageIcon(image2);
        assertNull("description", icon.getDescription());
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        assertEquals("image", image2, icon.getImage());
    }

    /*
     * Class under test for void ImageIcon()
     */
    public void testImageIcon() {
        icon = new ImageIcon();
        assertNull("image", icon.getImage());
        assertEquals("height", -1, icon.getIconHeight());
        assertEquals("width", -1, icon.getIconWidth());
        assertNull("description", icon.getDescription());
        assertEquals("loaded", 0, icon.getImageLoadStatus());
    }

    public void testGetAccessibleContext() {
        int width = 11;
        int height = 32;
        ImageIcon icon1 = new ImageIcon(new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB));
        ImageIcon icon2 = new ImageIcon(new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB));
        AccessibleContext accessible1 = icon1.getAccessibleContext();
        AccessibleContext accessible2 = icon1.getAccessibleContext();
        AccessibleContext accessible3 = icon2.getAccessibleContext();
        assertTrue("accessible", accessible1 instanceof ImageIcon.AccessibleImageIcon);
        assertSame("accessibles ain't unique", accessible1, accessible2);
        assertTrue("accessibles are unique", accessible1 != accessible3);
    }

    /**
     * this method is being tested by  testGetDescription();
     */
    public void testSetDescription() {
    }

    /*
     * Class under test for String toString()
     */
    public void testToString() {
        final String description = "bullet in your head";
        Image image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        icon = new ImageIcon(image1, description);
        assertEquals("string is not empty", description, icon.toString());
        icon = new ImageIcon(image1);
        assertTrue("string is not empty", icon.toString() != null
                && !icon.toString().equals(""));
    }

    public void testGetDescription() {
        final String description1 = "bullet in your head";
        final String description2 = "born without a face";
        final String description3 = "snakecharmer";
        Image image1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB) {
            @Override
            public Object getProperty(final String name, final ImageObserver observer) {
                return description1;
            }
        };
        Image image2 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB) {
            @Override
            public Object getProperty(final String name, final ImageObserver observer) {
                return description3;
            }
        };
        icon = new ImageIcon(image1);
        assertEquals("description", description1, icon.getDescription());
        icon.setDescription(description2);
        assertEquals("description", description2, icon.getDescription());
        icon.setDescription(null);
        assertNull("description", icon.getDescription());
        icon.setDescription(description2);
        assertEquals("description", description2, icon.getDescription());
        icon.setImage(image2);
        assertEquals("description", description2, icon.getDescription());
        icon = new ImageIcon(image2, description1);
        assertEquals("description", description1, icon.getDescription());
        icon = new ImageIcon(image2, null);
        assertNull("description", icon.getDescription());
    }

    public void testGetImageObserver() {
        ImageObserver observer1 = new JPanel();
        ImageObserver observer2 = new JPanel();
        int width = 111;
        int height = 235;
        icon = new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
        assertNull(icon.getImageObserver());
        icon.setImageObserver(observer1);
        assertEquals("observer", observer1, icon.getImageObserver());
        icon.setImageObserver(observer2);
        assertEquals("observer", observer2, icon.getImageObserver());
    }

    public void testSetImage() {
        int width = 111;
        int height = 235;
        Image image1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Image image2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        icon = new ImageIcon(image1);
        assertEquals("image", image1, icon.getImage());
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        icon.setImage(image2);
        assertEquals("image", image2, icon.getImage());
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        icon.setImage(image1);
        assertEquals("image", image1, icon.getImage());
        assertEquals("loaded", MediaTracker.COMPLETE, icon.getImageLoadStatus());
    }

    public void testLoadImage() {
        int width = 111;
        int height = 235;
        Image image1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        MyImageIcon icon = new MyImageIcon();
        assertEquals("load status", 0, icon.getImageLoadStatus());
        assertNull("image", icon.getImage());
        icon.loadImage(image1);
        assertEquals("load status", MediaTracker.COMPLETE, icon.getImageLoadStatus());
        assertNull("image", icon.getImage());
    }

    /**
     * this method is being tested by  testSetImage();
     */
    public void testGetImage() {
    }

    /**
     * this method is being tested by constructors testcases
     */
    public void testGetImageLoadStatus() {
    }

    public void testGetIconWidth() {
        int width = 111;
        int height = 235;
        icon = new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
        assertEquals("width", width, icon.getIconWidth());
        icon.setImage(new BufferedImage(7 * width, 4 * height, BufferedImage.TYPE_INT_RGB));
        assertEquals("width", 7 * width, icon.getIconWidth());
    }

    public void testGetIconHeight() {
        int width = 111;
        int height = 235;
        icon = new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
        assertEquals("height", height, icon.getIconHeight());
        icon.setImage(new BufferedImage(7 * width, 4 * height, BufferedImage.TYPE_INT_RGB));
        assertEquals("height", 4 * height, icon.getIconHeight());
        icon = new ImageIcon("");
        assertEquals("height", -1, icon.getIconHeight());
    }

    public void testStaticFields() {
        MyImageIcon myIcon = new MyImageIcon();
        assertNotNull(myIcon.getTracker());
        assertNotNull(myIcon.getComponent());
    }
    
    public void testAccessibleImageIcon() {
        int width = 111;
        int height = 235;
        icon = new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
        
        AccessibleContext ac = icon.getAccessibleContext();
        AccessibleIcon ai = (AccessibleIcon)ac;

        assertEquals(ac.getAccessibleChildrenCount(), 0);
        assertNull(ac.getAccessibleChild(0));
        assertNull(ac.getAccessibleChild(10));
        assertNull(ac.getAccessibleChild(-1));

        assertNull(ac.getAccessibleParent());
        
        ac.setAccessibleParent(new ImageIcon());
        assertNull(ac.getAccessibleParent());
        assertNull(ac.getAccessibleStateSet());

        assertEquals("width", width, ai.getAccessibleIconWidth());
        assertEquals("height", height, ai.getAccessibleIconHeight());
    }
}
