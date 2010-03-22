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
 * @author Anton Avtamonov
 */
package javax.swing;

public class JComponentRTest extends SwingTestCase {
    public void testPaintDoubleBufferedForInvisibleComponent() throws Exception {
        JButton b = new JButton();
        b.paintDoubleBuffered(createTestGraphics());
    }

    public void testResetKeyboardActions() throws Exception {
        JComponent c = new JComponent() {
            private static final long serialVersionUID = 1L;
        };
        c.resetKeyboardActions();
    }

    public void testSetBounds() throws Throwable {
        final Marker marker = new Marker();
        final JComponent button = new JButton("JButton") {
            private static final long serialVersionUID = 1L;

            @Override
            public void revalidate() {
                marker.setOccurred();
                super.revalidate();
            }
        };
        marker.reset();
        button.setSize(50, 500);
        assertFalse(marker.isOccurred());
    }
}
