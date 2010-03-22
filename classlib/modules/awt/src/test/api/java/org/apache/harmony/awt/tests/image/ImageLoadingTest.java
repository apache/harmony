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

import java.awt.image.ImageProducer;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;
import tests.support.resource.Support_Resources;

public class ImageLoadingTest extends TestCase {

    // Regression test for HARMONY-1718
    public void test_getContent1718() throws IOException {
        URL url;

        url = Support_Resources.class
                .getResource(Support_Resources.RESOURCE_PACKAGE + "Harmony.GIF");
        assertTrue("Returned object doesn't implement ImageProducer interface",
                url.getContent() instanceof ImageProducer);

        url = Support_Resources.class
                .getResource(Support_Resources.RESOURCE_PACKAGE + "Harmony.jpg");
        assertTrue("Returned object doesn't implement ImageProducer interface",
                url.getContent() instanceof ImageProducer);

        url = Support_Resources.class
                .getResource(Support_Resources.RESOURCE_PACKAGE + "Harmony.png");
        assertTrue("Returned object doesn't implement ImageProducer interface",
                url.getContent() instanceof ImageProducer);
    }
}
