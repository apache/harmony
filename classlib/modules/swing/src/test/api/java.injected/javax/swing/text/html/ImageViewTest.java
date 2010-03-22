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
 * @author Alexey A. Ivanov
 */
package javax.swing.text.html;

import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.swing.BasicSwingTestCase;
import javax.swing.Icon;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.View;
import javax.swing.text.Position.Bias;

@SuppressWarnings({"deprecation", "serial"})
public class ImageViewTest extends BasicSwingTestCase {
    private static final String DEFAULT_SRC = "image.jpg";
    private static final String DEFAULT_ALT = "image description";
    /**
     * Natural size of the image in <code>IMAGE_BYTES</code>.
     */
    private static final int    IMAGE_SIZE  = 16;
    /**
     * Size specified with attributes, as <code>int</code>.
     */
    private static final int    SIZE        = 20;
    /**
     * Size specified with attributes, as <code>String</code>.
     */
    private static final String SIZE_VALUE  = "20";
    private static final String WIDTH_HTML  = "width=\"20";
    private static final String HEIGHT_HTML = "height=\"20";
    private static final String WIDTH_CSS   = "width: 20pt";
    private static final String HEIGHT_CSS  = "height: 20pt";

    /**
     * Test image in GIF format. It is written to a temporary file and loaded
     * into the view.
     */
    private static final short[] IMAGE_BYTES = {
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x10, 0x00, 0x10, 0x00, 0xf7,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0x80, 0x00,
        0x80, 0x80, 0x00, 0x00, 0x00, 0x80, 0x80, 0x00, 0x80, 0x00, 0x80,
        0x80, 0x80, 0x80, 0x80, 0xc0, 0xc0, 0xc0, 0xff, 0x00, 0x00, 0x00,
        0xff, 0x00, 0xff, 0xff, 0x00, 0x00, 0x00, 0xff, 0xff, 0x00, 0xff,
        0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x33, 0x00, 0x00, 0x66, 0x00,
        0x00, 0x99, 0x00, 0x00, 0xcc, 0x00, 0x00, 0xff, 0x00, 0x33, 0x00,
        0x00, 0x33, 0x33, 0x00, 0x33, 0x66, 0x00, 0x33, 0x99, 0x00, 0x33,
        0xcc, 0x00, 0x33, 0xff, 0x00, 0x66, 0x00, 0x00, 0x66, 0x33, 0x00,
        0x66, 0x66, 0x00, 0x66, 0x99, 0x00, 0x66, 0xcc, 0x00, 0x66, 0xff,
        0x00, 0x99, 0x00, 0x00, 0x99, 0x33, 0x00, 0x99, 0x66, 0x00, 0x99,
        0x99, 0x00, 0x99, 0xcc, 0x00, 0x99, 0xff, 0x00, 0xcc, 0x00, 0x00,
        0xcc, 0x33, 0x00, 0xcc, 0x66, 0x00, 0xcc, 0x99, 0x00, 0xcc, 0xcc,
        0x00, 0xcc, 0xff, 0x00, 0xff, 0x00, 0x00, 0xff, 0x33, 0x00, 0xff,
        0x66, 0x00, 0xff, 0x99, 0x00, 0xff, 0xcc, 0x00, 0xff, 0xff, 0x33,
        0x00, 0x00, 0x33, 0x00, 0x33, 0x33, 0x00, 0x66, 0x33, 0x00, 0x99,
        0x33, 0x00, 0xcc, 0x33, 0x00, 0xff, 0x33, 0x33, 0x00, 0x33, 0x33,
        0x33, 0x33, 0x33, 0x66, 0x33, 0x33, 0x99, 0x33, 0x33, 0xcc, 0x33,
        0x33, 0xff, 0x33, 0x66, 0x00, 0x33, 0x66, 0x33, 0x33, 0x66, 0x66,
        0x33, 0x66, 0x99, 0x33, 0x66, 0xcc, 0x33, 0x66, 0xff, 0x33, 0x99,
        0x00, 0x33, 0x99, 0x33, 0x33, 0x99, 0x66, 0x33, 0x99, 0x99, 0x33,
        0x99, 0xcc, 0x33, 0x99, 0xff, 0x33, 0xcc, 0x00, 0x33, 0xcc, 0x33,
        0x33, 0xcc, 0x66, 0x33, 0xcc, 0x99, 0x33, 0xcc, 0xcc, 0x33, 0xcc,
        0xff, 0x33, 0xff, 0x00, 0x33, 0xff, 0x33, 0x33, 0xff, 0x66, 0x33,
        0xff, 0x99, 0x33, 0xff, 0xcc, 0x33, 0xff, 0xff, 0x66, 0x00, 0x00,
        0x66, 0x00, 0x33, 0x66, 0x00, 0x66, 0x66, 0x00, 0x99, 0x66, 0x00,
        0xcc, 0x66, 0x00, 0xff, 0x66, 0x33, 0x00, 0x66, 0x33, 0x33, 0x66,
        0x33, 0x66, 0x66, 0x33, 0x99, 0x66, 0x33, 0xcc, 0x66, 0x33, 0xff,
        0x66, 0x66, 0x00, 0x66, 0x66, 0x33, 0x66, 0x66, 0x66, 0x66, 0x66,
        0x99, 0x66, 0x66, 0xcc, 0x66, 0x66, 0xff, 0x66, 0x99, 0x00, 0x66,
        0x99, 0x33, 0x66, 0x99, 0x66, 0x66, 0x99, 0x99, 0x66, 0x99, 0xcc,
        0x66, 0x99, 0xff, 0x66, 0xcc, 0x00, 0x66, 0xcc, 0x33, 0x66, 0xcc,
        0x66, 0x66, 0xcc, 0x99, 0x66, 0xcc, 0xcc, 0x66, 0xcc, 0xff, 0x66,
        0xff, 0x00, 0x66, 0xff, 0x33, 0x66, 0xff, 0x66, 0x66, 0xff, 0x99,
        0x66, 0xff, 0xcc, 0x66, 0xff, 0xff, 0x99, 0x00, 0x00, 0x99, 0x00,
        0x33, 0x99, 0x00, 0x66, 0x99, 0x00, 0x99, 0x99, 0x00, 0xcc, 0x99,
        0x00, 0xff, 0x99, 0x33, 0x00, 0x99, 0x33, 0x33, 0x99, 0x33, 0x66,
        0x99, 0x33, 0x99, 0x99, 0x33, 0xcc, 0x99, 0x33, 0xff, 0x99, 0x66,
        0x00, 0x99, 0x66, 0x33, 0x99, 0x66, 0x66, 0x99, 0x66, 0x99, 0x99,
        0x66, 0xcc, 0x99, 0x66, 0xff, 0x99, 0x99, 0x00, 0x99, 0x99, 0x33,
        0x99, 0x99, 0x66, 0x99, 0x99, 0x99, 0x99, 0x99, 0xcc, 0x99, 0x99,
        0xff, 0x99, 0xcc, 0x00, 0x99, 0xcc, 0x33, 0x99, 0xcc, 0x66, 0x99,
        0xcc, 0x99, 0x99, 0xcc, 0xcc, 0x99, 0xcc, 0xff, 0x99, 0xff, 0x00,
        0x99, 0xff, 0x33, 0x99, 0xff, 0x66, 0x99, 0xff, 0x99, 0x99, 0xff,
        0xcc, 0x99, 0xff, 0xff, 0xcc, 0x00, 0x00, 0xcc, 0x00, 0x33, 0xcc,
        0x00, 0x66, 0xcc, 0x00, 0x99, 0xcc, 0x00, 0xcc, 0xcc, 0x00, 0xff,
        0xcc, 0x33, 0x00, 0xcc, 0x33, 0x33, 0xcc, 0x33, 0x66, 0xcc, 0x33,
        0x99, 0xcc, 0x33, 0xcc, 0xcc, 0x33, 0xff, 0xcc, 0x66, 0x00, 0xcc,
        0x66, 0x33, 0xcc, 0x66, 0x66, 0xcc, 0x66, 0x99, 0xcc, 0x66, 0xcc,
        0xcc, 0x66, 0xff, 0xcc, 0x99, 0x00, 0xcc, 0x99, 0x33, 0xcc, 0x99,
        0x66, 0xcc, 0x99, 0x99, 0xcc, 0x99, 0xcc, 0xcc, 0x99, 0xff, 0xcc,
        0xcc, 0x00, 0xcc, 0xcc, 0x33, 0xcc, 0xcc, 0x66, 0xcc, 0xcc, 0x99,
        0xcc, 0xcc, 0xcc, 0xcc, 0xcc, 0xff, 0xcc, 0xff, 0x00, 0xcc, 0xff,
        0x33, 0xcc, 0xff, 0x66, 0xcc, 0xff, 0x99, 0xcc, 0xff, 0xcc, 0xcc,
        0xff, 0xff, 0xff, 0x00, 0x00, 0xff, 0x00, 0x33, 0xff, 0x00, 0x66,
        0xff, 0x00, 0x99, 0xff, 0x00, 0xcc, 0xff, 0x00, 0xff, 0xff, 0x33,
        0x00, 0xff, 0x33, 0x33, 0xff, 0x33, 0x66, 0xff, 0x33, 0x99, 0xff,
        0x33, 0xcc, 0xff, 0x33, 0xff, 0xff, 0x66, 0x00, 0xff, 0x66, 0x33,
        0xff, 0x66, 0x66, 0xff, 0x66, 0x99, 0xff, 0x66, 0xcc, 0xff, 0x66,
        0xff, 0xff, 0x99, 0x00, 0xff, 0x99, 0x33, 0xff, 0x99, 0x66, 0xff,
        0x99, 0x99, 0xff, 0x99, 0xcc, 0xff, 0x99, 0xff, 0xff, 0xcc, 0x00,
        0xff, 0xcc, 0x33, 0xff, 0xcc, 0x66, 0xff, 0xcc, 0x99, 0xff, 0xcc,
        0xcc, 0xff, 0xcc, 0xff, 0xff, 0xff, 0x00, 0xff, 0xff, 0x33, 0xff,
        0xff, 0x66, 0xff, 0xff, 0x99, 0xff, 0xff, 0xcc, 0xff, 0xff, 0xff,
        0x21, 0xf9, 0x04, 0x01, 0x00, 0x00, 0x10, 0x00, 0x2c, 0x00, 0x00,
        0x00, 0x00, 0x10, 0x00, 0x10, 0x00, 0x00, 0x08, 0x63, 0x00, 0xbf,
        0xfd, 0x1b, 0x48, 0x90, 0x5f, 0x3f, 0x82, 0x04, 0xbd, 0xfd, 0x13,
        0x88, 0xb0, 0x1f, 0x3f, 0x84, 0xff, 0x14, 0x0e, 0x64, 0x38, 0xd0,
        0x20, 0x42, 0x89, 0x04, 0x29, 0x3a, 0x4c, 0x08, 0x71, 0x62, 0xc5,
        0x83, 0x11, 0x3b, 0x66, 0xfc, 0xb7, 0x11, 0xa3, 0xc8, 0x85, 0x06,
        0x4d, 0x0e, 0xcc, 0xf1, 0x83, 0xa5, 0xcb, 0x6f, 0xde, 0x5a, 0xca,
        0x64, 0x39, 0x53, 0xa6, 0xb7, 0x6f, 0x2e, 0x67, 0x76, 0xf4, 0xe6,
        0x90, 0xe2, 0x49, 0x85, 0x16, 0x7d, 0x42, 0x94, 0xb8, 0x71, 0xa1,
        0x48, 0x8c, 0x16, 0x3d, 0x5e, 0x6c, 0xf8, 0x70, 0xe4, 0x40, 0x95,
        0x49, 0x47, 0xaa, 0x24, 0xd9, 0x14, 0xe1, 0xb7, 0x80, 0x00, 0x3b,
    };

