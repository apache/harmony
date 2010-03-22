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
package javax.swing.plaf.basic;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingTestCase;
import javax.swing.border.EmptyBorder;

public class BasicOptionPaneUI_ButtonAreaLayoutTest extends SwingTestCase {
    protected BasicOptionPaneUI.ButtonAreaLayout layout;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BasicOptionPaneUI_ButtonAreaLayoutTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testButtonAreaLayout() {
        int padding1 = 100;
        int padding2 = 200;
        boolean syncAll1 = true;
        boolean syncAll2 = false;
        layout = new BasicOptionPaneUI.ButtonAreaLayout(syncAll1, padding1);
        assertEquals("syncAll", syncAll1, layout.getSyncAllWidths());
        assertEquals("padding", padding1, layout.getPadding());
        assertTrue("CentersChildren", layout.getCentersChildren());
        layout = new BasicOptionPaneUI.ButtonAreaLayout(syncAll2, padding2);
        assertEquals("syncAll", syncAll2, layout.getSyncAllWidths());
        assertEquals("padding", padding2, layout.getPadding());
        assertTrue("CentersChildren", layout.getCentersChildren());
    }

    public void testSetGetSyncAllWidths() {
        boolean syncAll1 = true;
        boolean syncAll2 = false;
        layout = new BasicOptionPaneUI.ButtonAreaLayout(syncAll1, 0);
        assertEquals("syncAll", syncAll1, layout.getSyncAllWidths());
        layout.setSyncAllWidths(syncAll2);
        assertEquals("syncAll", syncAll2, layout.getSyncAllWidths());
    }

    public void testSetGetPadding() {
        int padding1 = 100;
        int padding2 = 200;
        layout = new BasicOptionPaneUI.ButtonAreaLayout(true, padding1);
        assertEquals("padding", padding1, layout.getPadding());
        layout.setPadding(padding2);
        assertEquals("padding", padding2, layout.getPadding());
    }

    public void testSetGetCentersChildren() {
        boolean centersChildren1 = true;
        boolean centersChildren2 = false;
        layout = new BasicOptionPaneUI.ButtonAreaLayout(true, 0);
        assertTrue("CentersChildren", layout.getCentersChildren());
        layout.setCentersChildren(centersChildren1);
        assertEquals("CentersChildren", centersChildren1, layout.getCentersChildren());
        layout.setCentersChildren(centersChildren2);
        assertEquals("CentersChildren", centersChildren2, layout.getCentersChildren());
    }

