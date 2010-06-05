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

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

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
}
