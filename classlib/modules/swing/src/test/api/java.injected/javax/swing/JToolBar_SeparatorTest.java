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
 * @author Vadim L. Bogdanov
 */
package javax.swing;

import java.awt.Dimension;

public class JToolBar_SeparatorTest extends SwingTestCase {
    private JToolBar.Separator separator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        separator = new JToolBar.Separator(new Dimension(5, 10));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public JToolBar_SeparatorTest(String name) {
        super(name);
    }

    public void testGetMaximumSize() {
        assertEquals(separator.getSeparatorSize(), separator.getMaximumSize());
        separator.setOrientation(SwingConstants.VERTICAL);
        assertEquals(separator.getSeparatorSize(), separator.getMaximumSize());
    }

    public void testGetMinimumSize() {
        assertEquals(separator.getSeparatorSize(), separator.getMinimumSize());
        separator.setOrientation(SwingConstants.VERTICAL);
        assertEquals(separator.getSeparatorSize(), separator.getMinimumSize());
    }

    public void testGetPreferredSize() {
        assertEquals(separator.getSeparatorSize(), separator.getPreferredSize());
        separator.setOrientation(SwingConstants.VERTICAL);
        assertEquals(separator.getSeparatorSize(), separator.getPreferredSize());
    }

    public void testGetUIClassID() {
        assertEquals("ToolBarSeparatorUI", separator.getUIClassID());
    }

    public void testSeparator() {
        separator = new JToolBar.Separator();
        assertNotNull(separator.getUI());
        assertEquals(UIManager.get("ToolBar.separatorSize"), separator.getSeparatorSize());
        assertEquals(SwingConstants.HORIZONTAL, separator.getOrientation());
    }

    public void testSeparatorDimension() {
        Dimension dimension = new Dimension(1, 2);
        separator = new JToolBar.Separator(dimension);
        assertNotNull(separator.getUI());
        assertEquals(dimension, separator.getSeparatorSize());
        assertEquals(SwingConstants.HORIZONTAL, separator.getOrientation());
        separator = new JToolBar.Separator(null);
        assertNotNull(separator.getUI());
        assertEquals(UIManager.get("ToolBar.separatorSize"), separator.getSeparatorSize());
    }

    public void testSetGetSeparatorSize() {
        PropertyChangeController controller = new PropertyChangeController();
        separator.addPropertyChangeListener(controller);
        Dimension dimension = new Dimension(1, 2);
        separator.setSeparatorSize(dimension);
        assertFalse(controller.isChanged());
        assertSame(dimension, separator.getSeparatorSize());
        separator.setSeparatorSize(null);
        assertSame(dimension, separator.getSeparatorSize());
    }
}
