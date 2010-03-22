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
 * Created on 25.04.2005

 */
package javax.swing;

public class JToggleButtonModelTest extends DefaultButtonModelTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        buttonModel = new JToggleButton.ToggleButtonModel();
    }

    /*
     * @see DefaultButtonModelTest#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        buttonModel = null;
        super.tearDown();
    }

    @Override
    public void testSetPressed() {
        super.testSetPressed();
        AbstractButton button = new JToggleButton();
        ButtonModel model = button.getModel();
        model.setPressed(true);
        model.setArmed(true);
        model.setPressed(false);
        assertTrue("selected", button.isSelected());
        model.setPressed(true);
        model.setArmed(true);
        model.setPressed(false);
        assertFalse("selected", button.isSelected());
        model.setSelected(true);
        assertTrue("selected", button.isSelected());
    }

    public void testToggleButtonModel() {
        ButtonModel model = new JToggleButton.ToggleButtonModel();
        assertFalse("selected ", model.isSelected());
        assertFalse("pressed ", model.isPressed());
        assertFalse("armed ", model.isArmed());
        assertTrue("enabled ", model.isEnabled());
        assertFalse("rollover ", model.isRollover());
    }
}
