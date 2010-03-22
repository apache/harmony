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

import java.awt.ItemSelectable;

import junit.framework.TestCase;

public class ItemEventTest extends TestCase {

    ItemSelectable item = new ItemSelectable() {
        public void addItemListener(ItemListener a0) {
        }
        public Object[] getSelectedObjects() {
            return null;
        }
        public void removeItemListener(ItemListener a0) {
        }
    };

    public final void testItemEvent() {
        ItemEvent event = new ItemEvent(item, ItemEvent.ITEM_STATE_CHANGED, item, ItemEvent.SELECTED);

        assertEquals(event.getSource(), item);
        assertEquals(event.getID(), ItemEvent.ITEM_STATE_CHANGED);
        assertEquals(event.getItem(), item);
        assertEquals(event.getStateChange(), ItemEvent.SELECTED);
        assertEquals(event.getItemSelectable(), item);
    }

    public final void testParamString() {
        ItemEvent event = new ItemEvent(item, ItemEvent.ITEM_STATE_CHANGED, item, ItemEvent.SELECTED);

        assertTrue(event.paramString().startsWith("ITEM_STATE_CHANGED,item=java.awt.event.ItemEventTest"));
        assertTrue(event.paramString().endsWith(",stateChange=SELECTED"));

        event = new ItemEvent(item, ItemEvent.ITEM_STATE_CHANGED + 1024, item, ItemEvent.SELECTED + 1024);
        assertTrue(event.paramString().startsWith("unknown type,item=java.awt.event.ItemEventTest"));
        assertTrue(event.paramString().endsWith(",stateChange=unknown type"));
    }

}
