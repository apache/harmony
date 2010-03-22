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
package javax.swing.plaf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingTestCase;

public class UIResourceTest extends SwingTestCase {
    public void testIconUIResource() {
        Icon imageIcon = new Icon() {
            public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            }

            public int getIconWidth() {
                return 0;
            }

            public int getIconHeight() {
                return 0;
            }
        };
        IconUIResource icon = new IconUIResource(imageIcon);
        assertEquals(imageIcon.getIconHeight(), icon.getIconHeight());
        assertEquals(imageIcon.getIconWidth(), icon.getIconWidth());
    }

    public void testNullDelegate() {
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new BorderUIResource(null);
            }
        });
        testExceptionalCase(new IllegalArgumentCase() {
            @Override
            public void exceptionalAction() throws Exception {
                new IconUIResource(null);
            }
        });
    }

    public void testBorderUIResourceBlackLine() {
        assertNotNull(BorderUIResource.getBlackLineBorderUIResource());
        assertTrue(BorderUIResource.getBlackLineBorderUIResource() instanceof BorderUIResource.LineBorderUIResource);
        assertTrue(BorderUIResource.getBlackLineBorderUIResource() == BorderUIResource
                .getBlackLineBorderUIResource());
        assertNotNull(BorderUIResource.getBlackLineBorderUIResource());
        BorderUIResource.LineBorderUIResource border = (BorderUIResource.LineBorderUIResource) BorderUIResource
                .getBlackLineBorderUIResource();
        assertFalse(border.getRoundedCorners());
        assertTrue(border.isBorderOpaque());
        assertEquals(new Insets(1, 1, 1, 1), border.getBorderInsets(newJComponent()));
        assertEquals(new Color(0, 0, 0), border.getLineColor());
        assertEquals(1, border.getThickness());
    }

    public void testBorderUIResourceEtched() {
        assertNotNull(BorderUIResource.getEtchedBorderUIResource());
        assertTrue(BorderUIResource.getEtchedBorderUIResource() instanceof BorderUIResource.EtchedBorderUIResource);
        assertTrue(BorderUIResource.getEtchedBorderUIResource() == BorderUIResource
                .getEtchedBorderUIResource());
        assertNotNull(BorderUIResource.getEtchedBorderUIResource());
        BorderUIResource.EtchedBorderUIResource border = (BorderUIResource.EtchedBorderUIResource) BorderUIResource
                .getEtchedBorderUIResource();
        assertTrue(border.isBorderOpaque());
        assertEquals(new Insets(2, 2, 2, 2), border.getBorderInsets(newJComponent()));
        assertEquals(1, border.getEtchType());
        assertNull(border.getHighlightColor());
        assertNull(border.getShadowColor());
    }

    public void testBorderUIResourceLoweredBevel() {
        assertNotNull(BorderUIResource.getLoweredBevelBorderUIResource());
        assertTrue(BorderUIResource.getLoweredBevelBorderUIResource() instanceof BorderUIResource.BevelBorderUIResource);
        assertTrue(BorderUIResource.getLoweredBevelBorderUIResource() == BorderUIResource
                .getLoweredBevelBorderUIResource());
        checkBevelBorderAttrs((BorderUIResource.BevelBorderUIResource) BorderUIResource
                .getLoweredBevelBorderUIResource(), 1);
    }

    public void testBorderUIResourceRaisedBevel() {
        assertNotNull(BorderUIResource.getRaisedBevelBorderUIResource());
        assertTrue(BorderUIResource.getRaisedBevelBorderUIResource() instanceof BorderUIResource.BevelBorderUIResource);
        assertTrue(BorderUIResource.getRaisedBevelBorderUIResource() == BorderUIResource
                .getRaisedBevelBorderUIResource());
        checkBevelBorderAttrs((BorderUIResource.BevelBorderUIResource) BorderUIResource
                .getRaisedBevelBorderUIResource(), 0);
    }

    public void testColorUIResource() {
        Color testColor = new Color(1, 2, 3);
        ColorUIResource c = new ColorUIResource(testColor);
        assertEquals(testColor.getRGB(), c.getRGB());
        assertEquals(testColor.getRed(), c.getRed());
        assertEquals(testColor.getGreen(), c.getGreen());
        assertEquals(testColor.getBlue(), c.getBlue());
        assertEquals(testColor.getAlpha(), c.getAlpha());
    }

    public void testDimensionUIResource() {
        DimensionUIResource d = new DimensionUIResource(1, 2);
        assertTrue(1 == d.getWidth());
        assertTrue(2 == d.getHeight());
    }

    public void testFontUIResource() {
        Font font = new Font("Dialog", Font.BOLD, 1);
        FontUIResource f = new FontUIResource(font);
        assertEquals("Dialog", f.getName());
        assertEquals(1, f.getSize());
        assertEquals(Font.BOLD, f.getStyle());
    }

    public void testInsetsUIResource() {
        InsetsUIResource ins = new InsetsUIResource(1, 2, 3, 4);
        assertEquals(1, ins.top);
        assertEquals(2, ins.left);
        assertEquals(3, ins.bottom);
        assertEquals(4, ins.right);
    }

    private void checkBevelBorderAttrs(final BorderUIResource.BevelBorderUIResource border,
            final int type) {
        assertNotNull(border);
        assertTrue(border.isBorderOpaque());
        assertEquals(type, border.getBevelType());
        assertEquals(new Insets(2, 2, 2, 2), border.getBorderInsets(newJComponent()));
        assertNull(border.getHighlightInnerColor());
        assertNull(border.getHighlightOuterColor());
        assertNull(border.getShadowInnerColor());
        assertNull(border.getShadowOuterColor());
    }

    private JComponent newJComponent() {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
        };
    }
}
