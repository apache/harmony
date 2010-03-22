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
package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import junit.framework.TestCase;

@SuppressWarnings("serial")
public class ButtonTest extends TestCase {

    class TestButton extends Button {
        public TestButton(String label) {
            super(label);
        }
        public TestButton() {
            super();
        }        
    }

    private TestButton button;
    private boolean eventProcessed;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        button = new TestButton("Button");        
    }

    public void testButton() {
        button = new TestButton();

        assertTrue(button.getLabel().equals(""));
    }

    public void testButtonString() {
        assertTrue(button.getLabel() == "Button");
    }

    public void testGetSetLabel() {
        button.setLabel(null);
        assertNull(button.getLabel());

        button.setLabel("Button");
        assertTrue(button.getLabel().equals("Button"));
    }

    public void testGetSetActionCommand() {
        assertTrue(button.getActionCommand().equals("Button"));
        button.setLabel(null);
        assertNull(button.getActionCommand());

        button.setActionCommand("Button Command");
        assertTrue(button.getActionCommand().equals("Button Command"));
    }

    public void testAddGetRemoveActionListener() {
        assertTrue(button.getActionListeners().length == 0);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {}
        };
        button.addActionListener(listener);
        assertTrue(button.getActionListeners().length == 1);
        assertTrue(button.getActionListeners()[0] == listener);

        button.removeActionListener(listener);
        assertTrue(button.getActionListeners().length == 0);
    }

    public void testProcessEvent() {
        eventProcessed = false;
        button.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent a0) {
                eventProcessed = true;
            }
        });
        button.processEvent(new KeyEvent(button, KeyEvent.KEY_PRESSED, 0, 0, 0, 's'));
        assertTrue(eventProcessed);
    }

    public void testProcessActionEvent() {
        eventProcessed = false;
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                eventProcessed = true;
            }
        });
        button.processEvent(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, null, 0, 0));
        assertTrue(eventProcessed);
    }

    public void testGetListenersClass() {
        assertTrue(button.getListeners(KeyListener.class).length == 0);

        KeyAdapter listener = new KeyAdapter() {};
        button.addKeyListener(listener);
        assertTrue(button.getListeners(KeyListener.class).length == 1);
        assertTrue(button.getListeners(KeyListener.class)[0] == listener);

        button.removeKeyListener(listener);
        assertTrue(button.getListeners(KeyListener.class).length == 0);
    }
}
