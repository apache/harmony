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
 * Created on 27.04.2005

 */
package javax.swing.plaf.basic;

import javax.swing.JButton;
import javax.swing.SwingTestCase;

public class BasicToggleButtonUITest extends SwingTestCase {
    public class MyBasicToggleButtonUI extends BasicToggleButtonUI {
        @Override
        public String getPropertyPrefix() {
            return super.getPropertyPrefix();
        }

        @Override
        public int getTextShiftOffset() {
            return super.getTextShiftOffset();
        }
    };

    public MyBasicToggleButtonUI ui = null;

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(BasicToggleButtonUITest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new MyBasicToggleButtonUI();
    }

    public void testCreateUI() {
        assertTrue("created UI is not null", null != BasicToggleButtonUI
                .createUI(new JButton()));
        assertTrue("created UI is of the proper class",
                BasicToggleButtonUI.createUI(null) instanceof BasicToggleButtonUI);
        assertTrue("created UI is of unique",
                BasicToggleButtonUI.createUI(null) == BasicToggleButtonUI.createUI(null));
    }

    public void testGetPropertyPrefix() {
        assertEquals("prefix ", "ToggleButton.", ui.getPropertyPrefix());
    }

    public void testGetTextShiftOffset() {
        assertEquals("offset", 0, ui.getTextShiftOffset());
    }
}
