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
 */
package javax.swing.plaf.basic;

import java.awt.event.FocusEvent;
import java.util.ArrayList;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingTestCase;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class BasicButtonListenerTest extends SwingTestCase {

    public void testBasicButtonListener() {
        //TODO Implement BasicButtonListener().
    }

    public void testStateChanged() {
        //TODO Implement stateChanged().
    }

    public void testUninstallKeyboardActions() {
        //TODO Implement uninstallKeyboardActions().
    }

    public void testInstallKeyboardActions() {
        AbstractButton button = new JButton();
        BasicButtonListener listener = new BasicButtonListener(button);
        assertEquals(0, button.getActionMap().size());
        assertNotNull(button.getActionMap().getParent());
        assertEquals(3, button.getActionMap().getParent().size());
        assertEquals(2, button.getRegisteredKeyStrokes().length);
        KeyStroke ks = button.getRegisteredKeyStrokes()[0];
        assertNotNull(ks);
        listener.installKeyboardActions(button);
        assertEquals(0, button.getActionMap().size());
        assertEquals(3, button.getActionMap().getParent().size());
        assertEquals(2, button.getRegisteredKeyStrokes().length);
        listener.uninstallKeyboardActions(button);
        assertEquals(0, button.getActionMap().size());
        assertNull(button.getActionMap().getParent());
        if (!isHarmony()) {
            assertEquals(0, button.getRegisteredKeyStrokes().length);
        }
    }

    public void testCheckOpacity() {
        //TODO Implement checkOpacity().
    }

    public void testPropertyChange() {
        try {         
            javax.swing.plaf.basic.BasicButtonListener l = 
            new javax.swing.plaf.basic.BasicButtonListener(new JButton()); 
            l.propertyChange(null); 
            fail("NPE should be thrown");
        } catch (NullPointerException npe) {    
            // PASSED          
        }
    }

    public void testMouseReleased() {
        //TODO Implement mouseReleased().
    }

    public void testMousePressed() {
        //TODO Implement mousePressed().
    }

    public void testMouseMoved() {
        //TODO Implement mouseMoved().
    }

    public void testMouseExited() {
        //TODO Implement mouseExited().
    }

    public void testMouseEntered() {
        //TODO Implement mouseEntered().
    }

    public void testMouseDragged() {
        //TODO Implement mouseDragged().
    }

    public void testMouseClicked() {
        //TODO Implement mouseClicked().
    }

    class AdvancedChangeListener implements ChangeListener {
        public ArrayList<ChangeEvent> events = new ArrayList<ChangeEvent>();

        public ArrayList<Boolean> states = new ArrayList<Boolean>();

        public void stateChanged(ChangeEvent e) {
            events.add(e);
            ButtonModel model = (ButtonModel) e.getSource();
            states.add(Boolean.valueOf(model.isPressed()));
            states.add(Boolean.valueOf(model.isArmed()));
        }
    };

    public void testFocusLost() {
        final JToggleButton button = new JToggleButton();
        button.getModel().setPressed(true);
        button.getModel().setArmed(true);
        button.getModel().setRollover(true);
        BasicButtonListener listener = (BasicButtonListener) button.getChangeListeners()[0];
        AdvancedChangeListener changeListener = new AdvancedChangeListener();
        button.getModel().addChangeListener(changeListener);
        listener.focusLost(new FocusEvent(button, 0));
        if (isHarmony()) {
            assertEquals("number of events", 2, changeListener.events.size());
            assertEquals("Pressed", Boolean.TRUE, changeListener.states.get(0));
            assertEquals("Armed", Boolean.FALSE, changeListener.states.get(1));
            assertEquals("Pressed", Boolean.FALSE, changeListener.states.get(2));
            assertEquals("Armed", Boolean.FALSE, changeListener.states.get(3));
            assertFalse("Pressed", button.getModel().isPressed());
        }
        assertFalse("Armed", button.getModel().isArmed());
        assertTrue("Rollover", button.getModel().isRollover());
    }

    public void testFocusGained() {
        //TODO Implement focusGained().
    }
}