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

import java.awt.Dimension;
import java.util.Arrays;
import javax.swing.BasicSwingTestCase;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

public class BasicProgressBarUITest extends BasicSwingTestCase {
    private BasicProgressBarUI ui;

    private JProgressBar progressBar;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ui = new BasicProgressBarUI();
        progressBar = new JProgressBar();
        propertyChangeController = new PropertyChangeController();
        progressBar.addPropertyChangeListener(propertyChangeController);
        progressBar.getUI().uninstallUI(progressBar);
    }

    @Override
    public void tearDown() throws Exception {
        progressBar = null;
        ui = null;
        propertyChangeController = null;
        super.tearDown();
    }

    public void testCreateUI() {
        BasicProgressBarUI ui = (BasicProgressBarUI) BasicProgressBarUI.createUI(progressBar);
        assertNotNull(ui);
        assertNull(ui.changeListener);
        assertNull(ui.progressBar);
    }

    public void testInstallUninstallUI() {
        assertNull(progressBar.getBorder());
        ui.installUI(progressBar);
        assertSame(ui.progressBar, progressBar);
        assertNotNull(ui.changeListener);
        assertTrue(Arrays.asList(progressBar.getChangeListeners()).contains(ui.changeListener));
        assertTrue(propertyChangeController.isChanged("border"));
        assertNotNull(progressBar.getBorder());
        assertEquals(UIManager.getInt("ProgressBar.cellLength"), ui.getCellLength());
        assertEquals(UIManager.getInt("ProgressBar.cellSpacing"), ui.getCellSpacing());
        propertyChangeController.reset();
        ui.uninstallUI(progressBar);
        assertEquals(0, progressBar.getChangeListeners().length);
        assertNull(ui.progressBar);
        assertTrue(propertyChangeController.isChanged("border"));
        assertNull(progressBar.getBorder());
    }

    public void testInstallUninstallListeners() {
        ui.progressBar = progressBar;
        ui.installListeners();
        assertTrue(Arrays.asList(progressBar.getChangeListeners()).contains(ui.changeListener));
        assertEquals(2, progressBar.getPropertyChangeListeners().length);
        ui.uninstallListeners();
        assertEquals(1, progressBar.getPropertyChangeListeners().length);
    }

    public void testGetSelectionBackForeGround() {
        ui.installUI(progressBar);
        assertEquals(UIManager.getColor("ProgressBar.selectionBackground"), ui
                .getSelectionBackground());
        assertEquals(UIManager.getColor("ProgressBar.selectionForeground"), ui
                .getSelectionForeground());
    }

    public void testSetGetCellLengthSpacing() {
        ui.installUI(progressBar);
        assertEquals(UIManager.getInt("ProgressBar.cellLength"), ui.getCellLength());
        assertEquals(UIManager.getInt("ProgressBar.cellSpacing"), ui.getCellSpacing());
        ui.setCellLength(11);
        assertEquals(11, ui.getCellLength());
        ui.setCellLength(-1);
        assertEquals(-1, ui.getCellLength());
        progressBar.setStringPainted(true);
        int length = ui.getCellLength();
        if (isHarmony()) {
            assertEquals(3, length);
        }
        ui.setCellLength(11);
        assertEquals(length, ui.getCellLength());
        ui.setCellSpacing(11111);
        if (isHarmony()) {
            assertEquals(2, ui.getCellSpacing());
        }
        progressBar.setStringPainted(false);
        assertEquals(11111, ui.getCellSpacing());
    }

    public void testGetPreferredSizes() {
        ui.installUI(progressBar);
        Dimension inner = ui.getPreferredInnerHorizontal();
        assertSame(inner, ui.getPreferredInnerHorizontal());
        assertEquals(ui.getPreferredSize(progressBar).width, inner.width + 2);
        assertEquals(ui.getPreferredSize(progressBar).height, inner.height + 2);
        UIManager.put("ProgressBar.verticalSize", new Dimension(inner.width + 2,
                inner.height + 2));
        assertEquals(ui.getPreferredSize(progressBar).width, inner.width + 2);
        assertEquals(ui.getPreferredSize(progressBar).height, inner.height + 2);
        assertEquals(ui.getPreferredInnerHorizontal().width, inner.width);
        assertEquals(ui.getPreferredInnerHorizontal().height, inner.height);
    }

    public void testAnimation() throws ArithmeticException {
        ui.installUI(progressBar);
        progressBar.setIndeterminate(true);
        assertEquals(0, ui.getAnimationIndex());
        ui.setAnimationIndex(5);
        assertEquals(5, ui.getAnimationIndex());

        try { //Regression test for HARMONY-2699
            new BasicProgressBarUI().setAnimationIndex(5);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException e) {
            // Expected
        }
    }
    
    /**
     * Regression test for HARMONY-2701 
     * */
    public void testGetBoxLength() { 
        testBasicProgressBarUI pb = new testBasicProgressBarUI(); 
        assertEquals(0, pb.getBoxLength(0, 1)); 
    } 

    /**
     * Regression test for HARMONY-2701 
     * */
    public void testGetBoxLength2() { 
        testBasicProgressBarUI pb = new testBasicProgressBarUI(); 
        assertEquals(8, pb.getBoxLength(50, 1)); 
    }  

    class testBasicProgressBarUI extends BasicProgressBarUI { 
        public int getBoxLength(int a, int b) { 
            return super.getBoxLength(a, b); 
        } 
    }

    public void testStartStop() throws NullPointerException {
        BasicProgressBarUIExt pb = new BasicProgressBarUIExt();
        pb.startAnimationTimer(); 

        pb = new BasicProgressBarUIExt();
        pb.stopAnimationTimer(); 
    }
    
    class BasicProgressBarUIExt extends BasicProgressBarUI {
        public void startAnimationTimer() {
            super.startAnimationTimer();
        }
        
        public void stopAnimationTimer() {
            super.stopAnimationTimer();
        }
    }
    
    public void testHarmony2698Regression() {
        class testBasicProgressBarUI extends BasicProgressBarUI { 
            public Dimension getPreferredInnerVertical() { 
                return super.getPreferredInnerVertical(); 
            } 
      
        public Dimension getPreferredInnerHorizontal() { 
            return super.getPreferredInnerHorizontal(); 
            } 
        }

        try { 
            testBasicProgressBarUI pb = new testBasicProgressBarUI();
            pb.getPreferredInnerHorizontal(); 
            fail("NPE expected"); 
        } catch (NullPointerException e) { 
            //expected
        } 

        try { 
            testBasicProgressBarUI pb = new testBasicProgressBarUI(); 
            pb.getPreferredInnerVertical(); 
            fail("NPE expected"); 
        } catch (NullPointerException e) { 
            //expected
        } 
    }
}
