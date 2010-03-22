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
 * @author Dennis Ushakov
 */
package javax.swing.plaf.basic;

import java.awt.Component;
import java.util.Arrays;
import javax.swing.BasicSwingTestCase;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;

public class BasicSpinnerUITest extends BasicSwingTestCase {
    private BasicSpinnerUI ui;

    private JSpinner spinner;

    @Override
    public void setUp() {
        spinner = new JSpinner();
        spinner.getUI().uninstallUI(spinner);
        ui = new BasicSpinnerUI();
    }

    @Override
    public void tearDown() {
        spinner = null;
        ui = null;
    }

    public void testInstallListeners() {
        ui.spinner = spinner;
        ui.installListeners();
        assertTrue(spinner.getPropertyChangeListeners().length > 0);
    }

    public void testInstallUI() {
        ui.installUI(spinner);
        assertTrue(Arrays.asList(spinner.getComponents()).contains(spinner.getEditor()));
    }

    public void testCreatePreviousNextButton() {
        Component nextButton = ui.createNextButton();
        assertTrue(nextButton instanceof BasicArrowButton);
        assertEquals(((BasicArrowButton) nextButton).getDirection(), SwingConstants.NORTH);
        assertTrue(((BasicArrowButton) nextButton).getActionListeners().length > 0);
        assertTrue(((BasicArrowButton) nextButton).getMouseListeners().length > 1);
        assertTrue(((BasicArrowButton) nextButton).getFocusListeners().length > 1);
        assertNotSame(nextButton, ui.createNextButton());
        Component previousButton = ui.createPreviousButton();
        assertTrue(previousButton instanceof BasicArrowButton);
        assertEquals(((BasicArrowButton) previousButton).getDirection(), SwingConstants.SOUTH);
        assertTrue(((BasicArrowButton) previousButton).getActionListeners().length > 0);
        assertTrue(((BasicArrowButton) previousButton).getMouseListeners().length > 1);
        assertTrue(((BasicArrowButton) previousButton).getFocusListeners().length > 1);
        assertNotSame(previousButton, ui.createPreviousButton());
    }

    public void testCreateEditor() {
        ui.spinner = spinner;
        Component editor = ui.createEditor();
        assertSame(editor, spinner.getEditor());
    }
    
    /**
     * Regression test for HARMONY-2716 
     * */
    public void testInstallNextButtonListeners() throws ClassCastException {
        BasicSpinnerUIForTest localBasicSpinnerUI = new BasicSpinnerUIForTest(); 
        localBasicSpinnerUI.installNextButtonListeners(new JLabel()); 
    } 

    /**
     * Regression test for HARMONY-2716 
     * */
    public void testInstallPreviousButtonListeners() throws ClassCastException {
        BasicSpinnerUIForTest localBasicSpinnerUI = new BasicSpinnerUIForTest(); 
        localBasicSpinnerUI.installPreviousButtonListeners(new JLabel()); 
    } 

    class BasicSpinnerUIForTest extends BasicSpinnerUI { 
        public void installNextButtonListeners(Component c){ 
            super.installNextButtonListeners(c); 
        } 
            
        public void installPreviousButtonListeners(Component c){ 
            super.installPreviousButtonListeners(c); 
        } 
    }
}
