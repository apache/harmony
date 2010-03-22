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
 * Created on 29.11.2004

 */
package javax.swing.border;

import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;

public class EmptyBorderTest extends SwingTestCase {
    public void testIsBorderOpaque() {
        EmptyBorder border = new EmptyBorder(0, 1, 2, 3);
        assertFalse("EmptyBorder is not opaque ", border.isBorderOpaque());
    }

    /*
     * Class under test for void EmptyBorder(Insets)
     */
    public void testEmptyBorderInsets() {
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        EmptyBorder border = new EmptyBorder(new Insets(top, left, bottom, right));
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
    }

    /*
     * Class under test for void EmptyBorder(int, int, int, int)
     */
    public void testEmptyBorderintintintint() {
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        EmptyBorder border = new EmptyBorder(top, left, bottom, right);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
    }

    /*
     * Class under test for void EmptyBorder(int, int)
     */
    public void testEmptyBorderintint() {
        if (!isHarmony()) {
            return;
        }
        int top = 111;
        int bottom = 111;
        int left = 345;
        int right = 345;
        EmptyBorder border = new EmptyBorder(top, left);
        assertEquals(border.getBorderInsets(), new Insets(top, left, bottom, right));
    }

    /*
     * Class under test for Insets getBorderInsets(Component, Insets)
     */
    public void testGetBorderInsetsComponentInsets() {
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        EmptyBorder border = new EmptyBorder(top, left, bottom, right);
        Insets insets = new Insets(1, 1, 1, 1);
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(1, 1, 1, 1));
        border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
        insets = new Insets(1, 1, 1, 1);
        panel.setBorder(null);
        Insets newInsets = border.getBorderInsets(panel, insets);
        assertEquals("insets values coinside", top, newInsets.top);
        assertEquals("insets values coinside", left, newInsets.left);
        assertEquals("insets values coinside", right, newInsets.right);
        assertEquals("insets values coinside", bottom, newInsets.bottom);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
    }

    /*
     * Class under test for Insets getBorderInsets(Component)
     */
    public void testGetBorderInsetsComponent() {
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        EmptyBorder border = new EmptyBorder(top, left, bottom, right);
        Insets insets = null;
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(1, 1, 1, 1));
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
        panel.setBorder(null);
        insets = border.getBorderInsets(panel);
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
    }

    /*
     * Class under test for void paintBorder(Component, Graphics, int, int, int, int)
     *
     * since this method doesn't suppose to do anything,
     * i think the good decision is to leave this testcase empty
     */
    public void testPaintBorderComponentGraphicsintintintint() {
    }

    /*
     * Class under test for Insets getBorderInsets()
     */
    public void testGetBorderInsets() {
        int top = 100;
        int left = 200;
        int right = 300;
        int bottom = 400;
        EmptyBorder border = new EmptyBorder(top, left, bottom, right);
        Insets insets = border.getBorderInsets();
        assertEquals("insets values coinside", top, insets.top);
        assertEquals("insets values coinside", left, insets.left);
        assertEquals("insets values coinside", right, insets.right);
        assertEquals("insets values coinside", bottom, insets.bottom);
    }

    public void testReadWriteObject() throws Exception {
        EmptyBorder border1 = new EmptyBorder(10, 20, 30, 40);
        EmptyBorder border2 = new EmptyBorder(40, 50, 60, 70);
        EmptyBorder resurrectedBorder = (EmptyBorder) serializeObject(border1);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coinsides", resurrectedBorder.getBorderInsets(),
                border1.getBorderInsets());
        resurrectedBorder = (EmptyBorder) serializeObject(border2);
        assertNotNull(resurrectedBorder);
        assertEquals("Deserialized values coinsides", resurrectedBorder.getBorderInsets(),
                border2.getBorderInsets());
    }
}
