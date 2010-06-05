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
 * @author Dmitry A. Durnev
 */
package java.awt;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import junit.framework.TestCase;

/**
 * ScrollPaneAdjustableTest
 */
public class ScrollPaneAdjustableTest extends TestCase {
    ScrollPane scrollPane;
    Frame f;
    ScrollPaneAdjustable hAdjustable, vAdjustable;
    private final int HSIZE = 1500;
    private final int VSIZE = 200;

    @SuppressWarnings("deprecation")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        f = new Frame();
        scrollPane = new ScrollPane();
        hAdjustable = (ScrollPaneAdjustable) scrollPane.getHAdjustable();
        checkAdjustable(hAdjustable, Adjustable.HORIZONTAL, 0, 0, 0, 0, 1, 1);
        vAdjustable = (ScrollPaneAdjustable) scrollPane.getVAdjustable();
        checkAdjustable(vAdjustable, Adjustable.VERTICAL, 0, 0, 0, 0, 1, 1);
        Button b = new Button();
        b.setPreferredSize(new Dimension(HSIZE, VSIZE));
        scrollPane.add(b);
        f.add(scrollPane);
        f.show();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (f != null) {
            f.dispose();
        }
    }

    private void checkAdjustable(Adjustable adj, int orient, int value, int vis,
                            int min, int max, int unit, int block) {
        assertEquals(orient, adj.getOrientation());
        assertEquals(value, adj.getValue());
        assertEquals(vis, adj.getVisibleAmount());
        assertEquals(min, adj.getMinimum());
        assertEquals(max, adj.getMaximum());
        assertEquals(unit, adj.getUnitIncrement());
        assertEquals(block, adj.getBlockIncrement());
    }

    public final void testToString() {
        assertEquals("toString() format is correct",
                     vAdjustable.getClass().getName() + "[" +
                     vAdjustable.paramString() + "]",
                     vAdjustable.toString());
    }

    public final void testGetValue() {
        assertEquals(0, vAdjustable.getValue());
        assertEquals(0, hAdjustable.getValue());
    }

    public final void testSetValue() {
        int val = Integer.MIN_VALUE;
        vAdjustable.setValue(val);
        assertEquals(vAdjustable.getMinimum(), vAdjustable.getValue());
        vAdjustable.setValue(val = (VSIZE / 2));
        assertEquals(val, vAdjustable.getValue());
        vAdjustable.setValue(val = VSIZE);
        assertEquals(val - vAdjustable.getVisibleAmount(),
                     vAdjustable.getValue());
        vAdjustable.setValue(val = Integer.MAX_VALUE);
        assertEquals(vAdjustable.getMaximum() - vAdjustable.getVisibleAmount(),
                     vAdjustable.getValue());
    }

    public final void testGetBlockIncrement() {
        int vis = vAdjustable.getVisibleAmount();
        assertEquals(getBlockIncrFromVis(vis),
                     vAdjustable.getBlockIncrement());
        vis = hAdjustable.getVisibleAmount();
        assertEquals(getBlockIncrFromVis(vis), hAdjustable.getBlockIncrement());
    }

    private int getBlockIncrFromVis(int vis) {
        return Math.max(1, vis * 9 / 10);
    }

    private int getVisFromSize(int size, int gap) {
        return Math.max(1, size - gap);
    }
    public final void testGetMaximum() {
        assertEquals(VSIZE, vAdjustable.getMaximum());
        assertEquals(HSIZE, hAdjustable.getMaximum());
    }

    public final void testGetMinimum() {
        assertEquals(0, vAdjustable.getMinimum());
        assertEquals(0, hAdjustable.getMinimum());
    }

    public final void testGetOrientation() {
        assertEquals(Adjustable.HORIZONTAL, hAdjustable.getOrientation());
        assertEquals(Adjustable.VERTICAL, vAdjustable.getOrientation());
    }

    public final void testGetUnitIncrement() {
        assertEquals(1, vAdjustable.getUnitIncrement());
        assertEquals(1, hAdjustable.getUnitIncrement());
    }

    public final void testGetValueIsAdjusting() {
        assertFalse(vAdjustable.getValueIsAdjusting());
        assertFalse(hAdjustable.getValueIsAdjusting());
    }

    public final void testGetVisibleAmount() {
        int size = scrollPane.getHeight();
        Insets insets = scrollPane.getInsets();
        int vGap = insets.bottom + insets.top;
        int hGap = insets.left + insets.right;
        assertEquals(getVisFromSize(size, vGap), vAdjustable.getVisibleAmount());
        size = scrollPane.getWidth();
        assertEquals(getVisFromSize(size, hGap), hAdjustable.getVisibleAmount());
    }

    public final void testParamString() {
        String hStr = hAdjustable.paramString();
        String vStr = vAdjustable.paramString();
        assertTrue(vStr.indexOf("vertical") >= 0);
        assertTrue(hStr.indexOf("horizontal") >= 0);
        assertTrue(vStr.indexOf(",val=0") > 0);
        assertTrue(vStr.indexOf(",vis=" + vAdjustable.getVisibleAmount()) > 0);
        assertTrue(vStr.indexOf(",[" + vAdjustable.getMinimum() +
                               ".." + vAdjustable.getMaximum() + "]") > 0);
        assertTrue(vStr.indexOf(",unit=" + vAdjustable.getUnitIncrement()) > 0);
        assertTrue(vStr.indexOf(",block=" + vAdjustable.getBlockIncrement()) > 0);
        assertTrue(vStr.indexOf(",isAdjusting=false") > 0);
    }

    public final void testSetBlockIncrement() {
        int blockIncr = 25;
        hAdjustable.setBlockIncrement(blockIncr);
        assertEquals(blockIncr, hAdjustable.getBlockIncrement());

        hAdjustable.setBlockIncrement(blockIncr = Integer.MIN_VALUE);
        assertEquals(blockIncr, hAdjustable.getBlockIncrement());
        hAdjustable.setBlockIncrement(blockIncr = Integer.MAX_VALUE);
        assertEquals(blockIncr, hAdjustable.getBlockIncrement());
    }

    public final void testSetMaximum() {
        boolean errorCatched = false;
        try {
            hAdjustable.setMaximum(256);
        } catch (AWTError err) {
            errorCatched = true;
        }
        assertTrue(errorCatched);
    }

    public final void testSetMinimum() {
        boolean errorCatched = false;
        try {
            vAdjustable.setMinimum(-256);
        } catch (AWTError err) {
            errorCatched = true;
        }
        assertTrue(errorCatched);
    }

    public final void testSetUnitIncrement() {
        int unitIncr = 10;
        vAdjustable.setUnitIncrement(unitIncr);
        assertEquals(unitIncr, vAdjustable.getUnitIncrement());

        vAdjustable.setUnitIncrement(unitIncr = Integer.MIN_VALUE);
        assertEquals(unitIncr, vAdjustable.getUnitIncrement());
        vAdjustable.setUnitIncrement(unitIncr = Integer.MAX_VALUE);
        assertEquals(unitIncr, vAdjustable.getUnitIncrement());
    }

    public final void testSetValueIsAdjusting() {
        vAdjustable.setValueIsAdjusting(true);
        assertTrue(vAdjustable.getValueIsAdjusting());
        vAdjustable.setValueIsAdjusting(false);
        assertFalse(vAdjustable.getValueIsAdjusting());
    }

    public final void testSetVisibleAmount() {
        boolean errorCatched = false;
        try {
            hAdjustable.setVisibleAmount(HSIZE / 2);
        } catch (AWTError err) {
            errorCatched = true;
        }
        assertTrue(errorCatched);
    }

    public void testAddGetRemoveAdjustmentListener() {
        assertEquals(0, vAdjustable.getAdjustmentListeners().length);

        AdjustmentListener listener = new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent ae) {
            }
        };
        vAdjustable.addAdjustmentListener(listener);
        assertEquals(1, vAdjustable.getAdjustmentListeners().length);
        assertSame(listener, vAdjustable.getAdjustmentListeners()[0]);

        vAdjustable.removeAdjustmentListener(listener);
        assertEquals(0, vAdjustable.getAdjustmentListeners().length);
    }

}
