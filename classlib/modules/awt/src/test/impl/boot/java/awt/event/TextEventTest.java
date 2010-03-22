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

import junit.framework.TestCase;

public class TextEventTest extends TestCase {

    public final void testTextEvent() {
        Button button = new Button("Button");
        TextEvent event = new TextEvent(button, TextEvent.TEXT_VALUE_CHANGED);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), TextEvent.TEXT_VALUE_CHANGED);
    }

    public final void testParamString() {
        Button button = new Button("Button");
        TextEvent event = new TextEvent(button, TextEvent.TEXT_VALUE_CHANGED);

        assertEquals(event.paramString(), "TEXT_VALUE_CHANGED");
        event = new TextEvent(button, TextEvent.TEXT_VALUE_CHANGED + 1024);
        assertEquals(event.paramString(), "unknown type");
    }

}
