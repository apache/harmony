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
 * @author Vadim L. Bogdanov
 */
package javax.swing.plaf.basic;

import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;

public class BasicToolBarUIRTest extends SwingTestCase {
    public BasicToolBarUIRTest(final String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNavigateFocusedComponent() {
        class MyButton extends JButton {
            private static final long serialVersionUID = 1L;

            public boolean requestedFocus;

            @Override
            public void requestFocus() {
                requestedFocus = true;
                super.requestFocus();
            }
        }
        JToolBar toolbar = new JToolBar();
        BasicToolBarUI ui = new BasicToolBarUI();
        toolbar.setUI(ui);
        MyButton b1 = new MyButton();
        MyButton b2 = new MyButton();
        b2.setFocusable(false);
        toolbar.add(b1);
        toolbar.add(b2);
        ui.focusedCompIndex = 0;
        ui.navigateFocusedComp(SwingConstants.EAST);
        assertFalse(b2.requestedFocus);
    }
}