    private HTMLEditorKit kit;
    private HTMLDocument doc;
    private Element img;
    private ImageView view;
    private AttributeSet attrs;

    private Icon noIcon;
    private int iconWidth;
    private int iconHeight;
    private static File imageFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setIgnoreNotImplemented(true);
        init();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (imageFile != null) {
            imageFile.delete();
        }
    }

    public void testImageView() {
        final Marker properties = new Marker();
        view = new ImageView(img) {
            @Override
            protected void setPropertiesFromAttributes() {
                properties.setOccurred();
                super.setPropertiesFromAttributes();
            }
        };
        if (isHarmony()) {
            assertTrue(properties.isOccurred());
        } else {
            assertFalse(properties.isOccurred());
        }
        assertSame(img, view.getElement());
    }

    public void testGetImage() {
        assertNull(view.getImageURL());
        assertNull(view.getImage());
    }

    public void testGetImage_Image() throws Exception {
        initImage();
        view.setLoadsSynchronously(false);
        assertEquals(imageFile.toURL(), view.getImageURL());
        assertNotNull(view.getImage());
        waitForImage();
    }

    // URL lacks protocol specification
    public void testGetImageURL() {
        assertNull(view.getImageURL());
    }

    // HTTP protocol
    public void testGetImageURL_HTTP() throws Exception {
        init("http://base.url.test/" + DEFAULT_SRC);
        assertEquals("http://base.url.test/" + DEFAULT_SRC,
                     view.getImageURL().toString());
    }

    // Unknown protocol
    public void testGetImageURL_IMG() throws Exception {
        init("img://" + DEFAULT_SRC);
        assertNull(view.getImageURL());
    }

    // doc.getBase() is used to construct absolute URL
    public void testGetImageURL_BASE01() throws Exception {
        doc.setBase(new URL("http://base.url.test/"));
        assertEquals("http://base.url.test/" + DEFAULT_SRC,
                     view.getImageURL().toString());
    }

    // doc.getBase() is used to construct absolute URL
    public void testGetImageURL_BASE02() throws Exception {
        assertNull(view.getImageURL());
        assertNull(view.getImage());
        doc.setBase(new URL("http://base.url.test/"));
        assertEquals("http://base.url.test/" + DEFAULT_SRC,
                     view.getImageURL().toString());
        assertNull(view.getImage());
    }

    // No src attribute is set on <img>
    public void testGetImageURL_Null() throws Exception {
        init(null, null);
        assertNull(view.getImageURL());
    }

    public void testGetLoadingImageIcon() {
        final Icon icon = view.getLoadingImageIcon();
        assertNotNull(icon);
        assertSame(icon, view.getLoadingImageIcon());
        assertSame(icon, new ImageView(img).getLoadingImageIcon());

        assertEquals(isHarmony() ? 20 : 38, icon.getIconWidth());
        assertEquals(isHarmony() ? 18 : 38, icon.getIconHeight());
    }

    public void testGetNoImageIcon() {
        final Icon icon = view.getNoImageIcon();
        assertNotNull(icon);
        assertSame(icon, view.getNoImageIcon());
        assertSame(icon, new ImageView(img).getNoImageIcon());

        assertEquals(isHarmony() ? 20 : 38, icon.getIconWidth());
        assertEquals(isHarmony() ? 18 : 38, icon.getIconHeight());
    }

    public void testSetLoadsSynchronously() {
        assertFalse(view.getLoadsSynchronously());

        view.setLoadsSynchronously(true);
        assertTrue(view.getLoadsSynchronously());

        view.setLoadsSynchronously(false);
        assertFalse(view.getLoadsSynchronously());
    }

    public void testGetLoadsSynchronously() {
        assertFalse(view.getLoadsSynchronously());
    }

    // Has null image, alt text is not null
    public void testGetPreferredSpan01() {
        assertNull(attrs.getAttribute(CSS.Attribute.WIDTH));
        assertNull(attrs.getAttribute(CSS.Attribute.HEIGHT));

        final FontMetrics metrics = Toolkit.getDefaultToolkit()
                                    .getFontMetrics(view.getStyleSheet()
                                                    .getFont(attrs));
        assertEquals(metrics.stringWidth(DEFAULT_ALT) + iconWidth,
                     (int)view.getPreferredSpan(View.X_AXIS));
        if (isHarmony()) {
            assertEquals(Math.max(metrics.getHeight(), iconHeight),
                         (int)view.getPreferredSpan(View.Y_AXIS));
        } else {
            assertEquals(metrics.getHeight() + iconHeight,
                         (int)view.getPreferredSpan(View.Y_AXIS));
        }
    }

    // Has null image, alt text is null
    public void testGetPreferredSpan02() throws Exception {
        init(null, null);

        assertEquals(iconWidth,
                     (int)view.getPreferredSpan(View.X_AXIS));
        assertEquals(iconHeight,
                     (int)view.getPreferredSpan(View.Y_AXIS));
    }

    // Image has width and height HTML attributes set
    public void testGetPreferredSpan03() throws Exception {
        init(DEFAULT_SRC + "\" " + WIDTH_HTML + "\" " + HEIGHT_HTML, null);
        assertEquals(SIZE_VALUE,
                     img.getAttributes().getAttribute(HTML.Attribute.WIDTH));
        assertEquals(SIZE_VALUE,
                     img.getAttributes().getAttribute(HTML.Attribute.HEIGHT));

        assertEquals(iconWidth,
                     (int)view.getPreferredSpan(View.X_AXIS));
        assertEquals(iconHeight,
                     (int)view.getPreferredSpan(View.Y_AXIS));
    }

    // Image has width and height CSS attributes set
    public void testGetPreferredSpan04() throws Exception {
        init(DEFAULT_SRC + "\" style=\"" + WIDTH_CSS + "; " + HEIGHT_CSS, null);
        assertEquals(SIZE_VALUE + "pt",
                     img.getAttributes().getAttribute(CSS.Attribute.WIDTH)
                     .toString());
        assertEquals(SIZE_VALUE + "pt",
                     img.getAttributes().getAttribute(CSS.Attribute.HEIGHT)
                     .toString());

        assertEquals(iconWidth,
                     (int)view.getPreferredSpan(View.X_AXIS));
        assertEquals(iconHeight,
                     (int)view.getPreferredSpan(View.Y_AXIS));
    }

    // Loads external image. No attributes specified.
