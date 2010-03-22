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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import javax.swing.BasicSwingTestCase;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWaitTestCase;

public class BasicScrollBarUITest extends BasicSwingTestCase {
    private BasicScrollBarUI barUI;

    private JScrollBar bar;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bar = new JScrollBar();
        barUI = new BasicScrollBarUI();
        bar.setUI(barUI);
    }

    @Override
    protected void tearDown() throws Exception {
        barUI = null;
        bar = null;
        super.tearDown();
    }

    /**
     * Regression test for HARMONY-2854
     */
    public void testGetTrackThumbNewUI() {
        barUI = new BasicScrollBarUI();
        assertNull(barUI.getTrackBounds());
        assertNull(barUI.getThumbBounds());
    }
    
    public void testSetThumbBounds() throws Exception {
        Rectangle bounds = barUI.getThumbBounds();
        barUI.setThumbBounds(2, 3, 4, 5);
        assertEquals(new Rectangle(2, 3, 4, 5), barUI.getThumbBounds());
        assertTrue(barUI.getThumbBounds() == bounds);
    }

    public void testGetThumbBounds() throws Exception {
        Frame f = new Frame();
        f.setLayout(null);
        f.add(bar);
        f.setSize(200, 200);
        f.setVisible(true);
        SwingWaitTestCase.isRealized(f);
        bar.setBounds(0, 0, 10, 132);
        bar.setValues(0, 50, 0, 100);
        waitForIdle();
        checkIsCloseTo(0, barUI.getThumbBounds().x);
        checkIsCloseTo(16, barUI.getThumbBounds().y);
        checkIsCloseTo(10, barUI.getThumbBounds().width);
        checkIsCloseTo(50, barUI.getThumbBounds().height);
        bar.setValues(30, 50, 0, 100);
        waitForIdle();
        checkIsCloseTo(0, barUI.getThumbBounds().x);
        checkIsCloseTo(46, barUI.getThumbBounds().y);
        checkIsCloseTo(10, barUI.getThumbBounds().width);
        checkIsCloseTo(50, barUI.getThumbBounds().height);
        bar.setValues(50, 50, 0, 100);
        waitForIdle();
        checkIsCloseTo(0, barUI.getThumbBounds().x);
        checkIsCloseTo(66, barUI.getThumbBounds().y);
        checkIsCloseTo(10, barUI.getThumbBounds().width);
        checkIsCloseTo(50, barUI.getThumbBounds().height);
        f.dispose();
    }

    public void testButtonInScrollbar() throws Exception {
        assertEquals(2, bar.getComponentCount());
    }

    public void testPreferredLayoutSize() throws Exception {
        assertEquals(barUI.preferredLayoutSize(bar), barUI.getPreferredSize(bar));
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(500, 600));
        label.setBackground(Color.RED);
        label.setOpaque(true);
        final JScrollPane pane = new JScrollPane(label);
        pane.setPreferredSize(new Dimension(120, 120));
        final JFrame f = createFrame(pane);
        JScrollBar verticalScrollBar = pane.getVerticalScrollBar();
        BasicScrollBarUI basicScrollBarUI = ((BasicScrollBarUI) verticalScrollBar.getUI());
        assertEquals(basicScrollBarUI.getPreferredSize(verticalScrollBar), basicScrollBarUI
                .preferredLayoutSize(verticalScrollBar));
        f.dispose();
    }

    public void testGetMinimumSize() throws Exception {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(500, 600));
        label.setBackground(Color.RED);
        label.setOpaque(true);
        final JScrollPane pane = new JScrollPane(label);
        pane.setPreferredSize(new Dimension(120, 120));
        final JFrame f = createFrame(pane);
        JScrollBar verticalScrollBar = pane.getVerticalScrollBar();
        BasicScrollBarUI basicScrollBarUI = ((BasicScrollBarUI) verticalScrollBar.getUI());
        if (isHarmony()) {
            checkIsCloseTo(17, basicScrollBarUI.getMinimumSize(bar).width);
            checkIsCloseTo(45, basicScrollBarUI.getMinimumSize(bar).height);
        } else {
            assertEquals(new Dimension(15, 55), basicScrollBarUI.getMinimumSize(bar));
        }
        f.dispose();
    }

    public void testGetMaximumSize() throws Exception {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(500, 600));
        label.setBackground(Color.RED);
        label.setOpaque(true);
        final JScrollPane pane = new JScrollPane(label);
        pane.setPreferredSize(new Dimension(120, 120));
        final JFrame f = createFrame(pane);
        JScrollBar verticalScrollBar = pane.getVerticalScrollBar();
        BasicScrollBarUI basicScrollBarUI = ((BasicScrollBarUI) verticalScrollBar.getUI());
        assertEquals(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE), basicScrollBarUI
                .getMaximumSize(bar));
        f.dispose();
    }

    public void testCreateDecreaseButton() throws Exception {
        final JButton b = barUI.createDecreaseButton(SwingConstants.VERTICAL);
        assertTrue(b instanceof BasicArrowButton);
        assertFalse(b == barUI.createDecreaseButton(SwingConstants.VERTICAL));
    }

    public void testCreateIncreaseButton() throws Exception {
        final JButton b = barUI.createIncreaseButton(SwingConstants.VERTICAL);
        assertTrue(b instanceof BasicArrowButton);
        assertFalse(b == barUI.createIncreaseButton(SwingConstants.VERTICAL));
    }

    public void testGetMaximumThumbSize() throws Exception {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(500, 600));
        label.setBackground(Color.RED);
        label.setOpaque(true);
        final JScrollPane pane = new JScrollPane(label);
        pane.setPreferredSize(new Dimension(120, 120));
        final JFrame f = createFrame(pane);
        JScrollBar verticalScrollBar = pane.getVerticalScrollBar();
        BasicScrollBarUI basicScrollBarUI = ((BasicScrollBarUI) verticalScrollBar.getUI());
        assertEquals(new Dimension(4096, 4096), basicScrollBarUI.getMaximumThumbSize());
        f.dispose();
    }

    public void testCreateArrowButtonListener() throws Exception {
        assertNotNull(barUI.createArrowButtonListener());
        assertFalse(barUI.createArrowButtonListener() == barUI.createArrowButtonListener());
    }

    public void testCreatePropertyChangeListener() throws Exception {
        assertNotNull(barUI.createPropertyChangeListener());
        if (isHarmony()) {
            assertFalse(barUI.createPropertyChangeListener() == barUI
                    .createPropertyChangeListener());
        }
    }

    public void testCreateModelListener() throws Exception {
        assertNotNull(barUI.createModelListener());
        assertFalse(barUI.createModelListener() == barUI.createModelListener());
    }

    public void testCreateTrackListener() throws Exception {
        assertNotNull(barUI.createTrackListener());
        assertFalse(barUI.createTrackListener() == barUI.createTrackListener());
    }

    public void testUninstallListeners() throws Exception {
        assertEquals(2, barUI.incrButton.getMouseListeners().length);
        assertEquals(1, bar.getMouseListeners().length);
        assertEquals(1, bar.getPropertyChangeListeners().length);
        assertEquals(2, ((DefaultBoundedRangeModel) bar.getModel()).getChangeListeners().length);
        barUI.uninstallListeners();
        assertEquals(0, bar.getMouseListeners().length);
        assertEquals(0, bar.getPropertyChangeListeners().length);
        assertEquals(1, ((DefaultBoundedRangeModel) bar.getModel()).getChangeListeners().length);
        assertEquals(1, barUI.incrButton.getMouseListeners().length);
    }

    public void testLayoutContainer() throws Exception {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(500, 600));
        label.setBackground(Color.RED);
        label.setOpaque(true);
        final JScrollPane pane = new JScrollPane(label);
        pane.setPreferredSize(new Dimension(120, 120));
        final JFrame f = new JFrame();
        f.getContentPane().add(pane);
        JScrollBar verticalScrollBar = pane.getVerticalScrollBar();
        BasicScrollBarUI basicScrollBarUI = ((BasicScrollBarUI) verticalScrollBar.getUI());
        assertEquals(new Rectangle(0, 0, 0, 0), basicScrollBarUI.incrButton.getBounds());
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.pack();
                f.setVisible(true);
            }
        });
        SwingWaitTestCase.isRealized(f);
        checkIsCloseTo(0, basicScrollBarUI.incrButton.getBounds().x);
        checkIsCloseTo(85, basicScrollBarUI.incrButton.getBounds().y);
        checkIsCloseTo(16, basicScrollBarUI.incrButton.getBounds().width);
        checkIsCloseTo(16, basicScrollBarUI.incrButton.getBounds().height);
        f.dispose();
    }

    public void testConfigureScrollBarColors() {
	try {            
            new BasicScrollBarUI().configureScrollBarColors();
            fail("NPE expected");
        } catch (NullPointerException npe) {
            // PASSED
        }
    }

    private JFrame createFrame(final JScrollPane pane) throws Exception {
        final JFrame f = new JFrame();
        f.getContentPane().add(pane);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.pack();
                f.setVisible(true);
            }
        });
        SwingWaitTestCase.isRealized(f);
        return f;
    }

    private void checkIsCloseTo(final int expected, final int actual) {
        assertTrue("expected: " + expected + ", actual: " + actual,
                Math.abs(expected - actual) <= 2);
    }

    public void testPropertyChange() {
        try {            
            BasicScrollBarUI sbr = new BasicScrollBarUI();
            BasicScrollBarUI.PropertyChangeHandler h = sbr.new PropertyChangeHandler();
            final Object object = new Object();
            PropertyChangeEvent pce = new PropertyChangeEvent(object, "name", object, object);
            h.propertyChange(pce);
        } catch (NullPointerException npe) {            
            fail("NPE thrown");
        }
    }
}
