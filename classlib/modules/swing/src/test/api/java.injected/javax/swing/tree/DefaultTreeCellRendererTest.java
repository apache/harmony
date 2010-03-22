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
package javax.swing.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.BasicSwingTestCase;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

public class DefaultTreeCellRendererTest extends BasicSwingTestCase {
    private DefaultTreeCellRenderer renderer;

    public DefaultTreeCellRendererTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        renderer = new DefaultTreeCellRenderer();
    }

    @Override
    protected void tearDown() throws Exception {
        renderer = null;
    }

    public void testDefaultTreeCellRenderer() throws Exception {
        assertFalse(renderer.selected);
        assertFalse(renderer.hasFocus);
        assertNotNull(renderer.closedIcon);
        assertNotNull(renderer.leafIcon);
        assertNotNull(renderer.openIcon);
        assertSame(UIManager.getColor("Tree.selectionForeground"), renderer.textSelectionColor);
        assertSame(UIManager.getColor("Tree.foreground"), renderer.textNonSelectionColor);
        assertSame(UIManager.getColor("Tree.selectionBackground"),
                renderer.backgroundSelectionColor);
        assertSame(UIManager.getColor("Tree.background"), renderer.backgroundNonSelectionColor);
        assertSame(UIManager.getColor("Tree.selectionBorderColor"),
                renderer.borderSelectionColor);
        assertEquals(SwingConstants.LEFT, renderer.getHorizontalAlignment());
        assertEquals(SwingConstants.TRAILING, renderer.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, renderer.getVerticalAlignment());
        assertEquals(SwingConstants.CENTER, renderer.getVerticalTextPosition());
    }

    public void testGetDefaultOpenIcon() throws Exception {
        assertNotNull(renderer.getDefaultOpenIcon());
        assertSame(renderer.getDefaultOpenIcon(), renderer.openIcon);
        assertSame(UIManager.getIcon("Tree.openIcon"), renderer.openIcon);
    }

    public void testGetDefaultClosedIcon() throws Exception {
        assertNotNull(renderer.getDefaultClosedIcon());
        assertSame(renderer.getDefaultClosedIcon(), renderer.closedIcon);
        assertSame(UIManager.getIcon("Tree.closedIcon"), renderer.closedIcon);
    }

    public void testGetDefaultLeafIcon() throws Exception {
        assertNotNull(renderer.getDefaultLeafIcon());
        assertSame(renderer.getDefaultLeafIcon(), renderer.leafIcon);
        assertSame(UIManager.getIcon("Tree.leafIcon"), renderer.leafIcon);
    }

    public void testGetSetOpenIcon() throws Exception {
        assertSame(renderer.getDefaultOpenIcon(), renderer.getOpenIcon());
        Icon icon = createTestIcon();
        renderer.setOpenIcon(icon);
        assertSame(icon, renderer.openIcon);
        assertSame(icon, renderer.getOpenIcon());
        renderer.setOpenIcon(null);
        assertNull(renderer.getOpenIcon());
    }

    public void testGetSetClosedIcon() throws Exception {
        assertSame(renderer.getDefaultClosedIcon(), renderer.getClosedIcon());
        Icon icon = createTestIcon();
        renderer.setClosedIcon(icon);
        assertSame(icon, renderer.closedIcon);
        assertSame(icon, renderer.getClosedIcon());
        renderer.setClosedIcon(null);
        assertNull(renderer.getClosedIcon());
    }

    public void testGetSetLeafIcon() throws Exception {
        assertSame(renderer.getDefaultLeafIcon(), renderer.getLeafIcon());
        Icon icon = createTestIcon();
        renderer.setLeafIcon(icon);
        assertSame(icon, renderer.leafIcon);
        assertSame(icon, renderer.getLeafIcon());
        renderer.setLeafIcon(null);
        assertNull(renderer.getLeafIcon());
    }

    public void testGetSetTextSelectionColor() throws Exception {
        assertSame(renderer.textSelectionColor, renderer.getTextSelectionColor());
        renderer.setTextSelectionColor(Color.BLUE);
        assertSame(Color.BLUE, renderer.getTextSelectionColor());
        assertSame(renderer.textSelectionColor, renderer.getTextSelectionColor());
    }

    public void testGetSetTextNonSelectionColor() throws Exception {
        assertSame(renderer.textNonSelectionColor, renderer.getTextNonSelectionColor());
        renderer.setTextNonSelectionColor(Color.MAGENTA);
        assertSame(Color.MAGENTA, renderer.getTextNonSelectionColor());
        assertSame(renderer.textNonSelectionColor, renderer.getTextNonSelectionColor());
    }

    public void testGetSetBackgroundSelectionColor() throws Exception {
        assertSame(renderer.backgroundSelectionColor, renderer.getBackgroundSelectionColor());
        renderer.setBackgroundSelectionColor(Color.GREEN);
        assertSame(Color.GREEN, renderer.getBackgroundSelectionColor());
        assertSame(renderer.backgroundSelectionColor, renderer.getBackgroundSelectionColor());
    }

    public void testGetSetBackgroundNonSelectionColor() throws Exception {
        assertSame(renderer.backgroundNonSelectionColor, renderer
                .getBackgroundNonSelectionColor());
        renderer.setBackgroundNonSelectionColor(Color.GRAY);
        assertSame(Color.GRAY, renderer.getBackgroundNonSelectionColor());
        assertSame(renderer.backgroundNonSelectionColor, renderer
                .getBackgroundNonSelectionColor());
    }

    public void testGetSetBorderSelectionColor() throws Exception {
        assertSame(renderer.borderSelectionColor, renderer.getBorderSelectionColor());
        renderer.setBorderSelectionColor(Color.YELLOW);
        assertSame(Color.YELLOW, renderer.getBorderSelectionColor());
        assertSame(renderer.borderSelectionColor, renderer.getBorderSelectionColor());
    }

    public void testGetSetFont() throws Exception {
        assertNull(renderer.getFont());
        renderer.setFont(null);
        assertNull(renderer.getFont());
        renderer.setFont(new FontUIResource("font", 10, 0));
        assertNull(renderer.getFont());
        Font userFont = new Font("font", 20, 1);
        renderer.setFont(userFont);
        assertSame(userFont, renderer.getFont());
    }

    public void testSetBackground() throws Exception {
        assertNull(renderer.getBackground());
        renderer.setBackground(null);
        assertNull(renderer.getBackground());
        renderer.setBackground(new ColorUIResource(Color.BLUE));
        assertNull(renderer.getBackground());
        renderer.setBackground(Color.RED);
        assertSame(Color.RED, renderer.getBackground());
    }

    public void testGetTreeCellRendererComponent() throws Exception {
        JTree tree = new JTree();
        tree.setFont(new Font("font", 20, 1));
        tree.setBackground(Color.MAGENTA);
        assertSame(renderer, renderer.getTreeCellRendererComponent(tree, "value", false, false,
                false, 0, false));
        assertFalse(renderer.isOpaque());
        assertNull(renderer.getBackground());
        assertSame(renderer.textNonSelectionColor, renderer.getForeground());
        assertEquals("value", renderer.getText());
        assertSame(tree.getFont(), renderer.getFont());
        assertSame(renderer.getDefaultClosedIcon(), renderer.getIcon());
        assertNull(renderer.getBorder());
        renderer.getTreeCellRendererComponent(tree, "value", true, true, false, 0, false);
        assertFalse(renderer.isOpaque());
        assertNull(renderer.getBackground());
        assertSame(renderer.textSelectionColor, renderer.getForeground());
        assertEquals("value", renderer.getText());
        assertSame(tree.getFont(), renderer.getFont());
        assertSame(renderer.getDefaultOpenIcon(), renderer.getIcon());
        assertNull(renderer.getBorder());
        renderer.getTreeCellRendererComponent(tree, "value", true, true, true, 0, true);
        assertFalse(renderer.isOpaque());
        assertNull(renderer.getBackground());
        assertSame(renderer.textSelectionColor, renderer.getForeground());
        assertEquals("value", renderer.getText());
        assertSame(tree.getFont(), renderer.getFont());
        assertSame(renderer.getDefaultLeafIcon(), renderer.getIcon());
        assertNull(renderer.getBorder());
        renderer.setBackgroundSelectionColor(Color.RED);
        renderer.getTreeCellRendererComponent(tree, "value", true, true, true, 0, true);
        if (isHarmony()) {
            assertSame(Color.RED, renderer.getBackground());
        } else {
            assertNull(renderer.getBackground());
        }
    }

    public void testPaint() throws Exception {
        renderer.paint(createTestGraphics());
    }

    public void testGetPreferredSize() throws Exception {
        renderer.getTreeCellRendererComponent(new JTree(), "value", true, true, true, 0, false);
        JLabel label = new JLabel("value", renderer.getDefaultLeafIcon(), SwingConstants.LEFT);
        label.setFont(renderer.getFont());
        Dimension baseDimension = label.getPreferredSize();
        if (isHarmony()) {
            assertEquals(new Dimension(baseDimension.width + 2, baseDimension.height),
                    renderer.getPreferredSize());
        } else {
            assertEquals(new Dimension(baseDimension.width + 3, baseDimension.height),
                    renderer.getPreferredSize());
        }
    }

    private Icon createTestIcon() {
        return new Icon() {
            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            }

            public int getIconWidth() {
                return 10;
            }

            public int getIconHeight() {
                return 10;
            }
        };
    }
}
