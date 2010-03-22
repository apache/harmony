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
package javax.swing.plaf.metal;

import javax.swing.JButton;
import javax.swing.plaf.basic.BasicSeparatorUITest;

public class MetalSeparatorUITest extends BasicSeparatorUITest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ui = new MetalSeparatorUI();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'javax.swing.plaf.metal.MetalSeparatorUI.createUI(JComponent)'
     */
    @Override
    public void testCreateUI() {
        assertNotNull("created UI is not null", MetalSeparatorUI.createUI(new JButton()));
        assertTrue("created UI is of the proper class",
                MetalSeparatorUI.createUI(null) instanceof MetalSeparatorUI);
        assertNotSame("created UI is of unique", MetalSeparatorUI.createUI(null),
                MetalSeparatorUI.createUI(null));
    }
}
