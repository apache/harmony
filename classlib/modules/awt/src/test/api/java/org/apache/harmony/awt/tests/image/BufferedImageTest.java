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
package org.apache.harmony.awt.tests.image;

import java.awt.Frame;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.swing.ImageIcon;
import junit.framework.TestCase;

public class BufferedImageTest extends TestCase {

	private final int EXP_WIDTH = 320;
	private final int EXP_HEIGHT = 182;

	public void testJpg() throws InterruptedException {
		decodeImage("utest.jpg");
	}

	public void testGif() throws InterruptedException {
		decodeImage("utest.gif");
	}

	public void testPng() throws InterruptedException {
		decodeImage("utest.png");
	}

	private final ClassLoader c = ClassLoader.getSystemClassLoader();

	private Image createImage(String name) {
		final URL path = c.getResource("../resources/images/" + name);
		assertNotNull("Resource not found: " + name, path); //$NON-NLS-1$
		return Toolkit.getDefaultToolkit().createImage(path);
	}

	private void decodeImage(String name) throws InterruptedException {
		final Image im = createImage(name);
		final BufferedImage bim = new BufferedImage(EXP_WIDTH, EXP_HEIGHT,
				BufferedImage.TYPE_INT_RGB);
		final Frame f = new Frame();
		final MediaTracker t = new MediaTracker(f);

		t.addImage(im, 0);
		t.waitForAll();

		assertEquals(EXP_WIDTH, im.getWidth(null));
		assertEquals(EXP_HEIGHT, im.getHeight(null));

		bim.getGraphics().drawImage(im, 0, 0, null);
		int rgbVal = bim.getRGB(0, 0);
		assertEquals(0xFFFFFFFF, rgbVal);
	}

	/**
	 * Regression test for HARMONY-3602
	 */
	public void testTerminate() {
		final Image img = createImage("test.gif");

		System.out.println(new ImageIcon(img)); // Loads the image.
		img.flush(); // ERROR: Calls Thread.currentThread().interrupt().
		assertFalse("Current thread is interrupted", Thread.currentThread()
				.isInterrupted());
	}
}