//    public void testGetPreferredSpan05() throws Exception {
//        initImage();
//        waitForImage();
//        assertNotNull(view.getImage());
//
//        // These assertions should pass but they don't
//        assertEquals(IMAGE_SIZE,
//                     (int)view.getPreferredSpan(View.X_AXIS));
//        assertEquals(IMAGE_SIZE,
//                     (int)view.getPreferredSpan(View.Y_AXIS));
//    }

    // Loads external image. HTML attributes are set.
    public void testGetPreferredSpan06() throws Exception {
        initImage();
        init(imageFile.toURL().toString() + "\" "
             + WIDTH_HTML + "\" " + HEIGHT_HTML, null);
        waitForImage();

        assertEquals(SIZE_VALUE,
                     img.getAttributes().getAttribute(HTML.Attribute.WIDTH));
        assertEquals(SIZE_VALUE,
                     img.getAttributes().getAttribute(HTML.Attribute.HEIGHT));

        assertNotNull(view.getImage());

        assertEquals(SIZE,
                     (int)view.getPreferredSpan(View.X_AXIS));
        assertEquals(SIZE,
                     (int)view.getPreferredSpan(View.Y_AXIS));
    }

    public void testGetToolTipText01() {
        assertEquals(view.getAltText(),
                     view.getToolTipText(0, 0, new Rectangle()));
    }

    public void testGetToolTipText02() {
        final Marker marker = new Marker();
        view = new ImageView(img) {
            @Override
            public String getAltText() {
                marker.setOccurred();
                return null;
            }
        };
        assertFalse(marker.isOccurred());
        assertNull(view.getToolTipText(0, 0, new Rectangle()));
        assertTrue(marker.isOccurred());
    }

    public void testGetToolTipText03() {
        final String alt = view.getAltText();
        final Rectangle shape = new Rectangle(121, 219, 574, 38);

        assertEquals(alt, view.getToolTipText(-121, -219, shape));
        assertEquals(alt, view.getToolTipText(shape.x + shape.width + 41,
                                              shape.y + shape.height + 94,
                                              shape));
    }

    public void testGetAltText01() {
        assertEquals(DEFAULT_ALT, view.getAltText());
    }

    public void testGetAltText02() throws Exception {
        init(null, null);
        assertNull(view.getAltText());
    }

    public void testGetAltText03() throws Exception {
        final Marker marker = new Marker();
        final AttributeSet vas = new SimpleAttributeSet() {
            @Override
            public Object getAttribute(final Object key) {
                marker.setOccurred();
                return attrs.getAttribute(key);
            }
        };
        view = new ImageView(img) {
            @Override
            public AttributeSet getAttributes() {
                return vas;
            }
        };
        marker.reset();
        view.getAltText();
        assertFalse(marker.isOccurred());
    }

    public void testGetAltText04() throws Exception {
        final Marker marker = new Marker();

        final String value = "attribute value";
        final AttributeSet eas = new SimpleAttributeSet() {
            @Override
            public Object getAttribute(final Object key) {
                marker.setOccurred();
                if (key == HTML.Attribute.ALT) {
                    return value;
                }
                return attrs.getAttribute(key);
            }
        };
        final Element e = new Element() {
            public AttributeSet getAttributes() {
                return eas;
            }

            public Document getDocument() {
                return img.getDocument();
            }
            public Element getElement(int index) {
                return img.getElement(index);
            }
            public int getElementCount() {
                return img.getElementCount();
            }
            public int getElementIndex(int offset) {
                return img.getElementIndex(offset);
            }
            public int getEndOffset() {
                return img.getEndOffset();
            }
            public String getName() {
                return img.getName();
            }
            public Element getParentElement() {
                return img.getParentElement();
            }
            public int getStartOffset() {
                return img.getStartOffset();
            }
            public boolean isLeaf() {
                return img.isLeaf();
            }
        };

        view = new ImageView(e);
        marker.reset();
        assertSame(value, view.getAltText());
        assertTrue(marker.isOccurred());
    }

    public void testSetParent() {
        final Marker properties = new Marker();
        view = new ImageView(img) {
            @Override
            protected void setPropertiesFromAttributes() {
                properties.setOccurred();
                super.setPropertiesFromAttributes();
            }
        };
        if (isHarmony()) {
            assertTrue(properties.isOccurred());
        } else {
            assertFalse(properties.isOccurred());
        }

        view.setParent(new InlineView(img));

        if (isHarmony()) {
            assertTrue(properties.isOccurred());
        } else {
            assertFalse(properties.isOccurred());
        }
    }

    public void testSetSize() throws Exception {
        init(null, null);
        assertEquals(iconWidth,
                     (int)view.getPreferredSpan(View.X_AXIS));
        assertEquals(iconHeight,
                     (int)view.getPreferredSpan(View.Y_AXIS));

        view.setSize(231, 231);

        assertEquals(iconWidth,
                     (int)view.getPreferredSpan(View.X_AXIS));
        assertEquals(iconHeight,
                     (int)view.getPreferredSpan(View.Y_AXIS));
    }

    // modelToView(int, Shape, Bias)
    public void testModelToView() throws Exception {
        init(null, null);
        assertEquals(iconWidth,
                     (int)view.getPreferredSpan(View.X_AXIS));
        assertEquals(1, view.getEndOffset() - view.getStartOffset());
        Rectangle shape = new Rectangle(21, 33, 132, 129);
        assertEquals(new Rectangle(21, 33, 0, 129),
                     view.modelToView(view.getStartOffset(), shape,
                                      Bias.Forward));
        assertEquals(new Rectangle(21, 33, 0, 129),
                     view.modelToView(view.getStartOffset(), shape,
                                      Bias.Backward));
        assertEquals(new Rectangle(21 + shape.width, 33, 0, 129),
                     view.modelToView(view.getEndOffset(), shape,
                                      Bias.Forward));
        assertEquals(new Rectangle(21 + shape.width, 33, 0, 129),
                     view.modelToView(view.getEndOffset(), shape,
                                      Bias.Backward));
    }

    public void testModelToViewWithAltText() throws Exception {
        final FontMetrics metrics = Toolkit.getDefaultToolkit()
                                    .getFontMetrics(view.getStyleSheet()
                                                    .getFont(attrs));
        int textWidth = metrics.stringWidth(view.getAltText());
        assertEquals(iconWidth + textWidth,
                     (int)view.getPreferredSpan(View.X_AXIS));
        assertEquals(1, view.getEndOffset() - view.getStartOffset());
        Rectangle shape = new Rectangle(21, 33, 132, 129);
        assertEquals(new Rectangle(21, 33, 0, 129),
                     view.modelToView(view.getStartOffset(), shape,
                                      Bias.Forward));
        assertEquals(new Rectangle(21, 33, 0, 129),
                     view.modelToView(view.getStartOffset(), shape,
                                      Bias.Backward));


        assertEquals(new Rectangle(21 + shape.width, 33, 0, 129),
                     view.modelToView(view.getEndOffset(), shape,
                                      Bias.Forward));
        assertEquals(new Rectangle(21 + shape.width, 33, 0, 129),
                     view.modelToView(view.getEndOffset(), shape,
                                      Bias.Backward));
    }

    // viewToModel(float, float, Shape, Bias[]
    public void testViewToModel() {
        Rectangle shape = new Rectangle(21, 33, 132, 129);
        Bias[] bias = new Bias[1];
        assertEquals(view.getStartOffset(),
                     view.viewToModel(0, 0, shape, bias));
        assertSame(Bias.Forward, bias[0]);                      bias[0] = null;
        assertEquals(view.getStartOffset(),
                     view.viewToModel(shape.x + shape.width / 2 - 1, shape.y,
                                      shape, bias));
        assertSame(Bias.Forward, bias[0]);                      bias[0] = null;


        assertEquals(view.getStartOffset(),
                     view.viewToModel(shape.x + shape.width / 2 + 1, shape.y,
                                      shape, bias));
        assertSame(Bias.Forward, bias[0]);                      bias[0] = null;
        assertEquals(view.getEndOffset(),
                     view.viewToModel(shape.x + shape.width + 31,
                                      shape.y + shape.height + 33,
                                      shape, bias));
        assertSame(Bias.Backward, bias[0]);                     bias[0] = null;


        assertEquals(view.getStartOffset(),
                     view.viewToModel(shape.x + shape.width - 1,
                                      shape.y, shape, bias));
        assertSame(Bias.Forward, bias[0]);                      bias[0] = null;
        assertEquals(view.getEndOffset(),
                     view.viewToModel(shape.x + shape.width,
                                      shape.y, shape, bias));
        assertSame(Bias.Backward, bias[0]);                     bias[0] = null;
    }

    public void testGetAlignment() {
        assertEquals(0.5f, view.getAlignment(View.X_AXIS), 0);
        assertEquals(1.0f, view.getAlignment(View.Y_AXIS), 0);
    }

    public void testGetAttributes() {
        assertSame(attrs, view.getAttributes());
        assertNotSame(attrs, view.getElement().getAttributes());
    }

    public void testChangedUpdate() {
        final Marker properties = new Marker(true);
        view = new ImageView(img) {
            @Override
            protected void setPropertiesFromAttributes() {
                properties.setOccurred();
                super.setPropertiesFromAttributes();
            }
        };

        if (isHarmony()) {
            assertTrue(properties.isOccurred());
        } else {
            assertFalse(properties.isOccurred());
        }

        view.changedUpdate(new DocumentEvent() {
            public int getOffset() {
                return img.getStartOffset();
            }
            public int getLength() {
                return img.getEndOffset() - img.getStartOffset();
            }
            public Document getDocument() {
                return doc;
            }
            public EventType getType() {
                return EventType.CHANGE;
            }
            public ElementChange getChange(Element elem) {
                return null;
            }
        }, new Rectangle(), null);
        if (isHarmony()) {
            assertTrue(properties.isOccurred());
        } else {
            assertFalse(properties.isOccurred());
        }
    }

    public void testGetStyleSheet() {
        assertSame(doc.getStyleSheet(), view.getStyleSheet());
    }

    public void testSetPropertiesFromAttributes() {
        final Marker color = new Marker(true);
        view = new ImageView(img) {
            private AttributeSet attributes;
            @Override
            public AttributeSet getAttributes() {
                if (attributes == null) {
                    attributes = new SimpleAttributeSet(super.getAttributes()) {
                        @Override
                        public Object getAttribute(Object key) {
                            if (key == CSS.Attribute.COLOR) {
                                color.setOccurred();
                            }
                            return super.getAttribute(key);
                        }
                    };
                }
                return attributes;
            }
        };
        color.reset();
        view.setPropertiesFromAttributes();
        assertTrue(color.isOccurred());
    }

