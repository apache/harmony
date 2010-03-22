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

import java.awt.Button;
import java.awt.Container;

import junit.framework.TestCase;

public class HierarchyEventTest extends TestCase {

    public final void testHierarchyEventComponentintComponentContainer() {
        Button button = new Button();
        Container container = new Container();
        HierarchyEvent event = new HierarchyEvent(button, HierarchyEvent.ANCESTOR_RESIZED,
                button, container);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), HierarchyEvent.ANCESTOR_RESIZED);
        assertEquals(event.getComponent(), button);
        assertEquals(event.getChangeFlags(), 0);
        assertEquals(event.getChanged(), button);
        assertEquals(event.getChangedParent(), container);
    }

    public final void testHierarchyEventComponentintComponentContainerlong() {
        Button button = new Button();
        Container container = new Container();
        HierarchyEvent event = new HierarchyEvent(button, HierarchyEvent.HIERARCHY_CHANGED,
                button, container, HierarchyEvent.PARENT_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), HierarchyEvent.HIERARCHY_CHANGED);
        assertEquals(event.getComponent(), button);
        assertEquals(event.getChangeFlags(), HierarchyEvent.PARENT_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED);
        assertEquals(event.getChanged(), button);
        assertEquals(event.getChangedParent(), container);
    }

}
