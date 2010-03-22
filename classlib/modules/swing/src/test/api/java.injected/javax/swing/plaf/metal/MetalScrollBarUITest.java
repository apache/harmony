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
package javax.swing.plaf.metal;

import java.awt.Dimension;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;

public class MetalScrollBarUITest extends SwingTestCase {
    private MetalScrollBarUI barUI;

    private JScrollBar bar;

    @Override
    protected void setUp() throws Exception {
        bar = new JScrollBar();
        barUI = new MetalScrollBarUI();
        bar.setUI(barUI);
    }

    @Override
    protected void tearDown() throws Exception {
        barUI = null;
        bar = null;
    }

    public void testGetPreferredSize() throws Exception {
        bar.getLayout().layoutContainer(bar);
        assertEquals(new Dimension(17, 34), barUI.getPreferredSize(null));
        assertEquals(new Dimension(17, 16), barUI.increaseButton.getPreferredSize());
        assertEquals(new Dimension(17, 16), barUI.decreaseButton.getPreferredSize());
    }

    public void testCreateButtons() throws Exception {
        assertNotNull(barUI.increaseButton);
        assertNotNull(barUI.decreaseButton);
        assertFalse(barUI.increaseButton == barUI
                .createIncreaseButton(SwingConstants.HORIZONTAL));
        assertFalse(barUI.decreaseButton == barUI
                .createDecreaseButton(SwingConstants.HORIZONTAL));
    }

    public void testCreateUI() throws Exception {
        assertFalse(MetalScrollBarUI.createUI(bar) == MetalScrollBarUI.createUI(bar));
    }

    public void testCreatePropertyChangeListener() throws Exception {
        assertNotNull(barUI.createPropertyChangeListener());
        assertFalse(barUI.createPropertyChangeListener() == barUI
                .createPropertyChangeListener());
    }
}