//    public void testPaint() {
//
//    }

    private void init() throws Exception {
        init(DEFAULT_SRC);
    }

    private void init(final String src) throws Exception {
        init(src, DEFAULT_ALT);
    }

    private void init(final String src, final String alt) throws Exception {
        kit = new HTMLEditorKit();
        doc = (HTMLDocument)kit.createDefaultDocument();
        StringReader reader = new StringReader("<html><head></head>" +
               "<body>" +
               "<img" +
               (src != null ? " src=\"" + src + "\"" : "") +
               (alt != null ? " alt=\"" + alt + "\"" : "") + ">" +
               "</body></html>");
        kit.read(reader, doc, 0);

        img = doc.getCharacterElement(1);
        assertEquals(HTML.Tag.IMG.toString(), img.getName());

        view = new ImageView(img);
        attrs = view.getAttributes();

        noIcon = view.getNoImageIcon();
        iconWidth = noIcon.getIconWidth();
        iconHeight = noIcon.getIconHeight();
    }

    private void initImage() throws Exception {
        writeImage();
        init(imageFile.toURL().toString());
    }

    private static void writeImage() throws IOException {
        imageFile = File.createTempFile("imageViewTest", ".gif");
        FileOutputStream out = new FileOutputStream(imageFile);
        for (int i = 0; i < IMAGE_BYTES.length; i++) {
            out.write(IMAGE_BYTES[i]);
        }
        out.close();
    }

    private void waitForImage() throws Exception {
        final Image image = view.getImage();

        int w = -1;
        int h = -1;
        while (w == -1 || h == -1) {
            w = image.getWidth(null);
            h = image.getHeight(null);
            Thread.sleep(1000);
        };
        assertEquals(IMAGE_SIZE, w);
        assertEquals(IMAGE_SIZE, h);
    }
}
