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
 * @author Sergey Burlak
 */
package javax.swing.plaf.basic;

import javax.swing.JToolTip;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class BasicToolTipUITest extends SwingTestCase {
    private BasicToolTipUI tooltipUI;

    private JToolTip tooltip;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try {
            UIManager.setLookAndFeel(new BasicLookAndFeel() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isNativeLookAndFeel() {
                    return true;
                }

                @Override
                public boolean isSupportedLookAndFeel() {
                    return true;
                }

                @Override
                public String getDescription() {
                    return "";
                }

                @Override
                public String getID() {
                    return "";
                }

                @Override
                public String getName() {
                    return "";
                }
            });
        } catch (UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }
        tooltip = new JToolTip();
        tooltipUI = new BasicToolTipUI();
    }

    @Override
    protected void tearDown() throws Exception {
        tooltipUI = null;
        tooltip = null;
        super.tearDown();
    }

    public void testCreateUI() throws Exception {
        assertNotNull(BasicToolTipUI.createUI(null));
        assertTrue(BasicToolTipUI.createUI(null) == BasicToolTipUI.createUI(null));
    }

    public void testGetSize() throws Exception {
        assertEquals(tooltipUI.getPreferredSize(tooltip), tooltipUI.getMaximumSize(tooltip));
        assertEquals(tooltipUI.getPreferredSize(tooltip), tooltipUI.getMinimumSize(tooltip));
    }
}
