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

package java.awt;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class Graphics2DTest extends TestCase {
    private Frame frame;
    
    protected void setUp() throws Exception {
        super.setUp();
        frame = new Frame();
        frame.addNotify();
    }

    protected void tearDown() throws Exception {
        if (frame != null) {
            frame.dispose();
        }        
        super.tearDown();
    }
    
    public void testSetPaintScreen() {
        // regression test for HARMONY-1448        
        Graphics2D g2d = (Graphics2D) frame.getGraphics();
        Paint paint = g2d.getPaint();
        assertNotNull(paint);
        g2d.setPaint(null);
        assertNotNull(g2d.getPaint());        
    }
    
    public void testSetPaintImage() {
        // regression test for HARMONY-1448
        int imgType = BufferedImage.TYPE_INT_ARGB;
        BufferedImage img = new BufferedImage(100, 100, imgType);
        Graphics2D g2d = (Graphics2D) img.getGraphics();
        Paint paint = g2d.getPaint();
        assertNotNull(paint);
        g2d.setPaint(null);
        assertNotNull(g2d.getPaint());        
    }

    public void testGetRenderingHint() {
		// Regression test for HARMONY-4799
		final Graphics2D g2d = (Graphics2D) frame.getGraphics();

		assertEquals(
				g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING),
				RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
		assertEquals(g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING),
				RenderingHints.VALUE_ANTIALIAS_OFF);
		assertEquals(g2d.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL),
				RenderingHints.VALUE_STROKE_DEFAULT);
	}
    
    public void testSetRenderingHint() {
		// Regression test for HARMONY-4920
		final Graphics2D g2d = (Graphics2D) frame.getGraphics();
		final Map<RenderingHints.Key, Object> m = new HashMap<RenderingHints.Key, Object>();

		m.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		m.put(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

		g2d.setRenderingHints(m);

		assertEquals(RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY, g2d
				.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION));
		assertEquals(RenderingHints.VALUE_TEXT_ANTIALIAS_OFF, g2d
				.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
		assertEquals(RenderingHints.VALUE_ANTIALIAS_OFF, g2d
				.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
		assertEquals(RenderingHints.VALUE_STROKE_DEFAULT, g2d
				.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL));
	}
}
