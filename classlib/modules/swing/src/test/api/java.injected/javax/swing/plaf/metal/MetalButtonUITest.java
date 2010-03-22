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
 * @author Alexander T. Simbirtsev
 * Created on 22.02.2005

 */
package javax.swing.plaf.metal;

import java.awt.Color;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicButtonUITest;

public class MetalButtonUITest extends BasicButtonUITest {
    protected MetalButtonUI metalUI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new MetalButtonUI();
        metalUI = (MetalButtonUI) ui;
    }

    @Override
    public void testCreateUI() {
        assertTrue("created UI is not null", null != MetalButtonUI.createUI(new JButton()));
        assertTrue("created UI is of the proper class",
                MetalButtonUI.createUI(null) instanceof MetalButtonUI);
        assertTrue("created UI is of unique", MetalButtonUI.createUI(null) == MetalButtonUI
                .createUI(null));
    }

    public void testMetalButtonUI() {
        assertNull(metalUI.disabledTextColor);
        assertNull(metalUI.focusColor);
        assertNull(metalUI.selectColor);
    }

    public void testGetDisabledTextColor() {
        assertTrue("DisabledTextColor is ColorUIResource",
                metalUI.getDisabledTextColor() instanceof ColorUIResource);
        assertTrue("DisabledTextColor is ColorUIResource",
                metalUI.disabledTextColor instanceof ColorUIResource);
        metalUI.disabledTextColor = Color.WHITE;
        assertSame("DisabledTextColor", Color.WHITE, metalUI.disabledTextColor);
        assertTrue("DisabledTextColor is ColorUIResource",
                metalUI.getDisabledTextColor() instanceof ColorUIResource);
        assertNotSame("DisabledTextColor", Color.WHITE, metalUI.disabledTextColor);
        final ColorUIResource red = new ColorUIResource(Color.RED);
        metalUI.disabledTextColor = red;
        assertNotSame("DisabledTextColor", red, metalUI.getDisabledTextColor());
        metalUI.disabledTextColor = null;
        assertNotNull("DisabledTextColor", metalUI.getDisabledTextColor());
        UIManager.put("Button.disabledText", red);
        metalUI.disabledTextColor = null;
        assertSame("DisabledTextColor", red, metalUI.getDisabledTextColor());
    }

    public void testGetFocusColor() {
        assertTrue("FocusColor is ColorUIResource",
                metalUI.getFocusColor() instanceof ColorUIResource);
        assertTrue("FocusColor is ColorUIResource",
                metalUI.focusColor instanceof ColorUIResource);
        metalUI.focusColor = Color.WHITE;
        assertEquals("FocusColor", Color.WHITE, metalUI.focusColor);
        assertTrue("FocusColor is ColorUIResource",
                metalUI.getFocusColor() instanceof ColorUIResource);
        final ColorUIResource red = new ColorUIResource(Color.RED);
        metalUI.focusColor = red;
        assertNotSame(red, metalUI.getFocusColor());
        metalUI.disabledTextColor = null;
        assertNotNull(metalUI.getFocusColor());
    }

    public void testGetSelectColor() {
        assertTrue("SelectColor is ColorUIResource",
                metalUI.getSelectColor() instanceof ColorUIResource);
        assertTrue("SelectColor is ColorUIResource",
                metalUI.selectColor instanceof ColorUIResource);
        metalUI.selectColor = Color.WHITE;
        assertEquals("SelectColor", Color.WHITE, metalUI.selectColor);
        assertTrue("FocusColor is ColorUIResource",
                metalUI.getSelectColor() instanceof ColorUIResource);
        final ColorUIResource red = new ColorUIResource(Color.RED);
        metalUI.selectColor = red;
        assertNotSame(red, metalUI.getSelectColor());
        metalUI.selectColor = null;
        assertNotNull(metalUI.getSelectColor());
    }
}
