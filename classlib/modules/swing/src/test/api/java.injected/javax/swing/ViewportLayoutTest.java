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
 * @author Sergey Burlak
 */
package javax.swing;

import java.awt.Dimension;

public class ViewportLayoutTest extends SwingTestCase {
    private ViewportLayout layout;

    private JViewport viewport;

    private JLabel label;

    @Override
    public void setUp() {
        label = new JLabel();
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < 1000; i++) {
            b.append("w" + i);
        }
        label.setText(b.toString());
        viewport = new JViewport();
        viewport.add(label);
        layout = (ViewportLayout) viewport.getLayout();
    }

    @Override
    public void tearDown() {
        layout = null;
        viewport = null;
        label = null;
    }

    public void testPreferredSize() throws Exception {
        assertEquals(label.getPreferredSize(), layout.preferredLayoutSize(viewport));
    }

    public void testMinimumSize() throws Exception {
        assertEquals(new Dimension(4, 4), layout.minimumLayoutSize(viewport));
    }
}
