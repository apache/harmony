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

public class ContainerEventTest extends TestCase {

    public final void testContainerEvent() {
        Button button = new Button();
        Container container = new Container();
        ContainerEvent event = new ContainerEvent(container,
                ContainerEvent.COMPONENT_REMOVED, button);

        assertEquals(event.getSource(), container);
        assertEquals(event.getID(), ContainerEvent.COMPONENT_REMOVED);
        assertEquals(event.getContainer(), container);
        assertEquals(event.getChild(), button);
    }

    public final void testParamString() {
        Button button = new Button();
        Container container = new Container();
        ContainerEvent event = new ContainerEvent(container,
                ContainerEvent.COMPONENT_REMOVED, button);

        assertEquals(event.paramString(), "COMPONENT_REMOVED,child=button0");
        event = new ContainerEvent(container,
                ContainerEvent.COMPONENT_REMOVED + 1024, button);
        assertEquals(event.paramString(), "unknown type,child=button0");
    }

}
