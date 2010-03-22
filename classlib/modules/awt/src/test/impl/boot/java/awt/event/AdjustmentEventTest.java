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
 * @author Michael Danilov
 */
package java.awt.event;

import java.awt.Adjustable;

import junit.framework.TestCase;

public class AdjustmentEventTest extends TestCase {

    public final void testAdjustmentEventAdjustableintintint() {
        AdjustmentEvent event = new AdjustmentEvent(adj, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
                AdjustmentEvent.UNIT_DECREMENT, 10);

        assertEquals(event.getSource(), adj);
        assertEquals(event.getID(), AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED);
        assertEquals(event.getAdjustable(), adj);
        assertEquals(event.getValue(), 10);
        assertEquals(event.getAdjustmentType(), AdjustmentEvent.UNIT_DECREMENT);
        assertFalse(event.getValueIsAdjusting());
    }

    public final void testAdjustmentEventAdjustableintintintboolean() {
        AdjustmentEvent event = new AdjustmentEvent(adj, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
                AdjustmentEvent.BLOCK_DECREMENT, 11, true);

        assertEquals(event.getSource(), adj);
        assertEquals(event.getID(), AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED);
        assertEquals(event.getAdjustable(), adj);
        assertEquals(event.getValue(), 11);
        assertEquals(event.getAdjustmentType(), AdjustmentEvent.BLOCK_DECREMENT);
        assertTrue(event.getValueIsAdjusting());
    }

    public final void testParamString() {
        AdjustmentEvent event = new AdjustmentEvent(adj, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
                AdjustmentEvent.BLOCK_DECREMENT, 11, true);

        assertEquals(event.paramString(), "ADJUSTMENT_VALUE_CHANGED,adjType=BLOCK_DECREMENT,value=11,isAdjusting=true");
        event = new AdjustmentEvent(adj, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED - 1,
                AdjustmentEvent.BLOCK_DECREMENT, 11, true);
        assertEquals(event.paramString(), "unknown type,adjType=BLOCK_DECREMENT,value=11,isAdjusting=true");
        event = new AdjustmentEvent(adj, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
                AdjustmentEvent.BLOCK_DECREMENT + 1024, 11, true);
        assertEquals(event.paramString(), "ADJUSTMENT_VALUE_CHANGED,adjType=unknown type,value=11,isAdjusting=true");
    }

    static final Adjustable adj = new Adjustable() {
        public int getValue() {
            return 0;
        }
        public void setValue(int a0) {
        }
        public void addAdjustmentListener(AdjustmentListener a0) {
        }
        public int getBlockIncrement() {
            return 0;
        }
        public int getMaximum() {
            return 0;
        }
        public int getMinimum() {
            return 0;
        }
        public int getOrientation() {
            return 0;
        }
        public int getUnitIncrement() {
            return 0;
        }
        public int getVisibleAmount() {
            return 0;
        }
        public void removeAdjustmentListener(AdjustmentListener a0) {
        }
        public void setBlockIncrement(int a0) {
        }
        public void setMaximum(int a0) {
        }
        public void setMinimum(int a0) {
        }
        public void setUnitIncrement(int a0) {
        }
        public void setVisibleAmount(int a0) {
        }
    };

}
