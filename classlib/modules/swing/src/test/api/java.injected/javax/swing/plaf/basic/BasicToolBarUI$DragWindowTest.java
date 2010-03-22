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
package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;

public class BasicToolBarUI$DragWindowTest extends SwingTestCase {
    private JToolBar toolBar;

    private BasicToolBarUI ui;

    private BasicToolBarUI.DragWindow dragWindow;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new BasicToolBarUI();
        toolBar = new JToolBar();
        toolBar.setUI(ui);
        JFrame frame = new JFrame();
        frame.getContentPane().add(toolBar);
        dragWindow = ui.createDragWindow(toolBar);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public BasicToolBarUI$DragWindowTest(final String name) {
        super(name);
    }

    public void testGetInsets() {
        assertEquals(new Insets(1, 1, 1, 1), dragWindow.getInsets());
    }

    public void testSetGetBorderColor() {
        dragWindow.setBorderColor(Color.red);
        assertSame(Color.red, dragWindow.getBorderColor());
    }

    public void testSetGetOffset() {
        Point offset = new Point(1, 2);
        dragWindow.setOffset(offset);
        assertSame(offset, dragWindow.getOffset());
    }

    public void testSetOrientation() {
        dragWindow.setSize(1, 2);
        dragWindow.setOrientation(SwingConstants.VERTICAL);
        assertEquals(new Dimension(1, 2), dragWindow.getSize());
    }

    public void testPaint() {
        // Note: painting code, cannot test
    }
}
