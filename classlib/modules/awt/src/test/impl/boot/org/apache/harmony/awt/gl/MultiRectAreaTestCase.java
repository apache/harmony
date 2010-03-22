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
 * @author Denis M. Kishenko
 */
package org.apache.harmony.awt.gl;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Tools;
import java.awt.geom.PathIteratorTestCase;
import java.awt.image.BufferedImage;
import java.net.URL;

import org.apache.harmony.awt.gl.MultiRectArea;


public class MultiRectAreaTestCase extends PathIteratorTestCase {

    static Color colorBack = Color.white;
    static Color colorOverride = Color.black;
    
    String shapePath, outputPath;

    public MultiRectAreaTestCase(String name) {
        super(name);
       
        String classPath = "../resources/shapes/" + Tools.getClasstPath(this.getClass());
        URL url = ClassLoader.getSystemClassLoader().getResource(classPath);

        assertNotNull("Path not found " + classPath, url);
        shapePath = url.getPath();
        outputPath = shapePath + "output/";
    }

    Color getRectColor(int index) {
        return Tools.MultiRectArea.color[index % Tools.MultiRectArea.color.length];
    }

    private String concat(String msg1, String msg2) {
        if (msg1 == null) {
            return msg2;
        }
        return msg1 + " " + msg2;
    }

    public void checkValidation(MultiRectArea area, String areaName) {
        Rectangle[] rect = area.getRectangles();
        if (rect.length == 0) {
            return;
        }

        // Check Negative values
        for(int i = 0; i < rect.length; i++) {
            assertTrue(concat(areaName, "Negative or zero width #" + i), rect[i].width > 0);
            assertTrue(concat(areaName, "Negative or zero height #" + i), rect[i].height > 0);
        }

        // Check order
        for(int i = 1; i < rect.length; i++) {
            if (rect[i - 1].y > rect[i].y) {
                fail(concat(areaName, "Invalid Y order #" + i));
            }
            if (rect[i - 1].y == rect[i].y) {
                if (rect[i - 1].x + rect[i - 1].width > rect[i].x) {
                    fail(concat(areaName, "Invalid X order #" + i));
                }
            }
        }

        // Check override
        Rectangle bounds = area.getBounds();
        int width = bounds.x + bounds.width + 50;
        int height = bounds.y + bounds.height + 50;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setColor(colorBack);
        g.fillRect(0, 0, width, height);

        boolean override = false;
        for(int i = 0; i < rect.length; i++) {

            // Fill rect
            for(int x = rect[i].x; x < rect[i].x + rect[i].width; x++) {
                for(int y = rect[i].y; y < rect[i].y + rect[i].height; y++) {
                    if (img.getRGB(x, y) == colorBack.getRGB()) {
                        img.setRGB(x, y, getRectColor(i).getRGB());
                    } else {
                        override = true;
                        img.setRGB(x, y, colorOverride.getRGB());
                    }
                }
            }
        }

        if (areaName != null) {
            Tools.BufferedImage.saveIcon(img, outputPath + areaName + ".ico");
        }

        if (override) {
            fail(concat(areaName, "Overrided rectangles"));
        }
    }

    public void checkArea(String areaName, MultiRectArea area, int[] buf) {
        checkValidation(area, areaName);
        Rectangle[] rect = new Rectangle[buf.length / 4];
        int j = 0;
        for(int i = 0; i < rect.length; i++) {
            rect[i] = new Rectangle(
                    buf[j],
                    buf[j + 1],
                    buf[j + 2] - buf[j] + 1,
                    buf[j + 3] - buf[j + 1] + 1);
            j += 4;
        }
        assertEquals(areaName, rect, area.getRectangles());
    }

    String getSubArray(int[] buf, int k) {
        int i1 = Math.max(k - 2, 0);
        int i2 = Math.min(k + 2, buf.length - 1);
        String s = "";
        for(int i = i1; i <= i2; i++) {
            if (i == k) {
                s += "[" + buf[i] + "]";
            } else {
                s += buf[i];
            }
            if (i < i2) {
                s += ",";
            }
        }
        if (i1 > 0) {
            s =  ".." + s;
        }
        if (i2 < buf.length - 1) {
            s = s + "..";
        }
        return s;
    }

    public void assertEquals(int[] a1, int a2[]) {
        if (a1.length != a2.length) {
            fail("Nonequal arrays length");
        }
        for(int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                fail("Element #" + i + " expected <" + getSubArray(a1, i) + "> but was <" + getSubArray(a2, i) + ">");
            }
        }
    }

    public void assertEquals(Rectangle[] a1, Rectangle[] a2) {
        assertEquals(null, a1, a2);
    }

    public void assertEquals(String areaName, Rectangle[] a1, Rectangle[] a2) {
        if (a1.length != a2.length) {
            fail(concat(areaName, "Nonequal arrays length " + a1.length + " and " + a2.length));
        }
        for(int i = 0; i < a1.length; i++) {
            assertEquals(concat(areaName, "Element #" + i), a1[i], a2[i]);
        }
    }


}
