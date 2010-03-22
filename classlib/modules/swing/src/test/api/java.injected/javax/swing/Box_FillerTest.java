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
 * Created on 21.12.2004

 */
package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import javax.accessibility.AccessibleContext;

public class Box_FillerTest extends SwingTestCase {
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(Box_FillerTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testFiller() {
        Dimension minimumSize = new Dimension(100, 100);
        Dimension preferredSize = new Dimension(200, 200);
        Dimension maximumSize = new Dimension(300, 300);
        Box.Filler filler = new Box.Filler(minimumSize, preferredSize, maximumSize);
        assertEquals("Minimum size initialized correctly ", minimumSize, filler
                .getMinimumSize());
        assertEquals("Preferred size initialized correctly ", preferredSize, filler
                .getPreferredSize());
        assertEquals("Maximum size initialized correctly ", maximumSize, filler
                .getMaximumSize());
        assertFalse("Opaqueness initialized correctly", filler.isOpaque());
        filler = new Box.Filler(null, null, null);
        assertNull("Minimum size initialized correctly ", filler.getMinimumSize());
        assertNull("Preferred size initialized correctly ", filler.getPreferredSize());
        assertNull("Maximum size initialized correctly ", filler.getMaximumSize());
        assertFalse("Opaqueness initialized correctly", filler.isOpaque());
    }

    /*
     * Class under test for Dimension getMinimumSize()
     */
    public void testGetMinimumSize() {
        Dimension initMinimumSize = new Dimension(100, 100);
        Dimension initPreferredSize = new Dimension(200, 200);
        Dimension initMaximumSize = new Dimension(300, 300);
        Box.Filler filler = new Box.Filler(initMinimumSize, initPreferredSize, initMaximumSize);
        assertEquals("Minimum size initialized correctly ", initMinimumSize, filler
                .getMinimumSize());
        Dimension minimumSize = new Dimension(1000, 1000);
        filler.setMinimumSize(minimumSize);
        assertEquals("Minimum size is unchangeable", initMinimumSize, filler.getMinimumSize());
        filler.setMinimumSize(null);
        assertEquals("Minimum size is unchangeable ", initMinimumSize, filler.getMinimumSize());
    }

    /*
     * Class under test for Dimension getMaximumSize()
     */
    public void testGetMaximumSize() {
        Dimension initMinimumSize = new Dimension(100, 100);
        Dimension initPreferredSize = new Dimension(200, 200);
        Dimension initMaximumSize = new Dimension(300, 300);
        Box.Filler filler = new Box.Filler(initMinimumSize, initPreferredSize, initMaximumSize);
        assertEquals("Maximum size initialized correctly ", initMaximumSize, filler
                .getMaximumSize());
        Dimension maximumSize = new Dimension(1000, 1000);
        filler.setMaximumSize(maximumSize);
        assertEquals("Minimum size is unchangeable", initMaximumSize, filler.getMaximumSize());
        filler.setMaximumSize(null);
        assertEquals("Minimum size is unchangeable ", initMaximumSize, filler.getMaximumSize());
    }

    /*
     * Class under test for Dimension getPreferredSize()
     */
    public void testGetPreferredSize() {
        Dimension initMinimumSize = new Dimension(100, 100);
        Dimension initPreferredSize = new Dimension(200, 200);
        Dimension initMaximumSize = new Dimension(300, 300);
        Box.Filler filler = new Box.Filler(initMinimumSize, initPreferredSize, initMaximumSize);
        assertEquals("Preferred size initialized correctly ", initPreferredSize, filler
                .getPreferredSize());
        Dimension preferredSize = new Dimension(1000, 1000);
        filler.setPreferredSize(preferredSize);
        assertEquals("Minimum size is unchangeable", initPreferredSize, filler
                .getPreferredSize());
        filler.setPreferredSize(null);
        assertEquals("Minimum size is unchangeable ", initPreferredSize, filler
                .getPreferredSize());
    }

    /*
     * Class under test for AccessibleContext getAccessibleContext()
     */
    public void testGetAccessibleContext() {
        Component filler = Box.createVerticalGlue();
        AccessibleContext accessible = filler.getAccessibleContext();
        assertEquals("Accessible context is correct ", Box.Filler.AccessibleBoxFiller.class,
                accessible.getClass());
    }

    public void testChangeShape() {
        Dimension minimumSize = new Dimension(100, 100);
        Dimension preferredSize = new Dimension(200, 200);
        Dimension maximumSize = new Dimension(300, 300);
        Box.Filler filler = new Box.Filler(minimumSize, preferredSize, maximumSize);
        assertEquals("Minimum size initialized correctly ", minimumSize, filler
                .getMinimumSize());
        assertEquals("Preferred size initialized correctly ", preferredSize, filler
                .getPreferredSize());
        assertEquals("Maximum size initialized correctly ", maximumSize, filler
                .getMaximumSize());
        minimumSize = new Dimension(110, 110);
        preferredSize = new Dimension(220, 220);
        maximumSize = new Dimension(330, 330);
        filler.changeShape(minimumSize, preferredSize, maximumSize);
        assertEquals("Minimum size's changed correctly ", minimumSize, filler.getMinimumSize());
        assertEquals("Preferred size's changed correctly ", preferredSize, filler
                .getPreferredSize());
        assertEquals("Maximum size's changed correctly ", maximumSize, filler.getMaximumSize());
    }
}
