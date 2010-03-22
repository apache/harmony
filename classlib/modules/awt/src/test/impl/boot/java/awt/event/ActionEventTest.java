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

public class ActionEventTest extends TestCase {

    public final void testActionEventObjectintString() {
        Button button = new Button("Button");
        ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "Button");

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), ActionEvent.ACTION_PERFORMED);
        assertEquals(event.getActionCommand(), "Button");
        assertEquals(event.getWhen(), 0l);
        assertEquals(event.getModifiers(), 0);
    }

    public final void testActionEventObjectintStringint() {
        Button button = new Button("Button");
        ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "Button",
                ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), ActionEvent.ACTION_PERFORMED);
        assertEquals(event.getActionCommand(), "Button");
        assertEquals(event.getWhen(), 0l);
        assertEquals(event.getModifiers(), ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK);
    }

    public final void testActionEventObjectintStringlongint() {
        Button button = new Button("Button");
        long when = System.currentTimeMillis();
        ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "Button",
                when, ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK);

        assertEquals(event.getSource(), button);
        assertEquals(event.getID(), ActionEvent.ACTION_PERFORMED);
        assertEquals(event.getActionCommand(), "Button");
        assertEquals(event.getWhen(), when);
        assertEquals(event.getModifiers(), ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK);
    }

    public final void testParamString() {
        Button button = new Button("Button");
        long when = 1000000000;
        ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "Button",
                when, ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK);

        assertEquals(event.paramString(), "ACTION_PERFORMED,cmd=Button,when=1000000000,modifiers=Ctrl+Alt");
        event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED - 1, "Button",
                when, ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK);
        assertEquals(event.paramString(), "unknown type,cmd=Button,when=1000000000,modifiers=Ctrl+Alt");
    }

}
