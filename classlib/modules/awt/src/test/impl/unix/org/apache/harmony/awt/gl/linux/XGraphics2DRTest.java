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
 * @author Oleg V. Khaschansky
 */

package org.apache.harmony.awt.gl.linux;

import junit.framework.TestCase;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.VolatileImage;

public class XGraphics2DRTest extends TestCase {
    public void testNullClip() {
        try {
            GraphicsConfiguration gconf =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            VolatileImage vim = gconf.createCompatibleVolatileImage(10, 10);
            Graphics2D g2 = (Graphics2D) vim.getGraphics();
            g2.setClip(null);
        } catch(Exception e) {
            e.printStackTrace();
            fail("Cannot set null clip");
        }
    }

    public void testCopyArea() {
        GraphicsConfiguration gconf =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        VolatileImage vim = gconf.createCompatibleVolatileImage(20, 20);
        Graphics2D g2 = (Graphics2D) vim.getGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0,0,20,20);
        g2.setColor(Color.RED);
        g2.fillRect(2,2,1,1);
        g2.copyArea(2,2,1,1,2,4);
        int pix[] = vim.getSnapshot().getRaster().getPixel(4,6,(int [])null);
        assertEquals("copyArea failed for untranslated graphics", pix[0],255);
        assertEquals("copyArea failed for untranslated graphics", pix[1],0);
        assertEquals("copyArea failed for untranslated graphics", pix[2],0);
        g2.setTransform(AffineTransform.getTranslateInstance(2,2));
        g2.copyArea(0,0,1,1,4,8);
        pix = vim.getSnapshot().getRaster().getPixel(6,10,(int [])null);
        assertEquals("copyArea failed for translated graphics", pix[0],255);
        assertEquals("copyArea failed for translated graphics", pix[1],0);
        assertEquals("copyArea failed for translated graphics", pix[2],0);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(XGraphics2DRTest.class);
    }
}
