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
 * @author Pavel Dolgov
 */
package java.awt;

import junit.framework.TestCase;

public class ToolkitRTest extends TestCase {

    public void testGetScreenInsets() {
        // regression test HARMONY-1339
        Toolkit tlk = Toolkit.getDefaultToolkit();
        GraphicsConfiguration gc = new Frame().getGraphicsConfiguration();
        Insets screenInsets = tlk.getScreenInsets(gc);
        assertNotNull("screen insets are not null", screenInsets);
    }

    public void testGetScreenInsetsNull() {
        boolean npe = false;
        Toolkit tlk = Toolkit.getDefaultToolkit();
        GraphicsConfiguration gc = null;
        try {
            tlk.getScreenInsets(gc);
        } catch (NullPointerException e) {
            npe = true;
        }
        assertTrue(npe);
    }

    public void testGetPropertyWithNullName() {
        boolean npe = false;
        try {
            Toolkit.getProperty(null, "text");
        } catch (NullPointerException e) {
            npe = true;
        }
        assertTrue(npe);
    }

    public void testInitializeDesktopProperties() {
        // Regression for HARMONY-1483
        Toolkit.getDefaultToolkit().initializeDesktopProperties();
    }
}
