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
 * @author Evgeniya G. Maenkova
 */
package javax.swing.plaf.metal;

import javax.swing.JTextField;
import javax.swing.SwingTestCase;
import javax.swing.plaf.ComponentUI;

public class MetalTextFieldUITest extends SwingTestCase {
    JTextField tf;

    MetalTextFieldUI ui;

    @Override
    protected void setUp() throws Exception {
        tf = new JTextField();
        ui = new MetalTextFieldUI();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateUI() {
        ComponentUI ui1 = MetalTextFieldUI.createUI(tf);
        assertTrue(ui1 instanceof MetalTextFieldUI);
        ComponentUI ui2 = MetalTextFieldUI.createUI(tf);
        assertTrue(ui2 instanceof MetalTextFieldUI);
        assertNotSame(ui1, ui2);
    }
}