    public void testAddLayoutComponent() {
        Container container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 10);
        container.add(component1);
        container.add(component2);
        layout.addLayoutComponent("aaa", component1);
        layout.addLayoutComponent("bbb", component2);
        component1.setMinimumSize(new Dimension(50, 50));
        component2.setMinimumSize(new Dimension(70, 80));
        component3.setMinimumSize(new Dimension(90, 70));
        component4.setMinimumSize(new Dimension(80, 120));
        assertEquals("Sizes ", new Dimension(30, 10), layout.minimumLayoutSize(container));
        container.add(component3);
        assertEquals("Sizes ", new Dimension(50, 10), layout.minimumLayoutSize(container));
        layout.addLayoutComponent("asd", component3);
        assertEquals("Sizes ", new Dimension(50, 10), layout.minimumLayoutSize(container));
        container.add(component4);
        assertEquals("Sizes ", new Dimension(70, 10), layout.minimumLayoutSize(container));
        layout.addLayoutComponent("dsa", component4);
        assertEquals("Sizes ", new Dimension(70, 10), layout.minimumLayoutSize(container));
    }

    public void testLayoutContainer() {
        JComponent container = new JPanel();
        JComponent component1 = new JButton();
        JComponent component2 = new JButton();
        JComponent component3 = new JButton();
        JComponent component4 = new JButton();
        component4.setMinimumSize(new Dimension(41, 26));
        component3.setMinimumSize(new Dimension(48, 26));
        component2.setMinimumSize(new Dimension(55, 26));
        component1.setMinimumSize(new Dimension(62, 26));
        component4.setPreferredSize(new Dimension(41, 26));
        component3.setPreferredSize(new Dimension(48, 26));
        component2.setPreferredSize(new Dimension(55, 26));
        component1.setPreferredSize(new Dimension(62, 26));
        component4.setMaximumSize(new Dimension(41, 26));
        component3.setMaximumSize(new Dimension(48, 26));
        component2.setMaximumSize(new Dimension(55, 26));
        component1.setMaximumSize(new Dimension(62, 26));
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 10);
        container.setLayout(layout);
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        container.add(component1);
        container.add(component2);
        container.add(component3);
        container.add(component4);
        JDialog window = new JDialog();
        window.getContentPane().add(container);
        window.pack();
        assertEquals("Container's minimum requirements", new Dimension(276, 66), layout
                .minimumLayoutSize(container));
        assertEquals("Container's preferred requirements", new Dimension(276, 66), layout
                .preferredLayoutSize(container));
        assertEquals("Component1 location ", new Point(20, 20), component1.getLocation());
        assertEquals("Component1 size ", new Dimension(62, 26), component1.getSize());
        assertEquals("Component2 location ", new Point(92, 20), component2.getLocation());
        assertEquals("Component2 size ", new Dimension(55, 26), component2.getSize());
        assertEquals("Component3 location ", new Point(157, 20), component3.getLocation());
        assertEquals("Component3 size ", new Dimension(48, 26), component3.getSize());
        assertEquals("Component4 location ", new Point(215, 20), component4.getLocation());
        assertEquals("Component4 size ", new Dimension(41, 26), component4.getSize());
        container.setPreferredSize(new Dimension(1000, 100));
        window.pack();
        assertEquals("Container's minimum requirements", new Dimension(276, 66), layout
                .minimumLayoutSize(container));
        assertEquals("Container's preferred requirements", new Dimension(276, 66), layout
                .preferredLayoutSize(container));
        assertEquals("Component1 location ", new Point(382, 20), component1.getLocation());
        assertEquals("Component1 size ", new Dimension(62, 26), component1.getSize());
        assertEquals("Component2 location ", new Point(454, 20), component2.getLocation());
        assertEquals("Component2 size ", new Dimension(55, 26), component2.getSize());
        assertEquals("Component3 location ", new Point(519, 20), component3.getLocation());
        assertEquals("Component3 size ", new Dimension(48, 26), component3.getSize());
        assertEquals("Component4 location ", new Point(577, 20), component4.getLocation());
        assertEquals("Component4 size ", new Dimension(41, 26), component4.getSize());
        layout.setCentersChildren(false);
        container.setPreferredSize(new Dimension(1200, 100));
        window.pack();
        assertEquals("Container's minimum requirements", new Dimension(276, 66), layout
                .minimumLayoutSize(container));
        assertEquals("Container's preferred requirements", new Dimension(276, 66), layout
                .preferredLayoutSize(container));
        assertEquals("Component1 location ", new Point(20, 20), component1.getLocation());
        assertEquals("Component1 size ", new Dimension(62, 26), component1.getSize());
        assertEquals("Component2 location ", new Point(390, 20), component2.getLocation());
        assertEquals("Component2 size ", new Dimension(55, 26), component2.getSize());
        assertEquals("Component3 location ", new Point(753, 20), component3.getLocation());
        assertEquals("Component3 size ", new Dimension(48, 26), component3.getSize());
        assertEquals("Component4 location ", new Point(1109, 20), component4.getLocation());
        assertEquals("Component4 size ", new Dimension(41, 26), component4.getSize());
        layout = new BasicOptionPaneUI.ButtonAreaLayout(true, 30);
        container.setLayout(layout);
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        container.setPreferredSize(null);
        window.pack();
        assertEquals("Container's minimum requirements", new Dimension(378, 66), layout
                .minimumLayoutSize(container));
        assertEquals("Container's preferred requirements", new Dimension(378, 66), layout
                .preferredLayoutSize(container));
        int offset = isHarmony() ? 20 : 0;
        assertEquals("Component1 location ", new Point(offset + 0, 20), component1
                .getLocation());
        assertEquals("Component1 size ", new Dimension(62, 26), component1.getSize());
        assertEquals("Component2 location ", new Point(offset + 92, 20), component2
                .getLocation());
        assertEquals("Component2 size ", new Dimension(62, 26), component1.getSize());
        assertEquals("Component3 location ", new Point(offset + 184, 20), component3
                .getLocation());
        assertEquals("Component3 size ", new Dimension(62, 26), component1.getSize());
        assertEquals("Component4 location ", new Point(offset + 276, 20), component4
                .getLocation());
        assertEquals("Component4 size ", new Dimension(62, 26), component1.getSize());
    }

    public void testMinimumLayoutSize() {
        JComponent container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        component1.setMinimumSize(new Dimension(41, 26));
        component2.setMinimumSize(new Dimension(48, 26));
        component3.setMinimumSize(new Dimension(55, 26));
        component4.setMinimumSize(new Dimension(62, 26));
        layout = new BasicOptionPaneUI.ButtonAreaLayout(true, 25);
        container.add(component1);
        container.add(component2);
        assertEquals("Minimum size: ", new Dimension(45, 10), layout
                .minimumLayoutSize(container));
        assertEquals("Minimum size: ", new Dimension(45, 10), layout
                .preferredLayoutSize(container));
        component1.setMinimumSize(new Dimension(50, 50));
        component2.setMinimumSize(new Dimension(70, 120));
        component3.setMinimumSize(new Dimension(90, 150));
        component4.setMinimumSize(new Dimension(80, 90));
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 30);
        assertEquals("Minimum size: ", new Dimension(50, 10), layout
                .minimumLayoutSize(container));
        container.add(component3);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(true, 40);
        assertEquals("Minimum size: ", new Dimension(110, 10), layout
                .minimumLayoutSize(container));
        container.add(component4);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 60);
        assertEquals("Minimum size: ", new Dimension(220, 10), layout
                .minimumLayoutSize(container));
        container = new JPanel();
        container.add(component1);
        container.add(component2);
        component1.setAlignmentX(0.75f);
        component2.setAlignmentY(0.75f);
        component3.setAlignmentX(0.25f);
        component4.setAlignmentY(0.25f);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(true, 10);
        assertEquals("Minimum size: ", new Dimension(30, 10), layout
                .minimumLayoutSize(container));
        container.add(component3);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(true, 0);
        assertEquals("Minimum size: ", new Dimension(30, 10), layout
                .minimumLayoutSize(container));
        container.add(component4);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 0);
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        assertEquals("Minimum size: ", new Dimension(80, 50), layout
                .minimumLayoutSize(container));
    }

    public void testPreferredLayoutSize() {
        JComponent container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        component1.setPreferredSize(new Dimension(41, 26));
        component2.setPreferredSize(new Dimension(48, 26));
        component3.setPreferredSize(new Dimension(55, 26));
        component4.setPreferredSize(new Dimension(62, 26));
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 30);
        container.add(component1);
        container.add(component2);
        assertEquals("Preferred size: ", new Dimension(119, 26), layout
                .preferredLayoutSize(container));
        assertEquals("Preferred size: ", new Dimension(119, 26), layout
                .minimumLayoutSize(container));
        component1.setPreferredSize(new Dimension(50, 50));
        component2.setPreferredSize(new Dimension(70, 120));
        component3.setPreferredSize(new Dimension(90, 150));
        component4.setPreferredSize(new Dimension(80, 90));
        layout = new BasicOptionPaneUI.ButtonAreaLayout(true, 40);
        assertEquals("Preferred size: ", new Dimension(180, 120), layout
                .preferredLayoutSize(container));
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 40);
        assertEquals("Preferred size: ", new Dimension(160, 120), layout
                .preferredLayoutSize(container));
        container.add(component3);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 25);
        assertEquals("Preferred size: ", new Dimension(260, 150), layout
                .preferredLayoutSize(container));
        layout.setCentersChildren(true);
        assertEquals("Preferred size: ", new Dimension(260, 150), layout
                .preferredLayoutSize(container));
        container.add(component4);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 20);
        assertEquals("Preferred size: ", new Dimension(350, 150), layout
                .preferredLayoutSize(container));
        container = new JPanel();
        container.add(component1);
        container.add(component2);
        component1.setAlignmentX(0.75f);
        component2.setAlignmentY(0.75f);
        component3.setAlignmentX(0.25f);
        component4.setAlignmentY(0.25f);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 20);
        assertEquals("Preferred size: ", new Dimension(140, 120), layout
                .preferredLayoutSize(container));
        container.add(component3);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 20);
        assertEquals("Preferred size: ", new Dimension(250, 150), layout
                .preferredLayoutSize(container));
        container.add(component4);
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 20);
        container.setBorder(new EmptyBorder(20, 20, 20, 20));
        assertEquals("Preferred size: ", new Dimension(390, 190), layout
                .preferredLayoutSize(container));
    }

    // Regression for HARMONY-2900
    public void testPreferedLayoutSize() {
        layout = new BasicOptionPaneUI.ButtonAreaLayout(false, 20);
        assertEquals(new Dimension(), layout.preferredLayoutSize(null));
        // no exception expected
    }

    public void testRemoveLayoutComponent() {
        Container container = new JPanel();
        JComponent component1 = new JPanel();
        JComponent component2 = new JPanel();
        JComponent component3 = new JPanel();
        JComponent component4 = new JPanel();
        layout = new BasicOptionPaneUI.ButtonAreaLayout(true, 20);
        container.add(component1);
        container.add(component2);
        container.add(component3);
        container.add(component4);
        component1.setMinimumSize(new Dimension(50, 50));
        component2.setMinimumSize(new Dimension(70, 80));
        component3.setMinimumSize(new Dimension(90, 70));
        component4.setMinimumSize(new Dimension(80, 120));
        assertEquals("Sizes ", new Dimension(100, 10), layout.minimumLayoutSize(container));
        container.remove(component4);
        assertEquals("Sizes ", new Dimension(70, 10), layout.minimumLayoutSize(container));
        container.add(component4);
        layout.removeLayoutComponent(component4);
        container.remove(component4);
        assertEquals("Sizes ", new Dimension(70, 10), layout.minimumLayoutSize(container));
        container.remove(component3);
        assertEquals("Sizes ", new Dimension(40, 10), layout.minimumLayoutSize(container));
        container.add(component3);
        layout.removeLayoutComponent(component3);
        assertEquals("Sizes ", new Dimension(70, 10), layout.minimumLayoutSize(container));
    }
}
