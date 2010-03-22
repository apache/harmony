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
 * ScrollbarTest
 */
public class ScrollbarTest extends TestCase {
    Scrollbar scrollbar;
    private boolean eventProcessed;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scrollbar = new Scrollbar();
    }


    private void checkScrollbar(int orientation, int value, int visible,
                                int min, int max, int unitInc, int blockInc) {
        assertNotNull(scrollbar);
        assertEquals(orientation, scrollbar.getOrientation());
        assertEquals(value, scrollbar.getValue());
        assertEquals(visible, scrollbar.getVisibleAmount());
        assertEquals(min, scrollbar.getMinimum());
        assertEquals(max, scrollbar.getMaximum());
        assertEquals(unitInc, scrollbar.getUnitIncrement());
        assertEquals(blockInc, scrollbar.getBlockIncrement());
    }

    /**
     * @param orientation
     * @param val
     * @param vis
     * @param min
     * @param max
     */
    private void checkScrollbar(int orientation, int val, int vis, int min, int max) {
        checkScrollbar(orientation, val, vis, min, max, 1, 10);
    }

    private void checkScrollbar(int val, int vis, int min, int max) {
        checkScrollbar(Scrollbar.VERTICAL, val, vis, min, max, 1, 10);
    }

    private void checkScrollbar() {
        checkScrollbar(Scrollbar.VERTICAL);
    }

    private void checkScrollbar(int orientation) {
        checkScrollbar(orientation, 0, 10, 0, 100, 1, 10);
    }

    public final void testParamString() {
        String str = scrollbar.paramString();
        assertTrue(str.indexOf("scrollbar0") >= 0);
        assertTrue(str.indexOf(",val=0") > 0);
        assertTrue(str.indexOf(",vis=10") > 0);
        assertTrue(str.indexOf(",min=0") > 0);
        assertTrue(str.indexOf(",max=100") > 0);
        assertTrue(str.indexOf(",vert") > 0);
        assertTrue(str.indexOf(",isAdjusting=false") > 0);
    }

    public final void testScrollbar() {
        checkScrollbar();
    }

    public final void testScrollbarint() {
        int orientation = Scrollbar.HORIZONTAL;
        scrollbar = new Scrollbar(orientation);
        checkScrollbar(orientation);
        boolean iaeCaught = false;
        orientation = -1;
        try {
            scrollbar = new Scrollbar(orientation);
        } catch (IllegalArgumentException iae) {
            iaeCaught = true;
        }
        assertTrue(iaeCaught);
        orientation = Scrollbar.VERTICAL;
        scrollbar = new Scrollbar(orientation);
        checkScrollbar(orientation);
    }

    public final void testScrollbarintintintintint() {
        int orientation = Scrollbar.VERTICAL;
        int val = 100;
        int vis = 200;
        int min = -500;
        int max = 1500;
        scrollbar = new Scrollbar(orientation, val, vis, min, max);
        checkScrollbar(orientation, val, vis, min, max);
    }

    public final void testGetOrientation() {
        assertEquals(Scrollbar.VERTICAL, scrollbar.getOrientation());
    }

    public final void testSetOrientation() {
        int orientation = Scrollbar.HORIZONTAL;
        scrollbar.setOrientation(orientation);
        checkScrollbar(orientation);
        boolean iaeCaught = false;
        orientation = 10000;
        try {
            scrollbar.setOrientation(orientation);
        } catch (IllegalArgumentException iae) {
            iaeCaught = true;
        }
        assertTrue(iaeCaught);
        checkScrollbar(Scrollbar.HORIZONTAL);
        orientation = Scrollbar.VERTICAL;
        scrollbar.setOrientation(orientation);
        checkScrollbar(orientation);
    }

    public final void testGetValue() {
        assertEquals(0, scrollbar.getValue());
    }

    public final void testSetValue() {
        final int NORMAL_VAL = 66;
        final int SMALL_VAL = -10;
        final int BIG_VAL = 91;
        int val = NORMAL_VAL;
        scrollbar.setValue(val);
        assertEquals(val, scrollbar.getValue());
        scrollbar.setValue(val = SMALL_VAL);
        assertEquals(0, scrollbar.getValue());
        scrollbar.setValue(val = BIG_VAL);
        assertEquals(90, scrollbar.getValue());
    }

    public final void testGetMinimum() {
        assertEquals(0, scrollbar.getMinimum());
    }

    public final void testSetMinimum() {
        final int NORMAL_MIN = -100;
        final int MAX_MIN = Integer.MAX_VALUE;
        final int MIN_MIN = Integer.MIN_VALUE;
        int min = NORMAL_MIN;
        scrollbar.setMinimum(min);
        checkScrollbar(0, 10, min, 100);
        scrollbar.setMinimum(min = MIN_MIN);
        checkScrollbar(min + MAX_MIN - 10, 10, min, min + MAX_MIN);
        scrollbar = new Scrollbar(); //reset params
        scrollbar.setMinimum(min = 50);
        checkScrollbar(min, 10, min, 100);
        scrollbar.setMinimum(min = 95);
        checkScrollbar(min, 100 - min, min, 100);
        scrollbar.setMinimum(min = 100);
        checkScrollbar(min, 1, min, 101);
        scrollbar.setMinimum(min = 1000);
        checkScrollbar(min, 1, min, 1001);
        scrollbar.setMinimum(min = MAX_MIN);
        checkScrollbar(min - 1, 1, min - 1, min);
    }

    public final void testGetMaximum() {
        assertEquals(100, scrollbar.getMaximum());
    }

    public final void testSetMaximum() {
        scrollbar = new Scrollbar(Scrollbar.VERTICAL, -100, 10, -100, 0);
        final int NORMAL_MAX = 120;
        final int MAX_MAX = Integer.MAX_VALUE;
        final int MIN_MAX = Integer.MIN_VALUE;
        int max = NORMAL_MAX;
        scrollbar.setMaximum(max);
        checkScrollbar(-100, 10, -100, max);
        scrollbar.setMaximum(max = MAX_MAX);
        checkScrollbar(-100, 10, -100, MAX_MAX - 100);
        scrollbar = new Scrollbar(); //reset params
        scrollbar.setMaximum(max = 50);
        checkScrollbar(0, 10, 0, max);
        scrollbar.setMaximum(max = 5);
        checkScrollbar(0, max, 0, max);
        scrollbar.setMaximum(max = 0);
        checkScrollbar(-1, 1, -1, 0);
        scrollbar.setMaximum(max = -1000);
        checkScrollbar(-1001, 1, -1001, -1000);
        scrollbar.setMaximum(max = MIN_MAX);
        checkScrollbar(MIN_MAX, 1, MIN_MAX, MIN_MAX + 1);
    }

    public final void testGetVisibleAmount() {
        assertEquals(10, scrollbar.getVisibleAmount());
    }

    @SuppressWarnings("deprecation")
    public final void testGetVisible() {
        assertEquals(10, scrollbar.getVisible());
    }

    public final void testSetVisibleAmount() {
        final int NORMAL_AMOUNT = 15;
        final int SMALL_AMOUNT = -1;
        final int BIG_AMOUNT = 1000;
        int vis = NORMAL_AMOUNT;
        scrollbar.setVisibleAmount(vis);
        checkScrollbar(0, vis, 0, 100);
        scrollbar.setVisibleAmount(vis = SMALL_AMOUNT);
        checkScrollbar(0, 1, 0, 100);
        scrollbar.setVisibleAmount(vis = BIG_AMOUNT);
        checkScrollbar(0, 100, 0, 100);
    }

    public final void testGetUnitIncrement() {
        assertEquals(1, scrollbar.getUnitIncrement());
    }

    @SuppressWarnings("deprecation")
    public final void testGetLineIncrement() {
        assertEquals(1, scrollbar.getLineIncrement());
    }

    public final void testSetUnitIncrement() {
        int unitIncr = 5;
        scrollbar.setUnitIncrement(unitIncr);
        assertEquals(unitIncr, scrollbar.getUnitIncrement());
        scrollbar.setUnitIncrement(unitIncr = 0);
        assertEquals(1, scrollbar.getUnitIncrement());
        scrollbar.setUnitIncrement(unitIncr = Integer.MAX_VALUE);
        assertEquals(unitIncr, scrollbar.getUnitIncrement());
    }

    @SuppressWarnings("deprecation")
    public final void testSetLineIncrement() {
        scrollbar.setLineIncrement(10);
        assertEquals(10, scrollbar.getLineIncrement());
    }

    public final void testGetBlockIncrement() {
        assertEquals(10, scrollbar.getBlockIncrement());
    }

    @SuppressWarnings("deprecation")
    public final void testGetPageIncrement() {
        assertEquals(10, scrollbar.getPageIncrement());
    }

    public final void testSetBlockIncrement() {
        int blockIncr = 150;
        scrollbar.setBlockIncrement(blockIncr);
        assertEquals(blockIncr, scrollbar.getBlockIncrement());
        scrollbar.setBlockIncrement(blockIncr = 0);
        assertEquals(1, scrollbar.getBlockIncrement());
        scrollbar.setBlockIncrement(blockIncr = Integer.MAX_VALUE);
        assertEquals(blockIncr, scrollbar.getBlockIncrement());
    }

    @SuppressWarnings("deprecation")
    public final void testSetPageIncrement() {
        scrollbar.setPageIncrement(3);
        assertEquals(3, scrollbar.getPageIncrement());
    }

    public final void testGetValueIsAdjusting() {
        assertFalse(scrollbar.getValueIsAdjusting());
    }

    public final void testSetValueIsAdjusting() {
        scrollbar.setValueIsAdjusting(true);
        assertTrue(scrollbar.getValueIsAdjusting());
        scrollbar.setValueIsAdjusting(false);
        assertFalse(scrollbar.getValueIsAdjusting());
    }

    public final void testSetValues() {
        int val = 5, vis = 10, min = -100, max = 100;
        scrollbar.setValues(val, vis, min, max);
        checkScrollbar(val, vis, min, max);
        scrollbar.setValues(val = 0, vis = 20, min = 100, max = 50);
        checkScrollbar(min, 1, min, min + 1);
        scrollbar.setValues(val = 20, vis = 200,
                            min = Integer.MIN_VALUE,
                            max = Integer.MAX_VALUE);
        checkScrollbar(-1 - vis, vis, min, -1);
        scrollbar.setValues(val = 200, vis = -200,
                            min = 0,
                            max = 100);
        checkScrollbar(99, 1, min, max);

        scrollbar.setValues(val = -5, vis = 200,
                            min = 0,
                            max = 100);
        checkScrollbar(0, 100, min, max);
    }

    public void testAddGetRemoveAdjustmentListener() {
        assertEquals(0, scrollbar.getAdjustmentListeners().length);

        AdjustmentListener listener = new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent ae) {
            }
        };
        scrollbar.addAdjustmentListener(listener);
        assertEquals(1, scrollbar.getAdjustmentListeners().length);
        assertSame(listener, scrollbar.getAdjustmentListeners()[0]);

        scrollbar.removeAdjustmentListener(listener);
        assertEquals(0, scrollbar.getAdjustmentListeners().length);
    }

    public void testProcessAdjustmentEvent() {
        eventProcessed = false;
        scrollbar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent arg0) {
                eventProcessed = true;
            }
        });
        scrollbar.processEvent(new AdjustmentEvent(scrollbar,
                                              AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
                                              AdjustmentEvent.UNIT_INCREMENT, 1));
        assertTrue(eventProcessed);
    }

    public void testGetListenersClass() {
        Class<AdjustmentListener> cls = AdjustmentListener.class;
        assertEquals(0, scrollbar.getListeners(cls).length);

        AdjustmentListener listener = new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent arg0) {
            }
        };
        scrollbar.addAdjustmentListener(listener);
        assertEquals(1, scrollbar.getListeners(cls).length);
        assertSame(listener, scrollbar.getListeners(cls)[0]);

        scrollbar.removeAdjustmentListener(listener);
        assertEquals(0, scrollbar.getListeners(cls).length);
    }

}
