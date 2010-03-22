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
package javax.swing.plaf.basic;

import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingTestCase;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class BasicSliderUITest extends SwingTestCase {
    private BasicSliderUI sliderUI;

    private JSlider slider;

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
        slider = new JSlider();
        sliderUI = new BasicSliderUI(slider);
    }

    @Override
    protected void tearDown() throws Exception {
        sliderUI = null;
        slider = null;
        super.tearDown();
    }

    public void testCreateUI() throws Exception {
        assertNotNull(BasicSliderUI.createUI(slider));
        assertNotSame(BasicSliderUI.createUI(slider),
                      BasicSliderUI.createUI(slider));
    }

    public void testCreateTrackListener() throws Exception {
        assertNull(sliderUI.slider);
        assertNotNull(sliderUI.createTrackListener(slider));
        assertNull(sliderUI.slider);
        assertNull(sliderUI.trackListener);
        assertNotSame(sliderUI.createTrackListener(slider),
                      sliderUI.createTrackListener(new JSlider()));
    }

    public void testCreateChangeListener() throws Exception {
        assertNull(sliderUI.slider);
        assertNotNull(sliderUI.createChangeListener(slider));
        assertNull(sliderUI.slider);
        assertNull(sliderUI.changeListener);
        assertSame(sliderUI.createChangeListener(slider),
                   sliderUI.createChangeListener(new JSlider()));
    }

    public void testCreateChangeListenerNotSame() throws Exception {
        assertNotSame(sliderUI.createChangeListener(slider),
                      new BasicSliderUI(slider).createChangeListener(slider));
    }

    public void testCreateComponentListener() throws Exception {
        assertNull(sliderUI.slider);
        assertNotNull(sliderUI.createComponentListener(slider));
        assertNull(sliderUI.slider);
        assertNull(sliderUI.componentListener);
        assertSame(sliderUI.createComponentListener(slider),
                   sliderUI.createComponentListener(new JSlider()));
    }

    public void testCreateComponentListenerNotSame() throws Exception {
        assertNotSame(sliderUI.createComponentListener(slider),
                      new BasicSliderUI(slider).createComponentListener(slider));
    }

    public void testCreateFocusListener() throws Exception {
        assertNull(sliderUI.slider);
        assertNotNull(sliderUI.createFocusListener(slider));
        assertNull(sliderUI.slider);
        assertNull(sliderUI.focusListener);
        assertSame(sliderUI.createFocusListener(slider),
                   sliderUI.createFocusListener(new JSlider()));
    }

    public void testCreateFocusListenerNotSame() throws Exception {
        assertNotSame(sliderUI.createFocusListener(slider),
                      new BasicSliderUI(slider).createFocusListener(slider));
    }

    public void testCreateScrollListener() throws Exception {
        assertNull(sliderUI.slider);
        assertNotNull(sliderUI.createScrollListener(slider));
        assertNull(sliderUI.slider);
        assertNull(sliderUI.scrollListener);
        assertNotSame(sliderUI.createScrollListener(slider),
                      sliderUI.createScrollListener(slider));
    }

    // Regression for HARMONY-2878
    public void testCreateScrollListenerNull() throws Exception {
        assertNotNull(sliderUI.createScrollListener(null)); // no exception expected
    }

    public void testCreatePropertyChangeListener() throws Exception {
        assertNull(sliderUI.slider);
        assertNotNull(sliderUI.createPropertyChangeListener(slider));
        assertNull(sliderUI.slider);
        assertNull(sliderUI.propertyChangeListener);
        assertSame(sliderUI.createPropertyChangeListener(slider),
                   sliderUI.createPropertyChangeListener(new JSlider()));
    }

    public void testCreatePropertyChangeListenerNotSame() throws Exception {
        assertNotSame(sliderUI.createPropertyChangeListener(slider),
                      new BasicSliderUI(slider).createPropertyChangeListener(slider));
    }

    public void testGetShadowColor() throws Exception {
        assertNull(sliderUI.getShadowColor());
        sliderUI.installUI(slider);
        assertEquals(UIManager.getColor("Slider.shadow"), sliderUI.getShadowColor());
    }

    public void testGetHighlightColor() throws Exception {
        assertNull(sliderUI.getHighlightColor());
        sliderUI.installUI(slider);
        assertEquals(UIManager.getColor("Slider.highlight"), sliderUI.getHighlightColor());
    }

    public void testGetFocusColor() throws Exception {
        assertNull(sliderUI.getFocusColor());
        sliderUI.installUI(slider);
        assertEquals(UIManager.getColor("Slider.focus"), sliderUI.getFocusColor());
    }

    public void testGetLowestValueLabel() throws Exception {
        sliderUI.installUI(slider);
        slider.setLabelTable(slider.createStandardLabels(1));
        assertEquals("0", ((JLabel) sliderUI.getLowestValueLabel()).getText());
        slider.setLabelTable(slider.createStandardLabels(2, 57));
        assertEquals("57", ((JLabel) sliderUI.getLowestValueLabel()).getText());
    }

    public void testGetHighestValueLabel() throws Exception {
        sliderUI.installUI(slider);
        slider.setLabelTable(slider.createStandardLabels(1));
        assertEquals("100", ((JLabel) sliderUI.getHighestValueLabel()).getText());
        slider.setLabelTable(slider.createStandardLabels(2, 56));
        assertEquals("100", ((JLabel) sliderUI.getHighestValueLabel()).getText());
    }

    public void testGetWidthOfHighValueLabel() throws Exception {
        sliderUI.installUI(slider);
        Hashtable<Integer, JLabel> t = new Hashtable<Integer, JLabel>();
        t.put(new Integer("1"), new JLabel("1"));
        t.put(new Integer("100"), new JLabel("100"));
        JLabel label = new JLabel("1000000");
        t.put(new Integer("1000000"), label);
        slider.setLabelTable(t);
        assertEquals(label.getWidth(), sliderUI.getWidthOfHighValueLabel());
    }

    public void testGetWidthOfLowValueLabel() throws Exception {
        sliderUI.installUI(slider);
        Hashtable<Integer, JLabel> t = new Hashtable<Integer, JLabel>();
        JLabel label = new JLabel("1");
        t.put(new Integer("1"), label);
        t.put(new Integer("100"), new JLabel("100"));
        t.put(new Integer("1000000"), new JLabel("1000000"));
        slider.setLabelTable(t);
        assertEquals(label.getWidth(), sliderUI.getWidthOfLowValueLabel());
    }

    public void testGetHightOfHighValueLabel() throws Exception {
        sliderUI.installUI(slider);
        Hashtable<Integer, JLabel> t = new Hashtable<Integer, JLabel>();
        t.put(new Integer("1"), new JLabel("1"));
        t.put(new Integer("100"), new JLabel("100"));
        JLabel label = new JLabel("1000000");
        t.put(new Integer("1000000"), label);
        slider.setLabelTable(t);
        assertEquals(label.getHeight(), sliderUI.getHeightOfHighValueLabel());
    }

    public void testGetHeightOfLowValueLabel() throws Exception {
        sliderUI.installUI(slider);
        Hashtable<Integer, JLabel> t = new Hashtable<Integer, JLabel>();
        JLabel label = new JLabel("1");
        t.put(new Integer("1"), label);
        t.put(new Integer("100"), new JLabel("100"));
        t.put(new Integer("1000000"), new JLabel("1000000"));
        slider.setLabelTable(t);
        assertEquals(label.getHeight(), sliderUI.getHeightOfLowValueLabel());
    }

    public void testGetWidthOfWidestLabel() throws Exception {
        sliderUI.installUI(slider);
        Hashtable<Integer, JLabel> t = new Hashtable<Integer, JLabel>();
        t.put(new Integer("1"), new JLabel("1"));
        JLabel label = new JLabel("___________100");
        t.put(new Integer("100"), label);
        t.put(new Integer("1000000"), new JLabel("1000000"));
        slider.setLabelTable(t);
        assertEquals(label.getWidth(), sliderUI.getWidthOfWidestLabel());
    }

    public void testGetHeightOfTallestLabel() throws Exception {
        sliderUI.installUI(slider);
        Hashtable<Integer, JLabel> t = new Hashtable<Integer, JLabel>();
        JLabel label = new JLabel("1");
        label.setFont(label.getFont().deriveFont(50f));
        t.put(new Integer("1"), label);
        t.put(new Integer("100"), new JLabel("100"));
        t.put(new Integer("1000000"), new JLabel("1000000"));
        slider.setLabelTable(t);
        assertEquals(label.getHeight(), sliderUI.getHeightOfTallestLabel());
    }

    public void testSetThumbLocation() throws Exception {
        sliderUI.installUI(slider);
        sliderUI.setThumbLocation(200, 500);
        assertEquals(new Point(200, 500), sliderUI.thumbRect.getLocation());
        sliderUI.setThumbLocation(200, -500);
        assertEquals(new Point(200, -500), sliderUI.thumbRect.getLocation());
        sliderUI.setThumbLocation(-200, 500);
        assertEquals(new Point(-200, 500), sliderUI.thumbRect.getLocation());
    }

    // Regression test for HARMONY-2855
    public void testBasicSliderUI() throws Exception {
        assertNull(sliderUI.slider);
    }

    /**
     * <code>uninstallUI</code> is called with the same instance of
     * <code>JSlider</code> to which this UI was installed.
     */
    // Regression test for HARMONY-2855
    public void testUninstallUI01() {
        sliderUI.installUI(slider);
        sliderUI.uninstallUI(slider);
        // No exception is expected
    }

    /**
     * <code>uninstallUI</code> is called before <code>installUI</code>
     * was called.
     */
    // Regression test for HARMONY-2855
    public void testUninstallUI02() {
        try {
            sliderUI.uninstallUI(slider);
            fail("IllegalComponentStateException is expected");
        } catch (IllegalComponentStateException e) {
            // expected
        }
    }

    /**
     * <code>uninstallUI</code> is called with another instance of
     * <code>JSlider</code>.
     */
    // Regression test for HARMONY-2855
    public void testUninstallUI03() {
        try {
            sliderUI.uninstallUI(new JSlider());
            fail("IllegalComponentStateException is expected");
        } catch (IllegalComponentStateException e) {
            // expected
        }
    }

    /**
     * <code>uninstallUI</code> is called with instance of another class, i.e.
     * not <code>JSlider</code> instance.
     */
    // Regression test for HARMONY-2855
    public void testUninstallUI04() {
        try {
            sliderUI.uninstallUI(new JButton());
            fail("IllegalComponentStateException is expected");
        } catch (IllegalComponentStateException e) {
            // expected
        }
    }
    
    /**
     * Regression test for HARMONY-2591
     * */
    public void testActionScrollerEnabled() {
        BasicSliderUI.ActionScroller m = sliderUI.new ActionScroller(new JSlider(),
                3, true);
        assertTrue(m.isEnabled());
    } 
    
    /**
     * Regression test for HARMONY-4445
     */
    public void testMinMaxValue() {

        slider.setMaximum(Integer.MAX_VALUE);
        slider.setMinimum(0);
        slider.setBounds(0,0,100,100);

        int half = Integer.MAX_VALUE / 2;

        // UI slightly modified to omit unneeded actions - no functional changes
        // according to spec
        BasicSliderUI tested = new BasicSliderUI(slider) {
            @Override
            protected void installKeyboardActions(JSlider unneded) {
                // Empty. In real BasicSliderUI this method installs Keyboard
                // actions
            }

            @Override
            protected void installDefaults(JSlider unneded) {
                // Empty. In real BasicSliderUI this method installs defaults
                // (colors and fonts)
            }

            @Override
            protected void installListeners(JSlider unneded) {
                // Empty. In real BasicSliderUI this method installs listeners
            }
        };

        tested.installUI(slider);
        assertEquals(tested.xPositionForValue(half),
                getCenterHorisontalPosition(tested));

        slider.setOrientation(SwingConstants.VERTICAL);
        tested.installUI(slider);

        assertEquals(tested.yPositionForValue(half),
                getCenterVerticalPosition(tested));
        
    }

    private int getCenterVerticalPosition(BasicSliderUI ui) {
        return ui.trackRect.y + (ui.trackRect.height / 2);
    }

    private int getCenterHorisontalPosition(BasicSliderUI ui) {
        return ui.trackRect.x + (ui.trackRect.width / 2);
    }
    
}
