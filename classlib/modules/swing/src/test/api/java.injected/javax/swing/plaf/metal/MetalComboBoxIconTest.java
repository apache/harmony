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
package javax.swing.plaf.metal;

import javax.swing.JLabel;
import javax.swing.SwingTestCase;
import tests.support.Support_Excludes;

public class MetalComboBoxIconTest extends SwingTestCase {
    private MetalComboBoxIcon icon;

    public MetalComboBoxIconTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        icon = new MetalComboBoxIcon();
    }

    @Override
    protected void tearDown() throws Exception {
        icon = null;
    }

    public void testGetIconHeight() throws Exception {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(5, icon.getIconHeight());
    }

    public void testGetIconWidth() throws Exception {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        assertEquals(10, icon.getIconWidth());
    }

    public void testPaintIcon() throws Exception {
        if (Support_Excludes.isExcluded()) {
            return;
        }

        icon.paintIcon(new JLabel(), createTestGraphics(), 0, 0);
    }
}
